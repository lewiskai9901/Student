<template>
  <div>
    <h3 class="setup-step-title">执行排课</h3>
    <p class="setup-step-desc">系统将基于约束规则自动排课。已锁定或手动排定的课程不会被调整。</p>

    <!-- 排课模式选择 -->
    <div style="border: 1px solid #e5e7eb; border-radius: 10px; padding: 16px; margin-bottom: 16px; background: #fff;">
      <label class="tm-label" style="margin-bottom: 10px;">排课模式</label>
      <div class="sch-presets">
        <div
          v-for="p in presets"
          :key="p.key"
          :class="['sch-preset', { active: preset === p.key }]"
          @click="applyPreset(p.key)"
        >
          <div class="sch-preset-icon">{{ p.icon }}</div>
          <div class="sch-preset-title">{{ p.title }}</div>
          <div class="sch-preset-desc">{{ p.desc }}</div>
          <div class="sch-preset-time">约 {{ p.timeEstimate }}</div>
        </div>
      </div>

      <!-- 高级设置（折叠）-->
      <div style="margin-top: 12px;">
        <button class="sch-advanced-toggle" @click="showAdvanced = !showAdvanced">
          <svg :style="{ transform: showAdvanced ? 'rotate(90deg)' : 'rotate(0)' }" width="10" height="10" viewBox="0 0 10 10" fill="none"><path d="M3 2l4 3-4 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          高级参数（适用于有排课经验的用户）
        </button>
        <div v-if="showAdvanced" class="sch-advanced">
          <div class="tm-fields tm-cols-3">
            <div class="tm-field">
              <label class="tm-label">迭代次数</label>
              <input v-model.number="params.maxIterations" type="number" min="100" max="5000" step="100" class="tm-input" />
              <p class="sch-hint">数字越大越精准但越慢</p>
            </div>
            <div class="tm-field">
              <label class="tm-label">候选方案数</label>
              <input v-model.number="params.populationSize" type="number" min="10" max="100" step="10" class="tm-input" />
              <p class="sch-hint">同时探索的方案数量</p>
            </div>
            <div class="tm-field">
              <label class="tm-label">变异率 {{ params.mutationRate }}</label>
              <input v-model.number="params.mutationRate" type="range" min="0.01" max="0.5" step="0.01" style="width: 100%;" />
              <p class="sch-hint">越高越激进探索新方案</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <button class="tm-btn tm-btn-primary" :disabled="scheduling" @click="runSchedule" style="margin-bottom: 16px; padding: 10px 24px; font-size: 14px;">
      {{ scheduling ? '排课中...' : '▶ 开始智能排课' }}
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
      <p v-if="result.skippedNoTeacher > 0" style="font-size: 13px; color: #dc2626; margin-top: 6px; font-weight: 500;">
        ⚠ {{ result.skippedNoTeacher }} 个任务因未分配教师被跳过，请在
        <a href="/teaching/offerings?tab=fulfillment" target="_blank" style="color: #2563eb;">任务落实</a>
        中完成教师分配后重新排课。
      </p>
      <p v-if="result.message" style="font-size: 13px; color: #d97706; margin-top: 4px;">{{ result.message }}</p>
    </div>

    <!-- 容量警告 -->
    <div v-if="result?.capacityWarnings?.length > 0" style="border: 1px solid #fde68a; border-radius: 10px; background: #fffbeb; padding: 14px 16px; margin-bottom: 16px;">
      <p style="font-size: 13px; font-weight: 600; color: #d97706; margin: 0 0 8px;">教室容量警告 ({{ result.capacityWarnings.length }})</p>
      <div v-for="(w, i) in result.capacityWarnings" :key="i" style="font-size: 12px; color: #92400e; padding: 4px 0; border-bottom: 1px solid #fef3c7;">
        {{ w.message }}
      </div>
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
const preset = ref<'fast' | 'standard' | 'precise'>('standard')
const showAdvanced = ref(false)

const presets = [
  { key: 'fast', icon: '⚡', title: '快速', desc: '适合任务少或初次排课', timeEstimate: '10-30秒',
    params: { maxIterations: 200, populationSize: 20, mutationRate: 0.15 } },
  { key: 'standard', icon: '⚖️', title: '标准', desc: '兼顾速度和质量（推荐）', timeEstimate: '30秒-2分钟',
    params: { maxIterations: 500, populationSize: 30, mutationRate: 0.1 } },
  { key: 'precise', icon: '🎯', title: '精细', desc: '大量任务、复杂约束', timeEstimate: '2-5分钟',
    params: { maxIterations: 1500, populationSize: 50, mutationRate: 0.08 } },
] as const

function applyPreset(key: typeof preset.value) {
  preset.value = key
  const p = presets.find(x => x.key === key)
  if (p) {
    params.maxIterations = p.params.maxIterations
    params.populationSize = p.params.populationSize
    params.mutationRate = p.params.mutationRate
  }
}

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

/* 排课模式预设 */
.sch-presets { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.sch-preset {
  border: 1.5px solid #e5e7eb; border-radius: 8px;
  padding: 12px; cursor: pointer; text-align: center;
  transition: all 0.15s; background: #fff;
}
.sch-preset:hover { border-color: #9ca3af; background: #f9fafb; }
.sch-preset.active {
  border-color: #2563eb; background: #eff6ff;
  box-shadow: 0 0 0 3px rgba(37,99,235,0.08);
}
.sch-preset-icon { font-size: 22px; margin-bottom: 4px; }
.sch-preset-title { font-size: 13px; font-weight: 600; color: #111827; margin-bottom: 2px; }
.sch-preset.active .sch-preset-title { color: #2563eb; }
.sch-preset-desc { font-size: 11px; color: #6b7280; line-height: 1.3; margin-bottom: 4px; }
.sch-preset-time { font-size: 10.5px; color: #9ca3af; }

/* 高级设置 */
.sch-advanced-toggle {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 12px; color: #6b7280;
  background: none; border: none; cursor: pointer;
  padding: 4px 0;
}
.sch-advanced-toggle:hover { color: #2563eb; }
.sch-advanced-toggle svg { transition: transform 0.15s; }
.sch-advanced {
  margin-top: 10px; padding: 12px; background: #f9fafb;
  border: 1px solid #f3f4f6; border-radius: 8px;
}
.sch-hint { font-size: 10.5px; color: #9ca3af; margin-top: 3px; margin-bottom: 0; }
</style>
