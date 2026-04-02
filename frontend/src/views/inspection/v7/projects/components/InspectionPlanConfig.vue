<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Trash2, Play, Pencil, Calendar, Users, Zap, Clock, X } from 'lucide-vue-next'
import { inspPlanApi } from '@/api/insp/project'
import type { InspectionPlan, CreatePlanRequest } from '@/types/insp/template'

const props = defineProps<{
  projectId: number
  sections?: Array<{ id: number; sectionName: string }>
  inspectors?: Array<{ userId: number | string; userName: string }>
}>()

const loading = ref(false)
const plans = ref<InspectionPlan[]>([])
const dialogVisible = ref(false)
const editingPlan = ref<InspectionPlan | null>(null)
const saving = ref(false)

type FreqMode = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'ON_DEMAND'

const WEEKDAYS = [
  { v: 1, l: '一' }, { v: 2, l: '二' }, { v: 3, l: '三' }, { v: 4, l: '四' },
  { v: 5, l: '五' }, { v: 6, l: '六' }, { v: 7, l: '日' },
]
const MONTH_DAYS = Array.from({ length: 31 }, (_, i) => i + 1)

const form = ref({
  planName: '',
  sectionIds: [] as number[],
  freqMode: 'DAILY' as FreqMode,
  frequency: 1,
  weekDays: [] as number[],
  monthDays: [] as number[],
  timeSlots: [] as Array<{ start: string; end: string }>,
  skipHolidays: false,
  inspectorIds: [] as number[],
})

// ── Helpers ──
const sectionOptions = computed(() =>
  (props.sections || []).map(s => ({ value: Number(s.id), label: s.sectionName }))
)
const sectionMap = computed(() => {
  const m = new Map<number, string>()
  for (const s of (props.sections || [])) m.set(Number(s.id), s.sectionName)
  return m
})

function toggleArray<T>(arr: T[], val: T) {
  const idx = arr.indexOf(val)
  idx >= 0 ? arr.splice(idx, 1) : arr.push(val)
}

function addTimeSlot() {
  form.value.timeSlots.push({ start: '08:00', end: '09:00' })
}

function removeTimeSlot(i: number) {
  form.value.timeSlots.splice(i, 1)
}

// ── Format for display ──
function fmtSections(json: string): string {
  try {
    const ids: number[] = JSON.parse(json)
    if (!ids.length) return '全部分区'
    return ids.map(id => sectionMap.value.get(id) || `#${id}`).join('、')
  } catch { return '全部分区' }
}

function fmtInspectors(json: string | null): string {
  if (!json) return '全员可领取'
  try {
    const ids: number[] = JSON.parse(json)
    if (!ids.length) return '全员可领取'
    return ids.map(id => {
      const i = (props.inspectors || []).find(x => String(x.userId) === String(id))
      return i ? i.userName : `#${id}`
    }).join('、')
  } catch { return '全员可领取' }
}

function fmtSchedule(plan: InspectionPlan): string {
  if (plan.scheduleMode === 'ON_DEMAND') return '不定期（手动触发）'
  let s = ''
  const freq = plan.frequency > 1 ? ` ${plan.frequency}次` : ''
  if (plan.cycleType === 'DAILY') s = '每天' + freq
  else if (plan.cycleType === 'WEEKLY') {
    try {
      const days: number[] = JSON.parse(plan.scheduleDays || '[]')
      s = '每周' + (days.length ? days.map(d => WEEKDAYS.find(w => w.v === d)?.l || d).join('、') : '')
    } catch { s = '每周' }
  } else if (plan.cycleType === 'MONTHLY') {
    try {
      const days: number[] = JSON.parse(plan.scheduleDays || '[]')
      s = '每月' + (days.length ? days.map(d => d + '日').join('、') : '')
    } catch { s = '每月' }
  }
  if (plan.timeSlots) {
    try {
      const slots = JSON.parse(plan.timeSlots)
      if (Array.isArray(slots) && slots.length) {
        s += ' ' + slots.map((t: any) => `${t.start}-${t.end}`).join(' / ')
      }
    } catch {}
  }
  return s
}

