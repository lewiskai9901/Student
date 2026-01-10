<template>
  <div class="remark-editor">
    <!-- 触发按钮 -->
    <button
      @click="openDialog"
      :disabled="disabled"
      :class="[
        'flex items-center justify-center gap-1.5 rounded-lg py-2 text-xs font-medium transition-all',
        disabled
          ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
          : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
      ]"
      :style="{ width: buttonWidth }"
    >
      <FileText class="h-3.5 w-3.5" />
      <span>{{ buttonText }}</span>
      <span v-if="hasRemark" class="h-1.5 w-1.5 rounded-full bg-green-500"></span>
    </button>

    <!-- 备注预览 -->
    <div v-if="showPreview && hasRemark" class="mt-2 rounded-lg bg-gray-50 p-2 text-xs text-gray-600">
      <p class="line-clamp-2">{{ modelValue }}</p>
    </div>

    <!-- 编辑对话框 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="dialogVisible" class="fixed inset-0 z-[9999] flex items-center justify-center">
          <div class="fixed inset-0 bg-black/60 backdrop-blur-sm" @click="closeDialog"></div>
          <Transition name="slide-up">
            <div v-if="dialogVisible" class="relative w-full max-w-xl rounded-2xl bg-white shadow-2xl overflow-hidden">
              <!-- 头部 -->
              <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4 bg-gradient-to-r from-green-50 to-emerald-50">
                <div class="flex items-center gap-3">
                  <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-green-500 shadow-lg shadow-green-500/30">
                    <FileText class="h-5 w-5 text-white" />
                  </div>
                  <div>
                    <h3 class="text-lg font-semibold text-gray-900">{{ dialogTitle }}</h3>
                    <p class="text-xs text-gray-500">添加扣分说明或详细描述</p>
                  </div>
                </div>
                <button @click="closeDialog" class="rounded-lg p-2 hover:bg-gray-100 transition-colors">
                  <X class="h-5 w-5 text-gray-500" />
                </button>
              </div>

              <!-- 内容区 -->
              <div class="p-6">
                <!-- 快捷模板 -->
                <div v-if="quickTemplates.length > 0" class="mb-4">
                  <div class="mb-2 flex items-center gap-2 text-sm text-gray-500">
                    <Zap class="h-4 w-4 text-amber-500" />
                    <span>快捷输入</span>
                  </div>
                  <div class="flex flex-wrap gap-2">
                    <button
                      v-for="(template, index) in quickTemplates"
                      :key="index"
                      @click="insertTemplate(template)"
                      class="rounded-full border border-gray-200 bg-white px-3 py-1.5 text-xs text-gray-600 hover:border-green-300 hover:bg-green-50 hover:text-green-700 transition-all"
                    >
                      {{ template }}
                    </button>
                  </div>
                </div>

                <!-- 输入框 -->
                <div class="relative">
                  <textarea
                    ref="textareaRef"
                    v-model="localRemark"
                    :rows="rows"
                    :maxlength="maxLength"
                    :placeholder="placeholder"
                    class="w-full rounded-xl border-2 border-gray-200 p-4 text-sm leading-relaxed focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/20 transition-all resize-none"
                  ></textarea>
                  <div class="absolute bottom-3 right-3 text-xs text-gray-400">
                    {{ localRemark.length }}/{{ maxLength }}
                  </div>
                </div>

                <!-- 提示信息 -->
                <div class="mt-3 flex items-center gap-2 text-xs text-gray-400">
                  <Info class="h-3.5 w-3.5" />
                  <span>备注信息将记录在扣分明细中，可用于后续查询和申诉参考</span>
                </div>
              </div>

              <!-- 底部按钮 -->
              <div class="flex items-center justify-between border-t border-gray-100 px-6 py-4 bg-gray-50">
                <button
                  v-if="localRemark"
                  @click="localRemark = ''"
                  class="flex items-center gap-1 text-sm text-gray-500 hover:text-red-500 transition-colors"
                >
                  <Trash2 class="h-4 w-4" />
                  清空
                </button>
                <span v-else></span>
                <div class="flex gap-3">
                  <button
                    @click="closeDialog"
                    class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 transition-colors"
                  >
                    取消
                  </button>
                  <button
                    @click="confirmRemark"
                    class="rounded-lg bg-green-600 px-6 py-2 text-sm font-medium text-white hover:bg-green-700 transition-colors shadow-lg shadow-green-600/30"
                  >
                    确定
                  </button>
                </div>
              </div>
            </div>
          </Transition>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { FileText, X, Zap, Info, Trash2 } from 'lucide-vue-next'

