<template>
  <div class="flex h-screen bg-gray-50">
    <!-- 左侧树形导航 -->
    <aside class="w-72 flex-shrink-0 border-r border-gray-200 bg-white flex flex-col">
      <!-- 头部 -->
      <div class="p-4 border-b border-gray-100">
        <h1 class="text-lg font-semibold text-gray-900 flex items-center gap-2">
          <Settings class="h-5 w-5 text-blue-600" />
          量化配置
        </h1>
        <p class="text-xs text-gray-500 mt-1">管理模板、类别和扣分项</p>
      </div>

      <!-- 树形导航 -->
      <div class="flex-1 overflow-auto p-3">
        <!-- 检查模板分组 -->
        <div class="mb-4">
          <div
            class="flex items-center justify-between px-2 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 rounded cursor-pointer"
            @click="toggleSection('templates')"
          >
            <span class="flex items-center gap-2">
              <ChevronRight
                class="h-4 w-4 transition-transform"
                :class="{ 'rotate-90': expandedSections.has('templates') }"
              />
              <ClipboardList class="h-4 w-4 text-emerald-600" />
              检查模板
            </span>
            <button
              v-if="hasPermission('quantification:template:add')"
              @click.stop="handleAddTemplate"
              class="p-1 rounded hover:bg-emerald-100 text-emerald-600"
              title="新增模板"
            >
              <Plus class="h-3.5 w-3.5" />
            </button>
          </div>
          <div v-if="expandedSections.has('templates')" class="ml-4 mt-1 space-y-0.5">
            <div
              v-for="template in templateList"
              :key="template.id"
              @click="selectNode('template', template)"
              class="flex items-center gap-2 px-2 py-1.5 rounded cursor-pointer text-sm"
              :class="selectedNode?.type === 'template' && selectedNode?.data?.id === template.id
                ? 'bg-blue-50 text-blue-700'
                : 'text-gray-600 hover:bg-gray-50'"
            >
              <FileText class="h-4 w-4" />
              <span class="truncate flex-1">{{ template.templateName }}</span>
              <span v-if="template.isDefault === 1" class="text-amber-500">
                <Star class="h-3 w-3" />
              </span>
            </div>
            <div v-if="templateList.length === 0" class="px-2 py-2 text-xs text-gray-400">
              暂无模板
            </div>
          </div>
        </div>

        <!-- 检查类别分组 -->
        <div>
          <div
            class="flex items-center justify-between px-2 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 rounded cursor-pointer"
            @click="toggleSection('categories')"
          >
            <span class="flex items-center gap-2">
              <ChevronRight
                class="h-4 w-4 transition-transform"
                :class="{ 'rotate-90': expandedSections.has('categories') }"
              />
              <FolderOpen class="h-4 w-4 text-blue-600" />
              检查类别
            </span>
            <button
              v-if="hasPermission('quantification:config:add')"
              @click.stop="handleAddCategory"
              class="p-1 rounded hover:bg-blue-100 text-blue-600"
              title="新增类别"
            >
              <Plus class="h-3.5 w-3.5" />
            </button>
          </div>
          <div v-if="expandedSections.has('categories')" class="ml-4 mt-1 space-y-0.5">
            <div v-for="category in categoryList" :key="category.id">
              <!-- 类别节点 -->
              <div
                class="flex items-center gap-1 px-2 py-1.5 rounded cursor-pointer text-sm"
                :class="selectedNode?.type === 'category' && selectedNode?.data?.id === category.id
                  ? 'bg-blue-50 text-blue-700'
                  : 'text-gray-600 hover:bg-gray-50'"
              >
                <button
                  @click.stop="toggleCategory(category.id)"
                  class="p-0.5 hover:bg-gray-200 rounded"
                >
                  <ChevronRight
                    class="h-3.5 w-3.5 transition-transform"
                    :class="{ 'rotate-90': expandedCategories.has(category.id) }"
                  />
                </button>
                <span @click="selectNode('category', category)" class="flex items-center gap-2 flex-1 truncate">
                  <Folder class="h-4 w-4" />
                  {{ category.categoryName }}
                </span>
                <button
                  v-if="hasPermission('quantification:config:add')"
                  @click.stop="handleAddDeduction(category)"
                  class="p-1 rounded hover:bg-amber-100 text-amber-600 opacity-0 group-hover:opacity-100"
                  title="添加扣分项"
                >
                  <Plus class="h-3 w-3" />
                </button>
              </div>

              <!-- 扣分项列表 -->
              <div v-if="expandedCategories.has(category.id)" class="ml-5 mt-0.5 space-y-0.5">
                <div
                  v-for="item in getDeductionItems(category.id)"
                  :key="item.id"
                  @click="selectNode('deduction', item, category)"
                  class="flex items-center gap-2 px-2 py-1 rounded cursor-pointer text-sm"
                  :class="selectedNode?.type === 'deduction' && selectedNode?.data?.id === item.id
                    ? 'bg-blue-50 text-blue-700'
                    : 'text-gray-500 hover:bg-gray-50'"
                >
                  <ListChecks class="h-3.5 w-3.5" />
                  <span class="truncate">{{ item.itemName }}</span>
                </div>
                <div v-if="getDeductionItems(category.id).length === 0" class="px-2 py-1 text-xs text-gray-400">
                  无扣分项
                </div>
                <button
                  v-if="hasPermission('quantification:config:add')"
                  @click="handleAddDeduction(category)"
                  class="flex items-center gap-1 px-2 py-1 text-xs text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded w-full"
                >
                  <Plus class="h-3 w-3" />
                  添加扣分项
                </button>
              </div>
            </div>
            <div v-if="categoryList.length === 0" class="px-2 py-2 text-xs text-gray-400">
              暂无类别
            </div>
          </div>
        </div>
      </div>

      <!-- 底部刷新 -->
      <div class="p-3 border-t border-gray-100">
        <button
          @click="refreshAll"
          class="w-full flex items-center justify-center gap-2 px-3 py-2 text-sm text-gray-600 hover:bg-gray-100 rounded-lg"
        >
          <RotateCcw class="h-4 w-4" />
          刷新数据
        </button>
      </div>
    </aside>

    <!-- 右侧详情面板 -->
    <main class="flex-1 overflow-auto">
      <!-- 空状态 -->
      <div v-if="!selectedNode" class="h-full flex items-center justify-center">
        <div class="text-center">
          <MousePointerClick class="h-16 w-16 text-gray-300 mx-auto mb-4" />
          <p class="text-gray-500">请从左侧选择一个项目进行编辑</p>
          <p class="text-sm text-gray-400 mt-1">或点击 + 按钮创建新项目</p>
        </div>
      </div>

      <!-- 模板面板 -->
      <TemplatePanel
        v-else-if="selectedNode.type === 'template'"
        :template="selectedNode.data"
        :is-new="selectedNode.isNew"
        :categories="categoryList"
        @saved="handleTemplateSaved"
        @deleted="handleTemplateDeleted"
        @cancel="selectedNode = null"
      />

      <!-- 类别面板 -->
      <CategoryPanel
        v-else-if="selectedNode.type === 'category'"
        :category="selectedNode.data"
        :is-new="selectedNode.isNew"
        @saved="handleCategorySaved"
        @deleted="handleCategoryDeleted"
        @cancel="selectedNode = null"
      />

      <!-- 扣分项面板 -->
      <DeductionPanel
        v-else-if="selectedNode.type === 'deduction'"
        :deduction="selectedNode.data"
        :category="selectedNode.parentCategory"
        :categories="categoryList"
        :is-new="selectedNode.isNew"
        @saved="handleDeductionSaved"
        @deleted="handleDeductionDeleted"
        @cancel="selectedNode = null"
      />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Settings, ChevronRight, ClipboardList, FolderOpen, Folder,
  FileText, ListChecks, Plus, Star, RotateCcw, MousePointerClick
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { getAllCategories } from '@/api/v2/quantification'
import { getDeductionItemsByTypeId } from '@/api/v2/quantification'
import { getCheckTemplatePage } from '@/api/v2/quantification'
import TemplatePanel from './components/panels/TemplatePanel.vue'
import CategoryPanel from './components/panels/CategoryPanel.vue'
import DeductionPanel from './components/panels/DeductionPanel.vue'

