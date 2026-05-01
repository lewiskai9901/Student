<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Play, Pause, CheckCircle, Send, Trash2, Save, Users, Settings, BarChart3, ClipboardList, Lock,
  ClipboardCheck, Check, X, ListTree, LayoutDashboard,
} from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/inspection/inspExecutionStore'
import {
  ProjectStatusConfig, type ProjectStatus,
  InspectorRoleConfig, type InspectorRole,
  AssignmentModeConfig, type AssignmentMode, type ScopeType,
} from '@/types/insp/enums'
import type { InspProject, ProjectInspector, InspTask, InspSubmission } from '@/types/insp/project'
import { inspProjectApi, updateOperationalConfig } from '@/api/inspection/project'
import { getTasks, assignTask } from '@/api/inspection/task'
import { getSubmissions } from '@/api/inspection/submission'
import { getSections } from '@/api/inspection/template'
import { getProfileBySection, getGradeBands } from '@/api/inspection/scoring'
import { getSimpleUserList, getUser } from '@/api/user'
import { getOrgUnitTree } from '@/api/organization'
import type { OrgUnitTreeNode } from '@/api/organization'
import type { SimpleUser } from '@/types/user'
import { getRootSection } from '@/api/inspection/template'
import SectionConfigView from './components/SectionConfigView.vue'
import { buildSectionTree, type SectionTreeNode } from '@/utils/sectionTree'
import IndicatorScoreView from './components/IndicatorScoreView.vue'

const route = useRoute()
const router = useRouter()
const store = useInspExecutionStore()
const projectId = Number(route.params.id)

// ========== State ==========
const loading = ref(false)
const project = ref<InspProject | null>(null)
const inspectors = ref<ProjectInspector[]>([])
const allTasks = ref<InspTask[]>([])
const allSubmissions = ref<InspSubmission[]>([])
const sectionNameMap = ref<Map<number, { name: string; targetType?: string }>>(new Map())
const sectionTree = ref<SectionTreeNode[]>([])
const sectionList = computed(() => [...sectionNameMap.value.entries()].map(([id, info]) => ({ id, sectionName: info.name, targetType: info.targetType })))
const rootGradeBands = ref<Array<{ name: string; min: number; max: number }>>([])
const creatorName = ref('')
const rootSectionName = ref('')
const scopeOrgNames = ref<string[]>([])
const orgTree = ref<OrgUnitTreeNode[]>([])
const loadingOrgTree = ref(false)
// Tabs
const activeTab = ref('overview')

// ========== 日期范围筛选 ==========
const dateRangeType = ref('all')
const customDateRange = ref<[string, string] | null>(null)

const activeDateRange = computed<{ start: string | null; end: string | null }>(() => {
  const today = new Date()
  const fmt = (d: Date) => d.toISOString().split('T')[0]

  switch (dateRangeType.value) {
    case 'today':
      return { start: fmt(today), end: fmt(today) }
    case 'week': {
      const day = today.getDay() || 7
      const mon = new Date(today)
      mon.setDate(today.getDate() - day + 1)
      const sun = new Date(mon)
      sun.setDate(mon.getDate() + 6)
      return { start: fmt(mon), end: fmt(sun) }
    }
    case 'month': {
      const first = new Date(today.getFullYear(), today.getMonth(), 1)
      const last = new Date(today.getFullYear(), today.getMonth() + 1, 0)
      return { start: fmt(first), end: fmt(last) }
    }
    case 'custom':
      if (customDateRange.value && customDateRange.value.length === 2) {
        return { start: customDateRange.value[0], end: customDateRange.value[1] }
      }
      return { start: null, end: null }
    default:
      return { start: null, end: null }
  }
})

const filteredTasks = computed(() => {
  const range = activeDateRange.value
  if (!range.start || !range.end) return allTasks.value
  return allTasks.value.filter(t => {
    const d = t.taskDate
    return d && d >= range.start! && d <= range.end!
  })
})

const filteredSubmissions = computed(() => {
  const range = activeDateRange.value
  if (!range.start || !range.end) return allSubmissions.value
  const taskIds = new Set(filteredTasks.value.map(t => t.id))
  return allSubmissions.value.filter(s => taskIds.has(s.taskId))
})
const configDirty = ref(false)
const saving = ref(false)

// 配置表单
const cf = ref({ scopeType: 'ORG', scopeIds: [] as string[], startDate: '', endDate: '', assignmentMode: 'FREE', reviewRequired: true, autoPublish: false, projectName: '', evaluationMode: 'SINGLE', multiRaterMode: 'AVERAGE', trendEnabled: false, decayEnabled: false, calibrationEnabled: false })

// 范围树
const scopeTreeRef = ref<any>(null)
const scopeFilterText = ref('')

function handleScopeCheckChange() {
  const checked = scopeTreeRef.value?.getCheckedKeys(false) ?? []
  cf.value.scopeIds = checked.map(String)
}

function filterScopeNode(value: string, data: any): boolean {
  if (!value) return true
  return (data.unitName || data.label || '').includes(value)
}

// 检查员添加
const addQuery = ref('')
const addResults = ref<SimpleUser[]>([])
const addLoading = ref(false)
const addRole = ref<InspectorRole>('INSPECTOR')

const isDraft = computed(() => project.value?.status === 'DRAFT')
const isArchived = computed(() => project.value?.status === 'ARCHIVED')
const canPublish = computed(() => isDraft.value && cf.value.scopeIds.length > 0 && !!cf.value.startDate)

// ========== 任务统计 ==========
const taskStats = computed(() => {
  const t = filteredTasks.value
  return {
    total: t.length,
    pending: t.filter(x => x.status === 'PENDING').length,
    active: t.filter(x => ['CLAIMED', 'IN_PROGRESS'].includes(x.status)).length,
    done: t.filter(x => ['SUBMITTED', 'UNDER_REVIEW', 'REVIEWED', 'PUBLISHED'].includes(x.status)).length,
    totalTargets: t.reduce((s, x) => s + x.totalTargets, 0),
    completedTargets: t.reduce((s, x) => s + x.completedTargets, 0),
  }
})
const progressPct = computed(() => taskStats.value.total === 0 ? 0 : Math.round(taskStats.value.done / taskStats.value.total * 100))

// ========== 待分配任务 ==========
const pendingAssignTasks = computed(() => filteredTasks.value.filter(t => t.status === 'PENDING' && !t.inspectorId))
const assigningTaskId = ref<number | null>(null)

async function handleAssignTask(task: InspTask, inspector: ProjectInspector) {
  try {
    assigningTaskId.value = task.id
    await assignTask(task.id, { inspectorId: Number(inspector.userId), inspectorName: inspector.userName })
    ElMessage.success(`已分配给 ${inspector.userName}`)
    await loadProject()
  } catch (e: any) {
    ElMessage.error(e.message || '分配失败')
  } finally {
    assigningTaskId.value = null
  }
}

// ========== 审核 ==========
const pendingReviewTasks = computed(() => filteredTasks.value.filter(t => t.status === 'SUBMITTED'))
const pendingReviewCount = computed(() => pendingReviewTasks.value.length)

async function handleApproveTask(task: InspTask) {
  try {
    await ElMessageBox.confirm(`通过任务 ${task.taskCode} 的审核？`, '确认审核', { type: 'info' })
    await store.reviewTask(task.id, { reviewerName: 'admin', comment: '审核通过' })
    ElMessage.success('审核通过')
    loadProject()
  } catch (e: any) {
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') { console.error('审核通过失败', e); ElMessage.error('审核操作失败，请重试') }
  }
}

async function handleRejectTask(task: InspTask) {
  try {
    const { value: comment } = await ElMessageBox.prompt('请输入驳回原因', '驳回任务', { type: 'warning', inputPlaceholder: '驳回原因...' }) as any
    await store.reviewTask(task.id, { reviewerName: 'admin', comment: comment || '审核驳回' })
    ElMessage.success('已驳回')
    loadProject()
  } catch (e: any) {
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') { console.error('驳回失败', e); ElMessage.error('驳回操作失败，请重试') }
  }
}

// ========== 检查结果统计 ==========
const resultStats = computed(() => {
  const agg = aggregatedTargetScores.value
  if (agg.length === 0) return null
  const scores = agg.map(a => a.totalScore)
  const passed = agg.filter(a => a.passed === true).length
  const failed = agg.filter(a => a.passed === false).length
  const avg = scores.length > 0 ? scores.reduce((a, b) => a + b, 0) / scores.length : 0
  const max = scores.length > 0 ? Math.max(...scores) : 0
  const min = scores.length > 0 ? Math.min(...scores) : 0
  const gradeMap = new Map<string, number>()
  for (const a of agg) { if (a.grade) gradeMap.set(a.grade, (gradeMap.get(a.grade) || 0) + 1) }
  return { total: agg.length, avg: avg.toFixed(1), max, min, passed, failed, unrated: agg.filter(a => a.passed == null).length, grades: [...gradeMap.entries()].sort((a, b) => b[1] - a[1]) }
})

// ========== 总览 Tab 数据 ==========
// 分区得分 - 从分析维度/submissions派生
const sectionScores = computed(() => {
  const subs = filteredSubmissions.value.filter(s => s.status === 'COMPLETED' && s.finalScore != null)
  if (subs.length === 0) return []
  // Group by targetName as a proxy for section
  const map = new Map<string, { name: string; scores: number[] }>()
  for (const s of subs) {
    const key = s.targetName || '未知目标'
    if (!map.has(key)) map.set(key, { name: key, scores: [] })
    map.get(key)!.scores.push(s.finalScore!)
  }
  return [...map.values()]
    .map(g => ({ name: g.name, avg: g.scores.reduce((a, b) => a + b, 0) / g.scores.length }))
    .sort((a, b) => b.avg - a.avg)
    .slice(0, 10)
})

// 最近5条任务
const recentTasks = computed(() => {
  return [...filteredTasks.value]
    .filter(t => ['SUBMITTED', 'UNDER_REVIEW', 'REVIEWED', 'PUBLISHED'].includes(t.status))
    .sort((a, b) => (b.taskDate || '').localeCompare(a.taskDate || ''))
    .slice(0, 5)
})

