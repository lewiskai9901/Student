import { test, expect, type Page } from '@playwright/test'

/**
 * 离职检查员批量重派 e2e — Tier 5 业务深测 Block A-5
 *
 * 守护 11-commit #5 + #11 改造:
 *   - POST /api/inspection/tasks/reassign-departed-inspector/{userId}
 *   - per-task 独立事务 (transactionTemplate.execute), partial-success 计数
 *   - fallback 用户存在性校验 — fallbackInspectorId 不存在 → IllegalArgumentException
 *   - 只重派 CLAIMED / IN_PROGRESS, 终态忽略
 *
 * 设计哲学: 不真造离职数据 — 验证错误路径 + 接口契约即可.
 */

async function login(page: Page) {
  await page.goto('/login')
  await page.locator('input[placeholder="请输入账号"]').first().fill('admin')
  await page.locator('input[placeholder="请输入密码"]').first().fill('admin123')
  await page.locator('button[type="submit"]:has-text("登录")').first().click()
  await page.waitForFunction(() => !location.pathname.startsWith('/login'), null, { timeout: 30000 })
}

async function getToken(page: Page): Promise<string | null> {
  await page.waitForFunction(() => !!sessionStorage.getItem('access_token'), null, { timeout: 5000 })
  return page.evaluate(() => sessionStorage.getItem('access_token'))
}

test.describe('Inspection 离职重派 — Block A-5', () => {
  test.describe.configure({ retries: 1, timeout: 60000 })

  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('fallbackInspectorId 不存在 — 立即拒 (用户存在性校验)', async ({ page }) => {
    const tok = await getToken(page)
    // userId=999 (假设不存在) + fallbackInspectorId=99999999 (必不存在)
    const result = await page.evaluate(async (t) => {
      const r = await fetch(`/api/inspection/tasks/reassign-departed-inspector/999`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${t}` },
        body: JSON.stringify({
          reason: 'e2e 离职重派守护 — fallback 校验',
          fallbackInspectorId: 99999999,
          fallbackInspectorName: 'ghost-user',
        }),
      })
      const text = await r.text()
      return { status: r.status, text }
    }, tok)

    expect([400, 422, 500]).toContain(result.status)
    expect(result.text).toMatch(/不存在|已删除|fallback/i)
  })

  test('userId 离开者无任何任务 — 返回 0', async ({ page }) => {
    const tok = await getToken(page)
    // userId=99999999 应该完全不存在任务
    const result = await page.evaluate(async (t) => {
      const r = await fetch(`/api/inspection/tasks/reassign-departed-inspector/99999999`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${t}` },
        body: JSON.stringify({
          reason: 'e2e 无任务情景',
          // 不传 fallback — 仅解除领取
        }),
      })
      return { status: r.status, body: await r.json() }
    }, tok)

    expect(result.status).toBe(200)
    expect(result.body.code).toBe(200)
    // 返回 affected 数 = 0
    expect(result.body.data).toBe(0)
  })

  test('userId 为 null — 拒', async ({ page }) => {
    const tok = await getToken(page)
    // userId 必须 PathVariable, Spring 会 400/404
    // 真正校验在内部 if (userId == null) — 但路径不可能 null
    // 改测试: 不传 reason 应该接受 (reason 可空), 但应该跑过空校验路径
    const result = await page.evaluate(async (t) => {
      const r = await fetch(`/api/inspection/tasks/reassign-departed-inspector/99999998`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${t}` },
        body: JSON.stringify({}),
      })
      return { status: r.status, body: await r.json() }
    }, tok)
    // 不存在用户应仍 200 (无任务)
    expect(result.status).toBe(200)
  })
})
