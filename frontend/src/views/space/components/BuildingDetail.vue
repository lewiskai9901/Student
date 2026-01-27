<template>
  <div class="space-y-4">
    <!-- 标题和操作 -->
    <div class="flex items-center justify-between">
      <div>
        <div class="flex items-center gap-2">
          <h2 class="text-xl font-bold text-gray-900">{{ space.spaceName }}</h2>
          <span
            class="px-2 py-0.5 text-xs rounded"
            :class="getBuildingTypeClass(space.buildingType)"
          >
            {{ getBuildingTypeName(space.buildingType) }}
          </span>
        </div>
        <p class="text-sm text-gray-500 mt-1">
          {{ space.campusName }} · {{ space.buildingNo ? space.buildingNo + '号楼' : space.spaceCode }}
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

    <!-- 统计卡片 -->
    <div class="grid grid-cols-4 gap-4">
      <div class="bg-purple-50 rounded-lg p-4 text-center">
        <div class="text-3xl font-bold text-purple-600">{{ floorCount }}</div>
        <div class="text-sm text-purple-500 mt-1">楼层数</div>
      </div>
      <div class="bg-cyan-50 rounded-lg p-4 text-center">
        <div class="text-3xl font-bold text-cyan-600">{{ roomCount }}</div>
        <div class="text-sm text-cyan-500 mt-1">房间数</div>
      </div>
      <div class="bg-green-50 rounded-lg p-4 text-center">
        <div class="text-3xl font-bold text-green-600">{{ totalCapacity }}</div>
        <div class="text-sm text-green-500 mt-1">总容量</div>
      </div>
      <div class="bg-amber-50 rounded-lg p-4 text-center">
        <div class="text-3xl font-bold" :class="occupancyRateColor">{{ occupancyRate }}%</div>
        <div class="text-sm text-amber-500 mt-1">入住率</div>
      </div>
    </div>

    <!-- 基本信息卡片 -->
    <div class="bg-white rounded-lg shadow-sm p-4">
      <h3 class="font-medium text-gray-900 mb-3 text-sm flex items-center gap-2">
        <span class="w-1 h-4 bg-purple-500 rounded"></span>
        基本信息
      </h3>
      <div class="grid grid-cols-4 gap-x-8 gap-y-3 text-sm">
        <div>
          <span class="text-gray-400 text-xs">楼栋名称</span>
          <div class="text-gray-900">{{ space.spaceName || '-' }}</div>
        </div>
        <div>
          <span class="text-gray-400 text-xs">楼号</span>
          <div class="text-gray-900 font-semibold">{{ space.buildingNo ? space.buildingNo + '号楼' : '-' }}</div>
        </div>
        <div>
          <span class="text-gray-400 text-xs">楼栋编码</span>
          <div class="text-gray-900 font-mono text-xs">{{ space.spaceCode }}</div>
        </div>
        <div>
          <span class="text-gray-400 text-xs">所属校区</span>
          <div class="text-gray-900">{{ space.campusName || '-' }}</div>
        </div>
        <div v-if="space.description" class="col-span-4">
          <span class="text-gray-400 text-xs">描述</span>
          <div class="text-gray-900">{{ space.description }}</div>
        </div>
      </div>
    </div>

    <!-- 楼层列表 -->
    <div class="bg-white rounded-lg shadow-sm">
      <div class="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
        <h3 class="font-medium text-gray-900">楼层概览</h3>
      </div>
      <div class="p-4">
        <div v-if="!space.children?.length" class="text-center text-gray-400 py-8">
          暂无楼层
        </div>
        <div v-else class="space-y-3">
          <div
            v-for="floor in sortedFloors"
            :key="floor.id"
            class="border border-gray-200 rounded-lg p-4 hover:border-blue-300 transition-colors"
          >
            <div class="flex items-center justify-between mb-3">
              <span class="font-medium text-gray-900">
                {{ floor.floorNumber }}F
                <span class="text-sm text-gray-500 ml-2">{{ floor.spaceName }}</span>
              </span>
              <div class="flex items-center gap-4 text-sm">
                <span class="text-gray-500">
                  房间 <span class="font-medium text-gray-700">{{ floor.children?.length || 0 }}</span>
                </span>
                <span v-if="getFloorCapacity(floor)" class="text-gray-500">
                  入住 <span class="font-medium" :class="getFloorOccupancyColor(floor)">
                    {{ getFloorOccupancy(floor) }}/{{ getFloorCapacity(floor) }}
                  </span>
                </span>
              </div>
            </div>
            <!-- 房间预览 -->
            <div v-if="floor.children?.length" class="flex flex-wrap gap-2">
              <div
                v-for="room in floor.children.slice(0, 10)"
                :key="room.id"
                class="px-2 py-1 text-xs rounded border"
                :class="getRoomClass(room)"
              >
                <span class="font-medium">{{ room.roomNo ? room.roomNo + '室' : room.spaceName }}</span>
                <span v-if="room.capacity" class="text-gray-400 ml-1">
                  {{ room.currentOccupancy || 0 }}/{{ room.capacity }}
                </span>
              </div>
              <div
                v-if="floor.children.length > 10"
                class="px-2 py-1 text-xs text-gray-400"
              >
                +{{ floor.children.length - 10 }} 更多
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <SpaceFormDialog
      v-model:visible="editDialogVisible"
      mode="edit"
      :space-type="'BUILDING'"
      :edit-data="space"
      @success="emit('refresh')"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Delete, ArrowDown } from '@element-plus/icons-vue'
