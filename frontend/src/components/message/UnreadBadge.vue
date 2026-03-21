<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { messageApi } from '@/api/message'

const count = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

async function fetchCount() {
  try {
    const res = await messageApi.getUnreadCount()
    count.value = res?.count ?? 0
  } catch {
    // silent
  }
}

onMounted(() => {
  fetchCount()
  timer = setInterval(fetchCount, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

defineExpose({ refresh: fetchCount })
</script>

<template>
  <span v-if="count > 0" class="unread-badge">
    {{ count > 99 ? '99+' : count }}
  </span>
</template>

<style scoped>
.unread-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  border-radius: 9px;
  line-height: 1;
  border: 2px solid #fff;
  box-sizing: border-box;
}
</style>
