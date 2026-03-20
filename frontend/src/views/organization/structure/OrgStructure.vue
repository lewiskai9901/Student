<template>
  <div class="flex h-full bg-gray-50">
    <!-- Left Sidebar -->
    <div class="flex w-[280px] flex-shrink-0 flex-col border-r border-gray-200 bg-white">
      <OrgSidebar
        :tree-data="treeData"
        :selected-id="selectedNodeId"
        @select="handleSelectNode"
        @add-root="handleAddRoot"
        @add-child="handleAddChild"
      />
    </div>

    <!-- Right Content Panel -->
    <div class="flex flex-1 flex-col overflow-hidden">
      <!-- Top Header Bar -->
      <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
        <div>
          <h1 class="text-lg font-semibold text-gray-900">组织架构</h1>
          <p class="mt-0.5 text-sm text-gray-500">管理学校的组织与院系结构</p>
        </div>
        <router-link
          to="/system/org-types"
          class="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
        >
          <Settings class="h-4 w-4" />
          管理类型
        </router-link>
      </div>

      <!-- Stat Bar -->
      <div class="flex items-center gap-5 border-b border-gray-200 bg-white px-6 py-2.5">
        <div class="flex items-center gap-1.5">
          <div class="flex h-6 w-6 items-center justify-center rounded bg-blue-50">
            <Building2 class="h-3.5 w-3.5 text-blue-600" />
          </div>
          <span class="text-sm text-gray-500">总数</span>
          <span class="text-sm font-semibold text-gray-900">{{ stats.total }}</span>
        </div>
        <div class="h-3 w-px bg-gray-200" />
        <div class="flex items-center gap-1.5">
          <div class="flex h-6 w-6 items-center justify-center rounded bg-emerald-50">
            <CheckCircle class="h-3.5 w-3.5 text-emerald-600" />
          </div>
          <span class="text-sm text-gray-500">正常</span>
          <span class="text-sm font-semibold text-gray-900">{{ stats.active }}</span>
        </div>
        <div class="h-3 w-px bg-gray-200" />
        <div class="flex items-center gap-1.5">
          <div class="flex h-6 w-6 items-center justify-center rounded bg-orange-50">
            <XCircle class="h-3.5 w-3.5 text-orange-600" />
          </div>
          <span class="text-sm text-gray-500">冻结/撤销</span>
          <span class="text-sm font-semibold text-gray-900">{{ stats.inactive }}</span>
        </div>
      </div>

      <!-- Main Content Area -->
      <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
        <!-- Loading State -->
        <div v-if="loading" class="flex items-center justify-center py-20">
          <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
        </div>

        <!-- Detail Panel (when node selected) -->
        <OrgDetailPanel
          v-else-if="selectedNode"
          :node="selectedNode"
          :tree-data="treeData"
          @refresh="loadData"
          @add-child="handleAddChild"
          @edit="handleEdit"
          @delete="handleDelete"
          @toggle-status="handleToggleStatus"
          @merge="handleMerge"
          @split="handleSplit"
          @dissolve="handleDissolve"
        />

        <!-- Overview (when no node selected) -->
        <OrgOverview
          v-else
          :tree-data="treeData"
        />
      </div>
    </div>

    <!-- Form Dialog (OrgUnit) -->
    <OrgUnitForm
      v-model:visible="formVisible"
      :department="editingDept"
      :parent-department="parentDept"
      :all-departments="flattenedDepartments"
      @success="handleFormSuccess"
    />

    <!-- Edit Class Dialog -->
    <el-dialog
      v-model="classFormVisible"
      :title="editingClassId ? '编辑班级' : '新建班级'"
      width="640px"
      :close-on-click-modal="false"
    >
      <ClassForm
        v-if="classFormVisible"
        :mode="editingClassId ? 'edit' : 'add'"
        :class-id="editingClassId"
        @success="handleClassFormSuccess"
        @close="classFormVisible = false"
      />
    </el-dialog>

    <!-- Merge Dialog -->
    <MergeOrgDialog
      v-model:visible="mergeVisible"
      :source="mergingNode"
      :all-departments="treeData"
      @success="handleLifecycleSuccess"
    />

    <!-- Split Dialog -->
    <SplitOrgDialog
      v-model:visible="splitVisible"
      :source="splittingNode"
      @success="handleLifecycleSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Building2, CheckCircle, XCircle, Settings } from 'lucide-vue-next'
import OrgSidebar from './components/OrgSidebar.vue'
import OrgDetailPanel from './components/OrgDetailPanel.vue'
import OrgOverview from './components/OrgOverview.vue'
import OrgUnitForm from './components/OrgUnitForm.vue'
import MergeOrgDialog from './components/MergeOrgDialog.vue'
import SplitOrgDialog from './components/SplitOrgDialog.vue'
import ClassForm from '@/components/class/ClassForm.vue'
import {
  getDepartmentTree,
  freezeOrgUnit,
  unfreezeOrgUnit,
  dissolveOrgUnit,
  deleteOrgUnit,
  type DepartmentResponse
} from '@/api/organization'

