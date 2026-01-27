<template>
  <div class="p-6">
    <!-- Loading 状态 -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
    </div>

    <form v-else @submit.prevent="handleSubmit" class="space-y-6">
      <!-- 基本信息 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-medium text-gray-900">基本信息</h3>
        </div>
        <div class="p-4">
          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <!-- 第一行：年级 + 部门（必须先选择） -->
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">
                年级 <span class="text-red-500">*</span>
              </label>
              <select
                v-model="formData.gradeId"
                class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                :class="{ 'border-red-500': errors.gradeId }"
                @change="handleGradeChange"
              >
                <option :value="null">请选择年级</option>
                <option v-for="item in gradeList" :key="item.id" :value="item.id">
                  {{ item.gradeName }}
                </option>
              </select>
              <p v-if="errors.gradeId" class="mt-1 text-xs text-red-500">{{ errors.gradeId }}</p>
            </div>
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">
                所属部门 <span class="text-red-500">*</span>
              </label>
              <select
                v-model="formData.orgUnitId"
                class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                :class="{ 'border-red-500': errors.orgUnitId }"
                @change="handleDepartmentChange"
              >
                <option :value="null">请选择部门</option>
                <option v-for="item in departmentList" :key="item.id" :value="item.id">
                  {{ item.displayName || item.unitName }}
                </option>
              </select>
              <p v-if="errors.orgUnitId" class="mt-1 text-xs text-red-500">{{ errors.orgUnitId }}</p>
            </div>
            <!-- 第二行：专业方向（依赖年级和部门） -->
            <div class="md:col-span-2">
              <label class="mb-1.5 block text-sm font-medium text-gray-700">
                专业方向 <span class="text-red-500">*</span>
                <span v-if="directionLoading" class="ml-2 text-xs text-gray-400">加载中...</span>
              </label>
              <select
                v-model="formData.majorDirectionId"
                class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:bg-gray-50 disabled:text-gray-500"
                :class="{ 'border-red-500': errors.majorDirectionId }"
                :disabled="!formData.gradeId || !formData.orgUnitId || directionLoading"
                @change="handleDirectionChange"
              >
                <option :value="null">{{ directionLoading ? '加载中...' : getDirectionPlaceholder }}</option>
                <option v-for="item in filteredDirectionList" :key="item.majorDirectionId" :value="item.majorDirectionId">
                  {{ item.majorName }} - {{ item.directionName || `${getYearsDisplayForItem(item)}${item.level || ''}` }}
                </option>
              </select>
              <p v-if="errors.majorDirectionId" class="mt-1 text-xs text-red-500">{{ errors.majorDirectionId }}</p>
            </div>
            <!-- 第三行：班级序号 + 班级名称 -->
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">
                班级序号 <span class="text-red-500">*</span>
              </label>
              <input
                v-model.number="formData.classSequence"
                type="number"
                min="1"
                max="99"
                placeholder="自动生成"
                :disabled="!formData.majorDirectionId"
                class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:bg-gray-50 disabled:text-gray-500"
              />
              <p class="mt-1 text-xs text-gray-500">系统将自动分配下一个可用的班级序号</p>
            </div>
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">
                班级名称
              </label>
              <input
                v-model="formData.className"
                type="text"
                placeholder="将根据选择自动生成"
                disabled
                class="h-9 w-full rounded-md border border-gray-300 bg-gray-50 px-3 text-sm text-gray-500"
              />
              <p class="mt-1 text-xs text-gray-500">格式: {{ classNamePreview }}</p>
            </div>
            <!-- 第四行：班级类型 + 状态 -->
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">
                班级类型 <span class="text-red-500">*</span>
              </label>
              <select
                v-model="formData.classType"
                class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              >
                <option :value="1">普通班</option>
                <option :value="2">重点班</option>
                <option :value="3">实验班</option>
              </select>
            </div>
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">
                状态 <span class="text-red-500">*</span>
              </label>
              <div class="flex items-center gap-6 h-9">
                <label class="flex items-center cursor-pointer">
                  <input
                    type="radio"
                    v-model="formData.status"
                    :value="1"
                    class="h-4 w-4 text-blue-600 focus:ring-blue-500"
                  />
                  <span class="ml-2 text-sm text-gray-700">启用</span>
                </label>
                <label class="flex items-center cursor-pointer">
                  <input
                    type="radio"
                    v-model="formData.status"
                    :value="0"
                    class="h-4 w-4 text-blue-600 focus:ring-blue-500"
                  />
                  <span class="ml-2 text-sm text-gray-700">禁用</span>
                </label>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 专业方向详情 -->
      <div v-if="selectedDirection" class="rounded-lg border border-gray-200 bg-white">
        <div class="border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-medium text-gray-900">专业方向信息</h3>
        </div>
        <div class="p-4">
          <div class="grid grid-cols-2 gap-4 md:grid-cols-4">
            <div>
              <span class="text-xs text-gray-500">专业名称</span>
              <p class="mt-0.5 text-sm font-medium text-gray-900">{{ selectedDirection.majorName || '-' }}</p>
            </div>
            <div>
              <span class="text-xs text-gray-500">方向名称</span>
              <p class="mt-0.5 text-sm font-medium text-gray-900">{{ selectedDirection.directionName || '-' }}</p>
            </div>
            <div>
              <span class="text-xs text-gray-500">学制</span>
              <p class="mt-0.5 text-sm font-medium text-gray-900">{{ computedYearsDisplay }}</p>
            </div>
            <div>
              <span class="text-xs text-gray-500">技能等级</span>
              <p class="mt-0.5 text-sm font-medium text-gray-900">{{ computedLevelDisplay }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="flex items-center justify-end gap-3 pt-2">
        <button
          type="button"
          class="h-9 rounded-md border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
          @click="$emit('close')"
        >
          取消
        </button>
        <button
          type="submit"
          :disabled="submitting"
          class="h-9 rounded-md bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          <span v-if="submitting" class="mr-1.5 inline-block h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></span>
          {{ mode === 'add' ? '确定新增' : '确定修改' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getClassDetail,
  createClass,
  updateClass,
  getDepartmentList,
  getAllGrades,
  type Grade
} from '@/api/v2/organization'
import { getDirectionsByYear } from '@/api/v2/gradeMajorDirection'
import type { GradeMajorDirection } from '@/api/v2/gradeMajorDirection'

interface Props {
  mode: 'add' | 'edit'
  classId?: number | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  success: []
  close: []
}>()

const loading = ref(false)
const submitting = ref(false)
const directionLoading = ref(false)  // 专业方向加载状态（避免全表单刷新）
const departmentList = ref<any[]>([])
const gradeList = ref<Grade[]>([])
const directionList = ref<GradeMajorDirection[]>([])
const selectedDirection = ref<GradeMajorDirection | null>(null)

// 扁平化树形结构为列表（用于部门下拉框）
const flattenTree = (nodes: any[], level = 0): any[] => {
  const result: any[] = []
  for (const node of nodes) {
    result.push({
      ...node,
      level,
      displayName: level > 0 ? '　'.repeat(level) + node.unitName : node.unitName
    })
    if (node.children && node.children.length > 0) {
      result.push(...flattenTree(node.children, level + 1))
    }
  }
  return result
}

const formData = reactive({
  gradeId: null as number | null,
  gradeLevel: null as number | null,  // 后端需要的年级等级
  majorId: null as number | null,
  majorDirectionId: null as number | null,
  classSequence: null as number | null,
  className: '',
  classCode: '',  // 后端必填: 班级编码
  orgUnitId: null as number | null,
  enrollmentYear: null as number | null,
  graduationYear: null as number | null,  // 后端必填: 毕业年份
  classType: 1 as number,  // 后端必填: 班级类型 1普通班 2重点班 3实验班
  teacherId: null as number | null,  // 班主任ID
  assistantTeacherId: null as number | null,  // 副班主任ID
  classroomLocation: '',  // 教室位置
  educationSystem: '',
  skillLevel: '',
  duration: null as number | null,
  status: 1  // 后端: 1启用 0禁用
})

const errors = reactive<Record<string, string>>({})

// 计算学制显示（用于列表项）- 需要在其他computed之前定义
const getYearsDisplayForItem = (item: GradeMajorDirection) => {
  if (item.isSegmented === 1 && item.phase1Years && item.phase2Years) {
    return `${item.phase1Years}+${item.phase2Years}`
  }
  return item.years ? `${item.years}` : ''
}

// 计算选中方向的学制显示
const computedYearsDisplay = computed(() => {
  if (!selectedDirection.value) return '-'
  const direction = selectedDirection.value
  if (direction.isSegmented === 1 && direction.phase1Years && direction.phase2Years) {
    return `${direction.phase1Years}+${direction.phase2Years}年`
  }
  return direction.years ? `${direction.years}年` : '-'
})

// 计算选中方向的技能等级显示
const computedLevelDisplay = computed(() => {
  if (!selectedDirection.value) return '-'
  const direction = selectedDirection.value
  if (direction.isSegmented === 1 && direction.phase1Level && direction.phase2Level) {
    return `${direction.phase1Level}→${direction.phase2Level}`
  }
  return direction.level || '-'
})

// 专业方向下拉框占位文字
const getDirectionPlaceholder = computed(() => {
  if (!formData.gradeId && !formData.orgUnitId) {
    return '请先选择年级和部门'
  }
  if (!formData.gradeId) {
    return '请先选择年级'
  }
  if (!formData.orgUnitId) {
    return '请先选择部门'
  }
  if (filteredDirectionList.value.length === 0 && directionList.value.length > 0) {
    return '该部门下暂无可选专业方向'
  }
  return '请选择专业方向'
})

// 根据所选部门过滤专业方向列表
const filteredDirectionList = computed(() => {
  if (!formData.orgUnitId) {
    return directionList.value
  }
  // 过滤出属于所选部门的专业方向
  return directionList.value.filter(item => {
    // 如果专业方向有 orgUnitId，则必须匹配选中的部门
    // 如果没有 orgUnitId，则显示所有（兼容旧数据）
    // 编辑模式下，始终包含当前已选择的专业方向（避免因部门不匹配被过滤掉）
    return !item.orgUnitId || item.orgUnitId === formData.orgUnitId || item.majorDirectionId === formData.majorDirectionId
  })
})

// 班级名称预览
const classNamePreview = computed(() => {
  if (!selectedDirection.value || !formData.classSequence) {
    return '年级名称 + 专业方向名称 + 序号'
  }
  const grade = gradeList.value.find(g => g.id === formData.gradeId)
  const gradeName = grade?.gradeName || ''
  const direction = selectedDirection.value
  const yearsDisplay = getYearsDisplayForItem(direction)
  const level = direction.level || ''
  const majorName = direction.majorName || ''
  return `${gradeName}${yearsDisplay}${level}${majorName}${formData.classSequence}班`
})

// 验证表单
const validateForm = () => {
  // 清空错误
  Object.keys(errors).forEach(key => {
    errors[key] = ''
  })

  let isValid = true

  if (!formData.gradeId) {
    errors.gradeId = '请选择年级'
    isValid = false
  }

  if (!formData.majorDirectionId) {
    errors.majorDirectionId = '请选择专业方向'
    isValid = false
  }

  if (!formData.orgUnitId) {
    errors.orgUnitId = '请选择部门'
    isValid = false
  }

  return isValid
}

// 加载年级列表
const loadGradeList = async () => {
  try {
    const data = await getAllGrades()
    gradeList.value = data || []
  } catch (error) {
    console.error('加载年级列表失败:', error)
  }
}

// 加载部门列表（扁平化树形结构，只显示教学单位）
const loadDepartmentList = async () => {
  try {
    const treeData = await getDepartmentList()
    // 扁平化树形结构
    const flatList = flattenTree(treeData || [])
    // 只保留教学单位（academic/ACADEMIC），职能部门（functional）和行政单位（administrative）不能管理班级
    departmentList.value = flatList.filter(item => {
      const category = (item.unitCategory || '').toLowerCase()
      // 只允许教学单位或未设置类别的（默认当作教学单位）
      return category === 'academic' || category === '' || !item.unitCategory
    })
  } catch (error) {
    console.error('加载部门列表失败:', error)
  }
}

// 部门变更处理 - 切换部门时清空专业方向选择
const handleDepartmentChange = () => {
  // 部门变更后，如果当前选中的专业方向不属于新部门，则清空
  if (formData.majorDirectionId) {
    const currentDirection = directionList.value.find(d => d.majorDirectionId === formData.majorDirectionId)
    if (currentDirection && currentDirection.orgUnitId && currentDirection.orgUnitId !== formData.orgUnitId) {
      // 当前选中的专业方向不属于新选择的部门，清空选择
      formData.majorDirectionId = null
      formData.majorId = null
      formData.classSequence = null
      formData.className = ''
      formData.classCode = ''
      formData.graduationYear = null
      selectedDirection.value = null
    }
  }
}

// 年级变更处理
const handleGradeChange = async () => {
  const gradeId = formData.gradeId

  // 清空专业方向和班级序号
  formData.majorDirectionId = null
  formData.majorId = null
  formData.classSequence = null
  formData.className = ''
  formData.classCode = ''
  formData.gradeLevel = null
  formData.graduationYear = null
  selectedDirection.value = null
  directionList.value = []

  if (!gradeId) return

  // 先找到年级的入学年份
  const grade = gradeList.value.find(g => g.id === gradeId)
  if (!grade) return

  // 自动填充入学年份、年级等级
  formData.enrollmentYear = grade.enrollmentYear
  formData.gradeLevel = grade.gradeLevel

  // 使用入学年份加载该年级的专业方向列表（使用局部loading避免表单抖动）
  try {
    directionLoading.value = true
    const data = await getDirectionsByYear(grade.enrollmentYear)
    directionList.value = data || []
  } catch (error) {
    console.error('加载专业方向列表失败:', error)
    ElMessage.error('加载专业方向列表失败')
  } finally {
    directionLoading.value = false
  }
}

// 专业方向变更处理
const handleDirectionChange = () => {
  const directionId = formData.majorDirectionId

  formData.classSequence = null
  formData.className = ''
  formData.classCode = ''

  if (!directionId) {
    selectedDirection.value = null
    formData.graduationYear = null
    return
  }

  // 找到选中的专业方向
  const direction = directionList.value.find(d => d.majorDirectionId === directionId)
  if (direction) {
    selectedDirection.value = direction
    formData.majorId = direction.majorId || null

    // 计算实际学制年限
    let totalYears: number | null = null
    if (direction.isSegmented === 1 && direction.phase1Years && direction.phase2Years) {
      totalYears = direction.phase1Years + direction.phase2Years
    } else if (direction.years) {
      totalYears = direction.years
    }
    formData.duration = totalYears

    // 保存技能等级和学制信息（用于后端）
    formData.educationSystem = direction.isSegmented === 1
      ? `${direction.phase1Years}+${direction.phase2Years}`
      : (direction.years ? `${direction.years}年制` : '')
    formData.skillLevel = direction.level || ''

    // 自动填充下一个可用的班级序号
    formData.classSequence = (direction.actualClassCount || 0) + 1

    // 计算毕业年份 = 入学年份 + 学制年限
    if (formData.enrollmentYear && totalYears) {
      formData.graduationYear = formData.enrollmentYear + totalYears
    }

    // 生成班级名称和班级编码
    generateClassName()
    generateClassCode()
  }
}

// 生成班级名称
const generateClassName = () => {
  if (!formData.gradeId || !formData.majorDirectionId || !formData.classSequence) {
    formData.className = ''
    return
  }

  const grade = gradeList.value.find(g => g.id === formData.gradeId)
  if (!grade || !selectedDirection.value) {
    formData.className = ''
    return
  }

  const gradeName = grade.gradeName
  const direction = selectedDirection.value
  const majorName = direction.majorName || ''

  // 计算学制显示（如 "3+2" 或 "3"）
  let yearsDisplay = ''
  if (direction.isSegmented === 1 && direction.phase1Years && direction.phase2Years) {
    yearsDisplay = `${direction.phase1Years}+${direction.phase2Years}`
  } else if (direction.years) {
    yearsDisplay = `${direction.years}`
  }

  // 获取技能等级
  const skillLevel = direction.level || ''

  // Format: 年级+学制+技能等级+专业+流水号班
  // 例如: 2025级3+2高级工多媒体制作1班
  formData.className = `${gradeName}${yearsDisplay}${skillLevel}${majorName}${formData.classSequence}班`
}

// 生成班级编码
const generateClassCode = () => {
  if (!formData.enrollmentYear || !formData.classSequence) {
    formData.classCode = ''
    return
  }

  const grade = gradeList.value.find(g => g.id === formData.gradeId)
  const gradeCode = grade?.gradeCode || 'G'
  // 使用方向编码而非专业编码，避免同专业不同方向（如3+2和3+3）编码冲突
  const directionCode = selectedDirection.value?.directionCode || selectedDirection.value?.majorCode || 'M'
  const sequence = String(formData.classSequence).padStart(2, '0')

  // Format: 年份+年级编码+方向编码+序号，如 2024G01CNXXX01
  formData.classCode = `${formData.enrollmentYear}${gradeCode}${directionCode}${sequence}`
}

// 监听班级序号变化,重新生成班级名称和编码
watch(() => formData.classSequence, () => {
  generateClassName()
  generateClassCode()
})

// 加载班级详情
const loadClassDetail = async () => {
  if (props.mode !== 'edit' || !props.classId) return

  loading.value = true
  try {
    const data = await getClassDetail(props.classId)
    formData.gradeId = data.gradeId || null
    formData.gradeLevel = data.gradeLevel || null
    formData.majorId = data.majorId || null
    formData.majorDirectionId = data.majorDirectionId || null
    formData.classSequence = data.classSequence || null
    formData.className = data.className || ''
    formData.classCode = data.classCode || ''
    formData.orgUnitId = data.orgUnitId || null
    formData.enrollmentYear = data.enrollmentYear || null
    formData.graduationYear = data.graduationYear || null
    formData.classType = data.classType || 1
    formData.teacherId = data.teacherId || null
    formData.assistantTeacherId = data.assistantTeacherId || null
    formData.classroomLocation = data.classroomLocation || ''
    formData.educationSystem = data.educationSystem || ''
    formData.skillLevel = data.skillLevel || ''
    formData.duration = data.duration || null
    formData.status = data.status ?? 1

    // 加载年级的专业方向列表
    if (formData.gradeId) {
      const grade = gradeList.value.find(g => g.id === formData.gradeId)
      if (grade && grade.enrollmentYear) {
        const dirData = await getDirectionsByYear(grade.enrollmentYear)
        directionList.value = dirData || []

        // 恢复选中的专业方向
        if (formData.majorDirectionId) {
          const direction = directionList.value.find(d => d.majorDirectionId === formData.majorDirectionId)
          if (direction) {
            selectedDirection.value = direction
          }
        }

        // 如果gradeLevel为空，从年级获取
        if (!formData.gradeLevel) {
          formData.gradeLevel = grade.gradeLevel
        }
      }
    }
  } catch (error: any) {
    console.error('加载班级详情失败:', error)
    ElMessage.error('加载班级详情失败')
  } finally {
    loading.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }

  submitting.value = true

  try {
    // gradeLevel 使用入学年份
    const gradeLevel = formData.enrollmentYear

    // 确保班级编码已生成
    let classCode = formData.classCode
    if (!classCode) {
      const grade = gradeList.value.find(g => g.id === formData.gradeId)
      const gradeCode = grade?.gradeCode || 'G'
      // 使用方向编码而非专业编码
      const directionCode = selectedDirection.value?.directionCode || selectedDirection.value?.majorCode || 'M'
      const sequence = String(formData.classSequence || 1).padStart(2, '0')
      classCode = `${formData.enrollmentYear}${gradeCode}${directionCode}${sequence}`
    }

    // 确保毕业年份已计算
    let graduationYear = formData.graduationYear
    if (!graduationYear && formData.enrollmentYear && formData.duration) {
      graduationYear = formData.enrollmentYear + formData.duration
    }

    // 构建提交数据，符合后端ClassCreateRequest/ClassUpdateRequest要求
    const submitData = {
      className: formData.className || classNamePreview.value,
      classCode: classCode,
      gradeLevel: gradeLevel,
      gradeId: formData.gradeId,
      orgUnitId: formData.orgUnitId,
      majorId: formData.majorId,
      majorDirectionId: formData.majorDirectionId,
      teacherId: formData.teacherId,
      assistantTeacherId: formData.assistantTeacherId,
      classroomLocation: formData.classroomLocation,
      enrollmentYear: formData.enrollmentYear,
      graduationYear: graduationYear,
      classType: formData.classType,
      status: formData.status
    }

    if (props.mode === 'add') {
      await createClass(submitData)
      ElMessage.success('班级创建成功')
    } else if (props.classId) {
      await updateClass(props.classId, submitData)
      ElMessage.success('班级更新成功')
    }

    emit('success')
  } catch (error: any) {
    console.error('操作失败:', error)
    // 优先使用后端返回的业务错误消息
    const errorMsg = error.response?.data?.message || error.message || '操作失败'
    ElMessage.error(errorMsg)
  } finally {
    submitting.value = false
  }
}

// 监听 classId 变化（不使用 immediate，由 onMounted 控制）
watch(
  () => props.classId,
  (newId, oldId) => {
    // 仅在 classId 实际变化时触发（排除初始化）
    if (newId !== oldId && props.mode === 'edit') {
      loadClassDetail()
    }
  }
)

onMounted(async () => {
  // 先加载基础数据
  await Promise.all([loadGradeList(), loadDepartmentList()])

  // 基础数据加载完成后，如果是编辑模式则加载班级详情
  if (props.mode === 'edit' && props.classId) {
    await loadClassDetail()
  }
})
</script>
