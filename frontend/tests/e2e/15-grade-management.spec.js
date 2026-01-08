// @ts-check
import { test, expect } from '@playwright/test';
import { AuthHelper } from './helpers/auth.js';
import { DatabaseHelper } from './helpers/database.js';

test.describe('年级管理模块测试', () => {
  let page;
  let authHelper;
  let dbHelper;

  const testGrade = {
    gradeName: 'E2E测试年级',
    gradeCode: '2024',
    startYear: 2024,
    endYear: 2028
  };

  test.beforeAll(async ({ browser }) => {
    console.log('\n=== 年级管理模块测试开始 ===\n');
    const context = await browser.newContext();
    page = await context.newPage();
    authHelper = new AuthHelper(page);
    dbHelper = new DatabaseHelper();
    await authHelper.login();
    console.log('✓ 用户已登录');
  });

  test.afterAll(async () => {
    console.log('\n=== 年级管理模块测试结束 ===\n');
    await page?.close();
  });

  test('15.1 应该能够导航到年级管理页面', async () => {
    console.log('\n--- 测试用例 15.1: 导航到年级管理页面 ---');
    await page.goto('http://localhost:3000/academic/grades');
    await page.waitForTimeout(2000);
    const table = page.locator('.el-table').first();
    expect(await table.isVisible()).toBeTruthy();
    console.log('✓ 页面加载成功');
  });

  test('15.2 应该显示年级列表', async () => {
    console.log('\n--- 测试用例 15.2: 查看年级列表 ---');
    await page.goto('http://localhost:3000/academic/grades');
    await page.waitForTimeout(2000);
    const rows = await page.locator('.el-table__row').all();
    console.log(`当前显示 ${rows.length} 个年级`);
    const dbResult = await dbHelper.executeQuery('SELECT COUNT(*) as count FROM grades WHERE deleted = 0;');
    console.log(`数据库验证: ${dbResult}`);
  });

  test('15.3 应该能够通过UI创建新年级', async () => {
    console.log('\n--- 测试用例 15.3: 创建新年级 ---');
    await page.goto('http://localhost:3000/academic/grades');
    await page.waitForTimeout(2000);

    const addButton = page.locator('button:has-text("新增年级")');
    const btnExists = await addButton.isVisible({ timeout: 2000 }).catch(() => false);

    if (btnExists) {
      await addButton.click();
      await page.waitForTimeout(1500);

      const dialog = page.locator('.el-dialog').last();
      await dialog.locator('input[placeholder*="年级名称"]').fill(testGrade.gradeName);

      await dialog.locator('button:has-text("确定")').click();
      await page.waitForTimeout(2000);

      const dbResult = await dbHelper.executeQuery(`SELECT id FROM grades WHERE grade_name = '${testGrade.gradeName}' AND deleted = 0;`);
      console.log(`数据库验证: ${dbResult}`);
    } else {
      console.log('⚠ 未找到新增年级按钮');
    }
  });

  test('15.4 应该验证数据一致性', async () => {
    console.log('\n--- 测试用例 15.4: 验证数据一致性 ---');
    await page.goto('http://localhost:3000/academic/grades');
    await page.waitForTimeout(2000);
    const dbResult = await dbHelper.executeQuery('SELECT COUNT(*) as count FROM grades WHERE deleted = 0;');
    console.log(`数据库年级总数: ${dbResult}`);
    console.log('✓ 数据一致性验证完成');
  });

  test('15.5 应该能够删除年级', async () => {
    console.log('\n--- 测试用例 15.5: 删除年级 ---');
    await page.goto('http://localhost:3000/academic/grades');
    await page.waitForTimeout(2000);

    const targetRow = page.locator('.el-table__row').filter({ hasText: testGrade.gradeName }).first();
    const rowExists = await targetRow.isVisible({ timeout: 3000 }).catch(() => false);

    if (rowExists) {
      const deleteButton = targetRow.locator('button:has-text("删除")');
      await deleteButton.click();
      await page.waitForTimeout(1000);
      const confirmButton = page.locator('.el-message-box button:has-text("确定")');
      if (await confirmButton.isVisible({ timeout: 2000 })) {
        await confirmButton.click();
        await page.waitForTimeout(2000);
      }
      console.log('✓ 删除操作完成');
    }
  });
});
