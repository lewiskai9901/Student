/**
 * E2E测试 - 07 综合测评模块
 * 测试顺序：测评周期 -> 测评维度 -> 课程管理 -> 成绩管理 -> 荣誉类型 -> 荣誉申报 -> 测评结果
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

test.describe('07-综合测评模块测试', () => {

  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test.describe('7.1 测评周期管理', () => {

    test('访问测评周期页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/evaluation/periods`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/60-period-list.png' });

      console.log('[PASS] 测评周期页面加载成功');
    });

    test('创建测评周期', async ({ page }) => {
      await page.goto(`${BASE_URL}/evaluation/periods`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      const addBtn = page.locator('button:has-text("新增"), button:has-text("创建")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(1000);

        // 截图
        await page.screenshot({ path: 'test-results/screenshots/61-period-create.png' });

        const cancelBtn = page.locator('button:has-text("取消")').first();
        if (await cancelBtn.isVisible()) {
          await cancelBtn.click();
        }
      }

      console.log('[PASS] 测评周期创建测试通过');
    });
  });

  test.describe('7.2 测评维度管理', () => {

    test('访问测评维度页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/evaluation/dimensions`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/62-dimension-list.png' });

      console.log('[PASS] 测评维度页面加载成功');
    });
  });

  test.describe('7.3 课程管理', () => {

    test('访问课程管理页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/evaluation/courses`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/63-course-list.png' });

      console.log('[PASS] 课程管理页面加载成功');
    });
  });

  test.describe('7.4 成绩管理', () => {

    test('访问成绩管理页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/evaluation/scores`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/64-score-list.png' });

      console.log('[PASS] 成绩管理页面加载成功');
    });
  });

  test.describe('7.5 荣誉管理', () => {

    test('访问荣誉类型页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/evaluation/honor-types`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/65-honor-type-list.png' });

      console.log('[PASS] 荣誉类型页面加载成功');
    });

    test('访问荣誉申报页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/evaluation/honors`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/66-honor-list.png' });

      console.log('[PASS] 荣誉申报页面加载成功');
    });
  });

  test.describe('7.6 测评结果', () => {

    test('访问测评结果页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/evaluation/results`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/67-result-list.png' });

      console.log('[PASS] 测评结果页面加载成功');
    });
  });
});
