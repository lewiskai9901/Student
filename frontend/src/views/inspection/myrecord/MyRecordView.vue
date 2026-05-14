<template>
  <div class="my-record insp-shell">
    <!-- 顶部: 单元切换 -->
    <header class="page-head">
      <div>
        <div class="insp-eyebrow">受检主体视角 / My Record</div>
        <h1 class="insp-display page-title">我的成绩单</h1>
      </div>
      <div class="filter-bar">
        <span class="insp-caps">受检单元</span>
        <el-select v-model="selectedTargetKey" :placeholder="`选择${group}`"
          class="w-64" @change="loadAll" size="small" filterable>
          <el-option v-for="t in availableTargets" :key="t.key" :label="t.name" :value="t.key" />
        </el-select>
        <el-button size="small" :icon="RefreshRight" @click="loadAll">刷新</el-button>
      </div>
    </header>

    <hr class="insp-rule insp-rule--strong" />

    <div v-if="!current" class="empty-hint">
      <el-empty description="请选择一个受检单元" />
    </div>

    <template v-else>
      <!--  结果层 —— 1 行答案 -->
      <section class="result-banner" :class="resultClass">
        <div class="banner-main">
          <div class="banner-score">
            <span class="score-num insp-num">{{ current.avgScore?.toFixed(1) ?? '—' }}</span>
            <span class="score-grade">{{ current.grade || '—' }}</span>
          </div>
          <div class="banner-meta">
            <div class="banner-name">{{ current.targetName }}</div>
            <div class="banner-sub">
              排名 <strong>{{ current.ranking || '—' }}</strong> / {{ totalUnits }} ·
              本次检查 {{ current.summaryDate }} ·
              <span :class="trendClass">趋势 {{ trendArrow }} {{ trendDelta }}</span>
            </div>
          </div>
        </div>
        <div class="banner-redline">
          <div class="redline-label">距离绩效红线</div>
          <div class="redline-bar">
            <div class="redline-fill" :style="{ width: redlinePct + '%' }"></div>
          </div>
          <div class="redline-text">
            累计扣分 <strong>{{ totalDeductions }}</strong> / 红线 -100 ·
            <span :class="redlineColor">还剩 {{ 100 - totalDeductions }} 分</span>
          </div>
        </div>
      </section>

      <!--  影响层 +  责任层 —— 2 列 -->
      <div class="grid-2">
        <!-- 趋势 -->
        <el-card shadow="never">
          <template #header>
            <div class="card-head">
              <span>近 6 次得分趋势</span>
              <span v-if="trendData.length" class="trend-meta">
                {{ trendData[0].summaryDate?.slice(5) }} ~ {{ trendData.at(-1)?.summaryDate?.slice(5) }}
              </span>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-200"></div>
          <div v-if="!trendData.length" class="text-center text-gray-400 py-8 text-sm">暂无趋势数据</div>
        </el-card>

        <!-- 待整改 -->
        <el-card shadow="never">
          <template #header>
            <div class="card-head">
              <span> 待整改 <el-tag size="small" type="danger" v-if="openCases.length">{{ openCases.length }}</el-tag></span>
              <el-button size="small" link @click="goAllCases">全部 ></el-button>
            </div>
          </template>
          <div class="case-list">
            <div v-for="c in openCases.slice(0, 5)" :key="c.id" class="case-item" :class="caseUrgencyClass(c)">
              <div class="case-head">
                <span class="case-deadline">{{ deadlineLabel(c) }}</span>
                <el-tag size="small" :type="priorityType(c.priority)">{{ priorityLabel(c.priority) }}</el-tag>
              </div>
              <div class="case-text">{{ c.issueDescription }}</div>
              <div class="case-foot">
                <span v-if="c.deadline" class="case-time">{{ c.deadline?.slice(0, 10) }}</span>
                <el-button size="small" link type="primary" @click="goCase(c)">查看 / 整改</el-button>
              </div>
            </div>
            <div v-if="!openCases.length" class="text-center text-gray-400 py-6 text-sm">
               当前无待整改单, 表现良好
            </div>
          </div>
        </el-card>
      </div>

      <!--  责任层 — Top 问题 + 鼓励 -->
      <div class="grid-2">
        <el-card shadow="never">
          <template #header>
            <div class="card-head"><span> 高频扣分点 Top 3</span></div>
          </template>
          <div v-if="!topIssues.length" class="text-center text-gray-400 py-6 text-sm">本期暂无扣分</div>
          <div v-else class="issue-list">
            <div v-for="(it, i) in topIssues" :key="i" class="issue-item">
              <span class="issue-rank">{{ i + 1 }}</span>
              <div class="issue-body">
                <div class="issue-name">{{ it.itemName }}</div>
                <div class="issue-meta">{{ it.sectionName }} · 累计 {{ it.count }} 次</div>
              </div>
              <span class="issue-score">-{{ it.totalDeduction.toFixed(1) }}</span>
            </div>
          </div>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-head"><span> 我做得好的</span></div>
          </template>
          <div v-if="!goodItems.length" class="text-center text-gray-400 py-6 text-sm">继续努力</div>
          <div v-else class="good-list">
            <div v-for="(g, i) in goodItems" :key="i" class="good-item">
              <span class="good-icon"></span>
              <span class="good-text">{{ g.text }}</span>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 申诉成功率 + 历史对比 -->
      <div class="grid-2">
        <el-card shadow="never">
          <template #header>
            <div class="card-head"><span> 申诉历史</span></div>
          </template>
          <div v-if="!appealStats.total" class="text-center text-gray-400 py-6 text-sm">
            尚未提交过申诉
          </div>
          <div v-else class="appeal-stats">
            <div class="appeal-cell">
              <div class="appeal-num">{{ appealStats.total }}</div>
              <div class="appeal-lbl">累计申诉</div>
            </div>
            <div class="appeal-cell">
              <div class="appeal-num text-success">{{ appealStats.approved }}</div>
              <div class="appeal-lbl">已通过</div>
            </div>
            <div class="appeal-cell">
              <div class="appeal-num text-warning">{{ appealStats.pending }}</div>
              <div class="appeal-lbl">待处理</div>
            </div>
            <div class="appeal-cell">
              <div class="appeal-num">{{ appealStats.rate }}%</div>
              <div class="appeal-lbl">通过率</div>
            </div>
          </div>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-head"><span> 重复扣分项</span></div>
          </template>
          <div v-if="!repeatItems.length" class="text-center text-gray-400 py-6 text-sm">
             无重复扣分, 整改有效
          </div>
          <div v-else class="repeat-list">
            <div v-for="r in repeatItems" :key="r.itemCode" class="repeat-item">
              <span class="repeat-tag">连续 {{ r.times }} 次</span>
              <span class="repeat-name">{{ r.itemName }}</span>
              <span class="repeat-section">{{ r.sectionName }}</span>
            </div>
          </div>
        </el-card>
      </div>

      <!--  明细层 — 默认折叠 -->
      <el-card shadow="never" class="detail-card">
        <template #header>
          <div class="card-head">
            <el-button size="small" link @click="showDetails = !showDetails">
              {{ showDetails ? 'v 收起' : '> 展开' }} 完整明细 ({{ allDetails.length }} 条)
            </el-button>
            <span class="hint" v-if="showDetails">点击  申诉对扣分有异议的条目</span>
          </div>
        </template>
        <el-table v-if="showDetails" :data="allDetails" size="small" max-height="400" row-class-name="detail-row">
          <el-table-column prop="itemName" label="检查项" min-width="140">
            <template #default="{ row }">
              <span>{{ row.itemName }}</span>
              <el-tag v-if="isRepeatItem(row.itemCode)" size="small" type="warning" class="ml-1"> 复发</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sectionName" label="所属类别" min-width="100" show-overflow-tooltip />
          <el-table-column prop="itemType" label="类型" width="90" />
          <el-table-column label="响应" min-width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.responseValue === 'PASS' || (typeof row.responseValue === 'string' && /\d+/.test(row.responseValue) && +row.responseValue >= 80) ? 'success' : (row.responseValue === 'FAIL' ? 'danger' : 'info')">
                {{ row.responseValue }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="得分" width="70" align="right">
            <template #default="{ row }">
              <span :class="(row.score ?? 0) >= 3 ? 'text-success' : (row.score ?? 0) > 0 ? 'text-warning' : 'text-danger'">
                {{ row.score?.toFixed(1) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="标记" width="70">
            <template #default="{ row }">
              <el-tag v-if="row.isFlagged" size="small" type="danger">问题</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="申诉" width="80">
            <template #default="{ row }">
              <el-button v-if="row.isFlagged || (row.score ?? 5) < 3" size="small" link type="primary"
                @click="openAppeal(row)"> 申诉</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- 申诉对话框 -->
    <SubmitAppealDialog v-if="appealRow" v-model="appealDialog"
      :submission-detail-id="appealRow.id"
      :item-name="appealRow.itemName"
      :current-score="Number(appealRow.score)"
      @submitted="onAppealSubmitted" />
  </div>
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useEntityLabel } from '@/composables/useEntityLabel'

const { group } = useEntityLabel()
import { ElMessage } from 'element-plus'
import { RefreshRight } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, MarkLineComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ECharts, EChartsCoreOption as EChartsOption } from 'echarts/core'
import { http } from '@/utils/request'
import * as analyticsApi from '@/api/inspection/analytics'
import { useInspExecutionStore } from '@/stores/inspection/inspExecutionStore'
import SubmitAppealDialog from '@/views/inspection/appeals/components/SubmitAppealDialog.vue'

echarts.use([LineChart, GridComponent, TooltipComponent, MarkLineComponent, CanvasRenderer])

const router = useRouter()
const executionStore = useInspExecutionStore()

interface DailyTarget {
  targetType: string
  targetId: LongId
  targetName: string
  orgUnitId: LongId
  orgUnitName: string
  avgScore: number | null
  ranking: number | null
  grade: string | null
  summaryDate: string
  totalDeductions?: number | null
  inspectionCount?: number | null
}

interface CorrectiveCase {
  id: LongId
  caseCode: string
  issueDescription: string
  priority: string
  deadline: string | null
  status: string
  targetId: LongId
  targetName: string
  orgUnitId: LongId
  taskId: LongId
}

interface SubmissionDetailRow {
  id: LongId
  itemName: string
  itemCode: string
  itemType: string
  sectionName: string
  responseValue: string
  score: number | null
  isFlagged: number | boolean
}

// ========== State ==========

const projectId = ref<LongId | null>(null)
const projects = ref<{ id: LongId; projectName: string }[]>([])
const allTargets = ref<DailyTarget[]>([])
const selectedTargetKey = ref<string>('')

const current = ref<DailyTarget | null>(null)
const trendData = ref<DailyTarget[]>([])
const allCases = ref<CorrectiveCase[]>([])
const allDetails = ref<SubmissionDetailRow[]>([])
const showDetails = ref(false)

const trendChartRef = ref<HTMLElement | null>(null)
let trendChart: ECharts | null = null

// 申诉对话框 state
const appealDialog = ref(false)
const appealRow = ref<SubmissionDetailRow | null>(null)

// 申诉历史统计
const appealStats = ref<{ total: number; approved: number; pending: number; rejected: number; rate: number }>({
  total: 0, approved: 0, pending: 0, rejected: 0, rate: 0,
})

// 历史明细 (近 30 天) — 用于 "重复扣分项" 检测
const historicalFlaggedItems = ref<Map<string, { itemName: string; sectionName: string; dates: Set<string> }>>(new Map())

// ========== Derived ==========

const availableTargets = computed(() =>
  allTargets.value.map(t => ({ key: `${t.targetType}:${t.targetId}`, name: t.targetName, orgUnitId: t.orgUnitId }))
)

const totalUnits = computed(() => allTargets.value.length)

const trendDelta = computed(() => {
  if (trendData.value.length < 2) return ''
  const latest = trendData.value.at(-1)?.avgScore ?? 0
  const prev = trendData.value.at(-2)?.avgScore ?? 0
  const d = Number(latest) - Number(prev)
  return (d > 0 ? '+' : '') + d.toFixed(1)
})
const trendArrow = computed(() => {
  const d = parseFloat(trendDelta.value)
  return d > 0 ? '↑' : d < 0 ? '↓' : '>'
})
const trendClass = computed(() => {
  const d = parseFloat(trendDelta.value)
  return d > 0 ? 'text-success' : d < 0 ? 'text-danger' : 'text-gray-500'
})

const totalDeductions = computed(() => {
  return Math.abs(Math.round(trendData.value.reduce((s, t) => s + (Math.abs(Number(t.totalDeductions) || 0)), 0)))
})
const redlinePct = computed(() => Math.min(100, Math.round((totalDeductions.value / 100) * 100)))
const redlineColor = computed(() => {
  if (redlinePct.value > 80) return 'text-danger'
  if (redlinePct.value > 50) return 'text-warning'
  return 'text-success'
})

const resultClass = computed(() => {
  const s = current.value?.avgScore ?? 0
  if (s >= 90) return 'banner-excellent'
  if (s >= 75) return 'banner-good'
  if (s >= 60) return 'banner-warn'
  return 'banner-fail'
})

// 待整改 (OPEN/ASSIGNED/IN_PROGRESS)
const openCases = computed(() =>
  allCases.value.filter(c => ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'REJECTED'].includes(c.status))
    .sort((a, b) => (a.deadline || '').localeCompare(b.deadline || ''))
)

