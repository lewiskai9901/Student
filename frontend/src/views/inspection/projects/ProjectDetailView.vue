<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Play, Pause, CheckCircle, Send, Save, Users, Settings, BarChart3, ClipboardList, Lock,
  ClipboardCheck, ListTree, LayoutDashboard, Copy,
  AlertTriangle, AlertCircle, Info,
} from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/inspection/inspExecutionStore'
import { useAuthStore } from '@/stores/auth'
import {
  ProjectStatusConfig, type ProjectStatus, type InspectorRole,
  AssignmentModeConfig, type AssignmentMode, type ScopeType,
} from '@/types/insp/enums'
import type { InspProject, ProjectInspector, InspTask, InspSubmission } from '@/types/insp/project'
import { inspProjectApi, updateOperationalConfig } from '@/api/inspection/project'
import { http } from '@/utils/request'
import { getTasks, assignTask } from '@/api/inspection/task'
import { getSubmissions } from '@/api/inspection/submission'
import { getSections } from '@/api/inspection/template'
import { getProfiles } from '@/api/inspection/scoring'
import type { ScoringProfile } from '@/types/insp/scoring'
import { getSimpleUserList, getUser } from '@/api/user'
import { getOrgUnitTree } from '@/api/organization'
import { universalPlaceApi } from '@/api/universalPlace'
import type { PlaceTreeNode } from '@/types/universalPlace'
import type { OrgUnitTreeNode } from '@/api/organization'
import type { SimpleUser } from '@/types/user'
import { getRootSection } from '@/api/inspection/template'
import SectionConfigView from './components/SectionConfigView.vue'
import ProjectCorrectiveStrategy from './components/ProjectCorrectiveStrategy.vue'
import ScoringSectionCard from './components/ScoringSectionCard.vue'
import { buildSectionTree, type SectionTreeNode } from '@/utils/sectionTree'
import IndicatorScoreView from './components/IndicatorScoreView.vue'
import OrgScoreRanking from './components/OrgScoreRanking.vue'
import EvaluationConfigView from './components/EvaluationConfigView.vue'
import EvaluationResultsView from './components/EvaluationResultsView.vue'
import InspButton from '../shared/InspButton.vue'
import InspChip from '../shared/InspChip.vue'
import InspSpinner from '../shared/InspSpinner.vue'
// 2026-05-23: 人员与任务 Tab 重构 — 旧的 3 张卡片 (待审核/待分配/检查员管理) 替换为以人为中心的工作台
import TeamTab from './team/TeamTab.vue'

const route = useRoute()
const router = useRouter()
const store = useInspExecutionStore()
const auth = useAuthStore()
const projectId = route.params.id as string

// 当前登录用户姓名 — 用于审核/领取等操作署名, 不再硬编码 'admin'/'当前用户'
const currentUserName = computed(() =>
  auth.user?.realName || auth.user?.username || ''
)

// ========== State ==========
const loading = ref(false)
// P0 #2: 加载失败时给可见错态, 用户能重试; 不再只 ElMessage 一闪而过
const loadError = ref<string | null>(null)
const project = ref<InspProject | null>(null)
const inspectors = ref<ProjectInspector[]>([])
const allTasks = ref<InspTask[]>([])
const allSubmissions = ref<InspSubmission[]>([])
// P1 #12: 部分子任务的提交记录加载失败时, 成绩统计会悄悄缺数据 — 用此标志在成绩 Tab 顶部明示
const submissionsLoadFailedCount = ref(0)
const sectionNameMap = ref<Map<LongId, { name: string; targetType?: string }>>(new Map())
const sectionTree = ref<SectionTreeNode[]>([])
const sectionList = computed(() => [...sectionNameMap.value.entries()].map(([id, info]) => ({ id, sectionName: info.name, targetType: info.targetType })))
// 评分方案列表 — 仅供"评分方案"卡片显示用 (默认评分方案下拉已删除, Phase 1 评级引擎重构)
const scoringProfiles = ref<ScoringProfile[]>([])
const creatorName = ref('')
const rootSectionName = ref('')
const scopeOrgNames = ref<string[]>([])
const orgTree = ref<OrgUnitTreeNode[]>([])
const loadingOrgTree = ref(false)
// V20260524 Bug#12: 项目详情页设置 Tab 范围 picker 也按 scopeType 切换
const placeTree = ref<PlaceTreeNode[]>([])
const userList = ref<SimpleUser[]>([])
// Tabs
// L4 2026-05-26: 支持 ?tab=xxx 直达 (评分配置 → 评级 跨页跳转用)
const VALID_TABS = ['overview', 'config', 'team', 'scores', 'scoring', 'evaluation', 'corrective', 'settings']
const initialTab = typeof route.query.tab === 'string' && VALID_TABS.includes(route.query.tab)
  ? route.query.tab
  : 'overview'
const activeTab = ref(initialTab)
// 评级 Tab 的子页 (config / results)
const evalSubTab = ref<'config' | 'results'>('config')
// EvaluationConfigView 行内点"查看结果"会把 indicatorId 传到 results 子页预选
const focusedIndicatorId = ref<LongId | null>(null)
function handleViewResults(indicatorId: LongId) {
  focusedIndicatorId.value = indicatorId
  evalSubTab.value = 'results'
}

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

// 整改强度 → 自动建单门槛 (与后端 ProjectCorrectivePolicy.legacyFromStrictness 对齐)
const autoCreateLevelFromStrictness = computed(() => {
  switch (cf.value?.correctiveStrictness) {
    case 'STRICT':  return 'LOW'
    case 'NORMAL':  return 'NONE'
    case 'LENIENT': return 'NONE'
    case 'OFF':     return 'NONE'
    default: return 'NONE'
  }
})

// ════ 整改判定策略 — sev 阈值预设 (与后端 SeverityThresholds 保持一致) ════
const STRICTNESS_THRESHOLD_DEFAULTS: Record<string, { high: number; medium: number; low: number }> = {
  STRICT:  { high: 0.5, medium: 0.3, low: 0.1 },
  NORMAL:  { high: 0.8, medium: 0.5, low: 0.3 },
  LENIENT: { high: 0.9, medium: 0.7, low: 0.5 },
  OFF:     { high: 0.8, medium: 0.5, low: 0.3 },
}

// 配置表单
const cf = ref({ scopeType: 'ORG', scopeIds: [] as string[], startDate: '', endDate: '', assignmentMode: 'FREE', reviewRequired: true, autoPublish: false, projectName: '',
  // V108: 检查模式
  inspectionMode: 'PLANNED' as 'PLANNED'|'HYBRID'|'SPOT_CHECK'|'SELF_AUDIT'|'EMERGENCY',
  // 整改判定策略: 4 档预设 + 可选自定义阈值
  correctiveStrictness: 'NORMAL' as 'STRICT'|'NORMAL'|'LENIENT'|'OFF',
  useCustomThresholds: false as boolean,                  // 是否启用自定义阈值
  customThresholdMode: 'RATING_SCALE' as 'RATING_SCALE'|'DIRECT_SCORE'|'DEDUCTION'|'LEVEL',
  customRatingMax: 5 as number,                           // 星级满分
  customDirectMax: 10 as number,                          // 直接打分满分
  correctiveThresholdHigh: null as number | null,
  correctiveThresholdMedium: null as number | null,
  correctiveThresholdLow: null as number | null,
  correctiveDeadlineHigh: null as number | null,
  correctiveDeadlineMedium: null as number | null,
  correctiveDeadlineLow: null as number | null,
  allowAdHoc: false, allowSelfCheck: false, adHocQuotaPerInspector: null as number | null,
})

// 范围树
const scopeTreeRef = ref<any>(null)
const scopeFilterText = ref('')

function handleScopeCheckChange() {
  const checked = scopeTreeRef.value?.getCheckedKeys(false) ?? []
  cf.value.scopeIds = checked.map(String)
}

