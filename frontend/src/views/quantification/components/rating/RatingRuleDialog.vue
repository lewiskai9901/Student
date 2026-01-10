<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑评级规则' : '新建评级规则'"
    width="700px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="formRules"
      label-width="100px"
    >
      <el-form-item label="规则名称" prop="ruleName">
        <el-input v-model="form.ruleName" placeholder="请输入规则名称，如：卫生评级" />
      </el-form-item>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="评级类型" prop="ruleType">
            <el-select v-model="form.ruleType" placeholder="请选择" style="width: 100%">
              <el-option
                v-for="(label, value) in RULE_TYPE_LABELS"
                :key="value"
                :label="label"
                :value="value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="划分方式" prop="divisionMethod">
            <el-select v-model="form.divisionMethod" placeholder="请选择" style="width: 100%">
              <el-option
                v-for="(label, value) in DIVISION_METHOD_LABELS"
                :key="value"
                :label="label"
                :value="value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="评分来源" prop="scoreSource">
            <el-select v-model="form.scoreSource" placeholder="请选择" style="width: 100%">
              <el-option
                v-for="(label, value) in SCORE_SOURCE_LABELS"
                :key="value"
                :label="label"
                :value="value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12" v-if="form.scoreSource === 'CATEGORY'">
          <el-form-item label="检查类别" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择类别" style="width: 100%">
              <el-option
                v-for="cat in categories"
                :key="cat.id"
                :label="cat.categoryName"
                :value="cat.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20" v-if="form.ruleType === 'SUMMARY'">
        <el-col :span="12">
          <el-form-item label="汇总方式" prop="summaryMethod">
            <el-select v-model="form.summaryMethod" placeholder="请选择" style="width: 100%">
              <el-option
                v-for="(label, value) in SUMMARY_METHOD_LABELS"
                :key="value"
                :label="label"
                :value="value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="规则描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="2"
          placeholder="请输入规则描述（可选）"
        />
      </el-form-item>

      <!-- 等级配置 -->
      <el-divider content-position="left">等级配置</el-divider>

      <div class="level-config">
        <div class="level-header">
          <span class="level-hint">
            <el-icon><InfoFilled /></el-icon>
            {{ divisionHint }}
          </span>
          <el-button type="primary" link @click="addLevel">
            <el-icon><Plus /></el-icon>
            添加等级
          </el-button>
        </div>

        <div v-for="(level, index) in form.levels" :key="index" class="level-item">
          <el-row :gutter="12" align="middle">
            <el-col :span="1">
              <span class="level-order">{{ level.levelOrder }}</span>
            </el-col>
            <el-col :span="5">
              <el-input v-model="level.levelName" placeholder="等级名称" />
            </el-col>
            <el-col :span="3">
              <el-color-picker v-model="level.levelColor" size="small" />
            </el-col>

            <!-- 分数段 -->
            <template v-if="form.divisionMethod === 'SCORE_RANGE'">
              <el-col :span="5">
                <el-input-number
                  v-model="level.minScore"
                  :min="0"
                  :precision="2"
                  placeholder="最小扣分"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-col>
              <el-col :span="1" class="text-center">~</el-col>
              <el-col :span="5">
                <el-input-number
                  v-model="level.maxScore"
                  :min="0"
                  :precision="2"
                  placeholder="最大扣分"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-col>
            </template>

            <!-- 名次数量 -->
            <template v-else-if="form.divisionMethod === 'RANK_COUNT'">
              <el-col :span="6">
                <el-input-number
                  v-model="level.rankCount"
                  :min="1"
                  placeholder="名次数量"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-col>
              <el-col :span="5">
                <span class="level-desc">前 {{ getCumulativeRank(index) }} 名</span>
              </el-col>
            </template>

            <!-- 百分比 -->
            <template v-else-if="form.divisionMethod === 'PERCENTAGE'">
              <el-col :span="6">
                <el-input-number
                  v-model="level.percentage"
                  :min="0.01"
                  :max="100"
                  :precision="2"
                  placeholder="百分比"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-col>
              <el-col :span="5">
                <span class="level-desc">前 {{ getCumulativePercentage(index) }}%</span>
              </el-col>
            </template>

            <el-col :span="2">
              <el-button
                v-if="form.levels.length > 1"
                type="danger"
                link
                @click="removeLevel(index)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-col>
          </el-row>
        </div>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ isEdit ? '保存' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Plus, Delete, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  createRatingRule,
  updateRatingRule,
  RULE_TYPE_LABELS,
  SCORE_SOURCE_LABELS,
  DIVISION_METHOD_LABELS,
  SUMMARY_METHOD_LABELS,
  LEVEL_COLORS,
  type RatingRuleVO,
  type RatingRuleCreateDTO,
  type RatingLevelDTO
} from '@/api/v2/rating'

