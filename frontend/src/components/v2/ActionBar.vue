<template>
  <div class="action-bar" :class="{ 'action-bar--sticky': sticky }">
    <div class="action-bar__left">
      <slot name="left">
        <!-- 批量操作提示 -->
        <div v-if="selectedCount > 0" class="action-bar__selection-info">
          <el-checkbox
            :model-value="selectedCount > 0"
            :indeterminate="selectedCount > 0 && selectedCount < total"
            @change="handleSelectAllChange"
          />
          <span class="action-bar__selection-text">
            已选择 <strong>{{ selectedCount }}</strong> 项
            <template v-if="total > 0">
              / 共 {{ total }} 项
            </template>
          </span>
          <el-button text type="primary" size="small" @click="handleClearSelection">
            取消选择
          </el-button>
        </div>
      </slot>
    </div>

    <div class="action-bar__center">
      <slot name="center" />
    </div>

    <div class="action-bar__right">
      <slot name="right">
        <slot />
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  selectedCount?: number
  total?: number
  sticky?: boolean
}

withDefaults(defineProps<Props>(), {
  selectedCount: 0,
  total: 0,
  sticky: false
})

const emit = defineEmits<{
  'select-all': [selected: boolean]
  'clear-selection': []
}>()

const handleSelectAllChange = (val: boolean | string | number) => {
  emit('select-all', !!val)
}

const handleClearSelection = () => {
  emit('clear-selection')
}
</script>

<style scoped>
.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 1rem;
  background: white;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-gray-200);
}

.action-bar--sticky {
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: var(--shadow-sm);
}

.action-bar__left,
.action-bar__right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.action-bar__center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.action-bar__selection-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--color-gray-600);
}

.action-bar__selection-text strong {
  color: var(--color-primary-600);
  font-weight: 600;
}

@media (max-width: 768px) {
  .action-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .action-bar__left,
  .action-bar__center,
  .action-bar__right {
    justify-content: flex-start;
  }

  .action-bar__center {
    justify-content: flex-start;
  }
}
</style>
