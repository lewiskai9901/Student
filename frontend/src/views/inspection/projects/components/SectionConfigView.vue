<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Trash2, Pencil, Calendar, Users, Clock, X, Zap, Play, AlertTriangle,
} from 'lucide-vue-next'
import { inspPlanApi, getProject } from '@/api/inspection/project'
import {
  getIndicators,
} from '@/api/inspection/indicator'
import { getGradeSchemes } from '@/api/inspection/gradeScheme'
import type { InspectionPlan, CreatePlanRequest } from '@/types/insp/template'
import type { Indicator } from '@/types/insp/indicator'
import type { GradeScheme } from '@/types/insp/gradeScheme'

import { type SectionTreeNode } from '@/utils/sectionTree'

// ══════════════════════════════════════════════
//  Props
// ══════════════════════════════════════════════

const props = defineProps<{
  projectId: LongId
  sections: Array<{ id: LongId; sectionName: string; targetType?: string }>
  inspectors: Array<{ userId: LongId | string; userName: string }>
  sectionTree?: SectionTreeNode[]
  rootSectionId?: LongId | string | null
  rootSectionName?: string
  // P1 升级: 父组件传入项目任务列表用于计算检查计划运营数据
  projectTasks?: Array<{ id: LongId | string; inspectionPlanId?: LongId | string | null; status?: string; taskDate?: string }>
}>()

// 检查计划运营数据 (按 inspectionPlanId 聚合任务)
function planStats(planId: LongId | string) {
  const tasks = (props.projectTasks || []).filter(t => t.inspectionPlanId === planId)
  if (tasks.length === 0) return null
  const today = new Date().toISOString().slice(0, 10)
  return {
    total: tasks.length,
    done: tasks.filter(t => ['REVIEWED','PUBLISHED','SUBMITTED','UNDER_REVIEW'].includes(t.status || '')).length,
    overdue: tasks.filter(t => t.taskDate && t.taskDate < today &&
      !['REVIEWED','PUBLISHED','CANCELLED','EXPIRED'].includes(t.status || '')).length,
    lastTaskDate: tasks.map(t => t.taskDate).filter(Boolean).sort().reverse()[0] || null,
    nextTaskDate: tasks.map(t => t.taskDate).filter(d => d && d >= today).sort()[0] || null,
  }
}

// ══════════════════════════════════════════════
//  State
// ══════════════════════════════════════════════

const loading = ref(false)
const plans = ref<InspectionPlan[]>([])
const indicators = ref<Indicator[]>([])
const gradeSchemes = ref<GradeScheme[]>([])
const targetCount = ref(0) // 检查目标数量

// 检查计划可用检查员数 — inspectorIds 为空表示项目全员 (此时不校验上限)
const scheduleAvailableRaters = computed(() => scheduleForm.value.inspectorIds.length)
// 实时校验: 选了具体检查员时, 每目标评分人数不得超过可用检查员数
const ratersExceedsAvailable = computed(() =>
  scheduleAvailableRaters.value > 0 &&
  scheduleForm.value.ratersPerTarget > scheduleAvailableRaters.value,
)

// ── Schedule Dialog ──
const scheduleDialogVisible = ref(false)
const editingPlan = ref<InspectionPlan | null>(null)
const scheduleSaving = ref(false)

type FreqMode = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'ON_DEMAND'

const WEEKDAYS = [
  { v: 1, l: '一' }, { v: 2, l: '二' }, { v: 3, l: '三' }, { v: 4, l: '四' },
  { v: 5, l: '五' }, { v: 6, l: '六' }, { v: 7, l: '日' },
]
const MONTH_DAYS = Array.from({ length: 31 }, (_, i) => i + 1)

const scheduleForm = ref({
  planName: '',
  sectionIds: [] as LongId[],
  freqMode: 'DAILY' as FreqMode,
  frequency: 1,
  weekDays: [] as number[],
  monthDays: [] as number[],
  timeSlots: [] as Array<{ start: string; end: string }>,
  skipHolidays: false,
  inspectorIds: [] as LongId[],
  ratersPerTarget: 1,
})


