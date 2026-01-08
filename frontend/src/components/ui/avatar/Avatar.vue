<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  class?: string
  size?: 'sm' | 'md' | 'lg' | 'xl'
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
})

const sizeClasses = {
  sm: 'h-8 w-8',
  md: 'h-10 w-10',
  lg: 'h-12 w-12',
  xl: 'h-16 w-16',
}

const avatarClass = computed(() =>
  cn(
    'relative flex shrink-0 overflow-hidden rounded-full',
    sizeClasses[props.size],
    props.class
  )
)
</script>

<template>
  <span :class="avatarClass">
    <slot />
  </span>
</template>
