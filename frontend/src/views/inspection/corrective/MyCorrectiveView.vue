<template>
  <div class="mobile-shell">
    <div class="my-corrective">
      <!-- 顶部 KPI -->
      <header class="head">
        <div class="head-title">
          <h2>我的整改</h2>
          <div class="head-sub">{{ kpi.total }} 单 · 按时关闭率 {{ kpi.closeRate }}%</div>
        </div>
        <el-button :icon="RefreshRight" circle size="small" @click="loadData" :loading="loading" />
      </header>

      <!-- 状态切换 -->
      <div class="tabs">
        <button v-for="t in tabs" :key="t.key" class="tab" :class="{ 'is-active': activeTab === t.key }"
          @click="activeTab = t.key">
          {{ t.label }}
          <span v-if="t.count > 0" class="tab-count">{{ t.count }}</span>
        </button>
      </div>

      <!-- 紧急区 -->
      <div v-if="activeTab === 'todo' && urgent.length" class="urgent-banner">
        <span class="urgent-icon">⏰</span>
        <span class="urgent-text">{{ urgent.length }} 单 24 小时内截止, 优先处理</span>
      </div>

      <!-- 列表 -->
      <div v-if="loading" class="loading-area"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>
      <div v-else-if="filteredCases.length === 0" class="empty-area">
        <div class="empty-emoji">{{ activeTab === 'todo' ? '✨' : '📋' }}</div>
        <div>{{ activeTab === 'todo' ? '当前无待办整改, 状态优秀' : '暂无记录' }}</div>
      </div>
      <div v-else class="case-list">
        <article v-for="c in filteredCases" :key="c.id" class="case" :class="urgencyClass(c)">
          <header class="case-head">
            <span class="case-deadline">{{ deadlineLabel(c) }}</span>
            <el-tag size="small" :type="priorityType(c.priority)">{{ priorityLabel(c.priority) }}</el-tag>
            <el-tag size="small" :type="statusType(c.status)" effect="plain">{{ statusLabel(c.status) }}</el-tag>
          </header>
          <div class="case-target">{{ c.targetName || '—' }}</div>
          <div class="case-issue">{{ c.issueDescription }}</div>
          <div class="case-meta">
            <span v-if="c.deadline" class="meta-item">📅 {{ c.deadline.slice(0, 10) }}</span>
            <span class="meta-item">#{{ c.caseCode }}</span>
          </div>

          <!-- 行动按钮 (按状态分支) -->
          <div class="case-actions">
            <template v-if="c.status === 'OPEN' || c.status === 'ASSIGNED'">
              <el-button type="primary" size="small" @click="onStart(c)">▶ 开始整改</el-button>
            </template>
            <template v-else-if="c.status === 'IN_PROGRESS' || c.status === 'REJECTED'">
              <el-button type="success" size="small" @click="openSubmit(c)">📷 上传整改</el-button>
              <el-button size="small" @click="goDetail(c)">详情</el-button>
            </template>
            <template v-else>
              <el-button size="small" @click="goDetail(c)">查看</el-button>
            </template>
          </div>
        </article>
      </div>
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
    <el-dialog v-model="submitDialog" title="提交整改" width="92%" append-to-body>
      <div class="space-y-3" v-if="submitTarget">
        <div class="text-sm text-gray-700">{{ submitTarget.issueDescription }}</div>
        <el-input v-model="submitNote" type="textarea" :rows="3"
          placeholder="说明整改措施 (拍照可后续追加)" />
        <div class="text-xs text-gray-400">提交后等待复核员验证, 通过后自动关闭</div>
      </div>
      <template #footer>
        <el-button @click="submitDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, Loading } from '@element-plus/icons-vue'
import { http } from '@/utils/request'

interface CorrectiveCase {
  id: number; caseCode: string; issueDescription: string
  priority: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'
  status: 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'SUBMITTED' | 'VERIFIED' | 'CLOSED' | 'REJECTED' | 'ESCALATED'
  deadline: string | null
  targetName: string
  assigneeId: number | null
  escalationLevel: number
}

const router = useRouter()
const loading = ref(false)
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
      // 紧急在前 (按 deadline 升序, 无 deadline 在最后)
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
  try {
    // 优先尝试 my-cases (后端按 SecurityUtils 当前用户过滤)
    const r = await http.get<CorrectiveCase[]>('/inspection/corrective-cases/my-cases')
    let list = (r as any) || []
    // admin 测试场景: my-cases 可能为空, 退化拉前 50 条
    if (list.length === 0) {
      const all = await http.get<CorrectiveCase[]>('/inspection/corrective-cases')
      list = ((all as any) || []).slice(0, 30)
    }
    cases.value = list
  } catch (e: any) {
    ElMessage.error('加载失败: ' + (e?.message || '未知'))
    cases.value = []
  } finally {
    loading.value = false
  }
}

