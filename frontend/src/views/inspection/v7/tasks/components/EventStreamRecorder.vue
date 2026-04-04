<script setup lang="ts">
/**
 * EventStreamRecorder — generalized event-stream scoring component
 *
 * Supports all 3 target types (ORG / USER / PLACE) and all scoring modes.
 * Flow: Search target → select → score all items → save → next target
 */
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage, ElInputNumber } from 'element-plus'
import { Search } from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/insp/inspExecutionStore'
import { getSimpleUserList } from '@/api/user'
import { http } from '@/utils/request'
import type { InspSubmission, SubmissionDetail, UpdateDetailResponseRequest } from '@/types/insp/project'
import type { ScoringMode } from '@/types/insp/enums'

// ==================== Props ====================

interface Props {
  sectionId: number
  targetType: string  // 'ORG' | 'USER' | 'PLACE'
  items: SubmissionDetail[]
  submissions: InspSubmission[]
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
})

const store = useInspExecutionStore()

// ==================== Search ====================

interface SearchResult {
  id: number | string
  name: string
  subtitle: string
}

const searchKeyword = ref('')
const searchRef = ref<any>(null)
const searchResults = ref<SearchResult[]>([])
const searchLoading = ref(false)

const searchPlaceholder = computed(() => {
  switch (props.targetType) {
    case 'ORG': return '搜索组织...'
    case 'USER': return '搜索人员...'
    case 'PLACE': return '搜索场所...'
    default: return '搜索...'
  }
})

let searchTimer: ReturnType<typeof setTimeout> | null = null

watch(searchKeyword, (val) => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!val || val.length < 1) {
    searchResults.value = []
    return
  }
  searchTimer = setTimeout(() => doSearch(val), 300)
})

