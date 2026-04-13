<script setup lang="ts">
/**
 * CorrectionForm - Submit correction action form
 *
 * Allows the assignee to submit a corrective action with description,
 * optional file evidence, and root cause reference.
 */
import { ref, reactive } from 'vue'
import { Send, Upload } from 'lucide-vue-next'
import type { UploadFile } from 'element-plus'

const props = defineProps<{
  caseId: number
}>()

const emit = defineEmits<{
  submitted: [data: { correctionNote: string; evidenceIds: number[]; rootCauseRef?: string }]
}>()

const formRef = ref()
const submitting = ref(false)

const form = reactive({
  correctionNote: '',
  rootCauseRef: '',
})

const uploadedFiles = ref<UploadFile[]>([])
const evidenceIds = ref<number[]>([])

const rules = {
  correctionNote: [
    { required: true, message: '请填写整改措施描述', trigger: 'blur' },
    { min: 10, message: '整改描述不少于10个字符', trigger: 'blur' },
  ],
}

function handleUploadSuccess(response: any, file: UploadFile) {
  if (response?.id) {
    evidenceIds.value.push(response.id)
  }
}

function handleUploadRemove(file: UploadFile) {
  const index = uploadedFiles.value.findIndex((f) => f.uid === file.uid)
  if (index !== -1) {
    evidenceIds.value.splice(index, 1)
  }
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
    emit('submitted', {
      correctionNote: form.correctionNote,
      evidenceIds: evidenceIds.value,
      rootCauseRef: form.rootCauseRef || undefined,
    })
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  form.correctionNote = ''
  form.rootCauseRef = ''
  uploadedFiles.value = []
  evidenceIds.value = []
  formRef.value?.resetFields()
}

defineExpose({ resetForm })
</script>

<template>
  <div class="correction-form">
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      label-position="top"
    >
      <!-- Correction description -->
      <el-form-item label="整改措施描述" prop="correctionNote">
        <el-input
          v-model="form.correctionNote"
          type="textarea"
          :rows="5"
          placeholder="请详细描述所采取的整改措施、完成情况及效果..."
          maxlength="2000"
          show-word-limit
        />
      </el-form-item>

      <!-- Evidence upload -->
      <el-form-item label="佐证材料">
        <el-upload
          v-model:file-list="uploadedFiles"
          action="/api/v7/insp/files/upload"
          :limit="5"
          :on-success="handleUploadSuccess"
          :on-remove="handleUploadRemove"
          accept="image/*,.pdf,.doc,.docx"
          multiple
        >
          <el-button type="primary" plain>
            <Upload class="w-4 h-4 mr-1" />
            上传文件
          </el-button>
          <template #tip>
            <div class="text-gray-400 text-xs mt-1">
              支持图片、PDF、Word 文件，最多5个
            </div>
          </template>
        </el-upload>
      </el-form-item>

      <!-- Root cause reference -->
      <el-form-item label="根因分析引用">
        <el-input
          v-model="form.rootCauseRef"
          placeholder="可引用根因分析编号或简要说明根因（可选）"
          maxlength="500"
        />
      </el-form-item>

      <!-- Submit -->
      <el-form-item>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
        >
          <Send class="w-4 h-4 mr-1" />
          提交整改
        </el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.correction-form {
  padding: 8px 0;
}
</style>
