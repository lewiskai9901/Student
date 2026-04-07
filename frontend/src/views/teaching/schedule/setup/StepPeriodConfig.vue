<template>
  <div>
    <h3 class="setup-step-title">确认作息表</h3>
    <p class="setup-step-desc">排课将使用校历中配置的作息表。如需修改，请前往校历管理 → 作息表。</p>

    <div v-if="loading" style="text-align: center; padding: 30px; color: #9ca3af;">加载中...</div>

    <div v-else-if="!hasConfig" style="border: 1px solid #fde68a; border-radius: 10px; background: #fffbeb; padding: 20px; margin-bottom: 16px;">
      <p style="font-size: 14px; font-weight: 600; color: #d97706; margin: 0 0 8px;">本学期暂无作息表</p>
      <p style="font-size: 13px; color: #92400e; margin: 0 0 12px;">请先在校历管理中配置作息表，或点击下方按钮快速创建。</p>
      <div style="display: flex; gap: 8px;">
        <button class="tm-btn tm-btn-secondary" @click="initFromPrevious">从上学期继承</button>
        <button class="tm-btn tm-btn-primary" @click="createDefault">创建默认(8节制)</button>
      </div>
    </div>

    <div v-else style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; overflow: hidden;">
      <div style="display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid #f3f4f6;">
        <span style="font-size: 13px; font-weight: 600; color: #111827;">当前作息表 · {{ periods.length }}节制</span>
        <span class="tm-chip tm-chip-green">已配置</span>
      </div>
      <table class="tm-table" style="border: none; border-radius: 0;">
        <colgroup><col style="width: 60px" /><col /><col style="width: 100px" /><col style="width: 100px" /></colgroup>
        <thead><tr><th>节次</th><th class="text-left">名称</th><th>开始</th><th>结束</th></tr></thead>
        <tbody>
          <tr v-for="p in periods" :key="p.period">
            <td class="tm-mono">{{ p.period }}</td>
            <td class="text-left">{{ p.name }}</td>
            <td>{{ p.startTime }}</td>
            <td>{{ p.endTime }}</td>
          </tr>
        </tbody>
      </table>
      <div style="padding: 10px 16px; font-size: 12px; color: #6b7280; border-top: 1px solid #f3f4f6;">
        排课日: {{ scheduleDaysText }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { periodConfigApi } from '@/api/teaching'

const props = defineProps<{ semesterId: number | string | undefined; config: any }>()
const emit = defineEmits<{ saved: [config: any] }>()

const loading = ref(false)
const hasConfig = ref(false)
const periods = ref<any[]>([])
const scheduleDays = ref<number[]>([])

const dayNames: Record<number, string> = { 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' }
const scheduleDaysText = computed(() => scheduleDays.value.map(d => dayNames[d]).join('、'))

async function loadConfig() {
  if (!props.semesterId) return
  loading.value = true
  try {
    const res = await periodConfigApi.list(props.semesterId)
    const list = (res as any).data || res || []
    if (list.length > 0) {
      const c = list[0]
      hasConfig.value = true
      periods.value = typeof c.periods === 'string' ? JSON.parse(c.periods) : c.periods || []
      scheduleDays.value = typeof c.scheduleDays === 'string' ? JSON.parse(c.scheduleDays) : c.scheduleDays || []
      emit('saved', c)
    } else { hasConfig.value = false }
  } catch { hasConfig.value = false } finally { loading.value = false }
}

async function createDefault() {
  if (!props.semesterId) return
  const defaultPeriods = [
    { period: 1, name: '第一节', startTime: '08:00', endTime: '08:45' },{ period: 2, name: '第二节', startTime: '08:55', endTime: '09:40' },
    { period: 3, name: '第三节', startTime: '10:00', endTime: '10:45' },{ period: 4, name: '第四节', startTime: '10:55', endTime: '11:40' },
    { period: 5, name: '第五节', startTime: '14:00', endTime: '14:45' },{ period: 6, name: '第六节', startTime: '14:55', endTime: '15:40' },
    { period: 7, name: '第七节', startTime: '16:00', endTime: '16:45' },{ period: 8, name: '第八节', startTime: '16:55', endTime: '17:40' },
  ]
  try {
    await periodConfigApi.create({ semesterId: props.semesterId, configName: '默认作息表', periodsPerDay: 8, scheduleDays: [1,2,3,4,5], periods: defaultPeriods })
    ElMessage.success('已创建默认作息表'); loadConfig()
  } catch { ElMessage.error('创建失败') }
}

async function initFromPrevious() {
  if (!props.semesterId) return
  try {
    await periodConfigApi.initFromPrevious(props.semesterId)
    ElMessage.success('已继承'); loadConfig()
  } catch { ElMessage.error('继承失败') }
}

watch(() => props.semesterId, () => loadConfig(), { immediate: true })
</script>

<style scoped>
.setup-step-title { font-size: 16px; font-weight: 700; color: #111827; margin: 0 0 6px; }
.setup-step-desc { font-size: 13px; color: #6b7280; margin: 0 0 20px; }
</style>
