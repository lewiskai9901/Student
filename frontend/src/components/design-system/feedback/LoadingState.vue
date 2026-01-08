<template>
  <div
    :class="[
      'loading-state',
      {
        'loading-state--fullscreen': fullscreen,
        'loading-state--overlay': overlay
      }
    ]"
  >
    <!-- 背景遮罩 (仅全屏模式) -->
    <div v-if="fullscreen" class="loading-state__backdrop"></div>

    <!-- 加载内容 -->
    <div class="loading-state__content">
      <!-- 精密仪器加载动画 -->
      <div class="loading-state__spinner">
        <!-- 外圈 -->
        <div class="loading-state__ring loading-state__ring--outer"></div>
        <!-- 中圈 -->
        <div class="loading-state__ring loading-state__ring--middle"></div>
        <!-- 内圈 -->
        <div class="loading-state__ring loading-state__ring--inner"></div>
        <!-- 中心点 -->
        <div class="loading-state__center-dot"></div>

        <!-- 数据粒子 -->
        <div class="loading-state__particles">
          <div class="loading-state__particle" v-for="i in 8" :key="i" :style="{ '--index': i }"></div>
        </div>
      </div>

      <!-- 加载文字 -->
      <p v-if="text" class="loading-state__text">{{ text }}</p>

      <!-- 进度条 (可选) -->
      <div v-if="showProgress && progress !== undefined" class="loading-state__progress">
        <div class="loading-state__progress-bar">
          <div class="loading-state__progress-fill" :style="{ width: `${progress}%` }"></div>
        </div>
        <span class="loading-state__progress-text">{{ progress }}%</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  fullscreen?: boolean
  text?: string
  overlay?: boolean
  progress?: number
  showProgress?: boolean
}

withDefaults(defineProps<Props>(), {
  fullscreen: false,
  overlay: true,
  showProgress: false
})
</script>

<style scoped>
/* ============================================
   基础容器
   ============================================ */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}

.loading-state--fullscreen {
  position: fixed;
  inset: 0;
  z-index: var(--z-modal);
  padding: 0;
}

/* ============================================
   背景遮罩
   ============================================ */
.loading-state__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  animation: fade-in 200ms ease-out;
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* ============================================
   加载内容
   ============================================ */
.loading-state__content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
}

/* ============================================
   精密仪器加载动画
   ============================================ */
.loading-state__spinner {
  position: relative;
  width: 80px;
  height: 80px;
}

/* 旋转圆环 */
.loading-state__ring {
  position: absolute;
  border-radius: 50%;
  border: 2px solid transparent;
}

.loading-state__ring--outer {
  inset: 0;
  border-top-color: var(--color-primary-500);
  border-right-color: var(--color-primary-400);
  animation: spin-outer 1.5s cubic-bezier(0.68, -0.55, 0.265, 1.55) infinite;
}

.loading-state__ring--middle {
  inset: 12px;
  border-top-color: var(--color-accent-500);
  border-left-color: var(--color-accent-400);
  animation: spin-middle 1.2s cubic-bezier(0.68, -0.55, 0.265, 1.55) infinite reverse;
}

.loading-state__ring--inner {
  inset: 24px;
  border-top-color: var(--color-secondary-500);
  border-bottom-color: var(--color-secondary-400);
  animation: spin-inner 1s cubic-bezier(0.68, -0.55, 0.265, 1.55) infinite;
}

@keyframes spin-outer {
  0% {
    transform: rotate(0deg);
    border-width: 2px;
  }
  50% {
    transform: rotate(180deg);
    border-width: 3px;
  }
  100% {
    transform: rotate(360deg);
    border-width: 2px;
  }
}

@keyframes spin-middle {
  0% {
    transform: rotate(0deg);
    opacity: 0.8;
  }
  50% {
    transform: rotate(180deg);
    opacity: 1;
  }
  100% {
    transform: rotate(360deg);
    opacity: 0.8;
  }
}

@keyframes spin-inner {
  0% {
    transform: rotate(0deg) scale(1);
  }
  50% {
    transform: rotate(180deg) scale(0.9);
  }
  100% {
    transform: rotate(360deg) scale(1);
  }
}

/* 中心点 */
.loading-state__center-dot {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 8px;
  height: 8px;
  margin: -4px 0 0 -4px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-500), var(--color-accent-500));
  box-shadow: 0 0 12px rgba(37, 99, 235, 0.5);
  animation: pulse-dot 1.5s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.3);
    opacity: 0.8;
  }
}

/* ============================================
   数据粒子
   ============================================ */
.loading-state__particles {
  position: absolute;
  inset: -10px;
}

.loading-state__particle {
  position: absolute;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--color-primary-400);
  top: 50%;
  left: 50%;
  animation: orbit 3s linear infinite;
  animation-delay: calc(var(--index) * -0.375s);
}

@keyframes orbit {
  0% {
    transform: rotate(0deg) translateX(45px) scale(0);
    opacity: 0;
  }
  10% {
    opacity: 1;
    transform: rotate(36deg) translateX(45px) scale(1);
  }
  90% {
    opacity: 1;
    transform: rotate(324deg) translateX(45px) scale(1);
  }
  100% {
    transform: rotate(360deg) translateX(45px) scale(0);
    opacity: 0;
  }
}

/* ============================================
   加载文字
   ============================================ */
.loading-state__text {
  font-family: var(--font-display);
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-gray-700);
  margin: 0;
  animation: pulse-text 1.5s ease-in-out infinite;
}

@keyframes pulse-text {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}

/* ============================================
   进度条
   ============================================ */
.loading-state__progress {
  display: flex;
  align-items: center;
  gap: 1rem;
  width: 200px;
}

.loading-state__progress-bar {
  flex: 1;
  height: 4px;
  background: var(--color-gray-200);
  border-radius: var(--radius-full);
  overflow: hidden;
  position: relative;
}

.loading-state__progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--color-primary-500), var(--color-accent-500));
  border-radius: var(--radius-full);
  transition: width 300ms ease-out;
  position: relative;
}

.loading-state__progress-fill::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.5), transparent);
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

.loading-state__progress-text {
  font-family: var(--font-mono);
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-gray-600);
  min-width: 3rem;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

/* ============================================
   响应式
   ============================================ */
@media (max-width: 640px) {
  .loading-state__spinner {
    width: 64px;
    height: 64px;
  }

  .loading-state__particle {
    animation-name: orbit-mobile;
  }

  @keyframes orbit-mobile {
    0% {
      transform: rotate(0deg) translateX(36px) scale(0);
      opacity: 0;
    }
    10% {
      opacity: 1;
      transform: rotate(36deg) translateX(36px) scale(1);
    }
    90% {
      opacity: 1;
      transform: rotate(324deg) translateX(36px) scale(1);
    }
    100% {
      transform: rotate(360deg) translateX(36px) scale(0);
      opacity: 0;
    }
  }

  .loading-state__text {
    font-size: 0.875rem;
  }

  .loading-state__progress {
    width: 160px;
  }
}
</style>
