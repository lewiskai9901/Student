<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 搜索栏 -->
    <div class="mb-6 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-700">周期编码</label>
          <input v-model="queryParams.periodCode" type="text" placeholder="请输入周期编码" class="h-9 w-40 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @keyup.enter="handleSearch" />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-700">周期名称</label>
          <input v-model="queryParams.periodName" type="text" placeholder="请输入周期名称" class="h-9 w-40 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @keyup.enter="handleSearch" />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-700">学年</label>
          <input v-model="queryParams.academicYear" type="text" placeholder="如：2024-2025" class="h-9 w-36 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-700">状态</label>
          <select v-model="queryParams.status" class="h-9 w-32 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
            <option :value="undefined">全部</option>
            <option :value="0">未开始</option>
            <option :value="1">数据采集</option>
            <option :value="2">荣誉申报</option>
            <option :value="3">审核中</option>
            <option :value="4">公示中</option>
            <option :value="5">申诉处理</option>
            <option :value="6">已结束</option>
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
        <h3 class="font-medium text-gray-900">综测周期列表</h3>
        <button class="flex h-9 items-center gap-1 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700" @click="handleAdd">
          <Plus class="h-4 w-4" />新增周期
        </button>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-gray-50">
            <tr>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">周期编码</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">周期名称</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">学年</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">学期</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">状态</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">锁定</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">创建时间</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-if="loading"><td colspan="8" class="px-4 py-8 text-center text-sm text-gray-500">加载中...</td></tr>
            <tr v-else-if="tableData.length === 0"><td colspan="8" class="px-4 py-8 text-center text-sm text-gray-500">暂无数据</td></tr>
            <tr v-for="row in tableData" :key="row.id" class="hover:bg-gray-50">
              <td class="px-4 py-3 text-sm text-gray-900">{{ row.periodCode }}</td>
              <td class="px-4 py-3 text-sm text-gray-900">{{ row.periodName }}</td>
              <td class="px-4 py-3 text-sm text-gray-900">{{ row.academicYear }}</td>
              <td class="px-4 py-3 text-sm text-gray-900">{{ row.semesterName }}</td>
              <td class="px-4 py-3 text-center">
                <span :class="['inline-flex rounded-full px-2 py-0.5 text-xs', getStatusClass(row.status)]">{{ getStatusText(row.status) }}</span>
              </td>
              <td class="px-4 py-3 text-center">
                <span :class="['inline-flex rounded-full px-2 py-0.5 text-xs', row.isLocked === 1 ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700']">
                  {{ row.isLocked === 1 ? '已锁定' : '未锁定' }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-500">{{ row.createTime }}</td>
              <td class="px-4 py-3 text-center">
                <div class="flex items-center justify-center gap-2">
                  <button class="text-sm text-blue-600 hover:text-blue-700" @click="handleEdit(row)">编辑</button>
                  <div class="relative" v-click-outside="() => closeDropdown(row.id)">
                    <button class="flex items-center gap-1 text-sm text-blue-600 hover:text-blue-700" @click="toggleDropdown(row.id)">
                      状态操作<ChevronDown class="h-3 w-3" />
                    </button>
                    <div v-if="openDropdownId === row.id" class="absolute right-0 top-6 z-10 w-32 rounded-lg border border-gray-200 bg-white py-1 shadow-lg">
                      <button class="w-full px-3 py-1.5 text-left text-sm hover:bg-gray-50 disabled:opacity-50" :disabled="row.status !== 0" @click="handleCommand('startDataCollection', row)">开始数据采集</button>
                      <button class="w-full px-3 py-1.5 text-left text-sm hover:bg-gray-50 disabled:opacity-50" :disabled="row.status !== 1" @click="handleCommand('startApplication', row)">开始申报</button>
                      <button class="w-full px-3 py-1.5 text-left text-sm hover:bg-gray-50 disabled:opacity-50" :disabled="row.status !== 2" @click="handleCommand('startReview', row)">开始审核</button>
                      <button class="w-full px-3 py-1.5 text-left text-sm hover:bg-gray-50 disabled:opacity-50" :disabled="row.status !== 3" @click="handleCommand('startPublicity', row)">开始公示</button>
                      <button class="w-full px-3 py-1.5 text-left text-sm hover:bg-gray-50 disabled:opacity-50" :disabled="row.status >= 6" @click="handleCommand('finish', row)">结束周期</button>
                      <div class="my-1 border-t border-gray-200"></div>
                      <button class="w-full px-3 py-1.5 text-left text-sm hover:bg-gray-50 disabled:opacity-50" :disabled="row.isLocked === 1" @click="handleCommand('lock', row)">锁定</button>
                      <button class="w-full px-3 py-1.5 text-left text-sm hover:bg-gray-50 disabled:opacity-50" :disabled="row.isLocked === 0" @click="handleCommand('unlock', row)">解锁</button>
                    </div>
                  </div>
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
        <div class="relative w-full max-w-xl rounded-lg bg-white p-6 shadow-xl max-h-[90vh] overflow-y-auto">
          <div class="mb-6 flex items-center justify-between">
            <h3 class="text-lg font-medium text-gray-900">{{ dialogType === 'add' ? '新增综测周期' : '编辑综测周期' }}</h3>
            <button class="text-gray-400 hover:text-gray-500" @click="dialogVisible = false"><X class="h-5 w-5" /></button>
          </div>
          <form @submit.prevent="handleSubmit" class="space-y-4">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="mb-1 block text-sm text-gray-700">周期编码<span class="text-red-500">*</span></label>
                <input v-model="formData.periodCode" type="text" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" placeholder="请输入周期编码" />
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-700">周期名称<span class="text-red-500">*</span></label>
                <input v-model="formData.periodName" type="text" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" placeholder="请输入周期名称" />
              </div>
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-700">学期<span class="text-red-500">*</span></label>
              <select v-model="formData.semesterId" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
                <option :value="undefined" disabled>请选择学期</option>
                <option v-for="item in semesterList" :key="item.id" :value="item.id">{{ item.semesterName }}</option>
              </select>
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-700">数据采集时间</label>
              <div class="grid grid-cols-2 gap-2">
                <input v-model="formData.dataCollectionTime[0]" type="datetime-local" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
                <input v-model="formData.dataCollectionTime[1]" type="datetime-local" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-700">申报时间</label>
              <div class="grid grid-cols-2 gap-2">
                <input v-model="formData.applicationTime[0]" type="datetime-local" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
                <input v-model="formData.applicationTime[1]" type="datetime-local" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-700">审核时间</label>
              <div class="grid grid-cols-2 gap-2">
                <input v-model="formData.reviewTime[0]" type="datetime-local" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
                <input v-model="formData.reviewTime[1]" type="datetime-local" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-700">公示时间</label>
              <div class="grid grid-cols-2 gap-2">
                <input v-model="formData.publicityTime[0]" type="datetime-local" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
                <input v-model="formData.publicityTime[1]" type="datetime-local" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
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
import { Plus, X, ChevronDown, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { pagePeriods, createPeriod, updatePeriod, deletePeriod, startDataCollection, startApplication, startReview, startPublicity, finishPeriod, lockPeriod, unlockPeriod, listAllSemesters, type EvaluationPeriod, type Semester } from '@/api/evaluation'

const vClickOutside = { mounted(el: any, binding: any) { el._clickOutside = (e: Event) => { if (!el.contains(e.target)) binding.value() }; document.addEventListener('click', el._clickOutside) }, unmounted(el: any) { document.removeEventListener('click', el._clickOutside) } }

const queryParams = reactive({ pageNum: 1, pageSize: 10, periodCode: '', periodName: '', academicYear: '', status: undefined as number | undefined })
const loading = ref(false), tableData = ref<EvaluationPeriod[]>([]), total = ref(0), dialogVisible = ref(false), dialogType = ref<'add' | 'edit'>('add'), submitLoading = ref(false), semesterList = ref<Semester[]>([]), openDropdownId = ref<number | null>(null)

const formData = reactive({ id: undefined as number | undefined, periodCode: '', periodName: '', semesterId: undefined as number | undefined, dataCollectionTime: [] as string[], applicationTime: [] as string[], reviewTime: [] as string[], publicityTime: [] as string[], description: '' })

const getStatusClass = (status: number) => {
  const map: Record<number, string> = { 0: 'bg-gray-100 text-gray-700', 1: 'bg-amber-100 text-amber-700', 2: 'bg-blue-100 text-blue-700', 3: 'bg-amber-100 text-amber-700', 4: 'bg-green-100 text-green-700', 5: 'bg-red-100 text-red-700', 6: 'bg-gray-100 text-gray-700' }
  return map[status] || 'bg-gray-100 text-gray-700'
}
const getStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '未开始', 1: '数据采集', 2: '荣誉申报', 3: '审核中', 4: '公示中', 5: '申诉处理', 6: '已结束' }
  return map[status] || '未知'
}

