<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, computed, onMounted } from 'vue'
import {
  BarChart3, Trophy,
  Users, Calendar, ArrowUpDown, Search, Layers, UserCheck,
} from 'lucide-vue-next'
import type { InspTask, InspSubmission } from '@/types/insp/project'

// P0 #2: 不再自拉 tasks/submissions, 由父组件 (ProjectDetailView) 透传.
// 删除整套"指标得分加载+重算"死代码 (getIndicators/getGradeSchemes/getIndicatorScores/
// computeIndicatorScores/scoreMap/rootIndicators/getChildren/schemeName/handleCompute/
// lastComputedAt/computedTimeText/timeTicker/.da-compute/.da-last-computed), 模板 0 处使用.
const props = defineProps<{
  projectId: LongId
  allTasks: InspTask[]
  allSubmissions: InspSubmission[]
  submissionsLoadFailedCount?: number
}>()

const tasks = computed(() => props.allTasks)
const submissions = computed(() => props.allSubmissions)

// ═══ Filters ═══
const dateRangeType = ref<'week' | 'month' | 'all' | 'custom'>('all')
const customStart = ref('')
const customEnd = ref('')
const filterSection = ref<LongId | ''>('')
const filterInspector = ref<string>('') // P1 #6: 统一用 inspectorId 作为 key
const searchQuery = ref('')

// P2 #12: activeView 持久化到 localStorage
type ViewMode = 'ranking' | 'sections' | 'inspectors'
const VIEW_STORAGE_KEY = 'inspIndicatorScoreView'
function loadPersistedView(): ViewMode {
  try {
    const v = localStorage.getItem(VIEW_STORAGE_KEY)
    if (v === 'ranking' || v === 'sections' || v === 'inspectors') return v
  } catch { /* localStorage 不可用时 silently fall through */ }
  return 'ranking'
}
const activeView = ref<ViewMode>(loadPersistedView())
function setView(v: ViewMode) {
  activeView.value = v
  try { localStorage.setItem(VIEW_STORAGE_KEY, v) } catch { /* ignore */ }
}