interface Props {
  modelValue: string           // 备注内容
  disabled?: boolean           // 是否禁用
  buttonText?: string          // 按钮文字
  buttonWidth?: string         // 按钮宽度
  dialogTitle?: string         // 对话框标题
  placeholder?: string         // 输入提示
  maxLength?: number           // 最大长度
  rows?: number                // 行数
  showPreview?: boolean        // 是否显示预览
  quickTemplates?: string[]    // 快捷输入模板
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  buttonText: '备注',
  buttonWidth: '100%',
  dialogTitle: '添加备注',
  placeholder: '请输入备注信息...',
  maxLength: 500,
  rows: 5,
  showPreview: false,
  quickTemplates: () => [
    '未按规定整理内务',
    '卫生打扫不彻底',
    '违规使用电器',
    '未按时熄灯就寝',
    '大声喧哗影响他人',
    '物品摆放不整齐',
    '门窗未关闭',
    '垃圾未及时清理'
  ]
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'change': [value: string]
}>()

// 状态
const dialogVisible = ref(false)
const localRemark = ref('')
const textareaRef = ref<HTMLTextAreaElement>()

// 是否有备注
const hasRemark = computed(() => !!props.modelValue && props.modelValue.trim().length > 0)

// 同步外部值
watch(() => props.modelValue, (val) => {
  localRemark.value = val || ''
}, { immediate: true })

// 打开对话框
const openDialog = () => {
  if (props.disabled) return
  dialogVisible.value = true
  localRemark.value = props.modelValue || ''
  nextTick(() => {
    textareaRef.value?.focus()
  })
}

// 关闭对话框
const closeDialog = () => {
  dialogVisible.value = false
}

// 插入模板
const insertTemplate = (template: string) => {
  const textarea = textareaRef.value
  if (!textarea) {
    localRemark.value = localRemark.value ? `${localRemark.value}；${template}` : template
    return
  }

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const text = localRemark.value

  // 智能插入：如果有选中文字则替换，否则在光标位置插入
  if (start === end) {
    // 没有选中，在光标位置插入
    const prefix = text.slice(0, start)
    const suffix = text.slice(end)
    // 如果前面有内容且不是标点符号，加分号
    const separator = prefix && !prefix.match(/[，。；！？、\s]$/) ? '；' : ''
    localRemark.value = prefix + separator + template + suffix

    // 设置光标位置
    nextTick(() => {
      const newPos = start + separator.length + template.length
      textarea.setSelectionRange(newPos, newPos)
      textarea.focus()
    })
  } else {
    // 有选中，替换选中内容
    localRemark.value = text.slice(0, start) + template + text.slice(end)
    nextTick(() => {
      const newPos = start + template.length
      textarea.setSelectionRange(newPos, newPos)
      textarea.focus()
    })
  }
}

// 确认备注
const confirmRemark = () => {
  emit('update:modelValue', localRemark.value)
  emit('change', localRemark.value)
  dialogVisible.value = false
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}
.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

textarea {
  scrollbar-width: thin;
  scrollbar-color: #d1d5db #f9fafb;
}

textarea::-webkit-scrollbar {
  width: 6px;
}

textarea::-webkit-scrollbar-track {
  background: #f9fafb;
  border-radius: 3px;
}

textarea::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

textarea::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}
</style>
