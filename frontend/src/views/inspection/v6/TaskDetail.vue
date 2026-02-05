<template>
  <div class="v6-task-detail">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
          <span>任务详情</span>
          <div class="header-actions">
            <el-button type="primary" @click="executeTask" v-if="task?.status === 'IN_PROGRESS'">执行检查</el-button>
            <el-button type="success" @click="submitTask" v-if="task?.status === 'IN_PROGRESS'">提交</el-button>
            <el-button type="primary" @click="reviewTask" v-if="task?.status === 'SUBMITTED'">审核</el-button>
            <el-button type="success" @click="publishTask" v-if="task?.status === 'REVIEWED'">发布</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border v-loading="loading">
        <el-descriptions-item label="任务编号">{{ task?.taskCode }}</el-descriptions-item>
        <el-descriptions-item label="所属项目">{{ task?.projectName }}</el-descriptions-item>
        <el-descriptions-item label="任务日期">{{ task?.taskDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(task?.status)">{{ getStatusLabel(task?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="检查员">{{ task?.inspectorName || '未分配' }}</el-descriptions-item>
        <el-descriptions-item label="进度">{{ task?.completedCount || 0 }} / {{ task?.targetCount || 0 }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">检查目标</el-divider>

      <el-table :data="targets" style="width: 100%">
        <el-table-column prop="targetName" label="目标名称" min-width="150" />
        <el-table-column prop="targetType" label="类型" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getTargetStatusType(row.status)">{{ getTargetStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="baseScore" label="基础分" width="80" />
        <el-table-column prop="deductionTotal" label="扣分" width="80" />
        <el-table-column prop="bonusTotal" label="加分" width="80" />
        <el-table-column prop="finalScore" label="最终分" width="80" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="lockTarget(row)" v-if="row.status === 'PENDING'">锁定</el-button>
            <el-button link type="success" @click="completeTarget(row)" v-if="row.status === 'LOCKED'">完成</el-button>
            <el-button link type="warning" @click="skipTarget(row)" v-if="row.status === 'PENDING' || row.status === 'LOCKED'">跳过</el-button>
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
import { v6TaskApi } from '@/api/v6Inspection'
import type { InspectionTask, InspectionTarget } from '@/types/v6Inspection'

const router = useRouter()
const route = useRoute()
const taskId = Number(route.params.id)

const loading = ref(false)
const task = ref<InspectionTask | null>(null)
const targets = ref<InspectionTarget[]>([])

const getStatusType = (status?: string) => {
  const types: Record<string, string> = { PENDING: 'info', IN_PROGRESS: 'primary', SUBMITTED: 'warning', REVIEWED: 'success', PUBLISHED: 'success' }
  return types[status || ''] || 'info'
}

const getStatusLabel = (status?: string) => {
  const labels: Record<string, string> = { PENDING: '待开始', IN_PROGRESS: '进行中', SUBMITTED: '已提交', REVIEWED: '已审核', PUBLISHED: '已发布' }
  return labels[status || ''] || status
}

const getTargetStatusType = (status: string) => {
  const types: Record<string, string> = { PENDING: 'info', LOCKED: 'warning', COMPLETED: 'success', SKIPPED: 'info' }
  return types[status] || 'info'
}

const getTargetStatusLabel = (status: string) => {
  const labels: Record<string, string> = { PENDING: '待检查', LOCKED: '检查中', COMPLETED: '已完成', SKIPPED: '已跳过' }
  return labels[status] || status
}

const goBack = () => router.push('/inspection/v6/tasks')

const loadData = async () => {
  loading.value = true
  try {
    task.value = await v6TaskApi.getById(taskId)
    targets.value = await v6TaskApi.getTargets(taskId)
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const executeTask = () => router.push(`/inspection/v6/tasks/${taskId}/execute`)

const submitTask = async () => {
  try {
    await ElMessageBox.confirm('确定要提交该任务吗？', '提示')
    await v6TaskApi.submit(taskId)
    ElMessage.success('提交成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error('提交失败')
  }
}

const reviewTask = async () => {
  try {
    await v6TaskApi.review(taskId)
    ElMessage.success('审核通过')
    loadData()
  } catch (error) {
    ElMessage.error('审核失败')
  }
}

const publishTask = async () => {
  try {
    await v6TaskApi.publish(taskId)
    ElMessage.success('发布成功')
    loadData()
  } catch (error) {
    ElMessage.error('发布失败')
  }
}

const lockTarget = async (row: InspectionTarget) => {
  try {
    await v6TaskApi.lockTarget(row.id)
    ElMessage.success('锁定成功')
    loadData()
  } catch (error) {
    ElMessage.error('锁定失败')
  }
}

const completeTarget = async (row: InspectionTarget) => {
  try {
    await v6TaskApi.completeTarget(row.id)
    ElMessage.success('完成')
    loadData()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const skipTarget = async (row: InspectionTarget) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入跳过原因', '跳过目标')
    await v6TaskApi.skipTarget(row.id, value || '')
    ElMessage.success('已跳过')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error('操作失败')
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.v6-task-detail { padding: 20px; }
.card-header { display: flex; align-items: center; gap: 16px; }
.header-actions { margin-left: auto; display: flex; gap: 8px; }
</style>
