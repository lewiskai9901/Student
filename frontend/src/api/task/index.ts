import { http } from '@/utils/request'

// ==================== 类型定义 ====================

// 任务状态
export enum TaskStatus {
  PENDING = 0,      // 待接收
  IN_PROGRESS = 1,  // 进行中
  SUBMITTED = 2,    // 待审核
  COMPLETED = 3,    // 已完成
  REJECTED = 4,     // 已打回
  CANCELLED = 5,    // 已取消
  APPROVING = 6     // 审批中
}

// 任务优先级
export enum TaskPriority {
  URGENT = 1,   // 紧急
  NORMAL = 2,   // 普通
  LOW = 3       // 低
}

// 任务DTO - 使用string类型ID避免JavaScript大数字精度丢失
export interface TaskDTO {
  id: string
  taskCode: string
  title: string
  description?: string
  priority: number
  priorityText: string
  status: number
  statusText: string
  assignerId: string
  assignerName: string
  assignType: number
  assigneeId?: string
  assigneeName?: string
  departmentId?: string
  departmentName?: string
  dueDate?: string
  overdue?: boolean
  acceptedAt?: string
  submittedAt?: string
  completedAt?: string
  workflowTemplateId?: string
  workflowTemplateName?: string
  processInstanceId?: string
  currentNode?: string
  currentApprovers?: string[]
  attachmentIds?: string[]
  assignees?: TaskAssigneeDTO[]
  // 批量任务进度信息
  totalAssignees?: number
  submittedAssignees?: number
  completedAssignees?: number
  // 当前用户执行状态（仅在"我的任务"中返回）
  myAssigneeId?: string
  myStatus?: number
  myAcceptedAt?: string
  mySubmittedAt?: string
  submission?: TaskSubmissionDTO
  approvalRecords?: TaskApprovalRecordDTO[]
  createdAt: string
  updatedAt: string
}

// 任务执行人DTO
export interface TaskAssigneeDTO {
  id: string
  taskId: string
  assigneeId: string
  assigneeName: string
  departmentId?: string
  departmentName?: string
  status: number
  statusText: string
  acceptedAt?: string
  submittedAt?: string
  completedAt?: string
  processInstanceId?: string
  currentApprovalLevel?: number
  submission?: TaskSubmissionDTO
  approvalRecords?: TaskApprovalRecordDTO[]
}

// 任务提交记录DTO
export interface TaskSubmissionDTO {
  id: string
  taskId: string
  submitterId: string
  submitterName: string
  content?: string
  attachmentIds?: string[]
  attachmentUrls?: string[]
  submittedAt: string
  reviewStatus: number
  reviewStatusText: string
  finalReviewerId?: string
  finalReviewerName?: string
  finalReviewComment?: string
  finalReviewedAt?: string
  rejectCount: number
  approvalRecords?: TaskApprovalRecordDTO[]
}

// 审批记录DTO
export interface TaskApprovalRecordDTO {
  id: string
  taskId: string
  submissionId: string
  nodeName: string
  nodeOrder: number
  approverId: string
  approverName: string
  approverRole?: string
  approvalStatus: number
  approvalStatusText: string
  approvalComment?: string
  approvalTime?: string
  rejectToNode?: string
  rejectReason?: string
  transferToId?: string
  transferToName?: string
  transferReason?: string
  flowableTaskId?: string
}

// 任务流程进度节点DTO
export interface TaskProgressNodeDTO {
  order: number
  nodeName: string
  nodeType: 'CREATE' | 'EXECUTE' | 'APPROVE' | 'END'
  status: 'COMPLETED' | 'PROCESSING' | 'PENDING' | 'REJECTED'
  handlerName?: string
  handlerId?: string
  handledAt?: string
  comment?: string
  handlers?: TaskAssigneeDTO[]
  totalCount?: number
  completedCount?: number
  progressPercent?: number
}

// 任务统计DTO
export interface TaskStatisticsDTO {
  pendingCount: number
  inProgressCount: number
  submittedCount: number
  completedCount: number
  pendingApprovalCount: number
  completionRate: number
  totalCount?: number
  rejectedCount?: number
  cancelledCount?: number
  overdueCount?: number
  overdueRate?: number
}

