<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, ClipboardList, BarChart3, FileText } from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/insp/inspExecutionStore'
import { useInspTemplateStore } from '@/stores/insp/inspTemplateStore'
import { useInspCorrectiveStore } from '@/stores/insp/inspCorrectiveStore'

const router = useRouter()
const executionStore = useInspExecutionStore()
const templateStore = useInspTemplateStore()
const correctiveStore = useInspCorrectiveStore()

const loading = ref(false)

const stats = reactive({
  templateCount: 0,
  activeProjects: 0,
  todayTasks: 0,
  pendingCases: 0,
})

interface Activity {
  id: number
  description: string
  time: string
}

const recentActivities = ref<Activity[]>([])

async function loadDashboardData() {
  loading.value = true
  try {
    // Load templates count
    const tplResult = await templateStore.loadTemplates({ page: 1, size: 1 })
    stats.templateCount = tplResult.total

    // Load active projects
    const projects = await executionStore.loadProjects('PUBLISHED' as any)
    stats.activeProjects = projects?.length ?? 0

    // Load all tasks and count
    await executionStore.loadTasks()
    stats.todayTasks = executionStore.tasks?.length ?? 0

    // Load pending corrective cases
    await correctiveStore.fetchCases({ status: 'OPEN' as any })
    stats.pendingCases = correctiveStore.cases?.length ?? 0
  } catch (e: any) {
    ElMessage.error(e.message || '加载仪表盘数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboardData()
})
</script>

<template>
  <div class="p-5 space-y-4" v-loading="loading">
    <h2 class="text-lg font-semibold">检查平台</h2>

    <!-- Stats Cards -->
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="never">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm text-gray-500">模板数</div>
              <div class="text-2xl font-bold mt-1">{{ stats.templateCount }}</div>
            </div>
            <FileText class="w-8 h-8 text-blue-400" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm text-gray-500">活跃项目</div>
              <div class="text-2xl font-bold mt-1">{{ stats.activeProjects }}</div>
            </div>
            <Plus class="w-8 h-8 text-green-400" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm text-gray-500">今日任务</div>
              <div class="text-2xl font-bold mt-1">{{ stats.todayTasks }}</div>
            </div>
            <ClipboardList class="w-8 h-8 text-orange-400" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm text-gray-500">待整改</div>
              <div class="text-2xl font-bold mt-1">{{ stats.pendingCases }}</div>
            </div>
            <BarChart3 class="w-8 h-8 text-red-400" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Quick Links + Recent Activity -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span class="font-medium">快捷入口</span>
          </template>
          <div class="flex flex-wrap gap-3">
            <el-button @click="router.push('/inspection/v7/projects')">检查项目</el-button>
            <el-button @click="router.push('/inspection/v7/templates/create')">创建模板</el-button>
            <el-button @click="router.push('/inspection/v7/tasks')">检查任务</el-button>
            <el-button @click="router.push('/inspection/v7/analytics')">分析报表</el-button>
            <el-button @click="router.push('/inspection/v7/corrective')">整改管理</el-button>
            <el-button @click="router.push('/inspection/v7/scoring')">评分配置</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span class="font-medium">最近活动</span>
          </template>
          <div v-if="recentActivities.length === 0" class="py-8 text-center text-gray-400 text-sm">
            暂无活动记录
          </div>
          <div v-else class="space-y-2">
            <div
              v-for="activity in recentActivities"
              :key="activity.id"
              class="flex items-center justify-between py-2 border-b border-gray-100 last:border-0"
            >
              <span class="text-sm text-gray-700">{{ activity.description }}</span>
              <span class="text-xs text-gray-400">{{ activity.time }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