// 待整改数 = 未通过的submissions
const pendingCorrectiveCount = computed(() => filteredSubmissions.value.filter(s => s.passed === false).length)

function getSectionScoreColor(score: number): string {
  if (score >= 85) return '#10b981'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
}

// 按日期合并任务
interface DayTask { date: string; subTasks: { task: InspTask; projectName: string }[]; totalTargets: number; completedTargets: number; inspectorName: string; allDone: boolean }
const dayTasks = computed<DayTask[]>(() => {
  const dateMap = new Map<string, { task: InspTask; projectName: string }[]>()
  for (const task of filteredTasks.value) {
    if (!dateMap.has(task.taskDate)) dateMap.set(task.taskDate, [])
    dateMap.get(task.taskDate)!.push({ task, projectName: project.value?.projectName || '' })
  }
  return [...dateMap.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([date, subs]) => ({
    date, subTasks: subs,
    totalTargets: subs.reduce((s, x) => s + x.task.totalTargets, 0),
    completedTargets: subs.reduce((s, x) => s + x.task.completedTargets + x.task.skippedTargets, 0),
    inspectorName: subs[0]?.task.inspectorName || '未分配',
    allDone: subs.every(x => ['SUBMITTED', 'UNDER_REVIEW', 'REVIEWED', 'PUBLISHED'].includes(x.task.status)),
  }))
})

const inspectorStats = computed(() => {
  const todayStr = new Date().toISOString().slice(0, 10)
  const map = new Map<string, {
    name: string; assigned: number; completed: number; targets: number;
    active: number; overdue: number;
  }>()
  for (const task of filteredTasks.value) {
    const name = task.inspectorName || '未分配'
    if (!map.has(name)) map.set(name, { name, assigned: 0, completed: 0, targets: 0, active: 0, overdue: 0 })
    const s = map.get(name)!; s.assigned++
    if (['SUBMITTED', 'UNDER_REVIEW', 'REVIEWED', 'PUBLISHED'].includes(task.status)) s.completed++
    if (['CLAIMED', 'IN_PROGRESS'].includes(task.status)) s.active++
    const eff = (task as any).extendedTo || task.taskDate
    if (eff && eff < todayStr && !['REVIEWED', 'PUBLISHED', 'CANCELLED', 'EXPIRED'].includes(task.status)) {
      s.overdue++
    }
    s.targets += task.completedTargets
  }
  return [...map.values()].sort((a, b) => {
    // 优先排序: 逾期数 > 进行中数 > 总分配数
    if (b.overdue !== a.overdue) return b.overdue - a.overdue
    if (b.active !== a.active) return b.active - a.active
    return b.assigned - a.assigned
  })
})

// 检查员搜索 (人员卡片过滤)
const inspectorFilter = ref('')
const filteredInspectors = computed(() => {
  const q = inspectorFilter.value.trim().toLowerCase()
  if (!q) return inspectors.value
  return inspectors.value.filter(i => (i.userName || '').toLowerCase().includes(q))
})

const targetScores = computed(() => {
  return filteredSubmissions.value
    .filter(s => s.status === 'COMPLETED' && s.finalScore != null)
    .sort((a, b) => (a.finalScore ?? 0) - (b.finalScore ?? 0))
})

// Aggregated target scores: group by rootTargetId, compute average, include section breakdown
const aggregatedTargetScores = computed(() => {
  const completed = filteredSubmissions.value.filter(s => s.status === 'COMPLETED' && s.finalScore != null)
  // Group by rootTargetId (or targetId if rootTargetId is absent)
  const groups = new Map<number, typeof completed>()
  for (const s of completed) {
    const key = s.rootTargetId ?? s.targetId
    if (!key) continue
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key)!.push(s)
  }
  // Build aggregated rows
  const rows: Array<{
    targetId: number
    targetName: string
    totalScore: number
    grade: string | null
    passed: boolean | null
    sections: Array<{ sectionId: number; sectionName: string; score: number; grade: string | null }>
  }> = []
  for (const [targetId, subs] of groups) {
    // Simple average of all section scores
    let total = 0
    const sections: typeof rows[0]['sections'] = []
    for (const s of subs) {
      const secInfo = sectionNameMap.value.get(s.sectionId!)
      total += (s.finalScore ?? 0)
      sections.push({
        sectionId: s.sectionId!,
        sectionName: secInfo?.name || (s as any).sectionName || `分区${s.sectionId}`,
        score: s.finalScore ?? 0,
        grade: s.grade ?? null,
      })
    }
    const roundedTotal = subs.length > 0 ? Math.round((total / subs.length) * 10) / 10 : 0
    // Overall grade from root section's grade bands (absolute score matching)
    let overallGrade: string | null = null
    let overallPassed: boolean | null = null
    for (const band of rootGradeBands.value) {
      if (roundedTotal >= band.min && roundedTotal <= band.max) {
        overallGrade = band.name
        break
      }
    }
    // If no root grade bands, check if any section has grades
    const anySecGrade = sections.some(s => s.grade != null)
    if (!overallGrade && !anySecGrade) {
      overallPassed = null
    } else {
      overallPassed = overallGrade ? !overallGrade.includes('不') : null
    }
    rows.push({
      targetId,
      targetName: subs[0].rootTargetName || subs[0].targetName || `目标${targetId}`,
      totalScore: roundedTotal,
      grade: overallGrade,
      passed: overallPassed,
      sections
    })
  }
  return rows.sort((a, b) => a.totalScore - b.totalScore)
})

const hasGradeConfig = computed(() => {
  return aggregatedTargetScores.value.some(a => a.grade != null || a.sections.some(s => s.grade != null))
})

// ========== 维度选择 (Dimension tabs for scores) ==========
const selectedDimension = ref<'overall' | number>('overall')

const dimensionTabs = computed(() => {
  const tabs: Array<{ key: 'overall' | number; label: string }> = [{ key: 'overall', label: '综合' }]
  const entries = [...sectionNameMap.value.entries()]
  for (const [id, info] of entries) {
    tabs.push({ key: id, label: info.name })
  }
  return tabs
})

const dimensionScores = computed(() => {
  if (selectedDimension.value === 'overall') {
    return [...aggregatedTargetScores.value]
      .sort((a, b) => b.totalScore - a.totalScore)
      .map((a, i) => ({ ...a, rank: i + 1 }))
  }
  const sectionId = selectedDimension.value as number
  const completed = filteredSubmissions.value
    .filter(s => s.status === 'COMPLETED' && s.finalScore != null && s.sectionId === sectionId)
    .sort((a, b) => (b.finalScore ?? 0) - (a.finalScore ?? 0))
  return completed.map((s, i) => ({
    targetId: s.rootTargetId ?? s.targetId,
    targetName: s.rootTargetName || s.targetName || '?',
    totalScore: s.finalScore ?? 0,
    grade: s.grade ?? null,
    passed: null as boolean | null,
    rank: i + 1,
    sections: [] as Array<{ sectionId: number; sectionName: string; score: number; grade: string | null; weight: number }>
  }))
})

const dimensionStats = computed(() => {
  const scores = dimensionScores.value
  if (scores.length === 0) return null
  const vals = scores.map(s => s.totalScore)
  const avg = vals.reduce((a, b) => a + b, 0) / vals.length
  return {
    total: scores.length,
    avg: avg.toFixed(1),
    max: Math.max(...vals).toFixed(1),
    min: Math.min(...vals).toFixed(1),
    passed: scores.filter(s => s.grade && !s.grade.includes('不')).length,
    failed: scores.filter(s => s.grade && s.grade.includes('不')).length,
  }
})

const dimensionHasGrades = computed(() => {
  return dimensionScores.value.some(s => s.grade != null)
})

// Grade color resolver (简单回退，详细等级颜色由 IndicatorScoreView 处理)
function getGradeColor(grade: string | null, _sectionId?: number): string | null {
  if (!grade) return null
  if (grade.includes('不') || grade.includes('差')) return '#ef4444'
  return '#10b981'
}

// ========== Load ==========
async function loadProject() {
  loading.value = true
  try {
    project.value = await store.loadProject(projectId)
    inspectors.value = await store.loadInspectors(projectId)
    if (isDraft.value) activeTab.value = 'settings'
    syncForm()
    if (project.value.createdBy) { try { const u = await getUser(project.value.createdBy); creatorName.value = u.realName || u.username } catch (e: any) { console.warn('加载创建者信息失败', e) } }
    if (project.value.rootSectionId) {
      try { const section = await getRootSection(project.value.rootSectionId); rootSectionName.value = section.sectionName } catch (e: any) { console.warn('加载模板分区名称失败', e) }
    } else {
      // 多模板项目：模板通过计划关联，header 不显示具体模板名
      rootSectionName.value = ''
    }
    await loadScopeNames()
    // review #12: 加载模板版本状态 (异步, 不阻塞主流程)
    loadTemplateVersionStatus()
    // Load section tree
    if (project.value.rootSectionId) {
      try {
        const sections = await getSections(project.value.rootSectionId)
        const map = new Map<number, { name: string; targetType?: string }>()
        for (const sec of sections) {
          map.set(Number(sec.id), { name: sec.sectionName, targetType: sec.targetType ?? undefined })
        }
        sectionNameMap.value = map
        sectionTree.value = buildSectionTree(sections, project.value.rootSectionId)
      } catch (e) { console.warn('加载分区树失败', e) }
      // Load root section grade bands
      try {
        const rootProfile = await getProfileBySection(project.value.rootSectionId)
        if (rootProfile?.id) {
          const bands = await getGradeBands(rootProfile.id)
          rootGradeBands.value = bands.map(b => ({ name: b.gradeName || b.gradeCode, min: b.minScore, max: b.maxScore }))
        }
      } catch (e) { /* no root grade mapping */ rootGradeBands.value = [] }
    }
    allTasks.value = []; allSubmissions.value = []
    try {
      const tasks = await getTasks({ projectId })
      allTasks.value.push(...tasks)
      for (const t of tasks) { try { allSubmissions.value.push(...await getSubmissions({ taskId: t.id })) } catch (e: any) { console.warn(`加载任务 ${t.id} 的提交记录失败`, e) } }
    } catch (e: any) { console.error('加载任务列表失败', e); ElMessage.error('加载任务列表失败') }
    configDirty.value = false
  } catch (e: any) { ElMessage.error(e.message || '加载失败') }
  finally { loading.value = false }
}
function syncForm() {
  if (!project.value) return; const p = project.value
  let rawIds: (number | string)[] = []; try { rawIds = p.scopeConfig ? JSON.parse(p.scopeConfig) : [] } catch (e: any) { console.warn('解析 scopeConfig 失败', e) }
  const ids: string[] = rawIds.map(String)
  cf.value = { scopeType: p.scopeType || 'ORG', scopeIds: ids, startDate: p.startDate || '', endDate: p.endDate || '', assignmentMode: p.assignmentMode || 'FREE', reviewRequired: p.reviewRequired ?? true, autoPublish: p.autoPublish ?? false, projectName: p.projectName, evaluationMode: (p as any).evaluationMode || 'SINGLE', multiRaterMode: (p as any).multiRaterMode || 'AVERAGE', trendEnabled: (p as any).trendEnabled ?? false, decayEnabled: (p as any).decayEnabled ?? false, calibrationEnabled: (p as any).calibrationEnabled ?? false }
}

