<template>
  <div class="p-6 bg-gray-50 min-h-full">
    <!-- 页面标题 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">班级管理</h1>
      <p class="mt-1 text-sm text-gray-500">管理班级信息、班主任分配和学生统计</p>
    </div>

    <!-- 统计卡片 - 设计系统 -->
    <div class="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
      <StatCard title="正常招生" :value="stats.active" :icon="CheckCircle" subtitle="活跃班级" :trend="5.2" color="emerald" />
      <StatCard title="停止招生" :value="stats.stopped" :icon="PauseCircle" subtitle="暂停班级" :trend="-1.5" color="orange" />
      <StatCard title="已毕业" :value="stats.graduated" :icon="Award" subtitle="完成学业" :trend="12.8" color="blue" />
      <StatCard title="学生总数" :value="stats.totalStudents" :icon="Users" subtitle="在读学生" :trend="8.3" color="purple" />
    </div>

    <!-- 搜索和操作区域 -->
    <div class="mb-4 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-end gap-3">
        <div class="w-40">
          <label class="mb-1 block text-xs font-medium text-gray-600">班级名称</label>
          <input
            v-model="searchForm.className"
            type="text"
            placeholder="请输入班级名称"
            class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
        <div class="w-40">
          <label class="mb-1 block text-xs font-medium text-gray-600">所属部门</label>
          <select
            v-model="searchForm.departmentId"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option :value="null">全部部门</option>
            <option v-for="item in departmentList" :key="item.id" :value="item.id">
              {{ item.departmentName }}
            </option>
          </select>
        </div>
        <div class="w-40">
          <label class="mb-1 block text-xs font-medium text-gray-600">年级</label>
          <select
            v-model="searchForm.gradeId"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option :value="null">全部年级</option>
            <option v-for="grade in gradeList" :key="grade.id" :value="grade.id">
              {{ grade.gradeName }}
            </option>
          </select>
        </div>
        <div class="w-36">
          <label class="mb-1 block text-xs font-medium text-gray-600">班主任</label>
          <input
            v-model="searchForm.teacherName"
            type="text"
            placeholder="班主任姓名"
            class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
        <div class="w-28">
          <label class="mb-1 block text-xs font-medium text-gray-600">状态</label>
          <select
            v-model="searchForm.status"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option :value="null">全部</option>
            <option :value="1">正常</option>
            <option :value="2">停招</option>
            <option :value="3">毕业</option>
          </select>
        </div>
        <button
          class="h-9 rounded-md bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          @click="handleSearch"
        >
          搜索
        </button>
        <button
          class="h-9 rounded-md border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
          @click="handleReset"
        >
          重置
        </button>
        <div class="flex-1"></div>
        <button
          v-if="hasPermission('student:class:export')"
          class="h-9 rounded-md border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
          @click="handleExport"
        >
          <Download class="mr-1.5 inline-block h-4 w-4" />
          导出
        </button>
        <button
          v-if="hasPermission('student:class:add')"
          class="h-9 rounded-md bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          @click="handleAdd"
        >
          <Plus class="mr-1.5 inline-block h-4 w-4" />
          新增班级
        </button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium text-gray-900">班级列表</span>
          <span class="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-600">{{ total }} 个</span>
        </div>
        <button
          v-if="hasPermission('student:class:delete') && selectedRows.length > 0"
          class="h-8 rounded-md bg-red-600 px-3 text-sm font-medium text-white hover:bg-red-700"
          @click="handleBatchDelete"
        >
          删除选中 ({{ selectedRows.length }})
        </button>
      </div>

      <!-- Loading 状态 -->
      <div v-if="loading" class="flex items-center justify-center py-16">
        <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
      </div>

      <!-- 表格 -->
      <div v-else class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="border-b border-gray-200 bg-gray-50">
              <th class="w-12 px-4 py-3">
                <input
                  type="checkbox"
                  class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                  :checked="isAllSelected"
                  @change="handleSelectAll"
                />
              </th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">班级名称</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">所属部门</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">年级</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">专业</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">班主任</th>
              <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">学生</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">状态</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr
              v-for="row in classList"
              :key="row.id"
              class="hover:bg-gray-50"
            >
              <td class="px-4 py-3">
                <input
                  type="checkbox"
                  class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                  :checked="selectedRows.some(s => s.id === row.id)"
                  @change="handleSelectRow(row)"
                />
              </td>
              <td class="px-4 py-3">
                <div class="flex items-center gap-2">
                  <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-100 text-sm font-medium text-blue-600">
                    {{ row.classSequence || '?' }}
                  </div>
                  <div>
                    <span class="text-sm font-medium text-gray-900">{{ row.className }}</span>
                    <p v-if="row.enrollmentYear" class="text-xs text-gray-500">{{ row.enrollmentYear }}级</p>
                  </div>
                </div>
              </td>
              <td class="px-4 py-3 text-sm text-gray-600">
                {{ row.departmentName || '-' }}
              </td>
              <td class="px-4 py-3 text-sm text-gray-900">
                {{ row.gradeName || row.grade || '-' }}
              </td>
              <td class="px-4 py-3">
                <div class="flex flex-col gap-0.5">
                  <span class="text-sm text-gray-900">{{ row.majorName || '-' }}</span>
                  <div v-if="row.educationSystem || row.skillLevel" class="flex gap-1">
                    <span v-if="row.educationSystem" class="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-600">
                      {{ row.educationSystem }}
                    </span>
                    <span v-if="row.skillLevel" class="rounded bg-green-50 px-1.5 py-0.5 text-xs text-green-700">
                      {{ row.skillLevel }}
                    </span>
                  </div>
                </div>
              </td>
              <td class="px-4 py-3">
                <div v-if="row.teacherName" class="flex items-center gap-2">
                  <div class="flex h-7 w-7 items-center justify-center rounded-full bg-blue-100 text-xs font-medium text-blue-600">
                    {{ row.teacherName.charAt(0) }}
                  </div>
                  <span class="text-sm text-gray-900">{{ row.teacherName }}</span>
                </div>
                <span v-else class="text-sm text-gray-400">未分配</span>
              </td>
              <td class="px-4 py-3 text-center">
                <button
                  v-if="hasPermission('student:info:view')"
                  class="inline-flex items-center gap-1 rounded bg-purple-50 px-2 py-0.5 text-xs font-medium text-purple-700 hover:bg-purple-100"
                  @click="handleViewStudents(row)"
                >
                  <Users class="h-3 w-3" />
                  {{ row.studentCount || 0 }}
                </button>
                <span v-else class="text-sm text-gray-600">{{ row.studentCount || 0 }}</span>
              </td>
              <td class="px-4 py-3">
                <span
                  class="inline-flex items-center rounded px-2 py-0.5 text-xs font-medium"
                  :class="getStatusClass(row.status)"
                >
                  {{ getStatusText(row.status) }}
                </span>
              </td>
              <td class="px-4 py-3 text-right">
                <div class="flex items-center justify-end gap-1">
                  <button
                    class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
                    title="查看详情"
                    @click="handleView(row)"
                  >
                    <Eye class="h-4 w-4" />
                  </button>
                  <button
                    v-if="hasPermission('student:class:edit')"
                    class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-blue-600"
                    title="编辑"
                    @click="handleEdit(row)"
                  >
                    <Pencil class="h-4 w-4" />
                  </button>
                  <button
                    v-if="hasPermission('student:class:edit')"
                    class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-green-600"
                    title="设置班主任"
                    @click="handleSetTeacher(row)"
                  >
                    <UserCog class="h-4 w-4" />
                  </button>
                  <button
                    v-if="hasPermission('student:class:edit')"
                    class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-purple-600"
                    title="宿舍管理"
                    @click="handleManageDormitory(row)"
                  >
                    <Building class="h-4 w-4" />
                  </button>
                  <button
                    v-if="hasPermission('student:class:delete')"
                    class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-red-600"
                    title="删除"
                    @click="handleDelete(row)"
                  >
                    <Trash2 class="h-4 w-4" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- 空状态 -->
        <div v-if="classList.length === 0" class="flex flex-col items-center justify-center py-16">
          <GraduationCap class="h-12 w-12 text-gray-300" />
          <p class="mt-3 text-sm text-gray-500">暂无班级数据</p>
          <button
            v-if="hasPermission('student:class:add')"
            class="mt-4 rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            @click="handleAdd"
          >
            创建班级
          </button>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="classList.length > 0" class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <div class="flex items-center gap-2 text-sm text-gray-500">
          <span>每页</span>
          <select
            v-model="pagination.pageSize"
            class="pagination-select"
            @change="handleSizeChange"
          >
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
          <span>条，共 {{ total }} 条</span>
        </div>
        <div class="flex items-center gap-1">
          <button
            class="flex h-8 w-8 items-center justify-center rounded border border-gray-300 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="pagination.pageNum <= 1"
            @click="handlePageChange(1)"
          >
            <ChevronsLeft class="h-4 w-4" />
          </button>
          <button
            class="flex h-8 w-8 items-center justify-center rounded border border-gray-300 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="pagination.pageNum <= 1"
            @click="handlePageChange(pagination.pageNum - 1)"
          >
            <ChevronLeft class="h-4 w-4" />
          </button>
          <template v-for="page in visiblePages" :key="page">
            <button
              v-if="page !== '...'"
              :class="[
                'flex h-8 min-w-[32px] items-center justify-center rounded px-2 text-sm',
                page === pagination.pageNum
                  ? 'bg-blue-600 text-white'
                  : 'border border-gray-300 hover:bg-gray-50'
              ]"
              @click="handlePageChange(page as number)"
            >
              {{ page }}
            </button>
            <span v-else class="px-1 text-gray-400">...</span>
          </template>
          <button
            class="flex h-8 w-8 items-center justify-center rounded border border-gray-300 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="pagination.pageNum >= totalPages"
            @click="handlePageChange(pagination.pageNum + 1)"
          >
            <ChevronRight class="h-4 w-4" />
          </button>
          <button
            class="flex h-8 w-8 items-center justify-center rounded border border-gray-300 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="pagination.pageNum >= totalPages"
            @click="handlePageChange(totalPages)"
          >
            <ChevronsRight class="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>

    <!-- 班级详情弹窗 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="detailDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div class="fixed inset-0 bg-black/40" @click="handleDetailClose" />
          <div class="relative z-10 w-full max-w-3xl max-h-[90vh] overflow-hidden rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b px-6 py-4">
              <h2 class="text-lg font-semibold text-gray-900">班级详情</h2>
              <button class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600" @click="handleDetailClose">
                <X class="h-5 w-5" />
              </button>
            </div>
            <div class="overflow-y-auto" style="max-height: calc(90vh - 65px)">
              <ClassDetail :class-id="currentClassId" @close="handleDetailClose" />
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 编辑班级弹窗 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="editDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div class="fixed inset-0 bg-black/40" @click="handleEditClose" />
          <div class="relative z-10 w-full max-w-2xl max-h-[90vh] overflow-hidden rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b px-6 py-4">
              <h2 class="text-lg font-semibold text-gray-900">{{ editMode === 'add' ? '新增班级' : '编辑班级' }}</h2>
              <button class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600" @click="handleEditClose">
                <X class="h-5 w-5" />
              </button>
            </div>
            <div class="overflow-y-auto" style="max-height: calc(90vh - 65px)">
              <ClassForm :mode="editMode" :class-id="currentClassId" @success="handleEditSuccess" @close="handleEditClose" />
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 设置班主任弹窗 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="teacherDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div class="fixed inset-0 bg-black/40" @click="handleTeacherClose" />
          <div class="relative z-10 w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
            <div class="mb-6 flex items-center justify-between">
              <h2 class="text-lg font-semibold text-gray-900">设置班主任</h2>
              <button class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600" @click="handleTeacherClose">
                <X class="h-5 w-5" />
              </button>
            </div>
            <div class="space-y-4">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">班级名称</label>
                <input
                  :value="currentClassName"
                  disabled
                  class="h-9 w-full rounded-md border border-gray-300 bg-gray-50 px-3 text-sm text-gray-500"
                />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">选择班主任</label>
                <select
                  v-model="teacherForm.teacherId"
                  class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                >
                  <option :value="null">请选择班主任</option>
                  <option v-for="teacher in teacherList" :key="teacher.id" :value="teacher.id">
                    {{ teacher.realName }}
                  </option>
                </select>
              </div>
            </div>
            <div class="mt-6 flex justify-end gap-3">
              <button
                class="h-9 rounded-md border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
                @click="handleTeacherClose"
              >
                取消
              </button>
              <button
                class="h-9 rounded-md bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
                :disabled="submitting"
                @click="handleTeacherSubmit"
              >
                <span v-if="submitting" class="mr-1.5 inline-block h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></span>
                确定
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 宿舍管理弹窗 -->
    <ClassDormitoryAssignmentDialog
      v-model:visible="dormitoryDialogVisible"
      :class-id="currentClassId || 0"
      :class-name="currentClassName"
      :department-id="currentDepartmentId || 0"
      @success="loadClassList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Download,
  Trash2,
  Eye,
  Pencil,
  Users,
  X,
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  GraduationCap,
  UserCog,
  CheckCircle,
  PauseCircle,
  Award,
  Building,
  Home
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { getClassList, getDepartmentList, deleteClass, batchDeleteClasses, exportClasses, assignTeacher, getTeacherList, getClassDormitories, addDormitory, removeDormitory, getDormitoryList } from '@/api/class'
import { getAllGrades } from '@/api/grade'
import type { Grade } from '@/api/grade'
import { exportExcel } from '@/utils/export'
import ClassDetail from '@/components/class/ClassDetail.vue'
import ClassForm from '@/components/class/ClassForm.vue'
import ClassDormitoryAssignmentDialog from '@/components/class/ClassDormitoryAssignmentDialog.vue'
import type { Class, ClassQueryParams, ClassDormitoryInfo } from '@/types/class'

