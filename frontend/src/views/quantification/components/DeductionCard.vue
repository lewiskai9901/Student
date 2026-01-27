<template>
  <div
    :class="[
      'group relative overflow-hidden rounded-xl border transition-all duration-200',
      isDeducted
        ? 'border-red-300 bg-gradient-to-br from-red-50 to-orange-50 shadow-md shadow-red-100/50'
        : 'border-gray-200 bg-white hover:border-gray-300 hover:shadow-sm'
    ]"
  >
    <!-- 已选中标识 -->
    <div v-if="isDeducted" class="absolute -right-6 -top-6 h-12 w-12 rotate-45 bg-red-500">
      <Check class="absolute bottom-0.5 left-1/2 h-3 w-3 -translate-x-1/2 rotate-[-45deg] text-white" />
    </div>

    <!-- 加权标识 -->
    <div v-if="hasWeight" class="absolute left-2 top-2">
      <div class="flex items-center gap-0.5 rounded-full bg-amber-100 px-1.5 py-0.5 text-[10px] font-medium text-amber-700">
        <Scale class="h-2.5 w-2.5" />
        加权
      </div>
    </div>

    <!-- 卡片内容 -->
    <div class="p-3">
      <!-- 扣分模式标签 + 分数 -->
      <div class="mb-2 flex items-center justify-between">
        <span :class="[
          'rounded-full px-2 py-0.5 text-[10px] font-semibold',
          item.deductMode === 1 ? 'bg-yellow-100 text-yellow-700' :
          item.deductMode === 2 ? 'bg-purple-100 text-purple-700' :
          'bg-blue-100 text-blue-700'
        ]">
          {{ getModeLabel(item.deductMode) }}
        </span>
        <!-- 分数显示 -->
        <div class="flex min-h-[28px] flex-col items-end justify-center">
          <template v-if="isDeducted || currentScore > 0">
            <div class="flex items-baseline gap-0.5">
              <span class="text-lg font-bold text-red-600">-{{ currentScore }}</span>
              <span class="text-[10px] text-red-400">分</span>
            </div>
            <div v-if="hasWeight && weightFactor && weightFactor !== 1" class="flex items-baseline gap-0.5 text-[10px]">
              <span class="text-gray-400">加权</span>
              <span class="font-semibold text-orange-500">-{{ weightedScore }}</span>
            </div>
          </template>
        </div>
      </div>

      <!-- 扣分项名称 -->
      <h4 class="mb-2 text-sm font-semibold text-gray-900 leading-tight line-clamp-2">{{ item.itemName }}</h4>

      <!-- 扣分操作区域 - 统一高度 -->
      <div class="h-[108px]">
        <!-- 扣分模式 1: 固定扣分 -->
        <div v-if="item.deductMode === 1" class="flex h-full flex-col justify-end">
          <div class="flex items-center justify-between rounded-lg bg-gray-50 px-3 py-2">
            <span class="text-xs text-gray-500">固定扣分</span>
            <span class="text-base font-bold text-gray-900">{{ item.fixedScore }} 分</span>
          </div>
        </div>

        <!-- 扣分模式 2: 按人次扣分 -->
        <div v-else-if="item.deductMode === 2" class="flex h-full flex-col justify-between">
          <div class="space-y-1.5">
            <div class="rounded-lg bg-gray-50 p-2">
              <div class="mb-1 text-[10px] text-gray-500">公式</div>
              <div class="flex items-center gap-1 text-xs font-medium text-gray-700">
                <span class="rounded bg-white px-1.5 py-0.5 shadow-sm">{{ item.baseScore || 0 }}</span>
                <span class="text-gray-400">+</span>
                <span class="rounded bg-white px-1.5 py-0.5 shadow-sm">{{ item.scorePerPerson || item.perPersonScore || 0 }}</span>
                <span class="text-gray-400">×</span>
                <span class="text-purple-600">人数</span>
              </div>
            </div>
            <!-- 人数输入 -->
            <div class="flex items-center gap-2">
              <div class="flex flex-1 items-center rounded-lg border border-gray-200 bg-white">
                <button @click="decrementCount" :disabled="localPersonCount <= 0" class="flex h-7 w-7 items-center justify-center text-gray-500 hover:text-gray-900 disabled:opacity-30">
                  <Minus class="h-3 w-3" />
                </button>
                <input v-model.number="localPersonCount" type="number" min="0" class="h-7 w-full flex-1 border-none bg-transparent text-center text-sm font-bold text-gray-900 focus:outline-none" @change="handleCountChange" />
                <button @click="incrementCount" class="flex h-7 w-7 items-center justify-center text-gray-500 hover:text-gray-900">
                  <Plus class="h-3 w-3" />
                </button>
              </div>
              <span class="text-xs text-gray-500">人</span>
            </div>
          </div>
          <!-- 已关联学生显示 -->
          <div class="h-4 flex items-center">
            <div v-if="selectedStudents.length > 0" class="flex gap-1 overflow-hidden">
              <span
                v-for="student in selectedStudents.slice(0, 2)"
                :key="student.id"
                class="rounded-full bg-purple-50 px-1.5 text-[10px] text-purple-700 truncate max-w-[55px]"
                :title="student.studentName"
              >{{ student.studentName }}</span>
              <span v-if="selectedStudents.length > 2" class="rounded-full bg-gray-100 px-1.5 text-[10px] text-gray-500">+{{ selectedStudents.length - 2 }}</span>
            </div>
          </div>
        </div>

        <!-- 扣分模式 3: 区间扣分 -->
        <div v-else-if="item.deductMode === 3" class="flex h-full flex-col justify-end space-y-1.5">
          <div class="rounded-lg bg-gray-50 p-2">
            <div class="mb-1 flex items-center justify-between text-xs">
              <span class="text-gray-500">扣分区间</span>
              <span class="font-semibold text-blue-600">{{ parseRangeMin }} ~ {{ parseRangeMax }}</span>
            </div>
            <input
              v-model.number="localRangeScore"
              type="range"
              :min="parseRangeMin"
              :max="parseRangeMax"
              :step="0.5"
              class="h-1.5 w-full cursor-pointer appearance-none rounded-full bg-gray-200"
              style="accent-color: #3b82f6;"
              @change="handleRangeChange"
            />
          </div>
          <div class="flex items-center gap-2">
            <div class="flex flex-1 items-center rounded-lg border border-gray-200 bg-white">
              <button @click="decrementRange" :disabled="localRangeScore <= parseRangeMin" class="flex h-7 w-7 items-center justify-center text-gray-500 hover:text-gray-900 disabled:opacity-30">
                <Minus class="h-3 w-3" />
              </button>
              <input v-model.number="localRangeScore" type="number" :min="parseRangeMin" :max="parseRangeMax" :step="0.5" class="h-7 w-full flex-1 border-none bg-transparent text-center text-sm font-bold text-gray-900 focus:outline-none" @change="handleRangeChange" />
              <button @click="incrementRange" :disabled="localRangeScore >= parseRangeMax" class="flex h-7 w-7 items-center justify-center text-gray-500 hover:text-gray-900 disabled:opacity-30">
                <Plus class="h-3 w-3" />
              </button>
            </div>
            <span class="text-xs text-gray-500">分</span>
          </div>
        </div>
      </div>

      <!-- 底部操作栏 - 统一放置，固定高度确保对齐 -->
      <div class="mt-2 h-[60px] flex flex-col justify-end border-t border-gray-100 pt-2">
          <!-- 固定扣分模式：确认/取消扣分按钮 - 单独一行 -->
          <button
            v-if="item.deductMode === 1"
            @click="handleToggleFixed"
            :class="[
              'mb-1.5 flex w-full items-center justify-center gap-1.5 rounded-lg py-2.5 text-sm font-semibold transition-all',
              isDeducted
                ? 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                : 'bg-red-500 text-white hover:bg-red-600 shadow-sm shadow-red-500/30'
            ]"
          >
            <component :is="isDeducted ? X : Minus" class="h-4 w-4" />
            {{ isDeducted ? '取消' : '扣分' }}
          </button>
          <!-- 照片、备注、学生按钮 - 同一行 -->
          <div class="flex gap-1.5">
            <!-- 照片按钮 -->
            <div class="flex-1">
              <PhotoUploader
                v-if="item.allowPhoto && (isDeducted || currentScore > 0)"
                v-model="photos"
                :max-count="5"
                :max-size="5"
                button-text="照片"
                button-width="100%"
                dialog-title="上传扣分照片"
                :show-thumbnails="false"
                @change="handlePhotosChange"
              />
              <button
                v-else
                disabled
                class="flex w-full items-center justify-center gap-1 rounded-lg border border-gray-200 bg-gray-50 py-1.5 text-xs text-gray-400 cursor-not-allowed"
                :title="!item.allowPhoto ? '不支持照片' : '请先扣分'"
              >
                <Camera class="h-3 w-3" />
                照片
              </button>
            </div>
            <!-- 备注按钮 -->
            <div class="flex-1">
              <RemarkEditor
                v-if="item.allowRemark && (isDeducted || currentScore > 0)"
                v-model="localRemark"
                button-text="备注"
                button-width="100%"
                dialog-title="添加扣分备注"
                :show-preview="false"
                @change="handleRemarkChange"
              />
              <button
                v-else
                disabled
                class="flex w-full items-center justify-center gap-1 rounded-lg border border-gray-200 bg-gray-50 py-1.5 text-xs text-gray-400 cursor-not-allowed"
                :title="!item.allowRemark ? '不支持备注' : '请先扣分'"
              >
                <MessageSquare class="h-3 w-3" />
                备注
              </button>
            </div>
            <!-- 按人次模式：关联学生按钮 - 在照片备注后面 -->
            <button
              v-if="item.deductMode === 2"
              @click="showStudentDialog = true"
              class="flex flex-1 items-center justify-center gap-1 rounded-lg bg-purple-100 py-1.5 text-xs font-medium text-purple-700 hover:bg-purple-200 transition-colors"
            >
              <Users class="h-3 w-3" />
              学生
              <span v-if="selectedStudents.length > 0" class="rounded bg-purple-500 px-1 text-white text-[10px]">{{ selectedStudents.length }}</span>
            </button>
          </div>
        </div>
    </div>

    <!-- 学生关联对话框 - 重新设计 -->
    <Teleport to="body">
      <div v-if="showStudentDialog" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="showStudentDialog = false"></div>
        <div class="relative w-full max-w-3xl rounded-2xl bg-white shadow-2xl">
          <!-- 头部 -->
          <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
            <div>
              <h3 class="text-lg font-semibold text-gray-900">选择关联学生</h3>
              <p class="text-xs text-gray-500">{{ item.itemName }} · 按人次扣分</p>
            </div>
            <button @click="showStudentDialog = false" class="rounded-lg p-2 hover:bg-gray-100">
              <X class="h-5 w-5 text-gray-500" />
            </button>
          </div>

          <!-- 主内容区 - 左右布局 -->
          <div class="flex h-[420px]">
            <!-- 左侧：学生列表 -->
            <div class="flex-1 flex flex-col border-r border-gray-100">
              <!-- 搜索和操作栏 -->
              <div class="p-4 border-b border-gray-100">
                <div class="flex items-center gap-3">
                  <div class="relative flex-1">
                    <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                    <input
                      v-model="studentSearchKeyword"
                      type="text"
                      placeholder="搜索姓名或学号..."
                      class="w-full rounded-lg border border-gray-200 py-2 pl-9 pr-4 text-sm focus:border-purple-500 focus:outline-none"
                    />
                  </div>
                  <button
                    @click="selectAllStudents"
                    class="rounded-lg border border-gray-200 px-3 py-2 text-xs font-medium text-gray-600 hover:bg-gray-50 whitespace-nowrap"
                  >
                    全选
                  </button>
                </div>
              </div>

              <!-- 学生网格列表 -->
              <div class="flex-1 overflow-y-auto p-4">
                <div v-if="studentsLoading" class="flex items-center justify-center h-full">
                  <div class="h-8 w-8 animate-spin rounded-full border-3 border-purple-600 border-t-transparent"></div>
                </div>
                <div v-else-if="filteredStudents.length === 0" class="flex flex-col items-center justify-center h-full text-gray-400">
                  <Users class="h-12 w-12 mb-2 opacity-50" />
                  <span class="text-sm">暂无学生数据</span>
                </div>
                <div v-else class="grid grid-cols-2 gap-2">
                  <div
                    v-for="student in filteredStudents"
                    :key="student.id"
                    :class="[
                      'flex items-center gap-2 rounded-lg border-2 p-2.5 cursor-pointer transition-all',
                      isStudentSelected(student.id)
                        ? 'border-purple-500 bg-purple-50'
                        : 'border-gray-100 bg-gray-50 hover:border-gray-200 hover:bg-white'
                    ]"
                    @click="toggleStudentSelection(student.id)"
                  >
                    <div :class="[
                      'flex h-8 w-8 items-center justify-center rounded-full text-sm font-semibold',
                      isStudentSelected(student.id)
                        ? 'bg-purple-500 text-white'
                        : 'bg-gray-200 text-gray-600'
                    ]">
                      <Check v-if="isStudentSelected(student.id)" class="h-4 w-4" />
                      <span v-else>{{ student.studentName.charAt(0) }}</span>
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="text-sm font-medium text-gray-900 truncate">{{ student.studentName }}</div>
                      <div class="text-[11px] text-gray-500">{{ student.studentNo }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 右侧：已选学生 -->
            <div class="w-64 flex flex-col bg-gray-50">
              <div class="p-4 border-b border-gray-100">
                <div class="flex items-center justify-between">
                  <span class="text-sm font-medium text-gray-700">已选学生</span>
                  <span class="rounded-full bg-purple-100 px-2 py-0.5 text-xs font-semibold text-purple-700">
                    {{ tempSelectedStudentIds.size }} 人
                  </span>
                </div>
              </div>

              <div class="flex-1 overflow-y-auto p-3">
                <div v-if="tempSelectedStudentIds.size === 0" class="flex flex-col items-center justify-center h-full text-gray-400">
                  <UserPlus class="h-10 w-10 mb-2 opacity-50" />
                  <span class="text-xs">从左侧选择学生</span>
                </div>
                <div v-else class="space-y-2">
                  <div
                    v-for="student in getSelectedStudentsList()"
                    :key="student.id"
                    class="flex items-center gap-2 rounded-lg bg-white p-2 shadow-sm"
                  >
                    <div class="flex h-7 w-7 items-center justify-center rounded-full bg-purple-100 text-xs font-semibold text-purple-700">
                      {{ student.studentName.charAt(0) }}
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="text-sm font-medium text-gray-900 truncate">{{ student.studentName }}</div>
                    </div>
                    <button
                      @click.stop="toggleStudentSelection(student.id)"
                      class="flex h-6 w-6 items-center justify-center rounded-full text-gray-400 hover:bg-red-50 hover:text-red-500"
                    >
                      <X class="h-3.5 w-3.5" />
                    </button>
                  </div>
                </div>
              </div>

              <!-- 清空按钮 -->
              <div v-if="tempSelectedStudentIds.size > 0" class="p-3 border-t border-gray-100">
                <button
                  @click="clearTempSelection"
                  class="w-full rounded-lg border border-gray-200 bg-white py-2 text-xs font-medium text-gray-600 hover:bg-gray-50"
                >
                  清空全部
                </button>
              </div>
            </div>
          </div>

          <!-- 底部操作栏 -->
          <div class="flex items-center justify-between border-t border-gray-200 px-6 py-4">
            <div class="text-sm text-gray-500">
              扣分计算：<span class="font-medium text-gray-900">{{ item.baseScore || 0 }}</span> +
              <span class="font-medium text-purple-600">{{ tempSelectedStudentIds.size }}</span> ×
              <span class="font-medium text-gray-900">{{ item.scorePerPerson || item.perPersonScore || 0 }}</span> =
              <span class="font-bold text-red-600">-{{ (item.baseScore || 0) + tempSelectedStudentIds.size * (item.scorePerPerson || item.perPersonScore || 0) }} 分</span>
            </div>
            <div class="flex gap-3">
              <button
                @click="showStudentDialog = false"
                class="rounded-lg border border-gray-300 px-5 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                取消
              </button>
              <button
                @click="confirmStudentSelection"
                class="rounded-lg bg-purple-600 px-5 py-2 text-sm font-medium text-white hover:bg-purple-700"
              >
                确定选择
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Check, X, Minus, Plus, Scale, Users, Camera, MessageSquare, Search, UserPlus } from 'lucide-vue-next'
import type { DeductionItem } from '@/api/quantification'
import http from '@/utils/request'
import PhotoUploader from './PhotoUploader.vue'
import RemarkEditor from './RemarkEditor.vue'

