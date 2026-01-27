<template>
  <div class="rating-audit-view">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="page-header__left">
        <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
        <div class="page-header__title">
          <h2>{{ planName }}</h2>
          <span class="page-header__subtitle">评级审核管理</span>
        </div>
      </div>
      <div class="page-header__right">
        <el-button
          type="primary"
          :icon="Check"
          :disabled="selectedRows.length === 0"
          @click="handleBatchApprove"
        >
          批量通过 ({{ selectedRows.length }})
        </el-button>
        <el-button
          type="danger"
          :icon="X"
          :disabled="selectedRows.length === 0"
          @click="handleBatchReject"
        >
          批量驳回 ({{ selectedRows.length }})
        </el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-bar__left">
        <el-select
          v-model="filterStatus"
          placeholder="审核状态"
          style="width: 120px"
          clearable
          @change="loadData"
        >
          <el-option label="待审核" :value="APPROVAL_STATUS.PENDING" />
          <el-option label="已通过" :value="APPROVAL_STATUS.APPROVED" />
          <el-option label="已驳回" :value="APPROVAL_STATUS.REJECTED" />
        </el-select>
        <el-select
          v-model="filterPublishStatus"
          placeholder="发布状态"
          style="width: 120px"
          clearable
          @change="loadData"
        >
          <el-option label="未发布" :value="PUBLISH_STATUS.UNPUBLISHED" />
          <el-option label="已发布" :value="PUBLISH_STATUS.PUBLISHED" />
        </el-select>
        <el-select
          v-model="filterRuleId"
          placeholder="评级规则"
          style="width: 180px"
          clearable
          @change="loadData"
        >
          <el-option
            v-for="rule in ruleOptions"
            :key="rule.id"
            :label="rule.ruleName"
            :value="rule.id"
          />
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索班级"
          :prefix-icon="Search"
          clearable
          style="width: 180px"
          @input="handleSearch"
        />
      </div>
      <div class="filter-bar__right">
        <el-button :icon="Download" @click="handleExport">导出</el-button>
        <el-button :icon="RefreshCw" @click="loadData">刷新</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-section">
      <div class="stat-card pending">
        <div class="stat-icon">
          <Clock />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.pending }}</div>
          <div class="stat-label">待审核</div>
        </div>
      </div>
      <div class="stat-card approved">
        <div class="stat-icon">
          <CheckCircle />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.approved }}</div>
          <div class="stat-label">已通过</div>
        </div>
      </div>
      <div class="stat-card rejected">
        <div class="stat-icon">
          <XCircle />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.rejected }}</div>
          <div class="stat-label">已驳回</div>
        </div>
      </div>
      <div class="stat-card published">
        <div class="stat-icon">
          <Send />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.published }}</div>
          <div class="stat-label">已发布</div>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-section">
      <el-table
        ref="tableRef"
        :data="filteredResults"
        v-loading="loading"
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" :selectable="canSelect" />
        <el-table-column prop="checkDate" label="检查日期" width="110" />
        <el-table-column prop="className" label="班级" min-width="120" />
        <el-table-column prop="gradeName" label="年级" width="100" />
        <el-table-column prop="ruleName" label="评级规则" min-width="120" />
        <el-table-column prop="levelName" label="评级等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :color="row.levelColor"
              :style="{ color: '#fff', border: 'none' }"
              size="small"
            >
              {{ row.levelName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ranking" label="排名" width="70" align="center">
          <template #default="{ row }">
            <span :class="getRankClass(row.ranking)">#{{ row.ranking }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="扣分" width="80" align="center">
          <template #default="{ row }">
            <span class="score-value">{{ row.score?.toFixed(1) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getApprovalStatusType(row.approvalStatus)" size="small">
              {{ APPROVAL_STATUS_LABELS[row.approvalStatus ?? 0] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.publishStatus === 1 ? 'success' : 'info'" size="small">
              {{ PUBLISH_STATUS_LABELS[row.publishStatus ?? 0] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.approvalStatus === APPROVAL_STATUS.PENDING"
              type="success"
              link
              size="small"
              @click="handleApprove([row])"
            >
              通过
            </el-button>
            <el-button
              v-if="row.approvalStatus === APPROVAL_STATUS.PENDING"
              type="danger"
              link
              size="small"
              @click="handleReject([row])"
            >
              驳回
            </el-button>
            <el-button
              v-if="row.approvalStatus === APPROVAL_STATUS.APPROVED && row.publishStatus === PUBLISH_STATUS.UNPUBLISHED"
              type="primary"
              link
              size="small"
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button
              v-if="row.publishStatus === PUBLISH_STATUS.PUBLISHED"
              type="warning"
              link
              size="small"
              @click="handleUnpublish(row)"
            >
              撤回
            </el-button>
            <el-button
              type="primary"
              link
              size="small"
              @click="handleModify(row)"
            >
              修改
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 审核对话框 -->
    <el-dialog
      v-model="showApprovalDialog"
      :title="approvalAction === 'APPROVE' ? '审核通过' : '审核驳回'"
      width="500px"
    >
      <el-form :model="approvalForm" label-width="80px">
        <el-form-item label="选中数量">
          <span class="approval-count">{{ approvalRows.length }} 条评级结果</span>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="approvalForm.remark"
            type="textarea"
            :rows="3"
            :placeholder="approvalAction === 'APPROVE' ? '可选，填写审核通过说明' : '请填写驳回原因'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApprovalDialog = false">取消</el-button>
        <el-button
          :type="approvalAction === 'APPROVE' ? 'success' : 'danger'"
          :loading="submitting"
          @click="submitApproval"
        >
          {{ approvalAction === 'APPROVE' ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 修改评级对话框 -->
    <el-dialog
      v-model="showModifyDialog"
      title="修改评级等级"
      width="500px"
    >
      <el-form :model="modifyForm" label-width="100px">
        <el-form-item label="班级">
          <span>{{ modifyRow?.className }}</span>
        </el-form-item>
        <el-form-item label="当前等级">
          <el-tag
            :color="modifyRow?.levelColor"
            :style="{ color: '#fff', border: 'none' }"
          >
            {{ modifyRow?.levelName }}
          </el-tag>
        </el-form-item>
        <el-form-item label="新等级" required>
          <el-select v-model="modifyForm.newLevelId" placeholder="选择新等级" style="width: 100%">
            <el-option
              v-for="level in levelOptions"
              :key="level.id"
              :label="level.levelName"
              :value="level.id"
            >
              <span
                class="level-dot"
                :style="{ backgroundColor: level.levelColor }"
              ></span>
              {{ level.levelName }}
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="修改原因" required>
          <el-input
            v-model="modifyForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请填写修改原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showModifyDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitModify">
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft, Check, X, Search, RefreshCw, Clock,
  CheckCircle, XCircle, Send, Download
} from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRatingRulesByPlanId,
  getRatingResultsByRecord,
  getSummaryRatingResults,
  getPendingRatingResults,
  approveRatingResults,
  modifyRatingResult,
  publishRatingResult,
  unpublishRatingResult,
  exportRatingResults,
  APPROVAL_STATUS,
  APPROVAL_STATUS_LABELS,
  APPROVAL_STATUS_TYPES,
  PUBLISH_STATUS,
  PUBLISH_STATUS_LABELS,
  type RatingResultVO,
  type RatingRuleVO,
  type RatingLevelVO
} from '@/api/rating'
import { downloadBlob, getFilenameFromContentDisposition } from '@/utils/export'

const route = useRoute()
const router = useRouter()

// Props
const props = defineProps<{
  checkPlanId?: string | number
  checkPlanName?: string
}>()

// 计划信息
const planId = computed(() => props.checkPlanId || route.params.planId as string)
const planName = computed(() => props.checkPlanName || route.query.planName as string || '检查计划')

// 筛选条件
const filterStatus = ref<number | null>(null)
const filterPublishStatus = ref<number | null>(null)
const filterRuleId = ref<string | number | null>(null)
const searchKeyword = ref('')

// 数据
const results = ref<RatingResultVO[]>([])
const ruleOptions = ref<RatingRuleVO[]>([])
const selectedRows = ref<RatingResultVO[]>([])
const tableRef = ref()

// 加载状态
const loading = ref(false)
const submitting = ref(false)

// 审核对话框
const showApprovalDialog = ref(false)
const approvalAction = ref<'APPROVE' | 'REJECT'>('APPROVE')
const approvalRows = ref<RatingResultVO[]>([])
const approvalForm = ref({
  remark: ''
})

// 修改对话框
const showModifyDialog = ref(false)
const modifyRow = ref<RatingResultVO | null>(null)
const modifyForm = ref({
  newLevelId: null as string | number | null,
  reason: ''
})
const levelOptions = ref<RatingLevelVO[]>([])

// 统计数据
const stats = computed(() => {
  return {
    pending: results.value.filter(r => r.approvalStatus === APPROVAL_STATUS.PENDING).length,
    approved: results.value.filter(r => r.approvalStatus === APPROVAL_STATUS.APPROVED).length,
    rejected: results.value.filter(r => r.approvalStatus === APPROVAL_STATUS.REJECTED).length,
    published: results.value.filter(r => r.publishStatus === PUBLISH_STATUS.PUBLISHED).length
  }
})

// 过滤后的数据
const filteredResults = computed(() => {
  let data = results.value

  if (filterStatus.value !== null) {
    data = data.filter(r => r.approvalStatus === filterStatus.value)
  }

  if (filterPublishStatus.value !== null) {
    data = data.filter(r => r.publishStatus === filterPublishStatus.value)
  }

  if (filterRuleId.value) {
    data = data.filter(r => String(r.ruleId) === String(filterRuleId.value))
  }

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    data = data.filter(r =>
      r.className?.toLowerCase().includes(keyword) ||
      r.gradeName?.toLowerCase().includes(keyword)
    )
  }

  return data
})

// 判断行是否可选择（只有待审核的可以批量操作）
const canSelect = (row: RatingResultVO) => {
  return row.approvalStatus === APPROVAL_STATUS.PENDING
}

// 加载评级规则
const loadRules = async () => {
  try {
    const res = await getRatingRulesByPlanId(planId.value)
    ruleOptions.value = res || []
  } catch (e) {
    console.error('加载评级规则失败:', e)
  }
}

// 加载评级结果
const loadData = async () => {
  loading.value = true
  try {
    // 先加载待审核的，然后加载全部汇总结果
    const [pendingRes, summaryRes] = await Promise.all([
      getPendingRatingResults(planId.value),
      getSummaryRatingResults(planId.value)
    ])

    // 合并去重
    const allResults = [...(pendingRes || []), ...(summaryRes || [])]
    const uniqueMap = new Map<string, RatingResultVO>()
    allResults.forEach(r => {
      uniqueMap.set(String(r.id), r)
    })
    results.value = Array.from(uniqueMap.values())
  } catch (e) {
    console.error('加载评级结果失败:', e)
  } finally {
    loading.value = false
  }
}

// 处理搜索
const handleSearch = () => {
  // 由 computed 处理
}

// 处理选择变化
const handleSelectionChange = (rows: RatingResultVO[]) => {
  selectedRows.value = rows
}

// 获取排名样式
const getRankClass = (rank: number) => {
  if (rank === 1) return 'rank-gold'
  if (rank === 2) return 'rank-silver'
  if (rank === 3) return 'rank-bronze'
  return ''
}

// 获取审核状态类型
const getApprovalStatusType = (status?: number) => {
  return APPROVAL_STATUS_TYPES[status ?? 0] || 'info'
}

// 批量通过
const handleBatchApprove = () => {
  handleApprove(selectedRows.value)
}

// 批量驳回
const handleBatchReject = () => {
  handleReject(selectedRows.value)
}

// 审核通过
const handleApprove = (rows: RatingResultVO[]) => {
  approvalAction.value = 'APPROVE'
  approvalRows.value = rows
  approvalForm.value.remark = ''
  showApprovalDialog.value = true
}

// 审核驳回
const handleReject = (rows: RatingResultVO[]) => {
  approvalAction.value = 'REJECT'
  approvalRows.value = rows
  approvalForm.value.remark = ''
  showApprovalDialog.value = true
}

// 提交审核
const submitApproval = async () => {
  if (approvalAction.value === 'REJECT' && !approvalForm.value.remark) {
    ElMessage.warning('请填写驳回原因')
    return
  }

  submitting.value = true
  try {
    await approveRatingResults({
      resultIds: approvalRows.value.map(r => r.id),
      action: approvalAction.value,
      remark: approvalForm.value.remark
    })
    ElMessage.success(approvalAction.value === 'APPROVE' ? '审核通过成功' : '驳回成功')
    showApprovalDialog.value = false
    tableRef.value?.clearSelection()
    await loadData()
  } catch (e) {
    console.error('审核失败:', e)
  } finally {
    submitting.value = false
  }
}

// 发布
const handlePublish = async (row: RatingResultVO) => {
  try {
    await ElMessageBox.confirm('确定要发布此评级结果吗？', '发布确认', {
      confirmButtonText: '确定发布',
      cancelButtonText: '取消',
      type: 'info'
    })
    await publishRatingResult(row.id)
    ElMessage.success('发布成功')
    await loadData()
  } catch (e: any) {
    if (e !== 'cancel') {
      console.error('发布失败:', e)
    }
  }
}

// 撤回发布
const handleUnpublish = async (row: RatingResultVO) => {
  try {
    await ElMessageBox.confirm('确定要撤回此评级结果吗？', '撤回确认', {
      confirmButtonText: '确定撤回',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await unpublishRatingResult(row.id)
    ElMessage.success('撤回成功')
    await loadData()
  } catch (e: any) {
    if (e !== 'cancel') {
      console.error('撤回失败:', e)
    }
  }
}

// 修改评级
const handleModify = async (row: RatingResultVO) => {
  modifyRow.value = row
  modifyForm.value = {
    newLevelId: null,
    reason: ''
  }

  // 加载该规则的等级选项
  const rule = ruleOptions.value.find(r => String(r.id) === String(row.ruleId))
  levelOptions.value = rule?.levels || []

  showModifyDialog.value = true
}

// 提交修改
const submitModify = async () => {
  if (!modifyForm.value.newLevelId) {
    ElMessage.warning('请选择新等级')
    return
  }
  if (!modifyForm.value.reason) {
    ElMessage.warning('请填写修改原因')
    return
  }

  submitting.value = true
  try {
    await modifyRatingResult({
      resultId: modifyRow.value!.id,
      newLevelId: modifyForm.value.newLevelId,
      reason: modifyForm.value.reason
    })
    ElMessage.success('修改成功')
    showModifyDialog.value = false
    await loadData()
  } catch (e) {
    console.error('修改失败:', e)
  } finally {
    submitting.value = false
  }
}

// 导出
const handleExport = async () => {
  try {
    ElMessage.info('正在导出...')
    const response = await exportRatingResults({
      checkPlanId: planId.value,
      ruleId: filterRuleId.value || undefined,
      approvalStatus: filterStatus.value ?? undefined,
      publishStatus: filterPublishStatus.value ?? undefined,
      format: 'EXCEL'
    })

    // 处理blob响应
    let blob: Blob
    let filename = `评级结果_${new Date().toISOString().slice(0, 10)}.xlsx`

    if (response && (response as any).data instanceof Blob) {
      blob = (response as any).data
      const contentDisposition = (response as any).headers?.['content-disposition']
      if (contentDisposition) {
        filename = getFilenameFromContentDisposition(contentDisposition)
      }
    } else if (response instanceof Blob) {
      blob = response
    } else {
      throw new Error('导出响应格式错误')
    }

    downloadBlob(blob, filename)
    ElMessage.success('导出成功')
  } catch (error: any) {
    console.error('导出失败:', error)
    ElMessage.error(error.message || '导出失败')
  }
}

// 返回
const goBack = () => {
  router.back()
}

onMounted(async () => {
  await loadRules()
  await loadData()
})
</script>

<style scoped lang="scss">
.rating-audit-view {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100%;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  &__left {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  &__title {
    h2 {
      margin: 0;
      font-size: 18px;
      color: #303133;
    }
  }

  &__subtitle {
    font-size: 14px;
    color: #909399;
    margin-left: 8px;
  }

  &__right {
    display: flex;
    gap: 8px;
  }
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 12px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  .stat-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;

    svg {
      width: 24px;
      height: 24px;
      color: #fff;
    }
  }

  &.pending .stat-icon {
    background: linear-gradient(135deg, #e6a23c, #f5c575);
  }

  &.approved .stat-icon {
    background: linear-gradient(135deg, #67c23a, #95d475);
  }

  &.rejected .stat-icon {
    background: linear-gradient(135deg, #f56c6c, #fab6b6);
  }

  &.published .stat-icon {
    background: linear-gradient(135deg, #409eff, #79bbff);
  }

  .stat-content {
    flex: 1;
  }

  .stat-value {
    font-size: 28px;
    font-weight: 600;
    color: #303133;
  }

  .stat-label {
    font-size: 14px;
    color: #909399;
    margin-top: 4px;
  }
}

.table-section {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 20px;
}

.score-value {
  color: #f56c6c;
  font-weight: 500;
}

.rank-gold { color: #e6a23c; font-weight: 600; }
.rank-silver { color: #909399; font-weight: 600; }
.rank-bronze { color: #b87333; font-weight: 600; }

.approval-count {
  font-size: 16px;
  font-weight: 500;
  color: #409eff;
}

.level-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 8px;
  vertical-align: middle;
}
</style>