const props = defineProps<{
  visible: boolean
  checkPlanId: string | number
  ruleData: RatingRuleVO | null
  categories: Array<{ id: string | number; categoryName: string }>
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const isEdit = computed(() => !!props.ruleData)

// 表单数据
const form = ref({
  ruleName: '',
  ruleType: 'DAILY' as 'DAILY' | 'SUMMARY',
  scoreSource: 'TOTAL' as 'TOTAL' | 'CATEGORY',
  categoryId: undefined as string | number | undefined,
  useWeightedScore: 0,
  divisionMethod: 'SCORE_RANGE' as 'SCORE_RANGE' | 'RANK_COUNT' | 'PERCENTAGE',
  summaryMethod: 'AVERAGE' as 'AVERAGE' | 'SUM',
  description: '',
  levels: [] as RatingLevelDTO[]
})

// 表单校验规则
const formRules: FormRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleType: [{ required: true, message: '请选择评级类型', trigger: 'change' }],
  scoreSource: [{ required: true, message: '请选择评分来源', trigger: 'change' }],
  divisionMethod: [{ required: true, message: '请选择划分方式', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择类别', trigger: 'change' }]
}

// 划分方式提示
const divisionHint = computed(() => {
  switch (form.value.divisionMethod) {
    case 'SCORE_RANGE':
      return '按扣分范围划分等级，扣分越少等级越高'
    case 'RANK_COUNT':
      return '按排名数量划分等级，如前3名为优秀'
    case 'PERCENTAGE':
      return '按排名百分比划分等级，如前10%为优秀'
    default:
      return ''
  }
})

// 计算累计名次
const getCumulativeRank = (index: number) => {
  let total = 0
  for (let i = 0; i <= index; i++) {
    total += form.value.levels[i]?.rankCount || 0
  }
  return total
}

// 计算累计百分比
const getCumulativePercentage = (index: number) => {
  let total = 0
  for (let i = 0; i <= index; i++) {
    total += form.value.levels[i]?.percentage || 0
  }
  return total.toFixed(2)
}

// 初始化默认等级
const initDefaultLevels = () => {
  form.value.levels = [
    { levelOrder: 1, levelName: '优秀', levelColor: LEVEL_COLORS[0], minScore: 0, maxScore: 5, rankCount: 3, percentage: 10 },
    { levelOrder: 2, levelName: '良好', levelColor: LEVEL_COLORS[1], minScore: 5, maxScore: 10, rankCount: 5, percentage: 30 },
    { levelOrder: 3, levelName: '一般', levelColor: LEVEL_COLORS[2], minScore: 10, maxScore: 20, rankCount: 10, percentage: 40 },
    { levelOrder: 4, levelName: '较差', levelColor: LEVEL_COLORS[3], minScore: 20, maxScore: 9999, rankCount: 100, percentage: 20 }
  ]
}

// 添加等级
const addLevel = () => {
  const order = form.value.levels.length + 1
  const colorIndex = (order - 1) % LEVEL_COLORS.length
  form.value.levels.push({
    levelOrder: order,
    levelName: `等级${order}`,
    levelColor: LEVEL_COLORS[colorIndex],
    minScore: 0,
    maxScore: 0,
    rankCount: 1,
    percentage: 10
  })
}

// 删除等级
const removeLevel = (index: number) => {
  form.value.levels.splice(index, 1)
  // 重新排序
  form.value.levels.forEach((level, i) => {
    level.levelOrder = i + 1
  })
}

// 监听对话框打开，初始化表单
watch(() => props.visible, (val) => {
  if (val) {
    if (props.ruleData) {
      // 编辑模式，填充数据
      form.value = {
        ruleName: props.ruleData.ruleName,
        ruleType: props.ruleData.ruleType,
        scoreSource: props.ruleData.scoreSource,
        categoryId: props.ruleData.categoryId,
        useWeightedScore: props.ruleData.useWeightedScore,
        divisionMethod: props.ruleData.divisionMethod,
        summaryMethod: props.ruleData.summaryMethod || 'AVERAGE',
        description: props.ruleData.description || '',
        levels: props.ruleData.levels.map(l => ({
          levelOrder: l.levelOrder,
          levelName: l.levelName,
          levelColor: l.levelColor,
          levelIcon: l.levelIcon,
          minScore: l.minScore,
          maxScore: l.maxScore,
          rankCount: l.rankCount,
          percentage: l.percentage
        }))
      }
    } else {
      // 新建模式，重置表单
      form.value = {
        ruleName: '',
        ruleType: 'DAILY',
        scoreSource: 'TOTAL',
        categoryId: undefined,
        useWeightedScore: 0,
        divisionMethod: 'SCORE_RANGE',
        summaryMethod: 'AVERAGE',
        description: '',
        levels: []
      }
      initDefaultLevels()
    }
    formRef.value?.clearValidate()
  }
})

// 提交表单
const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  // 校验等级配置
  if (form.value.levels.length === 0) {
    ElMessage.warning('请至少配置一个等级')
    return
  }

  submitting.value = true
  try {
    const data: RatingRuleCreateDTO = {
      checkPlanId: props.checkPlanId,
      ruleName: form.value.ruleName,
      ruleType: form.value.ruleType,
      scoreSource: form.value.scoreSource,
      categoryId: form.value.scoreSource === 'CATEGORY' ? form.value.categoryId : undefined,
      useWeightedScore: form.value.useWeightedScore,
      divisionMethod: form.value.divisionMethod,
      summaryMethod: form.value.ruleType === 'SUMMARY' ? form.value.summaryMethod : undefined,
      description: form.value.description,
      levels: form.value.levels
    }

    if (isEdit.value && props.ruleData) {
      await updateRatingRule(props.ruleData.id, data)
      ElMessage.success('更新成功')
    } else {
      await createRatingRule(data)
      ElMessage.success('创建成功')
    }

    emit('update:visible', false)
    emit('success')
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.level-config {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
}

.level-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.level-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 13px;
}

.level-item {
  background: #fff;
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 8px;
}

.level-order {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: #409eff;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 500;
}

.level-desc {
  color: #606266;
  font-size: 13px;
}

.text-center {
  text-align: center;
  color: #909399;
}
</style>
