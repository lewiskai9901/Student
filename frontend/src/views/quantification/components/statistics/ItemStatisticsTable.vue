<template>
  <el-table
    :data="data"
    :loading="loading"
    stripe
    style="width: 100%"
    max-height="400"
  >
    <el-table-column type="index" label="#" width="50" align="center" />
    <el-table-column prop="itemName" label="扣分项" min-width="150">
      <template #default="{ row }">
        <div class="item-info">
          <span class="item-name">{{ row.itemName }}</span>
          <span class="category-name">{{ row.categoryName }}</span>
        </div>
      </template>
    </el-table-column>
    <el-table-column prop="deductModeDesc" label="扣分模式" width="100" align="center">
      <template #default="{ row }">
        <el-tag size="small" type="info">{{ row.deductModeDesc }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="triggerCount" label="触发次数" width="90" align="center" sortable>
      <template #default="{ row }">
        <span class="count-value">{{ row.triggerCount }}</span>
      </template>
    </el-table-column>
    <el-table-column prop="personCount" label="涉及人次" width="90" align="center" />
    <el-table-column prop="totalScore" label="总扣分" width="90" align="center" sortable>
      <template #default="{ row }">
        <span class="score-value">{{ row.totalScore.toFixed(1) }}</span>
      </template>
    </el-table-column>
    <el-table-column prop="avgScore" label="平均扣分" width="90" align="center">
      <template #default="{ row }">
        {{ row.avgScore.toFixed(1) }}
      </template>
    </el-table-column>
    <el-table-column prop="classCount" label="涉及班级" width="90" align="center" />
    <el-table-column label="TOP3班级" min-width="200">
      <template #default="{ row }">
        <div class="top-classes" v-if="row.topClasses?.length">
          <el-tag
            v-for="(cls, index) in row.topClasses.slice(0, 3)"
            :key="cls.classId"
            :type="getTopClassType(index)"
            size="small"
            class="top-class-tag"
          >
            {{ cls.className }} ({{ cls.totalScore.toFixed(1) }}分)
          </el-tag>
        </div>
        <span v-else class="text-muted">-</span>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import type { ItemStatistics } from '@/api/quantification-extra'

defineProps<{
  data: ItemStatistics[]
  loading?: boolean
}>()

function getTopClassType(index: number): 'danger' | 'warning' | 'info' {
  if (index === 0) return 'danger'
  if (index === 1) return 'warning'
  return 'info'
}
</script>

<style scoped lang="scss">
.item-info {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .item-name {
    font-weight: 500;
  }

  .category-name {
    font-size: 12px;
    color: #909399;
  }
}

.count-value {
  font-weight: 600;
  color: #409eff;
}

.score-value {
  font-weight: 600;
  color: #f56c6c;
}

.text-muted {
  color: #909399;
}

.top-classes {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;

  .top-class-tag {
    max-width: 150px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
