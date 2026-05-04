<template>
  <div class="flex h-full flex-col bg-gray-50">
    <!-- Header -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">考勤管理</h1>
        <p class="mt-0.5 text-sm text-gray-500">课程考勤、日常考勤、请假管理与统计分析</p>
      </div>
    </div>

    <!-- Tabs -->
    <div class="border-b border-gray-200 bg-white px-6">
      <nav class="-mb-px flex gap-6">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="border-b-2 px-1 py-3 text-sm font-medium transition-colors"
          :class="
            activeTab === tab.key
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
          "
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </nav>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
      <!-- Tab 1: 课程考勤 -->
      <div v-if="activeTab === 'checkin'">
        <!-- Filter row -->
        <div class="mb-4 flex flex-wrap items-center gap-3">
          <el-select
            v-model="checkin.semesterId"
            placeholder="选择学期"
            class="w-44"
            @change="onSemesterChange"
          >
            <el-option v-for="s in semesters" :key="s.id" :value="s.id" :label="s.semesterName || s.name" />
          </el-select>
          <el-select v-model="checkin.orgUnitId" placeholder="选择班级" class="w-40" filterable @change="loadClassStudents">
            <el-option v-for="c in classes" :key="c.id" :value="c.id" :label="c.name || c.className" />
          </el-select>
          <el-date-picker
            v-model="checkin.date"
            type="date"
            placeholder="日期"
            value-format="YYYY-MM-DD"
            class="!w-36"
          />
          <el-select v-model="checkin.courseId" placeholder="课程(可选)" clearable class="w-40" filterable>
            <el-option v-for="c in courses" :key="c.id" :value="c.id" :label="c.courseName || c.name" />
          </el-select>
          <el-select v-model="checkin.period" placeholder="节次(可选)" clearable class="w-28">
            <el-option v-for="p in 10" :key="p" :value="p" :label="'第' + p + '节'" />
          </el-select>
          <button
            class="inline-flex items-center gap-1.5 rounded-lg bg-gray-100 px-3.5 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-200"
            @click="loadClassStudents"
          >
            加载学生
          </button>
          <button
            class="ml-auto inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700 disabled:opacity-50"
            :disabled="!checkinRows.length || batchSaving"
            @click="saveBatchAttendance"
          >
            {{ batchSaving ? '保存中...' : '批量保存' }}
          </button>
        </div>

        <!-- Quick-set buttons -->
        <div v-if="checkinRows.length" class="mb-3 flex items-center gap-2">
          <span class="text-xs text-gray-500">快速设置全部:</span>
          <button
            v-for="opt in statusOptions"
            :key="opt.value"
            class="rounded px-2 py-0.5 text-xs font-medium transition-colors"
            :class="quickSetClass(opt.color)"
            @click="setAllStatus(opt.value)"
          >
            {{ opt.label }}
          </button>
        </div>

        <!-- Student list table -->
        <div v-if="checkinRows.length" class="rounded-xl border border-gray-200 bg-white">
          <el-table :data="checkinRows" stripe class="rounded-xl" max-height="calc(100vh - 360px)">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="studentNo" label="学号" width="120" />
            <el-table-column prop="studentName" label="姓名" width="100" />
            <el-table-column label="考勤状态" width="200">
              <template #default="{ row }">
                <el-radio-group v-model="row.status" size="small">
                  <el-radio-button
                    v-for="opt in statusOptions"
                    :key="opt.value"
                    :value="opt.value"
                  >
                    {{ opt.label }}
                  </el-radio-button>
                </el-radio-group>
              </template>
            </el-table-column>
            <el-table-column label="备注" min-width="180">
              <template #default="{ row }">
                <el-input v-model="row.remark" placeholder="备注(可选)" size="small" />
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else class="flex items-center justify-center py-20 text-sm text-gray-400">
          请选择班级和日期后点击"加载学生"
        </div>
      </div>

      <!-- Tab 2: 考勤记录 -->
      <div v-if="activeTab === 'records'">
        <!-- Filters -->
        <div class="mb-4 flex flex-wrap items-center gap-3">
          <el-select v-model="recordFilter.semesterId" placeholder="学期" clearable class="w-44" @change="loadRecords">
            <el-option v-for="s in semesters" :key="s.id" :value="s.id" :label="s.semesterName || s.name" />
          </el-select>
          <el-select v-model="recordFilter.orgUnitId" placeholder="班级" clearable filterable class="w-40" @change="loadRecords">
            <el-option v-for="c in classes" :key="c.id" :value="c.id" :label="c.name || c.className" />
          </el-select>
          <el-date-picker
            v-model="recordFilter.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="!w-64"
            @change="loadRecords"
          />
          <el-select v-model="recordFilter.status" placeholder="状态" clearable class="w-28" @change="loadRecords">
            <el-option v-for="opt in statusOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
          </el-select>
          <button
            class="inline-flex items-center gap-1.5 rounded-lg bg-gray-100 px-3.5 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-200"
            @click="loadRecords"
          >
            查询
          </button>
          <button
            class="inline-flex items-center gap-1.5 rounded-lg bg-gray-100 px-3.5 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-200 disabled:opacity-50"
            :disabled="!recordFilter.semesterId || exportingRecords"
            @click="doExportRecords"
          >
            {{ exportingRecords ? '导出中...' : '导出Excel' }}
          </button>
        </div>

        <!-- Records Table -->
        <div class="rounded-xl border border-gray-200 bg-white">
          <el-table :data="records" stripe class="rounded-xl" v-loading="recordsLoading">
            <el-table-column prop="attendanceDate" label="日期" width="110" />
            <el-table-column prop="studentNo" label="学号" width="120" />
            <el-table-column prop="studentName" label="姓名" width="100" />
            <el-table-column prop="courseName" label="课程" width="120" />
            <el-table-column prop="period" label="节次" width="70">
              <template #default="{ row }">
                {{ row.period ? '第' + row.period + '节' : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="getStatusTag(row.status)" size="small">
                  {{ getStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="140" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <button class="mr-2 text-xs text-blue-600 hover:text-blue-800" @click="editRecord(row)">修改</button>
                <button class="text-xs text-red-500 hover:text-red-700" @click="delRecord(row.id)">删除</button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- Tab 3: 考勤统计 -->
      <div v-if="activeTab === 'stats'">
        <!-- Filters -->
        <div class="mb-4 flex flex-wrap items-center gap-3">
          <el-select v-model="statsFilter.semesterId" placeholder="学期" class="w-44" @change="loadStats">
            <el-option v-for="s in semesters" :key="s.id" :value="s.id" :label="s.semesterName || s.name" />
          </el-select>
          <el-select v-model="statsFilter.orgUnitId" placeholder="班级(可选)" clearable filterable class="w-40" @change="loadStats">
            <el-option v-for="c in classes" :key="c.id" :value="c.id" :label="c.name || c.className" />
          </el-select>
          <el-date-picker
            v-model="statsFilter.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="!w-64"
            @change="loadStats"
          />
        </div>

        <!-- Stats Cards -->
        <div v-if="stats" class="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
          <div class="rounded-xl border border-gray-200 bg-white p-4">
            <div class="text-sm text-gray-500">总记录数</div>
            <div class="mt-1 text-2xl font-bold text-gray-900">{{ stats.total }}</div>
          </div>
          <div class="rounded-xl border border-gray-200 bg-white p-4">
            <div class="text-sm text-gray-500">出勤率</div>
            <div class="mt-1 text-2xl font-bold text-emerald-600">{{ stats.attendanceRate }}%</div>
          </div>
          <div class="rounded-xl border border-gray-200 bg-white p-4">
            <div class="text-sm text-gray-500">迟到</div>
            <div class="mt-1 text-2xl font-bold text-amber-500">{{ stats.late }}</div>
          </div>
          <div class="rounded-xl border border-gray-200 bg-white p-4">
            <div class="text-sm text-gray-500">旷课率</div>
            <div class="mt-1 text-2xl font-bold text-red-500">{{ stats.absentRate }}%</div>
          </div>
        </div>

        <!-- Breakdown bar -->
        <div v-if="stats && stats.total > 0" class="rounded-xl border border-gray-200 bg-white p-4">
          <h3 class="mb-3 text-sm font-medium text-gray-700">状态分布</h3>
          <div class="flex items-center gap-4 text-sm">
            <span class="text-gray-500">出勤 <span class="font-semibold text-emerald-600">{{ stats.present }}</span></span>
            <div class="h-3 w-px bg-gray-200" />
            <span class="text-gray-500">迟到 <span class="font-semibold text-amber-500">{{ stats.late }}</span></span>
            <div class="h-3 w-px bg-gray-200" />
            <span class="text-gray-500">早退 <span class="font-semibold text-amber-500">{{ stats.earlyLeave }}</span></span>
            <div class="h-3 w-px bg-gray-200" />
            <span class="text-gray-500">请假 <span class="font-semibold text-blue-500">{{ stats.leave }}</span></span>
            <div class="h-3 w-px bg-gray-200" />
            <span class="text-gray-500">旷课 <span class="font-semibold text-red-500">{{ stats.absent }}</span></span>
          </div>
          <!-- Visual bar -->
          <div class="mt-3 flex h-4 w-full overflow-hidden rounded-full bg-gray-100">
            <div
              class="bg-emerald-500 transition-all"
              :style="{ width: barWidth(stats.present) }"
            />
            <div
              class="bg-amber-400 transition-all"
              :style="{ width: barWidth(stats.late) }"
            />
            <div
              class="bg-amber-300 transition-all"
              :style="{ width: barWidth(stats.earlyLeave) }"
            />
            <div
              class="bg-blue-400 transition-all"
              :style="{ width: barWidth(stats.leave) }"
            />
            <div
              class="bg-red-500 transition-all"
              :style="{ width: barWidth(stats.absent) }"
            />
          </div>
        </div>
        <div v-else-if="!stats" class="flex items-center justify-center py-20 text-sm text-gray-400">
          请选择学期后查看统计数据
        </div>
      </div>

      <!-- Tab 4: 请假管理 -->
      <div v-if="activeTab === 'leave'">
        <!-- Filter row -->
        <div class="mb-4 flex flex-wrap items-center gap-3">
          <el-select v-model="leaveFilter.approvalStatus" placeholder="审批状态" clearable class="w-32" @change="loadLeaves">
            <el-option :value="0" label="待审批" />
            <el-option :value="1" label="已通过" />
            <el-option :value="2" label="已拒绝" />
          </el-select>
          <el-select v-model="leaveFilter.orgUnitId" placeholder="班级" clearable filterable class="w-40" @change="loadLeaves">
            <el-option v-for="c in classes" :key="c.id" :value="c.id" :label="c.name || c.className" />
          </el-select>
          <button
            class="inline-flex items-center gap-1.5 rounded-lg bg-gray-100 px-3.5 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-200"
            @click="loadLeaves"
          >
            查询
          </button>
          <button
            class="ml-auto inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3.5 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
            @click="showLeaveDialog = true"
          >
            新建请假
          </button>
        </div>

        <!-- Leave table -->
        <div class="rounded-xl border border-gray-200 bg-white">
          <el-table :data="leaveList" stripe class="rounded-xl" v-loading="leavesLoading">
            <el-table-column prop="studentName" label="学生" width="100" />
            <el-table-column prop="studentNo" label="学号" width="120" />
            <el-table-column label="类型" width="80">
              <template #default="{ row }">
                {{ leaveTypeLabel(row.leaveType) }}
              </template>
            </el-table-column>
            <el-table-column prop="startDate" label="开始日期" width="110" />
            <el-table-column prop="endDate" label="结束日期" width="110" />
            <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag
                  :type="row.approvalStatus === 0 ? 'warning' : row.approvalStatus === 1 ? 'success' : 'danger'"
                  size="small"
                >
                  {{ row.approvalStatus === 0 ? '待审批' : row.approvalStatus === 1 ? '已通过' : '已拒绝' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="提交时间" width="160" />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <template v-if="row.approvalStatus === 0">
                  <button class="mr-2 text-xs text-emerald-600 hover:text-emerald-800" @click="handleApprove(row.id)">通过</button>
                  <button class="text-xs text-red-500 hover:text-red-700" @click="handleReject(row.id)">拒绝</button>
                </template>
                <span v-else class="text-xs text-gray-400">{{ row.approvalComment || '-' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- Edit Record Dialog -->
    <el-dialog v-model="editDialogVisible" title="修改考勤记录" width="400px">
      <el-form label-width="70px">
        <el-form-item label="状态">
          <el-select v-model="editForm.status" class="w-full">
            <el-option v-for="opt in statusOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEditRecord">保存</el-button>
      </template>
    </el-dialog>

    <!-- New Leave Dialog -->
    <el-dialog v-model="showLeaveDialog" title="新建请假申请" width="480px">
      <el-form label-width="80px">
        <el-form-item label="班级">
          <el-select v-model="leaveForm.orgUnitId" placeholder="选择班级" filterable class="w-full" @change="loadLeaveStudents">
            <el-option v-for="c in classes" :key="c.id" :value="c.id" :label="c.name || c.className" />
          </el-select>
        </el-form-item>
        <el-form-item label="学生">
          <el-select v-model="leaveForm.studentId" placeholder="选择学生" filterable class="w-full">
            <el-option v-for="s in leaveStudents" :key="s.id" :value="s.id" :label="s.studentNo + ' ' + s.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="请假类型">
          <el-select v-model="leaveForm.leaveType" class="w-full">
            <el-option :value="1" label="事假" />
            <el-option :value="2" label="病假" />
            <el-option :value="3" label="公假" />
          </el-select>
        </el-form-item>
        <el-form-item label="起止日期">
          <el-date-picker
            v-model="leaveForm.dateRange"
            type="daterange"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            class="!w-full"
          />
        </el-form-item>
        <el-form-item label="请假原因">
          <el-input v-model="leaveForm.reason" type="textarea" :rows="3" placeholder="请输入请假原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLeaveDialog = false">取消</el-button>
        <el-button type="primary" @click="submitLeave">提交</el-button>
      </template>
    </el-dialog>

    <!-- Reject Comment Dialog -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝请假" width="400px">
      <el-form>
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectComment" type="textarea" :rows="3" placeholder="请输入拒绝原因(可选)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listRecords,
  getByClass,
  batchRecord,
  updateRecord,
  deleteRecord,
  getStatistics,
  createLeave,
  listLeaves,
  approveLeave,
  rejectLeave,
  exportRecords,
} from '@/api/attendance'
import { semesterApi } from '@/api/calendar'
import { courseApi } from '@/api/academic'
import { schoolClassApi } from '@/api/organization'
import { http } from '@/utils/request'
import type { AttendanceStats } from '@/types/attendance'

const tabs = [
  { key: 'checkin', label: '课程考勤' },
  { key: 'records', label: '考勤记录' },
  { key: 'stats', label: '考勤统计' },
  { key: 'leave', label: '请假管理' },
]
const activeTab = ref('checkin')

const statusOptions = [
  { value: 1, label: '出勤', color: 'success' },
  { value: 2, label: '迟到', color: 'warning' },
  { value: 3, label: '早退', color: 'warning' },
  { value: 4, label: '请假', color: 'info' },
  { value: 5, label: '旷课', color: 'danger' },
]

// ==================== Shared Data ====================
const semesters = ref<any[]>([])
const classes = ref<any[]>([])
const courses = ref<any[]>([])

async function loadBaseData() {
  try {
    const [semList, classList] = await Promise.all([
      semesterApi.list(),
      schoolClassApi.getAll(),
    ])
    semesters.value = semList || []
    classes.value = classList || []

    // Set current semester as default
    try {
      const current = await semesterApi.getCurrent()
      if (current?.id) {
        checkin.semesterId = current.id
        recordFilter.semesterId = current.id
        statsFilter.semesterId = current.id
      }
    } catch {
      // No current semester
    }

    // Load courses
    try {
      const courseResult = await courseApi.listAll()
      courses.value = courseResult || []
    } catch {
      courses.value = []
    }
  } catch (e) {
    console.error('Failed to load base data', e)
  }
}

// ==================== Tab 1: Checkin ====================
const checkin = reactive({
  semesterId: null as number | null,
  orgUnitId: null as number | null,
  date: new Date().toISOString().split('T')[0],
  courseId: null as number | null,
  period: null as number | null,
})
const checkinRows = ref<any[]>([])
const batchSaving = ref(false)

function onSemesterChange() {
  // Optionally reload data on semester change
}

async function loadClassStudents() {
  if (!checkin.orgUnitId || !checkin.date) {
    ElMessage.warning('请选择班级和日期')
    return
  }
  try {
    const params: any = { orgUnitId: checkin.orgUnitId, date: checkin.date }
    if (checkin.courseId) params.courseId = checkin.courseId
    if (checkin.period) params.period = checkin.period

    const rows = await getByClass(params)
    checkinRows.value = (rows || []).map((r: any) => ({
      ...r,
      status: r.status ?? 1, // Default to present
      remark: r.remark || '',
    }))
  } catch (e: any) {
    ElMessage.error('加载学生失败: ' + (e.message || '未知错误'))
  }
}

function setAllStatus(status: number) {
  checkinRows.value.forEach((r) => (r.status = status))
}

function quickSetClass(color: string) {
  const map: Record<string, string> = {
    success: 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100',
    warning: 'bg-amber-50 text-amber-700 hover:bg-amber-100',
    info: 'bg-blue-50 text-blue-700 hover:bg-blue-100',
    danger: 'bg-red-50 text-red-700 hover:bg-red-100',
  }
  return map[color] || 'bg-gray-50 text-gray-700 hover:bg-gray-100'
}

async function saveBatchAttendance() {
  if (!checkin.semesterId) {
    ElMessage.warning('请选择学期')
    return
  }
  batchSaving.value = true
  try {
    const payload = {
      semesterId: checkin.semesterId,
      orgUnitId: checkin.orgUnitId,
      courseId: checkin.courseId,
      date: checkin.date,
      period: checkin.period,
      attendanceType: checkin.courseId ? 1 : 2,
      students: checkinRows.value.map((r) => ({
        studentId: r.studentId,
        status: r.status,
        remark: r.remark || null,
      })),
    }
    await batchRecord(payload)
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  } finally {
    batchSaving.value = false
  }
}

// ==================== Tab 2: Records ====================
const recordFilter = reactive({
  semesterId: null as number | null,
  orgUnitId: null as number | null,
  dateRange: null as [string, string] | null,
  status: null as number | null,
})
const records = ref<any[]>([])
const recordsLoading = ref(false)

async function loadRecords() {
  recordsLoading.value = true
  try {
    const params: any = {}
    if (recordFilter.semesterId) params.semesterId = recordFilter.semesterId
    if (recordFilter.orgUnitId) params.orgUnitId = recordFilter.orgUnitId
    if (recordFilter.status) params.status = recordFilter.status
    if (recordFilter.dateRange?.[0]) params.startDate = recordFilter.dateRange[0]
    if (recordFilter.dateRange?.[1]) params.endDate = recordFilter.dateRange[1]

    records.value = (await listRecords(params)) || []
  } catch (e: any) {
    ElMessage.error('查询失败: ' + (e.message || '未知错误'))
  } finally {
    recordsLoading.value = false
  }
}

// Export records
const exportingRecords = ref(false)

async function doExportRecords() {
  if (!recordFilter.semesterId) {
    ElMessage.warning('请先选择学期')
    return
  }
  exportingRecords.value = true
  try {
    const params: any = { semesterId: recordFilter.semesterId }
    if (recordFilter.orgUnitId) params.orgUnitId = recordFilter.orgUnitId
    if (recordFilter.dateRange?.[0]) params.startDate = recordFilter.dateRange[0]
    if (recordFilter.dateRange?.[1]) params.endDate = recordFilter.dateRange[1]

    const res = await exportRecords(params)
    const blob = new Blob([res as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '考勤记录.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exportingRecords.value = false
  }
}

// Edit record
const editDialogVisible = ref(false)
const editForm = reactive({ id: 0, status: 1, remark: '' })

function editRecord(row: any) {
  editForm.id = row.id
  editForm.status = row.status
  editForm.remark = row.remark || ''
  editDialogVisible.value = true
}

async function saveEditRecord() {
  try {
    await updateRecord(editForm.id, { status: editForm.status, remark: editForm.remark })
    ElMessage.success('修改成功')
    editDialogVisible.value = false
    loadRecords()
  } catch (e: any) {
    ElMessage.error('修改失败')
  }
}

async function delRecord(id: number) {
  try {
    await ElMessageBox.confirm('确定删除此条考勤记录？', '确认', { type: 'warning' })
    await deleteRecord(id)
    ElMessage.success('已删除')
    loadRecords()
  } catch {
    // cancelled
  }
}

// ==================== Tab 3: Statistics ====================
const statsFilter = reactive({
  semesterId: null as number | null,
  orgUnitId: null as number | null,
  dateRange: null as [string, string] | null,
})
const stats = ref<AttendanceStats | null>(null)

async function loadStats() {
  if (!statsFilter.semesterId) {
    stats.value = null
    return
  }
  try {
    const params: any = { semesterId: statsFilter.semesterId }
    if (statsFilter.orgUnitId) params.orgUnitId = statsFilter.orgUnitId
    if (statsFilter.dateRange?.[0]) params.startDate = statsFilter.dateRange[0]
    if (statsFilter.dateRange?.[1]) params.endDate = statsFilter.dateRange[1]

    stats.value = await getStatistics(params)
  } catch (e: any) {
    ElMessage.error('加载统计失败')
  }
}

function barWidth(count: number): string {
  if (!stats.value || stats.value.total === 0) return '0%'
  return (count / stats.value.total * 100).toFixed(1) + '%'
}

// ==================== Tab 4: Leave Requests ====================
const leaveFilter = reactive({
  approvalStatus: null as number | null,
  orgUnitId: null as number | null,
})
const leaveList = ref<any[]>([])
const leavesLoading = ref(false)
const showLeaveDialog = ref(false)
const leaveStudents = ref<any[]>([])
const leaveForm = reactive({
  orgUnitId: null as number | null,
  studentId: null as number | null,
  leaveType: 1,
  dateRange: null as [string, string] | null,
  reason: '',
})

async function loadLeaves() {
  leavesLoading.value = true
  try {
    const params: any = {}
    if (leaveFilter.approvalStatus !== null && leaveFilter.approvalStatus !== undefined) {
      params.approvalStatus = leaveFilter.approvalStatus
    }
    if (leaveFilter.orgUnitId) params.orgUnitId = leaveFilter.orgUnitId
    leaveList.value = (await listLeaves(params)) || []
  } catch (e: any) {
    ElMessage.error('加载请假列表失败')
  } finally {
    leavesLoading.value = false
  }
}

async function loadLeaveStudents() {
  if (!leaveForm.orgUnitId) return
  try {
    const students = await http.get<any[]>('/organization/classes/' + leaveForm.orgUnitId + '/students')
    leaveStudents.value = students || []
  } catch {
    leaveStudents.value = []
  }
}

async function submitLeave() {
  if (!leaveForm.studentId || !leaveForm.dateRange || !leaveForm.reason) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    await createLeave({
      studentId: leaveForm.studentId,
      leaveType: leaveForm.leaveType,
      startDate: leaveForm.dateRange[0],
      endDate: leaveForm.dateRange[1],
      reason: leaveForm.reason,
    })
    ElMessage.success('请假申请已提交')
    showLeaveDialog.value = false
    leaveForm.studentId = null
    leaveForm.reason = ''
    leaveForm.dateRange = null
    loadLeaves()
  } catch (e: any) {
    ElMessage.error('提交失败')
  }
}

async function handleApprove(id: number) {
  try {
    await ElMessageBox.confirm('确认通过该请假申请？', '审批', { type: 'info' })
    await approveLeave(id)
    ElMessage.success('已通过')
    loadLeaves()
  } catch {
    // cancelled
  }
}

// Reject
const rejectDialogVisible = ref(false)
const rejectComment = ref('')
const rejectTargetId = ref<number | null>(null)

function handleReject(id: number) {
  rejectTargetId.value = id
  rejectComment.value = ''
  rejectDialogVisible.value = true
}

async function confirmReject() {
  if (rejectTargetId.value == null) return
  try {
    await rejectLeave(rejectTargetId.value, rejectComment.value || undefined)
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    loadLeaves()
  } catch {
    ElMessage.error('操作失败')
  }
}

function leaveTypeLabel(type: number) {
  const map: Record<number, string> = { 1: '事假', 2: '病假', 3: '公假' }
  return map[type] || '未知'
}

// ==================== Helpers ====================
function getStatusLabel(status: number) {
  const opt = statusOptions.find((o) => o.value === status)
  return opt?.label || '未知'
}

function getStatusTag(status: number): string {
  const opt = statusOptions.find((o) => o.value === status)
  return opt?.color || ''
}

// ==================== Init ====================
onMounted(async () => {
  await loadBaseData()
})
</script>
