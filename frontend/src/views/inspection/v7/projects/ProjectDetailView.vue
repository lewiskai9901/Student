<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Play, Pause, CheckCircle, Send, Plus, Trash2, Save,
  Calendar, Users, Building2, UserCircle, Settings, BarChart3, ClipboardList, Lock,
  ClipboardCheck, Check, X, ListTree, Star, LayoutDashboard,
} from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/insp/inspExecutionStore'
import {
  ProjectStatusConfig, type ProjectStatus,
  InspectorRoleConfig, type InspectorRole,
  AssignmentModeConfig, type AssignmentMode,
  ScopeTypeConfig, type ScopeType,
} from '@/types/insp/enums'
import type { InspProject, ProjectInspector, InspTask, InspSubmission } from '@/types/insp/project'
import { inspProjectApi, updateOperationalConfig } from '@/api/insp/project'
import { getTasks } from '@/api/insp/task'
import { getSubmissions } from '@/api/insp/submission'
import { getSimpleUserList, getUser } from '@/api/user'
import { getOrgUnitTree } from '@/api/organization'
import type { OrgUnitTreeNode } from '@/api/organization'
import type { SimpleUser } from '@/types/user'
import { getRootSection } from '@/api/insp/template'
import InspectionPlanConfig from './components/InspectionPlanConfig.vue'
import RatingDimensionConfig from './components/RatingDimensionConfig.vue'

const route = useRoute()
const router = useRouter()
const store = useInspExecutionStore()
const projectId = Number(route.params.id)

// ========== State ==========
const loading = ref(false)
const project = ref<InspProject | null>(null)
const inspectors = ref<ProjectInspector[]>([])
const allTasks = ref<InspTask[]>([])
const allSubmissions = ref<InspSubmission[]>([])
const creatorName = ref('')
const rootSectionName = ref('')
const scopeOrgNames = ref<string[]>([])
const orgTree = ref<OrgUnitTreeNode[]>([])
const loadingOrgTree = ref(false)
// Tabs: overview | plans | dimensions | settings
const activeTab = ref('overview')
const configDirty = ref(false)
const saving = ref(false)

// 配置表单
const cf = ref({ scopeType: 'ORG', scopeIds: [] as number[], startDate: '', endDate: '', assignmentMode: 'FREE', reviewRequired: true, autoPublish: false, projectName: '', evaluationMode: 'SINGLE', multiRaterMode: 'AVERAGE', trendEnabled: false, decayEnabled: false, calibrationEnabled: false })

// 检查员添加
const addQuery = ref('')
const addResults = ref<SimpleUser[]>([])
const addLoading = ref(false)
const addRole = ref<InspectorRole>('INSPECTOR')

const isDraft = computed(() => project.value?.status === 'DRAFT')
const isArchived = computed(() => project.value?.status === 'ARCHIVED')
const canPublish = computed(() => isDraft.value && cf.value.scopeIds.length > 0 && !!cf.value.startDate)

// ========== 任务统计 ==========
const taskStats = computed(() => {
  const t = allTasks.value
  return {
    total: t.length,
    pending: t.filter(x => x.status === 'PENDING').length,
    active: t.filter(x => ['CLAIMED', 'IN_PROGRESS'].includes(x.status)).length,
    done: t.filter(x => ['SUBMITTED', 'UNDER_REVIEW', 'REVIEWED', 'PUBLISHED'].includes(x.status)).length,
    totalTargets: t.reduce((s, x) => s + x.totalTargets, 0),
    completedTargets: t.reduce((s, x) => s + x.completedTargets, 0),
  }
})
const progressPct = computed(() => taskStats.value.total === 0 ? 0 : Math.round(taskStats.value.done / taskStats.value.total * 100))

// ========== 审核 ==========
const pendingReviewTasks = computed(() => allTasks.value.filter(t => t.status === 'SUBMITTED'))
const pendingReviewCount = computed(() => pendingReviewTasks.value.length)

async function handleApproveTask(task: InspTask) {
  try {
    await ElMessageBox.confirm(`通过任务 ${task.taskCode} 的审核？`, '确认审核', { type: 'info' })
    await store.reviewTask(task.id, { reviewerName: 'admin', comment: '审核通过' })
    ElMessage.success('审核通过')
    loadProject()
  } catch {}
}

async function handleRejectTask(task: InspTask) {
  try {
    const { value: comment } = await ElMessageBox.prompt('请输入驳回原因', '驳回任务', { type: 'warning', inputPlaceholder: '驳回原因...' }) as any
    await store.reviewTask(task.id, { reviewerName: 'admin', comment: comment || '审核驳回' })
    ElMessage.success('已驳回')
    loadProject()
  } catch {}
}

// ========== 检查结果统计 ==========
const resultStats = computed(() => {
  const subs = allSubmissions.value.filter(s => s.status === 'COMPLETED')
  if (subs.length === 0) return null
  const scores = subs.filter(s => s.finalScore != null).map(s => s.finalScore!)
  const passed = subs.filter(s => s.passed === true).length
  const failed = subs.filter(s => s.passed === false).length
  const avg = scores.length > 0 ? scores.reduce((a, b) => a + b, 0) / scores.length : 0
  const max = scores.length > 0 ? Math.max(...scores) : 0
  const min = scores.length > 0 ? Math.min(...scores) : 0
  const gradeMap = new Map<string, number>()
  for (const s of subs) { if (s.grade) gradeMap.set(s.grade, (gradeMap.get(s.grade) || 0) + 1) }
  return { total: subs.length, avg: avg.toFixed(1), max, min, passed, failed, unrated: subs.filter(s => s.passed == null).length, grades: [...gradeMap.entries()].sort((a, b) => b[1] - a[1]) }
})

