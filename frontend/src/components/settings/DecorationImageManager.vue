<template>
  <div class="decoration-image-manager">
    <div class="manager-header">
      <h4 class="text-sm font-semibold text-gray-900 flex items-center gap-2">
        <ImageIcon class="h-4 w-4 text-indigo-600" />
        装饰图片
      </h4>
      <span class="text-xs text-gray-400">{{ images.length }}/3</span>
    </div>

    <p class="text-xs text-gray-500 mb-3">
      添加装饰性图片，可拖拽到任意位置。支持PNG/JPG/GIF格式。
    </p>

    <!-- 图片列表 -->
    <div class="image-grid">
      <!-- 已有图片 -->
      <div
        v-for="image in images"
        :key="image.id"
        :class="[
          'image-card',
          selectedImageId === image.id ? 'selected' : ''
        ]"
        @click="selectImage(image.id)"
      >
        <div class="image-preview">
          <img
            :src="image.url"
            :style="{
              opacity: image.opacity / 100,
              transform: `rotate(${image.rotation}deg)`
            }"
          />
        </div>
        <div class="image-actions">
          <button
            type="button"
            class="action-btn delete"
            @click.stop="deleteImage(image.id)"
            title="删除"
          >
            <Trash2 class="h-3 w-3" />
          </button>
        </div>
      </div>

      <!-- 添加按钮 -->
      <button
        v-if="images.length < 3"
        type="button"
        class="add-card"
        @click="showUploader = true"
      >
        <Plus class="h-5 w-5" />
        <span>添加</span>
      </button>
    </div>

    <!-- 属性编辑面板 -->
    <div v-if="selectedImage" class="property-panel">
      <h5 class="text-xs font-medium text-gray-700 mb-3">属性设置</h5>

      <!-- 位置 -->
      <div class="property-row">
        <label class="text-xs text-gray-600">位置</label>
        <div class="flex gap-2">
          <div class="flex items-center gap-1">
            <span class="text-xs text-gray-400">X:</span>
            <input
              type="number"
              :value="selectedImage.positionX"
              @input="updateProperty('positionX', Number(($event.target as HTMLInputElement).value))"
              min="0"
              max="100"
              class="w-14 h-6 text-xs px-1 border border-gray-300 rounded"
            />
            <span class="text-xs text-gray-400">%</span>
          </div>
          <div class="flex items-center gap-1">
            <span class="text-xs text-gray-400">Y:</span>
            <input
              type="number"
              :value="selectedImage.positionY"
              @input="updateProperty('positionY', Number(($event.target as HTMLInputElement).value))"
              min="0"
              max="100"
              class="w-14 h-6 text-xs px-1 border border-gray-300 rounded"
            />
            <span class="text-xs text-gray-400">%</span>
          </div>
        </div>
      </div>

      <!-- 宽度 -->
      <div class="property-row">
        <div class="flex items-center justify-between">
          <label class="text-xs text-gray-600">宽度</label>
          <span class="text-xs text-gray-500">{{ selectedImage.width }}%</span>
        </div>
        <input
          type="range"
          :value="selectedImage.width"
          @input="updateProperty('width', Number(($event.target as HTMLInputElement).value))"
          min="5"
          max="50"
          step="1"
          class="w-full h-1.5 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-indigo-600"
        />
      </div>

      <!-- 透明度 -->
      <div class="property-row">
        <div class="flex items-center justify-between">
          <label class="text-xs text-gray-600">透明度</label>
          <span class="text-xs text-gray-500">{{ selectedImage.opacity }}%</span>
        </div>
        <input
          type="range"
          :value="selectedImage.opacity"
          @input="updateProperty('opacity', Number(($event.target as HTMLInputElement).value))"
          min="0"
          max="100"
          step="5"
          class="w-full h-1.5 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-indigo-600"
        />
      </div>

      <!-- 旋转 -->
      <div class="property-row">
        <div class="flex items-center justify-between">
          <label class="text-xs text-gray-600">旋转角度</label>
          <span class="text-xs text-gray-500">{{ selectedImage.rotation }}°</span>
        </div>
        <input
          type="range"
          :value="selectedImage.rotation"
          @input="updateProperty('rotation', Number(($event.target as HTMLInputElement).value))"
          min="-180"
          max="180"
          step="5"
          class="w-full h-1.5 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-indigo-600"
        />
      </div>
    </div>

    <!-- 上传对话框 -->
    <el-dialog
      v-model="showUploader"
      title="添加装饰图片"
      width="400px"
      :close-on-click-modal="false"
    >
      <ImageCropper
        v-model="tempImageUrl"
        :aspect-ratio="0"
        :max-size-m-b="5"
        accept-types="image/jpeg,image/png,image/gif,image/webp"
        :preview-width="200"
        :preview-height="150"
        @uploaded="onImageUploaded"
      />
      <template #footer>
        <el-button @click="showUploader = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!tempImageUrl"
          @click="confirmAddImage"
        >
          确定添加
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Image as ImageIcon, Plus, Trash2 } from 'lucide-vue-next'
import ImageCropper from '@/components/common/ImageCropper.vue'
import type { DecorationImage } from '@/types/loginCustomization'

interface Props {
  modelValue: DecorationImage[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: DecorationImage[]): void
}>()

const images = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const selectedImageId = ref<string | null>(null)
const showUploader = ref(false)
const tempImageUrl = ref('')

const selectedImage = computed(() => {
  if (!selectedImageId.value) return null
  return images.value.find(img => img.id === selectedImageId.value) || null
})

const selectImage = (id: string) => {
  selectedImageId.value = selectedImageId.value === id ? null : id
}

const deleteImage = (id: string) => {
  const newImages = images.value.filter(img => img.id !== id)
  emit('update:modelValue', newImages)
  if (selectedImageId.value === id) {
    selectedImageId.value = null
  }
}

const updateProperty = (
  property: keyof DecorationImage,
  value: number
) => {
  if (!selectedImageId.value) return

  const newImages = images.value.map(img => {
    if (img.id === selectedImageId.value) {
      return { ...img, [property]: value }
    }
    return img
  })
  emit('update:modelValue', newImages)
}

const onImageUploaded = (data: { url: string }) => {
  tempImageUrl.value = data.url
}

const confirmAddImage = () => {
  if (!tempImageUrl.value) return

  const newImage: DecorationImage = {
    id: `dec-${Date.now()}`,
    url: tempImageUrl.value,
    positionX: 50,
    positionY: 50,
    width: 20,
    opacity: 100,
    rotation: 0
  }

  emit('update:modelValue', [...images.value, newImage])

  tempImageUrl.value = ''
  showUploader.value = false
  selectedImageId.value = newImage.id
}
</script>

<style scoped>
.decoration-image-manager {
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

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.image-card {
  position: relative;
  aspect-ratio: 1;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.image-card:hover {
  border-color: #a5b4fc;
}

.image-card.selected {
  border-color: #6366f1;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
}

.image-preview {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
}

.image-preview img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.image-actions {
  position: absolute;
  top: 4px;
  right: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.image-card:hover .image-actions {
  opacity: 1;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  transition: background 0.2s;
}

.action-btn.delete:hover {
  background: #ef4444;
}

.add-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  aspect-ratio: 1;
  border: 2px dashed #d1d5db;
  border-radius: 8px;
  background: white;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.2s;
}

.add-card:hover {
  border-color: #6366f1;
  color: #6366f1;
  background: #eef2ff;
}

.add-card span {
  font-size: 10px;
}

.property-panel {
  padding: 12px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.property-row {
  margin-bottom: 12px;
}

.property-row:last-child {
  margin-bottom: 0;
}
</style>
