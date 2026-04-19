import { test, expect } from '@playwright/test'

/**
 * Phase 4A Gate — 动态路由注册
 *
 * 验证路径 (覆盖 commit d8ecf1c3 + e37234b1 两个 fix):
 *   1. EDU enabled  → /student/list 正常渲染 (包含 "学生列表" 标题)
 *   2. EDU enabled  → 侧栏含教育菜单 (学生/学术/教务/宿舍)
 *   3. EDU disabled → /student/list 落到 NotFound ("页面不存在")
 *   4. EDU disabled → 侧栏失去所有教育菜单
 *
 * 本 spec 不使用 auth.fixture 的快捷注入 (key 不匹配 tokenStorage),
 * 而是走完整 login form 触发 auth store loginAction → bootstrap.
 */

const API_BASE = 'http://localhost:8080/api'
const TEST_USER = { username: 'admin', password: 'admin123' }
const EDU_MENU_TITLES = ['学术管理', '学生管理', '教务管理', '宿舍管理']

async function apiLogin(): Promise<string> {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(TEST_USER)
  })
  const json = await res.json()
  if (json.code !== 200) throw new Error(`login failed: ${json.message}`)
  return json.data.accessToken
}

async function toggleEdu(token: string, enable: boolean): Promise<void> {
  const url = `${API_BASE}/plugin-platform/EDU/${enable ? 'enable' : 'disable'}`
  const res = await fetch(url, { method: 'POST', headers: { Authorization: `Bearer ${token}` } })
  if (!res.ok) throw new Error(`toggleEdu ${enable}: HTTP ${res.status}`)
}

/** 走完整登录表单 → 触发 auth store loginAction → bootstrap */
async function loginViaForm(page: import('@playwright/test').Page) {
  await page.goto('/login')
  await page.fill('input[type="text"]', TEST_USER.username)
  await page.fill('input[type="password"]', TEST_USER.password)
  await page.click('button[type="submit"]')
  await page.waitForURL(/\/dashboard/, { timeout: 10000 })
}

test.describe('Phase 4A 动态路由注册', () => {
  test.afterEach(async () => {
    // 每个用例结束后恢复 EDU enabled, 避免相互污染
    const token = await apiLogin()
    await toggleEdu(token, true)
  })

  test('EDU enabled: /student/list 渲染 + 侧栏有 EDU 菜单', async ({ page }) => {
    await toggleEdu(await apiLogin(), true)

    await loginViaForm(page)
    await page.goto('/student/list')
    await page.waitForLoadState('networkidle', { timeout: 10000 })

    // 1. 页面内容: StudentList 组件有 "学生列表" 标题
    await expect(page.locator('body')).toContainText('学生列表')
    await expect(page).toHaveURL(/\/student\/list/)

    // 2. 侧栏菜单: 4 个教育菜单都出现 (覆盖 e37234b1 fix)
    const sidebar = page.locator('aside')
    for (const title of EDU_MENU_TITLES) {
      await expect(sidebar).toContainText(title)
    }
  })

  test('EDU disabled: /student/list 落到 NotFound + dashboard 侧栏清空 EDU 菜单', async ({ page }) => {
    await toggleEdu(await apiLogin(), false)

    await loginViaForm(page)

    // 在 dashboard 检查侧栏 (NotFound 可能无 Layout, 直接在 dashboard 上检查)
    await expect(page).toHaveURL(/\/dashboard/)
    await page.waitForLoadState('networkidle', { timeout: 10000 })
    const dashSidebarText = await page.locator('aside').innerText()
    for (const title of EDU_MENU_TITLES) {
      expect(dashSidebarText).not.toMatch(new RegExp(`\\n${title}\\n`))
    }

    // 再访问 /student/list, 应落到 NotFound
    await page.goto('/student/list')
    await page.waitForLoadState('networkidle', { timeout: 10000 })
    await expect(page.locator('body')).toContainText('页面不存在')
    await expect(page.locator('body')).not.toContainText('学生列表')
  })
})
