/**
 * V7 检查平台 - 权限检查 Composable
 *
 * 基于 auth store 中的 permissions (string[]) 提供检查模块细粒度权限判断。
 * 权限字符串格式: "resource:action"，例如 "insp:template:create"
 *
 * auth store 的 hasPermission 方法支持通配符 "*" 匹配所有权限。
 */
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

export function useInspPermission() {
  const authStore = useAuthStore()

  /**
   * 检查是否拥有指定权限
   * 支持多段匹配: "insp:template:create"
   * 通配符: "*" 匹配所有, "insp:*" 匹配 insp 下所有
   */
  function hasPermission(resource: string, action: string): boolean {
    const perm = `${resource}:${action}`
    // 精确匹配或全局通配符
    if (authStore.hasPermission(perm) || authStore.hasPermission('*')) {
      return true
    }
    // 资源通配符, 例如 "insp:template:*"
    if (authStore.hasPermission(`${resource}:*`)) {
      return true
    }
    // 模块通配符, 例如 "insp:*"
    const parts = resource.split(':')
    if (parts.length > 1 && authStore.hasPermission(`${parts[0]}:*`)) {
      return true
    }
    return false
  }

  // ==================== 模板权限 ====================

  const canCreateTemplate = computed(() => hasPermission('insp:template', 'create'))
  const canEditTemplate = computed(() => hasPermission('insp:template', 'edit'))
  const canPublishTemplate = computed(() => hasPermission('insp:template', 'publish'))
  const canDeleteTemplate = computed(() => hasPermission('insp:template', 'delete'))

  // ==================== 项目权限 ====================

  const canCreateProject = computed(() => hasPermission('insp:project', 'create'))
  const canManageProject = computed(() => hasPermission('insp:project', 'manage'))

  // ==================== 任务权限 ====================

  const canClaimTask = computed(() => hasPermission('insp:task', 'claim'))
  const canReviewTask = computed(() => hasPermission('insp:task', 'review'))
  const canPublishTask = computed(() => hasPermission('insp:task', 'publish'))

  // ==================== 整改权限 ====================

  const canCreateCase = computed(() => hasPermission('insp:corrective', 'create'))
  const canAssignCase = computed(() => hasPermission('insp:corrective', 'assign'))
  const canVerifyCase = computed(() => hasPermission('insp:corrective', 'verify'))
  const canCloseCase = computed(() => hasPermission('insp:corrective', 'close'))

  // ==================== 统计分析权限 ====================

  const canViewAnalytics = computed(() => hasPermission('insp:analytics', 'view'))
  const canManageAnalytics = computed(() => hasPermission('insp:analytics', 'manage'))

  // ==================== 平台管理权限 ====================

  const canManagePlatform = computed(() => hasPermission('insp:platform', 'manage'))
  const canViewPlatform = computed(() => hasPermission('insp:platform', 'view'))

  return {
    hasPermission,
    // 模板
    canCreateTemplate,
    canEditTemplate,
    canPublishTemplate,
    canDeleteTemplate,
    // 项目
    canCreateProject,
    canManageProject,
    // 任务
    canClaimTask,
    canReviewTask,
    canPublishTask,
    // 整改
    canCreateCase,
    canAssignCase,
    canVerifyCase,
    canCloseCase,
    // 统计分析
    canViewAnalytics,
    canManageAnalytics,
    // 平台
    canManagePlatform,
    canViewPlatform,
  }
}
