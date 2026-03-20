/**
 * V7 检查平台 - 评级关联类型定义
 * 桥接 V7 检查项目 → 评级配置
 */

export interface InspRatingLink {
  id: number
  tenantId: number
  projectId: number
  ratingConfigId: number
  periodType: string
  autoCalculate: boolean
  createdBy: number | null
  createdAt: string
  updatedAt: string | null
}

export interface CreateRatingLinkRequest {
  projectId: number
  ratingConfigId: number
  periodType: string
  autoCalculate?: boolean
  createdBy?: number
}

export interface UpdateRatingLinkRequest {
  periodType?: string
  autoCalculate: boolean
}

export interface CalculateRatingRequest {
  projectId: number
  periodType: string
  periodStart: string
  periodEnd: string
}
