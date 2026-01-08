import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  Task,
  TaskDetail,
  TaskStatistics,
  TaskProgressNode,
  WorkflowTemplate,
  SystemMessage,
  TaskStatus,
  TaskQueryParams,
  CreateTaskRequest,
  TaskSubmitRequest,
  TaskApproveRequest,
  CreateWorkflowTemplateRequest,
  UpdateWorkflowTemplateRequest,
  SystemMessageQueryParams
} from '@/types/v2/task'
import { taskApi, workflowTemplateApi, systemMessageApi } from '@/api/v2/task'

/**
 * 任务管理 Store (V2)
 * 基于DDD架构的任务管理状态管理
 */
export const useTaskStore = defineStore('task-v2', () => {
  // ==================== 任务状态 ====================
  const tasks = ref<Task[]>([])
  const currentTask = ref<Task | null>(null)
  const taskDetail = ref<TaskDetail | null>(null)
  const tasksLoading = ref(false)
  const tasksTotal = ref(0)

  // ==================== 我的任务状态 ====================
  const myTasks = ref<Task[]>([])
  const myTasksLoading = ref(false)
  const myTasksTotal = ref(0)

  // ==================== 待审批状态 ====================
  const pendingApprovalTasks = ref<Task[]>([])
  const pendingApprovalLoading = ref(false)
  const pendingApprovalTotal = ref(0)

  // ==================== 统计状态 ====================
  const statistics = ref<TaskStatistics | null>(null)
  const statisticsLoading = ref(false)

  // ==================== 工作流模板状态 ====================
  const workflowTemplates = ref<WorkflowTemplate[]>([])
  const currentTemplate = ref<WorkflowTemplate | null>(null)
  const templatesLoading = ref(false)
  const templatesTotal = ref(0)

  // ==================== 系统消息状态 ====================
  const messages = ref<SystemMessage[]>([])
  const messagesLoading = ref(false)
  const messagesTotal = ref(0)
  const unreadCount = ref(0)

  // ==================== 计算属性 ====================

  // 按状态过滤任务
  const tasksByStatus = computed(() => {
    return (status: TaskStatus) => tasks.value.filter(t => t.status === status)
  })

  // 待接收任务
  const pendingTasks = computed(() => tasksByStatus.value(0))

  // 进行中任务
  const inProgressTasks = computed(() => tasksByStatus.value(1))

  // 已完成任务
  const completedTasks = computed(() => tasksByStatus.value(3))

  // 超期任务
  const overdueTasks = computed(() => tasks.value.filter(t => t.overdue))

  // 启用的工作流模板
  const enabledTemplates = computed(() =>
    workflowTemplates.value.filter(t => t.status === 1)
  )

  // 未读消息
  const unreadMessages = computed(() =>
    messages.value.filter(m => !m.isRead)
  )

  // ==================== 任务操作 ====================

  /**
   * 加载任务列表
   */
  const loadTasks = async (params?: TaskQueryParams) => {
    tasksLoading.value = true
    try {
      const data = await taskApi.getList(params)
      // request.ts 已解包，data 直接是响应数据
      if (data) {
        tasks.value = data.records || []
        tasksTotal.value = data.total || 0
      }
    } catch (error) {
      console.error('加载任务列表失败:', error)
    } finally {
      tasksLoading.value = false
    }
  }

  /**
   * 获取任务详情
   */
  const getTask = async (id: number) => {
    try {
      const data = await taskApi.getById(id)
      currentTask.value = data || null
      return data
    } catch (error) {
      console.error('获取任务详情失败:', error)
      return null
    }
  }

  /**
   * 获取任务详情（含卡片式执行人数据）
   */
  const getTaskDetail = async (id: number) => {
    try {
      const data = await taskApi.getDetail(id)
      taskDetail.value = data || null
      return data
    } catch (error) {
      console.error('获取任务详情失败:', error)
      return null
    }
  }

  /**
   * 创建任务
   */
  const createTask = async (data: CreateTaskRequest) => {
    const result = await taskApi.create(data)
    await loadTasks()
    return result
  }

  /**
   * 取消任务
   */
  const cancelTask = async (id: number, reason?: string) => {
    const result = await taskApi.cancel(id, reason)
    await getTask(id)
    return result
  }

  /**
   * 接收任务
   */
  const acceptTask = async (id: number) => {
    const result = await taskApi.accept(id)
    await getTask(id)
    await loadMyTasks()
    return result
  }

  /**
   * 提交任务
   */
  const submitTask = async (data: TaskSubmitRequest) => {
    const result = await taskApi.submit(data)
    await getTask(data.taskId)
    await loadMyTasks()
    return result
  }

  /**
   * 审批任务
   */
  const approveTask = async (data: TaskApproveRequest) => {
    const result = await taskApi.approve(data)
    await getTask(data.taskId)
    await loadPendingApprovalTasks()
    return result
  }

  /**
   * 获取任务进度
   */
  const getTaskProgress = async (id: number) => {
    try {
      const data = await taskApi.getProgress(id)
      return Array.isArray(data) ? data : []
    } catch (error) {
      console.error('获取任务进度失败:', error)
      return []
    }
  }

  // ==================== 我的任务操作 ====================

  /**
   * 加载我的任务
   */
  const loadMyTasks = async (params?: { pageNum?: number; pageSize?: number; status?: number }) => {
    myTasksLoading.value = true
    try {
      const data = await taskApi.getMyTasks(params)
      if (data) {
        myTasks.value = data.records || []
        myTasksTotal.value = data.total || 0
      }
    } catch (error) {
      console.error('加载我的任务失败:', error)
    } finally {
      myTasksLoading.value = false
    }
  }

  /**
   * 加载待审批任务
   */
  const loadPendingApprovalTasks = async (params?: { pageNum?: number; pageSize?: number }) => {
    pendingApprovalLoading.value = true
    try {
      const data = await taskApi.getPendingApproval(params)
      if (data) {
        pendingApprovalTasks.value = data.records || []
        pendingApprovalTotal.value = data.total || 0
      }
    } catch (error) {
      console.error('加载待审批任务失败:', error)
    } finally {
      pendingApprovalLoading.value = false
    }
  }

  // ==================== 统计操作 ====================

  /**
   * 加载任务统计
   */
  const loadStatistics = async () => {
    statisticsLoading.value = true
    try {
      const data = await taskApi.getStatistics()
      statistics.value = data || null
    } catch (error) {
      console.error('加载任务统计失败:', error)
    } finally {
      statisticsLoading.value = false
    }
  }

  // ==================== 工作流模板操作 ====================

  /**
   * 加载工作流模板列表
   */
  const loadWorkflowTemplates = async (params?: { pageNum?: number; pageSize?: number; status?: number }) => {
    templatesLoading.value = true
    try {
      const data = await workflowTemplateApi.getList(params)
      if (data) {
        workflowTemplates.value = data.records || []
        templatesTotal.value = data.total || 0
      }
    } catch (error) {
      console.error('加载工作流模板失败:', error)
    } finally {
      templatesLoading.value = false
    }
  }

  /**
   * 获取启用的工作流模板
   */
  const getEnabledTemplates = async () => {
    try {
      const data = await workflowTemplateApi.getEnabled()
      return Array.isArray(data) ? data : []
    } catch (error) {
      console.error('获取启用的工作流模板失败:', error)
      return []
    }
  }

  /**
   * 获取工作流模板详情
   */
  const getWorkflowTemplate = async (id: number) => {
    try {
      const data = await workflowTemplateApi.getById(id)
      currentTemplate.value = data || null
      return data
    } catch (error) {
      console.error('获取工作流模板详情失败:', error)
      return null
    }
  }

  /**
   * 创建工作流模板
   */
  const createWorkflowTemplate = async (data: CreateWorkflowTemplateRequest) => {
    const result = await workflowTemplateApi.create(data)
    await loadWorkflowTemplates()
    return result
  }

  /**
   * 更新工作流模板
   */
  const updateWorkflowTemplate = async (id: number, data: UpdateWorkflowTemplateRequest) => {
    const result = await workflowTemplateApi.update(id, data)
    await getWorkflowTemplate(id)
    return result
  }

  /**
   * 删除工作流模板
   */
  const deleteWorkflowTemplate = async (id: number) => {
    const result = await workflowTemplateApi.delete(id)
    await loadWorkflowTemplates()
    return result
  }

  /**
   * 启用工作流模板
   */
  const enableWorkflowTemplate = async (id: number) => {
    const result = await workflowTemplateApi.enable(id)
    await loadWorkflowTemplates()
    return result
  }

  /**
   * 禁用工作流模板
   */
  const disableWorkflowTemplate = async (id: number) => {
    const result = await workflowTemplateApi.disable(id)
    await loadWorkflowTemplates()
    return result
  }

  // ==================== 系统消息操作 ====================

  /**
   * 加载系统消息
   */
  const loadMessages = async (params?: SystemMessageQueryParams) => {
    messagesLoading.value = true
    try {
      const data = await systemMessageApi.getList(params)
      if (data) {
        messages.value = data.records || []
        messagesTotal.value = data.total || 0
      }
    } catch (error) {
      console.error('加载系统消息失败:', error)
    } finally {
      messagesLoading.value = false
    }
  }

  /**
   * 获取未读消息数量
   */
  const loadUnreadCount = async () => {
    try {
      const data = await systemMessageApi.getUnreadCount()
      unreadCount.value = data || 0
    } catch (error) {
      console.error('获取未读消息数量失败:', error)
    }
  }

  /**
   * 标记消息为已读
   */
  const markAsRead = async (id: number) => {
    await systemMessageApi.markAsRead(id)
    await loadUnreadCount()
    // 更新本地状态
    const message = messages.value.find(m => m.id === id)
    if (message) {
      message.isRead = true
    }
  }

  /**
   * 标记所有消息为已读
   */
  const markAllAsRead = async () => {
    await systemMessageApi.markAllAsRead()
    unreadCount.value = 0
    messages.value.forEach(m => {
      m.isRead = true
    })
  }

  // ==================== 重置状态 ====================
  const reset = () => {
    tasks.value = []
    currentTask.value = null
    taskDetail.value = null
    tasksTotal.value = 0
    myTasks.value = []
    myTasksTotal.value = 0
    pendingApprovalTasks.value = []
    pendingApprovalTotal.value = 0
    statistics.value = null
    workflowTemplates.value = []
    currentTemplate.value = null
    templatesTotal.value = 0
    messages.value = []
    messagesTotal.value = 0
    unreadCount.value = 0
  }

  return {
    // 任务状态
    tasks,
    currentTask,
    taskDetail,
    tasksLoading,
    tasksTotal,

    // 我的任务状态
    myTasks,
    myTasksLoading,
    myTasksTotal,

    // 待审批状态
    pendingApprovalTasks,
    pendingApprovalLoading,
    pendingApprovalTotal,

    // 统计状态
    statistics,
    statisticsLoading,

    // 工作流模板状态
    workflowTemplates,
    currentTemplate,
    templatesLoading,
    templatesTotal,

    // 系统消息状态
    messages,
    messagesLoading,
    messagesTotal,
    unreadCount,

    // 计算属性
    tasksByStatus,
    pendingTasks,
    inProgressTasks,
    completedTasks,
    overdueTasks,
    enabledTemplates,
    unreadMessages,

    // 任务操作
    loadTasks,
    getTask,
    getTaskDetail,
    createTask,
    cancelTask,
    acceptTask,
    submitTask,
    approveTask,
    getTaskProgress,

    // 我的任务操作
    loadMyTasks,
    loadPendingApprovalTasks,

    // 统计操作
    loadStatistics,

    // 工作流模板操作
    loadWorkflowTemplates,
    getEnabledTemplates,
    getWorkflowTemplate,
    createWorkflowTemplate,
    updateWorkflowTemplate,
    deleteWorkflowTemplate,
    enableWorkflowTemplate,
    disableWorkflowTemplate,

    // 系统消息操作
    loadMessages,
    loadUnreadCount,
    markAsRead,
    markAllAsRead,

    // 工具方法
    reset
  }
})
