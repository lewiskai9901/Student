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

// 班级角色类型
export type ClassRole = 'HEAD_TEACHER' | 'DEPUTY_HEAD_TEACHER' | 'SUBJECT_TEACHER' | 'COUNSELOR'

// 分配的班级信息
export interface AssignedClass {
  id: number
  className: string
  role: ClassRole
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
  orgUnit?: {
    orgUnitId: string
    orgUnitName: string
  }
  classInfo?: {
    classId: string
    className: string
  }
  // 用户分配的班级列表（用于"我的班级"等功能的菜单权限控制）
  assignedClasses?: AssignedClass[]
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface LogoutRequest {
  accessToken: string
}