// Top 3 高频扣分: 聚合 details 中 isFlagged 项
const topIssues = computed(() => {
  const map = new Map<string, { itemName: string; sectionName: string; count: number; totalDeduction: number }>()
  for (const d of allDetails.value) {
    if (!d.isFlagged) continue
    const key = d.itemCode || d.itemName
    if (!map.has(key)) map.set(key, { itemName: d.itemName, sectionName: d.sectionName, count: 0, totalDeduction: 0 })
    const obj = map.get(key)!
    obj.count++
    // 扣分 = 满分 - 实际, 这里用绝对值估算 (item_weight*5 是满分基准)
    obj.totalDeduction += Math.max(0, 5 - Number(d.score ?? 0))
  }
  return Array.from(map.values()).sort((a, b) => b.totalDeduction - a.totalDeduction).slice(0, 3)
})

// 重复扣分项: 历史 ≥2 次出现的同一 itemCode (说明整改无效)
const repeatItems = computed(() => {
  const out: { itemCode: string; itemName: string; sectionName: string; times: number }[] = []
  for (const [code, info] of historicalFlaggedItems.value.entries()) {
    if (info.dates.size >= 2) {
      out.push({ itemCode: code, itemName: info.itemName, sectionName: info.sectionName, times: info.dates.size })
    }
  }
  return out.sort((a, b) => b.times - a.times).slice(0, 5)
})

