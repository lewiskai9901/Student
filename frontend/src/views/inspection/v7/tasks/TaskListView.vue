<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Play, Send, Eye, XCircle, UserCheck, ListChecks, ChevronRight, Calendar, Target, Users } from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/insp/inspExecutionStore'
import { inspProjectApi } from '@/api/insp/project'
import { TaskStatusConfig, type TaskStatus, TargetTypeConfig, type TargetType } from '@/types/insp/enums'
import type { InspTask, InspProject } from '@/types/insp/project'
import InspEmptyState from '../shared/InspEmptyState.vue'

const route = useRoute()
const router = useRouter()
const store = useInspExecutionStore()

// State
const loading = ref(false)
const tasks = ref<InspTask[]>([])
const projects = ref<Map<number, InspProject>>(new Map())
const activeView = ref<'all' | 'my' | 'available'>('all')
const expandedProjects = ref<Set<number>>(new Set())

const projectId = computed(() =>
  route.query.projectId ? Number(route.query.projectId) : undefined
)

// 按项目分组的任务
interface ProjectGroup {
  project: InspProject | null
  projectId: number
  tasks: InspTask[]
  totalTargets: number
  completedTargets: number
}

const groupedTasks = computed<ProjectGroup[]>(() => {
  const groups = new Map<number, InspTask[]>()
  for (const task of tasks.value) {
    const pid = task.projectId
    if (!groups.has(pid)) groups.set(pid, [])
    groups.get(pid)!.push(task)
  }

  const result: ProjectGroup[] = []
  for (const [pid, groupTasks] of groups) {
    // 按日期排序
    groupTasks.sort((a, b) => a.taskDate.localeCompare(b.taskDate))
    const totalTargets = groupTasks.reduce((sum, t) => sum + t.totalTargets, 0)
    const completedTargets = groupTasks.reduce((sum, t) => sum + t.completedTargets + t.skippedTargets, 0)
    result.push({
      project: projects.value.get(pid) || null,
      projectId: pid,
      tasks: groupTasks,
      totalTargets,
      completedTargets,
    })
  }
  return result
})

// Actions
async function loadData() {
  loading.value = true
  try {
    if (activeView.value === 'my') {
      tasks.value = await store.loadMyTasks()
    } else if (activeView.value === 'available') {
      tasks.value = await store.loadAvailableTasks()
    } else {
      tasks.value = await store.loadTasks(projectId.value)
    }

    // 加载关联的项目信息
    const projectIds = [...new Set(tasks.value.map(t => t.projectId))]
    for (const pid of projectIds) {
      if (!projects.value.has(pid)) {
        try {
          const proj = await store.loadProject(pid)
          if (proj) projects.value.set(pid, proj)
        } catch { /* ignore */ }
      }
    }

    // 默认展开所有分组
    expandedProjects.value = new Set(projectIds)
  } catch (e: any) {
    ElMessage.error(e.message || '加载任务列表失败')
  } finally {
    loading.value = false
  }
}

function switchView(view: 'all' | 'my' | 'available') {
  activeView.value = view
  loadData()
}

function toggleGroup(pid: number) {
  if (expandedProjects.value.has(pid)) {
    expandedProjects.value.delete(pid)
  } else {
    expandedProjects.value.add(pid)
  }
}

function getProjectName(group: ProjectGroup): string {
  return group.project?.projectName || `项目 #${group.projectId}`
}

function getGroupStatusSummary(group: ProjectGroup): string {
  const pending = group.tasks.filter(t => t.status === 'PENDING').length
  const inProgress = group.tasks.filter(t => ['CLAIMED', 'IN_PROGRESS'].includes(t.status)).length
  const submitted = group.tasks.filter(t => ['SUBMITTED', 'UNDER_REVIEW', 'REVIEWED', 'PUBLISHED'].includes(t.status)).length
  const parts: string[] = []
  if (pending > 0) parts.push(`${pending} 待领取`)
  if (inProgress > 0) parts.push(`${inProgress} 进行中`)
  if (submitted > 0) parts.push(`${submitted} 已完成`)
  return parts.join('  ')
}

function formatTaskLabel(task: InspTask, group: ProjectGroup): string {
  return task.taskDate
}

function goExecute(task: InspTask) {
  router.push(`/inspection/v7/tasks/${task.id}/execute`)
}

function goProjectDetail(pid: number) {
  router.push(`/inspection/v7/projects/${pid}`)
}

async function handleClaim(task: InspTask) {
  try {
    await store.claimTask(task.id, { inspectorName: '当前用户' })
    ElMessage.success('领取成功')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '领取失败')
  }
}

async function handleCancel(task: InspTask) {
  try {
    await ElMessageBox.confirm('确定取消该任务？', '确认', { type: 'warning' })
    await store.cancelTask(task.id)
    ElMessage.success('已取消')
    loadData()
  } catch { /* cancelled */ }
}

