<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 搜索栏 -->
    <div class="mb-4 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-center gap-3">
        <input v-model="queryParams.studentNo" type="text" placeholder="学号" class="h-9 w-32 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @keyup.enter="handleSearch" />
        <input v-model="queryParams.studentName" type="text" placeholder="姓名" class="h-9 w-28 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @keyup.enter="handleSearch" />
        <select v-model="queryParams.honorTypeId" class="h-9 w-32 rounded-lg border border-gray-200 px-2 text-sm focus:border-blue-500 focus:outline-none">
          <option :value="undefined">全部类型</option>
          <option v-for="item in honorTypeList" :key="item.id" :value="item.id">{{ item.typeName }}</option>
        </select>
        <select v-model="queryParams.status" class="h-9 w-28 rounded-lg border border-gray-200 px-2 text-sm focus:border-blue-500 focus:outline-none">
          <option :value="undefined">全部状态</option>
          <option :value="0">待审核</option>
          <option :value="1">班级通过</option>
          <option :value="2">班级驳回</option>
          <option :value="3">院系通过</option>
          <option :value="4">院系驳回</option>
          <option :value="5">学校通过</option>
          <option :value="6">学校驳回</option>
          <option :value="7">已撤回</option>
        </select>
        <button class="h-9 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700" @click="handleSearch">查询</button>
        <button class="h-9 rounded-lg border border-gray-200 bg-white px-4 text-sm text-gray-600 hover:bg-gray-50" @click="handleReset">重置</button>
        <div class="flex-1"></div>
        <button class="flex h-9 items-center gap-1 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700" @click="handleAdd">
          <Plus class="h-4 w-4" />新增申报
        </button>
        <button class="h-9 rounded-lg bg-green-600 px-4 text-sm text-white hover:bg-green-700 disabled:opacity-50" :disabled="selectedRows.length === 0" @click="handleBatchReview(true)">批量通过</button>
        <button class="h-9 rounded-lg bg-red-600 px-4 text-sm text-white hover:bg-red-700 disabled:opacity-50" :disabled="selectedRows.length === 0" @click="handleBatchReview(false)">批量驳回</button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <table class="w-full">
        <thead class="bg-gray-50">
          <tr class="border-b border-gray-200">
            <th class="w-12 px-4 py-3"><input type="checkbox" class="h-4 w-4 rounded border-gray-300" @change="handleSelectAll" /></th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">学号</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">姓名</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">班级</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">荣誉类型</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">荣誉名称</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">级别</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">加分</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">维度</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">状态</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">申报时间</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">操作</th>
          </tr>
        </thead>
        <tbody v-if="!loading">
          <tr v-for="row in tableData" :key="row.id" class="border-b border-gray-100 hover:bg-gray-50">
            <td class="px-4 py-3"><input type="checkbox" :checked="selectedRows.some(r => r.id === row.id)" class="h-4 w-4 rounded border-gray-300" @change="handleSelectRow(row, $event)" /></td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ row.studentNo }}</td>
            <td class="px-4 py-3 text-sm text-gray-900">{{ row.studentName }}</td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ row.className }}</td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ row.honorTypeName }}</td>
            <td class="max-w-[160px] truncate px-4 py-3 text-sm text-gray-900" :title="row.honorName">{{ row.honorName }}</td>
            <td class="px-4 py-3 text-center text-sm text-gray-600">{{ row.honorLevel }}</td>
            <td class="px-4 py-3 text-center text-sm font-medium text-green-600">+{{ row.bonusScore }}</td>
            <td class="px-4 py-3 text-center text-sm text-gray-600">{{ getDimensionText(row.affectDimension) }}</td>
            <td class="px-4 py-3 text-center">
              <span :class="['inline-block rounded px-2 py-0.5 text-xs', getStatusClass(row.status)]">{{ getStatusText(row.status) }}</span>
            </td>
            <td class="px-4 py-3 text-sm text-gray-500">{{ row.applyTime }}</td>
            <td class="px-4 py-3 text-center">
              <button class="mr-2 text-sm text-blue-600 hover:text-blue-800" @click="handleView(row)">查看</button>
              <button v-if="row.status === 0" class="mr-2 text-sm text-green-600 hover:text-green-800" @click="handleReview(row, 'class')">班级审核</button>
              <button v-if="row.status === 1" class="mr-2 text-sm text-green-600 hover:text-green-800" @click="handleReview(row, 'department')">院系审核</button>
              <button v-if="row.status === 3" class="mr-2 text-sm text-green-600 hover:text-green-800" @click="handleReview(row, 'school')">学校审核</button>
              <button v-if="row.status === 0" class="text-sm text-red-600 hover:text-red-800" @click="handleWithdraw(row)">撤回</button>
            </td>
          </tr>
          <tr v-if="tableData.length === 0">
            <td colspan="12" class="py-12 text-center text-sm text-gray-400">暂无数据</td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr><td colspan="12" class="py-12 text-center text-sm text-gray-400">加载中...</td></tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <div class="text-sm text-gray-500">共 {{ total }} 条，已选 {{ selectedRows.length }} 条</div>
        <div class="flex items-center gap-2">
          <button class="flex h-8 w-8 items-center justify-center rounded border border-gray-200 hover:bg-gray-50 disabled:opacity-50" :disabled="queryParams.pageNum <= 1" @click="handlePageChange(queryParams.pageNum - 1)">
            <ChevronLeft class="h-4 w-4" />
          </button>
          <span class="px-2 text-sm">{{ queryParams.pageNum }} / {{ Math.ceil(total / queryParams.pageSize) || 1 }}</span>
          <button class="flex h-8 w-8 items-center justify-center rounded border border-gray-200 hover:bg-gray-50 disabled:opacity-50" :disabled="queryParams.pageNum >= Math.ceil(total / queryParams.pageSize)" @click="handlePageChange(queryParams.pageNum + 1)">
            <ChevronRight class="h-4 w-4" />
          </button>
          <select v-model="queryParams.pageSize" class="pagination-select" @change="handleSearch">
            <option :value="10">10条/页</option>
            <option :value="20">20条/页</option>
            <option :value="50">50条/页</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 新增申报弹窗 -->
    <Teleport to="body">
      <div v-if="addDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="addDialogVisible = false"></div>
        <div class="relative w-full max-w-lg rounded-lg bg-white shadow-xl">
          <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
            <h3 class="text-lg font-medium text-gray-900">新增荣誉申报</h3>
            <button class="text-gray-400 hover:text-gray-600" @click="addDialogVisible = false"><X class="h-5 w-5" /></button>
          </div>
          <div class="max-h-[60vh] overflow-y-auto p-6">
            <div class="space-y-4">
              <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">荣誉类型 <span class="text-red-500">*</span></label>
                <select v-model="addFormData.honorTypeId" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @change="handleHonorTypeChange">
                  <option :value="undefined">请选择</option>
                  <option v-for="item in availableHonorTypes" :key="item.id" :value="item.id">{{ item.typeName }}</option>
                </select>
              </div>
              <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">荣誉名称 <span class="text-red-500">*</span></label>
                <input v-model="addFormData.honorName" type="text" placeholder="请输入具体荣誉名称" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-1 block text-sm font-medium text-gray-700">荣誉级别 <span class="text-red-500">*</span></label>
                  <select v-model="addFormData.honorLevel" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
                    <option value="">请选择</option>
                    <option value="national">国家级</option>
                    <option value="provincial">省级</option>
                    <option value="city">市级</option>
                    <option value="school">校级</option>
                    <option value="department">院级</option>
                  </select>
                </div>
                <div>
                  <label class="mb-1 block text-sm font-medium text-gray-700">获得时间 <span class="text-red-500">*</span></label>
                  <input type="date" v-model="addFormData.achieveTime" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
                </div>
              </div>
              <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">加分</label>
                <input type="number" v-model="addFormData.bonusScore" min="0" max="100" step="0.1" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
              <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">证明材料</label>
                <div class="rounded-lg border border-dashed border-gray-300 bg-gray-50 p-4 text-center">
                  <p class="text-sm text-gray-500">点击或拖拽文件上传</p>
                  <p class="text-xs text-gray-400">支持 jpg/png/pdf，不超过10MB</p>
                </div>
              </div>
              <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">说明</label>
                <textarea v-model="addFormData.description" rows="3" placeholder="请输入说明（可选）" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"></textarea>
              </div>
            </div>
          </div>
          <div class="flex justify-end gap-3 border-t border-gray-200 px-6 py-4">
            <button class="h-9 rounded-lg border border-gray-200 px-4 text-sm text-gray-600 hover:bg-gray-50" @click="addDialogVisible = false">取消</button>
            <button class="h-9 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700 disabled:opacity-50" :disabled="submitLoading" @click="handleAddSubmit">{{ submitLoading ? '提交中...' : '提交申报' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 审核弹窗 -->
    <Teleport to="body">
      <div v-if="reviewDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="reviewDialogVisible = false"></div>
        <div class="relative w-full max-w-md rounded-lg bg-white shadow-xl">
          <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
            <h3 class="text-lg font-medium text-gray-900">{{ reviewLevelText[currentReviewLevel] }}审核</h3>
            <button class="text-gray-400 hover:text-gray-600" @click="reviewDialogVisible = false"><X class="h-5 w-5" /></button>
          </div>
          <div class="p-6">
            <label class="mb-2 block text-sm font-medium text-gray-700">审核意见</label>
            <textarea v-model="reviewFormData.comment" rows="4" placeholder="请输入审核意见" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"></textarea>
          </div>
          <div class="flex justify-end gap-3 border-t border-gray-200 px-6 py-4">
            <button class="h-9 rounded-lg border border-gray-200 px-4 text-sm text-gray-600 hover:bg-gray-50" @click="reviewDialogVisible = false">取消</button>
            <button class="h-9 rounded-lg bg-red-600 px-4 text-sm text-white hover:bg-red-700" @click="handleReviewSubmit(false)">驳回</button>
            <button class="h-9 rounded-lg bg-green-600 px-4 text-sm text-white hover:bg-green-700" @click="handleReviewSubmit(true)">通过</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 详情弹窗 -->
    <Teleport to="body">
      <div v-if="detailDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="detailDialogVisible = false"></div>
        <div class="relative w-full max-w-2xl rounded-lg bg-white shadow-xl">
          <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
            <h3 class="text-lg font-medium text-gray-900">申报详情</h3>
            <button class="text-gray-400 hover:text-gray-600" @click="detailDialogVisible = false"><X class="h-5 w-5" /></button>
          </div>
          <div class="max-h-[70vh] overflow-y-auto p-6">
            <!-- 基本信息 -->
            <div class="mb-6 grid grid-cols-2 gap-4 rounded-lg border border-gray-200 bg-gray-50 p-4">
              <div><span class="text-sm text-gray-500">学号：</span><span class="text-sm font-medium">{{ detailData.studentNo }}</span></div>
              <div><span class="text-sm text-gray-500">姓名：</span><span class="text-sm font-medium">{{ detailData.studentName }}</span></div>
              <div><span class="text-sm text-gray-500">班级：</span><span class="text-sm font-medium">{{ detailData.className }}</span></div>
              <div><span class="text-sm text-gray-500">院系：</span><span class="text-sm font-medium">{{ detailData.departmentName }}</span></div>
              <div><span class="text-sm text-gray-500">荣誉类型：</span><span class="text-sm font-medium">{{ detailData.honorTypeName }}</span></div>
              <div><span class="text-sm text-gray-500">荣誉名称：</span><span class="text-sm font-medium">{{ detailData.honorName }}</span></div>
              <div><span class="text-sm text-gray-500">级别：</span><span class="text-sm font-medium">{{ detailData.honorLevel }}</span></div>
              <div><span class="text-sm text-gray-500">获得时间：</span><span class="text-sm font-medium">{{ detailData.achieveTime }}</span></div>
              <div><span class="text-sm text-gray-500">加分：</span><span class="text-sm font-medium text-green-600">+{{ detailData.bonusScore }}</span></div>
              <div><span class="text-sm text-gray-500">影响维度：</span><span class="text-sm font-medium">{{ getDimensionText(detailData.affectDimension) }}</span></div>
              <div class="col-span-2"><span class="text-sm text-gray-500">申报时间：</span><span class="text-sm font-medium">{{ detailData.applyTime }}</span></div>
              <div class="col-span-2"><span class="text-sm text-gray-500">说明：</span><span class="text-sm font-medium">{{ detailData.description || '-' }}</span></div>
            </div>
            <!-- 审核记录 -->
            <div class="border-t border-gray-200 pt-4">
              <h4 class="mb-4 text-sm font-medium text-gray-900">审核记录</h4>
              <div class="space-y-4">
                <div v-if="detailData.classReviewTime" class="flex gap-4 rounded-lg border border-gray-200 p-4">
                  <div class="flex h-10 w-10 items-center justify-center rounded-full bg-blue-50 text-blue-600"><Check class="h-5 w-5" /></div>
                  <div class="flex-1">
                    <div class="flex items-center gap-2">
                      <span class="font-medium text-gray-900">班级审核</span>
                      <span :class="['rounded px-2 py-0.5 text-xs', detailData.status >= 2 && detailData.status !== 3 && detailData.status !== 5 ? 'bg-red-50 text-red-700' : 'bg-green-50 text-green-700']">{{ detailData.status >= 2 && detailData.status !== 3 && detailData.status !== 5 ? '已驳回' : '已通过' }}</span>
                    </div>
                    <p class="mt-1 text-sm text-gray-500">审核人：{{ detailData.classReviewerName }}</p>
                    <p class="text-sm text-gray-500">意见：{{ detailData.classReviewComment || '无' }}</p>
                    <p class="text-xs text-gray-400">{{ detailData.classReviewTime }}</p>
                  </div>
                </div>
                <div v-if="detailData.deptReviewTime" class="flex gap-4 rounded-lg border border-gray-200 p-4">
                  <div class="flex h-10 w-10 items-center justify-center rounded-full bg-purple-50 text-purple-600"><Check class="h-5 w-5" /></div>
                  <div class="flex-1">
                    <div class="flex items-center gap-2">
                      <span class="font-medium text-gray-900">院系审核</span>
                      <span :class="['rounded px-2 py-0.5 text-xs', detailData.status >= 4 && detailData.status !== 5 ? 'bg-red-50 text-red-700' : 'bg-green-50 text-green-700']">{{ detailData.status >= 4 && detailData.status !== 5 ? '已驳回' : '已通过' }}</span>
                    </div>
                    <p class="mt-1 text-sm text-gray-500">审核人：{{ detailData.deptReviewerName }}</p>
                    <p class="text-sm text-gray-500">意见：{{ detailData.deptReviewComment || '无' }}</p>
                    <p class="text-xs text-gray-400">{{ detailData.deptReviewTime }}</p>
                  </div>
                </div>
                <div v-if="detailData.schoolReviewTime" class="flex gap-4 rounded-lg border border-gray-200 p-4">
                  <div class="flex h-10 w-10 items-center justify-center rounded-full bg-amber-50 text-amber-600"><Check class="h-5 w-5" /></div>
                  <div class="flex-1">
                    <div class="flex items-center gap-2">
                      <span class="font-medium text-gray-900">学校审核</span>
                      <span :class="['rounded px-2 py-0.5 text-xs', detailData.status >= 6 ? 'bg-red-50 text-red-700' : 'bg-green-50 text-green-700']">{{ detailData.status >= 6 ? '已驳回' : '已通过' }}</span>
                    </div>
                    <p class="mt-1 text-sm text-gray-500">审核人：{{ detailData.schoolReviewerName }}</p>
                    <p class="text-sm text-gray-500">意见：{{ detailData.schoolReviewComment || '无' }}</p>
                    <p class="text-xs text-gray-400">{{ detailData.schoolReviewTime }}</p>
                  </div>
                </div>
                <div v-if="!detailData.classReviewTime && !detailData.deptReviewTime && !detailData.schoolReviewTime" class="py-8 text-center text-sm text-gray-400">暂无审核记录</div>
              </div>
            </div>
          </div>
          <div class="flex justify-end border-t border-gray-200 px-6 py-4">
            <button class="h-9 rounded-lg border border-gray-200 px-4 text-sm text-gray-600 hover:bg-gray-50" @click="detailDialogVisible = false">关闭</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, X, ChevronLeft, ChevronRight, Check } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageHonorApplications, getHonorApplicationDetail, submitHonorApplication, withdrawHonorApplication, classReviewHonorApplication, departmentReviewHonorApplication, schoolReviewHonorApplication, batchReviewHonorApplications, type HonorApplication, type HonorType } from '@/api/evaluation'

