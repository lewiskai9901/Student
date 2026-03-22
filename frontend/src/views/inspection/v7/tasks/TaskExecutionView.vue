<script setup lang="ts">
/**
 * TaskExecutionView — V7 打分界面（重写版）
 *
 * 布局：
 *   顶部导航栏：返回 + 任务信息 + 任务操作
 *   主体两栏：
 *     左：分区导航（section nav）+ 完成进度指示
 *     右：顶部目标选择器 + 字段列表（按 scoringMode 渲染不同控件）+ 底部实时得分
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Play, Send, Lock, SkipForward, Check, X,
  AlertTriangle, ChevronLeft, ChevronRight, Plus, Minus,
  Flag, Star,
} from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/insp/inspExecutionStore'
import {
  TaskStatusConfig, SubmissionStatusConfig, ScoringModeConfig, TargetTypeConfig,
  type TaskStatus, type SubmissionStatus, type ScoringMode,
} from '@/types/insp/enums'
import type { InspTask, InspSubmission, SubmissionDetail } from '@/types/insp/project'
import type { TemplateSection } from '@/types/insp/template'
import { getProject } from '@/api/insp/project'
import { http } from '@/utils/request'
import { inspTemplateApi } from '@/api/insp/template'
import ViolationRecordInput from './components/ViolationRecordInput.vue'
import PersonScoreGrid from './components/PersonScoreGrid.vue'

const route = useRoute()
const router = useRouter()
const store = useInspExecutionStore()
const taskId = Number(route.params.id)

// ==================== Core State ====================

const loading = ref(false)
const detailLoading = ref(false)
const task = ref<InspTask | null>(null)
const submissions = ref<InspSubmission[]>([])
const selectedSubmission = ref<InspSubmission | null>(null)
const details = ref<SubmissionDetail[]>([])
const allSections = ref<TemplateSection[]>([])
const rootSectionId = ref<number | null>(null)

// Active first-level section
const activeSectionId = ref<number | null>(null)

// Per-detail input state
const numberInputs = ref<Record<number, number>>({})
const selectInputs = ref<Record<number, string>>({})
const textInputs = ref<Record<number, string>>({})

const isEditable = computed(() => selectedSubmission.value?.status === 'IN_PROGRESS')

// ==================== Section Nav (left panel) ====================

/** First-level sections = direct children of root */
const firstLevelSections = computed(() =>
  allSections.value.filter(s => Number(s.parentSectionId) === Number(rootSectionId.value)),
)

/** For each first-level section: how many submissions belong to it? */
function getSectionSubmissions(sectionId: number): InspSubmission[] {
  return submissions.value.filter(s => Number(s.sectionId) === Number(sectionId))
}

function getSectionProgress(sectionId: number): { done: number; total: number } {
  const subs = getSectionSubmissions(sectionId)
  const done = subs.filter(s => s.status === 'COMPLETED' || s.status === 'SKIPPED').length
  return { done, total: subs.length }
}

/** Status icon for section: ● done (green) ◐ in-progress (blue) ○ not started (gray) */
function getSectionIcon(sectionId: number): 'done' | 'partial' | 'empty' {
  const { done, total } = getSectionProgress(sectionId)
  if (total === 0) return 'empty'
  if (done === total) return 'done'
  if (done > 0) return 'partial'
  return 'empty'
}

// ==================== Targets in right panel ====================

/** Submissions for the currently active section */
const sectionSubmissions = computed(() => {
  if (!activeSectionId.value) return submissions.value
  return submissions.value.filter(s => s.sectionId === activeSectionId.value)
})

const currentTargetIndex = computed(() => {
  if (!selectedSubmission.value) return -1
  return sectionSubmissions.value.findIndex(s => s.id === selectedSubmission.value!.id)
})

const hasPrev = computed(() => currentTargetIndex.value > 0)
const hasNext = computed(() =>
  currentTargetIndex.value >= 0 && currentTargetIndex.value < sectionSubmissions.value.length - 1,
)

