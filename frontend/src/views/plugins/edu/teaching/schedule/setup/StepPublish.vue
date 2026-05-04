<template>
  <div>
    <h3 class="setup-step-title">检查与发布</h3>
    <p class="setup-step-desc">确认排课结果无误后发布课表，发布后所有用户可查看。</p>

    <!-- Stats -->
    <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 20px;">
      <div class="stat-card">
        <div class="stat-num">{{ entryCount }}</div>
        <div class="stat-label">排课条目</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ conflictCount }}</div>
        <div class="stat-label">冲突数</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ planCount }}</div>
        <div class="stat-label">排课方案</div>
      </div>
    </div>

    <!-- Conflicts Warning -->
    <div v-if="conflictCount > 0" style="padding: 12px 16px; background: #fef2f2; border: 1px solid #fecaca; border-radius: 8px; margin-bottom: 16px;">
      <p style="font-size: 13px; font-weight: 600; color: #dc2626; margin: 0;">存在 {{ conflictCount }} 个冲突</p>
      <p style="font-size: 12px; color: #991b1b; margin: 4px 0 0;">建议在发布前解决所有冲突，或返回上一步重新排课。</p>
    </div>

    <!-- No conflicts -->
    <div v-else-if="entryCount > 0" style="padding: 12px 16px; background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; margin-bottom: 16px;">
      <p style="font-size: 13px; font-weight: 600; color: #16a34a; margin: 0;">无冲突，可以发布</p>
    </div>

    <div style="display: flex; gap: 12px;">
      <button class="tm-btn tm-btn-secondary" :disabled="generating || entryCount === 0" @click="generateLive" style="font-size: 14px; padding: 10px 20px;">
        {{ generating ? '生成中...' : '生成实况课表' }}
      </button>
      <button class="tm-btn tm-btn-primary" :disabled="publishing || entryCount === 0" @click="publish" style="font-size: 14px; padding: 10px 24px;">
        {{ publishing ? '发布中...' : '发布课表' }}
      </button>
    </div>
    <div v-if="liveResult" style="margin-top: 12px; padding: 10px 14px; background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; font-size: 13px; color: #16a34a;">
      实况课表已生成: {{ liveResult.generated }} 条实例, 校历影响 {{ liveResult.calendarAffected || 0 }} 条
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { scheduleApi, instanceApi } from '@/api/teaching'
import { scheduleConfigApi } from '@/api/teaching'

const props = defineProps<{ semesterId: number | string | undefined }>()
const emit = defineEmits<{ published: [] }>()

const entryCount = ref(0)
const conflictCount = ref(0)
const planCount = ref(0)
const publishing = ref(false)
const generating = ref(false)
const liveResult = ref<any>(null)

async function generateLive() {
  if (!props.semesterId) return
  generating.value = true
  try {
    const res = await instanceApi.generate(props.semesterId)
    liveResult.value = (res as any).data || res
    ElMessage.success(`实况课表生成完成`)
  } catch { ElMessage.error('生成失败') } finally { generating.value = false }
}

async function loadStats() {
  if (!props.semesterId) return
  try {
    const res = await scheduleConfigApi.checkReadiness(props.semesterId)
    const data = (res as any).data || res
    entryCount.value = data.plans?.entryCount || 0
    planCount.value = data.plans?.count || 0
    conflictCount.value = 0 // TODO: check conflicts API
  } catch { /* */ }
}

async function publish() {
  if (!props.semesterId) return
  try {
    await ElMessageBox.confirm('发布后课表将对所有用户可见，确定发布吗？', '发布确认', { type: 'warning' })
    publishing.value = true
    const plans = await scheduleApi.list({ semesterId: props.semesterId })
    const planList = (plans as any).data || plans || []
    for (const p of planList) {
      if (p.status === 0) await scheduleApi.publish(p.id)
    }
    ElMessage.success('课表已发布')
    emit('published')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '发布失败')
  } finally { publishing.value = false }
}

onMounted(loadStats)
</script>

<style scoped>
.setup-step-title { font-size: 16px; font-weight: 700; color: #111827; margin: 0 0 6px; }
.setup-step-desc { font-size: 13px; color: #6b7280; margin: 0 0 20px; }
.stat-card { border: 1px solid #e5e7eb; border-radius: 10px; padding: 16px; text-align: center; background: #f9fafb; }
.stat-num { font-size: 28px; font-weight: 700; color: #111827; }
.stat-label { font-size: 12px; color: #6b7280; margin-top: 4px; }
</style>
