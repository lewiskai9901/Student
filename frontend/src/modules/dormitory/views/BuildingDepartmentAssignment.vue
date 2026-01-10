<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 页面标题 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">宿舍楼-院系分配</h1>
      <p class="mt-1 text-sm text-gray-500">管理宿舍楼与院系的分配关系，支持按楼层范围分配</p>
    </div>

    <!-- 搜索区域 -->
    <div class="mb-6 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-600">宿舍楼</label>
          <select
            v-model="queryForm.buildingId"
            class="h-9 w-48 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          >
            <option value="">全部宿舍楼</option>
            <option v-for="item in buildingOptions" :key="item.id" :value="item.id">
              {{ item.buildingName }}
            </option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-600">院系</label>
          <select
            v-model="queryForm.departmentId"
            class="h-9 w-48 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          >
            <option value="">全部院系</option>
            <option v-for="item in departmentOptions" :key="item.id" :value="item.id">
              {{ item.deptName }}
            </option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-600">状态</label>
          <select
            v-model="queryForm.status"
            class="h-9 w-32 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          >
            <option value="">全部状态</option>
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
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
            @click="handleReset"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            <RotateCcw class="h-4 w-4" />
            重置
          </button>
        </div>
      </div>
    </div>

    <!-- 操作区域 -->
    <div class="mb-4 flex items-center justify-between">
      <button
        @click="handleAdd"
        class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
      >
        <Plus class="h-4 w-4" />
        新增分配
      </button>
    </div>

    <!-- 数据表格 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <table class="min-w-full divide-y divide-gray-200">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">宿舍楼</th>
            <th class="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">院系</th>
            <th class="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">楼层范围</th>
            <th class="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">分配房间/床位</th>
            <th class="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">优先级</th>
            <th class="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">状态</th>
            <th class="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-200 bg-white">
          <tr v-for="item in tableData" :key="item.id" class="hover:bg-gray-50">
            <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-900">{{ item.buildingName }}</td>
            <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-900">{{ item.departmentName }}</td>
            <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">{{ getFloorRangeDesc(item) }}</td>
            <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
              {{ item.allocatedRooms || '-' }} / {{ item.allocatedBeds || '-' }}
            </td>
            <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">{{ item.priority }}</td>
            <td class="whitespace-nowrap px-4 py-3">
              <span
                :class="[
                  'inline-flex rounded-full px-2 py-0.5 text-xs font-medium',
                  item.status === 1 ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                ]"
              >
                {{ item.status === 1 ? '启用' : '禁用' }}
              </span>
            </td>
            <td class="whitespace-nowrap px-4 py-3 text-sm">
              <div class="flex gap-2">
                <button
                  @click="handleEdit(item)"
                  class="text-blue-600 hover:text-blue-800"
                >
                  编辑
                </button>
                <button
                  v-if="item.status === 0"
                  @click="handleEnable(item.id)"
                  class="text-green-600 hover:text-green-800"
                >
                  启用
                </button>
                <button
                  v-else
                  @click="handleDisable(item.id)"
                  class="text-yellow-600 hover:text-yellow-800"
                >
                  禁用
                </button>
                <button
                  @click="handleDelete(item.id)"
                  class="text-red-600 hover:text-red-800"
                >
                  删除
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="tableData.length === 0">
            <td colspan="7" class="px-4 py-8 text-center text-sm text-gray-500">
              暂无数据
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="flex items-center justify-between border-t border-gray-200 bg-white px-4 py-3">
        <div class="text-sm text-gray-500">
          共 {{ total }} 条记录
        </div>
        <div class="flex gap-2">
          <button
            @click="handlePageChange(queryForm.pageNum - 1)"
            :disabled="queryForm.pageNum <= 1"
            class="inline-flex h-8 items-center rounded border border-gray-300 bg-white px-3 text-sm text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            上一页
          </button>
          <span class="inline-flex h-8 items-center px-3 text-sm text-gray-700">
            {{ queryForm.pageNum }} / {{ Math.ceil(total / queryForm.pageSize) || 1 }}
          </span>
          <button
            @click="handlePageChange(queryForm.pageNum + 1)"
            :disabled="queryForm.pageNum >= Math.ceil(total / queryForm.pageSize)"
            class="inline-flex h-8 items-center rounded border border-gray-300 bg-white px-3 text-sm text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <div
      v-if="dialogVisible"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50"
      @click.self="dialogVisible = false"
    >
      <div class="w-[500px] rounded-lg bg-white shadow-xl">
        <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
          <h3 class="text-lg font-medium text-gray-900">{{ isEdit ? '编辑分配' : '新增分配' }}</h3>
          <button @click="dialogVisible = false" class="text-gray-400 hover:text-gray-600">
            <X class="h-5 w-5" />
          </button>
        </div>
        <div class="space-y-4 px-6 py-4">
          <div v-if="!isEdit" class="flex items-center gap-4">
            <label class="w-24 text-right text-sm text-gray-600">宿舍楼 <span class="text-red-500">*</span></label>
            <select
              v-model="formData.buildingId"
              class="flex-1 h-9 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            >
              <option value="">请选择宿舍楼</option>
              <option v-for="item in buildingOptions" :key="item.id" :value="item.id">
                {{ item.buildingName }}
              </option>
            </select>
          </div>
          <div v-if="!isEdit" class="flex items-center gap-4">
            <label class="w-24 text-right text-sm text-gray-600">院系 <span class="text-red-500">*</span></label>
            <select
              v-model="formData.departmentId"
              class="flex-1 h-9 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            >
              <option value="">请选择院系</option>
              <option v-for="item in departmentOptions" :key="item.id" :value="item.id">
                {{ item.deptName }}
              </option>
            </select>
          </div>
          <div class="flex items-center gap-4">
            <label class="w-24 text-right text-sm text-gray-600">起始楼层</label>
            <input
              v-model.number="formData.floorStart"
              type="number"
              min="1"
              placeholder="留空表示全楼"
              class="flex-1 h-9 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            />
          </div>
          <div class="flex items-center gap-4">
            <label class="w-24 text-right text-sm text-gray-600">结束楼层</label>
            <input
              v-model.number="formData.floorEnd"
              type="number"
              min="1"
              placeholder="留空表示全楼"
              class="flex-1 h-9 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            />
          </div>
          <div class="flex items-center gap-4">
            <label class="w-24 text-right text-sm text-gray-600">分配房间数</label>
            <input
              v-model.number="formData.allocatedRooms"
              type="number"
              min="1"
              placeholder="不限"
              class="flex-1 h-9 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            />
          </div>
          <div class="flex items-center gap-4">
            <label class="w-24 text-right text-sm text-gray-600">分配床位数</label>
            <input
              v-model.number="formData.allocatedBeds"
              type="number"
              min="1"
              placeholder="不限"
              class="flex-1 h-9 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            />
          </div>
          <div class="flex items-center gap-4">
            <label class="w-24 text-right text-sm text-gray-600">优先级</label>
            <input
              v-model.number="formData.priority"
              type="number"
              min="0"
              max="100"
              placeholder="0-100，数值越大优先级越高"
              class="flex-1 h-9 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            />
          </div>
          <div class="flex items-center gap-4">
            <label class="w-24 text-right text-sm text-gray-600">备注</label>
            <textarea
              v-model="formData.notes"
              rows="3"
              placeholder="请输入备注信息"
              class="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            ></textarea>
          </div>
        </div>
        <div class="flex justify-end gap-3 border-t border-gray-200 px-6 py-4">
          <button
            @click="dialogVisible = false"
            class="inline-flex h-9 items-center rounded-lg border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            取消
          </button>
          <button
            @click="handleSubmit"
            :disabled="submitting"
            class="inline-flex h-9 items-center rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {{ submitting ? '提交中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, RotateCcw, Plus, X } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getBuildingDepartmentAssignmentList,
  createBuildingDepartmentAssignment,
  updateBuildingDepartmentAssignment,
  deleteBuildingDepartmentAssignment,
  enableBuildingDepartmentAssignment,
  disableBuildingDepartmentAssignment
} from '@/api/v2/dormitory'
import { getDormitoryBuildingList } from '@/api/v2/dormitory'
import { getAllEnabledDepartments } from '@/api/v2/organization'
import type { BuildingDepartmentAssignment } from '@/types/buildingDepartmentAssignment'

