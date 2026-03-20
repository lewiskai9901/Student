<template>
  <div class="flex h-full flex-col bg-gray-50">
    <!-- Header -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">场所类型配置</h1>
        <p class="mt-0.5 text-sm text-gray-500">配置场所类型的分类、行为特征和层级关系</p>
      </div>
      <button
        @click="loadData"
        class="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
      >
        <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
        刷新
      </button>
    </div>

    <!-- Main Content: Left Tree + Right Detail -->
    <div class="flex flex-1 overflow-hidden">
      <!-- Left: Type Tree -->
      <div class="w-80 flex-shrink-0 overflow-y-auto border-r border-gray-200 bg-white">
        <div v-if="loading" class="flex items-center justify-center py-20">
          <div class="h-6 w-6 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
        </div>

        <div v-else class="p-3">
          <!-- Search -->
          <el-input
            v-model="searchText"
            placeholder="搜索类型..."
            size="small"
            clearable
            class="mb-3"
          />

          <!-- Tree nodes -->
          <template v-if="filteredTree.length > 0">
            <TypeTreeNode
              v-for="node in filteredTree"
              :key="node.id"
              :node="node"
              :selected-id="selectedTypeId"
              :depth="0"
              @select="handleSelectType"
              @add-child="handleAddChild"
              @delete="handleDeleteType"
            />
          </template>
          <div v-else class="py-10 text-center text-sm text-gray-400">
            <MapPin class="mx-auto mb-2 h-8 w-8 text-gray-300" />
            暂无类型
          </div>

          <!-- Add root type button -->
          <button
            @click="handleAddRoot"
            class="mt-3 flex w-full items-center gap-1.5 rounded-lg border border-dashed border-gray-300 px-3 py-2 text-sm text-gray-500 hover:border-blue-400 hover:text-blue-600"
          >
            <Plus class="h-3.5 w-3.5" />
            新增根类型
          </button>
        </div>
      </div>

      <!-- Right: Detail / Edit -->
      <div class="flex-1 overflow-y-auto">
        <!-- No selection -->
        <div v-if="!selectedType && !isCreating" class="flex h-full items-center justify-center text-gray-400">
          <div class="text-center">
            <Settings class="mx-auto mb-3 h-10 w-10 text-gray-300" />
            <p class="text-sm">选择左侧类型进行编辑，或新增类型</p>
          </div>
        </div>

        <!-- Edit/Create form -->
        <div v-else class="p-6 space-y-5">
          <!-- Header -->
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <h2 class="text-base font-semibold text-gray-900">
                {{ isCreating ? (addParentType ? '新增子类型' : '新增根类型') : '编辑类型' }}
              </h2>
              <span v-if="formData.category" class="rounded bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-600">
                {{ categoryLabel(formData.category) }}
              </span>
            </div>
            <div class="flex items-center gap-2">
              <button
                v-if="!isCreating && selectedType && !selectedType.system"
                @click="handleDeleteType(selectedType!)"
                class="rounded-lg border border-red-200 px-3 py-1.5 text-sm text-red-600 hover:bg-red-50"
              >
                删除
              </button>
              <button
                v-if="isCreating"
                @click="cancelCreate"
                class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-50"
              >
                取消
              </button>
              <button
                @click="handleSave"
                :disabled="saveLoading || (selectedType?.system && !isCreating)"
                class="rounded-lg bg-blue-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                {{ saveLoading ? '保存中...' : (isCreating ? '创建' : '保存') }}
              </button>
            </div>
          </div>

          <!-- Category selector (only when creating) -->
          <div v-if="isCreating && !formData.category">
            <h4 class="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-400">选择基础分类</h4>
            <div class="grid grid-cols-3 gap-3">
              <button
                v-for="bc in availableCategories"
                :key="bc.code"
                @click="selectCategory(bc.code)"
                class="rounded-xl border-2 border-gray-200 p-4 text-center transition-all hover:border-blue-300 hover:bg-blue-50/50"
              >
                <div class="text-2xl mb-1">{{ categoryIcon(bc.code) }}</div>
                <div class="text-sm font-medium text-gray-900">{{ bc.label }}</div>
                <div class="mt-0.5 text-xs text-gray-400">{{ bc.code }}</div>
              </button>
            </div>
          </div>

          <!-- Form fields (show after category is selected) -->
          <template v-if="formData.category">
            <!-- Basic info -->
            <div>
              <h4 class="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-400">基本信息</h4>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-1 block text-sm font-medium text-gray-700">
                    类型编码
                  </label>
                  <el-input
                    v-model="formData.typeCode"
                    placeholder="自动生成"
                    :disabled="!isCreating"
                    maxlength="50"
                  />
                </div>
                <div>
                  <label class="mb-1 block text-sm font-medium text-gray-700">
                    类型名称 <span class="text-red-500">*</span>
                  </label>
                  <el-input v-model="formData.typeName" placeholder="如：教学楼、宿舍" maxlength="30" />
                </div>
              </div>
              <div class="mt-3 grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-1 block text-sm font-medium text-gray-700">排序号</label>
                  <el-input-number v-model="formData.sortOrder" :min="0" :max="999" style="width: 100%" />
                </div>
              </div>
              <div class="mt-3">
                <label class="mb-1 block text-sm font-medium text-gray-700">描述</label>
                <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="选填" />
              </div>
            </div>

            <!-- Features -->
            <div>
              <h4 class="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-400">行为特征</h4>
              <div class="rounded-lg border border-gray-200 divide-y divide-gray-100">
                <div class="flex items-center gap-3 px-3 py-2.5">
                  <el-switch v-model="formFeatures.hasCapacity" />
                  <span class="text-sm text-gray-700">有容量</span>
                  <template v-if="formFeatures.hasCapacity">
                    <el-select v-model="formData.capacityUnit" placeholder="单位" style="width: 90px" size="small">
                      <el-option label="人" value="人" />
                      <el-option label="床位" value="床位" />
                      <el-option label="工位" value="工位" />
                      <el-option label="平方米" value="平方米" />
                    </el-select>
                    <el-input-number
                      v-model="formData.defaultCapacity"
                      :min="0"
                      placeholder="默认容量"
                      size="small"
                      style="width: 120px"
                    />
                  </template>
                </div>
                <div class="flex items-center gap-6 px-3 py-2.5">
                  <label class="inline-flex cursor-pointer items-center gap-1.5 text-sm text-gray-700">
                    <input type="checkbox" v-model="formFeatures.bookable" class="rounded border-gray-300 text-blue-600" />
                    可预订
                  </label>
                  <label class="inline-flex cursor-pointer items-center gap-1.5 text-sm text-gray-700">
                    <input type="checkbox" v-model="formFeatures.assignable" class="rounded border-gray-300 text-blue-600" />
                    可分配
                  </label>
                  <label class="inline-flex cursor-pointer items-center gap-1.5 text-sm text-gray-700">
                    <input type="checkbox" v-model="formFeatures.occupiable" class="rounded border-gray-300 text-blue-600" />
                    可入住
                  </label>
                </div>
              </div>
            </div>

            <!-- Hierarchy config -->
            <div>
              <h4 class="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-400">层级配置</h4>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-1 block text-sm font-medium text-gray-700">允许的子类型</label>
                  <el-select
                    v-model="formData.allowedChildTypeCodes"
                    multiple
                    placeholder="选择允许的子类型编码"
                    style="width: 100%"
                    filterable
                  >
                    <el-option
                      v-for="t in allFlatTypes.filter(t => t.typeCode !== formData.typeCode)"
                      :key="t.typeCode"
                      :label="t.typeName"
                      :value="t.typeCode"
                    />
                  </el-select>
                </div>
                <div v-if="!formData.parentTypeCode">
                  <label class="mb-1 block text-sm font-medium text-gray-700">最大层级深度</label>
                  <el-input-number v-model="formData.maxDepth" :min="1" :max="10" style="width: 100%" />
                </div>
              </div>
            </div>

            <!-- Cross-domain references -->
            <div>
              <h4 class="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-400">跨域关联</h4>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-1 block text-sm font-medium text-gray-700">关联用户类型</label>
                  <el-select
                    v-model="formData.defaultUserTypeCodes"
                    multiple
                    placeholder="选填"
                    style="width: 100%"
                    filterable
                    allow-create
                  >
                    <el-option
                      v-for="ut in userTypes"
                      :key="ut.typeCode"
                      :label="ut.typeName"
                      :value="ut.typeCode"
                    />
                  </el-select>
                </div>
                <div>
                  <label class="mb-1 block text-sm font-medium text-gray-700">关联组织类型</label>
                  <el-select
                    v-model="formData.defaultOrgTypeCodes"
                    multiple
                    placeholder="选填"
                    style="width: 100%"
                    filterable
                    allow-create
                  >
                    <el-option
                      v-for="ot in orgTypes"
                      :key="ot.typeCode"
                      :label="ot.typeName"
                      :value="ot.typeCode"
                    />
                  </el-select>
                </div>
              </div>
            </div>

            <!-- System type info -->
            <div v-if="selectedType?.system" class="rounded-lg bg-amber-50 border border-amber-200 px-4 py-3">
              <div class="flex items-center gap-1.5 text-sm text-amber-700">
                <Lock class="h-4 w-4" />
                系统预置类型，不可修改
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, h, ref, type PropType } from 'vue'
import { ChevronRight, ChevronDown, Plus, Trash2 } from 'lucide-vue-next'
import type { PlaceTypeTreeNode } from '@/types/universalPlace'