const authStore = useAuthStore()
const hasPermission = (permission: string) => authStore.hasPermission(permission)

// 展开的区域
const expandedSections = ref<Set<string>>(new Set(['templates', 'categories']))
const expandedCategories = ref<Set<string | number>>(new Set())

// 数据列表
const templateList = ref<any[]>([])
const categoryList = ref<any[]>([])
const deductionCache = ref<Record<string | number, any[]>>({})

// 选中的节点
interface SelectedNode {
  type: 'template' | 'category' | 'deduction'
  data: any
  isNew?: boolean
  parentCategory?: any
}
const selectedNode = ref<SelectedNode | null>(null)

// 切换区域展开
const toggleSection = (section: string) => {
  if (expandedSections.value.has(section)) {
    expandedSections.value.delete(section)
  } else {
    expandedSections.value.add(section)
  }
}

// 切换类别展开
const toggleCategory = async (categoryId: string | number) => {
  if (expandedCategories.value.has(categoryId)) {
    expandedCategories.value.delete(categoryId)
  } else {
    expandedCategories.value.add(categoryId)
    // 加载扣分项
    if (!deductionCache.value[categoryId]) {
      await loadDeductionItems(categoryId)
    }
  }
}

// 选中节点
const selectNode = (type: 'template' | 'category' | 'deduction', data: any, parentCategory?: any) => {
  selectedNode.value = { type, data, parentCategory }
}

