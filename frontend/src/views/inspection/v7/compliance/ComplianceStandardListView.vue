<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Pencil, Trash2, ChevronRight, ChevronDown } from 'lucide-vue-next'
import {
  getStandards,
  createStandard,
  updateStandard,
  deleteStandard,
  getClauses,
  createClause,
  updateClause,
  deleteClause,
} from '@/api/insp/compliance'
import type {
  ComplianceStandard,
  CreateStandardRequest,
  UpdateStandardRequest,
  ComplianceClause,
  CreateClauseRequest,
  UpdateClauseRequest,
} from '@/types/insp/compliance'

// ==================== State ====================

const loading = ref(false)
const standards = ref<ComplianceStandard[]>([])

// Standard Dialog
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const dialogTitle = computed(() => (editingId.value ? '编辑标准' : '新建标准'))

const form = ref({
  standardCode: '',
  standardName: '',
  issuingBody: '',
  effectiveDate: '',
  version: '',
  description: '',
})

// Clauses panel
const expandedStandardId = ref<number | null>(null)
const clausesLoading = ref(false)
const clauses = ref<ComplianceClause[]>([])

// Clause Dialog
const clauseDialogVisible = ref(false)
const editingClauseId = ref<number | null>(null)
const clauseDialogTitle = computed(() => (editingClauseId.value ? '编辑条款' : '新建条款'))

const clauseForm = ref({
  clauseNumber: '',
  clauseTitle: '',
  clauseContent: '',
  parentClauseId: null as number | null,
  sortOrder: 0,
})

// ==================== Data Loading ====================

async function loadData() {
  loading.value = true
  try {
    standards.value = await getStandards()
  } catch (e: any) {
    ElMessage.error(e.message || '加载合规标准失败')
  } finally {
    loading.value = false
  }
}

// ==================== Standard CRUD ====================

function openCreate() {
  editingId.value = null
  form.value = {
    standardCode: '',
    standardName: '',
    issuingBody: '',
    effectiveDate: '',
    version: '',
    description: '',
  }
  dialogVisible.value = true
}

