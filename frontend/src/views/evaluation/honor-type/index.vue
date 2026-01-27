<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 搜索栏 -->
    <div class="mb-4 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-center gap-3">
        <input v-model="searchForm.typeCode" type="text" placeholder="类型编码" class="h-9 w-36 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @keyup.enter="handleSearch" />
        <input v-model="searchForm.typeName" type="text" placeholder="类型名称" class="h-9 w-36 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @keyup.enter="handleSearch" />
        <select v-model="searchForm.category" class="h-9 w-28 rounded-lg border border-gray-200 px-2 text-sm focus:border-blue-500 focus:outline-none">
          <option value="">全部类别</option>
          <option v-for="item in categoryOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
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
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">类型编码</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">类型名称</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">类别</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">影响维度</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">需附件</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">排序</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">状态</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">描述</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">操作</th>
          </tr>
        </thead>
        <tbody v-if="!loading">
          <tr v-for="row in tableData" :key="row.id" class="border-b border-gray-100 hover:bg-gray-50">
            <td class="px-4 py-3 text-sm font-mono text-gray-600">{{ row.typeCode }}</td>
            <td class="px-4 py-3 text-sm text-gray-900">{{ row.typeName }}</td>
            <td class="px-4 py-3 text-center">
              <span :class="['inline-block rounded px-2 py-0.5 text-xs', getCategoryClass(row.category)]">{{ getCategoryLabel(row.category) }}</span>
            </td>
            <td class="px-4 py-3 text-center">
              <span class="inline-block rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-600">{{ getDimensionLabel(row.evaluationDimension) }}</span>
            </td>
            <td class="px-4 py-3 text-center">
              <span :class="['inline-block rounded px-2 py-0.5 text-xs', row.requiredAttachments === 1 ? 'bg-amber-50 text-amber-700' : 'bg-gray-100 text-gray-600']">{{ row.requiredAttachments === 1 ? '需要' : '不需' }}</span>
            </td>
            <td class="px-4 py-3 text-center text-sm text-gray-600">{{ row.sortOrder }}</td>
            <td class="px-4 py-3 text-center">
              <span :class="['inline-block rounded px-2 py-0.5 text-xs', row.status === 1 ? 'bg-green-50 text-green-700' : 'bg-gray-100 text-gray-600']">{{ row.status === 1 ? '启用' : '禁用' }}</span>
            </td>
            <td class="max-w-[180px] truncate px-4 py-3 text-sm text-gray-500" :title="row.description">{{ row.description || '-' }}</td>
            <td class="px-4 py-3 text-center">
              <button class="mr-2 text-sm text-blue-600 hover:text-blue-800" @click="handleEdit(row)">编辑</button>
              <button class="mr-2 text-sm text-green-600 hover:text-green-800" @click="handleViewLevels(row)">等级</button>
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
            <h3 class="text-lg font-medium text-gray-900">{{ formData.id ? '编辑荣誉类型' : '新增荣誉类型' }}</h3>
            <button class="text-gray-400 hover:text-gray-600" @click="dialogVisible = false"><X class="h-5 w-5" /></button>
          </div>
          <div class="max-h-[60vh] overflow-y-auto p-6">
            <div class="space-y-4">
              <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">类型编码 <span class="text-red-500">*</span></label>
                <input v-model="formData.typeCode" type="text" placeholder="如：COMPETITION_TECH" :disabled="!!formData.id" :class="['h-9 w-full rounded-lg border px-3 text-sm focus:outline-none', formData.id ? 'bg-gray-100 border-gray-200 cursor-not-allowed' : 'border-gray-200 focus:border-blue-500']" />
                <p class="mt-1 text-xs text-gray-400">只能包含大写字母、数字和下划线，以大写字母开头</p>
              </div>
              <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">类型名称 <span class="text-red-500">*</span></label>
                <input v-model="formData.typeName" type="text" placeholder="请输入类型名称" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-1 block text-sm font-medium text-gray-700">类别 <span class="text-red-500">*</span></label>
                  <select v-model="formData.category" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
                    <option value="">请选择</option>
                    <option v-for="item in categoryOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
                  </select>
                </div>
                <div>
                  <label class="mb-1 block text-sm font-medium text-gray-700">影响维度 <span class="text-red-500">*</span></label>
                  <select v-model="formData.evaluationDimension" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
                    <option value="">请选择</option>
                    <option v-for="item in dimensionOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
                  </select>
                </div>
              </div>
              <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">是否需附件</label>
                <div class="flex gap-4">
                  <label class="flex cursor-pointer items-center gap-2">
                    <input type="radio" :value="1" v-model="formData.requiredAttachments" class="h-4 w-4 text-blue-600" />
                    <span class="text-sm text-gray-700">需要</span>
                  </label>
                  <label class="flex cursor-pointer items-center gap-2">
                    <input type="radio" :value="0" v-model="formData.requiredAttachments" class="h-4 w-4 text-blue-600" />
                    <span class="text-sm text-gray-700">不需要</span>
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

    <!-- 等级配置弹窗 -->
    <Teleport to="body">
      <div v-if="levelDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="levelDialogVisible = false"></div>
        <div class="relative w-full max-w-3xl rounded-lg bg-white shadow-xl">
          <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
            <h3 class="text-lg font-medium text-gray-900">荣誉等级配置</h3>
            <button class="text-gray-400 hover:text-gray-600" @click="levelDialogVisible = false"><X class="h-5 w-5" /></button>
          </div>
          <div class="p-6">
            <!-- 类型信息 -->
            <div v-if="currentHonorType" class="mb-4 grid grid-cols-3 gap-4 rounded-lg border border-gray-200 bg-gray-50 p-4">
              <div><span class="text-sm text-gray-500">类型编码：</span><span class="text-sm font-medium text-gray-900">{{ currentHonorType.typeCode }}</span></div>
              <div><span class="text-sm text-gray-500">类型名称：</span><span class="text-sm font-medium text-gray-900">{{ currentHonorType.typeName }}</span></div>
              <div><span class="text-sm text-gray-500">类别：</span><span :class="['rounded px-2 py-0.5 text-xs', getCategoryClass(currentHonorType.category)]">{{ getCategoryLabel(currentHonorType.category) }}</span></div>
            </div>
            <!-- 等级列表 -->
            <table class="w-full">
              <thead class="bg-gray-50">
                <tr class="border-b border-gray-200">
                  <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">级别</th>
                  <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">名次</th>
                  <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">加分分值</th>
                  <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">最大计次</th>
                  <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">优先级</th>
                  <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in levelConfigs" :key="row.levelCode" class="border-b border-gray-100">
                  <td class="px-4 py-3 text-center text-sm text-gray-900">{{ row.levelName }}</td>
                  <td class="px-4 py-3 text-center text-sm text-gray-600">{{ row.rankName }}</td>
                  <td class="px-4 py-3 text-center text-sm font-medium text-green-600">+{{ row.score }}</td>
                  <td class="px-4 py-3 text-center text-sm text-gray-600">{{ row.maxCount || '不限' }}</td>
                  <td class="px-4 py-3 text-center text-sm text-gray-600">{{ row.priority }}</td>
                  <td class="px-4 py-3 text-center">
                    <span :class="['inline-block rounded px-2 py-0.5 text-xs', row.status === 1 ? 'bg-green-50 text-green-700' : 'bg-gray-100 text-gray-600']">{{ row.status === 1 ? '启用' : '禁用' }}</span>
                  </td>
                </tr>
                <tr v-if="levelConfigs.length === 0">
                  <td colspan="6" class="py-8 text-center text-sm text-gray-400">暂无等级配置</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="flex justify-end border-t border-gray-200 px-6 py-4">
            <button class="h-9 rounded-lg border border-gray-200 px-4 text-sm text-gray-600 hover:bg-gray-50" @click="levelDialogVisible = false">关闭</button>
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
import { pageHonorTypes, getHonorTypeDetail, createHonorType, updateHonorType, deleteHonorType, type HonorType, type HonorLevelConfig } from '@/api/evaluation'

