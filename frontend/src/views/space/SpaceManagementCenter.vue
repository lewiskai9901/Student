<template>
  <div class="flex h-full bg-gray-50">
    <!-- 左侧面板：场所树 -->
    <div class="w-72 flex-shrink-0 border-r border-gray-200 bg-white flex flex-col">
      <!-- 标题和操作 -->
      <div class="p-4 border-b border-gray-100">
        <div class="flex items-center justify-between mb-3">
          <h2 class="text-base font-semibold text-gray-900">场所管理</h2>
          <div class="flex items-center gap-2">
            <el-button size="small" @click="router.push('/space/allocation')">
              <el-icon class="mr-1"><Share /></el-icon>
              批量分配
            </el-button>
            <el-dropdown @command="handleAddCommand">
              <el-button type="primary" size="small">
                <el-icon class="mr-1"><Plus /></el-icon>
                新增
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="campus">新增校区</el-dropdown-item>
                  <el-dropdown-item command="building" :disabled="!selectedCampus">新增楼栋</el-dropdown-item>
                  <el-dropdown-item command="floor" :disabled="!selectedBuilding">新增楼层</el-dropdown-item>
                  <el-dropdown-item command="room" :disabled="!selectedFloor">新增房间</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <!-- 楼栋类型筛选 -->
        <el-select
          v-model="filterBuildingType"
          placeholder="全部类型"
          clearable
          size="small"
          class="w-full"
          @change="loadTree"
        >
          <el-option label="全部类型" value="" />
          <el-option label="教学楼" value="TEACHING" />
          <el-option label="宿舍楼" value="DORMITORY" />
          <el-option label="办公楼" value="OFFICE" />
          <el-option label="综合楼" value="MIXED" />
        </el-select>
      </div>

      <!-- 场所树 -->
      <div class="flex-1 overflow-auto p-2">
        <el-tree
          ref="treeRef"
          :data="treeData"
          :props="treeProps"
          node-key="id"
          :highlight-current="true"
          :expand-on-click-node="false"
          :default-expanded-keys="expandedKeys"
          @node-click="handleNodeClick"
        >
          <template #default="{ node, data }">
            <div class="flex items-center justify-between w-full pr-2 py-1 group">
              <div class="flex items-center min-w-0">
                <el-icon class="mr-1.5 text-gray-400 flex-shrink-0">
                  <component :is="getNodeIcon(data.spaceType)" />
                </el-icon>
                <span class="truncate text-sm" :title="data.spaceName">
                  <!-- 楼层特殊显示：3F · 三楼 -->
                  <template v-if="data.spaceType === 'FLOOR'">
                    <span class="text-gray-600 font-medium">{{ data.floorNumber }}F</span>
                    <span class="text-gray-400 ml-1">· {{ data.spaceName }}</span>
                  </template>
                  <template v-else>
                    {{ data.spaceName }}
                  </template>
                  <span v-if="data.spaceType === 'BUILDING' && data.buildingNo" class="text-gray-400">({{ data.buildingNo }}号)</span>
                  <span v-if="data.spaceType === 'ROOM' && data.roomNo" class="text-gray-400">({{ data.roomNo }}室)</span>
                </span>
                <!-- 楼栋显示楼层数 -->
                <span v-if="data.spaceType === 'BUILDING'" class="ml-1 text-xs text-gray-400">
                  ({{ data.children?.length || 0 }}层)
                </span>
                <!-- 楼层显示房间数 -->
                <span v-if="data.spaceType === 'FLOOR'" class="ml-1 text-xs text-gray-400">
                  ({{ data.children?.length || 0 }}间)
                </span>
                <!-- 房间显示入住率 -->
                <span v-if="data.spaceType === 'ROOM' && data.capacity" class="ml-1 text-xs text-gray-400">
                  {{ data.currentOccupancy || 0 }}/{{ data.capacity }}
                </span>
              </div>
              <!-- 状态标记 -->
              <span
                v-if="data.status !== 1"
                class="ml-2 px-1.5 py-0.5 text-xs rounded flex-shrink-0"
                :class="getStatusClass(data.status)"
              >
                {{ data.statusText }}
              </span>
            </div>
          </template>
        </el-tree>
      </div>
    </div>

    <!-- 右侧内容区 -->
    <div class="flex-1 flex flex-col min-w-0 overflow-hidden">
      <!-- 统计面板 -->
      <div class="p-4 bg-white border-b border-gray-200">
        <div class="grid grid-cols-5 gap-4">
          <div
            v-for="stat in statistics"
            :key="stat.label"
            class="bg-gray-50 rounded-lg p-3 text-center"
          >
            <div class="text-2xl font-bold" :class="stat.color">{{ stat.value }}</div>
            <div class="text-xs text-gray-500 mt-1">{{ stat.label }}</div>
          </div>
        </div>
      </div>

      <!-- 内容区域：根据选中节点显示不同内容 -->
      <div class="flex-1 overflow-auto p-4">
        <!-- 未选中时显示概览 -->
        <div v-if="!selectedNode" class="flex items-center justify-center h-full">
          <div class="text-center text-gray-400">
            <el-icon class="text-6xl mb-4"><OfficeBuilding /></el-icon>
            <p>请从左侧选择一个场所查看详情</p>
          </div>
        </div>

        <!-- 选中校区：显示楼栋列表 -->
        <div v-else-if="selectedNode.spaceType === 'CAMPUS'">
          <CampusDetail :space="selectedNode" @refresh="loadTree" />
        </div>

        <!-- 选中楼栋：显示楼层和房间统计 -->
        <div v-else-if="selectedNode.spaceType === 'BUILDING'">
          <BuildingDetail :space="selectedNode" @refresh="loadTree" />
        </div>

        <!-- 选中楼层：显示房间网格 -->
        <div v-else-if="selectedNode.spaceType === 'FLOOR'">
          <FloorDetail :space="selectedNode" @refresh="loadTree" />
        </div>

        <!-- 选中房间：显示详情和入住信息 -->
        <div v-else-if="selectedNode.spaceType === 'ROOM'">
          <RoomDetail :space="selectedNode" @refresh="loadTree" />
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <SpaceFormDialog
      v-model:visible="formDialogVisible"
      :mode="formMode"
      :space-type="formSpaceType"
      :parent-id="formParentId"
      :edit-data="editData"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElTree } from 'element-plus'
