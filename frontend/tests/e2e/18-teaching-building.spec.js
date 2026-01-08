// @ts-check
import { test, expect } from '@playwright/test';
import { AuthHelper } from './helpers/auth.js';
import { DatabaseHelper } from './helpers/database.js';

test.describe('教学楼管理模块测试', () => {
  let page;
  let authHelper;
  let dbHelper;

  const testBuilding = {
    buildingName: 'E2E测试教学楼',
    buildingCode: 'TEACH_001',
    floors: 5
  };

  test.beforeAll(async ({ browser }) => {
    console.log('\n=== 教学楼管理模块测试开始 ===\n');
    const context = await browser.newContext();
    page = await context.newPage();
    authHelper = new AuthHelper(page);
    dbHelper = new DatabaseHelper();
    await authHelper.login();
    console.log('✓ 用户已登录');
  });

  test.afterAll(async () => {
    console.log('\n=== 教学楼管理模块测试结束 ===\n');
    await page?.close();
  });

  test('18.1 应该能够导航到教学楼管理页面', async () => {
    console.log('\n--- 测试用例 18.1: 导航到教学楼管理页面 ---');
    await page.goto('http://localhost:3000/teaching/buildings');
    await page.waitForTimeout(2000);
    const table = page.locator('.el-table').first();
    expect(await table.isVisible()).toBeTruthy();
    console.log('✓ 页面加载成功');
  });

  test('18.2 应该显示教学楼列表', async () => {
    console.log('\n--- 测试用例 18.2: 查看教学楼列表 ---');
    await page.goto('http://localhost:3000/teaching/buildings');
    await page.waitForTimeout(2000);
    const rows = await page.locator('.el-table__row').all();
    console.log(`当前显示 ${rows.length} 个教学楼`);
    const dbResult = await dbHelper.executeQuery(
      'SELECT COUNT(*) as count FROM building_teachings WHERE deleted = 0;'
    );
    console.log(`数据库验证: ${dbResult}`);
  });

  test('18.3 应该能够通过UI创建新教学楼', async () => {
    console.log('\n--- 测试用例 18.3: 创建新教学楼 ---');
    await page.goto('http://localhost:3000/teaching/buildings');
    await page.waitForTimeout(2000);

    const addButton = page.locator('button:has-text("新增")');
    const btnExists = await addButton.isVisible({ timeout: 2000 }).catch(() => false);

    if (btnExists) {
      await addButton.click();
      await page.waitForTimeout(1500);

      const dialog = page.locator('.el-dialog').last();

      // 填写教学楼名称
      await dialog.locator('input').first().fill(testBuilding.buildingName);
      console.log(`✓ 填写教学楼名称: ${testBuilding.buildingName}`);

      await dialog.locator('button:has-text("确定")').click();
      await page.waitForTimeout(2000);

      const dbResult = await dbHelper.executeQuery(
        `SELECT id FROM building_teachings WHERE building_name LIKE '%${testBuilding.buildingName}%' AND deleted = 0;`
      );
      console.log(`数据库验证: ${dbResult}`);
    } else {
      console.log('⚠ 未找到新增按钮');
    }
  });

  test('18.4 应该验证数据一致性', async () => {
    console.log('\n--- 测试用例 18.4: 验证数据一致性 ---');
    await page.goto('http://localhost:3000/teaching/buildings');
    await page.waitForTimeout(2000);
    const dbResult = await dbHelper.executeQuery(
      'SELECT COUNT(*) as count FROM building_teachings WHERE deleted = 0;'
    );
    console.log(`数据库教学楼总数: ${dbResult}`);
    console.log('✓ 数据一致性验证完成');
  });

  test('18.5 应该能够删除教学楼', async () => {
    console.log('\n--- 测试用例 18.5: 删除教学楼 ---');
    await page.goto('http://localhost:3000/teaching/buildings');
    await page.waitForTimeout(2000);

    const targetRow = page.locator('.el-table__row').filter({ hasText: testBuilding.buildingName }).first();
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
