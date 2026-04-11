<script setup lang="ts">
/**
 * 位置选择器组件 - 关联场所管理系统
 * 使用级联选择器，支持 校区 → 楼栋 → 楼层 → 房间
 */
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Location } from '@element-plus/icons-vue'
import { universalPlaceApi } from '@/api/universalPlace'
import type { PlaceTreeNode } from '@/types/universalPlace'
import type { RoomType } from '@/types/place'

// 使用新的空间树节点类型
type PlaceDTO = PlaceTreeNode

const props = defineProps<{
  locationType: string
  locationId?: number
  locationName?: string
}>()

const emit = defineEmits<{
  'update:locationType': [value: string]
  'update:locationId': [value: number | undefined]
  'update:locationName': [value: string]
}>()

// 加载状态
const loading = ref(false)
const placeTree = ref<PlaceDTO[]>([])

// 房间类型选项
const roomTypeOptions: { value: RoomType; label: string }[] = [
  { value: 'CLASSROOM', label: '教室' },
  { value: 'DORMITORY', label: '宿舍' },
  { value: 'OFFICE', label: '办公室' },
  { value: 'LAB', label: '实验室' },
  { value: 'MEETING', label: '会议室' },
  { value: 'STORAGE', label: '仓库' },
  { value: 'OTHER', label: '其他' }
]

// 级联选择的值
const cascaderValue = ref<number[]>([])

// 级联选择器配置
const cascaderProps = {
  value: 'id',
  label: 'placeName',
  children: 'children',
  emitPath: true,
  checkStrictly: false // 只能选择叶子节点（房间）
}

// 加载场所树
async function loadPlaceTree() {
  loading.value = true
  try {
    const data = await universalPlaceApi.getTree()
    placeTree.value = data || []
  } catch (error) {
    console.error('加载场所树失败:', error)
    ElMessage.error('加载位置数据失败')
  } finally {
    loading.value = false
  }
}

// 根据ID查找完整路径
function findPathById(tree: PlaceDTO[], targetId: number, path: number[] = []): number[] | null {
  for (const node of tree) {
    const currentPath = [...path, node.id]
    if (node.id === targetId) {
      return currentPath
    }
    if (node.children?.length) {
      const result = findPathById(node.children, targetId, currentPath)
      if (result) return result
    }
  }
  return null
}

// 根据ID查找节点
function findNodeById(tree: PlaceDTO[], targetId: number): PlaceDTO | null {
  for (const node of tree) {
    if (node.id === targetId) return node
    if (node.children?.length) {
      const result = findNodeById(node.children, targetId)
      if (result) return result
    }
  }
  return null
}

// 构建位置名称
function buildLocationName(path: number[]): string {
  const names: string[] = []
  let currentNodes = placeTree.value

  for (const id of path) {
    const node = currentNodes.find(n => n.id === id)
    if (node) {
      // 跳过校区和楼层，取楼栋和房间名称
      if (node.typeCode && !['CAMPUS', 'FLOOR'].includes(node.typeCode.toUpperCase())) {
        names.push(node.placeName)
      }
      currentNodes = node.children || []
    }
  }

  return names.join(' - ')
}

// 处理类型变化
function handleTypeChange(value: string) {
  emit('update:locationType', value)
  emit('update:locationId', undefined)
  emit('update:locationName', '')
  cascaderValue.value = []
}

// 根据楼栋类型推断房间类型
function inferRoomTypeFromBuilding(path: number[]): RoomType | null {
  // 查找路径中的楼栋节点
  let currentNodes = placeTree.value
  for (const id of path) {
    const node = currentNodes.find(n => n.id === id)
    if (node) {
      if (node.typeCode) {
        const tc = node.typeCode.toUpperCase()
        // 根据类型代码推断房间类型
        if (tc.includes('DORM')) return 'DORMITORY'
        if (tc.includes('TEACH') || tc.includes('CLASS')) return 'CLASSROOM'
        if (tc.includes('OFFICE')) return 'OFFICE'
        if (tc.includes('LAB')) return 'LAB'
      }
      currentNodes = node.children || []
    }
  }
  return null
}

