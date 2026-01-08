<template>
  <button
    :class="[
      'gradient-button',
      `gradient-button--${variant}`,
      `gradient-button--${size}`,
      {
        'gradient-button--loading': loading,
        'gradient-button--disabled': disabled,
        'gradient-button--icon-only': isIconOnly
      }
    ]"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <!-- 发光背景层 -->
    <div class="gradient-button__glow"></div>

    <!-- 渐变背景层 -->
    <div class="gradient-button__gradient"></div>

    <!-- 内容层 -->
    <span class="gradient-button__content">
      <!-- 加载图标 -->
      <span v-if="loading" class="gradient-button__loading-icon">
        <Loader2 class="animate-spin" />
      </span>

      <!-- 左侧图标 -->
      <component
        v-if="icon && iconPosition === 'left' && !loading"
        :is="icon"
        class="gradient-button__icon gradient-button__icon--left"
      />

      <!-- 文本内容 -->
      <span v-if="!isIconOnly" class="gradient-button__text">
        <slot></slot>
      </span>

      <!-- 右侧图标 -->
      <component
        v-if="icon && iconPosition === 'right' && !loading"
        :is="icon"
        class="gradient-button__icon gradient-button__icon--right"
      />

      <!-- 仅图标模式 -->
      <component
        v-if="isIconOnly && !loading"
        :is="icon"
        class="gradient-button__icon"
      />
    </span>

    <!-- 波纹效果容器 -->
    <span ref="rippleContainer" class="gradient-button__ripple-container"></span>
  </button>
</template>

<script setup lang="ts">
import { computed, ref, useSlots } from 'vue'
import { Loader2 } from 'lucide-vue-next'
import type { Component } from 'vue'

interface Props {
  variant?: 'primary' | 'success' | 'danger' | 'warning' | 'outline'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  disabled?: boolean
  icon?: Component
  iconPosition?: 'left' | 'right'
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md',
  loading: false,
  disabled: false,
  iconPosition: 'left'
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const slots = useSlots()
const rippleContainer = ref<HTMLElement | null>(null)

// 是否仅图标模式
const isIconOnly = computed(() => {
  return props.icon && !slots.default
})

// 点击处理
const handleClick = (event: MouseEvent) => {
  if (props.disabled || props.loading) return

  // 创建波纹效果
  if (rippleContainer.value) {
    const ripple = document.createElement('span')
    ripple.className = 'gradient-button__ripple'

    const rect = rippleContainer.value.getBoundingClientRect()
    const size = Math.max(rect.width, rect.height)
    const x = event.clientX - rect.left - size / 2
    const y = event.clientY - rect.top - size / 2

    ripple.style.width = ripple.style.height = `${size}px`
    ripple.style.left = `${x}px`
    ripple.style.top = `${y}px`

    rippleContainer.value.appendChild(ripple)

    setTimeout(() => {
      ripple.remove()
    }, 600)
  }

  emit('click', event)
}
</script>

<style scoped>
/* ============================================
   基础按钮样式
   ============================================ */
.gradient-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-weight: 600;
  border: none;
  border-radius: var(--radius-lg);
  cursor: pointer;
  overflow: hidden;
  transition: all var(--transition-base);
  isolation: isolate;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
}

.gradient-button:focus-visible {
  outline: 2px solid var(--color-primary-500);
  outline-offset: 2px;
}

/* ============================================
   尺寸变体
   ============================================ */
.gradient-button--sm {
  height: 2rem;
  padding: 0 0.875rem;
  font-size: 0.875rem;
  gap: 0.375rem;
}

.gradient-button--sm.gradient-button--icon-only {
  width: 2rem;
  padding: 0;
}

.gradient-button--md {
  height: 2.5rem;
  padding: 0 1.25rem;
  font-size: 0.9375rem;
  gap: 0.5rem;
}

.gradient-button--md.gradient-button--icon-only {
  width: 2.5rem;
  padding: 0;
}

.gradient-button--lg {
  height: 3rem;
  padding: 0 1.75rem;
  font-size: 1rem;
  gap: 0.625rem;
}

.gradient-button--lg.gradient-button--icon-only {
  width: 3rem;
  padding: 0;
}

/* ============================================
   发光背景层
   ============================================ */
.gradient-button__glow {
  position: absolute;
  inset: -4px;
  border-radius: var(--radius-lg);
  opacity: 0;
  transition: opacity var(--transition-base);
  z-index: 0;
  pointer-events: none;
}

.gradient-button:hover:not(.gradient-button--disabled):not(.gradient-button--loading) .gradient-button__glow {
  opacity: 1;
}

.gradient-button--primary .gradient-button__glow {
  box-shadow: 0 0 24px rgba(37, 99, 235, 0.6);
}

.gradient-button--success .gradient-button__glow {
  box-shadow: 0 0 24px rgba(16, 185, 129, 0.6);
}

.gradient-button--danger .gradient-button__glow {
  box-shadow: 0 0 24px rgba(239, 68, 68, 0.6);
}

.gradient-button--warning .gradient-button__glow {
  box-shadow: 0 0 24px rgba(245, 158, 11, 0.6);
}

.gradient-button--outline .gradient-button__glow {
  box-shadow: 0 0 24px rgba(37, 99, 235, 0.3);
}

/* ============================================
   渐变背景层
   ============================================ */
