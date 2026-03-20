<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Trash2, Play, Calendar, Clock } from 'lucide-vue-next'
import { inspPlanApi } from '@/api/insp/project'
import { inspTemplateApi } from '@/api/insp/template'
import type { InspectionPlan, CreatePlanRequest, TemplateSection } from '@/types/insp/template'
import { CycleTypeConfig, type CycleType } from '@/types/insp/enums'

const props = defineProps<{
  projectId: number
}>()

// ========== State ==========
const loading = ref(false)
const plans = ref<InspectionPlan[]>([])
const sections = ref<TemplateSection[]>([])
const dialogVisible = ref(false)
const editingPlan = ref<InspectionPlan | null>(null)
const saving = ref(false)

const form = ref({
  planName: '',
  sectionIds: [] as number[],
  scheduleMode: 'REGULAR' as 'REGULAR' | 'ON_DEMAND',
  cycleType: 'DAILY' as string,
  frequency: 1,
  scheduleDays: '' as string,
  timeSlots: '' as string,
  skipHolidays: false,
})

const scheduleModeOptions = [
  { value: 'REGULAR', label: '定期执行' },
  { value: 'ON_DEMAND', label: '按需触发' },
]

// ========== Computed ==========
const sectionOptions = computed(() =>
  sections.value.map(s => ({
    value: s.id,
    label: s.sectionName,
    targetType: s.targetType,
  }))
)

function getSectionNames(sectionIdsJson: string): string {
  try {
    const ids: number[] = JSON.parse(sectionIdsJson)
    return ids.map(id => {
      const s = sections.value.find(sec => sec.id === id)
      return s ? s.sectionName : `#${id}`
    }).join('、')
  } catch {
    return sectionIdsJson
  }
}

function getScheduleInfo(plan: InspectionPlan): string {
  if (plan.scheduleMode === 'ON_DEMAND') return '按需触发'
  const cycle = CycleTypeConfig[plan.cycleType as CycleType]?.label || plan.cycleType
  let info = cycle
  if (plan.frequency > 1) info += ` x${plan.frequency}`
  if (plan.scheduleDays) {
    try {
      const days = JSON.parse(plan.scheduleDays)
      if (Array.isArray(days) && days.length > 0) info += ` (${days.join(',')})`
    } catch {}
  }
  if (plan.timeSlots) {
    try {
      const slots = JSON.parse(plan.timeSlots)
      if (Array.isArray(slots) && slots.length > 0) {
        info += ` ${slots.map((s: any) => s.start ? `${s.start}-${s.end}` : s).join(', ')}`
      }
    } catch {}
  }
  return info
}

// ========== Load ==========
async function loadPlans() {
  loading.value = true
  try {
    plans.value = await inspPlanApi.list(props.projectId)
  } catch (e: any) {
    ElMessage.error(e.message || '加载计划失败')
  } finally {
    loading.value = false
  }
}

async function loadSections() {
  try {
    // Load from project's root section - we need the project first
    const { getProject } = await import('@/api/insp/project')
    const project = await getProject(props.projectId)
    if (project.rootSectionId) {
      const all = await inspTemplateApi.getSections(project.rootSectionId)
      // Only first-level children
      sections.value = all.filter(s => s.parentSectionId === project.rootSectionId)
    }
  } catch { /* ignore */ }
}

// ========== CRUD ==========
function openAddDialog() {
  editingPlan.value = null
  form.value = {
    planName: '',
    sectionIds: [],
    scheduleMode: 'REGULAR',
    cycleType: 'DAILY',
    frequency: 1,
    scheduleDays: '',
    timeSlots: '',
    skipHolidays: false,
  }
  dialogVisible.value = true
}

