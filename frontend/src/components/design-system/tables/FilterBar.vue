<template>
  <div :class="['filter-bar', { 'filter-bar--collapsed': collapsed && collapsible }]">
    <!-- 背景效果 -->
    <div class="filter-bar__backdrop"></div>

    <!-- 头部 -->
    <div class="filter-bar__header">
      <div class="filter-bar__title-wrapper">
        <Filter class="filter-bar__icon" />
        <h3 class="filter-bar__title">{{ title }}</h3>
        <span v-if="activeFiltersCount > 0" class="filter-bar__badge">
          {{ activeFiltersCount }}
        </span>
      </div>

      <button
        v-if="collapsible"
        class="filter-bar__collapse-btn"
        @click="collapsed = !collapsed"
      >
        <ChevronDown :class="['filter-bar__collapse-icon', { 'filter-bar__collapse-icon--collapsed': collapsed }]" />
      </button>
    </div>

    <!-- 筛选内容 -->
    <div v-show="!collapsed || !collapsible" class="filter-bar__content">
      <div class="filter-bar__fields">
        <slot></slot>
      </div>

      <!-- 操作按钮 -->
      <div class="filter-bar__actions">
        <GradientButton
          size="md"
          :icon="Search"
          :loading="loading"
          @click="handleSearch"
        >
          查询
        </GradientButton>

        <GradientButton
          variant="outline"
          size="md"
          :icon="RotateCcw"
          @click="handleReset"
        >
          重置
        </GradientButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, useSlots } from 'vue'
import { Filter, ChevronDown, Search, RotateCcw } from 'lucide-vue-next'
import GradientButton from '../buttons/GradientButton.vue'

interface Props {
  title?: string
  collapsible?: boolean
  loading?: boolean
  activeFiltersCount?: number
}

const props = withDefaults(defineProps<Props>(), {
  title: '筛选条件',
  collapsible: false,
  loading: false,
  activeFiltersCount: 0
})

const emit = defineEmits<{
  search: []
  reset: []
}>()

const collapsed = ref(false)

const handleSearch = () => {
  emit('search')
}

const handleReset = () => {
  emit('reset')
}
</script>

<style scoped>
/* ============================================
   基础容器
   ============================================ */
.filter-bar {
  position: relative;
  background: white;
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-xl);
  overflow: hidden;
  transition: all var(--transition-base);
}

.filter-bar--collapsed {
  background: rgba(255, 255, 255, 0.8);
}

/* ============================================
   背景效果 - 毛玻璃
   ============================================ */
.filter-bar__backdrop {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.02) 0%, rgba(99, 102, 241, 0.02) 100%);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  pointer-events: none;
  z-index: 0;
}

/* ============================================
   头部
   ============================================ */
.filter-bar__header {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--color-gray-100);
}

.filter-bar--collapsed .filter-bar__header {
  border-bottom: none;
}

.filter-bar__title-wrapper {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.filter-bar__icon {
  width: 1.125rem;
  height: 1.125rem;
  color: var(--color-primary-600);
}

.filter-bar__title {
  font-family: var(--font-display);
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-gray-900);
  margin: 0;
  letter-spacing: -0.01em;
}

.filter-bar__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.25rem;
  height: 1.25rem;
  padding: 0 0.375rem;
  background: var(--gradient-primary);
  color: white;
  font-family: var(--font-mono);
  font-size: 0.75rem;
  font-weight: 700;
  border-radius: var(--radius-full);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
  animation: badge-appear 300ms cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

@keyframes badge-appear {
  from {
    transform: scale(0);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.filter-bar__collapse-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-md);
  background: white;
  color: var(--color-gray-600);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.filter-bar__collapse-btn:hover {
  border-color: var(--color-primary-400);
  color: var(--color-primary-600);
  background: rgba(37, 99, 235, 0.05);
}

.filter-bar__collapse-icon {
  width: 1rem;
  height: 1rem;
  transition: transform var(--transition-base);
}

.filter-bar__collapse-icon--collapsed {
  transform: rotate(-90deg);
}

/* ============================================
   内容区域
   ============================================ */
.filter-bar__content {
  position: relative;
  z-index: 1;
  padding: 1.5rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.filter-bar__fields {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  align-items: end;
}

/* ============================================
   操作按钮
   ============================================ */
.filter-bar__actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding-top: 0.5rem;
  border-top: 1px solid var(--color-gray-100);
}

/* ============================================
   响应式
   ============================================ */
@media (max-width: 1024px) {
  .filter-bar__fields {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  }
}

@media (max-width: 768px) {
  .filter-bar__fields {
    grid-template-columns: 1fr;
  }

  .filter-bar__content {
    padding: 1.25rem 1rem;
  }

  .filter-bar__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