import { deleteSpace, changeSpaceStatus } from '@/api/space'
import type { SpaceDTO, SpaceStatus } from '@/types/space'
import SpaceFormDialog from './SpaceFormDialog.vue'

const props = defineProps<{
  space: SpaceDTO
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

// 排序后的楼层
const sortedFloors = computed(() => {
  if (!props.space.children) return []
  return [...props.space.children].sort((a, b) =>
    (b.floorNumber || 0) - (a.floorNumber || 0)
  )
})

// 统计
const floorCount = computed(() => props.space.children?.length || 0)

const roomCount = computed(() => {
  let count = 0
  props.space.children?.forEach(floor => {
    count += floor.children?.length || 0
  })
  return count
})

const totalCapacity = computed(() => {
  let capacity = 0
  props.space.children?.forEach(floor => {
    floor.children?.forEach(room => {
      capacity += room.capacity || 0
    })
  })
  return capacity
})

const totalOccupancy = computed(() => {
  let occupancy = 0
  props.space.children?.forEach(floor => {
    floor.children?.forEach(room => {
      occupancy += room.currentOccupancy || 0
    })
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
function getBuildingTypeName(type?: string): string {
  const names: Record<string, string> = {
    TEACHING: '教学楼',
    DORMITORY: '宿舍楼',
    OFFICE: '办公楼',
    MIXED: '综合楼'
  }
  return names[type || ''] || '未知'
}

function getBuildingTypeClass(type?: string): string {
  const classes: Record<string, string> = {
    TEACHING: 'bg-blue-100 text-blue-700',
    DORMITORY: 'bg-teal-100 text-teal-700',
    OFFICE: 'bg-purple-100 text-purple-700',
    MIXED: 'bg-gray-100 text-gray-700'
  }
  return classes[type || ''] || 'bg-gray-100 text-gray-600'
}

function getFloorCapacity(floor: SpaceDTO): number {
  let capacity = 0
  floor.children?.forEach(room => {
    capacity += room.capacity || 0
  })
  return capacity
}

function getFloorOccupancy(floor: SpaceDTO): number {
  let occupancy = 0
  floor.children?.forEach(room => {
    occupancy += room.currentOccupancy || 0
  })
  return occupancy
}

function getFloorOccupancyColor(floor: SpaceDTO): string {
  const capacity = getFloorCapacity(floor)
  if (capacity === 0) return 'text-gray-600'
  const rate = getFloorOccupancy(floor) / capacity * 100
  if (rate >= 90) return 'text-red-600'
  if (rate >= 70) return 'text-amber-600'
  return 'text-green-600'
}

function getRoomClass(room: SpaceDTO): string {
  if (room.status === 0) return 'bg-gray-100 text-gray-500 border-gray-200'
  if (room.status === 2) return 'bg-amber-50 text-amber-700 border-amber-200'

  if (!room.capacity) return 'bg-gray-50 text-gray-600 border-gray-200'

  const rate = (room.currentOccupancy || 0) / room.capacity
  if (rate >= 1) return 'bg-red-50 text-red-700 border-red-200'
  if (rate >= 0.8) return 'bg-amber-50 text-amber-700 border-amber-200'
  if (rate > 0) return 'bg-blue-50 text-blue-700 border-blue-200'
  return 'bg-green-50 text-green-700 border-green-200'
}

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
      `确定要删除楼栋"${props.space.spaceName}"吗？`,
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
