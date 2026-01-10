<template>
  <div class="rating-card" :class="{ 'is-compact': compact }">
    <!-- 卡片头部 -->
    <div class="card-header">
      <span class="rule-name">{{ ruleName || '评级结果' }}</span>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading && results.length === 0" class="loading-state">
      <el-skeleton :rows="2" animated />
    </div>

    <template v-else-if="results.length > 0">
      <!-- 统计徽章 -->
      <div class="stat-badges" v-if="statistics">
        <div
          v-for="stat in statistics.levelStatistics"
          :key="stat.levelId"
          class="badge"
          :style="{ backgroundColor: stat.levelColor || '#909399' }"
          :title="`${stat.levelName}: ${stat.classCount}个班级 (${stat.percentage.toFixed(1)}%)`"
        >
          {{ stat.levelName.charAt(0) }} {{ stat.classCount }}
        </div>
      </div>

      <!-- 评级列表 -->
      <div class="rating-list">
        <div
          v-for="result in displayResults"
          :key="result.id"
          class="rating-row"
        >
          <span class="rank" :class="getRankClass(result.ranking)">#{{ result.ranking }}</span>
          <span class="name">{{ result.className }}</span>
          <span class="score">{{ result.score.toFixed(1) }}</span>
          <span class="level" :style="{ backgroundColor: result.levelColor || '#909399' }">
            {{ result.levelName }}
          </span>
        </div>
      </div>

      <!-- 展开/收起 -->
      <div v-if="results.length > displayLimit" class="expand-bar" @click="expanded = !expanded">
        {{ expanded ? '收起' : `查看全部 ${results.length} 个` }}
        <el-icon :class="{ 'rotated': expanded }"><ArrowDown /></el-icon>
      </div>
    </template>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <span>暂无数据</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import {
  getRatingResultsByRecord,
  getRatingStatistics,
  calculateDailyRating,
  type RatingResultVO,
  type RatingStatisticsVO
} from '@/api/v2/rating'

const props = defineProps<{
  checkRecordId?: string | number
  ruleId?: string | number
  ruleName?: string
  highlightClassId?: string | number
  compact?: boolean
}>()

const loading = ref(false)
const results = ref<RatingResultVO[]>([])
const statistics = ref<RatingStatisticsVO | null>(null)
const expanded = ref(false)

// 紧凑模式默认显示5条，否则10条
const displayLimit = computed(() => props.compact ? 5 : 10)

const displayResults = computed(() => {
  if (expanded.value || results.value.length <= displayLimit.value) {
    return results.value
  }
  return results.value.slice(0, displayLimit.value)
})

const loadData = async () => {
  if (!props.checkRecordId) return

  loading.value = true
  try {
    let res = await getRatingResultsByRecord(props.checkRecordId)
    let data = res || []

    if (props.ruleId) {
      data = data.filter(r => String(r.ruleId) === String(props.ruleId))
    }

    // 如果没有数据，自动计算
    if (data.length === 0 && props.ruleId) {
      try {
        await calculateDailyRating(props.checkRecordId, props.ruleId, false)
        res = await getRatingResultsByRecord(props.checkRecordId)
        data = (res || []).filter(r => String(r.ruleId) === String(props.ruleId))
      } catch (e) {
        console.error('自动计算评级失败:', e)
      }
    }

    // 加载统计信息
    if (data.length > 0 && props.ruleId) {
      const statsRes = await getRatingStatistics(props.ruleId, props.checkRecordId)
      statistics.value = statsRes
    }

    results.value = data
  } catch (error) {
    console.error('加载评级结果失败:', error)
  } finally {
    loading.value = false
  }
}

const getRankClass = (rank: number) => {
  if (rank === 1) return 'gold'
  if (rank === 2) return 'silver'
  if (rank === 3) return 'bronze'
  return ''
}

watch(() => props.checkRecordId, loadData, { immediate: true })

defineExpose({ loadData })
</script>

<style scoped>
.rating-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  padding: 12px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.rule-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.loading-state {
  padding: 10px 0;
}

.stat-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.badge {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  color: #fff;
  font-weight: 500;
}

.rating-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
}

.rating-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: #fafafa;
  border-radius: 4px;
  font-size: 13px;
}

.rating-row:hover {
  background: #f0f2f5;
}

.rank {
  font-weight: 600;
  min-width: 28px;
  color: #909399;
}

.rank.gold { color: #e6a23c; }
.rank.silver { color: #909399; }
.rank.bronze { color: #b8702c; }

.name {
  flex: 1;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.score {
  color: #f56c6c;
  font-weight: 500;
  min-width: 40px;
  text-align: right;
}

.level {
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 11px;
  color: #fff;
  min-width: 32px;
  text-align: center;
}

.expand-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 6px 0;
  margin-top: 6px;
  font-size: 12px;
  color: #409eff;
  cursor: pointer;
  border-top: 1px solid #f0f0f0;
}

.expand-bar:hover {
  color: #66b1ff;
}

.expand-bar .el-icon {
  transition: transform 0.2s;
}

.expand-bar .el-icon.rotated {
  transform: rotate(180deg);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: #909399;
  font-size: 13px;
  gap: 8px;
}

/* 紧凑模式 */
.is-compact {
  padding: 10px;
}

.is-compact .card-header {
  margin-bottom: 8px;
  padding-bottom: 6px;
}

.is-compact .rule-name {
  font-size: 13px;
}

.is-compact .stat-badges {
  margin-bottom: 8px;
}

.is-compact .rating-row {
  padding: 4px 6px;
  font-size: 12px;
}

.is-compact .rank {
  min-width: 24px;
}

.is-compact .empty-state {
  padding: 15px;
}
</style>
