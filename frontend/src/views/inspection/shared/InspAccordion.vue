<template>
  <section class="ia" :class="{ 'ia--collapsed': !expanded }">
    <button class="ia-head" :aria-expanded="expanded" @click="toggle">
      <span class="ia-dot" :class="dotClass" aria-hidden />
      <span class="ia-title">{{ title }}</span>
      <span v-if="summary" class="ia-summary" :class="summaryClass">{{ summary }}</span>
      <slot name="summary" />
      <ChevronDown class="ia-arrow" :class="{ flip: expanded }" :size="14" />
    </button>
    <div v-show="expanded" class="ia-body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ChevronDown } from 'lucide-vue-next'

const props = withDefaults(defineProps<{
  title: string
  summary?: string
  /** 状态: ok=已配置/绿点 / warn=可选未做/黄点 / error=必填未做/红点 */
  status?: 'ok' | 'warn' | 'error' | 'neutral'
  defaultExpanded?: boolean
  /** localStorage key for state persistence; omit to disable */
  storageKey?: string
}>(), {
  status: 'neutral',
  defaultExpanded: true,
})

const expanded = ref(props.defaultExpanded)

onMounted(() => {
  if (!props.storageKey) return
  const stored = localStorage.getItem(props.storageKey)
  if (stored !== null) expanded.value = stored === '1'
})

function toggle() {
  expanded.value = !expanded.value
  if (props.storageKey) {
    localStorage.setItem(props.storageKey, expanded.value ? '1' : '0')
  }
}

const dotClass = computed(() => `ia-dot--${props.status}`)
const summaryClass = computed(() => `ia-summary--${props.status}`)
</script>

<style scoped>
.ia {
  border: 1px solid var(--insp-border-subtle, #e5e7eb);
  border-radius: 8px;
  background: var(--insp-bg-surface, #fff);
  overflow: hidden;
  transition: border-color 0.15s;
}
.ia--collapsed { background: var(--insp-bg-subtle, #fafbfc); }

.ia-head {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: transparent;
  border: 0;
  cursor: pointer;
  font: inherit;
  text-align: left;
  color: var(--insp-ink-primary, #111827);
  transition: background 0.15s;
}
.ia-head:hover { background: var(--insp-bg-subtle-hover, #f3f4f6); }

.ia-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.ia-dot--ok      { background: var(--insp-accent, #2563eb); }
.ia-dot--warn    { background: #eab308; }
.ia-dot--error   { background: #ef4444; }
.ia-dot--neutral { background: var(--insp-ink-quaternary, #d1d5db); }

.ia-title {
  font-size: 13px;
  font-weight: 600;
}
.ia-summary {
  font-size: 12px;
  color: var(--insp-ink-tertiary, #6b7280);
  font-weight: 400;
  margin-left: 4px;
}
.ia-summary--ok    { color: var(--insp-ink-tertiary, #6b7280); }
.ia-summary--warn  { color: #b45309; }
.ia-summary--error { color: #b91c1c; }

.ia-arrow {
  margin-left: auto;
  color: var(--insp-ink-tertiary, #6b7280);
  transition: transform 0.15s;
  flex-shrink: 0;
}
.ia-arrow.flip { transform: rotate(180deg); }

.ia-body {
  padding: 14px 16px 16px;
  border-top: 1px solid var(--insp-border-subtle, #e5e7eb);
}
</style>
