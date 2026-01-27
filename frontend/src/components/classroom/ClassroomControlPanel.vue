<template>
  <Transition name="drawer">
    <div v-if="visible" class="fixed inset-0 z-50 flex justify-end">
      <!-- Backdrop -->
      <div class="fixed inset-0 bg-black/30" @click="handleClose"></div>

      <!-- Panel -->
      <div class="relative z-10 flex h-full w-[420px] flex-col bg-white shadow-2xl">
        <!-- Header -->
        <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-cyan-100">
              <School class="h-5 w-5 text-cyan-600" />
            </div>
            <div>
              <h3 class="text-lg font-semibold text-gray-900">教室详情</h3>
              <p class="text-xs text-gray-500">{{ classroom?.classroomName || '加载中...' }}</p>
            </div>
          </div>
          <button @click="handleClose" class="rounded-lg p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
            <X class="h-5 w-5" />
          </button>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="flex flex-1 items-center justify-center">
          <Loader2 class="h-8 w-8 animate-spin text-cyan-500" />
        </div>

        <!-- Content -->
        <div v-else-if="classroom" class="flex-1 overflow-y-auto">
          <!-- Status Banner -->
          <div
            class="flex items-center gap-2 px-6 py-3"
            :class="classroom.status === 1 ? 'bg-green-50' : 'bg-gray-100'"
          >
            <div
              class="h-2 w-2 rounded-full"
              :class="classroom.status === 1 ? 'bg-green-500' : 'bg-gray-400'"
            ></div>
            <span
              class="text-sm font-medium"
              :class="classroom.status === 1 ? 'text-green-700' : 'text-gray-600'"
            >
              {{ classroom.status === 1 ? '启用中' : '已停用' }}
            </span>
          </div>

          <!-- Basic Info -->
          <div class="border-b border-gray-100 px-6 py-4">
            <h4 class="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-900">
              <Info class="h-4 w-4" />
              基本信息
            </h4>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-xs text-gray-500">教室编号</label>
                <span class="mt-0.5 block rounded-md bg-gray-100 px-2 py-1 font-mono text-sm">
                  {{ classroom.classroomCode }}
                </span>
              </div>
              <div>
                <label class="block text-xs text-gray-500">房间号</label>
                <span class="mt-0.5 block text-sm font-medium text-gray-700">{{ classroom.roomNumber }}</span>
              </div>
              <div>
                <label class="block text-xs text-gray-500">楼层</label>
                <span class="mt-0.5 block text-sm font-medium text-gray-700">{{ classroom.floor }}层</span>
              </div>
              <div>
                <label class="block text-xs text-gray-500">教室类型</label>
                <span
                  class="mt-0.5 inline-block rounded-full px-2 py-0.5 text-xs font-medium"
                  :class="getTypeBadgeClass(classroom.classroomType)"
                >
                  {{ classroom.classroomType }}
                </span>
              </div>
              <div>
                <label class="block text-xs text-gray-500">所属教学楼</label>
                <span class="mt-0.5 block text-sm font-medium text-gray-700">{{ classroom.buildingName }}</span>
              </div>
              <div>
                <label class="block text-xs text-gray-500">容量</label>
                <span class="mt-0.5 block text-sm font-medium text-gray-700">{{ classroom.capacity }}人</span>
              </div>
            </div>
          </div>

          <!-- Capacity Usage -->
          <div class="border-b border-gray-100 px-6 py-4">
            <h4 class="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-900">
              <Users class="h-4 w-4" />
              使用情况
            </h4>
            <div class="rounded-lg bg-gray-50 p-4">
              <div class="flex items-center justify-between text-sm">
                <span class="text-gray-600">当前人数</span>
                <span class="font-bold" :class="usageTextClass">
                  {{ classroom.studentCount || 0 }} / {{ classroom.capacity }}
                </span>
              </div>
              <div class="mt-2 h-2 overflow-hidden rounded-full bg-gray-200">
                <div
                  class="h-full rounded-full transition-all"
                  :class="usageBarClass"
                  :style="{ width: `${usageRate}%` }"
                ></div>
              </div>
              <div class="mt-1 text-right text-xs text-gray-500">{{ usageRate }}% 使用率</div>
            </div>
          </div>

          <!-- Class Assignment -->
          <div class="border-b border-gray-100 px-6 py-4">
            <h4 class="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-900">
              <GraduationCap class="h-4 w-4" />
              关联班级
            </h4>
            <div v-if="classroom.classId" class="rounded-lg border border-cyan-200 bg-cyan-50 p-3">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-cyan-100">
                    <GraduationCap class="h-4 w-4 text-cyan-600" />
                  </div>
                  <div>
                    <span class="block text-sm font-medium text-gray-900">{{ classroom.className }}</span>
                    <span class="text-xs text-gray-500">班主任: {{ classroom.headTeacherName || '未指定' }}</span>
                  </div>
                </div>
                <button
                  @click="handleUnassignClass"
                  class="rounded-lg p-1.5 text-gray-400 hover:bg-white hover:text-red-500"
                  title="取消关联"
                >
                  <Unlink class="h-4 w-4" />
                </button>
              </div>
            </div>
            <div v-else class="rounded-lg border-2 border-dashed border-gray-200 p-4 text-center">
              <p class="text-sm text-gray-500">暂未关联班级</p>
              <button
                @click="handleAssignClass"
                class="mt-2 inline-flex items-center gap-1 rounded-lg bg-cyan-50 px-3 py-1.5 text-xs font-medium text-cyan-600 hover:bg-cyan-100"
              >
                <Link2 class="h-3 w-3" />
                关联班级
              </button>
            </div>
          </div>

          <!-- Facilities -->
          <div class="border-b border-gray-100 px-6 py-4">
            <h4 class="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-900">
              <Tv class="h-4 w-4" />
              设施设备
            </h4>
            <p v-if="classroom.facilities" class="text-sm text-gray-600 leading-relaxed">
              {{ classroom.facilities }}
            </p>
            <p v-else class="text-sm text-gray-400">暂无设施信息</p>
          </div>

          <!-- Related Assets -->
          <div class="border-b border-gray-100 px-6 py-4">
            <h4 class="mb-3 flex items-center justify-between text-sm font-semibold text-gray-900">
              <span class="flex items-center gap-2">
                <Package class="h-4 w-4" />
                关联资产
              </span>
              <button
                @click="goToAssetManagement"
                class="text-xs font-normal text-cyan-600 hover:underline"
              >
                前往资产管理 →
              </button>
            </h4>
            <div v-if="assetsLoading" class="flex justify-center py-4">
              <Loader2 class="h-5 w-5 animate-spin text-gray-400" />
            </div>
            <div v-else-if="relatedAssets.length > 0" class="space-y-2">
              <div
                v-for="asset in relatedAssets.slice(0, 5)"
                :key="asset.id"
                class="flex items-center justify-between rounded-lg bg-gray-50 px-3 py-2"
              >
                <div>
                  <span class="text-sm font-medium text-gray-900">{{ asset.assetName }}</span>
                  <span class="ml-2 text-xs text-gray-500">{{ asset.assetCode }}</span>
                </div>
                <span
                  class="rounded-full px-2 py-0.5 text-xs"
                  :class="getAssetStatusClass(asset.status)"
                >
                  {{ getAssetStatusText(asset.status) }}
                </span>
              </div>
              <p v-if="relatedAssets.length > 5" class="text-center text-xs text-gray-400">
                还有 {{ relatedAssets.length - 5 }} 项资产...
              </p>
            </div>
            <div v-else class="rounded-lg border-2 border-dashed border-gray-200 p-4 text-center">
              <p class="text-sm text-gray-500">暂无关联资产</p>
              <p class="mt-1 text-xs text-gray-400">可在资产管理中将资产调拨至此教室</p>
            </div>
          </div>

          <!-- Timestamps -->
          <div class="px-6 py-4">
            <h4 class="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-900">
              <Clock class="h-4 w-4" />
              时间信息
            </h4>
            <div class="space-y-2 text-sm">
              <div class="flex justify-between">
                <span class="text-gray-500">创建时间</span>
                <span class="text-gray-700">{{ formatTime(classroom.createdAt) }}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-gray-500">更新时间</span>
                <span class="text-gray-700">{{ formatTime(classroom.updatedAt) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Error State -->
        <div v-else class="flex flex-1 flex-col items-center justify-center text-center">
          <AlertCircle class="h-12 w-12 text-gray-300" />
          <p class="mt-2 text-gray-500">加载失败</p>
          <button @click="loadClassroom" class="mt-2 text-sm text-cyan-600 hover:underline">
            重试
          </button>
        </div>

        <!-- Footer Actions -->
        <div class="border-t border-gray-100 px-6 py-4">
          <div class="flex gap-2">
            <button
              @click="handleEdit"
              class="flex flex-1 items-center justify-center gap-2 rounded-lg bg-cyan-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-cyan-700"
            >
              <Pencil class="h-4 w-4" />
              编辑
            </button>
            <button
              @click="handleToggleStatus"
              :disabled="statusUpdating"
              class="flex items-center justify-center gap-2 rounded-lg border border-gray-300 px-4 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              <Loader2 v-if="statusUpdating" class="h-4 w-4 animate-spin" />
              <template v-else>
                <Power class="h-4 w-4" />
                {{ classroom?.status === 1 ? '停用' : '启用' }}
              </template>
            </button>
            <button
              @click="handleDelete"
              :disabled="deleting"
              class="flex items-center justify-center gap-2 rounded-lg border border-red-200 px-4 py-2.5 text-sm font-medium text-red-600 hover:bg-red-50"
            >
              <Loader2 v-if="deleting" class="h-4 w-4 animate-spin" />
              <Trash2 v-else class="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  School,
  X,
  Loader2,
  Info,
  Users,
  GraduationCap,
  Tv,
  Clock,
  Pencil,
  Power,
  Trash2,
  AlertCircle,
  Link2,
  Unlink,
  Package
} from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { assetApi } from '@/api/v2/asset'
import type { Asset } from '@/types/v2/asset'
import { AssetStatus, AssetStatusMap } from '@/types/v2/asset'
import {
  getClassroomById,
  updateClassroom,
  deleteClassroom,
  unassignClassFromClassroom,
  type ClassroomWithDetails
} from '@/api/v2/teaching'

const props = defineProps<{
  classroomId: number | null
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  close: []
  refresh: []
  edit: [classroom: ClassroomWithDetails]
  assignClass: [classroom: ClassroomWithDetails]
}>()

const router = useRouter()
const loading = ref(false)
const statusUpdating = ref(false)
const deleting = ref(false)
const classroom = ref<ClassroomWithDetails | null>(null)

// 关联资产
const assetsLoading = ref(false)
const relatedAssets = ref<Asset[]>([])

// Computed
const usageRate = computed(() => {
  if (!classroom.value) return 0
  const count = classroom.value.studentCount || 0
  const capacity = classroom.value.capacity || 1
  return Math.min(Math.round((count / capacity) * 100), 100)
})

const usageTextClass = computed(() => {
  const rate = usageRate.value
  if (rate >= 100) return 'text-red-600'
  if (rate >= 80) return 'text-amber-600'
  return 'text-gray-900'
})

const usageBarClass = computed(() => {
  const rate = usageRate.value
  if (rate >= 100) return 'bg-red-500'
  if (rate >= 80) return 'bg-amber-500'
  if (rate >= 50) return 'bg-cyan-500'
  return 'bg-gray-300'
})

// Watch for classroomId changes
watch(
  () => props.classroomId,
  (newVal) => {
    if (newVal) {
      loadClassroom()
      loadRelatedAssets()
    }
  },
  { immediate: true }
)

// Methods
const loadClassroom = async () => {
  if (!props.classroomId) return

  loading.value = true
  try {
    classroom.value = await getClassroomById(props.classroomId)
  } catch (error: any) {
    ElMessage.error(error.message || '加载教室详情失败')
    classroom.value = null
  } finally {
    loading.value = false
  }
}

// 加载关联资产
const loadRelatedAssets = async () => {
  if (!props.classroomId) return

  assetsLoading.value = true
  try {
    const res = await assetApi.getAssetsByLocation('classroom', props.classroomId)
    relatedAssets.value = res.data || []
  } catch (error) {
    console.error('Failed to load related assets:', error)
    relatedAssets.value = []
  } finally {
    assetsLoading.value = false
  }
}

// 资产状态样式
const getAssetStatusClass = (status: number) => {
  switch (status) {
    case AssetStatus.IN_USE:
      return 'bg-green-100 text-green-700'
    case AssetStatus.IDLE:
      return 'bg-gray-100 text-gray-700'
    case AssetStatus.REPAIRING:
      return 'bg-amber-100 text-amber-700'
    case AssetStatus.SCRAPPED:
      return 'bg-red-100 text-red-700'
    default:
      return 'bg-gray-100 text-gray-700'
  }
}

// 资产状态文本
const getAssetStatusText = (status: number) => {
  return AssetStatusMap[status as AssetStatus] || '未知'
}

// 跳转到资产管理
const goToAssetManagement = () => {
  router.push({
    path: '/asset/center',
    query: {
      locationType: 'classroom',
      locationId: props.classroomId?.toString()
    }
  })
}

const getTypeBadgeClass = (type?: string) => {
  const classes: Record<string, string> = {
    '普通教室': 'bg-blue-100 text-blue-700',
    '多媒体教室': 'bg-purple-100 text-purple-700',
    '实验室': 'bg-amber-100 text-amber-700'
  }
  return classes[type || ''] || 'bg-gray-100 text-gray-700'
}

const formatTime = (time?: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handleClose = () => {
  emit('update:visible', false)
  emit('close')
}

const handleEdit = () => {
  if (classroom.value) {
    emit('edit', classroom.value)
    handleClose()
  }
}

const handleToggleStatus = async () => {
  if (!classroom.value) return

  const newStatus = classroom.value.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '停用'

  try {
    await ElMessageBox.confirm(`确定要${action}该教室吗?`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    statusUpdating.value = true
    await updateClassroom(classroom.value.id!, { status: newStatus })
    ElMessage.success(`教室已${action}`)
    classroom.value.status = newStatus
    emit('refresh')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || `${action}失败`)
    }
  } finally {
    statusUpdating.value = false
  }
}

const handleDelete = async () => {
  if (!classroom.value) return

  try {
    await ElMessageBox.confirm(
      `确定要删除教室 "${classroom.value.classroomName}" 吗? 此操作不可撤销。`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )

    deleting.value = true
    await deleteClassroom(classroom.value.id!)
    ElMessage.success('教室删除成功')
    emit('refresh')
    handleClose()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    deleting.value = false
  }
}

const handleAssignClass = () => {
  if (classroom.value) {
    emit('assignClass', classroom.value)
  }
}

const handleUnassignClass = async () => {
  if (!classroom.value) return

  try {
    await ElMessageBox.confirm(
      `确定要取消该教室与班级 "${classroom.value.className}" 的关联吗?`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await unassignClassFromClassroom(classroom.value.id!)
    ElMessage.success('已取消关联')
    await loadClassroom()
    emit('refresh')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}
</script>

<style scoped>
.drawer-enter-active,
.drawer-leave-active {
  transition: all 0.3s ease;
}

.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
}

.drawer-enter-from .relative,
.drawer-leave-to .relative {
  transform: translateX(100%);
}

.drawer-enter-to .relative,
.drawer-leave-from .relative {
  transform: translateX(0);
}
</style>
