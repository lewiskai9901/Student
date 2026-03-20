import { http } from '@/utils/request'

// ==================== 类型定义 ====================

/**
 * 待审批任务DTO
 */
export interface TaskApprovalDTO {
  /** 审批记录ID */
  recordId: number
  /** 任务ID */
  taskId: number
  /** 任务编号 */
  taskCode: string
  /** 任务标题 */
  title: string
  /** 任务描述 */
  description?: string
  /** 优先级: 1-紧急, 2-普通, 3-低 */
  priority: number
  /** 优先级文本 */
  priorityText: string
  /** 提交记录ID */
  submissionId: number
  /** 提交人ID */
  submitterId: number
  /** 提交人姓名 */
  submitterName: string
  /** 完成情况说明 */
  content?: string
  /** 提交时间 */
  submittedAt?: string
  /** 当前审批节点名称 */
  nodeName: string
  /** 当前审批顺序 */
  nodeOrder: number
  /** 审批人角色 */
  approverRole?: string
  /** 组织单元ID */
  orgUnitId?: number
  /** 组织单元名称 */
  orgUnitName?: string
  /** 截止时间 */
  deadline?: string
}

/**
 * 审批请求
 */
export interface ApproveRequest {
  /** 任务ID */
  taskId: number
  /** 提交记录ID */
  submissionId: number
  /** 审批动作: 1-通过, 2-打回 */
  action: number
  /** 审批意见 */
  comment?: string
  /** 打回到的节点 */
  rejectToNode?: string
}

/**
 * 提交任务请求
 */
export interface SubmitTaskRequest {
  /** 完成情况说明 */
  content?: string
  /** 附件ID列表 */
  attachmentIds?: string[]
}

// ==================== API方法 ====================

/**
 * 获取我的待审批列表
 */
export function getMyPendingApprovals() {
  return http.get<TaskApprovalDTO[]>('/tasks/approvals/pending')
}

/**
 * 审批任务
 * @param recordId 审批记录ID
 * @param data 审批请求
 */
export function approveTaskByRecord(recordId: number | string, data: ApproveRequest) {
  return http.post<void>(`/tasks/approvals/${recordId}/approve`, data)
}

/**
 * 提交任务完成情况
 * @param taskId 任务ID
 * @param data 提交请求
 */
export function submitTaskCompletion(taskId: number | string, data: SubmitTaskRequest) {
  return http.post<void>(`/tasks/approvals/${taskId}/submit`, data)
}
