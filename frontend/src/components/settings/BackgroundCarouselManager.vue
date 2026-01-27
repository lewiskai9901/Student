<template>
  <div class="carousel-manager">
    <div class="manager-header">
      <h4 class="text-sm font-semibold text-gray-900 flex items-center gap-2">
        <Images class="h-4 w-4 text-indigo-600" />
        背景轮播
      </h4>
      <!-- 启用开关 -->
      <button
        type="button"
        @click="toggleEnabled"
        :class="[
          'relative inline-flex h-5 w-9 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none',
          config.enabled ? 'bg-indigo-600' : 'bg-gray-300'
        ]"
      >
        <span
          :class="[
            'pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out',
            config.enabled ? 'translate-x-4' : 'translate-x-0'
          ]"
        />
      </button>
    </div>

    <p class="text-xs text-gray-500 mb-3">
      启用后支持多个媒体自动轮播（图片/GIF/视频），最多5个。仅在"图片/GIF"背景模式下生效。
    </p>

    <!-- 轮播设置 (仅启用时显示) -->
    <div v-if="config.enabled" class="carousel-settings">
      <!-- 媒体列表 -->
      <div class="media-list">
        <div class="list-header">
          <span class="text-xs text-gray-600">轮播媒体</span>
          <span class="text-xs text-gray-400">{{ config.items.length }}/5</span>
        </div>

        <!-- 拖拽排序列表 -->
        <div class="media-items">
          <div
            v-for="(item, index) in config.items"
            :key="item.id"
            class="media-item"
          >
            <div class="item-preview">
              <video
                v-if="item.type === 'video'"
                :src="item.url"
                muted
                class="preview-media"
              />
              <img v-else :src="item.url" class="preview-media" />
              <span v-if="item.type === 'gif'" class="type-badge gif">GIF</span>
              <span v-else-if="item.type === 'video'" class="type-badge video">
                <Film class="h-2.5 w-2.5" />
              </span>
            </div>
            <div class="item-info">
              <span class="text-xs text-gray-600">{{ getItemLabel(item.type) }} {{ index + 1 }}</span>
              <span class="text-xs text-gray-400">{{ item.type.toUpperCase() }}</span>
            </div>
            <div class="item-actions">
              <button
                type="button"
                class="action-btn"
                @click="moveUp(index)"
                :disabled="index === 0"
                title="上移"
              >
                <ChevronUp class="h-3 w-3" />
              </button>
              <button
                type="button"
                class="action-btn"
                @click="moveDown(index)"
                :disabled="index === config.items.length - 1"
                title="下移"
              >
                <ChevronDown class="h-3 w-3" />
              </button>
              <button
                type="button"
                class="action-btn delete"
                @click="deleteItem(index)"
                title="删除"
              >
                <Trash2 class="h-3 w-3" />
              </button>
            </div>
          </div>

          <!-- 添加按钮 -->
          <button
            v-if="config.items.length < 5"
            type="button"
            class="add-item"
            @click="showUploader = true"
          >
            <Plus class="h-4 w-4" />
            <span>添加媒体</span>
          </button>
        </div>
      </div>

      <!-- 轮播设置 -->
      <div class="settings-grid">
        <!-- 切换间隔 -->
        <div class="setting-item">
          <div class="flex items-center justify-between mb-1">
            <label class="text-xs text-gray-600">切换间隔</label>
            <span class="text-xs text-gray-500">{{ config.interval }}秒</span>
          </div>
          <input
            type="range"
            :value="config.interval"
            @input="updateInterval(Number(($event.target as HTMLInputElement).value))"
            min="3"
            max="30"
            step="1"
            class="w-full h-1.5 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-indigo-600"
          />
        </div>

        <!-- 切换动画 -->
        <div class="setting-item">
          <label class="text-xs text-gray-600 block mb-2">切换动画</label>
          <div class="flex gap-2">
            <button
              type="button"
              @click="updateTransition('fade')"
              :class="[
                'flex-1 py-1.5 rounded text-xs font-medium transition-all',
                config.transition === 'fade'
                  ? 'bg-indigo-600 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              ]"
            >
              淡入淡出
            </button>
            <button
              type="button"
              @click="updateTransition('slide')"
              :class="[
                'flex-1 py-1.5 rounded text-xs font-medium transition-all',
                config.transition === 'slide'
                  ? 'bg-indigo-600 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              ]"
            >
              滑动切换
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 上传对话框 -->
    <el-dialog
      v-model="showUploader"
      title="添加轮播媒体"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="upload-dialog-content">
        <p class="text-sm text-gray-500 mb-4">
          支持图片（JPG/PNG/GIF）和视频（MP4/WebM）。
          推荐比例：{{ aspectRatioLabel }}
        </p>
        <ImageCropper
          v-model="tempMediaUrl"
          :aspect-ratio="aspectRatio"
          :max-size-m-b="50"
          accept-types="image/jpeg,image/png,image/gif,image/webp,video/mp4,video/webm"
          :preview-width="previewWidth"
          :preview-height="previewHeight"
          @uploaded="onMediaUploaded"
        />
      </div>
      <template #footer>
        <el-button @click="showUploader = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!tempMediaUrl"
          @click="confirmAddMedia"
        >
          确定添加
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Images, Plus, Trash2, ChevronUp, ChevronDown, Film } from 'lucide-vue-next'
import ImageCropper from '@/components/common/ImageCropper.vue'
import type { BackgroundCarouselConfig, CarouselItem, DisplayMode } from '@/types/loginCustomization'

