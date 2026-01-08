/**
 * 用户状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getStorage, setStorage, removeStorage } from '@/utils/storage'
import { config } from '@/utils/config'

// 用户信息接口
export interface UserInfo {
  id: number
  username: string
  realName: string
  avatar?: string
  phone?: string
  email?: string
  studentNo?: string
  classId?: number
  className?: string
  departmentId?: number
  departmentName?: string
  roles: string[]
  permissions: string[]
}

// 学生详情接口
export interface StudentInfo {
  id: number
  studentNo: string
  userId: number
  realName: string
  gender: number
  phone?: string
  email?: string
  idCard?: string
  classId: number
  className: string
  majorId?: number
  majorName?: string
  departmentId: number
  departmentName: string
  dormitoryId?: number
  dormitoryName?: string
  bedNumber?: string
  enrollmentDate?: string
  graduationDate?: string
  status: number
}

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>(getStorage<string>(config.TOKEN_KEY) || '')
  const refreshToken = ref<string>(getStorage<string>(config.REFRESH_TOKEN_KEY) || '')
  const userInfo = ref<UserInfo | null>(getStorage<UserInfo>(config.USER_INFO_KEY))
  const studentInfo = ref<StudentInfo | null>(null)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const isStudent = computed(() => userInfo.value?.roles?.includes('ROLE_STUDENT') || false)
  const isTeacher = computed(() => userInfo.value?.roles?.includes('ROLE_TEACHER') || false)
  const isAdmin = computed(() => userInfo.value?.roles?.includes('ROLE_ADMIN') || false)
  const userName = computed(() => userInfo.value?.realName || userInfo.value?.username || '')
  const userAvatar = computed(() => userInfo.value?.avatar || 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PGNpcmNsZSBjeD0iNTAiIGN5PSI1MCIgcj0iNTAiIGZpbGw9IiNlMGUwZTAiLz48Y2lyY2xlIGN4PSI1MCIgY3k9IjQwIiByPSIxOCIgZmlsbD0iI2JkYmRiZCIvPjxwYXRoIGQ9Ik0yMCA4NWMwLTIwIDEzLTMwIDMwLTMwczMwIDEwIDMwIDMwIiBmaWxsPSIjYmRiZGJkIi8+PC9zdmc+')

  // 操作
  function setToken(accessToken: string, newRefreshToken?: string) {
    token.value = accessToken
    setStorage(config.TOKEN_KEY, accessToken)
    if (newRefreshToken) {
      refreshToken.value = newRefreshToken
      setStorage(config.REFRESH_TOKEN_KEY, newRefreshToken)
    }
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    setStorage(config.USER_INFO_KEY, info)
  }

  function setStudentInfo(info: StudentInfo) {
    studentInfo.value = info
  }

  function hasPermission(permission: string): boolean {
    if (!userInfo.value?.permissions) return false
    return userInfo.value.permissions.includes(permission)
  }

  function hasRole(role: string): boolean {
    if (!userInfo.value?.roles) return false
    return userInfo.value.roles.includes(role)
  }

  function logout() {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    studentInfo.value = null
    removeStorage(config.TOKEN_KEY)
    removeStorage(config.REFRESH_TOKEN_KEY)
    removeStorage(config.USER_INFO_KEY)
  }

  // 检查登录状态
  function checkLogin(): boolean {
    if (!token.value) {
      uni.navigateTo({ url: '/pages/login/index' })
      return false
    }
    return true
  }

  return {
    // 状态
    token,
    refreshToken,
    userInfo,
    studentInfo,
    // 计算属性
    isLoggedIn,
    isStudent,
    isTeacher,
    isAdmin,
    userName,
    userAvatar,
    // 操作
    setToken,
    setUserInfo,
    setStudentInfo,
    hasPermission,
    hasRole,
    logout,
    checkLogin
  }
})
