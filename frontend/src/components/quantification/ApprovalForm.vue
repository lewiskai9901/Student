<template>
  <div class="p-6">
    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
    </div>

    <div v-else class="space-y-6">
      <!-- 记录摘要 -->
      <div class="rounded-lg border-l-4 border-blue-500 bg-blue-50 p-4">
        <h4 class="mb-3 text-sm font-semibold text-gray-900">检查记录摘要</h4>
        <div class="grid grid-cols-2 gap-3">
          <div><span class="text-sm text-gray-500">检查类型:</span><span class="ml-2 text-sm text-gray-900">{{ recordSummary.typeName }}</span></div>
          <div><span class="text-sm text-gray-500">检查对象:</span><span class="ml-2 text-sm text-gray-900">{{ recordSummary.targetName }}</span></div>
          <div><span class="text-sm text-gray-500">检查员:</span><span class="ml-2 text-sm text-gray-900">{{ recordSummary.checkerName }}</span></div>
          <div><span class="text-sm text-gray-500">检查时间:</span><span class="ml-2 text-sm text-gray-900">{{ formatDateTime(recordSummary.checkDate) }}</span></div>
          <div class="col-span-2">
            <span class="text-sm text-gray-500">检查得分:</span>
            <span :class="['ml-2 inline-flex rounded px-2 py-0.5 text-sm font-semibold', getScoreClass(recordSummary.scoreRate)]">
              {{ recordSummary.totalScore }}分 ({{ recordSummary.scoreRate }}%)
            </span>
          </div>
        </div>
      </div>

      <!-- 审核表单 -->
      <div class="space-y-4">
        <div>
          <label class="mb-2 block text-sm font-medium text-gray-700">审核结果 <span class="text-red-500">*</span></label>
          <div class="grid grid-cols-2 gap-4">
            <label
              class="flex cursor-pointer items-center gap-3 rounded-lg border-2 p-4 transition-all"
              :class="formData.status === 2 ? 'border-green-500 bg-green-50' : 'border-gray-200 hover:border-green-300'"
            >
              <input type="radio" v-model="formData.status" :value="2" class="h-4 w-4 text-green-600" />
              <Check class="h-5 w-5 text-green-600" />
              <span class="text-sm font-medium text-gray-900">通过审核</span>
            </label>
            <label
              class="flex cursor-pointer items-center gap-3 rounded-lg border-2 p-4 transition-all"
              :class="formData.status === 3 ? 'border-red-500 bg-red-50' : 'border-gray-200 hover:border-red-300'"
            >
              <input type="radio" v-model="formData.status" :value="3" class="h-4 w-4 text-red-600" />
              <X class="h-5 w-5 text-red-600" />
              <span class="text-sm font-medium text-gray-900">驳回申请</span>
            </label>
          </div>
        </div>

        <div>
          <label class="mb-1.5 block text-sm font-medium text-gray-700">审核意见 <span class="text-red-500">*</span></label>
          <textarea
            v-model="formData.approvalRemark"
            rows="4"
            maxlength="500"
            placeholder="请填写审核意见（至少10个字符）..."
            class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          ></textarea>
          <div class="mt-1 text-right text-xs text-gray-400">{{ formData.approvalRemark?.length || 0 }}/500</div>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="mt-6 flex justify-end gap-3 border-t border-gray-200 pt-4">
      <button class="h-9 rounded-lg border border-gray-300 bg-white px-4 text-sm text-gray-700 hover:bg-gray-50" @click="$emit('close')">取消</button>
      <button class="h-9 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700 disabled:opacity-50" :disabled="!formData.status || submitting" @click="handleSubmit">
        <span v-if="submitting" class="mr-1.5 inline-block h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></span>
        确定审核
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, X } from 'lucide-vue-next'
import { formatDateTime } from '@/utils/date'

interface Props { recordId: number | null }
const props = defineProps<Props>()
const emit = defineEmits<{ success: [], close: [] }>()

const loading = ref(false)
const submitting = ref(false)

const recordSummary = ref({ typeName: '', targetName: '', checkerName: '', checkDate: '', totalScore: 0, scoreRate: 0 })
const formData = reactive({ status: null as number | null, approvalRemark: '' })

const getScoreClass = (scoreRate: number) => {
  if (scoreRate >= 90) return 'bg-green-100 text-green-700'
  if (scoreRate >= 80) return 'bg-amber-100 text-amber-700'
  if (scoreRate >= 60) return 'bg-blue-100 text-blue-700'
  return 'bg-red-100 text-red-700'
}

const loadRecordSummary = async () => {
  if (!props.recordId) return
  loading.value = true
  try {
    // 模拟数据
    recordSummary.value = { typeName: '宿舍卫生检查', targetName: '1号楼201', checkerName: '张老师', checkDate: '2024-01-15T10:30:00', totalScore: 85, scoreRate: 85 }
  } catch (error) { ElMessage.error('加载记录信息失败') } finally { loading.value = false }
}

const handleSubmit = async () => {
  if (!formData.status) { ElMessage.warning('请选择审核结果'); return }
  if (!formData.approvalRemark || formData.approvalRemark.length < 10) { ElMessage.warning('审核意见至少10个字符'); return }
  submitting.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 1000))
    const statusText = formData.status === 2 ? '通过' : '驳回'
    ElMessage.success(`审核${statusText}成功`)
    emit('success')
  } catch (error: any) { ElMessage.error(error.message || '审核失败') } finally { submitting.value = false }
}

onMounted(() => { loadRecordSummary() })
</script>
