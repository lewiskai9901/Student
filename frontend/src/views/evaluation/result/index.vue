<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 搜索栏 -->
    <div class="mb-4 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-center gap-3">
        <select v-model="queryParams.periodId" class="h-9 w-40 rounded-lg border border-gray-200 px-2 text-sm focus:border-blue-500 focus:outline-none">
          <option :value="undefined">全部周期</option>
          <option v-for="item in periodList" :key="item.id" :value="item.id">{{ item.periodName }}</option>
        </select>
        <input v-model="queryParams.studentNo" type="text" placeholder="学号" class="h-9 w-32 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @keyup.enter="handleSearch" />
        <input v-model="queryParams.studentName" type="text" placeholder="姓名" class="h-9 w-28 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @keyup.enter="handleSearch" />
        <select v-model="queryParams.classId" class="h-9 w-40 rounded-lg border border-gray-200 px-2 text-sm focus:border-blue-500 focus:outline-none">
          <option :value="undefined">全部班级</option>
          <option v-for="item in classList" :key="item.id" :value="item.id">{{ item.className }}</option>
        </select>
        <button class="h-9 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700" @click="handleSearch">查询</button>
        <button class="h-9 rounded-lg border border-gray-200 bg-white px-4 text-sm text-gray-600 hover:bg-gray-50" @click="handleReset">重置</button>
        <div class="flex-1"></div>
        <button class="flex h-9 items-center gap-1 rounded-lg bg-blue-600 px-3 text-sm text-white hover:bg-blue-700" @click="handleCalculateClass">
          <RefreshCw class="h-4 w-4" />计算综测
        </button>
        <button class="flex h-9 items-center gap-1 rounded-lg bg-green-600 px-3 text-sm text-white hover:bg-green-700" @click="handleCalculateRanking">
          <ArrowUpDown class="h-4 w-4" />计算排名
        </button>
        <button class="flex h-9 items-center gap-1 rounded-lg bg-amber-500 px-3 text-sm text-white hover:bg-amber-600" @click="handleExport">
          <Download class="h-4 w-4" />导出
        </button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="mb-4 grid grid-cols-6 gap-4">
      <div class="rounded-lg border border-gray-200 bg-white p-4 text-center">
        <p class="text-2xl font-bold text-gray-900">{{ statistics.totalCount || 0 }}</p>
        <p class="mt-1 text-sm text-gray-500">参评人数</p>
      </div>
      <div class="rounded-lg border border-gray-200 bg-white p-4 text-center">
        <p class="text-2xl font-bold text-green-600">{{ statistics.avgMoral || '-' }}</p>
        <p class="mt-1 text-sm text-gray-500">德育平均</p>
      </div>
      <div class="rounded-lg border border-gray-200 bg-white p-4 text-center">
        <p class="text-2xl font-bold text-blue-600">{{ statistics.avgIntellectual || '-' }}</p>
        <p class="mt-1 text-sm text-gray-500">智育平均</p>
      </div>
      <div class="rounded-lg border border-gray-200 bg-white p-4 text-center">
        <p class="text-2xl font-bold text-amber-600">{{ statistics.avgPhysical || '-' }}</p>
        <p class="mt-1 text-sm text-gray-500">体育平均</p>
      </div>
      <div class="rounded-lg border border-gray-200 bg-white p-4 text-center">
        <p class="text-2xl font-bold text-pink-600">{{ statistics.avgAesthetic || '-' }}</p>
        <p class="mt-1 text-sm text-gray-500">美育平均</p>
      </div>
      <div class="rounded-lg border border-gray-200 bg-white p-4 text-center">
        <p class="text-2xl font-bold text-gray-900">{{ statistics.avgTotal || '-' }}</p>
        <p class="mt-1 text-sm text-gray-500">总分平均</p>
      </div>
    </div>

    <!-- 表格 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-gray-50">
            <tr class="border-b border-gray-200">
              <th class="px-3 py-3 text-center text-sm font-medium text-gray-900">班级排名</th>
              <th class="px-3 py-3 text-left text-sm font-medium text-gray-900">学号</th>
              <th class="px-3 py-3 text-left text-sm font-medium text-gray-900">姓名</th>
              <th class="px-3 py-3 text-left text-sm font-medium text-gray-900">班级</th>
              <th class="px-3 py-3 text-center text-sm font-medium text-gray-900">德育(25%)</th>
              <th class="px-3 py-3 text-center text-sm font-medium text-gray-900">智育(40%)</th>
              <th class="px-3 py-3 text-center text-sm font-medium text-gray-900">体育(10%)</th>
              <th class="px-3 py-3 text-center text-sm font-medium text-gray-900">美育(10%)</th>
              <th class="px-3 py-3 text-center text-sm font-medium text-gray-900">劳育(10%)</th>
              <th class="px-3 py-3 text-center text-sm font-medium text-gray-900">发展(5%)</th>
              <th class="px-3 py-3 text-center text-sm font-medium text-gray-900">总分</th>
              <th class="px-3 py-3 text-center text-sm font-medium text-gray-900">年级排名</th>
              <th class="px-3 py-3 text-center text-sm font-medium text-gray-900">操作</th>
            </tr>
          </thead>
          <tbody v-if="!loading">
            <tr v-for="row in tableData" :key="row.id" class="border-b border-gray-100 hover:bg-gray-50">
              <td class="px-3 py-3 text-center">
                <span v-if="row.classRank && row.classRank <= 3" class="inline-flex h-6 w-6 items-center justify-center rounded-full bg-red-500 text-xs font-bold text-white">{{ row.classRank }}</span>
                <span v-else class="text-sm text-gray-600">{{ row.classRank || '-' }}</span>
              </td>
              <td class="px-3 py-3 text-sm text-gray-600">{{ row.studentNo }}</td>
              <td class="px-3 py-3 text-sm font-medium text-gray-900">{{ row.studentName }}</td>
              <td class="px-3 py-3 text-sm text-gray-600">{{ row.className }}</td>
              <td class="px-3 py-3 text-center"><span :class="getScoreClass(row.moralScore)">{{ formatScore(row.moralScore) }}</span></td>
              <td class="px-3 py-3 text-center"><span :class="getScoreClass(row.intellectualScore)">{{ formatScore(row.intellectualScore) }}</span></td>
              <td class="px-3 py-3 text-center"><span :class="getScoreClass(row.physicalScore)">{{ formatScore(row.physicalScore) }}</span></td>
              <td class="px-3 py-3 text-center"><span :class="getScoreClass(row.aestheticScore)">{{ formatScore(row.aestheticScore) }}</span></td>
              <td class="px-3 py-3 text-center"><span :class="getScoreClass(row.laborScore)">{{ formatScore(row.laborScore) }}</span></td>
              <td class="px-3 py-3 text-center"><span :class="getScoreClass(row.developmentScore)">{{ formatScore(row.developmentScore) }}</span></td>
              <td class="px-3 py-3 text-center text-sm font-bold text-gray-900">{{ formatScore(row.totalScore) }}</td>
              <td class="px-3 py-3 text-center text-sm text-gray-600">{{ row.gradeRank || '-' }}</td>
              <td class="px-3 py-3 text-center">
                <button class="mr-2 text-sm text-blue-600 hover:text-blue-800" @click="handleViewDetail(row)">详情</button>
                <button class="text-sm text-amber-600 hover:text-amber-800" @click="handleRecalculate(row)">重算</button>
              </td>
            </tr>
            <tr v-if="tableData.length === 0">
              <td colspan="13" class="py-12 text-center text-sm text-gray-400">暂无数据</td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr><td colspan="13" class="py-12 text-center text-sm text-gray-400">加载中...</td></tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <div class="text-sm text-gray-500">共 {{ total }} 条</div>
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

    <!-- 详情弹窗 -->
    <Teleport to="body">
      <div v-if="detailDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="detailDialogVisible = false"></div>
        <div class="relative w-full max-w-4xl rounded-lg bg-white shadow-xl">
          <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
            <h3 class="text-lg font-medium text-gray-900">综测详情</h3>
            <button class="text-gray-400 hover:text-gray-600" @click="detailDialogVisible = false"><X class="h-5 w-5" /></button>
          </div>
          <div class="max-h-[70vh] overflow-y-auto p-6">
            <!-- 基本信息 -->
            <div class="mb-6 grid grid-cols-3 gap-4 rounded-lg border border-gray-200 bg-gray-50 p-4">
              <div><span class="text-sm text-gray-500">学号：</span><span class="text-sm font-medium">{{ detailData.studentNo }}</span></div>
              <div><span class="text-sm text-gray-500">姓名：</span><span class="text-sm font-medium">{{ detailData.studentName }}</span></div>
              <div><span class="text-sm text-gray-500">班级：</span><span class="text-sm font-medium">{{ detailData.className }}</span></div>
              <div><span class="text-sm text-gray-500">综测周期：</span><span class="text-sm font-medium">{{ detailData.periodName }}</span></div>
              <div><span class="text-sm text-gray-500">班级排名：</span><span class="text-sm font-bold text-blue-600">{{ detailData.classRank }}</span></div>
              <div><span class="text-sm text-gray-500">年级排名：</span><span class="text-sm font-bold text-green-600">{{ detailData.gradeRank }}</span></div>
            </div>

            <!-- 成绩六边形可视化 -->
            <div class="mb-6 grid grid-cols-6 gap-3">
              <div v-for="(item, key) in dimensions" :key="key" class="rounded-lg border border-gray-200 p-3 text-center">
                <p class="text-xl font-bold" :class="item.color">{{ formatScore(detailData[item.scoreKey]) }}</p>
                <p class="mt-1 text-xs text-gray-500">{{ item.label }}({{ item.weight }})</p>
              </div>
            </div>

            <!-- Tab 导航 -->
            <div class="mb-4 flex gap-1 rounded-lg bg-gray-100 p-1">
              <button v-for="(tab, key) in detailTabs" :key="key" @click="activeDetailTab = key" :class="['flex-1 rounded-md px-4 py-2 text-sm', activeDetailTab === key ? 'bg-white font-medium text-gray-900 shadow-sm' : 'text-gray-600 hover:text-gray-900']">{{ tab.label }}</button>
            </div>

            <!-- Tab 内容 -->
            <div class="rounded-lg border border-gray-200">
              <div class="flex items-center justify-between border-b border-gray-200 bg-gray-50 px-4 py-3">
                <span class="text-sm font-medium text-gray-900">{{ detailTabs[activeDetailTab].label }}明细</span>
                <div class="flex items-center gap-4 text-sm">
                  <span class="text-gray-500">基础分：<span class="font-medium text-gray-900">{{ detailTabs[activeDetailTab].baseScore }}</span></span>
                  <span class="text-gray-500">最终得分：<span class="font-bold" :class="detailTabs[activeDetailTab].color">{{ formatScore(detailData[detailTabs[activeDetailTab].scoreKey]) }}</span></span>
                </div>
              </div>
              <table class="w-full">
                <thead class="bg-gray-50">
                  <tr class="border-b border-gray-200">
                    <th class="px-4 py-2 text-left text-sm font-medium text-gray-900">{{ activeDetailTab === 'intellectual' ? '课程' : '项目' }}</th>
                    <th class="px-4 py-2 text-center text-sm font-medium text-gray-900">{{ activeDetailTab === 'intellectual' ? '成绩' : '分数' }}</th>
                    <th v-if="activeDetailTab === 'intellectual'" class="px-4 py-2 text-center text-sm font-medium text-gray-900">学分</th>
                    <th v-else class="px-4 py-2 text-left text-sm font-medium text-gray-900">说明</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, idx) in currentDetailList" :key="idx" class="border-b border-gray-100">
                    <td class="px-4 py-2 text-sm text-gray-900">{{ item.itemName || item.courseName }}</td>
                    <td class="px-4 py-2 text-center text-sm" :class="item.score >= 0 ? 'text-green-600' : 'text-red-600'">{{ item.score >= 0 ? '+' : '' }}{{ item.score }}</td>
                    <td v-if="activeDetailTab === 'intellectual'" class="px-4 py-2 text-center text-sm text-gray-600">{{ item.credit }}</td>
                    <td v-else class="px-4 py-2 text-sm text-gray-500">{{ item.remark || '-' }}</td>
                  </tr>
                  <tr v-if="!currentDetailList || currentDetailList.length === 0">
                    <td :colspan="activeDetailTab === 'intellectual' ? 3 : 3" class="py-8 text-center text-sm text-gray-400">暂无明细数据</td>
                  </tr>
                </tbody>
              </table>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { RefreshCw, ArrowUpDown, Download, X, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageEvaluationResults, getEvaluationResultDetail, calculateClassEvaluation, calculateRankings, recalculateEvaluation, pagePeriods, type EvaluationResult, type EvaluationPeriod } from '@/api/evaluation'
