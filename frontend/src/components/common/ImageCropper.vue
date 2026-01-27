<template>
  <div class="image-cropper">
    <!-- 隐藏的文件输入框 - 始终渲染以支持重新上传 -->
    <input
      ref="fileInput"
      type="file"
      :accept="acceptTypes"
      class="hidden"
      @change="handleFileSelect"
    />

    <!-- 上传区域 -->
    <div
      v-if="!imageUrl && !previewUrl"
      class="upload-area"
      @click="triggerUpload"
      @dragover.prevent="onDragOver"
      @dragleave.prevent="onDragLeave"
      @drop.prevent="onDrop"
      :class="{ 'drag-over': isDragOver }"
    >
      <div class="upload-content">
        <Upload class="h-12 w-12 text-gray-400 mb-3" />
        <p class="text-sm text-gray-600 mb-1">点击或拖拽文件到此处上传</p>
        <p class="text-xs text-gray-400">
          支持 {{ acceptTypes }}，最大 {{ maxSizeMB }}MB
        </p>
      </div>
    </div>

    <!-- 裁切区域 -->
    <div v-else-if="showCropper && !isGifOrVideo" class="cropper-container">
      <div class="cropper-wrapper">
        <div
          ref="cropperRef"
          class="cropper-image-container"
          :style="containerStyle"
          @mousedown="startDrag"
          @touchstart="startDrag"
          @wheel.prevent="onWheel"
        >
          <img
            ref="imageRef"
            :src="imageUrl"
            :style="imageStyle"
            class="cropper-image"
            @load="onImageLoad"
          />
          <div class="crop-overlay">
            <div class="crop-area" :style="cropAreaStyle"></div>
          </div>
        </div>
      </div>

      <!-- 裁切控制 -->
      <div class="cropper-controls">
        <div class="zoom-hint mb-4">
          <Mouse class="h-4 w-4 text-gray-400" />
          <span class="text-sm text-gray-500">使用鼠标滚轮缩放图片，拖拽移动位置</span>
          <span class="ml-auto text-sm text-blue-600 font-medium">{{ (scale * 100).toFixed(1) }}%</span>
        </div>
        <div class="zoom-buttons mb-4">
          <button
            type="button"
            @click="zoomOut"
            class="zoom-btn"
            :disabled="scale <= actualMinScale"
          >
            <ZoomOut class="h-4 w-4" />
          </button>
          <div class="zoom-bar">
            <div class="zoom-bar-fill" :style="{ width: zoomBarWidth }"></div>
          </div>
          <button
            type="button"
            @click="zoomIn"
            class="zoom-btn"
            :disabled="scale >= maxScale"
          >
            <ZoomIn class="h-4 w-4" />
          </button>
          <button
            type="button"
            @click="resetZoom"
            class="zoom-btn ml-2"
            title="重置缩放"
          >
            <RotateCcw class="h-4 w-4" />
          </button>
          <div class="ml-auto flex items-center gap-2">
            <button
              type="button"
              @click="fitEntireImage"
              class="zoom-btn px-3 text-xs"
              :class="{ 'bg-blue-100 text-blue-600': isFitMode }"
              title="显示完整图片（黑边填充）"
              style="width: auto;"
            >
              <Maximize2 class="h-4 w-4 mr-1" />
              完整显示
            </button>
          </div>
        </div>
        <div class="flex gap-3">
          <button
            type="button"
            @click="cancelCrop"
            class="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
          >
            取消
          </button>
          <button
            type="button"
            @click="confirmCrop"
            class="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
          >
            确认裁切
          </button>
        </div>
      </div>
    </div>

    <!-- 预览区域 -->
    <div v-else-if="previewUrl" class="preview-container">
      <div class="preview-wrapper" :style="previewWrapperStyle">
        <video
          v-if="isVideo"
          :src="previewUrl"
          class="preview-media"
          autoplay
          loop
          muted
          playsinline
        />
        <img v-else :src="previewUrl" class="preview-media" />
      </div>
      <div class="preview-actions">
        <button
          type="button"
          @click="removeMedia"
          class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-red-600 bg-red-50 rounded-lg hover:bg-red-100 transition-colors"
        >
          <Trash2 class="h-4 w-4" />
          移除
        </button>
        <button
          type="button"
          @click="triggerUpload"
          class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
        >
          <RefreshCw class="h-4 w-4" />
          重新上传
        </button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="uploading" class="upload-loading">
      <Loader2 class="h-8 w-8 animate-spin text-blue-600" />
      <p class="mt-2 text-sm text-gray-600">上传中...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Trash2, RefreshCw, Loader2, Mouse, ZoomIn, ZoomOut, RotateCcw, Maximize2 } from 'lucide-vue-next'
