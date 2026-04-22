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

// #7 用户端角色禁用提示: 角色详情 (含插件禁用状态)
export interface RoleDetail {
  code: string
  name: string
  industry?: string
  pluginEnabled?: number | boolean
  status?: number
}

// 分配的班级信息
export interface AssignedClass {
  id: number | string
  className: string
  role: ClassRole
}

export interface UserInfo {
  userId: string
  id?: number | string
  username: string
  realName: string
  phone?: string
  email?: string
  avatar?: string
  gender?: number
  status: number
  roles: string[]
  /**
   * #7 新增: 角色详情数组, 含 pluginEnabled 标志. 旧字段 roles:string[] 保留兼容.
   * pluginEnabled=false 时表示该角色所属插件已被管理员禁用, Casbin 会跳过此角色权限计算.
   */
  roleDetails?: RoleDetail[]
  permissions: string[]
  lastLoginTime?: string
  tenantId?: number | string
  tenantName?: string
  // 用户类型编码（TEACHER / STUDENT / ADMIN / …），用于登录落地页路由决策
  userTypeCode?: string
  orgUnit?: {
    orgUnitId: string
    orgUnitName: string
  }
  classInfo?: {
    orgUnitId: string
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