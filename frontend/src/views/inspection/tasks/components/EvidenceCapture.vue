<script setup lang="ts">
/**
 * EvidenceCapture - Evidence photo/file upload
 *
 * Provides an upload area for attaching evidence (photos, files) to
 * an inspection submission. Supports image preview and file validation.
 */
import { computed, ref } from 'vue'
import { Plus, X, ZoomIn } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'

const props = withDefaults(defineProps<{
  modelValue: string[]
  maxCount?: number
  maxSizeMb?: number
  accept?: string
}>(), {
  maxCount: 9,
  maxSizeMb: 10,
  accept: 'image/*',
})

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

// ---------- State ----------

const previewVisible = ref(false)
const previewUrl = ref('')

const fileUrls = computed(() => props.modelValue ?? [])

const canUploadMore = computed(
  () => fileUrls.value.length < props.maxCount,
)

const uploadTip = computed(() => {
  const parts: string[] = []
  parts.push(`最多 ${props.maxCount} 个文件`)
  parts.push(`单个不超过 ${props.maxSizeMb}MB`)
  return parts.join('，')
})

// ---------- Image detection ----------

function isImageUrl(url: string): boolean {
  if (!url) return false
  const lower = url.toLowerCase()
  return (
    lower.includes('.jpg') ||
    lower.includes('.jpeg') ||
    lower.includes('.png') ||
    lower.includes('.gif') ||
    lower.includes('.webp') ||
    lower.includes('.bmp') ||
    lower.startsWith('data:image/')
  )
}

// ---------- Upload handler ----------

function handleUploadChange(file: any) {
  // Validate size
  if (file.size > props.maxSizeMb * 1024 * 1024) {
    ElMessage.warning(`文件大小不能超过 ${props.maxSizeMb}MB`)
    return
  }
  // Validate count
  if (!canUploadMore.value) {
    ElMessage.warning(`最多只能上传 ${props.maxCount} 个文件`)
    return
  }

  // In production, this would upload to server and get a URL.
  // For now, create a local blob URL for preview.
  const url = URL.createObjectURL(file.raw ?? file)
  emit('update:modelValue', [...fileUrls.value, url])
}

function handleUploadError() {
  ElMessage.error('上传失败，请重试')
}

// ---------- Remove ----------

function removeFile(index: number) {
  const updated = [...fileUrls.value]
  updated.splice(index, 1)
  emit('update:modelValue', updated)
}

// ---------- Preview ----------

function openPreview(url: string) {
  previewUrl.value = url
  previewVisible.value = true
}
</script>

<template>
  <div class="evidence-capture">
    <!-- File Grid -->
    <div class="flex flex-wrap gap-2">
      <!-- Existing files -->
      <div
        v-for="(url, index) in fileUrls"
        :key="index"
        class="relative group w-24 h-24 rounded-md border border-gray-200 overflow-hidden bg-gray-50"
      >
        <!-- Image preview -->
        <img
          v-if="isImageUrl(url)"
          :src="url"
          class="w-full h-full object-cover"
          alt="evidence"
        />
        <!-- Non-image file -->
        <div v-else class="flex items-center justify-center h-full">
          <span class="text-xs text-gray-400 px-1 text-center truncate">
            {{ url.split('/').pop() || '文件' }}
          </span>
        </div>

        <!-- Overlay actions -->
        <div
          class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition flex items-center justify-center gap-2"
        >
          <button
            v-if="isImageUrl(url)"
            class="w-7 h-7 rounded-full bg-white/80 flex items-center justify-center hover:bg-white"
            @click="openPreview(url)"
          >
            <ZoomIn class="w-4 h-4 text-gray-700" />
          </button>
          <button
            class="w-7 h-7 rounded-full bg-white/80 flex items-center justify-center hover:bg-white"
            @click="removeFile(index)"
          >
            <X class="w-4 h-4 text-red-500" />
          </button>
        </div>
      </div>

      <!-- Upload button -->
      <el-upload
        v-if="canUploadMore"
        :auto-upload="false"
        :show-file-list="false"
        :accept="accept"
        :on-change="handleUploadChange"
        :on-error="handleUploadError"
      >
        <div
          class="w-24 h-24 rounded-md border-2 border-dashed border-gray-300 flex flex-col items-center justify-center cursor-pointer hover:border-blue-400 transition"
        >
          <Plus class="w-6 h-6 text-gray-400" />
          <span class="text-xs text-gray-400 mt-1">上传</span>
        </div>
      </el-upload>
    </div>

    <!-- Upload info -->
    <p class="mt-2 text-xs text-gray-400">
      {{ uploadTip }}
      <span v-if="fileUrls.length > 0" class="ml-1">
        (已上传 {{ fileUrls.length }}/{{ maxCount }})
      </span>
    </p>

    <!-- Image Preview Dialog -->
    <el-dialog
      v-model="previewVisible"
      title="图片预览"
      width="auto"
      :close-on-click-modal="true"
    >
      <img
        :src="previewUrl"
        alt="preview"
        class="max-w-full max-h-[70vh] mx-auto block"
      />
    </el-dialog>
  </div>
</template>
