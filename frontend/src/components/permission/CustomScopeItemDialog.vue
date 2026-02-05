<!--
  自定义数据范围配置对话框
  用于配置 CUSTOM 范围时的具体范围项（部门、班级、楼栋等）
-->
<template>
  <el-dialog
    v-model="visible"
    title="配置自定义范围"
    width="650px"
    :close-on-click-modal="false"
    append-to-body
  >
    <!-- 模块信息 -->
    <div class="mb-4 p-3 bg-gray-50 rounded-lg">
      <span class="text-gray-600">数据模块：</span>
      <span class="font-medium text-gray-900">{{ module?.moduleName }}</span>
    </div>

    <!-- 添加范围项 -->
    <div class="mb-4 flex items-end gap-3">
      <div class="flex-1">
        <label class="block text-sm font-medium text-gray-700 mb-1">范围类型</label>
        <el-select v-model="addForm.itemTypeCode" placeholder="选择类型" class="w-full" @change="handleTypeChange">
          <el-option
            v-for="type in availableItemTypes"
            :key="type.itemTypeCode"
            :label="type.itemTypeName"
            :value="type.itemTypeCode"
          />
        </el-select>
      </div>

      <div class="flex-1">
        <label class="block text-sm font-medium text-gray-700 mb-1">选择范围</label>
        <el-select
          v-model="addForm.scopeId"
          placeholder="请选择"
          class="w-full"
          filterable
          :loading="loadingOptions"
        >
          <el-option
            v-for="opt in scopeItemOptions"
            :key="opt.id"
            :label="opt.name"
            :value="opt.id"
          />
        </el-select>
      </div>

      <!-- 包含下级 (仅部门) -->
      <div v-if="addForm.itemTypeCode === 'ORG_UNIT'" class="flex items-center">
        <el-checkbox v-model="addForm.includeChildren">包含下级</el-checkbox>
      </div>

      <el-button type="primary" :disabled="!canAdd" @click="handleAdd">
        <Plus class="h-4 w-4 mr-1" />
        添加
      </el-button>
    </div>

    <!-- 已添加的范围项列表 -->
    <div class="border border-gray-200 rounded-lg">
      <div class="bg-gray-50 px-4 py-2 border-b border-gray-200 text-sm font-medium text-gray-600">
        已添加的范围（{{ localItems.length }}项）
      </div>

      <div v-if="localItems.length === 0" class="p-8 text-center text-gray-400">
        <FolderOpen class="h-10 w-10 mx-auto mb-2" />
        <p>暂未添加任何范围项</p>
        <p class="text-xs mt-1">请在上方选择并添加范围</p>
      </div>

      <div v-else class="divide-y divide-gray-100 max-h-[300px] overflow-y-auto">
        <div
          v-for="(item, index) in localItems"
          :key="`${item.itemTypeCode}-${item.scopeId}`"
          class="px-4 py-3 flex items-center justify-between hover:bg-gray-50"
        >
          <div class="flex items-center gap-3">
            <component :is="getItemTypeIcon(item.itemTypeCode)" class="h-5 w-5 text-gray-400" />
            <div>
              <span class="text-gray-900">{{ item.scopeName }}</span>
              <span class="text-gray-400 text-sm ml-2">({{ getItemTypeName(item.itemTypeCode) }})</span>
              <span v-if="item.includeChildren" class="ml-2 text-xs text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded">
                含下级
              </span>
            </div>
          </div>

          <el-button type="danger" link @click="handleRemove(index)">
            <Trash2 class="h-4 w-4" />
          </el-button>
        </div>
      </div>
    </div>

    <!-- 底部按钮 -->
    <template #footer>
      <div class="flex justify-end gap-3">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Plus,
  Trash2,
  FolderOpen,
  Building2,
  Users,
  GraduationCap,
  Home,
  Layers
} from 'lucide-vue-next'
import { http } from '@/utils/request'
import type { ScopeItem, ScopeItemType } from '@/types/access'

// Props
const props = defineProps<{
  module: {
    moduleCode: string
    moduleName: string
  } | null
  items: ScopeItem[]
}>()

// Emits
const emit = defineEmits<{
  save: [items: ScopeItem[]]
}>()

