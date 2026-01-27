<template>
  <div class="h-full flex flex-col bg-gray-50">
    <!-- 顶部工具栏 -->
    <div class="bg-white border-b border-gray-200 px-6 py-4">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-xl font-bold text-gray-900">场所分配中心</h1>
          <p class="text-sm text-gray-500 mt-1">批量分配房间给部门和班级，支持图形化楼层视图</p>
        </div>
        <div class="flex items-center gap-3">
          <!-- 楼栋类型筛选 -->
          <el-select
            v-model="filterBuildingType"
            placeholder="全部楼栋类型"
            clearable
            style="width: 140px"
            @change="loadBuildings"
          >
            <el-option label="全部类型" value="" />
            <el-option label="宿舍楼" value="DORMITORY" />
            <el-option label="教学楼" value="TEACHING" />
            <el-option label="办公楼" value="OFFICE" />
            <el-option label="综合楼" value="MIXED" />
          </el-select>
          <!-- 部门筛选 -->
          <el-select
            v-model="filterOrgUnitId"
            placeholder="全部部门"
            clearable
            filterable
            style="width: 180px"
            @change="loadBuildings"
          >
            <el-option label="全部部门" :value="null" />
            <el-option label="未分配" :value="0" />
            <el-option
              v-for="org in orgUnitList"
              :key="org.id"
              :label="org.name"
              :value="org.id"
            />
          </el-select>
          <!-- 刷新 -->
          <el-button :icon="Refresh" @click="loadBuildings" :loading="loading">
            刷新
          </el-button>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="flex-1 overflow-hidden flex">
      <!-- 左侧：楼栋列表 -->
      <div class="w-80 flex-shrink-0 bg-white border-r border-gray-200 flex flex-col">
        <div class="p-4 border-b border-gray-100">
          <div class="flex items-center gap-2">
            <el-icon class="text-blue-500"><OfficeBuilding /></el-icon>
            <span class="font-medium text-gray-900">楼栋列表</span>
            <span class="ml-auto text-sm text-gray-400">{{ buildings.length }} 栋</span>
          </div>
        </div>

        <div class="flex-1 overflow-auto p-3 space-y-3">
          <div v-if="loading" class="flex justify-center py-8">
            <el-icon class="animate-spin text-2xl text-gray-400"><Loading /></el-icon>
          </div>
          <template v-else-if="buildings.length">
            <BuildingAllocationCard
              v-for="building in buildings"
              :key="building.id"
              :building="building"
              :selected="selectedBuilding?.id === building.id"
              :org-unit-colors="orgUnitColorMap"
              @click="selectBuilding(building)"
            />
          </template>
          <div v-else class="text-center py-8 text-gray-400">
            <el-icon class="text-4xl"><OfficeBuilding /></el-icon>
            <p class="mt-2">暂无楼栋数据</p>
          </div>
        </div>
      </div>

      <!-- 右侧：楼层平面图 -->
      <div class="flex-1 flex flex-col overflow-hidden">
        <template v-if="selectedBuilding">
          <!-- 楼栋信息头 -->
          <div class="bg-white border-b border-gray-200 px-6 py-4">
            <div class="flex items-center justify-between">
              <div>
                <div class="flex items-center gap-3">
                  <h2 class="text-lg font-bold text-gray-900">{{ selectedBuilding.spaceName }}</h2>
                  <span
                    class="px-2 py-0.5 text-xs rounded"
                    :class="getBuildingTypeClass(selectedBuilding.buildingType)"
                  >
                    {{ getBuildingTypeName(selectedBuilding.buildingType) }}
                  </span>
                </div>
                <p class="text-sm text-gray-500 mt-1">
                  {{ selectedBuilding.campusName }} · 共 {{ floorCount }} 层 · {{ roomCount }} 间房间
                </p>
              </div>
              <div class="flex items-center gap-2">
                <el-button
                  type="primary"
                  :disabled="selectedRooms.length === 0"
                  @click="showBatchAllocationDialog"
                >
                  <el-icon class="mr-1"><Share /></el-icon>
                  批量分配 ({{ selectedRooms.length }})
                </el-button>
                <el-button @click="clearSelection" :disabled="selectedRooms.length === 0">
                  取消选择
                </el-button>
              </div>
            </div>

            <!-- 分配统计 -->
            <div class="mt-4 flex items-center gap-6">
              <AllocationLegend :statistics="buildingAllocationStats" :org-unit-colors="orgUnitColorMap" />
            </div>
          </div>

          <!-- 楼层选择器 -->
          <div class="bg-gray-100 px-6 py-2 border-b border-gray-200 flex items-center gap-2 overflow-x-auto">
            <span class="text-sm text-gray-500 flex-shrink-0">楼层:</span>
            <el-button
              v-for="floor in sortedFloors"
              :key="floor.id"
              size="small"
              :type="selectedFloorId === floor.id ? 'primary' : 'default'"
              @click="selectFloor(floor.id)"
            >
              {{ floor.floorNumber }}F
              <span class="ml-1 text-xs opacity-70">({{ floor.children?.length || 0 }})</span>
            </el-button>
          </div>

          <!-- 楼层平面图 -->
          <div class="flex-1 overflow-auto p-6">
            <FloorPlanView
              v-if="selectedFloor"
              :floor="selectedFloor"
              :selected-rooms="selectedRooms"
              :org-unit-colors="orgUnitColorMap"
              @select="handleRoomSelect"
              @select-all="handleSelectAll"
              @deselect-all="handleDeselectAll"
            />
            <div v-else class="flex items-center justify-center h-full text-gray-400">
              <div class="text-center">
                <el-icon class="text-5xl"><Tickets /></el-icon>
                <p class="mt-2">请选择楼层查看房间分布</p>
              </div>
            </div>
          </div>
        </template>

        <!-- 未选择楼栋时的提示 -->
        <div v-else class="flex-1 flex items-center justify-center bg-white">
          <div class="text-center text-gray-400">
            <el-icon class="text-6xl"><OfficeBuilding /></el-icon>
            <p class="mt-4 text-lg">请从左侧选择一栋楼栋</p>
            <p class="mt-1 text-sm">查看楼层分布并进行房间分配</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 批量分配对话框 -->
    <BatchAllocationDialog
      v-model:visible="batchDialogVisible"
      :rooms="selectedRooms"
      :building-type="selectedBuilding?.buildingType"
      :org-unit-list="orgUnitList"
      @success="handleAllocationSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  OfficeBuilding,
  Refresh,
  Share,
  Tickets,
  Loading
} from '@element-plus/icons-vue'
import { getSpaceTree } from '@/api/v2/space'
import { getOrgUnits } from '@/api/v2/organization'
import type { SpaceDTO, BuildingType } from '@/types/v2/space'
import { getBuildingTypeName } from '@/types/v2/space'
import BuildingAllocationCard from './components/BuildingAllocationCard.vue'
import FloorPlanView from './components/FloorPlanView.vue'
import AllocationLegend from './components/AllocationLegend.vue'
import BatchAllocationDialog from './components/BatchAllocationDialog.vue'

