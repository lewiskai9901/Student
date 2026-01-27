<template>
  <div class="space-y-4">
    <!-- 标题和操作 -->
    <div class="flex items-center justify-between">
      <div>
        <div class="flex items-center gap-2">
          <h2 class="text-xl font-bold text-gray-900">{{ space.spaceName }}</h2>
          <span
            class="px-2 py-0.5 text-xs rounded"
            :class="getRoomTypeClass(space.roomType)"
          >
            {{ getRoomTypeName(space.roomType) }}
          </span>
          <span
            v-if="space.status !== 1"
            class="px-2 py-0.5 text-xs rounded"
            :class="getStatusClass(space.status)"
          >
            {{ space.statusText }}
          </span>
        </div>
        <p class="text-sm text-gray-500 mt-1">
          {{ space.campusName }} · {{ space.parentBuildingNo ? space.parentBuildingNo + '号楼' : space.buildingName }} · {{ space.floorNumber }}F
          <span v-if="space.roomNo" class="text-gray-600 font-medium"> · {{ space.roomNo }}室</span>
        </p>
      </div>
      <div class="flex items-center gap-2">
        <el-button size="small" @click="handleEdit">
          <el-icon class="mr-1"><Edit /></el-icon>
          编辑
        </el-button>
        <el-dropdown @command="handleStatusCommand">
          <el-button size="small">
            状态
            <el-icon class="ml-1"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="1" :disabled="space.status === 1">启用</el-dropdown-item>
              <el-dropdown-item command="0" :disabled="space.status === 0">禁用</el-dropdown-item>
              <el-dropdown-item command="2" :disabled="space.status === 2">维护</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button size="small" type="danger" @click="handleDelete">
          <el-icon class="mr-1"><Delete /></el-icon>
          删除
        </el-button>
      </div>
    </div>

    <!-- 信息卡片 -->
    <div class="grid grid-cols-3 gap-4">
      <!-- 基本信息 - 两列布局 -->
      <div class="bg-white rounded-lg shadow-sm p-4">
        <h3 class="font-medium text-gray-900 mb-3 text-sm flex items-center gap-2">
          <span class="w-1 h-4 bg-blue-500 rounded"></span>
          基本信息
        </h3>
        <div class="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
          <div>
            <span class="text-gray-400 text-xs">房间名称</span>
            <div class="text-gray-900">{{ space.spaceName || '-' }}</div>
          </div>
          <div>
            <span class="text-gray-400 text-xs">房间号</span>
            <div class="text-gray-900 font-semibold">{{ space.roomNo ? space.roomNo + '室' : '-' }}</div>
          </div>
          <div>
            <span class="text-gray-400 text-xs">楼栋名称</span>
            <div class="text-gray-900">{{ space.buildingName || '-' }}</div>
          </div>
          <div>
            <span class="text-gray-400 text-xs">楼栋号</span>
            <div class="text-gray-900 font-semibold">{{ space.parentBuildingNo ? space.parentBuildingNo + '号' : '-' }}</div>
          </div>
          <div>
            <span class="text-gray-400 text-xs">楼层名称</span>
            <div class="text-gray-900">{{ space.floorName || '-' }}</div>
          </div>
          <div>
            <span class="text-gray-400 text-xs">楼层号</span>
            <div class="text-gray-900 font-semibold">{{ space.floorNumber ? space.floorNumber + 'F' : '-' }}</div>
          </div>
          <div>
            <span class="text-gray-400 text-xs">房间编码</span>
            <div class="text-gray-900 font-mono text-xs">{{ space.spaceCode }}</div>
          </div>
          <div>
            <span class="text-gray-400 text-xs">容量</span>
            <div class="text-gray-900">{{ space.capacity || '-' }} 人</div>
          </div>
          <div>
            <span class="text-gray-400 text-xs">已入住</span>
            <div class="text-gray-900">{{ space.currentOccupancy || 0 }} 人</div>
          </div>
          <div v-if="space.genderType !== undefined && space.genderType !== 0">
            <span class="text-gray-400 text-xs">性别限制</span>
            <div :class="space.genderType === 1 ? 'text-blue-600' : 'text-pink-600'" class="font-medium">
              {{ space.genderType === 1 ? '男生' : '女生' }}
            </div>
          </div>
          <div>
            <span class="text-gray-400 text-xs">剩余床位</span>
            <div :class="(space.availableCapacity || 0) > 0 ? 'text-green-600' : 'text-red-600'" class="font-medium">
              {{ space.availableCapacity || 0 }} 个
            </div>
          </div>
        </div>
        <div v-if="space.description" class="mt-3 pt-3 border-t border-gray-100">
          <span class="text-gray-400 text-xs">备注</span>
          <p class="text-gray-700 mt-1 text-sm">{{ space.description }}</p>
        </div>
      </div>

      <!-- 归属信息 -->
      <div class="bg-white rounded-lg shadow-sm p-4">
        <h3 class="font-medium text-gray-900 mb-3 text-sm flex items-center gap-2">
          <span class="w-1 h-4 bg-green-500 rounded"></span>
          归属信息
        </h3>
        <div class="space-y-2 text-sm">
          <div class="flex justify-between">
            <span class="text-gray-500">所属部门</span>
            <span class="text-gray-900">{{ space.orgUnitName || '-' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500">归属班级</span>
            <span class="text-gray-900">{{ space.className || '-' }}</span>
          </div>
          <div v-if="space.classTeacherName" class="pt-2 border-t border-gray-100">
            <div class="flex items-center gap-2 mb-2">
              <el-icon class="text-gray-400"><User /></el-icon>
              <span class="text-gray-500">班主任</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-900 font-medium">{{ space.classTeacherName }}</span>
              <a
                v-if="space.classTeacherPhone"
                :href="'tel:' + space.classTeacherPhone"
                class="text-blue-600 hover:text-blue-800"
              >
                <el-icon class="mr-1"><Phone /></el-icon>
                {{ space.classTeacherPhone }}
              </a>
            </div>
          </div>
          <div v-else-if="!space.className" class="text-center text-gray-400 py-4">
            暂未分配
          </div>
        </div>
      </div>

      <!-- 入住率 -->
      <div class="bg-white rounded-lg shadow-sm p-4">
        <h3 class="font-medium text-gray-900 mb-3 text-sm flex items-center gap-2">
          <span class="w-1 h-4 bg-amber-500 rounded"></span>
          入住情况
        </h3>
        <div class="flex items-center justify-center h-32">
          <div class="text-center">
            <div class="text-5xl font-bold" :class="occupancyRateColor">
              {{ occupancyRate }}%
            </div>
            <div class="text-sm text-gray-500 mt-2">入住率</div>
            <div class="mt-3 w-32 h-2 bg-gray-200 rounded-full overflow-hidden mx-auto">
              <div
                class="h-full transition-all"
                :class="occupancyRateBarClass"
                :style="{ width: occupancyRate + '%' }"
              ></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 入住者列表 -->
    <div class="bg-white rounded-lg shadow-sm">
      <div class="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
        <h3 class="font-medium text-gray-900">入住者 ({{ occupants.length }})</h3>
        <el-button
          type="primary"
          size="small"
          :disabled="!canCheckIn"
          @click="showCheckInDialog = true"
        >
          <el-icon class="mr-1"><Plus /></el-icon>
          办理入住
        </el-button>
      </div>
      <div class="p-4">
        <div v-if="loadingOccupants" class="text-center py-8 text-gray-400">
          <el-icon class="animate-spin text-2xl"><Loading /></el-icon>
          <p class="mt-2">加载中...</p>
        </div>
        <div v-else-if="!occupants.length" class="text-center text-gray-400 py-8">
          暂无入住者
        </div>
        <div v-else class="space-y-2">
          <div
            v-for="occupant in occupants"
            :key="occupant.id"
            class="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
          >
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center">
                <span class="text-blue-600 font-medium">
                  {{ occupant.occupantName?.charAt(0) || '?' }}
                </span>
              </div>
              <div>
                <div class="font-medium text-gray-900">
                  {{ occupant.occupantName || '未知' }}
                  <span v-if="occupant.positionNo" class="text-xs text-gray-500 ml-1">
                    ({{ occupant.positionNo }}号位)
                  </span>
                </div>
                <div class="text-xs text-gray-500">
                  {{ getOccupantTypeName(occupant.occupantType) }}
                  <span v-if="occupant.className" class="ml-1">· {{ occupant.className }}</span>
                  <span v-if="occupant.checkInDate" class="ml-1">
                    · 入住于 {{ formatDate(occupant.checkInDate) }}
                  </span>
                </div>
              </div>
            </div>
            <el-button
              type="danger"
              size="small"
              text
              @click="handleCheckOut(occupant)"
            >
              退出
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 入住历史 -->
    <div class="bg-white rounded-lg shadow-sm">
      <div class="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
        <h3 class="font-medium text-gray-900">入住历史</h3>
        <el-button size="small" text @click="loadHistory">
          <el-icon class="mr-1"><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
      <div class="p-4">
        <div v-if="!history.length" class="text-center text-gray-400 py-4">
          暂无历史记录
        </div>
        <el-table v-else :data="history" size="small">
          <el-table-column prop="occupantName" label="姓名" width="100" />
          <el-table-column prop="occupantType" label="类型" width="80">
            <template #default="{ row }">
              {{ getOccupantTypeName(row.occupantType) }}
            </template>
          </el-table-column>
          <el-table-column prop="positionNo" label="位置" width="60" />
          <el-table-column prop="checkInDate" label="入住日期" width="110" />
          <el-table-column prop="checkOutDate" label="退出日期" width="110">
            <template #default="{ row }">
              {{ row.checkOutDate || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="statusText" label="状态" width="80">
            <template #default="{ row }">
              <span
                class="px-2 py-0.5 text-xs rounded"
                :class="row.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'"
              >
                {{ row.statusText }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <SpaceFormDialog
      v-model:visible="editDialogVisible"
      mode="edit"
      :space-type="'ROOM'"
      :edit-data="space"
      @success="handleRefresh"
    />

    <!-- 入住对话框 -->
    <CheckInDialog
      v-model:visible="showCheckInDialog"
      :space-id="space.id"
      :space-name="space.spaceName"
      :capacity="space.capacity || 0"
      :current-occupancy="space.currentOccupancy || 0"
      @success="handleCheckInSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Delete, ArrowDown, Plus, Refresh, Loading, User, Phone } from '@element-plus/icons-vue'
import { deleteSpace, changeSpaceStatus, getOccupants, getOccupantHistory, checkOut } from '@/api/space'
import type { SpaceDTO, SpaceOccupantDTO, SpaceStatus, RoomType, OccupantType } from '@/types/space'
import { getRoomTypeName, getStatusClass, getOccupantTypeName } from '@/types/space'
import SpaceFormDialog from './SpaceFormDialog.vue'
import CheckInDialog from './CheckInDialog.vue'

const props = defineProps<{
  space: SpaceDTO
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

// 入住率
const occupancyRate = computed(() => {
  if (!props.space.capacity) return 0
  return Math.round((props.space.currentOccupancy || 0) / props.space.capacity * 100)
})

const occupancyRateColor = computed(() => {
  if (occupancyRate.value >= 90) return 'text-red-600'
  if (occupancyRate.value >= 70) return 'text-amber-600'
  return 'text-green-600'
})

const occupancyRateBarClass = computed(() => {
  if (occupancyRate.value >= 90) return 'bg-red-500'
  if (occupancyRate.value >= 70) return 'bg-amber-500'
  return 'bg-green-500'
})

const canCheckIn = computed(() => {
  return props.space.status === 1 &&
    props.space.capacity &&
    (props.space.currentOccupancy || 0) < props.space.capacity
})

// 显示所属楼栋（优先显示楼号，其次显示楼栋名称）
const displayBuildingNo = computed(() => {
  if (props.space.parentBuildingNo) {
    return props.space.parentBuildingNo + '号楼'
  }
  if (props.space.buildingName) {
    return props.space.buildingName
  }
  return '-'
})

// 辅助函数
function getRoomTypeClass(type?: string): string {
  const classes: Record<string, string> = {
    DORMITORY: 'bg-teal-100 text-teal-700',
    CLASSROOM: 'bg-blue-100 text-blue-700',
    LAB: 'bg-purple-100 text-purple-700',
    COMPUTER_ROOM: 'bg-cyan-100 text-cyan-700',
    TRAINING: 'bg-indigo-100 text-indigo-700',
    MEETING: 'bg-amber-100 text-amber-700',
    OFFICE: 'bg-green-100 text-green-700',
    STORAGE: 'bg-gray-100 text-gray-700',
    OTHER: 'bg-gray-100 text-gray-600'
  }
  return classes[type || ''] || 'bg-gray-100 text-gray-600'
}

function formatDate(date?: string): string {
  if (!date) return '-'
  return date.split('T')[0]
}

// 入住者
const occupants = ref<SpaceOccupantDTO[]>([])
const loadingOccupants = ref(false)

async function loadOccupants() {
  loadingOccupants.value = true
  try {
    const res = await getOccupants(props.space.id)
    occupants.value = res || []
  } catch (error) {
    console.error('加载入住者失败:', error)
  } finally {
    loadingOccupants.value = false
  }
}

// 入住历史
const history = ref<SpaceOccupantDTO[]>([])

async function loadHistory() {
  try {
    const res = await getOccupantHistory(props.space.id)
    history.value = res || []
  } catch (error) {
    console.error('加载历史失败:', error)
  }
}

// 监听space变化重新加载
watch(
  () => props.space.id,
  () => {
    loadOccupants()
    loadHistory()
  },
  { immediate: true }
)

// 编辑
const editDialogVisible = ref(false)

function handleEdit() {
  editDialogVisible.value = true
}

// 状态变更
async function handleStatusCommand(command: string) {
  const status = parseInt(command) as SpaceStatus
  const statusNames: Record<number, string> = { 0: '禁用', 1: '启用', 2: '维护' }

  try {
    await changeSpaceStatus(props.space.id, status)
    ElMessage.success(`已${statusNames[status]}`)
    emit('refresh')
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 删除
async function handleDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除房间"${props.space.spaceName}"吗？`,
      '删除确认',
      { type: 'warning' }
    )

    await deleteSpace(props.space.id)
    ElMessage.success('删除成功')
    emit('refresh')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 入住
const showCheckInDialog = ref(false)

function handleCheckInSuccess() {
  loadOccupants()
  emit('refresh')
}

// 退出
async function handleCheckOut(occupant: SpaceOccupantDTO) {
  try {
    await ElMessageBox.confirm(
      `确定要为"${occupant.occupantName}"办理退出吗？`,
      '退出确认',
      { type: 'warning' }
    )

    await checkOut(props.space.id, occupant.id)
    ElMessage.success('退出成功')
    loadOccupants()
    emit('refresh')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

function handleRefresh() {
  emit('refresh')
}
</script>
