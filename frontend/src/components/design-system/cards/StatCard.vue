<template>
  <div
    :class="[
      'stat-card',
      `stat-card--${color}`,
      { 'stat-card--clickable': clickable }
    ]"
    @click="handleClick"
  >
    <!-- 背景装饰粒子 -->
    <div class="stat-card__particles">
      <div class="particle particle-1"></div>
      <div class="particle particle-2"></div>
      <div class="particle particle-3"></div>
    </div>

    <!-- 背景光晕 -->
    <div class="stat-card__glow"></div>

    <!-- 扫描线效果 -->
    <div class="stat-card__scan-line"></div>

    <!-- 内容区域 -->
    <div class="stat-card__content">
      <!-- 顶部:标题与图标 -->
      <div class="stat-card__header">
        <div class="stat-card__icon-wrapper">
          <div class="stat-card__icon-glow"></div>
          <component :is="icon" class="stat-card__icon" />
        </div>
        <h3 class="stat-card__title">{{ title }}</h3>
      </div>

      <!-- 主数据区域 -->
      <div class="stat-card__data">
        <div class="stat-card__value-wrapper">
          <span class="stat-card__value">{{ formattedValue }}</span>

          <!-- 趋势指示器 -->
          <div v-if="trend !== undefined" :class="['stat-card__trend', trendClass]">
            <component :is="trendIcon" class="stat-card__trend-icon" />
            <span class="stat-card__trend-value">{{ Math.abs(trend) }}%</span>
          </div>
        </div>

        <!-- 副标题/描述 -->
        <p v-if="subtitle" class="stat-card__subtitle">{{ subtitle }}</p>
      </div>

      <!-- 进度条 -->
      <div v-if="progress !== undefined" class="stat-card__progress">
        <div class="stat-card__progress-track">
          <div
            class="stat-card__progress-fill"
            :style="{ width: `${progress}%` }"
          ></div>
          <div class="stat-card__progress-glow" :style="{ width: `${progress}%` }"></div>
        </div>
        <span class="stat-card__progress-label">{{ progress }}%</span>
      </div>
    </div>

    <!-- 点击波纹效果 -->
    <div ref="rippleContainer" class="stat-card__ripple-container"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { TrendingUp, TrendingDown, Minus } from 'lucide-vue-next'
import type { Component } from 'vue'

interface Props {
  title: string
  value: number | string
  icon: Component
  trend?: number
  progress?: number
  color?: 'blue' | 'purple' | 'emerald' | 'orange' | 'cyan'
  clickable?: boolean
  subtitle?: string
}

const props = withDefaults(defineProps<Props>(), {
  color: 'blue',
  clickable: false
})

const emit = defineEmits<{
  click: []
}>()

const rippleContainer = ref<HTMLElement | null>(null)

// 格式化数值 - 添加千分位
const formattedValue = computed(() => {
  if (typeof props.value === 'number') {
    return props.value.toLocaleString('zh-CN')
  }
  return props.value
})

// 趋势图标
const trendIcon = computed(() => {
  if (props.trend === undefined) return null
  if (props.trend > 0) return TrendingUp
  if (props.trend < 0) return TrendingDown
  return Minus
})

// 趋势样式类
const trendClass = computed(() => {
  if (props.trend === undefined) return ''
  if (props.trend > 0) return 'stat-card__trend--positive'
  if (props.trend < 0) return 'stat-card__trend--negative'
  return 'stat-card__trend--neutral'
})

// 点击处理
const handleClick = (e: MouseEvent) => {
  if (!props.clickable) return

  // 创建波纹效果
  if (rippleContainer.value) {
    const ripple = document.createElement('div')
    ripple.className = 'stat-card__ripple'

    const rect = rippleContainer.value.getBoundingClientRect()
    const size = Math.max(rect.width, rect.height)
    const x = e.clientX - rect.left - size / 2
    const y = e.clientY - rect.top - size / 2

    ripple.style.width = ripple.style.height = `${size}px`
    ripple.style.left = `${x}px`
    ripple.style.top = `${y}px`

    rippleContainer.value.appendChild(ripple)

    setTimeout(() => {
      ripple.remove()
    }, 600)
  }

  emit('click')
}
</script>

<style scoped>
.stat-card {
  position: relative;
  background: white;
  border-radius: var(--radius-2xl);
  border: 1px solid var(--color-gray-200);
  padding: 1.5rem;
  overflow: hidden;
  transition: all var(--transition-base);
  isolation: isolate;
}

/* 可点击状态 */
.stat-card--clickable {
  cursor: pointer;
}