const queryParams = reactive({ pageNum: 1, pageSize: 10, studentNo: '', studentName: '', honorTypeId: undefined as number | undefined, status: undefined as number | undefined })
const loading = ref(false)
const tableData = ref<HonorApplication[]>([])
const total = ref(0)
const selectedRows = ref<HonorApplication[]>([])
const honorTypeList = ref<HonorType[]>([])
const availableHonorTypes = ref<any[]>([])

const addDialogVisible = ref(false)
const submitLoading = ref(false)
const addFormData = reactive({ honorTypeId: undefined as number | undefined, honorName: '', honorLevel: '', achieveTime: '', bonusScore: 0, description: '', materialUrls: '' })

const reviewDialogVisible = ref(false)
const currentReviewRow = ref<HonorApplication | null>(null)
const currentReviewLevel = ref<'class' | 'department' | 'school'>('class')
const reviewFormData = reactive({ comment: '' })
const reviewLevelText = { class: '班级', department: '院系', school: '学校' }

const detailDialogVisible = ref(false)
const detailData = ref<any>({})

const getStatusClass = (s: number) => {
  const map: Record<number, string> = { 0: 'bg-amber-50 text-amber-700', 1: 'bg-blue-50 text-blue-700', 2: 'bg-red-50 text-red-700', 3: 'bg-blue-50 text-blue-700', 4: 'bg-red-50 text-red-700', 5: 'bg-green-50 text-green-700', 6: 'bg-red-50 text-red-700', 7: 'bg-gray-100 text-gray-600' }
  return map[s] || 'bg-gray-100 text-gray-600'
}
const getStatusText = (s: number) => ({ 0: '待审核', 1: '班级通过', 2: '班级驳回', 3: '院系通过', 4: '院系驳回', 5: '学校通过', 6: '学校驳回', 7: '已撤回' }[s] || '未知')
const getDimensionText = (d: string) => ({ moral: '德育', intellectual: '智育', physical: '体育', aesthetic: '美育', labor: '劳育', development: '发展素质' }[d] || d)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await pageHonorApplications(queryParams)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}
const handleSearch = () => { queryParams.pageNum = 1; fetchData() }
const handleReset = () => { Object.assign(queryParams, { studentNo: '', studentName: '', honorTypeId: undefined, status: undefined }); handleSearch() }
const handlePageChange = (page: number) => { queryParams.pageNum = page; fetchData() }