function openEdit(std: ComplianceStandard) {
  editingId.value = std.id
  form.value = {
    standardCode: std.standardCode,
    standardName: std.standardName,
    issuingBody: std.issuingBody ?? '',
    effectiveDate: std.effectiveDate ?? '',
    version: std.standardVersion ?? '',
    description: std.description ?? '',
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.standardCode.trim()) {
    ElMessage.warning('请输入标准编号')
    return
  }
  if (!form.value.standardName.trim()) {
    ElMessage.warning('请输入标准名称')
    return
  }
  try {
    if (editingId.value) {
      const req: UpdateStandardRequest = {
        standardName: form.value.standardName,
        issuingBody: form.value.issuingBody || undefined,
        effectiveDate: form.value.effectiveDate || undefined,
        version: form.value.version || undefined,
        description: form.value.description || undefined,
      }
      await updateStandard(editingId.value, req)
      ElMessage.success('更新成功')
    } else {
      const req: CreateStandardRequest = {
        standardCode: form.value.standardCode,
        standardName: form.value.standardName,
        issuingBody: form.value.issuingBody || undefined,
        effectiveDate: form.value.effectiveDate || undefined,
        version: form.value.version || undefined,
        description: form.value.description || undefined,
      }
      await createStandard(req)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleDelete(std: ComplianceStandard) {
  try {
    await ElMessageBox.confirm(`确认删除标准「${std.standardName}」？`, '确认删除', { type: 'warning' })
    await deleteStandard(std.id)
    ElMessage.success('删除成功')
    if (expandedStandardId.value === std.id) {
      expandedStandardId.value = null
      clauses.value = []
    }
    await loadData()
  } catch { /* cancelled */ }
}

// ==================== Clauses ====================

async function toggleClauses(std: ComplianceStandard) {
  if (expandedStandardId.value === std.id) {
    expandedStandardId.value = null
    clauses.value = []
    return
  }
  expandedStandardId.value = std.id
  clausesLoading.value = true
  try {
    clauses.value = await getClauses(std.id)
  } catch (e: any) {
    ElMessage.error(e.message || '加载条款失败')
  } finally {
    clausesLoading.value = false
  }
}

// Flatten tree into indented list for rendering
interface FlatClause {
  clause: ComplianceClause
  depth: number
}

function flattenTree(flatClauses: ComplianceClause[]): FlatClause[] {
  // Build tree first
  const map = new Map<number, ComplianceClause>()
  const roots: ComplianceClause[] = []

  flatClauses.forEach(c => {
    map.set(c.id, { ...c, children: [] })
  })

  flatClauses.forEach(c => {
    const node = map.get(c.id)!
    if (c.parentClauseId && map.has(c.parentClauseId)) {
      map.get(c.parentClauseId)!.children!.push(node)
    } else {
      roots.push(node)
    }
  })

  // DFS flatten
  const result: FlatClause[] = []
  function dfs(nodes: ComplianceClause[], depth: number) {
    for (const node of nodes) {
      result.push({ clause: node, depth })
      if (node.children && node.children.length > 0) {
        dfs(node.children, depth + 1)
      }
    }
  }
  dfs(roots, 0)
  return result
}

const flatClauses = computed(() => flattenTree(clauses.value))

// Flatten clause options for parent selector (exclude self & descendants)
const parentClauseOptions = computed(() => {
  return clauses.value
    .filter(c => c.id !== editingClauseId.value)
    .map(c => ({
      value: c.id,
      label: `${c.clauseNumber} ${c.clauseTitle}`,
    }))
})

function openCreateClause() {
  if (!expandedStandardId.value) return
  editingClauseId.value = null
  clauseForm.value = { clauseNumber: '', clauseTitle: '', clauseContent: '', parentClauseId: null, sortOrder: 0 }
  clauseDialogVisible.value = true
}

function openEditClause(clause: ComplianceClause) {
  editingClauseId.value = clause.id
  clauseForm.value = {
    clauseNumber: clause.clauseNumber,
    clauseTitle: clause.clauseTitle,
    clauseContent: clause.clauseContent ?? '',
    parentClauseId: clause.parentClauseId,
    sortOrder: clause.sortOrder,
  }
  clauseDialogVisible.value = true
}

async function handleClauseSubmit() {
  if (!expandedStandardId.value) return
  if (!clauseForm.value.clauseNumber.trim()) {
    ElMessage.warning('请输入条款编号')
    return
  }
  if (!clauseForm.value.clauseTitle.trim()) {
    ElMessage.warning('请输入条款标题')
    return
  }
  try {
    if (editingClauseId.value) {
      const req: UpdateClauseRequest = {
        clauseNumber: clauseForm.value.clauseNumber,
        clauseTitle: clauseForm.value.clauseTitle,
        clauseContent: clauseForm.value.clauseContent || undefined,
        parentClauseId: clauseForm.value.parentClauseId ?? undefined,
        sortOrder: clauseForm.value.sortOrder,
      }
      await updateClause(expandedStandardId.value, editingClauseId.value, req)
      ElMessage.success('更新成功')
    } else {
      const req: CreateClauseRequest = {
        clauseNumber: clauseForm.value.clauseNumber,
        clauseTitle: clauseForm.value.clauseTitle,
        clauseContent: clauseForm.value.clauseContent || undefined,
        parentClauseId: clauseForm.value.parentClauseId ?? undefined,
        sortOrder: clauseForm.value.sortOrder,
      }
      await createClause(expandedStandardId.value, req)
      ElMessage.success('创建成功')
    }
    clauseDialogVisible.value = false
    clauses.value = await getClauses(expandedStandardId.value)
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleDeleteClause(clause: ComplianceClause) {
  if (!expandedStandardId.value) return
  try {
    await ElMessageBox.confirm(`确认删除条款「${clause.clauseNumber} ${clause.clauseTitle}」？`, '确认删除', { type: 'warning' })
    await deleteClause(expandedStandardId.value, clause.id)
    ElMessage.success('删除成功')
    clauses.value = await getClauses(expandedStandardId.value)
  } catch { /* cancelled */ }
}

// ==================== Lifecycle ====================

onMounted(() => loadData())
</script>

<template>
  <div class="p-5 space-y-4">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">合规标准管理</h2>
      <el-button type="primary" @click="openCreate">
        <Plus class="w-4 h-4 mr-1" />新建标准
      </el-button>
    </div>

    <!-- Standards Table -->
    <el-card shadow="never">
      <el-table :data="standards" v-loading="loading" stripe>
        <el-table-column label="" width="40">
          <template #default="{ row }">
            <el-button link size="small" @click="toggleClauses(row)">
              <component
                :is="expandedStandardId === row.id ? ChevronDown : ChevronRight"
                class="w-4 h-4 text-gray-400"
              />
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="standardCode" label="标准编号" width="160">
          <template #default="{ row }">
            <span class="font-mono text-sm">{{ row.standardCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="standardName" label="标准名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="issuingBody" label="发布机构" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.issuingBody || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="生效日期" width="120">
          <template #default="{ row }">
            <span class="text-sm">{{ row.effectiveDate || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="版本" width="80" align="center">
          <template #default="{ row }">
            <span class="text-sm">{{ row.standardVersion || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center gap-1">
              <el-button link type="primary" size="small" @click="openEdit(row)">
                <Pencil class="w-3.5 h-3.5" />
              </el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">
                <Trash2 class="w-3.5 h-3.5" />
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Clauses Panel (expanded) -->
    <el-card v-if="expandedStandardId" shadow="never" v-loading="clausesLoading">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-medium">
            条款列表 -
            {{ standards.find(s => s.id === expandedStandardId)?.standardName ?? '' }}
          </span>
          <el-button type="primary" size="small" @click="openCreateClause">
            <Plus class="w-3.5 h-3.5 mr-1" />新建条款
          </el-button>
        </div>
      </template>

      <!-- Flat rendered clause tree with indentation -->
      <div v-if="flatClauses.length > 0">
        <div
          v-for="item in flatClauses"
          :key="item.clause.id"
          class="flex items-center py-2 px-2 hover:bg-gray-50 rounded"
          :style="{ paddingLeft: (item.depth * 24 + 8) + 'px' }"
        >
          <div class="flex-1 flex items-center gap-2 min-w-0">
            <span class="font-mono text-sm text-gray-500 flex-shrink-0">{{ item.clause.clauseNumber }}</span>
            <span class="text-sm truncate">{{ item.clause.clauseTitle }}</span>
          </div>
          <div class="flex items-center gap-1 flex-shrink-0 ml-2">
            <el-button link type="primary" size="small" @click="openEditClause(item.clause)">
              <Pencil class="w-3.5 h-3.5" />
            </el-button>
            <el-button link type="danger" size="small" @click="handleDeleteClause(item.clause)">
              <Trash2 class="w-3.5 h-3.5" />
            </el-button>
          </div>
        </div>
      </div>
      <div v-else class="text-center py-8 text-gray-400">暂无条款</div>
    </el-card>

    <!-- ==================== Standard Dialog ==================== -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标准编号" required>
          <el-input v-model="form.standardCode" placeholder="例如：GB/T 28001-2020" />
        </el-form-item>
        <el-form-item label="标准名称" required>
          <el-input v-model="form.standardName" placeholder="输入标准名称" />
        </el-form-item>
        <el-form-item label="发布机构">
          <el-input v-model="form.issuingBody" placeholder="例如：国家标准化管理委员会" />
        </el-form-item>
        <el-form-item label="生效日期">
          <el-date-picker v-model="form.effectiveDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" class="w-full" />
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="form.version" placeholder="例如：v2.0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="标准简要描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- ==================== Clause Dialog ==================== -->
    <el-dialog v-model="clauseDialogVisible" :title="clauseDialogTitle" width="550px">
      <el-form :model="clauseForm" label-width="100px">
        <el-form-item label="条款编号" required>
          <el-input v-model="clauseForm.clauseNumber" placeholder="例如：4.1.2" />
        </el-form-item>
        <el-form-item label="条款标题" required>
          <el-input v-model="clauseForm.clauseTitle" placeholder="输入条款标题" />
        </el-form-item>
        <el-form-item label="条款内容">
          <el-input v-model="clauseForm.clauseContent" type="textarea" :rows="4" placeholder="输入条款详细内容" />
        </el-form-item>
        <el-form-item label="上级条款">
          <el-select v-model="clauseForm.parentClauseId" clearable placeholder="无（顶级条款）" class="w-full">
            <el-option
              v-for="opt in parentClauseOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="clauseForm.sortOrder" :min="0" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="clauseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleClauseSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
