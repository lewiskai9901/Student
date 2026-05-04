<template>
  <div class="my-widget" :class="resultClass" v-loading="loading">
    <div class="widget-head">
      <span class="widget-title">📋 我的检查记录</span>
      <button class="widget-link" @click="goRecord">查看完整 →</button>
    </div>

    <div v-if="!hasData" class="empty">暂无受检数据</div>
    <div v-else class="widget-body">
      <div class="score-block">
        <div class="score-num">{{ summary?.avgScore?.toFixed(1) ?? '—' }}</div>
        <div class="score-grade">{{ summary?.grade || '' }}</div>
      </div>
      <div class="meta-block">
        <div class="meta-line">
          <span class="meta-name">{{ summary?.targetName || '—' }}</span>
          <span class="meta-rank">#{{ summary?.ranking || '—' }}/{{ totalUnits }}</span>
        </div>
        <div class="meta-line meta-sub">
          <span :class="trendClass">趋势 {{ trendArrow }} {{ trendDelta }}</span>
          <span class="dot">·</span>
          <span>{{ summary?.summaryDate }}</span>
        </div>
        <div class="meta-line">
          <span class="redline-label">距红线</span>
          <div class="redline-bar"><div class="redline-fill" :style="{width: redlinePct+'%'}"></div></div>
          <span class="redline-text" :class="redlineColor">还剩 {{ 100 - totalDeductions }} 分</span>
        </div>
      </div>
      <div class="action-block">
        <div class="action-num" :class="caseCount > 0 ? 'text-warning' : 'text-success'">
          {{ caseCount }}
        </div>
        <div class="action-lbl">待整改</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '@/utils/request'
import * as analyticsApi from '@/api/inspection/analytics'
import { useInspExecutionStore } from '@/stores/inspection/inspExecutionStore'

const router = useRouter()
const executionStore = useInspExecutionStore()

const loading = ref(false)
const summary = ref<any>(null)
const totalUnits = ref(0)
const trendData = ref<any[]>([])
const caseCount = ref(0)

const hasData = computed(() => summary.value != null)

const trendDelta = computed(() => {
  if (trendData.value.length < 2) return ''
  const latest = Number(trendData.value.at(-1)?.avgScore ?? 0)
  const prev = Number(trendData.value.at(-2)?.avgScore ?? 0)
  const d = latest - prev
  return (d > 0 ? '+' : '') + d.toFixed(1)
})
const trendArrow = computed(() => {
  const d = parseFloat(trendDelta.value)
  return d > 0 ? '↗' : d < 0 ? '↘' : '→'
})
const trendClass = computed(() => {
  const d = parseFloat(trendDelta.value)
  return d > 0 ? 'text-success' : d < 0 ? 'text-danger' : 'text-gray-500'
})

const totalDeductions = computed(() => {
  return Math.abs(Math.round(trendData.value.reduce((s, t) => s + Math.abs(Number(t.totalDeductions) || 0), 0)))
})
const redlinePct = computed(() => Math.min(100, Math.round(totalDeductions.value)))
const redlineColor = computed(() => {
  if (redlinePct.value > 80) return 'text-danger'
  if (redlinePct.value > 50) return 'text-warning'
  return 'text-success'
})

const resultClass = computed(() => {
  const s = summary.value?.avgScore ?? 0
  if (s >= 90) return 'state-excellent'
  if (s >= 75) return 'state-good'
  if (s >= 60) return 'state-warn'
  if (s > 0) return 'state-fail'
  return ''
})

