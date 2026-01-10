<template>
  <div class="rating-result-view">
    <!-- 筛选条件 -->
    <el-card shadow="never" class="filter-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="评级配置">
          <el-select
            v-model="queryForm.ratingConfigId"
            placeholder="全部"
            clearable
            style="width: 200px"
            @change="handleQuery"
          >
            <el-option
              v-for="config in ratingConfigs"
              :key="config.id"
              :label="config.ratingName"
              :value="config.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="周期类型">
          <el-select
            v-model="queryForm.periodType"
            placeholder="全部"
            clearable
            style="width: 120px"
            @change="handleQuery"
          >
            <el-option label="日评级" value="DAILY" />
            <el-option label="周评级" value="WEEKLY" />
            <el-option label="月评级" value="MONTHLY" />
          </el-select>
        </el-form-item>

        <el-form-item label="获奖状态">
          <el-select
            v-model="queryForm.awarded"
            placeholder="全部"
            clearable
            style="width: 120px"
            @change="handleQuery"
          >
            <el-option label="已获奖" :value="1" />
            <el-option label="未获奖" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item label="发布状态">
          <el-select
            v-model="queryForm.status"
            placeholder="全部"
            clearable
            style="width: 120px"
            @change="handleQuery"
          >
            <el-option label="草稿" value="DRAFT" />
            <el-option label="待审核" value="PENDING_APPROVAL" />
            <el-option label="已发布" value="PUBLISHED" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 -->
    <el-card shadow="never" class="action-card" v-if="hasPermission('approve')">
      <el-button
        type="success"
        :disabled="selectedResults.length === 0"
        @click="handleBatchApprove(true)"
      >
        批量通过
      </el-button>
      <el-button
        type="warning"
        :disabled="selectedResults.length === 0"
        @click="handleBatchApprove(false)"
      >
        批量驳回
      </el-button>
      <el-button
        type="primary"
        :disabled="selectedResults.length === 0"
        @click="handleBatchPublish"
      >
        批量发布
      </el-button>
    </el-card>

    <!-- 结果列表 -->
    <el-card shadow="never" class="list-card">
      <el-table
        :data="resultList"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        stripe
      >
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="#" width="60" />

        <el-table-column label="评级信息" min-width="180">
          <template #default="{ row }">
            <div class="rating-info-cell">
              <el-icon v-if="row.icon" :style="{ color: row.color }">
                <component :is="row.icon" />
              </el-icon>
              <div>
                <div class="rating-name">{{ row.ratingName }}</div>
                <el-tag size="small" :type="getPeriodTypeTag(row.periodType)">
                  {{ row.periodTypeText }}
                </el-tag>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="班级" min-width="120">
          <template #default="{ row }">
            {{ row.className }}
          </template>
        </el-table-column>

        <el-table-column label="评级周期" min-width="180">
          <template #default="{ row }">
            {{ row.periodText }}
          </template>
        </el-table-column>

        <el-table-column label="排名" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getRankingTag(row.ranking)" effect="dark">
              第{{ row.ranking }}名
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="最终得分" width="100" align="center">
          <template #default="{ row }">
            <span class="score-value">{{ row.finalScore }}</span>
          </template>
        </el-table-column>

        <el-table-column label="获奖状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.awarded === 1 ? 'success' : 'info'">
              {{ row.awarded === 1 ? '已获奖' : '未获奖' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row)">
              详情
            </el-button>

            <el-button
              v-if="row.status === 'PENDING_APPROVAL' && hasPermission('approve')"
              link
              type="success"
              @click="handleApprove(row, true)"
            >
              通过
            </el-button>

            <el-button
              v-if="row.status === 'PENDING_APPROVAL' && hasPermission('approve')"
              link
              type="warning"
              @click="handleApprove(row, false)"
            >
              驳回
            </el-button>

            <el-button
              v-if="
                (row.status === 'DRAFT' || row.status === 'PENDING_APPROVAL') &&
                hasPermission('publish')
              "
              link
              type="primary"
              @click="handlePublish(row)"
            >
              发布
            </el-button>

            <el-button
              v-if="row.status === 'PUBLISHED' && hasPermission('publish')"
              link
              type="danger"
              @click="handleRevoke(row)"
            >
              撤销
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="queryForm.pageNum"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadResults"
        @size-change="loadResults"
        style="margin-top: 16px; justify-content: flex-end"
      />

      <el-empty v-if="!loading && resultList.length === 0" description="暂无评级结果" />
    </el-card>

    <!-- 详情对话框 -->
    <RatingResultDetailDialog
      v-model="detailDialogVisible"
      :result-id="currentResultId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRatingResultPage,
  approveRatingResult,
  batchApproveRatingResults,
  publishRatingResult,
  batchPublishRatingResults,
  revokeRatingResult,
  type RatingResultVO,
  type RatingResultQueryDTO
} from '@/api/v2/rating'
import { getRatingConfigsByPlan, type RatingConfigVO } from '@/api/v2/rating'
import RatingResultDetailDialog from './RatingResultDetailDialog.vue'

