<template>
  <div>
    <!-- 附件网格 -->
    <div class="grid grid-cols-4 gap-3">
      <div
        v-for="(attachment, idx) in attachments"
        :key="idx"
        class="relative group cursor-pointer rounded-lg border border-gray-200 overflow-hidden hover:shadow-md transition-shadow"
      >
        <!-- 图片预览 -->
        <div
          v-if="isImage(attachment.url)"
          class="aspect-square bg-gray-100 relative"
          @click="previewImage(idx)"
        >
          <img
            :src="attachment.url"
            :alt="attachment.name || `附件${idx + 1}`"
            class="w-full h-full object-cover"
            @error="handleImageError"
          />
          <div class="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-20 transition-opacity flex items-center justify-center">
            <EyeIcon class="h-8 w-8 text-white opacity-0 group-hover:opacity-100 transition-opacity" />
          </div>
        </div>

        <!-- 非图片文件 -->
        <div
          v-else
          class="aspect-square bg-gray-50 flex flex-col items-center justify-center p-3"
        >
          <DocumentIcon class="h-12 w-12 text-gray-400 mb-2" />
          <p class="text-xs text-gray-600 text-center truncate w-full">
            {{ getFileName(attachment.url) }}
          </p>
        </div>

        <!-- 下载按钮 -->
        <div class="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity">
          <a
            :href="attachment.url"
            :download="attachment.name || `附件${idx + 1}`"
            class="flex items-center justify-center w-8 h-8 bg-white rounded-full shadow-md hover:bg-gray-100"
            @click.stop
          >
            <ArrowDownTrayIcon class="h-4 w-4 text-gray-600" />
          </a>
        </div>

        <!-- 文件名 -->
        <div v-if="attachment.name" class="absolute bottom-0 left-0 right-0 bg-black bg-opacity-50 text-white text-xs p-1 truncate">
          {{ attachment.name }}
        </div>
      </div>
    </div>

    <!-- 批量下载按钮 -->
    <div v-if="attachments.length > 1" class="mt-3">
      <button
        class="text-sm text-blue-600 hover:text-blue-700 flex items-center gap-1"
        @click="downloadAll"
      >
        <ArrowDownTrayIcon class="h-4 w-4" />
        下载全部附件 ({{ attachments.length }})
      </button>
    </div>

    <!-- 图片预览对话框 -->
    <Teleport to="body">
      <div
        v-if="showPreview"
        class="fixed inset-0 z-[9999] flex items-center justify-center bg-black bg-opacity-90"
        @click="closePreview"
      >
        <button
          class="absolute top-4 right-4 text-white hover:text-gray-300 z-10"
          @click="closePreview"
        >
          <XMarkIcon class="h-8 w-8" />
        </button>

        <!-- 上一张 -->
        <button
          v-if="imageAttachments.length > 1"
          class="absolute left-4 text-white hover:text-gray-300 z-10"
          @click.stop="prevImage"
        >
          <ChevronLeftIcon class="h-12 w-12" />
        </button>

        <!-- 图片容器 -->
        <div class="max-w-7xl max-h-[90vh] p-4" @click.stop>
          <img
            :src="currentPreviewUrl"
            class="max-w-full max-h-full object-contain"
            @error="handleImageError"
          />
          <p class="text-white text-center mt-4">
            {{ currentPreviewIndex + 1 }} / {{ imageAttachments.length }}
          </p>
        </div>

        <!-- 下一张 -->
        <button
          v-if="imageAttachments.length > 1"
          class="absolute right-4 text-white hover:text-gray-300 z-10"
          @click.stop="nextImage"
        >
          <ChevronRightIcon class="h-12 w-12" />
        </button>

        <!-- 下载当前图片 -->
        <div class="absolute bottom-4 left-1/2 transform -translate-x-1/2">
          <a
            :href="currentPreviewUrl"
            :download="`image-${currentPreviewIndex + 1}`"
            class="flex items-center gap-2 px-4 py-2 bg-white rounded-lg hover:bg-gray-100"
            @click.stop
          >
            <ArrowDownTrayIcon class="h-5 w-5" />
            下载图片
          </a>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { EyeIcon, DocumentIcon, ArrowDownTrayIcon, XMarkIcon, ChevronLeftIcon, ChevronRightIcon } from '@heroicons/vue/24/outline'

interface Attachment {
  url: string
  name?: string
}

const props = defineProps<{
  attachments: Attachment[]
}>()

const showPreview = ref(false)
const currentPreviewIndex = ref(0)

// 判断是否是图片
const isImage = (url: string): boolean => {
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg']
  return imageExtensions.some(ext => url.toLowerCase().endsWith(ext))
}

// 获取文件名
const getFileName = (url: string): string => {
  const parts = url.split('/')
  return parts[parts.length - 1] || '未知文件'
}

// 过滤出图片附件
const imageAttachments = computed(() => {
  return props.attachments.filter(att => isImage(att.url))
})

// 当前预览的图片URL
const currentPreviewUrl = computed(() => {
  return imageAttachments.value[currentPreviewIndex.value]?.url || ''
})

// 预览图片
const previewImage = (index: number) => {
  // 找到这是第几个图片
  const imageIndex = props.attachments
    .slice(0, index + 1)
    .filter(att => isImage(att.url))
    .length - 1

  currentPreviewIndex.value = imageIndex
  showPreview.value = true
}

// 关闭预览
const closePreview = () => {
  showPreview.value = false
}

// 上一张
const prevImage = () => {
  if (currentPreviewIndex.value > 0) {
    currentPreviewIndex.value--
  } else {
    currentPreviewIndex.value = imageAttachments.value.length - 1
  }
}

// 下一张
const nextImage = () => {
  if (currentPreviewIndex.value < imageAttachments.value.length - 1) {
    currentPreviewIndex.value++
  } else {
    currentPreviewIndex.value = 0
  }
}

// 处理图片加载错误
const handleImageError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="100" height="100"%3E%3Crect fill="%23ddd" width="100" height="100"/%3E%3Ctext fill="%23999" x="50%25" y="50%25" text-anchor="middle" dy=".3em"%3E加载失败%3C/text%3E%3C/svg%3E'
}

// 批量下载
const downloadAll = () => {
  props.attachments.forEach((attachment, idx) => {
    setTimeout(() => {
      const link = document.createElement('a')
      link.href = attachment.url
      link.download = attachment.name || `附件${idx + 1}`
      link.click()
    }, idx * 200) // 延迟下载避免浏览器阻止
  })
}

// 键盘事件
const handleKeydown = (e: KeyboardEvent) => {
  if (!showPreview.value) return

  if (e.key === 'Escape') {
    closePreview()
  } else if (e.key === 'ArrowLeft') {
    prevImage()
  } else if (e.key === 'ArrowRight') {
    nextImage()
  }
}

// 监听键盘事件
if (typeof window !== 'undefined') {
  window.addEventListener('keydown', handleKeydown)
}
</script>

<style scoped>
/* 确保图片不会被拉伸 */
img {
  image-rendering: -webkit-optimize-contrast;
}
</style>
