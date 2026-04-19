import { defineStore } from 'pinia'
import { ref } from 'vue'
import { messageApi } from '@/api/message'

export const useMessageStore = defineStore('message', () => {
  const unreadCount = ref(0)
  let pollTimer: ReturnType<typeof setInterval> | null = null

  async function fetchUnreadCount() {
    try {
      const res = await messageApi.getUnreadCount()
      unreadCount.value = res?.count ?? 0
    } catch {
      // silent
    }
  }

  function decrementUnread(n = 1) {
    unreadCount.value = Math.max(0, unreadCount.value - n)
  }

  function clearUnread() {
    unreadCount.value = 0
  }

  function startPolling(intervalMs = 30000) {
    if (pollTimer) return
    fetchUnreadCount()
    pollTimer = setInterval(fetchUnreadCount, intervalMs)
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  return {
    unreadCount,
    fetchUnreadCount,
    decrementUnread,
    clearUnread,
    startPolling,
    stopPolling,
  }
})
