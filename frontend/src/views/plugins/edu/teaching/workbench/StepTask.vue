<template>
  <TaskFulfillmentTab :semester-id="semesterId" :selected-org="selectedOrg" />
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import TaskFulfillmentTab from '../offering/TaskFulfillmentTab.vue'

defineProps<{
  semesterId: LongId | string | undefined
  selectedOrg?: { type: string; id: LongId | string; name: string; classIds?: (number | string)[] }
}>()
</script>
