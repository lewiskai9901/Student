<template>
  <div class="login-preview">
    <div class="preview-header">
      <Eye class="h-4 w-4" />
      <span>实时预览</span>
      <span class="ml-auto text-xs text-gray-400">
        {{ config.displayMode === 'fullScreen' ? '全屏模式' : '半屏模式' }}
      </span>
    </div>
    <div class="preview-container">
      <!-- 全屏模式预览 -->
      <div v-if="config.displayMode === 'fullScreen'" class="preview-fullscreen">
        <!-- 全屏背景 -->
        <div class="absolute inset-0" :style="brandStyle">
          <!-- 背景视频 -->
          <video
            v-if="config.backgroundMode === 'video' && config.backgroundVideo"
            :src="config.backgroundVideo"
            class="absolute inset-0 w-full h-full object-cover"
            autoplay
            loop
            muted
            playsinline
          />

          <!-- 背景图片 -->
          <img
            v-else-if="config.backgroundMode === 'image' && config.backgroundImage"
            :src="config.backgroundImage"
            class="absolute inset-0 w-full h-full object-cover"
          />

          <!-- 遮罩 -->
          <div
            v-if="config.backgroundMode !== 'gradient'"
            class="absolute inset-0"
            :style="overlayStyle"
          ></div>
        </div>

        <!-- 全屏模式内容 -->
        <div class="relative z-10 h-full flex">
          <!-- 左侧品牌区域 - 可拖拽 -->
          <div
            ref="brandAreaRef"
            class="w-3/5 h-full relative"
            @mousedown="startDrag"
            @touchstart="startDrag"
          >
            <!-- 磁吸参考线 -->
            <template v-if="isDragging && activeSnapGuides.length > 0">
              <div
                v-for="guide in activeSnapGuides"
                :key="`${guide.type}-${guide.position}`"
                :class="[
                  'snap-guide',
                  guide.type === 'horizontal' ? 'snap-guide-horizontal' : 'snap-guide-vertical'
                ]"
                :style="guide.type === 'horizontal'
                  ? { top: `${guide.position}%` }
                  : { left: `${guide.position}%` }"
              >
                <span class="snap-guide-label">{{ guide.label }}</span>
              </div>
            </template>
            <!-- 可拖拽的文字区域 -->
            <div
              class="draggable-text absolute cursor-move select-none"
              :style="textPositionStyle"
              :class="{ 'dragging': isDragging }"
            >
              <div v-if="config.showLogo && config.showTitle" class="mb-2">
                <div
                  class="text-white tracking-wide text-center drop-shadow"
                  :style="titleStyle"
                  v-html="formattedTitle"
                ></div>
              </div>
              <p
                v-if="config.showSubtitle"
                class="text-[9px] text-white/90 text-center drop-shadow"
                v-html="formattedSubtitle"
              ></p>
            </div>
            <!-- 装饰图片 -->
            <img
              v-for="decImage in config.decorationImages"
              :key="decImage.id"
              :src="decImage.url"
              :style="getDecorationStyle(decImage)"
              class="decoration-image"
            />

            <!-- 拖拽提示 -->
            <div v-if="!isDragging" class="drag-hint">
              <Move class="h-3 w-3" />
              <span>拖拽调整位置</span>
            </div>
          </div>

          <!-- 右侧登录卡片 -->
          <div class="w-2/5 h-full flex items-center justify-center p-2">
            <div
              class="w-full p-2"
              :style="formContainerStyle"
            >
              <h2 :class="['text-xs font-semibold mb-2', config.formStyle === 'minimal' ? 'text-white' : 'text-gray-900']">登录</h2>
              <div class="space-y-1.5">
                <div
                  class="h-4 rounded text-[7px] flex items-center px-2"
                  :class="config.formStyle === 'minimal' ? 'text-white/70' : 'text-gray-400'"
                  :style="config.formStyle === 'minimal' ? formInputStyle : { backgroundColor: 'rgba(255,255,255,0.8)' }"
                >
                  用户名
                </div>
                <div
                  class="h-4 rounded text-[7px] flex items-center px-2"
                  :class="config.formStyle === 'minimal' ? 'text-white/70' : 'text-gray-400'"
                  :style="config.formStyle === 'minimal' ? formInputStyle : { backgroundColor: 'rgba(255,255,255,0.8)' }"
                >
                  密码
                </div>
                <div class="h-4 bg-blue-600 rounded text-[7px] text-white flex items-center justify-center mt-2">
                  登录
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部版权 -->
        <div v-if="config.showCopyright" class="absolute bottom-2 left-0 w-3/5 text-center text-white/50 text-[6px] z-10">
          {{ copyrightText }}
        </div>
      </div>

      <!-- 半屏模式预览 (原有布局) -->
      <div v-else class="preview-scale">
        <!-- 左侧品牌区域 - 可拖拽 -->
        <div
          ref="brandAreaRef"
          class="preview-brand"
          :style="brandStyle"
          @mousedown="startDrag"
          @touchstart="startDrag"
        >
          <!-- 磁吸参考线 -->
          <template v-if="isDragging && activeSnapGuides.length > 0">
            <div
              v-for="guide in activeSnapGuides"
              :key="`half-${guide.type}-${guide.position}`"
              :class="[
                'snap-guide',
                guide.type === 'horizontal' ? 'snap-guide-horizontal' : 'snap-guide-vertical'
              ]"
              :style="guide.type === 'horizontal'
                ? { top: `${guide.position}%` }
                : { left: `${guide.position}%` }"
            >
              <span class="snap-guide-label">{{ guide.label }}</span>
            </div>
          </template>
          <!-- 背景视频 -->
          <video
            v-if="config.backgroundMode === 'video' && config.backgroundVideo"
            :src="config.backgroundVideo"
            class="absolute inset-0 w-full h-full object-cover"
            autoplay
            loop
            muted
            playsinline
          />

          <!-- 背景图片 -->
          <img
            v-else-if="config.backgroundMode === 'image' && config.backgroundImage"
            :src="config.backgroundImage"
            class="absolute inset-0 w-full h-full object-cover"
          />

          <!-- 品牌内容遮罩 -->
          <div
            v-if="config.backgroundMode !== 'gradient'"
            class="absolute inset-0"
            :style="overlayStyle"
          ></div>

          <!-- 可拖拽的文字区域 -->
          <div
            class="draggable-text absolute cursor-move select-none z-10"
            :style="textPositionStyle"
            :class="{ 'dragging': isDragging }"
          >
            <!-- Logo/标题 -->
            <div v-if="config.showLogo && config.showTitle" class="mb-2">
              <div
                class="text-white tracking-wide text-center"
                :style="titleStyle"
                v-html="formattedTitle"
              ></div>
            </div>

            <!-- 标语 - 支持高亮关键词 -->
            <p
              v-if="config.showSubtitle"
              class="text-[9px] text-white/90 text-center"
              v-html="formattedSubtitle"
            ></p>
          </div>

          <!-- 装饰图片 -->
          <img
            v-for="decImage in config.decorationImages"
            :key="`half-${decImage.id}`"
            :src="decImage.url"
            :style="getDecorationStyle(decImage)"
            class="decoration-image"
          />

          <!-- 拖拽提示 -->
          <div v-if="!isDragging" class="drag-hint">
            <Move class="h-3 w-3" />
            <span>拖拽调整位置</span>
          </div>

          <!-- 底部版权 -->
          <div v-if="config.showCopyright" class="absolute bottom-2 left-0 right-0 text-center text-white/40 text-[6px]">
            {{ copyrightText }}
          </div>
        </div>

        <!-- 右侧登录区域 -->
        <div class="preview-login" :style="formBackgroundStyle">
          <div class="p-3 h-full flex flex-col justify-center" :style="formContainerStyle">
            <!-- 欢迎文字 -->
            <h2 :class="['text-xs font-semibold mb-2', config.formStyle === 'minimal' ? 'text-white' : 'text-gray-900']">登录</h2>

            <!-- 模拟表单 -->
            <div class="space-y-2">
              <div
                class="h-5 rounded text-[8px] flex items-center px-2"
                :class="config.formStyle === 'minimal' ? 'text-white/70' : 'text-gray-400'"
                :style="config.formStyle === 'minimal' ? formInputStyle : { backgroundColor: '#f3f4f6' }"
              >
                用户名
              </div>
              <div
                class="h-5 rounded text-[8px] flex items-center px-2"
                :class="config.formStyle === 'minimal' ? 'text-white/70' : 'text-gray-400'"
                :style="config.formStyle === 'minimal' ? formInputStyle : { backgroundColor: '#f3f4f6' }"
              >
                密码
              </div>
              <div class="h-5 bg-blue-600 rounded text-[8px] text-white flex items-center justify-center mt-3">
                登录
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <p class="preview-hint">
      预览比例为 1:{{ Math.round(1 / previewScale) }}，拖拽文字可调整位置
    </p>
    <p class="text-xs text-center text-gray-400 mt-1">
      当前位置: X {{ config.textPositionX }}%, Y {{ config.textPositionY }}%
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Eye, Move } from 'lucide-vue-next'
import type { LoginCustomizationConfig, SnapGuide, DecorationImage } from '@/types/loginCustomization'
import { getFontFamily, getFontWeight, SNAP_GUIDES, SNAP_THRESHOLD } from '@/types/loginCustomization'
import { useConfigStore } from '@/stores/config'

