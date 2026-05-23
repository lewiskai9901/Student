<script setup lang="ts">
/**
 * Phase D - 单人行卡片 (折叠态 + 展开后任务面板).
 *
 * 折叠态: 头像 / 姓名 / 角色徽章 / 本周进度 / 4 数字徽章 / 展开按钮
 * 展开态: 3 段任务列表 (进行中 / 待审核 / 逾期) + 待分配候选快捷指派
 */
import { ref, computed } from 'vue'
import { ElButton, ElTooltip, ElMessage, ElMessageBox } from 'element-plus'
import {
  ChevronDown, ChevronUp, Crown, ShieldCheck, User as UserIcon,
  Check, X, AlertCircle, Send, Trash2,
} from 'lucide-vue-next'
import type { LongId } from '@/types/common'
import type { PersonRow, WorkbenchTaskRow } from '@/api/inspection/project'
import { batchAssignTasks } from '@/api/inspection/project'
import { assignTask } from '@/api/inspection/task'
import { http } from '@/utils/request'

const props = defineProps<{
  projectId: LongId
  person: PersonRow
  pendingAssignTasks: WorkbenchTaskRow[]
  isDraft?: boolean
}>()

const emit = defineEmits<{ (e: 'reload'): void }>()

const hasPending = computed(() =>
  props.person.stats.overdue > 0 ||
  props.person.stats.pendingReview > 0 ||
  props.person.stats.inProgress > 0
)
const expanded = ref(hasPending.value)

// 头像首字母
const avatarChar = computed(() => (props.person.userName || '?').charAt(0).toUpperCase())

// 本周完成率
const weekProgress = computed(() => {
  const assigned = props.person.stats.weekAssigned
  const done = props.person.stats.weekCompleted
  if (assigned === 0) return { percent: 0, ratio: `0/0` }
  return {
    percent: Math.min(100, Math.round((done / assigned) * 100)),
    ratio: `${done}/${assigned}`,
  }
})

// 是否空闲 (4 数字徽章全 0)
const isIdle = computed(() => {
  const s = props.person.stats
  return s.overdue === 0 && s.pendingReview === 0 && s.inProgress === 0 && s.pendingAssignCandidates === 0
})

// 是否逾期高亮
const hasOverdue = computed(() => props.person.stats.overdue > 0)

// === 行动 ===
async function approveTask(taskId: LongId) {
  try {
    await http.post(`/inspection/tasks/${taskId}/review`, { comment: '通过' })
    ElMessage.success('已通过')
    emit('reload')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '审核失败')
  }
}

async function rejectTask(taskId: LongId) {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回理由', '驳回任务', {
      confirmButtonText: '确定驳回',
      cancelButtonText: '取消',
      inputValidator: (v: string) => !!v?.trim() || '请填写理由',
    })
    await http.post(`/inspection/tasks/${taskId}/reject`, { comment: value })
    ElMessage.success('已驳回')
    emit('reload')
  } catch (e: any) {
    if (e === 'cancel') return
    ElMessage.error(e?.response?.data?.message || '驳回失败')
  }
}

async function assignToSelf(task: WorkbenchTaskRow) {
  try {
    await assignTask(task.taskId, {
      inspectorId: props.person.userId,
      inspectorName: props.person.userName,
    })
    ElMessage.success(`已指派给 ${props.person.userName}`)
    emit('reload')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '指派失败')
  }
}

async function urgeTask(taskId: LongId) {
  // 催办 = 后端事件触发器 (假定有 /inspection/tasks/{id}/urge), v1 仅前端反馈
  ElMessage.success('已发送催办通知 (功能开发中)')
}

