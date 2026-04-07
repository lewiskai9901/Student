<template>
  <div>
    <h3 class="setup-step-title">约束规则配置</h3>
    <p class="setup-step-desc">配置排课约束（如教师每天不超过6节、某些时段禁排等）。此步骤可跳过。</p>
    <ConstraintConfig :semester-id="semesterId" />
  </div>
</template>

<script setup lang="ts">
import ConstraintConfig from '../ConstraintConfig.vue'
defineProps<{ semesterId: number | string | undefined }>()
</script>

<style scoped>
.setup-step-title { font-size: 16px; font-weight: 700; color: #111827; margin: 0 0 6px; }
.setup-step-desc { font-size: 13px; color: #6b7280; margin: 0 0 20px; }
</style>
