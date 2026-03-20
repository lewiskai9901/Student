<script setup lang="ts">
/**
 * EffectivenessReviewForm - Effectiveness verification form
 *
 * Used after a case has been verified and closed to confirm whether
 * the corrective action was truly effective over time. If the effectiveness
 * check fails, the case may be re-opened.
 */
import { ref, reactive } from 'vue'
import { CheckCircle, XCircle, CalendarDays } from 'lucide-vue-next'

const props = defineProps<{
  caseId: number
}>()

const emit = defineEmits<{
  confirmed: [data: { notes: string; checkDate: string }]
  failed: [data: { reason: string; checkDate: string; shouldReopen: boolean }]
}>()

const formRef = ref()
const submitting = ref(false)

const form = reactive({
  checkDate: new Date().toISOString().slice(0, 10),
  status: '' as 'CONFIRMED' | 'FAILED' | '',
  notes: '',
  failReason: '',
  shouldReopen: false,
})

const rules = {
  checkDate: [{ required: true, message: '请选择检查日期', trigger: 'change' }],
  status: [{ required: true, message: '请选择有效性状态', trigger: 'change' }],
  notes: [
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (form.status === 'CONFIRMED' && !value.trim()) {
          callback(new Error('请填写确认说明'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  failReason: [
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (form.status === 'FAILED' && !value.trim()) {
          callback(new Error('请填写失败原因'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (form.status === 'CONFIRMED') {
      emit('confirmed', {
        notes: form.notes,
        checkDate: form.checkDate,
      })
    } else {
      emit('failed', {
        reason: form.failReason,
        checkDate: form.checkDate,
        shouldReopen: form.shouldReopen,
      })
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="effectiveness-review-form">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mb-4"
    >
      <template #title>有效性验证</template>
      对已关闭的整改案例进行效果跟踪，确认整改措施是否真正有效。
    </el-alert>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      label-position="top"
    >
      <!-- Check date -->
      <el-form-item label="检查日期" prop="checkDate">
        <el-date-picker
          v-model="form.checkDate"
          type="date"
          placeholder="选择检查日期"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        >
          <template #prefix>
            <CalendarDays class="w-4 h-4" />
          </template>
        </el-date-picker>
      </el-form-item>

      <!-- Effectiveness status -->
      <el-form-item label="有效性状态" prop="status">
        <el-radio-group v-model="form.status" size="large">
          <el-radio-button value="CONFIRMED">
            <div class="flex items-center gap-1">
              <CheckCircle class="w-4 h-4" />
              <span>有效</span>
            </div>
          </el-radio-button>
          <el-radio-button value="FAILED">
            <div class="flex items-center gap-1">
              <XCircle class="w-4 h-4" />
              <span>无效</span>
            </div>
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- Notes (when CONFIRMED) -->
      <el-form-item
        v-if="form.status === 'CONFIRMED'"
        label="确认说明"
        prop="notes"
      >
        <el-input
          v-model="form.notes"
          type="textarea"
          :rows="3"
          placeholder="请说明整改措施的持续效果及验证依据..."
          maxlength="1000"
          show-word-limit
        />
      </el-form-item>

      <!-- Failure details (when FAILED) -->
      <template v-if="form.status === 'FAILED'">
        <el-form-item label="失败原因" prop="failReason">
          <el-input
            v-model="form.failReason"
            type="textarea"
            :rows="3"
            placeholder="请详细说明整改措施无效的原因、表现及影响..."
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="重新开启">
          <el-checkbox v-model="form.shouldReopen">
            整改无效，需要重新开启整改案例
          </el-checkbox>
          <div class="text-gray-400 text-xs mt-1">
            勾选后将重新打开此案例，重新进入整改流程。
          </div>
        </el-form-item>
      </template>

      <!-- Submit -->
      <el-form-item>
        <el-button
          :type="form.status === 'CONFIRMED' ? 'success' : form.status === 'FAILED' ? 'danger' : 'primary'"
          :loading="submitting"
          :disabled="!form.status"
          @click="handleSubmit"
        >
          {{
            form.status === 'CONFIRMED'
              ? '确认有效'
              : form.status === 'FAILED'
                ? '确认无效'
                : '提交验证'
          }}
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.effectiveness-review-form {
  padding: 8px 0;
}

:deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
}
</style>
