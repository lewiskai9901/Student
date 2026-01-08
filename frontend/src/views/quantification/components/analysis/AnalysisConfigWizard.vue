<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑分析配置' : '新建分析配置'"
    width="900px"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <el-steps :active="currentStep" finish-status="success" class="wizard-steps">
      <el-step title="基本信息" />
      <el-step title="范围配置" />
      <el-step title="目标配置" />
      <el-step title="指标配置" />
    </el-steps>

    <div class="wizard-content">
      <!-- 第一步：基本信息 -->
      <el-form
        v-show="currentStep === 0"
        ref="basicFormRef"
        :model="form"
        :rules="basicRules"
        label-width="120px"
        class="step-form"
      >
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="form.configName" placeholder="请输入配置名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="检查计划" prop="planId">
          <el-select
            v-model="form.planId"
            placeholder="请选择检查计划"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="plan in checkPlans"
              :key="plan.id"
              :label="plan.planName"
              :value="plan.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="配置描述">
          <el-input
            v-model="form.configDesc"
            type="textarea"
            :rows="3"
            placeholder="请输入配置描述"
            maxlength="500"
          />
        </el-form-item>
        <el-form-item label="缺检策略" prop="missingStrategy">
          <el-radio-group v-model="form.missingStrategy">
            <el-radio-button value="avg">
              <el-tooltip content="缺检部分用全局平均值填充">
                <span>平均扣分</span>
              </el-tooltip>
            </el-radio-button>
            <el-radio-button value="weighted">
              <el-tooltip content="仅用已有数据计算，不填充">
                <span>加权平均</span>
              </el-tooltip>
            </el-radio-button>
            <el-radio-button value="full_only">
              <el-tooltip content="只统计全覆盖的班级">
                <span>仅全覆盖</span>
              </el-tooltip>
            </el-radio-button>
            <el-radio-button value="penalty">
              <el-tooltip content="缺检部分用指定惩罚分填充">
                <span>缺检惩罚</span>
              </el-tooltip>
            </el-radio-button>
            <el-radio-button value="exempt">
              <el-tooltip content="缺检部分直接忽略">
                <span>缺检豁免</span>
              </el-tooltip>
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.missingStrategy === 'penalty'" label="惩罚分数">
          <el-input-number v-model="penaltyScore" :min="0" :max="100" :precision="1" />
          <span class="form-tip">缺检时每次检查计入的扣分</span>
        </el-form-item>
      </el-form>

      <!-- 第二步：范围配置 -->
      <el-form
        v-show="currentStep === 1"
        ref="scopeFormRef"
        :model="form"
        label-width="120px"
        class="step-form"
      >
        <el-form-item label="范围类型">
          <el-radio-group v-model="form.scopeType">
            <el-radio-button value="time">时间范围</el-radio-button>
            <el-radio-button value="record">记录选择</el-radio-button>
            <el-radio-button value="mixed">混合模式</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <template v-if="form.scopeType === 'time' || form.scopeType === 'mixed'">
          <el-form-item label="更新模式">
            <el-radio-group v-model="form.updateMode">
              <el-radio value="static">静态（固定时间范围）</el-radio>
              <el-radio value="dynamic">动态（自动包含新数据）</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="起始日期">
            <el-date-picker
              v-model="scopeStartDate"
              type="date"
              placeholder="选择起始日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          <el-form-item v-if="form.updateMode === 'static'" label="结束日期">
            <el-date-picker
              v-model="scopeEndDate"
              type="date"
              placeholder="选择结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          <el-form-item v-if="form.updateMode === 'dynamic'">
            <el-alert type="info" :closable="false">
              动态模式下，结束日期将自动设置为当天，每次查看都会包含最新数据。
            </el-alert>
          </el-form-item>
        </template>

        <template v-if="form.scopeType === 'record' || form.scopeType === 'mixed'">
          <el-form-item label="选择记录">
            <el-alert type="info" :closable="false" class="mb-10">
              可以手动选择要包含在分析中的检查记录（暂不支持，请使用时间范围模式）
            </el-alert>
          </el-form-item>
        </template>
      </el-form>

      <!-- 第三步：目标配置 -->
      <el-form
        v-show="currentStep === 2"
        ref="targetFormRef"
        :model="form"
        label-width="120px"
        class="step-form"
      >
        <el-form-item label="分析目标">
          <el-radio-group v-model="form.targetType" @change="handleTargetTypeChange">
            <el-radio-button value="plan_inherit">继承计划目标</el-radio-button>
            <el-radio-button value="all">全部班级</el-radio-button>
            <el-radio-button value="department">按部门</el-radio-button>
            <el-radio-button value="grade">按年级</el-radio-button>
            <el-radio-button value="custom">自定义</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- 继承计划目标说明 -->
        <template v-if="form.targetType === 'plan_inherit'">
          <el-form-item>
            <el-alert
              v-if="selectedPlanScope"
              type="success"
              :closable="false"
              show-icon
            >
              <template #title>
                将使用检查计划的目标范围配置
              </template>
              <div class="plan-scope-info">
                <span v-if="selectedPlanScope.type === 'all'">全部班级</span>
                <span v-else-if="selectedPlanScope.type === 'department'">按院系: {{ selectedPlanScope.count }} 个院系</span>
                <span v-else-if="selectedPlanScope.type === 'grade'">按年级: {{ selectedPlanScope.count }} 个年级</span>
                <span v-else-if="selectedPlanScope.type === 'custom'">自定义: {{ selectedPlanScope.count }} 个班级</span>
                <span v-else>未设置目标范围</span>
                <span v-if="selectedPlanScope.classCount" class="ml-10">（涵盖 {{ selectedPlanScope.classCount }} 个班级）</span>
              </div>
            </el-alert>
            <el-alert
              v-else
              type="warning"
              :closable="false"
              show-icon
            >
              当前计划未配置目标范围，将分析全部班级
            </el-alert>
          </el-form-item>
        </template>

        <template v-if="form.targetType === 'department'">
          <el-form-item label="选择部门">
            <el-tree-select
              v-model="targetDepartmentIds"
              :data="departmentTree"
              multiple
              check-strictly
              :render-after-expand="false"
              placeholder="选择部门"
              style="width: 100%"
            />
          </el-form-item>
        </template>

        <template v-if="form.targetType === 'grade'">
          <el-form-item label="选择年级">
            <el-select
              v-model="targetGradeIds"
              multiple
              placeholder="选择年级"
              style="width: 100%"
            >
              <el-option
                v-for="grade in grades"
                :key="grade.id"
                :label="grade.gradeName"
                :value="grade.id"
              />
            </el-select>
          </el-form-item>
        </template>

        <template v-if="form.targetType === 'custom'">
          <el-form-item label="选择班级">
            <el-select
              v-model="targetClassIds"
              multiple
              filterable
              placeholder="选择班级"
              style="width: 100%"
            >
              <el-option
                v-for="cls in classes"
                :key="cls.id"
                :label="`${cls.gradeName} - ${cls.className}`"
                :value="cls.id"
              />
            </el-select>
          </el-form-item>
        </template>

        <el-form-item v-if="form.targetType !== 'all'" label="排除班级">
          <el-select
            v-model="excludeClassIds"
            multiple
            filterable
            clearable
            placeholder="选择要排除的班级"
            style="width: 100%"
          >
            <el-option
              v-for="cls in classes"
              :key="cls.id"
              :label="`${cls.gradeName} - ${cls.className}`"
              :value="cls.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <!-- 第四步：指标配置 -->
      <div v-show="currentStep === 3" class="step-form">
        <el-alert type="info" :closable="false" class="mb-20">
          选择要在分析报告中显示的指标。您可以稍后在配置详情中调整每个指标的具体参数。
        </el-alert>

        <div class="metrics-selector">
          <el-checkbox-group v-model="selectedMetrics">
            <div class="metric-group">
              <div class="group-title">概览指标</div>
              <div class="metric-options">
                <el-checkbox value="overview">统计概览</el-checkbox>
                <el-checkbox value="total_score">总扣分</el-checkbox>
                <el-checkbox value="avg_score">平均扣分</el-checkbox>
                <el-checkbox value="check_count">检查次数</el-checkbox>
                <el-checkbox value="coverage_rate">覆盖率</el-checkbox>
              </div>
            </div>

            <div class="metric-group">
              <div class="group-title">排名分析</div>
              <div class="metric-options">
                <el-checkbox value="class_ranking">班级排名</el-checkbox>
                <el-checkbox value="grade_comparison">年级对比</el-checkbox>
                <el-checkbox value="department_comparison">院系对比</el-checkbox>
              </div>
            </div>

            <div class="metric-group">
              <div class="group-title">趋势分析</div>
              <div class="metric-options">
                <el-checkbox value="trend">扣分趋势</el-checkbox>
              </div>
            </div>

            <div class="metric-group">
              <div class="group-title">分布分析</div>
              <div class="metric-options">
                <el-checkbox value="category_distribution">类别分布</el-checkbox>
                <el-checkbox value="distribution">分数分布</el-checkbox>
              </div>
            </div>
          </el-checkbox-group>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button v-if="currentStep > 0" @click="prevStep">上一步</el-button>
        <el-button v-if="currentStep < 3" type="primary" @click="nextStep">下一步</el-button>
        <el-button v-if="currentStep === 3" type="primary" :loading="saving" @click="handleSave">
          保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getCheckPlanPage, getPlanTargetScope, getPlanTargetClasses, type CheckPlanListVO } from '@/api/checkPlan'
