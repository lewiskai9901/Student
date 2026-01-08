<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 搜索和操作栏 -->
    <div class="mb-6 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex items-center gap-4">
        <div class="relative flex-1 max-w-md">
          <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索部门名称或编码"
            class="h-9 w-full rounded-lg border border-gray-300 pl-10 pr-4 text-sm focus:border-blue-500 focus:outline-none"
            @keyup.enter="handleSearch"
          />
        </div>
        <button
          @click="handleSearch"
          class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
        >
          <Search class="h-4 w-4" />
          搜索
        </button>
        <button
          v-if="searchKeyword"
          @click="searchKeyword = ''; loadDepartmentTree()"
          class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          <RotateCcw class="h-4 w-4" />
          重置
        </button>
        <div class="ml-auto">
          <button
            @click="handleAdd"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          >
            <Plus class="h-4 w-4" />
            新增部门
          </button>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <div class="flex items-center gap-2">
          <span class="font-medium text-gray-900">部门列表</span>
          <span class="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-600">树形结构</span>
        </div>
      </div>

      <table class="w-full">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50">
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">部门编码</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">部门名称</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">负责人</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">联系电话</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">邮箱</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">排序</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">状态</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">操作</th>
          </tr>
        </thead>
        <tbody v-if="loading">
          <tr>
            <td colspan="8" class="py-16 text-center">
              <Loader2 class="mx-auto h-8 w-8 animate-spin text-blue-600" />
              <div class="mt-2 text-sm text-gray-500">加载中...</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else-if="flatDepartments.length === 0">
          <tr>
            <td colspan="8" class="py-16 text-center">
              <Building2 class="mx-auto h-12 w-12 text-gray-300" />
              <div class="mt-2 text-sm text-gray-500">暂无数据</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr
            v-for="row in flatDepartments"
            :key="row.id"
            class="border-b border-gray-100 hover:bg-gray-50"
          >
            <td class="px-4 py-3">
              <span class="rounded bg-gray-100 px-2 py-0.5 font-mono text-xs text-gray-600">{{ row.unitCode }}</span>
            </td>
            <td class="px-4 py-3">
              <div class="flex items-center gap-2" :style="{ paddingLeft: `${row.level * 20}px` }">
                <button
                  v-if="row.hasChildren"
                  @click="toggleExpand(row)"
                  class="rounded p-0.5 hover:bg-gray-100"
                >
                  <ChevronRight
                    class="h-4 w-4 text-gray-400 transition-transform"
                    :class="{ 'rotate-90': row.expanded }"
                  />
                </button>
                <span v-else class="w-5"></span>
                <div class="flex h-7 w-7 items-center justify-center rounded-lg bg-blue-100">
                  <Building2 class="h-4 w-4 text-blue-600" />
                </div>
                <span class="font-medium text-gray-900">{{ row.unitName }}</span>
              </div>
            </td>
            <td class="px-4 py-3 text-gray-600">{{ row.leaderName || '-' }}</td>
            <td class="px-4 py-3 text-gray-600">{{ row.phone || '-' }}</td>
            <td class="px-4 py-3 text-gray-600">{{ row.email || '-' }}</td>
            <td class="px-4 py-3 text-center text-gray-600">{{ row.sortOrder ?? 0 }}</td>
            <td class="px-4 py-3 text-center">
              <button
                @click="handleStatusChange(row)"
                :class="[
                  'relative inline-flex h-5 w-9 items-center rounded-full transition-colors',
                  row.isEnabled ? 'bg-blue-600' : 'bg-gray-300'
                ]"
              >
                <span
                  :class="[
                    'inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform',
                    row.isEnabled ? 'translate-x-4' : 'translate-x-0.5'
                  ]"
                />
              </button>
            </td>
            <td class="px-4 py-3">
              <div class="flex items-center justify-center gap-1">
                <button
                  @click="handleEdit(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-blue-50 hover:text-blue-600"
                  title="编辑"
                >
                  <Pencil class="h-4 w-4" />
                </button>
                <button
                  @click="handleDelete(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-red-50 hover:text-red-600"
                  title="删除"
                >
                  <Trash2 class="h-4 w-4" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新增/编辑对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="dialogVisible = false"></div>
          <div class="relative w-full max-w-xl rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">{{ dialogTitle }}</h3>
              <button @click="dialogVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="max-h-[60vh] overflow-y-auto p-6">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-1 block text-sm text-gray-700">
                    部门编码 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="formData.unitCode"
                    type="text"
                    placeholder="请输入部门编码"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">
                    部门名称 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="formData.unitName"
                    type="text"
                    placeholder="请输入部门名称"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div class="col-span-2">
                  <label class="mb-1 block text-sm text-gray-700">上级部门</label>
                  <select
                    v-model="formData.parentId"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  >
                    <option :value="undefined">顶级部门</option>
                    <option
                      v-for="dept in departmentOptions"
                      :key="dept.id"
                      :value="dept.id"
                    >{{ '　'.repeat(dept.level) }}{{ dept.unitName }}</option>
                  </select>
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">排序</label>
                  <input
                    v-model.number="formData.sortOrder"
                    type="number"
                    min="0"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
              </div>
            </div>
            <div class="flex justify-end gap-3 border-t border-gray-200 px-6 py-4">
              <button
                @click="dialogVisible = false"
                class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                取消
              </button>
              <button
                @click="handleSubmit"
                :disabled="submitLoading"
                class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
                确定
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Building2,
  Plus,
  Search,
  RotateCcw,
  Pencil,
  Trash2,
  X,
  Loader2,
  ChevronRight
} from 'lucide-vue-next'
import {
  getDepartmentTree,
  createDepartment,
  updateDepartment,
  deleteDepartment,
  updateDepartmentStatus,
  type DepartmentCreateRequest,
  type DepartmentResponse
} from '@/api/department'