// ══════════════════════════════════════════════
//  Helpers
// ══════════════════════════════════════════════

function toggleArray<T>(arr: T[], val: T) {
  const idx = arr.indexOf(val)
  idx >= 0 ? arr.splice(idx, 1) : arr.push(val)
}

const sectionMap = computed(() => {
  const m = new Map<LongId, string>()
  for (const s of props.sections) m.set(s.id, s.sectionName)
  return m
})

const inspectorMap = computed(() => {
  const m = new Map<LongId, string>()
  for (const i of (props.inspectors || [])) m.set(i.userId, i.userName)
  return m
})

function parsePlanSectionIds(plan: InspectionPlan): LongId[] {
  try { return (JSON.parse(plan.sectionIds || '[]') as Array<string | number>).map(String) } catch { return [] }
}

function parsePlanInspectorIds(plan: InspectionPlan): LongId[] {
  try { return (JSON.parse(plan.inspectorIds || '[]') as Array<string | number>).map(String) } catch { return [] }
}

// ── Display formatters ──
function fmtSchedule(plan: InspectionPlan): string {
  if (plan.scheduleMode === 'ON_DEMAND') return '不定期（手动触发）'
  let s = ''
  const freq = plan.frequency > 1 ? ` ${plan.frequency}次` : ''
  if (plan.cycleType === 'DAILY') s = '每天' + freq
  else if (plan.cycleType === 'WEEKLY') {
    try {
      const days: number[] = JSON.parse(plan.scheduleDays || '[]')
      s = '每周' + (days.length ? days.map(d => WEEKDAYS.find(w => w.v === d)?.l || d).join('、') : '') + freq
    } catch { s = '每周' + freq }
  } else if (plan.cycleType === 'MONTHLY') {
    try {
      const days: number[] = JSON.parse(plan.scheduleDays || '[]')
      s = '每月' + (days.length ? days.map(d => d + '日').join('、') : '') + freq
    } catch { s = '每月' + freq }
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

function fmtSections(plan: InspectionPlan): string {
  const ids = parsePlanSectionIds(plan)
  if (!ids.length) return '全部分区'
  return ids.map(id => sectionMap.value.get(id) || `#${id}`).join('、')
}

function fmtInspectors(plan: InspectionPlan): string {
  const ids = parsePlanInspectorIds(plan)
  if (!ids.length) return '全员可领取'
  return ids.map(id => inspectorMap.value.get(id) || `#${id}`).join('、')
}


// ══════════════════════════════════════════════
//  Data Loading
// ══════════════════════════════════════════════

async function loadAll() {
  loading.value = true
  try {
    const [p, ind, gs, proj] = await Promise.all([
      inspPlanApi.list(props.projectId),
      getIndicators(props.projectId),
      getGradeSchemes(),
      getProject(props.projectId),
    ])
    plans.value = p
    indicators.value = ind
    gradeSchemes.value = gs
    // 从项目 scopeConfig 获取目标数量
    if (proj.scopeConfig) {
      try { targetCount.value = JSON.parse(proj.scopeConfig).length } catch { targetCount.value = 0 }
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

watch(() => props.projectId, () => { if (props.projectId) loadAll() })
onMounted(() => { if (props.projectId) loadAll() })

// ══════════════════════════════════════════════
//  Schedule Group Dialog
// ══════════════════════════════════════════════

function openAddSchedule() {
  editingPlan.value = null
  scheduleForm.value = {
    planName: '', sectionIds: [], freqMode: 'DAILY', frequency: 1,
    weekDays: [], monthDays: [], timeSlots: [],
    skipHolidays: false, inspectorIds: [],
    ratersPerTarget: 1,
  }
  scheduleDialogVisible.value = true
}

function openEditSchedule(plan: InspectionPlan) {
  editingPlan.value = plan
  let sectionIds: LongId[] = [], inspectorIds: LongId[] = []
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
  scheduleForm.value = {
    planName: plan.planName, sectionIds, freqMode, frequency: plan.frequency || 1,
    weekDays, monthDays, timeSlots, skipHolidays: plan.skipHolidays, inspectorIds,
    ratersPerTarget: plan.ratersPerTarget ?? 1,
  }
  scheduleDialogVisible.value = true
}

async function handleSaveSchedule() {
  if (!scheduleForm.value.planName.trim()) { ElMessage.warning('请输入检查计划名称'); return }
  // P1 #10: 按 freqMode 校验周/月选择非空, 否则会产生永不触发的空调度
  if (scheduleForm.value.freqMode === 'WEEKLY' && scheduleForm.value.weekDays.length === 0) {
    ElMessage.warning('按周检查需至少选择一个星期'); return
  }
  if (scheduleForm.value.freqMode === 'MONTHLY' && scheduleForm.value.monthDays.length === 0) {
    ElMessage.warning('按月检查需至少选择一个日期'); return
  }
  // 每目标评分人数不得超过检查计划可用检查员数 (指定了检查员时)
  if (ratersExceedsAvailable.value) {
    ElMessage.warning('每目标评分人数不能超过已指定的检查员数量'); return
  }
  scheduleSaving.value = true
  try {
    const fm = scheduleForm.value.freqMode
    const data = {
      planName: scheduleForm.value.planName,
      sectionIds: JSON.stringify(scheduleForm.value.sectionIds),
      scheduleMode: fm === 'ON_DEMAND' ? 'ON_DEMAND' : 'REGULAR',
      cycleType: fm === 'ON_DEMAND' ? 'DAILY' : fm,
      frequency: scheduleForm.value.frequency,
      scheduleDays: fm === 'WEEKLY' ? JSON.stringify(scheduleForm.value.weekDays) :
                    fm === 'MONTHLY' ? JSON.stringify(scheduleForm.value.monthDays) : undefined,
      timeSlots: scheduleForm.value.timeSlots.length ? JSON.stringify(scheduleForm.value.timeSlots) : undefined,
      skipHolidays: scheduleForm.value.skipHolidays,
      inspectorIds: scheduleForm.value.inspectorIds.length ? JSON.stringify(scheduleForm.value.inspectorIds) : undefined,
      ratersPerTarget: scheduleForm.value.ratersPerTarget,
    }
    if (editingPlan.value) {
      await inspPlanApi.update(editingPlan.value.id, data)
      ElMessage.success('已更新')
    } else {
      await inspPlanApi.create({ ...data, projectId: props.projectId } as CreatePlanRequest)
      ElMessage.success('已创建')
    }
    scheduleDialogVisible.value = false
    await loadAll()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
  finally { scheduleSaving.value = false }
}

async function handleDeleteSchedule(plan: InspectionPlan) {
  try {
    await ElMessageBox.confirm(`删除「${plan.planName}」？`, '确认', { type: 'warning' })
    await inspPlanApi.delete(plan.id)
    ElMessage.success('已删除')
    await loadAll()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
}

async function handleToggleSchedule(plan: InspectionPlan) {
  try {
    plan.isEnabled ? await inspPlanApi.disable(plan.id) : await inspPlanApi.enable(plan.id)
    ElMessage.success(plan.isEnabled ? '已禁用' : '已启用')
    await loadAll()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
}

async function handleTriggerSchedule(plan: InspectionPlan) {
  try {
    await ElMessageBox.confirm(`立即触发「${plan.planName}」生成检查任务？`, '确认', { type: 'info' })
    await inspPlanApi.trigger(plan.id)
    ElMessage.success('任务已生成')
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '触发失败') }
}

function addTimeSlot() {
  scheduleForm.value.timeSlots.push({ start: '08:00', end: '09:00' })
}

function removeTimeSlot(i: number) {
  scheduleForm.value.timeSlots.splice(i, 1)
}

// ══════════════════════════════════════════════
//  Evaluation Dialog (Leaf Indicator per Section)
// ══════════════════════════════════════════════

// ══════════════════════════════════════════════

defineExpose({ reload: loadAll })
</script>

<template>
  <div class="scv" v-loading="loading">

    <!-- ═══════════════════════════════════════════ -->
    <!--  Section 1: Schedule Groups                -->
    <!-- ═══════════════════════════════════════════ -->
    <div class="scv-section">
      <div class="scv-head">
        <div class="scv-head-left">
          <Calendar class="w-4 h-4" style="color:var(--insp-accent)" />
          <span class="scv-title">检查计划</span>
          <span v-if="plans.length" class="scv-count">{{ plans.length }}</span>
        </div>
        <button class="scv-add-btn" @click="openAddSchedule">
          <Plus class="w-3.5 h-3.5" /> 添加检查计划
        </button>
      </div>

      <!-- Empty -->
      <div v-if="!plans.length && !loading" class="scv-empty-inline">
        <AlertTriangle class="w-4 h-4" style="color:#d97706" />
        <span>暂未配置检查调度，添加检查计划来安排检查频率和分工</span>
      </div>

      <!-- Plan cards -->
      <div class="scv-schedule-list">
        <div v-for="plan in plans" :key="plan.id" class="sc-card" :class="{ off: !plan.isEnabled }">
          <div class="sc-card-top">
            <span class="sc-card-name">{{ plan.planName }}</span>
            <div class="sc-pills">
              <span class="sc-pill" :class="plan.scheduleMode === 'ON_DEMAND' ? 'amber' : 'purple'">
                {{ plan.scheduleMode === 'ON_DEMAND' ? '手动' : '定期' }}
              </span>
              <span class="sc-pill" :class="plan.isEnabled ? 'green' : 'gray'">
                {{ plan.isEnabled ? '启用' : '禁用' }}
              </span>
            </div>
            <div class="sc-card-ops">
              <button class="sc-op" @click="handleToggleSchedule(plan)" :title="plan.isEnabled ? '禁用' : '启用'"><Zap class="w-3.5 h-3.5" /></button>
              <button v-if="plan.scheduleMode==='ON_DEMAND'" class="sc-op accent" @click="handleTriggerSchedule(plan)" title="触发"><Play class="w-3.5 h-3.5" /></button>
              <button class="sc-op" @click="openEditSchedule(plan)" title="编辑"><Pencil class="w-3.5 h-3.5" /></button>
              <button class="sc-op danger" @click="handleDeleteSchedule(plan)" title="删除"><Trash2 class="w-3.5 h-3.5" /></button>
            </div>
          </div>
          <div class="sc-card-info">
            <span><Clock class="w-3 h-3" /> {{ fmtSchedule(plan) }}</span>
            <span class="sc-dot" />
            <span><Users class="w-3 h-3" /> {{ fmtInspectors(plan) }}</span>
            <span v-if="plan.skipHolidays" class="sc-dot" />
            <span v-if="plan.skipHolidays" style="color:#d97706">跳过节假日</span>
          </div>
          <div class="sc-card-sections">
            {{ fmtSections(plan) }}
          </div>
          <!-- P1 升级: 检查计划运营数据 -->
          <div v-if="planStats(plan.id)" class="sc-card-ops-data">
            <div class="sc-data-stat">
              <span class="sc-data-num">{{ planStats(plan.id)!.total }}</span>
              <span class="sc-data-lbl">已生成任务</span>
            </div>
            <div class="sc-data-rule" />
            <div class="sc-data-stat">
              <span class="sc-data-num" style="color: #10b981">{{ planStats(plan.id)!.done }}</span>
              <span class="sc-data-lbl">已完成</span>
            </div>
            <div v-if="planStats(plan.id)!.overdue > 0" class="sc-data-rule" />
            <div v-if="planStats(plan.id)!.overdue > 0" class="sc-data-stat">
              <span class="sc-data-num" style="color: #ef4444">{{ planStats(plan.id)!.overdue }}</span>
              <span class="sc-data-lbl">逾期</span>
            </div>
            <span v-if="planStats(plan.id)!.lastTaskDate" class="sc-data-time">
              <Clock class="w-3 h-3" />
              上次 <b>{{ planStats(plan.id)!.lastTaskDate }}</b>
            </span>
            <span v-if="planStats(plan.id)!.nextTaskDate" class="sc-data-time">
              下次 <b>{{ planStats(plan.id)!.nextTaskDate }}</b>
            </span>
          </div>
          <div v-else-if="!loading" class="sc-card-ops-data sc-card-ops-data--empty">
            <span class="sc-data-empty">暂未生成任务 — 点击右上 > 触发, 或等待定时调度</span>
          </div>
        </div>
      </div>
    </div>


    <!-- ═══════════════════════════════════════════ -->
    <!--  Dialog 1: Schedule Group                  -->
    <!-- ═══════════════════════════════════════════ -->
    <el-dialog v-model="scheduleDialogVisible"
      :title="editingPlan ? '编辑检查计划' : '添加检查计划'"
      width="540px" :close-on-click-modal="false" class="scv-dlg">
      <div class="fd">
        <!-- Name -->
        <div class="fd-block">
          <label class="fd-lbl">检查计划名称 <b>*</b></label>
          <input v-model="scheduleForm.planName" class="fd-input" placeholder="如：每日常规巡查" />
        </div>

        <!-- Sections -->
        <div class="fd-block">
          <label class="fd-lbl">包含分区 <span class="fd-sub">不选=全部</span></label>
          <div class="fd-pills">
            <button v-for="s in props.sections" :key="s.id"
              class="fd-pill" :class="{ on: scheduleForm.sectionIds.includes(s.id) }"
              @click="toggleArray(scheduleForm.sectionIds, s.id)">
              {{ s.sectionName }}
            </button>
          </div>
          <div v-if="!props.sections.length" class="fd-empty">项目暂无分区</div>
        </div>

        <!-- Frequency -->
        <div class="fd-block">
          <label class="fd-lbl">检查频率</label>
          <div class="fd-freq">
            <button class="fd-freq-btn" :class="{ on: scheduleForm.freqMode === 'DAILY' }" @click="scheduleForm.freqMode = 'DAILY'">
              <Calendar class="w-4 h-4" /><span>每天</span>
            </button>
            <button class="fd-freq-btn" :class="{ on: scheduleForm.freqMode === 'WEEKLY' }" @click="scheduleForm.freqMode = 'WEEKLY'">
              <Calendar class="w-4 h-4" /><span>按周</span>
            </button>
            <button class="fd-freq-btn" :class="{ on: scheduleForm.freqMode === 'MONTHLY' }" @click="scheduleForm.freqMode = 'MONTHLY'">
              <Calendar class="w-4 h-4" /><span>按月</span>
            </button>
            <button class="fd-freq-btn" :class="{ on: scheduleForm.freqMode === 'ON_DEMAND' }" @click="scheduleForm.freqMode = 'ON_DEMAND'">
              <Zap class="w-4 h-4" /><span>不定期</span>
            </button>
          </div>
        </div>

        <!-- Weekly day picker -->
        <div v-if="scheduleForm.freqMode === 'WEEKLY'" class="fd-block">
          <label class="fd-lbl">选择星期</label>
          <div class="fd-week">
            <button v-for="d in WEEKDAYS" :key="d.v"
              class="fd-wday" :class="{ on: scheduleForm.weekDays.includes(d.v) }"
              @click="toggleArray(scheduleForm.weekDays, d.v)">
              {{ d.l }}
            </button>
          </div>
        </div>

        <!-- Monthly day picker -->
        <div v-if="scheduleForm.freqMode === 'MONTHLY'" class="fd-block">
          <label class="fd-lbl">选择日期</label>
          <div class="fd-month">
            <button v-for="d in MONTH_DAYS" :key="d"
              class="fd-mday" :class="{ on: scheduleForm.monthDays.includes(d) }"
              @click="toggleArray(scheduleForm.monthDays, d)">
              {{ d }}
            </button>
          </div>
        </div>

        <!-- Frequency per day -->
        <div v-if="scheduleForm.freqMode !== 'ON_DEMAND'" class="fd-block">
          <label class="fd-lbl">每天检查次数</label>
          <div class="fd-freq-count">
            <button v-for="n in [1,2,3,4,5]" :key="n"
              class="fd-fc-btn" :class="{ on: scheduleForm.frequency === n }"
              @click="scheduleForm.frequency = n">
              {{ n }}次
            </button>
          </div>
        </div>

        <!-- Time slots -->
        <div v-if="scheduleForm.freqMode !== 'ON_DEMAND'" class="fd-block">
          <label class="fd-lbl">固定时段 <span class="fd-sub">可选，不设则为不定时抽查</span></label>
          <div class="fd-slots">
            <div v-for="(slot, i) in scheduleForm.timeSlots" :key="i" class="fd-slot">
              <input v-model="slot.start" type="time" class="fd-time" />
              <span class="fd-time-sep">-</span>
              <input v-model="slot.end" type="time" class="fd-time" />
              <button class="fd-slot-del" @click="removeTimeSlot(i)"><X class="w-3 h-3" /></button>
            </div>
            <button class="fd-slot-add" @click="addTimeSlot"><Plus class="w-3 h-3" /> 添加时段</button>
          </div>
        </div>

        <!-- Skip holidays -->
        <label v-if="scheduleForm.freqMode !== 'ON_DEMAND'" class="fd-check-row">
          <input type="checkbox" v-model="scheduleForm.skipHolidays" />
          <span>跳过节假日</span>
        </label>

        <!-- Inspectors -->
        <div v-if="(props.inspectors || []).length > 0" class="fd-block">
          <label class="fd-lbl">指定检查员 <span class="fd-sub">不选=全员可领取</span></label>
          <div class="fd-pills">
            <button v-for="insp in props.inspectors" :key="insp.userId"
              class="fd-pill" :class="{ on: scheduleForm.inspectorIds.includes(insp.userId) }"
              @click="toggleArray(scheduleForm.inspectorIds, insp.userId)">
              {{ insp.userName }}
            </button>
          </div>
        </div>

        <!-- 评分方案下拉已移除 (评级引擎完美架构 Phase 1 删 inspection_plans.scoring_profile_id);
             评级配置改用项目「评级」Tab 的 Indicator 模型. -->

        <!-- Raters per target -->
        <div class="fd-block">
          <label class="fd-lbl">每目标评分人数</label>
          <el-input-number v-model="scheduleForm.ratersPerTarget" :min="1" size="small" />
          <div v-if="ratersExceedsAvailable" class="fd-err">
            不能超过已指定的检查员数量 ({{ scheduleAvailableRaters }} 人)
          </div>
          <div v-else class="fd-sub" style="margin-top: 4px">
            1=单人检查；&gt;1=每个目标由多名检查员分别评分后合并。
          </div>
        </div>
      </div>

      <template #footer>
        <div class="fd-footer">
          <button class="fd-btn ghost" @click="scheduleDialogVisible = false">取消</button>
          <button class="fd-btn primary" :disabled="scheduleSaving || ratersExceedsAvailable" @click="handleSaveSchedule">
            {{ scheduleSaving ? '保存中...' : (editingPlan ? '更新' : '创建') }}
          </button>
        </div>
      </template>
    </el-dialog>


    <!-- Composite dialog removed: eval dialog handles both leaf and intermediate -->

  </div>
</template>

<style scoped src="./SectionConfigView.css"></style>