import { uploadImage } from '@/api/upload'
import { uploadFile as uploadFileApi } from '@/api/file'

interface Props {
  /** 当前媒体URL */
  modelValue?: string
  /** 裁切比例 (宽:高)，如 3:4 为 0.75, 16:9 为 1.78 */
  aspectRatio?: number
  /** 最大文件大小(MB) */
  maxSizeMB?: number
  /** 接受的文件类型 */
  acceptTypes?: string
  /** 预览框宽度 */
  previewWidth?: number
  /** 预览框高度 */
  previewHeight?: number
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  aspectRatio: 3 / 4,
  maxSizeMB: 20,
  acceptTypes: 'image/*,video/mp4',
  previewWidth: 240,
  previewHeight: 320
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'uploaded', data: { url: string; type: 'image' | 'gif' | 'video' }): void
}>()

// Refs
const fileInput = ref<HTMLInputElement>()
const cropperRef = ref<HTMLElement>()
const imageRef = ref<HTMLImageElement>()

// 状态
const imageUrl = ref('')
const previewUrl = ref(props.modelValue)
const showCropper = ref(false)
const isDragOver = ref(false)
const uploading = ref(false)
const isGifOrVideo = ref(false)
const isVideo = ref(false)

// 裁切相关
const scale = ref(1)
const minScale = ref(0.1) // 填满裁切区域的最小缩放
const maxScale = 5
const offsetX = ref(0)
const offsetY = ref(0)
const imageWidth = ref(0)
const imageHeight = ref(0)
const isDragging = ref(false)
const startX = ref(0)
const startY = ref(0)
const startOffsetX = ref(0)
const startOffsetY = ref(0)
const fitScale = ref(1) // 适应容器的初始缩放
const isFitMode = ref(false) // 完整显示模式（允许黑边）
const fullFitScale = ref(0.1) // 显示完整图片所需的缩放

// 实际最小缩放（完整显示模式下允许更小的缩放）
const actualMinScale = computed(() => {
  return isFitMode.value ? Math.min(fullFitScale.value, minScale.value) : minScale.value
})

// 动态计算裁切区域尺寸 - 根据aspectRatio响应式变化
const containerBaseSize = 320 // 容器基准尺寸

const cropDimensions = computed(() => {
  const ratio = props.aspectRatio
  let width: number
  let height: number

  if (ratio >= 1) {
    // 横向比例 (如 16:9)
    width = containerBaseSize
    height = containerBaseSize / ratio
  } else {
    // 竖向比例 (如 3:4)
    height = containerBaseSize
    width = containerBaseSize * ratio
  }

  return { width, height }
})

// 容器尺寸 - 比裁切区域稍大以便有拖拽空间
const containerStyle = computed(() => {
  const { width, height } = cropDimensions.value
  return {
    width: `${width + 80}px`,
    height: `${height + 80}px`
  }
})

// 图片样式 - 需要保留居中的 translate(-50%, -50%)
const imageStyle = computed(() => ({
  transform: `translate(calc(-50% + ${offsetX.value}px), calc(-50% + ${offsetY.value}px)) scale(${scale.value})`,
  transformOrigin: 'center center'
}))

// 裁切区域样式
const cropAreaStyle = computed(() => ({
  width: `${cropDimensions.value.width}px`,
  height: `${cropDimensions.value.height}px`
}))

// 预览区域样式
const previewWrapperStyle = computed(() => ({
  width: `${props.previewWidth}px`,
  height: `${props.previewHeight}px`
}))

// 缩放进度条宽度
const zoomBarWidth = computed(() => {
  const min = actualMinScale.value
  const range = maxScale - min
  const current = scale.value - min
  return `${Math.max(0, Math.min(100, (current / range) * 100))}%`
})

// 触发文件选择
const triggerUpload = () => {
  fileInput.value?.click()
}

// 拖拽处理
const onDragOver = () => {
  isDragOver.value = true
}

const onDragLeave = () => {
  isDragOver.value = false
}

const onDrop = (e: DragEvent) => {
  isDragOver.value = false
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    handleFile(files[0])
  }
}

