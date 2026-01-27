<template>
  <div class="space-y-4">
    <!-- 标题和操作 -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-xl font-bold text-gray-900">{{ space.spaceName }}</h2>
        <p class="text-sm text-gray-500 mt-1">校区编码：{{ space.spaceCode }}</p>
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
      <div class="bg-blue-50 rounded-lg p-4 text-center">
        <div class="text-3xl font-bold text-blue-600">{{ buildingCount }}</div>
        <div class="text-sm text-blue-500 mt-1">楼栋数</div>
      </div>
      <div class="bg-purple-50 rounded-lg p-4 text-center">
        <div class="text-3xl font-bold text-purple-600">{{ roomCount }}</div>
        <div class="text-sm text-purple-500 mt-1">房间数</div>
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
        <span class="w-1 h-4 bg-blue-500 rounded"></span>
        基本信息
      </h3>
      <div class="grid grid-cols-4 gap-x-8 gap-y-3 text-sm">
        <div>
          <span class="text-gray-400 text-xs">校区名称</span>
          <div class="text-gray-900 font-medium">{{ space.spaceName || '-' }}</div>
        </div>
        <div>
          <span class="text-gray-400 text-xs">校区编码</span>
          <div class="text-gray-900 font-mono text-xs">{{ space.spaceCode || '-' }}</div>
        </div>
        <div>
          <span class="text-gray-400 text-xs">创建时间</span>
          <div class="text-gray-900">{{ formatDate(space.createdAt) }}</div>
        </div>
        <div>
          <span class="text-gray-400 text-xs">详细地址</span>
          <div class="text-gray-900">{{ space.description || '暂未填写' }}</div>
        </div>
      </div>
    </div>

    <!-- 楼栋列表 -->
    <div class="bg-white rounded-lg shadow-sm">
      <div class="px-4 py-3 border-b border-gray-100">
        <h3 class="font-medium text-gray-900">楼栋列表</h3>
      </div>
      <div class="p-4">
        <div v-if="!space.children?.length" class="text-center text-gray-400 py-8">
          暂无楼栋
        </div>
        <div v-else class="grid grid-cols-3 gap-4">
          <div
            v-for="building in space.children"
            :key="building.id"
            class="border border-gray-200 rounded-lg p-4 hover:border-blue-300 hover:shadow-sm transition-all cursor-pointer"
          >
            <div class="flex items-center justify-between mb-2">
              <span class="font-medium text-gray-900">{{ building.spaceName }}</span>
              <span
                class="px-2 py-0.5 text-xs rounded"
                :class="getBuildingTypeClass(building.buildingType)"
              >
                {{ getBuildingTypeName(building.buildingType) }}
              </span>
            </div>
            <div class="text-sm text-gray-500 space-y-1">
              <div class="flex justify-between">
                <span>楼层数</span>
                <span class="font-medium text-gray-700">{{ building.children?.length || 0 }}</span>
              </div>
              <div class="flex justify-between">
                <span>房间数</span>
                <span class="font-medium text-gray-700">{{ countRooms(building) }}</span>
              </div>
              <div v-if="building.capacity" class="flex justify-between">
                <span>入住率</span>
                <span class="font-medium" :class="getOccupancyRateColor(building)">
                  {{ Math.round((building.currentOccupancy || 0) / building.capacity * 100) }}%
                </span>
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
      :space-type="'CAMPUS'"
      :edit-data="space"
      @success="emit('refresh')"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Delete } from '@element-plus/icons-vue'
import { deleteSpace } from '@/api/v2/space'
import type { SpaceDTO, BuildingType } from '@/types/v2/space'
import SpaceFormDialog from './SpaceFormDialog.vue'

const props = defineProps<{
  space: SpaceDTO
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

// 格式化日期
function formatDate(dateStr?: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

// 统计
const buildingCount = computed(() => props.space.children?.length || 0)

const roomCount = computed(() => {
  let count = 0
  props.space.children?.forEach(building => {
    building.children?.forEach(floor => {
      count += floor.children?.length || 0
    })
  })
  return count
})

const totalCapacity = computed(() => {
  let capacity = 0
  props.space.children?.forEach(building => {
    building.children?.forEach(floor => {
      floor.children?.forEach(room => {
        capacity += room.capacity || 0
      })
    })
  })
  return capacity
})

const totalOccupancy = computed(() => {
  let occupancy = 0
  props.space.children?.forEach(building => {
    building.children?.forEach(floor => {
      floor.children?.forEach(room => {
        occupancy += room.currentOccupancy || 0
      })
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
function countRooms(building: SpaceDTO): number {
  let count = 0
  building.children?.forEach(floor => {
    count += floor.children?.length || 0
  })
  return count
}

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

function getOccupancyRateColor(building: SpaceDTO): string {
  if (!building.capacity) return 'text-gray-600'
  const rate = (building.currentOccupancy || 0) / building.capacity * 100
  if (rate >= 90) return 'text-red-600'
  if (rate >= 70) return 'text-amber-600'
  return 'text-green-600'
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
      `确定要删除校区"${props.space.spaceName}"吗？`,
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
