<template>
  <div class="class-ranking-table">
    <el-table
      :data="tableData"
      stripe
      border
      max-height="500"
      :default-sort="{ prop: 'ranking', order: 'ascending' }"
    >
      <el-table-column type="index" label="#" width="50" align="center" />
      <el-table-column prop="ranking" label="排名" width="70" align="center" sortable>
        <template #default="{ row }">
          <span :class="getRankingClass(row.ranking)">{{ row.ranking }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="className" label="班级" min-width="120" />
      <el-table-column prop="gradeName" label="年级" width="100" />
      <el-table-column prop="teacherName" label="班主任" width="100" />
      <el-table-column prop="checkCount" label="检查次数" width="90" align="center">
        <template #default="{ row }">
          <el-tooltip :content="`应检${row.expectedCheckCount}次，实检${row.checkCount}次`">
            <span>{{ row.checkCount }}/{{ row.expectedCheckCount }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="coverageRate" label="覆盖率" width="90" align="center" sortable>
        <template #default="{ row }">
          <el-progress
            :percentage="Number(row.coverageRate)"
            :stroke-width="6"
            :show-text="false"
            :color="getCoverageColor(row.coverageRate)"
            style="width: 60px"
          />
          <span style="margin-left: 8px">{{ row.coverageRate }}%</span>
        </template>
      </el-table-column>
      <el-table-column prop="totalScore" label="总扣分" width="90" align="right" sortable>
        <template #default="{ row }">
          {{ formatNumber(row.totalScore) }}
        </template>
      </el-table-column>
      <el-table-column prop="avgScore" label="平均扣分" width="90" align="right" sortable>
        <template #default="{ row }">
          {{ formatNumber(row.avgScore) }}
        </template>
      </el-table-column>
      <el-table-column prop="weightedScore" label="加权扣分" width="90" align="right" sortable>
        <template #default="{ row }">
          <el-tooltip content="根据覆盖率和缺检策略计算的归一化分数">
            <span>{{ formatNumber(row.weightedScore) }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="scoreLevel" label="评级" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="getLevelType(row.scoreLevel)" size="small">
            {{ row.scoreLevel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="vsAvg" label="与均值差" width="100" align="right" sortable>
        <template #default="{ row }">
          <span :class="getVsAvgClass(row.vsAvg)">
            {{ row.vsAvg > 0 ? '+' : '' }}{{ formatNumber(row.vsAvg) }}
          </span>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="data?.length > 10" class="table-footer">
      <el-button type="primary" link @click="showAll = !showAll">
        {{ showAll ? '收起' : `显示全部 ${data.length} 条` }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { ClassRankingItem } from '@/api/v2/quantification-extra'

const props = defineProps<{
  data: ClassRankingItem[] | null
}>()

const showAll = ref(false)

const tableData = computed(() => {
  if (!props.data) return []
  if (showAll.value) return props.data
  return props.data.slice(0, 20)
})

function formatNumber(value: any): string {
  if (value === null || value === undefined) return '-'
  return Number(value).toFixed(2)
}

function getRankingClass(ranking: number): string {
  if (ranking === 1) return 'rank-gold'
  if (ranking === 2) return 'rank-silver'
  if (ranking === 3) return 'rank-bronze'
  return ''
}

function getCoverageColor(rate: number): string {
  if (rate >= 100) return '#67c23a'
  if (rate >= 80) return '#409eff'
  if (rate >= 60) return '#e6a23c'
  return '#f56c6c'
}

function getLevelType(level: string): string {
  const types: Record<string, string> = {
    '优秀': 'success',
    '良好': '',
    '一般': 'warning',
    '较差': 'danger'
  }
  return types[level] || 'info'
}

function getVsAvgClass(vsAvg: number): string {
  if (vsAvg < 0) return 'text-success'  // 低于平均是好的（扣分少）
  if (vsAvg > 0) return 'text-danger'   // 高于平均是差的（扣分多）
  return ''
}
</script>

<style scoped lang="scss">
.class-ranking-table {
  .rank-gold {
    color: #f7ba2a;
    font-weight: bold;
  }

  .rank-silver {
    color: #909399;
    font-weight: bold;
  }

  .rank-bronze {
    color: #b87333;
    font-weight: bold;
  }

  .text-success {
    color: #67c23a;
  }

  .text-danger {
    color: #f56c6c;
  }

  .table-footer {
    text-align: center;
    padding-top: 15px;
  }
}
</style>