// ========== 总览 Tab 数据 ==========
// 分区得分 - 从分析维度/submissions派生
const sectionScores = computed(() => {
  const subs = allSubmissions.value.filter(s => s.status === 'COMPLETED' && s.finalScore != null)
  if (subs.length === 0) return []
  // Group by targetName as a proxy for section
  const map = new Map<string, { name: string; scores: number[] }>()
  for (const s of subs) {
    const key = s.targetName || '未知目标'
    if (!map.has(key)) map.set(key, { name: key, scores: [] })
    map.get(key)!.scores.push(s.finalScore!)
  }
  return [...map.values()]
    .map(g => ({ name: g.name, avg: g.scores.reduce((a, b) => a + b, 0) / g.scores.length }))
    .sort((a, b) => b.avg - a.avg)
    .slice(0, 10)
})

// 最近5条任务
const recentTasks = computed(() => {
  return [...allTasks.value]
    .filter(t => ['SUBMITTED', 'UNDER_REVIEW', 'REVIEWED', 'PUBLISHED'].includes(t.status))
    .sort((a, b) => (b.taskDate || '').localeCompare(a.taskDate || ''))
    .slice(0, 5)
})

// 待整改数 = 未通过的submissions
const pendingCorrectiveCount = computed(() => allSubmissions.value.filter(s => s.passed === false).length)

function getSectionScoreColor(score: number): string {
  if (score >= 85) return '#10b981'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
}

// 按日期合并任务
interface DayTask { date: string; subTasks: { task: InspTask; projectName: string }[]; totalTargets: number; completedTargets: number; inspectorName: string; allDone: boolean }
const dayTasks = computed<DayTask[]>(() => {
  const dateMap = new Map<string, { task: InspTask; projectName: string }[]>()
  for (const task of allTasks.value) {
    if (!dateMap.has(task.taskDate)) dateMap.set(task.taskDate, [])
    dateMap.get(task.taskDate)!.push({ task, projectName: project.value?.projectName || '' })
  }
  return [...dateMap.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([date, subs]) => ({
    date, subTasks: subs,
    totalTargets: subs.reduce((s, x) => s + x.task.totalTargets, 0),
    completedTargets: subs.reduce((s, x) => s + x.task.completedTargets + x.task.skippedTargets, 0),
    inspectorName: subs[0]?.task.inspectorName || '未分配',
    allDone: subs.every(x => ['SUBMITTED', 'UNDER_REVIEW', 'REVIEWED', 'PUBLISHED'].includes(x.task.status)),
  }))
})

const inspectorStats = computed(() => {
  const map = new Map<string, { name: string; assigned: number; completed: number; targets: number }>()
  for (const task of allTasks.value) {
    const name = task.inspectorName || '未分配'
    if (!map.has(name)) map.set(name, { name, assigned: 0, completed: 0, targets: 0 })
    const s = map.get(name)!; s.assigned++
    if (['SUBMITTED', 'UNDER_REVIEW', 'REVIEWED', 'PUBLISHED'].includes(task.status)) s.completed++
    s.targets += task.completedTargets
  }
  return [...map.values()].sort((a, b) => b.assigned - a.assigned)
})

const targetScores = computed(() => {
  return allSubmissions.value
    .filter(s => s.status === 'COMPLETED' && s.finalScore != null)
    .sort((a, b) => (a.finalScore ?? 0) - (b.finalScore ?? 0))
})

// ========== Load ==========
async function loadProject() {
  loading.value = true
  try {
    project.value = await store.loadProject(projectId)
    inspectors.value = await store.loadInspectors(projectId)
    if (isDraft.value) activeTab.value = 'settings'
    syncForm()
    if (project.value.createdBy) { try { const u = await getUser(project.value.createdBy); creatorName.value = u.realName || u.username } catch {} }
    if (project.value.rootSectionId) {
      try { const section = await getRootSection(project.value.rootSectionId); rootSectionName.value = section.sectionName } catch {}
    }
    await loadScopeNames()
    allTasks.value = []; allSubmissions.value = []
    try {
      const tasks = await getTasks({ projectId })
      allTasks.value.push(...tasks)
      for (const t of tasks) { try { allSubmissions.value.push(...await getSubmissions({ taskId: t.id })) } catch {} }
    } catch {}
    configDirty.value = false
  } catch (e: any) { ElMessage.error(e.message || '加载失败') }
  finally { loading.value = false }
}
function syncForm() {
  if (!project.value) return; const p = project.value
  let ids: number[] = []; try { ids = p.scopeConfig ? JSON.parse(p.scopeConfig) : [] } catch {}
  cf.value = { scopeType: p.scopeType || 'ORG', scopeIds: ids, startDate: p.startDate || '', endDate: p.endDate || '', assignmentMode: p.assignmentMode || 'FREE', reviewRequired: p.reviewRequired ?? true, autoPublish: p.autoPublish ?? false, projectName: p.projectName, evaluationMode: (p as any).evaluationMode || 'SINGLE', multiRaterMode: (p as any).multiRaterMode || 'AVERAGE', trendEnabled: (p as any).trendEnabled ?? false, decayEnabled: (p as any).decayEnabled ?? false, calibrationEnabled: (p as any).calibrationEnabled ?? false }
}

// Watch cf changes after initial sync
let watchEnabled = false
function startWatch() { watchEnabled = true }
import { watch } from 'vue'
watch(cf, () => { if (watchEnabled) configDirty.value = true }, { deep: true })

async function loadScopeNames() {
  scopeOrgNames.value = []
  if (!project.value?.scopeConfig) return
  try { const ids: number[] = JSON.parse(project.value.scopeConfig); if (orgTree.value.length === 0) await loadOrgTree(); const m = buildMap(orgTree.value); scopeOrgNames.value = ids.map(id => m.get(id) || `#${id}`) } catch {}
}
function buildMap(nodes: OrgUnitTreeNode[]): Map<number, string> { const m = new Map<number, string>(); function w(l: OrgUnitTreeNode[]) { for (const n of l) { m.set(n.id as number, n.unitName); if (n.children) w(n.children) } }; w(nodes); return m }
async function loadOrgTree() { if (orgTree.value.length > 0) return; loadingOrgTree.value = true; try { orgTree.value = await getOrgUnitTree() } catch {}; loadingOrgTree.value = false }

