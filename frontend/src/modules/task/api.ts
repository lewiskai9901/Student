/**
 * 任务管理 API - DDD架构适配
 *
 * 注意: 响应拦截器已解包 ApiResponse，API 直接返回 data 内容
 */
import { http } from '@/utils/request'
import type {
  Task,
  TaskDetail,
  TaskStatistics,
  TaskProgressNode,
  WorkflowTemplate,
  SystemMessage,
  CreateTaskRequest,
  TaskQueryParams,
  TaskSubmitRequest,
  TaskApproveRequest,
  CreateWorkflowTemplateRequest,
  UpdateWorkflowTemplateRequest,
  SystemMessageQueryParams
} from '@/types/v2/task'
import type { PageResponse } from '@/types/v2'

// 后端API路径 - V2 DDD架构
const TASK_URL = '/v2/tasks'
const WORKFLOW_URL = '/task/workflow-templates'  // 工作流模板暂用V1端点
const MESSAGE_URL = '/task/messages'  // 系统消息暂用V1端点

// ==================== 任务 CRUD ====================

/**
 * 分页查询任务
 */
export function getTasks(params?: TaskQueryParams): Promise<PageResponse<Task>> {
  return http.get<PageResponse<Task>>(TASK_URL, { params })
}

/**
 * 获取任务详情
 */
export function getTask(id: number): Promise<Task> {
  return http.get<Task>(`${TASK_URL}/${id}`)
}

/**
 * 获取任务详情（含卡片式执行人数据）
 */
export function getTaskDetail(id: number): Promise<TaskDetail> {
  return http.get<TaskDetail>(`${TASK_URL}/${id}/detail`)
}

/**
 * 创建任务
 */
export function createTask(data: CreateTaskRequest): Promise<Task> {
  return http.post<Task>(TASK_URL, data)
}

/**
 * 取消任务
 */
export function cancelTask(id: number, reason?: string): Promise<boolean> {
  return http.post<boolean>(`${TASK_URL}/${id}/cancel`, null, { params: { reason } })
}

// ==================== 任务执行操作 ====================

/**
 * 接收任务
 */
export function acceptTask(id: number): Promise<Task> {
  return http.post<Task>(`${TASK_URL}/${id}/accept`)
}

/**
 * 提交任务
 */
export function submitTask(data: TaskSubmitRequest): Promise<Task> {
  return http.post<Task>(`${TASK_URL}/submit`, data)
}

/**
 * 审批任务
 */
export function approveTask(data: TaskApproveRequest): Promise<Task> {
  return http.post<Task>(`${TASK_URL}/approve`, data)
}

// ==================== 任务查询 ====================

/**
 * 获取我的任务
 */
export function getMyTasks(params?: {
  pageNum?: number
  pageSize?: number
  status?: number
}): Promise<PageResponse<Task>> {
  return http.get<PageResponse<Task>>(`${TASK_URL}/my`, { params })
}

/**
 * 获取待我审批的任务
 */
export function getPendingApprovalTasks(params?: {
  pageNum?: number
  pageSize?: number
}): Promise<PageResponse<Task>> {
  return http.get<PageResponse<Task>>(`${TASK_URL}/pending-approval`, { params })
}

/**
 * 获取任务统计
 */
export function getTaskStatistics(): Promise<TaskStatistics> {
  return http.get<TaskStatistics>(`${TASK_URL}/statistics`)
}

/**
 * 获取任务流程进度
 */
export function getTaskProgress(id: number): Promise<TaskProgressNode[]> {
  return http.get<TaskProgressNode[]>(`${TASK_URL}/${id}/progress`)
}

// ==================== 工作流模板 (暂用V1端点) ====================

/**
 * 获取工作流模板列表
 */
export function getWorkflowTemplates(params?: {
  pageNum?: number
  pageSize?: number
  status?: number
}): Promise<PageResponse<WorkflowTemplate>> {
  return http.get<PageResponse<WorkflowTemplate>>(WORKFLOW_URL, { params })
}

/**
 * 获取所有启用的工作流模板
 */
export function getEnabledWorkflowTemplates(): Promise<WorkflowTemplate[]> {
  return http.get<WorkflowTemplate[]>(`${WORKFLOW_URL}/enabled`)
}

/**
 * 获取工作流模板详情
 */
export function getWorkflowTemplate(id: number): Promise<WorkflowTemplate> {
  return http.get<WorkflowTemplate>(`${WORKFLOW_URL}/${id}`)
}

/**
 * 创建工作流模板
 */
export function createWorkflowTemplate(data: CreateWorkflowTemplateRequest): Promise<WorkflowTemplate> {
  return http.post<WorkflowTemplate>(WORKFLOW_URL, data)
}

/**
 * 更新工作流模板
 */
export function updateWorkflowTemplate(id: number, data: UpdateWorkflowTemplateRequest): Promise<WorkflowTemplate> {
  return http.put<WorkflowTemplate>(`${WORKFLOW_URL}/${id}`, data)
}

/**
 * 删除工作流模板
 */
export function deleteWorkflowTemplate(id: number): Promise<void> {
  return http.delete(`${WORKFLOW_URL}/${id}`)
}

/**
 * 启用工作流模板
 */
export function enableWorkflowTemplate(id: number): Promise<void> {
  return http.post(`${WORKFLOW_URL}/${id}/enable`)
}

/**
 * 禁用工作流模板
 */
export function disableWorkflowTemplate(id: number): Promise<void> {
  return http.post(`${WORKFLOW_URL}/${id}/disable`)
}

// ==================== 系统消息 (暂用V1端点) ====================

/**
 * 获取系统消息列表
 */
export function getSystemMessages(params?: SystemMessageQueryParams): Promise<PageResponse<SystemMessage>> {
  return http.get<PageResponse<SystemMessage>>(MESSAGE_URL, { params })
}

/**
 * 获取未读消息数量
 */
export function getUnreadMessageCount(): Promise<number> {
  return http.get<number>(`${MESSAGE_URL}/unread-count`)
}

/**
 * 标记消息为已读
 */
export function markMessageAsRead(id: number): Promise<void> {
  return http.post(`${MESSAGE_URL}/${id}/read`)
}

/**
 * 标记所有消息为已读
 */
export function markAllMessagesAsRead(): Promise<void> {
  return http.post(`${MESSAGE_URL}/read-all`)
}

// ==================== API 对象封装（供 Store 使用） ====================

/**
 * 任务 API 对象
 */
export const taskApi = {
  getList: getTasks,
  getById: getTask,
  getDetail: getTaskDetail,
  create: createTask,
  cancel: cancelTask,
  accept: acceptTask,
  submit: submitTask,
  approve: approveTask,
  getMyTasks,
  getPendingApproval: getPendingApprovalTasks,
  getStatistics: getTaskStatistics,
  getProgress: getTaskProgress
}

/**
 * 工作流模板 API 对象
 */
export const workflowTemplateApi = {
  getList: getWorkflowTemplates,
  getEnabled: getEnabledWorkflowTemplates,
  getById: getWorkflowTemplate,
  create: createWorkflowTemplate,
  update: updateWorkflowTemplate,
  delete: deleteWorkflowTemplate,
  enable: enableWorkflowTemplate,
  disable: disableWorkflowTemplate
}

/**
 * 系统消息 API 对象
 */
export const systemMessageApi = {
  getList: getSystemMessages,
  getUnreadCount: getUnreadMessageCount,
  markAsRead: markMessageAsRead,
  markAllAsRead: markAllMessagesAsRead
}
