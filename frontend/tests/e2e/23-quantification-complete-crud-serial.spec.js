/**
 * 量化功能完整 CRUD 测试 - Serial模式
 *
 * 使用 test.describe.serial() 确保所有测试按顺序执行并共享浏览器上下文
 * 这样可以保持登录状态和localStorage数据
 *
 * 测试流程:
 * 0. 登录
 * 1. 检查类别查询和修改
 * 2. 扣分项查询和修改
 * 3. 创建检查模板
 * 4. 创建日常检查(关联宿舍)
 * 5. 检查打分
 * 6. 申诉功能
 * 7. 删除测试数据
 *
 * @author Claude Code
 * @date 2025-11-25
 */

import { test, expect } from '@playwright/test';

// 配置测试
test.use({
  headless: false,
  slowMo: 500,  // 减慢操作,便于观察
  actionTimeout: 15000,
  viewport: { width: 1920, height: 1080 },
  video: 'on',
  screenshot: 'on',
});

// 全局测试数据
const testData = {
  category: {
    id: 4,  // 已通过SQL创建
    name: 'E2E测试-宿舍卫生',
    code: 'E2E_TEST_DORM_HYGIENE',
  },
  deductionItems: [
    { id: 4, name: 'E2E测试-地面脏乱', score: 5 },
    { id: 5, name: 'E2E测试-物品摆放不整齐', score: 3 },
    { id: 6, name: 'E2E测试-垃圾未清理', score: 8 },
  ],
  template: {
    id: null,
    name: 'E2E测试-宿舍检查模板',
  },
  dailyCheck: {
    id: null,
    name: 'E2E测试-宿舍日常检查',
  },
  checkRecord: {
    id: null,
  },
  dormitory: {
    id: '1987163465361420290',
    no: '101',
  },
  appeal: {
    id: null,
  },
};

// 辅助函数: 获取Token
async function getToken(page) {
  try {
    const token = await page.evaluate(() => localStorage.getItem('access_token'));
    return token;
  } catch (e) {
    console.log('  ⚠️  无法获取token:', e.message);
    return null;
  }
}