// 文件选择处理
const handleFileSelect = (e: Event) => {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (files && files.length > 0) {
    handleFile(files[0])
  }
  // 重置input以允许选择相同文件
  target.value = ''
}

// 处理文件
const handleFile = (file: File) => {
  // 检查文件大小
  if (file.size > props.maxSizeMB * 1024 * 1024) {
    ElMessage.error(`文件大小不能超过 ${props.maxSizeMB}MB`)
    return
  }

  // 检查文件类型
  const isGif = file.type === 'image/gif'
  const isVid = file.type.startsWith('video/')
  const isImg = file.type.startsWith('image/')

  if (!isGif && !isVid && !isImg) {
    ElMessage.error('不支持的文件类型')
    return
  }

  isGifOrVideo.value = isGif || isVid
  isVideo.value = isVid

  if (isGif || isVid) {
    // GIF和视频直接上传，不裁切
    uploadFile(file)
  } else {
    // 静态图片显示裁切器
    const reader = new FileReader()
    reader.onload = (e) => {
      imageUrl.value = e.target?.result as string
      showCropper.value = true
      scale.value = 1
      offsetX.value = 0
      offsetY.value = 0
    }
    reader.readAsDataURL(file)
  }
}

// 图片加载完成 - 改进缩放计算，让高分辨率图片能完整显示
const onImageLoad = () => {
  if (imageRef.value) {
    imageWidth.value = imageRef.value.naturalWidth
    imageHeight.value = imageRef.value.naturalHeight

    const { width: cropW, height: cropH } = cropDimensions.value

    // 计算让图片完整显示在容器内的缩放比例
    const containerW = cropW + 80
    const containerH = cropH + 80

    // 计算适应容器的缩放（让整个图片可见）
    const fitScaleX = containerW / imageWidth.value
    const fitScaleY = containerH / imageHeight.value
    fitScale.value = Math.min(fitScaleX, fitScaleY) * 0.9 // 留点边距

    // 计算最小缩放（让图片至少覆盖裁切区域）
    const coverScaleX = cropW / imageWidth.value
    const coverScaleY = cropH / imageHeight.value
    minScale.value = Math.max(coverScaleX, coverScaleY)

    // 计算完整显示图片所需的缩放（让图片完全在裁切区域内）
    const fitInCropX = cropW / imageWidth.value
    const fitInCropY = cropH / imageHeight.value
    fullFitScale.value = Math.min(fitInCropX, fitInCropY)

    // 重置完整显示模式
    isFitMode.value = false

    // 初始缩放：如果图片比容器大，先缩小到能看全图；否则保持原始大小
    if (imageWidth.value > containerW || imageHeight.value > containerH) {
      // 大图：缩小到能看到完整图片，但不小于最小缩放
      scale.value = Math.max(fitScale.value, minScale.value)
    } else {
      // 小图：保持原始大小，但不小于最小缩放
      scale.value = Math.max(1, minScale.value)
    }

    // 重置偏移
    offsetX.value = 0
    offsetY.value = 0
  }
}

// 鼠标滚轮缩放 - 每次1%的幅度
const onWheel = (e: WheelEvent) => {
  const delta = e.deltaY > 0 ? -0.01 : 0.01
  const newScale = Math.max(actualMinScale.value, Math.min(maxScale, scale.value + delta))

  // 以鼠标位置为中心缩放
  if (cropperRef.value) {
    const rect = cropperRef.value.getBoundingClientRect()
    const mouseX = e.clientX - rect.left - rect.width / 2
    const mouseY = e.clientY - rect.top - rect.height / 2

    const scaleChange = newScale / scale.value
    offsetX.value = mouseX - (mouseX - offsetX.value) * scaleChange
    offsetY.value = mouseY - (mouseY - offsetY.value) * scaleChange
  }

  scale.value = newScale
}

// 缩放按钮 - 每次5%的幅度（按钮稍微大一点）
const zoomIn = () => {
  scale.value = Math.min(maxScale, scale.value + 0.05)
}

const zoomOut = () => {
  scale.value = Math.max(actualMinScale.value, scale.value - 0.05)
}

const resetZoom = () => {
  // 重置到适应容器的缩放
  isFitMode.value = false
  scale.value = Math.max(fitScale.value, minScale.value)
  offsetX.value = 0
  offsetY.value = 0
}

