<template>
  <details class="cor-editor" :open="hasRule">
    <summary>
      <span class="cor-title">整改触发规则覆盖</span>
      <span v-if="hasRule" class="cor-active">已启用</span>
      <span v-else class="cor-inactive">未启用 (用项目级策略)</span>
    </summary>

    <div class="cor-body">
      <p class="cor-hint">
        覆盖此检查项的整改判定规则. 留空 = 走项目级策略.
        优先级: neverCorrect &gt; forceCorrect &gt; thresholdOverride &gt; 项目级.
      </p>

      <!-- neverCorrect -->
      <div class="cor-row">
        <label class="cor-label">
          <el-switch v-model="rule.neverCorrect" size="small" />
          <span>永不建整改单</span>
        </label>
        <span class="cor-tip">无论分数多低,本项绝不触发整改 (例如"备注栏")</span>
      </div>

      <!-- forceCorrect -->
      <div class="cor-row">
        <label class="cor-label">强制触发响应值</label>
        <el-select
          v-model="rule.forceCorrect"
          multiple filterable allow-create default-first-option
          placeholder="例如 FAIL / D / NO"
          size="small" style="width:280px"
          :disabled="rule.neverCorrect"
        >
          <el-option v-for="opt in COMMON_RESPONSES" :key="opt" :label="opt" :value="opt" />
        </el-select>
        <span class="cor-tip">命中即建 HIGH 单 (无视分数)</span>
      </div>

      <!-- thresholdOverride -->
      <div class="cor-row cor-row--column">
        <label class="cor-label">
          <el-switch v-model="useThreshold" size="small"
                     :disabled="rule.neverCorrect" />
          <span>覆盖严重度阈值</span>
        </label>
        <div v-if="useThreshold" class="cor-grid">
          <div class="cor-cell">
            <label>HIGH 阈值</label>
            <el-input-number v-model="rule.thresholdHigh"
              :min="0" :max="1" :step="0.05" :precision="2" size="small" style="width:100%" />
          </div>
          <div class="cor-cell">
            <label>MEDIUM 阈值</label>
            <el-input-number v-model="rule.thresholdMedium"
              :min="0" :max="1" :step="0.05" :precision="2" size="small" style="width:100%" />
          </div>
          <div class="cor-cell">
            <label>LOW 阈值</label>
            <el-input-number v-model="rule.thresholdLow"
              :min="0" :max="1" :step="0.05" :precision="2" size="small" style="width:100%" />
          </div>
        </div>
      </div>

      <!-- deadlineOverride -->
      <div class="cor-row cor-row--column">
        <label class="cor-label">
          <el-switch v-model="useDeadline" size="small"
                     :disabled="rule.neverCorrect" />
          <span>覆盖整改 deadline</span>
        </label>
        <div v-if="useDeadline" class="cor-grid">
          <div class="cor-cell">
            <label>HIGH (天)</label>
            <el-input-number v-model="rule.deadlineHigh"
              :min="1" :max="60" size="small" style="width:100%" />
          </div>
          <div class="cor-cell">
            <label>MEDIUM (天)</label>
            <el-input-number v-model="rule.deadlineMedium"
              :min="1" :max="60" size="small" style="width:100%" />
          </div>
          <div class="cor-cell">
            <label>LOW (天)</label>
            <el-input-number v-model="rule.deadlineLow"
              :min="1" :max="60" size="small" style="width:100%" />
          </div>
        </div>
      </div>

      <div class="cor-actions">
        <el-button size="small" @click="clear">清除</el-button>
        <el-button type="primary" size="small" :loading="saving" @click="save">
          保存覆盖规则
        </el-button>
      </div>
    </div>
  </details>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'

interface Props {
  itemId: number | null
}

const props = defineProps<Props>()

const COMMON_RESPONSES = ['FAIL', 'D', 'NO', 'FALSE', '0', 'C', 'E']

const rule = ref({
  neverCorrect: false,
  forceCorrect: [] as string[],
  thresholdHigh: null as number | null,
  thresholdMedium: null as number | null,
  thresholdLow: null as number | null,
  deadlineHigh: null as number | null,
  deadlineMedium: null as number | null,
  deadlineLow: null as number | null,
})
const useThreshold = ref(false)
const useDeadline = ref(false)
const saving = ref(false)

const hasRule = computed(() =>
  rule.value.neverCorrect ||
  rule.value.forceCorrect.length > 0 ||
  useThreshold.value ||
  useDeadline.value
)

