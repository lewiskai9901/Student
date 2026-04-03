<template>
  <div class="flex h-full flex-col bg-gray-50">
    <!-- Top Header Bar -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">开课管理</h1>
        <p class="mt-0.5 text-sm text-gray-500">管理学期开课计划、班级课程确认与教学班组建</p>
      </div>
      <el-select v-model="semesterId" placeholder="选择学期" class="w-48" @change="onSemesterChange">
        <el-option v-for="s in semesters" :key="s.id" :value="s.id" :label="s.name" />
      </el-select>
    </div>

    <!-- Step Tabs -->
    <div class="flex items-center gap-0 border-b border-gray-200 bg-white px-6">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="relative px-5 py-3 text-sm font-medium transition-colors"
        :class="activeTab === tab.key ? 'text-blue-600' : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
        <div v-if="activeTab === tab.key" class="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-600" />
      </button>
    </div>

    <!-- ======================== Tab 1: Offering Plan ======================== -->
    <template v-if="activeTab === 'offering'">
      <!-- Stats bar -->
      <div class="flex items-center gap-4 border-b border-gray-200 bg-white px-6 py-2.5">
        <span class="text-sm text-gray-500">共 <span class="font-semibold text-gray-900">{{ offerings.length }}</span> 门课</span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">必修 <span class="font-semibold text-gray-900">{{ offeringRequiredCount }}</span></span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">选修 <span class="font-semibold text-gray-900">{{ offeringElectiveCount }}</span></span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">总周课时 <span class="font-semibold text-gray-900">{{ offeringTotalWeeklyHours }}</span></span>
      </div>

      <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
        <!-- Action bar -->
        <div class="mb-4 flex items-center gap-2">
          <button
            class="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            @click="showImportFromPlanDialog"
          >
            从培养方案导入
          </button>
          <button
            class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700"
            @click="showOfferingDialog()"
          >
            手动添加
          </button>
        </div>

        <!-- Table -->
        <div class="overflow-hidden rounded-xl border border-gray-200 bg-white">
          <el-table :data="offerings" v-loading="offeringLoading" stripe>
            <el-table-column prop="courseCode" label="课程代码" width="120" />
            <el-table-column prop="courseName" label="课程名称" min-width="150" />
            <el-table-column prop="applicableGrade" label="适用年级" width="100" />
            <el-table-column prop="weeklyHours" label="周课时" width="80" align="center" />
            <el-table-column label="课程类别" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="(row.courseType === 1 ? '' : row.courseType === 2 ? 'success' : 'info') as any">
                  {{ getCourseTypeName(row.courseType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="合堂" width="70" align="center">
              <template #default="{ row }">
                <span :class="row.allowCombined ? 'text-blue-600' : 'text-gray-300'" class="text-sm">
                  {{ row.allowCombined ? '是' : '否' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="走班" width="70" align="center">
              <template #default="{ row }">
                <span :class="row.allowWalking ? 'text-blue-600' : 'text-gray-300'" class="text-sm">
                  {{ row.allowWalking ? '是' : '否' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <span
                  class="inline-block rounded px-1.5 py-0.5 text-xs font-medium"
                  :class="getOfferingStatusClass(row.status)"
                >
                  {{ getOfferingStatusName(row.status) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" align="center" fixed="right">
              <template #default="{ row }">
                <button class="text-sm text-blue-600 hover:text-blue-800" @click="showOfferingDialog(row)">编辑</button>
                <button
                  v-if="row.status === 0"
                  class="ml-3 text-sm text-emerald-600 hover:text-emerald-800"
                  @click="confirmOffering(row)"
                >确认</button>
                <button class="ml-3 text-sm text-red-600 hover:text-red-800" @click="deleteOffering(row)">删除</button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- Empty state -->
        <div v-if="!offeringLoading && offerings.length === 0" class="mt-10 text-center text-sm text-gray-400">
          暂无开课计划，请选择学期后添加
        </div>
      </div>
    </template>

    <!-- ======================== Tab 2: Class Assignment ======================== -->
    <template v-if="activeTab === 'assignment'">
      <!-- Filters & Stats -->
      <div class="flex items-center gap-4 border-b border-gray-200 bg-white px-6 py-2.5">
        <el-select v-model="assignClassId" placeholder="选择班级" clearable class="w-48" @change="loadAssignments">
          <el-option v-for="c in classes" :key="c.id" :value="c.id" :label="c.className" />
        </el-select>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">该班本学期: <span class="font-semibold text-gray-900">{{ assignments.length }}</span> 门课</span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm" :class="assignmentTotalHours > 30 ? 'text-red-600 font-semibold' : 'text-gray-500'">
          总周课时: <span class="font-semibold" :class="assignmentTotalHours > 30 ? 'text-red-600' : 'text-gray-900'">{{ assignmentTotalHours }}</span>
          <span v-if="assignmentTotalHours > 30" class="ml-1 text-xs">(建议不超过30)</span>
        </span>
      </div>

      <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
        <!-- Action bar -->
        <div class="mb-4 flex items-center gap-2">
          <button
            class="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            :disabled="!assignClassId"
            @click="importAssignmentsFromPlan"
          >
            一键导入开课计划
          </button>
          <button
            class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700"
            :disabled="!assignClassId || assignments.length === 0"
            @click="batchConfirmAssignments"
          >
            确认全部
          </button>
        </div>

        <!-- Table -->
        <div class="overflow-hidden rounded-xl border border-gray-200 bg-white">
          <el-table :data="assignments" v-loading="assignmentLoading" stripe>
            <el-table-column prop="courseName" label="课程名称" min-width="200" />
            <el-table-column label="周课时" width="120" align="center">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.weeklyHours"
                  :min="1"
                  :max="20"
                  size="small"
                  controls-position="right"
                  class="!w-24"
                />
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'warning'">
                  {{ row.status === 1 ? '已确认' : '待确认' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <button class="text-sm text-red-600 hover:text-red-800" @click="deleteAssignment(row)">移除</button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="!assignmentLoading && !assignClassId" class="mt-10 text-center text-sm text-gray-400">
          请先选择班级查看课程分配
        </div>
        <div v-else-if="!assignmentLoading && assignClassId && assignments.length === 0" class="mt-10 text-center text-sm text-gray-400">
          该班暂无课程分配，点击"一键导入开课计划"开始
        </div>
      </div>
    </template>

    <!-- ======================== Tab 3: Teaching Class ======================== -->
    <template v-if="activeTab === 'teachingClass'">
      <!-- Stats bar -->
      <div class="flex items-center gap-4 border-b border-gray-200 bg-white px-6 py-2.5">
        <span class="text-sm text-gray-500">教学班总数 <span class="font-semibold text-gray-900">{{ teachingClasses.length }}</span></span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">普通 <span class="font-semibold text-gray-900">{{ tcNormalCount }}</span></span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">合堂 <span class="font-semibold text-gray-900">{{ tcCombinedCount }}</span></span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">走班 <span class="font-semibold text-gray-900">{{ tcWalkingCount }}</span></span>
      </div>

      <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
        <!-- Action bar -->
        <div class="mb-4 flex items-center gap-2">
          <button
            class="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            @click="autoGenerateTeachingClasses"
          >
            自动生成教学班
          </button>
          <button
            class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700"
            @click="showTeachingClassDialog()"
          >
            手动创建
          </button>
        </div>

        <!-- Table -->
        <div class="overflow-hidden rounded-xl border border-gray-200 bg-white">
          <el-table :data="teachingClasses" v-loading="tcLoading" stripe>
            <el-table-column prop="className" label="教学班名称" min-width="160" />
            <el-table-column label="类型" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="getClassTypeTagType(row.classType)">
                  {{ getClassTypeName(row.classType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="courseName" label="课程" min-width="140" />
            <el-table-column label="包含班级" min-width="160">
              <template #default="{ row }">
                <span v-if="row.members && row.members.length > 0" class="text-sm text-gray-600">
                  {{ row.members.filter((m: TeachingClassMember) => m.memberType === 1).map((m: TeachingClassMember) => m.adminClassName).join(', ') || '-' }}
                </span>
                <span v-else class="text-sm text-gray-400">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="studentCount" label="学生人数" width="100" align="center" />
            <el-table-column prop="weeklyHours" label="周课时" width="80" align="center" />
            <el-table-column label="操作" width="180" align="center" fixed="right">
              <template #default="{ row }">
                <button class="text-sm text-blue-600 hover:text-blue-800" @click="showTeachingClassDialog(row)">编辑</button>
                <button class="ml-3 text-sm text-cyan-600 hover:text-cyan-800" @click="showMembersDialog(row)">成员</button>
                <button class="ml-3 text-sm text-red-600 hover:text-red-800" @click="deleteTeachingClass(row)">删除</button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="!tcLoading && teachingClasses.length === 0" class="mt-10 text-center text-sm text-gray-400">
          暂无教学班，可点击"自动生成教学班"快速创建
        </div>
      </div>
    </template>

    <!-- ======================== Dialogs ======================== -->

    <!-- Offering Add/Edit Dialog -->
    <el-dialog
      v-model="offeringDialogVisible"
      :title="editingOffering ? '编辑开课' : '添加开课'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="offeringFormRef" :model="offeringForm" :rules="offeringRules" label-width="100px" class="pr-4">
        <el-form-item label="课程" prop="courseId">
          <el-select v-model="offeringForm.courseId" placeholder="选择课程" filterable class="w-full" @change="onOfferingCourseChange">
            <el-option v-for="c in allCourses" :key="c.id" :value="c.id" :label="`${c.courseCode} - ${c.courseName}`" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用年级" prop="applicableGrade">
          <el-input v-model="offeringForm.applicableGrade" placeholder="如: 2024级、全年级" />
        </el-form-item>
        <el-form-item label="周课时" prop="weeklyHours">
          <el-input-number v-model="offeringForm.weeklyHours" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="起始周" prop="startWeek">
          <el-input-number v-model="offeringForm.startWeek" :min="1" :max="30" />
        </el-form-item>
        <el-form-item label="结束周">
          <el-input-number v-model="offeringForm.endWeek" :min="offeringForm.startWeek" :max="30" />
        </el-form-item>
        <el-form-item label="课程类型">
          <el-select v-model="offeringForm.courseType" class="w-full">
            <el-option :value="1" label="必修" />
            <el-option :value="2" label="选修" />
            <el-option :value="3" label="通识" />
          </el-select>
        </el-form-item>
        <el-form-item label="允许合堂">
          <el-switch v-model="offeringForm.allowCombined" />
          <el-input-number
            v-if="offeringForm.allowCombined"
            v-model="offeringForm.maxCombinedClasses"
            :min="2"
            :max="10"
            class="ml-3"
            placeholder="最大合堂数"
          />
        </el-form-item>
        <el-form-item label="允许走班">
          <el-switch v-model="offeringForm.allowWalking" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="offeringForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offeringDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="offeringSaving" @click="saveOffering">保存</el-button>
      </template>
    </el-dialog>

    <!-- Import from Curriculum Plan Dialog -->
    <el-dialog v-model="importDialogVisible" title="从培养方案导入" width="480px" destroy-on-close>
      <el-form label-width="100px" class="pr-4">
        <el-form-item label="培养方案">
          <el-select v-model="importForm.planId" placeholder="选择培养方案" filterable class="w-full">
            <el-option
              v-for="p in curriculumPlans"
              :key="p.id"
              :value="p.id"
              :label="`${p.planName} (v${p.version})`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="限定班级">
          <el-select v-model="importForm.classIds" placeholder="可选，不选则全部导入" filterable multiple class="w-full">
            <el-option v-for="c in classes" :key="c.id" :value="c.id" :label="c.className" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="doImportFromPlan">导入</el-button>
      </template>
    </el-dialog>

    <!-- Teaching Class Create/Edit Dialog -->
    <el-dialog
      v-model="tcDialogVisible"
      :title="editingTC ? '编辑教学班' : '创建教学班'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="tcFormRef" :model="tcForm" :rules="tcRules" label-width="100px" class="pr-4">
        <el-form-item label="教学班名称" prop="className">
          <el-input v-model="tcForm.className" placeholder="如: 高一(1,2)班数学" />
        </el-form-item>
        <el-form-item label="课程" prop="courseId">
          <el-select v-model="tcForm.courseId" placeholder="选择课程" filterable class="w-full">
            <el-option v-for="c in allCourses" :key="c.id" :value="c.id" :label="`${c.courseCode} - ${c.courseName}`" />
          </el-select>
        </el-form-item>
        <el-form-item label="教学班类型" prop="classType">
          <el-radio-group v-model="tcForm.classType">
            <el-radio :value="1">普通</el-radio>
            <el-radio :value="2">合堂</el-radio>
            <el-radio :value="3">走班</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="周课时" prop="weeklyHours">
          <el-input-number v-model="tcForm.weeklyHours" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="起始周" prop="startWeek">
          <el-input-number v-model="tcForm.startWeek" :min="1" :max="30" />
        </el-form-item>
        <el-form-item label="结束周">
          <el-input-number v-model="tcForm.endWeek" :min="tcForm.startWeek" :max="30" />
        </el-form-item>
        <el-form-item label="教室类型">
          <el-input v-model="tcForm.requiredRoomType" placeholder="如: 多媒体教室、实验室" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="tcForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tcDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="tcSaving" @click="saveTeachingClass">保存</el-button>
      </template>
    </el-dialog>

    <!-- Members Dialog -->
    <el-dialog v-model="membersDialogVisible" title="教学班成员" width="640px" destroy-on-close>
      <div v-if="selectedTC" class="mb-4 text-sm text-gray-500">
        {{ selectedTC.className }} - {{ selectedTC.courseName }}
        <span class="ml-2">( {{ selectedTC.studentCount }} 人 )</span>
      </div>

      <!-- Add member -->
      <div class="mb-4 flex items-center gap-2">
        <el-select v-model="newMemberClassId" placeholder="选择行政班级" filterable class="flex-1">
          <el-option v-for="c in classes" :key="c.id" :value="c.id" :label="c.className" />
        </el-select>
        <el-button type="primary" size="small" :disabled="!newMemberClassId" @click="addClassMember">
          添加整班
        </el-button>
      </div>

      <!-- Members list -->
      <el-table :data="currentMembers" v-loading="membersLoading" stripe max-height="360">
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="(row.memberType === 1 ? '' : 'success') as any">
              {{ row.memberType === 1 ? '整班' : '个人' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="名称" min-width="160">
          <template #default="{ row }">
            {{ row.memberType === 1 ? row.adminClassName : row.studentName }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <button class="text-sm text-red-600 hover:text-red-800" @click="removeMember(row)">移除</button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { offeringApi, classAssignmentApi, teachingClassApi } from '@/api/teaching'
import { semesterApi } from '@/api/calendar'
import { courseApi, curriculumPlanApi } from '@/api/academic'
import { schoolClassApi } from '@/api/organization'
import type {
  SemesterOffering, ClassCourseAssignment, TeachingClass, TeachingClassMember,
  Semester, Course, CurriculumPlan,
} from '@/types/teaching'
import type { SchoolClass } from '@/types/organization'

// ==================== Common State ====================

const tabs = [
  { key: 'offering', label: '学期开课计划' },
  { key: 'assignment', label: '班级开课确认' },
  { key: 'teachingClass', label: '教学班组建' },
] as const

type TabKey = (typeof tabs)[number]['key']
const activeTab = ref<TabKey>('offering')

const semesterId = ref<number | string>('')
const semesters = ref<Semester[]>([])
const allCourses = ref<Course[]>([])
const classes = ref<SchoolClass[]>([])
const curriculumPlans = ref<CurriculumPlan[]>([])

// ==================== Tab 1: Offerings ====================

const offerings = ref<SemesterOffering[]>([])
const offeringLoading = ref(false)
const offeringDialogVisible = ref(false)
const offeringSaving = ref(false)
const editingOffering = ref<SemesterOffering | null>(null)
const offeringFormRef = ref<FormInstance>()

const offeringForm = ref({
  courseId: undefined as number | undefined,
  applicableGrade: '',
  weeklyHours: 2,
  startWeek: 1,
  endWeek: undefined as number | undefined,
  courseType: 1,
  allowCombined: false,
  maxCombinedClasses: 2,
  allowWalking: false,
  remark: '',
})

const offeringRules: FormRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  applicableGrade: [{ required: true, message: '请填写适用年级', trigger: 'blur' }],
  weeklyHours: [{ required: true, message: '请填写周课时', trigger: 'blur' }],
  startWeek: [{ required: true, message: '请填写起始周', trigger: 'blur' }],
}

const offeringRequiredCount = computed(() => offerings.value.filter(o => o.courseType === 1).length)
const offeringElectiveCount = computed(() => offerings.value.filter(o => o.courseType === 2).length)
const offeringTotalWeeklyHours = computed(() => offerings.value.reduce((sum, o) => sum + o.weeklyHours, 0))

// ==================== Tab 2: Assignments ====================

const assignClassId = ref<number | string>('')
const assignments = ref<ClassCourseAssignment[]>([])
const assignmentLoading = ref(false)

const assignmentTotalHours = computed(() => assignments.value.reduce((sum, a) => sum + a.weeklyHours, 0))

// ==================== Tab 3: Teaching Classes ====================

const teachingClasses = ref<TeachingClass[]>([])
const tcLoading = ref(false)
const tcDialogVisible = ref(false)
const tcSaving = ref(false)
const editingTC = ref<TeachingClass | null>(null)
const tcFormRef = ref<FormInstance>()

const tcForm = ref({
  className: '',
  courseId: undefined as number | undefined,
  classType: 1 as 1 | 2 | 3,
  weeklyHours: 2,
  startWeek: 1,
  endWeek: undefined as number | undefined,
  requiredRoomType: '',
  remark: '',
})

const tcRules: FormRules = {
  className: [{ required: true, message: '请填写教学班名称', trigger: 'blur' }],
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  classType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  weeklyHours: [{ required: true, message: '请填写周课时', trigger: 'blur' }],
  startWeek: [{ required: true, message: '请填写起始周', trigger: 'blur' }],
}

const tcNormalCount = computed(() => teachingClasses.value.filter(t => t.classType === 1).length)
const tcCombinedCount = computed(() => teachingClasses.value.filter(t => t.classType === 2).length)
const tcWalkingCount = computed(() => teachingClasses.value.filter(t => t.classType === 3).length)

// Members dialog
const membersDialogVisible = ref(false)
const membersLoading = ref(false)
const selectedTC = ref<TeachingClass | null>(null)
const currentMembers = ref<TeachingClassMember[]>([])
const newMemberClassId = ref<number | string>('')

// Import dialog
const importDialogVisible = ref(false)
const importLoading = ref(false)
const importForm = ref({
  planId: undefined as number | undefined,
  classIds: [] as number[],
})

// ==================== Helpers ====================

function getCourseTypeName(type: number) {
  const map: Record<number, string> = { 1: '必修', 2: '选修', 3: '通识' }
  return map[type] || '其他'
}

function getOfferingStatusName(status: number) {
  return status === 1 ? '已确认' : '待确认'
}

function getOfferingStatusClass(status: number) {
  return status === 1 ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'
}

function getClassTypeName(type: number) {
  const map: Record<number, string> = { 1: '普通', 2: '合堂', 3: '走班' }
  return map[type] || '-'
}

function getClassTypeTagType(type: number) {
  const map: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = { 1: '', 2: 'warning', 3: 'success' }
  return map[type] || 'info'
}

// ==================== Data Loading ====================

async function loadSemesters() {
  try {
    const data = await semesterApi.list()
    semesters.value = data
    // Auto-select current semester
    const current = data.find(s => s.isCurrent)
    if (current) {
      semesterId.value = current.id
    } else if (data.length > 0) {
      semesterId.value = data[0].id
    }
  } catch {
    ElMessage.error('加载学期列表失败')
  }
}

async function loadCourses() {
  try {
    allCourses.value = await courseApi.listAll()
  } catch {
    // non-critical
  }
}

async function loadClasses() {
  try {
    classes.value = await schoolClassApi.getAll()
  } catch {
    // non-critical
  }
}

async function loadCurriculumPlans() {
  try {
    const res = await curriculumPlanApi.list({ status: 1, pageNum: 1, pageSize: 200 })
    curriculumPlans.value = res.records || []
  } catch {
    // non-critical
  }
}

async function loadOfferings() {
  if (!semesterId.value) {
    offerings.value = []
    return
  }
  offeringLoading.value = true
  try {
    offerings.value = await offeringApi.list(semesterId.value)
  } catch {
    ElMessage.error('加载开课计划失败')
    offerings.value = []
  } finally {
    offeringLoading.value = false
  }
}

async function loadAssignments() {
  if (!semesterId.value || !assignClassId.value) {
    assignments.value = []
    return
  }
  assignmentLoading.value = true
  try {
    assignments.value = await classAssignmentApi.list(semesterId.value, assignClassId.value)
  } catch {
    ElMessage.error('加载班级课程分配失败')
    assignments.value = []
  } finally {
    assignmentLoading.value = false
  }
}

async function loadTeachingClasses() {
  if (!semesterId.value) {
    teachingClasses.value = []
    return
  }
  tcLoading.value = true
  try {
    teachingClasses.value = await teachingClassApi.list(semesterId.value)
  } catch {
    ElMessage.error('加载教学班列表失败')
    teachingClasses.value = []
  } finally {
    tcLoading.value = false
  }
}

function onSemesterChange() {
  loadOfferings()
  loadAssignments()
  loadTeachingClasses()
}

// Watch active tab changes to load relevant data
watch(activeTab, (tab) => {
  if (tab === 'offering') loadOfferings()
  else if (tab === 'assignment') loadAssignments()
  else if (tab === 'teachingClass') loadTeachingClasses()
})

// ==================== Offering CRUD ====================

function onOfferingCourseChange(courseId: number) {
  const course = allCourses.value.find(c => Number(c.id) === courseId)
  if (course) {
    offeringForm.value.courseType = course.courseType
  }
}

function showOfferingDialog(row?: SemesterOffering) {
  editingOffering.value = row || null
  if (row) {
    offeringForm.value = {
      courseId: row.courseId,
      applicableGrade: row.applicableGrade || '',
      weeklyHours: row.weeklyHours,
      startWeek: row.startWeek,
      endWeek: row.endWeek,
      courseType: row.courseType || 1,
      allowCombined: row.allowCombined,
      maxCombinedClasses: row.maxCombinedClasses || 2,
      allowWalking: row.allowWalking,
      remark: row.remark || '',
    }
  } else {
    offeringForm.value = {
      courseId: undefined,
      applicableGrade: '',
      weeklyHours: 2,
      startWeek: 1,
      endWeek: undefined,
      courseType: 1,
      allowCombined: false,
      maxCombinedClasses: 2,
      allowWalking: false,
      remark: '',
    }
  }
  offeringDialogVisible.value = true
}

async function saveOffering() {
  if (!offeringFormRef.value) return
  const valid = await offeringFormRef.value.validate().catch(() => false)
  if (!valid) return

  offeringSaving.value = true
  try {
    const payload: Partial<SemesterOffering> = {
      semesterId: Number(semesterId.value),
      courseId: offeringForm.value.courseId,
      applicableGrade: offeringForm.value.applicableGrade,
      weeklyHours: offeringForm.value.weeklyHours,
      startWeek: offeringForm.value.startWeek,
      endWeek: offeringForm.value.endWeek,
      courseType: offeringForm.value.courseType,
      allowCombined: offeringForm.value.allowCombined,
      maxCombinedClasses: offeringForm.value.maxCombinedClasses,
      allowWalking: offeringForm.value.allowWalking,
      remark: offeringForm.value.remark,
    }

    if (editingOffering.value) {
      await offeringApi.update(editingOffering.value.id, payload)
      ElMessage.success('更新成功')
    } else {
      await offeringApi.create(payload)
      ElMessage.success('添加成功')
    }
    offeringDialogVisible.value = false
    loadOfferings()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    offeringSaving.value = false
  }
}

async function confirmOffering(row: SemesterOffering) {
  try {
    await ElMessageBox.confirm('确认该开课记录?', '确认', { type: 'info' })
    await offeringApi.confirm(row.id)
    ElMessage.success('已确认')
    loadOfferings()
  } catch {
    // cancelled or error
  }
}

async function deleteOffering(row: SemesterOffering) {
  try {
    await ElMessageBox.confirm('确定删除该开课记录?', '删除确认', { type: 'warning' })
    await offeringApi.delete(row.id)
    ElMessage.success('已删除')
    loadOfferings()
  } catch {
    // cancelled or error
  }
}

// ==================== Import from Plan ====================

function showImportFromPlanDialog() {
  if (!semesterId.value) {
    ElMessage.warning('请先选择学期')
    return
  }
  importForm.value = { planId: undefined, classIds: [] }
  importDialogVisible.value = true
  loadCurriculumPlans()
}

async function doImportFromPlan() {
  if (!importForm.value.planId) {
    ElMessage.warning('请选择培养方案')
    return
  }
  importLoading.value = true
  try {
    await offeringApi.importFromPlan({
      semesterId: Number(semesterId.value),
      planId: importForm.value.planId,
      classIds: importForm.value.classIds.length > 0 ? importForm.value.classIds : undefined,
    })
    ElMessage.success('导入成功')
    importDialogVisible.value = false
    loadOfferings()
  } catch {
    ElMessage.error('导入失败')
  } finally {
    importLoading.value = false
  }
}

// ==================== Assignment Operations ====================

async function importAssignmentsFromPlan() {
  if (!semesterId.value || !assignClassId.value) return
  try {
    await ElMessageBox.confirm('将从开课计划导入该班级的所有课程，是否继续?', '导入确认', { type: 'info' })
    assignmentLoading.value = true
    // Create assignments for each offering applicable to this class
    const existingCourseIds = new Set(assignments.value.map(a => a.courseId))
    const toImport = offerings.value.filter(o => !existingCourseIds.has(o.courseId))
    if (toImport.length === 0) {
      ElMessage.info('所有课程已分配，无需导入')
      assignmentLoading.value = false
      return
    }
    for (const o of toImport) {
      await classAssignmentApi.create({
        semesterId: Number(semesterId.value),
        classId: Number(assignClassId.value),
        offeringId: o.id,
        courseId: o.courseId,
        weeklyHours: o.weeklyHours,
        status: 0,
      })
    }
    ElMessage.success(`已导入 ${toImport.length} 门课程`)
    loadAssignments()
  } catch {
    ElMessage.error('导入失败')
  } finally {
    assignmentLoading.value = false
  }
}

async function batchConfirmAssignments() {
  if (!semesterId.value || !assignClassId.value) return
  try {
    await ElMessageBox.confirm('确认该班级所有课程分配?', '批量确认', { type: 'info' })
    await classAssignmentApi.batchConfirm(semesterId.value, assignClassId.value)
    ElMessage.success('全部确认成功')
    loadAssignments()
  } catch {
    // cancelled or error
  }
}

async function deleteAssignment(row: ClassCourseAssignment) {
  try {
    await ElMessageBox.confirm('确定移除该课程分配?', '移除确认', { type: 'warning' })
    await classAssignmentApi.delete(row.id)
    ElMessage.success('已移除')
    loadAssignments()
  } catch {
    // cancelled or error
  }
}

// ==================== Teaching Class CRUD ====================

function showTeachingClassDialog(row?: TeachingClass) {
  editingTC.value = row || null
  if (row) {
    tcForm.value = {
      className: row.className,
      courseId: row.courseId,
      classType: row.classType,
      weeklyHours: row.weeklyHours,
      startWeek: row.startWeek,
      endWeek: row.endWeek,
      requiredRoomType: row.requiredRoomType || '',
      remark: row.remark || '',
    }
  } else {
    tcForm.value = {
      className: '',
      courseId: undefined,
      classType: 1,
      weeklyHours: 2,
      startWeek: 1,
      endWeek: undefined,
      requiredRoomType: '',
      remark: '',
    }
  }
  tcDialogVisible.value = true
}

async function saveTeachingClass() {
  if (!tcFormRef.value) return
  const valid = await tcFormRef.value.validate().catch(() => false)
  if (!valid) return

  tcSaving.value = true
  try {
    const payload: Partial<TeachingClass> = {
      semesterId: Number(semesterId.value),
      className: tcForm.value.className,
      courseId: tcForm.value.courseId,
      classType: tcForm.value.classType,
      weeklyHours: tcForm.value.weeklyHours,
      startWeek: tcForm.value.startWeek,
      endWeek: tcForm.value.endWeek,
      requiredRoomType: tcForm.value.requiredRoomType || undefined,
      remark: tcForm.value.remark || undefined,
    }

    if (editingTC.value) {
      await teachingClassApi.update(editingTC.value.id, payload)
      ElMessage.success('更新成功')
    } else {
      await teachingClassApi.create(payload)
      ElMessage.success('创建成功')
    }
    tcDialogVisible.value = false
    loadTeachingClasses()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    tcSaving.value = false
  }
}

async function deleteTeachingClass(row: TeachingClass) {
  try {
    await ElMessageBox.confirm('确定删除该教学班?', '删除确认', { type: 'warning' })
    await teachingClassApi.delete(row.id)
    ElMessage.success('已删除')
    loadTeachingClasses()
  } catch {
    // cancelled or error
  }
}

async function autoGenerateTeachingClasses() {
  if (!semesterId.value) {
    ElMessage.warning('请先选择学期')
    return
  }
  try {
    await ElMessageBox.confirm(
      '将根据已确认的班级课程分配自动生成教学班。合堂课程会自动合并，走班课程会单独建班。是否继续?',
      '自动生成',
      { type: 'info' }
    )
    tcLoading.value = true
    await teachingClassApi.autoGenerate(semesterId.value)
    ElMessage.success('自动生成完成')
    loadTeachingClasses()
  } catch {
    // cancelled or error
  } finally {
    tcLoading.value = false
  }
}

// ==================== Members ====================

async function showMembersDialog(row: TeachingClass) {
  selectedTC.value = row
  membersDialogVisible.value = true
  newMemberClassId.value = ''
  await loadMembers(row.id)
}

async function loadMembers(tcId: number) {
  membersLoading.value = true
  try {
    currentMembers.value = await teachingClassApi.getMembers(tcId)
  } catch {
    currentMembers.value = []
  } finally {
    membersLoading.value = false
  }
}

async function addClassMember() {
  if (!selectedTC.value || !newMemberClassId.value) return
  try {
    await teachingClassApi.addMembers(selectedTC.value.id, [
      { teachingClassId: selectedTC.value.id, memberType: 1, adminClassId: Number(newMemberClassId.value) },
    ])
    ElMessage.success('已添加')
    newMemberClassId.value = ''
    loadMembers(selectedTC.value.id)
    loadTeachingClasses() // refresh student count
  } catch {
    ElMessage.error('添加失败')
  }
}

async function removeMember(row: TeachingClassMember) {
  if (!selectedTC.value) return
  try {
    await ElMessageBox.confirm('确定移除该成员?', '确认', { type: 'warning' })
    await teachingClassApi.removeMembers(selectedTC.value.id, [row.id])
    ElMessage.success('已移除')
    loadMembers(selectedTC.value.id)
    loadTeachingClasses()
  } catch {
    // cancelled or error
  }
}

// ==================== Init ====================

onMounted(async () => {
  await loadSemesters()
  loadCourses()
  loadClasses()
  // Initial data load after semester is auto-selected
  if (semesterId.value) {
    loadOfferings()
  }
})
</script>
