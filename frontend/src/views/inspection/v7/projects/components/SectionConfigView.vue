<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Trash2, Pencil, Calendar, Users, Clock, X, Zap, Play,
  BarChart3, Layers, AlertTriangle, Settings2, TrendingUp, ExternalLink,
} from 'lucide-vue-next'
import { inspPlanApi, getProject } from '@/api/insp/project'
import {
  getIndicators, createLeafIndicator, createCompositeIndicator,
  updateIndicator, deleteIndicator,
} from '@/api/insp/indicator'
import { getGradeSchemes } from '@/api/insp/gradeScheme'
import { orgTypeApi } from '@/api/orgType'
import { userTypeApi } from '@/api/userType'
import { universalPlaceTypeApi } from '@/api/universalPlaceType'
import type { InspectionPlan, CreatePlanRequest } from '@/types/insp/template'
import type { Indicator } from '@/types/insp/indicator'
import type { GradeScheme } from '@/types/insp/gradeScheme'
import {
  SOURCE_AGG_OPTIONS, COMPOSITE_AGG_OPTIONS,
  MISSING_POLICY_OPTIONS, EVAL_PERIOD_OPTIONS, NORMALIZATION_OPTIONS,
  EVALUATION_METHOD_OPTIONS,
} from '@/types/insp/indicator'
import { flattenTree, type SectionTreeNode } from '@/utils/sectionTree'

// ══════════════════════════════════════════════
//  Props
// ══════════════════════════════════════════════

const props = defineProps<{
  projectId: number
  sections: Array<{ id: number; sectionName: string; targetType?: string }>
  inspectors: Array<{ userId: number | string; userName: string }>
  sectionTree?: SectionTreeNode[]
  rootSectionId?: number | string | null
  rootSectionName?: string
}>()

// ══════════════════════════════════════════════
//  State
// ══════════════════════════════════════════════

const loading = ref(false)
const plans = ref<InspectionPlan[]>([])
const indicators = ref<Indicator[]>([])
const gradeSchemes = ref<GradeScheme[]>([])
const targetCount = ref(0) // 检查目标数量

// ── Schedule Dialog ──
const scheduleDialogVisible = ref(false)
const editingPlan = ref<InspectionPlan | null>(null)
const scheduleSaving = ref(false)

type FreqMode = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'ON_DEMAND'

const WEEKDAYS = [
  { v: 1, l: '一' }, { v: 2, l: '二' }, { v: 3, l: '三' }, { v: 4, l: '四' },
  { v: 5, l: '五' }, { v: 6, l: '六' }, { v: 7, l: '日' },
]
const MONTH_DAYS = Array.from({ length: 31 }, (_, i) => i + 1)

const scheduleForm = ref({
  planName: '',
  sectionIds: [] as number[],
  freqMode: 'DAILY' as FreqMode,
  frequency: 1,
  weekDays: [] as number[],
  monthDays: [] as number[],
  timeSlots: [] as Array<{ start: string; end: string }>,
  skipHolidays: false,
  inspectorIds: [] as number[],
})

// ── Evaluation Dialog ──
const evalDialogVisible = ref(false)
const editingSectionId = ref<number | null>(null)
const editingSectionTargetType = computed(() => {
  if (!editingSectionId.value) return null
  // Try flat sections first
  const sec = props.sections.find(s => Number(s.id) === Number(editingSectionId.value))
  if (sec?.targetType) return sec.targetType
  // Fallback: look in tree
  if (hasTreeData.value) {
    const node = flattenTree(props.sectionTree || []).find(n => n.id === Number(editingSectionId.value))
    return node?.targetType || null
  }
  return null
})
const editingIndicator = ref<Indicator | null>(null)
const evalSaving = ref(false)

const evalForm = ref({
  evaluationPeriod: 'PER_TASK',
  gradeSchemeId: null as number | null,
  evaluationMethod: 'PERCENT_RANGE',
  sourceAggregation: 'AVG',
  normalization: 'NONE',
})
// Extra fields for intermediate section eval (composite-like)
const evalCompositeAgg = ref('WEIGHTED_AVG')
const evalMissingPolicy = ref('SKIP')
const normConfig = ref({ countType: 'USER', relation: 'member', value: 50, typeFilter: '' })
const typeFilterOptions = ref<Array<{ code: string; name: string }>>([])

async function loadTypeFilterOptions(countType: string) {
  try {
    let items: any[] = []
    if (countType === 'USER') items = await userTypeApi.getEnabled()
    else if (countType === 'ORG') items = await orgTypeApi.getEnabled()
    else if (countType === 'PLACE') items = await universalPlaceTypeApi.getEnabled()
    typeFilterOptions.value = items.map((t: any) => ({ code: t.typeCode || t.code || String(t.id), name: t.typeName || t.name || t.typeCode }))
  } catch { typeFilterOptions.value = [] }
}
const thresholdMap = ref<Record<string, number>>({})
const minScoreMap = ref<Record<string, number | null>>({})

// ── Composite Dialog ──
const compositeDialogVisible = ref(false)
const editingComposite = ref<Indicator | null>(null)
const compositeSaving = ref(false)

const compositeForm = ref({
  name: '',
  evaluationPeriod: 'WEEKLY',
  compositeAggregation: 'WEIGHTED_AVG',
  missingPolicy: 'SKIP',
  gradeSchemeId: null as number | null,
  evaluationMethod: 'PERCENT_RANGE',
  normalization: 'NONE',
})
const compositeNormConfig = ref({ countType: 'USER', relation: 'member', value: 50, typeFilter: '' })
const compositeThresholdMap = ref<Record<string, number>>({})
const compositeMinScoreMap = ref<Record<string, number | null>>({})

// ══════════════════════════════════════════════
//  Helpers
// ══════════════════════════════════════════════

function toggleArray<T>(arr: T[], val: T) {
  const idx = arr.indexOf(val)
  idx >= 0 ? arr.splice(idx, 1) : arr.push(val)
}

const sectionMap = computed(() => {
  const m = new Map<number, string>()
  for (const s of props.sections) m.set(Number(s.id), s.sectionName)
  return m
})

// ── Section tree flattened with depth (including root as "综合评价") ──
const flatSectionsWithDepth = computed(() => {
  const result: Array<{ node: SectionTreeNode; depth: number; isRoot?: boolean }> = []
  // Add root section as top-level composite entry
  if (props.rootSectionId && props.rootSectionName) {
    result.push({
      node: {
        id: Number(props.rootSectionId),
        sectionName: props.rootSectionName + '（综合评价）',
        parentSectionId: null,
        sortOrder: -1,
        children: props.sectionTree || [],
        isLeaf: false,
      },
      depth: 0,
      isRoot: true,
    })
  }
  const walk = (nodes: SectionTreeNode[], depth: number) => {
    for (const n of nodes) {
      result.push({ node: n, depth: depth })
      walk(n.children, depth + 1)
    }
  }
  walk(props.sectionTree || [], props.rootSectionId ? 1 : 0)
  return result
})

// Whether the section tree is available (multi-level mode)
const hasTreeData = computed(() => (props.sectionTree?.length ?? 0) > 0)

// Whether the section being edited in the eval dialog is a leaf
const editingSectionIsLeaf = computed(() => {
  if (!editingSectionId.value) return true
  if (!hasTreeData.value) return true
  const node = flattenTree(props.sectionTree || []).find(n => n.id === Number(editingSectionId.value))
  return node?.isLeaf ?? true
})

const inspectorMap = computed(() => {
  const m = new Map<number, string>()
  for (const i of (props.inspectors || [])) m.set(Number(i.userId), i.userName)
  return m
})

// ── Plan matching ──
function parsePlanSectionIds(plan: InspectionPlan): number[] {
  try { return JSON.parse(plan.sectionIds || '[]') } catch { return [] }
}

function parsePlanInspectorIds(plan: InspectionPlan): number[] {
  try { return JSON.parse(plan.inspectorIds || '[]') } catch { return [] }
}

// Which plans cover a given section (empty sectionIds = all sections)
function plansForSection(sectionId: number | string): InspectionPlan[] {
  const sid = Number(sectionId)
  return plans.value.filter(p => {
    const ids = parsePlanSectionIds(p)
    return ids.length === 0 || ids.some(id => Number(id) === sid)
  })
}

// For tree mode: check plans for intermediate sections by looking at children
function plansForSectionRecursive(sectionId: number | string): InspectionPlan[] {
  const direct = plansForSection(sectionId)
  if (direct.length > 0) return direct
  if (!hasTreeData.value) return []
  const node = flattenTree(props.sectionTree || []).find(n => n.id === Number(sectionId))
  if (node && !node.isLeaf) {
    const childIds = flattenTree(node.children).map(c => c.id)
    return plans.value.filter(p => {
      const ids = parsePlanSectionIds(p)
      return ids.some(id => childIds.includes(Number(id)))
    })
  }
  return []
}

// Find indicator for a section (leaf or composite for intermediate)
function indicatorForSectionAny(sectionId: number | string): Indicator | undefined {
  const sid = Number(sectionId)
  return indicators.value.find(
    i => Number(i.sourceSectionId) === sid && !i.parentIndicatorId
  )
}

// Find leaf indicator for a section
function indicatorForSection(sectionId: number | string): Indicator | undefined {
  const sid = Number(sectionId)
  return indicators.value.find(
    i => i.indicatorType === 'LEAF' && Number(i.sourceSectionId) === sid && !i.parentIndicatorId
  )
}

// Get root-level composite indicators
const compositeIndicators = computed(() =>
  indicators.value.filter(i => i.indicatorType === 'COMPOSITE' && !i.parentIndicatorId)
    .sort((a, b) => a.sortOrder - b.sortOrder)
)

