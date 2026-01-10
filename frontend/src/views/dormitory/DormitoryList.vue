<template>
  <div class="p-6 bg-gray-50 min-h-full">
    <!-- 页面标题 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">房间管理</h1>
      <p class="mt-1 text-sm text-gray-500">管理宿舍房间信息、床位分配和入住统计</p>
    </div>

    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard
        title="正常使用"
        :value="stats.active"
        :icon="CheckCircle"
        subtitle="可用房间"
        color="emerald"
      />
      <StatCard
        title="维修中"
        :value="stats.maintenance"
        :icon="Wrench"
        subtitle="维护状态"
        color="orange"
      />
      <StatCard
        title="已停用"
        :value="stats.disabled"
        :icon="XCircle"
        subtitle="停用房间"
        color="rose"
      />
      <StatCard
        title="平均入住率"
        :value="`${stats.occupancyRate}%`"
        :icon="Percent"
        subtitle="整体入住"
        :progress="stats.occupancyRate"
        color="purple"
      />
    </div>

    <!-- 搜索和操作区域 -->
    <div class="mb-4 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-end gap-3">
        <div class="w-40">
          <label class="mb-1 block text-xs font-medium text-gray-600">宿舍楼</label>
          <select
            v-model="searchForm.buildingName"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option :value="null">全部楼栋</option>
            <option v-for="item in buildingList" :key="item.id" :value="item.buildingName || item.buildingNo">
              {{ item.buildingName || item.buildingNo }}
            </option>
          </select>
        </div>
        <div class="w-32">
          <label class="mb-1 block text-xs font-medium text-gray-600">房间号</label>
          <input
            v-model="searchForm.dormitoryNo"
            type="text"
            placeholder="房间号"
            class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
        <div class="w-24">
          <label class="mb-1 block text-xs font-medium text-gray-600">楼层</label>
          <select
            v-model="searchForm.floorNumber"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option :value="null">全部</option>
            <option v-for="i in 10" :key="i" :value="i">{{ i }}楼</option>
          </select>
        </div>
        <div class="w-24">
          <label class="mb-1 block text-xs font-medium text-gray-600">容量</label>
          <select
            v-model="searchForm.roomType"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option :value="null">全部</option>
            <option :value="4">4人间</option>
            <option :value="6">6人间</option>
            <option :value="8">8人间</option>
          </select>
        </div>
        <div class="w-24">
          <label class="mb-1 block text-xs font-medium text-gray-600">状态</label>
          <select
            v-model="searchForm.status"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option :value="null">全部</option>
            <option :value="1">正常</option>
            <option :value="2">维修</option>
            <option :value="3">停用</option>
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
          v-if="hasPermission('student:dormitory:export')"
          class="h-9 rounded-md border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
          @click="handleExport"
        >
          <Download class="mr-1.5 inline-block h-4 w-4" />
          导出
        </button>
        <button
          v-if="hasPermission('student:dormitory:add')"
          class="h-9 rounded-md bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          @click="handleAdd"
        >
          <Plus class="mr-1.5 inline-block h-4 w-4" />
          新增房间
        </button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium text-gray-900">房间列表</span>
          <span class="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-600">{{ total }} 间</span>
        </div>
        <button
          v-if="hasPermission('student:dormitory:delete') && selectedRows.length > 0"
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
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">楼栋</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">房间号</th>
              <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">楼层</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">用途</th>
              <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">床位</th>
              <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">入住率</th>
              <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">性别</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">状态</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr
              v-for="row in dormitoryList"
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
                  <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-teal-100">
                    <Building2 class="h-4 w-4 text-teal-600" />
                  </div>
                  <div>
                    <span class="text-sm font-medium text-gray-900">{{ row.buildingName || '-' }}</span>
                    <p v-if="row.buildingNo" class="text-xs text-gray-500">{{ row.buildingNo }}号楼</p>
                  </div>
                </div>
              </td>
              <td class="px-4 py-3">
                <span class="font-mono text-sm font-medium text-gray-900">{{ row.dormitoryNo || '-' }}</span>
              </td>
              <td class="px-4 py-3 text-center">
                <span class="rounded bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-700">
                  {{ row.floorNumber }}F
                </span>
              </td>
              <td class="px-4 py-3">
                <span
                  class="inline-flex items-center rounded px-2 py-0.5 text-xs font-medium"
                  :class="getRoomUsageTypeClass(row.roomUsageType)"
                >
                  {{ getRoomUsageTypeName(row.roomUsageType) }}
                </span>
              </td>
              <td class="px-4 py-3 text-center">
                <span v-if="row.roomUsageType === 1 || row.roomUsageType === 2" class="text-sm font-medium text-gray-900">
                  {{ row.occupiedBeds || 0 }}/{{ row.bedCapacity || 0 }}
                </span>
                <span v-else class="text-gray-400">-</span>
              </td>
              <td class="px-4 py-3">
                <div v-if="row.roomUsageType === 1 || row.roomUsageType === 2" class="flex items-center justify-center gap-2">
                  <div class="h-2 w-16 overflow-hidden rounded-full bg-gray-200">
                    <div
                      class="h-full rounded-full transition-all"
                      :class="getOccupancyClass(row.occupiedBeds, row.bedCapacity)"
                      :style="{ width: `${row.bedCapacity > 0 ? (row.occupiedBeds / row.bedCapacity) * 100 : 0}%` }"
                    />
                  </div>
                  <span class="text-xs text-gray-500">{{ row.bedCapacity > 0 ? Math.round((row.occupiedBeds / row.bedCapacity) * 100) : 0 }}%</span>
                </div>
                <span v-else class="flex justify-center text-gray-400">-</span>
              </td>
              <td class="px-4 py-3 text-center">
                <span
                  class="inline-flex items-center rounded px-2 py-0.5 text-xs font-medium"
                  :class="getGenderTypeClass(row.genderType)"
                >
                  {{ getGenderTypeName(row.genderType) }}
                </span>
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
                    v-if="hasPermission('student:dormitory:edit')"
                    class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-blue-600"
                    title="编辑"
                    @click="handleEdit(row)"
                  >
                    <Pencil class="h-4 w-4" />
                  </button>
                  <button
                    v-if="hasPermission('student:dormitory:assign')"
                    class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-green-600"
                    title="分配床位"
                    @click="handleAssign(row)"
                  >
                    <BedDouble class="h-4 w-4" />
                  </button>
                  <button
                    v-if="hasPermission('student:dormitory:delete')"
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
        <div v-if="dormitoryList.length === 0" class="flex flex-col items-center justify-center py-16">
          <Building2 class="h-12 w-12 text-gray-300" />
          <p class="mt-3 text-sm text-gray-500">暂无房间数据</p>
          <button
            v-if="hasPermission('student:dormitory:add')"
            class="mt-4 rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            @click="handleAdd"
          >
            创建房间
          </button>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="dormitoryList.length > 0" class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
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

    <!-- 宿舍详情弹窗 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="detailDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div class="fixed inset-0 bg-black/40" @click="handleDetailClose" />
          <div class="relative z-10 w-full max-w-3xl max-h-[90vh] overflow-hidden rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b px-6 py-4">
              <h2 class="text-lg font-semibold text-gray-900">房间详情</h2>
              <button class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600" @click="handleDetailClose">
                <X class="h-5 w-5" />
              </button>
            </div>
            <div class="overflow-y-auto" style="max-height: calc(90vh - 65px)">
              <DormitoryDetail :dormitory-id="currentDormitoryId" @close="handleDetailClose" />
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 编辑宿舍弹窗 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="editDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div class="fixed inset-0 bg-black/40" @click="handleEditClose" />
          <div class="relative z-10 w-full max-h-[90vh] overflow-hidden rounded-xl bg-white shadow-xl" :class="editMode === 'add' ? 'max-w-4xl' : 'max-w-2xl'">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h2 class="flex items-center gap-2 text-lg font-semibold text-gray-900">
                <DoorOpen class="h-5 w-5 text-teal-600" />
                {{ editMode === 'add' ? '新增房间' : '编辑房间' }}
              </h2>
              <button class="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600" @click="handleEditClose">
                <X class="h-5 w-5" />
              </button>
            </div>
            <div class="overflow-y-auto" style="max-height: calc(90vh - 65px)">
              <DormitoryForm :mode="editMode" :dormitory-id="currentDormitoryId" @success="handleEditSuccess" @close="handleEditClose" />
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 床位分配弹窗 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="assignDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div class="fixed inset-0 bg-black/40" @click="handleAssignClose" />
          <div class="relative z-10 w-full max-w-4xl max-h-[90vh] overflow-hidden rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b px-6 py-4">
              <h2 class="text-lg font-semibold text-gray-900">床位分配</h2>
              <button class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600" @click="handleAssignClose">
                <X class="h-5 w-5" />
              </button>
            </div>
            <div class="overflow-y-auto" style="max-height: calc(90vh - 65px)">
              <BedAssignment :dormitory-id="currentDormitoryId" @success="handleAssignSuccess" @close="handleAssignClose" />
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Download,
  Trash2,
  Eye,
  Pencil,
  X,
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  Building2,
  BedDouble,
  CheckCircle,
  XCircle,
  Wrench,
  Percent,
  DoorOpen
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { getDormitories, deleteDormitory, deleteDormitories, getAllEnabledBuildings } from '@/api/v2/dormitory'
// 导出功能暂无V2端点
import { exportDormitories } from '@/api/dormitory'
import type { Building } from '@/types/v2/dormitory'
import { exportExcel } from '@/utils/export'
import DormitoryDetail from '@/components/dormitory/DormitoryDetail.vue'
import DormitoryForm from '@/components/dormitory/DormitoryForm.vue'
import BedAssignment from '@/components/dormitory/BedAssignment.vue'
import type { Dormitory, DormitoryQueryParams } from '@/types/v2/dormitory'

