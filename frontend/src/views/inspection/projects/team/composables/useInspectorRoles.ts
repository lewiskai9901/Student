/**
 * Phase F - 角色矩阵 toggle composable.
 * LEAD 不变量 (剩 1 LEAD 时拒绝移除) 由后端守护并以 409 返回, 前端在 UI 上禁用按钮 + tooltip 解释.
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { LongId } from '@/types/common'
import { addInspectorRole, removeInspectorRole } from '@/api/inspection/project'

export type InspectorRole = 'INSPECTOR' | 'REVIEWER' | 'LEAD'

export const RoleLabels: Record<InspectorRole, string> = {
  INSPECTOR: '检查员',
  REVIEWER: '审核员',
  LEAD: '项目负责人',
}

export function useInspectorRoles(projectId: { value: LongId }) {
  const pending = ref(false)

  async function toggleRole(userId: LongId, userName: string, role: InspectorRole, currentlyHas: boolean): Promise<boolean> {
    pending.value = true
    try {
      if (currentlyHas) {
        await removeInspectorRole(projectId.value, userId, role)
        ElMessage.success(`已移除 ${userName} 的「${RoleLabels[role]}」角色`)
      } else {
        await addInspectorRole(projectId.value, userId, role, userName)
        ElMessage.success(`已给 ${userName} 添加「${RoleLabels[role]}」角色`)
      }
      return true
    } catch (e: any) {
      // 后端 LEAD 不变量返回 message
      const msg = e?.response?.data?.message || e?.message || '操作失败'
      ElMessage.error(msg)
      return false
    } finally {
      pending.value = false
    }
  }

  return { pending, toggleRole, RoleLabels }
}
