<template>
  <div class="p-6 bg-gray-50 min-h-full">
    <!-- 页面标题 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">学生管理</h1>
      <p class="mt-1 text-sm text-gray-500">管理学生基本信息、班级分配和宿舍安排</p>
    </div>

    <!-- 统计卡片 - 设计系统 -->
    <div class="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
      <StatCard
        title="在校学生"
        :value="stats.inSchool"
        :icon="UserCheck"
        subtitle="正常在校"
        color="emerald"
        :trend="8.5"
      />
      <StatCard
        title="休学中"
        :value="stats.onLeave"
        :icon="Clock"
        subtitle="暂时离校"
        color="orange"
        :trend="-2.1"
      />
      <StatCard
        title="已毕业"
        :value="stats.graduated"
        :icon="GraduationCap"
        subtitle="完成学业"
        color="blue"
        :trend="15.3"
      />
      <StatCard
        title="本月新增"
        :value="stats.newThisMonth"
        :icon="UserPlus"
        subtitle="新入学"
        color="purple"
        :trend="25.0"
      />
    </div>

    <!-- 搜索和操作区域 -->
    <div class="mb-4 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-end gap-3">
        <div class="w-40">
          <label class="mb-1 block text-xs font-medium text-gray-600">学号</label>
          <input
            v-model="searchForm.studentNo"
            type="text"
            placeholder="请输入学号"
            class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
        <div class="w-40">
          <label class="mb-1 block text-xs font-medium text-gray-600">姓名</label>
          <input
            v-model="searchForm.realName"
            type="text"
            placeholder="请输入姓名"
            class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
        <div class="w-40">
          <label class="mb-1 block text-xs font-medium text-gray-600">年级</label>
          <select
            v-model="searchForm.gradeId"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            @change="handleGradeChange"
          >
            <option :value="null">全部年级</option>
            <option v-for="grade in gradeList" :key="grade.id" :value="grade.id">
              {{ grade.gradeName }}
            </option>
          </select>
        </div>
        <div class="w-40">
          <label class="mb-1 block text-xs font-medium text-gray-600">班级</label>
          <select
            v-model="searchForm.classId"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option :value="null">全部班级</option>
            <option v-for="item in filteredClassList" :key="item.id" :value="item.id">
              {{ item.className }}
            </option>
          </select>
        </div>
        <div class="w-28">
          <label class="mb-1 block text-xs font-medium text-gray-600">状态</label>
          <select
            v-model="searchForm.studentStatus"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option :value="null">全部</option>
            <option :value="1">在校</option>
            <option :value="2">休学</option>
            <option :value="3">毕业</option>
            <option :value="4">退学</option>
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
          v-if="hasPermission('student:info:import')"
          class="h-9 rounded-md border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
          @click="importDialogVisible = true"
        >
          <Upload class="mr-1.5 inline-block h-4 w-4" />
          导入
        </button>
        <button
          v-if="hasPermission('student:info:export')"
          class="h-9 rounded-md border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
          @click="handleExport"
        >
          <Download class="mr-1.5 inline-block h-4 w-4" />
          导出
        </button>
        <button
          v-if="hasPermission('student:info:add')"
          class="h-9 rounded-md bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          @click="handleAdd"
        >
          <Plus class="mr-1.5 inline-block h-4 w-4" />
          新增学生
        </button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium text-gray-900">学生列表</span>
          <span class="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-600">{{ total }} 条</span>
        </div>
        <button
          v-if="hasPermission('student:info:delete') && selectedRows.length > 0"
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
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">学号</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">姓名</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">性别</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">年级</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">班级</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">专业</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">联系电话</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">楼号</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">房间号</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">床位号</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">状态</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">入学时间</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr
              v-for="row in studentList"
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
                <span class="font-mono text-sm text-gray-900">{{ row.studentNo }}</span>
              </td>
              <td class="px-4 py-3">
                <span class="text-sm font-medium text-gray-900">{{ row.realName }}</span>
              </td>
              <td class="px-4 py-3">
                <span
                  :class="[
                    'inline-flex items-center rounded px-2 py-0.5 text-xs font-medium',
                    row.gender === 1 ? 'bg-blue-50 text-blue-700' : 'bg-pink-50 text-pink-700'
                  ]"
                >
                  {{ row.gender === 1 ? '男' : '女' }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-600">
                {{ row.gradeName || '-' }}
              </td>
              <td class="px-4 py-3 text-sm text-gray-600">
                {{ row.className || '-' }}
              </td>
              <td class="px-4 py-3 text-sm text-gray-600">
                {{ row.majorName || '-' }}
              </td>
              <td class="px-4 py-3 text-sm text-gray-600">
                {{ row.phone || '-' }}
              </td>
              <td class="px-4 py-3 text-sm text-gray-600">
                {{ row.buildingNo || '-' }}
              </td>
              <td class="px-4 py-3 text-sm text-gray-600">
                {{ row.roomNo || '-' }}
              </td>
              <td class="px-4 py-3 text-sm text-gray-600">
                {{ row.bedNumber || '-' }}
              </td>
              <td class="px-4 py-3">
                <span
                  class="inline-flex items-center rounded px-2 py-0.5 text-xs font-medium"
                  :class="getStatusClass(row.studentStatus)"
                >
                  {{ getStatusText(row.studentStatus) }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-600">
                {{ formatDate(row.admissionDate) }}
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
                    v-if="hasPermission('student:info:edit')"
                    class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-blue-600"
                    title="编辑"
                    @click="handleEdit(row)"
                  >
                    <Pencil class="h-4 w-4" />
                  </button>
                  <button
                    v-if="hasPermission('student:info:delete')"
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
        <div v-if="studentList.length === 0" class="flex flex-col items-center justify-center py-16">
          <Users class="h-12 w-12 text-gray-300" />
          <p class="mt-3 text-sm text-gray-500">暂无学生数据</p>
          <button
            v-if="hasPermission('student:info:add')"
            class="mt-4 rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            @click="handleAdd"
          >
            添加学生
          </button>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="studentList.length > 0" class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
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

    <!-- 学生详情弹窗 -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="detailDialogVisible"
          class="fixed inset-0 z-50 flex items-center justify-center p-4"
        >
          <div class="fixed inset-0 bg-black/40" @click="handleDetailClose" />
          <div class="relative z-10 w-full max-w-3xl max-h-[90vh] overflow-hidden rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b px-6 py-4">
              <h2 class="text-lg font-semibold text-gray-900">学生详情</h2>
              <button class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600" @click="handleDetailClose">
                <X class="h-5 w-5" />
              </button>
            </div>
            <div class="overflow-y-auto" style="max-height: calc(90vh - 65px)">
              <StudentDetail
                :student-id="currentStudentId"
                @close="handleDetailClose"
              />
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 编辑学生弹窗 -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="editDialogVisible"
          class="fixed inset-0 z-50 flex items-center justify-center p-4"
        >
          <div class="fixed inset-0 bg-black/40" @click="handleEditClose" />
          <div class="relative z-10 w-full max-w-4xl max-h-[90vh] overflow-hidden rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b px-6 py-4">
              <h2 class="text-lg font-semibold text-gray-900">{{ editMode === 'add' ? '新增学生' : '编辑学生' }}</h2>
              <button class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600" @click="handleEditClose">
                <X class="h-5 w-5" />
              </button>
            </div>
            <div class="overflow-y-auto" style="max-height: calc(90vh - 65px)">
              <StudentForm
                :mode="editMode"
                :student-id="currentStudentId"
                @success="handleEditSuccess"
                @close="handleEditClose"
              />
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 导入学生弹窗 -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="importDialogVisible"
          class="fixed inset-0 z-50 flex items-center justify-center p-4"
        >
          <div class="fixed inset-0 bg-black/40" @click="importDialogVisible = false" />
          <div class="relative z-10 w-full max-w-3xl max-h-[90vh] overflow-hidden rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b px-6 py-4">
              <h2 class="text-lg font-semibold text-gray-900">导入学生</h2>
              <button class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600" @click="importDialogVisible = false">
                <X class="h-5 w-5" />
              </button>
            </div>
            <div class="overflow-y-auto" style="max-height: calc(90vh - 65px)">
              <StudentImport
                @success="handleImportSuccess"
                @close="importDialogVisible = false"
              />
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 导出学生弹窗 -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="exportDialogVisible"
          class="fixed inset-0 z-50 flex items-center justify-center p-4"
        >
          <div class="fixed inset-0 bg-black/40" @click="exportDialogVisible = false" />
          <div class="relative z-10 w-full max-w-4xl max-h-[90vh] overflow-hidden rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b px-6 py-4">
              <h2 class="text-lg font-semibold text-gray-900">导出学生</h2>
              <button class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600" @click="exportDialogVisible = false">
                <X class="h-5 w-5" />
              </button>
            </div>
            <div class="overflow-y-auto" style="max-height: calc(90vh - 65px)">
              <StudentExport
                :selected-students="selectedRows"
                :filtered-students="studentList"
                :total-filtered="total"
                :search-params="searchForm"
                @success="handleExportSuccess"
                @close="exportDialogVisible = false"
              />
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Download,
  Upload,
  Trash2,
  Eye,
  Pencil,
  X,
  Users,
  UserPlus,
  UserCheck,
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  GraduationCap,
  Clock
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { formatDate } from '@/utils/date'
import { getStudentPage, deleteStudent, deleteStudents } from '@/api/student'
import { getClassList } from '@/api/class'
import { getAllGrades } from '@/api/grade'
import type { Grade } from '@/api/grade'
import StudentDetail from '@/components/student/StudentDetail.vue'
import StudentForm from '@/components/student/StudentForm.vue'
import { StatCard } from '@/components/design-system'
import StudentImport from '@/components/student/StudentImport.vue'
import StudentExport from '@/components/student/StudentExport.vue'
import type { Student } from '@/types/student'

const route = useRoute()
const authStore = useAuthStore()

// 权限检查
const hasPermission = (permission: string) => authStore.hasPermission(permission)

// 加载状态
const loading = ref(false)

// 统计数据
const stats = reactive({
  inSchool: 0,
  onLeave: 0,
  graduated: 0,
  newThisMonth: 0
})

// 搜索表单
const searchForm = reactive<any>({
  studentNo: '',
  realName: '',
  gradeId: null,
  classId: null,
  studentStatus: null
})

// 分页参数
const pagination = reactive({
  pageNum: 1,
  pageSize: 20
})

// 数据
const studentList = ref<Student[]>([])
const total = ref(0)
const selectedRows = ref<Student[]>([])
const classList = ref<any[]>([])
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
  return studentList.value.length > 0 && selectedRows.value.length === studentList.value.length
})

