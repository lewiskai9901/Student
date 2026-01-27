<template>
  <el-dialog
    v-model="visible"
    title="批量分配"
    width="520px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <!-- 已选房间信息 -->
    <div class="bg-gray-50 rounded-lg p-4 mb-4">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-purple-100 flex items-center justify-center">
            <el-icon class="text-purple-600 text-lg"><OfficeBuilding /></el-icon>
          </div>
          <div>
            <div class="font-medium text-gray-900">已选择 {{ rooms.length }} 间房间</div>
            <div class="text-xs text-gray-500 mt-0.5">{{ roomTypesSummary }}</div>
          </div>
        </div>
        <el-button text size="small" @click="showRoomList = !showRoomList">
          {{ showRoomList ? '收起' : '查看' }}
        </el-button>
      </div>

      <!-- 房间列表展开 -->
      <div v-if="showRoomList" class="mt-3 pt-3 border-t border-gray-200 max-h-24 overflow-auto">
        <div class="flex flex-wrap gap-1.5">
          <el-tag
            v-for="room in rooms"
            :key="room.id"
            size="small"
            :type="room.orgUnitId ? 'success' : 'info'"
          >
            {{ room.spaceName }}
          </el-tag>
        </div>
      </div>
    </div>

    <el-form label-position="top" class="space-y-4">
      <!-- 分配类型选择 -->
      <el-form-item label="分配类型">
        <el-radio-group v-model="allocationType" class="w-full">
          <el-radio-button value="orgUnit" class="flex-1">
            分配给部门
          </el-radio-button>
          <el-radio-button value="class" :disabled="!canAllocateToClass" class="flex-1">
            分配给班级
          </el-radio-button>
        </el-radio-group>
        <div v-if="!canAllocateToClass && allocationType !== 'class'" class="text-xs text-gray-400 mt-1">
          提示：所选房间类型不支持分配给班级
        </div>
      </el-form-item>

      <!-- 部门选择 -->
      <el-form-item v-if="allocationType === 'orgUnit'" label="选择部门">
        <el-select
          v-model="selectedOrgUnitId"
          placeholder="请选择部门（选'无'可取消分配）"
          class="w-full"
          filterable
          clearable
        >
          <el-option label="无（取消分配）" :value="0" />
          <el-option
            v-for="org in orgUnitList"
            :key="org.id"
            :label="org.name"
            :value="org.id"
          />
        </el-select>
      </el-form-item>

      <!-- 班级选择 -->
      <el-form-item v-if="allocationType === 'class'" label="选择班级">
        <el-select
          v-model="selectedClassId"
          placeholder="请选择班级（选'无'可取消分配）"
          class="w-full"
          filterable
          clearable
          :loading="loadingClasses"
        >
          <el-option label="无（取消分配）" :value="0" />
          <el-option
            v-for="cls in classList"
            :key="cls.id"
            :label="cls.name"
            :value="cls.id"
          >
            <div class="flex justify-between items-center">
              <span>{{ cls.name }}</span>
              <span class="text-xs text-gray-400">{{ cls.departmentName || '未设置部门' }}</span>
            </div>
          </el-option>
        </el-select>

        <!-- 班级所属部门提示 -->
        <div v-if="selectedClass" class="text-xs text-gray-500 mt-1">
          所属部门: {{ selectedClass.departmentName || '未设置' }}
        </div>
      </el-form-item>

      <!-- 操作预览 -->
      <div v-if="hasSelection" class="bg-blue-50 rounded-lg p-3 text-sm text-blue-700">
        <template v-if="allocationType === 'orgUnit'">
          <template v-if="selectedOrgUnitId === 0">
            将 <strong>{{ rooms.length }}</strong> 间房间的部门分配取消
          </template>
          <template v-else>
            将 <strong>{{ rooms.length }}</strong> 间房间分配给
            <strong>{{ selectedOrgUnitName }}</strong>
          </template>
        </template>
        <template v-else>
          <template v-if="selectedClassId === 0">
            将 <strong>{{ rooms.length }}</strong> 间房间的班级分配取消
          </template>
          <template v-else>
            将 <strong>{{ rooms.length }}</strong> 间房间分配给
            <strong>{{ selectedClassName }}</strong>
          </template>
        </template>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="!hasSelection"
        @click="handleSubmit"
      >
        确认分配
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { OfficeBuilding } from '@element-plus/icons-vue'
import { batchAssignOrgUnit, batchAssignClass } from '@/api/space'
import { getClassList } from '@/api/organization'
import type { SpaceDTO, BuildingType, RoomType } from '@/types/space'
import { getRoomTypeName, ROOM_TYPE_ALLOCATION_RULES } from '@/types/space'

