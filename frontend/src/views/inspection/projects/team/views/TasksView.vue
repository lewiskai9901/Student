<script setup lang="ts">
/**
 * Phase E - 「按任务」视图.
 *
 * 顶部 4 tab 按状态切换 (待分配 / 待审核 / 进行中 / 逾期);
 * 待分配 tab 支持多选 + 批量指派给某检查员; 其他 tab 单条操作 + 批量催办.
 */
import { ref, computed, watch } from 'vue'
import { ElButton, ElMessage, ElMessageBox, ElSelect, ElOption, ElCheckbox, ElTooltip } from 'element-plus'
import { Check, X, Send, AlertCircle } from 'lucide-vue-next'
import type { LongId } from '@/types/common'
import type { PersonRow, WorkbenchTaskRow } from '@/api/inspection/project'
import { batchAssignTasks } from '@/api/inspection/project'
import { assignTask, rejectTask, withdrawTask } from '@/api/inspection/task'
import { http } from '@/utils/request'

const props = defineProps<{
  projectId: LongId
  people: PersonRow[]
  pendingAssignTasks: WorkbenchTaskRow[]
  defaultTab?: 'pendingAssign' | 'pendingReview' | 'inProgress' | 'overdue'
}>()

const emit = defineEmits<{ (e: 'reload'): void }>()

type Tab = 'pendingAssign' | 'pendingReview' | 'inProgress' | 'overdue'
const activeTab = ref<Tab>(props.defaultTab || 'pendingAssign')

watch(() => props.defaultTab, (newTab) => {
  if (newTab) activeTab.value = newTab
})

// 聚合 3 段任务 (每个 person 的) 到全局列表
const allInProgress = computed(() => props.people.flatMap(p => p.tasks.inProgress))
const allOverdue = computed(() => props.people.flatMap(p => p.tasks.overdue))
// 待审核任务可能在多个审核员的 tasks.pendingReview 里, 去重
const allPendingReview = computed(() => {
  const seen = new Set<string>()
  const list: WorkbenchTaskRow[] = []
  for (const p of props.people) {
    for (const t of p.tasks.pendingReview) {
      const key = String(t.taskId)
      if (!seen.has(key)) {
        seen.add(key)
        list.push(t)
      }
    }
  }
  return list
})

const tabCounts = computed(() => ({
  pendingAssign: props.pendingAssignTasks.length,
  pendingReview: allPendingReview.value.length,
  inProgress: allInProgress.value.length,
  overdue: allOverdue.value.length,
}))

const currentTasks = computed<WorkbenchTaskRow[]>(() => {
  switch (activeTab.value) {
    case 'pendingAssign': return props.pendingAssignTasks
    case 'pendingReview': return allPendingReview.value
    case 'inProgress': return allInProgress.value
    case 'overdue': return allOverdue.value
    default: return []
  }
})

// === 批量选择 ===
const selectedIds = ref<Set<string>>(new Set())
watch(activeTab, () => { selectedIds.value = new Set() })
const allSelected = computed({
  get: () => currentTasks.value.length > 0 && currentTasks.value.every(t => selectedIds.value.has(String(t.taskId))),
  set: (val) => {
    if (val) {
      selectedIds.value = new Set(currentTasks.value.map(t => String(t.taskId)))
    } else {
      selectedIds.value = new Set()
    }
  },
})
function toggleOne(taskId: LongId, checked: boolean) {
  const next = new Set(selectedIds.value)
  if (checked) next.add(String(taskId))
  else next.delete(String(taskId))
  selectedIds.value = next
}
function isSelected(taskId: LongId) { return selectedIds.value.has(String(taskId)) }

// === 候选人 ===
const candidates = computed(() => props.people.filter(p =>
  p.isActive && (p.roles.includes('INSPECTOR') || p.roles.includes('LEAD'))
))
function workloadOf(p: PersonRow): '忙' | '空闲' | '中等' {
  const active = p.stats.totalAssigned - p.stats.totalCompleted
  if (active > 3) return '忙'
  if (active === 0) return '空闲'
  return '中等'
}

// === 批量指派 ===
const batchInspectorId = ref<LongId | null>(null)
async function doBatchAssign() {
  if (selectedIds.value.size === 0) {
    ElMessage.warning('请先勾选任务')
    return
  }
  if (!batchInspectorId.value) {
    ElMessage.warning('请选择目标检查员')
    return
  }
  const target = candidates.value.find(p => String(p.userId) === String(batchInspectorId.value))
  if (!target) return
  try {
    const taskIds = Array.from(selectedIds.value) as unknown as LongId[]
    const result = await batchAssignTasks(props.projectId, {
      taskIds,
      inspectorId: target.userId,
      inspectorName: target.userName,
    })
    if (result.failureCount === 0) {
      ElMessage.success(`已批量指派 ${result.successCount} 个任务给 ${target.userName}`)
    } else {
      ElMessage.warning(`部分成功: 成功 ${result.successCount} / 失败 ${result.failureCount}`)
    }
    selectedIds.value = new Set()
    batchInspectorId.value = null
    emit('reload')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '批量指派失败')
  }
}

