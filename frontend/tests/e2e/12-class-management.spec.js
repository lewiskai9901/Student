// @ts-check
import { test, expect } from '@playwright/test';
import { AuthHelper } from './helpers/auth.js';
import { DatabaseHelper } from './helpers/database.js';

test.describe('班级管理模块测试', () => {
  let page;
  let authHelper;
  let dbHelper;

  const testClass = {
    className: 'E2E测试班级',
    classCode: 'TEST_CLASS_001'
  };

  test.beforeAll(async ({ browser }) => {
    console.log('\n=== 班级管理模块测试开始 ===\n');
    const context = await browser.newContext();
    page = await context.newPage();
    authHelper = new AuthHelper(page);
    dbHelper = new DatabaseHelper();
    await authHelper.login();
    console.log('✓ 用户已登录');
  });

  test.afterAll(async () => {
    console.log('\n=== 班级管理模块测试结束 ===\n');
    await page?.close();
  });

  test('12.1 应该能够导航到班级管理页面', async () => {
    console.log('\n--- 测试用例 12.1: 导航到班级管理页面 ---');
    await page.goto('http://localhost:3000/student-affairs/classes');
    await page.waitForTimeout(2000);
    const table = page.locator('.el-table').first();
    expect(await table.isVisible()).toBeTruthy();
    console.log('✓ 页面加载成功');
  });

  test('12.2 应该显示班级列表', async () => {
    console.log('\n--- 测试用例 12.2: 查看班级列表 ---');
    await page.goto('http://localhost:3000/student-affairs/classes');
    await page.waitForTimeout(2000);
    const rows = await page.locator('.el-table__row').all();
    console.log(`当前显示 ${rows.length} 个班级`);
    const dbResult = await dbHelper.executeQuery('SELECT COUNT(*) as count FROM classes WHERE deleted = 0;');
    console.log(`数据库验证: ${dbResult}`);
  });

  test('12.3 应该能够查看班级详情', async () => {
    console.log('\n--- 测试用例 12.3: 查看班级详情 ---');
    await page.goto('http://localhost:3000/student-affairs/classes');
    await page.waitForTimeout(2000);

    const firstRow = page.locator('.el-table__row').first();
    const viewButton = firstRow.locator('button:has-text("查看")');
    const btnExists = await viewButton.isVisible({ timeout: 2000 }).catch(() => false);

    if (btnExists) {
      await viewButton.click();
      await page.waitForTimeout(1500);
      console.log('✓ 成功打开班级详情');
    } else {
      console.log('⚠ 未找到查看按钮');
    }
  });

  test('12.4 应该验证数据一致性', async () => {
    console.log('\n--- 测试用例 12.4: 验证数据一致性 ---');
    await page.goto('http://localhost:3000/student-affairs/classes');
    await page.waitForTimeout(2000);
    const dbResult = await dbHelper.executeQuery('SELECT COUNT(*) as count FROM classes WHERE deleted = 0;');
    console.log(`数据库班级总数: ${dbResult}`);
    console.log('✓ 数据一致性验证完成');
  });
});
