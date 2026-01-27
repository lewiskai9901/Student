<template>
  <div class="space-y-4">
    <!-- 工具栏 -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <span class="text-lg font-medium text-gray-900">
          {{ floor.floorNumber }}F · {{ floor.spaceName }}
        </span>
        <span class="text-sm text-gray-500">
          共 {{ rooms.length }} 间房间
        </span>
      </div>
      <div class="flex items-center gap-2">
        <el-button size="small" @click="emit('selectAll')">
          全选本层
        </el-button>
        <el-button size="small" @click="emit('deselectAll')">
          取消本层
        </el-button>
      </div>
    </div>

    <!-- 楼层平面图 -->
    <div class="bg-white rounded-lg border border-gray-200 p-6">
      <!-- 走廊标识 -->
      <div class="relative">
        <!-- 房间网格 - 上半部分 -->
        <div class="grid gap-3 mb-4" :style="{ gridTemplateColumns: `repeat(${gridColumns}, minmax(0, 1fr))` }">
          <template v-for="(room, index) in topRooms" :key="room.id">
            <RoomCard
              :room="room"
              :selected="isRoomSelected(room.id)"
              :org-unit-color="getOrgUnitColor(room.orgUnitId)"
              @click="toggleRoom(room)"
            />
          </template>
        </div>

        <!-- 走廊 -->
        <div class="relative h-8 my-4">
          <div class="absolute inset-0 bg-gray-100 rounded flex items-center justify-center">
            <span class="text-xs text-gray-400 tracking-widest">走 廊</span>
          </div>
        </div>

        <!-- 房间网格 - 下半部分 -->
        <div class="grid gap-3" :style="{ gridTemplateColumns: `repeat(${gridColumns}, minmax(0, 1fr))` }">
          <template v-for="(room, index) in bottomRooms" :key="room.id">
            <RoomCard
              :room="room"
              :selected="isRoomSelected(room.id)"
              :org-unit-color="getOrgUnitColor(room.orgUnitId)"
              @click="toggleRoom(room)"
            />
          </template>
        </div>
      </div>
    </div>

    <!-- 图例说明 -->
    <div class="bg-gray-50 rounded-lg p-4">
      <div class="text-sm text-gray-500 mb-2">图例说明：</div>
      <div class="flex flex-wrap gap-4 text-sm">
        <div class="flex items-center gap-2">
          <div class="w-4 h-4 rounded border-2 border-blue-500 bg-blue-100"></div>
          <span class="text-gray-600">已选择</span>
        </div>
        <div class="flex items-center gap-2">
          <div class="w-4 h-4 rounded bg-gray-100 border border-gray-300"></div>
          <span class="text-gray-600">未分配</span>
        </div>
        <div class="flex items-center gap-2">
          <div class="w-4 h-4 rounded bg-green-100 border border-green-300"></div>
          <span class="text-gray-600">已分配部门</span>
        </div>
        <div class="flex items-center gap-2">
          <div class="w-4 h-4 rounded bg-teal-100 border border-teal-300"></div>
          <span class="text-gray-600">已分配班级</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SpaceDTO } from '@/types/space'
import RoomCard from './RoomCard.vue'

const props = defineProps<{
  floor: SpaceDTO
  selectedRooms: SpaceDTO[]
  orgUnitColors: Record<string, string>
}>()

const emit = defineEmits<{
  (e: 'select', room: SpaceDTO, selected: boolean): void
  (e: 'selectAll'): void
  (e: 'deselectAll'): void
}>()

// 房间列表
const rooms = computed(() => props.floor.children || [])

// 网格列数（根据房间数量自动计算，最多10列）
const gridColumns = computed(() => {
  const count = rooms.value.length
  if (count <= 6) return count
  if (count <= 12) return Math.ceil(count / 2)
  if (count <= 20) return Math.ceil(count / 2)
  return 10
})

// 上半部分房间
const topRooms = computed(() => {
  const half = Math.ceil(rooms.value.length / 2)
  return rooms.value.slice(0, half)
})

// 下半部分房间
const bottomRooms = computed(() => {
  const half = Math.ceil(rooms.value.length / 2)
  return rooms.value.slice(half)
})

// 检查房间是否被选中
function isRoomSelected(roomId: number): boolean {
  return props.selectedRooms.some(r => r.id === roomId)
}

// 获取组织单元颜色
function getOrgUnitColor(orgUnitId?: number): string | undefined {
  if (!orgUnitId) return undefined
  return props.orgUnitColors[String(orgUnitId)]
}

// 切换房间选择
function toggleRoom(room: SpaceDTO) {
  const selected = isRoomSelected(room.id)
  emit('select', room, !selected)
}
</script>
