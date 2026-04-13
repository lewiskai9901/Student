<template>
  <div class="font-uploader">
    <div class="manager-header">
      <h4 class="text-sm font-semibold text-gray-900 flex items-center gap-2">
        <Type class="h-4 w-4 text-indigo-600" />
        自定义字体
      </h4>
      <span class="text-xs text-gray-400">{{ fonts.length }}/5</span>
    </div>

    <p class="text-xs text-gray-500 mb-3">
      上传自定义字体文件，支持 .ttf / .woff / .woff2 格式。
    </p>

    <!-- 字体列表 -->
    <div class="font-list">
      <div
        v-for="font in fonts"
        :key="font.id"
        :class="[
          'font-item',
          selectedFontId === font.id ? 'selected' : ''
        ]"
        @click="selectFont(font.id)"
      >
        <div class="font-preview" :style="{ fontFamily: font.fontFamily }">
          Aa
        </div>
        <div class="font-info">
          <span class="font-name">{{ font.name }}</span>
          <span class="font-format">.{{ font.format === 'truetype' ? 'ttf' : font.format }}</span>
        </div>
        <button
          type="button"
          class="delete-btn"
          @click.stop="deleteFont(font.id)"
          title="删除"
        >
          <Trash2 class="h-3 w-3" />
        </button>
      </div>

      <!-- 添加按钮 -->
      <button
        v-if="fonts.length < 5"
        type="button"
        class="add-font"
        @click="showDialog = true"
      >
        <Upload class="h-4 w-4" />
        <span>上传字体</span>
      </button>
    </div>

    <!-- 当前选中的字体用于标题 -->
    <div v-if="fonts.length > 0" class="current-font-setting">
      <label class="text-xs text-gray-600">标题使用自定义字体</label>
      <el-select
        :model-value="selectedFontId"
        @update:model-value="onSelectFontForTitle"
        placeholder="选择字体 (留空使用内置)"
        clearable
        size="small"
        class="w-full mt-1"
      >
        <el-option
          v-for="font in fonts"
          :key="font.id"
          :label="font.name"
          :value="font.id"
        >
          <span :style="{ fontFamily: font.fontFamily }">{{ font.name }}</span>
        </el-option>
      </el-select>
    </div>

    <!-- 上传对话框 -->
    <el-dialog
      v-model="showDialog"
      title="上传自定义字体"
      width="400px"
      :close-on-click-modal="false"
    >
      <div class="dialog-content">
        <!-- 字体名称输入 -->
        <div class="form-item">
          <label class="text-sm text-gray-700">字体名称</label>
          <el-input
            v-model="fontName"
            placeholder="请输入字体显示名称"
            maxlength="20"
            show-word-limit
          />
        </div>

        <!-- 文件上传 -->
        <div class="form-item">
          <label class="text-sm text-gray-700">字体文件</label>
          <el-upload
            ref="uploadRef"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :before-upload="beforeUpload"
            :on-success="onUploadSuccess"
            :on-error="onUploadError"
            :show-file-list="false"
            :auto-upload="true"
            accept=".ttf,.woff,.woff2"
            drag
            class="font-upload-area"
          >
            <div v-if="!uploadedFile" class="upload-placeholder">
              <Upload class="h-8 w-8 text-gray-400 mb-2" />
              <p class="text-sm text-gray-500">点击或拖拽上传字体文件</p>
              <p class="text-xs text-gray-400 mt-1">支持 .ttf / .woff / .woff2</p>
            </div>
            <div v-else class="upload-success">
              <Check class="h-6 w-6 text-green-500 mb-1" />
              <p class="text-sm text-gray-700">{{ uploadedFile.name }}</p>
              <p class="text-xs text-gray-400">上传成功</p>
            </div>
          </el-upload>
        </div>

        <!-- 预览 -->
        <div v-if="uploadedFile" class="font-preview-box">
          <label class="text-sm text-gray-700">预览效果</label>
          <div class="preview-text" :style="previewStyle">
            {{ fontName || '自定义字体' }} - 学生管理系统
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="cancelUpload">取消</el-button>
        <el-button
          type="primary"
          :disabled="!fontName || !uploadedFile"
          @click="confirmAddFont"
        >
          确定添加
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Type, Upload, Trash2, Check } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import type { CustomFont } from '@/types/loginCustomization'
import { useAuthStore } from '@/stores/auth'

interface Props {
  modelValue: CustomFont[]
  selectedFontId: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: CustomFont[]): void
  (e: 'update:selectedFontId', value: string): void
}>()

const authStore = useAuthStore()

const fonts = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const showDialog = ref(false)
const fontName = ref('')
const uploadedFile = ref<{
  name: string
  url: string
  format: 'truetype' | 'woff' | 'woff2'
} | null>(null)

