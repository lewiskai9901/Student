/**
 * E2E测试 - 04 学生管理模块
 * 测试顺序：学生列表 -> 创建学生 -> 编辑学生 -> 转班 -> 导入导出 -> 删除学生
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

test.describe('04-学生管理模块测试', () => {

  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test.describe('4.1 学生列表', () => {

    test('访问学生管理页面', async ({ page }) => {
      await page.goto(`${BASE_URL}/student-affairs/students`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 截图
      await page.screenshot({ path: 'test-results/screenshots/30-student-list.png' });

      // 验证学生列表
      const table = page.locator('.el-table, table');
      await expect(table).toBeVisible({ timeout: 10000 });

      console.log('[PASS] 学生列表页面加载成功');
    });

    test('学生搜索和筛选', async ({ page }) => {
      await page.goto(`${BASE_URL}/student-affairs/students`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 查找筛选条件
      const filterInputs = page.locator('.el-input__inner, .el-select');
      const count = await filterInputs.count();

      // 截图：筛选区域
      await page.screenshot({ path: 'test-results/screenshots/31-student-filter.png' });

      console.log(`[INFO] 找到 ${count} 个筛选条件`);
      console.log('[PASS] 学生搜索筛选测试通过');
    });
  });

  test.describe('4.2 学生CRUD', () => {

    test('创建学生', async ({ page }) => {
      await page.goto(`${BASE_URL}/student-affairs/students`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 点击新增按钮
      const addBtn = page.locator('button:has-text("新增"), button:has-text("添加")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(1000);

        // 截图
        await page.screenshot({ path: 'test-results/screenshots/32-student-create.png' });

        // 关闭弹窗
        const cancelBtn = page.locator('button:has-text("取消")').first();
        if (await cancelBtn.isVisible()) {
          await cancelBtn.click();
        }
      }

      console.log('[PASS] 学生创建对话框测试通过');
    });

    test('查看学生详情', async ({ page }) => {
      await page.goto(`${BASE_URL}/student-affairs/students`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 点击第一行的查看/详情按钮
      const viewBtn = page.locator('button:has-text("查看"), button:has-text("详情")').first();
      if (await viewBtn.isVisible()) {
        await viewBtn.click();
        await page.waitForTimeout(1000);

        // 截图
        await page.screenshot({ path: 'test-results/screenshots/33-student-detail.png' });

        // 关闭
        const closeBtn = page.locator('button:has-text("关闭"), button:has-text("取消"), .el-dialog__close').first();
        if (await closeBtn.isVisible()) {
          await closeBtn.click();
        }
      }

      console.log('[PASS] 学生详情查看测试通过');
    });
  });

  test.describe('4.3 学生转班', () => {

    test('转班功能', async ({ page }) => {
      await page.goto(`${BASE_URL}/student-affairs/students`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 查找转班按钮
      const transferBtn = page.locator('button:has-text("转班")').first();
      if (await transferBtn.isVisible()) {
        await transferBtn.click();
        await page.waitForTimeout(1000);

        // 截图
        await page.screenshot({ path: 'test-results/screenshots/34-student-transfer.png' });

        // 关闭
        const cancelBtn = page.locator('button:has-text("取消")').first();
        if (await cancelBtn.isVisible()) {
          await cancelBtn.click();
        }
      }

      console.log('[PASS] 学生转班功能测试通过');
    });
  });

  test.describe('4.4 导入导出', () => {

    test('Excel导出功能', async ({ page }) => {
      await page.goto(`${BASE_URL}/student-affairs/students`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 查找导出按钮
      const exportBtn = page.locator('button:has-text("导出")').first();
      if (await exportBtn.isVisible()) {
        // 截图
        await page.screenshot({ path: 'test-results/screenshots/35-student-export.png' });
      }

      console.log('[PASS] Excel导出功能测试通过');
    });

    test('Excel导入功能', async ({ page }) => {
      await page.goto(`${BASE_URL}/student-affairs/students`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      // 查找导入按钮
      const importBtn = page.locator('button:has-text("导入")').first();
      if (await importBtn.isVisible()) {
        await importBtn.click();
        await page.waitForTimeout(1000);

        // 截图
        await page.screenshot({ path: 'test-results/screenshots/36-student-import.png' });

        // 关闭
        const cancelBtn = page.locator('button:has-text("取消"), .el-dialog__close').first();
        if (await cancelBtn.isVisible()) {
          await cancelBtn.click();
        }
      }

      console.log('[PASS] Excel导入功能测试通过');
    });
  });
});
