<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Play, Pause, CheckCircle, Send, Plus, Trash2, Save,
  Calendar, Users, Building2, UserCircle, Settings, BarChart3, ClipboardList, Lock,
  Info, ClipboardCheck, Check, X, ListTree, Star,
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
const activeTab = ref('tasks')
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
  return { total: t.length, pending: t.filter(x => x.status === 'PENDING').length, active: t.filter(x => ['CLAIMED', 'IN_PROGRESS'].includes(x.status)).length, done: t.filter(x => ['SUBMITTED', 'UNDER_REVIEW', 'REVIEWED', 'PUBLISHED'].includes(x.status)).length, totalTargets: t.reduce((s, x) => s + x.totalTargets, 0), completedTargets: t.reduce((s, x) => s + x.completedTargets, 0) }
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
  const unrated = subs.filter(s => s.passed == null).length
  const avg = scores.length > 0 ? scores.reduce((a, b) => a + b, 0) / scores.length : 0
  const max = scores.length > 0 ? Math.max(...scores) : 0
  const min = scores.length > 0 ? Math.min(...scores) : 0
  const gradeMap = new Map<string, number>()
  for (const s of subs) { if (s.grade) gradeMap.set(s.grade, (gradeMap.get(s.grade) || 0) + 1) }
  return { total: subs.length, avg: avg.toFixed(1), max, min, passed, failed, unrated, grades: [...gradeMap.entries()].sort((a, b) => b[1] - a[1]) }
})

const targetScores = computed(() => {
  const subs = allSubmissions.value.filter(s => s.status === 'COMPLETED' && s.finalScore != null)
  return subs.sort((a, b) => (a.finalScore ?? 0) - (b.finalScore ?? 0))
})

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

