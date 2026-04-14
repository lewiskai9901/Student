<script setup lang="ts">
/**
 * TaskExecutionView — V7 打分界面（重写版）
 *
 * 布局：
 *   顶部导航栏：返回 + 任务信息 + 任务操作
 *   主体两栏：
 *     左：目标导航（target nav）+ 完成进度指示
 *     右：顶部目标选择器 + 字段列表（按 scoringMode 渲染不同控件）+ 底部实时得分
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Play, Send, RotateCcw,
  ChevronLeft, ChevronRight,
  Star, Search, Target,
} from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/insp/inspExecutionStore'
import {
  TaskStatusConfig, ScoringModeConfig,
  type TaskStatus, type ScoringMode,
} from '@/types/insp/enums'
import type { InspTask, InspSubmission, SubmissionDetail } from '@/types/insp/project'
import type { TemplateSection } from '@/types/insp/template'
import { getProject } from '@/api/insp/project'
import { http } from '@/utils/request'
import { inspTemplateApi } from '@/api/insp/template'
import { createDetail as createDetailApi } from '@/api/insp/submission'
import { orgUnitApi } from '@/api/organization'
import { buildSectionTree, flattenTree, type SectionTreeNode } from '@/utils/sectionTree'
import ViolationRecordInput from './components/ViolationRecordInput.vue'
import PersonScoreGrid from './components/PersonScoreGrid.vue'
import EventStreamRecorder from './components/EventStreamRecorder.vue'

const route = useRoute()
const router = useRouter()
const store = useInspExecutionStore()
const taskId = Number(route.params.id)

// ==================== Core State ====================

const loading = ref(false)
const detailLoading = ref(false)
const task = ref<InspTask | null>(null)
const submissions = ref<InspSubmission[]>([])
const details = ref<SubmissionDetail[]>([])
const allSections = ref<TemplateSection[]>([])
const rootSectionId = ref<number | null>(null)
const sectionTree = ref<SectionTreeNode[]>([])

// Per-detail input state (keyed by detail.id)
const numberInputs = ref<Record<number, number>>({})
const selectInputs = ref<Record<number, string>>({})
const textInputs = ref<Record<number, string>>({})

const isEditable = computed(() => {
  // In target mode, editable if ANY non-intermediate submission for the target is IN_PROGRESS
  return targetSectionGroups.value.some(g => !g.isIntermediate && g.submission.status === 'IN_PROGRESS')
})

// ==================== Target Mode ====================

const selectedTargetId = ref<number | null>(null)
const targetSearch = ref('')
const targetFilter = ref<'all' | 'pending' | 'completed'>('all')
const targetContextFilter = ref<string>('')

// ---- Target filter context (org tree / place types) ----
interface TargetMeta { targetId: number; targetName: string; targetType: string; parentId?: number; parentName?: string }
const targetMetaMap = ref<Map<number, TargetMeta>>(new Map())
const filterDimensions = ref<{ label: string; key: string; options: { value: string; label: string }[] }[]>([])

/** Load filter context based on target type */
async function loadTargetFilterContext() {
  const types = new Set(submissions.value.map(s => s.targetType))
  const dominantType = types.has('ORG') ? 'ORG' : types.has('PLACE') ? 'PLACE' : types.has('USER') ? 'USER' : ''

  if (dominantType === 'ORG') {
    try {
      const tree = await orgUnitApi.getTree()
      const parentMap = new Map<number, { id: number; name: string; type: string }>()
      const nameMap = new Map<number, string>()
      function walk(nodes: any[], parent?: any) {
        for (const n of nodes || []) {
          nameMap.set(Number(n.id), n.unitName || n.name || '')
          if (parent) parentMap.set(Number(n.id), { id: Number(parent.id), name: parent.unitName || parent.name, type: parent.unitType })
          walk(n.children || [], n)
        }
      }
      walk(Array.isArray(tree) ? tree : (tree as any)?.data || [])
      // Build meta with parent info, resolve empty targetName from org tree
      const newMap = new Map(targetMetaMap.value)
      for (const s of submissions.value) {
        const tid = Number(s.targetId)
        const p = parentMap.get(tid)
        const resolvedName = s.targetName || nameMap.get(tid) || `目标 #${s.targetId}`
        newMap.set(s.targetId, {
          targetId: s.targetId, targetName: resolvedName, targetType: s.targetType,
          parentId: p?.id, parentName: p?.name,
        })
      }
      targetMetaMap.value = newMap
      // Build dimension: filter by parent org
      const groups = new Map<string, string>()
      for (const m of newMap.values()) {
        if (m.parentName) groups.set(String(m.parentId), m.parentName)
      }
      if (groups.size > 1) {
        filterDimensions.value = [{ label: '上级组织', key: 'parentId', options: Array.from(groups, ([v, l]) => ({ value: v, label: l })) }]
      }
    } catch { /* ignore */ }
  } else if (dominantType === 'PLACE') {
    const groups = new Set<string>()
    const newMap = new Map(targetMetaMap.value)
    for (const s of submissions.value) {
      const m = s.targetName.match(/^(.+?)[第\d]/)
      if (m) groups.add(m[1])
      newMap.set(s.targetId, { targetId: s.targetId, targetName: s.targetName || `目标 #${s.targetId}`, targetType: s.targetType, parentName: m?.[1] })
    }
    targetMetaMap.value = newMap
    if (groups.size > 1) {
      filterDimensions.value = [{ label: '场所类型', key: 'parentName', options: Array.from(groups).sort().map(g => ({ value: g, label: g })) }]
    }
  } else if (dominantType === 'USER') {
    const depts = new Set<string>()
    const newMap = new Map(targetMetaMap.value)
    for (const s of submissions.value) {
      const dept = (s as any).targetOrgName || ''
      if (dept) depts.add(dept)
      newMap.set(s.targetId, { targetId: s.targetId, targetName: s.targetName || `目标 #${s.targetId}`, targetType: s.targetType, parentName: dept })
    }
    targetMetaMap.value = newMap
    if (depts.size > 1) {
      filterDimensions.value = [{ label: '所属部门', key: 'parentName', options: Array.from(depts).sort().map(d => ({ value: d, label: d })) }]
    }
  }
}

/** Unique targets deduped from submissions */
const uniqueTargets = computed(() => {
  const map = new Map<number, TargetMeta>()
  for (const s of submissions.value) {
    if (!map.has(s.targetId)) {
      const meta = targetMetaMap.value.get(s.targetId)
      // Use meta name (resolved from org tree) > submission name > fallback
      const name = meta?.targetName || s.targetName || `目标 #${s.targetId}`
      map.set(s.targetId, meta ? { ...meta, targetName: name } : { targetId: s.targetId, targetName: name, targetType: s.targetType })
    }
  }
  return Array.from(map.values())
})

/** Context filter label */
const contextFilterLabel = computed(() => filterDimensions.value[0]?.label ?? '分组')
/** Context filter options */
const contextFilterOptions = computed(() => filterDimensions.value[0]?.options ?? [])

function getTargetStatus(targetId: number): string {
  const subs = submissions.value.filter(s => s.targetId === targetId)
  if (subs.length === 0) return 'PENDING'
  if (subs.every(s => s.status === 'COMPLETED' || s.status === 'SKIPPED')) return 'COMPLETED'
  if (subs.some(s => s.status === 'IN_PROGRESS')) return 'IN_PROGRESS'
  return 'PENDING'
}

function getTargetProgress(targetId: number): { done: number; total: number } {
  const subs = submissions.value.filter(s => s.targetId === targetId)
  const done = subs.filter(s => s.status === 'COMPLETED' || s.status === 'SKIPPED').length
  return { done, total: subs.length }
}

const pendingCount = computed(() =>
  uniqueTargets.value.filter(t => getTargetStatus(t.targetId) !== 'COMPLETED').length
)
const completedTargetCount = computed(() =>
  uniqueTargets.value.filter(t => getTargetStatus(t.targetId) === 'COMPLETED').length
)

function getTargetDotClass(targetId: number): string {
  const s = getTargetStatus(targetId)
  return s === 'COMPLETED' ? 'done' : s === 'IN_PROGRESS' ? 'progress' : 'pending'
}

