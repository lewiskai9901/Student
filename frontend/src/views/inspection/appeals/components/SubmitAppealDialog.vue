<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as appealApi from '@/api/inspection/appeal'
import { http } from '@/utils/request'

const props = defineProps<{
  modelValue: boolean
  submissionDetailId: LongId
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
  observation: '',  // 申诉人观察 (给 AI 用)
})
const submitting = ref(false)
const aiSuggesting = ref(false)
const aiSuggestion = ref<{
  suggestedScore: number | null
  suggestedVerdict: string | null
  reasoning: string
  confidence: number
  provider: string
} | null>(null)

watch(visible, v => {
  if (v) {
    form.value = { reason: '', expectedAdjustment: undefined, attachments: '', observation: '' }
    aiSuggestion.value = null
  }
})

/** 调 AI 仲裁: 给申诉人参考 LLM 第三方建议 (heuristic 兜底) */
async function askAi() {
  if (!form.value.observation?.trim()) {
    ElMessage.warning('先填写"我方观察"再让 AI 评估')
    return
  }
  aiSuggesting.value = true
  try {
    const r = await http.post<any>('/inspection/ai/suggest-score', {
      description: form.value.observation,
      itemTitle: props.itemName || '检查项',
      itemMaxScore: 5,
      scoringMode: 'SCORE',
    })
    aiSuggestion.value = (r as any) || null
    // 若用户 reason 为空, 用 AI reasoning 预填
    if (!form.value.reason && aiSuggestion.value?.reasoning) {
      form.value.reason = aiSuggestion.value.reasoning
    }
    // expectedAdjustment 默认 = AI 建议分 - 当前分
    if (form.value.expectedAdjustment == null && aiSuggestion.value?.suggestedScore != null && props.currentScore != null) {
      form.value.expectedAdjustment = Number(aiSuggestion.value.suggestedScore) - Number(props.currentScore)
    }
    ElMessage.success(`AI 已给出建议 (${aiSuggestion.value?.provider || 'heuristic'})`)
  } catch (e: any) {
    ElMessage.warning('AI 暂不可用: ' + (e?.message || '未知'))
  } finally {
    aiSuggesting.value = false
  }
}

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
  <el-dialog v-model="visible" title="提交申诉" width="560px">
    <div v-if="itemName" class="mb-4 p-3 bg-gray-50 rounded text-sm">
      <div class="text-gray-600">扣分项: <span class="font-medium text-gray-800">{{ itemName }}</span></div>
      <div v-if="currentScore != null" class="text-gray-600 mt-1">
        当前分数: <span class="font-medium text-gray-800">{{ currentScore }}</span>
      </div>
    </div>
    <el-form :model="form" label-width="100px">
      <el-form-item label="我方观察">
        <el-input v-model="form.observation" type="textarea" :rows="2"
                  placeholder="描述你看到的现场情况 (供 AI 第三方评估用)" />
        <div class="mt-1 flex items-center justify-between">
          <span class="text-xs text-gray-500">填写观察后, 可让 AI 给出独立判断, 帮你写申诉理由</span>
          <el-button size="small" type="primary" plain :loading="aiSuggesting" @click="askAi">
             AI 评估
          </el-button>
        </div>
        <!-- AI 建议卡片 -->
        <div v-if="aiSuggestion" class="ai-card">
          <div class="ai-head">
            <span class="ai-icon"></span>
            <span class="ai-title">AI 第三方建议</span>
            <span class="ai-provider">{{ aiSuggestion.provider }} · 置信度 {{ Math.round((aiSuggestion.confidence||0)*100) }}%</span>
          </div>
          <div class="ai-body">
            <div v-if="aiSuggestion.suggestedScore != null" class="ai-score">
              建议分数: <strong>{{ aiSuggestion.suggestedScore }}</strong>
              <span v-if="currentScore != null" class="ai-delta" :class="aiSuggestion.suggestedScore > Number(currentScore) ? 'text-success' : ''">
                ({{ aiSuggestion.suggestedScore > Number(currentScore) ? '+' : '' }}{{ (Number(aiSuggestion.suggestedScore) - Number(currentScore)).toFixed(1) }})
              </span>
            </div>
            <div class="ai-reasoning">{{ aiSuggestion.reasoning }}</div>
            <div class="ai-hint">! 仅供参考, 最终由审核员判定</div>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="申诉理由" required>
        <el-input v-model="form.reason" type="textarea" :rows="4"
                  placeholder="请说明对该扣分项判定不服的具体理由 (可参考 AI 建议自动填充)" />
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

<style scoped>
.ai-card {
  margin-top: 8px; padding: 10px 12px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1px solid #93c5fd; border-radius: 8px;
}
.ai-head { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
.ai-icon { font-size: 16px; }
.ai-title { font-size: 13px; font-weight: 600; color: #1e40af; }
.ai-provider { margin-left: auto; font-size: 10px; color: #64748b; background: rgba(255,255,255,0.6); padding: 1px 6px; border-radius: 10px; }
.ai-body { font-size: 12px; }
.ai-score { color: #1e293b; margin-bottom: 4px; }
.ai-score strong { font-size: 14px; color: #1e40af; }
.ai-delta { margin-left: 4px; color: #f59e0b; font-weight: 500; }
.ai-delta.text-success { color: #22c55e; }
.ai-reasoning { color: #475569; line-height: 1.5; margin: 4px 0; }
.ai-hint { font-size: 10px; color: #94a3b8; }
</style>
