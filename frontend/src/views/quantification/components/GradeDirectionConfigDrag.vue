<template>
  <div class="h-full">
    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <Loader2 class="h-6 w-6 animate-spin text-emerald-600" />
      <span class="ml-2 text-gray-500">加载中...</span>
    </div>

    <div v-else class="flex h-full gap-6">
      <!-- 左侧: 专业列表（按专业分组） -->
      <div class="w-1/2 flex flex-col rounded-lg border border-gray-200 bg-white">
        <div class="flex items-center justify-between border-b border-gray-200 bg-gray-50 px-4 py-3">
          <h4 class="flex items-center gap-2 font-semibold text-gray-900">
            <BookOpen class="h-4 w-4 text-gray-600" />
            可选专业
          </h4>
          <div class="relative">
            <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input
              v-model="searchKeyword"
              type="text"
              placeholder="搜索专业..."
              class="h-8 w-44 rounded-lg border border-gray-300 pl-9 pr-3 text-sm focus:border-emerald-500 focus:outline-none"
            />
          </div>
        </div>

        <div class="flex-1 overflow-y-auto p-3">
          <div v-if="filteredMajorGroups.length === 0" class="flex flex-col items-center justify-center py-8 text-gray-400">
            <GraduationCap class="h-10 w-10" />
            <p class="mt-2 text-sm">暂无可选专业</p>
          </div>

          <div v-else class="space-y-4">
            <!-- 按专业分组显示 -->
            <div v-for="group in filteredMajorGroups" :key="group.majorId" class="rounded-lg border border-gray-200">
              <!-- 专业标题 -->
              <div class="flex items-center justify-between bg-gray-50 px-3 py-2 rounded-t-lg border-b border-gray-200">
                <div class="flex items-center gap-2">
                  <div class="flex h-7 w-7 items-center justify-center rounded bg-blue-100">
                    <GraduationCap class="h-4 w-4 text-blue-600" />
                  </div>
                  <span class="font-medium text-gray-900">{{ group.majorName }}</span>
                  <span class="rounded bg-gray-200 px-1.5 py-0.5 text-xs text-gray-600">
                    {{ group.directions.length }}个方向
                  </span>
                </div>
                <button
                  v-if="group.directions.length > 0"
                  @click="handleAddAllDirections(group)"
                  class="flex items-center gap-1 rounded px-2 py-1 text-xs text-emerald-600 hover:bg-emerald-50"
                  title="全部添加"
                >
                  <PlusCircle class="h-3.5 w-3.5" />
                  全部添加
                </button>
              </div>

              <!-- 专业下的方向列表 -->
              <div class="divide-y divide-gray-100">
                <div
                  v-for="direction in group.directions"
                  :key="direction.id"
                  class="group flex items-center justify-between px-3 py-2.5 hover:bg-gray-50"
                >
                  <div class="flex items-center gap-2">
                    <Compass class="h-4 w-4 text-purple-500" />
                    <div>
                      <span class="text-sm font-medium text-gray-800">{{ direction.directionName }}</span>
                      <div class="flex items-center gap-1.5 mt-0.5">
                        <span class="inline-flex items-center gap-0.5 rounded bg-blue-50 px-1 py-0.5 text-xs text-blue-600">
                          <Award class="h-2.5 w-2.5" />
                          {{ getLevelDisplay(direction) }}
                        </span>
                        <span class="inline-flex items-center gap-0.5 rounded bg-amber-50 px-1 py-0.5 text-xs text-amber-600">
                          <Clock class="h-2.5 w-2.5" />
                          {{ getYearsDisplay(direction) }}
                        </span>
                      </div>
                    </div>
                  </div>
                  <button
                    @click="handleAddDirection(direction)"
                    class="flex h-7 w-7 items-center justify-center rounded bg-emerald-50 text-emerald-600 opacity-0 transition-all hover:bg-emerald-100 group-hover:opacity-100"
                    title="添加"
                  >
                    <Plus class="h-4 w-4" />
                  </button>
                </div>
              </div>

              <!-- 空状态 -->
              <div v-if="group.directions.length === 0" class="px-3 py-4 text-center text-sm text-gray-400">
                该专业下暂无可添加的方向
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧: 已分配专业 -->
      <div class="flex-1 flex flex-col rounded-lg border border-gray-200 bg-white">
        <div class="flex items-center justify-between border-b border-gray-200 bg-emerald-50 px-4 py-3">
          <h4 class="flex items-center gap-2 font-semibold text-gray-900">
            <CheckCircle class="h-4 w-4 text-emerald-600" />
            {{ gradeName }} - 已分配专业
            <span class="rounded-full bg-emerald-600 px-2 py-0.5 text-xs font-medium text-white">
              {{ configuredDirections.length }}
            </span>
          </h4>
          <button
            v-if="configuredDirections.length > 0"
            @click="handleClearAll"
            class="flex items-center gap-1 rounded px-2 py-1 text-xs text-red-500 hover:bg-red-50"
          >
            <Trash2 class="h-3.5 w-3.5" />
            清空
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-3">
          <div v-if="configuredDirections.length === 0" class="flex flex-col items-center justify-center py-12 text-gray-400">
            <FolderOpen class="h-12 w-12" />
            <p class="mt-3 text-sm">尚未分配专业</p>
            <p class="text-xs">从左侧选择专业添加到此年级</p>
          </div>

          <div v-else class="space-y-4">
            <!-- 按专业分组显示 -->
            <div v-for="group in configuredMajorGroups" :key="group.majorName" class="rounded-lg border border-emerald-200">
              <!-- 专业标题 -->
              <div class="flex items-center justify-between bg-emerald-50 px-3 py-2 rounded-t-lg border-b border-emerald-200">
                <div class="flex items-center gap-2">
                  <div class="flex h-7 w-7 items-center justify-center rounded bg-emerald-100">
                    <GraduationCap class="h-4 w-4 text-emerald-600" />
                  </div>
                  <span class="font-medium text-gray-900">{{ group.majorName }}</span>
                  <span class="rounded bg-emerald-200 px-1.5 py-0.5 text-xs text-emerald-700">
                    {{ group.directions.length }}个方向
                  </span>
                </div>
              </div>

              <!-- 专业下的方向列表 -->
              <div class="divide-y divide-gray-100">
                <div
                  v-for="item in group.directions"
                  :key="item.id"
                  class="group flex items-center justify-between px-3 py-2.5 hover:bg-gray-50"
                >
                  <div class="flex items-center gap-2">
                    <Compass class="h-4 w-4 text-purple-500" />
                    <div>
                      <span class="text-sm font-medium text-gray-800">{{ item.directionName || '未命名' }}</span>
                      <div class="flex items-center gap-1.5 mt-0.5">
                        <span v-if="item.level" class="inline-flex items-center gap-0.5 rounded bg-blue-50 px-1 py-0.5 text-xs text-blue-600">
                          <Award class="h-2.5 w-2.5" />
                          {{ item.level }}
                        </span>
                        <span v-if="item.years" class="inline-flex items-center gap-0.5 rounded bg-amber-50 px-1 py-0.5 text-xs text-amber-600">
                          <Clock class="h-2.5 w-2.5" />
                          {{ item.years }}年
                        </span>
                      </div>
                    </div>
                  </div>
                  <button
                    @click="handleRemove(item)"
                    class="flex h-7 w-7 items-center justify-center rounded text-gray-400 opacity-0 transition-all hover:bg-red-50 hover:text-red-500 group-hover:opacity-100"
                    title="移除"
                  >
                    <X class="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  BookOpen,
  Search,
  GraduationCap,
  Compass,
  Plus,
  PlusCircle,
  CheckCircle,
  FolderOpen,
  Trash2,
  X,
  Loader2,
  Award,
  Clock
} from 'lucide-vue-next'
import {
  getDirectionsByYear,
  addDirectionToYear,
  deleteGradeMajorDirection,
  batchDeleteGradeMajorDirections
} from '@/api/gradeMajorDirection'
import { getAllEnabledMajors } from '@/api/major'
import { getDirectionsByMajor, type MajorDirection } from '@/api/majorDirection'

