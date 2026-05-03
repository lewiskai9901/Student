<script setup lang="ts">
/**
 * TaskReviewWorkbenchView — 任务审核工作台 (Audit Console redesign)
 * 双栏 + 键盘驱动 (J/K 切换 · A 通过+发布 · R 驳回 · E 延期 · Esc 取消)
 */
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SubmitAppealDialog from '@/views/inspection/appeals/components/SubmitAppealDialog.vue'
import {
  getTasks, reviewTask, rejectTask, publishTask, extendTaskDeadline,
} from '@/api/inspection/task'
import { getSubmissions, getDetails } from '@/api/inspection/submission'
import type { InspTask, InspSubmission, SubmissionDetail } from '@/types/insp/project'
import { useAuthStore } from '@/stores/auth'
import { TaskStatusConfig, type TaskStatus } from '@/types/insp/enums'

const authStore = useAuthStore()

const MAX_AUTO_REJECT = 3

// ── State ──
const loading = ref(false)
const loadingDetails = ref(false)
const submitting = ref(false)
const submittedTasks = ref<InspTask[]>([])
// P2: 仅看延迟交付 (审核侧 cherry pick)
const lateOnly = ref(false)
const filteredTasks = computed(() =>
  lateOnly.value ? submittedTasks.value.filter(t => t.lateSubmission) : submittedTasks.value
)
const lateCount = computed(() => submittedTasks.value.filter(t => t.lateSubmission).length)
const selectedTaskId = ref<number | null>(null)
const taskSubmissions = ref<InspSubmission[]>([])
const submissionDetails = ref<Map<number, SubmissionDetail[]>>(new Map())

const reviewComment = ref('')
const showRejectInline = ref(false)

// 申诉对话框
const appealDialog = ref(false)
const appealDetailId = ref<number | null>(null)
const appealItemName = ref<string | undefined>(undefined)
const appealCurrentScore = ref<number | undefined>(undefined)

const selectedTask = computed(() =>
  submittedTasks.value.find(t => t.id === selectedTaskId.value) ?? null
)

const subjectAggregate = computed(() => {
  const subs = taskSubmissions.value
  const total = subs.length
  const scored = subs.filter(s => s.finalScore != null).length
  const passed = subs.filter(s => s.passed === true).length
  const failed = subs.filter(s => s.passed === false).length
  const avgScore = subs.length > 0
    ? subs.reduce((sum, s) => sum + Number(s.finalScore ?? 0), 0) / subs.length
    : 0
  return { total, scored, passed, failed, avgScore }
})