function filterScopeNode(value: string, data: any): boolean {
  if (!value) return true
  // V20260524 Bug#12: 同时匹配 unitName / placeName / label
  return (data.unitName || data.placeName || data.label || '').includes(value)
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

// ========== A 级 KPI 集 (从 2 个升级到 8 个) ==========
// P1 #12: today 不再是模块加载时的 const, 改 computed 跨日刷新
const today = computed(() => new Date().toISOString().slice(0, 10))
const richKpis = computed(() => {
  const t = filteredTasks.value
  const subs = filteredSubmissions.value
  const todayStr = today.value
  const overdue = t.filter(x =>
    !['REVIEWED','PUBLISHED','CANCELLED','EXPIRED'].includes(x.status) &&
    x.taskDate && (((x as any).extendedTo || x.taskDate) < todayStr)
  ).length
  const reviewed = t.filter(x => ['REVIEWED','PUBLISHED'].includes(x.status)).length
  const pendingReview = t.filter(x => x.status === 'SUBMITTED').length
  const submittedSubs = subs.filter(s => ['COMPLETED','SUBMITTED','VERIFIED'].includes(s.status))
  const scores = submittedSubs.map(s => Number(s.finalScore ?? 0)).filter(n => !isNaN(n))
  const avgScore = scores.length === 0 ? 0 : scores.reduce((a, b) => a + b, 0) / scores.length
  const passed = submittedSubs.filter(s => Number(s.finalScore ?? 0) >= 60).length
  const passRate = submittedSubs.length === 0 ? 0 : Math.round(passed / submittedSubs.length * 100)
  const completionRate = taskStats.value.total === 0 ? 0
    : Math.round(taskStats.value.done / taskStats.value.total * 100)
  return {
    total: taskStats.value.total,
    done: taskStats.value.done,
    active: taskStats.value.active,
    overdue,
    pendingReview,
    reviewed,
    avgScore,
    passRate,
    completionRate,
    // P1 #6: 标志位 — 无任何已提交时模板用 — 占位避免 0.0 / 0% 误导
    hasScores: submittedSubs.length > 0,
  }
})

// 顶部告警条 — 需要管理员关注的事项汇总
// P0 #3: icon 字段不再用空字符串, 改为 type→lucide component 映射, 模板用 <component :is="a.icon" />
const overviewAlerts = computed(() => {
  type AlertKind = 'warn' | 'info' | 'fail'
  const iconFor = (t: AlertKind) => t === 'fail' ? AlertCircle : t === 'warn' ? AlertTriangle : Info
  const out: { type: AlertKind; icon: any; text: string; action?: { label: string; tab: string } }[] = []
  if (richKpis.value.overdue > 0) {
    out.push({
      type: 'fail', icon: iconFor('fail'),
      text: `${richKpis.value.overdue} 个任务已逾期未提交`,
      action: { label: '查看', tab: 'team' }
    })
  }
  if (richKpis.value.pendingReview > 0) {
    out.push({
      type: 'warn', icon: iconFor('warn'),
      text: `${richKpis.value.pendingReview} 个任务等待审核`,
      action: { label: '去审核', tab: 'team' }
    })
  }
  if (pendingAssignTasks.value.length > 0) {
    out.push({
      type: 'info', icon: iconFor('info'),
      text: `${pendingAssignTasks.value.length} 个任务尚未分配检查员`,
      action: { label: '分配', tab: 'team' }
    })
  }
  return out
})

// 7 天滚动趋势 (按 taskDate)
const weeklyTrend = computed(() => {
  const days: { date: string; label: string; total: number; done: number }[] = []
  const now = new Date()
  for (let i = 6; i >= 0; i--) {
    const d = new Date(now)
    d.setDate(now.getDate() - i)
    const iso = d.toISOString().slice(0, 10)
    const label = `${d.getMonth() + 1}/${d.getDate()}`
    const dayTasks = filteredTasks.value.filter(t => t.taskDate === iso)
    days.push({
      date: iso, label,
      total: dayTasks.length,
      done: dayTasks.filter(t => ['REVIEWED','PUBLISHED','SUBMITTED','UNDER_REVIEW'].includes(t.status)).length,
    })
  }
  return days
})
const weeklyTrendMax = computed(() => Math.max(1, ...weeklyTrend.value.map(d => d.total)))

// ========== 待分配任务 ==========
const pendingAssignTasks = computed(() => filteredTasks.value.filter(t => t.status === 'PENDING' && !t.inspectorId))
const assigningTaskId = ref<LongId | null>(null)
// P0 #14: 给 el-select 强制 re-render — 取消确认后内部状态残留, 同人不能再选
const assignSelectKey = ref(0)

async function handleAssignTask(task: InspTask, inspector: ProjectInspector) {
  // P1 #11: 选检查员后二次确认, 避免误点 el-select 即刻指派
  try {
    await ElMessageBox.confirm(
      `将任务「${task.taskCode}」指派给 ${inspector.userName}？`,
      '确认指派', { type: 'info' }
    )
  } catch {
    // P0 #14: 用户取消时刷 key, 让 el-select 恢复空 — 否则下次选同人不触发 @change
    assignSelectKey.value++
    await nextTick()
    return
  }
  try {
    assigningTaskId.value = task.id
    await assignTask(task.id, { inspectorId: inspector.userId, inspectorName: inspector.userName })
    ElMessage.success(`已分配给 ${inspector.userName}`)
    await loadProject()
  } catch (e: any) {
    ElMessage.error('分配失败: ' + (e?.message || '未知错误'))
  } finally {
    assigningTaskId.value = null
    assignSelectKey.value++
    await nextTick()
  }
}

// ========== 审核 ==========
const pendingReviewTasks = computed(() => filteredTasks.value.filter(t => t.status === 'SUBMITTED'))
const pendingReviewCount = computed(() => pendingReviewTasks.value.length)

// P0 #13: 当前用户名空 (auth.user 未就绪) 时拒绝署名操作, 避免提交空 reviewerName
function ensureCurrentUser(): boolean {
  if (!currentUserName.value) {
    ElMessage.error('用户信息未就绪, 请刷新页面后重试')
    return false
  }
  return true
}

async function handleApproveTask(task: InspTask) {
  if (!ensureCurrentUser()) return
  try {
    await ElMessageBox.confirm(`通过任务 ${task.taskCode} 的审核？`, '确认审核', { type: 'info' })
    // P1 #21: 区分 reviewTask 失败 vs publishTask 失败 (当前 store.reviewTask 内置 publish 调用)
    try {
      await store.reviewTask(task.id, { reviewerName: currentUserName.value, comment: '审核通过' })
    } catch (phaseErr: any) {
      console.error('审核两步原子操作失败', phaseErr)
      ElMessage.error('审核失败 (reviewTask/publishTask 阶段): ' + (phaseErr?.message || '未知错误'))
      throw 'phaseHandled'
    }
    ElMessage.success('审核通过')
    loadProject()
  } catch (e: any) {
    if (e === 'phaseHandled') return
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') { console.error('审核通过失败', e); ElMessage.error('审核操作失败，请重试') }
  }
}

async function handleRejectTask(task: InspTask) {
  if (!ensureCurrentUser()) return
  try {
    // P1 #18: 驳回原因必填, 空字符串 / 纯空格直接禁掉
    const { value: comment } = await ElMessageBox.prompt('请输入驳回原因', '驳回任务', {
      type: 'warning',
      inputPlaceholder: '驳回原因 (必填)...',
      inputValidator: (v: string) => !!v?.trim() || '请填写驳回原因',
    }) as any
    await store.reviewTask(task.id, { reviewerName: currentUserName.value, comment: comment.trim() })
    ElMessage.success('已驳回')
    loadProject()
  } catch (e: any) {
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') { console.error('驳回失败', e); ElMessage.error('驳回操作失败，请重试') }
  }
}

// ========== 总览 Tab 数据 ==========
// IA 收敛 (2026-05-23): 详细排名/分区得分分布下沉到「成绩统计」Tab (IndicatorScoreView),
// 总览只保留"最近活动 + 待办跳转". sectionScores/getSectionScoreColor 已移除.

// 最近5条任务
const recentTasks = computed(() => {
  return [...filteredTasks.value]
    .filter(t => ['SUBMITTED', 'UNDER_REVIEW', 'REVIEWED', 'PUBLISHED'].includes(t.status))
    .sort((a, b) => (b.taskDate || '').localeCompare(a.taskDate || ''))
    .slice(0, 5)
})


// P1 #8: 任务状态文案映射 — 上提为模块常量, 模板里 O(1) 直查
const TASK_STATUS_LABEL: Record<string, string> = {
  SUBMITTED: '已提交',
  UNDER_REVIEW: '审核中',
  REVIEWED: '已审核',
  PUBLISHED: '已发布',
}

const inspectorStats = computed(() => {
  const todayStr = today.value
  // P1 #17: key 改为 userId(string) 防同名 — 同名 userName 不会再互相聚合
  // 未分配任务 (inspectorId null) 用 sentinel 'unassigned' 兜底
  const map = new Map<string, {
    userId: string; name: string; assigned: number; completed: number; targets: number;
    active: number; overdue: number;
  }>()
  for (const task of filteredTasks.value) {
    const idKey = task.inspectorId != null ? String(task.inspectorId) : 'unassigned'
    const name = task.inspectorName || '未分配'
    if (!map.has(idKey)) map.set(idKey, { userId: idKey, name, assigned: 0, completed: 0, targets: 0, active: 0, overdue: 0 })
    const s = map.get(idKey)!; s.assigned++
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

// P1 #17: 按 userId 索引, 模板 inspectorStatsById.get(String(insp.userId)) — 同名安全
const inspectorStatsById = computed(() => {
  const m = new Map<string, (typeof inspectorStats.value)[number]>()
  for (const s of inspectorStats.value) m.set(s.userId, s)
  return m
})

// 检查员搜索 (人员卡片过滤)
const inspectorFilter = ref('')
const filteredInspectors = computed(() => {
  const q = inspectorFilter.value.trim().toLowerCase()
  if (!q) return inspectors.value
  return inspectors.value.filter(i => (i.userName || '').toLowerCase().includes(q))
})

// ========== Load ==========
async function loadProject() {
  loading.value = true
  loadError.value = null
  try {
    project.value = await store.loadProject(projectId)
    inspectors.value = await store.loadInspectors(projectId)
    try { scoringProfiles.value = await getProfiles(projectId) } catch (e) { console.warn('加载评分方案列表失败', e) }
    // P1 #4: 不再自动切到 settings — 让用户看到总览的"未发布"空态, 由空态 CTA 引导
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
        const map = new Map<LongId, { name: string; targetType?: string }>()
        for (const sec of sections) {
          map.set(sec.id, { name: sec.sectionName, targetType: sec.targetType ?? undefined })
        }
        sectionNameMap.value = map
        sectionTree.value = buildSectionTree(sections, project.value.rootSectionId)
      } catch (e) { console.warn('加载分区树失败', e) }
    }
    allTasks.value = []; allSubmissions.value = []
    submissionsLoadFailedCount.value = 0
    try {
      const tasks = await getTasks({ projectId })
      allTasks.value.push(...tasks)
      // P1 #5: 并发加载各任务的提交记录, 替代 N+1 串行 await
      const subResults = await Promise.all(tasks.map(async t => {
        try { return await getSubmissions({ taskId: t.id }) }
        catch (e: any) { console.warn(`加载任务 ${t.id} 的提交记录失败`, e); return null }
      }))
      for (const r of subResults) {
        if (r) allSubmissions.value.push(...r)
        else submissionsLoadFailedCount.value++
      }
      if (submissionsLoadFailedCount.value > 0) {
        ElMessage.warning(`${submissionsLoadFailedCount.value} 个任务的提交记录加载失败, 成绩统计可能不完整`)
      }
    } catch (e: any) { console.error('加载任务列表失败', e); ElMessage.error('加载任务列表失败: ' + (e?.message || '未知错误')) }
    configDirty.value = false
  } catch (e: any) {
    // P0 #2: 失败留下错误条 + 重试按钮, 不只一闪而过的 toast
    loadError.value = e?.message || '加载失败'
    ElMessage.error(loadError.value || '加载失败')
  }
  finally { loading.value = false }
}
function syncForm() {
  if (!project.value) return; const p = project.value
  let rawIds: (number | string)[] = []; try { rawIds = p.scopeConfig ? JSON.parse(p.scopeConfig) : [] } catch (e: any) { console.warn('解析 scopeConfig 失败', e) }
  const ids: string[] = rawIds.map(String)
  cf.value = { ...cf.value, scopeType: (p.scopeType as string) || 'ORG', scopeIds: ids, startDate: p.startDate || '', endDate: p.endDate || '', assignmentMode: (p.assignmentMode as string) || 'FREE', reviewRequired: p.reviewRequired ?? true, autoPublish: p.autoPublish ?? false, projectName: p.projectName,
    // V108
    inspectionMode: ((p as any).inspectionMode || 'PLANNED') as any,
    allowAdHoc: !!((p as any).allowAdHoc),
    allowSelfCheck: !!((p as any).allowSelfCheck),
    adHocQuotaPerInspector: ((p as any).adHocQuotaPerInspector ?? null) as number | null,
  }
  // V108: project DTO 可能没暴露 inspection_mode 字段, 单独 GET 一次
  loadInspectionModeFallback()
}

async function loadInspectionModeFallback() {
  try {
    const r = await http.get<any>('/inspection/tasks/projects/' + projectId + '/inspection-mode')
    if (r) {
      cf.value.inspectionMode = (r.inspection_mode || r.inspectionMode || 'PLANNED') as any
      cf.value.allowAdHoc = !!(r.allow_ad_hoc ?? r.allowAdHoc)
      cf.value.allowSelfCheck = !!(r.allow_self_check ?? r.allowSelfCheck)
      cf.value.adHocQuotaPerInspector = r.ad_hoc_quota_per_inspector ?? r.adHocQuotaPerInspector ?? null
    }
  } catch { /* skip */ }
  // V110: 加载整改判定策略
  try {
    const p = await http.get<any>('/inspection/corrective/projects/' + projectId + '/policy')
    if (p) {
      cf.value.correctiveStrictness = (p.strictness || 'NORMAL') as any
      cf.value.correctiveThresholdHigh = p.thresholdHigh ?? null
      cf.value.correctiveThresholdMedium = p.thresholdMedium ?? null
      cf.value.correctiveThresholdLow = p.thresholdLow ?? null
      cf.value.correctiveDeadlineHigh = p.deadlineHigh ?? null
      cf.value.correctiveDeadlineMedium = p.deadlineMedium ?? null
      cf.value.correctiveDeadlineLow = p.deadlineLow ?? null
      // 自定义阈值: 当返回的阈值偏离 strictness 默认值时, 自动开启
      const defaults = STRICTNESS_THRESHOLD_DEFAULTS[cf.value.correctiveStrictness] || STRICTNESS_THRESHOLD_DEFAULTS.NORMAL
      cf.value.useCustomThresholds =
        (p.thresholdHigh != null && Math.abs(p.thresholdHigh - defaults.high) > 0.001) ||
        (p.thresholdMedium != null && Math.abs(p.thresholdMedium - defaults.medium) > 0.001) ||
        (p.thresholdLow != null && Math.abs(p.thresholdLow - defaults.low) > 0.001)
    }
  } catch { /* skip */ }
}

// Watch cf changes after initial sync
let watchEnabled = false
function startWatch() { watchEnabled = true }
watch(cf, () => { if (watchEnabled) configDirty.value = true }, { deep: true })
watch(scopeFilterText, (val) => { scopeTreeRef.value?.filter(val) })

async function loadScopeNames() {
  scopeOrgNames.value = []
  if (!project.value?.scopeConfig) return
  try {
    const rawIds: (number | string)[] = JSON.parse(project.value.scopeConfig)
    const ids = rawIds.map(String)
    // V20260524 Bug#12: 按 scopeType 加载并构 id→name 映射
    await loadScopeSource()
    const m = new Map<string, string>()
    const st = project.value.scopeType
    function walkPlace(list: PlaceTreeNode[]) {
      for (const n of list) {
        m.set(String(n.id), n.placeName)
        if (n.children?.length) walkPlace(n.children)
      }
    }
    if (st === 'PLACE') {
      walkPlace(placeTree.value)
    } else if (st === 'USER') {
      for (const u of userList.value) m.set(String(u.id), u.realName || u.username || `#${u.id}`)
    } else {
      const om = buildMap(orgTree.value)
      om.forEach((v, k) => m.set(k, v))
    }
    scopeOrgNames.value = ids.map(id => m.get(id) || `#${id}`)
  } catch (e: any) { console.warn('加载检查范围名称失败', e) }
}
function buildMap(nodes: OrgUnitTreeNode[]): Map<string, string> { const m = new Map<string, string>(); function w(l: OrgUnitTreeNode[]) { for (const n of l) { m.set(String(n.id), n.unitName); if (n.children) w(n.children) } }; w(nodes); return m }
async function loadOrgTree() { if (orgTree.value.length > 0) return; loadingOrgTree.value = true; try { orgTree.value = await getOrgUnitTree() } catch (e: any) { console.error('加载组织树失败', e); ElMessage.error('加载组织结构失败') }; loadingOrgTree.value = false }

// V20260524 Bug#12: 按 scopeType 加载相应数据源
async function loadPlaceTree() {
  if (placeTree.value.length > 0) return
  loadingOrgTree.value = true
  try { placeTree.value = await universalPlaceApi.getTree() }
  catch (e: any) { ElMessage.error('加载场所失败') }
  finally { loadingOrgTree.value = false }
}
async function loadUserList() {
  if (userList.value.length > 0) return
  loadingOrgTree.value = true
  try { userList.value = await getSimpleUserList() }
  catch (e: any) { ElMessage.error('加载人员失败') }
  finally { loadingOrgTree.value = false }
}
async function loadScopeSource() {
  const st = project.value?.scopeType || cf.value.scopeType
  if (st === 'PLACE') await loadPlaceTree()
  else if (st === 'USER') await loadUserList()
  else await loadOrgTree()
}

// 当前 scope 数据源 (按 scopeType 切换 tree/list)
const scopeTreeData = computed(() => {
  const st = project.value?.scopeType || cf.value.scopeType
  if (st === 'PLACE') return placeTree.value
  if (st === 'USER') return userList.value.map(u => ({
    id: u.id, placeName: u.realName || u.username, unitName: u.realName || u.username,
    children: [],
  }))
  return orgTree.value
})
const scopeTreeProps = computed(() => {
  const st = project.value?.scopeType || cf.value.scopeType
  if (st === 'PLACE') return { children: 'children', label: 'placeName' }
  if (st === 'USER') return { children: 'children', label: 'unitName' }
  return { children: 'children', label: 'unitName' }
})
const scopeKindLabel = computed(() => {
  const st = project.value?.scopeType || cf.value.scopeType
  return st === 'PLACE' ? '场所' : (st === 'USER' ? '人员' : '组织单元')
})

// ========== Save ==========
async function saveConfig() {
  if (!project.value) return; saving.value = true
  try {
    if (isDraft.value) {
      await inspProjectApi.update(projectId, { projectName: cf.value.projectName, rootSectionId: project.value.rootSectionId, scopeType: cf.value.scopeType as ScopeType, scopeConfig: cf.value.scopeIds.length > 0 ? JSON.stringify(cf.value.scopeIds) : undefined, startDate: cf.value.startDate || undefined, endDate: cf.value.endDate || undefined, assignmentMode: cf.value.assignmentMode as AssignmentMode, reviewRequired: cf.value.reviewRequired, autoPublish: cf.value.autoPublish })
    } else {
      await updateOperationalConfig(projectId, { projectName: cf.value.projectName, assignmentMode: cf.value.assignmentMode, reviewRequired: cf.value.reviewRequired, autoPublish: cf.value.autoPublish })
    }
    // 检查模式 / 整改策略走独立端点 — 失败要让用户看见, 不能静默吞还报"已保存"
    const failed: string[] = []
    // V108: 单独 PUT 检查模式 (不依赖现有 update DTO)
    try {
      await http.put('/inspection/tasks/projects/' + projectId + '/inspection-mode', {
        inspectionMode: cf.value.inspectionMode,
        allowAdHoc: cf.value.allowAdHoc,
        allowSelfCheck: cf.value.allowSelfCheck,
        adHocQuotaPerInspector: cf.value.adHocQuotaPerInspector,
      })
    } catch (e: any) {
      console.warn('保存检查模式失败:', e?.message)
      failed.push('检查模式')
    }
    // V110: 保存整改判定策略
    try {
      await http.put('/inspection/corrective/projects/' + projectId + '/policy', {
        strictness: cf.value.correctiveStrictness,
        // 仅自定义启用时上送阈值, 否则后端用 strictness 预设默认
        thresholdHigh: cf.value.useCustomThresholds ? cf.value.correctiveThresholdHigh : null,
        thresholdMedium: cf.value.useCustomThresholds ? cf.value.correctiveThresholdMedium : null,
        thresholdLow: cf.value.useCustomThresholds ? cf.value.correctiveThresholdLow : null,
        deadlineHigh: cf.value.correctiveDeadlineHigh,
        deadlineMedium: cf.value.correctiveDeadlineMedium,
        deadlineLow: cf.value.correctiveDeadlineLow,
      })
    } catch (e: any) {
      console.warn('保存整改策略失败:', e?.message)
      failed.push('整改判定策略')
    }
    if (failed.length > 0) {
      // 部分失败 — 保留 configDirty 供重试, 不谎报"已保存"
      // P0 #22: 失败时不 reload — 否则会用服务端旧值覆盖用户未保存的输入
      ElMessage.error(`基本配置已保存,但 ${failed.join('、')} 保存失败,请重试`)
    } else {
      ElMessage.success('已保存'); configDirty.value = false
      loadProject()
    }
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
async function handleClaim(task: InspTask) {
  if (!ensureCurrentUser()) return
  try { await store.claimTask(task.id, { inspectorName: currentUserName.value }); ElMessage.success('已领取'); loadProject() } catch (e: any) { ElMessage.error(e.message || '领取失败') }
}

// ========== Inspector ==========
async function searchUsers(q: string) { if (!q.trim()) { addResults.value = []; return }; addLoading.value = true; try { addResults.value = await getSimpleUserList(q.trim()) } catch (e: any) { console.warn('搜索用户失败', e); addResults.value = [] }; addLoading.value = false }
async function handleAddInspector(userId: LongId) {
  const u = addResults.value.find(x => x.id === userId); if (!u) return
  // P0 #15: 前置去重 — 同一个人已在检查员列表里时直接提示, 不发请求
  if (inspectors.value.some(i => String(i.userId) === String(u.id))) {
    ElMessage.warning(`${u.realName || u.username} 已在检查员列表中`)
    addQuery.value = ''; addResults.value = []
    return
  }
  try {
    await store.addInspector(projectId, { userId: u.id, userName: u.realName || u.username, role: addRole.value })
    ElMessage.success(`已添加 ${u.realName || u.username}`)
    inspectors.value = await store.loadInspectors(projectId)
  } catch (e: any) {
    ElMessage.error(e.message || '失败')
  } finally {
    // P0 #15: 不管成败都清空, 否则失败后下次同样搜索词会带着旧 selected 卡住
    addQuery.value = ''; addResults.value = []
  }
}
async function handleRemoveInspector(insp: ProjectInspector) {
  // P1 #16: 移除前查影响范围, 名下有 active/overdue 任务时显式提示
  const stat = inspectorStatsById.value.get(String(insp.userId))
  let extra = ''
  if (stat && (stat.active > 0 || stat.overdue > 0)) {
    const parts: string[] = []
    if (stat.active > 0) parts.push(`${stat.active} 个进行中`)
    if (stat.overdue > 0) parts.push(`${stat.overdue} 个逾期`)
    extra = ` 该检查员名下还有 ${parts.join('、')}的任务, 移除后这些任务需要重新分配.`
  }
  try {
    await ElMessageBox.confirm(`移除「${insp.userName}」？${extra}`, '确认', { type: 'warning' })
    await store.removeInspector(projectId, insp.id)
    inspectors.value = await store.loadInspectors(projectId)
  } catch (e: any) {
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') { console.error('移除检查员失败', e); ElMessage.error('移除检查员失败，请重试') }
  }
}

function goBack() { router.push('/inspection/projects') }
function goExecuteTask(taskId: LongId) { router.push(`/inspection/tasks/${taskId}/execute`) }

// ============ 评分配置 · 按章节 (L1 2026-05-26) ============
// 进入指定评分方案编辑器 (评分配置按 section 切片, 每章一套)
function goEditProfile(profileId: LongId) {
  router.push(`/inspection/scoring/${profileId}`)
}
// 2026-05-24 修: TeamTab 内人员/角色变更后, 同步刷新 inspectors ref —
// 否则 SectionConfigView 调度组对话框依赖的 :inspectors prop 是 stale 值, 看不到新加的人.
async function onTeamChange() {
  try {
    inspectors.value = await store.loadInspectors(projectId)
  } catch (e) {
    console.warn('TeamTab change → 刷新 inspectors 失败', e)
  }
}

// "查看全部"已删除 (ScoringProfileListView 已废) — 评分方案在项目内直接 inline 列表

// P1 #23: 已发布项目的"克隆"按钮 — 跳到向导 clone 模式, 给"或克隆"文案真入口
function handleCloneProject() {
  router.push({
    path: '/inspection/projects/create',
    query: { clone: String(projectId) },
  })
}

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
  await loadScopeSource()
  await loadProject()
  startWatch()
})
</script>

<template>
  <div class="pdv insp-shell">
    <InspSpinner v-if="loading" overlay />
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
            <InspChip v-if="project" :tone="({
              DRAFT: 'pending', PUBLISHED: 'info', PAUSED: 'warn',
              COMPLETED: 'pass', ARCHIVED: 'pending',
            } as const)[project.status as ProjectStatus]">
              {{ ProjectStatusConfig[project.status as ProjectStatus]?.label }}
            </InspChip>
          </div>
          <div class="pdv-subtitle" v-if="project">
            <span v-if="project.startDate" class="insp-num">{{ project.startDate }}<template v-if="project.endDate"> ~ {{ project.endDate }}</template></span>
            <span v-if="rootSectionName" class="pdv-subtitle-sep">·</span>
            <span v-if="rootSectionName">{{ rootSectionName }}</span>
            <span v-if="inspectors.length" class="pdv-subtitle-sep">·</span>
            <span v-if="inspectors.length" :title="`项目检查员 ${inspectors.length} 人`">
              <span class="insp-num">{{ inspectors.length }}</span> 检查员
            </span>
            <template v-if="scopeOrgNames.length > 0">
              <span class="pdv-subtitle-sep">·</span>
              <span :title="scopeOrgNames.join(', ')">
                <span class="insp-num">{{ scopeOrgNames.length }}</span>
                {{ project?.scopeType === 'PLACE' ? '受检场所' : (project?.scopeType === 'USER' ? '受检人员' : '受检组织') }}
              </span>
            </template>
          </div>
        </div>
      </div>
      <div class="pdv-header-actions" v-if="project">
        <!-- P1 #25: 删除 activeTab==='settings' 约束 — configDirty 时全 tab 可见 -->
        <InspButton
          v-if="configDirty && !isArchived"
          variant="accent" size="sm" :loading="saving" @click="saveConfig">
          <Save :size="13" />保存配置
        </InspButton>
        <InspButton v-if="isDraft" variant="accent" size="sm" :disabled="!canPublish" @click="handlePublish">
          <Send :size="13" />发布项目
        </InspButton>
        <InspButton v-if="project.status === 'PUBLISHED'" size="sm" @click="handlePause"
                    title="暂停: 项目可恢复, 任务被冻结">
          <Pause :size="13" />暂停
        </InspButton>
        <InspButton v-if="project.status === 'PAUSED'" variant="accent" size="sm" @click="handleResume">
          <Play :size="13" />恢复
        </InspButton>
        <InspButton v-if="['PUBLISHED','PAUSED'].includes(project.status) && project.rootSectionId"
                    size="sm" @click="handleUpgradeTemplate"
                    :variant="templateVersionStatus?.drifted ? 'danger' : 'default'"
                    title="把项目锁定的模板快照升级至该模板的最新已发布版本">
          <span v-if="templateVersionStatus?.drifted">
            升级 v{{ templateVersionStatus.currentVersionNumber ?? '?' }} > v{{ templateVersionStatus.latestVersionNumber }}
          </span>
          <span v-else-if="templateVersionStatus">
            模板 v{{ templateVersionStatus.currentVersionNumber ?? '?' }}
          </span>
          <span v-else>升级模板版本</span>
        </InspButton>
        <InspButton v-if="['PUBLISHED','PAUSED'].includes(project.status)" variant="danger" size="sm"
                    @click="handleComplete"
                    title="完结: 不可逆, 任务被强制归档">
          <CheckCircle :size="13" />完结
        </InspButton>
        <InspButton v-if="project.status === 'COMPLETED'" variant="ghost" size="sm" @click="handleArchive">
          归档
        </InspButton>
      </div>
    </div>

    <!-- ====== Pill Tabs (顺序: 总览 > 检查计划 > 人员与任务 > 成绩统计 > 设置) ====== -->
    <!-- 命名约定: "检查计划"=分区评价+调度组 (如何执行) / "设置"=项目元数据+策略+评分方案 -->
    <div class="pdv-tabs">
      <button :class="['pdv-tab', activeTab === 'overview' && 'active']" @click="activeTab = 'overview'">
        <LayoutDashboard class="w-3.5 h-3.5" />总览
      </button>
      <button :class="['pdv-tab', activeTab === 'config' && 'active']" @click="activeTab = 'config'">
        <ListTree class="w-3.5 h-3.5" />检查计划
      </button>
      <button :class="['pdv-tab', activeTab === 'team' && 'active']" @click="activeTab = 'team'">
        <Users class="w-3.5 h-3.5" />人员与任务
        <span v-if="pendingReviewCount > 0 || pendingAssignTasks.length > 0" class="pdv-tab-badge">{{ pendingReviewCount + pendingAssignTasks.length }}</span>
      </button>
      <button :class="['pdv-tab', activeTab === 'scores' && 'active']" @click="activeTab = 'scores'">
        <BarChart3 class="w-3.5 h-3.5" />成绩统计
      </button>
      <button :class="['pdv-tab', activeTab === 'scoring' && 'active']" @click="activeTab = 'scoring'">
        <ListTree class="w-3.5 h-3.5" />评分方案
      </button>
      <button :class="['pdv-tab', activeTab === 'evaluation' && 'active']" @click="activeTab = 'evaluation'">
        <ListTree class="w-3.5 h-3.5" />评级
      </button>
      <button :class="['pdv-tab', activeTab === 'corrective' && 'active']" @click="activeTab = 'corrective'">
        <ListTree class="w-3.5 h-3.5" />整改
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

        <!-- P0 #2: 加载失败错误条 + 重试 (沿用 pdv-score-warn 配色, 整页可见) -->
        <div v-if="loadError" class="pdv-score-warn">
          <span class="pdv-score-warn__icon">!</span>
          <span>加载项目数据失败: {{ loadError }}</span>
          <button class="pdv-score-warn__retry" @click="loadProject">重试</button>
        </div>

        <!-- 未发布提示 -->
        <div v-if="isDraft" class="pdv-empty-state">
          <ClipboardList class="w-10 h-10 text-blue-300 mb-3" />
          <div class="font-medium text-gray-600 mb-1">项目尚未发布</div>
          <div class="text-sm text-gray-400 mb-4">请在「设置」中完成配置后发布</div>
          <div class="flex items-center gap-2">
            <el-button @click="activeTab = 'settings'" size="small" round>
              <Settings class="w-3.5 h-3.5 mr-1" />去设置
            </el-button>
            <el-button type="primary" :disabled="!canPublish" @click="handlePublish" size="small" round>
              <Send class="w-3.5 h-3.5 mr-1" />发布项目
            </el-button>
          </div>
        </div>

        <template v-else>
          <!-- A 级升级: 顶部告警条 (有需关注事项才显示)
               P0 #3: icon 改为 lucide component, 不再 raw 空字符串 -->
          <div v-if="overviewAlerts.length > 0" class="pdv-alert-strip">
            <div v-for="(a, i) in overviewAlerts" :key="i"
                 class="pdv-alert" :class="`pdv-alert--${a.type}`">
              <component :is="a.icon" class="pdv-alert__icon w-3.5 h-3.5" />
              <span class="pdv-alert__text">{{ a.text }}</span>
              <button v-if="a.action" class="pdv-alert__action"
                      @click="activeTab = a.action.tab">{{ a.action.label }} ></button>
            </div>
          </div>

          <!-- 8 KPI 网格 (替代之前 2 个 KPI) -->
          <div class="pdv-kpi-grid">
            <div class="pdv-kpi">
              <div class="pdv-kpi__num">{{ richKpis.total }}</div>
              <div class="pdv-kpi__label">总任务</div>
            </div>
            <div class="pdv-kpi">
              <div class="pdv-kpi__num" style="color: #10b981">{{ richKpis.done }}</div>
              <div class="pdv-kpi__label">已完成</div>
            </div>
            <div class="pdv-kpi">
              <div class="pdv-kpi__num" style="color: #3b82f6">{{ richKpis.active }}</div>
              <div class="pdv-kpi__label">进行中</div>
            </div>
            <div class="pdv-kpi" :class="{ 'pdv-kpi--alert': richKpis.overdue > 0 }">
              <div class="pdv-kpi__num" :style="{ color: richKpis.overdue > 0 ? '#ef4444' : '#9ca3af' }">
                {{ richKpis.overdue }}
              </div>
              <div class="pdv-kpi__label">逾期</div>
            </div>
            <div class="pdv-kpi" :class="{ 'pdv-kpi--alert': richKpis.pendingReview > 0 }">
              <div class="pdv-kpi__num" :style="{ color: richKpis.pendingReview > 0 ? '#f59e0b' : '#9ca3af' }">
                {{ richKpis.pendingReview }}
              </div>
              <div class="pdv-kpi__label">待审</div>
            </div>
            <div class="pdv-kpi">
              <!-- P1 #6: 无成绩数据时 0.0 误导, 改为 — 占位 -->
              <div class="pdv-kpi__num">{{ richKpis.hasScores ? richKpis.avgScore.toFixed(1) : '—' }}</div>
              <div class="pdv-kpi__label">平均得分</div>
            </div>
            <div class="pdv-kpi">
              <div v-if="!richKpis.hasScores" class="pdv-kpi__num">—</div>
              <div v-else class="pdv-kpi__num" :style="{ color: richKpis.passRate >= 80 ? '#10b981' : richKpis.passRate >= 60 ? '#f59e0b' : '#ef4444' }">
                {{ richKpis.passRate }}<span class="pdv-kpi__unit">%</span>
              </div>
              <div class="pdv-kpi__label">通过率</div>
            </div>
            <!-- P1 #11: 删 completionRate (与下方进度条同口径重复), 换"目标完成率" — 目标维度更有信息量 -->
            <div class="pdv-kpi">
              <div class="pdv-kpi__num">
                {{ taskStats.totalTargets === 0 ? '—' : Math.round(taskStats.completedTargets / taskStats.totalTargets * 100) }}<span v-if="taskStats.totalTargets > 0" class="pdv-kpi__unit">%</span>
              </div>
              <div class="pdv-kpi__label">目标完成率</div>
            </div>
          </div>

          <!-- 7 天趋势条形图 -->
          <div v-if="weeklyTrend.some(d => d.total > 0)" class="pdv-trend">
            <div class="pdv-trend__head">
              <span class="pdv-trend__title">最近 7 天任务量</span>
              <span class="pdv-trend__legend">
                <span class="pdv-trend__legend-dot" style="background: #1a6dff" /> 总
                <span class="pdv-trend__legend-dot" style="background: #10b981; margin-left: 8px" /> 完成
              </span>
            </div>
            <div class="pdv-trend__chart">
              <div v-for="d in weeklyTrend" :key="d.date" class="pdv-trend__col">
                <div class="pdv-trend__bars">
                  <div class="pdv-trend__bar pdv-trend__bar--total"
                       :style="{ height: (d.total / weeklyTrendMax * 100) + '%' }"
                       :title="`${d.label}: 总 ${d.total}`" />
                  <div v-if="d.done > 0" class="pdv-trend__bar pdv-trend__bar--done"
                       :style="{ height: (d.done / weeklyTrendMax * 100) + '%' }"
                       :title="`${d.label}: 完成 ${d.done}`" />
                </div>
                <div class="pdv-trend__label">{{ d.label }}</div>
              </div>
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

          <!-- IA 收敛: 总览只保留"最近活动 + 待办跳转",
               详细排名/维度对比/目标得分分布全部下沉到「成绩统计」Tab (IndicatorScoreView)
               P1 #7: 待分配卡空时整卡不再 v-if 隐藏 — 改为空态占位, 保持双栏稳定 -->
          <div class="pdv-two-col">

            <!-- 最近检查 -->
            <div class="pdv-card">
              <div class="pdv-card-title">
                <ClipboardList class="w-4 h-4 text-[#1a6dff]" />最近检查
                <button v-if="recentTasks.length > 0" class="pdv-card-link" @click="activeTab = 'scores'"
                        title="到「成绩统计」查看详细排名/维度对比">
                  查看详情 →
                </button>
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
                      {{ TASK_STATUS_LABEL[task.status] || task.status }}
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

            <!-- 待分配任务 — P1 #10: 总览只 readonly preview, 指派操作在 team tab 完成
                 P1 #7: 空态也保留卡片, 避免双栏塌成单栏 -->
            <div class="pdv-card">
              <div class="pdv-card-title">
                <Users class="w-4 h-4 text-orange-500" />待分配任务
                <span v-if="pendingAssignTasks.length > 0" class="pdv-badge-orange ml-1.5">{{ pendingAssignTasks.length }}</span>
                <!-- P1 #9: > 8 条时文案带总数, ≤ 8 条时不显总数 -->
                <button v-if="pendingAssignTasks.length > 0" class="pdv-card-link" @click="activeTab = 'team'"
                        title="到「人员与任务」分配">
                  {{ pendingAssignTasks.length > 8 ? `查看全部 ${pendingAssignTasks.length} →` : '去分配 →' }}
                </button>
              </div>
              <div v-if="pendingAssignTasks.length === 0" class="pdv-card-empty">所有任务已分配</div>
              <div v-else class="pdv-assign-list">
                <div v-for="task in pendingAssignTasks.slice(0, 8)" :key="task.id" class="pdv-assign-row">
                  <span class="text-xs text-gray-500">{{ task.taskDate }}</span>
                  <span class="text-xs text-gray-400 pdv-assign-row-code" :title="task.taskCode">{{ task.taskCode }}</span>
                  <span class="text-xs text-gray-400">{{ task.totalTargets }}个目标</span>
                </div>
              </div>
            </div>

          </div>

        </template>
      </div>

      <!-- ===== 成绩统计 Tab ===== -->
      <div v-if="activeTab === 'scores'">
        <!-- P1 #12: 部分提交记录加载失败 — 明示统计可能缺数据 -->
        <div v-if="submissionsLoadFailedCount > 0" class="pdv-score-warn">
          <span class="pdv-score-warn__icon">!</span>
          <span>{{ submissionsLoadFailedCount }} 个任务的提交记录加载失败, 下方成绩统计可能不完整</span>
          <button class="pdv-score-warn__retry" @click="loadProject">重新加载</button>
        </div>
        <template v-if="!isDraft">
          <!-- Phase 3.5b: 组织树 roll-up 得分排名 — 规模公平得分可见 -->
          <OrgScoreRanking :project-id="projectId" />
          <IndicatorScoreView
            :project-id="projectId"
            :all-tasks="allTasks"
            :all-submissions="allSubmissions"
            :submissions-load-failed-count="submissionsLoadFailedCount"
          />
        </template>

        <!-- P1 #14: Legacy score aggregation 整段死代码 (v-if="false") 已删除 — IndicatorScoreView 承接全部 -->
        <div v-else class="py-20 text-center">
          <BarChart3 class="w-10 h-10 text-gray-300 mx-auto mb-3" />
          <div class="text-sm text-gray-400">项目发布后可查看成绩统计</div>
        </div>
      </div>

      <!-- ===== 检查配置 Tab ===== -->
      <div v-if="activeTab === 'config'">
        <SectionConfigView :project-id="projectId" :sections="sectionList" :section-tree="sectionTree" :root-section-id="project?.rootSectionId" :root-section-name="rootSectionName" :inspectors="inspectors" :project-tasks="allTasks" />
      </div>

      <!-- ===== 评级 Tab (Phase 5 评级引擎完美架构) =====
           内部两段: 评级配置 (Indicator CRUD) + 评级结果 (IndicatorResult 三态机 + 修订链 + 手动评估) -->
      <div v-if="activeTab === 'evaluation'">
        <div v-if="isDraft" class="py-20 text-center">
          <ClipboardList class="w-10 h-10 text-gray-300 mx-auto mb-3" />
          <div class="text-sm text-gray-400">项目发布后可配置评级指标</div>
        </div>
        <template v-else>
          <div class="pdv-eval-sub-tabs">
            <button :class="['pdv-eval-sub-tab', evalSubTab === 'config' && 'active']" @click="evalSubTab = 'config'">
              评级配置
            </button>
            <button :class="['pdv-eval-sub-tab', evalSubTab === 'results' && 'active']" @click="evalSubTab = 'results'">
              评级结果
            </button>
          </div>
          <EvaluationConfigView
            v-if="evalSubTab === 'config'"
            :project-id="projectId"
            :sections="sectionList"
            @view-results="handleViewResults"
          />
          <EvaluationResultsView
            v-else-if="evalSubTab === 'results'"
            :project-id="projectId"
            :initial-indicator-id="focusedIndicatorId"
          />
        </template>
      </div>

      <!-- ===== 人员与任务 Tab (2026-05-23 重构) =====
           旧 3 卡片 (待审核 + 待分配 + 检查员管理) 替换为 TeamTab 工作台,
           3 视图切换 (按人/按任务/角色矩阵) + 顶部状态条; 详见 docs/plans/2026-05-23-inspection-team-tab-redesign.md -->
      <div v-if="activeTab === 'team'" class="cfg-section">
        <!-- @change: TeamTab 内添加/删除/改角色时, 同步刷新 inspectors ref,
             否则 SectionConfigView 调度组对话框依赖的 :inspectors prop 是 stale 旧值 -->
        <TeamTab :project-id="projectId" :is-draft="isDraft" @change="onTeamChange" />
      </div>

      <!-- ===== 评分配置 · 按章节 Tab (L1 2026-05-26 概念重塑) ===== -->
      <div v-if="activeTab === 'scoring'">
        <div class="pdv-tab-head">
          <div class="pdv-tab-title-row">
            <h2 class="pdv-tab-title">评分配置 · 按章节</h2>
            <span v-if="scoringProfiles.length" class="pdv-tab-count">{{ scoringProfiles.length }} 章已配置</span>
          </div>
          <div class="pdv-tab-ops">
            <el-button v-if="!isDraft && !isArchived" size="small" plain @click="handleCloneProject" round>
              <Copy class="w-3.5 h-3.5 mr-1" />克隆为新项目
            </el-button>
          </div>
        </div>
        <div class="pdv-tab-desc">
          每个章节有独立的评分规则 (例: 卫生章用扣分制 / 安全章用一票否决).
          项目总分汇总走<el-link type="primary" :underline="false" @click="activeTab = 'evaluation'">「评级」</el-link> Tab 的 Indicator 配置.
          <span v-if="!isDraft">已发布项目只读, 如需修改请点"克隆为新项目"复制一份草稿.</span>
        </div>
        <div v-if="scoringProfiles.length === 0" class="cfg-empty cfg-empty--card">
          本项目模板还没有任何章节配置过评分 ·
          <span v-if="isDraft && !isArchived">点击模板内任一分区进入编辑器, 系统会自动创建默认配置</span>
          <span v-else>已发布项目无法新增</span>
        </div>
        <div v-else class="pdv-scoring-grid">
          <ScoringSectionCard
            v-for="p in scoringProfiles"
            :key="p.id"
            :profile="p"
            :section-name="sectionNameMap.get(p.sectionId)?.name ?? null"
            :is-draft="isDraft"
            :is-archived="isArchived"
            @edit="goEditProfile"
          />
        </div>
      </div>

      <!-- ===== 整改 Tab (架构 E: 题目级整改设置) ===== -->
      <div v-if="activeTab === 'corrective'">
        <ProjectCorrectiveStrategy :project-id="projectId" />
      </div>


      <div v-if="activeTab === 'settings'" class="cfg-section">

        <!-- 锁定提示 (P1 升级: 显式列出锁定/可改字段)
             2026-05-23 IA 收敛: 每张卡片自带 .cfg-locked 视觉, 顶部清单视觉权重降级为收纳式 details. -->
        <details v-if="!isDraft" class="pdv-lock-notice pdv-lock-notice--compact">
          <summary class="pdv-lock-summary">
            <Lock class="w-3.5 h-3.5 cfg-lock-icon" />
            <span class="cfg-lock-title">部分配置已锁定 ({{ project?.status === 'PUBLISHED' ? '已发布' : project?.status === 'PAUSED' ? '已暂停' : '运行中' }})</span>
            <span class="pdv-lock-hint">— 锁定的卡片以虚框标识 · 展开查看全部清单</span>
          </summary>
          <div class="pdv-lock-body">
            <div class="pdv-lock-grid">
              <div class="pdv-lock-col">
                <span class="pdv-lock-col__label"> 已锁定</span>
                <ul class="pdv-lock-list">
                  <li>检查范围 (受检组织 / 班级)</li>
                  <li>根分区 (绑定的模板)</li>
                  <!-- P1 #24: 旧文案"评分配置快照(满分/精度/多评模式)"已不准 — 评分配置下沉到调度组后, 锁定的只剩绑定关系 -->
                  <li>评分方案绑定关系 (检查计划分配 / 默认方案选择)</li>
                  <li>开始日期</li>
                </ul>
              </div>
              <div class="pdv-lock-col">
                <span class="pdv-lock-col__label">√ 可调整</span>
                <ul class="pdv-lock-list">
                  <li>项目名称 / 描述</li>
                  <li>结束日期 (可延期)</li>
                  <li>分配模式 / 审核要求 / 自动发布</li>
                  <li>检查员名单 (添加 / 移除)</li>
                  <li>检查计划</li>
                </ul>
              </div>
            </div>
          </div>
        </details>

        <!-- 基本信息 -->
        <div class="cfg-card" :class="{ 'cfg-locked': isArchived }">
          <div class="cfg-card-header">
            <div class="cfg-card-title">基本信息</div>
            <Lock v-if="isArchived" class="w-3.5 h-3.5 cfg-lock-icon" />
          </div>
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
          <!-- P1 #26: 多模板项目 (rootSectionId 为 null) 也显式显示, 不再 v-if 隐藏整行 -->
          <div class="cfg-field cfg-field--mt">
            <label class="cfg-label">检查模板</label>
            <div v-if="rootSectionName" class="cfg-readonly-text">{{ rootSectionName }}</div>
            <div v-else class="cfg-readonly-text">多模板项目 (按检查计划分别绑定)</div>
          </div>
        </div>

        <!-- P1 #27 卡片顺序: 基本信息 → 运营配置 → 检查模式 → 整改判定策略 → 评分方案 → 检查范围 → 时间范围 (从松锁到强锁渐变);
             评分方案 / 检查范围 / 时间范围 已下移到整改判定策略之后 -->

        <!-- 运营配置 -->
        <div class="cfg-card" :class="{ 'cfg-locked': isArchived }">
          <div class="cfg-card-header">
            <div class="cfg-card-title">运营配置</div>
            <Lock v-if="isArchived" class="w-3.5 h-3.5 cfg-lock-icon" />
          </div>
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

        <!-- V108: 检查模式配置 -->
        <div class="cfg-card" :class="{ 'cfg-locked': isArchived }">
          <div class="cfg-card-header">
            <div class="cfg-card-title">检查模式</div>
            <Lock v-if="isArchived" class="w-3.5 h-3.5 cfg-lock-icon" />
          </div>
          <div class="cfg-desc">控制本项目允许哪些检查行为 — 计划任务/临时抽查/自查 等.</div>
          <div class="cfg-row2">
            <div class="cfg-field">
              <label class="cfg-label">运行模式</label>
              <select v-model="cf.inspectionMode" class="cfg-select" :disabled="isArchived">
                <option value="PLANNED">计划制 — 仅按调度生成</option>
                <option value="HYBRID">混合制 — 计划 + 临时抽查</option>
                <option value="SPOT_CHECK">抽查制 — 不生成计划任务</option>
                <option value="SELF_AUDIT">自查制 — 受检主体自评</option>
                <option value="EMERGENCY">突击专项 — 一次性</option>
              </select>
              <div class="cfg-hint">
                {{ cf.inspectionMode === 'PLANNED' ? '只有计划生成的任务, 不允许临时抽查' :
                   cf.inspectionMode === 'HYBRID' ? '计划 + 抽查并存 (推荐)' :
                   cf.inspectionMode === 'SPOT_CHECK' ? '完全靠检查员自助发起' :
                   cf.inspectionMode === 'SELF_AUDIT' ? '受检主体自我评估' :
                   '一次性突击, 完成后归档' }}
              </div>
            </div>
            <div class="cfg-field">
              <label class="cfg-label">允许临时抽查</label>
              <div class="cfg-toggle-row">
                <el-switch v-model="cf.allowAdHoc" :disabled="isArchived || cf.inspectionMode === 'PLANNED'" />
              </div>
              <div class="cfg-hint">
                {{ cf.allowAdHoc ? '检查员可在任务列表点 " 发起抽查"' : '只能由调度自动生成任务' }}
              </div>
            </div>
          </div>
          <div class="cfg-row2">
            <div class="cfg-field">
              <label class="cfg-label">允许自查</label>
              <div class="cfg-toggle-row">
                <el-switch v-model="cf.allowSelfCheck" :disabled="isArchived" />
              </div>
              <div class="cfg-hint">
                {{ cf.allowSelfCheck ? '受检主体可主动发起自评' : '受检主体不可自查' }}
              </div>
            </div>
            <div class="cfg-field">
              <label class="cfg-label">月度抽查配额</label>
              <el-input-number v-model="cf.adHocQuotaPerInspector" :min="0" :disabled="isArchived || !cf.allowAdHoc" placeholder="留空=无限" class="w-full" />
              <div class="cfg-hint">每检查员每月最多抽查次数, 0/留空 = 无限制</div>
            </div>
          </div>
        </div>

        <!-- 架构 E (2026-05-25): 原"整改判定策略"卡已迁移到独立"整改" Tab.
             模板纯粹 (评分定义) + 项目级题目逐项开关阈值 (在"整改" Tab 中). -->


        <!-- 评分方案已迁移到独立 "评分方案" Tab (P2, 2026-05-26) -->


        <!-- 检查范围 -->
        <div class="cfg-card" :class="{ 'cfg-locked': !isDraft }">
          <div class="cfg-card-header">
            <div class="cfg-card-title">检查范围</div>
            <Lock v-if="!isDraft" class="w-3.5 h-3.5 cfg-lock-icon" />
          </div>
          <div class="cfg-desc">选择哪些{{ scopeKindLabel }}参与本次检查，系统将根据分区的目标类型自动派生具体检查对象。</div>
          <div class="cfg-field cfg-field--mt">
            <label class="cfg-label">检查对象 <span v-if="isDraft" class="cfg-req">*</span></label>
            <div v-if="loadingOrgTree" class="cfg-org-list cfg-org-loading">加载中...</div>
            <div v-else-if="scopeTreeData.length === 0" class="cfg-org-list cfg-org-loading">暂无{{ scopeKindLabel }}</div>
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
                  :placeholder="`搜索${scopeKindLabel}...`"
                  size="small"
                  clearable
                  style="width: 200px"
                />
                <el-button size="small" link type="primary" @click="scopeTreeRef?.setCheckedKeys([]); cf.scopeIds = []">清空</el-button>
              </div>
              <div class="cfg-org-list">
                <el-tree
                  ref="scopeTreeRef"
                  :data="scopeTreeData"
                  :props="scopeTreeProps"
                  show-checkbox
                  check-strictly
                  node-key="id"
                  :default-checked-keys="cf.scopeIds"
                  :filter-node-method="filterScopeNode"
                  default-expand-all
                  @check="handleScopeCheckChange"
                />
              </div>
            </template>
          </div>
          <div v-if="cf.scopeIds.length > 0 && isDraft" class="cfg-hint">
            已选 {{ cf.scopeIds.length }} 个{{ scopeKindLabel }}，发布后将作为本项目的检查目标
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

        <!-- P2 #29: 旧"项目操作 已上提"空注释删除 -->

      </div>
    </div>
  </div>
