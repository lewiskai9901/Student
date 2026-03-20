import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginRequest, LoginResponse } from '@/types/auth'
import { login, logout, refreshToken, getCurrentUser } from '@/api/auth'
import { getToken, setToken, isTokenExpired } from '@/utils/token'
import { tokenStorage } from '@/utils/tokenStorage'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref<string>('')
  const refreshTokenValue = ref<string>('')
  const user = ref<LoginResponse['userInfo'] | null>(null)
  const permissions = ref<string[]>([])
  const tenantId = ref<number | string | undefined>(undefined)
  const tenantName = ref<string | undefined>(undefined)

  // 计算属性
  const isAuthenticated = computed(() => !!token.value && !isTokenExpired(token.value))
  const userName = computed(() => user.value?.realName || '')
  const userRoles = computed(() => user.value?.roles || [])

  // 登录
  const loginAction = async (loginData: LoginRequest) => {
    try {
      const response = await login(loginData)
      const { accessToken, refreshToken: newRefreshToken, userInfo } = response

      // 保存登录信息
      token.value = accessToken
      refreshTokenValue.value = newRefreshToken
      user.value = userInfo
      permissions.value = userInfo.permissions
      tenantId.value = userInfo.tenantId
      tenantName.value = userInfo.tenantName

      // 保存到安全存储
      setToken(accessToken)
      tokenStorage.setRefreshToken(newRefreshToken)
      tokenStorage.setUserInfo(userInfo)

      return response
    } catch (error) {
      console.error('登录失败:', error)
      throw error
    }
  }

  // 退出登录
  const logoutAction = async () => {
    try {
      if (token.value) {
        await logout({ accessToken: token.value })
      }
    } catch (error) {
      console.error('退出登录失败:', error)
    } finally {
      // 清除状态
      token.value = ''
      refreshTokenValue.value = ''
      user.value = null
      permissions.value = []
      tenantId.value = undefined
      tenantName.value = undefined

      // 清除安全存储
      tokenStorage.clearAll()
    }
  }

  // 刷新令牌
  const refreshTokenAction = async () => {
    try {
      if (!refreshTokenValue.value) {
        throw new Error('没有刷新令牌')
      }

      const response = await refreshToken({ refreshToken: refreshTokenValue.value })
      const { accessToken, refreshToken: newRefreshToken } = response

      token.value = accessToken
      refreshTokenValue.value = newRefreshToken

      setToken(accessToken)
      tokenStorage.setRefreshToken(newRefreshToken)

      return response
    } catch (error) {
      console.error('刷新令牌失败:', error)
      await logoutAction()
      throw error
    }
  }

  // 获取当前用户信息
  const getCurrentUserInfo = async () => {
    try {
      const userInfo = await getCurrentUser()
      user.value = userInfo
      permissions.value = userInfo.permissions
      tokenStorage.setUserInfo(userInfo)
      return userInfo
    } catch (error) {
      console.error('获取用户信息失败:', error)
      throw error
    }
  }

  // 检查权限
  const hasPermission = (permission: string) => {
    return permissions.value.includes(permission) || permissions.value.includes('*')
  }

  // 检查角色
  const hasRole = (role: string) => {
    return userRoles.value.includes(role)
  }

  // 初始化认证状态
  const initAuth = () => {
    const savedToken = getToken()
    const savedRefreshToken = tokenStorage.getRefreshToken()
    const savedUserInfo = tokenStorage.getUserInfo<LoginResponse['userInfo']>()

    if (savedToken && savedRefreshToken && savedUserInfo) {
      token.value = savedToken
      refreshTokenValue.value = savedRefreshToken
      user.value = savedUserInfo
      permissions.value = user.value?.permissions || []
    }
  }

  return {
    // 状态
    token,
    user,
    permissions,
    tenantId,
    tenantName,

    // 计算属性
    isAuthenticated,
    userName,
    userRoles,

    // 方法
    loginAction,
    logoutAction,
    refreshTokenAction,
    getCurrentUserInfo,
    hasPermission,
    hasRole,
    initAuth
  }
})