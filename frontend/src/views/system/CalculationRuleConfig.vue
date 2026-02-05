<template>
  <div class="calculation-rule-config">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>计算规则配置</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新增规则
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索规则名称或编码"
          clearable
          style="width: 300px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="filterType" placeholder="筛选类型" clearable style="width: 150px; margin-left: 10px">
          <el-option label="边界规则" value="boundary" />
          <el-option label="修正规则" value="modifier" />
          <el-option label="奖惩规则" value="bonus" />
          <el-option label="验证规则" value="validation" />
        </el-select>
      </div>

      <!-- 规则列表 -->
      <el-table :data="filteredRules" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="规则名称" min-width="180">
          <template #default="{ row }">
            <div class="rule-name">
              <span>{{ row.name }}</span>
              <el-tag v-if="row.isSystem" size="small" type="info" style="margin-left: 8px">内置</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="规则编码" width="150" />
        <el-table-column prop="ruleType" label="规则类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getRuleTypeTag(row.ruleType)" size="small">
              {{ getRuleTypeLabel(row.ruleType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="默认参数" width="180">
          <template #default="{ row }">
            <div class="params-preview" v-if="row.defaultParameters && Object.keys(row.defaultParameters).length">
              <span v-for="(val, key) in row.defaultParameters" :key="key" class="param-chip">
                {{ key }}: {{ val }}
              </span>
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)" size="small">{{ row.priority }}</el-tag>
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
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="info" @click="handlePriority(row)">优先级</el-button>
            <el-button link type="danger" @click="handleDelete(row)" :disabled="row.isSystem">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑计算规则' : '新增计算规则'"
      width="700px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="规则编码" prop="code">
          <el-input v-model="formData.code" placeholder="如: SCORE_CAP" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="formData.ruleType" placeholder="选择规则类型" style="width: 100%">
            <el-option label="边界规则（封顶/保底）" value="boundary" />
            <el-option label="修正规则（系数/修正值）" value="modifier" />
            <el-option label="奖惩规则（加/扣分）" value="bonus" />
            <el-option label="验证规则（合法性检查）" value="validation" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>

        <el-divider content-position="left">规则逻辑</el-divider>

        <el-form-item label="条件表达式" prop="conditionExpression">
          <el-input
            v-model="formData.conditionExpression"
            type="textarea"
            :rows="2"
            placeholder="JavaScript条件，如: ctx.score > ctx.maxScore"
            class="code-input"
          />
          <div class="form-tip">返回 true 时执行规则动作</div>
        </el-form-item>
        <el-form-item label="动作表达式" prop="actionExpression">
          <el-input
            v-model="formData.actionExpression"
            type="textarea"
            :rows="2"
            placeholder="JavaScript动作，如: ctx.score = ctx.maxScore"
            class="code-input"
          />
          <div class="form-tip">条件满足时执行的操作</div>
        </el-form-item>

        <el-divider content-position="left">参数配置</el-divider>

        <el-form-item label="默认参数">
          <div class="params-editor">
            <div v-for="(value, key) in formData.defaultParameters" :key="key" class="param-row">
              <el-input :model-value="key" disabled style="width: 120px" />
              <el-input-number v-model="formData.defaultParameters[key]" :controls="false" style="width: 100px" />
              <el-button link type="danger" @click="removeParam(key as string)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button link type="primary" @click="addParam">+ 添加参数</el-button>
          </div>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-input-number v-model="formData.priority" :min="1" :max="100" />
              <div class="form-tip">数值越小优先级越高</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号" prop="sortOrder">
              <el-input-number v-model="formData.sortOrder" :min="0" :max="9999" />
            </el-form-item>
          </el-col>
        </el-row>
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
          <el-input v-model="newParam.key" placeholder="如: maxScore" />
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

    <!-- 调整优先级对话框 -->
    <el-dialog v-model="priorityDialogVisible" title="调整优先级" width="400px">
      <el-form label-width="80px">
        <el-form-item label="当前规则">
          <strong>{{ currentRule?.name }}</strong>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="newPriority" :min="1" :max="100" />
          <div class="form-tip">数值越小优先级越高，1为最高优先级</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="priorityDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPriority" :loading="prioritySubmitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { CalculationRule, CreateCalculationRuleCommand, UpdateCalculationRuleCommand } from '@/types/scoring'
import {
  getAllRules,
  createRule,
  updateRule,
  deleteRule,
  toggleRule,
  updateRulePriority
} from '@/api/scoring'

// 状态
const loading = ref(false)
const submitting = ref(false)
const prioritySubmitting = ref(false)
const dialogVisible = ref(false)
const paramDialogVisible = ref(false)
const priorityDialogVisible = ref(false)
const isEdit = ref(false)
const searchKeyword = ref('')
const filterType = ref('')

const rules = ref<CalculationRule[]>([])
const currentRule = ref<CalculationRule | null>(null)
const newPriority = ref(10)
const newParam = reactive({ key: '', value: 0 })

const formRef = ref<FormInstance>()
const formData = ref<Partial<CreateCalculationRuleCommand>>({
  code: '',
  name: '',
  ruleType: 'boundary',
  description: '',
  conditionExpression: '',
  actionExpression: '',
  defaultParameters: {},
  parametersSchema: {},
  priority: 10,
  sortOrder: 0
})

const formRules: FormRules = {
  code: [
    { required: true, message: '请输入规则编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]*$/, message: '编码必须大写字母开头', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }]
}

const ruleTypeLabels: Record<string, string> = {
  boundary: '边界',
  modifier: '修正',
  bonus: '奖惩',
  validation: '验证'
}

const ruleTypeTags: Record<string, string> = {
  boundary: 'warning',
  modifier: 'primary',
  bonus: 'success',
  validation: 'info'
}

// 计算属性
const filteredRules = computed(() => {
  let result = rules.value
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(r =>
      r.name.toLowerCase().includes(keyword) ||
      r.code.toLowerCase().includes(keyword)
    )
  }
  if (filterType.value) {
    result = result.filter(r => r.ruleType === filterType.value)
  }
  return result.sort((a, b) => a.priority - b.priority)
})

const getRuleTypeLabel = (type: string) => ruleTypeLabels[type] || type
const getRuleTypeTag = (type: string) => ruleTypeTags[type] || 'info'
const getPriorityType = (priority: number) => {
  if (priority <= 3) return 'danger'
  if (priority <= 5) return 'warning'
  if (priority <= 10) return 'primary'
  return 'info'
}

// 方法
async function loadRules() {
  loading.value = true
  try {
    const res = await getAllRules()
    rules.value = res?.data?.data || res?.data || res || []
  } catch (error) {
    ElMessage.error('加载规则失败')
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  isEdit.value = false
  formData.value = {
    code: '',
    name: '',
    ruleType: 'boundary',
    description: '',
    conditionExpression: '',
    actionExpression: '',
    defaultParameters: {},
    parametersSchema: {},
    priority: 10,
    sortOrder: 0
  }
  dialogVisible.value = true
}

function handleEdit(row: CalculationRule) {
  isEdit.value = true
  formData.value = {
    code: row.code,
    name: row.name,
    ruleType: row.ruleType,
    description: row.description || '',
    conditionExpression: row.conditionExpression || '',
    actionExpression: row.actionExpression || '',
    defaultParameters: { ...(row.defaultParameters || {}) },
    parametersSchema: { ...(row.parametersSchema || {}) },
    priority: row.priority,
    sortOrder: row.sortOrder
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
      await updateRule(id, formData.value as UpdateCalculationRuleCommand)
      ElMessage.success('更新成功')
    } else {
      await createRule(formData.value as CreateCalculationRuleCommand)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadRules()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: CalculationRule) {
  try {
    await ElMessageBox.confirm(`确定要删除规则"${row.name}"吗？`, '确认删除', { type: 'warning' })
    await deleteRule(row.id)
    ElMessage.success('删除成功')
    await loadRules()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function handleToggleStatus(row: CalculationRule) {
  try {
    await toggleRule(row.id, row.isEnabled)
    ElMessage.success(row.isEnabled ? '已启用' : '已禁用')
  } catch (error: any) {
    row.isEnabled = !row.isEnabled
    ElMessage.error(error.message || '操作失败')
  }
}

function handlePriority(row: CalculationRule) {
  currentRule.value = row
  newPriority.value = row.priority
  priorityDialogVisible.value = true
}

async function confirmPriority() {
  if (!currentRule.value) return

  prioritySubmitting.value = true
  try {
    await updateRulePriority(currentRule.value.id, newPriority.value)
    ElMessage.success('优先级已更新')
    priorityDialogVisible.value = false
    await loadRules()
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    prioritySubmitting.value = false
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
  paramDialogVisible.value = false
}

function removeParam(key: string) {
  if (formData.value.defaultParameters) {
    delete formData.value.defaultParameters[key]
  }
}

onMounted(() => {
  loadRules()
})
</script>

<style scoped>
.calculation-rule-config {
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

.rule-name {
  display: flex;
  align-items: center;
}

.params-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.param-chip {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 11px;
  color: #5b5fc7;
  background: #f5f5ff;
  padding: 2px 6px;
  border-radius: 4px;
}

.text-muted {
  color: #999;
}

.code-input :deep(textarea) {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 13px;
}

.form-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
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
