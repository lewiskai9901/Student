<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getCampaign,
  createCampaign,
  updateCampaign,
  getCampaignLevels,
  saveCampaignLevels,
  listBatches,
  listCampaigns,
} from '@/api/evalCenter'
import type { EvalCampaign, EvalLevel, EvalCondition, EvalBatch } from '@/types/evalCenter'
import {
  EvalTargetTypeConfig,
  EvalPeriodConfig,
  ConditionScopeConfig,
} from '@/types/evalCenter'
import ConditionEditor from './components/ConditionEditor.vue'

const route = useRoute()
const router = useRouter()

const isCreate = computed(() => route.path.includes('/create'))
const campaignId = computed(() => isCreate.value ? null : Number(route.params.id))

// ==================== State ====================
const loading = ref(false)
const saving = ref(false)
const saveMsg = ref('')

// Basic info form
const form = ref<Partial<EvalCampaign>>({
  campaignName: '',
  campaignDescription: '',
  targetType: 'ORG',
  evaluationPeriod: 'MONTHLY',
  status: 'DRAFT',
  scopeOrgIds: null,
  isAutoExecute: false,
})

// Levels
const levels = ref<EvalLevel[]>([])

// Batches (history)
const batches = ref<EvalBatch[]>([])

// All campaigns (for condition history refs)
const allCampaigns = ref<{ id: number; campaignName: string }[]>([])

// Condition editor
const condEditorVisible = ref(false)
const editingLevelIdx = ref<number | null>(null)
const editingConditionIdx = ref<number | null>(null)
const editingCondition = ref<EvalCondition | null>(null)

// ==================== Computed ====================
const campaignLevelsForEditor = computed(() =>
  levels.value.map(l => ({ levelNum: l.levelNum, levelName: l.levelName }))
)

const previewText = computed(() => {
  if (levels.value.length === 0) return '暂无级别定义'
  return levels.value.map(l => {
    const condDesc = l.conditions.map((c, i) => {
      const prefix = i > 0 ? (l.conditionLogic === 'AND' ? ' 且 ' : ' 或 ') : ''
      return prefix + (c.description || `条件${i + 1}`)
    }).join('')
    return `${l.levelName}：${condDesc || '（无条件）'}`
  }).join('\n')
})

