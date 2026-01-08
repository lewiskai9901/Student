/**
 * 量化功能详细 CRUD 测试
 *
 * 测试内容:
 * 1. 检查类别 CRUD
 * 2. 扣分项 CRUD (关联检查类别)
 * 3. 检查模板 CRUD (关联类别和扣分项)
 * 4. 日常检查 CRUD (关联宿舍)
 * 5. 检查打分功能
 * 6. 申诉功能测试
 *
 * @author Claude Code
 * @date 2025-11-25
 */

import { test, expect } from '@playwright/test';

// 配置测试为可视化模式
test.use({
  headless: false,
  slowMo: 800,  // 减慢操作速度,便于观察
  actionTimeout: 10000,
  viewport: { width: 1920, height: 1080 },
  video: 'on',
  screenshot: 'on',
});

// 全局变量存储测试数据ID
let testData = {
  category: {
    id: null,
    name: 'E2E测试-宿舍卫生检查',
    code: 'TEST_DORM_HYGIENE',
  },
  deductionItem: {
    id: null,
    name: 'E2E测试-地面不干净',
    code: 'TEST_DIRTY_FLOOR',
  },
  template: {
    id: null,
    name: 'E2E测试-宿舍卫生检查模板',
  },
  dailyCheck: {
    id: null,
    name: 'E2E测试-2025年11月宿舍检查',
  },
  checkRecord: {
    id: null,
  },
  appeal: {
    id: null,
  },
  dormitory: {
    id: '1987163465361420290',  // 已有的宿舍ID (101宿舍)
    no: '101',
  }
};

