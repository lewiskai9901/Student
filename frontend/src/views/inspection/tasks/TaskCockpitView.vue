<script setup lang="ts">
/**
 * TaskCockpitView — 检查员驾驶舱 (彻底重构的交互模型)
 *
 * 与传统「逐目标点击逐项打分」不同, 此页面提供:
 *
 * ① **Sweep Mode (扫射模式)** — 默认
 *    一次锁定一个字段, 所有目标垂直排列, 1/2 数字键给当前目标打分自动跳下一个,
 *    类似批量阅卷, 6 个目标 × 5 个字段 = 30 次点击 → 1 秒一格的连续节奏.
 *
 * ② **Matrix Mode (矩阵视图)** — 全景概览
 *    目标×字段二维表格, 一眼看到所有评分进度, 点格直接编辑.
 *
 * ③ **全键盘驱动** — 检查员从不离开键盘:
 *    H/L 切字段 (字段间)
 *    J/K 切目标 (字段内)
 *    1   通过 / 2 不通过 / 3 跳过 (PASS_FAIL)
 *    数字 直接打分 (DEDUCTION 模式)
 *    Tab 切换模式 (Sweep ↔ Matrix)
 *    F   切聚焦/全屏
 *    Cmd+S 提交 / 普通 S 也可
 *    ⏎    确认+前进
 *    ⎋    取消
 *
 * ④ **心电图进度条** — 顶部一格代表一个目标, 颜色直观表达 通过/不通过/未评/进行中.
 *
 * ⑤ **效率可见** — 显示当前字段已评 X/Y, 总进度, 平均每格用时.
 */
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/utils/request'
import {
  getSubmissions, getDetails, updateDetailResponse, flagDetail, unflagDetail,
} from '@/api/inspection/submission'
import { getTask, submitTask } from '@/api/inspection/task'
import type { InspTask, InspSubmission, SubmissionDetail } from '@/types/insp/project'

const route = useRoute()
const router = useRouter()
const taskId = Number(route.params.id)

// ── State ──
const loading = ref(false)
const submitting = ref(false)
const task = ref<InspTask | null>(null)
const submissions = ref<InspSubmission[]>([])
const allDetails = ref<SubmissionDetail[]>([])
const errorMsg = ref('')

// View modes
type ViewMode = 'sweep' | 'matrix' | 'focus'
const mode = ref<ViewMode>('sweep')

// Cursor: index in unique fields × index in submissions
const fieldIdx = ref(0)
const targetIdx = ref(0)

// Editing remark
const editingRemarkId = ref<number | null>(null)
const remarkDraft = ref('')

// Time tracking for "speed visible"
const startTime = ref(Date.now())
const lastActionTime = ref(Date.now())
const actionCount = ref(0)
const recentInterval = ref(0)

// ── Derived ──
const uniqueFields = computed(() => {
  const seen = new Map<string, { code: string; name: string; sectionName: string | null; itemType: string; mode: string | null }>()
  for (const d of allDetails.value) {
    if (!seen.has(d.itemCode)) {
      seen.set(d.itemCode, {
        code: d.itemCode,
        name: d.itemName,
        sectionName: d.sectionName,
        itemType: d.itemType,
        mode: d.scoringMode,
      })
    }
  }
  return Array.from(seen.values())
})

const currentField = computed(() => uniqueFields.value[fieldIdx.value] ?? null)

// Detail map: { fieldCode: { submissionId: detail } }
const detailIndex = computed(() => {
  const map = new Map<string, Map<number, SubmissionDetail>>()
  for (const d of allDetails.value) {
    if (!map.has(d.itemCode)) map.set(d.itemCode, new Map())
    map.get(d.itemCode)!.set(d.submissionId, d)
  }
  return map
})

const currentTarget = computed(() => submissions.value[targetIdx.value] ?? null)

const detailForCurrentCell = computed(() => {
  if (!currentField.value || !currentTarget.value) return null
  return detailIndex.value.get(currentField.value.code)?.get(currentTarget.value.id) ?? null
})

// ── Progress metrics ──
const totalCells = computed(() => uniqueFields.value.length * submissions.value.length)
const filledCells = computed(() =>
  allDetails.value.filter(d => d.responseValue != null).length
)
const overallPct = computed(() =>
  totalCells.value === 0 ? 0 : Math.round((filledCells.value / totalCells.value) * 100)
)

const fieldFilledCount = computed(() => {
  if (!currentField.value) return 0
  const m = detailIndex.value.get(currentField.value.code)
  if (!m) return 0
  let c = 0
  for (const d of m.values()) if (d.responseValue != null) c++
  return c
})

// ECG strip data: each submission's status across all fields
type CellStatus = 'empty' | 'pass' | 'fail' | 'partial' | 'flagged' | 'scored'
function statusForSubmission(s: InspSubmission): CellStatus {
  const details = allDetails.value.filter(d => d.submissionId === s.id)
  if (details.length === 0) return 'empty'
  const filled = details.filter(d => d.responseValue != null)
  if (filled.length === 0) return 'empty'
  if (filled.length < details.length) return 'partial'
  // All filled
  if (filled.some(d => d.isFlagged)) return 'flagged'
  if (filled.every(d => d.responseValue === 'PASS')) return 'pass'
  if (filled.some(d => d.responseValue === 'FAIL')) return 'fail'
  return 'scored'
}

