<template>
  <el-tag
    :type="tagType"
    :size="size"
    :effect="effect"
    :round="round"
    class="status-tag"
  >
    <span v-if="showDot" class="status-tag__dot" :class="`status-tag__dot--${status}`" />
    <slot>{{ label }}</slot>
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type StatusType = 'success' | 'warning' | 'danger' | 'info' | 'primary' | 'default'
type TagType = 'success' | 'warning' | 'danger' | 'info' | 'primary' | ''

interface Props {
  status?: StatusType
  label?: string
  size?: 'large' | 'default' | 'small'
  effect?: 'dark' | 'light' | 'plain'
  round?: boolean
  showDot?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  status: 'default',
  label: '',
  size: 'default',
  effect: 'light',
  round: false,
  showDot: false
})

const tagType = computed<TagType>(() => {
  const typeMap: Record<StatusType, TagType> = {
    success: 'success',
    warning: 'warning',
    danger: 'danger',
    info: 'info',
    primary: 'primary',
    default: ''
  }
  return typeMap[props.status]
})
</script>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
}

.status-tag__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-tag__dot--success {
  background: var(--el-color-success);
}

.status-tag__dot--warning {
  background: var(--el-color-warning);
}

.status-tag__dot--danger {
  background: var(--el-color-danger);
}

.status-tag__dot--info {
  background: var(--el-color-info);
}

.status-tag__dot--primary {
  background: var(--el-color-primary);
}

.status-tag__dot--default {
  background: var(--color-gray-400);
}
</style>