// Props
const props = defineProps<{
  checkPlanId: string | number
}>()

// 响应式数据
const loading = ref(false)
const resultList = ref<RatingResultVO[]>([])
const total = ref(0)
const ratingConfigs = ref<RatingConfigVO[]>([])
const selectedResults = ref<RatingResultVO[]>([])
const detailDialogVisible = ref(false)
const currentResultId = ref<number>()

const queryForm = reactive<RatingResultQueryDTO>({
  checkPlanId: props.checkPlanId,
  pageNum: 1,
  pageSize: 20
})

// 权限检查（简化版，实际应从store获取）
const hasPermission = (action: string) => {
  return true // TODO: 实现真实的权限检查
}

// 加载评级配置
const loadConfigs = async () => {
  try {
    const res = await getRatingConfigsByPlan(props.checkPlanId)
    ratingConfigs.value = res.data || []
  } catch (error: any) {
    console.error('加载评级配置失败:', error)
  }
}

// 加载结果列表
const loadResults = async () => {
  loading.value = true
  try {
    const res = await getRatingResultPage(queryForm)
    resultList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 查询
const handleQuery = () => {
  queryForm.pageNum = 1
  loadResults()
}

// 重置
const handleReset = () => {
  Object.assign(queryForm, {
    ratingConfigId: undefined,
    periodType: undefined,
    awarded: undefined,
    status: undefined,
    pageNum: 1,
    pageSize: 20
  })
  loadResults()
}

// 选择变化
const handleSelectionChange = (selection: RatingResultVO[]) => {
  selectedResults.value = selection
}

// 获取周期类型标签
const getPeriodTypeTag = (type: string) => {
  const map: Record<string, string> = {
    DAILY: 'success',
    WEEKLY: 'primary',
    MONTHLY: 'warning'
  }
  return map[type] || ''
}

// 获取排名标签
const getRankingTag = (ranking: number) => {
  if (ranking === 1) return 'danger'
  if (ranking <= 3) return 'warning'
  return 'info'
}

// 获取状态标签
const getStatusTag = (status: string) => {
  const map: Record<string, string> = {
    DRAFT: 'info',
    PENDING_APPROVAL: 'warning',
    PUBLISHED: 'success'
  }
  return map[status] || ''
}

// 查看详情
const handleViewDetail = (row: RatingResultVO) => {
  currentResultId.value = row.id
  detailDialogVisible.value = true
}

// 审核
const handleApprove = async (row: RatingResultVO, approved: boolean) => {
  const action = approved ? '通过' : '驳回'
  try {
    await ElMessageBox.confirm(`确定要${action}该评级结果吗？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await approveRatingResult(row.id, approved)
    ElMessage.success(`${action}成功`)
    await loadResults()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || `${action}失败`)
    }
  }
}

// 批量审核
const handleBatchApprove = async (approved: boolean) => {
  const action = approved ? '通过' : '驳回'
  try {
    await ElMessageBox.confirm(
      `确定要批量${action} ${selectedResults.value.length} 条评级结果吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const ids = selectedResults.value.map((r) => r.id)
    await batchApproveRatingResults(ids, approved)
    ElMessage.success(`批量${action}成功`)
    await loadResults()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || `批量${action}失败`)
    }
  }
}

// 发布
const handlePublish = async (row: RatingResultVO) => {
  try {
    await ElMessageBox.confirm('确定要发布该评级结果吗？发布后将对外可见。', '确认发布', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await publishRatingResult(row.id)
    ElMessage.success('发布成功')
    await loadResults()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '发布失败')
    }
  }
}

// 批量发布
const handleBatchPublish = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要批量发布 ${selectedResults.value.length} 条评级结果吗？`,
      '确认发布',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const ids = selectedResults.value.map((r) => r.id)
    await batchPublishRatingResults(ids)
    ElMessage.success('批量发布成功')
    await loadResults()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批量发布失败')
    }
  }
}

// 撤销发布
const handleRevoke = async (row: RatingResultVO) => {
  try {
    await ElMessageBox.confirm('确定要撤销发布吗？撤销后将不再对外可见。', '确认撤销', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await revokeRatingResult(row.id)
    ElMessage.success('撤销成功')
    await loadResults()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '撤销失败')
    }
  }
}

// 初始化
onMounted(() => {
  loadConfigs()
  loadResults()
})
</script>

<style scoped lang="scss">
.rating-result-view {
  .filter-card,
  .action-card {
    margin-bottom: 16px;
  }

  .rating-info-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .el-icon {
      font-size: 24px;
    }

    .rating-name {
      font-weight: 600;
      margin-bottom: 4px;
    }
  }

  .score-value {
    font-size: 16px;
    font-weight: 600;
    color: #409eff;
  }
}
</style>
