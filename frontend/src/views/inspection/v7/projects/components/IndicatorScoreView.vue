<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  BarChart3, RefreshCw, Trophy,
  Users, Calendar, ArrowUpDown, Search, Layers, UserCheck,
} from 'lucide-vue-next'
import { getIndicators, getIndicatorScores, computeIndicatorScores } from '@/api/insp/indicator'
import { getGradeSchemes } from '@/api/insp/gradeScheme'
import { getSubmissions } from '@/api/insp/submission'
import { getTasks } from '@/api/insp/task'
import type { Indicator, IndicatorScore } from '@/types/insp/indicator'
import type { GradeScheme } from '@/types/insp/gradeScheme'
import type { InspTask, InspSubmission } from '@/types/insp/project'

const props = defineProps<{ projectId: number }>()

const loading = ref(false)
const computing = ref(false)
const indicators = ref<Indicator[]>([])
const gradeSchemes = ref<GradeScheme[]>([])
const scoreMap = ref<Map<number, IndicatorScore[]>>(new Map())
const tasks = ref<InspTask[]>([])
const submissions = ref<InspSubmission[]>([])

// ═══ Filters ═══
const dateRangeType = ref<'week' | 'month' | 'all' | 'custom'>('all')
const customStart = ref(''); const customEnd = ref('')
const filterSection = ref<number | ''>('')
const filterInspector = ref<string>('')
const searchQuery = ref('')
const activeView = ref<'ranking' | 'sections' | 'inspectors'>('ranking')

const periodRange = computed(() => {
  const today = new Date()
  const fmt = (d: Date) => d.toISOString().split('T')[0]
  if (dateRangeType.value === 'custom' && customStart.value && customEnd.value) return { start: customStart.value, end: customEnd.value }
  if (dateRangeType.value === 'month') return { start: fmt(new Date(today.getFullYear(), today.getMonth(), 1)), end: fmt(new Date(today.getFullYear(), today.getMonth() + 1, 0)) }
  if (dateRangeType.value === 'week') {
    const d = today.getDay() || 7; const m = new Date(today); m.setDate(today.getDate() - d + 1); const s = new Date(m); s.setDate(m.getDate() + 6)
    return { start: fmt(m), end: fmt(s) }
  }
  // all
  const dates = tasks.value.map(t => t.taskDate).filter(Boolean).sort()
  return { start: dates[0] || fmt(today), end: dates[dates.length - 1] || fmt(today) }
})

// ═══ Derived data ═══
const rootIndicators = computed(() => indicators.value.filter(i => !i.parentIndicatorId).sort((a, b) => a.sortOrder - b.sortOrder))
function getChildren(pid: number) { return indicators.value.filter(i => Number(i.parentIndicatorId) === pid).sort((a, b) => a.sortOrder - b.sortOrder) }
function schemeName(id: number | null) { return id ? (gradeSchemes.value.find(s => s.id === id)?.displayName || '') : '' }

// All sections from indicators
const allSections = computed(() => indicators.value.filter(i => i.sourceSectionId).map(i => ({ id: Number(i.sourceSectionId), name: i.name })))
// All inspectors from tasks
const allInspectors = computed(() => {
  const m = new Map<string, string>()
  tasks.value.forEach(t => { if (t.inspectorName) m.set(String(t.inspectorId || t.inspectorName), t.inspectorName) })
  return [...m.entries()].map(([id, name]) => ({ id, name }))
})

// Filtered submissions
const filteredSubmissions = computed(() => {
  let subs = submissions.value.filter(s => s.status === 'COMPLETED' && s.finalScore != null)
  // Date filter
  const range = periodRange.value
  subs = subs.filter(s => {
    const task = tasks.value.find(t => String(t.id) === String(s.taskId))
    return task && task.taskDate >= range.start && task.taskDate <= range.end
  })
  // Section filter
  if (filterSection.value) subs = subs.filter(s => Number(s.sectionId) === Number(filterSection.value))
  // Inspector filter
  if (filterInspector.value) {
    const taskIds = new Set(tasks.value.filter(t => String(t.inspectorId) === filterInspector.value || t.inspectorName === filterInspector.value).map(t => String(t.id)))
    subs = subs.filter(s => taskIds.has(String(s.taskId)))
  }
  return subs
})

// ═══ Ranking view ═══
const sortField = ref<'score' | 'name'>('score')
const sortDir = ref<'asc' | 'desc'>('desc')
function toggleSort(f: 'score' | 'name') {
  if (sortField.value === f) sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  else { sortField.value = f; sortDir.value = f === 'score' ? 'desc' : 'asc' }
}

interface TargetRow { targetId: number; targetName: string; score: number; count: number; avg: number; max: number; min: number; sections: Map<number, { score: number; count: number }> }

