<script setup lang="ts">
import { computed } from 'vue'
import { Users, ClipboardList, ClipboardCheck, AlertTriangle, Crown } from 'lucide-vue-next'
import type { WorkbenchSummary } from '@/api/inspection/project'

const props = defineProps<{
  summary: WorkbenchSummary | null
}>()

const emit = defineEmits<{
  (e: 'navigate', view: 'people' | 'tasks' | 'matrix', filter?: string): void
}>()

const isEmpty = computed(() => !props.summary)
</script>

<template>
  <div class="tsb" v-if="!isEmpty && summary">
    <!-- 1. 人员总数 -->
    <button class="tsb-item tsb-item--clickable" @click="emit('navigate', 'people')">
      <Users class="tsb-icon" />
      <span class="tsb-label">共</span>
      <span class="tsb-num">{{ summary.totalPeople }}</span>
      <span class="tsb-label">人</span>
    </button>
    <span class="tsb-divider" />

    <!-- 2. 待分配 -->
    <button class="tsb-item tsb-item--clickable"
            :class="{ 'tsb-item--alert': summary.pendingAssignCount > 0 }"
            @click="emit('navigate', 'tasks', 'pendingAssign')">
      <ClipboardList class="tsb-icon" />
      <span class="tsb-label">待分配</span>
      <span class="tsb-num">{{ summary.pendingAssignCount }}</span>
    </button>
    <span class="tsb-divider" />

    <!-- 3. 待审核 -->
    <button class="tsb-item tsb-item--clickable"
            :class="{ 'tsb-item--alert': summary.pendingReviewCount > 0 }"
            @click="emit('navigate', 'tasks', 'pendingReview')">
      <ClipboardCheck class="tsb-icon" />
      <span class="tsb-label">待审核</span>
      <span class="tsb-num">{{ summary.pendingReviewCount }}</span>
    </button>
    <span class="tsb-divider" />

    <!-- 4. 逾期 -->
    <button class="tsb-item tsb-item--clickable"
            :class="{ 'tsb-item--danger': summary.overdueCount > 0 }"
            @click="emit('navigate', 'tasks', 'overdue')">
      <AlertTriangle class="tsb-icon" />
      <span class="tsb-label">逾期</span>
      <span class="tsb-num">{{ summary.overdueCount }}</span>
    </button>
    <span class="tsb-divider" v-if="summary.leadName" />

    <!-- 5. 负责人 -->
    <div class="tsb-item tsb-item--lead" v-if="summary.leadName">
      <Crown class="tsb-icon tsb-icon--lead" />
      <span class="tsb-label">负责人</span>
      <span class="tsb-lead-name">{{ summary.leadName }}</span>
    </div>
  </div>
  <div class="tsb tsb--empty" v-else>
    <span class="tsb-label">暂无人员数据</span>
  </div>
</template>

<style scoped>
.tsb {
  display: flex;
  align-items: center;
  gap: 0;
  flex-wrap: wrap;
  background: var(--insp-bg-surface, #fff);
  border: 1px solid var(--insp-border-default, #e5e7eb);
  border-radius: var(--insp-radius-md, 8px);
  padding: 10px 16px;
  margin-bottom: 12px;
  font-size: 13px;
}
.tsb--empty { color: #9ca3af; justify-content: center; padding: 14px; }

.tsb-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: none;
  border: none;
  color: var(--insp-ink-secondary, #4b5563);
  font-size: 13px;
  cursor: default;
}
.tsb-item--clickable { cursor: pointer; border-radius: 6px; transition: background 0.15s; }
.tsb-item--clickable:hover { background: var(--insp-bg-subtle, #f3f4f6); }
.tsb-item--alert { color: #d97706; }
.tsb-item--alert .tsb-num { color: #d97706; font-weight: 700; }
.tsb-item--danger { color: #dc2626; }
.tsb-item--danger .tsb-num { color: #dc2626; font-weight: 700; }
.tsb-item--lead { background: linear-gradient(90deg, #fffbeb 0%, #fef9c3 100%); border-radius: 6px; padding-left: 10px; padding-right: 14px; }

.tsb-icon { width: 14px; height: 14px; color: var(--insp-ink-tertiary, #6b7280); }
.tsb-icon--lead { color: #d97706; }
.tsb-label { color: var(--insp-ink-tertiary, #6b7280); font-size: 12px; }
.tsb-num { color: var(--insp-ink-primary, #111827); font-weight: 600; font-size: 14px; }
.tsb-lead-name { color: #92400e; font-weight: 600; }
.tsb-divider { width: 1px; height: 16px; background: var(--insp-border-default, #e5e7eb); margin: 0 2px; }
</style>
