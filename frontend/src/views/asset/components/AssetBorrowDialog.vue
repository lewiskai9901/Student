<script setup lang="ts">
/**
 * 资产借用/领用对话框
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Box, User, Calendar, Document, Check } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { assetBorrowApi } from '@/api/assetBorrow'
import type { Asset, CreateBorrowRequest } from '@/types/asset'
import { BorrowType, BorrowTypeMap, ManagementMode, ManagementModeMap } from '@/types/asset'
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
const showSuccess = ref(false)
const borrowNo = ref('')

const formData = ref<CreateBorrowRequest>({
  borrowType: BorrowType.BORROW,
  assetId: 0,
  quantity: 1,
  borrowerId: 0,
  borrowerName: '',
  borrowerDept: '',
  borrowerPhone: '',
  expectedReturnDate: '',
  purpose: ''
})

// 长期借用（无需归还日期）
const isLongTermBorrow = ref(false)

const rules = computed<FormRules>(() => ({
  borrowType: [{ required: true, message: '请选择借用类型', trigger: 'change' }],
  borrowerId: [{ required: true, message: '请选择借用人', trigger: 'change' }],
  quantity: isBatchManagement.value
    ? [
        { required: true, message: '请输入借用数量', trigger: 'blur' },
        {
          validator: (_rule: any, value: number, callback: (error?: Error) => void) => {
            if (value > availableQuantity.value) {
              callback(new Error(`库存不足，最大可借用 ${availableQuantity.value}`))
            } else if (value < 1) {
              callback(new Error('借用数量至少为1'))
            } else {
              callback()
            }
          },
          trigger: 'change'
        }
      ]
    : [],
  expectedReturnDate: formData.value.borrowType === BorrowType.BORROW && !isLongTermBorrow.value
    ? [{ required: true, message: '请选择预计归还日期', trigger: 'change' }]
    : [],
  purpose: [{ required: true, message: '请填写借用原因', trigger: 'blur' }]
}))

// 计算最小归还日期（明天）
const minReturnDate = computed(() => {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  return tomorrow.toISOString().split('T')[0]
})

// 判断是否是批量管理模式
const isBatchManagement = computed(() => {
  return props.asset?.managementMode === ManagementMode.BATCH
})

// 可用库存数量
const availableQuantity = computed(() => {
  if (!props.asset) return 0
  // 批量模式时使用 availableQuantity，单品模式数量为1
  return isBatchManagement.value
    ? (props.asset.availableQuantity ?? props.asset.quantity ?? 1)
    : 1
})

// 最大借用数量
const maxBorrowQuantity = computed(() => {
  return availableQuantity.value
})

// 监听对话框打开
watch(() => props.visible, (val) => {
  if (val && props.asset) {
    showSuccess.value = false
    borrowNo.value = ''
    isLongTermBorrow.value = false
    formData.value = {
      borrowType: BorrowType.BORROW,
      assetId: props.asset.id,
      quantity: 1,
      borrowerId: 0,
      borrowerName: '',
      borrowerDept: '',
      borrowerPhone: '',
      expectedReturnDate: '',
      purpose: ''
    }
    formRef.value?.clearValidate()
  }
})

// 监听长期借用切换，清空归还日期
watch(isLongTermBorrow, (val) => {
  if (val) {
    formData.value.expectedReturnDate = ''
  }
  formRef.value?.clearValidate('expectedReturnDate')
})

// 用户选择
interface SelectedUser {
  id: string | number
  realName: string
  orgUnitName?: string
  phone?: string
}

function handleUserSelect(users: SelectedUser[]) {
  if (users.length > 0) {
    const user = users[0]
    formData.value.borrowerId = user.id
    formData.value.borrowerName = user.realName
    formData.value.borrowerDept = user.orgUnitName || ''
  }
  userSelectorVisible.value = false
}

function clearBorrower() {
  formData.value.borrowerId = 0
  formData.value.borrowerName = ''
  formData.value.borrowerDept = ''
  formData.value.borrowerPhone = ''
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return

  // 批量模式下检查库存
  if (isBatchManagement.value && formData.value.quantity > availableQuantity.value) {
    ElMessage.error(`库存不足，当前可用库存为 ${availableQuantity.value}`)
    return
  }

  try {
    loading.value = true
    await assetBorrowApi.createBorrow(formData.value)

    ElMessage.success(formData.value.borrowType === BorrowType.USE ? '领用登记成功' : '借用登记成功')
    showSuccess.value = true
  } catch (error: any) {
    console.error('Create borrow failed:', error)
    const message = error?.response?.data?.message || '借用登记失败'
    ElMessage.error(message)
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
    :title="showSuccess ? '借用登记成功' : '资产借用'"
    width="560px"
    :close-on-click-modal="false"
    class="borrow-dialog"
  >
    <!-- 成功提示 -->
    <div v-if="showSuccess" class="text-center py-8">
      <div class="w-20 h-20 mx-auto mb-4 rounded-full bg-green-100 flex items-center justify-center">
        <el-icon class="text-4xl text-green-600"><Check /></el-icon>
      </div>
      <h3 class="text-xl font-semibold text-gray-900 mb-2">借用登记成功</h3>
      <p class="text-gray-500 mb-4">
        {{ BorrowTypeMap[formData.borrowType as BorrowType] }}申请已提交
      </p>
      <div class="bg-gray-50 rounded-lg p-4 text-left max-w-sm mx-auto">
        <div class="grid grid-cols-2 gap-3 text-sm">
          <div>
            <span class="text-gray-500">资产名称</span>
            <p class="font-medium text-gray-900">{{ asset?.assetName }}</p>
          </div>
          <div>
            <span class="text-gray-500">借用人</span>
            <p class="font-medium text-gray-900">{{ formData.borrowerName }}</p>
          </div>
          <div v-if="formData.borrowType === BorrowType.BORROW">
            <span class="text-gray-500">预计归还</span>
            <p class="font-medium text-gray-900">
              {{ isLongTermBorrow ? '长期借用' : formData.expectedReturnDate }}
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- 借用表单 -->
    <el-form
      v-else
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      class="borrow-form"
    >
      <!-- 资产信息卡片 -->
      <div class="mb-5 p-4 bg-gray-50 rounded-lg">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-lg bg-blue-100 flex items-center justify-center">
            <el-icon class="text-xl text-blue-600"><Box /></el-icon>
          </div>
          <div class="flex-1">
            <div class="font-medium text-gray-900">{{ asset?.assetName }}</div>
            <div class="text-sm text-gray-500 mt-0.5">
              {{ asset?.assetCode }} · {{ asset?.categoryName || '未分类' }}
            </div>
          </div>
          <!-- 管理模式标签 -->
          <el-tag
            :type="isBatchManagement ? 'warning' : 'info'"
            size="small"
          >
            {{ ManagementModeMap[asset?.managementMode as ManagementMode] || '单品管理' }}
          </el-tag>
        </div>
        <!-- 批量管理时显示库存信息 -->
        <div v-if="isBatchManagement" class="mt-3 pt-3 border-t border-gray-200">
          <div class="flex items-center justify-between text-sm">
            <span class="text-gray-500">可用库存</span>
            <span :class="availableQuantity > 0 ? 'text-green-600 font-medium' : 'text-red-500 font-medium'">
              {{ availableQuantity }} {{ asset?.unit || '个' }}
            </span>
          </div>
          <div v-if="availableQuantity === 0" class="mt-2 text-xs text-red-500">
            库存不足，暂时无法借用
          </div>
        </div>
      </div>

      <!-- 借用类型 -->
      <el-form-item label="借用类型" prop="borrowType">
        <div class="flex gap-3 w-full">
          <div
            v-for="(label, value) in BorrowTypeMap"
            :key="value"
            :class="[
              'flex-1 p-4 border rounded-lg cursor-pointer transition-all',
              formData.borrowType === Number(value)
                ? 'border-blue-500 bg-blue-50'
                : 'border-gray-200 hover:border-gray-300'
            ]"
            @click="formData.borrowType = Number(value) as BorrowType"
          >
            <div class="flex items-center gap-2 mb-1">
              <div
                :class="[
                  'w-4 h-4 rounded-full border-2 flex items-center justify-center',
                  formData.borrowType === Number(value)
                    ? 'border-blue-500'
                    : 'border-gray-300'
                ]"
              >
                <div
                  v-if="formData.borrowType === Number(value)"
                  class="w-2 h-2 rounded-full bg-blue-500"
                />
              </div>
              <span class="font-medium text-gray-900">{{ label }}</span>
            </div>
            <p class="text-xs text-gray-500 ml-6">
              {{ Number(value) === BorrowType.USE ? '适用于消耗品，无需归还' : '适用于设备借用，需归还' }}
            </p>
          </div>
        </div>
      </el-form-item>

      <!-- 借用人 -->
      <el-form-item label="借用人" prop="borrowerId">
        <div
          :class="[
            'w-full flex items-center gap-3 px-4 py-3 border rounded-lg cursor-pointer transition-all',
            formData.borrowerId
              ? 'border-blue-400 bg-blue-50/50'
              : 'border-gray-200 hover:border-blue-400'
          ]"
          @click="userSelectorVisible = true"
        >
          <div class="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center">
            <el-icon v-if="!formData.borrowerId" class="text-gray-400 text-lg"><User /></el-icon>
            <span v-else class="font-medium text-blue-600">
              {{ formData.borrowerName?.charAt(0) }}
            </span>
          </div>
          <div class="flex-1 min-w-0">
            <div v-if="formData.borrowerId" class="font-medium text-gray-900">
              {{ formData.borrowerName }}
            </div>
            <div v-else class="text-gray-400">点击选择借用人</div>
            <div v-if="formData.borrowerDept" class="text-xs text-gray-500 mt-0.5">
              {{ formData.borrowerDept }}
            </div>
          </div>
          <el-icon class="text-gray-400"><User /></el-icon>
        </div>
      </el-form-item>

      <!-- 借用数量（批量管理时显示） -->
      <el-form-item v-if="isBatchManagement" label="借用数量" prop="quantity">
        <el-input-number
          v-model="formData.quantity"
          :min="1"
          :max="maxBorrowQuantity"
          :disabled="availableQuantity === 0"
          style="width: 100%"
        />
        <div class="text-xs text-gray-400 mt-1">
          最大可借用 {{ maxBorrowQuantity }} {{ asset?.unit || '个' }}
        </div>
      </el-form-item>

      <!-- 预计归还日期（仅借用时显示） -->
      <el-form-item
        v-if="formData.borrowType === BorrowType.BORROW"
        label="预计归还日期"
        prop="expectedReturnDate"
      >
        <!-- 长期借用选项 -->
        <div class="mb-3 flex items-center justify-between">
          <div class="flex items-center gap-2">
            <el-switch
              v-model="isLongTermBorrow"
              size="small"
            />
            <span class="text-sm text-gray-600">长期借用</span>
          </div>
          <span v-if="isLongTermBorrow" class="text-xs text-gray-400">无需指定归还日期</span>
        </div>
        <!-- 日期选择器 -->
        <el-date-picker
          v-if="!isLongTermBorrow"
          v-model="formData.expectedReturnDate"
          type="date"
          placeholder="选择预计归还日期"
          value-format="YYYY-MM-DD"
          :disabled-date="(date: Date) => date < new Date()"
          class="w-full"
        >
          <template #prefix>
            <el-icon><Calendar /></el-icon>
          </template>
        </el-date-picker>
        <div v-else class="p-3 bg-amber-50 border border-amber-200 rounded-lg text-sm text-amber-700">
          长期借用适用于办公设备等长期使用场景，归还时间由借用人自行决定
        </div>
      </el-form-item>

      <!-- 借用原因 -->
      <el-form-item label="借用原因" prop="purpose">
        <el-input
          v-model="formData.purpose"
          type="textarea"
          :rows="3"
          placeholder="请填写借用原因或用途"
          maxlength="500"
          show-word-limit
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
          <el-icon class="mr-1"><Document /></el-icon>
          确认{{ BorrowTypeMap[formData.borrowType as BorrowType] }}
        </el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 用户选择器弹窗 -->
  <el-dialog
    v-model="userSelectorVisible"
    title="选择借用人"
    width="800px"
    append-to-body
    :close-on-click-modal="false"
  >
    <UserSelector
      :model-value="formData.borrowerId?.toString() || null"
      :multiple="false"
      @change="handleUserSelect"
    />
    <template #footer>
      <el-button @click="userSelectorVisible = false">取消</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.borrow-dialog :deep(.el-dialog__body) {
  padding: 16px 24px;
}

.borrow-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
  padding-bottom: 4px;
}
</style>
