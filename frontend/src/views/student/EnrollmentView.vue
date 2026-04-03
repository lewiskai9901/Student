<template>
  <div class="flex h-full flex-col bg-gray-50">
    <!-- Header -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">招生管理</h1>
        <p class="mt-0.5 text-sm text-gray-500">招生计划制定、报名审核、录取与报到注册</p>
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
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </nav>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
      <!-- ==================== Tab 1: 招生计划 ==================== -->
      <div v-if="activeTab === 'plans'">
        <!-- Filter + Stats -->
        <div class="mb-4 flex flex-wrap items-center gap-3">
          <el-select v-model="planFilter.year" placeholder="招生年份" clearable class="w-32" @change="loadPlans">
            <el-option v-for="y in yearOptions" :key="y" :value="y" :label="y + '年'" />
          </el-select>
          <el-select v-model="planFilter.majorId" placeholder="专业" clearable filterable class="w-40" @change="loadPlans">
            <el-option v-for="m in majors" :key="m.id" :value="m.id" :label="m.name" />
          </el-select>
          <el-select v-model="planFilter.status" placeholder="状态" clearable class="w-28" @change="loadPlans">
            <el-option :value="0" label="草稿" />
            <el-option :value="1" label="已发布" />
            <el-option :value="2" label="招生中" />
            <el-option :value="3" label="已结束" />
          </el-select>
          <button
            class="ml-auto inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
            @click="openPlanDialog()"
          >
            新增计划
          </button>
        </div>

        <!-- Stats bar -->
        <div v-if="planStats" class="mb-4 flex items-center gap-4 rounded-lg border border-gray-200 bg-white px-5 py-3 text-sm">
          <span class="text-gray-500">总计划 <span class="font-semibold text-gray-900">{{ planStats.totalPlanned }}</span></span>
          <span class="text-gray-300">|</span>
          <span class="text-gray-500">已录取 <span class="font-semibold text-green-600">{{ planStats.totalAdmitted }}</span></span>
          <span class="text-gray-300">|</span>
          <span class="text-gray-500">已报到 <span class="font-semibold text-blue-600">{{ planStats.totalRegistered }}</span></span>
          <span class="text-gray-300">|</span>
          <span class="text-gray-500">完成率 <span class="font-semibold text-gray-900">{{ planStats.completionRate }}%</span></span>
        </div>

        <!-- Plan table -->
        <div class="rounded-lg border border-gray-200 bg-white">
          <el-table :data="plans" v-loading="plansLoading" stripe>
            <el-table-column prop="academicYear" label="年份" width="80" align="center" />
            <el-table-column prop="majorName" label="专业" min-width="120" />
            <el-table-column prop="majorDirectionName" label="方向" min-width="100">
              <template #default="{ row }">{{ row.majorDirectionName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="orgUnitName" label="系部" min-width="100">
              <template #default="{ row }">{{ row.orgUnitName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="plannedCount" label="计划数" width="80" align="center" />
            <el-table-column prop="actualCount" label="已录取" width="80" align="center">
              <template #default="{ row }">
                <span :class="row.actualCount > 0 ? 'text-green-600 font-medium' : ''">{{ row.actualCount }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="registeredCount" label="已报到" width="80" align="center">
              <template #default="{ row }">
                <span :class="row.registeredCount > 0 ? 'text-blue-600 font-medium' : ''">{{ row.registeredCount }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="enrollmentTarget" label="招生对象" width="100" />
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="planStatusType(row.status)" size="small">{{ planStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" align="center" fixed="right">
              <template #default="{ row }">
                <button
                  v-if="row.status === 0"
                  class="mr-2 text-sm text-green-600 hover:text-green-800"
                  @click="publishPlan(row)"
                >发布</button>
                <button class="mr-2 text-sm text-blue-600 hover:text-blue-800" @click="openPlanDialog(row)">编辑</button>
                <button class="text-sm text-red-500 hover:text-red-700" @click="deletePlan(row)">删除</button>
              </template>
            </el-table-column>
          </el-table>
          <div class="flex justify-end px-4 py-3">
            <el-pagination
              v-model:current-page="planFilter.page"
              v-model:page-size="planFilter.size"
              :total="planTotal"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              small
              @size-change="loadPlans"
              @current-change="loadPlans"
            />
          </div>
        </div>
      </div>

      <!-- ==================== Tab 2: 报名管理 ==================== -->
      <div v-if="activeTab === 'applications'">
        <!-- Filter row -->
        <div class="mb-4 flex flex-wrap items-center gap-3">
          <el-select v-model="appFilter.year" placeholder="年份" clearable class="w-32" @change="loadApplications">
            <el-option v-for="y in yearOptions" :key="y" :value="y" :label="y + '年'" />
          </el-select>
          <el-select v-model="appFilter.majorId" placeholder="专业" clearable filterable class="w-40" @change="loadApplications">
            <el-option v-for="m in majors" :key="m.id" :value="m.id" :label="m.name" />
          </el-select>
          <el-select v-model="appFilter.status" placeholder="状态" clearable class="w-28" @change="loadApplications">
            <el-option v-for="s in APPLICATION_STATUS" :key="s.value" :value="s.value" :label="s.label" />
          </el-select>
          <el-input
            v-model="appFilter.keyword"
            placeholder="搜索姓名/身份证/电话"
            clearable
            class="!w-52"
            @keyup.enter="loadApplications"
            @clear="loadApplications"
          />
          <button
            class="inline-flex items-center gap-1.5 rounded-lg bg-gray-100 px-3.5 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-200"
            @click="loadApplications"
          >
            查询
          </button>
          <button
            class="inline-flex items-center gap-1.5 rounded-lg bg-gray-100 px-3.5 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-200"
            @click="exportApplications"
          >
            导出
          </button>
          <button
            v-if="selectedAppIds.length > 0"
            class="inline-flex items-center gap-1.5 rounded-lg bg-green-600 px-3.5 py-2 text-sm font-medium text-white transition-colors hover:bg-green-700"
            @click="batchAdmit"
          >
            批量录取 ({{ selectedAppIds.length }})
          </button>
          <button
            class="ml-auto inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
            @click="openAppDialog()"
          >
            新增报名
          </button>
        </div>

        <!-- Application table -->
        <div class="rounded-lg border border-gray-200 bg-white">
          <el-table
            :data="applications"
            v-loading="appsLoading"
            stripe
            @selection-change="onAppSelectionChange"
          >
            <el-table-column type="selection" width="45" :selectable="(row: any) => row.status === 0" />
            <el-table-column prop="applicantName" label="姓名" width="90" />
            <el-table-column prop="gender" label="性别" width="60" align="center">
              <template #default="{ row }">{{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '-' }}</template>
            </el-table-column>
            <el-table-column prop="idCard" label="身份证号" width="170">
              <template #default="{ row }">{{ row.idCard || '-' }}</template>
            </el-table-column>
            <el-table-column prop="phone" label="电话" width="120">
              <template #default="{ row }">{{ row.phone || '-' }}</template>
            </el-table-column>
            <el-table-column prop="majorName" label="报考专业" min-width="100" />
            <el-table-column prop="majorDirectionName" label="方向" width="100">
              <template #default="{ row }">{{ row.majorDirectionName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="graduateFrom" label="毕业学校" min-width="100">
              <template #default="{ row }">{{ row.graduateFrom || '-' }}</template>
            </el-table-column>
            <el-table-column prop="examScore" label="成绩" width="70" align="center">
              <template #default="{ row }">{{ row.examScore ?? '-' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="appStatusType(row.status)" size="small">{{ appStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="assignedClassName" label="分配班级" width="110">
              <template #default="{ row }">{{ row.assignedClassName || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="200" align="center" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 0">
                  <button class="mr-2 text-sm text-green-600 hover:text-green-800" @click="admitApp(row)">录取</button>
                  <button class="mr-2 text-sm text-red-500 hover:text-red-700" @click="rejectApp(row)">拒绝</button>
                  <button class="mr-2 text-sm text-blue-600 hover:text-blue-800" @click="openAppDialog(row)">编辑</button>
                </template>
                <template v-else-if="row.status === 1">
                  <button class="mr-2 text-sm text-blue-600 hover:text-blue-800" @click="openRegisterDialog(row)">报到注册</button>
                </template>
                <template v-if="row.status === 3">
                  <span class="text-xs text-gray-400">已完成</span>
                </template>
                <button
                  v-if="row.status !== 3"
                  class="text-sm text-red-500 hover:text-red-700"
                  @click="deleteApp(row)"
                >删除</button>
              </template>
            </el-table-column>
          </el-table>
          <div class="flex justify-end px-4 py-3">
            <el-pagination
              v-model:current-page="appFilter.page"
              v-model:page-size="appFilter.size"
              :total="appTotal"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              small
              @size-change="loadApplications"
              @current-change="loadApplications"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== Plan Dialog ==================== -->
    <el-dialog
      v-model="planDialogVisible"
      :title="planForm.id ? '编辑招生计划' : '新增招生计划'"
      width="560px"
      destroy-on-close
    >
      <el-form :model="planForm" label-width="90px" class="pr-4">
        <el-form-item label="招生年份" required>
          <el-select v-model="planForm.academicYear" placeholder="选择年份" class="w-full">
            <el-option v-for="y in yearOptions" :key="y" :value="y" :label="y + '年'" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业" required>
          <el-select
            v-model="planForm.majorId"
            placeholder="选择专业"
            filterable
            class="w-full"
            @change="onPlanMajorChange"
          >
            <el-option v-for="m in majors" :key="m.id" :value="m.id" :label="m.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业方向">
          <el-select v-model="planForm.majorDirectionId" placeholder="选择方向(可选)" clearable class="w-full">
            <el-option v-for="d in planDirections" :key="d.id" :value="d.id" :label="d.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="招收系部">
          <el-select v-model="planForm.orgUnitId" placeholder="选择系部(可选)" clearable filterable class="w-full">
            <el-option v-for="o in orgUnits" :key="o.id" :value="o.id" :label="o.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划人数" required>
          <el-input-number v-model="planForm.plannedCount" :min="1" :max="9999" class="w-full" />
        </el-form-item>
        <el-form-item label="招生对象">
          <el-input v-model="planForm.enrollmentTarget" placeholder="如: 初中毕业生" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="planForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="planSaving" @click="savePlan">保存</el-button>
      </template>
    </el-dialog>

    <!-- ==================== Application Dialog ==================== -->
    <el-dialog
      v-model="appDialogVisible"
      :title="appForm.id ? '编辑报名记录' : '新增报名'"
      width="620px"
      destroy-on-close
    >
      <el-form :model="appForm" label-width="90px" class="pr-4">
        <div class="grid grid-cols-2 gap-x-4">
          <el-form-item label="招生计划" required class="col-span-2">
            <el-select v-model="appForm.planId" placeholder="选择招生计划" filterable class="w-full" @change="onAppPlanChange">
              <el-option
                v-for="p in availablePlans"
                :key="p.id"
                :value="p.id"
                :label="`${p.academicYear} - ${p.majorName || ''}${p.majorDirectionName ? '(' + p.majorDirectionName + ')' : ''}`"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="姓名" required>
            <el-input v-model="appForm.applicantName" placeholder="申请人姓名" />
          </el-form-item>
          <el-form-item label="性别">
            <el-select v-model="appForm.gender" placeholder="选择" clearable class="w-full">
              <el-option :value="1" label="男" />
              <el-option :value="2" label="女" />
            </el-select>
          </el-form-item>
          <el-form-item label="身份证号">
            <el-input v-model="appForm.idCard" placeholder="18位身份证号" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="appForm.phone" placeholder="手机号" />
          </el-form-item>
          <el-form-item label="监护人">
            <el-input v-model="appForm.guardianName" placeholder="监护人姓名" />
          </el-form-item>
          <el-form-item label="监护人电话">
            <el-input v-model="appForm.guardianPhone" />
          </el-form-item>
          <el-form-item label="毕业学校" class="col-span-2">
            <el-input v-model="appForm.graduateFrom" placeholder="毕业/在读学校" />
          </el-form-item>
          <el-form-item label="报考专业" required>
            <el-select v-model="appForm.majorId" placeholder="专业" filterable class="w-full" @change="onAppMajorChange">
              <el-option v-for="m in majors" :key="m.id" :value="m.id" :label="m.name" />
            </el-select>
          </el-form-item>
          <el-form-item label="专业方向">
            <el-select v-model="appForm.majorDirectionId" placeholder="方向(可选)" clearable class="w-full">
              <el-option v-for="d in appDirections" :key="d.id" :value="d.id" :label="d.name" />
            </el-select>
          </el-form-item>
          <el-form-item label="报名日期">
            <el-date-picker v-model="appForm.applicationDate" type="date" placeholder="日期" value-format="YYYY-MM-DD" class="!w-full" />
          </el-form-item>
          <el-form-item label="考试成绩">
            <el-input-number v-model="appForm.examScore" :min="0" :max="999" :precision="1" class="w-full" />
          </el-form-item>
        </div>
        <el-form-item label="备注">
          <el-input v-model="appForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="appSaving" @click="saveApp">保存</el-button>
      </template>
    </el-dialog>

    <!-- ==================== Register Dialog ==================== -->
    <el-dialog
      v-model="registerDialogVisible"
      title="报到注册"
      width="480px"
      destroy-on-close
    >
      <div class="mb-4 rounded-lg bg-blue-50 p-4 text-sm">
        <p><span class="text-gray-500">姓名:</span> <span class="font-medium">{{ registerTarget?.applicantName }}</span></p>
        <p class="mt-1"><span class="text-gray-500">专业:</span> {{ registerTarget?.majorName }}{{ registerTarget?.majorDirectionName ? ' - ' + registerTarget?.majorDirectionName : '' }}</p>
      </div>
      <el-form label-width="80px">
        <el-form-item label="分配班级" required>
          <el-select v-model="registerClassId" placeholder="选择班级" filterable class="w-full">
            <el-option v-for="c in classes" :key="c.id" :value="c.id" :label="c.name || c.className" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="registerSaving" @click="doRegister">确认报到</el-button>
      </template>
    </el-dialog>

    <!-- ==================== Reject Dialog ==================== -->
    <el-dialog v-model="rejectDialogVisible" title="不录取" width="420px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="审核意见">
          <el-input v-model="rejectComment" type="textarea" :rows="3" placeholder="填写不录取原因(可选)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectSaving" @click="doReject">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { enrollmentPlanApi, enrollmentApplicationApi } from '@/api/enrollment'
import { getAllEnabledMajors, getDirectionsByMajor } from '@/api/academic'
import { schoolClassApi } from '@/api/organization'
import { APPLICATION_STATUS, PLAN_STATUS } from '@/types/enrollment'
import { http } from '@/utils/request'

// ==================== Tabs ====================
const tabs = [
  { key: 'plans', label: '招生计划' },
  { key: 'applications', label: '报名管理' },
]
const activeTab = ref('plans')

function switchTab(key: string) {
  activeTab.value = key
  if (key === 'plans') {
    loadPlans()
    loadPlanStats()
  } else {
    loadApplications()
  }
}

// ==================== Common Data ====================
const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 5 }, (_, i) => currentYear + 1 - i)

const majors = ref<any[]>([])
const orgUnits = ref<any[]>([])
const classes = ref<any[]>([])

async function loadMajors() {
  try {
    majors.value = await getAllEnabledMajors()
  } catch { /* ignore */ }
}

async function loadOrgUnits() {
  try {
    const data = await http.get('/org-units/tree')
    // Flatten tree to list
    const flat: any[] = []
    const walk = (nodes: any[]) => {
      for (const n of nodes) {
        flat.push({ id: n.id, name: n.name })
        if (n.children?.length) walk(n.children)
      }
    }
    if (Array.isArray(data)) walk(data)
    orgUnits.value = flat
  } catch { /* ignore */ }
}

async function loadClasses() {
  try {
    classes.value = await schoolClassApi.getAll()
  } catch { /* ignore */ }
}

// ==================== Plans ====================
const plans = ref<any[]>([])
const planTotal = ref(0)
const plansLoading = ref(false)
const planStats = ref<any>(null)
const planFilter = reactive({ year: currentYear as number | null, majorId: null as number | null, status: null as number | null, page: 1, size: 20 })

async function loadPlans() {
  plansLoading.value = true
  try {
    const res: any = await enrollmentPlanApi.list({
      year: planFilter.year,
      majorId: planFilter.majorId,
      status: planFilter.status,
      page: planFilter.page,
      size: planFilter.size,
    })
    plans.value = res.records || []
    planTotal.value = res.total || 0
  } catch { /* ignore */ }
  plansLoading.value = false
}

async function loadPlanStats() {
  try {
    planStats.value = await enrollmentPlanApi.statistics({ year: planFilter.year })
  } catch { /* ignore */ }
}

// Plan dialog
const planDialogVisible = ref(false)
const planSaving = ref(false)
const planForm = reactive({
  id: null as number | null,
  academicYear: currentYear,
  majorId: null as number | null,
  majorDirectionId: null as number | null,
  orgUnitId: null as number | null,
  plannedCount: 40,
  enrollmentTarget: '初中毕业生',
  remark: '',
})
const planDirections = ref<any[]>([])

function openPlanDialog(row?: any) {
  if (row) {
    Object.assign(planForm, {
      id: row.id,
      academicYear: row.academicYear,
      majorId: row.majorId,
      majorDirectionId: row.majorDirectionId,
      orgUnitId: row.orgUnitId,
      plannedCount: row.plannedCount,
      enrollmentTarget: row.enrollmentTarget || '初中毕业生',
      remark: row.remark || '',
    })
    if (row.majorId) onPlanMajorChange(row.majorId)
  } else {
    Object.assign(planForm, {
      id: null,
      academicYear: currentYear,
      majorId: null,
      majorDirectionId: null,
      orgUnitId: null,
      plannedCount: 40,
      enrollmentTarget: '初中毕业生',
      remark: '',
    })
    planDirections.value = []
  }
  planDialogVisible.value = true
}

async function onPlanMajorChange(majorId: number) {
  planForm.majorDirectionId = null
  if (majorId) {
    try {
      planDirections.value = await getDirectionsByMajor(majorId)
    } catch { planDirections.value = [] }
  } else {
    planDirections.value = []
  }
}

async function savePlan() {
  if (!planForm.academicYear || !planForm.majorId || !planForm.plannedCount) {
    ElMessage.warning('请填写必填项')
    return
  }
  planSaving.value = true
  try {
    if (planForm.id) {
      await enrollmentPlanApi.update(planForm.id, planForm)
      ElMessage.success('更新成功')
    } else {
      await enrollmentPlanApi.create(planForm)
      ElMessage.success('创建成功')
    }
    planDialogVisible.value = false
    loadPlans()
    loadPlanStats()
  } catch { /* handled by interceptor */ }
  planSaving.value = false
}

async function publishPlan(row: any) {
  try {
    await ElMessageBox.confirm('确认发布该招生计划?', '发布确认')
    await enrollmentPlanApi.publish(row.id)
    ElMessage.success('已发布')
    loadPlans()
  } catch { /* cancelled */ }
}

async function deletePlan(row: any) {
  try {
    await ElMessageBox.confirm('确认删除该招生计划?', '删除确认', { type: 'warning' })
    await enrollmentPlanApi.delete(row.id)
    ElMessage.success('已删除')
    loadPlans()
    loadPlanStats()
  } catch { /* cancelled */ }
}

function planStatusLabel(s: number) {
  return PLAN_STATUS.find(x => x.value === s)?.label || '未知'
}
function planStatusType(s: number): any {
  return PLAN_STATUS.find(x => x.value === s)?.type || 'info'
}

// ==================== Applications ====================
const applications = ref<any[]>([])
const appTotal = ref(0)
const appsLoading = ref(false)
const selectedAppIds = ref<number[]>([])
const appFilter = reactive({
  year: currentYear as number | null,
  majorId: null as number | null,
  status: null as number | null,
  keyword: '',
  page: 1,
  size: 20,
})

async function loadApplications() {
  appsLoading.value = true
  try {
    const res: any = await enrollmentApplicationApi.list({
      year: appFilter.year,
      majorId: appFilter.majorId,
      status: appFilter.status,
      keyword: appFilter.keyword || undefined,
      page: appFilter.page,
      size: appFilter.size,
    })
    applications.value = res.records || []
    appTotal.value = res.total || 0
  } catch { /* ignore */ }
  appsLoading.value = false
}

function onAppSelectionChange(rows: any[]) {
  selectedAppIds.value = rows.map((r: any) => r.id)
}

function appStatusLabel(s: number) {
  return APPLICATION_STATUS.find(x => x.value === s)?.label || '未知'
}
function appStatusType(s: number): any {
  return APPLICATION_STATUS.find(x => x.value === s)?.type || 'info'
}

// App dialog
const appDialogVisible = ref(false)
const appSaving = ref(false)
const appForm = reactive({
  id: null as number | null,
  planId: null as number | null,
  applicantName: '',
  gender: null as number | null,
  idCard: '',
  phone: '',
  guardianName: '',
  guardianPhone: '',
  graduateFrom: '',
  majorId: null as number | null,
  majorDirectionId: null as number | null,
  applicationDate: '',
  examScore: null as number | null,
  remark: '',
})
const appDirections = ref<any[]>([])

// Available plans for selection in app dialog (published or in-progress)
const availablePlans = computed(() => {
  return plans.value.filter((p: any) => p.status >= 1 && p.status <= 2)
})

function openAppDialog(row?: any) {
  if (row) {
    Object.assign(appForm, {
      id: row.id,
      planId: row.planId,
      applicantName: row.applicantName,
      gender: row.gender,
      idCard: row.idCard || '',
      phone: row.phone || '',
      guardianName: row.guardianName || '',
      guardianPhone: row.guardianPhone || '',
      graduateFrom: row.graduateFrom || '',
      majorId: row.majorId,
      majorDirectionId: row.majorDirectionId,
      applicationDate: row.applicationDate || '',
      examScore: row.examScore,
      remark: row.remark || '',
    })
    if (row.majorId) onAppMajorChange(row.majorId)
  } else {
    Object.assign(appForm, {
      id: null,
      planId: null,
      applicantName: '',
      gender: null,
      idCard: '',
      phone: '',
      guardianName: '',
      guardianPhone: '',
      graduateFrom: '',
      majorId: null,
      majorDirectionId: null,
      applicationDate: new Date().toISOString().slice(0, 10),
      examScore: null,
      remark: '',
    })
    appDirections.value = []
  }
  // Load all plans for selection
  loadAllPlansForSelect()
  appDialogVisible.value = true
}

async function loadAllPlansForSelect() {
  try {
    const res: any = await enrollmentPlanApi.list({ year: appFilter.year, size: 200 })
    // Merge into plans for the computed
    plans.value = res.records || plans.value
  } catch { /* ignore */ }
}

function onAppPlanChange(planId: number) {
  const plan = plans.value.find((p: any) => p.id === planId)
  if (plan) {
    appForm.majorId = plan.majorId
    appForm.majorDirectionId = plan.majorDirectionId
    if (plan.majorId) onAppMajorChange(plan.majorId)
  }
}

async function onAppMajorChange(majorId: number) {
  appForm.majorDirectionId = null
  if (majorId) {
    try {
      appDirections.value = await getDirectionsByMajor(majorId)
    } catch { appDirections.value = [] }
  } else {
    appDirections.value = []
  }
}

async function saveApp() {
  if (!appForm.planId || !appForm.applicantName || !appForm.majorId) {
    ElMessage.warning('请填写必填项')
    return
  }
  appSaving.value = true
  try {
    if (appForm.id) {
      await enrollmentApplicationApi.update(appForm.id, appForm)
      ElMessage.success('更新成功')
    } else {
      await enrollmentApplicationApi.create(appForm)
      ElMessage.success('新增成功')
    }
    appDialogVisible.value = false
    loadApplications()
  } catch { /* handled by interceptor */ }
  appSaving.value = false
}

async function admitApp(row: any) {
  try {
    await ElMessageBox.confirm(`确认录取 ${row.applicantName}?`, '录取确认')
    await enrollmentApplicationApi.admit(row.id)
    ElMessage.success('已录取')
    loadApplications()
    loadPlanStats()
  } catch { /* cancelled */ }
}

// Reject
const rejectDialogVisible = ref(false)
const rejectComment = ref('')
const rejectSaving = ref(false)
let rejectTargetId: number | null = null

function rejectApp(row: any) {
  rejectTargetId = row.id
  rejectComment.value = ''
  rejectDialogVisible.value = true
}

async function doReject() {
  if (!rejectTargetId) return
  rejectSaving.value = true
  try {
    await enrollmentApplicationApi.reject(rejectTargetId, rejectComment.value || undefined)
    ElMessage.success('已标记为未录取')
    rejectDialogVisible.value = false
    loadApplications()
  } catch { /* handled */ }
  rejectSaving.value = false
}

async function batchAdmit() {
  if (!selectedAppIds.value.length) return
  try {
    await ElMessageBox.confirm(`确认批量录取 ${selectedAppIds.value.length} 名申请人?`, '批量录取')
    const res: any = await enrollmentApplicationApi.batchAdmit(selectedAppIds.value)
    ElMessage.success(`已录取 ${res.admitted} 人`)
    loadApplications()
    loadPlanStats()
  } catch { /* cancelled */ }
}

async function deleteApp(row: any) {
  try {
    await ElMessageBox.confirm('确认删除该报名记录?', '删除确认', { type: 'warning' })
    await enrollmentApplicationApi.delete(row.id)
    ElMessage.success('已删除')
    loadApplications()
  } catch { /* cancelled */ }
}

// Register
const registerDialogVisible = ref(false)
const registerSaving = ref(false)
const registerTarget = ref<any>(null)
const registerClassId = ref<number | null>(null)

function openRegisterDialog(row: any) {
  registerTarget.value = row
  registerClassId.value = null
  registerDialogVisible.value = true
}

async function doRegister() {
  if (!registerTarget.value || !registerClassId.value) {
    ElMessage.warning('请选择分配班级')
    return
  }
  registerSaving.value = true
  try {
    const res: any = await enrollmentApplicationApi.register(registerTarget.value.id, { classId: registerClassId.value })
    ElMessage.success(`报到成功，学号: ${res.studentNo}`)
    registerDialogVisible.value = false
    loadApplications()
    loadPlanStats()
  } catch { /* handled */ }
  registerSaving.value = false
}

// Export
async function exportApplications() {
  try {
    const blob: any = await enrollmentApplicationApi.export({
      year: appFilter.year,
      majorId: appFilter.majorId,
      status: appFilter.status,
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `报名记录_${appFilter.year || '全部'}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('导出失败')
  }
}

// ==================== Init ====================
onMounted(async () => {
  await Promise.all([loadMajors(), loadOrgUnits(), loadClasses()])
  loadPlans()
  loadPlanStats()
})
</script>
