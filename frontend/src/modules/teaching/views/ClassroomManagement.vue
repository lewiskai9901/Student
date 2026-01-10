<template>
  <div class="min-h-screen bg-gray-50/50 p-6">
    <!-- 页面头部 -->
    <div class="mb-6 rounded-xl bg-gradient-to-r from-cyan-600 to-blue-500 p-6 shadow-lg">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="flex items-center gap-3 text-2xl font-bold text-white">
            <School class="h-8 w-8" />
            教室管理
          </h1>
          <p class="mt-1 text-cyan-100">管理教学楼的教室信息</p>
        </div>
        <div class="flex gap-3">
          <button
            v-if="hasPermission('teaching:classroom:add')"
            @click="handleAdd"
            class="flex items-center gap-2 rounded-lg bg-white/95 px-4 py-2 font-medium text-cyan-600 shadow-md transition-all hover:-translate-y-0.5 hover:bg-white hover:shadow-lg"
          >
            <Plus class="h-5 w-5" />
            新增教室
          </button>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-4 gap-4">
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">教室总数</p>
            <p class="mt-1 text-2xl font-bold text-gray-900">{{ pagination.total }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-cyan-100 text-cyan-600">
            <School class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-cyan-500 to-blue-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">已启用</p>
            <p class="mt-1 text-2xl font-bold text-green-600">{{ stats.enabled }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-green-100 text-green-600">
            <CheckCircle class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-green-500 to-emerald-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">已停用</p>
            <p class="mt-1 text-2xl font-bold text-gray-500">{{ stats.disabled }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-gray-100 text-gray-500">
            <XCircle class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-gray-400 to-gray-300 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">教学楼</p>
            <p class="mt-1 text-2xl font-bold text-blue-600">{{ buildingOptions.length }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-100 text-blue-600">
            <Building class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-blue-500 to-indigo-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="mb-6 rounded-xl bg-white p-5 shadow-sm">
      <div class="flex flex-wrap items-end gap-4">
        <div class="min-w-[150px]">
          <label class="mb-1.5 block text-sm font-medium text-gray-700">教学楼</label>
          <select
            v-model="searchForm.buildingId"
            class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
          >
            <option :value="null">全部教学楼</option>
            <option v-for="building in buildingOptions" :key="building.id" :value="building.id">
              {{ building.buildingName }}
            </option>
          </select>
        </div>
        <div class="min-w-[100px]">
          <label class="mb-1.5 block text-sm font-medium text-gray-700">楼层</label>
          <input
            v-model.number="searchForm.floor"
            type="number"
            min="1"
            max="50"
            placeholder="楼层"
            class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
          />
        </div>
        <div class="min-w-[150px]">
          <label class="mb-1.5 block text-sm font-medium text-gray-700">教室类型</label>
          <select
            v-model="searchForm.classroomType"
            class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
          >
            <option value="">全部类型</option>
            <option value="普通教室">普通教室</option>
            <option value="多媒体教室">多媒体教室</option>
            <option value="实验室">实验室</option>
          </select>
        </div>
        <div class="min-w-[120px]">
          <label class="mb-1.5 block text-sm font-medium text-gray-700">状态</label>
          <select
            v-model="searchForm.status"
            class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
          >
            <option :value="null">全部状态</option>
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </div>
        <div class="flex gap-2">
          <button
            @click="handleSearch"
            class="flex items-center gap-2 rounded-lg bg-gradient-to-r from-cyan-600 to-blue-500 px-4 py-2 text-sm font-medium text-white shadow-sm transition-all hover:-translate-y-0.5 hover:shadow-md"
          >
            <Search class="h-4 w-4" />
            搜索
          </button>
          <button
            @click="handleReset"
            class="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 transition-all hover:bg-gray-50"
          >
            <RotateCcw class="h-4 w-4" />
            重置
          </button>
        </div>
      </div>
    </div>

    <!-- 表格卡片 -->
    <div class="rounded-xl bg-white shadow-sm">
      <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4">
        <div class="flex items-center gap-3">
          <h3 class="font-semibold text-gray-900">教室列表</h3>
          <span class="rounded-full bg-cyan-100 px-2.5 py-0.5 text-xs font-medium text-cyan-700">
            {{ pagination.total }} 条记录
          </span>
        </div>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="border-b border-gray-100 bg-gray-50/50">
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">教室名称</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">教室编号</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">教学楼</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">楼层</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">房间号</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">使用情况</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">关联班级</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">类型</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">状态</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">操作</th>
            </tr>
          </thead>
          <tbody v-if="loading">
            <tr>
              <td colspan="10" class="py-20 text-center">
                <div class="flex flex-col items-center gap-3">
                  <Loader2 class="h-8 w-8 animate-spin text-cyan-600" />
                  <span class="text-sm text-gray-500">加载中...</span>
                </div>
              </td>
            </tr>
          </tbody>
          <tbody v-else-if="classroomList.length === 0">
            <tr>
              <td colspan="10" class="py-20 text-center">
                <div class="flex flex-col items-center gap-3">
                  <School class="h-12 w-12 text-gray-300" />
                  <span class="text-sm text-gray-500">暂无教室数据</span>
                </div>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr
              v-for="(row, index) in classroomList"
              :key="row.id"
              class="animate-fade-in border-b border-gray-50 transition-colors hover:bg-cyan-50/30"
              :style="{ animationDelay: `${index * 50}ms` }"
            >
              <td class="whitespace-nowrap px-4 py-3">
                <div class="flex items-center gap-3">
                  <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-cyan-100">
                    <School class="h-5 w-5 text-cyan-600" />
                  </div>
                  <span class="font-medium text-gray-900">{{ row.classroomName }}</span>
                </div>
              </td>
              <td class="whitespace-nowrap px-4 py-3">
                <span class="rounded-md bg-gray-100 px-2 py-0.5 font-mono text-xs text-gray-600">
                  {{ row.classroomCode }}
                </span>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-gray-600">{{ row.buildingName }}</td>
              <td class="whitespace-nowrap px-4 py-3 text-center text-gray-600">{{ row.floor }}F</td>
              <td class="whitespace-nowrap px-4 py-3 text-center text-gray-600">{{ row.roomNumber }}</td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span :class="[(row.studentCount || 0) > row.capacity ? 'text-red-600' : 'text-green-600', 'font-medium']">
                  {{ row.studentCount || 0 }}/{{ row.capacity }}
                </span>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-gray-600">{{ row.className || '-' }}</td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span :class="getClassroomTypeClass(row.classroomType)">
                  {{ row.classroomType }}
                </span>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span :class="['rounded-full px-2.5 py-0.5 text-xs font-medium', row.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600']">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </span>
              </td>
              <td class="whitespace-nowrap px-4 py-3">
                <div class="flex items-center justify-center gap-1">
                  <button
                    v-if="hasPermission('teaching:classroom:edit')"
                    @click="handleEdit(row)"
                    class="rounded-lg p-1.5 text-gray-500 transition-colors hover:bg-cyan-100 hover:text-cyan-600"
                    title="编辑"
                  >
                    <Pencil class="h-4 w-4" />
                  </button>
                  <button
                    v-if="hasPermission('teaching:classroom:delete')"
                    @click="handleDelete(row)"
                    class="rounded-lg p-1.5 text-gray-500 transition-colors hover:bg-red-100 hover:text-red-600"
                    title="删除"
                  >
                    <Trash2 class="h-4 w-4" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div v-if="pagination.total > 0" class="flex items-center justify-between border-t border-gray-100 px-5 py-4">
        <div class="text-sm text-gray-500">
          共 {{ pagination.total }} 条记录，第 {{ pagination.pageNum }} / {{ Math.ceil(pagination.total / pagination.pageSize) }} 页
        </div>
        <div class="flex items-center gap-2">
          <button
            @click="pagination.pageNum = 1; loadClassrooms()"
            :disabled="pagination.pageNum <= 1"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            首页
          </button>
          <button
            @click="pagination.pageNum--; loadClassrooms()"
            :disabled="pagination.pageNum <= 1"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            <ChevronLeft class="h-4 w-4" />
          </button>
          <button
            @click="pagination.pageNum++; loadClassrooms()"
            :disabled="pagination.pageNum >= Math.ceil(pagination.total / pagination.pageSize)"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            <ChevronRight class="h-4 w-4" />
          </button>
          <button
            @click="pagination.pageNum = Math.ceil(pagination.total / pagination.pageSize); loadClassrooms()"
            :disabled="pagination.pageNum >= Math.ceil(pagination.total / pagination.pageSize)"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            末页
          </button>
          <select
            v-model="pagination.pageSize"
            @change="pagination.pageNum = 1; loadClassrooms()"
            class="pagination-select"
          >
            <option :value="10">10条/页</option>
            <option :value="20">20条/页</option>
            <option :value="50">50条/页</option>
            <option :value="100">100条/页</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <Teleport to="body">
      <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50 backdrop-blur-sm" @click="dialogVisible = false"></div>
        <div class="relative z-10 w-full max-w-2xl rounded-xl bg-white shadow-2xl">
          <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
            <h3 class="text-lg font-semibold text-gray-900">{{ dialogTitle }}</h3>
            <button @click="dialogVisible = false" class="rounded-lg p-1 hover:bg-gray-100">
              <X class="h-5 w-5 text-gray-500" />
            </button>
          </div>
          <div class="max-h-[70vh] overflow-y-auto p-6">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  教室名称 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="formData.classroomName"
                  type="text"
                  placeholder="请输入教室名称"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
                />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  教室编号 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="formData.classroomCode"
                  type="text"
                  placeholder="请输入教室编号"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm font-mono transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
                />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  教学楼 <span class="text-red-500">*</span>
                </label>
                <select
                  v-model="formData.buildingId"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
                >
                  <option value="" disabled>请选择教学楼</option>
                  <option v-for="building in buildingOptions" :key="building.id" :value="building.id">
                    {{ building.buildingName }}
                  </option>
                </select>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  楼层 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model.number="formData.floor"
                  type="number"
                  min="1"
                  max="50"
                  placeholder="请输入楼层"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
                />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  房间号 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="formData.roomNumber"
                  type="text"
                  placeholder="请输入房间号"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
                />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  容纳人数 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model.number="formData.capacity"
                  type="number"
                  min="1"
                  max="200"
                  placeholder="请输入容纳人数"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
                />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">教室类型</label>
                <select
                  v-model="formData.classroomType"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
                >
                  <option value="普通教室">普通教室</option>
                  <option value="多媒体教室">多媒体教室</option>
                  <option value="实验室">实验室</option>
                </select>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">状态</label>
                <div class="flex gap-6 py-2">
                  <label class="flex cursor-pointer items-center gap-2">
                    <input v-model="formData.status" type="radio" :value="1" class="h-4 w-4 border-gray-300 text-cyan-600 focus:ring-cyan-500" />
                    <span class="text-sm text-gray-700">启用</span>
                  </label>
                  <label class="flex cursor-pointer items-center gap-2">
                    <input v-model="formData.status" type="radio" :value="0" class="h-4 w-4 border-gray-300 text-cyan-600 focus:ring-cyan-500" />
                    <span class="text-sm text-gray-700">停用</span>
                  </label>
                </div>
              </div>
            </div>
            <div class="mt-4">
              <label class="mb-1.5 block text-sm font-medium text-gray-700">设施设备</label>
              <textarea
                v-model="formData.facilities"
                rows="2"
                placeholder="请输入设施设备信息"
                class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
              ></textarea>
            </div>
          </div>
          <div class="flex justify-end gap-3 border-t border-gray-100 px-6 py-4">
            <button
              @click="dialogVisible = false"
              class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
            >
              取消
            </button>
            <button
              @click="handleSubmit"
              :disabled="submitting"
              class="flex items-center gap-2 rounded-lg bg-gradient-to-r from-cyan-600 to-blue-500 px-4 py-2 text-sm font-medium text-white transition-all hover:-translate-y-0.5 hover:shadow-md disabled:opacity-50"
            >
              <Loader2 v-if="submitting" class="h-4 w-4 animate-spin" />
              确定
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  School,
  Plus,
  Search,
  RotateCcw,
  Pencil,
  Trash2,
  X,
  Loader2,
  ChevronLeft,
  ChevronRight,
  CheckCircle,
  XCircle,
  Building
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import type { Classroom } from '@/types/teaching'
import {
  getClassrooms,
  createClassroom,
  updateClassroom,
  deleteClassroom
} from '@/api/v2/teaching'
import { getAllEnabledBuildings } from '@/api/v2/dormitory'
import type { Building as BuildingType } from '@/types/building'

const authStore = useAuthStore()
const hasPermission = (permission: string) => authStore.hasPermission(permission)

const buildingOptions = ref<BuildingType[]>([])

const searchForm = reactive({
  buildingId: null as number | string | null,
  floor: null as number | null,
  classroomType: '',
  status: null as number | null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const loading = ref(false)
const classroomList = ref<Classroom[]>([])

// 统计数据
const stats = computed(() => {
  return {
    enabled: classroomList.value.filter(c => c.status === 1).length,
    disabled: classroomList.value.filter(c => c.status === 0).length
  }
})

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('新增教室')
const submitting = ref(false)
const formData = reactive<Classroom>({
  buildingId: '',
  classroomName: '',
  classroomCode: '',
  floor: 1,
  roomNumber: '',
  capacity: 30,
  classroomType: '普通教室',
  facilities: '',
  status: 1
})

// 获取教室类型样式
const getClassroomTypeClass = (type: string) => {
  const classes: Record<string, string> = {
    '普通教室': 'rounded-full bg-blue-100 px-2.5 py-0.5 text-xs font-medium text-blue-700',
    '多媒体教室': 'rounded-full bg-purple-100 px-2.5 py-0.5 text-xs font-medium text-purple-700',
    '实验室': 'rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-medium text-amber-700'
  }
  return classes[type] || classes['普通教室']
}

// 加载教学楼选项
const loadBuildingOptions = async () => {
  try {
    buildingOptions.value = await getAllEnabledBuildings(1)
  } catch (error: any) {
    ElMessage.error(error.message || '加载教学楼失败')
  }
}

// 加载数据
const loadClassrooms = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    }
    const res = await getClassrooms(params)
    classroomList.value = res.records
    pagination.total = res.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  loadClassrooms()
}

// 重置
const handleReset = () => {
  searchForm.buildingId = null
  searchForm.floor = null
  searchForm.classroomType = ''
  searchForm.status = null
  pagination.pageNum = 1
  loadClassrooms()
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增教室'
  Object.assign(formData, {
    id: undefined,
    buildingId: '',
    classroomName: '',
    classroomCode: '',
    floor: 1,
    roomNumber: '',
    capacity: 30,
    classroomType: '普通教室',
    facilities: '',
    status: 1
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: Classroom) => {
  dialogTitle.value = '编辑教室'
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row: Classroom) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除教室"${row.classroomName}"吗?`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteClassroom(row.id!)
    ElMessage.success('删除成功')
    loadClassrooms()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 提交
const handleSubmit = async () => {
  if (!formData.classroomName) {
    ElMessage.error('请输入教室名称')
    return
  }
  if (!formData.classroomCode) {
    ElMessage.error('请输入教室编号')
    return
  }
  if (!formData.buildingId) {
    ElMessage.error('请选择教学楼')
    return
  }

  submitting.value = true
  try {
    if (formData.id) {
      await updateClassroom(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createClassroom(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadClassrooms()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadBuildingOptions()
  loadClassrooms()
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
