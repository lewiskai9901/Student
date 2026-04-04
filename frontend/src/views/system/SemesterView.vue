<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard
        title="学期总数"
        :value="total"
        :icon="Calendar"
        subtitle="全部学期"
        color="blue"
      />
      <StatCard
        title="当前学期"
        :value="currentSemesterName"
        :icon="CalendarCheck"
        subtitle="进行中"
        color="emerald"
      />
      <StatCard
        title="正常学期"
        :value="activeSemesterCount"
        :icon="CalendarDays"
        subtitle="可用学期"
        color="purple"
      />
      <StatCard
        title="已结束"
        :value="endedSemesterCount"
        :icon="CalendarX"
        subtitle="历史学期"
        color="orange"
      />
    </div>

    <!-- 搜索和操作栏 -->
    <div class="mb-6 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-end gap-4">
        <div class="w-44">
          <label class="mb-1 block text-sm text-gray-600">学期名称</label>
          <input
            v-model="queryParams.semesterName"
            type="text"
            placeholder="请输入学期名称"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            @keyup.enter="handleQuery"
          />
        </div>
        <div class="w-36">
          <label class="mb-1 block text-sm text-gray-600">开始年份</label>
          <select
            v-model="queryParams.startYear"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          >
            <option :value="undefined">全部</option>
            <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}</option>
          </select>
        </div>
        <div class="w-32">
          <label class="mb-1 block text-sm text-gray-600">学期类型</label>
          <select
            v-model="queryParams.semesterType"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          >
            <option :value="undefined">全部</option>
            <option :value="1">第一学期</option>
            <option :value="2">第二学期</option>
          </select>
        </div>
        <div class="w-28">
          <label class="mb-1 block text-sm text-gray-600">状态</label>
          <select
            v-model="queryParams.status"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          >
            <option :value="undefined">全部</option>
            <option :value="1">正常</option>
            <option :value="0">已结束</option>
          </select>
        </div>
        <div class="flex gap-2">
          <button
            @click="handleQuery"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          >
            <Search class="h-4 w-4" />
            查询
          </button>
          <button
            @click="resetQuery"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            <RotateCcw class="h-4 w-4" />
            重置
          </button>
        </div>
        <div class="ml-auto">
          <button
            @click="handleAdd"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          >
            <Plus class="h-4 w-4" />
            新增学期
          </button>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <div class="flex items-center gap-2">
          <span class="font-medium text-gray-900">学期列表</span>
          <span class="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-600">{{ total }} 条</span>
        </div>
      </div>

      <table class="w-full">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50">
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">学期编码</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">学期名称</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">学期类型</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">开始日期</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">结束日期</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">时长(天)</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">当前学期</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">状态</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">操作</th>
          </tr>
        </thead>
        <tbody v-if="loading">
          <tr>
            <td colspan="9" class="py-16 text-center">
              <Loader2 class="mx-auto h-8 w-8 animate-spin text-blue-600" />
              <div class="mt-2 text-sm text-gray-500">加载中...</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else-if="semesterList.length === 0">
          <tr>
            <td colspan="9" class="py-16 text-center">
              <Calendar class="mx-auto h-12 w-12 text-gray-300" />
              <div class="mt-2 text-sm text-gray-500">暂无数据</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr
            v-for="row in filteredSemesterList"
            :key="row.id"
            class="border-b border-gray-100 hover:bg-gray-50"
          >
            <td class="px-4 py-3 font-mono text-sm text-gray-600">{{ row.semesterCode }}</td>
            <td class="px-4 py-3 font-medium text-gray-900">{{ row.semesterName }}</td>
            <td class="px-4 py-3 text-center">
              <span
                :class="[
                  'inline-flex rounded-full px-2 py-0.5 text-xs',
                  row.semesterType === 1 ? 'bg-blue-100 text-blue-700' : 'bg-green-100 text-green-700'
                ]"
              >
                {{ row.semesterType === 1 ? '第一学期' : '第二学期' }}
              </span>
            </td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ row.startDate }}</td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ row.endDate }}</td>
            <td class="px-4 py-3 text-center text-sm text-gray-600">{{ row.durationDays || '-' }}</td>
            <td class="px-4 py-3 text-center">
              <span
                v-if="row.isCurrent"
                class="inline-flex items-center gap-1 rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700"
              >
                <Star class="h-3 w-3" />
                当前
              </span>
              <span v-else class="text-gray-400">-</span>
            </td>
            <td class="px-4 py-3 text-center">
              <span
                :class="[
                  'inline-flex rounded-full px-2 py-0.5 text-xs',
                  row.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
                ]"
              >
                {{ row.status === 1 ? '正常' : '已结束' }}
              </span>
            </td>
            <td class="px-4 py-3">
              <div class="flex items-center justify-center gap-1">
                <button
                  @click="handleEdit(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-blue-50 hover:text-blue-600"
                  title="编辑"
                >
                  <Pencil class="h-4 w-4" />
                </button>
                <button
                  v-if="!row.isCurrent && row.status === 1"
                  @click="handleSetCurrent(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-amber-50 hover:text-amber-600"
                  title="设为当前学期"
                >
                  <Star class="h-4 w-4" />
                </button>
                <button
                  v-if="row.status === 1"
                  @click="handleEndSemester(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-orange-50 hover:text-orange-600"
                  title="结束学期"
                >
                  <CircleStop class="h-4 w-4" />
                </button>
                <button
                  v-if="row.status === 0"
                  @click="handleReactivate(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-green-50 hover:text-green-600"
                  title="重新激活"
                >
                  <RefreshCw class="h-4 w-4" />
                </button>
                <button
                  @click="handleDelete(row)"
                  :disabled="row.isCurrent"
                  :class="[
                    'rounded p-1.5',
                    row.isCurrent ? 'cursor-not-allowed text-gray-300' : 'text-gray-500 hover:bg-red-50 hover:text-red-600'
                  ]"
                  title="删除"
                >
                  <Trash2 class="h-4 w-4" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <div class="text-sm text-gray-500">
          共 {{ total }} 条
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="dialogVisible = false"></div>
          <div class="relative w-full max-w-lg rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">{{ dialogTitle }}</h3>
              <button @click="dialogVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="p-6">
              <div class="space-y-4">
                <div>
                  <label class="mb-1 block text-sm text-gray-700">
                    学期名称 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="formData.semesterName"
                    type="text"
                    placeholder="如：2024-2025学年第一学期"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div class="grid grid-cols-2 gap-4">
                  <div>
                    <label class="mb-1 block text-sm text-gray-700">
                      开始年份 <span class="text-red-500">*</span>
                    </label>
                    <select
                      v-model="formData.startYear"
                      @change="generateCode"
                      class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                    >
                      <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}</option>
                    </select>
                  </div>
                  <div>
                    <label class="mb-1 block text-sm text-gray-700">
                      学期类型 <span class="text-red-500">*</span>
                    </label>
                    <select
                      v-model="formData.semesterType"
                      @change="generateCode"
                      class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                    >
                      <option :value="1">第一学期</option>
                      <option :value="2">第二学期</option>
                    </select>
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-4">
                  <div>
                    <label class="mb-1 block text-sm text-gray-700">
                      开始日期 <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="formData.startDate"
                      type="date"
                      class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                    />
                  </div>
                  <div>
                    <label class="mb-1 block text-sm text-gray-700">
                      结束日期 <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="formData.endDate"
                      type="date"
                      class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                    />
                  </div>
                </div>
                <div v-if="generatedCode" class="rounded-lg bg-blue-50 p-3">
                  <div class="text-sm text-blue-700">
                    <span class="font-medium">学期编码：</span>{{ generatedCode }}
                  </div>
                </div>
              </div>
            </div>
            <div class="flex justify-end gap-3 border-t border-gray-200 px-6 py-4">
              <button
                @click="dialogVisible = false"
                class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                取消
              </button>
              <button
                @click="handleSubmit"
                :disabled="submitLoading"
                class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
                确定
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Calendar,
  CalendarCheck,
  CalendarDays,
  CalendarX,
  Plus,
  Search,
  RotateCcw,
  Pencil,
  Trash2,
  Star,
  CircleStop,
  RefreshCw,
  X,
  Loader2
} from 'lucide-vue-next'
import StatCard from '@/components/design-system/cards/StatCard.vue'
import { semesterApi } from '@/api/calendar'
import type { Semester } from '@/types/teaching'

