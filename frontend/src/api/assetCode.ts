import { http as request } from '@/utils/request'

const BASE_URL = '/asset-codes'

export interface LabelRequest {
  assetId: number | string
  assetCode: string
  assetName: string
  location?: string
}

export interface LabelData {
  assetCode: string
  assetName: string
  location?: string
  qrcode: string
  barcode: string
}

export const assetCodeApi = {
  /**
   * 生成资产编码
   */
  generateAssetCode(categoryCode: string, currentMaxSeq = 0): Promise<string> {
    return request.post(`${BASE_URL}/generate`, null, {
      params: { categoryCode, currentMaxSeq }
    })
  },

  /**
   * 批量生成资产编码
   */
  generateAssetCodes(categoryCode: string, count: number, currentMaxSeq = 0): Promise<string[]> {
    return request.post(`${BASE_URL}/generate-batch`, null, {
      params: { categoryCode, count, currentMaxSeq }
    })
  },

  /**
   * 生成资产二维码
   */
  generateQRCode(assetCode: string, assetId: number | string, size = 200): Promise<{
    qrcode: string
    assetCode: string
  }> {
    return request.get(`${BASE_URL}/qrcode`, {
      params: { assetCode, assetId, size }
    })
  },

  /**
   * 生成条形码
   */
  generateBarcode(assetCode: string, width = 300, height = 100): Promise<{
    barcode: string
    assetCode: string
  }> {
    return request.get(`${BASE_URL}/barcode`, {
      params: { assetCode, width, height }
    })
  },

  /**
   * 解析二维码内容
   */
  parseQRCode(content: string): Promise<{
    type: string
    assetCode: string
    assetId: string
  }> {
    return request.post(`${BASE_URL}/parse-qrcode`, null, {
      params: { content }
    })
  },

  /**
   * 生成打印用的完整标签数据
   */
  generateLabel(
    assetCode: string,
    assetId: number | string,
    assetName: string,
    location?: string
  ): Promise<LabelData> {
    return request.get(`${BASE_URL}/label`, {
      params: { assetCode, assetId, assetName, location }
    })
  },

  /**
   * 批量生成标签数据
   */
  generateLabels(requests: LabelRequest[]): Promise<LabelData[]> {
    return request.post(`${BASE_URL}/labels`, requests)
  }
}
