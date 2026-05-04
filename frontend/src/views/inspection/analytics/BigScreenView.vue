<template>
  <div class="big-screen">
    <header class="big-screen-header">
      <h1>检查数据大屏</h1>
      <div class="header-right">
        <el-select v-model="projectId" size="small" class="!w-44 dark-select" placeholder="项目"
          @change="loadAll">
          <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
        </el-select>
        <span class="datetime">{{ currentTime }}</span>
        <el-button size="small" text style="color:#8cf" @click="loadAll">刷新</el-button>
        <el-button size="small" text style="color:#8cf" @click="toggleFullscreen">
          {{ isFullscreen ? '退出全屏' : '全屏' }}
        </el-button>
        <el-button size="small" text style="color:#8cf" @click="$router.back()">返回</el-button>
      </div>
    </header>

    <div class="big-screen-body">
      <div class="metric-row">
        <div class="metric-card" v-for="m in metrics" :key="m.label">
          <div class="metric-value">{{ m.value }}</div>
          <div class="metric-label">{{ m.label }}</div>
        </div>
      </div>

      <div class="chart-row">
        <div class="chart-panel">
          <h3>近 7 天得分趋势</h3>
          <div class="chart-placeholder">
            <div v-for="(v, i) in trendData" :key="i" class="bar-item">
              <div class="bar" :style="{ height: v.pct + '%' }" :title="v.score + '分'"></div>
              <span class="bar-label">{{ v.label }}</span>
            </div>
            <div v-if="!trendData.length" class="empty-tip">暂无趋势数据</div>
          </div>
        </div>
        <div class="chart-panel">
          <h3>组织得分排名 (Top 5)</h3>
          <div class="rank-list">
            <div v-for="(r, i) in rankData" :key="i" class="rank-item">
              <span class="rank-no" :class="{ top3: i < 3 }">{{ i + 1 }}</span>
              <span class="rank-name" :title="r.name">{{ r.name }}</span>
              <span class="rank-score">{{ r.score?.toFixed(1) }}</span>
              <div class="rank-bar-bg"><div class="rank-bar" :style="{ width: r.pct + '%' }"></div></div>
            </div>
            <div v-if="!rankData.length" class="empty-tip">暂无排名数据</div>
          </div>
        </div>
        <div class="chart-panel">
          <h3>整改状态分布</h3>
          <div class="category-list">
            <div v-for="c in categoryData" :key="c.name" class="cat-item">
              <span class="cat-name">{{ c.name }}</span>
              <div class="cat-bar-bg"><div class="cat-bar" :style="{ width: c.pct + '%', background: c.color }"></div></div>
              <span class="cat-count">{{ c.count }}</span>
            </div>
            <div v-if="!categoryData.length" class="empty-tip">暂无整改数据</div>
          </div>
        </div>
      </div>

      <div class="bottom-row">
        <div class="chart-panel wide">
          <h3>实时观察 (近 10 条负面)</h3>
          <div class="live-feed">
            <div v-for="(item, i) in liveFeed" :key="i" class="feed-item">
              <span class="feed-time">{{ item.time }}</span>
              <span class="feed-text">{{ item.text }}</span>
              <span v-if="item.score != null" class="feed-score" :class="{ neg: item.score < 0 }">{{ item.score > 0 ? '+' : '' }}{{ item.score }}</span>
            </div>
            <div v-if="!liveFeed.length" class="empty-tip">暂无观察记录</div>
          </div>
        </div>
        <div class="chart-panel">
          <h3>今日完成率</h3>
          <div class="completion-ring">
            <svg viewBox="0 0 120 120" class="ring-svg">
              <circle cx="60" cy="60" r="50" class="ring-bg" />
              <circle cx="60" cy="60" r="50" class="ring-fill"
                :stroke-dasharray="314" :stroke-dashoffset="314 * (1 - completionRate / 100)" />
            </svg>
            <div class="ring-text">{{ completionRate }}%</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as analyticsApi from '@/api/inspection/analytics'
import { observationApi } from '@/api/observation'
import { useInspExecutionStore } from '@/stores/inspection/inspExecutionStore'

const executionStore = useInspExecutionStore()
const projects = ref<{ id: number; projectName: string }[]>([])
const projectId = ref<number | null>(null)

const currentTime = ref('')
const isFullscreen = ref(false)
const completionRate = ref(0)

const metrics = ref([
  { label: '今日检查', value: 0 },
  { label: '已发布任务', value: 0 },
  { label: '待整改', value: 0 },
  { label: '平均得分', value: '—' },
])

const trendData = ref<{ label: string; pct: number; score: number }[]>([])
const rankData = ref<{ name: string; score: number; pct: number }[]>([])
const categoryData = ref<{ name: string; count: number; pct: number; color: string }[]>([])
const liveFeed = ref<{ time: string; text: string; score: number | null }[]>([])

let timer: ReturnType<typeof setInterval>
let pollTimer: ReturnType<typeof setInterval>

