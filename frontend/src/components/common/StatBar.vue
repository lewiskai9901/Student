<template>
  <div class="tm-stat-bar">
    <div v-for="(it, i) in items" :key="i" class="tm-stat-item">
      <span class="tm-stat-label">{{ it.label }}</span>
      <span class="tm-stat-value">{{ it.value }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 紧凑文字统计条 (2026-06) — 替代彩色图标方块统计卡。
 * 纯文字标签+数值, 竖线分隔, 无彩色图标方块、无大留白。
 * 与 OrgStructure 统计条 / DashboardView divide-x 对齐。
 */
defineProps<{
  items: { label: string; value: string | number }[]
}>()
</script>

<style scoped>
.tm-stat-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 10px 4px;
}
.tm-stat-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
  padding: 0 16px;
  border-left: 1px solid #e5e7eb;
}
.tm-stat-item:first-child {
  border-left: none;
}
.tm-stat-label {
  font-size: 12px;
  color: #6b7280;
}
.tm-stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}
</style>