interface Props {
  gradeId: number
  gradeName: string
  enrollmentYear: number
}

interface MajorGroup {
  majorId: number
  majorName: string
  directions: MajorDirection[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  refresh: []
}>()

const loading = ref(false)
const searchKeyword = ref('')
const configuredDirections = ref<any[]>([])
const majorGroups = ref<MajorGroup[]>([])

// 获取层次显示文本
const getLevelDisplay = (direction: MajorDirection) => {
  if (direction.isSegmented === 1) {
    return `${direction.phase1Level}→${direction.phase2Level}`
  }
  return direction.level
}

// 获取学制显示文本
const getYearsDisplay = (direction: MajorDirection) => {
  if (direction.isSegmented === 1) {
    return `${direction.phase1Years}+${direction.phase2Years}年`
  }
  return `${direction.years}年`
}

// 过滤后的专业分组（排除已配置的方向）
const filteredMajorGroups = computed(() => {
  const configuredIds = configuredDirections.value.map(d => d.majorDirectionId)

  let groups = majorGroups.value.map(group => ({
    ...group,
    directions: group.directions.filter(d => !configuredIds.includes(d.id))
  }))

  // 关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    groups = groups.map(group => ({
      ...group,
      directions: group.directions.filter(d =>
        d.directionName?.toLowerCase().includes(keyword) ||
        group.majorName.toLowerCase().includes(keyword) ||
        d.level?.toLowerCase().includes(keyword)
      )
    })).filter(group =>
      group.majorName.toLowerCase().includes(keyword) || group.directions.length > 0
    )
  }

  // 只显示有可用方向的专业
  return groups.filter(g => g.directions.length > 0)
})

