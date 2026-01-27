<script setup lang="ts">
/**
 * 资产标签打印对话框
 * 显示资产的二维码和条形码，支持打印
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Printer, Download } from '@element-plus/icons-vue'
import { assetCodeApi, type LabelData } from '@/api/assetCode'

// Props
const props = defineProps<{
  modelValue: boolean
  assets: Array<{
    id: number
    assetCode: string
    assetName: string
    location?: string
  }>
}>()

// Emits
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

// State
const loading = ref(false)
const labels = ref<LabelData[]>([])
const labelSize = ref<'small' | 'medium' | 'large'>('medium')

// Computed
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const labelSizeConfig = computed(() => {
  switch (labelSize.value) {
    case 'small':
      return { width: 180, qrSize: 80, fontSize: 10 }
    case 'large':
      return { width: 300, qrSize: 150, fontSize: 14 }
    default:
      return { width: 240, qrSize: 120, fontSize: 12 }
  }
})

// Watch
watch(
  () => props.modelValue,
  async (visible) => {
    if (visible && props.assets.length > 0) {
      await loadLabels()
    }
  }
)

// Methods
async function loadLabels() {
  if (props.assets.length === 0) return

  loading.value = true
  try {
    const requests = props.assets.map((asset) => ({
      assetId: asset.id,
      assetCode: asset.assetCode,
      assetName: asset.assetName,
      location: asset.location
    }))

    labels.value = await assetCodeApi.generateLabels(requests)
  } catch (error) {
    console.error('Failed to load labels:', error)
    ElMessage.error('加载标签失败')
  } finally {
    loading.value = false
  }
}

function handlePrint() {
  const printContent = document.getElementById('print-area')
  if (!printContent) return

  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    ElMessage.error('无法打开打印窗口，请检查浏览器设置')
    return
  }

  const config = labelSizeConfig.value
  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>资产标签打印</title>
      <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: Arial, sans-serif; }
        .label-grid {
          display: flex;
          flex-wrap: wrap;
          gap: 10px;
          padding: 10px;
        }
        .label-item {
          width: ${config.width}px;
          border: 1px solid #333;
          padding: 10px;
          page-break-inside: avoid;
        }
        .label-header {
          text-align: center;
          font-weight: bold;
          font-size: ${config.fontSize + 2}px;
          margin-bottom: 8px;
          border-bottom: 1px solid #ddd;
          padding-bottom: 5px;
        }
        .label-codes {
          display: flex;
          justify-content: center;
          gap: 10px;
          margin: 10px 0;
        }
        .label-codes img {
          max-width: ${config.qrSize}px;
          max-height: ${config.qrSize}px;
        }
        .label-info {
          font-size: ${config.fontSize}px;
        }
        .label-info p {
          margin: 3px 0;
          word-break: break-all;
        }
        .label-code-text {
          text-align: center;
          font-family: monospace;
          font-size: ${config.fontSize + 1}px;
          font-weight: bold;
          margin-top: 5px;
        }
        @media print {
          body { -webkit-print-color-adjust: exact; }
        }
      </style>
    </head>
    <body>
      ${printContent.innerHTML}
    </body>
    </html>
  `)
  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => {
    printWindow.print()
    printWindow.close()
  }, 250)
}

function downloadLabel(label: LabelData) {
  // 创建下载链接
  const link = document.createElement('a')
  link.href = label.qrcode
  link.download = `${label.assetCode}.png`
  link.click()
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="资产标签"
    width="800px"
    :close-on-click-modal="false"
  >
    <!-- 工具栏 -->
    <div class="mb-4 flex items-center justify-between">
      <div class="flex items-center gap-4">
        <span class="text-sm text-gray-500">标签尺寸：</span>
        <el-radio-group v-model="labelSize" size="small">
          <el-radio-button value="small">小</el-radio-button>
          <el-radio-button value="medium">中</el-radio-button>
          <el-radio-button value="large">大</el-radio-button>
        </el-radio-group>
      </div>
      <div class="flex items-center gap-2">
        <el-button :icon="Printer" type="primary" @click="handlePrint">
          打印全部
        </el-button>
      </div>
    </div>

    <!-- 标签预览 -->
    <div v-loading="loading" class="label-preview">
      <div id="print-area" class="label-grid">
        <div
          v-for="label in labels"
          :key="label.assetCode"
          class="label-item"
          :style="{ width: labelSizeConfig.width + 'px' }"
        >
          <div class="label-header">资产标签</div>
          <div class="label-codes">
            <img :src="label.qrcode" alt="QR Code" />
          </div>
          <div class="label-code-text">{{ label.assetCode }}</div>
          <div class="label-info">
            <p><strong>名称：</strong>{{ label.assetName }}</p>
            <p v-if="label.location"><strong>位置：</strong>{{ label.location }}</p>
          </div>
          <div class="label-actions">
            <el-button
              text
              size="small"
              :icon="Download"
              @click="downloadLabel(label)"
            >
              下载二维码
            </el-button>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && labels.length === 0" description="暂无标签数据" />
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.label-preview {
  max-height: 500px;
  overflow-y: auto;
}

.label-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding: 10px;
}

.label-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
  background: white;
}

.label-header {
  text-align: center;
  font-weight: bold;
  font-size: 14px;
  color: #303133;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.label-codes {
  display: flex;
  justify-content: center;
  margin: 12px 0;
}

.label-codes img {
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.label-code-text {
  text-align: center;
  font-family: monospace;
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin: 8px 0;
}

.label-info {
  font-size: 12px;
  color: #606266;
}

.label-info p {
  margin: 4px 0;
  word-break: break-all;
}

.label-actions {
  margin-top: 8px;
  text-align: center;
  border-top: 1px solid #ebeef5;
  padding-top: 8px;
}
</style>
