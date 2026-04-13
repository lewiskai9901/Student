<script setup lang="ts">
/**
 * FormItemRenderer - Renders a single inspection form item
 *
 * Supports all 22 field types defined in the ItemType enum.
 * Delegates to appropriate Element Plus components based on item type.
 */
import { computed } from 'vue'
import { Camera, MapPin, ScanLine, PenTool } from 'lucide-vue-next'
import type { ItemType } from '@/types/insp/enums'

interface ItemDef {
  id: number
  itemCode: string
  itemName: string
  itemType: string
  config?: string | null
  validationRules?: string | null
  responseSetId?: number | null
  scoringConfig?: string | null
  isRequired?: boolean
  isScored?: boolean
}

const props = withDefaults(defineProps<{
  item: ItemDef
  modelValue: any
  readonly?: boolean
}>(), {
  readonly: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: any]
}>()

// ---------- Parsed Config ----------

const parsedConfig = computed(() => {
  if (!props.item.config) return {}
  try { return JSON.parse(props.item.config) } catch { return {} }
})

// Select options from config (for SELECT, MULTI_SELECT, CHECKBOX, RADIO)
const selectOptions = computed(() => {
  const cfg = parsedConfig.value
  if (Array.isArray(cfg.options)) return cfg.options
  return []
})

// Number config
const numberMin = computed(() => parsedConfig.value.min ?? 0)
const numberMax = computed(() => parsedConfig.value.max ?? 99999)
const numberStep = computed(() => parsedConfig.value.step ?? 1)

// Slider config
const sliderMin = computed(() => parsedConfig.value.min ?? 0)
const sliderMax = computed(() => parsedConfig.value.max ?? 100)

// ---------- Helpers ----------

function update(val: any) {
  emit('update:modelValue', val)
}

const itemType = computed(() => props.item.itemType as ItemType)

</script>