// Watch cf changes after initial sync
let watchEnabled = false
function startWatch() { watchEnabled = true }
watch(cf, () => { if (watchEnabled) configDirty.value = true }, { deep: true })
watch(scopeFilterText, (val) => { scopeTreeRef.value?.filter(val) })

async function loadScopeNames() {
  scopeOrgNames.value = []
  if (!project.value?.scopeConfig) return
  try { const rawIds: (number | string)[] = JSON.parse(project.value.scopeConfig); const ids = rawIds.map(String); if (orgTree.value.length === 0) await loadOrgTree(); const m = buildMap(orgTree.value); scopeOrgNames.value = ids.map(id => m.get(id) || `#${id}`) } catch (e: any) { console.warn('加载检查范围名称失败', e) }
}
function buildMap(nodes: OrgUnitTreeNode[]): Map<string, string> { const m = new Map<string, string>(); function w(l: OrgUnitTreeNode[]) { for (const n of l) { m.set(String(n.id), n.unitName); if (n.children) w(n.children) } }; w(nodes); return m }
async function loadOrgTree() { if (orgTree.value.length > 0) return; loadingOrgTree.value = true; try { orgTree.value = await getOrgUnitTree() } catch (e: any) { console.error('加载组织树失败', e); ElMessage.error('加载组织结构失败') }; loadingOrgTree.value = false }

// ========== Save ==========
async function saveConfig() {
  if (!project.value) return; saving.value = true
  try {
    if (isDraft.value) {
      await inspProjectApi.update(projectId, { projectName: cf.value.projectName, rootSectionId: project.value.rootSectionId, scopeType: cf.value.scopeType as ScopeType, scopeConfig: cf.value.scopeIds.length > 0 ? JSON.stringify(cf.value.scopeIds) : undefined, startDate: cf.value.startDate || undefined, endDate: cf.value.endDate || undefined, assignmentMode: cf.value.assignmentMode as AssignmentMode, reviewRequired: cf.value.reviewRequired, autoPublish: cf.value.autoPublish })
    } else {
      await updateOperationalConfig(projectId, { projectName: cf.value.projectName, assignmentMode: cf.value.assignmentMode, reviewRequired: cf.value.reviewRequired, autoPublish: cf.value.autoPublish })
    }
    ElMessage.success('已保存'); configDirty.value = false; loadProject()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') } finally { saving.value = false }
}

// ========== Actions ==========
async function handlePublish() {
  if (!project.value) return
  if (cf.value.scopeIds.length === 0) { ElMessage.error('请先配置检查范围'); activeTab.value = 'settings'; return }
  if (!cf.value.startDate) { ElMessage.error('请先设置开始日期'); activeTab.value = 'settings'; return }
  if (configDirty.value) await saveConfig()
  try { await ElMessageBox.confirm('确定发布？将自动生成检查任务。', '确认发布', { type: 'warning' }); await store.publishProject(projectId, { templateVersionId: project.value.templateVersionId || null as any }); ElMessage.success('已发布'); activeTab.value = 'overview'; loadProject() } catch (e: any) {
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') { console.error('发布项目失败', e); ElMessage.error('发布项目失败，请重试') }
  }
}
async function handlePause() {
  const live = taskStats.value.total - taskStats.value.done
  const tip = live > 0
    ? `当前还有 ${live} 个未完成任务, 暂停后将冻结. 继续?`
    : '确定暂停此项目?'
  try {
    await ElMessageBox.confirm(tip, '确认暂停', { type: 'warning' })
    await store.pauseProject(projectId); ElMessage.success('已暂停'); loadProject()
  } catch (e: any) {
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') { ElMessage.error(e.message || '暂停失败') }
  }
}
async function handleResume() { try { await store.resumeProject(projectId); ElMessage.success('已恢复'); loadProject() } catch (e: any) { ElMessage.error(e.message || '失败') } }
async function handleComplete() {
  if (!project.value) return
  const live = taskStats.value.total - taskStats.value.done
  const tip = live > 0
    ? `还有 ${live} 个未完成任务. 完结后项目不可恢复, 任务会被强制归档. 确认?`
    : '完结操作不可逆. 确认?'
  try {
    await ElMessageBox.confirm(tip, '确认完结 (不可逆)', { type: 'warning', confirmButtonText: '确认完结', confirmButtonClass: 'el-button--danger' })
    await store.completeProject(projectId); ElMessage.success('已完结'); loadProject()
  } catch (e: any) {
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') { console.error('完结项目失败', e); ElMessage.error('完结项目失败，请重试') }
  }
}
async function handleArchive() { try { await ElMessageBox.confirm('确定归档？归档后不可恢复为活跃状态。', '确认归档', { type: 'warning' }); await inspProjectApi.archive(projectId); ElMessage.success('已归档'); loadProject() } catch (e: any) {
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') { console.error('归档项目失败', e); ElMessage.error('归档项目失败，请重试') }
  } }
async function handleClaim(task: InspTask) { try { await store.claimTask(task.id, { inspectorName: '当前用户' }); ElMessage.success('已领取'); loadProject() } catch (e: any) { ElMessage.error(e.message || '失败') } }

// ========== Inspector ==========
async function searchUsers(q: string) { if (!q.trim()) { addResults.value = []; return }; addLoading.value = true; try { addResults.value = await getSimpleUserList(q.trim()) } catch (e: any) { console.warn('搜索用户失败', e); addResults.value = [] }; addLoading.value = false }
async function handleAddInspector(userId: number) {
  const u = addResults.value.find(x => Number(x.id) === userId); if (!u) return
  try { await store.addInspector(projectId, { userId: Number(u.id), userName: u.realName || u.username, role: addRole.value }); ElMessage.success(`已添加 ${u.realName || u.username}`); addQuery.value = ''; addResults.value = []; inspectors.value = await store.loadInspectors(projectId) } catch (e: any) { ElMessage.error(e.message || '失败') }
}
async function handleRemoveInspector(insp: ProjectInspector) { try { await ElMessageBox.confirm(`移除「${insp.userName}」？`, '确认', { type: 'warning' }); await store.removeInspector(projectId, insp.id); inspectors.value = await store.loadInspectors(projectId) } catch (e: any) {
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') { console.error('移除检查员失败', e); ElMessage.error('移除检查员失败，请重试') }
  } }

function goBack() { router.push('/inspection/projects') }
function goExecuteTask(taskId: number) { router.push(`/inspection/tasks/${taskId}/execute`) }

// review #12: 模板版本状态 (drifted / 当前 / 最新)
const templateVersionStatus = ref<{
  drifted: boolean
  currentVersionNumber?: number
  latestVersionNumber?: number
  multiTemplate?: boolean
} | null>(null)

async function loadTemplateVersionStatus() {
  if (!project.value || !project.value.rootSectionId) {
    templateVersionStatus.value = null
    return
  }
  try {
    templateVersionStatus.value = await inspProjectApi.getTemplateVersionStatus(projectId)
  } catch (e) {
    templateVersionStatus.value = null
  }
}

// review #1: 升级模板版本至最新
async function handleUpgradeTemplate() {
  if (!project.value) return
  try {
    await ElMessageBox.confirm(
      '将把项目的模板快照升级到该模板的最新已发布版本. 升级后新生成的任务将按新模板结构填充. 确认?',
      '升级模板版本', { type: 'warning' },
    )
    const oldVersionId = project.value.templateVersionId
    const updated = await inspProjectApi.upgradeTemplateVersion(projectId)
    if (updated.templateVersionId === oldVersionId) {
      ElMessage.info('已是最新版本, 无需升级')
    } else {
      ElMessage.success(`模板版本已从 ${oldVersionId} 升级到 ${updated.templateVersionId}`)
      loadProject()
    }
  } catch (e: any) {
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') {
      ElMessage.error(e.message || '升级失败')
    }
  }
}
onMounted(async () => {
  await loadOrgTree()
  await loadProject()
  startWatch()
})
</script>

