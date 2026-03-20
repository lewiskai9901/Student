<script setup lang="ts">
/**
 * VerificationForm - Verification form for submitted corrections
 *
 * Displays correction details (readonly) and allows the verifier to
 * pass or fail the correction with notes.
 */
import { ref, reactive } from 'vue'
import { CheckCircle, XCircle } from 'lucide-vue-next'

interface CorrectionInfo {
  correctionNote: string | null
  correctedAt: string | null
  assigneeName: string | null
  evidenceIds?: number[]
}

const props = defineProps<{
  caseId: number
  correction: CorrectionInfo
}>()

const emit = defineEmits<{
  verified: [data: { note: string }]
  rejected: [data: { reason: string }]
}>()

const formRef = ref()
const submitting = ref(false)

const form = reactive({
  verdict: '' as 'PASS' | 'FAIL' | '',
  notes: '',
})

const rules = {
  verdict: [{ required: true, message: '请选择验证结果', trigger: 'change' }],
  notes: [{ required: true, message: '请填写验证说明', trigger: 'blur' }],
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
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
    if (form.verdict === 'PASS') {
      emit('verified', { note: form.notes })
    } else {
      emit('rejected', { reason: form.notes })
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="verification-form">
    <!-- Correction details (readonly) -->
    <el-card shadow="never" class="mb-4">
      <template #header>
        <span class="font-medium text-sm">整改提交详情</span>
      </template>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="责任人">
          {{ correction.assigneeName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">
          {{ formatDate(correction.correctedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="整改说明">
          <div class="whitespace-pre-wrap">{{ correction.correctionNote || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="佐证材料">
          <span v-if="correction.evidenceIds && correction.evidenceIds.length > 0">
            {{ correction.evidenceIds.length }} 个文件
          </span>
          <span v-else class="text-gray-400">无</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- Verification form -->
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      label-position="top"
    >
      <!-- Verdict -->
      <el-form-item label="验证结果" prop="verdict">
        <el-radio-group v-model="form.verdict" size="large">
          <el-radio-button value="PASS">
            <div class="flex items-center gap-1">
              <CheckCircle class="w-4 h-4" />
              <span>通过</span>
            </div>
          </el-radio-button>
          <el-radio-button value="FAIL">
            <div class="flex items-center gap-1">
              <XCircle class="w-4 h-4" />
              <span>不通过</span>
            </div>
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- Notes -->
      <el-form-item label="验证说明" prop="notes">
        <el-input
          v-model="form.notes"
          type="textarea"
          :rows="4"
          :placeholder="
            form.verdict === 'FAIL'
              ? '请说明不通过的原因及需要补充整改的内容...'
              : '请填写验证说明、验证依据等...'
          "
          maxlength="1000"
          show-word-limit
        />
      </el-form-item>

      <!-- Submit -->
      <el-form-item>
        <el-button
          :type="form.verdict === 'PASS' ? 'success' : form.verdict === 'FAIL' ? 'danger' : 'primary'"
          :loading="submitting"
          :disabled="!form.verdict"
          @click="handleSubmit"
        >
          {{ form.verdict === 'PASS' ? '确认通过' : form.verdict === 'FAIL' ? '确认驳回' : '提交验证' }}
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.verification-form {
  padding: 8px 0;
}

:deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
}
</style>