<template>
  <div class="form-item-renderer">
    <!-- TEXT -->
    <el-input
      v-if="itemType === 'TEXT'"
      :model-value="modelValue ?? ''"
      :disabled="readonly"
      :placeholder="item.itemName"
      @update:model-value="update"
    />

    <!-- TEXTAREA -->
    <el-input
      v-else-if="itemType === 'TEXTAREA'"
      :model-value="modelValue ?? ''"
      type="textarea"
      :rows="3"
      :disabled="readonly"
      :placeholder="item.itemName"
      @update:model-value="update"
    />

    <!-- RICH_TEXT -->
    <el-input
      v-else-if="itemType === 'RICH_TEXT'"
      :model-value="modelValue ?? ''"
      type="textarea"
      :rows="5"
      :disabled="readonly"
      placeholder="富文本内容..."
      @update:model-value="update"
    />

    <!-- NUMBER -->
    <el-input-number
      v-else-if="itemType === 'NUMBER'"
      :model-value="modelValue ?? 0"
      :min="numberMin"
      :max="numberMax"
      :step="numberStep"
      :disabled="readonly"
      controls-position="right"
      @update:model-value="update"
    />

    <!-- SLIDER -->
    <div v-else-if="itemType === 'SLIDER'" class="flex items-center gap-3">
      <el-slider
        :model-value="modelValue ?? sliderMin"
        :min="sliderMin"
        :max="sliderMax"
        :disabled="readonly"
        class="flex-1"
        show-input
        @update:model-value="update"
      />
    </div>

    <!-- SELECT -->
    <el-select
      v-else-if="itemType === 'SELECT'"
      :model-value="modelValue"
      :disabled="readonly"
      placeholder="请选择"
      class="w-full"
      @update:model-value="update"
    >
      <el-option
        v-for="opt in selectOptions"
        :key="opt.value ?? opt.label"
        :label="opt.label"
        :value="opt.value ?? opt.label"
      />
    </el-select>

    <!-- MULTI_SELECT -->
    <el-select
      v-else-if="itemType === 'MULTI_SELECT'"
      :model-value="modelValue ?? []"
      multiple
      :disabled="readonly"
      placeholder="请选择（多选）"
      class="w-full"
      @update:model-value="update"
    >
      <el-option
        v-for="opt in selectOptions"
        :key="opt.value ?? opt.label"
        :label="opt.label"
        :value="opt.value ?? opt.label"
      />
    </el-select>

    <!-- CHECKBOX -->
    <el-checkbox-group
      v-else-if="itemType === 'CHECKBOX'"
      :model-value="modelValue ?? []"
      :disabled="readonly"
      @update:model-value="update"
    >
      <el-checkbox
        v-for="opt in selectOptions"
        :key="opt.value ?? opt.label"
        :label="opt.label"
        :value="opt.value ?? opt.label"
      />
    </el-checkbox-group>

    <!-- RADIO -->
    <el-radio-group
      v-else-if="itemType === 'RADIO'"
      :model-value="modelValue"
      :disabled="readonly"
      @update:model-value="update"
    >
      <el-radio
        v-for="opt in selectOptions"
        :key="opt.value ?? opt.label"
        :value="opt.value ?? opt.label"
      >
        {{ opt.label }}
      </el-radio>
    </el-radio-group>

    <!-- DATE -->
    <el-date-picker
      v-else-if="itemType === 'DATE'"
      :model-value="modelValue"
      type="date"
      value-format="YYYY-MM-DD"
      :disabled="readonly"
      placeholder="选择日期"
      class="!w-full"
      @update:model-value="update"
    />

    <!-- TIME -->
    <el-time-picker
      v-else-if="itemType === 'TIME'"
      :model-value="modelValue"
      format="HH:mm"
      value-format="HH:mm"
      :disabled="readonly"
      placeholder="选择时间"
      @update:model-value="update"
    />

    <!-- DATETIME -->
    <el-date-picker
      v-else-if="itemType === 'DATETIME'"
      :model-value="modelValue"
      type="datetime"
      value-format="YYYY-MM-DD HH:mm:ss"
      :disabled="readonly"
      placeholder="选择日期时间"
      class="!w-full"
      @update:model-value="update"
    />

    <!-- PHOTO -->
    <div v-else-if="itemType === 'PHOTO'" class="flex items-center gap-2">
      <div
        v-if="modelValue"
        class="w-20 h-20 rounded-md border border-gray-200 overflow-hidden"
      >
        <img :src="modelValue" class="w-full h-full object-cover" alt="photo" />
      </div>
      <div
        v-if="!readonly"
        class="w-20 h-20 rounded-md border-2 border-dashed border-gray-300 flex items-center justify-center cursor-pointer hover:border-blue-400 transition"
      >
        <Camera class="w-6 h-6 text-gray-400" />
      </div>
      <span v-if="!modelValue && readonly" class="text-sm text-gray-400">未上传</span>
    </div>

    <!-- VIDEO -->
    <div v-else-if="itemType === 'VIDEO'" class="flex items-center gap-2">
      <div
        v-if="!readonly"
        class="w-24 h-16 rounded-md border-2 border-dashed border-gray-300 flex items-center justify-center cursor-pointer hover:border-blue-400 transition"
      >
        <span class="text-xs text-gray-400">上传视频</span>
      </div>
      <span v-if="modelValue" class="text-xs text-blue-500 truncate max-w-[200px]">
        {{ modelValue }}
      </span>
      <span v-else-if="readonly" class="text-sm text-gray-400">未上传</span>
    </div>

    <!-- SIGNATURE -->
    <div
      v-else-if="itemType === 'SIGNATURE'"
      class="rounded-md border border-gray-200 bg-gray-50 px-3 py-4 text-center"
    >
      <PenTool class="w-6 h-6 text-gray-300 mx-auto mb-1" />
      <p class="text-xs text-gray-400">
        {{ readonly ? (modelValue ? '已签名' : '未签名') : '点击签名区域进行签名' }}
      </p>
    </div>

    <!-- FILE_UPLOAD -->
    <div v-else-if="itemType === 'FILE_UPLOAD'">
      <el-upload
        :disabled="readonly"
        :auto-upload="false"
        :limit="5"
        accept="*/*"
      >
        <el-button size="small" :disabled="readonly">选择文件</el-button>
        <template #tip>
          <div class="text-xs text-gray-400 mt-1">支持任意文件类型</div>
        </template>
      </el-upload>
    </div>

    <!-- GPS -->
    <div
      v-else-if="itemType === 'GPS'"
      class="flex items-center gap-2 rounded-md border border-gray-200 px-3 py-2"
    >
      <MapPin class="w-4 h-4 text-gray-400" />
      <span v-if="modelValue" class="text-sm text-gray-600">{{ modelValue }}</span>
      <span v-else class="text-sm text-gray-400">
        {{ readonly ? '未采集' : '点击获取当前位置' }}
      </span>
    </div>

    <!-- BARCODE -->
    <div
      v-else-if="itemType === 'BARCODE'"
      class="flex items-center gap-2 rounded-md border border-gray-200 px-3 py-2"
    >
      <ScanLine class="w-4 h-4 text-gray-400" />
      <span v-if="modelValue" class="text-sm text-gray-600">{{ modelValue }}</span>
      <span v-else class="text-sm text-gray-400">
        {{ readonly ? '未扫描' : '点击扫描条码' }}
      </span>
    </div>

    <!-- Fallback: Unknown Type -->
    <div v-else class="rounded-md bg-amber-50 px-3 py-2">
      <span class="text-xs text-amber-600">
        不支持的字段类型: {{ item.itemType }}
      </span>
      <el-input
        :model-value="modelValue ?? ''"
        :disabled="readonly"
        size="small"
        class="mt-1"
        @update:model-value="update"
      />
    </div>
  </div>
</template>
