<template>
  <OfferingListTab :semester-id="semesterId ?? undefined" :selected-org="selectedOrg" />
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import OfferingListTab from '../offering/OfferingListTab.vue'

defineProps<{
  semesterId: LongId | string | undefined
  selectedOrg?: any
}>()
</script>
