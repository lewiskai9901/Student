<template>
  <div class="pfd-root">
    <details class="pfd-details">
      <summary class="pfd-summary">
        <span class="pfd-icon">📊</span>
        <span class="pfd-title">引擎判定流程图</span>
        <span class="pfd-subtitle">点击展开看每道题如何被引擎处理</span>
      </summary>
      <div class="pfd-body">
        <!-- 第 1 步: 项目开关 -->
        <div class="pfd-step" :class="{ 'pfd-step--active': step1Active, 'pfd-step--dead': !step1Active }">
          <div class="pfd-step-num">1</div>
          <div class="pfd-step-content">
            <div class="pfd-step-title">项目引擎开关</div>
            <div class="pfd-step-detail">
              当前: <b>{{ strictnessLabel }}</b>
              <span v-if="!step1Active" class="pfd-dead-label">↓ 引擎关闭, 所有题不建单</span>
              <span v-else class="pfd-active-label">↓ 引擎启用, 进入题目级判定</span>
            </div>
          </div>
        </div>

        <template v-if="step1Active">
          <div class="pfd-arrow">▼ 对每道题独立判定</div>

          <!-- 第 2 步: 题目级 NEVER -->
          <div class="pfd-step pfd-branch">
            <div class="pfd-step-num">2</div>
            <div class="pfd-step-content">
              <div class="pfd-step-title">题目级 "永不建单"?</div>
              <div class="pfd-step-detail">
                <span class="pfd-branch-yes">是 → 此题跳过</span>
                <span class="pfd-branch-no">否 → 下一步</span>
              </div>
            </div>
          </div>

          <div class="pfd-arrow pfd-arrow-light">▼</div>

          <!-- 第 3 步: 题目级 ALWAYS -->
          <div class="pfd-step pfd-branch">
            <div class="pfd-step-num">3</div>
            <div class="pfd-step-content">
              <div class="pfd-step-title">题目级 "任何不达标都建单" (红线)?</div>
              <div class="pfd-step-detail">
                <span class="pfd-branch-yes">是 → 锁严重单</span>
                <span class="pfd-branch-no">否 → 下一步</span>
              </div>
            </div>
          </div>

          <div class="pfd-arrow pfd-arrow-light">▼</div>

          <!-- 第 4 步: 题目级 CUSTOM -->
          <div class="pfd-step pfd-branch">
            <div class="pfd-step-num">4</div>
            <div class="pfd-step-content">
              <div class="pfd-step-title">题目级 "按本题量表配置"?</div>
              <div class="pfd-step-detail">
                <span class="pfd-branch-yes">是 → 按本题阈值判定 (项目阈值不生效)</span>
                <span class="pfd-branch-no">否 → 下一步 (默认/跟随项目)</span>
              </div>
            </div>
          </div>

          <div class="pfd-arrow pfd-arrow-light">▼</div>

          <!-- 第 5 步: 项目阈值 -->
          <div class="pfd-step" :class="{ 'pfd-step--active': true }">
            <div class="pfd-step-num">5</div>
            <div class="pfd-step-content">
              <div class="pfd-step-title">项目阈值切 sev → 严重度</div>
              <div class="pfd-step-detail">
                当前阈值 (sev 0~1): 严重 ≥ <b>{{ thresholds.high }}</b>, 中度 ≥ <b>{{ thresholds.medium }}</b>, 轻微 ≥ <b>{{ thresholds.low }}</b>
                <span class="pfd-hint"> · sev &lt; {{ thresholds.low }} 视为达标, 不建单</span>
              </div>
            </div>
          </div>

          <div class="pfd-arrow pfd-arrow-light">▼</div>

          <!-- 第 6 步: 自动建单门槛 -->
          <div class="pfd-step">
            <div class="pfd-step-num">6</div>
            <div class="pfd-step-content">
              <div class="pfd-step-title">自动建单门槛</div>
              <div class="pfd-step-detail">
                当前: <b>{{ autoCreateLabel }}</b>
                <span class="pfd-hint"> · 门槛之上自动落库, 门槛之下进候选确认</span>
              </div>
            </div>
          </div>

          <div class="pfd-arrow pfd-arrow-light">▼</div>

          <!-- 第 7 步: deadline -->
          <div class="pfd-step">
            <div class="pfd-step-num">7</div>
            <div class="pfd-step-content">
              <div class="pfd-step-title">完成时限</div>
              <div class="pfd-step-detail">
                题目级 override 优先 · 否则项目默认: <b>严重 {{ deadlines.high }}天 / 中度 {{ deadlines.medium }}天 / 轻微 {{ deadlines.low }}天</b>
              </div>
            </div>
          </div>
        </template>
      </div>
    </details>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  strictness: string
  enabled?: boolean
  autoCreateLevel?: string
  thresholdHigh?: number | null
  thresholdMedium?: number | null
  thresholdLow?: number | null
  deadlineHigh?: number | null
  deadlineMedium?: number | null
  deadlineLow?: number | null
}
const props = defineProps<Props>()

