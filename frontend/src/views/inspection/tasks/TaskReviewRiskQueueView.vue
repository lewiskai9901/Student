<template>
  <div class="risk-queue insp-shell">
    <header class="page-head">
      <div>
        <div class="insp-eyebrow">审核员视角 / Risk-Sorted Review Queue</div>
        <h1 class="insp-display page-title">待审风险池</h1>
      </div>
      <div class="filter-bar">
        <span class="insp-caps">排序</span>
        <el-radio-group v-model="sortMode" size="small">
          <el-radio-button value="risk">风险分</el-radio-button>
          <el-radio-button value="time">提交时间</el-radio-button>
          <el-radio-button value="score">分数</el-radio-button>
        </el-radio-group>
        <el-button :icon="RefreshRight" size="small" @click="loadAll" :loading="loading">刷新</el-button>
      </div>
    </header>

    <hr class="insp-rule insp-rule--strong" />

    <!-- 风险维度 KPI -->
    <section class="kpi-strip">
      <div class="kpi-cell" :class="{active: kpiFilter==='all'}" @click="kpiFilter='all'">
        <div class="kpi-label">全部待审</div>
        <div class="kpi-num">{{ allTasks.length }}</div>
      </div>
      <div class="kpi-rule" />
      <div class="kpi-cell clickable" :class="{active: kpiFilter==='overdue'}" @click="kpiFilter='overdue'">
        <div class="kpi-label">🕐 临期未审</div>
        <div class="kpi-num text-danger">{{ kpiCount.overdue }}</div>
        <div class="kpi-sub">提交超 24h</div>
      </div>
      <div class="kpi-rule" />
      <div class="kpi-cell clickable" :class="{active: kpiFilter==='high-spike'}" @click="kpiFilter='high-spike'">
        <div class="kpi-label">⚠ 高分突变</div>
        <div class="kpi-num text-warning">{{ kpiCount.highSpike }}</div>
        <div class="kpi-sub">>均值 +15</div>
      </div>
      <div class="kpi-rule" />
      <div class="kpi-cell clickable" :class="{active: kpiFilter==='low-spike'}" @click="kpiFilter='low-spike'">
        <div class="kpi-label">⚠ 低分突变</div>
        <div class="kpi-num text-warning">{{ kpiCount.lowSpike }}</div>
        <div class="kpi-sub">&lt;均值 -15</div>
      </div>
      <div class="kpi-rule" />
      <div class="kpi-cell clickable" :class="{active: kpiFilter==='inconsistent'}" @click="kpiFilter='inconsistent'">
        <div class="kpi-label">📍 检查员差异</div>
        <div class="kpi-num text-warning">{{ kpiCount.inconsistent }}</div>
        <div class="kpi-sub">同目标差 >20</div>
      </div>
      <div class="kpi-rule" />
      <div class="kpi-cell clickable" :class="{active: kpiFilter==='late'}" @click="kpiFilter='late'">
        <div class="kpi-label">⏰ 延迟交付</div>
        <div class="kpi-num text-warning">{{ kpiCount.late }}</div>
        <div class="kpi-sub">超 deadline 提交</div>
      </div>
    </section>

    <!-- 列表 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-head">
          <span>{{ filterLabel }} ({{ filteredAndSorted.length }})</span>
          <span class="hint">点击行进入审核, 风险分越高越优先</span>
        </div>
      </template>
      <div v-if="loading" class="text-center py-12 text-gray-400">
        <el-icon class="is-loading"><Loading /></el-icon> 计算风险分...
      </div>
      <div v-else-if="!filteredAndSorted.length" class="text-center py-12 text-gray-400">
        <div class="text-4xl mb-2">✨</div>
        当前无符合条件的待审任务
      </div>
      <div v-else class="task-list">
        <div v-for="t in filteredAndSorted" :key="t.id" class="task-row" :class="riskClass(t.riskScore)" @click="goReview(t.id)">
          <!-- 风险分胶囊 -->
          <div class="risk-pill" :class="riskClass(t.riskScore)">
            <div class="risk-num">{{ t.riskScore.toFixed(0) }}</div>
            <div class="risk-lbl">风险</div>
          </div>

          <!-- 主信息 -->
          <div class="task-main">
            <div class="task-head">
              <span class="task-code">#{{ t.taskCode }}</span>
              <el-tag size="small" :type="statusType(t.status)">{{ statusLabel(t.status) }}</el-tag>
              <span v-if="t.lateSubmission" class="badge late">⏰ 延迟</span>
              <span v-if="t.flags.overdue" class="badge overdue">🕐 临期</span>
              <span v-if="t.flags.highSpike" class="badge spike-up">⚠ 高分突变</span>
              <span v-if="t.flags.lowSpike" class="badge spike-down">⚠ 低分突变</span>
              <span v-if="t.flags.inconsistent" class="badge incon">📍 差异</span>
            </div>
            <div class="task-meta">
              <span>📅 {{ t.taskDate }}</span>
              <span v-if="t.inspectorName">👤 {{ t.inspectorName }}</span>
              <span v-if="t.submittedAt">提交于 {{ formatRelative(t.submittedAt) }}</span>
            </div>
          </div>

          <!-- 得分 -->
          <div class="task-score">
            <div class="score-num insp-num" :class="scoreColor(t.avgScore)">
              {{ t.avgScore != null ? t.avgScore.toFixed(0) : '—' }}
            </div>
            <div class="score-lbl">{{ t.subCount }} 提交</div>
          </div>

          <!-- 跳转 -->
          <el-icon class="task-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, RefreshRight, Loading } from '@element-plus/icons-vue'
