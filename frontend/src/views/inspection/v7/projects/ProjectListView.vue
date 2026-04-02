<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/insp/inspExecutionStore'
import { inspProjectApi } from '@/api/insp/project'
import { getTasks } from '@/api/insp/task'
import {
  ProjectStatusConfig, type ProjectStatus,
} from '@/types/insp/enums'
import type { InspProject, InspTask } from '@/types/insp/project'

const router = useRouter()
const store = useInspExecutionStore()

// State
const loading = ref(false)
const projects = ref<InspProject[]>([])
const taskListMap = ref<Map<number, InspTask[]>>(new Map())
const inspectorCountMap = ref<Map<number, number>>(new Map())
const today = new Date().toISOString().slice(0, 10)

const viewMode = ref<'kanban' | 'list'>('kanban')
const searchQuery = ref('')

// Group projects by status for kanban
const kanbanColumns = computed(() => {
  const cols = [
    { key: 'DRAFT', label: '草稿', color: '#9ca3af', cssClass: 'draft', projects: [] as InspProject[] },
    { key: 'PUBLISHED', label: '进行中', color: '#1a6dff', cssClass: 'active', projects: [] as InspProject[] },
    { key: 'COMPLETED', label: '已完结', color: '#10b981', cssClass: 'completed', projects: [] as InspProject[] },
    { key: 'ARCHIVED', label: '已归档', color: '#64748b', cssClass: 'archived', projects: [] as InspProject[] },
  ]
  const q = searchQuery.value.toLowerCase()
  for (const p of projects.value) {
    if (q && !p.projectName.toLowerCase().includes(q)) continue
    const col = cols.find(c => c.key === p.status)
    if (col) col.projects.push(p)
    else if (p.status === 'PAUSED') {
      const pubCol = cols.find(c => c.key === 'PUBLISHED')
      if (pubCol) pubCol.projects.push(p)
    }
  }
  return cols
})

// Filtered projects for list view
const filteredProjects = computed(() => {
  const q = searchQuery.value.toLowerCase()
  if (!q) return projects.value
  return projects.value.filter(p => p.projectName.toLowerCase().includes(q))
})

