<template>
  <div class="adjustment-view">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="我的申请" name="my">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>我的调课申请</span>
              <el-button type="primary" @click="showApplyDialog">申请调课</el-button>
            </div>
          </template>

          <el-table :data="myApplications" v-loading="loading" border stripe>
            <el-table-column label="原课程安排" min-width="200">
              <template #default="{ row }">
                <div>{{ row.originalEntry?.courseName }}</div>
                <div class="text-muted">
                  {{ getWeekdayName(row.originalEntry?.dayOfWeek) }}
                  第{{ row.originalEntry?.periodStart }}-{{ row.originalEntry?.periodEnd }}节
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="adjustmentType" label="调课类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getAdjustmentTypeTag(row.adjustmentType)">
                  {{ getAdjustmentTypeName(row.adjustmentType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="调整后" width="180">
              <template #default="{ row }">
                <template v-if="row.adjustmentType === 2">
                  <span class="text-danger">停课</span>
                </template>
                <template v-else>
                  <div>{{ row.newClassroomName || '-' }}</div>
                  <div class="text-muted">
                    {{ row.newDayOfWeek ? getWeekdayName(row.newDayOfWeek) : '-' }}
                    {{ row.newPeriodStart ? `第${row.newPeriodStart}-${row.newPeriodEnd}节` : '' }}
                  </div>
                </template>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getStatusTag(row.status)">{{ getStatusName(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="appliedAt" label="申请时间" width="160" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 0"
                  size="small"
                  text
                  type="danger"
                  @click="cancelApplication(row)"
                >撤销</el-button>
                <el-button v-else size="small" text @click="viewDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="myQueryParams.page"
            v-model:page-size="myQueryParams.size"
            :total="myTotal"
            layout="total, prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
            @current-change="loadMyApplications"
          />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="待我审批" name="pending">
        <el-card>
          <template #header>
            <span>待审批的调课申请</span>
          </template>

          <el-table :data="pendingApprovals" v-loading="loading" border stripe>
            <el-table-column prop="applicantName" label="申请人" width="100" />
            <el-table-column label="原课程安排" min-width="200">
              <template #default="{ row }">
                <div>{{ row.originalEntry?.courseName }}</div>
                <div class="text-muted">
                  {{ getWeekdayName(row.originalEntry?.dayOfWeek) }}
                  第{{ row.originalEntry?.periodStart }}-{{ row.originalEntry?.periodEnd }}节
                  @ {{ row.originalEntry?.classroomName }}
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="adjustmentType" label="调课类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getAdjustmentTypeTag(row.adjustmentType)">
                  {{ getAdjustmentTypeName(row.adjustmentType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="调整后" width="180">
              <template #default="{ row }">
                <template v-if="row.adjustmentType === 2">
                  <span class="text-danger">停课</span>
                </template>
                <template v-else>
                  <div>{{ row.newClassroomName || '-' }}</div>
                  <div class="text-muted">
                    {{ row.newDayOfWeek ? getWeekdayName(row.newDayOfWeek) : '-' }}
                    {{ row.newPeriodStart ? `第${row.newPeriodStart}-${row.newPeriodEnd}节` : '' }}
                  </div>
                </template>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
            <el-table-column prop="appliedAt" label="申请时间" width="160" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" text type="success" @click="approveAdjustment(row)">批准</el-button>
                <el-button size="small" text type="danger" @click="rejectAdjustment(row)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="pendingQueryParams.page"
            v-model:page-size="pendingQueryParams.size"
            :total="pendingTotal"
            layout="total, prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
            @current-change="loadPendingApprovals"
          />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="全部记录" name="all">
        <el-card>
          <el-form :inline="true" :model="allQueryParams" style="margin-bottom: 16px">
            <el-form-item label="类型">
              <el-select v-model="allQueryParams.adjustmentType" placeholder="全部" clearable>
                <el-option :value="1" label="调课" />
                <el-option :value="2" label="停课" />
                <el-option :value="3" label="补课" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="allQueryParams.status" placeholder="全部" clearable>
                <el-option :value="0" label="待审批" />
                <el-option :value="1" label="已批准" />
                <el-option :value="2" label="已驳回" />
                <el-option :value="3" label="已执行" />
                <el-option :value="4" label="已取消" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadAllAdjustments">查询</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="allAdjustments" v-loading="loading" border stripe>
            <el-table-column prop="applicantName" label="申请人" width="100" />
            <el-table-column label="课程" min-width="150">
              <template #default="{ row }">
                {{ row.originalEntry?.courseName }}
              </template>
            </el-table-column>
            <el-table-column prop="adjustmentType" label="类型" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="getAdjustmentTypeTag(row.adjustmentType)">
                  {{ getAdjustmentTypeName(row.adjustmentType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="120" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="getStatusTag(row.status)">{{ getStatusName(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="approverName" label="审批人" width="100" />
            <el-table-column prop="appliedAt" label="申请时间" width="160" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button size="small" text @click="viewDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="allQueryParams.page"
            v-model:page-size="allQueryParams.size"
            :total="allTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
            @size-change="loadAllAdjustments"
            @current-change="loadAllAdjustments"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 申请调课对话框 -->
    <el-dialog v-model="applyDialogVisible" title="申请调课" width="600px">
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="100px">
        <el-form-item label="选择课程" prop="entryId">
          <el-select v-model="applyForm.entryId" placeholder="选择要调整的课程" style="width: 100%">
            <el-option
              v-for="entry in myScheduleEntries"
              :key="entry.id"
              :value="entry.id"
              :label="`${entry.courseName} - ${getWeekdayName(entry.dayOfWeek)} 第${entry.periodStart}-${entry.periodEnd}节`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="调课类型" prop="adjustmentType">
          <el-radio-group v-model="applyForm.adjustmentType">
            <el-radio :value="1">调课</el-radio>
            <el-radio :value="2">停课</el-radio>
            <el-radio :value="3">补课</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="applyForm.adjustmentType !== 2">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="新星期">
                <el-select v-model="applyForm.newDayOfWeek" style="width: 100%">
                  <el-option v-for="d in weekdays" :key="d.value" :value="d.value" :label="d.label" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="新教室">
                <el-select v-model="applyForm.newClassroomId" placeholder="选择教室" style="width: 100%">
                  <el-option v-for="c in classrooms" :key="c.id" :value="c.id" :label="c.name" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="开始节次">
                <el-select v-model="applyForm.newPeriodStart" style="width: 100%">
                  <el-option v-for="p in periods" :key="p.period" :value="p.period" :label="p.name" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="结束节次">
                <el-select v-model="applyForm.newPeriodEnd" style="width: 100%">
                  <el-option v-for="p in periods" :key="p.period" :value="p.period" :label="p.name" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="调整周次">
            <el-input-number v-model="applyForm.newWeek" :min="1" :max="20" />
          </el-form-item>
        </template>

        <el-form-item label="调课原因" prop="reason">
          <el-input v-model="applyForm.reason" type="textarea" rows="3" placeholder="请说明调课原因..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitApplication">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 驳回原因对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="驳回申请" width="400px">
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="驳回原因">
          <el-input v-model="rejectForm.remark" type="textarea" rows="3" placeholder="请说明驳回原因..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="confirmReject">确定驳回</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="调课详情" width="600px">
      <template v-if="currentAdjustment">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请人">{{ currentAdjustment.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ currentAdjustment.appliedAt }}</el-descriptions-item>
          <el-descriptions-item label="课程">{{ currentAdjustment.originalEntry?.courseName }}</el-descriptions-item>
          <el-descriptions-item label="调课类型">
            <el-tag :type="getAdjustmentTypeTag(currentAdjustment.adjustmentType)">
              {{ getAdjustmentTypeName(currentAdjustment.adjustmentType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="原时间" :span="2">
            {{ getWeekdayName(currentAdjustment.originalEntry?.dayOfWeek) }}
            第{{ currentAdjustment.originalEntry?.periodStart }}-{{ currentAdjustment.originalEntry?.periodEnd }}节
            @ {{ currentAdjustment.originalEntry?.classroomName }}
          </el-descriptions-item>
          <el-descriptions-item v-if="currentAdjustment.adjustmentType !== 2" label="调整后" :span="2">
            {{ currentAdjustment.newDayOfWeek ? getWeekdayName(currentAdjustment.newDayOfWeek) : '-' }}
            {{ currentAdjustment.newPeriodStart ? `第${currentAdjustment.newPeriodStart}-${currentAdjustment.newPeriodEnd}节` : '' }}
            {{ currentAdjustment.newClassroomName ? `@ ${currentAdjustment.newClassroomName}` : '' }}
          </el-descriptions-item>
          <el-descriptions-item label="原因" :span="2">{{ currentAdjustment.reason }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTag(currentAdjustment.status)">{{ getStatusName(currentAdjustment.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审批人">{{ currentAdjustment.approverName || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="currentAdjustment.approvalRemark" label="审批备注" :span="2">
            {{ currentAdjustment.approvalRemark }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { adjustmentApi } from '@/api/teaching'
import type { ScheduleAdjustment, ScheduleEntry } from '@/types/teaching'
import { WEEKDAYS, DEFAULT_PERIODS } from '@/types/teaching'

// 状态
const loading = ref(false)
const saving = ref(false)
const activeTab = ref('my')

// 数据
const myApplications = ref<ScheduleAdjustment[]>([])
const pendingApprovals = ref<ScheduleAdjustment[]>([])
const allAdjustments = ref<ScheduleAdjustment[]>([])
const myScheduleEntries = ref<ScheduleEntry[]>([])
const currentAdjustment = ref<ScheduleAdjustment>()

// 分页
const myTotal = ref(0)
const pendingTotal = ref(0)
const allTotal = ref(0)

const myQueryParams = reactive({ page: 1, size: 10 })
const pendingQueryParams = reactive({ page: 1, size: 10 })
const allQueryParams = reactive({ adjustmentType: undefined as number | undefined, status: undefined as number | undefined, page: 1, size: 10 })

// 常量
const weekdays = WEEKDAYS
const periods = DEFAULT_PERIODS
const classrooms = ref([
  { id: 1, name: '教学楼A-101' },
  { id: 2, name: '教学楼A-102' },
  { id: 3, name: '教学楼B-201' },
])

// 对话框状态
const applyDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const detailDialogVisible = ref(false)

// 表单
const applyFormRef = ref<FormInstance>()
const applyForm = ref({
  entryId: undefined as number | undefined,
  adjustmentType: 1,
  newClassroomId: undefined as number | undefined,
  newDayOfWeek: undefined as number | undefined,
  newPeriodStart: undefined as number | undefined,
  newPeriodEnd: undefined as number | undefined,
  newWeek: undefined as number | undefined,
  reason: '',
})
const rejectForm = ref({ remark: '' })
const currentRejectId = ref<number>()

const applyRules: FormRules = {
  entryId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  adjustmentType: [{ required: true, message: '请选择调课类型', trigger: 'change' }],
  reason: [{ required: true, message: '请填写调课原因', trigger: 'blur' }],
}

// 方法
const loadMyApplications = async () => {
  loading.value = true
  try {
    const res = await adjustmentApi.getMyApplications(myQueryParams)
    const data = res.data || res
    myApplications.value = data.records || []
    myTotal.value = data.total || 0
  } catch (error) {
    console.error('Failed to load my applications:', error)
  } finally {
    loading.value = false
  }
}

const loadPendingApprovals = async () => {
  loading.value = true
  try {
    const res = await adjustmentApi.getPendingApprovals(pendingQueryParams)
    const data = res.data || res
    pendingApprovals.value = data.records || []
    pendingTotal.value = data.total || 0
  } catch (error) {
    console.error('Failed to load pending approvals:', error)
  } finally {
    loading.value = false
  }
}

const loadAllAdjustments = async () => {
  loading.value = true
  try {
    const res = await adjustmentApi.list(allQueryParams)
    const data = res.data || res
    allAdjustments.value = data.records || []
    allTotal.value = data.total || 0
  } catch (error) {
    console.error('Failed to load all adjustments:', error)
  } finally {
    loading.value = false
  }
}

const showApplyDialog = () => {
  applyForm.value = {
    entryId: undefined,
    adjustmentType: 1,
    newClassroomId: undefined,
    newDayOfWeek: undefined,
    newPeriodStart: undefined,
    newPeriodEnd: undefined,
    newWeek: undefined,
    reason: '',
  }
  // TODO: 加载当前用户的课程安排
  myScheduleEntries.value = [
    { id: 1, scheduleId: 1, taskId: 1, courseName: '数据结构', className: '计算机2024-1班', teacherName: '张老师', classroomId: 1, classroomName: '教学楼A-101', dayOfWeek: 1, periodStart: 1, periodEnd: 2, weekStart: 1, weekEnd: 16, weekType: 0 },
    { id: 2, scheduleId: 1, taskId: 2, courseName: '操作系统', className: '计算机2024-1班', teacherName: '张老师', classroomId: 2, classroomName: '教学楼A-102', dayOfWeek: 3, periodStart: 3, periodEnd: 4, weekStart: 1, weekEnd: 16, weekType: 0 },
  ]
  applyDialogVisible.value = true
}

const submitApplication = async () => {
  await applyFormRef.value?.validate()
  if (!applyForm.value.entryId) return
  saving.value = true
  try {
    await adjustmentApi.apply({
      entryId: applyForm.value.entryId,
      adjustmentType: applyForm.value.adjustmentType,
      newClassroomId: applyForm.value.newClassroomId,
      newDayOfWeek: applyForm.value.newDayOfWeek,
      newPeriodStart: applyForm.value.newPeriodStart,
      newPeriodEnd: applyForm.value.newPeriodEnd,
      newWeek: applyForm.value.newWeek,
      reason: applyForm.value.reason,
    })
    ElMessage.success('申请已提交')
    applyDialogVisible.value = false
    loadMyApplications()
  } catch (error) {
    ElMessage.error('提交失败')
  } finally {
    saving.value = false
  }
}

const cancelApplication = async (adjustment: ScheduleAdjustment) => {
  await ElMessageBox.confirm('确定撤销该调课申请吗？', '提示', { type: 'warning' })
  try {
    await adjustmentApi.cancel(adjustment.id)
    ElMessage.success('已撤销')
    loadMyApplications()
  } catch (error) {
    ElMessage.error('撤销失败')
  }
}

const approveAdjustment = async (adjustment: ScheduleAdjustment) => {
  await ElMessageBox.confirm('确定批准该调课申请吗？', '提示', { type: 'info' })
  try {
    await adjustmentApi.approve(adjustment.id)
    ElMessage.success('已批准')
    loadPendingApprovals()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const rejectAdjustment = (adjustment: ScheduleAdjustment) => {
  currentRejectId.value = adjustment.id
  rejectForm.value = { remark: '' }
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!currentRejectId.value || !rejectForm.value.remark) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  saving.value = true
  try {
    await adjustmentApi.reject(currentRejectId.value, rejectForm.value.remark)
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    loadPendingApprovals()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    saving.value = false
  }
}

const viewDetail = (adjustment: ScheduleAdjustment) => {
  currentAdjustment.value = adjustment
  detailDialogVisible.value = true
}

const getWeekdayName = (day?: number) => {
  if (!day) return '-'
  return weekdays.find(w => w.value === day)?.label || '-'
}

const getAdjustmentTypeName = (type: number) => {
  const names: Record<number, string> = { 1: '调课', 2: '停课', 3: '补课' }
  return names[type] || '未知'
}

const getAdjustmentTypeTag = (type: number) => {
  const types: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    1: 'warning',
    2: 'danger',
    3: 'success',
  }
  return types[type] || 'info'
}

const getStatusName = (status: number) => {
  const names: Record<number, string> = {
    0: '待审批',
    1: '已批准',
    2: '已驳回',
    3: '已执行',
    4: '已取消',
  }
  return names[status] || '未知'
}

const getStatusTag = (status: number) => {
  const types: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    0: 'warning',
    1: 'success',
    2: 'danger',
    3: '',
    4: 'info',
  }
  return types[status] || 'info'
}

onMounted(() => {
  loadMyApplications()
  loadPendingApprovals()
  loadAllAdjustments()
})
</script>

<style scoped lang="scss">
.adjustment-view {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

.text-danger {
  color: #f56c6c;
  font-weight: 600;
}
</style>
