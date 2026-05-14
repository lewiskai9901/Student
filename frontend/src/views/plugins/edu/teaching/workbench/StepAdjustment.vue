<template>
  <div style="padding: 20px 24px;">
    <AdjustmentPanel :semester-id="semesterId" />
  </div>
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import AdjustmentPanel from '../schedule/AdjustmentPanel.vue'

defineProps<{
  semesterId: LongId | string | undefined
}>()
</script>