const targetRows = computed<TargetRow[]>(() => {
  const map = new Map<number, TargetRow>()
  for (const s of filteredSubmissions.value) {
    const tid = Number(s.targetId)
    if (!map.has(tid)) map.set(tid, { targetId: tid, targetName: s.targetName || `#${tid}`, score: 0, count: 0, avg: 0, max: -Infinity, min: Infinity, sections: new Map() })
    const row = map.get(tid)!
    const score = s.finalScore!
    row.score += score; row.count++
    if (score > row.max) row.max = score
    if (score < row.min) row.min = score
    // Section breakdown
    const secId = Number(s.sectionId)
    if (!row.sections.has(secId)) row.sections.set(secId, { score: 0, count: 0 })
    const sec = row.sections.get(secId)!; sec.score += score; sec.count++
  }
  for (const row of map.values()) {
    row.avg = row.count > 0 ? Math.round(row.score / row.count * 10) / 10 : 0
    if (row.max === -Infinity) row.max = 0
    if (row.min === Infinity) row.min = 0
  }
  let rows = [...map.values()]
  if (searchQuery.value.trim()) { const q = searchQuery.value.toLowerCase(); rows = rows.filter(r => r.targetName.toLowerCase().includes(q)) }
  rows.sort((a, b) => sortField.value === 'name' ? (sortDir.value === 'asc' ? a.targetName.localeCompare(b.targetName) : b.targetName.localeCompare(a.targetName)) : (sortDir.value === 'asc' ? a.avg - b.avg : b.avg - a.avg))
  return rows
})

// ═══ Section analysis view ═══
interface SectionStat { sectionId: number; sectionName: string; totalScore: number; count: number; avg: number; max: number; min: number; targets: number }
const sectionStats = computed<SectionStat[]>(() => {
  const map = new Map<number, SectionStat>()
  const targetSets = new Map<number, Set<number>>()
  for (const s of filteredSubmissions.value) {
    const secId = Number(s.sectionId)
    const secName = allSections.value.find(x => x.id === secId)?.name || `分区#${secId}`
    if (!map.has(secId)) { map.set(secId, { sectionId: secId, sectionName: secName, totalScore: 0, count: 0, avg: 0, max: -Infinity, min: Infinity, targets: 0 }); targetSets.set(secId, new Set()) }
    const stat = map.get(secId)!; const score = s.finalScore!
    stat.totalScore += score; stat.count++
    if (score > stat.max) stat.max = score; if (score < stat.min) stat.min = score
    targetSets.get(secId)!.add(Number(s.targetId))
  }
  for (const [secId, stat] of map) { stat.avg = stat.count > 0 ? Math.round(stat.totalScore / stat.count * 10) / 10 : 0; stat.targets = targetSets.get(secId)!.size; if (stat.max === -Infinity) stat.max = 0; if (stat.min === Infinity) stat.min = 0 }
  return [...map.values()].sort((a, b) => b.avg - a.avg)
})

// ═══ Inspector analysis view ═══
interface InspectorStat { name: string; taskCount: number; submissionCount: number; avgScore: number; totalScore: number; targets: number }
const inspectorStats = computed<InspectorStat[]>(() => {
  const map = new Map<string, InspectorStat>()
  for (const t of tasks.value) {
    if (!t.inspectorName) continue
    const range = periodRange.value
    if (t.taskDate < range.start || t.taskDate > range.end) continue
    const name = t.inspectorName
    if (!map.has(name)) map.set(name, { name, taskCount: 0, submissionCount: 0, avgScore: 0, totalScore: 0, targets: 0 })
    map.get(name)!.taskCount++
  }
  const targetSets = new Map<string, Set<number>>()
  for (const s of filteredSubmissions.value) {
    const task = tasks.value.find(t => String(t.id) === String(s.taskId))
    if (!task?.inspectorName) continue
    const name = task.inspectorName
    if (!map.has(name)) map.set(name, { name, taskCount: 0, submissionCount: 0, avgScore: 0, totalScore: 0, targets: 0 })
    const stat = map.get(name)!; stat.submissionCount++; stat.totalScore += s.finalScore!
    if (!targetSets.has(name)) targetSets.set(name, new Set())
    targetSets.get(name)!.add(Number(s.targetId))
  }
  for (const [name, stat] of map) { stat.avgScore = stat.submissionCount > 0 ? Math.round(stat.totalScore / stat.submissionCount * 10) / 10 : 0; stat.targets = targetSets.get(name)?.size || 0 }
  return [...map.values()].sort((a, b) => b.submissionCount - a.submissionCount)
})