// P2 #14: 用本地日期格式, 避免 toISOString().split('T')[0] 时区漂移
function fmtLocal(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

// periodRange: 'all' 时返回 null 边界, 不再用前端任务反推窗口 (P0 #3 关联).
const periodRange = computed<{ start: string | null; end: string | null }>(() => {
  const today = new Date()
  if (dateRangeType.value === 'custom' && customStart.value && customEnd.value) {
    return { start: customStart.value, end: customEnd.value }
  }
  if (dateRangeType.value === 'month') {
    return {
      start: fmtLocal(new Date(today.getFullYear(), today.getMonth(), 1)),
      end: fmtLocal(new Date(today.getFullYear(), today.getMonth() + 1, 0)),
    }
  }
  if (dateRangeType.value === 'week') {
    const d = today.getDay() || 7
    const m = new Date(today); m.setDate(today.getDate() - d + 1)
    const s = new Date(m); s.setDate(m.getDate() + 6)
    return { start: fmtLocal(m), end: fmtLocal(s) }
  }
  // all
  return { start: null, end: null }
})

function inRange(taskDate: string | null | undefined): boolean {
  if (!taskDate) return false
  const { start, end } = periodRange.value
  if (start && taskDate < start) return false
  if (end && taskDate > end) return false
  return true
}

// ═══ Derived data ═══
// P2 #9: allSections 按 sectionId 去重
const allSections = computed(() => {
  const seen = new Map<string, { id: LongId; name: string }>()
  for (const s of submissions.value) {
    if (s.sectionId == null) continue
    const key = String(s.sectionId)
    if (seen.has(key)) continue
    seen.set(key, { id: s.sectionId, name: `分区#${s.sectionId}` })
  }
  return [...seen.values()]
})

// P2 #10: allInspectors / inspectorStats 用 inspectorId 当 key, 同名异人不合并
interface InspectorOption { id: string; name: string }
const allInspectors = computed<InspectorOption[]>(() => {
  const m = new Map<string, string>()
  tasks.value.forEach(t => {
    if (t.inspectorId == null) return
    const id = String(t.inspectorId)
    if (!m.has(id)) m.set(id, t.inspectorName || `#${id}`)
  })
  return [...m.entries()].map(([id, name]) => ({ id, name }))
})

// ═══ Filtered submissions ═══
const filteredSubmissions = computed(() => {
  let subs = submissions.value.filter(s => s.status === 'COMPLETED' && s.finalScore != null)
  // Date filter (仅当存在 range 时执行)
  if (periodRange.value.start || periodRange.value.end) {
    subs = subs.filter(s => {
      const task = tasks.value.find(t => String(t.id) === String(s.taskId))
      return task && inRange(task.taskDate)
    })
  }
  // Section filter
  if (filterSection.value !== '') {
    const want = String(filterSection.value)
    subs = subs.filter(s => s.sectionId != null && String(s.sectionId) === want)
  }
  // Inspector filter (P1 #6: 仅按 inspectorId 匹配)
  if (filterInspector.value) {
    const want = filterInspector.value
    const taskIds = new Set(
      tasks.value
        .filter(t => t.inspectorId != null && String(t.inspectorId) === want)
        .map(t => String(t.id)),
    )
    subs = subs.filter(s => taskIds.has(String(s.taskId)))
  }
  return subs
})

// ═══ P1 #4: 按 sectionId (=调度组/ScoringProfile) 分桶后再平均 ═══
// 跨调度组的 finalScore 量纲可能不同 (各自 ScoringProfile), 直接均值会失真.
// 策略: 先按 sectionId 分桶计算每桶均值, 再对桶均值取平均 (等权融合);
// 达标率读 submission.passed 字段而非硬编码 ≥60.
function bucketAvg(subs: InspSubmission[]): number {
  if (!subs.length) return 0
  const buckets = new Map<string, { sum: number; n: number }>()
  for (const s of subs) {
    if (s.finalScore == null) continue
    const key = s.sectionId == null ? '__nosec__' : String(s.sectionId)
    if (!buckets.has(key)) buckets.set(key, { sum: 0, n: 0 })
    const b = buckets.get(key)!
    b.sum += s.finalScore
    b.n++
  }
  if (!buckets.size) return 0
  let totalAvg = 0
  let bucketCount = 0
  for (const b of buckets.values()) {
    if (b.n === 0) continue
    totalAvg += b.sum / b.n
    bucketCount++
  }
  return bucketCount > 0 ? totalAvg / bucketCount : 0
}
function passRateFromPassed(subs: InspSubmission[]): number {
  // 只计算 passed 有值的样本; 全为 null 时返回 0.
  const known = subs.filter(s => s.passed !== null && s.passed !== undefined)
  if (!known.length) return 0
  const passed = known.filter(s => s.passed === true).length
  return Math.round(passed / known.length * 100)
}

// ═══ Ranking view ═══
const sortField = ref<'score' | 'name'>('score')
const sortDir = ref<'asc' | 'desc'>('desc')
function toggleSort(f: 'score' | 'name') {
  if (sortField.value === f) sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  else { sortField.value = f; sortDir.value = f === 'score' ? 'desc' : 'asc' }
}

interface TargetRow {
  targetId: LongId
  targetName: string
  count: number
  avg: number
  max: number
  min: number
  sections: Map<string, { sum: number; count: number; sectionId: LongId }>
}

const targetRows = computed<TargetRow[]>(() => {
  const map = new Map<string, TargetRow>()
  // collect per-target per-section
  for (const s of filteredSubmissions.value) {
    const tid = String(s.targetId)
    if (!map.has(tid)) {
      map.set(tid, {
        targetId: s.targetId,
        targetName: s.targetName || `#${tid}`,
        count: 0,
        avg: 0,
        max: -Infinity,
        min: Infinity,
        sections: new Map(),
      })
    }
    const row = map.get(tid)!
    const score = s.finalScore!
    row.count++
    if (score > row.max) row.max = score
    if (score < row.min) row.min = score
    if (s.sectionId == null) continue
    const secKey = String(s.sectionId)
    if (!row.sections.has(secKey)) {
      row.sections.set(secKey, { sum: 0, count: 0, sectionId: s.sectionId })
    }
    const sec = row.sections.get(secKey)!
    sec.sum += score
    sec.count++
  }
  // P1 #4: row.avg = 按 section 桶等权融合
  for (const row of map.values()) {
    let avgSum = 0
    let bucketN = 0
    for (const sec of row.sections.values()) {
      if (sec.count === 0) continue
      avgSum += sec.sum / sec.count
      bucketN++
    }
    if (bucketN > 0) {
      row.avg = Math.round(avgSum / bucketN * 10) / 10
    } else if (row.count > 0) {
      // 没有 section 的目标 — 用 finalScore 直接均值兜底 (数据残缺场景)
      const fallback = filteredSubmissions.value
        .filter(s => String(s.targetId) === String(row.targetId))
        .reduce((acc, s) => acc + (s.finalScore ?? 0), 0) / row.count
      row.avg = Math.round(fallback * 10) / 10
    }
    if (row.max === -Infinity) row.max = 0
    if (row.min === Infinity) row.min = 0
  }
  let rows = [...map.values()]
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    rows = rows.filter(r => r.targetName.toLowerCase().includes(q))
  }
  rows.sort((a, b) => sortField.value === 'name'
    ? (sortDir.value === 'asc' ? a.targetName.localeCompare(b.targetName) : b.targetName.localeCompare(a.targetName))
    : (sortDir.value === 'asc' ? a.avg - b.avg : b.avg - a.avg))
  return rows
})

