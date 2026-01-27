<script setup lang="ts">
/**
 * 完成维修对话框
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Tools, Money } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { assetApi } from '@/api/asset'
import type { AssetMaintenance, CompleteMaintenanceRequest } from '@/types/asset'
import { MaintenanceType } from '@/types/asset'

const props = defineProps<{
  visible: boolean
  maintenance: AssetMaintenance | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  completed: []
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const formRef = ref<FormInstance>()
const loading = ref(false)
const showSuccess = ref(false)

const formData = ref<CompleteMaintenanceRequest>({
  result: '',
  cost: undefined,
  maintainer: ''
})

const rules: FormRules = {
  result: [{ required: true, message: '请填写维修结果', trigger: 'blur' }]
}

// 监听对话框打开
watch(() => props.visible, (val) => {
  if (val && props.maintenance) {
    showSuccess.value = false
    formData.value = {
      result: '',
      cost: undefined,
      maintainer: props.maintenance.maintainer || ''
    }
    formRef.value?.clearValidate()
  }
})

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid || !props.maintenance) return

  try {
    loading.value = true
    await assetApi.completeMaintenance(props.maintenance.id, formData.value)

    ElMessage.success('维修已完成')
    showSuccess.value = true
  } catch (error) {
    console.error('Complete maintenance failed:', error)
    ElMessage.error('操作失败')
  } finally {
    loading.value = false
  }
}

function handleFinish() {
  emit('completed')
  dialogVisible.value = false
}

function handleClose() {
  dialogVisible.value = false
}

// 是否是维修类型
const isRepair = computed(() =>
  props.maintenance?.maintenanceType === MaintenanceType.REPAIR
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="showSuccess ? '维修已完成' : '完成维修/保养'"
    width="480px"
    :close-on-click-modal="false"
    class="complete-dialog"
  >
    <!-- 成功提示 -->
    <div v-if="showSuccess" class="text-center py-8">
      <div class="w-20 h-20 mx-auto mb-4 rounded-full bg-green-100 flex items-center justify-center">
        <el-icon class="text-4xl text-green-600"><Check /></el-icon>
      </div>
      <h3 class="text-xl font-semibold text-gray-900 mb-2">维修已完成</h3>
      <p class="text-gray-500">
        {{ maintenance?.assetName }} 已恢复正常使用
      </p>
    </div>

    <!-- 表单 -->
    <el-form
      v-else
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      class="complete-form"
    >
      <!-- 维修信息卡片 -->
      <div v-if="maintenance" class="mb-5 p-4 bg-gray-50 rounded-lg">
        <div class="flex items-center gap-3 mb-3">
          <div class="w-10 h-10 rounded-lg bg-amber-100 flex items-center justify-center">
            <el-icon class="text-amber-600"><Tools /></el-icon>
          </div>
          <div class="flex-1">
            <div class="font-medium text-gray-900">{{ maintenance.assetName }}</div>
            <div class="text-sm text-gray-500">{{ maintenance.assetCode }}</div>
          </div>
          <span class="px-2 py-1 bg-amber-100 text-amber-700 text-xs rounded">
            {{ maintenance.maintenanceTypeDesc }}
          </span>
        </div>
        <div class="pt-3 border-t border-gray-200 text-sm">
          <div class="text-gray-500 mb-1">{{ isRepair ? '故障描述' : '保养内容' }}：</div>
          <div class="text-gray-700">{{ maintenance.faultDesc || '-' }}</div>
        </div>
        <div class="mt-2 text-xs text-gray-400">
          开始时间：{{ maintenance.startDate }}
        </div>
      </div>

      <!-- 维修结果 -->
      <el-form-item :label="isRepair ? '维修结果' : '保养结果'" prop="result">
        <el-input
          v-model="formData.result"
          type="textarea"
          :rows="3"
          :placeholder="isRepair ? '请填写维修结果，如：更换零件、修复故障等' : '请填写保养结果'"
          maxlength="500"
        />
      </el-form-item>

      <!-- 维修费用 -->
      <el-form-item label="费用">
        <el-input-number
          v-model="formData.cost"
          :min="0"
          :precision="2"
          placeholder="可选"
          style="width: 100%"
          controls-position="right"
        >
          <template #prefix>
            <el-icon class="text-gray-400"><Money /></el-icon>
          </template>
        </el-input-number>
      </el-form-item>

      <!-- 维修人员 -->
      <el-form-item label="维修/保养人员">
        <el-input
          v-model="formData.maintainer"
          placeholder="可选，填写或修改维修人员信息"
          maxlength="100"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div v-if="showSuccess" class="flex justify-center">
        <el-button type="primary" @click="handleFinish">完成</el-button>
      </div>
      <div v-else class="flex justify-end gap-2">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          <el-icon class="mr-1"><Check /></el-icon>
          确认完成
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.complete-dialog :deep(.el-dialog__body) {
  padding: 16px 24px;
}

.complete-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
  padding-bottom: 4px;
}
</style>
