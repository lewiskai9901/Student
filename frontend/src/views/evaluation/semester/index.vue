<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 搜索栏 -->
    <div class="mb-6 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-700">学期编码</label>
          <input v-model="queryParams.semesterCode" type="text" placeholder="请输入学期编码" class="h-9 w-40 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @keyup.enter="handleSearch" />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-700">学期名称</label>
          <input v-model="queryParams.semesterName" type="text" placeholder="请输入学期名称" class="h-9 w-40 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @keyup.enter="handleSearch" />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-700">学年</label>
          <input v-model="queryParams.academicYear" type="text" placeholder="如：2024-2025" class="h-9 w-36 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-700">学期类型</label>
          <select v-model="queryParams.semesterType" class="h-9 w-28 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
            <option :value="undefined">全部</option>
            <option :value="1">第一学期</option>
            <option :value="2">第二学期</option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-700">状态</label>
          <select v-model="queryParams.status" class="h-9 w-24 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
            <option :value="undefined">全部</option>
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
        </div>
        <div class="flex gap-2">
          <button class="h-9 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700" @click="handleSearch">查询</button>
          <button class="h-9 rounded-lg border border-gray-200 px-4 text-sm text-gray-700 hover:bg-gray-50" @click="handleReset">重置</button>
        </div>
      </div>
    </div>

    <!-- 表格区域 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <h3 class="font-medium text-gray-900">学期列表</h3>
        <button class="flex h-9 items-center gap-1 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700" @click="handleAdd">
          <Plus class="h-4 w-4" />新增学期
        </button>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-gray-50">
            <tr>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">学期编码</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">学期名称</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">学年</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">学期类型</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">开始日期</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">结束日期</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">当前学期</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">状态</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-if="loading"><td colspan="9" class="px-4 py-8 text-center text-sm text-gray-500">加载中...</td></tr>
            <tr v-else-if="tableData.length === 0"><td colspan="9" class="px-4 py-8 text-center text-sm text-gray-500">暂无数据</td></tr>
            <tr v-for="row in tableData" :key="row.id" class="hover:bg-gray-50">
              <td class="px-4 py-3 text-sm text-gray-900">{{ row.semesterCode }}</td>
              <td class="px-4 py-3 text-sm text-gray-900">{{ row.semesterName }}</td>
              <td class="px-4 py-3 text-sm text-gray-900">{{ row.academicYear }}</td>
              <td class="px-4 py-3 text-center">
                <span :class="['inline-flex rounded-full px-2 py-0.5 text-xs', row.semesterType === 1 ? 'bg-blue-100 text-blue-700' : 'bg-green-100 text-green-700']">
                  {{ row.semesterType === 1 ? '第一学期' : '第二学期' }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-500">{{ row.startDate }}</td>
              <td class="px-4 py-3 text-sm text-gray-500">{{ row.endDate }}</td>
              <td class="px-4 py-3 text-center">
                <span v-if="row.isCurrent === 1" class="inline-flex rounded-full bg-red-100 px-2 py-0.5 text-xs text-red-700">当前</span>
                <span v-else class="text-sm text-gray-400">-</span>
              </td>
              <td class="px-4 py-3 text-center">
                <span :class="['inline-flex rounded-full px-2 py-0.5 text-xs', row.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700']">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="px-4 py-3 text-center">
                <div class="flex items-center justify-center gap-2">
                  <button class="text-sm text-blue-600 hover:text-blue-700" @click="handleEdit(row)">编辑</button>
                  <button class="text-sm text-green-600 hover:text-green-700" :disabled="row.isCurrent === 1" :class="{ 'opacity-50 cursor-not-allowed': row.isCurrent === 1 }" @click="handleSetCurrent(row)">设为当前</button>
                  <button class="text-sm text-red-600 hover:text-red-700" @click="handleDelete(row)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <!-- 分页 -->
      <div class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <span class="text-sm text-gray-500">共 {{ total }} 条</span>
        <div class="flex items-center gap-2">
          <button class="h-8 w-8 rounded border border-gray-200 text-sm disabled:opacity-50" :disabled="queryParams.pageNum <= 1" @click="queryParams.pageNum--; fetchData()">
            <ChevronLeft class="mx-auto h-4 w-4" />
          </button>
          <span class="text-sm text-gray-700">{{ queryParams.pageNum }} / {{ Math.ceil(total / queryParams.pageSize) || 1 }}</span>
          <button class="h-8 w-8 rounded border border-gray-200 text-sm disabled:opacity-50" :disabled="queryParams.pageNum >= Math.ceil(total / queryParams.pageSize)" @click="queryParams.pageNum++; fetchData()">
            <ChevronRight class="mx-auto h-4 w-4" />
          </button>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <Teleport to="body">
      <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="dialogVisible = false"></div>
        <div class="relative w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
          <div class="mb-6 flex items-center justify-between">
            <h3 class="text-lg font-medium text-gray-900">{{ dialogType === 'add' ? '新增学期' : '编辑学期' }}</h3>
            <button class="text-gray-400 hover:text-gray-500" @click="dialogVisible = false"><X class="h-5 w-5" /></button>
          </div>
          <form @submit.prevent="handleSubmit" class="space-y-4">
            <div>
              <label class="mb-1 block text-sm text-gray-700">学期编码<span class="text-red-500">*</span></label>
              <input v-model="formData.semesterCode" type="text" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" placeholder="如：2024-2025-1" />
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-700">学期名称<span class="text-red-500">*</span></label>
              <input v-model="formData.semesterName" type="text" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" placeholder="如：2024-2025学年第一学期" />
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="mb-1 block text-sm text-gray-700">学年<span class="text-red-500">*</span></label>
                <input v-model="formData.academicYear" type="text" required pattern="\d{4}-\d{4}" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" placeholder="如：2024-2025" />
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-700">学期类型<span class="text-red-500">*</span></label>
                <select v-model="formData.semesterType" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
                  <option :value="1">第一学期</option>
                  <option :value="2">第二学期</option>
                </select>
              </div>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="mb-1 block text-sm text-gray-700">开始日期<span class="text-red-500">*</span></label>
                <input v-model="formData.startDate" type="date" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-700">结束日期<span class="text-red-500">*</span></label>
                <input v-model="formData.endDate" type="date" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-700">状态</label>
              <div class="flex gap-4">
                <label class="flex items-center gap-2"><input type="radio" v-model="formData.status" :value="1" class="text-blue-600" /><span class="text-sm">启用</span></label>
                <label class="flex items-center gap-2"><input type="radio" v-model="formData.status" :value="0" class="text-blue-600" /><span class="text-sm">禁用</span></label>
              </div>
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-700">描述</label>
              <textarea v-model="formData.description" rows="3" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none" placeholder="请输入描述"></textarea>
            </div>
            <div class="flex justify-end gap-3 pt-4">
              <button type="button" class="h-9 rounded-lg border border-gray-200 px-4 text-sm text-gray-700 hover:bg-gray-50" @click="dialogVisible = false">取消</button>
              <button type="submit" :disabled="submitLoading" class="h-9 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700 disabled:opacity-50">
                {{ submitLoading ? '保存中...' : '确定' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, X, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { pageSemesters, createSemester, updateSemester, deleteSemester, setCurrentSemester, type Semester } from '@/api/evaluation'

const queryParams = reactive({ pageNum: 1, pageSize: 10, semesterCode: '', semesterName: '', academicYear: '', semesterType: undefined as number | undefined, status: undefined as number | undefined })
const loading = ref(false), tableData = ref<Semester[]>([]), total = ref(0), dialogVisible = ref(false), dialogType = ref<'add' | 'edit'>('add'), submitLoading = ref(false)
const formData = reactive({ id: undefined as number | undefined, semesterCode: '', semesterName: '', academicYear: '', semesterType: 1, startDate: '', endDate: '', status: 1, description: '' })

const fetchData = async () => {
  loading.value = true
  try { const res = await pageSemesters(queryParams); tableData.value = res.data?.records || []; total.value = res.data?.total || 0 } finally { loading.value = false }
}
const handleSearch = () => { queryParams.pageNum = 1; fetchData() }
const handleReset = () => { queryParams.semesterCode = ''; queryParams.semesterName = ''; queryParams.academicYear = ''; queryParams.semesterType = undefined; queryParams.status = undefined; handleSearch() }

const handleAdd = () => { dialogType.value = 'add'; resetForm(); dialogVisible.value = true }
const handleEdit = (row: Semester) => {
  dialogType.value = 'edit'
  Object.assign(formData, { id: row.id, semesterCode: row.semesterCode, semesterName: row.semesterName, academicYear: row.academicYear, semesterType: row.semesterType, startDate: row.startDate, endDate: row.endDate, status: row.status ?? 1, description: row.description || '' })
  dialogVisible.value = true
}
const handleDelete = async (row: Semester) => {
  try { await ElMessageBox.confirm('确定要删除该学期吗？删除后不可恢复。', '提示', { type: 'warning' }); await deleteSemester(row.id!); ElMessage.success('删除成功'); fetchData() } catch {}
}
const handleSetCurrent = async (row: Semester) => {
  try { await ElMessageBox.confirm(`确定要将"${row.semesterName}"设为当前学期吗？`, '提示', { type: 'warning' }); await setCurrentSemester(row.id!); ElMessage.success('设置成功'); fetchData() } catch {}
}

const handleSubmit = async () => {
  submitLoading.value = true
  try {
    const data: Semester = { semesterCode: formData.semesterCode, semesterName: formData.semesterName, academicYear: formData.academicYear, semesterType: formData.semesterType, startDate: formData.startDate, endDate: formData.endDate, status: formData.status, description: formData.description }
    if (dialogType.value === 'add') { await createSemester(data); ElMessage.success('新增成功') } else { await updateSemester(formData.id!, data); ElMessage.success('更新成功') }
    dialogVisible.value = false; fetchData()
  } finally { submitLoading.value = false }
}

const resetForm = () => { formData.id = undefined; formData.semesterCode = ''; formData.semesterName = ''; formData.academicYear = ''; formData.semesterType = 1; formData.startDate = ''; formData.endDate = ''; formData.status = 1; formData.description = '' }

onMounted(() => fetchData())
</script>
