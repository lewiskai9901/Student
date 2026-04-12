<template>
  <div style="padding: 20px 24px;">
    <h3 class="wb-step-title">智能排课</h3>
    <p class="wb-step-desc">检查数据就绪状态，然后执行自动排课。</p>

    <!-- Readiness Check -->
    <StepDataReady
      :semester-id="semesterId"
      :readiness="readiness"
      @refresh="loadReadiness"
    />

    <div style="border-top: 1px solid #f3f4f6; margin: 20px 0;" />

    <!-- Schedule History -->
    <div v-if="plans.length > 0" class="wb-card" style="margin-bottom: 16px;">
      <div class="wb-card-header">
        <span class="wb-card-title">排课历史版本</span>
        <span style="font-size: 12px; color: #6b7280;">共 {{ plans.length }} 个版本</span>
      </div>
      <div style="padding: 12px;">
        <div v-for="plan in plans" :key="plan.id" class="plan-item" :class="{ active: plan.status === 1 }">
          <div style="flex: 1; min-width: 0;">
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="font-size: 13px; font-weight: 500; color: #111827;">{{ plan.name }}</span>
              <span v-if="plan.status === 1" class="tm-chip tm-chip-green">当前</span>
              <span v-else-if="plan.status === 2" class="tm-chip tm-chip-gray">已归档</span>
              <span v-else class="tm-chip tm-chip-amber">草稿</span>
            </div>
            <div style="font-size: 11px; color: #9ca3af; margin-top: 2px;">
              {{ plan.entryCount || 0 }}条排课 | {{ plan.generatedAt || plan.createdAt || '' }}
            </div>
          </div>
          <div style="display: flex; gap: 4px;">
            <button v-if="plan.status !== 1" class="tm-action" @click="activatePlan(plan)">恢复</button>
            <button class="tm-action tm-action-danger" @click="deletePlan(plan)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Re-schedule button -->
    <div v-if="hasEntries" style="display: flex; align-items: center; gap: 8px; margin-bottom: 16px; padding: 12px 16px; border: 1px solid #fde68a; border-radius: 10px; background: #fffbeb;">
      <span style="font-size: 13px; color: #92400e; flex: 1;">当前已有排课数据，重排将归档现有版本并重新生成。</span>
      <button class="tm-btn tm-btn-secondary" :disabled="rescheduling" @click="handleReschedule">
        {{ rescheduling ? '处理中...' : '清除并重排' }}
      </button>
    </div>

    <!-- Execute -->
    <StepExecute
      :semester-id="semesterId"
      @scheduled="onScheduled"
    />

    <div style="border-top: 1px solid #f3f4f6; margin: 20px 0;" />

    <!-- Self-Study Fill -->
    <SelfStudyFiller :semester-id="semesterId" :total-weeks="totalWeeks" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { scheduleConfigApi } from '@/api/teaching'
import { http } from '@/utils/request'
import StepDataReady from '../schedule/setup/StepDataReady.vue'
import StepExecute from '../schedule/setup/StepExecute.vue'
import SelfStudyFiller from './SelfStudyFiller.vue'

const props = defineProps<{ semesterId: number | string | undefined; totalWeeks?: number }>()
const emit = defineEmits<{ scheduled: [] }>()

const readiness = ref<any>(null)
const plans = ref<any[]>([])
const rescheduling = ref(false)

const hasEntries = computed(() => readiness.value?.plans?.entryCount > 0)

async function loadReadiness() {
  if (!props.semesterId) return
  try {
    const res = await scheduleConfigApi.checkReadiness(props.semesterId)
    readiness.value = (res as any).data || res
  } catch { readiness.value = null }
}

async function loadPlans() {
  if (!props.semesterId) return
  try {
    const res = await http.get('/teaching/schedule-plans', { params: { semesterId: props.semesterId } })
    const data = (res as any).data || res
    plans.value = Array.isArray(data) ? data : data.records || []
  } catch { plans.value = [] }
}

async function handleReschedule() {
  try {
    await ElMessageBox.confirm(
      '将归档当前排课版本并清除排课数据，然后可以重新排课。确定继续？',
      '重新排课', { type: 'warning' }
    )
  } catch { return }

  rescheduling.value = true
  try {
    // 1. 归档当前排课为历史版本
    await http.post('/teaching/schedule-plans', {
      semesterId: props.semesterId,
      name: '排课版本 ' + new Date().toLocaleString('zh-CN'),
      status: 2, // archived
    })
    // 2. 清除排课条目，重置任务状态
    await http.post('/teaching/schedules/reset', { semesterId: props.semesterId })
    ElMessage.success('已归档旧版本，可以重新排课')
    loadReadiness()
    loadPlans()
    emit('scheduled')
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    rescheduling.value = false
  }
}

async function activatePlan(plan: any) {
  ElMessage.info('恢复历史版本功能开发中')
}

async function deletePlan(plan: any) {
  try {
    await ElMessageBox.confirm(`确定删除版本"${plan.name}"？`, '删除', { type: 'warning' })
    await http.delete(`/teaching/schedule-plans/${plan.id}`)
    ElMessage.success('已删除')
    loadPlans()
  } catch { /* cancelled */ }
}

function onScheduled() {
  loadReadiness()
  loadPlans()
  emit('scheduled')
}

watch(() => props.semesterId, () => {
  if (props.semesterId) { loadReadiness(); loadPlans() }
}, { immediate: true })
</script>

<style scoped>
.wb-step-title { font-size: 16px; font-weight: 700; color: #111827; margin: 0 0 4px; }
.wb-step-desc { font-size: 13px; color: #6b7280; margin: 0 0 20px; }
.wb-card { border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; overflow: hidden; }
.wb-card-header { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid #f3f4f6; }
.wb-card-title { font-size: 13px; font-weight: 600; color: #111827; }
.plan-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 12px; border: 1px solid #e5e7eb; border-radius: 8px; margin-bottom: 6px;
}
.plan-item:last-child { margin-bottom: 0; }
.plan-item.active { border-color: #bbf7d0; background: #f0fdf4; }
</style>