import { getTasks } from '@/api/inspection/task'
import { getSubmissions } from '@/api/inspection/submission'
import type { InspTask } from '@/types/insp/project'

const router = useRouter()

interface RiskTask extends InspTask {
  avgScore: number | null
  subCount: number
  riskScore: number
  flags: {
    overdue: boolean
    highSpike: boolean
    lowSpike: boolean
    inconsistent: boolean
  }
}

const loading = ref(false)
const allTasks = ref<RiskTask[]>([])
const sortMode = ref<'risk' | 'time' | 'score'>('risk')
const kpiFilter = ref<'all' | 'overdue' | 'high-spike' | 'low-spike' | 'inconsistent' | 'late'>('all')

const filterLabel = computed(() => ({
  all: '待审任务',
  overdue: '临期未审',
  'high-spike': '高分突变',
  'low-spike': '低分突变',
  inconsistent: '检查员差异',
  late: '延迟交付',
}[kpiFilter.value]))

const kpiCount = computed(() => ({
  overdue: allTasks.value.filter(t => t.flags.overdue).length,
  highSpike: allTasks.value.filter(t => t.flags.highSpike).length,
  lowSpike: allTasks.value.filter(t => t.flags.lowSpike).length,
  inconsistent: allTasks.value.filter(t => t.flags.inconsistent).length,
  late: allTasks.value.filter(t => t.lateSubmission).length,
}))

const filteredAndSorted = computed(() => {
  let arr = [...allTasks.value]
  switch (kpiFilter.value) {
    case 'overdue': arr = arr.filter(t => t.flags.overdue); break
    case 'high-spike': arr = arr.filter(t => t.flags.highSpike); break
    case 'low-spike': arr = arr.filter(t => t.flags.lowSpike); break
    case 'inconsistent': arr = arr.filter(t => t.flags.inconsistent); break
    case 'late': arr = arr.filter(t => t.lateSubmission); break
  }
  switch (sortMode.value) {
    case 'risk': return arr.sort((a, b) => b.riskScore - a.riskScore)
    case 'time': return arr.sort((a, b) => (a.submittedAt || '').localeCompare(b.submittedAt || ''))
    case 'score': return arr.sort((a, b) => Number(a.avgScore ?? 999) - Number(b.avgScore ?? 999))
  }
  return arr
})

