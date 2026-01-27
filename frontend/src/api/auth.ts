import { http } from '@/utils/request'
import type { LoginRequest, LoginResponse, RefreshTokenRequest, LogoutRequest, UserInfo } from '@/types/auth'

// 用户登录
export function login(data: LoginRequest): Promise<LoginResponse> {
  return http.post<LoginResponse>('/auth/login', data)
}

// 刷新令牌
export function refreshToken(data: RefreshTokenRequest): Promise<LoginResponse> {
  return http.post<LoginResponse>('/auth/refresh', data)
}

// 用户登出
export function logout(data: LogoutRequest): Promise<void> {
  return http.post('/auth/logout', data)
}

// 获取当前用户信息
export function getCurrentUser(): Promise<UserInfo> {
  return http.get<UserInfo>('/auth/me')
}

// 更新个人资料请求参数
export interface ProfileUpdateRequest {
  realName?: string
  phone?: string
  email?: string
  avatar?: string
  gender?: number
}

// 更新个人资料
export function updateProfile(data: ProfileUpdateRequest): Promise<void> {
  return http.put('/auth/profile', data)
}

// 修改密码请求参数
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

// 修改密码
export function changePassword(data: ChangePasswordRequest): Promise<void> {
  return http.put('/auth/password', data)
}
