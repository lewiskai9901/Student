<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Pencil, Trash2, FolderTree, ChevronRight, ChevronDown } from 'lucide-vue-next'
import { catalogApi } from '@/api/insp/catalog'
import type { TemplateCatalog, TemplateCatalogTreeNode, CreateCatalogRequest, UpdateCatalogRequest } from '@/types/insp/template'
import InspEmptyState from '../shared/InspEmptyState.vue'

// State
const loading = ref(false)
const catalogs = ref<TemplateCatalog[]>([])
const catalogTree = ref<TemplateCatalogTreeNode[]>([])
const expandedIds = ref<Set<number>>(new Set())

// Mousedown guard for dialog mask
const maskMouseDownTarget = ref<EventTarget | null>(null)
function onMaskMouseDown(e: MouseEvent) { maskMouseDownTarget.value = e.target }
function onMaskClose(e: MouseEvent, closeFn: () => void) {
  if (e.target === e.currentTarget && maskMouseDownTarget.value === e.currentTarget) closeFn()
  maskMouseDownTarget.value = null
}

// Dialog
const dialogVisible = ref(false)
const dialogTitle = ref('创建分类')
const editingId = ref<number | null>(null)
const form = ref({
  catalogCode: '',
  catalogName: '',
  parentId: null as number | null,
  description: '',
  icon: '',
  sortOrder: 0,
  isEnabled: true,
})

// Actions
async function loadData() {
  loading.value = true
  try {
    const [list, tree] = await Promise.all([catalogApi.getList(), catalogApi.getTree()])
    catalogs.value = list
    catalogTree.value = tree
  } catch (e: any) {
    ElMessage.error(e.message || '加载分类失败')
  } finally {
    loading.value = false
  }
}

function toggleExpand(id: number) {
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
  }
}

function openCreate(parentId?: number) {
  editingId.value = null
  dialogTitle.value = '创建分类'
  form.value = {
    catalogCode: '',
    catalogName: '',
    parentId: parentId || null,
    description: '',
    icon: '',
    sortOrder: 0,
    isEnabled: true,
  }
  dialogVisible.value = true
}

