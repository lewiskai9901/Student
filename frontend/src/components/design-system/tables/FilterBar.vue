<template>
  <div class="filter-bar">
    <slot />
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
</style>
