import { test, expect } from './fixtures/auth.fixture'
import type { Page } from '@playwright/test'

/**
 * 模板快照漂移 e2e — Tier 5 业务深测 Block A-1
 *
 * 守护 11-commit 整改系列中 #G "快照漂移" 的核心 API 契约:
 *   - GET /api/inspection/projects/{id}/template-version-status
 *       返回 drifted / currentVersionId / latestVersionId 等字段
 *   - POST /api/inspection/projects/{id}/upgrade-template-version
 *       仅 PUBLISHED / PAUSED 状态可升级
 *       已是最新版本时幂等 no-op
 *   - 多模板项目 (rootSectionId=null) → multiTemplate=true, 不在 project 层升级
 *
 * 设计哲学: 自愈型 — 无可用项目时 skip 而非 fail. 不依赖具体业务数据 seed.
 *           token 由 worker-scoped fixture 注入 sessionStorage, 不需要 beforeEach 登录.
 */

async function getToken(page: Page): Promise<string | null> {
  // fixture 已通过 addInitScript 注入 access_token, 但需先 goto 触发脚本
  if (!page.url() || page.url() === 'about:blank') {
    await page.goto('/')
  }
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

test.describe('Inspection 模板漂移 — Block A-1', () => {
  test.describe.configure({ retries: 1, timeout: 60000 })

  test('GET /template-version-status 返回标准结构', async ({ page }) => {
    const projects = await listProjects(page)
    test.skip(projects.length === 0, '无可用项目, skip')
    const projectId = projects[0].id
    expect(projectId).toBeTruthy()

    const tok = await getToken(page)
    const result = await page.evaluate(async ({ id, t }) => {
      const r = await fetch(`/api/inspection/projects/${id}/template-version-status`,
        { headers: { Authorization: `Bearer ${t}` } })
      return { status: r.status, body: await r.json() }
    }, { id: projectId, t: tok })

    expect(result.status).toBe(200)
    expect(result.body.code).toBe(200)
    const data = result.body.data
    // 必返字段
    expect(data).toHaveProperty('drifted')
    expect(typeof data.drifted).toBe('boolean')
    expect(data).toHaveProperty('currentVersionId')
    // rootSectionId 决定后续字段
    if (data.rootSectionId == null) {
      // 多模板项目
      expect(data.multiTemplate).toBe(true)
    } else {
      // 单模板路径: 应有 templateId
      expect(data).toHaveProperty('templateId')
    }
  })

  test('POST /upgrade-template-version 仅 PUBLISHED/PAUSED 可升级', async ({ page }) => {
    const projects = await listProjects(page)
    // 找一个 DRAFT 项目 (后端应拒绝升级)
    const draft = projects.find((p: any) => p.status === 'DRAFT')
    test.skip(!draft, '无 DRAFT 项目, skip')
    const tok = await getToken(page)
    const result = await page.evaluate(async ({ id, t }) => {
      const r = await fetch(`/api/inspection/projects/${id}/upgrade-template-version`,
        { method: 'POST', headers: { Authorization: `Bearer ${t}` } })
      const text = await r.text()
      let body: any
      try { body = JSON.parse(text) } catch { body = { raw: text } }
      return { status: r.status, body }
    }, { id: draft.id, t: tok })

    // 后端用 IllegalStateException, 全局异常处理通常 500 或 200+code != 200
    expect([400, 422, 500]).toContain(result.status)
    expect(JSON.stringify(result.body)).toMatch(/已发布|已暂停|状态/)
  })

  test('POST /upgrade-template-version 已最新时幂等 no-op (返回 200)', async ({ page }) => {
    const projects = await listProjects(page)
    const publishedSingleTemplate = projects.find((p: any) =>
      (p.status === 'PUBLISHED' || p.status === 'PAUSED') && p.rootSectionId != null
    )
    test.skip(!publishedSingleTemplate, '无 PUBLISHED+单模板项目, skip')

    const tok = await getToken(page)
    // 先升级一次 (若 drifted 则升上去, 后续都是 no-op)
    await page.evaluate(async ({ id, t }) => {
      await fetch(`/api/inspection/projects/${id}/upgrade-template-version`,
        { method: 'POST', headers: { Authorization: `Bearer ${t}` } })
    }, { id: publishedSingleTemplate.id, t: tok })

    // 再升一次 — 应该 200, 业务 no-op
    const result = await page.evaluate(async ({ id, t }) => {
      const r = await fetch(`/api/inspection/projects/${id}/upgrade-template-version`,
        { method: 'POST', headers: { Authorization: `Bearer ${t}` } })
      return { status: r.status, body: await r.json() }
    }, { id: publishedSingleTemplate.id, t: tok })

    expect(result.status).toBe(200)
    expect(result.body.code).toBe(200)
    // 升级后 drifted 必须是 false
    const status2 = await page.evaluate(async ({ id, t }) => {
      const r = await fetch(`/api/inspection/projects/${id}/template-version-status`,
        { headers: { Authorization: `Bearer ${t}` } })
      return r.json()
    }, { id: publishedSingleTemplate.id, t: tok })
    expect(status2.data.drifted).toBe(false)
  })

  test('多模板项目 (rootSectionId=null) status 返回 multiTemplate=true', async ({ page }) => {
    const projects = await listProjects(page)
    const multi = projects.find((p: any) => p.rootSectionId == null)
    test.skip(!multi, '无多模板项目, skip')

    const tok = await getToken(page)
    const status = await page.evaluate(async ({ id, t }) => {
      const r = await fetch(`/api/inspection/projects/${id}/template-version-status`,
        { headers: { Authorization: `Bearer ${t}` } })
      return { status: r.status, body: await r.json() }
    }, { id: multi.id, t: tok })

    expect(status.status).toBe(200)
    expect(status.body.data.multiTemplate).toBe(true)

    // 多模板项目升级应被拒
    const result = await page.evaluate(async ({ id, t }) => {
      const r = await fetch(`/api/inspection/projects/${id}/upgrade-template-version`,
        { method: 'POST', headers: { Authorization: `Bearer ${t}` } })
      const text = await r.text()
      let body: any
      try { body = JSON.parse(text) } catch { body = { raw: text } }
      return { status: r.status, body }
    }, { id: multi.id, t: tok })
    expect([400, 422, 500]).toContain(result.status)
    expect(JSON.stringify(result.body)).toMatch(/多模板|计划管理/)
  })

  test('不存在的项目 ID 返回错误', async ({ page }) => {
    const tok = await getToken(page)
    const result = await page.evaluate(async (t) => {
      const r = await fetch(`/api/inspection/projects/99999999/template-version-status`,
        { headers: { Authorization: `Bearer ${t}` } })
      return r.status
    }, tok)
    expect([400, 404, 500]).toContain(result)
  })
})