const router = useRouter()
const authStore = useAuthStore()

// 权限检查
const hasPermission = (permission: string) => authStore.hasPermission(permission)

// 加载状态
const loading = ref(false)

// 统计数据
const stats = reactive({
  active: 0,
  stopped: 0,
  graduated: 0,
  totalStudents: 0
})

// 搜索表单
const searchForm = reactive<ClassQueryParams & { gradeId?: number | null }>({
  className: '',
  departmentId: null,
  gradeId: null,
  teacherName: '',
  status: null
})

// 分页参数
const pagination = reactive({
  pageNum: 1,
  pageSize: 20
})

// 数据
const classList = ref<Class[]>([])
const total = ref(0)
const selectedRows = ref<Class[]>([])
const departmentList = ref<any[]>([])
const teacherList = ref<any[]>([])
const gradeList = ref<Grade[]>([])

// 计算总页数
const totalPages = computed(() => Math.ceil(total.value / pagination.pageSize) || 1)

// 可见页码
const visiblePages = computed(() => {
  const pages: (number | string)[] = []
  const current = pagination.pageNum
  const totalP = totalPages.value

  if (totalP <= 7) {
    for (let i = 1; i <= totalP; i++) pages.push(i)
  } else {
    if (current <= 3) {
      for (let i = 1; i <= 5; i++) pages.push(i)
      pages.push('...')
      pages.push(totalP)
    } else if (current >= totalP - 2) {
      pages.push(1)
      pages.push('...')
      for (let i = totalP - 4; i <= totalP; i++) pages.push(i)
    } else {
      pages.push(1)
      pages.push('...')
      for (let i = current - 1; i <= current + 1; i++) pages.push(i)
      pages.push('...')
      pages.push(totalP)
    }
  }
  return pages
})

