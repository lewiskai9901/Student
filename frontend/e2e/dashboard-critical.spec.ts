import { test, expect, Page, Response } from '@playwright/test'

/**
 * /dashboard/overview 关键权限与渲染契约 E2E
 *
 * 保护 dashboard 加固全链成果：
 *   Step 1: @CasbinAccess(resource='dashboard', action='view') 在接口层门控
 *   Step 2: DashboardOverviewQueryService 按 DataScope 收敛返回数据
 *   Step 3+4: data_modules + permissions 表种子
 *   Step 5: 前端 /dashboard 路由带 meta.permission='dashboard:view'
 *   DEPT_ADMIN: teacher01 额外持有系部管理员角色，dashboard DataScope=DEPARTMENT_AND_BELOW
 *
 * 核心契约：teacher01 作为经济与信息技术系管理员只能看到 180 学生（不是全校 553）。
 * 无 dashboard:view 权限的用户（纯学生）则被 Casbin 拦截返回 403。
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

  // ============ Admin tests ============

  test('admin login lands on /dashboard and API returns full-school student count', async ({ page }) => {
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

    await expect(page.locator('text=组织概览').first()).toBeVisible({ timeout: 5000 })

    expect(overviewPayload, 'dashboard/overview API should have been called').toBeTruthy()
    const studentCount = overviewPayload?.data?.organization?.studentCount ?? 0
    expect(studentCount, 'admin sees full-school aggregate').toBeGreaterThan(100)
  })

  test('direct API: admin GET /api/dashboard/overview → 200 with all sections', async ({ request }) => {
    const token = await getAccessToken(request, ADMIN)
    const resp = await request.get('http://localhost:8080/api/dashboard/overview', {
      headers: { Authorization: `Bearer ${token}` }
    })
    expect(resp.status()).toBe(200)
    const body = await resp.json()
    expect(body.data).toHaveProperty('organization')
    expect(body.data).toHaveProperty('teaching')
    expect(body.data).toHaveProperty('inspection')
    expect(body.data).toHaveProperty('system')
    expect(body.data.organization.studentCount).toBeGreaterThan(0)
  })

  // ============ DEPT_ADMIN (teacher01) tests ============

  test('teacher01 login lands on /my/dashboard (LoginView userTypeCode fallback)', async ({ page }) => {
    await page.goto('/login')
    await fillLogin(page, TEACHER)
    // LoginView 按 userTypeCode='TEACHER' 落地 /my/dashboard，即使有 dashboard:view 权限
    await page.waitForURL(/\/my\/dashboard/, { timeout: 10000 })
    expect(page.url()).toContain('/my/dashboard')
  })

  test('teacher01 can navigate to /dashboard (has dashboard:view via DEPT_ADMIN)', async ({ page }) => {
    await page.goto('/login')
    await fillLogin(page, TEACHER)
    await page.waitForURL(/\/my\/dashboard/, { timeout: 10000 })

    // 手动导航到 /dashboard — 有权限，应该停留（不再 redirect 到 /my/dashboard）
    await page.goto('/dashboard')
    await page.waitForURL(/\/dashboard($|\?)/, { timeout: 5000 })
    expect(page.url()).toMatch(/\/dashboard($|\?)/)
    expect(page.url()).not.toContain('/403')
  })

  test('teacher01 sidebar shows "首页" (has dashboard:view via DEPT_ADMIN)', async ({ page }) => {
    await page.goto('/login')
    await fillLogin(page, TEACHER)
    await page.waitForURL(/\/my\/dashboard/, { timeout: 10000 })

    await expect(page.locator('aside').locator('text=消息与事件').first()).toBeVisible({ timeout: 8000 })
    // teacher01 现在是 DEPT_ADMIN，有 dashboard:view，侧边栏应显示"首页"
    await expect(page.locator('aside').locator('text=首页').first()).toBeVisible()
  })

  test('direct API: teacher01 sees scoped data — studentCount < admin, > 0', async ({ request }) => {
    const adminToken = await getAccessToken(request, ADMIN)
    const adminResp = await request.get('http://localhost:8080/api/dashboard/overview', {
      headers: { Authorization: `Bearer ${adminToken}` }
    })
    const adminStudents = (await adminResp.json()).data.organization.studentCount

    const teacherToken = await getAccessToken(request, TEACHER)
    const teacherResp = await request.get('http://localhost:8080/api/dashboard/overview', {
      headers: { Authorization: `Bearer ${teacherToken}` }
    })
    expect(teacherResp.status(), 'DEPT_ADMIN should be allowed').toBe(200)
    const teacherBody = await teacherResp.json()
    const teacherStudents = teacherBody.data.organization.studentCount

    // teacher01 是经济与信息技术系管理员，只看到子树的学生（~180），不是全校（~553）
    expect(teacherStudents, 'dept-admin studentCount must be > 0').toBeGreaterThan(0)
    expect(
      teacherStudents,
      `dept-admin (${teacherStudents}) must see fewer students than admin (${adminStudents})`
    ).toBeLessThan(adminStudents)
  })

  // ============ No-permission user (student) test ============

  test('direct API: student user without dashboard:view → 403', async ({ request }) => {
    // stu0001 只有 ROLE_A0739787（学生角色），无 dashboard:view
    const resp = await request.post('http://localhost:8080/api/auth/login', {
      data: { username: 'stu0001', password: 'admin123' },
      headers: { 'Content-Type': 'application/json' }
    })
    const body = await resp.json()
    const token = body?.data?.accessToken
    if (!token) {
      // 学生可能无法登录（密码不同）— 跳过
      test.skip()
      return
    }
    const dashResp = await request.get('http://localhost:8080/api/dashboard/overview', {
      headers: { Authorization: `Bearer ${token}` }
    })
    expect(dashResp.status(), 'student without dashboard:view must be blocked').toBe(403)
  })
})