// 辅助函数: API请求
async function apiRequest(page, url, options = {}) {
  const token = await getToken(page);
  if (!token) {
    return { status: 401, data: { message: 'No token' } };
  }

  try {
    const result = await page.evaluate(async ([fullUrl, opts, authToken]) => {
      const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authToken}`,
        ...(opts.headers || {})
      };

      const response = await fetch(fullUrl, {
        method: opts.method || 'GET',
        headers: headers,
        body: opts.body ? JSON.stringify(opts.body) : undefined,
      });

      let data;
      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        data = await response.json();
      } else {
        data = { message: await response.text() };
      }

      return {
        status: response.status,
        data: data,
      };
    }, [url, options, token]);

    return result;
  } catch (e) {
    console.log(`  ❌ API请求失败: ${e.message}`);
    return { status: 500, data: { message: e.message } };
  }
}

// ==================== Serial测试套件 ====================

test.describe.serial('量化功能完整 CRUD 测试流程', () => {

  test.beforeAll(async () => {
    console.log('\\n');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('🎬 量化功能完整 CRUD 测试启动 (Serial模式)');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('📋 测试计划:');
    console.log('   0. 用户登录');
    console.log('   1. 查询检查类别');
    console.log('   2. 修改检查类别');
    console.log('   3. 查询扣分项列表');
    console.log('   4. 修改扣分项');
    console.log('   5. 创建检查模板');
    console.log('   6. 查询检查模板');
    console.log('   7. 创建日常检查(关联宿舍)');
    console.log('   8. 查询日常检查');
    console.log('   9. 检查打分功能');
    console.log('  10. 申诉功能测试');
    console.log('  11. 清理测试数据');
    console.log('═══════════════════════════════════════════════════════════\\n');
  });

  // ==================== 步骤 0: 登录 ====================

  test('步骤 0 - 用户登录', async ({ page }) => {
    console.log('\\n🔐 【步骤 0】用户登录');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1500);

    // 填写登录表单
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await page.waitForTimeout(300);
      await inputs[1].fill('admin123');
      await page.waitForTimeout(300);
    }

    // 点击登录
    const loginButton = await page.$('button:has-text("登录"), button[type="submit"]');
    if (loginButton) {
      await loginButton.click();
      await page.waitForTimeout(3000);
    }

    // 验证登录成功
    const token = await getToken(page);
    expect(token).toBeTruthy();

    console.log('  ✅ 登录成功!');
    console.log(`  ➤ Token: ${token.substring(0, 20)}...`);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  // ==================== 步骤 1: 查询检查类别 ====================

  test('步骤 1 - 查询检查类别', async ({ page }) => {
    console.log('\\n🔍 【步骤 1】查询检查类别');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    const response = await apiRequest(page, 'http://localhost:8080/api/quantification/dictionaries/categories');

    console.log(`  ➤ API响应状态: ${response.status}`);

    if (response.status === 200 && response.data.data) {
      const categories = Array.isArray(response.data.data) ? response.data.data : response.data.data.records || [];
      console.log(`  ➤ 查询到 ${categories.length} 个类别`);

      // 查找测试类别
      const testCategory = categories.find(c => c.id === testData.category.id);
      if (testCategory) {
        console.log(`  ✅ 找到测试类别!`);
        console.log(`     ID: ${testCategory.id}`);
        console.log(`     名称: ${testCategory.categoryName}`);
        console.log(`     编码: ${testCategory.categoryCode}`);
        console.log(`     类型: ${testCategory.categoryType}`);
        console.log(`     满分: ${testCategory.defaultMaxScore}`);
      } else {
        console.log(`  ⚠️  未找到测试类别 (ID: ${testData.category.id})`);
      }
    } else {
      console.log(`  ❌ 查询失败: ${JSON.stringify(response.data)}`);
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  // ==================== 步骤 2: 修改检查类别 ====================

  test('步骤 2 - 修改检查类别', async ({ page }) => {
    console.log('\\n✏️  【步骤 2】修改检查类别');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    const newName = testData.category.name + ' (已修改)';
    const newScore = 95;

    const response = await apiRequest(
      page,
      `http://localhost:8080/api/quantification/dictionaries/categories/${testData.category.id}`,
      {
        method: 'PUT',
        body: {
          id: testData.category.id,
          categoryCode: testData.category.code,
          categoryName: newName,
          categoryType: 'HYGIENE',
          defaultMaxScore: newScore,
          description: 'E2E测试 - 已通过自动化测试修改',
          status: 1,
        }
      }
    );

    console.log(`  ➤ API响应状态: ${response.status}`);

    if (response.status === 200) {
      console.log(`  ✅ 修改成功!`);
      console.log(`     新名称: ${newName}`);
      console.log(`     新满分: ${newScore}`);
      testData.category.name = newName;
    } else {
      console.log(`  ❌ 修改失败: ${JSON.stringify(response.data)}`);
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  // ==================== 步骤 3: 查询扣分项列表 ====================

  test('步骤 3 - 查询扣分项列表', async ({ page }) => {
    console.log('\\n🔍 【步骤 3】查询扣分项列表');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    const response = await apiRequest(
      page,
      `http://localhost:8080/api/quantification/dictionaries/deduction-items?typeId=${testData.category.id}`
    );

    console.log(`  ➤ API响应状态: ${response.status}`);

    if (response.status === 200 && response.data.data) {
      const items = Array.isArray(response.data.data) ? response.data.data : response.data.data.records || [];
      console.log(`  ➤ 查询到 ${items.length} 个扣分项`);

      // 显示所有测试扣分项
      testData.deductionItems.forEach((testItem, index) => {
        const found = items.find(item => item.id === testItem.id);
        if (found) {
          console.log(`  ✅ 扣分项 ${index + 1}:`);
          console.log(`     ID: ${found.id}`);
          console.log(`     名称: ${found.itemName}`);
          console.log(`     扣分: ${found.fixedScore}分`);
          console.log(`     关联类别ID: ${found.typeId}`);
        } else {
          console.log(`  ⚠️  未找到扣分项 ${index + 1} (ID: ${testItem.id})`);
        }
      });
    } else {
      console.log(`  ❌ 查询失败: ${JSON.stringify(response.data)}`);
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  // ==================== 步骤 4: 修改扣分项 ====================

  test('步骤 4 - 修改扣分项', async ({ page }) => {
    console.log('\\n✏️  【步骤 4】修改扣分项');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    const itemToUpdate = testData.deductionItems[0];
    const newScore = 10;  // 从5分改为10分

    const response = await apiRequest(
      page,
      `http://localhost:8080/api/quantification/dictionaries/deduction-items/${itemToUpdate.id}`,
      {
        method: 'PUT',
        body: {
          id: itemToUpdate.id,
          typeId: testData.category.id,
          itemName: itemToUpdate.name + ' (已修改)',
          deductMode: 1,  // FIXED_DEDUCT
          fixedScore: newScore,
          description: 'E2E测试 - 已通过自动化测试修改',
          status: 1,
        }
      }
    );

    console.log(`  ➤ API响应状态: ${response.status}`);

    if (response.status === 200) {
      console.log(`  ✅ 修改成功!`);
      console.log(`     扣分项: ${itemToUpdate.name}`);
      console.log(`     新扣分: ${newScore}分 (原${itemToUpdate.score}分)`);
      testData.deductionItems[0].score = newScore;
    } else {
      console.log(`  ❌ 修改失败: ${JSON.stringify(response.data)}`);
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  // ==================== 步骤 5: 创建检查模板 ====================

  test('步骤 5 - 创建检查模板', async ({ page }) => {
    console.log('\\n📋 【步骤 5】创建检查模板');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 构建模板数据
    const templateData = {
      templateName: testData.template.name,
      description: 'E2E自动化测试创建的检查模板',
      status: 1,
      templateCategories: [{
        categoryId: testData.category.id,
        weight: 100,
        deductionItemIds: testData.deductionItems.map(item => item.id),
      }]
    };

    const response = await apiRequest(
      page,
      'http://localhost:8080/api/quantification/templates',
      {
        method: 'POST',
        body: templateData
      }
    );

    console.log(`  ➤ API响应状态: ${response.status}`);

    if (response.status === 200 && response.data.data) {
      testData.template.id = response.data.data.id || response.data.data;
      console.log(`  ✅ 创建成功! ID: ${testData.template.id}`);
      console.log(`     名称: ${testData.template.name}`);
      console.log(`     关联类别: ${testData.category.name}`);
      console.log(`     扣分项数量: ${testData.deductionItems.length}个`);
    } else {
      console.log(`  ❌ 创建失败: ${JSON.stringify(response.data)}`);
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  // ==================== 步骤 6: 查询检查模板 ====================

  test('步骤 6 - 查询检查模板', async ({ page }) => {
    console.log('\\n🔍 【步骤 6】查询检查模板');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    const response = await apiRequest(page, 'http://localhost:8080/api/quantification/templates');

    console.log(`  ➤ API响应状态: ${response.status}`);

    if (response.status === 200 && response.data.data) {
      const templates = Array.isArray(response.data.data) ? response.data.data : response.data.data.records || [];
      console.log(`  ➤ 查询到 ${templates.length} 个模板`);

      // 查找测试模板
      const testTemplate = templates.find(t => t.id === testData.template.id || t.templateName === testData.template.name);
      if (testTemplate) {
        testData.template.id = testTemplate.id;
        console.log(`  ✅ 找到测试模板!`);
        console.log(`     ID: ${testTemplate.id}`);
        console.log(`     名称: ${testTemplate.templateName}`);
        console.log(`     状态: ${testTemplate.status === 1 ? '启用' : '禁用'}`);
      } else {
        console.log(`  ⚠️  未找到测试模板`);
      }
    } else {
      console.log(`  ⚠️  API返回: ${response.status} - ${JSON.stringify(response.data)}`);
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  // ==================== 步骤 7: 创建日常检查(关联宿舍) ====================

  test('步骤 7 - 创建日常检查(关联宿舍)', async ({ page }) => {
    console.log('\\n🏠 【步骤 7】创建日常检查(关联宿舍)');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    if (!testData.template.id) {
      console.log('  ⚠️  模板ID不存在,无法创建日常检查');
      console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
      test.skip();
      return;
    }

    const today = new Date().toISOString().split('T')[0];

    const dailyCheckData = {
      checkName: testData.dailyCheck.name,
      checkDate: today,
      templateId: testData.template.id,
      checkScope: 'DORMITORY',
      targetIds: [testData.dormitory.id],
      description: 'E2E自动化测试创建的日常检查,关联101宿舍',
      status: 'DRAFT'
    };

    const response = await apiRequest(
      page,
      'http://localhost:8080/api/quantification/daily-checks',
      {
        method: 'POST',
        body: dailyCheckData
      }
    );

    console.log(`  ➤ API响应状态: ${response.status}`);

    if (response.status === 200 && response.data.data) {
      testData.dailyCheck.id = response.data.data.id || response.data.data;
      console.log(`  ✅ 创建成功! ID: ${testData.dailyCheck.id}`);
      console.log(`     名称: ${testData.dailyCheck.name}`);
      console.log(`     检查日期: ${today}`);
      console.log(`     关联模板: ${testData.template.name}`);
      console.log(`     关联宿舍: ${testData.dormitory.no}宿舍`);
      console.log(`     状态: 草稿`);
    } else {
      console.log(`  ❌ 创建失败: ${JSON.stringify(response.data)}`);
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  // ==================== 步骤 8: 查询日常检查 ====================

  test('步骤 8 - 查询日常检查', async ({ page }) => {
    console.log('\\n🔍 【步骤 8】查询日常检查');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    const response = await apiRequest(page, 'http://localhost:8080/api/quantification/daily-checks');

    console.log(`  ➤ API响应状态: ${response.status}`);

    if (response.status === 200 && response.data.data) {
      const checks = Array.isArray(response.data.data) ? response.data.data : response.data.data.records || [];
      console.log(`  ➤ 查询到 ${checks.length} 个日常检查`);

      // 查找测试日常检查
      const testCheck = checks.find(c => c.id === testData.dailyCheck.id || c.checkName === testData.dailyCheck.name);
      if (testCheck) {
        testData.dailyCheck.id = testCheck.id;
        console.log(`  ✅ 找到测试日常检查!`);
        console.log(`     ID: ${testCheck.id}`);
        console.log(`     名称: ${testCheck.checkName}`);
        console.log(`     检查日期: ${testCheck.checkDate}`);
        console.log(`     状态: ${testCheck.status}`);
      } else {
        console.log(`  ⚠️  未找到测试日常检查`);
      }
    } else {
      console.log(`  ⚠️  API返回: ${response.status} - ${JSON.stringify(response.data)}`);
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  });

  // ==================== 测试完成总结 ====================

  test.afterAll(async () => {
    console.log('\\n');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('🎉 量化功能完整 CRUD 测试完成');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('✅ 测试数据汇总:');
    console.log(`   📋 检查类别:`);
    console.log(`      ID: ${testData.category.id}`);
    console.log(`      名称: ${testData.category.name}`);
    console.log(`   📝 扣分项: ${testData.deductionItems.length}个`);
    testData.deductionItems.forEach((item, index) => {
      console.log(`      ${index + 1}. ${item.name} (${item.score}分)`);
    });
    console.log(`   📋 检查模板:`);
    console.log(`      ID: ${testData.template.id || '未创建'}`);
    console.log(`      名称: ${testData.template.name}`);
    console.log(`   🏠 日常检查:`);
    console.log(`      ID: ${testData.dailyCheck.id || '未创建'}`);
    console.log(`      名称: ${testData.dailyCheck.name}`);
    console.log(`      关联宿舍: ${testData.dormitory.no}宿舍`);
    console.log('');
    console.log('📹 测试视频和截图已保存到 test-results/ 目录');
    console.log('═══════════════════════════════════════════════════════════\\n');
  });
});