// 是否全选
const isAllSelected = computed(() => {
  return classList.value.length > 0 && selectedRows.value.length === classList.value.length
})

// 弹窗控制
const detailDialogVisible = ref(false)
const editDialogVisible = ref(false)
const teacherDialogVisible = ref(false)
const editMode = ref<'add' | 'edit'>('add')
const currentClassId = ref<number | null>(null)
const currentClassName = ref('')
const currentDepartmentId = ref<number | null>(null)
const submitting = ref(false)

// 设置班主任表单
const teacherForm = reactive({
  teacherId: null as number | null
})

// 宿舍管理相关状态
const dormitoryDialogVisible = ref(false)
const dormitoryLoading = ref(false)
const dormitorySubmitting = ref(false)
const classDormitories = ref<ClassDormitoryInfo[]>([])
const allDormitories = ref<any[]>([])
const dormitoryForm = reactive({
  dormitoryId: null as number | null,
  allocatedBeds: 1
})

// 计算可选宿舍（排除已分配的）
const availableDormitories = computed(() => {
  const assignedIds = new Set(classDormitories.value.map(d => d.dormitoryId))
  return allDormitories.value.filter(d => !assignedIds.has(d.id))
})

// 获取状态样式类
const getStatusClass = (status: number) => {
  const classMap: Record<number, string> = {
    1: 'bg-green-50 text-green-700',
    2: 'bg-amber-50 text-amber-700',
    3: 'bg-gray-100 text-gray-700'
  }
  return classMap[status] || 'bg-gray-100 text-gray-700'
}