interface Props {
  item: DeductionItem
  isDeducted: boolean
  deductedData: any
  categoryId: string | number
  classId: string | number
  linkId?: string | number
  linkType?: number
  linkNo?: string
  hasWeight?: boolean
  weightConfigName?: string
  weightFactor?: number
  checkRound?: number  // 检查轮次
}

const props = defineProps<Props>()
const emit = defineEmits<{
  toggle: [detail: any, isAdd: boolean]
  update: [detail: any]
}>()

// 学生接口类型 - 使用 string 存储 ID 避免大数字精度丢失
interface Student {
  id: string
  studentNo: string
  studentName: string
}

// 本地状态
const localPersonCount = ref(0)
const localRangeScore = ref(0)
const localRemark = ref('')
const photos = ref<string[]>([])

// 学生关联状态
const showStudentDialog = ref(false)
const studentsLoading = ref(false)
const studentList = ref<Student[]>([])
const selectedStudents = ref<Student[]>([])
// 使用 string 存储 ID，避免 JavaScript 大数字精度丢失
const tempSelectedStudentIds = ref(new Set<string>())
const studentSearchKeyword = ref('')

// 当前扣分（用于显示）
const currentScore = computed(() => {
  if (props.item.deductMode === 1) {
    return props.isDeducted ? (props.item.fixedScore || 0) : 0
  } else if (props.item.deductMode === 2) {
    const baseScore = props.item.baseScore || 0
    const perPerson = props.item.scorePerPerson || props.item.perPersonScore || 0
    return baseScore + localPersonCount.value * perPerson
  } else if (props.item.deductMode === 3) {
    return localRangeScore.value
  }
  return 0
})

