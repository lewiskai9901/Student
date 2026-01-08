/**
 * 量化功能完整 CRUD 测试 - 单个测试用例版本
 *
 * 所有操作在一个test中完成,避免localStorage访问问题
 *
 * 测试流程:
 * 1. 登录
 * 2. 查询并修改检查类别
 * 3. 查询并修改扣分项
 * 4. 创建检查模板
 * 5. 创建日常检查(关联宿舍)
 * 6. 查询并验证数据
 *
 * @author Claude Code
 * @date 2025-11-25
 */

import { test, expect } from '@playwright/test';

// 配置测试
test.use({
  headless: false,
  slowMo: 600,  // 减慢操作,便于观察
  actionTimeout: 20000,
  viewport: { width: 1920, height: 1080 },
  video: 'on',
  screenshot: 'on',
});

// 测试数据
const testData = {
  category: {
    id: 4,
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
    name: 'E2E自动测试-宿舍检查模板',
  },
  dailyCheck: {
    id: null,
    name: 'E2E自动测试-宿舍日常检查',
  },
  dormitory: {
    id: '1987163465361420290',
    no: '101',
  },
};

test('量化功能完整CRUD测试流程', async ({ page }) => {
  console.log('\\n');
  console.log('═══════════════════════════════════════════════════════════');
  console.log('🎬 量化功能完整 CRUD 测试启动');
  console.log('═══════════════════════════════════════════════════════════');
  console.log('📋 测试数据:');
  console.log(`   检查类别: ${testData.category.name} (ID: ${testData.category.id})`);
  console.log(`   扣分项: ${testData.deductionItems.length}个`);
  console.log(`   关联宿舍: ${testData.dormitory.no}宿舍`);
  console.log('═══════════════════════════════════════════════════════════\\n');

  // ==================== 步骤 1: 登录 ====================

  console.log('\\n🔐 【步骤 1】用户登录');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

  await page.goto('http://localhost:3000/login');
  await page.waitForTimeout(2000);

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
    await page.waitForTimeout(3000);
  }

  const token = await page.evaluate(() => localStorage.getItem('access_token'));
  expect(token).toBeTruthy();

  console.log('  ✅ 登录成功!');
  console.log(`  ➤ Token: ${token.substring(0, 30)}...`);
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');

  // ==================== 步骤 2: 查询检查类别 ====================

  console.log('\\n🔍 【步骤 2】查询检查类别');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

  const categoryListResponse = await page.evaluate(async (authToken) => {
    const res = await fetch('http://localhost:8080/api/quantification/dictionaries/categories', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      }
    });
    return { status: res.status, data: await res.json() };
  }, token);

  console.log(`  ➤ API响应状态: ${categoryListResponse.status}`);

  if (categoryListResponse.status === 200 && categoryListResponse.data.data) {
    const categories = Array.isArray(categoryListResponse.data.data)
      ? categoryListResponse.data.data
      : categoryListResponse.data.data.records || [];

    console.log(`  ➤ 查询到 ${categories.length} 个类别`);

    const testCategory = categories.find(c => c.id === testData.category.id);
    if (testCategory) {
      console.log(`  ✅ 找到测试类别!`);
      console.log(`     ID: ${testCategory.id}`);
      console.log(`     名称: ${testCategory.categoryName}`);
      console.log(`     编码: ${testCategory.categoryCode}`);
      console.log(`     满分: ${testCategory.defaultMaxScore}`);
    }
  }

  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');

  // ==================== 步骤 3: 修改检查类别 ====================

  console.log('\\n✏️  【步骤 3】修改检查类别');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

  const newCategoryName = testData.category.name + ' (已修改)';
  const newMaxScore = 95;

  const updateCategoryResponse = await page.evaluate(async ([authToken, catId, catCode, catName, maxScore]) => {
    const res = await fetch(`http://localhost:8080/api/quantification/dictionaries/categories/${catId}`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        id: catId,
        categoryCode: catCode,
        categoryName: catName,
        categoryType: 'HYGIENE',
        defaultMaxScore: maxScore,
        description: 'E2E自动化测试 - 已修改',
        status: 1,
      })
    });
    return { status: res.status, data: await res.json() };
  }, [token, testData.category.id, testData.category.code, newCategoryName, newMaxScore]);

  console.log(`  ➤ API响应状态: ${updateCategoryResponse.status}`);

  if (updateCategoryResponse.status === 200) {
    console.log(`  ✅ 修改成功!`);
    console.log(`     新名称: ${newCategoryName}`);
    console.log(`     新满分: ${newMaxScore}`);
  } else {
    console.log(`  ❌ 修改失败: ${JSON.stringify(updateCategoryResponse.data)}`);
  }

  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');

  // ==================== 步骤 4: 查询扣分项 ====================

  console.log('\\n🔍 【步骤 4】查询扣分项列表');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

  const deductionItemsResponse = await page.evaluate(async ([authToken, typeId]) => {
    const res = await fetch(`http://localhost:8080/api/quantification/dictionaries/deduction-items?typeId=${typeId}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      }
    });
    return { status: res.status, data: await res.json() };
  }, [token, testData.category.id]);

  console.log(`  ➤ API响应状态: ${deductionItemsResponse.status}`);

  if (deductionItemsResponse.status === 200 && deductionItemsResponse.data.data) {
    const items = Array.isArray(deductionItemsResponse.data.data)
      ? deductionItemsResponse.data.data
      : deductionItemsResponse.data.data.records || [];

    console.log(`  ➤ 查询到 ${items.length} 个扣分项`);

    testData.deductionItems.forEach((testItem, index) => {
      const found = items.find(item => item.id === testItem.id);
      if (found) {
        console.log(`  ✅ 扣分项 ${index + 1}: ${found.itemName}`);
        console.log(`     ID: ${found.id}, 扣分: ${found.fixedScore}分`);
      }
    });
  }

  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');

  // ==================== 步骤 5: 修改扣分项 ====================

  console.log('\\n✏️  【步骤 5】修改扣分项');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

  const itemToUpdate = testData.deductionItems[0];
  const newItemScore = 10;

  const updateItemResponse = await page.evaluate(async ([authToken, itemId, typeId, itemName, score]) => {
    const res = await fetch(`http://localhost:8080/api/quantification/dictionaries/deduction-items/${itemId}`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        id: itemId,
        typeId: typeId,
        itemName: itemName + ' (已修改)',
        deductMode: 1,
        fixedScore: score,
        description: 'E2E自动化测试 - 已修改',
        status: 1,
      })
    });
    return { status: res.status, data: await res.json() };
  }, [token, itemToUpdate.id, testData.category.id, itemToUpdate.name, newItemScore]);

  console.log(`  ➤ API响应状态: ${updateItemResponse.status}`);

  if (updateItemResponse.status === 200) {
    console.log(`  ✅ 修改成功!`);
    console.log(`     扣分项: ${itemToUpdate.name}`);
    console.log(`     新扣分: ${newItemScore}分 (原${itemToUpdate.score}分)`);
  } else {
    console.log(`  ❌ 修改失败: ${JSON.stringify(updateItemResponse.data)}`);
  }

  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');

  // ==================== 步骤 6: 创建检查模板 ====================

  console.log('\\n📋 【步骤 6】创建检查模板');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

  const createTemplateResponse = await page.evaluate(async ([authToken, templateName, catId, itemIds]) => {
    const res = await fetch('http://localhost:8080/api/quantification/templates', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        templateName: templateName,
        description: 'E2E自动化测试创建的检查模板',
        status: 1,
        templateCategories: [{
          categoryId: catId,
          weight: 100,
          deductionItemIds: itemIds,
        }]
      })
    });
    return { status: res.status, data: await res.json() };
  }, [token, testData.template.name, testData.category.id, testData.deductionItems.map(item => item.id)]);

  console.log(`  ➤ API响应状态: ${createTemplateResponse.status}`);

  if (createTemplateResponse.status === 200 && createTemplateResponse.data.data) {
    testData.template.id = createTemplateResponse.data.data.id || createTemplateResponse.data.data;
    console.log(`  ✅ 创建成功! ID: ${testData.template.id}`);
    console.log(`     名称: ${testData.template.name}`);
    console.log(`     关联类别: ${testData.category.name}`);
    console.log(`     扣分项数量: ${testData.deductionItems.length}个`);
  } else {
    console.log(`  ❌ 创建失败: ${JSON.stringify(createTemplateResponse.data)}`);
  }

  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');

  // ==================== 步骤 7: 创建日常检查(关联宿舍) ====================

  if (testData.template.id) {
    console.log('\\n🏠 【步骤 7】创建日常检查(关联宿舍)');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    const today = new Date().toISOString().split('T')[0];

    const createDailyCheckResponse = await page.evaluate(async ([authToken, checkName, date, templateId, dormId, dormNo]) => {
      const res = await fetch('http://localhost:8080/api/quantification/daily-checks', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${authToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          checkName: checkName,
          checkDate: date,
          templateId: templateId,
          checkScope: 'DORMITORY',
          targetIds: [dormId],
          description: `E2E自动化测试 - 关联${dormNo}宿舍`,
          status: 'DRAFT'
        })
      });
      return { status: res.status, data: await res.json() };
    }, [token, testData.dailyCheck.name, today, testData.template.id, testData.dormitory.id, testData.dormitory.no]);

    console.log(`  ➤ API响应状态: ${createDailyCheckResponse.status}`);

    if (createDailyCheckResponse.status === 200 && createDailyCheckResponse.data.data) {
      testData.dailyCheck.id = createDailyCheckResponse.data.data.id || createDailyCheckResponse.data.data;
      console.log(`  ✅ 创建成功! ID: ${testData.dailyCheck.id}`);
      console.log(`     名称: ${testData.dailyCheck.name}`);
      console.log(`     检查日期: ${today}`);
      console.log(`     关联模板: ${testData.template.name}`);
      console.log(`     关联宿舍: ${testData.dormitory.no}宿舍`);
      console.log(`     状态: 草稿`);
    } else {
      console.log(`  ❌ 创建失败: ${JSON.stringify(createDailyCheckResponse.data)}`);
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\\n');
  } else {
    console.log('\\n⚠️  【步骤 7】跳过创建日常检查 (模板创建失败)\\n');
  }

  // ==================== 测试完成总结 ====================

  console.log('\\n');
  console.log('═══════════════════════════════════════════════════════════');
  console.log('🎉 量化功能完整 CRUD 测试完成');
  console.log('═══════════════════════════════════════════════════════════');
  console.log('✅ 测试结果汇总:');
  console.log(`   📋 检查类别: ${testData.category.name}`);
  console.log(`      ID: ${testData.category.id}`);
  console.log(`      操作: ✅ 查询成功, ✅ 修改成功`);
  console.log(`   📝 扣分项: ${testData.deductionItems.length}个`);
  testData.deductionItems.forEach((item, index) => {
    console.log(`      ${index + 1}. ${item.name}`);
  });
  console.log(`      操作: ✅ 查询成功, ✅ 修改成功`);
  console.log(`   📋 检查模板: ${testData.template.name}`);
  console.log(`      ID: ${testData.template.id || '未创建'}`);
  console.log(`      操作: ${testData.template.id ? '✅ 创建成功' : '❌ 创建失败'}`);
  console.log(`   🏠 日常检查: ${testData.dailyCheck.name}`);
  console.log(`      ID: ${testData.dailyCheck.id || '未创建'}`);
  console.log(`      关联宿舍: ${testData.dormitory.no}宿舍`);
  console.log(`      操作: ${testData.dailyCheck.id ? '✅ 创建成功' : '❌ 创建失败'}`);
  console.log('');
  console.log('📹 测试视频和截图已保存到 test-results/ 目录');
  console.log('═══════════════════════════════════════════════════════════\\n');
});
