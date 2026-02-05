<template>
  <div class="input-type-config">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>打分方式配置</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新增打分方式
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索名称或编码"
          clearable
          style="width: 300px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="filterCategory" placeholder="筛选分类" clearable style="width: 150px; margin-left: 10px">
          <el-option label="基础方式" value="basic" />
          <el-option label="扩展方式" value="extended" />
        </el-select>
      </div>

      <!-- 打分方式列表 -->
      <el-table :data="filteredInputTypes" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="名称" min-width="150">
          <template #default="{ row }">
            <div class="type-name">
              <el-icon :size="18" class="type-icon" :style="{ color: getComponentColor(row.componentType) }">
                <component :is="getComponentIcon(row.componentType)" />
              </el-icon>
              <span>{{ row.name }}</span>
              <el-tag v-if="row.isSystem" size="small" type="info" style="margin-left: 8px">内置</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="编码" width="140" />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag :type="row.category === 'basic' ? 'primary' : 'success'" size="small">
              {{ row.category === 'basic' ? '基础' : '扩展' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="componentType" label="组件类型" width="120">
          <template #default="{ row }">
            <code class="component-code">{{ row.componentType }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="组件配置" width="150">
          <template #default="{ row }">
            <el-popover placement="left" :width="300" trigger="hover" v-if="row.componentConfig">
              <template #reference>
                <el-button link type="primary" size="small">查看配置</el-button>
              </template>
              <pre class="config-preview">{{ JSON.stringify(row.componentConfig, null, 2) }}</pre>
            </el-popover>
            <span v-else class="text-muted">-</span>
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
      :title="isEdit ? '编辑打分方式' : '新增打分方式'"
      width="650px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="formData.code" placeholder="如: NUMBER_INPUT" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-radio-group v-model="formData.category">
            <el-radio value="basic">基础方式</el-radio>
            <el-radio value="extended">扩展方式</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="组件类型" prop="componentType">
          <el-select v-model="formData.componentType" placeholder="选择组件类型" style="width: 100%" @change="handleComponentTypeChange">
            <el-option label="数值输入 (number)" value="number" />
            <el-option label="下拉选择 (select)" value="select" />
            <el-option label="复选框 (checkbox)" value="checkbox" />
            <el-option label="滑块 (slider)" value="slider" />
            <el-option label="星级评分 (star)" value="star" />
            <el-option label="等级选择 (grade)" value="grade" />
            <el-option label="文本输入 (textarea)" value="textarea" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>

        <el-divider content-position="left">组件配置</el-divider>

        <!-- 数值输入配置 -->
        <template v-if="formData.componentType === 'number'">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="最小值">
                <el-input-number v-model="formData.componentConfig.min" :controls="false" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="最大值">
                <el-input-number v-model="formData.componentConfig.max" :controls="false" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="步长">
                <el-input-number v-model="formData.componentConfig.step" :min="0.1" :step="0.1" :controls="false" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <!-- 选择器配置 -->
        <template v-else-if="formData.componentType === 'select'">
          <el-form-item label="选项列表">
            <div class="options-editor">
              <div v-for="(opt, idx) in formData.componentConfig.options" :key="idx" class="option-row">
                <el-input v-model="opt.label" placeholder="选项名称" style="width: 120px" />
                <el-input-number v-model="opt.score" placeholder="分值" :controls="false" style="width: 80px" />
                <el-button v-if="formData.componentConfig.options.length > 2" link type="danger" @click="removeOption(idx)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <el-button link type="primary" @click="addOption">+ 添加选项</el-button>
            </div>
          </el-form-item>
        </template>

        <!-- 星级配置 -->
        <template v-else-if="formData.componentType === 'star'">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="最大星数">
                <el-input-number v-model="formData.componentConfig.max" :min="3" :max="10" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="允许半星">
                <el-switch v-model="formData.componentConfig.allowHalf" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <!-- 等级配置 -->
        <template v-else-if="formData.componentType === 'grade'">
          <el-form-item label="等级列表">
            <div class="grades-editor">
              <div v-for="(grade, idx) in formData.componentConfig.grades" :key="idx" class="grade-row">
                <el-input v-model="formData.componentConfig.grades[idx]" placeholder="等级" style="width: 80px" />
                <el-color-picker v-model="formData.componentConfig.colors[idx]" />
                <el-button v-if="formData.componentConfig.grades.length > 2" link type="danger" @click="removeGrade(idx)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <el-button link type="primary" @click="addGrade">+ 添加等级</el-button>
            </div>
          </el-form-item>
        </template>

        <!-- 滑块配置 -->
        <template v-else-if="formData.componentType === 'slider'">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="最小值">
                <el-input-number v-model="formData.componentConfig.min" :controls="false" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="最大值">
                <el-input-number v-model="formData.componentConfig.max" :controls="false" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="步长">
                <el-input-number v-model="formData.componentConfig.step" :min="1" :controls="false" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, markRaw } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Delete, Edit, Select, Checked, Operation, Star, Medal, Document } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { InputType, CreateInputTypeCommand, UpdateInputTypeCommand } from '@/types/scoring'
import {
  getAllInputTypes,
  createInputType,
  updateInputType,
  deleteInputType,
  toggleInputType
} from '@/api/scoring'

// 图标映射
const iconMap: Record<string, any> = {
  number: markRaw(Edit),
  select: markRaw(Select),
  checkbox: markRaw(Checked),
  slider: markRaw(Operation),
  star: markRaw(Star),
  grade: markRaw(Medal),
  textarea: markRaw(Document)
}

const colorMap: Record<string, string> = {
  number: '#5b5fc7',
  select: '#52c41a',
  checkbox: '#1890ff',
  slider: '#fa8c16',
  star: '#fadb14',
  grade: '#eb2f96',
  textarea: '#8c8c8c'
}

const getComponentIcon = (type: string) => iconMap[type] || Edit
const getComponentColor = (type: string) => colorMap[type] || '#666'

// 状态
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const searchKeyword = ref('')
const filterCategory = ref('')

const inputTypes = ref<InputType[]>([])

const formRef = ref<FormInstance>()
const formData = ref<Partial<CreateInputTypeCommand> & { componentConfig: any }>({
  code: '',
  name: '',
  category: 'basic',
  componentType: 'number',
  description: '',
  componentConfig: {},
  sortOrder: 0
})

const formRules: FormRules = {
  code: [
    { required: true, message: '请输入编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]*$/, message: '编码必须大写字母开头', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  componentType: [{ required: true, message: '请选择组件类型', trigger: 'change' }]
}

// 计算属性
const filteredInputTypes = computed(() => {
  let result = inputTypes.value
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(t =>
      t.name.toLowerCase().includes(keyword) ||
      t.code.toLowerCase().includes(keyword)
    )
  }
  if (filterCategory.value) {
    result = result.filter(t => t.category === filterCategory.value)
  }
  return result
})

// 方法
async function loadInputTypes() {
  loading.value = true
  try {
    const res = await getAllInputTypes()
    inputTypes.value = res?.data?.data || res?.data || res || []
  } catch (error) {
    ElMessage.error('加载打分方式失败')
  } finally {
    loading.value = false
  }
}

function getDefaultConfig(componentType: string) {
  switch (componentType) {
    case 'number':
      return { min: -10, max: 10, step: 1 }
    case 'select':
      return { options: [{ label: '合格', score: 0 }, { label: '不合格', score: -2 }] }
    case 'star':
      return { max: 5, allowHalf: false }
    case 'grade':
      return { grades: ['A', 'B', 'C', 'D'], colors: ['#52c41a', '#1890ff', '#faad14', '#ff4d4f'] }
    case 'slider':
      return { min: 0, max: 100, step: 1 }
    case 'checkbox':
      return { checkedScore: 0, uncheckedScore: -1 }
    default:
      return {}
  }
}

function handleComponentTypeChange(type: string) {
  formData.value.componentConfig = getDefaultConfig(type)
}

function handleCreate() {
  isEdit.value = false
  formData.value = {
    code: '',
    name: '',
    category: 'basic',
    componentType: 'number',
    description: '',
    componentConfig: getDefaultConfig('number'),
    sortOrder: 0
  }
  dialogVisible.value = true
}

function handleEdit(row: InputType) {
  isEdit.value = true
  formData.value = {
    code: row.code,
    name: row.name,
    category: row.category,
    componentType: row.componentType,
    description: row.description || '',
    componentConfig: { ...(row.componentConfig || getDefaultConfig(row.componentType)) },
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
    const data = {
      ...formData.value,
      componentConfig: formData.value.componentConfig
    }
    if (isEdit.value) {
      const id = (formData.value as any)._id
      await updateInputType(id, data as UpdateInputTypeCommand)
      ElMessage.success('更新成功')
    } else {
      await createInputType(data as CreateInputTypeCommand)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadInputTypes()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: InputType) {
  try {
    await ElMessageBox.confirm(`确定要删除"${row.name}"吗？`, '确认删除', { type: 'warning' })
    await deleteInputType(row.id)
    ElMessage.success('删除成功')
    await loadInputTypes()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function handleToggleStatus(row: InputType) {
  try {
    await toggleInputType(row.id, row.isEnabled)
    ElMessage.success(row.isEnabled ? '已启用' : '已禁用')
  } catch (error: any) {
    row.isEnabled = !row.isEnabled
    ElMessage.error(error.message || '操作失败')
  }
}

function addOption() {
  if (!formData.value.componentConfig.options) {
    formData.value.componentConfig.options = []
  }
  formData.value.componentConfig.options.push({ label: '', score: 0 })
}

function removeOption(idx: number) {
  formData.value.componentConfig.options.splice(idx, 1)
}

function addGrade() {
  if (!formData.value.componentConfig.grades) {
    formData.value.componentConfig.grades = []
    formData.value.componentConfig.colors = []
  }
  formData.value.componentConfig.grades.push('')
  formData.value.componentConfig.colors.push('#999999')
}

function removeGrade(idx: number) {
  formData.value.componentConfig.grades.splice(idx, 1)
  formData.value.componentConfig.colors.splice(idx, 1)
}

onMounted(() => {
  loadInputTypes()
})
</script>

<style scoped>
.input-type-config {
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

.type-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-icon {
  flex-shrink: 0;
}

.component-code {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 12px;
  color: #5b5fc7;
  background: #f5f5ff;
  padding: 2px 6px;
  border-radius: 4px;
}

.config-preview {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 12px;
  margin: 0;
  white-space: pre-wrap;
}

.text-muted {
  color: #999;
}

.options-editor, .grades-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-row, .grade-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
