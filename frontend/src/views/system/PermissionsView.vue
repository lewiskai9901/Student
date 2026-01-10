<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 操作栏 -->
    <div class="mb-6 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex items-center gap-4">
        <button
          @click="expandAll"
          class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          <component :is="isExpandAll ? FolderMinus : FolderPlus" class="h-4 w-4" />
          {{ isExpandAll ? '全部折叠' : '全部展开' }}
        </button>
        <button
          @click="loadPermissions"
          class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          <RotateCcw class="h-4 w-4" />
          刷新
        </button>
        <div class="ml-auto">
          <button
            @click="handleAdd(null)"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          >
            <Plus class="h-4 w-4" />
            新增根权限
          </button>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <div class="flex items-center gap-2">
          <span class="font-medium text-gray-900">权限列表</span>
          <span class="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-600">树形结构</span>
        </div>
      </div>

      <table class="w-full">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50">
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">权限名称</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">权限编码</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">类型</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">路由路径</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">排序</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">状态</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">操作</th>
          </tr>
        </thead>
        <tbody v-if="loading">
          <tr>
            <td colspan="7" class="py-16 text-center">
              <Loader2 class="mx-auto h-8 w-8 animate-spin text-blue-600" />
              <div class="mt-2 text-sm text-gray-500">加载中...</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else-if="flatPermissions.length === 0">
          <tr>
            <td colspan="7" class="py-16 text-center">
              <Lock class="mx-auto h-12 w-12 text-gray-300" />
              <div class="mt-2 text-sm text-gray-500">暂无数据</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr
            v-for="row in flatPermissions"
            :key="row.id"
            class="border-b border-gray-100 hover:bg-gray-50"
          >
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
                <div
                  class="flex h-7 w-7 items-center justify-center rounded-lg"
                  :class="getResourceTypeIconBg(row.resourceType)"
                >
                  <component
                    :is="getResourceTypeIcon(row.resourceType)"
                    class="h-4 w-4"
                    :class="getResourceTypeIconColor(row.resourceType)"
                  />
                </div>
                <span class="font-medium text-gray-900">{{ row.permissionName }}</span>
              </div>
            </td>
            <td class="px-4 py-3">
              <span class="rounded bg-gray-100 px-2 py-0.5 font-mono text-xs text-gray-600">{{ row.permissionCode }}</span>
            </td>
            <td class="px-4 py-3 text-center">
              <span
                :class="[
                  'rounded-full px-2 py-0.5 text-xs font-medium',
                  getResourceTypeBadge(row.resourceType)
                ]"
              >{{ getResourceTypeText(row.resourceType) }}</span>
            </td>
            <td class="max-w-xs truncate px-4 py-3 text-gray-600">{{ row.path || '-' }}</td>
            <td class="px-4 py-3 text-center text-gray-600">{{ row.sortOrder ?? 0 }}</td>
            <td class="px-4 py-3 text-center">
              <button
                @click="handleStatusChange(row)"
                :class="[
                  'relative inline-flex h-5 w-9 items-center rounded-full transition-colors',
                  row.status === 1 ? 'bg-blue-600' : 'bg-gray-300'
                ]"
              >
                <span
                  :class="[
                    'inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform',
                    row.status === 1 ? 'translate-x-4' : 'translate-x-0.5'
                  ]"
                />
              </button>
            </td>
            <td class="px-4 py-3">
              <div class="flex items-center justify-center gap-1">
                <button
                  @click="handleAdd(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-green-50 hover:text-green-600"
                  title="新增子权限"
                >
                  <Plus class="h-4 w-4" />
                </button>
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
          <div class="relative w-full max-w-md rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">{{ dialogTitle }}</h3>
              <button @click="dialogVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="max-h-[60vh] overflow-y-auto p-6">
              <div class="space-y-4">
                <div>
                  <label class="mb-1 block text-sm text-gray-700">
                    权限名称 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="formData.permissionName"
                    type="text"
                    placeholder="请输入权限名称"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">
                    权限编码 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="formData.permissionCode"
                    type="text"
                    placeholder="如: system:user:view"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 font-mono text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">父级权限</label>
                  <select
                    v-model="formData.parentId"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  >
                    <option :value="null">根权限</option>
                    <option
                      v-for="perm in permissionOptions"
                      :key="perm.id"
                      :value="perm.id"
                    >{{ '　'.repeat(perm.level) }}{{ perm.permissionName }}</option>
                  </select>
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">资源类型</label>
                  <div class="flex h-9 items-center gap-4">
                    <label class="flex items-center gap-1.5">
                      <input v-model="formData.resourceType" type="radio" :value="1" class="h-4 w-4" />
                      <span class="text-sm text-gray-700">菜单</span>
                    </label>
                    <label class="flex items-center gap-1.5">
                      <input v-model="formData.resourceType" type="radio" :value="2" class="h-4 w-4" />
                      <span class="text-sm text-gray-700">按钮</span>
                    </label>
                    <label class="flex items-center gap-1.5">
                      <input v-model="formData.resourceType" type="radio" :value="3" class="h-4 w-4" />
                      <span class="text-sm text-gray-700">接口</span>
                    </label>
                  </div>
                </div>
                <div v-if="formData.resourceType === 1">
                  <label class="mb-1 block text-sm text-gray-700">路由路径</label>
                  <input
                    v-model="formData.path"
                    type="text"
                    placeholder="如: /system/users"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div v-if="formData.resourceType === 1">
                  <label class="mb-1 block text-sm text-gray-700">组件路径</label>
                  <input
                    v-model="formData.component"
                    type="text"
                    placeholder="如: views/system/UsersView.vue"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div v-if="formData.resourceType === 1">
                  <label class="mb-1 block text-sm text-gray-700">图标</label>
                  <input
                    v-model="formData.icon"
                    type="text"
                    placeholder="如: User"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div class="grid grid-cols-2 gap-4">
                  <div>
                    <label class="mb-1 block text-sm text-gray-700">排序</label>
                    <input
                      v-model.number="formData.sortOrder"
                      type="number"
                      min="0"
                      class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                    />
                  </div>
                  <div>
                    <label class="mb-1 block text-sm text-gray-700">状态</label>
                    <div class="flex h-9 items-center gap-4">
                      <label class="flex items-center gap-1.5">
                        <input v-model="formData.status" type="radio" :value="1" class="h-4 w-4" />
                        <span class="text-sm text-gray-700">启用</span>
                      </label>
                      <label class="flex items-center gap-1.5">
                        <input v-model="formData.status" type="radio" :value="0" class="h-4 w-4" />
                        <span class="text-sm text-gray-700">禁用</span>
                      </label>
                    </div>
                  </div>
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">描述</label>
                  <textarea
                    v-model="formData.permissionDesc"
                    rows="2"
                    placeholder="请输入描述"
                    class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                  ></textarea>
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
  Lock,
  Plus,
  RotateCcw,
  Pencil,
  Trash2,
  X,
  Loader2,
  ChevronRight,
  FolderPlus,
  FolderMinus,
  Menu,
  MousePointer2,
  Link
} from 'lucide-vue-next'
import {
  getPermissionTree,
  createPermission,
  updatePermission,
  deletePermission
} from '@/api/v2/access'

