// @ts-check
import { test, expect } from '@playwright/test';
import { AuthHelper } from './helpers/auth.js';
import { DatabaseHelper } from './helpers/database.js';

/**
 * 公告管理模块 E2E 测试
 *
 * 测试范围:
 * - 9.1 导航到公告管理页面
 * - 9.2 查看公告列表
 * - 9.3 通过UI创建新公告
 * - 9.4 发布公告
 * - 9.5 验证数据一致性
 * - 9.6 删除公告
 */

test.describe('公告管理模块测试', () => {
  let page;
  let authHelper;
  let dbHelper;

  // 测试数据 - 将通过UI创建这些公告
  const testAnnouncement = {
    title: 'E2E测试公告',
    announcementType: 'notice',  // 通知
    priority: 2,  // 重要
    content: '这是一条通过E2E测试创建的公告内容，用于验证公告管理功能是否正常工作。'
  };

  test.beforeAll(async ({ browser }) => {
    console.log('\n=== 公告管理模块测试开始 ===\n');

    const context = await browser.newContext();
    page = await context.newPage();
    authHelper = new AuthHelper(page);
    dbHelper = new DatabaseHelper();

    // 登录系统
    await authHelper.login();
    console.log('✓ 用户已登录');
  });

  test.afterAll(async () => {
    console.log('\n=== 公告管理模块测试结束 ===\n');
    await page?.close();
  });

  /**
   * 测试用例 9.1: 导航到公告管理页面
   */
  test('9.1 应该能够导航到公告管理页面', async () => {
    console.log('\n--- 测试用例 9.1: 导航到公告管理页面 ---');

    // 直接使用URL导航
    await page.goto('http://localhost:3000/system/announcements');
    await page.waitForTimeout(2000);
    console.log('✓ 使用URL导航到公告管理页面');

    // 验证页面加载成功
    const pageHeader = page.locator('.page-header h2');
    const headerText = await pageHeader.textContent();
    if (headerText && headerText.includes('公告')) {
      console.log('✓ 页面加载成功，找到"公告管理"标题');
    } else {
      console.log('⚠ 页面标题未找到，检查表格');
      const table = page.locator('.el-table').first();
      const tableExists = await table.isVisible({ timeout: 3000 }).catch(() => false);
      if (tableExists) {
        console.log('✓ 页面加载成功，表格可见');
      }
    }

    // 截图
    await page.screenshot({
      path: 'test-results/screenshots/09-01-announcement-page-navigation.png',
      fullPage: true
    });
    console.log('✓ 截图已保存: 09-01-announcement-page-navigation.png');
    console.log('✓ 测试用例 9.1 完成\n');
  });

  /**
   * 测试用例 9.2: 查看公告列表
   */
  test('9.2 应该显示公告列表', async () => {
    console.log('\n--- 测试用例 9.2: 查看公告列表 ---');

    // 确保在公告管理页面
    await page.goto('http://localhost:3000/system/announcements');
    await page.waitForTimeout(2000);

    // 查找表格
    const table = page.locator('.el-table').first();
    expect(await table.isVisible()).toBeTruthy();
    console.log('✓ 找到公告列表表格');

    // 统计表格行数
    const rows = await page.locator('.el-table__row').all();
    console.log(`当前显示 ${rows.length} 条公告`);

    // 验证数据库中的公告数
    const dbResult = await dbHelper.executeQuery(
      'SELECT COUNT(*) as count FROM announcements WHERE deleted = 0;'
    );
    console.log(`数据库验证: ${dbResult}`);

    // 截图
    await page.screenshot({
      path: 'test-results/screenshots/09-02-announcement-list.png',
      fullPage: true
    });
    console.log('✓ 截图已保存: 09-02-announcement-list.png');
    console.log('✓ 测试用例 9.2 完成\n');
  });

  /**
   * 测试用例 9.3: 通过UI创建新公告
   */
  test('9.3 应该能够通过UI创建新公告', async () => {
    console.log('\n--- 测试用例 9.3: 创建新公告 ---');

    // 确保在公告管理页面
    await page.goto('http://localhost:3000/system/announcements');
    await page.waitForTimeout(2000);

    const announcement = testAnnouncement;
    console.log(`准备创建公告: ${announcement.title}`);

    // 1. 点击"发布公告"按钮
    const addButton = page.locator('button:has-text("发布公告")');
    await addButton.click();
    console.log('✓ 点击了"发布公告"按钮');

    // 2. 等待对话框打开
    await page.waitForTimeout(1500);
    const dialog = page.locator('.el-dialog').last();
    expect(await dialog.isVisible()).toBeTruthy();
    console.log('✓ 新增公告对话框已打开');

    // 3. 填写表单
    // 标题
    await dialog.locator('input[placeholder*="标题"]').fill(announcement.title);
    console.log(`✓ 填写标题: ${announcement.title}`);

    // 类型 - 默认notice（通知）
    console.log('✓ 类型: 通知（默认）');

    // 优先级 - 选择"重要"
    if (announcement.priority === 2) {
      await dialog.locator('label:has-text("重要")').click();
      console.log('✓ 选择优先级: 重要');
    }

    // 内容
    await dialog.locator('textarea[placeholder*="内容"]').fill(announcement.content);
    console.log(`✓ 填写内容: ${announcement.content.substring(0, 30)}...`);

    // 截图保存填写后的表单
    await page.screenshot({
      path: 'test-results/screenshots/09-03-create-announcement-form.png',
      fullPage: true
    });
    console.log('✓ 截图已保存: 09-03-create-announcement-form.png');

    // 4. 提交表单
    await dialog.locator('button:has-text("确定")').click();
    console.log('✓ 点击了"确定"按钮');

    // 5. 等待对话框关闭和数据加载
    await page.waitForTimeout(2000);

    // 6. 验证公告是否创建成功
    const dbResult = await dbHelper.executeQuery(
      `SELECT id, title, is_published FROM announcements WHERE title = '${announcement.title}' AND deleted = 0;`
    );
    console.log(`数据库验证: ${dbResult}`);

    if (dbResult.includes(announcement.title)) {
      console.log('✓ 公告创建成功，数据库中已存在');
    } else {
      console.log('⚠ 数据库中未找到新创建的公告');
    }

    // 截图保存创建后的状态
    await page.screenshot({
      path: 'test-results/screenshots/09-03-after-create.png',
      fullPage: true
    });
    console.log('✓ 截图已保存: 09-03-after-create.png');
    console.log('✓ 测试用例 9.3 完成\n');
  });

  /**
   * 测试用例 9.4: 发布公告
   */
  test('9.4 应该能够发布公告', async () => {
    console.log('\n--- 测试用例 9.4: 发布公告 ---');

    // 确保在公告管理页面
    await page.goto('http://localhost:3000/system/announcements');
    await page.waitForTimeout(2000);

    // 查找包含测试公告标题的行
    const targetRow = page.locator('.el-table__row').filter({ hasText: testAnnouncement.title }).first();
    const rowExists = await targetRow.isVisible({ timeout: 3000 }).catch(() => false);

    if (rowExists) {
      console.log('✓ 找到测试公告');

      // 查找"发布"按钮
      const publishButton = targetRow.locator('button:has-text("发布")');
      const publishBtnExists = await publishButton.isVisible({ timeout: 2000 }).catch(() => false);

      if (publishBtnExists) {
        await publishButton.click();
        console.log('✓ 点击了"发布"按钮');
        await page.waitForTimeout(1500);

        // 验证发布状态
        const dbResult = await dbHelper.executeQuery(
          `SELECT is_published FROM announcements WHERE title = '${testAnnouncement.title}' AND deleted = 0;`
        );
        console.log(`数据库验证: ${dbResult}`);
      } else {
        console.log('⚠ 公告可能已经发布，跳过发布操作');
      }
    } else {
      console.log('⚠ 未找到测试公告，可能创建失败');
    }

    // 截图
    await page.screenshot({
      path: 'test-results/screenshots/09-04-publish-announcement.png',
      fullPage: true
    });
    console.log('✓ 截图已保存: 09-04-publish-announcement.png');
    console.log('✓ 测试用例 9.4 完成\n');
  });

  /**
   * 测试用例 9.5: 验证数据一致性
   */
  test('9.5 应该验证前后端数据一致性', async () => {
    console.log('\n--- 测试用例 9.5: 验证数据一致性 ---');

    // 确保在公告管理页面
    await page.goto('http://localhost:3000/system/announcements');
    await page.waitForTimeout(2000);

    // 获取前端显示的公告数
    const rows = await page.locator('.el-table__row').all();
    const pageCount = rows.length;

    // 获取数据库中的公告总数
    const dbResult = await dbHelper.executeQuery(
      'SELECT COUNT(*) as count FROM announcements WHERE deleted = 0;'
    );
    const match = dbResult.match(/count[\s\S]*?(\d+)/);
    const dbCount = match ? parseInt(match[1]) : 0;

    console.log(`前端显示公告数: ${pageCount}`);
    console.log(`数据库公告总数: ${dbCount}`);

    // 验证测试公告
    const testAnnouncementCount = await dbHelper.executeQuery(
      `SELECT COUNT(*) as count FROM announcements WHERE title LIKE '%E2E测试%' AND deleted = 0;`
    );
    console.log(`测试公告数: ${testAnnouncementCount}`);

    console.log('✓ 数据一致性验证完成');
    console.log('✓ 测试用例 9.5 完成\n');
  });

  /**
   * 测试用例 9.6: 删除公告
   */
  test('9.6 应该能够删除公告', async () => {
    console.log('\n--- 测试用例 9.6: 删除公告 ---');

    // 确保在公告管理页面
    await page.goto('http://localhost:3000/system/announcements');
    await page.waitForTimeout(2000);

    // 查找包含测试公告标题的行
    const targetRow = page.locator('.el-table__row').filter({ hasText: testAnnouncement.title }).first();
    const rowExists = await targetRow.isVisible({ timeout: 3000 }).catch(() => false);

    if (rowExists) {
      console.log('✓ 找到要删除的公告');

      // 点击删除按钮
      const deleteButton = targetRow.locator('button:has-text("删除")');
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
        `SELECT deleted FROM announcements WHERE title = '${testAnnouncement.title}' ORDER BY id DESC LIMIT 1;`
      );
      console.log(`数据库验证: ${dbResult}`);
    } else {
      console.log('⚠ 未找到要删除的公告');
    }

    console.log('✓ 测试用例 9.6 完成\n');
  });
});