// 获取实际扣分值（用于提交，固定扣分始终返回固定值）
const getActualScore = () => {
  if (props.item.deductMode === 1) {
    return props.item.fixedScore || 0
  } else if (props.item.deductMode === 2) {
    const baseScore = props.item.baseScore || 0
    const perPerson = props.item.scorePerPerson || props.item.perPersonScore || 0
    return baseScore + localPersonCount.value * perPerson
  } else if (props.item.deductMode === 3) {
    return localRangeScore.value
  }
  return 0
}

// 加权后扣分
const weightedScore = computed(() => {
  if (!props.hasWeight || !props.weightFactor) return currentScore.value
  return (currentScore.value * props.weightFactor).toFixed(1)
})

// 解析区间范围
const parseRangeMin = computed(() => {
  if (props.item.minScore !== undefined) return props.item.minScore
  if (props.item.rangeConfig) {
    try {
      const config = typeof props.item.rangeConfig === 'string' ? JSON.parse(props.item.rangeConfig) : props.item.rangeConfig
      if (Array.isArray(config) && config.length > 0 && config[0].min !== undefined) {
        return config[0].min
      }
    } catch (e) {}
  }
  return 0
})

const parseRangeMax = computed(() => {
  if (props.item.maxScore !== undefined) return props.item.maxScore
  if (props.item.rangeConfig) {
    try {
      const config = typeof props.item.rangeConfig === 'string' ? JSON.parse(props.item.rangeConfig) : props.item.rangeConfig
      if (Array.isArray(config) && config.length > 0 && config[0].max !== undefined) {
        return config[0].max
      }
    } catch (e) {}
  }
  return 10
})

