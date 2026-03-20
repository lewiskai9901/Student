<script setup lang="ts">
/**
 * TaskExecutionView - Design B
 *
 * Layout:
 *   [Top bar]       back + task name + task code + status actions
 *   [Root targets]  horizontal pills filtered from submissions' rootTargetId
 *   [Section tabs]  first-level sections (direct children of root)
 *   [Left | Right]  target list sidebar  |  scoring form
 *   [Bottom bar]    save draft + submit target
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Play, Send, Lock, SkipForward, Check, X,
  AlertTriangle, ChevronLeft, ChevronRight, ChevronDown,
} from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/insp/inspExecutionStore'
import {
  TaskStatusConfig, type TaskStatus,
  SubmissionStatusConfig, type SubmissionStatus,
} from '@/types/insp/enums'
import type { InspTask, InspSubmission, SubmissionDetail } from '@/types/insp/project'
import type { TemplateSection } from '@/types/insp/template'
import { useSubmissionScoring, MODE_LABEL, MODE_TAG_TYPE } from '@/composables/insp/useSubmissionScoring'
import { useConditionLogicState } from '@/composables/insp/useConditionLogicState'
import { getProject } from '@/api/insp/project'
import { inspTemplateApi } from '@/api/insp/template'
import ScoringItemRow from './components/ScoringItemRow.vue'
import PersonScoreGrid from './components/PersonScoreGrid.vue'
import ViolationRecordInput from './components/ViolationRecordInput.vue'

const route = useRoute()
const router = useRouter()
const store = useInspExecutionStore()
const taskId = Number(route.params.id)

// ==================== Core State ====================

const loading = ref(false)
const task = ref<InspTask | null>(null)
const submissions = ref<InspSubmission[]>([])
const selectedSubmission = ref<InspSubmission | null>(null)
const details = ref<SubmissionDetail[]>([])

// Section tree state
const allSections = ref<TemplateSection[]>([])
const rootSectionId = ref<number | null>(null)

// Selection state
const selectedRootTargetId = ref<number | null>(null)
const activeSectionId = ref<number | null>(null)

// Collapsible sub-section state
const collapsedSections = ref<Set<string>>(new Set())

const isEditable = computed(() => selectedSubmission.value?.status === 'IN_PROGRESS')

// ==================== Composables ====================

const sectionConditions = computed(() => store.sectionConditions)

const conditionLogic = useConditionLogicState(details, sectionConditions, ref({}))

const scoring = useSubmissionScoring(
  details,
  store,
  conditionLogic.getScoreOverride,
  conditionLogic.isItemDisabled,
  conditionLogic.isItemVisible,
)

scoring.setConditionRequiredFn(conditionLogic.isItemConditionallyRequired)

// ==================== Root Targets ====================

interface RootTarget {
  id: number | null
  name: string
  count: number
  completedCount: number
}

const rootTargets = computed<RootTarget[]>(() => {
  const map = new Map<number | null, { name: string; count: number; completed: number }>()
  for (const sub of submissions.value) {
    const key = sub.rootTargetId
    if (!map.has(key)) {
      map.set(key, {
        name: sub.rootTargetName || '(未分组)',
        count: 0,
        completed: 0,
      })
    }
    const entry = map.get(key)!
    entry.count++
    if (sub.status === 'COMPLETED' || sub.status === 'SKIPPED') {
      entry.completed++
    }
  }
  return Array.from(map, ([id, v]) => ({
    id,
    name: v.name,
    count: v.count,
    completedCount: v.completed,
  }))
})

// ==================== First-level Sections ====================

const firstLevelSections = computed(() => {
  if (!rootSectionId.value) return []
  return allSections.value.filter(s => s.parentSectionId === rootSectionId.value)
})

// ==================== Filtered Submissions ====================

/** Submissions filtered by both root target and section */
const filteredSubmissions = computed(() => {
  let filtered = submissions.value

  // Filter by root target
  if (selectedRootTargetId.value !== null || rootTargets.value.length > 0) {
    filtered = filtered.filter(s => s.rootTargetId === selectedRootTargetId.value)
  }

  // Filter by active section
  if (activeSectionId.value) {
    filtered = filtered.filter(s => s.sectionId === activeSectionId.value)
  }

  return filtered
})

