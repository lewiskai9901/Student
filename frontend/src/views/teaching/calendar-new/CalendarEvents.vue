<template>
  <div>
    <!-- Action Bar -->
    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;">
      <div class="tm-stats" style="margin-top: 0;">
        <span>共 <b>{{ events.length }}</b> 个事件</span>
        <span class="sep" />
        <span>放假 <b>{{ events.filter(e => e.eventType === 1).length }}</b></span>
        <span class="sep" />
        <span>考试 <b>{{ events.filter(e => e.eventType === 2).length }}</b></span>
      </div>
      <div style="display: flex; gap: 8px;">
        <select v-model="filterType" class="tm-select" @change="$forceUpdate()">
          <option :value="undefined">全部类型</option>
          <option :value="1">放假</option>
          <option :value="2">考试</option>
          <option :value="3">活动</option>
          <option :value="4">会议</option>
          <option :value="5">其他</option>
        </select>
        <button class="tm-btn tm-btn-primary" @click="showDialog()">新建事件</button>
      </div>
    </div>

    <!-- Events Table -->
    <table class="tm-table">
      <colgroup>
        <col />
        <col style="width: 80px" />
        <col style="width: 120px" />
        <col style="width: 120px" />
        <col style="width: 100px" />
        <col style="width: 100px" />
        <col style="width: 120px" />
      </colgroup>
      <thead>
        <tr>
          <th class="text-left">事件名称</th>
          <th>类型</th>
          <th>开始日期</th>
          <th>结束日期</th>
          <th>排课影响</th>
          <th>补课按</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="filteredEvents.length === 0">
          <td colspan="7" class="tm-empty">暂无校历事件</td>
        </tr>
        <tr v-for="evt in filteredEvents" :key="evt.id">
          <td class="text-left" style="font-weight: 500;">{{ evt.eventName }}</td>
          <td><span :class="['tm-chip', getEventChip(evt.eventType)]">{{ getEventTypeName(evt.eventType) }}</span></td>
          <td style="font-size: 12px;">{{ evt.startDate }}</td>
          <td style="font-size: 12px;">{{ evt.endDate || '-' }}</td>
          <td><span :class="['tm-chip', getAffectChip(evt.affectType)]">{{ getAffectName(evt.affectType) }}</span></td>
          <td>{{ evt.substituteWeekday ? getWeekdayName(evt.substituteWeekday) : '-' }}</td>
          <td>
            <button class="tm-action" @click="showDialog(evt)">编辑</button>
            <button class="tm-action tm-action-danger" @click="handleDelete(evt)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Event Dialog (Drawer) -->
    <Transition name="tm-drawer">
      <div v-if="dialogVisible" class="tm-drawer-overlay" @click.self="dialogVisible = false">
        <div class="tm-drawer">
          <div class="tm-drawer-header">
            <h3 class="tm-drawer-title">{{ editingEvent ? '编辑事件' : '新建校历事件' }}</h3>
            <button class="tm-drawer-close" @click="dialogVisible = false">&times;</button>
          </div>
          <div class="tm-drawer-body">
            <div class="tm-section">
              <h4 class="tm-section-title">基本信息</h4>
              <div class="tm-field">
                <label class="tm-label">事件名称 <span class="req">*</span></label>
                <input v-model="form.eventName" class="tm-input" placeholder="如：国庆节放假" />
              </div>
              <div class="tm-fields tm-cols-2">
                <div class="tm-field">
                  <label class="tm-label">开始日期 <span class="req">*</span></label>
                  <input v-model="form.startDate" type="date" class="tm-input" />
                </div>
                <div class="tm-field">
                  <label class="tm-label">结束日期</label>
                  <input v-model="form.endDate" type="date" class="tm-input" />
                </div>
              </div>
              <div class="tm-field">
                <label class="tm-label">事件类型</label>
                <select v-model="form.eventType" class="tm-field-select">
                  <option :value="1">放假</option>
                  <option :value="2">考试</option>
                  <option :value="3">活动</option>
                  <option :value="4">会议</option>
                  <option :value="5">其他</option>
                </select>
              </div>
              <div class="tm-field">
                <label class="tm-label">说明</label>
                <textarea v-model="form.description" class="tm-textarea" rows="2"></textarea>
              </div>
            </div>

            <div class="tm-section">
              <h4 class="tm-section-title">排课影响</h4>
              <div class="tm-field">
                <label class="tm-label">影响类型</label>
                <select v-model="form.affectType" class="tm-field-select">
                  <option :value="0">无影响</option>
                  <option :value="1">全天停课</option>
                  <option :value="2">半天停课</option>
                  <option :value="3">补课日(按某天课表)</option>
                  <option :value="4">考试周(停常规课)</option>
                </select>
              </div>
              <div v-if="form.affectType === 2" class="tm-field">
                <label class="tm-label">停课节次（如 1-4 表示上午）</label>
                <input v-model="form.affectSlots" class="tm-input" placeholder="1-4" />
              </div>
              <div v-if="form.affectType === 3" class="tm-field">
                <label class="tm-label">按周几课表上课</label>
                <select v-model="form.substituteWeekday" class="tm-field-select">
                  <option :value="undefined" disabled>选择</option>
                  <option :value="1">周一</option>
                  <option :value="2">周二</option>
                  <option :value="3">周三</option>
                  <option :value="4">周四</option>
                  <option :value="5">周五</option>
                </select>
              </div>
            </div>
          </div>
          <div class="tm-drawer-footer">
            <button class="tm-btn tm-btn-secondary" @click="dialogVisible = false">取消</button>
            <button class="tm-btn tm-btn-primary" :disabled="saving" @click="handleSave">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { academicEventApi } from '@/api/calendar'