// 上传相关
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
const uploadUrl = `${baseURL}/upload/font`
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${authStore.token}`
}))

// 预览样式
const previewFontFamily = ref('')
const previewStyle = computed(() => ({
  fontFamily: previewFontFamily.value || 'inherit',
  fontSize: '24px'
}))

// 加载自定义字体
const loadFontForPreview = async (url: string, family: string, format: string) => {
  try {
    const formatMap: Record<string, string> = {
      truetype: 'truetype',
      woff: 'woff',
      woff2: 'woff2'
    }
    const fontFace = new FontFace(family, `url(${url})`, {
      style: 'normal',
      weight: '400'
    })
    await fontFace.load()
    document.fonts.add(fontFace)
    previewFontFamily.value = family
  } catch (error) {
    console.error('加载字体失败:', error)
  }
}

// 在组件挂载时加载所有已保存的自定义字体
onMounted(() => {
  fonts.value.forEach(font => {
    loadFontForPreview(font.url, font.fontFamily, font.format)
  })
})

const selectFont = (id: string) => {
  // 点击已选中的取消选中
  if (props.selectedFontId === id) {
    emit('update:selectedFontId', '')
  } else {
    emit('update:selectedFontId', id)
  }
}

const onSelectFontForTitle = (id: string) => {
  emit('update:selectedFontId', id || '')
}

const deleteFont = (id: string) => {
  const newFonts = fonts.value.filter(f => f.id !== id)
  emit('update:modelValue', newFonts)
  if (props.selectedFontId === id) {
    emit('update:selectedFontId', '')
  }
}

const beforeUpload = (file: File) => {
  const ext = file.name.split('.').pop()?.toLowerCase()
  const allowedExts = ['ttf', 'woff', 'woff2']

  if (!ext || !allowedExts.includes(ext)) {
    ElMessage.error('只支持 .ttf / .woff / .woff2 格式的字体文件')
    return false
  }

  const maxSize = 10 * 1024 * 1024 // 10MB
  if (file.size > maxSize) {
    ElMessage.error('字体文件不能超过 10MB')
    return false
  }

  return true
}

const onUploadSuccess = (response: any, file: any) => {
  if (response.code === 200 && response.data) {
    const ext = file.name.split('.').pop()?.toLowerCase()
    const formatMap: Record<string, 'truetype' | 'woff' | 'woff2'> = {
      ttf: 'truetype',
      woff: 'woff',
      woff2: 'woff2'
    }

    uploadedFile.value = {
      name: file.name,
      url: response.data.url,
      format: formatMap[ext || 'ttf'] || 'truetype'
    }

    // 生成唯一的 fontFamily 名称
    const fontFamily = `custom-font-${Date.now()}`
    loadFontForPreview(response.data.url, fontFamily, uploadedFile.value.format)

    ElMessage.success('字体上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const onUploadError = (error: any) => {
  console.error('上传失败:', error)
  ElMessage.error('字体上传失败，请重试')
}

const cancelUpload = () => {
  showDialog.value = false
  fontName.value = ''
  uploadedFile.value = null
  previewFontFamily.value = ''
}

const confirmAddFont = () => {
  if (!fontName.value || !uploadedFile.value) return

  const newFont: CustomFont = {
    id: `font-${Date.now()}`,
    name: fontName.value,
    url: uploadedFile.value.url,
    fontFamily: previewFontFamily.value || `custom-font-${Date.now()}`,
    format: uploadedFile.value.format
  }

  emit('update:modelValue', [...fonts.value, newFont])

  cancelUpload()
}
</script>

<style scoped>
.font-uploader {
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.manager-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.font-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.font-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.font-item:hover {
  border-color: #a5b4fc;
}

.font-item.selected {
  border-color: #6366f1;
  background: #eef2ff;
}

.font-preview {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border-radius: 6px;
  font-size: 18px;
  color: #374151;
}

.font-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.font-name {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.font-format {
  font-size: 11px;
  color: #9ca3af;
}

.delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background: transparent;
  color: #9ca3af;
  opacity: 0;
  transition: all 0.2s;
}

.font-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: #fee2e2;
  color: #ef4444;
}

.add-font {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px;
  border: 2px dashed #d1d5db;
  border-radius: 8px;
  background: white;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.2s;
}

.add-font:hover {
  border-color: #6366f1;
  color: #6366f1;
  background: #eef2ff;
}

.add-font span {
  font-size: 12px;
}

.current-font-setting {
  padding: 12px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.dialog-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.font-upload-area {
  width: 100%;
}

.font-upload-area :deep(.el-upload) {
  width: 100%;
}

.font-upload-area :deep(.el-upload-dragger) {
  width: 100%;
  padding: 24px;
}

.upload-placeholder,
.upload-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.font-preview-box {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.preview-text {
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
  text-align: center;
  color: #374151;
}
</style>