// ── Loaders ──
async function loadSubmittedTasks() {
  loading.value = true
  try {
    const all = await getTasks()
    submittedTasks.value = all.filter(t => t.status === 'SUBMITTED' || t.status === 'UNDER_REVIEW')
    if (!selectedTaskId.value && submittedTasks.value.length > 0) {
      await selectTask(submittedTasks.value[0])
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载待审任务失败')
  } finally {
    loading.value = false
  }
}

async function selectTask(task: InspTask) {
  selectedTaskId.value = task.id
  reviewComment.value = ''
  showRejectInline.value = false
  loadingDetails.value = true
  try {
    const subs = await getSubmissions(task.projectId as any)
    taskSubmissions.value = subs.filter(s => s.taskId === task.id)
    const detailMap = new Map<number, SubmissionDetail[]>()
    for (const sub of taskSubmissions.value) {
      try {
        detailMap.set(sub.id, await getDetails(sub.id))
      } catch { /* ignore */ }
    }
    submissionDetails.value = detailMap
  } catch (e: any) {
    console.warn('Load details failed', e)
  } finally {
    loadingDetails.value = false
  }
}

// ── Verdict actions ──
async function approve() {
  if (!selectedTaskId.value) return
  submitting.value = true
  try {
    await reviewTask(selectedTaskId.value, {
      reviewerName: authStore.userName || 'admin',
      comment: reviewComment.value || '审核通过',
    })
    await publishTask(selectedTaskId.value)
    ElMessage.success('已通过并发布')
    moveToNext()
  } catch (e: any) {
    ElMessage.error(e.message || '审批失败')
  } finally {
    submitting.value = false
  }
}

async function reject() {
  if (!selectedTaskId.value) return
  if (!reviewComment.value.trim()) {
    ElMessage.warning('驳回必须填写审核意见')
    return
  }
  submitting.value = true
  try {
    await rejectTask(selectedTaskId.value, reviewComment.value)
    ElMessage.success('已驳回, 任务退回给检查员修改')
    moveToNext()
  } catch (e: any) {
    ElMessage.error(e.message || '驳回失败')
  } finally {
    submitting.value = false
  }
}

async function extend() {
  if (!selectedTaskId.value || !selectedTask.value) return
  const current = (selectedTask.value as any).extendedTo || selectedTask.value.taskDate
  try {
    const { value } = await ElMessageBox.prompt(
      `当前期限: ${current}\n输入新延期日期 YYYY-MM-DD, 必须晚于当前`,
      '延长任务期限',
      { inputPattern: /^\d{4}-\d{2}-\d{2}$/, inputErrorMessage: '日期格式必须为 YYYY-MM-DD', inputValue: '' },
    )
    await extendTaskDeadline(selectedTaskId.value, value)
    ElMessage.success(`已延期到 ${value}`)
    if (selectedTask.value) (selectedTask.value as any).extendedTo = value
  } catch (e: any) {
    if (e?.message) ElMessage.error(e.message)
  }
}

function moveToNext() {
  const remaining = submittedTasks.value.filter(t => t.id !== selectedTaskId.value)
  submittedTasks.value = remaining
  if (remaining.length) {
    selectTask(remaining[0])
  } else {
    selectedTaskId.value = null
    taskSubmissions.value = []
    submissionDetails.value = new Map()
    loadSubmittedTasks()
  }
}

// ── Appeal launcher ──
function openAppeal(d: SubmissionDetail) {
  appealDetailId.value = d.id
  appealItemName.value = d.itemName
  appealCurrentScore.value = d.score ?? undefined
  appealDialog.value = true
}

// ── Helpers ──
function fmtDate(s?: string | null) {
  if (!s) return '—'
  const d = new Date(s)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}

function passVariant(passed: boolean | null | undefined): string {
  if (passed === true) return 'pass'
  if (passed === false) return 'fail'
  return 'pending'
}

function rejectionVariant(count?: number) {
  if (!count) return 'pending'
  if (count >= MAX_AUTO_REJECT) return 'fail'
  if (count >= 2) return 'warn'
  return 'info'
}

// ── Keyboard hotkeys ──
function onKey(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA') return
  if (e.metaKey || e.ctrlKey || e.altKey) return

  const idx = submittedTasks.value.findIndex(t => t.id === selectedTaskId.value)
  switch (e.key) {
    case 'j': case 'ArrowDown':
      if (idx < submittedTasks.value.length - 1) selectTask(submittedTasks.value[idx + 1])
      e.preventDefault()
      break
    case 'k': case 'ArrowUp':
      if (idx > 0) selectTask(submittedTasks.value[idx - 1])
      e.preventDefault()
      break
    case 'a':
      if (selectedTask.value && !showRejectInline.value) approve()
      e.preventDefault()
      break
    case 'r':
      showRejectInline.value = true
      e.preventDefault()
      break
    case 'e':
      extend()
      e.preventDefault()
      break
    case 'Escape':
      showRejectInline.value = false
      break
  }
}

watch(selectedTaskId, () => { reviewComment.value = ''; showRejectInline.value = false })

onMounted(() => {
  loadSubmittedTasks()
  window.addEventListener('keydown', onKey)
})
onUnmounted(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <div class="insp-shell task-review">
    <!-- ── Operator bar ─────────────── -->
    <header class="op-bar">
      <div class="op-bar__lead">
        <span class="insp-stamp">审核台 · 待审 {{ submittedTasks.length }}</span>
        <h1 class="op-title">检查任务审核工作台</h1>
      </div>
      <div class="op-bar__right">
        <div class="kbd-tray">
          <span class="kbd-pair"><kbd class="insp-kbd">J</kbd><kbd class="insp-kbd">K</kbd> 切换</span>
          <span class="kbd-pair"><kbd class="insp-kbd insp-kbd--inverted">A</kbd> 通过+发布</span>
          <span class="kbd-pair"><kbd class="insp-kbd">R</kbd> 驳回</span>
          <span class="kbd-pair"><kbd class="insp-kbd">E</kbd> 延期</span>
        </div>
        <button class="insp-btn insp-btn--ghost" @click="loadSubmittedTasks">刷新</button>
      </div>
    </header>

    <!-- ── Two-pane ─────────────── -->
    <div class="pane">
      <!-- Queue -->
      <aside class="queue" v-loading="loading">
        <div class="queue-meta">
          <span class="insp-caps">待审清单</span>
          <span class="queue-meta__count insp-num">{{ filteredTasks.length }}<span v-if="lateOnly" class="dim">/{{ submittedTasks.length }}</span></span>
          <button
            v-if="lateCount > 0"
            class="late-filter-btn"
            :class="{ 'is-active': lateOnly }"
            @click="lateOnly = !lateOnly"
            :title="lateOnly ? '点击查看全部' : `仅看延迟交付 (${lateCount} 条)`"
          >
            ⏱ 延迟 {{ lateCount }}
          </button>
        </div>
        <ol class="queue-list">
          <li
            v-for="(t, i) in filteredTasks" :key="t.id"
            class="queue-item"
            :class="{ 'is-selected': selectedTaskId === t.id }"
            @click="selectTask(t)"
          >
            <span class="queue-item__idx insp-num">{{ String(i + 1).padStart(2, '0') }}</span>
            <div class="queue-item__body">
              <div class="queue-item__code">{{ t.taskCode }}</div>
              <div class="queue-item__sub">
                <span>{{ t.inspectorName || `检查员#${t.inspectorId}` }}</span>
                <span class="queue-item__sep">·</span>
                <span class="insp-num">{{ fmtDate(t.taskDate) }}</span>
              </div>
              <div class="queue-item__metrics">
                <span class="insp-chip insp-chip--info">
                  {{ TaskStatusConfig[t.status]?.label || t.status }}
                </span>
                <span class="queue-item__progress insp-num">
                  {{ t.completedTargets || 0 }}<span class="dim">/{{ t.totalTargets || 0 }}</span>
                </span>
              </div>
            </div>
            <span
              v-if="(t as any).rejectionCount && (t as any).rejectionCount > 0"
              class="insp-chip insp-chip--ghost"
              :class="`insp-chip--${rejectionVariant((t as any).rejectionCount)}`"
              :title="`已驳回 ${(t as any).rejectionCount}/${MAX_AUTO_REJECT} 次`"
            >驳{{ (t as any).rejectionCount }}</span>
            <span
              v-if="t.lateSubmission"
              class="late-badge"
              :title="`延迟 ${t.lateDays} 天交付`"
            >迟{{ t.lateDays }}</span>
          </li>
          <li v-if="!loading && filteredTasks.length === 0 && lateOnly" class="queue-empty">
            <div class="insp-stamp">没有延迟交付任务</div>
          </li>
          <li v-if="!loading && submittedTasks.length === 0" class="queue-empty">
            <div class="insp-stamp">无待审</div>
          </li>
        </ol>
      </aside>

      <!-- Detail -->
      <main class="detail" v-loading="loadingDetails">
        <template v-if="selectedTask">
          <!-- Detail head -->
          <div class="detail-head">
            <div class="detail-head__lead">
              <div class="insp-eyebrow">案件 · 当前正在审理</div>
              <h2 class="detail-code">{{ selectedTask.taskCode }}</h2>
              <div class="detail-meta">
                <span>{{ selectedTask.inspectorName || `检查员#${selectedTask.inspectorId}` }} 提交</span>
                <span class="dot">·</span>
                <span class="insp-num">{{ fmtDate(selectedTask.taskDate) }}</span>
                <span v-if="(selectedTask as any).extendedTo" class="dot">·</span>
                <span v-if="(selectedTask as any).extendedTo" class="extended-tag">
                  延至 <span class="insp-num">{{ (selectedTask as any).extendedTo }}</span>
                </span>
                <span v-if="selectedTask.lateSubmission" class="dot">·</span>
                <span v-if="selectedTask.lateSubmission" class="late-tag" :title="`延迟 ${selectedTask.lateDays} 天提交`">
                  延迟交付 <span class="insp-num">{{ selectedTask.lateDays }}</span> 天
                </span>
              </div>
            </div>
            <!-- Aggregate stats -->
            <div class="detail-head__metrics">
              <div class="insp-stat">
                <span class="insp-stat__value">{{ subjectAggregate.scored }}<span class="dim">/{{ subjectAggregate.total }}</span></span>
                <span class="insp-stat__label">已评分</span>
              </div>
              <div class="insp-rule rule-vert" />
              <div class="insp-stat">
                <span class="insp-stat__value" :style="{ color: subjectAggregate.avgScore < 0 ? 'var(--insp-fail)' : 'var(--insp-pass)' }">
                  {{ subjectAggregate.avgScore.toFixed(1) }}
                </span>
                <span class="insp-stat__label">平均得分</span>
              </div>
              <div class="insp-rule rule-vert" />
              <div class="insp-stat">
                <span class="insp-stat__value">
                  <span class="metric-pass">{{ subjectAggregate.passed }}</span>
                  <span class="dim"> / </span>
                  <span class="metric-fail">{{ subjectAggregate.failed }}</span>
                </span>
                <span class="insp-stat__label">通过 / 未达标</span>
              </div>
            </div>
          </div>

          <!-- Rejection notice -->
          <div
            v-if="(selectedTask as any).rejectionCount > 0 || (selectedTask as any).extendedTo"
            class="alert-strip"
            :class="{ 'alert-strip--max': (selectedTask as any).rejectionCount >= MAX_AUTO_REJECT }"
          >
            <span class="alert-strip__icon">!</span>
            <span class="alert-strip__text">
              已驳回 <strong class="insp-num">{{ (selectedTask as any).rejectionCount || 0 }}/{{ MAX_AUTO_REJECT }}</strong> 次
              <template v-if="(selectedTask as any).rejectionCount >= MAX_AUTO_REJECT">
                · 已达自动驳回上限, 再驳回将被拒, 请延期或重新分派检查员
              </template>
            </span>
            <button class="insp-btn insp-btn--sm" @click="extend">
              <kbd class="insp-kbd">E</kbd> 延期
            </button>
          </div>

          <hr class="insp-rule" />

          <!-- Submissions -->
          <section class="subs">
            <div class="subs-head">
              <span class="insp-eyebrow">受检对象 · {{ taskSubmissions.length }} 个</span>
            </div>
            <article v-for="sub in taskSubmissions" :key="sub.id" class="sub-card">
              <header class="sub-card__head">
                <div class="sub-card__lead">
                  <span class="sub-card__name">{{ sub.targetName || `目标#${sub.targetId}` }}</span>
                  <span class="insp-chip" :class="`insp-chip--${passVariant(sub.passed)}`">
                    {{ sub.passed === true ? '达标' : sub.passed === false ? '未达标' : '未评定' }}
                  </span>
                </div>
                <div class="sub-card__metrics">
                  <span v-if="sub.grade" class="insp-stamp">{{ sub.grade }}</span>
                  <span v-if="sub.finalScore != null" class="sub-card__score insp-num"
                        :style="{ color: Number(sub.finalScore) < 0 ? 'var(--insp-fail)' : 'var(--insp-pass)' }">
                    {{ Number(sub.finalScore).toFixed(1) }}
                  </span>
                </div>
              </header>
              <ul v-if="(submissionDetails.get(sub.id) || []).length" class="detail-rows">
                <li v-for="d in submissionDetails.get(sub.id)" :key="d.id" class="detail-row"
                    :class="{ 'is-flagged': d.isFlagged }">
                  <span class="detail-row__name">{{ d.itemName }}</span>
                  <span class="detail-row__resp">{{ d.responseValue || '—' }}</span>
                  <span class="detail-row__score insp-num"
                        :style="{ color: Number(d.score ?? 0) < 0 ? 'var(--insp-fail)' : 'var(--insp-ink-secondary)' }">
                    {{ d.score != null ? Number(d.score).toFixed(1) : '—' }}
                  </span>
                  <button class="detail-row__appeal insp-btn insp-btn--ghost insp-btn--sm"
                          title="对此扣分项发起申诉" @click="openAppeal(d)">
                    申诉
                  </button>
                </li>
              </ul>
            </article>

            <div v-if="!loadingDetails && taskSubmissions.length === 0" class="subs-empty">
              <span class="insp-stamp">无打分数据</span>
            </div>
          </section>

          <hr class="insp-rule insp-rule--strong" />

          <!-- Verdict panel -->
          <section class="verdict">
            <div class="verdict-head">
              <span class="insp-eyebrow">裁决</span>
              <span class="verdict-hint">
                <kbd class="insp-kbd insp-kbd--inverted">A</kbd> 通过+发布 ·
                <kbd class="insp-kbd">R</kbd> 驳回 ·
                <kbd class="insp-kbd">E</kbd> 延期
              </span>
            </div>

            <div v-if="!showRejectInline" class="verdict-form">
              <label class="verdict-field">
                <span class="insp-caps">审核备注 · 可选</span>
                <textarea v-model="reviewComment" rows="2" class="verdict-input"
                          placeholder="可选 · 通过时附加给申诉记录的备注" />
              </label>
              <div class="verdict-actions">
                <button class="insp-btn" @click="showRejectInline = true">
                  <kbd class="insp-kbd">R</kbd> 改为驳回
                </button>
                <button class="insp-btn insp-btn--accent"
                        :disabled="submitting" @click="approve">
                  <kbd class="insp-kbd insp-kbd--inverted">A</kbd> 通过 + 发布
                </button>
              </div>
            </div>

            <div v-else class="verdict-form verdict-form--reject">
              <div class="reject-head">
                <span class="insp-stamp">驳回</span>
                <span class="reject-warn">驳回必须填写明确理由让检查员理解需要修改什么</span>
              </div>
              <textarea v-model="reviewComment" rows="4" class="verdict-input"
                        placeholder="例: 经济2025-2 班的卫生扣分缺乏照片证据, 请补充后重新提交"
                        autofocus />
              <div class="verdict-actions">
                <button class="insp-btn" @click="showRejectInline = false">取消</button>
                <button class="insp-btn insp-btn--primary"
                        :disabled="submitting" @click="reject">确认驳回</button>
              </div>
            </div>
          </section>
        </template>

        <div v-else class="detail-empty">
          <div class="insp-stamp">无待审</div>
          <p class="detail-empty__hint">
            待审清单为空 · 全部任务都已审核通过
          </p>
        </div>
      </main>
    </div>

    <!-- 申诉对话框 -->
    <SubmitAppealDialog
      v-if="appealDetailId != null"
      v-model="appealDialog"
      :submission-detail-id="appealDetailId"
      :item-name="appealItemName"
      :current-score="appealCurrentScore"
    />
  </div>
</template>

<style scoped>
.task-review {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--insp-bg-page);
}

/* ─ Operator bar ─────── */
.op-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--insp-sp-5);
  padding: var(--insp-sp-4) var(--insp-sp-7);
  background: var(--insp-bg-surface);
  border-bottom: 1px solid var(--insp-border-default);
  flex-shrink: 0;
}

