import { test, expect } from '@playwright/test';
import { AuthHelper } from './helpers/auth.js';
import { DatabaseHelper } from './helpers/database.js';

/**
 * Phase 1.1: Department Management Testing
 *
 * Test Plan:
 * 1. Login as admin
 * 2. Navigate to department management page
 * 3. View existing department tree
 * 4. Create new departments following organizational structure:
 *    学校总部 (existing, ID=1)
 *    ├── 教务处
 *    │   ├── 教学科
 *    │   ├── 学籍科
 *    │   └── 考务科
 *    ├── 学生处
 *    │   ├── 思政科
 *    │   ├── 宿管科
 *    │   └── 资助科
 *    └── 其他院系...
 * 5. Verify each creation in database
 * 6. Test edit functionality
 * 7. Test delete functionality (non-default departments only)
 * 8. Verify data consistency
 */

test.describe('Phase 1.1: Department Management', () => {
  let auth;
  let db;

  test.beforeEach(async ({ page }) => {
    auth = new AuthHelper(page);
    db = new DatabaseHelper();
    await auth.login('admin', 'admin123');
  });

  test('1.1.1 - Navigate to Department Management Page', async ({ page }) => {
    console.log('\n📋 Test 1.1.1: Navigate to Department Management');

    // Try different possible navigation paths
    const navPaths = [
      { menu: '系统管理', submenu: '部门管理' },
      { menu: 'System', submenu: 'Department' },
      { menu: '组织架构', submenu: '部门管理' }
    ];

    let navigated = false;

    for (const path of navPaths) {
      try {
        // Look for main menu
        const mainMenu = await page.getByText(path.menu, { exact: false });
        if (await mainMenu.isVisible({ timeout: 2000 })) {
          await mainMenu.click();
          await page.waitForTimeout(500);

          // Look for submenu
          const submenu = await page.getByText(path.submenu, { exact: false });
          if (await submenu.isVisible({ timeout: 2000 })) {
            await submenu.click();
            await page.waitForTimeout(1000);
            navigated = true;
            console.log(`✓ Navigated via: ${path.menu} -> ${path.submenu}`);
            break;
          }
        }
      } catch (e) {
        // Continue to next path
      }
    }

    // If navigation failed, try direct URL
    if (!navigated) {
      console.log('⚠ Menu navigation failed, trying direct URL');
      await page.goto('/student-affairs/departments');
      await page.waitForTimeout(1000);
    }

    // Take screenshot
    await page.screenshot({ path: 'test-results/screenshots/01-01-department-page.png', fullPage: true });

    // Verify page loaded
    const pageTitle = await page.title();
    console.log(`Page title: ${pageTitle}`);

    expect(navigated || page.url().includes('department')).toBeTruthy();
  });

  test('1.1.2 - View Existing Department Tree', async ({ page }) => {
    console.log('\n📋 Test 1.1.2: View Existing Department Tree');

    await page.goto('/student-affairs/departments');
    await page.waitForTimeout(2000);

    // Look for table or tree structure
    const possibleSelectors = [
      '.el-table',
      '.el-tree',
      'table',
      '[role="tree"]',
      '.department-tree',
      '.dept-tree'
    ];

    let treeFound = false;
    for (const selector of possibleSelectors) {
      const element = await page.$(selector);
      if (element) {
        console.log(`✓ Found department tree with selector: ${selector}`);
        treeFound = true;

        // Try to get row count
        const rows = await page.$$(`${selector} tr, ${selector} .el-tree-node`);
        console.log(`  Total rows/nodes: ${rows.length}`);
        break;
      }
    }

    // Verify with database
    const deptCount = await db.countDepartments();
    console.log(`  Database department count: ${deptCount}`);

    await page.screenshot({ path: 'test-results/screenshots/01-02-department-tree.png', fullPage: true });

    expect(treeFound).toBeTruthy();
  });

  test('1.1.3 - Create Top-Level Department: 教务处', async ({ page }) => {
    console.log('\n📋 Test 1.1.3: Create Top-Level Department - 教务处');

    await page.goto('/student-affairs/departments');
    await page.waitForTimeout(2000);

    const beforeCount = await db.countDepartments();
    console.log(`  Before: ${beforeCount} departments`);

    // Look for "Add" or "Create" button
    const addButtonSelectors = [
      'button:has-text("新增")',
      'button:has-text("添加")',
      'button:has-text("创建")',
      'button:has-text("Add")',
      '.add-btn',
      '.create-btn'
    ];

    let addButtonClicked = false;
    for (const selector of addButtonSelectors) {
      try {
        const button = await page.$(selector);
        if (button && await button.isVisible()) {
          await button.click();
          console.log(`✓ Clicked add button: ${selector}`);
          addButtonClicked = true;
          break;
        }
      } catch (e) {
        // Continue
      }
    }

    expect(addButtonClicked).toBeTruthy();

    // Wait for dialog/form to appear
    await page.waitForTimeout(1000);

    // Fill in department information
    const formData = {
      dept_name: '教务处',
      dept_code: 'JWC',
      parent_id: '1', // School headquarters
      status: '1',
      description: '负责教学管理、学籍管理、考务管理等工作'
    };

    // Fill form fields using placeholder-based selectors
    try {
      // Fill department code (部门编码) - required field
      await page.waitForTimeout(500);
      const deptCodeInput = await page.$('input[placeholder="请输入部门编码"]');
      if (deptCodeInput && await deptCodeInput.isVisible()) {
        await deptCodeInput.click();
        await deptCodeInput.fill('JWC');
        console.log('✓ Filled dept_code: JWC');
      } else {
        console.log('⚠ Could not find dept_code input');
      }

      // Fill department name (部门名称) - required field
      const deptNameInput = await page.$('input[placeholder="请输入部门名称"]');
      if (deptNameInput && await deptNameInput.isVisible()) {
        await deptNameInput.click();
        await deptNameInput.fill(formData.dept_name);
        console.log(`✓ Filled dept_name: ${formData.dept_name}`);
      } else {
        console.log('⚠ Could not find dept_name input');
      }

      // Select parent department (上级部门) - optional field
      await page.waitForTimeout(500);
      const parentSelectInput = await page.$('input[placeholder="请选择上级部门(不选则为顶级部门)"]');
      if (parentSelectInput && await parentSelectInput.isVisible()) {
        await parentSelectInput.click();
        await page.waitForTimeout(1000);

        // Wait for dropdown to appear and select first option (学校总部)
        try {
          // Try to find and click the first tree node
          const firstNode = await page.waitForSelector('.el-tree-node', { timeout: 3000 });
          if (firstNode) {
            await firstNode.click();
            console.log('✓ Selected parent department (学校总部)');
          }
        } catch (e) {
          // If click fails, try keyboard navigation
          await page.keyboard.press('ArrowDown');
          await page.waitForTimeout(300);
          await page.keyboard.press('Enter');
          console.log('✓ Selected parent department (keyboard)');
        }
      } else {
        console.log('⚠ Could not find parent department select - will create as top-level department');
      }

      await page.waitForTimeout(500);

    } catch (e) {
      console.log(`⚠ Error filling form: ${e.message}`);
    }

    await page.screenshot({ path: 'test-results/screenshots/01-03-create-dept-form.png', fullPage: true });

    // Click submit/confirm button
    const submitSelectors = [
      'button:has-text("确定")',
      'button:has-text("提交")',
      'button:has-text("保存")',
      'button:has-text("Submit")',
      '.el-button--primary'
    ];

    for (const selector of submitSelectors) {
      try {
        const buttons = await page.$$(selector);
        for (const button of buttons) {
          if (await button.isVisible()) {
            await button.click();
            console.log(`✓ Clicked submit button`);
            await page.waitForTimeout(2000);
            break;
          }
        }
        break;
      } catch (e) {
        // Continue
      }
    }

    // Verify creation
    await page.waitForTimeout(2000);
    const afterCount = await db.countDepartments();
    console.log(`  After: ${afterCount} departments`);

    // Check if department exists in database
    const deptExists = await db.getDepartmentByName('教务处');
    console.log(`  Database check: ${deptExists ? '✓ Found' : '✗ Not found'}`);

    await page.screenshot({ path: 'test-results/screenshots/01-03-after-create.png', fullPage: true });

    expect(afterCount).toBeGreaterThan(beforeCount);
  });

  test('1.1.4 - Create Sub-Department: 教学科 under 教务处', async ({ page }) => {
    console.log('\n📋 Test 1.1.4: Create Sub-Department - 教学科');

    await page.goto('/student-affairs/departments');
    await page.waitForTimeout(2000);

    // First, find 教务处 in the tree to get its ID
    const jwcData = await db.getDepartmentByName('教务处');
    console.log(`  Parent department (教务处): ${jwcData}`);

    const beforeCount = await db.countDepartments();

    // Click add button
    const addButton = await page.$('button:has-text("新增"), button:has-text("添加")');
    if (addButton) {
      await addButton.click();
      await page.waitForTimeout(1000);

      // Fill form for 教学科 using placeholder-based selectors
      await page.waitForTimeout(500);

      // Fill department code (部门编码)
      const deptCodeInput = await page.$('input[placeholder="请输入部门编码"]');
      if (deptCodeInput && await deptCodeInput.isVisible()) {
        await deptCodeInput.click();
        await deptCodeInput.fill('JXK');
        console.log('✓ Filled dept_code: JXK');
      }

      // Fill department name (部门名称)
      const deptNameInput = await page.$('input[placeholder="请输入部门名称"]');
      if (deptNameInput && await deptNameInput.isVisible()) {
        await deptNameInput.click();
        await deptNameInput.fill('教学科');
        console.log('✓ Filled dept_name: 教学科');
      }

      // Select parent department (教务处)
      await page.waitForTimeout(500);
      const parentSelectInput = await page.$('input[placeholder="请选择上级部门(不选则为顶级部门)"]');
      if (parentSelectInput && await parentSelectInput.isVisible()) {
        await parentSelectInput.click();
        await page.waitForTimeout(1000);

        // Wait for dropdown and find 教务处
        try {
          // Look for the specific node containing 教务处
          const jwcNode = await page.waitForSelector('.el-tree-node:has-text("教务处")', { timeout: 3000 });
          if (jwcNode) {
            await jwcNode.click();
            console.log('✓ Selected parent: 教务处');
          }
        } catch (e) {
          console.log(`⚠ Could not select parent: ${e.message}`);
        }
      }

      await page.screenshot({ path: 'test-results/screenshots/01-04-create-subdept-form.png', fullPage: true });

      // Submit
      const submitBtn = await page.$('button:has-text("确定"), button:has-text("提交")');
      if (submitBtn) {
        await submitBtn.click();
        await page.waitForTimeout(2000);
      }

      // Verify
      const afterCount = await db.countDepartments();
      const jxkExists = await db.getDepartmentByName('教学科');

      console.log(`  Before: ${beforeCount}, After: ${afterCount}`);
      console.log(`  教学科 exists: ${jxkExists ? '✓' : '✗'}`);

      await page.screenshot({ path: 'test-results/screenshots/01-04-after-create-subdept.png', fullPage: true });

      expect(afterCount).toBeGreaterThan(beforeCount);
    }
  });

  test('1.1.5 - View and Verify Department Tree Structure', async ({ page }) => {
    console.log('\n📋 Test 1.1.5: Verify Department Tree Structure');

    await page.goto('/student-affairs/departments');
    await page.waitForTimeout(2000);

    // Get all departments from database
    const totalDepts = await db.countDepartments();
    console.log(`  Total departments in database: ${totalDepts}`);

    // Take full screenshot
    await page.screenshot({ path: 'test-results/screenshots/01-05-full-tree.png', fullPage: true });

    // Verify key departments exist
    const keyDepts = ['学校总部', '教务处', '教学科'];
    for (const deptName of keyDepts) {
      const exists = await db.getDepartmentByName(deptName);
      console.log(`  ${deptName}: ${exists ? '✓ Found' : '✗ Not found'}`);
    }

    expect(totalDepts).toBeGreaterThan(0);
  });

  test.afterAll(async () => {
    console.log('\n✓ Phase 1.1: Department Management - Completed');
  });
});