import {
  Plus,
  School,
  OfficeBuilding,
  Menu,
  House,
  Share
} from '@element-plus/icons-vue'

import { getSpaceTree } from '@/api/space'
import type { SpaceDTO, SpaceType, BuildingType } from '@/types/space'
import { getStatusClass } from '@/types/space'

const router = useRouter()

// 子组件（稍后创建）
import CampusDetail from './components/CampusDetail.vue'
import BuildingDetail from './components/BuildingDetail.vue'
import FloorDetail from './components/FloorDetail.vue'
import RoomDetail from './components/RoomDetail.vue'
import SpaceFormDialog from './components/SpaceFormDialog.vue'

// ========== 树形数据 ==========
const treeRef = ref<InstanceType<typeof ElTree>>()
const treeData = ref<SpaceDTO[]>([])
const expandedKeys = ref<number[]>([])
const filterBuildingType = ref<BuildingType | ''>('')

const treeProps = {
  children: 'children',
  label: 'spaceName'
}

// 加载树
async function loadTree() {
  try {
    const res = await getSpaceTree(
      filterBuildingType.value || undefined,
      true
    )
    treeData.value = res || []

    // 默认展开第一个校区
    if (treeData.value.length > 0 && expandedKeys.value.length === 0) {
      expandedKeys.value = [treeData.value[0].id]
    }
  } catch (error) {
    console.error('加载场所树失败:', error)
    ElMessage.error('加载数据失败')
  }
}

