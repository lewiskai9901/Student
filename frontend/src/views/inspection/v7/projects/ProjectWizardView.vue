<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useInspExecutionStore } from '@/stores/insp/inspExecutionStore'
import { useInspTemplateStore } from '@/stores/insp/inspTemplateStore'
import { templateModuleRefApi } from '@/api/insp/templateModuleRef'
import type { InspTemplate, TemplateModuleRef } from '@/types/insp/template'
import { TargetTypeConfig, type TargetType } from '@/types/insp/enums'

const router = useRouter()
const executionStore = useInspExecutionStore()
const templateStore = useInspTemplateStore()

const currentStep = ref(0)
const submitting = ref(false)
const templates = ref<InspTemplate[]>([])
const moduleRefs = ref<TemplateModuleRef[]>([])
const loadingRefs = ref(false)

const steps = [
  { title: '基本信息', description: '项目名称和描述' },
  { title: '模板选择', description: '选择检查模板' },
  { title: '范围配置', description: '设置检查范围' },
  { title: '周期配置', description: '检查时间安排' },
  { title: '确认创建', description: '确认并提交' },
]

const form = reactive({
  // Step 1: Basic info
  projectName: '',
  projectCode: '',
  description: '',
  // Step 2: Template
  templateId: undefined as number | undefined,
  // Step 3: Scope
  scopeType: 'ORG_UNIT' as string,
  scopeIds: [] as number[],
  // Step 4: Schedule
  startDate: '',
  endDate: '',
  cycleType: 'DAILY' as string,
  inspectionTime: '',
})

const canNext = computed(() => {
  switch (currentStep.value) {
    case 0: return !!form.projectName && !!form.projectCode
    case 1: return !!form.templateId
    case 2: return !!form.scopeType
    case 3: return !!form.startDate
    default: return true
  }
})

function nextStep() {
  if (currentStep.value < steps.length - 1) {
    currentStep.value++
  }
}

