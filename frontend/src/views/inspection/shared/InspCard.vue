<script setup lang="ts">
/**
 * InspCard — 检查平台标准卡片 (A 级)
 *
 * 用法 (无 padding 容器, 内部自己控制):
 *   <InspCard>
 *     <header>...</header>
 *     <main>...</main>
 *   </InspCard>
 *
 * 带 padding 简单卡:
 *   <InspCard padded>{{ content }}</InspCard>
 *
 * hover 高亮:
 *   <InspCard hover @click="goDetail">{{ ... }}</InspCard>
 */
import { computed } from 'vue'

interface Props {
  padded?: boolean
  hover?: boolean
  /** 顶部彩色边: pass / warn / fail / accent */
  topAccent?: 'pass' | 'warn' | 'fail' | 'accent'
}

const props = defineProps<Props>()

const cls = computed(() => [
  'insp-card',
  props.padded && 'insp-card--padded',
  props.hover && 'insp-card--hover',
  props.topAccent && `insp-card--top-${props.topAccent}`,
])
</script>

<template>
  <div :class="cls">
    <slot />
  </div>
</template>

<style scoped>
.insp-card--top-pass { border-top: 3px solid var(--insp-pass); }
.insp-card--top-warn { border-top: 3px solid var(--insp-warn); }
.insp-card--top-fail { border-top: 3px solid var(--insp-fail); }
.insp-card--top-accent { border-top: 3px solid var(--insp-accent); }
</style>
