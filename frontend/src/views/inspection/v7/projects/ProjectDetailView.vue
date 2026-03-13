<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Play, Pause, CheckCircle, Send, Users, Plus, Trash2 } from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/insp/inspExecutionStore'
import {
  ProjectStatusConfig, type ProjectStatus,
  InspectorRoleConfig, type InspectorRole,
} from '@/types/insp/enums'
import type { InspProject, ProjectInspector, AddInspectorRequest } from '@/types/insp/project'
import { TargetTypeConfig, type TargetType } from '@/types/insp/enums'
import { inspProjectApi } from '@/api/insp/project'
import ProjectScoreTree from './components/ProjectScoreTree.vue'

const route = useRoute()
const router = useRouter()
const store = useInspExecutionStore()

const projectId = Number(route.params.id)

// State
const loading = ref(false)
const project = ref<InspProject | null>(null)
const inspectors = ref<ProjectInspector[]>([])
const childProjects = ref<InspProject[]>([])
const activeTab = ref('info')
const hasChildren = ref(false)

// Inspector dialog
const inspectorDialogVisible = ref(false)
const inspectorForm = ref<AddInspectorRequest>({
  userId: 0,
  userName: '',
  role: 'INSPECTOR',
})

const inspectorRoleOptions = computed(() =>
  Object.entries(InspectorRoleConfig).map(([key, val]) => ({
    value: key,
    label: val.label,
  }))
)

// Load
async function loadProject() {
  loading.value = true
  try {
    project.value = await store.loadProject(projectId)
    inspectors.value = await store.loadInspectors(projectId)
    childProjects.value = await inspProjectApi.getChildProjects(projectId)
    hasChildren.value = childProjects.value.length > 0
  } catch (e: any) {
    ElMessage.error(e.message || '加载项目失败')
  } finally {
    loading.value = false
  }
}

// Lifecycle
async function handlePublish() {
  if (!project.value) return
  try {
    await ElMessageBox.confirm('确定发布该项目？发布后将开始生成检查任务。', '确认发布', { type: 'warning' })
    const templateVersionId = project.value.templateVersionId
    if (!templateVersionId) {
      ElMessage.warning('请先选择模板版本')
      return
    }
    await store.publishProject(projectId, { templateVersionId })
    ElMessage.success('项目已发布')
    loadProject()
  } catch { /* cancelled */ }
}

