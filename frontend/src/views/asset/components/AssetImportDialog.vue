<script setup lang="ts">
/**
 * 资产Excel导入对话框
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Download, Check, Warning, Document, Close } from '@element-plus/icons-vue'
import type { UploadInstance, UploadFile, UploadRawFile } from 'element-plus'
import { assetApi } from '@/api/asset'
import type { ImportResult } from '@/types/asset'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  imported: [result: ImportResult]
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const uploadRef = ref<UploadInstance>()
const loading = ref(false)
const templateLoading = ref(false)
const selectedFile = ref<UploadFile | null>(null)
const importResult = ref<ImportResult | null>(null)
const showResult = ref(false)

// 监听对话框打开，重置状态
watch(() => props.visible, (val) => {
  if (val) {
    selectedFile.value = null
    importResult.value = null
    showResult.value = false
    uploadRef.value?.clearFiles()
  }
})

// 下载导入模板
async function handleDownloadTemplate() {
  try {
    templateLoading.value = true
    const response = await assetApi.downloadImportTemplate()

    // 创建下载链接
    const blob = new Blob([response as unknown as BlobPart], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '资产导入模板.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('模板下载成功')
  } catch (error) {
    console.error('Download template failed:', error)
    ElMessage.error('模板下载失败')
  } finally {
    templateLoading.value = false
  }
}

// 文件变化处理
function handleFileChange(uploadFile: UploadFile) {
  // 验证文件类型
  const fileName = uploadFile.name.toLowerCase()
  if (!fileName.endsWith('.xlsx') && !fileName.endsWith('.xls')) {
    ElMessage.error('只支持 .xlsx 或 .xls 格式的Excel文件')
    uploadRef.value?.clearFiles()
    return
  }

  // 验证文件大小 (5MB)
  if (uploadFile.size && uploadFile.size > 5 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 5MB')
    uploadRef.value?.clearFiles()
    return
  }

  selectedFile.value = uploadFile
}

// 移除文件
function handleRemoveFile() {
  selectedFile.value = null
  uploadRef.value?.clearFiles()
}

// 执行导入
async function handleImport() {
  if (!selectedFile.value?.raw) {
    ElMessage.warning('请先选择要导入的文件')
    return
  }

  try {
    loading.value = true
    const result = await assetApi.importAssets(selectedFile.value.raw)
    importResult.value = result
    showResult.value = true

    if (result.failCount === 0) {
      ElMessage.success(`成功导入 ${result.successCount} 条资产`)
    } else {
      ElMessage.warning(`导入完成：成功 ${result.successCount} 条，失败 ${result.failCount} 条`)
    }
  } catch (error: unknown) {
    console.error('Import failed:', error)
    const errMsg = error instanceof Error ? error.message : '导入失败，请检查文件格式'
    ElMessage.error(errMsg)
  } finally {
    loading.value = false
  }
}

// 关闭对话框
function handleClose() {
  if (loading.value) return
  dialogVisible.value = false
}

// 完成并关闭
function handleFinish() {
  if (importResult.value) {
    emit('imported', importResult.value)
  }
  dialogVisible.value = false
}

// 继续导入
function handleContinue() {
  showResult.value = false
  selectedFile.value = null
  importResult.value = null
  uploadRef.value?.clearFiles()
}

// 格式化文件大小
function formatFileSize(bytes: number | undefined): string {
  if (!bytes) return '未知'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

// 阻止自动上传
function beforeUpload(file: UploadRawFile) {
  return false
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="Excel导入资产"
    width="640px"
    :close-on-click-modal="false"
    :close-on-press-escape="!loading"
    class="import-dialog"
  >
    <!-- 导入结果展示 -->
    <div v-if="showResult && importResult" class="result-section">
      <div class="text-center py-6">
        <!-- 成功/部分成功/失败图标 -->
        <div
          :class="[
            'w-20 h-20 mx-auto mb-4 rounded-full flex items-center justify-center',
            importResult.failCount === 0
              ? 'bg-green-100'
              : importResult.successCount > 0
                ? 'bg-amber-100'
                : 'bg-red-100'
          ]"
        >
          <el-icon
            :class="[
              'text-4xl',
              importResult.failCount === 0
                ? 'text-green-600'
                : importResult.successCount > 0
                  ? 'text-amber-600'
                  : 'text-red-600'
            ]"
          >
            <Check v-if="importResult.failCount === 0" />
            <Warning v-else />
          </el-icon>
        </div>

        <h3 class="text-xl font-semibold text-gray-900 mb-2">
          {{ importResult.failCount === 0 ? '导入成功' : '导入完成' }}
        </h3>

        <p class="text-gray-500 mb-6">
          共 <span class="font-semibold">{{ importResult.totalCount }}</span> 条记录，
          成功 <span class="font-semibold text-green-600">{{ importResult.successCount }}</span> 条
          <template v-if="importResult.failCount > 0">
            ，失败 <span class="font-semibold text-red-600">{{ importResult.failCount }}</span> 条
          </template>
        </p>

        <!-- 统计数据 -->
        <div class="grid grid-cols-3 gap-4 max-w-sm mx-auto mb-6">
          <div class="bg-gray-50 rounded-lg p-3">
            <div class="text-2xl font-bold text-gray-900">{{ importResult.totalCount }}</div>
            <div class="text-xs text-gray-500 mt-1">总记录</div>
          </div>
          <div class="bg-green-50 rounded-lg p-3">
            <div class="text-2xl font-bold text-green-600">{{ importResult.successCount }}</div>
            <div class="text-xs text-gray-500 mt-1">导入成功</div>
          </div>
          <div class="bg-red-50 rounded-lg p-3">
            <div class="text-2xl font-bold text-red-600">{{ importResult.failCount }}</div>
            <div class="text-xs text-gray-500 mt-1">导入失败</div>
          </div>
        </div>

        <!-- 错误详情 -->
        <div v-if="importResult.errors?.length" class="text-left">
          <h4 class="text-sm font-medium text-gray-700 mb-2 flex items-center gap-2">
            <el-icon class="text-red-500"><Warning /></el-icon>
            错误详情 ({{ importResult.errors.length }} 条)
          </h4>
          <div class="max-h-48 overflow-y-auto border border-gray-200 rounded-lg">
            <table class="w-full text-sm">
              <thead class="bg-gray-50 sticky top-0">
                <tr>
                  <th class="px-3 py-2 text-left font-medium text-gray-600 w-16">行号</th>
                  <th class="px-3 py-2 text-left font-medium text-gray-600 w-32">资产名称</th>
                  <th class="px-3 py-2 text-left font-medium text-gray-600">错误原因</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-100">
                <tr v-for="err in importResult.errors" :key="err.rowNum" class="hover:bg-gray-50">
                  <td class="px-3 py-2 text-gray-600">{{ err.rowNum }}</td>
                  <td class="px-3 py-2 text-gray-900 truncate max-w-32">{{ err.assetName || '-' }}</td>
                  <td class="px-3 py-2 text-red-600">{{ err.errorMessage }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- 导入表单 -->
    <div v-else class="import-form">
      <!-- 步骤说明 -->
      <div class="mb-6">
        <h4 class="text-sm font-medium text-gray-700 mb-3">导入步骤</h4>
        <div class="flex items-center gap-2 text-sm">
          <div class="flex items-center gap-2 px-3 py-2 bg-blue-50 text-blue-700 rounded-lg">
            <span class="w-5 h-5 rounded-full bg-blue-600 text-white text-xs flex items-center justify-center">1</span>
            下载模板
          </div>
          <el-icon class="text-gray-300"><i class="el-icon-arrow-right" /></el-icon>
          <div class="flex items-center gap-2 px-3 py-2 bg-gray-50 text-gray-600 rounded-lg">
            <span class="w-5 h-5 rounded-full bg-gray-400 text-white text-xs flex items-center justify-center">2</span>
            填写数据
          </div>
          <el-icon class="text-gray-300"><i class="el-icon-arrow-right" /></el-icon>
          <div class="flex items-center gap-2 px-3 py-2 bg-gray-50 text-gray-600 rounded-lg">
            <span class="w-5 h-5 rounded-full bg-gray-400 text-white text-xs flex items-center justify-center">3</span>
            上传导入
          </div>
        </div>
      </div>

      <!-- 下载模板 -->
      <div class="mb-6 p-4 border border-dashed border-gray-300 rounded-lg bg-gray-50">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-lg bg-green-100 flex items-center justify-center">
              <el-icon class="text-xl text-green-600"><Document /></el-icon>
            </div>
            <div>
              <div class="font-medium text-gray-900">资产导入模板.xlsx</div>
              <div class="text-xs text-gray-500 mt-0.5">包含字段说明和示例数据</div>
            </div>
          </div>
          <el-button
            type="primary"
            plain
            :loading="templateLoading"
            @click="handleDownloadTemplate"
          >
            <el-icon class="mr-1"><Download /></el-icon>
            下载模板
          </el-button>
        </div>
      </div>

      <!-- 上传区域 -->
      <div class="mb-4">
        <h4 class="text-sm font-medium text-gray-700 mb-3">上传文件</h4>

        <!-- 已选文件展示 -->
        <div v-if="selectedFile" class="p-4 border border-blue-200 bg-blue-50 rounded-lg">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-lg bg-blue-100 flex items-center justify-center">
                <el-icon class="text-xl text-blue-600"><Document /></el-icon>
              </div>
              <div>
                <div class="font-medium text-gray-900">{{ selectedFile.name }}</div>
                <div class="text-xs text-gray-500 mt-0.5">{{ formatFileSize(selectedFile.size) }}</div>
              </div>
            </div>
            <el-button text type="danger" @click="handleRemoveFile">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- 上传控件 -->
        <el-upload
          v-else
          ref="uploadRef"
          drag
          :auto-upload="false"
          :show-file-list="false"
          :limit="1"
          accept=".xlsx,.xls"
          :before-upload="beforeUpload"
          :on-change="handleFileChange"
          class="upload-area"
        >
          <div class="py-8 text-center">
            <el-icon class="text-4xl text-gray-300 mb-3"><Upload /></el-icon>
            <div class="text-gray-600 mb-1">
              将Excel文件拖到此处，或 <span class="text-blue-600">点击上传</span>
            </div>
            <div class="text-xs text-gray-400">
              支持 .xlsx / .xls 格式，文件大小不超过 5MB，单次最多导入 1000 条
            </div>
          </div>
        </el-upload>
      </div>

      <!-- 注意事项 -->
      <div class="p-3 bg-amber-50 border border-amber-200 rounded-lg">
        <div class="flex items-start gap-2">
          <el-icon class="text-amber-600 mt-0.5"><Warning /></el-icon>
          <div class="text-sm text-amber-800">
            <div class="font-medium mb-1">注意事项</div>
            <ul class="list-disc list-inside text-xs text-amber-700 space-y-0.5">
              <li>资产分类编码必须与系统中已有的分类匹配</li>
              <li>资产名称和计量单位为必填项</li>
              <li>日期格式为 yyyy-MM-dd，如 2024-01-01</li>
              <li>导入过程中如有错误，正确的数据仍会被导入</li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div v-if="showResult" class="flex justify-center gap-4">
        <el-button @click="handleContinue">
          <el-icon class="mr-1"><Upload /></el-icon>
          继续导入
        </el-button>
        <el-button type="primary" @click="handleFinish">
          完成
        </el-button>
      </div>
      <div v-else class="flex justify-end gap-2">
        <el-button :disabled="loading" @click="handleClose">取消</el-button>
        <el-button
          type="primary"
          :loading="loading"
          :disabled="!selectedFile"
          @click="handleImport"
        >
          <el-icon class="mr-1"><Upload /></el-icon>
          开始导入
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.import-dialog :deep(.el-dialog__body) {
  padding: 16px 24px;
}

.upload-area :deep(.el-upload-dragger) {
  border-color: #e5e7eb;
  border-radius: 8px;
}

.upload-area :deep(.el-upload-dragger:hover) {
  border-color: #3b82f6;
}

.result-section {
  min-height: 300px;
}
</style>