async function withdrawTask(taskId: LongId) {
  try {
    await ElMessageBox.confirm('确定撤回该任务的指派?', '撤回任务', {
      confirmButtonText: '撤回',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await http.post(`/inspection/tasks/${taskId}/withdraw`)
    ElMessage.success('已撤回')
    emit('reload')
  } catch (e: any) {
    if (e === 'cancel') return
    ElMessage.error(e?.response?.data?.message || '撤回失败')
  }
}

// 候选数 — 展示用
const candidateTasks = computed(() => {
  // INSPECTOR/LEAD 可作为候选, 其他角色不显示
  const canAssign = props.person.roles.includes('INSPECTOR') || props.person.roles.includes('LEAD')
  return canAssign ? props.pendingAssignTasks : []
})
</script>

<template>
  <div class="prc" :class="{ 'prc--overdue': hasOverdue, 'prc--expanded': expanded }">
    <!-- 折叠态 -->
    <div class="prc-summary" @click="expanded = !expanded">
      <div class="prc-avatar" :class="{ 'prc-avatar--lead': person.roles.includes('LEAD') }">
        {{ avatarChar }}
      </div>
      <div class="prc-meta">
        <div class="prc-name-line">
          <span class="prc-name">{{ person.userName }}</span>
          <!-- 角色徽章 -->
          <span v-if="person.roles.includes('LEAD')" class="prc-badge prc-badge--lead">
            <Crown class="w-3 h-3" />负责人
          </span>
          <span v-if="person.roles.includes('REVIEWER')" class="prc-badge prc-badge--reviewer">
            <ShieldCheck class="w-3 h-3" />审核员
          </span>
          <span v-if="person.roles.includes('INSPECTOR')" class="prc-badge prc-badge--inspector">
            <UserIcon class="w-3 h-3" />检查员
          </span>
          <span v-if="person.isCreator" class="prc-badge prc-badge--creator" title="项目创建者">创建者</span>
          <span v-if="person.orgUnitName" class="prc-org">· {{ person.orgUnitName }}</span>
        </div>

        <!-- 进度条 + 本周比 -->
        <div class="prc-progress" v-if="!isDraft && weekProgress.percent > 0 || person.stats.weekAssigned > 0">
          <div class="prc-progress-bar">
            <div class="prc-progress-fill" :style="{ width: weekProgress.percent + '%' }" />
          </div>
          <span class="prc-progress-text">本周 {{ weekProgress.ratio }} ({{ weekProgress.percent }}%)</span>
        </div>

        <!-- 4 数字徽章 -->
        <div class="prc-stats" v-if="!isDraft">
          <span v-if="person.stats.overdue > 0" class="prc-stat prc-stat--danger">
            <AlertCircle class="w-3 h-3" />逾期 <b>{{ person.stats.overdue }}</b>
          </span>
          <span v-if="person.stats.pendingReview > 0" class="prc-stat prc-stat--warn">
            待我审核 <b>{{ person.stats.pendingReview }}</b>
          </span>
          <span v-if="person.stats.inProgress > 0" class="prc-stat prc-stat--info">
            进行中 <b>{{ person.stats.inProgress }}</b>
          </span>
          <span v-if="person.stats.pendingAssignCandidates > 0 && (person.roles.includes('INSPECTOR') || person.roles.includes('LEAD'))"
                class="prc-stat prc-stat--neutral">
            待分配候选 <b>{{ person.stats.pendingAssignCandidates }}</b>
          </span>
          <span v-if="isIdle" class="prc-stat prc-stat--idle">空闲</span>
        </div>
      </div>

      <button class="prc-toggle" @click.stop="expanded = !expanded">
        <ChevronDown v-if="!expanded" class="w-4 h-4" />
        <ChevronUp v-else class="w-4 h-4" />
      </button>
    </div>

    <!-- 展开态 -->
    <div v-if="expanded" class="prc-expanded">
      <!-- 进行中 -->
      <div v-if="person.tasks.inProgress.length > 0" class="prc-section">
        <div class="prc-section-title prc-section-title--info">
          进行中 ({{ person.tasks.inProgress.length }})
        </div>
        <div v-for="t in person.tasks.inProgress" :key="t.taskId" class="prc-task-row">
          <div class="prc-task-info">
            <span class="prc-task-code">{{ t.taskCode }}</span>
            <span class="prc-task-date">{{ t.taskDate }}</span>
            <span v-if="t.completedTargets !== null && t.totalTargets !== null" class="prc-task-progress-num">
              {{ t.completedTargets }}/{{ t.totalTargets }}
            </span>
          </div>
          <div class="prc-task-actions">
            <ElButton size="small" plain @click="urgeTask(t.taskId)">催办</ElButton>
            <ElButton size="small" plain type="danger" @click="withdrawTask(t.taskId)">撤回</ElButton>
          </div>
        </div>
      </div>

      <!-- 待审核 -->
      <div v-if="person.tasks.pendingReview.length > 0" class="prc-section">
        <div class="prc-section-title prc-section-title--warn">
          待我审核 ({{ person.tasks.pendingReview.length }})
        </div>
        <div v-for="t in person.tasks.pendingReview" :key="t.taskId" class="prc-task-row">
          <div class="prc-task-info">
            <span class="prc-task-code">{{ t.taskCode }}</span>
            <span class="prc-task-meta">{{ t.inspectorName || '-' }} · {{ t.submittedAt?.substring(0, 16) || t.taskDate }}</span>
          </div>
          <div class="prc-task-actions">
            <ElButton size="small" type="success" @click="approveTask(t.taskId)">
              <Check class="w-3.5 h-3.5 mr-0.5" />通过
            </ElButton>
            <ElButton size="small" type="danger" plain @click="rejectTask(t.taskId)">
              <X class="w-3.5 h-3.5 mr-0.5" />驳回
            </ElButton>
          </div>
        </div>
      </div>

      <!-- 逾期 -->
      <div v-if="person.tasks.overdue.length > 0" class="prc-section prc-section--danger">
        <div class="prc-section-title prc-section-title--danger">
          已逾期 ({{ person.tasks.overdue.length }})
        </div>
        <div v-for="t in person.tasks.overdue" :key="t.taskId" class="prc-task-row">
          <div class="prc-task-info">
            <span class="prc-task-code">{{ t.taskCode }}</span>
            <span class="prc-task-date">{{ t.taskDate }}</span>
            <span class="prc-task-overdue">超时 {{ t.daysOverdue }} 天</span>
          </div>
          <div class="prc-task-actions">
            <ElButton size="small" plain @click="urgeTask(t.taskId)">催办</ElButton>
            <ElButton size="small" plain type="danger" @click="withdrawTask(t.taskId)">重派他人</ElButton>
          </div>
        </div>
      </div>

      <!-- 候选待分配 -->
      <div v-if="candidateTasks.length > 0" class="prc-section">
        <div class="prc-section-title">
          待分配候选 ({{ candidateTasks.length }})
        </div>
        <div v-for="t in candidateTasks.slice(0, 3)" :key="t.taskId" class="prc-task-row">
          <div class="prc-task-info">
            <span class="prc-task-code">{{ t.taskCode }}</span>
            <span class="prc-task-date">{{ t.taskDate }}</span>
          </div>
          <div class="prc-task-actions">
            <ElButton size="small" type="primary" plain @click="assignToSelf(t)">
              <Send class="w-3.5 h-3.5 mr-0.5" />指派给本人
            </ElButton>
          </div>
        </div>
        <div v-if="candidateTasks.length > 3" class="prc-more-hint">
          还有 {{ candidateTasks.length - 3 }} 个待分配, 切换到「按任务」视图查看全部
        </div>
      </div>

      <!-- 全空 -->
      <div v-if="person.tasks.inProgress.length === 0 && person.tasks.pendingReview.length === 0 && person.tasks.overdue.length === 0 && candidateTasks.length === 0"
           class="prc-empty">
        <span v-if="person.stats.totalAssigned === 0">尚未参与任何任务</span>
        <span v-else>暂无待办</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.prc {
  background: var(--insp-bg-surface, #fff);
  border: 1px solid var(--insp-border-default, #e5e7eb);
  border-radius: 8px;
  overflow: hidden;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.prc:hover { border-color: #c7d2fe; }
.prc--overdue { border-color: #fecaca; background: linear-gradient(180deg, #fff 0%, #fef2f2 100%); }
.prc--expanded { box-shadow: 0 1px 3px rgba(0,0,0,0.06); }

.prc-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
}
.prc-summary:hover { background: rgba(0,0,0,0.015); }

.prc-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #818cf8 0%, #6366f1 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}
.prc-avatar--lead {
  background: linear-gradient(135deg, #fbbf24 0%, #d97706 100%);
}

.prc-meta { flex: 1; min-width: 0; }
.prc-name-line {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.prc-name { font-weight: 600; font-size: 14px; color: var(--insp-ink-primary, #111827); }
.prc-org { font-size: 11px; color: var(--insp-ink-tertiary, #6b7280); }

.prc-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 10px;
  font-weight: 500;
}
.prc-badge--lead { background: #fef3c7; color: #92400e; }
.prc-badge--reviewer { background: #dbeafe; color: #1e40af; }
.prc-badge--inspector { background: #f3f4f6; color: #4b5563; }
.prc-badge--creator { background: #ede9fe; color: #6d28d9; }

.prc-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 5px;
}
.prc-progress-bar {
  flex: 1;
  max-width: 180px;
  height: 4px;
  background: #f3f4f6;
  border-radius: 2px;
  overflow: hidden;
}
.prc-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #22c55e 0%, #16a34a 100%);
  transition: width 0.3s;
}
.prc-progress-text { font-size: 11px; color: var(--insp-ink-tertiary, #6b7280); }

.prc-stats {
  display: flex;
  gap: 10px;
  margin-top: 6px;
  flex-wrap: wrap;
}
.prc-stat {
  font-size: 11.5px;
  color: var(--insp-ink-secondary, #4b5563);
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.prc-stat b { color: var(--insp-ink-primary, #111827); font-weight: 600; }
.prc-stat--danger { color: #dc2626; }
.prc-stat--danger b { color: #dc2626; }
.prc-stat--warn { color: #d97706; }
.prc-stat--warn b { color: #d97706; }
.prc-stat--info { color: #2563eb; }
.prc-stat--info b { color: #2563eb; }
.prc-stat--neutral { color: var(--insp-ink-tertiary, #6b7280); }
.prc-stat--idle { color: #16a34a; font-style: italic; }

.prc-toggle {
  background: none;
  border: none;
  color: var(--insp-ink-tertiary, #6b7280);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  flex-shrink: 0;
}
.prc-toggle:hover { background: rgba(0,0,0,0.04); color: var(--insp-ink-primary, #111827); }

.prc-expanded {
  border-top: 1px solid var(--insp-border-default, #e5e7eb);
  padding: 8px 16px 12px;
  background: rgba(0,0,0,0.012);
}

.prc-section { margin-top: 8px; }
.prc-section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--insp-ink-secondary, #4b5563);
  padding: 4px 0 6px;
  border-bottom: 1px dashed var(--insp-border-default, #e5e7eb);
  margin-bottom: 4px;
}
.prc-section-title--info { color: #2563eb; }
.prc-section-title--warn { color: #d97706; }
.prc-section-title--danger { color: #dc2626; }

.prc-task-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 7px 0;
  border-bottom: 1px solid rgba(0,0,0,0.04);
}
.prc-task-row:last-child { border-bottom: none; }
.prc-task-info {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12.5px;
  color: var(--insp-ink-secondary, #4b5563);
  min-width: 0;
  flex: 1;
}
.prc-task-code { font-weight: 600; color: var(--insp-ink-primary, #111827); font-family: 'SF Mono', monospace; }
.prc-task-date { color: var(--insp-ink-tertiary, #6b7280); font-size: 11.5px; }
.prc-task-meta { color: var(--insp-ink-tertiary, #6b7280); font-size: 11.5px; }
.prc-task-progress-num { color: #2563eb; font-size: 11.5px; }
.prc-task-overdue { color: #dc2626; font-weight: 600; font-size: 11.5px; }
.prc-task-actions { display: flex; gap: 6px; flex-shrink: 0; }

.prc-more-hint {
  font-size: 11.5px;
  color: var(--insp-ink-tertiary, #6b7280);
  text-align: center;
  padding: 6px 0 0;
  font-style: italic;
}
.prc-empty {
  text-align: center;
  padding: 16px 0;
  color: var(--insp-ink-tertiary, #6b7280);
  font-size: 12px;
}
</style>