const categoryOptions = [
  { value: 'COMPETITION', label: '竞赛' },
  { value: 'CERTIFICATE', label: '证书' },
  { value: 'TITLE', label: '荣誉称号' },
  { value: 'ACTIVITY', label: '活动' },
  { value: 'PUBLICATION', label: '学术成果' },
  { value: 'OTHER', label: '其他' }
]
const dimensionOptions = [
  { value: 'MORAL', label: '德育' },
  { value: 'INTELLECTUAL', label: '智育' },
  { value: 'PHYSICAL', label: '体育' },
  { value: 'AESTHETIC', label: '美育' },
  { value: 'LABOR', label: '劳育' },
  { value: 'DEVELOPMENT', label: '发展素质' }
]

const searchForm = reactive({ typeCode: '', typeName: '', category: '', status: undefined as number | undefined })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const tableData = ref<HonorType[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formData = reactive<HonorType>({ typeCode: '', typeName: '', category: '', evaluationDimension: '', description: '', requiredAttachments: 0, sortOrder: 0, status: 1 })
const levelDialogVisible = ref(false)
const currentHonorType = ref<HonorType | null>(null)
const levelConfigs = ref<HonorLevelConfig[]>([])

const getCategoryClass = (cat: string) => {
  const map: Record<string, string> = { COMPETITION: 'bg-blue-50 text-blue-700', CERTIFICATE: 'bg-green-50 text-green-700', TITLE: 'bg-amber-50 text-amber-700', ACTIVITY: 'bg-purple-50 text-purple-700', PUBLICATION: 'bg-red-50 text-red-700', OTHER: 'bg-gray-100 text-gray-600' }
  return map[cat] || 'bg-gray-100 text-gray-600'
}
const getCategoryLabel = (cat: string) => categoryOptions.find(o => o.value === cat)?.label || cat
const getDimensionLabel = (dim: string) => dimensionOptions.find(o => o.value === dim)?.label || dim

const loadData = async () => {
  loading.value = true
  try {
    const res = await pageHonorTypes({ pageNum: pagination.pageNum, pageSize: pagination.pageSize, ...searchForm })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (e) { console.error(e); ElMessage.error('加载数据失败') }
  finally { loading.value = false }
}
const handleSearch = () => { pagination.pageNum = 1; loadData() }
const handleReset = () => { Object.assign(searchForm, { typeCode: '', typeName: '', category: '', status: undefined }); handleSearch() }
const handlePageChange = (page: number) => { pagination.pageNum = page; loadData() }
const handleSizeChange = () => { pagination.pageNum = 1; loadData() }

const handleAdd = () => {
  Object.assign(formData, { id: undefined, typeCode: '', typeName: '', category: '', evaluationDimension: '', description: '', requiredAttachments: 0, sortOrder: 0, status: 1 })
  dialogVisible.value = true
}
const handleEdit = (row: HonorType) => { Object.assign(formData, { ...row }); dialogVisible.value = true }
const handleViewLevels = async (row: HonorType) => {
  try {
    const res = await getHonorTypeDetail(row.id!)
    currentHonorType.value = row
    if (res.data?.levelConfigs) {
      levelConfigs.value = typeof res.data.levelConfigs === 'string' ? JSON.parse(res.data.levelConfigs) : res.data.levelConfigs
    } else { levelConfigs.value = [] }
    levelDialogVisible.value = true
  } catch (e) { console.error(e); ElMessage.error('加载等级配置失败') }
}
const handleDelete = async (row: HonorType) => {
  try {
    await ElMessageBox.confirm(`确定要删除"${row.typeName}"吗？`, '删除确认', { type: 'warning' })
    await deleteHonorType(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
}
const handleSubmit = async () => {
  if (!formData.typeCode || !formData.typeName || !formData.category || !formData.evaluationDimension) {
    ElMessage.warning('请填写必填项')
    return
  }
  if (!/^[A-Z][A-Z0-9_]*$/.test(formData.typeCode)) {
    ElMessage.warning('编码格式不正确')
    return
  }
  submitLoading.value = true
  try {
    if (formData.id) { await updateHonorType(formData.id, formData); ElMessage.success('更新成功') }
    else { await createHonorType(formData); ElMessage.success('创建成功') }
    dialogVisible.value = false
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '提交失败') }
  finally { submitLoading.value = false }
}

onMounted(() => loadData())
</script>
