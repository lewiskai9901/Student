import { test, expect, Page, Response } from '@playwright/test'

/**
 * /dashboard/overview 关键权限与渲染契约 E2E
 *
 * 保护本轮 6 步 dashboard 加固成果：
 *   Step 1: @CasbinAccess(resource='dashboard', action='view') 在接口层挡住无权角色
 *   Step 2: DashboardOverviewQueryService 按 DataScope 收敛返回数据
 *   Step 3+4: data_modules + permissions 表种子
 *   Step 5: 前端 /dashboard 路由带 meta.permission='dashboard:view'，
 *           缺权限的已登录用户被 router.beforeEach 重定向到 /my/dashboard
 *
 * 回归场景：teacher01 若能访问 /api/dashboard/overview 就会看到 553 名学生的全校聚合，
 * 这正是本次 commit 324f515c 之前的安全缺口。
 */

const ADMIN = { username: 'admin', password: 'admin123' }
const TEACHER = { username: 'teacher01', password: 'admin123' }

async function fillLogin(page: Page, user: { username: string; password: string }) {
  await page.locator('input[placeholder="请输入账号"]').first().fill(user.username)
  await page.locator('input[placeholder="请输入密码"]').first().fill(user.password)
  await page.locator('button[type="submit"]:has-text("登录")').first().click()
}

async function getAccessToken(request: { post: any }, user: { username: string; password: string }): Promise<string> {
  const resp = await request.post('http://localhost:8080/api/auth/login', {
    data: { username: user.username, password: user.password },
    headers: { 'Content-Type': 'application/json' }
  })
  const body = await resp.json()
  return body?.data?.accessToken as string
}

test.describe('Critical dashboard access-control flow', () => {

  test('admin login lands on /dashboard and API returns non-zero student count', async ({ page }) => {
    // Intercept the dashboard overview API call to validate payload directly
    let overviewPayload: any = null
    page.on('response', async (resp: Response) => {
      if (resp.url().includes('/dashboard/overview') && resp.status() === 200) {
        try { overviewPayload = await resp.json() } catch { /* ignore */ }
      }
    })

    await page.goto('/login')
    await fillLogin(page, ADMIN)
    await page.waitForURL(/\/dashboard($|\?)/, { timeout: 10000 })
    await page.waitForLoadState('networkidle')

    // '组织概览' 统计条渲染
    await expect(page.locator('text=组织概览').first()).toBeVisible({ timeout: 5000 })

    // Validate via intercepted API response (more reliable than DOM scraping)
    expect(overviewPayload, 'dashboard/overview API should have been called').toBeTruthy()
    const studentCount = overviewPayload?.data?.organization?.studentCount ?? 0
    expect(studentCount, 'admin studentCount should be > 0').toBeGreaterThan(0)
  })

  test('teacher01 login lands on /my/dashboard (not /dashboard)', async ({ page }) => {
    await page.goto('/login')
    await fillLogin(page, TEACHER)
    await page.waitForURL(/\/my\/dashboard/, { timeout: 10000 })
    expect(page.url()).toContain('/my/dashboard')
    expect(page.url()).not.toContain('/dashboard?')
  })

  test('teacher01 manual navigation to /dashboard silently redirects to /my/dashboard', async ({ page }) => {
    // 先登录落 /my/dashboard
    await page.goto('/login')
    await fillLogin(page, TEACHER)
    await page.waitForURL(/\/my\/dashboard/, { timeout: 10000 })

    // 手动跳 /dashboard — router 守卫应兜底到 /my/dashboard，不应命中 /403
    await page.goto('/dashboard')
    await page.waitForURL(/\/my\/dashboard/, { timeout: 5000 })
    expect(page.url()).toContain('/my/dashboard')
    expect(page.url()).not.toContain('/403')
  })

  test('teacher01 has NO "首页" entry in the sidebar menu', async ({ page }) => {
    await page.goto('/login')
    await fillLogin(page, TEACHER)
    await page.waitForURL(/\/my\/dashboard/, { timeout: 10000 })

    // 等待侧边栏菜单渲染完成（teacher01 能看到不受权限控制的一级菜单）
    await expect(page.locator('aside').locator('text=消息与事件').first()).toBeVisible({ timeout: 8000 })

    // '首页' 菜单项对应 /dashboard — 受 meta.permission='dashboard:view' 过滤，
    // teacher01 没有该权限，menu-generator 应将其过滤掉。
    // 注意：breadcrumb 组件（<nav>）也含"首页"文本，这里只检查侧边栏（<aside>）。
    const homeMenu = page.locator('aside').locator('text=首页')
    await expect(homeMenu).toHaveCount(0)
  })

  test('direct API: admin GET /api/dashboard/overview → 200 with aggregated payload', async ({ request }) => {
    const token = await getAccessToken(request, ADMIN)
    const resp = await request.get('http://localhost:8080/api/dashboard/overview', {
      headers: { Authorization: `Bearer ${token}` }
    })
    expect(resp.status(), 'admin should be allowed').toBe(200)
    const body = await resp.json()
    expect(body.code).toBe(200)
    expect(body.data).toHaveProperty('organization')
    expect(body.data).toHaveProperty('teaching')
    expect(body.data).toHaveProperty('inspection')
    expect(body.data).toHaveProperty('system')
    expect(body.data.organization.studentCount).toBeGreaterThan(0)
  })

  test('direct API: teacher01 GET /api/dashboard/overview → 403 (Casbin gate holds)', async ({ request }) => {
    const token = await getAccessToken(request, TEACHER)
    const resp = await request.get('http://localhost:8080/api/dashboard/overview', {
      headers: { Authorization: `Bearer ${token}` }
    })
    expect(
      resp.status(),
      'teacher must not see full-school aggregate — this is the original leak from pre-324f515c'
    ).toBe(403)
  })
})