// ========== Save ==========
async function saveConfig() {
  if (!project.value) return; saving.value = true
  try {
    if (isDraft.value) {
      await inspProjectApi.update(projectId, { projectName: cf.value.projectName, rootSectionId: project.value.rootSectionId, scopeType: cf.value.scopeType as ScopeType, scopeConfig: cf.value.scopeIds.length > 0 ? JSON.stringify(cf.value.scopeIds) : undefined, startDate: cf.value.startDate || undefined, endDate: cf.value.endDate || undefined, assignmentMode: cf.value.assignmentMode as AssignmentMode, reviewRequired: cf.value.reviewRequired, autoPublish: cf.value.autoPublish })
    } else {
      await updateOperationalConfig(projectId, { projectName: cf.value.projectName, assignmentMode: cf.value.assignmentMode, reviewRequired: cf.value.reviewRequired, autoPublish: cf.value.autoPublish })
    }
    ElMessage.success('已保存'); configDirty.value = false; loadProject()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') } finally { saving.value = false }
}

// ========== Actions ==========
async function handlePublish() {
  if (!project.value) return
  if (cf.value.scopeIds.length === 0) { ElMessage.error('请先配置检查范围'); activeTab.value = 'settings'; return }
  if (!cf.value.startDate) { ElMessage.error('请先设置开始日期'); activeTab.value = 'settings'; return }
  if (configDirty.value) await saveConfig()
  try { await ElMessageBox.confirm('确定发布？将自动生成检查任务。', '确认发布', { type: 'warning' }); await store.publishProject(projectId, { templateVersionId: project.value.templateVersionId || null as any }); ElMessage.success('已发布'); activeTab.value = 'overview'; loadProject() } catch {}
}
async function handlePause() { try { await store.pauseProject(projectId); ElMessage.success('已暂停'); loadProject() } catch (e: any) { ElMessage.error(e.message || '失败') } }
async function handleResume() { try { await store.resumeProject(projectId); ElMessage.success('已恢复'); loadProject() } catch (e: any) { ElMessage.error(e.message || '失败') } }
async function handleComplete() { try { await ElMessageBox.confirm('确定完结？', '确认', { type: 'warning' }); await store.completeProject(projectId); ElMessage.success('已完结'); loadProject() } catch {} }
async function handleArchive() { try { await ElMessageBox.confirm('确定归档？归档后不可恢复为活跃状态。', '确认归档', { type: 'warning' }); await inspProjectApi.archive(projectId); ElMessage.success('已归档'); loadProject() } catch {} }
async function handleClaim(task: InspTask) { try { await store.claimTask(task.id, { inspectorName: '当前用户' }); ElMessage.success('已领取'); loadProject() } catch (e: any) { ElMessage.error(e.message || '失败') } }

// ========== Inspector ==========
async function searchUsers(q: string) { if (!q.trim()) { addResults.value = []; return }; addLoading.value = true; try { addResults.value = await getSimpleUserList(q.trim()) } catch { addResults.value = [] }; addLoading.value = false }
async function handleAddInspector(userId: number) {
  const u = addResults.value.find(x => Number(x.id) === userId); if (!u) return
  try { await store.addInspector(projectId, { userId: Number(u.id), userName: u.realName || u.username, role: addRole.value }); ElMessage.success(`已添加 ${u.realName || u.username}`); addQuery.value = ''; addResults.value = []; inspectors.value = await store.loadInspectors(projectId) } catch (e: any) { ElMessage.error(e.message || '失败') }
}
async function handleRemoveInspector(insp: ProjectInspector) { try { await ElMessageBox.confirm(`移除「${insp.userName}」？`, '确认', { type: 'warning' }); await store.removeInspector(projectId, insp.id); inspectors.value = await store.loadInspectors(projectId) } catch {} }

function goBack() { router.push('/inspection/v7/projects') }
function goExecuteTask(taskId: number) { router.push(`/inspection/v7/tasks/${taskId}/execute`) }
onMounted(async () => {
  await loadOrgTree()
  await loadProject()
  startWatch()
})
</script>

<template>
  <div class="pdv" v-loading="loading">
    <!-- ====== Header ====== -->
    <div class="pdv-header">
      <div class="pdv-header-left">
        <button class="pdv-back-btn" @click="goBack">
          <ArrowLeft class="w-4 h-4" />
        </button>
        <div>
          <div class="pdv-title">
            <span>{{ project?.projectName || '加载中...' }}</span>
            <span v-if="project" class="pdv-status-dot" :class="`status-${project.status.toLowerCase()}`">
              {{ ProjectStatusConfig[project.status as ProjectStatus]?.label }}
            </span>
          </div>
          <div class="pdv-subtitle" v-if="project">
            <span v-if="project.startDate">{{ project.startDate }}<template v-if="project.endDate"> ~ {{ project.endDate }}</template></span>
            <span v-if="rootSectionName" class="pdv-subtitle-sep">·</span>
            <span v-if="rootSectionName">{{ rootSectionName }}</span>
            <span v-if="inspectors.length" class="pdv-subtitle-sep">·</span>
            <span v-if="inspectors.length">{{ inspectors.length }} 人</span>
          </div>
        </div>
      </div>
      <div class="pdv-header-actions" v-if="project">
        <el-button v-if="isDraft" type="primary" :disabled="!canPublish" @click="handlePublish" size="small" round>
          <Send class="w-3.5 h-3.5 mr-1" />发布项目
        </el-button>
        <el-button v-if="project.status === 'PUBLISHED'" type="warning" @click="handlePause" size="small" round>
          <Pause class="w-3.5 h-3.5 mr-1" />暂停
        </el-button>
        <el-button v-if="project.status === 'PAUSED'" type="success" @click="handleResume" size="small" round>
          <Play class="w-3.5 h-3.5 mr-1" />恢复
        </el-button>
        <el-button v-if="['PUBLISHED','PAUSED'].includes(project.status)" @click="handleComplete" size="small" round>
          <CheckCircle class="w-3.5 h-3.5 mr-1" />完结
        </el-button>
        <el-button v-if="project.status === 'COMPLETED'" type="info" @click="handleArchive" size="small" round plain>归档</el-button>
      </div>
    </div>

    <!-- ====== Pill Tabs ====== -->
    <div class="pdv-tabs">
      <button :class="['pdv-tab', activeTab === 'overview' && 'active']" @click="activeTab = 'overview'">
        <LayoutDashboard class="w-3.5 h-3.5" />总览
      </button>
      <button :class="['pdv-tab', activeTab === 'plans' && 'active']" @click="activeTab = 'plans'">
        <Calendar class="w-3.5 h-3.5" />检查计划
      </button>
      <button :class="['pdv-tab', activeTab === 'dimensions' && 'active']" @click="activeTab = 'dimensions'">
        <Star class="w-3.5 h-3.5" />评级规则
      </button>
      <button :class="['pdv-tab', activeTab === 'settings' && 'active']" @click="activeTab = 'settings'">
        <Settings class="w-3.5 h-3.5" />设置
        <span v-if="configDirty" class="pdv-tab-dot" />
      </button>
    </div>

    <!-- ====== Tab Content ====== -->
    <div class="pdv-body">

      <!-- ===== 总览 Tab ===== -->
      <div v-if="activeTab === 'overview'">

        <!-- 未发布提示 -->
        <div v-if="isDraft" class="pdv-empty-state">
          <ClipboardList class="w-10 h-10 text-blue-300 mb-3" />
          <div class="font-medium text-gray-600 mb-1">项目尚未发布</div>
          <div class="text-sm text-gray-400 mb-4">请在「设置」中完成配置后发布</div>
          <el-button type="primary" :disabled="!canPublish" @click="handlePublish" size="small" round>
            <Send class="w-3.5 h-3.5 mr-1" />发布项目
          </el-button>
        </div>

        <template v-else>
          <!-- 统计卡片行 -->
          <div class="pdv-stat-row">
            <div class="pdv-stat-card">
              <div class="pdv-stat-value">{{ taskStats.total }}</div>
              <div class="pdv-stat-label">总任务数</div>
            </div>
            <div class="pdv-stat-divider" />
            <div class="pdv-stat-card">
              <div class="pdv-stat-value text-green-600">{{ taskStats.done }}</div>
              <div class="pdv-stat-label">已完成</div>
            </div>
            <div class="pdv-stat-divider" />
            <div class="pdv-stat-card">
              <div class="pdv-stat-value text-blue-600">{{ resultStats ? resultStats.avg : '-' }}</div>
              <div class="pdv-stat-label">平均分</div>
            </div>
            <div class="pdv-stat-divider" />
            <div class="pdv-stat-card">
              <div class="pdv-stat-value text-orange-500">{{ pendingCorrectiveCount }}</div>
              <div class="pdv-stat-label">待整改</div>
            </div>
          </div>

          <!-- 进度条 -->
          <div v-if="taskStats.total > 0" class="pdv-progress-row">
            <div class="pdv-progress-label">
              <span class="text-sm font-medium text-gray-700">执行进度</span>
              <span class="text-sm font-bold text-[#1a6dff]">{{ progressPct }}%</span>
            </div>
            <div class="pdv-progress-track">
              <div class="pdv-progress-fill" :style="{ width: progressPct + '%' }" />
            </div>
            <div class="pdv-progress-stats">
              <span><span class="text-orange-500 font-semibold">{{ taskStats.pending }}</span> 待领取</span>
              <span><span class="text-blue-600 font-semibold">{{ taskStats.active }}</span> 进行中</span>
              <span><span class="text-green-600 font-semibold">{{ taskStats.done }}</span> 已完成</span>
            </div>
          </div>

          <!-- 两列布局: 分区得分 + 最近检查 -->
          <div class="pdv-two-col">

            <!-- 分区得分 -->
            <div class="pdv-card">
              <div class="pdv-card-title">
                <BarChart3 class="w-4 h-4 text-[#1a6dff]" />目标得分分布
              </div>
              <div v-if="sectionScores.length === 0" class="pdv-card-empty">暂无得分数据</div>
              <div v-else class="pdv-score-list">
                <div v-for="item in sectionScores" :key="item.name" class="pdv-score-item">
                  <div class="pdv-score-name" :title="item.name">{{ item.name }}</div>
                  <div class="pdv-score-bar-wrap">
                    <div class="pdv-score-bar-track">
                      <div
                        class="pdv-score-bar-fill"
                        :style="{ width: item.avg + '%', background: getSectionScoreColor(item.avg) }"
                      />
                    </div>
                    <span class="pdv-score-val" :style="{ color: getSectionScoreColor(item.avg) }">
                      {{ item.avg.toFixed(1) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 最近检查 -->
            <div class="pdv-card">
              <div class="pdv-card-title">
                <ClipboardList class="w-4 h-4 text-[#1a6dff]" />最近检查
              </div>
              <div v-if="recentTasks.length === 0" class="pdv-card-empty">暂无已完成任务</div>
              <div v-else class="pdv-recent-list">
                <div v-for="task in recentTasks" :key="task.id" class="pdv-recent-item">
                  <div class="pdv-recent-meta">
                    <span class="pdv-recent-date">{{ task.taskDate }}</span>
                    <span class="pdv-recent-inspector">{{ task.inspectorName || '-' }}</span>
                  </div>
                  <div class="pdv-recent-code text-gray-500 text-xs truncate">{{ task.taskCode }}</div>
                  <div class="flex items-center gap-1.5 mt-1">
                    <el-tag
                      :type="task.status === 'PUBLISHED' ? 'success' : task.status === 'REVIEWED' ? 'primary' : 'warning'"
                      size="small" round effect="plain"
                    >
                      {{ { SUBMITTED:'已提交', UNDER_REVIEW:'审核中', REVIEWED:'已审核', PUBLISHED:'已发布' }[task.status] || task.status }}
                    </el-tag>
                    <span class="text-xs text-gray-400">{{ task.completedTargets }}/{{ task.totalTargets }} 目标</span>
                  </div>
                </div>
              </div>

              <!-- 审核待办 -->
              <div v-if="pendingReviewCount > 0" class="pdv-review-alert">
                <ClipboardCheck class="w-3.5 h-3.5 text-orange-500" />
                <span>{{ pendingReviewCount }} 个任务待审核</span>
                <button class="pdv-review-link" @click="activeTab = 'settings'">去处理</button>
              </div>
            </div>

          </div>

          <!-- 检查结果统计 (仅有数据时) -->
          <div v-if="resultStats" class="pdv-card mt-4">
            <div class="pdv-card-title">
              <BarChart3 class="w-4 h-4 text-[#1a6dff]" />检查结果汇总
            </div>
            <div class="pdv-result-stats">
              <div class="pdv-result-item">
                <div class="pdv-result-val">{{ resultStats.total }}</div>
                <div class="pdv-result-lbl">已完成</div>
              </div>
              <div class="pdv-result-divider" />
              <div class="pdv-result-item">
                <div class="pdv-result-val text-green-600">{{ resultStats.passed }}</div>
                <div class="pdv-result-lbl">通过</div>
              </div>
              <div class="pdv-result-divider" />
              <div class="pdv-result-item">
                <div class="pdv-result-val text-red-500">{{ resultStats.failed }}</div>
                <div class="pdv-result-lbl">未通过</div>
              </div>
              <div class="pdv-result-divider" />
              <div class="pdv-result-item">
                <div class="pdv-result-val text-blue-600">{{ resultStats.avg }}</div>
                <div class="pdv-result-lbl">平均分</div>
              </div>
              <div class="pdv-result-divider" />
              <div class="pdv-result-item">
                <div class="pdv-result-val text-gray-700">{{ resultStats.max }}<span class="text-xs font-normal text-gray-400">/{{ resultStats.min }}</span></div>
                <div class="pdv-result-lbl">最高/最低</div>
              </div>
            </div>

            <!-- 评级分布 -->
            <div v-if="resultStats.grades.length > 0" class="mt-4 pt-3 border-t border-gray-100">
              <div class="text-xs font-medium text-gray-500 mb-2">评级分布</div>
              <div class="flex items-end gap-4">
                <div v-for="[grade, count] in resultStats.grades" :key="grade" class="text-center">
                  <div class="w-10 bg-blue-100 rounded-t mx-auto flex items-end justify-center"
                       :style="{ height: Math.max(16, count / resultStats.total * 80) + 'px' }">
                    <span class="text-[10px] font-bold text-blue-700 pb-0.5">{{ count }}</span>
                  </div>
                  <div class="text-xs text-gray-500 mt-0.5 font-medium">{{ grade }}</div>
                </div>
              </div>
            </div>

            <!-- 目标得分明细 -->
            <div v-if="targetScores.length > 0" class="mt-4 pt-3 border-t border-gray-100">
              <div class="text-xs font-medium text-gray-500 mb-2">目标得分明细 <span class="text-gray-300 font-normal">（低→高）</span></div>
              <el-table :data="targetScores.slice(0, 20)" max-height="280" size="small">
                <el-table-column prop="targetName" label="检查目标" min-width="160" />
                <el-table-column label="分数" width="80" align="center">
                  <template #default="{ row }">
                    <span class="font-bold" :style="{ color: getSectionScoreColor(row.finalScore ?? 0) }">
                      {{ row.finalScore?.toFixed(1) ?? '-' }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column label="评级" width="70" align="center">
                  <template #default="{ row }">
                    <el-tag v-if="row.grade" size="small" round effect="plain">{{ row.grade }}</el-tag>
                    <span v-else class="text-gray-300">-</span>
                  </template>
                </el-table-column>
                <el-table-column label="结果" width="70" align="center">
                  <template #default="{ row }">
                    <el-tag v-if="row.passed === true" type="success" size="small" round effect="dark">通过</el-tag>
                    <el-tag v-else-if="row.passed === false" type="danger" size="small" round effect="dark">未通过</el-tag>
                    <span v-else class="text-gray-300">-</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>

        </template>
      </div>

      <!-- ===== 检查计划 Tab ===== -->
      <div v-if="activeTab === 'plans'">
        <InspectionPlanConfig :project-id="projectId" />
      </div>

      <!-- ===== 评级规则 Tab ===== -->
      <div v-if="activeTab === 'dimensions'">
        <RatingDimensionConfig :project-id="projectId" />
      </div>

      <!-- ===== 设置 Tab ===== -->
      <div v-if="activeTab === 'settings'" class="space-y-5">

        <!-- 锁定提示 -->
        <div v-if="!isDraft" class="pdv-lock-notice">
          <Lock class="w-4 h-4 text-amber-500 mt-0.5 flex-shrink-0" />
          <div>
            <div class="font-medium text-amber-800 text-sm">部分配置已锁定</div>
            <div class="text-amber-600 text-xs mt-0.5">发布后，检查范围和根分区不可更改。项目名称和运营配置可随时调整。</div>
          </div>
        </div>

        <!-- 基本信息 -->
        <div class="cfg-card">
          <div class="cfg-card-title">基本信息</div>
          <div class="cfg-field">
            <label class="cfg-label">项目名称</label>
            <el-input v-model="cf.projectName" :disabled="isArchived" placeholder="输入项目名称" />
          </div>
          <div class="cfg-field mt-3">
            <label class="cfg-label">根分区</label>
            <div class="text-sm text-gray-700">{{ rootSectionName || '-' }} <span class="text-xs text-gray-400">(ID: {{ project?.rootSectionId }})</span></div>
          </div>
        </div>

        <!-- 检查范围 -->
        <div class="cfg-card" :class="{ 'cfg-locked': !isDraft }">
          <div class="cfg-card-header">
            <div class="cfg-card-title">检查范围</div>
            <Lock v-if="!isDraft" class="w-3.5 h-3.5 text-gray-300" />
          </div>
          <div class="cfg-desc">选择哪些组织单元参与本次检查，系统将根据分区的目标类型自动派生具体检查对象。</div>
          <div class="grid grid-cols-2 gap-4 mt-3">
            <div class="cfg-field">
              <label class="cfg-label">范围类型</label>
              <el-select v-model="cf.scopeType" class="w-full" :disabled="!isDraft">
                <el-option v-for="(c,k) in ScopeTypeConfig" :key="k" :label="c.label" :value="k" />
              </el-select>
            </div>
            <div class="cfg-field">
              <label class="cfg-label">检查对象 <span v-if="isDraft" class="text-red-400">*</span></label>
              <el-tree-select v-model="cf.scopeIds" :data="orgTree" multiple show-checkbox check-strictly node-key="id" :props="{label:'unitName',children:'children'}" :placeholder="isDraft ? '选择组织单元' : ''" class="w-full" :loading="loadingOrgTree" collapse-tags collapse-tags-tooltip filterable :render-after-expand="false" :disabled="!isDraft" />
            </div>
          </div>
          <div v-if="cf.scopeIds.length > 0 && isDraft" class="text-xs text-gray-400 mt-2">
            已选 {{ cf.scopeIds.length }} 个组织单元，发布后将自动匹配下属场所、部门等目标
          </div>
        </div>

        <!-- 时间范围 -->
        <div class="cfg-card" :class="{ 'cfg-locked': !isDraft }">
          <div class="cfg-card-header">
            <div class="cfg-card-title">时间范围</div>
            <Lock v-if="!isDraft" class="w-3.5 h-3.5 text-gray-300" />
          </div>
          <div class="cfg-desc">设置检查的起止时间。具体调度频率在「检查计划」标签页中配置。</div>
          <div class="grid grid-cols-2 gap-4 mt-3">
            <div class="cfg-field">
              <label class="cfg-label">开始日期 <span v-if="isDraft" class="text-red-400">*</span></label>
              <el-date-picker v-model="cf.startDate" type="date" value-format="YYYY-MM-DD" class="!w-full" :disabled="!isDraft" placeholder="选择日期" />
            </div>
            <div class="cfg-field">
              <label class="cfg-label">结束日期</label>
              <el-date-picker v-model="cf.endDate" type="date" value-format="YYYY-MM-DD" class="!w-full" :disabled="!isDraft" placeholder="选择日期" />
            </div>
          </div>
        </div>

        <!-- 运营配置 -->
        <div class="cfg-card">
          <div class="cfg-card-title">运营配置</div>
          <div class="cfg-desc">以下配置可随时调整，不影响已生成的任务结构。</div>
          <div class="grid grid-cols-3 gap-5 mt-3">
            <div class="cfg-field">
              <label class="cfg-label">任务分配方式</label>
              <el-select v-model="cf.assignmentMode" class="w-full" :disabled="isArchived">
                <el-option v-for="(c,k) in AssignmentModeConfig" :key="k" :label="c.label" :value="k" />
              </el-select>
              <div class="text-xs text-gray-400 mt-1">{{ { FREE:'任何检查员可自由领取任务', ASSIGNED:'管理员手动指派给特定检查员' }[cf.assignmentMode] || '' }}</div>
            </div>
            <div class="cfg-field">
              <label class="cfg-label">提交后审核</label>
              <div class="mt-1"><el-switch v-model="cf.reviewRequired" :disabled="isArchived" /></div>
              <div class="text-xs text-gray-400 mt-1">{{ cf.reviewRequired ? '检查员提交后需审核员审批' : '提交后直接生效，无需审核' }}</div>
            </div>
            <div class="cfg-field">
              <label class="cfg-label">自动发布结果</label>
              <div class="mt-1"><el-switch v-model="cf.autoPublish" :disabled="isArchived" /></div>
              <div class="text-xs text-gray-400 mt-1">{{ cf.autoPublish ? '审核通过后自动发布分数' : '需手动发布检查结果' }}</div>
            </div>
          </div>
        </div>

        <!-- 高级评分设置 -->
        <div class="cfg-card">
          <div class="cfg-card-title">高级评分设置</div>
          <div class="cfg-desc">配置本次检查的评分方式。</div>
          <div class="grid grid-cols-2 gap-5 mt-3">
            <div class="cfg-field">
              <label class="cfg-label">评分模式</label>
              <el-select v-model="cf.evaluationMode" class="w-full" :disabled="isArchived">
                <el-option value="SINGLE" label="单人评分" />
                <el-option value="MULTI" label="多人评分" />
              </el-select>
              <div class="text-xs text-gray-400 mt-1">{{ cf.evaluationMode === 'SINGLE' ? '每个目标由一名检查员独立检查' : '每个目标由多名检查员分别检查，结果合并' }}</div>
            </div>
            <div class="cfg-field" v-if="cf.evaluationMode === 'MULTI'">
              <label class="cfg-label">多人合并方式</label>
              <el-select v-model="cf.multiRaterMode" class="w-full" :disabled="isArchived">
                <el-option value="AVERAGE" label="取平均" />
                <el-option value="MEDIAN" label="取中位数" />
                <el-option value="MAX" label="取最高" />
                <el-option value="MIN" label="取最低" />
                <el-option value="CONSENSUS" label="共识模式" />
              </el-select>
            </div>
          </div>
          <div class="flex items-center gap-6 mt-4 pt-3 border-t border-gray-100">
            <label class="flex items-center gap-2 text-sm text-gray-600 cursor-pointer">
              <el-switch v-model="cf.trendEnabled" :disabled="isArchived" size="small" />
              <span>趋势因子</span>
              <span class="text-xs text-gray-400">根据历史趋势自动加减分</span>
            </label>
            <label class="flex items-center gap-2 text-sm text-gray-600 cursor-pointer">
              <el-switch v-model="cf.decayEnabled" :disabled="isArchived" size="small" />
              <span>分数衰减</span>
              <span class="text-xs text-gray-400">分数随时间递减</span>
            </label>
            <label class="flex items-center gap-2 text-sm text-gray-600 cursor-pointer">
              <el-switch v-model="cf.calibrationEnabled" :disabled="isArchived" size="small" />
              <span>尺度校准</span>
              <span class="text-xs text-gray-400">校正不同检查员的评分尺度差异</span>
            </label>
          </div>
        </div>

        <!-- 保存按钮 -->
        <div v-if="configDirty && !isArchived" class="flex items-center gap-3 pb-2">
          <el-button type="primary" :loading="saving" @click="saveConfig" round size="small">
            <Save class="w-3.5 h-3.5 mr-1" />保存配置
          </el-button>
          <span class="text-xs text-gray-400">有未保存的修改</span>
        </div>

        <!-- 检查员管理 -->
        <div class="cfg-card">
          <div class="cfg-card-header mb-3">
            <div class="cfg-card-title flex items-center gap-1.5">
              <Users class="w-4 h-4 text-[#1a6dff]" />检查员管理
              <span v-if="inspectors.length" class="text-xs font-normal text-gray-400">({{ inspectors.length }} 人)</span>
            </div>
          </div>

          <!-- 添加 -->
          <div class="mb-4 p-3 bg-blue-50/50 border border-blue-100 rounded-xl">
            <div class="text-xs font-medium text-gray-600 mb-2">添加检查员</div>
            <div class="flex items-center gap-2">
              <div class="flex-1">
                <el-select v-model="addQuery" filterable remote reserve-keyword :remote-method="searchUsers" :loading="addLoading" placeholder="输入姓名搜索..." class="w-full" size="default" @change="handleAddInspector" clearable>
                  <el-option v-for="u in addResults" :key="Number(u.id)" :label="(u.realName||u.username) + (u.orgUnitName ? ` (${u.orgUnitName})` : '')" :value="Number(u.id)">
                    <div class="flex items-center justify-between"><span class="font-medium">{{ u.realName || u.username }}</span><span class="text-xs text-gray-400">{{ u.orgUnitName || u.username }}</span></div>
                  </el-option>
                </el-select>
              </div>
              <div class="w-28">
                <el-select v-model="addRole">
                  <el-option v-for="(v,k) in InspectorRoleConfig" :key="k" :label="v.label" :value="k" />
                </el-select>
              </div>
            </div>
          </div>

          <!-- 工作量 -->
          <div v-if="inspectorStats.length > 0 && !isDraft" class="grid grid-cols-4 gap-2 mb-4">
            <div v-for="stat in inspectorStats" :key="stat.name" class="bg-gray-50 border border-gray-200 rounded-xl px-3 py-2">
              <div class="font-medium text-gray-800 text-sm truncate">{{ stat.name }}</div>
              <div class="flex gap-3 mt-1.5 text-xs">
                <div><span class="text-gray-400">分配 </span><span class="font-bold text-gray-700">{{ stat.assigned }}</span></div>
                <div><span class="text-gray-400">完成 </span><span class="font-bold text-green-600">{{ stat.completed }}</span></div>
              </div>
            </div>
          </div>

          <!-- 列表 -->
          <div v-if="inspectors.length === 0" class="py-6 text-center text-gray-400 text-sm">暂无检查员</div>
          <div v-else class="grid grid-cols-2 gap-2">
            <div v-for="insp in inspectors" :key="insp.id" class="flex items-center gap-2.5 p-2.5 bg-white border border-gray-200 rounded-xl">
              <div class="w-8 h-8 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 text-white flex items-center justify-center text-xs font-bold">{{ (insp.userName || '?')[0] }}</div>
              <div class="flex-1 min-w-0">
                <div class="font-medium text-gray-800 text-sm">{{ insp.userName }}</div>
                <div class="text-xs text-gray-400">{{ InspectorRoleConfig[insp.role as InspectorRole]?.label }}</div>
              </div>
              <el-tag :type="insp.isActive ? 'success' : 'info'" size="small" round effect="plain">{{ insp.isActive ? '启用' : '禁用' }}</el-tag>
              <el-button link type="danger" size="small" @click="handleRemoveInspector(insp)"><Trash2 class="w-3.5 h-3.5" /></el-button>
            </div>
          </div>
        </div>

        <!-- 审核待办 -->
        <div v-if="pendingReviewCount > 0" class="cfg-card">
          <div class="cfg-card-title flex items-center gap-1.5 mb-3">
            <ClipboardCheck class="w-4 h-4 text-[#1a6dff]" />待审核任务
            <span class="pdv-badge-red">{{ pendingReviewCount }}</span>
          </div>
          <div class="space-y-2">
            <div v-for="task in pendingReviewTasks" :key="task.id"
                 class="flex items-center justify-between p-3 bg-orange-50 border border-orange-100 rounded-xl">
              <div class="flex-1 min-w-0">
                <div class="font-medium text-gray-800 text-sm">{{ task.taskCode }}</div>
                <div class="text-xs text-gray-400 mt-0.5">检查员: {{ task.inspectorName || '-' }} · {{ task.updatedAt?.substring(0, 16) || '-' }}</div>
              </div>
              <div class="flex items-center gap-1.5">
                <el-tag type="warning" size="small" round>待审核</el-tag>
                <el-button type="success" size="small" @click="handleApproveTask(task)"><Check class="w-3.5 h-3.5 mr-0.5" />通过</el-button>
                <el-button type="danger" size="small" plain @click="handleRejectTask(task)"><X class="w-3.5 h-3.5 mr-0.5" />驳回</el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 操作区 -->
        <div class="cfg-card">
          <div class="cfg-card-title mb-3">项目操作</div>
          <div class="flex flex-wrap gap-2">
            <el-button v-if="project?.status === 'PUBLISHED'" type="warning" size="small" plain @click="handlePause" round>
              <Pause class="w-3.5 h-3.5 mr-1" />暂停项目
            </el-button>
            <el-button v-if="project?.status === 'PAUSED'" type="success" size="small" plain @click="handleResume" round>
              <Play class="w-3.5 h-3.5 mr-1" />恢复项目
            </el-button>
            <el-button v-if="['PUBLISHED','PAUSED'].includes(project?.status || '')" size="small" plain @click="handleComplete" round>
              <CheckCircle class="w-3.5 h-3.5 mr-1" />完结项目
            </el-button>
            <el-button v-if="project?.status === 'COMPLETED'" type="info" size="small" plain @click="handleArchive" round>归档项目</el-button>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
/* ========== Layout ========== */
.pdv {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

/* ========== Header ========== */
.pdv-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}
.pdv-header-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.pdv-back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
  margin-top: 2px;
}
.pdv-back-btn:hover { background: #f3f4f6; color: #374151; }
.pdv-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  line-height: 1.3;
}
.pdv-status-dot {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 20px;
}
.pdv-status-dot.status-draft     { background: #f3f4f6; color: #6b7280; }
.pdv-status-dot.status-published { background: #dcfce7; color: #16a34a; }
.pdv-status-dot.status-paused    { background: #fef9c3; color: #ca8a04; }
.pdv-status-dot.status-completed { background: #dbeafe; color: #2563eb; }
.pdv-status-dot.status-archived  { background: #f3f4f6; color: #9ca3af; }
.pdv-subtitle {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #9ca3af;
  margin-top: 3px;
}
.pdv-subtitle-sep { color: #d1d5db; }
.pdv-header-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }

/* ========== Pill Tabs ========== */
.pdv-tabs {
  display: flex;
  gap: 4px;
  background: #f3f4f6;
  border-radius: 10px;
  padding: 3px;
  margin-bottom: 20px;
  width: fit-content;
}
.pdv-tab {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.pdv-tab:hover { color: #374151; background: rgba(255,255,255,0.6); }
.pdv-tab.active { color: #1a6dff; background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,0.1); font-weight: 600; }
.pdv-tab-dot { width: 6px; height: 6px; border-radius: 50%; background: #f59e0b; flex-shrink: 0; }

/* ========== Body ========== */
.pdv-body { min-height: 300px; }

/* ========== Empty state ========== */
.pdv-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  text-align: center;
}

/* ========== Overview: Stat Row ========== */
.pdv-stat-row {
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px 24px;
  margin-bottom: 16px;
  gap: 0;
}
.pdv-stat-card {
  flex: 1;
  text-align: center;
}
.pdv-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
}
.pdv-stat-label {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
}
.pdv-stat-divider {
  width: 1px;
  height: 32px;
  background: #e5e7eb;
  flex-shrink: 0;
}

/* ========== Progress ========== */
.pdv-progress-row {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 14px 20px;
  margin-bottom: 16px;
}
.pdv-progress-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.pdv-progress-track {
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}
.pdv-progress-fill {
  height: 100%;
  background: #1a6dff;
  border-radius: 4px;
  transition: width 0.4s ease;
}
.pdv-progress-stats {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  font-size: 12px;
  color: #6b7280;
}

/* ========== Two-column layout ========== */
.pdv-two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 0;
}

/* ========== Cards ========== */
.pdv-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
}
.pdv-card-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}
.pdv-card-empty {
  padding: 24px 0;
  text-align: center;
  font-size: 12px;
  color: #d1d5db;
}

/* ========== Section Score List ========== */
.pdv-score-list { display: flex; flex-direction: column; gap: 8px; }
.pdv-score-item { display: flex; align-items: center; gap: 8px; }
.pdv-score-name {
  font-size: 12px;
  color: #4b5563;
  width: 110px;
  flex-shrink: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pdv-score-bar-wrap { display: flex; align-items: center; gap: 6px; flex: 1; }
.pdv-score-bar-track { flex: 1; height: 6px; background: #f3f4f6; border-radius: 3px; overflow: hidden; }
.pdv-score-bar-fill { height: 100%; border-radius: 3px; transition: width 0.3s ease; }
.pdv-score-val { font-size: 12px; font-weight: 600; width: 36px; text-align: right; flex-shrink: 0; }

/* ========== Recent Tasks ========== */
.pdv-recent-list { display: flex; flex-direction: column; gap: 8px; }
.pdv-recent-item {
  padding: 8px 10px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}
.pdv-recent-meta { display: flex; align-items: center; justify-content: space-between; margin-bottom: 2px; }
.pdv-recent-date { font-size: 12px; font-weight: 500; color: #374151; }
.pdv-recent-inspector { font-size: 11px; color: #9ca3af; }
.pdv-recent-code { font-size: 11px; }

.pdv-review-alert {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 10px;
  padding: 6px 10px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 8px;
  font-size: 12px;
  color: #9a3412;
}
.pdv-review-link {
  margin-left: auto;
  font-size: 12px;
  font-weight: 500;
  color: #1a6dff;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}
.pdv-review-link:hover { text-decoration: underline; }

/* ========== Result Stats ========== */
.pdv-result-stats {
  display: flex;
  align-items: center;
  gap: 0;
}
.pdv-result-item { flex: 1; text-align: center; }
.pdv-result-val { font-size: 20px; font-weight: 700; color: #1f2937; }
.pdv-result-lbl { font-size: 11px; color: #9ca3af; margin-top: 2px; }
.pdv-result-divider { width: 1px; height: 28px; background: #e5e7eb; flex-shrink: 0; }

/* ========== Badge ========== */
.pdv-badge-red {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: #ef4444;
  border-radius: 9px;
}

/* ========== Config card styles (settings tab) ========== */
.cfg-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  transition: border-color 0.2s;
}
.cfg-card:hover { border-color: #d1d5db; }
.cfg-card.cfg-locked {
  background: #fafafa;
  border-style: dashed;
}
.cfg-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.cfg-card-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}
.cfg-desc {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
  line-height: 1.5;
}
.cfg-field { display: flex; flex-direction: column; }
.cfg-label {
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 4px;
}

/* ========== Lock notice ========== */
.pdv-lock-notice {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 10px;
}
</style>
