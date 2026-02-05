<template>
  <div class="h-full flex bg-gray-50">
    <!-- 左侧：空间树面板 -->
    <div class="w-80 flex-shrink-0 bg-white border-r border-gray-200 flex flex-col">
      <!-- 头部 -->
      <div class="p-4 border-b border-gray-100">
        <div class="flex items-center justify-between mb-3">
          <h2 class="text-lg font-semibold text-gray-800">空间管理</h2>
          <el-button type="primary" size="small" @click="handleAddRoot">
            <el-icon class="mr-1"><Plus /></el-icon>
            新增
          </el-button>
        </div>
        <!-- 搜索框 -->
        <el-input
          v-model="searchKeyword"
          placeholder="搜索空间..."
          prefix-icon="Search"
          clearable
          size="small"
        />
      </div>

      <!-- 空间树 -->
      <div class="flex-1 overflow-auto p-3">
        <el-tree
          ref="treeRef"
          :data="treeData"
          :props="treeProps"
          node-key="id"
          :highlight-current="true"
          :expand-on-click-node="false"
          :default-expanded-keys="expandedKeys"
          :filter-node-method="filterNode"
          @node-click="handleNodeClick"
        >
          <template #default="{ node, data }">
            <div class="tree-node">
              <span class="node-name">{{ data.spaceName }}</span>
              <span class="node-type">{{ data.typeName }}</span>
              <span v-if="data.hasCapacity && data.capacity" class="node-capacity">
                {{ data.currentOccupancy || 0 }}/{{ data.capacity }}
              </span>
              <span v-if="data.status !== 1" class="node-status" :class="getStatusDotClass(data.status)" />
            </div>
          </template>
        </el-tree>

        <!-- 空状态 -->
        <div v-if="treeData.length === 0 && !loading" class="text-center py-12 text-gray-400">
          <el-icon class="text-4xl mb-2"><FolderOpened /></el-icon>
          <p>暂无空间数据</p>
          <el-button type="primary" size="small" class="mt-3" @click="handleAddRoot">
            创建第一个空间
          </el-button>
        </div>
      </div>
    </div>

    <!-- 右侧：详情/表单面板 -->
    <div class="flex-1 flex flex-col min-w-0 overflow-hidden">
      <!-- 统计卡片 -->
      <div class="p-4 bg-white border-b border-gray-200">
        <div class="grid grid-cols-4 gap-4">
          <div
            v-for="stat in statisticsCards"
            :key="stat.label"
            class="rounded-xl p-4 transition-all hover:shadow-md cursor-pointer"
            :class="stat.bgClass"
          >
            <div class="flex items-center justify-between">
              <div>
                <div class="text-2xl font-bold" :class="stat.textClass">{{ stat.value }}</div>
                <div class="text-sm text-gray-500 mt-1">{{ stat.label }}</div>
              </div>
              <div class="w-10 h-10 rounded-full flex items-center justify-center" :class="stat.iconBgClass">
                <el-icon class="text-lg" :class="stat.iconClass"><component :is="stat.icon" /></el-icon>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 内容区域 -->
      <div class="flex-1 overflow-auto p-4">
        <!-- 未选中时的欢迎界面 -->
        <div v-if="!selectedNode" class="h-full flex items-center justify-center">
          <div class="text-center">
            <div class="w-24 h-24 mx-auto mb-4 rounded-full bg-blue-50 flex items-center justify-center">
              <el-icon class="text-5xl text-blue-400"><OfficeBuilding /></el-icon>
            </div>
            <h3 class="text-lg font-medium text-gray-700 mb-2">选择一个空间</h3>
            <p class="text-gray-400 text-sm">从左侧树中选择空间查看详情，或点击新增按钮创建空间</p>
          </div>
        </div>

        <!-- 选中空间后显示详情 -->
        <div v-else class="space-y-4">
          <!-- 详情卡片 -->
          <div class="bg-white rounded-xl shadow-sm p-5">
            <div class="flex items-start justify-between mb-4">
              <div>
                <h2 class="text-lg font-semibold text-gray-800">{{ selectedNode.spaceName }}</h2>
                <div class="flex items-center gap-2 mt-1">
                  <el-tag size="small" :type="getStatusType(selectedNode.status)">
                    {{ getStatusLabel(selectedNode.status) }}
                  </el-tag>
                  <span class="text-sm text-gray-400">{{ selectedNode.typeName }}</span>
                  <span class="text-sm text-gray-400">·</span>
                  <span class="text-sm text-gray-400">{{ selectedNode.spaceCode }}</span>
                </div>
              </div>
              <div class="flex gap-2">
                <el-button size="small" @click="handleEdit(selectedNode)">
                  <el-icon class="mr-1"><Edit /></el-icon>编辑
                </el-button>
                <el-button size="small" @click="handleAddChild(selectedNode)" v-if="!selectedNode.leaf">
                  <el-icon class="mr-1"><Plus /></el-icon>添加子空间
                </el-button>
                <el-dropdown @command="handleMoreCommand">
                  <el-button size="small">
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="maintenance" v-if="selectedNode.status === 1">
                        <el-icon class="mr-2"><Tools /></el-icon>设为维护中
                      </el-dropdown-item>
                      <el-dropdown-item command="enable" v-if="selectedNode.status !== 1">
                        <el-icon class="mr-2"><Check /></el-icon>恢复正常
                      </el-dropdown-item>
                      <el-dropdown-item command="disable" v-if="selectedNode.status === 1" divided>
                        <el-icon class="mr-2"><Close /></el-icon>停用
                      </el-dropdown-item>
                      <el-dropdown-item command="delete" divided style="color: #f56c6c">
                        <el-icon class="mr-2"><Delete /></el-icon>删除
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>

            <!-- 基础信息网格 -->
            <div class="grid grid-cols-2 gap-4">
              <!-- 层级路径 -->
              <div class="col-span-2 p-3 bg-gray-50 rounded-lg">
                <div class="text-xs text-gray-400 mb-1">层级路径</div>
                <div class="text-sm text-gray-600">{{ selectedNode.path || selectedNode.spaceName }}</div>
              </div>

              <!-- 描述 -->
              <div v-if="selectedNode.description" class="col-span-2 p-3 bg-gray-50 rounded-lg">
                <div class="text-xs text-gray-400 mb-1">描述</div>
                <div class="text-sm text-gray-600">{{ selectedNode.description }}</div>
              </div>

              <!-- 所属部门 -->
              <div class="p-3 bg-gray-50 rounded-lg">
                <div class="text-xs text-gray-400 mb-1">所属部门</div>
                <div class="text-sm text-gray-700">{{ selectedNode.orgUnitName || '未分配' }}</div>
              </div>

              <!-- 负责人 -->
              <div class="p-3 bg-gray-50 rounded-lg">
                <div class="text-xs text-gray-400 mb-1">负责人</div>
                <div class="text-sm text-gray-700">{{ selectedNode.responsibleUserName || '未指定' }}</div>
              </div>

              <!-- 容量 -->
              <div v-if="selectedNode.hasCapacity" class="p-3 bg-gray-50 rounded-lg">
                <div class="text-xs text-gray-400 mb-1">容量</div>
                <div class="text-sm text-gray-700">
                  <span class="text-lg font-medium">{{ selectedNode.currentOccupancy || 0 }}</span>
                  <span class="text-gray-400">/</span>
                  <span>{{ selectedNode.capacity || '-' }}</span>
                  <span class="text-gray-400 ml-1">{{ selectedNode.capacityUnit || '' }}</span>
                </div>
                <el-progress
                  v-if="selectedNode.capacity"
                  :percentage="Math.round((selectedNode.currentOccupancy || 0) / selectedNode.capacity * 100)"
                  :stroke-width="4"
                  :show-text="false"
                  class="mt-2"
                />
              </div>

              <!-- 特性 -->
              <div class="p-3 bg-gray-50 rounded-lg">
                <div class="text-xs text-gray-400 mb-1">特性</div>
                <div class="flex flex-wrap gap-1">
                  <el-tag v-if="selectedNode.bookable" size="small" type="success">可预订</el-tag>
                  <el-tag v-if="selectedNode.assignable" size="small" type="warning">可分配</el-tag>
                  <el-tag v-if="selectedNode.occupiable" size="small" type="danger">可入住</el-tag>
                  <span v-if="!selectedNode.bookable && !selectedNode.assignable && !selectedNode.occupiable" class="text-sm text-gray-400">无特殊特性</span>
                </div>
              </div>

              <!-- 扩展属性 (如果有) -->
              <div v-if="selectedNode.attributes && Object.keys(selectedNode.attributes).length > 0" class="col-span-2 p-3 bg-gray-50 rounded-lg">
                <div class="text-xs text-gray-400 mb-1">扩展属性</div>
                <div class="grid grid-cols-2 gap-2">
                  <div v-for="(value, key) in selectedNode.attributes" :key="key" class="text-sm">
                    <span class="text-gray-500">{{ key }}:</span>
                    <span class="text-gray-700 ml-1">{{ value }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 子空间列表（紧凑） -->
          <div v-if="childSpaces.length > 0" class="bg-white rounded-xl shadow-sm p-4">
            <div class="flex items-center justify-between mb-3">
              <h3 class="text-sm font-medium text-gray-700">
                子空间 <span class="text-gray-400">({{ childSpaces.length }})</span>
              </h3>
            </div>
            <div class="child-list">
              <div
                v-for="child in childSpaces"
                :key="child.id"
                class="child-item"
                @click="selectSpace(child)"
              >
                <span class="child-name">{{ child.spaceName }}</span>
                <span class="child-type">{{ child.typeName }}</span>
                <span v-if="child.hasCapacity && child.capacity" class="child-capacity">
                  {{ child.currentOccupancy || 0 }}/{{ child.capacity }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑抽屉 -->
    <SpaceFormDrawer
      v-model:visible="formDialogVisible"
      :mode="formMode"
      :parent-space="formParentSpace"
      :edit-data="formEditData"
      :allowed-types="formAllowedTypes"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, markRaw } from 'vue'
import { ElMessage, ElMessageBox, ElTree } from 'element-plus'
import {
  Plus, Search, Edit, Delete, MoreFilled, Tools, Check, Close,
  OfficeBuilding, House, School, Menu, Location, Grid, Box, FolderOpened
} from '@element-plus/icons-vue'
import { universalSpaceApi, type SpaceStatistics } from '@/api/universalSpace'
import { universalSpaceTypeApi } from '@/api/universalSpaceType'
import type { SpaceTreeNode, UniversalSpace, UniversalSpaceType } from '@/types/universalSpace'
import SpaceFormDrawer from './components/SpaceFormDrawer.vue'

// ========== 数据 ==========
const loading = ref(false)
const treeRef = ref<InstanceType<typeof ElTree>>()
const treeData = ref<SpaceTreeNode[]>([])
const expandedKeys = ref<number[]>([])
const searchKeyword = ref('')
const selectedNode = ref<SpaceTreeNode | null>(null)
const childSpaces = ref<UniversalSpace[]>([])
const statistics = ref<SpaceStatistics | null>(null)

const treeProps = {
  children: 'children',
  label: 'spaceName'
}

// ========== 表单相关 ==========
const formDialogVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const formParentSpace = ref<SpaceTreeNode | null>(null)
const formEditData = ref<SpaceTreeNode | null>(null)
const formAllowedTypes = ref<UniversalSpaceType[]>([])

// ========== 统计卡片 ==========
const statisticsCards = computed(() => {
  const stats = statistics.value
  if (!stats) return []

  return [
    {
      label: '空间总数',
      value: stats.totalCount,
      icon: markRaw(Grid),
      bgClass: 'bg-blue-50',
      textClass: 'text-blue-600',
      iconBgClass: 'bg-blue-100',
      iconClass: 'text-blue-500'
    },
    {
      label: '总容量',
      value: stats.totalCapacity,
      icon: markRaw(Box),
      bgClass: 'bg-green-50',
      textClass: 'text-green-600',
      iconBgClass: 'bg-green-100',
      iconClass: 'text-green-500'
    },
    {
      label: '已占用',
      value: stats.totalOccupancy,
      icon: markRaw(House),
      bgClass: 'bg-amber-50',
      textClass: 'text-amber-600',
      iconBgClass: 'bg-amber-100',
      iconClass: 'text-amber-500'
    },
    {
      label: '占用率',
      value: `${stats.occupancyRate?.toFixed(1) || 0}%`,
      icon: markRaw(Location),
      bgClass: 'bg-purple-50',
      textClass: 'text-purple-600',
      iconBgClass: 'bg-purple-100',
      iconClass: 'text-purple-500'
    }
  ]
})

// ========== 加载数据 ==========
async function loadTree() {
  loading.value = true
  try {
    treeData.value = await universalSpaceApi.getTree()
    // 默认展开第一级
    if (treeData.value.length > 0 && expandedKeys.value.length === 0) {
      expandedKeys.value = treeData.value.map(n => n.id)
    }
  } catch (error) {
    console.error('加载空间树失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  try {
    statistics.value = await universalSpaceApi.getStatistics()
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

async function loadChildSpaces(parentId: number) {
  try {
    childSpaces.value = await universalSpaceApi.getChildren(parentId)
  } catch (error) {
    console.error('加载子空间失败:', error)
    childSpaces.value = []
  }
}

// ========== 树操作 ==========
function handleNodeClick(data: SpaceTreeNode) {
  selectedNode.value = data
  loadChildSpaces(data.id)
}

function selectSpace(space: UniversalSpace) {
  // 在树中找到并选中
  const node = findNodeInTree(treeData.value, space.id)
  if (node) {
    selectedNode.value = node
    treeRef.value?.setCurrentKey(space.id)
    loadChildSpaces(space.id)
  }
}

function findNodeInTree(nodes: SpaceTreeNode[], id: number): SpaceTreeNode | null {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children) {
      const found = findNodeInTree(node.children, id)
      if (found) return found
    }
  }
  return null
}

function filterNode(value: string, data: SpaceTreeNode) {
  if (!value) return true
  return data.spaceName.toLowerCase().includes(value.toLowerCase())
}

// ========== 新增/编辑 ==========
async function handleAddRoot() {
  formMode.value = 'create'
  formParentSpace.value = null
  formEditData.value = null
  try {
    formAllowedTypes.value = await universalSpaceApi.getAllowedChildTypesForRoot()
    formDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取可用类型失败')
  }
}

async function handleAddChild(parent: SpaceTreeNode) {
  formMode.value = 'create'
  formParentSpace.value = parent
  formEditData.value = null
  try {
    formAllowedTypes.value = await universalSpaceApi.getAllowedChildTypes(parent.id)
    if (formAllowedTypes.value.length === 0) {
      ElMessage.warning('该空间类型不允许创建子空间')
      return
    }
    formDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取可用类型失败')
  }
}

function handleEdit(space: SpaceTreeNode) {
  formMode.value = 'edit'
  formParentSpace.value = null
  formEditData.value = space
  formAllowedTypes.value = []
  formDialogVisible.value = true
}

function handleFormSuccess() {
  loadTree()
  loadStatistics()
  if (selectedNode.value) {
    loadChildSpaces(selectedNode.value.id)
  }
}

// ========== 更多操作 ==========
async function handleMoreCommand(command: string) {
  if (!selectedNode.value) return

  const space = selectedNode.value

  switch (command) {
    case 'maintenance':
      await changeStatus(space.id, 2)
      break
    case 'enable':
      await changeStatus(space.id, 1)
      break
    case 'disable':
      await changeStatus(space.id, 0)
      break
    case 'delete':
      handleDelete(space)
      break
  }
}

async function changeStatus(id: number, status: number) {
  try {
    await universalSpaceApi.changeStatus(id, status)
    ElMessage.success('状态更新成功')
    loadTree()
    loadStatistics()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

async function handleDelete(space: SpaceTreeNode) {
  try {
    await ElMessageBox.confirm(
      `确定要删除空间"${space.spaceName}"吗？此操作不可恢复。`,
      '删除确认',
      { type: 'warning' }
    )
    await universalSpaceApi.delete(space.id)
    ElMessage.success('删除成功')
    selectedNode.value = null
    childSpaces.value = []
    loadTree()
    loadStatistics()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// ========== 工具函数 ==========
function getTypeIcon(iconName?: string) {
  const iconMap: Record<string, any> = {
    'School': markRaw(School),
    'OfficeBuilding': markRaw(OfficeBuilding),
    'House': markRaw(House),
    'Menu': markRaw(Menu),
    'Location': markRaw(Location),
    'Grid': markRaw(Grid),
    'Box': markRaw(Box),
    'MapPin': markRaw(Location)
  }
  return iconMap[iconName || ''] || OfficeBuilding
}

function getTypeColorClass(typeCode: string) {
  const colorMap: Record<string, string> = {
    'SITE': 'bg-blue-500',
    'BUILDING': 'bg-purple-500',
    'FLOOR': 'bg-cyan-500',
    'ROOM': 'bg-green-500',
    'AREA': 'bg-amber-500',
    'STATION': 'bg-rose-500'
  }
  return colorMap[typeCode] || 'bg-gray-500'
}

function getStatusDotClass(status: number) {
  switch (status) {
    case 0: return 'bg-gray-400'
    case 2: return 'bg-amber-400'
    default: return 'bg-green-400'
  }
}

function getStatusType(status: number) {
  switch (status) {
    case 0: return 'info'
    case 2: return 'warning'
    default: return 'success'
  }
}

function getStatusLabel(status: number) {
  switch (status) {
    case 0: return '停用'
    case 2: return '维护中'
    default: return '正常'
  }
}

// ========== 监听搜索 ==========
watch(searchKeyword, (val) => {
  treeRef.value?.filter(val)
})

// ========== 初始化 ==========
onMounted(() => {
  loadTree()
  loadStatistics()
})
</script>

<style scoped>
.el-tree {
  --el-tree-node-hover-bg-color: #f8fafc;
}

:deep(.el-tree-node__content) {
  height: auto;
  min-height: 32px;
  padding: 2px 8px;
  border-radius: 4px;
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: #eff6ff;
}

/* 树节点样式 */
.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  padding: 2px 0;
}

.node-name {
  color: #374151;
  font-weight: 500;
}

.node-type {
  color: #9ca3af;
  font-size: 11px;
}

.node-capacity {
  color: #6b7280;
  font-size: 11px;
  padding: 1px 4px;
  background: #f3f4f6;
  border-radius: 3px;
}

.node-status {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* 紧凑子空间列表 */
.child-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.child-item {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  font-size: 12px;
}

.child-item:hover {
  border-color: #93c5fd;
  background: #eff6ff;
}

.child-name {
  color: #374151;
  margin-right: 4px;
}

.child-type {
  color: #9ca3af;
  font-size: 11px;
}

.child-capacity {
  margin-left: 4px;
  padding: 1px 4px;
  background: #f3f4f6;
  border-radius: 3px;
  font-size: 10px;
  color: #6b7280;
}
</style>