const authStore = useAuthStore()

// 权限检查
const hasPermission = (permission: string) => authStore.hasPermission(permission)

// 加载状态
const loading = ref(false)

// 统计数据
const stats = reactive({
  active: 0,
  maintenance: 0,
  disabled: 0,
  occupancyRate: 0
})

// 搜索表单
const searchForm = reactive<DormitoryQueryParams>({
  buildingName: null,
  dormitoryNo: '',
  floorNumber: null,
  roomType: null,
  status: null
})

// 分页参数
const pagination = reactive({
  pageNum: 1,
  pageSize: 20
})

// 数据
const dormitoryList = ref<Dormitory[]>([])
const total = ref(0)
const selectedRows = ref<Dormitory[]>([])
const buildingList = ref<Building[]>([])

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
  return dormitoryList.value.length > 0 && selectedRows.value.length === dormitoryList.value.length
})

// 弹窗控制
const detailDialogVisible = ref(false)
const editDialogVisible = ref(false)
const assignDialogVisible = ref(false)
const editMode = ref<'add' | 'edit'>('add')
const currentDormitoryId = ref<number | null>(null)

// 获取入住率样式
const getOccupancyClass = (occupied: number, total: number) => {
  if (!total) return 'bg-gray-300'
  const rate = (occupied / total) * 100
  if (rate === 100) return 'bg-red-500'
  if (rate >= 80) return 'bg-amber-500'
  return 'bg-green-500'
}

