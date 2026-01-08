<template>
  <div class="min-h-screen bg-gray-50/50 p-6">
    <!-- 页面头部 -->
    <div class="mb-6 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-500 p-6 shadow-lg">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="flex items-center gap-3 text-2xl font-bold text-white">
            <GraduationCap class="h-8 w-8" />
            年级管理
          </h1>
          <p class="mt-1 text-emerald-100">管理学校年级及专业方向配置</p>
        </div>
        <button
          @click="handleAdd"
          class="flex items-center gap-2 rounded-lg bg-white/20 px-4 py-2 text-white backdrop-blur-sm transition-all hover:bg-white/30"
        >
          <Plus class="h-5 w-5" />
          新增年级
        </button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-4 gap-4">
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">年级总数</p>
            <p class="mt-1 text-2xl font-bold text-emerald-600">{{ total }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600">
            <Layers class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-emerald-500 to-teal-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">已启用</p>
            <p class="mt-1 text-2xl font-bold text-blue-600">{{ enabledCount }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-100 text-blue-600">
            <CheckCircle class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-blue-500 to-indigo-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">已禁用</p>
            <p class="mt-1 text-2xl font-bold text-gray-600">{{ disabledCount }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-gray-100 text-gray-600">
            <XCircle class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-gray-400 to-gray-300 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">专业方向</p>
            <p class="mt-1 text-2xl font-bold text-purple-600">{{ totalDirections }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-purple-100 text-purple-600">
            <BookOpen class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-purple-500 to-violet-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
    </div>

    <!-- 筛选区 -->
    <div class="mb-6 rounded-xl bg-white p-4 shadow-sm">
      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center gap-2">
          <label class="text-sm font-medium text-gray-600">年级名称</label>
          <input
            v-model="queryForm.gradeName"
            type="text"
            placeholder="请输入年级名称"
            class="w-48 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
          />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm font-medium text-gray-600">年级代码</label>
          <input
            v-model="queryForm.gradeCode"
            type="text"
            placeholder="请输入年级代码"
            class="w-48 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
          />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm font-medium text-gray-600">状态</label>
          <select
            v-model="queryForm.status"
            class="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
          >
            <option :value="undefined">全部</option>
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <button
            @click="handleQuery"
            class="flex items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-emerald-700"
          >
            <Search class="h-4 w-4" />
            查询
          </button>
          <button
            @click="handleReset"
            class="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
          >
            <RotateCcw class="h-4 w-4" />
            重置
          </button>
        </div>
      </div>
    </div>

    <!-- 年级列表 -->
    <div class="rounded-xl bg-white shadow-sm">
      <div v-if="loading" class="flex items-center justify-center py-12">
        <div class="flex items-center gap-2 text-gray-500">
          <Loader2 class="h-5 w-5 animate-spin" />
          <span>加载中...</span>
        </div>
      </div>
      <div v-else-if="gradeList.length === 0" class="flex flex-col items-center justify-center py-12 text-gray-500">
        <GraduationCap class="h-12 w-12 text-gray-300" />
        <span class="mt-2">暂无年级数据</span>
      </div>
      <div v-else class="divide-y divide-gray-100">
        <div
          v-for="(grade, index) in gradeList"
          :key="grade.id"
          class="flex items-center justify-between p-4 transition-colors hover:bg-gray-50/50"
          :style="{ animationDelay: `${index * 30}ms` }"
        >
          <div class="flex-1">
            <div class="flex items-center gap-3">
              <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-emerald-100 text-emerald-600">
                <GraduationCap class="h-5 w-5" />
              </div>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-base font-semibold text-gray-900">{{ grade.gradeName }}</span>
                  <span
                    class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium"
                    :class="grade.status === 1 ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-700'"
                  >
                    {{ grade.status === 1 ? '启用' : '禁用' }}
                  </span>
                </div>
                <div class="mt-1 flex items-center gap-4 text-sm text-gray-500">
                  <span class="flex items-center gap-1">
                    <Hash class="h-3.5 w-3.5" />
                    {{ grade.gradeCode }}
                  </span>
                  <span class="flex items-center gap-1">
                    <Calendar class="h-3.5 w-3.5" />
                    {{ grade.enrollmentYear }}级
                  </span>
                  <span class="flex items-center gap-1">
                    <Users class="h-3.5 w-3.5" />
                    班额 {{ grade.standardClassSize || 40 }}人
                  </span>
                  <span class="flex items-center gap-1">
                    <BookOpen class="h-3.5 w-3.5" />
                    专业方向
                    <span class="inline-flex items-center rounded-full bg-blue-100 px-1.5 py-0.5 text-xs font-medium text-blue-700">
                      {{ getGradeDirectionsCount(grade.id) }}
                    </span>
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <button
              @click="handleConfigDirections(grade)"
              class="flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-emerald-700"
            >
              <Settings class="h-4 w-4" />
              配置专业
            </button>
            <button
              @click="handleEdit(grade)"
              class="rounded-lg p-1.5 text-blue-600 transition-colors hover:bg-blue-50"
              title="编辑"
            >
              <Pencil class="h-4 w-4" />
            </button>
            <button
              @click="handleDelete(grade)"
              class="rounded-lg p-1.5 text-red-600 transition-colors hover:bg-red-50"
              title="删除"
            >
              <Trash2 class="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <div class="text-sm text-gray-500">
          共 <span class="font-medium text-gray-900">{{ total }}</span> 条记录
        </div>
        <div class="flex items-center gap-2">
          <select
            v-model="queryForm.pageSize"
            @change="handleQuery"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
          >
            <option :value="10">10条/页</option>
            <option :value="20">20条/页</option>
            <option :value="50">50条/页</option>
            <option :value="100">100条/页</option>
          </select>
          <div class="flex items-center gap-1">
            <button
              @click="queryForm.pageNum = Math.max(1, queryForm.pageNum - 1); handleQuery()"
              :disabled="queryForm.pageNum <= 1"
              class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              上一页
            </button>
            <span class="px-3 py-1.5 text-sm text-gray-600">
              {{ queryForm.pageNum }} / {{ Math.ceil(total / queryForm.pageSize) || 1 }}
            </span>
            <button
              @click="queryForm.pageNum = Math.min(Math.ceil(total / queryForm.pageSize), queryForm.pageNum + 1); handleQuery()"
              :disabled="queryForm.pageNum >= Math.ceil(total / queryForm.pageSize)"
              class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              下一页
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 专业配置抽屉 -->
    <Teleport to="body">
      <div
        v-if="configDrawerVisible"
        class="fixed inset-0 z-50 flex"
      >
        <div
          class="flex-1 bg-black/50 backdrop-blur-sm"
          @click="configDrawerVisible = false"
        ></div>
        <div class="h-full w-[60%] animate-slide-in-right overflow-y-auto bg-white shadow-2xl">
          <div class="sticky top-0 z-10 flex items-center justify-between border-b border-gray-200 bg-white p-4">
            <h3 class="flex items-center gap-2 text-lg font-semibold text-gray-900">
              <Settings class="h-5 w-5 text-emerald-600" />
              {{ currentGrade?.gradeName }} - 专业配置
            </h3>
            <button
              @click="configDrawerVisible = false"
              class="rounded-lg p-1 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
            >
              <X class="h-5 w-5" />
            </button>
          </div>
          <div class="p-4">
            <GradeDirectionConfigDrag
              v-if="currentGrade"
              :grade-id="Number(currentGrade.id)"
              :grade-name="currentGrade.gradeName"
              :enrollment-year="currentGrade.enrollmentYear"
              @refresh="handleQuery"
            />
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 年级表单弹窗 -->
    <GradeForm
      v-model="formVisible"
      :form-data="formData"
      :is-edit="isEdit"
      @success="handleQuery"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  GraduationCap,
  Plus,
  Layers,
  CheckCircle,
  XCircle,
  BookOpen,
  Search,
  RotateCcw,
  Loader2,
  Hash,
  Calendar,
  Users,
  Settings,
  Pencil,
  Trash2,
  X
} from 'lucide-vue-next'
import {
  getGradePage,
  deleteGrade
} from '@/api/grade'
import { getDirectionsByYear } from '@/api/gradeMajorDirection'
import GradeForm from '@/components/grade/GradeForm.vue'
import GradeDirectionConfigDrag from './components/GradeDirectionConfigDrag.vue'

// 查询表单
const queryForm = reactive({
  gradeName: '',
  gradeCode: '',
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 20
})

const loading = ref(false)
const total = ref(0)
const gradeList = ref<any[]>([])
const formVisible = ref(false)
const formData = ref<any>({})
const isEdit = ref(false)

// 专业方向配置
const configDrawerVisible = ref(false)
const currentGrade = ref<any>(null)
const gradeDirectionsMap = ref<Map<number, any[]>>(new Map())

// 统计数据
const enabledCount = computed(() => gradeList.value.filter(g => g.status === 1).length)
const disabledCount = computed(() => gradeList.value.filter(g => g.status === 0).length)
const totalDirections = computed(() => {
  let count = 0
  gradeDirectionsMap.value.forEach(directions => {
    count += directions.length
  })
  return count
})

// 查询
const handleQuery = async () => {
  loading.value = true
  try {
    const { records, total: totalCount } = await getGradePage(queryForm)
    gradeList.value = records || []
    total.value = totalCount || 0

    // 加载每个年级的专业方向数量
    for (const grade of gradeList.value) {
      loadGradeDirections(grade.enrollmentYear, grade.id)
    }
  } catch (error) {
    console.error('查询年级列表失败:', error)
    ElMessage.error('查询年级列表失败')
  } finally {
    loading.value = false
  }
}

// 加载年级专业方向
const loadGradeDirections = async (enrollmentYear: number, gradeId: number) => {
  try {
    const data = await getDirectionsByYear(enrollmentYear)
    gradeDirectionsMap.value.set(gradeId, data || [])
  } catch (error) {
    console.error('加载年级专业方向失败:', error)
  }
}

// 获取年级专业方向数量
const getGradeDirectionsCount = (gradeId: number) => {
  const directions = gradeDirectionsMap.value.get(gradeId)
  return directions ? directions.length : 0
}

// 重置
const handleReset = () => {
  queryForm.gradeName = ''
  queryForm.gradeCode = ''
  queryForm.status = undefined
  queryForm.pageNum = 1
  handleQuery()
}

// 新增
const handleAdd = () => {
  formData.value = {}
  isEdit.value = false
  formVisible.value = true
}

// 编辑
const handleEdit = (row: any) => {
  formData.value = { ...row }
  isEdit.value = true
  formVisible.value = true
}

// 删除
const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除年级【${row.gradeName}】吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteGrade(row.id)
    ElMessage.success('删除成功')
    handleQuery()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 配置专业方向
const handleConfigDirections = (grade: any) => {
  currentGrade.value = grade
  configDrawerVisible.value = true
}

onMounted(() => {
  handleQuery()
})
</script>

<style scoped>
@keyframes slide-in-right {
  from {
    transform: translateX(100%);
  }
  to {
    transform: translateX(0);
  }
}

.animate-slide-in-right {
  animation: slide-in-right 0.3s ease-out;
}

.divide-y > div {
  animation: row-fade-in 0.3s ease-out forwards;
  opacity: 0;
}

@keyframes row-fade-in {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