const handleSelectAll = (e: Event) => {
  const checked = (e.target as HTMLInputElement).checked
  selectedRows.value = checked ? [...tableData.value] : []
}
const handleSelectRow = (row: HonorApplication, e: Event) => {
  const checked = (e.target as HTMLInputElement).checked
  if (checked) selectedRows.value.push(row)
  else selectedRows.value = selectedRows.value.filter(r => r.id !== row.id)
}

const handleAdd = () => {
  Object.assign(addFormData, { honorTypeId: undefined, honorName: '', honorLevel: '', achieveTime: '', bonusScore: 0, description: '', materialUrls: '' })
  addDialogVisible.value = true
}
const handleHonorTypeChange = () => {
  const type = availableHonorTypes.value.find(t => t.id === addFormData.honorTypeId)
  if (type) addFormData.bonusScore = type.bonusScore || 0
}
const handleAddSubmit = async () => {
  if (!addFormData.honorTypeId || !addFormData.honorName || !addFormData.honorLevel || !addFormData.achieveTime) {
    ElMessage.warning('请填写必填项')
    return
  }
  submitLoading.value = true
  try {
    await submitHonorApplication({ periodId: 1, studentId: 1, ...addFormData } as HonorApplication)
    ElMessage.success('申报成功')
    addDialogVisible.value = false
    fetchData()
  } catch (e: any) { ElMessage.error(e.message || '提交失败') }
  finally { submitLoading.value = false }
}