const props = defineProps<{ semesterId: number | string | undefined; yearId: number | string | undefined; events: any[] }>()
const emit = defineEmits<{ refresh: [] }>()

const filterType = ref<number | undefined>()
const dialogVisible = ref(false)
const saving = ref(false)
const editingEvent = ref<any>(null)
const form = ref<any>({ eventName: '', eventType: 5, startDate: '', endDate: '', description: '', affectType: 0 })

const filteredEvents = computed(() => {
  if (!filterType.value) return props.events
  return props.events.filter(e => e.eventType === filterType.value)
})

function getEventTypeName(t: number) { return ({ 1: '放假', 2: '考试', 3: '活动', 4: '会议', 5: '其他' } as any)[t] || '事件' }
function getEventChip(t: number) { return ({ 1: 'tm-chip-red', 2: 'tm-chip-purple', 3: 'tm-chip-blue', 4: 'tm-chip-amber', 5: 'tm-chip-gray' } as any)[t] || 'tm-chip-gray' }
function getAffectName(t: number) { return ({ 0: '无', 1: '全天停课', 2: '半天停课', 3: '补课日', 4: '考试周' } as any)[t] || '无' }
function getAffectChip(t: number) { return ({ 0: 'tm-chip-gray', 1: 'tm-chip-red', 2: 'tm-chip-amber', 3: 'tm-chip-green', 4: 'tm-chip-purple' } as any)[t] || 'tm-chip-gray' }
function getWeekdayName(d: number) { return ({ 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五' } as any)[d] || '' }

function showDialog(evt?: any) {
  editingEvent.value = evt || null
  form.value = evt ? { ...evt } : { eventName: '', eventType: 5, startDate: '', endDate: '', description: '', affectType: 0, affectSlots: '', substituteWeekday: undefined }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.eventName?.trim() || !form.value.startDate) { ElMessage.warning('请填写事件名称和日期'); return }
  saving.value = true
  try {
    const payload = { ...form.value, yearId: props.yearId, semesterId: props.semesterId }
    if (editingEvent.value?.id) {
      await academicEventApi.update(editingEvent.value.id, payload)
    } else {
      await academicEventApi.create(payload)
    }
    ElMessage.success('保存成功'); dialogVisible.value = false; emit('refresh')
  } catch { ElMessage.error('保存失败') } finally { saving.value = false }
}

async function handleDelete(evt: any) {
  await ElMessageBox.confirm(`确定删除事件"${evt.eventName}"？`, '删除确认', { type: 'warning' })
  try { await academicEventApi.delete(evt.id); ElMessage.success('已删除'); emit('refresh') } catch { ElMessage.error('删除失败') }
}
</script>

<style>
@import '@/styles/teaching-ui.css';
</style>
