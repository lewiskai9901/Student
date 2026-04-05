<template>
  <div>
    <!-- Action bar -->
    <div class="mb-4 flex items-center gap-3">
      <el-button type="primary" @click="showAdjustmentDialog()">
        <Plus class="mr-1 h-4 w-4" /> 申请调课
      </el-button>
      <el-radio-group v-model="adjustmentView" size="small">
        <el-radio-button value="my">我的申请</el-radio-button>
        <el-radio-button value="pending">待审批</el-radio-button>
        <el-radio-button value="all">全部记录</el-radio-button>
      </el-radio-group>
    </div>

    <!-- Adjustment table -->
    <div class="rounded-xl border border-gray-200 bg-white">
      <div class="border-t border-gray-100">
        <el-table :data="adjustments" v-loading="adjustmentLoading" stripe>
          <el-table-column label="类型" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="getAdjTypeTag(row.adjustmentType)">
                {{ getAdjTypeName(row.adjustmentType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="applicantName" label="申请人" width="100" />
          <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
          <el-table-column label="新时间" width="160">
            <template #default="{ row }">
              <template v-if="row.newDayOfWeek">
                {{ getWeekdayName(row.newDayOfWeek) }}
                第{{ row.newPeriodStart }}-{{ row.newPeriodEnd }}节
              </template>
              <span v-else class="text-gray-400">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="newClassroomName" label="新教室" width="100" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="getAdjStatusTag(row.status)">
                {{ getAdjStatusName(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="appliedAt" label="申请时间" width="160" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 0 && adjustmentView === 'pending'">
                <el-button size="small" text type="success" @click="approveAdjustment(row)">批准</el-button>
                <el-button size="small" text type="danger" @click="rejectAdjustment(row)">驳回</el-button>
              </template>
              <template v-else-if="row.status === 1">
                <el-button size="small" text type="primary" @click="executeAdjustment(row)">执行</el-button>
              </template>
              <template v-else-if="row.status === 0 && adjustmentView === 'my'">
                <el-button size="small" text type="danger" @click="cancelAdjustment(row)">撤回</el-button>
              </template>
              <span v-else class="text-xs text-gray-400">-</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="flex justify-end px-4 py-3">
          <el-pagination
            v-model:current-page="adjustmentPagination.page"
            v-model:page-size="adjustmentPagination.size"
            :total="adjustmentPagination.total"
            :page-sizes="[20, 50]"
            layout="total, prev, pager, next"
            small
            @current-change="loadAdjustments"
          />
        </div>
      </div>
    </div>

    <!-- Adjustment Apply Dialog -->
    <el-dialog v-model="adjustmentDialogVisible" title="申请调课" width="560px">
      <el-form ref="adjFormRef" :model="adjForm" :rules="adjRules" label-width="100px">
        <el-form-item label="调课类型" prop="adjustmentType">
          <el-radio-group v-model="adjForm.adjustmentType">
            <el-radio :value="1">调课</el-radio>
            <el-radio :value="2">停课</el-radio>
            <el-radio :value="3">补课</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="原课程" prop="entryId">
          <el-select v-model="adjForm.entryId" placeholder="选择要调整的课程" style="width: 100%" filterable>
            <el-option
              v-for="entry in adjEntryOptions"
              :key="entry.id"
              :value="entry.id"
              :label="`${entry.courseName} - ${getWeekdayName(entry.dayOfWeek)} 第${entry.periodStart}-${entry.periodEnd}节`"
            />
          </el-select>
        </el-form-item>
        <template v-if="adjForm.adjustmentType !== 2">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="新星期">
                <el-select v-model="adjForm.newDayOfWeek" style="width: 100%" clearable>
                  <el-option v-for="d in weekdays" :key="d.value" :value="d.value" :label="d.label" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="新教室">
                <el-select v-model="adjForm.newClassroomId" placeholder="选择教室" style="width: 100%" filterable clearable>
                  <el-option v-for="c in classrooms" :key="c.id" :value="c.id" :label="c.name" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="新开始节">
                <el-select v-model="adjForm.newPeriodStart" style="width: 100%" clearable>
                  <el-option v-for="p in periods" :key="p.period" :value="p.period" :label="p.name" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="新结束节">
                <el-select v-model="adjForm.newPeriodEnd" style="width: 100%" clearable>
                  <el-option v-for="p in periods" :key="p.period" :value="p.period" :label="p.name" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="新周次">
                <el-input-number v-model="adjForm.newWeek" :min="1" :max="20" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
        <el-form-item label="原因" prop="reason">
          <el-input v-model="adjForm.reason" type="textarea" :rows="2" placeholder="请填写调课原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustmentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjSaving" @click="submitAdjustment">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from 'lucide-vue-next'
import { http as request } from '@/utils/request'
import { scheduleApi, adjustmentApi } from '@/api/teaching'
import type { ScheduleEntry, ScheduleAdjustment, CourseSchedule } from '@/types/teaching'
import { WEEKDAYS, DEFAULT_PERIODS } from '@/types/teaching'

// ==================== Props ====================

const props = defineProps<{
  semesterId: number | string | undefined
}>()

// ==================== Constants ====================

const weekdays = WEEKDAYS
const periods = DEFAULT_PERIODS

// ==================== State ====================

const adjustments = ref<ScheduleAdjustment[]>([])
const adjustmentLoading = ref(false)
const adjustmentView = ref<'my' | 'pending' | 'all'>('my')
const adjustmentPagination = reactive({ page: 1, size: 20, total: 0 })

const adjustmentDialogVisible = ref(false)
const adjSaving = ref(false)
const adjFormRef = ref<FormInstance>()
const adjForm = ref<Partial<ScheduleAdjustment & { newWeek?: number }>>({
  adjustmentType: 1,
  reason: '',
})
const adjEntryOptions = ref<ScheduleEntry[]>([])
const classrooms = ref<{ id: number; name: string }[]>([])
const scheduleList = ref<CourseSchedule[]>([])

const adjRules: FormRules = {
  adjustmentType: [{ required: true, message: '请选择调课类型', trigger: 'change' }],
  entryId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  reason: [{ required: true, message: '请填写原因', trigger: 'blur' }],
}

// ==================== Data Loading ====================

async function loadAdjustments() {
  adjustmentLoading.value = true
  try {
    let res
    const params = { page: adjustmentPagination.page, size: adjustmentPagination.size }
    if (adjustmentView.value === 'my') {
      res = await adjustmentApi.getMyApplications(params)
    } else if (adjustmentView.value === 'pending') {
      res = await adjustmentApi.getPendingApprovals(params)
    } else {
      res = await adjustmentApi.list(params)
    }
    const data = (res as any).data || res
    if (data.records) {
      adjustments.value = data.records
      adjustmentPagination.total = data.total || 0
    } else if (Array.isArray(data)) {
      adjustments.value = data
      adjustmentPagination.total = data.length
    }
  } catch (e) {
    console.error('Failed to load adjustments:', e)
  } finally {
    adjustmentLoading.value = false
  }
}

async function loadClassrooms() {
  try {
    const res = await request.get('/v9/places', { params: { typeCode: 'CLASSROOM' } })
    const data = (res as any).data || res
    const items = Array.isArray(data) ? data : data.records || []
    classrooms.value = items.map((p: any) => ({ id: p.id, name: p.placeName || p.name }))
  } catch (e) {
    console.error('Failed to load classrooms:', e)
  }
}

async function loadScheduleList() {
  if (!props.semesterId) return
  try {
    const res = await scheduleApi.list({ semesterId: props.semesterId })
    scheduleList.value = (res as any).data || res
    if (!Array.isArray(scheduleList.value)) scheduleList.value = []
  } catch (e) {
    console.error('Failed to load schedules:', e)
  }
}

async function loadAdjEntryOptions() {
  if (!props.semesterId) return
  try {
    if (scheduleList.value.length > 0) {
      const res = await scheduleApi.getEntries(scheduleList.value[0].id)
      adjEntryOptions.value = (res as any).data || res
    }
  } catch (e) {
    console.error('Failed to load entry options:', e)
  }
}

// ==================== Actions ====================

function showAdjustmentDialog() {
  adjForm.value = { adjustmentType: 1, reason: '' }
  loadAdjEntryOptions()
  adjustmentDialogVisible.value = true
}

async function submitAdjustment() {
  await adjFormRef.value?.validate()
  adjSaving.value = true
  try {
    await adjustmentApi.apply({
      entryId: adjForm.value.entryId!,
      adjustmentType: adjForm.value.adjustmentType!,
      newClassroomId: adjForm.value.newClassroomId,
      newDayOfWeek: adjForm.value.newDayOfWeek,
      newPeriodStart: adjForm.value.newPeriodStart,
      newPeriodEnd: adjForm.value.newPeriodEnd,
      newWeek: adjForm.value.newWeek,
      reason: adjForm.value.reason!,
    })
    ElMessage.success('申请已提交')
    adjustmentDialogVisible.value = false
    loadAdjustments()
  } catch (e) {
    ElMessage.error('提交失败')
  } finally {
    adjSaving.value = false
  }
}

async function approveAdjustment(adj: ScheduleAdjustment) {
  await ElMessageBox.confirm('确定批准该调课申请？', '审批确认')
  try {
    await adjustmentApi.approve(adj.id)
    ElMessage.success('已批准')
    loadAdjustments()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function rejectAdjustment(adj: ScheduleAdjustment) {
  const { value } = await ElMessageBox.prompt('请填写驳回原因', '驳回确认', {
    inputType: 'textarea',
    inputValidator: (v) => (v && v.trim() ? true : '请填写驳回原因'),
  })
  try {
    await adjustmentApi.reject(adj.id, value)
    ElMessage.success('已驳回')
    loadAdjustments()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function executeAdjustment(adj: ScheduleAdjustment) {
  await ElMessageBox.confirm('执行后将正式修改课表，确定执行？', '执行确认', { type: 'warning' })
  try {
    await adjustmentApi.execute(adj.id)
    ElMessage.success('已执行')
    loadAdjustments()
  } catch (e) {
    ElMessage.error('执行失败')
  }
}

async function cancelAdjustment(adj: ScheduleAdjustment) {
  await ElMessageBox.confirm('确定撤回该申请？', '撤回确认')
  try {
    await adjustmentApi.cancel(adj.id)
    ElMessage.success('已撤回')
    loadAdjustments()
  } catch (e) {
    ElMessage.error('撤回失败')
  }
}

// ==================== Helpers ====================

function getWeekdayName(day: number) {
  return weekdays.find(w => w.value === day)?.label || ''
}

function getAdjTypeName(t: number) {
  const m: Record<number, string> = { 1: '调课', 2: '停课', 3: '补课' }
  return m[t] || '未知'
}

function getAdjTypeTag(t: number) {
  const m: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    1: '', 2: 'danger', 3: 'success',
  }
  return m[t] || 'info'
}

function getAdjStatusName(s: number) {
  const m: Record<number, string> = { 0: '待审批', 1: '已批准', 2: '已驳回', 3: '已执行', 4: '已取消' }
  return m[s] || '未知'
}

function getAdjStatusTag(s: number) {
  const m: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    0: 'warning', 1: 'success', 2: 'danger', 3: '', 4: 'info',
  }
  return m[s] || 'info'
}

// ==================== Watchers ====================

watch(() => props.semesterId, (val) => {
  if (val) {
    loadScheduleList()
    loadClassrooms()
  }
  loadAdjustments()
}, { immediate: true })

watch(adjustmentView, () => {
  adjustmentPagination.page = 1
  loadAdjustments()
})
</script>