// ==================== Load ====================
async function load() {
  if (isCreate.value) {
    // Default empty level
    levels.value = [{
      levelNum: 1,
      levelName: '一级',
      conditionLogic: 'AND',
      conditions: [],
    }]
    return
  }
  loading.value = true
  try {
    const [camp, lvls, batchList, campaigns] = await Promise.all([
      getCampaign(campaignId.value!),
      getCampaignLevels(campaignId.value!),
      listBatches(campaignId.value!),
      listCampaigns({ size: 50 }).then(r => r.records ?? []),
    ])
    form.value = {
      campaignName: camp.campaignName,
      campaignDescription: camp.campaignDescription,
      targetType: camp.targetType,
      evaluationPeriod: camp.evaluationPeriod,
      status: camp.status,
      scopeOrgIds: camp.scopeOrgIds,
      isAutoExecute: camp.isAutoExecute,
    }
    levels.value = lvls.map(l => ({
      ...l,
      conditions: l.conditions ?? [],
    }))
    batches.value = batchList
    allCampaigns.value = campaigns.map(c => ({ id: c.id!, campaignName: c.campaignName }))
  } catch (e: any) {
    alert(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)

// ==================== Form actions ====================
function goBack() {
  router.push('/eval-center')
}

async function handleSave() {
  if (!form.value.campaignName?.trim()) {
    alert('请输入活动名称')
    return
  }
  saving.value = true
  saveMsg.value = ''
  try {
    let id = campaignId.value
    if (isCreate.value) {
      const created = await createCampaign(form.value as any)
      id = created.id!
    } else {
      await updateCampaign(id!, form.value)
    }
    // Save levels
    await saveCampaignLevels(id!, levels.value)
    saveMsg.value = '已保存'
    if (isCreate.value) {
      router.replace(`/eval-center/campaigns/${id}`)
    }
    setTimeout(() => { saveMsg.value = '' }, 2000)
  } catch (e: any) {
    alert(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ==================== Level actions ====================
function addLevel() {
  const maxNum = levels.value.reduce((m, l) => Math.max(m, l.levelNum), 0)
  levels.value.push({
    levelNum: maxNum + 1,
    levelName: `${maxNum + 1}级`,
    conditionLogic: 'AND',
    conditions: [],
  })
}

function removeLevel(idx: number) {
  if (!confirm('确认删除此级别？')) return
  levels.value.splice(idx, 1)
  // Re-number
  levels.value.forEach((l, i) => { l.levelNum = i + 1 })
}

function moveLevelUp(idx: number) {
  if (idx === 0) return
  const tmp = levels.value[idx]
  levels.value[idx] = levels.value[idx - 1]
  levels.value[idx - 1] = tmp
  levels.value.forEach((l, i) => { l.levelNum = i + 1 })
}

function moveLevelDown(idx: number) {
  if (idx === levels.value.length - 1) return
  const tmp = levels.value[idx]
  levels.value[idx] = levels.value[idx + 1]
  levels.value[idx + 1] = tmp
  levels.value.forEach((l, i) => { l.levelNum = i + 1 })
}

// ==================== Condition actions ====================
function openAddCondition(levelIdx: number) {
  editingLevelIdx.value = levelIdx
  editingConditionIdx.value = null
  editingCondition.value = null
  condEditorVisible.value = true
}

function openEditCondition(levelIdx: number, condIdx: number) {
  editingLevelIdx.value = levelIdx
  editingConditionIdx.value = condIdx
  editingCondition.value = { ...levels.value[levelIdx].conditions[condIdx] }
  condEditorVisible.value = true
}

function removeCondition(levelIdx: number, condIdx: number) {
  levels.value[levelIdx].conditions.splice(condIdx, 1)
}

function onConditionSave(condition: EvalCondition) {
  const li = editingLevelIdx.value
  if (li === null) return
  if (editingConditionIdx.value !== null) {
    levels.value[li].conditions[editingConditionIdx.value] = condition
  } else {
    levels.value[li].conditions.push(condition)
  }
  condEditorVisible.value = false
}

// ==================== Helpers ====================
function formatDate(t?: string | null) {
  if (!t) return '-'
  return new Date(t).toLocaleDateString('zh-CN')
}

function parseSummary(summary?: string | null): string {
  if (!summary) return ''
  try {
    const obj = JSON.parse(summary)
    return Object.entries(obj)
      .map(([k, v]) => `${k}: ${v}`)
      .join(' | ')
  } catch { return summary }
}
</script>

<template>
  <div class="eced">
    <!-- Header -->
    <div class="eced-header">
      <div class="eced-header-left">
        <button class="back-btn" @click="goBack">← 返回</button>
        <h1 class="eced-title">{{ isCreate ? '新建评选活动' : (form.campaignName || '编辑评选活动') }}</h1>
      </div>
      <div class="eced-header-right">
        <span v-if="saveMsg" class="save-msg">{{ saveMsg }}</span>
        <button class="btn-ghost" @click="goBack">取消</button>
        <button class="btn-primary" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="state-loading">
      <div class="spinner" />
      <span>加载中...</span>
    </div>

    <!-- Two-column layout -->
    <div v-else class="eced-body">
      <!-- Left: config -->
      <div class="eced-left">
        <!-- Basic Info -->
        <div class="config-card">
          <div class="config-card-title">基本信息</div>
          <div class="config-card-body">
            <div class="fld">
              <label>活动名称 <span class="req">*</span></label>
              <input v-model="form.campaignName" type="text" placeholder="如：星级宿舍评选" />
            </div>
            <div class="fld">
              <label>活动描述</label>
              <textarea v-model="form.campaignDescription" rows="2" placeholder="可选描述" />
            </div>
            <div class="fld-row">
              <div class="fld">
                <label>目标类型 <span class="req">*</span></label>
                <select v-model="form.targetType">
                  <option v-for="(cfg, key) in EvalTargetTypeConfig" :key="key" :value="key">
                    {{ cfg.icon }} {{ cfg.label }}
                  </option>
                </select>
              </div>
              <div class="fld">
                <label>评选周期 <span class="req">*</span></label>
                <select v-model="form.evaluationPeriod">
                  <option v-for="(cfg, key) in EvalPeriodConfig" :key="key" :value="key">
                    {{ cfg.label }}
                  </option>
                </select>
              </div>
            </div>
            <div class="fld">
              <label>状态</label>
              <select v-model="form.status">
                <option value="DRAFT">草稿</option>
                <option value="ACTIVE">运行中</option>
                <option value="PAUSED">已暂停</option>
                <option value="ARCHIVED">已归档</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Level Definitions -->
        <div class="config-card">
          <div class="config-card-title">
            级别定义
            <span class="card-title-hint">从高到低依次判定，首个满足条件的级别即为结果</span>
          </div>
          <div class="config-card-body">
            <div
              v-for="(level, li) in levels"
              :key="li"
              class="level-block"
            >
              <!-- Level header -->
              <div class="level-head">
                <div class="level-num">第 {{ level.levelNum }} 级</div>
                <input
                  v-model="level.levelName"
                  type="text"
                  class="level-name-input"
                  placeholder="级别名称"
                />
                <select v-model="level.conditionLogic" class="logic-select" title="条件逻辑">
                  <option value="AND">全部满足（AND）</option>
                  <option value="OR">任一满足（OR）</option>
                </select>
                <div class="level-actions">
                  <button class="level-act" @click="moveLevelUp(li)" :disabled="li === 0" title="上移">↑</button>
                  <button class="level-act" @click="moveLevelDown(li)" :disabled="li === levels.length - 1" title="下移">↓</button>
                  <button class="level-act danger" @click="removeLevel(li)" title="删除">×</button>
                </div>
              </div>

              <!-- Conditions -->
              <div class="level-conditions">
                <div
                  v-for="(cond, ci) in level.conditions"
                  :key="ci"
                  class="condition-item"
                >
                  <div class="cond-logic-prefix" v-if="ci > 0">
                    {{ level.conditionLogic === 'AND' ? 'AND' : 'OR' }}
                  </div>
                  <div class="cond-desc" @click="openEditCondition(li, ci)">
                    {{ cond.description || `条件 ${ci + 1}` }}
                  </div>
                  <div class="cond-actions">
                    <button class="cond-act" @click="openEditCondition(li, ci)">编辑</button>
                    <button class="cond-act danger" @click="removeCondition(li, ci)">删除</button>
                  </div>
                </div>
                <div v-if="level.conditions.length === 0" class="level-empty">
                  暂无条件，点击下方添加
                </div>
                <button class="add-cond-btn" @click="openAddCondition(li)">
                  + 添加条件
                </button>
              </div>
            </div>

            <button class="add-level-btn" @click="addLevel">
              + 添加级别
            </button>
          </div>
        </div>
      </div>

      <!-- Right: preview + history -->
      <div class="eced-right">
        <!-- Preview -->
        <div class="preview-card">
          <div class="preview-card-title">级别预览</div>
          <div class="preview-card-body">
            <div
              v-for="(level, li) in levels"
              :key="li"
              class="preview-level"
            >
              <div class="preview-level-name">{{ level.levelNum }}. {{ level.levelName }}</div>
              <div class="preview-logic-badge">{{ level.conditionLogic === 'AND' ? '全部满足' : '任一满足' }}</div>
              <div v-if="level.conditions.length > 0" class="preview-conditions">
                <div
                  v-for="(cond, ci) in level.conditions"
                  :key="ci"
                  class="preview-cond"
                >
                  <span class="preview-cond-prefix">{{ ci > 0 ? (level.conditionLogic === 'AND' ? '且' : '或') : '条件' }}</span>
                  {{ cond.description || `条件${ci + 1}` }}
                </div>
              </div>
              <div v-else class="preview-no-cond">（暂无条件）</div>
            </div>
            <div v-if="levels.length === 0" class="preview-empty">请添加级别定义</div>
          </div>
        </div>

        <!-- Execution history -->
        <div v-if="!isCreate" class="history-card">
          <div class="history-card-title">执行历史</div>
          <div class="history-card-body">
            <div v-if="batches.length === 0" class="history-empty">暂无执行记录</div>
            <div
              v-else
              v-for="batch in batches"
              :key="batch.id"
              class="history-row"
              @click="router.push(`/eval-center/batches/${batch.id}`)"
            >
              <div class="history-cycle">{{ batch.cycleStart }} ~ {{ batch.cycleEnd }}</div>
              <div class="history-meta">
                <span class="history-targets">{{ batch.totalTargets }} 个目标</span>
                <span class="history-time">{{ formatDate(batch.executedAt) }}</span>
              </div>
              <div v-if="batch.summary" class="history-summary">
                {{ parseSummary(batch.summary) }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Condition Editor -->
    <ConditionEditor
      :visible="condEditorVisible"
      :initial="editingCondition"
      :campaign-levels="campaignLevelsForEditor"
      :campaign-list="allCampaigns"
      @save="onConditionSave"
      @close="condEditorVisible = false"
    />
  </div>
</template>

<style scoped>
/* ==================== Root ==================== */
.eced {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ==================== Header ==================== */
.eced-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
  gap: 16px;
}
.eced-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: none;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  font-size: 13px;
  color: #5a6474;
  cursor: pointer;
  white-space: nowrap;
  font-family: inherit;
  flex-shrink: 0;
}
.back-btn:hover { background: #f4f6f9; }
.eced-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e2a3a;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.eced-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.save-msg {
  font-size: 12px;
  color: #10b981;
  font-weight: 500;
}

/* ==================== Body ==================== */
.eced-body {
  flex: 1;
  overflow: hidden;
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 0;
}
.eced-left {
  overflow-y: auto;
  padding: 20px 16px 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.eced-right {
  overflow-y: auto;
  padding: 20px 24px 20px 8px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  border-left: 1px solid #e8ecf0;
  background: #fafbfc;
}

/* ==================== Config cards ==================== */
.config-card, .preview-card, .history-card {
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 10px;
  overflow: hidden;
}
.config-card-title, .preview-card-title, .history-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #1e2a3a;
  border-bottom: 1px solid #f0f2f5;
  background: #fafbfc;
}
.card-title-hint {
  font-size: 11px;
  font-weight: 400;
  color: #8c95a3;
}
.config-card-body, .preview-card-body, .history-card-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* ==================== Form fields ==================== */
.fld {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.fld label {
  font-size: 12px;
  font-weight: 500;
  color: #5a6474;
}
.fld input, .fld textarea, .fld select {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 13px;
  outline: none;
  color: #1e2a3a;
  background: #fff;
  transition: border-color 0.2s, box-shadow 0.2s;
  font-family: inherit;
  resize: vertical;
}
.fld input:focus, .fld textarea:focus, .fld select:focus {
  border-color: #7aadff;
  box-shadow: 0 0 0 3px rgba(26, 109, 255, 0.08);
}
.fld-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.req { color: #d93025; }

/* ==================== Level blocks ==================== */
.level-block {
  border: 1px solid #e8ecf0;
  border-radius: 8px;
  overflow: hidden;
}
.level-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #f8fafc;
  border-bottom: 1px solid #f0f2f5;
}
.level-num {
  font-size: 12px;
  font-weight: 700;
  color: #1a6dff;
  white-space: nowrap;
  background: #e8f0ff;
  padding: 3px 8px;
  border-radius: 4px;
}
.level-name-input {
  flex: 1;
  height: 30px;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  padding: 0 10px;
  font-size: 13px;
  font-weight: 500;
  outline: none;
  background: #fff;
  color: #1e2a3a;
  font-family: inherit;
}
.level-name-input:focus { border-color: #7aadff; }
.logic-select {
  height: 30px;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  padding: 0 8px;
  font-size: 12px;
  outline: none;
  background: #fff;
  color: #5a6474;
  cursor: pointer;
  font-family: inherit;
  white-space: nowrap;
}
.level-actions {
  display: flex;
  gap: 4px;
}
.level-act {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dce1e8;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
  color: #5a6474;
  font-family: inherit;
}
.level-act:hover { background: #f4f6f9; color: #1a6dff; border-color: #c8d4e3; }
.level-act:disabled { opacity: 0.3; cursor: not-allowed; }
.level-act.danger { color: #d93025; }
.level-act.danger:hover { background: #fef2f2; border-color: #fca5a5; }

.level-conditions {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.condition-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #f8fafc;
  border: 1px solid #f0f2f5;
  border-radius: 6px;
}
.cond-logic-prefix {
  font-size: 11px;
  font-weight: 700;
  color: #1a6dff;
  background: #e8f0ff;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
}
.cond-desc {
  flex: 1;
  font-size: 12px;
  color: #3d4757;
  cursor: pointer;
  line-height: 1.5;
}
.cond-desc:hover { color: #1a6dff; }
.cond-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}
.cond-act {
  padding: 3px 8px;
  border: none;
  border-radius: 4px;
  font-size: 11px;
  cursor: pointer;
  background: #e8ecf0;
  color: #5a6474;
  font-family: inherit;
}
.cond-act:hover { background: #d8e0ec; color: #1a6dff; }
.cond-act.danger { background: #fee2e2; color: #d93025; }
.cond-act.danger:hover { background: #fca5a5; }

.level-empty {
  font-size: 12px;
  color: #b8c0cc;
  text-align: center;
  padding: 8px 0;
}
.add-cond-btn {
  align-self: flex-start;
  padding: 5px 12px;
  border: 1px dashed #c8d4e3;
  border-radius: 6px;
  background: none;
  font-size: 12px;
  color: #1a6dff;
  cursor: pointer;
  font-family: inherit;
  margin-top: 4px;
}
.add-cond-btn:hover { background: #e8f0ff; }

.add-level-btn {
  align-self: flex-start;
  padding: 8px 16px;
  border: 1px dashed #c8d4e3;
  border-radius: 8px;
  background: none;
  font-size: 13px;
  color: #1a6dff;
  cursor: pointer;
  font-family: inherit;
  margin-top: 4px;
}
.add-level-btn:hover { background: #e8f0ff; }

/* ==================== Preview ==================== */
.preview-level {
  padding: 10px;
  border: 1px solid #f0f2f5;
  border-radius: 8px;
  background: #fafbfc;
}
.preview-level + .preview-level { margin-top: 8px; }
.preview-level-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e2a3a;
  margin-bottom: 4px;
}
.preview-logic-badge {
  display: inline-block;
  font-size: 10px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 10px;
  background: #e8f0ff;
  color: #1a6dff;
  margin-bottom: 6px;
}
.preview-conditions { display: flex; flex-direction: column; gap: 4px; }
.preview-cond {
  font-size: 12px;
  color: #5a6474;
  line-height: 1.5;
}
.preview-cond-prefix {
  font-size: 10px;
  font-weight: 700;
  color: #8c95a3;
  margin-right: 4px;
}
.preview-no-cond {
  font-size: 12px;
  color: #b8c0cc;
  font-style: italic;
}
.preview-empty {
  font-size: 12px;
  color: #b8c0cc;
  text-align: center;
  padding: 20px 0;
}

/* ==================== History ==================== */
.history-empty {
  font-size: 12px;
  color: #b8c0cc;
  text-align: center;
  padding: 20px 0;
}
.history-row {
  padding: 10px;
  border: 1px solid #f0f2f5;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.12s;
}
.history-row + .history-row { margin-top: 6px; }
.history-row:hover { background: #f8fafc; border-color: #c8d4e3; }
.history-cycle { font-size: 12px; font-weight: 600; color: #1e2a3a; }
.history-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 3px;
}
.history-targets { font-size: 11px; color: #5a6474; }
.history-time { font-size: 11px; color: #b8c0cc; }
.history-summary {
  font-size: 11px;
  color: #8c95a3;
  margin-top: 3px;
}

/* ==================== Loading ==================== */
.state-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 80px 0;
  color: #b8c0cc;
  font-size: 13px;
  flex: 1;
}
.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e8ecf0;
  border-top-color: #1a6dff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ==================== Buttons ==================== */
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 20px;
  background: #1a6dff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
  font-family: inherit;
}
.btn-primary:hover { background: #1558d6; }
.btn-primary:disabled { background: #93b8ff; cursor: not-allowed; }
.btn-ghost {
  padding: 8px 16px;
  background: none;
  border: 1px solid #dce1e8;
  border-radius: 8px;
  font-size: 13px;
  color: #5a6474;
  cursor: pointer;
  font-family: inherit;
}
.btn-ghost:hover { background: #f4f6f9; }
</style>
