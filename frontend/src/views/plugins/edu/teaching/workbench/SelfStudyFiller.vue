<template>
  <div class="wb-card">
    <div class="wb-card-header">
      <span class="wb-card-title">自习课填充</span>
      <span style="font-size: 12px; color: #6b7280;">排课完成后，自动为空课位填充自习课</span>
    </div>
    <div style="padding: 16px;">
      <!-- Params -->
      <div style="display: flex; gap: 16px; align-items: end; margin-bottom: 16px; flex-wrap: wrap;">
        <div class="tm-field" style="margin: 0;">
          <label class="tm-label">每日节次上限</label>
          <input v-model.number="maxPeriods" type="number" min="4" max="12" class="tm-input" style="width: 100px;" />
        </div>
        <div class="tm-field" style="margin: 0;">
          <label class="tm-label">排课日数</label>
          <select v-model.number="maxWeekday" class="tm-field-select" style="width: 120px;">
            <option :value="5">周一至周五</option>
            <option :value="6">周一至周六</option>
            <option :value="7">周一至周日</option>
          </select>
        </div>
        <div class="tm-field" style="margin: 0;">
          <label class="tm-label">起始周</label>
          <input v-model.number="startWeek" type="number" min="1" max="30" class="tm-input" style="width: 80px;" />
        </div>
        <div class="tm-field" style="margin: 0;">
          <label class="tm-label">结束周</label>
          <input v-model.number="endWeek" type="number" min="1" max="30" class="tm-input" style="width: 80px;" />
        </div>
      </div>

      <!-- Actions -->
      <div style="display: flex; gap: 8px;">
        <button class="tm-btn tm-btn-primary" :disabled="filling || !semesterId" @click="handleFill">
          {{ filling ? '填充中...' : '自动填充自习课' }}
        </button>
        <button class="tm-btn tm-btn-secondary" :disabled="clearing || !semesterId" @click="handleClear">
          {{ clearing ? '清除中...' : '清除所有自习课' }}
        </button>
      </div>

      <!-- Result -->
      <div v-if="result" style="margin-top: 12px; padding: 10px 14px; border: 1px solid #bbf7d0; border-radius: 8px; background: #f0fdf4; font-size: 13px; color: #16a34a;">
        {{ result }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { selfStudyApi } from '@/api/teaching'

const props = defineProps<{ semesterId: number | string | undefined; totalWeeks?: number }>()

const maxPeriods = ref(8)
const maxWeekday = ref(5)
const startWeek = ref(1)
const endWeek = ref(props.totalWeeks ?? 22)

watch(() => props.totalWeeks, (v) => { if (v && v > 0) endWeek.value = v })
const filling = ref(false)
const clearing = ref(false)
const result = ref('')

async function handleFill() {
  if (!props.semesterId) return
  filling.value = true
  result.value = ''
  try {
    const res = await selfStudyApi.fill({
      semesterId: props.semesterId,
      maxPeriods: maxPeriods.value,
      maxWeekday: maxWeekday.value,
      startWeek: startWeek.value,
      endWeek: endWeek.value,
    })
    const data = (res as any).data || res
    result.value = `已填充 ${data.inserted ?? 0} 个自习课条目，覆盖 ${data.classCount ?? 0} 个班级`
    ElMessage.success('自习课填充完成')
  } catch (e: any) {
    ElMessage.error(e?.message || '填充失败')
  } finally {
    filling.value = false
  }
}

async function handleClear() {
  if (!props.semesterId) return
  try {
    await ElMessageBox.confirm('确定清除本学期所有自习课？', '清除确认', { type: 'warning' })
  } catch { return }

  clearing.value = true
  result.value = ''
  try {
    const res = await selfStudyApi.clear(props.semesterId)
    const data = (res as any).data || res
    result.value = `已清除 ${data.cleared ?? 0} 条自习课`
    ElMessage.success('自习课已清除')
  } catch (e: any) {
    ElMessage.error(e?.message || '清除失败')
  } finally {
    clearing.value = false
  }
}
</script>

<style scoped>
.wb-card { border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; overflow: hidden; }
.wb-card-header { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid #f3f4f6; }
.wb-card-title { font-size: 13px; font-weight: 600; color: #111827; }
</style>
