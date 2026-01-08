/**
 * 量化检查测试辅助类
 * 提供量化检查相关的API调用和UI操作封装
 */

export class QuantificationHelper {
  constructor(page) {
    this.page = page;
    this.baseUrl = 'http://localhost:8080/api';
  }

  /**
   * 获取当前登录用户的Token
   */
  async getToken() {
    return await this.page.evaluate(() => localStorage.getItem('token'));
  }

  /**
   * 发起带认证的API请求
   */
  async apiRequest(method, endpoint, data = null) {
    let token = await this.getToken();

    // 如果没有token，尝试先登录
    if (!token) {
      console.log('⚠️ Token不存在，尝试重新登录获取...');
      // 直接通过API获取token
      const loginResponse = await this.page.request.post(`${this.baseUrl}/auth/login`, {
        data: { username: 'admin', password: 'admin123' }
      });
      const loginData = await loginResponse.json();
      if (loginData.code === 200 && loginData.data?.accessToken) {
        token = loginData.data.accessToken;
        console.log('✅ 重新获取Token成功');
      } else {
        throw new Error('无法获取认证Token');
      }
    }

    const options = {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    };
    if (data) {
      options.data = data;
    }

    const url = `${this.baseUrl}${endpoint}`;
    let response;
    switch (method.toUpperCase()) {
      case 'GET':
        response = await this.page.request.get(url, options);
        break;
      case 'POST':
        response = await this.page.request.post(url, options);
        break;
      case 'PUT':
        response = await this.page.request.put(url, options);
        break;
      case 'DELETE':
        response = await this.page.request.delete(url, options);
        break;
      default:
        throw new Error(`Unsupported method: ${method}`);
    }

    // 检查响应状态
    if (!response.ok()) {
      console.log(`⚠️ API请求失败: ${response.status()} ${response.statusText()}`);
      const text = await response.text();
      console.log(`   响应内容: ${text.substring(0, 200)}`);
      return { code: response.status(), message: response.statusText() };
    }

    const text = await response.text();
    if (!text) {
      return { code: 200, data: null };
    }

    try {
      return JSON.parse(text);
    } catch (e) {
      console.log(`⚠️ JSON解析失败: ${text.substring(0, 100)}`);
      return { code: 500, message: 'JSON parse error' };
    }
  }

  // ==================== 检查类别 API ====================

  /**
   * 获取所有启用的检查类别
   */
  async getEnabledCategories() {
    return await this.apiRequest('GET', '/quantification/dictionaries/categories/enabled');
  }

  /**
   * 创建检查类别
   */
  async createCategory(data) {
    const categoryData = {
      categoryName: data.name || `E2E测试类别_${Date.now()}`,
      categoryCode: data.code || `E2E_CAT_${Date.now()}`,
      categoryType: data.type || 'DORMITORY',
      maxScore: data.maxScore || 100,
      description: data.description || 'E2E自动化测试创建',
      status: data.status ?? 1
    };
    return await this.apiRequest('POST', '/quantification/dictionaries/categories', categoryData);
  }

  /**
   * 获取类别详情
   */
  async getCategoryById(id) {
    return await this.apiRequest('GET', `/quantification/dictionaries/categories/${id}`);
  }

  /**
   * 更新检查类别
   */
  async updateCategory(id, data) {
    return await this.apiRequest('PUT', `/quantification/dictionaries/categories/${id}`, data);
  }

  /**
   * 删除检查类别
   */
  async deleteCategory(id) {
    return await this.apiRequest('DELETE', `/quantification/dictionaries/categories/${id}`);
  }

  // ==================== 扣分项 API ====================

  /**
   * 获取类别下的扣分项
   */
  async getItemsByCategory(categoryId) {
    return await this.apiRequest('GET', `/quantification/dictionaries/items/by-category/${categoryId}`);
  }