// ==================== Overview Tab ====================

/** "Overview" is a virtual tab with id = 0. Real tabs are first-level sections. */
const OVERVIEW_TAB_ID = 0

const activeSectionTab = computed({
  get: () => activeSectionId.value ?? OVERVIEW_TAB_ID,
  set: (val: number) => {
    activeSectionId.value = val === OVERVIEW_TAB_ID ? null : val
    // When switching section tab, auto-clear selected submission
    selectedSubmission.value = null
    details.value = []
  },
})

// ==================== Form: section + child sections ====================

/**
 * Build the form structure for the selected submission.
 * The form shows:
 *   - All items from the active section
 *   - Items from child sections that have NO targetType (they inherit the parent target)
 * Sub-sections are rendered as collapsible group headers.
 */
interface FormGroup {
  sectionId: number | null
  sectionName: string
  isSubSection: boolean
  regularItems: SubmissionDetail[]
  personScoreItems: SubmissionDetail[]
  violationRecordItems: SubmissionDetail[]
}

const formGroups = computed<FormGroup[]>(() => {
  if (!selectedSubmission.value || details.value.length === 0) return []

  const visible = scoring.visibleDetails.value

  // Group by sectionId (which maps to either the active section or its child sections)
  const sectionMap = new Map<number | null, SubmissionDetail[]>()
  for (const d of visible) {
    const key = d.sectionId
    if (!sectionMap.has(key)) sectionMap.set(key, [])
    sectionMap.get(key)!.push(d)
  }

  // Build ordered groups. The active section items come first, then child sections sorted by sortOrder.
  const groups: FormGroup[] = []

  // Find child sections (sections whose parentSectionId = activeSectionId and have no targetType)
  const childSections = activeSectionId.value
    ? allSections.value
        .filter(s => s.parentSectionId === activeSectionId.value && !s.targetType)
        .sort((a, b) => a.sortOrder - b.sortOrder)
    : []

  // Items directly under the active section
  const directItems = sectionMap.get(activeSectionId.value) || []
  // Also collect items whose sectionId matches the active section by sectionName fallback
  const activeSectionName = activeSectionId.value
    ? allSections.value.find(s => s.id === activeSectionId.value)?.sectionName
    : null

  // Items that don't match any known child section go to the main group
  const childSectionIds = new Set(childSections.map(s => s.id))
  const mainItems = visible.filter(d => {
    // Include if directly under active section or has no section assignment
    if (d.sectionId === activeSectionId.value || d.sectionId === null) return true
    if (!childSectionIds.has(d.sectionId!)) {
      // Item belongs to a section that is not a direct child - check by name
      if (d.sectionName === activeSectionName) return true
    }
    return false
  })

  if (mainItems.length > 0) {
    groups.push(buildFormGroup(null, activeSectionName || '检查项', false, mainItems))
  }

  // Child section groups
  for (const cs of childSections) {
    const csItems = visible.filter(d => d.sectionId === cs.id || d.sectionName === cs.sectionName)
    if (csItems.length > 0) {
      groups.push(buildFormGroup(cs.id, cs.sectionName, true, csItems))
    }
  }

  // If we have items not yet assigned to any group, put them in an "Other" group
  const assignedIds = new Set(groups.flatMap(g => [
    ...g.regularItems.map(d => d.id),
    ...g.personScoreItems.map(d => d.id),
    ...g.violationRecordItems.map(d => d.id),
  ]))
  const unassigned = visible.filter(d => !assignedIds.has(d.id))
  if (unassigned.length > 0) {
    groups.push(buildFormGroup(null, '其他', true, unassigned))
  }

  return groups
})

function buildFormGroup(
  sectionId: number | null,
  name: string,
  isSubSection: boolean,
  items: SubmissionDetail[],
): FormGroup {
  return {
    sectionId,
    sectionName: name,
    isSubSection,
    regularItems: items.filter(d => d.itemType !== 'PERSON_SCORE' && d.itemType !== 'VIOLATION_RECORD'),
    personScoreItems: items.filter(d => d.itemType === 'PERSON_SCORE'),
    violationRecordItems: items.filter(d => d.itemType === 'VIOLATION_RECORD'),
  }
}

