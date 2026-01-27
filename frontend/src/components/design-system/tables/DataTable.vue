<template>
  <el-table :data="data" v-bind="$attrs">
    <slot />
  </el-table>
</template>

<script setup lang="ts">
defineProps<{
  data: any[]
}>()
</script>
