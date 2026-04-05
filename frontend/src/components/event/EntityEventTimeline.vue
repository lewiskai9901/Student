<template>
  <div class="eet-root">
    <!-- Filter + Stats -->
    <div class="eet-toolbar">
      <div class="eet-filters">
        <select v-model="polarityFilter" class="eet-select" @change="filterEvents">
          <option value="">全部极性</option>
          <option value="POSITIVE">正向</option>
          <option value="NEGATIVE">负向</option>
          <option value="NEUTRAL">中性</option>
        </select>
        <select v-model="categoryFilter" class="eet-select" @change="filterEvents">
          <option value="">全部分类</option>
          <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
        </select>
        <span class="eet-total">共 <b>{{ filteredEvents.length }}</b> 条</span>
      </div>
      <div class="eet-stats">
        <span class="eet-stat">
          <em class="dot dot-positive" /> 正向 <b class="c-positive">{{ positiveCount }}</b>
        </span>
        <i class="stat-sep" />
        <span class="eet-stat">
          <em class="dot dot-negative" /> 负向 <b class="c-negative">{{ negativeCount }}</b>
        </span>
        <i class="stat-sep" />
        <span class="eet-stat">
          <em class="dot dot-neutral" /> 中性 <b class="c-neutral">{{ neutralCount }}</b>
        </span>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="eet-loading">
      <div class="spinner" />
      <span>加载中...</span>
    </div>

    <!-- Empty -->
    <div v-else-if="filteredEvents.length === 0" class="eet-empty">
      暂无事件记录
    </div>

    <!-- Timeline grouped by month -->
    <div v-else class="eet-timeline">
      <div v-for="group in monthGroups" :key="group.month" class="eet-month-group">
        <div class="eet-month-label">{{ group.month }}</div>
        <div class="eet-month-items">
          <div
            v-for="evt in group.events"
            :key="evt.id"
            class="eet-item"
            :class="{ expanded: expandedId === evt.id }"
            @click="toggleExpand(evt.id)"
          >
            <div class="eet-item-row">
              <em class="dot" :class="'dot-' + getPolarity(evt).toLowerCase()" />
              <span class="eet-date">{{ formatShortDate(evt.occurredAt) }}</span>
              <span class="eet-label">{{ evt.eventLabel || evt.eventType }}</span>
              <span class="eet-tag">[{{ evt.eventCategory }}/{{ evt.eventType }}]</span>
            </div>
            <!-- Expanded payload -->
            <Transition name="expand">
              <div v-if="expandedId === evt.id" class="eet-detail">
                <div v-if="evt.createdByName" class="eet-detail-row">
                  <span class="eet-detail-label">操作人</span>
                  <span>{{ evt.createdByName }}</span>
                </div>
                <div v-if="evt.sourceModule" class="eet-detail-row">
                  <span class="eet-detail-label">来源</span>
                  <span>{{ evt.sourceModule }}</span>
                </div>
                <div v-if="payloadEntries(evt).length" class="eet-detail-payload">
                  <div v-for="[key, val] in payloadEntries(evt)" :key="key" class="eet-detail-row">
                    <span class="eet-detail-label">{{ key }}</span>
                    <span>{{ val }}</span>
                  </div>
                </div>
                <div class="eet-detail-row">
                  <span class="eet-detail-label">时间</span>
                  <span>{{ formatFullDate(evt.occurredAt) }}</span>
                </div>
              </div>
            </Transition>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { entityEventApi, eventTypeApi } from '@/api/event'
import type { EntityEvent, EventType } from '@/types/event'

const props = defineProps<{
  subjectType: 'USER' | 'ORG_UNIT' | 'PLACE'
  subjectId: number | string
}>()

// ==================== State ====================
const loading = ref(false)
const events = ref<EntityEvent[]>([])
const eventTypes = ref<EventType[]>([])
const expandedId = ref<number | null>(null)

const polarityFilter = ref('')
const categoryFilter = ref('')

// ==================== Computed ====================
const categories = computed(() => {
  const set = new Set<string>()
  for (const e of events.value) {
    if (e.eventCategory) set.add(e.eventCategory)
  }
  return Array.from(set).sort()
})

const eventTypeMap = computed(() => {
  const map = new Map<string, EventType>()
  for (const et of eventTypes.value) {
    map.set(et.typeCode, et)
  }
  return map
})

function getPolarity(evt: EntityEvent): string {
  const et = eventTypeMap.value.get(evt.eventType)
  if (et) return et.polarity || 'NEUTRAL'
  // Guess from category
  const cat = (evt.eventCategory || '').toUpperCase()
  if (cat.includes('REWARD') || cat.includes('HONOR') || cat.includes('POSITIVE')) return 'POSITIVE'
  if (cat.includes('VIOLATION') || cat.includes('PENALTY') || cat.includes('NEGATIVE')) return 'NEGATIVE'
  return 'NEUTRAL'
}

const filteredEvents = computed(() => {
  let list = events.value
  if (polarityFilter.value) {
    list = list.filter(e => getPolarity(e) === polarityFilter.value)
  }
  if (categoryFilter.value) {
    list = list.filter(e => e.eventCategory === categoryFilter.value)
  }
  return list
})

const positiveCount = computed(() => events.value.filter(e => getPolarity(e) === 'POSITIVE').length)
const negativeCount = computed(() => events.value.filter(e => getPolarity(e) === 'NEGATIVE').length)
const neutralCount = computed(() => events.value.length - positiveCount.value - negativeCount.value)

interface MonthGroup {
  month: string
  events: EntityEvent[]
}