async function doSearch(keyword: string) {
  searchLoading.value = true
  try {
    let results: SearchResult[] = []
    switch (props.targetType) {
      case 'ORG': {
        const list = await http.get<any[]>('/org-units', { params: { keyword } })
        results = (Array.isArray(list) ? list : []).map(o => ({
          id: o.id,
          name: o.unitName || o.name || '',
          subtitle: o.unitType || o.typeName || '',
        }))
        break
      }
      case 'USER': {
        const users = await getSimpleUserList(keyword)
        results = (users || []).map(u => ({
          id: u.id,
          name: u.realName || u.username || '',
          subtitle: u.orgUnitName || '',
        }))
        break
      }
      case 'PLACE': {
        const list = await http.get<any[]>('/v9/places', { params: { keyword } })
        results = (Array.isArray(list) ? list : []).map(p => ({
          id: p.id,
          name: p.placeName || p.name || '',
          subtitle: p.typeLabel || p.placeType || '',
        }))
        break
      }
    }
    searchResults.value = results.slice(0, 10)
  } catch {
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

// ==================== Selected Target ====================

const selectedResult = ref<SearchResult | null>(null)

// Per-item score state (keyed by item detail id)
const scoreValues = ref<Record<number, string>>({})
const numScores = ref<Record<number, number>>({})

function selectTarget(result: SearchResult) {
  selectedResult.value = result
  searchResults.value = []
  searchKeyword.value = ''
  // Init score state for all items
  scoreValues.value = {}
  numScores.value = {}
  for (const item of props.items) {
    // Check if this target already has a submission with saved scores
    const sub = findSubmission(result)
    if (sub) {
      // Load existing values from details if available
      const existingDetail = props.items.find(d => d.id === item.id)
      if (existingDetail?.responseValue) {
        scoreValues.value[item.id] = existingDetail.responseValue
        const parsed = parseFloat(existingDetail.responseValue)
        if (!isNaN(parsed)) numScores.value[item.id] = parsed
      }
    }
    // Set defaults for steppers
    if (numScores.value[item.id] === undefined) {
      const mode = item.scoringMode
      if (mode === 'DEDUCTION' || mode === 'ADDITION' || mode === 'CUMULATIVE') {
        numScores.value[item.id] = 0
      } else if (mode === 'DIRECT') {
        numScores.value[item.id] = 0
      }
    }
  }
}

function findSubmission(result: SearchResult): InspSubmission | undefined {
  return props.submissions.find(s => String(s.targetId) === String(result.id))
}

// ==================== Scoring Helpers ====================

function getScore(item: SubmissionDetail): string {
  return scoreValues.value[item.id] || ''
}

function getNumScore(item: SubmissionDetail): number {
  return numScores.value[item.id] ?? 0
}

function setScore(item: SubmissionDetail, label: string, score?: number) {
  scoreValues.value[item.id] = label
  if (score !== undefined) {
    numScores.value[item.id] = score
  }
}

function stepScore(item: SubmissionDetail, delta: number) {
  const current = numScores.value[item.id] ?? 0
  const next = Math.max(0, current + delta)
  numScores.value[item.id] = next
  scoreValues.value[item.id] = String(next)
}

/**
 * Parse levels from scoringConfig JSON for LEVEL / SCORE_TABLE / TIERED_DEDUCTION
 */
function getLevels(item: SubmissionDetail): Array<{ label: string; score: number }> {
  try {
    const cfg = JSON.parse(item.scoringConfig || '{}')
    // Common patterns: levels array, tiers array, bands array, options array
    const arr = cfg.levels || cfg.tiers || cfg.bands || cfg.options || []
    return arr.map((lv: any) => ({
      label: lv.label || lv.name || lv.grade || String(lv.score ?? ''),
      score: lv.score ?? lv.value ?? 0,
    }))
  } catch {
    return []
  }
}

/**
 * Get max stars for RATING_SCALE
 */
function getMaxStars(item: SubmissionDetail): number {
  try {
    const cfg = JSON.parse(item.scoringConfig || '{}')
    return cfg.maxStars || cfg.maxScore || 5
  } catch {
    return 5
  }
}

// ==================== Recorded Targets ====================

interface RecordedTarget {
  targetId: number | string
  targetName: string
  summary: string
}

const recordedTargets = ref<RecordedTarget[]>([])

// ==================== Save ====================

const saving = ref(false)

async function saveAndNext() {
  if (!selectedResult.value || props.disabled) return
  saving.value = true
  try {
    let savedCount = 0
    for (const item of props.items) {
      const mode = item.scoringMode
      if (!mode) continue

      let responseValue = scoreValues.value[item.id] || ''
      let score: number | undefined

      // Compute score based on scoring mode
      if (mode === 'PASS_FAIL') {
        if (!responseValue) continue // not scored
        score = responseValue === 'PASS' ? (getMaxFromConfig(item) ?? 100) : 0
      } else if (mode === 'DEDUCTION') {
        const val = numScores.value[item.id] ?? 0
        responseValue = String(val)
        score = -Math.abs(val)
      } else if (mode === 'ADDITION' || mode === 'CUMULATIVE') {
        const val = numScores.value[item.id] ?? 0
        responseValue = String(val)
        score = val
      } else if (mode === 'DIRECT') {
        const val = numScores.value[item.id]
        if (val === undefined) continue
        responseValue = String(val)
        score = val
      } else if (mode === 'LEVEL' || mode === 'SCORE_TABLE' || mode === 'TIERED_DEDUCTION') {
        if (!responseValue) continue
        const levels = getLevels(item)
        const found = levels.find(l => l.label === responseValue)
        score = found ? found.score : undefined
      } else if (mode === 'RATING_SCALE') {
        const val = numScores.value[item.id]
        if (val === undefined) continue
        responseValue = String(val)
        score = val
      } else {
        // Fallback: just save responseValue
        if (!responseValue && numScores.value[item.id] !== undefined) {
          responseValue = String(numScores.value[item.id])
          score = numScores.value[item.id]
        }
      }

      if (!responseValue) continue

      const data: UpdateDetailResponseRequest = {
        responseValue,
        scoringMode: mode as ScoringMode,
        ...(score !== undefined ? { score } : {}),
      }

      try {
        await store.updateDetailResponse(item.id, data)
        savedCount++
      } catch (e: any) {
        console.error(`Failed to save item ${item.itemName}:`, e)
      }
    }

    if (savedCount > 0) {
      // Build summary
      const scoredItems = props.items.filter(i => scoreValues.value[i.id] || numScores.value[i.id] !== undefined)
      const summary = scoredItems
        .map(i => {
          const v = scoreValues.value[i.id] || String(numScores.value[i.id] ?? '')
          return `${i.itemName}: ${v}`
        })
        .join(', ')

      recordedTargets.value.unshift({
        targetId: selectedResult.value.id,
        targetName: selectedResult.value.name,
        summary: summary || `${savedCount} 项已评分`,
      })

      ElMessage.success(`${selectedResult.value.name}: ${savedCount} 项已保存`)
    } else {
      ElMessage.warning('没有需要保存的评分')
    }

    // Clear selection for next target
    selectedResult.value = null
    scoreValues.value = {}
    numScores.value = {}

    // Re-focus search
    await nextTick()
    const inputEl = searchRef.value?.$el?.querySelector('input') as HTMLInputElement | null
    inputEl?.focus()
  } catch (e: any) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

function getMaxFromConfig(item: SubmissionDetail): number | null {
  try {
    const cfg = JSON.parse(item.scoringConfig || '{}')
    return cfg.maxScore ?? cfg.passScore ?? null
  } catch {
    return null
  }
}

function closePanel() {
  selectedResult.value = null
  scoreValues.value = {}
  numScores.value = {}
}
</script>

<template>
  <div class="esr">
    <!-- Search bar -->
    <div class="esr-search">
      <el-input
        ref="searchRef"
        v-model="searchKeyword"
        :placeholder="searchPlaceholder"
        size="default"
        clearable
        :disabled="disabled || !!selectedResult"
      >
        <template #prefix>
          <Search :size="16" class="text-gray-400" />
        </template>
      </el-input>

      <!-- Search results dropdown -->
      <div v-if="searchResults.length > 0 && !selectedResult" class="esr-results">
        <div
          v-for="result in searchResults"
          :key="String(result.id)"
          class="esr-result"
          @click="selectTarget(result)"
        >
          <span class="esr-result-name">{{ result.name }}</span>
          <span class="esr-result-sub">{{ result.subtitle }}</span>
        </div>
      </div>
      <div v-else-if="searchKeyword && searchResults.length === 0 && !searchLoading && !selectedResult" class="esr-no-results">
        无匹配结果
      </div>
    </div>

    <!-- Scoring panel -->
    <div v-if="selectedResult" class="esr-scoring">
      <div class="esr-scoring-header">
        <span class="esr-scoring-name">{{ selectedResult.name }}</span>
        <span v-if="selectedResult.subtitle" class="esr-scoring-sub">{{ selectedResult.subtitle }}</span>
        <button class="esr-scoring-close" @click="closePanel">&times;</button>
      </div>
      <div class="esr-scoring-items">
        <div v-for="item in items" :key="item.id" class="esr-score-row">
          <span class="esr-item-name">{{ item.itemName }}</span>
          <div class="esr-item-control">
            <!-- PASS_FAIL -->
            <template v-if="item.scoringMode === 'PASS_FAIL'">
              <button
                class="esr-pill pass"
                :class="{ active: getScore(item) === 'PASS' }"
                @click="setScore(item, 'PASS')"
              >&#10003;</button>
              <button
                class="esr-pill fail"
                :class="{ active: getScore(item) === 'FAIL' }"
                @click="setScore(item, 'FAIL')"
              >&#10007;</button>
            </template>

            <!-- DEDUCTION / ADDITION / CUMULATIVE -->
            <template v-else-if="item.scoringMode && ['DEDUCTION','ADDITION','CUMULATIVE'].includes(item.scoringMode)">
              <div class="esr-stepper">
                <button @click="stepScore(item, -1)" :disabled="getNumScore(item) <= 0">&minus;</button>
                <span class="esr-stepper-val">{{ getNumScore(item) }}</span>
                <button @click="stepScore(item, 1)">+</button>
              </div>
            </template>

            <!-- LEVEL / SCORE_TABLE / TIERED_DEDUCTION -->
            <template v-else-if="item.scoringMode && ['LEVEL','SCORE_TABLE','TIERED_DEDUCTION'].includes(item.scoringMode)">
              <button
                v-for="lv in getLevels(item)"
                :key="lv.label"
                class="esr-pill level"
                :class="{ active: getScore(item) === lv.label }"
                @click="setScore(item, lv.label, lv.score)"
              >{{ lv.label }}</button>
              <span v-if="getLevels(item).length === 0" class="esr-hint">无等级配置</span>
            </template>

            <!-- DIRECT -->
            <template v-else-if="item.scoringMode === 'DIRECT'">
              <el-input-number
                v-model="numScores[item.id]"
                size="small"
                :min="0"
                :max="100"
                :step="1"
                controls-position="right"
                style="width: 100px"
              />
            </template>

            <!-- RATING_SCALE -->
            <template v-else-if="item.scoringMode === 'RATING_SCALE'">
              <div class="esr-stars">
                <button
                  v-for="s in getMaxStars(item)"
                  :key="s"
                  class="esr-star"
                  :class="{ active: (numScores[item.id] ?? 0) >= s }"
                  @click="numScores[item.id] = s; scoreValues[item.id] = String(s)"
                >&#9733;</button>
              </div>
            </template>

            <!-- Fallback: text input for other modes -->
            <template v-else>
              <el-input-number
                v-model="numScores[item.id]"
                size="small"
                :min="0"
                controls-position="right"
                style="width: 100px"
              />
            </template>
          </div>
        </div>
        <div v-if="items.length === 0" class="esr-hint" style="padding: 12px 0; text-align: center">
          该分区暂无检查项
        </div>
      </div>
      <button
        class="esr-save-btn"
        :disabled="saving || disabled"
        @click="saveAndNext"
      >
        {{ saving ? '保存中...' : '保存并继续' }}
      </button>
    </div>

    <!-- Record log -->
    <div class="esr-log">
      <div class="esr-log-header">
        已记录 <strong>{{ recordedTargets.length }}</strong> 个目标
      </div>
      <div v-for="rec in recordedTargets" :key="String(rec.targetId)" class="esr-log-item">
        <span class="esr-log-name">{{ rec.targetName }}</span>
        <span class="esr-log-summary">{{ rec.summary }}</span>
      </div>
      <div v-if="recordedTargets.length === 0" class="esr-empty">
        搜索目标开始记录
      </div>
    </div>
  </div>
</template>

<style scoped>
.esr {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* ========== Search ========== */
.esr-search {
  position: relative;
}

.esr-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 80;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.08), 0 4px 6px -4px rgba(0, 0, 0, 0.04);
  margin-top: 4px;
  max-height: 280px;
  overflow-y: auto;
}

.esr-result {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.1s;
  border-bottom: 1px solid #f3f4f6;
}

.esr-result:last-child {
  border-bottom: none;
}

.esr-result:hover {
  background: #eff6ff;
}

.esr-result-name {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
}

.esr-result-sub {
  font-size: 12px;
  color: #9ca3af;
  margin-left: 8px;
  flex-shrink: 0;
}

.esr-no-results {
  padding: 10px;
  text-align: center;
  font-size: 12px;
  color: #9ca3af;
}

/* ========== Scoring Panel ========== */
.esr-scoring {
  border: 1px solid #e0e7ff;
  border-radius: 8px;
  background: #f8faff;
  overflow: hidden;
}

.esr-scoring-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #eef2ff;
  border-bottom: 1px solid #e0e7ff;
}

.esr-scoring-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e3a5f;
}