// Speed: avg seconds since first action
const avgSeconds = computed(() => {
  if (actionCount.value < 2) return 0
  const elapsed = (Date.now() - startTime.value) / 1000
  return Math.round((elapsed / actionCount.value) * 10) / 10
})

// ── Loaders ──
async function loadAll() {
  loading.value = true
  errorMsg.value = ''
  try {
    task.value = await getTask(taskId)
    if (!task.value) {
      errorMsg.value = '任务不存在'
      return
    }
    const subs = await getSubmissions({ taskId })
    submissions.value = (subs || []).filter(s => Number(s.taskId) === taskId)

    const allDets: SubmissionDetail[] = []
    for (const sub of submissions.value) {
      try {
        const dets = await getDetails(sub.id)
        for (const d of dets) allDets.push(d)
      } catch { /* ignore */ }
    }
    allDetails.value = allDets
  } catch (e: any) {
    errorMsg.value = e?.message || '加载失败'
    ElMessage.error(errorMsg.value)
  } finally {
    loading.value = false
  }
}

// ── Scoring actions ──
async function setResponse(detail: SubmissionDetail, value: string, score?: number) {
  trackAction()
  // optimistic
  detail.responseValue = value
  if (score != null) detail.score = score
  detail.updatedAt = new Date().toISOString()
  try {
    await updateDetailResponse(detail.id, {
      responseValue: value,
      scoringMode: detail.scoringMode || undefined,
      score,
    })
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
    // revert
    loadAll()
  }
}

async function passCurrent() {
  const d = detailForCurrentCell.value
  if (!d) return
  if (d.scoringMode === 'PASS_FAIL') {
    await setResponse(d, 'PASS', 0)
  } else {
    await setResponse(d, 'PASS', 0)
  }
  advanceTarget()
}

async function failCurrent() {
  const d = detailForCurrentCell.value
  if (!d) return
  if (d.scoringMode === 'PASS_FAIL') {
    await setResponse(d, 'FAIL', -5)
  } else {
    await setResponse(d, 'FAIL', -5)
  }
  advanceTarget()
}

async function flagCurrent(reason = '需复查') {
  const d = detailForCurrentCell.value
  if (!d) return
  trackAction()
  try {
    if (d.isFlagged) {
      await unflagDetail(d.id)
      d.isFlagged = false
    } else {
      await flagDetail(d.id, { reason })
      d.isFlagged = true
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '标记失败')
  }
}

function advanceTarget() {
  if (targetIdx.value < submissions.value.length - 1) {
    targetIdx.value++
  } else if (fieldIdx.value < uniqueFields.value.length - 1) {
    // wrap to next field
    fieldIdx.value++
    targetIdx.value = 0
  }
}

