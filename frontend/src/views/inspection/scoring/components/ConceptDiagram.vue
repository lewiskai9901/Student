<template>
  <div class="cd-root" :class="{ 'cd-root--open': !collapsed }">
    <!-- 折叠态 / 展开态共用头 -->
    <button class="cd-head" @click="toggle">
      <span class="cd-head-title">评分是怎么算出来的?</span>
      <span class="cd-head-meta">本章 3 步 + 项目合并 · 点击{{ collapsed ? '展开' : '收起' }}</span>
      <ChevronDown :size="14" class="cd-head-arrow" :class="{ flip: !collapsed }" />
    </button>

    <!-- 展开态: 流程图 + 算例 -->
    <div v-if="!collapsed" class="cd-body">
      <div class="cd-flow">
        <button
          v-for="(step, i) in steps"
          :key="step.id || step.name"
          class="cd-step"
          :class="{
            'cd-step--result': step.isResult,
            'cd-step--external': step.isExternal,
          }"
          :title="step.isExternal ? '跳转到「评级」Tab 查看跨章节合并配置' : undefined"
          @click="handleStepClick(step)"
        >
          <span class="cd-step-name">{{ step.name }}</span>
          <span class="cd-step-value">{{ step.value }}</span>
          <span class="cd-step-sub">{{ step.sub }}</span>
          <span v-if="i < steps.length - 1" class="cd-arrow" aria-hidden>
            {{ stepsArrowChar(i) }}
          </span>
        </button>
      </div>

      <p class="cd-eg">
        <span class="cd-eg-lbl">例:</span>
        学生小张原始 <b>95</b> 分 → 4 个维度加权 <b>92</b> 分
        → 规则链触发"3 项不合格扣 3 分" = <b>本章节 89 分</b>
        → 与其他章节合并(由「评级」Tab 的 Indicator 配置)→ <b>项目评级</b>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronDown } from 'lucide-vue-next'
import type { LongId } from '@/types/common'

const props = defineProps<{
  /** L4 2026-05-26: 用于"项目评级"步骤跳转到「评级」Tab. 单 section 编辑时可空 */
  projectId?: LongId
}>()

const router = useRouter()

const STORAGE_KEY = 'scoring-concept-collapsed'

const collapsed = ref(true)

onMounted(() => {
  const stored = localStorage.getItem(STORAGE_KEY)
  collapsed.value = stored === null ? true : stored === '1'
})

function toggle() {
  collapsed.value = !collapsed.value
  localStorage.setItem(STORAGE_KEY, collapsed.value ? '1' : '0')
}

interface Step {
  id: string
  name: string
  value: string
  sub: string
  isResult?: boolean
  /** L4: 项目级步骤, 点击跳到项目「评级」Tab */
  isExternal?: boolean
}

const steps: Step[] = [
  { id: 'sp-anchor-raw',   name: '原始打分', value: '100/100', sub: '评分员录入' },
  { id: 'sp-anchor-dims',  name: '维度汇总', value: '按权重',  sub: '评分维度' },
  { id: 'sp-anchor-rules', name: '规则链',   value: '加减分',  sub: '计算规则链' },
  { id: '',                name: '本章节得分', value: '89',      sub: '本章配置输出', isResult: true },
  { id: '_rating',         name: '项目评级',   value: '→「评级」', sub: '跨章合并 + Indicator', isExternal: true },
]

function stepsArrowChar(i: number): string {
  // index 2 -> '=' (规则链算出本章节得分, 求和语义), 其余 '→'
  return i === 2 ? '=' : '→'
}

function handleStepClick(step: Step) {
  if (step.isExternal) {
    if (!props.projectId) return
    router.push(`/inspection/projects/${props.projectId}?tab=evaluation`)
    return
  }
  if (!step.id || step.id.startsWith('_')) return
  scrollTo(step.id)
}

function scrollTo(id: string) {
  const el = document.getElementById(id)
  if (!el) return
  el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  el.classList.add('cd-anchor-flash')
  window.setTimeout(() => el.classList.remove('cd-anchor-flash'), 1200)
}
</script>