// 完整显示图片（允许黑边）
const fitEntireImage = () => {
  isFitMode.value = !isFitMode.value
  if (isFitMode.value) {
    // 缩小到完整显示图片
    scale.value = fullFitScale.value
    offsetX.value = 0
    offsetY.value = 0
  } else {
    // 恢复到填满裁切区域
    scale.value = minScale.value
    offsetX.value = 0
    offsetY.value = 0
  }
}

// 开始拖拽
const startDrag = (e: MouseEvent | TouchEvent) => {
  isDragging.value = true
  const point = 'touches' in e ? e.touches[0] : e
  startX.value = point.clientX
  startY.value = point.clientY
  startOffsetX.value = offsetX.value
  startOffsetY.value = offsetY.value

  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  document.addEventListener('touchmove', onDrag)
  document.addEventListener('touchend', stopDrag)
}

// 拖拽中
const onDrag = (e: MouseEvent | TouchEvent) => {
  if (!isDragging.value) return
  const point = 'touches' in e ? e.touches[0] : e
  const dx = point.clientX - startX.value
  const dy = point.clientY - startY.value
  offsetX.value = startOffsetX.value + dx
  offsetY.value = startOffsetY.value + dy
}

// 停止拖拽
const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', stopDrag)
}

// 取消裁切
const cancelCrop = () => {
  imageUrl.value = ''
  showCropper.value = false
}

// 确认裁切
const confirmCrop = async () => {
  if (!imageRef.value || !cropperRef.value) return

  const { width: cropW, height: cropH } = cropDimensions.value

  // 创建canvas进行裁切 - 使用高分辨率输出
  // 根据原图分辨率动态计算输出比例，保证高清输出
  // 目标：至少1080p，最高保持原图分辨率
  const minOutputWidth = 1080
  const maxOutputWidth = 4096

  // 计算裁切区域在原图上对应的实际像素大小
  const sourceW = cropW / scale.value
  const sourceH = cropH / scale.value

  // 输出尺寸：取原图裁切区域大小和最小输出尺寸中的较大值，但不超过最大值
  const outputWidth = Math.min(maxOutputWidth, Math.max(minOutputWidth, sourceW))
  const outputScale = outputWidth / cropW

  const canvas = document.createElement('canvas')
  canvas.width = Math.round(cropW * outputScale)
  canvas.height = Math.round(cropH * outputScale)
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  // 启用高质量图像缩放
  ctx.imageSmoothingEnabled = true
  ctx.imageSmoothingQuality = 'high'

  // 先填充黑色背景（用于完整显示模式下的黑边）
  ctx.fillStyle = '#000000'
  ctx.fillRect(0, 0, canvas.width, canvas.height)

  // 获取容器和图片的位置信息
  const containerRect = cropperRef.value.getBoundingClientRect()
  const containerCenterX = containerRect.width / 2
  const containerCenterY = containerRect.height / 2

  // 图片在容器中的实际中心位置（考虑偏移）
  const imageCenterX = containerCenterX + offsetX.value
  const imageCenterY = containerCenterY + offsetY.value

  // 计算图片在裁切区域中的位置
  const scaledImgW = imageWidth.value * scale.value
  const scaledImgH = imageHeight.value * scale.value

  // 图片左上角相对于容器中心的位置
  const imgLeftInContainer = imageCenterX - scaledImgW / 2
  const imgTopInContainer = imageCenterY - scaledImgH / 2

  // 裁切区域左上角相对于容器中心的位置
  const cropLeftInContainer = containerCenterX - cropW / 2
  const cropTopInContainer = containerCenterY - cropH / 2

  // 计算图片相对于裁切区域的位置
  const imgLeftInCrop = imgLeftInContainer - cropLeftInContainer
  const imgTopInCrop = imgTopInContainer - cropTopInContainer

  // 如果图片完全覆盖裁切区域，使用原来的裁切逻辑
  if (scale.value >= minScale.value - 0.001) {
    // 计算裁切区域在原始图片上的坐标
    const sourceX = (cropLeftInContainer - imageCenterX + imageWidth.value * scale.value / 2) / scale.value
    const sourceY = (cropTopInContainer - imageCenterY + imageHeight.value * scale.value / 2) / scale.value
    const sourceW = cropW / scale.value
    const sourceH = cropH / scale.value

    // 绘制裁切后的图片
    ctx.drawImage(
      imageRef.value,
      sourceX,
      sourceY,
      sourceW,
      sourceH,
      0,
      0,
      canvas.width,
      canvas.height
    )
  } else {
    // 完整显示模式：图片可能不填满裁切区域
    // 计算图片在输出canvas上的位置和大小
    const destX = imgLeftInCrop * outputScale
    const destY = imgTopInCrop * outputScale
    const destW = scaledImgW * outputScale
    const destH = scaledImgH * outputScale

    // 绘制完整图片到计算出的位置
    ctx.drawImage(
      imageRef.value,
      0,
      0,
      imageWidth.value,
      imageHeight.value,
      destX,
      destY,
      destW,
      destH
    )
  }

  // 转换为Blob并上传 - 使用高质量JPEG（0.95）或PNG
  // 如果输出尺寸较大（超过2000px），使用JPEG以控制文件大小；否则使用PNG保持最高质量
  const useJpeg = canvas.width > 2000 || canvas.height > 2000
  const mimeType = useJpeg ? 'image/jpeg' : 'image/png'
  const quality = useJpeg ? 0.95 : undefined
  const extension = useJpeg ? 'jpg' : 'png'

  canvas.toBlob(async (blob) => {
    if (blob) {
      const file = new File([blob], `cropped-image.${extension}`, { type: mimeType })
      await uploadFile(file)
    }
  }, mimeType, quality)
}

