export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
}

export interface UserInfo {
  userId: number
  username: string
  realName: string
  phone: string | null
  email: string | null
  avatar: string | null
  gender: number | null
  status: number
  roles: string[]
  permissions: string[]
  orgUnitId: number | null
  tenantId: number | null
}

export interface RefreshTokenRequest {
  refreshToken: string
}
