<template>
  <div class="flex h-full flex-col bg-gray-50">
    <!-- Header -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">培养方案</h1>
        <p class="mt-0.5 text-sm text-gray-500">管理专业培养方案与课程设置</p>
      </div>
      <button
        class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3.5 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
        @click="showPlanDialog()"
      >
        <Plus class="h-4 w-4" />
        新建方案
      </button>
    </div>

    <!-- Stat Bar -->
    <div class="flex items-center gap-4 border-b border-gray-200 bg-white px-6 py-2.5">
      <span class="text-sm text-gray-500">总数 <span class="font-semibold text-gray-900">{{ total }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">草稿 <span class="font-semibold text-gray-900">{{ statusCounts.draft }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">已发布 <span class="font-semibold text-gray-900">{{ statusCounts.published }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">已废弃 <span class="font-semibold text-gray-900">{{ statusCounts.deprecated }}</span></span>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
      <!-- Filter Bar -->
      <div class="mb-4 rounded-xl border border-gray-200 bg-white px-5 py-3">
        <el-form :inline="true" :model="queryParams" class="flex flex-wrap items-center gap-x-4 gap-y-2">
          <el-form-item label="专业" class="!mb-0">
            <el-select
              v-model="queryParams.majorId"
              placeholder="全部"
              clearable
              filterable
              class="!w-[180px]"
            >
              <el-option
                v-for="m in majors"
                :key="m.id"
                :value="m.id"
                :label="m.majorName"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="年级" class="!mb-0">
            <el-select v-model="queryParams.gradeYear" placeholder="全部" clearable class="!w-[120px]">
              <el-option v-for="year in gradeYears" :key="year" :value="year" :label="`${year}级`" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" class="!mb-0">
            <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-[120px]">
              <el-option :value="0" label="草稿" />
              <el-option :value="1" label="已发布" />
              <el-option :value="2" label="已废弃" />
            </el-select>
          </el-form-item>
          <el-form-item class="!mb-0">
            <button
              class="inline-flex items-center gap-1 rounded-md bg-blue-600 px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-blue-700"
              @click="search"
            >
              <Search class="h-3.5 w-3.5" />
              查询
            </button>
            <button
              class="ml-2 inline-flex items-center gap-1 rounded-md border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-600 transition-colors hover:bg-gray-50"
              @click="resetQuery"
            >
              <RotateCcw class="h-3.5 w-3.5" />
              重置
            </button>
          </el-form-item>
        </el-form>
      </div>

      <!-- Table Card -->
      <div class="rounded-xl border border-gray-200 bg-white">
        <div class="px-5 py-3">
          <el-table :data="plans" v-loading="loading" stripe class="w-full">
            <el-table-column prop="planName" label="方案名称" min-width="200">
              <template #default="{ row }">
                <button
                  class="text-left font-medium text-blue-600 transition-colors hover:text-blue-700"
                  @click="viewPlan(row)"
                >
                  {{ row.planName }}
                </button>
              </template>
            </el-table-column>
            <el-table-column prop="majorName" label="专业" width="150">
              <template #default="{ row }">
                {{ row.majorName || '--' }}
              </template>
            </el-table-column>
            <el-table-column prop="gradeYear" label="适用年级" width="100" align="center">
              <template #default="{ row }">{{ row.gradeYear }}级</template>
            </el-table-column>
            <el-table-column prop="version" label="版本" width="80" align="center">
              <template #default="{ row }">v{{ row.version }}</template>
            </el-table-column>
            <el-table-column prop="totalCredits" label="总学分" width="100" align="center">
              <template #default="{ row }">
                <span class="font-medium">{{ row.totalCredits }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <span
                  class="inline-flex rounded px-1.5 py-0.5 text-[11px] font-medium"
                  :class="statusBadgeClass(row.status)"
                >
                  {{ getStatusName(row.status) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <div class="flex items-center gap-1">
                  <button
                    class="rounded px-2 py-1 text-xs text-blue-600 transition-colors hover:bg-blue-50"
                    @click="viewPlan(row)"
                  >查看</button>
                  <button
                    class="rounded px-2 py-1 text-xs text-gray-600 transition-colors hover:bg-gray-100"
                    @click="showPlanDialog(row)"
                  >编辑</button>
                  <button
                    v-if="row.status === 0"
                    class="rounded px-2 py-1 text-xs text-emerald-600 transition-colors hover:bg-emerald-50"
                    @click="publishPlan(row)"
                  >发布</button>
                  <button
                    class="rounded px-2 py-1 text-xs text-gray-500 transition-colors hover:bg-gray-100"
                    @click="handleCopyPlan(row)"
                  >复制</button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- Pagination -->
        <div class="flex items-center justify-end border-t border-gray-100 px-5 py-3">
          <el-pagination
            v-model:current-page="queryParams.pageNum"
            v-model:page-size="queryParams.pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            small
            @size-change="loadPlans"
            @current-change="loadPlans"
          />
        </div>
      </div>
    </div>

    <!-- Plan Detail Drawer -->
    <el-drawer v-model="detailDrawerVisible" title="" size="70%" :with-header="false">
      <template v-if="currentPlan">
        <!-- Drawer Header -->
        <div class="border-b border-gray-200 px-6 py-4">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <h2 class="text-lg font-semibold text-gray-900">{{ currentPlan.planName }}</h2>
              <span
                class="inline-flex rounded px-1.5 py-0.5 text-[11px] font-medium"
                :class="statusBadgeClass(currentPlan.status)"
              >
                {{ getStatusName(currentPlan.status) }}
              </span>
            </div>
            <button
              class="rounded-md p-1.5 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
              @click="detailDrawerVisible = false"
            >
              <X class="h-5 w-5" />
            </button>
          </div>
          <!-- Meta info -->
          <div class="mt-2 flex flex-wrap items-center gap-4 text-sm text-gray-500">
            <span>专业：{{ currentPlan.majorName || '--' }}</span>
            <div class="h-3 w-px bg-gray-200" />
            <span>年级：{{ currentPlan.gradeYear }}级</span>
            <div class="h-3 w-px bg-gray-200" />
            <span>版本：v{{ currentPlan.version }}</span>
            <div class="h-3 w-px bg-gray-200" />
            <span>总学分：<span class="font-semibold text-gray-900">{{ currentPlan.totalCredits }}</span></span>
          </div>
        </div>

        <!-- Drawer Content -->
        <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
          <!-- Section Header -->
          <div class="mb-4 flex items-center justify-between">
            <h3 class="text-sm font-semibold text-gray-900">课程设置</h3>
            <button
              v-if="currentPlan.status === 0"
              class="inline-flex items-center gap-1 rounded-md bg-blue-600 px-2.5 py-1.5 text-xs font-medium text-white transition-colors hover:bg-blue-700"
              @click="showCourseDialog()"
            >
              <Plus class="h-3.5 w-3.5" />
              添加课程
            </button>
          </div>

          <!-- Semester Tabs -->
          <div class="rounded-xl border border-gray-200 bg-white">
            <el-tabs v-model="activeSemester" class="plan-tabs">
              <el-tab-pane
                v-for="sem in 8"
                :key="sem"
                :label="`第${sem}学期`"
                :name="String(sem)"
              >
                <div class="px-4 pb-4">
                  <el-table
                    v-if="getCoursesForSemester(sem).length > 0"
                    :data="getCoursesForSemester(sem)"
                    stripe
                    class="w-full"
                  >
                    <el-table-column prop="courseName" label="课程名称" min-width="150" />
                    <el-table-column prop="courseCode" label="课程编码" width="110" />
                    <el-table-column prop="credits" label="学分" width="80" align="center">
                      <template #default="{ row }">
                        <span class="font-medium">{{ row.credits }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column prop="courseCategory" label="课程类别" width="120">
                      <template #default="{ row }">
                        <span
                          class="inline-flex rounded px-1.5 py-0.5 text-[11px] font-medium"
                          :class="categoryBadgeClass(row.courseCategory)"
                        >
                          {{ getCategoryName(row.courseCategory) }}
                        </span>
                      </template>
                    </el-table-column>
                    <el-table-column prop="courseType" label="性质" width="80" align="center">
                      <template #default="{ row }">
                        <span
                          class="inline-flex rounded px-1.5 py-0.5 text-[11px] font-medium"
                          :class="row.courseType === 1 ? 'bg-red-50 text-red-600' : 'bg-emerald-50 text-emerald-600'"
                        >
                          {{ row.courseType === 1 ? '必修' : row.courseType === 2 ? '限选' : '任选' }}
                        </span>
                      </template>
                    </el-table-column>
                    <el-table-column prop="weeklyHours" label="周学时" width="80" align="center" />
                    <el-table-column v-if="currentPlan.status === 0" label="操作" width="80" align="center">
                      <template #default="{ row }">
                        <button
                          class="rounded px-1.5 py-0.5 text-xs text-red-500 transition-colors hover:bg-red-50"
                          @click="removeCourse(row)"
                        >移除</button>
                      </template>
                    </el-table-column>
                  </el-table>

                  <div v-else class="flex flex-col items-center justify-center py-10 text-gray-400">
                    <BookOpen class="mb-2 h-8 w-8 text-gray-300" />
                    <span class="text-sm">暂无课程</span>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
      </template>
    </el-drawer>

    <!-- Create/Edit Plan Dialog -->
    <el-dialog
      v-model="planDialogVisible"
      :title="planForm.id ? '编辑培养方案' : '新建培养方案'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="planFormRef" :model="planForm" :rules="planRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="方案编码" prop="planCode">
              <el-input v-model="planForm.planCode" placeholder="如：CS2025V1" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="方案名称" prop="planName">
              <el-input v-model="planForm.planName" placeholder="培养方案名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="专业" prop="majorId">
              <el-select
                v-model="planForm.majorId"
                placeholder="选择专业"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="m in majors"
                  :key="m.id"
                  :value="m.id"
                  :label="m.majorName"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="适用年级" prop="gradeYear">
              <el-select v-model="planForm.gradeYear" placeholder="选择年级" style="width: 100%">
                <el-option v-for="year in gradeYears" :key="year" :value="year" :label="`${year}级`" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="总学分" prop="totalCredits">
              <el-input-number v-model="planForm.totalCredits" :min="0" :max="300" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12" />
        </el-row>
        <el-form-item label="培养目标">
          <el-input v-model="planForm.trainingObjective" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePlan">保存</el-button>
      </template>
    </el-dialog>

    <!-- Add Course Dialog -->
    <el-dialog v-model="courseDialogVisible" title="添加课程" width="600px" :close-on-click-modal="false">
      <el-form ref="courseFormRef" :model="courseForm" :rules="courseRules" label-width="100px">
        <el-form-item label="选择课程" prop="courseId">
          <el-select
            v-model="courseForm.courseId"
            filterable
            remote
            :remote-method="searchCourses"
            placeholder="搜索课程名称或编码"
            style="width: 100%"
          >
            <el-option
              v-for="c in availableCourses"
              :key="c.id"
              :value="c.id"
              :label="`${c.courseCode} - ${c.courseName}`"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开课学期" prop="semesterNumber">
              <el-select v-model="courseForm.semesterNumber" style="width: 100%">
                <el-option v-for="sem in 8" :key="sem" :value="sem" :label="`第${sem}学期`" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程类别" prop="courseCategory">
              <el-select v-model="courseForm.courseCategory" style="width: 100%">
                <el-option :value="1" label="公共基础" />
                <el-option :value="2" label="专业基础" />
                <el-option :value="3" label="专业核心" />
                <el-option :value="4" label="专业选修" />
                <el-option :value="5" label="实践环节" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="课程性质" prop="courseType">
              <el-select v-model="courseForm.courseType" style="width: 100%">
                <el-option :value="1" label="必修" />
                <el-option :value="2" label="限选" />
                <el-option :value="3" label="任选" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="周学时">
              <el-input-number v-model="courseForm.weeklyHours" :min="1" :max="20" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="courseDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="addCourse">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Search, RotateCcw, X, BookOpen } from 'lucide-vue-next'
import {
  curriculumPlanApi, courseApi, getAllEnabledMajors,
} from '@/api/academic'
import type { CurriculumPlan, PlanCourse, Course, CurriculumPlanQueryParams, Major } from '@/types/academic'

// State
const loading = ref(false)
const saving = ref(false)
const plans = ref<CurriculumPlan[]>([])
const total = ref(0)
const majors = ref<Major[]>([])
const availableCourses = ref<Course[]>([])
const planCourses = ref<PlanCourse[]>([])
const currentPlan = ref<CurriculumPlan>()

// Dialog state
const planDialogVisible = ref(false)
const detailDrawerVisible = ref(false)
const courseDialogVisible = ref(false)

// Forms
const planFormRef = ref<FormInstance>()
const courseFormRef = ref<FormInstance>()
const planForm = ref<Partial<CurriculumPlan>>({})
const courseForm = ref<Partial<PlanCourse>>({})
const activeSemester = ref('1')

const queryParams = reactive<CurriculumPlanQueryParams>({
  majorId: undefined,
  gradeYear: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 10,
})

// Grade year options
const currentYear = new Date().getFullYear()
const gradeYears = computed(() => {
  const years = []
  for (let i = currentYear + 1; i >= currentYear - 5; i--) {
    years.push(i)
  }
  return years
})

// Computed stats from loaded data
const statusCounts = computed(() => {
  const draft = plans.value.filter(p => p.status === 0).length
  const published = plans.value.filter(p => p.status === 1).length
  const deprecated = plans.value.filter(p => p.status === 2).length
  return { draft, published, deprecated }
})

// Validation rules
const planRules: FormRules = {
  planCode: [{ required: true, message: '请输入方案编码', trigger: 'blur' }],
  planName: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
  majorId: [{ required: true, message: '请选择专业', trigger: 'change' }],
  gradeYear: [{ required: true, message: '请选择年级', trigger: 'change' }],
  totalCredits: [{ required: true, message: '请输入总学分', trigger: 'blur' }],
}

const courseRules: FormRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  semesterNumber: [{ required: true, message: '请选择学期', trigger: 'change' }],
  courseCategory: [{ required: true, message: '请选择课程类别', trigger: 'change' }],
  courseType: [{ required: true, message: '请选择课程性质', trigger: 'change' }],
}

// Status helpers
const getStatusName = (status: number) => {
  const names: Record<number, string> = { 0: '草稿', 1: '已发布', 2: '已废弃' }
  return names[status] || '未知'
}

const statusBadgeClass = (status: number) => {
  const classes: Record<number, string> = {
    0: 'bg-gray-100 text-gray-600',
    1: 'bg-emerald-50 text-emerald-600',
    2: 'bg-orange-50 text-orange-600',
  }
  return classes[status] || 'bg-gray-100 text-gray-600'
}

const getCategoryName = (category: number) => {
  const names: Record<number, string> = {
    1: '公共基础',
    2: '专业基础',
    3: '专业核心',
    4: '专业选修',
    5: '实践环节',
  }
  return names[category] || '其他'
}

const categoryBadgeClass = (category: number) => {
  const classes: Record<number, string> = {
    1: 'bg-blue-50 text-blue-600',
    2: 'bg-cyan-50 text-cyan-600',
    3: 'bg-purple-50 text-purple-600',
    4: 'bg-amber-50 text-amber-600',
    5: 'bg-teal-50 text-teal-600',
  }
  return classes[category] || 'bg-gray-100 text-gray-600'
}

// Data loading
const loadPlans = async () => {
  loading.value = true
  try {
    const res: any = await curriculumPlanApi.list(queryParams)
    const data = res.data || res
    plans.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('Failed to load plans:', error)
  } finally {
    loading.value = false
  }
}

const loadMajors = async () => {
  try {
    const res = await getAllEnabledMajors()
    majors.value = (res as any)?.data || res || []
  } catch (error) {
    console.error('Failed to load majors:', error)
    majors.value = []
  }
}

const search = () => {
  queryParams.pageNum = 1
  loadPlans()
}

const resetQuery = () => {
  queryParams.majorId = undefined
  queryParams.gradeYear = undefined
  queryParams.status = undefined
  queryParams.pageNum = 1
  loadPlans()
}

// Plan CRUD
const showPlanDialog = (plan?: CurriculumPlan) => {
  planForm.value = plan
    ? { ...plan }
    : { gradeYear: currentYear, totalCredits: 160, status: 0 }
  planDialogVisible.value = true
}

const savePlan = async () => {
  await planFormRef.value?.validate()
  saving.value = true
  try {
    if (planForm.value.id) {
      await curriculumPlanApi.update(planForm.value.id, planForm.value)
    } else {
      await curriculumPlanApi.create(planForm.value)
    }
    ElMessage.success('保存成功')
    planDialogVisible.value = false
    loadPlans()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const viewPlan = async (plan: CurriculumPlan) => {
  currentPlan.value = plan
  activeSemester.value = '1'
  try {
    const res = await curriculumPlanApi.getCourses(plan.id)
    planCourses.value = (res as any)?.data || res || []
  } catch (error) {
    console.error('Failed to load plan courses:', error)
  }
  detailDrawerVisible.value = true
}

const publishPlan = async (plan: CurriculumPlan) => {
  await ElMessageBox.confirm('发布后将无法修改课程设置，确定发布吗？', '提示', { type: 'warning' })
  try {
    await curriculumPlanApi.publish(plan.id)
    ElMessage.success('发布成功')
    loadPlans()
  } catch (error) {
    ElMessage.error('发布失败')
  }
}

const handleCopyPlan = async (plan: CurriculumPlan) => {
  await ElMessageBox.confirm(`确定复制方案"${plan.planName}"吗？将自动创建新版本。`, '复制确认', { type: 'info' })
  try {
    const res: any = await curriculumPlanApi.copyPlan(plan.id)
    const data = res.data || res
    ElMessage.success(`复制成功，新版本 v${data.version}，已复制 ${data.copiedCourses} 门课程`)
    loadPlans()
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

// Course management
const showCourseDialog = () => {
  courseForm.value = { semesterNumber: parseInt(activeSemester.value), courseType: 1, weeklyHours: 2 }
  courseDialogVisible.value = true
}

const searchCourses = async (query: string) => {
  if (query.length < 2) return
  try {
    const res = await courseApi.list({ keyword: query, pageNum: 1, pageSize: 20 })
    const data = (res as any)?.data || res
    availableCourses.value = data.records || []
  } catch (error) {
    console.error('Failed to search courses:', error)
  }
}

const addCourse = async () => {
  await courseFormRef.value?.validate()
  if (!currentPlan.value) return
  saving.value = true
  try {
    await curriculumPlanApi.addCourse(currentPlan.value.id, courseForm.value)
    ElMessage.success('添加成功')
    courseDialogVisible.value = false
    const res = await curriculumPlanApi.getCourses(currentPlan.value.id)
    planCourses.value = (res as any)?.data || res || []
  } catch (error) {
    ElMessage.error('添加失败')
  } finally {
    saving.value = false
  }
}

const removeCourse = async (course: PlanCourse) => {
  if (!currentPlan.value) return
  await ElMessageBox.confirm('确定移除该课程吗？', '提示', { type: 'warning' })
  try {
    await curriculumPlanApi.removeCourse(currentPlan.value.id, course.id)
    ElMessage.success('移除成功')
    const res = await curriculumPlanApi.getCourses(currentPlan.value.id)
    planCourses.value = (res as any)?.data || res || []
  } catch (error) {
    ElMessage.error('移除失败')
  }
}

const getCoursesForSemester = (semester: number) => {
  return planCourses.value.filter(c => c.semesterNumber === semester)
}

// Init
onMounted(() => {
  loadPlans()
  loadMajors()
})
</script>

<style>
/* Minimal overrides for el-tabs inside drawer - no scoped needed */
.plan-tabs .el-tabs__header {
  margin: 0;
  padding: 0 16px;
}
.plan-tabs .el-tabs__nav-wrap::after {
  height: 1px;
}
</style>
