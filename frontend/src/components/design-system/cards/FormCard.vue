<template>
  <div :class="['form-card', { 'form-card--glassmorphism': glassmorphism, 'form-card--loading': loading }]">
    <!-- 背景装饰 -->
    <div v-if="!glassmorphism" class="form-card__decoration">
      <div class="form-card__pattern"></div>
    </div>

    <!-- 头部 -->
    <div v-if="title || $slots.header" class="form-card__header">
      <slot name="header">
        <div class="form-card__title-wrapper">
          <div v-if="icon" class="form-card__icon-wrapper">
            <component :is="icon" class="form-card__icon" />
          </div>
          <div>
            <h2 class="form-card__title">{{ title }}</h2>
            <p v-if="description" class="form-card__description">{{ description }}</p>
          </div>
        </div>
      </slot>
    </div>

    <!-- 内容 -->
    <div class="form-card__content">
      <!-- 加载遮罩 -->
      <div v-if="loading" class="form-card__loading-overlay">
        <LoadingState />
      </div>

      <slot></slot>
    </div>

    <!-- 底部操作栏 -->
    <div v-if="$slots.actions" class="form-card__actions">
      <slot name="actions"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import LoadingState from '../feedback/LoadingState.vue'
import type { Component } from 'vue'

interface Props {
  title?: string
  description?: string
  icon?: Component
  loading?: boolean
  glassmorphism?: boolean
}

withDefaults(defineProps<Props>(), {
  loading: false,
  glassmorphism: false
})
</script>

<style scoped>
/* ============================================
   基础容器
   ============================================ */
.form-card {
  position: relative;
  background: white;
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-xl);
  overflow: hidden;
  isolation: isolate;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-base);
}

.form-card:hover {
  box-shadow: var(--shadow-md);
}

/* ============================================
   毛玻璃变体
   ============================================ */
.form-card--glassmorphism {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

/* ============================================
   背景装饰
   ============================================ */
.form-card__decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

.form-card__pattern {
  position: absolute;
  inset: -50%;
  background-image: var(--pattern-grid);
  opacity: 0.4;
  animation: pattern-drift 30s linear infinite;
}

@keyframes pattern-drift {
  from {
    transform: translate(0, 0);
  }
  to {
    transform: translate(40px, 40px);
  }
}

/* ============================================
   头部
   ============================================ */
.form-card__header {
  position: relative;
  z-index: 1;
  padding: 1.5rem 1.75rem;
  border-bottom: 1px solid var(--color-gray-100);
  background: linear-gradient(180deg, rgba(249, 250, 251, 0.8) 0%, rgba(255, 255, 255, 0.8) 100%);
}

.form-card--glassmorphism .form-card__header {
  background: rgba(249, 250, 251, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.form-card__title-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
}

.form-card__icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.75rem;
  height: 2.75rem;
  border-radius: var(--radius-lg);
  background: var(--gradient-primary);
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
}

.form-card__icon {
  width: 1.375rem;
  height: 1.375rem;
  color: white;
}

.form-card__title {
  font-family: var(--font-display);
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-gray-900);
  margin: 0;
  letter-spacing: -0.02em;
}

.form-card__description {
  font-size: 0.875rem;
  color: var(--color-gray-600);
  margin: 0.375rem 0 0;
  line-height: 1.5;
}

/* ============================================
   内容区域
   ============================================ */
.form-card__content {
  position: relative;
  z-index: 1;
  padding: 1.75rem;
}

.form-card--loading .form-card__content {
  min-height: 200px;
}

.form-card__loading-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  z-index: 10;
  animation: overlay-appear 200ms ease-out;
}

@keyframes overlay-appear {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* ============================================
   底部操作栏
   ============================================ */
.form-card__actions {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1.25rem 1.75rem;
  background: var(--color-gray-50);
  border-top: 1px solid var(--color-gray-100);
}

.form-card--glassmorphism .form-card__actions {
  background: rgba(249, 250, 251, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

/* ============================================
   响应式
   ============================================ */
@media (max-width: 768px) {
  .form-card__header {
    padding: 1.25rem 1.25rem;
  }

  .form-card__content {
    padding: 1.5rem 1.25rem;
  }

  .form-card__actions {
    padding: 1rem 1.25rem;
    flex-direction: column;
    align-items: stretch;
  }

  .form-card__title {
    font-size: 1rem;
  }

  .form-card__icon-wrapper {
    width: 2.5rem;
    height: 2.5rem;
  }

  .form-card__icon {
    width: 1.25rem;
    height: 1.25rem;
  }
}
</style>
