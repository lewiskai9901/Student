<script setup lang="ts">
/**
 * 资产调拨对话框 - 简洁设计
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Close, Location, Right } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { assetApi } from '@/api/v2/asset'
import type { Asset, TransferAssetRequest } from '@/types/v2/asset'
import LocationSelector from './LocationSelector.vue'
import UserSelector from '@/components/common/UserSelector.vue'

const props = defineProps<{
  visible: boolean
  asset: Asset | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const formRef = ref<FormInstance>()
const loading = ref(false)
const userSelectorVisible = ref(false)

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

const formData = ref<TransferAssetRequest>({
  locationType: '',
  locationId: 0,
  locationName: '',
  responsibleUserId: undefined,
  responsibleUserName: '',
  remark: ''
})

const rules: FormRules = {
  locationName: [{ required: true, message: '请选择或输入新位置', trigger: 'blur' }]
}

// 监听 visible 变化
watch(() => props.visible, (val) => {
  if (val) {
    formData.value = {
      locationType: '',
      locationId: 0,
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

    if (!props.asset) return

    loading.value = true

    await assetApi.transferAsset(props.asset.id, {
      locationType: formData.value.locationType,
      locationId: formData.value.locationId || 0,
      locationName: formData.value.locationName,
      responsibleUserId: formData.value.responsibleUserId,
      responsibleUserName: formData.value.responsibleUserName || undefined,
      remark: formData.value.remark || undefined
    })

    ElMessage.success('调拨成功')
    emit('saved')
    dialogVisible.value = false
  } catch (error) {
    console.error('Transfer failed:', error)
    ElMessage.error('调拨失败')
  } finally {
    loading.value = false
  }
}

function handleClose() {
  dialogVisible.value = false
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="资产调拨"
    width="580px"
    :close-on-click-modal="false"
    class="transfer-dialog"
  >
    <!-- 当前资产信息卡片 -->
    <div v-if="asset" class="mb-5 p-4 bg-gradient-to-r from-blue-50 to-indigo-50 rounded-xl border border-blue-100">
      <div class="flex items-center gap-3">
        <div class="w-12 h-12 rounded-lg bg-white shadow-sm flex items-center justify-center">
          <span class="text-xl">📦</span>
        </div>
        <div class="flex-1 min-w-0">
          <div class="font-semibold text-gray-900 truncate">{{ asset.assetName }}</div>
          <div class="flex items-center gap-3 mt-1 text-sm text-gray-500">
            <span class="font-mono">{{ asset.assetCode }}</span>
            <span class="text-gray-300">|</span>
            <span class="flex items-center gap-1">
              <el-icon><Location /></el-icon>
              {{ asset.locationName || '未分配位置' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 调拨方向提示 -->
    <div class="flex items-center justify-center gap-3 mb-5 text-gray-400">
      <div class="text-center">
        <div class="text-sm">当前位置</div>
        <div class="text-xs mt-1">{{ asset?.locationName || '未知' }}</div>
      </div>
      <el-icon class="text-2xl text-blue-400"><Right /></el-icon>
      <div class="text-center">
        <div class="text-sm">新位置</div>
        <div class="text-xs mt-1 text-blue-600">{{ formData.locationName || '待选择' }}</div>
      </div>
    </div>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      class="transfer-form"
    >
      <!-- 位置选择器 -->
      <LocationSelector
        v-model:location-type="formData.locationType"
        v-model:location-id="formData.locationId"
        v-model:location-name="formData.locationName"
      />

      <el-divider />

      <!-- 责任人选择 -->
      <el-form-item label="新责任人（可选）">
        <div class="flex gap-2 w-full">
          <div
            class="flex-1 flex items-center gap-3 px-4 py-3 border border-gray-200 rounded-lg cursor-pointer hover:border-blue-400 hover:bg-blue-50/50 transition-all"
            @click="userSelectorVisible = true"
          >
            <div class="w-9 h-9 rounded-full bg-gray-100 flex items-center justify-center">
              <el-icon v-if="!formData.responsibleUserId" class="text-gray-400"><User /></el-icon>
              <span v-else class="font-medium text-blue-600 text-sm">
                {{ formData.responsibleUserName?.charAt(0) }}
              </span>
            </div>
            <div class="flex-1 min-w-0">
              <div v-if="formData.responsibleUserId" class="font-medium text-gray-900 truncate">
                {{ formData.responsibleUserName }}
              </div>
              <div v-else class="text-gray-400 text-sm">点击选择新责任人</div>
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

      <el-form-item label="调拨备注">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="2"
          placeholder="请输入调拨原因或备注（可选）"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        确认调拨
      </el-button>
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
.transfer-dialog :deep(.el-dialog__body) {
  padding: 16px 24px;
}

.transfer-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
}
</style>