// 包装函数 — 适配旧调用签名
const getAllSemesters = () => semesterApi.list()
const getCurrentSemester = () => semesterApi.getCurrent()
const createSemester = (data: any) => semesterApi.create(data)
const updateSemester = (id: number | string, data: any) => semesterApi.update(id, data)
const deleteSemester = (id: number | string) => semesterApi.delete(id)
const setCurrentSemester = (id: number | string) => semesterApi.setCurrent(id)
const endSemester = (id: number | string) => semesterApi.end(id)
const reactivateSemester = (id: number | string) => semesterApi.reactivate(id)
const generateSemesterCode = (startYear: number, semesterType: number) => semesterApi.generateCode(startYear, semesterType)

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentSemesterId = ref<number | string>()

const semesterList = ref<Semester[]>([])
const currentSemester = ref<Semester | null>(null)
const generatedCode = ref('')

// 查询参数
const queryParams = reactive({
  semesterName: '',
  startYear: undefined as number | undefined,
  semesterType: undefined as number | undefined,
  status: undefined as number | undefined
})

// 表单数据
const formData = reactive({
  semesterName: '',
  startYear: new Date().getFullYear(),
  semesterType: 1,
  startDate: '',
  endDate: ''
})

// 年份选项
const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 10 }, (_, i) => currentYear - 5 + i)

// 计算属性
const total = computed(() => semesterList.value.length)

const currentSemesterName = computed(() => {
  return currentSemester.value?.semesterName || '未设置'
})

