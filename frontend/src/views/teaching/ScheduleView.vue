<template>
  <div class="flex h-full flex-col bg-gray-50">
    <!-- Top Header Bar -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">排课中心</h1>
        <p class="mt-0.5 text-sm text-gray-500">排课管理、课表查看、冲突检测与调课</p>
      </div>
      <div class="flex items-center gap-3">
        <el-select
          v-model="semesterId"
          placeholder="选择学期"
          class="w-48"
          @change="onSemesterChange"
        >
          <el-option v-for="s in semesters" :key="s.id" :value="s.id" :label="s.semesterName" />
        </el-select>
      </div>
    </div>

    <!-- Tabs -->
    <div class="border-b border-gray-200 bg-white px-6">
      <nav class="-mb-px flex gap-6">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="border-b-2 px-1 py-3 text-sm font-medium transition-colors"
          :class="
            activeTab === tab.key
              ? 'border-blue-500 text-blue-600'
              : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
          "
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </nav>
    </div>

    <!-- Tab Content -->
    <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
      <!-- Loading -->
      <div v-if="globalLoading" class="flex items-center justify-center py-20">
        <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
      </div>

      <template v-else>
        <ScheduleOverview v-if="activeTab === 'overview'" :semester-id="semesterId" />
        <ScheduleManager v-else-if="activeTab === 'schedules'" :semester-id="semesterId" />
        <TimetableViewer v-else-if="activeTab === 'timetable'" :semester-id="semesterId" />
        <ConflictPanel v-else-if="activeTab === 'conflicts'" :semester-id="semesterId" />
        <AdjustmentPanel v-else-if="activeTab === 'adjustments'" :semester-id="semesterId" />

        <!-- Constraints (lightweight, kept inline) -->
        <div v-else-if="activeTab === 'constraints'">
          <div class="rounded-xl border border-gray-200 bg-white p-8 text-center">
            <Settings class="mx-auto mb-3 h-10 w-10 text-gray-300" />
            <p class="mb-1 text-sm font-medium text-gray-600">约束配置</p>
            <p class="mb-4 text-xs text-gray-400">约束规则管理已移至独立页面，点击下方按钮前往配置。</p>
            <router-link
              v-if="constraintRouteExists"
              :to="{ name: 'ConstraintConfig' }"
              class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
            >
              <ExternalLink class="h-4 w-4" />
              前往约束配置
            </router-link>
            <div v-else class="text-xs text-gray-400">约束配置页面尚未创建，敬请期待。</div>
          </div>
        </div>

        <!-- Export (lightweight, kept inline) -->
        <div v-else-if="activeTab === 'export'">
          <div class="rounded-xl border border-gray-200 bg-white p-5">
            <h3 class="mb-4 text-sm font-semibold text-gray-900">导出课表</h3>
            <el-form label-width="100px">
              <el-form-item label="导出维度">
                <el-radio-group v-model="exportDimension" @change="exportTargetId = ''">
                  <el-radio value="class">班级课表</el-radio>
                  <el-radio value="teacher">教师课表</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="选择对象">
                <el-select v-model="exportTargetId" placeholder="请选择" class="w-64" filterable clearable>
                  <el-option
                    v-for="item in exportTargetList"
                    :key="item.id"
                    :value="item.id"
                    :label="item.name"
                  />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="exporting" @click="doExport" :disabled="!exportTargetId">
                  <Download class="mr-1 h-4 w-4" /> 下载 Excel 课表
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Settings, ExternalLink, Download } from 'lucide-vue-next'
import { http as request } from '@/utils/request'
import { scheduleApi } from '@/api/teaching'
import { semesterApi } from '@/api/calendar'
import type { Semester } from '@/types/teaching'

import ScheduleOverview from './schedule/ScheduleOverview.vue'
import ScheduleManager from './schedule/ScheduleManager.vue'
import TimetableViewer from './schedule/TimetableViewer.vue'
import ConflictPanel from './schedule/ConflictPanel.vue'
import AdjustmentPanel from './schedule/AdjustmentPanel.vue'

// ==================== Constants ====================

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

// ==================== Core State ====================

const router = useRouter()
const globalLoading = ref(false)
const activeTab = ref<TabKey>('overview')
const semesterId = ref<number | string>()
const semesters = ref<Semester[]>([])

// ==================== Constraints (inline) ====================

const constraintRouteExists = computed(() => {
  return router.getRoutes().some(r => r.name === 'ConstraintConfig')
})

// ==================== Export (inline) ====================

const exportDimension = ref<'class' | 'teacher'>('class')
const exportTargetId = ref<number | string>('')
const exporting = ref(false)
const classList = ref<{ id: number; name: string }[]>([])
const teacherList = ref<{ id: number; name: string }[]>([])

const exportTargetList = computed(() => {
  if (exportDimension.value === 'class') return classList.value
  return teacherList.value
})

async function loadClassList() {
  try {
    const res = await request.get('/organization/classes/list')
    const data = (res as any).data || res
    const items = Array.isArray(data) ? data : data.records || []
    classList.value = items.map((c: any) => ({ id: c.id, name: c.className || c.name }))
  } catch (e) {
    console.error('Failed to load class list:', e)
  }
}

async function loadTeacherList() {
  try {
    const res = await request.get('/users', { params: { role: 'TEACHER' } })
    const data = (res as any).data || res
    const items = Array.isArray(data) ? data : data.records || []
    teacherList.value = items.map((t: any) => ({ id: t.id, name: t.realName || t.username || t.name }))
  } catch (e) {
    console.error('Failed to load teacher list:', e)
  }
}

async function doExport() {
  if (!exportTargetId.value || !semesterId.value) {
    ElMessage.warning('请先选择学期和导出对象')
    return
  }
  exporting.value = true
  try {
    const blob = exportDimension.value === 'class'
      ? await scheduleApi.exportClassSchedule(semesterId.value, exportTargetId.value)
      : await scheduleApi.exportTeacherSchedule(semesterId.value, exportTargetId.value)
    const url = window.URL.createObjectURL(new Blob([blob as any]))
    const a = document.createElement('a')
    a.href = url
    a.download = `${exportDimension.value}_schedule.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e: any) {
    ElMessage.error('导出失败: ' + (e.message || ''))
  } finally {
    exporting.value = false
  }
}

// ==================== Data Loading ====================

async function loadSemesters() {
  try {
    const res = await semesterApi.list()
    semesters.value = (res as any).data || res
    if (Array.isArray(semesters.value) && semesters.value.length > 0) {
      const current = semesters.value.find(s => s.isCurrent)
      semesterId.value = current ? current.id : semesters.value[0].id
    }
  } catch (e) {
    console.error('Failed to load semesters:', e)
  }
}

function onSemesterChange() {
  // Sub-components react via their own semesterId watcher
}

// ==================== Init ====================

onMounted(async () => {
  globalLoading.value = true
  try {
    await loadSemesters()
    await Promise.all([loadClassList(), loadTeacherList()])
  } finally {
    globalLoading.value = false
  }
})
</script>
