<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from 'lucide-vue-next'
import { getSubjectTimeline, getSubjectStats } from '@/api/entityEvent'
import type { EntityEvent, EntityEventStats } from '@/types/entityEvent'

const route = useRoute()
const router = useRouter()

const subjectType = route.params.subjectType as string
const subjectId = Number(route.params.subjectId)

// ========== State ==========
const loading = ref(false)
const loadingMore = ref(false)
const stats = ref<EntityEventStats | null>(null)
const events = ref<EntityEvent[]>([])
const allEvents = ref<EntityEvent[]>([])
const activeFilter = ref('ALL')
const pageSize = 20
const displayCount = ref(pageSize)

// ========== Filter Options ==========
const filterOptions = [
  { label: '全部', value: 'ALL' },
  { label: '检查', value: 'INSP' },
  { label: '违规', value: 'VIOLATION' },
  { label: '荣誉', value: 'HONOR' },
  { label: '组织', value: 'ORG' },
  { label: '场所', value: 'PLACE' },
]

// ========== Computed ==========
const subjectTypeLabel = computed(() => {
  const map: Record<string, string> = {
    student: '学生',
    class: '班级',
    dormitory: '宿舍',
    org_unit: '组织',
    place: '场所',
    user: '用户',
  }
  return map[subjectType] || subjectType
})

const subjectName = computed(() => {
  if (allEvents.value.length > 0 && allEvents.value[0].subjectName) {
    return allEvents.value[0].subjectName
  }
  return `#${subjectId}`
})

const filteredEvents = computed(() => {
  if (activeFilter.value === 'ALL') return allEvents.value
  return allEvents.value.filter((e) => {
    const cat = e.eventCategory?.toUpperCase() || ''
    const type = e.eventType?.toUpperCase() || ''
    const filter = activeFilter.value
    if (filter === 'INSP') return cat.includes('INSP') && !type.includes('VIOLATION') && !type.includes('HONOR')
    if (filter === 'VIOLATION') return type.includes('VIOLATION') || cat === 'VIOLATION'
    if (filter === 'HONOR') return type.includes('HONOR') || cat === 'HONOR' || type.includes('RATING')
    if (filter === 'ORG') return cat.startsWith('ORG')
    if (filter === 'PLACE') return cat.startsWith('PLACE')
    return true
  })
})

const displayedEvents = computed(() => filteredEvents.value.slice(0, displayCount.value))

const hasMore = computed(() => displayCount.value < filteredEvents.value.length)

// Group events by date
const groupedEvents = computed(() => {
  const groups: Array<{ date: string; events: EntityEvent[] }> = []
  const dateMap = new Map<string, EntityEvent[]>()

  for (const event of displayedEvents.value) {
    const date = event.occurredAt ? event.occurredAt.substring(0, 10) : '未知日期'
    if (!dateMap.has(date)) {
      dateMap.set(date, [])
      groups.push({ date, events: dateMap.get(date)! })
    }
    dateMap.get(date)!.push(event)
  }
  return groups
})

// ========== Stats helpers ==========
const totalCount = computed(() => stats.value?.totalCount ?? 0)
const violationCount = computed(() => stats.value?.categoryBreakdown?.VIOLATION ?? 0)
const honorCount = computed(() => stats.value?.categoryBreakdown?.HONOR ?? 0)

function getAvgScore(): string {
  const scoreEvents = allEvents.value.filter((e) => {
    try {
      if (!e.payload) return false
      const p = JSON.parse(e.payload)
      return p.score != null
    } catch {
      return false
    }
  })
  if (scoreEvents.length === 0) return '-'
  const sum = scoreEvents.reduce((acc, e) => {
    try {
      const p = JSON.parse(e.payload!)
      return acc + (p.score ?? 0)
    } catch {
      return acc
    }
  }, 0)
  return (sum / scoreEvents.length).toFixed(1)
}

// ========== Event display helpers ==========
function getEventDotClass(event: EntityEvent): string {
  const type = event.eventType?.toUpperCase() || ''
  const cat = event.eventCategory?.toUpperCase() || ''
  if (type.includes('VIOLATION')) return 'dot-red'
  if (type.includes('RATING') || type.includes('HONOR')) return 'dot-gold'
  if (cat.startsWith('ORG')) return 'dot-blue'
  if (cat.startsWith('PLACE')) return 'dot-gray'
  if (cat.includes('INSP')) return 'dot-green'
  return 'dot-default'
}

