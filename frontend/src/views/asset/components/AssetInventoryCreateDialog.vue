<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createInventory } from '@/api/assetInventory'
import { getCategoryTree } from '@/api/asset'
import type { CreateInventoryRequest, AssetCategory } from '@/types/asset'
import { InventoryScopeType, InventoryScopeTypeMap } from '@/types/asset'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', visible: boolean): void
  (e: 'success'): void
}>()

// 表单
const formRef = ref<FormInstance>()
const loading = ref(false)
const categoryList = ref<AssetCategory[]>([])

const formData = reactive<CreateInventoryRequest & { scopeType: string; categoryId?: number }>({
  inventoryName: '',
  scopeType: InventoryScopeType.ALL,
  scopeValue: '',
  categoryId: undefined,
  startDate: '',
  endDate: ''
})

// 表单校验规则
const rules: FormRules = {
  inventoryName: [
    { required: true, message: '请输入盘点名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度在2-50个字符', trigger: 'blur' }
  ],
  scopeType: [
    { required: true, message: '请选择盘点范围', trigger: 'change' }
  ],
  startDate: [
    { required: true, message: '请选择开始日期', trigger: 'change' }
  ],
  endDate: [
    { required: true, message: '请选择截止日期', trigger: 'change' }
  ]
}

// 范围类型选项
const scopeOptions = [
  { value: InventoryScopeType.ALL, label: '全部资产' },
  { value: InventoryScopeType.CATEGORY, label: '按分类' },
  { value: InventoryScopeType.LOCATION, label: '按位置' }
]

// 位置类型选项
const locationOptions = [
  { value: 'classroom', label: '教室' },
  { value: 'dormitory', label: '宿舍' },
  { value: 'office', label: '办公室' },
  { value: 'warehouse', label: '仓库' }
]

// 计算属性：是否显示分类选择
const showCategorySelect = computed(() => formData.scopeType === InventoryScopeType.CATEGORY)

// 计算属性：是否显示位置选择
const showLocationSelect = computed(() => formData.scopeType === InventoryScopeType.LOCATION)

// 监听弹窗打开
watch(() => props.visible, async (visible) => {
  if (visible) {
    resetForm()
    await loadCategories()
  }
})

// 加载分类列表
async function loadCategories() {
  try {
    const res = await getCategoryTree()
    categoryList.value = res || []
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

// 重置表单
function resetForm() {
  formData.inventoryName = ''
  formData.scopeType = InventoryScopeType.ALL
  formData.scopeValue = ''
  formData.categoryId = undefined
  formData.startDate = ''
  formData.endDate = ''
  formRef.value?.clearValidate()
}

// 关闭弹窗
function handleClose() {
  emit('update:visible', false)
}

// 提交
async function handleSubmit() {
  if (!formRef.value) return

  await formRef.value.validate()

  // 处理 scopeValue
  let scopeValue = ''
  if (formData.scopeType === InventoryScopeType.CATEGORY && formData.categoryId) {
    scopeValue = formData.categoryId.toString()
  } else if (formData.scopeType === InventoryScopeType.LOCATION) {
    scopeValue = formData.scopeValue
  }

  loading.value = true
  try {
    await createInventory({
      inventoryName: formData.inventoryName,
      scopeType: formData.scopeType,
      scopeValue: scopeValue,
      startDate: formData.startDate,
      endDate: formData.endDate
    })
    emit('success')
  } catch (error: any) {
    ElMessage.error(error.message || '创建失败')
  } finally {
    loading.value = false
  }
}

// 快捷日期选择
function setDateRange(days: number) {
  const today = new Date()
  const end = new Date(today)
  end.setDate(end.getDate() + days)

  formData.startDate = formatDate(today)
  formData.endDate = formatDate(end)
}

function formatDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="新建盘点任务"
    width="550px"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
      label-position="right"
    >
      <el-form-item label="盘点名称" prop="inventoryName">
        <el-input
          v-model="formData.inventoryName"
          placeholder="例如：2026年1月固定资产盘点"
          maxlength="50"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="盘点范围" prop="scopeType">
        <el-radio-group v-model="formData.scopeType">
          <el-radio-button
            v-for="opt in scopeOptions"
            :key="opt.value"
            :value="opt.value"
          >
            {{ opt.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- 按分类时选择分类 -->
      <el-form-item
        v-if="showCategorySelect"
        label="选择分类"
        prop="categoryId"
        :rules="[{ required: true, message: '请选择分类', trigger: 'change' }]"
      >
        <el-tree-select
          v-model="formData.categoryId"
          :data="categoryList"
          :props="{ label: 'categoryName', value: 'id', children: 'children' }"
          placeholder="选择资产分类"
          check-strictly
          style="width: 100%"
        />
      </el-form-item>

      <!-- 按位置时选择位置 -->
      <el-form-item
        v-if="showLocationSelect"
        label="选择位置"
        prop="scopeValue"
        :rules="[{ required: true, message: '请选择位置类型', trigger: 'change' }]"
      >
        <el-select v-model="formData.scopeValue" placeholder="选择位置类型" style="width: 100%">
          <el-option
            v-for="opt in locationOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <div class="text-xs text-gray-400 mt-1">
          将盘点该位置类型下的所有资产
        </div>
      </el-form-item>

      <el-form-item label="开始日期" prop="startDate">
        <el-date-picker
          v-model="formData.startDate"
          type="date"
          placeholder="选择开始日期"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="截止日期" prop="endDate">
        <el-date-picker
          v-model="formData.endDate"
          type="date"
          placeholder="选择截止日期"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="快捷设置">
        <div class="flex gap-2">
          <el-button size="small" @click="setDateRange(7)">一周</el-button>
          <el-button size="small" @click="setDateRange(14)">两周</el-button>
          <el-button size="small" @click="setDateRange(30)">一个月</el-button>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        创建盘点
      </el-button>
    </template>
  </el-dialog>
</template>
