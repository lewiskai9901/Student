<template>
  <div class="v6-project-create">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
          <span>创建检查项目</span>
        </div>
      </template>

      <el-steps :active="currentStep" finish-status="success" align-center class="steps-container">
        <el-step title="基本信息" description="项目名称与模板" />
        <el-step title="检查范围" description="选择检查对象" />
        <el-step title="打分设置" description="配置打分规则" />
        <el-step title="周期设置" description="检查周期与分配" />
        <el-step title="完成" description="确认创建" />
      </el-steps>

      <div class="form-container">
        <!-- Step 1: 基本信息 -->
        <el-form v-show="currentStep === 0" ref="step1FormRef" :model="formData" :rules="step1Rules" label-width="120px">
          <el-form-item label="项目编号" prop="projectCode">
            <el-input v-model="formData.projectCode" placeholder="请输入项目编号，如: DORM_CHECK_2024">
              <template #append>
                <el-button @click="generateCode">自动生成</el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="项目名称" prop="projectName">
            <el-input v-model="formData.projectName" placeholder="请输入项目名称，如: 2024年春季宿舍检查" />
          </el-form-item>
          <el-form-item label="检查模板" prop="templateId">
            <el-select v-model="formData.templateId" placeholder="请选择检查模板" style="width: 100%"
                       @change="onTemplateChange" filterable>
              <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id">
                <span>{{ t.name }}</span>
                <span class="template-desc" v-if="t.description">{{ t.description }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="项目描述">
            <el-input v-model="formData.description" type="textarea" rows="3" placeholder="请输入项目描述" />
          </el-form-item>
        </el-form>

        <!-- Step 2: 检查范围 -->
        <div v-show="currentStep === 1" class="scope-step">
          <el-form ref="step2FormRef" :model="formData" :rules="step2Rules" label-width="120px">
            <el-form-item label="目标类型" prop="scopeType">
              <el-radio-group v-model="formData.scopeType" @change="onScopeTypeChange">
                <el-radio-button v-for="c in entityCategories" :key="c.code" :value="c.code">
                  {{ c.name }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="实体类型" prop="entityTypeCode">
              <el-select v-model="formData.entityTypeCode" placeholder="选择具体类型" style="width: 100%"
                         @change="onEntityTypeChange">
                <el-option v-for="t in filteredEntityTypes" :key="t.typeCode"
                           :label="t.typeName" :value="t.typeCode" />
              </el-select>
            </el-form-item>

            <el-form-item label="选择方式">
              <el-radio-group v-model="scopeSelectionMode">
                <el-radio value="tree">组织树选择</el-radio>
                <el-radio value="group">使用分组</el-radio>
                <el-radio value="manual">手动输入</el-radio>
              </el-radio-group>
            </el-form-item>

            <!-- 组织树选择 -->
            <el-form-item v-if="scopeSelectionMode === 'tree'" label="选择目标">
              <div class="tree-selector">
                <el-tree
                  ref="scopeTreeRef"
                  :data="scopeTreeData"
                  show-checkbox
                  node-key="id"
                  :props="{ label: 'unitName', children: 'children' }"
                  @check-change="onTreeCheckChange"
                  default-expand-all
                />
              </div>
            </el-form-item>

            <!-- 使用分组 -->
            <el-form-item v-if="scopeSelectionMode === 'group'" label="选择分组">
              <el-select v-model="selectedGroupId" placeholder="选择预定义分组" style="width: 100%"
                         @change="onGroupChange">
                <el-option v-for="g in filteredGroups" :key="g.id"
                           :label="`${g.groupName} (${g.cachedMemberCount || 0}个成员)`" :value="g.id" />
              </el-select>
            </el-form-item>

            <!-- 手动输入 -->
            <el-form-item v-if="scopeSelectionMode === 'manual'" label="目标ID" prop="scopeIds">
              <el-input v-model="formData.scopeIds" type="textarea" rows="4"
                        placeholder="输入目标ID，每行一个" />
            </el-form-item>

            <el-form-item label="已选目标">
              <el-tag v-for="id in selectedScopeIds" :key="id" closable @close="removeScopeId(id)"
                      class="scope-tag">
                {{ id }}
              </el-tag>
              <span v-if="selectedScopeIds.length === 0" class="no-selection">未选择任何目标</span>
            </el-form-item>
          </el-form>
        </div>

        <!-- Step 3: 打分设置 -->
        <el-form v-show="currentStep === 2" ref="step3FormRef" :model="formData" :rules="step3Rules" label-width="120px">
          <el-form-item label="打分模式" prop="scoringMode">
            <el-select v-model="formData.scoringMode" placeholder="选择打分模式" style="width: 100%">
              <el-option v-for="m in scoringModes" :key="m.value" :label="m.label" :value="m.value">
                <div>
                  <span>{{ m.label }}</span>
                  <span class="mode-desc">{{ m.description }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="基础分" prop="baseScore"
                        v-if="['DEDUCTION', 'BASE_SCORE', 'HYBRID'].includes(formData.scoringMode)">
            <el-input-number v-model="formData.baseScore" :min="0" :max="200" :precision="2" />
            <span class="form-tip">扣分制检查的起始分数</span>
          </el-form-item>

          <el-form-item label="最高分" prop="maxScore"
                        v-if="['ADDITION', 'RATING'].includes(formData.scoringMode)">
            <el-input-number v-model="formData.maxScore" :min="0" :max="200" :precision="2" />
          </el-form-item>

          <el-form-item label="最低分" prop="minScore">
            <el-input-number v-model="formData.minScore" :min="0" :max="formData.baseScore" :precision="2" />
            <span class="form-tip">分数不能低于此值</span>
          </el-form-item>

          <el-form-item label="允许小数">
            <el-switch v-model="formData.allowDecimal" />
            <span class="form-tip">是否允许小数分数</span>
          </el-form-item>
        </el-form>

        <!-- Step 4: 周期设置 -->
        <el-form v-show="currentStep === 3" ref="step4FormRef" :model="formData" :rules="step4Rules" label-width="120px">
          <el-form-item label="周期类型" prop="cycleType">
            <el-radio-group v-model="formData.cycleType">
              <el-radio value="DAILY">每日</el-radio>
              <el-radio value="WEEKLY">每周</el-radio>
              <el-radio value="MONTHLY">每月</el-radio>
              <el-radio value="SEMESTER">学期</el-radio>
              <el-radio value="CUSTOM">自定义</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="有效期" prop="dateRange">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              @change="onDateRangeChange"
            />
          </el-form-item>

          <el-form-item label="检查员分配">
            <el-radio-group v-model="formData.inspectorAssignmentMode">
              <el-radio value="FREE">
                <span>自由领取</span>
                <el-tooltip content="检查员可自由选择任务执行" placement="top">
                  <el-icon><QuestionFilled /></el-icon>
                </el-tooltip>
              </el-radio>
              <el-radio value="ASSIGNED">
                <span>指定分配</span>
                <el-tooltip content="由管理员分配任务给指定检查员" placement="top">
                  <el-icon><QuestionFilled /></el-icon>
                </el-tooltip>
              </el-radio>
              <el-radio value="HYBRID">
                <span>混合模式</span>
                <el-tooltip content="部分指定分配，部分自由领取" placement="top">
                  <el-icon><QuestionFilled /></el-icon>
                </el-tooltip>
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="自动创建任务">
            <el-switch v-model="formData.autoCreateTasks" />
            <span class="form-tip">根据周期自动创建检查任务</span>
          </el-form-item>

          <el-form-item label="任务提醒">
            <el-switch v-model="formData.enableReminder" />
            <span class="form-tip">检查任务到期前发送提醒</span>
          </el-form-item>
        </el-form>

        <!-- Step 5: 完成 -->
        <div v-show="currentStep === 4" class="complete-step">
          <div v-if="!createdProjectId" class="summary-section">
            <h3>创建确认</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="项目编号">{{ formData.projectCode }}</el-descriptions-item>
              <el-descriptions-item label="项目名称">{{ formData.projectName }}</el-descriptions-item>
              <el-descriptions-item label="检查模板">{{ selectedTemplateName }}</el-descriptions-item>
              <el-descriptions-item label="目标类型">{{ getScopeTypeName(formData.scopeType) }}</el-descriptions-item>
              <el-descriptions-item label="检查目标数">{{ selectedScopeIds.length }}个</el-descriptions-item>
              <el-descriptions-item label="打分模式">{{ getScoringModeName(formData.scoringMode) }}</el-descriptions-item>
              <el-descriptions-item label="基础分">{{ formData.baseScore }}</el-descriptions-item>
              <el-descriptions-item label="周期类型">{{ getCycleTypeName(formData.cycleType) }}</el-descriptions-item>
              <el-descriptions-item label="有效期">{{ formData.startDate }} 至 {{ formData.endDate || '无限期' }}</el-descriptions-item>
              <el-descriptions-item label="分配模式">{{ getAssignmentModeName(formData.inspectorAssignmentMode) }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <el-result v-else icon="success" title="项目创建成功" sub-title="您可以继续配置项目或返回列表">
            <template #extra>
              <el-button type="primary" @click="goToConfig">配置项目</el-button>
              <el-button @click="createAnother">继续创建</el-button>
              <el-button @click="goBack">返回列表</el-button>
            </template>
          </el-result>
        </div>
      </div>

      <div class="step-actions" v-if="currentStep < 4 || !createdProjectId">
        <el-button @click="prevStep" v-if="currentStep > 0">上一步</el-button>
        <el-button type="primary" @click="nextStep" v-if="currentStep < 4">下一步</el-button>
        <el-button type="primary" @click="handleSubmit" v-if="currentStep === 4 && !createdProjectId"
                   :loading="submitting">确认创建</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, QuestionFilled } from '@element-plus/icons-vue'
import { v6ProjectApi } from '@/api/v6Inspection'
import { orgUnitApi } from '@/api/organization'

// 简化的类型定义（V6 EntityType已移除，使用简化版本）
type EntityCategory = 'ORGANIZATION' | 'SPACE' | 'USER'
interface EntityType {
  typeCode: string
  typeName: string
  category: EntityCategory
  isLeaf: boolean
}
interface EntityGroup {
  id: number
  groupName: string
  entityCategory: EntityCategory
  cachedMemberCount?: number
}

const router = useRouter()
const currentStep = ref(0)
const submitting = ref(false)
const createdProjectId = ref<number | null>(null)

const step1FormRef = ref()
const step2FormRef = ref()
const step3FormRef = ref()
const step4FormRef = ref()
const scopeTreeRef = ref()

// 数据
const templates = ref<any[]>([])
const entityTypes = ref<EntityType[]>([])
const entityGroups = ref<EntityGroup[]>([])
const entityCategories = ref<{code: EntityCategory, name: string}[]>([
  { code: 'ORGANIZATION', name: '组织' },
  { code: 'SPACE', name: '场所' },
  { code: 'USER', name: '用户' }
])

// 范围选择
const scopeSelectionMode = ref<'tree' | 'group' | 'manual'>('tree')
const selectedGroupId = ref<number | null>(null)
const scopeTreeData = ref<any[]>([])
const selectedScopeIds = ref<number[]>([])
const dateRange = ref<[string, string] | null>(null)

// 打分模式选项
const scoringModes = [
  { value: 'DEDUCTION', label: '扣分制', description: '从基础分扣除' },
  { value: 'ADDITION', label: '加分制', description: '从0分累加' },
  { value: 'BASE_SCORE', label: '基准分制', description: '可加可减' },
  { value: 'RATING', label: '等级评定', description: '优良中差等级' },
  { value: 'GRADE', label: 'ABC等级', description: 'A/B/C等级' },
  { value: 'PASS_FAIL', label: '合格/不合格', description: '二元判定' },
  { value: 'CHECKLIST', label: '清单打卡', description: '完成/未完成' },
  { value: 'HYBRID', label: '混合模式', description: '多种模式组合' }
]

const formData = reactive({
  projectCode: '',
  projectName: '',
  templateId: undefined as number | undefined,
  description: '',
  scopeType: 'ORGANIZATION' as EntityCategory,
  entityTypeCode: '',
  scopeIds: '',
  scoringMode: 'DEDUCTION',
  baseScore: 100,
  maxScore: 100,
  minScore: 0,
  allowDecimal: false,
  cycleType: 'DAILY',
  startDate: '',
  endDate: '',
  inspectorAssignmentMode: 'FREE',
  autoCreateTasks: true,
  enableReminder: true
})

// 计算属性
const filteredEntityTypes = computed(() => {
  return entityTypes.value.filter(t => t.category === formData.scopeType && t.isLeaf)
})

const filteredGroups = computed(() => {
  return entityGroups.value.filter(g => g.entityCategory === formData.scopeType)
})

const selectedTemplateName = computed(() => {
  const t = templates.value.find(t => t.id === formData.templateId)
  return t ? t.name : ''
})

// 表单验证规则
const step1Rules = {
  projectCode: [{ required: true, message: '请输入项目编号', trigger: 'blur' }],
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  templateId: [{ required: true, message: '请选择检查模板', trigger: 'change' }]
}

const step2Rules = {
  scopeType: [{ required: true, message: '请选择目标类型', trigger: 'change' }]
}

const step3Rules = {
  scoringMode: [{ required: true, message: '请选择打分模式', trigger: 'change' }]
}

const step4Rules = {
  cycleType: [{ required: true, message: '请选择周期类型', trigger: 'change' }]
}

// 方法
const goBack = () => {
  router.push('/inspection/v6/projects')
}

const generateCode = () => {
  const prefix = formData.scopeType === 'ORGANIZATION' ? 'ORG' :
                 formData.scopeType === 'SPACE' ? 'SPACE' : 'USER'
  const date = new Date().toISOString().slice(0, 10).replace(/-/g, '')
  const random = Math.random().toString(36).substring(2, 6).toUpperCase()
  formData.projectCode = `${prefix}_${date}_${random}`
}

const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

const nextStep = async () => {
  const formRefs = [step1FormRef, step2FormRef, step3FormRef, step4FormRef]
  const currentFormRef = formRefs[currentStep.value]

  if (currentFormRef?.value) {
    try {
      await currentFormRef.value.validate()
    } catch (error) {
      return
    }
  }

  // 特殊验证
  if (currentStep.value === 1 && selectedScopeIds.value.length === 0) {
    ElMessage.warning('请选择至少一个检查目标')
    return
  }

  currentStep.value++
}

const handleSubmit = async () => {
  try {
    submitting.value = true

    const submitData = {
      ...formData,
      scopeIds: selectedScopeIds.value.join(',')
    }

    const res = await v6ProjectApi.create(submitData)
    createdProjectId.value = res.id
    ElMessage.success('项目创建成功')
  } catch (error: any) {
    ElMessage.error(error.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

const goToConfig = () => {
  if (createdProjectId.value) {
    router.push(`/inspection/v6/projects/${createdProjectId.value}/config`)
  }
}

const createAnother = () => {
  createdProjectId.value = null
  currentStep.value = 0
  Object.assign(formData, {
    projectCode: '',
    projectName: '',
    templateId: undefined,
    description: '',
    scopeType: 'ORGANIZATION',
    entityTypeCode: '',
    scopeIds: '',
    scoringMode: 'DEDUCTION',
    baseScore: 100,
    maxScore: 100,
    minScore: 0,
    allowDecimal: false,
    cycleType: 'DAILY',
    startDate: '',
    endDate: '',
    inspectorAssignmentMode: 'FREE',
    autoCreateTasks: true,
    enableReminder: true
  })
  selectedScopeIds.value = []
  dateRange.value = null
}

const onTemplateChange = (templateId: number) => {
  const template = templates.value.find(t => t.id === templateId)
  if (template && !formData.projectName) {
    formData.projectName = `${template.name}-${new Date().toISOString().slice(0, 10)}`
  }
}

const onScopeTypeChange = () => {
  formData.entityTypeCode = ''
  selectedScopeIds.value = []
  loadScopeTreeData()
}

const onEntityTypeChange = () => {
  selectedScopeIds.value = []
  loadScopeTreeData()
}

const onTreeCheckChange = () => {
  const checkedKeys = scopeTreeRef.value?.getCheckedKeys(true) || []
  selectedScopeIds.value = checkedKeys
}

const onGroupChange = async (groupId: number) => {
  // 分组功能暂时禁用（V6 EntityGroup API已移除）
  console.warn('分组功能暂时不可用，请使用树选择或手动输入')
  selectedScopeIds.value = []
}

const removeScopeId = (id: number) => {
  selectedScopeIds.value = selectedScopeIds.value.filter(i => i !== id)
  scopeTreeRef.value?.setCheckedKeys(selectedScopeIds.value)
}

const onDateRangeChange = (val: [string, string] | null) => {
  if (val) {
    formData.startDate = val[0]
    formData.endDate = val[1]
  } else {
    formData.startDate = ''
    formData.endDate = ''
  }
}

const loadScopeTreeData = async () => {
  try {
    if (formData.scopeType === 'ORGANIZATION') {
      // axios拦截器已解包响应，直接返回数组
      const treeData = await orgUnitApi.getTree()
      scopeTreeData.value = treeData || []
    } else {
      // TODO: 加载场所或用户树
      scopeTreeData.value = []
    }
  } catch (error) {
    console.error('加载树数据失败:', error)
  }
}

// 辅助方法
const getScopeTypeName = (type: string) => {
  const names: Record<string, string> = {
    'ORGANIZATION': '组织',
    'SPACE': '场所',
    'USER': '用户'
  }
  return names[type] || type
}

const getScoringModeName = (mode: string) => {
  const m = scoringModes.find(s => s.value === mode)
  return m ? m.label : mode
}

const getCycleTypeName = (type: string) => {
  const names: Record<string, string> = {
    'DAILY': '每日',
    'WEEKLY': '每周',
    'MONTHLY': '每月',
    'SEMESTER': '学期',
    'CUSTOM': '自定义'
  }
  return names[type] || type
}

const getAssignmentModeName = (mode: string) => {
  const names: Record<string, string> = {
    'FREE': '自由领取',
    'ASSIGNED': '指定分配',
    'HYBRID': '混合模式'
  }
  return names[mode] || mode
}

// 加载数据
const loadData = async () => {
  try {
    // 模板数据暂时使用默认值（V6模板API待实现）
    templates.value = [
      { id: 1, name: '日常卫生检查模板', description: '用于日常卫生检查' },
      { id: 2, name: '宿舍安全检查模板', description: '用于宿舍安全检查' },
      { id: 3, name: '教室纪律检查模板', description: '用于教室纪律检查' }
    ]

    // 简化的实体类型（V6 EntityType系统已移除）
    entityTypes.value = [
      { typeCode: 'CLASS', typeName: '班级', category: 'ORGANIZATION', isLeaf: true },
      { typeCode: 'DORMITORY', typeName: '宿舍', category: 'SPACE', isLeaf: true },
      { typeCode: 'CLASSROOM', typeName: '教室', category: 'SPACE', isLeaf: true },
      { typeCode: 'STUDENT', typeName: '学生', category: 'USER', isLeaf: true }
    ]

    // 暂时不使用分组功能
    entityGroups.value = []

    // 加载组织树
    loadScopeTreeData()
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.v6-project-create {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.steps-container {
  margin: 30px 0;
}

.form-container {
  max-width: 700px;
  margin: 0 auto;
  padding: 20px 0;
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.complete-step {
  text-align: center;
  padding: 40px 0;
}

.summary-section h3 {
  margin-bottom: 20px;
  color: #303133;
}

.template-desc {
  color: #909399;
  font-size: 12px;
  margin-left: 10px;
}

.mode-desc {
  color: #909399;
  font-size: 12px;
  margin-left: 10px;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  margin-left: 10px;
}

.tree-selector {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
}

.scope-tag {
  margin: 2px 4px;
}

.no-selection {
  color: #909399;
  font-size: 14px;
}

.scope-step {
  margin-bottom: 20px;
}
</style>
