<template>
  <div class="space-y-4">
    <!-- 标题和操作 -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-xl font-bold text-gray-900">
          {{ space.floorNumber }}F · {{ space.spaceName }}
        </h2>
        <p class="text-sm text-gray-500 mt-1">
          {{ space.campusName }} · {{ space.buildingName }}
        </p>
      </div>
      <div class="flex items-center gap-2">
        <el-button size="small" @click="handleEdit">
          <el-icon class="mr-1"><Edit /></el-icon>
          编辑
        </el-button>
        <el-button size="small" type="danger" @click="handleDelete">
          <el-icon class="mr-1"><Delete /></el-icon>
          删除
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-4 gap-4">
      <div class="bg-cyan-50 rounded-lg p-4 text-center">
        <div class="text-3xl font-bold text-cyan-600">{{ roomCount }}</div>
        <div class="text-sm text-cyan-500 mt-1">房间数</div>
      </div>
      <div class="bg-green-50 rounded-lg p-4 text-center">
        <div class="text-3xl font-bold text-green-600">{{ totalCapacity }}</div>
        <div class="text-sm text-green-500 mt-1">总容量</div>
      </div>
      <div class="bg-blue-50 rounded-lg p-4 text-center">
        <div class="text-3xl font-bold text-blue-600">{{ totalOccupancy }}</div>
        <div class="text-sm text-blue-500 mt-1">已入住</div>
      </div>
      <div class="bg-amber-50 rounded-lg p-4 text-center">
        <div class="text-3xl font-bold" :class="occupancyRateColor">{{ occupancyRate }}%</div>
        <div class="text-sm text-amber-500 mt-1">入住率</div>
      </div>
    </div>

    <!-- 基本信息卡片 -->
    <div class="bg-white rounded-lg shadow-sm p-4">
      <h3 class="font-medium text-gray-900 mb-3 text-sm flex items-center gap-2">
        <span class="w-1 h-4 bg-cyan-500 rounded"></span>
        基本信息
      </h3>
      <div class="grid grid-cols-4 gap-x-8 gap-y-3 text-sm">
        <div>
          <span class="text-gray-400 text-xs">楼层名称</span>
          <div class="text-gray-900">{{ space.spaceName || '-' }}</div>
        </div>
        <div>
          <span class="text-gray-400 text-xs">楼层号</span>
          <div class="text-gray-900 font-semibold">{{ space.floorNumber ? space.floorNumber + 'F' : '-' }}</div>
        </div>
        <div>
          <span class="text-gray-400 text-xs">所属楼栋</span>
          <div class="text-gray-900">{{ space.buildingName || '-' }}</div>
        </div>
        <div>
          <span class="text-gray-400 text-xs">所属校区</span>
          <div class="text-gray-900">{{ space.campusName || '-' }}</div>
        </div>
      </div>
    </div>

    <!-- 房间网格 -->
    <div class="bg-white rounded-lg shadow-sm">
      <div class="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
        <h3 class="font-medium text-gray-900">房间列表</h3>
        <div class="flex items-center gap-4 text-xs">
          <span class="flex items-center gap-1">
            <span class="w-3 h-3 rounded bg-green-100 border border-green-300"></span>
            空闲
          </span>
          <span class="flex items-center gap-1">
            <span class="w-3 h-3 rounded bg-blue-100 border border-blue-300"></span>
            部分入住
          </span>
          <span class="flex items-center gap-1">
            <span class="w-3 h-3 rounded bg-red-100 border border-red-300"></span>
            满员
          </span>
          <span class="flex items-center gap-1">
            <span class="w-3 h-3 rounded bg-gray-100 border border-gray-300"></span>
            不可用
          </span>
        </div>
      </div>
      <div class="p-4">
        <div v-if="!space.children?.length" class="text-center text-gray-400 py-8">
          暂无房间
        </div>
        <div v-else class="grid grid-cols-6 gap-3">
          <div
            v-for="room in sortedRooms"
            :key="room.id"
            class="border rounded-lg p-3 cursor-pointer transition-all hover:shadow-md"
            :class="getRoomClass(room)"
            @click="$emit('selectRoom', room)"
          >
            <div class="flex items-center justify-between">
              <span class="font-bold text-sm">{{ room.roomNo ? room.roomNo + '室' : room.spaceName }}</span>
              <span class="text-xs px-1.5 py-0.5 rounded bg-gray-100 text-gray-600">
                {{ getRoomTypeName(room.roomType) }}
              </span>
            </div>
            <div v-if="room.roomNo" class="text-xs text-gray-400 mt-0.5 truncate" :title="room.spaceName">
              {{ room.spaceName }}
            </div>
            <div v-if="room.capacity" class="mt-2 flex items-center justify-between text-xs">
              <span>入住</span>
              <span class="font-medium">{{ room.currentOccupancy || 0 }}/{{ room.capacity }}</span>
            </div>
            <!-- 入住率进度条 -->
            <div v-if="room.capacity" class="mt-1 h-1 bg-gray-200 rounded-full overflow-hidden">
              <div
                class="h-full transition-all"
                :class="getProgressBarClass(room)"
                :style="{ width: getOccupancyPercent(room) + '%' }"
              ></div>
            </div>
            <!-- 状态标签 -->
            <div v-if="room.status !== 1" class="mt-2">
              <span
                class="px-1.5 py-0.5 text-xs rounded"
                :class="getStatusClass(room.status)"
              >
                {{ room.statusText }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <SpaceFormDialog
      v-model:visible="editDialogVisible"
      mode="edit"
      :space-type="'FLOOR'"
      :edit-data="space"
      @success="emit('refresh')"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Delete } from '@element-plus/icons-vue'
