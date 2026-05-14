import type { LongId } from '@/types/common'

/**
 * 检查平台 - 评级关联类型定义
 * 桥接 检查项目 > 评级配置
 */

export interface InspRatingLink {
  id: LongId
  tenantId: LongId
  projectId: LongId
  ratingConfigId: LongId
  periodType: string
  autoCalculate: boolean
  createdBy: number | null
  createdAt: string
  updatedAt: string | null
}

export interface CreateRatingLinkRequest {
  projectId: LongId
  ratingConfigId: LongId
  periodType: string
  autoCalculate?: boolean
  createdBy?: number
}

export interface UpdateRatingLinkRequest {
  periodType?: string
  autoCalculate: boolean
}

export interface CalculateRatingRequest {
  projectId: LongId
  periodType: string
  periodStart: string
  periodEnd: string
}
