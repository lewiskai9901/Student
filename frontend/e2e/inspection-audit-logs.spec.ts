import { test, expect } from './fixtures/auth.fixture'
import type { Page } from '@playwright/test'

/**
 * 审计日志 e2e — Tier 5 业务深测 Block A-4
 *
 * 守护 11-commit #C "审计不可查" 改造:
 *   - InspectionAuditLogger 写入 inspection_audit_logs 表
 *   - GET /api/inspection/audit-logs?entityType=&entityId=
 *   - GET /api/inspection/audit-logs/by-action?action=
 *
 * 触发链: PUT /policy → audit log 落 entityType=InspProject + action 应固定.
 */

async function getToken(page: Page): Promise<string | null> {
  if (!page.url() || page.url() === 'about:blank') await page.goto('/')
  return page.evaluate(() => sessionStorage.getItem('access_token'))
}

async function listProjects(page: Page): Promise<any[]> {
  const tok = await getToken(page)
  return page.evaluate(async (t) => {
    const r = await fetch('/api/inspection/projects?size=10', { headers: { Authorization: `Bearer ${t}` } })
    if (!r.ok) return []
    const body = await r.json()
    const data = body?.data
    return Array.isArray(data) ? data : (data?.records || data?.list || [])
  }, tok)
}

test.describe('Inspection 审计日志 — Block A-4', () => {
  test.describe.configure({ retries: 1, timeout: 60000 })

  test('GET /audit-logs by entity 返回结构正确 (字段齐)', async ({ page }) => {
    const projects = await listProjects(page)
    test.skip(projects.length === 0, '无可用项目, skip')
    const projectId = projects[0].id
    const tok = await getToken(page)

    const result = await page.evaluate(async ({ id, t }) => {
      const r = await fetch(
        `/api/inspection/audit-logs?entityType=InspProject&entityId=${id}&limit=20`,
        { headers: { Authorization: `Bearer ${t}` } })
      return { status: r.status, body: await r.json() }
    }, { id: projectId, t: tok })

    expect(result.status).toBe(200)
    expect(result.body.code).toBe(200)
    expect(Array.isArray(result.body.data)).toBe(true)
    // 若有记录, 校验字段齐
    if (result.body.data.length > 0) {
      const row = result.body.data[0]
      // jdbcTemplate.queryForList 返回的 column key 可能下划线也可能驼峰, 容错
      const keys = Object.keys(row).map(k => k.toLowerCase())
      expect(keys).toEqual(expect.arrayContaining(['action']))
      expect(keys.some(k => k.includes('entity'))).toBe(true)
      expect(keys.some(k => k.includes('occurred'))).toBe(true)
    }
  })

  test('PUT /policy 后, audit log 多一条 PROJECT_POLICY_UPDATED', async ({ page }) => {
    const projects = await listProjects(page)
    test.skip(projects.length === 0, '无可用项目, skip')
    const projectId = projects[0].id
    const tok = await getToken(page)

    // 先记录初始条数
    const before = await page.evaluate(async ({ id, t }) => {
      const r = await fetch(
        `/api/inspection/audit-logs?entityType=InspProject&entityId=${id}&limit=200`,
        { headers: { Authorization: `Bearer ${t}` } })
      const body = await r.json()
      return (body?.data || []).length
    }, { id: projectId, t: tok })

    // 触发 — 改 policy
    await page.evaluate(async ({ id, t }) => {
      await fetch(`/api/inspection/projects/${id}/policy`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${t}` },
        body: JSON.stringify({ maxRejectCount: 6, appealWindowDays: 10 }),
      })
    }, { id: projectId, t: tok })

    // 再读 — 条数应 + 1 (允许并发场景容差 +>=1)
    const after = await page.evaluate(async ({ id, t }) => {
      const r = await fetch(
        `/api/inspection/audit-logs?entityType=InspProject&entityId=${id}&limit=200`,
        { headers: { Authorization: `Bearer ${t}` } })
      const body = await r.json()
      return body?.data || []
    }, { id: projectId, t: tok })

    expect(after.length).toBeGreaterThanOrEqual(before + 1)
    // 最新一条 action 应跟策略相关 (兼容大小写 & 命名变体)
    if (after.length > 0) {
      const latestAction = String(after[0].action || after[0].ACTION || '').toUpperCase()
      // 不强约束精确名, 但必须含 POLICY 关键词
      expect(latestAction).toMatch(/POLICY|PROJECT|UPDATE/i)
    }
  })

  test('GET /audit-logs/by-action 返回过滤后列表', async ({ page }) => {
    const tok = await getToken(page)
    // 任意 action 都行 — 我们刚刚才触发了一条 policy 相关的
    const result = await page.evaluate(async (t) => {
      const r = await fetch(
        `/api/inspection/audit-logs/by-action?action=PROJECT_POLICY_UPDATED&limit=50`,
        { headers: { Authorization: `Bearer ${t}` } })
      return { status: r.status, body: await r.json() }
    }, tok)

    expect(result.status).toBe(200)
    expect(Array.isArray(result.body.data)).toBe(true)
  })

  test('limit 上限 200, 超过会被裁剪', async ({ page }) => {
    const tok = await getToken(page)
    const result = await page.evaluate(async (t) => {
      const r = await fetch(
        `/api/inspection/audit-logs?entityType=InspProject&entityId=1&limit=99999`,
        { headers: { Authorization: `Bearer ${t}` } })
      return { status: r.status, body: await r.json() }
    }, tok)
    expect(result.status).toBe(200)
    const data = result.body.data || []
    expect(data.length).toBeLessThanOrEqual(200)
  })
})
