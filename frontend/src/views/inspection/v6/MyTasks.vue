<template>
  <div class="v6-my-tasks">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的检查任务</span>
          <el-button type="primary" @click="loadAvailable">刷新可领取任务</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="我的任务" name="my">
          <el-table :data="myTasks" v-loading="loading" style="width: 100%">
            <el-table-column prop="taskCode" label="任务编号" width="150" />
            <el-table-column prop="projectName" label="项目" min-width="150" />
            <el-table-column prop="taskDate" label="日期" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="completedCount" label="进度" width="100">
              <template #default="{ row }">
                {{ row.completedCount || 0 }}/{{ row.targetCount || 0 }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button link type="primary" @click="viewTask(row)">查看</el-button>
                <el-button link type="success" @click="startTask(row)" v-if="row.status === 'PENDING'">开始</el-button>
                <el-button link type="success" @click="executeTask(row)" v-if="row.status === 'IN_PROGRESS'">执行</el-button>
                <el-button link type="primary" @click="submitTask(row)" v-if="row.status === 'IN_PROGRESS'">提交</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="可领取任务" name="available">
          <el-table :data="availableTasks" v-loading="loadingAvailable" style="width: 100%">
            <el-table-column prop="taskCode" label="任务编号" width="150" />
            <el-table-column prop="projectName" label="项目" min-width="150" />
            <el-table-column prop="taskDate" label="日期" width="120" />
            <el-table-column prop="targetCount" label="目标数" width="80" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="claimTask(row)">领取</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { v6TaskApi } from '@/api/v6Inspection'
import type { InspectionTask } from '@/types/v6Inspection'

const router = useRouter()
const activeTab = ref('my')
const loading = ref(false)
const loadingAvailable = ref(false)
const myTasks = ref<InspectionTask[]>([])
const availableTasks = ref<InspectionTask[]>([])

const getStatusType = (status: string) => {
  const types: Record<string, string> = { PENDING: 'info', IN_PROGRESS: 'primary', SUBMITTED: 'warning', REVIEWED: 'success', PUBLISHED: 'success' }
  return types[status] || 'info'
}

const getStatusLabel = (status: string) => {
  const labels: Record<string, string> = { PENDING: '待开始', IN_PROGRESS: '进行中', SUBMITTED: '已提交', REVIEWED: '已审核', PUBLISHED: '已发布' }
  return labels[status] || status
}

const loadMyTasks = async () => {
  loading.value = true
  try {
    myTasks.value = await v6TaskApi.getMyTasks()
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const loadAvailable = async () => {
  loadingAvailable.value = true
  try {
    availableTasks.value = await v6TaskApi.getAvailable()
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loadingAvailable.value = false
  }
}

const viewTask = (row: InspectionTask) => router.push(`/inspection/v6/tasks/${row.id}`)
const executeTask = (row: InspectionTask) => router.push(`/inspection/v6/tasks/${row.id}/execute`)

const startTask = async (row: InspectionTask) => {
  try {
    await v6TaskApi.start(row.id)
    ElMessage.success('任务已开始')
    loadMyTasks()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const submitTask = async (row: InspectionTask) => {
  try {
    await v6TaskApi.submit(row.id)
    ElMessage.success('提交成功')
    loadMyTasks()
  } catch (error) {
    ElMessage.error('提交失败')
  }
}

const claimTask = async (row: InspectionTask) => {
  try {
    await v6TaskApi.claim(row.id)
    ElMessage.success('领取成功')
    loadMyTasks()
    loadAvailable()
  } catch (error) {
    ElMessage.error('领取失败')
  }
}

onMounted(() => {
  loadMyTasks()
  loadAvailable()
})
</script>

<style scoped>
.v6-my-tasks { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