// ── Submit ──
async function handleSubmit() {
  if (!task.value) return
  if (filledCells.value < totalCells.value) {
    try {
      await ElMessageBox.confirm(
        `还有 ${totalCells.value - filledCells.value} / ${totalCells.value} 项未评分, 确认提交?`,
        '确认提交',
        { type: 'warning' }
      )
    } catch { return }
  }
  submitting.value = true
  try {
    await submitTask(taskId)
    ElMessage.success('已提交')
    router.push('/inspection/my-tasks' as any).catch(() => router.push('/inspection'))
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

// ── Tracking ──
function trackAction() {
  const now = Date.now()
  recentInterval.value = (now - lastActionTime.value) / 1000
  lastActionTime.value = now
  actionCount.value++
}

// ── Keyboard ──
function onKey(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA') {
    if (e.key === 'Escape' && tag === 'TEXTAREA') {
      (e.target as HTMLElement).blur()
      editingRemarkId.value = null
    }
    return
  }

  if (e.metaKey || e.ctrlKey) {
    if (e.key === 's') {
      e.preventDefault()
      handleSubmit()
    }
    return
  }
  if (e.altKey) return

  switch (e.key) {
    case '1': passCurrent(); e.preventDefault(); break
    case '2': failCurrent(); e.preventDefault(); break
    case '3':
      if (detailForCurrentCell.value) setResponse(detailForCurrentCell.value, 'SKIP', 0)
      advanceTarget()
      e.preventDefault()
      break
    case 'j':
    case 'ArrowDown':
      if (targetIdx.value < submissions.value.length - 1) targetIdx.value++
      e.preventDefault()
      break
    case 'k':
    case 'ArrowUp':
      if (targetIdx.value > 0) targetIdx.value--
      e.preventDefault()
      break
    case 'h':
    case 'ArrowLeft':
      if (fieldIdx.value > 0) { fieldIdx.value--; targetIdx.value = 0 }
      e.preventDefault()
      break
    case 'l':
    case 'ArrowRight':
      if (fieldIdx.value < uniqueFields.value.length - 1) { fieldIdx.value++; targetIdx.value = 0 }
      e.preventDefault()
      break
    case ' ':
      flagCurrent()
      e.preventDefault()
      break
    case 'n':
      if (detailForCurrentCell.value) {
        editingRemarkId.value = detailForCurrentCell.value.id
        remarkDraft.value = detailForCurrentCell.value.remark || ''
        nextTick(() => {
          const el = document.querySelector('.remark-input') as HTMLElement | null
          el?.focus()
        })
      }
      e.preventDefault()
      break
    case 'Tab':
      mode.value = mode.value === 'sweep' ? 'matrix' : 'sweep'
      e.preventDefault()
      break
    case 'f':
      mode.value = mode.value === 'focus' ? 'sweep' : 'focus'
      e.preventDefault()
      break
    case 's':
      handleSubmit()
      e.preventDefault()
      break
    case 'Enter':
      advanceTarget()
      e.preventDefault()
      break
    case 'Escape':
      router.push(`/inspection/tasks/${taskId}/execute`)
      break
  }
}

onMounted(() => {
  loadAll()
  startTime.value = Date.now()
  lastActionTime.value = Date.now()
  window.addEventListener('keydown', onKey)
})
onUnmounted(() => window.removeEventListener('keydown', onKey))

// helper
function fmtTargetName(s: InspSubmission) {
  return s.targetName || `目标 #${s.targetId}`
}

// helper for matrix cell display
function cellDisplay(d: SubmissionDetail | undefined): { glyph: string; cls: string } {
  if (!d) return { glyph: '·', cls: 'cell--empty' }
  if (d.responseValue == null) return { glyph: '·', cls: 'cell--empty' }
  if (d.responseValue === 'PASS') return { glyph: '✓', cls: 'cell--pass' }
  if (d.responseValue === 'FAIL') return { glyph: '✗', cls: 'cell--fail' }
  if (d.responseValue === 'SKIP') return { glyph: '–', cls: 'cell--skip' }
  if (d.score != null) return { glyph: String(d.score), cls: d.score < 0 ? 'cell--fail' : 'cell--pass' }
  return { glyph: '?', cls: 'cell--empty' }
}

watch(currentField, () => { targetIdx.value = 0 })
</script>

<template>
  <div class="cockpit insp-shell" v-loading="loading">
    <!-- ── Top stripe: ECG progress + KPIs ─────────────── -->
    <header class="strip">
      <div class="strip-left">
        <span class="insp-stamp">驾驶舱</span>
        <h1 class="strip-title">{{ task?.taskCode || 'TSK …' }}</h1>
        <span class="strip-meta insp-num">{{ task?.taskDate || '' }}</span>
      </div>

      <!-- ECG bars -->
      <div class="ecg" :title="`${submissions.length} 个检查目标`">
        <div
          v-for="(s, i) in submissions" :key="s.id"
          class="ecg-bar"
          :class="['ecg-' + statusForSubmission(s), { 'is-cursor': i === targetIdx }]"
          @click="targetIdx = i"
        >
          <span class="ecg-bar__num insp-num">{{ String(i + 1).padStart(2, '0') }}</span>
        </div>
      </div>

      <div class="strip-right">
        <div class="kpi">
          <span class="kpi__num insp-num">{{ filledCells }}<span class="dim">/{{ totalCells }}</span></span>
          <span class="kpi__label">CELLS</span>
        </div>
        <div class="kpi">
          <span class="kpi__num insp-num">{{ overallPct }}<span class="dim">%</span></span>
          <span class="kpi__label">DONE</span>
        </div>
        <div class="kpi" v-if="actionCount >= 2">
          <span class="kpi__num insp-num">{{ avgSeconds }}<span class="dim">s</span></span>
          <span class="kpi__label">AVG</span>
        </div>
      </div>
    </header>

    <!-- ── Mode switch ─────────────── -->
    <div class="mode-bar">
      <button class="mode-btn" :class="{ 'is-active': mode === 'sweep' }" @click="mode = 'sweep'">
        <span class="mode-btn__name">扫射</span>
        <span class="mode-btn__hint">逐字段全目标快速打分</span>
      </button>
      <button class="mode-btn" :class="{ 'is-active': mode === 'matrix' }" @click="mode = 'matrix'">
        <span class="mode-btn__name">矩阵</span>
        <span class="mode-btn__hint">所有字段×目标全景</span>
      </button>
      <button class="mode-btn" :class="{ 'is-active': mode === 'focus' }" @click="mode = 'focus'">
        <span class="mode-btn__name">聚焦</span>
        <span class="mode-btn__hint">单格沉浸大字</span>
      </button>
      <div class="mode-spacer" />
      <span class="field-counter insp-num">字段 {{ fieldIdx + 1 }}<span class="dim">/{{ uniqueFields.length }}</span></span>
    </div>

    <!-- ── Sweep mode: stacked targets for current field ─────────────── -->
    <main v-if="mode === 'sweep' && currentField" class="sweep">
      <!-- Field header -->
      <div class="field-head">
        <div class="field-head__lead">
          <div class="insp-eyebrow">{{ currentField.sectionName || '字段' }}</div>
          <h2 class="field-name">{{ currentField.name }}</h2>
          <div class="field-meta">
            <span class="insp-chip insp-chip--info">{{ currentField.mode || currentField.itemType }}</span>
            <span class="field-meta__count insp-num">{{ fieldFilledCount }} / {{ submissions.length }}</span>
            <span class="field-meta__code insp-num">{{ currentField.code }}</span>
          </div>
        </div>
        <div class="field-nav">
          <button class="insp-btn insp-btn--sm" :disabled="fieldIdx === 0"
                  @click="fieldIdx > 0 && (fieldIdx--, targetIdx = 0)">
            <kbd class="insp-kbd">H</kbd> 上一字段
          </button>
          <button class="insp-btn insp-btn--sm" :disabled="fieldIdx === uniqueFields.length - 1"
                  @click="fieldIdx < uniqueFields.length - 1 && (fieldIdx++, targetIdx = 0)">
            下一字段 <kbd class="insp-kbd">L</kbd>
          </button>
        </div>
      </div>

      <!-- Target rows for sweep -->
      <ol class="sweep-list">
        <li
          v-for="(s, i) in submissions" :key="s.id"
          class="sweep-row"
          :class="{ 'is-cursor': i === targetIdx }"
          @click="targetIdx = i"
        >
          <span class="sweep-row__idx insp-num">{{ String(i + 1).padStart(2, '0') }}</span>
          <span class="sweep-row__name">{{ fmtTargetName(s) }}</span>

          <!-- Score buttons (PASS_FAIL) -->
          <template v-if="currentField.mode === 'PASS_FAIL'">
            <button class="grade-btn grade-btn--pass"
                    :class="{ 'is-on': detailIndex.get(currentField.code)?.get(s.id)?.responseValue === 'PASS' }"
                    @click.stop="targetIdx = i; passCurrent()">
              <span class="grade-btn__glyph">✓</span>
              <span class="grade-btn__label">通过</span>
            </button>
            <button class="grade-btn grade-btn--fail"
                    :class="{ 'is-on': detailIndex.get(currentField.code)?.get(s.id)?.responseValue === 'FAIL' }"
                    @click.stop="targetIdx = i; failCurrent()">
              <span class="grade-btn__glyph">✗</span>
              <span class="grade-btn__label">不通过</span>
            </button>
          </template>

          <!-- Numeric input for DEDUCTION -->
          <template v-else-if="currentField.mode === 'DEDUCTION'">
            <input
              type="number" step="0.5" class="num-input insp-num"
              :value="detailIndex.get(currentField.code)?.get(s.id)?.score ?? ''"
              @input="(e) => {
                const v = (e.target as HTMLInputElement).value
                const det = detailIndex.get(currentField.code!)?.get(s.id)
                if (det) setResponse(det, v, parseFloat(v))
              }"
              @click.stop
              placeholder="—"
            />
          </template>

          <!-- Generic value cell -->
          <template v-else>
            <span class="value-cell">
              {{ detailIndex.get(currentField.code)?.get(s.id)?.responseValue || '—' }}
            </span>
          </template>

          <!-- Flag dot -->
          <button
            class="flag-btn"
            :class="{ 'is-on': detailIndex.get(currentField.code)?.get(s.id)?.isFlagged }"
            @click.stop="targetIdx = i; flagCurrent()"
            title="标记为问题项 (空格)"
          >
            <span class="flag-glyph">▲</span>
          </button>
        </li>
      </ol>
    </main>

    <!-- ── Matrix mode ─────────────── -->
    <main v-else-if="mode === 'matrix'" class="matrix-wrap">
      <div class="matrix-scroll">
        <table class="matrix">
          <thead>
            <tr>
              <th class="m-corner">目标 ╲ 字段</th>
              <th
                v-for="(f, fi) in uniqueFields" :key="f.code"
                :class="{ 'is-cursor': fi === fieldIdx }"
                @click="fieldIdx = fi"
              >
                <div class="m-th">
                  <span class="m-th__name">{{ f.name }}</span>
                  <span class="m-th__mode insp-num">{{ f.mode || f.itemType }}</span>
                </div>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(s, ti) in submissions" :key="s.id"
              :class="{ 'is-cursor': ti === targetIdx }"
            >
              <th class="m-th-row" @click="targetIdx = ti">
                <span class="m-th-row__num insp-num">{{ String(ti + 1).padStart(2, '0') }}</span>
                <span class="m-th-row__name">{{ fmtTargetName(s) }}</span>
              </th>
              <td
                v-for="(f, fi) in uniqueFields" :key="f.code"
                class="m-cell"
                :class="[
                  cellDisplay(detailIndex.get(f.code)?.get(s.id)).cls,
                  {
                    'is-cursor': ti === targetIdx && fi === fieldIdx,
                    'is-flagged': detailIndex.get(f.code)?.get(s.id)?.isFlagged,
                  }
                ]"
                @click="fieldIdx = fi; targetIdx = ti"
              >
                {{ cellDisplay(detailIndex.get(f.code)?.get(s.id)).glyph }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>

    <!-- ── Focus mode ─────────────── -->
    <main v-else-if="mode === 'focus'" class="focus">
      <div v-if="currentField && currentTarget" class="focus-card">
        <div class="focus-meta">
          <span class="insp-eyebrow">{{ currentField.sectionName }} · 字段 {{ fieldIdx + 1 }}/{{ uniqueFields.length }}</span>
        </div>
        <h2 class="focus-target">{{ fmtTargetName(currentTarget) }}</h2>
        <h3 class="focus-field">{{ currentField.name }}</h3>

        <!-- PASS_FAIL big buttons -->
        <div v-if="currentField.mode === 'PASS_FAIL'" class="focus-grades">
          <button class="big-grade big-grade--pass"
                  :class="{ 'is-on': detailForCurrentCell?.responseValue === 'PASS' }"
                  @click="passCurrent">
            <span class="big-grade__glyph">✓</span>
            <span class="big-grade__name">通过</span>
            <span class="big-grade__kbd"><kbd class="insp-kbd insp-kbd--inverted">1</kbd></span>
          </button>
          <button class="big-grade big-grade--fail"
                  :class="{ 'is-on': detailForCurrentCell?.responseValue === 'FAIL' }"
                  @click="failCurrent">
            <span class="big-grade__glyph">✗</span>
            <span class="big-grade__name">不通过</span>
            <span class="big-grade__kbd"><kbd class="insp-kbd insp-kbd--inverted">2</kbd></span>
          </button>
        </div>

        <!-- Generic input -->
        <div v-else class="focus-input-wrap">
          <input
            type="text" class="focus-input"
            :value="detailForCurrentCell?.responseValue ?? ''"
            @change="(e) => detailForCurrentCell && setResponse(detailForCurrentCell, (e.target as HTMLInputElement).value)"
            placeholder="输入答案 …"
          />
        </div>

        <div class="focus-position">
          <span class="insp-num">{{ targetIdx + 1 }} / {{ submissions.length }}</span>
        </div>
      </div>
    </main>

    <!-- ── Bottom command bar ─────────────── -->
    <footer class="cmd-bar">
      <div class="cmd-bar__left">
        <span class="cmd-pair"><kbd class="insp-kbd insp-kbd--inverted">1</kbd> 通过</span>
        <span class="cmd-pair"><kbd class="insp-kbd insp-kbd--inverted">2</kbd> 不通过</span>
        <span class="cmd-pair"><kbd class="insp-kbd">3</kbd> 跳过</span>
        <span class="cmd-divider" />
        <span class="cmd-pair"><kbd class="insp-kbd">H</kbd><kbd class="insp-kbd">L</kbd> 字段</span>
        <span class="cmd-pair"><kbd class="insp-kbd">J</kbd><kbd class="insp-kbd">K</kbd> 目标</span>
        <span class="cmd-divider" />
        <span class="cmd-pair"><kbd class="insp-kbd">⎵</kbd> 标记</span>
        <span class="cmd-pair"><kbd class="insp-kbd">N</kbd> 备注</span>
        <span class="cmd-pair"><kbd class="insp-kbd">⇥</kbd> 切模式</span>
        <span class="cmd-pair"><kbd class="insp-kbd">F</kbd> 聚焦</span>
      </div>
      <div class="cmd-bar__right">
        <span v-if="recentInterval > 0" class="cmd-tick insp-num">+{{ recentInterval.toFixed(1) }}s</span>
        <button class="insp-btn" @click="router.back()">退出</button>
        <button class="insp-btn insp-btn--accent" :disabled="submitting" @click="handleSubmit">
          <kbd class="insp-kbd insp-kbd--inverted">S</kbd> 提交任务
        </button>
      </div>
    </footer>

    <!-- Remark drawer (n key) -->
    <Transition name="drawer">
      <div v-if="editingRemarkId" class="remark-drawer">
        <div class="remark-drawer__head">
          <span class="insp-eyebrow">备注 · 当前格</span>
          <button class="insp-btn insp-btn--ghost insp-btn--sm" @click="editingRemarkId = null">×</button>
        </div>
        <textarea
          v-model="remarkDraft" class="remark-input" rows="3"
          placeholder="备注 (Esc 关闭, 回车保存)"
          @keydown.enter.prevent="async () => {
            const d = allDetails.find(x => x.id === editingRemarkId)
            if (d) {
              try {
                await http.put(`/inspection/submissions/details/${d.id}/remark`, { remark: remarkDraft })
                d.remark = remarkDraft
                ElMessage.success('已保存')
              } catch {}
            }
            editingRemarkId = null
          }"
        />
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.cockpit {
  display: flex; flex-direction: column;
  height: 100vh;
  background: var(--insp-bg-page);
  overflow: hidden;
  font-family: var(--insp-font-body);
}

