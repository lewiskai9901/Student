<script setup lang="ts">
/**
 * InspSpinner — 统一加载指示
 *
 * 替代 element-plus v-loading (默认蓝圈和检查平台不搭).
 *
 * 用法:
 *   <InspSpinner v-if="loading" />
 *   <InspSpinner inline size="sm" />
 *   <InspSpinner :overlay="true" /> <!-- 整页遮罩 -->
 */
interface Props {
  size?: 'sm' | 'md' | 'lg'
  inline?: boolean
  /** 全屏遮罩 (绝对定位覆盖父容器) */
  overlay?: boolean
  label?: string
}

withDefaults(defineProps<Props>(), {
  size: 'md',
})
</script>

<template>
  <div class="ispn" :class="[`ispn--${size}`, { 'ispn--inline': inline, 'ispn--overlay': overlay }]">
    <span class="ispn-dot ispn-dot--1" />
    <span class="ispn-dot ispn-dot--2" />
    <span class="ispn-dot ispn-dot--3" />
    <span v-if="label" class="ispn-label">{{ label }}</span>
  </div>
</template>

<style scoped>
.ispn {
  display: inline-flex;
  align-items: center;
  gap: var(--insp-sp-2);
}
.ispn:not(.ispn--inline):not(.ispn--overlay) {
  padding: var(--insp-sp-7) 0;
  width: 100%;
  justify-content: center;
}
.ispn--overlay {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(2px);
  justify-content: center;
  z-index: var(--insp-z-overlay);
}

.ispn-dot {
  display: inline-block;
  background: var(--insp-accent);
  border-radius: var(--insp-radius-pill);
  animation: ispn-bounce 1200ms infinite ease-in-out;
}
.ispn--sm .ispn-dot { width: 4px; height: 4px; }
.ispn--md .ispn-dot { width: 6px; height: 6px; }
.ispn--lg .ispn-dot { width: 8px; height: 8px; }

.ispn-dot--1 { animation-delay: 0ms; }
.ispn-dot--2 { animation-delay: 160ms; }
.ispn-dot--3 { animation-delay: 320ms; }

.ispn-label {
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-tertiary);
  margin-left: var(--insp-sp-2);
}

@keyframes ispn-bounce {
  0%, 80%, 100% { transform: scale(0.7); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}
</style>
