<template>
  <div class="empty-state">
    <el-empty :description="description || '暂无数据'">
      <slot />
    </el-empty>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  description?: string
}>()
</script>

<style scoped>
.empty-state {
  padding: 40px 0;
}
</style>