.op-bar__lead { display: flex; align-items: center; gap: var(--insp-sp-5); }
.op-title {
  font-family: var(--insp-font-display);
  font-size: 22px; font-weight: 500; margin: 0;
  letter-spacing: var(--insp-tracking-display);
  color: var(--insp-ink-primary);
}
.op-bar__right { display: flex; align-items: center; gap: var(--insp-sp-5); }
.kbd-tray {
  display: flex; align-items: center; gap: var(--insp-sp-4);
  padding-right: var(--insp-sp-4);
  border-right: 1px solid var(--insp-border-subtle);
}
.kbd-pair {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: var(--insp-text-xs); color: var(--insp-ink-tertiary);
}

/* ─ Pane ─────── */
.pane {
  display: grid;
  grid-template-columns: 380px 1fr;
  flex: 1;
  min-height: 0;
}

/* ─ Queue ─────── */
.queue {
  border-right: 1px solid var(--insp-border-subtle);
  background: var(--insp-bg-surface);
  display: flex; flex-direction: column;
  overflow: hidden;
}
.queue-meta {
  display: flex; align-items: baseline; justify-content: space-between;
  padding: var(--insp-sp-4) var(--insp-sp-5) var(--insp-sp-3);
  border-bottom: 1px solid var(--insp-border-subtle);
}
.queue-meta__count { font-size: var(--insp-text-md); font-weight: 600; color: var(--insp-accent); }
.queue-list { flex: 1; overflow-y: auto; list-style: none; margin: 0; padding: 0; }