function isRepeatItem(itemCode: string): boolean {
  const info = historicalFlaggedItems.value.get(itemCode)
  return info ? info.dates.size >= 2 : false
}

// 鼓励项: section 维度的零扣分集合
const goodItems = computed(() => {
  if (!allDetails.value.length) return []
  const bySection = new Map<string, { total: number; flagged: number }>()
  for (const d of allDetails.value) {
    const key = d.sectionName || '其他'
    if (!bySection.has(key)) bySection.set(key, { total: 0, flagged: 0 })
    const obj = bySection.get(key)!
    obj.total++
    if (d.isFlagged) obj.flagged++
  }
  const out: { text: string }[] = []
  for (const [name, v] of bySection.entries()) {
    if (v.flagged === 0 && v.total >= 2) out.push({ text: `${name} 全部达标 (${v.total} 项零扣分)` })
  }
  return out.slice(0, 4)
})

// ========== Methods ==========

async function loadProjects() {
  await executionStore.loadProjects()
  projects.value = (executionStore.projects || []).map((p: any) => ({ id: p.id, projectName: p.projectName }))
  if (projects.value.length && !projectId.value) {
    projectId.value = projects.value[0].id
  }
}

/** 找最近一日有 daily_summary 的日期 + 拉所有 targets */
async function loadAvailableTargets() {
  if (!projectId.value) return
  for (let i = 0; i < 30; i++) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    const ds = d.toISOString().slice(0, 10)
    try {
      const rows = await analyticsApi.getDailyRanking(projectId.value, ds) as DailyTarget[]
      if (rows.length > 0) {
        allTargets.value = rows
        if (!selectedTargetKey.value) selectedTargetKey.value = `${rows[0].targetType}:${rows[0].targetId}`
        return
      }
    } catch { /* skip */ }
  }
  allTargets.value = []
}

