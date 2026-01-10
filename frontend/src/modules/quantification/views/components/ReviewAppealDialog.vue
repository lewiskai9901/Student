<template>
  <el-dialog
    v-model="visible"
    title="审核申诉"
    width="600px"
    @close="handleClose"
  >
    <el-form
      v-if="appeal"
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
    >
      <!-- 申诉信息 -->
      <el-form-item label="班级">
        <el-text>{{ appeal.className }}</el-text>
      </el-form-item>

      <el-form-item label="扣分项">
        <el-text>{{ appeal.deductionItemName }}</el-text>
      </el-form-item>

      <el-form-item label="原扣分">
        <el-text type="danger">-{{ appeal.originalScore }}</el-text>
      </el-form-item>

      <el-form-item label="申诉理由">
        <el-text>{{ appeal.appealReason }}</el-text>
      </el-form-item>

      <el-form-item label="申诉图片" v-if="appeal.appealPhotoUrls">
        <div class="photo-preview">
          <el-image
            v-for="(url, index) in getPhotoUrls(appeal.appealPhotoUrls)"
            :key="index"
            :src="url"
            :preview-src-list="getPhotoUrls(appeal.appealPhotoUrls)"
            :initial-index="index"
            fit="cover"
            style="width: 80px; height: 80px; margin-right: 8px"
          />
        </div>
      </el-form-item>

      <!-- 审核表单 -->
      <el-divider />

      <el-form-item label="审核结果" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :label="2">通过</el-radio>
          <el-radio :label="3">驳回</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item
        v-if="form.status === 2"
        label="修订扣分"
        prop="revisedScore"
      >
        <el-input-number
          v-model="form.revisedScore"
          :min="0"
          :max="appeal.originalScore"
          :precision="1"
          :step="0.5"
        />
        <el-text size="small" type="info" style="margin-left: 10px">
          原扣分: {{ appeal.originalScore }}
        </el-text>
      </el-form-item>

      <el-form-item label="审核意见" prop="reviewOpinion">
        <el-input
          v-model="form.reviewOpinion"
          type="textarea"
          :rows="4"
          placeholder="请输入审核意见..."
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        提交审核
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { reviewAppeal, type AppealResponse, type AppealReviewRequest } from '@/api/v2/quantification-extra'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  modelValue: boolean
  appeal: AppealResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'reviewed'): void
}>()

const authStore = useAuthStore()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const formRef = ref<FormInstance>()
const form = reactive<{
  status: number
  revisedScore?: number
  reviewOpinion: string
}>({
  status: 2, // 默认通过
  revisedScore: undefined,
  reviewOpinion: ''
})

const rules: FormRules = {
  status: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
  reviewOpinion: [{ required: true, message: '请输入审核意见', trigger: 'blur' }]
}

const submitting = ref(false)

// 获取图片URL列表
const getPhotoUrls = (photoUrls: string) => {
  if (!photoUrls) return []
  return photoUrls.split(',').filter(url => url.trim())
}

// 监听对话框打开
watch(() => props.appeal, (newVal) => {
  if (newVal) {
    // 重置表单
    form.status = 2
    form.revisedScore = newVal.originalScore // 默认修订分数等于原扣分
    form.reviewOpinion = ''
    formRef.value?.clearValidate()
  }
})

// 提交审核
const handleSubmit = async () => {
  if (!props.appeal) return

  try {
    await formRef.value?.validate()
    submitting.value = true

    const data: AppealReviewRequest = {
      appealId: props.appeal.id,
      status: form.status,
      revisedScore: form.status === 2 ? form.revisedScore : undefined,
      reviewOpinion: form.reviewOpinion,
      reviewerId: authStore.user?.id || 0,
      reviewerName: authStore.user?.realName || ''
    }

    await reviewAppeal(props.appeal.id, data)
    ElMessage.success('审核成功')
    emit('reviewed')
    visible.value = false
  } catch (error: any) {
    if (error !== false) { // 不是表单验证错误
      ElMessage.error(error.message || '审核失败')
    }
  } finally {
    submitting.value = false
  }
}

// 关闭对话框
const handleClose = () => {
  visible.value = false
}
</script>

<style scoped lang="scss">
.photo-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
