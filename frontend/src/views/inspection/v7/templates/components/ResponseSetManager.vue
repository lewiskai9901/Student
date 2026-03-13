<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Check, ExternalLink } from 'lucide-vue-next'
import { responseSetApi } from '@/api/insp/responseSet'
import type { ResponseSet, ResponseSetOption } from '@/types/insp/template'

const props = defineProps<{
  modelValue: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number | null]
}>()

const sets = ref<ResponseSet[]>([])
const selectedOptions = ref<ResponseSetOption[]>([])
const loading = ref(false)

async function loadSets() {
  try {
    const result = await responseSetApi.getList({ page: 1, size: 200 })
    sets.value = result.records
  } catch { /* ignore */ }
}

async function loadOptions(setId: number) {
  loading.value = true
  try {
    selectedOptions.value = await responseSetApi.getOptions(setId)
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function handleSelect(setId: number | null) {
  emit('update:modelValue', setId)
  if (setId) loadOptions(setId)
  else selectedOptions.value = []
}

onMounted(() => {
  loadSets()
  if (props.modelValue) loadOptions(props.modelValue)
})
</script>

<template>
  <div class="space-y-2">
    <div class="flex items-center gap-2">
      <select
        :value="modelValue"
        class="flex-1 rounded-md border border-gray-300 px-3 py-1.5 text-sm outline-none focus:border-blue-400"
        @change="handleSelect(($event.target as HTMLSelectElement).value ? Number(($event.target as HTMLSelectElement).value) : null)"
      >
        <option :value="null">-- 选择选项集 --</option>
        <option v-for="s in sets" :key="s.id" :value="s.id">{{ s.setName }} ({{ s.setCode }})</option>
      </select>
      <router-link
        to="/inspection/v7/response-sets"
        target="_blank"
        class="shrink-0 rounded border border-gray-200 p-1.5 text-gray-400 hover:text-blue-500"
        title="管理选项集"
      >
        <ExternalLink :size="14" />
      </router-link>
    </div>

    <!-- Preview selected options -->
    <div v-if="modelValue && selectedOptions.length > 0" class="rounded-md border border-gray-100 bg-gray-50 p-2">
      <div class="mb-1 text-xs text-gray-400">选项预览</div>
      <div class="flex flex-wrap gap-1.5">
        <span
          v-for="opt in selectedOptions"
          :key="opt.id"
          class="inline-flex items-center gap-1 rounded-full border border-gray-200 bg-white px-2 py-0.5 text-xs"
        >
          <span
            class="inline-block h-2.5 w-2.5 rounded-full"
            :style="{ backgroundColor: opt.optionColor || '#ddd' }"
          />
          {{ opt.optionLabel }}
          <span v-if="opt.score" class="text-blue-500">{{ opt.score }}分</span>
        </span>
      </div>
    </div>
    <div v-else-if="modelValue && loading" class="text-xs text-gray-400">加载选项中...</div>
  </div>
</template>
