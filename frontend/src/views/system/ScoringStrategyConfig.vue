<template>
  <div class="scoring-strategy-config">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>计分策略配置</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新增策略
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索策略名称或编码"
          clearable
          style="width: 300px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="filterCategory" placeholder="筛选分类" clearable style="width: 150px; margin-left: 10px">
          <el-option label="基础策略" value="basic" />
          <el-option label="等级策略" value="grade" />
          <el-option label="高级策略" value="advanced" />
          <el-option label="时间策略" value="time" />
        </el-select>
      </div>

      <!-- 策略列表 -->
      <el-table :data="filteredStrategies" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="策略名称" min-width="180">
          <template #default="{ row }">
            <div class="strategy-name">
              <span>{{ row.name }}</span>
              <el-tag v-if="row.isSystem" size="small" type="info" style="margin-left: 8px">内置</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="策略编码" width="150" />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag :type="getCategoryType(row.category)" size="small">
              {{ getCategoryLabel(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="formulaDescription" label="公式说明" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <code class="formula-code">{{ row.formulaDescription }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.isEnabled"
              :disabled="row.isSystem"
              @change="handleToggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)" :disabled="row.isSystem">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑计分策略' : '新增计分策略'"
      width="700px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="策略编码" prop="code">
          <el-input v-model="formData.code" placeholder="如: DEDUCTION_BASED" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="策略名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入策略名称" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="formData.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="基础策略" value="basic" />
            <el-option label="等级策略" value="grade" />
            <el-option label="高级策略" value="advanced" />
            <el-option label="时间策略" value="time" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>

        <el-divider content-position="left">公式配置</el-divider>

        <el-form-item label="公式说明" prop="formulaDescription">
          <el-input v-model="formData.formulaDescription" placeholder="如: 总分 = 基础分 - 扣分" />
        </el-form-item>
        <el-form-item label="计算公式" prop="formulaTemplate">
          <el-input
            v-model="formData.formulaTemplate"
            type="textarea"
            :rows="4"
            placeholder="JavaScript公式，如: ctx.baseScore - sum(ctx.deductions)"
            class="code-input"
          />
        </el-form-item>

        <el-divider content-position="left">参数配置</el-divider>

        <el-form-item label="默认参数">
          <div class="params-editor">
            <div v-for="(value, key) in formData.defaultParameters" :key="key" class="param-row">
              <el-input v-model="paramKeys[key]" placeholder="参数名" style="width: 120px" disabled />
              <el-input-number v-model="formData.defaultParameters[key]" :controls="false" style="width: 100px" />
              <el-button link type="danger" @click="removeParam(key as string)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button link type="primary" @click="addParam">+ 添加参数</el-button>
          </div>
        </el-form-item>

        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加参数对话框 -->
    <el-dialog v-model="paramDialogVisible" title="添加参数" width="400px">
      <el-form :model="newParam" label-width="80px">
        <el-form-item label="参数名">
          <el-input v-model="newParam.key" placeholder="如: baseScore" />
        </el-form-item>
        <el-form-item label="默认值">
          <el-input-number v-model="newParam.value" :controls="false" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="paramDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddParam">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { ScoringStrategy, CreateScoringStrategyCommand, UpdateScoringStrategyCommand } from '@/types/scoring'
import {
  getAllStrategies,
  createStrategy,
  updateStrategy,
  deleteStrategy,
  toggleStrategy
} from '@/api/scoring'

// 状态
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const paramDialogVisible = ref(false)
const isEdit = ref(false)
const searchKeyword = ref('')
const filterCategory = ref('')

const strategies = ref<ScoringStrategy[]>([])
const paramKeys = ref<Record<string, string>>({})
const newParam = reactive({ key: '', value: 0 })

const formRef = ref<FormInstance>()
const formData = ref<Partial<CreateScoringStrategyCommand>>({
  code: '',
  name: '',
  category: 'basic',
  description: '',
  formulaTemplate: '',
  formulaDescription: '',
  defaultParameters: {},
  parametersSchema: {},
  sortOrder: 0
})

const formRules: FormRules = {
  code: [
    { required: true, message: '请输入策略编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]*$/, message: '编码必须大写字母开头', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const categoryLabels: Record<string, string> = {
  basic: '基础策略',
  grade: '等级策略',
  advanced: '高级策略',
  time: '时间策略'
}

const categoryTypes: Record<string, string> = {
  basic: 'primary',
  grade: 'success',
  advanced: 'warning',
  time: 'info'
}

// 计算属性
const filteredStrategies = computed(() => {
  let result = strategies.value
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(s =>
      s.name.toLowerCase().includes(keyword) ||
      s.code.toLowerCase().includes(keyword)
    )
  }
  if (filterCategory.value) {
    result = result.filter(s => s.category === filterCategory.value)
  }
  return result
})

const getCategoryLabel = (category: string) => categoryLabels[category] || category
const getCategoryType = (category: string) => categoryTypes[category] || 'info'

// 方法
async function loadStrategies() {
  loading.value = true
  try {
    const res = await getAllStrategies()
    strategies.value = res?.data?.data || res?.data || res || []
  } catch (error) {
    ElMessage.error('加载策略失败')
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  isEdit.value = false
  formData.value = {
    code: '',
    name: '',
    category: 'basic',
    description: '',
    formulaTemplate: '',
    formulaDescription: '',
    defaultParameters: {},
    parametersSchema: {},
    sortOrder: 0
  }
  paramKeys.value = {}
  dialogVisible.value = true
}

function handleEdit(row: ScoringStrategy) {
  isEdit.value = true
  formData.value = {
    code: row.code,
    name: row.name,
    category: row.category,
    description: row.description || '',
    formulaTemplate: row.formulaTemplate || '',
    formulaDescription: row.formulaDescription || '',
    defaultParameters: { ...(row.defaultParameters || {}) },
    parametersSchema: { ...(row.parametersSchema || {}) },
    sortOrder: row.sortOrder
  }
  // 初始化参数键
  paramKeys.value = {}
  for (const key of Object.keys(formData.value.defaultParameters || {})) {
    paramKeys.value[key] = key
  }
  ;(formData.value as any)._id = row.id
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      const id = (formData.value as any)._id
      await updateStrategy(id, formData.value as UpdateScoringStrategyCommand)
      ElMessage.success('更新成功')
    } else {
      await createStrategy(formData.value as CreateScoringStrategyCommand)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadStrategies()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: ScoringStrategy) {
  try {
    await ElMessageBox.confirm(`确定要删除策略"${row.name}"吗？`, '确认删除', { type: 'warning' })
    await deleteStrategy(row.id)
    ElMessage.success('删除成功')
    await loadStrategies()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function handleToggleStatus(row: ScoringStrategy) {
  try {
    await toggleStrategy(row.id, row.isEnabled)
    ElMessage.success(row.isEnabled ? '已启用' : '已禁用')
  } catch (error: any) {
    row.isEnabled = !row.isEnabled
    ElMessage.error(error.message || '操作失败')
  }
}

function addParam() {
  newParam.key = ''
  newParam.value = 0
  paramDialogVisible.value = true
}

function confirmAddParam() {
  if (!newParam.key) {
    ElMessage.warning('请输入参数名')
    return
  }
  if (!formData.value.defaultParameters) {
    formData.value.defaultParameters = {}
  }
  formData.value.defaultParameters[newParam.key] = newParam.value
  paramKeys.value[newParam.key] = newParam.key
  paramDialogVisible.value = false
}

function removeParam(key: string) {
  if (formData.value.defaultParameters) {
    delete formData.value.defaultParameters[key]
  }
  delete paramKeys.value[key]
}

onMounted(() => {
  loadStrategies()
})
</script>

<style scoped>
.scoring-strategy-config {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.strategy-name {
  display: flex;
  align-items: center;
}

.formula-code {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 12px;
  color: #5b5fc7;
  background: #f5f5ff;
  padding: 2px 6px;
  border-radius: 4px;
}

.code-input :deep(textarea) {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 13px;
}

.params-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.param-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