// ── Display formatters ──
function fmtSchedule(plan: InspectionPlan): string {
  if (plan.scheduleMode === 'ON_DEMAND') return '不定期（手动触发）'
  let s = ''
  const freq = plan.frequency > 1 ? ` ${plan.frequency}次` : ''
  if (plan.cycleType === 'DAILY') s = '每天' + freq
  else if (plan.cycleType === 'WEEKLY') {
    try {
      const days: number[] = JSON.parse(plan.scheduleDays || '[]')
      s = '每周' + (days.length ? days.map(d => WEEKDAYS.find(w => w.v === d)?.l || d).join('、') : '') + freq
    } catch { s = '每周' + freq }
  } else if (plan.cycleType === 'MONTHLY') {
    try {
      const days: number[] = JSON.parse(plan.scheduleDays || '[]')
      s = '每月' + (days.length ? days.map(d => d + '日').join('、') : '') + freq
    } catch { s = '每月' + freq }
  }
  if (plan.timeSlots) {
    try {
      const slots = JSON.parse(plan.timeSlots)
      if (Array.isArray(slots) && slots.length) {
        s += ' ' + slots.map((t: any) => `${t.start}-${t.end}`).join(' / ')
      }
    } catch {}
  }
  return s
}

function fmtSections(plan: InspectionPlan): string {
  const ids = parsePlanSectionIds(plan)
  if (!ids.length) return '全部分区'
  return ids.map(id => sectionMap.value.get(id) || `#${id}`).join('、')
}

function fmtInspectors(plan: InspectionPlan): string {
  const ids = parsePlanInspectorIds(plan)
  if (!ids.length) return '全员可领取'
  return ids.map(id => inspectorMap.value.get(id) || `#${id}`).join('、')
}

function fmtScheduleShort(plan: InspectionPlan): string {
  if (plan.scheduleMode === 'ON_DEMAND') return '手动'
  if (plan.cycleType === 'DAILY') return `每天${plan.frequency > 1 ? plan.frequency + '次' : ''}`
  if (plan.cycleType === 'WEEKLY') return `每周`
  if (plan.cycleType === 'MONTHLY') return `每月`
  return plan.cycleType
}

function periodLabel(p: string): string {
  return EVAL_PERIOD_OPTIONS.find(o => o.value === p)?.label || p
}

function methodLabel(m: string | null): string {
  return EVALUATION_METHOD_OPTIONS.find(o => o.value === m)?.label || m || ''
}

function normLabel(n: string | null): string {
  if (!n || n === 'NONE') return '不归一化'
  return NORMALIZATION_OPTIONS.find(o => o.value === n)?.label || n
}

function aggLabel(agg: string | null): string {
  return COMPOSITE_AGG_OPTIONS.find(o => o.value === agg)?.label || agg || ''
}

function missingLabel(m: string | null): string {
  return MISSING_POLICY_OPTIONS.find(o => o.value === m)?.label || m || ''
}

function schemeName(id: number | null): string {
  if (!id) return ''
  return gradeSchemes.value.find(s => s.id === id)?.displayName || ''
}

function fmtThresholdsShort(ind: Indicator): string {
  if (!ind.gradeSchemeId || !ind.gradeThresholds) return ''
  const scheme = gradeSchemes.value.find(s => s.id === ind.gradeSchemeId)
  if (!scheme) return ''
  try {
    const thresholds = JSON.parse(ind.gradeThresholds) as Array<{ gradeCode: string; value: number }>
    const method = ind.evaluationMethod
    const unit = method === 'SCORE_RANGE' ? '分' : method === 'PERCENT_RANGE' ? '%' : method === 'RANK_COUNT' ? '名' : '%'
    const op = (method === 'RANK_COUNT' || method === 'RANK_PERCENT') ? '前' : '>='
    return scheme.grades
      .map(g => {
        const t = thresholds.find(th => th.gradeCode === g.code)
        if (!t) return null
        return `${op}${t.value}${unit}${g.name}`
      })
      .filter(Boolean)
      .join(' ')
  } catch { return '' }
}

// ── Grade helpers (shared) ──
function selectedSchemeGrades(schemeId: number | null): GradeScheme['grades'] {
  if (!schemeId) return []
  return gradeSchemes.value.find(s => s.id === schemeId)?.grades || []
}

// ══════════════════════════════════════════════
//  Data Loading
// ══════════════════════════════════════════════

