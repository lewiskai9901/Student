<template>
  <el-drawer
    v-model="visible"
    :title="title"
    :size="size"
    :direction="direction"
    :destroy-on-close="destroyOnClose"
    :close-on-click-modal="closeOnClickModal"
    :close-on-press-escape="closeOnPressEscape"
    class="detail-drawer"
  >
    <template #header>
      <div class="detail-drawer__header">
        <span class="detail-drawer__title">{{ title }}</span>
        <div v-if="$slots.extra" class="detail-drawer__extra">
          <slot name="extra" />
        </div>
      </div>
    </template>

    <div v-if="loading" class="detail-drawer__loading">
      <el-skeleton :rows="10" animated />
    </div>

    <div v-else class="detail-drawer__content">
      <slot />
    </div>

    <template v-if="$slots.footer" #footer>
      <div class="detail-drawer__footer">
        <slot name="footer" />
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  modelValue: boolean
  title?: string
  size?: string | number
  direction?: 'rtl' | 'ltr' | 'ttb' | 'btt'
  loading?: boolean
  destroyOnClose?: boolean
  closeOnClickModal?: boolean
  closeOnPressEscape?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '详情',
  size: '500px',
  direction: 'rtl',
  loading: false,
  destroyOnClose: true,
  closeOnClickModal: true,
  closeOnPressEscape: true
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})
</script>

<style scoped>
.detail-drawer__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.detail-drawer__title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--color-gray-900);
}

.detail-drawer__extra {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.detail-drawer__loading {
  padding: 1rem;
}

.detail-drawer__content {
  padding: 0 1rem;
}

.detail-drawer__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