async function onStart(c: CorrectiveCase) {
  try {
    if (c.status === 'OPEN') {
      await ElMessageBox.confirm('该单尚未分配, 直接接单并开始整改?', '确认接单', { type: 'info' })
      // 简化: OPEN 状态直接 start-work (后端可能要求先 assign, 此处依赖后端策略)
    }
    await http.post(`/inspection/corrective-cases/${c.id}/start-work`)
    ElMessage.success('已开始整改')
    await loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.warning(e?.message || '操作失败')
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
  if (ms < 0) return `⚠ 已超期 ${-days}d`
  if (hours <= 24) return `⏰ ${hours}h 内`
  if (days <= 2) return `⏰ ${days} 天内`
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

const PRIORITY: Record<string, { label: string; type: 'danger' | 'warning' | 'info' | 'success' }> = {
  CRITICAL: { label: '紧急', type: 'danger' },
  HIGH: { label: '高', type: 'warning' },
  MEDIUM: { label: '中', type: 'info' },
  LOW: { label: '低', type: 'success' },
}
const STATUS: Record<string, { label: string; type: 'danger' | 'warning' | 'info' | 'success' | 'primary' }> = {
  OPEN: { label: '待分配', type: 'info' },
  ASSIGNED: { label: '已分配', type: 'info' },
  IN_PROGRESS: { label: '整改中', type: 'warning' },
  SUBMITTED: { label: '待验证', type: 'primary' },
  VERIFIED: { label: '已验证', type: 'success' },
  CLOSED: { label: '已关闭', type: 'success' },
  REJECTED: { label: '驳回', type: 'danger' },
  ESCALATED: { label: '已升级', type: 'danger' },
}
function priorityLabel(p: string) { return PRIORITY[p]?.label || p }
function priorityType(p: string) { return PRIORITY[p]?.type || 'info' }
function statusLabel(s: string) { return STATUS[s]?.label || s }
function statusType(s: string) { return (STATUS[s]?.type || 'info') as any }

onMounted(loadData)
</script>

<style scoped>
.mobile-shell {
  max-width: 480px; margin: 0 auto; min-height: 100vh;
  background: #f9fafb; padding-bottom: 100px;
  box-shadow: 0 0 32px rgba(0,0,0,0.06); position: relative;
}
@media (max-width: 480px) {
  .mobile-shell { box-shadow: none; max-width: 100%; }
}

.my-corrective { padding: 16px; }

.head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.head-title h2 { font-size: 22px; margin: 0; font-weight: 600; }
.head-sub { font-size: 12px; color: #6b7280; margin-top: 2px; }

/* tabs */
.tabs { display: flex; gap: 6px; background: #fff; border-radius: 10px; padding: 4px; margin-bottom: 12px;
  border: 1px solid #e5e7eb; }
.tab { flex: 1; padding: 8px 0; background: none; border: none; font-size: 13px; color: #4b5563;
  border-radius: 8px; cursor: pointer; transition: 0.2s; display: flex; align-items: center; justify-content: center; gap: 4px; }
.tab.is-active { background: #2563eb; color: white; font-weight: 500; }
.tab-count { background: rgba(255,255,255,0.25); padding: 1px 6px; border-radius: 10px; font-size: 11px; min-width: 18px; }
.tab:not(.is-active) .tab-count { background: #fee2e2; color: #b91c1c; }

/* urgent banner */
.urgent-banner {
  display: flex; align-items: center; gap: 8px; padding: 10px 12px; margin-bottom: 12px;
  background: linear-gradient(90deg, #fff7ed, #ffedd5); border: 1px solid #fdba74;
  border-radius: 8px; color: #9a3412; font-size: 13px; font-weight: 500;
}
.urgent-icon { font-size: 18px; }

/* case card */
.case-list { display: flex; flex-direction: column; gap: 10px; }
.case {
  background: #fff; border-radius: 10px; padding: 12px;
  border: 1px solid #e5e7eb; transition: all 0.2s;
}
.case.urgency-overdue { border-color: #ef4444; background: #fef2f2; }
.case.urgency-critical { border-color: #f97316; background: #fff7ed; }
.case.urgency-soon { border-color: #f59e0b; }

.case-head { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }
.case-deadline { font-size: 12px; font-weight: 600; color: #475569; flex: 1; }
.case.urgency-overdue .case-deadline { color: #b91c1c; }
.case.urgency-critical .case-deadline { color: #c2410c; }
.case.urgency-soon .case-deadline { color: #b45309; }

.case-target { font-size: 13px; font-weight: 500; color: #1e293b; }
.case-issue { font-size: 14px; color: #374151; line-height: 1.5; margin-top: 4px; }
.case-meta { display: flex; gap: 12px; margin-top: 6px; font-size: 11px; color: #9ca3af; }
.meta-item { font-variant-numeric: tabular-nums; }

.case-actions { display: flex; gap: 8px; margin-top: 10px; padding-top: 8px; border-top: 1px dashed #e5e7eb; }
.case-actions .el-button { flex: 1; }

/* states */
.loading-area, .empty-area {
  text-align: center; padding: 60px 20px; color: #94a3b8; font-size: 13px;
}
.empty-emoji { font-size: 36px; margin-bottom: 8px; }

/* 底部成绩单 */
.score-card {
  position: sticky; bottom: 0; background: white;
  border-top: 1px solid #e5e7eb; padding: 12px 16px;
  margin-top: 16px;
}
.score-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 4px; }
.score-cell { text-align: center; }
.score-num { font-size: 18px; font-weight: 700; color: #1e293b; line-height: 1.2; font-variant-numeric: tabular-nums; }
.score-label { font-size: 11px; color: #6b7280; margin-top: 2px; }
.text-danger { color: #ef4444; }
.text-warning { color: #f59e0b; }
</style>