watch(() => props.itemId, async itemId => {
  resetRule()
  if (!itemId) return
  try {
    const json = await http.get<string | null>(
      `/inspection/corrective/template-items/${itemId}/override`
    )
    if (!json) return
    const obj = typeof json === 'string' ? JSON.parse(json) : json
    rule.value.neverCorrect = !!obj.neverCorrect
    rule.value.forceCorrect = Array.isArray(obj.forceCorrect) ? obj.forceCorrect : []
    if (obj.thresholdOverride) {
      useThreshold.value = true
      rule.value.thresholdHigh = obj.thresholdOverride.high ?? null
      rule.value.thresholdMedium = obj.thresholdOverride.medium ?? null
      rule.value.thresholdLow = obj.thresholdOverride.low ?? null
    }
    if (obj.deadlineOverride) {
      useDeadline.value = true
      rule.value.deadlineHigh = obj.deadlineOverride.high ?? null
      rule.value.deadlineMedium = obj.deadlineOverride.medium ?? null
      rule.value.deadlineLow = obj.deadlineOverride.low ?? null
    }
  } catch (e) {
    console.warn('加载 ItemRule 失败', e)
  }
}, { immediate: true })

function resetRule() {
  rule.value = {
    neverCorrect: false,
    forceCorrect: [],
    thresholdHigh: null, thresholdMedium: null, thresholdLow: null,
    deadlineHigh: null, deadlineMedium: null, deadlineLow: null,
  }
  useThreshold.value = false
  useDeadline.value = false
}

function buildPayload(): Record<string, unknown> {
  const out: Record<string, unknown> = {}
  if (rule.value.neverCorrect) {
    out.neverCorrect = true
    return out
  }
  if (rule.value.forceCorrect.length > 0) out.forceCorrect = rule.value.forceCorrect
  if (useThreshold.value) {
    out.thresholdOverride = {
      high: rule.value.thresholdHigh ?? 0.8,
      medium: rule.value.thresholdMedium ?? 0.5,
      low: rule.value.thresholdLow ?? 0.3,
    }
  }
  if (useDeadline.value) {
    out.deadlineOverride = {
      high: rule.value.deadlineHigh ?? 3,
      medium: rule.value.deadlineMedium ?? 7,
      low: rule.value.deadlineLow ?? 14,
    }
  }
  return out
}

async function save() {
  if (!props.itemId) {
    ElMessage.warning('请先保存检查项, 再设置整改规则')
    return
  }
  saving.value = true
  try {
    const payload = buildPayload()
    await http.put(`/inspection/corrective/template-items/${props.itemId}/override`, payload)
    ElMessage.success(Object.keys(payload).length === 0 ? '已清除覆盖规则' : '已保存覆盖规则')
  } catch (e: unknown) {
    console.error('保存 ItemRule 失败', e)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function clear() {
  resetRule()
  if (!props.itemId) return
  saving.value = true
  try {
    await http.put(`/inspection/corrective/template-items/${props.itemId}/override`, {})
    ElMessage.success('已清除覆盖规则')
  } catch {
    ElMessage.error('清除失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.cor-editor {
  margin-top: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fafbfc;
}
.cor-editor[open] { background: #fff; border-color: #d1d5db; }
.cor-editor summary {
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
  display: flex; align-items: center; gap: 10px;
  font-size: 13px; font-weight: 500;
  color: #1f2937;
}
.cor-title { flex: 1; }
.cor-active {
  background: #ede9fe; color: #6d28d9;
  padding: 1px 8px; border-radius: 3px;
  font-size: 11px; font-weight: 600;
}
.cor-inactive {
  color: #9ca3af; font-size: 12px; font-weight: 400;
}
.cor-body {
  padding: 0 14px 16px;
  border-top: 1px solid #f3f4f6;
}
.cor-hint {
  margin: 12px 0;
  padding: 8px 12px;
  background: #f5f3ff; color: #5b21b6;
  font-size: 12px; line-height: 1.5;
  border-radius: 4px;
}
.cor-row {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 0;
}
.cor-row--column { flex-direction: column; align-items: stretch; }
.cor-label {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: #374151; min-width: 130px;
}
.cor-tip { font-size: 12px; color: #9ca3af; }
.cor-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 8px;
  padding-left: 22px;
}
.cor-cell label {
  display: block;
  font-size: 12px; color: #6b7280;
  margin-bottom: 3px;
}
.cor-actions {
  display: flex; justify-content: flex-end; gap: 8px;
  margin-top: 12px;
  border-top: 1px dashed #e5e7eb;
  padding-top: 12px;
}
</style>