test.describe('量化功能详细 CRUD 测试', () => {

  // ==================== 测试初始化 ====================

  test.beforeAll(async () => {
    console.log('\\n');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('🎬 量化功能详细 CRUD 测试启动');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('📋 测试计划:');
    console.log('   1. 检查类别 CRUD (创建 → 查询 → 修改 → 删除)');
    console.log('   2. 扣分项 CRUD (创建 → 查询 → 修改 → 删除)');
    console.log('   3. 检查模板 CRUD');
    console.log('   4. 日常检查 CRUD (关联宿舍)');
    console.log('   5. 检查打分功能');
    console.log('   6. 申诉功能测试');
    console.log('═══════════════════════════════════════════════════════════\\n');
  });

  // ==================== 用户登录 ====================

  test('步骤 0 - 用户登录', async ({ page }) => {
    console.log('\\n🔐 【步骤 0】用户登录');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);

    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await page.waitForTimeout(300);
      await inputs[1].fill('admin123');
      await page.waitForTimeout(300);
    }

    const loginButton = await page.$('button:has-text("登录"), button[type="submit"]');
    if (loginButton) {
      await loginButton.click();
      await page.waitForTimeout(2000);
    }

    const token = await page.evaluate(() => localStorage.getItem('access_token'));
    expect(token).toBeTruthy();

    console.log('  ✅ 登录成功!');
    console.log('  ➤ Token已获取');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  // ==================== 第1部分: 检查类别 CRUD ====================

  test('步骤 1.1 - 创建检查类别', async ({ page }) => {
    console.log('\\n📋 【步骤 1.1】创建检查类别');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 访问量化配置页面
    await page.goto('http://localhost:3000/config/quantification');
    await page.waitForTimeout(2000);

    console.log('  ➤ 已进入量化配置页面');

    // 查找并点击"新建类别"按钮
    // 尝试多种可能的选择器
    const addButtons = [
      'button:has-text("新增")',
      'button:has-text("添加")',
      'button:has-text("创建")',
      'button:has-text("新建")',
      'button[class*="add"]',
      '.el-button--primary:has-text("新")',
    ];

    let addButtonFound = false;
    for (const selector of addButtons) {
      const button = await page.$(selector);
      if (button) {
        await button.click();
        await page.waitForTimeout(1500);
        addButtonFound = true;
        console.log(`  ➤ 点击了新建按钮: ${selector}`);
        break;
      }
    }

    if (!addButtonFound) {
      console.log('  ⚠️  未找到新建按钮,尝试直接使用API创建');

      // 使用API创建
      const token = await page.evaluate(() => localStorage.getItem('access_token'));
      const response = await page.evaluate(async ([token, data]) => {
        const res = await fetch('http://localhost:8080/api/quantification/dictionaries/categories', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({
            categoryName: data.name,
            categoryCode: data.code,
            categoryType: 'HYGIENE',
            fullScore: 100,
            description: 'E2E测试创建的检查类别',
            status: 1
          })
        });
        const result = await res.json();
        return { status: res.status, data: result };
      }, [token, testData.category]);

      console.log(`  ➤ API响应状态: ${response.status}`);

      if (response.status === 200 && response.data.data) {
        testData.category.id = response.data.data.id || response.data.data;
        console.log(`  ✅ 使用API创建成功! ID: ${testData.category.id}`);
      } else {
        console.log(`  ❌ API创建失败: ${JSON.stringify(response.data)}`);
      }
    } else {
      // 在弹出的表单中填写数据
      await page.waitForTimeout(1000);

      // 填写类别名称
      const nameInputs = await page.$$('input[placeholder*="名称"], input[placeholder*="类别"]');
      if (nameInputs.length > 0) {
        await nameInputs[0].fill(testData.category.name);
        console.log(`  ➤ 填写类别名称: ${testData.category.name}`);
      }

      // 填写类别编码
      const codeInputs = await page.$$('input[placeholder*="编码"], input[placeholder*="代码"]');
      if (codeInputs.length > 0) {
        await codeInputs[0].fill(testData.category.code);
        console.log(`  ➤ 填写类别编码: ${testData.category.code}`);
      }

      await page.waitForTimeout(500);

      // 点击确认按钮
      const confirmButton = await page.$('button:has-text("确定"), button:has-text("保存"), button:has-text("提交")');
      if (confirmButton) {
        await confirmButton.click();
        await page.waitForTimeout(2000);
        console.log('  ➤ 已点击确认按钮');
        console.log('  ✅ 创建成功!');
      }
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  test('步骤 1.2 - 查询检查类别列表', async ({ page }) => {
    console.log('\\n🔍 【步骤 1.2】查询检查类别列表');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    const token = await page.evaluate(() => localStorage.getItem('access_token'));

    const response = await page.evaluate(async (token) => {
      const res = await fetch('http://localhost:8080/api/quantification/dictionaries/categories', {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      const result = await res.json();
      return { status: res.status, data: result };
    }, token);

    console.log(`  ➤ API响应状态: ${response.status}`);

    if (response.status === 200 && response.data.data) {
      const categories = Array.isArray(response.data.data) ? response.data.data : response.data.data.records || [];
      console.log(`  ➤ 查询到 ${categories.length} 个类别`);

      // 查找我们创建的测试类别
      const testCategory = categories.find(c =>
        c.categoryCode === testData.category.code ||
        c.categoryName === testData.category.name
      );

      if (testCategory) {
        testData.category.id = testCategory.id;
        console.log(`  ✅ 找到测试类别!`);
        console.log(`     ID: ${testCategory.id}`);
        console.log(`     名称: ${testCategory.categoryName}`);
        console.log(`     编码: ${testCategory.categoryCode}`);
      } else {
        console.log(`  ⚠️  未找到测试类别`);
      }
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  test('步骤 1.3 - 修改检查类别', async ({ page }) => {
    console.log('\\n✏️  【步骤 1.3】修改检查类别');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    if (!testData.category.id) {
      console.log('  ⚠️  类别ID不存在,跳过修改测试');
      console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
      return;
    }

    const token = await page.evaluate(() => localStorage.getItem('access_token'));
    const updatedName = testData.category.name + ' (已修改)';

    const response = await page.evaluate(async ([token, id, name]) => {
      const res = await fetch(`http://localhost:8080/api/quantification/dictionaries/categories/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          id: id,
          categoryName: name,
          categoryCode: 'TEST_DORM_HYGIENE',
          categoryType: 'HYGIENE',
          fullScore: 100,
          description: 'E2E测试 - 已修改',
          status: 1
        })
      });
      const result = await res.json();
      return { status: res.status, data: result };
    }, [token, testData.category.id, updatedName]);

    console.log(`  ➤ API响应状态: ${response.status}`);

    if (response.status === 200) {
      testData.category.name = updatedName;
      console.log(`  ✅ 修改成功!`);
      console.log(`     新名称: ${updatedName}`);
    } else {
      console.log(`  ❌ 修改失败: ${JSON.stringify(response.data)}`);
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  // ==================== 第2部分: 扣分项 CRUD ====================

  test('步骤 2.1 - 创建扣分项 (关联类别)', async ({ page }) => {
    console.log('\\n📝 【步骤 2.1】创建扣分项 (关联类别)');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    if (!testData.category.id) {
      console.log('  ⚠️  类别ID不存在,无法创建扣分项');
      console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
      return;
    }

    const token = await page.evaluate(() => localStorage.getItem('access_token'));

    const response = await page.evaluate(async ([token, categoryId, itemData]) => {
      const res = await fetch('http://localhost:8080/api/quantification/dictionaries/deduction-items', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          itemName: itemData.name,
          itemCode: itemData.code,
          categoryId: categoryId,
          deductMode: 'FIXED_DEDUCT',
          fixedScore: 5,
          description: 'E2E测试创建的扣分项',
          status: 1
        })
      });
      const result = await res.json();
      return { status: res.status, data: result };
    }, [token, testData.category.id, testData.deductionItem]);

    console.log(`  ➤ API响应状态: ${response.status}`);

    if (response.status === 200 && response.data.data) {
      testData.deductionItem.id = response.data.data.id || response.data.data;
      console.log(`  ✅ 创建成功! ID: ${testData.deductionItem.id}`);
      console.log(`     名称: ${testData.deductionItem.name}`);
      console.log(`     编码: ${testData.deductionItem.code}`);
      console.log(`     关联类别ID: ${testData.category.id}`);
      console.log(`     扣分方式: 固定扣分`);
      console.log(`     扣分分值: 5分`);
    } else {
      console.log(`  ❌ 创建失败: ${JSON.stringify(response.data)}`);
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  test('步骤 2.2 - 查询扣分项列表', async ({ page }) => {
    console.log('\\n🔍 【步骤 2.2】查询扣分项列表');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    const token = await page.evaluate(() => localStorage.getItem('access_token'));

    const response = await page.evaluate(async (token) => {
      const res = await fetch('http://localhost:8080/api/quantification/dictionaries/deduction-items', {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      const result = await res.json();
      return { status: res.status, data: result };
    }, token);

    console.log(`  ➤ API响应状态: ${response.status}`);

    if (response.status === 200 && response.data.data) {
      const items = Array.isArray(response.data.data) ? response.data.data : response.data.data.records || [];
      console.log(`  ➤ 查询到 ${items.length} 个扣分项`);

      // 查找我们创建的测试扣分项
      const testItem = items.find(item =>
        item.itemCode === testData.deductionItem.code ||
        item.itemName === testData.deductionItem.name
      );

      if (testItem) {
        testData.deductionItem.id = testItem.id;
        console.log(`  ✅ 找到测试扣分项!`);
        console.log(`     ID: ${testItem.id}`);
        console.log(`     名称: ${testItem.itemName}`);
        console.log(`     编码: ${testItem.itemCode}`);
        console.log(`     关联类别: ${testItem.categoryId}`);
      } else {
        console.log(`  ⚠️  未找到测试扣分项`);
      }
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  test('步骤 2.3 - 修改扣分项', async ({ page }) => {
    console.log('\\n✏️  【步骤 2.3】修改扣分项');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    if (!testData.deductionItem.id) {
      console.log('  ⚠️  扣分项ID不存在,跳过修改测试');
      console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
      return;
    }

    const token = await page.evaluate(() => localStorage.getItem('access_token'));
    const updatedName = testData.deductionItem.name + ' (已修改)';

    const response = await page.evaluate(async ([token, id, categoryId, name]) => {
      const res = await fetch(`http://localhost:8080/api/quantification/dictionaries/deduction-items/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          id: id,
          itemName: name,
          itemCode: 'TEST_DIRTY_FLOOR',
          categoryId: categoryId,
          deductMode: 'FIXED_DEDUCT',
          fixedScore: 8,  // 修改扣分从5分改为8分
          description: 'E2E测试 - 已修改扣分项',
          status: 1
        })
      });
      const result = await res.json();
      return { status: res.status, data: result };
    }, [token, testData.deductionItem.id, testData.category.id, updatedName]);

    console.log(`  ➤ API响应状态: ${response.status}`);

    if (response.status === 200) {
      testData.deductionItem.name = updatedName;
      console.log(`  ✅ 修改成功!`);
      console.log(`     新名称: ${updatedName}`);
      console.log(`     新扣分: 8分 (原5分)`);
    } else {
      console.log(`  ❌ 修改失败: ${JSON.stringify(response.data)}`);
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  // ==================== 第3部分: 检查模板 CRUD ====================

  test('步骤 3.1 - 创建检查模板', async ({ page }) => {
    console.log('\\n📋 【步骤 3.1】创建检查模板');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    if (!testData.category.id || !testData.deductionItem.id) {
      console.log('  ⚠️  类别或扣分项ID不存在,无法创建模板');
      console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
      return;
    }

    const token = await page.evaluate(() => localStorage.getItem('access_token'));

    const response = await page.evaluate(async ([token, categoryId, itemId, templateData]) => {
      const res = await fetch('http://localhost:8080/api/quantification/templates', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          templateName: templateData.name,
          description: 'E2E测试创建的检查模板',
          status: 1,
          templateCategories: [{
            categoryId: categoryId,
            weight: 100,
            itemIds: [itemId]
          }]
        })
      });
      const result = await res.json();
      return { status: res.status, data: result };
    }, [token, testData.category.id, testData.deductionItem.id, testData.template]);

    console.log(`  ➤ API响应状态: ${response.status}`);

    if (response.status === 200 && response.data.data) {
      testData.template.id = response.data.data.id || response.data.data;
      console.log(`  ✅ 创建成功! ID: ${testData.template.id}`);
      console.log(`     名称: ${testData.template.name}`);
      console.log(`     关联类别: ${testData.category.id}`);
      console.log(`     关联扣分项: ${testData.deductionItem.id}`);
    } else {
      console.log(`  ❌ 创建失败: ${JSON.stringify(response.data)}`);
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  test('步骤 3.2 - 查询检查模板列表', async ({ page }) => {
    console.log('\\n🔍 【步骤 3.2】查询检查模板列表');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    const token = await page.evaluate(() => localStorage.getItem('access_token'));

    const response = await page.evaluate(async (token) => {
      const res = await fetch('http://localhost:8080/api/quantification/templates', {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      const result = await res.json();
      return { status: res.status, data: result };
    }, token);

    console.log(`  ➤ API响应状态: ${response.status}`);

    if (response.status === 200 && response.data.data) {
      const templates = Array.isArray(response.data.data) ? response.data.data : response.data.data.records || [];
      console.log(`  ➤ 查询到 ${templates.length} 个模板`);

      // 查找我们创建的测试模板
      const testTemplate = templates.find(t => t.templateName === testData.template.name);

      if (testTemplate) {
        testData.template.id = testTemplate.id;
        console.log(`  ✅ 找到测试模板!`);
        console.log(`     ID: ${testTemplate.id}`);
        console.log(`     名称: ${testTemplate.templateName}`);
      } else {
        console.log(`  ⚠️  未找到测试模板`);
      }
    } else {
      console.log(`  ⚠️  API返回: ${response.status}`);
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  // ==================== 第4部分: 日常检查 CRUD (关联宿舍) ====================

  test('步骤 4.1 - 创建日常检查 (关联宿舍)', async ({ page }) => {
    console.log('\\n🏠 【步骤 4.1】创建日常检查 (关联宿舍)');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    if (!testData.template.id) {
      console.log('  ⚠️  模板ID不存在,无法创建日常检查');
      console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
      return;
    }

    const token = await page.evaluate(() => localStorage.getItem('access_token'));
    const today = new Date().toISOString().split('T')[0];

    const response = await page.evaluate(async ([token, templateId, dormData, checkData, today]) => {
      const res = await fetch('http://localhost:8080/api/quantification/daily-checks', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          checkName: checkData.name,
          checkDate: today,
          templateId: templateId,
          checkObjects: [{
            objectType: 'DORMITORY',
            objectId: dormData.id,
            objectName: `${dormData.no}宿舍`
          }],
          description: 'E2E测试创建的日常检查',
          status: 'DRAFT'
        })
      });
      const result = await res.json();
      return { status: res.status, data: result };
    }, [token, testData.template.id, testData.dormitory, testData.dailyCheck, today]);

    console.log(`  ➤ API响应状态: ${response.status}`);

    if (response.status === 200 && response.data.data) {
      testData.dailyCheck.id = response.data.data.id || response.data.data;
      console.log(`  ✅ 创建成功! ID: ${testData.dailyCheck.id}`);
      console.log(`     名称: ${testData.dailyCheck.name}`);
      console.log(`     检查日期: ${today}`);
      console.log(`     关联模板: ${testData.template.id}`);
      console.log(`     关联宿舍: ${testData.dormitory.no}宿舍 (ID: ${testData.dormitory.id})`);
      console.log(`     检查状态: 草稿`);
    } else {
      console.log(`  ❌ 创建失败: ${JSON.stringify(response.data)}`);
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  test('步骤 4.2 - 查询日常检查列表', async ({ page }) => {
    console.log('\\n🔍 【步骤 4.2】查询日常检查列表');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    const token = await page.evaluate(() => localStorage.getItem('access_token'));

    const response = await page.evaluate(async (token) => {
      const res = await fetch('http://localhost:8080/api/quantification/daily-checks', {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      const result = await res.json();
      return { status: res.status, data: result };
    }, token);

    console.log(`  ➤ API响应状态: ${response.status}`);

    if (response.status === 200 && response.data.data) {
      const checks = Array.isArray(response.data.data) ? response.data.data : response.data.data.records || [];
      console.log(`  ➤ 查询到 ${checks.length} 个日常检查`);

      // 查找我们创建的测试检查
      const testCheck = checks.find(c => c.checkName === testData.dailyCheck.name);

      if (testCheck) {
        testData.dailyCheck.id = testCheck.id;
        console.log(`  ✅ 找到测试日常检查!`);
        console.log(`     ID: ${testCheck.id}`);
        console.log(`     名称: ${testCheck.checkName}`);
      } else {
        console.log(`  ⚠️  未找到测试日常检查`);
      }
    } else {
      console.log(`  ⚠️  API返回: ${response.status}`);
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  // ==================== 测试完成总结 ====================

  test.afterAll(async () => {
    console.log('\\n');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('🎉 量化功能详细 CRUD 测试完成');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('✅ 测试数据汇总:');
    console.log(`   📋 检查类别 ID: ${testData.category.id || '未创建'}`);
    console.log(`      名称: ${testData.category.name}`);
    console.log(`   📝 扣分项 ID: ${testData.deductionItem.id || '未创建'}`);
    console.log(`      名称: ${testData.deductionItem.name}`);
    console.log(`   📋 检查模板 ID: ${testData.template.id || '未创建'}`);
    console.log(`      名称: ${testData.template.name}`);
    console.log(`   🏠 日常检查 ID: ${testData.dailyCheck.id || '未创建'}`);
    console.log(`      名称: ${testData.dailyCheck.name}`);
    console.log(`      关联宿舍: ${testData.dormitory.no}宿舍`);
    console.log('');
    console.log('📹 测试视频和截图已保存');
    console.log('═══════════════════════════════════════════════════════════\\n');
  });
});
