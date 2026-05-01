<script setup lang="ts">
/**
 * AiSuggestionDialog — Track 5 AI 辅助打分对话框.
 *
 * 检查员输入观察描述, 后端返回评分建议 + 类目标签 + 理由 + 置信度.
 * 检查员一键应用 (emit('apply', suggestion)) 或者忽略.
 */
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Sparkles } from 'lucide-vue-next'
import { suggestScore, type SuggestScoreResponse } from '@/api/inspection/aiScoring'

const props = defineProps<{
  modelValue: boolean
  itemTitle?: string
  itemMaxScore?: number
  scoringMode?: 'SCORE' | 'PASS_FAIL' | 'DEDUCTION'
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  apply: [SuggestScoreResponse]
}>()

const description = ref('')
const loading = ref(false)
const result = ref<SuggestScoreResponse | null>(null)

watch(() => props.modelValue, (open) => {
  if (open) {
    description.value = ''
    result.value = null
  }
})

async function handleSuggest() {
  if (!description.value.trim()) {
    ElMessage.warning('请输入观察描述')
    return
  }
  loading.value = true
  try {
    result.value = await suggestScore({
      description: description.value.trim(),
      itemTitle: props.itemTitle,
      itemMaxScore: props.itemMaxScore,
      scoringMode: props.scoringMode || 'SCORE',
    })
  } catch (e: any) {
    ElMessage.error(e?.message || 'AI 建议获取失败')
  } finally {
    loading.value = false
  }
}

function handleApply() {
  if (!result.value) return
  emit('apply', result.value)
  emit('update:modelValue', false)
}

function close() {
  emit('update:modelValue', false)
}

function confidenceClass(conf: number): string {
  if (conf >= 0.8) return 'conf-high'
  if (conf >= 0.6) return 'conf-medium'
  return 'conf-low'
}

function confidenceLabel(conf: number): string {
  if (conf >= 0.8) return '高'
  if (conf >= 0.6) return '中'
  return '低'
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    title="AI 辅助打分"
    width="540px"
    :close-on-click-modal="false"
    @close="close"
  >
    <div class="ai-body">
      <div class="ai-context" v-if="itemTitle">
        <span class="ai-label">评分项</span>
        <span class="ai-value">{{ itemTitle }}</span>
        <span v-if="itemMaxScore" class="ai-meta">满分 {{ itemMaxScore }}</span>
        <span class="ai-meta">{{ scoringMode || 'SCORE' }}</span>
      </div>

      <label class="ai-label">观察描述</label>
      <el-input
        v-model="description"
        type="textarea"
        :rows="4"
        placeholder="详细描述现场观察 (例如: 卫生区域多处脏污, 有异味, 学生纪律一般)"
        maxlength="500"
        show-word-limit
      />

      <div class="ai-actions">
        <button class="ai-btn ai-btn--primary" :disabled="loading || !description.trim()" @click="handleSuggest">
          <Sparkles :size="13" />
          {{ loading ? '正在生成…' : '生成建议' }}
        </button>
        <button class="ai-btn" @click="close">取消</button>
      </div>

      <div v-if="result" class="ai-result">
        <div class="ai-result-head">
          <span class="ai-result-title">评分建议</span>
          <span class="ai-conf" :class="confidenceClass(result.confidence)">
            置信度 {{ confidenceLabel(result.confidence) }} · {{ Math.round(result.confidence * 100) }}%
          </span>
          <span class="ai-provider">via {{ result.provider }}</span>
        </div>

        <div class="ai-suggestion">
          <template v-if="result.suggestedScore !== null">
            <span class="ai-score">{{ result.suggestedScore }}</span>
            <span v-if="itemMaxScore" class="ai-score-max">/ {{ itemMaxScore }}</span>
          </template>
          <template v-else-if="result.suggestedVerdict">
            <span class="ai-verdict" :class="`ai-verdict--${result.suggestedVerdict.toLowerCase()}`">
              {{ result.suggestedVerdict === 'PASS' ? '通过' : '不通过' }}
            </span>
          </template>
        </div>

        <div v-if="result.categoryTags.length" class="ai-tags">
          <span class="ai-label">问题类目</span>
          <span v-for="tag in result.categoryTags" :key="tag" class="ai-tag">{{ tag }}</span>
        </div>

        <div class="ai-reasoning">
          <span class="ai-label">理由</span>
          <p>{{ result.reasoning }}</p>
        </div>

        <div class="ai-actions">
          <button class="ai-btn ai-btn--primary" @click="handleApply">应用此建议</button>
          <button class="ai-btn" @click="close">关闭</button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.ai-body { display: flex; flex-direction: column; gap: 12px; font-size: 13px; }
.ai-context {
  display: flex; gap: 8px; align-items: baseline;
  padding: 8px 12px; background: #f8fafc; border-radius: 6px;
  font-size: 12px; color: #475569;
}
.ai-context .ai-label { color: #94a3b8; font-size: 10px; text-transform: uppercase; }
.ai-context .ai-value { font-weight: 600; color: #0f172a; }
.ai-context .ai-meta { font-family: var(--insp-font-mono, monospace); color: #64748b; }
.ai-label { display: block; font-size: 11px; color: #64748b; margin-bottom: 4px; font-weight: 500; }
.ai-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 4px; }
.ai-btn {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 6px 14px; height: 30px;
  border: 1px solid #e2e8f0; border-radius: 6px;
  background: #fff; color: #475569;
  font-size: 12px; font-family: inherit; cursor: pointer;
  transition: all 0.15s;
}
.ai-btn:hover:not(:disabled) { background: #f1f5f9; }
.ai-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.ai-btn--primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.ai-btn--primary:hover:not(:disabled) { background: #1d4ed8; }

.ai-result {
  margin-top: 8px; padding: 14px;
  background: linear-gradient(135deg, #eff6ff 0%, #f0f9ff 100%);
  border: 1px solid #bfdbfe; border-radius: 8px;
}
.ai-result-head { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.ai-result-title { font-size: 13px; font-weight: 600; color: #1e40af; }
.ai-conf {
  font-size: 11px; padding: 2px 8px; border-radius: 12px;
  font-weight: 500;
}
.conf-high { background: #dcfce7; color: #15803d; }
.conf-medium { background: #fef9c3; color: #a16207; }
.conf-low { background: #fef3c7; color: #b45309; }
.ai-provider {
  margin-left: auto; font-size: 10px; color: #94a3b8;
  font-family: var(--insp-font-mono, monospace);
}
.ai-suggestion {
  display: flex; align-items: baseline; gap: 8px;
  padding: 12px 0; margin-bottom: 8px;
  border-bottom: 1px dashed #cbd5e1;
}
.ai-score { font-size: 32px; font-weight: 700; color: #1e40af; line-height: 1; }
.ai-score-max { font-size: 14px; color: #64748b; }
.ai-verdict {
  font-size: 22px; font-weight: 700; padding: 4px 12px; border-radius: 6px;
}
.ai-verdict--pass { background: #dcfce7; color: #15803d; }
.ai-verdict--fail { background: #fee2e2; color: #b91c1c; }
.ai-tags { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin-bottom: 8px; }
.ai-tag {
  padding: 2px 8px; background: #fff; border: 1px solid #cbd5e1;
  border-radius: 12px; font-size: 11px; color: #475569;
}
.ai-reasoning { margin-bottom: 8px; }
.ai-reasoning p { margin: 0; font-size: 12px; color: #334155; line-height: 1.5; }
</style>
