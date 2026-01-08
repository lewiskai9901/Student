<template>
  <div class="photo-uploader">
    <!-- 触发按钮 -->
    <button
      @click="openDialog"
      :disabled="disabled"
      :class="[
        'flex items-center justify-center gap-1.5 rounded-lg py-2 text-xs font-medium transition-all',
        disabled
          ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
          : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
      ]"
      :style="{ width: buttonWidth }"
    >
      <Camera class="h-3.5 w-3.5" />
      <span>{{ buttonText }}</span>
      <span v-if="photos.length > 0" class="rounded bg-blue-500 px-1.5 text-white">{{ photos.length }}</span>
    </button>

    <!-- 照片预览缩略图 -->
    <div v-if="showThumbnails && photos.length > 0" class="mt-2 flex flex-wrap gap-1.5">
      <div
        v-for="(url, index) in photos.slice(0, maxThumbnails)"
        :key="index"
        class="relative group"
      >
        <img
          :src="url"
          class="h-12 w-12 rounded-lg object-cover cursor-pointer border border-gray-200 hover:border-blue-400 transition-colors"
          @click="previewPhoto(index)"
        />
        <button
          v-if="!disabled"
          @click.stop="removePhoto(index)"
          class="absolute -right-1 -top-1 h-4 w-4 rounded-full bg-red-500 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
        >
          <X class="h-3 w-3" />
        </button>
      </div>
      <div
        v-if="photos.length > maxThumbnails"
        class="h-12 w-12 rounded-lg bg-gray-100 flex items-center justify-center text-xs text-gray-500 font-medium"
      >
        +{{ photos.length - maxThumbnails }}
      </div>
    </div>

    <!-- 上传对话框 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="dialogVisible" class="fixed inset-0 z-[9999] flex items-center justify-center">
          <div class="fixed inset-0 bg-black/60 backdrop-blur-sm" @click="closeDialog"></div>
          <Transition name="slide-up">
            <div v-if="dialogVisible" class="relative w-full max-w-2xl rounded-2xl bg-white shadow-2xl overflow-hidden">
              <!-- 头部 -->
              <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4 bg-gradient-to-r from-blue-50 to-indigo-50">
                <div class="flex items-center gap-3">
                  <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-500 shadow-lg shadow-blue-500/30">
                    <Camera class="h-5 w-5 text-white" />
                  </div>
                  <div>
                    <h3 class="text-lg font-semibold text-gray-900">{{ dialogTitle }}</h3>
                    <p class="text-xs text-gray-500">最多可上传 {{ maxCount }} 张照片</p>
                  </div>
                </div>
                <button @click="closeDialog" class="rounded-lg p-2 hover:bg-gray-100 transition-colors">
                  <X class="h-5 w-5 text-gray-500" />
                </button>
              </div>

              <!-- 内容区 -->
              <div class="p-6">
                <!-- 上传区域 -->
                <div class="mb-4">
                  <el-upload
                    ref="uploadRef"
                    :action="uploadUrl"
                    :headers="uploadHeaders"
                    list-type="picture-card"
                    :file-list="fileList"
                    :on-success="handleUploadSuccess"
                    :on-error="handleUploadError"
                    :on-remove="handleRemove"
                    :on-preview="handlePreview"
                    :before-upload="beforeUpload"
                    :limit="maxCount"
                    :on-exceed="handleExceed"
                    accept="image/*"
                    :disabled="uploading"
                    class="photo-upload-area"
                  >
                    <div class="flex flex-col items-center justify-center py-3">
                      <div class="mb-2 flex h-10 w-10 items-center justify-center rounded-full bg-blue-50">
                        <Plus class="h-5 w-5 text-blue-500" />
                      </div>
                      <span class="text-xs text-gray-500">点击上传</span>
                    </div>
                  </el-upload>
                </div>

                <!-- 提示信息 -->
                <div class="flex items-center gap-2 rounded-lg bg-gray-50 px-4 py-3 text-xs text-gray-500">
                  <Info class="h-4 w-4 text-gray-400" />
                  <span>支持 JPG、PNG、GIF 格式，单张图片不超过 {{ maxSize }}MB</span>
                </div>
              </div>

              <!-- 底部按钮 -->
              <div class="flex items-center justify-between border-t border-gray-100 px-6 py-4 bg-gray-50">
                <span class="text-sm text-gray-500">
                  已选择 <span class="font-semibold text-blue-600">{{ fileList.length }}</span> / {{ maxCount }} 张
                </span>
                <div class="flex gap-3">
                  <button
                    @click="closeDialog"
                    class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 transition-colors"
                  >
                    取消
                  </button>
                  <button
                    @click="confirmPhotos"
                    :disabled="uploading"
                    class="rounded-lg bg-blue-600 px-6 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50 transition-colors shadow-lg shadow-blue-600/30"
                  >
                    {{ uploading ? '上传中...' : '确定' }}
                  </button>
                </div>
              </div>
            </div>
          </Transition>
        </div>
      </Transition>
    </Teleport>

    <!-- 图片预览对话框 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="previewVisible" class="fixed inset-0 z-[10000] flex items-center justify-center bg-black/90" @click="previewVisible = false">
          <button
            @click.stop="previewVisible = false"
            class="absolute right-4 top-4 rounded-full bg-white/10 p-2 text-white hover:bg-white/20 transition-colors"
          >
            <X class="h-6 w-6" />
          </button>

          <!-- 切换按钮 -->
          <button
            v-if="photos.length > 1"
            @click.stop="prevPreview"
            class="absolute left-4 top-1/2 -translate-y-1/2 rounded-full bg-white/10 p-3 text-white hover:bg-white/20 transition-colors"
          >
            <ChevronLeft class="h-6 w-6" />
          </button>
          <button
            v-if="photos.length > 1"
            @click.stop="nextPreview"
            class="absolute right-4 top-1/2 -translate-y-1/2 rounded-full bg-white/10 p-3 text-white hover:bg-white/20 transition-colors"
          >
            <ChevronRight class="h-6 w-6" />
          </button>

          <!-- 图片 -->
          <div class="relative max-h-[90vh] max-w-[90vw]" @click.stop>
            <img
              :src="previewUrl"
              class="max-h-[90vh] max-w-[90vw] object-contain rounded-lg"
              @click.stop
            />
            <!-- 图片计数 -->
            <div v-if="photos.length > 1" class="absolute bottom-4 left-1/2 -translate-x-1/2 rounded-full bg-black/50 px-4 py-1.5 text-sm text-white">
              {{ previewIndex + 1 }} / {{ photos.length }}
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, X, Plus, Info, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { getUploadUrl } from '@/api/upload'
import { getToken } from '@/utils/token'