// Recursive tree node component
const TypeTreeNode = defineComponent({
  name: 'TypeTreeNode',
  props: {
    node: { type: Object as PropType<PlaceTypeTreeNode>, required: true },
    selectedId: { type: Number, default: null },
    depth: { type: Number, default: 0 }
  },
  emits: ['select', 'add-child', 'delete'],
  setup(props, { emit }) {
    const expanded = ref(true)

    const categoryIcons: Record<string, string> = {
      SITE: '🏫', BUILDING: '🏢', FLOOR: '📋', ROOM: '🚪', AREA: '📐', POINT: '📍'
    }

    return () => {
      const node = props.node
      const isSelected = props.selectedId === node.id
      const hasChildren = node.children && node.children.length > 0
      const icon = categoryIcons[node.category || ''] || '📦'
      const paddingLeft = `${props.depth * 16 + 8}px`
      const isLeaf = !node.allowedChildTypeCodes || node.allowedChildTypeCodes.length === 0

      const children: any[] = []

      // Main row
      children.push(
        h('div', {
          class: [
            'flex items-center gap-1.5 rounded-lg px-2 py-1.5 cursor-pointer text-sm transition-colors group',
            isSelected ? 'bg-blue-50 text-blue-700' : 'text-gray-700 hover:bg-gray-50'
          ],
          style: { paddingLeft },
          onClick: () => emit('select', node)
        }, [
          // Expand toggle
          hasChildren
            ? h('button', {
                class: 'flex-shrink-0 w-4 h-4 flex items-center justify-center',
                onClick: (e: Event) => { e.stopPropagation(); expanded.value = !expanded.value }
              }, [
                h(expanded.value ? ChevronDown : ChevronRight, { class: 'h-3 w-3 text-gray-400' })
              ])
            : h('span', { class: 'w-4' }),
          // Icon
          h('span', { class: 'text-sm flex-shrink-0' }, icon),
          // Name
          h('span', { class: 'flex-1 truncate text-sm font-medium' }, node.typeName),
          // Category badge
          h('span', {
            class: 'rounded px-1 py-0.5 text-[10px] bg-gray-100 text-gray-400 flex-shrink-0'
          }, node.category || ''),
          // Actions (shown on hover)
          h('div', { class: 'hidden group-hover:flex items-center gap-0.5 flex-shrink-0' }, [
            // Add child button (if not leaf)
            !isLeaf && h('button', {
              class: 'rounded p-0.5 text-gray-400 hover:text-green-600',
              title: '添加子类型',
              onClick: (e: Event) => { e.stopPropagation(); emit('add-child', node) }
            }, [h(Plus, { class: 'h-3 w-3' })]),
            // Delete button
            !node.system && h('button', {
              class: 'rounded p-0.5 text-gray-400 hover:text-red-500',
              title: '删除',
              onClick: (e: Event) => { e.stopPropagation(); emit('delete', node) }
            }, [h(Trash2, { class: 'h-3 w-3' })])
          ])
        ])
      )

      // Children
      if (hasChildren && expanded.value) {
        for (const child of node.children!) {
          children.push(
            h(TypeTreeNode, {
              node: child,
              selectedId: props.selectedId,
              depth: props.depth + 1,
              onSelect: (n: any) => emit('select', n),
              onAddChild: (n: any) => emit('add-child', n),
              onDelete: (n: any) => emit('delete', n)
            })
          )
        }
      }

      return h('div', children)
    }
  }
})