<template>
  <div class="pdv insp-shell" v-loading="loading">
    <!-- ====== Header (Audit Console redesign) ====== -->
    <div class="pdv-header">
      <div class="pdv-header-left">
        <button class="pdv-back-btn" @click="goBack" title="返回">
          <ArrowLeft class="w-4 h-4" />
        </button>
        <div class="pdv-head-block">
          <div class="insp-eyebrow">检查项目 · {{ project?.projectCode || '加载中' }}</div>
          <div class="pdv-title">
            <span class="pdv-title__name">{{ project?.projectName || '加载中...' }}</span>
            <span v-if="project" class="insp-chip" :class="{
              'insp-chip--pending': project.status === 'DRAFT',
              'insp-chip--info': project.status === 'PUBLISHED',
              'insp-chip--warn': project.status === 'PAUSED',
              'insp-chip--pass': project.status === 'COMPLETED',
              'insp-chip--fail': project.status === 'ARCHIVED',
            }">
              {{ ProjectStatusConfig[project.status as ProjectStatus]?.label }}
            </span>
          </div>
          <div class="pdv-subtitle" v-if="project">
            <span v-if="project.startDate" class="insp-num">{{ project.startDate }}<template v-if="project.endDate"> ~ {{ project.endDate }}</template></span>
            <span v-if="rootSectionName" class="pdv-subtitle-sep">·</span>
            <span v-if="rootSectionName">{{ rootSectionName }}</span>
            <span v-if="inspectors.length" class="pdv-subtitle-sep">·</span>
            <span v-if="inspectors.length"><span class="insp-num">{{ inspectors.length }}</span> 人</span>
          </div>
        </div>
      </div>
      <div class="pdv-header-actions" v-if="project">
        <el-button
          v-if="configDirty && !isArchived && activeTab === 'settings'"
          type="primary"
          :loading="saving"
          @click="saveConfig"
          size="small"
          round
        >
          <Save class="w-3.5 h-3.5 mr-1" />保存配置
        </el-button>
        <el-button v-if="isDraft" type="primary" :disabled="!canPublish" @click="handlePublish" size="small" round>
          <Send class="w-3.5 h-3.5 mr-1" />发布项目
        </el-button>
        <el-button v-if="project.status === 'PUBLISHED'" plain @click="handlePause" size="small" round
                   title="暂停: 项目可恢复, 任务被冻结">
          <Pause class="w-3.5 h-3.5 mr-1" />暂停
        </el-button>
        <el-button v-if="project.status === 'PAUSED'" type="primary" @click="handleResume" size="small" round>
          <Play class="w-3.5 h-3.5 mr-1" />恢复
        </el-button>
        <el-button v-if="['PUBLISHED','PAUSED'].includes(project.status) && project.rootSectionId"
                   @click="handleUpgradeTemplate" size="small" round
                   :type="templateVersionStatus?.drifted ? 'warning' : ''"
                   :plain="!templateVersionStatus?.drifted"
                   title="把项目锁定的模板快照升级至该模板的最新已发布版本; 模板被改后已发布项目可能无法创建新任务">
          <span v-if="templateVersionStatus?.drifted">
            升级模板版本 (v{{ templateVersionStatus.currentVersionNumber ?? '?' }} → v{{ templateVersionStatus.latestVersionNumber }})
          </span>
          <span v-else-if="templateVersionStatus">
            模板版本 v{{ templateVersionStatus.currentVersionNumber ?? '?' }} (最新)
          </span>
          <span v-else>
            升级模板版本
          </span>
        </el-button>
        <el-button v-if="['PUBLISHED','PAUSED'].includes(project.status)" type="danger" @click="handleComplete" size="small" round plain
                   title="完结: 不可逆, 任务被强制归档">
          <CheckCircle class="w-3.5 h-3.5 mr-1" />完结
        </el-button>
        <el-button v-if="project.status === 'COMPLETED'" type="info" @click="handleArchive" size="small" round plain>归档</el-button>
      </div>
    </div>

    <!-- ====== Pill Tabs (顺序: 总览 → 检查配置 → 人员与任务 → 成绩统计 → 设置) ====== -->
    <div class="pdv-tabs">
      <button :class="['pdv-tab', activeTab === 'overview' && 'active']" @click="activeTab = 'overview'">
        <LayoutDashboard class="w-3.5 h-3.5" />总览
      </button>
      <button :class="['pdv-tab', activeTab === 'config' && 'active']" @click="activeTab = 'config'">
        <ListTree class="w-3.5 h-3.5" />检查配置
      </button>
      <button :class="['pdv-tab', activeTab === 'team' && 'active']" @click="activeTab = 'team'">
        <Users class="w-3.5 h-3.5" />人员与任务
        <span v-if="pendingReviewCount > 0 || pendingAssignTasks.length > 0" class="pdv-tab-badge">{{ pendingReviewCount + pendingAssignTasks.length }}</span>
      </button>
      <button :class="['pdv-tab', activeTab === 'scores' && 'active']" @click="activeTab = 'scores'">
        <BarChart3 class="w-3.5 h-3.5" />成绩统计
      </button>
      <button :class="['pdv-tab', activeTab === 'settings' && 'active']" @click="activeTab = 'settings'">
        <Settings class="w-3.5 h-3.5" />设置
        <span v-if="configDirty" class="pdv-tab-dot" />
      </button>
    </div>

    <!-- ====== Tab Content ====== -->
    <div class="pdv-body">

      <!-- ===== 总览 Tab ===== -->
      <div v-if="activeTab === 'overview'">

        <!-- 未发布提示 -->
        <div v-if="isDraft" class="pdv-empty-state">
          <ClipboardList class="w-10 h-10 text-blue-300 mb-3" />
          <div class="font-medium text-gray-600 mb-1">项目尚未发布</div>
          <div class="text-sm text-gray-400 mb-4">请在「设置」中完成配置后发布</div>
          <el-button type="primary" :disabled="!canPublish" @click="handlePublish" size="small" round>
            <Send class="w-3.5 h-3.5 mr-1" />发布项目
          </el-button>
        </div>

        <template v-else>
          <!-- 统计卡片行 -->
          <div class="pdv-stat-row">
            <div class="pdv-stat-card">
              <div class="pdv-stat-value">{{ taskStats.total }}</div>
              <div class="pdv-stat-label">总任务数</div>
            </div>
            <div class="pdv-stat-divider" />
            <div class="pdv-stat-card">
              <div class="pdv-stat-value text-green-600">{{ taskStats.done }}</div>
              <div class="pdv-stat-label">已完成</div>
            </div>
          </div>

          <!-- 进度条 -->
          <div v-if="taskStats.total > 0" class="pdv-progress-row">
            <div class="pdv-progress-label">
              <span class="text-sm font-medium text-gray-700">执行进度</span>
              <span class="text-sm font-bold text-[#1a6dff]">{{ progressPct }}%</span>
            </div>
            <div class="pdv-progress-track">
              <div class="pdv-progress-fill" :style="{ width: progressPct + '%' }" />
            </div>
            <div class="pdv-progress-stats">
              <span><span class="text-orange-500 font-semibold">{{ taskStats.pending }}</span> 待领取</span>
              <span><span class="text-blue-600 font-semibold">{{ taskStats.active }}</span> 进行中</span>
              <span><span class="text-green-600 font-semibold">{{ taskStats.done }}</span> 已完成</span>
            </div>
          </div>

          <!-- 两列布局: 分区得分 + 最近检查 -->
          <div class="pdv-two-col">

            <!-- 分区得分 -->
            <div class="pdv-card">
              <div class="pdv-card-title">
                <BarChart3 class="w-4 h-4 text-[#1a6dff]" />目标得分分布
              </div>
              <div v-if="sectionScores.length === 0" class="pdv-card-empty">暂无得分数据</div>
              <div v-else class="pdv-score-list">
                <div v-for="item in sectionScores" :key="item.name" class="pdv-score-item">
                  <div class="pdv-score-name" :title="item.name">{{ item.name }}</div>
                  <div class="pdv-score-bar-wrap">
                    <div class="pdv-score-bar-track">
                      <div
                        class="pdv-score-bar-fill"
                        :style="{ width: item.avg + '%', background: getSectionScoreColor(item.avg) }"
                      />
                    </div>
                    <span class="pdv-score-val" :style="{ color: getSectionScoreColor(item.avg) }">
                      {{ item.avg.toFixed(1) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 最近检查 -->
            <div class="pdv-card">
              <div class="pdv-card-title">
                <ClipboardList class="w-4 h-4 text-[#1a6dff]" />最近检查
              </div>
              <div v-if="recentTasks.length === 0" class="pdv-card-empty">暂无已完成任务</div>
              <div v-else class="pdv-recent-list">
                <div v-for="task in recentTasks" :key="task.id" class="pdv-recent-item">
                  <div class="pdv-recent-meta">
                    <span class="pdv-recent-date">{{ task.taskDate }}</span>
                    <span class="pdv-recent-inspector">{{ task.inspectorName || '-' }}</span>
                  </div>
                  <div class="pdv-recent-code text-gray-500 text-xs truncate">{{ task.taskCode }}</div>
                  <div class="flex items-center gap-1.5 mt-1">
                    <el-tag
                      :type="task.status === 'PUBLISHED' ? 'success' : task.status === 'REVIEWED' ? 'primary' : 'warning'"
                      size="small" round effect="plain"
                    >
                      {{ ({ SUBMITTED:'已提交', UNDER_REVIEW:'审核中', REVIEWED:'已审核', PUBLISHED:'已发布' } as Record<string, string>)[task.status] || task.status }}
                    </el-tag>
                    <span class="text-xs text-gray-400">{{ task.completedTargets }}/{{ task.totalTargets }} 目标</span>
                  </div>
                </div>
              </div>

              <!-- 审核待办 -->
              <div v-if="pendingReviewCount > 0" class="pdv-review-alert">
                <ClipboardCheck class="w-3.5 h-3.5 text-orange-500" />
                <span>{{ pendingReviewCount }} 个任务待审核</span>
                <button class="pdv-review-link" @click="activeTab = 'team'">去处理</button>
              </div>
            </div>

            <!-- 待分配任务 -->
            <div v-if="pendingAssignTasks.length > 0" class="pdv-card">
              <div class="pdv-card-title">
                <Users class="w-4 h-4 text-orange-500" />待分配任务
                <span class="pdv-badge-orange ml-1.5">{{ pendingAssignTasks.length }}</span>
              </div>
              <div class="pdv-assign-list">
                <div v-for="task in pendingAssignTasks.slice(0, 8)" :key="task.id" class="pdv-assign-row">
                  <span class="text-xs text-gray-500">{{ task.taskDate }}</span>
                  <span class="text-xs text-gray-400">{{ task.totalTargets }}个目标</span>
                  <el-select
                    :model-value="null"
                    placeholder="选择检查员"
                    size="small"
                    style="width: 140px"
                    :loading="assigningTaskId === task.id"
                    @change="(val: any) => {
                      const insp = inspectors.find((i: ProjectInspector) => String(i.userId) === String(val))
                      if (insp) handleAssignTask(task, insp)
                    }"
                  >
                    <el-option
                      v-for="insp in inspectors"
                      :key="insp.userId"
                      :label="insp.userName"
                      :value="insp.userId"
                    />
                  </el-select>
                </div>
              </div>
            </div>

          </div>

        </template>
      </div>

      <!-- ===== 成绩统计 Tab ===== -->
      <div v-if="activeTab === 'scores'">
        <IndicatorScoreView v-if="!isDraft" :project-id="projectId" />

        <!-- Legacy score aggregation removed — IndicatorScoreView handles everything -->
        <template v-if="false">
          <!-- Filter Bar -->
          <div class="flex items-center gap-3 mb-5">
            <el-radio-group v-model="dateRangeType" size="small">
              <el-radio-button value="today">今日</el-radio-button>
              <el-radio-button value="week">本周</el-radio-button>
              <el-radio-button value="month">本月</el-radio-button>
              <el-radio-button value="custom">自定义</el-radio-button>
              <el-radio-button value="all">全部</el-radio-button>
            </el-radio-group>
            <el-date-picker v-if="dateRangeType === 'custom'" v-model="customDateRange"
              type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束"
              size="small" value-format="YYYY-MM-DD" style="width: 220px" />
          </div>

          <!-- Dimension Tabs -->
          <div class="flex gap-1 mb-5 border-b border-gray-100 pb-3">
            <button v-for="tab in dimensionTabs" :key="tab.key"
              :class="['px-3.5 py-1.5 rounded-full text-sm font-medium transition-all',
                selectedDimension === tab.key
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-gray-500 hover:text-gray-700 hover:bg-gray-100']"
              @click="selectedDimension = tab.key">
              {{ tab.label }}
            </button>
          </div>

          <!-- Stats Cards -->
          <div v-if="dimensionStats" class="grid gap-4 mb-5"
               :class="dimensionHasGrades ? 'grid-cols-5' : 'grid-cols-3'">
            <div class="bg-white rounded-xl border border-gray-100 p-4 text-center">
              <div class="text-2xl font-bold text-gray-800">{{ dimensionStats!.total }}</div>
              <div class="text-xs text-gray-400 mt-1">已评目标</div>
            </div>
            <div class="bg-white rounded-xl border border-gray-100 p-4 text-center">
              <div class="text-2xl font-bold text-blue-600">{{ dimensionStats!.avg }}</div>
              <div class="text-xs text-gray-400 mt-1">平均分</div>
            </div>
            <div class="bg-white rounded-xl border border-gray-100 p-4 text-center">
              <div class="text-2xl font-bold text-gray-600">
                {{ dimensionStats!.max }}<span class="text-sm font-normal text-gray-300"> / {{ dimensionStats!.min }}</span>
              </div>
              <div class="text-xs text-gray-400 mt-1">最高 / 最低</div>
            </div>
            <div v-if="dimensionHasGrades" class="bg-white rounded-xl border border-gray-100 p-4 text-center">
              <div class="text-2xl font-bold text-emerald-600">{{ dimensionStats!.passed }}</div>
              <div class="text-xs text-gray-400 mt-1">达标</div>
            </div>
            <div v-if="dimensionHasGrades" class="bg-white rounded-xl border border-gray-100 p-4 text-center">
              <div class="text-2xl font-bold text-red-500">{{ dimensionStats!.failed }}</div>
              <div class="text-xs text-gray-400 mt-1">未达标</div>
            </div>
          </div>

          <!-- Score Distribution (only for overall) -->
          <div v-if="selectedDimension === 'overall' && sectionScores.length > 0" class="bg-white rounded-xl border border-gray-100 p-4 mb-5">
            <div class="text-xs font-semibold text-gray-500 mb-3">各维度平均分</div>
            <div class="flex flex-col gap-2">
              <div v-for="item in sectionScores" :key="item.name" class="flex items-center gap-2">
                <span class="text-xs text-gray-500 w-24 truncate flex-shrink-0" :title="item.name">{{ item.name }}</span>
                <div class="flex-1 h-5 bg-gray-50 rounded overflow-hidden">
                  <div class="h-full rounded transition-all duration-300"
                    :style="{ width: item.avg + '%', backgroundColor: getSectionScoreColor(item.avg) }" />
                </div>
                <span class="text-xs font-semibold w-10 text-right" :style="{ color: getSectionScoreColor(item.avg) }">
                  {{ item.avg.toFixed(1) }}
                </span>
              </div>
            </div>
          </div>

          <!-- Rankings Table -->
          <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
            <div class="px-5 py-3 border-b border-gray-50 flex items-center justify-between">
              <span class="text-sm font-semibold text-gray-700">
                {{ selectedDimension === 'overall' ? '综合排名' : dimensionTabs.find(t => t.key === selectedDimension)?.label + '排名' }}
              </span>
              <span class="text-xs text-gray-400">{{ dimensionScores.length }} 个目标</span>
            </div>

            <div v-if="dimensionScores.length === 0" class="py-16 text-center text-gray-400 text-sm">
              暂无成绩数据
            </div>

            <el-table v-else :data="dimensionScores" size="small" row-key="targetId"
              :row-class-name="({rowIndex}: any) => rowIndex < 3 ? 'top-rank-row' : ''">

              <!-- Expand (only for overall) -->
              <el-table-column v-if="selectedDimension === 'overall'" type="expand" width="32">
                <template #default="{ row }">
                  <div class="px-6 py-3 bg-gray-50/60">
                    <div class="grid grid-cols-2 gap-x-8 gap-y-2">
                      <div v-for="sec in row.sections" :key="sec.sectionName"
                           class="flex items-center justify-between py-1">
                        <div class="flex items-center gap-2">
                          <span class="text-sm text-gray-600">{{ sec.sectionName }}</span>
                        </div>
                        <div class="flex items-center gap-2">
                          <span class="text-sm font-semibold" :style="{ color: getSectionScoreColor(sec.score) }">
                            {{ sec.score.toFixed(1) }}
                          </span>
                          <span v-if="sec.grade" class="text-[11px] px-1.5 py-0.5 rounded font-medium"
                            :style="getGradeColor(sec.grade, sec.sectionId) ? {
                              background: getGradeColor(sec.grade, sec.sectionId) + '18',
                              color: getGradeColor(sec.grade, sec.sectionId) ?? undefined,
                            } : undefined"
                            :class="!getGradeColor(sec.grade, sec.sectionId) ? 'bg-blue-50 text-blue-600' : ''">
                            {{ sec.grade }}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </template>
              </el-table-column>

              <!-- Rank -->
              <el-table-column label="#" width="50" align="center">
                <template #default="{ row }">
                  <span v-if="row.rank === 1" class="text-lg">&#x1F947;</span>
                  <span v-else-if="row.rank === 2" class="text-lg">&#x1F948;</span>
                  <span v-else-if="row.rank === 3" class="text-lg">&#x1F949;</span>
                  <span v-else class="text-sm text-gray-400 font-medium">{{ row.rank }}</span>
                </template>
              </el-table-column>

              <!-- Target Name -->
              <el-table-column prop="targetName" label="检查目标" min-width="180">
                <template #default="{ row }">
                  <span class="text-sm font-medium text-gray-700">{{ row.targetName }}</span>
                </template>
              </el-table-column>

              <!-- Score -->
              <el-table-column label="得分" width="90" align="center">
                <template #default="{ row }">
                  <span class="text-sm font-bold" :style="{ color: getSectionScoreColor(row.totalScore) }">
                    {{ row.totalScore.toFixed(1) }}
                  </span>
                </template>
              </el-table-column>

              <!-- Grade (conditional) -->
              <el-table-column v-if="dimensionHasGrades" label="等级" width="100" align="center">
                <template #default="{ row }">
                  <span v-if="row.grade" class="pdv-grade-badge"
                    :style="getGradeColor(row.grade) ? {
                      background: getGradeColor(row.grade) + '18',
                      color: getGradeColor(row.grade) ?? undefined,
                      borderColor: getGradeColor(row.grade) + '30',
                    } : undefined"
                    :class="!getGradeColor(row.grade) ? (
                      row.grade.includes('不') ? 'bg-red-50 text-red-600' :
                      row.grade === '优秀' ? 'bg-emerald-50 text-emerald-600' :
                      row.grade === '合格' ? 'bg-amber-50 text-amber-600' :
                      'bg-blue-50 text-blue-600'
                    ) : ''">
                    <span v-if="getGradeColor(row.grade)" class="pdv-grade-dot" :style="{ background: getGradeColor(row.grade) ?? undefined }" />
                    {{ row.grade }}
                  </span>
                  <span v-else class="text-gray-300">-</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>

        <div v-else class="py-20 text-center">
          <BarChart3 class="w-10 h-10 text-gray-300 mx-auto mb-3" />
          <div class="text-sm text-gray-400">项目发布后可查看成绩统计</div>
        </div>
      </div>

      <!-- ===== 检查配置 Tab ===== -->
      <div v-if="activeTab === 'config'">
        <SectionConfigView :project-id="projectId" :sections="sectionList" :section-tree="sectionTree" :root-section-id="project?.rootSectionId" :root-section-name="rootSectionName" :inspectors="inspectors" />
      </div>

      <!-- ===== 设置 Tab ===== -->
      <!-- ===== 人员与任务 Tab ===== -->
      <div v-if="activeTab === 'team'" class="cfg-section">

        <!-- 待分配任务 -->
        <div v-if="pendingAssignTasks.length > 0" class="cfg-card">
          <div class="cfg-card-title cfg-card-title--with-icon cfg-card-title--mb">
            <ClipboardList class="w-4 h-4" style="color:#f59e0b" />待分配任务
            <span class="pdv-badge-orange">{{ pendingAssignTasks.length }}</span>
          </div>
          <div class="pdv-assign-list">
            <div v-for="task in pendingAssignTasks" :key="task.id" class="pdv-assign-row">
              <span class="text-xs font-medium">{{ task.taskCode }}</span>
              <span class="text-xs text-gray-500">{{ task.taskDate }}</span>
              <span class="text-xs text-gray-400">{{ task.totalTargets }}个目标</span>
              <el-select
                :model-value="null"
                placeholder="指派检查员"
                size="small"
                style="width: 160px"
                :loading="assigningTaskId === task.id"
                @change="(val: any) => {
                  const insp = inspectors.find((i: ProjectInspector) => String(i.userId) === String(val))
                  if (insp) handleAssignTask(task, insp)
                }"
              >
                <el-option v-for="insp in inspectors" :key="insp.userId" :label="insp.userName" :value="insp.userId" />
              </el-select>
            </div>
          </div>
        </div>

        <!-- 检查员管理 -->
        <div class="cfg-card">
          <div class="cfg-card-header cfg-card-header--mb">
            <div class="cfg-card-title cfg-card-title--with-icon">
              <Users class="w-4 h-4" style="color:#1a6dff" />检查员管理
              <span v-if="inspectors.length" class="cfg-count">({{ inspectors.length }} 人)</span>
            </div>
          </div>

          <!-- 添加 -->
          <div class="cfg-add-insp">
            <div class="cfg-hint cfg-hint--mb">添加检查员</div>
            <div class="cfg-add-insp-row">
              <div class="cfg-add-insp-search">
                <el-select v-model="addQuery" filterable remote reserve-keyword :remote-method="searchUsers" :loading="addLoading" placeholder="输入姓名搜索..." class="w-full" size="default" @change="handleAddInspector" clearable>
                  <el-option v-for="u in addResults" :key="Number(u.id)" :label="(u.realName||u.username) + (u.orgUnitName ? ` (${u.orgUnitName})` : '')" :value="Number(u.id)">
                    <div class="cfg-user-option"><span class="cfg-user-name">{{ u.realName || u.username }}</span><span class="cfg-hint">{{ u.orgUnitName || u.username }}</span></div>
                  </el-option>
                </el-select>
              </div>
              <div class="cfg-add-insp-role">
                <el-select v-model="addRole">
                  <el-option v-for="(v,k) in InspectorRoleConfig" :key="k" :label="v.label" :value="k" />
                </el-select>
              </div>
            </div>
          </div>

          <!-- 检查员搜索 -->
          <div v-if="inspectors.length > 4" class="cfg-add-insp" style="padding-top: 8px">
            <input v-model="inspectorFilter" placeholder="按姓名筛选检查员…"
                   class="pdv-insp-search" />
          </div>

          <!-- 列表 (升级版: 含负载饱和度 + 进行中 + 逾期) -->
          <div v-if="inspectors.length === 0" class="cfg-empty">暂无检查员</div>
          <div v-else class="pdv-insp-list">
            <div v-for="insp in filteredInspectors" :key="insp.id"
                 class="pdv-insp-row"
                 :class="{ 'pdv-insp-row--overdue': (inspectorStats.find(s => s.name === insp.userName)?.overdue ?? 0) > 0 }">
              <div class="pdv-insp-avatar">{{ (insp.userName || '?')[0] }}</div>
              <div class="pdv-insp-meta">
                <div class="pdv-insp-name-line">
                  <span class="pdv-insp-name">{{ insp.userName }}</span>
                  <span class="pdv-insp-role">{{ InspectorRoleConfig[insp.role as InspectorRole]?.label }}</span>
                </div>
                <div class="pdv-insp-stats" v-if="!isDraft">
                  <template v-if="inspectorStats.find(s => s.name === insp.userName)">
                    <span class="pdv-stat">
                      分配 <b>{{ inspectorStats.find(s => s.name === insp.userName)!.assigned }}</b>
                    </span>
                    <span class="pdv-stat">
                      完成 <b style="color: var(--insp-pass)">{{ inspectorStats.find(s => s.name === insp.userName)!.completed }}</b>
                    </span>
                    <span class="pdv-stat" v-if="inspectorStats.find(s => s.name === insp.userName)!.active > 0">
                      进行中 <b style="color: var(--insp-info)">{{ inspectorStats.find(s => s.name === insp.userName)!.active }}</b>
                    </span>
                    <span class="pdv-stat pdv-stat--alert" v-if="inspectorStats.find(s => s.name === insp.userName)!.overdue > 0">
                      逾期 <b>{{ inspectorStats.find(s => s.name === insp.userName)!.overdue }}</b>
                    </span>
                  </template>
                  <span v-else class="pdv-stat-empty">暂无任务</span>
                </div>
                <!-- 负载饱和度条 -->
                <div v-if="!isDraft && inspectorStats.find(s => s.name === insp.userName)" class="pdv-insp-bar">
                  <div class="pdv-insp-bar-bg">
                    <div class="pdv-insp-bar-done"
                         :style="{ width: ((inspectorStats.find(s => s.name === insp.userName)!.completed / Math.max(inspectorStats.find(s => s.name === insp.userName)!.assigned, 1)) * 100) + '%' }" />
                  </div>
                </div>
              </div>
              <el-tag :type="insp.isActive ? 'success' : 'info'" size="small" round effect="plain">{{ insp.isActive ? '启用' : '禁用' }}</el-tag>
              <el-button link type="danger" size="small" @click="handleRemoveInspector(insp)"><Trash2 class="w-3.5 h-3.5" /></el-button>
            </div>
            <div v-if="inspectorFilter && filteredInspectors.length === 0" class="cfg-empty" style="padding: 16px">
              没有匹配 "{{ inspectorFilter }}" 的检查员
            </div>
          </div>
        </div>

        <!-- 审核待办 -->
        <div v-if="pendingReviewCount > 0" class="cfg-card">
          <div class="cfg-card-title cfg-card-title--with-icon cfg-card-title--mb">
            <ClipboardCheck class="w-4 h-4" style="color:#1a6dff" />待审核任务
            <span class="pdv-badge-red">{{ pendingReviewCount }}</span>
          </div>
          <div class="cfg-review-list">
            <div v-for="task in pendingReviewTasks" :key="task.id" class="cfg-review-item">
              <div class="cfg-review-info">
                <div class="cfg-insp-name">{{ task.taskCode }}</div>
                <div class="cfg-hint">检查员: {{ task.inspectorName || '-' }} · {{ task.updatedAt?.substring(0, 16) || '-' }}</div>
              </div>
              <div class="cfg-review-actions">
                <el-tag type="warning" size="small" round>待审核</el-tag>
                <el-button type="success" size="small" @click="handleApproveTask(task)"><Check class="w-3.5 h-3.5 mr-0.5" />通过</el-button>
                <el-button type="danger" size="small" plain @click="handleRejectTask(task)"><X class="w-3.5 h-3.5 mr-0.5" />驳回</el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 无待办时 -->
        <div v-if="pendingAssignTasks.length === 0 && pendingReviewCount === 0 && inspectors.length > 0" class="cfg-card">
          <div class="cfg-empty" style="padding: 16px">
            <CheckCircle class="w-5 h-5 text-green-400" style="margin: 0 auto 6px" />
            所有任务已分配，无待审核项目
          </div>
        </div>
      </div>

      <div v-if="activeTab === 'settings'" class="cfg-section">

        <!-- 锁定提示 -->
        <div v-if="!isDraft" class="pdv-lock-notice">
          <Lock class="w-4 h-4 text-amber-500 mt-0.5 flex-shrink-0" />
          <div>
            <div class="cfg-lock-title">部分配置已锁定</div>
            <div class="cfg-lock-desc">发布后，检查范围和根分区不可更改。项目名称和运营配置可随时调整。</div>
          </div>
        </div>

        <!-- 基本信息 -->
        <div class="cfg-card">
          <div class="cfg-card-title">基本信息</div>
          <div class="cfg-field">
            <label class="cfg-label">项目名称</label>
            <input
              v-model="cf.projectName"
              type="text"
              class="cfg-input"
              :disabled="isArchived"
              placeholder="输入项目名称"
            />
          </div>
          <div v-if="rootSectionName" class="cfg-field cfg-field--mt">
            <label class="cfg-label">检查模板</label>
            <div class="cfg-readonly-text">{{ rootSectionName }}</div>
          </div>
        </div>

        <!-- 检查范围 -->
        <div class="cfg-card" :class="{ 'cfg-locked': !isDraft }">
          <div class="cfg-card-header">
            <div class="cfg-card-title">检查范围</div>
            <Lock v-if="!isDraft" class="w-3.5 h-3.5 cfg-lock-icon" />
          </div>
          <div class="cfg-desc">选择哪些组织单元参与本次检查，系统将根据分区的目标类型自动派生具体检查对象。</div>
          <div class="cfg-field cfg-field--mt">
            <label class="cfg-label">检查对象 <span v-if="isDraft" class="cfg-req">*</span></label>
            <div v-if="loadingOrgTree" class="cfg-org-list cfg-org-loading">加载中...</div>
            <div v-else-if="orgTree.length === 0" class="cfg-org-list cfg-org-loading">暂无组织单元</div>
            <div v-else-if="!isDraft" class="cfg-org-readonly">
              <span v-if="cf.scopeIds.length === 0" class="cfg-readonly-text">未选择</span>
              <template v-else>
                <span v-for="id in cf.scopeIds" :key="id" class="cfg-scope-tag">
                  {{ scopeOrgNames[cf.scopeIds.indexOf(id)] || id }}
                </span>
              </template>
            </div>
            <template v-else>
              <div class="flex items-center gap-2 mb-1.5">
                <el-input
                  v-model="scopeFilterText"
                  placeholder="搜索组织..."
                  size="small"
                  clearable
                  style="width: 200px"
                />
                <el-button size="small" link type="primary" @click="scopeTreeRef?.setCheckedKeys([]); cf.scopeIds = []">清空</el-button>
              </div>
              <div class="cfg-org-list">
                <el-tree
                  ref="scopeTreeRef"
                  :data="orgTree"
                  :props="{ children: 'children', label: 'unitName' }"
                  show-checkbox
                  check-strictly
                  node-key="id"
                  :default-checked-keys="cf.scopeIds.map(Number)"
                  :filter-node-method="filterScopeNode"
                  default-expand-all
                  @check="handleScopeCheckChange"
                />
              </div>
            </template>
          </div>
          <div v-if="cf.scopeIds.length > 0 && isDraft" class="cfg-hint">
            已选 {{ cf.scopeIds.length }} 个组织单元，发布后将自动匹配下属场所、部门等目标
          </div>
        </div>

        <!-- 时间范围 -->
        <div class="cfg-card" :class="{ 'cfg-locked': !isDraft }">
          <div class="cfg-card-header">
            <div class="cfg-card-title">时间范围</div>
            <Lock v-if="!isDraft" class="w-3.5 h-3.5 cfg-lock-icon" />
          </div>
          <div class="cfg-desc">设置检查的起止时间。具体调度频率在「检查计划」标签页中配置。</div>
          <div class="cfg-row2">
            <div class="cfg-field">
              <label class="cfg-label">开始日期 <span v-if="isDraft" class="cfg-req">*</span></label>
              <input
                v-model="cf.startDate"
                type="date"
                class="cfg-input"
                :disabled="!isDraft"
              />
            </div>
            <div class="cfg-field">
              <label class="cfg-label">结束日期</label>
              <input
                v-model="cf.endDate"
                type="date"
                class="cfg-input"
                :disabled="!isDraft"
              />
            </div>
          </div>
        </div>

        <!-- 运营配置 -->
        <div class="cfg-card">
          <div class="cfg-card-title">运营配置</div>
          <div class="cfg-desc">以下配置可随时调整，不影响已生成的任务结构。</div>
          <div class="cfg-row3">
            <div class="cfg-field">
              <label class="cfg-label">任务分配方式</label>
              <select v-model="cf.assignmentMode" class="cfg-select" :disabled="isArchived">
                <option v-for="(c,k) in AssignmentModeConfig" :key="k" :value="k">{{ c.label }}</option>
              </select>
              <div class="cfg-hint">{{ { FREE:'任何检查员可自由领取任务', ASSIGNED:'管理员手动指派给特定检查员' }[cf.assignmentMode] || '' }}</div>
            </div>
            <div class="cfg-field">
              <label class="cfg-label">提交后审核</label>
              <div class="cfg-toggle-row">
                <el-switch v-model="cf.reviewRequired" :disabled="isArchived" />
              </div>
              <div class="cfg-hint">{{ cf.reviewRequired ? '检查员提交后需审核员审批' : '提交后直接生效，无需审核' }}</div>
            </div>
            <div class="cfg-field">
              <label class="cfg-label">自动发布结果</label>
              <div class="cfg-toggle-row">
                <el-switch v-model="cf.autoPublish" :disabled="isArchived" />
              </div>
              <div class="cfg-hint">{{ cf.autoPublish ? '审核通过后自动发布分数' : '需手动发布检查结果' }}</div>
            </div>
          </div>
        </div>

        <!-- 高级评分设置 -->
        <div class="cfg-card">
          <div class="cfg-card-title">高级评分设置</div>
          <div class="cfg-desc">配置本次检查的评分方式。</div>
          <div class="cfg-row2">
            <div class="cfg-field">
              <label class="cfg-label">评分模式</label>
              <select v-model="cf.evaluationMode" class="cfg-select" :disabled="isArchived">
                <option value="SINGLE">单人评分</option>
                <option value="MULTI">多人评分</option>
              </select>
              <div class="cfg-hint">{{ cf.evaluationMode === 'SINGLE' ? '每个目标由一名检查员独立检查' : '每个目标由多名检查员分别检查，结果合并' }}</div>
            </div>
            <div v-if="cf.evaluationMode === 'MULTI'" class="cfg-field">
              <label class="cfg-label">多人合并方式</label>
              <select v-model="cf.multiRaterMode" class="cfg-select" :disabled="isArchived">
                <option value="AVERAGE">取平均</option>
                <option value="MEDIAN">取中位数</option>
                <option value="MAX">取最高</option>
                <option value="MIN">取最低</option>
                <option value="CONSENSUS">共识模式</option>
              </select>
            </div>
          </div>
          <div class="cfg-toggle-group">
            <label class="cfg-toggle-item">
              <el-switch v-model="cf.trendEnabled" :disabled="isArchived" size="small" />
              <span class="cfg-toggle-name">趋势因子</span>
              <span class="cfg-hint">根据历史趋势自动加减分</span>
            </label>
            <label class="cfg-toggle-item">
              <el-switch v-model="cf.decayEnabled" :disabled="isArchived" size="small" />
              <span class="cfg-toggle-name">分数衰减</span>
              <span class="cfg-hint">分数随时间递减</span>
            </label>
            <label class="cfg-toggle-item">
              <el-switch v-model="cf.calibrationEnabled" :disabled="isArchived" size="small" />
              <span class="cfg-toggle-name">尺度校准</span>
              <span class="cfg-hint">校正不同检查员的评分尺度差异</span>
            </label>
          </div>
        </div>

        <!-- 操作区 -->
        <div class="cfg-card">
          <div class="cfg-card-title cfg-card-title--mb">项目操作</div>
          <div class="cfg-ops-row">
            <el-button v-if="project?.status === 'PUBLISHED'" type="warning" size="small" plain @click="handlePause" round>
              <Pause class="w-3.5 h-3.5 mr-1" />暂停项目
            </el-button>
            <el-button v-if="project?.status === 'PAUSED'" type="success" size="small" plain @click="handleResume" round>
              <Play class="w-3.5 h-3.5 mr-1" />恢复项目
            </el-button>
            <el-button v-if="['PUBLISHED','PAUSED'].includes(project?.status || '')" size="small" plain @click="handleComplete" round>
              <CheckCircle class="w-3.5 h-3.5 mr-1" />完结项目
            </el-button>
            <el-button v-if="project?.status === 'COMPLETED'" type="info" size="small" plain @click="handleArchive" round>归档项目</el-button>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