async function loadAll() {
  loading.value = true
  try {
    const tasksRaw = await getTasks() as InspTask[]
    const pending = tasksRaw.filter(t => t.status === 'SUBMITTED' || t.status === 'UNDER_REVIEW')
    if (!pending.length) { allTasks.value = []; return }

    // 拉每个任务的 submissions
    const subsByTask = new Map<number, any[]>()
    await Promise.all(pending.map(async (t) => {
      try {
        const subs = await getSubmissions({ taskId: t.id })
        subsByTask.set(t.id, subs || [])
      } catch { subsByTask.set(t.id, []) }
    }))

    // 全局基准: 所有提交的 finalScore 均值
    const allScores: number[] = []
    for (const subs of subsByTask.values()) {
      for (const s of subs) {
        if (s.finalScore != null) allScores.push(Number(s.finalScore))
      }
    }
    const globalAvg = allScores.length ? allScores.reduce((a, b) => a + b, 0) / allScores.length : 80

    // 同 (orgUnit, day) 跨检查员差异: 收集同 targetId 不同 inspector 的分差
    // 简化: 对每个 task 的 target avgScore 与该 target 全局 avgScore 比较
    const targetAvg = new Map<number, number[]>()
    for (const subs of subsByTask.values()) {
      for (const s of subs) {
        if (s.finalScore == null || !s.targetId) continue
        const id = Number(s.targetId)
        if (!targetAvg.has(id)) targetAvg.set(id, [])
        targetAvg.get(id)!.push(Number(s.finalScore))
      }
    }

    const now = Date.now()
    const enriched: RiskTask[] = pending.map(t => {
      const subs = subsByTask.get(t.id) || []
      const subCount = subs.length
      const scores = subs.filter(s => s.finalScore != null).map(s => Number(s.finalScore))
      const avgScore = scores.length ? scores.reduce((a, b) => a + b, 0) / scores.length : null

      // overdue: 提交超 24h 未审
      const submittedAt = (t as any).submittedAt
      const overdue = submittedAt
        ? (now - new Date(submittedAt).getTime()) > 24 * 3600 * 1000
        : false

      // 高分/低分突变: 任务均值偏离全局均值 ±15
      const highSpike = avgScore != null && avgScore > globalAvg + 15
      const lowSpike = avgScore != null && avgScore < globalAvg - 15

      // 检查员差异: 该 task 内任一 target 的得分,与同 target 全局均值差 >20
      let inconsistent = false
      for (const s of subs) {
        if (s.finalScore == null || !s.targetId) continue
        const arr = targetAvg.get(Number(s.targetId)) || []
        if (arr.length < 2) continue
        const otherAvg = (arr.reduce((a, b) => a + b, 0) - Number(s.finalScore)) / (arr.length - 1)
        if (Math.abs(Number(s.finalScore) - otherAvg) > 20) { inconsistent = true; break }
      }

      // 综合风险分 (0-100)
      let risk = 0
      if (overdue) risk += 35
      if (highSpike) risk += 20
      if (lowSpike) risk += 25
      if (inconsistent) risk += 30
      if ((t as any).lateSubmission) risk += 15
      // 提交时间越久风险越高 (按小时折算, 最多 +20)
      if (submittedAt) {
        const hours = (now - new Date(submittedAt).getTime()) / 3600000
        risk += Math.min(20, hours / 6)
      }

      return {
        ...t,
        avgScore,
        subCount,
        riskScore: Math.min(100, risk),
        flags: { overdue, highSpike, lowSpike, inconsistent },
      } as RiskTask
    })

    allTasks.value = enriched
  } catch (e: any) {
    ElMessage.error('加载失败: ' + (e?.message || '未知'))
    allTasks.value = []
  } finally {
    loading.value = false
  }
}

function goReview(taskId: number) {
  router.push(`/inspection/tasks/review?taskId=${taskId}`)
}

function riskClass(score: number) {
  if (score >= 60) return 'risk-high'
  if (score >= 30) return 'risk-mid'
  if (score >= 10) return 'risk-low'
  return 'risk-none'
}
function scoreColor(s: number | null) {
  if (s == null) return ''
  if (s >= 90) return 'text-success'
  if (s >= 75) return 'text-primary'
  if (s >= 60) return 'text-warning'
  return 'text-danger'
}
function statusLabel(s: string) {
  return ({ SUBMITTED: '已提交', UNDER_REVIEW: '审核中' } as any)[s] || s
}
function statusType(s: string): any {
  return ({ SUBMITTED: 'primary', UNDER_REVIEW: 'warning' } as any)[s] || 'info'
}
function formatRelative(iso: string) {
  const ms = Date.now() - new Date(iso).getTime()
  if (ms < 0) return iso.slice(0, 16)
  const h = Math.round(ms / 3600000)
  if (h < 1) return '刚刚'
  if (h < 24) return `${h} 小时前`
  return `${Math.round(h / 24)} 天前`
}

onMounted(loadAll)
</script>