.queue-item {
  display: grid;
  grid-template-columns: 32px 1fr auto;
  gap: var(--insp-sp-3);
  align-items: start;
  padding: var(--insp-sp-4) var(--insp-sp-5);
  border-bottom: 1px solid var(--insp-border-subtle);
  cursor: pointer;
  transition: background var(--insp-t-fast);
  position: relative;
}
.queue-item:hover { background: var(--insp-bg-subtle); }
.queue-item.is-selected { background: var(--insp-bg-subtle); }
.queue-item.is-selected::before {
  content: ''; position: absolute; left: 0; top: 0; bottom: 0;
  width: 3px; background: var(--insp-accent);
}

.queue-item__idx {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs); font-weight: 500;
  color: var(--insp-ink-quaternary);
  padding-top: 2px;
}
.queue-item__body { min-width: 0; }
.queue-item__code {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-sm); font-weight: 600;
  color: var(--insp-ink-primary);
}
.queue-item__sub {
  margin-top: 4px;
  display: flex; align-items: center; gap: 4px;
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-secondary);
}
.queue-item__sub .queue-item__sep { color: var(--insp-ink-quaternary); }
.queue-item__metrics {
  margin-top: 8px;
  display: flex; align-items: center; gap: var(--insp-sp-2);
}
.queue-item__progress { font-family: var(--insp-font-mono); font-size: var(--insp-text-xs); color: var(--insp-ink-tertiary); }
.queue-item__progress .dim { color: var(--insp-ink-quaternary); }

