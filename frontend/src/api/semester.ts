/**
 * 学期管理 API - DDD架构适配
 *
 * 使用纯 DDD 端点: /domain/semesters
 */
import { http } from '@/utils/request'
import type {
  Semester,
  CreateSemesterRequest,
  UpdateSemesterRequest
} from '@/types/semester'

// 后端API路径 - 纯DDD端点
const SEMESTER_URL = '/domain/semesters'

// ==================== 学期 CRUD API ====================

/**
 * 创建学期
 */
export function createSemester(data: CreateSemesterRequest): Promise<Semester> {
  return http.post<Semester>(SEMESTER_URL, data)
}

/**
 * 更新学期
 */
export function updateSemester(id: number | string, data: UpdateSemesterRequest): Promise<Semester> {
  return http.put<Semester>(`${SEMESTER_URL}/${id}`, data)
}

/**
 * 删除学期
 */
export function deleteSemester(id: number | string): Promise<void> {
  return http.delete(`${SEMESTER_URL}/${id}`)
}

/**
 * 获取学期详情
 */
export function getSemester(id: number | string): Promise<Semester> {
  return http.get<Semester>(`${SEMESTER_URL}/${id}`)
}

/**
 * 根据编码获取学期
 */
export function getSemesterByCode(code: string): Promise<Semester> {
  return http.get<Semester>(`${SEMESTER_URL}/by-code/${code}`)
}

/**
 * 获取当前学期
 */
export function getCurrentSemester(): Promise<Semester> {
  return http.get<Semester>(`${SEMESTER_URL}/current`)
}

/**
 * 获取所有正常状态的学期
 */
export function getActiveSemesters(): Promise<Semester[]> {
  return http.get<Semester[]>(`${SEMESTER_URL}/active`)
}

/**
 * 获取所有学期
 */
export function getAllSemesters(): Promise<Semester[]> {
  return http.get<Semester[]>(SEMESTER_URL)
}

/**
 * 根据年份获取学期列表
 */
export function getSemestersByYear(year: number): Promise<Semester[]> {
  return http.get<Semester[]>(`${SEMESTER_URL}/by-year/${year}`)
}

/**
 * 检查学期编码是否存在
 */
export function checkSemesterCodeExists(semesterCode: string, excludeId?: number | string): Promise<boolean> {
  return http.get<boolean>(`${SEMESTER_URL}/exists`, {
    params: { semesterCode, excludeId }
  })
}

/**
 * 生成学期编码
 */
export function generateSemesterCode(startYear: number, semesterType: number): Promise<string> {
  return http.get<string>(`${SEMESTER_URL}/generate-code`, {
    params: { startYear, semesterType }
  })
}

// ==================== 状态操作 API ====================

/**
 * 设置当前学期
 */
export function setCurrentSemester(id: number | string): Promise<Semester> {
  return http.put<Semester>(`${SEMESTER_URL}/${id}/set-current`)
}

/**
 * 结束学期
 */
export function endSemester(id: number | string): Promise<Semester> {
  return http.post<Semester>(`${SEMESTER_URL}/${id}/end`)
}

/**
 * 重新激活学期
 */
export function reactivateSemester(id: number | string): Promise<Semester> {
  return http.post<Semester>(`${SEMESTER_URL}/${id}/reactivate`)
}

// ==================== API 对象封装（供 Store 使用） ====================

/**
 * 学期 API 对象
 */
export const semesterApi = {
  // CRUD
  create: createSemester,
  update: updateSemester,
  delete: deleteSemester,
  getById: getSemester,
  getByCode: getSemesterByCode,
  getCurrent: getCurrentSemester,
  getActive: getActiveSemesters,
  getAll: getAllSemesters,
  getByYear: getSemestersByYear,
  checkCodeExists: checkSemesterCodeExists,
  generateCode: generateSemesterCode,

  // 状态
  setCurrent: setCurrentSemester,
  end: endSemester,
  reactivate: reactivateSemester
}

export default semesterApi