export default { components: { TypeTreeNode } }
</script>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, RefreshCw, Trash2, Lock, MapPin, Settings,
  ChevronRight, ChevronDown
} from 'lucide-vue-next'
import { universalPlaceTypeApi } from '@/api/universalPlaceType'
import { userTypeApi } from '@/api/userType'
import { orgTypeApi } from '@/api/orgType'
import type {
  PlaceTypeTreeNode, CreatePlaceTypeRequest, UpdatePlaceTypeRequest, PlaceCategory
} from '@/types/universalPlace'

// --- State ---
const loading = ref(false)
const saveLoading = ref(false)
const typeTree = ref<PlaceTypeTreeNode[]>([])
const categories = ref<PlaceCategory[]>([])
const allFlatTypes = ref<PlaceTypeTreeNode[]>([])
const selectedTypeId = ref<number | null>(null)
const selectedType = ref<PlaceTypeTreeNode | null>(null)
const isCreating = ref(false)
const addParentType = ref<PlaceTypeTreeNode | null>(null)
const searchText = ref('')

// Cross-domain refs
const userTypes = ref<{ typeCode: string; typeName: string }[]>([])
const orgTypes = ref<{ typeCode: string; typeName: string }[]>([])

const formData = reactive({
  typeCode: '',
  typeName: '',
  category: '',
  parentTypeCode: '',
  description: '',
  sortOrder: 0,
  capacityUnit: '人',
  defaultCapacity: undefined as number | undefined,
  allowedChildTypeCodes: [] as string[],
  maxDepth: undefined as number | undefined,
  defaultUserTypeCodes: [] as string[],
  defaultOrgTypeCodes: [] as string[],
  metadataSchema: '',
})

