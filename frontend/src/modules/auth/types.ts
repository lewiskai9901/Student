/**
 * 认证模块类型定义
 */

// 登录请求
export interface LoginRequest {
  username: string
  password: string
  rememberMe?: boolean
}

// 登录响应
export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tokenType: string
  user: UserInfo
}

// 刷新令牌请求
export interface RefreshTokenRequest {
  refreshToken: string
}

// 登出请求
export interface LogoutRequest {
  accessToken: string
  refreshToken: string
}

// 用户信息
export interface UserInfo {
  id: number
  username: string
  realName: string
  phone?: string
  email?: string
  avatar?: string
  gender?: number
  status: number
  roles: RoleInfo[]
  permissions: string[]
}

// 角色信息
export interface RoleInfo {
  id: number
  roleCode: string
  roleName: string
}
