<template>
  <div class="analytics-tab">
    <div class="placeholder-content">
      <BarChart3 :size="64" class="placeholder-icon" />
      <h3>数据分析</h3>
      <p>该功能正在开发中，敬请期待...</p>
      <p class="hint">将提供班级量化数据的多维度分析，包括趋势分析、对比分析等</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { BarChart3 } from 'lucide-vue-next'

defineProps<{
  orgUnitId: string | number
}>()
</script>

<style lang="scss" scoped>
.analytics-tab {
  padding: 0;
}

.placeholder-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background: white;
  border-radius: 12px;
  padding: 40px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #ebeef5;

  .placeholder-icon {
    color: #c0c4cc;
    margin-bottom: 16px;
  }

  h3 {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 8px;
  }

  p {
    font-size: 14px;
    color: #909399;
    margin: 0 0 8px;
  }

  .hint {
    font-size: 12px;
    color: #c0c4cc;
  }
}
</style>
