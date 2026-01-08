<script setup lang="ts">
import { cn } from '@/lib/utils'

interface Props {
  src?: string
  alt?: string
  class?: string
}

const props = defineProps<Props>()
</script>

<template>
  <img
    v-if="src"
    :src="src"
    :alt="alt"
    :class="cn('aspect-square h-full w-full object-cover', props.class)"
  />
</template>