// ═══ Section analysis view ═══
interface SectionStat {
  sectionId: LongId
  sectionName: string
  totalScore: number
  count: number
  avg: number
  max: number
  min: number
  targets: number
}
const sectionStats = computed<SectionStat[]>(() => {
  const map = new Map<string, SectionStat>()
  const targetSets = new Map<string, Set<string>>()
  for (const s of filteredSubmissions.value) {
    if (s.sectionId == null) continue
    const key = String(s.sectionId)
    if (!map.has(key)) {
      map.set(key, {
        sectionId: s.sectionId,
        sectionName: `分区#${s.sectionId}`,
        totalScore: 0,
        count: 0,
        avg: 0,
        max: -Infinity,
        min: Infinity,
        targets: 0,
      })
      targetSets.set(key, new Set())
    }
    const stat = map.get(key)!
    const score = s.finalScore!
    stat.totalScore += score
    stat.count++
    if (score > stat.max) stat.max = score
    if (score < stat.min) stat.min = score
    targetSets.get(key)!.add(String(s.targetId))
  }
  for (const [key, stat] of map) {
    stat.avg = stat.count > 0 ? Math.round(stat.totalScore / stat.count * 10) / 10 : 0
    stat.targets = targetSets.get(key)!.size
    if (stat.max === -Infinity) stat.max = 0
    if (stat.min === Infinity) stat.min = 0
  }
  return [...map.values()].sort((a, b) => b.avg - a.avg)
})

// ═══ Inspector analysis view (P2 #10: 用 inspectorId 当 key) ═══
interface InspectorStat {
  id: string
  name: string
  taskCount: number
  submissionCount: number
  avgScore: number
  totalScore: number
  targets: number
}
const inspectorStats = computed<InspectorStat[]>(() => {
  const map = new Map<string, InspectorStat>()
  for (const t of tasks.value) {
    if (t.inspectorId == null) continue
    if (!inRange(t.taskDate)) continue
    const id = String(t.inspectorId)
    if (!map.has(id)) {
      map.set(id, {
        id,
        name: t.inspectorName || `#${id}`,
        taskCount: 0,
        submissionCount: 0,
        avgScore: 0,
        totalScore: 0,
        targets: 0,
      })
    }
    map.get(id)!.taskCount++
  }
  const targetSets = new Map<string, Set<string>>()
  for (const s of filteredSubmissions.value) {
    const task = tasks.value.find(t => String(t.id) === String(s.taskId))
    if (!task || task.inspectorId == null) continue
    const id = String(task.inspectorId)
    if (!map.has(id)) {
      map.set(id, {
        id,
        name: task.inspectorName || `#${id}`,
        taskCount: 0,
        submissionCount: 0,
        avgScore: 0,
        totalScore: 0,
        targets: 0,
      })
    }
    const stat = map.get(id)!
    stat.submissionCount++
    stat.totalScore += s.finalScore!
    if (!targetSets.has(id)) targetSets.set(id, new Set())
    targetSets.get(id)!.add(String(s.targetId))
  }
  for (const [id, stat] of map) {
    stat.avgScore = stat.submissionCount > 0 ? Math.round(stat.totalScore / stat.submissionCount * 10) / 10 : 0
    stat.targets = targetSets.get(id)?.size || 0
  }
  // P2 #11: inspectors 视图也响应 searchQuery
  let rows = [...map.values()]
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    rows = rows.filter(r => r.name.toLowerCase().includes(q))
  }
  return rows.sort((a, b) => b.submissionCount - a.submissionCount)
})

