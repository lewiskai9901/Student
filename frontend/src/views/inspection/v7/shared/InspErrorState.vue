<script setup lang="ts">
import { AlertTriangle } from 'lucide-vue-next'

defineProps<{
  title?: string
  message?: string
  retryable?: boolean
}>()

const emit = defineEmits<{
  retry: []
}>()
</script>

<template>
  <div class="flex flex-col items-center justify-center py-16 text-gray-400">
    <AlertTriangle :size="48" class="mb-4 text-red-400 opacity-60" />
    <p class="text-base font-medium text-gray-500">{{ title || '加载失败' }}</p>
    <p v-if="message" class="mt-1 text-sm text-gray-400">{{ message }}</p>
    <button
      v-if="retryable !== false"
      class="mt-4 rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-600 transition hover:bg-gray-50"
      @click="emit('retry')"
    >
      重试
    </button>
  </div>
</template>
