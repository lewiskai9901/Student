<template>
  <div class="flex h-full flex-col">
    <!-- 已配置专业区域 -->
    <div
      class="flex-1 overflow-y-auto rounded-xl border-2 border-dashed p-4 transition-colors"
      :class="isDragOver ? 'border-emerald-400 bg-emerald-50' : 'border-gray-200 bg-white'"
      @dragover="handleDragOver"
      @dragleave="handleDragLeave"
      @drop="handleDrop"
    >
      <!-- 空状态 -->
      <div v-if="groupedMajors.length === 0" class="flex h-full flex-col items-center justify-center">
        <div class="relative">
          <GraduationCap class="h-20 w-20 text-gray-200" />
          <div class="absolute -bottom-1 -right-1 flex h-8 w-8 items-center justify-center rounded-full bg-amber-100">
            <AlertCircle class="h-5 w-5 text-amber-500" />
          </div>
        </div>
        <h3 class="mt-4 text-lg font-medium text-gray-600">该年级尚未配置专业</h3>
        <p class="mt-1 text-sm text-gray-400">从下方专业池拖拽添加，或点击下方按钮快速配置</p>

        <div class="mt-6 flex items-center gap-3">
          <button
            @click="showCopyDialog = true"
            class="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
          >
            <Copy class="h-4 w-4" />
            复制其他年级
          </button>
          <button
            @click="showAddDialog = true"
            class="flex items-center gap-2 rounded-lg bg-emerald-500 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-emerald-600"
          >
            <Plus class="h-4 w-4" />
            添加专业
          </button>
        </div>

        <!-- 拖拽提示 -->
        <div
          v-if="isDragOver"
          class="mt-8 flex items-center gap-2 rounded-lg bg-emerald-100 px-4 py-2 text-emerald-600"
        >
          <ArrowDown class="h-5 w-5 animate-bounce" />
          松开鼠标添加专业
        </div>
      </div>

      <!-- 已配置专业卡片网格 -->
      <div v-else>
        <div class="mb-4 flex items-center justify-between">
          <h3 class="text-sm font-medium text-gray-500">已配置专业</h3>
          <button
            @click="showAddDialog = true"
            class="flex items-center gap-1 rounded-lg bg-emerald-50 px-3 py-1.5 text-sm text-emerald-600 transition-colors hover:bg-emerald-100"
          >
            <Plus class="h-4 w-4" />
            添加专业
          </button>
        </div>

        <div class="grid grid-cols-2 gap-4 xl:grid-cols-3">
          <MajorCard
            v-for="(group, index) in groupedMajors"
            :key="group.majorId"
            :major-name="group.majorName"
            :major-id="group.majorId"
            :directions="group.directions"
            :color-index="index"
            @remove="handleRemoveMajor(group)"
            @remove-direction="handleRemoveDirection"
          />

          <!-- 添加专业卡片占位 -->
          <div
            class="flex min-h-[180px] cursor-pointer items-center justify-center rounded-xl border-2 border-dashed border-gray-300 bg-gray-50 transition-colors hover:border-emerald-400 hover:bg-emerald-50"
            :class="isDragOver ? 'border-emerald-400 bg-emerald-50' : ''"
            @click="showAddDialog = true"
          >
            <div class="text-center">
              <Plus class="mx-auto h-8 w-8 text-gray-400" />
              <p class="mt-2 text-sm text-gray-400">添加专业</p>
            </div>
          </div>
        </div>

        <!-- 拖拽提示遮罩 -->
        <div
          v-if="isDragOver"
          class="mt-4 flex items-center justify-center gap-2 rounded-lg bg-emerald-100 py-3 text-emerald-600"
        >
          <ArrowDown class="h-5 w-5 animate-bounce" />
          松开鼠标添加专业
        </div>
      </div>
    </div>

    <!-- 专业池 -->
    <div class="mt-4">
      <MajorPool
        :majors="availableMajors"
        :configured-major-ids="configuredMajorIds"
        @add="handleAddMajor"
        @drag-start="handlePoolDragStart"
        @drag-end="handlePoolDragEnd"
      />
    </div>

    <!-- 添加专业对话框 -->
    <el-dialog
      v-model="showAddDialog"
      title="添加专业"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="space-y-4">
        <div>
          <label class="mb-2 block text-sm font-medium text-gray-700">选择专业</label>
          <el-select
            v-model="selectedMajorId"
            placeholder="请选择专业"
            style="width: 100%"
            filterable
            @change="handleMajorSelect"
          >
            <el-option
              v-for="major in unConfiguredMajors"
              :key="major.id"
              :label="`${major.majorName} (${major.directions?.length || 0}个方向)`"
              :value="major.id"
            />
          </el-select>
        </div>

        <div v-if="selectedMajorDirections.length > 0">
          <label class="mb-2 block text-sm font-medium text-gray-700">选择要添加的方向</label>
          <div class="max-h-60 space-y-2 overflow-y-auto rounded-lg border border-gray-200 p-3">
            <div
              v-for="direction in selectedMajorDirections"
              :key="direction.id"
              class="flex items-center gap-3 rounded-lg border border-gray-100 p-2 transition-colors hover:bg-gray-50"
            >
              <el-checkbox v-model="selectedDirectionIds" :label="direction.id">
                <span class="text-sm">{{ direction.directionName || direction.level }}</span>
                <span class="ml-2 text-xs text-gray-400">{{ direction.years }}年制</span>
              </el-checkbox>
            </div>
          </div>
          <div class="mt-2 flex items-center justify-between text-xs text-gray-400">
            <span>已选 {{ selectedDirectionIds.length }} 个方向</span>
            <button @click="selectAllDirections" class="text-emerald-600 hover:text-emerald-700">
              {{ selectedDirectionIds.length === selectedMajorDirections.length ? '取消全选' : '全选' }}
            </button>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmAddMajor" :disabled="selectedDirectionIds.length === 0">
          添加 {{ selectedDirectionIds.length }} 个方向
        </el-button>
      </template>
    </el-dialog>

    <!-- 复制配置对话框 -->
    <el-dialog
      v-model="showCopyDialog"
      title="复制其他年级配置"
      width="400px"
    >
      <div class="space-y-4">
        <p class="text-sm text-gray-500">选择要复制配置的源年级，将复制该年级的全部专业方向配置。</p>
        <el-select v-model="copySourceGradeId" placeholder="请选择源年级" style="width: 100%">
          <el-option
            v-for="g in otherGrades"
            :key="g.id"
            :label="g.gradeName"
            :value="g.id"
          />
        </el-select>
      </div>
      <template #footer>
        <el-button @click="showCopyDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmCopyConfig" :disabled="!copySourceGradeId">
          确定复制
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  GraduationCap,
  AlertCircle,
  Copy,
  Plus,
  ArrowDown
} from 'lucide-vue-next'
import MajorCard from './MajorCard.vue'
import MajorPool from './MajorPool.vue'
import {
  addDirectionToYear,
  deleteGradeMajorDirection,
  batchDeleteGradeMajorDirections
} from '@/api/gradeMajorDirection'