const configStore = useConfigStore()

interface Props {
  config: LoginCustomizationConfig
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:textPosition', x: number, y: number): void
}>()

// 预览缩放比例
const previewScale = 0.25

// 拖拽状态
const isDragging = ref(false)
const brandAreaRef = ref<HTMLElement | null>(null)
const startX = ref(0)
const startY = ref(0)
const startPosX = ref(0)
const startPosY = ref(0)

// 磁吸参考线状态
const activeSnapGuides = ref<SnapGuide[]>([])

/**
 * 计算磁吸位置
 */
const calculateSnap = (x: number, y: number): { x: number; y: number; guides: SnapGuide[] } => {
  let snappedX = x
  let snappedY = y
  const guides: SnapGuide[] = []

  for (const guide of SNAP_GUIDES) {
    const diff = guide.type === 'vertical'
      ? Math.abs(x - guide.position)
      : Math.abs(y - guide.position)

    if (diff <= SNAP_THRESHOLD) {
      if (guide.type === 'vertical') {
        snappedX = guide.position
      } else {
        snappedY = guide.position
      }
      guides.push(guide)
    }
  }

  return { x: snappedX, y: snappedY, guides }
}

// 品牌区域样式
const brandStyle = computed(() => {
  if (props.config.backgroundMode === 'gradient') {
    return {
      background: `linear-gradient(to bottom right, ${props.config.gradientFrom}, ${props.config.gradientVia}, ${props.config.gradientTo})`
    }
  }
  return {
    background: '#1f2937'
  }
})