// 查询表单
const queryForm = reactive({
  pageNum: 1,
  pageSize: 10,
  buildingId: '' as number | '',
  departmentId: '' as number | '',
  status: '' as number | ''
})

// 表格数据
const tableData = ref<BuildingDepartmentAssignment[]>([])
const total = ref(0)
const loading = ref(false)

// 下拉选项
const buildingOptions = ref<Array<{ id: number; buildingName: string }>>([])
const departmentOptions = ref<Array<{ id: number; deptName: string }>>([])

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formData = reactive({
  id: undefined as number | undefined,
  buildingId: '' as number | '',
  departmentId: '' as number | '',
  floorStart: undefined as number | undefined,
  floorEnd: undefined as number | undefined,
  allocatedRooms: undefined as number | undefined,
  allocatedBeds: undefined as number | undefined,
  priority: 0,
  notes: ''
})

// 获取楼层范围描述
function getFloorRangeDesc(item: BuildingDepartmentAssignment): string {
  if (item.floorRangeDesc) return item.floorRangeDesc
  if (item.floorStart == null && item.floorEnd == null) return '全楼'
  if (item.floorStart != null && item.floorEnd != null) {
    if (item.floorStart === item.floorEnd) return `${item.floorStart}楼`
    return `${item.floorStart}-${item.floorEnd}楼`
  }
  if (item.floorStart != null) return `${item.floorStart}楼及以上`
  return `${item.floorEnd}楼及以下`
}

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const params = {
      pageNum: queryForm.pageNum,
      pageSize: queryForm.pageSize,
      buildingId: queryForm.buildingId || undefined,
      departmentId: queryForm.departmentId || undefined,
      status: queryForm.status !== '' ? queryForm.status : undefined
    }
    const res = await getBuildingDepartmentAssignmentList(params)
    tableData.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 加载下拉选项
