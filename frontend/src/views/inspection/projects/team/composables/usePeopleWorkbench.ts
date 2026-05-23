/**
 * Phase B/C - 人员工作台数据加载 composable.
 *
 * 一次拉取 PeopleWorkbenchView 聚合视图, 提供 reactive state + reload action.
 * 视图组件 (PeopleView / TasksView / RoleMatrixView) 共享同一数据源.
 */
import { ref, computed, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { LongId } from '@/types/common'
import { getPeopleWorkbench, type PeopleWorkbenchView, type PersonRow } from '@/api/inspection/project'

export function usePeopleWorkbench(projectId: Ref<LongId>) {
  const data = ref<PeopleWorkbenchView | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function reload() {
    if (!projectId.value) return
    loading.value = true
    error.value = null
    try {
      data.value = await getPeopleWorkbench(projectId.value)
    } catch (e: any) {
      error.value = e?.message || '加载人员工作台失败'
      ElMessage.error(error.value || '加载失败')
    } finally {
      loading.value = false
    }
  }

  /** 项目级 summary, 顶部状态条用 */
  const summary = computed(() => data.value?.summary || null)
  /** 人员列表 — 已由后端按忙碌度排序 + LEAD 置顶 */
  const people = computed<PersonRow[]>(() => data.value?.people || [])
  /** 共享池待分配任务 */
  const pendingAssignTasks = computed(() => data.value?.pendingAssignTasks || [])
  const projectName = computed(() => data.value?.projectName || '')

  /** 按姓名过滤的人员列表 */
  function filterPeople(filter: string): PersonRow[] {
    const all = people.value
    if (!filter || !filter.trim()) return all
    const q = filter.trim().toLowerCase()
    return all.filter(p =>
      (p.userName || '').toLowerCase().includes(q) ||
      (p.orgUnitName || '').toLowerCase().includes(q)
    )
  }

  /** 按角色过滤 */
  function filterByRole(role: 'INSPECTOR' | 'REVIEWER' | 'LEAD' | 'ALL'): PersonRow[] {
    if (role === 'ALL') return people.value
    return people.value.filter(p => p.roles.includes(role))
  }

  /** 计算检查员忙/空闲 — assigned-completed > 3 = 忙, 否则空闲 */
  function getWorkloadLevel(p: PersonRow): '忙' | '空闲' | '中等' {
    const active = p.stats.totalAssigned - p.stats.totalCompleted
    if (active > 3) return '忙'
    if (active === 0) return '空闲'
    return '中等'
  }

  return {
    data,
    loading,
    error,
    summary,
    people,
    pendingAssignTasks,
    projectName,
    reload,
    filterPeople,
    filterByRole,
    getWorkloadLevel,
  }
}
