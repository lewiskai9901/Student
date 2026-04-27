<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as appealApi from '@/api/inspection/appeal'

const props = defineProps<{
  modelValue: boolean
  submissionDetailId: number
  itemName?: string
  currentScore?: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'submitted': []
}>()

const visible = ref(props.modelValue)
watch(() => props.modelValue, v => (visible.value = v))
watch(visible, v => emit('update:modelValue', v))

const form = ref({
  reason: '',
  expectedAdjustment: undefined as number | undefined,
  attachments: '',
})
const submitting = ref(false)

watch(visible, v => {
  if (v) {
    form.value = { reason: '', expectedAdjustment: undefined, attachments: '' }
  }
})

async function handleSubmit() {
  if (!form.value.reason?.trim()) {
    ElMessage.warning('请填写申诉理由')
    return
  }
  submitting.value = true
  try {
    await appealApi.submitAppeal({
      submissionDetailId: props.submissionDetailId,
      reason: form.value.reason,
      expectedAdjustment: form.value.expectedAdjustment,
      attachments: form.value.attachments || undefined,
    })
    ElMessage.success('申诉已提交, 请等待审核')
    visible.value = false
    emit('submitted')
  } catch (e: any) {
    ElMessage.error(e.message || '申诉提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="提交申诉" width="500px">
    <div v-if="itemName" class="mb-4 p-3 bg-gray-50 rounded text-sm">
      <div class="text-gray-600">扣分项: <span class="font-medium text-gray-800">{{ itemName }}</span></div>
      <div v-if="currentScore != null" class="text-gray-600 mt-1">
        当前分数: <span class="font-medium text-gray-800">{{ currentScore }}</span>
      </div>
    </div>
    <el-form :model="form" label-width="100px">
      <el-form-item label="申诉理由" required>
        <el-input v-model="form.reason" type="textarea" :rows="4"
                  placeholder="请说明对该扣分项判定不服的具体理由" />
      </el-form-item>
      <el-form-item label="期望调整">
        <el-input-number v-model="form.expectedAdjustment" :precision="2" :step="0.5" class="w-full" />
        <div class="text-xs text-gray-500 mt-1">
          期望分数调整值, 审核员会基于此和实际情况决定最终值
        </div>
      </el-form-item>
      <el-form-item label="证据附件">
        <el-input v-model="form.attachments" type="textarea" :rows="2"
                  placeholder="附件 URL (多个用逗号分隔, 后续可优化为上传组件)" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申诉</el-button>
    </template>
  </el-dialog>
</template>
