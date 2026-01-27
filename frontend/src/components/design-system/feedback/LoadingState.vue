<template>
  <div class="loading-state" v-loading="true" :element-loading-text="text || '加载中...'">
    <div style="height: 200px;"></div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  text?: string
}>()
</script>

<style scoped>
.loading-state {
  min-height: 200px;
}
</style>
