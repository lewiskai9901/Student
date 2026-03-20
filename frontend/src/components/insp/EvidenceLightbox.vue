<template>
  <Teleport to="body">
    <Transition name="lightbox-fade">
      <div
        v-if="visible"
        class="lightbox-overlay"
        @click.self="close"
        @keydown.esc="close"
        @keydown.left="prev"
        @keydown.right="next"
        tabindex="0"
        ref="overlayRef"
      >
        <!-- Close button -->
        <button class="lightbox-close" @click="close" title="关闭">
          <el-icon :size="24"><Close /></el-icon>
        </button>

        <!-- Image counter -->
        <div class="lightbox-counter" v-if="images.length > 1">
          {{ currentIndex + 1 }} / {{ images.length }}
        </div>

        <!-- Main image area -->
        <div class="lightbox-content">
          <!-- Prev button -->
          <button
            v-if="images.length > 1"
            class="lightbox-nav lightbox-nav-prev"
            :disabled="currentIndex === 0"
            @click.stop="prev"
            title="上一张"
          >
            <el-icon :size="28"><ArrowLeft /></el-icon>
          </button>

          <!-- Image display -->
          <div class="lightbox-image-wrapper">
            <Transition :name="slideDirection" mode="out-in">
              <img
                v-if="currentSrc"
                :key="currentIndex"
                :src="currentSrc"
                :alt="`Evidence ${currentIndex + 1}`"
                class="lightbox-image"
                :style="imageStyle"
                @load="onImageLoad"
                @error="onImageError"
              />
            </Transition>
            <div v-if="loading" class="lightbox-loading">
              <el-icon :size="32" class="is-loading"><Loading /></el-icon>
            </div>
            <div v-if="loadError" class="lightbox-error">
              <el-icon :size="32"><WarningFilled /></el-icon>
              <span>图片加载失败</span>
            </div>
          </div>

          <!-- Next button -->
          <button
            v-if="images.length > 1"
            class="lightbox-nav lightbox-nav-next"
            :disabled="currentIndex === images.length - 1"
            @click.stop="next"
            title="下一张"
          >
            <el-icon :size="28"><ArrowRight /></el-icon>
          </button>
        </div>

        <!-- Zoom controls -->
        <div class="lightbox-toolbar">
          <button class="toolbar-btn" @click="zoomOut" title="缩小">
            <el-icon :size="18"><ZoomOut /></el-icon>
          </button>
          <span class="toolbar-text">{{ Math.round(scale * 100) }}%</span>
          <button class="toolbar-btn" @click="zoomIn" title="放大">
            <el-icon :size="18"><ZoomIn /></el-icon>
          </button>
          <button class="toolbar-btn" @click="resetZoom" title="重置">
            <el-icon :size="18"><RefreshRight /></el-icon>
          </button>
        </div>

        <!-- Thumbnail strip -->
        <div v-if="images.length > 1" class="lightbox-thumbs">
          <button
            v-for="(img, idx) in images"
            :key="idx"
            class="thumb-item"
            :class="{ active: idx === currentIndex }"
            @click.stop="goTo(idx)"
          >
            <img :src="img" :alt="`Thumb ${idx + 1}`" />
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import {
  Close,
  ArrowLeft,
  ArrowRight,
  ZoomIn,
  ZoomOut,
  RefreshRight,
  Loading,
  WarningFilled,
} from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  visible: boolean
  images: string[]
  initialIndex?: number
}>(), {
  initialIndex: 0,
})

const emit = defineEmits<{
  'update:visible': [val: boolean]
}>()

const overlayRef = ref<HTMLElement | null>(null)
const currentIndex = ref(0)
const scale = ref(1)
const loading = ref(false)
const loadError = ref(false)
const slideDirection = ref<'slide-left' | 'slide-right'>('slide-left')

const currentSrc = computed(() => {
  if (currentIndex.value >= 0 && currentIndex.value < props.images.length) {
    return props.images[currentIndex.value]
  }
  return ''
})

const imageStyle = computed(() => ({
  transform: `scale(${scale.value})`,
  transition: 'transform 0.2s ease',
}))