interface Props {
  modelValue: string[]  // 照片URL数组
  maxCount?: number     // 最大上传数量
  maxSize?: number      // 最大文件大小(MB)
  disabled?: boolean    // 是否禁用
  buttonText?: string   // 按钮文字
  buttonWidth?: string  // 按钮宽度
  dialogTitle?: string  // 对话框标题
  showThumbnails?: boolean  // 是否显示缩略图
  maxThumbnails?: number    // 最大缩略图数量
}

const props = withDefaults(defineProps<Props>(), {
  maxCount: 5,
  maxSize: 5,
  disabled: false,
  buttonText: '照片',
  buttonWidth: '100%',
  dialogTitle: '上传照片',
  showThumbnails: true,
  maxThumbnails: 3
})

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
  'change': [value: string[]]
}>()

// 状态
const dialogVisible = ref(false)
const previewVisible = ref(false)
const previewUrl = ref('')
const previewIndex = ref(0)
const uploading = ref(false)
const uploadRef = ref()
const fileList = ref<any[]>([])

// 上传配置
const uploadUrl = computed(() => getUploadUrl())
const uploadHeaders = computed(() => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
})

// 照片列表（双向绑定）
const photos = computed({
  get: () => props.modelValue || [],
  set: (val) => {
    emit('update:modelValue', val)
    emit('change', val)
  }
})

