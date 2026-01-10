<template>
  <Teleport to="body">
    <div v-if="modelValue" class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="fixed inset-0 bg-black/50" @click="handleClose"></div>
      <div class="relative w-full max-w-lg rounded-lg bg-white shadow-xl">
        <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
          <h3 class="text-base font-semibold text-gray-900">{{ isEdit ? '编辑年级' : '新增年级' }}</h3>
          <button class="text-gray-400 hover:text-gray-600" @click="handleClose"><X class="h-5 w-5" /></button>
        </div>
        <form @submit.prevent="handleSubmit" class="p-4">
          <div class="space-y-4">
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">年级名称 <span class="text-red-500">*</span></label>
              <input v-model="form.gradeName" type="text" placeholder="请输入年级名称" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" :class="{ 'border-red-500': errors.gradeName }" />
              <p v-if="errors.gradeName" class="mt-1 text-xs text-red-500">{{ errors.gradeName }}</p>
            </div>
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">年级代码 <span class="text-red-500">*</span></label>
              <input v-model="form.gradeCode" type="text" placeholder="请输入年级代码" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" :class="{ 'border-red-500': errors.gradeCode }" />
              <p v-if="errors.gradeCode" class="mt-1 text-xs text-red-500">{{ errors.gradeCode }}</p>
            </div>
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">入学年份 <span class="text-red-500">*</span></label>
              <el-date-picker
                v-model="form.enrollmentYear"
                type="year"
                placeholder="选择入学年份"
                format="YYYY"
                value-format="YYYY"
                class="!w-full"
                :clearable="false"
              />
            </div>
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">标准班额 <span class="text-red-500">*</span></label>
              <input v-model.number="form.standardClassSize" type="number" min="1" max="100" placeholder="请输入标准班额" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
            </div>
            <div class="flex items-center gap-3">
              <label class="text-sm font-medium text-gray-700">状态</label>
              <button type="button" @click="form.status = form.status === 1 ? 0 : 1" :class="['relative inline-flex h-6 w-11 items-center rounded-full transition-colors', form.status === 1 ? 'bg-blue-600' : 'bg-gray-200']">
                <span :class="['inline-block h-4 w-4 transform rounded-full bg-white transition-transform', form.status === 1 ? 'translate-x-6' : 'translate-x-1']"></span>
              </button>
              <span class="text-sm text-gray-500">{{ form.status === 1 ? '启用' : '禁用' }}</span>
            </div>
          </div>
          <div class="mt-6 flex justify-end gap-3 border-t border-gray-200 pt-4">
            <button type="button" class="h-9 rounded-lg border border-gray-300 bg-white px-4 text-sm text-gray-700 hover:bg-gray-50" @click="handleClose">取消</button>
            <button type="submit" :disabled="loading" class="h-9 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700 disabled:opacity-50">
              <span v-if="loading" class="mr-1.5 inline-block h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></span>
              确定
            </button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, ElDatePicker } from 'element-plus'
import { X } from 'lucide-vue-next'
import { createGrade, updateGrade } from '@/api/v2/organization'
import type { Grade } from '@/api/v2/organization'

interface Props { modelValue: boolean, formData: Partial<Grade>, isEdit: boolean }
const props = defineProps<Props>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean], success: [] }>()

const loading = ref(false)
const errors = reactive<Record<string, string>>({})

const currentYear = new Date().getFullYear()

const form = reactive({
  gradeName: '', gradeCode: '', enrollmentYear: currentYear.toString(), standardClassSize: 40, status: 1
})

watch(() => props.formData, (newVal) => {
  if (newVal && Object.keys(newVal).length > 0) {
    Object.assign(form, {
      gradeName: newVal.gradeName || '', gradeCode: newVal.gradeCode || '',
      enrollmentYear: newVal.enrollmentYear?.toString() || currentYear.toString(), standardClassSize: newVal.standardClassSize || 40, status: newVal.status ?? 1
    })
  }
}, { immediate: true, deep: true })

const validate = () => {
  Object.keys(errors).forEach(key => { errors[key] = '' })
  let valid = true
  if (!form.gradeName) { errors.gradeName = '请输入年级名称'; valid = false }
  if (!form.gradeCode) { errors.gradeCode = '请输入年级代码'; valid = false }
  return valid
}

const handleSubmit = async () => {
  if (!validate()) return
  loading.value = true
  try {
    const data: any = {
      gradeName: form.gradeName, gradeCode: form.gradeCode,
      enrollmentYear: parseInt(form.enrollmentYear), standardClassSize: form.standardClassSize
    }
    if (props.isEdit && props.formData.id) {
      data.id = props.formData.id
      data.status = form.status
      await updateGrade(data)
      ElMessage.success('更新成功')
    } else {
      await createGrade(data)
      ElMessage.success('创建成功')
    }
    emit('success')
    handleClose()
  } catch (error: any) {
    console.error('提交失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally { loading.value = false }
}

const handleClose = () => {
  Object.assign(form, { gradeName: '', gradeCode: '', enrollmentYear: currentYear.toString(), standardClassSize: 40, status: 1 })
  Object.keys(errors).forEach(key => { errors[key] = '' })
  emit('update:modelValue', false)
}
</script>
