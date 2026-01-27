/**
 * 任务管理类型定义 - DDD架构适配
 */

// ==================== 枚举类型 ====================

/**
 * 任务状态
 */
export type TaskStatus = 0 | 1 | 2 | 3 | 4 | 5 | 6

export const TaskStatusMap: Record<TaskStatus, string> = {
  0: '待接收',
  1: '进行中',
  2: '待审核',
  3: '已完成',
  4: '已打回',
  5: '已取消',
  6: '已超期'
}

/**
 * 任务优先级
 */
export type TaskPriority = 1 | 2 | 3

export const TaskPriorityMap: Record<TaskPriority, string> = {
  1: '紧急',
  2: '普通',
  3: '低'
}

/**
 * 分配类型
 */
export type AssignType = 1 | 2

export const AssignTypeMap: Record<AssignType, string> = {
  1: '指定个人',
  2: '批量分配'
}

/**
 * 审批动作
 */
export type ApprovalAction = 1 | 2

export const ApprovalActionMap: Record<ApprovalAction, string> = {
  1: '通过',
  2: '打回'
}

// ==================== 实体类型 ====================

/**
 * 任务提交记录
 */
export interface TaskSubmission {
  id: number
  taskId: number
  assigneeId: number
  content: string
  attachmentIds?: number[]
  submittedAt: string
  createdAt?: string
}

/**
 * 任务审批记录
 */
export interface TaskApprovalRecord {
  id: number
  taskId: number
  submissionId: number
  approverId: number
  approverName: string
  approvalLevel: number
  action: ApprovalAction
  actionText?: string
  comment?: string
  createdAt: string
}

/**
 * 任务执行人
 */
export interface TaskAssignee {
  id: number
  taskId: number
  assigneeId: number
  assigneeName: string
  orgUnitId?: number
  orgUnitName?: string
  status: TaskStatus
  statusText?: string
  acceptedAt?: string
  submittedAt?: string
  completedAt?: string
  processInstanceId?: string
  currentApprovalLevel?: number
  submission?: TaskSubmission
  approvalRecords?: TaskApprovalRecord[]
}

/**
 * 任务进度节点
 */
export interface TaskProgressNode {
  nodeId: string
  nodeName: string
  nodeType: string
  status: 'pending' | 'active' | 'completed' | 'rejected'
  operatorId?: number
  operatorName?: string
  operatedAt?: string
  comment?: string
}

/**
 * 任务实体
 */
export interface Task {
  id: number
  taskCode: string
  title: string
  description?: string
  priority: TaskPriority
  priorityText?: string
  status: TaskStatus
  statusText?: string
  assignerId: number
  assignerName: string
  assignType: AssignType
  assigneeId?: number
  assigneeName?: string
  orgUnitId?: number
  orgUnitName?: string
  assignees?: TaskAssignee[]
  totalAssignees?: number
  submittedAssignees?: number
  completedAssignees?: number
  myAssigneeId?: number
  myStatus?: TaskStatus
  myAcceptedAt?: string
  mySubmittedAt?: string
  dueDate?: string
  overdue?: boolean
  acceptedAt?: string
  submittedAt?: string
  completedAt?: string
  workflowTemplateId?: number
  workflowTemplateName?: string
  processInstanceId?: string
  currentNode?: string
  currentApprovers?: number[]
  attachmentIds?: number[]
  submission?: TaskSubmission
  approvalRecords?: TaskApprovalRecord[]
  createdAt: string
  updatedAt?: string
}

/**
 * 任务详情（含卡片式执行人数据）
 */
export interface TaskDetail extends Task {
  assigneeCards?: TaskAssigneeCard[]
}

/**
 * 执行人卡片数据
 */
export interface TaskAssigneeCard {
  assignee: TaskAssignee
  submission?: TaskSubmission
  approvalRecords?: TaskApprovalRecord[]
  progressNodes?: TaskProgressNode[]
}

/**
 * 任务统计
 */
export interface TaskStatistics {
  totalCount: number
  pendingCount: number
  inProgressCount: number
  submittedCount: number
  completedCount: number
  rejectedCount: number
  cancelledCount: number
  overdueCount: number
  completionRate: number
  overdueRate: number
  pendingApprovalCount: number
}

/**
 * 工作流模板
 */
export interface WorkflowTemplate {
  id: number
  name: string
  description?: string
  approvalLevels: number
  levelConfigs?: ApprovalLevelConfig[]
  status: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 审批级别配置
 */
export interface ApprovalLevelConfig {
  level: number
  name: string
  approverType: 'role' | 'user' | 'orgUnit'
  approverIds?: number[]
}

/**
 * 组织单元审批配置
 */
export interface OrgUnitApprovalConfig {
  orgUnitId: number
  orgUnitName?: string
  approverConfigs: ApproverConfig[]
}

/**
 * 审批人配置
 */
export interface ApproverConfig {
  level: number
  approverId: number
  approverName?: string
}

// ==================== 请求类型 ====================

/**
 * 创建任务请求
 */
export interface CreateTaskRequest {
  title: string
  description?: string
  priority?: TaskPriority
  assignType: AssignType
  assigneeId?: number
  targetIds?: number[]
  orgUnitId?: number
  dueDate?: string
  workflowTemplateId: number
  approvalConfigs: OrgUnitApprovalConfig[]
  attachmentIds?: number[]
}

/**
 * 任务查询参数
 */
export interface TaskQueryParams {
  keyword?: string
  status?: TaskStatus
  priority?: TaskPriority
  assignerId?: number
  assigneeId?: number
  orgUnitId?: number
  startTime?: string
  endTime?: string
  dueStartTime?: string
  dueEndTime?: string
  myTask?: boolean
  pendingApproval?: boolean
  pageNum?: number
  pageSize?: number
}

/**
 * 任务提交请求
 */
export interface TaskSubmitRequest {
  taskId: number
  content: string
  attachmentIds?: number[]
}

/**
 * 任务审批请求
 */
export interface TaskApproveRequest {
  taskId: number
  submissionId: number
  action: ApprovalAction
  comment?: string
  rejectToNode?: string
  flowableTaskId?: string
}

/**
 * 创建工作流模板请求
 */
export interface CreateWorkflowTemplateRequest {
  name: string
  description?: string
  approvalLevels: number
  levelConfigs?: ApprovalLevelConfig[]
}

/**
 * 更新工作流模板请求
 */
export interface UpdateWorkflowTemplateRequest extends Partial<CreateWorkflowTemplateRequest> {
  id?: number
}

// ==================== 系统消息 ====================

/**
 * 系统消息
 */
export interface SystemMessage {
  id: number
  userId: number
  type: string
  title: string
  content: string
  relatedId?: number
  relatedType?: string
  isRead: boolean
  createdAt: string
}

/**
 * 系统消息查询参数
 */
export interface SystemMessageQueryParams {
  type?: string
  isRead?: boolean
  pageNum?: number
  pageSize?: number
}