const monthGroups = computed<MonthGroup[]>(() => {
  const map = new Map<string, EntityEvent[]>()
  for (const evt of filteredEvents.value) {
    const d = evt.occurredAt || evt.createdAt
    const month = d ? d.substring(0, 7) : '未知'
    if (!map.has(month)) map.set(month, [])
    map.get(month)!.push(evt)
  }
  // Sort months descending
  return Array.from(map.entries())
    .sort(([a], [b]) => b.localeCompare(a))
    .map(([month, evts]) => ({
      month,
      events: evts.sort((a, b) => (b.occurredAt || '').localeCompare(a.occurredAt || '')),
    }))
})

// ==================== Helpers ====================
function formatShortDate(d: string): string {
  if (!d) return '--'
  return d.substring(5, 10) // MM-DD
}

function formatFullDate(d: string): string {
  if (!d) return '--'
  return d.replace('T', ' ').substring(0, 19)
}

function payloadEntries(evt: EntityEvent): [string, unknown][] {
  let payload = evt.payload
  if (!payload) return []
  if (typeof payload === 'string') {
    try { payload = JSON.parse(payload) } catch { return [] }
  }
  if (typeof payload !== 'object') return []
  return Object.entries(payload as Record<string, unknown>)
}

function toggleExpand(id: number) {
  expandedId.value = expandedId.value === id ? null : id
}

function filterEvents() { /* reactive */ }

// ==================== Load ====================
async function loadEvents() {
  if (!props.subjectId) return
  loading.value = true
  try {
    const [evtList, etList] = await Promise.all([
      entityEventApi.bySubject(props.subjectType, Number(props.subjectId), 200),
      eventTypeApi.list(),
    ])
    events.value = (evtList as any) || []
    eventTypes.value = (etList as any) || []
  } catch (e: any) {
    console.error('Failed to load events:', e)
  } finally {
    loading.value = false
  }
}

// ==================== Lifecycle ====================
onMounted(() => { loadEvents() })

watch(() => [props.subjectType, props.subjectId], () => { loadEvents() })
</script>

<style scoped>
/* ============================================
   Entity Event Timeline — Embeddable Component
   ============================================ */
.eet-root {
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  color: #374151;
}

/* Toolbar */
.eet-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  gap: 12px;
  flex-wrap: wrap;
}
.eet-filters { display: flex; align-items: center; gap: 8px; }
.eet-select {
  padding: 5px 24px 5px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  font-family: inherit;
  color: #374151;
  background: #fafafa url("data:image/svg+xml,%3Csvg width='8' height='5' viewBox='0 0 8 5' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1l3 3 3-3' stroke='%239ca3af' stroke-width='1.2' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E") right 8px center no-repeat;
  appearance: none;
  outline: none;
  cursor: pointer;
}
.eet-total { font-size: 12px; color: #9ca3af; }
.eet-total b { font-weight: 600; color: #374151; }

.eet-stats { display: flex; align-items: center; gap: 8px; }
.eet-stat { font-size: 12px; color: #6b7280; }
.eet-stat b { font-weight: 600; }
.c-positive { color: #16a34a; }
.c-negative { color: #dc2626; }
.c-neutral { color: #6b7280; }
.stat-sep { display: block; width: 1px; height: 10px; background: #d1d5db; }
.dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 3px; vertical-align: middle; }
.dot-positive { background: #22c55e; }
.dot-negative { background: #ef4444; }
.dot-neutral { background: #9ca3af; }
.dot-info { background: #3b82f6; }

/* Loading */
.eet-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 24px 0;
  justify-content: center;
  color: #9ca3af;
  font-size: 13px;
}
.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #e5e7eb;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Empty */
.eet-empty {
  text-align: center;
  padding: 32px 0;
  color: #9ca3af;
  font-size: 13px;
}

/* Timeline */
.eet-timeline {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.eet-month-group { margin-bottom: 8px; }
.eet-month-label {
  font-size: 11px;
  font-weight: 700;
  color: #9ca3af;
  letter-spacing: 0.05em;
  padding: 6px 0 4px;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 2px;
}

.eet-month-items {
  display: flex;
  flex-direction: column;
}

.eet-item {
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.1s;
}
.eet-item:hover { background: #f8fafc; }
.eet-item.expanded { background: #f0f5ff; }

.eet-item-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
}
.eet-date {
  font-family: 'JetBrains Mono', monospace;
  font-size: 11.5px;
  color: #6b7280;
  flex-shrink: 0;
  width: 42px;
}
.eet-label {
  font-size: 13px;
  color: #111827;
  font-weight: 500;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.eet-tag {
  font-size: 10px;
  color: #9ca3af;
  flex-shrink: 0;
  font-family: 'JetBrains Mono', monospace;
}

/* Detail expansion */
.eet-detail {
  padding: 4px 8px 8px 28px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.eet-detail-row {
  display: flex;
  gap: 8px;
  font-size: 12px;
}
.eet-detail-label {
  color: #9ca3af;
  font-weight: 600;
  min-width: 56px;
  flex-shrink: 0;
}
.eet-detail-payload {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 4px 0;
  border-top: 1px dashed #e5e7eb;
  border-bottom: 1px dashed #e5e7eb;
}

/* Expand transition */
.expand-enter-active, .expand-leave-active { transition: all 0.15s; overflow: hidden; }
.expand-enter-from, .expand-leave-to { opacity: 0; max-height: 0; }
.expand-enter-to, .expand-leave-from { opacity: 1; max-height: 200px; }
</style>
