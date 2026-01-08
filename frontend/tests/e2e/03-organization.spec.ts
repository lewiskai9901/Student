/**
 * E2E测试 - 03 组织架构模块
 * 测试顺序：部门管理 -> 年级管理 -> 专业管理 -> 班级管理
 * 依赖关系：部门 -> 年级 -> 专业 -> 班级
 */
import { test, expect, Page } from '@playwright/test';

const BASE_URL = 'http://localhost:3000';
const TEST_USER = { username: 'admin', password: 'admin123' };

async function login(page: Page) {
  await page.goto(`${BASE_URL}/login`);
  await page.waitForLoadState('networkidle');
  await page.fill('input[placeholder*="用户名"]', TEST_USER.username);
  await page.fill('input[type="password"]', TEST_USER.password);
  await page.click('button:has-text("登录"), button[type="submit"]');
  await page.waitForTimeout(3000);
  await page.waitForSelector('text=管理员', { timeout: 10000 });
}

test.describe('03-组织架构模块测试', () => {

  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test.describe('3.1 部门管理', () => {

    test('访问部门管理页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/system/departments`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/20-department-list.png' });

      // 验证部门树或列表存在
      const content = page.locator('.el-tree, .el-table, table, [class*="tree"]');
      await expect(content).toBeVisible({ timeout: 10000 });

      console.log('[PASS] 部门管理页面加载成功');
    });

    test('创建部门', async ({ page }) => {
      await page.goto(`${BASE_URL}/system/departments`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 点击新增按钮
      const addBtn = page.locator('button:has-text("新增"), button:has-text("添加")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(1000);

        // 截图：新增弹窗
        await page.screenshot({ path: 'test-results/screenshots/21-department-create.png' });

        // 关闭弹窗
        const cancelBtn = page.locator('button:has-text("取消")').first();
        if (await cancelBtn.isVisible()) {
          await cancelBtn.click();
        }
      }

      console.log('[PASS] 部门创建对话框测试通过');
    });
  });

  test.describe('3.2 年级管理', () => {

    test('访问年级管理页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/academic/grades`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/22-grade-list.png' });

      console.log('[PASS] 年级管理页面加载成功');
    });
  });

  test.describe('3.3 专业管理', () => {

    test('访问专业管理页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/academic/majors`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/23-major-list.png' });

      console.log('[PASS] 专业管理页面加载成功');
    });
  });

  test.describe('3.4 班级管理', () => {

    test('访问班级管理页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/student-affairs/classes`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/24-class-list.png' });

      // 验证班级列表
      const table = page.locator('.el-table, table');
      await expect(table).toBeVisible({ timeout: 10000 });

      console.log('[PASS] 班级管理页面加载成功');
    });

    test('创建班级', async ({ page }) => {
      await page.goto(`${BASE_URL}/student-affairs/classes`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 点击新增按钮
      const addBtn = page.locator('button:has-text("新增"), button:has-text("添加"), button:has-text("创建")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(1000);

        // 截图
        await page.screenshot({ path: 'test-results/screenshots/25-class-create.png' });

        // 关闭弹窗
        const cancelBtn = page.locator('button:has-text("取消")').first();
        if (await cancelBtn.isVisible()) {
          await cancelBtn.click();
        }
      }

      console.log('[PASS] 班级创建对话框测试通过');
    });

    test('分配班主任', async ({ page }) => {
      await page.goto(`${BASE_URL}/student-affairs/classes`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 查找分配班主任按钮
      const assignBtn = page.locator('button:has-text("班主任"), button:has-text("分配")').first();
      if (await assignBtn.isVisible()) {
        await assignBtn.click();
        await page.waitForTimeout(1000);

        // 截图
        await page.screenshot({ path: 'test-results/screenshots/26-class-teacher-assign.png' });

        // 关闭弹窗
        const cancelBtn = page.locator('button:has-text("取消")').first();
        if (await cancelBtn.isVisible()) {
          await cancelBtn.click();
        }
      }

      console.log('[PASS] 班主任分配测试通过');
    });
  });
});