const filteredTargets = computed(() => {
  let list = uniqueTargets.value
  // Context filter (by parent id or parent name)
  if (targetContextFilter.value) {
    const dim = filterDimensions.value[0]
    if (dim?.key === 'parentId') {
      list = list.filter(t => String(t.parentId) === targetContextFilter.value)
    } else {
      list = list.filter(t => t.parentName === targetContextFilter.value)
    }
  }
  if (targetSearch.value) {
    const q = targetSearch.value.toLowerCase()
    list = list.filter(t => t.targetName.toLowerCase().includes(q))
  }
  if (targetFilter.value === 'pending') {
    list = list.filter(t => getTargetStatus(t.targetId) !== 'COMPLETED')
  } else if (targetFilter.value === 'completed') {
    list = list.filter(t => getTargetStatus(t.targetId) === 'COMPLETED')
  }
  return list
})

interface TargetSectionGroup {
  section: TemplateSection | null
  submission: InspSubmission
  details: SubmissionDetail[]
  collapsed: boolean
  depth: number           // tree depth for indentation
  isIntermediate: boolean // true = has child sections, renders as group header
}
const targetSectionGroups = ref<TargetSectionGroup[]>([])

/** Current target index in filtered list */
const currentTargetIdx = computed(() => {
  if (!selectedTargetId.value) return -1
  return filteredTargets.value.findIndex(t => t.targetId === selectedTargetId.value)
})
const hasNextTarget = computed(() => currentTargetIdx.value >= 0 && currentTargetIdx.value < filteredTargets.value.length - 1)
const hasPrevTarget = computed(() => currentTargetIdx.value > 0)

function goToNextTarget() {
  if (hasNextTarget.value) selectTarget(filteredTargets.value[currentTargetIdx.value + 1].targetId)
}
function goToPrevTarget() {
  if (hasPrevTarget.value) selectTarget(filteredTargets.value[currentTargetIdx.value - 1].targetId)
}

const currentTargetName = computed(() => {
  const t = uniqueTargets.value.find(t => t.targetId === selectedTargetId.value)
  return t?.targetName ?? ''
})

const targetItemProgress = computed(() => {
  let total = 0, done = 0
  for (const g of targetSectionGroups.value) {
    if (g.isIntermediate) continue
    total += g.details.length
    done += g.details.filter(d =>
      numberInputs.value[d.id] !== undefined ||
      selectInputs.value[d.id] !== undefined ||
      textInputs.value[d.id] !== undefined
    ).length
  }
  return { done, total }
})

const targetProgressPercent = computed(() => {
  const { done, total } = targetItemProgress.value
  return total > 0 ? Math.round((done / total) * 100) : 0
})

const completedSectionCount = computed(() =>
  targetSectionGroups.value.filter(g => !g.isIntermediate && g.submission.status === 'COMPLETED').length
)

const leafSectionCount = computed(() =>
  targetSectionGroups.value.filter(g => !g.isIntermediate).length
)

const targetScore = computed(() => {
  let total = 0
  for (const g of targetSectionGroups.value) {
    if (g.isIntermediate) continue
    for (const d of g.details) {
      const val = numberInputs.value[d.id]
      if (val !== undefined) total += val
    }
  }
  return total
})

function isDetailScored(detail: SubmissionDetail): boolean {
  return numberInputs.value[detail.id] !== undefined ||
    selectInputs.value[detail.id] !== undefined ||
    textInputs.value[detail.id] !== undefined
}

function scoredCountInGroup(group: TargetSectionGroup): number {
  return group.details.filter(d => isDetailScored(d)).length
}

function groupProgressPercent(group: TargetSectionGroup): number {
  const total = group.details.length
  return total > 0 ? Math.round((scoredCountInGroup(group) / total) * 100) : 0
}

/** Count child leaf sections under an intermediate group */
function intermediateChildCount(group: TargetSectionGroup): number {
  if (!group.section || !sectionTree.value.length) return 0
  const node = flattenTree(sectionTree.value).find(n => n.id === Number(group.section!.id))
  if (!node) return 0
  return flattenTree(node.children).filter(c => c.isLeaf).length
}

/** Check if group is hidden because any ancestor intermediate section is collapsed */
function isGroupHiddenByParent(gi: number): boolean {
  const group = targetSectionGroups.value[gi]
  if (!group || group.depth === 0) return false
  // Walk backwards and check ALL ancestor intermediate sections
  for (let i = gi - 1; i >= 0; i--) {
    const prev = targetSectionGroups.value[i]
    if (prev.isIntermediate && prev.depth < group.depth) {
      if (prev.collapsed) return true
    }
    // Stop if we've gone past all possible parents
    if (prev.depth === 0 && prev.isIntermediate) break
  }
  return false
}

async function selectTarget(targetId: number) {
  selectedTargetId.value = targetId
  details.value = []
  numberInputs.value = {}
  selectInputs.value = {}
  textInputs.value = {}

  detailLoading.value = true
  try {
    // Auto-transition PENDING submissions for this target
    const subs = submissions.value.filter(s => s.targetId === targetId)
    for (const sub of subs) {
      if (sub.status === 'PENDING' || sub.status === 'LOCKED') {
        try {
          const updated = await store.startFillingSubmission(sub.id)
          const idx = submissions.value.findIndex(s => s.id === sub.id)
          if (idx >= 0) submissions.value[idx] = updated
        } catch { /* non-fatal */ }
      }
    }
    // Load details for all submissions of this target
    const rawGroups: TargetSectionGroup[] = []
    const updatedSubs = submissions.value.filter(s => s.targetId === targetId)
    for (const sub of updatedSubs) {
      let section = allSections.value.find(s => Number(s.id) === Number(sub.sectionId)) || null
      // Fallback: if sectionId is null/missing, use root section
      if (!section && !sub.sectionId && allSections.value.length > 0 && rootSectionId.value) {
        section = allSections.value.find(s => Number(s.id) === rootSectionId.value) || allSections.value[0]
      }
      let dets: SubmissionDetail[] = []
      try { dets = await store.loadDetails(sub.id) } catch { /* skip */ }

      // Build groups per leaf section from template tree
      if (sectionTree.value.length > 0) {
        const leafNodes = flattenTree(sectionTree.value).filter(n => n.isLeaf)
        // Index existing details by sectionId
        const detsBySectionId = new Map<number, SubmissionDetail[]>()
        for (const d of dets) {
          const sid = Number(d.sectionId || 0)
          if (!detsBySectionId.has(sid)) detsBySectionId.set(sid, [])
          detsBySectionId.get(sid)!.push(d)
        }
        // For each leaf section, use saved details or load template items as placeholders
        for (const leaf of leafNodes) {
          const savedDets = detsBySectionId.get(leaf.id) || []
          let leafDets: SubmissionDetail[] = savedDets
          // If no saved details for this section, load from template
          if (savedDets.length === 0) {
            try {
              const items = await inspTemplateApi.getItems(leaf.id)
              leafDets = items.map(item => ({
                id: -(leaf.id * 10000 + Number(item.id || 0)),
                submissionId: sub.id,
                templateItemId: Number(item.id),
                itemCode: item.itemCode || '',
                itemName: item.itemName || '',
                itemType: item.itemType || 'NUMBER',
                sectionId: leaf.id,
                sectionName: leaf.sectionName || '',
                scoringMode: 'DIRECT' as any,
                scoringConfig: item.scoringConfig || '',
                responseValue: '',
                score: null as any,
                maxScore: 100,
              } as unknown as SubmissionDetail))
            } catch { /* skip */ }
          }
          if (leafDets.length === 0) continue
          const leafSection = allSections.value.find(s => Number(s.id) === leaf.id) || { id: leaf.id, sectionName: leaf.sectionName } as any
          rawGroups.push({ section: leafSection, submission: sub, details: leafDets, collapsed: false, depth: 0, isIntermediate: false })
        }
        // Also include any details not matched to a leaf section (orphaned)
        const leafIds = new Set(leafNodes.map(n => n.id))
        const orphaned = dets.filter(d => !leafIds.has(Number(d.sectionId || 0)))
        if (orphaned.length > 0) {
          rawGroups.push({ section, submission: sub, details: orphaned, collapsed: false, depth: 0, isIntermediate: false })
        }
      } else {
        rawGroups.push({ section, submission: sub, details: dets, collapsed: false, depth: 0, isIntermediate: false })
      }
    }

    // If we have a section tree, annotate with depth and insert intermediate headers
    if (sectionTree.value.length > 0) {
      const allFlat = flattenTree(sectionTree.value)
      const sectionIdToNode = new Map<number, SectionTreeNode>()
      for (const n of allFlat) sectionIdToNode.set(n.id, n)

      // Compute depth for each section from tree
      const depthMap = new Map<number, number>()
      const walkDepth = (nodes: SectionTreeNode[], d: number) => {
        for (const n of nodes) { depthMap.set(n.id, d); walkDepth(n.children, d + 1) }
      }
      walkDepth(sectionTree.value, 0)

      // Build ordered groups following tree order, inserting intermediate headers
      const orderedGroups: TargetSectionGroup[] = []
      const insertedIntermediates = new Set<number>()
      const groupBySectionId = new Map<number, TargetSectionGroup>()
      for (const g of rawGroups) {
        if (g.section) groupBySectionId.set(Number(g.section.id), g)
      }

      const walkTree = (nodes: SectionTreeNode[]) => {
        for (const node of nodes) {
          const depth = depthMap.get(node.id) ?? 0
          if (!node.isLeaf) {
            // Insert intermediate group header (no submission/details)
            if (!insertedIntermediates.has(node.id)) {
              insertedIntermediates.add(node.id)
              // Check if any child leaf has a submission for this target
              const childLeafIds = flattenTree(node.children).filter(c => c.isLeaf).map(c => c.id)
              const hasChildSubmissions = childLeafIds.some(id => groupBySectionId.has(id))
              if (hasChildSubmissions) {
                orderedGroups.push({
                  section: allSections.value.find(s => Number(s.id) === node.id) || { id: node.id, sectionName: node.sectionName } as any,
                  submission: {} as InspSubmission, // placeholder
                  details: [],
                  collapsed: false,
                  depth,
                  isIntermediate: true,
                })
              }
            }
            walkTree(node.children)
          } else {
            const existing = groupBySectionId.get(node.id)
            if (existing) {
              existing.depth = depth
              existing.isIntermediate = false
              orderedGroups.push(existing)
            }
          }
        }
      }
      walkTree(sectionTree.value)

      // Append any groups not in tree (safety fallback)
      for (const g of rawGroups) {
        if (!orderedGroups.includes(g)) {
          orderedGroups.push(g)
        }
      }
      targetSectionGroups.value = orderedGroups
    } else {
      targetSectionGroups.value = rawGroups
    }
    // Init inputs for all details across all groups
    const allDets = targetSectionGroups.value.flatMap(g => g.details)
    initInputs(allDets)
  } finally {
    detailLoading.value = false
  }
}