function openEditDialog(plan: InspectionPlan) {
  editingPlan.value = plan
  let sectionIds: number[] = []
  try { sectionIds = JSON.parse(plan.sectionIds) } catch {}
  form.value = {
    planName: plan.planName,
    sectionIds,
    scheduleMode: plan.scheduleMode,
    cycleType: plan.cycleType,
    frequency: plan.frequency,
    scheduleDays: plan.scheduleDays || '',
    timeSlots: plan.timeSlots || '',
    skipHolidays: plan.skipHolidays,
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.planName.trim()) {
    ElMessage.warning('请输入计划名称')
    return
  }
  saving.value = true
  try {
    const data = {
      planName: form.value.planName,
      sectionIds: JSON.stringify(form.value.sectionIds),
      scheduleMode: form.value.scheduleMode,
      cycleType: form.value.cycleType,
      frequency: form.value.frequency,
      scheduleDays: form.value.scheduleDays || undefined,
      timeSlots: form.value.timeSlots || undefined,
      skipHolidays: form.value.skipHolidays,
    }

    if (editingPlan.value) {
      await inspPlanApi.update(editingPlan.value.id, data)
      ElMessage.success('已更新')
    } else {
      await inspPlanApi.create({ ...data, projectId: props.projectId } as CreatePlanRequest)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    loadPlans()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(plan: InspectionPlan) {
  try {
    await ElMessageBox.confirm(`删除计划「${plan.planName}」？`, '确认', { type: 'warning' })
    await inspPlanApi.delete(plan.id)
    ElMessage.success('已删除')
    loadPlans()
  } catch {}
}

async function handleToggleEnable(plan: InspectionPlan) {
  try {
    if (plan.isEnabled) {
      await inspPlanApi.disable(plan.id)
      ElMessage.success('已禁用')
    } else {
      await inspPlanApi.enable(plan.id)
      ElMessage.success('已启用')
    }
    loadPlans()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleTrigger(plan: InspectionPlan) {
  try {
    await ElMessageBox.confirm(`立即触发计划「${plan.planName}」？将生成新的检查任务。`, '确认触发', { type: 'info' })
    await inspPlanApi.trigger(plan.id)
    ElMessage.success('已触发，任务生成中')
  } catch {}
}

onMounted(() => {
  loadPlans()
  loadSections()
})
</script>

<template>
  <div v-loading="loading">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <div class="text-sm text-gray-500">管理检查计划，配置调度频率和关联分区</div>
      <el-button type="primary" size="small" @click="openAddDialog">
        <Plus class="w-3.5 h-3.5 mr-1" />添加计划
      </el-button>
    </div>

    <!-- Plan list -->
    <div v-if="plans.length === 0" class="py-16 text-center text-gray-400">
      <Calendar class="w-12 h-12 mx-auto mb-3 text-gray-300" />
      <div class="text-sm">暂无检查计划</div>
      <div class="text-xs text-gray-300 mt-1">添加计划来配置检查调度</div>
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="plan in plans"
        :key="plan.id"
        class="bg-white border border-gray-200 rounded-xl p-4 shadow-sm hover:border-gray-300 transition-colors"
      >
        <div class="flex items-start justify-between">
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span class="font-medium text-gray-800">{{ plan.planName }}</span>
              <el-tag :type="plan.scheduleMode === 'ON_DEMAND' ? 'warning' : 'primary'" size="small" round effect="plain">
                {{ plan.scheduleMode === 'ON_DEMAND' ? '按需' : '定期' }}
              </el-tag>
              <el-tag :type="plan.isEnabled ? 'success' : 'info'" size="small" round effect="plain">
                {{ plan.isEnabled ? '启用' : '禁用' }}
              </el-tag>
            </div>
            <div class="text-xs text-gray-400 mt-1.5 space-y-0.5">
              <div class="flex items-center gap-1">
                <Calendar class="w-3 h-3" />
                <span>调度: {{ getScheduleInfo(plan) }}</span>
                <span v-if="plan.skipHolidays" class="text-orange-400 ml-1">跳过节假日</span>
              </div>
              <div class="flex items-center gap-1">
                <Clock class="w-3 h-3" />
                <span>关联分区: {{ getSectionNames(plan.sectionIds) || '全部' }}</span>
              </div>
            </div>
          </div>
          <div class="flex items-center gap-1.5 shrink-0 ml-3">
            <el-switch
              :model-value="plan.isEnabled"
              size="small"
              @change="handleToggleEnable(plan)"
            />
            <el-button
              v-if="plan.scheduleMode === 'ON_DEMAND'"
              type="primary"
              size="small"
              plain
              @click="handleTrigger(plan)"
            >
              <Play class="w-3.5 h-3.5 mr-0.5" />触发
            </el-button>
            <el-button size="small" plain @click="openEditDialog(plan)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(plan)">
              <Trash2 class="w-3.5 h-3.5" />
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingPlan ? '编辑检查计划' : '添加检查计划'"
      width="560px"
      destroy-on-close
    >
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">计划名称 <span class="text-red-500">*</span></label>
          <el-input v-model="form.planName" placeholder="例如：每日卫生检查" maxlength="100" />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">关联分区</label>
          <el-select v-model="form.sectionIds" multiple class="w-full" placeholder="选择分区（不选则覆盖全部）" collapse-tags collapse-tags-tooltip>
            <el-option v-for="opt in sectionOptions" :key="opt.value" :label="opt.label" :value="opt.value">
              <div class="flex items-center justify-between">
                <span>{{ opt.label }}</span>
                <el-tag v-if="opt.targetType" size="small" type="info" class="ml-2">{{ opt.targetType }}</el-tag>
              </div>
            </el-option>
          </el-select>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">调度方式</label>
            <el-select v-model="form.scheduleMode" class="w-full">
              <el-option v-for="opt in scheduleModeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </div>
          <div v-if="form.scheduleMode === 'REGULAR'">
            <label class="block text-sm font-medium text-gray-700 mb-1">检查周期</label>
            <el-select v-model="form.cycleType" class="w-full">
              <el-option v-for="(cfg, key) in CycleTypeConfig" :key="key" :label="cfg.label" :value="key" />
            </el-select>
          </div>
        </div>

        <div v-if="form.scheduleMode === 'REGULAR'" class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">频率</label>
            <el-input-number v-model="form.frequency" :min="1" :max="100" class="!w-full" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">执行日（JSON数组）</label>
            <el-input v-model="form.scheduleDays" placeholder='例如: [1,3,5] 或留空' />
          </div>
        </div>

        <div v-if="form.scheduleMode === 'REGULAR'">
          <label class="block text-sm font-medium text-gray-700 mb-1">时间段（JSON数组）</label>
          <el-input v-model="form.timeSlots" placeholder='例如: [{"start":"08:00","end":"12:00"}] 或留空' />
        </div>

        <div>
          <el-checkbox v-model="form.skipHolidays">跳过节假日</el-checkbox>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ editingPlan ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
