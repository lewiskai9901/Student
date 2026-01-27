<script setup lang="ts">
/**
 * 资产维修/保养对话框
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Box, Tools, Setting, Check } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { assetApi } from '@/api/asset'
import type { Asset, CreateMaintenanceRequest } from '@/types/asset'
import { MaintenanceType, MaintenanceTypeMap } from '@/types/asset'

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
const showSuccess = ref(false)

const formData = ref<CreateMaintenanceRequest>({
  maintenanceType: MaintenanceType.REPAIR,
  faultDesc: '',
  maintainer: ''
})

const rules: FormRules = {
  maintenanceType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  faultDesc: [{ required: true, message: '请填写故障描述或保养内容', trigger: 'blur' }]
}

// 维修类型选项
const typeOptions = [
  {
    value: MaintenanceType.REPAIR,
    label: '维修',
    icon: Tools,
    color: 'amber',
    desc: '设备发生故障，需要维修'
  },
  {
    value: MaintenanceType.MAINTENANCE,
    label: '保养',
    icon: Setting,
    color: 'blue',
    desc: '定期保养维护，预防性维护'
  }
]

// 监听对话框打开
watch(() => props.visible, (val) => {
  if (val) {
    showSuccess.value = false
    formData.value = {
      maintenanceType: MaintenanceType.REPAIR,
      faultDesc: '',
      maintainer: ''
    }
    formRef.value?.clearValidate()
  }
})

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid || !props.asset) return

  try {
    loading.value = true
    await assetApi.createMaintenance(props.asset.id, formData.value)

    ElMessage.success('维修记录创建成功')
    showSuccess.value = true
  } catch (error) {
    console.error('Create maintenance failed:', error)
    ElMessage.error('创建失败')
  } finally {
    loading.value = false
  }
}

function handleFinish() {
  emit('saved')
  dialogVisible.value = false
}

function handleClose() {
  dialogVisible.value = false
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="showSuccess ? '维修记录已创建' : '创建维修/保养记录'"
    width="520px"
    :close-on-click-modal="false"
    class="maintenance-dialog"
  >
    <!-- 成功提示 -->
    <div v-if="showSuccess" class="text-center py-8">
      <div class="w-20 h-20 mx-auto mb-4 rounded-full bg-green-100 flex items-center justify-center">
        <el-icon class="text-4xl text-green-600"><Check /></el-icon>
      </div>
      <h3 class="text-xl font-semibold text-gray-900 mb-2">
        {{ formData.maintenanceType === MaintenanceType.REPAIR ? '维修记录' : '保养记录' }}已创建
      </h3>
      <p class="text-gray-500">
        {{ asset?.assetName }} 已进入{{ formData.maintenanceType === MaintenanceType.REPAIR ? '维修' : '保养' }}状态
      </p>
    </div>

    <!-- 表单 -->
    <el-form
      v-else
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      class="maintenance-form"
    >
      <!-- 资产信息卡片 -->
      <div v-if="asset" class="mb-5 p-4 bg-gray-50 rounded-lg">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-lg bg-blue-100 flex items-center justify-center">
            <el-icon class="text-xl text-blue-600"><Box /></el-icon>
          </div>
          <div>
            <div class="font-medium text-gray-900">{{ asset.assetName }}</div>
            <div class="text-sm text-gray-500 mt-0.5">
              {{ asset.assetCode }} · {{ asset.categoryName || '未分类' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 维修类型 -->
      <el-form-item label="类型" prop="maintenanceType">
        <div class="flex gap-3 w-full">
          <div
            v-for="option in typeOptions"
            :key="option.value"
            :class="[
              'flex-1 p-4 border rounded-lg cursor-pointer transition-all',
              formData.maintenanceType === option.value
                ? `border-${option.color}-500 bg-${option.color}-50`
                : 'border-gray-200 hover:border-gray-300'
            ]"
            @click="formData.maintenanceType = option.value"
          >
            <div class="flex items-center gap-2 mb-1">
              <el-icon
                :class="[
                  'text-xl',
                  formData.maintenanceType === option.value
                    ? `text-${option.color}-600`
                    : 'text-gray-400'
                ]"
              >
                <component :is="option.icon" />
              </el-icon>
              <span class="font-medium text-gray-900">{{ option.label }}</span>
            </div>
            <p class="text-xs text-gray-500">{{ option.desc }}</p>
          </div>
        </div>
      </el-form-item>

      <!-- 故障描述/保养内容 -->
      <el-form-item
        :label="formData.maintenanceType === MaintenanceType.REPAIR ? '故障描述' : '保养内容'"
        prop="faultDesc"
      >
        <el-input
          v-model="formData.faultDesc"
          type="textarea"
          :rows="4"
          :placeholder="formData.maintenanceType === MaintenanceType.REPAIR
            ? '请详细描述设备故障现象...'
            : '请填写保养内容...'"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>

      <!-- 维修人员 -->
      <el-form-item label="维修/保养人员">
        <el-input
          v-model="formData.maintainer"
          placeholder="可选，填写维修人员或维修公司"
          maxlength="100"
        />
      </el-form-item>

      <!-- 提示信息 -->
      <div
        v-if="formData.maintenanceType === MaintenanceType.REPAIR"
        class="p-3 rounded-lg bg-amber-50"
      >
        <div class="flex items-start gap-2">
          <el-icon class="mt-0.5 text-amber-600"><Tools /></el-icon>
          <div class="text-sm text-amber-700">
            创建维修记录后，该资产状态将变为"维修中"，维修完成后请及时更新记录。
          </div>
        </div>
      </div>
    </el-form>

    <template #footer>
      <div v-if="showSuccess" class="flex justify-center">
        <el-button type="primary" @click="handleFinish">完成</el-button>
      </div>
      <div v-else class="flex justify-end gap-2">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          <el-icon class="mr-1"><Tools /></el-icon>
          创建{{ MaintenanceTypeMap[formData.maintenanceType as MaintenanceType] }}记录
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.maintenance-dialog :deep(.el-dialog__body) {
  padding: 16px 24px;
}

.maintenance-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
  padding-bottom: 4px;
}

/* 动态颜色类 */
.border-amber-500 { border-color: #f59e0b; }
.bg-amber-50 { background-color: #fffbeb; }
.text-amber-600 { color: #d97706; }

.border-blue-500 { border-color: #3b82f6; }
.bg-blue-50 { background-color: #eff6ff; }
.text-blue-600 { color: #2563eb; }
</style>
