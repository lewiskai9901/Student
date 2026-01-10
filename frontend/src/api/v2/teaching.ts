import { http } from '@/utils/request'
import type {
  TeachingBuilding,
  Classroom,
  BuildingQueryParams,
  ClassroomQueryParams
} from '@/types/teaching'
import type { PageResult } from '@/types/common'

// ========== 教学楼管理 API ==========

/**
 * 分页查询教学楼
 */
export function getBuildings(params: BuildingQueryParams) {
  return http.get<PageResult<TeachingBuilding>>('/teaching/buildings', { params })
}

/**
 * 获取所有启用的教学楼
 */
export function getEnabledBuildings() {
  return http.get<TeachingBuilding[]>('/teaching/buildings/enabled')
}

/**
 * 获取教学楼详情
 */
export function getBuildingDetail(id: number | string) {
  return http.get<TeachingBuilding>(`/teaching/buildings/${id}`)
}

/**
 * 创建教学楼
 */
export function createBuilding(data: TeachingBuilding) {
  return http.post<TeachingBuilding>('/teaching/buildings', data)
}

/**
 * 更新教学楼
 */
export function updateBuilding(id: number | string, data: TeachingBuilding) {
  return http.put<TeachingBuilding>(`/teaching/buildings/${id}`, data)
}

/**
 * 删除教学楼
 */
export function deleteBuilding(id: number | string) {
  return http.delete(`/teaching/buildings/${id}`)
}

// ========== 教室管理 API ==========

/**
 * 分页查询教室
 */
export function getClassrooms(params: ClassroomQueryParams) {
  return http.get<PageResult<Classroom>>('/teaching/classrooms', { params })
}

/**
 * 获取教室详情
 */
export function getClassroomDetail(id: number | string) {
  return http.get<Classroom>(`/teaching/classrooms/${id}`)
}

/**
 * 创建教室
 */
export function createClassroom(data: Classroom) {
  return http.post<Classroom>('/teaching/classrooms', data)
}

/**
 * 更新教室
 */
export function updateClassroom(id: number | string, data: Classroom) {
  return http.put<Classroom>(`/teaching/classrooms/${id}`, data)
}

/**
 * 删除教室
 */
export function deleteClassroom(id: number | string) {
  return http.delete(`/teaching/classrooms/${id}`)
}

/**
 * 关联班级到教室
 */
export function assignClassToClassroom(id: number | string, classId: number | string) {
  return http.post<Classroom>(`/teaching/classrooms/${id}/assign-class`, null, {
    params: { classId }
  })
}