interface Props {
  grade: any
  theme: any
  configuredDirections: any[]
  availableMajors: any[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'add-direction': [direction: any]
  'remove-direction': [direction: any]
  'copy-config': [sourceGrade: any]
  refresh: []
}>()

// 状态
const isDragOver = ref(false)
const isPoolDragging = ref(false)
const showAddDialog = ref(false)
const showCopyDialog = ref(false)
const selectedMajorId = ref<number | null>(null)
const selectedDirectionIds = ref<(number | string)[]>([])
const copySourceGradeId = ref<number | null>(null)

// 按专业分组已配置的方向
const groupedMajors = computed(() => {
  const groups = new Map<string, { majorId: string | number; majorName: string; directions: any[] }>()

  for (const direction of props.configuredDirections) {
    const majorId = String(direction.majorId)
    const majorName = direction.majorName || '未知专业'

    if (!groups.has(majorId)) {
      groups.set(majorId, { majorId, majorName, directions: [] })
    }
    groups.get(majorId)!.directions.push(direction)
  }

  return Array.from(groups.values())
})

// 已配置的专业ID列表
const configuredMajorIds = computed(() => {
  return groupedMajors.value.map(g => g.majorId)
})

// 未配置的专业
const unConfiguredMajors = computed(() => {
  return props.availableMajors.filter(
    m => !configuredMajorIds.value.includes(m.id) && !configuredMajorIds.value.includes(String(m.id))
  )
})

// 选中专业的方向列表
const selectedMajorDirections = computed(() => {
  if (!selectedMajorId.value) return []
  const major = props.availableMajors.find(m => m.id === selectedMajorId.value)
  return major?.directions || []
})

// 其他年级（用于复制配置）
const otherGrades = computed(() => {
  // 这里需要从父组件传入其他年级列表
  return []
})

// 处理专业选择
const handleMajorSelect = () => {
  selectedDirectionIds.value = []
}

// 全选/取消全选方向
const selectAllDirections = () => {
  if (selectedDirectionIds.value.length === selectedMajorDirections.value.length) {
    selectedDirectionIds.value = []
  } else {
    selectedDirectionIds.value = selectedMajorDirections.value.map((d: any) => d.id)
  }
}

// 确认添加专业
const confirmAddMajor = async () => {
  if (selectedDirectionIds.value.length === 0) return

  try {
    for (const directionId of selectedDirectionIds.value) {
      await addDirectionToYear({
        academicYear: props.grade.enrollmentYear,
        majorDirectionId: directionId as number,
        remarks: ''
      })
    }

    ElMessage.success(`成功添加 ${selectedDirectionIds.value.length} 个方向`)
    showAddDialog.value = false
    selectedMajorId.value = null
    selectedDirectionIds.value = []
    emit('refresh')
  } catch (error: any) {
    ElMessage.error(error.message || '添加失败')
  }
}

// 确认复制配置
const confirmCopyConfig = async () => {
  if (!copySourceGradeId.value) return
  // TODO: 实现复制配置逻辑
  showCopyDialog.value = false
  copySourceGradeId.value = null
}

// 从专业池拖拽添加
const handleAddMajor = async (major: any) => {
  if (!major.directions || major.directions.length === 0) {
    ElMessage.warning('该专业暂无可用方向')
    return
  }

  try {
    for (const direction of major.directions) {
      await addDirectionToYear({
        academicYear: props.grade.enrollmentYear,
        majorDirectionId: direction.id,
        remarks: ''
      })
    }

    ElMessage.success(`成功添加 ${major.majorName} 的 ${major.directions.length} 个方向`)
    emit('refresh')
  } catch (error: any) {
    ElMessage.error(error.message || '添加失败')
  }
}

// 移除整个专业
const handleRemoveMajor = async (group: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要移除专业【${group.majorName}】的所有方向吗？`,
      '移除确认',
      { type: 'warning' }
    )

    const ids = group.directions.map((d: any) => d.id)
    await batchDeleteGradeMajorDirections(ids)
    ElMessage.success('移除成功')
    emit('refresh')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '移除失败')
    }
  }
}

// 移除单个方向
const handleRemoveDirection = async (direction: any) => {
  try {
    await deleteGradeMajorDirection(direction.id)
    ElMessage.success('移除成功')
    emit('refresh')
  } catch (error: any) {
    ElMessage.error(error.message || '移除失败')
  }
}

// 拖拽相关
const handleDragOver = (e: DragEvent) => {
  e.preventDefault()
  e.dataTransfer!.dropEffect = 'copy'
  isDragOver.value = true
}

const handleDragLeave = () => {
  isDragOver.value = false
}

const handleDrop = async (e: DragEvent) => {
  e.preventDefault()
  isDragOver.value = false

  try {
    const data = e.dataTransfer!.getData('application/json')
    if (data) {
      const major = JSON.parse(data)
      await handleAddMajor(major)
    }
  } catch (error) {
    console.error('拖拽添加失败:', error)
  }
}

const handlePoolDragStart = () => {
  isPoolDragging.value = true
}

const handlePoolDragEnd = () => {
  isPoolDragging.value = false
  isDragOver.value = false
}

// 监听对话框关闭时重置状态
watch(showAddDialog, (val) => {
  if (!val) {
    selectedMajorId.value = null
    selectedDirectionIds.value = []
  }
})
</script>
