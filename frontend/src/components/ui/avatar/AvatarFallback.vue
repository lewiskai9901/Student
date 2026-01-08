<script setup lang="ts">
import { cn } from '@/lib/utils'

interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

<template>
  <span
    :class="cn('flex h-full w-full items-center justify-center rounded-full bg-muted text-muted-foreground font-medium', props.class)"
  >
    <slot />
  </span>
</template>
