import { test, expect, type Page } from '@playwright/test'

/**
 * 项目级业务策略 e2e — Tier 5 业务深测 Block A-3
 *
 * 守护 11-commit #7 改造: insp_projects 加 max_reject_count /
 * max_escalation_level / appeal_window_days. NULL=用默认.
 * PUT /api/inspection/projects/{id}/policy 改完应立即反映到下次校验.
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

async function listProjects(page: Page): Promise<any[]> {
  const tok = await getToken(page)
  return page.evaluate(async (t) => {
    const r = await fetch('/api/inspection/projects?size=50', { headers: { Authorization: `Bearer ${t}` } })
    if (!r.ok) return []
    const body = await r.json()
    const data = body?.data
    return Array.isArray(data) ? data : (data?.records || data?.list || [])
  }, tok)
}

test.describe('Inspection 项目策略 — Block A-3', () => {
  test.describe.configure({ retries: 1, timeout: 60000 })

  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('PUT /policy 更新生效, GET 立即可读', async ({ page }) => {
    const projects = await listProjects(page)
    test.skip(projects.length === 0, '无可用项目, skip')
    const projectId = projects[0].id
    const tok = await getToken(page)

    // 改策略
    const updated = await page.evaluate(async ({ id, t }) => {
      const r = await fetch(`/api/inspection/projects/${id}/policy`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${t}` },
        body: JSON.stringify({
          maxRejectCount: 7,
          maxEscalationLevel: 2,
          appealWindowDays: 14,
        }),
      })
      return { status: r.status, body: await r.json() }
    }, { id: projectId, t: tok })

    expect(updated.status).toBe(200)
    expect(updated.body.code).toBe(200)
    const data = updated.body.data
    expect(data.maxRejectCount).toBe(7)
    expect(data.maxEscalationLevel).toBe(2)
    expect(data.appealWindowDays).toBe(14)

    // GET 再读 — 应反映新值
    const fetched = await page.evaluate(async ({ id, t }) => {
      const r = await fetch(`/api/inspection/projects/${id}`, { headers: { Authorization: `Bearer ${t}` } })
      return { status: r.status, body: await r.json() }
    }, { id: projectId, t: tok })
    expect(fetched.status).toBe(200)
    expect(fetched.body.data.maxRejectCount).toBe(7)
    expect(fetched.body.data.maxEscalationLevel).toBe(2)
    expect(fetched.body.data.appealWindowDays).toBe(14)
  })

  test('PUT /policy NULL 字段 — 表示用默认', async ({ page }) => {
    const projects = await listProjects(page)
    test.skip(projects.length === 0, '无可用项目, skip')
    const projectId = projects[0].id
    const tok = await getToken(page)

    const result = await page.evaluate(async ({ id, t }) => {
      const r = await fetch(`/api/inspection/projects/${id}/policy`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${t}` },
        body: JSON.stringify({
          maxRejectCount: null,
          maxEscalationLevel: null,
          appealWindowDays: null,
        }),
      })
      return { status: r.status, body: await r.json() }
    }, { id: projectId, t: tok })

    expect(result.status).toBe(200)
    // NULL 应被接受 (字段可空). Jackson 默认 NON_NULL 序列化时 null 字段会被省略,
    // 所以 undefined 也算 — 用 ?? null 容错.
    const data = result.body.data
    expect(data.maxRejectCount ?? null).toBeNull()
    expect(data.maxEscalationLevel ?? null).toBeNull()
    expect(data.appealWindowDays ?? null).toBeNull()
  })

  test('PUT /policy 不存在的项目 ID — 报错', async ({ page }) => {
    const tok = await getToken(page)
    const result = await page.evaluate(async (t) => {
      const r = await fetch(`/api/inspection/projects/99999999/policy`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${t}` },
        body: JSON.stringify({ maxRejectCount: 5 }),
      })
      return r.status
    }, tok)
    expect([400, 404, 500]).toContain(result)
  })
})
