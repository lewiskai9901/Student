<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 页面标题 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">宿舍楼管理</h1>
      <p class="mt-1 text-sm text-gray-500">管理宿舍楼类型、管理员分配及相关信息</p>
    </div>

    <!-- 提示信息 -->
    <div class="mb-6 flex items-start gap-3 rounded-lg border border-blue-200 bg-blue-50 p-4">
      <Info class="h-5 w-5 flex-shrink-0 text-blue-500" />
      <div>
        <p class="text-sm font-medium text-blue-800">温馨提示</p>
        <p class="mt-1 text-sm text-blue-600">宿舍楼的创建和删除请在【楼宇管理】中进行，此处仅管理宿舍楼类型、管理员等信息</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard
        title="男生宿舍"
        :value="stats.maleBuildings"
        :icon="UserCircle"
        subtitle="男生楼宇"
        color="blue"
      />
      <StatCard
        title="女生宿舍"
        :value="stats.femaleBuildings"
        :icon="UserCircle2"
        subtitle="女生楼宇"
        color="pink"
      />
      <StatCard
        title="总床位数"
        :value="stats.totalBeds"
        :icon="BedDouble"
        subtitle="可用床位"
        color="emerald"
      />
      <StatCard
        title="入住率"
        :value="`${stats.occupancyRate}%`"
        :icon="BarChart3"
        subtitle="整体入住"
        color="orange"
      />
    </div>

    <!-- 搜索区域 -->
    <div class="mb-6 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-600">楼宇名称</label>
          <input
            v-model="queryForm.buildingName"
            type="text"
            placeholder="请输入楼宇名称"
            class="h-9 w-48 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            @keyup.enter="handleQuery"
          />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-600">宿舍类型</label>
          <select
            v-model="queryForm.dormitoryType"
            class="h-9 w-36 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          >
            <option value="">全部类型</option>
            <option v-for="item in dormitoryTypeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </option>
          </select>
        </div>
        <div class="flex gap-2">
          <button
            @click="handleQuery"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          >
            <Search class="h-4 w-4" />
            查询
          </button>
          <button
            @click="handleReset"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            <RotateCcw class="h-4 w-4" />
            重置
          </button>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <table class="w-full">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50">
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-600">楼号</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-600">楼宇名称</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-600">位置</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-600">宿舍类型</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-600">房间数</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-600">床位数</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-600">管理员</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-600">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-if="loading">
            <td colspan="8" class="px-4 py-12 text-center text-gray-400">
              <Loader2 class="mx-auto h-6 w-6 animate-spin" />
              <p class="mt-2">加载中...</p>
            </td>
          </tr>
          <tr v-else-if="tableData.length === 0">
            <td colspan="8" class="px-4 py-12 text-center text-gray-400">
              <Building2 class="mx-auto h-10 w-10" />
              <p class="mt-2">暂无数据</p>
            </td>
          </tr>
          <tr v-for="row in tableData" :key="row.id" class="hover:bg-gray-50">
            <td class="px-4 py-3">
              <span class="inline-flex h-7 w-7 items-center justify-center rounded bg-blue-100 text-sm font-medium text-blue-700">
                {{ row.buildingNo }}
              </span>
            </td>
            <td class="px-4 py-3">
              <div class="flex items-center gap-2">
                <Building class="h-4 w-4 text-gray-400" />
                <span class="font-medium text-gray-900">{{ row.buildingName }}</span>
              </div>
            </td>
            <td class="px-4 py-3 text-sm text-gray-600">
              {{ row.location || '-' }}
            </td>
            <td class="px-4 py-3">
              <span
                v-if="row.dormitoryType"
                :class="[
                  'inline-flex items-center gap-1 rounded px-2 py-0.5 text-xs font-medium',
                  getDormitoryTypeClass(row.dormitoryType)
                ]"
              >
                {{ row.dormitoryTypeName }}
              </span>
              <span v-else class="text-sm text-gray-400">未设置</span>
            </td>
            <td class="px-4 py-3 text-center">
              <div class="flex items-center justify-center gap-1 text-sm">
                <span class="font-medium text-gray-900">{{ row.totalRooms || 0 }}</span>
                <span class="text-gray-400">/</span>
                <span :class="row.occupiedRooms > 0 ? 'text-red-500' : 'text-green-500'">
                  {{ row.occupiedRooms || 0 }}
                </span>
              </div>
              <div class="mx-auto mt-1 h-1 w-16 overflow-hidden rounded-full bg-gray-200">
                <div
                  class="h-full rounded-full"
                  :class="getRoomOccupancyColor(row.occupiedRooms, row.totalRooms)"
                  :style="{ width: `${getOccupancyRate(row.occupiedRooms, row.totalRooms)}%` }"
                ></div>
              </div>
            </td>
            <td class="px-4 py-3 text-center">
              <div class="flex items-center justify-center gap-1 text-sm">
                <span class="font-medium text-gray-900">{{ row.totalBeds || 0 }}</span>
                <span class="text-gray-400">/</span>
                <span :class="row.occupiedBeds > 0 ? 'text-red-500' : 'text-green-500'">
                  {{ row.occupiedBeds || 0 }}
                </span>
              </div>
              <div class="mx-auto mt-1 h-1 w-16 overflow-hidden rounded-full bg-gray-200">
                <div
                  class="h-full rounded-full"
                  :class="getBedOccupancyColor(row.occupiedBeds, row.totalBeds)"
                  :style="{ width: `${getOccupancyRate(row.occupiedBeds, row.totalBeds)}%` }"
                ></div>
              </div>
            </td>
            <td class="px-4 py-3">
              <div v-if="row.managerNames && row.managerNames.length > 0" class="flex flex-wrap gap-1">
                <span
                  v-for="(name, idx) in row.managerNames.slice(0, 2)"
                  :key="idx"
                  class="inline-flex items-center gap-1 rounded bg-green-50 px-2 py-0.5 text-xs text-green-700"
                >
                  <UserCheck class="h-3 w-3" />
                  {{ name }}
                </span>
                <span
                  v-if="row.managerNames.length > 2"
                  class="inline-flex items-center rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-500"
                >
                  +{{ row.managerNames.length - 2 }}
                </span>
              </div>
              <span v-else class="text-sm text-gray-400">未分配</span>
            </td>
            <td class="px-4 py-3">
              <div class="flex items-center justify-center gap-1">
                <button
                  @click="handleEdit(row)"
                  class="inline-flex items-center gap-1 rounded px-2 py-1 text-xs text-blue-600 hover:bg-blue-50"
                >
                  <Settings class="h-3.5 w-3.5" />
                  设置
                </button>
                <button
                  @click="handleManageManagers(row)"
                  class="inline-flex items-center gap-1 rounded px-2 py-1 text-xs text-green-600 hover:bg-green-50"
                >
                  <UserCog class="h-3.5 w-3.5" />
                  管理员
                </button>
                <button
                  @click="handleAddRoom(row)"
                  class="inline-flex items-center gap-1 rounded px-2 py-1 text-xs text-amber-600 hover:bg-amber-50"
                >
                  <DoorOpen class="h-3.5 w-3.5" />
                  新增房间
                </button>
                <button
                  @click="handleDepartmentAssignment(row)"
                  class="inline-flex items-center gap-1 rounded px-2 py-1 text-xs text-purple-600 hover:bg-purple-50"
                >
                  <GraduationCap class="h-3.5 w-3.5" />
                  院系分配
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <div class="text-sm text-gray-500">
          共 <span class="font-medium text-gray-900">{{ total }}</span> 条记录
        </div>
        <div class="flex items-center gap-2">
          <select
            v-model="queryForm.pageSize"
            @change="handleQuery"
            class="pagination-select"
          >
            <option :value="10">10条/页</option>
            <option :value="20">20条/页</option>
            <option :value="50">50条/页</option>
          </select>
          <div class="flex items-center gap-1">
            <button
              @click="handlePageChange(1)"
              :disabled="queryForm.pageNum === 1"
              class="flex h-8 w-8 items-center justify-center rounded border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <ChevronsLeft class="h-4 w-4" />
            </button>
            <button
              @click="handlePageChange(queryForm.pageNum - 1)"
              :disabled="queryForm.pageNum === 1"
              class="flex h-8 w-8 items-center justify-center rounded border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <ChevronLeft class="h-4 w-4" />
            </button>
            <span class="px-3 text-sm text-gray-600">
              {{ queryForm.pageNum }} / {{ totalPages }}
            </span>
            <button
              @click="handlePageChange(queryForm.pageNum + 1)"
              :disabled="queryForm.pageNum >= totalPages"
              class="flex h-8 w-8 items-center justify-center rounded border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <ChevronRight class="h-4 w-4" />
            </button>
            <button
              @click="handlePageChange(totalPages)"
              :disabled="queryForm.pageNum >= totalPages"
              class="flex h-8 w-8 items-center justify-center rounded border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <ChevronsRight class="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑宿舍楼对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="editDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="editDialogVisible = false"></div>
          <div class="relative w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
            <div class="mb-6 flex items-center justify-between">
              <h3 class="text-lg font-semibold text-gray-900">{{ currentRow?.buildingName }} - 宿舍楼设置</h3>
              <button @click="editDialogVisible = false" class="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
                <X class="h-5 w-5" />
              </button>
            </div>

            <form @submit.prevent="handleEditSubmit" class="space-y-4">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  宿舍楼类型 <span class="text-red-500">*</span>
                </label>
                <select
                  v-model="editForm.dormitoryType"
                  required
                  class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                >
                  <option value="">请选择宿舍楼类型</option>
                  <option v-for="item in dormitoryTypeOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
                <p class="mt-1 text-xs text-gray-500">设置后，只能创建对应性别类型的宿舍房间</p>
              </div>

              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">管理规定</label>
                <textarea
                  v-model="editForm.managementRules"
                  rows="2"
                  placeholder="如：禁止大声喧哗、保持宿舍整洁等"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                ></textarea>
              </div>

              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">访问时间</label>
                <input
                  v-model="editForm.visitingHours"
                  type="text"
                  placeholder="如：周末 10:00-18:00"
                  class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                />
              </div>

              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">设施配置</label>
                <textarea
                  v-model="editForm.facilities"
                  rows="2"
                  placeholder="如：空调、热水器、独立卫浴"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                ></textarea>
              </div>

              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">备注说明</label>
                <textarea
                  v-model="editForm.description"
                  rows="2"
                  placeholder="请输入备注说明"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                ></textarea>
              </div>

              <div class="flex justify-end gap-3 border-t border-gray-200 pt-4">
                <button
                  type="button"
                  @click="editDialogVisible = false"
                  class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  取消
                </button>
                <button
                  type="submit"
                  :disabled="editSubmitLoading"
                  class="inline-flex h-9 items-center gap-2 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  <Loader2 v-if="editSubmitLoading" class="h-4 w-4 animate-spin" />
                  保存
                </button>
              </div>
            </form>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 管理员管理对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="managerDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="managerDialogVisible = false"></div>
          <div class="relative w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
            <div class="mb-6 flex items-center justify-between">
              <h3 class="text-lg font-semibold text-gray-900">{{ currentRow?.buildingName }} - 管理员管理</h3>
              <button @click="managerDialogVisible = false" class="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
                <X class="h-5 w-5" />
              </button>
            </div>

            <div class="space-y-4">
              <!-- 当前管理员 -->
              <div>
                <label class="mb-2 block text-sm font-medium text-gray-700">当前管理员</label>
                <div v-if="currentManagers.length > 0" class="rounded-lg border border-gray-200 bg-gray-50 p-3">
                  <div class="flex flex-wrap gap-2">
                    <span
                      v-for="manager in currentManagers"
                      :key="manager.userId"
                      class="inline-flex items-center gap-2 rounded-full border border-gray-200 bg-white py-1 pl-3 pr-1 text-sm text-gray-700"
                    >
                      <UserCheck class="h-4 w-4 text-green-500" />
                      {{ manager.realName }}
                      <span v-if="manager.departmentName" class="text-gray-400">({{ manager.departmentName }})</span>
                      <button
                        @click="handleRemoveManager(manager.userId)"
                        class="ml-1 rounded-full p-1 text-gray-400 hover:bg-red-100 hover:text-red-500"
                      >
                        <X class="h-3.5 w-3.5" />
                      </button>
                    </span>
                  </div>
                </div>
                <div v-else class="rounded-lg border border-dashed border-gray-300 p-4 text-center text-sm text-gray-400">
                  暂无管理员
                </div>
              </div>

              <!-- 添加管理员 -->
              <div>
                <label class="mb-2 block text-sm font-medium text-gray-700">添加管理员</label>
                <div class="max-h-60 overflow-y-auto rounded-lg border border-gray-200">
                  <div
                    v-for="user in availableManagers"
                    :key="user.id"
                    class="flex items-center justify-between border-b border-gray-100 px-4 py-3 last:border-b-0 hover:bg-gray-50"
                  >
                    <div class="flex items-center gap-3">
                      <div class="flex h-8 w-8 items-center justify-center rounded-full bg-blue-100 text-sm font-medium text-blue-600">
                        {{ user.realName?.charAt(0) || 'U' }}
                      </div>
                      <div>
                        <p class="text-sm font-medium text-gray-900">{{ user.realName }}</p>
                        <p class="text-xs text-gray-500">{{ user.username }}</p>
                      </div>
                    </div>
                    <label class="relative inline-flex cursor-pointer items-center">
                      <input
                        type="checkbox"
                        :checked="selectedManagerIds.includes(user.id) || isManagerSelected(user.id)"
                        :disabled="isManagerSelected(user.id)"
                        @change="toggleManager(user.id)"
                        class="peer sr-only"
                      />
                      <div class="peer h-5 w-9 rounded-full bg-gray-200 after:absolute after:left-[2px] after:top-[2px] after:h-4 after:w-4 after:rounded-full after:bg-white after:transition-all peer-checked:bg-blue-600 peer-checked:after:translate-x-full peer-disabled:cursor-not-allowed peer-disabled:opacity-50"></div>
                    </label>
                  </div>
                  <div v-if="availableManagers.length === 0" class="px-4 py-8 text-center text-sm text-gray-400">
                    暂无可分配的管理员
                  </div>
                </div>
                <p class="mt-2 text-xs text-gray-500">最多分配5个管理员</p>
              </div>

              <div class="flex justify-end gap-3 border-t border-gray-200 pt-4">
                <button
                  @click="managerDialogVisible = false"
                  class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  取消
                </button>
                <button
                  @click="handleManagerSubmit"
                  :disabled="managerSubmitLoading"
                  class="inline-flex h-9 items-center gap-2 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  <Loader2 v-if="managerSubmitLoading" class="h-4 w-4 animate-spin" />
                  保存
                </button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 新增房间对话框 -->
    <RoomFormDialog
      v-if="roomDialogVisible"
      v-model:visible="roomDialogVisible"
      :building-id="currentRow!.buildingId"
      :building-name="currentRow!.buildingName"
      :building-no="currentRow!.buildingNo"
      :dormitory-type="currentRow!.dormitoryType!"
      :dormitory-type-name="currentRow!.dormitoryTypeName || ''"
      @success="handleRoomSuccess"
      @close="roomDialogVisible = false"
    />

    <!-- 院系分配对话框 -->
    <DepartmentAssignmentDialog
      v-if="departmentDialogVisible"
      v-model:visible="departmentDialogVisible"
      :building-id="currentRow!.buildingId"
      :building-name="currentRow!.buildingName"
      @success="handleDepartmentAssignmentSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Building2,
  Building,
  Users,
  BedDouble,
  BarChart3,
  Search,
  RotateCcw,
  Info,
  UserCheck,
  UserCog,
  Settings,
  DoorOpen,
  Loader2,
  X,
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  GraduationCap
} from 'lucide-vue-next'
import {
  getDormitoryBuildingList,
  updateDormitoryBuilding,
  assignManagers,
  removeManager,
  getUsersWithDormitoryPermission
} from '@/api/dormitoryBuilding'
import type {
  BuildingDormitory,
  BuildingDormitoryQueryParams,
  BuildingDormitoryFormData,
  ManagerInfo
} from '@/types/buildingDormitory'
import { dormitoryTypeOptions } from '@/types/buildingDormitory'
import RoomFormDialog from '@/components/dormitory/RoomFormDialog.vue'
import DepartmentAssignmentDialog from '@/components/dormitory/DepartmentAssignmentDialog.vue'

