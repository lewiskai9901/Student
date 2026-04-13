<template>
  <div>
    <!-- Action bar -->
    <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 16px;">
      <button class="tm-btn tm-btn-primary" @click="showAdjustmentDialog()">申请调课</button>
      <div class="tm-radios" style="width: auto;">
        <label :class="['tm-radio', { active: adjustmentView === 'my' }]" @click="adjustmentView = 'my'"><input type="radio" />我的申请</label>
        <label :class="['tm-radio', { active: adjustmentView === 'pending' }]" @click="adjustmentView = 'pending'"><input type="radio" />待审批</label>
        <label :class="['tm-radio', { active: adjustmentView === 'all' }]" @click="adjustmentView = 'all'"><input type="radio" />全部记录</label>
      </div>
    </div>

    <!-- Adjustment table -->
    <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; overflow: hidden;">
      <table class="tm-table" style="border-radius: 0; border: none;">
        <colgroup>
          <col style="width: 65px" />
          <col style="width: 75px" />
          <col />
          <col style="width: 180px" />
          <col style="width: 75px" />
          <col style="width: 130px" />
          <col style="width: 130px" />
        </colgroup>
        <thead>
          <tr>
            <th>类型</th>
            <th>申请人</th>
            <th class="text-left">原因</th>
            <th class="text-left">调整至</th>
            <th>状态</th>
            <th>申请时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="adjustmentLoading">
            <td colspan="7" class="tm-empty"><span class="tm-spin" style="display:inline-block;width:16px;height:16px;border:2px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;" /> 加载中...</td>
          </tr>
          <tr v-else-if="adjustments.length === 0">
            <td colspan="7" class="tm-empty">暂无调课记录</td>
          </tr>
          <tr v-for="row in adjustments" :key="row.id">
            <td><span :class="['tm-chip', { 1:'tm-chip-blue', 2:'tm-chip-red', 3:'tm-chip-green' }[row.adjustmentType] || 'tm-chip-gray']">{{ getAdjTypeName(row.adjustmentType) }}</span></td>
            <td>{{ row.applicantName }}</td>
            <td class="text-left" style="white-space: normal !important; font-size: 12px;">{{ row.reason }}</td>
            <td class="text-left" style="font-size: 12px; white-space: normal !important;">
              <template v-if="row.newDayOfWeek">{{ getWeekdayName(row.newDayOfWeek) }} 第{{ row.newPeriodStart }}-{{ row.newPeriodEnd }}节</template>
              <span v-if="row.newClassroomName" style="margin-left: 4px; color: #6b7280;">{{ row.newClassroomName }}</span>
              <span v-if="!row.newDayOfWeek && !row.newClassroomName" style="color: #9ca3af;">-</span>
            </td>
            <td><span :class="['tm-chip', { 0:'tm-chip-amber', 1:'tm-chip-green', 2:'tm-chip-red', 3:'tm-chip-blue', 4:'tm-chip-gray' }[row.status] || 'tm-chip-gray']">{{ getAdjStatusName(row.status) }}</span></td>
            <td style="font-size: 12px; color: #6b7280;">{{ row.appliedAt }}</td>
            <td>
              <template v-if="row.status === 0 && adjustmentView === 'pending'">
                <button class="tm-action" style="color: #16a34a;" @click="approveAdjustment(row)">批准</button>
                <button class="tm-action tm-action-danger" @click="rejectAdjustment(row)">驳回</button>
              </template>
              <template v-else-if="row.status === 1">
                <button class="tm-action" style="color: #2563eb;" @click="executeAdjustment(row)">执行</button>
              </template>
              <template v-else-if="row.status === 0 && adjustmentView === 'my'">
                <button class="tm-action tm-action-danger" @click="cancelAdjustment(row)">撤回</button>
              </template>
              <span v-else style="font-size: 11px; color: #9ca3af;">-</span>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="adjustmentPagination.total > adjustmentPagination.size" class="tm-pagination">
        <span class="tm-page-info">共 {{ adjustmentPagination.total }} 条</span>
        <div class="tm-page-controls">
          <button class="tm-page-btn" :disabled="adjustmentPagination.page <= 1" @click="adjustmentPagination.page--; loadAdjustments()">‹</button>
          <span class="tm-page-current">{{ adjustmentPagination.page }}</span>
          <button class="tm-page-btn" :disabled="adjustmentPagination.page * adjustmentPagination.size >= adjustmentPagination.total" @click="adjustmentPagination.page++; loadAdjustments()">›</button>
        </div>
      </div>
    </div>

    <!-- Adjustment Apply Drawer -->
    <Transition name="tm-drawer">
      <div v-if="adjustmentDialogVisible" class="tm-drawer-overlay" @click.self="adjustmentDialogVisible = false">
        <div class="tm-drawer">
          <div class="tm-drawer-header">
            <h3 class="tm-drawer-title">申请调课</h3>
            <button class="tm-drawer-close" @click="adjustmentDialogVisible = false">&times;</button>
          </div>
          <div class="tm-drawer-body">
            <div class="tm-section">
              <h4 class="tm-section-title">调课信息</h4>
              <div class="tm-field">
                <label class="tm-label">调课类型 <span class="req">*</span></label>
                <div class="tm-radios" style="width: 240px;">
                  <label :class="['tm-radio', { active: adjForm.adjustmentType === 1 }]" @click="adjForm.adjustmentType = 1"><input type="radio" />调课</label>
                  <label :class="['tm-radio', { active: adjForm.adjustmentType === 2 }]" @click="adjForm.adjustmentType = 2"><input type="radio" />停课</label>
                  <label :class="['tm-radio', { active: adjForm.adjustmentType === 3 }]" @click="adjForm.adjustmentType = 3"><input type="radio" />补课</label>
                </div>
              </div>
              <div class="tm-field">
                <label class="tm-label">原课程 <span class="req">*</span></label>
                <select v-model="adjForm.entryId" class="tm-field-select">
                  <option :value="undefined" disabled>选择要调整的课程</option>
                  <option v-for="entry in adjEntryOptions" :key="entry.id" :value="entry.id">
                    {{ entry.courseName }} - {{ getWeekdayName(entry.dayOfWeek) }} 第{{ entry.periodStart }}-{{ entry.periodEnd }}节
                  </option>
                </select>
              </div>
              <template v-if="adjForm.adjustmentType !== 2">
                <div class="tm-fields tm-cols-2">
                  <div class="tm-field">
                    <label class="tm-label">新星期</label>
                    <select v-model="adjForm.newDayOfWeek" class="tm-field-select">
                      <option :value="undefined">不变</option>
                      <option v-for="d in weekdays" :key="d.value" :value="d.value">{{ d.label }}</option>
                    </select>
                  </div>
                  <div class="tm-field">
                    <label class="tm-label">新教室</label>
                    <select v-model="adjForm.newClassroomId" class="tm-field-select">
                      <option :value="undefined">不变</option>
                      <option v-for="c in classrooms" :key="c.id" :value="c.id">{{ c.name }}</option>
                    </select>
                  </div>
                </div>
                <div class="tm-fields tm-cols-3">
                  <div class="tm-field">
                    <label class="tm-label">新开始节</label>
                    <select v-model="adjForm.newPeriodStart" class="tm-field-select">
                      <option :value="undefined">不变</option>
                      <option v-for="p in periods" :key="p.period" :value="p.period">{{ p.name }}</option>
                    </select>
                  </div>
                  <div class="tm-field">
                    <label class="tm-label">新结束节</label>
                    <select v-model="adjForm.newPeriodEnd" class="tm-field-select">
                      <option :value="undefined">不变</option>
                      <option v-for="p in periods" :key="p.period" :value="p.period">{{ p.name }}</option>
                    </select>
                  </div>
                  <div class="tm-field">
                    <label class="tm-label">新周次</label>
                    <input v-model.number="adjForm.newWeek" type="number" min="1" max="20" class="tm-input" />
                  </div>
                </div>
              </template>
              <div class="tm-field">
                <label class="tm-label">原因 <span class="req">*</span></label>
                <textarea v-model="adjForm.reason" class="tm-textarea" rows="2" placeholder="请填写调课原因"></textarea>
              </div>
            </div>
          </div>
          <div class="tm-drawer-footer">
            <button class="tm-btn tm-btn-secondary" @click="adjustmentDialogVisible = false">取消</button>
            <button class="tm-btn tm-btn-primary" :disabled="adjSaving" @click="submitAdjustment">{{ adjSaving ? '提交中...' : '提交' }}</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { universalPlaceApi } from '@/api/universalPlace'