import { getDepartmentTree, type Department } from '@/api/department'
import { getGradePage, type Grade } from '@/api/grade'
import { getClassList } from '@/api/class'
import type { Class } from '@/types/class'
import {
  getConfigDetail,
  createConfig,
  updateConfig,
  type AnalysisConfig,
  type AnalysisMetric
} from '@/api/analysisConfig'

// Props & Emits
const props = defineProps<{
  visible: boolean
  configId?: number | string | null
  planId?: number | string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

// 计算属性
const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const isEdit = computed(() => !!props.configId)

// 状态
const currentStep = ref(0)
const saving = ref(false)
const basicFormRef = ref<FormInstance>()
const scopeFormRef = ref<FormInstance>()
const targetFormRef = ref<FormInstance>()

// 数据
const checkPlans = ref<CheckPlanListVO[]>([])
const departmentTree = ref<Department[]>([])
const grades = ref<Grade[]>([])
const classes = ref<Class[]>([])

// 表单数据
const form = reactive<Partial<AnalysisConfig>>({
  configName: '',
  planId: undefined,
  configDesc: '',
  scopeType: 'time',
  updateMode: 'static',
  missingStrategy: 'avg',
  targetType: 'all'
})

// 配置详情
const penaltyScore = ref(5)
const scopeStartDate = ref('')
const scopeEndDate = ref('')
const targetDepartmentIds = ref<number[]>([])
const targetGradeIds = ref<number[]>([])
const targetClassIds = ref<number[]>([])
const excludeClassIds = ref<number[]>([])
const selectedMetrics = ref<string[]>(['overview', 'class_ranking', 'trend', 'category_distribution'])

// 计划目标范围信息
interface PlanScopeInfo {
  type: string
  count: number
  classCount?: number
  config?: any
}
const selectedPlanScope = ref<PlanScopeInfo | null>(null)

// 表单验证规则
const basicRules: FormRules = {
  configName: [
    { required: true, message: '请输入配置名称', trigger: 'blur' },
    { max: 100, message: '配置名称不能超过100字符', trigger: 'blur' }
  ],
  planId: [
    { required: true, message: '请选择检查计划', trigger: 'change' }
  ],
  missingStrategy: [
    { required: true, message: '请选择缺检策略', trigger: 'change' }
  ]
}

// 加载基础数据
async function loadBaseData() {
  try {
    const [plansRes, deptRes, gradesRes, classesRes] = await Promise.all([
      getCheckPlanPage({ pageNum: 1, pageSize: 100, status: 1 }),
      getDepartmentTree(),
      getGradePage({ pageNum: 1, pageSize: 100 }),
      getClassList({ pageNum: 1, pageSize: 1000 })
    ])
    checkPlans.value = plansRes.data?.records || []
    departmentTree.value = deptRes.data || []
    grades.value = gradesRes.data?.records || []
    classes.value = classesRes.data?.records || []
  } catch (error) {
    console.error('加载基础数据失败:', error)
  }
}

// 加载计划目标范围配置
async function loadPlanScope(planId: number | string) {
  if (!planId) {
    selectedPlanScope.value = null
    return
  }

  try {
    const [scopeRes, classesRes] = await Promise.all([
      getPlanTargetScope(planId),
      getPlanTargetClasses(planId)
    ])

    const scopeData = scopeRes
    const targetClasses = classesRes || []

    if (scopeData?.scopeType && scopeData.scopeType !== 'all') {
      const scopeConfig = scopeData.scopeConfig ? JSON.parse(scopeData.scopeConfig) : {}
      let count = 0

      if (scopeData.scopeType === 'department') {
        count = scopeConfig.departmentIds?.length || 0
      } else if (scopeData.scopeType === 'grade') {
        count = scopeConfig.gradeIds?.length || 0
      } else if (scopeData.scopeType === 'custom') {
        count = scopeConfig.classIds?.length || 0
      }

      selectedPlanScope.value = {
        type: scopeData.scopeType,
        count,
        classCount: targetClasses.length,
        config: scopeConfig
      }
    } else {
      selectedPlanScope.value = {
        type: 'all',
        count: 0,
        classCount: targetClasses.length
      }
    }
  } catch (error) {
    console.error('加载计划目标范围失败:', error)
    selectedPlanScope.value = null
  }
}

// 目标类型变更处理
function handleTargetTypeChange(value: string) {
  if (value === 'plan_inherit' && form.planId) {
    loadPlanScope(form.planId)
  }
}

// 加载配置详情
async function loadConfig() {
  if (!props.configId) return

  try {
    const res = await getConfigDetail(props.configId)
    const config = res.data

    // 填充表单
    form.configName = config.configName
    form.planId = config.planId
    form.configDesc = config.configDesc
    form.scopeType = config.scopeType
    form.updateMode = config.updateMode
    form.missingStrategy = config.missingStrategy
    form.targetType = config.targetType

    // 填充配置详情
    if (config.scopeConfig) {
      scopeStartDate.value = config.scopeConfig.startDate || ''
      scopeEndDate.value = config.scopeConfig.endDate || ''
    }

    if (config.targetConfig) {
      targetDepartmentIds.value = config.targetConfig.departmentIds || []
      targetGradeIds.value = config.targetConfig.gradeIds || []
      targetClassIds.value = config.targetConfig.classIds || []
      excludeClassIds.value = config.targetConfig.excludeClassIds || []
    }

    if (config.missingStrategyConfig?.penaltyScore) {
      penaltyScore.value = config.missingStrategyConfig.penaltyScore
    }

    // 填充指标
    if (config.metrics?.length) {
      selectedMetrics.value = config.metrics.map(m => m.metricType)
    }
  } catch (error) {
    ElMessage.error('加载配置详情失败')
  }
}

// 上一步
function prevStep() {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

// 下一步
async function nextStep() {
  if (currentStep.value === 0) {
    const valid = await basicFormRef.value?.validate().catch(() => false)
    if (!valid) return
  }

  if (currentStep.value < 3) {
    currentStep.value++
  }
}

// 保存
async function handleSave() {
  saving.value = true

  try {
    // 构建配置数据
    const configData: AnalysisConfig = {
      ...form,
      planId: form.planId!,
      configName: form.configName!,
      scopeType: form.scopeType as 'time' | 'record' | 'mixed',
      updateMode: form.updateMode as 'static' | 'dynamic',
      missingStrategy: form.missingStrategy as 'avg' | 'weighted' | 'full_only' | 'penalty' | 'exempt',
      targetType: form.targetType as 'all' | 'department' | 'grade' | 'custom',
      scopeConfig: {
        startDate: scopeStartDate.value,
        endDate: scopeEndDate.value
      },
      targetConfig: buildTargetConfig(),
      missingStrategyConfig: form.missingStrategy === 'penalty'
        ? { penaltyScore: penaltyScore.value }
        : undefined,
      metrics: buildMetrics()
    }

    if (isEdit.value) {
      await updateConfig(props.configId!, configData)
      ElMessage.success('更新成功')
    } else {
      await createConfig(configData)
      ElMessage.success('创建成功')
    }

    emit('success')
    handleClose()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 构建目标配置
function buildTargetConfig() {
  switch (form.targetType) {
    case 'plan_inherit':
      // 继承计划目标配置
      if (selectedPlanScope.value?.config) {
        return {
          ...selectedPlanScope.value.config,
          inheritFromPlan: true
        }
      }
      return { inheritFromPlan: true }
    case 'department':
      return {
        departmentIds: targetDepartmentIds.value,
        excludeClassIds: excludeClassIds.value
      }
    case 'grade':
      return {
        gradeIds: targetGradeIds.value,
        excludeClassIds: excludeClassIds.value
      }
    case 'custom':
      return {
        classIds: targetClassIds.value
      }
    default:
      return {}
  }
}

// 构建指标配置
function buildMetrics(): AnalysisMetric[] {
  const metricConfigs: Record<string, Partial<AnalysisMetric>> = {
    overview: { metricCode: 'overview', metricName: '统计概览', chartType: 'number' },
    total_score: { metricCode: 'total_score', metricName: '总扣分', chartType: 'number' },
    avg_score: { metricCode: 'avg_score', metricName: '平均扣分', chartType: 'number' },
    check_count: { metricCode: 'check_count', metricName: '检查次数', chartType: 'number' },
    coverage_rate: { metricCode: 'coverage_rate', metricName: '覆盖率', chartType: 'number', unit: '%' },
    class_ranking: { metricCode: 'class_ranking', metricName: '班级排名', chartType: 'table' },
    grade_comparison: { metricCode: 'grade_comparison', metricName: '年级对比', chartType: 'bar' },
    department_comparison: { metricCode: 'department_comparison', metricName: '院系对比', chartType: 'bar' },
    trend: { metricCode: 'trend', metricName: '扣分趋势', chartType: 'line' },
    category_distribution: { metricCode: 'category_distribution', metricName: '类别分布', chartType: 'pie' },
    distribution: { metricCode: 'distribution', metricName: '分数分布', chartType: 'bar' }
  }

  return selectedMetrics.value.map((type, index) => ({
    metricCode: metricConfigs[type]?.metricCode || type,
    metricName: metricConfigs[type]?.metricName || type,
    metricType: type as any,
    chartType: metricConfigs[type]?.chartType as any || 'number',
    unit: metricConfigs[type]?.unit,
    isEnabled: true,
    displayOrder: index
  }))
}

// 关闭
function handleClose() {
  visible.value = false
  resetForm()
}

// 重置表单
function resetForm() {
  currentStep.value = 0
  form.configName = ''
  form.planId = undefined
  form.configDesc = ''
  form.scopeType = 'time'
  form.updateMode = 'static'
  form.missingStrategy = 'avg'
  form.targetType = 'plan_inherit'  // 默认继承计划目标
  penaltyScore.value = 5
  scopeStartDate.value = ''
  scopeEndDate.value = ''
  targetDepartmentIds.value = []
  targetGradeIds.value = []
  targetClassIds.value = []
  excludeClassIds.value = []
  selectedMetrics.value = ['overview', 'class_ranking', 'trend', 'category_distribution']
  selectedPlanScope.value = null
}

// 监听显示状态
watch(() => props.visible, (val) => {
  if (val) {
    loadBaseData()
    if (props.planId) {
      form.planId = props.planId
      // 自动加载计划目标范围
      loadPlanScope(props.planId)
    }
    if (props.configId) {
      loadConfig()
    }
  }
})

// 监听计划选择变化
watch(() => form.planId, (newPlanId) => {
  if (newPlanId && form.targetType === 'plan_inherit') {
    loadPlanScope(newPlanId)
  }
})

// 初始化
onMounted(() => {
  loadBaseData()
})
</script>

<style scoped lang="scss">
.wizard-steps {
  margin-bottom: 30px;
  padding: 0 40px;
}

.wizard-content {
  min-height: 400px;
  padding: 20px;

  .step-form {
    max-width: 700px;
    margin: 0 auto;
  }

  .form-tip {
    margin-left: 10px;
    color: #909399;
    font-size: 12px;
  }

  .mb-10 {
    margin-bottom: 10px;
  }

  .mb-20 {
    margin-bottom: 20px;
  }
}

.metrics-selector {
  .metric-group {
    margin-bottom: 20px;
    padding: 15px;
    background: #f5f7fa;
    border-radius: 8px;

    .group-title {
      font-weight: 600;
      margin-bottom: 12px;
      color: #303133;
    }

    .metric-options {
      display: flex;
      flex-wrap: wrap;
      gap: 20px;

      .el-checkbox {
        margin-right: 0;
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