.queue-empty { padding: var(--insp-sp-8) var(--insp-sp-5); text-align: center; }

/* ─ Detail ─────── */
.detail {
  overflow-y: auto;
  padding: var(--insp-sp-7) var(--insp-sp-9);
  max-width: 1080px;
  width: 100%;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--insp-sp-7);
  margin-bottom: var(--insp-sp-5);
}
.detail-code {
  font-family: var(--insp-font-display);
  font-size: 36px; font-weight: 500;
  letter-spacing: var(--insp-tracking-display);
  margin: 4px 0 var(--insp-sp-2);
}
.detail-meta {
  display: flex; align-items: center; gap: 6px;
  font-size: var(--insp-text-sm); color: var(--insp-ink-secondary);
  flex-wrap: wrap;
}
.detail-meta .dot { color: var(--insp-ink-quaternary); }
.extended-tag {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 1px 8px;
  background: var(--insp-info-pale);
  color: var(--insp-info);
  font-size: var(--insp-text-xs);
  border-radius: var(--insp-radius-sm);
  font-weight: 500;
}
.late-tag {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 1px 8px;
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
  border: 1px solid rgba(245, 158, 11, 0.35);
  font-size: var(--insp-text-xs);
  border-radius: var(--insp-radius-sm);
  font-weight: 600;
}
.late-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 28px;
  padding: 0 5px; height: 18px;
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
  border: 1px solid rgba(245, 158, 11, 0.4);
  border-radius: 9px;
  font-size: 10px;
  font-weight: 600;
  margin-left: 4px;
  font-family: var(--insp-font-mono);
}
.late-filter-btn {
  margin-left: auto;
  padding: 2px 8px;
  height: 22px;
  border-radius: 11px;
  border: 1px solid rgba(245, 158, 11, 0.4);
  background: rgba(245, 158, 11, 0.1);
  color: #b45309;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--insp-t-fast);
}
.late-filter-btn:hover { background: rgba(245, 158, 11, 0.2); }
.late-filter-btn.is-active {
  background: #b45309;
  color: white;
  border-color: #b45309;
}
.queue-meta__count .dim {
  color: var(--insp-ink-quaternary);
  font-weight: 400;
}

