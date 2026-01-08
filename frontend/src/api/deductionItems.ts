import { http } from '@/utils/request'

/**
 * 扣分项
 */
export interface DeductionItem {
  id: number
  itemName: string
  itemCode?: string
  typeId: number
  deductMode: number  // 1固定分数 2区间分数 3按人次
  fixedScore?: number
  minScore?: number
  maxScore?: number
  baseScore?: number  // 按人次扣分的基础分
  scorePerPerson?: number
  perPersonScore?: number  // 兼容后端字段名
  rangeConfig?: string | any  // 区间配置JSON
  description?: string
  status: number
  sortOrder: number
  allowPhoto?: number  // 是否允许上传照片 1是 0否
  allowRemark?: number  // 是否允许添加备注 1是 0否
  allowStudents?: number  // 是否允许添加学生 1是 0否 (仅按人次模式)
  createdAt: string
  updatedAt: string
}

/**
 * 获取扣分项列表(按类型ID)
 */
export function getDeductionItemsByTypeId(typeId: string | number) {
  return http.get<DeductionItem[]>(`/deduction-items/type/${typeId}`)
}

/**
 * 获取启用的扣分项列表(按类型ID)
 */
export function getEnabledDeductionItemsByTypeId(typeId: string | number) {
  return http.get<DeductionItem[]>(`/deduction-items/type/${typeId}/enabled`)
}

/**
 * 获取扣分项详情
 */
export function getDeductionItemById(id: string | number) {
  return http.get<any>(`/deduction-items/${id}`)
}

/**
 * 创建扣分项
 */
export function createDeductionItem(data: any) {
  return http.post<number>('/deduction-items', data)
}

/**
 * 更新扣分项
 */
export function updateDeductionItem(id: string | number, data: any) {
  return http.put(`/deduction-items/${id}`, data)
}

/**
 * 删除扣分项
 */
export function deleteDeductionItem(id: string | number) {
  return http.delete(`/deduction-items/${id}`)
}

/**
 * 批量删除扣分项
 */
export function batchDeleteDeductionItems(ids: (string | number)[]) {
  return http.delete('/deduction-items/batch', { data: ids })
}

/**
 * 更新扣分项状态
 */
export function updateDeductionItemStatus(id: string | number, status: number) {
  return http.patch(`/deduction-items/${id}/status`, null, { params: { status } })
}