/* ── Top stripe ─────── */
.strip {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(400px, 2fr) minmax(220px, auto);
  gap: var(--insp-sp-5);
  align-items: center;
  padding: var(--insp-sp-4) var(--insp-sp-7);
  background: var(--insp-ink-primary);
  color: var(--insp-bg-surface);
  border-bottom: 2px solid var(--insp-accent);
  flex-shrink: 0;
}

.strip-left { display: flex; align-items: center; gap: var(--insp-sp-4); }

.strip-title {
  font-family: var(--insp-font-mono);
  font-size: 18px; font-weight: 600;
  margin: 0;
  letter-spacing: -0.01em;
}

.strip-meta {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs);
  color: rgba(255, 255, 255, 0.55);
}

.strip .insp-stamp {
  background: var(--insp-accent);
  color: white;
  border-color: var(--insp-accent);
}

/* ── ECG strip ─────── */
.ecg {
  display: flex;
  align-items: stretch;
  gap: 2px;
  height: 38px;
}

.ecg-bar {
  flex: 1; min-width: 22px;
  position: relative;
  display: flex; align-items: center; justify-content: center;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: transform var(--insp-t-fast);
}

.ecg-bar:hover { transform: translateY(-2px); }

.ecg-bar__num {
  font-family: var(--insp-font-mono);
  font-size: 9px; font-weight: 600;
  color: rgba(255, 255, 255, 0.4);
}

