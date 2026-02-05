<template>
  <div class="p-6 bg-gray-50 min-h-full">
    <!-- 页面标题 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">组织架构</h1>
      <p class="mt-1 text-sm text-gray-500">管理学校的组织与院系结构</p>
    </div>

    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
      <StatCard title="组织总数" :value="stats.total" :icon="Building2" subtitle="全部组织单元" color="blue" />
      <StatCard title="已启用" :value="stats.enabled" :icon="CheckCircle" subtitle="正常运行" color="emerald" />
      <StatCard title="已禁用" :value="stats.disabled" :icon="XCircle" subtitle="暂停使用" color="orange" />
      <StatCard title="院系数" :value="stats.academic" :icon="GraduationCap" subtitle="教学组织" color="purple" />
    </div>

    <!-- 搜索和操作区域 -->
    <div class="mb-4 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-end gap-3">
        <div class="w-48">
          <label class="mb-1 block text-xs font-medium text-gray-600">关键词</label>
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="组织名称/编码"
            class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="w-28">
          <label class="mb-1 block text-xs font-medium text-gray-600">状态</label>
          <select
            v-model="statusFilter"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option value="">全部</option>
            <option value="enabled">启用</option>
            <option value="disabled">禁用</option>
          </select>
        </div>
        <button
          class="h-9 rounded-md bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          @click="handleSearch"
        >
          搜索
        </button>
        <button
          class="h-9 rounded-md border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
          @click="handleReset"
        >
          重置
        </button>
        <div class="flex-1"></div>
        <button
          class="h-9 rounded-md bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          @click="handleAdd"
        >
          <Plus class="mr-1.5 inline-block h-4 w-4" />
          新增组织
        </button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium text-gray-900">组织列表</span>
          <span class="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-600">{{ stats.total }} 个</span>
        </div>
      </div>

      <!-- Loading 状态 -->
      <div v-if="loading" class="flex items-center justify-center py-16">
        <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
      </div>

      <!-- 表格 -->
      <div v-else class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="border-b border-gray-200 bg-gray-50">
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">组织名称</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">组织编码</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">类别</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">负责人</th>
              <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">状态</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <DeptTableRow
              v-for="node in filteredData"
              :key="node.id"
              :dept="node"
              :level="0"
              :expanded-keys="expandedKeys"
              @toggle="handleToggleExpand"
              @add-child="handleAddChild"
              @edit="handleEdit"
              @toggle-status="handleToggleStatus"
              @delete="handleDelete"
            />
          </tbody>
        </table>

        <!-- 空状态 -->
        <div v-if="filteredData.length === 0" class="flex flex-col items-center justify-center py-16">
          <Building2 class="h-12 w-12 text-gray-300" />
          <p class="mt-3 text-sm text-gray-500">暂无组织数据</p>
          <button
            class="mt-4 rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            @click="handleAdd"
          >
            创建组织
          </button>
        </div>
      </div>
    </div>

    <!-- 表单弹窗 -->
    <DepartmentForm
      v-model:visible="formVisible"
      :department="editingDept"
      :parent-department="parentDept"
      :all-departments="deptTree"
      @success="loadData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Building2, CheckCircle, XCircle, GraduationCap, Plus } from 'lucide-vue-next'
import StatCard from '@/components/design-system/cards/StatCard.vue'
import DepartmentForm from './components/DepartmentForm.vue'
import DeptTableRow from './components/DeptTableRow.vue'
import {
  getDepartmentTree,
  deleteDepartment,
  updateDepartmentStatus,
  type DepartmentResponse
} from '@/api/organization'

const loading = ref(false)
const deptTree = ref<DepartmentResponse[]>([])
const searchKeyword = ref('')
const statusFilter = ref('')
const formVisible = ref(false)
const editingDept = ref<DepartmentResponse | null>(null)
const parentDept = ref<DepartmentResponse | null>(null)
const expandedKeys = ref<Set<number>>(new Set())