.detail-head__metrics {
  display: flex; align-items: stretch; gap: var(--insp-sp-5);
  padding: var(--insp-sp-3) 0;
  flex-shrink: 0;
}
.detail-head__metrics .insp-stat__value { font-size: 28px; }
.rule-vert { width: 1px; height: 40px; background: var(--insp-border-subtle); align-self: center; }
.metric-pass { color: var(--insp-pass); }
.metric-fail { color: var(--insp-fail); }
.dim { color: var(--insp-ink-quaternary); }

/* ─ Alert strip ─────── */
.alert-strip {
  display: flex; align-items: center; gap: var(--insp-sp-3);
  padding: 10px var(--insp-sp-4);
  background: var(--insp-warn-pale);
  border: 1px solid color-mix(in oklab, var(--insp-warn) 35%, transparent);
  border-radius: var(--insp-radius-md);
  margin-bottom: var(--insp-sp-5);
}
.alert-strip--max {
  background: var(--insp-fail-pale);
  border-color: var(--insp-fail);
}
.alert-strip__icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 22px; height: 22px;
  background: var(--insp-warn); color: white;
  border-radius: var(--insp-radius-pill);
  font-family: var(--insp-font-mono); font-weight: 700; font-size: 13px;
  flex-shrink: 0;
}
.alert-strip--max .alert-strip__icon { background: var(--insp-fail); }
.alert-strip__text {
  flex: 1;
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-secondary);
}
.alert-strip--max .alert-strip__text { color: var(--insp-fail); font-weight: 500; }

