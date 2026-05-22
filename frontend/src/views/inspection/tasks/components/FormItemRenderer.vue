<script setup lang="ts">
/**
 * FormItemRenderer - Renders a single inspection form item
 *
 * Supports all 22 field types defined in the ItemType enum.
 * Delegates to appropriate Element Plus components based on item type.
 */
import type { LongId } from '@/types/common'
import { computed, ref, nextTick, onBeforeUnmount } from 'vue'
import { Camera, MapPin, ScanLine, PenTool } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import SignaturePad from 'signature_pad'
import type { ItemType } from '@/types/insp/enums'
import { uploadImage } from '@/api/upload'
import { uploadFile } from '@/api/file'
import { useGeolocation } from '@/composables/inspection/useGeolocation'

interface ItemDef {
  id: LongId
  itemCode: string
  itemName: string
  itemType: string
  config?: string | null
  validationRules?: string | null
  responseSetId?: LongId | null
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

// ---------- 媒体上传 (PHOTO / VIDEO / FILE_UPLOAD) ----------
const uploading = ref(false)

async function handleUpload(uploadFileObj: any) {
  const raw: File | undefined = uploadFileObj?.raw ?? uploadFileObj
  if (!raw) return
  uploading.value = true
  try {
    const url = props.item.itemType === 'PHOTO'
      ? (await uploadImage(raw)).url
      : (await uploadFile(raw, 'inspection-form')).fileUrl
    update(url)
    ElMessage.success('上传成功')
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '未知错误'
    ElMessage.error('上传失败: ' + msg)
  } finally {
    uploading.value = false
  }
}

// ---------- 手写签名 (SIGNATURE) ----------
const signatureDialogVisible = ref(false)
const signatureCanvas = ref<HTMLCanvasElement | null>(null)
const signatureSaving = ref(false)
let signaturePad: SignaturePad | null = null

function resizeSignatureCanvas() {
  const canvas = signatureCanvas.value
  if (!canvas) return
  const ratio = Math.max(window.devicePixelRatio || 1, 1)
  const rect = canvas.getBoundingClientRect()
  canvas.width = rect.width * ratio
  canvas.height = rect.height * ratio
  const ctx = canvas.getContext('2d')
  if (ctx) ctx.scale(ratio, ratio)
  signaturePad?.clear()
}

async function openSignatureDialog() {
  signatureDialogVisible.value = true
  await nextTick()
  if (!signatureCanvas.value) return
  signaturePad = new SignaturePad(signatureCanvas.value, {
    penColor: '#1f2937',
    backgroundColor: 'rgba(255,255,255,1)',
  })
  resizeSignatureCanvas()
  window.addEventListener('resize', resizeSignatureCanvas)
}

function closeSignatureDialog() {
  window.removeEventListener('resize', resizeSignatureCanvas)
  signaturePad?.off()
  signaturePad = null
  signatureDialogVisible.value = false
}

function clearSignature() {
  signaturePad?.clear()
}

async function saveSignature() {
  if (!signaturePad || !signatureCanvas.value) return
  if (signaturePad.isEmpty()) {
    ElMessage.warning('请先签名')
    return
  }
  signatureSaving.value = true
  try {
    const blob: Blob = await new Promise((resolve, reject) => {
      signatureCanvas.value!.toBlob((b) => {
        if (b) resolve(b)
        else reject(new Error('导出签名图片失败'))
      }, 'image/png')
    })
    const file = new File([blob], `signature-${Date.now()}.png`, { type: 'image/png' })
    const url = (await uploadImage(file)).url
    update(url)
    ElMessage.success('签名已保存')
    closeSignatureDialog()
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '未知错误'
    ElMessage.error('保存失败: ' + msg)
  } finally {
    signatureSaving.value = false
  }
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeSignatureCanvas)
  signaturePad?.off()
  signaturePad = null
})

// ---------- GPS 定位 ----------
const { loading: gpsLoading, error: gpsError, getCurrentPosition } = useGeolocation()