.stat-card--clickable:hover {
  transform: translateY(-4px);
  border-color: var(--color-primary-300);
  box-shadow: var(--shadow-xl);
}

.stat-card--clickable:active {
  transform: translateY(-2px);
}

/* ============================================
   背景装饰 - 粒子效果
   ============================================ */
.stat-card__particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.particle {
  position: absolute;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  opacity: 0;
  transition: opacity var(--transition-slow);
}

.stat-card:hover .particle {
  opacity: 0.4;
}

.particle-1 {
  top: 20%;
  right: 15%;
  animation: particle-float 3s ease-in-out infinite;
}

.particle-2 {
  top: 60%;
  right: 25%;
  animation: particle-float 4s ease-in-out infinite 0.5s;
}

.particle-3 {
  top: 40%;
  right: 10%;
  animation: particle-float 3.5s ease-in-out infinite 1s;
}

.stat-card--blue .particle {
  background: var(--color-primary-400);
}

.stat-card--purple .particle {
  background: var(--color-secondary-400);
}

.stat-card--emerald .particle {
  background: var(--color-success-500);
}

.stat-card--orange .particle {
  background: var(--color-warning-500);
}

.stat-card--cyan .particle {
  background: var(--color-accent-400);
}

/* ============================================
   背景光晕
   ============================================ */
.stat-card__glow {
  position: absolute;
  top: -50%;
  right: -20%;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.08;
  transition: all var(--transition-slow);
  z-index: 0;
}

.stat-card:hover .stat-card__glow {
  opacity: 0.15;
  transform: scale(1.3);
}

.stat-card--blue .stat-card__glow {
  background: var(--color-primary-400);
}

.stat-card--purple .stat-card__glow {
  background: var(--color-secondary-400);
}

.stat-card--emerald .stat-card__glow {
  background: var(--color-success-500);
}

.stat-card--orange .stat-card__glow {
  background: var(--color-warning-500);
}

.stat-card--cyan .stat-card__glow {
  background: var(--color-accent-400);
}

/* ============================================
   扫描线效果
   ============================================ */
.stat-card__scan-line {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(6, 182, 212, 0.5), transparent);
  opacity: 0;
  transition: opacity var(--transition-base);
  z-index: 1;
}

.stat-card:hover .stat-card__scan-line {
  opacity: 1;
  animation: scan-line 2s linear infinite;
}

/* ============================================
   内容区域
   ============================================ */
.stat-card__content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* ============================================
   头部 - 标题与图标
   ============================================ */
.stat-card__header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.stat-card__icon-wrapper {
  position: relative;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all var(--transition-base);
}

.stat-card--blue .stat-card__icon-wrapper {
  background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-600));
}

.stat-card--purple .stat-card__icon-wrapper {
  background: linear-gradient(135deg, var(--color-secondary-500), var(--color-secondary-600));
}

.stat-card--emerald .stat-card__icon-wrapper {
  background: linear-gradient(135deg, var(--color-success-500), var(--color-success-600));
}

.stat-card--orange .stat-card__icon-wrapper {
  background: linear-gradient(135deg, var(--color-warning-500), var(--color-warning-600));
}

.stat-card--cyan .stat-card__icon-wrapper {
  background: linear-gradient(135deg, var(--color-accent-500), var(--color-accent-600));
}

.stat-card:hover .stat-card__icon-wrapper {
  transform: scale(1.05);
}

.stat-card__icon-glow {
  position: absolute;
  inset: -4px;
  border-radius: var(--radius-lg);
  opacity: 0;
  transition: opacity var(--transition-base);
  z-index: -1;
}

.stat-card:hover .stat-card__icon-glow {
  opacity: 0.5;
}

.stat-card--blue .stat-card__icon-glow {
  box-shadow: 0 0 20px rgba(37, 99, 235, 0.5);
}

.stat-card--purple .stat-card__icon-glow {
  box-shadow: 0 0 20px rgba(99, 102, 241, 0.5);
}

.stat-card--emerald .stat-card__icon-glow {
  box-shadow: 0 0 20px rgba(16, 185, 129, 0.5);
}

.stat-card--orange .stat-card__icon-glow {
  box-shadow: 0 0 20px rgba(245, 158, 11, 0.5);
}

.stat-card--cyan .stat-card__icon-glow {
  box-shadow: 0 0 20px rgba(6, 182, 212, 0.5);
}

.stat-card__icon {
  width: 1.25rem;
  height: 1.25rem;
  color: white;
  transition: transform var(--transition-base);
}

.stat-card:hover .stat-card__icon {
  transform: scale(1.1);
}

.stat-card__title {
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-gray-600);
  margin: 0;
  letter-spacing: normal;
}

