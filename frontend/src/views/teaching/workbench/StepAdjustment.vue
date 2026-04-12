<template>
  <div style="padding: 20px 24px;">
    <AdjustmentPanel :semester-id="semesterId" />
  </div>
</template>

<script setup lang="ts">
import AdjustmentPanel from '../schedule/AdjustmentPanel.vue'

defineProps<{
  semesterId: number | string | undefined
}>()
</script>
