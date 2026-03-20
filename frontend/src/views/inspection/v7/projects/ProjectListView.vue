<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Play, Pause, CheckCircle, Archive, Trash2, Calendar, Users, ClipboardList } from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/insp/inspExecutionStore'
import { inspProjectApi } from '@/api/insp/project'
import { getTasks } from '@/api/insp/task'
import {
  ProjectStatusConfig, type ProjectStatus,
  AssignmentModeConfig, type AssignmentMode,
} from '@/types/insp/enums'
import type { InspProject } from '@/types/insp/project'
import InspEmptyState from '../shared/InspEmptyState.vue'

const router = useRouter()
const store = useInspExecutionStore()

// State
const loading = ref(false)
const projects = ref<InspProject[]>([])
const taskCountMap = ref<Map<number, { total: number; pending: number; done: number }>>(new Map())
const inspectorCountMap = ref<Map<number, number>>(new Map())

const queryParams = reactive({
  status: undefined as ProjectStatus | undefined,
})

const statusOptions = computed(() =>
  Object.entries(ProjectStatusConfig).map(([key, val]) => ({ value: key, label: val.label }))
)

// Actions
async function loadData() {
  loading.value = true
  try {
    projects.value = await store.loadProjects(queryParams.status)

    // Load task stats and inspector counts in parallel
    await Promise.all(projects.value.map(async (p) => {
      try {
        let total = 0, pending = 0, done = 0
        try {
          const tasks = await getTasks({ projectId: p.id })
          total += tasks.length
          pending += tasks.filter(t => t.status === 'PENDING').length
          done += tasks.filter(t => ['SUBMITTED', 'UNDER_REVIEW', 'REVIEWED', 'PUBLISHED'].includes(t.status)).length
        } catch { /* ignore */ }
        taskCountMap.value.set(p.id, { total, pending, done })

        const inspectors = await inspProjectApi.getInspectors(p.id)
        inspectorCountMap.value.set(p.id, inspectors.length)
      } catch { /* ignore */ }
    }))
  } catch (e: any) {
    ElMessage.error(e.message || '加载项目列表失败')
  } finally {
    loading.value = false
  }
}

function goCreate() {
  router.push('/inspection/v7/projects/create')
}

function goDetail(project: InspProject) {
  router.push(`/inspection/v7/projects/${project.id}`)
}

function getTaskCount(pid: number) {
  return taskCountMap.value.get(pid) || { total: 0, pending: 0, done: 0 }
}

function getDateRange(p: InspProject): string {
  if (!p.startDate) return '未设置'
  if (!p.endDate) return p.startDate + ' 起'
  return p.startDate + ' ~ ' + p.endDate
}

async function handleDelete(project: InspProject) {
  try {
    await ElMessageBox.confirm(`确定删除项目「${project.projectName}」？`, '确认删除', { type: 'warning' })
    await store.removeProject(project.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled */ }
}

async function handlePause(project: InspProject) {
  try {
    await store.pauseProject(project.id)
    ElMessage.success('已暂停')
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
}

async function handleResume(project: InspProject) {
  try {
    await store.resumeProject(project.id)
    ElMessage.success('已恢复')
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
}

async function handleComplete(project: InspProject) {
  try {
    await ElMessageBox.confirm('确定完结该项目？完结后不可恢复。', '确认完结', { type: 'warning' })
    await store.completeProject(project.id)
    ElMessage.success('已完结')
    loadData()
  } catch { /* cancelled */ }
}

async function handleArchive(project: InspProject) {
  try {
    await store.archiveProject(project.id)
    ElMessage.success('已归档')
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
}

onMounted(() => { loadData() })
</script>

<template>
  <div class="p-5 space-y-4">
    <!-- 标题栏 -->
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">检查项目</h2>
      <el-button type="primary" @click="goCreate">
        <Plus class="w-4 h-4 mr-1" />新建项目
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="flex items-center gap-3">
      <el-select v-model="queryParams.status" placeholder="项目状态" clearable class="w-36" @change="loadData">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
    </div>

    <!-- 项目卡片列表 -->
    <div v-loading="loading" class="space-y-3">
      <div
        v-for="project in projects"
        :key="project.id"
        class="border border-gray-200 rounded-lg hover:shadow-sm transition-shadow cursor-pointer"
        @click="goDetail(project)"
      >
        <!-- 卡片头部 -->
        <div class="px-4 py-3 flex items-start justify-between">
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span class="font-medium text-gray-900">{{ project.projectName }}</span>
              <el-tag :type="(ProjectStatusConfig[project.status as ProjectStatus]?.type as any)" size="small">
                {{ ProjectStatusConfig[project.status as ProjectStatus]?.label }}
              </el-tag>
            </div>
            <div class="text-xs text-gray-400 mt-0.5">{{ project.projectCode }}</div>
          </div>
          <!-- 操作按钮 -->
          <div class="flex items-center gap-1 shrink-0" @click.stop>
            <el-button v-if="project.status === 'PUBLISHED'" link type="warning" size="small" @click="handlePause(project)">
              <Pause class="w-3.5 h-3.5" />
            </el-button>
            <el-button v-if="project.status === 'PAUSED'" link type="success" size="small" @click="handleResume(project)">
              <Play class="w-3.5 h-3.5" />
            </el-button>
            <el-button v-if="project.status === 'PUBLISHED' || project.status === 'PAUSED'" link size="small" @click="handleComplete(project)">
              <CheckCircle class="w-3.5 h-3.5" />
            </el-button>
            <el-button v-if="project.status === 'COMPLETED'" link size="small" @click="handleArchive(project)">
              <Archive class="w-3.5 h-3.5" />
            </el-button>
            <el-button v-if="project.status === 'DRAFT'" link type="danger" size="small" @click="handleDelete(project)">
              <Trash2 class="w-3.5 h-3.5" />
            </el-button>
          </div>
        </div>

        <!-- 卡片统计行 -->
        <div class="px-4 pb-3 flex items-center gap-6 text-xs text-gray-500">
          <span class="flex items-center gap-1">
            <Calendar class="w-3.5 h-3.5 text-gray-400" />
            {{ getDateRange(project) }}
          </span>
          <span class="flex items-center gap-1">
            <Users class="w-3.5 h-3.5 text-gray-400" />
            {{ inspectorCountMap.get(project.id) || 0 }} 检查员
          </span>
          <span class="flex items-center gap-1">
            <ClipboardList class="w-3.5 h-3.5 text-gray-400" />
            {{ getTaskCount(project.id).total }} 任务
            <template v-if="getTaskCount(project.id).total > 0">
              <span class="text-green-600">{{ getTaskCount(project.id).done }} 完成</span>
              <span v-if="getTaskCount(project.id).pending > 0" class="text-orange-500">{{ getTaskCount(project.id).pending }} 待领取</span>
            </template>
          </span>
          <span v-if="project.assignmentMode" class="flex items-center gap-1">
            {{ AssignmentModeConfig[project.assignmentMode as AssignmentMode]?.label }}
          </span>
        </div>
      </div>

      <InspEmptyState v-if="!loading && projects.length === 0" message="暂无检查项目" />
    </div>
  </div>
</template>