/* ========== Layout ========== */
.pdv {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

/* ========== Header (与 Tabs 视觉整合, 去掉硬分隔线) ========== */
.pdv-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: var(--insp-sp-4);
  margin-bottom: var(--insp-sp-3);
}
.pdv-header-left {
  display: flex;
  align-items: flex-start;
  gap: var(--insp-sp-3);
}
.pdv-back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  background: var(--insp-bg-surface);
  color: var(--insp-ink-secondary);
  cursor: pointer;
  transition: background var(--insp-t-fast), border-color var(--insp-t-fast), color var(--insp-t-fast);
  flex-shrink: 0;
  margin-top: 4px;
}
.pdv-back-btn:hover {
  background: var(--insp-bg-subtle);
  border-color: var(--insp-border-strong);
  color: var(--insp-ink-primary);
}
.pdv-head-block { display: flex; flex-direction: column; gap: 4px; }
.pdv-title {
  display: flex;
  align-items: baseline;
  gap: var(--insp-sp-3);
  font-family: var(--insp-font-display);
  font-size: 28px;
  font-weight: 500;
  letter-spacing: var(--insp-tracking-display);
  color: var(--insp-ink-primary);
  line-height: 1.15;
  margin-top: 2px;
}
.pdv-title__name { display: inline-block; }
.pdv-status-dot {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 20px;
}
.pdv-status-dot.status-draft     { background: #f3f4f6; color: #6b7280; }
.pdv-status-dot.status-published { background: #dcfce7; color: #16a34a; }
.pdv-status-dot.status-paused    { background: #fef9c3; color: #ca8a04; }
.pdv-status-dot.status-completed { background: #dbeafe; color: #2563eb; }
.pdv-status-dot.status-archived  { background: #f3f4f6; color: #9ca3af; }
.pdv-subtitle {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #9ca3af;
  margin-top: 3px;
}
.pdv-subtitle-sep { color: #d1d5db; }
.pdv-header-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }

/* ========== Pill Tabs (集成在 header 下方, 同卡片视觉) ========== */
.pdv-tabs {
  display: flex;
  gap: 4px;
  background: #f3f4f6;
  border-radius: 10px;
  padding: 3px;
  margin-bottom: 16px;
  width: fit-content;
  position: relative;
  top: -4px;
}
.pdv-tab {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.pdv-tab:hover { color: #374151; background: rgba(255,255,255,0.6); }
.pdv-tab.active { color: #1a6dff; background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,0.1); font-weight: 600; }
.pdv-tab-dot { width: 6px; height: 6px; border-radius: 50%; background: #f59e0b; flex-shrink: 0; }
.pdv-tab-badge {
  font-size: 10px; min-width: 16px; height: 16px; line-height: 16px;
  text-align: center; border-radius: 8px;
  background: #ef4444; color: #fff; font-weight: 700;
  padding: 0 4px; flex-shrink: 0;
}

/* ========== Body ========== */
.pdv-body { min-height: 300px; }

/* ========== Empty state ========== */
.pdv-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  text-align: center;
}

/* ========== Overview: Stat Row ========== */
.pdv-stat-row {
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px 24px;
  margin-bottom: 16px;
  gap: 0;
}
.pdv-stat-card {
  flex: 1;
  text-align: center;
}
.pdv-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
}
.pdv-stat-label {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
}
.pdv-stat-divider {
  width: 1px;
  height: 32px;
  background: #e5e7eb;
  flex-shrink: 0;
}

/* ========== Progress ========== */
.pdv-progress-row {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 14px 20px;
  margin-bottom: 16px;
}
.pdv-progress-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.pdv-progress-track {
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}
.pdv-progress-fill {
  height: 100%;
  background: #1a6dff;
  border-radius: 4px;
  transition: width 0.4s ease;
}
.pdv-progress-stats {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  font-size: 12px;
  color: #6b7280;
}

/* ========== Two-column layout ========== */
.pdv-two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 0;
}

/* ========== Cards ========== */
.pdv-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
}
.pdv-card-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}
.pdv-card-empty {
  padding: 24px 0;
  text-align: center;
  font-size: 12px;
  color: #d1d5db;
}

/* ========== Section Score List ========== */
.pdv-score-list { display: flex; flex-direction: column; gap: 8px; }
.pdv-score-item { display: flex; align-items: center; gap: 8px; }
.pdv-score-name {
  font-size: 12px;
  color: #4b5563;
  width: 110px;
  flex-shrink: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pdv-score-bar-wrap { display: flex; align-items: center; gap: 6px; flex: 1; }
.pdv-score-bar-track { flex: 1; height: 6px; background: #f3f4f6; border-radius: 3px; overflow: hidden; }
.pdv-score-bar-fill { height: 100%; border-radius: 3px; transition: width 0.3s ease; }
.pdv-score-val { font-size: 12px; font-weight: 600; width: 36px; text-align: right; flex-shrink: 0; }

/* ========== Recent Tasks ========== */
.pdv-recent-list { display: flex; flex-direction: column; gap: 8px; }
.pdv-recent-item {
  padding: 8px 10px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}
.pdv-recent-meta { display: flex; align-items: center; justify-content: space-between; margin-bottom: 2px; }
.pdv-recent-date { font-size: 12px; font-weight: 500; color: #374151; }
.pdv-recent-inspector { font-size: 11px; color: #9ca3af; }
.pdv-recent-code { font-size: 11px; }

.pdv-review-alert {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 10px;
  padding: 6px 10px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 8px;
  font-size: 12px;
  color: #9a3412;
}
.pdv-badge-orange {
  font-size: 11px; padding: 1px 7px; border-radius: 8px;
  background: #fffbeb; color: #f59e0b; font-weight: 600;
}
.pdv-assign-list { display: flex; flex-direction: column; gap: 6px; }
.pdv-assign-row {
  display: flex; align-items: center; gap: 8px;
  padding: 4px 0;
}
.pdv-review-link {
  margin-left: auto;
  font-size: 12px;
  font-weight: 500;
  color: #1a6dff;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}
.pdv-review-link:hover { text-decoration: underline; }

/* ========== Result Stats ========== */
.pdv-result-stats {
  display: flex;
  align-items: center;
  gap: 0;
}
.pdv-result-item { flex: 1; text-align: center; }
.pdv-result-val { font-size: 20px; font-weight: 700; color: #1f2937; }
.pdv-result-lbl { font-size: 11px; color: #9ca3af; margin-top: 2px; }
.pdv-result-divider { width: 1px; height: 28px; background: #e5e7eb; flex-shrink: 0; }

/* ========== Badge ========== */
.pdv-badge-red {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: #ef4444;
  border-radius: 9px;
}

/* ========== Settings tab layout ========== */
.cfg-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ========== Config card styles (settings tab) ========== */
.cfg-card {
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 10px;
  padding: 18px 20px;
  transition: border-color 0.2s;
}
.cfg-card:hover { border-color: #d1d5db; }
.cfg-card.cfg-locked {
  background: #fafafa;
  border-style: dashed;
}
.cfg-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.cfg-card-header--mb { margin-bottom: 12px; }
.cfg-card-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
}
.cfg-card-title--with-icon {
  display: flex;
  align-items: center;
  gap: 6px;
}
.cfg-card-title--mb { margin-bottom: 12px; }
.cfg-lock-icon { color: #d1d5db; }
.cfg-count {
  font-size: 11px;
  font-weight: 400;
  color: #9ca3af;
}
.cfg-desc {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
  line-height: 1.5;
}
.cfg-field { display: flex; flex-direction: column; }
.cfg-field--mt { margin-top: 12px; }
.cfg-label {
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 4px;
}
.cfg-req { color: #f87171; }

/* ========== Native inputs ========== */
.cfg-input {
  height: 32px;
  padding: 0 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  color: #1f2937;
  background: #fff;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
  width: 100%;
}
.cfg-input:focus { border-color: #1a6dff; }
.cfg-input:disabled { background: #f9fafb; color: #9ca3af; cursor: not-allowed; }

.cfg-select {
  height: 32px;
  padding: 0 28px 0 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  color: #1f2937;
  background: #fff url("data:image/svg+xml,%3Csvg width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%239ca3af' stroke-width='2' stroke-linecap='round' stroke-linejoin='round' xmlns='http://www.w3.org/2000/svg'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E") no-repeat right 8px center;
  -webkit-appearance: none;
  appearance: none;
  outline: none;
  cursor: pointer;
  transition: border-color 0.15s;
  box-sizing: border-box;
  width: 100%;
}
.cfg-select:focus { border-color: #1a6dff; }
.cfg-select:disabled { background-color: #f9fafb; color: #9ca3af; cursor: not-allowed; }

/* ========== Grid layouts ========== */
.cfg-row2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 12px;
}
.cfg-row3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 12px;
}

/* ========== Readonly display ========== */
.cfg-readonly-text {
  font-size: 13px;
  color: #374151;
  padding: 4px 0;
}
.cfg-org-readonly {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 6px 0;
}
.cfg-scope-tag {
  display: inline-block;
  padding: 2px 8px;
  background: #eff5ff;
  color: #1a6dff;
  border-radius: 4px;
  font-size: 12px;
}

/* ========== Org unit tree ========== */
.cfg-org-list {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
  max-height: 320px;
  overflow-y: auto;
  padding: 4px 0;
}
.cfg-org-loading {
  padding: 16px;
  text-align: center;
  font-size: 12px;
  color: #9ca3af;
}

/* ========== Toggle ========== */
.cfg-toggle-row {
  padding: 4px 0;
}
.cfg-toggle-group {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
.cfg-toggle-item {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}
.cfg-toggle-name {
  font-size: 13px;
  color: #374151;
}

/* ========== Hint text ========== */
.cfg-hint {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
  line-height: 1.4;
}
.cfg-hint--mb { margin-top: 0; margin-bottom: 6px; font-weight: 500; color: #6b7280; }

/* ========== Save bar ========== */
.cfg-save-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 4px;
}

/* ========== Inspector management ========== */
.cfg-add-insp {
  margin-bottom: 14px;
  padding: 10px 12px;
  background: rgba(239, 245, 255, 0.5);
  border: 1px solid #dbeafe;
  border-radius: 8px;
}
.cfg-add-insp-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.cfg-add-insp-search { flex: 1; }
.cfg-add-insp-role { width: 110px; }
.cfg-user-option { display: flex; align-items: center; justify-content: space-between; }
.cfg-user-name { font-size: 13px; font-weight: 500; color: #1f2937; }

.cfg-workload-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 14px;
}
.cfg-workload-item {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 8px 10px;
}
.cfg-workload-name {
  font-size: 12px;
  font-weight: 500;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}
.cfg-workload-stats { display: flex; gap: 10px; font-size: 11px; }
.cfg-workload-val { font-weight: 700; color: #1f2937; }
.cfg-workload-val--done { color: #16a34a; }

.cfg-empty {
  padding: 20px 0;
  text-align: center;
  font-size: 12px;
  color: #9ca3af;
}

/* ===== 人员与任务 升级版 ===== */
.pdv-insp-search {
  width: 100%;
  height: 28px;
  padding: 0 10px;
  font-size: 12px; font-family: inherit;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-sm);
  background: var(--insp-bg-surface);
}
.pdv-insp-search:focus { outline: none; border-color: var(--insp-accent); box-shadow: 0 0 0 3px var(--insp-accent-paler); }

.pdv-insp-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 8px;
  margin-top: 8px;
}
.pdv-insp-row {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  transition: border-color var(--insp-t-fast);
}
.pdv-insp-row:hover { border-color: var(--insp-accent-pale); }
.pdv-insp-row--overdue {
  background: var(--insp-fail-pale);
  border-color: var(--insp-fail-border);
}
.pdv-insp-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: linear-gradient(135deg, #60a5fa, #2563eb);
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; flex-shrink: 0;
}
.pdv-insp-meta { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.pdv-insp-name-line { display: flex; align-items: baseline; gap: 8px; }
.pdv-insp-name { font-size: 13px; font-weight: 600; color: var(--insp-ink-primary); }
.pdv-insp-role { font-size: 10px; color: var(--insp-ink-tertiary); }
.pdv-insp-stats {
  display: flex; flex-wrap: wrap; gap: 8px;
  font-size: 11px; color: var(--insp-ink-tertiary);
  font-family: var(--insp-font-mono);
}
.pdv-insp-stats .pdv-stat b { color: var(--insp-ink-primary); font-weight: 700; }
.pdv-insp-stats .pdv-stat--alert { color: var(--insp-fail); font-weight: 600; }
.pdv-insp-stats .pdv-stat--alert b { color: var(--insp-fail); }
.pdv-stat-empty { font-size: 11px; color: var(--insp-ink-quaternary); font-style: italic; }
.pdv-insp-bar { margin-top: 2px; }
.pdv-insp-bar-bg {
  height: 3px;
  background: var(--insp-bg-sunken);
  border-radius: 2px;
  overflow: hidden;
}
.pdv-insp-bar-done {
  height: 100%;
  background: var(--insp-pass);
  transition: width var(--insp-t-medium);
}

.cfg-insp-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.cfg-insp-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}
.cfg-insp-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #60a5fa, #2563eb);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}
.cfg-insp-info { flex: 1; min-width: 0; }
.cfg-insp-name { font-size: 13px; font-weight: 500; color: #1f2937; }

/* ========== Review list ========== */
.cfg-review-list { display: flex; flex-direction: column; gap: 8px; }
.cfg-review-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 10px;
}
.cfg-review-info { flex: 1; min-width: 0; }
.cfg-review-actions { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }

/* ========== Ops row ========== */
.cfg-ops-row { display: flex; flex-wrap: wrap; gap: 8px; }

/* ========== Lock notice ========== */
.pdv-lock-notice {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 10px;
}
.cfg-lock-title {
  font-size: 13px;
  font-weight: 500;
  color: #92400e;
}
.cfg-lock-desc {
  font-size: 11px;
  color: #b45309;
  margin-top: 2px;
}

/* ========== Scores tab: top rank highlight ========== */
:deep(.top-rank-row) {
  background-color: #fafbff !important;
}

/* ========== Grade badge with scheme colors ========== */
.pdv-grade-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 20px;
  border: 1px solid transparent;
}
.pdv-grade-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
