<template>
  <div class="v6-project-detail">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
          <span>项目详情</span>
          <div class="header-actions">
            <el-button type="primary" @click="handleConfig" v-if="project?.status === 'DRAFT'">配置</el-button>
            <el-button type="success" @click="handlePublish" v-if="project?.status === 'DRAFT'">发布</el-button>
            <el-button type="warning" @click="handlePause" v-if="project?.status === 'ACTIVE'">暂停</el-button>
            <el-button type="success" @click="handleResume" v-if="project?.status === 'PAUSED'">恢复</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border v-loading="loading">
        <el-descriptions-item label="项目编号">{{ project?.projectCode }}</el-descriptions-item>
        <el-descriptions-item label="项目名称">{{ project?.projectName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(project?.status)">{{ getStatusLabel(project?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="周期类型">{{ getCycleTypeLabel(project?.cycleType) }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ project?.startDate }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ project?.endDate }}</el-descriptions-item>
        <el-descriptions-item label="基础分">{{ project?.baseScore }}</el-descriptions-item>
        <el-descriptions-item label="范围类型">{{ project?.scopeType }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ project?.description || '无' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">检查任务</el-divider>

      <el-table :data="tasks" style="width: 100%">
        <el-table-column prop="taskCode" label="任务编号" width="150" />
        <el-table-column prop="taskDate" label="任务日期" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getTaskStatusType(row.status)">{{ getTaskStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="inspectorName" label="检查员" width="120" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewTask(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { v6ProjectApi, v6TaskApi } from '@/api/v6Inspection'
import type { InspectionProject, InspectionTask } from '@/types/v6Inspection'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const project = ref<InspectionProject | null>(null)
const tasks = ref<InspectionTask[]>([])

const projectId = route.params.id as string

const getStatusType = (status?: string) => {
  const types: Record<string, string> = { DRAFT: 'info', ACTIVE: 'success', PAUSED: 'warning', COMPLETED: 'primary', ARCHIVED: 'info' }
  return types[status || ''] || 'info'
}

const getStatusLabel = (status?: string) => {
  const labels: Record<string, string> = { DRAFT: '草稿', ACTIVE: '进行中', PAUSED: '已暂停', COMPLETED: '已完成', ARCHIVED: '已归档' }
  return labels[status || ''] || status
}

const getCycleTypeLabel = (cycleType?: string) => {
  const labels: Record<string, string> = { DAILY: '每日', WEEKLY: '每周', MONTHLY: '每月', CUSTOM: '自定义' }
  return labels[cycleType || ''] || cycleType
}

const getTaskStatusType = (status: string) => {
  const types: Record<string, string> = { PENDING: 'info', IN_PROGRESS: 'primary', SUBMITTED: 'warning', REVIEWED: 'success', PUBLISHED: 'success', CANCELLED: 'info' }
  return types[status] || 'info'
}

const getTaskStatusLabel = (status: string) => {
  const labels: Record<string, string> = { PENDING: '待开始', IN_PROGRESS: '进行中', SUBMITTED: '已提交', REVIEWED: '已审核', PUBLISHED: '已发布', CANCELLED: '已取消' }
  return labels[status] || status
}

const goBack = () => router.push('/inspection/v6/projects')

const handleConfig = () => router.push(`/inspection/v6/projects/${projectId}/config`)

const handlePublish = async () => {
  try {
    await ElMessageBox.confirm('确定要发布该项目吗？', '提示')
    await v6ProjectApi.publish(projectId)
    ElMessage.success('发布成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error('发布失败')
  }
}

const handlePause = async () => {
  try {
    await ElMessageBox.confirm('确定要暂停该项目吗？', '提示')
    await v6ProjectApi.pause(projectId)
    ElMessage.success('暂停成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error('暂停失败')
  }
}

const handleResume = async () => {
  try {
    await ElMessageBox.confirm('确定要恢复该项目吗？', '提示')
    await v6ProjectApi.resume(projectId)
    ElMessage.success('恢复成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error('恢复失败')
  }
}

const viewTask = (row: InspectionTask) => router.push(`/inspection/v6/tasks/${row.id}`)

const loadData = async () => {
  loading.value = true
  try {
    project.value = await v6ProjectApi.getById(projectId)
    const res = await v6TaskApi.list({ projectId, page: 1, size: 20 })
    tasks.value = res.records || []
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.v6-project-detail { padding: 20px; }
.card-header { display: flex; align-items: center; gap: 16px; }
.header-actions { margin-left: auto; display: flex; gap: 8px; }
</style>
