<template>
  <div class="tm-page">
    <!-- Header -->
    <div class="tm-header">
      <div>
        <h1 class="tm-title">排课中心</h1>
        <div class="tm-stats">
          <span>排课管理、课表查看、冲突检测与调课</span>
        </div>
      </div>
      <select v-model="semesterId" class="tm-select" @change="onSemesterChange">
        <option :value="undefined" disabled>选择学期</option>
        <option v-for="s in semesters" :key="s.id" :value="s.id">{{ s.semesterName }}</option>
      </select>
    </div>

    <!-- Tabs -->
    <div class="tm-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['tm-tab', { active: activeTab === tab.key }]"
        @click="activeTab = tab.key"
      >{{ tab.label }}</button>
    </div>

    <!-- Tab Content -->
    <div style="flex: 1; overflow-y: auto; padding: 16px 24px;">
      <!-- Loading -->
      <div v-if="globalLoading" style="text-align: center; padding: 60px 0;">
        <span class="tm-spin" style="display:inline-block;width:24px;height:24px;border:2px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;" />
      </div>

      <template v-else>
        <ScheduleOverview v-if="activeTab === 'overview'" :semester-id="semesterId" />
        <ScheduleManager v-else-if="activeTab === 'schedules'" :semester-id="semesterId" />
        <TimetableViewer v-else-if="activeTab === 'timetable'" :semester-id="semesterId" />
        <ConflictPanel v-else-if="activeTab === 'conflicts'" :semester-id="semesterId" />
        <AdjustmentPanel v-else-if="activeTab === 'adjustments'" :semester-id="semesterId" />

        <!-- Constraints (lightweight inline) -->
        <div v-else-if="activeTab === 'constraints'" style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; padding: 40px; text-align: center;">
          <p style="font-size: 13px; font-weight: 500; color: #6b7280; margin-bottom: 4px;">约束配置</p>
          <p style="font-size: 12px; color: #9ca3af; margin-bottom: 12px;">约束规则管理已移至独立页面</p>
          <router-link
            v-if="constraintRouteExists"
            :to="{ name: 'ConstraintConfig' }"
            class="tm-btn tm-btn-primary"
            style="text-decoration: none;"
          >前往约束配置</router-link>
          <span v-else style="font-size: 12px; color: #9ca3af;">约束配置页面尚未创建</span>
        </div>

        <!-- Export (lightweight inline) -->
        <div v-else-if="activeTab === 'export'" style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; padding: 20px;">
          <h3 style="font-size: 14px; font-weight: 600; color: #111827; margin: 0 0 16px 0;">导出课表</h3>
          <div class="tm-field">
            <label class="tm-label">导出维度</label>
            <div class="tm-radios" style="width: 240px;">
              <label :class="['tm-radio', { active: exportDimension === 'class' }]" @click="exportDimension = 'class'; exportTargetId = ''"><input type="radio" />班级课表</label>
              <label :class="['tm-radio', { active: exportDimension === 'teacher' }]" @click="exportDimension = 'teacher'; exportTargetId = ''"><input type="radio" />教师课表</label>
            </div>
          </div>
          <div class="tm-field">
            <label class="tm-label">选择对象</label>
            <select v-model="exportTargetId" class="tm-field-select" style="width: 260px;">
              <option :value="''" disabled>请选择</option>
              <option v-for="item in exportTargetList" :key="item.id" :value="item.id">{{ item.name }}</option>
            </select>
          </div>
          <button class="tm-btn tm-btn-primary" :disabled="!exportTargetId || exporting" @click="doExport">
            {{ exporting ? '导出中...' : '下载 Excel 课表' }}
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http as request } from '@/utils/request'
import { scheduleApi } from '@/api/teaching'
import { semesterApi } from '@/api/calendar'
import type { Semester } from '@/types/teaching'

import ScheduleOverview from './schedule/ScheduleOverview.vue'
import ScheduleManager from './schedule/ScheduleManager.vue'
import TimetableViewer from './schedule/TimetableViewer.vue'
import ConflictPanel from './schedule/ConflictPanel.vue'
import AdjustmentPanel from './schedule/AdjustmentPanel.vue'

const tabs = [
  { key: 'overview', label: '排课总览' },
  { key: 'schedules', label: '排课方案' },
  { key: 'timetable', label: '课表视图' },
  { key: 'constraints', label: '约束配置' },
  { key: 'conflicts', label: '冲突中心' },
  { key: 'adjustments', label: '调课管理' },
  { key: 'export', label: '导出打印' },
] as const

type TabKey = (typeof tabs)[number]['key']

const router = useRouter()
const globalLoading = ref(false)
const activeTab = ref<TabKey>('overview')
const semesterId = ref<number | string>()
const semesters = ref<Semester[]>([])

const constraintRouteExists = computed(() => router.getRoutes().some(r => r.name === 'ConstraintConfig'))

const exportDimension = ref<'class' | 'teacher'>('class')
const exportTargetId = ref<number | string>('')
const exporting = ref(false)
const classList = ref<{ id: number; name: string }[]>([])
const teacherList = ref<{ id: number; name: string }[]>([])

const exportTargetList = computed(() => exportDimension.value === 'class' ? classList.value : teacherList.value)

async function loadClassList() {
  try {
    const res = await request.get('/organization/classes/list')
    const data = (res as any).data || res
    const items = Array.isArray(data) ? data : data.records || []
    classList.value = items.map((c: any) => ({ id: c.id, name: c.className || c.name }))
  } catch { /* */ }
}

async function loadTeacherList() {
  try {
    const res = await request.get('/users', { params: { role: 'TEACHER' } })
    const data = (res as any).data || res
    const items = Array.isArray(data) ? data : data.records || []
    teacherList.value = items.map((t: any) => ({ id: t.id, name: t.realName || t.username || t.name }))
  } catch { /* */ }
}

async function doExport() {
  if (!exportTargetId.value || !semesterId.value) { ElMessage.warning('请先选择学期和导出对象'); return }
  exporting.value = true
  try {
    const blob = exportDimension.value === 'class'
      ? await scheduleApi.exportClassSchedule(semesterId.value, exportTargetId.value)
      : await scheduleApi.exportTeacherSchedule(semesterId.value, exportTargetId.value)
    const url = window.URL.createObjectURL(new Blob([blob as any]))
    const a = document.createElement('a'); a.href = url; a.download = `${exportDimension.value}_schedule.xlsx`
    document.body.appendChild(a); a.click(); document.body.removeChild(a); window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e: any) { ElMessage.error('导出失败: ' + (e.message || '')) } finally { exporting.value = false }
}

async function loadSemesters() {
  try {
    const res = await semesterApi.list()
    semesters.value = (res as any).data || res
    if (Array.isArray(semesters.value) && semesters.value.length > 0) {
      const current = semesters.value.find(s => s.isCurrent)
      semesterId.value = current ? current.id : semesters.value[0].id
    }
  } catch { /* */ }
}

function onSemesterChange() {}

onMounted(async () => {
  globalLoading.value = true
  try { await loadSemesters(); await Promise.all([loadClassList(), loadTeacherList()]) }
  finally { globalLoading.value = false }
})
</script>

<style>
@import '@/styles/teaching-ui.css';
</style>
