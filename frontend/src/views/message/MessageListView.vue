<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { messageApi } from '@/api/message'
import { useMessageStore } from '@/stores/message'
import type { MsgNotification } from '@/types/message'
import { MsgTypeConfig } from '@/types/message'

const router = useRouter()
const messageStore = useMessageStore()
const { unreadCount: unreadTotal } = storeToRefs(messageStore)

// ==================== State ====================
const loading = ref(false)
const loadingMore = ref(false)
const notifications = ref<MsgNotification[]>([])
const total = ref(0)
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
    messageStore.decrementUnread(1)
  } catch { /* silent */ }
}

async function handleMarkAllRead() {
  try {
    await messageApi.markAllRead()
    notifications.value.forEach(n => { n.isRead = true; n.readAt = new Date().toISOString() })
    messageStore.clearUnread()
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
      messageStore.decrementUnread(1)
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

onMounted(() => {
  loadNotifications(true)
  messageStore.fetchUnreadCount()
})
</script>

<template>
  <div class="msg-page">
    <!-- Header -->
    <header class="msg-header">
      <div class="header-left">
        <h1 class="page-title">消息中心</h1>
        <div class="stats-row">
          <span class="stat">全部 <b>{{ total }}</b></span>
          <i class="stat-sep" />
          <span class="stat stat-unread">
            <span class="dot dot-unread" />未读 <b>{{ unreadTotal }}</b>
          </span>
        </div>
      </div>
      <div class="header-right">
        <button v-if="unreadTotal > 0" class="btn-ghost" @click="handleMarkAllRead">全部已读</button>
        <button class="btn-create" @click="goConfig">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
            <path d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" stroke="currentColor" stroke-width="1.8"/>
            <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" stroke="currentColor" stroke-width="1.8"/>
          </svg>
          消息配置
        </button>
      </div>
    </header>

    <!-- Tab Bar -->
    <div class="tab-bar">
      <button class="tab-btn" :class="{ active: activeTab === 'ALL' }" @click="activeTab = 'ALL'">
        全部
        <span class="tab-count">{{ total }}</span>
      </button>
      <button class="tab-btn" :class="{ active: activeTab === 'UNREAD' }" @click="activeTab = 'UNREAD'">
        未读
        <span class="tab-count">{{ unreadTotal }}</span>
      </button>
    </div>

    <!-- Filter Bar -->
    <div class="filter-bar">
      <div class="search-box">
        <svg class="search-icon" width="14" height="14" viewBox="0 0 24 24" fill="none">
          <path d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <input v-model="keyword" class="search-input" type="text" placeholder="搜索消息标题或内容..." />
      </div>
      <select v-model="activeMsgType" class="filter-select">
        <option :value="null">全部类型</option>
        <option v-for="(cfg, key) in MsgTypeConfig" :key="key" :value="key">{{ cfg.label }}</option>
      </select>
      <button v-if="keyword || activeMsgType" class="btn-reset" @click="keyword = ''; activeMsgType = null">
        重置
      </button>
    </div>

    <!-- List -->
    <div class="list-container">
      <div v-if="loading" class="state state-loading">
        <div class="spinner" />
        <span>加载中...</span>
      </div>

      <div v-else-if="displayedNotifications.length === 0" class="state state-empty">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" class="empty-icon">
          <path d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <p class="empty-text">{{ keyword ? '未找到匹配的消息' : '暂无消息' }}</p>
      </div>

      <div v-else class="msg-list">
        <div
          v-for="n in displayedNotifications"
          :key="n.id"
          class="msg-row"
          :class="{ 'is-unread': !n.isRead }"
          @click="openDetail(n)"
        >
          <span class="dot" :class="n.isRead ? 'dot-read' : 'dot-unread'" />
          <div class="msg-main">
            <div class="msg-title-line">
              <span class="msg-title" :class="{ 'msg-title-unread': !n.isRead }">{{ n.title }}</span>
              <code class="code-badge">{{ getMsgTypeLabel(n.msgType) }}</code>
            </div>
            <div v-if="n.content" class="msg-preview">{{ n.content }}</div>
          </div>
          <span class="msg-time">{{ formatTime(n.createdAt) }}</span>
          <button class="msg-delete" title="删除" @click.stop="confirmDelete(n.id)">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
              <path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>

        <div v-if="hasMore" class="load-more">
          <button class="btn-load" :disabled="loadingMore" @click="loadMore">
            <template v-if="loadingMore"><div class="spinner-sm" />加载中...</template>
            <template v-else>加载更多</template>
          </button>
        </div>
        <div v-else class="no-more">已显示全部 {{ displayedNotifications.length }} 条消息</div>
      </div>
    </div>

    <!-- Detail Drawer -->
    <Teleport to="body">
      <Transition name="drawer">
        <div v-if="detailVisible" class="drawer-mask" @click="detailVisible = false">
          <div class="drawer-panel" @click.stop>
            <div class="drawer-head">
              <h3>消息详情</h3>
              <button class="modal-close" @click="detailVisible = false">×</button>
            </div>
            <div v-if="detailMessage" class="drawer-body">
              <div class="detail-title">{{ detailMessage.title }}</div>
              <div class="detail-meta">
                <code class="code-badge">{{ getMsgTypeLabel(detailMessage.msgType) }}</code>
                <span class="meta-time">{{ formatFullTime(detailMessage.createdAt) }}</span>
                <span v-if="detailMessage.isRead && detailMessage.readAt" class="meta-read">
                  已读 · {{ formatFullTime(detailMessage.readAt) }}
                </span>
              </div>
              <div v-if="detailMessage.sourceEventType" class="detail-source">
                <span class="source-label">来源</span>
                <code class="code-badge">{{ detailMessage.sourceEventType }}</code>
              </div>
              <div class="detail-content">{{ detailMessage.content || '（无正文内容）' }}</div>
              <div class="detail-actions">
                <button class="btn-danger-outline" @click="confirmDelete(detailMessage.id)">删除此消息</button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Delete Confirm -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="deleteConfirmVisible"
          class="modal-mask"
          @mousedown="onMaskMouseDown"
          @click="onMaskClick($event, () => deleteConfirmVisible = false)"
        >
          <div class="modal-box">
            <div class="modal-head">
              <h3>确认删除</h3>
              <button class="modal-close" @click="deleteConfirmVisible = false">×</button>
            </div>
            <div class="modal-body">
              <p class="confirm-text">确认删除此消息？删除后不可恢复。</p>
            </div>
            <div class="modal-foot">
              <button class="btn-cancel" @click="deleteConfirmVisible = false">取消</button>
              <button class="btn-save btn-save-danger" @click="executeDelete">删除</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
/* ============================================
   Message List — 对齐项目风格 (MessageConfig / EventTrigger / EventTypeManagement)
   ============================================ */
.msg-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f8f9fb;
  font-family: 'DM Sans', sans-serif;
}

/* Header */
.msg-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 20px 24px 16px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
}
.header-left { display: flex; flex-direction: column; }
.page-title {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.025em;
  margin: 0;
}
.stats-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}
.stat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12.5px;
  color: #6b7280;
}
.stat b { font-weight: 600; color: #111827; }
.stat-unread b { color: #ef4444; }
.stat-sep { display: block; width: 1px; height: 10px; background: #d1d5db; }

.header-right { display: flex; gap: 8px; align-items: center; }

.btn-ghost {
  padding: 7px 14px;
  background: none;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.btn-ghost:hover { border-color: #9ca3af; color: #374151; }

.btn-create {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  background: #111827;
  color: #fff;
  border: none;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.btn-create:hover { background: #1f2937; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.15); }

/* Tab Bar */
.tab-bar {
  display: flex;
  gap: 0;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
}
.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.tab-btn:hover { color: #374151; }
.tab-btn.active { color: #111827; border-bottom-color: #111827; font-weight: 600; }
.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  font-size: 11px;
  font-weight: 600;
  background: #f3f4f6;
  color: #6b7280;
  border-radius: 9px;
}
.tab-btn.active .tab-count { background: #111827; color: #fff; }

/* Filter Bar */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 24px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
}
.search-box { position: relative; flex: 0 0 260px; }
.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  pointer-events: none;
}
.search-input {
  width: 100%;
  padding: 7px 10px 7px 32px;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  font-size: 13px;
  font-family: inherit;
  color: #374151;
  background: #f9fafb;
  transition: all 0.15s;
  outline: none;
}
.search-input:focus { border-color: #2563eb; background: #fff; box-shadow: 0 0 0 3px rgba(37,99,235,0.08); }
.search-input::placeholder { color: #9ca3af; }

.filter-select {
  padding: 7px 28px 7px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  font-size: 13px;
  font-family: inherit;
  color: #374151;
  background: #f9fafb url("data:image/svg+xml,%3Csvg width='10' height='6' viewBox='0 0 10 6' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%239ca3af' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E") right 10px center no-repeat;
  appearance: none;
  cursor: pointer;
  outline: none;
  transition: border-color 0.15s;
}
.filter-select:focus { border-color: #2563eb; }

.btn-reset {
  padding: 7px 12px;
  font-size: 12px;
  color: #6b7280;
  background: none;
  border: 1px dashed #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.btn-reset:hover { color: #374151; border-color: #9ca3af; }

/* List */
.list-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}
.msg-list {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}
.msg-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background 0.1s;
}
.msg-row:last-of-type { border-bottom: none; }
.msg-row:hover { background: #fafbfc; }
.msg-row.is-unread { background: #fbfcff; }
.msg-row.is-unread:hover { background: #f4f7ff; }

.dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.dot-read { background: #d1d5db; }
.dot-unread { background: #ef4444; }

.msg-main { flex: 1; min-width: 0; }
.msg-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.msg-title {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 1;
  min-width: 0;
}
.msg-title-unread { color: #111827; font-weight: 600; }
.msg-preview {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.msg-time {
  font-size: 11.5px;
  color: #9ca3af;
  white-space: nowrap;
  flex-shrink: 0;
}
.code-badge {
  display: inline-block;
  padding: 2px 7px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  color: #475569;
  white-space: nowrap;
  flex-shrink: 0;
}

.msg-delete {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  background: none;
  border: none;
  border-radius: 5px;
  color: #9ca3af;
  cursor: pointer;
  opacity: 0;
  transition: all 0.15s;
  flex-shrink: 0;
}
.msg-row:hover .msg-delete { opacity: 1; }
.msg-delete:hover { background: #fef2f2; color: #dc2626; }

/* Load more / empty / loading */
.load-more {
  display: flex;
  justify-content: center;
  padding: 12px 0 8px;
}
.btn-load {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  font-size: 12.5px;
  color: #6b7280;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.btn-load:hover:not(:disabled) { color: #111827; border-color: #9ca3af; }
.btn-load:disabled { opacity: 0.6; cursor: not-allowed; }

.no-more {
  text-align: center;
  padding: 12px 0 4px;
  font-size: 11.5px;
  color: #d1d5db;
}

.state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 72px 0;
}
.state-loading { color: #9ca3af; font-size: 13px; flex-direction: row; }
.empty-icon { color: #d1d5db; }
.empty-text { font-size: 13px; color: #9ca3af; margin: 0; }

/* Spinner */
.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e5e7eb;
  border-top-color: #111827;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
.spinner-sm {
  width: 12px;
  height: 12px;
  border: 2px solid #e5e7eb;
  border-top-color: #6b7280;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ============================================
   Drawer
   ============================================ */
.drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0,0,0,0.3);
  backdrop-filter: blur(2px);
  display: flex;
  justify-content: flex-end;
}
.drawer-panel {
  width: 460px;
  max-width: 90vw;
  height: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 24px rgba(0,0,0,0.12);
}
.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e8eaed;
  flex-shrink: 0;
}
.drawer-head h3 {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}
.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.detail-title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  line-height: 1.5;
}
.detail-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.meta-time { font-size: 12px; color: #6b7280; }
.meta-read { font-size: 11.5px; color: #16a34a; }
.detail-source {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f9fafb;
  border: 1px solid #f3f4f6;
  border-radius: 7px;
}
.source-label { font-size: 11.5px; color: #9ca3af; }
.detail-content {
  font-size: 13px;
  color: #374151;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 12px 14px;
  background: #f9fafb;
  border: 1px solid #f3f4f6;
  border-radius: 7px;
}
.detail-actions {
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}
.btn-danger-outline {
  padding: 7px 14px;
  background: none;
  border: 1px solid #fecaca;
  border-radius: 7px;
  font-size: 12.5px;
  font-weight: 500;
  color: #dc2626;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.btn-danger-outline:hover { background: #fef2f2; border-color: #f87171; }

/* Drawer transitions */
.drawer-enter-active { transition: all 0.25s ease-out; }
.drawer-leave-active { transition: all 0.2s ease-in; }
.drawer-enter-from, .drawer-leave-to { opacity: 0; }
.drawer-enter-from .drawer-panel, .drawer-leave-to .drawer-panel { transform: translateX(100%); }

/* ============================================
   Modal (Delete confirm)
   ============================================ */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(0,0,0,0.3);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-box {
  width: 400px;
  max-width: 90vw;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e8eaed;
}
.modal-head h3 { font-size: 15px; font-weight: 700; color: #111827; margin: 0; }
.modal-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}
.modal-close:hover { background: #e5e7eb; color: #111827; }
.modal-body { padding: 16px 20px; }
.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid #f3f4f6;
}
.confirm-text { font-size: 13px; color: #374151; margin: 0; line-height: 1.6; }

.btn-cancel {
  padding: 7px 16px;
  background: #f3f4f6;
  color: #6b7280;
  border: none;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.btn-cancel:hover { background: #e5e7eb; }
.btn-save {
  padding: 7px 16px;
  background: #111827;
  color: #fff;
  border: none;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.btn-save:hover { background: #1f2937; }
.btn-save-danger { background: #ef4444; }
.btn-save-danger:hover { background: #dc2626; }

/* Modal transitions */
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-active .modal-box, .modal-leave-active .modal-box { transition: transform 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .modal-box { transform: scale(0.95) translateY(10px); }
</style>