// 上传文件
const uploadFile = async (file: File) => {
  uploading.value = true
  try {
    let url: string
    if (isVideo.value) {
      // 视频使用通用文件上传API
      const result = await uploadFileApi(file, 'login_background')
      url = result.fileUrl
    } else {
      // 图片使用图片上传API
      const result = await uploadImage(file)
      url = result.url
    }
    previewUrl.value = url
    emit('update:modelValue', url)
    emit('uploaded', {
      url: url,
      type: isVideo.value ? 'video' : (isGifOrVideo.value ? 'gif' : 'image')
    })
    showCropper.value = false
    imageUrl.value = ''
    ElMessage.success('上传成功')
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

// 移除媒体
const removeMedia = () => {
  previewUrl.value = ''
  emit('update:modelValue', '')
}

// 监听modelValue变化
watch(() => props.modelValue, (newVal) => {
  previewUrl.value = newVal
  if (newVal) {
    isVideo.value = newVal.endsWith('.mp4') || newVal.includes('video')
  }
}, { immediate: true })

// 监听aspectRatio变化，重新计算缩放
watch(() => props.aspectRatio, () => {
  if (showCropper.value && imageRef.value && imageWidth.value > 0) {
    // 重新计算最小缩放
    const { width: cropW, height: cropH } = cropDimensions.value
    const coverScaleX = cropW / imageWidth.value
    const coverScaleY = cropH / imageHeight.value
    minScale.value = Math.max(coverScaleX, coverScaleY)

    // 如果当前缩放小于最小缩放，调整到最小缩放
    if (scale.value < minScale.value) {
      scale.value = minScale.value
    }
  }
})

onMounted(() => {
  // 初始化
})

onUnmounted(() => {
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', stopDrag)
})
</script>

<style scoped>
.image-cropper {
  position: relative;
}

.upload-area {
  border: 2px dashed #e5e7eb;
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-area:hover,
.upload-area.drag-over {
  border-color: #3b82f6;
  background: #eff6ff;
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.cropper-container {
  background: #f3f4f6;
  border-radius: 12px;
  padding: 20px;
}

.cropper-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.cropper-image-container {
  position: relative;
  overflow: hidden;
  border-radius: 8px;
  cursor: move;
  background: #1f2937;
}

.cropper-image {
  position: absolute;
  top: 50%;
  left: 50%;
  /* transform is set via inline style */
  max-width: none;
  user-select: none;
  pointer-events: none;
}

.crop-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.crop-area {
  border: 2px solid white;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.5);
  border-radius: 4px;
}

.cropper-controls {
  background: white;
  padding: 16px;
  border-radius: 8px;
}

.zoom-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f9fafb;
  border-radius: 6px;
}

.zoom-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.zoom-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: #f3f4f6;
  color: #374151;
  transition: all 0.2s;
}

.zoom-btn:hover:not(:disabled) {
  background: #e5e7eb;
}

.zoom-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.zoom-bar {
  flex: 1;
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
}

.zoom-bar-fill {
  height: 100%;
  background: #3b82f6;
  border-radius: 3px;
  transition: width 0.1s;
}

.preview-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.preview-wrapper {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.preview-media {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-actions {
  display: flex;
  gap: 12px;
}

.upload-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
}
</style>