function toggleSectionCollapse(sectionName: string) {
  if (collapsedSections.value.has(sectionName)) {
    collapsedSections.value.delete(sectionName)
  } else {
    collapsedSections.value.add(sectionName)
  }
}

function isSectionCollapsed(sectionName: string): boolean {
  return collapsedSections.value.has(sectionName)
}

// ==================== Navigation ====================

const currentTargetIndex = computed(() => {
  if (!selectedSubmission.value) return -1
  return filteredSubmissions.value.findIndex(s => s.id === selectedSubmission.value!.id)
})

const hasPrevTarget = computed(() => currentTargetIndex.value > 0)
const hasNextTarget = computed(() =>
  currentTargetIndex.value >= 0 && currentTargetIndex.value < filteredSubmissions.value.length - 1,
)

function goToPrevTarget() {
  if (hasPrevTarget.value) {
    selectSubmission(filteredSubmissions.value[currentTargetIndex.value - 1])
  }
}

function goToNextTarget() {
  if (hasNextTarget.value) {
    selectSubmission(filteredSubmissions.value[currentTargetIndex.value + 1])
  }
}

// ==================== Task Progress ====================

const progressText = computed(() => {
  if (!task.value) return ''
  const done = task.value.completedTargets + task.value.skippedTargets
  return `${done}/${task.value.totalTargets}`
})

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
    'A+': '#10b981', A: '#22c55e', B: '#3b82f6', C: '#f59e0b', D: '#ef4444', F: '#dc2626',
  }
  return c[grade] || '#6b7280'
}

// ==================== Data Loading ====================

