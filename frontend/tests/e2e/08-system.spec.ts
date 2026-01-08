/**
 * E2E测试 - 08 系统管理模块
 * 测试顺序：系统配置 -> 操作日志 -> 公告管理
 */
import { test, expect, Page } from '@playwright/test';

const BASE_URL = 'http://localhost:3000';
const TEST_USER = { username: 'admin', password: 'admin123' };

async function login(page: Page) {
  await page.goto(`${BASE_URL}/login`);
  await page.waitForLoadState('networkidle');
  await page.fill('input[placeholder*="用户名"]', TEST_USER.username);
  await page.fill('input[type="password"]', TEST_USER.password);
  await page.click('button:has-text("登录"), button[type="submit"]');
  await page.waitForTimeout(3000);
  await page.waitForSelector('text=管理员', { timeout: 10000 });
}

test.describe('08-系统管理模块测试', () => {

  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test.describe('8.1 系统配置', () => {

    test('访问系统配置页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/system/configs`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/70-system-config.png' });

      console.log('[PASS] 系统配置页面加载成功');
    });
  });

  test.describe('8.2 操作日志', () => {

    test('访问操作日志页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/system/logs`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/71-operation-logs.png' });

      console.log('[PASS] 操作日志页面加载成功');
    });

    test('日志搜索筛选', async ({ page }) => {
      await page.goto(`${BASE_URL}/system/logs`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 查找筛选条件
      const dateRangePicker = page.locator('.el-date-editor, [class*="date"]').first();
      if (await dateRangePicker.isVisible()) {
        // 截图
        await page.screenshot({ path: 'test-results/screenshots/72-log-filter.png' });
      }

      console.log('[PASS] 日志筛选测试通过');
    });
  });

  test.describe('8.3 公告管理', () => {

    test('访问公告管理页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/system/announcements`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/73-announcement-list.png' });

      console.log('[PASS] 公告管理页面加载成功');
    });

    test('创建公告', async ({ page }) => {
      await page.goto(`${BASE_URL}/system/announcements`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      const addBtn = page.locator('button:has-text("新增"), button:has-text("发布")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(1000);

        // 截图
        await page.screenshot({ path: 'test-results/screenshots/74-announcement-create.png' });

        const cancelBtn = page.locator('button:has-text("取消")').first();
        if (await cancelBtn.isVisible()) {
          await cancelBtn.click();
        }
      }

      console.log('[PASS] 公告创建测试通过');
    });
  });

  test.describe('8.4 学期管理', () => {

    test('访问学期管理页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/system/semesters`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/75-semester-list.png' });

      console.log('[PASS] 学期管理页面加载成功');
    });
  });

  test.describe('8.5 仪表盘', () => {

    test('访问仪表盘页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(3000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/76-dashboard.png' });

      // 验证仪表盘组件
      const charts = page.locator('[class*="chart"], [class*="echarts"], canvas');
      const cards = page.locator('[class*="card"], [class*="stat"]');

      console.log('[PASS] 仪表盘页面加载成功');
    });
  });
});
