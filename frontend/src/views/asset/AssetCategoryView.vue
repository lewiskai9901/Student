<script setup lang="ts">
/**
 * 资产分类管理
 * UI优化版本 - 使用设计系统组件
 */
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  FolderTree,
  FolderOpen,
  Folder,
  Plus,
  Pencil,
  Trash2,
  Package,
  Layers
} from 'lucide-vue-next'
import { assetApi } from '@/api/v2/asset'
import type { AssetCategory, CreateCategoryRequest } from '@/types/v2/asset'
import { CategoryType, CategoryTypeMap, ManagementMode, ManagementModeMap } from '@/types/v2/asset'

// 设计系统组件
import EmptyState from '@/components/design-system/feedback/EmptyState.vue'

const loading = ref(false)
const categoryTree = ref<AssetCategory[]>([])
const expandedKeys = ref<number[]>([])

// 计算分类层级的辅助Map
const categoryLevelMap = ref<Map<number, number>>(new Map())

// 构建层级Map
function buildLevelMap(categories: AssetCategory[], level: number = 0) {
  for (const cat of categories) {
    categoryLevelMap.value.set(cat.id, level)
    if (cat.children && cat.children.length > 0) {
      buildLevelMap(cat.children, level + 1)
    }
  }
}

// 获取分类层级
function getCategoryLevel(categoryId: number): number {
  return categoryLevelMap.value.get(categoryId) || 0
}

// 表单
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const editingId = ref<number | null>(null)

const formData = ref<CreateCategoryRequest>({
  parentId: undefined,
  categoryCode: '',
  categoryName: '',
  categoryType: CategoryType.FIXED_ASSET,
  defaultManagementMode: ManagementMode.SINGLE_ITEM,
  depreciationYears: undefined,
  unit: '',
  sortOrder: 0,
  remark: ''
})