.ecg-empty   { background: rgba(255, 255, 255, 0.06); border-color: rgba(255, 255, 255, 0.1); }
.ecg-partial { background: rgba(245, 200, 100, 0.18); border-color: var(--insp-warn); }
.ecg-pass    { background: rgba(60, 180, 100, 0.22); border-color: var(--insp-pass); }
.ecg-fail    { background: rgba(220, 80, 60, 0.22); border-color: var(--insp-fail); }
.ecg-flagged { background: rgba(220, 80, 60, 0.18); border-color: var(--insp-fail); border-style: dashed; }
.ecg-scored  { background: rgba(100, 150, 220, 0.2); border-color: var(--insp-info); }

.ecg-bar.is-cursor {
  background: var(--insp-accent) !important;
  border-color: var(--insp-accent) !important;
  transform: translateY(-2px);
}
.ecg-bar.is-cursor .ecg-bar__num { color: white; }

.ecg-bar.is-cursor::before {
  content: '';
  position: absolute;
  left: 50%; transform: translateX(-50%);
  top: -4px;
  width: 0; height: 0;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 4px solid var(--insp-accent);
}

/* ── Strip KPIs ─────── */
.strip-right { display: flex; align-items: center; gap: var(--insp-sp-5); justify-content: flex-end; }