// 根据选择的年级过滤班级列表
const filteredClassList = computed(() => {
  if (!searchForm.gradeId) {
    return classList.value
  }
  return classList.value.filter(item => item.gradeId === searchForm.gradeId)
})

// 弹窗控制
const detailDialogVisible = ref(false)
const editDialogVisible = ref(false)
const importDialogVisible = ref(false)
const exportDialogVisible = ref(false)
const editMode = ref<'add' | 'edit'>('add')
const currentStudentId = ref<number | null>(null)

// 获取状态样式类
const getStatusClass = (status: number) => {
  const classMap: Record<number, string> = {
    1: 'bg-green-50 text-green-700',
    2: 'bg-amber-50 text-amber-700',
    3: 'bg-gray-100 text-gray-700',
    4: 'bg-red-50 text-red-700'
  }
  return classMap[status] || 'bg-gray-100 text-gray-700'
}

// 获取状态文本
const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    1: '在校',
    2: '休学',
    3: '毕业',
    4: '退学'
  }
  return statusMap[status] || '未知'
}

// 加载学生列表
const loadStudentList = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }

    if (searchForm.studentNo) params.studentNo = searchForm.studentNo
    if (searchForm.realName) params.realName = searchForm.realName
    if (searchForm.gradeId) params.gradeId = searchForm.gradeId
    if (searchForm.classId) params.classId = searchForm.classId
    if (searchForm.studentStatus) params.studentStatus = searchForm.studentStatus

    const response = await getStudentPage(params)
    studentList.value = response.records || []
    total.value = response.total || 0

    // 模拟统计数据
    stats.inSchool = Math.round(total.value * 0.85)
    stats.onLeave = Math.round(total.value * 0.03)
    stats.graduated = Math.round(total.value * 0.1)
    stats.newThisMonth = Math.round(total.value * 0.02)
  } catch (error: any) {
    const message = error.response?.data?.message || '加载学生列表失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

// 加载班级列表
const loadClassList = async () => {
  try {
    // 传入足够大的 pageSize 以获取所有班级
    const response = await getClassList({ pageNum: 1, pageSize: 1000 })
    classList.value = response.records || []
  } catch (error: any) {
    console.error('加载班级列表失败:', error)
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

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  loadStudentList()
}

// 年级改变时,清空班级选择
const handleGradeChange = () => {
  searchForm.classId = null
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, {
    studentNo: '',
    realName: '',
    gradeId: null,
    classId: null,
    studentStatus: null
  })
  pagination.pageNum = 1
  loadStudentList()
}

// 新增
const handleAdd = () => {
  editMode.value = 'add'
  currentStudentId.value = null
  editDialogVisible.value = true
}

// 查看详情
const handleView = (row: Student) => {
  currentStudentId.value = row.id
  detailDialogVisible.value = true
}

// 编辑
const handleEdit = (row: Student) => {
  editMode.value = 'edit'
  currentStudentId.value = row.id
  editDialogVisible.value = true
}

// 删除
const handleDelete = async (row: Student) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除学生 "${row.realName}" 吗？此操作不可恢复！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteStudent(row.id)
    ElMessage.success('删除成功')
    loadStudentList()
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
      `确定要删除选中的 ${selectedRows.value.length} 个学生吗？此操作不可恢复！`,
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const ids = selectedRows.value.map(item => item.id)
    await deleteStudents(ids)
    ElMessage.success('批量删除成功')
    selectedRows.value = []
    loadStudentList()
  } catch (error: any) {
    if (error !== 'cancel') {
      const message = error.response?.data?.message || '批量删除失败'
      ElMessage.error(message)
    }
  }
}