interface Permission {
  id: number
  permissionName: string
  permissionCode: string
  permissionDesc?: string
  resourceType: number
  parentId: number | null
  path?: string
  component?: string
  icon?: string
  sortOrder?: number
  status: number
  children?: Permission[]
}

interface FlatPermission extends Permission {
  level: number
  expanded: boolean
  hasChildren: boolean
}

const loading = ref(false)
const submitLoading = ref(false)
const permissionTree = ref<Permission[]>([])
const expandedIds = ref<Set<number>>(new Set())
const isExpandAll = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)

const formData = reactive({
  id: null as number | null,
  permissionName: '',
  permissionCode: '',
  permissionDesc: '',
  resourceType: 1,
  parentId: null as number | null,
  path: '',
  component: '',
  icon: '',
  sortOrder: 0,
  status: 1
})

const flatPermissions = computed(() => {
  const result: FlatPermission[] = []
  const flatten = (nodes: Permission[], level: number, parentExpanded: boolean) => {
    nodes.forEach(node => {
      const hasChildren = node.children && node.children.length > 0
      const expanded = expandedIds.value.has(node.id)
      if (parentExpanded) {
        result.push({ ...node, level, expanded, hasChildren })
      }
      if (hasChildren && expanded && parentExpanded) {
        flatten(node.children!, level + 1, true)
      }
    })
  }
  flatten(permissionTree.value, 0, true)
  return result
})