// ========== 状态 ==========

const loading = ref(false)
const filterBuildingType = ref<BuildingType | ''>('')
const filterOrgUnitId = ref<number | null>(null)

// 楼栋数据
const spaceTree = ref<SpaceDTO[]>([])
const buildings = ref<SpaceDTO[]>([])
const selectedBuilding = ref<SpaceDTO | null>(null)
const selectedFloorId = ref<number | null>(null)
const selectedRooms = ref<SpaceDTO[]>([])

// 组织单元
const orgUnitList = ref<{ id: number; name: string }[]>([])

// 批量分配对话框
const batchDialogVisible = ref(false)

// ========== 计算属性 ==========

// 当前楼栋的楼层列表（排序）
const sortedFloors = computed(() => {
  if (!selectedBuilding.value?.children) return []
  return [...selectedBuilding.value.children].sort((a, b) =>
    (b.floorNumber || 0) - (a.floorNumber || 0)
  )
})

// 当前选中的楼层
const selectedFloor = computed(() => {
  if (!selectedFloorId.value) return null
  return sortedFloors.value.find(f => f.id === selectedFloorId.value) || null
})

// 楼层数
const floorCount = computed(() => selectedBuilding.value?.children?.length || 0)

// 房间数
const roomCount = computed(() => {
  if (!selectedBuilding.value?.children) return 0
  return selectedBuilding.value.children.reduce((sum, floor) =>
    sum + (floor.children?.length || 0), 0
  )
})

// 楼栋分配统计
const buildingAllocationStats = computed(() => {
  if (!selectedBuilding.value?.children) return { total: 0, byOrgUnit: {} }

  const stats: { total: number; unassigned: number; byOrgUnit: Record<string, { name: string; count: number }> } = {
    total: 0,
    unassigned: 0,
    byOrgUnit: {}
  }

  selectedBuilding.value.children.forEach(floor => {
    floor.children?.forEach(room => {
      stats.total++
      if (!room.orgUnitId) {
        stats.unassigned++
      } else {
        const key = String(room.orgUnitId)
        if (!stats.byOrgUnit[key]) {
          stats.byOrgUnit[key] = { name: room.orgUnitName || '未知部门', count: 0 }
        }
        stats.byOrgUnit[key].count++
      }
    })
  })

  return stats
})