async function loadAll() {
  loading.value = true
  try {
    const [p, ind, gs, proj] = await Promise.all([
      inspPlanApi.list(props.projectId),
      getIndicators(props.projectId),
      getGradeSchemes(),
      getProject(props.projectId),
    ])
    plans.value = p
    indicators.value = ind
    gradeSchemes.value = gs
    // 从项目 scopeConfig 获取目标数量
    if (proj.scopeConfig) {
      try { targetCount.value = JSON.parse(proj.scopeConfig).length } catch { targetCount.value = 0 }
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

watch(() => props.projectId, () => { if (props.projectId) loadAll() })
onMounted(() => { if (props.projectId) loadAll() })

// ══════════════════════════════════════════════
//  Schedule Group Dialog
// ══════════════════════════════════════════════

function openAddSchedule() {
  editingPlan.value = null
  scheduleForm.value = {
    planName: '', sectionIds: [], freqMode: 'DAILY', frequency: 1,
    weekDays: [], monthDays: [], timeSlots: [],
    skipHolidays: false, inspectorIds: [],
  }
  scheduleDialogVisible.value = true
}

function openEditSchedule(plan: InspectionPlan) {
  editingPlan.value = plan
  let sectionIds: number[] = [], inspectorIds: number[] = []
  let weekDays: number[] = [], monthDays: number[] = []
  let timeSlots: Array<{ start: string; end: string }> = []
  try { sectionIds = JSON.parse(plan.sectionIds || '[]') } catch {}
  try { inspectorIds = JSON.parse(plan.inspectorIds || '[]') } catch {}
  try {
    const days: number[] = JSON.parse(plan.scheduleDays || '[]')
    if (plan.cycleType === 'WEEKLY') weekDays = days
    else if (plan.cycleType === 'MONTHLY') monthDays = days
  } catch {}
  try {
    const raw = JSON.parse(plan.timeSlots || '[]')
    if (Array.isArray(raw)) timeSlots = raw.map((t: any) => ({ start: t.start || '', end: t.end || '' }))
  } catch {}
  let freqMode: FreqMode = 'DAILY'
  if (plan.scheduleMode === 'ON_DEMAND') freqMode = 'ON_DEMAND'
  else if (plan.cycleType === 'WEEKLY') freqMode = 'WEEKLY'
  else if (plan.cycleType === 'MONTHLY') freqMode = 'MONTHLY'
  scheduleForm.value = { planName: plan.planName, sectionIds, freqMode, frequency: plan.frequency || 1, weekDays, monthDays, timeSlots, skipHolidays: plan.skipHolidays, inspectorIds }
  scheduleDialogVisible.value = true
}

async function handleSaveSchedule() {
  if (!scheduleForm.value.planName.trim()) { ElMessage.warning('请输入调度组名称'); return }
  scheduleSaving.value = true
  try {
    const fm = scheduleForm.value.freqMode
    const data = {
      planName: scheduleForm.value.planName,
      sectionIds: JSON.stringify(scheduleForm.value.sectionIds),
      scheduleMode: fm === 'ON_DEMAND' ? 'ON_DEMAND' : 'REGULAR',
      cycleType: fm === 'ON_DEMAND' ? 'DAILY' : fm,
      frequency: scheduleForm.value.frequency,
      scheduleDays: fm === 'WEEKLY' ? JSON.stringify(scheduleForm.value.weekDays) :
                    fm === 'MONTHLY' ? JSON.stringify(scheduleForm.value.monthDays) : undefined,
      timeSlots: scheduleForm.value.timeSlots.length ? JSON.stringify(scheduleForm.value.timeSlots) : undefined,
      skipHolidays: scheduleForm.value.skipHolidays,
      inspectorIds: scheduleForm.value.inspectorIds.length ? JSON.stringify(scheduleForm.value.inspectorIds) : undefined,
    }
    if (editingPlan.value) {
      await inspPlanApi.update(editingPlan.value.id, data)
      ElMessage.success('已更新')
    } else {
      await inspPlanApi.create({ ...data, projectId: props.projectId } as CreatePlanRequest)
      ElMessage.success('已创建')
    }
    scheduleDialogVisible.value = false
    await loadAll()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
  finally { scheduleSaving.value = false }
}

async function handleDeleteSchedule(plan: InspectionPlan) {
  try {
    await ElMessageBox.confirm(`删除「${plan.planName}」？`, '确认', { type: 'warning' })
    await inspPlanApi.delete(plan.id)
    ElMessage.success('已删除')
    await loadAll()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
}

async function handleToggleSchedule(plan: InspectionPlan) {
  try {
    plan.isEnabled ? await inspPlanApi.disable(plan.id) : await inspPlanApi.enable(plan.id)
    ElMessage.success(plan.isEnabled ? '已禁用' : '已启用')
    await loadAll()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
}

async function handleTriggerSchedule(plan: InspectionPlan) {
  try {
    await ElMessageBox.confirm(`立即触发「${plan.planName}」生成检查任务？`, '确认', { type: 'info' })
    await inspPlanApi.trigger(plan.id)
    ElMessage.success('任务已生成')
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '触发失败') }
}

function addTimeSlot() {
  scheduleForm.value.timeSlots.push({ start: '08:00', end: '09:00' })
}

function removeTimeSlot(i: number) {
  scheduleForm.value.timeSlots.splice(i, 1)
}

// ══════════════════════════════════════════════
//  Evaluation Dialog (Leaf Indicator per Section)
// ══════════════════════════════════════════════

function openEditEval(sectionId: number) {
  editingSectionId.value = sectionId
  const existing = indicatorForSectionAny(sectionId) || indicatorForSection(sectionId)
  editingIndicator.value = existing || null
  if (existing) {
    evalForm.value = {
      evaluationPeriod: existing.evaluationPeriod,
      gradeSchemeId: existing.gradeSchemeId,
      evaluationMethod: existing.evaluationMethod || 'PERCENT_RANGE',
      sourceAggregation: existing.sourceAggregation || 'AVG',
      normalization: existing.normalization || 'NONE',
    }
    // Also populate composite fields if editing existing composite indicator
    if (existing.indicatorType === 'COMPOSITE') {
      evalCompositeAgg.value = existing.compositeAggregation || 'WEIGHTED_AVG'
      evalMissingPolicy.value = existing.missingPolicy || 'SKIP'
    } else {
      evalCompositeAgg.value = 'WEIGHTED_AVG'
      evalMissingPolicy.value = 'SKIP'
    }
    normConfig.value = { countType: 'USER', relation: 'member', value: 50 }
    if (existing.normalizationConfig) {
      try { Object.assign(normConfig.value, JSON.parse(existing.normalizationConfig)) } catch {}
    }
    thresholdMap.value = {}
    minScoreMap.value = {}
    if (existing.gradeThresholds) {
      try {
        const arr = JSON.parse(existing.gradeThresholds) as Array<{ gradeCode: string; value: number; minScore?: number }>
        for (const t of arr) {
          thresholdMap.value[t.gradeCode] = t.value
          if (t.minScore != null) minScoreMap.value[t.gradeCode] = t.minScore
        }
      } catch {}
    }
  } else {
    evalForm.value = {
      evaluationPeriod: 'PER_TASK', gradeSchemeId: null,
      evaluationMethod: 'PERCENT_RANGE', sourceAggregation: 'AVG',
      normalization: 'NONE',
    }
    evalCompositeAgg.value = 'WEIGHTED_AVG'
    evalMissingPolicy.value = 'SKIP'
    normConfig.value = { countType: 'USER', relation: 'member', value: 50 }
    thresholdMap.value = {}
    minScoreMap.value = {}
  }
  evalDialogVisible.value = true
  // 预加载类型过滤选项
  loadTypeFilterOptions(normConfig.value.countType || 'USER')
}

async function handleSaveEval() {
  if (!editingSectionId.value) return
  const sectionId = editingSectionId.value
  const sectionName = sectionMap.value.get(Number(sectionId)) || flattenTree(props.sectionTree || []).find(n => n.id === Number(sectionId))?.sectionName || `分区#${sectionId}`
  const normCfg = evalForm.value.normalization !== 'NONE' ? JSON.stringify(normConfig.value) : undefined
  const grades = selectedSchemeGrades(evalForm.value.gradeSchemeId)
  const serializedThresholds = evalForm.value.gradeSchemeId && grades.length
    ? JSON.stringify(grades.map(g => ({
        gradeCode: g.code,
        value: thresholdMap.value[g.code] || 0,
        ...(minScoreMap.value[g.code] != null ? { minScore: minScoreMap.value[g.code] } : {}),
      })))
    : undefined

  const isLeaf = editingSectionIsLeaf.value

  // 自动查找父分区的 indicator ID（镜像分区树层级）
  const sectionNode = flattenTree(props.sectionTree || []).find(n => n.id === Number(sectionId))
  let parentIndicatorId: number | null = null
  if (sectionNode?.parentSectionId) {
    const parentInd = indicators.value.find(i =>
      Number(i.sourceSectionId) === Number(sectionNode.parentSectionId) ||
      (i.indicatorType === 'COMPOSITE' && i.name === sectionMap.value.get(Number(sectionNode.parentSectionId)))
    )
    if (parentInd) parentIndicatorId = parentInd.id
  }

  evalSaving.value = true
  try {
    if (editingIndicator.value) {
      const updateData: Record<string, any> = {
        evaluationPeriod: evalForm.value.evaluationPeriod,
        gradeSchemeId: evalForm.value.gradeSchemeId,
        evaluationMethod: evalForm.value.evaluationMethod,
        gradeThresholds: serializedThresholds,
        normalization: evalForm.value.normalization,
        normalizationConfig: normCfg,
      }
      if (isLeaf) {
        updateData.sourceAggregation = evalForm.value.sourceAggregation
      } else {
        updateData.compositeAggregation = evalCompositeAgg.value
        updateData.missingPolicy = evalMissingPolicy.value
      }
      await updateIndicator(editingIndicator.value.id, updateData)
      ElMessage.success('评价配置已更新')
    } else if (isLeaf) {
      await createLeafIndicator({
        projectId: props.projectId,
        parentIndicatorId: parentIndicatorId,
        name: sectionName,
        sourceSectionId: Number(sectionId),
        sourceAggregation: evalForm.value.sourceAggregation,
        evaluationPeriod: evalForm.value.evaluationPeriod,
        gradeSchemeId: evalForm.value.gradeSchemeId,
        evaluationMethod: evalForm.value.evaluationMethod,
        gradeThresholds: serializedThresholds,
        normalization: evalForm.value.normalization,
        normalizationConfig: normCfg,
        sortOrder: indicators.value.filter(i => !i.parentIndicatorId).length,
      })
      ElMessage.success('评价配置已创建')
    } else {
      // Intermediate section -> create composite indicator
      await createCompositeIndicator({
        projectId: props.projectId,
        parentIndicatorId: parentIndicatorId,
        name: sectionName,
        sourceSectionId: Number(sectionId),
        compositeAggregation: evalCompositeAgg.value,
        missingPolicy: evalMissingPolicy.value,
        evaluationPeriod: evalForm.value.evaluationPeriod,
        gradeSchemeId: evalForm.value.gradeSchemeId,
        evaluationMethod: evalForm.value.evaluationMethod,
        gradeThresholds: serializedThresholds,
        normalization: evalForm.value.normalization,
        normalizationConfig: normCfg,
        sortOrder: indicators.value.filter(i => !i.parentIndicatorId).length,
      })
      ElMessage.success('评价配置已创建')
    }
    evalDialogVisible.value = false
    await loadAll()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
  finally { evalSaving.value = false }
}

async function handleDeleteEval(ind: Indicator) {
  try {
    await ElMessageBox.confirm(`删除此分区的评价配置？`, '确认删除', { type: 'warning' })
    await deleteIndicator(ind.id)
    ElMessage.success('已删除')
    await loadAll()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
}

// ══════════════════════════════════════════════
//  Composite Indicator Dialog
// ══════════════════════════════════════════════

function openAddComposite() {
  editingComposite.value = null
  compositeForm.value = {
    name: '', evaluationPeriod: 'WEEKLY', compositeAggregation: 'WEIGHTED_AVG',
    missingPolicy: 'SKIP', gradeSchemeId: null, evaluationMethod: 'PERCENT_RANGE',
    normalization: 'NONE',
  }
  compositeNormConfig.value = { countType: 'USER', relation: 'member', value: 50 }
  compositeThresholdMap.value = {}
  compositeMinScoreMap.value = {}
  compositeDialogVisible.value = true
}

function openEditComposite(ind: Indicator) {
  editingComposite.value = ind
  compositeForm.value = {
    name: ind.name,
    evaluationPeriod: ind.evaluationPeriod,
    compositeAggregation: ind.compositeAggregation || 'WEIGHTED_AVG',
    missingPolicy: ind.missingPolicy || 'SKIP',
    gradeSchemeId: ind.gradeSchemeId,
    evaluationMethod: ind.evaluationMethod || 'PERCENT_RANGE',
    normalization: ind.normalization || 'NONE',
  }
  compositeNormConfig.value = { countType: 'USER', relation: 'member', value: 50 }
  if (ind.normalizationConfig) {
    try { Object.assign(compositeNormConfig.value, JSON.parse(ind.normalizationConfig)) } catch {}
  }
  compositeThresholdMap.value = {}
  compositeMinScoreMap.value = {}
  if (ind.gradeThresholds) {
    try {
      const arr = JSON.parse(ind.gradeThresholds) as Array<{ gradeCode: string; value: number; minScore?: number }>
      for (const t of arr) {
        compositeThresholdMap.value[t.gradeCode] = t.value
        if (t.minScore != null) compositeMinScoreMap.value[t.gradeCode] = t.minScore
      }
    } catch {}
  }
  compositeDialogVisible.value = true
}

async function handleSaveComposite() {
  if (!compositeForm.value.name.trim()) { ElMessage.warning('请输入名称'); return }
  const normCfg = compositeForm.value.normalization !== 'NONE' ? JSON.stringify(compositeNormConfig.value) : undefined
  const grades = selectedSchemeGrades(compositeForm.value.gradeSchemeId)
  const serializedThresholds = compositeForm.value.gradeSchemeId && grades.length
    ? JSON.stringify(grades.map(g => ({
        gradeCode: g.code,
        value: compositeThresholdMap.value[g.code] || 0,
        ...(compositeMinScoreMap.value[g.code] != null ? { minScore: compositeMinScoreMap.value[g.code] } : {}),
      })))
    : undefined

  compositeSaving.value = true
  try {
    if (editingComposite.value) {
      await updateIndicator(editingComposite.value.id, {
        name: compositeForm.value.name,
        evaluationPeriod: compositeForm.value.evaluationPeriod,
        compositeAggregation: compositeForm.value.compositeAggregation,
        missingPolicy: compositeForm.value.missingPolicy,
        gradeSchemeId: compositeForm.value.gradeSchemeId,
        evaluationMethod: compositeForm.value.evaluationMethod,
        gradeThresholds: serializedThresholds,
        normalization: compositeForm.value.normalization,
        normalizationConfig: normCfg,
      })
      ElMessage.success('已更新')
    } else {
      await createCompositeIndicator({
        projectId: props.projectId,
        parentIndicatorId: null,
        name: compositeForm.value.name,
        compositeAggregation: compositeForm.value.compositeAggregation,
        missingPolicy: compositeForm.value.missingPolicy,
        evaluationPeriod: compositeForm.value.evaluationPeriod,
        gradeSchemeId: compositeForm.value.gradeSchemeId,
        evaluationMethod: compositeForm.value.evaluationMethod,
        gradeThresholds: serializedThresholds,
        normalization: compositeForm.value.normalization,
        normalizationConfig: normCfg,
        sortOrder: compositeIndicators.value.length,
      })
      ElMessage.success('已创建')
    }
    compositeDialogVisible.value = false
    await loadAll()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
  finally { compositeSaving.value = false }
}

async function handleDeleteComposite(ind: Indicator) {
  try {
    await ElMessageBox.confirm(`删除「${ind.name}」？`, '确认删除', { type: 'warning' })
    await deleteIndicator(ind.id)
    ElMessage.success('已删除')
    await loadAll()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
}

// ══════════════════════════════════════════════
//  Expose reload
// ══════════════════════════════════════════════

defineExpose({ reload: loadAll })
</script>

<template>
  <div class="scv" v-loading="loading">

    <!-- ═══════════════════════════════════════════ -->
    <!--  Section 1: Schedule Groups                -->
    <!-- ═══════════════════════════════════════════ -->
    <div class="scv-section">
      <div class="scv-head">
        <div class="scv-head-left">
          <Calendar class="w-4 h-4" style="color:#8b5cf6" />
          <span class="scv-title">调度组</span>
          <span v-if="plans.length" class="scv-count">{{ plans.length }}</span>
        </div>
        <button class="scv-add-btn" @click="openAddSchedule">
          <Plus class="w-3.5 h-3.5" /> 添加调度组
        </button>
      </div>

      <!-- Empty -->
      <div v-if="!plans.length && !loading" class="scv-empty-inline">
        <AlertTriangle class="w-4 h-4" style="color:#d97706" />
        <span>暂未配置检查调度，添加调度组来安排检查频率和分工</span>
      </div>

      <!-- Plan cards -->
      <div class="scv-schedule-list">
        <div v-for="plan in plans" :key="plan.id" class="sc-card" :class="{ off: !plan.isEnabled }">
          <div class="sc-card-top">
            <span class="sc-card-name">{{ plan.planName }}</span>
            <div class="sc-pills">
              <span class="sc-pill" :class="plan.scheduleMode === 'ON_DEMAND' ? 'amber' : 'purple'">
                {{ plan.scheduleMode === 'ON_DEMAND' ? '手动' : '定期' }}
              </span>
              <span class="sc-pill" :class="plan.isEnabled ? 'green' : 'gray'">
                {{ plan.isEnabled ? '启用' : '禁用' }}
              </span>
            </div>
            <div class="sc-card-ops">
              <button class="sc-op" @click="handleToggleSchedule(plan)" :title="plan.isEnabled ? '禁用' : '启用'"><Zap class="w-3.5 h-3.5" /></button>
              <button v-if="plan.scheduleMode==='ON_DEMAND'" class="sc-op accent" @click="handleTriggerSchedule(plan)" title="触发"><Play class="w-3.5 h-3.5" /></button>
              <button class="sc-op" @click="openEditSchedule(plan)" title="编辑"><Pencil class="w-3.5 h-3.5" /></button>
              <button class="sc-op danger" @click="handleDeleteSchedule(plan)" title="删除"><Trash2 class="w-3.5 h-3.5" /></button>
            </div>
          </div>
          <div class="sc-card-info">
            <span><Clock class="w-3 h-3" /> {{ fmtSchedule(plan) }}</span>
            <span class="sc-dot" />
            <span><Users class="w-3 h-3" /> {{ fmtInspectors(plan) }}</span>
            <span v-if="plan.skipHolidays" class="sc-dot" />
            <span v-if="plan.skipHolidays" style="color:#d97706">跳过节假日</span>
          </div>
          <div class="sc-card-sections">
            {{ fmtSections(plan) }}
          </div>
        </div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════ -->
    <!--  Section 2: Per-section evaluation         -->
    <!-- ═══════════════════════════════════════════ -->
    <div class="scv-section">
      <div class="scv-head">
        <div class="scv-head-left">
          <BarChart3 class="w-4 h-4" style="color:#8b5cf6" />
          <span class="scv-title">分区评价配置</span>
          <span v-if="hasTreeData" class="scv-count">{{ flatSectionsWithDepth.length }} 个分区</span>
          <span v-else-if="props.sections.length" class="scv-count">{{ props.sections.length }} 个分区</span>
        </div>
      </div>

      <div v-if="!hasTreeData && !props.sections.length && !loading" class="scv-empty-inline">
        <AlertTriangle class="w-4 h-4" style="color:#9ca3af" />
        <span>项目暂无分区</span>
      </div>

      <div class="scv-eval-list">
        <!-- ── Tree mode: render with depth indentation ── -->
        <template v-if="hasTreeData">
          <div v-for="{ node: section, depth } in flatSectionsWithDepth" :key="section.id"
            class="ev-card" :class="{ 'ev-card-intermediate': !section.isLeaf }"
            :style="{ marginLeft: depth * 20 + 'px' }">
            <div class="ev-card-top">
              <div class="ev-card-dot" :class="{ intermediate: !section.isLeaf }" />
              <span class="ev-card-name">{{ section.sectionName }}</span>
              <span v-if="!section.isLeaf" class="ev-card-type-tag">分组</span>
              <div class="ev-card-ops">
                <template v-if="indicatorForSectionAny(section.id)">
                  <button class="sc-op" @click="openEditEval(section.id)" title="编辑评价"><Pencil class="w-3.5 h-3.5" /></button>
                  <button class="sc-op danger" @click="handleDeleteEval(indicatorForSectionAny(section.id)!)" title="删除评价"><Trash2 class="w-3.5 h-3.5" /></button>
                </template>
                <button v-else class="ev-config-btn" @click="openEditEval(section.id)">
                  <Settings2 class="w-3 h-3" /> 配置评价
                </button>
              </div>
            </div>

            <!-- Has indicator -->
            <template v-if="indicatorForSectionAny(section.id)">
              <div class="ev-card-body">
                <div class="ev-row">
                  <BarChart3 class="w-3 h-3 ev-row-icon" />
                  <span v-if="indicatorForSectionAny(section.id)!.gradeSchemeId" class="ev-detail">
                    {{ schemeName(indicatorForSectionAny(section.id)!.gradeSchemeId) }}
                    <span class="ev-sep">/</span>
                    {{ methodLabel(indicatorForSectionAny(section.id)!.evaluationMethod) }}
                    <span v-if="fmtThresholdsShort(indicatorForSectionAny(section.id)!)" class="ev-thresh">{{ fmtThresholdsShort(indicatorForSectionAny(section.id)!) }}</span>
                  </span>
                  <span v-else class="ev-detail muted">未配置等级方案</span>
                </div>
                <div class="ev-row">
                  <Calendar class="w-3 h-3 ev-row-icon" />
                  <span class="ev-detail">
                    {{ periodLabel(indicatorForSectionAny(section.id)!.evaluationPeriod) }}
                    <template v-if="!section.isLeaf">
                      <span class="ev-sep">/</span>
                      汇总: {{ aggLabel(indicatorForSectionAny(section.id)!.compositeAggregation) }}
                      <span class="ev-sep">/</span>
                      缺失: {{ missingLabel(indicatorForSectionAny(section.id)!.missingPolicy) }}
                    </template>
                    <template v-else>
                      <span class="ev-sep">/</span>
                      归一化: {{ normLabel(indicatorForSectionAny(section.id)!.normalization) }}
                    </template>
                  </span>
                </div>
              </div>
            </template>

            <!-- No indicator -->
            <template v-else>
              <div class="ev-card-body">
                <div class="ev-row warn">
                  <AlertTriangle class="w-3 h-3" />
                  <span>暂未配置评价</span>
                </div>
              </div>
            </template>

            <!-- Schedule info -->
            <div v-if="section.isLeaf" class="ev-card-schedule">
              <template v-if="plansForSectionRecursive(section.id).length">
                <Clock class="w-3 h-3 ev-row-icon" />
                <span class="ev-detail">
                  调度:
                  <span v-for="(p, idx) in plansForSectionRecursive(section.id)" :key="p.id">
                    {{ p.planName }} ({{ fmtScheduleShort(p) }})<template v-if="idx < plansForSectionRecursive(section.id).length - 1">、</template>
                  </span>
                </span>
              </template>
              <template v-else>
                <div class="ev-row warn">
                  <AlertTriangle class="w-3 h-3" />
                  <span>暂无检查调度</span>
                </div>
              </template>
            </div>
            <div v-else class="ev-card-schedule">
              <Layers class="w-3 h-3 ev-row-icon" />
              <span class="ev-detail muted">包含 {{ section.children.length }} 个子分区</span>
            </div>
          </div>
        </template>

        <!-- ── Flat mode (fallback): original flat list ── -->
        <template v-else>
          <div v-for="section in props.sections" :key="section.id" class="ev-card">
            <div class="ev-card-top">
              <div class="ev-card-dot" />
              <span class="ev-card-name">{{ section.sectionName }}</span>
              <div class="ev-card-ops">
                <template v-if="indicatorForSection(section.id)">
                  <button class="sc-op" @click="openEditEval(section.id)" title="编辑评价"><Pencil class="w-3.5 h-3.5" /></button>
                  <button class="sc-op danger" @click="handleDeleteEval(indicatorForSection(section.id)!)" title="删除评价"><Trash2 class="w-3.5 h-3.5" /></button>
                </template>
                <button v-else class="ev-config-btn" @click="openEditEval(section.id)">
                  <Settings2 class="w-3 h-3" /> 配置评价
                </button>
              </div>
            </div>

            <!-- Has indicator -->
            <template v-if="indicatorForSection(section.id)">
              <div class="ev-card-body">
                <div class="ev-row">
                  <BarChart3 class="w-3 h-3 ev-row-icon" />
                  <span v-if="indicatorForSection(section.id)!.gradeSchemeId" class="ev-detail">
                    {{ schemeName(indicatorForSection(section.id)!.gradeSchemeId) }}
                    <span class="ev-sep">/</span>
                    {{ methodLabel(indicatorForSection(section.id)!.evaluationMethod) }}
                    <span v-if="fmtThresholdsShort(indicatorForSection(section.id)!)" class="ev-thresh">{{ fmtThresholdsShort(indicatorForSection(section.id)!) }}</span>
                  </span>
                  <span v-else class="ev-detail muted">未配置等级方案</span>
                </div>
                <div class="ev-row">
                  <Calendar class="w-3 h-3 ev-row-icon" />
                  <span class="ev-detail">
                    {{ periodLabel(indicatorForSection(section.id)!.evaluationPeriod) }}
                    <span class="ev-sep">/</span>
                    归一化: {{ normLabel(indicatorForSection(section.id)!.normalization) }}
                  </span>
                </div>
              </div>
            </template>

            <!-- No indicator -->
            <template v-else>
              <div class="ev-card-body">
                <div class="ev-row warn">
                  <AlertTriangle class="w-3 h-3" />
                  <span>暂未配置评价</span>
                </div>
              </div>
            </template>

            <!-- Schedule info for this section -->
            <div class="ev-card-schedule">
              <template v-if="plansForSection(section.id).length">
                <Clock class="w-3 h-3 ev-row-icon" />
                <span class="ev-detail">
                  调度:
                  <span v-for="(p, idx) in plansForSection(section.id)" :key="p.id">
                    {{ p.planName }} ({{ fmtScheduleShort(p) }})<template v-if="idx < plansForSection(section.id).length - 1">、</template>
                  </span>
                </span>
              </template>
              <template v-else>
                <div class="ev-row warn">
                  <AlertTriangle class="w-3 h-3" />
                  <span>暂无检查调度</span>
                </div>
              </template>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════ -->
    <!--  Dialog 1: Schedule Group                  -->
    <!-- ═══════════════════════════════════════════ -->
    <el-dialog v-model="scheduleDialogVisible"
      :title="editingPlan ? '编辑调度组' : '添加调度组'"
      width="540px" :close-on-click-modal="false" class="scv-dlg">
      <div class="fd">
        <!-- Name -->
        <div class="fd-block">
          <label class="fd-lbl">调度组名称 <b>*</b></label>
          <input v-model="scheduleForm.planName" class="fd-input" placeholder="如：每日常规巡查" />
        </div>

        <!-- Sections -->
        <div class="fd-block">
          <label class="fd-lbl">包含分区 <span class="fd-sub">不选=全部</span></label>
          <div class="fd-pills">
            <button v-for="s in props.sections" :key="Number(s.id)"
              class="fd-pill" :class="{ on: scheduleForm.sectionIds.includes(Number(s.id)) }"
              @click="toggleArray(scheduleForm.sectionIds, Number(s.id))">
              {{ s.sectionName }}
            </button>
          </div>
          <div v-if="!props.sections.length" class="fd-empty">项目暂无分区</div>
        </div>

        <!-- Frequency -->
        <div class="fd-block">
          <label class="fd-lbl">检查频率</label>
          <div class="fd-freq">
            <button class="fd-freq-btn" :class="{ on: scheduleForm.freqMode === 'DAILY' }" @click="scheduleForm.freqMode = 'DAILY'">
              <Calendar class="w-4 h-4" /><span>每天</span>
            </button>
            <button class="fd-freq-btn" :class="{ on: scheduleForm.freqMode === 'WEEKLY' }" @click="scheduleForm.freqMode = 'WEEKLY'">
              <Calendar class="w-4 h-4" /><span>按周</span>
            </button>
            <button class="fd-freq-btn" :class="{ on: scheduleForm.freqMode === 'MONTHLY' }" @click="scheduleForm.freqMode = 'MONTHLY'">
              <Calendar class="w-4 h-4" /><span>按月</span>
            </button>
            <button class="fd-freq-btn" :class="{ on: scheduleForm.freqMode === 'ON_DEMAND' }" @click="scheduleForm.freqMode = 'ON_DEMAND'">
              <Zap class="w-4 h-4" /><span>不定期</span>
            </button>
          </div>
        </div>

        <!-- Weekly day picker -->
        <div v-if="scheduleForm.freqMode === 'WEEKLY'" class="fd-block">
          <label class="fd-lbl">选择星期</label>
          <div class="fd-week">
            <button v-for="d in WEEKDAYS" :key="d.v"
              class="fd-wday" :class="{ on: scheduleForm.weekDays.includes(d.v) }"
              @click="toggleArray(scheduleForm.weekDays, d.v)">
              {{ d.l }}
            </button>
          </div>
        </div>

        <!-- Monthly day picker -->
        <div v-if="scheduleForm.freqMode === 'MONTHLY'" class="fd-block">
          <label class="fd-lbl">选择日期</label>
          <div class="fd-month">
            <button v-for="d in MONTH_DAYS" :key="d"
              class="fd-mday" :class="{ on: scheduleForm.monthDays.includes(d) }"
              @click="toggleArray(scheduleForm.monthDays, d)">
              {{ d }}
            </button>
          </div>
        </div>

        <!-- Frequency per day -->
        <div v-if="scheduleForm.freqMode !== 'ON_DEMAND'" class="fd-block">
          <label class="fd-lbl">每天检查次数</label>
          <div class="fd-freq-count">
            <button v-for="n in [1,2,3,4,5]" :key="n"
              class="fd-fc-btn" :class="{ on: scheduleForm.frequency === n }"
              @click="scheduleForm.frequency = n">
              {{ n }}次
            </button>
          </div>
        </div>

        <!-- Time slots -->
        <div v-if="scheduleForm.freqMode !== 'ON_DEMAND'" class="fd-block">
          <label class="fd-lbl">固定时段 <span class="fd-sub">可选，不设则为不定时抽查</span></label>
          <div class="fd-slots">
            <div v-for="(slot, i) in scheduleForm.timeSlots" :key="i" class="fd-slot">
              <input v-model="slot.start" type="time" class="fd-time" />
              <span class="fd-time-sep">-</span>
              <input v-model="slot.end" type="time" class="fd-time" />
              <button class="fd-slot-del" @click="removeTimeSlot(i)"><X class="w-3 h-3" /></button>
            </div>
            <button class="fd-slot-add" @click="addTimeSlot"><Plus class="w-3 h-3" /> 添加时段</button>
          </div>
        </div>

        <!-- Skip holidays -->
        <label v-if="scheduleForm.freqMode !== 'ON_DEMAND'" class="fd-check-row">
          <input type="checkbox" v-model="scheduleForm.skipHolidays" />
          <span>跳过节假日</span>
        </label>

        <!-- Inspectors -->
        <div v-if="(props.inspectors || []).length > 0" class="fd-block">
          <label class="fd-lbl">指定检查员 <span class="fd-sub">不选=全员可领取</span></label>
          <div class="fd-pills">
            <button v-for="insp in props.inspectors" :key="Number(insp.userId)"
              class="fd-pill" :class="{ on: scheduleForm.inspectorIds.includes(Number(insp.userId)) }"
              @click="toggleArray(scheduleForm.inspectorIds, Number(insp.userId))">
              {{ insp.userName }}
            </button>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="fd-footer">
          <button class="fd-btn ghost" @click="scheduleDialogVisible = false">取消</button>
          <button class="fd-btn primary" :disabled="scheduleSaving" @click="handleSaveSchedule">
            {{ scheduleSaving ? '保存中...' : (editingPlan ? '更新' : '创建') }}
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- ═══════════════════════════════════════════ -->
    <!--  Dialog 2: Section Evaluation              -->
    <!-- ═══════════════════════════════════════════ -->
    <el-dialog v-model="evalDialogVisible"
      :title="editingIndicator ? '编辑分区评价' : '配置分区评价'"
      width="520px" :close-on-click-modal="false" class="scv-dlg">
      <div class="fd">
        <!-- Section name (read-only) -->
        <div class="fd-block">
          <label class="fd-lbl">检查分区</label>
          <div class="fd-readonly">
            {{ sectionMap.get(Number(editingSectionId)) || flattenTree(props.sectionTree || []).find(n => n.id === Number(editingSectionId))?.sectionName || '' }}
            <span v-if="!editingSectionIsLeaf" class="ev-card-type-tag" style="margin-left:6px">分组</span>
          </div>
        </div>

        <!-- Leaf: Evaluation period & Source Aggregation -->
        <div v-if="editingSectionIsLeaf" class="fd-row">
          <div class="fd-block fd-half">
            <label class="fd-lbl">评估周期</label>
            <el-select v-model="evalForm.evaluationPeriod" style="width:100%" size="default">
              <el-option v-for="o in EVAL_PERIOD_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </div>
          <div class="fd-block fd-half">
            <label class="fd-lbl">多次检查合并</label>
            <el-select v-model="evalForm.sourceAggregation" style="width:100%" size="default">
              <el-option v-for="o in SOURCE_AGG_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </div>
        </div>
        <!-- Intermediate: Evaluation period & Composite Aggregation & Missing Policy -->
        <template v-else>
          <div class="fd-row">
            <div class="fd-block fd-half">
              <label class="fd-lbl">评估周期</label>
              <el-select v-model="evalForm.evaluationPeriod" style="width:100%" size="default">
                <el-option v-for="o in EVAL_PERIOD_OPTIONS.filter(x => x.value !== 'PER_TASK')" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </div>
            <div class="fd-block fd-half">
              <label class="fd-lbl">汇总方式</label>
              <el-select v-model="evalCompositeAgg" style="width:100%" size="default">
                <el-option v-for="o in COMPOSITE_AGG_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </div>
          </div>
          <div class="fd-block">
            <label class="fd-lbl">数据缺失策略</label>
            <div class="fd-missing-grid">
              <div v-for="o in MISSING_POLICY_OPTIONS" :key="o.value"
                class="fd-missing-opt" :class="{ on: evalMissingPolicy === o.value }"
                @click="evalMissingPolicy = o.value">
                {{ o.label }}
              </div>
            </div>
          </div>
        </template>

        <!-- Grade scheme -->
        <div class="fd-block">
          <label class="fd-lbl">
            等级方案
            <a href="/inspection/v7/grade-schemes" target="_blank" class="fd-link">
              <ExternalLink class="w-3 h-3" /> 管理方案
            </a>
          </label>
          <el-select v-model="evalForm.gradeSchemeId" clearable placeholder="不映射等级" style="width:100%" size="default">
            <el-option v-for="s in gradeSchemes" :key="s.id" :label="s.displayName" :value="s.id" />
          </el-select>
        </div>

        <!-- Evaluation method -->
        <template v-if="evalForm.gradeSchemeId">
          <div class="fd-block">
            <label class="fd-lbl">评价方式</label>
            <div class="fd-method-grid">
              <div v-for="o in EVALUATION_METHOD_OPTIONS" :key="o.value"
                class="fd-method-opt" :class="{ on: evalForm.evaluationMethod === o.value }"
                @click="evalForm.evaluationMethod = o.value">
                <div class="fd-method-label">{{ o.label }}</div>
                <div class="fd-method-desc">{{ o.description }}</div>
              </div>
            </div>
          </div>

          <!-- Thresholds -->
          <div class="fd-block" v-if="selectedSchemeGrades(evalForm.gradeSchemeId).length">
            <label class="fd-lbl">
              阈值设置
              <span v-if="targetCount > 0 && (evalForm.evaluationMethod === 'RANK_COUNT' || evalForm.evaluationMethod === 'RANK_PERCENT')" class="fd-target-hint">
                当前共 {{ targetCount }} 个检查目标
              </span>
            </label>
            <div class="fd-thresh-list">
              <div v-for="g in selectedSchemeGrades(evalForm.gradeSchemeId)" :key="g.code" class="fd-thresh-row">
                <span class="fd-thresh-grade" :style="{ color: g.color || '#6b7280' }">{{ g.name }}</span>
                <span class="fd-thresh-op">{{ (evalForm.evaluationMethod === 'RANK_COUNT' || evalForm.evaluationMethod === 'RANK_PERCENT') ? '前' : '>=' }}</span>
                <input v-model.number="thresholdMap[g.code]" type="number" class="fd-thresh-input" />
                <span class="fd-thresh-unit">{{ evalForm.evaluationMethod === 'RANK_COUNT' ? '名' : evalForm.evaluationMethod === 'RANK_PERCENT' ? '%' : evalForm.evaluationMethod === 'PERCENT_RANGE' ? '%' : '分' }}</span>
                <template v-if="evalForm.evaluationMethod === 'RANK_COUNT' || evalForm.evaluationMethod === 'RANK_PERCENT'">
                  <span class="fd-thresh-op" style="margin-left:4px">且>=</span>
                  <input v-model.number="minScoreMap[g.code]" type="number" class="fd-thresh-input" placeholder="不限" />
                  <span class="fd-thresh-unit">分</span>
                </template>
              </div>
            </div>
          </div>
        </template>

        <!-- Normalization -->
        <div class="fd-block">
          <label class="fd-lbl">分数归一化 <span class="fd-sub">解决不同体量目标的公平比较问题</span></label>
          <div class="norm-cards">
            <div class="norm-card" :class="{ active: evalForm.normalization === 'NONE' }"
              @click="evalForm.normalization = 'NONE'">
              <div class="norm-card-icon">═</div>
              <div class="norm-card-body">
                <div class="norm-card-name">不归一化</div>
                <div class="norm-card-desc">直接使用原始分数</div>
              </div>
            </div>
            <div class="norm-card" :class="{ active: evalForm.normalization === 'RELATION_COUNT', disabled: editingSectionTargetType === 'USER' }"
              @click="() => {
                if (editingSectionTargetType === 'USER') return
                evalForm.normalization = 'RELATION_COUNT'
                normConfig.countType = 'USER'
                normConfig.relation = editingSectionTargetType === 'PLACE' ? 'occupant' : 'member'
                normConfig.typeFilter = ''
                loadTypeFilterOptions('USER')
              }">
              <div class="norm-card-icon">÷</div>
              <div class="norm-card-body">
                <div class="norm-card-name">按关联数量</div>
                <div class="norm-card-desc">{{ editingSectionTargetType === 'USER' ? '个人评价不适用' : '除以关联实体数量（如人数）' }}</div>
              </div>
            </div>
            <div class="norm-card" :class="{ active: evalForm.normalization === 'FIXED_VALUE' }"
              @click="evalForm.normalization = 'FIXED_VALUE'">
              <div class="norm-card-icon">#</div>
              <div class="norm-card-body">
                <div class="norm-card-name">固定除数</div>
                <div class="norm-card-desc">除以固定数值</div>
              </div>
            </div>
            <div class="norm-card" :class="{ active: evalForm.normalization === 'PERCENTAGE' }"
              @click="evalForm.normalization = 'PERCENTAGE'">
              <div class="norm-card-icon">%</div>
              <div class="norm-card-body">
                <div class="norm-card-name">转百分比</div>
                <div class="norm-card-desc">转换为得分率</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Normalization detail config -->
        <div v-if="evalForm.normalization === 'RELATION_COUNT'" class="norm-config-box">
          <div class="norm-formula-visual">
            <div class="norm-fv-item">
              <span class="norm-fv-label">检查得分</span>
            </div>
            <span class="norm-fv-op">÷</span>
            <div class="norm-fv-item editable">
              <select v-model="normConfig.countType" class="norm-fv-select"
                @change="() => { normConfig.relation = normConfig.countType === 'USER' ? (editingSectionTargetType === 'PLACE' ? 'occupant' : 'member') : 'belongs_to'; normConfig.typeFilter = ''; loadTypeFilterOptions(normConfig.countType) }">
                <option value="USER">关联用户数</option>
                <option value="ORG">关联组织数</option>
                <option value="PLACE">关联场所数</option>
              </select>
            </div>
            <span class="norm-fv-op">=</span>
            <div class="norm-fv-item">
              <span class="norm-fv-label">归一化分</span>
            </div>
          </div>
          <div class="norm-filter-row">
            <span class="norm-filter-label">限定类型</span>
            <select v-model="normConfig.typeFilter" class="norm-filter-select">
              <option value="">全部（不限类型）</option>
              <option v-for="t in typeFilterOptions" :key="t.code" :value="t.code">{{ t.name }}</option>
            </select>
            <span class="norm-filter-hint">{{ normConfig.typeFilter ? `仅统计「${typeFilterOptions.find(t => t.code === normConfig.typeFilter)?.name || normConfig.typeFilter}」` : '统计所有' }}{{ normConfig.countType === 'USER' ? '用户' : normConfig.countType === 'PLACE' ? '场所' : '组织' }}</span>
          </div>
          <div class="norm-example-hint">
            {{ normConfig.typeFilter
              ? `仅统计类型为「${normConfig.typeFilter}」的${normConfig.countType === 'USER' ? '用户' : normConfig.countType === 'PLACE' ? '场所' : '组织'}数量进行归一化`
              : normConfig.countType === 'USER'
              ? '示例: A班50人得85分→人均1.7, B班20人得85分→人均4.25, 消除人数差异'
              : normConfig.countType === 'PLACE'
              ? '示例: A区10间得80分→每间8分, B区5间得80分→每间16分, 消除场所数差异'
              : '示例: A部门3个子组织得90分→每个30, B部门1个→每个90, 消除规模差异' }}
          </div>
        </div>
        <div v-if="evalForm.normalization === 'FIXED_VALUE'" class="norm-config-box">
          <div class="norm-formula-visual">
            <div class="norm-fv-item">
              <span class="norm-fv-label">原始分</span>
              <span class="norm-fv-example">85</span>
            </div>
            <span class="norm-fv-op">÷</span>
            <div class="norm-fv-item editable">
              <input v-model.number="normConfig.value" type="number" min="1" class="norm-fv-fixed" placeholder="50" />
            </div>
            <span class="norm-fv-op">=</span>
            <div class="norm-fv-item">
              <span class="norm-fv-label">归一化分</span>
              <span class="norm-fv-example result">{{ normConfig.value ? (85 / normConfig.value).toFixed(1) : '—' }}</span>
            </div>
          </div>
        </div>
        <div v-if="evalForm.normalization === 'PERCENTAGE'" class="norm-config-box">
          <div class="norm-formula-visual">
            <div class="norm-fv-item">
              <span class="norm-fv-label">原始分</span>
              <span class="norm-fv-example">85</span>
            </div>
            <span class="norm-fv-op">÷</span>
            <div class="norm-fv-item">
              <span class="norm-fv-label">满分</span>
              <span class="norm-fv-example">100</span>
            </div>
            <span class="norm-fv-op">×100 =</span>
            <div class="norm-fv-item">
              <span class="norm-fv-label">得分率</span>
              <span class="norm-fv-example result">85%</span>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="fd-footer">
          <button class="fd-btn ghost" @click="evalDialogVisible = false">取消</button>
          <button class="fd-btn primary" :disabled="evalSaving" @click="handleSaveEval">
            {{ evalSaving ? '保存中...' : (editingIndicator ? '更新' : '创建') }}
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- Composite dialog removed: eval dialog handles both leaf and intermediate -->

  </div>
</template>

<style scoped>
/* ══════════════════════════════════════════════
   Layout & Sections
   ══════════════════════════════════════════════ */
.scv { display: flex; flex-direction: column; gap: 24px; min-height: 120px; }

.scv-section { }

.scv-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px; padding-bottom: 10px; border-bottom: 1px solid #f0f0f3;
}
.scv-head-left { display: flex; align-items: center; gap: 8px; }
.scv-title { font-size: 14px; font-weight: 700; color: #1e1b4b; letter-spacing: -0.3px; }
.scv-count {
  font-size: 10px; color: #8b5cf6; background: #f3f0ff;
  padding: 1px 7px; border-radius: 10px; font-weight: 700;
}

.scv-add-btn {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 6px 14px; border-radius: 8px; font-size: 12px; font-weight: 600;
  background: #8b5cf6; color: #fff; border: none; cursor: pointer; transition: background 0.15s;
}
.scv-add-btn:hover { background: #7c3aed; }

.scv-empty-inline {
  display: flex; align-items: center; gap: 8px;
  padding: 14px 16px; border-radius: 10px;
  background: #fafbfc; border: 1px dashed #e5e7eb;
  font-size: 12px; color: #9ca3af;
}

/* ══════════════════════════════════════════════
   Schedule Group Cards
   ══════════════════════════════════════════════ */
.scv-schedule-list { display: flex; flex-direction: column; gap: 8px; }

.sc-card {
  border: 1px solid #e5e7eb; border-radius: 10px; background: #fff;
  padding: 10px 14px; transition: all 0.15s;
}
.sc-card:hover { box-shadow: 0 2px 8px rgba(139,92,246,0.06); }
.sc-card.off { opacity: 0.45; }

.sc-card-top {
  display: flex; align-items: center; gap: 6px; margin-bottom: 4px;
}
.sc-card-name { font-size: 13px; font-weight: 700; color: #1e1b4b; }
.sc-pills { display: flex; gap: 4px; flex: 1; }
.sc-pill {
  font-size: 9px; font-weight: 700; padding: 1px 6px; border-radius: 4px;
  text-transform: uppercase; letter-spacing: 0.3px;
}
.sc-pill.purple { background: #f3f0ff; color: #8b5cf6; }
.sc-pill.amber { background: #fffbeb; color: #d97706; }
.sc-pill.green { background: #f0fdf4; color: #16a34a; }
.sc-pill.gray { background: #f3f4f6; color: #9ca3af; }

.sc-card-ops { display: flex; gap: 1px; }
.sc-op {
  width: 26px; height: 26px; border: none; border-radius: 6px;
  background: transparent; color: #9ca3af; cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center; transition: all 0.15s;
}
.sc-op:hover { background: #f3f4f6; color: #374151; }
.sc-op.accent:hover { background: #f3f0ff; color: #8b5cf6; }
.sc-op.danger:hover { background: #fef2f2; color: #ef4444; }

.sc-card-info {
  display: flex; align-items: center; gap: 5px;
  font-size: 11px; color: #9ca3af; flex-wrap: wrap; margin-bottom: 2px;
}
.sc-dot { width: 2px; height: 2px; border-radius: 50%; background: #d1d5db; flex-shrink: 0; }

.sc-card-sections {
  font-size: 11px; color: #b0b0b0; padding-top: 3px;
  border-top: 1px solid #f5f5f5; margin-top: 3px;
}

/* ══════════════════════════════════════════════
   Evaluation Cards (per section)
   ══════════════════════════════════════════════ */
.scv-eval-list { display: flex; flex-direction: column; gap: 8px; }

.ev-card {
  border: 1px solid #e5e7eb; border-radius: 10px; background: #fff;
  padding: 10px 14px; transition: all 0.15s;
}
.ev-card:hover { box-shadow: 0 2px 8px rgba(139,92,246,0.06); }

.ev-card-top {
  display: flex; align-items: center; gap: 8px; margin-bottom: 6px;
}
.ev-card-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: #8b5cf6; flex-shrink: 0;
}
.ev-card-name { font-size: 13px; font-weight: 700; color: #1e1b4b; flex: 1; }
.ev-card-ops { display: flex; gap: 2px; }

.ev-config-btn {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 600;
  background: #f5f3ff; color: #7c3aed; border: 1px solid #ddd8fe;
  cursor: pointer; transition: all 0.15s;
}
.ev-config-btn:hover { background: #ede9fe; border-color: #c4b5fd; }

.ev-card-body { padding-left: 15px; }

.ev-row {
  display: flex; align-items: center; gap: 5px;
  font-size: 11px; color: #6b7280; margin-bottom: 3px;
}
.ev-row.warn { color: #d97706; }
.ev-row-icon { color: #c4b5fd; flex-shrink: 0; }
.ev-detail { display: flex; align-items: center; gap: 3px; flex-wrap: wrap; }
.ev-detail.muted { color: #b0b0b0; }
.ev-sep { color: #d1d5db; margin: 0 1px; }
.ev-thresh { color: #9ca3af; font-size: 10px; margin-left: 4px; }

.ev-card-schedule {
  display: flex; align-items: center; gap: 5px;
  font-size: 11px; color: #6b7280;
  padding-top: 5px; padding-left: 15px; margin-top: 4px;
  border-top: 1px solid #f9fafb;
}

/* Intermediate (non-leaf) section cards */
.ev-card-intermediate {
  background: #fafbfc;
  border-left: 3px solid #8b5cf6;
}
.ev-card-dot.intermediate {
  background: #8b5cf6;
  width: 8px; height: 8px;
  border: 2px solid #ddd8fe;
  box-sizing: content-box;
}
.ev-card-type-tag {
  font-size: 9px; font-weight: 700; padding: 1px 5px; border-radius: 3px;
  background: #f3f0ff; color: #8b5cf6; text-transform: uppercase;
  flex-shrink: 0;
}

/* ══════════════════════════════════════════════
   Composite Cards
   ══════════════════════════════════════════════ */
.scv-composite-list { display: flex; flex-direction: column; gap: 8px; }

.cp-card {
  border: 1px solid #e5e7eb; border-radius: 10px; background: #fff;
  padding: 10px 14px; transition: all 0.15s;
}
.cp-card:hover { box-shadow: 0 2px 8px rgba(99,102,241,0.08); }

.cp-card-top {
  display: flex; align-items: center; gap: 8px; margin-bottom: 5px;
}
.cp-card-name { font-size: 13px; font-weight: 700; color: #1e1b4b; flex: 1; }

.cp-card-body {
  display: flex; align-items: center; gap: 5px; flex-wrap: wrap;
  font-size: 11px; color: #9ca3af; padding-left: 22px;
}

/* ══════════════════════════════════════════════
   Dialog Shared (fd-* classes)
   ══════════════════════════════════════════════ */
.fd { display: flex; flex-direction: column; gap: 16px; }
.fd-block { display: flex; flex-direction: column; gap: 6px; }
.fd-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.fd-half { }
.fd-lbl {
  font-size: 12px; font-weight: 700; color: #374151;
  display: flex; align-items: center; gap: 6px;
}
.fd-lbl b { color: #ef4444; font-weight: 700; }
.fd-sub { font-size: 10px; font-weight: 400; color: #b0b0b0; }
.fd-empty { font-size: 11px; color: #d1d5db; }
.fd-link {
  display: inline-flex; align-items: center; gap: 3px; margin-left: auto;
  font-size: 11px; font-weight: 500; color: #8b5cf6; text-decoration: none;
}
.fd-link:hover { text-decoration: underline; }

.fd-input {
  width: 100%; padding: 9px 12px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  font-size: 13px; color: #1f2937; outline: none; transition: border-color 0.15s;
  box-sizing: border-box;
}
.fd-input:focus { border-color: #8b5cf6; }
.fd-input::placeholder { color: #d1d5db; }

.fd-readonly {
  padding: 8px 12px; background: #f9fafb; border: 1px solid #f0f0f3;
  border-radius: 8px; font-size: 13px; color: #6b7280; font-weight: 500;
}

/* Pills */
.fd-pills { display: flex; flex-wrap: wrap; gap: 6px; }
.fd-pill {
  padding: 5px 12px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  font-size: 12px; font-weight: 500; color: #6b7280; background: #fff;
  cursor: pointer; transition: all 0.15s; user-select: none;
}
.fd-pill:hover { border-color: #c4b5fd; }
.fd-pill.on { border-color: #8b5cf6; background: #f5f3ff; color: #6d28d9; font-weight: 600; }

/* Frequency buttons */
.fd-freq { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; }
.fd-freq-btn {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 12px 8px; border: 1.5px solid #e5e7eb; border-radius: 10px;
  background: #fff; color: #6b7280; cursor: pointer; transition: all 0.15s;
  font-size: 12px; font-weight: 600;
}
.fd-freq-btn:hover { border-color: #c4b5fd; }
.fd-freq-btn.on { border-color: #8b5cf6; background: #f5f3ff; color: #6d28d9; }

/* Frequency count */
.fd-freq-count { display: flex; gap: 4px; }
.fd-fc-btn {
  padding: 5px 14px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  font-size: 12px; font-weight: 600; color: #6b7280; background: #fff;
  cursor: pointer; transition: all 0.12s;
}
.fd-fc-btn:hover { border-color: #c4b5fd; }
.fd-fc-btn.on { border-color: #8b5cf6; background: #8b5cf6; color: #fff; }

/* Week day picker */
.fd-week { display: flex; gap: 4px; }
.fd-wday {
  width: 38px; height: 34px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  font-size: 12px; font-weight: 600; color: #6b7280; background: #fff;
  cursor: pointer; transition: all 0.12s;
}
.fd-wday:hover { border-color: #c4b5fd; }
.fd-wday.on { border-color: #8b5cf6; background: #8b5cf6; color: #fff; }

/* Month day grid */
.fd-month { display: grid; grid-template-columns: repeat(7, 1fr); gap: 3px; }
.fd-mday {
  height: 30px; border: 1px solid #e5e7eb; border-radius: 6px;
  font-size: 11px; font-weight: 600; color: #6b7280; background: #fff;
  cursor: pointer; transition: all 0.12s;
}
.fd-mday:hover { border-color: #c4b5fd; }
.fd-mday.on { border-color: #8b5cf6; background: #8b5cf6; color: #fff; }

/* Time slots */
.fd-slots { display: flex; flex-direction: column; gap: 6px; }
.fd-slot { display: flex; align-items: center; gap: 6px; }
.fd-time {
  padding: 6px 10px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  font-size: 13px; color: #1f2937; outline: none; width: 110px;
}
.fd-time:focus { border-color: #8b5cf6; }
.fd-time-sep { color: #d1d5db; font-size: 12px; }
.fd-slot-del {
  width: 24px; height: 24px; border: none; border-radius: 6px;
  background: transparent; color: #d1d5db; cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: all 0.12s;
}
.fd-slot-del:hover { background: #fef2f2; color: #ef4444; }
.fd-slot-add {
  display: inline-flex; align-items: center; gap: 4px; align-self: flex-start;
  padding: 4px 10px; border: 1px dashed #e5e7eb; border-radius: 6px;
  font-size: 11px; color: #9ca3af; background: none; cursor: pointer; transition: all 0.12s;
}
.fd-slot-add:hover { color: #8b5cf6; border-color: #c4b5fd; background: #f5f3ff; }

/* Checkbox */
.fd-check-row {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 12px; color: #374151; cursor: pointer; user-select: none;
}

/* Evaluation method grid */
.fd-method-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
.fd-method-opt {
  padding: 8px 10px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  cursor: pointer; transition: all 0.15s;
}
.fd-method-opt:hover { border-color: #c4b5fd; }
.fd-method-opt.on { border-color: #8b5cf6; background: #f5f3ff; }
.fd-method-label { font-size: 12px; font-weight: 600; color: #1e1b4b; }
.fd-method-desc { font-size: 10px; color: #9ca3af; margin-top: 1px; }

/* Thresholds */
.fd-thresh-list { display: flex; flex-direction: column; gap: 6px; }
.fd-target-hint {
  font-size: 11px; font-weight: 500; color: #8b5cf6;
  background: #f5f3ff; padding: 2px 8px; border-radius: 4px; margin-left: auto;
}
.fd-thresh-row { display: flex; align-items: center; gap: 6px; }
.fd-thresh-grade { font-size: 12px; font-weight: 600; min-width: 50px; }
.fd-thresh-op { font-size: 11px; color: #9ca3af; }
.fd-thresh-input {
  width: 60px; padding: 4px 8px; border: 1px solid #e5e7eb; border-radius: 6px;
  font-size: 12px; text-align: center; outline: none;
}
.fd-thresh-input:focus { border-color: #8b5cf6; }
.fd-thresh-unit { font-size: 11px; color: #9ca3af; }

/* Normalization */
.fd-norm-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
.fd-norm-opt {
  padding: 8px 10px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  cursor: pointer; transition: all 0.15s;
}
.fd-norm-opt:hover { border-color: #c4b5fd; }
.fd-norm-opt.on { border-color: #8b5cf6; background: #f5f3ff; }
/* ── Normalization Cards ── */
.norm-cards { display: grid; grid-template-columns: repeat(2, 1fr); gap: 6px; }
.norm-card {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border: 1.5px solid #e5e7eb; border-radius: 10px;
  cursor: pointer; transition: all 0.15s;
}
.norm-card:hover { border-color: #c4b5fd; }
.norm-card.active { border-color: #8b5cf6; background: #f5f3ff; }
.norm-card.disabled { opacity: 0.35; cursor: not-allowed; }
.norm-card-icon {
  width: 32px; height: 32px; border-radius: 8px;
  background: #f3f4f6; color: #6b7280;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; font-weight: 700; flex-shrink: 0;
}
.norm-card.active .norm-card-icon { background: #ede9fe; color: #7c3aed; }
.norm-card-body { min-width: 0; }
.norm-card-name { font-size: 12px; font-weight: 700; color: #1e1b4b; }
.norm-card.active .norm-card-name { color: #6d28d9; }
.norm-card-desc { font-size: 10px; color: #9ca3af; margin-top: 1px; }

/* ── Normalization Config Box ── */
.norm-config-box {
  margin-top: 8px; padding: 14px; background: #fafbff;
  border: 1px solid #ede9fe; border-radius: 10px;
}
.norm-formula-visual {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  padding: 8px 0;
}
.norm-fv-item {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  padding: 6px 12px; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px;
  min-width: 60px;
}
.norm-fv-item.editable { background: #f5f3ff; border-color: #c4b5fd; }
.norm-fv-label { font-size: 9px; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.5px; }
.norm-fv-example { font-size: 18px; font-weight: 700; color: #1e1b4b; }
.norm-fv-example.result { color: #7c3aed; }
.norm-fv-op { font-size: 18px; font-weight: 300; color: #9ca3af; }
.norm-fv-select {
  font-size: 11px; font-weight: 600; color: #6d28d9; background: transparent;
  border: none; outline: none; cursor: pointer; text-align: center;
  padding: 2px 0;
}
.norm-fv-relation {
  font-size: 10px; color: #9ca3af; background: transparent;
  border: none; border-bottom: 1px dashed #c4b5fd; outline: none;
  text-align: center; width: 80px; padding: 2px 0;
}
.norm-fv-relation::placeholder { color: #d1d5db; }
.norm-fv-fixed {
  font-size: 18px; font-weight: 700; color: #6d28d9; background: transparent;
  border: none; border-bottom: 2px solid #c4b5fd; outline: none;
  text-align: center; width: 50px;
}
.norm-fv-fixed::placeholder { color: #d1d5db; font-weight: 400; }
.norm-filter-row {
  display: flex; align-items: center; gap: 8px; margin-top: 10px;
  padding-top: 10px; border-top: 1px dashed #ede9fe;
}
.norm-filter-label { font-size: 11px; font-weight: 600; color: #6b7280; white-space: nowrap; }
.norm-filter-select {
  padding: 5px 8px; border: 1.5px solid #e5e7eb; border-radius: 6px;
  font-size: 12px; color: #1f2937; outline: none; min-width: 120px;
}
.norm-filter-select:focus { border-color: #8b5cf6; }
.norm-filter-hint { font-size: 10px; color: #9ca3af; }

.norm-example-hint {
  font-size: 10px; color: #8b5cf6; background: #ede9fe;
  padding: 6px 10px; border-radius: 6px; margin-top: 8px;
  line-height: 1.5;
}
.fd-norm-title { font-size: 12px; font-weight: 600; color: #1e1b4b; }
.fd-norm-desc { font-size: 10px; color: #9ca3af; margin-top: 1px; }

.fd-norm-detail {
  padding: 10px 12px; background: #fafbff; border-radius: 8px;
}
.fd-norm-row {
  display: flex; align-items: center; gap: 8px; margin-bottom: 6px;
}
.fd-norm-row label { font-size: 12px; color: #6b7280; min-width: 56px; }
.fd-norm-formula {
  font-size: 11px; color: #8b5cf6; background: #f3f0ff;
  padding: 6px 10px; border-radius: 6px; margin-top: 4px;
}

/* Missing policy grid */
.fd-missing-grid { display: flex; gap: 6px; flex-wrap: wrap; }
.fd-missing-opt {
  padding: 6px 14px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  font-size: 12px; font-weight: 500; color: #6b7280; background: #fff;
  cursor: pointer; transition: all 0.15s; user-select: none;
}
.fd-missing-opt:hover { border-color: #c4b5fd; }
.fd-missing-opt.on { border-color: #8b5cf6; background: #f5f3ff; color: #6d28d9; font-weight: 600; }

/* Footer */
.fd-footer { display: flex; justify-content: flex-end; gap: 8px; }
.fd-btn {
  padding: 8px 20px; border-radius: 8px; font-size: 13px; font-weight: 600;
  border: none; cursor: pointer; transition: all 0.15s;
}
.fd-btn.ghost { background: #f3f4f6; color: #6b7280; }
.fd-btn.ghost:hover { background: #e5e7eb; }
.fd-btn.primary { background: #8b5cf6; color: #fff; }
.fd-btn.primary:hover { background: #7c3aed; }
.fd-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
