<template>
  <div class="flex h-[calc(100vh-64px)] bg-gray-50">
    <!-- 左侧年级选择器 -->
    <GradeSelector
      :grades="gradeList"
      :selected-id="selectedGradeId"
      :loading="loading"
      :grade-stats="gradeStatsMap"
      @select="handleSelectGrade"
      @add="handleAddGrade"
      @edit="handleEditGrade"
      @delete="handleDeleteGrade"
    />

    <!-- 右侧专业配置面板 -->
    <div class="flex-1 flex flex-col overflow-hidden">
      <!-- 顶部标题栏 -->
      <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
        <div class="flex items-center gap-3">
          <div
            class="flex h-10 w-10 items-center justify-center rounded-xl"
            :class="selectedGradeTheme.bgLight"
          >
            <GraduationCap class="h-5 w-5" :class="selectedGradeTheme.text" />
          </div>
          <div>
            <h1 class="text-xl font-bold text-gray-900">
              {{ selectedGrade?.gradeName || '年级管理' }}
            </h1>
            <p class="text-sm text-gray-500">
              {{ selectedGrade ? `入学年份 ${selectedGrade.enrollmentYear}` : '选择左侧年级查看专业配置' }}
            </p>
          </div>
        </div>
        <div v-if="selectedGrade" class="flex items-center gap-3">
          <div class="flex items-center gap-6 rounded-lg bg-gray-50 px-4 py-2">
            <div class="text-center">
              <div class="text-lg font-bold" :class="selectedGradeTheme.text">
                {{ currentGradeStats.majorCount }}
              </div>
              <div class="text-xs text-gray-500">专业</div>
            </div>
            <div class="h-8 w-px bg-gray-200"></div>
            <div class="text-center">
              <div class="text-lg font-bold" :class="selectedGradeTheme.text">
                {{ currentGradeStats.directionCount }}
              </div>
              <div class="text-xs text-gray-500">方向</div>
            </div>
            <div class="h-8 w-px bg-gray-200"></div>
            <div class="text-center">
              <div class="text-lg font-bold" :class="selectedGradeTheme.text">
                {{ currentGradeStats.classCount }}
              </div>
              <div class="text-xs text-gray-500">班级</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 主内容区 -->
      <div class="flex-1 overflow-hidden p-6">
        <MajorConfigPanel
          v-if="selectedGrade"
          :key="selectedGradeId"
          :grade="selectedGrade"
          :theme="selectedGradeTheme"
          :configured-directions="configuredDirections"
          :available-majors="availableMajors"
          @add-direction="handleAddDirection"
          @remove-direction="handleRemoveDirection"
          @copy-config="handleCopyConfig"
          @refresh="loadGradeConfig"
        />

        <!-- 未选择年级时的空状态 -->
        <div v-else class="flex h-full flex-col items-center justify-center text-gray-400">
          <GraduationCap class="h-16 w-16 text-gray-300" />
          <p class="mt-4 text-lg">请从左侧选择年级</p>
          <p class="text-sm">选择年级后可配置该年级的专业方向</p>
        </div>
      </div>
    </div>

    <!-- 年级表单弹窗 -->
    <GradeForm
      v-model="formVisible"
      :form-data="formData"
      :is-edit="isEdit"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { GraduationCap } from 'lucide-vue-next'
import GradeSelector from './components/GradeSelector.vue'
import MajorConfigPanel from './components/MajorConfigPanel.vue'
import GradeForm from '@/components/grade/GradeForm.vue'
import { getCohortPage, deleteCohort } from '@/api/organization'
import { getDirectionsByYear } from '@/api/gradeMajorDirection'
import { getAllEnabledMajors, getDirectionsByMajor } from '@/api/academic'

// 主题色配置
const themeColors = {
  blue: {
    bg: 'bg-blue-500',
    bgLight: 'bg-blue-100',
    text: 'text-blue-600',
    border: 'border-blue-500',
    ring: 'ring-blue-500',
  },
  violet: {
    bg: 'bg-violet-500',
    bgLight: 'bg-violet-100',
    text: 'text-violet-600',
    border: 'border-violet-500',
    ring: 'ring-violet-500',
  },
  cyan: {
    bg: 'bg-cyan-500',
    bgLight: 'bg-cyan-100',
    text: 'text-cyan-600',
    border: 'border-cyan-500',
    ring: 'ring-cyan-500',
  },
  orange: {
    bg: 'bg-orange-500',
    bgLight: 'bg-orange-100',
    text: 'text-orange-600',
    border: 'border-orange-500',
    ring: 'ring-orange-500',
  },
  gray: {
    bg: 'bg-gray-500',
    bgLight: 'bg-gray-100',
    text: 'text-gray-600',
    border: 'border-gray-500',
    ring: 'ring-gray-500',
  },
}

// 根据年级索引获取主题色
const getGradeTheme = (index: number) => {
  const colors = ['blue', 'violet', 'cyan', 'orange', 'gray'] as const
  return themeColors[colors[index % colors.length]]
}