/** Target-mode: check if a specific group's submission is editable */
function isGroupEditable(group: TargetSectionGroup): boolean {
  return group.submission.status === 'IN_PROGRESS'
}

/**
 * Ensure a detail record is persisted (saved to backend).
 * If detail.id < 0, it's a placeholder from template items - create it first.
 * Returns the real (persisted) detail with a valid positive ID.
 */
async function ensureDetailPersisted(detail: SubmissionDetail): Promise<SubmissionDetail> {
  if (detail.id > 0) return detail // Already persisted

  // Create detail via API
  const created = await createDetailApi(detail.submissionId, {
    templateItemId: detail.templateItemId,
    itemCode: detail.itemCode,
    itemName: detail.itemName,
    itemType: detail.itemType,
    sectionId: detail.sectionId,
    sectionName: detail.sectionName,
    scoringMode: detail.scoringMode || 'DIRECT',
  } as any)

  // Replace placeholder in all groups
  for (const g of targetSectionGroups.value) {
    const idx = g.details.findIndex(d => d.id === detail.id)
    if (idx >= 0) {
      g.details[idx] = { ...g.details[idx], ...created, id: created.id }
      // Update input maps with new ID
      if (numberInputs.value[detail.id] !== undefined) {
        numberInputs.value[created.id] = numberInputs.value[detail.id]
        delete numberInputs.value[detail.id]
      }
      if (selectInputs.value[detail.id] !== undefined) {
        selectInputs.value[created.id] = selectInputs.value[detail.id]
        delete selectInputs.value[detail.id]
      }
      if (textInputs.value[detail.id] !== undefined) {
        textInputs.value[created.id] = textInputs.value[detail.id]
        delete textInputs.value[detail.id]
      }
      break
    }
  }
  return created
}

/** Get all submissions for a given section */
function getSubmissionsForSection(sectionId: number): InspSubmission[] {
  return submissions.value.filter(s => String(s.sectionId) === String(sectionId))
}

// ==================== Scoring State & Helpers ====================

function parseScoringConfig(detail: SubmissionDetail): any {
  if (!detail.scoringConfig) return {}
  try { return JSON.parse(detail.scoringConfig) } catch { return {} }
}

function resolveMode(detail: SubmissionDetail): ScoringMode | null {
  return detail.scoringMode || null
}

function isNonScoring(detail: SubmissionDetail): boolean {
  return !detail.scoringMode || detail.itemType === 'PERSON_SCORE' || detail.itemType === 'VIOLATION_RECORD'
}

/** Real-time score summary for selected submission */
const scoreSummary = computed(() => {
  const scoreable = details.value.filter(d => d.scoringMode && d.itemType !== 'PERSON_SCORE' && d.itemType !== 'VIOLATION_RECORD')
  let deductions = 0
  let bonuses = 0
  let directTotal = 0
  let passCount = 0
  let failCount = 0

  for (const d of scoreable) {
    const cfg = parseScoringConfig(d)
    const mode = d.scoringMode!

    if (mode === 'DEDUCTION') {
      const inputVal = numberInputs.value[d.id]
      if (inputVal !== undefined) {
        deductions += Math.abs(inputVal) // inputVal 是负数如 -3，取绝对值
      }
    } else if (mode === 'PASS_FAIL') {
      const sel = selectInputs.value[d.id]
      if (sel === 'PASS') {
        passCount++
        // passScore 通常是 0（不加分不扣分）
      } else if (sel === 'FAIL') {
        failCount++
        deductions += Math.abs(cfg.failScore ?? 5) // failScore 是负数如 -5
      }
    } else if (mode === 'ADDITION') {
      const inputVal = numberInputs.value[d.id]
      if (inputVal !== undefined && inputVal > 0) {
        bonuses += inputVal
      }
    } else if (mode === 'DIRECT') {
      const inputVal = numberInputs.value[d.id]
      if (inputVal !== undefined) {
        directTotal += inputVal
      }
    } else {
      const inputVal = numberInputs.value[d.id]
      if (inputVal !== undefined) {
        directTotal += inputVal
      }
    }
  }

  // 已评分数：PASS_FAIL 通过 selectInputs 计数，其他通过 numberInputs
  const passFailItems = scoreable.filter(d => d.scoringMode === 'PASS_FAIL')
  const passFailScored = passFailItems.filter(d => selectInputs.value[d.id] !== undefined).length
  const otherItems = scoreable.filter(d => d.scoringMode !== 'PASS_FAIL')
  const otherScored = otherItems.filter(d => numberInputs.value[d.id] !== undefined).length
  const realScored = passFailScored + otherScored

  // 总分 = 直接打分 + 加分 - 扣分
  const finalScore = directTotal + bonuses - deductions

  return {
    total: scoreable.length,
    scored: realScored,
    finalScore,
    deductions,
    bonuses,
    passCount,
    failCount,
  }
})

/**
 * Estimate-only grade calculation with hardcoded thresholds.
 * Used only for real-time preview during IN_PROGRESS status.
 * The actual grade is calculated by the backend scoring engine
 * (ScoringProfile + dimensions + grade bands) on submission complete.
 */
function getGrade(score: number): string {
  if (score >= 95) return 'A+'
  if (score >= 90) return 'A'
  if (score >= 80) return 'B'
  if (score >= 70) return 'C'
  if (score >= 60) return 'D'
  return 'F'
}

function getGradeColor(grade: string): string {
  const c: Record<string, string> = {
    'A+': '#10b981', A: '#10b981', B: '#1a6dff', C: '#f59e0b', D: '#ef4444', F: '#dc2626',
  }
  return c[grade] || '#6b7280'
}

// ==================== Deduction button groups ====================

/** Deduction range for el-input-number */
function getDeductionRange(detail: SubmissionDetail): { min: number; step: number } {
  const cfg = parseScoringConfig(detail)
  const max = cfg.maxDeduction ?? cfg.maxScore ?? 5
  return { min: -max, step: cfg.step ?? cfg.deductPerViolation ?? 1 }
}