/* ─ Submissions ─────── */
.subs { padding: var(--insp-sp-5) 0; }
.subs-head { margin-bottom: var(--insp-sp-3); }

.sub-card {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-subtle);
  border-radius: var(--insp-radius-md);
  margin-bottom: var(--insp-sp-3);
  overflow: hidden;
}
.sub-card__head {
  display: flex; align-items: center; justify-content: space-between;
  gap: var(--insp-sp-3);
  padding: var(--insp-sp-3) var(--insp-sp-4);
  background: var(--insp-bg-subtle);
  border-bottom: 1px solid var(--insp-border-subtle);
}
.sub-card__lead { display: flex; align-items: center; gap: var(--insp-sp-2); }
.sub-card__name {
  font-size: var(--insp-text-md); font-weight: 600;
  color: var(--insp-ink-primary);
}
.sub-card__metrics {
  display: flex; align-items: center; gap: var(--insp-sp-3);
}
.sub-card__score {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-lg); font-weight: 600;
  letter-spacing: -0.02em;
}

.detail-rows { list-style: none; margin: 0; padding: 0; }
.detail-row {
  display: grid;
  grid-template-columns: 1fr 100px 80px 56px;
  gap: var(--insp-sp-3);
  align-items: center;
  padding: 8px var(--insp-sp-4);
  border-bottom: 1px solid var(--insp-border-subtle);
  font-size: var(--insp-text-sm);
}
.detail-row:last-child { border-bottom: 0; }
.detail-row.is-flagged {
  background: linear-gradient(to right, var(--insp-fail-pale) 0, transparent 60%);
}
.detail-row__name { color: var(--insp-ink-primary); }
.detail-row__resp {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-tertiary);
}
.detail-row__score {
  font-family: var(--insp-font-mono);
  font-weight: 500;
  text-align: right;
}
.detail-row__appeal {
  justify-self: end;
  color: var(--insp-accent);
}
.detail-row__appeal:hover { background: var(--insp-accent-paler); }