interface Props {
  modelValue: BackgroundCarouselConfig
  displayMode?: DisplayMode
}

const props = withDefaults(defineProps<Props>(), {
  displayMode: 'halfScreen'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: BackgroundCarouselConfig): void
}>()

const config = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const showUploader = ref(false)
const tempMediaUrl = ref('')
const tempMediaType = ref<'image' | 'gif' | 'video'>('image')

// 根据显示模式计算裁切比例
const aspectRatio = computed(() => {
  return props.displayMode === 'fullScreen' ? 16 / 9 : 3 / 4
})

const aspectRatioLabel = computed(() => {
  return props.displayMode === 'fullScreen' ? '16:9（横向）' : '3:4（竖向）'
})

const previewWidth = computed(() => {
  return props.displayMode === 'fullScreen' ? 200 : 150
})

const previewHeight = computed(() => {
  return props.displayMode === 'fullScreen' ? 112 : 200
})

const getItemLabel = (type: 'image' | 'gif' | 'video') => {
  switch (type) {
    case 'video': return '视频'
    case 'gif': return '动图'
    default: return '图片'
  }
}

const toggleEnabled = () => {
  emit('update:modelValue', {
    ...config.value,
    enabled: !config.value.enabled
  })
}

const updateInterval = (value: number) => {
  emit('update:modelValue', {
    ...config.value,
    interval: value
  })
}

const updateTransition = (value: 'fade' | 'slide') => {
  emit('update:modelValue', {
    ...config.value,
    transition: value
  })
}

const moveUp = (index: number) => {
  if (index === 0) return
  const items = [...config.value.items]
  ;[items[index - 1], items[index]] = [items[index], items[index - 1]]
  emit('update:modelValue', { ...config.value, items })
}

const moveDown = (index: number) => {
  if (index === config.value.items.length - 1) return
  const items = [...config.value.items]
  ;[items[index], items[index + 1]] = [items[index + 1], items[index]]
  emit('update:modelValue', { ...config.value, items })
}

const deleteItem = (index: number) => {
  const items = config.value.items.filter((_, i) => i !== index)
  emit('update:modelValue', { ...config.value, items })
}

const onMediaUploaded = (data: { url: string; type?: 'image' | 'gif' | 'video' }) => {
  tempMediaUrl.value = data.url
  // 检测媒体类型
  if (data.type === 'video' || data.url.toLowerCase().match(/\.(mp4|webm)$/)) {
    tempMediaType.value = 'video'
  } else if (data.type === 'gif' || data.url.toLowerCase().endsWith('.gif')) {
    tempMediaType.value = 'gif'
  } else {
    tempMediaType.value = 'image'
  }
}

const confirmAddMedia = () => {
  if (!tempMediaUrl.value) return

  const newItem: CarouselItem = {
    id: `carousel-${Date.now()}`,
    url: tempMediaUrl.value,
    type: tempMediaType.value
  }

  emit('update:modelValue', {
    ...config.value,
    items: [...config.value.items, newItem]
  })

  tempMediaUrl.value = ''
  showUploader.value = false
}
</script>

<style scoped>
.carousel-manager {
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

.carousel-settings {
  margin-top: 12px;
}

.media-list {
  margin-bottom: 16px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.media-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.media-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.item-preview {
  position: relative;
  width: 48px;
  height: 27px;
  border-radius: 4px;
  overflow: hidden;
  background: #f3f4f6;
}

.preview-media {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.type-badge {
  position: absolute;
  bottom: 2px;
  right: 2px;
  padding: 0 3px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 8px;
  border-radius: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.type-badge.video {
  background: rgba(99, 102, 241, 0.8);
}

.type-badge.gif {
  background: rgba(16, 185, 129, 0.8);
}

.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-actions {
  display: flex;
  gap: 4px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  background: #f3f4f6;
  color: #6b7280;
  transition: all 0.2s;
}

.action-btn:hover:not(:disabled) {
  background: #e5e7eb;
  color: #374151;
}

.action-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.action-btn.delete:hover {
  background: #fee2e2;
  color: #ef4444;
}

.add-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px;
  border: 2px dashed #d1d5db;
  border-radius: 8px;
  background: white;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.2s;
}

.add-item:hover {
  border-color: #6366f1;
  color: #6366f1;
  background: #eef2ff;
}

.add-item span {
  font-size: 12px;
}

.settings-grid {
  display: grid;
  gap: 12px;
}

.setting-item {
  padding: 12px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.upload-dialog-content {
  padding: 8px 0;
}
</style>
