// 认证相关类型定义

export interface LoginRequest {
  username: string
  password: string
  rememberMe?: boolean
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
}

export interface UserInfo {
  userId: string
  id?: number
  username: string
  realName: string
  phone?: string
  email?: string
  avatar?: string
  employeeNo?: string
  gender?: number
  status: number
  roles: string[]
  permissions: string[]
  lastLoginTime?: string
  department?: {
    departmentId: string
    departmentName: string
  }
  classInfo?: {
    classId: string
    className: string
  }
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface LogoutRequest {
  accessToken: string
}