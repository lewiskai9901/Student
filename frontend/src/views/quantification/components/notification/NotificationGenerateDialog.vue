<template>
  <el-dialog
    v-model="visible"
    title="生成通报"
    width="800px"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <el-steps :active="currentStep" finish-status="success" align-center style="margin-bottom: 30px">
      <el-step title="选择类型" />
      <el-step title="配置参数" />
      <el-step title="生成预览" />
    </el-steps>

    <!-- 第一步：选择通报类型 -->
    <div v-show="currentStep === 0" class="step-content">
      <div class="notification-type-grid">
        <div
          v-for="type in notificationTypes"
          :key="type.value"
          class="type-card"
          :class="{ selected: form.notificationType === type.value }"
          @click="form.notificationType = type.value"
        >
          <div class="type-icon" :class="type.iconClass">
            <el-icon :size="48">
              <component :is="type.icon" />
            </el-icon>
          </div>
          <h3 class="type-title">{{ type.label }}</h3>
          <p class="type-description">{{ type.description }}</p>
          <div v-if="form.notificationType === type.value" class="selected-badge">
            <el-icon><Check /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 第二步：配置参数 -->
    <div v-show="currentStep === 1" class="step-content">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="检查计划" prop="checkPlanId">
          <el-select
            v-model="form.checkPlanId"
            placeholder="选择检查计划"
            style="width: 100%"
            @change="loadRatingRules"
          >
            <el-option
              v-for="plan in checkPlans"
              :key="plan.id"
              :label="plan.planName"
              :value="plan.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="评级规则" prop="ruleId">
          <el-select
            v-model="form.ruleId"
            placeholder="选择评级规则（可选）"
            clearable
            style="width: 100%"
            @change="loadRatingLevels"
          >
            <el-option
              v-for="rule in ratingRules"
              :key="rule.id"
              :label="rule.ruleName"
              :value="rule.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="评级等级" prop="levelIds">
          <el-select
            v-model="form.levelIds"
            placeholder="选择评级等级（可多选）"
            multiple
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="level in ratingLevels"
              :key="level.id"
              :label="level.levelName"
              :value="level.id"
            >
              <span :style="{ color: level.color }">{{ level.levelName }}</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="统计周期" prop="dateRange">
          <el-date-picker
            v-model="form.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="最小频次">
          <el-input-number
            v-model="form.minFrequency"
            :min="1"
            :max="100"
            placeholder="最少获奖次数"
          />
          <span class="form-tip">只统计获奖次数≥该值的班级</span>
        </el-form-item>

        <el-form-item label="通报标题">
          <el-input
            v-model="form.title"
            placeholder="自动生成（可自定义）"
            maxlength="100"
          />
        </el-form-item>
      </el-form>
    </div>

    <!-- 第三步：生成预览 -->
    <div v-show="currentStep === 2" class="step-content">
      <el-result
        v-if="generateResult"
        icon="success"
        title="通报生成成功！"
        :sub-title="`涉及 ${generateResult.classCount} 个班级`"
      >
        <template #extra>
          <div class="result-info">
            <p><strong>文件名：</strong>{{ generateResult.fileName }}</p>
            <p>
              <strong>生成时间：</strong>
              {{ new Date(generateResult.generatedAt).toLocaleString() }}
            </p>
          </div>
          <el-button type="primary" @click="handleDownload">
            <el-icon><Download /></el-icon>
            下载通报
          </el-button>
          <el-button @click="handleGenerateAgain">重新生成</el-button>
        </template>
      </el-result>

      <div v-else class="generating-placeholder">
        <el-empty description="请点击"生成通报"按钮" />
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button v-if="currentStep > 0 && currentStep < 2" @click="currentStep--">
          上一步
        </el-button>
        <el-button @click="visible = false">取消</el-button>
        <el-button
          v-if="currentStep < 2"
          type="primary"
          @click="handleNext"
        >
          {{ currentStep === 1 ? '生成通报' : '下一步' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, shallowRef } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Trophy, Document, Warning, Check, Download } from '@element-plus/icons-vue'
import {
  generateHonorNotification,
  type NotificationGenerateResultVO,
  NOTIFICATION_TYPE_LABELS,
  NOTIFICATION_TYPE_DESCRIPTIONS
} from '@/api/ratingNotification'
import { getCheckPlanPage } from '@/api/checkPlan'
import { getRatingRulesByPlan, getRatingLevelsByRule } from '@/api/checkPlanRating'

// Props
const props = defineProps<{
  modelValue: boolean
  checkPlanId?: number
}>()

// Emits
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

// 响应式数据
const formRef = ref<FormInstance>()
const visible = ref(false)
const currentStep = ref(0)
const generating = ref(false)

const checkPlans = ref<any[]>([])
const ratingRules = ref<any[]>([])
const ratingLevels = ref<any[]>([])
const generateResult = ref<NotificationGenerateResultVO | null>(null)

// 通报类型配置
const notificationTypes = [
  {
    value: 'HONOR',
    label: NOTIFICATION_TYPE_LABELS.HONOR,
    description: NOTIFICATION_TYPE_DESCRIPTIONS.HONOR,
    icon: shallowRef(Trophy),
    iconClass: 'icon-success'
  },
  {
    value: 'FULL',
    label: NOTIFICATION_TYPE_LABELS.FULL,
    description: NOTIFICATION_TYPE_DESCRIPTIONS.FULL,
    icon: shallowRef(Document),
    iconClass: 'icon-primary'
  },
  {
    value: 'ALERT',
    label: NOTIFICATION_TYPE_LABELS.ALERT,
    description: NOTIFICATION_TYPE_DESCRIPTIONS.ALERT,
    icon: shallowRef(Warning),
    iconClass: 'icon-warning'
  }
]

