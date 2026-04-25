<template>
  <div class="crc-root">
    <div class="crc-header">
      <h3 class="sp-section-title">计算规则链</h3>
      <button class="sp-btn-primary sm" @click="showAdd = true">
        添加规则
      </button>
    </div>

    <p class="sp-section-desc">规则按优先级从低到高依次执行</p>

    <div v-if="rules.length === 0" class="sp-empty">
      暂无规则
    </div>

    <div v-else class="crc-rule-list">
      <div
        v-for="(rule, idx) in rules"
        :key="rule.id"
        class="sp-rule-card group"
        :class="rule.isEnabled ? 'is-enabled' : 'is-disabled'"
      >
        <div class="crc-rule-row">
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
          <div class="crc-rule-name-block">
            <div class="sp-rule-name crc-truncate">{{ rule.ruleName }}</div>
            <div class="sp-rule-code crc-truncate">{{ rule.ruleCode }}</div>
          </div>
          <!-- Priority -->
          <div class="crc-priority-col">
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
                class="crc-icon-md"
                :class="rule.isEnabled ? 'crc-green' : 'crc-gray'"
              />
            </button>
            <button class="sp-ic-s" @click="startEdit(rule)">
              <component :is="iconMap.Pencil" class="crc-icon-sm" />
            </button>
            <button class="sp-ic-s danger" @click="$emit('delete', rule.id)">
              <component :is="iconMap.Trash2" class="crc-icon-sm" />
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
              <div class="crc-form-row">
                <div class="sp-fld crc-w40" v-if="!editingRule">
                  <label>规则编码</label>
                  <input v-model="form.ruleCode" placeholder="ceiling_100" />
                </div>
                <div class="sp-fld crc-flex1">
                  <label>规则名称</label>
                  <input v-model="form.ruleName" placeholder="满分封顶" />
                </div>
              </div>
              <div class="crc-form-row">
                <div class="sp-fld crc-flex1">
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
                <div class="sp-fld crc-w24">
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
              <div class="crc-checkbox-row">
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
import { ref } from 'vue'
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
      scopeType: 'GLOBAL',
    })
  }
  closeDialog()
}
</script>

<style scoped>
/* Root layout */
.crc-root { display: flex; flex-direction: column; gap: 12px; }
.crc-header { display: flex; align-items: center; justify-content: space-between; }
.crc-rule-list { display: flex; flex-direction: column; gap: 8px; }

