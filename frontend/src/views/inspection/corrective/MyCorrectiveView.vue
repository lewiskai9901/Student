<template>
  <div class="insp-shell my-corrective">
    <!-- 顶部 -->
    <header class="page-head">
      <div>
        <div class="insp-eyebrow">受检主体视角 / My Corrective</div>
        <h1 class="insp-display page-title">我的整改</h1>
        <div class="page-sub">{{ kpi.total }} 单 · 按时关闭率 {{ kpi.closeRate }}%</div>
      </div>
      <div class="filter-bar">
        <router-link to="/inspection/received" class="insp-btn insp-btn--ghost insp-btn--sm">
          受检中心 →
        </router-link>
        <el-button :icon="RefreshRight" circle size="small" @click="loadData" :loading="loading" />
      </div>
    </header>

    <hr class="insp-rule insp-rule--strong" />

    <!-- 状态切换 -->
    <nav class="filter-rail">
      <button v-for="t in tabs" :key="t.key" class="filter-tab"
        :class="{ 'is-active': activeTab === t.key }" @click="activeTab = t.key">
        <span class="filter-tab__label">{{ t.label }}</span>
        <span v-if="t.count > 0" class="filter-tab__count">{{ t.count }}</span>
      </button>
    </nav>

    <!-- 紧急区 -->
    <div v-if="activeTab === 'todo' && urgent.length" class="urgent-banner">
      <AlertTriangle :size="16" />
      <span>{{ urgent.length }} 单 24 小时内截止, 优先处理</span>
    </div>

    <!-- 列表 -->
    <div v-if="loading" class="state-area">
      <el-icon class="is-loading"><Loading /></el-icon> 加载中...
    </div>
    <InspErrorState v-else-if="loadError" :message="loadError" @retry="loadData" />
    <InspEmptyState v-else-if="filteredCases.length === 0"
      :title="activeTab === 'todo' ? '当前无待办整改' : '暂无记录'"
      :description="activeTab === 'todo' ? '状态优秀, 继续保持' : ''" />
    <div v-else class="case-list">
      <article v-for="c in filteredCases" :key="c.id" class="case" :class="urgencyClass(c)">
        <header class="case-head">
          <span class="case-deadline">{{ deadlineLabel(c) }}</span>
          <span class="insp-chip" :class="`insp-chip--${priorityVariant(c.priority)}`">{{ priorityLabel(c.priority) }}</span>
          <span class="insp-chip" :class="`insp-chip--${statusVariant(c.status)}`">{{ statusLabel(c.status) }}</span>
        </header>
        <div class="case-target">{{ c.targetName || '—' }}</div>
        <div class="case-issue">{{ c.issueDescription }}</div>
        <div class="case-meta">
          <span v-if="c.deadline" class="meta-item">{{ c.deadline.slice(0, 10) }}</span>
          <span class="meta-item">#{{ c.caseCode }}</span>
        </div>

        <!-- 行动按钮 (按状态分支) -->
        <div class="case-actions">
          <template v-if="c.status === 'OPEN' || c.status === 'ASSIGNED'">
            <button class="insp-btn insp-btn--accent insp-btn--sm" @click="onStart(c)">开始整改</button>
          </template>
          <template v-else-if="c.status === 'IN_PROGRESS' || c.status === 'REJECTED'">
            <button class="insp-btn insp-btn--accent insp-btn--sm" @click="openSubmit(c)">上传整改</button>
            <button class="insp-btn insp-btn--sm" @click="goDetail(c)">详情</button>
          </template>
          <template v-else>
            <button class="insp-btn insp-btn--sm" @click="goDetail(c)">查看</button>
          </template>
        </div>
      </article>
    </div>

    <!-- 底部成绩单 -->
    <div class="score-card" v-if="kpi.total > 0">
      <div class="score-row">
        <div class="score-cell">
          <div class="score-num">{{ kpi.closed }}</div>
          <div class="score-label">已关闭</div>
        </div>
        <div class="score-cell">
          <div class="score-num">{{ kpi.inProgress }}</div>
          <div class="score-label">进行中</div>
        </div>
        <div class="score-cell">
          <div class="score-num text-danger">{{ kpi.overdue }}</div>
          <div class="score-label">逾期</div>
        </div>
        <div class="score-cell">
          <div class="score-num text-warning">{{ kpi.escalated }}</div>
          <div class="score-label">升级</div>
        </div>
      </div>
    </div>

    <!-- 提交整改对话框 -->
    <el-dialog v-model="submitDialog" title="提交整改" width="480px" append-to-body>
      <div class="space-y-3" v-if="submitTarget">
        <div class="text-sm" style="color: var(--insp-ink-secondary)">{{ submitTarget.issueDescription }}</div>
        <el-input v-model="submitNote" type="textarea" :rows="3"
          placeholder="说明整改措施 (拍照可后续追加)" />
        <div class="text-xs" style="color: var(--insp-ink-tertiary)">提交后等待复核员验证, 通过后自动关闭</div>
      </div>
      <template #footer>
        <el-button @click="submitDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, Loading } from '@element-plus/icons-vue'