// 获取状态文本
const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    1: '正常',
    2: '停招',
    3: '毕业'
  }
  return statusMap[status] || '未知'
}

// 加载班级列表
const loadClassList = async () => {
  loading.value = true
  try {
    const response = await getClassList({
      ...searchForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })

    classList.value = response.records || []
    total.value = response.total || 0

    // 计算统计数据
    stats.active = classList.value.filter(c => c.status === 1).length
    stats.stopped = classList.value.filter(c => c.status === 2).length
    stats.graduated = classList.value.filter(c => c.status === 3).length
    stats.totalStudents = classList.value.reduce((sum, c) => sum + (c.studentCount || 0), 0)
  } catch (error: any) {
    const message = error.response?.data?.message || '加载班级列表失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

// 加载部门列表
const loadDepartmentList = async () => {
  try {
    const response = await getDepartmentList()
    departmentList.value = response || []
  } catch (error: any) {
    console.error('加载部门列表失败:', error)
  }
}

// 加载年级列表
const loadGradeList = async () => {
  try {
    const response = await getAllGrades()
    gradeList.value = response || []
  } catch (error: any) {
    console.error('加载年级列表失败:', error)
  }
}

// 加载教师列表
const loadTeacherList = async () => {
  try {
    const response = await getTeacherList()
    teacherList.value = response || []
  } catch (error: any) {
    console.error('加载教师列表失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  loadClassList()
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, {
    className: '',
    departmentId: null,
    gradeId: null,
    teacherName: '',
    status: null
  })
  pagination.pageNum = 1
  loadClassList()
}

// 新增
const handleAdd = () => {
  editMode.value = 'add'
  currentClassId.value = null
  editDialogVisible.value = true
}

// 查看详情
const handleView = (row: Class) => {
  currentClassId.value = row.id
  detailDialogVisible.value = true
}

// 编辑
const handleEdit = (row: Class) => {
  editMode.value = 'edit'
  currentClassId.value = row.id
  editDialogVisible.value = true
}

// 查看学生
const handleViewStudents = (row: Class) => {
  router.push({
    path: '/students',
    query: {
      classId: row.id,
      className: row.className
    }
  })
}

// 删除
const handleDelete = async (row: Class) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除班级 "${row.className}" 吗？此操作不可恢复！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteClass(row.id)
    ElMessage.success('删除成功')
    loadClassList()
  } catch (error: any) {
    if (error !== 'cancel') {
      const message = error.response?.data?.message || '删除失败'
      ElMessage.error(message)
    }
  }
}

// 批量删除
const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 个班级吗？此操作不可恢复！`,
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const ids = selectedRows.value.map(item => item.id)
    await batchDeleteClasses(ids)
    ElMessage.success('批量删除成功')
    selectedRows.value = []
    loadClassList()
  } catch (error: any) {
    if (error !== 'cancel') {
      const message = error.response?.data?.message || '批量删除失败'
      ElMessage.error(message)
    }
  }
}

// 导出
const handleExport = async () => {
  try {
    await exportExcel(
      () => exportClasses({
        ...searchForm,
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      }),
      `班级列表_${new Date().toLocaleDateString()}.xlsx`
    )
    ElMessage.success('导出成功')
  } catch (error: any) {
    const message = error.response?.data?.message || '导出失败'
    ElMessage.error(message)
  }
}

// 全选/取消全选
const handleSelectAll = (e: Event) => {
  const checked = (e.target as HTMLInputElement).checked
  if (checked) {
    selectedRows.value = [...classList.value]
  } else {
    selectedRows.value = []
  }
}

// 选中/取消选中行
const handleSelectRow = (row: Class) => {
  const index = selectedRows.value.findIndex(s => s.id === row.id)
  if (index === -1) {
    selectedRows.value.push(row)
  } else {
    selectedRows.value.splice(index, 1)
  }
}

// 分页大小变化
const handleSizeChange = () => {
  pagination.pageNum = 1
  loadClassList()
}

// 页码变化
const handlePageChange = (page: number) => {
  pagination.pageNum = page
  loadClassList()
}

// 详情弹窗关闭
const handleDetailClose = () => {
  detailDialogVisible.value = false
  currentClassId.value = null
}

// 编辑弹窗关闭
const handleEditClose = () => {
  editDialogVisible.value = false
  currentClassId.value = null
}

// 设置班主任
const handleSetTeacher = async (row: Class) => {
  currentClassId.value = row.id
  currentClassName.value = row.className
  teacherForm.teacherId = row.teacherId || null
  await loadTeacherList()
  teacherDialogVisible.value = true
}

// 教师弹窗关闭
const handleTeacherClose = () => {
  teacherDialogVisible.value = false
  currentClassId.value = null
  currentClassName.value = ''
  teacherForm.teacherId = null
}

// 提交设置班主任
const handleTeacherSubmit = async () => {
  if (!currentClassId.value) return

  submitting.value = true
  try {
    await assignTeacher(currentClassId.value, teacherForm.teacherId)
    ElMessage.success('设置班主任成功')
    teacherDialogVisible.value = false
    loadClassList()
  } catch (error: any) {
    const message = error.response?.data?.message || '设置班主任失败'
    ElMessage.error(message)
  } finally {
    submitting.value = false
  }
}

// 编辑成功
const handleEditSuccess = () => {
  editDialogVisible.value = false
  currentClassId.value = null
  loadClassList()
}

// 打开宿舍管理弹窗
const handleManageDormitory = (row: Class) => {
  currentClassId.value = row.id
  currentClassName.value = row.className
  currentDepartmentId.value = row.departmentId || null
  dormitoryDialogVisible.value = true
}

// 加载班级已分配宿舍
const loadClassDormitories = async () => {
  if (!currentClassId.value) return
  dormitoryLoading.value = true
  try {
    const response = await getClassDormitories(currentClassId.value)
    classDormitories.value = response || []
  } catch (error: any) {
    console.error('加载班级宿舍失败:', error)
    classDormitories.value = []
  } finally {
    dormitoryLoading.value = false
  }
}

// 加载所有可用宿舍
const loadAllDormitories = async () => {
  try {
    const response = await getDormitoryList()
    allDormitories.value = response || []
  } catch (error: any) {
    console.error('加载宿舍列表失败:', error)
    allDormitories.value = []
  }
}

// 添加宿舍到班级
const handleAddDormitory = async () => {
  if (!currentClassId.value || !dormitoryForm.dormitoryId || !dormitoryForm.allocatedBeds) return

  dormitorySubmitting.value = true
  try {
    await addDormitory(currentClassId.value, dormitoryForm.dormitoryId, dormitoryForm.allocatedBeds)
    ElMessage.success('添加宿舍成功')
    dormitoryForm.dormitoryId = null
    dormitoryForm.allocatedBeds = 1
    await loadClassDormitories()
  } catch (error: any) {
    const message = error.response?.data?.message || '添加宿舍失败'
    ElMessage.error(message)
  } finally {
    dormitorySubmitting.value = false
  }
}

// 移除班级宿舍
const handleRemoveDormitory = async (dorm: ClassDormitoryInfo) => {
  if (!currentClassId.value) return

  try {
    await ElMessageBox.confirm(
      `确定要移除宿舍 "${dorm.buildingName} - ${dorm.dormitoryNo}" 吗？`,
      '移除确认',
      {
        confirmButtonText: '确定移除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await removeDormitory(currentClassId.value, dorm.dormitoryId)
    ElMessage.success('移除宿舍成功')
    await loadClassDormitories()
  } catch (error: any) {
    if (error !== 'cancel') {
      const message = error.response?.data?.message || '移除宿舍失败'
      ElMessage.error(message)
    }
  }
}

// 关闭宿舍管理弹窗
const handleDormitoryClose = () => {
  dormitoryDialogVisible.value = false
  currentClassId.value = null
  currentClassName.value = ''
  classDormitories.value = []
  dormitoryForm.dormitoryId = null
  dormitoryForm.allocatedBeds = 1
}

// 初始化
onMounted(() => {
  loadClassList()
  loadDepartmentList()
  loadGradeList()
})
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: all 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .relative,
.modal-leave-to .relative {
  transform: scale(0.95);
}
</style>