  /**
   * 创建扣分项
   * 注意: categoryId使用字符串类型，避免JavaScript大整数精度丢失
   */
  async createDeductionItem(data) {
    const score = data.standardScore || data.defaultDeductScore || 5;
    const itemData = {
      categoryId: String(data.categoryId), // 保持字符串避免精度丢失
      itemName: data.name || `E2E测试扣分项_${Date.now()}`,
      itemCode: data.code || `E2E_ITEM_${Date.now()}`,
      itemDescription: data.description || 'E2E自动化测试创建',
      deductMode: data.deductMode || 1, // 1-固定扣分
      defaultDeductScore: score,
      minDeductScore: data.minDeductScore || score,
      maxDeductScore: data.maxDeductScore || score,
      checkPoints: data.checkPoints || '检查要点',
      sortOrder: data.sortOrder || 0
    };
    return await this.apiRequest('POST', '/quantification/dictionaries/items', itemData);
  }

  /**
   * 更新扣分项
   */
  async updateDeductionItem(id, data) {
    return await this.apiRequest('PUT', `/quantification/dictionaries/items/${id}`, data);
  }

  /**
   * 删除扣分项
   */
  async deleteDeductionItem(id) {
    return await this.apiRequest('DELETE', `/quantification/dictionaries/items/${id}`);
  }

  // ==================== 检查模板 API ====================

  /**
   * 获取模板列表
   */
  async getTemplates(pageNum = 1, pageSize = 10) {
    return await this.apiRequest('GET', `/quantification/templates?pageNum=${pageNum}&pageSize=${pageSize}`);
  }

  /**
   * 创建检查模板
   */
  async createTemplate(data) {
    const templateData = {
      templateName: data.name || `E2E测试模板_${Date.now()}`,
      templateCode: data.code || `E2E_TPL_${Date.now()}`,
      description: data.description || 'E2E自动化测试创建',
      status: data.status ?? 1,
      isDefault: data.isDefault ?? 0,
      categories: data.categories || []
    };
    return await this.apiRequest('POST', '/quantification/templates', templateData);
  }

  /**
   * 获取模板详情
   */
  async getTemplateById(id) {
    return await this.apiRequest('GET', `/quantification/templates/${id}`);
  }

  /**
   * 更新模板
   */
  async updateTemplate(id, data) {
    return await this.apiRequest('PUT', `/quantification/templates/${id}`, data);
  }

  /**
   * 删除模板
   */
  async deleteTemplate(id) {
    return await this.apiRequest('DELETE', `/quantification/templates/${id}`);
  }

  // ==================== 检查计划 API ====================

  /**
   * 获取检查计划列表
   */
  async getCheckPlans(pageNum = 1, pageSize = 10) {
    return await this.apiRequest('GET', `/quantification/daily-checks?pageNum=${pageNum}&pageSize=${pageSize}`);
  }

  /**
   * 创建检查计划
   */
  async createCheckPlan(data) {
    const planData = {
      checkName: data.name || `E2E检查计划_${Date.now()}`,
      checkDate: data.date || new Date().toISOString().split('T')[0],
      templateId: data.templateId,
      checkType: data.checkType || 1,
      targets: data.targets || [],
      description: data.description || 'E2E自动化测试创建'
    };
    return await this.apiRequest('POST', '/quantification/daily-checks', planData);
  }

  /**
   * 获取检查计划详情
   */
  async getCheckPlanById(id) {
    return await this.apiRequest('GET', `/quantification/daily-checks/${id}`);
  }

  // ==================== 检查记录 API ====================

  /**
   * 获取检查记录列表
   */
  async getCheckRecords(pageNum = 1, pageSize = 10) {
    return await this.apiRequest('GET', `/quantification/check-records-v3/list?pageNum=${pageNum}&pageSize=${pageSize}`);
  }

  /**
   * 获取检查记录详情
   */
  async getCheckRecordById(id) {
    return await this.apiRequest('GET', `/quantification/check-records-v3/${id}`);
  }

