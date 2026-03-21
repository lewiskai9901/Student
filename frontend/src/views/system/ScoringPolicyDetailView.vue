<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus, Trash2, GripVertical, Save, X, Info } from 'lucide-vue-next'
import {
  getPolicy,
  updatePolicy,
  getGradeBands,
  createGradeBand,
  updateGradeBand,
  deleteGradeBand,
  getCalcRules,
  createCalcRule,
  updateCalcRule,
  deleteCalcRule,
} from '@/api/insp/scoringPolicy'
import type { ScoringPolicy, PolicyGradeBand, PolicyCalcRule } from '@/types/insp/evaluation'

const route = useRoute()
const router = useRouter()

const policyId = computed(() => Number(route.params.id))

// ==================== State ====================
const loading = ref(false)
const saving = ref(false)
const policy = ref<ScoringPolicy | null>(null)
const bands = ref<PolicyGradeBand[]>([])
const rules = ref<PolicyCalcRule[]>([])
const basicDirty = ref(false)

// ==================== Grade band colors ====================
const GRADE_COLORS: Record<string, { bg: string; text: string; gradient: string }> = {
  A: { bg: '#dcfce7', text: '#15803d', gradient: 'linear-gradient(135deg, #bbf7d0, #86efac)' },
  B: { bg: '#dbeafe', text: '#1d4ed8', gradient: 'linear-gradient(135deg, #bfdbfe, #93c5fd)' },
  C: { bg: '#fef9c3', text: '#a16207', gradient: 'linear-gradient(135deg, #fde68a, #fcd34d)' },
  D: { bg: '#ffedd5', text: '#c2410c', gradient: 'linear-gradient(135deg, #fed7aa, #fb923c)' },
  E: { bg: '#f3e8ff', text: '#7e22ce', gradient: 'linear-gradient(135deg, #e9d5ff, #c084fc)' },
  F: { bg: '#fee2e2', text: '#b91c1c', gradient: 'linear-gradient(135deg, #fecaca, #f87171)' },
  P: { bg: '#dcfce7', text: '#15803d', gradient: 'linear-gradient(135deg, #bbf7d0, #86efac)' },
  S: { bg: '#e0f2fe', text: '#0369a1', gradient: 'linear-gradient(135deg, #bae6fd, #7dd3fc)' },
}

function getBandGradient(gradeCode: string): string {
  const key = gradeCode.toUpperCase()
  return (GRADE_COLORS[key] || GRADE_COLORS['E']).gradient
}
function getBandTextColor(gradeCode: string): string {
  const key = gradeCode.toUpperCase()
  return (GRADE_COLORS[key] || GRADE_COLORS['E']).text
}

// ==================== Grade presets ====================
const PRESETS: Record<string, Array<{ gradeCode: string; gradeName: string; minScore: number; maxScore: number }>> = {
  FIVE: [
    { gradeCode: 'A', gradeName: '优秀', minScore: 90, maxScore: 100 },
    { gradeCode: 'B', gradeName: '良好', minScore: 80, maxScore: 89.99 },
    { gradeCode: 'C', gradeName: '中等', minScore: 70, maxScore: 79.99 },
    { gradeCode: 'D', gradeName: '及格', minScore: 60, maxScore: 69.99 },
    { gradeCode: 'F', gradeName: '不及格', minScore: 0, maxScore: 59.99 },
  ],
  FOUR: [
    { gradeCode: 'A', gradeName: '优秀', minScore: 90, maxScore: 100 },
    { gradeCode: 'B', gradeName: '良好', minScore: 75, maxScore: 89.99 },
    { gradeCode: 'C', gradeName: '及格', minScore: 60, maxScore: 74.99 },
    { gradeCode: 'D', gradeName: '不及格', minScore: 0, maxScore: 59.99 },
  ],
  PASS: [
    { gradeCode: 'P', gradeName: '通过', minScore: 60, maxScore: 100 },
    { gradeCode: 'F', gradeName: '不通过', minScore: 0, maxScore: 59.99 },
  ],
}

