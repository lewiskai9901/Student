<script setup lang="ts">
/**
 * 批量入库对话框 - 一次性入库多件相同规格的资产
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Close, Box, Coin, DocumentAdd, Check } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { assetApi } from '@/api/v2/asset'
import type { AssetCategory, BatchCreateAssetRequest, BatchCreateResult, CreateAssetRequest } from '@/types/v2/asset'
import { ManagementMode } from '@/types/v2/asset'
import LocationSelector from './LocationSelector.vue'
import UserSelector from '@/components/common/UserSelector.vue'

const props = defineProps<{
  visible: boolean
  categories: AssetCategory[]
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: [result: BatchCreateResult]
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const formRef = ref<FormInstance>()
const loading = ref(false)
const userSelectorVisible = ref(false)
const activeSection = ref('basic') // basic | finance | location
const showResult = ref(false)
const createResult = ref<BatchCreateResult | null>(null)

// 入库模式: individual=单品入库(每件独立编码), batch=批量入库(一条记录多数量)
const createMode = ref<'individual' | 'batch'>('individual')

const formData = ref<BatchCreateAssetRequest>({
  assetName: '',
  categoryId: 0,
  brand: '',
  model: '',
  unit: '',
  quantity: 1,
  originalValue: undefined,
  purchaseDate: '',
  warrantyDate: '',
  supplier: '',
  locationType: '',
  locationId: undefined,
  locationName: '',
  responsibleUserId: undefined,
  responsibleUserName: '',
  remark: ''
})

const rules = computed<FormRules>(() => ({
  assetName: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择资产分类', trigger: 'change' }],
  unit: [{ required: true, message: '请输入计量单位', trigger: 'blur' }],
  quantity: [
    { required: true, message: '请输入入库数量', trigger: 'blur' },
    {
      type: 'number',
      min: 1,
      max: createMode.value === 'batch' ? 99999 : 1000,
      message: createMode.value === 'batch' ? '入库数量在1-99999之间' : '入库数量在1-1000之间',
      trigger: 'blur'
    }
  ]
}))

// 计算总价值
const totalValue = computed(() => {
  if (formData.value.originalValue && formData.value.quantity) {
    return formData.value.originalValue * formData.value.quantity
  }
  return 0
})

// 展开分类选项
const flatCategories = computed(() => {
  const result: { id: number; label: string; level: number; code: string }[] = []

  const flatten = (items: AssetCategory[], level = 0) => {
    items.forEach(item => {
      result.push({
        id: item.id,
        label: '　'.repeat(level) + item.categoryName,
        level,
        code: item.categoryCode
      })
      if (item.children?.length) {
        flatten(item.children, level + 1)
      }
    })
  }

  flatten(props.categories)
  return result
})

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
    activeSection.value = 'basic'
    showResult.value = false
    createResult.value = null
    createMode.value = 'individual'
    formData.value = {
      assetName: '',
      categoryId: 0,
      brand: '',
      model: '',
      unit: '',
      quantity: 1,
      originalValue: undefined,
      purchaseDate: '',
      warrantyDate: '',
      supplier: '',
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

// 分类变化时自动填充单位
watch(() => formData.value.categoryId, (categoryId) => {
  if (categoryId) {
    const findCategory = (items: AssetCategory[]): AssetCategory | null => {
      for (const item of items) {
        if (item.id === categoryId) return item
        if (item.children?.length) {
          const found = findCategory(item.children)
          if (found) return found
        }
      }
      return null
    }

    const category = findCategory(props.categories)
    if (category?.unit) {
      formData.value.unit = category.unit
    }
  }
})

async function handleSubmit() {
  try {
    const valid = await formRef.value?.validate()
    if (!valid) return

    loading.value = true

    if (createMode.value === 'batch') {
      // 批量管理模式：创建一条记录，数量为N
      const createData: CreateAssetRequest = {
        assetName: formData.value.assetName,
        categoryId: formData.value.categoryId,
        brand: formData.value.brand || undefined,
        model: formData.value.model || undefined,
        unit: formData.value.unit,
        quantity: formData.value.quantity,
        managementMode: ManagementMode.BATCH,
        originalValue: formData.value.originalValue,
        purchaseDate: formData.value.purchaseDate || undefined,
        warrantyDate: formData.value.warrantyDate || undefined,
        supplier: formData.value.supplier || undefined,
        locationType: formData.value.locationType || undefined,
        locationId: formData.value.locationId,
        locationName: formData.value.locationName || undefined,
        responsibleUserId: formData.value.responsibleUserId,
        responsibleUserName: formData.value.responsibleUserName || undefined,
        remark: formData.value.remark || undefined
      }
      await assetApi.createAsset(createData)

      // 构造结果对象
      createResult.value = {
        totalCount: formData.value.quantity,
        successCount: formData.value.quantity,
        firstAssetCode: '-',
        lastAssetCode: '-',
        assetIds: [],
        totalValue: formData.value.originalValue
          ? formData.value.originalValue * formData.value.quantity
          : undefined
      }
      showResult.value = true
      ElMessage.success(`成功入库 ${formData.value.quantity} 件资产（批量管理）`)
    } else {
      // 单品管理模式：创建N条记录，每条数量为1
      const result = await assetApi.batchCreateAssets(formData.value)
      createResult.value = result
      showResult.value = true
      ElMessage.success(`成功入库 ${result.successCount} 件资产（单品管理）`)
    }
  } catch (error) {
    console.error('Batch create failed:', error)
    ElMessage.error('入库失败')
  } finally {
    loading.value = false
  }
}

function handleFinish() {
  if (createResult.value) {
    emit('saved', createResult.value)
  }
  dialogVisible.value = false
}

function handleClose() {
  dialogVisible.value = false
}

function continueBatchCreate() {
  showResult.value = false
  createResult.value = null
  // 保留分类和位置信息，清空其他
  formData.value.assetName = ''
  formData.value.brand = ''
  formData.value.model = ''
  formData.value.quantity = 1
  formData.value.originalValue = undefined
  formData.value.remark = ''
  activeSection.value = 'basic'
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="批量入库"
    width="720px"
    :close-on-click-modal="false"
    class="batch-create-dialog"
  >
    <!-- 入库结果展示 -->
    <div v-if="showResult && createResult" class="result-section">
      <div class="text-center py-8">
        <div class="w-20 h-20 mx-auto mb-4 rounded-full bg-green-100 flex items-center justify-center">
          <el-icon class="text-4xl text-green-600"><Check /></el-icon>
        </div>
        <h3 class="text-xl font-semibold text-gray-900 mb-2">入库成功</h3>
        <p class="text-gray-500 mb-6">
          已成功入库 <span class="font-semibold text-green-600">{{ createResult.successCount }}</span> 件资产
          <el-tag :type="createMode === 'batch' ? 'warning' : 'info'" size="small" class="ml-2">
            {{ createMode === 'batch' ? '批量管理' : '单品管理' }}
          </el-tag>
        </p>

        <div class="bg-gray-50 rounded-lg p-4 text-left max-w-md mx-auto">
          <div class="grid grid-cols-2 gap-4 text-sm">
            <div v-if="createMode === 'individual'">
              <span class="text-gray-500">资产编号范围</span>
              <p class="font-medium text-gray-900 mt-1">
                {{ createResult.firstAssetCode }}<br/>
                ~ {{ createResult.lastAssetCode }}
              </p>
            </div>
            <div v-else>
              <span class="text-gray-500">管理模式</span>
              <p class="font-medium text-amber-600 mt-1">批量管理（共用一条记录）</p>
            </div>
            <div>
              <span class="text-gray-500">入库总数</span>
              <p class="font-medium text-gray-900 mt-1">{{ createResult.totalCount }} 件</p>
            </div>
            <div v-if="createResult.totalValue">
              <span class="text-gray-500">总价值</span>
              <p class="font-medium text-gray-900 mt-1">{{ createResult.totalValue.toLocaleString() }} 元</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 入库表单 -->
    <el-form
      v-else
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      class="batch-form"
    >
      <!-- 分段导航 -->
      <div class="flex gap-1 mb-5 p-1 bg-gray-100 rounded-lg">
        <button
          type="button"
          :class="[
            'flex-1 px-4 py-2 text-sm font-medium rounded-md transition-all',
            activeSection === 'basic'
              ? 'bg-white text-blue-600 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          ]"
          @click="activeSection = 'basic'"
        >
          <el-icon class="mr-1"><Box /></el-icon>
          基本信息
        </button>
        <button
          type="button"
          :class="[
            'flex-1 px-4 py-2 text-sm font-medium rounded-md transition-all',
            activeSection === 'finance'
              ? 'bg-white text-blue-600 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          ]"
          @click="activeSection = 'finance'"
        >
          <el-icon class="mr-1"><Coin /></el-icon>
          财务信息
        </button>
        <button
          type="button"
          :class="[
            'flex-1 px-4 py-2 text-sm font-medium rounded-md transition-all',
            activeSection === 'location'
              ? 'bg-white text-blue-600 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          ]"
          @click="activeSection = 'location'"
        >
          <el-icon class="mr-1"><User /></el-icon>
          位置与责任人
        </button>
      </div>

      <!-- 基本信息 -->
      <div v-show="activeSection === 'basic'" class="form-section">
        <!-- 入库模式选择 -->
        <div class="mb-4">
          <div class="text-sm font-medium text-gray-700 mb-2">入库模式</div>
          <div class="grid grid-cols-2 gap-3">
            <div
              :class="[
                'p-3 border rounded-lg cursor-pointer transition-all',
                createMode === 'individual'
                  ? 'border-blue-500 bg-blue-50'
                  : 'border-gray-200 hover:border-gray-300'
              ]"
              @click="createMode = 'individual'"
            >
              <div class="flex items-center gap-2 mb-1">
                <div
                  :class="[
                    'w-4 h-4 rounded-full border-2 flex items-center justify-center',
                    createMode === 'individual' ? 'border-blue-500' : 'border-gray-300'
                  ]"
                >
                  <div v-if="createMode === 'individual'" class="w-2 h-2 rounded-full bg-blue-500" />
                </div>
                <span class="font-medium text-gray-900">单品入库</span>
              </div>
              <p class="text-xs text-gray-500 ml-6">
                创建N条记录，每件有唯一编码<br/>
                适用于：电脑、投影仪等需独立追踪的设备
              </p>
            </div>
            <div
              :class="[
                'p-3 border rounded-lg cursor-pointer transition-all',
                createMode === 'batch'
                  ? 'border-blue-500 bg-blue-50'
                  : 'border-gray-200 hover:border-gray-300'
              ]"
              @click="createMode = 'batch'"
            >
              <div class="flex items-center gap-2 mb-1">
                <div
                  :class="[
                    'w-4 h-4 rounded-full border-2 flex items-center justify-center',
                    createMode === 'batch' ? 'border-blue-500' : 'border-gray-300'
                  ]"
                >
                  <div v-if="createMode === 'batch'" class="w-2 h-2 rounded-full bg-blue-500" />
                </div>
                <span class="font-medium text-gray-900">批量入库</span>
              </div>
              <p class="text-xs text-gray-500 ml-6">
                创建1条记录，数量为N<br/>
                适用于：书桌、椅子、文具等批量物品
              </p>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-x-4">
          <el-form-item label="资产名称" prop="assetName" class="col-span-2">
            <el-input
              v-model="formData.assetName"
              placeholder="请输入资产名称，如：学生课桌"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="资产分类" prop="categoryId">
            <el-select
              v-model="formData.categoryId"
              placeholder="请选择分类"
              filterable
              class="w-full"
            >
              <el-option
                v-for="item in flatCategories"
                :key="item.id"
                :label="item.label"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="入库数量" prop="quantity">
            <el-input-number
              v-model="formData.quantity"
              :min="1"
              :max="1000"
              class="w-full"
            />
          </el-form-item>

          <el-form-item label="计量单位" prop="unit">
            <el-input v-model="formData.unit" placeholder="如：台、个、套" />
          </el-form-item>

          <el-form-item label="供应商">
            <el-input v-model="formData.supplier" placeholder="请输入供应商名称" />
          </el-form-item>

          <el-form-item label="品牌">
            <el-input v-model="formData.brand" placeholder="请输入品牌" />
          </el-form-item>

          <el-form-item label="型号规格">
            <el-input v-model="formData.model" placeholder="请输入型号规格" />
          </el-form-item>
        </div>

        <el-form-item label="备注" class="mt-2">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="2"
            placeholder="可选，填写资产相关备注信息"
            maxlength="500"
          />
        </el-form-item>
      </div>

      <!-- 财务信息 -->
      <div v-show="activeSection === 'finance'" class="form-section">
        <div class="grid grid-cols-2 gap-x-4">
          <el-form-item label="单价 (元)">
            <el-input-number
              v-model="formData.originalValue"
              :precision="2"
              :min="0"
              :max="999999999"
              placeholder="每件资产的单价"
              class="w-full"
            />
          </el-form-item>

          <el-form-item label="总价值">
            <div class="h-10 flex items-center px-3 bg-gray-100 rounded-lg text-lg font-semibold text-blue-600">
              {{ totalValue.toLocaleString() }} 元
            </div>
          </el-form-item>

          <el-form-item label="购置日期">
            <el-date-picker
              v-model="formData.purchaseDate"
              type="date"
              placeholder="选择购置日期"
              value-format="YYYY-MM-DD"
              class="w-full"
            />
          </el-form-item>

          <el-form-item label="保修截止">
            <el-date-picker
              v-model="formData.warrantyDate"
              type="date"
              placeholder="选择保修截止日期"
              value-format="YYYY-MM-DD"
              class="w-full"
            />
          </el-form-item>
        </div>

        <div class="mt-4 p-4 bg-amber-50 rounded-lg">
          <div class="flex items-center gap-2 text-amber-700">
            <el-icon><Coin /></el-icon>
            <span class="font-medium">价值计算</span>
          </div>
          <p class="mt-2 text-sm text-amber-600">
            入库 <strong>{{ formData.quantity || 0 }}</strong> 件资产，
            单价 <strong>{{ (formData.originalValue || 0).toLocaleString() }}</strong> 元/件，
            总价值 <strong class="text-lg">{{ totalValue.toLocaleString() }}</strong> 元
          </p>
        </div>
      </div>

      <!-- 位置与责任人 -->
      <div v-show="activeSection === 'location'" class="form-section">
        <!-- 位置选择器 -->
        <LocationSelector
          v-model:location-type="formData.locationType"
          v-model:location-id="formData.locationId"
          v-model:location-name="formData.locationName"
        />

        <el-divider />

        <!-- 责任人选择 -->
        <el-form-item label="责任人">
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
                <div v-else class="text-gray-400">点击选择责任人</div>
                <div class="text-xs text-gray-400 mt-0.5">负责资产的日常管理与维护</div>
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
      </div>
    </el-form>

    <template #footer>
      <div v-if="showResult" class="flex justify-center gap-4">
        <el-button @click="continueBatchCreate">
          <el-icon class="mr-1"><DocumentAdd /></el-icon>
          继续入库
        </el-button>
        <el-button type="primary" @click="handleFinish">
          完成
        </el-button>
      </div>
      <div v-else class="flex justify-between items-center">
        <div class="text-sm text-gray-400">
          <span v-if="activeSection === 'basic'">1 / 3</span>
          <span v-else-if="activeSection === 'finance'">2 / 3</span>
          <span v-else>3 / 3</span>
        </div>
        <div class="flex gap-2">
          <el-button @click="handleClose">取消</el-button>
          <el-button type="primary" :loading="loading" @click="handleSubmit">
            <el-icon class="mr-1"><DocumentAdd /></el-icon>
            确认入库 ({{ formData.quantity }} 件)
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>

  <!-- 用户选择器弹窗 -->
  <el-dialog
    v-model="userSelectorVisible"
    title="选择责任人"
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
.batch-create-dialog :deep(.el-dialog__body) {
  padding: 16px 24px 8px;
}

.batch-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
  padding-bottom: 4px;
}

.batch-form :deep(.el-input__wrapper),
.batch-form :deep(.el-select__wrapper) {
  box-shadow: 0 0 0 1px #e5e7eb inset;
}

.batch-form :deep(.el-input__wrapper:hover),
.batch-form :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px #3b82f6 inset;
}

.form-section {
  min-height: 320px;
}

.result-section {
  min-height: 320px;
}
</style>