const fetchData = async () => {
  loading.value = true
  try { const res = await pagePeriods(queryParams); tableData.value = res.data?.records || []; total.value = res.data?.total || 0 } finally { loading.value = false }
}
const handleSearch = () => { queryParams.pageNum = 1; fetchData() }
const handleReset = () => { queryParams.periodCode = ''; queryParams.periodName = ''; queryParams.academicYear = ''; queryParams.status = undefined; handleSearch() }

const toggleDropdown = (id: number) => { openDropdownId.value = openDropdownId.value === id ? null : id }
const closeDropdown = (id: number) => { if (openDropdownId.value === id) openDropdownId.value = null }

const handleAdd = () => { dialogType.value = 'add'; resetForm(); dialogVisible.value = true }
const handleEdit = (row: EvaluationPeriod) => {
  dialogType.value = 'edit'
  Object.assign(formData, {
    id: row.id, periodCode: row.periodCode, periodName: row.periodName, semesterId: row.semesterId,
    dataCollectionTime: row.dataCollectionStartTime && row.dataCollectionEndTime ? [row.dataCollectionStartTime.replace(' ', 'T'), row.dataCollectionEndTime.replace(' ', 'T')] : [],
    applicationTime: row.applicationStartTime && row.applicationEndTime ? [row.applicationStartTime.replace(' ', 'T'), row.applicationEndTime.replace(' ', 'T')] : [],
    reviewTime: row.reviewStartTime && row.reviewEndTime ? [row.reviewStartTime.replace(' ', 'T'), row.reviewEndTime.replace(' ', 'T')] : [],
    publicityTime: row.publicityStartTime && row.publicityEndTime ? [row.publicityStartTime.replace(' ', 'T'), row.publicityEndTime.replace(' ', 'T')] : [],
    description: row.description || ''
  })
  dialogVisible.value = true
}
const handleDelete = async (row: EvaluationPeriod) => {
  try { await ElMessageBox.confirm('确定要删除该周期吗？', '提示', { type: 'warning' }); await deletePeriod(row.id!); ElMessage.success('删除成功'); fetchData() } catch {}
}

