<template>
  <div class="description-list" :class="[`description-list--${layout}`, `description-list--${size}`]">
    <div
      v-for="(item, index) in items"
      :key="index"
      class="description-list__item"
      :style="{ gridColumn: item.span ? `span ${item.span}` : undefined }"
    >
      <dt class="description-list__label">{{ item.label }}</dt>
      <dd class="description-list__value">
        <slot :name="item.key" :item="item" :value="item.value">
          <template v-if="item.value !== undefined && item.value !== null && item.value !== ''">
            {{ item.value }}
          </template>
          <span v-else class="description-list__empty">{{ emptyText }}</span>
        </slot>
      </dd>
    </div>
  </div>
</template>

<script setup lang="ts">
interface DescriptionItem {
  key: string
  label: string
  value?: string | number | boolean | null
  span?: number
}

interface Props {
  items: DescriptionItem[]
  layout?: 'horizontal' | 'vertical'
  size?: 'small' | 'default' | 'large'
  columns?: number
  emptyText?: string
}

withDefaults(defineProps<Props>(), {
  layout: 'horizontal',
  size: 'default',
  columns: 2,
  emptyText: '-'
})
</script>

<style scoped>
.description-list {
  display: grid;
  gap: 1rem;
}

.description-list--horizontal {
  grid-template-columns: repeat(v-bind(columns), 1fr);
}

.description-list--vertical {
  grid-template-columns: 1fr;
}

.description-list__item {
  display: flex;
  gap: 0.5rem;
}

.description-list--horizontal .description-list__item {
  flex-direction: row;
}

.description-list--vertical .description-list__item {
  flex-direction: column;
}

.description-list__label {
  flex-shrink: 0;
  color: var(--color-gray-500);
  font-weight: 500;
}

.description-list--horizontal .description-list__label {
  min-width: 100px;
}

.description-list--horizontal .description-list__label::after {
  content: '：';
}

.description-list__value {
  flex: 1;
  color: var(--color-gray-800);
  word-break: break-all;
}

.description-list__empty {
  color: var(--color-gray-400);
}

/* 尺寸变体 */
.description-list--small {
  font-size: 0.75rem;
  gap: 0.75rem;
}

.description-list--small .description-list__label {
  min-width: 80px;
}

.description-list--large {
  font-size: 1rem;
  gap: 1.25rem;
}

.description-list--large .description-list__label {
  min-width: 120px;
}

@media (max-width: 768px) {
  .description-list--horizontal {
    grid-template-columns: 1fr;
  }
}
</style>