// 获取节点图标
function getNodeIcon(spaceType: SpaceType) {
  const icons: Record<SpaceType, any> = {
    CAMPUS: markRaw(School),
    BUILDING: markRaw(OfficeBuilding),
    FLOOR: markRaw(Menu),
    ROOM: markRaw(House)
  }
  return icons[spaceType] || OfficeBuilding
}

// ========== 选中状态 ==========
const selectedNode = ref<SpaceDTO | null>(null)

// 计算选中的各级节点
const selectedCampus = computed(() => {
  if (!selectedNode.value) return null
  if (selectedNode.value.spaceType === 'CAMPUS') return selectedNode.value
  return treeData.value.find(c => c.id === selectedNode.value?.campusId) || null
})

const selectedBuilding = computed(() => {
  if (!selectedNode.value) return null
  if (selectedNode.value.spaceType === 'BUILDING') return selectedNode.value
  if (selectedNode.value.spaceType === 'FLOOR' || selectedNode.value.spaceType === 'ROOM') {
    const campus = selectedCampus.value
    if (campus?.children) {
      return campus.children.find(b => b.id === selectedNode.value?.buildingId) || null
    }
  }
  return null
})

const selectedFloor = computed(() => {
  if (!selectedNode.value) return null
  if (selectedNode.value.spaceType === 'FLOOR') return selectedNode.value
  if (selectedNode.value.spaceType === 'ROOM') {
    const building = selectedBuilding.value
    if (building?.children) {
      return building.children.find(f => f.id === selectedNode.value?.parentId) || null
    }
  }
  return null
})

function handleNodeClick(data: SpaceDTO) {
  selectedNode.value = data
}

// ========== 统计数据 ==========
const statistics = computed(() => {
  // 计算全局统计
  let totalBuildings = 0
  let totalRooms = 0
  let totalCapacity = 0
  let totalOccupancy = 0

  function traverse(nodes: SpaceDTO[]) {
    for (const node of nodes) {
      if (node.spaceType === 'BUILDING') totalBuildings++
      if (node.spaceType === 'ROOM') {
        totalRooms++
        totalCapacity += node.capacity || 0
        totalOccupancy += node.currentOccupancy || 0
      }
      if (node.children) traverse(node.children)
    }
  }
  traverse(treeData.value)

  const rate = totalCapacity > 0 ? Math.round(totalOccupancy / totalCapacity * 100) : 0

  return [
    { label: '校区数', value: treeData.value.length, color: 'text-blue-600' },
    { label: '楼栋数', value: totalBuildings, color: 'text-purple-600' },
    { label: '房间数', value: totalRooms, color: 'text-cyan-600' },
    { label: '总容量', value: totalCapacity, color: 'text-green-600' },
    { label: '入住率', value: `${rate}%`, color: rate > 80 ? 'text-red-600' : 'text-amber-600' }
  ]
})

// ========== 新增/编辑 ==========
const formDialogVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const formSpaceType = ref<SpaceType>('CAMPUS')
const formParentId = ref<number | undefined>()
const editData = ref<SpaceDTO | null>(null)

function handleAddCommand(command: string) {
  formMode.value = 'create'
  editData.value = null

  switch (command) {
    case 'campus':
      formSpaceType.value = 'CAMPUS'
      formParentId.value = undefined
      break
    case 'building':
      formSpaceType.value = 'BUILDING'
      formParentId.value = selectedCampus.value?.id
      break
    case 'floor':
      formSpaceType.value = 'FLOOR'
      formParentId.value = selectedBuilding.value?.id
      break
    case 'room':
      formSpaceType.value = 'ROOM'
      formParentId.value = selectedFloor.value?.id
      break
  }

  formDialogVisible.value = true
}

function handleFormSuccess() {
  loadTree()
  ElMessage.success(formMode.value === 'create' ? '创建成功' : '更新成功')
}

// ========== 初始化 ==========
onMounted(() => {
  loadTree()
})
</script>

<style scoped>
.el-tree {
  --el-tree-node-hover-bg-color: #f3f4f6;
}

:deep(.el-tree-node__content) {
  height: auto;
  min-height: 32px;
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: #eff6ff;
}
</style>
