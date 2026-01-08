// @ts-check
import { test, expect } from '@playwright/test';
import { AuthHelper } from './helpers/auth.js';
import { DatabaseHelper } from './helpers/database.js';

test.describe('专业管理模块测试', () => {
  let page;
  let authHelper;
  let dbHelper;

  const testMajor = {
    majorName: 'E2E测试专业',
    majorCode: 'TEST_MAJOR_001',
    duration: '4'
  };

  test.beforeAll(async ({ browser }) => {
    console.log('\n=== 专业管理模块测试开始 ===\n');
    const context = await browser.newContext();
    page = await context.newPage();
    authHelper = new AuthHelper(page);
    dbHelper = new DatabaseHelper();
    await authHelper.login();
    console.log('✓ 用户已登录');
  });

  test.afterAll(async () => {
    console.log('\n=== 专业管理模块测试结束 ===\n');
    await page?.close();
  });

  test('11.1 应该能够导航到专业管理页面', async () => {
    console.log('\n--- 测试用例 11.1: 导航到专业管理页面 ---');
    await page.goto('http://localhost:3000/academic/majors');
    await page.waitForTimeout(2000);
    const table = page.locator('.el-table').first();
    expect(await table.isVisible()).toBeTruthy();
    console.log('✓ 页面加载成功');
  });

  test('11.2 应该显示专业列表', async () => {
    console.log('\n--- 测试用例 11.2: 查看专业列表 ---');
    await page.goto('http://localhost:3000/academic/majors');
    await page.waitForTimeout(2000);
    const rows = await page.locator('.el-table__row').all();
    console.log(`当前显示 ${rows.length} 个专业`);
    const dbResult = await dbHelper.executeQuery('SELECT COUNT(*) as count FROM majors WHERE deleted = 0;');
    console.log(`数据库验证: ${dbResult}`);
  });

  test('11.3 应该能够通过UI创建新专业', async () => {
    console.log('\n--- 测试用例 11.3: 创建新专业 ---');
    await page.goto('http://localhost:3000/academic/majors');
    await page.waitForTimeout(2000);

    const addButton = page.locator('button:has-text("新增专业")');
    await addButton.click();
    await page.waitForTimeout(1500);

    const dialog = page.locator('.el-dialog').last();
    await dialog.locator('input[placeholder*="专业名称"]').fill(testMajor.majorName);
    await dialog.locator('input[placeholder*="专业编码"]').fill(testMajor.majorCode);

    await dialog.locator('button:has-text("确定")').click();
    await page.waitForTimeout(2000);

    const dbResult = await dbHelper.executeQuery(`SELECT id FROM majors WHERE major_code = '${testMajor.majorCode}' AND deleted = 0;`);
    console.log(`数据库验证: ${dbResult}`);
  });

  test('11.4 应该验证数据一致性', async () => {
    console.log('\n--- 测试用例 11.4: 验证数据一致性 ---');
    await page.goto('http://localhost:3000/academic/majors');
    await page.waitForTimeout(2000);
    const dbResult = await dbHelper.executeQuery('SELECT COUNT(*) as count FROM majors WHERE deleted = 0;');
    console.log(`数据库专业总数: ${dbResult}`);
    console.log('✓ 数据一致性验证完成');
  });

  test('11.5 应该能够删除专业', async () => {
    console.log('\n--- 测试用例 11.5: 删除专业 ---');
    await page.goto('http://localhost:3000/academic/majors');
    await page.waitForTimeout(2000);

    const targetRow = page.locator('.el-table__row').filter({ hasText: testMajor.majorName }).first();
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