import { scheduleApi, adjustmentApi } from '@/api/teaching'
import type { ScheduleEntry, ScheduleAdjustment, CourseSchedule } from '@/types/teaching'
import { WEEKDAYS, DEFAULT_PERIODS } from '@/types/teaching'

const props = defineProps<{ semesterId: number | string | undefined }>()
const weekdays = WEEKDAYS
const periods = DEFAULT_PERIODS

const adjustments = ref<ScheduleAdjustment[]>([])
const adjustmentLoading = ref(false)
const adjustmentView = ref<'my' | 'pending' | 'all'>('my')
const adjustmentPagination = reactive({ page: 1, size: 20, total: 0 })

const adjustmentDialogVisible = ref(false)
const adjSaving = ref(false)
const adjForm = ref<Partial<ScheduleAdjustment & { newWeek?: number }>>({ adjustmentType: 1, reason: '' })
const adjEntryOptions = ref<ScheduleEntry[]>([])
const classrooms = ref<{ id: number; name: string }[]>([])
const scheduleList = ref<CourseSchedule[]>([])

async function loadAdjustments() {
  adjustmentLoading.value = true
  try {
    let res; const params = { page: adjustmentPagination.page, size: adjustmentPagination.size }
    if (adjustmentView.value === 'my') res = await adjustmentApi.getMyApplications(params)
    else if (adjustmentView.value === 'pending') res = await adjustmentApi.getPendingApprovals(params)
    else res = await adjustmentApi.list(params)
    const data = (res as any).data || res
    if (data.records) { adjustments.value = data.records; adjustmentPagination.total = data.total || 0 }
    else if (Array.isArray(data)) { adjustments.value = data; adjustmentPagination.total = data.length }
  } catch { /* */ } finally { adjustmentLoading.value = false }
}