// ==================== Rule type config ====================
const RULE_TYPE_OPTIONS: { value: PolicyCalcRule['ruleType']; label: string; color: string }[] = [
  { value: 'VETO', label: '一票否决', color: '#ef4444' },
  { value: 'PENALTY', label: '扣分规则', color: '#f59e0b' },
  { value: 'BONUS', label: '加分规则', color: '#10b981' },
  { value: 'PROGRESSIVE', label: '递进规则', color: '#7c3aed' },
  { value: 'CUSTOM', label: '自定义', color: '#6b7685' },
]

function ruleTypeLabel(type: string) {
  return RULE_TYPE_OPTIONS.find(o => o.value === type)?.label || type
}
function ruleTypeColor(type: string) {
  return RULE_TYPE_OPTIONS.find(o => o.value === type)?.color || '#6b7685'
}

// ==================== Load ====================
async function loadData() {
  loading.value = true
  try {
    const [p, b, r] = await Promise.all([
      getPolicy(policyId.value),
      getGradeBands(policyId.value),
      getCalcRules(policyId.value),
    ])
    policy.value = p
    bands.value = b.sort((a, b) => a.sortOrder - b.sortOrder)
    rules.value = r.sort((a, b) => a.priority - b.priority)
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// ==================== Save basic ====================
async function saveBasic() {
  if (!policy.value) return
  saving.value = true
  try {
    const updated = await updatePolicy(policyId.value, {
      policyName: policy.value.policyName,
      description: policy.value.description,
      maxScore: policy.value.maxScore,
      minScore: policy.value.minScore,
      precisionDigits: policy.value.precisionDigits,
    })
    policy.value = updated
    basicDirty.value = false
    ElMessage.success('基础设置已保存')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ==================== Band CRUD ====================
const addingBand = ref(false)
const newBand = ref({ gradeCode: '', gradeName: '', minScore: 0, maxScore: 100 })

async function applyPreset(key: string) {
  if (!PRESETS[key]) return
  if (bands.value.length > 0) {
    try {
      await ElMessageBox.confirm('应用预设将清除当前所有等级，确认继续？', '应用预设', { type: 'warning' })
    } catch { return }
    // Delete all existing bands
    await Promise.all(bands.value.map(b => deleteGradeBand(policyId.value, b.id)))
    bands.value = []
  }
  for (let i = 0; i < PRESETS[key].length; i++) {
    const item = PRESETS[key][i]
    try {
      const created = await createGradeBand(policyId.value, { ...item, sortOrder: i })
      bands.value.push(created)
    } catch {}
  }
  ElMessage.success('预设已应用')
}

async function handleAddBand() {
  if (!newBand.value.gradeCode.trim()) { ElMessage.warning('请填写等级编码'); return }
  if (!newBand.value.gradeName.trim()) { ElMessage.warning('请填写等级名称'); return }
  try {
    const created = await createGradeBand(policyId.value, {
      ...newBand.value,
      gradeCode: newBand.value.gradeCode.trim().toUpperCase(),
      gradeName: newBand.value.gradeName.trim(),
      sortOrder: bands.value.length,
    })
    bands.value.push(created)
    newBand.value = { gradeCode: '', gradeName: '', minScore: 0, maxScore: 100 }
    addingBand.value = false
    ElMessage.success('等级已添加')
  } catch (e: any) {
    ElMessage.error(e.message || '添加失败')
  }
}

async function handleDeleteBand(band: PolicyGradeBand) {
  try {
    await ElMessageBox.confirm(`确认删除等级「${band.gradeName}」？`, '确认', { type: 'warning' })
    await deleteGradeBand(policyId.value, band.id)
    bands.value = bands.value.filter(b => b.id !== band.id)
    ElMessage.success('已删除')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

async function handleUpdateBandField(band: PolicyGradeBand, field: keyof PolicyGradeBand, value: any) {
  try {
    const updated = await updateGradeBand(policyId.value, band.id, { [field]: value })
    const idx = bands.value.findIndex(b => b.id === band.id)
    if (idx >= 0) bands.value[idx] = updated
  } catch {}
}

// ==================== Rule CRUD ====================
const addingRule = ref(false)
const newRule = ref<{ ruleName: string; ruleType: PolicyCalcRule['ruleType']; priority: number; config: string }>({
  ruleName: '',
  ruleType: 'VETO',
  priority: 1,
  config: '',
})

async function handleAddRule() {
  if (!newRule.value.ruleName.trim()) { ElMessage.warning('请填写规则名称'); return }
  try {
    const created = await createCalcRule(policyId.value, {
      ruleName: newRule.value.ruleName.trim(),
      ruleType: newRule.value.ruleType,
      priority: newRule.value.priority,
      config: newRule.value.config.trim() || null,
      isEnabled: true,
    })
    rules.value.push(created)
    rules.value.sort((a, b) => a.priority - b.priority)
    newRule.value = { ruleName: '', ruleType: 'VETO', priority: rules.value.length + 1, config: '' }
    addingRule.value = false
    ElMessage.success('规则已添加')
  } catch (e: any) {
    ElMessage.error(e.message || '添加失败')
  }
}

async function handleToggleRule(rule: PolicyCalcRule) {
  try {
    const updated = await updateCalcRule(policyId.value, rule.id, { isEnabled: !rule.isEnabled })
    const idx = rules.value.findIndex(r => r.id === rule.id)
    if (idx >= 0) rules.value[idx] = updated
  } catch {}
}

async function handleDeleteRule(rule: PolicyCalcRule) {
  try {
    await ElMessageBox.confirm(`确认删除规则「${rule.ruleName}」？`, '确认', { type: 'warning' })
    await deleteCalcRule(policyId.value, rule.id)
    rules.value = rules.value.filter(r => r.id !== rule.id)
    ElMessage.success('已删除')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

// ==================== Lifecycle ====================
onMounted(() => { loadData() })
</script>

<template>
  <div class="spd-root">
    <!-- Loading -->
    <div v-if="loading" class="spd-loading">
      <div class="spinner" />
      <span>加载中...</span>
    </div>

    <template v-else-if="policy">
      <!-- Header -->
      <header class="spd-header">
        <div class="spd-header-left">
          <button class="icon-btn" @click="router.push('/system/scoring-policies')">
            <ArrowLeft :size="16" />
          </button>
          <div class="spd-title-group">
            <h1 class="spd-title">{{ policy.policyName }}</h1>
            <span class="spd-code">{{ policy.policyCode }}</span>
          </div>
          <span v-if="policy.isSystem" class="badge-system">系统方案（只读）</span>
        </div>
        <div class="spd-header-right" v-if="!policy.isSystem">
          <button class="btn-primary" :disabled="!basicDirty || saving" @click="saveBasic">
            <Save :size="13" />
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </header>

      <!-- Body (single column, max-width) -->
      <div class="spd-body">

        <!-- ==================== 基础设置 ==================== -->
        <section class="spd-section">
          <h2 class="section-title">基础设置</h2>
          <div class="section-card">
            <div class="form-row">
              <div class="form-field" style="flex: 2">
                <label>方案名称</label>
                <input
                  v-model="policy.policyName"
                  class="form-input"
                  :disabled="policy.isSystem"
                  @input="basicDirty = true"
                  placeholder="方案名称"
                />
              </div>
              <div class="form-field" style="flex: 1.5">
                <label>方案编码</label>
                <input
                  :value="policy.policyCode"
                  class="form-input"
                  disabled
                />
              </div>
            </div>
            <div class="form-row">
              <div class="form-field form-field-narrow">
                <label>满分</label>
                <input
                  v-model.number="policy.maxScore"
                  type="number"
                  class="form-input"
                  :disabled="policy.isSystem"
                  @input="basicDirty = true"
                />
              </div>
              <div class="form-field form-field-narrow">
                <label>最低分</label>
                <input
                  v-model.number="policy.minScore"
                  type="number"
                  class="form-input"
                  :disabled="policy.isSystem"
                  @input="basicDirty = true"
                />
              </div>
              <div class="form-field form-field-narrow">
                <label>精度（小数位）</label>
                <input
                  v-model.number="policy.precisionDigits"
                  type="number"
                  min="0"
                  max="4"
                  class="form-input"
                  :disabled="policy.isSystem"
                  @input="basicDirty = true"
                />
              </div>
            </div>
            <div class="form-field">
              <label>描述</label>
              <input
                v-model="policy.description"
                class="form-input"
                :disabled="policy.isSystem"
                @input="basicDirty = true"
                placeholder="可选，如：≥90优秀 ≥80良好..."
              />
            </div>
          </div>
        </section>

        <!-- ==================== 等级映射 ==================== -->
        <section class="spd-section">
          <div class="section-title-row">
            <h2 class="section-title">等级映射</h2>
            <div v-if="!policy.isSystem" class="section-actions">
              <div class="preset-group">
                <span class="preset-label">快速预设:</span>
                <button class="preset-btn" @click="applyPreset('FIVE')">五级制</button>
                <button class="preset-btn" @click="applyPreset('FOUR')">四级制</button>
                <button class="preset-btn" @click="applyPreset('PASS')">通过/不通过</button>
              </div>
              <button class="btn-add" @click="addingBand = !addingBand">
                <Plus :size="13" />
                添加等级
              </button>
            </div>
          </div>

          <div class="section-card">
            <!-- Bands table -->
            <div v-if="bands.length > 0" class="bands-table">
              <div class="bt-head">
                <span></span>
                <span>等级编码</span>
                <span>等级名称</span>
                <span>最低分</span>
                <span>最高分</span>
                <span></span>
              </div>
              <div
                v-for="band in bands"
                :key="band.id"
                class="bt-row"
              >
                <div class="bt-drag">
                  <GripVertical :size="13" class="drag-icon" />
                </div>
                <div class="bt-grade">
                  <span class="grade-pill" :style="{ background: getBandGradient(band.gradeCode), color: getBandTextColor(band.gradeCode) }">
                    {{ band.gradeCode }}
                  </span>
                </div>
                <div>
                  <input
                    :value="band.gradeName"
                    class="bt-input"
                    :disabled="policy.isSystem"
                    @blur="(e) => handleUpdateBandField(band, 'gradeName', (e.target as HTMLInputElement).value)"
                  />
                </div>
                <div>
                  <input
                    :value="band.minScore"
                    type="number"
                    class="bt-input bt-input-num"
                    :disabled="policy.isSystem"
                    @blur="(e) => handleUpdateBandField(band, 'minScore', Number((e.target as HTMLInputElement).value))"
                  />
                </div>
                <div>
                  <input
                    :value="band.maxScore"
                    type="number"
                    class="bt-input bt-input-num"
                    :disabled="policy.isSystem"
                    @blur="(e) => handleUpdateBandField(band, 'maxScore', Number((e.target as HTMLInputElement).value))"
                  />
                </div>
                <div class="bt-actions">
                  <button
                    v-if="!policy.isSystem"
                    class="icon-btn-sm icon-btn-danger"
                    @click="handleDeleteBand(band)"
                  >
                    <Trash2 :size="12" />
                  </button>
                </div>
              </div>
            </div>
            <p v-else class="empty-hint">暂无等级定义，点击「添加等级」或选择快速预设</p>

            <!-- Add band inline form -->
            <div v-if="addingBand && !policy.isSystem" class="add-band-form">
              <div class="add-band-row">
                <input
                  v-model="newBand.gradeCode"
                  class="bt-input"
                  placeholder="编码，如 A"
                  maxlength="5"
                  style="width: 80px"
                />
                <input
                  v-model="newBand.gradeName"
                  class="bt-input"
                  placeholder="名称，如 优秀"
                  style="flex: 1"
                />
                <input
                  v-model.number="newBand.minScore"
                  type="number"
                  class="bt-input bt-input-num"
                  placeholder="最低"
                  style="width: 80px"
                />
                <input
                  v-model.number="newBand.maxScore"
                  type="number"
                  class="bt-input bt-input-num"
                  placeholder="最高"
                  style="width: 80px"
                />
                <button class="btn-primary btn-sm" @click="handleAddBand">确认</button>
                <button class="btn-ghost btn-sm" @click="addingBand = false">取消</button>
              </div>
            </div>
          </div>
        </section>

        <!-- ==================== 即时规则 ==================== -->
        <section class="spd-section">
          <div class="section-title-row">
            <h2 class="section-title">即时规则</h2>
            <div class="section-actions" v-if="!policy.isSystem">
              <button class="btn-add" @click="addingRule = !addingRule">
                <Plus :size="13" />
                添加规则
              </button>
            </div>
          </div>

          <div class="section-card">
            <div v-if="rules.length > 0" class="rules-list">
              <div
                v-for="(rule, idx) in rules"
                :key="rule.id"
                class="rule-row"
                :class="{ 'rule-disabled': !rule.isEnabled }"
              >
                <span class="rule-num">{{ idx + 1 }}</span>
                <span
                  class="rule-type-badge"
                  :style="{ background: ruleTypeColor(rule.ruleType) + '18', color: ruleTypeColor(rule.ruleType) }"
                >{{ ruleTypeLabel(rule.ruleType) }}</span>
                <span class="rule-name">{{ rule.ruleName }}</span>
                <span class="rule-code">{{ rule.ruleCode }}</span>
                <span class="rule-priority-badge">P{{ rule.priority }}</span>
                <div class="rule-actions">
                  <button
                    class="toggle-btn"
                    :class="{ on: rule.isEnabled }"
                    :disabled="policy.isSystem"
                    @click="handleToggleRule(rule)"
                    :title="rule.isEnabled ? '点击停用' : '点击启用'"
                  >
                    <span class="toggle-dot" :class="{ on: rule.isEnabled }" />
                    {{ rule.isEnabled ? '启用' : '停用' }}
                  </button>
                  <button
                    v-if="!policy.isSystem"
                    class="icon-btn-sm icon-btn-danger"
                    @click="handleDeleteRule(rule)"
                  >
                    <Trash2 :size="12" />
                  </button>
                </div>
              </div>
            </div>
            <p v-else class="empty-hint">暂无即时规则。即时规则在评分计算时自动触发，如一票否决、额外加分等。</p>

            <!-- Add rule inline form -->
            <div v-if="addingRule && !policy.isSystem" class="add-rule-form">
              <div class="add-rule-row">
                <input
                  v-model="newRule.ruleName"
                  class="bt-input"
                  placeholder="规则名称"
                  style="flex: 1"
                />
                <select v-model="newRule.ruleType" class="bt-select">
                  <option v-for="opt in RULE_TYPE_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
                <input
                  v-model.number="newRule.priority"
                  type="number"
                  class="bt-input bt-input-num"
                  placeholder="优先级"
                  style="width: 80px"
                />
                <button class="btn-primary btn-sm" @click="handleAddRule">确认</button>
                <button class="btn-ghost btn-sm" @click="addingRule = false">取消</button>
              </div>
              <div class="add-rule-config">
                <input
                  v-model="newRule.config"
                  class="bt-input"
                  placeholder="规则配置（JSON，可选）"
                  style="width: 100%"
                />
                <div class="config-hint">
                  <Info :size="11" />
                  配置参数因规则类型而异，如不填则使用默认配置
                </div>
              </div>
            </div>
          </div>
        </section>

      </div>
    </template>

    <!-- Error state -->
    <div v-else class="spd-error">
      <p>加载失败</p>
      <button class="btn-primary" @click="loadData">重试</button>
    </div>
  </div>
</template>

<style scoped>
/* ==================== Root ==================== */
.spd-root {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ==================== Loading / Error ==================== */
.spd-loading, .spd-error {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex: 1;
  font-size: 13px;
  color: #b8c0cc;
  flex-direction: column;
}
.spinner {
  width: 22px;
  height: 22px;
  border: 2px solid #e8ecf0;
  border-top-color: #1a6dff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ==================== Header ==================== */
.spd-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
}
.spd-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.spd-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: none;
  border: 1px solid #e8ecf0;
  border-radius: 8px;
  color: #8c95a3;
  cursor: pointer;
  transition: all 0.15s;
}
.icon-btn:hover { background: #f4f6f9; color: #1a6dff; border-color: #c5d8ff; }
.spd-title-group {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.spd-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0;
}
.spd-code {
  font-size: 11px;
  font-family: 'SFMono-Regular', Consolas, monospace;
  color: #8c95a3;
  background: #f4f6f9;
  padding: 2px 7px;
  border-radius: 4px;
}
.badge-system {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 4px;
  background: #f3e8ff;
  color: #7c3aed;
}

/* ==================== Body ==================== */
.spd-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 860px;
}

/* ==================== Section ==================== */
.spd-section {}
.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #5a6474;
  margin: 0 0 8px;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  flex-wrap: wrap;
  gap: 8px;
}
.section-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.section-card {
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 10px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* ==================== Form ==================== */
.form-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.form-row .form-field { flex: 1; min-width: 120px; }
.form-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.form-field-narrow { max-width: 130px; }
.form-field label {
  font-size: 12px;
  font-weight: 500;
  color: #5a6474;
}
.form-input {
  width: 100%;
  box-sizing: border-box;
  height: 34px;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  padding: 0 12px;
  font-size: 13px;
  outline: none;
  color: #1e2a3a;
  background: #fff;
  transition: border-color 0.2s, box-shadow 0.2s;
  font-family: inherit;
}
.form-input::placeholder { color: #b8c0cc; }
.form-input:focus { border-color: #7aadff; box-shadow: 0 0 0 3px rgba(26,109,255,0.08); }
.form-input:disabled { background: #f4f6f9; color: #8c95a3; cursor: default; }

/* ==================== Grade bands table ==================== */
.bands-table {
  display: flex;
  flex-direction: column;
  border: 1px solid #e8ecf0;
  border-radius: 8px;
  overflow: hidden;
}
.bt-head {
  display: grid;
  grid-template-columns: 30px 80px 1fr 90px 90px 36px;
  gap: 8px;
  padding: 8px 12px;
  background: #f4f6f9;
  font-size: 11px;
  font-weight: 600;
  color: #5a6474;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  align-items: center;
}
.bt-row {
  display: grid;
  grid-template-columns: 30px 80px 1fr 90px 90px 36px;
  gap: 8px;
  padding: 7px 12px;
  border-top: 1px solid #f0f2f5;
  align-items: center;
  transition: background 0.1s;
}
.bt-row:hover { background: #fafbfc; }
.bt-drag { display: flex; align-items: center; justify-content: center; }
.drag-icon { color: #d1d5db; cursor: grab; }
.drag-icon:hover { color: #9ca3af; }
.bt-grade { display: flex; align-items: center; }
.grade-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 24px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.bt-input {
  width: 100%;
  box-sizing: border-box;
  height: 28px;
  border: 1px solid transparent;
  border-radius: 5px;
  padding: 0 8px;
  font-size: 13px;
  outline: none;
  color: #1e2a3a;
  background: transparent;
  transition: border-color 0.15s, background 0.15s;
  font-family: inherit;
}
.bt-input:focus {
  border-color: #93c5fd;
  background: #fff;
  box-shadow: 0 0 0 2px rgba(37,99,235,0.06);
}
.bt-input:disabled { color: #8c95a3; cursor: default; }
.bt-input-num { text-align: right; font-family: 'SFMono-Regular', Consolas, monospace; font-size: 12px; }
.bt-select {
  height: 28px;
  border: 1px solid #dce1e8;
  border-radius: 5px;
  padding: 0 6px;
  font-size: 12px;
  outline: none;
  color: #1e2a3a;
  background: #fff;
  cursor: pointer;
}
.bt-actions { display: flex; align-items: center; justify-content: flex-end; }

/* ==================== Add band form ==================== */
.add-band-form {
  border-top: 1px dashed #e8ecf0;
  padding-top: 10px;
}
.add-band-row, .add-rule-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

/* ==================== Rules list ==================== */
.rules-list { display: flex; flex-direction: column; gap: 6px; }
.rule-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #fafbfc;
  border: 1px solid #e8ecf0;
  border-radius: 8px;
  font-size: 12px;
  transition: opacity 0.15s, background 0.1s;
}
.rule-row:hover { background: #f4f6f9; }
.rule-row.rule-disabled { opacity: 0.45; }
.rule-num {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e8ecf0;
  border-radius: 50%;
  font-size: 10px;
  font-weight: 700;
  color: #5a6474;
  flex-shrink: 0;
}
.rule-type-badge {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 7px;
  border-radius: 4px;
  white-space: nowrap;
  flex-shrink: 0;
}
.rule-name { flex: 1; font-weight: 500; color: #1e2a3a; min-width: 0; }
.rule-code {
  font-size: 10px;
  font-family: 'SFMono-Regular', Consolas, monospace;
  color: #b8c0cc;
  background: #f4f6f9;
  padding: 1px 5px;
  border-radius: 3px;
  flex-shrink: 0;
}
.rule-priority-badge {
  font-size: 10px;
  font-weight: 700;
  color: #8c95a3;
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 4px;
  flex-shrink: 0;
}
.rule-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

/* Toggle btn */
.toggle-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 8px;
  border: 1px solid #e8ecf0;
  border-radius: 6px;
  font-size: 11px;
  color: #8c95a3;
  background: #fff;
  cursor: pointer;
  transition: all 0.15s;
}
.toggle-btn:hover { border-color: #b8c0cc; }
.toggle-btn.on { border-color: #6ee7b7; color: #059669; background: #f0fdf4; }
.toggle-btn:disabled { cursor: default; opacity: 0.6; }
.toggle-dot {
  display: block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d1d5db;
  transition: background 0.15s;
}
.toggle-dot.on { background: #10b981; }

/* Add rule form */
.add-rule-form {
  border-top: 1px dashed #e8ecf0;
  padding-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.add-rule-config {}
.config-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  color: #b8c0cc;
  margin-top: 4px;
}

/* Preset group */
.preset-group {
  display: flex;
  align-items: center;
  gap: 4px;
}
.preset-label { font-size: 11px; color: #8c95a3; }
.preset-btn {
  padding: 3px 10px;
  border: 1px solid #dce1e8;
  border-radius: 5px;
  font-size: 11px;
  color: #5a6474;
  background: #fff;
  cursor: pointer;
  transition: all 0.15s;
}
.preset-btn:hover { border-color: #1a6dff; color: #1a6dff; background: #f0f6ff; }

/* ==================== Icon buttons ==================== */
.icon-btn-sm {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  background: none;
  border: none;
  border-radius: 5px;
  color: #b8c0cc;
  cursor: pointer;
  transition: all 0.12s;
}
.icon-btn-sm:hover { background: #f0f2f5; color: #1a6dff; }
.icon-btn-danger:hover { background: #fef2f2; color: #ef4444; }

/* ==================== Empty hint ==================== */
.empty-hint {
  font-size: 12px;
  color: #b8c0cc;
  margin: 4px 0 0;
  font-style: italic;
}

/* ==================== Buttons ==================== */
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 16px;
  background: #1a6dff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
  white-space: nowrap;
}
.btn-primary:hover:not(:disabled) { background: #1558d6; transform: translateY(-1px); }
.btn-primary:disabled { opacity: 0.45; cursor: not-allowed; }
.btn-primary.btn-sm { padding: 5px 12px; font-size: 12px; border-radius: 6px; }
.btn-ghost {
  padding: 8px 16px;
  background: none;
  border: 1px solid #dce1e8;
  border-radius: 8px;
  font-size: 13px;
  color: #5a6474;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-ghost.btn-sm { padding: 5px 12px; font-size: 12px; border-radius: 6px; }
.btn-ghost:hover { background: #f4f6f9; }
.btn-add {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  background: #fff;
  border: 1px solid #dce1e8;
  border-radius: 7px;
  font-size: 12px;
  color: #5a6474;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-add:hover { border-color: #1a6dff; color: #1a6dff; background: #f0f6ff; }
</style>
