<template>
  <el-card class="form-card" shadow="hover">
    <template #header v-if="title">
      <div class="card-header">
        <span>{{ title }}</span>
      </div>
    </template>
    <slot />
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
}>()
</script>

<style scoped>
.form-card {
  border-radius: 8px;
}
.card-header {
  font-weight: 600;
}
</style>