// 统计
const countDepts = (nodes: DepartmentResponse[]): { total: number; enabled: number; disabled: number; academic: number } => {
  let total = 0, enabled = 0, disabled = 0, academic = 0
  const traverse = (list: DepartmentResponse[]) => {
    for (const node of list) {
      total++
      if (node.isEnabled) enabled++
      else disabled++
      if (!['处', '办', '中心', '馆', '室'].some(k => node.unitName.includes(k)) && node.parentId) {
        academic++
      }
      if (node.children) traverse(node.children)
    }
  }
  traverse(nodes)
  return { total, enabled, disabled, academic }
}

const stats = computed(() => countDepts(deptTree.value))

// 过滤数据
const filteredData = computed(() => {
  const filterTree = (nodes: DepartmentResponse[]): DepartmentResponse[] => {
    return nodes.reduce((acc: DepartmentResponse[], node) => {
      const matchKeyword = !searchKeyword.value ||
        node.unitName.includes(searchKeyword.value) ||
        node.unitCode.includes(searchKeyword.value)
      const matchStatus = !statusFilter.value ||
        (statusFilter.value === 'enabled' && node.isEnabled) ||
        (statusFilter.value === 'disabled' && !node.isEnabled)

      const filteredChildren = node.children ? filterTree(node.children) : []

      if ((matchKeyword && matchStatus) || filteredChildren.length > 0) {
        acc.push({
          ...node,
          children: filteredChildren.length > 0 ? filteredChildren : undefined
        })
      }
      return acc
    }, [])
  }
  return filterTree(deptTree.value)
})

// 收集所有节点ID用于默认展开
const collectAllIds = (nodes: DepartmentResponse[]): number[] => {
  const ids: number[] = []
  const traverse = (list: DepartmentResponse[]) => {
    for (const node of list) {
      ids.push(node.id)
      if (node.children) traverse(node.children)
    }
  }
  traverse(nodes)
  return ids
}

const loadData = async () => {
  loading.value = true
  try {
    deptTree.value = await getDepartmentTree() || []
    // 默认展开所有节点
    expandedKeys.value = new Set(collectAllIds(deptTree.value))
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleToggleExpand = (id: number) => {
  if (expandedKeys.value.has(id)) {
    expandedKeys.value.delete(id)
  } else {
    expandedKeys.value.add(id)
  }
  expandedKeys.value = new Set(expandedKeys.value)
}

const handleSearch = () => {
  // 搜索时数据已通过 computed 自动过滤
}

const handleReset = () => {
  searchKeyword.value = ''
  statusFilter.value = ''
}

const handleAdd = () => {
  editingDept.value = null
  parentDept.value = null
  formVisible.value = true
}

const handleAddChild = (parent: DepartmentResponse) => {
  editingDept.value = null
  parentDept.value = parent
  formVisible.value = true
}

const handleEdit = (dept: DepartmentResponse) => {
  editingDept.value = dept
  parentDept.value = null
  formVisible.value = true
}

// 递归查找并更新节点
const updateNodeStatus = (nodes: DepartmentResponse[], id: number, isEnabled: boolean): boolean => {
  for (const node of nodes) {
    if (node.id === id) {
      node.isEnabled = isEnabled
      return true
    }
    if (node.children && updateNodeStatus(node.children, id, isEnabled)) {
      return true
    }
  }
  return false
}

const handleToggleStatus = async (dept: DepartmentResponse) => {
  try {
    const newStatus = !dept.isEnabled
    await updateDepartmentStatus(dept.id, newStatus ? 1 : 0)
    // 更新源数据
    updateNodeStatus(deptTree.value, dept.id, newStatus)
    // 触发响应式更新
    deptTree.value = [...deptTree.value]
    ElMessage.success(newStatus ? '已启用' : '已禁用')
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (dept: DepartmentResponse) => {
  try {
    await ElMessageBox.confirm(
      dept.children?.length
        ? `确定删除"${dept.unitName}"及其子组织吗？`
        : `确定删除"${dept.unitName}"吗？`,
      '提示',
      { type: 'warning' }
    )
    await deleteDepartment(dept.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

onMounted(loadData)
</script>
