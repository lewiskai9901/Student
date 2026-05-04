<template>
  <OfferingListTab :semester-id="semesterId ?? null" :selected-org="selectedOrg" />
</template>

<script setup lang="ts">
import OfferingListTab from '../offering/OfferingListTab.vue'

defineProps<{
  semesterId: number | string | undefined
  selectedOrg?: any
}>()
</script>