async function handlePause() {
  try {
    await store.pauseProject(projectId)
    ElMessage.success('已暂停')
    loadProject()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleResume() {
  try {
    await store.resumeProject(projectId)
    ElMessage.success('已恢复')
    loadProject()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleComplete() {
  try {
    await ElMessageBox.confirm('确定完结？', '确认', { type: 'warning' })
    await store.completeProject(projectId)
    ElMessage.success('已完结')
    loadProject()
  } catch { /* cancelled */ }
}

// Inspectors
function openAddInspector() {
  inspectorForm.value = { userId: 0, userName: '', role: 'INSPECTOR' }
  inspectorDialogVisible.value = true
}

async function submitAddInspector() {
  try {
    await store.addInspector(projectId, inspectorForm.value)
    ElMessage.success('添加成功')
    inspectorDialogVisible.value = false
    inspectors.value = await store.loadInspectors(projectId)
  } catch (e: any) {
    ElMessage.error(e.message || '添加失败')
  }
}

async function handleRemoveInspector(inspector: ProjectInspector) {
  try {
    await ElMessageBox.confirm(`确定移除「${inspector.userName}」？`, '确认', { type: 'warning' })
    await store.removeInspector(projectId, inspector.id)
    ElMessage.success('已移除')
    inspectors.value = await store.loadInspectors(projectId)
  } catch { /* cancelled */ }
}

function goBack() {
  router.push('/inspection/v7/projects')
}

function goTasks() {
  router.push(`/inspection/v7/tasks?projectId=${projectId}`)
}

onMounted(() => {
  loadProject()
})
</script>

<template>
  <div class="p-5 space-y-4" v-loading="loading">
    <!-- 标题栏 -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <el-button link @click="goBack"><ArrowLeft class="w-4 h-4" /></el-button>
        <h2 class="text-lg font-semibold">{{ project?.projectName || '加载中...' }}</h2>
        <el-tag v-if="project" :type="(ProjectStatusConfig[project.status as ProjectStatus]?.type as any)" size="small">
          {{ ProjectStatusConfig[project.status as ProjectStatus]?.label }}
        </el-tag>
      </div>
      <div class="flex items-center gap-2" v-if="project">
        <el-button v-if="project.status === 'DRAFT'" type="primary" @click="handlePublish">
          <Send class="w-4 h-4 mr-1" />发布
        </el-button>
        <el-button v-if="project.status === 'PUBLISHED'" type="warning" @click="handlePause">
          <Pause class="w-4 h-4 mr-1" />暂停
        </el-button>
        <el-button v-if="project.status === 'PAUSED'" type="success" @click="handleResume">
          <Play class="w-4 h-4 mr-1" />恢复
        </el-button>
        <el-button v-if="project.status === 'PUBLISHED' || project.status === 'PAUSED'" @click="handleComplete">
          <CheckCircle class="w-4 h-4 mr-1" />完结
        </el-button>
        <el-button @click="goTasks">查看任务</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <!-- 基本信息 -->
      <el-tab-pane label="基本信息" name="info">
        <div class="grid grid-cols-2 gap-4 max-w-2xl" v-if="project">
          <div><span class="text-gray-500 text-sm">项目编码</span><div>{{ project.projectCode }}</div></div>
          <div><span class="text-gray-500 text-sm">项目名称</span><div>{{ project.projectName }}</div></div>
          <div><span class="text-gray-500 text-sm">开始日期</span><div>{{ project.startDate }}</div></div>
          <div><span class="text-gray-500 text-sm">结束日期</span><div>{{ project.endDate || '-' }}</div></div>
          <div><span class="text-gray-500 text-sm">审核必需</span><div>{{ project.reviewRequired ? '是' : '否' }}</div></div>
          <div><span class="text-gray-500 text-sm">自动发布</span><div>{{ project.autoPublish ? '是' : '否' }}</div></div>
          <div><span class="text-gray-500 text-sm">创建时间</span><div>{{ project.createdAt }}</div></div>
        </div>
      </el-tab-pane>

      <!-- 子项目 (仅组合项目显示) -->
      <el-tab-pane v-if="hasChildren" label="子项目" name="children">
        <el-table :data="childProjects" stripe>
          <el-table-column prop="projectCode" label="编码" width="180" />
          <el-table-column prop="projectName" label="名称" />
          <el-table-column label="目标类型" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.targetType" size="small" type="info">
                {{ TargetTypeConfig[row.targetType as TargetType]?.label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="(ProjectStatusConfig[row.status as ProjectStatus]?.type as any)" size="small">
                {{ ProjectStatusConfig[row.status as ProjectStatus]?.label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="router.push(`/inspection/v7/projects/${row.id}`)">
                查看
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 分数汇总 (仅组合项目显示) -->
      <el-tab-pane v-if="hasChildren" label="分数汇总" name="scores">
        <ProjectScoreTree :project-id="projectId" />
      </el-tab-pane>

      <!-- 检查员 -->
      <el-tab-pane label="检查员" name="inspectors">
        <div class="mb-3">
          <el-button type="primary" size="small" @click="openAddInspector">
            <Plus class="w-3.5 h-3.5 mr-1" />添加检查员
          </el-button>
        </div>
        <el-table :data="inspectors" stripe>
          <el-table-column prop="userName" label="姓名" width="150" />
          <el-table-column label="角色" width="120">
            <template #default="{ row }">
              {{ InspectorRoleConfig[row.role as InspectorRole]?.label }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.isActive ? 'success' : 'info'" size="small">
                {{ row.isActive ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="加入时间" />
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="danger" size="small" @click="handleRemoveInspector(row)">
                <Trash2 class="w-3.5 h-3.5" />
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 添加检查员对话框 -->
    <el-dialog v-model="inspectorDialogVisible" title="添加检查员" width="400px">
      <el-form :model="inspectorForm" label-width="80px">
        <el-form-item label="用户ID">
          <el-input-number v-model="inspectorForm.userId" :min="1" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="inspectorForm.userName" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="inspectorForm.role">
            <el-option
              v-for="opt in inspectorRoleOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inspectorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAddInspector">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