function getEventIcon(event: EntityEvent): string {
  const type = event.eventType?.toUpperCase() || ''
  const cat = event.eventCategory?.toUpperCase() || ''
  if (type.includes('VIOLATION')) return '🔴'
  if (type.includes('RATING')) return '🏆'
  if (type.includes('HONOR')) return '🏅'
  if (cat.startsWith('ORG')) return '👥'
  if (cat.startsWith('PLACE')) return '📍'
  if (cat.includes('INSP')) return '🟢'
  return '📋'
}

function getEventLabel(event: EntityEvent): string {
  return event.eventLabel || event.eventType || ''
}

function getEventTime(event: EntityEvent): string {
  if (!event.occurredAt) return ''
  const d = new Date(event.occurredAt)
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${hh}:${mm}`
}

function formatDate(dateStr: string): string {
  if (!dateStr || dateStr === '未知日期') return dateStr
  const d = new Date(dateStr)
  const now = new Date()
  const today = now.toISOString().substring(0, 10)
  const yesterday = new Date(now.getTime() - 86400000).toISOString().substring(0, 10)
  if (dateStr === today) return '今天'
  if (dateStr === yesterday) return '昨天'
  const m = d.getMonth() + 1
  const dd = d.getDate()
  return `${m}/${dd}`
}

function parsePayloadSummary(event: EntityEvent): string {
  if (!event.payload) return ''
  try {
    const p = JSON.parse(event.payload)
    const parts: string[] = []
    if (p.score != null) parts.push(`${p.score}分`)
    if (p.grade) parts.push(p.grade)
    if (p.rank) parts.push(`排名第${p.rank}`)
    if (p.reason) parts.push(p.reason)
    if (p.description) parts.push(p.description)
    return parts.join('  ')
  } catch {
    return ''
  }
}

// ========== API ==========
async function loadData() {
  loading.value = true
  try {
    const [statsData, timelineData] = await Promise.all([
      getSubjectStats(subjectType, subjectId),
      getSubjectTimeline(subjectType, subjectId, 200),
    ])
    stats.value = statsData
    allEvents.value = timelineData
  } catch (e: any) {
    ElMessage.error(e.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

function loadMore() {
  loadingMore.value = true
  setTimeout(() => {
    displayCount.value += pageSize
    loadingMore.value = false
  }, 200)
}

function setFilter(val: string) {
  activeFilter.value = val
  displayCount.value = pageSize
}

function goBack() {
  router.back()
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="timeline-page">
    <!-- Header -->
    <div class="page-header">
      <button class="back-btn" @click="goBack">
        <ArrowLeft class="back-icon" />
      </button>
      <div class="header-info">
        <div class="header-title">
          <span class="subject-name">{{ subjectName }}</span>
          <span class="title-sep">·</span>
          <span class="title-sub">事件记录</span>
        </div>
        <div class="header-meta">
          <span class="subject-type-badge">{{ subjectTypeLabel }}</span>
          <span class="meta-id">ID {{ subjectId }}</span>
        </div>
      </div>
    </div>

    <!-- Stats row -->
    <div class="stats-row" v-loading="loading">
      <div class="stat-card">
        <div class="stat-value">{{ totalCount }}</div>
        <div class="stat-label">总事件</div>
      </div>
      <div class="stat-card stat-card--red">
        <div class="stat-value">{{ violationCount }}</div>
        <div class="stat-label">违规</div>
      </div>
      <div class="stat-card stat-card--gold">
        <div class="stat-value">{{ honorCount }}</div>
        <div class="stat-label">荣誉</div>
      </div>
      <div class="stat-card stat-card--blue">
        <div class="stat-value">{{ getAvgScore() }}</div>
        <div class="stat-label">均分</div>
      </div>
    </div>

    <!-- Filter pills -->
    <div class="filter-bar">
      <button
        v-for="opt in filterOptions"
        :key="opt.value"
        class="filter-pill"
        :class="{ active: activeFilter === opt.value }"
        @click="setFilter(opt.value)"
      >
        {{ opt.label }}
      </button>
    </div>

    <!-- Timeline -->
    <div class="timeline-container" v-loading="loading">
      <template v-if="!loading && groupedEvents.length === 0">
        <div class="empty-state">
          <div class="empty-icon">📋</div>
          <div class="empty-text">暂无事件记录</div>
        </div>
      </template>

      <template v-for="group in groupedEvents" :key="group.date">
        <!-- Date separator -->
        <div class="date-sep">
          <span class="date-label">{{ formatDate(group.date) }}</span>
        </div>

        <!-- Event items -->
        <div
          v-for="event in group.events"
          :key="event.id"
          class="timeline-item"
        >
          <!-- Left: dot + line -->
          <div class="tl-left">
            <div class="tl-dot" :class="getEventDotClass(event)"></div>
            <div class="tl-line"></div>
          </div>

          <!-- Right: content -->
          <div class="tl-content">
            <div class="tl-row">
              <span class="tl-time">{{ getEventTime(event) }}</span>
              <span class="tl-icon">{{ getEventIcon(event) }}</span>
              <span class="tl-label">{{ getEventLabel(event) }}</span>
              <span v-if="parsePayloadSummary(event)" class="tl-payload">
                {{ parsePayloadSummary(event) }}
              </span>
            </div>
            <div v-if="event.createdByName || event.sourceModule" class="tl-meta">
              <span v-if="event.sourceModule" class="tl-source">来源: {{ event.sourceModule }}</span>
              <span v-if="event.createdByName" class="tl-author">{{ event.createdByName }}</span>
            </div>
          </div>
        </div>
      </template>

      <!-- Load more -->
      <div v-if="hasMore" class="load-more">
        <button class="load-more-btn" @click="loadMore" :disabled="loadingMore">
          {{ loadingMore ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.timeline-page {
  padding: 20px 24px;
  max-width: 800px;
  margin: 0 auto;
  font-family: inherit;
}

/* Header */
.page-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 18px;
}

.back-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  color: #6b7280;
  display: flex;
  align-items: center;
  border-radius: 4px;
  margin-top: 2px;
}

.back-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.back-icon {
  width: 16px;
  height: 16px;
}

.header-info {
  flex: 1;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 4px;
}

.title-sep {
  color: #9ca3af;
}

.title-sub {
  color: #6b7280;
  font-weight: 400;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.subject-type-badge {
  background: #eff6ff;
  color: #1a6dff;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 7px;
  border-radius: 4px;
  border: 1px solid #bfdbfe;
}

.meta-id {
  font-size: 11px;
  color: #9ca3af;
}

/* Stats row */
.stats-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 80px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px 14px;
  text-align: center;
}

.stat-card--red .stat-value { color: #ef4444; }
.stat-card--gold .stat-value { color: #d97706; }
.stat-card--blue .stat-value { color: #1a6dff; }

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  line-height: 1.2;
}

.stat-label {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 3px;
}

/* Filter pills */
.filter-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.filter-pill {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  color: #6b7280;
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.15s;
}

.filter-pill:hover {
  border-color: #1a6dff;
  color: #1a6dff;
}

.filter-pill.active {
  background: #1a6dff;
  border-color: #1a6dff;
  color: #fff;
  font-weight: 600;
}

/* Timeline container */
.timeline-container {
  min-height: 120px;
}

.empty-state {
  text-align: center;
  padding: 48px 0;
  color: #9ca3af;
}

.empty-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.empty-text {
  font-size: 13px;
}

/* Date separator */
.date-sep {
  display: flex;
  align-items: center;
  margin: 16px 0 8px 0;
}

.date-label {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
}

/* Timeline item */
.timeline-item {
  display: flex;
  gap: 0;
  margin-bottom: 2px;
}

.tl-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 24px;
  flex-shrink: 0;
}

.tl-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
  border: 2px solid transparent;
}

.tl-dot.dot-green  { background: #10b981; }
.tl-dot.dot-red    { background: #ef4444; }
.tl-dot.dot-gold   { background: #d97706; }
.tl-dot.dot-blue   { background: #1a6dff; }
.tl-dot.dot-gray   { background: #9ca3af; }
.tl-dot.dot-default{ background: #d1d5db; }

.tl-line {
  flex: 1;
  width: 1px;
  background: #e5e7eb;
  margin-top: 4px;
  min-height: 20px;
}

.tl-content {
  flex: 1;
  padding: 4px 0 10px 10px;
}

.tl-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.tl-time {
  font-size: 11px;
  color: #9ca3af;
  min-width: 36px;
  flex-shrink: 0;
}

.tl-icon {
  font-size: 13px;
  line-height: 1;
}

.tl-label {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
}

.tl-payload {
  font-size: 12px;
  color: #6b7280;
}

.tl-meta {
  display: flex;
  gap: 10px;
  margin-top: 3px;
  padding-left: 0;
}

.tl-source,
.tl-author {
  font-size: 11px;
  color: #9ca3af;
}

/* Load more */
.load-more {
  text-align: center;
  padding: 20px 0;
}

.load-more-btn {
  background: none;
  border: 1px solid #e5e7eb;
  color: #6b7280;
  font-size: 12px;
  padding: 6px 20px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}

.load-more-btn:hover:not(:disabled) {
  border-color: #1a6dff;
  color: #1a6dff;
}

.load-more-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