function updateTime() {
  currentTime.value = new Date().toLocaleString('zh-CN', { hour12: false })
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

function fmtDate(d: Date) {
  return d.toISOString().slice(0, 10)
}

async function loadProjects() {
  await executionStore.loadProjects()
  projects.value = (executionStore.projects || []).map((p: any) => ({ id: p.id, projectName: p.projectName }))
  if (projects.value.length && !projectId.value) {
    projectId.value = projects.value[0].id
  }
}

async function loadAll() {
  if (!projectId.value) return
  await Promise.all([loadTrend(), loadRanking(), loadCorrective(), loadObservations()])
}

/** 近 7 天 dimension-breakdown 平均分 */
async function loadTrend() {
  if (!projectId.value) return
  const end = new Date()
  const start = new Date(); start.setDate(end.getDate() - 6)
  try {
    const data = await analyticsApi.getDimensionBreakdown(projectId.value, fmtDate(start), fmtDate(end))
    const byDate = new Map<string, number[]>()
    for (const r of (data as any[])) {
      if (!r.summaryDate || r.avgScore == null) continue
      if (!byDate.has(r.summaryDate)) byDate.set(r.summaryDate, [])
      byDate.get(r.summaryDate)!.push(Number(r.avgScore))
    }
    const out: { label: string; pct: number; score: number }[] = []
    for (let i = 6; i >= 0; i--) {
      const d = new Date(end); d.setDate(end.getDate() - i)
      const ds = fmtDate(d)
      const arr = byDate.get(ds) || []
      const avg = arr.length ? arr.reduce((a, b) => a + b, 0) / arr.length : 0
      out.push({ label: ds.slice(5), pct: avg, score: +avg.toFixed(1) })
    }
    trendData.value = out
    // 平均得分
    const all = out.filter(x => x.score > 0)
    metrics.value[3].value = all.length ? (all.reduce((a, b) => a + b.score, 0) / all.length).toFixed(1) : '—'
  } catch { trendData.value = [] }
}

/** 今日 daily-ranking, fallback 取最近一日 */
async function loadRanking() {
  if (!projectId.value) return
  let date = fmtDate(new Date())
  let rows: any[] = []
  for (let i = 0; i < 14 && rows.length === 0; i++) {
    const d = new Date(); d.setDate(d.getDate() - i)
    const ds = fmtDate(d)
    try {
      const r = await analyticsApi.getDailyRanking(projectId.value, ds) as any[]
      if (r.length) { rows = r; date = ds; break }
    } catch { /* ignore */ }
  }
  metrics.value[0].value = rows.length // 今日检查目标数
  metrics.value[1].value = rows.reduce((a: number, x: any) => a + (x.inspectionCount || 0), 0)
  // Ranking top 5
  const top = [...rows].sort((a, b) => (b.avgScore ?? 0) - (a.avgScore ?? 0)).slice(0, 5)
  const max = top[0]?.avgScore ?? 100
  rankData.value = top.map(r => ({
    name: r.targetName || '未命名',
    score: Number(r.avgScore ?? 0),
    pct: Number(r.avgScore ?? 0) / Math.max(max, 1) * 100,
  }))
  // 完成率
  if (rows.length > 0) {
    const passed = rows.filter((r: any) => (r.avgScore ?? 0) >= 60).length
    completionRate.value = Math.round(passed / rows.length * 100)
  }
}

/** 整改 summary live */
async function loadCorrective() {
  if (!projectId.value) return
  try {
    const s = await analyticsApi.getCorrectiveSummary(projectId.value) as any
    const n = (v: any) => Number(v ?? 0) || 0
    const open = n(s?.open), assigned = n(s?.assigned), inProgress = n(s?.inProgress),
          verified = n(s?.verified), rejected = n(s?.rejected), closed = n(s?.closed),
          total = n(s?.total)
    metrics.value[2].value = open + assigned + inProgress
    categoryData.value = [
      { name: '待分配', count: open, pct: pct(open, total), color: '#94a3b8' },
      { name: '整改中', count: assigned + inProgress, pct: pct(assigned + inProgress, total), color: '#e6a23c' },
      { name: '已验证', count: verified, pct: pct(verified, total), color: '#67c23a' },
      { name: '已驳回', count: rejected, pct: pct(rejected, total), color: '#f56c6c' },
      { name: '已关闭', count: closed, pct: pct(closed, total), color: '#409eff' },
    ].filter(c => c.count > 0)
  } catch { categoryData.value = [] }
}

function pct(part: number | undefined, total: number | undefined): number {
  if (!total || !part) return 0
  return Math.min(100, Math.round(part / total * 100))
}

/** 实时观察 */
async function loadObservations() {
  if (!projectId.value) return
  try {
    const r = await observationApi.list({ projectId: projectId.value, isNegative: true, page: 1, size: 10 })
    const items = (r as any).records ?? r ?? []
    liveFeed.value = items.slice(0, 10).map((o: any) => ({
      time: (o.observedAt || o.createdAt || '').slice(11, 16) || '--:--',
      text: `${o.subjectType || ''} ${o.subjectId || ''} ${o.itemName || o.eventTypeCode || ''} ${o.note || ''}`.trim(),
      score: o.score ?? null,
    }))
  } catch { liveFeed.value = [] }
}

onMounted(async () => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  await loadProjects()
  await loadAll()
  // 60 秒轮询
  pollTimer = setInterval(loadAll, 60_000)
})
onUnmounted(() => {
  clearInterval(timer)
  clearInterval(pollTimer)
})
</script>