// 状态
const loading = ref(false)
const gradeList = ref<any[]>([])
const selectedGradeId = ref<string | null>(null)
const configuredDirections = ref<any[]>([])
const availableMajors = ref<any[]>([])
const gradeStatsMap = ref<Map<string, { majorCount: number; directionCount: number; classCount: number }>>(new Map())

// 表单相关
const formVisible = ref(false)
const formData = ref<any>({})
const isEdit = ref(false)

// 计算属性
const selectedGrade = computed(() => {
  return gradeList.value.find(g => String(g.id) === selectedGradeId.value) || null
})

const selectedGradeIndex = computed(() => {
  return gradeList.value.findIndex(g => String(g.id) === selectedGradeId.value)
})

const selectedGradeTheme = computed(() => {
  return getGradeTheme(selectedGradeIndex.value >= 0 ? selectedGradeIndex.value : 0)
})

const currentGradeStats = computed(() => {
  if (!selectedGradeId.value) return { majorCount: 0, directionCount: 0, classCount: 0 }
  return gradeStatsMap.value.get(selectedGradeId.value) || { majorCount: 0, directionCount: 0, classCount: 0 }
})

// 加载年级列表
const loadGrades = async () => {
  loading.value = true
  try {
    const { records } = await getCohortPage({ pageNum: 1, pageSize: 100 })
    // 按入学年份降序排列
    gradeList.value = (records || []).sort((a: any, b: any) => b.enrollmentYear - a.enrollmentYear)

    // 加载每个年级的统计数据
    for (const grade of gradeList.value) {
      loadGradeStats(grade)
    }

    // 默认选中第一个年级
    if (gradeList.value.length > 0 && !selectedGradeId.value) {
      selectedGradeId.value = String(gradeList.value[0].id)
    }
  } catch (error) {
    console.error('加载年级列表失败:', error)
    ElMessage.error('加载年级列表失败')
  } finally {
    loading.value = false
  }
}

// 加载年级统计数据
const loadGradeStats = async (grade: any) => {
  try {
    const directions = await getDirectionsByYear(grade.enrollmentYear)
    const majorSet = new Set((directions || []).map((d: any) => d.majorId))
    gradeStatsMap.value.set(String(grade.id), {
      majorCount: majorSet.size,
      directionCount: (directions || []).length,
      classCount: (directions || []).reduce((sum: number, d: any) => sum + (d.actualClassCount || 0), 0),
    })
  } catch (error) {
    console.error('加载年级统计失败:', error)
  }
}

// 加载年级配置
const loadGradeConfig = async () => {
  if (!selectedGrade.value) return

  try {
    // 加载已配置的专业方向
    const directions = await getDirectionsByYear(selectedGrade.value.enrollmentYear)
    configuredDirections.value = directions || []

    // 更新统计
    await loadGradeStats(selectedGrade.value)
  } catch (error) {
    console.error('加载年级配置失败:', error)
  }
}

// 加载可用专业
const loadAvailableMajors = async () => {
  try {
    const majors = await getAllEnabledMajors()
    const majorsWithDirections = []

    for (const major of majors) {
      try {
        const directions = await getDirectionsByMajor(major.id!)
        if (directions && directions.length > 0) {
          majorsWithDirections.push({
            ...major,
            directions: directions.map((d: any) => ({
              ...d,
              majorName: major.majorName,
            })),
          })
        }
      } catch (error) {
        console.error(`加载专业${major.majorName}的方向失败:`, error)
      }
    }

    availableMajors.value = majorsWithDirections
  } catch (error) {
    console.error('加载可用专业失败:', error)
  }
}

// 选择年级
const handleSelectGrade = (gradeId: string) => {
  selectedGradeId.value = gradeId
}

// 添加年级
const handleAddGrade = () => {
  formData.value = {}
  isEdit.value = false
  formVisible.value = true
}

// 编辑年级
const handleEditGrade = (grade: any) => {
  formData.value = { ...grade }
  isEdit.value = true
  formVisible.value = true
}

// 删除年级
const handleDeleteGrade = async (grade: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除年级【${grade.gradeName}】吗？`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )

    await deleteCohort(grade.id)
    ElMessage.success('删除成功')

    // 如果删除的是当前选中的年级，清空选择
    if (String(grade.id) === selectedGradeId.value) {
      selectedGradeId.value = null
    }

    await loadGrades()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 表单提交成功
const handleFormSuccess = () => {
  loadGrades()
}

// 添加专业方向
const handleAddDirection = async (direction: any) => {
  await loadGradeConfig()
}

// 移除专业方向
const handleRemoveDirection = async (direction: any) => {
  await loadGradeConfig()
}

// 复制其他年级配置
const handleCopyConfig = async (sourceGrade: any) => {
  await loadGradeConfig()
}

// 监听选中年级变化
watch(selectedGradeId, () => {
  if (selectedGradeId.value) {
    loadGradeConfig()
  }
})

onMounted(() => {
  loadGrades()
  loadAvailableMajors()
})
</script>
