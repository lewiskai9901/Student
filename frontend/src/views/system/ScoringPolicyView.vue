<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { BarChart2, Plus, Search, Copy, ChevronDown, ChevronUp, X, Settings2 } from 'lucide-vue-next'
import {
  listPolicies,
  getGradeBands,
  getCalcRules,
  createPolicy,
  updatePolicy,
  deletePolicy,
} from '@/api/insp/scoringPolicy'
import type { ScoringPolicy, PolicyGradeBand, PolicyCalcRule } from '@/types/insp/evaluation'

const router = useRouter()

// ==================== State ====================
const loading = ref(false)
const allPolicies = ref<ScoringPolicy[]>([])
const searchKeyword = ref('')
const activeFilter = ref<'ALL' | 'SYSTEM' | 'CUSTOM'>('ALL')
const expandedIds = ref<Set<number>>(new Set())
const detailCache = ref<Map<number, { bands: PolicyGradeBand[]; rules: PolicyCalcRule[] }>>(new Map())
const detailLoading = ref<Set<number>>(new Set())

// ==================== Computed ====================
const filteredPolicies = computed(() => {
  let list = allPolicies.value
  if (activeFilter.value === 'SYSTEM') list = list.filter(p => p.isSystem)
  if (activeFilter.value === 'CUSTOM') list = list.filter(p => !p.isSystem)
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    list = list.filter(p =>
      p.policyName.toLowerCase().includes(kw) ||
      p.policyCode.toLowerCase().includes(kw)
    )
  }
  return list
})

const totalCount = computed(() => allPolicies.value.length)
const systemCount = computed(() => allPolicies.value.filter(p => p.isSystem).length)
const customCount = computed(() => allPolicies.value.filter(p => !p.isSystem).length)

