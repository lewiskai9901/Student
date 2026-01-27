import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  Student,
  StudentStatus,
  StudentQueryParams,
  CreateStudentRequest,
  UpdateStudentRequest,
  AssignDormitoryRequest
} from '@/types/student'
import { studentApi } from '@/api/student'

/**
 * 学生管理 Store (V2)
 * 基于DDD架构的学生管理状态管理
 */
export const useStudentStore = defineStore('student-v2', () => {
  // ==================== 状态 ====================
  const students = ref<Student[]>([])
  const currentStudent = ref<Student | null>(null)
  const loading = ref(false)
  const total = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(10)

  // 搜索结果缓存
  const searchResults = ref<Student[]>([])
  const searchLoading = ref(false)

  // ==================== 计算属性 ====================

  // 按状态过滤 (V2 使用 status 字段)
  const studentsByStatus = computed(() => {
    return (status: StudentStatus) => students.value.filter(s => s.status === status)
  })

  // 在读学生
  const activeStudents = computed(() => studentsByStatus.value(0))

  // 休学学生
  const suspendedStudents = computed(() => studentsByStatus.value(1))

  // 毕业学生
  const graduatedStudents = computed(() => studentsByStatus.value(3))

  // 按性别统计
  const genderStats = computed(() => ({
    male: students.value.filter(s => s.gender === 1).length,
    female: students.value.filter(s => s.gender === 2).length
  }))

  // ==================== 学生操作 ====================

  /**
   * 加载学生列表
   */
  const loadStudents = async (params?: StudentQueryParams) => {
    loading.value = true
    try {
      const data = await studentApi.getList(params)
      // request.ts 已解包，data 直接是响应数据
      if (data) {
        students.value = data.records || []
        total.value = data.total || 0
        currentPage.value = params?.pageNum || 1
        pageSize.value = params?.pageSize || 10
      }
    } catch (error) {
      console.error('加载学生列表失败:', error)
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取学生详情
   */
  const getStudent = async (id: number) => {
    try {
      const data = await studentApi.getById(id)
      currentStudent.value = data || null
      return data
    } catch (error) {
      console.error('获取学生详情失败:', error)
      return null
    }
  }

  /**
   * 根据学号获取学生
   */
  const getStudentByNo = async (studentNo: string) => {
    try {
      const data = await studentApi.getByNo(studentNo)
      return data || null
    } catch (error) {
      console.error('根据学号获取学生失败:', error)
      return null
    }
  }

  /**
   * 根据班级获取学生列表
   */
  const getStudentsByClass = async (classId: number) => {
    try {
      const data = await studentApi.getByClass(classId)
      return Array.isArray(data) ? data : []
    } catch (error) {
      console.error('根据班级获取学生失败:', error)
      return []
    }
  }

  /**
   * 搜索学生
   */
  const searchStudents = async (keyword: string, classId?: number, limit = 10) => {
    searchLoading.value = true
    try {
      const data = await studentApi.search({ keyword, classId, limit })
      searchResults.value = Array.isArray(data) ? data : []
      return searchResults.value
    } catch (error) {
      console.error('搜索学生失败:', error)
      return []
    } finally {
      searchLoading.value = false
    }
  }

  /**
   * 创建学生
   */
  const createStudent = async (data: CreateStudentRequest) => {
    try {
      const result = await studentApi.create(data)
      await loadStudents({ pageNum: currentPage.value, pageSize: pageSize.value })
      return { success: true, data: result }
    } catch (error: any) {
      console.error('创建学生失败:', error)
      return { success: false, message: error.message || '创建失败' }
    }
  }

  /**
   * 更新学生
   */
  const updateStudent = async (id: number, data: UpdateStudentRequest) => {
    try {
      await studentApi.update(id, data)
      await getStudent(id)
      return { success: true }
    } catch (error: any) {
      console.error('更新学生失败:', error)
      return { success: false, message: error.message || '更新失败' }
    }
  }

  /**
   * 删除学生
   */
  const deleteStudent = async (id: number) => {
    try {
      await studentApi.delete(id)
      await loadStudents({ pageNum: currentPage.value, pageSize: pageSize.value })
      return { success: true }
    } catch (error: any) {
      console.error('删除学生失败:', error)
      return { success: false, message: error.message || '删除失败' }
    }
  }

  /**
   * 批量删除学生
   */
  const deleteStudents = async (ids: number[]) => {
    const result = await studentApi.deleteBatch(ids)
    await loadStudents({ pageNum: currentPage.value, pageSize: pageSize.value })
    return result
  }

  /**
   * 检查学号是否存在
   */
  const existsStudentNo = async (studentNo: string, excludeId?: number) => {
    try {
      const data = await studentApi.exists(studentNo, excludeId)
      return data || false
    } catch (error) {
      console.error('检查学号失败:', error)
      return false
    }
  }

  // ==================== 状态操作 ====================

  /**
   * 更新学生状态
   */
  const updateStudentStatus = async (id: number, status: StudentStatus) => {
    const result = await studentApi.updateStatus(id, status)
    await getStudent(id)
    return result
  }

  /**
   * 分配宿舍
   */
  const assignDormitory = async (id: number, data: AssignDormitoryRequest) => {
    const result = await studentApi.assignDormitory(id, data)
    await getStudent(id)
    return result
  }

  /**
   * 学生转班
   */
  const transferClass = async (id: number, newClassId: number) => {
    const result = await studentApi.transferClass(id, newClassId)
    await getStudent(id)
    return result
  }

  /**
   * 重置密码
   */
  const resetPassword = async (id: number, newPassword: string) => {
    const result = await studentApi.resetPassword(id, { newPassword })
    return result
  }

  // ==================== 统计操作 ====================

  /**
   * 统计班级学生数量
   */
  const countByClass = async (classId: number) => {
    try {
      const data = await studentApi.countByClass(classId)
      return data || 0
    } catch (error) {
      console.error('统计班级学生数量失败:', error)
      return 0
    }
  }

  /**
   * 统计宿舍学生数量
   */
  const countByDormitory = async (dormitoryId: number) => {
    try {
      const data = await studentApi.countByDormitory(dormitoryId)
      return data || 0
    } catch (error) {
      console.error('统计宿舍学生数量失败:', error)
      return 0
    }
  }

  // ==================== 导入导出 ====================

  /**
   * 导出学生数据
   */
  const exportStudents = async (params?: StudentQueryParams) => {
    try {
      const blob = await studentApi.export(params)
      return blob
    } catch (error) {
      console.error('导出学生数据失败:', error)
      throw error
    }
  }

  /**
   * 导入学生数据
   */
  const importStudents = async (file: File) => {
    const result = await studentApi.import(file)
    await loadStudents({ pageNum: currentPage.value, pageSize: pageSize.value })
    return result
  }

  /**
   * 下载导入模板
   */
  const downloadTemplate = async () => {
    try {
      const blob = await studentApi.downloadTemplate()
      return blob
    } catch (error) {
      console.error('下载导入模板失败:', error)
      throw error
    }
  }

  // ==================== View 兼容方法 ====================

  /**
   * 获取学生列表 (View 兼容方法)
   * 返回 { success, data } 格式供视图使用
   */
  const getStudentList = async (params?: StudentQueryParams) => {
    loading.value = true
    try {
      const data = await studentApi.getList(params)
      if (data) {
        students.value = data.records || []
        total.value = data.total || 0
        currentPage.value = params?.pageNum || 1
        pageSize.value = params?.pageSize || 10
        return {
          success: true,
          data: {
            records: data.records || [],
            total: data.total || 0
          }
        }
      }
      return { success: false, data: null }
    } catch (error) {
      console.error('加载学生列表失败:', error)
      return { success: false, data: null }
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取学生统计数据 (View 兼容方法)
   */
  const getStudentStatistics = async () => {
    // 基于当前加载的学生列表计算统计
    // 实际生产环境应该调用后端API
    const stats = {
      total: total.value,
      active: students.value.filter(s => s.status === 0).length,  // V2: status
      suspended: students.value.filter(s => s.status === 1).length,  // V2: status
      graduated: students.value.filter(s => s.status === 3).length,  // V2: status
      byGender: {
        male: students.value.filter(s => s.gender === 1).length,
        female: students.value.filter(s => s.gender === 2).length
      }
    }
    return { success: true, data: stats }
  }

  // ==================== 重置状态 ====================
  const reset = () => {
    students.value = []
    currentStudent.value = null
    total.value = 0
    currentPage.value = 1
    pageSize.value = 10
    searchResults.value = []
  }

  return {
    // 状态
    students,
    currentStudent,
    loading,
    total,
    currentPage,
    pageSize,
    searchResults,
    searchLoading,

    // 计算属性
    studentsByStatus,
    activeStudents,
    suspendedStudents,
    graduatedStudents,
    genderStats,

    // 学生操作
    loadStudents,
    getStudentList,  // View 兼容方法
    getStudentStatistics,  // View 兼容方法
    getStudent,
    getStudentByNo,
    getStudentsByClass,
    searchStudents,
    createStudent,
    updateStudent,
    deleteStudent,
    deleteStudents,
    existsStudentNo,

    // 状态操作
    updateStudentStatus,
    assignDormitory,
    transferClass,
    resetPassword,

    // 统计操作
    countByClass,
    countByDormitory,

    // 导入导出
    exportStudents,
    importStudents,
    downloadTemplate,

    // 工具方法
    reset
  }
})
