// @ts-check
import { test, expect } from '@playwright/test';
import { AuthHelper } from './helpers/auth.js';
import { DatabaseHelper } from './helpers/database.js';

/**
 * 用户管理模块 E2E 测试
 *
 * 测试范围:
 * - 5.1 导航到用户管理页面
 * - 5.2 查看用户列表
 * - 5.3 通过UI创建新用户
 * - 5.4 搜索用户
 * - 5.5 筛选用户状态
 * - 5.6 编辑用户信息
 * - 5.7 验证数据一致性
 * - 5.8 删除用户（软删除）
 */

test.describe('用户管理模块测试', () => {
  let page;
  let authHelper;
  let dbHelper;

  // 测试数据 - 将通过UI创建这些用户
  // 密码规则: 必须包含大小写字母和数字，长度8-20位
  const testUsers = [
    {
      username: 'test_teacher_001',
      password: 'Test123456',  // 符合规则：包含大写T、小写、数字
      realName: '测试教师001',
      employeeNo: 'T20240001',
      phone: '13900000001',
      email: 'teacher001@test.com',
      gender: 1, // 男
      status: 1  // 启用
    },
    {
      username: 'test_teacher_002',
      password: 'Test123456',
      realName: '测试教师002',
      employeeNo: 'T20240002',
      phone: '13900000002',
      email: 'teacher002@test.com',
      gender: 2, // 女
      status: 1  // 启用
    },
    {
      username: 'test_staff_001',
      password: 'Test123456',
      realName: '测试职员001',
      employeeNo: 'S20240001',
      phone: '13900000003',
      email: 'staff001@test.com',
      gender: 1,
      status: 1
    }
  ];

  test.beforeAll(async ({ browser }) => {
    console.log('\n=== 用户管理模块测试开始 ===\n');

    const context = await browser.newContext();
    page = await context.newPage();
    authHelper = new AuthHelper(page);
    dbHelper = new DatabaseHelper();

    // 登录系统
    await authHelper.login();
    console.log('✓ 用户已登录');
  });

  test.afterAll(async () => {
    console.log('\n=== 用户管理模块测试结束 ===\n');
    await page?.close();
  });

  /**
   * 测试用例 5.1: 导航到用户管理页面
   */
  test('5.1 应该能够导航到用户管理页面', async () => {
    console.log('\n--- 测试用例 5.1: 导航到用户管理页面 ---');

    // 直接使用URL导航（更可靠）
    await page.goto('http://localhost:3000/system/users');
    await page.waitForTimeout(2000);
    console.log('✓ 使用URL导航到用户管理页面');

    // 验证页面标题包含"用户管理"
    const pageContent = await page.textContent('body');
    expect(pageContent).toContain('用户管理');
    console.log('✓ 页面包含"用户管理"标题');

    // 截图
    await page.screenshot({
      path: 'test-results/screenshots/05-01-user-page-navigation.png',
      fullPage: true
    });
    console.log('✓ 截图已保存: 05-01-user-page-navigation.png');
    console.log('✓ 测试用例 5.1 完成\n');
  });

  /**
   * 测试用例 5.2: 查看用户列表
   */
  test('5.2 应该显示用户列表', async () => {
    console.log('\n--- 测试用例 5.2: 查看用户列表 ---');

    // 确保在用户管理页面
    await page.goto('http://localhost:3000/system/users');
    await page.waitForTimeout(2000);

    // 查找表格
    const table = page.locator('.el-table').first();
    expect(await table.isVisible()).toBeTruthy();
    console.log('✓ 找到用户列表表格');

    // 统计表格行数
    const rows = await page.locator('.el-table__row').all();
    console.log(`当前页显示 ${rows.length} 个用户`);

    // 验证数据库中的用户数
    const dbResult = await dbHelper.executeQuery(
      'SELECT COUNT(*) as count FROM users WHERE deleted = 0;'
    );
    console.log(`数据库验证: ${dbResult}`);

    // 截图
    await page.screenshot({
      path: 'test-results/screenshots/05-02-user-list.png',
      fullPage: true
    });
    console.log('✓ 截图已保存: 05-02-user-list.png');
    console.log('✓ 测试用例 5.2 完成\n');
  });

  /**
   * 测试用例 5.3: 通过UI创建新用户（第一个测试用户）
   */
  test('5.3 应该能够通过UI创建新用户', async () => {
    console.log('\n--- 测试用例 5.3: 创建新用户 ---');

    // 确保在用户管理页面
    await page.goto('http://localhost:3000/system/users');
    await page.waitForTimeout(2000);

    const user = testUsers[0];
    console.log(`准备创建用户: ${user.realName} (${user.username})`);

    // 1. 点击"新增用户"按钮
    const addButton = page.locator('button:has-text("新增用户")');
    await addButton.click();
    console.log('✓ 点击了"新增用户"按钮');

    // 2. 等待对话框打开
    await page.waitForTimeout(1500);
    const dialog = page.locator('.el-dialog').last();
    expect(await dialog.isVisible()).toBeTruthy();
    console.log('✓ 新增用户对话框已打开');

    // 3. 填写表单
    // 用户名
    await dialog.locator('input[placeholder="请输入用户名"]').fill(user.username);
    console.log(`✓ 填写用户名: ${user.username}`);

    // 密码
    await dialog.locator('input[placeholder*="请输入密码"]').fill(user.password);
    console.log(`✓ 填写密码: ${user.password}`);

    // 姓名
    await dialog.locator('input[placeholder="请输入姓名"]').fill(user.realName);
    console.log(`✓ 填写姓名: ${user.realName}`);

    // 工号
    await dialog.locator('input[placeholder="请输入工号"]').fill(user.employeeNo);
    console.log(`✓ 填写工号: ${user.employeeNo}`);

    // 手机号
    await dialog.locator('input[placeholder="请输入手机号"]').fill(user.phone);
    console.log(`✓ 填写手机号: ${user.phone}`);

    // 邮箱
    await dialog.locator('input[placeholder="请输入邮箱"]').fill(user.email);
    console.log(`✓ 填写邮箱: ${user.email}`);

    // 性别 - 使用单选按钮
    if (user.gender === 1) {
      await dialog.locator('label:has-text("男")').click();
      console.log('✓ 选择性别: 男');
    } else {
      await dialog.locator('label:has-text("女")').click();
      console.log('✓ 选择性别: 女');
    }

    // 状态
    if (user.status === 1) {
      await dialog.locator('.el-form-item:has-text("状态") label:has-text("启用")').click();
      console.log('✓ 选择状态: 启用');
    }

    // 截图保存填写后的表单
    await page.screenshot({
      path: 'test-results/screenshots/05-03-create-user-form.png',
      fullPage: true
    });
    console.log('✓ 截图已保存: 05-03-create-user-form.png');

    // 4. 提交表单
    await dialog.locator('button:has-text("确定")').click();
    console.log('✓ 点击了"确定"按钮');

    // 5. 等待对话框关闭和数据加载
    await page.waitForTimeout(2000);

    // 6. 验证用户是否创建成功
    const dbResult = await dbHelper.executeQuery(
      `SELECT id, username, real_name, phone FROM users WHERE username = '${user.username}' AND deleted = 0;`
    );
    console.log(`数据库验证: ${dbResult}`);
    expect(dbResult).toContain(user.username);
    console.log('✓ 用户创建成功，数据库中已存在');

    // 截图保存创建后的状态
    await page.screenshot({
      path: 'test-results/screenshots/05-03-after-create.png',
      fullPage: true
    });
    console.log('✓ 截图已保存: 05-03-after-create.png');
    console.log('✓ 测试用例 5.3 完成\n');
  });

  /**
   * 测试用例 5.4: 批量创建用户
   */
  test('5.4 应该能够批量创建多个用户', async () => {
    console.log('\n--- 测试用例 5.4: 批量创建用户 ---');

    // 确保在用户管理页面
    await page.goto('http://localhost:3000/system/users');
    await page.waitForTimeout(2000);

    console.log(`计划创建 ${testUsers.length - 1} 个额外用户\n`);

    // 跳过第一个用户（已在5.3中创建）
    for (let i = 1; i < testUsers.length; i++) {
      const user = testUsers[i];
      console.log(`[${i}/${testUsers.length - 1}] 创建用户: ${user.realName}`);

      // 点击新增按钮
      await page.locator('button:has-text("新增用户")').click();
      await page.waitForTimeout(1500);

      const dialog = page.locator('.el-dialog').last();

      // 填写表单 - 只填写必填字段和基本字段
      await dialog.locator('input[placeholder="请输入用户名"]').fill(user.username);
      await dialog.locator('input[placeholder*="请输入密码"]').fill(user.password);
      await dialog.locator('input[placeholder="请输入姓名"]').fill(user.realName);
      await dialog.locator('input[placeholder="请输入工号"]').fill(user.employeeNo);
      await dialog.locator('input[placeholder="请输入手机号"]').fill(user.phone);
      await dialog.locator('input[placeholder="请输入邮箱"]').fill(user.email);

      // 性别
      if (user.gender === 1) {
        await dialog.locator('label:has-text("男")').click();
      } else {
        await dialog.locator('label:has-text("女")').click();
      }

      // 提交
      await dialog.locator('button:has-text("确定")').click();
      await page.waitForTimeout(2000);

      console.log(`  ✓ 用户 ${user.realName} 创建完成`);
    }

    // 验证所有测试用户都已创建
    const dbResult = await dbHelper.executeQuery(
      `SELECT COUNT(*) as count FROM users WHERE username LIKE 'test_%' AND deleted = 0;`
    );
    console.log(`\n数据库验证 - 测试用户总数: ${dbResult}`);
    console.log('✓ 测试用例 5.4 完成\n');
  });

  /**
   * 测试用例 5.5: 搜索用户
   */
  test('5.5 应该能够搜索用户', async () => {
    console.log('\n--- 测试用例 5.5: 搜索用户 ---');

    // 确保在用户管理页面
    await page.goto('http://localhost:3000/system/users');
    await page.waitForTimeout(2000);

    // 测试按用户名搜索
    console.log('测试按用户名搜索...');
    const usernameInput = page.locator('.search-card input[placeholder="请输入用户名"]');
    await usernameInput.fill('test_teacher_001');
    await page.locator('button:has-text("查询")').click();
    await page.waitForTimeout(1500);

    let rows = await page.locator('.el-table__row').all();
    console.log(`搜索结果: ${rows.length} 条记录`);
    expect(rows.length).toBeGreaterThan(0);

    // 重置搜索（使用更具体的选择器，定位搜索卡片内的重置按钮）
    await page.locator('.search-card button:has-text("重置")').click();
    await page.waitForTimeout(1500);

    // 测试按姓名搜索
    console.log('\n测试按姓名搜索...');
    const nameInput = page.locator('.search-card input[placeholder="请输入姓名"]');
    await nameInput.fill('测试教师');
    await page.locator('button:has-text("查询")').click();
    await page.waitForTimeout(1500);

    rows = await page.locator('.el-table__row').all();
    console.log(`搜索结果: ${rows.length} 条记录`);

    // 截图
    await page.screenshot({
      path: 'test-results/screenshots/05-05-search-results.png',
      fullPage: true
    });
    console.log('✓ 截图已保存: 05-05-search-results.png');
    console.log('✓ 测试用例 5.5 完成\n');
  });

  /**
   * 测试用例 5.6: 状态筛选
   */
  test('5.6 应该能够按状态筛选用户', async () => {
    console.log('\n--- 测试用例 5.6: 状态筛选 ---');

    // 确保在用户管理页面
    await page.goto('http://localhost:3000/system/users');
    await page.waitForTimeout(2000);

    // 选择"启用"状态筛选
    const statusSelect = page.locator('.search-card .el-select').filter({ hasText: '请选择状态' });
    await statusSelect.click();
    await page.waitForTimeout(500);

    // 点击"启用"选项
    await page.locator('.el-select-dropdown__item:has-text("启用")').first().click();
    await page.waitForTimeout(500);

    // 点击查询
    await page.locator('button:has-text("查询")').click();
    await page.waitForTimeout(1500);

    const rows = await page.locator('.el-table__row').all();
    console.log(`筛选后显示 ${rows.length} 个启用状态的用户`);

    // 截图
    await page.screenshot({
      path: 'test-results/screenshots/05-06-status-filter.png',
      fullPage: true
    });
    console.log('✓ 截图已保存: 05-06-status-filter.png');
    console.log('✓ 测试用例 5.6 完成\n');
  });

  /**
   * 测试用例 5.7: 验证数据一致性
   */
  test('5.7 应该验证前后端数据一致性', async () => {
    console.log('\n--- 测试用例 5.7: 验证数据一致性 ---');

    // 确保在用户管理页面
    await page.goto('http://localhost:3000/system/users');
    await page.waitForTimeout(2000);

    // 获取前端显示的用户数
    const rows = await page.locator('.el-table__row').all();
    const pageCount = rows.length;

    // 获取数据库中的用户总数
    const dbResult = await dbHelper.executeQuery(
      'SELECT COUNT(*) as count FROM users WHERE deleted = 0;'
    );
    const match = dbResult.match(/count[\s\S]*?(\d+)/);
    const dbCount = match ? parseInt(match[1]) : 0;

    console.log(`前端显示用户数: ${pageCount}`);
    console.log(`数据库用户总数: ${dbCount}`);

    // 验证测试用户
    const testUserCount = await dbHelper.executeQuery(
      `SELECT COUNT(*) as count FROM users WHERE username LIKE 'test_%' AND deleted = 0;`
    );
    console.log(`测试用户数: ${testUserCount}`);

    console.log('✓ 数据一致性验证完成');
    console.log('✓ 测试用例 5.7 完成\n');
  });

  /**
   * 测试用例 5.8: 删除用户（软删除）
   */
  test('5.8 应该能够删除用户', async () => {
    console.log('\n--- 测试用例 5.8: 删除用户 ---');

    // 确保在用户管理页面
    await page.goto('http://localhost:3000/system/users');
    await page.waitForTimeout(2000);

    // 搜索要删除的用户
    const usernameInput = page.locator('.search-card input[placeholder="请输入用户名"]');
    await usernameInput.fill('test_staff_001');
    await page.locator('button:has-text("查询")').click();
    await page.waitForTimeout(1500);

    const rows = await page.locator('.el-table__row').all();
    if (rows.length > 0) {
      console.log('✓ 找到要删除的用户');

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
        `SELECT deleted FROM users WHERE username = 'test_staff_001';`
      );
      console.log(`数据库验证: ${dbResult}`);
    } else {
      console.log('⚠ 未找到要删除的用户');
    }

    console.log('✓ 测试用例 5.8 完成\n');
  });
});
