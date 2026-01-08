/**
 * 权限检查工具
 */
import { useUserStore } from '@/stores/user'

// 角色代码
export const RoleCode = {
  SUPER_ADMIN: 'SUPER_ADMIN',      // 超级管理员
  SCHOOL_ADMIN: 'SCHOOL_ADMIN',    // 学校管理员
  CLASS_TEACHER: 'CLASS_TEACHER',  // 班主任
  STUDENT: 'STUDENT',              // 学生
  DORMITORY_ADMIN: 'DORMITORY_ADMIN', // 宿舍管理员
}

// 权限代码
export const PermissionCode = {
  // 学生管理
  STUDENT_VIEW: 'student:view',
  STUDENT_CREATE: 'student:create',
  STUDENT_UPDATE: 'student:update',
  STUDENT_DELETE: 'student:delete',
  STUDENT_EXPORT: 'student:export',
  STUDENT_IMPORT: 'student:import',

  // 班级管理
  CLASS_VIEW: 'class:view',
  CLASS_CREATE: 'class:create',
  CLASS_UPDATE: 'class:update',
  CLASS_DELETE: 'class:delete',

  // 宿舍管理
  DORMITORY_VIEW: 'dormitory:view',
  DORMITORY_CREATE: 'dormitory:create',
  DORMITORY_UPDATE: 'dormitory:update',
  DORMITORY_DELETE: 'dormitory:delete',
  DORMITORY_ASSIGN: 'dormitory:assign',

  // 量化检查
  CHECK_PLAN_VIEW: 'check:plan:view',
  CHECK_PLAN_CREATE: 'check:plan:create',
  CHECK_PLAN_UPDATE: 'check:plan:update',
  CHECK_PLAN_DELETE: 'check:plan:delete',
  CHECK_RECORD_VIEW: 'check:record:view',
  CHECK_RECORD_SCORING: 'check:record:scoring',
  CHECK_TEMPLATE_VIEW: 'check:template:view',
  CHECK_TEMPLATE_MANAGE: 'check:template:manage',

  // 申诉管理
  APPEAL_VIEW: 'appeal:view',
  APPEAL_CREATE: 'appeal:create',
  APPEAL_REVIEW: 'appeal:review',

  // 统计
  STATISTICS_VIEW: 'statistics:view',
  STATISTICS_EXPORT: 'statistics:export',

  // 系统管理
  USER_VIEW: 'user:view',
  USER_MANAGE: 'user:manage',
  ROLE_VIEW: 'role:view',
  ROLE_MANAGE: 'role:manage',
  DEPARTMENT_VIEW: 'department:view',
  DEPARTMENT_MANAGE: 'department:manage',
  ANNOUNCEMENT_VIEW: 'announcement:view',
  ANNOUNCEMENT_MANAGE: 'announcement:manage',
}

/**
 * 检查是否有指定权限
 */
export function hasPermission(permission: string): boolean {
  const userStore = useUserStore()
  const permissions = userStore.userInfo?.permissions || []

  // 超级管理员拥有所有权限
  if (hasRole(RoleCode.SUPER_ADMIN)) {
    return true
  }

  return permissions.includes(permission)
}

/**
 * 检查是否有任意一个权限
 */
export function hasAnyPermission(permissions: string[]): boolean {
  return permissions.some(permission => hasPermission(permission))
}

/**
 * 检查是否有所有权限
 */
export function hasAllPermissions(permissions: string[]): boolean {
  return permissions.every(permission => hasPermission(permission))
}

/**
 * 检查是否有指定角色
 */
export function hasRole(role: string): boolean {
  const userStore = useUserStore()
  const roles = userStore.userInfo?.roles || []
  return roles.includes(role)
}

/**
 * 检查是否有任意一个角色
 */
export function hasAnyRole(roles: string[]): boolean {
  return roles.some(role => hasRole(role))
}

/**
 * 检查是否是管理员(超级管理员或学校管理员)
 */
export function isAdmin(): boolean {
  return hasAnyRole([RoleCode.SUPER_ADMIN, RoleCode.SCHOOL_ADMIN])
}

/**
 * 检查是否是超级管理员
 */
export function isSuperAdmin(): boolean {
  return hasRole(RoleCode.SUPER_ADMIN)
}

/**
 * 检查是否是班主任
 */
export function isClassTeacher(): boolean {
  return hasRole(RoleCode.CLASS_TEACHER)
}

/**
 * 检查是否是学生
 */
export function isStudent(): boolean {
  return hasRole(RoleCode.STUDENT)
}

/**
 * 页面级权限守卫
 * 如果没有权限，显示提示并返回上一页
 */
export function guardPage(requiredPermission: string): boolean {
  if (!hasPermission(requiredPermission)) {
    uni.showToast({
      title: '无权限访问',
      icon: 'none'
    })
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
    return false
  }
  return true
}

/**
 * 页面级角色守卫
 */
export function guardPageByRole(requiredRole: string): boolean {
  if (!hasRole(requiredRole)) {
    uni.showToast({
      title: '无权限访问',
      icon: 'none'
    })
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
    return false
  }
  return true
}

/**
 * 获取用户可访问的功能模块
 */
export function getAccessibleModules(): string[] {
  const modules: string[] = []

  // 学生管理
  if (hasPermission(PermissionCode.STUDENT_VIEW)) {
    modules.push('student')
  }

  // 班级管理
  if (hasPermission(PermissionCode.CLASS_VIEW)) {
    modules.push('class')
  }

  // 宿舍管理
  if (hasPermission(PermissionCode.DORMITORY_VIEW)) {
    modules.push('dormitory')
  }

  // 量化检查
  if (hasPermission(PermissionCode.CHECK_PLAN_VIEW)) {
    modules.push('check-plan')
  }
  if (hasPermission(PermissionCode.CHECK_RECORD_VIEW)) {
    modules.push('check-record')
  }
  if (hasPermission(PermissionCode.CHECK_RECORD_SCORING)) {
    modules.push('scoring')
  }

  // 申诉
  if (hasPermission(PermissionCode.APPEAL_VIEW) || hasPermission(PermissionCode.APPEAL_CREATE)) {
    modules.push('appeal')
  }

  // 统计
  if (hasPermission(PermissionCode.STATISTICS_VIEW)) {
    modules.push('statistics')
  }

  // 系统管理
  if (hasPermission(PermissionCode.USER_VIEW)) {
    modules.push('user')
  }
  if (hasPermission(PermissionCode.ANNOUNCEMENT_VIEW)) {
    modules.push('announcement')
  }

  return modules
}

/**
 * 检查是否可以访问指定模块
 */
export function canAccessModule(module: string): boolean {
  const accessibleModules = getAccessibleModules()
  return accessibleModules.includes(module)
}