const permissionOptions = computed(() => {
  const result: { id: number; permissionName: string; level: number }[] = []
  const flatten = (nodes: Permission[], level: number) => {
    nodes.forEach(node => {
      if (!isEdit.value || node.id !== formData.id) {
        result.push({ id: node.id, permissionName: node.permissionName, level })
        if (node.children?.length) flatten(node.children, level + 1)
      }
    })
  }
  flatten(permissionTree.value, 0)
  return result
})

const getResourceTypeIcon = (type: number) => {
  switch (type) {
    case 1: return Menu
    case 2: return MousePointer2
    case 3: return Link
    default: return Menu
  }
}

const getResourceTypeIconBg = (type: number) => {
  switch (type) {
    case 1: return 'bg-blue-100'
    case 2: return 'bg-green-100'
    case 3: return 'bg-gray-100'
    default: return 'bg-gray-100'
  }
}

const getResourceTypeIconColor = (type: number) => {
  switch (type) {
    case 1: return 'text-blue-600'
    case 2: return 'text-green-600'
    case 3: return 'text-gray-600'
    default: return 'text-gray-600'
  }
}

const getResourceTypeBadge = (type: number) => {
  switch (type) {
    case 1: return 'bg-blue-100 text-blue-700'
    case 2: return 'bg-green-100 text-green-700'
    case 3: return 'bg-gray-100 text-gray-700'
    default: return 'bg-gray-100 text-gray-700'
  }
}

const getResourceTypeText = (type: number) => {
  switch (type) {
    case 1: return '菜单'
    case 2: return '按钮'
    case 3: return '接口'
    default: return '-'
  }
}

const toggleExpand = (row: FlatPermission) => {
  if (expandedIds.value.has(row.id)) {
    expandedIds.value.delete(row.id)
  } else {
    expandedIds.value.add(row.id)
  }
}

const expandAll = () => {
  if (isExpandAll.value) {
    expandedIds.value.clear()
  } else {
    const addAllIds = (nodes: Permission[]) => {
      nodes.forEach(node => {
        expandedIds.value.add(node.id)
        if (node.children?.length) addAllIds(node.children)
      })
    }
    addAllIds(permissionTree.value)
  }
  isExpandAll.value = !isExpandAll.value
}

const loadPermissions = async () => {
  loading.value = true
  try {
    const response = await getPermissionTree()
    permissionTree.value = response || []
    permissionTree.value.forEach(node => expandedIds.value.add(node.id))
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = (row: FlatPermission | null) => {
  isEdit.value = false
  dialogTitle.value = row ? `新增子权限 (${row.permissionName})` : '新增根权限'
  Object.assign(formData, {
    id: null,
    permissionName: '',
    permissionCode: '',
    permissionDesc: '',
    resourceType: row ? 2 : 1,
    parentId: row ? row.id : null,
    path: '',
    component: '',
    icon: '',
    sortOrder: 0,
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row: FlatPermission) => {
  isEdit.value = true
  dialogTitle.value = '编辑权限'
  Object.assign(formData, {
    id: row.id,
    permissionName: row.permissionName,
    permissionCode: row.permissionCode,
    permissionDesc: row.permissionDesc || '',
    resourceType: row.resourceType,
    parentId: row.parentId,
    path: row.path || '',
    component: row.component || '',
    icon: row.icon || '',
    sortOrder: row.sortOrder || 0,
    status: row.status
  })
  dialogVisible.value = true
}

const handleDelete = async (row: FlatPermission) => {
  try {
    await ElMessageBox.confirm(`确定删除权限"${row.permissionName}"吗?`, '删除确认', { type: 'warning' })
    await deletePermission(row.id)
    ElMessage.success('删除成功')
    loadPermissions()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleStatusChange = async (row: FlatPermission) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await updatePermission(row.id, { isEnabled: newStatus === 1 })
    row.status = newStatus
    ElMessage.success('状态已更新')
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

const handleSubmit = async () => {
  if (!formData.permissionName || !formData.permissionCode) {
    ElMessage.error('请填写必填项')
    return
  }
  try {
    submitLoading.value = true
    const data = {
      permissionName: formData.permissionName,
      permissionCode: formData.permissionCode,
      description: formData.permissionDesc,
      type: String(formData.resourceType),
      parentId: formData.parentId || 0,
      sortOrder: formData.sortOrder,
      isEnabled: formData.status === 1
    }
    if (isEdit.value && formData.id) {
      await updatePermission(formData.id, data)
      ElMessage.success('更新成功')
    } else {
      await createPermission(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadPermissions()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadPermissions()
})
</script>

<style scoped>
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
