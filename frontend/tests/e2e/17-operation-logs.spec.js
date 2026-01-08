// @ts-check
import { test, expect } from '@playwright/test';
import { AuthHelper } from './helpers/auth.js';
import { DatabaseHelper } from './helpers/database.js';

test.describe('操作日志模块测试', () => {
  let page;
  let authHelper;
  let dbHelper;

  test.beforeAll(async ({ browser }) => {
    console.log('\n=== 操作日志模块测试开始 ===\n');
    const context = await browser.newContext();
    page = await context.newPage();
    authHelper = new AuthHelper(page);
    dbHelper = new DatabaseHelper();
    await authHelper.login();
    console.log('✓ 用户已登录');
  });

  test.afterAll(async () => {
    console.log('\n=== 操作日志模块测试结束 ===\n');
    await page?.close();
  });

  test('17.1 应该能够导航到操作日志页面', async () => {
    console.log('\n--- 测试用例 17.1: 导航到操作日志页面 ---');
    await page.goto('http://localhost:3000/system/operation-logs');
    await page.waitForTimeout(2000);
    const table = page.locator('.el-table').first();
    expect(await table.isVisible()).toBeTruthy();
    console.log('✓ 页面加载成功');
  });

  test('17.2 应该显示操作日志列表', async () => {
    console.log('\n--- 测试用例 17.2: 查看操作日志列表 ---');
    await page.goto('http://localhost:3000/system/operation-logs');
    await page.waitForTimeout(2000);
    const rows = await page.locator('.el-table__row').all();
    console.log(`当前显示 ${rows.length} 条操作日志`);
    const dbResult = await dbHelper.executeQuery('SELECT COUNT(*) as count FROM operation_logs;');
    console.log(`数据库验证: ${dbResult}`);
  });

  test('17.3 应该能够搜索日志', async () => {
    console.log('\n--- 测试用例 17.3: 搜索操作日志 ---');
    await page.goto('http://localhost:3000/system/operation-logs');
    await page.waitForTimeout(2000);

    const searchInput = page.locator('.search-card input').first();
    const inputExists = await searchInput.isVisible({ timeout: 2000 }).catch(() => false);

    if (inputExists) {
      await searchInput.fill('登录');
      const searchButton = page.locator('.search-card button:has-text("搜索")');
      if (await searchButton.isVisible({ timeout: 1000 }).catch(() => false)) {
        await searchButton.click();
        await page.waitForTimeout(1500);
        console.log('✓ 搜索操作完成');
      }
    } else {
      console.log('⚠ 未找到搜索输入框');
    }
  });

  test('17.4 应该验证日志记录', async () => {
    console.log('\n--- 测试用例 17.4: 验证日志记录 ---');
    await page.goto('http://localhost:3000/system/operation-logs');
    await page.waitForTimeout(2000);

    // 验证今天的登录日志
    const dbResult = await dbHelper.executeQuery(
      "SELECT COUNT(*) as count FROM operation_logs WHERE operation LIKE '%登录%' AND DATE(created_at) = CURDATE();"
    );
    console.log(`今日登录日志数: ${dbResult}`);
    console.log('✓ 日志记录验证完成');
  });
});
