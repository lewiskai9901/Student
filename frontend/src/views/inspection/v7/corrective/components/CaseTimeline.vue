<script setup lang="ts">
/**
 * CaseTimeline - Vertical timeline for corrective case history
 *
 * Displays a chronological list of events in the corrective case lifecycle,
 * with color-coded icons per event type.
 */
import {
  Plus,
  UserPlus,
  Send,
  CheckCircle,
  Lock,
  AlertTriangle,
  RotateCcw,
} from 'lucide-vue-next'
import { computed, type Component } from 'vue'

type EventType = 'create' | 'assign' | 'submit' | 'verify' | 'close' | 'escalate' | 'reopen'

interface TimelineEvent {
  date: string
  action: string
  actor: string
  notes?: string
  type: EventType
}

const props = defineProps<{
  events: TimelineEvent[]
}>()

// Map event type to Element Plus timeline node type
const typeColorMap: Record<EventType, string> = {
  create: 'info',
  assign: 'primary',
  submit: 'warning',
  verify: 'success',
  close: 'success',
  escalate: 'danger',
  reopen: 'warning',
}

// Map event type to icon component
const typeIconMap: Record<EventType, Component> = {
  create: Plus,
  assign: UserPlus,
  submit: Send,
  verify: CheckCircle,
  close: Lock,
  escalate: AlertTriangle,
  reopen: RotateCcw,
}

const sortedEvents = computed(() => {
  return [...props.events].sort(
    (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime(),
  )
})

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}
</script>

<template>
  <div class="case-timeline">
    <el-timeline v-if="sortedEvents.length > 0">
      <el-timeline-item
        v-for="(event, index) in sortedEvents"
        :key="index"
        :type="typeColorMap[event.type] as any"
        :hollow="false"
        :timestamp="formatDate(event.date)"
        placement="top"
      >
        <template #dot>
          <div
            class="timeline-dot"
            :class="`timeline-dot--${typeColorMap[event.type]}`"
          >
            <component :is="typeIconMap[event.type]" class="w-3 h-3" />
          </div>
        </template>
        <div class="timeline-content">
          <div class="timeline-header">
            <span class="timeline-action font-medium">{{ event.action }}</span>
            <span class="timeline-actor text-gray-500 text-sm ml-2">
              -- {{ event.actor }}
            </span>
          </div>
          <div v-if="event.notes" class="timeline-notes text-gray-600 text-sm mt-1">
            {{ event.notes }}
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else description="暂无历史记录" :image-size="64" />
  </div>
</template>

<style scoped>
.case-timeline {
  padding: 4px 0;
}

.timeline-dot {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  color: #fff;
}

.timeline-dot--info {
  background-color: #909399;
}

.timeline-dot--primary {
  background-color: #409eff;
}

.timeline-dot--warning {
  background-color: #e6a23c;
}

.timeline-dot--success {
  background-color: #67c23a;
}

.timeline-dot--danger {
  background-color: #f56c6c;
}

.timeline-content {
  padding: 4px 0;
}

.timeline-header {
  display: flex;
  align-items: center;
}
</style>