// 处理级联选择变化
function handleCascaderChange(value: number[]) {
  if (value && value.length > 0) {
    const selectedId = value[value.length - 1]
    const selectedNode = findNodeById(placeTree.value, selectedId)

    if (selectedNode) {
      emit('update:locationId', selectedId)
      emit('update:locationName', buildLocationName(value))

      // 根据选中的场所类型自动更新 locationType
      // 优先从 typeCode 推断，其次从路径中的父节点推断
      const inferredFromNode = inferRoomTypeFromBuilding([selectedId])
      if (inferredFromNode) {
        emit('update:locationType', inferredFromNode)
      } else {
        const inferredType = inferRoomTypeFromBuilding(value)
        if (inferredType) {
          emit('update:locationType', inferredType)
        }
      }
    }
  } else {
    emit('update:locationId', undefined)
    emit('update:locationName', '')
  }
}

// 处理手动输入
function handleManualInput(value: string) {
  emit('update:locationName', value)
  emit('update:locationId', undefined)
}

// 初始化时如果有值，恢复选中状态
watch(() => props.locationId, (newId) => {
  if (newId && placeTree.value.length > 0) {
    const path = findPathById(placeTree.value, newId)
    if (path) {
      cascaderValue.value = path
    }
  }
}, { immediate: true })

// 监听placeTree加载完成后恢复选中状态
watch(() => placeTree.value, (newTree) => {
  if (props.locationId && newTree.length > 0) {
    const path = findPathById(newTree, props.locationId)
    if (path) {
      cascaderValue.value = path
    }
  }
})

onMounted(() => {
  loadPlaceTree()
})
</script>

<template>
  <div class="location-selector">
    <!-- 位置类型 + 级联选择器 -->
    <el-form-item label="存放位置">
      <div class="location-row">
        <!-- 类型选择 -->
        <el-select
          :model-value="locationType"
          placeholder="类型"
          class="type-select"
          clearable
          @change="handleTypeChange"
        >
          <el-option
            v-for="item in roomTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>

        <!-- 级联选择器 -->
        <el-cascader
          v-model="cascaderValue"
          :options="placeTree"
          :props="cascaderProps"
          :show-all-levels="false"
          placeholder="选择具体位置（校区/楼栋/楼层/房间）"
          filterable
          clearable
          class="location-cascader"
          :loading="loading"
          @change="handleCascaderChange"
        >
          <template #default="{ node, data }">
            <div class="flex items-center gap-1.5">
              <span class="text-xs text-gray-400 w-8">
                {{ data.typeName || data.typeCode || '' }}
              </span>
              <span>{{ data.placeName }}</span>
              <span v-if="data.placeCode" class="text-gray-400">({{ data.placeCode }})</span>
            </div>
          </template>
        </el-cascader>
      </div>
    </el-form-item>

    <!-- 手动输入备选 -->
    <el-form-item v-if="!cascaderValue.length && locationType" label="">
      <div class="flex items-center gap-2 text-sm text-gray-500">
        <el-icon><Location /></el-icon>
        <span>找不到对应位置？</span>
        <el-input
          :model-value="locationName"
          placeholder="手动输入位置描述"
          size="small"
          class="flex-1 max-w-xs"
          @input="handleManualInput"
        />
      </div>
    </el-form-item>
  </div>
</template>

<style scoped>
.location-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.type-select {
  width: 100px !important;
  flex-shrink: 0;
}

.type-select :deep(.el-select__wrapper) {
  width: 100px;
}

.location-cascader {
  flex: 1;
  min-width: 0;
}

.location-selector :deep(.el-cascader) {
  width: 100%;
}

.location-selector :deep(.el-cascader-menu) {
  min-width: 180px;
}
</style>
