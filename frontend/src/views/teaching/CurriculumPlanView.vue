<template>
  <div class="curriculum-plan-view">
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="年级">
          <el-select v-model="queryParams.gradeYear" placeholder="全部" clearable>
            <el-option v-for="year in gradeYears" :key="year" :value="year" :label="`${year}级`" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option :value="0" label="草稿" />
            <el-option :value="1" label="已发布" />
            <el-option :value="2" label="已废弃" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>培养方案列表</span>
          <el-button type="primary" @click="showPlanDialog()">新建方案</el-button>
        </div>
      </template>

      <el-table :data="plans" v-loading="loading" border stripe>
        <el-table-column prop="name" label="方案名称" min-width="200" />
        <el-table-column prop="majorName" label="专业" width="150" />
        <el-table-column prop="gradeYear" label="适用年级" width="100" align="center">
          <template #default="{ row }">{{ row.gradeYear }}级</template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="100" align="center" />
        <el-table-column prop="totalCredits" label="总学分" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text @click="viewPlan(row)">查看</el-button>
            <el-button size="small" text @click="showPlanDialog(row)">编辑</el-button>
            <el-button
              v-if="row.status === 0"
              size="small"
              text
              type="success"
              @click="publishPlan(row)"
            >发布</el-button>
            <el-button size="small" text @click="copyPlan(row)">复制</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="loadPlans"
        @current-change="loadPlans"
      />
    </el-card>

    <!-- 培养方案对话框 -->
    <el-dialog
      v-model="planDialogVisible"
      :title="planForm.id ? '编辑培养方案' : '新建培养方案'"
      width="600px"
    >
      <el-form ref="planFormRef" :model="planForm" :rules="planRules" label-width="100px">
        <el-form-item label="方案名称" prop="name">
          <el-input v-model="planForm.name" placeholder="如：计算机科学与技术专业2025级培养方案" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="专业" prop="majorId">
              <el-select v-model="planForm.majorId" placeholder="选择专业" style="width: 100%">
                <el-option v-for="m in majors" :key="m.id" :value="m.id" :label="m.name" />
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
            <el-form-item label="版本号" prop="version">
              <el-input v-model="planForm.version" placeholder="如：v1.0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总学分" prop="totalCredits">
              <el-input-number v-model="planForm.totalCredits" :min="0" :max="300" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="方案描述">
          <el-input v-model="planForm.description" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePlan">保存</el-button>
      </template>
    </el-dialog>

    <!-- 培养方案详情 -->
    <el-drawer v-model="detailDrawerVisible" title="培养方案详情" size="70%">
      <template v-if="currentPlan">
        <div class="plan-header">
          <h2>{{ currentPlan.name }}</h2>
          <div class="plan-meta">
            <span>专业：{{ currentPlan.majorName }}</span>
            <span>年级：{{ currentPlan.gradeYear }}级</span>
            <span>版本：{{ currentPlan.version }}</span>
            <span>总学分：{{ currentPlan.totalCredits }}</span>
            <el-tag :type="getStatusTag(currentPlan.status)">{{ getStatusName(currentPlan.status) }}</el-tag>
          </div>
        </div>

        <el-divider />

        <div class="plan-courses">
          <div class="section-header">
            <h3>课程设置</h3>
            <el-button v-if="currentPlan.status === 0" type="primary" size="small" @click="showCourseDialog()">
              添加课程
            </el-button>
          </div>

          <el-tabs v-model="activeSemester">
            <el-tab-pane
              v-for="sem in 8"
              :key="sem"
              :label="`第${sem}学期`"
              :name="String(sem)"
            >
              <el-table :data="getCoursesForSemester(sem)" border>
                <el-table-column prop="courseName" label="课程名称" min-width="150" />
                <el-table-column prop="courseCode" label="课程编码" width="100" />
                <el-table-column prop="credits" label="学分" width="80" align="center" />
                <el-table-column prop="courseCategory" label="课程类别" width="120">
                  <template #default="{ row }">
                    <el-tag size="small">{{ getCategoryName(row.courseCategory) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="isRequired" label="是否必修" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.isRequired ? 'danger' : 'success'" size="small">
                      {{ row.isRequired ? '必修' : '选修' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="weeklyHours" label="周学时" width="80" align="center" />
                <el-table-column v-if="currentPlan.status === 0" label="操作" width="100">
                  <template #default="{ row }">
                    <el-button size="small" text type="danger" @click="removeCourse(row)">移除</el-button>
                  </template>
                </el-table-column>
              </el-table>

              <div v-if="getCoursesForSemester(sem).length === 0" class="empty-semester">
                <el-empty description="暂无课程" :image-size="60" />
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </template>
    </el-drawer>

    <!-- 添加课程对话框 -->
    <el-dialog v-model="courseDialogVisible" title="添加课程" width="600px">
      <el-form ref="courseFormRef" :model="courseForm" :rules="courseRules" label-width="100px">
        <el-form-item label="选择课程" prop="courseId">
          <el-select
            v-model="courseForm.courseId"
            filterable
            remote
            :remote-method="searchCourses"
            placeholder="搜索课程"
            style="width: 100%"
          >
            <el-option
              v-for="c in availableCourses"
              :key="c.id"
              :value="c.id"
              :label="`${c.code} - ${c.name}`"
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
            <el-form-item label="是否必修">
              <el-switch v-model="courseForm.isRequired" />
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

    <!-- 复制方案对话框 -->
    <el-dialog v-model="copyDialogVisible" title="复制培养方案" width="400px">
      <el-form :model="copyForm" label-width="100px">
        <el-form-item label="新版本号">
          <el-input v-model="copyForm.newVersion" placeholder="如：v2.0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="copyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirmCopy">确定复制</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { curriculumPlanApi, courseApi } from '@/api/v2/teaching'
import type { CurriculumPlan, PlanCourse, Course, CurriculumPlanQueryParams } from '@/types/v2/teaching'

// 状态
const loading = ref(false)
const saving = ref(false)
const plans = ref<CurriculumPlan[]>([])
const total = ref(0)
const majors = ref<{ id: number; name: string }[]>([])
const availableCourses = ref<Course[]>([])
const planCourses = ref<PlanCourse[]>([])
const currentPlan = ref<CurriculumPlan>()

// 对话框状态
const planDialogVisible = ref(false)
const detailDrawerVisible = ref(false)
const courseDialogVisible = ref(false)
const copyDialogVisible = ref(false)

// 表单
const planFormRef = ref<FormInstance>()
const courseFormRef = ref<FormInstance>()
const planForm = ref<Partial<CurriculumPlan>>({})
const courseForm = ref<Partial<PlanCourse>>({})
const copyForm = ref({ planId: 0, newVersion: '' })
const activeSemester = ref('1')

const queryParams = reactive<CurriculumPlanQueryParams>({
  majorId: undefined,
  gradeYear: undefined,
  status: undefined,
  page: 1,
  size: 10,
})

// 年级选项
const currentYear = new Date().getFullYear()
const gradeYears = computed(() => {
  const years = []
  for (let i = currentYear + 1; i >= currentYear - 5; i--) {
    years.push(i)
  }
  return years
})

// 验证规则
const planRules: FormRules = {
  name: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
  majorId: [{ required: true, message: '请选择专业', trigger: 'change' }],
  gradeYear: [{ required: true, message: '请选择年级', trigger: 'change' }],
  version: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  totalCredits: [{ required: true, message: '请输入总学分', trigger: 'blur' }],
}

const courseRules: FormRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  semesterNumber: [{ required: true, message: '请选择学期', trigger: 'change' }],
  courseCategory: [{ required: true, message: '请选择课程类别', trigger: 'change' }],
}

// 方法
const loadPlans = async () => {
  loading.value = true
  try {
    const res = await curriculumPlanApi.list(queryParams)
    const data = res.data || res
    plans.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('Failed to load plans:', error)
  } finally {
    loading.value = false
  }
}

const search = () => {
  queryParams.page = 1
  loadPlans()
}

const resetQuery = () => {
  queryParams.majorId = undefined
  queryParams.gradeYear = undefined
  queryParams.status = undefined
  queryParams.page = 1
  loadPlans()
}

const showPlanDialog = (plan?: CurriculumPlan) => {
  planForm.value = plan
    ? { ...plan }
    : { gradeYear: currentYear, version: 'v1.0', totalCredits: 160, status: 0 }
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
  try {
    const res = await curriculumPlanApi.getCourses(plan.id)
    planCourses.value = res.data || res
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

const copyPlan = (plan: CurriculumPlan) => {
  copyForm.value = { planId: plan.id, newVersion: `${plan.version}-copy` }
  copyDialogVisible.value = true
}

const confirmCopy = async () => {
  saving.value = true
  try {
    await curriculumPlanApi.copyPlan(copyForm.value.planId, copyForm.value.newVersion)
    ElMessage.success('复制成功')
    copyDialogVisible.value = false
    loadPlans()
  } catch (error) {
    ElMessage.error('复制失败')
  } finally {
    saving.value = false
  }
}

const showCourseDialog = () => {
  courseForm.value = { semesterNumber: parseInt(activeSemester.value), isRequired: true, weeklyHours: 2 }
  courseDialogVisible.value = true
}

const searchCourses = async (query: string) => {
  if (query.length < 2) return
  try {
    const res = await courseApi.list({ keyword: query, page: 1, size: 20 })
    const data = res.data || res
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
    planCourses.value = res.data || res
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
    planCourses.value = res.data || res
  } catch (error) {
    ElMessage.error('移除失败')
  }
}

const getCoursesForSemester = (semester: number) => {
  return planCourses.value.filter(c => c.semesterNumber === semester)
}

const getStatusName = (status: number) => {
  const names: Record<number, string> = { 0: '草稿', 1: '已发布', 2: '已废弃' }
  return names[status] || '未知'
}

const getStatusTag = (status: number) => {
  const types: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    0: 'info',
    1: 'success',
    2: 'warning',
  }
  return types[status] || 'info'
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

onMounted(() => {
  loadPlans()
  // TODO: 加载专业列表
  majors.value = [
    { id: 1, name: '计算机科学与技术' },
    { id: 2, name: '软件工程' },
    { id: 3, name: '信息安全' },
  ]
})
</script>

<style scoped lang="scss">
.curriculum-plan-view {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.plan-header {
  h2 {
    margin: 0 0 12px 0;
  }

  .plan-meta {
    display: flex;
    gap: 16px;
    color: #606266;
    font-size: 14px;
    align-items: center;
  }
}

.plan-courses {
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h3 {
      margin: 0;
    }
  }
}

.empty-semester {
  padding: 40px 0;
}
</style>