async function loadTrend() {
  if (!projectId.value || !current.value) return
  const end = new Date()
  const start = new Date(); start.setDate(end.getDate() - 30)
  try {
    const data = await analyticsApi.getTrend(
      projectId.value, current.value.targetType, current.value.targetId,
      start.toISOString().slice(0, 10), end.toISOString().slice(0, 10)
    ) as DailyTarget[]
    trendData.value = data.sort((a, b) => a.summaryDate.localeCompare(b.summaryDate)).slice(-6)
  } catch { trendData.value = [] }
  await nextTick()
  drawTrend()
}

async function loadCases() {
  if (!current.value || !projectId.value) return
  try {
    // 后端 list 暂不支持 orgUnitId 过滤, 拉项目级再客户端按 targetId 过滤
    const r = await http.get<CorrectiveCase[]>('/inspection/corrective-cases', {
      params: { projectId: projectId.value }
    })
    const all = ((r as any) || []) as any[]
    const tid = String(current.value.targetId)
    allCases.value = all.filter(c => String(c.targetId) === tid)
  } catch { allCases.value = [] }
}

/** 拉最近一次提交的 details */
async function loadLatestDetails() {
  if (!current.value || !projectId.value) return
  try {
    // 最近一次 submission for this target
    const subsResp = await http.get<any>('/inspection/submissions', {
      params: { projectId: projectId.value, targetType: current.value.targetType, targetId: current.value.targetId, size: 1 }
    })
    const subs = subsResp?.records || subsResp || []
    if (!subs.length) { allDetails.value = []; return }
    const submissionId = subs[0].id
    const details = await http.get<any>('/inspection/submissions/' + submissionId + '/details')
    allDetails.value = (details as any) || []
  } catch { allDetails.value = [] }
}