// 获取扣分模式标签
const getModeLabel = (mode: number) => {
  const labels: Record<number, string> = {
    1: '固定扣分',
    2: '按人次',
    3: '区间扣分'
  }
  return labels[mode] || '未知'
}

// 同步已有数据
watch(() => props.deductedData, (data) => {
  if (data) {
    if (props.item.deductMode === 2) {
      localPersonCount.value = data.personCount || 0
      // 恢复学生选择状态 - 使用字符串存储 ID
      if (data.studentIds && data.studentNames) {
        const ids = String(data.studentIds).split(',').map(id => id.trim()).filter(id => id)
        const names = String(data.studentNames).split(',')
        selectedStudents.value = ids.map((id, index) => ({
          id: String(id),
          studentNo: '',
          studentName: names[index]?.trim() || ''
        }))
      } else if (data.students && Array.isArray(data.students)) {
        // 从 students 数组恢复
        selectedStudents.value = data.students.map((s: any) => ({
          id: String(s.id || s.studentId),
          studentNo: s.studentNo || '',
          studentName: s.studentName || s.name || ''
        }))
      } else {
        selectedStudents.value = []
      }
    } else if (props.item.deductMode === 3) {
      localRangeScore.value = data.deductScore || 0
    }
    localRemark.value = data.remark || ''
    if (data.photoUrls) {
      try {
        photos.value = typeof data.photoUrls === 'string' ? JSON.parse(data.photoUrls) : data.photoUrls
      } catch (e) {
        photos.value = []
      }
    } else {
      photos.value = []
    }
  } else {
    localPersonCount.value = 0
    localRangeScore.value = 0
    localRemark.value = ''
    photos.value = []
    selectedStudents.value = []
  }
}, { immediate: true, deep: true })