import { getAllClasses } from '@/api/organization'

const queryParams = reactive({ pageNum: 1, pageSize: 10, periodId: undefined as number | undefined, studentNo: '', studentName: '', classId: undefined as number | undefined })
const loading = ref(false)
const tableData = ref<EvaluationResult[]>([])
const total = ref(0)
const periodList = ref<EvaluationPeriod[]>([])
const classList = ref<{ id: number; className: string }[]>([])
const statistics = ref<Record<string, any>>({})

const detailDialogVisible = ref(false)
const detailData = ref<any>({})
const activeDetailTab = ref<'moral' | 'intellectual' | 'physical' | 'aesthetic' | 'labor' | 'development'>('moral')

const dimensions = {
  moral: { label: '德育', weight: '25%', color: 'text-green-600', scoreKey: 'moralScore' },
  intellectual: { label: '智育', weight: '40%', color: 'text-blue-600', scoreKey: 'intellectualScore' },
  physical: { label: '体育', weight: '10%', color: 'text-amber-600', scoreKey: 'physicalScore' },
  aesthetic: { label: '美育', weight: '10%', color: 'text-pink-600', scoreKey: 'aestheticScore' },
  labor: { label: '劳育', weight: '10%', color: 'text-purple-600', scoreKey: 'laborScore' },
  development: { label: '发展', weight: '5%', color: 'text-indigo-600', scoreKey: 'developmentScore' }
}
const detailTabs = {
  moral: { label: '德育', baseScore: 60, color: 'text-green-600', scoreKey: 'moralScore', detailKey: 'moralDetails' },
  intellectual: { label: '智育', baseScore: '-', color: 'text-blue-600', scoreKey: 'intellectualScore', detailKey: 'intellectualDetails' },
  physical: { label: '体育', baseScore: 60, color: 'text-amber-600', scoreKey: 'physicalScore', detailKey: 'physicalDetails' },
  aesthetic: { label: '美育', baseScore: 60, color: 'text-pink-600', scoreKey: 'aestheticScore', detailKey: 'aestheticDetails' },
  labor: { label: '劳育', baseScore: 60, color: 'text-purple-600', scoreKey: 'laborScore', detailKey: 'laborDetails' },
  development: { label: '发展素质', baseScore: 0, color: 'text-indigo-600', scoreKey: 'developmentScore', detailKey: 'developmentDetails' }
}
const currentDetailList = computed(() => detailData.value[detailTabs[activeDetailTab.value].detailKey] || [])

