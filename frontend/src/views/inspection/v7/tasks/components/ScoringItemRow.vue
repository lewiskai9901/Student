<script setup lang="ts">
/**
 * ScoringItemRow - Single item scoring controls
 *
 * Renders one inspection detail item with its scoring mode controls,
 * remark input, flag toggle, and score display.
 * Supports both scored items (13 scoring modes) and capture items (input-only).
 */
import { computed } from 'vue'
import { Check, X, Flag } from 'lucide-vue-next'
import type { SubmissionDetail } from '@/types/insp/project'

const props = defineProps<{
  detail: SubmissionDetail
  editable: boolean
  // Scoring state
  resolveMode: (d: SubmissionDetail) => string
  getScoringParams: (d: SubmissionDetail) => any
  getMaxScoreForMode: (d: SubmissionDetail) => number
  formatScore: (d: SubmissionDetail) => string
  scoreColor: (d: SubmissionDetail) => string
  rowBg: (d: SubmissionDetail) => string
  needsRemark: (d: SubmissionDetail) => boolean
  isNonScoring: (d: SubmissionDetail) => boolean
  scoringInProgress: boolean
  numberInputs: Record<number, number>
  remarkInputs: Record<number, string>
  selectInputs: Record<number, string>
  multiInputs: Record<number, Record<string, number>>
  modeLabelMap: Record<string, string>
  modeTagTypeMap: Record<string, string>
  // Condition state
  isDisabled: boolean
}>()

const emit = defineEmits<{
  passFail: [detail: SubmissionDetail, value: 'PASS' | 'FAIL']
  deduction: [detail: SubmissionDetail]
  addition: [detail: SubmissionDetail]
  direct: [detail: SubmissionDetail]
  level: [detail: SubmissionDetail, label: string, score: number]
  scoreTable: [detail: SubmissionDetail, label: string, score: number]
  cumulative: [detail: SubmissionDetail]
  tieredDeduction: [detail: SubmissionDetail, label: string, score: number]
  ratingScale: [detail: SubmissionDetail, stars: number]
  weightedMulti: [detail: SubmissionDetail]
  riskMatrix: [detail: SubmissionDetail]
  threshold: [detail: SubmissionDetail]
  formula: [detail: SubmissionDetail]
  toggleFlag: [detail: SubmissionDetail]
  remarkChange: [detail: SubmissionDetail]
  'update:numberInput': [detailId: number, value: number]
  'update:multiInput': [detailId: number, key: string, value: number]
  'update:remarkInput': [detailId: number, value: string]
  'update:selectInput': [detailId: number, value: string]
}>()

// Shorthand
const mode = props.resolveMode(props.detail)
const params = props.getScoringParams(props.detail)

// Capture item type label
const captureTypeLabel = computed(() => {
  const typeMap: Record<string, string> = {
    TEXT: '文本', TEXTAREA: '文本', RICH_TEXT: '文本',
    NUMBER: '数字', SLIDER: '数字',
    SELECT: '选择', MULTI_SELECT: '多选', CHECKBOX: '勾选', RADIO: '选择',
    DATE: '日期', TIME: '时间', DATETIME: '日期时间',
    PHOTO: '照片', VIDEO: '视频', SIGNATURE: '签名', FILE_UPLOAD: '文件',
    GPS: '定位', BARCODE: '扫码',
  }
  return typeMap[props.detail.itemType] || '填写'
})

// Determine if capture item needs expanded input area (bottom section)
const captureNeedsExpandedInput = computed(() => {
  if (!props.isNonScoring(props.detail)) return false
  return ['TEXTAREA', 'TEXT', 'RICH_TEXT', 'NUMBER', 'DATE', 'TIME', 'DATETIME', 'SELECT', 'RADIO'].includes(props.detail.itemType)
})
</script>