// ==================== Load ====================
async function loadPolicies() {
  loading.value = true
  try {
    allPolicies.value = await listPolicies()
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadDetail(policyId: number) {
  if (detailCache.value.has(policyId)) return
  detailLoading.value.add(policyId)
  try {
    const [bands, rules] = await Promise.all([
      getGradeBands(policyId),
      getCalcRules(policyId),
    ])
    detailCache.value.set(policyId, { bands, rules })
  } catch {
    detailCache.value.set(policyId, { bands: [], rules: [] })
  } finally {
    detailLoading.value.delete(policyId)
  }
}

function toggleExpand(id: number) {
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
    loadDetail(id)
  }
}

// ==================== Grade band display ====================
const GRADE_COLORS: Record<string, { bg: string; text: string; gradient: string }> = {
  A: { bg: '#dcfce7', text: '#15803d', gradient: 'linear-gradient(135deg, #bbf7d0, #86efac)' },
  B: { bg: '#dbeafe', text: '#1d4ed8', gradient: 'linear-gradient(135deg, #bfdbfe, #93c5fd)' },
  C: { bg: '#fef9c3', text: '#a16207', gradient: 'linear-gradient(135deg, #fde68a, #fcd34d)' },
  D: { bg: '#ffedd5', text: '#c2410c', gradient: 'linear-gradient(135deg, #fed7aa, #fb923c)' },
  F: { bg: '#fee2e2', text: '#b91c1c', gradient: 'linear-gradient(135deg, #fecaca, #f87171)' },
  P: { bg: '#dcfce7', text: '#15803d', gradient: 'linear-gradient(135deg, #bbf7d0, #86efac)' },
  E: { bg: '#f3e8ff', text: '#7e22ce', gradient: 'linear-gradient(135deg, #e9d5ff, #c084fc)' },
}

function getBandStyle(gradeCode: string): { background: string; color: string } {
  const cfg = GRADE_COLORS[gradeCode.toUpperCase()] || GRADE_COLORS['E']
  return { background: cfg.gradient, color: cfg.text }
}

const RULE_TYPE_LABELS: Record<string, { label: string; color: string }> = {
  VETO: { label: '一票否决', color: '#ef4444' },
  PENALTY: { label: '扣分', color: '#f59e0b' },
  BONUS: { label: '加分', color: '#10b981' },
  PROGRESSIVE: { label: '递进', color: '#7c3aed' },
  CUSTOM: { label: '自定义', color: '#6b7685' },
}

// ==================== Dialog ====================
const dialogVisible = ref(false)
const maskMouseDownTarget = ref<EventTarget | null>(null)
const isEdit = ref(false)
const editingId = ref<number | null>(null)

const form = ref({
  policyCode: '',
  policyName: '',
  description: '',
  maxScore: 100,
  minScore: 0,
  precisionDigits: 2,
  sortOrder: 0,
})

function openCreate() {
  isEdit.value = false
  editingId.value = null
  Object.assign(form.value, {
    policyCode: '',
    policyName: '',
    description: '',
    maxScore: 100,
    minScore: 0,
    precisionDigits: 2,
    sortOrder: 0,
  })
  dialogVisible.value = true
}

function openEdit(policy: ScoringPolicy) {
  if (policy.isSystem) return
  isEdit.value = true
  editingId.value = policy.id
  Object.assign(form.value, {
    policyCode: policy.policyCode,
    policyName: policy.policyName,
    description: policy.description || '',
    maxScore: policy.maxScore,
    minScore: policy.minScore,
    precisionDigits: policy.precisionDigits,
    sortOrder: policy.sortOrder,
  })
  dialogVisible.value = true
}

async function openCopy(policy: ScoringPolicy) {
  Object.assign(form.value, {
    policyCode: policy.policyCode + '_COPY',
    policyName: policy.policyName + '（副本）',
    description: policy.description || '',
    maxScore: policy.maxScore,
    minScore: policy.minScore,
    precisionDigits: policy.precisionDigits,
    sortOrder: 0,
  })
  isEdit.value = false
  editingId.value = null
  dialogVisible.value = true
}

function closeDialog() { dialogVisible.value = false }
function onMaskMouseDown(e: MouseEvent) { maskMouseDownTarget.value = e.target }
function onMaskClick(e: MouseEvent) {
  if (e.target === e.currentTarget && maskMouseDownTarget.value === e.currentTarget) closeDialog()
  maskMouseDownTarget.value = null
}

async function handleSave() {
  if (!form.value.policyCode.trim()) { ElMessage.warning('请填写方案编码'); return }
  if (!form.value.policyName.trim()) { ElMessage.warning('请填写方案名称'); return }
  try {
    const payload: Partial<ScoringPolicy> = {
      policyCode: form.value.policyCode.trim().toUpperCase(),
      policyName: form.value.policyName.trim(),
      description: form.value.description.trim() || null,
      maxScore: form.value.maxScore,
      minScore: form.value.minScore,
      precisionDigits: form.value.precisionDigits,
      sortOrder: form.value.sortOrder,
    }
    if (isEdit.value && editingId.value !== null) {
      await updatePolicy(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createPolicy(payload)
      ElMessage.success('创建成功')
    }
    closeDialog()
    detailCache.value.clear()
    await loadPolicies()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

// ==================== Delete ====================
async function handleDelete(policy: ScoringPolicy) {
  if (policy.isSystem) return
  try {
    await ElMessageBox.confirm(
      `确认删除评分方案「${policy.policyName}」？此操作不可恢复。`,
      '确认删除',
      { type: 'warning' }
    )
    await deletePolicy(policy.id)
    ElMessage.success('已删除')
    detailCache.value.delete(policy.id)
    expandedIds.value.delete(policy.id)
    await loadPolicies()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

// ==================== Navigate to detail ====================
function goDetail(policy: ScoringPolicy) {
  router.push(`/system/scoring-policies/${policy.id}`)
}

// ==================== Lifecycle ====================
onMounted(() => { loadPolicies() })
</script>

<template>
  <div class="sp-root">
    <!-- Header -->
    <div class="sp-header">
      <div class="sp-header-left">
        <BarChart2 :size="18" class="sp-header-icon" />
        <h1 class="sp-title">评分方案管理</h1>
      </div>
      <button class="btn-primary" @click="openCreate">
        <Plus :size="14" />
        新建方案
      </button>
    </div>

    <!-- Stats bar -->
    <div class="sp-stats">
      <span class="stat-text">共 <strong>{{ totalCount }}</strong> 个方案</span>
      <span class="stat-sep">│</span>
      <span class="stat-text">系统内置 <strong class="text-system">{{ systemCount }}</strong></span>
      <span class="stat-sep">│</span>
      <span class="stat-text">自定义 <strong class="text-custom">{{ customCount }}</strong></span>
    </div>

    <!-- Toolbar -->
    <div class="sp-toolbar">
      <div class="sp-toolbar-left">
        <button
          class="filter-tab"
          :class="{ active: activeFilter === 'ALL' }"
          @click="activeFilter = 'ALL'"
        >全部 {{ totalCount }}</button>
        <button
          class="filter-tab"
          :class="{ active: activeFilter === 'SYSTEM' }"
          @click="activeFilter = 'SYSTEM'"
        >系统 {{ systemCount }}</button>
        <button
          class="filter-tab"
          :class="{ active: activeFilter === 'CUSTOM' }"
          @click="activeFilter = 'CUSTOM'"
        >自定义 {{ customCount }}</button>
      </div>
      <div class="search-wrap">
        <Search :size="13" class="search-icon" />
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索方案名称或编码..."
          class="search-input"
        />
      </div>
    </div>

    <!-- Content -->
    <div class="sp-content">
      <!-- Loading -->
      <div v-if="loading" class="state-loading">
        <div class="spinner" />
        <span>加载中...</span>
      </div>

      <!-- Empty -->
      <div v-else-if="filteredPolicies.length === 0" class="state-empty">
        <BarChart2 :size="44" class="empty-icon" />
        <p class="empty-title">暂无评分方案</p>
        <p class="empty-sub">评分方案定义了评级档次与即时计算规则</p>
        <button class="btn-primary" @click="openCreate">
          <Plus :size="14" /> 创建第一个方案
        </button>
      </div>

      <!-- Policy cards -->
      <div v-else class="policy-list">
        <div
          v-for="policy in filteredPolicies"
          :key="policy.id"
          class="policy-card"
          :class="{ 'card-system': policy.isSystem }"
        >
          <!-- Card header -->
          <div class="card-head">
            <div class="card-head-left">
              <div class="card-name-row">
                <span class="card-name">{{ policy.policyName }}</span>
                <span v-if="policy.isSystem" class="badge badge-system">系统</span>
                <span v-else class="badge badge-custom">自定义</span>
                <span class="badge badge-enabled" :class="{ disabled: !policy.isEnabled }">
                  {{ policy.isEnabled ? '启用' : '停用' }}
                </span>
              </div>
              <div class="card-meta">
                <span class="card-code">{{ policy.policyCode }}</span>
                <span class="meta-sep">·</span>
                <span class="card-range">满分 {{ policy.maxScore }} / 最低 {{ policy.minScore }}</span>
                <span class="meta-sep">·</span>
                <span class="card-precision">精度 {{ policy.precisionDigits }} 位</span>
              </div>
              <p v-if="policy.description" class="card-desc">{{ policy.description }}</p>
            </div>
            <div class="card-head-actions">
              <button class="act-btn" title="详情编辑" @click="goDetail(policy)">
                <Settings2 :size="14" />
              </button>
              <button class="act-btn" title="复制方案" @click="openCopy(policy)">
                <Copy :size="14" />
              </button>
              <button
                v-if="!policy.isSystem"
                class="act-btn act-btn-danger"
                title="删除"
                @click="handleDelete(policy)"
              >
                <X :size="14" />
              </button>
              <button
                class="act-btn expand-btn"
                :title="expandedIds.has(policy.id) ? '收起' : '展开详情'"
                @click="toggleExpand(policy.id)"
              >
                <ChevronDown :size="14" class="chevron" :class="{ rotated: expandedIds.has(policy.id) }" />
              </button>
            </div>
          </div>

          <!-- Grade bands preview (always shown, compact) -->
          <div class="bands-preview">
            <div
              v-if="detailCache.has(policy.id) && detailCache.get(policy.id)!.bands.length"
              class="bands-row"
            >
              <div
                v-for="band in detailCache.get(policy.id)!.bands"
                :key="band.id"
                class="band-pill"
                :style="getBandStyle(band.gradeCode)"
                :title="`${band.gradeName}: ${band.minScore} ~ ${band.maxScore}`"
              >
                <span class="band-code">{{ band.gradeCode }}</span>
                <span class="band-score">≥{{ band.minScore }}</span>
              </div>
            </div>
            <div v-else-if="detailLoading.has(policy.id)" class="bands-loading">
              <div class="mini-spinner" />
            </div>
            <div v-else-if="!detailCache.has(policy.id)" class="bands-placeholder">
              <button class="load-bands-btn" @click="loadDetail(policy.id)">加载等级信息</button>
            </div>
            <div v-else class="bands-empty">暂无等级定义</div>
          </div>

          <!-- Expanded detail -->
          <Transition name="expand">
            <div v-if="expandedIds.has(policy.id)" class="card-detail">
              <div v-if="detailLoading.has(policy.id)" class="detail-loading">
                <div class="spinner" />
                <span>加载中...</span>
              </div>
              <template v-else-if="detailCache.has(policy.id)">
                <!-- Grade bands table -->
                <div class="detail-section">
                  <div class="detail-section-title">等级映射</div>
                  <div v-if="detailCache.get(policy.id)!.bands.length" class="bands-table">
                    <div class="bands-table-head">
                      <span>等级</span>
                      <span>名称</span>
                      <span>分数区间</span>
                    </div>
                    <div
                      v-for="band in detailCache.get(policy.id)!.bands"
                      :key="band.id"
                      class="bands-table-row"
                    >
                      <span>
                        <span class="band-tag" :style="getBandStyle(band.gradeCode)">{{ band.gradeCode }}</span>
                      </span>
                      <span class="band-name-text">{{ band.gradeName }}</span>
                      <span class="band-range-text">{{ band.minScore }} ~ {{ band.maxScore }}</span>
                    </div>
                  </div>
                  <p v-else class="detail-empty">暂无等级定义</p>
                </div>

                <!-- Calc rules -->
                <div class="detail-section">
                  <div class="detail-section-title">即时规则</div>
                  <div v-if="detailCache.get(policy.id)!.rules.length" class="rules-list">
                    <div
                      v-for="rule in detailCache.get(policy.id)!.rules"
                      :key="rule.id"
                      class="rule-item"
                      :class="{ 'rule-disabled': !rule.isEnabled }"
                    >
                      <span
                        class="rule-type-badge"
                        :style="{ background: (RULE_TYPE_LABELS[rule.ruleType]?.color || '#6b7685') + '18', color: RULE_TYPE_LABELS[rule.ruleType]?.color || '#6b7685' }"
                      >{{ RULE_TYPE_LABELS[rule.ruleType]?.label || rule.ruleType }}</span>
                      <span class="rule-name">{{ rule.ruleName }}</span>
                      <span class="rule-priority">优先级{{ rule.priority }}</span>
                      <span class="rule-status" :class="{ on: rule.isEnabled }">
                        {{ rule.isEnabled ? '启用' : '停用' }}
                      </span>
                    </div>
                  </div>
                  <p v-else class="detail-empty">暂无计算规则</p>
                </div>

                <!-- Actions -->
                <div class="detail-actions">
                  <button class="btn-outline" @click="goDetail(policy)">
                    <Settings2 :size="13" /> 编辑方案详情
                  </button>
                  <button v-if="!policy.isSystem" class="btn-outline btn-edit" @click="openEdit(policy)">
                    快速编辑基础设置
                  </button>
                </div>
              </template>
            </div>
          </Transition>
        </div>
      </div>
    </div>

    <!-- Create / Edit Dialog -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="dialogVisible"
          class="modal-mask"
          @mousedown="onMaskMouseDown"
          @click="onMaskClick"
        >
          <div class="modal-box">
            <div class="modal-head">
              <h3>{{ isEdit ? '编辑方案基础设置' : '新建评分方案' }}</h3>
              <button class="modal-close" @click="closeDialog"><X :size="16" /></button>
            </div>

            <div class="modal-body">
              <div class="fld-row">
                <div class="fld">
                  <label>方案编码 <span class="req">*</span></label>
                  <input
                    v-model="form.policyCode"
                    placeholder="如：STANDARD_FIVE"
                    class="fld-input"
                    :disabled="isEdit"
                  />
                </div>
                <div class="fld">
                  <label>方案名称 <span class="req">*</span></label>
                  <input
                    v-model="form.policyName"
                    placeholder="如：标准五级评分"
                    class="fld-input"
                  />
                </div>
              </div>

              <div class="fld-row">
                <div class="fld fld-narrow">
                  <label>满分</label>
                  <input v-model.number="form.maxScore" type="number" class="fld-input" />
                </div>
                <div class="fld fld-narrow">
                  <label>最低分</label>
                  <input v-model.number="form.minScore" type="number" class="fld-input" />
                </div>
                <div class="fld fld-narrow">
                  <label>精度（小数位）</label>
                  <input v-model.number="form.precisionDigits" type="number" min="0" max="4" class="fld-input" />
                </div>
                <div class="fld fld-narrow">
                  <label>排序</label>
                  <input v-model.number="form.sortOrder" type="number" class="fld-input" />
                </div>
              </div>

              <div class="fld">
                <label>描述（可选）</label>
                <input v-model="form.description" placeholder="如：≥90优秀 ≥80良好..." class="fld-input" />
              </div>
            </div>

            <div class="modal-foot">
              <button class="btn-ghost" @click="closeDialog">取消</button>
              <button class="btn-primary" @click="handleSave">
                {{ isEdit ? '保存修改' : '创建并配置' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
/* ==================== Root ==================== */
.sp-root {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ==================== Header ==================== */
.sp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px 14px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
}
.sp-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.sp-header-icon { color: #1a6dff; }
.sp-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0;
}

/* ==================== Stats ==================== */
.sp-stats {
  display: flex;
  align-items: center;
  padding: 8px 24px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
}
.stat-text {
  font-size: 13px;
  color: #5a6474;
  padding: 0 4px;
}
.stat-text strong { color: #1e2a3a; font-weight: 700; }
.text-system { color: #7c3aed !important; }
.text-custom { color: #10b981 !important; }
.stat-sep { color: #dce1e8; font-size: 14px; padding: 0 14px; user-select: none; }

/* ==================== Toolbar ==================== */
.sp-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 24px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
  gap: 12px;
  flex-wrap: wrap;
}
.sp-toolbar-left {
  display: flex;
  align-items: center;
  gap: 4px;
}
.filter-tab {
  padding: 5px 14px;
  font-size: 13px;
  font-weight: 500;
  color: #5a6474;
  background: none;
  border: 1px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}
.filter-tab:hover { background: #f4f6f9; color: #1e2a3a; }
.filter-tab.active { background: #e8f0ff; color: #1a6dff; border-color: #c5d8ff; }

.search-wrap { position: relative; }
.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #b8c0cc;
  pointer-events: none;
}
.search-input {
  height: 34px;
  width: 220px;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  padding: 0 12px 0 32px;
  font-size: 13px;
  outline: none;
  background: #fff;
  color: #3d4757;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.search-input::placeholder { color: #b8c0cc; }
.search-input:focus {
  border-color: #7aadff;
  box-shadow: 0 0 0 3px rgba(26, 109, 255, 0.08);
}

/* ==================== Content ==================== */
.sp-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

/* ==================== Policy list ==================== */
.policy-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 900px;
}

/* ==================== Policy card ==================== */
.policy-card {
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s, border-color 0.2s;
}
.policy-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border-color: #d0ddf7;
}
.card-system {
  border-left: 3px solid #7c3aed;
}

/* Card head */
.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 14px 16px 10px;
  gap: 12px;
}
.card-head-left { flex: 1; min-width: 0; }
.card-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}
.card-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e2a3a;
}
.card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.card-code {
  font-size: 11px;
  font-family: 'SFMono-Regular', Consolas, monospace;
  color: #8c95a3;
  background: #f4f6f9;
  padding: 1px 6px;
  border-radius: 4px;
}
.card-range, .card-precision {
  font-size: 12px;
  color: #8c95a3;
}
.meta-sep { color: #dce1e8; font-size: 12px; }
.card-desc {
  font-size: 12px;
  color: #8c95a3;
  margin: 4px 0 0;
  font-style: italic;
}

/* Card head actions */
.card-head-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}
.act-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  background: none;
  border: none;
  border-radius: 6px;
  color: #8c95a3;
  cursor: pointer;
  transition: all 0.12s;
}
.act-btn:hover { background: #f0f2f5; color: #1a6dff; }
.act-btn-danger:hover { background: #fef2f2; color: #ef4444; }
.expand-btn .chevron {
  transition: transform 0.2s;
}
.expand-btn .chevron.rotated { transform: rotate(180deg); }

/* ==================== Badges ==================== */
.badge {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
}
.badge-system { background: #f3e8ff; color: #7c3aed; }
.badge-custom { background: #ecfdf5; color: #059669; }
.badge-enabled { background: #ecfdf5; color: #059669; }
.badge-enabled.disabled { background: #f4f6f9; color: #b8c0cc; }

/* ==================== Bands preview ==================== */
.bands-preview {
  padding: 0 16px 12px;
  min-height: 32px;
}
.bands-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.band-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  transition: transform 0.12s, box-shadow 0.12s;
  cursor: default;
}
.band-pill:hover { transform: translateY(-1px); box-shadow: 0 3px 8px rgba(0,0,0,0.15); }
.band-code { font-size: 12px; font-weight: 700; }
.band-score { font-size: 10px; opacity: 0.8; }

.bands-loading {
  display: flex;
  align-items: center;
  padding: 4px 0;
}
.mini-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid #e8ecf0;
  border-top-color: #1a6dff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
.bands-placeholder { padding: 0; }
.load-bands-btn {
  font-size: 11px;
  color: #1a6dff;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  text-decoration: underline;
  opacity: 0.7;
}
.load-bands-btn:hover { opacity: 1; }
.bands-empty { font-size: 11px; color: #b8c0cc; }

/* ==================== Card detail (expanded) ==================== */
.card-detail {
  border-top: 1px solid #f0f2f5;
  background: #fafbfc;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.detail-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #b8c0cc;
  padding: 10px 0;
}
.detail-section {}
.detail-section-title {
  font-size: 11px;
  font-weight: 600;
  color: #5a6474;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}
.detail-empty { font-size: 12px; color: #b8c0cc; margin: 0; }

/* Bands table */
.bands-table {
  display: flex;
  flex-direction: column;
  border: 1px solid #e8ecf0;
  border-radius: 8px;
  overflow: hidden;
}
.bands-table-head, .bands-table-row {
  display: grid;
  grid-template-columns: 60px 1fr 1fr;
  gap: 8px;
  padding: 7px 12px;
  font-size: 12px;
  align-items: center;
}
.bands-table-head {
  background: #f4f6f9;
  font-weight: 600;
  color: #5a6474;
  font-size: 11px;
}
.bands-table-row {
  border-top: 1px solid #f0f2f5;
  color: #3d4757;
}
.bands-table-row:hover { background: #f7f8fb; }
.band-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 20px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
}
.band-name-text { font-weight: 500; color: #1e2a3a; }
.band-range-text { font-family: 'SFMono-Regular', Consolas, monospace; font-size: 11px; color: #8c95a3; }

/* Rules list */
.rules-list { display: flex; flex-direction: column; gap: 4px; }
.rule-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 6px;
  font-size: 12px;
  transition: opacity 0.12s;
}
.rule-item.rule-disabled { opacity: 0.5; }
.rule-type-badge {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
  flex-shrink: 0;
}
.rule-name { flex: 1; font-weight: 500; color: #1e2a3a; min-width: 0; }
.rule-priority { font-size: 11px; color: #b8c0cc; flex-shrink: 0; }
.rule-status { font-size: 10px; font-weight: 600; color: #b8c0cc; flex-shrink: 0; }
.rule-status.on { color: #10b981; }

/* Detail actions */
.detail-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* ==================== Expand transition ==================== */
.expand-enter-active { transition: all 0.2s ease-out; }
.expand-leave-active { transition: all 0.15s ease-in; }
.expand-enter-from, .expand-leave-to { opacity: 0; transform: translateY(-6px); }

/* ==================== State views ==================== */
.state-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 80px 0;
  color: #b8c0cc;
  font-size: 13px;
}
.state-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  gap: 10px;
}
.empty-icon { color: #dce1e8; }
.empty-title { font-size: 15px; font-weight: 600; color: #6b7685; margin: 0; }
.empty-sub { font-size: 12px; color: #b8c0cc; margin: 0; text-align: center; max-width: 320px; }

/* ==================== Spinner ==================== */
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
.btn-primary:hover { background: #1558d6; transform: translateY(-1px); }
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
.btn-ghost:hover { background: #f4f6f9; }
.btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  background: #fff;
  border: 1px solid #dce1e8;
  border-radius: 7px;
  font-size: 12px;
  color: #5a6474;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-outline:hover { border-color: #1a6dff; color: #1a6dff; background: #f0f6ff; }
.btn-outline.btn-edit:hover { border-color: #10b981; color: #10b981; background: #f0fdf4; }

/* ==================== Modal ==================== */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(2px);
}
.modal-box {
  width: 560px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}
.modal-head h3 { font-size: 16px; font-weight: 600; color: #1e2a3a; margin: 0; }
.modal-close {
  background: none;
  border: none;
  color: #b8c0cc;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  transition: color 0.12s;
}
.modal-close:hover { color: #5a6474; }
.modal-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px 24px;
}
.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 24px 20px;
}

/* ==================== Form ==================== */
.fld-row { display: flex; gap: 12px; flex-wrap: wrap; }
.fld-row .fld { flex: 1; min-width: 100px; }
.fld { display: flex; flex-direction: column; gap: 5px; }
.fld-narrow { max-width: 110px; }
.fld label { font-size: 12px; font-weight: 500; color: #5a6474; }
.fld-input {
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
.fld-input::placeholder { color: #b8c0cc; }
.fld-input:focus {
  border-color: #7aadff;
  box-shadow: 0 0 0 3px rgba(26, 109, 255, 0.08);
}
.fld-input:disabled { background: #f4f6f9; color: #8c95a3; cursor: not-allowed; }
.req { color: #d93025; }

/* Modal transition */
.modal-enter-active { transition: all 0.2s ease-out; }
.modal-leave-active { transition: all 0.15s ease-in; }
.modal-enter-from { opacity: 0; }
.modal-enter-from .modal-box { transform: translateY(12px) scale(0.97); }
.modal-leave-to { opacity: 0; }
.modal-leave-to .modal-box { transform: translateY(-8px) scale(0.98); }
</style>
