import { test, expect, type Page } from '@playwright/test'

/**
 * Task 2.2: 组织管理关键流程 E2E 测试
 *
 * 覆盖场景：
 * 1. 组织树页面能加载并显示节点（早期审计曾误报"树点不动"）
 * 2. 点击组织节点 → 右侧面板显示详情（下级组织 / 成员 / 基本信息）
 * 3. 侧边栏不会把 /system/org-types 等原始路径直接当作菜单名渲染
 *    （回归 commit 421ebf2c：路由 meta.title 缺失导致菜单渲染为 path 字符串）
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

test.describe('Organization management critical flows', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page)
  })

  test('org tree page loads with tree data', async ({ page }) => {
    await page.goto('/organization/units')
    // 组织树标题应出现
    await expect(page.locator('text=组织树').first()).toBeVisible({ timeout: 5000 })
    // 至少一个组织节点（默认种子数据含"枣庄技师学院"）
    await expect(page.locator('text=枣庄技师学院').first()).toBeVisible({ timeout: 10000 })
  })

  test('clicking org node shows detail panel', async ({ page }) => {
    await page.goto('/organization/units')
    await expect(page.locator('text=枣庄技师学院').first()).toBeVisible({ timeout: 10000 })

    // 点击树节点
    await page.locator('text=枣庄技师学院').first().click()

    // 点击后右侧应显示详情 Tab（下级组织 / 成员 / 基本信息）
    await expect(
      page.locator('text=/下级组织|成员|基本信息/').first()
    ).toBeVisible({ timeout: 5000 })
  })

  test('sidebar does NOT show raw system paths (commit 421ebf2c regression)', async ({ page }) => {
    await page.goto('/dashboard')

    // 展开"系统管理"菜单
    const systemMenu = page.locator('text=系统管理').first()
    await systemMenu.click()
    await page.waitForTimeout(500) // 等待菜单展开动画

    // 以下原始路径字符串绝不能作为菜单标签出现
    // 注意: Playwright 的 text=/.../ 会被当正则, 需引号包起来强制字面匹配
    await expect(page.locator('text="/system/org-types"')).not.toBeVisible()
    await expect(page.locator('text="/system/place-types"')).not.toBeVisible()
    await expect(page.locator('text="/system/user-types"')).not.toBeVisible()
  })
})