/* Rule row layout */
.crc-rule-row { display: flex; align-items: center; gap: 12px; }
.crc-rule-name-block { flex: 1; min-width: 0; }
.crc-truncate { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.crc-priority-col { text-align: right; flex-shrink: 0; }

/* Icon sizes */
.crc-icon-md { width: 16px; height: 16px; }
.crc-icon-sm { width: 14px; height: 14px; }
.crc-green { color: #22c55e; }
.crc-gray { color: #9ca3af; }

/* Form layout helpers */
.crc-form-row { display: flex; gap: 12px; }
.crc-flex1 { flex: 1; }
.crc-w40 { width: 160px; }
.crc-w24 { width: 96px; }
.crc-checkbox-row { display: flex; align-items: center; gap: 8px; }

/* Modal */
.sp-mask { position:fixed; inset:0; z-index:1000; display:flex; align-items:center; justify-content:center; background:rgba(15,23,42,0.35); backdrop-filter:blur(1px); }
.sp-modal { width:480px; background:#fff; border-radius:10px; box-shadow:0 16px 48px rgba(0,0,0,0.15); overflow:hidden; display:flex; flex-direction:column; max-height:80vh; }
.sp-modal-head { display:flex; align-items:center; justify-content:space-between; padding:10px 14px; border-bottom:1px solid #f0f1f3; }
.sp-modal-head h3 { font-size:13px; font-weight:600; color:#1e2a3a; margin:0; }
.sp-modal-close { background:none; border:none; font-size:18px; color:#9ca3af; cursor:pointer; line-height:1; }
.sp-modal-body { display:flex; flex-direction:column; gap:8px; padding:10px 14px; }
.sp-modal-scroll { flex:1; overflow-y:auto; }
.sp-modal-foot { display:flex; justify-content:flex-end; gap:6px; padding:6px 14px 10px; }

/* Animation */
.sp-modal-enter-active { transition:all 0.15s ease-out; }
.sp-modal-leave-active { transition:all 0.1s ease-in; }
.sp-modal-enter-from, .sp-modal-leave-to { opacity:0; }

/* Buttons */
.sp-btn-primary { padding:4px 10px; background:#1a6dff; color:#fff; border:none; border-radius:5px; font-size:11px; font-weight:500; cursor:pointer; }
.sp-btn-primary:hover { background:#1558d6; }
.sp-btn-primary.sm { padding:3px 8px; font-size:10px; }
.sp-btn-ghost { padding:4px 10px; background:none; border:1px solid #e5e7eb; border-radius:5px; font-size:11px; color:#6b7280; cursor:pointer; }
.sp-btn-ghost:hover { background:#f4f6f9; }
.sp-ic-s { background:none; border:none; padding:1px 3px; color:#b8c0cc; cursor:pointer; font-size:12px; border-radius:3px; display:inline-flex; align-items:center; }
.sp-ic-s:hover { color:#1a6dff; }
.sp-ic-s.danger:hover { color:#d93025; }

/* Form fields */
.sp-fld { display:flex; flex-direction:column; }
.sp-fld label { font-size:11px; font-weight:500; color:#6b7280; margin-bottom:2px; }
.sp-fld input, .sp-fld select, .sp-fld textarea { width:100%; border:1px solid #e5e7eb; border-radius:6px; padding:4px 8px; font-size:12px; outline:none; color:#111827; background:#fff; }
.sp-fld input:focus, .sp-fld select:focus, .sp-fld textarea:focus { border-color:#93c5fd; box-shadow:0 0 0 2px rgba(37,99,235,0.06); }
.sp-fld .help { font-size:10px; color:#8c95a3; margin-top:2px; }

/* Section */
.sp-section-title { font-size:11px; font-weight:600; color:#374151; margin:0; }
.sp-section-desc { font-size:10px; color:#9ca3af; margin:0; }
.sp-empty { text-align:center; padding:8px 0; color:#b8c0cc; font-size:11px; }

/* Rule card */
.sp-rule-card { border:1px solid #f0f1f3; border-radius:5px; padding:6px 8px; transition:all 0.1s; }
.sp-rule-card.is-enabled { background:#fff; }
.sp-rule-card.is-disabled { border-style:dashed; opacity:0.5; }
.sp-rule-card:hover { border-color:#dbeafe; }

.sp-priority-badge { width:20px; height:20px; border-radius:50%; background:#f4f6f9; display:flex; align-items:center; justify-content:center; font-size:10px; font-family:monospace; color:#5a6474; flex-shrink:0; }
.sp-type-badge { font-size:9px; padding:1px 6px; border-radius:99px; color:#fff; flex-shrink:0; }
.sp-rule-name { font-size:12px; font-weight:500; color:#1e2a3a; }
.sp-rule-code { font-size:10px; color:#8c95a3; font-family:monospace; }
.sp-priority-label { font-size:10px; color:#8c95a3; }
.sp-priority-value { font-size:11px; font-family:monospace; color:#5a6474; }
.sp-rule-actions { display:flex; align-items:center; gap:2px; opacity:0; transition:opacity 0.1s; flex-shrink:0; }
.group:hover .sp-rule-actions { opacity:1; }

.sp-config-summary { margin-top:4px; font-size:10px; background:#f8f9fb; border-radius:4px; padding:4px 6px; color:#5a6474; font-family:monospace; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.sp-config-section { border-top:1px solid #e8ecf0; padding-top:8px; }
.sp-checkbox-label { font-size:11px; color:#5a6474; }
</style>