// ═══ Global stats (P1 #4: 用 bucketAvg / passed) ═══
const globalStats = computed(() => {
  const subs = filteredSubmissions.value
  if (!subs.length) return null
  const scores = subs.map(s => s.finalScore!)
  const targets = new Set(subs.map(s => String(s.targetId))).size
  const sections = new Set(
    subs.filter(s => s.sectionId != null).map(s => String(s.sectionId)),
  ).size
  return {
    totalChecks: subs.length,
    targets,
    sections,
    avg: bucketAvg(subs).toFixed(1),
    max: Math.max(...scores).toFixed(1),
    min: Math.min(...scores).toFixed(1),
    passRate: passRateFromPassed(subs),
  }
})

// P2 #16: 统一用 --insp-* 语义 token, 不再硬编码多套色板
function scoreColor(s: number | null) {
  if (s == null) return 'var(--insp-ink-quaternary)'
  if (s >= 90) return 'var(--insp-pass)'
  if (s >= 75) return 'var(--insp-info)'
  if (s >= 60) return 'var(--insp-warn)'
  return 'var(--insp-fail)'
}

// P2 #13: range bar 把 min/max 归一化到 0-100 (满分 100 假设)
function rangeStyle(min: number, max: number): Record<string, string> {
  const clamp = (n: number) => Math.max(0, Math.min(100, n))
  return {
    '--min': clamp(min) + '%',
    '--max': clamp(max) + '%',
  }
}

onMounted(() => {
  // 不再加载任何数据 — 全部由 props 透传 (P0 #2)
})
</script>

