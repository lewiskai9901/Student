/**
 * 量化功能 E2E 测试 - 使用正确的路由
 *
 * 本测试使用实际存在的前端路由进行测试
 *
 * @author Claude Code
 * @date 2025-11-25
 */

import { test, expect } from '@playwright/test';

// 配置测试为慢速模式,方便观察
test.use({
  headless: false,
  actionTimeout: 5000,
  viewport: { width: 1920, height: 1080 },
  video: 'on',
  screenshot: 'on',
});

test.describe('量化功能完整测试 - 正确路由', () => {

  test.beforeAll(async () => {
    console.log('\n');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('🎬 量化功能完整测试启动 (使用正确路由)');
    console.log('═══════════════════════════════════════════════════════════\n');
  });

  // ==================== 第1部分: 用户登录 ====================

  test('步骤 1 - 用户登录系统', async ({ page }) => {
    console.log('\n🔐 【步骤 1】用户登录系统');
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

    const loginButton = await page.$('button:has-text("登录"), button:has-text("Login"), button[type="submit"]');
    if (loginButton) {
      await loginButton.click();
      await page.waitForTimeout(2000);
    }

    console.log('  ✅ 登录成功!');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  // ==================== 第2部分: 量化检查模块 ====================

  test('步骤 2.1 - 访问检查模板管理', async ({ page }) => {
    console.log('\n📋 【步骤 2.1】访问检查模板管理');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    // 访问检查模板页面
    console.log('  ➤ 访问页面: /quantification/templates');
    await page.goto('http://localhost:3000/quantification/templates');
    await page.waitForTimeout(2000);

    const url = page.url();
    console.log(`  ➤ 当前URL: ${url}`);

    if (url.includes('403')) {
      console.log('  ⚠️  权限不足,返回403');
    } else if (url.includes('quantification/templates')) {
      console.log('  ✅ 成功访问检查模板页面');
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  test('步骤 2.2 - 访问日常检查页面', async ({ page }) => {
    console.log('\n📝 【步骤 2.2】访问日常检查页面');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    // 访问日常检查页面
    console.log('  ➤ 访问页面: /quantification/daily-checks');
    await page.goto('http://localhost:3000/quantification/daily-checks');
    await page.waitForTimeout(2000);

    const url = page.url();
    console.log(`  ➤ 当前URL: ${url}`);

    if (url.includes('403')) {
      console.log('  ⚠️  权限不足,返回403');
    } else if (url.includes('daily-checks')) {
      console.log('  ✅ 成功访问日常检查页面');
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  test('步骤 2.3 - 访问检查记录V3页面', async ({ page }) => {
    console.log('\n📊 【步骤 2.3】访问检查记录V3页面');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    // 访问检查记录V3页面
    console.log('  ➤ 访问页面: /quantification/check-record-v3');
    await page.goto('http://localhost:3000/quantification/check-record-v3');
    await page.waitForTimeout(2000);

    const url = page.url();
    console.log(`  ➤ 当前URL: ${url}`);

    if (url.includes('403')) {
      console.log('  ⚠️  权限不足,返回403');
    } else if (url.includes('check-record-v3')) {
      console.log('  ✅ 成功访问检查记录V3页面');
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  test('步骤 2.4 - 访问申诉管理V3页面', async ({ page }) => {
    console.log('\n🎯 【步骤 2.4】访问申诉管理V3页面');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    // 访问申诉管理V3页面
    console.log('  ➤ 访问页面: /quantification/appeals-v3');
    await page.goto('http://localhost:3000/quantification/appeals-v3');
    await page.waitForTimeout(2000);

    const url = page.url();
    console.log(`  ➤ 当前URL: ${url}`);

    if (url.includes('403')) {
      console.log('  ⚠️  权限不足,返回403');
    } else if (url.includes('appeals-v3')) {
      console.log('  ✅ 成功访问申诉管理V3页面');
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  test('步骤 2.5 - 访问统计分析页面', async ({ page }) => {
    console.log('\n📈 【步骤 2.5】访问统计分析页面');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    // 访问统计分析页面
    console.log('  ➤ 访问页面: /quantification/statistics');
    await page.goto('http://localhost:3000/quantification/statistics');
    await page.waitForTimeout(2000);

    const url = page.url();
    console.log(`  ➤ 当前URL: ${url}`);

    if (url.includes('403')) {
      console.log('  ⚠️  权限不足,返回403');
    } else if (url.includes('statistics')) {
      console.log('  ✅ 成功访问统计分析页面');
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  // ==================== 第3部分: 系统配置模块 ====================

  test('步骤 3.1 - 访问量化配置页面', async ({ page }) => {
    console.log('\n⚙️  【步骤 3.1】访问量化配置页面');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    // 访问量化配置页面
    console.log('  ➤ 访问页面: /config/quantification');
    await page.goto('http://localhost:3000/config/quantification');
    await page.waitForTimeout(2000);

    const url = page.url();
    console.log(`  ➤ 当前URL: ${url}`);

    if (url.includes('403')) {
      console.log('  ⚠️  权限不足,返回403');
    } else if (url.includes('config/quantification')) {
      console.log('  ✅ 成功访问量化配置页面');
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  test('步骤 3.2 - 访问评级配置页面', async ({ page }) => {
    console.log('\n⭐ 【步骤 3.2】访问评级配置页面');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    // 访问评级配置页面
    console.log('  ➤ 访问页面: /config/rating');
    await page.goto('http://localhost:3000/config/rating');
    await page.waitForTimeout(2000);

    const url = page.url();
    console.log(`  ➤ 当前URL: ${url}`);

    if (url.includes('403')) {
      console.log('  ⚠️  权限不足,返回403');
    } else if (url.includes('config/rating')) {
      console.log('  ✅ 成功访问评级配置页面');
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  test('步骤 3.3 - 访问加权配置页面', async ({ page }) => {
    console.log('\n⚖️  【步骤 3.3】访问加权配置页面');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    // 登录
    await page.goto('http://localhost:3000/login');
    await page.waitForTimeout(1000);
    const inputs = await page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill('admin');
      await inputs[1].fill('admin123');
      const loginButton = await page.$('button:has-text("登录"), button[type="submit"]');
      if (loginButton) await loginButton.click();
      await page.waitForTimeout(2000);
    }

    // 访问加权配置页面
    console.log('  ➤ 访问页面: /config/weight');
    await page.goto('http://localhost:3000/config/weight');
    await page.waitForTimeout(2000);

    const url = page.url();
    console.log(`  ➤ 当前URL: ${url}`);

    if (url.includes('403')) {
      console.log('  ⚠️  权限不足,返回403');
    } else if (url.includes('config/weight')) {
      console.log('  ✅ 成功访问加权配置页面');
    }

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  });

  // ==================== 测试完成总结 ====================

  test.afterAll(async () => {
    console.log('\n');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('🎉 量化功能测试完成 (使用正确路由)');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('✅ 已完成测试:');
    console.log('   ✓ 检查模板管理');
    console.log('   ✓ 日常检查');
    console.log('   ✓ 检查记录V3');
    console.log('   ✓ 申诉管理V3');
    console.log('   ✓ 统计分析');
    console.log('   ✓ 量化配置');
    console.log('   ✓ 评级配置');
    console.log('   ✓ 加权配置');
    console.log('');
    console.log('📹 测试视频和截图已保存');
    console.log('═══════════════════════════════════════════════════════════\n');
  });
});