/** Addition range for el-input-number */
function getAdditionRange(detail: SubmissionDetail): { max: number; step: number } {
  const cfg = parseScoringConfig(detail)
  const max = cfg.maxAddition ?? cfg.maxBonus ?? cfg.maxScore ?? 5
  return { max, step: cfg.step ?? 1 }
}

/** Grade levels config */
function getGradeLevels(detail: SubmissionDetail): Array<{ label: string; score: number }> {
  const cfg = parseScoringConfig(detail)
  if (cfg.levels && Array.isArray(cfg.levels)) return cfg.levels
  return [
    { label: '优', score: 100 },
    { label: '良', score: 80 },
    { label: '中', score: 60 },
    { label: '差', score: 40 },
  ]
}

/** Direct score range */
function getDirectRange(detail: SubmissionDetail): { min: number; max: number } {
  const cfg = parseScoringConfig(detail)
  return { min: cfg.minScore ?? 0, max: cfg.maxScore ?? 100 }
}

/** Rating scale max stars */
function getRatingMax(detail: SubmissionDetail): number {
  const cfg = parseScoringConfig(detail)
  return cfg.maxStars ?? cfg.maxRating ?? 5
}

/** Cumulative config */
function getCumulativeConfig(detail: SubmissionDetail): { countLabel: string; scorePerUnit: number } {
  const cfg = parseScoringConfig(detail)
  return { countLabel: cfg.countLabel ?? '次', scorePerUnit: cfg.scorePerUnit ?? 1 }
}

// ==================== Input handlers ====================

async function handleDeductionSelect(detail: SubmissionDetail, val: number) {
  numberInputs.value[detail.id] = val
  try {
    const real = await ensureDetailPersisted(detail)
    await store.updateDetailResponse(real.id, {
      responseValue: String(val),
      scoringMode: 'DEDUCTION',
      score: val,
    })
  } catch (e: any) { console.error('扣分保存失败', e); ElMessage.error('扣分保存失败，请重试') }
}

async function handlePassFail(detail: SubmissionDetail, val: 'PASS' | 'FAIL') {
  const isDeselect = selectInputs.value[detail.id] === val
  if (isDeselect) {
    delete selectInputs.value[detail.id]
    try {
      const real = await ensureDetailPersisted(detail)
      await store.updateDetailResponse(real.id, { responseValue: '', scoringMode: 'PASS_FAIL', score: 0 })
    } catch (e: any) { console.error('取消评判失败', e); ElMessage.error('取消评判失败，请重试') }
    return
  }
  selectInputs.value[detail.id] = val
  const cfg = parseScoringConfig(detail)
  const score = val === 'PASS' ? (cfg.passScore ?? 0) : (cfg.failScore ?? -5)
  try {
    const real = await ensureDetailPersisted(detail)
    await store.updateDetailResponse(real.id, {
      responseValue: val,
      scoringMode: 'PASS_FAIL',
      score,
    })
  } catch (e: any) { console.error('评判保存失败', e); ElMessage.error('评判保存失败，请重试') }
}

async function handleGradeSelect(detail: SubmissionDetail, label: string, score: number) {
  const isDeselect = selectInputs.value[detail.id] === label
  if (isDeselect) {
    delete selectInputs.value[detail.id]
    delete numberInputs.value[detail.id]
    try {
      const real = await ensureDetailPersisted(detail)
      await store.updateDetailResponse(real.id, { responseValue: '', scoringMode: detail.scoringMode!, score: 0 })
    } catch (e: any) { console.error('取消等级失败', e); ElMessage.error('取消等级失败，请重试') }
    return
  }
  selectInputs.value[detail.id] = label
  numberInputs.value[detail.id] = score
  try {
    const real = await ensureDetailPersisted(detail)
    await store.updateDetailResponse(real.id, {
      responseValue: label,
      scoringMode: detail.scoringMode!,
      score,
    })
  } catch (e: any) { console.error('等级保存失败', e); ElMessage.error('等级保存失败，请重试') }
}

async function handleAdditionSelect(detail: SubmissionDetail, val: number) {
  numberInputs.value[detail.id] = val
  try {
    const real = await ensureDetailPersisted(detail)
    await store.updateDetailResponse(real.id, {
      responseValue: String(val),
      scoringMode: 'ADDITION',
      score: val,
    })
  } catch (e: any) { console.error('加分保存失败', e); ElMessage.error('加分保存失败，请重试') }
}

async function handleDirectInput(detail: SubmissionDetail, val: number) {
  numberInputs.value[detail.id] = val
  try {
    const real = await ensureDetailPersisted(detail)
    await store.updateDetailResponse(real.id, {
      responseValue: String(val),
      scoringMode: 'DIRECT',
      score: val,
    })
  } catch (e: any) { console.error('分数保存失败', e); ElMessage.error('分数保存失败，请重试') }
}

async function handleRatingScale(detail: SubmissionDetail, stars: number) {
  const isDeselect = numberInputs.value[detail.id] === stars
  if (isDeselect) {
    delete numberInputs.value[detail.id]
    try {
      const real = await ensureDetailPersisted(detail)
      await store.updateDetailResponse(real.id, { responseValue: '', scoringMode: 'RATING_SCALE', score: 0 })
    } catch (e: any) { console.error('取消评分失败', e); ElMessage.error('取消评分失败，请重试') }
    return
  }
  numberInputs.value[detail.id] = stars
  const cfg = parseScoringConfig(detail)
  const maxScore = cfg.maxScore ?? 100
  const maxStars = getRatingMax(detail)
  const score = Math.round((stars / maxStars) * maxScore)
  try {
    const real = await ensureDetailPersisted(detail)
    await store.updateDetailResponse(real.id, {
      responseValue: String(stars),
      scoringMode: 'RATING_SCALE',
      score,
    })
  } catch (e: any) { console.error('评分保存失败', e); ElMessage.error('评分保存失败，请重试') }
}

function handleDeductionStep(detail: SubmissionDetail, direction: number) {
  const range = getDeductionRange(detail)
  const current = numberInputs.value[detail.id] ?? 0
  const next = Math.max(range.min, Math.min(0, current + direction * range.step))
  if (next !== current) handleDeductionSelect(detail, next)
}

function handleAdditionStep(detail: SubmissionDetail, direction: number) {
  const range = getAdditionRange(detail)
  const current = numberInputs.value[detail.id] ?? 0
  const next = Math.max(0, Math.min(range.max, current + direction * range.step))
  if (next !== current) handleAdditionSelect(detail, next)
}

function handleCumulativeChange(detail: SubmissionDetail, delta: number) {
  const cur = numberInputs.value[detail.id] ?? 0
  const next = Math.max(0, cur + delta)
  numberInputs.value[detail.id] = next
  const cfg = getCumulativeConfig(detail)
  const score = next * cfg.scorePerUnit
  ensureDetailPersisted(detail).then(real =>
    store.updateDetailResponse(real.id, {
      responseValue: String(next),
      scoringMode: 'CUMULATIVE',
      score,
    })
  ).catch((e: any) => { console.error('计数保存失败', e); ElMessage.error('计数保存失败，请重试') })
}

async function handleTextInput(detail: SubmissionDetail, val: string) {
  textInputs.value[detail.id] = val
}

async function saveTextInput(detail: SubmissionDetail) {
  const val = textInputs.value[detail.id] ?? ''
  try {
    const real = await ensureDetailPersisted(detail)
    await store.updateDetailResponse(real.id, {
      responseValue: val,
      score: undefined,
    })
  } catch (e: any) { console.error('文本保存失败', e); ElMessage.error('文本保存失败，请重试') }
}

// ==================== Init inputs from existing response ====================

function initInputs(list: SubmissionDetail[]) {
  for (const d of list) {
    if (!d.responseValue) continue
    const mode = d.scoringMode
    if (mode === 'PASS_FAIL') {
      selectInputs.value[d.id] = d.responseValue
    } else if (mode === 'LEVEL' || mode === 'SCORE_TABLE' || mode === 'TIERED_DEDUCTION') {
      selectInputs.value[d.id] = d.responseValue
      if (d.score != null) numberInputs.value[d.id] = d.score
    } else if (mode === 'DEDUCTION' || mode === 'ADDITION' || mode === 'DIRECT'
              || mode === 'CUMULATIVE' || mode === 'RATING_SCALE') {
      const n = parseFloat(d.responseValue)
      if (!isNaN(n)) numberInputs.value[d.id] = n
    } else if (!mode) {
      textInputs.value[d.id] = d.responseValue
    }
  }
}