// Model
const visible = defineModel<boolean>({ default: false })

// 状态
const loadingOptions = ref(false)
const itemTypes = ref<ScopeItemType[]>([])
const scopeItemOptions = ref<{ id: number; name: string }[]>([])
const localItems = ref<ScopeItem[]>([])

// 添加表单
const addForm = reactive({
  itemTypeCode: '',
  scopeId: null as number | null,
  includeChildren: false
})

// 可用的范围项类型 (根据模块过滤)
const availableItemTypes = computed(() => {
  // 这里可以根据 module.moduleCode 过滤适用的类型
  // 暂时返回所有类型
  return itemTypes.value
})

// 是否可以添加
const canAdd = computed(() => {
  return addForm.itemTypeCode && addForm.scopeId
})

// 获取范围项类型图标
function getItemTypeIcon(typeCode: string) {
  const icons: Record<string, any> = {
    'ORG_UNIT': Building2,
    'CLASS': Users,
    'GRADE': GraduationCap,
    'BUILDING': Home,
    'MAJOR': Layers
  }
  return icons[typeCode] || Building2
}

// 获取范围项类型名称
function getItemTypeName(typeCode: string) {
  const type = itemTypes.value.find(t => t.itemTypeCode === typeCode)
  return type?.itemTypeName || typeCode
}

// 加载范围项类型
async function loadItemTypes() {
  const defaultTypes: ScopeItemType[] = [
    { itemTypeCode: 'ORG_UNIT', itemTypeName: '部门', supportChildren: true },
    { itemTypeCode: 'CLASS', itemTypeName: '班级', supportChildren: false },
    { itemTypeCode: 'GRADE', itemTypeName: '年级', supportChildren: false },
    { itemTypeCode: 'BUILDING', itemTypeName: '楼栋', supportChildren: false }
  ]

  try {
    // 从配置表加载
    const res = await http.get<ScopeItemType[]>('/roles/data-permissions/scope-item-types')
    itemTypes.value = res || defaultTypes
  } catch {
    // 使用默认值
    itemTypes.value = defaultTypes
  }
}

// 切换类型时加载对应选项
async function handleTypeChange(typeCode: string) {
  addForm.scopeId = null
  addForm.includeChildren = false
  scopeItemOptions.value = []

  if (!typeCode) return

  loadingOptions.value = true
  try {
    const res = await http.get<{ id: number; name: string }[]>(
      '/roles/data-permissions/scope-items',
      { params: { itemTypeCode: typeCode } }
    )
    scopeItemOptions.value = res || []
  } catch (error) {
    console.error('加载选项失败:', error)
  } finally {
    loadingOptions.value = false
  }
}

// 添加范围项
function handleAdd() {
  if (!addForm.itemTypeCode || !addForm.scopeId) return

  // 检查是否已存在
  const exists = localItems.value.some(
    item => item.itemTypeCode === addForm.itemTypeCode && item.scopeId === addForm.scopeId
  )
  if (exists) {
    ElMessage.warning('该范围项已存在')
    return
  }

  // 获取名称
  const option = scopeItemOptions.value.find(o => o.id === addForm.scopeId)
  const scopeName = option?.name || String(addForm.scopeId)

  localItems.value.push({
    itemTypeCode: addForm.itemTypeCode,
    scopeId: addForm.scopeId,
    scopeName,
    includeChildren: addForm.itemTypeCode === 'ORG_UNIT' ? addForm.includeChildren : false
  })

  // 重置表单
  addForm.scopeId = null
  addForm.includeChildren = false
}

// 移除范围项
function handleRemove(index: number) {
  localItems.value.splice(index, 1)
}

// 保存
function handleSave() {
  emit('save', [...localItems.value])
  visible.value = false
}

// 监听对话框打开
watch(visible, (val) => {
  if (val) {
    // 复制传入的项
    localItems.value = props.items ? [...props.items] : []
    // 重置表单
    addForm.itemTypeCode = ''
    addForm.scopeId = null
    addForm.includeChildren = false
    scopeItemOptions.value = []
    // 加载类型
    loadItemTypes()
  }
})
</script>