async function loadAll() {
  if (!selectedTargetKey.value) return
  current.value = allTargets.value.find(
    t => `${t.targetType}:${t.targetId}` === selectedTargetKey.value
  ) || null
  if (!current.value) return
  await Promise.all([
    loadTrend(), loadCases(), loadLatestDetails(),
    loadHistoricalFlagged(), loadAppealStats(),
  ])
}

/** 拉历史 5 次提交的 details, 聚合 isFlagged itemCode 统计复发 */
async function loadHistoricalFlagged() {
  if (!current.value || !projectId.value) return
  try {
    const subsResp = await http.get<any>('/inspection/submissions', {
      params: { projectId: projectId.value, targetType: current.value.targetType,
        targetId: current.value.targetId, size: 6 }
    })
    const subs = (subsResp as any)?.records ?? subsResp ?? []
    const map = new Map<string, { itemName: string; sectionName: string; dates: Set<string> }>()
    for (const s of subs.slice(0, 6)) {
      try {
        const details = await http.get<any>('/inspection/submissions/' + s.id + '/details') as any[]
        const date = (s.createdAt || s.completedAt || '').slice(0, 10) || s.id
        for (const d of details) {
          if (!d.isFlagged) continue
          const key = d.itemCode || d.itemName
          if (!map.has(key)) {
            map.set(key, { itemName: d.itemName, sectionName: d.sectionName, dates: new Set() })
          }
          map.get(key)!.dates.add(date)
        }
      } catch { /* skip */ }
    }
    historicalFlaggedItems.value = map
  } catch { historicalFlaggedItems.value = new Map() }
}

/** 拉申诉成功率 — 当前用户提交过的所有申诉 */
async function loadAppealStats() {
  try {
    const r = await http.get<any[]>('/inspection/appeals/my')
    const list = ((r as any) || []) as any[]
    const total = list.length
    const approved = list.filter(a => a.status === 'APPROVED').length
    const pending = list.filter(a => a.status === 'PENDING').length
    const rejected = list.filter(a => a.status === 'REJECTED').length
    const reviewed = approved + rejected
    const rate = reviewed > 0 ? Math.round((approved / reviewed) * 100) : 0
    appealStats.value = { total, approved, pending, rejected, rate }
  } catch { appealStats.value = { total: 0, approved: 0, pending: 0, rejected: 0, rate: 0 } }
}

function openAppeal(row: SubmissionDetailRow) {
  appealRow.value = row
  appealDialog.value = true
}
function onAppealSubmitted() {
  appealDialog.value = false
  loadAppealStats()
}

