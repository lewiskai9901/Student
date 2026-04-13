import { http as request } from '@/utils/request'

const BASE_URL = '/asset-depreciation'

export interface AssetDepreciationDTO {
  id: number
  assetId: number
  assetCode: string
  depreciationPeriod: string
  beginningValue: number
  beginningAccumulatedDepreciation: number
  beginningNetValue: number
  depreciationAmount: number
  endingAccumulatedDepreciation: number
  endingNetValue: number
  usedMonths: number
  remainingMonths: number
  depreciationMethod: number
  depreciationMethodName: string
  depreciationDate: string
  createdAt: string
  remark?: string
}

export interface DepreciationMethod {
  code: number
  name: string
}

export const assetDepreciationApi = {
  /**
   * 计算单个资产的折旧
   */
  calculateDepreciation(assetId: number | string, period?: string): Promise<AssetDepreciationDTO> {
    return request.post(`${BASE_URL}/${assetId}/calculate`, null, {
      params: { period }
    })
  },

  /**
   * 批量计提所有资产折旧
   */
  calculateAllDepreciation(period?: string): Promise<{
    period: string
    processedCount: number
  }> {
    return request.post(`${BASE_URL}/calculate-all`, null, {
      params: { period }
    })
  },

  /**
   * 手动触发折旧任务
   */
  triggerDepreciation(period: string): Promise<void> {
    return request.post(`${BASE_URL}/trigger`, null, {
      params: { period }
    })
  },

  /**
   * 预览折旧计算结果
   */
  previewDepreciation(assetId: number | string, period?: string): Promise<AssetDepreciationDTO> {
    return request.get(`${BASE_URL}/${assetId}/preview`, {
      params: { period }
    })
  },

  /**
   * 获取资产折旧历史
   */
  getDepreciationHistory(assetId: number | string): Promise<AssetDepreciationDTO[]> {
    return request.get(`${BASE_URL}/${assetId}/history`)
  },

  /**
   * 获取资产折旧历史（分页）
   */
  getDepreciationHistoryPage(
    assetId: number | string,
    pageNum = 1,
    pageSize = 10
  ): Promise<{
    records: AssetDepreciationDTO[]
    total: number
    pageNum: number
    pageSize: number
  }> {
    return request.get(`${BASE_URL}/${assetId}/history-page`, {
      params: { pageNum, pageSize }
    })
  },

  /**
   * 获取某期间的折旧汇总
   */
  getPeriodSummary(period: string): Promise<{
    period: string
    assetCount: number
    totalDepreciation: number
    records: AssetDepreciationDTO[]
  }> {
    return request.get(`${BASE_URL}/period/${period}`)
  },

  /**
   * 获取折旧方法列表
   */
  getDepreciationMethods(): Promise<DepreciationMethod[]> {
    return request.get(`${BASE_URL}/methods`)
  }
}

// 折旧方法枚举
export const DepreciationMethodMap: Record<number, string> = {
  0: '不计提折旧',
  1: '直线法',
  2: '双倍余额递减法',
  3: '年数总和法',
  4: '工作量法'
}
