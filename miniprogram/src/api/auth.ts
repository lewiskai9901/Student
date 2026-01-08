/**
 * 认证相关接口
 */
import { post, get } from '@/utils/request'
import { clearToken, saveToken } from '@/utils/request'
import type { UserInfo } from '@/stores/user'

// 登录请求参数
export interface LoginParams {
  username: string
  password: string
  loginType?: number  // 1-PC端 2-小程序
  deviceInfo?: DeviceInfo
}

// 设备信息
export interface DeviceInfo {
  deviceId?: string
  platform?: string
  version?: string
}

// 微信登录参数
export interface WxLoginParams {
  code: string
  nickName?: string
  avatarUrl?: string
  deviceInfo?: DeviceInfo
}

// 微信绑定参数
export interface WxBindParams {
  code: string
  username: string
  password: string
  deviceInfo?: DeviceInfo
}

// 登录响应
export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo?: UserInfoResponse
}

// 微信登录响应
export interface WxLoginResponse {
  bound: boolean
  openId?: string  // 未绑定时返回
  loginResponse?: LoginResponse  // 已绑定时返回
}

// 用户信息响应
export interface UserInfoResponse extends UserInfo {}

/**
 * 获取设备信息
 */
export function getDeviceInfo(): DeviceInfo {
  const systemInfo = uni.getSystemInfoSync()
  return {
    deviceId: systemInfo.deviceId || '',
    platform: 'miniapp',
    version: systemInfo.appVersion || '1.0.0'
  }
}

/**
 * 用户名密码登录
 */
export async function login(params: LoginParams): Promise<LoginResponse> {
  const loginParams = {
    ...params,
    loginType: 2,  // 小程序端
    deviceInfo: getDeviceInfo()
  }
  const data = await post<LoginResponse>('/auth/login', loginParams, { showLoading: true })
  // 保存到 storage（用于请求拦截器）
  saveToken(data.accessToken, data.refreshToken)
  return data
}

/**
 * 微信小程序登录
 */
export async function wxLogin(params: WxLoginParams): Promise<WxLoginResponse> {
  const loginParams = {
    ...params,
    deviceInfo: getDeviceInfo()
  }
  const data = await post<WxLoginResponse>('/miniapp/auth/wx-login', loginParams, { showLoading: true })

  // 如果已绑定，保存token
  if (data.bound && data.loginResponse) {
    saveToken(data.loginResponse.accessToken, data.loginResponse.refreshToken)
  }

  return data
}

/**
 * 绑定微信账号到系统账号
 */
export async function bindWxAccount(params: WxBindParams): Promise<LoginResponse> {
  const bindParams = {
    ...params,
    deviceInfo: getDeviceInfo()
  }
  const data = await post<LoginResponse>('/miniapp/auth/bind', bindParams, { showLoading: true })
  saveToken(data.accessToken, data.refreshToken)
  return data
}

/**
 * 检查微信是否已绑定
 */
export async function checkWxBinding(openId: string): Promise<boolean> {
  const data = await get<boolean>('/miniapp/auth/check-binding', { openId }, { showLoading: false })
  return data
}

/**
 * 获取当前用户信息
 */
export async function getUserInfo(): Promise<UserInfoResponse> {
  return get<UserInfoResponse>('/auth/me', null, { showLoading: false })
}

/**
 * 退出登录
 */
export async function logout(): Promise<void> {
  try {
    await post('/auth/logout', null, { showError: false })
  } finally {
    clearToken()
  }
}

/**
 * 修改密码
 */
export async function changePassword(params: {
  oldPassword: string
  newPassword: string
}): Promise<void> {
  await post('/auth/change-password', params)
}

/**
 * 更新用户头像
 */
export async function updateAvatar(avatarUrl: string): Promise<void> {
  await post('/users/avatar', { avatar: avatarUrl })
}

/**
 * 更新用户基本信息
 */
export async function updateUserProfile(params: {
  realName?: string
  phone?: string
  email?: string
}): Promise<void> {
  await post('/users/profile', params)
}