const formFeatures = reactive({
  hasCapacity: false,
  bookable: false,
  assignable: false,
  occupiable: false,
})

// --- Computed ---
const filteredTree = computed(() => {
  if (!searchText.value) return typeTree.value
  const q = searchText.value.toLowerCase()
  function filterNodes(nodes: PlaceTypeTreeNode[]): PlaceTypeTreeNode[] {
    return nodes
      .map(node => {
        const childMatches = node.children ? filterNodes(node.children) : []
        const selfMatch = node.typeName.toLowerCase().includes(q) || node.typeCode.toLowerCase().includes(q)
        if (selfMatch || childMatches.length > 0) {
          return { ...node, children: childMatches.length > 0 ? childMatches : node.children }
        }
        return null
      })
      .filter(Boolean) as PlaceTypeTreeNode[]
  }
  return filterNodes(typeTree.value)
})

const availableCategories = computed(() => {
  if (!addParentType.value) {
    // Root type → only SITE
    return categories.value.filter(bc => bc.root)
  }
  const parentCat = addParentType.value.category
  if (!parentCat) return []
  const parentCategory = categories.value.find(bc => bc.code === parentCat)
  if (!parentCategory) return []
  return categories.value.filter(bc => parentCategory.allowedChildCategories.includes(bc.code))
})

// --- Helpers ---
const categoryIcons: Record<string, string> = {
  SITE: '🏫', BUILDING: '🏢', FLOOR: '📋', ROOM: '🚪', AREA: '📐', POINT: '📍'
}

function categoryIcon(code: string): string {
  return categoryIcons[code] || '📦'
}

function categoryLabel(code: string): string {
  return categories.value.find(bc => bc.code === code)?.label || code
}

