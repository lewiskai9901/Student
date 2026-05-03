<script setup lang="ts">
/**
 * InspChip — 检查平台标准 chip (A 级)
 *
 * tone: pass / warn / fail / info / pending / accent (默认 default)
 * size: sm (默认) / xs
 *
 * 用法:
 *   <InspChip tone="pass">已发布</InspChip>
 *   <InspChip tone="fail" size="xs">逾期</InspChip>
 *
 *   <!-- 状态机 helper: 自动按 status 字段映射 -->
 *   <InspChip :auto-tone="ProjectStatusConfig[status]?.tone">{{ status }}</InspChip>
 */
import { computed } from 'vue'

interface Props {
  tone?: 'pass' | 'warn' | 'fail' | 'info' | 'pending' | 'accent'
  size?: 'xs' | 'sm'
  /** 圆点 + 文字格式 (左侧 6px 实心圆) */
  dot?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'sm',
})

const cls = computed(() => [
  'insp-chip',
  props.tone && `insp-chip--${props.tone}`,
  props.size === 'xs' && 'insp-chip--xs',
  props.dot && 'insp-chip--dot',
])
</script>

<template>
  <span :class="cls">
    <span v-if="dot" class="ic-dot" />
    <slot />
  </span>
</template>

<style scoped>
.insp-chip--xs {
  height: 18px;
  padding: 0 var(--insp-sp-1);
  font-size: 10px;
  border-radius: var(--insp-radius-sm);
}
.ic-dot {
  width: 5px; height: 5px;
  border-radius: var(--insp-radius-pill);
  background: currentColor;
  flex-shrink: 0;
}
</style>
