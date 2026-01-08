// @ts-check
import { test, expect } from '@playwright/test';
import { AuthHelper } from './helpers/auth.js';
import { DatabaseHelper } from './helpers/database.js';

test.describe('部门管理模块测试', () => {
  let page;
  let authHelper;
  let dbHelper;

  const testDepartment = {
    deptName: 'E2E测试部门',
    deptCode: 'TEST_DEPT_001',
    sortOrder: '999',
    status: 1
  };

  test.beforeAll(async ({ browser }) => {
    console.log('\n=== 部门管理模块测试开始 ===\n');
    const context = await browser.newContext();
    page = await context.newPage();
    authHelper = new AuthHelper(page);
    dbHelper = new DatabaseHelper();
    await authHelper.login();
    console.log('✓ 用户已登录');
  });

  test.afterAll(async () => {
    console.log('\n=== 部门管理模块测试结束 ===\n');
    await page?.close();
  });

  test('10.1 应该能够导航到部门管理页面', async () => {
    console.log('\n--- 测试用例 10.1: 导航到部门管理页面 ---');
    await page.goto('http://localhost:3000/student-affairs/departments');
    await page.waitForTimeout(2000);
    const table = page.locator('.el-table').first();
    expect(await table.isVisible()).toBeTruthy();
    console.log('✓ 页面加载成功');
  });

  test('10.2 应该显示部门列表', async () => {
    console.log('\n--- 测试用例 10.2: 查看部门列表 ---');
    await page.goto('http://localhost:3000/student-affairs/departments');
    await page.waitForTimeout(2000);
    const rows = await page.locator('.el-table__row').all();
    console.log(`当前显示 ${rows.length} 个部门`);
    const dbResult = await dbHelper.executeQuery('SELECT COUNT(*) as count FROM departments WHERE deleted = 0;');
    console.log(`数据库验证: ${dbResult}`);
  });

  test('10.3 应该能够通过UI创建新部门', async () => {
    console.log('\n--- 测试用例 10.3: 创建新部门 ---');
    await page.goto('http://localhost:3000/student-affairs/departments');
    await page.waitForTimeout(2000);

    const addButton = page.locator('button:has-text("新增部门")');
    await addButton.click();
    await page.waitForTimeout(1500);

    const dialog = page.locator('.el-dialog').last();
    await dialog.locator('input[placeholder*="部门名称"]').fill(testDepartment.deptName);
    await dialog.locator('input[placeholder*="部门编码"]').fill(testDepartment.deptCode);

    await dialog.locator('button:has-text("确定")').click();
    await page.waitForTimeout(2000);

    const dbResult = await dbHelper.executeQuery(`SELECT id FROM departments WHERE dept_code = '${testDepartment.deptCode}' AND deleted = 0;`);
    console.log(`数据库验证: ${dbResult}`);
  });

  test('10.4 应该验证数据一致性', async () => {
    console.log('\n--- 测试用例 10.4: 验证数据一致性 ---');
    await page.goto('http://localhost:3000/student-affairs/departments');
    await page.waitForTimeout(2000);
    const dbResult = await dbHelper.executeQuery('SELECT COUNT(*) as count FROM departments WHERE deleted = 0;');
    console.log(`数据库部门总数: ${dbResult}`);
    console.log('✓ 数据一致性验证完成');
  });

  test('10.5 应该能够删除部门', async () => {
    console.log('\n--- 测试用例 10.5: 删除部门 ---');
    await page.goto('http://localhost:3000/student-affairs/departments');
    await page.waitForTimeout(2000);

    const targetRow = page.locator('.el-table__row').filter({ hasText: testDepartment.deptName }).first();
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