async function batchUrge() {
  if (selectedIds.value.size === 0) { ElMessage.warning('请先勾选任务'); return }
  ElMessage.success(`已发送催办: ${selectedIds.value.size} 个 (功能开发中)`)
  selectedIds.value = new Set()
}

// === 单条操作 ===
async function assignOne(taskId: LongId, inspectorId: LongId) {
  const target = candidates.value.find(p => String(p.userId) === String(inspectorId))
  if (!target) return
  try {
    await assignTask(taskId, { inspectorId: target.userId, inspectorName: target.userName })
    ElMessage.success(`已指派给 ${target.userName}`)
    emit('reload')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '指派失败')
  }
}

async function approveOne(taskId: LongId) {
  try {
    await http.post(`/inspection/tasks/${taskId}/review`, { comment: '通过' })
    ElMessage.success('已通过')
    emit('reload')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '审核失败')
  }
}

async function rejectOne(taskId: LongId) {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回理由', '驳回任务', {
      confirmButtonText: '驳回', cancelButtonText: '取消',
      inputValidator: (v: string) => !!v?.trim() || '请填写理由',
    })
    await rejectTask(taskId, value)
    ElMessage.success('已驳回')
    emit('reload')
  } catch (e: any) {
    if (e === 'cancel') return
    ElMessage.error(e?.response?.data?.message || '驳回失败')
  }
}

async function withdrawOne(taskId: LongId) {
  try {
    await ElMessageBox.confirm('确定撤回该任务?', '撤回任务', { type: 'warning' })
    await withdrawTask(taskId)
    ElMessage.success('已撤回')
    emit('reload')
  } catch (e: any) {
    if (e === 'cancel') return
    ElMessage.error(e?.response?.data?.message || '撤回失败')
  }
}
</script>