// 过滤学生列表
const filteredStudents = computed(() => {
  if (!studentSearchKeyword.value) return studentList.value
  const keyword = studentSearchKeyword.value.toLowerCase()
  return studentList.value.filter(s =>
    s.studentName.toLowerCase().includes(keyword) ||
    s.studentNo.toLowerCase().includes(keyword)
  )
})

// 加载班级学生列表
const loadClassStudents = async () => {
  if (!props.classId) return
  studentsLoading.value = true
  try {
    const res = await http.get(`/students/by-class/${props.classId}`)
    // 使用 String() 保留完整 ID，避免大数字精度丢失
    // http.ts已解包响应数据
    studentList.value = (res || []).map((s: any) => ({
      id: String(s.id),
      studentNo: s.studentNo,
      studentName: s.studentName || s.realName || s.name
    }))
    // 学生列表加载完成后，初始化选中状态
    initTempSelectedStudents()
  } catch {
    // 加载学生列表失败
    studentList.value = []
  } finally {
    studentsLoading.value = false
  }
}

// 初始化临时选中状态
const initTempSelectedStudents = () => {
  // 从已选中的学生中提取ID，使用字符串比较
  const existingIds = selectedStudents.value.map(s => String(s.id))
  // 创建新的 Set，只保留存在于学生列表中的ID
  const newSet = new Set<string>()
  studentList.value.forEach(s => {
    if (existingIds.includes(String(s.id))) {
      newSet.add(String(s.id))
    }
  })
  tempSelectedStudentIds.value = newSet
}