// 组织单元颜色映射
const orgUnitColorMap = computed(() => {
  const colors = [
    'blue', 'green', 'purple', 'orange', 'cyan', 'pink', 'teal', 'indigo', 'amber', 'rose'
  ]
  const map: Record<string, string> = {}

  const orgUnitsInUse = Object.keys(buildingAllocationStats.value.byOrgUnit)
  orgUnitsInUse.forEach((orgId, index) => {
    map[orgId] = colors[index % colors.length]
  })

  return map
})

// ========== 方法 ==========

// 获取楼栋类型样式
function getBuildingTypeClass(type?: BuildingType) {
  const classes: Record<string, string> = {
    DORMITORY: 'bg-teal-100 text-teal-700',
    TEACHING: 'bg-blue-100 text-blue-700',
    OFFICE: 'bg-slate-100 text-slate-700',
    MIXED: 'bg-purple-100 text-purple-700'
  }
  return classes[type || ''] || 'bg-gray-100 text-gray-600'
}

// 加载楼栋数据
async function loadBuildings() {
  loading.value = true
  try {
    const res = await getSpaceTree(filterBuildingType.value || undefined, true)
    spaceTree.value = res || []

    // 提取所有楼栋
    const allBuildings: SpaceDTO[] = []
    spaceTree.value.forEach(campus => {
      campus.children?.forEach(building => {
        if (building.spaceType === 'BUILDING') {
          // 附加校区名称
          building.campusName = campus.spaceName
          allBuildings.push(building)
        }
      })
    })

    // 如果有部门筛选，过滤楼栋
    if (filterOrgUnitId.value !== null) {
      buildings.value = allBuildings.filter(building => {
        // 检查楼栋下是否有房间属于该部门
        return building.children?.some(floor =>
          floor.children?.some(room =>
            filterOrgUnitId.value === 0
              ? !room.orgUnitId
              : room.orgUnitId === filterOrgUnitId.value
          )
        )
      })
    } else {
      buildings.value = allBuildings
    }

    // 如果之前选中的楼栋不在当前列表中，清除选中
    if (selectedBuilding.value && !buildings.value.find(b => b.id === selectedBuilding.value!.id)) {
      selectedBuilding.value = null
      selectedFloorId.value = null
      selectedRooms.value = []
    }
  } catch (error) {
    console.error('加载楼栋数据失败:', error)
    ElMessage.error('加载楼栋数据失败')
  } finally {
    loading.value = false
  }
}

// 加载组织单元
async function loadOrgUnits() {
  try {
    const res = await getOrgUnits()
    orgUnitList.value = (res || []).map((org: any) => ({
      id: org.id,
      name: org.name
    }))
  } catch (error) {
    console.error('加载组织单元失败:', error)
  }
}

// 选择楼栋
function selectBuilding(building: SpaceDTO) {
  selectedBuilding.value = building
  selectedRooms.value = []

  // 默认选中最高层
  if (building.children?.length) {
    const sorted = [...building.children].sort((a, b) =>
      (b.floorNumber || 0) - (a.floorNumber || 0)
    )
    selectedFloorId.value = sorted[0].id
  } else {
    selectedFloorId.value = null
  }
}

// 选择楼层
function selectFloor(floorId: number) {
  selectedFloorId.value = floorId
}

// 处理房间选择
function handleRoomSelect(room: SpaceDTO, selected: boolean) {
  if (selected) {
    if (!selectedRooms.value.find(r => r.id === room.id)) {
      selectedRooms.value.push(room)
    }
  } else {
    selectedRooms.value = selectedRooms.value.filter(r => r.id !== room.id)
  }
}

// 全选当前楼层
function handleSelectAll() {
  if (!selectedFloor.value?.children) return

  selectedFloor.value.children.forEach(room => {
    if (!selectedRooms.value.find(r => r.id === room.id)) {
      selectedRooms.value.push(room)
    }
  })
}

// 取消全选当前楼层
function handleDeselectAll() {
  if (!selectedFloor.value?.children) return

  const floorRoomIds = new Set(selectedFloor.value.children.map(r => r.id))
  selectedRooms.value = selectedRooms.value.filter(r => !floorRoomIds.has(r.id))
}

// 清除所有选择
function clearSelection() {
  selectedRooms.value = []
}

// 显示批量分配对话框
function showBatchAllocationDialog() {
  if (selectedRooms.value.length === 0) {
    ElMessage.warning('请先选择要分配的房间')
    return
  }
  batchDialogVisible.value = true
}

// 分配成功回调
function handleAllocationSuccess() {
  ElMessage.success('分配成功')
  selectedRooms.value = []
  loadBuildings()
}

// ========== 生命周期 ==========

onMounted(() => {
  loadBuildings()
  loadOrgUnits()
})
</script>

<style scoped>
/* 自定义滚动条 */
:deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}
</style>