import { AlertTriangle } from 'lucide-vue-next'
import { http } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import InspEmptyState from '@/views/inspection/shared/InspEmptyState.vue'
import InspErrorState from '@/views/inspection/shared/InspErrorState.vue'

interface CorrectiveCase {
  id: LongId; caseCode: string; issueDescription: string
  priority: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'
  status: 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'SUBMITTED' | 'VERIFIED' | 'CLOSED' | 'REJECTED' | 'ESCALATED'
  deadline: string | null
  targetName: string
  assigneeId: LongId | null
  escalationLevel: number
}

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const loadError = ref('')
const cases = ref<CorrectiveCase[]>([])
const activeTab = ref<'todo' | 'pending' | 'closed'>('todo')

const submitDialog = ref(false)
const submitTarget = ref<CorrectiveCase | null>(null)
const submitNote = ref('')
const submitting = ref(false)

const tabs = computed(() => [
  { key: 'todo' as const, label: '待办', count: cases.value.filter(c =>
      ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'REJECTED'].includes(c.status)).length },
  { key: 'pending' as const, label: '待验证', count: cases.value.filter(c => c.status === 'SUBMITTED').length },
  { key: 'closed' as const, label: '已完成', count: cases.value.filter(c =>
      ['VERIFIED', 'CLOSED'].includes(c.status)).length },
])

const filteredCases = computed(() => {
  const set = activeTab.value === 'todo' ? ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'REJECTED']
    : activeTab.value === 'pending' ? ['SUBMITTED']
    : ['VERIFIED', 'CLOSED']
  return [...cases.value]
    .filter(c => set.includes(c.status))
    .sort((a, b) => {
      const da = a.deadline ? new Date(a.deadline).getTime() : Infinity
      const db = b.deadline ? new Date(b.deadline).getTime() : Infinity
      return da - db
    })
})