// ==================== Data Loading ====================

async function loadData() {
  loading.value = true
  try {
    task.value = await store.loadTask(taskId)
    submissions.value = await store.loadSubmissions(taskId)

    // 如果 submissions 为空，尝试重新填充
    if (task.value && submissions.value.length === 0 && task.value.totalTargets === 0) {
      try {
        await http.post(`/v7/insp/tasks/${taskId}/repopulate`)
        task.value = await store.loadTask(taskId)
        submissions.value = await store.loadSubmissions(taskId)
      } catch (e: any) {
        console.warn('重新填充 submissions 失败', e)
      }
    }

    if (task.value) {
      try {
        const project = await getProject(task.value.projectId)
        // 多模板项目：从计划获取 rootSectionId，回退到项目的
        let rsi = project.rootSectionId
        if (!rsi) {
          try {
            const plans = await http.get<any[]>('/v7/insp/plans', { params: { projectId: task.value.projectId } })
            const plan = task.value.inspectionPlanId
              ? plans?.find((p: any) => Number(p.id) === Number(task.value!.inspectionPlanId)) || plans?.[0]
              : plans?.[0]
            if (plan?.rootSectionId) rsi = plan.rootSectionId
          } catch (e: any) { console.warn('加载检查计划失败，尝试其他方式', e) }
        }
        // 再回退：从 submissions 的 sectionId 找根分区
        if (!rsi && submissions.value.length > 0) {
          const secId = submissions.value[0].sectionId
          if (secId != null) {
            try {
              const sec = await inspTemplateApi.getSection(secId)
              rsi = sec.parentSectionId || secId
            } catch (e: any) { console.warn('从提交记录推断分区失败', e) }
          }
        }
        if (rsi) {
          rootSectionId.value = Number(rsi)
          try {
            allSections.value = await inspTemplateApi.getSections(Number(rsi))
          } catch (e: any) {
            console.warn('getSections 失败，回退到 getChildSections', e)
            // 如果 getSections(tree) 失败，用 getChildSections
            try {
              allSections.value = await inspTemplateApi.getChildSections(Number(rsi))
            } catch (e2: any) { console.error('加载分区失败', e2); ElMessage.error('加载检查分区失败') }
          }
          // Build section tree for multi-level rendering
          if (allSections.value.length > 0) {
            sectionTree.value = buildSectionTree(allSections.value, Number(rsi))
          }
        }
      } catch (e: any) { console.error('加载项目信息失败', e); ElMessage.error('加载项目信息失败') }
    }

    // Load target filter context (org tree, etc.) - must await before rendering targets
    if (submissions.value.length > 0) {
      try { await loadTargetFilterContext() } catch { /* non-fatal */ }
    }

    // Auto-select first pending target
    if (uniqueTargets.value.length > 0 && !selectedTargetId.value) {
      // Select first pending target, or first target
      const pending = uniqueTargets.value.find(t => getTargetStatus(t.targetId) !== 'COMPLETED')
      await selectTarget((pending || uniqueTargets.value[0]).targetId)
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function reloadAll(keepSelected = true) {
  const prevTargetId = selectedTargetId.value
  submissions.value = await store.loadSubmissions(taskId)
  task.value = await store.loadTask(taskId)
  if (keepSelected && prevTargetId) {
    // Re-select the same target to refresh its groups
    await selectTarget(prevTargetId)
  }
}

// ==================== Task Lifecycle ====================

async function handleStartTask() {
  try {
    await store.startTask(taskId)
    ElMessage.success('任务已开始')
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleSubmitTask() {
  try {
    await ElMessageBox.confirm('确认提交此检查任务？提交后如需修改可撤回。', '提交确认', { type: 'warning' })
    await store.submitTask(taskId)
    ElMessage.success('任务已提交')
    await reloadAll(false)
  } catch (e: any) {
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') { console.error('提交任务失败', e); ElMessage.error('提交任务失败，请重试') }
  }
}

async function handleWithdrawTask() {
  try {
    await ElMessageBox.confirm('撤回后任务将回到进行中状态，可重新修改打分。', '撤回确认', { type: 'info' })
    await store.withdrawTask(taskId)
    ElMessage.success('任务已撤回，可重新修改')
    await reloadAll(false)
  } catch (e: any) {
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') { console.error('撤回失败', e); ElMessage.error('撤回失败，请重试') }
  }
}

// ==================== Target Completion ====================

/** Target mode: complete all sections for the selected target */
async function handleCompleteTarget() {
  if (!selectedTargetId.value) return
  const groups = targetSectionGroups.value
  // Check all groups have all items scored
  for (const group of groups) {
    if (group.isIntermediate) continue
    if (group.submission.status === 'COMPLETED' || group.submission.status === 'SKIPPED') continue
    const scorable = group.details.filter(d => d.scoringMode)
    const scored = scorable.filter(d => {
      if (['DEDUCTION', 'ADDITION', 'DIRECT', 'CUMULATIVE', 'RATING_SCALE'].includes(d.scoringMode!)) {
        return numberInputs.value[d.id] !== undefined
      }
      if (['PASS_FAIL', 'LEVEL', 'SCORE_TABLE', 'TIERED_DEDUCTION'].includes(d.scoringMode!)) {
        return selectInputs.value[d.id] !== undefined
      }
      return d.responseValue != null && d.responseValue !== ''
    })
    if (scored.length < scorable.length) {
      ElMessage.warning(`「${group.section?.sectionName || '分区'}」还有 ${scorable.length - scored.length} 项未评分`)
      return
    }
  }
  try {
    for (const group of groups) {
      if (group.isIntermediate) continue
      if (group.submission.status !== 'IN_PROGRESS') continue
      // Calculate score for this group
      let deductions = 0, bonuses = 0, base = 100, passCount = 0, failCount = 0
      for (const d of group.details) {
        if (!d.scoringMode) continue
        const num = numberInputs.value[d.id]
        const sel = selectInputs.value[d.id]
        if (d.scoringMode === 'DEDUCTION' && num != null) deductions += Math.abs(num)
        else if (d.scoringMode === 'ADDITION' && num != null) bonuses += num
        else if (d.scoringMode === 'PASS_FAIL') { if (sel === 'PASS') passCount++; else failCount++ }
        else if (['LEVEL', 'SCORE_TABLE', 'TIERED_DEDUCTION'].includes(d.scoringMode) && num != null) {
          // score is already set via handleGradeSelect
        }
        else if (d.scoringMode === 'DIRECT' && num != null) base = num
      }
      const finalScore = Math.max(0, base - deductions + bonuses)
      const grade = getGrade(finalScore)
      await store.completeSubmission(group.submission.id, {
        baseScore: finalScore,
        finalScore,
        deductionTotal: deductions,
        bonusTotal: bonuses,
        scoreBreakdown: JSON.stringify({ passCount, failCount, deductions, bonuses }),
        grade,
        passed: finalScore >= 60,
      })
    }
    ElMessage.success('所有分区评分已完成')
    await reloadAll()
    // Auto-advance to next target
    if (hasNextTarget.value) {
      const next = filteredTargets.value.find(t => getTargetStatus(t.targetId) !== 'COMPLETED')
      if (next) await selectTarget(next.targetId)
      else goToNextTarget()
    }
  } catch (e: any) {
    ElMessage.error(e.message || '完成失败')
  }
}

function goBack() {
  router.push('/inspection/v7/tasks')
}

const progressText = computed(() => {
  if (!task.value) return ''
  const done = task.value.completedTargets + task.value.skippedTargets
  return `${done}/${task.value.totalTargets}`
})

onMounted(() => loadData())
</script>

<template>
  <div class="exec-root" v-loading="loading">

    <!-- ===== TOP BAR ===== -->
    <div class="topbar">
      <div class="topbar-left">
        <button class="back-btn" @click="goBack">
          <ArrowLeft :size="16" />
        </button>
        <div class="topbar-info">
          <div class="topbar-title">
            <span class="task-code">{{ task?.taskCode || '—' }}</span>
            <el-tag
              v-if="task"
              :type="(TaskStatusConfig[task.status as TaskStatus]?.type as any)"
              size="small"
              effect="plain"
            >
              {{ TaskStatusConfig[task.status as TaskStatus]?.label }}
            </el-tag>
          </div>
          <div class="topbar-sub" v-if="task">
            {{ task.taskDate }}
            <span v-if="task.inspectorName">· {{ task.inspectorName }}</span>
            <span class="progress-text">进度 {{ progressText }}</span>
          </div>
        </div>
      </div>
      <div class="topbar-actions" v-if="task">
        <el-button
          v-if="task.status === 'CLAIMED'"
          type="primary" size="small"
          @click="handleStartTask"
        >
          <Play :size="13" class="btn-icon" />开始检查
        </el-button>
        <el-button
          v-if="task.status === 'IN_PROGRESS'"
          type="warning" size="small"
          @click="handleSubmitTask"
        >
          <Send :size="13" class="btn-icon" />提交任务
        </el-button>
        <el-button
          v-if="task.status === 'SUBMITTED'"
          type="info" size="small"
          @click="handleWithdrawTask"
        >
          <RotateCcw :size="13" class="btn-icon" />撤回
        </el-button>
      </div>
    </div>

    <!-- ===== MAIN BODY ===== -->
    <div class="main-body">

      <!-- ===== LEFT SIDEBAR ===== -->
      <div class="sidebar">
        <div class="sidebar-header">
          <div class="search-box">
            <el-input v-model="targetSearch" placeholder="搜索目标..." size="small" clearable>
              <template #prefix><Search :size="13" /></template>
            </el-input>
          </div>
          <div v-if="contextFilterOptions.length > 1" class="filter-row">
            <select v-model="targetContextFilter" class="filter-select">
              <option value="">{{ contextFilterLabel }}: 全部</option>
              <option v-for="opt in contextFilterOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </div>
          <div class="status-pills">
            <button class="status-pill" :class="{ active: targetFilter === 'all' }" @click="targetFilter = 'all'">
              全部 <span class="pill-count">{{ uniqueTargets.length }}</span>
            </button>
            <button class="status-pill" :class="{ active: targetFilter === 'pending' }" @click="targetFilter = 'pending'">
              待检 <span class="pill-count">{{ pendingCount }}</span>
            </button>
            <button class="status-pill" :class="{ active: targetFilter === 'completed' }" @click="targetFilter = 'completed'">
              已完成 <span class="pill-count">{{ completedTargetCount }}</span>
            </button>
          </div>
        </div>
        <div class="target-list">
          <div
            v-for="t in filteredTargets" :key="t.targetId"
            class="target-item" :class="{ active: selectedTargetId === t.targetId }"
            @click="selectTarget(t.targetId)"
          >
            <span class="target-dot" :class="getTargetDotClass(t.targetId)" />
            <span class="target-name">{{ t.targetName }}</span>
            <span class="target-score">{{ getTargetProgress(t.targetId).done }}/{{ getTargetProgress(t.targetId).total }}</span>
          </div>
          <div v-if="filteredTargets.length === 0" class="target-empty">无匹配目标</div>
        </div>
      </div>

      <!-- ===== RIGHT: SCORING PANEL ===== -->
      <div class="scoring-panel">

        <!-- Panel Header -->
        <div class="panel-header" v-if="selectedTargetId">
          <div class="panel-header-info">
            <div class="panel-target-name">{{ currentTargetName }}</div>
            <div class="panel-progress">
              <span>{{ targetItemProgress.done }}/{{ targetItemProgress.total }} 已评</span>
              <div class="progress-bar-wrap">
                <div class="progress-bar-fill" :style="{ width: targetProgressPercent + '%' }" />
              </div>
              <span class="progress-pct" :style="{ color: 'var(--blue)' }">{{ targetProgressPercent }}%</span>
            </div>
          </div>
          <button class="nav-btn" :disabled="!hasPrevTarget" @click="goToPrevTarget">
            <ChevronLeft :size="16" />
          </button>
          <button class="nav-btn" :disabled="!hasNextTarget" @click="goToNextTarget">
            <ChevronRight :size="16" />
          </button>
        </div>

        <!-- Scrollable Body -->
        <div class="panel-body" v-loading="detailLoading">
          <div v-if="!selectedTargetId" class="panel-empty">
            <Target :size="28" style="color: var(--gray-300)" />
            <p>请在左侧选择检查目标</p>
          </div>
          <template v-else>
            <!-- Section cards -->
            <div v-for="(group, gi) in targetSectionGroups" :key="gi"
              v-show="!isGroupHiddenByParent(gi)"
              class="section" :class="{ collapsed: group.collapsed, 'section--intermediate': group.isIntermediate, [`section--depth-${group.depth}`]: true }"
              :style="{ paddingLeft: group.depth * 24 + 'px' }">
              <!-- Depth connector line -->
              <div v-if="group.depth > 0" class="section-depth-line" :style="{ left: (group.depth * 24 - 12) + 'px' }" />
              <!-- Intermediate section: group header only -->
              <div v-if="group.isIntermediate" class="section-header section-header--intermediate" @click="group.collapsed = !group.collapsed">
                <svg class="section-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"/></svg>
                <span class="section-depth-badge" v-if="group.depth > 0">L{{ group.depth }}</span>
                <span class="section-title section-title--intermediate">{{ group.section?.sectionName || '未知分组' }}</span>
                <div class="section-meta">
                  <span class="section-count">{{ intermediateChildCount(group) }} 子分区</span>
                </div>
              </div>
              <!-- Leaf section: normal header with progress -->
              <div v-else class="section-header" @click="group.collapsed = !group.collapsed">
                <svg class="section-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"/></svg>
                <span class="section-depth-badge" v-if="group.depth > 0">L{{ group.depth }}</span>
                <span class="section-title">{{ group.section?.sectionName || '未知分区' }}</span>
                <div class="section-meta">
                  <span class="section-count">{{ group.details.length }}项</span>
                  <span>{{ scoredCountInGroup(group) }}/{{ group.details.length }} 已评</span>
                  <div class="section-progress-mini">
                    <div class="section-progress-fill" :style="{ width: groupProgressPercent(group) + '%' }" />
                  </div>
                </div>
              </div>
              <div v-if="!group.isIntermediate" class="section-body">
                <!-- Field cards with scoring controls -->
                <div v-for="detail in group.details" :key="detail.id"
                  class="score-card" :class="{ scored: isDetailScored(detail), 'score-card--fullwidth': detail.inputMode === 'EVENT_STREAM' }">
                  <div class="card-top">
                    <span class="card-name">{{ detail.itemName }}</span>
                    <el-tag v-if="detail.scoringMode" size="small" effect="plain"
                      :type="detail.scoringMode === 'DEDUCTION' ? 'danger' : detail.scoringMode === 'ADDITION' ? 'success' : detail.scoringMode === 'PASS_FAIL' ? 'primary' : 'warning'"
                    >{{ ScoringModeConfig[detail.scoringMode]?.label ?? detail.scoringMode }}</el-tag>
                    <el-tag v-else size="small" type="info" effect="plain">采集</el-tag>
                  </div>
                  <!-- EVENT_STREAM mode: show search+record interface -->
                  <template v-if="detail.inputMode === 'EVENT_STREAM'">
                    <EventStreamRecorder
                      :section-id="Number(group.submission.sectionId || 0)"
                      :target-type="group.submission.targetType"
                      :items="[detail]"
                      :submissions="getSubmissionsForSection(Number(group.submission.sectionId || 0))"
                      :disabled="!isGroupEditable(group)"
                    />
                  </template>
                  <!-- INLINE mode: standard scoring controls -->
                  <div v-else-if="detail.scoringMode === 'DEDUCTION'" class="card-control">
                    <div class="stepper">
                      <button class="stepper-btn" :disabled="!isGroupEditable(group) || (numberInputs[detail.id] ?? 0) <= getDeductionRange(detail).min"
                        @click="handleDeductionStep(detail, -1)">−</button>
                      <span class="stepper-value" :class="{ negative: (numberInputs[detail.id] ?? 0) < 0 }">
                        {{ numberInputs[detail.id] ?? 0 }}
                      </span>
                      <button class="stepper-btn" :disabled="!isGroupEditable(group) || (numberInputs[detail.id] ?? 0) >= 0"
                        @click="handleDeductionStep(detail, 1)">+</button>
                    </div>
                    <span v-if="(numberInputs[detail.id] ?? 0) < 0" class="score-hint">{{ numberInputs[detail.id] }}分</span>
                  </div>
                  <div v-else-if="detail.scoringMode === 'ADDITION'" class="card-control">
                    <div class="stepper">
                      <button class="stepper-btn" :disabled="!isGroupEditable(group) || (numberInputs[detail.id] ?? 0) <= 0"
                        @click="handleAdditionStep(detail, -1)">−</button>
                      <span class="stepper-value" :class="{ positive: (numberInputs[detail.id] ?? 0) > 0 }">
                        {{ numberInputs[detail.id] ?? 0 }}
                      </span>
                      <button class="stepper-btn" :disabled="!isGroupEditable(group) || (numberInputs[detail.id] ?? 0) >= getAdditionRange(detail).max"
                        @click="handleAdditionStep(detail, 1)">+</button>
                    </div>
                    <span v-if="(numberInputs[detail.id] ?? 0) > 0" class="score-hint">+{{ numberInputs[detail.id] }}分</span>
                  </div>
                  <div v-else-if="detail.scoringMode === 'PASS_FAIL'" class="card-control">
                    <div class="pill-group">
                      <button class="pill-btn" :class="{ 'active-green': selectInputs[detail.id] === 'PASS' }"
                        :disabled="!isGroupEditable(group)" @click="handlePassFail(detail, 'PASS')">&#10003; 通过 <span class="pill-score">{{ parseScoringConfig(detail).passScore ?? 0 }}分</span></button>
                      <button class="pill-btn" :class="{ 'active-red': selectInputs[detail.id] === 'FAIL' }"
                        :disabled="!isGroupEditable(group)" @click="handlePassFail(detail, 'FAIL')">&#10007; 不通过 <span class="pill-score">{{ parseScoringConfig(detail).failScore ?? -5 }}分</span></button>
                    </div>
                  </div>
                  <div v-else-if="['LEVEL','SCORE_TABLE','TIERED_DEDUCTION'].includes(detail.scoringMode!)" class="card-control">
                    <div class="pill-group">
                      <button v-for="lv in getGradeLevels(detail)" :key="lv.label"
                        class="pill-btn" :class="{ 'active-blue': selectInputs[detail.id] === lv.label }"
                        :disabled="!isGroupEditable(group)" @click="handleGradeSelect(detail, lv.label, lv.score)">
                        {{ lv.label }} <span class="pill-score">{{ lv.score }}分</span>
                      </button>
                    </div>
                  </div>
                  <div v-else-if="detail.scoringMode === 'DIRECT'" class="card-control card-control--direct">
                    <el-input-number v-model="numberInputs[detail.id]" :min="getDirectRange(detail).min" :max="getDirectRange(detail).max"
                      :disabled="!isGroupEditable(group)" size="small" style="width: 120px" @change="(val: any) => handleDirectInput(detail, val)" />
                    <span class="range-hint">范围 {{ getDirectRange(detail).min }}–{{ getDirectRange(detail).max }}</span>
                  </div>
                  <div v-else-if="detail.scoringMode === 'RATING_SCALE'" class="card-control">
                    <button v-for="n in getRatingMax(detail)" :key="n" class="star-btn"
                      :class="{ 'star-btn--active': (numberInputs[detail.id] ?? 0) >= n }"
                      :disabled="!isGroupEditable(group)" @click="handleRatingScale(detail, n)"><Star :size="18" /></button>
                    <span class="star-val" v-if="numberInputs[detail.id] !== undefined">{{ numberInputs[detail.id] }}/{{ getRatingMax(detail) }} <span class="pill-score">{{ Math.round((numberInputs[detail.id] / getRatingMax(detail)) * (parseScoringConfig(detail).maxScore ?? 100)) }}分</span></span>
                  </div>
                  <div v-else-if="detail.scoringMode === 'CUMULATIVE'" class="card-control">
                    <div class="stepper">
                      <button class="stepper-btn" :disabled="!isGroupEditable(group) || (numberInputs[detail.id] ?? 0) <= 0"
                        @click="handleCumulativeChange(detail, -1)">−</button>
                      <span class="stepper-value">{{ numberInputs[detail.id] ?? 0 }}</span>
                      <button class="stepper-btn" :disabled="!isGroupEditable(group)"
                        @click="handleCumulativeChange(detail, 1)">+</button>
                    </div>
                    <span class="counter-unit">{{ getCumulativeConfig(detail).countLabel }}</span>
                    <span class="score-hint">× {{ getCumulativeConfig(detail).scorePerUnit }}分 = {{ (numberInputs[detail.id] ?? 0) * getCumulativeConfig(detail).scorePerUnit }}分</span>
                  </div>
                  <div v-else-if="!detail.scoringMode && detail.itemType !== 'VIOLATION_RECORD' && detail.itemType !== 'PERSON_SCORE'" class="card-control card-control--capture">
                    <el-input v-model="textInputs[detail.id]" :disabled="!isGroupEditable(group)" size="small" placeholder="请填写..." clearable @blur="saveTextInput(detail)" />
                  </div>
                  <!-- VIOLATION_RECORD: EVENT_STREAM or INLINE mode -->
                  <template v-else-if="detail.itemType === 'VIOLATION_RECORD'">
                    <EventStreamRecorder
                      v-if="detail.inputMode === 'EVENT_STREAM'"
                      :section-id="Number(group.submission.sectionId || 0)"
                      :target-type="group.submission.targetType"
                      :items="[detail]"
                      :submissions="getSubmissionsForSection(Number(group.submission.sectionId || 0))"
                      :disabled="!isGroupEditable(group)"
                    />
                    <ViolationRecordInput
                      v-else
                      :submission-id="group.submission.id"
                      :detail-id="detail.id"
                      :section-id="detail.sectionId"
                      :item-id="detail.templateItemId"
                      :disabled="!isGroupEditable(group)"
                    />
                  </template>
                  <!-- PERSON_SCORE -->
                  <template v-else-if="detail.itemType === 'PERSON_SCORE'">
                    <PersonScoreGrid
                      :target-type="group.submission.targetType"
                      :target-id="group.submission.targetId"
                      :detail-id="detail.id"
                      :disabled="!isGroupEditable(group)"
                    />
                  </template>
                </div>
                <div v-if="group.details.length === 0" class="panel-empty" style="height:80px"><p>暂无检查项</p></div>
              </div>
            </div>

            <div v-if="targetSectionGroups.length === 0 && !detailLoading" class="panel-empty">
              <Target :size="28" style="color: var(--gray-300)" />
              <p>暂无检查分区</p>
              <p style="font-size:11px;color:var(--gray-400)">请检查模板是否已配置检查项，或联系管理员</p>
            </div>
          </template>
        </div>

        <!-- Bottom Bar -->
        <div class="bottom-bar" v-if="selectedTargetId">
          <div class="bottom-info">
            目标 <strong>{{ currentTargetIdx + 1 }}</strong> / {{ filteredTargets.length }}
            &nbsp;&middot;&nbsp; 分区 <strong>{{ completedSectionCount }}</strong>/{{ leafSectionCount }} 已完成
          </div>
          <div class="bottom-score">
            <div class="score-display">
              <div class="score-label">当前得分</div>
              <div class="score-value">{{ targetScore }}</div>
            </div>
            <el-button type="primary" :disabled="getTargetStatus(selectedTargetId) === 'COMPLETED'"
              @click="handleCompleteTarget">
              完成并下一个 <ChevronRight :size="14" />
            </el-button>
          </div>
        </div>

      </div>
      <!-- /scoring-panel -->
    </div>
    <!-- /main-body -->
  </div>
</template>

<style scoped>
/* ===== Root ===== */
.exec-root {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f6fa;
  font-size: 12px;
  color: #1f2937;
}

/* ===== Top Bar ===== */
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 52px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  transition: background 0.15s;
}
.back-btn:hover { background: #f3f4f6; color: #374151; }

.topbar-info { display: flex; flex-direction: column; gap: 2px; }

.topbar-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-code {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.topbar-sub {
  font-size: 11px;
  color: #9ca3af;
  display: flex;
  align-items: center;
  gap: 6px;
}

.progress-text {
  margin-left: 8px;
  color: #6b7280;
}

.topbar-actions { display: flex; align-items: center; gap: 8px; }

.btn-icon { margin-right: 4px; }

/* ===== Main Body ===== */
.main-body {
  display: flex;
  flex: 1;
  min-height: 0;
}

/* ===== Sidebar (iAuditor style) ===== */
.sidebar {
  width: 220px;
  min-width: 220px;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  padding: 12px 12px 8px;
  flex-shrink: 0;
}

.search-box {
  margin-bottom: 8px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.filter-select {
  flex: 1;
  height: 26px;
  border: 1px solid #e5e7eb;
  border-radius: 5px;
  padding: 0 6px;
  font-size: 11px;
  color: #4b5563;
  background: #fff;
  outline: none;
  cursor: pointer;
}

.status-pills {
  display: flex;
  gap: 4px;
  margin-bottom: 4px;
}
.status-pill {
  flex: 1;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  border: 1px solid #e5e7eb;
  border-radius: 5px;
  font-size: 11px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
  background: #fff;
}
.status-pill:hover { border-color: #1a6dff; color: #1a6dff; }
.status-pill.active {
  background: #1a6dff;
  color: #fff;
  border-color: #1a6dff;
}
.pill-count { font-weight: 600; }

.target-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 6px;
}
.target-list::-webkit-scrollbar { width: 4px; }
.target-list::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 2px; }

.target-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  border-radius: 5px;
  cursor: pointer;
  transition: background 0.12s;
  font-size: 11px;
  line-height: 1.4;
}
.target-item:hover { background: #f9fafb; }
.target-item.active { background: #e8f0ff; }

.target-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}
.target-dot.done { background: #10b981; }
.target-dot.progress { background: #1a6dff; }
.target-dot.pending { background: #d1d5db; }

.target-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #374151;
}
.target-item.active .target-name { color: #1a6dff; font-weight: 500; }

.target-score {
  flex-shrink: 0;
  font-size: 10px;
  color: #9ca3af;
  font-variant-numeric: tabular-nums;
}
.target-item.active .target-score { color: #1a6dff; }

.target-empty {
  padding: 16px 12px;
  font-size: 11px;
  color: #d1d5db;
  text-align: center;
}

/* ===== Right Scoring Panel (iAuditor style) ===== */
.scoring-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  background: #f9fafb;
  position: relative;
}

/* Panel header */
.panel-header {
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.panel-header-info { flex: 1; }
.panel-target-name {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 2px;
}
.panel-progress {
  font-size: 12px;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 8px;
}
.progress-bar-wrap {
  width: 120px;
  height: 4px;
  background: #e5e7eb;
  border-radius: 2px;
  overflow: hidden;
}
.progress-bar-fill {
  height: 100%;
  background: #1a6dff;
  border-radius: 2px;
  transition: width 0.3s;
}
.progress-pct {
  font-weight: 500;
}

.nav-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #6b7280;
  transition: all 0.15s;
}
.nav-btn:hover:not(:disabled) { border-color: #1a6dff; color: #1a6dff; background: #e8f0ff; }
.nav-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* Scrollable body */
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px 80px;
}
.panel-body::-webkit-scrollbar { width: 6px; }
.panel-body::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 3px; }

/* Panel empty state */
.panel-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #d1d5db;
  gap: 8px;
}
.panel-empty p { font-size: 12px; }

/* Sections */
.section {
  margin-bottom: 16px;
}
.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  user-select: none;
  transition: background 0.12s;
}
.section-header:hover { background: #f9fafb; }

.section-chevron {
  width: 16px;
  height: 16px;
  color: #9ca3af;
  transition: transform 0.2s;
  flex-shrink: 0;
}
.section.collapsed .section-chevron { transform: rotate(-90deg); }

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
}
.section-meta {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: #9ca3af;
}
.section-count { font-weight: 500; }
.section-progress-mini {
  width: 40px;
  height: 3px;
  background: #e5e7eb;
  border-radius: 2px;
  overflow: hidden;
}
.section-progress-fill {
  height: 100%;
  border-radius: 2px;
  background: #1a6dff;
}

.section-body {
  margin-top: 8px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 8px;
}
.section-body--stream {
  margin-top: 8px;
  display: block;
}
.section.collapsed .section-body { display: none; }

/* Intermediate section (group header) */
.section {
  position: relative;
}
.section-depth-line {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 1px;
  background: #e5e7eb;
}
.section-depth-badge {
  font-size: 9px;
  font-weight: 600;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f0f0f0;
  color: #888;
  flex-shrink: 0;
  margin-right: 2px;
}
.section--intermediate {
  margin-bottom: 8px;
}
.section-header--intermediate {
  background: #fafbfc;
  border-left: 3px solid #8b5cf6;
}
.section-header--intermediate:hover { background: #f5f3ff; }
.section-title--intermediate {
  font-size: 14px;
  font-weight: 700;
  color: #1e1b4b;
}
.section-type-tag {
  font-size: 9px; font-weight: 700; padding: 1px 5px; border-radius: 3px;
  background: #f3f0ff; color: #8b5cf6; text-transform: uppercase;
  flex-shrink: 0;
}

/* Score Cards */
.score-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-left: 3px solid transparent;
  border-radius: 8px;
  padding: 10px 12px;
  transition: box-shadow 0.15s, border-color 0.15s;
}
.score-card:hover { box-shadow: 0 1px 3px rgba(0,0,0,0.1), 0 1px 2px rgba(0,0,0,0.06); border-color: #d1d5db; border-left-color: transparent; }
.score-card.scored { border-left-color: #1a6dff; }
.score-card--fullwidth { grid-column: 1 / -1; }

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.card-name {
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
}

/* Card controls */
.card-control { display: flex; align-items: center; gap: 6px; }
.card-control--direct { gap: 10px; }
.card-control--capture { display: block; }

.range-hint {
  font-size: 11px;
  color: #9ca3af;
}

/* Stepper */
.stepper {
  display: inline-flex;
  align-items: center;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
  height: 30px;
}
.stepper-btn {
  width: 30px;
  height: 30px;
  border: none;
  background: #f9fafb;
  color: #4b5563;
  font-size: 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.12s;
  line-height: 1;
}
.stepper-btn:hover { background: #f3f4f6; color: #1a6dff; }
.stepper-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.stepper-value {
  width: 40px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  background: #fff;
  border-left: 1px solid #e5e7eb;
  border-right: 1px solid #e5e7eb;
  height: 30px;
  line-height: 30px;
  font-variant-numeric: tabular-nums;
}
.stepper-value.negative { color: #ef4444; }
.stepper-value.positive { color: #10b981; }

/* Pill Group */
.pill-group {
  display: flex;
  gap: 0;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
}
.pill-btn {
  padding: 5px 12px;
  font-size: 12px;
  border: none;
  background: #fff;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.12s;
  font-weight: 500;
  white-space: nowrap;
  border-right: 1px solid #e5e7eb;
}
.pill-btn:last-child { border-right: none; }
.pill-btn:hover { background: #f9fafb; color: #374151; }
.pill-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.pill-btn.active-green { background: #10b981; color: #fff; }
.pill-btn.active-red { background: #ef4444; color: #fff; }
.pill-btn.active-blue { background: #1a6dff; color: #fff; }

.counter-unit { font-size: 11px; color: #9ca3af; }
.score-hint { font-size: 11px; color: #9ca3af; margin-left: 4px; }
.pill-score { font-size: 11px; opacity: 0.75; margin-left: 2px; }

/* Star buttons */
.star-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 2px;
  color: #d1d5db;
  transition: color 0.1s;
}
.star-btn:hover:not(:disabled) { color: #f59e0b; }
.star-btn--active { color: #f59e0b; }
.star-btn:disabled { opacity: 0.45; cursor: not-allowed; }

.star-val {
  font-size: 11px;
  color: #9ca3af;
  margin-left: 4px;
}

/* ===== Bottom Bar ===== */
.bottom-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: #fff;
  border-top: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  z-index: 10;
}
.bottom-info {
  font-size: 13px;
  color: #6b7280;
}
.bottom-info strong { color: #374151; }

.bottom-score {
  display: flex;
  align-items: center;
  gap: 16px;
}
.score-display {
  text-align: right;
}
.score-label {
  font-size: 11px;
  color: #9ca3af;
}
.score-value {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}
</style>