</template>

<style scoped src="./ProjectDetailView.css"></style>

<style scoped>
/* ===== 独立 Tab 通用头 (P2 评分方案 Tab) ===== */
.pdv-tab-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}
.pdv-tab-title-row {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}
.pdv-tab-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--insp-ink-primary, #111827);
  margin: 0;
}
.pdv-tab-count {
  font-size: 12px;
  color: var(--insp-ink-tertiary, #6b7280);
  font-weight: 500;
}
.pdv-tab-ops {
  display: inline-flex;
  gap: 8px;
  margin-left: auto;
}
.pdv-tab-desc {
  font-size: 12px;
  color: var(--insp-ink-tertiary, #6b7280);
  line-height: 1.6;
  margin-bottom: 16px;
}

/* ===== 评分方案卡片 (Phase 4: 评分方案下沉项目-owned) ===== */
.cfg-card-ops {
  display: inline-flex;
  gap: 8px;
  margin-left: auto;
}
/* 评分配置 · 按章节 卡片网格 (L1 2026-05-26) */
.pdv-scoring-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.pdv-profile-list {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--insp-border-subtle, #e5e7eb);
  border-radius: 6px;
  overflow: hidden;
}
.pdv-profile-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--insp-border-subtle, #f1f3f5);
  background: var(--insp-bg-surface, #fff);
  transition: background 0.15s;
}
.pdv-profile-row:last-child { border-bottom: 0; }
.pdv-profile-row:hover { background: var(--insp-bg-subtle, #fafbfc); }
.pdv-profile-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.pdv-profile-name {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--insp-ink-primary, #111827);
}
.pdv-profile-section { color: var(--insp-ink-primary, #111827); }
.pdv-profile-section--orphan {
  color: var(--insp-ink-quaternary, #9ca3af);
  font-style: italic;
  font-weight: 500;
}
.pdv-profile-id {
  font-family: var(--insp-font-mono, monospace);
  font-size: 11px;
  color: var(--insp-ink-tertiary, #6b7280);
  font-weight: 500;
}
.pdv-profile-stats {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--insp-ink-tertiary, #6b7280);
}
.pdv-profile-sep { color: var(--insp-ink-quaternary, #d1d5db); }
.pdv-profile-feat {
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 600;
  background: var(--insp-accent-paler, #eff6ff);
  color: var(--insp-accent, #1a6dff);
}

/* ===== 评级 Tab 子页切换 (Phase 5) ===== */
.pdv-eval-sub-tabs {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 4px;
  background: var(--insp-bg-subtle, #f4f6f9);
  border-radius: 8px;
  margin-bottom: 12px;
}
.pdv-eval-sub-tab {
  padding: 4px 14px; border-radius: 6px;
  background: transparent; border: none;
  color: var(--insp-ink-tertiary, #6b7280);
  font-size: 12px; cursor: pointer;
  transition: all 0.15s;
}
.pdv-eval-sub-tab:hover { color: var(--insp-ink-primary, #111827); }
.pdv-eval-sub-tab.active {
  background: var(--insp-bg-surface, #fff);
  color: var(--insp-accent, #1a6dff);
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
</style>