// 获取扣分项
const getDeductionItems = (categoryId: string | number) => {
  return deductionCache.value[categoryId] || []
}

// 加载模板列表
const loadTemplates = async () => {
  try {
    const res = await getCheckTemplatePage({ pageNum: 1, pageSize: 100 })
    templateList.value = res.records || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载模板失败')
  }
}

// 加载类别列表
const loadCategories = async () => {
  try {
    categoryList.value = await getAllCategories()
  } catch (error: any) {
    ElMessage.error(error.message || '加载类别失败')
  }
}

// 加载扣分项
const loadDeductionItems = async (categoryId: string | number) => {
  try {
    const items = await getDeductionItemsByTypeId(categoryId)
    deductionCache.value[categoryId] = items || []
  } catch (error: any) {
    console.error('加载扣分项失败', error)
    deductionCache.value[categoryId] = []
  }
}

// 刷新所有数据
const refreshAll = async () => {
  deductionCache.value = {}
  await Promise.all([loadTemplates(), loadCategories()])
  ElMessage.success('数据已刷新')
}

// 新增模板
const handleAddTemplate = () => {
  selectedNode.value = {
    type: 'template',
    data: {
      templateName: '',
      description: '',
      isDefault: 0,
      status: 1,
      categories: []
    },
    isNew: true
  }
}

// 新增类别
const handleAddCategory = () => {
  selectedNode.value = {
    type: 'category',
    data: {
      categoryName: '',
      categoryCode: '',
      status: 1
    },
    isNew: true
  }
}

// 新增扣分项
const handleAddDeduction = (category: any) => {
  selectedNode.value = {
    type: 'deduction',
    data: {
      typeId: category.id,
      itemName: '',
      deductMode: 1,
      fixedScore: 0,
      baseScore: 0,
      perPersonScore: 0,
      rangeMin: 0,
      rangeMax: 0,
      description: '',
      sortOrder: 0,
      status: 1
    },
    isNew: true,
    parentCategory: category
  }
}

// 模板保存后
const handleTemplateSaved = () => {
  loadTemplates()
  selectedNode.value = null
}

// 模板删除后
const handleTemplateDeleted = () => {
  loadTemplates()
  selectedNode.value = null
}

// 类别保存后
const handleCategorySaved = () => {
  loadCategories()
  selectedNode.value = null
}

// 类别删除后
const handleCategoryDeleted = () => {
  loadCategories()
  selectedNode.value = null
}

// 扣分项保存后
const handleDeductionSaved = (categoryId: string | number) => {
  loadDeductionItems(categoryId)
  selectedNode.value = null
}

// 扣分项删除后
const handleDeductionDeleted = (categoryId: string | number) => {
  loadDeductionItems(categoryId)
  selectedNode.value = null
}

onMounted(() => {
  loadTemplates()
  loadCategories()
})
</script>