<template>
  <div class="tv">
    <!-- Tab 切换 -->
    <div class="tv-tabs">
      <button class="tv-tab" :class="{ active: activeTab === 'pendingAssign' }"
              @click="activeTab = 'pendingAssign'">
        待分配 <span class="tv-tab-num">{{ tabCounts.pendingAssign }}</span>
      </button>
      <button class="tv-tab" :class="{ active: activeTab === 'pendingReview' }"
              @click="activeTab = 'pendingReview'">
        待审核 <span class="tv-tab-num">{{ tabCounts.pendingReview }}</span>
      </button>
      <button class="tv-tab" :class="{ active: activeTab === 'inProgress' }"
              @click="activeTab = 'inProgress'">
        进行中 <span class="tv-tab-num">{{ tabCounts.inProgress }}</span>
      </button>
      <button class="tv-tab tv-tab--danger" :class="{ active: activeTab === 'overdue' }"
              @click="activeTab = 'overdue'">
        逾期 <span class="tv-tab-num">{{ tabCounts.overdue }}</span>
      </button>
    </div>

    <!-- 批量操作栏 -->
    <div v-if="currentTasks.length > 0" class="tv-batch-bar">
      <ElCheckbox v-model="allSelected">
        全选 ({{ selectedIds.size }}/{{ currentTasks.length }})
      </ElCheckbox>
      <div v-if="selectedIds.size > 0" class="tv-batch-actions">
        <template v-if="activeTab === 'pendingAssign'">
          <ElSelect v-model="batchInspectorId" placeholder="选择检查员" size="small" style="width: 200px" clearable>
            <ElOption v-for="p in candidates" :key="p.userId" :value="p.userId" :label="p.userName">
              <span>{{ p.userName }}</span>
              <span class="tv-workload" :class="`tv-workload--${workloadOf(p)}`">{{ workloadOf(p) }}</span>
            </ElOption>
          </ElSelect>
          <ElButton type="primary" size="small" :disabled="!batchInspectorId" @click="doBatchAssign">
            <Send class="w-3.5 h-3.5 mr-0.5" />批量指派
          </ElButton>
        </template>
        <ElButton v-if="activeTab === 'inProgress' || activeTab === 'overdue'" size="small" @click="batchUrge">
          批量催办
        </ElButton>
      </div>
    </div>

    <!-- 任务列表 -->
    <div v-if="currentTasks.length === 0" class="tv-empty">
      <span v-if="activeTab === 'pendingAssign'">所有任务都已分配</span>
      <span v-else-if="activeTab === 'pendingReview'">没有待审核的任务</span>
      <span v-else-if="activeTab === 'inProgress'">没有进行中的任务</span>
      <span v-else>没有逾期任务 🎉</span>
    </div>
    <div v-else class="tv-list">
      <div v-for="t in currentTasks" :key="t.taskId"
           class="tv-row"
           :class="{ 'tv-row--overdue': t.daysOverdue > 0 && activeTab !== 'overdue', 'tv-row--selected': isSelected(t.taskId) }">
        <ElCheckbox :model-value="isSelected(t.taskId)" @change="(v: any) => toggleOne(t.taskId, v)" />
        <div class="tv-row-main">
          <div class="tv-row-line1">
            <span class="tv-code">{{ t.taskCode }}</span>
            <span class="tv-date">{{ t.taskDate }}</span>
            <span v-if="t.totalTargets !== null && t.totalTargets > 0" class="tv-targets">{{ t.totalTargets }}个目标</span>
            <span v-if="t.daysOverdue > 0" class="tv-overdue">
              <AlertCircle class="w-3 h-3" />超时 {{ t.daysOverdue }} 天
            </span>
          </div>
          <div class="tv-row-line2">
            <span v-if="t.inspectorName">检查员: {{ t.inspectorName }}</span>
            <span v-if="t.reviewerName"> · 审核员: {{ t.reviewerName }}</span>
            <span v-if="t.submittedAt"> · 提交于 {{ t.submittedAt.substring(0, 16) }}</span>
          </div>
        </div>
        <div class="tv-row-actions">
          <!-- 待分配 -->
          <template v-if="activeTab === 'pendingAssign'">
            <ElSelect placeholder="指派给..." size="small" style="width: 160px"
                      @change="(v: any) => assignOne(t.taskId, v)">
              <ElOption v-for="p in candidates" :key="p.userId" :value="p.userId" :label="p.userName">
                <span>{{ p.userName }}</span>
                <span class="tv-workload" :class="`tv-workload--${workloadOf(p)}`">{{ workloadOf(p) }}</span>
              </ElOption>
            </ElSelect>
          </template>
          <!-- 待审核 -->
          <template v-else-if="activeTab === 'pendingReview'">
            <ElButton size="small" type="success" @click="approveOne(t.taskId)">
              <Check class="w-3.5 h-3.5 mr-0.5" />通过
            </ElButton>
            <ElButton size="small" type="danger" plain @click="rejectOne(t.taskId)">
              <X class="w-3.5 h-3.5 mr-0.5" />驳回
            </ElButton>
          </template>
          <!-- 进行中 / 逾期 -->
          <template v-else>
            <ElButton size="small" plain @click="withdrawOne(t.taskId)">撤回</ElButton>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tv-tabs {
  display: inline-flex;
  background: var(--insp-bg-surface, #fff);
  border: 1px solid var(--insp-border-default, #e5e7eb);
  border-radius: 8px;
  padding: 3px;
  gap: 2px;
  margin-bottom: 12px;
}
.tv-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: none;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  color: var(--insp-ink-secondary, #4b5563);
  cursor: pointer;
}
.tv-tab.active { background: var(--insp-bg-subtle, #f3f4f6); color: var(--insp-ink-primary, #111827); font-weight: 600; }
.tv-tab--danger.active { background: #fef2f2; color: #dc2626; }
.tv-tab-num {
  background: rgba(0,0,0,0.06);
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}
.tv-tab--danger .tv-tab-num { background: #fecaca; color: #dc2626; }

.tv-batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: var(--insp-bg-surface, #fff);
  border: 1px solid var(--insp-border-default, #e5e7eb);
  border-radius: 6px;
  padding: 8px 14px;
  margin-bottom: 8px;
}
.tv-batch-actions { display: flex; gap: 8px; align-items: center; }

.tv-list { display: flex; flex-direction: column; gap: 6px; }
.tv-row {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--insp-bg-surface, #fff);
  border: 1px solid var(--insp-border-default, #e5e7eb);
  border-radius: 6px;
  padding: 10px 14px;
}
.tv-row:hover { border-color: #c7d2fe; }
.tv-row--overdue { border-color: #fecaca; }
.tv-row--selected { background: #eff6ff; border-color: #93c5fd; }

.tv-row-main { flex: 1; min-width: 0; }
.tv-row-line1 {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}
.tv-row-line2 {
  font-size: 11.5px;
  color: var(--insp-ink-tertiary, #6b7280);
  margin-top: 3px;
}
.tv-code { font-weight: 600; color: var(--insp-ink-primary, #111827); font-family: 'SF Mono', monospace; }
.tv-date { color: var(--insp-ink-tertiary, #6b7280); }
.tv-targets { color: #2563eb; font-size: 11.5px; }
.tv-overdue {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: #dc2626;
  font-size: 11.5px;
  font-weight: 600;
}

.tv-row-actions { display: flex; gap: 6px; flex-shrink: 0; }

.tv-workload {
  margin-left: 8px;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 600;
}
.tv-workload--忙 { background: #fef3c7; color: #92400e; }
.tv-workload--中等 { background: #dbeafe; color: #1e40af; }
.tv-workload--空闲 { background: #dcfce7; color: #166534; }

.tv-empty {
  text-align: center;
  padding: 48px 16px;
  color: var(--insp-ink-tertiary, #6b7280);
  font-size: 13px;
  background: var(--insp-bg-surface, #fff);
  border: 1px dashed var(--insp-border-default, #e5e7eb);
  border-radius: 8px;
}
</style>