// 获取状态样式类
const getStatusClass = (status: number) => {
  const classMap: Record<number, string> = {
    1: 'bg-green-50 text-green-700',
    2: 'bg-amber-50 text-amber-700',
    3: 'bg-red-50 text-red-700'
  }
  return classMap[status] || 'bg-gray-100 text-gray-700'
}

// 获取状态文本
const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    1: '正常',
    2: '维修',
    3: '停用'
  }
  return statusMap[status] || '未知'
}

// 获取房间用途类型名称
const getRoomUsageTypeName = (type: number) => {
  const typeMap: Record<number, string> = {
    1: '学生宿舍',
    2: '教职工宿舍',
    3: '配电室',
    4: '卫生间',
    5: '杂物间',
    6: '其他'
  }
  return typeMap[type] || '未知'
}

// 获取房间用途类型样式
const getRoomUsageTypeClass = (type: number) => {
  const classMap: Record<number, string> = {
    1: 'bg-teal-50 text-teal-700',
    2: 'bg-blue-50 text-blue-700',
    3: 'bg-yellow-50 text-yellow-700',
    4: 'bg-cyan-50 text-cyan-700',
    5: 'bg-gray-100 text-gray-700',
    6: 'bg-gray-100 text-gray-700'
  }
  return classMap[type] || 'bg-gray-100 text-gray-700'
}

// 获取性别类型名称
const getGenderTypeName = (type: number) => {
  const typeMap: Record<number, string> = {
    1: '男',
    2: '女',
    3: '混合'
  }
  return typeMap[type] || '-'
}

// 获取性别类型样式
const getGenderTypeClass = (type: number) => {
  const classMap: Record<number, string> = {
    1: 'bg-blue-50 text-blue-700',
    2: 'bg-pink-50 text-pink-700',
    3: 'bg-purple-50 text-purple-700'
  }
  return classMap[type] || 'bg-gray-100 text-gray-700'
}