// Actions
async function loadData() {
  loading.value = true
  try {
    projects.value = await store.loadProjects()

    // Load tasks and inspector counts in parallel
    await Promise.all(projects.value.map(async (p) => {
      try {
        const tasks = await getTasks({ projectId: p.id }).catch(() => [] as InspTask[])
        taskListMap.value.set(p.id, tasks)
        const inspectors = await inspProjectApi.getInspectors(p.id).catch(() => [])
        inspectorCountMap.value.set(p.id, inspectors.length)
      } catch (e: any) { console.warn(`加载项目 ${p.id} 数据失败`, e) }
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

function getProjectTasks(pid: number): InspTask[] {
  return taskListMap.value.get(pid) || []
}

function getTodayTasks(pid: number): InspTask[] {
  return getProjectTasks(pid).filter(t => t.taskDate === today)
}

function getTaskStats(pid: number) {
  const tasks = getProjectTasks(pid)
  return {
    total: tasks.length,
    done: tasks.filter(t => ['SUBMITTED', 'UNDER_REVIEW', 'REVIEWED', 'PUBLISHED'].includes(t.status)).length,
    pending: tasks.filter(t => t.status === 'PENDING').length,
    inProgress: tasks.filter(t => ['CLAIMED', 'IN_PROGRESS'].includes(t.status)).length,
  }
}

function getProgressPercent(pid: number): number {
  const stats = getTaskStats(pid)
  if (stats.total === 0) return 0
  return Math.round(stats.done / stats.total * 100)
}

function getProgressColor(pid: number): string {
  const pct = getProgressPercent(pid)
  if (pct >= 100) return 'green'
  if (pct >= 50) return 'blue'
  if (pct > 0) return 'orange'
  return 'gray'
}

async function handleDelete(project: InspProject) {
  try {
    await ElMessageBox.confirm(`确定删除项目「${project.projectName}」？`, '确认删除', { type: 'warning' })
    await store.removeProject(project.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') { console.error('删除项目失败', e); ElMessage.error('删除项目失败，请重试') }
  }
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
  } catch (e: any) {
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') { console.error('完结项目失败', e); ElMessage.error('完结项目失败，请重试') }
  }
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
  <div class="pm-page">
    <!-- Header -->
    <div class="pm-header">
      <h2 class="pm-title">项目管理</h2>
      <el-button type="primary" @click="goCreate">
        <Plus :size="14" style="margin-right: 4px;" />新建项目
      </el-button>
    </div>

    <!-- Filter bar -->
    <div class="pm-filters">
      <el-input v-model="searchQuery" placeholder="搜索项目名..." size="small" clearable class="pm-search" />
      <div class="pm-view-toggle">
        <button :class="{ active: viewMode === 'kanban' }" @click="viewMode = 'kanban'">看板</button>
        <button :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">列表</button>
      </div>
    </div>

    <!-- Kanban Board -->
    <div v-if="viewMode === 'kanban'" v-loading="loading" class="kanban-container">
      <div class="kanban-board">
        <div v-for="col in kanbanColumns" :key="col.key" class="kanban-column" :class="{ 'col-active': col.key === 'PUBLISHED' }">
          <div class="col-header">
            <div class="col-header-content">
              <span class="col-title">{{ col.label }}</span>
              <span class="col-count" :class="col.cssClass">{{ col.projects.length }}</span>
            </div>
          </div>
          <div class="col-accent" :class="col.cssClass" />
          <div class="col-body">
            <div v-for="p in col.projects" :key="p.id"
              class="card" :class="{ 'card-active': col.key === 'PUBLISHED' }"
              @click="goDetail(p)">
              <!-- Card name -->
              <div class="card-name">
                {{ p.projectName }}
                <span v-if="col.key === 'PUBLISHED' && p.status === 'PUBLISHED'" class="pulse-dot" />
              </div>
              <div class="card-meta">
                <span>{{ p.projectCode }}</span>
                <span v-if="p.startDate">{{ p.startDate }}{{ p.endDate ? ' ~ ' + p.endDate : '' }}</span>
              </div>

              <!-- Active projects: execution stats -->
              <template v-if="col.key === 'PUBLISHED'">
                <div class="card-section">
                  <div class="card-section-title">今日执行</div>
                  <div class="progress-row">
                    <div class="progress-bar">
                      <div class="progress-fill" :class="getProgressColor(p.id)" :style="{ width: getProgressPercent(p.id) + '%' }" />
                    </div>
                    <span class="progress-label"><b>{{ getTaskStats(p.id).done }}</b>/{{ getTaskStats(p.id).total }}</span>
                  </div>
                  <div class="card-stats">
                    <span>检查员 <span class="val">{{ inspectorCountMap.get(p.id) || 0 }}</span> 人</span>
                    <span>任务 <span class="val">{{ getTodayTasks(p.id).length }}</span> 今日</span>
                  </div>
                </div>
                <div v-if="p.status === 'PAUSED'" style="margin-top: 6px;">
                  <span class="card-tag paused">已暂停</span>
                </div>
              </template>

              <!-- Draft cards -->
              <template v-if="col.key === 'DRAFT'">
                <div class="card-meta" style="margin-top: 6px;">
                  <span>创建于 {{ p.createdAt?.slice(5, 10) }}</span>
                </div>
              </template>

              <!-- Completed cards -->
              <template v-if="col.key === 'COMPLETED'">
                <div class="final-stats">
                  <div class="final-stat">
                    <span class="fs-label">总任务</span>
                    <span class="fs-value">{{ getTaskStats(p.id).total }}</span>
                  </div>
                  <div class="final-stat">
                    <span class="fs-label">已完成</span>
                    <span class="fs-value good">{{ getTaskStats(p.id).done }}</span>
                  </div>
                </div>
              </template>

              <!-- Archived cards -->
              <template v-if="col.key === 'ARCHIVED'">
                <div style="margin-top: 8px;">
                  <span class="card-tag">完成 {{ getTaskStats(p.id).done }}/{{ getTaskStats(p.id).total }}</span>
                </div>
              </template>

              <!-- Actions -->
              <div class="card-actions" @click.stop>
                <button v-if="p.status === 'DRAFT'" class="btn btn-sm btn-danger-ghost" @click="handleDelete(p)">删除</button>
                <button v-if="p.status === 'PUBLISHED'" class="btn btn-sm btn-ghost" @click="handlePause(p)">暂停</button>
                <button v-if="p.status === 'PAUSED'" class="btn btn-sm btn-green" @click="handleResume(p)">恢复</button>
                <button v-if="p.status === 'PUBLISHED' || p.status === 'PAUSED'" class="btn btn-sm btn-ghost" @click="handleComplete(p)">完结</button>
                <button v-if="p.status === 'COMPLETED'" class="btn btn-sm btn-ghost" @click="handleArchive(p)">归档</button>
                <button class="btn btn-sm" @click="goDetail(p)">
                  详情
                  <svg width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
                </button>
              </div>
            </div>

            <div v-if="col.projects.length === 0" class="kanban-empty">无项目</div>
          </div>
        </div>
      </div>
    </div>

    <!-- List View -->
    <div v-else v-loading="loading" class="list-view">
      <table class="list-table">
        <thead>
          <tr>
            <th>项目名称</th>
            <th>状态</th>
            <th>日期</th>
            <th>检查员</th>
            <th>完成率</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in filteredProjects" :key="p.id" @click="goDetail(p)">
            <td class="list-name">{{ p.projectName }}</td>
            <td>
              <span class="status-badge"
                :class="{
                  draft: p.status === 'DRAFT',
                  active: p.status === 'PUBLISHED',
                  paused: p.status === 'PAUSED',
                  completed: p.status === 'COMPLETED',
                  archived: p.status === 'ARCHIVED',
                }">
                <span class="sd" />
                {{ ProjectStatusConfig[p.status as ProjectStatus]?.label }}
              </span>
            </td>
            <td class="list-date">{{ p.startDate || '—' }}{{ p.endDate ? ' ~ ' + p.endDate : '' }}</td>
            <td>{{ inspectorCountMap.get(p.id) || 0 }}</td>
            <td>
              <span class="mini-progress">
                <span class="mp-fill" :style="{ width: getProgressPercent(p.id) + '%', background: getProgressPercent(p.id) >= 80 ? 'var(--pm-green)' : getProgressPercent(p.id) >= 50 ? 'var(--pm-blue)' : 'var(--pm-orange)' }" />
              </span>
              <b>{{ getTaskStats(p.id).done }}/{{ getTaskStats(p.id).total }}</b>
            </td>
            <td @click.stop>
              <button v-if="p.status === 'DRAFT'" class="btn btn-sm btn-danger-ghost" @click="handleDelete(p)">删除</button>
              <button v-if="p.status === 'PUBLISHED'" class="btn btn-sm btn-ghost" @click="handlePause(p)">暂停</button>
              <button v-if="p.status === 'PAUSED'" class="btn btn-sm btn-green" @click="handleResume(p)">恢复</button>
              <button v-if="p.status === 'PUBLISHED' || p.status === 'PAUSED'" class="btn btn-sm btn-ghost" @click="handleComplete(p)">完结</button>
              <button v-if="p.status === 'COMPLETED'" class="btn btn-sm btn-ghost" @click="handleArchive(p)">归档</button>
              <button class="btn btn-sm" @click="goDetail(p)">详情</button>
            </td>
          </tr>
          <tr v-if="filteredProjects.length === 0">
            <td colspan="6" class="kanban-empty">暂无检查项目</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
:root {
  --pm-blue: #1a6dff;
  --pm-green: #10b981;
  --pm-orange: #f59e0b;
  --pm-red: #ef4444;
  --pm-gray: #9ca3af;
  --pm-slate: #64748b;
  --pm-border: #e5e7eb;
  --pm-border-light: #f0f0f0;
  --pm-bg: #f0f2f5;
}

.pm-page {
  background: #f0f2f5;
  min-height: 100%;
}

/* ── Header ── */
.pm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px 0;
}
.pm-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.3px;
  color: #1a1a2e;
}

/* ── Filter Bar ── */
.pm-filters {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 24px;
  flex-wrap: wrap;
}
.pm-search {
  width: 220px;
}
.pm-view-toggle {
  display: flex;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
  margin-left: auto;
}
.pm-view-toggle button {
  padding: 5px 14px;
  font-size: 12px;
  border: none;
  background: #fff;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
  font-weight: 500;
}
.pm-view-toggle button + button {
  border-left: 1px solid #e5e7eb;
}
.pm-view-toggle button.active {
  background: #1a6dff;
  color: #fff;
}
.pm-view-toggle button:hover:not(.active) {
  background: #f3f4f6;
}

/* ── Kanban Board ── */
.kanban-container {
  padding: 0 20px 16px;
  overflow-x: auto;
  scrollbar-width: thin;
  scrollbar-color: #d1d5db transparent;
}
.kanban-board {
  display: flex;
  gap: 14px;
  min-height: calc(100vh - 200px);
  padding-bottom: 16px;
}
.kanban-column {
  flex: 0 0 290px;
  min-width: 290px;
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
  border-radius: 12px;
  max-height: calc(100vh - 190px);
  transition: all 0.2s ease;
}
.kanban-column.col-active {
  background: #f5f8ff;
  flex: 0 0 320px;
  min-width: 320px;
}

/* Column header */
.col-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px 8px;
  flex-shrink: 0;
}
.col-header-content {
  display: flex;
  align-items: center;
  gap: 8px;
}
.col-title {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a2e;
}
.col-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
}
.col-count.draft { background: #9ca3af; }
.col-count.active { background: #1a6dff; }
.col-count.completed { background: #10b981; }
.col-count.archived { background: #64748b; }

.col-accent {
  height: 2px;
  margin: 0 14px 4px;
  border-radius: 1px;
}
.col-accent.draft { background: #9ca3af; }
.col-accent.active { background: #1a6dff; }
.col-accent.completed { background: #10b981; }
.col-accent.archived { background: #64748b; }

.col-body {
  flex: 1;
  overflow-y: auto;
  padding: 4px 10px 10px;
  scrollbar-width: thin;
  scrollbar-color: #d1d5db transparent;
}

/* ── Cards ── */
.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}
.card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #d0d5dd;
  transform: translateY(-1px);
}
.card:last-child { margin-bottom: 4px; }
.card-active {
  border-left: 3px solid #1a6dff;
  padding-left: 12px;
}

.card-name {
  font-size: 13.5px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1a6dff;
  position: relative;
  flex-shrink: 0;
}
.pulse-dot::after {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  background: rgba(26, 109, 255, 0.3);
  animation: pulse 2s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.8); opacity: 0; }
}

.card-meta {
  font-size: 11.5px;
  color: #9ca3af;
  display: flex;
  flex-wrap: wrap;
  gap: 4px 0;
  margin-bottom: 2px;
}
.card-meta span + span::before {
  content: '\00B7';
  margin: 0 5px;
  color: #e5e7eb;
}

.card-section {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}
.card-section-title {
  font-size: 11px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 6px;
  letter-spacing: 0.3px;
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.progress-bar {
  flex: 1;
  height: 4px;
  background: #e5e7eb;
  border-radius: 2px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.4s ease;
}
.progress-fill.blue { background: #1a6dff; }
.progress-fill.green { background: #10b981; }
.progress-fill.orange { background: #f59e0b; }
.progress-fill.gray { background: #9ca3af; }

.progress-label {
  font-size: 11px;
  color: #6b7280;
  white-space: nowrap;
  flex-shrink: 0;
}
.progress-label b {
  color: #1a1a2e;
  font-weight: 600;
}

.card-stats {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  font-size: 11px;
  color: #9ca3af;
}
.card-stats span {
  display: flex;
  align-items: center;
  gap: 3px;
}
.card-stats .val {
  font-weight: 600;
  color: #6b7280;
}

.card-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 10.5px;
  font-weight: 500;
  background: #f3f4f6;
  color: #6b7280;
}
.card-tag.paused {
  background: #fffbeb;
  color: #f59e0b;
}

/* Final stats for completed/archived */
.final-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  margin-top: 8px;
}
.final-stat {
  display: flex;
  flex-direction: column;
  padding: 6px 10px;
  border-radius: 6px;
  background: #f3f4f6;
}
.final-stat .fs-label {
  font-size: 10px;
  color: #9ca3af;
}
.final-stat .fs-value {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a2e;
}
.final-stat .fs-value.good {
  color: #10b981;
}

/* Card actions */
.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

/* Buttons (matching mockup style) */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #1a1a2e;
  transition: all 0.2s ease;
  user-select: none;
  white-space: nowrap;
  font-family: inherit;
}
.btn:hover { border-color: #bbb; background: #f3f4f6; }
.btn-sm { padding: 4px 10px; font-size: 11.5px; }
.btn-ghost { border: none; background: transparent; color: #6b7280; }
.btn-ghost:hover { background: #f3f4f6; color: #1a1a2e; }
.btn-danger-ghost { border: none; background: transparent; color: #ef4444; }
.btn-danger-ghost:hover { background: #fef2f2; color: #ef4444; }
.btn-green { background: #10b981; color: #fff; border-color: #10b981; }
.btn-green:hover { background: #0da472; border-color: #0da472; }

.kanban-empty {
  text-align: center;
  padding: 24px 12px;
  font-size: 12px;
  color: #d1d5db;
}

/* ── List View ── */
.list-view {
  padding: 0 24px 16px;
}
.list-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}
.list-table thead {
  background: #f3f4f6;
}
.list-table th {
  padding: 9px 14px;
  text-align: left;
  font-size: 11.5px;
  font-weight: 600;
  color: #6b7280;
  border-bottom: 1px solid #e5e7eb;
  white-space: nowrap;
}
.list-table td {
  padding: 10px 14px;
  font-size: 12.5px;
  color: #1a1a2e;
  border-bottom: 1px solid #f0f0f0;
  vertical-align: middle;
}
.list-table tr:last-child td { border-bottom: none; }
.list-table tbody tr { cursor: pointer; }
.list-table tbody tr:hover td { background: #fafbfc; }
.list-name { font-weight: 600; }
.list-date { color: #9ca3af; font-size: 12px; }

/* Status badge */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}
.status-badge.draft { background: #f3f4f6; color: #9ca3af; }
.status-badge.active { background: #e8f0fe; color: #1a6dff; }
.status-badge.paused { background: #fffbeb; color: #f59e0b; }
.status-badge.completed { background: #ecfdf5; color: #10b981; }
.status-badge.archived { background: #f1f5f9; color: #64748b; }
.status-badge .sd {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

/* Mini progress bar for list view */
.mini-progress {
  width: 60px;
  height: 3px;
  background: #e5e7eb;
  border-radius: 2px;
  display: inline-block;
  vertical-align: middle;
  margin-right: 6px;
  overflow: hidden;
}
.mini-progress .mp-fill {
  height: 100%;
  border-radius: 2px;
}

/* ── Scrollbar (webkit) ── */
::-webkit-scrollbar { width: 5px; height: 5px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 4px; }
::-webkit-scrollbar-thumb:hover { background: #b0b5bd; }

/* ── Responsive ── */
@media (max-width: 1200px) {
  .kanban-column { flex: 0 0 260px; min-width: 260px; }
  .kanban-column.col-active { flex: 0 0 290px; min-width: 290px; }
}
</style>
