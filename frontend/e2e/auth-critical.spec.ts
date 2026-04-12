import { test, expect } from '@playwright/test'

/**
 * Task 2.1: 关键认证流程 E2E 测试
 *
 * 覆盖场景（UI 层面，补充 auth.spec.ts 中 API 层面的测试）：
 * 1. 管理员登录成功 → 跳转到 /dashboard 且侧边栏加载完成
 * 2. 错误凭据 → 显示错误提示且仍停留在 /login
 * 3. 登录 → 点击用户菜单 → 退出登录 → 返回 /login
 *
 * 备注：登录页使用 tailwind 原生 input（非 el-input），表单存在移动端 + 桌面端
 *       两份（sm:hidden / hidden sm:block），因此选择器需加 .first() 或可见过滤。
 */
test.describe('Critical auth flows', () => {
  test('admin login + reach dashboard', async ({ page }) => {
    await page.goto('/login')

    // 登录页有两套布局（移动/桌面），取第一个可见的 input
    await page.locator('input[placeholder="请输入账号"]').first().fill('admin')
    await page.locator('input[placeholder="请输入密码"]').first().fill('admin123')
    await page.locator('button[type="submit"]:has-text("登录")').first().click()

    // 登录成功后应跳转到 /dashboard
    await page.waitForURL(/\/dashboard/, { timeout: 10000 })

    // 验证侧边栏加载（组织管理为 DDD 导航的核心一级菜单）
    await expect(page.locator('text=组织管理').first()).toBeVisible({ timeout: 5000 })
  })

  test('invalid credentials should show error', async ({ page }) => {
    await page.goto('/login')
    await page.locator('input[placeholder="请输入账号"]').first().fill('wronguser')
    await page.locator('input[placeholder="请输入密码"]').first().fill('wrongpass')
    await page.locator('button[type="submit"]:has-text("登录")').first().click()

    // Element Plus 错误提示应出现
    await expect(
      page.locator('.el-message--error, .el-message.is-error').first()
    ).toBeVisible({ timeout: 5000 })

    // 仍停留在登录页
    await expect(page).toHaveURL(/\/login/)
  })

  test('logout returns to login page', async ({ page }) => {
    // 先登录
    await page.goto('/login')
    await page.locator('input[placeholder="请输入账号"]').first().fill('admin')
    await page.locator('input[placeholder="请输入密码"]').first().fill('admin123')
    await page.locator('button[type="submit"]:has-text("登录")').first().click()
    await page.waitForURL(/\/dashboard/, { timeout: 10000 })

    // 打开右上角用户下拉菜单（触发器显示"系统管理员"小字）
    await page.locator('text=系统管理员').first().click()

    // 点击"退出登录"按钮
    await page.locator('button:has-text("退出登录")').first().click()

    // 处理 ElMessageBox 确认弹窗（"确认退出登录吗？"）
    const confirmBtn = page.locator('.el-message-box .el-button--primary')
    if (await confirmBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
      await confirmBtn.click()
    }

    // 跳转回登录页
    await page.waitForURL(/\/login/, { timeout: 5000 })
  })
})