// 同步fileList
watch(() => props.modelValue, (urls) => {
  if (urls && urls.length > 0) {
    fileList.value = urls.map((url, i) => ({
      name: `photo-${i}`,
      url,
      status: 'success'
    }))
  } else {
    fileList.value = []
  }
}, { immediate: true })

// 打开对话框
const openDialog = () => {
  if (props.disabled) return
  dialogVisible.value = true
}

// 关闭对话框
const closeDialog = () => {
  dialogVisible.value = false
}

// 上传前校验
const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
    return false
  }
  const isLtMaxSize = file.size / 1024 / 1024 < props.maxSize
  if (!isLtMaxSize) {
    ElMessage.error(`图片大小不能超过 ${props.maxSize}MB！`)
    return false
  }
  uploading.value = true
  return true
}

// 上传成功
const handleUploadSuccess = (response: any, file: any) => {
  uploading.value = false
  if (response.code === 200 && response.data) {
    const url = response.data.url || response.data.fileUrl
    const existing = fileList.value.find(f => f.uid === file.uid)
    if (existing) {
      existing.url = url
      existing.status = 'success'
    } else {
      // 如果 fileList 中没有找到，直接添加
      fileList.value.push({
        uid: file.uid,
        name: file.name,
        url: url,
        status: 'success'
      })
    }
  } else {
    ElMessage.error(response.message || '上传失败')
    // 移除失败的文件
    const index = fileList.value.findIndex(f => f.uid === file.uid)
    if (index > -1) {
      fileList.value.splice(index, 1)
    }
  }
}

// 上传失败
const handleUploadError = (error: any) => {
  uploading.value = false
  console.error('上传失败:', error)
  // 检查是否是认证问题
  if (error?.status === 401 || error?.status === 403) {
    ElMessage.error('登录已过期，请重新登录')
  } else {
    ElMessage.error('上传失败，请重试')
  }
}

// 移除文件
const handleRemove = (file: any) => {
  const index = fileList.value.findIndex(f => f.uid === file.uid || f.url === file.url)
  if (index > -1) {
    fileList.value.splice(index, 1)
  }
}

// 预览文件
const handlePreview = (file: any) => {
  previewUrl.value = file.url
  previewIndex.value = fileList.value.findIndex(f => f.url === file.url)
  previewVisible.value = true
}

// 超出限制
const handleExceed = () => {
  ElMessage.warning(`最多只能上传 ${props.maxCount} 张照片`)
}

// 确认照片
const confirmPhotos = () => {
  const urls = fileList.value
    .filter(f => f.url && f.status === 'success')
    .map(f => f.url)
  photos.value = urls
  dialogVisible.value = false
}

// 删除单张照片（缩略图模式）
const removePhoto = (index: number) => {
  const newPhotos = [...photos.value]
  newPhotos.splice(index, 1)
  photos.value = newPhotos
}

// 预览照片（缩略图模式）
const previewPhoto = (index: number) => {
  previewIndex.value = index
  previewUrl.value = photos.value[index]
  previewVisible.value = true
}

// 上一张
const prevPreview = () => {
  previewIndex.value = (previewIndex.value - 1 + photos.value.length) % photos.value.length
  previewUrl.value = photos.value[previewIndex.value]
}

// 下一张
const nextPreview = () => {
  previewIndex.value = (previewIndex.value + 1) % photos.value.length
  previewUrl.value = photos.value[previewIndex.value]
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}
.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

:deep(.photo-upload-area) {
  .el-upload-list--picture-card {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .el-upload--picture-card {
    width: 100px;
    height: 100px;
    border-radius: 12px;
    border: 2px dashed #e5e7eb;
    background: #f9fafb;
    transition: all 0.2s;

    &:hover {
      border-color: #3b82f6;
      background: #eff6ff;
    }
  }

  .el-upload-list__item {
    width: 100px;
    height: 100px;
    border-radius: 12px;
    margin: 0;

    &.is-success {
      border-color: #22c55e;
    }
  }

  .el-upload-list__item-thumbnail {
    object-fit: cover;
  }
}
</style>