.kpi {
  display: flex; flex-direction: column;
  text-align: right;
}
.kpi__num {
  font-family: var(--insp-font-mono);
  font-size: 22px; font-weight: 600;
  color: white;
  letter-spacing: -0.025em; line-height: 1;
}
.kpi__num .dim { color: rgba(255, 255, 255, 0.4); font-weight: 400; }
.kpi__label {
  font-family: var(--insp-font-mono);
  font-size: 9px; font-weight: 600;
  letter-spacing: 0.12em;
  color: var(--insp-accent);
  margin-top: 2px;
}

/* ── Mode bar ─────── */
.mode-bar {
  display: flex;
  align-items: stretch;
  background: var(--insp-bg-surface);
  border-bottom: 1px solid var(--insp-border-default);
  flex-shrink: 0;
}

.mode-btn {
  display: flex; flex-direction: column;
  align-items: flex-start; gap: 2px;
  padding: 10px 22px;
  background: transparent;
  border: 0; border-right: 1px solid var(--insp-border-subtle);
  cursor: pointer;
  font-family: inherit;
  text-align: left;
  transition: background var(--insp-t-fast);
}
.mode-btn:hover { background: var(--insp-bg-subtle); }
.mode-btn.is-active {
  background: var(--insp-bg-page);
  box-shadow: inset 0 -3px 0 var(--insp-accent);
}
.mode-btn__name {
  font-family: var(--insp-font-display);
  font-size: 15px; font-weight: 500;
  color: var(--insp-ink-primary);
}
.mode-btn__hint {
  font-size: var(--insp-text-2xs);
  color: var(--insp-ink-tertiary);
  letter-spacing: 0.04em;
}
.mode-spacer { flex: 1; }
.field-counter {
  display: flex; align-items: center;
  padding: 0 var(--insp-sp-5);
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-md); font-weight: 600;
  color: var(--insp-ink-primary);
}
.field-counter .dim { color: var(--insp-ink-quaternary); font-weight: 400; }

/* ── Sweep mode ─────── */
.sweep {
  flex: 1;
  overflow-y: auto;
  padding: var(--insp-sp-7) var(--insp-sp-9);
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
}

.field-head {
  display: flex; align-items: flex-end; justify-content: space-between;
  gap: var(--insp-sp-5);
  margin-bottom: var(--insp-sp-6);
  padding-bottom: var(--insp-sp-4);
  border-bottom: 2px solid var(--insp-ink-primary);
}

.field-head__lead { display: flex; flex-direction: column; gap: 4px; }
.field-name {
  font-family: var(--insp-font-display);
  font-size: 36px; font-weight: 500;
  letter-spacing: var(--insp-tracking-display);
  margin: 0;
  color: var(--insp-ink-primary);
}
.field-meta {
  display: flex; align-items: center; gap: var(--insp-sp-3);
  margin-top: 6px;
}
.field-meta__count {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-md); font-weight: 600;
  color: var(--insp-accent);
}
.field-meta__code {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-tertiary);
}
.field-nav { display: flex; gap: var(--insp-sp-2); }

.sweep-list { list-style: none; margin: 0; padding: 0; }

.sweep-row {
  display: grid;
  grid-template-columns: 48px 1fr auto auto 36px;
  gap: var(--insp-sp-4);
  align-items: center;
  padding: var(--insp-sp-3) var(--insp-sp-4);
  border-bottom: 1px solid var(--insp-border-subtle);
  cursor: pointer;
  transition: background var(--insp-t-fast);
  position: relative;
}
.sweep-row:hover { background: var(--insp-bg-subtle); }
.sweep-row.is-cursor {
  background: var(--insp-bg-subtle);
  box-shadow: inset 4px 0 0 var(--insp-accent);
}

.sweep-row__idx {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-md); font-weight: 600;
  color: var(--insp-ink-tertiary);
}
.sweep-row__name {
  font-size: var(--insp-text-md);
  color: var(--insp-ink-primary);
  font-weight: 500;
}

