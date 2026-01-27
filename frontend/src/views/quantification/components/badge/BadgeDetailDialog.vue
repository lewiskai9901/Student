<template>
  <el-dialog
    v-model="visible"
    title="徽章详情"
    width="600px"
    :close-on-click-modal="false"
  >
    <div v-if="badgeRecord" class="badge-detail-content">
      <!-- 徽章展示区 -->
      <div class="badge-showcase">
        <div
          class="badge-medal-large"
          :class="getBadgeLevelClass(badgeRecord.badgeLevel)"
        >
          <el-icon :size="80"><Medal /></el-icon>
        </div>
        <h2 class="badge-name">{{ badgeRecord.badgeName }}</h2>
        <el-tag
          :type="getBadgeLevelType(badgeRecord.badgeLevel)"
          size="large"
        >
          {{ getBadgeLevelLabel(badgeRecord.badgeLevel) }}
        </el-tag>
      </div>

      <!-- 基本信息 -->
      <el-divider>基本信息</el-divider>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="班级名称">
          {{ badgeRecord.className }}
        </el-descriptions-item>
        <el-descriptions-item label="授予时间">
          {{ new Date(badgeRecord.grantedAt).toLocaleString() }}
        </el-descriptions-item>
        <el-descriptions-item label="统计周期" :span="2">
          <span v-if="badgeRecord.periodStart && badgeRecord.periodEnd">
            {{ badgeRecord.periodStart }} 至 {{ badgeRecord.periodEnd }}
          </span>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 成就数据 -->
      <el-divider v-if="badgeRecord.achievementData">成就数据</el-divider>
      <div v-if="badgeRecord.achievementData" class="achievement-grid">
        <div
          v-if="badgeRecord.achievementData.frequency"
          class="achievement-card"
        >
          <div class="achievement-icon">
            <el-icon :size="32" color="#409EFF"><TrendCharts /></el-icon>
          </div>
          <div class="achievement-info">
            <p class="achievement-label">获奖次数</p>
            <p class="achievement-value">{{ badgeRecord.achievementData.frequency }}</p>
          </div>
        </div>

        <div
          v-if="badgeRecord.achievementData.rank"
          class="achievement-card"
        >
          <div class="achievement-icon">
            <el-icon :size="32" color="#67C23A"><Trophy /></el-icon>
          </div>
          <div class="achievement-info">
            <p class="achievement-label">排名</p>
            <p class="achievement-value">第 {{ badgeRecord.achievementData.rank }} 名</p>
          </div>
        </div>

        <div
          v-if="badgeRecord.achievementData.rate"
          class="achievement-card"
        >
          <div class="achievement-icon">
            <el-icon :size="32" color="#E6A23C"><DataAnalysis /></el-icon>
          </div>
          <div class="achievement-info">
            <p class="achievement-label">获奖率</p>
            <p class="achievement-value">{{ badgeRecord.achievementData.rate }}%</p>
          </div>
        </div>

        <div
          v-if="badgeRecord.achievementData.consecutiveCount"
          class="achievement-card"
        >
          <div class="achievement-icon">
            <el-icon :size="32" color="#F56C6C"><Star /></el-icon>
          </div>
          <div class="achievement-info">
            <p class="achievement-label">连续次数</p>
            <p class="achievement-value">{{ badgeRecord.achievementData.consecutiveCount }}</p>
          </div>
        </div>
      </div>

      <!-- 证书信息 -->
      <el-divider v-if="badgeRecord.certificateGenerated">荣誉证书</el-divider>
      <div v-if="badgeRecord.certificateGenerated" class="certificate-section">
        <el-alert
          title="证书已生成"
          type="success"
          :closable="false"
          show-icon
        >
          <template #default>
            <p style="margin: 0;">
              荣誉证书已成功生成，可点击下方按钮查看或下载。
            </p>
          </template>
        </el-alert>
        <el-button
          type="primary"
          style="margin-top: 12px; width: 100%;"
          @click="viewCertificate"
        >
          <el-icon><Document /></el-icon>
          查看证书
        </el-button>
      </div>

      <!-- 撤销信息 -->
      <div v-if="badgeRecord.revoked" class="revoked-section">
        <el-alert
          title="徽章已撤销"
          type="warning"
          :closable="false"
          show-icon
        >
          <template #default>
            <p style="margin: 0;">
              <strong>撤销原因：</strong>{{ badgeRecord.revokedReason || '无' }}
            </p>
          </template>
        </el-alert>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Medal, TrendCharts, Trophy } from '@element-plus/icons-vue'
import type { ClassBadgeRecordVO } from '@/api/rating'
import { BADGE_LEVEL_LABELS } from '@/api/rating'

// Props
const props = defineProps<{
  modelValue: boolean
  badgeRecord: ClassBadgeRecordVO | null
}>()

// Emits
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

// 响应式数据
const visible = ref(false)

// 监听visible变化
watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
  }
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 获取徽章等级类名
const getBadgeLevelClass = (level: string) => {
  return `badge-level-${level.toLowerCase()}`
}

// 获取徽章等级类型
const getBadgeLevelType = (level: string) => {
  const typeMap: Record<string, any> = {
    GOLD: 'warning',
    SILVER: 'info',
    BRONZE: 'success'
  }
  return typeMap[level] || 'info'
}

// 获取徽章等级标签
const getBadgeLevelLabel = (level: string) => {
  return BADGE_LEVEL_LABELS[level] || level
}

// 查看证书
const viewCertificate = () => {
  if (props.badgeRecord?.certificateUrl) {
    window.open(props.badgeRecord.certificateUrl, '_blank')
  }
}
</script>

<style scoped lang="scss">
.badge-detail-content {
  .badge-showcase {
    text-align: center;
    padding: 30px 20px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    margin-bottom: 20px;

    .badge-medal-large {
      width: 120px;
      height: 120px;
      margin: 0 auto 20px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 4px solid;
      background: rgba(255, 255, 255, 0.2);
      backdrop-filter: blur(10px);

      &.badge-level-gold {
        border-color: #ffd700;
        color: #ffd700;
      }

      &.badge-level-silver {
        border-color: #c0c0c0;
        color: #c0c0c0;
      }

      &.badge-level-bronze {
        border-color: #cd7f32;
        color: #cd7f32;
      }
    }

    .badge-name {
      margin: 0 0 15px 0;
      font-size: 28px;
      font-weight: 600;
      color: white;
    }
  }

  .achievement-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    gap: 15px;
    margin-bottom: 20px;

    .achievement-card {
      padding: 16px;
      border: 1px solid #ebeef5;
      border-radius: 8px;
      text-align: center;
      transition: all 0.3s;

      &:hover {
        border-color: #409eff;
        box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
      }

      .achievement-icon {
        margin-bottom: 10px;
      }

      .achievement-info {
        .achievement-label {
          margin: 0 0 6px 0;
          font-size: 13px;
          color: #909399;
        }

        .achievement-value {
          margin: 0;
          font-size: 22px;
          font-weight: 600;
          color: #303133;
        }
      }
    }
  }

  .certificate-section {
    margin-bottom: 20px;
  }

  .revoked-section {
    margin-top: 20px;
  }
}
</style>