<style scoped>
.big-screen {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  color: #e2e8f0;
  display: flex;
  flex-direction: column;
}
.big-screen-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 24px; border-bottom: 1px solid rgba(255,255,255,0.08);
}
.big-screen-header h1 {
  font-size: 20px; font-weight: 600;
  background: linear-gradient(90deg, #60a5fa, #a78bfa);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
}
.header-right { display: flex; align-items: center; gap: 16px; }
.datetime { font-size: 13px; color: #94a3b8; font-variant-numeric: tabular-nums; }

:deep(.dark-select .el-input__wrapper) {
  background: rgba(255,255,255,0.05); box-shadow: none;
  border: 1px solid rgba(255,255,255,0.12);
}
:deep(.dark-select .el-input__inner) { color: #cbd5e1; }

.big-screen-body { flex: 1; padding: 16px 24px; display: flex; flex-direction: column; gap: 16px; }

.metric-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.metric-card {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 12px; padding: 20px; text-align: center;
}
.metric-value { font-size: 32px; font-weight: 700; color: #60a5fa; }
.metric-label { font-size: 13px; color: #94a3b8; margin-top: 4px; }

.chart-row { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 16px; flex: 1; }
.chart-panel {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 12px; padding: 16px;
  display: flex; flex-direction: column;
}
.chart-panel h3 { font-size: 14px; font-weight: 500; color: #cbd5e1; margin-bottom: 12px; }
.chart-panel.wide { flex: 2; }

.bottom-row { display: grid; grid-template-columns: 2fr 1fr; gap: 16px; }

.chart-placeholder { display: flex; align-items: flex-end; gap: 8px; flex: 1; padding-top: 8px; min-height: 120px; }
.bar-item { flex: 1; display: flex; flex-direction: column; align-items: center; }
.bar { width: 100%; background: linear-gradient(180deg, #60a5fa, #3b82f6); border-radius: 4px 4px 0 0; min-height: 4px; transition: height 0.5s; }
.bar-label { font-size: 11px; color: #94a3b8; margin-top: 6px; }

.rank-list { display: flex; flex-direction: column; gap: 8px; }
.rank-item { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.rank-no { width: 20px; height: 20px; display: flex; align-items: center; justify-content: center; border-radius: 4px; font-size: 11px; font-weight: 600; background: rgba(255,255,255,0.1); }
.rank-no.top3 { background: #f59e0b; color: #0f172a; }
.rank-name { width: 110px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rank-score { width: 44px; text-align: right; color: #60a5fa; font-weight: 500; }
.rank-bar-bg { flex: 1; height: 6px; background: rgba(255,255,255,0.08); border-radius: 3px; }
.rank-bar { height: 100%; background: #3b82f6; border-radius: 3px; transition: width 0.5s; }

.category-list { display: flex; flex-direction: column; gap: 10px; }
.cat-item { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.cat-name { width: 60px; }
.cat-bar-bg { flex: 1; height: 8px; background: rgba(255,255,255,0.08); border-radius: 4px; }
.cat-bar { height: 100%; border-radius: 4px; transition: width 0.5s; }
.cat-count { width: 30px; text-align: right; color: #94a3b8; }

.live-feed { flex: 1; overflow-y: auto; }
.feed-item { display: flex; gap: 12px; align-items: center; padding: 8px 0; border-bottom: 1px solid rgba(255,255,255,0.05); font-size: 13px; }
.feed-time { color: #60a5fa; font-variant-numeric: tabular-nums; white-space: nowrap; }
.feed-text { color: #cbd5e1; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.feed-score { font-weight: 500; color: #67c23a; font-variant-numeric: tabular-nums; }
.feed-score.neg { color: #f87171; }

.completion-ring { display: flex; align-items: center; justify-content: center; flex: 1; position: relative; }
.ring-svg { width: 120px; height: 120px; }
.ring-bg { fill: none; stroke: rgba(255,255,255,0.08); stroke-width: 8; }
.ring-fill { fill: none; stroke: #60a5fa; stroke-width: 8; stroke-linecap: round; transform: rotate(-90deg); transform-origin: center; transition: stroke-dashoffset 0.5s; }
.ring-text { position: absolute; font-size: 28px; font-weight: 700; color: #60a5fa; }

.empty-tip { text-align: center; color: #475569; padding: 32px 0; font-size: 13px; flex: 1; display: flex; align-items: center; justify-content: center; }
</style>
