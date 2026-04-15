import { test, expect, Page, Response } from '@playwright/test'

/**
 * 关键教师工作台流程 E2E 测试
 *
 * 保护本次 Casbin 重构后的契约：
 *   - TEACHER 登录后自动落地 /my/dashboard (LoginView handleLogin 硬编码)
 *   - 4 个 /my/* 端点 (summary / today / classes / substitute) 均返回 200
 *   - PermissionScope.SELF 放行逻辑在真实请求链上有效
 *   - 我的班级卡片渲染真实 DB 数据
 *
 * 登录页有两套布局（sm:hidden / hidden sm:block），选择器需加 .first()。
 */

const TEACHER = { username: 'teacher01', password: 'admin123' }

async function loginAsTeacher(page: Page) {
  await page.goto('/login')
  await page.locator('input[placeholder="请输入账号"]').first().fill(TEACHER.username)
  await page.locator('input[placeholder="请输入密码"]').first().fill(TEACHER.password)
  await page.locator('button[type="submit"]:has-text("登录")').first().click()
  await page.waitForURL(/\/my\/dashboard/, { timeout: 10000 })
}

test.describe('Critical teacher dashboard flow', () => {

  test('teacher login routes to /my/dashboard and renders greeting', async ({ page }) => {
    await loginAsTeacher(page)
    expect(page.url()).toContain('/my/dashboard')
    await expect(page.locator('.my-title').first()).toBeVisible({ timeout: 5000 })
  })

  test('4 /my/* endpoints all respond 200', async ({ page }) => {
    const apiCalls: { url: string; status: number }[] = []
    page.on('response', (resp: Response) => {
      const url = resp.url()
      if (url.includes('/api/my/')) {
        apiCalls.push({ url, status: resp.status() })
      }
    })

    await loginAsTeacher(page)
    await page.waitForLoadState('networkidle')

    const segments = ['dashboard/summary', 'schedule/today', 'classes', 'tasks/substitute']
    for (const seg of segments) {
      const hit = apiCalls.find(c => c.url.includes(seg))
      expect(hit, `expected /my/${seg} to be called`).toBeDefined()
      expect(hit!.status, `/my/${seg} should return 200, got ${hit!.status}`).toBe(200)
    }
  })

  test('header stats render three numeric slots (no "-" placeholder)', async ({ page }) => {
    await loginAsTeacher(page)
    await page.waitForLoadState('networkidle')

    const bolds = page.locator('.my-stat b')
    const count = await bolds.count()
    expect(count).toBeGreaterThanOrEqual(3)
    for (let i = 0; i < count; i++) {
      const text = (await bolds.nth(i).textContent())?.trim() ?? ''
      expect(text, `stat ${i} should not be "-"`).not.toBe('-')
    }
  })

  test('my-classes card shows teacher01 as head teacher (DB has binding)', async ({ page }) => {
    await loginAsTeacher(page)
    await page.waitForLoadState('networkidle')

    const card = page.locator('.my-card:not(.my-card-wide)').filter({
      has: page.locator('.my-card-title', { hasText: '我的班级' }),
    })
    await expect(card).toBeVisible()
    await expect(card.locator('.my-row').first()).toBeVisible({ timeout: 5000 })
    await expect(card.locator('.my-row-tag', { hasText: '班主任' }).first()).toBeVisible()
  })

  test('no 401/403 on /my/* — Casbin SELF scope must bypass enforcer', async ({ page }) => {
    const forbidden: { url: string; status: number }[] = []
    page.on('response', (resp: Response) => {
      const url = resp.url()
      if (url.includes('/api/my/') && (resp.status() === 401 || resp.status() === 403)) {
        forbidden.push({ url, status: resp.status() })
      }
    })

    await loginAsTeacher(page)
    await page.waitForLoadState('networkidle')

    expect(
      forbidden,
      `got ${forbidden.length} 401/403 on /my/*: ${JSON.stringify(forbidden)}`
    ).toHaveLength(0)
  })
})
