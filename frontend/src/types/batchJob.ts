/**
 * 批量作业相关类型定义
 * 对标: AWS S3 Batch Operations
 */

/**
 * 批量作业状态
 */
export type BatchJobStatus =
  | 'PENDING'      // 待处理
  | 'RUNNING'      // 执行中
  | 'COMPLETED'    // 已完成
  | 'FAILED'       // 失败
  | 'CANCELLED'    // 已取消

/**
 * 批量作业类型
 */
export type BatchJobType =
  | 'BATCH_ASSIGN_ORG'          // 批量分配组织
  | 'BATCH_ASSIGN_RESPONSIBLE'  // 批量分配负责人
  | 'BATCH_CHANGE_STATUS'       // 批量变更状态

/**
 * 批量作业DTO
 */
export interface BatchJobDTO {
  /** 作业ID */
  jobId: string
  /** 作业类型 */
  jobType: BatchJobType
  /** 作业名称 */
  jobName: string
  /** 作业状态 */
  jobStatus: BatchJobStatus
  /** 总项目数 */
  totalItems: number
  /** 已处理项目数 */
  processedItems: number
  /** 成功数量 */
  successCount: number
  /** 失败数量 */
  failureCount: number
  /** 跳过数量 */
  skippedCount: number
  /** 进度百分比 */
  progressPercentage: number
  /** 创建时间 */
  createdAt: string
  /** 开始时间 */
  startedAt?: string
  /** 完成时间 */
  completedAt?: string
  /** 创建用户ID */
  createdBy: number | string
  /** 创建用户名 */
  createdByName: string
  /** 最后错误信息 */
  lastError?: string
}

/**
 * 批量分配组织请求
 */
export interface BatchAssignOrgRequest {
  /** 场所ID列表 */
  placeIds: (number | string)[]
  /** 目标组织单元ID（null表示清除覆盖） */
  targetOrgUnitId: number | string | null
  /** 操作原因 */
  reason?: string
}

/**
 * 批量分配负责人请求
 */
export interface BatchAssignResponsibleRequest {
  /** 场所ID列表 */
  placeIds: (number | string)[]
  /** 目标负责人ID（null表示清除） */
  targetResponsibleUserId: number | string | null
  /** 操作原因 */
  reason?: string
}

/**
 * 批量变更状态请求
 */
export interface BatchChangeStatusRequest {
  /** 场所ID列表 */
  placeIds: (number | string)[]
  /** 目标状态 */
  targetStatus: 'NORMAL' | 'DISABLED' | 'MAINTENANCE'
  /** 操作原因 */
  reason?: string
}

/**
 * 作业提交响应
 */
export interface BatchJobSubmitResponse {
  /** 作业ID */
  jobId: string
  /** 响应消息 */
  message: string
}

/**
 * 作业监控配置
 */
export interface BatchJobMonitorOptions {
  /** 轮询间隔（毫秒） */
  pollInterval?: number
  /** 自动刷新 */
  autoRefresh?: boolean
  /** 显示详情 */
  showDetails?: boolean
}