// 检查学生是否被选中
const isStudentSelected = (studentId: string) => {
  return tempSelectedStudentIds.value.has(String(studentId))
}

// 切换学生选中状态
const toggleStudentSelection = (studentId: string) => {
  const id = String(studentId)
  // 创建新的 Set 以触发 Vue 响应式更新
  const newSet = new Set(tempSelectedStudentIds.value)
  if (newSet.has(id)) {
    newSet.delete(id)
  } else {
    newSet.add(id)
  }
  tempSelectedStudentIds.value = newSet
}

// 清空临时选择
const clearTempSelection = () => {
  tempSelectedStudentIds.value = new Set<string>()
}

// 全选学生
const selectAllStudents = () => {
  const newSet = new Set<string>()
  filteredStudents.value.forEach(s => {
    newSet.add(String(s.id))
  })
  tempSelectedStudentIds.value = newSet
}

// 获取已选学生列表（用于右侧显示）
const getSelectedStudentsList = () => {
  return studentList.value.filter(s => tempSelectedStudentIds.value.has(String(s.id)))
}

// 确认学生选择
const confirmStudentSelection = () => {
  // 确保类型一致，使用字符串比较
  selectedStudents.value = studentList.value.filter(s =>
    tempSelectedStudentIds.value.has(String(s.id))
  )
  localPersonCount.value = selectedStudents.value.length
  showStudentDialog.value = false
  // 触发更新
  if (selectedStudents.value.length > 0) {
    emit('toggle', buildDetail(), true)
  } else if (props.isDeducted) {
    emit('toggle', buildDetail(), false)
  }
}

