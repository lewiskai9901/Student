<template>
  <button class="ssc-card" :class="{ 'ssc-card--draft': isDraft, 'ssc-card--readonly': !isDraft }" @click="$emit('edit', profile.id)">
    <!-- 头: section 名 + 状态点 + 版本 -->
    <div class="ssc-head">
      <span class="ssc-dot" aria-hidden></span>
      <span class="ssc-name">{{ sectionName || '未关联分区' }}</span>
      <span v-if="profile.currentVersion" class="ssc-ver">v{{ profile.currentVersion }}</span>
    </div>

    <!-- 主体: 关键参数 + 启用算法 -->
    <div class="ssc-body">
      <div class="ssc-line">
        <span>{{ profile.minScore }}–{{ profile.maxScore }} 分</span>
        <span class="ssc-sep">·</span>
        <span>精度 {{ profile.precisionDigits }}</span>
      </div>
      <div class="ssc-line">
        <span v-if="normMode" class="ssc-flag">{{ normMode }}</span>
        <span v-else class="ssc-muted">未归一化</span>
      </div>
      <div class="ssc-line">
        <span v-if="ruleMode">{{ ruleMode }}</span>
        <span v-else class="ssc-muted">规则未配置</span>
      </div>
    </div>

    <!-- 脚: 操作 -->
    <div class="ssc-foot">
      <span class="ssc-foot-action">
        <Pencil class="ssc-icon" />
        <span>{{ isDraft && !isArchived ? '编辑配置' : '查看配置' }}</span>
      </span>
    </div>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Pencil } from 'lucide-vue-next'
import type { LongId } from '@/types/common'
import type { ScoringProfile } from '@/types/insp/scoring'
import { NormalizeByOptions } from '@/types/insp/scoring'

const props = defineProps<{
  profile: ScoringProfile
  sectionName: string | null
  isDraft: boolean
  isArchived: boolean
}>()

defineEmits<{
  edit: [profileId: LongId]
}>()

const ruleMode = computed(() => {
  // 暂未拿到本 profile 的 rules 列表 (避免 N+1 API), 留占位
  // L4 会引入实时预览, 那时再细化
  return null as string | null
})

// 规模归一化维度 (规模公平性) — 取代原"高级算法"标记
const normMode = computed(() => {
  const nb = props.profile.normalizeBy
  if (!nb || nb === 'NONE') return null
  return NormalizeByOptions.find(o => o.value === nb)?.label ?? null
})
</script>

<style scoped>
.ssc-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  background: var(--insp-bg-surface, #fff);
  border: 1px solid var(--insp-border-subtle, #e5e7eb);
  border-radius: 8px;
  text-align: left;
  font: inherit;
  color: inherit;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s, transform 0.1s;
}
.ssc-card:hover {
  border-color: var(--insp-accent, #2563eb);
  box-shadow: 0 1px 4px rgba(37, 99, 235, 0.08);
}
.ssc-card:active { transform: translateY(1px); }
.ssc-card--readonly { cursor: pointer; opacity: 0.95; }

.ssc-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ssc-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--insp-accent, #2563eb);
  flex-shrink: 0;
}
.ssc-name {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: var(--insp-ink-primary, #111827);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ssc-ver {
  font-size: 11px;
  font-family: var(--insp-font-mono, monospace);
  color: var(--insp-ink-tertiary, #6b7280);
  font-weight: 500;
  padding: 1px 6px;
  border-radius: 3px;
  background: var(--insp-bg-subtle, #f3f4f6);
}

.ssc-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: var(--insp-ink-secondary, #374151);
}
.ssc-line {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.ssc-sep { color: var(--insp-ink-quaternary, #9ca3af); }
.ssc-muted { color: var(--insp-ink-quaternary, #9ca3af); font-style: italic; }
.ssc-flags {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 2px;
}
.ssc-flag {
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 9px;
  background: var(--insp-accent-tint, #eff6ff);
  color: var(--insp-accent, #2563eb);
  font-weight: 500;
}
.ssc-flag--off {
  background: var(--insp-bg-subtle, #f3f4f6);
  color: var(--insp-ink-tertiary, #6b7280);
}

.ssc-foot {
  display: flex;
  justify-content: flex-end;
  padding-top: 6px;
  border-top: 1px solid var(--insp-border-subtle, #f1f3f5);
}
.ssc-foot-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--insp-accent, #2563eb);
  font-weight: 500;
}
.ssc-icon {
  width: 12px;
  height: 12px;
}
</style>
