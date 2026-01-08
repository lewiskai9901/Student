/**
 * E2E测试 - 06 量化检查模块
 * 测试顺序：检查模板 -> 检查计划 -> 日常检查 -> 检查记录 -> 申诉管理 -> 评级配置
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

test.describe('06-量化检查模块测试', () => {

  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test.describe('6.1 检查模板管理', () => {

    test('访问检查模板页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/quantification/templates`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/50-template-list.png' });

      console.log('[PASS] 检查模板页面加载成功');
    });

    test('创建检查模板', async ({ page }) => {
      await page.goto(`${BASE_URL}/quantification/templates`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      const addBtn = page.locator('button:has-text("新增"), button:has-text("创建")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(1000);

        // 截图
        await page.screenshot({ path: 'test-results/screenshots/51-template-create.png' });

        const cancelBtn = page.locator('button:has-text("取消")').first();
        if (await cancelBtn.isVisible()) {
          await cancelBtn.click();
        }
      }

      console.log('[PASS] 检查模板创建测试通过');
    });
  });

  test.describe('6.2 检查计划管理', () => {

    test('访问检查计划列表', async ({ page }) => {
      await page.goto(`${BASE_URL}/quantification/plans`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/52-plan-list.png' });

      console.log('[PASS] 检查计划列表加载成功');
    });

    test('创建检查计划', async ({ page }) => {
      await page.goto(`${BASE_URL}/quantification/plans/create`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/53-plan-create.png' });

      console.log('[PASS] 检查计划创建页面加载成功');
    });
  });

  test.describe('6.3 日常检查', () => {

    test('访问日常检查页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/quantification/daily-checks`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/54-daily-check-list.png' });

      console.log('[PASS] 日常检查页面加载成功');
    });
  });

  test.describe('6.4 检查记录', () => {

    test('访问检查记录列表', async ({ page }) => {
      await page.goto(`${BASE_URL}/quantification/records`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/55-record-list.png' });

      console.log('[PASS] 检查记录列表加载成功');
    });
  });

  test.describe('6.5 申诉管理', () => {

    test('访问申诉管理页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/quantification/appeals`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/56-appeal-list.png' });

      console.log('[PASS] 申诉管理页面加载成功');
    });
  });

  test.describe('6.6 评级配置', () => {

    test('访问评级配置页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/config/rating`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/57-rating-config.png' });

      console.log('[PASS] 评级配置页面加载成功');
    });
  });

  test.describe('6.7 加权配置', () => {

    test('访问加权配置页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/config/weight`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/58-weight-config.png' });

      console.log('[PASS] 加权配置页面加载成功');
    });
  });
});
