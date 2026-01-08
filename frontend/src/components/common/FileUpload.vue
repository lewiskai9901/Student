<template>
  <div class="file-upload">
    <el-upload
      ref="uploadRef"
      :action="uploadUrl"
      :headers="headers"
      :data="uploadData"
      :multiple="multiple"
      :limit="limit"
      :accept="accept"
      :file-list="fileList"
      :on-success="handleSuccess"
      :on-error="handleError"
      :on-exceed="handleExceed"
      :on-remove="handleRemove"
      :before-upload="beforeUpload"
      :auto-upload="autoUpload"
      :list-type="listType"
    >
      <template v-if="listType === 'picture-card'">
        <el-icon><Plus /></el-icon>
      </template>
      <template v-else>
        <el-button type="primary" :icon="Upload">
          {{ buttonText }}
        </el-button>
        <template #tip>
          <div class="el-upload__tip">
            {{ tip }}
          </div>
        </template>
      </template>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'
import type { UploadProps, UploadUserFile, UploadFile } from 'element-plus'
import { getToken } from '@/utils/token'

interface Props {
  modelValue?: UploadUserFile[]
  businessType?: string
  businessId?: number
  multiple?: boolean
  limit?: number
  accept?: string
  maxSize?: number // MB
  autoUpload?: boolean
  listType?: 'text' | 'picture' | 'picture-card'
  buttonText?: string
  tip?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  multiple: true,
  limit: 10,
  accept: '*',
  maxSize: 10,
  autoUpload: true,
  listType: 'text',
  buttonText: '点击上传',
  tip: '只能上传jpg/png/pdf/doc/xls文件，且不超过10MB'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: UploadUserFile[]): void
  (e: 'success', file: any): void
  (e: 'error', error: any): void
}>()

const uploadRef = ref()
const fileList = ref<UploadUserFile[]>(props.modelValue)

// 上传地址
const uploadUrl = computed(() => {
  let url = import.meta.env.VITE_API_BASE_URL + '/files/upload'
  if (!url) {
    url = '/api/files/upload'
  }
  return url
})

// 上传请求头
const headers = computed(() => {
  return {
    Authorization: `Bearer ${getToken()}`
  }
})

// 上传附加数据
const uploadData = computed(() => {
  const data: any = {}
  if (props.businessType) {
    data.businessType = props.businessType
  }
  if (props.businessId) {
    data.businessId = props.businessId
  }
  return data
})

// 上传前的钩子
const beforeUpload: UploadProps['beforeUpload'] = (rawFile) => {
  // 检查文件大小
  const maxSizeInBytes = props.maxSize * 1024 * 1024
  if (rawFile.size > maxSizeInBytes) {
    ElMessage.error(`文件大小不能超过 ${props.maxSize}MB`)
    return false
  }

  // 检查文件类型
  if (props.accept !== '*') {
    const acceptTypes = props.accept.split(',')
    const fileType = rawFile.name.substring(rawFile.name.lastIndexOf('.'))
    if (!acceptTypes.some(type => fileType.toLowerCase().includes(type.toLowerCase()))) {
      ElMessage.error(`只能上传 ${props.accept} 格式的文件`)
      return false
    }
  }

  return true
}

// 上传成功
const handleSuccess: UploadProps['onSuccess'] = (response, uploadFile) => {
  if (response.code === 200) {
    ElMessage.success('上传成功')
    emit('success', response.data)
    emit('update:modelValue', fileList.value)
  } else {
    ElMessage.error(response.message || '上传失败')
    handleError(new Error(response.message), uploadFile, fileList.value)
  }
}

// 上传失败
const handleError: UploadProps['onError'] = (error, uploadFile) => {
  ElMessage.error('上传失败: ' + error.message)
  emit('error', error)
  // 从列表中移除失败的文件
  const index = fileList.value.findIndex(file => file.uid === uploadFile.uid)
  if (index > -1) {
    fileList.value.splice(index, 1)
  }
}

// 文件超出限制
const handleExceed: UploadProps['onExceed'] = () => {
  ElMessage.warning(`最多只能上传 ${props.limit} 个文件`)
}

// 移除文件
const handleRemove: UploadProps['onRemove'] = (uploadFile) => {
  const index = fileList.value.findIndex(file => file.uid === uploadFile.uid)
  if (index > -1) {
    fileList.value.splice(index, 1)
    emit('update:modelValue', fileList.value)
  }
}

// 手动上传
const submit = () => {
  uploadRef.value?.submit()
}

// 清空文件列表
const clearFiles = () => {
  uploadRef.value?.clearFiles()
  fileList.value = []
  emit('update:modelValue', [])
}

defineExpose({
  submit,
  clearFiles
})
</script>

<style scoped lang="scss">
.file-upload {
  width: 100%;
}
</style>
