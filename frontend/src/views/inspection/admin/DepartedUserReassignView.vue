<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserMinus, UserCheck } from 'lucide-vue-next'
import { reassignDepartedUser } from '@/api/inspection/correctiveCase'
import { reassignDepartedInspector } from '@/api/inspection/task'

const form = ref({
  userId: undefined as LongId | undefined,
  reason: '',
  scope: 'BOTH' as 'BOTH' | 'CORRECTIVE' | 'TASK',
  fallbackUserId: undefined as LongId | undefined,
  fallbackUserName: '',
})

const submitting = ref(false)
const lastResult = ref<{ correctiveCount?: number; taskCount?: number } | null>(null)

async function handleSubmit() {
  if (!form.value.userId) {
    ElMessage.warning('请填写离职用户 ID')
    return
  }
  if (!form.value.reason?.trim()) {
    ElMessage.warning('请填写重派原因')
    return
  }

  const summary = form.value.fallbackUserId
    ? `将用户 #${form.value.userId} 的所有非终态项目${form.value.scope === 'BOTH' ? '(整改+任务)' : form.value.scope === 'CORRECTIVE' ? '(整改)' : '(任务)'}重派给 #${form.value.fallbackUserId}`
    : `解除用户 #${form.value.userId} 的所有非终态项目${form.value.scope === 'BOTH' ? '(整改+任务)' : form.value.scope === 'CORRECTIVE' ? '(整改)' : '(任务)'}, 等待管理员逐个分配`

  try {
    await ElMessageBox.confirm(summary + ', 确认执行?', '确认重派', { type: 'warning' })
  } catch {
    return
  }

  submitting.value = true
  lastResult.value = null
  try {
    const result: { correctiveCount?: number; taskCount?: number } = {}
    if (form.value.scope === 'BOTH' || form.value.scope === 'CORRECTIVE') {
      result.correctiveCount = await reassignDepartedUser(form.value.userId, {
        reason: form.value.reason,
        fallbackAssigneeId: form.value.fallbackUserId,
        fallbackAssigneeName: form.value.fallbackUserName || undefined,
      })
    }
    if (form.value.scope === 'BOTH' || form.value.scope === 'TASK') {
      result.taskCount = await reassignDepartedInspector(form.value.userId, {
        reason: form.value.reason,
        fallbackInspectorId: form.value.fallbackUserId,
        fallbackInspectorName: form.value.fallbackUserName || undefined,
      })
    }
    lastResult.value = result
    ElMessage.success('重派完成')
  } catch (e: any) {
    ElMessage.error(e.message || '重派失败')
  } finally {
    submitting.value = false
  }
}

function reset() {
  form.value = {
    userId: undefined,
    reason: '',
    scope: 'BOTH',
    fallbackUserId: undefined,
    fallbackUserName: '',
  }
  lastResult.value = null
}
</script>

<template>
  <div class="p-5 max-w-3xl">
    <div class="flex items-center mb-5">
      <UserMinus class="w-5 h-5 mr-2 text-red-500" />
      <h2 class="text-lg font-semibold">离职 / 退出用户重派</h2>
    </div>

    <div class="mb-4 p-3 bg-yellow-50 border-l-4 border-yellow-400 rounded text-sm text-gray-700">
      <p>本操作用于用户离职 / 退出项目时, 把其所有 <strong>未完成</strong> 的整改单 (assignee) /
        检查任务 (inspector) 批量解除并可选自动重派给指定用户.</p>
      <p class="mt-1">已 SUBMITTED / VERIFIED / CLOSED 等终态记录不动. 操作有审计日志.</p>
    </div>

    <el-form :model="form" label-width="120px">
      <el-form-item label="离职用户 ID" required>
        <el-input-number v-model="form.userId" :min="1" :step="1" class="w-full" />
      </el-form-item>

      <el-form-item label="重派范围">
        <el-radio-group v-model="form.scope">
          <el-radio value="BOTH">整改 + 任务</el-radio>
          <el-radio value="CORRECTIVE">仅整改责任人</el-radio>
          <el-radio value="TASK">仅任务检查员</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="重派原因" required>
        <el-input v-model="form.reason" type="textarea" :rows="2"
                  placeholder="如: 张三于 2026-04-29 离职, 其名下整改和任务批量重派" />
      </el-form-item>

      <el-form-item label="新负责人 ID">
        <el-input-number v-model="form.fallbackUserId" :min="1" :step="1" class="w-full" />
        <div class="text-xs text-gray-500 mt-1">
          可选, 留空则记录解除后停留 OPEN/PENDING 等管理员逐个分配
        </div>
      </el-form-item>

      <el-form-item label="新负责人姓名" v-if="form.fallbackUserId">
        <el-input v-model="form.fallbackUserName" placeholder="审计/通知用, 可选" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          <UserCheck class="w-4 h-4 mr-1" />执行重派
        </el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <div v-if="lastResult" class="mt-6 p-4 bg-green-50 border border-green-200 rounded">
      <div class="font-medium text-green-800 mb-2">重派结果</div>
      <div v-if="lastResult.correctiveCount != null" class="text-sm text-gray-700">
        整改案例: {{ lastResult.correctiveCount }} 条
      </div>
      <div v-if="lastResult.taskCount != null" class="text-sm text-gray-700">
        检查任务: {{ lastResult.taskCount }} 条
      </div>
    </div>
  </div>
</template>
