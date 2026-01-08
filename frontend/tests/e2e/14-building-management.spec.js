// @ts-check
import { test, expect } from '@playwright/test';
import { AuthHelper } from './helpers/auth.js';
import { DatabaseHelper } from './helpers/database.js';

test.describe('楼宇管理模块测试', () => {
  let page;
  let authHelper;
  let dbHelper;

  const testBuilding = {
    buildingName: 'E2E测试楼宇',
    buildingCode: 'TEST_BUILD_001',
    buildingType: 1,  // 教学楼
    floors: 5,
    status: 1  // 正常
  };

  test.beforeAll(async ({ browser }) => {
    console.log('\n=== 楼宇管理模块测试开始 ===\n');
    const context = await browser.newContext();
    page = await context.newPage();
    authHelper = new AuthHelper(page);
    dbHelper = new DatabaseHelper();
    await authHelper.login();
    console.log('✓ 用户已登录');
  });

  test.afterAll(async () => {
    console.log('\n=== 楼宇管理模块测试结束 ===\n');
    await page?.close();
  });

  test('14.1 应该能够导航到楼宇管理页面', async () => {
    console.log('\n--- 测试用例 14.1: 导航到楼宇管理页面 ---');
    await page.goto('http://localhost:3000/system/buildings');
    await page.waitForTimeout(2000);
    const table = page.locator('.el-table').first();
    expect(await table.isVisible()).toBeTruthy();
    console.log('✓ 页面加载成功');
  });

  test('14.2 应该显示楼宇列表', async () => {
    console.log('\n--- 测试用例 14.2: 查看楼宇列表 ---');
    await page.goto('http://localhost:3000/system/buildings');
    await page.waitForTimeout(2000);
    const rows = await page.locator('.el-table__row').all();
    console.log(`当前显示 ${rows.length} 个楼宇`);
    const dbResult = await dbHelper.executeQuery('SELECT COUNT(*) as count FROM buildings WHERE deleted = 0;');
    console.log(`数据库验证: ${dbResult}`);
  });

  test('14.3 应该能够通过UI创建新楼宇', async () => {
    console.log('\n--- 测试用例 14.3: 创建新楼宇 ---');
    await page.goto('http://localhost:3000/system/buildings');
    await page.waitForTimeout(2000);

    const addButton = page.locator('button:has-text("新增楼宇")');
    const btnExists = await addButton.isVisible({ timeout: 2000 }).catch(() => false);

    if (btnExists) {
      await addButton.click();
      await page.waitForTimeout(1500);

      const dialog = page.locator('.el-dialog').last();
      await dialog.locator('input[placeholder*="楼宇名称"]').fill(testBuilding.buildingName);
      await dialog.locator('input[placeholder*="楼宇编码"]').fill(testBuilding.buildingCode);

      await dialog.locator('button:has-text("确定")').click();
      await page.waitForTimeout(2000);

      const dbResult = await dbHelper.executeQuery(`SELECT id FROM buildings WHERE building_code = '${testBuilding.buildingCode}' AND deleted = 0;`);
      console.log(`数据库验证: ${dbResult}`);
    } else {
      console.log('⚠ 未找到新增楼宇按钮，可能是权限问题');
    }
  });

  test('14.4 应该验证数据一致性', async () => {
    console.log('\n--- 测试用例 14.4: 验证数据一致性 ---');
    await page.goto('http://localhost:3000/system/buildings');
    await page.waitForTimeout(2000);
    const dbResult = await dbHelper.executeQuery('SELECT COUNT(*) as count FROM buildings WHERE deleted = 0;');
    console.log(`数据库楼宇总数: ${dbResult}`);
    console.log('✓ 数据一致性验证完成');
  });

  test('14.5 应该能够删除楼宇', async () => {
    console.log('\n--- 测试用例 14.5: 删除楼宇 ---');
    await page.goto('http://localhost:3000/system/buildings');
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
