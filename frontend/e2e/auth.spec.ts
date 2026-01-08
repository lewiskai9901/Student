import { test, expect, API_BASE_URL, TEST_USER } from './fixtures/auth.fixture'

/**
 * 认证模块 E2E 测试
 */
test.describe('认证功能', () => {
  test.describe('登录', () => {
    test('应该能够使用正确的凭据登录', async ({ request }) => {
      const response = await request.post(`${API_BASE_URL}/auth/login`, {
        data: TEST_USER
      })

      expect(response.ok()).toBeTruthy()
      const data = await response.json()

      expect(data.code).toBe(200)
      expect(data.data.accessToken).toBeTruthy()
      expect(data.data.refreshToken).toBeTruthy()
      expect(data.data.tokenType).toBe('Bearer')
      console.log('✅ 登录成功')
    })

    test('应该拒绝错误的用户名', async ({ request }) => {
      const response = await request.post(`${API_BASE_URL}/auth/login`, {
        data: {
          username: 'wronguser',
          password: 'admin123'
        }
      })

      const data = await response.json()
      expect(data.code).not.toBe(200)
      console.log('✅ 错误用户名被正确拒绝')
    })

    test('应该拒绝错误的密码', async ({ request }) => {
      const response = await request.post(`${API_BASE_URL}/auth/login`, {
        data: {
          username: 'admin',
          password: 'wrongpassword'
        }
      })

      const data = await response.json()
      expect(data.code).not.toBe(200)
      console.log('✅ 错误密码被正确拒绝')
    })

    test('应该拒绝空的登录请求', async ({ request }) => {
      const response = await request.post(`${API_BASE_URL}/auth/login`, {
        data: {}
      })

      const data = await response.json()
      expect(data.code).not.toBe(200)
      console.log('✅ 空登录请求被正确拒绝')
    })
  })

  test.describe('Token 刷新', () => {
    test('应该能够使用 refreshToken 刷新 accessToken', async ({ request }) => {
      // 先登录获取 tokens
      const loginResponse = await request.post(`${API_BASE_URL}/auth/login`, {
        data: TEST_USER
      })
      const loginData = await loginResponse.json()
      const { refreshToken } = loginData.data

      // 刷新 token
      const refreshResponse = await request.post(`${API_BASE_URL}/auth/refresh`, {
        data: { refreshToken }
      })

      expect(refreshResponse.ok()).toBeTruthy()
      const refreshData = await refreshResponse.json()
      expect(refreshData.code).toBe(200)
      expect(refreshData.data.accessToken).toBeTruthy()
      console.log('✅ Token 刷新成功')
    })

    test('应该拒绝无效的 refreshToken', async ({ request }) => {
      const response = await request.post(`${API_BASE_URL}/auth/refresh`, {
        data: { refreshToken: 'invalid_refresh_token' }
      })

      const data = await response.json()
      expect(data.code).not.toBe(200)
      console.log('✅ 无效 refreshToken 被正确拒绝')
    })
  })

  test.describe('登出', () => {
    test('应该能够成功登出', async ({ request }) => {
      // 先登录
      const loginResponse = await request.post(`${API_BASE_URL}/auth/login`, {
        data: TEST_USER
      })
      const loginData = await loginResponse.json()
      const { accessToken, refreshToken } = loginData.data

      // 登出
      const logoutResponse = await request.post(`${API_BASE_URL}/auth/logout`, {
        data: { refreshToken },
        headers: { Authorization: `Bearer ${accessToken}` }
      })

      expect(logoutResponse.ok()).toBeTruthy()
      console.log('✅ 登出成功')
    })
  })

  test.describe('受保护的端点', () => {
    test('无 Token 访问应返回 403', async ({ request }) => {
      const response = await request.get(`${API_BASE_URL}/users/profile`)

      expect(response.status()).toBeGreaterThanOrEqual(400)
      console.log(`✅ 无 Token 访问正确返回 ${response.status()}`)
    })

    test('有效 Token 应能访问用户信息', async ({ authToken, request }) => {
      const response = await request.get(`${API_BASE_URL}/users/profile`, {
        headers: { Authorization: `Bearer ${authToken}` }
      })

      expect(response.ok()).toBeTruthy()
      const data = await response.json()
      expect(data.code).toBe(200)
      expect(data.data.username).toBe(TEST_USER.username)
      console.log('✅ 获取用户信息成功')
    })

    test('过期/无效 Token 应被拒绝', async ({ request }) => {
      const response = await request.get(`${API_BASE_URL}/users/profile`, {
        headers: { Authorization: 'Bearer expired_or_invalid_token' }
      })

      expect(response.status()).toBeGreaterThanOrEqual(400)
      console.log(`✅ 无效 Token 被正确拒绝，状态码: ${response.status()}`)
    })
  })
})

test.describe('UI 登录流程', () => {
  test('应该显示登录页面', async ({ page }) => {
    await page.goto('/login')

    // 检查登录表单存在
    await expect(page.locator('input[type="text"], input[placeholder*="用户名"]')).toBeVisible()
    await expect(page.locator('input[type="password"]')).toBeVisible()
    console.log('✅ 登录页面正确显示')
  })

  test('应该能通过 UI 登录', async ({ page }) => {
    await page.goto('/login')

    // 填写登录表单
    await page.fill('input[type="text"], input[placeholder*="用户名"]', TEST_USER.username)
    await page.fill('input[type="password"]', TEST_USER.password)

    // 提交表单
    await page.click('button[type="submit"], .el-button--primary')

    // 等待登录成功后跳转
    await page.waitForURL('**/*', { timeout: 10000 })

    // 验证登录成功（不在登录页）
    const currentUrl = page.url()
    expect(currentUrl).not.toContain('/login')
    console.log('✅ UI 登录成功')
  })

  test('应该在登录失败时显示错误', async ({ page }) => {
    await page.goto('/login')

    // 填写错误的凭据
    await page.fill('input[type="text"], input[placeholder*="用户名"]', 'wronguser')
    await page.fill('input[type="password"]', 'wrongpassword')

    // 提交表单
    await page.click('button[type="submit"], .el-button--primary')

    // 等待错误提示（Element Plus 的 Message 组件）
    await page.waitForSelector('.el-message--error, .el-message-box__message', { timeout: 5000 })
    console.log('✅ 登录失败正确显示错误提示')
  })
})
