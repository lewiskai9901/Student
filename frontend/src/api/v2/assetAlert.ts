import request from '@/utils/request'
import type { AssetAlert, AlertStatistics } from '@/types/v2/asset'

const BASE_URL = '/v2/asset-alerts'

export const assetAlertApi = {
  /**
   * 获取预警详情
   */
  getAlert(id: number): Promise<AssetAlert> {
    return request.get(`${BASE_URL}/${id}`)
  },

  /**
   * 获取我的未读预警
   */
  getUnreadAlerts(): Promise<AssetAlert[]> {
    return request.get(`${BASE_URL}/unread`)
  },

  /**
   * 获取我的未处理预警
   */
  getUnhandledAlerts(): Promise<AssetAlert[]> {
    return request.get(`${BASE_URL}/unhandled`)
  },

  /**
   * 标记为已读
   */
  markAsRead(id: number): Promise<void> {
    return request.post(`${BASE_URL}/${id}/read`)
  },

  /**
   * 全部标记为已读
   */
  markAllAsRead(): Promise<void> {
    return request.post(`${BASE_URL}/read-all`)
  },

  /**
   * 处理预警
   */
  handleAlert(id: number, remark?: string): Promise<void> {
    return request.post(`${BASE_URL}/${id}/handle`, { remark })
  },

  /**
   * 分页查询预警列表
   */
  queryAlerts(params: {
    alertType?: number
    isRead?: boolean
    isHandled?: boolean
    pageNum?: number
    pageSize?: number
  }): Promise<{
    records: AssetAlert[]
    total: number
    pageNum: number
    pageSize: number
  }> {
    return request.get(BASE_URL, { params })
  },

  /**
   * 获取预警统计
   */
  getStatistics(): Promise<AlertStatistics> {
    return request.get(`${BASE_URL}/statistics`)
  },

  /**
   * 获取未读数量
   */
  countUnread(): Promise<number> {
    return request.get(`${BASE_URL}/unread/count`)
  }
}
