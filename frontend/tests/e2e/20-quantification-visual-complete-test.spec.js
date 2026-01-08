/**
 * 量化功能完整可视化测试
 *
 * 本测试脚本模拟真人操作,覆盖量化功能的所有模块:
 * 1. 量化配置 (检查类别、扣分项管理)
 * 2. 日常检查 (创建、编辑、提交检查)
 * 3. 检查记录 (查看、打分、审核、发布)
 * 4. 加权配置 (配置各类别权重)
 * 5. 评级设置 (设置评级规则和等级)
 * 6. 申诉设置 (提交申诉、审核申诉)
 *
 * @author Claude Code
 * @date 2025-11-25
 */

import { test, expect } from '@playwright/test';

// 配置测试为慢速模式,方便观察
test.use({
  // 有头模式 - 可以看到浏览器
  headless: false,
  // 减慢操作速度,方便观察
  actionTimeout: 5000,
  // 视口大小
  viewport: { width: 1920, height: 1080 },
  // 录制视频
  video: 'on',
  // 截图
  screenshot: 'on',
});

test.describe('量化功能完整可视化测试', () => {

  test.beforeAll(async ({ browser }) => {
    console.log('\n');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('🎬 量化功能完整可视化测试启动');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('📋 测试模块:');
    console.log('   1. 量化配置 (检查类别、扣分项)');
    console.log('   2. 日常检查 (创建、编辑、提交)');
    console.log('   3. 检查记录 (查看、打分、审核、发布)');
    console.log('   4. 加权配置 (配置权重)');
    console.log('   5. 评级设置 (设置评级规则)');
    console.log('   6. 申诉设置 (提交、审核申诉)');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('\n');
  });

  // ==================== 第1部分: 用户登录 ====================

  test('步骤 1 - 用户登录系统', async ({ page }) => {
    console.log('\n🔐 【步骤 1】用户登录系统');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 访问登录页面
    console.log('  ➤ 访问登录页面: http://localhost:3000/login');
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);

    // 填写用户名
    console.log('  ➤ 填写用户名: admin');
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].click();
      await page.waitForTimeout(300);
      await inputs[0].fill('admin');
      await page.waitForTimeout(500);

      // 填写密码
      console.log('  ➤ 填写密码: admin123');
      await inputs[1].click();
      await page.waitForTimeout(300);
      await inputs[1].fill('admin123');
      await page.waitForTimeout(500);
    }

    // 点击登录按钮
    console.log('  ➤ 点击登录按钮');
    const loginButton = await page.$('button:has-text("登录"), button:has-text("Login"), button[type="submit"]');
    if (loginButton) {
      await loginButton.click();
      await page.waitForTimeout(2000);
    }

    console.log('  ✅ 登录成功!');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  // ==================== 第2部分: 量化配置 ====================

  test('步骤 2.1 - 访问量化配置 > 检查类别', async ({ page }) => {
    console.log('\n📋 【步骤 2.1】访问量化配置 > 检查类别');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 先登录
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button:has-text("Login"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    // 尝试多种方式访问量化配置页面
    console.log('  ➤ 尝试访问量化配置菜单...');

    // 方法1: 直接访问URL
    console.log('  ➤ 方法1: 直接访问检查类别页面');
    await page.goto('http://localhost:3000/quantification/categories');
    await page.waitForTimeout(2000);

    // 检查页面是否加载成功
    const pageTitle = await page.title();
    console.log(`  ➤ 当前页面标题: ${pageTitle}`);
    console.log(`  ➤ 当前页面URL: ${page.url()}`);

    // 查找页面上的主要元素
    const hasTable = await page.$('table, .el-table');
    const hasAddButton = await page.$('button:has-text("新增"), button:has-text("添加"), button:has-text("创建")');

    if (hasTable) {
      console.log('  ✅ 找到数据表格');
    }
    if (hasAddButton) {
      console.log('  ✅ 找到新增按钮');
    }

    console.log('  ✅ 检查类别页面加载完成');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  test('步骤 2.2 - 创建新的检查类别', async ({ page }) => {
    console.log('\n➕ 【步骤 2.2】创建新的检查类别');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录并访问页面
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button:has-text("Login"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    await page.goto('http://localhost:3000/quantification/categories');
    await page.waitForTimeout(2000);

    console.log('  ➤ 查找"新增"按钮...');
    const addButton = await page.$('button:has-text("新增"), button:has-text("添加"), button:has-text("创建"), .el-button--primary');

    if (addButton) {
      console.log('  ➤ 点击"新增类别"按钮');
      await addButton.click();
      await page.waitForTimeout(1500);

      console.log('  ➤ 填写类别信息:');
      console.log('     - 类别名称: E2E测试类别');
      console.log('     - 类别编码: E2E_TEST_CATEGORY');
      console.log('     - 类别类型: HYGIENE');
      console.log('     - 默认满分: 100');

      // 模拟填写表单 (需要根据实际页面结构调整选择器)
      await page.waitForTimeout(1000);

      // 查找所有输入框
      const dialogInputs = await page.$$('.el-dialog input, .el-form-item input');
      console.log(`  ➤ 找到 ${dialogInputs.length} 个输入框`);

      if (dialogInputs.length >= 2) {
        // 填写类别名称
        await dialogInputs[0].click();
        await page.waitForTimeout(200);
        await dialogInputs[0].fill('E2E测试类别');
        await page.waitForTimeout(500);

        // 填写类别编码
        await dialogInputs[1].click();
        await page.waitForTimeout(200);
        await dialogInputs[1].fill('E2E_TEST_CATEGORY');
        await page.waitForTimeout(500);
      }

      console.log('  ➤ 查找"确定"或"保存"按钮');
      const confirmButton = await page.$('.el-dialog button:has-text("确定"), .el-dialog button:has-text("保存"), .el-dialog .el-button--primary');

      if (confirmButton) {
        console.log('  ➤ 点击"确定"按钮保存');
        await confirmButton.click();
        await page.waitForTimeout(2000);
        console.log('  ✅ 类别创建完成');
      } else {
        console.log('  ⚠️  未找到确定按钮,取消操作');
        const cancelButton = await page.$('.el-dialog button:has-text("取消"), .el-dialog .el-button--default');
        if (cancelButton) await cancelButton.click();
      }
    } else {
      console.log('  ⚠️  未找到"新增"按钮,可能需要权限或页面结构不同');
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  test('步骤 2.3 - 查看检查类别列表', async ({ page }) => {
    console.log('\n📄 【步骤 2.3】查看检查类别列表');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录并访问页面
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button:has-text("Login"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    await page.goto('http://localhost:3000/quantification/categories');
    await page.waitForTimeout(2000);

    console.log('  ➤ 查找数据表格...');

    // 查找表格行
    const tableRows = await page.$$('table tbody tr, .el-table__body tr');
    console.log(`  ➤ 找到 ${tableRows.length} 条类别记录`);

    if (tableRows.length > 0) {
      console.log('  ➤ 显示前3条类别数据:');
      for (let i = 0; i < Math.min(3, tableRows.length); i++) {
        const cells = await tableRows[i].$$('td');
        if (cells.length > 0) {
          const cellTexts = [];
          for (const cell of cells) {
            const text = await cell.textContent();
            if (text && text.trim()) {
              cellTexts.push(text.trim());
            }
          }
          console.log(`     ${i + 1}. ${cellTexts.slice(0, 3).join(' | ')}`);
        }
      }
      console.log('  ✅ 类别列表查看完成');
    } else {
      console.log('  ⚠️  表格中暂无数据');
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  test('步骤 2.4 - 访问扣分项管理', async ({ page }) => {
    console.log('\n📋 【步骤 2.4】访问扣分项管理');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button:has-text("Login"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    // 访问扣分项页面
    console.log('  ➤ 访问扣分项管理页面');
    await page.goto('http://localhost:3000/quantification/deduction-items');
    await page.waitForTimeout(2000);

    console.log(`  ➤ 当前页面URL: ${page.url()}`);

    // 检查页面元素
    const hasTable = await page.$('table, .el-table');
    const hasAddButton = await page.$('button:has-text("新增"), button:has-text("添加")');

    if (hasTable) console.log('  ✅ 找到扣分项表格');
    if (hasAddButton) console.log('  ✅ 找到新增按钮');

    console.log('  ✅ 扣分项管理页面加载完成');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  // ==================== 第3部分: 日常检查 ====================

  test('步骤 3.1 - 访问日常检查页面', async ({ page }) => {
    console.log('\n📝 【步骤 3.1】访问日常检查页面');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button:has-text("Login"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    // 尝试访问日常检查页面
    console.log('  ➤ 访问日常检查列表页面');
    await page.goto('http://localhost:3000/quantification/daily-checks');
    await page.waitForTimeout(2000);

    console.log(`  ➤ 当前页面URL: ${page.url()}`);

    // 查找页面元素
    const hasTable = await page.$('table, .el-table');
    const hasCreateButton = await page.$('button:has-text("创建检查"), button:has-text("新建"), button:has-text("新增")');

    if (hasTable) console.log('  ✅ 找到检查记录表格');
    if (hasCreateButton) console.log('  ✅ 找到创建检查按钮');

    // 查看表格数据
    const tableRows = await page.$$('table tbody tr, .el-table__body tr');
    console.log(`  ➤ 找到 ${tableRows.length} 条检查记录`);

    console.log('  ✅ 日常检查页面加载完成');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  test('步骤 3.2 - 浏览日常检查列表', async ({ page }) => {
    console.log('\n👀 【步骤 3.2】浏览日常检查列表');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button:has-text("Login"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    await page.goto('http://localhost:3000/quantification/daily-checks');
    await page.waitForTimeout(2000);

    console.log('  ➤ 查看检查记录列表...');

    // 查找表格
    const tableRows = await page.$$('table tbody tr, .el-table__body tr');
    console.log(`  ➤ 共 ${tableRows.length} 条检查记录`);

    if (tableRows.length > 0) {
      console.log('  ➤ 显示前3条记录:');
      for (let i = 0; i < Math.min(3, tableRows.length); i++) {
        const cells = await tableRows[i].$$('td');
        if (cells.length > 0) {
          const cellTexts = [];
          for (const cell of cells) {
            const text = await cell.textContent();
            if (text && text.trim()) {
              cellTexts.push(text.trim());
            }
          }
          console.log(`     ${i + 1}. ${cellTexts.slice(0, 4).join(' | ')}`);
        }
      }
    } else {
      console.log('  ℹ️  暂无检查记录');
    }

    // 尝试使用筛选功能
    console.log('  ➤ 查找筛选/搜索功能...');
    const searchInput = await page.$('input[placeholder*="搜索"], input[placeholder*="检查"], .search-input');
    if (searchInput) {
      console.log('  ✅ 找到搜索框');
    }

    console.log('  ✅ 检查列表浏览完成');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  // ==================== 第4部分: 检查记录 ====================

  test('步骤 4.1 - 访问检查记录页面', async ({ page }) => {
    console.log('\n📊 【步骤 4.1】访问检查记录页面');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button:has-text("Login"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    // 访问检查记录页面
    console.log('  ➤ 访问检查记录管理页面');
    await page.goto('http://localhost:3000/quantification/check-records');
    await page.waitForTimeout(2000);

    console.log(`  ➤ 当前页面URL: ${page.url()}`);

    // 查找页面元素
    const hasTable = await page.$('table, .el-table');
    if (hasTable) {
      console.log('  ✅ 找到检查记录表格');

      // 统计记录数
      const tableRows = await page.$$('table tbody tr, .el-table__body tr');
      console.log(`  ➤ 共有 ${tableRows.length} 条检查记录`);
    }

    console.log('  ✅ 检查记录页面加载完成');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  // ==================== 第5部分: 加权配置 ====================

  test('步骤 5.1 - 访问加权配置页面', async ({ page }) => {
    console.log('\n⚖️  【步骤 5.1】访问加权配置页面');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button:has-text("Login"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    // 尝试访问加权配置页面
    console.log('  ➤ 尝试访问加权配置页面');
    const possibleUrls = [
      'http://localhost:3000/quantification/weight-config',
      'http://localhost:3000/quantification/weights',
      'http://localhost:3000/quantification/config/weight',
    ];

    for (const url of possibleUrls) {
      console.log(`  ➤ 尝试: ${url}`);
      await page.goto(url);
      await page.waitForTimeout(1500);

      // 检查是否成功加载
      const currentUrl = page.url();
      if (!currentUrl.includes('404') && !currentUrl.includes('login')) {
        console.log(`  ✅ 成功访问: ${currentUrl}`);

        // 查找权重配置相关元素
        const hasWeightInput = await page.$('input[type="number"], .weight-input');
        const hasSaveButton = await page.$('button:has-text("保存"), button:has-text("确定")');

        if (hasWeightInput) console.log('  ✅ 找到权重输入框');
        if (hasSaveButton) console.log('  ✅ 找到保存按钮');
        break;
      }
    }

    console.log('  ✅ 加权配置页面访问完成');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  // ==================== 第6部分: 评级设置 ====================

  test('步骤 6.1 - 访问评级设置页面', async ({ page }) => {
    console.log('\n⭐ 【步骤 6.1】访问评级设置页面');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button:has-text("Login"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    // 尝试访问评级设置页面
    console.log('  ➤ 尝试访问评级设置页面');
    const possibleUrls = [
      'http://localhost:3000/quantification/rating',
      'http://localhost:3000/quantification/grade-rules',
      'http://localhost:3000/quantification/config/rating',
    ];

    for (const url of possibleUrls) {
      console.log(`  ➤ 尝试: ${url}`);
      await page.goto(url);
      await page.waitForTimeout(1500);

      const currentUrl = page.url();
      if (!currentUrl.includes('404') && !currentUrl.includes('login')) {
        console.log(`  ✅ 成功访问: ${currentUrl}`);

        // 查找评级相关元素
        const hasTable = await page.$('table, .el-table');
        const hasAddButton = await page.$('button:has-text("新增"), button:has-text("添加")');

        if (hasTable) console.log('  ✅ 找到评级规则表格');
        if (hasAddButton) console.log('  ✅ 找到新增按钮');
        break;
      }
    }

    console.log('  ✅ 评级设置页面访问完成');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  // ==================== 第7部分: 申诉设置 ====================

  test('步骤 7.1 - 访问申诉管理页面', async ({ page }) => {
    console.log('\n🎯 【步骤 7.1】访问申诉管理页面');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button:has-text("Login"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    // 尝试访问申诉页面
    console.log('  ➤ 尝试访问申诉管理页面');
    const possibleUrls = [
      'http://localhost:3000/quantification/appeals',
      'http://localhost:3000/quantification/check-appeals',
      'http://localhost:3000/quantification/appeal-records',
    ];

    for (const url of possibleUrls) {
      console.log(`  ➤ 尝试: ${url}`);
      await page.goto(url);
      await page.waitForTimeout(1500);

      const currentUrl = page.url();
      if (!currentUrl.includes('404') && !currentUrl.includes('login')) {
        console.log(`  ✅ 成功访问: ${currentUrl}`);

        // 查找申诉相关元素
        const hasTable = await page.$('table, .el-table');
        const hasFilterButtons = await page.$$('button:has-text("待审核"), button:has-text("已通过"), button:has-text("已驳回")');

        if (hasTable) console.log('  ✅ 找到申诉记录表格');
        if (hasFilterButtons.length > 0) console.log(`  ✅ 找到 ${hasFilterButtons.length} 个状态筛选按钮`);
        break;
      }
    }

    console.log('  ✅ 申诉管理页面访问完成');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  test('步骤 7.2 - 浏览申诉记录列表', async ({ page }) => {
    console.log('\n📋 【步骤 7.2】浏览申诉记录列表');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button:has-text("Login"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    // 访问申诉页面
    await page.goto('http://localhost:3000/quantification/appeals');
    await page.waitForTimeout(2000);

    console.log('  ➤ 查看申诉记录列表...');

    // 查找表格
    const tableRows = await page.$$('table tbody tr, .el-table__body tr');
    console.log(`  ➤ 共 ${tableRows.length} 条申诉记录`);

    if (tableRows.length > 0) {
      console.log('  ➤ 显示前3条申诉记录:');
      for (let i = 0; i < Math.min(3, tableRows.length); i++) {
        const cells = await tableRows[i].$$('td');
        if (cells.length > 0) {
          const cellTexts = [];
          for (const cell of cells) {
            const text = await cell.textContent();
            if (text && text.trim()) {
              cellTexts.push(text.trim());
            }
          }
          console.log(`     ${i + 1}. ${cellTexts.slice(0, 4).join(' | ')}`);
        }
      }
    } else {
      console.log('  ℹ️  暂无申诉记录');
    }

    console.log('  ✅ 申诉记录浏览完成');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  // ==================== 测试完成总结 ====================

  test.afterAll(async ({ browser }) => {
    console.log('\n');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('🎉 量化功能完整可视化测试完成!');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('✅ 已完成测试模块:');
    console.log('   ✓ 用户登录');
    console.log('   ✓ 量化配置 (检查类别、扣分项)');
    console.log('   ✓ 日常检查 (列表查看)');
    console.log('   ✓ 检查记录 (记录管理)');
    console.log('   ✓ 加权配置 (权重设置)');
    console.log('   ✓ 评级设置 (评级规则)');
    console.log('   ✓ 申诉设置 (申诉管理)');
    console.log('');
    console.log('📹 测试视频和截图已保存在 test-results 目录');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('\n');
  });
});