.subs-empty { padding: var(--insp-sp-8); text-align: center; }

/* ─ Verdict ─────── */
.verdict { margin-top: var(--insp-sp-5); }
.verdict-head {
  display: flex; align-items: baseline; justify-content: space-between;
  margin-bottom: var(--insp-sp-4);
}
.verdict-hint {
  font-size: var(--insp-text-xs); color: var(--insp-ink-tertiary);
  display: inline-flex; align-items: center; gap: 4px;
}
.verdict-form {
  display: flex; flex-direction: column; gap: var(--insp-sp-4);
}
.verdict-field { display: flex; flex-direction: column; gap: 6px; }

.verdict-input {
  padding: 10px 14px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  font-family: inherit;
  font-size: var(--insp-text-md);
  line-height: var(--insp-leading-snug);
  color: var(--insp-ink-primary);
  background: var(--insp-bg-surface);
  resize: vertical;
}
.verdict-input:focus {
  outline: none;
  border-color: var(--insp-accent);
  box-shadow: 0 0 0 3px var(--insp-accent-paler);
}

.verdict-form--reject {
  padding: var(--insp-sp-5);
  background: var(--insp-fail-pale);
  border: 1px solid var(--insp-fail);
  border-radius: var(--insp-radius-md);
}
.reject-head {
  display: flex; align-items: center; gap: var(--insp-sp-3);
}
.reject-warn { font-size: var(--insp-text-sm); color: var(--insp-fail); font-weight: 500; }

.verdict-actions {
  display: flex; justify-content: flex-end; gap: var(--insp-sp-3);
}
.verdict-actions .insp-btn .insp-kbd { margin-right: 4px; }

/* ─ Empty ─────── */
.detail-empty {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  height: 100%;
  text-align: center;
  padding: var(--insp-sp-10);
}
.detail-empty__hint {
  margin-top: var(--insp-sp-4);
  color: var(--insp-ink-tertiary);
  font-size: var(--insp-text-md);
}

/* ─ Responsive ─────── */
@media (max-width: 1100px) {
  .pane { grid-template-columns: 320px 1fr; }
  .detail { padding: var(--insp-sp-5) var(--insp-sp-6); }
  .detail-head { flex-direction: column; gap: var(--insp-sp-4); }
}
@media (max-width: 800px) {
  .pane { grid-template-columns: 1fr; }
  .queue { display: none; }
  .kbd-tray { display: none; }
}
</style>