function flattenTree(nodes: PlaceTypeTreeNode[]): PlaceTypeTreeNode[] {
  const result: PlaceTypeTreeNode[] = []
  for (const node of nodes) {
    result.push(node)
    if (node.children) result.push(...flattenTree(node.children))
  }
  return result
}

function resetForm() {
  Object.assign(formData, {
    typeCode: '',
    typeName: '',
    category: '',
    parentTypeCode: '',
    description: '',
    sortOrder: 0,
    capacityUnit: '人',
    defaultCapacity: undefined,
    allowedChildTypeCodes: [],
    maxDepth: undefined,
    defaultUserTypeCodes: [],
    defaultOrgTypeCodes: [],
    metadataSchema: '',
  })
  Object.assign(formFeatures, {
    hasCapacity: false,
    bookable: false,
    assignable: false,
    occupiable: false,
  })
}

// --- Actions ---
async function loadData() {
  loading.value = true
  try {
    const [tree, cats] = await Promise.all([
      universalPlaceTypeApi.getTree(),
      universalPlaceTypeApi.getCategories()
    ])
    typeTree.value = tree
    categories.value = cats
    allFlatTypes.value = flattenTree(tree)

    // Re-select if previously selected
    if (selectedTypeId.value) {
      const found = allFlatTypes.value.find(t => t.id === selectedTypeId.value)
      if (!found) {
        selectedTypeId.value = null
        selectedType.value = null
      }
    }
  } catch {
    ElMessage.error('加载场所类型失败')
  } finally {
    loading.value = false
  }

  // Load cross-domain types silently
  loadCrossTypes()
}

async function loadCrossTypes() {
  try {
    const [ut, ot] = await Promise.all([
      userTypeApi.getEnabled().catch(() => []),
      orgTypeApi.getEnabled().catch(() => [])
    ])
    userTypes.value = (ut || []).map((t: any) => ({ typeCode: t.typeCode, typeName: t.typeName }))
    orgTypes.value = (ot || []).map((t: any) => ({ typeCode: t.typeCode, typeName: t.typeName }))
  } catch { /* silent */ }
}

function handleSelectType(node: PlaceTypeTreeNode) {
  selectedTypeId.value = node.id
  selectedType.value = node
  isCreating.value = false
  addParentType.value = null

  // Load into form
  resetForm()
  formData.typeCode = node.typeCode
  formData.typeName = node.typeName
  formData.category = node.category || ''
  formData.parentTypeCode = node.parentTypeCode || ''
  formData.sortOrder = node.sortOrder || 0
  formData.capacityUnit = node.capacityUnit || '人'
  formData.defaultCapacity = node.defaultCapacity
  formData.allowedChildTypeCodes = node.allowedChildTypeCodes || []
  formData.maxDepth = node.maxDepth
  formData.defaultUserTypeCodes = node.defaultUserTypeCodes || []
  formData.defaultOrgTypeCodes = node.defaultOrgTypeCodes || []
  formData.metadataSchema = node.metadataSchema || ''

  // Load features
  const f = node.features || {}
  formFeatures.hasCapacity = !!f.hasCapacity
  formFeatures.bookable = !!f.bookable
  formFeatures.assignable = !!f.assignable
  formFeatures.occupiable = !!f.occupiable

  // Load full type from API for description
  universalPlaceTypeApi.getById(node.id).then(fullType => {
    formData.description = fullType.description || ''
  }).catch(() => {})
}

function handleAddRoot() {
  selectedTypeId.value = null
  selectedType.value = null
  isCreating.value = true
  addParentType.value = null
  resetForm()
}

function handleAddChild(parentNode: PlaceTypeTreeNode) {
  selectedTypeId.value = null
  selectedType.value = null
  isCreating.value = true
  addParentType.value = parentNode
  resetForm()
  formData.parentTypeCode = parentNode.typeCode
}

