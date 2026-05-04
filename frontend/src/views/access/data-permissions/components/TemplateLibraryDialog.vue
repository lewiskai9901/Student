<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="close" />
        <div class="relative w-full max-w-3xl rounded-xl bg-white shadow-2xl">
          <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4">
            <div class="flex items-center gap-2">
              <Library class="h-5 w-5 text-blue-600" />
              <h3 class="text-lg font-semibold text-gray-900">模板库</h3>
              <span class="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-500">
                {{ BUILTIN_TEMPLATES.length }}
              </span>
            </div>
            <button class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600" @click="close">
              <X class="h-5 w-5" />
            </button>
          </div>

          <div class="p-5">
            <p class="mb-4 text-xs text-gray-500">
              选择一个模板快速应用到当前角色. 应用后可在右侧"场景模板"微调.
            </p>

            <div class="grid grid-cols-2 gap-3 md:grid-cols-3">
              <button
                v-for="tpl in BUILTIN_TEMPLATES"
                :key="tpl.id"
                class="group relative flex flex-col items-start rounded-lg border border-gray-200 bg-white p-4 text-left transition hover:border-blue-400 hover:shadow-md"
                @click="apply(tpl)"
              >
                <div class="mb-2 flex items-center gap-2">
                  <div
                    class="flex h-9 w-9 items-center justify-center rounded-lg"
                    :style="{ background: industryBg(tpl.industry) }"
                  >
                    <component :is="iconFor(tpl.icon)" class="h-5 w-5" :style="{ color: industryColor(tpl.industry) }" />
                  </div>
                  <div class="min-w-0 flex-1">
                    <h4 class="truncate text-sm font-semibold text-gray-900">{{ tpl.name }}</h4>
                    <span
                      class="inline-block rounded px-1.5 py-0.5 text-[10px] font-medium"
                      :style="{
                        background: industryBg(tpl.industry),
                        color: industryColor(tpl.industry),
                      }"
                    >
                      {{ industryLabel(tpl.industry) }}
                    </span>
                  </div>
                </div>
                <p class="text-xs text-gray-600">{{ tpl.description }}</p>
                <p v-if="tpl.scenario" class="mt-1 text-[11px] text-gray-400">
                  典型: {{ tpl.scenario }}
                </p>
                <div
                  class="absolute right-2 top-2 rounded bg-blue-50 px-1.5 py-0.5 text-[10px] font-medium text-blue-600 opacity-0 transition group-hover:opacity-100"
                >
                  应用 >
                </div>
              </button>
            </div>

            <div class="mt-4 rounded-md bg-gray-50 px-3 py-2 text-[11px] text-gray-500">
              <Info class="mr-1 inline h-3 w-3" />
              应用模板会覆盖当前角色的所有模块 scope. 已有的 CUSTOM 自定义数据会被清除.
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import {
  X,
  Library,
  Info,
  Crown,
  Users,
  BookOpen,
  GraduationCap,
  Stethoscope,
  User,
  Settings,
} from 'lucide-vue-next'
import { BUILTIN_TEMPLATES, type RoleTemplate } from '../composables/useTemplateLibrary'

interface Props {
  visible: boolean
}

defineProps<Props>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  apply: [template: RoleTemplate]
}>()

const iconMap: Record<string, any> = {
  Crown,
  Users,
  BookOpen,
  GraduationCap,
  Stethoscope,
  User,
}

function iconFor(name: string) {
  return iconMap[name] || Settings
}

const INDUSTRY_LABELS: Record<string, string> = {
  CORE: '通用核心',
  EDU: '教育',
  HEALTH: '医疗',
  CARE: '养老',
  CUSTOM: '自定义',
}
function industryLabel(code: string) {
  return INDUSTRY_LABELS[code] || code
}

function industryColor(code: string) {
  return (
    { CORE: '#2563eb', EDU: '#d97706', HEALTH: '#be185d', CARE: '#059669', CUSTOM: '#6b7280' } as Record<
      string,
      string
    >
  )[code] || '#6b7280'
}

function industryBg(code: string) {
  return (
    { CORE: '#dbeafe', EDU: '#fef3c7', HEALTH: '#fce7f3', CARE: '#d1fae5', CUSTOM: '#f3f4f6' } as Record<
      string,
      string
    >
  )[code] || '#f3f4f6'
}

function close() {
  emit('update:visible', false)
}

function apply(tpl: RoleTemplate) {
  emit('apply', tpl)
  close()
}
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