function goToPrev() {
  if (hasPrev.value) selectSubmission(sectionSubmissions.value[currentTargetIndex.value - 1])
}
function goToNext() {
  if (hasNext.value) selectSubmission(sectionSubmissions.value[currentTargetIndex.value + 1])
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

/** Generate deduction buttons based on scoringConfig.maxDeduction */
function getDeductionSteps(detail: SubmissionDetail): number[] {
  const cfg = parseScoringConfig(detail)
  const max = cfg.maxDeduction ?? cfg.maxScore ?? 5
  const step = cfg.step ?? 1
  const steps: number[] = [0]
  for (let i = step; i <= max; i += step) steps.push(-i)
  return steps
}

/** Generate addition buttons */
function getAdditionSteps(detail: SubmissionDetail): number[] {
  const cfg = parseScoringConfig(detail)
  const max = cfg.maxAddition ?? cfg.maxBonus ?? cfg.maxScore ?? 5
  const step = cfg.step ?? 1
  const steps: number[] = []
  for (let i = 0; i <= max; i += step) steps.push(i)
  return steps
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
    await store.updateDetailResponse(detail.id, {
      responseValue: String(val),
      scoringMode: 'DEDUCTION',
      score: val,
    })
  } catch { /* silent */ }
}

async function handlePassFail(detail: SubmissionDetail, val: 'PASS' | 'FAIL') {
  selectInputs.value[detail.id] = val
  const cfg = parseScoringConfig(detail)
  const score = val === 'PASS' ? (cfg.passScore ?? 0) : (cfg.failScore ?? -5)
  try {
    await store.updateDetailResponse(detail.id, {
      responseValue: val,
      scoringMode: 'PASS_FAIL',
      score,
    })
  } catch { /* silent */ }
}

async function handleGradeSelect(detail: SubmissionDetail, label: string, score: number) {
  selectInputs.value[detail.id] = label
  numberInputs.value[detail.id] = score
  try {
    await store.updateDetailResponse(detail.id, {
      responseValue: label,
      scoringMode: detail.scoringMode!,
      score,
    })
  } catch { /* silent */ }
}

async function handleAdditionSelect(detail: SubmissionDetail, val: number) {
  numberInputs.value[detail.id] = val
  try {
    await store.updateDetailResponse(detail.id, {
      responseValue: String(val),
      scoringMode: 'ADDITION',
      score: val,
    })
  } catch { /* silent */ }
}

async function handleDirectInput(detail: SubmissionDetail, val: number) {
  numberInputs.value[detail.id] = val
  try {
    await store.updateDetailResponse(detail.id, {
      responseValue: String(val),
      scoringMode: 'DIRECT',
      score: val,
    })
  } catch { /* silent */ }
}

async function handleRatingScale(detail: SubmissionDetail, stars: number) {
  numberInputs.value[detail.id] = stars
  const cfg = parseScoringConfig(detail)
  const maxScore = cfg.maxScore ?? 100
  const maxStars = getRatingMax(detail)
  const score = Math.round((stars / maxStars) * maxScore)
  try {
    await store.updateDetailResponse(detail.id, {
      responseValue: String(stars),
      scoringMode: 'RATING_SCALE',
      score,
    })
  } catch { /* silent */ }
}

function handleCumulativeChange(detail: SubmissionDetail, delta: number) {
  const cur = numberInputs.value[detail.id] ?? 0
  const next = Math.max(0, cur + delta)
  numberInputs.value[detail.id] = next
  const cfg = getCumulativeConfig(detail)
  const score = next * cfg.scorePerUnit
  store.updateDetailResponse(detail.id, {
    responseValue: String(next),
    scoringMode: 'CUMULATIVE',
    score,
  }).catch(() => {})
}

async function handleTextInput(detail: SubmissionDetail, val: string) {
  textInputs.value[detail.id] = val
}

