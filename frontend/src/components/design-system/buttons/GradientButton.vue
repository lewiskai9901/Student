<template>
  <el-button :type="type || 'primary'" v-bind="$attrs">
    <slot />
  </el-button>
</template>

<script setup lang="ts">
defineProps<{
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
}>()
</script>
