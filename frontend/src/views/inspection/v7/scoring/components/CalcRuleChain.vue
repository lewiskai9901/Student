<template>
  <div class="space-y-3">
    <div class="flex items-center justify-between">
      <h3 class="sp-section-title">计算规则链</h3>
      <button class="sp-btn-primary sm" @click="showAdd = true">
        添加规则
      </button>
    </div>

    <p class="sp-section-desc">规则按优先级从低到高依次执行</p>

    <div v-if="rules.length === 0" class="sp-empty">
      暂无规则
    </div>

    <div v-else class="space-y-2">
      <div
        v-for="(rule, idx) in rules"
        :key="rule.id"
        class="sp-rule-card group"
        :class="rule.isEnabled ? 'is-enabled' : 'is-disabled'"
      >
        <div class="flex items-center gap-3">
          <!-- Priority badge -->
          <div class="sp-priority-badge">
            {{ idx + 1 }}
          </div>
          <!-- Rule type badge -->
          <span
            class="sp-type-badge"
            :style="{ backgroundColor: RuleTypeConfig[rule.ruleType]?.color || '#909399' }"
          >
            {{ RuleTypeConfig[rule.ruleType]?.label || rule.ruleType }}
          </span>
          <!-- Name -->
          <div class="flex-1 min-w-0">
            <div class="sp-rule-name truncate">{{ rule.ruleName }}</div>
            <div class="sp-rule-code truncate">{{ rule.ruleCode }}</div>
          </div>
          <!-- Priority -->
          <div class="text-right shrink-0">
            <div class="sp-priority-label">优先级</div>
            <div class="sp-priority-value">{{ rule.priority }}</div>
          </div>
          <!-- Actions -->
          <div class="sp-rule-actions">
            <button
              class="sp-ic-s"
              :title="rule.isEnabled ? '禁用' : '启用'"
              @click="toggleEnabled(rule)"
            >
              <component
                :is="rule.isEnabled ? iconMap.ToggleRight : iconMap.ToggleLeft"
                class="w-4 h-4"
                :class="rule.isEnabled ? 'text-green-500' : 'text-gray-400'"
              />
            </button>
            <button class="sp-ic-s" @click="startEdit(rule)">
              <component :is="iconMap.Pencil" class="w-3.5 h-3.5" />
            </button>
            <button class="sp-ic-s danger" @click="$emit('delete', rule.id)">
              <component :is="iconMap.Trash2" class="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
        <!-- Config summary (readable) -->
        <div class="sp-config-summary">
          {{ getConfigSummary(rule.ruleType, rule.config) }}
        </div>
      </div>
    </div>

    <!-- Add/Edit Dialog -->
    <Teleport to="body">
      <Transition name="sp-modal">
        <div v-if="showAdd || editingRule" class="sp-mask" @click.self="closeDialog">
          <div class="sp-modal">
            <div class="sp-modal-head">
              <h3>{{ editingRule ? '编辑规则' : '添加规则' }}</h3>
              <button class="sp-modal-close" @click="closeDialog">&times;</button>
            </div>
            <div class="sp-modal-body sp-modal-scroll">
              <div class="flex gap-3">
                <div class="sp-fld w-40" v-if="!editingRule">
                  <label>规则编码</label>
                  <input v-model="form.ruleCode" placeholder="ceiling_100" />
                </div>
                <div class="sp-fld flex-1">
                  <label>规则名称</label>
                  <input v-model="form.ruleName" placeholder="满分封顶" />
                </div>
              </div>
              <div class="flex gap-3">
                <div class="sp-fld flex-1">
                  <label>规则类型</label>
                  <select v-model="form.ruleType">
                    <option v-for="(cfg, key) in RuleTypeConfig" :key="key" :value="key">
                      {{ cfg.label }} - {{ cfg.description }}
                    </option>
                  </select>
                  <p class="help">
                    {{ RuleTypeConfig[form.ruleType]?.description }}
                  </p>
                </div>
                <div class="sp-fld w-24">
                  <label>优先级</label>
                  <input v-model.number="form.priority" type="number" min="0" />
                </div>
              </div>
              <!-- Structured config form -->
              <div class="sp-config-section">
                <div class="sp-fld">
                  <label>规则配置</label>
                </div>
                <RuleConfigForm v-model="form.config" :rule-type="form.ruleType" :template-id="props.templateId" />
              </div>
              <div class="flex items-center gap-2">
                <input v-model="form.isEnabled" type="checkbox" id="rule-enabled" class="rounded" />
                <label for="rule-enabled" class="sp-checkbox-label">启用</label>
              </div>
            </div>
            <div class="sp-modal-foot">
              <button class="sp-btn-ghost" @click="closeDialog">取消</button>
              <button class="sp-btn-primary" @click="handleSubmit">
                {{ editingRule ? '保存' : '添加' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Pencil, Trash2, ToggleLeft, ToggleRight } from 'lucide-vue-next'
import type { CalculationRule, CreateRuleRequest, UpdateRuleRequest, RuleType } from '@/types/insp/scoring'
import { RuleTypeConfig } from '@/types/insp/scoring'
import RuleConfigForm from './RuleConfigForm.vue'

const iconMap = { Pencil, Trash2, ToggleLeft, ToggleRight }

const props = defineProps<{
  rules: CalculationRule[]
  templateId?: number
}>()

const emit = defineEmits<{
  create: [data: CreateRuleRequest]
  update: [id: number, data: UpdateRuleRequest]
  delete: [id: number]
}>()

const showAdd = ref(false)
const editingRule = ref<CalculationRule | null>(null)

const form = ref({
  ruleCode: '',
  ruleName: '',
  priority: 0,
  ruleType: 'VETO' as RuleType,
  config: '{"conditions":[]}',
  isEnabled: true,
})

// ==================== Config summary ====================

function getConfigSummary(ruleType: RuleType, configJson: string): string {
  try {
    const c = JSON.parse(configJson)
    const condCount = (c.conditions || []).length
    switch (ruleType) {
      case 'VETO': {
        const logic = c.conditionLogic === 'ALL' ? 'AND' : 'OR'
        const hasCount = (c.conditions || []).some((x: any) => (x.minCount || 1) > 1)
        const lookback = hasCount ? ` | ${c.lookbackDays ?? 30}天` : ''
        return `${condCount}项${condCount > 1 ? `(${logic})` : ''}${lookback} | 否决→${c.vetoScore ?? 0}分`
      }
      case 'PENALTY': {
        const penLogic = c.conditionLogic === 'ALL' ? 'AND' : 'OR'
        const penHasCount = (c.conditions || []).some((x: any) => (x.minCount || 1) > 1)
        const penLookback = penHasCount ? ` | ${c.lookbackDays ?? 30}天` : ''
        return `${condCount}项${condCount > 1 ? `(${penLogic})` : ''}${penLookback} | -${c.penaltyScore ?? 5}分`
      }
      case 'PROGRESSIVE': {
        const pCnt = (c.conditions || []).length
        const pLogic = pCnt > 1 ? `(${c.conditionLogic === 'ALL' ? 'AND' : 'OR'})` : ''
        const ts = c.thresholds || []
        const thresholdStr = ts.map((t: any) => `${t.count}次→扣${t.penalty}`).join(', ') || '无阈值'
        return `${pCnt}项${pLogic} | ${c.lookbackDays ?? 30}天 | ${thresholdStr}`
      }
      case 'PROGRESSIVE_BONUS': {
        const pbCnt = (c.conditions || []).length
        const pbLogic = pbCnt > 1 ? `(${c.conditionLogic === 'ALL' ? 'AND' : 'OR'})` : ''
        const ts2 = c.thresholds || []
        const thresholdStr2 = ts2.map((t: any) => `${t.count}次→+${t.bonus}`).join(', ') || '无阈值'
        return `${pbCnt}项${pbLogic} | ${c.lookbackDays ?? 30}天 | ${thresholdStr2}`
      }
      case 'BONUS': {
        const logic2 = c.conditionLogic === 'ALL' ? 'AND' : 'OR'
        const hasCount2 = (c.conditions || []).some((x: any) => (x.minCount || 1) > 1)
        const lookback2 = hasCount2 ? ` | ${c.lookbackDays ?? 30}天` : ''
        return `${condCount}项${condCount > 1 ? `(${logic2})` : ''}${lookback2} | +${c.bonusScore ?? 5}分`
      }
      case 'CUSTOM': return `公式: ${c.formula ? c.formula.substring(0, 60) : '未设置'}`
      default: return configJson
    }
  } catch {
    return configJson
  }
}

function startEdit(rule: CalculationRule) {
  editingRule.value = rule
  form.value = {
    ruleCode: rule.ruleCode,
    ruleName: rule.ruleName,
    priority: rule.priority,
    ruleType: rule.ruleType,
    config: rule.config,
    isEnabled: rule.isEnabled,
  }
}

function closeDialog() {
  showAdd.value = false
  editingRule.value = null
  form.value = { ruleCode: '', ruleName: '', priority: 0, ruleType: 'VETO', config: '{"conditions":[]}', isEnabled: true }
}

function toggleEnabled(rule: CalculationRule) {
  emit('update', rule.id, {
    ruleName: rule.ruleName,
    priority: rule.priority,
    ruleType: rule.ruleType,
    config: rule.config,
    isEnabled: !rule.isEnabled,
  })
}

function handleSubmit() {
  if (editingRule.value) {
    emit('update', editingRule.value.id, {
      ruleName: form.value.ruleName,
      priority: form.value.priority,
      ruleType: form.value.ruleType,
      config: form.value.config,
      isEnabled: form.value.isEnabled,
    })
  } else {
    emit('create', {
      ruleCode: form.value.ruleCode,
      ruleName: form.value.ruleName,
      priority: form.value.priority,
      ruleType: form.value.ruleType,
      config: form.value.config,
      isEnabled: form.value.isEnabled,
    })
  }
  closeDialog()
}
</script>

<style scoped>
/* ========== Modal ========== */
.sp-mask { position:fixed; inset:0; z-index:1000; display:flex; align-items:center; justify-content:center; background:rgba(15,23,42,0.4); backdrop-filter:blur(2px); }
.sp-modal { width:600px; background:#fff; border-radius:14px; box-shadow:0 24px 64px rgba(0,0,0,0.18); overflow:hidden; display:flex; flex-direction:column; max-height:80vh; }
.sp-modal-head { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0; }
.sp-modal-head h3 { font-size:16px; font-weight:600; color:#1e2a3a; margin:0; }
.sp-modal-close { background:none; border:none; font-size:22px; color:#b8c0cc; cursor:pointer; padding:0 4px; line-height:1; }
.sp-modal-close:hover { color:#5a6474; }
.sp-modal-body { display:flex; flex-direction:column; gap:16px; padding:20px 24px; }
.sp-modal-scroll { flex:1; overflow-y:auto; }
.sp-modal-foot { display:flex; justify-content:flex-end; gap:10px; padding:0 24px 20px; }

/* ========== Animation ========== */
.sp-modal-enter-active { transition:all 0.2s ease-out; }
.sp-modal-leave-active { transition:all 0.15s ease-in; }
.sp-modal-enter-from { opacity:0; }
.sp-modal-enter-from .sp-modal { transform:translateY(12px) scale(0.97); }
.sp-modal-leave-to { opacity:0; }
.sp-modal-leave-to .sp-modal { transform:translateY(-8px) scale(0.98); }

/* ========== Buttons ========== */
.sp-btn-primary { display:inline-flex; align-items:center; gap:5px; padding:8px 16px; background:#1a6dff; color:#fff; border:none; border-radius:8px; font-size:13px; font-weight:500; cursor:pointer; transition:background 0.15s; white-space:nowrap; }
.sp-btn-primary:hover { background:#1558d6; }
.sp-btn-primary.sm { padding:6px 12px; font-size:12px; border-radius:6px; }
.sp-btn-ghost { padding:8px 16px; background:none; border:1px solid #dce1e8; border-radius:8px; font-size:13px; color:#5a6474; cursor:pointer; transition:background 0.15s; }
.sp-btn-ghost:hover { background:#f4f6f9; }
.sp-ic-s { background:none; border:none; padding:3px; color:#b8c0cc; cursor:pointer; border-radius:4px; display:flex; align-items:center; transition:all 0.12s; }
.sp-ic-s:hover { color:#1a6dff; }
.sp-ic-s.danger:hover { color:#d93025; }

/* ========== Form fields ========== */
.sp-fld label { display:block; font-size:12px; font-weight:500; color:#5a6474; margin-bottom:5px; }
.sp-fld input, .sp-fld select, .sp-fld textarea { width:100%; border:1px solid #dce1e8; border-radius:8px; padding:8px 12px; font-size:13px; outline:none; transition:border-color 0.2s, box-shadow 0.2s; color:#1e2a3a; background:#fff; }
.sp-fld input::placeholder, .sp-fld textarea::placeholder { color:#b8c0cc; }
.sp-fld input:focus, .sp-fld select:focus, .sp-fld textarea:focus { border-color:#7aadff; box-shadow:0 0 0 3px rgba(26,109,255,0.08); }
.sp-fld .help { font-size:11px; color:#8c95a3; margin-top:4px; }

/* ========== Section title / desc ========== */
.sp-section-title { font-size:14px; font-weight:600; color:#1e2a3a; margin:0; }
.sp-section-desc { font-size:12px; color:#8c95a3; }

/* ========== Empty state ========== */
.sp-empty { text-align:center; padding:32px 0; color:#b8c0cc; font-size:13px; }

/* ========== Rule card ========== */
.sp-rule-card { border:1px solid #e8ecf0; border-radius:10px; padding:12px; transition:all 0.15s; border-left:3px solid transparent; }
.sp-rule-card.is-enabled { background:#fff; }
.sp-rule-card.is-disabled { border-style:dashed; border-color:#dce1e8; background:#f8f9fb; opacity:0.6; }
.sp-rule-card:hover { border-left:3px solid #1a6dff; }

.sp-priority-badge { width:28px; height:28px; border-radius:50%; background:#f4f6f9; display:flex; align-items:center; justify-content:center; font-size:12px; font-family:monospace; color:#5a6474; flex-shrink:0; }
.sp-type-badge { font-size:11px; padding:2px 8px; border-radius:99px; color:#fff; flex-shrink:0; }
.sp-rule-name { font-size:13px; font-weight:500; color:#1e2a3a; }
.sp-rule-code { font-size:11px; color:#8c95a3; font-family:monospace; }
.sp-priority-label { font-size:11px; color:#8c95a3; }
.sp-priority-value { font-size:13px; font-family:monospace; color:#5a6474; }
.sp-rule-actions { display:flex; align-items:center; gap:2px; opacity:0; transition:opacity 0.15s; flex-shrink:0; }
.group:hover .sp-rule-actions { opacity:1; }

/* ========== Config summary ========== */
.sp-config-summary { margin-top:8px; font-size:12px; background:#f4f6f9; border-radius:8px; padding:8px 10px; color:#5a6474; font-family:monospace; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }

/* ========== Config section divider ========== */
.sp-config-section { border-top:1px solid #e8ecf0; padding-top:12px; }

/* ========== Checkbox label ========== */
.sp-checkbox-label { font-size:13px; color:#5a6474; }

</style>
