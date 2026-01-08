import { test as base, expect, Page } from '@playwright/test'

// API 基础 URL
const API_BASE_URL = 'http://localhost:8080/api'

// 测试账号
const TEST_USER = {
  username: 'admin',
  password: 'admin123'
}

// 扩展的测试上下文
interface AuthFixtures {
  authenticatedPage: Page
  authToken: string
}

/**
 * 登录并获取认证 token
 */
async function login(page: Page): Promise<string> {
  const response = await page.request.post(`${API_BASE_URL}/auth/login`, {
    data: TEST_USER
  })

  expect(response.ok()).toBeTruthy()
  const data = await response.json()
  expect(data.code).toBe(200)

  return data.data.accessToken
}

/**
 * 带认证的测试基础
 */
export const test = base.extend<AuthFixtures>({
  // 已认证的页面
  authenticatedPage: async ({ page }, use) => {
    // 登录获取 token
    const token = await login(page)

    // 存储 token 到 localStorage
    await page.addInitScript((token) => {
      localStorage.setItem('accessToken', token)
    }, token)

    // 访问首页
    await page.goto('/')

    await use(page)
  },

  // 认证 token
  authToken: async ({ page }, use) => {
    const token = await login(page)
    await use(token)
  }
})

export { expect, API_BASE_URL, TEST_USER }