async function loadData() {
  loading.value = true
  try {
    await executionStore.loadProjects()
    const projects = executionStore.projects || []
    if (!projects.length) return
    const projectId = projects[0].id

    // 找最近 30 天有 daily summary 的日期
    let latestRanking: any[] = []
    for (let i = 0; i < 30; i++) {
      const d = new Date(); d.setDate(d.getDate() - i)
      const ds = d.toISOString().slice(0, 10)
      try {
        const rows = await analyticsApi.getDailyRanking(projectId, ds) as any[]
        if (rows.length) { latestRanking = rows; break }
      } catch { /* skip */ }
    }
    if (!latestRanking.length) return

    totalUnits.value = latestRanking.length
    // demo: 选第一个目标作为"我管的"
    summary.value = latestRanking[0]

    // 拉趋势 (近 30 天)
    const end = new Date()
    const start = new Date(); start.setDate(end.getDate() - 30)
    try {
      const trend = await analyticsApi.getTrend(
        projectId, summary.value.targetType, summary.value.targetId,
        start.toISOString().slice(0, 10), end.toISOString().slice(0, 10)
      ) as any[]
      trendData.value = trend.sort((a, b) => a.summaryDate.localeCompare(b.summaryDate))
    } catch { trendData.value = [] }

    // 拉待整改数 (按 targetId)
    try {
      const r = await http.get<any[]>('/inspection/corrective-cases', { params: { projectId } })
      const all = ((r as any) || []) as any[]
      caseCount.value = all.filter(c =>
        String(c.targetId) === String(summary.value.targetId) &&
        ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'REJECTED'].includes(c.status)
      ).length
    } catch { caseCount.value = 0 }
  } finally {
    loading.value = false
  }
}

function goRecord() { router.push('/inspection/my-record') }

onMounted(loadData)
</script>

<style scoped>
.my-widget {
  border: 1px solid #e5e7eb; border-radius: 10px;
  padding: 14px 16px; background: #fff;
  border-left-width: 4px; transition: border-color 0.2s;
}
.my-widget.state-excellent { border-left-color: #22c55e; }
.my-widget.state-good { border-left-color: #3b82f6; }
.my-widget.state-warn { border-left-color: #f59e0b; }
.my-widget.state-fail { border-left-color: #ef4444; }

.widget-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.widget-title { font-size: 13px; font-weight: 500; color: #475569; }
.widget-link { font-size: 12px; color: #3b82f6; background: none; border: none; cursor: pointer; }
.widget-link:hover { text-decoration: underline; }

.widget-body { display: grid; grid-template-columns: 80px 1fr 70px; gap: 14px; align-items: center; }
.empty { color: #94a3b8; font-size: 12px; padding: 8px 0; }

.score-block { text-align: center; }
.score-num { font-size: 32px; font-weight: 700; line-height: 1; color: #1e293b; font-variant-numeric: tabular-nums; letter-spacing: -0.02em; }
.score-grade { font-size: 13px; color: #64748b; margin-top: 2px; }

.meta-block { min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.meta-line { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.meta-name { font-weight: 500; color: #1e293b; max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.meta-rank { background: #f1f5f9; color: #475569; padding: 1px 6px; border-radius: 3px; font-size: 11px; font-variant-numeric: tabular-nums; }
.meta-sub { color: #64748b; font-size: 11px; }
.dot { color: #cbd5e1; }

.redline-label { color: #94a3b8; font-size: 10px; min-width: 36px; }
.redline-bar { flex: 1; height: 4px; background: #f1f5f9; border-radius: 2px; overflow: hidden; max-width: 100px; }
.redline-fill { height: 100%; background: linear-gradient(90deg, #22c55e, #f59e0b, #ef4444); transition: width 0.5s; }
.redline-text { font-size: 11px; font-variant-numeric: tabular-nums; }

.action-block { text-align: center; padding: 4px; border-radius: 6px; background: #f8fafc; }
.action-num { font-size: 20px; font-weight: 700; line-height: 1; font-variant-numeric: tabular-nums; }
.action-lbl { font-size: 10px; color: #64748b; margin-top: 2px; }

.text-success { color: #22c55e; }
.text-warning { color: #f59e0b; }
.text-danger { color: #ef4444; }
</style>
