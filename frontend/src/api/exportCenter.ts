/**
 * 导出中心 API
 * 支持多种导出场景：扣分明细、评级报表、统计报表
 */
import { http } from '@/utils/request'

// ==================== 类型定义 ====================

export type ExportScenario = 'DEDUCTION_DETAIL' | 'RATING_REPORT' | 'STATISTICS_REPORT'

export interface ExportRequest {
  scenario: ExportScenario
  startDate: string
  endDate: string
  sessionId?: number
  classIds?: number[]
  categoryIds?: number[]
  format?: string
}

export interface ExportResult {
  fileName: string
  filePath: string
  downloadUrl: string
  fileSize: number
  recordCount: number
  scenario: string
  generatedAt: string
}

export interface ExportScenarioInfo {
  code: string
  name: string
  description: string
}

export interface ExportEstimate {
  estimatedCount: number
  scenario: string
  async: boolean
}

// 场景配置
export const ExportScenarioConfig: Record<ExportScenario, { label: string; description: string; icon: string }> = {
  DEDUCTION_DETAIL: {
    label: '扣分明细',
    description: '导出检查扣分的详细记录，包含班级、扣分项、分值等信息',
    icon: 'Document'
  },
  RATING_REPORT: {
    label: '评级报表',
    description: '导出班级评级汇总报表，包含评级结果、得分排名等',
    icon: 'DataLine'
  },
  STATISTICS_REPORT: {
    label: '统计报表',
    description: '导出综合统计分析报表，包含趋势、对比、分布等数据',
    icon: 'TrendCharts'
  }
}

// ==================== API 方法 ====================

export const exportCenterApi = {
  /** 获取可用导出场景列表 */
  getScenarios: () =>
    http.get<ExportScenarioInfo[]>('/export-center/scenarios'),

  /** 执行数据导出 */
  exportData: (data: ExportRequest) =>
    http.post<ExportResult>('/export-center/export', data),

  /** 预估导出数据量 */
  estimateCount: (data: ExportRequest) =>
    http.post<ExportEstimate>('/export-center/estimate', data),
}
