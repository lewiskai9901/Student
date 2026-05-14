<template>
  <div style="padding: 20px 24px;">
    <ConstraintConfig :semester-id="semesterId" />
  </div>
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import ConstraintConfig from '../schedule/ConstraintConfig.vue'

defineProps<{
  semesterId: LongId | string | undefined
}>()
</script>