<template>
  <div class="da">

    <!-- ═══ Filter bar ═══ -->
    <div class="da-filters">
      <div class="da-filter">
        <Calendar class="w-3 h-3" />
        <select v-model="dateRangeType" class="da-select">
          <option value="all">全部时间</option>
          <option value="week">本周</option>
          <option value="month">本月</option>
          <option value="custom">自定义</option>
        </select>
      </div>
      <template v-if="dateRangeType === 'custom'">
        <input type="date" v-model="customStart" class="da-date" />
        <span class="da-sep">~</span>
        <input type="date" v-model="customEnd" class="da-date" />
      </template>
      <div class="da-filter">
        <Layers class="w-3 h-3" />
        <select v-model="filterSection" class="da-select">
          <option value="">全部分区</option>
          <option v-for="s in allSections" :key="String(s.id)" :value="s.id">{{ s.name }}</option>
        </select>
      </div>
      <div class="da-filter">
        <UserCheck class="w-3 h-3" />
        <select v-model="filterInspector" class="da-select">
          <option value="">全部检查员</option>
          <option v-for="i in allInspectors" :key="i.id" :value="i.id">{{ i.name }}</option>
        </select>
      </div>
      <div class="da-search">
        <Search class="w-3 h-3" />
        <input v-model="searchQuery" placeholder="搜索..." class="da-search-input" />
      </div>
    </div>

    <!-- P1 #5: 父组件加载状态明示 (props 化后子组件不再自维护) -->
    <div v-if="(props.submissionsLoadFailedCount ?? 0) > 0" class="da-warn">
      ! {{ props.submissionsLoadFailedCount }} 个任务的提交记录加载失败, 统计可能不完整
    </div>

    <!-- ═══ Stats strip ═══ -->
    <div v-if="globalStats" class="da-stats">
      <div class="da-stat"><span class="da-stat-v">{{ globalStats.totalChecks }}</span><span class="da-stat-l">检查次数</span></div>
      <div class="da-stat-sep" />
      <div class="da-stat"><span class="da-stat-v">{{ globalStats.targets }}</span><span class="da-stat-l">目标数</span></div>
      <div class="da-stat-sep" />
      <div class="da-stat"><span class="da-stat-v" style="color:var(--insp-info)">{{ globalStats.avg }}</span><span class="da-stat-l">桶均(分调度组)</span></div>
      <div class="da-stat-sep" />
      <div class="da-stat"><span class="da-stat-v" style="color:var(--insp-pass)">{{ globalStats.max }}</span><span class="da-stat-l">最高</span></div>
      <div class="da-stat-sep" />
      <div class="da-stat"><span class="da-stat-v" style="color:var(--insp-fail)">{{ globalStats.min }}</span><span class="da-stat-l">最低</span></div>
      <div class="da-stat-sep" />
      <div class="da-stat"><span class="da-stat-v" style="color:var(--insp-pass)">{{ globalStats.passRate }}%</span><span class="da-stat-l">达标率</span></div>
      <div class="da-stat-sep" />
      <div class="da-stat"><span class="da-stat-v">{{ globalStats.sections }}</span><span class="da-stat-l">分区数</span></div>
    </div>

    <!-- ═══ View tabs ═══ -->
    <div class="da-tabs">
      <button class="da-tab" :class="{ on: activeView === 'ranking' }" @click="setView('ranking')">
        <Trophy class="w-3.5 h-3.5" /> 目标排名
      </button>
      <button class="da-tab" :class="{ on: activeView === 'sections' }" @click="setView('sections')">
        <Layers class="w-3.5 h-3.5" /> 分区分析
      </button>
      <button class="da-tab" :class="{ on: activeView === 'inspectors' }" @click="setView('inspectors')">
        <Users class="w-3.5 h-3.5" /> 检查员分析
      </button>
      <span class="da-tab-count">{{ filteredSubmissions.length }} 条数据</span>
    </div>

    <!-- ═══ View: Ranking ═══ -->
    <div v-if="activeView === 'ranking'" class="da-view">
      <div v-if="targetRows.length" class="da-table">
        <div class="da-thead">
          <div class="da-th da-th-rank">#</div>
          <div class="da-th da-th-name" @click="toggleSort('name')">目标 <ArrowUpDown class="w-3 h-3" :style="{ opacity: sortField === 'name' ? 1 : 0.2 }" /></div>
          <div v-for="sec in allSections" :key="String(sec.id)" class="da-th da-th-sec">{{ sec.name }}</div>
          <div class="da-th da-th-avg" @click="toggleSort('score')">桶均 <ArrowUpDown class="w-3 h-3" :style="{ opacity: sortField === 'score' ? 1 : 0.2 }" /></div>
          <div class="da-th da-th-count">次数</div>
          <div class="da-th da-th-range">范围</div>
        </div>
        <div v-for="(row, idx) in targetRows" :key="String(row.targetId)" class="da-tr" :class="{ top: idx < 3 }">
          <div class="da-td da-td-rank">
            <span v-if="idx === 0 && sortDir === 'desc' && sortField === 'score'" class="da-medal gold">1</span>
            <span v-else-if="idx === 1 && sortDir === 'desc' && sortField === 'score'" class="da-medal silver">2</span>
            <span v-else-if="idx === 2 && sortDir === 'desc' && sortField === 'score'" class="da-medal bronze">3</span>
            <span v-else class="da-rank-n">{{ idx + 1 }}</span>
          </div>
          <div class="da-td da-td-name">{{ row.targetName }}</div>
          <div v-for="sec in allSections" :key="String(sec.id)" class="da-td da-td-sec">
            <template v-if="row.sections.has(String(sec.id))">
              <span class="da-sec-score" :style="{ color: scoreColor(row.sections.get(String(sec.id))!.sum / row.sections.get(String(sec.id))!.count) }">
                {{ (row.sections.get(String(sec.id))!.sum / row.sections.get(String(sec.id))!.count).toFixed(1) }}
              </span>
            </template>
            <span v-else class="da-empty">—</span>
          </div>
          <div class="da-td da-td-avg"><span class="da-avg-num" :style="{ color: scoreColor(row.avg) }">{{ row.avg }}</span></div>
          <div class="da-td da-td-count">{{ row.count }}</div>
          <div class="da-td da-td-range"><span class="da-range-bar" :style="rangeStyle(row.min, row.max)" /><span class="da-range-text">{{ row.min.toFixed(0) }}-{{ row.max.toFixed(0) }}</span></div>
        </div>
      </div>
      <div v-else class="da-no-data">暂无数据</div>
    </div>

    <!-- ═══ View: Sections ═══ -->
    <div v-if="activeView === 'sections'" class="da-view">
      <div v-if="sectionStats.length" class="da-section-grid">
        <div v-for="sec in sectionStats" :key="String(sec.sectionId)" class="da-sec-card">
          <div class="da-sec-head">
            <span class="da-sec-name">{{ sec.sectionName }}</span>
            <span class="da-sec-avg" :style="{ color: scoreColor(sec.avg) }">{{ sec.avg }}</span>
          </div>
          <div class="da-sec-bar-wrap">
            <div class="da-sec-bar" :style="{ width: Math.min(sec.avg, 100) + '%', background: scoreColor(sec.avg) }" />
          </div>
          <div class="da-sec-meta">
            <span>{{ sec.count }}次检查</span>
            <span>{{ sec.targets }}个目标</span>
            <span>最高 {{ sec.max.toFixed(0) }}</span>
            <span>最低 {{ sec.min.toFixed(0) }}</span>
          </div>
        </div>
      </div>
      <div v-else class="da-no-data">暂无分区数据</div>
    </div>

    <!-- ═══ View: Inspectors ═══ -->
    <div v-if="activeView === 'inspectors'" class="da-view">
      <div v-if="inspectorStats.length" class="da-table">
        <div class="da-thead">
          <div class="da-th" style="flex:2">检查员</div>
          <div class="da-th" style="flex:1">任务数</div>
          <div class="da-th" style="flex:1">检查次数</div>
          <div class="da-th" style="flex:1">覆盖目标</div>
          <div class="da-th" style="flex:1">平均分</div>
        </div>
        <div v-for="insp in inspectorStats" :key="insp.id" class="da-tr">
          <div class="da-td" style="flex:2; font-weight:600; color:#1e1b4b">{{ insp.name }}</div>
          <div class="da-td" style="flex:1">{{ insp.taskCount }}</div>
          <div class="da-td" style="flex:1">{{ insp.submissionCount }}</div>
          <div class="da-td" style="flex:1">{{ insp.targets }}</div>
          <div class="da-td" style="flex:1"><span class="da-avg-num" :style="{ color: scoreColor(insp.avgScore) }">{{ insp.avgScore || '—' }}</span></div>
        </div>
      </div>
      <div v-else class="da-no-data">暂无检查员数据</div>
    </div>

    <!-- ═══ Empty ═══ -->
    <div v-if="!filteredSubmissions.length && !globalStats" class="da-empty">
      <BarChart3 class="w-10 h-10" style="color:#e5e7eb" />
      <div class="da-empty-t">暂无检查数据</div>
      <div class="da-empty-d">完成检查任务后这里将展示成绩分析</div>
    </div>
  </div>
