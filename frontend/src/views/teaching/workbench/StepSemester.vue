<template>
  <div style="padding: 20px 24px;">
    <h3 class="wb-step-title">学期准备</h3>
    <p class="wb-step-desc">配置本学期校历和作息表，确认教学周数和假期安排。</p>

    <!-- Semester Overview (inline) -->
    <div v-if="semester" class="wb-card" style="margin-bottom: 16px;">
      <div class="wb-card-header">
        <span class="wb-card-title">学期信息</span>
      </div>
      <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; padding: 16px;">
        <div class="wb-stat"><div class="wb-stat-val">{{ semester.semesterName || '-' }}</div><div class="wb-stat-lbl">当前学期</div></div>
        <div class="wb-stat"><div class="wb-stat-val">{{ semester.startDate || '-' }}</div><div class="wb-stat-lbl">开始日期</div></div>
        <div class="wb-stat"><div class="wb-stat-val">{{ semester.endDate || '-' }}</div><div class="wb-stat-lbl">结束日期</div></div>
        <div class="wb-stat"><div class="wb-stat-val">{{ weeks.length }}周</div><div class="wb-stat-lbl">总周数</div></div>
      </div>
    </div>

    <!-- Teaching Week Progress -->
    <div v-if="weeks.length > 0" class="wb-card" style="margin-bottom: 16px;">
      <div class="wb-card-header">
        <span class="wb-card-title">教学周进度</span>
        <span style="font-size: 12px; color: #6b7280;">
          教学 <b>{{ teachingWeekCount }}</b>周
          <template v-if="examWeekCount"> | 考试 <b>{{ examWeekCount }}</b>周</template>
          <template v-if="holidayWeekCount"> | 假期 <b>{{ holidayWeekCount }}</b>周</template>
        </span>
      </div>
      <div style="display: flex; gap: 3px; padding: 16px; flex-wrap: wrap;">
        <div
          v-for="w in taggedWeeks"
          :key="w.weekNumber"
          :title="`第${w.weekNumber}周 (${w.tag === 'exam' ? '考试周' : w.tag === 'holiday' ? '假期周' : '教学周'})`"
          :style="{
            width: '28px', height: '22px', borderRadius: '4px', display: 'flex',
            alignItems: 'center', justifyContent: 'center', fontSize: '10px', fontWeight: 500,
            background: w.tag === 'exam' ? '#f5f3ff' : w.tag === 'holiday' ? '#fef2f2' : currentWeekNum === w.weekNumber ? '#dbeafe' : '#f0fdf4',
            color: w.tag === 'exam' ? '#7c3aed' : w.tag === 'holiday' ? '#dc2626' : currentWeekNum === w.weekNumber ? '#2563eb' : '#16a34a',
            border: currentWeekNum === w.weekNumber ? '2px solid #2563eb' : '1px solid transparent',
          }"
        >{{ w.weekNumber }}</div>
      </div>
      <!-- Legend -->
      <div style="display: flex; gap: 16px; padding: 0 16px 12px; font-size: 11px; color: #6b7280;">
        <span style="display:flex;align-items:center;gap:4px;"><span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:#f0fdf4;" /> 教学</span>
        <span style="display:flex;align-items:center;gap:4px;"><span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:#f5f3ff;" /> 考试</span>
        <span style="display:flex;align-items:center;gap:4px;"><span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:#fef2f2;" /> 假期</span>
      </div>
    </div>

    <!-- Period Settings -->
    <PeriodSettings :semester-id="semesterId" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { semesterApi } from '@/api/calendar'
import PeriodSettings from '../calendar-new/PeriodSettings.vue'

const props = defineProps<{ semesterId: number | string | undefined }>()

const semester = ref<any>(null)
const weeks = ref<any[]>([])
const currentWeekNum = ref(0)

/** 根据 weekType 字段标记：1=teaching, 2=exam, 3=holiday */
const taggedWeeks = computed(() => {
  return weeks.value.map(w => {
    const wt = w.weekType ?? 1
    const tag = wt === 2 ? 'exam' : wt === 3 ? 'holiday' : 'teaching'
    return { ...w, tag }
  })
})

const teachingWeekCount = computed(() => taggedWeeks.value.filter(w => w.tag === 'teaching').length)
const examWeekCount = computed(() => taggedWeeks.value.filter(w => w.tag === 'exam').length)
const holidayWeekCount = computed(() => taggedWeeks.value.filter(w => w.tag === 'holiday').length)

async function load() {
  if (!props.semesterId) return
  try {
    const [semRes, weekRes] = await Promise.all([
      semesterApi.getById(props.semesterId),
      semesterApi.getWeeks(props.semesterId),
    ])
    semester.value = (semRes as any).data || semRes
    const wData = (weekRes as any).data || weekRes
    weeks.value = Array.isArray(wData) ? wData : []

    // Determine current week
    const today = new Date()
    const cw = weeks.value.find(w => {
      if (!w.startDate || !w.endDate) return false
      return new Date(w.startDate) <= today && today <= new Date(w.endDate)
    })
    currentWeekNum.value = cw?.weekNumber || 0
  } catch {
    semester.value = null
    weeks.value = []
  }
}

watch(() => props.semesterId, load, { immediate: true })
</script>

<style scoped>
.wb-step-title { font-size: 16px; font-weight: 700; color: #111827; margin: 0 0 4px; }
.wb-step-desc { font-size: 13px; color: #6b7280; margin: 0 0 20px; }
.wb-card { border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; overflow: hidden; }
.wb-card-header { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid #f3f4f6; }
.wb-card-title { font-size: 13px; font-weight: 600; color: #111827; }
.wb-stat { text-align: center; }
.wb-stat-val { font-size: 14px; font-weight: 600; color: #111827; margin-bottom: 2px; }
.wb-stat-lbl { font-size: 11px; color: #9ca3af; }
</style>