.grade-btn {
  display: inline-flex; align-items: center; gap: var(--insp-sp-2);
  padding: 8px 14px;
  border: 1px solid var(--insp-border-default);
  background: var(--insp-bg-surface);
  border-radius: var(--insp-radius-md);
  font-family: inherit;
  font-size: var(--insp-text-sm); font-weight: 500;
  color: var(--insp-ink-secondary);
  cursor: pointer;
  transition: all var(--insp-t-fast);
}
.grade-btn:hover {
  border-color: var(--insp-border-strong);
  color: var(--insp-ink-primary);
}
.grade-btn__glyph {
  font-family: var(--insp-font-mono);
  font-size: 18px; font-weight: 700;
  line-height: 1;
}
.grade-btn--pass.is-on {
  background: var(--insp-pass-pale);
  color: var(--insp-pass);
  border-color: var(--insp-pass);
}
.grade-btn--fail.is-on {
  background: var(--insp-fail-pale);
  color: var(--insp-fail);
  border-color: var(--insp-fail);
}

.num-input {
  width: 100px;
  padding: 8px 12px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-md); font-weight: 500;
  text-align: right;
  color: var(--insp-ink-primary);
  background: var(--insp-bg-surface);
}
.num-input:focus {
  outline: none;
  border-color: var(--insp-accent);
  box-shadow: 0 0 0 3px var(--insp-accent-paler);
}

.value-cell {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-tertiary);
}

.flag-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 28px; height: 28px;
  background: transparent;
  border: 1px solid var(--insp-border-subtle);
  border-radius: var(--insp-radius-sm);
  cursor: pointer;
  color: var(--insp-ink-quaternary);
  transition: all var(--insp-t-fast);
}
.flag-btn:hover { color: var(--insp-fail); border-color: var(--insp-fail); }
.flag-btn.is-on {
  color: var(--insp-fail);
  background: var(--insp-fail-pale);
  border-color: var(--insp-fail);
}
.flag-glyph {
  font-size: 11px;
  line-height: 1;
}

/* ── Matrix mode ─────── */
.matrix-wrap {
  flex: 1;
  overflow: hidden;
  padding: var(--insp-sp-5) var(--insp-sp-7);
  background: var(--insp-bg-page);
}
.matrix-scroll {
  width: 100%; height: 100%;
  overflow: auto;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-subtle);
}

.matrix {
  border-collapse: separate;
  border-spacing: 0;
  font-family: var(--insp-font-mono);
  width: max-content;
  min-width: 100%;
}
.matrix th, .matrix td {
  border-right: 1px solid var(--insp-border-subtle);
  border-bottom: 1px solid var(--insp-border-subtle);
}
.matrix thead th {
  position: sticky; top: 0;
  background: var(--insp-bg-subtle);
  z-index: 2;
  padding: var(--insp-sp-3) var(--insp-sp-3);
  cursor: pointer;
  white-space: nowrap;
}
.matrix thead th.is-cursor {
  background: var(--insp-accent-paler);
  box-shadow: inset 0 -3px 0 var(--insp-accent);
}
.m-th { display: flex; flex-direction: column; gap: 2px; align-items: flex-start; }
.m-th__name {
  font-family: var(--insp-font-display);
  font-size: var(--insp-text-md); font-weight: 500;
  color: var(--insp-ink-primary);
  letter-spacing: 0;
}
.m-th__mode {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-2xs);
  color: var(--insp-ink-tertiary);
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.m-corner {
  position: sticky; left: 0; z-index: 3;
  background: var(--insp-bg-subtle);
  font-family: var(--insp-font-display);
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-tertiary);
  font-weight: 400;
  padding: var(--insp-sp-3);
}

.m-th-row {
  position: sticky; left: 0; z-index: 1;
  background: var(--insp-bg-surface);
  padding: var(--insp-sp-2) var(--insp-sp-3);
  text-align: left;
  cursor: pointer;
  white-space: nowrap;
}
.m-th-row.is-cursor, tr.is-cursor .m-th-row { background: var(--insp-accent-paler); }

.m-th-row__num {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-2xs);
  color: var(--insp-ink-quaternary);
  margin-right: 6px;
}
.m-th-row__name {
  font-family: var(--insp-font-body);
  font-weight: 500;
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-primary);
}

.m-cell {
  width: 56px; height: 38px;
  text-align: center;
  vertical-align: middle;
  font-family: var(--insp-font-mono);
  font-size: 16px; font-weight: 600;
  cursor: pointer;
  transition: background var(--insp-t-fast);
}
.cell--empty { color: var(--insp-ink-quaternary); }
.cell--pass { background: var(--insp-pass-pale); color: var(--insp-pass); }
.cell--fail { background: var(--insp-fail-pale); color: var(--insp-fail); }
.cell--skip { color: var(--insp-ink-tertiary); }
.m-cell.is-cursor {
  outline: 2px solid var(--insp-accent);
  outline-offset: -2px;
  z-index: 1;
  position: relative;
}
.m-cell.is-flagged::after {
  content: '▲';
  position: absolute;
  top: 1px; right: 3px;
  font-size: 8px;
  color: var(--insp-fail);
}

