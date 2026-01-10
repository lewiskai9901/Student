<template>
  <div class="min-h-screen bg-gray-50/50 p-6">
    <!-- 页面头部 -->
    <div class="mb-6 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 p-6 shadow-lg">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="flex items-center gap-3 text-2xl font-bold text-white">
            <Building2 class="h-8 w-8" />
            教学楼管理
          </h1>
          <p class="mt-1 text-blue-100">管理学校的教学楼信息</p>
        </div>
        <div class="flex items-center gap-3">
          <div class="rounded-lg bg-white/20 px-4 py-2 backdrop-blur-sm">
            <span class="text-sm text-blue-100">共</span>
            <span class="mx-1 text-xl font-bold text-white">{{ pagination.total }}</span>
            <span class="text-sm text-blue-100">栋教学楼</span>
          </div>
          <button
            v-if="hasPermission('teaching:building:add')"
            @click="handleAdd"
            class="flex items-center gap-2 rounded-lg bg-white/95 px-4 py-2 font-medium text-blue-600 shadow-md transition-all hover:-translate-y-0.5 hover:bg-white hover:shadow-lg"
          >
            <Plus class="h-5 w-5" />
            新增教学楼
          </button>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-4 gap-4">
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">总教学楼</p>
            <p class="mt-1 text-2xl font-bold text-blue-600">{{ stats.total }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-100 text-blue-600">
            <Building class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-blue-500 to-blue-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">启用中</p>
            <p class="mt-1 text-2xl font-bold text-emerald-600">{{ stats.enabled }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600">
            <CheckCircle class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-emerald-500 to-emerald-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">已停用</p>
            <p class="mt-1 text-2xl font-bold text-red-600">{{ stats.disabled }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-red-100 text-red-600">
            <XCircle class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-red-500 to-red-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">总楼层数</p>
            <p class="mt-1 text-2xl font-bold text-amber-600">{{ stats.totalFloors }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-amber-100 text-amber-600">
            <Layers class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-amber-500 to-amber-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
    </div>

    <!-- 搜索区域 -->
    <div class="mb-6 rounded-xl bg-white p-4 shadow-sm">
      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center gap-2">
          <label class="text-sm font-medium text-gray-600">教学楼名称</label>
          <input
            v-model="searchForm.buildingName"
            type="text"
            placeholder="请输入教学楼名称"
            class="h-9 w-48 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm font-medium text-gray-600">状态</label>
          <select
            v-model="searchForm.status"
            class="h-9 w-32 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option :value="null">全部状态</option>
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </div>
        <div class="flex gap-2">
          <button
            @click="handleSearch"
            class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
          >
            <Search class="h-4 w-4" />
            搜索
          </button>
          <button
            @click="handleReset"
            class="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
          >
            <RotateCcw class="h-4 w-4" />
            重置
          </button>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="rounded-xl bg-white shadow-sm">
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="border-b border-gray-200 bg-gray-50/50">
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-600">教学楼名称</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider text-gray-600">楼层数</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-600">位置</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-600">描述</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider text-gray-600">状态</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider text-gray-600">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr v-if="loading" class="animate-pulse">
              <td colspan="6" class="px-4 py-8 text-center text-gray-400">
                <div class="flex items-center justify-center gap-2">
                  <Loader2 class="h-5 w-5 animate-spin" />
                  加载中...
                </div>
              </td>
            </tr>
            <tr v-else-if="buildingList.length === 0">
              <td colspan="6" class="px-4 py-8 text-center text-gray-400">
                <Building2 class="mx-auto h-12 w-12 text-gray-300" />
                <p class="mt-2">暂无数据</p>
              </td>
            </tr>
            <tr
              v-for="(row, index) in buildingList"
              :key="row.id"
              class="animate-fade-in transition-colors hover:bg-gray-50/50"
              :style="{ animationDelay: `${index * 30}ms` }"
            >
              <td class="px-4 py-3.5">
                <div class="flex items-center gap-3">
                  <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-blue-500 to-cyan-500 text-white shadow">
                    <Building class="h-5 w-5" />
                  </div>
                  <span class="font-medium text-gray-900">{{ row.buildingName }}</span>
                </div>
              </td>
              <td class="px-4 py-3.5 text-center">
                <span class="inline-flex items-center gap-1 rounded-full bg-blue-100 px-3 py-1 text-sm font-medium text-blue-700">
                  <Layers class="h-3.5 w-3.5" />
                  {{ row.floorCount }}层
                </span>
              </td>
              <td class="px-4 py-3.5">
                <div class="flex items-center gap-1 text-sm text-gray-600">
                  <MapPin class="h-4 w-4 text-gray-400" />
                  {{ row.location || '-' }}
                </div>
              </td>
              <td class="max-w-xs px-4 py-3.5">
                <span class="text-sm text-gray-500 truncate block" :title="row.description">
                  {{ row.description || '-' }}
                </span>
              </td>
              <td class="px-4 py-3.5 text-center">
                <span :class="[
                  'inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium',
                  row.status === 1 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                ]">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </span>
              </td>
              <td class="px-4 py-3.5">
                <div class="flex items-center justify-center gap-1">
                  <button
                    v-if="hasPermission('teaching:building:edit')"
                    @click="handleEdit(row)"
                    class="inline-flex items-center gap-1 rounded-lg px-2.5 py-1.5 text-xs font-medium text-blue-600 transition-colors hover:bg-blue-50"
                    title="编辑"
                  >
                    <Pencil class="h-3.5 w-3.5" />
                    编辑
                  </button>
                  <button
                    v-if="hasPermission('teaching:building:delete')"
                    @click="handleDelete(row)"
                    class="inline-flex items-center gap-1 rounded-lg px-2.5 py-1.5 text-xs font-medium text-red-600 transition-colors hover:bg-red-50"
                    title="删除"
                  >
                    <Trash2 class="h-3.5 w-3.5" />
                    删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <div class="text-sm text-gray-500">
          共 <span class="font-medium text-gray-900">{{ pagination.total }}</span> 条记录
        </div>
        <div class="flex items-center gap-2">
          <select
            v-model="pagination.pageSize"
            @change="loadBuildings"
            class="pagination-select"
          >
            <option :value="10">10条/页</option>
            <option :value="20">20条/页</option>
            <option :value="50">50条/页</option>
            <option :value="100">100条/页</option>
          </select>
          <div class="flex items-center gap-1">
            <button
              @click="handlePageChange(1)"
              :disabled="pagination.pageNum === 1"
              class="flex h-8 w-8 items-center justify-center rounded-lg border border-gray-300 text-gray-600 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <ChevronsLeft class="h-4 w-4" />
            </button>
            <button
              @click="handlePageChange(pagination.pageNum - 1)"
              :disabled="pagination.pageNum === 1"
              class="flex h-8 w-8 items-center justify-center rounded-lg border border-gray-300 text-gray-600 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <ChevronLeft class="h-4 w-4" />
            </button>
            <span class="px-3 text-sm text-gray-600">
              第 <span class="font-medium text-gray-900">{{ pagination.pageNum }}</span> /
              <span class="font-medium text-gray-900">{{ totalPages }}</span> 页
            </span>
            <button
              @click="handlePageChange(pagination.pageNum + 1)"
              :disabled="pagination.pageNum >= totalPages"
              class="flex h-8 w-8 items-center justify-center rounded-lg border border-gray-300 text-gray-600 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <ChevronRight class="h-4 w-4" />
            </button>
            <button
              @click="handlePageChange(totalPages)"
              :disabled="pagination.pageNum >= totalPages"
              class="flex h-8 w-8 items-center justify-center rounded-lg border border-gray-300 text-gray-600 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <ChevronsRight class="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <Teleport to="body">
      <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50 backdrop-blur-sm" @click="dialogVisible = false"></div>
        <div class="relative w-full max-w-xl rounded-2xl bg-white p-6 shadow-2xl animate-fade-in">
          <div class="mb-6 flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-100">
                <Building2 class="h-5 w-5 text-blue-600" />
              </div>
              <div>
                <h3 class="text-lg font-semibold text-gray-900">{{ dialogTitle }}</h3>
                <p class="text-sm text-gray-500">{{ formData.id ? '修改教学楼信息' : '添加新的教学楼' }}</p>
              </div>
            </div>
            <button @click="dialogVisible = false" class="rounded-lg p-2 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600">
              <X class="h-5 w-5" />
            </button>
          </div>

          <form @submit.prevent="handleSubmit" class="space-y-4">
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">
                教学楼名称 <span class="text-red-500">*</span>
              </label>
              <input
                v-model="formData.buildingName"
                type="text"
                required
                placeholder="请输入教学楼名称"
                class="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-sm transition-colors focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              />
            </div>

            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">
                楼层数 <span class="text-red-500">*</span>
              </label>
              <input
                v-model.number="formData.floorCount"
                type="number"
                min="1"
                max="50"
                required
                placeholder="请输入楼层数"
                class="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-sm transition-colors focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              />
            </div>

            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">位置</label>
              <input
                v-model="formData.location"
                type="text"
                placeholder="请输入位置"
                class="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-sm transition-colors focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              />
            </div>

            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">描述</label>
              <textarea
                v-model="formData.description"
                rows="3"
                placeholder="请输入描述"
                class="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-sm transition-colors focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              ></textarea>
            </div>

            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">
                状态 <span class="text-red-500">*</span>
              </label>
              <div class="flex gap-4 py-2">
                <label class="flex cursor-pointer items-center gap-2">
                  <input
                    v-model="formData.status"
                    type="radio"
                    :value="1"
                    class="h-4 w-4 border-gray-300 text-blue-600 focus:ring-blue-500"
                  />
                  <span class="text-sm text-gray-700">启用</span>
                </label>
                <label class="flex cursor-pointer items-center gap-2">
                  <input
                    v-model="formData.status"
                    type="radio"
                    :value="0"
                    class="h-4 w-4 border-gray-300 text-blue-600 focus:ring-blue-500"
                  />
                  <span class="text-sm text-gray-700">停用</span>
                </label>
              </div>
            </div>

            <div class="flex justify-end gap-3 pt-4 border-t border-gray-200">
              <button
                type="button"
                @click="dialogVisible = false"
                class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
              >
                取消
              </button>
              <button
                type="submit"
                :disabled="submitting"
                class="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700 disabled:opacity-50"
              >
                <Loader2 v-if="submitting" class="h-4 w-4 animate-spin" />
                <Save v-else class="h-4 w-4" />
                确定
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Building2,
  Building,
  Plus,
  Search,
  RotateCcw,
  Layers,
  MapPin,
  CheckCircle,
  XCircle,
  Pencil,
  Trash2,
  X,
  Save,
  Loader2,
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import type { TeachingBuilding } from '@/types/teaching'
import {
  getBuildings,
  createBuilding,
  updateBuilding,
  deleteBuilding
} from '@/api/v2/teaching'

const authStore = useAuthStore()
const hasPermission = (permission: string) => authStore.hasPermission(permission)

// 搜索表单
const searchForm = reactive({
  buildingName: '',
  status: null as number | null
})

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 统计数据
const stats = reactive({
  total: 0,
  enabled: 0,
  disabled: 0,
  totalFloors: 0
})

// 计算总页数
const totalPages = computed(() => Math.ceil(pagination.total / pagination.pageSize) || 1)

// 数据列表
const loading = ref(false)
const buildingList = ref<TeachingBuilding[]>([])

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('新增教学楼')
const submitting = ref(false)
const formData = reactive<TeachingBuilding>({
  buildingName: '',
  floorCount: 1,
  location: '',
  description: '',
  status: 1
})

// 加载数据
const loadBuildings = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    }
    const res = await getBuildings(params)
    buildingList.value = res.records
    pagination.total = res.total

    // 计算统计数据
    stats.total = res.total
    stats.enabled = res.records.filter((b: TeachingBuilding) => b.status === 1).length
    stats.disabled = res.records.filter((b: TeachingBuilding) => b.status === 0).length
    stats.totalFloors = res.records.reduce((sum: number, b: TeachingBuilding) => sum + (b.floorCount || 0), 0)
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  loadBuildings()
}

// 重置
const handleReset = () => {
  searchForm.buildingName = ''
  searchForm.status = null
  pagination.pageNum = 1
  loadBuildings()
}

// 分页
const handlePageChange = (page: number) => {
  if (page < 1 || page > totalPages.value) return
  pagination.pageNum = page
  loadBuildings()
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增教学楼'
  Object.assign(formData, {
    id: undefined,
    buildingName: '',
    floorCount: 1,
    location: '',
    description: '',
    status: 1
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: TeachingBuilding) => {
  dialogTitle.value = '编辑教学楼'
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row: TeachingBuilding) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除教学楼"${row.buildingName}"吗?`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteBuilding(row.id!)
    ElMessage.success('删除成功')
    loadBuildings()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 提交
const handleSubmit = async () => {
  if (!formData.buildingName) {
    ElMessage.error('请输入教学楼名称')
    return
  }

  submitting.value = true
  try {
    if (formData.id) {
      await updateBuilding(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createBuilding(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadBuildings()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

// 初始化
onMounted(() => {
  loadBuildings()
})
</script>

<style scoped>
@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in {
  animation: fade-in 0.3s ease-out forwards;
  opacity: 0;
}
</style>
