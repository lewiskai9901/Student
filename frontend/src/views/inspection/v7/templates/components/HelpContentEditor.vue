<script setup lang="ts">
import { computed } from 'vue'
import { Plus, Trash2, Image, HelpCircle } from 'lucide-vue-next'

// ==================== Types ====================

interface HelpContent {
  helpText?: string
  referenceImages?: string[]
}

// ==================== Props & Emits ====================

const props = defineProps<{
  modelValue: HelpContent
}>()

const emit = defineEmits<{
  'update:modelValue': [value: HelpContent]
}>()

// ==================== Computed ====================

const helpText = computed({
  get: () => props.modelValue.helpText || '',
  set: (val) => emit('update:modelValue', { ...props.modelValue, helpText: val || undefined }),
})

const images = computed(() => props.modelValue.referenceImages || [])

// ==================== Helpers ====================

const inputCls = 'w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm outline-none focus:border-blue-400'

function addImage() {
  const current = [...images.value, '']
  emit('update:modelValue', { ...props.modelValue, referenceImages: current })
}

function removeImage(idx: number) {
  const current = [...images.value]
  current.splice(idx, 1)
  emit('update:modelValue', {
    ...props.modelValue,
    referenceImages: current.length > 0 ? current : undefined,
  })
}

function updateImage(idx: number, url: string) {
  const current = [...images.value]
  current[idx] = url
  emit('update:modelValue', { ...props.modelValue, referenceImages: current })
}

function isValidUrl(url: string): boolean {
  if (!url) return false
  try {
    new URL(url)
    return true
  } catch {
    return url.startsWith('/') || url.startsWith('./') || url.startsWith('data:')
  }
}

const textLength = computed(() => helpText.value.length)

const hasContent = computed(() =>
  (helpText.value && helpText.value.trim().length > 0) || images.value.length > 0
)
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center gap-2">
      <HelpCircle :size="16" class="text-gray-400" />
      <h3 class="text-sm font-medium text-gray-700">帮助内容</h3>
    </div>
    <p class="text-xs text-gray-400 -mt-2">
      检查员在填写此字段时会看到这些提示信息和参考图片
    </p>

    <!-- Help text -->
    <div>
      <div class="flex items-center justify-between mb-1">
        <label class="text-xs text-gray-500">帮助文字</label>
        <span class="text-[10px] text-gray-400">{{ textLength }} 字</span>
      </div>
      <textarea
        v-model="helpText"
        :class="inputCls"
        rows="4"
        placeholder="请输入帮助提示文字，例如：&#10;- 检查要点说明&#10;- 评分标准参考&#10;- 常见问题注意事项"
      />
    </div>

    <!-- Reference images -->
    <div>
      <div class="flex items-center justify-between mb-1">
        <label class="text-xs text-gray-500">参考图片</label>
        <button
          class="flex items-center gap-1 text-xs px-2 py-1 bg-blue-50 text-blue-600 rounded hover:bg-blue-100 transition"
          @click="addImage"
        >
          <Plus :size="12" /> 添加图片
        </button>
      </div>

      <div v-if="images.length === 0" class="text-center py-6 border border-dashed border-gray-300 rounded-lg">
        <Image :size="24" class="mx-auto text-gray-300 mb-1" />
        <p class="text-xs text-gray-400">暂无参考图片</p>
        <p class="text-[10px] text-gray-400 mt-0.5">添加图片 URL 供检查员参考</p>
      </div>

      <div v-else class="space-y-2">
        <div
          v-for="(url, idx) in images"
          :key="idx"
          class="flex items-start gap-2 rounded-lg border border-gray-200 p-2"
        >
          <!-- Thumbnail preview -->
          <div class="w-16 h-16 rounded bg-gray-100 flex items-center justify-center shrink-0 overflow-hidden">
            <img
              v-if="isValidUrl(url)"
              :src="url"
              class="w-full h-full object-cover"
              @error="($event.target as HTMLImageElement).style.display = 'none'"
            />
            <Image v-else :size="20" class="text-gray-300" />
          </div>

          <!-- URL input -->
          <div class="flex-1 min-w-0 space-y-1">
            <div class="flex items-center gap-1">
              <span class="text-[10px] text-gray-400 shrink-0">图片 {{ idx + 1 }}</span>
              <span v-if="url && isValidUrl(url)" class="text-[10px] text-green-500">有效</span>
              <span v-else-if="url" class="text-[10px] text-red-400">无效 URL</span>
            </div>
            <input
              :value="url"
              :class="inputCls"
              placeholder="https://example.com/image.jpg"
              @input="updateImage(idx, ($event.target as HTMLInputElement).value)"
            />
          </div>

          <!-- Remove -->
          <button
            class="rounded p-1 text-gray-400 hover:text-red-500 shrink-0 mt-4"
            @click="removeImage(idx)"
          >
            <Trash2 :size="14" />
          </button>
        </div>
      </div>
    </div>

    <!-- Preview section -->
    <div v-if="hasContent" class="rounded-lg border border-green-200 bg-green-50/30 p-3 space-y-2">
      <span class="text-xs font-medium text-green-700">预览效果</span>
      <div v-if="helpText" class="text-xs text-gray-700 whitespace-pre-wrap leading-relaxed">
        {{ helpText }}
      </div>
      <div v-if="images.length > 0" class="flex gap-2 flex-wrap">
        <div
          v-for="(url, idx) in images"
          :key="'preview-' + idx"
          class="w-20 h-20 rounded border border-green-200 overflow-hidden bg-white"
        >
          <img
            v-if="isValidUrl(url)"
            :src="url"
            class="w-full h-full object-cover"
            @error="($event.target as HTMLImageElement).style.display = 'none'"
          />
          <div v-else class="w-full h-full flex items-center justify-center">
            <Image :size="16" class="text-gray-300" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
