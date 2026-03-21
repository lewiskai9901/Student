<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import type { EvaluationRule, EvaluationLevel, EvaluationCondition, ConditionType } from '@/types/insp/evaluation'
import { ConditionTypeConfig } from '@/types/insp/evaluation'
import {
  listRules, createRule, updateRule, deleteRule,
  getLevels, saveLevels, evaluate,
} from '@/api/insp/evaluationRule'

const props = defineProps<{ projectId: number }>()

// ─── State ───────────────────────────────────────────────────
const loading = ref(false)
const rules = ref<EvaluationRule[]>([])
const dialogOpen = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const evaluating = ref<number | null>(null)

// ─── Form ─────────────────────────────────────────────────────
interface LevelForm {
  levelNum: number
  levelName: string
  conditionLogic: 'AND' | 'OR'
  conditions: EvaluationCondition[]
}

const form = reactive<{
  ruleName: string
  targetType: string
  evaluationPeriod: string
  awardName: string
  rankingEnabled: boolean
  levels: LevelForm[]
}>({
  ruleName: '',
  targetType: 'PLACE',
  evaluationPeriod: 'MONTHLY',
  awardName: '',
  rankingEnabled: false,
  levels: [],
})

// ─── Options ──────────────────────────────────────────────────
const targetTypeOptions = [
  { value: 'PLACE', label: '场所' },
  { value: 'ORG', label: '组织' },
  { value: 'STUDENT', label: '学生' },
  { value: 'CLASS', label: '班级' },
]

const periodOptions = [
  { value: 'WEEKLY', label: '每周' },
  { value: 'MONTHLY', label: '每月' },
  { value: 'QUARTERLY', label: '每季度' },
  { value: 'SEMESTER', label: '每学期' },
  { value: 'YEARLY', label: '每年' },
]

const operatorOptions = [
  { value: '>=', label: '≥' },
  { value: '<=', label: '≤' },
  { value: '=', label: '=' },
  { value: '>', label: '>' },
  { value: '<', label: '<' },
  { value: '!=', label: '≠' },
]

// group condition types for dropdown
const conditionGroups = computed(() => {
  const groups: Record<string, { key: ConditionType; label: string }[]> = {}
  for (const [key, cfg] of Object.entries(ConditionTypeConfig) as [ConditionType, { label: string; group: string }][]) {
    if (!groups[cfg.group]) groups[cfg.group] = []
    groups[cfg.group].push({ key, label: cfg.label })
  }
  return groups
})

