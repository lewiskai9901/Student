<template>
  <div class="empty-state">
    <!-- 背景装饰 -->
    <div class="empty-state__decoration">
      <div class="empty-state__circle empty-state__circle--1"></div>
      <div class="empty-state__circle empty-state__circle--2"></div>
      <div class="empty-state__circle empty-state__circle--3"></div>
    </div>

    <!-- 图标容器 -->
    <div class="empty-state__icon-wrapper">
      <div class="empty-state__icon-bg"></div>
      <component
        v-if="icon"
        :is="icon"
        class="empty-state__icon"
      />
      <div class="empty-state__icon-particles">
        <div class="empty-state__particle" v-for="i in 3" :key="i"></div>
      </div>
    </div>

    <!-- 内容 -->
    <div class="empty-state__content">
      <h3 class="empty-state__title">{{ title }}</h3>
      <p v-if="description" class="empty-state__description">{{ description }}</p>
    </div>

    <!-- 操作按钮 -->
    <div v-if="actionText || $slots.action" class="empty-state__actions">
      <slot name="action">
        <GradientButton
          v-if="actionText"
          @click="handleAction"
          :icon="actionIcon"
        >
          {{ actionText }}
        </GradientButton>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Plus } from 'lucide-vue-next'
import GradientButton from '../buttons/GradientButton.vue'
import type { Component } from 'vue'

interface Props {
  icon?: Component
  title: string
  description?: string
  actionText?: string
  actionIcon?: Component
}

const props = withDefaults(defineProps<Props>(), {
  actionIcon: Plus
})

const emit = defineEmits<{
  action: []
}>()

const handleAction = () => {
  emit('action')
}
</script>

<style scoped>
/* ============================================
   基础容器
   ============================================ */
.empty-state {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  text-align: center;
  isolation: isolate;
}

/* ============================================
   背景装饰
   ============================================ */
.empty-state__decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

.empty-state__circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.03;
}

.empty-state__circle--1 {
  top: 10%;
  left: 10%;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, var(--color-primary-400), transparent);
  animation: float-1 8s ease-in-out infinite;
}

.empty-state__circle--2 {
  top: 50%;
  right: 15%;
  width: 150px;
  height: 150px;
  background: radial-gradient(circle, var(--color-accent-400), transparent);
  animation: float-2 10s ease-in-out infinite;
}

.empty-state__circle--3 {
  bottom: 20%;
  left: 20%;
  width: 100px;
  height: 100px;
  background: radial-gradient(circle, var(--color-secondary-400), transparent);
  animation: float-3 12s ease-in-out infinite;
}

@keyframes float-1 {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(20px, -20px) scale(1.1);
  }
}

@keyframes float-2 {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(-15px, 15px) scale(0.9);
  }
}

@keyframes float-3 {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(10px, 10px) scale(1.05);
  }
}

/* ============================================
   图标容器
   ============================================ */
.empty-state__icon-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
  margin-bottom: 2rem;
  z-index: 1;
  animation: icon-appear 600ms cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

@keyframes icon-appear {
  from {
    opacity: 0;
    transform: scale(0.8) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.empty-state__icon-bg {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-50), var(--color-accent-50));
  border: 2px solid var(--color-gray-100);
  transition: all var(--transition-base);
}

.empty-state:hover .empty-state__icon-bg {
  transform: scale(1.05);
  border-color: var(--color-primary-200);
  box-shadow: 0 0 0 8px rgba(37, 99, 235, 0.05);
}

.empty-state__icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 48px;
  height: 48px;
  color: var(--color-gray-400);
  transition: all var(--transition-base);
}

.empty-state:hover .empty-state__icon {
  color: var(--color-primary-500);
  transform: translate(-50%, -50%) scale(1.1);
}

/* 图标粒子 */
.empty-state__icon-particles {
  position: absolute;
  inset: -10px;
  pointer-events: none;
}

.empty-state__particle {
  position: absolute;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--color-primary-300);
  opacity: 0;
  transition: all var(--transition-base);
}

.empty-state:hover .empty-state__particle {
  opacity: 0.6;
}

.empty-state__particle:nth-child(1) {
  top: 20%;
  right: 10%;
  animation: particle-drift-1 3s ease-in-out infinite;
}

.empty-state__particle:nth-child(2) {
  bottom: 25%;
  left: 15%;
  animation: particle-drift-2 4s ease-in-out infinite;
}

.empty-state__particle:nth-child(3) {
  top: 50%;
  right: 5%;
  animation: particle-drift-3 3.5s ease-in-out infinite;
}

@keyframes particle-drift-1 {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(-5px, -8px);
  }
}

@keyframes particle-drift-2 {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(8px, 5px);
  }
}

@keyframes particle-drift-3 {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(-6px, 6px);
  }
}

/* ============================================
   内容区域
   ============================================ */
.empty-state__content {
  position: relative;
  z-index: 1;
  max-width: 400px;
  animation: content-appear 600ms cubic-bezier(0.68, -0.55, 0.265, 1.55) 100ms backwards;
}

@keyframes content-appear {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.empty-state__title {
  font-family: var(--font-display);
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-gray-900);
  margin: 0 0 0.5rem;
  letter-spacing: -0.01em;
}

.empty-state__description {
  font-size: 0.9375rem;
  color: var(--color-gray-500);
  line-height: 1.6;
  margin: 0;
}

/* ============================================
   操作按钮
   ============================================ */
.empty-state__actions {
  position: relative;
  z-index: 1;
  margin-top: 2rem;
  animation: actions-appear 600ms cubic-bezier(0.68, -0.55, 0.265, 1.55) 200ms backwards;
}

@keyframes actions-appear {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ============================================
   响应式
   ============================================ */
@media (max-width: 640px) {
  .empty-state {
    padding: 3rem 1.5rem;
  }

  .empty-state__icon-wrapper {
    width: 96px;
    height: 96px;
    margin-bottom: 1.5rem;
  }

  .empty-state__icon {
    width: 40px;
    height: 40px;
  }

  .empty-state__title {
    font-size: 1.125rem;
  }

  .empty-state__description {
    font-size: 0.875rem;
  }

  .empty-state__circle--1 {
    width: 150px;
    height: 150px;
  }

  .empty-state__circle--2 {
    width: 100px;
    height: 100px;
  }

  .empty-state__circle--3 {
    width: 80px;
    height: 80px;
  }
}
</style>