function prevStep() {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

async function handleSubmit() {
  submitting.value = true
  try {
    await executionStore.addProject({
      projectName: form.projectName,
      templateId: form.templateId!,
      startDate: form.startDate,
    })
    ElMessage.success('项目创建成功')
    router.push('/inspection/v7/projects')
  } catch (e: any) {
    ElMessage.error(e.message || '创建项目失败')
  } finally {
    submitting.value = false
  }
}

const selectedTemplate = computed(() =>
  templates.value.find(t => t.id === form.templateId)
)

function getTemplateName(templateId: number): string {
  return templates.value.find(t => t.id === templateId)?.templateName ?? `模板#${templateId}`
}

function getTemplateTargetType(templateId: number): TargetType | undefined {
  return templates.value.find(t => t.id === templateId)?.targetType
}

watch(() => form.templateId, async (newId) => {
  moduleRefs.value = []
  if (!newId) return
  loadingRefs.value = true
  try {
    moduleRefs.value = await templateModuleRefApi.list(newId)
  } catch { /* ignore */ }
  loadingRefs.value = false
})

async function loadTemplates() {
  try {
    const result = await templateStore.loadTemplates({ page: 1, size: 100, status: 'PUBLISHED' })
    templates.value = result.records
  } catch { /* ignore */ }
}

onMounted(() => {
  loadTemplates()
})
</script>

<template>
  <div class="p-5 space-y-5">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">创建检查项目</h2>
      <el-button @click="router.back()">返回</el-button>
    </div>

    <!-- Steps -->
    <el-steps :active="currentStep" finish-status="success" align-center>
      <el-step v-for="(step, idx) in steps" :key="idx" :title="step.title" :description="step.description" />
    </el-steps>

    <!-- Step Content -->
    <el-card shadow="never" class="min-h-[300px]">
      <!-- Step 0: Basic Info -->
      <div v-show="currentStep === 0">
        <el-form label-width="100px" class="max-w-lg">
          <el-form-item label="项目名称" required>
            <el-input v-model="form.projectName" placeholder="输入项目名称" />
          </el-form-item>
          <el-form-item label="项目编码" required>
            <el-input v-model="form.projectCode" placeholder="输入项目编码" />
          </el-form-item>
          <el-form-item label="项目描述">
            <el-input v-model="form.description" type="textarea" :rows="3" placeholder="项目描述（可选）" />
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 1: Template Selection -->
      <div v-show="currentStep === 1">
        <el-form label-width="100px" class="max-w-lg">
          <el-form-item label="检查模板" required>
            <el-select v-model="form.templateId" placeholder="选择检查模板" class="w-full">
              <el-option
                v-for="tpl in templates"
                :key="tpl.id"
                :label="tpl.templateName"
                :value="tpl.id"
              />
            </el-select>
          </el-form-item>
          <div v-if="templates.length === 0" class="text-sm text-gray-400 pl-[100px]">
            暂无已发布的模板，请先发布模板
          </div>
        </el-form>

        <!-- 子模板预览 -->
        <div v-if="form.templateId && moduleRefs.length > 0" class="mt-4 ml-[100px] max-w-lg">
          <div class="text-sm font-medium text-gray-600 mb-2">子模板结构预览</div>
          <div class="border border-gray-200 rounded-md p-3 bg-gray-50 space-y-2">
            <div class="flex items-center gap-2 text-sm text-gray-700 font-medium">
              <span>{{ selectedTemplate?.templateName }}</span>
              <el-tag v-if="selectedTemplate?.targetType" size="small" type="info">
                {{ TargetTypeConfig[selectedTemplate.targetType]?.label }}
              </el-tag>
            </div>
            <div
              v-for="ref in moduleRefs"
              :key="ref.id"
              class="flex items-center gap-2 pl-5 text-sm text-gray-600"
            >
              <span class="text-gray-400">└</span>
              <span>{{ getTemplateName(ref.moduleTemplateId) }}</span>
              <el-tag size="small" type="info">
                {{ TargetTypeConfig[getTemplateTargetType(ref.moduleTemplateId) as TargetType]?.label || '未知' }}
              </el-tag>
              <span class="text-gray-400 text-xs">权重 {{ ref.weight }}%</span>
            </div>
          </div>
          <div class="text-xs text-gray-400 mt-1">
            创建项目时将自动生成 {{ moduleRefs.length }} 个子项目
          </div>
        </div>
        <div v-if="loadingRefs" class="mt-4 ml-[100px] text-sm text-gray-400">
          加载子模板信息...
        </div>
      </div>

      <!-- Step 2: Scope -->
      <div v-show="currentStep === 2">
        <el-form label-width="100px" class="max-w-lg">
          <el-form-item label="范围类型" required>
            <el-select v-model="form.scopeType" class="w-full">
              <el-option label="组织单元" value="ORG_UNIT" />
              <el-option label="场所" value="PLACE" />
              <el-option label="自定义" value="CUSTOM" />
            </el-select>
          </el-form-item>
          <el-form-item label="范围说明">
            <span class="text-sm text-gray-500">
              具体范围将在项目创建后在详情页配置
            </span>
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 3: Schedule -->
      <div v-show="currentStep === 3">
        <el-form label-width="100px" class="max-w-lg">
          <el-form-item label="开始日期" required>
            <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择开始日期" class="w-full" />
          </el-form-item>
          <el-form-item label="结束日期">
            <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="选择结束日期（可选）" class="w-full" />
          </el-form-item>
          <el-form-item label="检查周期">
            <el-select v-model="form.cycleType" class="w-full">
              <el-option label="每日" value="DAILY" />
              <el-option label="每周" value="WEEKLY" />
              <el-option label="每月" value="MONTHLY" />
              <el-option label="一次性" value="ONCE" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 4: Confirm -->
      <div v-show="currentStep === 4">
        <el-descriptions :column="1" border class="max-w-lg">
          <el-descriptions-item label="项目名称">{{ form.projectName }}</el-descriptions-item>
          <el-descriptions-item label="项目编码">{{ form.projectCode }}</el-descriptions-item>
          <el-descriptions-item label="项目描述">{{ form.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="检查模板">
            {{ templates.find(t => t.id === form.templateId)?.templateName ?? '-' }}
            <el-tag v-if="moduleRefs.length > 0" size="small" class="ml-2">
              含 {{ moduleRefs.length }} 个子模板
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="范围类型">{{ form.scopeType }}</el-descriptions-item>
          <el-descriptions-item label="开始日期">{{ form.startDate }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ form.endDate || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="检查周期">{{ form.cycleType }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>

    <!-- Navigation Buttons -->
    <div class="flex justify-between">
      <el-button v-if="currentStep > 0" @click="prevStep">上一步</el-button>
      <span v-else />
      <div class="flex gap-2">
        <el-button v-if="currentStep < steps.length - 1" type="primary" :disabled="!canNext" @click="nextStep">
          下一步
        </el-button>
        <el-button v-if="currentStep === steps.length - 1" type="primary" :loading="submitting" @click="handleSubmit">
          确认创建
        </el-button>
      </div>
    </div>
  </div>
</template>