// 卡片式任务详情DTO
export interface TaskDetailDTO {
  task: TaskDTO
  assigneesByDepartment: DepartmentAssigneesDTO[]
  approvalFlows: DepartmentApprovalFlowDTO[]
  statistics: TaskStatisticsDTO
}

// 系部执行人分组
export interface DepartmentAssigneesDTO {
  departmentId: string
  departmentName: string
  totalCount: number
  completedCount: number
  assignees: TaskAssigneeDetailDTO[]
}

// 执行人详情（卡片数据）
export interface TaskAssigneeDetailDTO {
  assigneeId: string
  assigneeName: string
  status: number
  statusText: string
  currentApprovalLevel: number
  approvalConfig?: ApprovalConfigDTO[]
  approvalRecords?: ApprovalRecordDTO[]
  submittedAt?: string
  completedAt?: string
}

// 审批配置
export interface ApprovalConfigDTO {
  level: number
  approverId: string
  approverName: string
  approverRole: string
}

// 审批记录（简化）
export interface ApprovalRecordDTO {
  approvalLevel: number
  approvalStatus: number
  approvalTime?: string
}

// 系部审批流程配置
export interface DepartmentApprovalFlowDTO {
  departmentId: string
  departmentName: string
  flowChain: string
}

// 任务查询请求
export interface TaskQueryRequest {
  pageNum?: number
  pageSize?: number
  keyword?: string
  status?: number
  priority?: number
  assigneeId?: string
}

// 创建任务请求
export interface TaskCreateRequest {
  title: string
  description?: string
  priority?: number
  assignType: number
  assigneeIds?: string[]
  departmentIds?: string[]
  dueDate?: string
  workflowTemplateId?: string
  attachmentIds?: string[]
}

// 提交任务请求
export interface TaskSubmitRequest {
  taskId: string
  content?: string
  attachmentIds?: string[]
}

// 审批任务请求
export interface TaskApproveRequest {
  /** 任务ID */
  taskId: string | number
  /** 提交记录ID */
  submissionId?: string | number
  /** 审批动作: 1-通过, 2-打回 */
  action: number
  /** 审批意见 */
  comment?: string
  /** 打回到的节点 */
  rejectToNode?: string
}

// ==================== API方法 ====================

// 分页查询任务
export function getTaskList(params: TaskQueryRequest) {
  return http.get<{ records: TaskDTO[]; total: number }>('/task/tasks', { params })
}

// 获取任务详情
export function getTaskDetail(id: string | number) {
  return http.get<TaskDTO>(`/task/tasks/${id}`)
}

// 创建任务
export function createTask(data: TaskCreateRequest) {
  return http.post<TaskDTO>('/task/tasks', data)
}

// 接收任务
export function acceptTask(id: string | number) {
  return http.post<TaskDTO>(`/task/tasks/${id}/accept`)
}

// 提交任务
export function submitTask(data: TaskSubmitRequest) {
  return http.post<TaskDTO>('/task/tasks/submit', data)
}

// 审批任务
export function approveTask(data: TaskApproveRequest) {
  return http.post<TaskDTO>('/task/tasks/approve', data)
}

// 取消任务
export function cancelTask(id: string | number, reason?: string) {
  return http.post<boolean>(`/task/tasks/${id}/cancel`, null, { params: { reason } })
}

// 获取我的任务
export function getMyTasks(params: { pageNum?: number; pageSize?: number; status?: number }) {
  return http.get<{ records: TaskDTO[]; total: number }>('/task/tasks/my', { params })
}

// 获取待审批任务
export function getPendingApprovalTasks(params: { pageNum?: number; pageSize?: number }) {
  return http.get<{ records: TaskDTO[]; total: number }>('/task/tasks/pending-approval', { params })
}

// 获取任务统计
export function getTaskStatistics() {
  return http.get<TaskStatisticsDTO>('/task/tasks/statistics')
}

// 获取任务流程进度
export function getTaskProgress(id: string | number) {
  return http.get<TaskProgressNodeDTO[]>(`/task/tasks/${id}/progress`)
}

// 获取卡片式任务详情（按系部分组展示执行人）
export function getTaskCardDetail(id: string | number) {
  return http.get<TaskDetailDTO>(`/task/tasks/${id}/detail`)
}
