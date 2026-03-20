<template>
  <div class="ranking-table">
    <div v-if="title" class="table-title">{{ title }}</div>
    <el-table
      :data="sortedData"
      size="small"
      :max-height="maxHeight"
      :default-sort="{ prop: 'rank', order: 'ascending' }"
      @sort-change="handleSortChange"
    >
      <el-table-column label="排名" prop="rank" width="70" align="center" sortable="custom">
        <template #default="{ row }">
          <span v-if="row.rank === 1" class="medal medal-gold">1</span>
          <span v-else-if="row.rank === 2" class="medal medal-silver">2</span>
          <span v-else-if="row.rank === 3" class="medal medal-bronze">3</span>
          <span v-else class="rank-number">{{ row.rank }}</span>
        </template>
      </el-table-column>
      <el-table-column label="名称" prop="name" min-width="140" show-overflow-tooltip sortable="custom" />
      <el-table-column label="得分" prop="score" width="90" align="right" sortable="custom">
        <template #default="{ row }">
          <span :class="scoreClass(row.score)">{{ row.score.toFixed(1) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="变化" width="80" align="center" v-if="hasChangeData">
        <template #default="{ row }">
          <span v-if="row.change != null && row.change > 0" class="change-up">
            <el-icon :size="12"><Top /></el-icon>
            {{ row.change.toFixed(1) }}
          </span>
          <span v-else-if="row.change != null && row.change < 0" class="change-down">
            <el-icon :size="12"><Bottom /></el-icon>
            {{ Math.abs(row.change).toFixed(1) }}
          </span>
          <span v-else class="change-stable">-</span>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="!sortedData.length" class="empty-hint">
      <el-empty description="暂无排名数据" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Top, Bottom } from '@element-plus/icons-vue'

export interface RankingItem {
  rank: number
  name: string
  score: number
  change?: number
}

const props = withDefaults(defineProps<{
  data: RankingItem[]
  title?: string
  maxHeight?: number
}>(), {
  maxHeight: 400,
})

const sortKey = ref<string>('rank')
const sortOrder = ref<'ascending' | 'descending'>('ascending')

const hasChangeData = computed(() => {
  return props.data.some(item => item.change != null)
})

const sortedData = computed(() => {
  const items = [...props.data]
  const key = sortKey.value as keyof RankingItem
  const asc = sortOrder.value === 'ascending'

  items.sort((a, b) => {
    const va = a[key] ?? 0
    const vb = b[key] ?? 0
    if (typeof va === 'string' && typeof vb === 'string') {
      return asc ? va.localeCompare(vb) : vb.localeCompare(va)
    }
    return asc ? (va as number) - (vb as number) : (vb as number) - (va as number)
  })
  return items
})

function handleSortChange({ prop, order }: { prop: string; order: string | null }) {
  if (prop && order) {
    sortKey.value = prop
    sortOrder.value = order as 'ascending' | 'descending'
  } else {
    sortKey.value = 'rank'
    sortOrder.value = 'ascending'
  }
}

function scoreClass(score: number): string {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 60) return 'score-average'
  return 'score-poor'
}
</script>

<style scoped>
.ranking-table {
  width: 100%;
}

.table-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  color: #303133;
}

.medal {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
}

.medal-gold {
  background: linear-gradient(135deg, #f5c842, #d4a017);
}

.medal-silver {
  background: linear-gradient(135deg, #c0c0c0, #9e9e9e);
}

.medal-bronze {
  background: linear-gradient(135deg, #cd7f32, #a0522d);
}

.rank-number {
  color: #606266;
  font-size: 13px;
}

.change-up {
  color: #67C23A;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.change-down {
  color: #F56C6C;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.change-stable {
  color: #909399;
  font-size: 12px;
}

.score-excellent {
  color: #67C23A;
  font-weight: 500;
}

.score-good {
  color: #409EFF;
}

.score-average {
  color: #E6A23C;
}

.score-poor {
  color: #F56C6C;
  font-weight: 500;
}

.empty-hint {
  padding: 20px 0;
}
</style>