/* ── Focus mode ─────── */
.focus {
  flex: 1;
  display: flex; align-items: center; justify-content: center;
  padding: var(--insp-sp-7);
  background: var(--insp-bg-page);
}
.focus-card {
  max-width: 720px; width: 100%;
  display: flex; flex-direction: column;
  gap: var(--insp-sp-5);
  text-align: center;
}
.focus-meta { color: var(--insp-ink-tertiary); }
.focus-target {
  font-family: var(--insp-font-display);
  font-size: 56px; font-weight: 500;
  letter-spacing: -0.025em;
  margin: 0;
  color: var(--insp-ink-primary);
  line-height: 1.05;
}
.focus-field {
  font-family: var(--insp-font-display);
  font-size: 24px; font-weight: 400;
  margin: 0;
  color: var(--insp-ink-secondary);
  letter-spacing: var(--insp-tracking-tight);
}
.focus-grades {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--insp-sp-4);
  margin-top: var(--insp-sp-7);
}
.big-grade {
  display: flex; flex-direction: column; align-items: center; gap: var(--insp-sp-3);
  padding: var(--insp-sp-9) var(--insp-sp-6);
  background: var(--insp-bg-surface);
  border: 2px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  cursor: pointer;
  font-family: inherit;
  transition: all var(--insp-t-fast);
}
.big-grade:hover { transform: translateY(-2px); }
.big-grade__glyph {
  font-family: var(--insp-font-mono);
  font-size: 80px; font-weight: 700;
  line-height: 1;
  color: var(--insp-ink-quaternary);
}
.big-grade__name {
  font-family: var(--insp-font-display);
  font-size: 26px; font-weight: 500;
  color: var(--insp-ink-primary);
}
.big-grade__kbd { margin-top: var(--insp-sp-2); }

.big-grade--pass.is-on {
  background: var(--insp-pass-pale);
  border-color: var(--insp-pass);
}
.big-grade--pass.is-on .big-grade__glyph,
.big-grade--pass.is-on .big-grade__name { color: var(--insp-pass); }

.big-grade--fail.is-on {
  background: var(--insp-fail-pale);
  border-color: var(--insp-fail);
}
.big-grade--fail.is-on .big-grade__glyph,
.big-grade--fail.is-on .big-grade__name { color: var(--insp-fail); }

.focus-input-wrap { padding: var(--insp-sp-7) 0; }
.focus-input {
  width: 100%;
  padding: var(--insp-sp-5);
  font-family: var(--insp-font-display);
  font-size: 32px; font-weight: 400;
  text-align: center;
  border: 0;
  border-bottom: 2px solid var(--insp-border-strong);
  background: transparent;
  color: var(--insp-ink-primary);
  letter-spacing: -0.01em;
}
.focus-input:focus {
  outline: none;
  border-color: var(--insp-accent);
}

.focus-position {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-md);
  color: var(--insp-ink-tertiary);
  margin-top: var(--insp-sp-5);
}

/* ── Command bar ─────── */
.cmd-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--insp-sp-5);
  padding: 10px var(--insp-sp-7);
  background: var(--insp-ink-primary);
  border-top: 1px solid var(--insp-border-strong);
  flex-shrink: 0;
}
.cmd-bar__left {
  display: flex; align-items: center;
  gap: var(--insp-sp-3);
  flex-wrap: wrap;
}
.cmd-pair {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: var(--insp-text-xs);
  color: rgba(255, 255, 255, 0.65);
}
.cmd-pair .insp-kbd {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.2);
  color: white;
}
.cmd-divider {
  width: 1px; height: 18px;
  background: rgba(255, 255, 255, 0.15);
}
.cmd-bar__right { display: flex; align-items: center; gap: var(--insp-sp-3); }
.cmd-tick {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs);
  color: var(--insp-accent);
  font-weight: 600;
  margin-right: var(--insp-sp-2);
}
.cmd-bar__right .insp-btn {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.15);
  color: white;
}
.cmd-bar__right .insp-btn:hover {
  background: rgba(255, 255, 255, 0.12);
}
.cmd-bar__right .insp-btn--accent {
  background: var(--insp-accent);
  border-color: var(--insp-accent);
}

/* ── Remark drawer ─────── */
.remark-drawer {
  position: fixed; right: 0; bottom: 56px;
  width: 360px;
  background: var(--insp-bg-surface);
  border-top: 2px solid var(--insp-accent);
  border-left: 1px solid var(--insp-border-strong);
  box-shadow: var(--insp-shadow-lg);
  padding: var(--insp-sp-4);
  z-index: 200;
}
.remark-drawer__head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: var(--insp-sp-3);
}
.remark-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  font-family: inherit;
  font-size: var(--insp-text-sm);
  resize: vertical;
}
.remark-input:focus {
  outline: none;
  border-color: var(--insp-accent);
  box-shadow: 0 0 0 3px var(--insp-accent-paler);
}

.drawer-enter-active, .drawer-leave-active { transition: transform var(--insp-t-medium), opacity var(--insp-t-medium); }
.drawer-enter-from, .drawer-leave-to { transform: translateY(20px); opacity: 0; }

@media (max-width: 1100px) {
  .strip { grid-template-columns: 1fr; gap: var(--insp-sp-3); }
  .ecg { grid-column: 1; }
  .strip-right { justify-content: flex-start; }
  .field-name { font-size: 28px; }
  .focus-target { font-size: 36px; }
}

@media (max-width: 768px) {
  .cmd-bar { padding: 8px var(--insp-sp-3); }
  .cmd-bar__left { display: none; }
}
</style>