function drawTrend() {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  if (!trendData.value.length) { trendChart.clear(); return }

  const dates = trendData.value.map(t => t.summaryDate.slice(5))
  const scores = trendData.value.map(t => Number(t.avgScore ?? 0))

  const opt: EChartsOption = {
    grid: { top: 20, right: 20, bottom: 30, left: 40 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', min: Math.max(0, Math.floor(Math.min(...scores) * 0.9)) },
    series: [{
      type: 'line',
      data: scores,
      smooth: true,
      itemStyle: { color: '#3b82f6' },
      lineStyle: { width: 2.5 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(59,130,246,0.25)' },
        { offset: 1, color: 'rgba(59,130,246,0.02)' },
      ])},
      markLine: {
        symbol: 'none',
        data: [{ yAxis: 60, lineStyle: { color: '#f87171', type: 'dashed' }, label: { formatter: '通过线' } }]
      }
    }]
  }
  trendChart.setOption(opt, true)
}

function priorityLabel(p: string) {
  return ({ CRITICAL: '紧急', HIGH: '高', MEDIUM: '中', LOW: '低' } as any)[p] || p
}
function priorityType(p: string) {
  return ({ CRITICAL: 'danger', HIGH: 'warning', MEDIUM: 'info', LOW: 'success' } as any)[p] || 'info'
}
function deadlineLabel(c: CorrectiveCase) {
  if (!c.deadline) return ''
  const days = Math.ceil((new Date(c.deadline).getTime() - Date.now()) / 86400000)
  if (days < 0) return ` 已超期 ${-days}d`
  if (days === 0) return ` 今日截止`
  if (days <= 2) return ` ${days} 天内`
  return `${days} 天后`
}
function caseUrgencyClass(c: CorrectiveCase) {
  if (!c.deadline) return ''
  const days = Math.ceil((new Date(c.deadline).getTime() - Date.now()) / 86400000)
  if (days < 0) return 'urgent-overdue'
  if (days <= 2) return 'urgent-soon'
  return ''
}
function goCase(c: CorrectiveCase) {
  router.push(`/inspection/corrective?caseId=${c.id}`)
    .catch(() => ElMessage.info('整改单 #' + c.caseCode))
}
function goAllCases() {
  if (!current.value) return
  router.push(`/inspection/corrective?orgUnitId=${current.value.orgUnitId}`)
}

function handleResize() { trendChart?.resize() }

watch(() => projects.value, async (v) => {
  if (v.length && !projectId.value) {
    projectId.value = v[0].id
    await loadAvailableTargets()
    await loadAll()
  }
})

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await loadProjects()
  await loadAvailableTargets()
  await loadAll()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
})
</script>

<style scoped>
.my-record { padding: 32px 48px 64px; max-width: 1500px; margin: 0 auto; min-height: 100vh; background: var(--insp-bg-page); }

.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 16px; }
.page-title { font-size: 44px; margin: 0; font-weight: 500; }
.filter-bar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.filter-bar .insp-caps { color: var(--insp-ink-tertiary); }

.empty-hint { padding: 80px 0; }

