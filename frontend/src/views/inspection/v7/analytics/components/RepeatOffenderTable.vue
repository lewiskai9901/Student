<template>
  <div class="repeat-offender-table">
    <el-table
      :data="data"
      size="small"
      :max-height="maxHeight"
      :default-sort="{ prop: 'occurrences', order: 'descending' }"
      stripe
    >
      <el-table-column label="检查目标" prop="targetName" min-width="120" show-overflow-tooltip sortable />

      <el-table-column label="检查项" prop="itemName" min-width="140" show-overflow-tooltip sortable />

      <el-table-column label="发生次数" prop="occurrences" width="100" align="center" sortable>
        <template #default="{ row }">
          <el-tag
            :type="frequencyTagType(row.occurrences)"
            size="small"
            effect="plain"
          >
            {{ row.occurrences }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="最近发生" prop="lastOccurrence" width="120" align="center" sortable>
        <template #default="{ row }">
          <span class="date-text">{{ formatDate(row.lastOccurrence) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="频率等级" width="90" align="center">
        <template #default="{ row }">
          <div class="frequency-indicator">
            <span
              v-for="i in 5"
              :key="i"
              class="freq-dot"
              :class="{ active: i <= frequencyLevel(row.occurrences) }"
            />
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!data.length" class="empty-hint">
      <el-empty description="暂无重复违规数据" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
export interface RepeatOffenderItem {
  targetName: string
  itemName: string
  occurrences: number
  lastOccurrence: string
}

const props = withDefaults(defineProps<{
  data: RepeatOffenderItem[]
  maxHeight?: number
}>(), {
  maxHeight: 400,
})

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  // Handle ISO date strings
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return dateStr
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${m}-${d}`
}

function frequencyTagType(occurrences: number): string {
  if (occurrences >= 10) return 'danger'
  if (occurrences >= 5) return 'warning'
  if (occurrences >= 3) return 'primary'
  return 'info'
}

function frequencyLevel(occurrences: number): number {
  if (occurrences >= 10) return 5
  if (occurrences >= 7) return 4
  if (occurrences >= 5) return 3
  if (occurrences >= 3) return 2
  return 1
}
</script>

<style scoped>
.repeat-offender-table {
  width: 100%;
}

.date-text {
  font-size: 12px;
  color: #606266;
}

.frequency-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
}

.freq-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #DCDFE6;
  transition: background 0.2s;
}

.freq-dot.active {
  background: #F56C6C;
}

.empty-hint {
  padding: 20px 0;
}
</style>
