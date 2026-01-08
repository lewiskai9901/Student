<template>
  <nav class="flex items-center" aria-label="Breadcrumb">
    <ol class="flex items-center gap-1">
      <li v-for="(item, index) in breadcrumbs" :key="item.path" class="flex items-center">
        <ChevronRight v-if="index > 0" class="mx-1.5 h-4 w-4 text-gray-400" />
        <router-link
          v-if="index !== breadcrumbs.length - 1"
          :to="item.path"
          class="text-sm text-gray-600 transition-colors hover:text-blue-600"
        >
          {{ item.title }}
        </router-link>
        <span v-else class="text-sm text-gray-400">{{ item.title }}</span>
      </li>
    </ol>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronRight } from 'lucide-vue-next'
import { generateBreadcrumb } from '@/utils/menu-generator'

const route = useRoute()
const router = useRouter()

const breadcrumbs = computed(() => {
  const mainRoute = router.getRoutes().find(r => r.path === '/')
  if (!mainRoute || !mainRoute.children) return []
  const items = generateBreadcrumb(route.path, mainRoute.children)
  if (items.length > 0 && items[0].path !== '/dashboard') {
    items.unshift({ title: '首页', path: '/dashboard' })
  }
  return items
})
</script>
