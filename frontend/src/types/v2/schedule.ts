/**
 * 排班管理模块类型定义
 */

// 策略类型
export type PolicyType = 'DAILY' | 'WEEKLY' | 'CUSTOM'

// 轮询算法
export type RotationAlgorithm = 'ROUND_ROBIN' | 'RANDOM' | 'LOAD_BALANCED'

// 执行状态
export type ExecutionStatus = 'PLANNED' | 'EXECUTED' | 'SKIPPED' | 'FAILED'

// 排班策略
export interface SchedulePolicy {
  id: number
  policyCode: string
  policyName: string
  policyType: PolicyType
  rotationAlgorithm: RotationAlgorithm
  templateId?: number
  inspectorPool: number[]
  scheduleConfig?: string
  excludedDates?: string[]
  enabled: boolean
  createdBy?: number
  createdAt: string
  updatedAt?: string
}

// 排班执行记录
export interface ScheduleExecution {
  id: number
  policyId: number
  executionDate: string
  assignedInspectors: number[]
  sessionId?: number
  status: ExecutionStatus
  failureReason?: string
  createdAt: string
}

// 创建策略请求
export interface CreatePolicyRequest {
  policyName: string
  policyType: PolicyType
  rotationAlgorithm: RotationAlgorithm
  templateId?: number
  inspectorPool: number[]
  scheduleConfig?: string
  excludedDates?: string[]
}

// 状态配置
export const PolicyTypeConfig: Record<PolicyType, { label: string; type: string }> = {
  DAILY: { label: '每日', type: 'primary' },
  WEEKLY: { label: '每周', type: 'success' },
  CUSTOM: { label: '自定义', type: 'warning' }
}

export const RotationAlgorithmConfig: Record<RotationAlgorithm, { label: string; description: string }> = {
  ROUND_ROBIN: { label: '轮询', description: '按顺序轮流分配检查员' },
  RANDOM: { label: '随机', description: '随机选择检查员' },
  LOAD_BALANCED: { label: '工作量均衡', description: '优先分配工作量最少的检查员' }
}

export const ExecutionStatusConfig: Record<ExecutionStatus, { label: string; type: string }> = {
  PLANNED: { label: '已计划', type: 'info' },
  EXECUTED: { label: '已执行', type: 'success' },
  SKIPPED: { label: '已跳过', type: 'warning' },
  FAILED: { label: '失败', type: 'danger' }
}
