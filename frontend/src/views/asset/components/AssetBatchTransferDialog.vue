<script setup lang="ts">
/**
 * 批量调拨对话框 - 将多件资产一次性调拨到新位置
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Close, Position, Check, Warning } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { assetApi } from '@/api/v2/asset'
import type { Asset, BatchTransferResult } from '@/types/v2/asset'
import LocationSelector from './LocationSelector.vue'
import UserSelector from '@/components/common/UserSelector.vue'

const props = defineProps<{
  visible: boolean
  assets: Asset[]
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: [result: BatchTransferResult]
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const formRef = ref<FormInstance>()
const loading = ref(false)
const userSelectorVisible = ref(false)
const showResult = ref(false)
const transferResult = ref<BatchTransferResult | null>(null)

const formData = ref({
  locationType: '',
  locationId: undefined as number | undefined,
  locationName: '',
  responsibleUserId: undefined as number | undefined,
  responsibleUserName: '',
  remark: ''
})

const rules: FormRules = {
  locationType: [{ required: true, message: '请选择目标位置类型', trigger: 'change' }],
  locationId: [{ required: true, message: '请选择目标位置', trigger: 'change' }]
}

// 处理用户选择
interface SelectedUser {
  id: string | number
  realName: string
  orgUnitName?: string
}

function handleUserSelect(users: SelectedUser[]) {
  if (users.length > 0) {
    const user = users[0]
    formData.value.responsibleUserId = Number(user.id)
    formData.value.responsibleUserName = user.realName
  }
  userSelectorVisible.value = false
}

// 清除责任人
function clearResponsible() {
  formData.value.responsibleUserId = undefined
  formData.value.responsibleUserName = ''
}

// 监听 visible 变化，初始化表单
watch(() => props.visible, (val) => {
  if (val) {
    showResult.value = false
    transferResult.value = null
    formData.value = {
      locationType: '',
      locationId: undefined,
      locationName: '',
      responsibleUserId: undefined,
      responsibleUserName: '',
      remark: ''
    }
    formRef.value?.clearValidate()
  }
})

async function handleSubmit() {
  try {
    const valid = await formRef.value?.validate()
    if (!valid) return

    loading.value = true

    const result = await assetApi.batchTransferAssets({
      assetIds: props.assets.map(a => a.id),
      locationType: formData.value.locationType,
      locationId: formData.value.locationId!,
      locationName: formData.value.locationName,
      responsibleUserId: formData.value.responsibleUserId,
      responsibleUserName: formData.value.responsibleUserName,
      remark: formData.value.remark
    })

    transferResult.value = result
    showResult.value = true

    if (result.failedCount === 0) {
      ElMessage.success(`成功调拨 ${result.successCount} 件资产`)
    } else {
      ElMessage.warning(`调拨完成: ${result.successCount} 成功, ${result.failedCount} 失败`)
    }
  } catch (error) {
    console.error('Batch transfer failed:', error)
    ElMessage.error('批量调拨失败')
  } finally {
    loading.value = false
  }
}

function handleFinish() {
  if (transferResult.value) {
    emit('saved', transferResult.value)
  }
  dialogVisible.value = false
}

function handleClose() {
  dialogVisible.value = false
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="批量调拨"
    width="640px"
    :close-on-click-modal="false"
    class="batch-transfer-dialog"
  >
    <!-- 调拨结果展示 -->
    <div v-if="showResult && transferResult" class="result-section">
      <div class="text-center py-6">
        <div
          class="w-16 h-16 mx-auto mb-4 rounded-full flex items-center justify-center"
          :class="transferResult.failedCount === 0 ? 'bg-green-100' : 'bg-amber-100'"
        >
          <el-icon
            class="text-3xl"
            :class="transferResult.failedCount === 0 ? 'text-green-600' : 'text-amber-600'"
          >
            <Check v-if="transferResult.failedCount === 0" />
            <Warning v-else />
          </el-icon>
        </div>
        <h3 class="text-lg font-semibold text-gray-900 mb-2">
          {{ transferResult.failedCount === 0 ? '调拨成功' : '调拨部分完成' }}
        </h3>
        <p class="text-gray-500 mb-4">
          已将 <span class="font-semibold text-green-600">{{ transferResult.successCount }}</span> 件资产
          调拨至 <span class="font-semibold">{{ transferResult.targetLocationName }}</span>
        </p>

        <!-- 统计信息 -->
        <div class="flex justify-center gap-8 py-4 bg-gray-50 rounded-lg">
          <div class="text-center">
            <div class="text-2xl font-bold text-gray-900">{{ transferResult.totalCount }}</div>
            <div class="text-sm text-gray-500">总数</div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-bold text-green-600">{{ transferResult.successCount }}</div>
            <div class="text-sm text-gray-500">成功</div>
          </div>
          <div v-if="transferResult.failedCount > 0" class="text-center">
            <div class="text-2xl font-bold text-red-600">{{ transferResult.failedCount }}</div>
            <div class="text-sm text-gray-500">失败</div>
          </div>
        </div>

        <!-- 失败列表 -->
        <div v-if="transferResult.failedAssets.length > 0" class="mt-4 text-left">
          <div class="text-sm font-medium text-gray-700 mb-2">失败详情:</div>
          <div class="max-h-40 overflow-y-auto border border-red-200 rounded-lg">
            <div
              v-for="item in transferResult.failedAssets"
              :key="item.assetId"
              class="px-3 py-2 border-b border-red-100 last:border-b-0 text-sm"
            >
              <span class="font-medium">{{ item.assetCode || item.assetId }}</span>
              <span v-if="item.assetName" class="text-gray-500 mx-1">{{ item.assetName }}</span>
              <span class="text-red-600">- {{ item.reason }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 调拨表单 -->
    <template v-else>
      <!-- 选中资产预览 -->
      <div class="mb-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
        <div class="flex items-center justify-between mb-2">
          <span class="font-medium text-blue-900">已选择 {{ assets.length }} 件资产</span>
        </div>
        <div class="flex flex-wrap gap-2 max-h-24 overflow-y-auto">
          <span
            v-for="asset in assets.slice(0, 10)"
            :key="asset.id"
            class="px-2 py-1 bg-white rounded text-sm text-gray-700 border border-blue-100"
          >
            {{ asset.assetCode }}
          </span>
          <span v-if="assets.length > 10" class="px-2 py-1 text-sm text-blue-600">
            +{{ assets.length - 10 }} 更多
          </span>
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-position="top"
        class="batch-transfer-form"
      >
        <!-- 目标位置选择 -->
        <div class="mb-4">
          <div class="text-sm font-medium text-gray-700 mb-2">
            <el-icon class="mr-1"><Position /></el-icon>
            目标位置
          </div>
          <LocationSelector
            v-model:location-type="formData.locationType"
            v-model:location-id="formData.locationId"
            v-model:location-name="formData.locationName"
          />
        </div>

        <el-divider />

        <!-- 新责任人选择 (可选) -->
        <el-form-item label="新责任人 (可选)">
          <div class="flex gap-2 w-full">
            <div
              class="flex-1 flex items-center gap-3 px-4 py-3 border border-gray-200 rounded-lg cursor-pointer hover:border-blue-400 hover:bg-blue-50/50 transition-all"
              @click="userSelectorVisible = true"
            >
              <div class="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center">
                <el-icon v-if="!formData.responsibleUserId" class="text-gray-400 text-lg"><User /></el-icon>
                <span v-else class="font-medium text-blue-600">
                  {{ formData.responsibleUserName?.charAt(0) }}
                </span>
              </div>
              <div class="flex-1 min-w-0">
                <div v-if="formData.responsibleUserId" class="font-medium text-gray-900 truncate">
                  {{ formData.responsibleUserName }}
                </div>
                <div v-else class="text-gray-400">点击选择新责任人（不选则保持原责任人）</div>
              </div>
              <el-icon class="text-gray-400"><User /></el-icon>
            </div>
            <el-button
              v-if="formData.responsibleUserId"
              text
              type="danger"
              class="h-auto"
              @click.stop="clearResponsible"
            >
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </el-form-item>

        <!-- 备注 -->
        <el-form-item label="调拨原因/备注">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="2"
            placeholder="可选，填写调拨原因"
            maxlength="500"
          />
        </el-form-item>
      </el-form>
    </template>

    <template #footer>
      <div v-if="showResult" class="flex justify-center">
        <el-button type="primary" @click="handleFinish">完成</el-button>
      </div>
      <div v-else class="flex justify-end gap-2">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          <el-icon class="mr-1"><Position /></el-icon>
          确认调拨 ({{ assets.length }} 件)
        </el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 用户选择器弹窗 -->
  <el-dialog
    v-model="userSelectorVisible"
    title="选择新责任人"
    width="800px"
    append-to-body
    :close-on-click-modal="false"
  >
    <UserSelector
      :model-value="formData.responsibleUserId?.toString() || null"
      :multiple="false"
      @change="handleUserSelect"
    />
    <template #footer>
      <el-button @click="userSelectorVisible = false">取消</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.batch-transfer-dialog :deep(.el-dialog__body) {
  padding: 16px 24px 8px;
}

.batch-transfer-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
  padding-bottom: 4px;
}

.result-section {
  min-height: 280px;
}
</style>