async function loadClassrooms() {
  try {
    const allItems = await universalPlaceApi.getFlatList()
    // Filter leaf nodes with capacity (classrooms)
    const items = allItems.filter((p: any) => p.capacity && p.capacity > 0)
    classrooms.value = items.map((p: any) => ({ id: p.id, name: p.placeName || p.name }))
  } catch { /* */ }
}

async function loadScheduleList() {
  if (!props.semesterId) return
  try { const res = await scheduleApi.list({ semesterId: props.semesterId }); scheduleList.value = (res as any).data || res; if (!Array.isArray(scheduleList.value)) scheduleList.value = [] } catch { /* */ }
}

async function loadAdjEntryOptions() {
  if (!props.semesterId || scheduleList.value.length === 0) return
  try { const res = await scheduleApi.getEntries(scheduleList.value[0].id); adjEntryOptions.value = (res as any).data || res } catch { /* */ }
}

function showAdjustmentDialog() { adjForm.value = { adjustmentType: 1, reason: '' }; loadAdjEntryOptions(); adjustmentDialogVisible.value = true }

async function submitAdjustment() {
  if (!adjForm.value.entryId || !adjForm.value.reason?.trim()) { ElMessage.warning('请填写完整信息'); return }
  adjSaving.value = true
  try {
    await adjustmentApi.apply({ entryId: adjForm.value.entryId!, adjustmentType: adjForm.value.adjustmentType!, newClassroomId: adjForm.value.newClassroomId, newDayOfWeek: adjForm.value.newDayOfWeek, newPeriodStart: adjForm.value.newPeriodStart, newPeriodEnd: adjForm.value.newPeriodEnd, newWeek: adjForm.value.newWeek, reason: adjForm.value.reason! })
    ElMessage.success('申请已提交'); adjustmentDialogVisible.value = false; loadAdjustments()
  } catch { ElMessage.error('提交失败') } finally { adjSaving.value = false }
}

async function approveAdjustment(adj: ScheduleAdjustment) {
  await ElMessageBox.confirm('确定批准该调课申请？', '审批确认')
  try { await adjustmentApi.approve(adj.id); ElMessage.success('已批准'); loadAdjustments() } catch { ElMessage.error('操作失败') }
}

async function rejectAdjustment(adj: ScheduleAdjustment) {
  const { value } = await ElMessageBox.prompt('请填写驳回原因', '驳回确认', { inputType: 'textarea', inputValidator: (v) => (v?.trim() ? true : '请填写原因') })
  try { await adjustmentApi.reject(adj.id, value); ElMessage.success('已驳回'); loadAdjustments() } catch { ElMessage.error('操作失败') }
}

async function executeAdjustment(adj: ScheduleAdjustment) {
  await ElMessageBox.confirm('执行后将正式修改课表，确定执行？', '执行确认', { type: 'warning' })
  try { await adjustmentApi.execute(adj.id); ElMessage.success('已执行'); loadAdjustments() } catch { ElMessage.error('执行失败') }
}

async function cancelAdjustment(adj: ScheduleAdjustment) {
  await ElMessageBox.confirm('确定撤回该申请？', '撤回确认')
  try { await adjustmentApi.cancel(adj.id); ElMessage.success('已撤回'); loadAdjustments() } catch { ElMessage.error('撤回失败') }
}

function getWeekdayName(day: number) { return weekdays.find(w => w.value === day)?.label || '' }
function getAdjTypeName(t: number) { return ({ 1: '调课', 2: '停课', 3: '补课' } as any)[t] || '未知' }
function getAdjStatusName(s: number) { return ({ 0: '待审批', 1: '已批准', 2: '已驳回', 3: '已执行', 4: '已取消' } as any)[s] || '未知' }

watch(() => props.semesterId, (val) => { if (val) { loadScheduleList(); loadClassrooms() }; loadAdjustments() }, { immediate: true })
watch(adjustmentView, () => { adjustmentPagination.page = 1; loadAdjustments() })
</script>

<style>
@import '@/styles/teaching-ui.css';
</style>