const PRESET_DEFAULTS: Record<string, { high: number; medium: number; low: number }> = {
  STRICT:  { high: 0.5, medium: 0.3, low: 0.1 },
  NORMAL:  { high: 0.8, medium: 0.5, low: 0.3 },
  LENIENT: { high: 0.9, medium: 0.7, low: 0.5 },
  OFF:     { high: 0.8, medium: 0.5, low: 0.3 },
}

// 项目级启用 = strictness != OFF (enabled prop 可选, 仅用于显式禁用)
const step1Active = computed(() => props.strictness !== 'OFF')

const strictnessLabel = computed(() => {
  switch (props.strictness) {
    case 'STRICT':  return '严 — 不达标都建单 (引擎启用)'
    case 'NORMAL':  return '标准 — 中等及以上严重度才建 (引擎启用)'
    case 'LENIENT': return '松 — 仅严重问题建议 (引擎启用)'
    case 'OFF':     return '关闭 — 完全人工'
    default:        return props.strictness
  }
})

const thresholds = computed(() => {
  const def = PRESET_DEFAULTS[props.strictness] ?? PRESET_DEFAULTS.NORMAL
  return {
    high: props.thresholdHigh ?? def.high,
    medium: props.thresholdMedium ?? def.medium,
    low: props.thresholdLow ?? def.low,
  }
})

const deadlines = computed(() => ({
  high: props.deadlineHigh ?? 3,
  medium: props.deadlineMedium ?? 7,
  low: props.deadlineLow ?? 14,
}))

const autoCreateLabel = computed(() => {
  switch (props.autoCreateLevel) {
    case 'HIGH':   return '严重 及以上 自动建'
    case 'MEDIUM': return '中度 及以上 自动建'
    case 'LOW':    return '轻微 及以上 自动建 (近似全自动)'
    case 'NONE':
    default:       return '不自动建 (全部进候选确认)'
  }
})
</script>

<style scoped>
.pfd-root {
  margin-bottom: 14px;
  padding: 0;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}
.pfd-details summary {
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
  display: flex; align-items: center; gap: 10px;
  font-size: 13px;
}
.pfd-icon { font-size: 16px; }
.pfd-title { font-weight: 600; color: #1e293b; }
.pfd-subtitle { color: #94a3b8; font-size: 11px; }
.pfd-body {
  padding: 12px 14px 14px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}
.pfd-step {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 10px;
  background: #fafafa;
  border: 1px solid #e5e7eb;
  border-radius: 5px;
}
.pfd-step--active { background: #ecfdf5; border-color: #86efac; }
.pfd-step--dead { background: #fef2f2; border-color: #fecaca; opacity: 0.85; }
.pfd-step-num {
  flex-shrink: 0;
  width: 22px; height: 22px;
  display: flex; align-items: center; justify-content: center;
  background: #1e40af; color: #fff;
  border-radius: 50%;
  font-size: 11px; font-weight: 600;
}
.pfd-step-content { flex: 1; min-width: 0; }
.pfd-step-title { font-size: 13px; font-weight: 500; color: #1f2937; }
.pfd-step-detail { font-size: 11px; color: #6b7280; margin-top: 2px; }
.pfd-step-detail b { color: #1f2937; }
.pfd-active-label { color: #15803d; margin-left: 8px; }
.pfd-dead-label { color: #b91c1c; margin-left: 8px; }
.pfd-branch { background: #fffbeb; border-color: #fcd34d; }
.pfd-branch-yes { color: #b45309; margin-right: 16px; }
.pfd-branch-no { color: #6b7280; }
.pfd-arrow {
  margin: 4px 0;
  text-align: center;
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
}
.pfd-arrow-light { color: #cbd5e1; }
.pfd-hint { color: #94a3b8; font-style: italic; }
</style>