// ========== Load ==========
async function loadProject() {
  loading.value = true
  try {
    project.value = await store.loadProject(projectId)
    inspectors.value = await store.loadInspectors(projectId)
    if (isDraft.value) activeTab.value = 'config'
    syncForm()
    if (project.value.createdBy) { try { const u = await getUser(project.value.createdBy); creatorName.value = u.realName || u.username } catch {} }
    // Load root section name
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
watch(cf, () => { configDirty.value = true }, { deep: true })

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

// ========== Lifecycle ==========
async function handlePublish() {
  if (!project.value) return
  if (cf.value.scopeIds.length === 0) { ElMessage.error('请先配置检查范围'); activeTab.value = 'config'; return }
  if (!cf.value.startDate) { ElMessage.error('请先设置开始日期'); activeTab.value = 'config'; return }
  if (configDirty.value) await saveConfig()
  try { await ElMessageBox.confirm('确定发布？将自动生成检查任务。', '确认发布', { type: 'warning' }); await store.publishProject(projectId, { templateVersionId: project.value.templateVersionId || null as any }); ElMessage.success('已发布'); activeTab.value = 'tasks'; loadProject() } catch {}
}
async function handlePause() { try { await store.pauseProject(projectId); ElMessage.success('已暂停'); loadProject() } catch (e: any) { ElMessage.error(e.message || '失败') } }
async function handleResume() { try { await store.resumeProject(projectId); ElMessage.success('已恢复'); loadProject() } catch (e: any) { ElMessage.error(e.message || '失败') } }
async function handleComplete() { try { await ElMessageBox.confirm('确定完结？', '确认', { type: 'warning' }); await store.completeProject(projectId); ElMessage.success('已完结'); loadProject() } catch {} }
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
onMounted(() => { loadOrgTree(); loadProject() })
</script>

<template>
  <div class="p-6 max-w-[1200px] mx-auto" v-loading="loading">
    <!-- ====== Header ====== -->
    <div class="flex items-center justify-between mb-5">
      <div class="flex items-center gap-3">
        <el-button link @click="goBack"><ArrowLeft class="w-4 h-4" /></el-button>
        <h2 class="text-xl font-bold text-gray-900">{{ project?.projectName || '...' }}</h2>
        <el-tag v-if="project" :type="(ProjectStatusConfig[project.status as ProjectStatus]?.type as any)" effect="dark" round>
          {{ ProjectStatusConfig[project.status as ProjectStatus]?.label }}
        </el-tag>
      </div>
      <div class="flex items-center gap-2" v-if="project">
        <el-button v-if="isDraft" type="primary" :disabled="!canPublish" @click="handlePublish" round><Send class="w-4 h-4 mr-1" />发布项目</el-button>
        <el-button v-if="project.status === 'PUBLISHED'" type="warning" @click="handlePause" round><Pause class="w-4 h-4 mr-1" />暂停</el-button>
        <el-button v-if="project.status === 'PAUSED'" type="success" @click="handleResume" round><Play class="w-4 h-4 mr-1" />恢复</el-button>
        <el-button v-if="['PUBLISHED','PAUSED'].includes(project.status)" @click="handleComplete" round><CheckCircle class="w-4 h-4 mr-1" />完结</el-button>
      </div>
    </div>

    <!-- ====== Dashboard ====== -->
    <div v-if="project" class="rounded-xl bg-gradient-to-r from-slate-50 to-blue-50 border border-slate-200 p-5 mb-5">
      <div class="grid grid-cols-4 gap-6 text-sm">
        <div><div class="text-xs text-gray-400 mb-1 flex items-center gap-1"><UserCircle class="w-3.5 h-3.5" />发起人</div><div class="font-semibold text-gray-800">{{ creatorName || '-' }}</div></div>
        <div><div class="text-xs text-gray-400 mb-1 flex items-center gap-1"><Building2 class="w-3.5 h-3.5" />检查范围</div><div class="font-semibold text-gray-800 truncate" :title="scopeOrgNames.join('、')">{{ scopeOrgNames.length > 0 ? scopeOrgNames.join('、') : '未配置' }}</div></div>
        <div><div class="text-xs text-gray-400 mb-1 flex items-center gap-1"><Calendar class="w-3.5 h-3.5" />时间范围</div><div class="font-semibold text-gray-800">{{ project.startDate }}<template v-if="project.endDate"> ~ {{ project.endDate }}</template></div></div>
        <div><div class="text-xs text-gray-400 mb-1 flex items-center gap-1"><ListTree class="w-3.5 h-3.5" />根分区</div><div class="font-semibold text-gray-800">{{ rootSectionName || `#${project.rootSectionId}` }}</div></div>
      </div>
      <!-- Progress -->
      <div v-if="!isDraft && taskStats.total > 0" class="mt-4 pt-4 border-t border-slate-200/60">
        <div class="flex items-center gap-5">
          <div class="flex-1">
            <el-progress :percentage="progressPct" :stroke-width="10" :show-text="false" color="#3b82f6" />
          </div>
          <div class="flex items-center gap-5 text-sm shrink-0">
            <div class="text-center"><div class="text-lg font-bold text-orange-500">{{ taskStats.pending }}</div><div class="text-[10px] text-gray-400">待领取</div></div>
            <div class="text-center"><div class="text-lg font-bold text-blue-600">{{ taskStats.active }}</div><div class="text-[10px] text-gray-400">进行中</div></div>
            <div class="text-center"><div class="text-lg font-bold text-green-600">{{ taskStats.done }}</div><div class="text-[10px] text-gray-400">已完成</div></div>
            <div class="text-center"><div class="text-lg font-bold text-gray-800">{{ taskStats.completedTargets }}<span class="text-xs font-normal text-gray-400">/{{ taskStats.totalTargets }}</span></div><div class="text-[10px] text-gray-400">检查目标</div></div>
          </div>
        </div>
      </div>
    </div>

    <!-- ====== Tabs ====== -->
    <el-tabs v-model="activeTab" type="border-card" class="detail-tabs">

      <!-- ===== 任务管理 ===== -->
      <el-tab-pane name="tasks">
        <template #label><div class="flex items-center gap-1.5 px-1"><ClipboardList class="w-4 h-4" /><span>任务管理</span></div></template>
        <div v-if="isDraft" class="py-16 text-center">
          <div class="w-16 h-16 rounded-full bg-blue-50 flex items-center justify-center mx-auto mb-4"><ClipboardList class="w-8 h-8 text-blue-400" /></div>
          <div class="text-gray-600 font-medium mb-1">项目尚未发布</div>
          <div class="text-sm text-gray-400 mb-4">请在「项目配置」中完成检查范围和时间设置后发布</div>
          <el-button type="primary" :disabled="!canPublish" @click="handlePublish" round><Send class="w-4 h-4 mr-1" />发布项目</el-button>
        </div>
        <div v-else-if="dayTasks.length === 0" class="py-16 text-center text-sm text-gray-400">暂无任务</div>
        <div v-else>
          <el-table :data="dayTasks" class="rounded-lg overflow-hidden">
            <el-table-column prop="date" label="检查日期" width="120" />
            <el-table-column label="检查员" width="100">
              <template #default="{ row }"><span :class="row.inspectorName === '未分配' ? 'text-gray-300 italic' : 'font-medium'">{{ row.inspectorName }}</span></template>
            </el-table-column>
            <el-table-column label="完成情况" min-width="300">
              <template #default="{ row }">
                <div class="flex items-center gap-1.5 flex-wrap">
                  <span v-for="sub in row.subTasks" :key="sub.task.id" class="text-xs px-2.5 py-1 rounded-full border"
                    :class="['SUBMITTED','UNDER_REVIEW','REVIEWED','PUBLISHED'].includes(sub.task.status) ? 'bg-green-50 text-green-700 border-green-200' : sub.task.status === 'IN_PROGRESS' ? 'bg-blue-50 text-blue-700 border-blue-200' : 'bg-gray-50 text-gray-500 border-gray-200'">
                    {{ sub.projectName }} <span v-if="sub.task.totalTargets" class="opacity-60 ml-0.5">{{ sub.task.completedTargets }}/{{ sub.task.totalTargets }}</span>
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.allDone" type="success" effect="plain" round size="small">已完成</el-tag>
                <el-button v-else-if="row.subTasks.some((s: any) => s.task.status === 'PENDING')" type="primary" size="small" round @click="handleClaim(row.subTasks.find((s: any) => s.task.status === 'PENDING')!.task)">领取</el-button>
                <el-button v-else type="primary" size="small" round plain @click="goExecuteTask(row.subTasks.find((s: any) => ['CLAIMED','IN_PROGRESS'].includes(s.task.status))?.task?.id || row.subTasks[0].task.id)"><Play class="w-3.5 h-3.5 mr-0.5" />执行</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- ===== 检查计划 ===== -->
      <el-tab-pane name="plans">
        <template #label><div class="flex items-center gap-1.5 px-1"><Calendar class="w-4 h-4" /><span>检查计划</span></div></template>
        <InspectionPlanConfig :project-id="projectId" />
      </el-tab-pane>

      <!-- ===== 项目配置 ===== -->
      <el-tab-pane name="config">
        <template #label><div class="flex items-center gap-1.5 px-1"><Settings class="w-4 h-4" /><span>项目配置</span><span v-if="configDirty" class="w-2 h-2 bg-blue-500 rounded-full" /></div></template>

        <div class="space-y-5">

          <!-- 锁定提示 -->
          <div v-if="!isDraft" class="flex items-start gap-3 p-3 bg-amber-50 border border-amber-200 rounded-xl text-sm">
            <Lock class="w-4 h-4 text-amber-500 mt-0.5 flex-shrink-0" />
            <div>
              <div class="font-medium text-amber-800">部分配置已锁定</div>
              <div class="text-amber-600 text-xs mt-0.5">发布后，检查范围和根分区已用于生成任务，修改会导致已有数据不一致，因此不可更改。项目名称和运营配置可随时调整。</div>
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

          <!-- 评分策略 -->
          <div class="cfg-card">
            <div class="cfg-card-title">评分策略</div>
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

          <!-- Save -->
          <div v-if="configDirty && !isArchived" class="flex items-center gap-3 pt-1 pb-4">
            <el-button type="primary" :loading="saving" @click="saveConfig" round><Save class="w-4 h-4 mr-1" />保存配置</el-button>
            <span class="text-xs text-gray-400">有未保存的修改</span>
          </div>
        </div>
      </el-tab-pane>

      <!-- ===== 检查员 ===== -->
      <el-tab-pane name="inspectors">
        <template #label><div class="flex items-center gap-1.5 px-1"><Users class="w-4 h-4" /><span>检查员</span><el-badge v-if="inspectors.length" :value="inspectors.length" type="info" class="ml-0.5" /></div></template>

        <!-- 添加 -->
        <div class="mb-5 p-4 bg-blue-50/50 border border-blue-100 rounded-xl">
          <div class="text-sm font-medium text-gray-700 mb-2">添加检查员</div>
          <div class="flex items-end gap-3">
            <div class="flex-1">
              <el-select v-model="addQuery" filterable remote reserve-keyword :remote-method="searchUsers" :loading="addLoading" placeholder="输入姓名搜索系统用户..." class="w-full" size="large" @change="handleAddInspector" clearable>
                <el-option v-for="u in addResults" :key="Number(u.id)" :label="(u.realName||u.username) + (u.orgUnitName ? ` (${u.orgUnitName})` : '')" :value="Number(u.id)">
                  <div class="flex items-center justify-between"><span class="font-medium">{{ u.realName || u.username }}</span><span class="text-xs text-gray-400">{{ u.orgUnitName || u.username }}</span></div>
                </el-option>
              </el-select>
            </div>
            <div class="w-28">
              <el-select v-model="addRole" size="large">
                <el-option v-for="(v,k) in InspectorRoleConfig" :key="k" :label="v.label" :value="k" />
              </el-select>
            </div>
          </div>
        </div>

        <!-- 工作量 -->
        <div v-if="inspectorStats.length > 0 && !isDraft" class="grid grid-cols-4 gap-3 mb-5">
          <div v-for="stat in inspectorStats" :key="stat.name" class="bg-white border border-gray-200 rounded-xl px-4 py-3 shadow-sm">
            <div class="font-medium text-gray-800">{{ stat.name }}</div>
            <div class="flex gap-4 mt-2 text-xs">
              <div><span class="text-gray-400">分配</span> <span class="font-bold text-gray-700">{{ stat.assigned }}</span></div>
              <div><span class="text-gray-400">完成</span> <span class="font-bold text-green-600">{{ stat.completed }}</span></div>
              <div><span class="text-gray-400">目标</span> <span class="font-bold text-blue-600">{{ stat.targets }}</span></div>
            </div>
          </div>
        </div>

        <!-- 列表 -->
        <div v-if="inspectors.length === 0" class="py-8 text-center text-gray-400 text-sm">暂无检查员</div>
        <div v-else class="grid grid-cols-2 gap-3">
          <div v-for="insp in inspectors" :key="insp.id" class="flex items-center gap-3 p-3 bg-white border border-gray-200 rounded-xl shadow-sm">
            <div class="w-10 h-10 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 text-white flex items-center justify-center text-sm font-bold shadow">{{ (insp.userName || '?')[0] }}</div>
            <div class="flex-1 min-w-0">
              <div class="font-medium text-gray-800">{{ insp.userName }}</div>
              <div class="text-xs text-gray-400">{{ InspectorRoleConfig[insp.role as InspectorRole]?.label }}</div>
            </div>
            <el-tag :type="insp.isActive ? 'success' : 'info'" size="small" round effect="plain">{{ insp.isActive ? '启用' : '禁用' }}</el-tag>
            <el-button link type="danger" size="small" @click="handleRemoveInspector(insp)"><Trash2 class="w-4 h-4" /></el-button>
          </div>
        </div>
      </el-tab-pane>

      <!-- ===== 评级维度 ===== -->
      <el-tab-pane name="dimensions">
        <template #label><div class="flex items-center gap-1.5 px-1"><Star class="w-4 h-4" /><span>评级维度</span></div></template>
        <RatingDimensionConfig :project-id="projectId" />
      </el-tab-pane>

      <!-- ===== 审核 ===== -->
      <el-tab-pane name="review">
        <template #label>
          <div class="flex items-center gap-1.5 px-1">
            <ClipboardCheck class="w-4 h-4" />
            <span>审核</span>
            <el-badge v-if="pendingReviewCount > 0" :value="pendingReviewCount" type="danger" class="ml-0.5" />
          </div>
        </template>

        <div v-if="pendingReviewTasks.length === 0" class="py-16 text-center text-gray-400">
          <ClipboardCheck class="w-12 h-12 mx-auto mb-3 text-gray-300" />
          <div class="text-sm">暂无待审核任务</div>
        </div>

        <div v-else class="space-y-3">
          <div v-for="task in pendingReviewTasks" :key="task.id"
               class="flex items-center justify-between p-4 bg-white border border-gray-200 rounded-xl shadow-sm">
            <div class="flex-1 min-w-0">
              <div class="font-medium text-gray-800">{{ task.taskCode }}</div>
              <div class="text-xs text-gray-400 mt-1">
                检查员: {{ task.inspectorName || '-' }} · 提交于 {{ task.updatedAt?.substring(0, 16) || '-' }}
              </div>
            </div>
            <div class="flex items-center gap-2">
              <el-tag type="warning" size="small" round>待审核</el-tag>
              <el-button type="success" size="small" @click="handleApproveTask(task)">
                <Check class="w-3.5 h-3.5 mr-1" />通过
              </el-button>
              <el-button type="danger" size="small" plain @click="handleRejectTask(task)">
                <X class="w-3.5 h-3.5 mr-1" />驳回
              </el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- ===== 检查结果 ===== -->
      <el-tab-pane name="results">
        <template #label><div class="flex items-center gap-1.5 px-1"><BarChart3 class="w-4 h-4" /><span>检查结果</span></div></template>

        <div v-if="!resultStats" class="py-16 text-center text-gray-400">
          <BarChart3 class="w-12 h-12 mx-auto mb-3 text-gray-300" />
          <div class="text-sm">暂无检查结果数据</div>
          <div class="text-xs text-gray-300 mt-1">检查完成后将在此显示统计</div>
        </div>

        <div v-else class="space-y-5">
          <!-- 概览指标 -->
          <div class="grid grid-cols-5 gap-3">
            <div class="bg-white border border-gray-200 rounded-xl px-4 py-3 text-center shadow-sm">
              <div class="text-2xl font-bold text-gray-800">{{ resultStats.total }}</div>
              <div class="text-xs text-gray-400 mt-1">已完成检查</div>
            </div>
            <div class="bg-white border border-gray-200 rounded-xl px-4 py-3 text-center shadow-sm">
              <div class="text-2xl font-bold text-blue-600">{{ resultStats.avg }}</div>
              <div class="text-xs text-gray-400 mt-1">平均分</div>
            </div>
            <div class="bg-white border border-gray-200 rounded-xl px-4 py-3 text-center shadow-sm">
              <div class="text-2xl font-bold text-green-600">{{ resultStats.passed }}</div>
              <div class="text-xs text-gray-400 mt-1">通过</div>
            </div>
            <div class="bg-white border border-gray-200 rounded-xl px-4 py-3 text-center shadow-sm">
              <div class="text-2xl font-bold text-red-500">{{ resultStats.failed }}</div>
              <div class="text-xs text-gray-400 mt-1">未通过</div>
            </div>
            <div class="bg-white border border-gray-200 rounded-xl px-4 py-3 text-center shadow-sm">
              <div class="text-2xl font-bold text-gray-800">{{ resultStats.max }}<span class="text-sm font-normal text-gray-400">/{{ resultStats.min }}</span></div>
              <div class="text-xs text-gray-400 mt-1">最高/最低</div>
            </div>
          </div>

          <!-- 评级分布 -->
          <div v-if="resultStats.grades.length > 0" class="bg-white border border-gray-200 rounded-xl p-4 shadow-sm">
            <div class="text-sm font-medium text-gray-700 mb-3">评级分布</div>
            <div class="flex items-end gap-4">
              <div v-for="[grade, count] in resultStats.grades" :key="grade" class="text-center">
                <div class="w-12 bg-blue-100 rounded-t-lg mx-auto flex items-end justify-center" :style="{ height: Math.max(20, count / resultStats.total * 120) + 'px' }">
                  <span class="text-xs font-bold text-blue-700 pb-1">{{ count }}</span>
                </div>
                <div class="text-xs text-gray-600 mt-1 font-medium">{{ grade }}</div>
              </div>
            </div>
          </div>

          <!-- 目标得分明细 -->
          <div class="bg-white border border-gray-200 rounded-xl shadow-sm overflow-hidden">
            <div class="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
              <span class="text-sm font-medium text-gray-700">检查目标得分明细</span>
              <span class="text-xs text-gray-400">按分数从低到高排序</span>
            </div>
            <el-table :data="targetScores" max-height="400" size="small">
              <el-table-column prop="targetName" label="检查目标" min-width="180" />
              <el-table-column label="分数" width="100" align="center">
                <template #default="{ row }">
                  <span class="font-bold" :class="(row.finalScore ?? 0) >= 60 ? 'text-green-600' : 'text-red-500'">{{ row.finalScore?.toFixed(1) ?? '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="评级" width="80" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.grade" size="small" round effect="plain">{{ row.grade }}</el-tag>
                  <span v-else class="text-gray-300">-</span>
                </template>
              </el-table-column>
              <el-table-column label="结果" width="80" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.passed === true" type="success" size="small" round effect="dark">通过</el-tag>
                  <el-tag v-else-if="row.passed === false" type="danger" size="small" round effect="dark">未通过</el-tag>
                  <span v-else class="text-gray-300">-</span>
                </template>
              </el-table-column>
              <el-table-column prop="completedAt" label="完成时间" width="170" />
            </el-table>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.detail-tabs :deep(.el-tabs__header) {
  background: #f9fafb;
  border-radius: 8px 8px 0 0;
}
.detail-tabs :deep(.el-tabs__item) {
  font-weight: 500;
}

/* Config card styles */
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
</style>