const activeSemesterCount = computed(() => {
  return semesterList.value.filter(s => s.status === 1).length
})

const endedSemesterCount = computed(() => {
  return semesterList.value.filter(s => s.status === 0).length
})

const dialogTitle = computed(() => (isEdit.value ? '编辑学期' : '新增学期'))

// 过滤后的学期列表
const filteredSemesterList = computed(() => {
  let list = [...semesterList.value]

  if (queryParams.semesterName) {
    list = list.filter(s => s.semesterName.includes(queryParams.semesterName))
  }
  if (queryParams.startYear) {
    list = list.filter(s => s.startYear === queryParams.startYear)
  }
  if (queryParams.semesterType) {
    list = list.filter(s => s.semesterType === queryParams.semesterType)
  }
  if (queryParams.status) {
    list = list.filter(s => s.status === queryParams.status)
  }

  // 按开始年份降序排列
  return list.sort((a, b) => {
    if (a.isCurrent) return -1
    if (b.isCurrent) return 1
    return (b.startYear || 0) - (a.startYear || 0)
  })
})

// 加载学期列表
const loadSemesterList = async () => {
  loading.value = true
  try {
    const [list, current] = await Promise.all([
      getAllSemesters(),
      getCurrentSemester().catch(() => null)
    ])
    semesterList.value = list || []
    currentSemester.value = current
  } catch (error) {
    console.error('加载学期列表失败:', error)
    ElMessage.error('加载学期列表失败')
  } finally {
    loading.value = false
  }
}

// 生成学期编码
const generateCode = async () => {
  if (formData.startYear && formData.semesterType) {
    try {
      generatedCode.value = await generateSemesterCode(formData.startYear, formData.semesterType)
    } catch {
      // 如果后端生成失败，使用前端生成
      generatedCode.value = `${formData.startYear}-${formData.startYear + 1}-${formData.semesterType}`
    }
  }
}

const handleQuery = () => {
  // 前端过滤，不需要重新加载
}

const resetQuery = () => {
  queryParams.semesterName = ''
  queryParams.startYear = undefined
  queryParams.semesterType = undefined
  queryParams.status = undefined
}

const handleAdd = () => {
  isEdit.value = false
  formData.semesterName = ''
  formData.startYear = currentYear
  formData.semesterType = 1
  formData.startDate = ''
  formData.endDate = ''
  generatedCode.value = ''
  generateCode()
  dialogVisible.value = true
}

const handleEdit = (row: Semester) => {
  isEdit.value = true
  currentSemesterId.value = row.id
  formData.semesterName = row.semesterName
  formData.startYear = row.startYear || currentYear
  formData.semesterType = (row.semesterType as number) || 1
  formData.startDate = row.startDate
  formData.endDate = row.endDate
  generatedCode.value = row.semesterCode
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formData.semesterName) {
    ElMessage.error('请输入学期名称')
    return
  }
  if (!formData.startDate || !formData.endDate) {
    ElMessage.error('请选择开始和结束日期')
    return
  }

  try {
    submitLoading.value = true
    if (isEdit.value && currentSemesterId.value) {
      const updateData: Partial<Semester> = {
        semesterName: formData.semesterName,
        startDate: formData.startDate,
        endDate: formData.endDate
      }
      await updateSemester(currentSemesterId.value, updateData)
      ElMessage.success('更新成功')
    } else {
      const createData: Partial<Semester> & { startYear: number; semesterType: number } = {
        semesterName: formData.semesterName,
        startYear: formData.startYear,
        semesterType: formData.semesterType,
        startDate: formData.startDate,
        endDate: formData.endDate
      }
      await createSemester(createData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadSemesterList()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row: Semester) => {
  if (row.isCurrent) {
    ElMessage.warning('当前学期不能删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要删除学期"${row.semesterName}"吗?`, '删除确认', { type: 'warning' })
    await deleteSemester(row.id)
    ElMessage.success('删除成功')
    loadSemesterList()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

const handleSetCurrent = async (row: Semester) => {
  try {
    await ElMessageBox.confirm(`确定要将"${row.semesterName}"设为当前学期吗?`, '确认', { type: 'info' })
    await setCurrentSemester(row.id)
    ElMessage.success('设置成功')
    loadSemesterList()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '设置失败')
  }
}

const handleEndSemester = async (row: Semester) => {
  try {
    await ElMessageBox.confirm(`确定要结束学期"${row.semesterName}"吗?`, '确认', { type: 'warning' })
    await endSemester(row.id)
    ElMessage.success('学期已结束')
    loadSemesterList()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '操作失败')
  }
}

const handleReactivate = async (row: Semester) => {
  try {
    await ElMessageBox.confirm(`确定要重新激活学期"${row.semesterName}"吗?`, '确认', { type: 'info' })
    await reactivateSemester(row.id)
    ElMessage.success('学期已重新激活')
    loadSemesterList()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '操作失败')
  }
}

onMounted(() => {
  loadSemesterList()
})
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