/* ============================================
   主数据区域
   ============================================ */
.stat-card__data {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.stat-card__value-wrapper {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.stat-card__value {
  font-family: var(--font-mono);
  font-size: 2rem;
  font-weight: 700;
  color: var(--color-gray-900);
  line-height: 1;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.stat-card__subtitle {
  font-size: 0.75rem;
  color: var(--color-gray-500);
  margin: 0;
}

/* ============================================
   趋势指示器
   ============================================ */
.stat-card__trend {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-md);
  font-size: 0.75rem;
  font-weight: 600;
  font-family: var(--font-mono);
}

.stat-card__trend--positive {
  background: rgba(16, 185, 129, 0.1);
  color: var(--color-success-700);
}

.stat-card__trend--negative {
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-danger-700);
}

.stat-card__trend--neutral {
  background: rgba(107, 114, 128, 0.1);
  color: var(--color-gray-700);
}

.stat-card__trend-icon {
  width: 0.875rem;
  height: 0.875rem;
}

.stat-card__trend-value {
  font-variant-numeric: tabular-nums;
}

/* ============================================
   进度条
   ============================================ */
.stat-card__progress {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.stat-card__progress-track {
  position: relative;
  flex: 1;
  height: 0.375rem;
  background: var(--color-gray-100);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.stat-card__progress-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  border-radius: var(--radius-full);
  transition: width var(--transition-slow);
  z-index: 1;
}

.stat-card--blue .stat-card__progress-fill {
  background: linear-gradient(90deg, var(--color-primary-500), var(--color-primary-600));
}

.stat-card--purple .stat-card__progress-fill {
  background: linear-gradient(90deg, var(--color-secondary-500), var(--color-secondary-600));
}

.stat-card--emerald .stat-card__progress-fill {
  background: linear-gradient(90deg, var(--color-success-500), var(--color-success-600));
}

.stat-card--orange .stat-card__progress-fill {
  background: linear-gradient(90deg, var(--color-warning-500), var(--color-warning-600));
}

.stat-card--cyan .stat-card__progress-fill {
  background: linear-gradient(90deg, var(--color-accent-500), var(--color-accent-600));
}

.stat-card__progress-glow {
  position: absolute;
  top: -2px;
  left: 0;
  height: calc(100% + 4px);
  border-radius: var(--radius-full);
  filter: blur(4px);
  opacity: 0.5;
  transition: width var(--transition-slow);
  z-index: 0;
}

.stat-card--blue .stat-card__progress-glow {
  background: var(--color-primary-400);
}

.stat-card--purple .stat-card__progress-glow {
  background: var(--color-secondary-400);
}

.stat-card--emerald .stat-card__progress-glow {
  background: var(--color-success-500);
}

.stat-card--orange .stat-card__progress-glow {
  background: var(--color-warning-500);
}

.stat-card--cyan .stat-card__progress-glow {
  background: var(--color-accent-400);
}

.stat-card__progress-label {
  font-family: var(--font-mono);
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-gray-600);
  min-width: 2.5rem;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

/* ============================================
   波纹效果
   ============================================ */
.stat-card__ripple-container {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  border-radius: var(--radius-2xl);
}

.stat-card__ripple {
  position: absolute;
  border-radius: 50%;
  transform: scale(0);
  animation: ripple-animation 600ms ease-out;
  pointer-events: none;
}

.stat-card--blue .stat-card__ripple {
  background: radial-gradient(circle, rgba(37, 99, 235, 0.3), transparent);
}

.stat-card--purple .stat-card__ripple {
  background: radial-gradient(circle, rgba(99, 102, 241, 0.3), transparent);
}

.stat-card--emerald .stat-card__ripple {
  background: radial-gradient(circle, rgba(16, 185, 129, 0.3), transparent);
}

.stat-card--orange .stat-card__ripple {
  background: radial-gradient(circle, rgba(245, 158, 11, 0.3), transparent);
}

.stat-card--cyan .stat-card__ripple {
  background: radial-gradient(circle, rgba(6, 182, 212, 0.3), transparent);
}

@keyframes ripple-animation {
  to {
    transform: scale(2);
    opacity: 0;
  }
}

/* ============================================
   响应式
   ============================================ */
@media (max-width: 640px) {
  .stat-card {
    padding: 1.25rem;
  }

  .stat-card__value {
    font-size: 1.75rem;
  }

  .stat-card__icon-wrapper {
    width: 2.25rem;
    height: 2.25rem;
  }

  .stat-card__icon {
    width: 1.125rem;
    height: 1.125rem;
  }
}
</style>