/* ── 结果横幅 ── */
.result-banner {
  display: grid; grid-template-columns: 1fr 1fr; gap: 32px;
  padding: 28px 32px; border-radius: 16px; margin: 24px 0;
  border: 1px solid var(--insp-border-subtle);
  background: linear-gradient(135deg, #fafbfc 0%, #f3f4f6 100%);
}
.result-banner.banner-excellent { background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%); border-color: #86efac; }
.result-banner.banner-good      { background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%); border-color: #93c5fd; }
.result-banner.banner-warn      { background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%); border-color: #fcd34d; }
.result-banner.banner-fail      { background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%); border-color: #fca5a5; }

.banner-main { display: flex; align-items: center; gap: 24px; }
.banner-score { display: flex; align-items: baseline; gap: 8px; }
.score-num { font-size: 64px; font-weight: 700; line-height: 1; letter-spacing: -0.03em; color: #1e293b; }
.score-grade { font-size: 22px; color: #475569; font-weight: 500; }
.banner-meta { display: flex; flex-direction: column; gap: 6px; }
.banner-name { font-size: 18px; font-weight: 600; color: #1e293b; }
.banner-sub { font-size: 13px; color: #64748b; }
.banner-sub strong { color: #334155; font-variant-numeric: tabular-nums; }

.banner-redline { display: flex; flex-direction: column; gap: 8px; align-self: center; }
.redline-label { font-size: 12px; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; }
.redline-bar { height: 10px; background: rgba(0,0,0,0.06); border-radius: 5px; overflow: hidden; }
.redline-fill {
  height: 100%; border-radius: 5px;
  background: linear-gradient(90deg, #22c55e 0%, #f59e0b 50%, #ef4444 100%);
  transition: width 0.5s;
}
.redline-text { font-size: 13px; color: #475569; font-variant-numeric: tabular-nums; }

/* ── 网格布局 ── */
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
.trend-meta { font-size: 11px; color: #94a3b8; }
.chart-200 { height: 200px; width: 100%; }

/* ── 待整改列表 ── */
.case-list { display: flex; flex-direction: column; gap: 8px; max-height: 240px; overflow-y: auto; }
.case-item {
  padding: 10px 12px; border-radius: 8px; border: 1px solid #e5e7eb;
  background: #fff; transition: all 0.2s;
}
.case-item.urgent-overdue { border-color: #ef4444; background: #fef2f2; }
.case-item.urgent-soon { border-color: #f59e0b; background: #fffbeb; }
.case-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.case-deadline { font-size: 12px; font-weight: 600; color: #475569; }
.urgent-overdue .case-deadline { color: #ef4444; }
.urgent-soon .case-deadline { color: #f59e0b; }
.case-text { font-size: 13px; color: #1e293b; line-height: 1.4; margin-bottom: 4px; }
.case-foot { display: flex; justify-content: space-between; align-items: center; }
.case-time { font-size: 11px; color: #94a3b8; font-variant-numeric: tabular-nums; }

/* ── Top 问题 ── */
.issue-list { display: flex; flex-direction: column; gap: 10px; }
.issue-item { display: flex; align-items: center; gap: 12px; padding: 6px 0; }
.issue-rank {
  width: 24px; height: 24px; border-radius: 6px; background: #fef3c7; color: #92400e;
  display: flex; align-items: center; justify-content: center; font-weight: 600; font-size: 13px;
}
.issue-item:nth-child(1) .issue-rank { background: #fee2e2; color: #991b1b; }
.issue-item:nth-child(2) .issue-rank { background: #fed7aa; color: #9a3412; }
.issue-item:nth-child(3) .issue-rank { background: #fef3c7; color: #92400e; }
.issue-body { flex: 1; }
.issue-name { font-size: 14px; color: #1e293b; font-weight: 500; }
.issue-meta { font-size: 11px; color: #94a3b8; margin-top: 2px; }
.issue-score { color: #ef4444; font-weight: 600; font-variant-numeric: tabular-nums; }

/* ── 鼓励 ── */
.good-list { display: flex; flex-direction: column; gap: 8px; }
.good-item { display: flex; gap: 10px; align-items: center; padding: 6px 10px; background: #f0fdf4; border-radius: 6px; }
.good-icon { font-size: 14px; }
.good-text { font-size: 13px; color: #166534; }

/* ── 申诉历史 ── */
.appeal-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.appeal-cell { text-align: center; }
.appeal-num { font-size: 26px; font-weight: 700; color: #1e293b; line-height: 1; font-variant-numeric: tabular-nums; }
.appeal-lbl { font-size: 11px; color: #94a3b8; margin-top: 4px; }

/* ── 重复扣分 ── */
.repeat-list { display: flex; flex-direction: column; gap: 6px; }
.repeat-item {
  display: grid; grid-template-columns: 90px 1fr 100px; gap: 10px;
  padding: 8px 10px; background: #fffbeb; border-radius: 6px;
  border-left: 3px solid #f59e0b;
  font-size: 13px; align-items: center;
}
.repeat-tag { background: #fef3c7; color: #92400e; padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 500; text-align: center; }
.repeat-name { color: #1e293b; font-weight: 500; }
.repeat-section { color: #94a3b8; font-size: 11px; text-align: right; }

/* ── 明细 ── */
.detail-card { margin-top: 16px; }
.detail-card .hint { font-size: 11px; color: #94a3b8; font-weight: normal; }
.ml-1 { margin-left: 4px; }

.text-success { color: #22c55e; }
.text-warning { color: #f59e0b; }
.text-danger { color: #ef4444; }
</style>
