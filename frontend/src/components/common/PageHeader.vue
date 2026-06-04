<template>
  <div class="tm-page-header">
    <div class="tm-ph-titles">
      <h1 class="tm-ph-title">{{ title }}</h1>
      <span v-if="subtitle" class="tm-ph-subtitle">{{ subtitle }}</span>
    </div>
    <div v-if="$slots.actions" class="tm-ph-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 紧凑页头 (2026-06) — 替代各页彩色渐变横幅。
 * 纯标题 (15px) + 可选副标题 + 右侧 actions 插槽, 无渐变、无大留白。
 * 与 OrgStructure / DashboardView 的紧凑页头对齐。
 */
defineProps<{
  title: string
  subtitle?: string
}>()
</script>

<style scoped>
.tm-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 2px 0 12px;
}
.tm-ph-titles {
  display: flex;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
}
.tm-ph-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
  line-height: 1.4;
}
.tm-ph-subtitle {
  font-size: 12px;
  color: #9ca3af;
}
.tm-ph-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
</style>
