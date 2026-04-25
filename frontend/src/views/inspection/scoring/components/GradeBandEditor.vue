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

    <div v-if="sortedBands.length === 0" class="sp-empty">
      暂无等级映射
    </div>

    <template v-else>
      <!-- Band list -->
      <div class="sp-band-list">
        <div v-for="band in sortedBands" :key="band.id" class="sp-band-card group">
          <span class="sp-band-code">{{ band.gradeCode }}</span>
          <span class="sp-band-name">{{ band.gradeName }}</span>
          <span class="sp-band-range">{{ band.minScore }}~{{ band.maxScore }}</span>
          <div class="sp-band-actions">
            <button class="sp-ic-s" @click="startEdit(band)">✎</button>
            <button class="sp-ic-s danger" @click="$emit('delete', band.id)">×</button>
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
import type { GradeBand, CreateGradeBandRequest, UpdateGradeBandRequest } from '@/types/insp/scoring'

const props = defineProps<{
  gradeBands: GradeBand[]
}>()

const emit = defineEmits<{
  create: [data: CreateGradeBandRequest]
  update: [id: number, data: UpdateGradeBandRequest]
  delete: [id: number]
  applyPreset: [bands: CreateGradeBandRequest[]]
}>()

const showAdd = ref(false)
const editingBand = ref<GradeBand | null>(null)

const form = ref({
  gradeCode: '',
  gradeName: '',
  minScore: 0,
  maxScore: 100,
})

// ==================== Computed ====================

