<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { messageApi } from '@/api/message'
import type { MsgNotification } from '@/types/message'

const router = useRouter()

// ==================== State ====================
const loading = ref(false)
const loadingMore = ref(false)
const notifications = ref<MsgNotification[]>([])
const total = ref(0)
const keyword = ref('')
const activeTab = ref<'ALL' | 'UNREAD' | 'READ'>('ALL')
const page = ref(1)
const PAGE_SIZE = 20

// ==================== Computed ====================
const unreadCount = computed(() => notifications.value.filter(n => !n.isRead).length)
const readCount = computed(() => notifications.value.filter(n => n.isRead).length)
const hasMore = computed(() => notifications.value.length < total.value)

const filteredNotifications = computed(() => {
  let list = notifications.value
  if (activeTab.value === 'UNREAD') list = list.filter(n => !n.isRead)
  else if (activeTab.value === 'READ') list = list.filter(n => n.isRead)
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(n =>
      n.title.toLowerCase().includes(kw) ||
      (n.content && n.content.toLowerCase().includes(kw))
    )
  }
  return list
})

// 按时间分组
interface Group {
  label: string
  items: MsgNotification[]
}

const groupedNotifications = computed((): Group[] => {
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterdayStart = new Date(todayStart.getTime() - 86400000)

  const today: MsgNotification[] = []
  const yesterday: MsgNotification[] = []
  const earlier: MsgNotification[] = []

  for (const n of filteredNotifications.value) {
    const d = new Date(n.createdAt)
    if (d >= todayStart) today.push(n)
    else if (d >= yesterdayStart) yesterday.push(n)
    else earlier.push(n)
  }

  const groups: Group[] = []
  if (today.length) groups.push({ label: '今天', items: today })
  if (yesterday.length) groups.push({ label: '昨天', items: yesterday })
  if (earlier.length) groups.push({ label: '更早', items: earlier })
  return groups
})

