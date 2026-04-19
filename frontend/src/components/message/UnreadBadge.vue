<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useMessageStore } from '@/stores/message'

const store = useMessageStore()
const { unreadCount } = storeToRefs(store)

const displayCount = computed(() => unreadCount.value > 99 ? '99+' : unreadCount.value)

onMounted(() => {
  store.startPolling(30000)
})
</script>

<template>
  <span v-if="unreadCount > 0" class="unread-badge">
    {{ displayCount }}
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
