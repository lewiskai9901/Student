<!--
  学生个人检查记录组件
  显示针对当前登录学生的所有扣分记录
  数据权限：使用 inspection_personal 模块配置，通过 target_id 过滤
-->
<template>
  <div class="personal-inspection-records">
    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      <div class="bg-white rounded-xl p-4 border border-gray-100 shadow-sm">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500">本月扣分</p>
            <p class="text-2xl font-bold text-red-600">{{ stats.monthDeduction }}</p>
          </div>
          <div class="w-10 h-10 rounded-lg bg-red-50 flex items-center justify-center">
            <TrendingDown class="h-5 w-5 text-red-500" />
          </div>
        </div>
      </div>

      <div class="bg-white rounded-xl p-4 border border-gray-100 shadow-sm">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500">本学期扣分</p>
            <p class="text-2xl font-bold text-orange-600">{{ stats.semesterDeduction }}</p>
          </div>
          <div class="w-10 h-10 rounded-lg bg-orange-50 flex items-center justify-center">
            <Calendar class="h-5 w-5 text-orange-500" />
          </div>
        </div>
      </div>

      <div class="bg-white rounded-xl p-4 border border-gray-100 shadow-sm">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500">申诉中</p>
            <p class="text-2xl font-bold text-blue-600">{{ stats.appealPending }}</p>
          </div>
          <div class="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center">
            <MessageSquare class="h-5 w-5 text-blue-500" />
          </div>
        </div>
      </div>

      <div class="bg-white rounded-xl p-4 border border-gray-100 shadow-sm">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500">申诉成功</p>
            <p class="text-2xl font-bold text-green-600">{{ stats.appealSuccess }}</p>
          </div>
          <div class="w-10 h-10 rounded-lg bg-green-50 flex items-center justify-center">
            <CheckCircle class="h-5 w-5 text-green-500" />
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="bg-white rounded-xl p-4 border border-gray-100 shadow-sm mb-6">
      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center gap-2">
          <Calendar class="h-4 w-4 text-gray-400" />
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            class="w-64"
            @change="handleSearch"
          />
        </div>

        <div class="flex items-center gap-2">
          <Filter class="h-4 w-4 text-gray-400" />
          <el-select v-model="filterCategory" placeholder="扣分类别" clearable class="w-36" @change="handleSearch">
            <el-option
              v-for="cat in categories"
              :key="cat.code"
              :label="cat.name"
              :value="cat.code"
            />
          </el-select>
        </div>

        <div class="flex items-center gap-2">
          <el-select v-model="filterStatus" placeholder="申诉状态" clearable class="w-32" @change="handleSearch">
            <el-option label="未申诉" value="NONE" />
            <el-option label="申诉中" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </div>

        <div class="ml-auto">
          <el-button @click="handleRefresh" :loading="loading">
            <RefreshCw class="h-4 w-4 mr-1" />
            刷新
          </el-button>
        </div>
      </div>
    </div>

    <!-- 记录列表 -->
    <div class="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden">
      <div class="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <FileText class="h-5 w-5 text-gray-400" />
          <span class="font-medium text-gray-900">扣分记录</span>
          <span class="text-sm text-gray-400">({{ filteredRecords.length }} 条)</span>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="flex items-center justify-center py-16">
        <Loader2 class="h-8 w-8 text-blue-600 animate-spin" />
        <span class="ml-3 text-gray-500">加载中...</span>
      </div>

      <!-- 空状态 -->
      <div v-else-if="filteredRecords.length === 0" class="flex flex-col items-center justify-center py-16">
        <CheckCircle class="h-12 w-12 text-green-300 mb-3" />
        <p class="text-gray-500">暂无扣分记录</p>
        <p class="text-sm text-gray-400 mt-1">继续保持！</p>
      </div>

      <!-- 记录列表 -->
      <div v-else class="divide-y divide-gray-100">
        <div
          v-for="record in filteredRecords"
          :key="record.id"
          class="px-4 py-4 hover:bg-gray-50 transition-colors"
        >
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <div class="flex items-center gap-2 mb-1">
                <span class="font-medium text-gray-900">{{ record.itemName }}</span>
                <span
                  class="px-2 py-0.5 rounded-full text-xs"
                  :class="getCategoryStyle(record.categoryCode)"
                >
                  {{ record.categoryName }}
                </span>
              </div>

              <div class="text-sm text-gray-500 mb-2">
                {{ record.remark || '无备注' }}
              </div>

              <div class="flex items-center gap-4 text-xs text-gray-400">
                <span class="flex items-center gap-1">
                  <Calendar class="h-3.5 w-3.5" />
                  {{ record.checkDate }}
                </span>
                <span class="flex items-center gap-1">
                  <MapPin class="h-3.5 w-3.5" />
                  {{ record.spaceName || '-' }}
                </span>
                <span class="flex items-center gap-1">
                  <User class="h-3.5 w-3.5" />
                  {{ record.inspectorName }}
                </span>
              </div>
            </div>

            <div class="flex flex-col items-end gap-2 ml-4">
              <!-- 扣分数值 -->
              <span class="text-lg font-bold text-red-600">
                -{{ record.deductionScore }}
              </span>

              <!-- 申诉状态 -->
              <span
                v-if="record.appealStatus"
                class="px-2 py-0.5 rounded-full text-xs"
                :class="getAppealStatusStyle(record.appealStatus)"
              >
                {{ getAppealStatusLabel(record.appealStatus) }}
              </span>

              <!-- 操作按钮 -->
              <el-button
                v-if="!record.appealStatus && canAppeal(record)"
                type="primary"
                size="small"
                link
                @click="handleAppeal(record)"
              >
                <MessageSquare class="h-4 w-4 mr-1" />
                申诉
              </el-button>
            </div>
          </div>

          <!-- 图片证据 -->
          <div v-if="record.photos && record.photos.length > 0" class="mt-3 flex gap-2">
            <div
              v-for="(photo, idx) in record.photos.slice(0, 4)"
              :key="idx"
              class="w-16 h-16 rounded-lg overflow-hidden cursor-pointer hover:opacity-80"
              @click="previewImage(photo)"
            >
              <img :src="photo" class="w-full h-full object-cover" />
            </div>
            <div
              v-if="record.photos.length > 4"
              class="w-16 h-16 rounded-lg bg-gray-100 flex items-center justify-center text-gray-500 text-sm"
            >
              +{{ record.photos.length - 4 }}
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="filteredRecords.length > 0" class="px-4 py-3 border-t border-gray-100 flex items-center justify-between">
        <span class="text-sm text-gray-500">
          共 {{ total }} 条记录
        </span>
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next"
          @change="handlePageChange"
        />
      </div>
    </div>

    <!-- 申诉对话框 -->
    <el-dialog
      v-model="appealDialogVisible"
      title="提交申诉"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="appealFormRef" :model="appealForm" :rules="appealRules" label-width="80px">
        <el-form-item label="扣分项目">
          <span class="text-gray-900">{{ appealRecord?.itemName }}</span>
          <span class="ml-2 text-red-600 font-medium">-{{ appealRecord?.deductionScore }}分</span>
        </el-form-item>

        <el-form-item label="申诉理由" prop="reason">
          <el-input
            v-model="appealForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请详细说明申诉理由..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="证据图片">
          <el-upload
            v-model:file-list="appealForm.attachments"
            :action="uploadUrl"
            list-type="picture-card"
            :limit="5"
            accept="image/*"
          >
            <Plus class="h-6 w-6" />
          </el-upload>
          <div class="text-xs text-gray-400 mt-1">最多上传5张图片作为证据</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="appealDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAppeal">
          提交申诉
        </el-button>
      </template>
    </el-dialog>

    <!-- 图片预览 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewList"
      :initial-index="previewIndex"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  TrendingDown,
  Calendar,
  MessageSquare,
  CheckCircle,
  Filter,
  RefreshCw,
  FileText,
  Loader2,
  MapPin,
  User,
  Plus
} from 'lucide-vue-next'
import { http } from '@/utils/request'