// ==================== Load ====================
async function loadNotifications(reset = false) {
  if (reset) {
    page.value = 1
    notifications.value = []
  }
  if (reset) loading.value = true
  else loadingMore.value = true

  try {
    const res = await messageApi.getNotifications({
      page: page.value,
      size: PAGE_SIZE,
    })
    if (reset) {
      notifications.value = res.records ?? []
    } else {
      notifications.value.push(...(res.records ?? []))
    }
    total.value = res.total ?? 0
  } catch {
    // silent
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadMore() {
  page.value++
  await loadNotifications(false)
}

// ==================== Actions ====================
async function handleMarkRead(n: MsgNotification) {
  if (n.isRead) return
  try {
    await messageApi.markRead(n.id)
    n.isRead = true
    n.readAt = new Date().toISOString()
  } catch { /* silent */ }
}

async function handleMarkAllRead() {
  try {
    await messageApi.markAllRead()
    notifications.value.forEach(n => { n.isRead = true; n.readAt = new Date().toISOString() })
  } catch { /* silent */ }
}

function handleSearch() {
  // filter is computed, no need to reload
}

function goConfig() {
  router.push('/messages/config')
}

// ==================== Helpers ====================
function formatTime(t: string): string {
  const d = new Date(t)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  if (d >= todayStart) return '今天'
  const yesterdayStart = new Date(todayStart.getTime() - 86400000)
  if (d >= yesterdayStart) return '昨天'
  return `${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getDate().toString().padStart(2, '0')}`
}

// ==================== Polling ====================
let pollTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  loadNotifications(true)
  // 每30秒刷新未读角标（通过重新加载）
  pollTimer = setInterval(() => loadNotifications(true), 30000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
  <div class="msg-view">
    <!-- Header -->
    <div class="msg-header">
      <div class="msg-header-left">
        <svg class="msg-header-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        <h1 class="msg-title">消息中心</h1>
      </div>
      <div class="msg-header-right">
        <button v-if="unreadCount > 0" class="btn-text" @click="handleMarkAllRead">全部已读</button>
        <button class="btn-icon-only" title="消息配置" @click="goConfig">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Stats bar -->
    <div class="msg-stats">
      <button class="stat-item" :class="{ active: activeTab === 'ALL' }" @click="activeTab = 'ALL'">
        <span class="stat-label">全部</span>
        <span class="stat-value">{{ total }}</span>
      </button>
      <span class="stat-sep">│</span>
      <button class="stat-item" :class="{ active: activeTab === 'UNREAD' }" @click="activeTab = 'UNREAD'">
        <span class="stat-label">未读</span>
        <span class="stat-value unread">{{ unreadCount }}</span>
      </button>
      <span class="stat-sep">│</span>
      <button class="stat-item" :class="{ active: activeTab === 'READ' }" @click="activeTab = 'READ'">
        <span class="stat-label">已读</span>
        <span class="stat-value">{{ readCount }}</span>
      </button>

      <!-- Search -->
      <div class="search-wrap">
        <svg class="search-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" width="13" height="13">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索消息..."
          class="search-input"
          @input="handleSearch"
        />
      </div>
    </div>

    <!-- Content -->
    <div class="msg-content">
      <!-- Loading -->
      <div v-if="loading" class="state-loading">
        <div class="spinner" />
        <span>加载中...</span>
      </div>

      <!-- Empty -->
      <div v-else-if="groupedNotifications.length === 0" class="state-empty">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="48" height="48" class="empty-icon">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        <p class="empty-title">暂无消息</p>
        <p class="empty-sub">{{ keyword ? '未找到匹配的消息' : '当前没有任何消息通知' }}</p>
      </div>

      <!-- Grouped list -->
      <template v-else>
        <div v-for="group in groupedNotifications" :key="group.label" class="msg-group">
          <div class="group-label">{{ group.label }}</div>
          <div
            v-for="n in group.items"
            :key="n.id"
            class="msg-item"
            :class="{ unread: !n.isRead }"
            @click="handleMarkRead(n)"
          >
            <span class="read-dot" :class="{ unread: !n.isRead }" />
            <div class="msg-item-body">
              <div class="msg-item-head">
                <span class="msg-item-title">{{ n.title }}</span>
                <span class="msg-item-time">{{ formatTime(n.createdAt) }}</span>
              </div>
              <div v-if="n.content" class="msg-item-content">{{ n.content }}</div>
            </div>
          </div>
        </div>

        <!-- Load more -->
        <div v-if="hasMore" class="load-more-wrap">
          <button class="btn-load-more" :disabled="loadingMore" @click="loadMore">
            <template v-if="loadingMore">
              <div class="spinner-sm" /> 加载中...
            </template>
            <template v-else>加载更多</template>
          </button>
        </div>
        <div v-else class="no-more">已显示全部消息</div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.msg-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* Header */
.msg-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px 14px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
}
.msg-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.msg-header-icon {
  width: 18px;
  height: 18px;
  color: #1a6dff;
}
.msg-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0;
}
.msg-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Stats bar */
.msg-stats {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 10px 24px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
}
.stat-sep {
  color: #dce1e8;
  font-size: 14px;
  padding: 0 16px;
  user-select: none;
}
.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}
.stat-item:hover { background: #f4f6f9; }
.stat-item.active { background: #e8f0ff; }
.stat-label {
  font-size: 13px;
  color: #5a6474;
  font-weight: 500;
}
.stat-item.active .stat-label { color: #1a6dff; }
.stat-value {
  font-size: 13px;
  font-weight: 700;
  color: #1e2a3a;
}
.stat-value.unread { color: #ef4444; }
.stat-item.active .stat-value { color: #1a6dff; }

/* Search */
.search-wrap {
  position: relative;
  margin-left: auto;
}
.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #b8c0cc;
  pointer-events: none;
}
.search-input {
  height: 32px;
  width: 200px;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  padding: 0 12px 0 30px;
  font-size: 12px;
  outline: none;
  background: #fff;
  color: #3d4757;
  transition: border-color 0.2s, box-shadow 0.2s;
  font-family: inherit;
}
.search-input::placeholder { color: #b8c0cc; }
.search-input:focus {
  border-color: #7aadff;
  box-shadow: 0 0 0 3px rgba(26, 109, 255, 0.08);
}

/* Content */
.msg-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* Group */
.msg-group {
  margin-bottom: 16px;
}
.group-label {
  font-size: 11px;
  font-weight: 600;
  color: #b8c0cc;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0 0 8px 4px;
}

/* Message item */
.msg-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.msg-item:hover {
  border-color: #c8d4e3;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.msg-item.unread {
  border-left: 3px solid #1a6dff;
  background: #fafcff;
}

/* Read dot */
.read-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #dce1e8;
  flex-shrink: 0;
  margin-top: 4px;
}
.read-dot.unread {
  background: #1a6dff;
}

/* Item body */
.msg-item-body {
  flex: 1;
  min-width: 0;
}
.msg-item-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;
}
.msg-item-title {
  font-size: 13px;
  font-weight: 600;
  color: #1e2a3a;
  line-height: 1.4;
  flex: 1;
  min-width: 0;
}
.msg-item.unread .msg-item-title {
  color: #1a6dff;
}
.msg-item-time {
  font-size: 11px;
  color: #b8c0cc;
  flex-shrink: 0;
  white-space: nowrap;
}
.msg-item-content {
  font-size: 12px;
  color: #6b7685;
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* Load more */
.load-more-wrap {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}
.btn-load-more {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 24px;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #5a6474;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.btn-load-more:hover:not(:disabled) {
  background: #f4f6f9;
  border-color: #c8d0db;
}
.btn-load-more:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.no-more {
  text-align: center;
  font-size: 12px;
  color: #b8c0cc;
  padding: 12px 0;
}

/* States */
.state-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 80px 0;
  color: #b8c0cc;
  font-size: 13px;
}
.state-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  gap: 10px;
}
.empty-icon { color: #dce1e8; }
.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: #6b7685;
  margin: 0;
}
.empty-sub {
  font-size: 12px;
  color: #b8c0cc;
  margin: 0;
}

/* Spinner */
.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e8ecf0;
  border-top-color: #1a6dff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
.spinner-sm {
  width: 14px;
  height: 14px;
  border: 2px solid #e8ecf0;
  border-top-color: #5a6474;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Buttons */
.btn-text {
  padding: 5px 12px;
  background: none;
  border: 1px solid #dce1e8;
  border-radius: 5px;
  font-size: 12px;
  color: #5a6474;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.btn-text:hover {
  background: #f4f6f9;
  color: #1a6dff;
  border-color: #c8d0db;
}
.btn-icon-only {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: none;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  color: #6b7685;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.btn-icon-only:hover {
  background: #f4f6f9;
  color: #1a6dff;
}
</style>
