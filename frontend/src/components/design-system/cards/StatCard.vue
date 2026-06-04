<template>
  <div class="stat-card-compact">
    <div class="scc-value">{{ value }}</div>
    <div class="scc-title">{{ title }}</div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

/**
 * 紧凑统计卡 (2026-06 改造)。
 *
 * 原实现是 48px 彩色圆角图标方块 + 28px 大数值 + subtitle + p-20 大留白 + hover 上浮,
 * 违反紧凑风格偏好 (彩色图标方块 / 大留白 / 冗余 subtitle)。现改为纯 标题+数值 紧凑卡,
 * 与 OrgOverview 紧凑卡对齐。保留 color/icon/subtitle props 仅为兼容既有调用方, 不再渲染。
 */
defineProps<{
  title: string
  value: string | number
  subtitle?: string
  color?: 'blue' | 'green' | 'orange' | 'red' | 'purple' | 'cyan' | 'emerald' | 'gray'
  icon?: Component
}>()
</script>

<style scoped>
.stat-card-compact {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 12px 14px;
}
.scc-value {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.2;
}
.scc-title {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}
</style>
