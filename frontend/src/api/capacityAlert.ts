/**
 * 场所容量告警API
 * 对标: AWS CloudWatch Alarms API
 */

import request from '@/utils/request'
import type { CapacityAlertDTO, TypeAlertSummary, AlertCheckResponse } from '@/types/capacityAlert'

/**
 * 查询容量告警列表
 */
export function getHighOccupancyAlerts(typeCode?: string): Promise<CapacityAlertDTO[]> {
  return request({
    url: '/v9/places/capacity-alerts',
    method: 'get',
    params: { typeCode }
  })
}

/**
 * 查询单个场所告警
 */
export function getPlaceAlert(placeId: number | string): Promise<CapacityAlertDTO> {
  return request({
    url: `/v9/places/capacity-alerts/${placeId}`,
    method: 'get'
  })
}

/**
 * 检查告警状态
 */
export function checkAlert(placeId: number | string): Promise<AlertCheckResponse> {
  return request({
    url: `/v9/places/capacity-alerts/${placeId}/should-alert`,
    method: 'get'
  })
}

/**
 * 查询告警汇总
 */
export function getAlertSummary(): Promise<TypeAlertSummary[]> {
  return request({
    url: '/v9/places/capacity-alerts/summary',
    method: 'get'
  })
}
