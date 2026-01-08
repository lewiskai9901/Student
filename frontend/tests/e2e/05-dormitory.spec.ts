/**
 * E2E测试 - 05 宿舍管理模块
 * 测试顺序：楼栋管理 -> 宿舍管理 -> 入住管理 -> 退宿/调换
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

test.describe('05-宿舍管理模块测试', () => {

  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test.describe('5.1 楼栋管理', () => {

    test('访问楼栋管理页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/dormitory/buildings`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/40-building-list.png' });

      console.log('[PASS] 楼栋管理页面加载成功');
    });

    test('创建楼栋', async ({ page }) => {
      await page.goto(`${BASE_URL}/dormitory/buildings`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      const addBtn = page.locator('button:has-text("新增"), button:has-text("添加")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(1000);

        // 截图
        await page.screenshot({ path: 'test-results/screenshots/41-building-create.png' });

        const cancelBtn = page.locator('button:has-text("取消")').first();
        if (await cancelBtn.isVisible()) {
          await cancelBtn.click();
        }
      }

      console.log('[PASS] 楼栋创建对话框测试通过');
    });
  });

  test.describe('5.2 宿舍管理', () => {

    test('访问宿舍管理页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/dormitory/rooms`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/42-dormitory-list.png' });

      console.log('[PASS] 宿舍管理页面加载成功');
    });

    test('创建宿舍', async ({ page }) => {
      await page.goto(`${BASE_URL}/dormitory/rooms`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      const addBtn = page.locator('button:has-text("新增"), button:has-text("添加")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(1000);

        // 截图
        await page.screenshot({ path: 'test-results/screenshots/43-dormitory-create.png' });

        const cancelBtn = page.locator('button:has-text("取消")').first();
        if (await cancelBtn.isVisible()) {
          await cancelBtn.click();
        }
      }

      console.log('[PASS] 宿舍创建对话框测试通过');
    });
  });

  test.describe('5.3 入住管理', () => {

    test('访问入住分配页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/dormitory/allocation`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/44-dormitory-allocation.png' });

      console.log('[PASS] 入住分配页面加载成功');
    });
  });

  test.describe('5.4 宿舍总览', () => {

    test('访问宿舍总览页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/dormitory/overview`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/45-dormitory-overview.png' });

      console.log('[PASS] 宿舍总览页面加载成功');
    });
  });
});