async function saveTextInput(detail: SubmissionDetail) {
  const val = textInputs.value[detail.id] ?? ''
  try {
    await store.updateDetailResponse(detail.id, {
      responseValue: val,
      score: undefined,
    })
  } catch { /* silent */ }
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

// ==================== Section active section targetType ====================

const activeSectionInfo = computed(() =>
  allSections.value.find(s => s.id === activeSectionId.value) ?? null,
)

// ==================== Data Loading ====================

async function loadData() {
  loading.value = true
  try {
    task.value = await store.loadTask(taskId)
    submissions.value = await store.loadSubmissions(taskId)

    if (task.value) {
      try {
        const project = await getProject(task.value.projectId)
        // 多模板项目：从计划获取 rootSectionId，回退到项目的
        let rsi = project.rootSectionId
        if (!rsi && task.value.planId) {
          try {
            const plans = await http.get<any[]>(`/v7/insp/projects/${task.value.projectId}/plans`)
            const plan = plans?.find((p: any) => Number(p.id) === Number(task.value!.planId)) || plans?.[0]
            if (plan?.rootSectionId) rsi = plan.rootSectionId
          } catch {}
        }
        // 再回退：从 submissions 的 sectionId 找根分区
        if (!rsi && submissions.value.length > 0) {
          const secId = submissions.value[0].sectionId
          try {
            const sec = await inspTemplateApi.getSection(secId)
            rsi = sec.parentSectionId || secId
          } catch {}
        }
        if (rsi) {
          rootSectionId.value = Number(rsi)
          try {
            allSections.value = await inspTemplateApi.getSections(Number(rsi))
          } catch {
            // 如果 getSections(tree) 失败，用 getChildSections
            try {
              allSections.value = await inspTemplateApi.getChildSections(Number(rsi))
            } catch {}
          }
        }
      } catch { /* ignore */ }
    }

    // Auto-select first section
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
  numberInputs.value = {}
  selectInputs.value = {}
  textInputs.value = {}
  detailLoading.value = true
  try {
    const list = await store.loadDetails(sub.id)
    details.value = list
    initInputs(list)
  } catch (e: any) {
    ElMessage.error(e.message || '加载明细失败')
  } finally {
    detailLoading.value = false
  }
}

async function reloadAll(keepSelected = true) {
  const prevId = selectedSubmission.value?.id
  submissions.value = await store.loadSubmissions(taskId)
  task.value = await store.loadTask(taskId)
  if (keepSelected && prevId) {
    const updated = submissions.value.find(s => s.id === prevId)
    if (updated) selectedSubmission.value = updated
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
    await ElMessageBox.confirm('确认提交此检查任务？提交后不可修改。', '提交确认', { type: 'warning' })
    await store.submitTask(taskId)
    ElMessage.success('任务已提交')
    await reloadAll(false)
  } catch { /* cancelled */ }
}

// ==================== Submission Lifecycle ====================

async function handleStartFilling(sub: InspSubmission) {
  try {
    await store.startFillingSubmission(sub.id)
    ElMessage.success('开始打分')
    await reloadAll()
    const updated = submissions.value.find(s => s.id === sub.id)
    if (updated) await selectSubmission(updated)
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleLock(sub: InspSubmission) {
  try {
    await store.lockSubmission(sub.id)
    ElMessage.success('已锁定')
    await reloadAll()
  } catch (e: any) {
    ElMessage.error(e.message || '锁定失败')
  }
}

async function handleSkip(sub: InspSubmission) {
  try {
    await ElMessageBox.confirm('确定跳过此检查对象？', '确认', { type: 'warning' })
    await store.skipSubmission(sub.id)
    ElMessage.success('已跳过')
    await reloadAll()
    selectedSubmission.value = null
    details.value = []
  } catch { /* cancelled */ }
}

async function handleComplete() {
  if (!selectedSubmission.value) return
  const s = scoreSummary.value
  if (s.scored < s.total) {
    ElMessage.warning(`还有 ${s.total - s.scored} 项未评分`)
    return
  }
  const grade = getGrade(s.finalScore)
  try {
    await store.completeSubmission(selectedSubmission.value.id, {
      baseScore: s.finalScore,
      finalScore: s.finalScore,
      deductionTotal: s.deductions,
      bonusTotal: s.bonuses,
      scoreBreakdown: JSON.stringify({
        passCount: s.passCount,
        failCount: s.failCount,
        deductions: s.deductions,
        bonuses: s.bonuses,
      }),
      grade,
      passed: s.finalScore >= 60,
    })
    ElMessage.success(`完成！得分 ${s.finalScore}（${grade}）`)
    await reloadAll()
  } catch (e: any) {
    ElMessage.error(e.message || '完成失败')
  }
}

function goBack() {
  router.push('/inspection/v7/tasks')
}

// ==================== Computed: items split by type ====================

const regularItems = computed(() =>
  details.value.filter(d => d.itemType !== 'PERSON_SCORE' && d.itemType !== 'VIOLATION_RECORD'),
)
const personScoreItems = computed(() =>
  details.value.filter(d => d.itemType === 'PERSON_SCORE'),
)
const violationItems = computed(() =>
  details.value.filter(d => d.itemType === 'VIOLATION_RECORD'),
)

const progressText = computed(() => {
  if (!task.value) return ''
  const done = task.value.completedTargets + task.value.skippedTargets
  return `${done}/${task.value.totalTargets}`
})

// ==================== Watchers ====================

watch(activeSectionId, () => {
  selectedSubmission.value = null
  details.value = []
  numberInputs.value = {}
  selectInputs.value = {}
  textInputs.value = {}
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
      </div>
    </div>

    <!-- ===== MAIN BODY ===== -->
    <div class="main-body">

      <!-- ===== LEFT: SECTION NAV ===== -->
      <div class="section-nav">
        <div class="section-nav-header">分区导航</div>
        <div
          v-for="sec in firstLevelSections"
          :key="sec.id"
          class="section-item"
          :class="{ active: activeSectionId === sec.id }"
          @click="activeSectionId = sec.id"
        >
          <!-- Status indicator -->
          <span
            class="sec-dot"
            :class="{
              'sec-dot--done':    getSectionIcon(sec.id) === 'done',
              'sec-dot--partial': getSectionIcon(sec.id) === 'partial',
              'sec-dot--empty':   getSectionIcon(sec.id) === 'empty',
            }"
          />
          <div class="sec-label-wrap">
            <span class="sec-name">{{ sec.sectionName }}</span>
            <span class="sec-prog">
              {{ getSectionProgress(sec.id).done }}/{{ getSectionProgress(sec.id).total }}
            </span>
          </div>
        </div>
        <div v-if="firstLevelSections.length === 0" class="section-empty">
          暂无分区
        </div>
      </div>

      <!-- ===== RIGHT: SCORING PANEL ===== -->
      <div class="scoring-panel">

        <!-- === Section header row === -->
        <div class="section-header-row" v-if="activeSectionInfo">
          <span class="sec-header-name">{{ activeSectionInfo.sectionName }}</span>
          <el-tag
            v-if="activeSectionInfo.targetType"
            size="small"
            type="info"
            effect="plain"
          >
            {{ TargetTypeConfig[activeSectionInfo.targetType]?.label ?? activeSectionInfo.targetType }}
          </el-tag>
        </div>

        <!-- === Target pill selector === -->
        <div class="target-row">
          <div class="target-pills">
            <button
              v-for="sub in sectionSubmissions"
              :key="sub.id"
              class="target-pill"
              :class="{
                'target-pill--active':     selectedSubmission?.id === sub.id,
                'target-pill--completed':  sub.status === 'COMPLETED',
                'target-pill--skipped':    sub.status === 'SKIPPED',
                'target-pill--inprogress': sub.status === 'IN_PROGRESS',
              }"
              @click="selectSubmission(sub)"
            >
              <Check v-if="sub.status === 'COMPLETED'" :size="11" class="pill-check" />
              {{ sub.targetName || sub.rootTargetName || activeSectionInfo?.sectionName || '检查对象' }}
            </button>
            <span v-if="sectionSubmissions.length === 0" class="target-empty-hint">
              本分区暂无检查对象
            </span>
          </div>
          <div class="target-nav">
            <button class="nav-btn" :disabled="!hasPrev" @click="goToPrev">
              <ChevronLeft :size="14" />
            </button>
            <span class="nav-count">
              {{ currentTargetIndex >= 0 ? currentTargetIndex + 1 : '-' }}
              /
              {{ sectionSubmissions.length }}
            </span>
            <button class="nav-btn" :disabled="!hasNext" @click="goToNext">
              <ChevronRight :size="14" />
            </button>
          </div>
        </div>

        <!-- === Scoring form area === -->
        <div class="form-area" v-loading="detailLoading">

          <!-- Empty / pre-start state -->
          <div v-if="!selectedSubmission" class="form-placeholder">
            <ChevronLeft :size="28" class="ph-icon" />
            <p>请在上方选择检查对象开始打分</p>
          </div>

          <!-- Pre-filling prompt -->
          <div
            v-else-if="selectedSubmission.status === 'PENDING' || selectedSubmission.status === 'LOCKED'"
            class="form-prestart"
          >
            <div class="prestart-actions">
              <el-button
                v-if="selectedSubmission.status === 'PENDING'"
                size="small"
                @click="handleLock(selectedSubmission)"
              >
                <Lock :size="13" class="btn-icon" />锁定
              </el-button>
              <el-button type="primary" size="small" @click="handleStartFilling(selectedSubmission)">
                <Play :size="13" class="btn-icon" />开始打分
              </el-button>
              <el-button size="small" @click="handleSkip(selectedSubmission)">
                <SkipForward :size="13" class="btn-icon" />跳过
              </el-button>
            </div>
            <p class="prestart-hint">点击「开始打分」加载检查项</p>
          </div>

          <!-- Actual scoring form -->
          <div v-else class="field-list">

            <!-- Regular scoring items -->
            <div
              v-for="detail in regularItems"
              :key="detail.id"
              class="field-card"
            >
              <div class="field-header">
                <span class="field-name">{{ detail.itemName }}</span>
                <el-tag
                  v-if="detail.scoringMode"
                  size="small"
                  effect="plain"
                  :type="detail.scoringMode === 'DEDUCTION' ? 'danger'
                       : detail.scoringMode === 'ADDITION' ? 'success'
                       : detail.scoringMode === 'PASS_FAIL' ? ''
                       : 'warning'"
                >
                  {{ ScoringModeConfig[detail.scoringMode]?.label ?? detail.scoringMode }}
                </el-tag>
                <el-tag v-else size="small" type="info" effect="plain">采集</el-tag>
              </div>

              <!-- DEDUCTION: button group [0][-1][-2]... -->
              <div v-if="detail.scoringMode === 'DEDUCTION'" class="ctrl-row">
                <button
                  v-for="step in getDeductionSteps(detail)"
                  :key="step"
                  class="score-btn"
                  :class="{
                    'score-btn--active': numberInputs[detail.id] === step,
                    'score-btn--zero':   step === 0,
                    'score-btn--neg':    step < 0,
                  }"
                  :disabled="!isEditable"
                  @click="handleDeductionSelect(detail, step)"
                >
                  {{ step === 0 ? '0' : step }}
                </button>
              </div>

              <!-- ADDITION: button group [0][+1][+2]... -->
              <div v-else-if="detail.scoringMode === 'ADDITION'" class="ctrl-row">
                <button
                  v-for="step in getAdditionSteps(detail)"
                  :key="step"
                  class="score-btn score-btn--pos"
                  :class="{ 'score-btn--active': numberInputs[detail.id] === step }"
                  :disabled="!isEditable"
                  @click="handleAdditionSelect(detail, step)"
                >
                  {{ step === 0 ? '0' : `+${step}` }}
                </button>
              </div>

              <!-- PASS_FAIL: 通过 / 不通过 -->
              <div v-else-if="detail.scoringMode === 'PASS_FAIL'" class="ctrl-row">
                <button
                  class="pf-btn pf-btn--pass"
                  :class="{ 'pf-btn--active': selectInputs[detail.id] === 'PASS' }"
                  :disabled="!isEditable"
                  @click="handlePassFail(detail, 'PASS')"
                >
                  <Check :size="13" /> 通过
                </button>
                <button
                  class="pf-btn pf-btn--fail"
                  :class="{ 'pf-btn--active': selectInputs[detail.id] === 'FAIL' }"
                  :disabled="!isEditable"
                  @click="handlePassFail(detail, 'FAIL')"
                >
                  <X :size="13" /> 不通过
                </button>
              </div>

              <!-- LEVEL / SCORE_TABLE / TIERED_DEDUCTION: pill grade selector -->
              <div
                v-else-if="detail.scoringMode === 'LEVEL' || detail.scoringMode === 'SCORE_TABLE' || detail.scoringMode === 'TIERED_DEDUCTION'"
                class="ctrl-row"
              >
                <button
                  v-for="lv in getGradeLevels(detail)"
                  :key="lv.label"
                  class="grade-btn"
                  :class="{ 'grade-btn--active': selectInputs[detail.id] === lv.label }"
                  :disabled="!isEditable"
                  @click="handleGradeSelect(detail, lv.label, lv.score)"
                >
                  {{ lv.label }}
                  <span class="grade-score">{{ lv.score }}</span>
                </button>
              </div>

              <!-- DIRECT: number input + range hint -->
              <div v-else-if="detail.scoringMode === 'DIRECT'" class="ctrl-row ctrl-row--direct">
                <el-input-number
                  v-model="numberInputs[detail.id]"
                  :min="getDirectRange(detail).min"
                  :max="getDirectRange(detail).max"
                  :disabled="!isEditable"
                  size="small"
                  style="width: 120px"
                  @change="(val: number) => handleDirectInput(detail, val)"
                />
                <span class="range-hint">范围 {{ getDirectRange(detail).min }}–{{ getDirectRange(detail).max }}</span>
              </div>

              <!-- RATING_SCALE: star buttons -->
              <div v-else-if="detail.scoringMode === 'RATING_SCALE'" class="ctrl-row">
                <button
                  v-for="n in getRatingMax(detail)"
                  :key="n"
                  class="star-btn"
                  :class="{ 'star-btn--active': (numberInputs[detail.id] ?? 0) >= n }"
                  :disabled="!isEditable"
                  @click="handleRatingScale(detail, n)"
                >
                  <Star :size="18" />
                </button>
                <span class="star-val" v-if="numberInputs[detail.id] !== undefined">
                  {{ numberInputs[detail.id] }}/{{ getRatingMax(detail) }}
                </span>
              </div>

              <!-- CUMULATIVE: +/- counter -->
              <div v-else-if="detail.scoringMode === 'CUMULATIVE'" class="ctrl-row ctrl-row--cumulative">
                <button class="counter-btn" :disabled="!isEditable" @click="handleCumulativeChange(detail, -1)">
                  <Minus :size="14" />
                </button>
                <span class="counter-val">{{ numberInputs[detail.id] ?? 0 }}</span>
                <button class="counter-btn" :disabled="!isEditable" @click="handleCumulativeChange(detail, 1)">
                  <Plus :size="14" />
                </button>
                <span class="counter-unit">{{ getCumulativeConfig(detail).countLabel }}</span>
              </div>

              <!-- Other scoring modes: show numeric input -->
              <div
                v-else-if="detail.scoringMode && !['DEDUCTION','ADDITION','PASS_FAIL','LEVEL','SCORE_TABLE','TIERED_DEDUCTION','DIRECT','RATING_SCALE','CUMULATIVE'].includes(detail.scoringMode)"
                class="ctrl-row ctrl-row--direct"
              >
                <el-input-number
                  v-model="numberInputs[detail.id]"
                  :min="0"
                  :max="100"
                  :disabled="!isEditable"
                  size="small"
                  style="width: 120px"
                  @change="(val: number) => handleDirectInput(detail, val)"
                />
                <span class="range-hint">{{ ScoringModeConfig[detail.scoringMode!]?.label }}</span>
              </div>

              <!-- Non-scoring capture items -->
              <div v-else-if="!detail.scoringMode" class="ctrl-row ctrl-row--capture">
                <el-input
                  v-model="textInputs[detail.id]"
                  :disabled="!isEditable"
                  size="small"
                  placeholder="请填写..."
                  clearable
                  @blur="saveTextInput(detail)"
                />
              </div>

            </div>
            <!-- /regular items -->

            <!-- PERSON_SCORE items -->
            <div v-for="detail in personScoreItems" :key="'ps-' + detail.id" class="field-card">
              <div class="field-header">
                <span class="field-name">{{ detail.itemName }}</span>
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
            <div v-for="detail in violationItems" :key="'vr-' + detail.id" class="field-card field-card--violation">
              <div class="field-header">
                <span class="field-name">{{ detail.itemName }}</span>
                <el-tag size="small" type="warning" effect="plain">违纪记录</el-tag>
              </div>
              <ViolationRecordInput
                v-if="selectedSubmission"
                :submission-id="selectedSubmission.id"
                :detail-id="detail.id"
                :section-id="detail.sectionId ?? 0"
                :item-id="detail.templateItemId"
                :disabled="!isEditable"
              />
            </div>

            <!-- Empty items state -->
            <div v-if="details.length === 0" class="form-placeholder">
              <p>暂无检查项</p>
            </div>

          </div>
          <!-- /field-list -->
        </div>
        <!-- /form-area -->

        <!-- === Bottom score bar === -->
        <div
          v-if="selectedSubmission && (isEditable || selectedSubmission.status === 'COMPLETED')"
          class="score-bar"
        >
          <div class="score-bar-left">
            <span
              class="score-value"
              :style="{ color: getGradeColor(getGrade(scoreSummary.finalScore)) }"
            >
              {{ scoreSummary.finalScore }}
            </span>
            <span v-if="scoreSummary.deductions > 0" class="score-sep stat-deduct">
              -{{ scoreSummary.deductions }}
            </span>
            <span v-if="scoreSummary.bonuses > 0" class="score-sep stat-bonus">
              +{{ scoreSummary.bonuses }}
            </span>
            <span
              class="score-grade"
              :style="{ color: getGradeColor(getGrade(scoreSummary.finalScore)) }"
            >
              {{ getGrade(scoreSummary.finalScore) }}
            </span>
          </div>
          <div class="score-bar-stats">
            <span v-if="scoreSummary.passCount > 0" class="stat-pass">
              <Check :size="11" /> {{ scoreSummary.passCount }}
            </span>
            <span v-if="scoreSummary.failCount > 0" class="stat-fail">
              <X :size="11" /> {{ scoreSummary.failCount }}
            </span>
            <span v-if="scoreSummary.deductions > 0" class="stat-deduct">
              -{{ scoreSummary.deductions }}
            </span>
            <span v-if="scoreSummary.bonuses > 0" class="stat-bonus">
              +{{ scoreSummary.bonuses }}
            </span>
          </div>
          <div class="score-bar-progress">
            {{ scoreSummary.scored }}/{{ scoreSummary.total }} 已评分
          </div>
          <div class="score-bar-actions" v-if="isEditable">
            <el-button
              type="primary" size="small"
              :disabled="scoreSummary.scored < scoreSummary.total"
              @click="handleComplete"
            >
              <Check :size="13" class="btn-icon" />
              提交 ({{ scoreSummary.scored }}/{{ scoreSummary.total }})
            </el-button>
            <el-button size="small" @click="handleSkip(selectedSubmission!)">
              <SkipForward :size="13" class="btn-icon" />跳过
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

/* ===== Left Section Nav ===== */
.section-nav {
  width: 180px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.section-nav-header {
  padding: 10px 12px 8px;
  font-size: 10px;
  font-weight: 600;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-bottom: 1px solid #f3f4f6;
}

.section-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  cursor: pointer;
  border-left: 2px solid transparent;
  transition: all 0.15s;
}
.section-item:hover { background: #f9fafb; }
.section-item.active {
  border-left-color: #1a6dff;
  background: #eff6ff;
}
.section-item.active .sec-name { color: #1a6dff; font-weight: 600; }

.sec-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.sec-dot--done    { background: #10b981; }
.sec-dot--partial { background: #1a6dff; }
.sec-dot--empty   { background: #d1d5db; }

.sec-label-wrap {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.sec-name {
  font-size: 12px;
  font-weight: 500;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sec-prog {
  font-size: 10px;
  color: #9ca3af;
}

.section-empty {
  padding: 16px 12px;
  font-size: 11px;
  color: #d1d5db;
  text-align: center;
}

/* ===== Right Scoring Panel ===== */
.scoring-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

/* Section header row */
.section-header-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px 8px;
  background: #fff;
  border-bottom: 1px solid #f3f4f6;
  flex-shrink: 0;
}

.sec-header-name {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

/* Target pill row */
.target-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 20px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.target-pills {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  flex: 1;
}

.target-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 20px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  font-size: 12px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.target-pill:hover { border-color: #93c5fd; color: #1a6dff; }

.target-pill--active {
  border-color: #1a6dff;
  background: #eff6ff;
  color: #1a6dff;
  font-weight: 600;
}

.target-pill--completed {
  border-color: #6ee7b7;
  background: #f0fdf4;
  color: #059669;
}
.target-pill--completed.target-pill--active {
  border-color: #10b981;
  background: #d1fae5;
}

.target-pill--inprogress {
  border-color: #93c5fd;
  background: #eff6ff;
  color: #1d4ed8;
}

.target-pill--skipped {
  border-color: #fcd34d;
  color: #b45309;
  background: #fffbeb;
}

.pill-check { flex-shrink: 0; }

.target-empty-hint {
  font-size: 11px;
  color: #d1d5db;
  padding: 4px 0;
}

.target-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
}
.nav-btn:hover:not(:disabled) { border-color: #1a6dff; color: #1a6dff; }
.nav-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.nav-count {
  font-size: 11px;
  color: #9ca3af;
  min-width: 36px;
  text-align: center;
  tabular-nums: true;
}

/* Form area */
.form-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.form-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #d1d5db;
  gap: 8px;
}
.ph-icon { color: #e5e7eb; }
.form-placeholder p { font-size: 12px; }

.form-prestart {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding-top: 40px;
}
.prestart-actions { display: flex; gap: 8px; }
.prestart-hint { font-size: 11px; color: #9ca3af; }

.field-list { display: flex; flex-direction: column; gap: 10px; }

/* Field card */
.field-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px 14px;
  transition: box-shadow 0.15s;
}
.field-card:hover { box-shadow: 0 1px 6px rgba(0,0,0,0.06); }

.field-card--violation {
  background: #fffdf0;
  border-color: #fde68a;
}

.field-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.field-name {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  flex: 1;
}

/* Controls row */
.ctrl-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.ctrl-row--direct {
  align-items: center;
  gap: 10px;
}

.ctrl-row--capture {
  display: block;
}

.ctrl-row--cumulative {
  gap: 8px;
}

.range-hint {
  font-size: 11px;
  color: #9ca3af;
}

/* Score buttons (deduction/addition) */
.score-btn {
  min-width: 38px;
  height: 32px;
  padding: 0 8px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  font-size: 12px;
  font-weight: 500;
  color: #374151;
  cursor: pointer;
  transition: all 0.15s;
}
.score-btn:hover:not(:disabled) { border-color: #1a6dff; color: #1a6dff; }
.score-btn:disabled { opacity: 0.45; cursor: not-allowed; }

.score-btn--active {
  background: #1a6dff;
  border-color: #1a6dff;
  color: #fff !important;
}

.score-btn--zero { }

.score-btn--neg { color: #ef4444; border-color: #fca5a5; background: #fff5f5; }
.score-btn--neg:hover:not(:disabled) { background: #ef4444; border-color: #ef4444; color: #fff; }
.score-btn--neg.score-btn--active { background: #ef4444; border-color: #ef4444; color: #fff; }

.score-btn--pos { color: #10b981; border-color: #6ee7b7; background: #f0fdf4; }
.score-btn--pos:hover:not(:disabled) { background: #10b981; border-color: #10b981; color: #fff; }
.score-btn--pos.score-btn--active { background: #10b981; border-color: #10b981; color: #fff; }

/* Pass/Fail buttons */
.pf-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 18px;
  border-radius: 20px;
  border: 1.5px solid;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}
.pf-btn:disabled { opacity: 0.45; cursor: not-allowed; }

.pf-btn--pass {
  border-color: #6ee7b7;
  color: #059669;
  background: #f0fdf4;
}
.pf-btn--pass:hover:not(:disabled) { background: #10b981; color: #fff; border-color: #10b981; }
.pf-btn--pass.pf-btn--active { background: #10b981; color: #fff; border-color: #10b981; }

.pf-btn--fail {
  border-color: #fca5a5;
  color: #dc2626;
  background: #fff5f5;
}
.pf-btn--fail:hover:not(:disabled) { background: #ef4444; color: #fff; border-color: #ef4444; }
.pf-btn--fail.pf-btn--active { background: #ef4444; color: #fff; border-color: #ef4444; }

/* Grade buttons */
.grade-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 14px;
  border-radius: 20px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  font-size: 12px;
  font-weight: 500;
  color: #374151;
  cursor: pointer;
  transition: all 0.15s;
}
.grade-btn:hover:not(:disabled) { border-color: #93c5fd; color: #1a6dff; }
.grade-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.grade-btn--active { background: #1a6dff; border-color: #1a6dff; color: #fff; }
.grade-btn--active .grade-score { color: rgba(255,255,255,0.75); }

.grade-score {
  font-size: 10px;
  color: #9ca3af;
  font-weight: 400;
}

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

/* Cumulative counter */
.counter-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  color: #374151;
  cursor: pointer;
  transition: all 0.15s;
}
.counter-btn:hover:not(:disabled) { border-color: #1a6dff; color: #1a6dff; }
.counter-btn:disabled { opacity: 0.45; cursor: not-allowed; }

.counter-val {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  min-width: 28px;
  text-align: center;
}

.counter-unit {
  font-size: 11px;
  color: #9ca3af;
}

/* ===== Score Bar ===== */
.score-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 20px;
  background: #fff;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.score-bar-left {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.score-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
  tabular-nums: true;
}

.score-sep, .score-max {
  font-size: 14px;
  color: #9ca3af;
}

.score-grade {
  font-size: 14px;
  font-weight: 700;
  margin-left: 4px;
}

.score-bar-stats {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 11px;
}

.stat-pass  { color: #10b981; display: flex; align-items: center; gap: 3px; }
.stat-fail  { color: #ef4444; display: flex; align-items: center; gap: 3px; }
.stat-deduct { color: #ef4444; }
.stat-bonus  { color: #10b981; }

.score-bar-progress {
  font-size: 11px;
  color: #9ca3af;
  margin-left: auto;
}

.score-bar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
