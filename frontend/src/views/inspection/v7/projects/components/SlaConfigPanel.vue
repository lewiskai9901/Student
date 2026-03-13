<script setup lang="ts">
/**
 * SlaConfigPanel - SLA time limit + escalation chain config
 *
 * Configures response/resolution time limits and an escalation chain
 * for when SLA thresholds are breached.
 */
import { ref, computed } from 'vue'
import { Plus, Trash2, AlertTriangle, Search } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'

interface EscalationStep {
  level: number
  userId: number
  userName: string
  hours: number
}

interface SlaConfig {
  responseHours: number
  resolutionHours: number
  escalationChain: EscalationStep[]
}

const props = defineProps<{
  modelValue: SlaConfig
}>()

const emit = defineEmits<{
  'update:modelValue': [value: SlaConfig]
}>()

// ---------- Computed ----------

const responseHours = computed({
  get: () => props.modelValue.responseHours ?? 24,
  set: (val: number) => update({ responseHours: val }),
})

const resolutionHours = computed({
  get: () => props.modelValue.resolutionHours ?? 72,
  set: (val: number) => update({ resolutionHours: val }),
})

const escalationChain = computed(() => props.modelValue.escalationChain ?? [])

// ---------- Quick-add escalation dialog ----------

const addDialogVisible = ref(false)
const addForm = ref({
  userId: 0,
  userName: '',
  hours: 24,
})

function openAddEscalation() {
  const nextLevel = escalationChain.value.length + 1
  addForm.value = { userId: 0, userName: '', hours: nextLevel * 24 }
  addDialogVisible.value = true
}

function confirmAddEscalation() {
  if (!addForm.value.userId || !addForm.value.userName) {
    ElMessage.warning('请输入责任人信息')
    return
  }
  const newStep: EscalationStep = {
    level: escalationChain.value.length + 1,
    userId: addForm.value.userId,
    userName: addForm.value.userName,
    hours: addForm.value.hours,
  }
  const updated = [...escalationChain.value, newStep]
  update({ escalationChain: updated })
  addDialogVisible.value = false
}

function removeEscalation(index: number) {
  const updated = [...escalationChain.value]
  updated.splice(index, 1)
  // Re-number levels
  updated.forEach((step, i) => { step.level = i + 1 })
  update({ escalationChain: updated })
}

function updateEscalationHours(index: number, hours: number) {
  const updated = [...escalationChain.value]
  updated[index] = { ...updated[index], hours }
  update({ escalationChain: updated })
}

// ---------- Helper ----------

function update(partial: Partial<SlaConfig>) {
  emit('update:modelValue', { ...props.modelValue, ...partial })
}

function formatHours(hours: number): string {
  if (hours < 24) return `${hours} 小时`
  const days = Math.floor(hours / 24)
  const remain = hours % 24
  return remain > 0 ? `${days} 天 ${remain} 小时` : `${days} 天`
}
</script>

<template>
  <div class="sla-config-panel space-y-5">
    <!-- Time Limits -->
    <div>
      <h4 class="text-sm font-medium text-gray-700 mb-3">时限设置</h4>
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="mb-1 block text-sm text-gray-600">响应时限（小时）</label>
          <el-input-number
            v-model="responseHours"
            :min="1"
            :max="720"
            :step="1"
            controls-position="right"
            class="!w-full"
          />
          <p class="mt-1 text-xs text-gray-400">
            任务创建后，在此时间内必须被认领
          </p>
        </div>
        <div>
          <label class="mb-1 block text-sm text-gray-600">解决时限（小时）</label>
          <el-input-number
            v-model="resolutionHours"
            :min="1"
            :max="2160"
            :step="1"
            controls-position="right"
            class="!w-full"
          />
          <p class="mt-1 text-xs text-gray-400">
            任务认领后，在此时间内必须完成提交
          </p>
        </div>
      </div>
    </div>

    <!-- Summary -->
    <div class="rounded-md bg-blue-50 px-3 py-2">
      <p class="text-xs text-blue-600">
        响应: {{ formatHours(responseHours) }} | 解决: {{ formatHours(resolutionHours) }}
      </p>
    </div>

    <!-- Escalation Chain -->
    <div>
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-2">
          <AlertTriangle class="w-4 h-4 text-amber-500" />
          <h4 class="text-sm font-medium text-gray-700">升级链</h4>
          <el-tag size="small" type="info">{{ escalationChain.length }} 级</el-tag>
        </div>
        <el-button type="primary" size="small" @click="openAddEscalation">
          <Plus class="w-3.5 h-3.5 mr-1" />添加层级
        </el-button>
      </div>

      <el-table
        :data="escalationChain"
        stripe
        size="small"
        empty-text="暂无升级层级，超时后将无升级通知"
      >
        <el-table-column label="级别" width="70" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.level <= 2 ? 'warning' : 'danger'">
              L{{ row.level }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userName" label="责任人" min-width="100" />
        <el-table-column label="触发时间" width="160">
          <template #default="{ row, $index }">
            <div class="flex items-center gap-1">
              <el-input-number
                :model-value="row.hours"
                :min="1"
                :max="2160"
                size="small"
                controls-position="right"
                class="!w-24"
                @update:model-value="(val: number) => updateEscalationHours($index, val)"
              />
              <span class="text-xs text-gray-400">小时</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ $index }">
            <el-button
              link
              type="danger"
              size="small"
              @click="removeEscalation($index)"
            >
              <Trash2 class="w-3.5 h-3.5" />
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Add Escalation Dialog -->
    <el-dialog
      v-model="addDialogVisible"
      title="添加升级层级"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form :model="addForm" label-width="80px" size="default">
        <el-form-item label="用户ID">
          <el-input-number v-model="addForm.userId" :min="1" class="!w-full" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="addForm.userName" placeholder="输入责任人姓名" />
        </el-form-item>
        <el-form-item label="触发时间">
          <div class="flex items-center gap-2 w-full">
            <el-input-number
              v-model="addForm.hours"
              :min="1"
              :max="2160"
              controls-position="right"
              class="flex-1"
            />
            <span class="text-sm text-gray-500 shrink-0">小时后升级</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddEscalation">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