// 查询表单
const queryForm = reactive<BuildingDormitoryQueryParams>({
  pageNum: 1,
  pageSize: 10
})

// 表格数据
const tableData = ref<BuildingDormitory[]>([])
const total = ref(0)
const loading = ref(false)

// 统计数据
const stats = reactive({
  maleBuildings: 0,
  femaleBuildings: 0,
  totalBeds: 0,
  occupancyRate: 0
})

// 计算总页数
const totalPages = computed(() => Math.ceil(total.value / queryForm.pageSize) || 1)

// 加载列表
const loadList = async () => {
  loading.value = true
  try {
    const res = await getDormitoryBuildingList(queryForm)
    tableData.value = res.records
    total.value = res.total

    // 计算统计数据
    let maleCount = 0
    let femaleCount = 0
    let beds = 0
    let occupiedBeds = 0

    res.records.forEach((item: BuildingDormitory) => {
      if (item.dormitoryType === 1) maleCount++
      else if (item.dormitoryType === 2) femaleCount++
      beds += item.totalBeds || 0
      occupiedBeds += item.occupiedBeds || 0
    })

    stats.maleBuildings = maleCount
    stats.femaleBuildings = femaleCount
    stats.totalBeds = beds
    stats.occupancyRate = beds > 0 ? Math.round((occupiedBeds / beds) * 100) : 0
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

// 查询
const handleQuery = () => {
  queryForm.pageNum = 1
  loadList()
}

// 重置
const handleReset = () => {
  queryForm.buildingName = undefined
  queryForm.dormitoryType = undefined
  queryForm.pageNum = 1
  loadList()
}

// 分页
const handlePageChange = (page: number) => {
  if (page < 1 || page > totalPages.value) return
  queryForm.pageNum = page
  loadList()
}

// 获取宿舍类型样式
const getDormitoryTypeClass = (type: number) => {
  const classes: Record<number, string> = {
    1: 'bg-blue-100 text-blue-700',
    2: 'bg-pink-100 text-pink-700',
    3: 'bg-purple-100 text-purple-700',
    4: 'bg-amber-100 text-amber-700',
    5: 'bg-gray-100 text-gray-700'
  }
  return classes[type] || 'bg-gray-100 text-gray-700'
}

// 获取房间入住率颜色
const getRoomOccupancyColor = (occupied: number, total: number) => {
  const rate = total > 0 ? (occupied / total) * 100 : 0
  if (rate >= 90) return 'bg-red-500'
  if (rate >= 70) return 'bg-amber-500'
  return 'bg-green-500'
}

// 获取床位入住率颜色
const getBedOccupancyColor = (occupied: number, total: number) => {
  const rate = total > 0 ? (occupied / total) * 100 : 0
  if (rate >= 90) return 'bg-red-500'
  if (rate >= 70) return 'bg-amber-500'
  return 'bg-green-500'
}

// 计算入住率
const getOccupancyRate = (occupied: number, total: number) => {
  if (!total || total === 0) return 0
  return Math.min(100, Math.round((occupied / total) * 100))
}

// 编辑相关
const editDialogVisible = ref(false)
const editSubmitLoading = ref(false)
const currentRow = ref<BuildingDormitory>()

const editForm = reactive<BuildingDormitoryFormData>({
  dormitoryType: undefined,
  managementRules: '',
  visitingHours: '',
  facilities: '',
  description: ''
})

// 打开编辑对话框
const handleEdit = (row: BuildingDormitory) => {
  currentRow.value = row
  editForm.dormitoryType = row.dormitoryType || undefined
  editForm.managementRules = row.managementRules || ''
  editForm.visitingHours = row.visitingHours || ''
  editForm.facilities = row.facilities || ''
  editForm.description = row.description || ''
  editDialogVisible.value = true
}

// 提交编辑
const handleEditSubmit = async () => {
  if (!currentRow.value) return
  if (!editForm.dormitoryType) {
    ElMessage.warning('请选择宿舍楼类型')
    return
  }

  editSubmitLoading.value = true
  try {
    const submitData = {
      ...editForm,
      buildingId: currentRow.value.buildingId
    }
    await updateDormitoryBuilding(currentRow.value.id, submitData)
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    loadList()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    editSubmitLoading.value = false
  }
}

// 管理员管理相关
const managerDialogVisible = ref(false)
const managerSubmitLoading = ref(false)
const currentManagers = ref<ManagerInfo[]>([])
const selectedManagerIds = ref<number[]>([])
const availableManagers = ref<any[]>([])

// 打开管理员管理对话框
const handleManageManagers = async (row: BuildingDormitory) => {
  currentRow.value = row
  const managers: ManagerInfo[] = []
  if (row.managerIds && row.managerNames) {
    for (let i = 0; i < row.managerIds.length; i++) {
      managers.push({
        id: 0,
        userId: row.managerIds[i],
        username: '',
        realName: row.managerNames[i]
      })
    }
  }
  currentManagers.value = managers
  selectedManagerIds.value = []

  try {
    availableManagers.value = await getUsersWithDormitoryPermission()
  } catch (error: any) {
    ElMessage.error(error.message || '加载管理员列表失败')
  }

  managerDialogVisible.value = true
}

// 判断管理员是否已被选择
const isManagerSelected = (userId: number) => {
  return currentManagers.value.some(m => m.userId === userId)
}

// 切换管理员选择
const toggleManager = (userId: number) => {
  const index = selectedManagerIds.value.indexOf(userId)
  if (index > -1) {
    selectedManagerIds.value.splice(index, 1)
  } else {
    selectedManagerIds.value.push(userId)
  }
}

// 移除管理员
const handleRemoveManager = async (userId: number) => {
  if (!currentRow.value) return

  try {
    await ElMessageBox.confirm('确定要移除此管理员吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await removeManager(currentRow.value.buildingId, userId)
    ElMessage.success('移除成功')
    currentManagers.value = currentManagers.value.filter(m => m.userId !== userId)
    loadList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '移除失败')
    }
  }
}

// 提交管理员分配
const handleManagerSubmit = async () => {
  if (!currentRow.value) return

  const totalManagers = currentManagers.value.length + selectedManagerIds.value.length
  if (totalManagers > 5) {
    ElMessage.warning('最多只能分配5个管理员')
    return
  }

  if (selectedManagerIds.value.length === 0) {
    ElMessage.warning('请选择要添加的管理员')
    return
  }

  managerSubmitLoading.value = true
  try {
    const allManagerIds = [
      ...currentManagers.value.map(m => m.userId),
      ...selectedManagerIds.value
    ]

    await assignManagers(currentRow.value.buildingId, allManagerIds)
    ElMessage.success('分配成功')
    managerDialogVisible.value = false
    loadList()
  } catch (error: any) {
    ElMessage.error(error.message || '分配失败')
  } finally {
    managerSubmitLoading.value = false
  }
}

// 新增房间相关
const roomDialogVisible = ref(false)

// 院系分配相关
const departmentDialogVisible = ref(false)

// 打开院系分配对话框
const handleDepartmentAssignment = (row: BuildingDormitory) => {
  if (!row.dormitoryType) {
    ElMessage.warning('请先设置宿舍楼类型后再进行院系分配')
    return
  }
  currentRow.value = row
  departmentDialogVisible.value = true
}

// 院系分配成功后的回调
const handleDepartmentAssignmentSuccess = () => {
  loadList()
}

// 打开新增房间对话框
const handleAddRoom = (row: BuildingDormitory) => {
  if (!row.dormitoryType) {
    ElMessage.warning('请先设置宿舍楼类型后再新增房间')
    return
  }
  currentRow.value = row
  roomDialogVisible.value = true
}

// 新增房间成功后
const handleRoomSuccess = () => {
  ElMessage.success('新增房间成功')
  loadList()
}

// 初始化加载数据
onMounted(() => {
  loadList()
})
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
