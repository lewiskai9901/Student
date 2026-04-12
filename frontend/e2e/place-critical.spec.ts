import { test, expect, type Page } from '@playwright/test'

/**
 * Task 2.3: 场所管理关键流程 E2E 测试
 *
 * 覆盖场景：
 * 1. 场所管理页能加载（左侧场所树 + 右侧统计面板）
 * 2. 实体类型配置页（metadata schema editor）可访问
 * 3. /system/place-types 重定向到 /system/entity-types（commit 421ebf2c 新增重定向）
 *
 * 备注：登录模式沿用 auth-critical.spec.ts —— tailwind 原生 input + 移动/桌面双布局。
 */

// Helper: 以 admin 身份登录
async function loginAsAdmin(page: Page) {
  await page.goto('/login')
  await page.locator('input[placeholder="请输入账号"]').first().fill('admin')
  await page.locator('input[placeholder="请输入密码"]').first().fill('admin123')
  await page.locator('button[type="submit"]:has-text("登录")').first().click()
  await page.waitForURL(/\/dashboard/, { timeout: 10000 })
}

test.describe('Place management critical flows', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page)
  })

  test('place management page loads with tree + statistics', async ({ page }) => {
    await page.goto('/place/management')

    // Left sidebar tree
    await expect(page.locator('text=场所树').first()).toBeVisible({ timeout: 5000 })

    // Right statistics panel — shows totals
    await expect(page.locator('text=总数').first()).toBeVisible({ timeout: 5000 })
  })

  test('entity type config page accessible (metadata schema editor)', async ({ page }) => {
    await page.goto('/system/entity-types')
    // Page heading or main area should render
    await expect(page.locator('body')).toContainText(/类型|实体/, { timeout: 5000 })
  })

  test('place type config accessible via /system/place-types redirect', async ({ page }) => {
    // Should redirect to /system/entity-types (commit 421ebf2c added redirect)
    await page.goto('/system/place-types')
    await page.waitForURL(/\/system\/entity-types/, { timeout: 5000 })
  })
})
