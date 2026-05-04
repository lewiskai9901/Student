<template>
  <div class="rmx-editor">
    <div class="rmx-toolbar">
      <label class="rmx-label">矩阵尺寸</label>
      <el-select v-model="size" size="small" style="width:120px" @change="resize">
        <el-option label="3 × 3" :value="3" />
        <el-option label="4 × 4" :value="4" />
        <el-option label="5 × 5" :value="5" />
      </el-select>
      <span class="rmx-hint">点击单元格切换风险等级 → 系统据此计算严重度</span>
    </div>

    <table class="rmx-table">
      <thead>
        <tr>
          <th class="rmx-th-corner">
            <span class="rmx-axis-y">概率 ↓</span>
            <span class="rmx-axis-x">影响 →</span>
          </th>
          <th v-for="(label, i) in axesX" :key="`x-${i}`">
            <input v-model="axesX[i]" class="rmx-axis-input" :placeholder="`列 ${i+1}`" />
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, ri) in matrix" :key="`r-${ri}`">
          <th>
            <input v-model="axesY[ri]" class="rmx-axis-input" :placeholder="`行 ${ri+1}`" />
          </th>
          <td v-for="(cell, ci) in row" :key="`c-${ri}-${ci}`"
              :class="['rmx-cell', `rmx-${cell.toLowerCase()}`]"
              @click="cycleCell(ri, ci)">
            {{ cell }}
          </td>
        </tr>
      </tbody>
    </table>

    <div class="rmx-legend">
      <h5 class="rmx-section-title">等级 → 严重度</h5>
      <div class="rmx-levels">
        <div v-for="lv in LEVELS" :key="lv" class="rmx-level-row">
          <span :class="['rmx-chip', `rmx-${lv.toLowerCase()}`]">{{ lv }}</span>
          <el-input-number
            v-model="levelToSeverity[lv]"
            :min="0" :max="1" :step="0.05" :precision="2" size="small"
            style="width:120px" />
          <span class="rmx-level-hint">{{ severityHint(levelToSeverity[lv]) }}</span>
        </div>
      </div>

      <h5 class="rmx-section-title rmx-trigger-title">建议触发线</h5>
      <el-radio-group v-model="triggerLevel" size="small">
        <el-radio v-for="lv in LEVELS" :key="`t-${lv}`" :value="lv">{{ lv }} 及以上</el-radio>
        <el-radio value="">不限制 (引擎全自主)</el-radio>
      </el-radio-group>
    </div>

    <div class="rmx-preview">
      <h5 class="rmx-section-title">JSON 预览</h5>
      <pre class="rmx-json">{{ jsonPreview }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

interface Props {
  modelValue?: string  // JSON 字符串
}
const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', json: string): void
}>()

type Level = 'L' | 'M' | 'H' | 'VH'
const LEVELS: Level[] = ['L', 'M', 'H', 'VH']

const size = ref(4)
const axesX = ref<string[]>(['轻微', '一般', '严重', '灾难'])
const axesY = ref<string[]>(['极低', '低', '中', '高'])
const matrix = ref<Level[][]>(defaultMatrix(4))
const levelToSeverity = ref<Record<Level, number>>({ L: 0, M: 0.4, H: 0.75, VH: 1.0 })
const triggerLevel = ref<string>('M')

function defaultMatrix(n: number): Level[][] {
  const grid: Level[][] = []
  for (let r = 0; r < n; r++) {
    const row: Level[] = []
    for (let c = 0; c < n; c++) {
      const sum = r + c
      const max = (n - 1) * 2
      const ratio = sum / max
      row.push(ratio < 0.34 ? 'L' : ratio < 0.65 ? 'M' : ratio < 0.85 ? 'H' : 'VH')
    }
    grid.push(row)
  }
  return grid
}

function cycleCell(r: number, c: number) {
  const idx = LEVELS.indexOf(matrix.value[r][c])
  matrix.value[r][c] = LEVELS[(idx + 1) % LEVELS.length]
}

function resize(n: number) {
  axesX.value = Array.from({ length: n }, (_, i) => axesX.value[i] || `列 ${i + 1}`)
  axesY.value = Array.from({ length: n }, (_, i) => axesY.value[i] || `行 ${i + 1}`)
  matrix.value = defaultMatrix(n)
}

