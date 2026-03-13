<template>
  <div class="sp-section">
    <div class="sp-section-header">
      <h3 class="sp-section-title">等级映射</h3>
      <div class="sp-header-actions">
        <!-- Preset buttons -->
        <div class="sp-preset-group">
          <button
            v-for="preset in PRESETS"
            :key="preset.key"
            class="sp-pill"
            @click="applyPreset(preset)"
          >
            {{ preset.label }}
          </button>
        </div>
        <button
          class="sp-btn-primary sm"
          @click="showAdd = true"
        >
          添加等级
        </button>
      </div>
    </div>

    <!-- Dimension filter -->
    <div v-if="dimensions.length > 0" class="sp-filter-row">
      <div class="sp-fld">
        <label style="margin-bottom:0">筛选:</label>
      </div>
      <div class="sp-fld" style="flex:0 0 auto;">
        <select
          v-model="filterDimensionId"
          style="width:auto"
        >
          <option :value="null">全局 (所有分区)</option>
          <option v-for="dim in dimensions" :key="dim.id" :value="dim.id">
            {{ dim.dimensionName }}
          </option>
        </select>
      </div>
    </div>

    <div v-if="filteredBands.length === 0" class="sp-empty">
      暂无等级映射
    </div>

    <template v-else>
      <!-- Stacked color band visualization -->
      <div class="sp-viz-bar">
        <div
          v-for="band in sortedBands"
          :key="'viz-' + band.id"
          class="sp-viz-segment"
          :style="{
            backgroundColor: band.color || '#909399',
            width: ((band.maxScore - band.minScore) / maxRange * 100) + '%',
          }"
          :title="`${band.gradeCode} ${band.gradeName}: ${band.minScore}~${band.maxScore}`"
        >
          <span v-if="(band.maxScore - band.minScore) / maxRange > 0.08">{{ band.gradeCode }}</span>
        </div>
      </div>

      <!-- Band list -->
      <div class="sp-band-list">
        <div
          v-for="band in filteredBands"
          :key="band.id"
          class="sp-band-card group"
        >
          <div class="sp-band-badge" :style="{ backgroundColor: band.color || '#909399' }">
            {{ band.gradeCode }}
          </div>
          <div class="sp-band-info">
            <div class="sp-band-name">{{ band.gradeName }}</div>
            <div class="sp-band-range">
              {{ band.minScore }} ~ {{ band.maxScore }} 分
              <span v-if="band.dimensionId && getDimensionName(band.dimensionId)" class="sp-band-dim">
                ({{ getDimensionName(band.dimensionId) }})
              </span>
            </div>
          </div>
          <!-- Score range bar -->
          <div class="sp-range-bar-bg">
            <div
              class="sp-range-bar-fill"
              :style="{
                backgroundColor: band.color || '#909399',
                width: ((band.maxScore - band.minScore) / maxRange * 100) + '%',
                marginLeft: (band.minScore / maxRange * 100) + '%',
              }"
            />
          </div>
          <div class="sp-band-actions">
            <button class="sp-ic-s" @click="startEdit(band)">
              <component :is="iconMap.Pencil" class="w-3.5 h-3.5" />
            </button>
            <button class="sp-ic-s danger" @click="$emit('delete', band.id)">
              <component :is="iconMap.Trash2" class="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      <!-- Validation warnings -->
      <div v-if="validationWarnings.length > 0" class="sp-warnings">
        <div
          v-for="(warn, idx) in validationWarnings"
          :key="idx"
          class="sp-warning"
          :class="warn.type === 'overlap' ? 'sp-warning-overlap' : 'sp-warning-gap'"
        >
          {{ warn.message }}
        </div>
      </div>
    </template>

    <!-- Add/Edit Dialog -->
    <Teleport to="body">
      <Transition name="sp-modal">
        <div v-if="showAdd || editingBand" class="sp-mask" @click.self="closeDialog">
          <div class="sp-modal" style="width:420px">
            <div class="sp-modal-head">
              <h3>{{ editingBand ? '编辑等级' : '添加等级' }}</h3>
              <button class="sp-modal-close" @click="closeDialog">&times;</button>
            </div>
            <div class="sp-modal-body">
              <div class="sp-form-row">
                <div class="sp-fld" style="width:96px; flex-shrink:0;">
                  <label>等级代码</label>
                  <input v-model="form.gradeCode" placeholder="A" :disabled="!!editingBand" />
                </div>
                <div class="sp-fld" style="flex:1;">
                  <label>等级名称</label>
                  <input v-model="form.gradeName" placeholder="优秀" />
                </div>
              </div>
              <div class="sp-form-row">
                <div class="sp-fld" style="flex:1;">
                  <label>最低分</label>
                  <input v-model.number="form.minScore" type="number" />
                </div>
                <div class="sp-fld" style="flex:1;">
                  <label>最高分</label>
                  <input v-model.number="form.maxScore" type="number" />
                </div>
              </div>
              <!-- Dimension selector -->
              <div v-if="dimensions.length > 0" class="sp-fld">
                <label>所属分区</label>
                <select v-model="form.dimensionId">
                  <option :value="null">全局 (不绑定分区)</option>
                  <option v-for="dim in dimensions" :key="dim.id" :value="dim.id">
                    {{ dim.dimensionName }}
                  </option>
                </select>
              </div>
              <div class="sp-form-row">
                <div class="sp-fld" style="flex:1;">
                  <label>颜色</label>
                  <div class="sp-color-row">
                    <input v-model="form.color" type="color" class="sp-color-input" />
                    <input v-model="form.color" style="flex:1; font-family:monospace;" placeholder="#67C23A" />
                  </div>
                </div>
                <div class="sp-fld" style="width:128px; flex-shrink:0;">
                  <label>图标</label>
                  <input v-model="form.icon" placeholder="可选" />
                </div>
              </div>
            </div>
            <div class="sp-modal-foot">
              <button class="sp-btn-ghost" @click="closeDialog">取消</button>
              <button class="sp-btn-primary" @click="handleSubmit">
                {{ editingBand ? '保存' : '添加' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Preset confirm dialog -->
    <Teleport to="body">
      <Transition name="sp-modal">
        <div v-if="pendingPreset" class="sp-mask" @click.self="pendingPreset = null">
          <div class="sp-modal" style="width:360px">
            <div class="sp-modal-head">
              <h3>应用预设: {{ pendingPreset.label }}</h3>
              <button class="sp-modal-close" @click="pendingPreset = null">&times;</button>
            </div>
            <div class="sp-modal-body">
              <p class="sp-confirm-text">此操作将替换现有的所有等级映射，是否继续？</p>
            </div>
            <div class="sp-modal-foot">
              <button class="sp-btn-ghost" @click="pendingPreset = null">取消</button>
              <button class="sp-btn-primary" @click="confirmPreset">确认</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Pencil, Trash2, AlertTriangle } from 'lucide-vue-next'
import type { GradeBand, CreateGradeBandRequest, UpdateGradeBandRequest, ScoreDimension } from '@/types/insp/scoring'

const iconMap = { Pencil, Trash2, AlertTriangle }

const props = withDefaults(defineProps<{
  gradeBands: GradeBand[]
  dimensions?: ScoreDimension[]
}>(), {
  dimensions: () => [],
})

const emit = defineEmits<{
  create: [data: CreateGradeBandRequest]
  update: [id: number, data: UpdateGradeBandRequest]
  delete: [id: number]
  applyPreset: [bands: CreateGradeBandRequest[]]
}>()

const showAdd = ref(false)
const editingBand = ref<GradeBand | null>(null)
const filterDimensionId = ref<number | null>(null)

const form = ref({
  gradeCode: '',
  gradeName: '',
  minScore: 0,
  maxScore: 100,
  color: '#67C23A',
  icon: '',
  dimensionId: null as number | null,
})

// ==================== Computed ====================

const maxRange = computed(() => {
  if (props.gradeBands.length === 0) return 100
  return Math.max(100, ...props.gradeBands.map(b => b.maxScore))
})

const sortedBands = computed(() =>
  [...props.gradeBands].sort((a, b) => b.minScore - a.minScore)
)

const filteredBands = computed(() => {
  if (filterDimensionId.value === null) return sortedBands.value
  return sortedBands.value.filter(b =>
    b.dimensionId === filterDimensionId.value || b.dimensionId === null
  )
})

// ==================== Validation ====================

interface ValidationWarning {
  type: 'overlap' | 'gap'
  message: string
}

const validationWarnings = computed<ValidationWarning[]>(() => {
  const warnings: ValidationWarning[] = []
  const bands = sortedBands.value

  for (let i = 0; i < bands.length; i++) {
    for (let j = i + 1; j < bands.length; j++) {
      const a = bands[i]
      const b = bands[j]
      // Only compare bands in the same dimension scope
      if (a.dimensionId !== b.dimensionId) continue

      if (a.minScore < b.maxScore && b.minScore < a.maxScore) {
        warnings.push({
          type: 'overlap',
          message: `${a.gradeCode}(${a.minScore}~${a.maxScore}) 与 ${b.gradeCode}(${b.minScore}~${b.maxScore}) 分数范围重叠`,
        })
      }
    }
  }

  // Check for gaps between adjacent bands (same dimension)
  const dimensionGroups = new Map<number | null, GradeBand[]>()
  for (const band of bands) {
    const key = band.dimensionId
    if (!dimensionGroups.has(key)) dimensionGroups.set(key, [])
    dimensionGroups.get(key)!.push(band)
  }

  for (const [, group] of dimensionGroups) {
    const sorted = [...group].sort((a, b) => a.minScore - b.minScore)
    for (let i = 0; i < sorted.length - 1; i++) {
      const gap = sorted[i + 1].minScore - sorted[i].maxScore
      if (gap > 0.01) {
        warnings.push({
          type: 'gap',
          message: `${sorted[i].gradeCode}(${sorted[i].maxScore}) 与 ${sorted[i + 1].gradeCode}(${sorted[i + 1].minScore}) 之间存在 ${gap.toFixed(2)} 分间隙`,
        })
      }
    }
  }

  return warnings
})

// ==================== Presets ====================

interface Preset {
  key: string
  label: string
  bands: CreateGradeBandRequest[]
}

const PRESETS: Preset[] = [
  {
    key: 'five',
    label: '五级制(A-F)',
    bands: [
      { gradeCode: 'A', gradeName: '优秀', minScore: 90, maxScore: 100, color: '#67C23A' },
      { gradeCode: 'B', gradeName: '良好', minScore: 80, maxScore: 89.99, color: '#409EFF' },
      { gradeCode: 'C', gradeName: '中等', minScore: 70, maxScore: 79.99, color: '#E6A23C' },
      { gradeCode: 'D', gradeName: '及格', minScore: 60, maxScore: 69.99, color: '#F5A623' },
      { gradeCode: 'F', gradeName: '不及格', minScore: 0, maxScore: 59.99, color: '#F56C6C' },
    ],
  },
  {
    key: 'four',
    label: '四级制(A-D)',
    bands: [
      { gradeCode: 'A', gradeName: '优秀', minScore: 85, maxScore: 100, color: '#67C23A' },
      { gradeCode: 'B', gradeName: '良好', minScore: 70, maxScore: 84.99, color: '#409EFF' },
      { gradeCode: 'C', gradeName: '合格', minScore: 60, maxScore: 69.99, color: '#E6A23C' },
      { gradeCode: 'D', gradeName: '不合格', minScore: 0, maxScore: 59.99, color: '#F56C6C' },
    ],
  },
  {
    key: 'binary',
    label: '通过/不通过',
    bands: [
      { gradeCode: 'P', gradeName: '通过', minScore: 60, maxScore: 100, color: '#67C23A' },
      { gradeCode: 'F', gradeName: '不通过', minScore: 0, maxScore: 59.99, color: '#F56C6C' },
    ],
  },
]

const pendingPreset = ref<Preset | null>(null)

function applyPreset(preset: Preset) {
  pendingPreset.value = preset
}

function confirmPreset() {
  if (!pendingPreset.value) return
  emit('applyPreset', pendingPreset.value.bands)
  pendingPreset.value = null
}

// ==================== Helpers ====================

function getDimensionName(dimId: number): string {
  return props.dimensions.find(d => String(d.id) === String(dimId))?.dimensionName || ''
}

function startEdit(band: GradeBand) {
  editingBand.value = band
  form.value = {
    gradeCode: band.gradeCode,
    gradeName: band.gradeName,
    minScore: band.minScore,
    maxScore: band.maxScore,
    color: band.color || '#909399',
    icon: band.icon || '',
    dimensionId: band.dimensionId,
  }
}

function closeDialog() {
  showAdd.value = false
  editingBand.value = null
  form.value = { gradeCode: '', gradeName: '', minScore: 0, maxScore: 100, color: '#67C23A', icon: '', dimensionId: null }
}

function handleSubmit() {
  if (editingBand.value) {
    emit('update', editingBand.value.id, {
      gradeName: form.value.gradeName,
      minScore: form.value.minScore,
      maxScore: form.value.maxScore,
      color: form.value.color || undefined,
      icon: form.value.icon || undefined,
    })
  } else {
    emit('create', {
      gradeCode: form.value.gradeCode,
      gradeName: form.value.gradeName,
      minScore: form.value.minScore,
      maxScore: form.value.maxScore,
      color: form.value.color || undefined,
      icon: form.value.icon || undefined,
      dimensionId: form.value.dimensionId,
    })
  }
  closeDialog()
}
</script>

<style scoped>
/* ========== Section layout ========== */
.sp-section { display:flex; flex-direction:column; gap:12px; }
.sp-section-header { display:flex; align-items:center; justify-content:space-between; }
.sp-section-title { font-size:14px; font-weight:600; color:#1e2a3a; margin:0; }
.sp-header-actions { display:flex; align-items:center; gap:8px; }

/* ========== Filter row ========== */
.sp-filter-row { display:flex; align-items:center; gap:8px; }

/* ========== Modal ========== */
.sp-mask { position:fixed; inset:0; z-index:1000; display:flex; align-items:center; justify-content:center; background:rgba(15,23,42,0.4); backdrop-filter:blur(2px); }
.sp-modal { background:#fff; border-radius:14px; box-shadow:0 24px 64px rgba(0,0,0,0.18); overflow:hidden; }
.sp-modal-head { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0; }
.sp-modal-head h3 { font-size:16px; font-weight:600; color:#1e2a3a; margin:0; }
.sp-modal-close { background:none; border:none; font-size:22px; color:#b8c0cc; cursor:pointer; padding:0 4px; line-height:1; }
.sp-modal-close:hover { color:#5a6474; }
.sp-modal-body { display:flex; flex-direction:column; gap:16px; padding:20px 24px; }
.sp-modal-foot { display:flex; justify-content:flex-end; gap:10px; padding:0 24px 20px; }

/* ========== Animation ========== */
.sp-modal-enter-active { transition:all 0.2s ease-out; }
.sp-modal-leave-active { transition:all 0.15s ease-in; }
.sp-modal-enter-from { opacity:0; }
.sp-modal-enter-from .sp-modal { transform:translateY(12px) scale(0.97); }
.sp-modal-leave-to { opacity:0; }
.sp-modal-leave-to .sp-modal { transform:translateY(-8px) scale(0.98); }

/* ========== Buttons ========== */
.sp-btn-primary { display:inline-flex; align-items:center; gap:5px; padding:8px 16px; background:#1a6dff; color:#fff; border:none; border-radius:8px; font-size:13px; font-weight:500; cursor:pointer; transition:background 0.15s; white-space:nowrap; }
.sp-btn-primary:hover { background:#1558d6; }
.sp-btn-primary.sm { padding:6px 12px; font-size:12px; border-radius:6px; }
.sp-btn-ghost { padding:8px 16px; background:none; border:1px solid #dce1e8; border-radius:8px; font-size:13px; color:#5a6474; cursor:pointer; transition:background 0.15s; }
.sp-btn-ghost:hover { background:#f4f6f9; }
.sp-ic-s { background:none; border:none; padding:3px; color:#b8c0cc; cursor:pointer; border-radius:4px; display:flex; align-items:center; transition:all 0.12s; }
.sp-ic-s:hover { color:#1a6dff; }
.sp-ic-s.danger:hover { color:#d93025; }

/* ========== Form fields ========== */
.sp-fld label { display:block; font-size:12px; font-weight:500; color:#5a6474; margin-bottom:5px; }
.sp-fld input, .sp-fld select, .sp-fld textarea { width:100%; border:1px solid #dce1e8; border-radius:8px; padding:8px 12px; font-size:13px; outline:none; transition:border-color 0.2s, box-shadow 0.2s; color:#1e2a3a; background:#fff; }
.sp-fld input::placeholder, .sp-fld textarea::placeholder { color:#b8c0cc; }
.sp-fld input:focus, .sp-fld select:focus, .sp-fld textarea:focus { border-color:#7aadff; box-shadow:0 0 0 3px rgba(26,109,255,0.08); }
.sp-fld .help { font-size:11px; color:#8c95a3; margin-top:4px; }

/* ========== Form layout helpers ========== */
.sp-form-row { display:flex; gap:12px; }
.sp-color-row { display:flex; align-items:center; gap:8px; }

/* ========== Preset pills ========== */
.sp-preset-group { display:flex; align-items:center; gap:4px; border-right:1px solid #e8ecf0; padding-right:8px; margin-right:4px; }
.sp-pill { font-size:11px; padding:4px 10px; border-radius:12px; border:1px solid #dce1e8; color:#5a6474; background:none; cursor:pointer; transition:all 0.15s; }
.sp-pill:hover { border-color:#7aadff; color:#1a6dff; background:#f0f4ff; }

/* ========== Empty state ========== */
.sp-empty { text-align:center; padding:32px 0; color:#b8c0cc; font-size:13px; }

/* ========== Color visualization bar ========== */
.sp-viz-bar { height:24px; border-radius:99px; overflow:hidden; display:flex; background:#f0f2f5; }
.sp-viz-segment { height:100%; position:relative; display:flex; align-items:center; justify-content:center; font-size:9px; font-weight:700; color:#fff; overflow:hidden; transition:all 0.15s; }

/* ========== Band list & cards ========== */
.sp-band-list { display:flex; flex-direction:column; gap:6px; }
.sp-band-card { display:flex; align-items:center; gap:12px; padding:8px 12px; border-radius:10px; border:1px solid #e8ecf0; border-left:3px solid transparent; transition:border-color 0.15s; }
.sp-band-card:hover { border-left-color:#1a6dff; }

.sp-band-badge { width:32px; height:32px; border-radius:8px; display:flex; align-items:center; justify-content:center; font-size:12px; font-weight:700; color:#fff; flex-shrink:0; }

.sp-band-info { flex:1; min-width:0; }
.sp-band-name { font-size:13px; font-weight:500; color:#1e2a3a; }
.sp-band-range { font-size:12px; color:#8c95a3; }
.sp-band-dim { margin-left:4px; color:#1a6dff; }

.sp-range-bar-bg { width:128px; height:8px; background:#f0f2f5; border-radius:99px; overflow:hidden; flex-shrink:0; }
.sp-range-bar-fill { height:100%; border-radius:99px; }

.sp-band-actions { display:flex; align-items:center; gap:4px; opacity:0; transition:opacity 0.15s; flex-shrink:0; }
.group:hover .sp-band-actions { opacity:1; }

/* ========== Validation warnings ========== */
.sp-warnings { display:flex; flex-direction:column; gap:4px; }
.sp-warning { font-size:12px; padding:6px 12px; border-radius:8px; border-left:3px solid transparent; }
.sp-warning-overlap { border-left-color:#d93025; background:#fef2f2; color:#b91c1c; }
.sp-warning-gap { border-left-color:#d97706; background:#fffbeb; color:#92400e; }

/* ========== Confirm dialog text ========== */
.sp-confirm-text { font-size:13px; color:#5a6474; margin:0; }

/* ========== Color input override ========== */
.sp-color-input { width:32px !important; height:32px !important; padding:2px !important; border-radius:8px; border:1px solid #dce1e8; cursor:pointer; }
</style>