const handleCommand = async (command: string, row: EvaluationPeriod) => {
  const actions: Record<string, () => Promise<any>> = { startDataCollection: () => startDataCollection(row.id!), startApplication: () => startApplication(row.id!), startReview: () => startReview(row.id!), startPublicity: () => startPublicity(row.id!), finish: () => finishPeriod(row.id!), lock: () => lockPeriod(row.id!), unlock: () => unlockPeriod(row.id!) }
  const names: Record<string, string> = { startDataCollection: '开始数据采集', startApplication: '开始申报', startReview: '开始审核', startPublicity: '开始公示', finish: '结束周期', lock: '锁定', unlock: '解锁' }
  try { await ElMessageBox.confirm(`确定要${names[command]}吗？`, '提示', { type: 'warning' }); await actions[command](); ElMessage.success('操作成功'); openDropdownId.value = null; fetchData() } catch {}
}

const handleSubmit = async () => {
  submitLoading.value = true
  try {
    const data: EvaluationPeriod = {
      periodCode: formData.periodCode, periodName: formData.periodName, semesterId: formData.semesterId!,
      dataCollectionStartTime: formData.dataCollectionTime?.[0]?.replace('T', ' '), dataCollectionEndTime: formData.dataCollectionTime?.[1]?.replace('T', ' '),
      applicationStartTime: formData.applicationTime?.[0]?.replace('T', ' '), applicationEndTime: formData.applicationTime?.[1]?.replace('T', ' '),
      reviewStartTime: formData.reviewTime?.[0]?.replace('T', ' '), reviewEndTime: formData.reviewTime?.[1]?.replace('T', ' '),
      publicityStartTime: formData.publicityTime?.[0]?.replace('T', ' '), publicityEndTime: formData.publicityTime?.[1]?.replace('T', ' '),
      description: formData.description
    }
    if (dialogType.value === 'add') { await createPeriod(data); ElMessage.success('新增成功') } else { await updatePeriod(formData.id!, data); ElMessage.success('更新成功') }
    dialogVisible.value = false; fetchData()
  } finally { submitLoading.value = false }
}

const resetForm = () => { formData.id = undefined; formData.periodCode = ''; formData.periodName = ''; formData.semesterId = undefined; formData.dataCollectionTime = []; formData.applicationTime = []; formData.reviewTime = []; formData.publicityTime = []; formData.description = '' }

const loadSemesterList = async () => { try { const res = await listAllSemesters(); semesterList.value = res.data || [] } catch (e) { console.error('加载学期列表失败', e) } }

onMounted(() => { fetchData(); loadSemesterList() })
</script>