<style scoped>
.cd-root {
  border: 1px solid var(--insp-border-subtle, #e5e7eb);
  border-radius: 6px;
  background: var(--insp-bg-surface, #fff);
  margin-bottom: 12px;
  overflow: hidden;
}
.cd-root--open {
  background: var(--insp-bg-subtle, #fafbfc);
}

.cd-head {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 14px;
  background: transparent;
  border: 0;
  cursor: pointer;
  font: inherit;
  text-align: left;
  color: var(--insp-ink-primary, #111827);
}
.cd-head:hover {
  background: var(--insp-bg-subtle-hover, #f3f4f6);
}
.cd-head-title { font-size: 13px; font-weight: 600; }
.cd-head-meta {
  font-size: 12px;
  color: var(--insp-ink-tertiary, #6b7280);
  font-weight: 400;
}
.cd-head-arrow {
  margin-left: auto;
  color: var(--insp-ink-tertiary, #6b7280);
  transition: transform 0.15s;
}
.cd-head-arrow.flip { transform: rotate(180deg); }

.cd-body {
  padding: 14px 16px 16px;
  border-top: 1px solid var(--insp-border-subtle, #e5e7eb);
}

.cd-flow {
  display: flex;
  align-items: stretch;
  gap: 8px;
  flex-wrap: wrap;
}
.cd-step {
  position: relative;
  flex: 1 1 0;
  min-width: 110px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 8px 14px 8px 10px;
  background: var(--insp-bg-surface, #fff);
  border: 1px solid var(--insp-border-subtle, #e5e7eb);
  border-radius: 5px;
  cursor: pointer;
  font: inherit;
  text-align: left;
  transition: border-color 0.15s, background 0.15s;
}
.cd-step:not(.cd-step--result):not(.cd-step--external):hover {
  border-color: var(--insp-accent, #2563eb);
  background: var(--insp-accent-tint, #eff6ff);
}
.cd-step--result {
  cursor: default;
  border-color: var(--insp-accent, #2563eb);
  background: var(--insp-accent-tint, #eff6ff);
}
/* L4: 外部步骤 (跳「评级」Tab), 虚线表示跨编辑器边界 */
.cd-step--external {
  border-style: dashed;
  border-color: var(--insp-ink-tertiary, #6b7280);
  background: var(--insp-bg-surface, #fff);
  color: var(--insp-ink-secondary, #374151);
}
.cd-step--external:hover {
  border-style: solid;
  border-color: var(--insp-accent, #2563eb);
  background: var(--insp-accent-tint, #eff6ff);
}
.cd-step-name {
  font-size: 12px;
  color: var(--insp-ink-tertiary, #6b7280);
  font-weight: 500;
}
.cd-step-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--insp-ink-primary, #111827);
}
.cd-step-sub {
  font-size: 11px;
  color: var(--insp-ink-quaternary, #9ca3af);
}
.cd-arrow {
  position: absolute;
  right: -7px;
  top: 50%;
  transform: translate(50%, -50%);
  width: 14px;
  height: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--insp-bg-subtle, #fafbfc);
  color: var(--insp-ink-tertiary, #6b7280);
  font-size: 12px;
  font-weight: 600;
  z-index: 1;
}

.cd-eg {
  margin: 12px 0 0;
  padding: 8px 10px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--insp-ink-secondary, #374151);
  background: var(--insp-bg-surface, #fff);
  border: 1px solid var(--insp-border-subtle, #e5e7eb);
  border-radius: 5px;
}
.cd-eg-lbl {
  font-weight: 600;
  color: var(--insp-ink-primary, #111827);
  margin-right: 4px;
}
.cd-eg b {
  color: var(--insp-ink-primary, #111827);
  font-weight: 600;
}
</style>

<style>
/* 全局: 锚点闪烁 (scoped 选不到外部 section id) */
.cd-anchor-flash {
  outline: 2px solid var(--insp-accent, #2563eb);
  outline-offset: 3px;
  border-radius: 8px;
  transition: outline 0.2s;
}
</style>