const props = defineProps<{
  visible: boolean
  rooms: SpaceDTO[]
  buildingType?: BuildingType
  orgUnitList: { id: number; name: string }[]
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'success'): void
}>()

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

// 状态
const allocationType = ref<'orgUnit' | 'class'>('orgUnit')
const selectedOrgUnitId = ref<number | null>(null)
const selectedClassId = ref<number | null>(null)
const showRoomList = ref(false)
const submitting = ref(false)

// 班级列表
const classList = ref<{ id: number; name: string; departmentName?: string }[]>([])
const loadingClasses = ref(false)

// 房间类型统计
const roomTypesSummary = computed(() => {
  const counts: Record<string, number> = {}
  props.rooms.forEach(room => {
    const type = room.roomType || 'OTHER'
    counts[type] = (counts[type] || 0) + 1
  })

  return Object.entries(counts)
    .map(([type, count]) => `${getRoomTypeName(type as RoomType)} ${count}间`)
    .join('、')
})

// 是否可以分配给班级
const canAllocateToClass = computed(() => {
  return props.rooms.every(room => {
    const rule = ROOM_TYPE_ALLOCATION_RULES[room.roomType as RoomType]
    return rule?.needsClass ?? false
  })
})

// 是否有选择
const hasSelection = computed(() => {
  if (allocationType.value === 'orgUnit') {
    return selectedOrgUnitId.value !== null
  }
  return selectedClassId.value !== null
})

// 选中的部门名称
const selectedOrgUnitName = computed(() => {
  if (!selectedOrgUnitId.value) return ''
  const org = props.orgUnitList.find(o => o.id === selectedOrgUnitId.value)
  return org?.name || ''
})

// 选中的班级
const selectedClass = computed(() => {
  if (!selectedClassId.value) return null
  return classList.value.find(c => c.id === selectedClassId.value) || null
})

// 选中的班级名称
const selectedClassName = computed(() => {
  return selectedClass.value?.name || ''
})

// 加载班级列表
async function loadClasses() {
  loadingClasses.value = true
  try {
    const res = await getClassList()
    classList.value = (res || []).map((cls: any) => ({
      id: cls.id,
      name: cls.name,
      departmentName: cls.departmentName
    }))
  } catch (error) {
    console.error('加载班级列表失败:', error)
  } finally {
    loadingClasses.value = false
  }
}

// 提交
async function handleSubmit() {
  if (!hasSelection.value) return

  submitting.value = true
  try {
    const spaceIds = props.rooms.map(r => r.id)

    if (allocationType.value === 'orgUnit') {
      await batchAssignOrgUnit({
        spaceIds,
        orgUnitId: selectedOrgUnitId.value || 0
      })
    } else {
      await batchAssignClass(spaceIds, selectedClassId.value || 0)
    }

    ElMessage.success('分配成功')
    emit('success')
    visible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '分配失败')
  } finally {
    submitting.value = false
  }
}

// 关闭时重置
function handleClose() {
  allocationType.value = 'orgUnit'
  selectedOrgUnitId.value = null
  selectedClassId.value = null
  showRoomList.value = false
}

// 监听对话框打开，加载班级列表
watch(visible, (val) => {
  if (val && classList.value.length === 0) {
    loadClasses()
  }
})
</script>

<style scoped>
:deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
  font-size: 13px;
}

:deep(.el-radio-button__inner) {
  width: 100%;
}

.w-full {
  width: 100%;
}
</style>