const sortedBands = computed(() =>
  [...props.gradeBands].sort((a, b) => b.minScore - a.minScore)
)

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
      if (a.minScore < b.maxScore && b.minScore < a.maxScore) {
        warnings.push({
          type: 'overlap',
          message: `${a.gradeCode}(${a.minScore}~${a.maxScore}) 与 ${b.gradeCode}(${b.minScore}~${b.maxScore}) 分数范围重叠`,
        })
      }
    }
  }

  // Check for gaps between adjacent bands
  const sorted = [...bands].sort((a, b) => a.minScore - b.minScore)
  for (let i = 0; i < sorted.length - 1; i++) {
    const gap = sorted[i + 1].minScore - sorted[i].maxScore
    if (gap > 0.01) {
      warnings.push({
        type: 'gap',
        message: `${sorted[i].gradeCode}(${sorted[i].maxScore}) 与 ${sorted[i + 1].gradeCode}(${sorted[i + 1].minScore}) 之间存在 ${gap.toFixed(2)} 分间隙`,
      })
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

function startEdit(band: GradeBand) {
  editingBand.value = band
  form.value = {
    gradeCode: band.gradeCode,
    gradeName: band.gradeName,
    minScore: band.minScore,
    maxScore: band.maxScore,
  }
}

function closeDialog() {
  showAdd.value = false
  editingBand.value = null
  form.value = { gradeCode: '', gradeName: '', minScore: 0, maxScore: 100 }
}

function handleSubmit() {
  if (editingBand.value) {
    emit('update', editingBand.value.id, {
      gradeName: form.value.gradeName,
      minScore: form.value.minScore,
      maxScore: form.value.maxScore,
    })
  } else {
    emit('create', {
      gradeCode: form.value.gradeCode,
      gradeName: form.value.gradeName,
      minScore: form.value.minScore,
      maxScore: form.value.maxScore,
    })
  }
  closeDialog()
}
</script>

<style scoped>
.sp-section { display:flex; flex-direction:column; gap:6px; }
.sp-section-header { display:flex; align-items:center; justify-content:space-between; }
.sp-section-title { font-size:11px; font-weight:600; color:#374151; margin:0; }
.sp-header-actions { display:flex; align-items:center; gap:6px; }

/* Preset pills */
.sp-preset-group { display:flex; align-items:center; gap:3px; border-right:1px solid #e8ecf0; padding-right:6px; margin-right:2px; }
.sp-pill { font-size:10px; padding:2px 8px; border-radius:10px; border:1px solid #dce1e8; color:#5a6474; background:none; cursor:pointer; transition:all 0.15s; }
.sp-pill:hover { border-color:#7aadff; color:#1a6dff; background:#f0f4ff; }

/* Buttons */
.sp-btn-primary { padding:4px 10px; background:#1a6dff; color:#fff; border:none; border-radius:5px; font-size:11px; font-weight:500; cursor:pointer; }
.sp-btn-primary:hover { background:#1558d6; }
.sp-btn-primary.sm { padding:3px 8px; font-size:10px; }
.sp-btn-ghost { padding:4px 10px; background:none; border:1px solid #e5e7eb; border-radius:5px; font-size:11px; color:#6b7280; cursor:pointer; }
.sp-btn-ghost:hover { background:#f4f6f9; }
.sp-ic-s { background:none; border:none; padding:1px 3px; color:#b8c0cc; cursor:pointer; font-size:12px; border-radius:3px; }
.sp-ic-s:hover { color:#1a6dff; }
.sp-ic-s.danger:hover { color:#d93025; }

/* Empty */
.sp-empty { text-align:center; padding:8px 0; color:#b8c0cc; font-size:11px; }

/* Band list */
.sp-band-list { display:flex; flex-direction:column; gap:2px; }
.sp-band-card { display:flex; align-items:center; gap:6px; padding:4px 6px; border-radius:5px; border:1px solid #f0f1f3; }
.sp-band-card:hover { border-color:#dbeafe; background:#f8faff; }
.sp-band-code { font-size:11px; font-weight:600; color:#1a6dff; min-width:20px; }
.sp-band-name { font-size:12px; color:#1e2a3a; flex:1; }
.sp-band-range { font-size:11px; color:#8c95a3; white-space:nowrap; }
.sp-band-actions { display:flex; gap:2px; opacity:0; transition:opacity 0.1s; }
.sp-band-card:hover .sp-band-actions { opacity:1; }

/* Warnings */
.sp-warnings { display:flex; flex-direction:column; gap:2px; }
.sp-warning { font-size:10px; padding:3px 8px; border-radius:4px; border-left:2px solid transparent; }
.sp-warning-overlap { border-left-color:#d93025; background:#fef2f2; color:#b91c1c; }
.sp-warning-gap { border-left-color:#d97706; background:#fffbeb; color:#92400e; }

/* Modal */
.sp-mask { position:fixed; inset:0; z-index:1000; display:flex; align-items:center; justify-content:center; background:rgba(15,23,42,0.35); backdrop-filter:blur(1px); }
.sp-modal { background:#fff; border-radius:10px; box-shadow:0 16px 48px rgba(0,0,0,0.15); width:380px; overflow:hidden; }
.sp-modal-head { display:flex; align-items:center; justify-content:space-between; padding:10px 14px; border-bottom:1px solid #f0f1f3; }
.sp-modal-head h3 { font-size:13px; font-weight:600; color:#1e2a3a; margin:0; }
.sp-modal-close { background:none; border:none; font-size:18px; color:#9ca3af; cursor:pointer; line-height:1; }
.sp-modal-body { display:flex; flex-direction:column; gap:8px; padding:10px 14px; }
.sp-modal-foot { display:flex; justify-content:flex-end; gap:6px; padding:6px 14px 10px; }
.sp-confirm-text { font-size:12px; color:#5a6474; margin:0; }

/* Form fields (modal) */
.sp-fld { display:flex; flex-direction:column; }
.sp-fld label { font-size:11px; font-weight:500; color:#6b7280; margin-bottom:2px; }
.sp-fld input, .sp-fld select { width:100%; border:1px solid #e5e7eb; border-radius:6px; padding:4px 8px; font-size:12px; outline:none; color:#111827; background:#fff; }
.sp-fld input:focus, .sp-fld select:focus { border-color:#93c5fd; box-shadow:0 0 0 2px rgba(37,99,235,0.06); }
.sp-form-row { display:flex; gap:8px; }

/* Animation */
.sp-modal-enter-active { transition:all 0.15s ease-out; }
.sp-modal-leave-active { transition:all 0.1s ease-in; }
.sp-modal-enter-from, .sp-modal-leave-to { opacity:0; }
</style>