watch(() => props.visible, (val) => {
  if (val) {
    currentIndex.value = props.initialIndex
    scale.value = 1
    loadError.value = false
    loading.value = true
    nextTick(() => {
      overlayRef.value?.focus()
    })
    // Prevent body scrolling
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})

function close() {
  emit('update:visible', false)
}

function prev() {
  if (currentIndex.value > 0) {
    slideDirection.value = 'slide-right'
    currentIndex.value--
    resetImageState()
  }
}

function next() {
  if (currentIndex.value < props.images.length - 1) {
    slideDirection.value = 'slide-left'
    currentIndex.value++
    resetImageState()
  }
}

function goTo(index: number) {
  if (index === currentIndex.value) return
  slideDirection.value = index > currentIndex.value ? 'slide-left' : 'slide-right'
  currentIndex.value = index
  resetImageState()
}

function resetImageState() {
  scale.value = 1
  loadError.value = false
  loading.value = true
}

function zoomIn() {
  scale.value = Math.min(scale.value + 0.25, 3)
}

function zoomOut() {
  scale.value = Math.max(scale.value - 0.25, 0.25)
}

function resetZoom() {
  scale.value = 1
}

function onImageLoad() {
  loading.value = false
  loadError.value = false
}

function onImageError() {
  loading.value = false
  loadError.value = true
}
</script>

<style scoped>
.lightbox-overlay {
  position: fixed; inset: 0; z-index: 3000;
  background: rgba(0, 0, 0, 0.85);
  display: flex; flex-direction: column; align-items: center; justify-content: center; outline: none;
}
.lightbox-close, .lightbox-nav, .toolbar-btn {
  border: none; border-radius: 50%; color: #fff; cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: background 0.2s;
}
.lightbox-close {
  position: absolute; top: 16px; right: 16px; z-index: 10;
  width: 40px; height: 40px; background: rgba(255, 255, 255, 0.15);
}
.lightbox-close:hover, .lightbox-nav:hover:not(:disabled) { background: rgba(255, 255, 255, 0.25); }
.lightbox-counter {
  position: absolute; top: 20px; left: 50%; transform: translateX(-50%);
  color: rgba(255, 255, 255, 0.8); font-size: 14px; z-index: 10;
}
.lightbox-content {
  display: flex; align-items: center; justify-content: center;
  width: 100%; flex: 1; min-height: 0; padding: 60px 20px 20px;
}
.lightbox-nav {
  flex-shrink: 0; width: 48px; height: 48px;
  background: rgba(255, 255, 255, 0.1); z-index: 5;
}
.lightbox-nav:disabled { opacity: 0.3; cursor: not-allowed; }
.lightbox-nav-prev { margin-right: 16px; }
.lightbox-nav-next { margin-left: 16px; }
.lightbox-image-wrapper {
  position: relative; flex: 1; display: flex; align-items: center; justify-content: center;
  max-width: calc(100vw - 200px); max-height: calc(100vh - 180px); overflow: hidden;
}
.lightbox-image {
  max-width: 100%; max-height: calc(100vh - 180px);
  object-fit: contain; user-select: none; -webkit-user-drag: none;
}
.lightbox-loading, .lightbox-error {
  position: absolute; display: flex; flex-direction: column; align-items: center;
  gap: 8px; color: rgba(255, 255, 255, 0.6); font-size: 14px;
}
.lightbox-toolbar {
  display: flex; align-items: center; gap: 8px; padding: 8px 16px;
  background: rgba(0, 0, 0, 0.4); border-radius: 20px; margin-top: 12px;
}
.toolbar-btn { width: 32px; height: 32px; background: transparent; color: rgba(255, 255, 255, 0.8); }
.toolbar-btn:hover { background: rgba(255, 255, 255, 0.15); }
.toolbar-text { color: rgba(255, 255, 255, 0.7); font-size: 12px; min-width: 40px; text-align: center; }
.lightbox-thumbs { display: flex; gap: 6px; padding: 10px; overflow-x: auto; max-width: 80vw; }
.thumb-item {
  flex-shrink: 0; width: 48px; height: 48px; border: 2px solid transparent;
  border-radius: 4px; overflow: hidden; cursor: pointer; padding: 0; background: none;
  opacity: 0.5; transition: opacity 0.2s, border-color 0.2s;
}
.thumb-item.active { border-color: #409EFF; opacity: 1; }
.thumb-item:hover { opacity: 0.9; }
.thumb-item img { width: 100%; height: 100%; object-fit: cover; }
/* Transitions */
.lightbox-fade-enter-active, .lightbox-fade-leave-active { transition: opacity 0.25s ease; }
.lightbox-fade-enter-from, .lightbox-fade-leave-to { opacity: 0; }
.slide-left-enter-active, .slide-left-leave-active,
.slide-right-enter-active, .slide-right-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.slide-left-enter-from { opacity: 0; transform: translateX(30px); }
.slide-left-leave-to { opacity: 0; transform: translateX(-30px); }
.slide-right-enter-from { opacity: 0; transform: translateX(-30px); }
.slide-right-leave-to { opacity: 0; transform: translateX(30px); }
</style>