// 文字位置样式 - 使用百分比定位
const textPositionStyle = computed(() => ({
  left: `${props.config.textPositionX}%`,
  top: `${props.config.textPositionY}%`,
  transform: 'translate(-50%, -50%)'
}))

// 格式化标语 - 支持用 **关键词** 语法高亮
const formattedSubtitle = computed(() => {
  const text = props.config.subtitle || ''
  // 将 **关键词** 转换为蓝色高亮的 span
  return text.replace(/\*\*(.+?)\*\*/g, '<span style="color: #60a5fa; font-weight: 600;">$1</span>')
})

// 格式化标题 - 支持 \n 换行
const formattedTitle = computed(() => {
  const text = props.config.title || ''
  // 将 \n 转换为 <br> 标签
  return text.replace(/\\n/g, '<br>')
})

// 标题样式
const titleStyle = computed(() => {
  const style: Record<string, string> = {
    fontFamily: getFontFamily(props.config.titleFontFamily),
    fontSize: `${Math.round(props.config.titleFontSize * 0.25)}px`, // 缩放预览
    fontWeight: String(getFontWeight(props.config.titleFontWeight))
  }

  if (props.config.titleTextShadow) {
    style.textShadow = '0 2px 4px rgba(0, 0, 0, 0.3)'
  }

  if (props.config.titleGradient) {
    style.background = `linear-gradient(135deg, ${props.config.titleGradientFrom}, ${props.config.titleGradientTo})`
    style.WebkitBackgroundClip = 'text'
    style.WebkitTextFillColor = 'transparent'
    style.backgroundClip = 'text'
  }

  return style
})

// 版权文字
const copyrightText = computed(() => {
  return props.config.copyrightText || configStore.systemCopyright || 'Copyright'
})

// 装饰图片样式（预览缩放）
const getDecorationStyle = (image: DecorationImage) => ({
  position: 'absolute' as const,
  left: `${image.positionX}%`,
  top: `${image.positionY}%`,
  width: `${image.width}%`,
  opacity: image.opacity / 100,
  transform: `translate(-50%, -50%) rotate(${image.rotation}deg)`,
  pointerEvents: 'none' as const,
  zIndex: 5
})

// 表单容器样式
const formContainerStyle = computed(() => {
  const opacity = props.config.formBgOpacity / 100

  switch (props.config.formStyle) {
    case 'classic':
      return {
        backgroundColor: `rgba(255, 255, 255, ${opacity})`,
        borderRadius: '8px',
        boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)'
      }
    case 'modern':
      return {
        backgroundColor: `rgba(255, 255, 255, ${opacity * 0.85})`,
        backdropFilter: 'blur(8px)',
        borderRadius: '12px',
        boxShadow: '0 8px 16px rgba(0, 0, 0, 0.15)',
        border: '1px solid rgba(255, 255, 255, 0.2)'
      }
    case 'minimal':
      return {
        backgroundColor: 'transparent',
        border: 'none'
      }
    default:
      return {
        backgroundColor: `rgba(255, 255, 255, ${opacity})`
      }
  }
})

