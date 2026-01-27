<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 搜索栏 -->
    <div class="mb-4 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-center gap-3">
        <input v-model="searchForm.behaviorCode" type="text" placeholder="行为编码" class="h-9 w-36 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @keyup.enter="handleSearch" />
        <input v-model="searchForm.behaviorName" type="text" placeholder="行为名称" class="h-9 w-36 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @keyup.enter="handleSearch" />
        <select v-model="searchForm.behaviorCategory" class="h-9 w-28 rounded-lg border border-gray-200 px-2 text-sm focus:border-blue-500 focus:outline-none">
          <option value="">全部类别</option>
          <option v-for="item in categoryOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
        <select v-model="searchForm.behaviorNature" class="h-9 w-28 rounded-lg border border-gray-200 px-2 text-sm focus:border-blue-500 focus:outline-none">
          <option :value="undefined">全部性质</option>
          <option v-for="item in natureOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
        <select v-model="searchForm.status" class="h-9 w-24 rounded-lg border border-gray-200 px-2 text-sm focus:border-blue-500 focus:outline-none">
          <option :value="undefined">全部状态</option>
          <option :value="1">启用</option>
          <option :value="0">禁用</option>
        </select>
        <button class="h-9 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700" @click="handleSearch">查询</button>
        <button class="h-9 rounded-lg border border-gray-200 bg-white px-4 text-sm text-gray-600 hover:bg-gray-50" @click="handleReset">重置</button>
        <div class="flex-1"></div>
        <button class="flex h-9 items-center gap-1 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700" @click="handleAdd">
          <Plus class="h-4 w-4" />新增
        </button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <table class="w-full">
        <thead class="bg-gray-50">
          <tr class="border-b border-gray-200">
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">行为编码</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">行为名称</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">类别</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">性质</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">影响范围</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">排序</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">状态</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">描述</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">操作</th>
          </tr>
        </thead>
        <tbody v-if="!loading">
          <tr v-for="row in tableData" :key="row.id" class="border-b border-gray-100 hover:bg-gray-50">
            <td class="px-4 py-3 text-sm font-mono text-gray-600">{{ row.behaviorCode }}</td>
            <td class="px-4 py-3 text-sm text-gray-900">{{ row.behaviorName }}</td>
            <td class="px-4 py-3 text-center">
              <span :class="['inline-block rounded px-2 py-0.5 text-xs', getCategoryClass(row.behaviorCategory)]">{{ getCategoryLabel(row.behaviorCategory) }}</span>
            </td>
            <td class="px-4 py-3 text-center">
              <span :class="['inline-block rounded px-2 py-0.5 text-xs', getNatureClass(row.behaviorNature)]">{{ getNatureLabel(row.behaviorNature) }}</span>
            </td>
            <td class="px-4 py-3 text-center text-sm text-gray-600">{{ getScopeLabel(row.defaultAffectScope) }}</td>
            <td class="px-4 py-3 text-center text-sm text-gray-600">{{ row.sortOrder }}</td>
            <td class="px-4 py-3 text-center">
              <span :class="['inline-block rounded px-2 py-0.5 text-xs', row.status === 1 ? 'bg-green-50 text-green-700' : 'bg-gray-100 text-gray-600']">{{ row.status === 1 ? '启用' : '禁用' }}</span>
            </td>
            <td class="max-w-[200px] truncate px-4 py-3 text-sm text-gray-500" :title="row.description">{{ row.description || '-' }}</td>
            <td class="px-4 py-3 text-center">
              <button class="mr-2 text-sm text-blue-600 hover:text-blue-800" @click="handleEdit(row)">编辑</button>
              <button class="text-sm text-red-600 hover:text-red-800" @click="handleDelete(row)">删除</button>
            </td>
          </tr>
          <tr v-if="tableData.length === 0">
            <td colspan="9" class="py-12 text-center text-sm text-gray-400">暂无数据</td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr><td colspan="9" class="py-12 text-center text-sm text-gray-400">加载中...</td></tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <div class="text-sm text-gray-500">共 {{ pagination.total }} 条</div>
        <div class="flex items-center gap-2">
          <button class="flex h-8 w-8 items-center justify-center rounded border border-gray-200 hover:bg-gray-50 disabled:opacity-50" :disabled="pagination.pageNum <= 1" @click="handlePageChange(pagination.pageNum - 1)">
            <ChevronLeft class="h-4 w-4" />
          </button>
          <span class="px-2 text-sm">{{ pagination.pageNum }} / {{ Math.ceil(pagination.total / pagination.pageSize) || 1 }}</span>
          <button class="flex h-8 w-8 items-center justify-center rounded border border-gray-200 hover:bg-gray-50 disabled:opacity-50" :disabled="pagination.pageNum >= Math.ceil(pagination.total / pagination.pageSize)" @click="handlePageChange(pagination.pageNum + 1)">
            <ChevronRight class="h-4 w-4" />
          </button>
          <select v-model="pagination.pageSize" class="pagination-select" @change="handleSizeChange">
            <option :value="10">10条/页</option>
            <option :value="20">20条/页</option>
            <option :value="50">50条/页</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <Teleport to="body">
      <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="dialogVisible = false"></div>
        <div class="relative w-full max-w-lg rounded-lg bg-white shadow-xl">
          <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
            <h3 class="text-lg font-medium text-gray-900">{{ formData.id ? '编辑行为类型' : '新增行为类型' }}</h3>
            <button class="text-gray-400 hover:text-gray-600" @click="dialogVisible = false"><X class="h-5 w-5" /></button>
          </div>
          <div class="max-h-[60vh] overflow-y-auto p-6">
            <div class="space-y-4">
              <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">行为编码 <span class="text-red-500">*</span></label>
                <input v-model="formData.behaviorCode" type="text" placeholder="如：LATE" :disabled="!!formData.id" :class="['h-9 w-full rounded-lg border px-3 text-sm focus:outline-none', formData.id ? 'bg-gray-100 border-gray-200 cursor-not-allowed' : 'border-gray-200 focus:border-blue-500']" />
                <p class="mt-1 text-xs text-gray-400">只能包含大写字母、数字和下划线，以大写字母开头</p>
              </div>
              <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">行为名称 <span class="text-red-500">*</span></label>
                <input v-model="formData.behaviorName" type="text" placeholder="请输入行为名称" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-1 block text-sm font-medium text-gray-700">行为类别 <span class="text-red-500">*</span></label>
                  <select v-model="formData.behaviorCategory" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
                    <option value="">请选择</option>
                    <option v-for="item in categoryOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
                  </select>
                </div>
                <div>
                  <label class="mb-1 block text-sm font-medium text-gray-700">影响范围 <span class="text-red-500">*</span></label>
                  <select v-model="formData.defaultAffectScope" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
                    <option v-for="item in scopeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
                  </select>
                </div>
              </div>
              <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">行为性质 <span class="text-red-500">*</span></label>
                <div class="flex gap-4">
                  <label v-for="item in natureOptions" :key="item.value" class="flex cursor-pointer items-center gap-2">
                    <input type="radio" :value="item.value" v-model="formData.behaviorNature" class="h-4 w-4 text-blue-600" />
                    <span :class="['rounded px-2 py-0.5 text-sm', getNatureClass(item.value)]">{{ item.label }}</span>
                  </label>
                </div>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-1 block text-sm font-medium text-gray-700">排序</label>
                  <input type="number" v-model="formData.sortOrder" min="0" max="9999" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
                </div>
                <div>
                  <label class="mb-1 block text-sm font-medium text-gray-700">状态</label>
                  <div class="flex gap-4 pt-2">
                    <label class="flex cursor-pointer items-center gap-2">
                      <input type="radio" :value="1" v-model="formData.status" class="h-4 w-4 text-blue-600" />
                      <span class="text-sm text-gray-700">启用</span>
                    </label>
                    <label class="flex cursor-pointer items-center gap-2">
                      <input type="radio" :value="0" v-model="formData.status" class="h-4 w-4 text-blue-600" />
                      <span class="text-sm text-gray-700">禁用</span>
                    </label>
                  </div>
                </div>
              </div>
              <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">描述</label>
                <textarea v-model="formData.description" rows="3" placeholder="请输入描述" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"></textarea>
              </div>
            </div>
          </div>
          <div class="flex justify-end gap-3 border-t border-gray-200 px-6 py-4">
            <button class="h-9 rounded-lg border border-gray-200 px-4 text-sm text-gray-600 hover:bg-gray-50" @click="dialogVisible = false">取消</button>
            <button class="h-9 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700 disabled:opacity-50" :disabled="submitLoading" @click="handleSubmit">{{ submitLoading ? '提交中...' : '确定' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, X, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageBehaviorTypes, createBehaviorType, updateBehaviorType, deleteBehaviorType, type BehaviorType } from '@/api/evaluation'

const categoryOptions = [
  { value: 'ATTENDANCE', label: '考勤' },
  { value: 'DISCIPLINE', label: '纪律' },
  { value: 'HYGIENE', label: '卫生' },
  { value: 'STUDY', label: '学习' },
  { value: 'ACTIVITY', label: '活动' },
  { value: 'HONOR', label: '荣誉' }
]
const natureOptions = [
  { value: 1, label: '正向' },
  { value: 2, label: '负向' },
  { value: 3, label: '中性' }
]
const scopeOptions = [
  { value: 1, label: '仅当事人' },
  { value: 2, label: '宿舍全员' },
  { value: 3, label: '班级全员' }
]

const searchForm = reactive({ behaviorCode: '', behaviorName: '', behaviorCategory: '', behaviorNature: undefined as number | undefined, status: undefined as number | undefined })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const tableData = ref<BehaviorType[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formData = reactive<BehaviorType>({ behaviorCode: '', behaviorName: '', behaviorCategory: '', behaviorNature: 2, defaultAffectScope: 1, description: '', sortOrder: 0, status: 1 })

const getCategoryClass = (cat: string) => {
  const map: Record<string, string> = { ATTENDANCE: 'bg-blue-50 text-blue-700', DISCIPLINE: 'bg-red-50 text-red-700', HYGIENE: 'bg-amber-50 text-amber-700', STUDY: 'bg-green-50 text-green-700', ACTIVITY: 'bg-purple-50 text-purple-700', HONOR: 'bg-pink-50 text-pink-700' }
  return map[cat] || 'bg-gray-100 text-gray-700'
}
const getCategoryLabel = (cat: string) => categoryOptions.find(o => o.value === cat)?.label || cat
const getNatureClass = (n: number) => ({ 1: 'bg-green-50 text-green-700', 2: 'bg-red-50 text-red-700', 3: 'bg-gray-100 text-gray-600' }[n] || 'bg-gray-100 text-gray-600')
const getNatureLabel = (n: number) => natureOptions.find(o => o.value === n)?.label || String(n)
const getScopeLabel = (s: number) => scopeOptions.find(o => o.value === s)?.label || String(s)

const loadData = async () => {
  loading.value = true
  try {
    const res = await pageBehaviorTypes({ pageNum: pagination.pageNum, pageSize: pagination.pageSize, ...searchForm })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (e) { console.error(e); ElMessage.error('加载数据失败') }
  finally { loading.value = false }
}
const handleSearch = () => { pagination.pageNum = 1; loadData() }
const handleReset = () => { Object.assign(searchForm, { behaviorCode: '', behaviorName: '', behaviorCategory: '', behaviorNature: undefined, status: undefined }); handleSearch() }
const handlePageChange = (page: number) => { pagination.pageNum = page; loadData() }
const handleSizeChange = () => { pagination.pageNum = 1; loadData() }

const handleAdd = () => {
  Object.assign(formData, { id: undefined, behaviorCode: '', behaviorName: '', behaviorCategory: '', behaviorNature: 2, defaultAffectScope: 1, description: '', sortOrder: 0, status: 1 })
  dialogVisible.value = true
}
const handleEdit = (row: BehaviorType) => { Object.assign(formData, { ...row }); dialogVisible.value = true }
const handleDelete = async (row: BehaviorType) => {
  try {
    await ElMessageBox.confirm(`确定要删除"${row.behaviorName}"吗？`, '删除确认', { type: 'warning' })
    await deleteBehaviorType(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
}
const handleSubmit = async () => {
  if (!formData.behaviorCode || !formData.behaviorName || !formData.behaviorCategory) {
    ElMessage.warning('请填写必填项')
    return
  }
  if (!/^[A-Z][A-Z0-9_]*$/.test(formData.behaviorCode)) {
    ElMessage.warning('编码格式不正确')
    return
  }
  submitLoading.value = true
  try {
    if (formData.id) { await updateBehaviorType(formData.id, formData); ElMessage.success('更新成功') }
    else { await createBehaviorType(formData); ElMessage.success('创建成功') }
    dialogVisible.value = false
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '提交失败') }
  finally { submitLoading.value = false }
}

onMounted(() => loadData())
</script>