function selectCategory(code: string) {
  formData.category = code

  // Apply default features from category
  const cat = categories.value.find(b => b.code === code)
  if (cat?.defaultFeatures) {
    formFeatures.hasCapacity = !!cat.defaultFeatures.hasCapacity
    formFeatures.bookable = !!cat.defaultFeatures.bookable
    formFeatures.assignable = !!cat.defaultFeatures.assignable
    formFeatures.occupiable = !!cat.defaultFeatures.occupiable
  }

  // Auto-recommend allowed child type codes based on category's allowedChildCategories
  if (cat?.allowedChildCategories && cat.allowedChildCategories.length > 0) {
    const childCodes = allFlatTypes.value
      .filter(t => t.category && cat.allowedChildCategories.includes(t.category) && t.typeCode !== formData.typeCode)
      .map(t => t.typeCode)
    if (childCodes.length > 0) {
      formData.allowedChildTypeCodes = childCodes
    }
  }
}

function cancelCreate() {
  isCreating.value = false
  addParentType.value = null
  resetForm()
}

async function handleDeleteType(node: PlaceTypeTreeNode) {
  try {
    await ElMessageBox.confirm(
      `确定删除类型"${node.typeName}"吗？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await universalPlaceTypeApi.delete(node.id)
    ElMessage.success('删除成功')
    if (selectedTypeId.value === node.id) {
      selectedTypeId.value = null
      selectedType.value = null
    }
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

function buildFeaturesMap(): Record<string, boolean> {
  return {
    hasCapacity: formFeatures.hasCapacity,
    bookable: formFeatures.bookable,
    assignable: formFeatures.assignable,
    occupiable: formFeatures.occupiable,
  }
}

async function handleSave() {
  if (!formData.typeName.trim()) {
    ElMessage.error('请填写类型名称')
    return
  }
  if (!formData.category) {
    ElMessage.error('请选择基础分类')
    return
  }

  saveLoading.value = true
  try {
    if (isCreating.value) {
      const createData: CreatePlaceTypeRequest = {
        typeCode: formData.typeCode || undefined,
        typeName: formData.typeName,
        category: formData.category,
        parentTypeCode: formData.parentTypeCode || undefined,
        description: formData.description || undefined,
        features: buildFeaturesMap(),
        metadataSchema: formData.metadataSchema || undefined,
        allowedChildTypeCodes: formData.allowedChildTypeCodes.length > 0 ? formData.allowedChildTypeCodes : undefined,
        maxDepth: formData.maxDepth,
        defaultUserTypeCodes: formData.defaultUserTypeCodes.length > 0 ? formData.defaultUserTypeCodes : undefined,
        defaultOrgTypeCodes: formData.defaultOrgTypeCodes.length > 0 ? formData.defaultOrgTypeCodes : undefined,
        capacityUnit: formFeatures.hasCapacity ? formData.capacityUnit : undefined,
        defaultCapacity: formFeatures.hasCapacity ? formData.defaultCapacity : undefined,
        sortOrder: formData.sortOrder,
      }
      const created = await universalPlaceTypeApi.create(createData)
      ElMessage.success('创建成功')
      isCreating.value = false
      addParentType.value = null
      await loadData()
      selectedTypeId.value = created.id
      const found = allFlatTypes.value.find(t => t.id === created.id)
      if (found) selectedType.value = found
    } else if (selectedType.value) {
      const updateData: UpdatePlaceTypeRequest = {
        typeName: formData.typeName,
        category: formData.category,
        description: formData.description || undefined,
        features: buildFeaturesMap(),
        metadataSchema: formData.metadataSchema || undefined,
        allowedChildTypeCodes: formData.allowedChildTypeCodes,
        maxDepth: formData.maxDepth,
        defaultUserTypeCodes: formData.defaultUserTypeCodes,
        defaultOrgTypeCodes: formData.defaultOrgTypeCodes,
        capacityUnit: formFeatures.hasCapacity ? formData.capacityUnit : undefined,
        defaultCapacity: formFeatures.hasCapacity ? formData.defaultCapacity : undefined,
        sortOrder: formData.sortOrder,
      }
      await universalPlaceTypeApi.update(selectedType.value.id, updateData)
      ElMessage.success('更新成功')
      loadData()
    }
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    saveLoading.value = false
  }
}

onMounted(loadData)
</script>
