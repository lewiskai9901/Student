<template>
  <div>
    <!-- Level Tabs -->
    <div class="tm-tabs" style="border-radius: 10px 10px 0 0; border: 1px solid #e5e7eb; border-bottom: none;">
      <button
        v-for="lvl in CONSTRAINT_LEVELS"
        :key="lvl.value"
        :class="['tm-tab', { active: activeLevel === lvl.value }]"
        @click="switchLevel(lvl.value)"
      >{{ lvl.label }}约束</button>
    </div>

    <!-- Target Selector (level 2-4 only) -->
    <div v-if="activeLevel > 1" class="tm-filters" style="border: 1px solid #e5e7eb; border-top: none; border-bottom: none;">
      <span style="font-size: 12.5px; color: #6b7280;">{{ targetLabel }}：</span>
      <select v-model="targetId" class="tm-select" @change="onTargetChange">
        <option :value="''" disabled>选择{{ targetLabel }}</option>
        <option v-for="t in targetOptions" :key="t.value" :value="t.value">{{ t.label }}</option>
      </select>
      <span v-if="!targetId" style="font-size: 11px; color: #9ca3af;">请先选择{{ targetLabel }}以查看约束</span>
    </div>

    <!-- Stat Bar -->
    <div class="tm-filters" style="border: 1px solid #e5e7eb; border-top: none;">
      <span style="font-size: 12.5px; color: #6b7280;">总约束 <b>{{ constraints.length }}</b></span>
      <i class="tm-sep" />
      <span style="font-size: 12.5px; color: #6b7280;">硬约束 <b>{{ hardConstraints.length }}</b></span>
      <i class="tm-sep" />
      <span style="font-size: 12.5px; color: #6b7280;">软约束 <b>{{ softConstraints.length }}</b></span>
      <i class="tm-sep" />
      <span style="font-size: 12.5px; color: #16a34a;">已启用 <b>{{ enabledCount }}</b></span>
      <span style="flex: 1;" />
      <button
        class="tm-btn tm-btn-primary"
        :disabled="activeLevel > 1 && !targetId"
        @click="openAddDialog"
      >新增约束</button>
    </div>

    <!-- Main Content -->
    <div style="margin-top: 16px;">
      <!-- Loading -->
      <div v-if="loading" style="text-align: center; padding: 50px 0; color: #9ca3af; font-size: 13px;">
        <span class="tm-spin" style="display:inline-block;width:20px;height:20px;border:2px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;" /> 加载中...
      </div>

      <!-- Empty for level 2-4 without target -->
      <div v-else-if="activeLevel > 1 && !targetId" style="text-align: center; padding: 50px 0; color: #9ca3af; font-size: 13px;">
        请先在上方选择{{ targetLabel }}
      </div>

      <!-- Two columns: constraint list + time matrix -->
      <div v-else style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
        <!-- Constraint List -->
        <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; overflow: hidden;">
          <div style="border-bottom: 1px solid #f3f4f6; padding: 10px 16px; font-size: 13px; font-weight: 600; color: #374151;">约束列表</div>
          <div style="max-height: calc(100vh - 400px); overflow-y: auto; padding: 12px;">
            <div v-if="constraints.length === 0" style="text-align: center; padding: 40px 0; color: #9ca3af; font-size: 13px;">暂无约束配置</div>

            <!-- Hard Constraints -->
            <template v-if="hardConstraints.length > 0">
              <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
                <div style="flex: 1; height: 1px; background: #fecaca;" />
                <span style="font-size: 11px; font-weight: 500; color: #dc2626;">硬约束 ({{ hardConstraints.length }})</span>
                <div style="flex: 1; height: 1px; background: #fecaca;" />
              </div>
              <div v-for="c in hardConstraints" :key="c.id" class="cst-card" :class="{ disabled: !c.enabled }">
                <div style="display: flex; align-items: start; justify-content: space-between;">
                  <div style="min-width: 0; flex: 1;">
                    <div style="display: flex; align-items: center; gap: 6px;">
                      <span style="color: #dc2626;">&#9679;</span>
                      <span style="font-size: 13px; font-weight: 500; color: #111827;">{{ c.constraintName }}</span>
                    </div>
                    <div style="margin-top: 4px; font-size: 11px; color: #6b7280;">
                      <span class="tm-chip tm-chip-gray">{{ getTypeLabel(c.constraintType) }}</span>
                      <span v-if="getParamSummary(c)" style="margin-left: 4px;">{{ getParamSummary(c) }}</span>
                    </div>
                  </div>
                  <div style="display: flex; align-items: center; gap: 4px; margin-left: 8px;">
                    <button class="tm-action" @click="openEditDialog(c)">编辑</button>
                    <button class="tm-action tm-action-danger" @click="handleDelete(c)">删除</button>
                    <button class="tm-action" :style="{ color: c.enabled ? '#d97706' : '#16a34a' }" @click="handleToggle(c)">{{ c.enabled ? '禁用' : '启用' }}</button>
                  </div>
                </div>
              </div>
            </template>

            <!-- Soft Constraints -->
            <template v-if="softConstraints.length > 0">
              <div style="display: flex; align-items: center; gap: 8px; margin: 12px 0 8px;">
                <div style="flex: 1; height: 1px; background: #fde68a;" />
                <span style="font-size: 11px; font-weight: 500; color: #d97706;">软约束 ({{ softConstraints.length }})</span>
                <div style="flex: 1; height: 1px; background: #fde68a;" />
              </div>
              <div v-for="c in softConstraints" :key="c.id" class="cst-card" :class="{ disabled: !c.enabled }">
                <div style="display: flex; align-items: start; justify-content: space-between;">
                  <div style="min-width: 0; flex: 1;">
                    <div style="display: flex; align-items: center; gap: 6px;">
                      <span style="color: #d97706;">&#9675;</span>
                      <span style="font-size: 13px; font-weight: 500; color: #111827;">{{ c.constraintName }}</span>
                    </div>
                    <div style="margin-top: 4px; font-size: 11px; color: #6b7280;">
                      <span class="tm-chip tm-chip-gray">{{ getTypeLabel(c.constraintType) }}</span>
                      <span v-if="getParamSummary(c)" style="margin-left: 4px;">{{ getParamSummary(c) }}</span>
                    </div>
                    <div style="margin-top: 6px; display: flex; align-items: center; gap: 6px;">
                      <div style="width: 80px; height: 5px; background: #f3f4f6; border-radius: 99px; overflow: hidden;">
                        <div style="height: 100%; background: #3b82f6; border-radius: 99px;" :style="{ width: c.priority + '%' }" />
                      </div>
                      <span style="font-size: 11px; color: #6b7280;">{{ c.priority }}</span>
                    </div>
                  </div>
                  <div style="display: flex; align-items: center; gap: 4px; margin-left: 8px;">
                    <button class="tm-action" @click="openEditDialog(c)">编辑</button>
                    <button class="tm-action tm-action-danger" @click="handleDelete(c)">删除</button>
                    <button class="tm-action" :style="{ color: c.enabled ? '#d97706' : '#16a34a' }" @click="handleToggle(c)">{{ c.enabled ? '禁用' : '启用' }}</button>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>

        <!-- Time Matrix -->
        <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; overflow: hidden;">
          <div style="border-bottom: 1px solid #f3f4f6; padding: 10px 16px; font-size: 13px; font-weight: 600; color: #374151;">时间矩阵</div>
          <div style="padding: 12px;">
            <div v-if="matrixLoading" style="text-align: center; padding: 40px 0;">
              <span class="tm-spin" style="display:inline-block;width:16px;height:16px;border:2px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;" />
            </div>
            <template v-else>
              <!-- Legend -->
              <div style="display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 10px; font-size: 11px; color: #6b7280;">
                <span style="display:flex;align-items:center;gap:4px;"><span style="display:inline-block;width:12px;height:12px;border:1px solid #e5e7eb;border-radius:2px;background:#fff;" /> 可用</span>
                <span style="display:flex;align-items:center;gap:4px;"><span style="display:inline-block;width:12px;height:12px;border-radius:2px;background:#fecaca;" /> 禁排</span>
                <span style="display:flex;align-items:center;gap:4px;"><span style="display:inline-block;width:12px;height:12px;border-radius:2px;background:#bfdbfe;" /> 偏好</span>
                <span style="display:flex;align-items:center;gap:4px;"><span style="display:inline-block;width:12px;height:12px;border-radius:2px;background:#fef08a;" /> 回避</span>
              </div>
              <table class="tm-table" style="font-size: 11px;">
                <thead>
                  <tr>
                    <th style="width: 60px;">节次</th>
                    <th v-for="wd in displayWeekdays" :key="wd.value">{{ wd.label }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="period in actualPeriods" :key="period.period">
                    <td style="font-weight: 500;">{{ period.name }}</td>
                    <td
                      v-for="wd in displayWeekdays"
                      :key="wd.value"
                      :style="getSlotStyle(wd.value, period.period)"
                      :title="getSlotReasons(wd.value, period.period).join('; ')"
                    >{{ getSlotSymbol(wd.value, period.period) }}</td>
                  </tr>
                </tbody>
              </table>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- Add/Edit Constraint Drawer -->
    <Transition name="tm-drawer">
      <div v-if="dialogVisible" class="tm-drawer-overlay" @click.self="dialogVisible = false">
        <div class="tm-drawer">
          <div class="tm-drawer-header">
            <h3 class="tm-drawer-title">{{ editingConstraint ? '编辑约束' : '新增约束' }}</h3>
            <button class="tm-drawer-close" @click="dialogVisible = false">&times;</button>
          </div>
          <div class="tm-drawer-body">
            <div class="tm-section">
              <h4 class="tm-section-title">基本信息</h4>
              <div class="tm-field">
                <label class="tm-label">约束名称 <span class="req">*</span></label>
                <input v-model="form.constraintName" class="tm-input" placeholder="输入约束名称" maxlength="50" />
              </div>
              <div class="tm-field">
                <label class="tm-label">约束类型 <span class="req">*</span></label>
                <select v-model="form.constraintType" class="tm-field-select" @change="onConstraintTypeChange">
                  <option value="" disabled>选择约束类型</option>
                  <option v-for="ct in CONSTRAINT_TYPES" :key="ct.value" :value="ct.value">{{ ct.label }}</option>
                </select>
              </div>
              <div class="tm-field">
                <label class="tm-label">约束强度</label>
                <div class="tm-radios" style="width: 300px;">
                  <label :class="['tm-radio', { active: form.isHard }]" @click="form.isHard = true"><input type="radio" />硬约束（必须）</label>
                  <label :class="['tm-radio', { active: !form.isHard }]" @click="form.isHard = false"><input type="radio" />软约束（尽量）</label>
                </div>
              </div>
              <div v-if="!form.isHard" class="tm-field">
                <label class="tm-label">优先权重: {{ form.priority }}</label>
                <input v-model.number="form.priority" type="range" min="1" max="100" style="width: 100%;" />
              </div>
              <div class="tm-field">
                <label class="tm-label">生效周次</label>
                <input v-model="form.effectiveWeeks" class="tm-input" placeholder="如 1-16，留空表示全部周次" />
              </div>
            </div>

            <!-- Dynamic Params -->
            <div class="tm-section">
              <h4 class="tm-section-title">参数配置</h4>

              <!-- TIME_FORBIDDEN / TIME_PREFERRED / TIME_AVOIDED -->
              <template v-if="isTimeType">
                <div class="tm-field">
                  <label class="tm-label">星期</label>
                  <div style="display: flex; flex-wrap: wrap; gap: 6px;">
                    <label v-for="wd in WEEKDAYS" :key="wd.value" style="display:flex;align-items:center;gap:4px;font-size:12px;cursor:pointer;">
                      <input type="checkbox" :value="wd.value" v-model="paramDays" /> {{ wd.label }}
                    </label>
                  </div>
                </div>
                <div class="tm-field">
                  <label class="tm-label">节次</label>
                  <div style="display: flex; flex-wrap: wrap; gap: 6px;">
                    <label v-for="p in actualPeriods" :key="p.period" style="display:flex;align-items:center;gap:4px;font-size:12px;cursor:pointer;">
                      <input type="checkbox" :value="p.period" v-model="paramPeriods" /> {{ p.name }}
                    </label>
                  </div>
                </div>
              </template>

              <!-- TIME_FIXED -->
              <template v-if="form.constraintType === 'TIME_FIXED'">
                <div class="tm-field">
                  <label class="tm-label">固定星期</label>
                  <select v-model="paramFixedDay" class="tm-field-select">
                    <option :value="undefined" disabled>选择星期</option>
                    <option v-for="wd in WEEKDAYS" :key="wd.value" :value="wd.value">{{ wd.label }}</option>
                  </select>
                </div>
                <div class="tm-field">
                  <label class="tm-label">固定节次</label>
                  <div style="display: flex; flex-wrap: wrap; gap: 6px;">
                    <label v-for="p in actualPeriods" :key="p.period" style="display:flex;align-items:center;gap:4px;font-size:12px;cursor:pointer;">
                      <input type="checkbox" :value="p.period" v-model="paramPeriods" /> {{ p.name }}
                    </label>
                  </div>
                </div>
              </template>

              <template v-if="form.constraintType === 'MAX_DAILY'">
                <div class="tm-field"><label class="tm-label">每日上限（节）</label><input v-model.number="paramMaxPeriods" type="number" min="1" max="10" class="tm-input" style="width: 100px;" /></div>
              </template>
              <template v-if="form.constraintType === 'MAX_CONSECUTIVE'">
                <div class="tm-field"><label class="tm-label">最大连排（节）</label><input v-model.number="paramMaxPeriods" type="number" min="1" max="6" class="tm-input" style="width: 100px;" /></div>
              </template>
              <template v-if="form.constraintType === 'SPREAD_EVEN' || form.constraintType === 'MIN_GAP'">
                <div class="tm-field"><label class="tm-label">最小间隔（天）</label><input v-model.number="paramMinGapDays" type="number" min="1" max="5" class="tm-input" style="width: 100px;" /></div>
              </template>
              <template v-if="form.constraintType === 'COMPACT_SCHEDULE'">
                <div class="tm-field"><label class="tm-label">最多排课（天）</label><input v-model.number="paramMaxDays" type="number" min="1" max="7" class="tm-input" style="width: 100px;" /></div>
              </template>
              <template v-if="form.constraintType === 'ROOM_REQUIRED' || form.constraintType === 'ROOM_PREFERRED'">
                <div class="tm-fields tm-cols-2">
                  <div class="tm-field"><label class="tm-label">教室类型</label><input v-model="paramRoomType" class="tm-input" placeholder="如：多媒体教室" /></div>
                  <div class="tm-field"><label class="tm-label">最小容量</label><input v-model.number="paramMinCapacity" type="number" min="0" max="500" class="tm-input" /></div>
                </div>
              </template>
              <template v-if="form.constraintType === 'MORNING_PRIORITY'">
                <div class="tm-field">
                  <label class="tm-label">优先节次</label>
                  <div style="display: flex; flex-wrap: wrap; gap: 6px;">
                    <label v-for="p in morningPeriods" :key="p.period" style="display:flex;align-items:center;gap:4px;font-size:12px;cursor:pointer;">
                      <input type="checkbox" :value="p.period" v-model="paramPeriods" /> {{ p.name }}
                    </label>
                  </div>
                </div>
              </template>
              <div v-if="!form.constraintType" style="text-align: center; padding: 20px 0; color: #9ca3af; font-size: 13px;">请先选择约束类型以配置参数</div>
            </div>
          </div>
          <div class="tm-drawer-footer">
            <button class="tm-btn tm-btn-secondary" @click="dialogVisible = false">取消</button>
            <button class="tm-btn tm-btn-primary" :disabled="saving" @click="handleSave">{{ saving ? '保存中...' : '确定' }}</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { constraintApi, scheduleConfigApi } from '@/api/teaching'
import { courseApi } from '@/api/academic'
import { http } from '@/utils/request'
import type { SchedulingConstraint, TimeSlotStatus, Course, PeriodConfig } from '@/types/teaching'
import { CONSTRAINT_LEVELS, CONSTRAINT_TYPES, WEEKDAYS, DEFAULT_PERIODS } from '@/types/teaching'

const props = defineProps<{
  semesterId: number | string | undefined
}>()

// State
const activeLevel = ref<1 | 2 | 3 | 4>(1)
const targetId = ref<number | string>('')
const constraints = ref<SchedulingConstraint[]>([])
const loading = ref(false)
const matrixLoading = ref(false)

const teachers = ref<{ value: number | string; label: string }[]>([])
const classes = ref<{ value: number | string; label: string }[]>([])
const courses = ref<{ value: number | string; label: string }[]>([])

// 实际学期作息（来自 /teaching/schedule-config）
const actualPeriods = ref<PeriodConfig[]>(DEFAULT_PERIODS)
// 上午节次（开始时间<12:00）
const morningPeriods = computed(() => actualPeriods.value.filter(p => (p.startTime || '00:00') < '12:00'))

async function loadActualPeriods() {
  if (!props.semesterId) return
  try {
    const res: any = await scheduleConfigApi.get(props.semesterId)
    const data = res.data || res
    if (data?.periods && Array.isArray(data.periods) && data.periods.length > 0) {
      actualPeriods.value = data.periods
    } else {
      actualPeriods.value = DEFAULT_PERIODS
    }
  } catch {
    actualPeriods.value = DEFAULT_PERIODS
  }
}

const timeMatrix = ref<TimeSlotStatus[][]>([])
const flatMatrix = computed<Map<string, TimeSlotStatus>>(() => {
  const map = new Map<string, TimeSlotStatus>()
  for (const row of timeMatrix.value) {
    if (!Array.isArray(row)) continue
    for (const slot of row) { if (slot) map.set(`${slot.day}-${slot.period}`, slot) }
  }
  return map
})

// Dialog
const dialogVisible = ref(false)
const editingConstraint = ref<SchedulingConstraint | null>(null)
const saving = ref(false)

const form = ref({ constraintName: '', constraintType: '' as string, isHard: true, priority: 50, effectiveWeeks: '' })

const paramDays = ref<number[]>([])
const paramPeriods = ref<number[]>([])
const paramFixedDay = ref<number | undefined>()
const paramMaxPeriods = ref(4)
const paramMinGapDays = ref(1)
const paramMaxDays = ref(5)
const paramRoomType = ref('')
const paramMinCapacity = ref(0)

// Computed
const hardConstraints = computed(() => constraints.value.filter(c => c.isHard))
const softConstraints = computed(() => constraints.value.filter(c => !c.isHard))
const enabledCount = computed(() => constraints.value.filter(c => c.enabled).length)
const targetLabel = computed(() => ({ 2: '教师', 3: '班级', 4: '课程' } as any)[activeLevel.value] || '')
const targetOptions = computed(() => ({ 2: teachers.value, 3: classes.value, 4: courses.value } as any)[activeLevel.value] || [])
const displayWeekdays = computed(() => WEEKDAYS.filter(w => w.value <= 5))
const isTimeType = computed(() => ['TIME_FORBIDDEN', 'TIME_PREFERRED', 'TIME_AVOIDED'].includes(form.value.constraintType))

// Data loading
async function loadTargetLists() {
  try {
    const [teacherRes, classRes, courseRes] = await Promise.allSettled([
      http.get<any>('/users', { params: { role: 'TEACHER', pageSize: 500 } }),
      http.get<any>('/students/classes'),
      courseApi.listAll(),
    ])
    if (teacherRes.status === 'fulfilled') {
      const list = Array.isArray(teacherRes.value) ? teacherRes.value : (teacherRes.value?.records || [])
      teachers.value = list.map((t: any) => ({ value: t.id, label: t.realName || t.name || t.username || `教师${t.id}` }))
    }
    if (classRes.status === 'fulfilled') {
      const list = Array.isArray(classRes.value) ? classRes.value : (classRes.value?.records || [])
      classes.value = list.map((c: any) => ({ value: c.id, label: c.name || c.className || `班级${c.id}` }))
    }
    if (courseRes.status === 'fulfilled') {
      const list = Array.isArray(courseRes.value) ? courseRes.value : []
      courses.value = list.map((c: Course) => ({ value: c.id, label: `${c.courseName} (${c.courseCode})` }))
    }
  } catch { /* */ }
}

async function loadConstraints() {
  if (!props.semesterId) return
  if (activeLevel.value > 1 && !targetId.value) { constraints.value = []; timeMatrix.value = []; return }
  loading.value = true
  try {
    const params: any = { semesterId: props.semesterId, level: activeLevel.value }
    if (activeLevel.value > 1 && targetId.value) params.targetId = targetId.value
    const data = await constraintApi.list(params)
    constraints.value = Array.isArray(data) ? data : []
  } catch { constraints.value = [] } finally { loading.value = false }
  loadTimeMatrix()
}

async function loadTimeMatrix() {
  if (!props.semesterId) return
  if (activeLevel.value > 1 && !targetId.value) { timeMatrix.value = []; return }
  matrixLoading.value = true
  try {
    const params: any = { semesterId: props.semesterId, level: activeLevel.value }
    if (activeLevel.value > 1 && targetId.value) params.targetId = targetId.value
    const data = await constraintApi.getTimeMatrix(params)
    timeMatrix.value = Array.isArray(data) ? data : []
  } catch { timeMatrix.value = [] } finally { matrixLoading.value = false }
}

// Time Matrix helpers
function getSlotStyle(day: number, period: number): Record<string, string> {
  const slot = flatMatrix.value.get(`${day}-${period}`)
  if (!slot) return {}
  switch (slot.status) {
    case 'forbidden': return { background: '#fecaca', color: '#dc2626' }
    case 'preferred': return { background: '#bfdbfe', color: '#2563eb' }
    case 'avoided': return { background: '#fef08a', color: '#d97706' }
    default: return {}
  }
}
function getSlotSymbol(day: number, period: number) { const s = flatMatrix.value.get(`${day}-${period}`); return s ? ({ forbidden: '禁', preferred: '优', avoided: '避' } as any)[s.status] || '' : '' }
function getSlotReasons(day: number, period: number) { return flatMatrix.value.get(`${day}-${period}`)?.reasons || [] }

// Tab / selector switching
function switchLevel(level: number) {
  activeLevel.value = level as 1 | 2 | 3 | 4; targetId.value = ''
  if (level === 1) loadConstraints()
  else { constraints.value = []; timeMatrix.value = [] }
}
function onTargetChange() { loadConstraints() }

// Helpers
function getTypeLabel(type: string) { return CONSTRAINT_TYPES.find(t => t.value === type)?.label || type }
function dayNames(days: number[]) { return days.map(d => WEEKDAYS.find(w => w.value === d)?.label || `${d}`).join('、') }
function periodNames(periods: number[]) {
  if (!Array.isArray(periods) || periods.length === 0) return ''
  const sorted = [...periods].sort((a, b) => a - b)
  return sorted.length <= 3 ? sorted.map(p => `第${p}节`).join('、') : `第${sorted[0]}-${sorted[sorted.length - 1]}节`
}
function getParamSummary(c: SchedulingConstraint) {
  const p = c.params || {}; const ct = c.constraintType
  if (['TIME_FORBIDDEN', 'TIME_PREFERRED', 'TIME_AVOIDED'].includes(ct)) { const parts: string[] = []; if (p.days?.length) parts.push(dayNames(p.days)); if (p.periods?.length) parts.push(periodNames(p.periods)); return parts.join(' ') }
  if (ct === 'TIME_FIXED') { const parts: string[] = []; if (p.day) parts.push(WEEKDAYS.find(w => w.value === p.day)?.label || ''); if (p.periods?.length) parts.push(periodNames(p.periods)); return parts.join(' ') }
  if (ct === 'MAX_DAILY') return `每日最多 ${p.maxPeriods ?? '-'} 节`
  if (ct === 'MAX_CONSECUTIVE') return `最多连排 ${p.maxPeriods ?? '-'} 节`
  if (ct === 'SPREAD_EVEN' || ct === 'MIN_GAP') return `最小间隔 ${p.minGapDays ?? '-'} 天`
  if (ct === 'COMPACT_SCHEDULE') return `最多 ${p.maxDays ?? '-'} 天排课`
  if (ct === 'ROOM_REQUIRED' || ct === 'ROOM_PREFERRED') { const parts: string[] = []; if (p.roomType) parts.push(p.roomType); if (p.minCapacity) parts.push(`容量>=${p.minCapacity}`); return parts.join('，') }
  if (ct === 'MORNING_PRIORITY') return p.periods?.length ? periodNames(p.periods) : '上午1-4节'
  return ''
}

// Dialog
function resetParams() { paramDays.value = []; paramPeriods.value = []; paramFixedDay.value = undefined; paramMaxPeriods.value = 4; paramMinGapDays.value = 1; paramMaxDays.value = 5; paramRoomType.value = ''; paramMinCapacity.value = 0 }
function onConstraintTypeChange() { const ct = CONSTRAINT_TYPES.find(t => t.value === form.value.constraintType); if (ct) form.value.isHard = ct.isHardDefault; resetParams() }
function buildParams() {
  const ct = form.value.constraintType; const r: Record<string, any> = {}
  if (['TIME_FORBIDDEN', 'TIME_PREFERRED', 'TIME_AVOIDED'].includes(ct)) { r.days = paramDays.value; r.periods = paramPeriods.value }
  else if (ct === 'TIME_FIXED') { r.day = paramFixedDay.value; r.periods = paramPeriods.value }
  else if (ct === 'MAX_DAILY' || ct === 'MAX_CONSECUTIVE') r.maxPeriods = paramMaxPeriods.value
  else if (ct === 'SPREAD_EVEN' || ct === 'MIN_GAP') r.minGapDays = paramMinGapDays.value
  else if (ct === 'COMPACT_SCHEDULE') r.maxDays = paramMaxDays.value
  else if (ct === 'ROOM_REQUIRED' || ct === 'ROOM_PREFERRED') { r.roomType = paramRoomType.value; r.minCapacity = paramMinCapacity.value }
  else if (ct === 'MORNING_PRIORITY') r.periods = paramPeriods.value
  return r
}
function loadParamsFromConstraint(c: SchedulingConstraint) {
  resetParams(); const p = c.params || {}; const ct = c.constraintType
  if (['TIME_FORBIDDEN', 'TIME_PREFERRED', 'TIME_AVOIDED'].includes(ct)) { paramDays.value = [...(p.days || [])]; paramPeriods.value = [...(p.periods || [])] }
  else if (ct === 'TIME_FIXED') { paramFixedDay.value = p.day; paramPeriods.value = [...(p.periods || [])] }
  else if (ct === 'MAX_DAILY' || ct === 'MAX_CONSECUTIVE') paramMaxPeriods.value = p.maxPeriods ?? 4
  else if (ct === 'SPREAD_EVEN' || ct === 'MIN_GAP') paramMinGapDays.value = p.minGapDays ?? 1
  else if (ct === 'COMPACT_SCHEDULE') paramMaxDays.value = p.maxDays ?? 5
  else if (ct === 'ROOM_REQUIRED' || ct === 'ROOM_PREFERRED') { paramRoomType.value = p.roomType ?? ''; paramMinCapacity.value = p.minCapacity ?? 0 }
  else if (ct === 'MORNING_PRIORITY') paramPeriods.value = [...(p.periods || [])]
}

function openAddDialog() {
  editingConstraint.value = null; form.value = { constraintName: '', constraintType: '', isHard: true, priority: 50, effectiveWeeks: '' }; resetParams(); dialogVisible.value = true
}
function openEditDialog(c: SchedulingConstraint) {
  editingConstraint.value = c; form.value = { constraintName: c.constraintName, constraintType: c.constraintType, isHard: c.isHard, priority: c.priority, effectiveWeeks: c.effectiveWeeks || '' }; loadParamsFromConstraint(c); dialogVisible.value = true
}

// CRUD
async function handleSave() {
  if (!form.value.constraintName?.trim() || !form.value.constraintType) { ElMessage.warning('请填写约束名称和类型'); return }
  const payload: Partial<SchedulingConstraint> = {
    semesterId: props.semesterId as number, constraintName: form.value.constraintName, constraintLevel: activeLevel.value,
    constraintType: form.value.constraintType, isHard: form.value.isHard, priority: form.value.isHard ? 100 : form.value.priority,
    params: buildParams(), effectiveWeeks: form.value.effectiveWeeks || undefined, enabled: true,
  }
  if (activeLevel.value > 1 && targetId.value) payload.targetId = targetId.value as number
  saving.value = true
  try {
    if (editingConstraint.value) { await constraintApi.update(editingConstraint.value.id, payload); ElMessage.success('约束已更新') }
    else { await constraintApi.create(payload); ElMessage.success('约束已创建') }
    dialogVisible.value = false; loadConstraints()
  } catch (e: any) { ElMessage.error(e?.message || '保存失败') } finally { saving.value = false }
}

async function handleDelete(c: SchedulingConstraint) {
  try {
    await ElMessageBox.confirm(`确定删除约束"${c.constraintName}"？`, '删除确认', { type: 'warning' })
    await constraintApi.delete(c.id); ElMessage.success('已删除'); loadConstraints()
  } catch { /* */ }
}

async function handleToggle(c: SchedulingConstraint) {
  try {
    if (c.enabled) { await constraintApi.disable(c.id); ElMessage.success('已禁用') }
    else { await constraintApi.enable(c.id); ElMessage.success('已启用') }
    loadConstraints()
  } catch (e: any) { ElMessage.error(e?.message || '操作失败') }
}

// Watch semesterId from parent
watch(() => props.semesterId, () => {
  if (props.semesterId) { loadActualPeriods(); loadTargetLists(); loadConstraints() }
}, { immediate: true })
</script>

<style scoped>
.cst-card { border: 1px solid #e5e7eb; border-radius: 8px; padding: 10px 12px; margin-bottom: 8px; transition: opacity 0.15s; }
.cst-card.disabled { opacity: 0.5; background: #f9fafb; }
.tm-sep { display: inline-block; width: 1px; height: 10px; background: #d1d5db; vertical-align: middle; margin: 0 4px; }
</style>

<style>
@import '@/styles/teaching-ui.css';
</style>
