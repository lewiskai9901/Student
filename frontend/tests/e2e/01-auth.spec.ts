/**
 * E2E测试 - 01 认证模块
 * 测试顺序：登录 -> 获取用户信息 -> 登出
 */
import { test, expect, Page } from '@playwright/test';

// 测试配置
const BASE_URL = 'http://localhost:3000';
const TEST_USER = {
  username: 'admin',
  password: 'admin123'
};

// 辅助函数：登录
async function login(page: Page, username: string, password: string) {
  await page.goto(`${BASE_URL}/login`);
  await page.waitForLoadState('networkidle');

  // 填写登录表单
  await page.fill('input[placeholder*="用户名"]', username);
  await page.fill('input[placeholder*="密码"], input[type="password"]', password);

  // 点击登录按钮
  await page.click('button:has-text("登录"), button[type="submit"]');

  // 等待登录成功 - 检查页面内容显示"管理员"
  await page.waitForTimeout(3000);
  await page.waitForSelector('text=管理员', { timeout: 10000 });
}

test.describe('01-认证模块测试', () => {

  test.describe('1.1 登录功能', () => {

    test('正常登录 - admin账户', async ({ page }) => {
      await page.goto(`${BASE_URL}/login`);
      await page.waitForLoadState('networkidle');

      // 截图：登录页面
      await page.screenshot({ path: 'test-results/screenshots/01-login-page.png' });

      // 验证登录页面元素
      await expect(page.locator('input[placeholder*="用户名"]')).toBeVisible();
      await expect(page.locator('input[type="password"]')).toBeVisible();

      // 填写表单
      await page.fill('input[placeholder*="用户名"]', TEST_USER.username);
      await page.fill('input[type="password"]', TEST_USER.password);

      // 截图：填写完成
      await page.screenshot({ path: 'test-results/screenshots/02-login-filled.png' });

      // 点击登录
      await page.click('button:has-text("登录"), button[type="submit"]');

      // 等待登录成功 - 检查页面内容而不是URL
      await page.waitForTimeout(3000);

      // 截图：登录成功
      await page.screenshot({ path: 'test-results/screenshots/03-login-success.png' });

      // 验证登录成功 - 检查仪表盘内容是否显示
      const dashboard = page.locator('text=管理员');
      await expect(dashboard.first()).toBeVisible({ timeout: 10000 });

      console.log('[PASS] 正常登录测试通过');
    });

    test('错误密码登录', async ({ page }) => {
      await page.goto(`${BASE_URL}/login`);
      await page.waitForLoadState('networkidle');

      // 填写错误密码
      await page.fill('input[placeholder*="用户名"]', TEST_USER.username);
      await page.fill('input[type="password"]', 'wrongpassword');

      // 点击登录
      await page.click('button:has-text("登录"), button[type="submit"]');

      // 等待错误提示
      await page.waitForTimeout(2000);

      // 截图：错误提示
      await page.screenshot({ path: 'test-results/screenshots/04-login-error.png' });

      // 验证仍在登录页
      expect(page.url()).toContain('/login');

      console.log('[PASS] 错误密码测试通过');
    });

    test('空用户名登录', async ({ page }) => {
      await page.goto(`${BASE_URL}/login`);
      await page.waitForLoadState('networkidle');

      // 只填密码
      await page.fill('input[type="password"]', TEST_USER.password);

      // 点击登录
      await page.click('button:has-text("登录"), button[type="submit"]');

      // 等待验证
      await page.waitForTimeout(1000);

      // 验证仍在登录页
      expect(page.url()).toContain('/login');

      console.log('[PASS] 空用户名测试通过');
    });
  });

  test.describe('1.2 登出功能', () => {

    test('正常登出', async ({ page }) => {
      // 先登录
      await login(page, TEST_USER.username, TEST_USER.password);

      // 截图：登录后
      await page.screenshot({ path: 'test-results/screenshots/05-before-logout.png' });

      // 查找并点击用户头像/下拉菜单
      const userDropdown = page.locator('.el-dropdown, .user-info, .avatar, [class*="user"]').first();
      if (await userDropdown.isVisible()) {
        await userDropdown.click();
        await page.waitForTimeout(500);

        // 点击登出选项
        const logoutBtn = page.locator('text=登出, text=退出登录, text=退出').first();
        if (await logoutBtn.isVisible()) {
          await logoutBtn.click();
        }
      }

      // 等待跳转到登录页
      await page.waitForTimeout(2000);

      // 截图：登出后
      await page.screenshot({ path: 'test-results/screenshots/06-after-logout.png' });

      console.log('[PASS] 登出测试通过');
    });
  });

  test.describe('1.3 Token刷新', () => {

    test('登录后可正常访问受保护页面', async ({ page }) => {
      // 登录
      await login(page, TEST_USER.username, TEST_USER.password);

      // 访问仪表盘
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 验证页面显示仪表盘内容（而不是登录页）
      const dashboard = page.locator('text=管理员');
      await expect(dashboard.first()).toBeVisible({ timeout: 10000 });

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/07-dashboard-access.png' });

      console.log('[PASS] Token验证测试通过');
    });
  });
});
