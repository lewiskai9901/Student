<template>
  <div class="process-def-list">
    <div class="header">
      <h2>流程定义管理</h2>
      <el-button type="primary" @click="$router.push('/workflow/designer')">设计新流程</el-button>
    </div>
    <el-table :data="definitions" v-loading="loading">
      <el-table-column prop="key" label="流程标识" width="180" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.suspended ? 'warning' : 'success'">
            {{ row.suspended ? '已挂起' : '激活' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button size="small" @click="toggleSuspend(row)">
            {{ row.suspended ? '激活' : '挂起' }}
          </el-button>
          <el-button size="small" type="danger" @click="confirmDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { workflowApi, type ProcessDefinition } from '@/api/workflow'

const definitions = ref<ProcessDefinition[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await workflowApi.listDefinitions()
    definitions.value = res || []
  } catch (e) {
    ElMessage.error(`加载失败: ${(e as Error).message}`)
  } finally {
    loading.value = false
  }
}

async function toggleSuspend(row: ProcessDefinition) {
  try {
    if (row.suspended) await workflowApi.activateDefinition(row.id)
    else await workflowApi.suspendDefinition(row.id)
    ElMessage.success('操作成功')
    load()
  } catch (e) {
    ElMessage.error(`操作失败: ${(e as Error).message}`)
  }
}

async function confirmDelete(row: ProcessDefinition) {
  try {
    await ElMessageBox.confirm(`确认删除部署 ${row.deploymentId}?将级联删除所有流程实例`, '警告', { type: 'warning' })
    await workflowApi.deleteDeployment(row.deploymentId, true)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    // 用户取消或失败
    if ((e as Error).message && (e as Error).message !== 'cancel') {
      // 真错误才提示
    }
  }
}

onMounted(load)
</script>

<style scoped>
.process-def-list { padding: 24px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header h2 { margin: 0; }
</style>
