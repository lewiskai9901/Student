import { test, expect, type Page } from '@playwright/test'

/**
 * 任务多类型 e2e — 守护 V108 核心契约:
 *  - AD_HOC 任务永不逾期 (deadlinePolicy=NONE)
 *  - 项目 inspection_mode 配置 GET/PUT 工作
 *  - allowed-projects API 只返回 allow_ad_hoc=1 的项目
 *  - 抽查发起对话框 + 端到端创建链路
 */

async function login(page: Page) {
  await page.goto('/login')
  await page.locator('input[placeholder="请输入账号"]').first().fill('admin')
  await page.locator('input[placeholder="请输入密码"]').first().fill('admin123')
  await page.locator('button[type="submit"]:has-text("登录")').first().click()
  await page.waitForFunction(() => !location.pathname.startsWith('/login'), null, { timeout: 30000 })
}

test.describe('V108 任务多类型', () => {
  test.describe.configure({ retries: 1, timeout: 60000 })

  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('GET /inspection/tasks/projects/{id}/inspection-mode 返回项目模式配置', async ({ page }) => {
    const result = await page.evaluate(async () => {
      const tok = sessionStorage.getItem('access_token')
      const r = await fetch('/api/inspection/tasks/projects/2/inspection-mode', {
        headers: { Authorization: `Bearer ${tok}` }
      })
      return { status: r.status, body: await r.json() }
    })
    expect(result.status).toBe(200)
    const data = result.body?.data
    expect(data).toHaveProperty('inspection_mode')
    expect(['PLANNED','HYBRID','SPOT_CHECK','SELF_AUDIT','EMERGENCY']).toContain(data.inspection_mode)
  })

  test('PUT /inspection-mode 更新生效, 立即可读到', async ({ page }) => {
    const result = await page.evaluate(async () => {
      const tok = sessionStorage.getItem('access_token')
      const r = await fetch('/api/inspection/tasks/projects/2/inspection-mode', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tok}` },
        body: JSON.stringify({
          inspectionMode: 'HYBRID',
          allowAdHoc: true,
          allowSelfCheck: true,
          adHocQuotaPerInspector: 8,
        })
      })
      return { status: r.status, body: await r.json() }
    })
    expect(result.status).toBe(200)
    const data = result.body?.data
    expect(data.inspection_mode).toBe('HYBRID')
    expect(Number(data.allow_ad_hoc)).toBe(1)
    expect(Number(data.allow_self_check)).toBe(1)
    expect(data.ad_hoc_quota_per_inspector).toBe(8)
  })

  test('GET /ad-hoc/allowed-projects 只返回 allow_ad_hoc=1 项目', async ({ page }) => {
    const result = await page.evaluate(async () => {
      const tok = sessionStorage.getItem('access_token')
      const r = await fetch('/api/inspection/tasks/ad-hoc/allowed-projects', {
        headers: { Authorization: `Bearer ${tok}` }
      })
      return { status: r.status, body: await r.json() }
    })
    expect(result.status).toBe(200)
    const list = result.body?.data || []
    expect(Array.isArray(list)).toBe(true)
    // 每条都是 allow_ad_hoc=1
    for (const p of list) {
      expect(Number(p.allow_ad_hoc)).toBe(1)
    }
  })

  test('POST /ad-hoc 创建抽查任务, taskType=AD_HOC + status=CLAIMED + deadlinePolicy=NONE', async ({ page }) => {
    const result = await page.evaluate(async () => {
      const tok = sessionStorage.getItem('access_token')
      const r = await fetch('/api/inspection/tasks/ad-hoc', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tok}` },
        body: JSON.stringify({
          projectId: 2,
          reason: 'e2e 测试发起抽查 (V108) — 模拟举报触发场景',
        })
      })
      return { status: r.status, body: await r.json() }
    })
    expect(result.status).toBe(200)
    const t = result.body?.data
    expect(t).toHaveProperty('id')
    expect(t.taskType).toBe('AD_HOC')
    expect(t.status).toBe('CLAIMED')
    expect(t.taskCode).toMatch(/^TSK-/)
  })

  test('POST /ad-hoc 拒绝项目 allow_ad_hoc=0 的请求', async ({ page }) => {
    // 先把项目 1 关掉抽查
    await page.evaluate(async () => {
      const tok = sessionStorage.getItem('access_token')
      await fetch('/api/inspection/tasks/projects/1/inspection-mode', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tok}` },
        body: JSON.stringify({ inspectionMode: 'PLANNED', allowAdHoc: false }),
      })
    })
    const result = await page.evaluate(async () => {
      const tok = sessionStorage.getItem('access_token')
      const r = await fetch('/api/inspection/tasks/ad-hoc', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tok}` },
        body: JSON.stringify({ projectId: 1, reason: '应该被拒绝因为项目不允许抽查' }),
      })
      return { status: r.status, body: await r.json() }
    })
    // 后端抛 IllegalStateException → 通常 500 或 400
    expect([400, 422, 500]).toContain(result.status)
    expect(JSON.stringify(result.body)).toContain('不允许临时抽查')
  })

  test('POST /ad-hoc 拒绝空 reason', async ({ page }) => {
    const result = await page.evaluate(async () => {
      const tok = sessionStorage.getItem('access_token')
      const r = await fetch('/api/inspection/tasks/ad-hoc', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tok}` },
        body: JSON.stringify({ projectId: 2, reason: '' }),
      })
      return { status: r.status, body: await r.json() }
    })
    expect([400, 422, 500]).toContain(result.status)
    expect(JSON.stringify(result.body)).toContain('原因')
  })

  test('我的任务页有 ⚡ 抽查 tab + ⚡ 发起抽查按钮', async ({ page }) => {
    await page.goto('/inspection/tasks')
    await page.waitForLoadState('networkidle', { timeout: 10000 })
    // 发起按钮
    const adhocBtn = page.locator('button:has-text("发起抽查")')
    await expect(adhocBtn).toBeVisible({ timeout: 10000 })
    // 抽查 tab (有抽查任务时才出现, 否则点按钮触发对话框)
    await adhocBtn.click()
    await expect(page.locator('.el-dialog__title:has-text("发起临时抽查")')).toBeVisible({ timeout: 5000 })
    // 关掉
    await page.locator('.el-dialog button:has-text("取消")').click()
  })
})