// ═══ Global stats ═══
const globalStats = computed(() => {
  const subs = filteredSubmissions.value
  if (!subs.length) return null
  const scores = subs.map(s => s.finalScore!)
  const avg = scores.reduce((a, b) => a + b, 0) / scores.length
  const targets = new Set(subs.map(s => s.targetId)).size
  const sections = new Set(subs.map(s => s.sectionId)).size
  return {
    totalChecks: subs.length, targets, sections,
    avg: avg.toFixed(1), max: Math.max(...scores).toFixed(1), min: Math.min(...scores).toFixed(1),
    passRate: Math.round(scores.filter(s => s >= 60).length / scores.length * 100),
  }
})

function scoreColor(s: number | null) { if (s == null) return '#d1d5db'; if (s >= 90) return '#16a34a'; if (s >= 75) return '#2563eb'; if (s >= 60) return '#d97706'; return '#ef4444' }

// ═══ Load ═══
async function loadData() {
  loading.value = true
  try {
    const [ind, gs, tks] = await Promise.all([getIndicators(props.projectId), getGradeSchemes(), getTasks({ projectId: props.projectId })])
    indicators.value = ind; gradeSchemes.value = gs; tasks.value = tks
    const allSubs: InspSubmission[] = []
    for (const t of tks) { try { allSubs.push(...await getSubmissions({ taskId: t.id })) } catch {} }
    submissions.value = allSubs
    await loadScores()
  } catch (e: any) { ElMessage.error(e.message || '加载失败') }
  finally { loading.value = false }
}
async function loadScores() {
  const r = periodRange.value; const m = new Map<number, IndicatorScore[]>()
  await Promise.all(indicators.value.map(async ind => { try { m.set(ind.id, await getIndicatorScores(ind.id, r.start, r.end)) } catch { m.set(ind.id, []) } }))
  scoreMap.value = m
}
watch(periodRange, () => { if (indicators.value.length) loadScores() })
onMounted(loadData)

async function handleCompute() {
  computing.value = true
  try { await computeIndicatorScores(props.projectId, periodRange.value.start, periodRange.value.end); ElMessage.success('计算完成'); await loadScores() }
  catch (e: any) { ElMessage.error(e.message || '计算失败') }
  finally { computing.value = false }
}
</script>

