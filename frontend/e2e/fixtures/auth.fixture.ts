import { test as base, expect, type Page } from '@playwright/test'

// API 基础 URL (后端)
const API_BASE_URL = 'http://localhost:8080/api'

// 测试账号
const TEST_USER = {
  username: 'admin',
  password: 'admin123',
}

interface SharedToken {
  accessToken: string
  refreshToken: string
}

interface AuthFixtures {
  /** worker-scoped: 每个 worker 只登录 1 次, 后续所有 test 共享. 消除 e2e 冷启动 race 与 beforeEach 登录开销. */
  sharedToken: SharedToken
  /** test-scoped: 自动注入 access_token / refresh_token 到 sessionStorage / localStorage, page 一打开即"已登录". */
  page: Page
  authenticatedPage: Page // 兼容旧 spec
  authToken: string // 兼容旧 spec
}

/** 通过 backend API 获取 admin token (API 直登, 不走 UI) */
async function loginViaApi(): Promise<SharedToken> {
  // 在 Node 端用 fetch (Playwright worker setup 拿不到 page.request)
  const resp = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(TEST_USER),
  })
  if (!resp.ok) {
    throw new Error(`登录失败: HTTP ${resp.status}`)
  }
  const env = await resp.json()
  if (env.code !== 200) {
    throw new Error(`登录失败: code=${env.code} msg=${env.message}`)
  }
  return {
    accessToken: env.data.accessToken,
    refreshToken: env.data.refreshToken,
  }
}

export const test = base.extend<AuthFixtures, { sharedToken: SharedToken }>({
  // worker-scoped: 整个 worker 进程只跑一次
  sharedToken: [
    async ({}, use) => {
      const tok = await loginViaApi()
      await use(tok)
    },
    { scope: 'worker' },
  ],

  // 覆盖默认 page fixture: 每个 test page 一打开自动注入 token
  // tokenStorage.ts 约定: access_token → sessionStorage; refresh_token → localStorage
  page: async ({ page, sharedToken }, use) => {
    await page.addInitScript(({ at, rt }) => {
      try {
        sessionStorage.setItem('access_token', at)
        localStorage.setItem('refresh_token', rt)
      } catch {
        // 某些 about:blank 阶段 sessionStorage 不可写, 忽略 — 后续路由会重新触发
      }
    }, { at: sharedToken.accessToken, rt: sharedToken.refreshToken })
    await use(page)
  },

  // 兼容: 旧 spec 用 authenticatedPage = 已登录页 (现在等价于 page)
  authenticatedPage: async ({ page }, use) => {
    await page.goto('/')
    await use(page)
  },

  // 兼容: 旧 spec 直接拿 token 调 backend
  authToken: async ({ sharedToken }, use) => {
    await use(sharedToken.accessToken)
  },
})

export { expect, API_BASE_URL, TEST_USER }
