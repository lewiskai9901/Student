import { http } from '@/utils/request'
import type { Class, ClassQueryParams, ClassFormData, ClassDormitoryInfo } from '@/types/class'

// V2 API 基础路径
const BASE_URL = '/v2/organization/classes'

/**
 * 获取班级列表
 */
export function getClassList(params: ClassQueryParams) {
  return http.get<{
    records: Class[]
    total: number
  }>(BASE_URL, { params })
}

/**
 * 获取班级详情
 */
export function getClassDetail(id: number) {
  return http.get<Class>(`${BASE_URL}/${id}`)
}

/**
 * 创建班级
 */
export function createClass(data: ClassFormData) {
  return http.post<Class>(BASE_URL, data)
}

/**
 * 更新班级信息
 */
export function updateClass(id: number, data: ClassFormData) {
  return http.put<Class>(`${BASE_URL}/${id}`, data)
}

/**
 * 删除班级
 */
export function deleteClass(id: number) {
  return http.delete(`${BASE_URL}/${id}`)
}

/**
 * 批量删除班级
 */
export function batchDeleteClasses(ids: number[]) {
  return http.delete(`${BASE_URL}/batch`, { data: { ids } })
}

/**
 * 获取部门列表 (使用V2 org-units)
 */
export function getDepartmentList() {
  return http.get('/v2/org-units/tree')
}

/**
 * 获取专业列表
 */
export function getMajorList(departmentId?: number) {
  return http.get('/majors', {
    params: departmentId ? { departmentId } : undefined
  })
}

/**
 * 获取教师列表
 */
export function getTeacherList() {
  return http.get('/users', {
    params: {
      pageNum: 1,
      pageSize: 1000
    }
  }).then((res: any) => {
    return res.records || []
  })
}

/**
 * 获取班级学生列表
 */
export function getClassStudents(classId: number) {
  return http.get(`/v2/students/by-class/${classId}`)
}

/**
 * 导出班级数据
 */
export function exportClasses(params: ClassQueryParams) {
  return http.get(`${BASE_URL}/export`, {
    params,
    responseType: 'blob'
  })
}

/**
 * 设置班主任
 */
export function assignTeacher(classId: number, teacherId: number | null) {
  return http.post(`${BASE_URL}/${classId}/head-teacher`, {
    teacherId,
    teacherName: ''
  })
}

/**
 * 激活班级
 */
export function activateClass(classId: number) {
  return http.post(`${BASE_URL}/${classId}/activate`)
}

/**
 * 班级毕业
 */
export function graduateClass(classId: number) {
  return http.post(`${BASE_URL}/${classId}/graduate`)
}

/**
 * 撤销班级
 */
export function dissolveClass(classId: number) {
  return http.post(`${BASE_URL}/${classId}/dissolve`)
}

/**
 * 为班级分配教室
 */
export function assignClassroom(classId: number, classroomId: number) {
  return http.post(`/classes/${classId}/assign-classroom`, null, {
    params: { classroomId }
  })
}

/**
 * 取消班级教室分配
 */
export function removeClassroom(classId: number) {
  return http.delete(`/classes/${classId}/classroom`)
}

/**
 * 获取班级的教室信息
 */
export function getClassClassroom(classId: number) {
  return http.get(`/classes/${classId}/classroom`)
}

/**
 * 为班级添加宿舍
 */
export function addDormitory(classId: number, dormitoryId: number, allocatedBeds: number) {
  return http.post(`/classes/${classId}/dormitories`, null, {
    params: { dormitoryId, allocatedBeds }
  })
}

/**
 * 移除班级宿舍
 */
export function removeDormitory(classId: number, dormitoryId: number) {
  return http.delete(`/classes/${classId}/dormitories/${dormitoryId}`)
}

/**
 * 获取班级的宿舍列表
 */
export function getClassDormitories(classId: number) {
  return http.get<ClassDormitoryInfo[]>(`/classes/${classId}/dormitories`)
}

/**
 * 获取所有教室列表
 */
export function getClassroomList() {
  return http.get('/teaching/classrooms', {
    params: {
      pageNum: 1,
      pageSize: 1000,
      status: 1
    }
  }).then((res: any) => {
    return res.records || []
  })
}

/**
 * 获取所有宿舍列表
 */
export function getDormitoryList() {
  return http.get('/v2/dormitory/rooms', {
    params: {
      pageNum: 1,
      pageSize: 1000,
      status: 1
    }
  }).then((res: any) => {
    return res.records || []
  })
}

/**
 * 根据部门ID获取宿舍列表
 */
export function getDormitoriesByDepartment(departmentId: number) {
  return http.get('/v2/dormitory/rooms', {
    params: {
      departmentId
    }
  })
}

/**
 * 获取所有班级(不分页,用于下拉选择)
 */
export function getAllClasses() {
  return http.get(BASE_URL, {
    params: {
      pageNum: 1,
      pageSize: 1000
    }
  }).then((res: any) => {
    return res.records || []
  })
}

/**
 * 检查班级编码是否存在
 */
export function checkClassCodeExists(classCode: string) {
  return http.get<boolean>(`${BASE_URL}/check-code`, {
    params: { classCode }
  })
}

/**
 * 根据班主任ID获取班级
 */
export function getClassesByHeadTeacher(teacherId: number) {
  return http.get(`${BASE_URL}/head-teacher/${teacherId}`)
}

/**
 * 获取即将毕业的班级
 */
export function getGraduatingClasses(year: number) {
  return http.get(`${BASE_URL}/graduating`, {
    params: { year }
  })
}
