<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getOrgScores } from '@/api/inspection/scoring'
import type { OrgScoreView } from '@/types/insp/scoring'

// Phase 3.5b: 项目详情 — 某周期下各组织的 roll-up 得分排名.
// 后端沿 tree_path 用均值汇总, 让"5 班 vs 20 班"的规模公平得分可见.
const props = defineProps<{
  projectId: LongId
}>()

function todayStr(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

const cycleDate = ref<string>(todayStr())
const loading = ref(false)
const loadError = ref<string | null>(null)
const rows = ref<OrgScoreView[]>([])

async function load() {
  if (!cycleDate.value) return
  loading.value = true
  loadError.value = null
  try {
    // 后端已按 score 降序, 前端直接渲染
    rows.value = await getOrgScores(props.projectId, cycleDate.value)
  } catch (e: any) {
    loadError.value = e?.message || '加载组织得分失败'
    rows.value = []
    ElMessage.error(loadError.value || '加载组织得分失败')
  } finally {
    loading.value = false
  }
}

watch(cycleDate, () => load(), { immediate: true })
</script>

<template>
  <div class="osr">
    <div class="osr-head">
      <span class="osr-title">组织得分排名</span>
      <span class="osr-sep">·</span>
      <span class="osr-hint">沿组织树用均值汇总, 规模公平</span>
      <div class="osr-head-right">
        <el-date-picker
          v-model="cycleDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择周期"
          size="small"
          :clearable="false"
          style="width: 140px"
        />
      </div>
    </div>

    <div v-if="loadError" class="osr-state osr-state--error">
      {{ loadError }}
      <button class="osr-retry" @click="load">重试</button>
    </div>
    <div v-else-if="loading" class="osr-state">加载中…</div>
    <div v-else-if="rows.length === 0" class="osr-state">
      该周期暂无组织得分（需先完成检查并算分）
    </div>

    <div v-else class="osr-list">
      <div v-for="(r, i) in rows" :key="r.orgUnitId" class="osr-row">
        <span class="osr-rank">{{ i + 1 }}</span>
        <span class="osr-name" :title="r.orgUnitName">{{ r.orgUnitName }}</span>
        <span class="osr-meta">
          <span v-if="r.childCount > 0">{{ r.childCount }} 子单位</span>
          <span v-if="r.childCount > 0 && r.sourceCount > 0" class="osr-meta-sep">·</span>
          <span v-if="r.sourceCount > 0">{{ r.sourceCount }} 样本均值</span>
        </span>
        <span v-if="r.grade" class="osr-grade">{{ r.grade }}</span>
        <span class="osr-score">{{ r.score.toFixed(1) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.osr {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px 14px;
  margin-bottom: 16px;
  background: #fff;
}
.osr-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  font-size: 13px;
}
.osr-title {
  font-weight: 600;
  color: #303133;
}
.osr-sep,
.osr-meta-sep {
  color: #c0c4cc;
}
.osr-hint {
  color: #909399;
  font-size: 12px;
}
.osr-head-right {
  margin-left: auto;
}
.osr-state {
  padding: 24px 0;
  text-align: center;
  color: #909399;
  font-size: 13px;
}
.osr-state--error {
  color: #f56c6c;
}
.osr-retry {
  margin-left: 8px;
  border: none;
  background: none;
  color: #409eff;
  cursor: pointer;
  font-size: 13px;
}
.osr-list {
  display: flex;
  flex-direction: column;
}
.osr-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 0;
  font-size: 13px;
  border-bottom: 1px solid #f5f7fa;
}
.osr-row:last-child {
  border-bottom: none;
}
.osr-rank {
  width: 22px;
  text-align: right;
  color: #909399;
  font-variant-numeric: tabular-nums;
}
.osr-name {
  flex: 1;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.osr-meta {
  color: #909399;
  font-size: 12px;
}
.osr-grade {
  color: #606266;
  min-width: 28px;
  text-align: center;
}
.osr-score {
  font-weight: 600;
  color: #303133;
  font-variant-numeric: tabular-nums;
  min-width: 48px;
  text-align: right;
}
</style>