// 导出
const handleExport = () => {
  exportDialogVisible.value = true
}

// 导出成功
const handleExportSuccess = () => {
  exportDialogVisible.value = false
}

// 全选/取消全选
const handleSelectAll = (e: Event) => {
  const checked = (e.target as HTMLInputElement).checked
  if (checked) {
    selectedRows.value = [...studentList.value]
  } else {
    selectedRows.value = []
  }
}

// 选中/取消选中行
const handleSelectRow = (row: Student) => {
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
  loadStudentList()
}

// 页码变化
const handlePageChange = (page: number) => {
  pagination.pageNum = page
  loadStudentList()
}

// 详情弹窗关闭
const handleDetailClose = () => {
  detailDialogVisible.value = false
  currentStudentId.value = null
}

// 编辑弹窗关闭
const handleEditClose = () => {
  editDialogVisible.value = false
  currentStudentId.value = null
}

// 编辑成功
const handleEditSuccess = () => {
  editDialogVisible.value = false
  currentStudentId.value = null
  loadStudentList()
}

// 导入成功
const handleImportSuccess = () => {
  loadStudentList()
}

// 初始化
onMounted(async () => {
  await Promise.all([loadClassList(), loadGradeList()])

  const classIdFromQuery = route.query.classId
  const classNameFromQuery = route.query.className

  if (classIdFromQuery) {
    // 不使用 Number() 转换，避免雪花ID精度丢失
    searchForm.classId = classIdFromQuery as any
    if (classNameFromQuery) {
      ElMessage.info(`正在显示班级"${classNameFromQuery}"的学生`)
    }
  }

  loadStudentList()
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