<style scoped>
.risk-queue { padding: 32px 48px 64px; max-width: 1500px; margin: 0 auto; min-height: 100vh; background: var(--insp-bg-page); }

.page-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; gap: 24px; }
.page-title { font-size: 44px; margin: 0; font-weight: 500; }
.filter-bar { display: flex; align-items: center; gap: 12px; }
.filter-bar .insp-caps { color: var(--insp-ink-tertiary); }

/* KPI 顶条 */
.kpi-strip {
  display: flex; padding: 16px 8px; margin: 24px 0;
  background: var(--insp-bg-surface); border: 1px solid var(--insp-border-subtle);
  border-radius: var(--insp-radius-md);
}
.kpi-cell { flex: 1; padding: 0 var(--insp-sp-4); cursor: default; transition: 0.2s; }
.kpi-cell.clickable { cursor: pointer; border-radius: 6px; }
.kpi-cell.clickable:hover { background: rgba(59, 130, 246, 0.05); }
.kpi-cell.active { background: rgba(59, 130, 246, 0.1); box-shadow: inset 0 0 0 1px #3b82f6; }
.kpi-label { font-size: 11px; color: var(--insp-ink-tertiary); text-transform: uppercase; letter-spacing: 0.05em; }
.kpi-num { font-size: 28px; font-weight: 700; line-height: 1.1; margin: 4px 0; font-variant-numeric: tabular-nums; }
.kpi-sub { font-size: 11px; color: #94a3b8; }
.kpi-rule { width: 1px; background: var(--insp-border-subtle); margin: 4px 0; }

.card-head { display: flex; justify-content: space-between; align-items: center; }
.card-head .hint { font-size: 11px; color: #94a3b8; font-weight: normal; }

/* 任务列表 */
.task-list { display: flex; flex-direction: column; gap: 6px; }
.task-row {
  display: grid; grid-template-columns: 80px 1fr 100px 24px;
  gap: 16px; align-items: center;
  padding: 12px; border-radius: 8px; cursor: pointer;
  border: 1px solid #e5e7eb; background: #fff; transition: all 0.15s;
}
.task-row:hover { border-color: #3b82f6; background: #f8fafc; transform: translateX(2px); }
.task-row.risk-high { border-left: 4px solid #ef4444; }
.task-row.risk-mid { border-left: 4px solid #f59e0b; }
.task-row.risk-low { border-left: 4px solid #3b82f6; }

.risk-pill {
  text-align: center; padding: 8px; border-radius: 8px;
  background: #f3f4f6; color: #475569;
}
.risk-pill.risk-high { background: #fef2f2; color: #b91c1c; }
.risk-pill.risk-mid { background: #fffbeb; color: #b45309; }
.risk-pill.risk-low { background: #eff6ff; color: #1d4ed8; }
.risk-num { font-size: 24px; font-weight: 700; line-height: 1; font-variant-numeric: tabular-nums; }
.risk-lbl { font-size: 10px; margin-top: 2px; opacity: 0.7; }

.task-main { min-width: 0; }
.task-head { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin-bottom: 4px; }
.task-code { font-size: 13px; font-weight: 600; color: #1e293b; font-variant-numeric: tabular-nums; }
.task-meta { font-size: 12px; color: #6b7280; display: flex; gap: 12px; flex-wrap: wrap; }
.task-meta span { white-space: nowrap; }

.badge {
  font-size: 10px; padding: 2px 6px; border-radius: 10px; font-weight: 500;
}
.badge.late { background: #fef3c7; color: #92400e; }
.badge.overdue { background: #fee2e2; color: #991b1b; }
.badge.spike-up { background: #fed7aa; color: #9a3412; }
.badge.spike-down { background: #fecaca; color: #b91c1c; }
.badge.incon { background: #ddd6fe; color: #6d28d9; }

.task-score { text-align: right; }
.score-num { font-size: 22px; font-weight: 700; line-height: 1; font-variant-numeric: tabular-nums; }
.score-lbl { font-size: 11px; color: #94a3b8; margin-top: 2px; }

.task-arrow { color: #cbd5e1; transition: transform 0.15s; }
.task-row:hover .task-arrow { color: #3b82f6; transform: translateX(2px); }

.text-success { color: #22c55e; }
.text-primary { color: #3b82f6; }
.text-warning { color: #f59e0b; }
.text-danger { color: #ef4444; }
</style>