async function loadOptions() {
  try {
    const [buildingsRes, deptsRes] = await Promise.all([
      getDormitoryBuildingList({ pageNum: 1, pageSize: 1000 }),
      getAllEnabledDepartments()
    ])
    buildingOptions.value = buildingsRes.records || []
    departmentOptions.value = deptsRes || []
  } catch (error) {
    console.error('加载选项失败:', error)
  }
}

// 查询
function handleQuery() {
  queryForm.pageNum = 1
  loadData()
}

// 重置
function handleReset() {
  queryForm.buildingId = ''
  queryForm.departmentId = ''
  queryForm.status = ''
  queryForm.pageNum = 1
  loadData()
}

// 分页
function handlePageChange(page: number) {
  queryForm.pageNum = page
  loadData()
}

// 新增
function handleAdd() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    buildingId: '',
    departmentId: '',
    floorStart: undefined,
    floorEnd: undefined,
    allocatedRooms: undefined,
    allocatedBeds: undefined,
    priority: 0,
    notes: ''
  })
  dialogVisible.value = true
}

// 编辑
function handleEdit(row: BuildingDepartmentAssignment) {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    buildingId: row.buildingId,
    departmentId: row.departmentId,
    floorStart: row.floorStart,
    floorEnd: row.floorEnd,
    allocatedRooms: row.allocatedRooms,
    allocatedBeds: row.allocatedBeds,
    priority: row.priority,
    notes: row.notes || ''
  })
  dialogVisible.value = true
}

// 提交
async function handleSubmit() {
  // 校验
  if (!isEdit.value) {
    if (!formData.buildingId) {
      ElMessage.warning('请选择宿舍楼')
      return
    }
    if (!formData.departmentId) {
      ElMessage.warning('请选择院系')
      return
    }
  }
  if (formData.floorStart && formData.floorEnd && formData.floorStart > formData.floorEnd) {
    ElMessage.warning('起始楼层不能大于结束楼层')
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateBuildingDepartmentAssignment({
        id: formData.id!,
        floorStart: formData.floorStart || null,
        floorEnd: formData.floorEnd || null,
        allocatedRooms: formData.allocatedRooms || null,
        allocatedBeds: formData.allocatedBeds || null,
        priority: formData.priority,
        notes: formData.notes
      })
      ElMessage.success('更新成功')
    } else {
      await createBuildingDepartmentAssignment({
        buildingId: formData.buildingId as number,
        departmentId: formData.departmentId as number,
        floorStart: formData.floorStart || null,
        floorEnd: formData.floorEnd || null,
        allocatedRooms: formData.allocatedRooms || null,
        allocatedBeds: formData.allocatedBeds || null,
        priority: formData.priority,
        notes: formData.notes
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

// 启用
async function handleEnable(id: number) {
  try {
    await enableBuildingDepartmentAssignment(id)
    ElMessage.success('启用成功')
    loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '启用失败')
  }
}

// 禁用
async function handleDisable(id: number) {
  try {
    await disableBuildingDepartmentAssignment(id)
    ElMessage.success('禁用成功')
    loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '禁用失败')
  }
}

// 删除
async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该分配吗？', '提示', {
      type: 'warning'
    })
    await deleteBuildingDepartmentAssignment(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadOptions()
  loadData()
})
</script>