const urgent = computed(() => {
  const now = Date.now()
  return cases.value.filter(c => {
    if (!c.deadline) return false
    if (!['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'REJECTED'].includes(c.status)) return false
    const ms = new Date(c.deadline).getTime() - now
    return ms < 24 * 3600 * 1000
  })
})

const kpi = computed(() => {
  const total = cases.value.length
  const closed = cases.value.filter(c => ['VERIFIED', 'CLOSED'].includes(c.status)).length
  const inProgress = cases.value.filter(c => ['IN_PROGRESS', 'SUBMITTED'].includes(c.status)).length
  const overdue = cases.value.filter(c => {
    if (!c.deadline || ['VERIFIED', 'CLOSED'].includes(c.status)) return false
    return new Date(c.deadline).getTime() < Date.now()
  }).length
  const escalated = cases.value.filter(c => c.escalationLevel > 0).length
  const closeRate = total > 0 ? Math.round((closed / total) * 100) : 0
  return { total, closed, inProgress, overdue, escalated, closeRate }
})

async function loadData() {
  loading.value = true
  loadError.value = ''
  try {
    // my-cases 后端按 SecurityUtils 当前用户过滤; 为空即本人无整改单, 直接显示空状态.
    // 注: 不可 fallback 拉 /corrective-cases 全量 — 会把他人整改单当成"我的"展示 (数据泄露).
    const r = await http.get<CorrectiveCase[]>('/inspection/corrective-cases/my-cases')
    cases.value = (r as any) || []
  } catch (e: any) {
    loadError.value = e?.message || '未知错误'
    ElMessage.error('加载失败: ' + loadError.value)
    cases.value = []
  } finally {
    loading.value = false
  }
}

async function onStart(c: CorrectiveCase) {
  try {
    // OPEN 状态: 尚未分配责任人, 后端 start-work 会因状态机校验失败.
    // 先把当前用户设为 assignee (接单), 再 start-work.
    if (c.status === 'OPEN') {
      await ElMessageBox.confirm('该单尚未分配, 直接接单并开始整改?', '确认接单', { type: 'info' })
      const me = authStore.user?.userId
      if (!me) { ElMessage.error('无法获取当前用户身份, 请重新登录'); return }
      await http.post(`/inspection/corrective-cases/${c.id}/assign`, {
        assigneeId: me,
        assigneeName: authStore.userName || '我',
      })
    }
    await http.post(`/inspection/corrective-cases/${c.id}/start-work`)
    ElMessage.success('已开始整改')
    await loadData()
  } catch (e: any) {
    if (e === 'cancel') return
    ElMessage.error('操作失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  }
}

function openSubmit(c: CorrectiveCase) {
  submitTarget.value = c
  submitNote.value = ''
  submitDialog.value = true
}

async function confirmSubmit() {
  if (!submitTarget.value) return
  if (!submitNote.value.trim()) { ElMessage.warning('请填写整改措施'); return }
  submitting.value = true
  try {
    await http.post(`/inspection/corrective-cases/${submitTarget.value.id}/submit-correction`, {
      correctionNote: submitNote.value, evidenceIds: []
    })
    ElMessage.success('已提交, 等待验证')
    submitDialog.value = false
    await loadData()
  } catch (e: any) {
    ElMessage.error('提交失败: ' + (e?.message || '未知'))
  } finally {
    submitting.value = false
  }
}

function goDetail(c: CorrectiveCase) {
  router.push(`/inspection/corrective/${c.id}`)
}

function deadlineLabel(c: CorrectiveCase) {
  if (!c.deadline) return '无截止'
  const ms = new Date(c.deadline).getTime() - Date.now()
  const days = Math.ceil(ms / 86400000)
  const hours = Math.ceil(ms / 3600000)
  if (ms < 0) return `已超期 ${-days}d`
  if (hours <= 24) return `${hours}h 内`
  if (days <= 2) return `${days} 天内`
  if (days <= 7) return `${days} 天后`
  return new Date(c.deadline).toISOString().slice(5, 10)
}

function urgencyClass(c: CorrectiveCase) {
  if (!c.deadline || ['VERIFIED', 'CLOSED'].includes(c.status)) return ''
  const ms = new Date(c.deadline).getTime() - Date.now()
  if (ms < 0) return 'urgency-overdue'
  if (ms < 86400000) return 'urgency-critical'
  if (ms < 3 * 86400000) return 'urgency-soon'
  return ''
}

const PRIORITY: Record<string, { label: string; variant: string }> = {
  CRITICAL: { label: '紧急', variant: 'fail' },
  HIGH: { label: '高', variant: 'warn' },
  MEDIUM: { label: '中', variant: 'info' },
  LOW: { label: '低', variant: 'pass' },
}
const STATUS: Record<string, { label: string; variant: string }> = {
  OPEN: { label: '待分配', variant: 'pending' },
  ASSIGNED: { label: '已分配', variant: 'info' },
  IN_PROGRESS: { label: '整改中', variant: 'warn' },
  SUBMITTED: { label: '待验证', variant: 'info' },
  VERIFIED: { label: '已验证', variant: 'pass' },
  CLOSED: { label: '已关闭', variant: 'pass' },
  REJECTED: { label: '驳回', variant: 'fail' },
  ESCALATED: { label: '已升级', variant: 'fail' },
}
function priorityLabel(p: string) { return PRIORITY[p]?.label || p }
function priorityVariant(p: string) { return PRIORITY[p]?.variant || 'info' }
function statusLabel(s: string) { return STATUS[s]?.label || s }
function statusVariant(s: string) { return STATUS[s]?.variant || 'info' }

onMounted(loadData)
</script>

<style scoped>
.my-corrective {
  padding: 32px 48px 64px;
  max-width: 1500px;
  margin: 0 auto;
  min-height: 100vh;
  background: var(--insp-bg-page);
}
@media (max-width: 767px) {
  .my-corrective { padding: 20px 16px 64px; }
}

.page-head {
  display: flex; align-items: flex-end; justify-content: space-between;
  gap: var(--insp-sp-7); margin-bottom: var(--insp-sp-4);
}
.page-title { font-size: 44px; margin: 0; font-weight: 500; }
@media (max-width: 767px) { .page-title { font-size: 32px; } }
.page-sub { font-size: 13px; color: var(--insp-ink-tertiary); margin-top: 4px; }
.filter-bar { display: flex; align-items: center; gap: 10px; }

/* filter rail — 与同域 MyInspectionView 一致 */
.filter-rail {
  display: flex; align-items: center; gap: 0;
  margin: var(--insp-sp-5) 0;
  border-bottom: 1px solid var(--insp-border-subtle);
}
.filter-tab {
  position: relative;
  display: inline-flex; align-items: baseline; gap: var(--insp-sp-2);
  padding: 10px 18px 12px;
  border: 0; background: transparent; cursor: pointer;
  font-family: inherit; font-size: var(--insp-text-md); font-weight: 500;
  color: var(--insp-ink-tertiary);
  transition: color var(--insp-t-fast);
}
.filter-tab:hover { color: var(--insp-ink-primary); }
.filter-tab.is-active { color: var(--insp-ink-primary); }
.filter-tab.is-active::after {
  content: ''; position: absolute; left: 18px; right: 18px; bottom: -1px;
  height: 2px; background: var(--insp-accent);
}
.filter-tab__count {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs); font-weight: 500;
  color: var(--insp-ink-quaternary);
}
.filter-tab.is-active .filter-tab__count { color: var(--insp-accent); }

/* urgent banner */
.urgent-banner {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 16px; margin-bottom: 16px;
  background: var(--insp-warn-pale);
  border: 1px solid var(--insp-warn);
  border-radius: var(--insp-radius-md);
  color: var(--insp-warn);
  font-size: 13px; font-weight: 500;
}

/* case grid */
.case-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}
@media (max-width: 1199px) { .case-list { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 767px) { .case-list { grid-template-columns: 1fr; gap: 10px; } }

.case {
  background: var(--insp-bg-surface);
  border-radius: var(--insp-radius-md);
  padding: 14px 16px;
  border: 1px solid var(--insp-border-subtle);
  transition: all var(--insp-t-fast);
}
.case:hover { border-color: var(--insp-border-strong); box-shadow: 0 2px 12px rgba(0,0,0,0.05); }
.case.urgency-overdue { border-color: var(--insp-fail); background: var(--insp-fail-pale); }
.case.urgency-critical { border-color: var(--insp-warn); background: var(--insp-warn-pale); }
.case.urgency-soon { border-color: var(--insp-warn); }

.case-head { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }
.case-deadline { font-size: 12px; font-weight: 600; color: var(--insp-ink-secondary); flex: 1; }
.case.urgency-overdue .case-deadline,
.case.urgency-critical .case-deadline { color: var(--insp-fail); }
.case.urgency-soon .case-deadline { color: var(--insp-warn); }

.case-target { font-size: 13px; font-weight: 500; color: var(--insp-ink-primary); }
.case-issue { font-size: 14px; color: var(--insp-ink-secondary); line-height: 1.5; margin-top: 4px; }
.case-meta { display: flex; gap: 12px; margin-top: 6px; font-size: 11px; color: var(--insp-ink-quaternary); }
.meta-item { font-variant-numeric: tabular-nums; }

.case-actions {
  display: flex; gap: 8px; margin-top: 10px; padding-top: 8px;
  border-top: 1px dashed var(--insp-border-subtle);
}
.case-actions .insp-btn { flex: 1; }

/* states */
.state-area {
  text-align: center; padding: 60px 20px;
  color: var(--insp-ink-tertiary); font-size: 13px;
}

/* 底部成绩单 */
.score-card {
  background: var(--insp-bg-surface);
  padding: 16px 24px;
  margin-top: 24px;
  border-radius: var(--insp-radius-md);
  border: 1px solid var(--insp-border-subtle);
}
.score-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.score-cell { text-align: center; }
.score-num {
  font-size: 24px; font-weight: 700; color: var(--insp-ink-primary);
  line-height: 1.2; font-variant-numeric: tabular-nums;
}
.score-label { font-size: 12px; color: var(--insp-ink-tertiary); margin-top: 4px; }
.text-danger { color: var(--insp-fail); }
.text-warning { color: var(--insp-warn); }
</style>