async function handlePublish(task: InspTask) {
  try {
    await store.publishTask(task.id)
    ElMessage.success('已发布')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '发布失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="p-5 space-y-4">
    <!-- 标题栏 -->
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">检查任务</h2>
      <el-radio-group v-model="activeView" size="small" @change="switchView">
        <el-radio-button value="all"><ListChecks class="w-3.5 h-3.5 mr-1 inline-block" />全部</el-radio-button>
        <el-radio-button value="my"><UserCheck class="w-3.5 h-3.5 mr-1 inline-block" />我的</el-radio-button>
        <el-radio-button value="available">可领取</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="space-y-3">
      <!-- 按项目分组 -->
      <div
        v-for="group in groupedTasks"
        :key="group.projectId"
        class="border border-gray-200 rounded-lg overflow-hidden"
      >
        <!-- 项目头部 -->
        <div
          class="flex items-center gap-3 px-4 py-3 bg-gray-50 cursor-pointer select-none hover:bg-gray-100 transition-colors"
          @click="toggleGroup(group.projectId)"
        >
          <ChevronRight
            class="w-4 h-4 text-gray-400 transition-transform shrink-0"
            :class="{ 'rotate-90': expandedProjects.has(group.projectId) }"
          />
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span class="font-medium text-sm text-gray-900 truncate">{{ getProjectName(group) }}</span>
              <el-tag
                v-if="group.project?.targetType"
                size="small"
                type="info"
              >{{ TargetTypeConfig[group.project.targetType as TargetType]?.label }}</el-tag>
            </div>
            <div class="text-xs text-gray-500 mt-0.5">
              {{ getGroupStatusSummary(group) }}
            </div>
          </div>
          <div class="flex items-center gap-4 shrink-0">
            <div class="text-xs text-gray-500 flex items-center gap-1">
              <Calendar class="w-3.5 h-3.5" />
              {{ group.tasks.length }} 个任务
            </div>
            <div class="text-xs text-gray-500 flex items-center gap-1">
              <Target class="w-3.5 h-3.5" />
              {{ group.completedTargets }}/{{ group.totalTargets }}
            </div>
            <el-button
              link type="primary" size="small"
              @click.stop="goProjectDetail(group.projectId)"
            >项目详情</el-button>
          </div>
        </div>

        <!-- 任务列表（展开时显示） -->
        <div v-show="expandedProjects.has(group.projectId)">
          <div
            v-for="task in group.tasks"
            :key="task.id"
            class="flex items-center gap-3 px-4 py-2.5 border-t border-gray-100 hover:bg-blue-50/30 transition-colors"
          >
            <!-- 日期 -->
            <div class="w-24 shrink-0">
              <div class="text-sm font-medium text-gray-700">{{ task.taskDate }}</div>
            </div>

            <!-- 检查员 -->
            <div class="w-24 shrink-0 text-sm text-gray-500 flex items-center gap-1">
              <Users class="w-3.5 h-3.5 text-gray-400" />
              {{ task.inspectorName || '未分配' }}
            </div>

            <!-- 状态 -->
            <div class="w-20 shrink-0">
              <el-tag
                :type="(TaskStatusConfig[task.status as TaskStatus]?.type as any)"
                size="small"
              >
                {{ TaskStatusConfig[task.status as TaskStatus]?.label }}
              </el-tag>
            </div>

            <!-- 进度 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <el-progress
                  :percentage="task.totalTargets > 0 ? Math.round((task.completedTargets + task.skippedTargets) / task.totalTargets * 100) : 0"
                  :stroke-width="6"
                  :show-text="false"
                  class="flex-1"
                  style="max-width: 120px"
                />
                <span class="text-xs text-gray-500">
                  {{ task.completedTargets + task.skippedTargets }}/{{ task.totalTargets }}
                </span>
              </div>
            </div>

            <!-- 操作 -->
            <div class="flex items-center gap-1 shrink-0">
              <el-button
                v-if="task.status === 'PENDING'"
                link type="primary" size="small"
                @click="handleClaim(task)"
              >领取</el-button>
              <el-button
                v-if="['CLAIMED', 'IN_PROGRESS'].includes(task.status)"
                link type="primary" size="small"
                @click="goExecute(task)"
              >
                <Play class="w-3.5 h-3.5 mr-0.5" />执行
              </el-button>
              <el-button
                v-if="task.status === 'REVIEWED'"
                link type="success" size="small"
                @click="handlePublish(task)"
              >
                <Send class="w-3.5 h-3.5 mr-0.5" />发布
              </el-button>
              <el-button
                v-if="['PENDING', 'CLAIMED'].includes(task.status)"
                link type="danger" size="small"
                @click="handleCancel(task)"
              >
                <XCircle class="w-3.5 h-3.5" />
              </el-button>
              <el-button link size="small" @click="goExecute(task)">
                <Eye class="w-3.5 h-3.5" />
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <InspEmptyState v-if="!loading && groupedTasks.length === 0"
        :message="activeView === 'my' ? '你还没有领取任何检查任务' : activeView === 'available' ? '暂无可领取的任务' : '暂无检查任务'" />
    </div>
  </div>
</template>
