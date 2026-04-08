<template>
  <div class="org-page">
    <!-- Left Sidebar -->
    <div class="org-sidebar-wrap">
      <OrgSidebar
        :tree-data="treeData"
        :selected-id="selectedNodeId"
        @select="handleSelectNode"
        @add-root="handleAddRoot"
        @add-child="handleAddChild"
      />
    </div>

    <!-- Right Content Panel -->
    <div class="org-main">
      <!-- Top Header Bar -->
      <header class="tm-header">
        <div>
          <h1 class="tm-title">组织架构</h1>
          <div class="tm-stats" style="margin-top: 4px;">管理学校的组织与院系结构</div>
        </div>
        <router-link to="/system/org-types" class="tm-btn tm-btn-secondary">
          <Settings style="width: 16px; height: 16px;" />
          管理类型
        </router-link>
      </header>

      <!-- Stat Bar -->
      <div class="tm-stats-bar">
        <span class="tm-stats">总数 <b>{{ stats.total }}</b></span>
        <i class="sep" />
        <span class="tm-stats"><em class="dot dot-green" /> 正常 <b>{{ stats.active }}</b></span>
        <i class="sep" />
        <span class="tm-stats"><em class="dot dot-gray" /> 冻结/撤销 <b>{{ stats.inactive }}</b></span>
      </div>

      <!-- Main Content Area -->
      <div class="org-content">
        <!-- Loading State -->
        <div v-if="loading" class="org-loading">
          <div class="org-spinner tm-spin"></div>
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
// ClassForm removed - class management done via DynamicForm + SPI plugins
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
// classFormVisible/editingClassId removed - class editing via DynamicForm
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
    // All types use same generic form now
  }
  editingDept.value = node
  parentDept.value = null
  formVisible.value = true
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

<style scoped>
@import '@/styles/teaching-ui.css';

.org-page {
  display: flex;
  height: 100%;
  background: #f8f9fb;
  font-family: 'DM Sans', sans-serif;
}
.org-sidebar-wrap {
  display: flex;
  flex-direction: column;
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid #e5e7eb;
  background: #fff;
}
.org-main {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}
.tm-stats-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 24px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
  font-size: 12.5px;
  color: #6b7280;
}
.tm-stats-bar .sep { display: block; width: 1px; height: 10px; background: #d1d5db; }
.tm-stats-bar .dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 2px; vertical-align: middle; }
.tm-stats-bar .dot-green { background: #10b981; }
.tm-stats-bar .dot-gray { background: #9ca3af; }
.tm-stats-bar b { font-weight: 600; color: #111827; }
.org-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px 24px;
}
.org-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}
.org-spinner {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid #2563eb;
  border-top-color: transparent;
}
</style>