interface FlatDepartment extends DepartmentResponse {
  level: number
  expanded: boolean
  hasChildren: boolean
  visible: boolean
}

const loading = ref(false)
const submitLoading = ref(false)
const departmentTree = ref<DepartmentResponse[]>([])
const expandedIds = ref<Set<number>>(new Set())
const searchKeyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('新增部门')
const isEdit = ref(false)
const currentId = ref<number>()

const formData = reactive<DepartmentCreateRequest>({
  unitCode: '',
  unitName: '',
  parentId: undefined,
  sortOrder: 0
})

const flatDepartments = computed(() => {
  const result: FlatDepartment[] = []
  const flatten = (nodes: DepartmentResponse[], level: number, parentExpanded: boolean) => {
    nodes.forEach(node => {
      const hasChildren = node.children && node.children.length > 0
      const expanded = expandedIds.value.has(node.id)
      if (parentExpanded) {
        result.push({ ...node, level, expanded, hasChildren, visible: true })
      }
      if (hasChildren && expanded && parentExpanded) {
        flatten(node.children!, level + 1, true)
      }
    })
  }
  flatten(departmentTree.value, 0, true)
  return result
})

const departmentOptions = computed(() => {
  const result: { id: number; unitName: string; level: number }[] = []
  const flatten = (nodes: DepartmentResponse[], level: number) => {
    nodes.forEach(node => {
      if (!isEdit.value || node.id !== currentId.value) {
        result.push({ id: node.id, unitName: node.unitName, level })
        if (node.children?.length) flatten(node.children, level + 1)
      }
    })
  }
  flatten(departmentTree.value, 0)
  return result
})

const toggleExpand = (row: FlatDepartment) => {
  if (expandedIds.value.has(row.id)) {
    expandedIds.value.delete(row.id)
  } else {
    expandedIds.value.add(row.id)
  }
}

const loadDepartmentTree = async () => {
  loading.value = true
  try {
    const data = await getDepartmentTree()
    departmentTree.value = data || []
    data.forEach((node: DepartmentResponse) => expandedIds.value.add(node.id))
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  if (!searchKeyword.value.trim()) {
    loadDepartmentTree()
    return
  }
  const keyword = searchKeyword.value.toLowerCase()
  const filterTree = (nodes: DepartmentResponse[]): DepartmentResponse[] => {
    return nodes.filter(node => {
      const match = node.unitName.toLowerCase().includes(keyword) || node.unitCode.toLowerCase().includes(keyword)
      const filteredChildren = node.children ? filterTree(node.children) : []
      if (match || filteredChildren.length > 0) {
        expandedIds.value.add(node.id)
        return true
      }
      return false
    }).map(node => ({ ...node, children: node.children ? filterTree(node.children) : undefined }))
  }
  departmentTree.value = filterTree(departmentTree.value)
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增部门'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: FlatDepartment) => {
  isEdit.value = true
  currentId.value = row.id
  dialogTitle.value = '编辑部门'
  Object.assign(formData, {
    unitCode: row.unitCode,
    unitName: row.unitName,
    parentId: row.parentId || undefined,
    sortOrder: row.sortOrder
  })
  dialogVisible.value = true
}

const handleDelete = async (row: FlatDepartment) => {
  try {
    await ElMessageBox.confirm(`确定删除部门"${row.unitName}"吗?`, '删除确认', { type: 'warning' })
    await deleteDepartment(row.id)
    ElMessage.success('删除成功')
    searchKeyword.value = ''
    await loadDepartmentTree()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

const handleStatusChange = async (row: FlatDepartment) => {
  const newStatus = row.isEnabled ? 0 : 1
  try {
    await updateDepartmentStatus(row.id, newStatus)
    row.isEnabled = !row.isEnabled
    ElMessage.success('状态已更新')
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

const handleSubmit = async () => {
  if (!formData.unitCode || !formData.unitName) {
    ElMessage.error('请填写必填项')
    return
  }
  submitLoading.value = true
  try {
    if (isEdit.value && currentId.value) {
      await updateDepartment(currentId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await createDepartment(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadDepartmentTree()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

const resetForm = () => {
  formData.unitCode = ''
  formData.unitName = ''
  formData.parentId = undefined
  formData.sortOrder = 0
}

onMounted(() => {
  loadDepartmentTree()
})
</script>

<style scoped>
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