// 监听学生对话框打开
watch(showStudentDialog, (visible) => {
  if (visible) {
    studentSearchKeyword.value = ''
    // 加载学生列表（加载完成后会自动初始化选中状态）
    loadClassStudents()
  }
})

// 构建扣分详情
const buildDetail = () => ({
  categoryId: props.categoryId,
  deductionItemId: props.item.id,
  deductionItemName: props.item.itemName,
  deductMode: props.item.deductMode,
  classId: props.classId,
  deductScore: getActualScore(),
  personCount: props.item.deductMode === 2 ? localPersonCount.value : undefined,
  studentIds: props.item.deductMode === 2 && selectedStudents.value.length > 0
    ? selectedStudents.value.map(s => s.id).join(',')
    : undefined,
  studentNames: props.item.deductMode === 2 && selectedStudents.value.length > 0
    ? selectedStudents.value.map(s => s.studentName).join(',')
    : undefined,
  linkType: props.linkType || 0,
  linkId: props.linkId || 0,
  dormitoryId: props.linkType === 1 ? props.linkId : undefined,
  dormitoryNo: props.linkType === 1 ? props.linkNo : undefined,
  classroomId: props.linkType === 2 ? props.linkId : undefined,
  classroomNo: props.linkType === 2 ? props.linkNo : undefined,
  photoUrls: photos.value.length > 0 ? JSON.stringify(photos.value) : null,
  remark: localRemark.value || null,
  checkRound: props.checkRound || 1,  // 检查轮次
  // 传递扣分项参数，用于父组件计算单人扣分
  baseScore: props.item.baseScore || 0,
  perPersonScore: props.item.scorePerPerson || props.item.perPersonScore || 0
})

// 固定扣分切换
const handleToggleFixed = () => {
  emit('toggle', buildDetail(), !props.isDeducted)
}

// 人次变化
const incrementCount = () => {
  localPersonCount.value++
  handleCountChange()
}

const decrementCount = () => {
  if (localPersonCount.value > 0) {
    localPersonCount.value--
    handleCountChange()
  }
}

const handleCountChange = () => {
  if (localPersonCount.value <= 0) {
    localPersonCount.value = 0
    if (props.isDeducted) {
      emit('toggle', buildDetail(), false)
    }
  } else {
    emit('toggle', buildDetail(), true)
  }
}

// 区间变化
const incrementRange = () => {
  if (localRangeScore.value < parseRangeMax.value) {
    localRangeScore.value = Math.min(localRangeScore.value + 0.5, parseRangeMax.value)
    handleRangeChange()
  }
}

const decrementRange = () => {
  if (localRangeScore.value > parseRangeMin.value) {
    localRangeScore.value = Math.max(localRangeScore.value - 0.5, parseRangeMin.value)
    handleRangeChange()
  }
}

const handleRangeChange = () => {
  if (localRangeScore.value <= 0) {
    if (props.isDeducted) {
      emit('toggle', buildDetail(), false)
    }
  } else {
    emit('toggle', buildDetail(), true)
  }
}

// 照片变更处理
const handlePhotosChange = (newPhotos: string[]) => {
  photos.value = newPhotos
  if (props.isDeducted || currentScore.value > 0) {
    emit('update', buildDetail())
  }
}

// 备注变更处理
const handleRemarkChange = (newRemark: string) => {
  localRemark.value = newRemark
  if (props.isDeducted || currentScore.value > 0) {
    emit('update', buildDetail())
  }
}
</script>

<style scoped>
input[type="number"]::-webkit-inner-spin-button,
input[type="number"]::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
input[type="number"] {
  -moz-appearance: textfield;
}

input[type="range"] {
  -webkit-appearance: none;
  height: 8px;
  border-radius: 4px;
  background: #e5e7eb;
}
input[type="range"]::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #3b82f6;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.4);
}
</style>