async function loadData() {
  loading.value = true
  try {
    task.value = await store.loadTask(taskId)
    submissions.value = await store.loadSubmissions(taskId)

    // Load section tree from project's root section
    if (task.value) {
      await store.loadSectionConditions(task.value.projectId)
      try {
        const project = await getProject(task.value.projectId)
        if (project.rootSectionId) {
          rootSectionId.value = project.rootSectionId
          allSections.value = await inspTemplateApi.getSections(project.rootSectionId)
        }
      } catch { /* ignore section tree load failure */ }
    }

    // Auto-select first root target
    if (rootTargets.value.length > 0 && selectedRootTargetId.value === null) {
      selectedRootTargetId.value = rootTargets.value[0].id
    }

    // Auto-select first section tab
    if (firstLevelSections.value.length > 0 && !activeSectionId.value) {
      activeSectionId.value = firstLevelSections.value[0].id
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function selectSubmission(sub: InspSubmission) {
  selectedSubmission.value = sub
  collapsedSections.value.clear()
  try {
    const list = await store.loadDetails(sub.id)
    details.value = list
    scoring.initInputsFromDetails(list)
  } catch (e: any) {
    ElMessage.error(e.message || '加载明细失败')
  }
}

async function reloadSubmissions(selectId?: number) {
  const updatedSubs = await store.loadSubmissions(taskId)
  submissions.value = updatedSubs
  task.value = await store.loadTask(taskId)
  if (selectId) {
    const updated = updatedSubs.find(s => s.id === selectId)
    if (updated) selectedSubmission.value = updated
  }
}

// ==================== Task Lifecycle ====================

async function handleStartTask() {
  try {
    await store.startTask(taskId)
    ElMessage.success('任务已开始')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleSubmitTask() {
  try {
    await ElMessageBox.confirm('提交此检查任务?', '确认', { type: 'warning' })
    await store.submitTask(taskId)
    ElMessage.success('任务已提交')
    loadData()
  } catch { /* cancelled */ }
}

async function handleCompleteSubmission() {
  if (!selectedSubmission.value) return
  const s = scoring.scoreSummary.value
  const unscored = s.total - s.scored
  if (unscored > 0) {
    ElMessage.warning(`还有 ${unscored} 项未评分`)
    return
  }
  for (const d of scoring.scoreableDetails.value) {
    const err = scoring.validateDetail(d)
    if (err) {
      ElMessage.warning(`${d.itemName}: ${err}`)
      return
    }
  }
  try {
    const grade = getGrade(s.finalScore)
    await store.completeSubmission(selectedSubmission.value.id, {
      baseScore: s.baseScore,
      finalScore: s.finalScore,
      deductionTotal: s.deductions,
      bonusTotal: s.bonuses,
      scoreBreakdown: JSON.stringify({
        passCount: s.passCount, failCount: s.failCount,
        deductions: s.deductions, bonuses: s.bonuses, flagged: s.flagged,
      }),
      grade,
      passed: s.finalScore >= 60,
    })
    ElMessage.success(`完成! 得分: ${s.finalScore} (${grade})`)
    await reloadSubmissions(selectedSubmission.value.id)
    details.value = await store.loadDetails(selectedSubmission.value.id)
  } catch (e: any) {
    ElMessage.error(e.message || '完成失败')
  }
}

async function handleSaveDraft() {
  if (!selectedSubmission.value) return
  try {
    // Save the current form data as a JSON string
    const formData = JSON.stringify({
      numberInputs: scoring.numberInputs.value,
      selectInputs: scoring.selectInputs.value,
      multiInputs: scoring.multiInputs.value,
      remarkInputs: scoring.remarkInputs.value,
    })
    await store.saveFormData(selectedSubmission.value.id, { formData })
    ElMessage.success('草稿已保存')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

// ==================== Submission Actions ====================

async function handleLockSubmission(sub: InspSubmission) {
  try {
    await store.lockSubmission(sub.id)
    ElMessage.success('已锁定')
    await reloadSubmissions(sub.id)
  } catch (e: any) {
    ElMessage.error(e.message || '锁定失败')
  }
}

async function handleStartFilling(sub: InspSubmission) {
  try {
    await store.startFillingSubmission(sub.id)
    ElMessage.success('开始打分')
    await reloadSubmissions(sub.id)
    await selectSubmission({ ...sub, status: 'IN_PROGRESS' } as InspSubmission)
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleSkipSubmission(sub: InspSubmission) {
  try {
    await ElMessageBox.confirm('确定跳过此对象?', '确认', { type: 'warning' })
    await store.skipSubmission(sub.id)
    ElMessage.success('已跳过')
    await reloadSubmissions()
    selectedSubmission.value = null
    details.value = []
  } catch { /* cancelled */ }
}

function goBack() {
  router.push('/inspection/v7/tasks')
}

// ==================== ScoringItemRow Event Handlers ====================

function onUpdateNumberInput(detailId: number, value: number) {
  scoring.numberInputs.value[detailId] = value
}

function onUpdateMultiInput(detailId: number, key: string, value: number) {
  if (!scoring.multiInputs.value[detailId]) {
    scoring.multiInputs.value[detailId] = {}
  }
  scoring.multiInputs.value[detailId][key] = value
}

function onUpdateRemarkInput(detailId: number, value: string) {
  scoring.remarkInputs.value[detailId] = value
}

// ==================== PERSON_SCORE support ====================
const personScoreValues = ref<Record<number, Record<number, number>>>({})

function onPersonScoreUpdate(detailId: number, val: Record<number, number>) {
  personScoreValues.value[detailId] = val
  const detail = details.value.find(d => d.id === detailId)
  if (detail) {
    scoring.numberInputs.value[detailId] = Object.values(val).length > 0
      ? Object.values(val).reduce((a, b) => a + b, 0) / Object.values(val).length
      : 0
  }
}

// ==================== Watchers ====================

// When root target changes, reset section tab and selection
watch(selectedRootTargetId, () => {
  selectedSubmission.value = null
  details.value = []
})

onMounted(() => loadData())
</script>

<template>
  <div class="flex flex-col h-full bg-gray-50" v-loading="loading">
    <!-- ===== Top Bar ===== -->
    <div class="flex items-center justify-between px-5 py-3 bg-white border-b shadow-sm">
      <div class="flex items-center gap-3">
        <button
          class="flex items-center justify-center w-8 h-8 rounded-lg text-gray-500 hover:bg-gray-100 hover:text-gray-700 transition-colors"
          @click="goBack"
        >
          <ArrowLeft class="w-4 h-4" />
        </button>
        <div>
          <div class="flex items-center gap-2">
            <h1 class="text-base font-semibold text-gray-900">
              {{ task?.taskCode || '...' }}
            </h1>
            <el-tag
              v-if="task"
              :type="(TaskStatusConfig[task.status as TaskStatus]?.type as any)"
              size="small"
              effect="plain"
            >
              {{ TaskStatusConfig[task.status as TaskStatus]?.label }}
            </el-tag>
          </div>
          <p v-if="task" class="text-xs text-gray-400 mt-0.5">
            {{ task.taskDate }} | {{ task.inspectorName || '-' }}
          </p>
        </div>
      </div>
      <div class="flex items-center gap-2" v-if="task">
        <el-button v-if="task.status === 'CLAIMED'" type="primary" size="small" @click="handleStartTask">
          <Play class="w-3.5 h-3.5 mr-1" />开始检查
        </el-button>
        <el-button v-if="task.status === 'IN_PROGRESS'" type="warning" size="small" @click="handleSubmitTask">
          <Send class="w-3.5 h-3.5 mr-1" />提交任务
        </el-button>
      </div>
    </div>

    <!-- ===== Root Target Bar ===== -->
    <div
      v-if="rootTargets.length > 1"
      class="flex items-center gap-3 px-5 py-2.5 bg-white border-b"
    >
      <span class="text-xs text-gray-400 shrink-0">根目标:</span>
      <div class="flex items-center gap-1.5 flex-wrap">
        <button
          v-for="rt in rootTargets"
          :key="rt.id ?? 'null'"
          class="inline-flex items-center gap-1.5 px-3 py-1 text-xs font-medium rounded-full border transition-all duration-150"
          :class="selectedRootTargetId === rt.id
            ? 'bg-blue-500 text-white border-blue-500 shadow-sm'
            : 'bg-white text-gray-600 border-gray-200 hover:border-blue-300 hover:text-blue-600'"
          @click="selectedRootTargetId = rt.id"
        >
          <span>{{ rt.name }}</span>
          <span
            class="inline-flex items-center justify-center min-w-[18px] h-[18px] px-1 rounded-full text-[10px] font-bold"
            :class="selectedRootTargetId === rt.id
              ? 'bg-blue-400/40 text-white'
              : 'bg-gray-100 text-gray-500'"
          >{{ rt.completedCount }}/{{ rt.count }}</span>
        </button>
      </div>
      <div class="ml-auto text-xs text-gray-500 shrink-0">
        进度 {{ progressText }}
      </div>
    </div>
    <!-- Single root target or no root targets: just show progress -->
    <div v-else class="flex items-center justify-between px-5 py-2 bg-white border-b">
      <span v-if="rootTargets.length === 1" class="text-xs text-gray-500">
        {{ rootTargets[0].name }}
      </span>
      <span v-else class="text-xs text-gray-400">--</span>
      <span class="text-xs text-gray-500">进度 {{ progressText }}</span>
    </div>

    <!-- ===== Section Tabs ===== -->
    <div v-if="firstLevelSections.length > 0" class="bg-white border-b">
      <div class="flex items-center px-5 gap-0.5 overflow-x-auto">
        <button
          v-for="section in firstLevelSections"
          :key="section.id"
          class="relative px-4 py-2.5 text-sm font-medium whitespace-nowrap transition-colors"
          :class="activeSectionTab === section.id
            ? 'text-blue-600'
            : 'text-gray-500 hover:text-gray-700'"
          @click="activeSectionTab = section.id"
        >
          {{ section.sectionName }}
          <!-- Active indicator -->
          <span
            v-if="activeSectionTab === section.id"
            class="absolute bottom-0 left-2 right-2 h-0.5 bg-blue-500 rounded-full"
          />
        </button>
      </div>
    </div>

    <!-- ===== Main Content: Left sidebar + Right form ===== -->
    <div class="flex flex-1 min-h-0">
      <!-- Left: Target List -->
      <div class="w-64 shrink-0 bg-white border-r flex flex-col">
        <div class="px-3 py-2.5 border-b bg-gray-50/80">
          <span class="text-xs font-semibold text-gray-500 uppercase tracking-wide">
            检查对象 ({{ filteredSubmissions.length }})
          </span>
        </div>
        <div class="flex-1 overflow-y-auto">
          <div
            v-for="sub in filteredSubmissions"
            :key="sub.id"
            class="px-3 py-2.5 cursor-pointer border-l-2 transition-all duration-150"
            :class="selectedSubmission?.id === sub.id
              ? 'border-l-blue-500 bg-blue-50/70'
              : 'border-l-transparent hover:bg-gray-50'"
            @click="selectSubmission(sub)"
          >
            <div class="flex items-center justify-between gap-2">
              <span
                class="text-sm font-medium truncate"
                :class="selectedSubmission?.id === sub.id ? 'text-blue-700' : 'text-gray-700'"
              >{{ sub.targetName }}</span>
              <span
                class="shrink-0 w-1.5 h-1.5 rounded-full"
                :class="{
                  'bg-green-400': sub.status === 'COMPLETED',
                  'bg-blue-400': sub.status === 'IN_PROGRESS',
                  'bg-yellow-400': sub.status === 'SKIPPED',
                  'bg-gray-300': sub.status === 'PENDING' || sub.status === 'LOCKED',
                }"
              />
            </div>
            <div class="text-xs text-gray-400 mt-0.5">
              <template v-if="sub.finalScore != null">
                <span class="font-semibold" :style="{ color: getGradeColor(sub.grade || '') }">
                  {{ sub.finalScore }}分
                </span>
                <span class="ml-1 opacity-70">{{ sub.grade }}</span>
              </template>
              <template v-else>
                {{ SubmissionStatusConfig[sub.status as SubmissionStatus]?.label }}
              </template>
            </div>
          </div>
          <div
            v-if="filteredSubmissions.length === 0"
            class="px-4 py-8 text-center text-sm text-gray-400"
          >
            暂无检查对象
          </div>
        </div>
      </div>

      <!-- Right: Scoring Panel -->
      <div class="flex-1 flex flex-col min-w-0">
        <template v-if="selectedSubmission">
          <!-- Submission Header -->
          <div class="flex items-center justify-between px-5 py-3 bg-white border-b">
            <div class="flex items-center gap-3 min-w-0">
              <h2 class="text-base font-semibold text-gray-800 truncate">
                <span v-if="activeSectionId" class="text-gray-400 font-normal">
                  {{ allSections.find(s => s.id === activeSectionId)?.sectionName }} &gt;
                </span>
                {{ selectedSubmission.targetName }}
              </h2>
            </div>
            <div class="flex items-center gap-2 shrink-0">
              <!-- Before scoring: action buttons -->
              <template v-if="selectedSubmission.status === 'PENDING' || selectedSubmission.status === 'LOCKED'">
                <el-button
                  v-if="selectedSubmission.status === 'PENDING'"
                  size="small" @click="handleLockSubmission(selectedSubmission)"
                ><Lock class="w-3.5 h-3.5 mr-1" />锁定</el-button>
                <el-button size="small" type="primary" @click="handleStartFilling(selectedSubmission)">
                  <Play class="w-3.5 h-3.5 mr-1" />开始打分
                </el-button>
                <el-button size="small" @click="handleSkipSubmission(selectedSubmission)">
                  <SkipForward class="w-3.5 h-3.5 mr-1" />跳过
                </el-button>
              </template>
              <!-- During scoring: quick actions dropdown -->
              <template v-if="isEditable">
                <el-dropdown trigger="click" size="small">
                  <el-button size="small">
                    快捷操作 <el-icon class="ml-1"><arrow-down /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="scoring.markAllPass">全部通过 (PASS_FAIL)</el-dropdown-item>
                      <el-dropdown-item @click="scoring.markAllDeductionZero">不扣分 (DEDUCTION)</el-dropdown-item>
                      <el-dropdown-item @click="scoring.markAllDirectPerfect">满分 (DIRECT/LEVEL等)</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </div>
          </div>

          <!-- Score Summary Bar (visible when in scoring or completed) -->
          <div
            v-if="isEditable || selectedSubmission.status === 'COMPLETED'"
            class="flex items-center gap-5 px-5 py-2 bg-white border-b"
          >
            <div class="flex items-baseline gap-1">
              <span class="text-2xl font-bold tabular-nums" :style="{ color: getGradeColor(getGrade(scoring.scoreSummary.value.finalScore)) }">
                {{ scoring.scoreSummary.value.finalScore }}
              </span>
              <span class="text-sm font-semibold" :style="{ color: getGradeColor(getGrade(scoring.scoreSummary.value.finalScore)) }">
                {{ getGrade(scoring.scoreSummary.value.finalScore) }}
              </span>
            </div>
            <div class="h-6 w-px bg-gray-200" />
            <div class="flex items-center gap-4 text-xs text-gray-500">
              <span class="text-green-600">
                <Check class="w-3 h-3 inline -mt-px" /> {{ scoring.scoreSummary.value.passCount }}
              </span>
              <span class="text-red-500">
                <X class="w-3 h-3 inline -mt-px" /> {{ scoring.scoreSummary.value.failCount }}
              </span>
              <span class="text-red-400">-{{ scoring.scoreSummary.value.deductions }}</span>
              <span v-if="scoring.scoreSummary.value.bonuses > 0" class="text-emerald-500">+{{ scoring.scoreSummary.value.bonuses }}</span>
              <span v-if="scoring.scoreSummary.value.flagged > 0" class="text-orange-500">
                <AlertTriangle class="w-3 h-3 inline -mt-px" /> {{ scoring.scoreSummary.value.flagged }}
              </span>
            </div>
            <div class="ml-auto text-xs text-gray-400">
              {{ scoring.scoreSummary.value.scored }}/{{ scoring.scoreSummary.value.total }} 已评分
            </div>
          </div>

          <!-- Scoring Form -->
          <div class="flex-1 overflow-y-auto px-5 py-4">
            <div v-if="formGroups.length > 0" class="space-y-5">
              <div v-for="(group, gi) in formGroups" :key="gi">
                <!-- Sub-section collapsible header -->
                <div
                  v-if="group.isSubSection"
                  class="flex items-center gap-2 px-3 py-2 bg-gray-100 rounded-lg cursor-pointer select-none mb-2"
                  @click="toggleSectionCollapse(group.sectionName)"
                >
                  <component
                    :is="isSectionCollapsed(group.sectionName) ? ChevronRight : ChevronDown"
                    class="w-4 h-4 text-gray-400"
                  />
                  <span class="text-sm font-semibold text-gray-600">{{ group.sectionName }}</span>
                  <span class="text-xs text-gray-400">({{ group.regularItems.length + group.personScoreItems.length + group.violationRecordItems.length }}项)</span>
                </div>

                <!-- Non sub-section: just a subtle label if not the only group -->
                <div
                  v-else-if="formGroups.length > 1"
                  class="flex items-center gap-2 mb-2 pb-1 border-b border-gray-200"
                >
                  <span class="text-sm font-semibold text-gray-700">{{ group.sectionName }}</span>
                  <span class="text-xs text-gray-400">({{ group.regularItems.length }}项)</span>
                </div>

                <!-- Content (collapsible for sub-sections) -->
                <div v-if="!group.isSubSection || !isSectionCollapsed(group.sectionName)" class="space-y-4">
                  <!-- Regular scoring items -->
                  <div v-if="group.regularItems.length > 0" class="space-y-1">
                    <ScoringItemRow
                      v-for="detail in group.regularItems"
                      :key="detail.id"
                      :detail="detail"
                      :editable="isEditable"
                      :resolve-mode="scoring.resolveMode"
                      :get-scoring-params="scoring.getScoringParams"
                      :get-max-score-for-mode="scoring.getMaxScoreForMode"
                      :format-score="scoring.formatScore"
                      :score-color="scoring.scoreColor"
                      :row-bg="scoring.rowBg"
                      :needs-remark="scoring.needsRemark"
                      :is-non-scoring="scoring.isNonScoring"
                      :scoring-in-progress="scoring.scoringInProgress.value"
                      :number-inputs="scoring.numberInputs.value"
                      :remark-inputs="scoring.remarkInputs.value"
                      :select-inputs="scoring.selectInputs.value"
                      :multi-inputs="scoring.multiInputs.value"
                      :mode-label-map="MODE_LABEL"
                      :mode-tag-type-map="MODE_TAG_TYPE"
                      :is-disabled="conditionLogic.isItemDisabled(detail)"
                      @pass-fail="scoring.handlePassFail"
                      @deduction="scoring.handleDeduction"
                      @addition="scoring.handleAddition"
                      @direct="scoring.handleDirect"
                      @level="scoring.handleLevel"
                      @score-table="scoring.handleScoreTable"
                      @cumulative="scoring.handleCumulative"
                      @tiered-deduction="scoring.handleTieredDeduction"
                      @rating-scale="scoring.handleRatingScale"
                      @weighted-multi="scoring.handleWeightedMulti"
                      @risk-matrix="scoring.handleRiskMatrix"
                      @threshold="scoring.handleThreshold"
                      @formula="scoring.handleFormula"
                      @toggle-flag="scoring.toggleFlag"
                      @remark-change="scoring.handleRemarkChange"
                      @update:number-input="onUpdateNumberInput"
                      @update:multi-input="onUpdateMultiInput"
                      @update:remark-input="onUpdateRemarkInput"
                    />
                  </div>

                  <!-- PERSON_SCORE items -->
                  <div v-for="detail in group.personScoreItems" :key="'ps-' + detail.id">
                    <div class="flex items-center gap-2 mb-2 pb-1 border-b border-gray-200">
                      <span class="text-sm font-semibold text-gray-700">{{ detail.itemName }}</span>
                      <el-tag size="small" type="info" effect="plain">逐人评分</el-tag>
                    </div>
                    <PersonScoreGrid
                      v-if="selectedSubmission"
                      :target-type="selectedSubmission.targetType"
                      :target-id="selectedSubmission.targetId"
                      :detail-id="detail.id"
                      :disabled="!isEditable"
                    />
                  </div>

                  <!-- VIOLATION_RECORD items -->
                  <div v-for="detail in group.violationRecordItems" :key="'vr-' + detail.id">
                    <div class="flex items-center gap-2 mb-2 pb-1 border-b border-gray-200">
                      <span class="text-sm font-semibold text-gray-700">{{ detail.itemName }}</span>
                      <el-tag size="small" type="warning" effect="plain">违纪记录</el-tag>
                    </div>
                    <ViolationRecordInput
                      v-if="selectedSubmission"
                      :submission-id="selectedSubmission.id"
                      :detail-id="detail.id"
                      :section-id="detail.sectionId || 0"
                      :item-id="detail.templateItemId"
                      :disabled="!isEditable"
                    />
                  </div>
                </div>
              </div>
            </div>

            <!-- Empty state for scoring form -->
            <div
              v-else-if="details.length === 0 && selectedSubmission.status !== 'PENDING'"
              class="text-center text-sm text-gray-400 py-12"
            >
              暂无检查项
            </div>

            <!-- Pre-scoring prompt -->
            <div
              v-else-if="selectedSubmission.status === 'PENDING' || selectedSubmission.status === 'LOCKED'"
              class="flex flex-col items-center justify-center py-16 text-gray-400"
            >
              <Play class="w-10 h-10 mb-3 text-gray-300" />
              <p class="text-sm">点击上方「开始打分」按钮开始评分</p>
            </div>
          </div>

          <!-- Bottom Bar: Navigation + Actions -->
          <div class="flex items-center justify-between px-5 py-3 bg-white border-t">
            <div class="flex items-center gap-2">
              <el-button size="small" :disabled="!hasPrevTarget" @click="goToPrevTarget">
                <ChevronLeft class="w-3.5 h-3.5 mr-1" />上一个
              </el-button>
              <span class="text-xs text-gray-400 tabular-nums">
                {{ currentTargetIndex + 1 }} / {{ filteredSubmissions.length }}
              </span>
              <el-button size="small" :disabled="!hasNextTarget" @click="goToNextTarget">
                下一个<ChevronRight class="w-3.5 h-3.5 ml-1" />
              </el-button>
            </div>
            <div class="flex items-center gap-2">
              <el-button
                v-if="isEditable"
                size="small"
                @click="handleSaveDraft"
              >
                保存草稿
              </el-button>
              <el-button
                v-if="isEditable"
                type="primary"
                size="small"
                :disabled="scoring.scoreSummary.value.total !== scoring.scoreSummary.value.scored"
                @click="handleCompleteSubmission"
              >
                <Check class="w-3.5 h-3.5 mr-1" />
                提交此目标 ({{ scoring.scoreSummary.value.scored }}/{{ scoring.scoreSummary.value.total }})
              </el-button>
            </div>
          </div>
        </template>

        <!-- No submission selected -->
        <template v-else>
          <div class="flex-1 flex items-center justify-center">
            <div class="text-center text-gray-400">
              <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-gray-100 flex items-center justify-center">
                <ChevronLeft class="w-6 h-6" />
              </div>
              <p class="text-sm">请在左侧选择检查对象开始打分</p>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
