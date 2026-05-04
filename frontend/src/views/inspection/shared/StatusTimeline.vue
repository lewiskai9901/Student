<script setup lang="ts">
/**
 * StatusTimeline — 状态机可视化组件
 * 用于 任务 / 整改 / 申诉 详情页, 让用户清楚知道:
 *   - 当前卡在哪一步
 *   - 谁是下一步的责任人
 *   - 已经走过的节点 (含时间戳 + 操作人)
 */
import { computed } from 'vue'

interface TimelineStep {
  /** 状态 code (如 PENDING, CLAIMED, IN_PROGRESS) */
  code: string
  /** 显示标签 (如 "待领取") */
  label: string
  /** 该状态下的责任人提示 (如 "等待检查员领取") */
  hint?: string
  /** 进入该状态的时间戳 */
  at?: string | null
  /** 进入该状态的操作人 */
  by?: string | null
  /** 终止/失败状态? (红色) */
  isTerminal?: boolean
  /** 跳过 (非主线状态)? (灰色虚线) */
  isAlternate?: boolean
}

const props = defineProps<{
  steps: TimelineStep[]
  /** 当前所处的 code */
  current: string
}>()

const enriched = computed(() => {
  let pastCurrent = false
  return props.steps.map(s => {
    const isCurrent = s.code === props.current
    if (isCurrent) pastCurrent = true
    return {
      ...s,
      isCurrent,
      isPast: !isCurrent && !pastCurrent,
      isFuture: !isCurrent && pastCurrent,
    }
  })
})

function fmtTime(s?: string | null): string {
  if (!s) return ''
  const d = new Date(s)
  const M = String(d.getMonth() + 1).padStart(2, '0')
  const D = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${M}.${D} ${h}:${m}`
}
</script>

<template>
  <ol class="tl">
    <li
      v-for="(step, i) in enriched" :key="step.code"
      class="tl-step"
      :class="{
        'is-past': step.isPast,
        'is-current': step.isCurrent,
        'is-future': step.isFuture,
        'is-alternate': step.isAlternate,
        'is-terminal': step.isTerminal,
      }"
    >
      <div class="tl-marker">
        <span class="tl-dot">
          <span v-if="step.isPast" class="tl-glyph">√</span>
          <span v-else-if="step.isCurrent" class="tl-pulse" />
          <span v-else>{{ i + 1 }}</span>
        </span>
        <div v-if="i < enriched.length - 1" class="tl-line" />
      </div>

      <div class="tl-body">
        <div class="tl-line1">
          <span class="tl-label">{{ step.label }}</span>
          <span v-if="step.at" class="tl-time insp-num">{{ fmtTime(step.at) }}</span>
        </div>
        <div v-if="step.isCurrent && step.hint" class="tl-hint">
          -> {{ step.hint }}
        </div>
        <div v-if="step.by" class="tl-by">{{ step.by }}</div>
      </div>
    </li>
  </ol>
</template>

<style scoped>
.tl {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.tl-step {
  display: grid;
  grid-template-columns: 28px 1fr;
  gap: 10px;
  padding: 8px 0;
  position: relative;
}

.tl-marker {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.tl-dot {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--insp-bg-surface);
  border: 1.5px solid var(--insp-border-strong);
  color: var(--insp-ink-tertiary);
  font-size: 11px;
  font-weight: 600;
  font-family: var(--insp-font-mono);
  line-height: 1;
  flex-shrink: 0;
}

.tl-line {
  flex: 1;
  width: 2px;
  background: var(--insp-border-default);
  min-height: 14px;
}

.tl-step.is-past .tl-dot {
  background: var(--insp-pass);
  border-color: var(--insp-pass);
  color: white;
}
.tl-step.is-past + .tl-step .tl-line,
.tl-step.is-past .tl-line {
  background: var(--insp-pass);
}

.tl-step.is-current .tl-dot {
  background: var(--insp-accent);
  border-color: var(--insp-accent);
  color: white;
  box-shadow: 0 0 0 4px var(--insp-accent-paler);
}

.tl-pulse {
  width: 8px;
  height: 8px;
  background: white;
  border-radius: 50%;
  animation: tl-pulse 1.6s ease-in-out infinite;
}
@keyframes tl-pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(0.6); opacity: 0.7; }
}

.tl-step.is-future .tl-dot {
  background: var(--insp-bg-subtle);
  border-color: var(--insp-border-default);
  color: var(--insp-ink-quaternary);
  border-style: dashed;
}
.tl-step.is-future .tl-line {
  background: transparent;
  border-left: 2px dashed var(--insp-border-default);
  width: 0;
}

.tl-step.is-terminal.is-past .tl-dot,
.tl-step.is-terminal.is-current .tl-dot {
  background: var(--insp-fail);
  border-color: var(--insp-fail);
}

.tl-step.is-alternate .tl-dot {
  border-style: dashed;
}

.tl-glyph {
  font-size: 13px;
  line-height: 1;
}

.tl-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-bottom: 6px;
}

.tl-line1 {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.tl-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--insp-ink-primary);
}

.tl-step.is-future .tl-label {
  color: var(--insp-ink-quaternary);
  font-weight: 400;
}

.tl-step.is-current .tl-label {
  color: var(--insp-accent);
}

.tl-time {
  font-family: var(--insp-font-mono);
  font-size: 10px;
  color: var(--insp-ink-tertiary);
}

.tl-hint {
  font-size: 12px;
  color: var(--insp-accent);
  background: var(--insp-accent-paler);
  padding: 4px 10px;
  border-radius: 4px;
  border-left: 2px solid var(--insp-accent);
  margin: 4px 0 2px;
  display: inline-block;
}

.tl-by {
  font-size: 11px;
  color: var(--insp-ink-tertiary);
}
</style>
