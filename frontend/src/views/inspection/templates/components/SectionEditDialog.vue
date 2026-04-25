<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import type { TemplateSection } from '@/types/insp/template'

const props = defineProps<{
  visible: boolean
  section: TemplateSection | null
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
  save: [data: { sectionName: string; isRepeatable: boolean }]
}>()

const sectionNameInput = ref<HTMLInputElement | null>(null)

const maskMouseDownTarget = ref<EventTarget | null>(null)
function onMaskMouseDown(e: MouseEvent) { maskMouseDownTarget.value = e.target }
function onMaskClick(e: MouseEvent) {
  if (e.target === e.currentTarget && maskMouseDownTarget.value === e.currentTarget) emit('update:visible', false)
  maskMouseDownTarget.value = null
}

const form = ref({
  sectionName: '',
  isRepeatable: false,
})

watch(() => props.section, (s) => {
  if (s) {
    form.value = {
      sectionName: s.sectionName,
      isRepeatable: s.isRepeatable,
    }
  }
}, { immediate: true })

watch(() => props.visible, (v) => {
  if (v) {
    nextTick(() => {
      sectionNameInput.value?.focus()
      if (sectionNameInput.value) sectionNameInput.value.select()
    })
  }
})

function handleSave() {
  if (!form.value.sectionName.trim()) return
  emit('save', {
    sectionName: form.value.sectionName,
    isRepeatable: form.value.isRepeatable,
  })
}
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="visible"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
        @mousedown="onMaskMouseDown"
        @click="onMaskClick"
      >
        <div class="w-[560px] rounded-lg bg-white p-6 shadow-xl max-h-[80vh] overflow-y-auto">
          <h3 class="mb-4 text-base font-semibold text-gray-800">编辑分区</h3>
          <div class="space-y-3">
            <div>
              <label class="mb-1 block text-sm text-gray-600">分区名称 <span class="text-red-400">*</span></label>
              <input
                ref="sectionNameInput"
                v-model="form.sectionName"
                placeholder="请输入分区名称"
                class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-400"
              />
            </div>
            <div>
              <label class="flex items-center gap-2 text-sm text-gray-600">
                <input v-model="form.isRepeatable" type="checkbox" class="rounded" />
                可重复
              </label>
            </div>
          </div>
          <div class="mt-5 flex justify-end gap-3">
            <button
              class="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-600 hover:bg-gray-50"
              @click="emit('update:visible', false)"
            >
              取消
            </button>
            <button
              class="rounded-md bg-blue-500 px-4 py-2 text-sm text-white hover:bg-blue-600"
              @click="handleSave"
            >
              保存
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
