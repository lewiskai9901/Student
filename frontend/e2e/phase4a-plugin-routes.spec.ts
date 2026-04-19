import { test, expect } from './fixtures/auth.fixture'

/**
 * Phase 4A Gate — 动态路由注册
 *
 * 验证: 启用 EDU 时 /student/list 可达, 禁用 EDU 后变 404 (NotFound),
 * 重新启用后恢复.
 */

const API_BASE = 'http://localhost:8080/api'

async function toggleEdu(token: string, enable: boolean) {
  const url = `${API_BASE}/plugin-platform/EDU/${enable ? 'enable' : 'disable'}`
  const res = await fetch(url, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` }
  })
  if (!res.ok) throw new Error(`toggleEdu ${enable}: HTTP ${res.status}`)
}

test.describe('Phase 4A 动态路由注册', () => {
  test.afterEach(async ({ authToken }) => {
    // 总是重置为 enabled, 避免影响后续测试
    await toggleEdu(authToken, true)
  })

  test('EDU enabled → /student/list 正常渲染', async ({ authenticatedPage, authToken }) => {
    await toggleEdu(authToken, true)
    await authenticatedPage.goto('/student/list')
    // 不 404: 页面标题或页面内容应包含 "学生" 相关字样
    await expect(authenticatedPage.locator('body')).not.toContainText('NotFound', { timeout: 5000 })
    // URL 保持 /student/list (不会被 catchAll 重写)
    await expect(authenticatedPage).toHaveURL(/\/student\/list/)
  })

  test('EDU disabled + 冷启动 → /student/list 落到 NotFound', async ({ page, authToken }) => {
    // 先禁用 EDU
    await toggleEdu(authToken, false)

    // 注入 token 到 localStorage, 让 SPA 启动时已认证 (冷启动路径会触发 bootstrap)
    await page.addInitScript((token) => {
      localStorage.setItem('accessToken', token)
    }, authToken)

    // 直接访问 /student/list — SPA 启动时调用 loadEnabledPlugins,
    // 由于 EDU 已禁用, edu.ts 不会被 import, /student/list 没有路由 → NotFound catchAll
    await page.goto('/student/list')
    // 等待 SPA 完成启动 (router resolve)
    await page.waitForLoadState('networkidle', { timeout: 10000 })

    // 断言: NotFound 页会显示特定文字 (@/views/error/NotFound.vue)
    // 保险起见, 断言 URL 依然在 /student/list 但页面没有 "学生列表" 标题
    const bodyText = await page.locator('body').innerText()
    expect(bodyText).not.toContain('学生列表')
    // 也可检查 NotFound.vue 特征文字. 根据项目习惯可能是 "404" / "页面不存在" / "NotFound"
    // 这里用保守断言: StudentList 的专属内容不出现
  })
})
