<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { Play, RotateCcw, Calculator } from 'lucide-vue-next'
import type { ScoringProfile } from '@/types/insp/scoring'

// ==================== Props ====================

const props = defineProps<{
  scoringProfile: ScoringProfile
}>()

// ==================== State ====================

const testInputs = reactive({
  itemScores: '80, 90, 75, 85',
  population: 40,
  deductions: 5,
  bonuses: 2,
  customDivisor: 1,
})

const result = ref<TestResult | null>(null)
const hasError = ref(false)
const errorMessage = ref('')

// ==================== Types ====================

interface TestResult {
  rawScore: number
  normalizedScore: number
  grade: string
  gradeColor: string
  steps: string[]
}

// ==================== Computed ====================

const profile = computed(() => props.scoringProfile)

const parsedItemScores = computed<number[]>(() => {
  return testInputs.itemScores
    .split(/[,，\s]+/)
    .map(s => s.trim())
    .filter(s => s !== '')
    .map(Number)
    .filter(n => !isNaN(n))
})

const itemCount = computed(() => parsedItemScores.value.length)

// ==================== Test Calculation ====================

function runTest() {
  hasError.value = false
  errorMessage.value = ''
  result.value = null

  try {
    const scores = parsedItemScores.value
    if (scores.length === 0) {
      throw new Error('请输入至少一个项目分数')
    }

    const steps: string[] = []
    const maxScore = profile.value.maxScore
    const minScore = profile.value.minScore

    steps.push(`分区内聚合: SUM，分区间聚合: WEIGHTED_AVG`)

    // Step 1: SUM item scores within dimension
    let rawScore = scores.reduce((sum, s) => sum + s, 0)
    steps.push(`SUM(${scores.join(', ')}) = ${rawScore}`)

    // Step 2: Apply deductions and bonuses
    const afterAdjust = rawScore - testInputs.deductions + testInputs.bonuses
    if (testInputs.deductions > 0 || testInputs.bonuses > 0) {
      steps.push(`${rawScore} - ${testInputs.deductions}(扣分) + ${testInputs.bonuses}(加分) = ${afterAdjust}`)
    }

    // Step 3: Clamp to bounds
    let clamped = afterAdjust
    if (clamped > maxScore) {
      clamped = maxScore
      steps.push(`超过上限 ${maxScore}，封顶`)
    }
    if (clamped < minScore) {
      clamped = minScore
      steps.push(`低于下限 ${minScore}，保底`)
    }

    // Step 4: Normalization (simple simulation)
    let normalizedScore = clamped
    if (testInputs.population > 1) {
      normalizedScore = clamped / testInputs.population
      steps.push(`人均归一化: ${clamped} / ${testInputs.population} = ${Math.round(normalizedScore * 100) / 100}`)
    }

    // Step 5: Round to precision
    const precision = profile.value.precisionDigits
    normalizedScore = Number(normalizedScore.toFixed(precision))
    rawScore = Number(clamped.toFixed(precision))
    steps.push(`精度: ${precision} 位小数`)

    // Step 6: Determine grade (simple thresholds for demo)
    let grade = '未知'
    let gradeColor = '#909399'
    const pct = maxScore > 0 ? (rawScore / maxScore * 100) : 0
    if (pct >= 90) { grade = '优秀'; gradeColor = '#67C23A' }
    else if (pct >= 80) { grade = '良好'; gradeColor = '#409EFF' }
    else if (pct >= 60) { grade = '合格'; gradeColor = '#E6A23C' }
    else { grade = '不合格'; gradeColor = '#F56C6C' }
    steps.push(`得分率: ${Math.round(pct)}%，等级: ${grade}`)

    result.value = {
      rawScore,
      normalizedScore,
      grade,
      gradeColor,
      steps,
    }
  } catch (e: any) {
    hasError.value = true
    errorMessage.value = e.message || '计算失败'
  }
}

function reset() {
  testInputs.itemScores = '80, 90, 75, 85'
  testInputs.population = 40
  testInputs.deductions = 5
  testInputs.bonuses = 2
  testInputs.customDivisor = 1
  result.value = null
  hasError.value = false
  errorMessage.value = ''
}
</script>

