<script setup lang="ts">
import { PackageOpen } from 'lucide-vue-next'

defineProps<{
  title?: string
  description?: string
  actionLabel?: string
}>()

const emit = defineEmits<{
  action: []
}>()
</script>

<template>
  <div class="flex flex-col items-center justify-center py-16 text-gray-400">
    <PackageOpen :size="48" class="mb-4 opacity-40" />
    <p class="text-base font-medium text-gray-500">{{ title || '暂无数据' }}</p>
    <p v-if="description" class="mt-1 text-sm text-gray-400">{{ description }}</p>
    <button
      v-if="actionLabel"
      class="mt-4 rounded-md bg-blue-500 px-4 py-2 text-sm text-white transition hover:bg-blue-600"
      @click="emit('action')"
    >
      {{ actionLabel }}
    </button>
  </div>
</template>
