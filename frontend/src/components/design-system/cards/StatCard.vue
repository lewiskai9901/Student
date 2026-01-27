<template>
  <el-card class="stat-card" shadow="hover" :class="cardClass">
    <div class="stat-content">
      <div class="stat-icon" :class="iconClass">
        <slot name="icon">
          <el-icon :size="24">
            <component :is="icon" v-if="icon" />
          </el-icon>
        </slot>
      </div>
      <div class="stat-info">
        <div class="stat-value">{{ value }}</div>
        <div class="stat-title">{{ title }}</div>
        <div v-if="subtitle" class="stat-subtitle">{{ subtitle }}</div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

const props = defineProps<{
  title: string
  value: string | number
  subtitle?: string
  color?: 'blue' | 'green' | 'orange' | 'red' | 'purple' | 'cyan' | 'emerald' | 'gray'
  icon?: Component
}>()

// Color mappings
const colorMap: Record<string, { bg: string; text: string; iconBg: string }> = {
  blue: { bg: 'bg-blue-50', text: 'text-blue-600', iconBg: 'bg-blue-100' },
  green: { bg: 'bg-green-50', text: 'text-green-600', iconBg: 'bg-green-100' },
  emerald: { bg: 'bg-emerald-50', text: 'text-emerald-600', iconBg: 'bg-emerald-100' },
  orange: { bg: 'bg-orange-50', text: 'text-orange-600', iconBg: 'bg-orange-100' },
  red: { bg: 'bg-red-50', text: 'text-red-600', iconBg: 'bg-red-100' },
  purple: { bg: 'bg-purple-50', text: 'text-purple-600', iconBg: 'bg-purple-100' },
  cyan: { bg: 'bg-cyan-50', text: 'text-cyan-600', iconBg: 'bg-cyan-100' },
  gray: { bg: 'bg-gray-50', text: 'text-gray-600', iconBg: 'bg-gray-100' }
}

const cardClass = props.color ? colorMap[props.color]?.bg || '' : ''
const iconClass = props.color ? `${colorMap[props.color]?.iconBg || ''} ${colorMap[props.color]?.text || ''}` : 'bg-blue-100 text-blue-600'
</script>

<style scoped>
.stat-card {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}
.stat-card :deep(.el-card__body) {
  padding: 20px;
}
.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-info {
  flex: 1;
  min-width: 0;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.2;
}
.stat-title {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-top: 4px;
}
.stat-subtitle {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}
</style>
