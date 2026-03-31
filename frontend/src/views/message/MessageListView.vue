<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { messageApi } from '@/api/message'
import type { MsgNotification } from '@/types/message'
import { MsgTypeConfig } from '@/types/message'

const router = useRouter()

// ==================== State ====================
const loading = ref(false)
const loadingMore = ref(false)
const notifications = ref<MsgNotification[]>([])
const total = ref(0)
const unreadTotal = ref(0)
const keyword = ref('')
const activeTab = ref<'ALL' | 'UNREAD'>('ALL')
const activeMsgType = ref<string | null>(null)
const page = ref(1)
const PAGE_SIZE = 20

// Detail drawer
const detailVisible = ref(false)
const detailMessage = ref<MsgNotification | null>(null)

// Delete confirm
const deleteConfirmVisible = ref(false)
const deleteTargetId = ref<number | null>(null)

// ==================== Computed ====================
const hasMore = computed(() => notifications.value.length < total.value)

const displayedNotifications = computed(() => {
  let list = notifications.value
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(n =>
      n.title.toLowerCase().includes(kw) ||
      (n.content && n.content.toLowerCase().includes(kw))
    )
  }
  return list
})

// Group by time
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

  for (const n of displayedNotifications.value) {
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
    const params: Record<string, any> = {
      page: page.value,
      size: PAGE_SIZE,
    }
    if (activeTab.value === 'UNREAD') {
      params.isRead = false
    }
    if (activeMsgType.value) {
      params.msgType = activeMsgType.value
    }
    const res = await messageApi.getNotifications(params)
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

async function fetchUnreadCount() {
  try {
    const res = await messageApi.getUnreadCount()
    unreadTotal.value = res?.count ?? 0
  } catch { /* silent */ }
}

async function loadMore() {
  page.value++
  await loadNotifications(false)
}

// Watch tab/type changes to reload
watch([activeTab, activeMsgType], () => {
  loadNotifications(true)
})

// ==================== Actions ====================
function openDetail(n: MsgNotification) {
  detailMessage.value = n
  detailVisible.value = true
  if (!n.isRead) {
    handleMarkRead(n)
  }
}

async function handleMarkRead(n: MsgNotification) {
  if (n.isRead) return
  try {
    await messageApi.markRead(n.id)
    n.isRead = true
    n.readAt = new Date().toISOString()
    unreadTotal.value = Math.max(0, unreadTotal.value - 1)
  } catch { /* silent */ }
}

async function handleMarkAllRead() {
  try {
    await messageApi.markAllRead()
    notifications.value.forEach(n => { n.isRead = true; n.readAt = new Date().toISOString() })
    unreadTotal.value = 0
  } catch { /* silent */ }
}

function confirmDelete(id: number) {
  deleteTargetId.value = id
  deleteConfirmVisible.value = true
}

async function executeDelete() {
  if (!deleteTargetId.value) return
  try {
    const targetId = deleteTargetId.value
    await messageApi.delete(targetId)
    const removed = notifications.value.find(n => n.id === targetId)
    notifications.value = notifications.value.filter(n => n.id !== targetId)
    total.value = Math.max(0, total.value - 1)
    if (removed && !removed.isRead) {
      unreadTotal.value = Math.max(0, unreadTotal.value - 1)
    }
    if (detailMessage.value?.id === targetId) {
      detailVisible.value = false
      detailMessage.value = null
    }
  } catch { /* silent */ } finally {
    deleteConfirmVisible.value = false
    deleteTargetId.value = null
  }
}

function goConfig() {
  router.push('/messages/config')
}

function getMsgTypeLabel(type: string): string {
  return MsgTypeConfig[type as keyof typeof MsgTypeConfig]?.label ?? type
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

function formatFullTime(t: string): string {
  const d = new Date(t)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// Mask helpers
const maskMouseDownTarget = ref<EventTarget | null>(null)
function onMaskMouseDown(e: MouseEvent) { maskMouseDownTarget.value = e.target }
function onMaskClick(e: MouseEvent, closeFn: () => void) {
  if (e.target === e.currentTarget && maskMouseDownTarget.value === e.currentTarget) closeFn()
  maskMouseDownTarget.value = null
}

// ==================== Polling ====================
let pollTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  loadNotifications(true)
  fetchUnreadCount()
  pollTimer = setInterval(() => {
    fetchUnreadCount()
  }, 30000)
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
        <button v-if="unreadTotal > 0" class="btn-text" @click="handleMarkAllRead">全部已读</button>
        <button class="btn-icon-only" title="消息配置" @click="goConfig">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Stats bar + filters -->
    <div class="msg-stats">
      <button class="stat-item" :class="{ active: activeTab === 'ALL' }" @click="activeTab = 'ALL'">
        <span class="stat-label">全部</span>
        <span class="stat-value">{{ total }}</span>
      </button>
      <span class="stat-sep">|</span>
      <button class="stat-item" :class="{ active: activeTab === 'UNREAD' }" @click="activeTab = 'UNREAD'">
        <span class="stat-label">未读</span>
        <span class="stat-value unread">{{ unreadTotal }}</span>
      </button>

      <!-- msgType filters -->
      <span class="stat-sep">|</span>
      <button
        class="stat-item"
        :class="{ active: activeMsgType === null }"
        @click="activeMsgType = null"
      >
        <span class="stat-label">全类型</span>
      </button>
      <button
        v-for="(cfg, key) in MsgTypeConfig"
        :key="key"
        class="stat-item"
        :class="{ active: activeMsgType === key }"
        @click="activeMsgType = key"
      >
        <span class="stat-label">{{ cfg.label }}</span>
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
            @click="openDetail(n)"
          >
            <span class="read-dot" :class="{ unread: !n.isRead }" />
            <div class="msg-item-body">
              <div class="msg-item-head">
                <span class="msg-item-title">{{ n.title }}</span>
                <div class="msg-item-right">
                  <span class="msg-type-tag">{{ getMsgTypeLabel(n.msgType) }}</span>
                  <span class="msg-item-time">{{ formatTime(n.createdAt) }}</span>
                </div>
              </div>
              <div v-if="n.content" class="msg-item-content">{{ n.content }}</div>
            </div>
            <button
              class="msg-item-delete"
              title="删除"
              @click.stop="confirmDelete(n.id)"
            >
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="14" height="14">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
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

    <!-- ==================== Detail Drawer ==================== -->
    <Teleport to="body">
      <Transition name="drawer">
        <div v-if="detailVisible" class="drawer-mask" @click="detailVisible = false">
          <div class="drawer-panel" @click.stop>
            <div class="drawer-head">
              <h3>消息详情</h3>
              <button class="drawer-close" @click="detailVisible = false">&times;</button>
            </div>
            <div v-if="detailMessage" class="drawer-body">
              <div class="detail-title">{{ detailMessage.title }}</div>
              <div class="detail-meta">
                <span class="detail-type-tag">{{ getMsgTypeLabel(detailMessage.msgType) }}</span>
                <span class="detail-time">{{ formatFullTime(detailMessage.createdAt) }}</span>
                <span v-if="detailMessage.isRead && detailMessage.readAt" class="detail-read">
                  已读于 {{ formatFullTime(detailMessage.readAt) }}
                </span>
              </div>
              <div v-if="detailMessage.sourceEventType" class="detail-source">
                来源: {{ detailMessage.sourceEventType }}
              </div>
              <div class="detail-content">
                {{ detailMessage.content || '（无正文内容）' }}
              </div>
              <div class="detail-actions">
                <button class="btn-danger-sm" @click="confirmDelete(detailMessage.id)">删除此消息</button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ==================== Delete Confirm ==================== -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="deleteConfirmVisible"
          class="modal-mask"
          @mousedown="onMaskMouseDown"
          @click="onMaskClick($event, () => deleteConfirmVisible = false)"
        >
          <div class="modal-box modal-box-sm">
            <div class="modal-head">
              <h3>确认删除</h3>
              <button class="modal-close" @click="deleteConfirmVisible = false">&times;</button>
            </div>
            <div class="modal-body">
              <p class="confirm-text">确认删除此消息？删除后不可恢复。</p>
            </div>
            <div class="modal-foot">
              <button class="btn-ghost" @click="deleteConfirmVisible = false">取消</button>
              <button class="btn-danger" @click="executeDelete">删除</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
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
  flex-wrap: wrap;
}
.stat-sep {
  color: #dce1e8;
  font-size: 14px;
  padding: 0 12px;
  user-select: none;
}
.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}
.stat-item:hover { background: #f4f6f9; }
.stat-item.active { background: #e8f0ff; }
.stat-label {
  font-size: 12px;
  color: #5a6474;
  font-weight: 500;
}
.stat-item.active .stat-label { color: #1a6dff; }
.stat-value {
  font-size: 12px;
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
.msg-item-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.msg-type-tag {
  font-size: 10px;
  font-weight: 500;
  padding: 1px 6px;
  border-radius: 3px;
  background: #f0f2f5;
  color: #6b7685;
  white-space: nowrap;
}
.msg-item-time {
  font-size: 11px;
  color: #b8c0cc;
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

/* Delete button on item */
.msg-item-delete {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: none;
  border: none;
  border-radius: 4px;
  color: #b8c0cc;
  cursor: pointer;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.15s, color 0.15s, background 0.15s;
}
.msg-item:hover .msg-item-delete {
  opacity: 1;
}
.msg-item-delete:hover {
  color: #d93025;
  background: #fef2f2;
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

/* ==================== Drawer ==================== */
.drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(15, 23, 42, 0.3);
  display: flex;
  justify-content: flex-end;
}
.drawer-panel {
  width: 440px;
  max-width: 90vw;
  background: #fff;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.12);
}
.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
}
.drawer-head h3 {
  font-size: 15px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0;
}
.drawer-close {
  background: none;
  border: none;
  font-size: 22px;
  color: #b8c0cc;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
}
.drawer-close:hover { color: #5a6474; }
.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e2a3a;
  line-height: 1.4;
}
.detail-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.detail-type-tag {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
  background: #e8f0ff;
  color: #2563eb;
}
.detail-time {
  font-size: 12px;
  color: #6b7685;
}
.detail-read {
  font-size: 11px;
  color: #10b981;
}
.detail-source {
  font-size: 12px;
  color: #8c95a3;
  padding: 8px 12px;
  background: #f8f9fb;
  border-radius: 6px;
}
.detail-content {
  font-size: 13px;
  color: #3d4757;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.detail-actions {
  padding-top: 16px;
  border-top: 1px solid #e8ecf0;
}
.btn-danger-sm {
  padding: 6px 16px;
  background: none;
  border: 1px solid #fecaca;
  border-radius: 6px;
  font-size: 12px;
  color: #d93025;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.btn-danger-sm:hover {
  background: #fef2f2;
  color: #b91c1c;
}

/* Drawer transitions */
.drawer-enter-active { transition: all 0.25s ease-out; }
.drawer-leave-active { transition: all 0.2s ease-in; }
.drawer-enter-from { opacity: 0; }
.drawer-enter-from .drawer-panel { transform: translateX(100%); }
.drawer-leave-to { opacity: 0; }
.drawer-leave-to .drawer-panel { transform: translateX(100%); }

/* ==================== Modal (Delete confirm) ==================== */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 1100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(2px);
}
.modal-box {
  width: 380px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}
.modal-box-sm { width: 360px; }
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}
.modal-head h3 {
  font-size: 15px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0;
}
.modal-close {
  background: none;
  border: none;
  font-size: 22px;
  color: #b8c0cc;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
}
.modal-close:hover { color: #5a6474; }
.modal-body {
  padding: 18px 24px;
}
.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 24px 20px;
}
.confirm-text {
  font-size: 13px;
  color: #3d4757;
  margin: 0;
  line-height: 1.6;
}
.btn-ghost {
  padding: 8px 20px;
  background: none;
  border: 1px solid #dce1e8;
  border-radius: 8px;
  font-size: 13px;
  color: #5a6474;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-ghost:hover { background: #f4f6f9; }
.btn-danger {
  padding: 8px 20px;
  background: #ef4444;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-danger:hover { background: #dc2626; }

/* Modal transitions */
.modal-enter-active { transition: all 0.2s ease-out; }
.modal-leave-active { transition: all 0.15s ease-in; }
.modal-enter-from { opacity: 0; }
.modal-enter-from .modal-box { transform: translateY(12px) scale(0.97); }
.modal-leave-to { opacity: 0; }
.modal-leave-to .modal-box { transform: translateY(-8px) scale(0.98); }
</style>