function openEdit(catalog: TemplateCatalog) {
  editingId.value = catalog.id
  dialogTitle.value = '编辑分类'
  form.value = {
    catalogCode: catalog.catalogCode,
    catalogName: catalog.catalogName,
    parentId: catalog.parentId,
    description: catalog.description || '',
    icon: catalog.icon || '',
    sortOrder: catalog.sortOrder,
    isEnabled: catalog.isEnabled,
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.catalogName.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  try {
    if (editingId.value) {
      const data: UpdateCatalogRequest = {
        catalogName: form.value.catalogName,
        description: form.value.description || undefined,
        parentId: form.value.parentId,
        icon: form.value.icon || undefined,
        sortOrder: form.value.sortOrder,
        isEnabled: form.value.isEnabled,
      }
      await catalogApi.update(editingId.value, data)
      ElMessage.success('更新成功')
    } else {
      if (!form.value.catalogCode.trim()) {
        ElMessage.warning('请输入分类编码')
        return
      }
      const data: CreateCatalogRequest = {
        catalogCode: form.value.catalogCode,
        catalogName: form.value.catalogName,
        parentId: form.value.parentId,
        description: form.value.description || undefined,
        icon: form.value.icon || undefined,
        sortOrder: form.value.sortOrder,
      }
      await catalogApi.create(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleDelete(catalog: TemplateCatalog) {
  try {
    await ElMessageBox.confirm(`确认删除分类「${catalog.catalogName}」？`, '确认删除', { type: 'warning' })
    await catalogApi.delete(catalog.id)
    ElMessage.success('已删除')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

onMounted(loadData)
</script>

<template>
  <div class="p-6">
    <!-- Header -->
    <div class="mb-5 flex items-center justify-between">
      <div class="flex items-center gap-2">
        <FolderTree :size="20" class="text-blue-500" />
        <h2 class="text-lg font-semibold text-gray-800">模板分类</h2>
      </div>
      <button
        class="flex items-center gap-1.5 rounded-md bg-blue-500 px-4 py-2 text-sm text-white transition hover:bg-blue-600"
        @click="openCreate()"
      >
        <Plus :size="16" />
        创建分类
      </button>
    </div>

    <!-- Content -->
    <div v-if="loading" class="py-16 text-center text-gray-400">加载中...</div>
    <InspEmptyState
      v-else-if="catalogTree.length === 0"
      title="暂无分类"
      description="创建模板分类来组织模板"
      action-label="创建分类"
      @action="openCreate()"
    />
    <div v-else class="space-y-1">
      <template v-for="node in catalogTree" :key="node.id">
        <!-- Root node -->
        <div
          class="group flex items-center justify-between rounded-lg border border-gray-200 px-4 py-3 transition hover:border-blue-200 hover:bg-blue-50/30"
        >
          <div class="flex items-center gap-2">
            <button
              v-if="node.children && node.children.length > 0"
              class="text-gray-400 hover:text-gray-600"
              @click="toggleExpand(node.id)"
            >
              <ChevronDown v-if="expandedIds.has(node.id)" :size="16" />
              <ChevronRight v-else :size="16" />
            </button>
            <span v-else class="w-4" />
            <span class="font-medium text-gray-700">{{ node.catalogName }}</span>
            <span class="text-xs text-gray-400">{{ node.catalogCode }}</span>
            <span
              v-if="!node.isEnabled"
              class="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-400"
            >已禁用</span>
          </div>
          <div class="flex items-center gap-2 opacity-0 transition group-hover:opacity-100">
            <button class="text-gray-400 hover:text-blue-500" title="添加子分类" @click="openCreate(node.id)">
              <Plus :size="16" />
            </button>
            <button class="text-gray-400 hover:text-blue-500" title="编辑" @click="openEdit(node)">
              <Pencil :size="16" />
            </button>
            <button class="text-gray-400 hover:text-red-500" title="删除" @click="handleDelete(node)">
              <Trash2 :size="16" />
            </button>
          </div>
        </div>
        <!-- Children -->
        <template v-if="expandedIds.has(node.id) && node.children">
          <div
            v-for="child in node.children"
            :key="child.id"
            class="group ml-8 flex items-center justify-between rounded-lg border border-gray-100 px-4 py-2.5 transition hover:border-blue-200 hover:bg-blue-50/30"
          >
            <div class="flex items-center gap-2">
              <span class="text-gray-600">{{ child.catalogName }}</span>
              <span class="text-xs text-gray-400">{{ child.catalogCode }}</span>
            </div>
            <div class="flex items-center gap-2 opacity-0 transition group-hover:opacity-100">
              <button class="text-gray-400 hover:text-blue-500" title="编辑" @click="openEdit(child)">
                <Pencil :size="16" />
              </button>
              <button class="text-gray-400 hover:text-red-500" title="删除" @click="handleDelete(child)">
                <Trash2 :size="16" />
              </button>
            </div>
          </div>
        </template>
      </template>
    </div>

    <!-- Dialog -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @mousedown="onMaskMouseDown" @click="onMaskClose($event, () => dialogVisible = false)">
          <div class="w-[480px] rounded-lg bg-white p-6 shadow-xl">
            <h3 class="mb-4 text-base font-semibold text-gray-800">{{ dialogTitle }}</h3>
            <div class="space-y-4">
              <div v-if="!editingId">
                <label class="mb-1 block text-sm text-gray-600">分类编码 <span class="text-red-400">*</span></label>
                <input v-model="form.catalogCode" class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-400" placeholder="唯一编码" />
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-600">分类名称 <span class="text-red-400">*</span></label>
                <input v-model="form.catalogName" class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-400" placeholder="分类名称" />
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-600">描述</label>
                <textarea v-model="form.description" class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-400" rows="2" placeholder="可选描述" />
              </div>
              <div class="flex gap-4">
                <div class="flex-1">
                  <label class="mb-1 block text-sm text-gray-600">排序</label>
                  <input v-model.number="form.sortOrder" type="number" class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-400" />
                </div>
                <div v-if="editingId" class="flex items-end gap-2 pb-0.5">
                  <label class="flex items-center gap-1.5 text-sm text-gray-600">
                    <input v-model="form.isEnabled" type="checkbox" class="rounded" />
                    启用
                  </label>
                </div>
              </div>
            </div>
            <div class="mt-6 flex justify-end gap-3">
              <button class="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-600 hover:bg-gray-50" @click="dialogVisible = false">取消</button>
              <button class="rounded-md bg-blue-500 px-4 py-2 text-sm text-white hover:bg-blue-600" @click="handleSave">保存</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