<template>
  <div class="stp-root">
    <div class="stp-header">
      <div class="stp-header-left">
        <Calculator :size="16" class="stp-header-icon" />
        <h3 class="stp-title">评分测试</h3>
      </div>
      <div class="stp-actions">
        <button class="sp-btn-ghost sm" @click="reset">
          <RotateCcw :size="12" /> 重置
        </button>
        <button class="sp-btn-primary sm" @click="runTest">
          <Play :size="12" /> 计算
        </button>
      </div>
    </div>

    <!-- Profile summary -->
    <div class="stp-profile">
      <div class="stp-profile-line">范围: {{ profile.minScore }} ~ {{ profile.maxScore }} | 精度: {{ profile.precisionDigits }}位</div>
      <div class="stp-profile-line">分区内: SUM | 分区间: WEIGHTED_AVG</div>
    </div>

    <!-- Input section -->
    <div class="stp-inputs">
      <div class="sp-fld">
        <div class="stp-label-row">
          <label>项目分数 (逗号分隔)</label>
          <span class="stp-count">共 {{ itemCount }} 项</span>
        </div>
        <textarea
          v-model="testInputs.itemScores"
          rows="2"
          placeholder="80, 90, 75, 85"
        />
      </div>

      <div class="stp-grid-2">
        <div class="sp-fld">
          <label>人数 (归一化用)</label>
          <input v-model.number="testInputs.population" type="number" :min="1" />
        </div>
        <div class="sp-fld">
          <label>自定义除数</label>
          <input v-model.number="testInputs.customDivisor" type="number" :min="1" />
        </div>
      </div>

      <div class="stp-grid-2">
        <div class="sp-fld">
          <label>扣分合计</label>
          <input v-model.number="testInputs.deductions" type="number" :min="0" />
        </div>
        <div class="sp-fld">
          <label>加分合计</label>
          <input v-model.number="testInputs.bonuses" type="number" :min="0" />
        </div>
      </div>
    </div>

    <!-- Error -->
    <div v-if="hasError" class="stp-error">
      {{ errorMessage }}
    </div>

    <!-- Result -->
    <div v-if="result" class="stp-result">
      <div class="stp-scores">
        <div class="stp-score-item">
          <div class="stp-score-label">原始分</div>
          <div class="stp-score-value">{{ result.rawScore }}</div>
        </div>
        <div class="stp-score-item">
          <div class="stp-score-label">归一化分</div>
          <div class="stp-score-value stp-score-blue">{{ result.normalizedScore }}</div>
        </div>
        <div class="stp-score-item">
          <div class="stp-score-label">等级</div>
          <div class="stp-score-value" :style="{ color: result.gradeColor }">
            {{ result.grade }}
          </div>
        </div>
      </div>

      <div class="stp-steps">
        <div class="stp-steps-label">计算步骤:</div>
        <div
          v-for="(step, idx) in result.steps"
          :key="idx"
          class="stp-step-row"
        >
          <span class="stp-step-num">{{ idx + 1 }}</span>
          <span class="stp-step-text">{{ step }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== sp-* shared styles (subset) ===== */
.sp-btn-primary {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 8px 16px; background: #1a6dff; color: #fff;
  border: none; border-radius: 8px; font-size: 13px; font-weight: 500;
  cursor: pointer; transition: background 0.15s; white-space: nowrap;
}
.sp-btn-primary:hover { background: #1558d6; }
.sp-btn-primary.sm { padding: 6px 12px; font-size: 12px; border-radius: 6px; }

.sp-btn-ghost {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 8px 16px; background: none; border: 1px solid #dce1e8;
  border-radius: 8px; font-size: 13px; color: #5a6474;
  cursor: pointer; transition: background 0.15s;
}
.sp-btn-ghost:hover { background: #f4f6f9; }
.sp-btn-ghost.sm { padding: 6px 12px; font-size: 12px; border-radius: 6px; }

.sp-fld label {
  display: block; font-size: 12px; font-weight: 500;
  color: #5a6474; margin-bottom: 5px;
}
.sp-fld input,
.sp-fld select,
.sp-fld textarea {
  width: 100%; border: 1px solid #dce1e8; border-radius: 8px;
  padding: 8px 12px; font-size: 13px; outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  color: #1e2a3a; background: #fff;
}
.sp-fld input::placeholder,
.sp-fld textarea::placeholder { color: #b8c0cc; }
.sp-fld input:focus,
.sp-fld select:focus,
.sp-fld textarea:focus {
  border-color: #7aadff;
  box-shadow: 0 0 0 3px rgba(26,109,255,0.08);
}

/* ===== Root & layout ===== */
.stp-root { display: flex; flex-direction: column; gap: 16px; }

.stp-header { display: flex; align-items: center; justify-content: space-between; }
.stp-header-left { display: flex; align-items: center; gap: 8px; }
.stp-header-icon { color: #8c95a3; }
.stp-title { font-size: 14px; font-weight: 500; color: #1e2a3a; margin: 0; }
.stp-actions { display: flex; align-items: center; gap: 8px; }

/* ===== Profile summary (blue left border card) ===== */
.stp-profile {
  border-left: 3px solid #1a6dff;
  background: #f8f9fb;
  border-radius: 0 8px 8px 0;
  padding: 12px 16px;
  display: flex; flex-direction: column; gap: 2px;
}
.stp-profile-line { font-size: 12px; color: #5a6474; }

/* ===== Input section ===== */
.stp-inputs { display: flex; flex-direction: column; gap: 12px; }

.stp-label-row {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 5px;
}
.stp-label-row label { margin-bottom: 0; }
.stp-count { font-size: 10px; color: #8c95a3; }

.stp-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

/* ===== Error (red left border card) ===== */
.stp-error {
  border-left: 3px solid #d93025;
  background: #fef2f2;
  border-radius: 0 8px 8px 0;
  padding: 12px 16px;
  font-size: 12px;
  color: #b91c1c;
}

/* ===== Result (green left border card) ===== */
.stp-result {
  border-left: 3px solid #34d399;
  background: #f0fdf4;
  border-radius: 0 8px 8px 0;
  padding: 12px 16px;
  display: flex; flex-direction: column; gap: 12px;
}

.stp-scores { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; }
.stp-score-item { text-align: center; }
.stp-score-label { font-size: 10px; color: #8c95a3; margin-bottom: 2px; }
.stp-score-value { font-size: 20px; font-weight: 700; color: #1e2a3a; }
.stp-score-blue { color: #1a6dff; }

/* Steps */
.stp-steps { border-top: 1px solid #bbf7d0; padding-top: 10px; }
.stp-steps-label { font-size: 10px; color: #8c95a3; margin-bottom: 6px; }

.stp-step-row {
  display: flex; align-items: baseline; gap: 8px;
  padding: 3px 0;
}
.stp-step-num {
  display: inline-flex; align-items: center; justify-content: center;
  width: 18px; height: 18px; flex-shrink: 0;
  background: #1a6dff; color: #fff;
  border-radius: 50%; font-size: 10px; font-weight: 600;
}
.stp-step-text { font-size: 12px; color: #374151; font-family: monospace; }
</style>