</template>

<style scoped>
.da { min-height: 200px; }

/* ═══ Filters ═══ */
.da-filters {
  display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
  padding: 8px 10px; background: #fff; border: 1px solid #e5e7eb;
  border-radius: 10px; margin-bottom: 10px;
}
.da-filter {
  display: flex; align-items: center; gap: 4px; color: #9ca3af;
}
.da-select {
  padding: 4px 6px; border: 1px solid #e5e7eb; border-radius: 6px;
  font-size: 12px; color: #374151; outline: none; background: #fff;
}
.da-select:focus { border-color: var(--insp-accent); }
.da-date { padding: 3px 6px; border: 1px solid #e5e7eb; border-radius: 6px; font-size: 11px; width: 100px; }
.da-sep { color: #d1d5db; font-size: 11px; }
.da-search {
  display: flex; align-items: center; gap: 4px; padding: 4px 8px;
  border: 1px solid #e5e7eb; border-radius: 6px; color: #9ca3af; flex: 1; max-width: 180px;
}
.da-search-input { border: none; outline: none; font-size: 12px; width: 100%; color: #374151; }
.da-search-input::placeholder { color: #d1d5db; }

/* P1 #5: 父级加载失败提示 */
.da-warn {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 10px; margin-bottom: 8px;
  background: var(--insp-warn-paler, #fef3c7); color: var(--insp-warn, #b45309);
  border: 1px solid var(--insp-warn-pale, #fde68a); border-radius: 8px;
  font-size: 12px;
}

/* ═══ Stats ═══ */
.da-stats {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 14px; background: #fff; border: 1px solid #e5e7eb;
  border-radius: 10px; margin-bottom: 10px;
}
.da-stat { display: flex; align-items: baseline; gap: 4px; }
.da-stat-v { font-size: 16px; font-weight: 800; color: #1e1b4b; }
.da-stat-l { font-size: 10px; color: #9ca3af; }
.da-stat-sep { width: 1px; height: 18px; background: #f0f0f3; }

/* ═══ Tabs ═══ */
.da-tabs {
  display: flex; align-items: center; gap: 2px;
  margin-bottom: 10px;
}
.da-tab {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 14px; border-radius: 8px; font-size: 12px; font-weight: 600;
  color: #6b7280; background: none; border: none; cursor: pointer; transition: all 0.15s;
}
.da-tab:hover { background: #f3f4f6; }
.da-tab.on { background: var(--insp-accent); color: #fff; }
.da-tab-count { font-size: 11px; color: #9ca3af; margin-left: auto; }

/* ═══ Table (P1 #7: 横向溢出可滚动) ═══ */
.da-table {
  background: #fff; border: 1px solid #e5e7eb; border-radius: 10px;
  overflow: auto; max-width: 100%;
}
.da-thead {
  display: flex; padding: 7px 12px; background: #fafbfc; border-bottom: 1px solid #f0f0f3;
  font-size: 10px; font-weight: 700; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.3px;
  min-width: max-content;
}
.da-tr {
  display: flex; padding: 7px 12px; border-bottom: 1px solid #f9fafb;
  align-items: center; transition: background 0.1s; font-size: 12px;
  min-width: max-content;
}
.da-tr:last-child { border-bottom: none; }
.da-tr:hover { background: #fafbff; }
.da-tr.top { background: #fafbfe; }

.da-th, .da-td { display: flex; align-items: center; gap: 3px; cursor: default; }
.da-th-rank, .da-td-rank { width: 32px; justify-content: center; flex-shrink: 0; }
.da-th-name, .da-td-name { flex: 1.5; min-width: 120px; font-weight: 600; color: #1e1b4b; cursor: pointer; }
.da-th-sec, .da-td-sec { flex: 0.8; min-width: 80px; justify-content: center; }
.da-th-avg, .da-td-avg { width: 60px; justify-content: center; flex-shrink: 0; cursor: pointer; }
.da-th-count, .da-td-count { width: 50px; justify-content: center; flex-shrink: 0; color: #9ca3af; }
.da-th-range, .da-td-range { width: 90px; flex-shrink: 0; flex-direction: column; align-items: stretch; gap: 2px; }

.da-medal { width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 9px; font-weight: 800; color: #fff; }
.da-medal.gold { background: linear-gradient(135deg, #fbbf24, #f59e0b); }
.da-medal.silver { background: linear-gradient(135deg, #d1d5db, #9ca3af); }
.da-medal.bronze { background: linear-gradient(135deg, #fdba74, #ea580c); }
.da-rank-n { font-size: 10px; color: #d1d5db; font-weight: 600; }

.da-sec-score { font-size: 12px; font-weight: 700; }
.da-avg-num { font-size: 14px; font-weight: 800; }
.da-empty { color: #e5e7eb; }

.da-range-bar {
  height: 3px; background: #f0f0f3; border-radius: 2px; position: relative; width: 100%;
}
.da-range-bar::after {
  content: ''; position: absolute; top: 0; height: 100%; border-radius: 2px;
  left: var(--min, 0%); right: calc(100% - var(--max, 100%));
  background: var(--insp-accent);
}
.da-range-text { font-size: 9px; color: #9ca3af; text-align: center; }

/* ═══ Section cards ═══ */
.da-section-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
.da-sec-card {
  background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 12px 14px;
}
.da-sec-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.da-sec-name { font-size: 13px; font-weight: 700; color: #1e1b4b; }
.da-sec-avg { font-size: 18px; font-weight: 800; }
.da-sec-bar-wrap { height: 4px; background: #f3f4f6; border-radius: 2px; overflow: hidden; margin-bottom: 6px; }
.da-sec-bar { height: 100%; border-radius: 2px; transition: width 0.3s; }
.da-sec-meta { display: flex; gap: 8px; font-size: 10px; color: #9ca3af; }

/* ═══ No data / Empty ═══ */
.da-no-data { padding: 40px; text-align: center; font-size: 13px; color: #d1d5db; background: #fff; border: 1px solid #f0f0f3; border-radius: 10px; }
.da-empty { display: flex; flex-direction: column; align-items: center; padding: 60px 20px; }
.da-empty-t { font-size: 14px; font-weight: 600; color: #6b7280; margin-top: 8px; }
.da-empty-d { font-size: 12px; color: #9ca3af; margin-top: 4px; }
.da-view { min-height: 100px; }

@media (max-width: 768px) {
  .da-section-grid { grid-template-columns: 1fr; }
  .da-stats { flex-wrap: wrap; }
}
</style>
