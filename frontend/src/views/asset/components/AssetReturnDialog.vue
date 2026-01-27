<script setup lang="ts">
/**
 * 资产归还对话框
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Box, Check, Warning, CircleClose } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { assetBorrowApi } from '@/api/v2/assetBorrow'
import type { AssetBorrow, ReturnBorrowRequest } from '@/types/v2/asset'
import { ReturnCondition, ReturnConditionMap } from '@/types/v2/asset'

const props = defineProps<{
  visible: boolean
  borrow: AssetBorrow | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  returned: []
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const formRef = ref<FormInstance>()
const loading = ref(false)
const showSuccess = ref(false)

const formData = ref<ReturnBorrowRequest>({
  returnCondition: ReturnCondition.GOOD,
  returnRemark: ''
})

const rules: FormRules = {
  returnCondition: [{ required: true, message: '请选择归还状况', trigger: 'change' }]
}

// 归还状况选项
const conditionOptions = [
  { value: ReturnCondition.GOOD, label: '完好', icon: Check, color: 'green', desc: '资产完好无损' },
  { value: ReturnCondition.DAMAGED, label: '损坏', icon: Warning, color: 'amber', desc: '资产有损坏' },
  { value: ReturnCondition.LOST, label: '丢失', icon: CircleClose, color: 'red', desc: '资产已丢失' }
]

// 监听对话框打开
watch(() => props.visible, (val) => {
  if (val) {
    showSuccess.value = false
    formData.value = {
      returnCondition: ReturnCondition.GOOD,
      returnRemark: ''
    }
    formRef.value?.clearValidate()
  }
})

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid || !props.borrow) return

  try {
    loading.value = true
    await assetBorrowApi.returnBorrow(props.borrow.id, formData.value)

    ElMessage.success('归还登记成功')
    showSuccess.value = true
  } catch (error) {
    console.error('Return borrow failed:', error)
    ElMessage.error('归还登记失败')
  } finally {
    loading.value = false
  }
}

function handleFinish() {
  emit('returned')
  dialogVisible.value = false
}

function handleClose() {
  dialogVisible.value = false
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="showSuccess ? '归还成功' : '资产归还'"
    width="480px"
    :close-on-click-modal="false"
    class="return-dialog"
  >
    <!-- 成功提示 -->
    <div v-if="showSuccess" class="text-center py-8">
      <div class="w-20 h-20 mx-auto mb-4 rounded-full bg-green-100 flex items-center justify-center">
        <el-icon class="text-4xl text-green-600"><Check /></el-icon>
      </div>
      <h3 class="text-xl font-semibold text-gray-900 mb-2">归还登记成功</h3>
      <p class="text-gray-500">
        {{ borrow?.assetName }} 已归还
      </p>
    </div>

    <!-- 归还表单 -->
    <el-form
      v-else
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      class="return-form"
    >
      <!-- 借用信息卡片 -->
      <div v-if="borrow" class="mb-5 p-4 bg-gray-50 rounded-lg">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-lg bg-blue-100 flex items-center justify-center">
            <el-icon class="text-xl text-blue-600"><Box /></el-icon>
          </div>
          <div class="flex-1">
            <div class="font-medium text-gray-900">{{ borrow.assetName }}</div>
            <div class="text-sm text-gray-500 mt-0.5">
              {{ borrow.assetCode }}
            </div>
          </div>
        </div>
        <div class="mt-3 pt-3 border-t border-gray-200 grid grid-cols-2 gap-2 text-sm">
          <div>
            <span class="text-gray-500">借用人：</span>
            <span class="text-gray-900">{{ borrow.borrowerName }}</span>
          </div>
          <div>
            <span class="text-gray-500">借出日期：</span>
            <span class="text-gray-900">{{ borrow.borrowDate?.slice(0, 10) }}</span>
          </div>
          <div v-if="borrow.expectedReturnDate">
            <span class="text-gray-500">预计归还：</span>
            <span :class="borrow.overdue ? 'text-red-600 font-medium' : 'text-gray-900'">
              {{ borrow.expectedReturnDate }}
              <template v-if="borrow.overdue">（已逾期{{ borrow.overdueDays }}天）</template>
            </span>
          </div>
        </div>
      </div>

      <!-- 归还状况 -->
      <el-form-item label="归还状况" prop="returnCondition">
        <div class="flex gap-3 w-full">
          <div
            v-for="option in conditionOptions"
            :key="option.value"
            :class="[
              'flex-1 p-3 border rounded-lg cursor-pointer transition-all text-center',
              formData.returnCondition === option.value
                ? `border-${option.color}-500 bg-${option.color}-50`
                : 'border-gray-200 hover:border-gray-300'
            ]"
            @click="formData.returnCondition = option.value"
          >
            <el-icon
              :class="[
                'text-2xl mb-1',
                formData.returnCondition === option.value
                  ? `text-${option.color}-600`
                  : 'text-gray-400'
              ]"
            >
              <component :is="option.icon" />
            </el-icon>
            <div class="font-medium text-gray-900">{{ option.label }}</div>
            <div class="text-xs text-gray-500 mt-0.5">{{ option.desc }}</div>
          </div>
        </div>
      </el-form-item>

      <!-- 归还备注 -->
      <el-form-item label="归还备注">
        <el-input
          v-model="formData.returnRemark"
          type="textarea"
          :rows="3"
          placeholder="可选，填写归还相关备注信息"
          maxlength="500"
        />
      </el-form-item>

      <!-- 损坏/丢失提示 -->
      <div
        v-if="formData.returnCondition !== ReturnCondition.GOOD"
        class="p-3 rounded-lg"
        :class="formData.returnCondition === ReturnCondition.LOST ? 'bg-red-50' : 'bg-amber-50'"
      >
        <div class="flex items-start gap-2">
          <el-icon
            class="mt-0.5"
            :class="formData.returnCondition === ReturnCondition.LOST ? 'text-red-600' : 'text-amber-600'"
          >
            <Warning />
          </el-icon>
          <div
            class="text-sm"
            :class="formData.returnCondition === ReturnCondition.LOST ? 'text-red-700' : 'text-amber-700'"
          >
            <template v-if="formData.returnCondition === ReturnCondition.DAMAGED">
              请在备注中详细说明损坏情况，以便后续维修处理。
            </template>
            <template v-else>
              资产丢失将记录在案，请在备注中说明丢失原因。
            </template>
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
          确认归还
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.return-dialog :deep(.el-dialog__body) {
  padding: 16px 24px;
}

.return-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
  padding-bottom: 4px;
}

/* 动态颜色类 */
.border-green-500 { border-color: #22c55e; }
.bg-green-50 { background-color: #f0fdf4; }
.text-green-600 { color: #16a34a; }

.border-amber-500 { border-color: #f59e0b; }
.bg-amber-50 { background-color: #fffbeb; }
.text-amber-600 { color: #d97706; }

.border-red-500 { border-color: #ef4444; }
.bg-red-50 { background-color: #fef2f2; }
.text-red-600 { color: #dc2626; }
</style>
