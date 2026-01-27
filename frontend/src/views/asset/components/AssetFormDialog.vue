<script setup lang="ts">
/**
 * 资产表单对话框 - 简洁现代设计
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Close, Calendar, Coin, Box, Tag } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { assetApi } from '@/api/asset'
import type {
  Asset,
  AssetCategory,
  CreateAssetRequest,
  UpdateAssetRequest
} from '@/types/asset'
import { ManagementMode, ManagementModeMap } from '@/types/asset'
import LocationSelector from './LocationSelector.vue'
import UserSelector from '@/components/common/UserSelector.vue'

const props = defineProps<{
  visible: boolean
  asset: Asset | null
  categories: AssetCategory[]
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const isEdit = computed(() => !!props.asset)
const title = computed(() => isEdit.value ? '编辑资产' : '新建资产')

const formRef = ref<FormInstance>()
const loading = ref(false)
const userSelectorVisible = ref(false)
const activeSection = ref('basic') // basic | finance | location

const formData = ref<CreateAssetRequest>({
  assetName: '',
  categoryId: 0,
  brand: '',
  model: '',
  unit: '',
  quantity: 1,
  managementMode: undefined,
  originalValue: undefined,
  netValue: undefined,
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

// 当前选中分类
const selectedCategory = computed(() => {
  if (!formData.value.categoryId) return null
  const findCategory = (items: AssetCategory[]): AssetCategory | null => {
    for (const item of items) {
      if (item.id === formData.value.categoryId) return item
      if (item.children?.length) {
        const found = findCategory(item.children)
        if (found) return found
      }
    }
    return null
  }
  return findCategory(props.categories)
})

// 是否为批量管理模式
const isBatchMode = computed(() => {
  return formData.value.managementMode === ManagementMode.BATCH
})

// 管理模式选项
const managementModeOptions = computed(() => {
  return Object.entries(ManagementModeMap).map(([key, label]) => ({
    value: Number(key),
    label
  }))
})

const rules: FormRules = {
  assetName: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择资产分类', trigger: 'change' }],
  unit: [{ required: true, message: '请输入计量单位', trigger: 'blur' }]
}

// 展开分类选项
const flatCategories = computed(() => {
  const result: { id: number; label: string; level: number }[] = []

  const flatten = (items: AssetCategory[], level = 0) => {
    items.forEach(item => {
      result.push({
        id: item.id,
        label: '　'.repeat(level) + item.categoryName,
        level
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
    if (props.asset) {
      // 编辑模式
      formData.value = {
        assetName: props.asset.assetName,
        categoryId: props.asset.categoryId,
        brand: props.asset.brand || '',
        model: props.asset.model || '',
        unit: props.asset.unit,
        quantity: props.asset.quantity,
        managementMode: props.asset.managementMode ?? ManagementMode.SINGLE_ITEM,
        originalValue: props.asset.originalValue ?? undefined,
        netValue: props.asset.netValue ?? undefined,
        purchaseDate: props.asset.purchaseDate || '',
        warrantyDate: props.asset.warrantyDate || '',
        supplier: props.asset.supplier || '',
        locationType: props.asset.locationType || '',
        locationId: props.asset.locationId ?? undefined,
        locationName: props.asset.locationName || '',
        responsibleUserId: props.asset.responsibleUserId ?? undefined,
        responsibleUserName: props.asset.responsibleUserName || '',
        remark: props.asset.remark || ''
      }
    } else {
      // 新建模式
      formData.value = {
        assetName: '',
        categoryId: 0,
        brand: '',
        model: '',
        unit: '',
        quantity: 1,
        managementMode: undefined,
        originalValue: undefined,
        netValue: undefined,
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
    }
    formRef.value?.clearValidate()
  }
})

// 分类变化时自动填充单位和管理模式
watch(() => formData.value.categoryId, (categoryId) => {
  if (categoryId && !isEdit.value) {
    const category = selectedCategory.value
    if (category) {
      // 自动填充单位
      if (category.unit) {
        formData.value.unit = category.unit
      }
      // 自动设置管理模式
      if (category.defaultManagementMode) {
        formData.value.managementMode = category.defaultManagementMode
        // 单品管理模式下数量固定为1
        if (category.defaultManagementMode === ManagementMode.SINGLE_ITEM) {
          formData.value.quantity = 1
        }
      }
    }
  }
})

// 管理模式变化时调整数量
watch(() => formData.value.managementMode, (mode) => {
  if (mode === ManagementMode.SINGLE_ITEM) {
    formData.value.quantity = 1
  }
})

async function handleSubmit() {
  try {
    const valid = await formRef.value?.validate()
    if (!valid) return

    loading.value = true

    if (isEdit.value && props.asset) {
      // 更新
      const updateData: UpdateAssetRequest = {
        assetName: formData.value.assetName,
        categoryId: formData.value.categoryId,
        brand: formData.value.brand || undefined,
        model: formData.value.model || undefined,
        unit: formData.value.unit,
        quantity: formData.value.quantity,
        originalValue: formData.value.originalValue,
        netValue: formData.value.netValue,
        purchaseDate: formData.value.purchaseDate || undefined,
        warrantyDate: formData.value.warrantyDate || undefined,
        supplier: formData.value.supplier || undefined,
        responsibleUserId: formData.value.responsibleUserId,
        responsibleUserName: formData.value.responsibleUserName || undefined,
        remark: formData.value.remark || undefined
      }
      await assetApi.updateAsset(props.asset.id, updateData)
      ElMessage.success('更新成功')
    } else {
      // 创建
      await assetApi.createAsset(formData.value)
      ElMessage.success('创建成功')
    }

    emit('saved')
    dialogVisible.value = false
  } catch (error) {
    console.error('Save failed:', error)
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
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
    :title="title"
    width="680px"
    :close-on-click-modal="false"
    class="asset-form-dialog"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      class="asset-form"
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
        <div class="grid grid-cols-2 gap-x-4">
          <el-form-item label="资产名称" prop="assetName" class="col-span-2">
            <el-input
              v-model="formData.assetName"
              placeholder="请输入资产名称"
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

          <el-form-item label="计量单位" prop="unit">
            <el-input v-model="formData.unit" placeholder="如：台、个、套" />
          </el-form-item>

          <el-form-item label="管理模式">
            <el-select
              v-model="formData.managementMode"
              placeholder="根据分类自动选择"
              class="w-full"
              :disabled="isEdit"
            >
              <el-option
                v-for="item in managementModeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <div class="text-xs text-gray-400 mt-1">
              <template v-if="formData.managementMode === 1">
                单品管理：每件资产有唯一编码，适用于电脑、投影仪等
              </template>
              <template v-else-if="formData.managementMode === 2">
                批量管理：同类资产共用一条记录，适用于文具、耗材等
              </template>
              <template v-else>
                选择分类后自动设置管理模式
              </template>
            </div>
          </el-form-item>

          <el-form-item label="数量">
            <el-input-number
              v-model="formData.quantity"
              :min="1"
              :max="isBatchMode ? 99999 : 1"
              :disabled="!isBatchMode"
              class="w-full"
            />
            <div v-if="!isBatchMode" class="text-xs text-gray-400 mt-1">
              单品管理模式下数量固定为1
            </div>
          </el-form-item>

          <el-form-item label="品牌">
            <el-input v-model="formData.brand" placeholder="请输入品牌" />
          </el-form-item>

          <el-form-item label="型号规格">
            <el-input v-model="formData.model" placeholder="请输入型号规格" />
          </el-form-item>

          <el-form-item label="供应商">
            <el-input v-model="formData.supplier" placeholder="请输入供应商名称" />
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
          <el-form-item label="原值 (元)">
            <el-input-number
              v-model="formData.originalValue"
              :precision="2"
              :min="0"
              :max="999999999"
              placeholder="资产原始价值"
              class="w-full"
            />
          </el-form-item>

          <el-form-item label="净值 (元)">
            <el-input-number
              v-model="formData.netValue"
              :precision="2"
              :min="0"
              :max="999999999"
              placeholder="当前净值"
              class="w-full"
            />
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

        <div class="mt-4 p-4 bg-blue-50 rounded-lg">
          <div class="flex items-center gap-2 text-blue-700">
            <el-icon><Coin /></el-icon>
            <span class="font-medium">财务提示</span>
          </div>
          <p class="mt-2 text-sm text-blue-600">
            原值为资产购入时的价格，净值为扣除折旧后的当前价值。
            保修期内资产可享受免费维修服务。
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
      <div class="flex justify-between items-center">
        <div class="text-sm text-gray-400">
          <span v-if="activeSection === 'basic'">1 / 3</span>
          <span v-else-if="activeSection === 'finance'">2 / 3</span>
          <span v-else>3 / 3</span>
        </div>
        <div class="flex gap-2">
          <el-button @click="handleClose">取消</el-button>
          <el-button type="primary" :loading="loading" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '创建资产' }}
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
.asset-form-dialog :deep(.el-dialog__body) {
  padding: 16px 24px 8px;
}

.asset-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
  padding-bottom: 4px;
}

.asset-form :deep(.el-input__wrapper),
.asset-form :deep(.el-select__wrapper) {
  box-shadow: 0 0 0 1px #e5e7eb inset;
}

.asset-form :deep(.el-input__wrapper:hover),
.asset-form :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px #3b82f6 inset;
}

.form-section {
  min-height: 280px;
}
</style>