async function handleGetGps() {
  const pos = await getCurrentPosition()
  if (pos) {
    update(`${pos.latitude.toFixed(6)}, ${pos.longitude.toFixed(6)}`)
    ElMessage.success('定位成功')
  } else {
    ElMessage.error('定位失败: ' + (gpsError.value || '无法获取位置'))
  }
}

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
      <a
        v-if="modelValue"
        :href="modelValue"
        target="_blank"
        rel="noopener"
        class="w-20 h-20 rounded-md border border-gray-200 overflow-hidden block"
      >
        <img :src="modelValue" class="w-full h-full object-cover" alt="photo" />
      </a>
      <el-upload
        v-if="!readonly"
        :auto-upload="false"
        :show-file-list="false"
        accept="image/*"
        :on-change="handleUpload"
      >
        <el-button size="small" :loading="uploading">
          <Camera class="w-3.5 h-3.5 mr-1" />{{ modelValue ? '重新上传' : '上传照片' }}
        </el-button>
      </el-upload>
      <span v-if="!modelValue && readonly" class="text-sm text-gray-400">未上传</span>
    </div>

    <!-- VIDEO -->
    <div v-else-if="itemType === 'VIDEO'" class="flex items-center gap-2">
      <el-upload
        v-if="!readonly"
        :auto-upload="false"
        :show-file-list="false"
        accept="video/*"
        :on-change="handleUpload"
      >
        <el-button size="small" :loading="uploading">{{ modelValue ? '重新上传' : '上传视频' }}</el-button>
      </el-upload>
      <a v-if="modelValue" :href="modelValue" target="_blank" rel="noopener"
         class="text-xs text-blue-500 truncate max-w-[200px]">查看视频</a>
      <span v-else-if="readonly" class="text-sm text-gray-400">未上传</span>
    </div>

    <!-- SIGNATURE — 手写签名 (signature_pad) -->
    <div v-else-if="itemType === 'SIGNATURE'" class="flex items-center gap-2">
      <a
        v-if="modelValue"
        :href="modelValue"
        target="_blank"
        rel="noopener"
        class="h-20 rounded-md border border-gray-200 overflow-hidden bg-white block"
      >
        <img :src="modelValue" class="h-full object-contain" alt="signature" />
      </a>
      <el-button v-if="!readonly" size="small" @click="openSignatureDialog">
        <PenTool class="w-3.5 h-3.5 mr-1" />{{ modelValue ? '重新签名' : '签名' }}
      </el-button>
      <span v-if="!modelValue && readonly" class="text-sm text-gray-400">未签名</span>

      <el-dialog
        v-model="signatureDialogVisible"
        title="手写签名"
        width="560px"
        append-to-body
        :close-on-click-modal="false"
        @closed="closeSignatureDialog"
      >
        <div class="signature-canvas-wrap">
          <canvas ref="signatureCanvas" class="signature-canvas"></canvas>
          <p class="signature-hint">在上方框内手写签名</p>
        </div>
        <template #footer>
          <el-button @click="clearSignature">清除</el-button>
          <el-button @click="closeSignatureDialog">取消</el-button>
          <el-button type="primary" :loading="signatureSaving" @click="saveSignature">
            保存签名
          </el-button>
        </template>
      </el-dialog>
    </div>

    <!-- FILE_UPLOAD -->
    <div v-else-if="itemType === 'FILE_UPLOAD'">
      <div class="flex items-center gap-2">
        <a v-if="modelValue" :href="modelValue" target="_blank" rel="noopener"
           class="text-xs text-blue-500 truncate max-w-[200px]">已上传文件</a>
        <el-upload
          v-if="!readonly"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleUpload"
        >
          <el-button size="small" :loading="uploading">{{ modelValue ? '重新上传' : '选择文件' }}</el-button>
        </el-upload>
        <span v-else-if="!modelValue" class="text-sm text-gray-400">未上传</span>
      </div>
    </div>

    <!-- GPS -->
    <div
      v-else-if="itemType === 'GPS'"
      class="flex items-center gap-2 rounded-md border border-gray-200 px-3 py-2"
    >
      <MapPin class="w-4 h-4 text-gray-400" />
      <span v-if="modelValue" class="text-sm text-gray-600">{{ modelValue }}</span>
      <el-button v-if="!readonly" size="small" :loading="gpsLoading" @click="handleGetGps">
        {{ modelValue ? '重新定位' : '获取当前位置' }}
      </el-button>
      <span v-else-if="!modelValue" class="text-sm text-gray-400">未采集</span>
    </div>

    <!-- BARCODE — 需扫码第三方库, 桌面端暂不支持手工录入兜底 -->
    <div
      v-else-if="itemType === 'BARCODE'"
      class="flex items-center gap-2 rounded-md border border-gray-200 px-3 py-2"
    >
      <ScanLine class="w-4 h-4 text-gray-400" />
      <el-input
        :model-value="modelValue ?? ''"
        :disabled="readonly"
        size="small"
        placeholder="手工录入条码（扫码需移动端）"
        class="flex-1"
        @update:model-value="update"
      />
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

<style scoped>
.signature-canvas-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.signature-canvas {
  width: 100%;
  height: 200px;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  background: #fff;
  touch-action: none;
  cursor: crosshair;
}

.signature-hint {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
}
</style>
