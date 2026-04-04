import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import { setTokens, clearTokens } from '@/utils/request'
import type { UserInfo, LoginRequest } from '@/types/auth'

export const useAuthStore = defineStore('auth', () => {
  const userInfo = ref<UserInfo | null>(null)
  const isLoggedIn = computed(() => !!userInfo.value)
  const permissions = computed(() => userInfo.value?.permissions || [])
  const roles = computed(() => userInfo.value?.roles || [])
  const realName = computed(() => userInfo.value?.realName || '')

  function hasPermission(code: string): boolean {
    return permissions.value.includes(code)
  }

  function init() {
    const cached = uni.getStorageSync('userInfo')
    if (cached) {
      try {
        userInfo.value = typeof cached === 'string' ? JSON.parse(cached) : cached
      } catch {
        userInfo.value = null
      }
    }
  }

  async function login(data: LoginRequest) {
    const res = await authApi.login(data)
    setTokens(res.accessToken, res.refreshToken)
    userInfo.value = res.userInfo
    uni.setStorageSync('userInfo', JSON.stringify(res.userInfo))
    return res
  }

  async function logout() {
    try {
      const refreshToken = uni.getStorageSync('refreshToken')
      await authApi.logout(refreshToken)
    } catch { /* ignore */ }
    userInfo.value = null
    clearTokens()
    uni.reLaunch({ url: '/pages/login/index' })
  }

  async function fetchUserInfo() {
    const info = await authApi.getMe()
    userInfo.value = info
    uni.setStorageSync('userInfo', JSON.stringify(info))
    return info
  }

  return {
    userInfo, isLoggedIn, permissions, roles, realName,
    hasPermission, init, login, logout, fetchUserInfo,
  }
})