.esr-scoring-sub {
  font-size: 12px;
  color: #6b7280;
}

.esr-scoring-close {
  margin-left: auto;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  border: none;
  background: transparent;
  font-size: 16px;
  line-height: 1;
  color: #9ca3af;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.12s;
}

.esr-scoring-close:hover {
  background: #e5e7eb;
  color: #374151;
}

.esr-scoring-items {
  padding: 6px 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 320px;
  overflow-y: auto;
}

.esr-score-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 5px 0;
  border-bottom: 1px solid #f1f3f9;
}

.esr-score-row:last-child {
  border-bottom: none;
}

.esr-item-name {
  font-size: 13px;
  color: #374151;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.esr-item-control {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

/* ========== Pill Buttons ========== */
.esr-pill {
  padding: 3px 10px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #fff;
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.12s;
  white-space: nowrap;
  line-height: 1.4;
}

.esr-pill:hover {
  border-color: #d1d5db;
  background: #f9fafb;
}

.esr-pill.pass.active {
  background: #dcfce7;
  border-color: #86efac;
  color: #166534;
}

.esr-pill.fail.active {
  background: #fee2e2;
  border-color: #fca5a5;
  color: #991b1b;
}

.esr-pill.level.active {
  background: #dbeafe;
  border-color: #93c5fd;
  color: #1e40af;
}

/* ========== Stepper ========== */
.esr-stepper {
  display: inline-flex;
  align-items: center;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}

.esr-stepper button {
  width: 26px;
  height: 26px;
  border: none;
  background: #f9fafb;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.1s;
}

.esr-stepper button:hover:not(:disabled) {
  background: #e5e7eb;
}

.esr-stepper button:disabled {
  color: #d1d5db;
  cursor: not-allowed;
}

.esr-stepper-val {
  min-width: 28px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  border-left: 1px solid #e5e7eb;
  border-right: 1px solid #e5e7eb;
  padding: 0 2px;
  line-height: 26px;
}

/* ========== Stars ========== */
.esr-stars {
  display: flex;
  gap: 2px;
}

.esr-star {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  font-size: 18px;
  color: #d1d5db;
  cursor: pointer;
  padding: 0;
  line-height: 1;
  transition: color 0.1s;
}

.esr-star.active {
  color: #f59e0b;
}

.esr-star:hover {
  color: #fbbf24;
}

/* ========== Save Button ========== */
.esr-save-btn {
  width: 100%;
  padding: 8px 0;
  border: none;
  background: #3b82f6;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.esr-save-btn:hover:not(:disabled) {
  background: #2563eb;
}

.esr-save-btn:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}

/* ========== Record Log ========== */
.esr-log {
  border-top: 1px solid #f3f4f6;
  padding-top: 8px;
}

.esr-log-header {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 6px;
}

.esr-log-header strong {
  color: #1f2937;
}

.esr-log-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 4px 0;
  border-bottom: 1px solid #f9fafb;
}

.esr-log-item:last-child {
  border-bottom: none;
}

.esr-log-name {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  flex-shrink: 0;
}

.esr-log-summary {
  font-size: 12px;
  color: #9ca3af;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.esr-empty {
  padding: 16px;
  text-align: center;
  font-size: 12px;
  color: #d1d5db;
}

.esr-hint {
  font-size: 11px;
  color: #9ca3af;
}
</style>
