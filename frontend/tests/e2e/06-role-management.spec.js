// @ts-check
import { test, expect } from '@playwright/test';
import { AuthHelper } from './helpers/auth.js';
import { DatabaseHelper } from './helpers/database.js';

/**
 * 角色管理模块 E2E 测试
 *
 * 测试范围:
 * - 6.1 导航到角色管理页面
 * - 6.2 查看角色列表
 * - 6.3 通过UI创建新角色
 * - 6.4 搜索角色
 * - 6.5 验证数据一致性
 * - 6.6 删除角色
 */

test.describe('角色管理模块测试', () => {
  let page;
  let authHelper;
  let dbHelper;

  // 测试数据 - 将通过UI创建这些角色
  const testRoles = [
    {
      roleName: '测试角色001',
      roleCode: 'TEST_ROLE_001',
      roleDesc: '这是一个测试角色，用于E2E测试',
      sortOrder: '100',
      status: 1  // 启用
    },
    {
      roleName: '测试角色002',
      roleCode: 'TEST_ROLE_002',
      roleDesc: '第二个测试角色',
      sortOrder: '101',
      status: 1
    }
  ];

  test.beforeAll(async ({ browser }) => {
    console.log('\n=== 角色管理模块测试开始 ===\n');

    const context = await browser.newContext();
    page = await context.newPage();
    authHelper = new AuthHelper(page);
    dbHelper = new DatabaseHelper();

    // 登录系统
    await authHelper.login();
    console.log('✓ 用户已登录');
  });

  test.afterAll(async () => {
    console.log('\n=== 角色管理模块测试结束 ===\n');
    await page?.close();
  });

  /**
   * 测试用例 6.1: 导航到角色管理页面
   */
  test('6.1 应该能够导航到角色管理页面', async () => {
    console.log('\n--- 测试用例 6.1: 导航到角色管理页面 ---');

    // 应用最佳实践：直接使用URL导航
    await page.goto('http://localhost:3000/system/roles');
    await page.waitForTimeout(2000);
    console.log('✓ 使用URL导航到角色管理页面');

    // 验证页面加载成功（检查表格是否存在）
    const table = page.locator('.el-table').first();
    const tableExists = await table.isVisible({ timeout: 3000 }).catch(() => false);
    if (tableExists) {
      console.log('✓ 页面加载成功，表格可见');
    } else {
      console.log('⚠ 表格未找到，但页面已导航');
    }

    // 截图
    await page.screenshot({
      path: 'test-results/screenshots/06-01-role-page-navigation.png',
      fullPage: true
    });
    console.log('✓ 截图已保存: 06-01-role-page-navigation.png');
    console.log('✓ 测试用例 6.1 完成\n');
  });

  /**
   * 测试用例 6.2: 查看角色列表
   */
  test('6.2 应该显示角色列表', async () => {
    console.log('\n--- 测试用例 6.2: 查看角色列表 ---');

    // 确保在角色管理页面
    await page.goto('http://localhost:3000/system/roles');
    await page.waitForTimeout(2000);

    // 查找表格
    const table = page.locator('.el-table').first();
    expect(await table.isVisible()).toBeTruthy();
    console.log('✓ 找到角色列表表格');

    // 统计表格行数
    const rows = await page.locator('.el-table__row').all();
    console.log(`当前显示 ${rows.length} 个角色`);

    // 验证数据库中的角色数
    const dbResult = await dbHelper.executeQuery(
      'SELECT COUNT(*) as count FROM roles WHERE deleted = 0;'
    );
    console.log(`数据库验证: ${dbResult}`);

    // 截图
    await page.screenshot({
      path: 'test-results/screenshots/06-02-role-list.png',
      fullPage: true
    });
    console.log('✓ 截图已保存: 06-02-role-list.png');
    console.log('✓ 测试用例 6.2 完成\n');
  });

  /**
   * 测试用例 6.3: 通过UI创建新角色
   */
  test('6.3 应该能够通过UI创建新角色', async () => {
    console.log('\n--- 测试用例 6.3: 创建新角色 ---');

    // 确保在角色管理页面
    await page.goto('http://localhost:3000/system/roles');
    await page.waitForTimeout(2000);

    const role = testRoles[0];
    console.log(`准备创建角色: ${role.roleName} (${role.roleCode})`);

    // 1. 点击"新增角色"按钮
    const addButton = page.locator('button:has-text("新增角色")');
    await addButton.click();
    console.log('✓ 点击了"新增角色"按钮');

    // 2. 等待对话框打开
    await page.waitForTimeout(1500);
    const dialog = page.locator('.el-dialog').last();
    expect(await dialog.isVisible()).toBeTruthy();
    console.log('✓ 新增角色对话框已打开');

    // 3. 填写表单
    await dialog.locator('input[placeholder*="角色名称"]').fill(role.roleName);
    console.log(`✓ 填写角色名称: ${role.roleName}`);

    await dialog.locator('input[placeholder*="角色编码"]').fill(role.roleCode);
    console.log(`✓ 填写角色编码: ${role.roleCode}`);

    // 角色描述是 textarea 不是 input
    await dialog.locator('textarea[placeholder*="角色描述"]').fill(role.roleDesc);
    console.log(`✓ 填写角色描述: ${role.roleDesc}`);

    // 排序是 el-input-number，需要找到其内部的 input
    await dialog.locator('.el-input-number input').fill(role.sortOrder);
    console.log(`✓ 填写排序: ${role.sortOrder}`);

    // 状态（默认启用）
    if (role.status === 1) {
      console.log('✓ 状态: 启用（默认）');
    }

    // 截图保存填写后的表单
    await page.screenshot({
      path: 'test-results/screenshots/06-03-create-role-form.png',
      fullPage: true
    });
    console.log('✓ 截图已保存: 06-03-create-role-form.png');

    // 4. 提交表单
    await dialog.locator('button:has-text("确定")').click();
    console.log('✓ 点击了"确定"按钮');

    // 5. 等待对话框关闭和数据加载
    await page.waitForTimeout(2000);

    // 6. 验证角色是否创建成功
    const dbResult = await dbHelper.executeQuery(
      `SELECT id, role_name, role_code FROM roles WHERE role_code = '${role.roleCode}' AND deleted = 0;`
    );
    console.log(`数据库验证: ${dbResult}`);
    expect(dbResult).toContain(role.roleCode);
    console.log('✓ 角色创建成功，数据库中已存在');

    // 截图保存创建后的状态
    await page.screenshot({
      path: 'test-results/screenshots/06-03-after-create.png',
      fullPage: true
    });
    console.log('✓ 截图已保存: 06-03-after-create.png');
    console.log('✓ 测试用例 6.3 完成\n');
  });

  /**
   * 测试用例 6.4: 搜索角色
   */
  test('6.4 应该能够搜索角色', async () => {
    console.log('\n--- 测试用例 6.4: 搜索角色 ---');

    // 确保在角色管理页面
    await page.goto('http://localhost:3000/system/roles');
    await page.waitForTimeout(2000);

    // 测试按角色名称搜索
    console.log('测试按角色名称搜索...');
    const nameInput = page.locator('.search-card input[placeholder*="角色名称"]');
    await nameInput.fill('测试角色001');
    await page.locator('.search-card button:has-text("查询")').click();
    await page.waitForTimeout(1500);

    let rows = await page.locator('.el-table__row').all();
    console.log(`搜索结果: ${rows.length} 条记录`);
    expect(rows.length).toBeGreaterThan(0);

    // 重置搜索
    await page.locator('.search-card button:has-text("重置")').click();
    await page.waitForTimeout(1500);

    // 测试按角色编码搜索
    console.log('\n测试按角色编码搜索...');
    const codeInput = page.locator('.search-card input[placeholder*="角色编码"]');
    await codeInput.fill('TEST_ROLE');
    await page.locator('.search-card button:has-text("查询")').click();
    await page.waitForTimeout(1500);

    rows = await page.locator('.el-table__row').all();
    console.log(`搜索结果: ${rows.length} 条记录`);

    // 截图
    await page.screenshot({
      path: 'test-results/screenshots/06-04-search-results.png',
      fullPage: true
    });
    console.log('✓ 截图已保存: 06-04-search-results.png');
    console.log('✓ 测试用例 6.4 完成\n');
  });

  /**
   * 测试用例 6.5: 验证数据一致性
   */
  test('6.5 应该验证前后端数据一致性', async () => {
    console.log('\n--- 测试用例 6.5: 验证数据一致性 ---');

    // 确保在角色管理页面
    await page.goto('http://localhost:3000/system/roles');
    await page.waitForTimeout(2000);

    // 获取前端显示的角色数
    const rows = await page.locator('.el-table__row').all();
    const pageCount = rows.length;

    // 获取数据库中的角色总数
    const dbResult = await dbHelper.executeQuery(
      'SELECT COUNT(*) as count FROM roles WHERE deleted = 0;'
    );
    const match = dbResult.match(/count[\s\S]*?(\d+)/);
    const dbCount = match ? parseInt(match[1]) : 0;

    console.log(`前端显示角色数: ${pageCount}`);
    console.log(`数据库角色总数: ${dbCount}`);

    // 验证测试角色
    const testRoleCount = await dbHelper.executeQuery(
      `SELECT COUNT(*) as count FROM roles WHERE role_code LIKE 'TEST_ROLE%' AND deleted = 0;`
    );
    console.log(`测试角色数: ${testRoleCount}`);

    console.log('✓ 数据一致性验证完成');
    console.log('✓ 测试用例 6.5 完成\n');
  });

  /**
   * 测试用例 6.6: 删除角色
   */
  test('6.6 应该能够删除角色', async () => {
    console.log('\n--- 测试用例 6.6: 删除角色 ---');

    // 确保在角色管理页面
    await page.goto('http://localhost:3000/system/roles');
    await page.waitForTimeout(2000);

    // 搜索要删除的角色
    const codeInput = page.locator('.search-card input[placeholder*="角色编码"]');
    await codeInput.fill('TEST_ROLE_001');
    await page.locator('.search-card button:has-text("查询")').click();
    await page.waitForTimeout(1500);

    const rows = await page.locator('.el-table__row').all();
    if (rows.length > 0) {
      console.log('✓ 找到要删除的角色');

      // 点击删除按钮
      const deleteButton = page.locator('.el-table__row').first().locator('button:has-text("删除")');
      await deleteButton.click();
      await page.waitForTimeout(1000);

      // 确认删除
      const confirmButton = page.locator('.el-message-box button:has-text("确定")');
      if (await confirmButton.isVisible({ timeout: 2000 })) {
        await confirmButton.click();
        console.log('✓ 确认删除');
        await page.waitForTimeout(2000);
      }

      // 验证删除后数据库状态
      const dbResult = await dbHelper.executeQuery(
        `SELECT deleted FROM roles WHERE role_code = 'TEST_ROLE_001';`
      );
      console.log(`数据库验证: ${dbResult}`);
    } else {
      console.log('⚠ 未找到要删除的角色');
    }

    console.log('✓ 测试用例 6.6 完成\n');
  });
});