  // ==================== 申诉管理 API ====================

  /**
   * 获取申诉列表
   */
  async getAppeals(pageNum = 1, pageSize = 10) {
    return await this.apiRequest('GET', `/quantification/appeals-v3?pageNum=${pageNum}&pageSize=${pageSize}`);
  }

  /**
   * 获取待审核申诉
   */
  async getPendingAppeals() {
    return await this.apiRequest('GET', '/quantification/appeals-v3/pending');
  }

  // ==================== 评级配置 API ====================

  /**
   * 获取评级配置列表
   */
  async getRatingConfigs(pageNum = 1, pageSize = 10) {
    return await this.apiRequest('GET', `/quantification/rating-config/configs?pageNum=${pageNum}&pageSize=${pageSize}`);
  }

  // ==================== 加权配置 API ====================

  /**
   * 获取加权配置列表
   */
  async getWeightConfigs(pageNum = 1, pageSize = 10) {
    return await this.apiRequest('GET', `/quantification/weight-config/configs?pageNum=${pageNum}&pageSize=${pageSize}`);
  }

  // ==================== 统计分析 API ====================

  /**
   * 获取分析配置列表
   */
  async getAnalysisConfigs(pageNum = 1, pageSize = 10) {
    return await this.apiRequest('GET', `/quantification/analysis/configs?pageNum=${pageNum}&pageSize=${pageSize}`);
  }

  /**
   * 创建分析配置
   */
  async createAnalysisConfig(data) {
    return await this.apiRequest('POST', '/quantification/analysis/configs', data);
  }

  /**
   * 生成分析报告
   */
  async generateReport(configId) {
    return await this.apiRequest('GET', `/quantification/analysis/report/${configId}`);
  }

  // ==================== UI 操作辅助方法 ====================

  /**
   * 等待表格加载完成
   */
  async waitForTableLoad() {
    await this.page.waitForSelector('.el-table');
    await this.page.waitForTimeout(500); // 等待数据渲染
  }

  /**
   * 点击新增按钮
   */
  async clickAddButton(text = '新增') {
    await this.page.click(`button:has-text("${text}")`);
    await this.page.waitForSelector('.el-dialog');
  }

  /**
   * 点击确定按钮
   */
  async clickConfirmButton() {
    await this.page.click('.el-dialog button:has-text("确定")');
  }

  /**
   * 等待成功消息
   */
  async waitForSuccessMessage() {
    await this.page.waitForSelector('.el-message--success', { timeout: 5000 });
  }

  /**
   * 填写输入框
   */
  async fillInput(placeholder, value) {
    await this.page.fill(`input[placeholder*="${placeholder}"]`, value);
  }

  /**
   * 选择下拉框选项
   */
  async selectOption(labelText, optionText) {
    await this.page.click(`.el-select:has-text("${labelText}")`);
    await this.page.waitForSelector('.el-select-dropdown__item');
    await this.page.click(`.el-select-dropdown__item:has-text("${optionText}")`);
  }

  /**
   * 点击表格行中的按钮
   */
  async clickTableRowButton(rowText, buttonText) {
    await this.page.click(`.el-table__row:has-text("${rowText}") button:has-text("${buttonText}")`);
  }

  /**
   * 确认删除对话框
   */
  async confirmDelete() {
    await this.page.click('.el-message-box__btns button:has-text("确定")');
    await this.waitForSuccessMessage();
  }

  /**
   * 清理测试数据 - 删除以E2E开头的类别
   */
  async cleanupTestData() {
    try {
      const categories = await this.getEnabledCategories();
      if (categories.data) {
        for (const cat of categories.data) {
          if (cat.categoryCode && cat.categoryCode.startsWith('E2E_')) {
            await this.deleteCategory(cat.id);
          }
        }
      }
    } catch (error) {
      console.log('清理测试数据时发生错误:', error.message);
    }
  }
}
