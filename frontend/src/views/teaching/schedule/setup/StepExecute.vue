<template>
  <div>
    <h3 class="setup-step-title">执行排课</h3>
    <p class="setup-step-desc">系统将基于约束规则自动排课。已手动排定的课程不会被调整。</p>

    <!-- Params -->
    <div style="border: 1px solid #e5e7eb; border-radius: 10px; padding: 16px; margin-bottom: 16px;">
      <div class="tm-fields tm-cols-3">
        <div class="tm-field">
          <label class="tm-label">最大迭代</label>
          <input v-model.number="params.maxIterations" type="number" min="100" max="5000" step="100" class="tm-input" />
        </div>
        <div class="tm-field">
          <label class="tm-label">种群大小</label>
          <input v-model.number="params.populationSize" type="number" min="10" max="100" step="10" class="tm-input" />
        </div>
        <div class="tm-field">
          <label class="tm-label">变异率: {{ params.mutationRate }}</label>
          <input v-model.number="params.mutationRate" type="range" min="0.01" max="0.5" step="0.01" style="width: 100%;" />
        </div>
      </div>
    </div>

    <button class="tm-btn tm-btn-primary" :disabled="scheduling" @click="runSchedule" style="margin-bottom: 16px;">
      {{ scheduling ? '排课中...' : '开始智能排课' }}
    </button>

    <!-- Progress -->
    <div v-if="scheduling" style="margin-bottom: 16px;">
      <p style="font-size: 13px; color: #6b7280; margin-bottom: 8px;">正在排课...</p>
      <div style="height: 6px; background: #f3f4f6; border-radius: 99px; overflow: hidden;">
        <div style="height: 100%; background: #3b82f6; border-radius: 99px; transition: width 0.3s;" :style="{ width: progress + '%' }" />
      </div>
    </div>

    <!-- Result -->
    <div v-if="result" style="padding: 16px; border-radius: 10px; margin-bottom: 16px;" :style="{ background: result.success ? '#f0fdf4' : '#fef2f2', border: '1px solid ' + (result.success ? '#bbf7d0' : '#fecaca') }">
      <p style="font-weight: 600; font-size: 14px;" :style="{ color: result.success ? '#16a34a' : '#dc2626' }">
        {{ result.success ? '排课完成' : '排课失败' }}
      </p>
      <p style="font-size: 13px; color: #6b7280; margin-top: 4px;">
        生成 <b>{{ result.entriesGenerated || 0 }}</b> 条排课 | 耗时 {{ ((result.executionTime || 0) / 1000).toFixed(1) }}s
        <span v-if="result.conflicts?.length > 0" style="color: #d97706; margin-left: 8px;">（{{ result.conflicts.length }} 个冲突）</span>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { scheduleApi } from '@/api/teaching'

const props = defineProps<{ semesterId: number | string | undefined }>()
const emit = defineEmits<{ scheduled: [] }>()

const params = reactive({ maxIterations: 500, populationSize: 30, mutationRate: 0.1 })
const scheduling = ref(false)
const progress = ref(0)
const result = ref<any>(null)

async function runSchedule() {
  if (!props.semesterId) return
  scheduling.value = true
  progress.value = 0
  result.value = null
  const timer = setInterval(() => { if (progress.value < 90) progress.value += Math.random() * 15 }, 500)
  try {
    const res = await scheduleApi.autoSchedule({ semesterId: props.semesterId, ...params })
    progress.value = 100
    result.value = (res as any).data || res
    if (result.value.success) {
      ElMessage.success(`排课完成！生成 ${result.value.entriesGenerated} 条`)
      emit('scheduled')
    } else { ElMessage.error('排课失败') }
  } catch (e: any) { ElMessage.error('排课失败: ' + (e.message || '')) }
  finally { clearInterval(timer); scheduling.value = false }
}
</script>

<style scoped>
.setup-step-title { font-size: 16px; font-weight: 700; color: #111827; margin: 0 0 6px; }
.setup-step-desc { font-size: 13px; color: #6b7280; margin: 0 0 20px; }
</style>