<template>
  <div
    class="rounded-md transition-colors"
    :class="[rowBg(detail), editable ? 'hover:bg-gray-50' : '']"
  >
    <div class="flex items-center gap-3 px-3 py-2">
      <!-- Item name + mode badge -->
      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2">
          <span class="text-sm">{{ detail.itemName }}</span>
          <el-tag
            v-if="!isNonScoring(detail)"
            :type="(modeTagTypeMap[resolveMode(detail)] as any) || 'info'"
            size="small" effect="plain"
            class="!text-[10px] !px-1.5 !py-0"
          >
            {{ modeLabelMap[resolveMode(detail)] || resolveMode(detail) }}
          </el-tag>
          <el-tag
            v-else
            type="info" size="small" effect="plain"
            class="!text-[10px] !px-1.5 !py-0"
            style="color: #059669; border-color: #a7f3d0; background: #ecfdf5;"
          >
            {{ captureTypeLabel }}
          </el-tag>
          <el-tag v-if="detail.isFlagged" type="warning" size="small" effect="dark"
            class="!text-[10px] !px-1.5 !py-0">
            已标记
          </el-tag>
        </div>
      </div>

      <!-- === Read-only display (not editable) === -->
      <template v-if="!editable">
        <div class="w-28 text-right">
          <template v-if="isNonScoring(detail)">
            <span v-if="detail.responseValue" class="text-xs text-gray-600">{{ detail.responseValue }}</span>
            <span v-else class="text-gray-300 text-xs">-</span>
          </template>
          <template v-else-if="resolveMode(detail) === 'PASS_FAIL'">
            <el-tag v-if="detail.responseValue === 'PASS'" type="success" size="small">通过</el-tag>
            <el-tag v-else-if="detail.responseValue === 'FAIL'" type="danger" size="small">不通过</el-tag>
            <span v-else class="text-gray-300 text-xs">-</span>
          </template>
          <template v-else-if="['LEVEL','SCORE_TABLE','TIERED_DEDUCTION'].includes(resolveMode(detail))">
            <span v-if="detail.responseValue" class="text-xs mr-1 text-gray-500">{{ detail.responseValue }}</span>
            <span class="text-sm font-medium" :style="{ color: scoreColor(detail) }">{{ formatScore(detail) }}</span>
          </template>
          <template v-else>
            <span class="text-sm font-medium" :style="{ color: scoreColor(detail) }">
              {{ formatScore(detail) }}
            </span>
          </template>
        </div>
      </template>

      <!-- === Interactive controls (editable) === -->
      <template v-if="editable">
        <!-- Disabled by condition logic -->
        <div v-if="isDisabled" class="text-xs text-gray-400 italic">
          已禁用
        </div>
        <!-- Non-scoring capture items: inline indicator -->
        <div v-else-if="isNonScoring(detail)" class="text-xs text-gray-400">
          仅填写
        </div>
        <!-- PASS_FAIL mode -->
        <div v-else-if="resolveMode(detail) === 'PASS_FAIL'" class="flex items-center gap-1.5">
          <button
            class="px-3 py-1 rounded text-xs font-medium transition-all"
            :class="detail.responseValue === 'PASS'
              ? 'bg-green-500 text-white shadow-sm'
              : 'bg-gray-100 text-gray-500 hover:bg-green-100 hover:text-green-700'"
            @click="emit('passFail', detail, 'PASS')" :disabled="scoringInProgress"
          >
            <Check class="w-3.5 h-3.5 inline -mt-0.5" /> 通过
          </button>
          <button
            class="px-3 py-1 rounded text-xs font-medium transition-all"
            :class="detail.responseValue === 'FAIL'
              ? 'bg-red-500 text-white shadow-sm'
              : 'bg-gray-100 text-gray-500 hover:bg-red-100 hover:text-red-700'"
            @click="emit('passFail', detail, 'FAIL')" :disabled="scoringInProgress"
          >
            <X class="w-3.5 h-3.5 inline -mt-0.5" /> 不通过
          </button>
        </div>

        <!-- DEDUCTION mode -->
        <div v-else-if="resolveMode(detail) === 'DEDUCTION'" class="flex items-center gap-2">
          <el-input-number
            :model-value="numberInputs[detail.id]"
            :min="params.maxDeduction ?? -10"
            :max="0"
            :step="params.step ?? 1"
            size="small" controls-position="right"
            style="width: 110px"
            @update:model-value="(v: number) => emit('update:numberInput', detail.id, v)"
          />
          <el-button
            size="small" type="danger" plain
            :disabled="scoringInProgress"
            @click="emit('deduction', detail)"
          >确认</el-button>
        </div>

        <!-- ADDITION mode -->
        <div v-else-if="resolveMode(detail) === 'ADDITION'" class="flex items-center gap-2">
          <el-input-number
            :model-value="numberInputs[detail.id]"
            :min="0"
            :max="params.maxBonus ?? 5"
            :step="params.step ?? 1"
            size="small" controls-position="right"
            style="width: 110px"
            @update:model-value="(v: number) => emit('update:numberInput', detail.id, v)"
          />
          <el-button
            size="small" type="success" plain
            :disabled="scoringInProgress"
            @click="emit('addition', detail)"
          >确认</el-button>
        </div>

        <!-- DIRECT mode -->
        <div v-else-if="resolveMode(detail) === 'DIRECT'" class="flex items-center gap-2">
          <el-slider
            :model-value="numberInputs[detail.id]"
            :min="params.minScore ?? 0"
            :max="params.maxScore ?? 10"
            :step="1"
            :show-tooltip="false"
            style="width: 120px"
            @update:model-value="(v: number) => emit('update:numberInput', detail.id, v)"
          />
          <span class="text-xs font-medium w-10 text-center"
            :class="(numberInputs[detail.id] ?? 10) >= (params.maxScore ?? 10) * 0.8 ? 'text-green-600' :
                    (numberInputs[detail.id] ?? 10) >= (params.maxScore ?? 10) * 0.6 ? 'text-yellow-600' : 'text-red-500'">
            {{ numberInputs[detail.id] ?? (params.maxScore ?? 10) }}/{{ params.maxScore ?? 10 }}
          </span>
          <el-button
            size="small" type="primary" plain
            :disabled="scoringInProgress"
            @click="emit('direct', detail)"
          >确认</el-button>
        </div>

        <!-- LEVEL mode -->
        <div v-else-if="resolveMode(detail) === 'LEVEL'" class="flex items-center gap-1.5">
          <button
            v-for="lv in (params.levels || [])" :key="lv.label"
            class="px-2.5 py-1 rounded text-xs font-medium transition-all"
            :class="detail.responseValue === lv.label
              ? 'bg-blue-500 text-white shadow-sm'
              : 'bg-gray-100 text-gray-500 hover:bg-blue-100 hover:text-blue-700'"
            @click="emit('level', detail, lv.label, lv.score)" :disabled="scoringInProgress"
          >{{ lv.label }} ({{ lv.score }})</button>
        </div>

        <!-- SCORE_TABLE mode -->
        <div v-else-if="resolveMode(detail) === 'SCORE_TABLE'" class="flex items-center gap-1.5 flex-wrap">
          <el-popover
            v-for="opt in (params.options || [])" :key="opt.label"
            placement="top" :width="200" trigger="hover"
          >
            <template #reference>
              <button
                class="px-2.5 py-1 rounded text-xs font-medium transition-all"
                :class="detail.responseValue === opt.label
                  ? 'bg-blue-500 text-white shadow-sm'
                  : 'bg-gray-100 text-gray-500 hover:bg-blue-100 hover:text-blue-700'"
                @click="emit('scoreTable', detail, opt.label, opt.score)" :disabled="scoringInProgress"
              >{{ opt.label }} ({{ opt.score }})</button>
            </template>
            <p class="text-xs text-gray-600">{{ opt.description || '无描述' }}</p>
          </el-popover>
        </div>

        <!-- CUMULATIVE mode -->
        <div v-else-if="resolveMode(detail) === 'CUMULATIVE'" class="flex items-center gap-2">
          <el-input-number
            :model-value="numberInputs[detail.id]"
            :min="0"
            :max="params.maxCount ?? 10"
            :step="1"
            size="small" controls-position="right"
            style="width: 100px"
            @update:model-value="(v: number) => emit('update:numberInput', detail.id, v)"
          />
          <span class="text-xs text-gray-500">
            {{ numberInputs[detail.id] ?? 0 }}次 × ({{ params.scorePerCount ?? -2 }}) = {{ (numberInputs[detail.id] ?? 0) * (params.scorePerCount ?? -2) }}分
          </span>
          <el-button
            size="small" :type="(params.scorePerCount ?? -2) < 0 ? 'danger' : 'success'" plain
            :disabled="scoringInProgress"
            @click="emit('cumulative', detail)"
          >确认</el-button>
        </div>

        <!-- TIERED_DEDUCTION mode -->
        <div v-else-if="resolveMode(detail) === 'TIERED_DEDUCTION'" class="flex items-center gap-1.5">
          <button
            v-for="(t, ti) in (params.tiers || [])" :key="t.label"
            class="px-2.5 py-1 rounded text-xs font-medium transition-all"
            :class="detail.responseValue === t.label
              ? 'bg-red-500 text-white shadow-sm'
              : 'text-gray-500 hover:text-red-700'"
            :style="detail.responseValue !== t.label ? { backgroundColor: `rgba(239,68,68,${0.05 + ti * 0.08})` } : {}"
            @click="emit('tieredDeduction', detail, t.label, t.score)" :disabled="scoringInProgress"
          >{{ t.label }} ({{ t.score }})</button>
        </div>

        <!-- RATING_SCALE mode -->
        <div v-else-if="resolveMode(detail) === 'RATING_SCALE'" class="flex items-center gap-2">
          <el-rate
            :model-value="numberInputs[detail.id] ?? 0"
            :max="params.maxStars ?? 5"
            :disabled="scoringInProgress"
            @change="(v: number) => emit('ratingScale', detail, v)"
          />
          <span class="text-xs text-gray-500">
            {{ numberInputs[detail.id] ?? 0 }}星 = {{ (numberInputs[detail.id] ?? 0) * (params.scorePerStar ?? 2) }}分
          </span>
        </div>

        <!-- WEIGHTED_MULTI mode -->
        <div v-else-if="resolveMode(detail) === 'WEIGHTED_MULTI'" class="flex flex-col gap-1 min-w-[260px]">
          <div v-for="dim in (params.dimensions || [])" :key="dim.key"
            class="flex items-center gap-2">
            <span class="text-xs text-gray-500 w-12 truncate">{{ dim.label }}</span>
            <el-slider
              :model-value="(multiInputs[detail.id] || {})[dim.key] ?? 0"
              :min="0" :max="dim.maxScore ?? 10" :step="1"
              :show-tooltip="false"
              style="width: 80px"
              @input="(v: number) => emit('update:multiInput', detail.id, dim.key, v)"
            />
            <span class="text-[10px] text-gray-400 w-14">{{ (multiInputs[detail.id] || {})[dim.key] ?? 0 }}/{{ dim.maxScore }} ({{ dim.weight }}%)</span>
          </div>
          <el-button size="small" type="primary" plain :disabled="scoringInProgress" @click="emit('weightedMulti', detail)">确认</el-button>
        </div>

        <!-- RISK_MATRIX mode -->
        <div v-else-if="resolveMode(detail) === 'RISK_MATRIX'" class="flex items-center gap-2">
          <div class="flex flex-col gap-1">
            <div class="flex items-center gap-1">
              <span class="text-[10px] text-gray-400 w-10">可能性</span>
              <button
                v-for="p in (params.probabilities || [])" :key="p.value"
                class="px-1.5 py-0.5 rounded text-[10px] transition-all"
                :class="(multiInputs[detail.id] || {}).probability === p.value ? 'bg-orange-500 text-white' : 'bg-gray-100 text-gray-500 hover:bg-orange-100'"
                @click="emit('update:multiInput', detail.id, 'probability', p.value)"
              >{{ p.label }}</button>
            </div>
            <div class="flex items-center gap-1">
              <span class="text-[10px] text-gray-400 w-10">影响度</span>
              <button
                v-for="imp in (params.impacts || [])" :key="imp.value"
                class="px-1.5 py-0.5 rounded text-[10px] transition-all"
                :class="(multiInputs[detail.id] || {}).impact === imp.value ? 'bg-red-500 text-white' : 'bg-gray-100 text-gray-500 hover:bg-red-100'"
                @click="emit('update:multiInput', detail.id, 'impact', imp.value)"
              >{{ imp.label }}</button>
            </div>
          </div>
          <div class="text-xs text-gray-500">
            风险 = {{ (multiInputs[detail.id] || {}).probability ?? 1 }} × {{ (multiInputs[detail.id] || {}).impact ?? 1 }} = {{ ((multiInputs[detail.id] || {}).probability ?? 1) * ((multiInputs[detail.id] || {}).impact ?? 1) }}
          </div>
          <el-button size="small" type="danger" plain :disabled="scoringInProgress" @click="emit('riskMatrix', detail)">确认</el-button>
        </div>

        <!-- THRESHOLD mode -->
        <div v-else-if="resolveMode(detail) === 'THRESHOLD'" class="flex items-center gap-2">
          <el-input-number
            :model-value="numberInputs[detail.id]"
            :step="1"
            size="small" controls-position="right"
            style="width: 100px"
            @update:model-value="(v: number) => emit('update:numberInput', detail.id, v)"
          />
          <span class="text-xs text-gray-400">{{ params.unit }}</span>
          <span v-if="selectInputs[detail.id]" class="text-xs font-medium"
            :class="(detail.score ?? 0) >= getMaxScoreForMode(detail) * 0.8 ? 'text-green-600' : (detail.score ?? 0) >= getMaxScoreForMode(detail) * 0.5 ? 'text-yellow-600' : 'text-red-500'">
            {{ selectInputs[detail.id] }}
          </span>
          <el-button size="small" type="primary" plain :disabled="scoringInProgress" @click="emit('threshold', detail)">确认</el-button>
        </div>

        <!-- FORMULA mode -->
        <div v-else-if="resolveMode(detail) === 'FORMULA'" class="flex items-center gap-2">
          <div v-for="inp in (params.inputs || [])" :key="inp.key" class="flex items-center gap-1">
            <span class="text-[10px] text-gray-400">{{ inp.label }}</span>
            <el-input-number
              :model-value="(multiInputs[detail.id] || {})[inp.key] ?? 0"
              :step="1"
              size="small" controls-position="right"
              style="width: 90px"
              @change="(v: number) => emit('update:multiInput', detail.id, inp.key, v ?? 0)"
            />
          </div>
          <el-button size="small" type="primary" plain :disabled="scoringInProgress" @click="emit('formula', detail)">确认</el-button>
        </div>

        <!-- Flag + Score display (only for scored items) -->
        <template v-if="!isNonScoring(detail)">
          <button
            class="p-1 rounded transition-all"
            :class="detail.isFlagged
              ? 'text-orange-500 bg-orange-50'
              : 'text-gray-300 hover:text-orange-500 hover:bg-orange-50'"
            @click="emit('toggleFlag', detail)" title="标记问题"
          >
            <Flag class="w-3.5 h-3.5" />
          </button>
          <div class="w-14 text-right text-sm font-medium" :style="{ color: scoreColor(detail) }">
            {{ formatScore(detail) }}
          </div>
        </template>
      </template>
    </div>

    <!-- Remark input: shown when FAIL + requiredIfFail -->
    <div
      v-if="editable && detail.responseValue === 'FAIL' && needsRemark(detail)"
      class="px-3 pb-2"
    >
      <input
        :value="remarkInputs[detail.id]"
        class="w-full rounded border border-red-200 bg-red-50/30 px-2 py-1 text-xs text-gray-700 outline-none focus:border-red-400 placeholder:text-gray-400"
        placeholder="请填写不通过的原因..."
        @input="(e) => emit('update:remarkInput', detail.id, (e.target as HTMLInputElement).value)"
        @blur="emit('remarkChange', detail)"
        @keyup.enter="emit('remarkChange', detail)"
      />
    </div>

    <!-- ==================== Capture item input controls ==================== -->
    <template v-if="editable && isNonScoring(detail) && !isDisabled">
      <!-- TEXT input -->
      <div
        v-if="detail.itemType === 'TEXT'"
        class="px-3 pb-2"
      >
        <input
          :value="remarkInputs[detail.id]"
          class="w-full rounded border border-gray-200 bg-gray-50/50 px-2 py-1.5 text-xs text-gray-700 outline-none focus:border-blue-300 placeholder:text-gray-400"
          :placeholder="`请填写${detail.itemName}...`"
          @input="(e) => emit('update:remarkInput', detail.id, (e.target as HTMLInputElement).value)"
          @blur="emit('remarkChange', detail)"
          @keyup.enter="emit('remarkChange', detail)"
        />
      </div>

      <!-- TEXTAREA input -->
      <div
        v-else-if="detail.itemType === 'TEXTAREA' || detail.itemType === 'RICH_TEXT'"
        class="px-3 pb-2"
      >
        <textarea
          :value="remarkInputs[detail.id]"
          class="w-full rounded border border-gray-200 bg-gray-50/50 px-2 py-1.5 text-xs text-gray-700 outline-none focus:border-blue-300 placeholder:text-gray-400 resize-none"
          rows="2"
          :placeholder="`请填写${detail.itemName}...`"
          @input="(e) => emit('update:remarkInput', detail.id, (e.target as HTMLTextAreaElement).value)"
          @blur="emit('remarkChange', detail)"
        />
      </div>

      <!-- NUMBER input -->
      <div
        v-else-if="detail.itemType === 'NUMBER' || detail.itemType === 'SLIDER'"
        class="px-3 pb-2"
      >
        <el-input-number
          :model-value="numberInputs[detail.id] ?? 0"
          size="small" controls-position="right"
          style="width: 160px"
          @update:model-value="(v: number) => { emit('update:numberInput', detail.id, v); emit('update:remarkInput', detail.id, String(v)) }"
          @blur="emit('remarkChange', detail)"
        />
      </div>

      <!-- DATE input -->
      <div
        v-else-if="detail.itemType === 'DATE'"
        class="px-3 pb-2"
      >
        <el-date-picker
          :model-value="remarkInputs[detail.id]"
          type="date"
          size="small"
          :placeholder="`选择${detail.itemName}`"
          value-format="YYYY-MM-DD"
          style="width: 180px"
          @update:model-value="(v: string) => { emit('update:remarkInput', detail.id, v || ''); emit('remarkChange', detail) }"
        />
      </div>

      <!-- TIME input -->
      <div
        v-else-if="detail.itemType === 'TIME'"
        class="px-3 pb-2"
      >
        <el-time-picker
          :model-value="remarkInputs[detail.id]"
          size="small"
          :placeholder="`选择${detail.itemName}`"
          value-format="HH:mm:ss"
          style="width: 160px"
          @update:model-value="(v: string) => { emit('update:remarkInput', detail.id, v || ''); emit('remarkChange', detail) }"
        />
      </div>

      <!-- DATETIME input -->
      <div
        v-else-if="detail.itemType === 'DATETIME'"
        class="px-3 pb-2"
      >
        <el-date-picker
          :model-value="remarkInputs[detail.id]"
          type="datetime"
          size="small"
          :placeholder="`选择${detail.itemName}`"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 220px"
          @update:model-value="(v: string) => { emit('update:remarkInput', detail.id, v || ''); emit('remarkChange', detail) }"
        />
      </div>

      <!-- PHOTO placeholder -->
      <div
        v-else-if="detail.itemType === 'PHOTO' || detail.itemType === 'VIDEO'"
        class="px-3 pb-2"
      >
        <div class="flex items-center gap-2 text-xs text-gray-400 py-1">
          <span>{{ detail.itemType === 'PHOTO' ? '📷' : '📹' }} {{ detail.itemType === 'PHOTO' ? '拍照' : '视频' }}功能将在移动端支持</span>
        </div>
      </div>

      <!-- SELECT / RADIO (single choice from response value options) -->
      <div
        v-else-if="detail.itemType === 'SELECT' || detail.itemType === 'RADIO'"
        class="px-3 pb-2"
      >
        <input
          :value="remarkInputs[detail.id]"
          class="w-full rounded border border-gray-200 bg-gray-50/50 px-2 py-1.5 text-xs text-gray-700 outline-none focus:border-blue-300 placeholder:text-gray-400"
          :placeholder="`请输入${detail.itemName}...`"
          @input="(e) => emit('update:remarkInput', detail.id, (e.target as HTMLInputElement).value)"
          @blur="emit('remarkChange', detail)"
          @keyup.enter="emit('remarkChange', detail)"
        />
      </div>

      <!-- SIGNATURE / FILE_UPLOAD / GPS / BARCODE placeholders -->
      <div
        v-else-if="['SIGNATURE', 'FILE_UPLOAD', 'GPS', 'BARCODE'].includes(detail.itemType)"
        class="px-3 pb-2"
      >
        <input
          :value="remarkInputs[detail.id]"
          class="w-full rounded border border-gray-200 bg-gray-50/50 px-2 py-1.5 text-xs text-gray-700 outline-none focus:border-blue-300 placeholder:text-gray-400"
          :placeholder="`请输入${detail.itemName}...`"
          @input="(e) => emit('update:remarkInput', detail.id, (e.target as HTMLInputElement).value)"
          @blur="emit('remarkChange', detail)"
          @keyup.enter="emit('remarkChange', detail)"
        />
      </div>
    </template>
  </div>
</template>