const loading = ref(false)
const treeData = ref<DepartmentResponse[]>([])
const selectedNodeId = ref<number | null>(null)
const formVisible = ref(false)
const editingDept = ref<DepartmentResponse | null>(null)
const parentDept = ref<DepartmentResponse | null>(null)
const classFormVisible = ref(false)
const editingClassId = ref<number | null>(null)
const mergeVisible = ref(false)
const mergingNode = ref<DepartmentResponse | null>(null)
const splitVisible = ref(false)
const splittingNode = ref<DepartmentResponse | null>(null)

// Flatten tree for form parent selector
const flattenedDepartments = computed(() => {
  return treeData.value
})

// Find selected node in tree
const findNode = (nodes: DepartmentResponse[], id: number): DepartmentResponse | null => {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children) {
      const found = findNode(node.children, id)
      if (found) return found
    }
  }
  return null
}

const selectedNode = computed(() => {
  if (!selectedNodeId.value) return null
  return findNode(treeData.value, selectedNodeId.value)
})

// Count statistics
const countNodes = (nodes: DepartmentResponse[]): {
  total: number; active: number; inactive: number
} => {
  let total = 0
  let active = 0
  let inactive = 0
  const traverse = (list: DepartmentResponse[]) => {
    for (const node of list) {
      total++
      if (node.status === 'ACTIVE') active++
      else inactive++
      if (node.children) traverse(node.children)
    }
  }
  traverse(nodes)
  return { total, active, inactive }
}

const stats = computed(() => countNodes(treeData.value))

// Load data
const loadData = async () => {
  loading.value = true
  try {
    treeData.value = await getDepartmentTree() || []
  } catch {
    ElMessage.error('加载组织架构失败')
  } finally {
    loading.value = false
  }
}

// Event handlers
const handleSelectNode = (node: DepartmentResponse) => {
  selectedNodeId.value = node.id
}

const handleAddRoot = () => {
  editingDept.value = null
  parentDept.value = null
  formVisible.value = true
}

const handleAddChild = (node: DepartmentResponse) => {
  editingDept.value = null
  parentDept.value = node
  formVisible.value = true
}

const handleEdit = (node: DepartmentResponse) => {
  if (node.category === 'GROUP') {
    editingClassId.value = node.id
    classFormVisible.value = true
  } else {
    editingDept.value = node
    parentDept.value = null
    formVisible.value = true
  }
}

const handleClassFormSuccess = () => {
  classFormVisible.value = false
  editingClassId.value = null
  loadData()
}

const handleDelete = async (node: DepartmentResponse) => {
  try {
    await ElMessageBox.confirm(
      node.children?.length
        ? `确定删除"${node.unitName}"及其所有子组织吗？此操作不可恢复。`
        : `确定删除"${node.unitName}"吗？此操作不可恢复。`,
      '确认删除',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await deleteOrgUnit(node.id)
    ElMessage.success('删除成功')
    if (selectedNodeId.value === node.id) {
      selectedNodeId.value = null
    }
    await loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

const handleToggleStatus = async (node: DepartmentResponse) => {
  try {
    if (node.status === 'ACTIVE') {
      await freezeOrgUnit(node.id)
      ElMessage.success('已冻结')
    } else if (node.status === 'FROZEN') {
      await unfreezeOrgUnit(node.id)
      ElMessage.success('已解冻')
    }
    await loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleMerge = (node: DepartmentResponse) => {
  mergingNode.value = node
  mergeVisible.value = true
}

const handleSplit = (node: DepartmentResponse) => {
  splittingNode.value = node
  splitVisible.value = true
}

const handleDissolve = async (node: DepartmentResponse) => {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      `确定要解散「${node.unitName}」吗？此操作不可撤销。`,
      '解散组织',
      {
        confirmButtonText: '确认解散',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入解散原因',
        inputValidator: (val) => !!val?.trim() || '请输入解散原因',
        type: 'warning'
      }
    )
    await dissolveOrgUnit(node.id, reason!)
    ElMessage.success('已解散')
    await loadData()
  } catch (e: any) {
    if (e !== 'cancel' && e?.action !== 'cancel') {
      ElMessage.error(e.message || '操作失败')
    }
  }
}

const handleLifecycleSuccess = () => {
  selectedNodeId.value = null
  loadData()
}

const handleFormSuccess = async (createdId?: number) => {
  await loadData()
  if (createdId) {
    selectedNodeId.value = createdId
  }
}

onMounted(loadData)
</script>