// Props
defineProps<{
  studentId?: number
}>()

// 类型定义
interface DeductionRecord {
  id: number
  itemName: string
  itemCode: string
  categoryCode: string
  categoryName: string
  deductionScore: number
  remark: string
  checkDate: string
  spaceName: string
  inspectorName: string
  photos: string[]
  appealStatus?: string
  appealId?: number
}

interface Statistics {
  monthDeduction: number
  semesterDeduction: number
  appealPending: number
  appealSuccess: number
}

// 状态
const loading = ref(false)
const submitting = ref(false)
const records = ref<DeductionRecord[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

// 筛选
const dateRange = ref<[string, string] | null>(null)
const filterCategory = ref('')
const filterStatus = ref('')

// 统计
const stats = ref<Statistics>({
  monthDeduction: 0,
  semesterDeduction: 0,
  appealPending: 0,
  appealSuccess: 0
})

// 类别选项
const categories = ref<{ code: string; name: string }[]>([
  { code: 'DORMITORY', name: '宿舍卫生' },
  { code: 'DISCIPLINE', name: '纪律' },
  { code: 'APPEARANCE', name: '仪容仪表' },
  { code: 'OTHER', name: '其他' }
])

// 申诉对话框
const appealDialogVisible = ref(false)
const appealRecord = ref<DeductionRecord | null>(null)
const appealFormRef = ref()
const appealForm = reactive({
  reason: '',
  attachments: [] as any[]
})
const appealRules = {
  reason: [{ required: true, message: '请输入申诉理由', trigger: 'blur' }]
}
const uploadUrl = '/api/files/upload'

// 图片预览
const previewVisible = ref(false)
const previewList = ref<string[]>([])
const previewIndex = ref(0)

// 计算属性
const filteredRecords = computed(() => {
  let result = records.value

  if (dateRange.value && dateRange.value.length === 2) {
    const [start, end] = dateRange.value
    result = result.filter(r => r.checkDate >= start && r.checkDate <= end)
  }

  if (filterCategory.value) {
    result = result.filter(r => r.categoryCode === filterCategory.value)
  }

  if (filterStatus.value) {
    if (filterStatus.value === 'NONE') {
      result = result.filter(r => !r.appealStatus)
    } else {
      result = result.filter(r => r.appealStatus === filterStatus.value)
    }
  }

  return result
})

// 方法
async function loadRecords() {
  loading.value = true
  try {
    // 调用后端API获取个人扣分记录
    // 后端会根据数据权限自动按 target_id 过滤
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    const res = await http.get<{ records: DeductionRecord[]; total: number }>('/inspection/personal/deductions', { params })
    records.value = res.records || []
    total.value = res.total || 0
  } catch (error: any) {
    ElMessage.error('加载失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await http.get<Statistics>('/inspection/personal/statistics')
    stats.value = res || {
      monthDeduction: 0,
      semesterDeduction: 0,
      appealPending: 0,
      appealSuccess: 0
    }
  } catch {
    // 忽略统计加载错误
  }
}

function handleSearch() {
  pageNum.value = 1
  loadRecords()
}

function handleRefresh() {
  loadRecords()
  loadStats()
}

function handlePageChange() {
  loadRecords()
}

// 判断是否可以申诉
function canAppeal(record: DeductionRecord): boolean {
  // 7天内可申诉
  const checkDate = new Date(record.checkDate)
  const now = new Date()
  const diffDays = (now.getTime() - checkDate.getTime()) / (1000 * 60 * 60 * 24)
  return diffDays <= 7
}

// 打开申诉对话框
function handleAppeal(record: DeductionRecord) {
  appealRecord.value = record
  appealForm.reason = ''
  appealForm.attachments = []
  appealDialogVisible.value = true
}

// 提交申诉
async function submitAppeal() {
  if (!appealFormRef.value) return

  try {
    await appealFormRef.value.validate()
    submitting.value = true

    const data = {
      deductionDetailId: appealRecord.value!.id,
      reason: appealForm.reason,
      attachments: appealForm.attachments.map((f: any) => f.response?.url || f.url).join(',')
    }

    await http.post('/appeals', data)
    ElMessage.success('申诉已提交')
    appealDialogVisible.value = false
    loadRecords()
    loadStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('提交失败: ' + (error.message || '未知错误'))
    }
  } finally {
    submitting.value = false
  }
}

// 预览图片
function previewImage(url: string) {
  previewList.value = [url]
  previewIndex.value = 0
  previewVisible.value = true
}

// 获取类别样式
function getCategoryStyle(code: string): string {
  const styles: Record<string, string> = {
    DORMITORY: 'bg-blue-50 text-blue-600',
    DISCIPLINE: 'bg-red-50 text-red-600',
    APPEARANCE: 'bg-purple-50 text-purple-600',
    OTHER: 'bg-gray-100 text-gray-600'
  }
  return styles[code] || 'bg-gray-100 text-gray-600'
}

// 获取申诉状态样式
function getAppealStatusStyle(status: string): string {
  const styles: Record<string, string> = {
    PENDING: 'bg-amber-50 text-amber-600',
    PENDING_LEVEL1_REVIEW: 'bg-blue-50 text-blue-600',
    PENDING_LEVEL2_REVIEW: 'bg-indigo-50 text-indigo-600',
    APPROVED: 'bg-green-50 text-green-600',
    REJECTED: 'bg-red-50 text-red-600'
  }
  return styles[status] || 'bg-gray-100 text-gray-600'
}

// 获取申诉状态标签
function getAppealStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    PENDING: '待审核',
    PENDING_LEVEL1_REVIEW: '一级审核中',
    PENDING_LEVEL2_REVIEW: '二级审核中',
    APPROVED: '申诉成功',
    REJECTED: '申诉被驳回'
  }
  return labels[status] || status
}

// 初始化
onMounted(() => {
  loadRecords()
  loadStats()
})
</script>

<style scoped>
:deep(.el-date-editor) {
  --el-date-editor-width: 240px;
}
</style>
