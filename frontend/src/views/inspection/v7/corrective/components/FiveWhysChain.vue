<script setup lang="ts">
/**
 * FiveWhysChain - 5-Why chain editor
 *
 * Provides a numbered chain of 5 "Why?" questions, each with a textarea.
 * Arrow/flow indicators connect steps visually.
 */
import { computed } from 'vue'
import { ArrowDown } from 'lucide-vue-next'

const props = defineProps<{
  modelValue: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

// Ensure we always have exactly 5 entries
const whys = computed(() => {
  const current = props.modelValue || []
  const result: string[] = []
  for (let i = 0; i < 5; i++) {
    result.push(current[i] || '')
  }
  return result
})

function updateWhy(index: number, value: string) {
  const updated = [...whys.value]
  updated[index] = value
  emit('update:modelValue', updated)
}

const stepLabels = [
  '为什么发生了这个问题？',
  '为什么会出现上述原因？',
  '为什么存在上述条件？',
  '为什么会形成该状况？',
  '根本原因是什么？',
]
</script>

<template>
  <div class="five-whys-chain">
    <div
      v-for="(_, index) in 5"
      :key="index"
      class="why-step"
    >
      <!-- Step header -->
      <div class="step-header">
        <div class="step-number" :class="{ 'step-number--filled': whys[index] }">
          {{ index + 1 }}
        </div>
        <div class="step-label">
          <span class="font-medium">Why {{ index + 1 }}</span>
          <span class="text-gray-400 text-xs ml-2">{{ stepLabels[index] }}</span>
        </div>
      </div>

      <!-- Input -->
      <div class="step-input">
        <el-input
          :model-value="whys[index]"
          type="textarea"
          :rows="2"
          :placeholder="`请输入第 ${index + 1} 层原因分析...`"
          maxlength="500"
          show-word-limit
          @update:model-value="(val: string) => updateWhy(index, val)"
        />
      </div>

      <!-- Arrow indicator (not after the last one) -->
      <div v-if="index < 4" class="step-arrow">
        <ArrowDown class="w-5 h-5 text-gray-300" />
      </div>
    </div>

    <!-- Root cause summary -->
    <div v-if="whys[4]" class="root-cause-summary">
      <el-alert type="info" :closable="false" show-icon>
        <template #title>
          <span class="font-medium">根本原因</span>
        </template>
        <div class="text-sm mt-1">{{ whys[4] }}</div>
      </el-alert>
    </div>
  </div>
</template>

<style scoped>
.five-whys-chain {
  padding: 8px 0;
}

.why-step {
  margin-bottom: 4px;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.step-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid #dcdfe6;
  color: #909399;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
  transition: all 0.2s;
}

.step-number--filled {
  border-color: #409eff;
  background-color: #409eff;
  color: #fff;
}

.step-label {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.step-input {
  padding-left: 36px;
}

.step-arrow {
  display: flex;
  justify-content: center;
  padding: 6px 0 2px 36px;
}

.root-cause-summary {
  margin-top: 12px;
  padding-left: 36px;
}
</style>
