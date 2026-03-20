<template>
  <div class="dt">
    <div class="dt-head">
      <span class="dt-title">子项权重</span>
      <span class="dt-hint">权重由分区设置同步，只读</span>
    </div>

    <div v-if="dimensions.length === 0" class="dt-empty">暂无子项</div>

    <div v-else class="dt-list">
      <div v-for="dim in dimensions" :key="dim.id" class="dt-row">
        <span class="dt-name">{{ dim.dimensionName }}</span>
        <span v-if="dim.sourceType === 'ITEM'" class="dt-tag">字段</span>
        <span class="dt-weight">{{ dim.weight }}%</span>
      </div>
    </div>

    <!-- Weight bar -->
    <div v-if="dimensions.length > 0" class="dt-bar-row">
      <div class="dt-bar-bg">
        <div class="dt-bar-fill"
          :class="totalWeight === 100 ? 'ok' : totalWeight > 100 ? 'over' : 'under'"
          :style="{ width: Math.min(totalWeight, 100) + '%' }" />
      </div>
      <span class="dt-bar-val" :class="totalWeight === 100 ? 'ok' : 'bad'">{{ totalWeight }}%</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ScoreDimension } from '@/types/insp/scoring'

const props = defineProps<{
  dimensions: ScoreDimension[]
  templateId: number
}>()

const totalWeight = computed(() => props.dimensions.reduce((s, d) => s + d.weight, 0))
</script>

<style scoped>
.dt { display: flex; flex-direction: column; gap: 6px; }
.dt-head { display: flex; align-items: baseline; gap: 6px; }
.dt-title { font-size: 11px; font-weight: 600; color: #374151; }
.dt-hint { font-size: 10px; color: #9ca3af; }
.dt-empty { font-size: 11px; color: #b8c0cc; padding: 6px 0; }

.dt-list { display: flex; flex-direction: column; gap: 2px; }
.dt-row { display: flex; align-items: center; gap: 6px; padding: 4px 6px; border-radius: 5px; border: 1px solid #f0f1f3; }
.dt-name { font-size: 12px; color: #1e2a3a; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dt-tag { font-size: 9px; color: #0891b2; background: #ecfeff; padding: 0 4px; border-radius: 3px; }
.dt-weight { font-size: 12px; font-weight: 600; color: #1a6dff; min-width: 36px; text-align: right; }

/* Weight bar */
.dt-bar-row { display: flex; align-items: center; gap: 6px; }
.dt-bar-bg { flex: 1; height: 3px; background: #f0f1f3; border-radius: 3px; overflow: hidden; }
.dt-bar-fill { height: 100%; border-radius: 3px; transition: all 0.3s; }
.dt-bar-fill.ok { background: #34d399; }
.dt-bar-fill.over { background: #ef4444; }
.dt-bar-fill.under { background: #eab308; }
.dt-bar-val { font-size: 11px; font-weight: 500; flex-shrink: 0; }
.dt-bar-val.ok { color: #059669; }
.dt-bar-val.bad { color: #ef4444; }
</style>
