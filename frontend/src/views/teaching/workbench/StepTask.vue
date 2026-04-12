<template>
  <TaskFulfillmentTab :semester-id="semesterId" :selected-org="selectedOrg" />
</template>

<script setup lang="ts">
import TaskFulfillmentTab from '../offering/TaskFulfillmentTab.vue'

defineProps<{
  semesterId: number | string | undefined
  selectedOrg?: { type: string; id: number | string; name: string; classIds?: (number | string)[] }
}>()
</script>