// 已配置的方向按专业分组
const configuredMajorGroups = computed(() => {
  const groupMap = new Map<string, { majorName: string; directions: any[] }>()

  for (const item of configuredDirections.value) {
    const majorName = item.majorName || '未分类'
    if (!groupMap.has(majorName)) {
      groupMap.set(majorName, { majorName, directions: [] })
    }
    groupMap.get(majorName)!.directions.push(item)
  }

  return Array.from(groupMap.values())
})

// 加载已配置的专业
const loadConfiguredDirections = async () => {
  try {
    const data = await getDirectionsByYear(props.enrollmentYear)
    configuredDirections.value = data || []
  } catch (error) {
    console.error('加载已配置专业失败:', error)
  }
}

// 加载所有可用的专业和方向
const loadMajorGroups = async () => {
  loading.value = true
  try {
    const majors = await getAllEnabledMajors()
    const groups: MajorGroup[] = []

    for (const major of majors) {
      try {
        const directions = await getDirectionsByMajor(major.id!)
        if (directions && directions.length > 0) {
          // 添加专业名称到方向
          directions.forEach((d: MajorDirection) => {
            d.majorName = major.majorName
          })
          groups.push({
            majorId: major.id!,
            majorName: major.majorName,
            directions
          })
        }
      } catch (error) {
        console.error(`加载专业${major.majorName}的方向失败:`, error)
      }
    }

    majorGroups.value = groups
  } catch (error) {
    console.error('加载专业列表失败:', error)
    ElMessage.error('加载专业列表失败')
  } finally {
    loading.value = false
  }
}

// 添加单个方向
const handleAddDirection = async (direction: MajorDirection) => {
  try {
    loading.value = true
    await addDirectionToYear({
      academicYear: props.enrollmentYear,
      majorDirectionId: direction.id!,
      remarks: ''
    })
    ElMessage.success('添加成功')
    await loadConfiguredDirections()
    emit('refresh')
  } catch (error: any) {
    console.error('添加失败:', error)
    ElMessage.error(error.message || '添加失败')
  } finally {
    loading.value = false
  }
}

// 添加专业下所有方向
const handleAddAllDirections = async (group: MajorGroup) => {
  try {
    await ElMessageBox.confirm(
      `确定要将【${group.majorName}】下的所有方向添加到此年级吗？`,
      '批量添加',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    loading.value = true
    for (const direction of group.directions) {
      try {
        await addDirectionToYear({
          academicYear: props.enrollmentYear,
          majorDirectionId: direction.id!,
          remarks: ''
        })
      } catch (error) {
        console.error(`添加${direction.directionName}失败:`, error)
      }
    }
    ElMessage.success(`已添加${group.directions.length}个方向`)
    await loadConfiguredDirections()
    emit('refresh')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('批量添加失败:', error)
      ElMessage.error(error.message || '批量添加失败')
    }
  } finally {
    loading.value = false
  }
}

// 移除专业
const handleRemove = async (item: any) => {
  try {
    loading.value = true
    await deleteGradeMajorDirection(item.id!)
    ElMessage.success('已移除')
    await loadConfiguredDirections()
    emit('refresh')
  } catch (error: any) {
    console.error('移除失败:', error)
    ElMessage.error(error.message || '移除失败')
  } finally {
    loading.value = false
  }
}

// 清空所有
const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要清空此年级的所有专业配置吗？`,
      '清空确认',
      {
        confirmButtonText: '确定清空',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    const ids = configuredDirections.value.map(d => d.id)
    await batchDeleteGradeMajorDirections(ids)
    ElMessage.success('已清空')
    await loadConfiguredDirections()
    emit('refresh')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('清空失败:', error)
      ElMessage.error(error.message || '清空失败')
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadConfiguredDirections()
  loadMajorGroups()
})
</script>