import { deleteSpace } from '@/api/space'
import type { SpaceDTO, SpaceStatus } from '@/types/space'
import { getRoomTypeName, getStatusClass } from '@/types/space'
import SpaceFormDialog from './SpaceFormDialog.vue'

const props = defineProps<{
  space: SpaceDTO
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
  (e: 'selectRoom', room: SpaceDTO): void
}>()

// 排序后的房间
const sortedRooms = computed(() => {
  if (!props.space.children) return []
  return [...props.space.children].sort((a, b) =>
    a.spaceName.localeCompare(b.spaceName, 'zh')
  )
})

// 统计
const roomCount = computed(() => props.space.children?.length || 0)

const totalCapacity = computed(() => {
  let capacity = 0
  props.space.children?.forEach(room => {
    capacity += room.capacity || 0
  })
  return capacity
})

const totalOccupancy = computed(() => {
  let occupancy = 0
  props.space.children?.forEach(room => {
    occupancy += room.currentOccupancy || 0
  })
  return occupancy
})

const occupancyRate = computed(() => {
  if (totalCapacity.value === 0) return 0
  return Math.round(totalOccupancy.value / totalCapacity.value * 100)
})

const occupancyRateColor = computed(() => {
  if (occupancyRate.value >= 90) return 'text-red-600'
  if (occupancyRate.value >= 70) return 'text-amber-600'
  return 'text-green-600'
})

// 辅助函数
function getRoomClass(room: SpaceDTO): string {
  if (room.status === 0) return 'bg-gray-50 border-gray-200 opacity-60'
  if (room.status === 2) return 'bg-amber-50 border-amber-200'

  if (!room.capacity) return 'bg-gray-50 border-gray-200'

  const rate = (room.currentOccupancy || 0) / room.capacity
  if (rate >= 1) return 'bg-red-50 border-red-200'
  if (rate > 0) return 'bg-blue-50 border-blue-200'
  return 'bg-green-50 border-green-200'
}

function getProgressBarClass(room: SpaceDTO): string {
  if (!room.capacity) return 'bg-gray-400'
  const rate = (room.currentOccupancy || 0) / room.capacity
  if (rate >= 1) return 'bg-red-500'
  if (rate >= 0.8) return 'bg-amber-500'
  return 'bg-green-500'
}

function getOccupancyPercent(room: SpaceDTO): number {
  if (!room.capacity) return 0
  return Math.min(100, Math.round((room.currentOccupancy || 0) / room.capacity * 100))
}

// 编辑
const editDialogVisible = ref(false)

function handleEdit() {
  editDialogVisible.value = true
}

// 删除
async function handleDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除楼层"${props.space.spaceName}"吗？`,
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
</script>
