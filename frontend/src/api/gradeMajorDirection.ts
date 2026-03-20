import { http } from '@/utils/request'

export interface GradeMajorDirection {
  id?: number
  academicYear: number       // 学年（如：2024）
  majorDirectionId: number   // 专业方向ID
  majorId?: number           // 专业ID
  majorCode?: string         // 专业编码
  directionCode?: string     // 方向编码（用于生成班级编码）
  orgUnitId?: number      // 所属组织单元ID（来自majors表）
  remarks?: string
  createdAt?: string
  updatedAt?: string
  createdBy?: number
  updatedBy?: number
  // 关联显示字段
  directionName?: string
  majorName?: string
  level?: string
  years?: number
  duration?: number          // 学制年限
  educationSystem?: string   // 学制类型 (三年制/五年制等)
  skillLevel?: string        // 技能等级
  actualClassCount?: number  // 已有班级数
  // 分段培养字段
  isSegmented?: number       // 是否分段培养 0-否 1-是
  phase1Level?: string       // 第一阶段层次
  phase1Years?: number       // 第一阶段年数
  phase2Level?: string       // 第二阶段层次
  phase2Years?: number       // 第二阶段年数
}

export interface GradeMajorDirectionQueryParams {
  pageNum?: number
  pageSize?: number
  academicYear?: number
  majorDirectionId?: number
}

// 获取学年专业方向列表(分页)
export const getGradeMajorDirectionList = (params: GradeMajorDirectionQueryParams) => {
  return http.get<{
    records: GradeMajorDirection[]
    total: number
    pages: number
  }>('/grade-major-directions', { params })
}

// 根据学年获取专业方向列表
export const getDirectionsByYear = (academicYear: number | string) => {
  return http.get<GradeMajorDirection[]>(`/grade-major-directions/year/${academicYear}`)
}

// 根据专业方向ID获取关联的学年列表
export const getYearsByDirection = (directionId: number | string) => {
  return http.get<GradeMajorDirection[]>(`/grade-major-directions/direction/${directionId}`)
}

// 获取详情
export const getGradeMajorDirectionDetail = (id: number | string) => {
  return http.get<GradeMajorDirection>(`/grade-major-directions/${id}`)
}

// 根据学年和专业方向ID查询
export const getByYearAndDirection = (academicYear: number, directionId: number | string) => {
  return http.get<GradeMajorDirection>(`/grade-major-directions/year/${academicYear}/direction/${directionId}`)
}

// 为学年添加专业方向
export const addDirectionToYear = (data: GradeMajorDirection) => {
  return http.post<GradeMajorDirection>('/grade-major-directions', data)
}

// 批量为学年添加专业方向
export const batchAddDirectionsToYear = (academicYear: number, directionIds: (number | string)[]) => {
  return http.post(`/grade-major-directions/year/${academicYear}/batch`, directionIds)
}

// 更新学年专业方向配置
export const updateGradeMajorDirection = (id: number | string, data: GradeMajorDirection) => {
  return http.put(`/grade-major-directions/${id}`, data)
}

// 删除学年专业方向关联
export const deleteGradeMajorDirection = (id: number | string) => {
  return http.delete(`/grade-major-directions/${id}`)
}

// 批量删除学年专业方向关联
export const batchDeleteGradeMajorDirections = (ids: (number | string)[]) => {
  return http.delete('/grade-major-directions/batch', { data: ids })
}

// 兼容旧API - 根据年级ID获取（重定向到使用学年）
export const getDirectionsByGrade = (gradeId: number | string) => {
  // 保持兼容性，继续使用原有API直到前端全部迁移
  return http.get<GradeMajorDirection[]>(`/grade-major-directions/year/${gradeId}`)
}

// 兼容旧API - 为年级添加专业方向
export const addDirectionToGrade = (data: {
  gradeId: number | string
  majorDirectionId: number | string
  plannedClassCount?: number
  isEnabled?: number
}) => {
  return http.post<GradeMajorDirection>('/grade-major-directions', {
    academicYear: data.gradeId, // 使用 gradeId 作为 academicYear
    majorDirectionId: data.majorDirectionId,
    remarks: ''
  })
}