const formatScore = (score: number | undefined) => (score === undefined || score === null) ? '-' : score.toFixed(2)
const getScoreClass = (score: number | undefined) => {
  if (score === undefined || score === null) return 'text-sm text-gray-400'
  if (score >= 90) return 'text-sm font-bold text-green-600'
  if (score >= 80) return 'text-sm font-medium text-blue-600'
  if (score >= 70) return 'text-sm text-amber-600'
  if (score >= 60) return 'text-sm text-gray-600'
  return 'text-sm text-red-600'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await pageEvaluationResults(queryParams)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}
const handleSearch = () => { queryParams.pageNum = 1; fetchData() }
const handleReset = () => { Object.assign(queryParams, { periodId: undefined, studentNo: '', studentName: '', classId: undefined }); handleSearch() }
const handlePageChange = (page: number) => { queryParams.pageNum = page; fetchData() }

const handleViewDetail = async (row: EvaluationResult) => {
  try {
    const res = await getEvaluationResultDetail(row.id!)
    detailData.value = res.data || {}
    activeDetailTab.value = 'moral'
    detailDialogVisible.value = true
  } catch { ElMessage.error('加载详情失败') }
}
const handleRecalculate = async (row: EvaluationResult) => {
  try {
    await ElMessageBox.confirm('确定要重新计算该学生的综测吗？', '提示', { type: 'warning' })
    await recalculateEvaluation(row.id!)
    ElMessage.success('重新计算完成')
    fetchData()
  } catch { /* cancelled */ }
}
const handleCalculateClass = async () => {
  if (!queryParams.classId) { ElMessage.warning('请先选择班级'); return }
  if (!queryParams.periodId) { ElMessage.warning('请先选择综测周期'); return }
  try {
    await ElMessageBox.confirm('确定要计算该班级的综测吗？', '提示', { type: 'warning' })
    await calculateClassEvaluation(queryParams.periodId, queryParams.classId)
    ElMessage.success('计算完成')
    fetchData()
  } catch { /* cancelled */ }
}
const handleCalculateRanking = async () => {
  if (!queryParams.periodId) { ElMessage.warning('请先选择综测周期'); return }
  try {
    await ElMessageBox.confirm('确定要计算该周期的排名吗？', '提示', { type: 'warning' })
    await calculateRankings(queryParams.periodId)
    ElMessage.success('排名计算完成')
    fetchData()
  } catch { /* cancelled */ }
}
const handleExport = () => ElMessage.info('导出功能开发中')

const loadPeriodList = async () => {
  try {
    const res = await pagePeriods({ pageNum: 1, pageSize: 100 })
    periodList.value = res.data?.records || []
    const current = periodList.value.find(p => p.status === 4 || p.status === 3)
    if (current) queryParams.periodId = current.id
  } catch (e) { console.error('加载周期列表失败', e) }
}
const loadClassList = async () => {
  try {
    const res = await getAllClasses()
    classList.value = (res || []).map((c: any) => ({ id: c.id, className: c.className }))
  } catch (e) { console.error('加载班级列表失败', e) }
}

onMounted(() => { loadPeriodList(); loadClassList(); fetchData() })
</script>