.gradient-button__gradient {
  position: absolute;
  inset: 0;
  border-radius: var(--radius-lg);
  transition: all var(--transition-base);
  z-index: 1;
  pointer-events: none;
}

/* Primary 变体 */
.gradient-button--primary .gradient-button__gradient {
  background: linear-gradient(135deg, #2563eb 0%, #4f46e5 50%, #6366f1 100%);
  background-size: 200% 200%;
  background-position: 0% 50%;
}

.gradient-button--primary:hover:not(.gradient-button--disabled):not(.gradient-button--loading) .gradient-button__gradient {
  background-position: 100% 50%;
  filter: brightness(1.1);
}

/* Success 变体 */
.gradient-button--success .gradient-button__gradient {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  background-size: 200% 200%;
  background-position: 0% 50%;
}

.gradient-button--success:hover:not(.gradient-button--disabled):not(.gradient-button--loading) .gradient-button__gradient {
  background-position: 100% 50%;
  filter: brightness(1.1);
}

/* Danger 变体 */
.gradient-button--danger .gradient-button__gradient {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  background-size: 200% 200%;
  background-position: 0% 50%;
}

.gradient-button--danger:hover:not(.gradient-button--disabled):not(.gradient-button--loading) .gradient-button__gradient {
  background-position: 100% 50%;
  filter: brightness(1.1);
}

/* Warning 变体 */
.gradient-button--warning .gradient-button__gradient {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  background-size: 200% 200%;
  background-position: 0% 50%;
}

.gradient-button--warning:hover:not(.gradient-button--disabled):not(.gradient-button--loading) .gradient-button__gradient {
  background-position: 100% 50%;
  filter: brightness(1.1);
}

/* Outline 变体 */
.gradient-button--outline .gradient-button__gradient {
  background: white;
  border: 2px solid transparent;
  background-clip: padding-box;
}

.gradient-button--outline::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: var(--radius-lg);
  padding: 2px;
  background: linear-gradient(135deg, #2563eb, #6366f1);
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  z-index: 1;
  pointer-events: none;
}

.gradient-button--outline:hover:not(.gradient-button--disabled):not(.gradient-button--loading) .gradient-button__gradient {
  background: rgba(37, 99, 235, 0.05);
}

/* ============================================
   内容层
   ============================================ */
.gradient-button__content {
  position: relative;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: inherit;
  pointer-events: none;
  transition: transform var(--transition-base);
}

.gradient-button:hover:not(.gradient-button--disabled):not(.gradient-button--loading) .gradient-button__content {
  transform: scale(1.02);
}

.gradient-button:active:not(.gradient-button--disabled):not(.gradient-button--loading) .gradient-button__content {
  transform: scale(0.98);
}

/* 文本 */
.gradient-button__text {
  color: white;
  line-height: 1;
}

.gradient-button--outline .gradient-button__text {
  background: linear-gradient(135deg, #2563eb, #6366f1);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  font-weight: 700;
}

/* 图标 */
.gradient-button__icon {
  flex-shrink: 0;
  transition: transform var(--transition-base);
}

.gradient-button--sm .gradient-button__icon {
  width: 1rem;
  height: 1rem;
}

.gradient-button--md .gradient-button__icon {
  width: 1.125rem;
  height: 1.125rem;
}

.gradient-button--lg .gradient-button__icon {
  width: 1.25rem;
  height: 1.25rem;
}

.gradient-button:not(.gradient-button--outline) .gradient-button__icon {
  color: white;
}

.gradient-button--outline .gradient-button__icon {
  color: var(--color-primary-600);
}

.gradient-button:hover:not(.gradient-button--disabled):not(.gradient-button--loading) .gradient-button__icon {
  transform: scale(1.1);
}

/* 加载图标 */
.gradient-button__loading-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.gradient-button:not(.gradient-button--outline) .gradient-button__loading-icon {
  color: white;
}

.gradient-button--outline .gradient-button__loading-icon {
  color: var(--color-primary-600);
}

.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* ============================================
   状态变体
   ============================================ */
/* 禁用状态 */
.gradient-button--disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

/* 加载状态 */
.gradient-button--loading {
  cursor: wait;
}

/* ============================================
   波纹效果
   ============================================ */
.gradient-button__ripple-container {
  position: absolute;
  inset: 0;
  border-radius: var(--radius-lg);
  overflow: hidden;
  pointer-events: none;
  z-index: 3;
}

.gradient-button__ripple {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  transform: scale(0);
  animation: ripple-expand 600ms ease-out;
  pointer-events: none;
}

.gradient-button--outline .gradient-button__ripple {
  background: rgba(37, 99, 235, 0.2);
}

@keyframes ripple-expand {
  to {
    transform: scale(2.5);
    opacity: 0;
  }
}

/* ============================================
   交互效果
   ============================================ */
.gradient-button:hover:not(.gradient-button--disabled):not(.gradient-button--loading) {
  transform: translateY(-1px);
  box-shadow: var(--shadow-lg);
}

.gradient-button:active:not(.gradient-button--disabled):not(.gradient-button--loading) {
  transform: translateY(0);
  box-shadow: var(--shadow-sm);
}

/* ============================================
   响应式
   ============================================ */
@media (max-width: 640px) {
  .gradient-button--lg {
    height: 2.75rem;
    padding: 0 1.5rem;
  }

  .gradient-button--md {
    height: 2.375rem;
    padding: 0 1.125rem;
  }
}
</style>