const rules: FormRules = {
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  categoryType: [{ required: true, message: '请选择分类类型', trigger: 'change' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑分类' : '新建分类')

const categoryTypeOptions = computed(() => {
  return Object.entries(CategoryTypeMap).map(([value, label]) => ({
    value: Number(value),
    label
  }))
})

const managementModeOptions = computed(() => {
  return Object.entries(ManagementModeMap).map(([value, label]) => ({
    value: Number(value),
    label
  }))
})

// 根据分类类型获取默认管理模式
function getDefaultManagementMode(categoryType: CategoryType): ManagementMode {
  switch (categoryType) {
    case CategoryType.FIXED_ASSET:
      return ManagementMode.SINGLE_ITEM
    case CategoryType.LOW_VALUE:
      return ManagementMode.SINGLE_ITEM // 低值易耗品默认单品，可修改
    case CategoryType.CONSUMABLE:
      return ManagementMode.BATCH
    default:
      return ManagementMode.SINGLE_ITEM
  }
}

// 判断分类类型是否支持批量管理
function supportsBatchManagement(categoryType: CategoryType): boolean {
  return categoryType !== CategoryType.FIXED_ASSET
}

// 分类类型变更时自动调整管理模式
function handleCategoryTypeChange(newType: CategoryType) {
  formData.value.defaultManagementMode = getDefaultManagementMode(newType)
}

onMounted(() => {
  loadCategories()
})

async function loadCategories() {
  loading.value = true
  try {
    const res = await assetApi.getCategoryTree()
    categoryTree.value = res || []
    // 构建层级Map
    categoryLevelMap.value.clear()
    buildLevelMap(categoryTree.value)
    // 默认展开所有一级分类
    expandedKeys.value = categoryTree.value.map(c => c.id)
  } catch (error) {
    console.error('Failed to load categories:', error)
    ElMessage.error('加载分类失败')
  } finally {
    loading.value = false
  }
}

function handleAdd(parent?: AssetCategory) {
  isEdit.value = false
  editingId.value = null
  const categoryType = parent?.categoryType ?? CategoryType.FIXED_ASSET
  formData.value = {
    parentId: parent?.id,
    categoryCode: parent ? `${parent.categoryCode}-` : '',
    categoryName: '',
    categoryType,
    defaultManagementMode: parent?.defaultManagementMode ?? getDefaultManagementMode(categoryType),
    depreciationYears: undefined,
    unit: parent?.unit || '',
    sortOrder: 0,
    remark: ''
  }
  dialogVisible.value = true
}

function handleEdit(category: AssetCategory) {
  isEdit.value = true
  editingId.value = category.id
  formData.value = {
    parentId: category.parentId ?? undefined,
    categoryCode: category.categoryCode,
    categoryName: category.categoryName,
    categoryType: category.categoryType,
    defaultManagementMode: category.defaultManagementMode ?? getDefaultManagementMode(category.categoryType),
    depreciationYears: category.depreciationYears ?? undefined,
    unit: category.unit || '',
    sortOrder: category.sortOrder ?? 0,
    remark: category.remark || ''
  }
  dialogVisible.value = true
}

async function handleDelete(category: AssetCategory) {
  if (category.children && category.children.length > 0) {
    ElMessage.warning('该分类存在子分类，无法删除')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除分类"${category.categoryName}"吗？`,
      '确认删除',
      { type: 'warning' }
    )

    await assetApi.deleteCategory(category.id)
    ElMessage.success('删除成功')
    loadCategories()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Delete failed:', error)
      ElMessage.error('删除失败')
    }
  }
}

async function handleSubmit() {
  try {
    const valid = await formRef.value?.validate()
    if (!valid) return

    if (isEdit.value && editingId.value) {
      await assetApi.updateCategory(editingId.value, formData.value)
      ElMessage.success('更新成功')
    } else {
      await assetApi.createCategory(formData.value)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadCategories()
  } catch (error) {
    console.error('Save failed:', error)
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  }
}

function handleDialogClose() {
  formRef.value?.resetFields()
}
</script>

<template>
  <div class="min-h-full bg-gray-50/50 p-6">
    <!-- 页面头部 -->
    <div class="mb-8">
      <div class="flex items-center gap-3 mb-2">
        <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-teal-500 to-emerald-600 flex items-center justify-center shadow-lg shadow-teal-500/20">
          <FolderTree class="w-5 h-5 text-white" />
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-900">资产分类管理</h1>
          <p class="text-sm text-gray-500">管理资产分类树，支持多级分类结构</p>
        </div>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="bg-white rounded-xl border border-gray-200 p-4 mb-6 shadow-sm">
      <div class="flex justify-between items-center">
        <el-button type="primary" @click="handleAdd()" class="!bg-gradient-to-r !from-teal-500 !to-emerald-600 !border-none shadow-lg shadow-teal-500/25 hover:shadow-teal-500/40 transition-shadow">
          <Plus class="w-4 h-4 mr-1" />
          新建一级分类
        </el-button>
        <el-button :icon="Refresh" @click="loadCategories" :loading="loading" class="!border-gray-200">
          刷新
        </el-button>
      </div>
    </div>

    <!-- 分类树表格 -->
    <div class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
      <div class="p-4 border-b border-gray-100">
        <h3 class="text-base font-semibold text-gray-900">分类结构</h3>
      </div>

      <div v-loading="loading" class="p-4">
        <!-- 空状态 -->
        <EmptyState
          v-if="categoryTree.length === 0 && !loading"
          title="暂无分类"
          description='点击"新建一级分类"按钮创建第一个分类'
          action-text="新建分类"
          @action="handleAdd()"
        >
          <template #icon><FolderTree /></template>
        </EmptyState>

        <!-- 分类树表格 -->
        <el-table
          v-else
          :data="categoryTree"
          row-key="id"
          :tree-props="{ children: 'children' }"
          :default-expand-all="true"
          class="modern-table"
        >
          <el-table-column label="分类名称" min-width="280">
            <template #default="{ row }">
              <div
                class="flex items-center gap-2"
                :style="{ paddingLeft: `${getCategoryLevel(row.id) * 24}px` }"
              >
                <!-- 层级连接线指示器 -->
                <div
                  v-if="getCategoryLevel(row.id) > 0"
                  class="flex items-center text-gray-300 mr-1"
                >
                  <span class="text-xs">└</span>
                </div>
                <div :class="[
                  'w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0',
                  row.children?.length ? 'bg-amber-100' : 'bg-gray-100'
                ]">
                  <FolderOpen v-if="row.children?.length" class="w-4 h-4 text-amber-600" />
                  <Folder v-else class="w-4 h-4 text-gray-500" />
                </div>
                <span class="font-medium text-gray-900">{{ row.categoryName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="categoryCode" label="分类编码" width="150">
            <template #default="{ row }">
              <span class="font-mono text-sm text-teal-600 bg-teal-50 px-2 py-0.5 rounded">{{ row.categoryCode }}</span>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <span :class="[
                'px-2.5 py-1 rounded-full text-xs font-medium',
                row.categoryType === 1 ? 'bg-blue-100 text-blue-700' :
                row.categoryType === 2 ? 'bg-amber-100 text-amber-700' :
                'bg-emerald-100 text-emerald-700'
              ]">
                {{ CategoryTypeMap[row.categoryType as keyof typeof CategoryTypeMap] || '未知' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="管理模式" width="100">
            <template #default="{ row }">
              <span :class="[
                'px-2 py-0.5 rounded text-xs font-medium',
                row.defaultManagementMode === 1 ? 'bg-gray-100 text-gray-700' : 'bg-purple-100 text-purple-700'
              ]">
                {{ ManagementModeMap[row.defaultManagementMode as keyof typeof ManagementModeMap] || '单品管理' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="depreciationYears" label="折旧年限" width="100" align="center">
            <template #default="{ row }">
              <span class="text-gray-600">{{ row.depreciationYears ? `${row.depreciationYears}年` : '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="unit" label="默认单位" width="80" align="center">
            <template #default="{ row }">
              <span class="text-gray-600">{{ row.unit || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="assetCount" label="资产数" width="90" align="center">
            <template #default="{ row }">
              <span class="inline-flex items-center gap-1 text-blue-600 font-semibold bg-blue-50 px-2 py-0.5 rounded">
                <Package class="w-3.5 h-3.5" />
                {{ row.assetCount || 0 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="70" align="center">
            <template #default="{ row }">
              <span class="text-gray-500 text-sm">{{ row.sortOrder }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <div class="flex items-center gap-1">
                <el-button type="primary" text size="small" @click="handleAdd(row)">
                  <Plus class="w-4 h-4 mr-0.5" />
                  子分类
                </el-button>
                <el-button type="primary" text size="small" @click="handleEdit(row)">
                  <Pencil class="w-4 h-4 mr-0.5" />
                  编辑
                </el-button>
                <el-button
                  type="danger"
                  text
                  size="small"
                  @click="handleDelete(row)"
                  :disabled="row.children?.length > 0 || row.assetCount > 0"
                >
                  <Trash2 class="w-4 h-4 mr-0.5" />
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      width="540px"
      :close-on-click-modal="false"
      @close="handleDialogClose"
      class="modern-dialog"
    >
      <template #header>
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-teal-500 to-emerald-600 flex items-center justify-center shadow-lg">
            <Layers class="w-5 h-5 text-white" />
          </div>
          <div>
            <h3 class="text-lg font-semibold text-gray-900">{{ dialogTitle }}</h3>
            <p class="text-sm text-gray-500">{{ isEdit ? '修改分类信息' : '创建新的资产分类' }}</p>
          </div>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
        class="mt-4"
      >
        <el-form-item label="分类编码" prop="categoryCode">
          <el-input v-model="formData.categoryCode" placeholder="如: TEACH-DESK" />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="formData.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类类型" prop="categoryType">
          <el-select
            v-model="formData.categoryType"
            placeholder="选择类型"
            style="width: 100%"
            @change="handleCategoryTypeChange"
          >
            <el-option
              v-for="item in categoryTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="管理模式">
          <el-select
            v-model="formData.defaultManagementMode"
            placeholder="选择管理模式"
            style="width: 100%"
            :disabled="formData.categoryType === CategoryType.FIXED_ASSET"
          >
            <el-option
              v-for="item in managementModeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
              :disabled="formData.categoryType === CategoryType.FIXED_ASSET && item.value === ManagementMode.BATCH"
            />
          </el-select>
          <div class="text-xs text-gray-400 mt-1 bg-gray-50 p-2 rounded">
            <template v-if="formData.categoryType === CategoryType.FIXED_ASSET">
              固定资产只能使用单品管理模式
            </template>
            <template v-else-if="formData.categoryType === CategoryType.CONSUMABLE">
              消耗品建议使用批量管理模式
            </template>
            <template v-else>
              低值易耗品可选择单品或批量管理
            </template>
          </div>
        </el-form-item>
        <el-form-item v-if="formData.categoryType === CategoryType.FIXED_ASSET" label="折旧年限">
          <el-input-number
            v-model="formData.depreciationYears"
            :min="1"
            :max="50"
            placeholder="年"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="默认单位">
          <el-input v-model="formData.unit" placeholder="如: 台、个、张" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sortOrder" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="2"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="flex items-center justify-end gap-3">
          <el-button @click="dialogVisible = false" class="!px-5 !rounded-lg">取消</el-button>
          <el-button type="primary" @click="handleSubmit" class="!px-5 !rounded-lg !bg-gradient-to-r !from-teal-500 !to-emerald-600 !border-none">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 现代表格样式 */
.modern-table :deep(.el-table__header th) {
  background: #f8fafc !important;
  font-weight: 600;
  color: #475569;
  font-size: 0.8125rem;
}

.modern-table :deep(.el-table__row) {
  transition: all 0.2s ease;
}

.modern-table :deep(.el-table__row:hover > td) {
  background: #f1f5f9 !important;
}

/* 树形表格缩进样式 */
.modern-table :deep(.el-table__indent) {
  padding-left: 0 !important;
}

.modern-table :deep(.el-table__placeholder) {
  width: 20px;
}

.modern-table :deep(.el-table__expand-icon) {
  margin-right: 8px;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.modern-table :deep(.el-table__expand-icon--expanded) {
  transform: rotate(90deg);
}

/* 子节点行缩进指示 - 使用背景渐变来表示层级 */
.modern-table :deep(.el-table__row--level-1) {
  background-color: #fafafa;
}

.modern-table :deep(.el-table__row--level-1 td:first-child .cell) {
  padding-left: 40px !important;
}

.modern-table :deep(.el-table__row--level-2 td:first-child .cell) {
  padding-left: 70px !important;
}

.modern-table :deep(.el-table__row--level-3 td:first-child .cell) {
  padding-left: 100px !important;
}

/* 对话框样式 */
.modern-dialog :deep(.el-dialog__header) {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 0;
}

.modern-dialog :deep(.el-dialog__body) {
  padding: 1.5rem;
}

.modern-dialog :deep(.el-dialog__footer) {
  padding: 1rem 1.5rem;
  border-top: 1px solid #e5e7eb;
}
</style>