// 表单数据
const form = reactive({
  notificationType: 'HONOR',
  checkPlanId: undefined as number | undefined,
  ruleId: undefined as number | undefined,
  levelIds: [] as number[],
  dateRange: [] as string[],
  minFrequency: 1,
  title: ''
})

// 表单验证规则
const rules: FormRules = {
  checkPlanId: [{ required: true, message: '请选择检查计划', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择统计周期', trigger: 'change' }]
}

// 监听visible变化
watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
    if (val) {
      loadCheckPlans()
      // 默认设置最近一个月
      const now = new Date()
      const periodEnd = now.toISOString().split('T')[0]
      const periodStart = new Date(now.getFullYear(), now.getMonth() - 1, 1)
        .toISOString()
        .split('T')[0]
      form.dateRange = [periodStart, periodEnd]

      if (props.checkPlanId) {
        form.checkPlanId = props.checkPlanId
        loadRatingRules()
      }
    }
  }
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 加载检查计划
const loadCheckPlans = async () => {
  try {
    const res = await getCheckPlanPage({ pageNum: 1, pageSize: 100 })
    checkPlans.value = res?.data?.records || []
  } catch (error) {
    console.error('加载检查计划失败:', error)
    ElMessage.error('加载检查计划失败，请检查后端服务')
  }
}

// 加载评级规则
const loadRatingRules = async () => {
  if (!form.checkPlanId) return

  try {
    const res = await getRatingRulesByPlan(form.checkPlanId)
    ratingRules.value = res.data || []
  } catch (error) {
    console.error('加载评级规则失败:', error)
  }
}

// 加载评级等级
const loadRatingLevels = async () => {
  if (!form.ruleId) return

  try {
    const res = await getRatingLevelsByRule(form.ruleId)
    ratingLevels.value = res.data || []
  } catch (error) {
    console.error('加载评级等级失败:', error)
  }
}

// 下一步
const handleNext = async () => {
  if (currentStep.value === 0) {
    // 第一步：选择类型
    if (!form.notificationType) {
      ElMessage.warning('请选择通报类型')
      return
    }
    currentStep.value++
  } else if (currentStep.value === 1) {
    // 第二步：配置参数并生成
    if (!formRef.value) return

    try {
      await formRef.value.validate()
      await handleGenerate()
    } catch (error) {
      // 验证失败
    }
  }
}

// 生成通报
const handleGenerate = async () => {
  generating.value = true

  try {
    const res = await generateHonorNotification({
      notificationType: form.notificationType as any,
      checkPlanId: form.checkPlanId!,
      ruleId: form.ruleId,
      levelIds: form.levelIds,
      periodStart: form.dateRange[0],
      periodEnd: form.dateRange[1],
      minFrequency: form.minFrequency,
      title: form.title || undefined
    })

    generateResult.value = res.data
    currentStep.value = 2
    ElMessage.success('通报生成成功！')
    emit('success')
  } catch (error: any) {
    ElMessage.error(error.message || '生成通报失败')
  } finally {
    generating.value = false
  }
}

// 下载通报
const handleDownload = () => {
  if (generateResult.value) {
    window.open(generateResult.value.filePath, '_blank')
  }
}

// 重新生成
const handleGenerateAgain = () => {
  currentStep.value = 1
  generateResult.value = null
}

// 重置表单
const resetForm = () => {
  currentStep.value = 0
  form.notificationType = 'HONOR'
  form.checkPlanId = undefined
  form.ruleId = undefined
  form.levelIds = []
  form.dateRange = []
  form.minFrequency = 1
  form.title = ''
  generateResult.value = null

  formRef.value?.clearValidate()
}

// 对话框关闭回调
const handleClosed = () => {
  resetForm()
}
</script>

<style scoped lang="scss">
.step-content {
  min-height: 300px;
  padding: 20px 0;
}

.notification-type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  padding: 10px;

  .type-card {
    position: relative;
    padding: 30px 20px;
    border: 2px solid #e4e7ed;
    border-radius: 12px;
    text-align: center;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      border-color: #409eff;
      box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
    }

    &.selected {
      border-color: #409eff;
      background: #f0f9ff;
      box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
    }

    .type-icon {
      margin-bottom: 16px;

      &.icon-success {
        color: #67c23a;
      }

      &.icon-primary {
        color: #409eff;
      }

      &.icon-warning {
        color: #e6a23c;
      }
    }

    .type-title {
      margin: 0 0 10px 0;
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }

    .type-description {
      margin: 0;
      font-size: 13px;
      color: #909399;
      line-height: 1.6;
    }

    .selected-badge {
      position: absolute;
      top: 10px;
      right: 10px;
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: #409eff;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
    }
  }
}

.form-tip {
  margin-left: 10px;
  font-size: 13px;
  color: #909399;
}

.result-info {
  margin-bottom: 20px;
  text-align: left;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 6px;

  p {
    margin: 8px 0;
    font-size: 14px;
    color: #606266;
  }
}

.generating-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
