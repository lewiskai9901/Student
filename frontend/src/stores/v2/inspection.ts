import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  InspectionTemplate,
  TemplateStatus,
  InspectionCategory,
  DeductionItem,
  InspectionRecord,
  RecordStatus,
  ClassScore,
  Appeal,
  AppealStatus
} from '@/types/v2/inspection'
import {
  templateApi,
  recordApi,
  appealApi
} from '@/api/v2/inspection'

/**
 * 量化检查 Store (V2)
 * 基于DDD架构的量化检查状态管理
 */
export const useInspectionStore = defineStore('inspection-v2', () => {
  // ==================== 模板状态 ====================
  const templates = ref<InspectionTemplate[]>([])
  const currentTemplate = ref<InspectionTemplate | null>(null)
  const templatesLoading = ref(false)
  const templatesTotal = ref(0)

  // ==================== 记录状态 ====================
  const records = ref<InspectionRecord[]>([])
  const currentRecord = ref<InspectionRecord | null>(null)
  const recordsLoading = ref(false)
  const recordsTotal = ref(0)

  // ==================== 申诉状态 ====================
  const appeals = ref<Appeal[]>([])
  const currentAppeal = ref<Appeal | null>(null)
  const appealsLoading = ref(false)
  const appealsTotal = ref(0)
  const appealStatistics = ref<{
    pending: number
    reviewing: number
    approved: number
    rejected: number
  } | null>(null)

  // ==================== 计算属性 ====================

  // 已发布的模板
  const publishedTemplates = computed(() =>
    templates.value.filter(t => t.status === 'PUBLISHED')
  )

  // 草稿模板
  const draftTemplates = computed(() =>
    templates.value.filter(t => t.status === 'DRAFT')
  )

  // 待审核的记录
  const pendingRecords = computed(() =>
    records.value.filter(r => r.status === 'PENDING')
  )

  // 已发布的记录
  const publishedRecords = computed(() =>
    records.value.filter(r => r.status === 'PUBLISHED')
  )

  // 待处理的申诉
  const pendingAppeals = computed(() =>
    appeals.value.filter(a =>
      a.status === 'PENDING' ||
      a.status === 'REVIEWING' ||
      a.status === 'DEPT_REVIEWING' ||
      a.status === 'FINAL_REVIEWING'
    )
  )

  // ==================== 模板操作 ====================

  /**
   * 加载模板列表
   */
  const loadTemplates = async (params?: Parameters<typeof templateApi.getList>[0]) => {
    templatesLoading.value = true
    try {
      const data = await templateApi.getList(params)
      // request.ts 已解包，data 直接是响应数据
      // 兼容两种响应格式：直接数组 或 分页对象 {items/records, total}
      if (Array.isArray(data)) {
        templates.value = data
        templatesTotal.value = data.length
      } else if (data) {
        templates.value = data.items || data.records || []
        templatesTotal.value = data.total || 0
      }
    } catch (error) {
      console.error('加载模板列表失败:', error)
    } finally {
      templatesLoading.value = false
    }
  }

  /**
   * 获取模板详情
   */
  const getTemplate = async (id: number) => {
    try {
      const data = await templateApi.getById(id)
      currentTemplate.value = data || null
      return data
    } catch (error) {
      console.error('获取模板详情失败:', error)
      return null
    }
  }

  /**
   * 创建模板
   */
  const createTemplate = async (data: Parameters<typeof templateApi.create>[0]) => {
    const result = await templateApi.create(data)
    await loadTemplates()
    return result
  }

  /**
   * 更新模板
   */
  const updateTemplate = async (id: number, data: Parameters<typeof templateApi.update>[1]) => {
    const result = await templateApi.update(id, data)
    await getTemplate(id)
    return result
  }

  /**
   * 发布模板
   */
  const publishTemplate = async (id: number) => {
    const result = await templateApi.publish(id)
    await getTemplate(id)
    return result
  }

  /**
   * 归档模板
   */
  const archiveTemplate = async (id: number) => {
    const result = await templateApi.archive(id)
    await getTemplate(id)
    return result
  }

  /**
   * 添加检查类别
   */
  const addCategory = async (
    templateId: number,
    data: Parameters<typeof templateApi.addCategory>[1]
  ) => {
    const result = await templateApi.addCategory(templateId, data)
    await getTemplate(templateId)
    return result
  }

  /**
   * 删除模板
   */
  const deleteTemplate = async (id: number) => {
    const result = await templateApi.delete(id)
    await loadTemplates()
    return result
  }

  // ==================== 记录操作 ====================

  /**
   * 加载记录列表
   */
  const loadRecords = async (params?: Parameters<typeof recordApi.getList>[0]) => {
    recordsLoading.value = true
    try {
      const data = await recordApi.getList(params)
      // request.ts 已解包，data 直接是响应数据
      if (Array.isArray(data)) {
        records.value = data
        recordsTotal.value = data.length
      } else if (data) {
        records.value = data.items || data.records || []
        recordsTotal.value = data.total || 0
      }
    } catch (error) {
      console.error('加载记录列表失败:', error)
    } finally {
      recordsLoading.value = false
    }
  }

  /**
   * 获取记录详情
   */
  const getRecord = async (id: number) => {
    try {
      const data = await recordApi.getById(id)
      currentRecord.value = data || null
      return data
    } catch (error) {
      console.error('获取记录详情失败:', error)
      return null
    }
  }

  /**
   * 创建记录
   */
  const createRecord = async (data: Parameters<typeof recordApi.create>[0]) => {
    const result = await recordApi.create(data)
    await loadRecords()
    return result
  }

  /**
   * 添加班级得分
   */
  const addClassScore = async (
    recordId: number,
    data: Parameters<typeof recordApi.addClassScore>[1]
  ) => {
    const result = await recordApi.addClassScore(recordId, data)
    await getRecord(recordId)
    return result
  }

  /**
   * 记录扣分
   */
  const recordDeduction = async (
    recordId: number,
    data: Parameters<typeof recordApi.recordDeduction>[1]
  ) => {
    const result = await recordApi.recordDeduction(recordId, data)
    await getRecord(recordId)
    return result
  }

  /**
   * 提交记录审核
   */
  const submitRecord = async (id: number) => {
    const result = await recordApi.submit(id)
    await loadRecords()
    return result
  }

  /**
   * 发布记录
   */
  const publishRecord = async (id: number) => {
    const result = await recordApi.publish(id)
    await loadRecords()
    return result
  }

  /**
   * 拒绝记录
   */
  const rejectRecord = async (id: number, reason: string) => {
    const result = await recordApi.reject(id, { reason })
    await loadRecords()
    return result
  }

  /**
   * 删除记录
   */
  const deleteRecord = async (id: number) => {
    const result = await recordApi.delete(id)
    await loadRecords()
    return result
  }

  // ==================== 申诉操作 ====================

  /**
   * 加载申诉列表
   */
  const loadAppeals = async (params?: Parameters<typeof appealApi.getList>[0]) => {
    appealsLoading.value = true
    try {
      const data = await appealApi.getList(params)
      // request.ts 已解包，data 直接是响应数据
      if (Array.isArray(data)) {
        appeals.value = data
        appealsTotal.value = data.length
      } else if (data) {
        appeals.value = data.items || data.records || []
        appealsTotal.value = data.total || 0
      }
    } catch (error) {
      console.error('加载申诉列表失败:', error)
    } finally {
      appealsLoading.value = false
    }
  }

  /**
   * 获取申诉详情
   */
  const getAppeal = async (id: number) => {
    try {
      const data = await appealApi.getById(id)
      currentAppeal.value = data || null
      return data
    } catch (error) {
      console.error('获取申诉详情失败:', error)
      return null
    }
  }

  /**
   * 创建申诉
   */
  const createAppeal = async (data: Parameters<typeof appealApi.create>[0]) => {
    const result = await appealApi.create(data)
    await loadAppeals()
    return result
  }

  /**
   * 提交审核
   */
  const submitForReview = async (id: number) => {
    const result = await appealApi.submitForReview(id)
    await getAppeal(id)
    return result
  }

  /**
   * 审核申诉
   */
  const reviewAppeal = async (
    id: number,
    data: Parameters<typeof appealApi.review>[1]
  ) => {
    const result = await appealApi.review(id, data)
    await getAppeal(id)
    return result
  }

  /**
   * 终审申诉
   */
  const finalReviewAppeal = async (
    id: number,
    data: Parameters<typeof appealApi.finalReview>[1]
  ) => {
    const result = await appealApi.finalReview(id, data)
    await getAppeal(id)
    return result
  }

  /**
   * 加载申诉统计
   */
  const loadAppealStatistics = async () => {
    try {
      const data = await appealApi.getStatistics()
      appealStatistics.value = data || null
    } catch (error) {
      console.error('加载申诉统计失败:', error)
    }
  }

  /**
   * 加载我的申诉
   */
  const loadMyAppeals = async (params?: Parameters<typeof appealApi.getMyAppeals>[0]) => {
    appealsLoading.value = true
    try {
      const data = await appealApi.getMyAppeals(params)
      // request.ts 已解包，data 直接是响应数据
      if (Array.isArray(data)) {
        appeals.value = data
        appealsTotal.value = data.length
      } else if (data) {
        appeals.value = data.items || data.records || []
        appealsTotal.value = data.total || 0
      }
    } catch (error) {
      console.error('加载我的申诉失败:', error)
    } finally {
      appealsLoading.value = false
    }
  }

  /**
   * 加载待审核申诉
   */
  const loadPendingReviewAppeals = async (params?: Parameters<typeof appealApi.getPendingReview>[0]) => {
    appealsLoading.value = true
    try {
      const data = await appealApi.getPendingReview(params)
      // request.ts 已解包，data 直接是响应数据
      if (Array.isArray(data)) {
        appeals.value = data
        appealsTotal.value = data.length
      } else if (data) {
        appeals.value = data.items || data.records || []
        appealsTotal.value = data.total || 0
      }
    } catch (error) {
      console.error('加载待审核申诉失败:', error)
    } finally {
      appealsLoading.value = false
    }
  }

  // ==================== 重置状态 ====================
  const reset = () => {
    templates.value = []
    currentTemplate.value = null
    templatesTotal.value = 0
    records.value = []
    currentRecord.value = null
    recordsTotal.value = 0
    appeals.value = []
    currentAppeal.value = null
    appealsTotal.value = 0
    appealStatistics.value = null
  }

  return {
    // 模板状态
    templates,
    currentTemplate,
    templatesLoading,
    templatesTotal,

    // 记录状态
    records,
    currentRecord,
    recordsLoading,
    recordsTotal,

    // 申诉状态
    appeals,
    currentAppeal,
    appealsLoading,
    appealsTotal,
    appealStatistics,

    // 计算属性
    publishedTemplates,
    draftTemplates,
    pendingRecords,
    publishedRecords,
    pendingAppeals,

    // 模板操作
    loadTemplates,
    getTemplate,
    createTemplate,
    updateTemplate,
    publishTemplate,
    archiveTemplate,
    addCategory,
    deleteTemplate,

    // 记录操作
    loadRecords,
    getRecord,
    createRecord,
    addClassScore,
    recordDeduction,
    submitRecord,
    publishRecord,
    rejectRecord,
    deleteRecord,

    // 申诉操作
    loadAppeals,
    getAppeal,
    createAppeal,
    submitForReview,
    reviewAppeal,
    finalReviewAppeal,
    loadAppealStatistics,
    loadMyAppeals,
    loadPendingReviewAppeals,

    // 工具方法
    reset
  }
})