<template>
  <div class="da" v-loading="loading">

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
          <option v-for="s in allSections" :key="s.id" :value="s.id">{{ s.name }}</option>
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
        <input v-model="searchQuery" placeholder="搜索目标..." class="da-search-input" />
      </div>
      <button class="da-compute" :disabled="computing" @click="handleCompute">
        <RefreshCw class="w-3 h-3" :class="{ 'animate-spin': computing }" /> 重算
      </button>
    </div>

    <!-- ═══ Stats strip ═══ -->
    <div v-if="globalStats" class="da-stats">
      <div class="da-stat"><span class="da-stat-v">{{ globalStats.totalChecks }}</span><span class="da-stat-l">检查次数</span></div>
      <div class="da-stat-sep" />
      <div class="da-stat"><span class="da-stat-v">{{ globalStats.targets }}</span><span class="da-stat-l">目标数</span></div>
      <div class="da-stat-sep" />
      <div class="da-stat"><span class="da-stat-v" style="color:#2563eb">{{ globalStats.avg }}</span><span class="da-stat-l">平均分</span></div>
      <div class="da-stat-sep" />
      <div class="da-stat"><span class="da-stat-v" style="color:#16a34a">{{ globalStats.max }}</span><span class="da-stat-l">最高</span></div>
      <div class="da-stat-sep" />
      <div class="da-stat"><span class="da-stat-v" style="color:#ef4444">{{ globalStats.min }}</span><span class="da-stat-l">最低</span></div>
      <div class="da-stat-sep" />
      <div class="da-stat"><span class="da-stat-v" style="color:#0d9488">{{ globalStats.passRate }}%</span><span class="da-stat-l">达标率</span></div>
      <div class="da-stat-sep" />
      <div class="da-stat"><span class="da-stat-v">{{ globalStats.sections }}</span><span class="da-stat-l">分区数</span></div>
    </div>

    <!-- ═══ View tabs ═══ -->
    <div class="da-tabs">
      <button class="da-tab" :class="{ on: activeView === 'ranking' }" @click="activeView = 'ranking'">
        <Trophy class="w-3.5 h-3.5" /> 目标排名
      </button>
      <button class="da-tab" :class="{ on: activeView === 'sections' }" @click="activeView = 'sections'">
        <Layers class="w-3.5 h-3.5" /> 分区分析
      </button>
      <button class="da-tab" :class="{ on: activeView === 'inspectors' }" @click="activeView = 'inspectors'">
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
          <div v-for="sec in allSections" :key="sec.id" class="da-th da-th-sec">{{ sec.name }}</div>
          <div class="da-th da-th-avg" @click="toggleSort('score')">均分 <ArrowUpDown class="w-3 h-3" :style="{ opacity: sortField === 'score' ? 1 : 0.2 }" /></div>
          <div class="da-th da-th-count">次数</div>
          <div class="da-th da-th-range">范围</div>
        </div>
        <div v-for="(row, idx) in targetRows" :key="row.targetId" class="da-tr" :class="{ top: idx < 3 }">
          <div class="da-td da-td-rank">
            <span v-if="idx === 0 && sortDir === 'desc' && sortField === 'score'" class="da-medal gold">1</span>
            <span v-else-if="idx === 1 && sortDir === 'desc' && sortField === 'score'" class="da-medal silver">2</span>
            <span v-else-if="idx === 2 && sortDir === 'desc' && sortField === 'score'" class="da-medal bronze">3</span>
            <span v-else class="da-rank-n">{{ idx + 1 }}</span>
          </div>
          <div class="da-td da-td-name">{{ row.targetName }}</div>
          <div v-for="sec in allSections" :key="sec.id" class="da-td da-td-sec">
            <template v-if="row.sections.has(sec.id)">
              <span class="da-sec-score" :style="{ color: scoreColor(row.sections.get(sec.id)!.score / row.sections.get(sec.id)!.count) }">
                {{ (row.sections.get(sec.id)!.score / row.sections.get(sec.id)!.count).toFixed(1) }}
              </span>
            </template>
            <span v-else class="da-empty">—</span>
          </div>
          <div class="da-td da-td-avg"><span class="da-avg-num" :style="{ color: scoreColor(row.avg) }">{{ row.avg }}</span></div>
          <div class="da-td da-td-count">{{ row.count }}</div>
          <div class="da-td da-td-range"><span class="da-range-bar" :style="{ '--min': row.min + '%', '--max': row.max + '%' }" /><span class="da-range-text">{{ row.min.toFixed(0) }}-{{ row.max.toFixed(0) }}</span></div>
        </div>
      </div>
      <div v-else class="da-no-data">暂无数据</div>
    </div>

    <!-- ═══ View: Sections ═══ -->
    <div v-if="activeView === 'sections'" class="da-view">
      <div v-if="sectionStats.length" class="da-section-grid">
        <div v-for="sec in sectionStats" :key="sec.sectionId" class="da-sec-card">
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
        <div v-for="insp in inspectorStats" :key="insp.name" class="da-tr">
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
    <div v-if="!filteredSubmissions.length && !loading && !globalStats" class="da-empty">
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
.da-select:focus { border-color: #6366f1; }
.da-date { padding: 3px 6px; border: 1px solid #e5e7eb; border-radius: 6px; font-size: 11px; width: 100px; }
.da-sep { color: #d1d5db; font-size: 11px; }
.da-search {
  display: flex; align-items: center; gap: 4px; padding: 4px 8px;
  border: 1px solid #e5e7eb; border-radius: 6px; color: #9ca3af; flex: 1; max-width: 180px;
}
.da-search-input { border: none; outline: none; font-size: 12px; width: 100%; color: #374151; }
.da-search-input::placeholder { color: #d1d5db; }
.da-compute {
  display: flex; align-items: center; gap: 3px; margin-left: auto;
  padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 600;
  background: #f5f3ff; color: #6366f1; border: 1px solid #e0e0ff; cursor: pointer;
}
.da-compute:hover { background: #ede9fe; }
.da-compute:disabled { opacity: 0.5; }

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
.da-tab.on { background: #6366f1; color: #fff; }
.da-tab-count { font-size: 11px; color: #9ca3af; margin-left: auto; }

/* ═══ Table ═══ */
.da-table { background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; overflow: hidden; }
.da-thead {
  display: flex; padding: 7px 12px; background: #fafbfc; border-bottom: 1px solid #f0f0f3;
  font-size: 10px; font-weight: 700; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.3px;
}
.da-tr {
  display: flex; padding: 7px 12px; border-bottom: 1px solid #f9fafb;
  align-items: center; transition: background 0.1s; font-size: 12px;
}
.da-tr:last-child { border-bottom: none; }
.da-tr:hover { background: #fafbff; }
.da-tr.top { background: #fafbfe; }

.da-th, .da-td { display: flex; align-items: center; gap: 3px; cursor: default; }
.da-th-rank, .da-td-rank { width: 32px; justify-content: center; flex-shrink: 0; }
.da-th-name, .da-td-name { flex: 1.5; min-width: 0; font-weight: 600; color: #1e1b4b; cursor: pointer; }
.da-th-sec, .da-td-sec { flex: 0.8; justify-content: center; }
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
  background: #6366f1;
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