// 加载宿舍列表
const loadDormitoryList = async () => {
  loading.value = true
  try {
    const response = await getDormitories({
      ...searchForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })

    if (response) {
      dormitoryList.value = response.records || []
      total.value = Number(response.total) || 0

      // 计算统计数据
      stats.active = dormitoryList.value.filter(d => d.status === 1).length
      stats.maintenance = dormitoryList.value.filter(d => d.status === 2).length
      stats.disabled = dormitoryList.value.filter(d => d.status === 3).length

      // 计算入住率
      const rooms = dormitoryList.value.filter(d => (d.roomUsageType === 1 || d.roomUsageType === 2) && d.bedCapacity > 0)
      if (rooms.length > 0) {
        const totalBeds = rooms.reduce((sum, r) => sum + (r.bedCapacity || 0), 0)
        const occupiedBeds = rooms.reduce((sum, r) => sum + (r.occupiedBeds || 0), 0)
        stats.occupancyRate = totalBeds > 0 ? Math.round((occupiedBeds / totalBeds) * 100) : 0
      } else {
        stats.occupancyRate = 0
      }
    } else {
      dormitoryList.value = []
      total.value = 0
    }
  } catch (error: any) {
    const message = error.response?.data?.message || '加载宿舍列表失败，请稍后重试'
    ElMessage.error(message)
    dormitoryList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 加载宿舍楼列表
const loadBuildingList = async () => {
  try {
    buildingList.value = await getAllEnabledBuildings(2)
  } catch (error: any) {
    console.error('加载宿舍楼列表失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  loadDormitoryList()
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, {
    buildingName: null,
    dormitoryNo: '',
    floorNumber: null,
    roomType: null,
    status: null
  })
  pagination.pageNum = 1
  loadDormitoryList()
}

// 新增
const handleAdd = () => {
  editMode.value = 'add'
  currentDormitoryId.value = null
  editDialogVisible.value = true
}

// 查看详情
const handleView = (row: Dormitory) => {
  currentDormitoryId.value = row.id
  detailDialogVisible.value = true
}

// 编辑
const handleEdit = (row: Dormitory) => {
  editMode.value = 'edit'
  currentDormitoryId.value = row.id
  editDialogVisible.value = true
}

// 分配床位
const handleAssign = (row: Dormitory) => {
  currentDormitoryId.value = row.id
  assignDialogVisible.value = true
}

// 删除
const handleDelete = async (row: Dormitory) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除房间 "${row.dormitoryNo}" 吗？此操作不可恢复！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    try {
      await deleteDormitory(row.id, false)
      ElMessage.success('删除成功')
      loadDormitoryList()
    } catch (deleteError: any) {
      const errorMsg = deleteError.response?.data?.message || ''
      if (errorMsg.includes('名学生')) {
        await ElMessageBox.confirm(
          errorMsg,
          '强制删除确认',
          {
            confirmButtonText: '强制删除',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        await deleteDormitory(row.id, true)
        ElMessage.success('删除成功，已清空该房间内学生的宿舍信息')
        loadDormitoryList()
      } else {
        ElMessage.error(errorMsg || '删除失败')
      }
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Delete error:', error)
    }
  }
}

// 批量删除
const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 个宿舍吗？此操作不可恢复！`,
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const ids = selectedRows.value.map(item => item.id)
    await deleteDormitories(ids)
    ElMessage.success('批量删除成功')
    selectedRows.value = []
    loadDormitoryList()
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
      () => exportDormitories({
        ...searchForm,
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      }),
      `宿舍列表_${new Date().toLocaleDateString()}.xlsx`
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
    selectedRows.value = [...dormitoryList.value]
  } else {
    selectedRows.value = []
  }
}

// 选中/取消选中行
const handleSelectRow = (row: Dormitory) => {
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
  loadDormitoryList()
}

// 页码变化
const handlePageChange = (page: number) => {
  pagination.pageNum = page
  loadDormitoryList()
}

// 详情弹窗关闭
const handleDetailClose = () => {
  detailDialogVisible.value = false
  currentDormitoryId.value = null
}

// 编辑弹窗关闭
const handleEditClose = () => {
  editDialogVisible.value = false
  currentDormitoryId.value = null
}

// 分配弹窗关闭
const handleAssignClose = () => {
  assignDialogVisible.value = false
  currentDormitoryId.value = null
}

// 编辑成功
const handleEditSuccess = () => {
  editDialogVisible.value = false
  currentDormitoryId.value = null
  loadDormitoryList()
}

// 分配成功
const handleAssignSuccess = () => {
  assignDialogVisible.value = false
  currentDormitoryId.value = null
  loadDormitoryList()
}

// 初始化
onMounted(() => {
  loadDormitoryList()
  loadBuildingList()
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
