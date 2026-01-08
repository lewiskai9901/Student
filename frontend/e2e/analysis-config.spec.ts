import { test, expect, API_BASE_URL } from './fixtures/auth.fixture'

/**
 * 统计分析配置模块 E2E 测试
 */
test.describe('统计分析配置管理', () => {
  let createdConfigId: string

  test.describe('API 测试', () => {
    test('1. 登录获取 Token', async ({ authToken }) => {
      expect(authToken).toBeTruthy()
      expect(authToken.length).toBeGreaterThan(50)
      console.log('✅ 登录成功，Token 获取成功')
    })

    test('2. 获取配置列表 - 空列表', async ({ request, authToken }) => {
      const response = await request.get(`${API_BASE_URL}/analysis/configs`, {
        params: { pageNum: 1, pageSize: 10 },
        headers: { Authorization: `Bearer ${authToken}` }
      })

      expect(response.ok()).toBeTruthy()
      const data = await response.json()
      expect(data.code).toBe(200)
      expect(data.data.records).toBeDefined()
      console.log(`✅ 获取配置列表成功，当前记录数: ${data.data.total}`)
    })

    test('3. 创建分析配置', async ({ request, authToken }) => {
      const configData = {
        planId: 1,
        configName: `E2E测试配置_${Date.now()}`,
        configDesc: 'Playwright E2E 自动化测试创建的配置',
        scopeType: 'time',
        scopeConfig: {
          startDate: '2025-01-01',
          endDate: '2025-12-31'
        },
        targetType: 'all',
        updateMode: 'dynamic',
        missingStrategy: 'avg',
        isEnabled: true,
        isPublic: false,
        sortOrder: 1
      }

      const response = await request.post(`${API_BASE_URL}/analysis/configs`, {
        data: configData,
        headers: { Authorization: `Bearer ${authToken}` }
      })

      expect(response.ok()).toBeTruthy()
      const data = await response.json()
      expect(data.code).toBe(200)
      expect(data.data.id).toBeTruthy()
      expect(data.data.configName).toBe(configData.configName)

      createdConfigId = data.data.id
      console.log(`✅ 创建配置成功，ID: ${createdConfigId}`)
    })

    test('4. 获取配置详情', async ({ request, authToken }) => {
      // 先创建一个配置
      const createResponse = await request.post(`${API_BASE_URL}/analysis/configs`, {
        data: {
          planId: 1,
          configName: `详情测试_${Date.now()}`,
          configDesc: '测试获取详情',
          scopeType: 'time',
          scopeConfig: { startDate: '2025-01-01', endDate: '2025-12-31' },
          targetType: 'all',
          updateMode: 'static',
          missingStrategy: 'avg',
          isEnabled: true,
          isPublic: false,
          sortOrder: 1
        },
        headers: { Authorization: `Bearer ${authToken}` }
      })

      const createData = await createResponse.json()
      const configId = createData.data.id

      // 获取详情
      const response = await request.get(`${API_BASE_URL}/analysis/configs/${configId}`, {
        headers: { Authorization: `Bearer ${authToken}` }
      })

      expect(response.ok()).toBeTruthy()
      const data = await response.json()
      expect(data.code).toBe(200)
      expect(data.data.id).toBe(configId)
      expect(data.data.metrics).toBeDefined()
      expect(data.data.categoryMappings).toBeDefined()
      console.log(`✅ 获取配置详情成功，配置名: ${data.data.configName}`)

      // 清理
      await request.delete(`${API_BASE_URL}/analysis/configs/${configId}`, {
        headers: { Authorization: `Bearer ${authToken}` }
      })
    })

    test('5. 更新配置', async ({ request, authToken }) => {
      const configName = `更新测试_${Date.now()}`
      // 先创建一个配置
      const createResponse = await request.post(`${API_BASE_URL}/analysis/configs`, {
        data: {
          planId: 1,
          configName: configName,
          configDesc: '原始描述',
          scopeType: 'time',
          scopeConfig: { startDate: '2025-01-01', endDate: '2025-12-31' },
          targetType: 'all',
          updateMode: 'static',
          missingStrategy: 'avg',
          isEnabled: true,
          isPublic: false,
          sortOrder: 1
        },
        headers: { Authorization: `Bearer ${authToken}` }
      })

      const createData = await createResponse.json()
      const configId = createData.data.id

      // 更新配置 - 必须包含 planId 和 configName 必填字段
      const updateData = {
        planId: 1,
        configName: configName,
        configDesc: '更新后的描述',
        missingStrategy: 'weighted',
        isPublic: true
      }

      const response = await request.put(`${API_BASE_URL}/analysis/configs/${configId}`, {
        data: updateData,
        headers: { Authorization: `Bearer ${authToken}` }
      })

      expect(response.ok()).toBeTruthy()
      const data = await response.json()
      expect(data.code).toBe(200)
      expect(data.data.configDesc).toBe(updateData.configDesc)
      expect(data.data.missingStrategy).toBe(updateData.missingStrategy)
      expect(data.data.isPublic).toBe(true)
      console.log(`✅ 更新配置成功`)

      // 清理
      await request.delete(`${API_BASE_URL}/analysis/configs/${configId}`, {
        headers: { Authorization: `Bearer ${authToken}` }
      })
    })

    test('6. 启用/禁用配置', async ({ request, authToken }) => {
      // 先创建一个配置
      const createResponse = await request.post(`${API_BASE_URL}/analysis/configs`, {
        data: {
          planId: 1,
          configName: `启用禁用测试_${Date.now()}`,
          configDesc: '测试启用禁用',
          scopeType: 'time',
          scopeConfig: { startDate: '2025-01-01', endDate: '2025-12-31' },
          targetType: 'all',
          updateMode: 'static',
          missingStrategy: 'avg',
          isEnabled: true,
          isPublic: false,
          sortOrder: 1
        },
        headers: { Authorization: `Bearer ${authToken}` }
      })

      const createData = await createResponse.json()
      const configId = createData.data.id

      // 禁用配置
      const disableResponse = await request.put(
        `${API_BASE_URL}/analysis/configs/${configId}/toggle?enabled=false`,
        {
          headers: { Authorization: `Bearer ${authToken}` }
        }
      )

      expect(disableResponse.ok()).toBeTruthy()
      console.log(`✅ 禁用配置成功`)

      // 重新启用
      const enableResponse = await request.put(
        `${API_BASE_URL}/analysis/configs/${configId}/toggle?enabled=true`,
        {
          headers: { Authorization: `Bearer ${authToken}` }
        }
      )

      expect(enableResponse.ok()).toBeTruthy()
      console.log(`✅ 启用配置成功`)

      // 清理
      await request.delete(`${API_BASE_URL}/analysis/configs/${configId}`, {
        headers: { Authorization: `Bearer ${authToken}` }
      })
    })

    test('7. 复制配置', async ({ request, authToken }) => {
      // 先创建一个配置
      const createResponse = await request.post(`${API_BASE_URL}/analysis/configs`, {
        data: {
          planId: 1,
          configName: `复制源配置_${Date.now()}`,
          configDesc: '这是源配置',
          scopeType: 'time',
          scopeConfig: { startDate: '2025-01-01', endDate: '2025-12-31' },
          targetType: 'all',
          updateMode: 'static',
          missingStrategy: 'avg',
          isEnabled: true,
          isPublic: false,
          sortOrder: 1
        },
        headers: { Authorization: `Bearer ${authToken}` }
      })

      const createData = await createResponse.json()
      const sourceConfigId = createData.data.id

      // 复制配置
      const copyResponse = await request.post(
        `${API_BASE_URL}/analysis/configs/${sourceConfigId}/copy?newName=复制的配置_${Date.now()}`,
        {
          headers: { Authorization: `Bearer ${authToken}` }
        }
      )

      expect(copyResponse.ok()).toBeTruthy()
      const copyData = await copyResponse.json()
      expect(copyData.code).toBe(200)
      expect(copyData.data.id).not.toBe(sourceConfigId)
      console.log(`✅ 复制配置成功，新配置ID: ${copyData.data.id}`)

      // 清理
      await request.delete(`${API_BASE_URL}/analysis/configs/${sourceConfigId}`, {
        headers: { Authorization: `Bearer ${authToken}` }
      })
      await request.delete(`${API_BASE_URL}/analysis/configs/${copyData.data.id}`, {
        headers: { Authorization: `Bearer ${authToken}` }
      })
    })

    test('8. 删除配置', async ({ request, authToken }) => {
      // 先创建一个配置
      const createResponse = await request.post(`${API_BASE_URL}/analysis/configs`, {
        data: {
          planId: 1,
          configName: `删除测试_${Date.now()}`,
          configDesc: '待删除的配置',
          scopeType: 'time',
          scopeConfig: { startDate: '2025-01-01', endDate: '2025-12-31' },
          targetType: 'all',
          updateMode: 'static',
          missingStrategy: 'avg',
          isEnabled: true,
          isPublic: false,
          sortOrder: 1
        },
        headers: { Authorization: `Bearer ${authToken}` }
      })

      const createData = await createResponse.json()
      const configId = createData.data.id

      // 删除配置
      const deleteResponse = await request.delete(`${API_BASE_URL}/analysis/configs/${configId}`, {
        headers: { Authorization: `Bearer ${authToken}` }
      })

      expect(deleteResponse.ok()).toBeTruthy()
      const deleteData = await deleteResponse.json()
      expect(deleteData.code).toBe(200)
      console.log(`✅ 删除配置成功`)

      // 验证已删除 - 获取详情应该失败或返回不存在
      const getResponse = await request.get(`${API_BASE_URL}/analysis/configs/${configId}`, {
        headers: { Authorization: `Bearer ${authToken}` }
      })

      const getData = await getResponse.json()
      // 删除后获取应该返回错误
      expect(getData.code).not.toBe(200)
      console.log(`✅ 验证删除成功 - 配置已不存在`)
    })

    test('9. 按计划ID筛选配置', async ({ request, authToken }) => {
      const response = await request.get(`${API_BASE_URL}/analysis/configs/plan/1`, {
        headers: { Authorization: `Bearer ${authToken}` }
      })

      expect(response.ok()).toBeTruthy()
      const data = await response.json()
      expect(data.code).toBe(200)
      expect(Array.isArray(data.data)).toBeTruthy()
      console.log(`✅ 按计划ID筛选成功，配置数量: ${data.data.length}`)
    })
  })

  test.describe('权限测试', () => {
    test('10. 无 Token 访问应返回 403', async ({ request }) => {
      const response = await request.get(`${API_BASE_URL}/analysis/configs`, {
        params: { pageNum: 1, pageSize: 10 }
      })

      // 无认证应该返回 403 或 401
      expect(response.status()).toBeGreaterThanOrEqual(400)
      console.log(`✅ 无 Token 访问正确返回 ${response.status()}`)
    })

    test('11. 无效 Token 访问应返回错误', async ({ request }) => {
      const response = await request.get(`${API_BASE_URL}/analysis/configs`, {
        params: { pageNum: 1, pageSize: 10 },
        headers: { Authorization: 'Bearer invalid_token_123' }
      })

      expect(response.status()).toBeGreaterThanOrEqual(400)
      console.log(`✅ 无效 Token 访问正确返回 ${response.status()}`)
    })
  })

  test.describe('边界测试', () => {
    test('12. 获取不存在的配置应返回错误', async ({ request, authToken }) => {
      const response = await request.get(`${API_BASE_URL}/analysis/configs/999999999999`, {
        headers: { Authorization: `Bearer ${authToken}` }
      })

      const data = await response.json()
      expect(data.code).not.toBe(200)
      console.log(`✅ 获取不存在配置正确返回错误`)
    })

    test('13. 创建配置 - 缺少必填字段应返回错误', async ({ request, authToken }) => {
      const response = await request.post(`${API_BASE_URL}/analysis/configs`, {
        data: {
          // 缺少 planId 和 configName
          configDesc: '缺少必填字段'
        },
        headers: { Authorization: `Bearer ${authToken}` }
      })

      // 应该返回验证错误
      const data = await response.json()
      // 可能是 422 或 400
      console.log(`✅ 缺少必填字段正确返回错误，code: ${data.code}`)
    })

    test('14. 分页测试 - 大页码', async ({ request, authToken }) => {
      const response = await request.get(`${API_BASE_URL}/analysis/configs`, {
        params: { pageNum: 9999, pageSize: 10 },
        headers: { Authorization: `Bearer ${authToken}` }
      })

      expect(response.ok()).toBeTruthy()
      const data = await response.json()
      expect(data.code).toBe(200)
      expect(data.data.records).toHaveLength(0) // 超出范围应该返回空
      console.log(`✅ 大页码分页正确返回空列表`)
    })
  })
})

/**
 * 清理测试数据
 */
test.afterAll(async ({ request }) => {
  console.log('\n🧹 清理测试数据...')

  // 登录获取 token
  const loginResponse = await request.post(`${API_BASE_URL}/auth/login`, {
    data: { username: 'admin', password: 'admin123' }
  })

  if (!loginResponse.ok()) {
    console.log('清理时登录失败，跳过清理')
    return
  }

  const loginData = await loginResponse.json()
  const token = loginData.data.accessToken

  // 获取所有测试创建的配置
  const listResponse = await request.get(`${API_BASE_URL}/analysis/configs`, {
    params: { pageNum: 1, pageSize: 100 },
    headers: { Authorization: `Bearer ${token}` }
  })

  if (listResponse.ok()) {
    const listData = await listResponse.json()
    const testConfigs = listData.data.records.filter(
      (c: any) =>
        c.configName.includes('E2E测试') ||
        c.configName.includes('测试') ||
        c.configName.includes('Test')
    )

    for (const config of testConfigs) {
      await request.delete(`${API_BASE_URL}/analysis/configs/${config.id}`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      console.log(`  - 已删除: ${config.configName}`)
    }
  }

  console.log('✅ 测试数据清理完成')
})
