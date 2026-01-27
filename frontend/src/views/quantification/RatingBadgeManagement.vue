<template>
  <div class="badge-management-container">
    <!-- 页面标题和操作栏 -->
    <div class="page-header">
      <div class="header-left">
        <h2>荣誉徽章管理</h2>
        <p class="subtitle">配置徽章、设置授予条件、自动授予徽章</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="handleCreateBadge">
          <el-icon><Plus /></el-icon>
          创建徽章
        </el-button>
        <el-button type="success" @click="handleAutoGrant">
          <el-icon><Medal /></el-icon>
          自动授予
        </el-button>
      </div>
    </div>

    <!-- 筛选条件 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="filterForm" inline>
        <el-form-item label="检查计划">
          <el-select
            v-model="filterForm.checkPlanId"
            placeholder="选择检查计划"
            style="width: 200px"
            @change="loadBadges"
          >
            <el-option
              v-for="plan in checkPlans"
              :key="plan.id"
              :label="plan.planName"
              :value="plan.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 徽章列表 -->
    <el-row :gutter="20" class="badge-grid">
      <el-col
        v-for="badge in badges"
        :key="badge.id"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
      >
        <el-card class="badge-card" :class="{ disabled: !badge.enabled }" shadow="hover">
          <!-- 徽章等级标识 -->
          <div
            class="badge-level-tag"
            :style="{ backgroundColor: getBadgeLevelColor(badge.badgeLevel) }"
          >
            {{ getBadgeLevelLabel(badge.badgeLevel) }}
          </div>

          <!-- 徽章图标 -->
          <div class="badge-icon-wrapper">
            <div
              class="badge-icon"
              :style="{ borderColor: getBadgeLevelColor(badge.badgeLevel) }"
            >
              <el-icon :size="48" :color="getBadgeLevelColor(badge.badgeLevel)">
                <Medal />
              </el-icon>
            </div>
          </div>

          <!-- 徽章信息 -->
          <div class="badge-info">
            <h3 class="badge-name">{{ badge.badgeName }}</h3>
            <p class="badge-description">{{ badge.description || '暂无描述' }}</p>

            <!-- 授予条件 -->
            <div class="badge-condition">
              <el-tag size="small" type="info">
                {{ formatCondition(badge.grantCondition) }}
              </el-tag>
            </div>

            <!-- 状态标签 -->
            <div class="badge-status">
              <el-tag v-if="badge.autoGrant" size="small" type="success">自动授予</el-tag>
              <el-tag v-else size="small" type="warning">手动授予</el-tag>
              <el-tag v-if="!badge.enabled" size="small" type="info">已禁用</el-tag>
            </div>

            <!-- 符合条件班级预览 -->
            <div class="qualified-preview" v-if="badge.enabled && qualifiedCounts[badge.id]">
              <el-icon><InfoFilled /></el-icon>
              <span>当前符合 {{ qualifiedCounts[badge.id] }} 个班级</span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="badge-actions">
            <el-button size="small" text type="primary" @click="handleEditBadge(badge)">
              编辑
            </el-button>
            <el-button
              size="small"
              text
              :type="badge.enabled ? 'warning' : 'success'"
              @click="handleToggleBadge(badge)"
            >
              {{ badge.enabled ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" text type="danger" @click="handleDeleteBadge(badge)">
              删除
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 空状态 -->
      <el-col v-if="badges.length === 0" :span="24">
        <el-empty description="暂无徽章，点击右上角创建徽章" />
      </el-col>
    </el-row>

    <!-- 创建/编辑徽章对话框 -->
    <BadgeConfigDialog
      v-model="dialogVisible"
      :badge="currentBadge"
      :check-plan-id="filterForm.checkPlanId"
      @success="handleDialogSuccess"
    />

    <!-- 自动授予对话框 -->
    <AutoGrantDialog
      v-model="autoGrantDialogVisible"
      :check-plan-id="filterForm.checkPlanId"
      @success="handleAutoGrantSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getBadgesByPlan,
  deleteBadge,
  toggleBadgeEnabled,
  getQualifiedClassCount,
  type RatingBadgeVO,
  BADGE_LEVEL_LABELS,
  BADGE_LEVEL_COLORS
} from '@/api/rating'
import { getCheckPlanPage } from '@/api/quantification'
import BadgeConfigDialog from './components/badge/BadgeConfigDialog.vue'
import AutoGrantDialog from './components/badge/AutoGrantDialog.vue'

// 响应式数据
const badges = ref<RatingBadgeVO[]>([])
const checkPlans = ref<any[]>([])
const qualifiedCounts = ref<Record<string, number>>({})

const dialogVisible = ref(false)
const autoGrantDialogVisible = ref(false)
const currentBadge = ref<RatingBadgeVO | null>(null)

// 筛选表单
const filterForm = reactive({
  checkPlanId: undefined as number | undefined
})

// 加载检查计划
const loadCheckPlans = async () => {
  try {
    const res = await getCheckPlanPage({ pageNum: 1, pageSize: 100 })
    checkPlans.value = res?.data?.records || []
    if (checkPlans.value.length > 0) {
      filterForm.checkPlanId = checkPlans.value[0].id
      await loadBadges()
    }
  } catch (error) {
    console.error('加载检查计划失败:', error)
    ElMessage.error('加载检查计划失败，请检查后端服务')
  }
}

// 加载徽章列表
const loadBadges = async () => {
  if (!filterForm.checkPlanId) return

  try {
    const res = await getBadgesByPlan(filterForm.checkPlanId)
    badges.value = res.data || []

    // 加载符合条件的班级数量
    await loadQualifiedCounts()
  } catch (error: any) {
    ElMessage.error(error.message || '加载徽章列表失败')
  }
}

// 加载符合条件的班级数量
const loadQualifiedCounts = async () => {
  const now = new Date()
  const periodEnd = now.toISOString().split('T')[0]
  const periodStart = new Date(now.getFullYear(), now.getMonth() - 3, 1)
    .toISOString()
    .split('T')[0]

  for (const badge of badges.value) {
    try {
      const res = await getQualifiedClassCount({
        badgeId: badge.id,
        periodStart,
        periodEnd
      })
      qualifiedCounts.value[badge.id as string] = res.data
    } catch (error) {
      console.error(`加载徽章 ${badge.id} 符合条件班级数失败:`, error)
    }
  }
}

// 创建徽章
const handleCreateBadge = () => {
  if (!filterForm.checkPlanId) {
    ElMessage.warning('请先选择检查计划')
    return
  }
  currentBadge.value = null
  dialogVisible.value = true
}

// 编辑徽章
const handleEditBadge = (badge: RatingBadgeVO) => {
  currentBadge.value = badge
  dialogVisible.value = true
}

// 切换徽章启用状态
const handleToggleBadge = async (badge: RatingBadgeVO) => {
  try {
    await toggleBadgeEnabled(badge.id, !badge.enabled)
    ElMessage.success(badge.enabled ? '徽章已禁用' : '徽章已启用')
    await loadBadges()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 删除徽章
const handleDeleteBadge = async (badge: RatingBadgeVO) => {
  try {
    await ElMessageBox.confirm(`确定要删除徽章"${badge.badgeName}"吗？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })

    await deleteBadge(badge.id)
    ElMessage.success('徽章删除成功')
    await loadBadges()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 自动授予
const handleAutoGrant = () => {
  if (!filterForm.checkPlanId) {
    ElMessage.warning('请先选择检查计划')
    return
  }
  autoGrantDialogVisible.value = true
}

// 对话框成功回调
const handleDialogSuccess = () => {
  loadBadges()
}

// 自动授予成功回调
const handleAutoGrantSuccess = () => {
  ElMessage.success('自动授予完成')
}

// 获取徽章等级标签
const getBadgeLevelLabel = (level: string) => {
  return BADGE_LEVEL_LABELS[level] || level
}

// 获取徽章等级颜色
const getBadgeLevelColor = (level: string) => {
  return BADGE_LEVEL_COLORS[level] || '#409EFF'
}

// 格式化条件
const formatCondition = (condition: any) => {
  if (!condition) return '暂无条件'

  if (condition.conditionType === 'FREQUENCY') {
    return `获得 ${condition.frequencyThreshold} 次以上`
  } else if (condition.conditionType === 'CONSECUTIVE') {
    return `连续 ${condition.consecutiveThreshold} 次`
  } else if (condition.conditionType === 'MULTI_RULE') {
    return '多规则组合'
  }

  return '暂无条件'
}

// 初始化
onMounted(() => {
  loadCheckPlans()
})
</script>

<style scoped lang="scss">
.badge-management-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  .header-left {
    h2 {
      margin: 0;
      font-size: 24px;
      font-weight: 500;
      color: #303133;
    }

    .subtitle {
      margin: 5px 0 0 0;
      font-size: 14px;
      color: #909399;
    }
  }

  .header-right {
    display: flex;
    gap: 10px;
  }
}

.filter-card {
  margin-bottom: 20px;

  :deep(.el-card__body) {
    padding: 15px 20px;
  }
}

.badge-grid {
  .badge-card {
    position: relative;
    margin-bottom: 20px;
    transition: all 0.3s;

    &.disabled {
      opacity: 0.6;
    }

    &:hover {
      transform: translateY(-4px);
    }

    :deep(.el-card__body) {
      padding: 20px;
    }

    .badge-level-tag {
      position: absolute;
      top: 10px;
      right: 10px;
      padding: 4px 12px;
      border-radius: 12px;
      color: white;
      font-size: 12px;
      font-weight: 500;
    }

    .badge-icon-wrapper {
      display: flex;
      justify-content: center;
      margin-bottom: 15px;

      .badge-icon {
        width: 80px;
        height: 80px;
        border: 3px solid;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        background: white;
      }
    }

    .badge-info {
      text-align: center;

      .badge-name {
        margin: 0 0 8px 0;
        font-size: 18px;
        font-weight: 600;
        color: #303133;
      }

      .badge-description {
        margin: 0 0 12px 0;
        font-size: 13px;
        color: #909399;
        min-height: 36px;
        line-height: 1.5;
      }

      .badge-condition {
        margin-bottom: 10px;
      }

      .badge-status {
        display: flex;
        justify-content: center;
        gap: 8px;
        margin-bottom: 12px;
      }

      .qualified-preview {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 6px;
        padding: 8px;
        background: #f0f9ff;
        border-radius: 6px;
        font-size: 13px;
        color: #409eff;
        margin-bottom: 12px;
      }
    }

    .badge-actions {
      display: flex;
      justify-content: space-around;
      padding-top: 12px;
      border-top: 1px solid #ebeef5;
    }
  }
}
</style>