const handleView = async (row: HonorApplication) => {
  try {
    const res = await getHonorApplicationDetail(row.id!)
    detailData.value = res.data || {}
    detailDialogVisible.value = true
  } catch (e) { ElMessage.error('加载详情失败') }
}
const handleWithdraw = async (row: HonorApplication) => {
  try {
    await ElMessageBox.confirm('确定要撤回该申报吗？', '提示', { type: 'warning' })
    await withdrawHonorApplication(row.id!)
    ElMessage.success('撤回成功')
    fetchData()
  } catch { /* cancelled */ }
}
const handleReview = (row: HonorApplication, level: 'class' | 'department' | 'school') => {
  currentReviewRow.value = row
  currentReviewLevel.value = level
  reviewFormData.comment = ''
  reviewDialogVisible.value = true
}
const handleReviewSubmit = async (approved: boolean) => {
  if (!currentReviewRow.value) return
  const actions = { class: classReviewHonorApplication, department: departmentReviewHonorApplication, school: schoolReviewHonorApplication }
  try {
    await actions[currentReviewLevel.value](currentReviewRow.value.id!, approved, reviewFormData.comment)
    ElMessage.success('审核完成')
    reviewDialogVisible.value = false
    fetchData()
  } catch (e: any) { ElMessage.error(e.message || '审核失败') }
}
const handleBatchReview = async (approved: boolean) => {
  if (selectedRows.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定要批量${approved ? '通过' : '驳回'}选中的${selectedRows.value.length}条申报吗？`, '提示', { type: 'warning' })
    const ids = selectedRows.value.map(row => row.id!)
    await batchReviewHonorApplications(ids, 'class', approved)
    ElMessage.success('批量审核完成')
    selectedRows.value = []
    fetchData()
  } catch { /* cancelled */ }
}

onMounted(() => fetchData())
</script>
