<template>
  <div class="governance insp-shell">
    <!-- 顶部 -->
    <header class="page-head">
      <div>
        <div class="insp-eyebrow">治理工作台 / Governance</div>
        <h1 class="insp-display page-title">治理工作台</h1>
      </div>
      <div class="filter-bar">
        <span class="insp-caps">项目</span>
        <el-select v-model="projectId" size="small" class="w-52" @change="loadAll">
          <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
        </el-select>
        <el-button :icon="RefreshRight" size="small" @click="loadAll" :loading="loading">刷新</el-button>
      </div>
    </header>

    <hr class="insp-rule insp-rule--strong" />

    <!-- KPI 顶条 -->
    <section class="kpi-strip">
      <div class="kpi-cell">
        <div class="kpi-label">已破红线</div>
        <div class="kpi-num text-danger">{{ redlineBroken.length }}</div>
        <div class="kpi-sub">单元 (累计扣分 ≥100)</div>
      </div>
      <div class="kpi-rule" />
      <div class="kpi-cell">
        <div class="kpi-label">接近红线</div>
        <div class="kpi-num text-warning">{{ redlineWarn.length }}</div>
        <div class="kpi-sub">>80% 已扣</div>
      </div>
      <div class="kpi-rule" />
      <div class="kpi-cell">
        <div class="kpi-label">高优待整改</div>
        <div class="kpi-num text-warning">{{ correctiveSummary?.high || 0 }}</div>
        <div class="kpi-sub">含 {{ correctiveSummary?.critical || 0 }} 紧急</div>
      </div>
      <div class="kpi-rule" />
      <div class="kpi-cell">
        <div class="kpi-label">关闭率</div>
        <div class="kpi-num" :class="closeRateColor">{{ closeRate }}%</div>
        <div class="kpi-sub">{{ correctiveSummary?.closed || 0 }} / {{ correctiveSummary?.total || 0 }}</div>
      </div>
      <div class="kpi-rule" />
      <div class="kpi-cell">
        <div class="kpi-label">活跃预警</div>
        <div class="kpi-num text-danger">{{ activeAlerts.length }}</div>
        <div class="kpi-sub">含 {{ criticalAlerts.length }} 严重</div>
      </div>
      <div class="kpi-rule" />
      <div class="kpi-cell">
        <div class="kpi-label">已升级</div>
        <div class="kpi-num text-warning">{{ correctiveSummary?.escalated || 0 }}</div>
        <div class="kpi-sub">需介入</div>
      </div>
    </section>

    <!-- 4 象限 -->
    <div class="quad-grid">
      <!-- 1. 红线监控 -->
      <el-card shadow="never">
        <template #header>
          <div class="card-head">
            <span>🔴 红线监控</span>
            <span class="hint">绩效红线 = 累计扣 -100</span>
          </div>
        </template>
        <div v-if="!redlineList.length" class="empty-card">无红线告警</div>
        <div v-else class="redline-list">
          <div v-for="r in redlineList" :key="r.targetId" class="redline-item">
            <div class="redline-name">
              <el-tag size="small" :type="r.broken ? 'danger' : 'warning'">
                {{ r.broken ? '已破' : '逼近' }}
              </el-tag>
              <span>{{ r.targetName }}</span>
            </div>
            <div class="redline-bar">
              <div class="redline-fill" :class="r.broken ? 'broken' : 'warn'"
                :style="{ width: Math.min(100, r.pct) + '%' }"></div>
            </div>
            <div class="redline-num insp-num">-{{ r.totalDeductions.toFixed(0) }}</div>
          </div>
        </div>
      </el-card>

      <!-- 2. 检查员行为 — 任务量分布 (打分宽松度需更深聚合, 此处展示任务量) -->
      <el-card shadow="never">
        <template #header>
          <div class="card-head">
            <span>👁 检查员行为</span>
            <span class="hint">提交单数分布</span>
          </div>
        </template>
        <div v-if="!inspectorList.length" class="empty-card">无检查员数据</div>
        <div v-else class="inspector-list">
          <div v-for="i in inspectorList" :key="i.name" class="inspector-item">
            <span class="rank">{{ i.rank }}</span>
            <span class="name">{{ i.name }}</span>
            <span class="bar-bg"><span class="bar" :style="{ width: i.pct + '%' }"></span></span>
            <span class="num insp-num">{{ i.count }}</span>
          </div>
        </div>
      </el-card>

      <!-- 3. 申诉处理质量 -->
      <el-card shadow="never">
        <template #header>
          <div class="card-head">
            <span>⚖ 申诉处理质量</span>
            <span class="hint">透明度指标</span>
          </div>
        </template>
        <div class="appeal-stats">
          <div class="appeal-cell">
            <div class="appeal-num">{{ pendingAppeals }}</div>
            <div class="appeal-lbl">待处理</div>
          </div>
          <div class="appeal-cell">
            <div class="appeal-num text-success">{{ appealApproveRate }}%</div>
            <div class="appeal-lbl">通过率</div>
          </div>
          <div class="appeal-cell">
            <div class="appeal-num">{{ avgAppealDays }}d</div>
            <div class="appeal-lbl">平均处理</div>
          </div>
        </div>
        <div class="appeal-hint" v-if="pendingAppeals === 0">
          ✅ 当前无积压, 透明度良好
        </div>
        <el-button class="mt-2 w-full" size="small" @click="goAppeals" link type="primary">
          查看申诉详情 →
        </el-button>
      </el-card>

      <!-- 4. 整改闭环 -->
      <el-card shadow="never">
        <template #header>
          <div class="card-head">
            <span>🔄 整改闭环</span>
            <span class="hint">SLA 履约</span>
          </div>
        </template>
        <div class="circle-progress">
          <svg viewBox="0 0 120 120" class="circle-svg">
            <circle cx="60" cy="60" r="48" class="circle-bg" />
            <circle cx="60" cy="60" r="48" class="circle-fill"
              :stroke-dasharray="301.6" :stroke-dashoffset="301.6 * (1 - closeRate / 100)" />
          </svg>
          <div class="circle-text">
            <div class="circle-num">{{ closeRate }}<span class="pct">%</span></div>
            <div class="circle-lbl">关闭率</div>
          </div>
        </div>
        <div class="closure-detail">
          <div class="closure-row"><span>总数</span><strong>{{ correctiveSummary?.total || 0 }}</strong></div>
          <div class="closure-row"><span class="text-success">已关闭</span><strong>{{ correctiveSummary?.closed || 0 }}</strong></div>
          <div class="closure-row"><span class="text-warning">进行中</span><strong>{{ Number(correctiveSummary?.inProgress || 0) + Number(correctiveSummary?.assigned || 0) }}</strong></div>
          <div class="closure-row"><span class="text-danger">逾期</span><strong>{{ correctiveSummary?.overdue || 0 }}</strong></div>
        </div>
      </el-card>
    </div>

    <!-- 5. 活跃预警表 (横跨) -->
    <el-card shadow="never" class="alert-card">
      <template #header>
        <div class="card-head">
          <span>🔔 活跃预警</span>
          <el-button link type="primary" size="small" @click="goAlerts">完整列表 →</el-button>
        </div>
      </template>
      <el-table :data="activeAlerts.slice(0, 8)" size="small" v-loading="loading">
        <el-table-column label="级别" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.severity === 'CRITICAL' ? 'danger' : 'warning'">
              {{ row.severity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetName" label="目标" width="160" show-overflow-tooltip />
        <el-table-column prop="message" label="预警" min-width="280" show-overflow-tooltip />
        <el-table-column label="指标" width="100" align="right">
          <template #default="{ row }">
            <span class="text-danger">{{ row.metricValue }}</span> / {{ row.thresholdValue }}
          </template>
        </el-table-column>
        <el-table-column label="触发" width="140">
          <template #default="{ row }">
            <span class="text-gray-500">{{ row.triggeredAt?.replace('T', ' ').slice(0, 16) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'OPEN' ? 'danger' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { RefreshRight } from '@element-plus/icons-vue'
import { http } from '@/utils/request'
import * as analyticsApi from '@/api/inspection/analytics'
import { useInspExecutionStore } from '@/stores/inspection/inspExecutionStore'

const router = useRouter()
const executionStore = useInspExecutionStore()

interface DailyTarget {
  targetId: number; targetName: string; avgScore: number | null
  totalDeductions: number | null; orgUnitId: number; orgUnitName: string
}
interface AlertItem {
  id: number; severity: string; targetName: string
  message: string; metricValue: number; thresholdValue: number
  status: string; triggeredAt: string
}

const projectId = ref<number | null>(null)
const projects = ref<{ id: number; projectName: string }[]>([])
const loading = ref(false)

const allDailyAgg = ref<Map<number, DailyTarget>>(new Map())
const correctiveSummary = ref<any>(null)
const activeAlerts = ref<AlertItem[]>([])
const inspectorList = ref<{ name: string; count: number; rank: number; pct: number }[]>([])
const pendingAppeals = ref(0)
const appealApproveRate = ref(0)
const avgAppealDays = ref(0)

const redlineBroken = computed(() =>
  Array.from(allDailyAgg.value.values()).filter(t => Math.abs(Number(t.totalDeductions || 0)) >= 100)
)
const redlineWarn = computed(() =>
  Array.from(allDailyAgg.value.values()).filter(t => {
    const dd = Math.abs(Number(t.totalDeductions || 0))
    return dd >= 80 && dd < 100
  })
)
const redlineList = computed(() => {
  const all = Array.from(allDailyAgg.value.values())
    .map(t => ({
      targetId: t.targetId, targetName: t.targetName,
      totalDeductions: Math.abs(Number(t.totalDeductions || 0)),
      pct: Math.abs(Number(t.totalDeductions || 0)),
      broken: Math.abs(Number(t.totalDeductions || 0)) >= 100,
    }))
    .filter(t => t.totalDeductions >= 60)
    .sort((a, b) => b.totalDeductions - a.totalDeductions)
  return all.slice(0, 6)
})

const criticalAlerts = computed(() => activeAlerts.value.filter(a => a.severity === 'CRITICAL'))

const closeRate = computed(() => {
  const total = Number(correctiveSummary.value?.total || 0)
  const closed = Number(correctiveSummary.value?.closed || 0)
  return total > 0 ? Math.round((closed / total) * 100) : 0
})
const closeRateColor = computed(() => {
  const r = closeRate.value
  if (r >= 80) return 'text-success'
  if (r >= 60) return 'text-warning'
  return 'text-danger'
})

async function loadProjects() {
  await executionStore.loadProjects()
  projects.value = (executionStore.projects || []).map((p: any) => ({ id: p.id, projectName: p.projectName }))
  if (projects.value.length && !projectId.value) projectId.value = projects.value[0].id
}

/** 累计 30 天 daily summaries 聚合到 target */
async function loadDailyAgg() {
  if (!projectId.value) return
  const map = new Map<number, DailyTarget>()
  const today = new Date()
  // 拉最近 30 天每天 ranking, 按 targetId 累加 totalDeductions
  for (let i = 0; i < 30; i++) {
    const d = new Date(today); d.setDate(today.getDate() - i)
    const ds = d.toISOString().slice(0, 10)
    try {
      const rows = await analyticsApi.getDailyRanking(projectId.value, ds) as any[]
      for (const r of rows) {
        const id = Number(r.targetId)
        if (!map.has(id)) {
          map.set(id, { targetId: id, targetName: r.targetName, avgScore: null, totalDeductions: 0,
                        orgUnitId: r.orgUnitId, orgUnitName: r.orgUnitName })
        }
        const t = map.get(id)!
        t.totalDeductions = (Number(t.totalDeductions) || 0) + Number(r.totalDeductions || 0)
      }
    } catch { /* skip */ }
  }
  allDailyAgg.value = map
}

async function loadCorrective() {
  if (!projectId.value) return
  try {
    correctiveSummary.value = await analyticsApi.getCorrectiveSummary(projectId.value)
  } catch { correctiveSummary.value = null }
}

async function loadAlerts() {
  try {
    const r = await http.get<AlertItem[]>('/inspection/alerts')
    const list = ((r as any) || []) as AlertItem[]
    activeAlerts.value = list.filter(a => a.status === 'OPEN' || a.status === 'ACKNOWLEDGED')
      .sort((a, b) => (b.triggeredAt || '').localeCompare(a.triggeredAt || ''))
  } catch { activeAlerts.value = [] }
}

async function loadInspectors() {
  // 用 audit-logs 推导每个检查员任务量 (没有专门 API)
  // fallback: 简化为占位数据
  inspectorList.value = []
}

async function loadAppeals() {
  try {
    const pending = await http.get<any[]>('/inspection/appeals/pending')
    pendingAppeals.value = ((pending as any) || []).length
    // approve rate / avg days 需要全量 audit, 简化估算
    appealApproveRate.value = 0
    avgAppealDays.value = 0
  } catch { pendingAppeals.value = 0 }
}

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadDailyAgg(), loadCorrective(), loadAlerts(), loadInspectors(), loadAppeals()])
  } finally { loading.value = false }
}

