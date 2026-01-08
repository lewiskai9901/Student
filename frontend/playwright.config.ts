import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright 配置文件
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  // 测试目录
  testDir: './e2e',

  // 测试文件匹配模式
  testMatch: '**/*.spec.ts',

  // 全局超时时间
  timeout: 30 * 1000,

  // 期望超时时间
  expect: {
    timeout: 5000
  },

  // 完全并行运行测试
  fullyParallel: true,

  // 禁止在 CI 中只运行失败的测试
  forbidOnly: !!process.env.CI,

  // CI 中重试失败的测试
  retries: process.env.CI ? 2 : 0,

  // 并行工作进程数
  workers: process.env.CI ? 1 : undefined,

  // 报告器配置
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['list']
  ],

  // 全局配置
  use: {
    // 基础 URL
    baseURL: 'http://localhost:3000',

    // API 基础 URL
    extraHTTPHeaders: {
      'Content-Type': 'application/json'
    },

    // 收集失败测试的追踪
    trace: 'on-first-retry',

    // 截图配置
    screenshot: 'only-on-failure',

    // 视频录制
    video: 'on-first-retry'
  },

  // 项目配置 - 不同浏览器
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ],

  // 本地开发时启动前端服务
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120 * 1000
  }
})
