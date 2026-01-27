/**
 * 我的班级 API - DDD架构适配
 *
 * 注意: 响应拦截器已解包 ApiResponse，API 直接返回 data 内容
 */
import { http } from '@/utils/request'
import type {
  MyClassItem,
  MyClassOverview,
  MyClassStudent,
  DormitoryDistribution
} from '@/types/v2/myClass'

// 后端API路径 - V2 DDD架构
const BASE_URL = '/v2/my-class'

// ==================== 我的班级 API ====================

/**
 * 获取我管理的班级列表
 */
export function getMyClasses(): Promise<MyClassItem[]> {
  return http.get<MyClassItem[]>(`${BASE_URL}/classes`)
}

/**
 * 获取班级概览数据
 */
export function getClassOverview(classId: string | number): Promise<MyClassOverview> {
  return http.get<MyClassOverview>(`${BASE_URL}/classes/${classId}/overview`)
}

/**
 * 获取班级学生列表
 */
export function getClassStudents(classId: string | number, params?: {
  keyword?: string
  status?: string
}): Promise<MyClassStudent[]> {
  return http.get<MyClassStudent[]>(`${BASE_URL}/classes/${classId}/students`, { params })
}

/**
 * 获取班级宿舍分布
 */
export function getClassDormitoryDistribution(classId: string | number): Promise<DormitoryDistribution[]> {
  console.log('[myClass API] getClassDormitoryDistribution called with classId:', classId, 'type:', typeof classId)
  console.log('[myClass API] Request URL:', `${BASE_URL}/classes/${classId}/dormitory-distribution`)
  return http.get<DormitoryDistribution[]>(`${BASE_URL}/classes/${classId}/dormitory-distribution`)
    .then((response: DormitoryDistribution[]) => {
      console.log('[myClass API] getClassDormitoryDistribution response:', response)
      return response
    })
}

// ==================== API 对象封装（供 Store 使用） ====================

/**
 * 我的班级 API 对象
 */
export const myClassApi = {
  getClasses: getMyClasses,
  getOverview: getClassOverview,
  getStudents: getClassStudents,
  getDormitoryDistribution: getClassDormitoryDistribution
}

export default myClassApi