// 表单输入框样式
const formInputStyle = computed(() => {
  switch (props.config.formStyle) {
    case 'minimal':
      return {
        backgroundColor: 'rgba(255, 255, 255, 0.1)',
        border: '1px solid rgba(255, 255, 255, 0.3)',
        color: 'white'
      }
    default:
      return {}
  }
})

// 遮罩样式
const overlayStyle = computed(() => ({
  backgroundColor: `rgba(0, 0, 0, ${props.config.overlayOpacity / 100})`
}))

// 表单背景样式（半屏模式）
const formBackgroundStyle = computed(() => {
  const opacity = props.config.formBgOpacity / 100
  return {
    backgroundColor: `rgba(249, 250, 251, ${opacity})`
  }
})

// 全屏模式表单样式
const fullScreenFormStyle = computed(() => {
  const opacity = props.config.formBgOpacity / 100
  return {
    backgroundColor: `rgba(255, 255, 255, ${opacity})`,
    backdropFilter: opacity < 1 ? 'blur(8px)' : 'none',
    boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)'
  }
})

// 开始拖拽
const startDrag = (e: MouseEvent | TouchEvent) => {
  isDragging.value = true
  const point = 'touches' in e ? e.touches[0] : e
  startX.value = point.clientX
  startY.value = point.clientY
  startPosX.value = props.config.textPositionX
  startPosY.value = props.config.textPositionY

  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  document.addEventListener('touchmove', onDrag)
  document.addEventListener('touchend', stopDrag)
}

// 拖拽中
const onDrag = (e: MouseEvent | TouchEvent) => {
  if (!isDragging.value || !brandAreaRef.value) return

  const point = 'touches' in e ? e.touches[0] : e
  const rect = brandAreaRef.value.getBoundingClientRect()

  // 计算鼠标移动的像素距离
  const dx = point.clientX - startX.value
  const dy = point.clientY - startY.value

  // 将像素转换为百分比（相对于品牌区域大小）
  const percentX = (dx / rect.width) * 100
  const percentY = (dy / rect.height) * 100

  // 计算新位置，限制在 5% - 95% 范围内
  const rawX = Math.max(5, Math.min(95, startPosX.value + percentX))
  const rawY = Math.max(5, Math.min(95, startPosY.value + percentY))

  // 应用磁吸
  const snapped = calculateSnap(rawX, rawY)
  activeSnapGuides.value = snapped.guides

  emit('update:textPosition', Math.round(snapped.x), Math.round(snapped.y))
}

// 停止拖拽
const stopDrag = () => {
  isDragging.value = false
  activeSnapGuides.value = [] // 清除参考线
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', stopDrag)
}

onUnmounted(() => {
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', stopDrag)
})
</script>

<style scoped>
.login-preview {
  background: #f9fafb;
  border-radius: 16px;
  padding: 16px;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.preview-container {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.preview-scale {
  display: flex;
  width: 100%;
  height: 200px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.preview-fullscreen {
  position: relative;
  width: 100%;
  height: 200px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.preview-brand {
  position: relative;
  width: 55%;
  overflow: hidden;
}

.preview-login {
  width: 45%;
  background: #f9fafb;
}

.preview-hint {
  margin-top: 12px;
  font-size: 12px;
  color: #9ca3af;
  text-align: center;
}

.draggable-text {
  transition: box-shadow 0.2s, transform 0.1s;
  padding: 8px;
  border-radius: 4px;
}

.draggable-text:hover {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.5);
}

.draggable-text.dragging {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.8);
  transform: translate(-50%, -50%) scale(1.02);
}

.drag-hint {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 4px;
  font-size: 8px;
  color: rgba(255, 255, 255, 0.7);
  pointer-events: none;
}

/* 磁吸参考线样式 */
.snap-guide {
  position: absolute;
  pointer-events: none;
  z-index: 100;
}

.snap-guide-horizontal {
  left: 0;
  right: 0;
  height: 1px;
  border-top: 1px dashed #3b82f6;
  transform: translateY(-50%);
}

.snap-guide-vertical {
  top: 0;
  bottom: 0;
  width: 1px;
  border-left: 1px dashed #3b82f6;
  transform: translateX(-50%);
}

.snap-guide-label {
  position: absolute;
  background: #3b82f6;
  color: white;
  font-size: 7px;
  padding: 1px 4px;
  border-radius: 2px;
  white-space: nowrap;
}

.snap-guide-horizontal .snap-guide-label {
  left: 4px;
  top: 2px;
}

.snap-guide-vertical .snap-guide-label {
  top: 4px;
  left: 4px;
}

/* 装饰图片样式 */
.decoration-image {
  max-width: none;
  height: auto;
}
</style>