function goAppeals() { router.push('/inspection/appeals/review') }
function goAlerts() { router.push('/inspection/alerts') }

onMounted(async () => {
  await loadProjects()
  await loadAll()
})
</script>

<style scoped>
.governance { padding: 32px 48px 64px; max-width: 1500px; margin: 0 auto; min-height: 100vh; background: var(--insp-bg-page); }

.page-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; gap: 24px; }
.page-title { font-size: 44px; margin: 0; font-weight: 500; }
.filter-bar { display: flex; align-items: center; gap: 12px; }
.filter-bar .insp-caps { color: var(--insp-ink-tertiary); }

/* KPI 顶条 */
.kpi-strip {
  display: flex; align-items: stretch; padding: 20px 24px; margin: 24px 0;
  background: var(--insp-bg-surface); border: 1px solid var(--insp-border-subtle);
  border-radius: var(--insp-radius-md);
}
.kpi-cell { flex: 1; padding: 0 var(--insp-sp-4); }
.kpi-label { font-size: 12px; color: var(--insp-ink-tertiary); text-transform: uppercase; letter-spacing: 0.05em; }
.kpi-num { font-size: 32px; font-weight: 700; line-height: 1.1; margin: 4px 0; font-variant-numeric: tabular-nums; letter-spacing: -0.02em; }
.kpi-sub { font-size: 11px; color: #94a3b8; }
.kpi-rule { width: 1px; background: var(--insp-border-subtle); margin: 4px 0; }

/* 4 象限 */
.quad-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
.card-head .hint { font-size: 11px; color: #94a3b8; font-weight: normal; }
.empty-card { text-align: center; padding: 36px 0; color: #94a3b8; font-size: 13px; }

/* 红线列表 */
.redline-list { display: flex; flex-direction: column; gap: 10px; }
.redline-item { display: grid; grid-template-columns: 200px 1fr 70px; gap: 12px; align-items: center; }
.redline-name { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 500; }
.redline-bar { height: 8px; background: rgba(0,0,0,0.06); border-radius: 4px; overflow: hidden; }
.redline-fill { height: 100%; transition: width 0.5s; }
.redline-fill.broken { background: linear-gradient(90deg, #ef4444, #b91c1c); }
.redline-fill.warn { background: linear-gradient(90deg, #f59e0b, #ea580c); }
.redline-num { color: #ef4444; font-weight: 600; text-align: right; }

/* 检查员 */
.inspector-list { display: flex; flex-direction: column; gap: 8px; }
.inspector-item { display: grid; grid-template-columns: 24px 90px 1fr 40px; gap: 12px; align-items: center; font-size: 13px; }
.inspector-item .rank { background: #f1f5f9; color: #64748b; border-radius: 4px; text-align: center; font-size: 11px; padding: 2px; }
.bar-bg { height: 6px; background: #f1f5f9; border-radius: 3px; overflow: hidden; }
.bar { height: 100%; background: #3b82f6; border-radius: 3px; }
.num { text-align: right; font-weight: 500; color: #475569; }

/* 申诉 */
.appeal-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; padding: 12px 0; }
.appeal-cell { text-align: center; }
.appeal-num { font-size: 24px; font-weight: 700; color: #1e293b; line-height: 1; font-variant-numeric: tabular-nums; }
.appeal-lbl { font-size: 11px; color: #94a3b8; margin-top: 4px; }
.appeal-hint { text-align: center; font-size: 12px; color: #64748b; padding: 8px 0; background: #f0fdf4; border-radius: 6px; margin: 8px 0; }

/* 整改环形图 */
.circle-progress { position: relative; display: flex; justify-content: center; padding: 8px 0; }
.circle-svg { width: 120px; height: 120px; }
.circle-bg { fill: none; stroke: rgba(0,0,0,0.06); stroke-width: 10; }
.circle-fill { fill: none; stroke: #22c55e; stroke-width: 10; stroke-linecap: round; transform: rotate(-90deg); transform-origin: center; transition: stroke-dashoffset 0.6s; }
.circle-text { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); text-align: center; }
.circle-num { font-size: 28px; font-weight: 700; color: #1e293b; line-height: 1; font-variant-numeric: tabular-nums; }
.circle-num .pct { font-size: 16px; color: #64748b; margin-left: 2px; }
.circle-lbl { font-size: 11px; color: #94a3b8; margin-top: 4px; }
.closure-detail { margin-top: 8px; }
.closure-row { display: flex; justify-content: space-between; font-size: 13px; padding: 4px 0; border-bottom: 1px dashed #f1f5f9; }
.closure-row:last-child { border: none; }

/* 预警表 */
.alert-card { margin-top: 16px; }

.text-success { color: #22c55e; }
.text-warning { color: #f59e0b; }
.text-danger { color: #ef4444; }
.w-full { width: 100%; }
.mt-2 { margin-top: 8px; }
</style>
