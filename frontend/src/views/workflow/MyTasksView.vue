<template>
  <div class="my-tasks">
    <div class="header">
      <h2>我的待办</h2>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="tasks" v-loading="loading">
      <el-table-column prop="name" label="任务名称" />
      <el-table-column prop="processDefinitionId" label="所属流程" width="240" />
      <el-table-column prop="assignee" label="处理人" width="120" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button v-if="!row.assignee" size="small" type="primary" @click="claim(row)">认领</el-button>
          <el-button size="small" type="success" @click="openComplete(row)">完成</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="completeDialogVisible" title="完成任务" width="500px">
      <el-form>
        <el-form-item label="备注">
          <el-input v-model="completeRemark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmComplete">确认完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { workflowApi, type WorkflowTask } from '@/api/workflow'

const tasks = ref<WorkflowTask[]>([])
const loading = ref(false)
const completeDialogVisible = ref(false)
const completeRemark = ref('')
const currentTask = ref<WorkflowTask | null>(null)

async function load() {
  loading.value = true
  try {
    const res = await workflowApi.myTasks()
    tasks.value = res || []
  } catch (e) {
    ElMessage.error(`加载失败: ${(e as Error).message}`)
  } finally {
    loading.value = false
  }
}

async function claim(row: WorkflowTask) {
  try {
    await workflowApi.claimTask(row.id)
    ElMessage.success('认领成功')
    load()
  } catch (e) {
    ElMessage.error(`认领失败: ${(e as Error).message}`)
  }
}

function openComplete(row: WorkflowTask) {
  currentTask.value = row
  completeRemark.value = ''
  completeDialogVisible.value = true
}

async function confirmComplete() {
  if (!currentTask.value) return
  try {
    await workflowApi.completeTask(currentTask.value.id, { remark: completeRemark.value })
    ElMessage.success('完成成功')
    completeDialogVisible.value = false
    load()
  } catch (e) {
    ElMessage.error(`完成失败: ${(e as Error).message}`)
  }
}

onMounted(load)
</script>

<style scoped>
.my-tasks { padding: 24px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header h2 { margin: 0; }
</style>
