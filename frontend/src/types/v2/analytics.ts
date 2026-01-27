/**
 * 数据分析模块类型定义
 */

// 快照类型
export type SnapshotType = 'CLASS_RANKING' | 'DEPARTMENT_TREND' | 'INSPECTOR_WORKLOAD' | 'VIOLATION_DISTRIBUTION'

// 分析响应
export interface AnalyticsResponse {
  type: string
  data: Record<string, any>[]
}

// 班级排名项
export interface ClassRankingItem {
  classId: number
  className: string
  avgScore: number
  checkCount: number
}

// 违规分布项
export interface ViolationDistributionItem {
  itemName: string
  occurrenceCount: number
  totalDeduction: number
}

// 检查员工作量项
export interface InspectorWorkloadItem {
  inspectorId: number
  inspectorName: string
  sessionCount: number
  classCount: number
}

// 系部对比项
export interface DepartmentComparisonItem {
  departmentId: number
  departmentName: string
  avgScore: number
  classCount: number
  recordCount: number
}

// 导出任务状态
export type ExportStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'

// 导出格式
export type ExportFormat = 'EXCEL' | 'PDF'

// 导出任务
export interface ExportTask {
  id: number
  taskCode: string
  exportType: string
  exportFormat: ExportFormat
  status: ExportStatus
  progress: number
  fileName?: string
  fileSize?: number
  downloadUrl?: string
  errorMessage?: string
  createdAt: string
  completedAt?: string
}

// 创建导出请求
export interface CreateExportRequest {
  exportType: string
  exportFormat: ExportFormat
  filters?: string
}

// 导出状态配置
export const ExportStatusConfig: Record<ExportStatus, { label: string; type: string }> = {
  PENDING: { label: '等待中', type: 'info' },
  PROCESSING: { label: '处理中', type: 'primary' },
  COMPLETED: { label: '已完成', type: 'success' },
  FAILED: { label: '失败', type: 'danger' },
  CANCELLED: { label: '已取消', type: 'warning' }
}

export const SnapshotTypeConfig: Record<SnapshotType, { label: string }> = {
  CLASS_RANKING: { label: '班级排名' },
  DEPARTMENT_TREND: { label: '系部趋势' },
  INSPECTOR_WORKLOAD: { label: '检查员工作量' },
  VIOLATION_DISTRIBUTION: { label: '违规分布' }
}