// ─── Load ─────────────────────────────────────────────────────
async function loadRules() {
  loading.value = true
  try {
    const data = await listRules(props.projectId)
    // also load levels for each rule
    const withLevels = await Promise.all(
      data.map(async r => {
        try {
          r.levels = await getLevels(r.id)
        } catch {
          r.levels = []
        }
        return r
      })
    )
    rules.value = withLevels
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

onMounted(loadRules)

// ─── Helpers ──────────────────────────────────────────────────
function getTargetLabel(t: string) {
  return targetTypeOptions.find(o => o.value === t)?.label ?? t
}

function getPeriodLabel(p: string) {
  return periodOptions.find(o => o.value === p)?.label ?? p
}

function getConditionLabel(type: ConditionType) {
  return ConditionTypeConfig[type]?.label ?? type
}

function starString(total: number, filled: number) {
  return '★'.repeat(filled) + '☆'.repeat(total - filled)
}

function levelStars(rule: EvaluationRule, level: EvaluationLevel) {
  const total = rule.levels?.length ?? 0
  const filled = total - level.levelNum + 1
  return starString(total, Math.max(0, filled))
}

// ─── Dialog ───────────────────────────────────────────────────
function resetForm() {
  form.ruleName = ''
  form.targetType = 'PLACE'
  form.evaluationPeriod = 'MONTHLY'
  form.awardName = ''
  form.rankingEnabled = false
  form.levels = []
}

function openCreate() {
  resetForm()
  editingId.value = null
  dialogOpen.value = true
}

async function openEdit(rule: EvaluationRule) {
  resetForm()
  editingId.value = rule.id
  form.ruleName = rule.ruleName
  form.targetType = rule.targetType
  form.evaluationPeriod = rule.evaluationPeriod
  form.awardName = rule.awardName ?? ''
  form.rankingEnabled = rule.rankingEnabled
  // load full levels with conditions
  try {
    const levels = rule.levels && rule.levels.length > 0
      ? rule.levels
      : await getLevels(rule.id)
    form.levels = levels.map(l => ({
      levelNum: l.levelNum,
      levelName: l.levelName,
      conditionLogic: l.conditionLogic,
      conditions: l.conditions.map(c => ({ ...c })),
    }))
  } catch {
    form.levels = []
  }
  dialogOpen.value = true
}

// ─── Level editing ────────────────────────────────────────────
function addLevel() {
  form.levels.push({
    levelNum: form.levels.length + 1,
    levelName: '',
    conditionLogic: 'AND',
    conditions: [],
  })
}

function removeLevel(idx: number) {
  form.levels.splice(idx, 1)
  // renumber
  form.levels.forEach((l, i) => { l.levelNum = i + 1 })
}

function addCondition(level: LevelForm) {
  level.conditions.push({
    conditionType: 'SCORE_AVG',
    operator: '>=',
    threshold: 0,
    parameters: {},
    description: '',
  })
}

function removeCondition(level: LevelForm, idx: number) {
  level.conditions.splice(idx, 1)
}

// ─── Save ─────────────────────────────────────────────────────
async function handleSave() {
  if (!form.ruleName.trim()) return
  saving.value = true
  try {
    const payload: Partial<EvaluationRule> = {
      projectId: props.projectId,
      ruleName: form.ruleName,
      targetType: form.targetType,
      evaluationPeriod: form.evaluationPeriod,
      awardName: form.awardName || null,
      rankingEnabled: form.rankingEnabled,
    }
    let ruleId: number
    if (editingId.value) {
      await updateRule(editingId.value, payload)
      ruleId = editingId.value
    } else {
      const created = await createRule(payload)
      ruleId = created.id
    }
    // save levels
    const levelsPayload: EvaluationLevel[] = form.levels.map(l => ({
      levelNum: l.levelNum,
      levelName: l.levelName,
      levelIcon: null,
      levelColor: null,
      conditionLogic: l.conditionLogic,
      conditions: l.conditions,
    }))
    await saveLevels(ruleId, levelsPayload)
    dialogOpen.value = false
    loadRules()
  } catch {
    // ignore
  } finally {
    saving.value = false
  }
}

// ─── Delete ───────────────────────────────────────────────────
const confirmDeleteId = ref<number | null>(null)

function askDelete(rule: EvaluationRule) {
  confirmDeleteId.value = rule.id
}

async function confirmDelete() {
  if (!confirmDeleteId.value) return
  try {
    await deleteRule(confirmDeleteId.value)
    confirmDeleteId.value = null
    loadRules()
  } catch {
    // ignore
  }
}

// ─── Evaluate ────────────────────────────────────────────────
const evalDialogOpen = ref(false)
const evalRuleId = ref<number | null>(null)
const evalStart = ref('')
const evalEnd = ref('')
const evalResults = ref<any[]>([])
const evalLoading = ref(false)

function openEval(rule: EvaluationRule) {
  evalRuleId.value = rule.id
  // default to current month
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  evalStart.value = `${y}-${m}-01`
  const lastDay = new Date(y, now.getMonth() + 1, 0).getDate()
  evalEnd.value = `${y}-${m}-${lastDay}`
  evalResults.value = []
  evalDialogOpen.value = true
}

async function runEval() {
  if (!evalRuleId.value || !evalStart.value || !evalEnd.value) return
  evalLoading.value = true
  try {
    evalResults.value = await evaluate(evalRuleId.value, evalStart.value, evalEnd.value)
  } catch {
    // ignore
  } finally {
    evalLoading.value = false
  }
}
</script>

<template>
  <div class="erc">
    <!-- ── Header ── -->
    <div class="erc-header">
      <span class="erc-header-title">评选规则</span>
      <button class="erc-btn-primary" @click="openCreate">+ 新建规则</button>
    </div>

    <!-- ── Loading ── -->
    <div v-if="loading" class="erc-empty">加载中...</div>

    <!-- ── Empty ── -->
    <div v-else-if="rules.length === 0" class="erc-empty">
      <div class="erc-empty-icon">⭐</div>
      <div class="erc-empty-text">暂无评选规则</div>
      <div class="erc-empty-sub">点击「新建规则」开始配置评选标准</div>
    </div>

    <!-- ── Rule cards ── -->
    <div v-else class="erc-list">
      <div v-for="rule in rules" :key="rule.id" class="erc-card">
        <!-- card header -->
        <div class="erc-card-head">
          <span class="erc-card-icon">⭐</span>
          <span class="erc-card-name">{{ rule.ruleName }}</span>
          <div class="erc-card-meta">
            <span class="erc-meta-item">目标: {{ getTargetLabel(rule.targetType) }}</span>
            <span class="erc-meta-sep">·</span>
            <span class="erc-meta-item">周期: {{ getPeriodLabel(rule.evaluationPeriod) }}</span>
            <template v-if="rule.rankingEnabled">
              <span class="erc-meta-sep">·</span>
              <span class="erc-meta-item erc-meta-badge">排名: 开启</span>
            </template>
            <template v-if="rule.awardName">
              <span class="erc-meta-sep">·</span>
              <span class="erc-meta-item">奖项: {{ rule.awardName }}</span>
            </template>
          </div>
        </div>

        <!-- levels -->
        <div v-if="rule.levels && rule.levels.length > 0" class="erc-levels">
          <div v-for="level in rule.levels" :key="level.levelNum" class="erc-level">
            <div class="erc-level-head">
              <span class="erc-level-stars">{{ levelStars(rule, level) }}</span>
              <span class="erc-level-name">{{ level.levelName }}</span>
            </div>
            <div v-if="level.conditions.length > 0" class="erc-conditions">
              <div v-for="(cond, ci) in level.conditions" :key="ci" class="erc-cond">
                · {{ getConditionLabel(cond.conditionType) }} {{ cond.operator }} {{ cond.threshold }}
                <span v-if="cond.description" class="erc-cond-desc">{{ cond.description }}</span>
              </div>
              <div class="erc-cond-logic">逻辑: {{ level.conditionLogic === 'AND' ? '全部满足(AND)' : '任一满足(OR)' }}</div>
            </div>
            <div v-else class="erc-cond erc-cond--empty">无条件</div>
          </div>
        </div>
        <div v-else class="erc-levels-empty">尚未配置等级</div>

        <!-- card footer -->
        <div class="erc-card-foot">
          <span class="erc-last-eval">上次评定: --</span>
          <div class="erc-card-actions">
            <button class="erc-btn-ghost" @click="openEdit(rule)">编辑</button>
            <button class="erc-btn-ghost erc-btn-ghost--blue" @click="openEval(rule)">触发评选</button>
            <button class="erc-btn-ghost erc-btn-ghost--red" @click="askDelete(rule)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ════════════════════════════════════════════════════════
         Edit / Create Dialog
    ════════════════════════════════════════════════════════════ -->
    <Teleport to="body">
      <div v-if="dialogOpen" class="erc-overlay" @click.self="dialogOpen = false">
        <div class="erc-dialog">
          <!-- dialog header -->
          <div class="erc-dialog-head">
            <span class="erc-dialog-title">{{ editingId ? '编辑评选规则' : '新建评选规则' }}</span>
            <button class="erc-dialog-close" @click="dialogOpen = false">×</button>
          </div>

          <!-- dialog body -->
          <div class="erc-dialog-body">
            <!-- 规则名称 -->
            <div class="erc-field">
              <label class="erc-label">规则名称</label>
              <input
                v-model="form.ruleName"
                class="erc-input"
                placeholder="例如：星级宿舍"
                maxlength="50"
              />
            </div>

            <!-- 目标类型 / 周期 / 排名 -->
            <div class="erc-row3">
              <div class="erc-field">
                <label class="erc-label">目标类型</label>
                <select v-model="form.targetType" class="erc-select">
                  <option v-for="opt in targetTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </div>
              <div class="erc-field">
                <label class="erc-label">评选周期</label>
                <select v-model="form.evaluationPeriod" class="erc-select">
                  <option v-for="opt in periodOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </div>
              <div class="erc-field erc-field--check">
                <label class="erc-label">排名</label>
                <label class="erc-checkbox-label">
                  <input type="checkbox" v-model="form.rankingEnabled" class="erc-checkbox" />
                  <span>启用排名</span>
                </label>
              </div>
            </div>

            <!-- 奖项名称 -->
            <div class="erc-field">
              <label class="erc-label">奖项名称<span class="erc-label-hint">（选填）</span></label>
              <input
                v-model="form.awardName"
                class="erc-input"
                placeholder="例如：流动红旗"
                maxlength="50"
              />
            </div>

            <!-- 评选等级 -->
            <div class="erc-section-title">评选等级</div>

            <div v-if="form.levels.length === 0" class="erc-levels-hint">
              尚无等级，点击「添加等级」开始配置
            </div>

            <div v-for="(level, li) in form.levels" :key="li" class="erc-level-editor">
              <div class="erc-level-editor-head">
                <div class="erc-level-editor-left">
                  <span class="erc-level-num">等级{{ li + 1 }}</span>
                  <input
                    v-model="level.levelName"
                    class="erc-input erc-input--sm erc-input--inline"
                    placeholder="等级名称，如五星"
                    maxlength="20"
                  />
                </div>
                <button class="erc-btn-icon-del" @click="removeLevel(li)" title="删除等级">×</button>
              </div>

              <!-- conditions header -->
              <div class="erc-cond-header">
                <span class="erc-cond-label">条件</span>
                <select v-model="level.conditionLogic" class="erc-select erc-select--xs">
                  <option value="AND">全部满足(AND)</option>
                  <option value="OR">任一满足(OR)</option>
                </select>
              </div>

              <!-- condition rows -->
              <div v-for="(cond, ci) in level.conditions" :key="ci" class="erc-cond-row">
                <!-- type -->
                <select v-model="cond.conditionType" class="erc-select erc-select--cond">
                  <optgroup v-for="(items, group) in conditionGroups" :key="group" :label="group">
                    <option v-for="item in items" :key="item.key" :value="item.key">{{ item.label }}</option>
                  </optgroup>
                </select>
                <!-- operator -->
                <select v-model="cond.operator" class="erc-select erc-select--op">
                  <option v-for="op in operatorOptions" :key="op.value" :value="op.value">{{ op.label }}</option>
                </select>
                <!-- threshold -->
                <input
                  v-model.number="cond.threshold"
                  type="number"
                  class="erc-input erc-input--threshold"
                  placeholder="值"
                />
                <!-- description -->
                <input
                  v-model="cond.description"
                  class="erc-input erc-input--desc"
                  placeholder="备注（选填）"
                  maxlength="30"
                />
                <button class="erc-btn-icon-del" @click="removeCondition(level, ci)" title="删除条件">×</button>
              </div>

              <button class="erc-btn-text" @click="addCondition(level)">+ 添加条件</button>
            </div>

            <button class="erc-btn-dashed" @click="addLevel">+ 添加等级</button>
          </div>

          <!-- dialog footer -->
          <div class="erc-dialog-foot">
            <button class="erc-btn-cancel" @click="dialogOpen = false">取消</button>
            <button
              class="erc-btn-primary"
              :disabled="saving || !form.ruleName.trim()"
              @click="handleSave"
            >{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ════════════════════════════════════════════════════════
         Delete Confirm Dialog
    ════════════════════════════════════════════════════════════ -->
    <Teleport to="body">
      <div v-if="confirmDeleteId !== null" class="erc-overlay" @click.self="confirmDeleteId = null">
        <div class="erc-dialog erc-dialog--sm">
          <div class="erc-dialog-head">
            <span class="erc-dialog-title">确认删除</span>
            <button class="erc-dialog-close" @click="confirmDeleteId = null">×</button>
          </div>
          <div class="erc-dialog-body">
            <p class="erc-confirm-text">删除后不可恢复，确定要删除这条评选规则吗？</p>
          </div>
          <div class="erc-dialog-foot">
            <button class="erc-btn-cancel" @click="confirmDeleteId = null">取消</button>
            <button class="erc-btn-danger" @click="confirmDelete">确认删除</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ════════════════════════════════════════════════════════
         Evaluate Dialog
    ════════════════════════════════════════════════════════════ -->
    <Teleport to="body">
      <div v-if="evalDialogOpen" class="erc-overlay" @click.self="evalDialogOpen = false">
        <div class="erc-dialog erc-dialog--sm">
          <div class="erc-dialog-head">
            <span class="erc-dialog-title">触发评选</span>
            <button class="erc-dialog-close" @click="evalDialogOpen = false">×</button>
          </div>
          <div class="erc-dialog-body">
            <div class="erc-row2">
              <div class="erc-field">
                <label class="erc-label">周期开始</label>
                <input v-model="evalStart" type="date" class="erc-input" />
              </div>
              <div class="erc-field">
                <label class="erc-label">周期结束</label>
                <input v-model="evalEnd" type="date" class="erc-input" />
              </div>
            </div>
            <!-- results -->
            <div v-if="evalResults.length > 0" class="erc-eval-results">
              <div class="erc-section-title">评选结果 ({{ evalResults.length }} 条)</div>
              <div v-for="r in evalResults" :key="r.id" class="erc-eval-row">
                <span class="erc-eval-name">{{ r.targetName || r.targetId }}</span>
                <span class="erc-eval-level">{{ r.levelName || '未达标' }}</span>
                <span v-if="r.rankNo" class="erc-eval-rank">第{{ r.rankNo }}名</span>
              </div>
            </div>
          </div>
          <div class="erc-dialog-foot">
            <button class="erc-btn-cancel" @click="evalDialogOpen = false">关闭</button>
            <button
              class="erc-btn-primary"
              :disabled="evalLoading || !evalStart || !evalEnd"
              @click="runEval"
            >{{ evalLoading ? '评选中...' : '执行评选' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
/* ── Root ── */
.erc {
  font-size: 12px;
  color: #374151;
}

/* ── Header ── */
.erc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.erc-header-title {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

/* ── Buttons ── */
.erc-btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  background: #1a6dff;
  color: #fff;
  border: none;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.erc-btn-primary:hover:not(:disabled) { background: #1558d6; }
.erc-btn-primary:disabled { opacity: 0.55; cursor: not-allowed; }

.erc-btn-cancel {
  display: inline-flex;
  align-items: center;
  padding: 5px 12px;
  background: #f3f4f6;
  color: #374151;
  border: none;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}
.erc-btn-cancel:hover { background: #e5e7eb; }

.erc-btn-danger {
  display: inline-flex;
  align-items: center;
  padding: 5px 12px;
  background: #ef4444;
  color: #fff;
  border: none;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.erc-btn-danger:hover { background: #dc2626; }

.erc-btn-ghost {
  display: inline-flex;
  align-items: center;
  padding: 3px 9px;
  background: transparent;
  color: #6b7280;
  border: 1px solid #e5e7eb;
  border-radius: 5px;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.15s;
}
.erc-btn-ghost:hover { background: #f9fafb; color: #374151; }
.erc-btn-ghost--blue { color: #1a6dff; border-color: #bfdbfe; }
.erc-btn-ghost--blue:hover { background: #eff6ff; }
.erc-btn-ghost--red { color: #ef4444; border-color: #fecaca; }
.erc-btn-ghost--red:hover { background: #fef2f2; }

.erc-btn-text {
  display: inline-flex;
  align-items: center;
  padding: 3px 6px;
  background: transparent;
  color: #1a6dff;
  border: none;
  font-size: 11px;
  cursor: pointer;
  margin-top: 4px;
}
.erc-btn-text:hover { text-decoration: underline; }

.erc-btn-dashed {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 8px;
  background: transparent;
  color: #6b7280;
  border: 1px dashed #d1d5db;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  margin-top: 8px;
  transition: all 0.15s;
}
.erc-btn-dashed:hover { border-color: #1a6dff; color: #1a6dff; background: #eff6ff; }

.erc-btn-icon-del {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  padding: 0;
  background: transparent;
  color: #9ca3af;
  border: none;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  border-radius: 3px;
  flex-shrink: 0;
}
.erc-btn-icon-del:hover { color: #ef4444; background: #fef2f2; }

/* ── Empty ── */
.erc-empty {
  text-align: center;
  padding: 48px 0;
  color: #9ca3af;
}
.erc-empty-icon { font-size: 32px; margin-bottom: 10px; }
.erc-empty-text { font-size: 13px; font-weight: 500; color: #6b7280; margin-bottom: 4px; }
.erc-empty-sub { font-size: 11px; }

/* ── Rule Cards ── */
.erc-list { display: flex; flex-direction: column; gap: 10px; }

.erc-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  transition: border-color 0.15s;
}
.erc-card:hover { border-color: #d1d5db; }

.erc-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px 8px;
  border-bottom: 1px solid #f3f4f6;
  flex-wrap: wrap;
}
.erc-card-icon { font-size: 14px; }
.erc-card-name {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}
.erc-card-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  flex-wrap: wrap;
}
.erc-meta-item { font-size: 11px; color: #6b7280; }
.erc-meta-sep { font-size: 11px; color: #d1d5db; }
.erc-meta-badge {
  background: #dcfce7;
  color: #16a34a;
  padding: 1px 6px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
}

/* ── Levels ── */
.erc-levels {
  padding: 8px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.erc-levels-empty {
  padding: 8px 14px;
  font-size: 11px;
  color: #9ca3af;
  font-style: italic;
}

.erc-level {}
.erc-level-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}
.erc-level-stars {
  font-size: 12px;
  color: #f59e0b;
  letter-spacing: 1px;
}
.erc-level-name {
  font-size: 12px;
  font-weight: 600;
  color: #374151;
}

.erc-conditions {
  padding-left: 8px;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.erc-cond {
  font-size: 11px;
  color: #6b7280;
  line-height: 1.7;
}
.erc-cond--empty { font-style: italic; }
.erc-cond-desc {
  margin-left: 4px;
  color: #9ca3af;
}
.erc-cond-logic {
  font-size: 10px;
  color: #9ca3af;
  margin-top: 2px;
}

/* ── Card footer ── */
.erc-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 14px;
  background: #fafafa;
  border-top: 1px solid #f3f4f6;
}
.erc-last-eval { font-size: 11px; color: #9ca3af; }
.erc-card-actions { display: flex; gap: 6px; }

/* ── Dialog Overlay ── */
.erc-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
}

.erc-dialog {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.18);
  width: 100%;
  max-width: 600px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}
.erc-dialog--sm { max-width: 420px; }

.erc-dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px 12px;
  border-bottom: 1px solid #f3f4f6;
  flex-shrink: 0;
}
.erc-dialog-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}
.erc-dialog-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: transparent;
  border: none;
  font-size: 18px;
  color: #9ca3af;
  cursor: pointer;
  border-radius: 4px;
  line-height: 1;
}
.erc-dialog-close:hover { background: #f3f4f6; color: #374151; }

.erc-dialog-body {
  padding: 16px 18px;
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.erc-dialog-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 18px;
  border-top: 1px solid #f3f4f6;
  flex-shrink: 0;
}

/* ── Form Fields ── */
.erc-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.erc-field--check {
  justify-content: flex-start;
}

.erc-label {
  font-size: 12px;
  font-weight: 600;
  color: #374151;
}
.erc-label-hint {
  font-weight: 400;
  color: #9ca3af;
  margin-left: 3px;
}

.erc-input {
  height: 30px;
  padding: 0 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  color: #374151;
  outline: none;
  transition: border-color 0.15s;
  background: #fff;
}
.erc-input:focus { border-color: #1a6dff; }
.erc-input--sm { height: 26px; font-size: 12px; }
.erc-input--inline { flex: 1; min-width: 0; }
.erc-input--threshold { width: 70px; flex-shrink: 0; }
.erc-input--desc { flex: 1; min-width: 0; }

.erc-select {
  height: 30px;
  padding: 0 6px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  color: #374151;
  outline: none;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s;
}
.erc-select:focus { border-color: #1a6dff; }
.erc-select--xs { height: 24px; font-size: 11px; padding: 0 4px; }
.erc-select--cond { flex: 1.4; min-width: 0; }
.erc-select--op { width: 54px; flex-shrink: 0; }

.erc-checkbox-label {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #374151;
  cursor: pointer;
  height: 30px;
}
.erc-checkbox { width: 14px; height: 14px; cursor: pointer; accent-color: #1a6dff; }

.erc-row2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.erc-row3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
}

/* ── Level editor ── */
.erc-section-title {
  font-size: 12px;
  font-weight: 600;
  color: #374151;
  padding-bottom: 6px;
  border-bottom: 1px solid #f3f4f6;
}

.erc-levels-hint {
  font-size: 11px;
  color: #9ca3af;
  font-style: italic;
  padding: 4px 0;
}

.erc-level-editor {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.erc-level-editor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.erc-level-editor-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.erc-level-num {
  font-size: 11px;
  font-weight: 600;
  color: #6b7280;
  white-space: nowrap;
}

.erc-cond-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.erc-cond-label {
  font-size: 11px;
  color: #6b7280;
}

.erc-cond-row {
  display: flex;
  align-items: center;
  gap: 5px;
}

/* ── Confirm dialog ── */
.erc-confirm-text {
  font-size: 13px;
  color: #374151;
  line-height: 1.6;
  margin: 0;
}

/* ── Eval results ── */
.erc-eval-results { margin-top: 8px; }
.erc-eval-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 0;
  border-bottom: 1px solid #f3f4f6;
  font-size: 12px;
}
.erc-eval-name { flex: 1; color: #374151; }
.erc-eval-level {
  background: #fef9c3;
  color: #ca8a04;
  padding: 1px 7px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}
.erc-eval-rank { font-size: 11px; color: #9ca3af; }
</style>