// ── CRUD ──
async function loadPlans() {
  loading.value = true
  try { plans.value = await inspPlanApi.list(props.projectId) }
  catch (e: any) { ElMessage.error(e.message || '加载失败') }
  finally { loading.value = false }
}

function openAdd() {
  editingPlan.value = null
  form.value = {
    planName: '', sectionIds: [], freqMode: 'DAILY', frequency: 1,
    weekDays: [], monthDays: [], timeSlots: [],
    skipHolidays: false, inspectorIds: [],
  }
  dialogVisible.value = true
}

function openEdit(plan: InspectionPlan) {
  editingPlan.value = plan
  let sectionIds: number[] = [], inspectorIds: number[] = []
  let weekDays: number[] = [], monthDays: number[] = []
  let timeSlots: Array<{ start: string; end: string }> = []
  try { sectionIds = JSON.parse(plan.sectionIds || '[]') } catch {}
  try { inspectorIds = JSON.parse(plan.inspectorIds || '[]') } catch {}
  try {
    const days: number[] = JSON.parse(plan.scheduleDays || '[]')
    if (plan.cycleType === 'WEEKLY') weekDays = days
    else if (plan.cycleType === 'MONTHLY') monthDays = days
  } catch {}
  try {
    const raw = JSON.parse(plan.timeSlots || '[]')
    if (Array.isArray(raw)) timeSlots = raw.map((t: any) => ({ start: t.start || '', end: t.end || '' }))
  } catch {}
  let freqMode: FreqMode = 'DAILY'
  if (plan.scheduleMode === 'ON_DEMAND') freqMode = 'ON_DEMAND'
  else if (plan.cycleType === 'WEEKLY') freqMode = 'WEEKLY'
  else if (plan.cycleType === 'MONTHLY') freqMode = 'MONTHLY'
  form.value = { planName: plan.planName, sectionIds, freqMode, frequency: plan.frequency || 1, weekDays, monthDays, timeSlots, skipHolidays: plan.skipHolidays, inspectorIds }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.planName.trim()) { ElMessage.warning('请输入计划名称'); return }
  saving.value = true
  try {
    const fm = form.value.freqMode
    const data = {
      planName: form.value.planName,
      sectionIds: JSON.stringify(form.value.sectionIds),
      scheduleMode: fm === 'ON_DEMAND' ? 'ON_DEMAND' : 'REGULAR',
      cycleType: fm === 'ON_DEMAND' ? 'DAILY' : fm,
      frequency: form.value.frequency,
      scheduleDays: fm === 'WEEKLY' ? JSON.stringify(form.value.weekDays) :
                    fm === 'MONTHLY' ? JSON.stringify(form.value.monthDays) : undefined,
      timeSlots: form.value.timeSlots.length ? JSON.stringify(form.value.timeSlots) : undefined,
      skipHolidays: form.value.skipHolidays,
      inspectorIds: form.value.inspectorIds.length ? JSON.stringify(form.value.inspectorIds) : undefined,
    }
    if (editingPlan.value) {
      await inspPlanApi.update(editingPlan.value.id, data)
      ElMessage.success('已更新')
    } else {
      await inspPlanApi.create({ ...data, projectId: props.projectId } as CreatePlanRequest)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    loadPlans()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
  finally { saving.value = false }
}

async function handleDelete(plan: InspectionPlan) {
  try {
    await ElMessageBox.confirm(`删除「${plan.planName}」？`, '确认', { type: 'warning' })
    await inspPlanApi.delete(plan.id)
    ElMessage.success('已删除')
    loadPlans()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
}

async function handleToggle(plan: InspectionPlan) {
  try {
    plan.isEnabled ? await inspPlanApi.disable(plan.id) : await inspPlanApi.enable(plan.id)
    ElMessage.success(plan.isEnabled ? '已禁用' : '已启用')
    loadPlans()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
}

async function handleTrigger(plan: InspectionPlan) {
  try {
    await ElMessageBox.confirm(`立即触发「${plan.planName}」生成检查任务？`, '确认', { type: 'info' })
    await inspPlanApi.trigger(plan.id)
    ElMessage.success('任务已生成')
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '触发失败') }
}

onMounted(() => loadPlans())
</script>

<template>
  <div class="ipc" v-loading="loading">
    <div class="ipc-head">
      <div class="ipc-head-left">
        <Calendar class="w-4 h-4" style="color:#8b5cf6" />
        <span class="ipc-title">检查计划</span>
        <span v-if="plans.length" class="ipc-count">{{ plans.length }}</span>
      </div>
      <button class="ipc-add-btn" @click="openAdd"><Plus class="w-3.5 h-3.5" /> 添加计划</button>
    </div>

    <!-- Empty -->
    <div v-if="!plans.length && !loading" class="ipc-empty">
      <Calendar class="w-8 h-8" style="color:#ddd8fe" />
      <p>暂无检查计划</p>
      <span>添加计划来设定检查频率和分工</span>
    </div>

    <!-- Plan cards -->
    <div class="ipc-list">
      <div v-for="plan in plans" :key="plan.id" class="ipc-card" :class="{ off: !plan.isEnabled }">
        <div class="ipc-card-row1">
          <span class="ipc-card-name">{{ plan.planName }}</span>
          <div class="ipc-pills">
            <span class="ipc-pill" :class="plan.scheduleMode === 'ON_DEMAND' ? 'amber' : 'purple'">
              {{ plan.scheduleMode === 'ON_DEMAND' ? '手动' : '定期' }}
            </span>
            <span class="ipc-pill" :class="plan.isEnabled ? 'green' : 'gray'">
              {{ plan.isEnabled ? '启用' : '禁用' }}
            </span>
          </div>
          <div class="ipc-card-ops">
            <button class="ipc-op" @click="handleToggle(plan)" :title="plan.isEnabled ? '禁用' : '启用'"><Zap class="w-3.5 h-3.5" /></button>
            <button v-if="plan.scheduleMode==='ON_DEMAND'" class="ipc-op accent" @click="handleTrigger(plan)" title="触发"><Play class="w-3.5 h-3.5" /></button>
            <button class="ipc-op" @click="openEdit(plan)" title="编辑"><Pencil class="w-3.5 h-3.5" /></button>
            <button class="ipc-op danger" @click="handleDelete(plan)" title="删除"><Trash2 class="w-3.5 h-3.5" /></button>
          </div>
        </div>
        <div class="ipc-card-row2">
          <span><Clock class="w-3 h-3" /> {{ fmtSchedule(plan) }}</span>
          <span class="ipc-dot" />
          <span>{{ fmtSections(plan.sectionIds) }}</span>
          <span class="ipc-dot" />
          <span><Users class="w-3 h-3" /> {{ fmtInspectors(plan.inspectorIds) }}</span>
          <span v-if="plan.skipHolidays" class="ipc-dot" />
          <span v-if="plan.skipHolidays" style="color:#d97706">跳过节假日</span>
        </div>
      </div>
    </div>

    <!-- ─── Dialog ─── -->
    <el-dialog v-model="dialogVisible" :title="editingPlan ? '编辑计划' : '添加计划'" width="540px" :close-on-click-modal="false" class="ipc-dlg">
      <div class="fd">
        <!-- Name -->
        <div class="fd-block">
          <label class="fd-lbl">计划名称 <b>*</b></label>
          <input v-model="form.planName" class="fd-input" placeholder="如：每日卫生检查" />
        </div>

        <!-- Sections -->
        <div class="fd-block">
          <label class="fd-lbl">检查分区 <span class="fd-sub">不选=全部</span></label>
          <div class="fd-pills">
            <button v-for="o in sectionOptions" :key="o.value"
              class="fd-pill" :class="{ on: form.sectionIds.includes(o.value) }"
              @click="toggleArray(form.sectionIds, o.value)">
              {{ o.label }}
            </button>
          </div>
          <div v-if="!sectionOptions.length" class="fd-empty">项目暂无分区</div>
        </div>

        <!-- Frequency -->
        <div class="fd-block">
          <label class="fd-lbl">检查频率</label>
          <div class="fd-freq">
            <button class="fd-freq-btn" :class="{ on: form.freqMode === 'DAILY' }" @click="form.freqMode = 'DAILY'">
              <Calendar class="w-4 h-4" /><span>每天</span>
            </button>
            <button class="fd-freq-btn" :class="{ on: form.freqMode === 'WEEKLY' }" @click="form.freqMode = 'WEEKLY'">
              <Calendar class="w-4 h-4" /><span>按周</span>
            </button>
            <button class="fd-freq-btn" :class="{ on: form.freqMode === 'MONTHLY' }" @click="form.freqMode = 'MONTHLY'">
              <Calendar class="w-4 h-4" /><span>按月</span>
            </button>
            <button class="fd-freq-btn" :class="{ on: form.freqMode === 'ON_DEMAND' }" @click="form.freqMode = 'ON_DEMAND'">
              <Zap class="w-4 h-4" /><span>不定期</span>
            </button>
          </div>
        </div>

        <!-- Weekly day picker -->
        <div v-if="form.freqMode === 'WEEKLY'" class="fd-block">
          <label class="fd-lbl">选择星期</label>
          <div class="fd-week">
            <button v-for="d in WEEKDAYS" :key="d.v"
              class="fd-wday" :class="{ on: form.weekDays.includes(d.v) }"
              @click="toggleArray(form.weekDays, d.v)">
              {{ d.l }}
            </button>
          </div>
        </div>

        <!-- Monthly day picker -->
        <div v-if="form.freqMode === 'MONTHLY'" class="fd-block">
          <label class="fd-lbl">选择日期</label>
          <div class="fd-month">
            <button v-for="d in MONTH_DAYS" :key="d"
              class="fd-mday" :class="{ on: form.monthDays.includes(d) }"
              @click="toggleArray(form.monthDays, d)">
              {{ d }}
            </button>
          </div>
        </div>

        <!-- Frequency per day -->
        <div v-if="form.freqMode !== 'ON_DEMAND'" class="fd-block">
          <label class="fd-lbl">每天检查次数</label>
          <div class="fd-freq-count">
            <button v-for="n in [1,2,3,4,5]" :key="n"
              class="fd-fc-btn" :class="{ on: form.frequency === n }"
              @click="form.frequency = n">
              {{ n }}次
            </button>
          </div>
        </div>

        <!-- Time slots (optional) -->
        <div v-if="form.freqMode !== 'ON_DEMAND'" class="fd-block">
          <label class="fd-lbl">固定时段 <span class="fd-sub">可选，不设则为不定时抽查</span></label>
          <div class="fd-slots">
            <div v-for="(slot, i) in form.timeSlots" :key="i" class="fd-slot">
              <input v-model="slot.start" type="time" class="fd-time" />
              <span class="fd-time-sep">—</span>
              <input v-model="slot.end" type="time" class="fd-time" />
              <button class="fd-slot-del" @click="removeTimeSlot(i)"><X class="w-3 h-3" /></button>
            </div>
            <button class="fd-slot-add" @click="addTimeSlot"><Plus class="w-3 h-3" /> 添加时段</button>
          </div>
        </div>

        <!-- Skip holidays -->
        <label v-if="form.freqMode !== 'ON_DEMAND'" class="fd-check-row">
          <input type="checkbox" v-model="form.skipHolidays" />
          <span>跳过节假日</span>
        </label>

        <!-- Inspectors -->
        <div v-if="(props.inspectors || []).length > 0" class="fd-block">
          <label class="fd-lbl">指定检查员 <span class="fd-sub">不选=全员可领取</span></label>
          <div class="fd-pills">
            <button v-for="insp in props.inspectors" :key="Number(insp.userId)"
              class="fd-pill" :class="{ on: form.inspectorIds.includes(Number(insp.userId)) }"
              @click="toggleArray(form.inspectorIds, Number(insp.userId))">
              {{ insp.userName }}
            </button>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="fd-footer">
          <button class="fd-btn ghost" @click="dialogVisible = false">取消</button>
          <button class="fd-btn primary" :disabled="saving" @click="handleSave">
            {{ saving ? '保存中...' : (editingPlan ? '更新' : '创建') }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ── Layout ── */
.ipc { min-height: 80px; }
.ipc-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.ipc-head-left { display: flex; align-items: center; gap: 8px; }
.ipc-title { font-size: 14px; font-weight: 700; color: #1e1b4b; }
.ipc-count { font-size: 10px; color: #8b5cf6; background: #f3f0ff; padding: 1px 7px; border-radius: 10px; font-weight: 700; }

.ipc-add-btn {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 6px 14px; border-radius: 8px; font-size: 12px; font-weight: 600;
  background: #8b5cf6; color: #fff; border: none; cursor: pointer; transition: background 0.15s;
}
.ipc-add-btn:hover { background: #7c3aed; }

.ipc-empty { text-align: center; padding: 36px 0; }
.ipc-empty p { font-size: 13px; color: #9ca3af; margin: 8px 0 2px; }
.ipc-empty span { font-size: 11px; color: #d1d5db; }

/* ── Card ── */
.ipc-list { display: flex; flex-direction: column; gap: 8px; }
.ipc-card {
  border: 1px solid #e5e7eb; border-radius: 10px; background: #fff;
  padding: 10px 14px; transition: all 0.15s;
}
.ipc-card:hover { box-shadow: 0 2px 8px rgba(139,92,246,0.06); }
.ipc-card.off { opacity: 0.45; }

.ipc-card-row1 { display: flex; align-items: center; gap: 6px; margin-bottom: 5px; }
.ipc-card-name { font-size: 13px; font-weight: 700; color: #1e1b4b; }
.ipc-pills { display: flex; gap: 4px; flex: 1; }
.ipc-pill { font-size: 9px; font-weight: 700; padding: 1px 6px; border-radius: 4px; text-transform: uppercase; letter-spacing: 0.3px; }
.ipc-pill.purple { background: #f3f0ff; color: #8b5cf6; }
.ipc-pill.amber { background: #fffbeb; color: #d97706; }
.ipc-pill.green { background: #f0fdf4; color: #16a34a; }
.ipc-pill.gray { background: #f3f4f6; color: #9ca3af; }

.ipc-card-ops { display: flex; gap: 1px; }
.ipc-op {
  width: 26px; height: 26px; border: none; border-radius: 6px;
  background: transparent; color: #9ca3af; cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center; transition: all 0.15s;
}
.ipc-op:hover { background: #f3f4f6; color: #374151; }
.ipc-op.accent:hover { background: #f3f0ff; color: #8b5cf6; }
.ipc-op.danger:hover { background: #fef2f2; color: #ef4444; }

.ipc-card-row2 { display: flex; align-items: center; gap: 5px; font-size: 11px; color: #9ca3af; flex-wrap: wrap; }
.ipc-dot { width: 2px; height: 2px; border-radius: 50%; background: #d1d5db; }

/* ── Dialog form ── */
.fd { display: flex; flex-direction: column; gap: 18px; }
.fd-block { display: flex; flex-direction: column; gap: 6px; }
.fd-lbl { font-size: 12px; font-weight: 700; color: #374151; display: flex; align-items: center; gap: 6px; }
.fd-lbl b { color: #ef4444; font-weight: 700; }
.fd-sub { font-size: 10px; font-weight: 400; color: #b0b0b0; }
.fd-empty { font-size: 11px; color: #d1d5db; }

.fd-input {
  width: 100%; padding: 9px 12px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  font-size: 13px; color: #1f2937; outline: none; transition: border-color 0.15s;
}
.fd-input:focus { border-color: #8b5cf6; }
.fd-input::placeholder { color: #d1d5db; }

/* Pills (sections / inspectors) */
.fd-pills { display: flex; flex-wrap: wrap; gap: 6px; }
.fd-pill {
  padding: 5px 12px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  font-size: 12px; font-weight: 500; color: #6b7280; background: #fff;
  cursor: pointer; transition: all 0.15s; user-select: none;
}
.fd-pill:hover { border-color: #c4b5fd; }
.fd-pill.on { border-color: #8b5cf6; background: #f5f3ff; color: #6d28d9; font-weight: 600; }

/* Frequency buttons */
.fd-freq { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; }
.fd-freq-btn {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 12px 8px; border: 1.5px solid #e5e7eb; border-radius: 10px;
  background: #fff; color: #6b7280; cursor: pointer; transition: all 0.15s;
  font-size: 12px; font-weight: 600;
}
.fd-freq-btn:hover { border-color: #c4b5fd; }
.fd-freq-btn.on { border-color: #8b5cf6; background: #f5f3ff; color: #6d28d9; }

/* Frequency count */
.fd-freq-count { display: flex; gap: 4px; }
.fd-fc-btn {
  padding: 5px 14px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  font-size: 12px; font-weight: 600; color: #6b7280; background: #fff;
  cursor: pointer; transition: all 0.12s;
}
.fd-fc-btn:hover { border-color: #c4b5fd; }
.fd-fc-btn.on { border-color: #8b5cf6; background: #8b5cf6; color: #fff; }

/* Week day picker */
.fd-week { display: flex; gap: 4px; }
.fd-wday {
  width: 38px; height: 34px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  font-size: 12px; font-weight: 600; color: #6b7280; background: #fff;
  cursor: pointer; transition: all 0.12s;
}
.fd-wday:hover { border-color: #c4b5fd; }
.fd-wday.on { border-color: #8b5cf6; background: #8b5cf6; color: #fff; }

/* Month day grid */
.fd-month {
  display: grid; grid-template-columns: repeat(7, 1fr); gap: 3px;
}
.fd-mday {
  height: 30px; border: 1px solid #e5e7eb; border-radius: 6px;
  font-size: 11px; font-weight: 600; color: #6b7280; background: #fff;
  cursor: pointer; transition: all 0.12s;
}
.fd-mday:hover { border-color: #c4b5fd; }
.fd-mday.on { border-color: #8b5cf6; background: #8b5cf6; color: #fff; }

/* Time slots */
.fd-slots { display: flex; flex-direction: column; gap: 6px; }
.fd-slot { display: flex; align-items: center; gap: 6px; }
.fd-time {
  padding: 6px 10px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  font-size: 13px; color: #1f2937; outline: none; width: 110px;
}
.fd-time:focus { border-color: #8b5cf6; }
.fd-time-sep { color: #d1d5db; font-size: 12px; }
.fd-slot-del {
  width: 24px; height: 24px; border: none; border-radius: 6px;
  background: transparent; color: #d1d5db; cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.12s;
}
.fd-slot-del:hover { background: #fef2f2; color: #ef4444; }
.fd-slot-add {
  display: inline-flex; align-items: center; gap: 4px; align-self: flex-start;
  padding: 4px 10px; border: 1px dashed #e5e7eb; border-radius: 6px;
  font-size: 11px; color: #9ca3af; background: none; cursor: pointer; transition: all 0.12s;
}
.fd-slot-add:hover { color: #8b5cf6; border-color: #c4b5fd; background: #f5f3ff; }

/* Checkbox */
.fd-check-row {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 12px; color: #374151; cursor: pointer; user-select: none;
}

/* Footer */
.fd-footer { display: flex; justify-content: flex-end; gap: 8px; }
.fd-btn {
  padding: 8px 20px; border-radius: 8px; font-size: 13px; font-weight: 600;
  border: none; cursor: pointer; transition: all 0.15s;
}
.fd-btn.ghost { background: #f3f4f6; color: #6b7280; }
.fd-btn.ghost:hover { background: #e5e7eb; }
.fd-btn.primary { background: #8b5cf6; color: #fff; }
.fd-btn.primary:hover { background: #7c3aed; }
.fd-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
