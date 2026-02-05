<template>
  <div class="scoring-strategy-management">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span class="title">打分策略管理</span>
          <el-button type="primary" @click="showCreateDialog">
            <el-icon><Plus /></el-icon>
            新建策略
          </el-button>
        </div>
      </template>

      <!-- 策略类型切换 -->
      <div class="type-tabs">
        <el-radio-group v-model="selectedType" @change="loadStrategies">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button v-for="t in strategyTypes" :key="t.code" :value="t.code">
            {{ t.name }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- 策略列表 -->
      <el-table :data="filteredStrategies" stripe border v-loading="loading">
        <el-table-column prop="strategyName" label="策略名称" min-width="150" />
        <el-table-column prop="strategyCode" label="策略代码" width="150" />
        <el-table-column label="策略类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.strategyType)" size="small">
              {{ row.strategyTypeName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="resultFormat" label="结果格式" width="150" />
        <el-table-column label="系统内置" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isSystem" type="info" size="small">是</el-tag>
            <el-tag v-else type="success" size="small">否</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.isEnabled" @change="toggleStatus(row)"
                       :disabled="row.isSystem" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewConfig(row)">查看</el-button>
            <el-button link type="primary" size="small" @click="editStrategy(row)"
                       :disabled="row.isSystem">编辑</el-button>
            <el-button link type="danger" size="small" @click="deleteStrategy(row)"
                       :disabled="row.isSystem">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑策略对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingStrategy ? '编辑策略' : '新建策略'" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="策略类型" prop="strategyType" v-if="!editingStrategy">
          <el-select v-model="form.strategyType" placeholder="选择策略类型" style="width: 100%"
                     @change="onTypeChange">
            <el-option v-for="t in strategyTypes" :key="t.code" :label="t.name" :value="t.code">
              <div>
                <span>{{ t.name }}</span>
                <span class="type-desc">{{ t.description }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="策略代码" prop="strategyCode" v-if="!editingStrategy">
          <el-input v-model="form.strategyCode" placeholder="如: MY_DEDUCTION">
            <template #append>
              <el-button @click="generateCode">自动生成</el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="策略名称" prop="strategyName">
          <el-input v-model="form.strategyName" placeholder="如: 自定义扣分制" />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" rows="2" placeholder="策略描述" />
        </el-form-item>

        <!-- 基准分策略配置 -->
        <template v-if="form.strategyType === 'BASE_SCORE'">
          <el-form-item label="基准分" prop="baseScore">
            <el-input-number v-model="form.baseScore" :min="0" :max="200" />
          </el-form-item>
          <el-form-item label="最低分" prop="minScore">
            <el-input-number v-model="form.minScore" :min="0" :max="form.baseScore" />
          </el-form-item>
        </template>

        <!-- 评分制配置 -->
        <template v-if="form.strategyType === 'RATING'">
          <el-form-item label="最低分" prop="minRating">
            <el-input-number v-model="form.minRating" :min="1" :max="10" />
          </el-form-item>
          <el-form-item label="最高分" prop="maxRating">
            <el-input-number v-model="form.maxRating" :min="form.minRating" :max="10" />
          </el-form-item>
        </template>

        <!-- 等级制配置 -->
        <template v-if="form.strategyType === 'GRADE'">
          <el-form-item label="等级配置">
            <el-input v-model="form.gradesConfig" type="textarea" rows="4"
                      placeholder='{"grades": [{"code":"A","name":"优秀","min_score":90},{"code":"B","name":"良好","min_score":75}]}' />
          </el-form-item>
        </template>

        <el-form-item label="结果格式" prop="resultFormat">
          <el-input v-model="form.resultFormat" placeholder="如: {score}分, {grade}({grade_name})" />
          <div class="form-tip">
            可用变量: {score}, {total}, {grade}, {grade_name}, {checked}, {rate}
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看配置对话框 -->
    <el-dialog v-model="configDialogVisible" title="策略配置详情" width="500px">
      <el-descriptions :column="1" border v-if="viewingStrategy">
        <el-descriptions-item label="策略代码">{{ viewingStrategy.strategyCode }}</el-descriptions-item>
        <el-descriptions-item label="策略名称">{{ viewingStrategy.strategyName }}</el-descriptions-item>
        <el-descriptions-item label="策略类型">{{ viewingStrategy.strategyTypeName }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ viewingStrategy.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="配置">
          <pre class="config-json">{{ formatJson(viewingStrategy.config) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="结果格式">{{ viewingStrategy.resultFormat }}</el-descriptions-item>
        <el-descriptions-item label="系统内置">{{ viewingStrategy.isSystem ? '是' : '否' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  scoringStrategyApi,
  type ScoringStrategy, type StrategyTypeInfo, type ScoringStrategyType
} from '@/api/v6ScoringStrategy'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const configDialogVisible = ref(false)

const strategies = ref<ScoringStrategy[]>([])
const strategyTypes = ref<StrategyTypeInfo[]>([])
const selectedType = ref<ScoringStrategyType | ''>('')
const editingStrategy = ref<ScoringStrategy | null>(null)
const viewingStrategy = ref<ScoringStrategy | null>(null)

const formRef = ref()

const form = reactive({
  strategyType: '' as ScoringStrategyType | '',
  strategyCode: '',
  strategyName: '',
  description: '',
  baseScore: 100,
  minScore: 0,
  minRating: 1,
  maxRating: 5,
  gradesConfig: '',
  resultFormat: ''
})

const rules = {
  strategyType: [{ required: true, message: '请选择策略类型', trigger: 'change' }],
  strategyCode: [{ required: true, message: '请输入策略代码', trigger: 'blur' }],
  strategyName: [{ required: true, message: '请输入策略名称', trigger: 'blur' }]
}

const filteredStrategies = computed(() => {
  if (!selectedType.value) return strategies.value
  return strategies.value.filter(s => s.strategyType === selectedType.value)
})

const getTypeTagType = (type: string) => {
  const types: Record<string, string> = {
    'DEDUCTION': 'danger',
    'ADDITION': 'success',
    'BASE_SCORE': 'primary',
    'RATING': 'warning',
    'GRADE': 'info',
    'PASS_FAIL': '',
    'CHECKLIST': ''
  }
  return types[type] || ''
}

const loadData = async () => {
  try {
    loading.value = true
    const [strategiesRes, typesRes] = await Promise.all([
      scoringStrategyApi.getAllEnabled(),
      scoringStrategyApi.getStrategyTypes()
    ])
    // request.ts拦截器已经提取了data字段，直接使用
    strategies.value = strategiesRes as ScoringStrategy[]
    strategyTypes.value = typesRes as StrategyTypeInfo[]
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

const loadStrategies = async () => {
  await loadData()
}

const showCreateDialog = () => {
  editingStrategy.value = null
  Object.assign(form, {
    strategyType: '',
    strategyCode: '',
    strategyName: '',
    description: '',
    baseScore: 100,
    minScore: 0,
    minRating: 1,
    maxRating: 5,
    gradesConfig: '',
    resultFormat: ''
  })
  dialogVisible.value = true
}

const generateCode = () => {
  const prefix = form.strategyType || 'CUSTOM'
  const random = Math.random().toString(36).substring(2, 6).toUpperCase()
  form.strategyCode = `${prefix}_${random}`
}

const onTypeChange = () => {
  // 设置默认结果格式
  const formats: Record<string, string> = {
    'DEDUCTION': '累计扣{total}分',
    'ADDITION': '累计加{total}分',
    'BASE_SCORE': '{score}分',
    'RATING': '{score}/{max}分',
    'GRADE': '{grade}({grade_name})',
    'PASS_FAIL': '{result}',
    'CHECKLIST': '{checked}/{total}项({rate}%)'
  }
  if (form.strategyType && !form.resultFormat) {
    form.resultFormat = formats[form.strategyType] || ''
  }
}

const editStrategy = (strategy: ScoringStrategy) => {
  editingStrategy.value = strategy
  Object.assign(form, {
    strategyType: strategy.strategyType,
    strategyCode: strategy.strategyCode,
    strategyName: strategy.strategyName,
    description: strategy.description || '',
    resultFormat: strategy.resultFormat || ''
  })
  dialogVisible.value = true
}

const viewConfig = (strategy: ScoringStrategy) => {
  viewingStrategy.value = strategy
  configDialogVisible.value = true
}

const formatJson = (jsonStr?: string) => {
  if (!jsonStr) return '-'
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2)
  } catch {
    return jsonStr
  }
}

const submitForm = async () => {
  try {
    await formRef.value?.validate()
    submitting.value = true

    if (editingStrategy.value) {
      await scoringStrategyApi.update(editingStrategy.value.id, {
        strategyName: form.strategyName,
        description: form.description,
        resultFormat: form.resultFormat
      })
      ElMessage.success('更新成功')
    } else {
      // 根据类型调用不同的创建API
      if (form.strategyType === 'DEDUCTION') {
        await scoringStrategyApi.createDeduction({
          strategyCode: form.strategyCode,
          strategyName: form.strategyName,
          description: form.description
        })
      } else if (form.strategyType === 'BASE_SCORE') {
        await scoringStrategyApi.createBaseScore({
          strategyCode: form.strategyCode,
          strategyName: form.strategyName,
          description: form.description,
          baseScore: form.baseScore,
          minScore: form.minScore
        })
      } else if (form.strategyType === 'RATING') {
        await scoringStrategyApi.createRating({
          strategyCode: form.strategyCode,
          strategyName: form.strategyName,
          description: form.description,
          minRating: form.minRating,
          maxRating: form.maxRating
        })
      } else if (form.strategyType === 'GRADE') {
        await scoringStrategyApi.createGrade({
          strategyCode: form.strategyCode,
          strategyName: form.strategyName,
          description: form.description,
          gradesConfig: form.gradesConfig
        })
      } else {
        await scoringStrategyApi.create({
          strategyCode: form.strategyCode,
          strategyName: form.strategyName,
          description: form.description,
          strategyType: form.strategyType as ScoringStrategyType,
          resultFormat: form.resultFormat
        })
      }
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    await loadData()
  } catch (error: any) {
    if (error !== false) {
      ElMessage.error(error.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

const toggleStatus = async (strategy: ScoringStrategy) => {
  try {
    if (strategy.isEnabled) {
      await scoringStrategyApi.enable(strategy.id)
    } else {
      await scoringStrategyApi.disable(strategy.id)
    }
    ElMessage.success('状态更新成功')
  } catch (error: any) {
    strategy.isEnabled = !strategy.isEnabled
    ElMessage.error(error.message || '状态更新失败')
  }
}

const deleteStrategy = async (strategy: ScoringStrategy) => {
  try {
    await ElMessageBox.confirm(`确定删除策略"${strategy.strategyName}"吗？`, '提示', {
      type: 'warning'
    })
    await scoringStrategyApi.delete(strategy.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.scoring-strategy-management {
  padding: 20px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 16px;
  font-weight: 500;
}

.type-tabs {
  margin-bottom: 20px;
}

.type-desc {
  color: #909399;
  font-size: 12px;
  margin-left: 10px;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

.config-json {
  background: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
