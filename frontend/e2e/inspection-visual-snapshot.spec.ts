import { test } from '@playwright/test'

/**
 * UI 统一化战役验证 — 4 个核心页面快照
 *
 * 不做 assertion, 只截图给人眼审查 token 化是否到位.
 */

async function login(page) {
  await page.goto('/login')
  await page.locator('input[placeholder="请输入账号"]').first().fill('admin')
  await page.locator('input[placeholder="请输入密码"]').first().fill('admin123')
  await page.locator('button[type="submit"]:has-text("登录")').first().click()
  await page.waitForFunction(() => !location.pathname.startsWith('/login'), null, { timeout: 30000 })
}

test.describe.configure({ retries: 0, timeout: 60000 })

test('snapshot: 项目列表页', async ({ page }) => {
  await login(page)
  await page.goto('/inspection/projects')
  await page.waitForSelector('h1:has-text("检查项目")', { timeout: 10000 })
  await page.waitForTimeout(800)
  await page.screenshot({ path: 'test-results/snap-1-project-list.png', fullPage: true })
})

test('snapshot: 项目详情页', async ({ page }) => {
  await login(page)
  await page.goto('/inspection/projects/1')
  await page.waitForSelector('.pdv-tab', { timeout: 10000 })
  await page.waitForTimeout(800)
  await page.screenshot({ path: 'test-results/snap-2-project-detail.png', fullPage: true })
})

test('snapshot: 检查配置中心', async ({ page }) => {
  await login(page)
  await page.goto('/inspection/config')
  await page.waitForSelector('h1', { timeout: 10000 })
  await page.waitForTimeout(800)
  await page.screenshot({ path: 'test-results/snap-3-config.png', fullPage: true })
})

test('snapshot: 评分方案列表', async ({ page }) => {
  await login(page)
  await page.goto('/inspection/scoring-profiles')
  await page.waitForSelector('h1:has-text("评分方案")', { timeout: 10000 })
  await page.waitForTimeout(800)
  await page.screenshot({ path: 'test-results/snap-4-scoring-profiles.png', fullPage: true })
})

test('snapshot: 项目看板视图', async ({ page }) => {
  await login(page)
  await page.goto('/inspection/projects?view=kanban')
  await page.waitForSelector('.bucket-name', { timeout: 10000 })
  await page.waitForTimeout(800)
  await page.screenshot({ path: 'test-results/snap-5-project-kanban.png', fullPage: true })
})

test('snapshot: 项目时间轴视图', async ({ page }) => {
  await login(page)
  await page.goto('/inspection/projects?view=timeline')
  await page.waitForSelector('.tl-grid, .empty', { timeout: 10000 })
  await page.waitForTimeout(800)
  await page.screenshot({ path: 'test-results/snap-6-project-timeline.png', fullPage: true })
})