function severityHint(v: number) {
  if (v >= 0.85) return '极严重'
  if (v >= 0.6)  return '严重'
  if (v >= 0.3)  return '中等'
  if (v > 0)     return '轻微'
  return '无'
}

const jsonPreview = computed(() => {
  const data = {
    axes: { probability: axesY.value, impact: axesX.value },
    matrix: matrix.value.map(row => row.map(l => ({ level: l }))),
    levelToSeverity: levelToSeverity.value,
    correctiveTrigger: triggerLevel.value ? { fromLevel: triggerLevel.value } : undefined,
  }
  return JSON.stringify(data, null, 2)
})

watch(jsonPreview, v => emit('update:modelValue', v), { immediate: true })

// 反向加载已存在配置
watch(() => props.modelValue, raw => {
  if (!raw) return
  try {
    const obj = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (obj?.matrix?.length) {
      const n = obj.matrix.length
      size.value = n
      matrix.value = obj.matrix.map((row: any[]) =>
        row.map(c => (typeof c === 'string' ? c : c?.level || 'L') as Level)
      )
      if (obj.axes?.impact) axesX.value = obj.axes.impact
      if (obj.axes?.probability) axesY.value = obj.axes.probability
    }
    if (obj?.levelToSeverity) {
      LEVELS.forEach(lv => {
        if (obj.levelToSeverity[lv] != null)
          levelToSeverity.value[lv] = Number(obj.levelToSeverity[lv])
      })
    }
    if (obj?.correctiveTrigger?.fromLevel != null) {
      triggerLevel.value = obj.correctiveTrigger.fromLevel
    }
  } catch { /* ignore */ }
}, { immediate: true })
</script>

<style scoped>
.rmx-editor { display: flex; flex-direction: column; gap: 14px; }
.rmx-toolbar {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 0;
}
.rmx-label { font-size: 13px; color: #374151; }
.rmx-hint { font-size: 12px; color: #9ca3af; margin-left: auto; }

.rmx-table {
  border-collapse: collapse;
  width: 100%;
  table-layout: fixed;
}
.rmx-table th, .rmx-table td {
  border: 1px solid #e5e7eb;
  padding: 8px;
  text-align: center;
  font-size: 13px;
}
.rmx-table th { background: #f9fafb; font-weight: 500; color: #374151; }
.rmx-th-corner {
  background: #f3f4f6 !important;
  position: relative;
  height: 56px;
}
.rmx-axis-y { display: block; font-size: 11px; color: #6b7280; }
.rmx-axis-x { display: block; font-size: 11px; color: #6b7280; }
.rmx-axis-input {
  width: 100%; border: none; background: transparent;
  text-align: center; font-size: 12px; color: #4b5563;
  outline: none;
}
.rmx-axis-input:focus { background: #fff; border: 1px solid #93c5fd; border-radius: 3px; }

.rmx-cell {
  cursor: pointer;
  font-weight: 600; font-size: 13px;
  user-select: none;
  transition: filter 0.1s;
}
.rmx-cell:hover { filter: brightness(0.92); }
.rmx-l  { background: #d1fae5; color: #065f46; }
.rmx-m  { background: #fef3c7; color: #92400e; }
.rmx-h  { background: #fed7aa; color: #9a3412; }
.rmx-vh { background: #fecaca; color: #991b1b; }

.rmx-section-title {
  margin: 0 0 8px;
  font-size: 13px; font-weight: 600; color: #111827;
}
.rmx-trigger-title { margin-top: 14px; }
.rmx-legend { padding: 12px; background: #f9fafb; border-radius: 6px; }
.rmx-levels { display: flex; flex-direction: column; gap: 8px; }
.rmx-level-row { display: flex; align-items: center; gap: 10px; }
.rmx-chip {
  display: inline-block;
  min-width: 36px; padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px; font-weight: 600;
  text-align: center;
}
.rmx-level-hint { font-size: 12px; color: #6b7280; }

.rmx-preview {
  padding: 12px; background: #1f2937; border-radius: 6px;
}
.rmx-preview .rmx-section-title { color: #f3f4f6; }
.rmx-json {
  margin: 0; color: #d1d5db; font-size: 11px;
  font-family: ui-monospace, monospace;
  max-height: 200px; overflow: auto;
}
</style>
