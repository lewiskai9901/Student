<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Play, Pause, CheckCircle, Archive, Trash2, Eye, Users } from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/insp/inspExecutionStore'
import { ProjectStatusConfig, type ProjectStatus } from '@/types/insp/enums'
import type { InspProject } from '@/types/insp/project'
import InspEmptyState from '../shared/InspEmptyState.vue'

const router = useRouter()
const store = useInspExecutionStore()

// State
const loading = ref(false)
const projects = ref<InspProject[]>([])

const queryParams = reactive({
  status: undefined as ProjectStatus | undefined,
})

// Computed
const statusOptions = computed(() =>
  Object.entries(ProjectStatusConfig).map(([key, val]) => ({
    value: key,
    label: val.label,
  }))
)

// Actions
async function loadData() {
  loading.value = true
  try {
    projects.value = await store.loadProjects(queryParams.status)
  } catch (e: any) {
    ElMessage.error(e.message || '加载项目列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

function resetQuery() {
  queryParams.status = undefined
  loadData()
}

function goCreate() {
  router.push('/inspection/v7/projects/create')
}

function goDetail(project: InspProject) {
  router.push(`/inspection/v7/projects/${project.id}`)
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
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleResume(project: InspProject) {
  try {
    await store.resumeProject(project.id)
    ElMessage.success('已恢复')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
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
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
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
      <h2 class="text-lg font-semibold">检查项目</h2>
      <el-button type="primary" @click="goCreate">
        <Plus class="w-4 h-4 mr-1" />新建项目
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="flex items-center gap-3">
      <el-select
        v-model="queryParams.status"
        placeholder="项目状态"
        clearable
        class="w-36"
        @change="handleSearch"
      >
        <el-option
          v-for="opt in statusOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <!-- 项目列表 -->
    <el-table :data="projects" v-loading="loading" stripe>
      <el-table-column prop="projectCode" label="项目编码" width="180" />
      <el-table-column prop="projectName" label="项目名称" min-width="200" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag
            :type="(ProjectStatusConfig[row.status as ProjectStatus]?.type as any)"
            size="small"
          >
            {{ ProjectStatusConfig[row.status as ProjectStatus]?.label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startDate" label="开始日期" width="120" />
      <el-table-column prop="endDate" label="结束日期" width="120" />
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <div class="flex items-center gap-1">
            <el-button link type="primary" size="small" @click="goDetail(row)">
              <Eye class="w-3.5 h-3.5" />
            </el-button>
            <el-button
              v-if="row.status === 'PUBLISHED'"
              link type="warning" size="small"
              @click="handlePause(row)"
            >
              <Pause class="w-3.5 h-3.5" />
            </el-button>
            <el-button
              v-if="row.status === 'PAUSED'"
              link type="success" size="small"
              @click="handleResume(row)"
            >
              <Play class="w-3.5 h-3.5" />
            </el-button>
            <el-button
              v-if="row.status === 'PUBLISHED' || row.status === 'PAUSED'"
              link type="primary" size="small"
              @click="handleComplete(row)"
            >
              <CheckCircle class="w-3.5 h-3.5" />
            </el-button>
            <el-button
              v-if="row.status === 'COMPLETED'"
              link size="small"
              @click="handleArchive(row)"
            >
              <Archive class="w-3.5 h-3.5" />
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              link type="danger" size="small"
              @click="handleDelete(row)"
            >
              <Trash2 class="w-3.5 h-3.5" />
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <InspEmptyState v-if="!loading && projects.length === 0" message="暂无检查项目" />
  </div>
</template>
