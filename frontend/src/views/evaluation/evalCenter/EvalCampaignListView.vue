<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  listCampaigns,
  deleteCampaign,
  executeCampaign,
  listBatches,
} from '@/api/evalCenter'
import type { EvalCampaign, EvalCampaignStatus, EvalBatch } from '@/types/evalCenter'
import {
  EvalCampaignStatusConfig,
  EvalTargetTypeConfig,
  EvalPeriodConfig,
} from '@/types/evalCenter'

const router = useRouter()

// ==================== State ====================
const loading = ref(false)
const campaigns = ref<EvalCampaign[]>([])
const total = ref(0)
const keyword = ref('')
const activeFilter = ref<EvalCampaignStatus | 'ALL'>('ALL')

// Execute dialog
const execDialogVisible = ref(false)
const execCampaignId = ref<number | null>(null)
const execForm = ref({ cycleStart: '', cycleEnd: '' })
const execLoading = ref(false)

// Dropdown
const openDropdownId = ref<number | null>(null)
const dropdownPos = ref({ top: '0px', right: '0px' })

// ==================== Stats ====================
const stats = computed(() => {
  const all = campaigns.value
  return {
    total: total.value,
    active: all.filter(c => c.status === 'ACTIVE').length,
    draft: all.filter(c => c.status === 'DRAFT').length,
    archived: all.filter(c => c.status === 'ARCHIVED' || c.status === 'PAUSED').length,
  }
})

const filteredCampaigns = computed(() => {
  let list = [...campaigns.value]
  if (activeFilter.value !== 'ALL') {
    list = list.filter(c => c.status === activeFilter.value)
  }
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(c => c.campaignName.toLowerCase().includes(kw))
  }
  return list
})

// ==================== Load ====================
async function loadCampaigns() {
  loading.value = true
  try {
    const res = await listCampaigns({ size: 100 })
    campaigns.value = res.records || []
    total.value = res.total || 0
  } catch (e: any) {
    console.error('加载失败', e)
  } finally {
    loading.value = false
  }
}

// ==================== Navigation ====================
function goCreate() {
  router.push('/eval-center/campaigns/create')
}

function goEdit(c: EvalCampaign) {
  router.push(`/eval-center/campaigns/${c.id}`)
}

function goResults(c: EvalCampaign) {
  // Navigate to latest batch if available
  router.push(`/eval-center/campaigns/${c.id}`)
}

// ==================== Execute ====================
function openExecDialog(c: EvalCampaign) {
  execCampaignId.value = c.id ?? null
  // Default: current month
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const lastDay = new Date(y, now.getMonth() + 1, 0).getDate()
  execForm.value = {
    cycleStart: `${y}-${m}-01`,
    cycleEnd: `${y}-${m}-${String(lastDay).padStart(2, '0')}`,
  }
  execDialogVisible.value = true
  closeDropdowns()
}

async function handleExecute() {
  if (!execCampaignId.value) return
  if (!execForm.value.cycleStart || !execForm.value.cycleEnd) {
    alert('请填写周期日期')
    return
  }
  execLoading.value = true
  try {
    const batch = await executeCampaign(execCampaignId.value, {
      cycleStart: execForm.value.cycleStart,
      cycleEnd: execForm.value.cycleEnd,
    })
    execDialogVisible.value = false
    // Navigate to result
    if (batch && batch.id) {
      router.push(`/eval-center/batches/${batch.id}`)
    }
  } catch (e: any) {
    alert(e?.message || '执行失败')
  } finally {
    execLoading.value = false
  }
}

// ==================== Delete ====================
async function handleDelete(c: EvalCampaign) {
  if (!confirm(`确认删除「${c.campaignName}」？此操作不可恢复。`)) return
  try {
    await deleteCampaign(c.id!)
    await loadCampaigns()
  } catch (e: any) {
    alert(e?.message || '删除失败')
  }
  closeDropdowns()
}

// ==================== Dropdown ====================
function toggleDropdown(id: number, event?: MouseEvent) {
  event?.stopPropagation()
  if (openDropdownId.value === id) { openDropdownId.value = null; return }
  openDropdownId.value = id
  if (event) {
    const btn = event.currentTarget as HTMLElement
    const rect = btn.getBoundingClientRect()
    dropdownPos.value = {
      top: `${rect.bottom + 4}px`,
      right: `${window.innerWidth - rect.right}px`,
    }
  }
}
function closeDropdowns() { openDropdownId.value = null }

function onDocClick() { openDropdownId.value = null }
onMounted(() => {
  document.addEventListener('click', onDocClick)
  loadCampaigns()
})
onUnmounted(() => document.removeEventListener('click', onDocClick))

// ==================== Helpers ====================
function getTargetIcon(targetType: string): string {
  return EvalTargetTypeConfig[targetType as keyof typeof EvalTargetTypeConfig]?.icon ?? '🎯'
}

function getTargetLabel(targetType: string): string {
  return EvalTargetTypeConfig[targetType as keyof typeof EvalTargetTypeConfig]?.label ?? targetType
}

function getPeriodLabel(period: string): string {
  return EvalPeriodConfig[period as keyof typeof EvalPeriodConfig]?.label ?? period
}

function getStatusConfig(status: string) {
  return EvalCampaignStatusConfig[status as EvalCampaignStatus] ?? { label: status, color: '#94a3b8' }
}

function getLevelCount(c: EvalCampaign): number {
  return c.levels?.length ?? 0
}

function getConditionCount(c: EvalCampaign): number {
  return c.levels?.reduce((sum, l) => sum + (l.conditions?.length ?? 0), 0) ?? 0
}

function formatDate(t?: string | null) {
  if (!t) return '-'
  const d = new Date(t)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const maskMouseDownTarget = ref<EventTarget | null>(null)
function onMaskMouseDown(e: MouseEvent) { maskMouseDownTarget.value = e.target }
function onMaskClick(e: MouseEvent, closeFn: () => void) {
  if (e.target === e.currentTarget && maskMouseDownTarget.value === e.currentTarget) closeFn()
  maskMouseDownTarget.value = null
}
</script>

<template>
  <div class="ec" @click="closeDropdowns">
    <!-- Header -->
    <div class="ec-header">
      <div class="ec-header-left">
        <span class="ec-header-icon">⭐</span>
        <h1 class="ec-title">评级中心</h1>
      </div>
      <button class="btn-primary" @click="goCreate">
        <span class="btn-icon">+</span>
        新建评选
      </button>
    </div>

    <!-- Stats bar -->
    <div class="ec-stats">
      <button
        class="stat-item" :class="{ active: activeFilter === 'ALL' }"
        @click="activeFilter = 'ALL'"
      >
        <span class="stat-label">全部</span>
        <span class="stat-value">{{ stats.total }}</span>
      </button>
      <span class="stat-sep">│</span>
      <button
        class="stat-item" :class="{ active: activeFilter === 'ACTIVE' }"
        @click="activeFilter = 'ACTIVE'"
      >
        <span class="stat-label">运行中</span>
        <span class="stat-value active-val">{{ stats.active }}</span>
      </button>
      <span class="stat-sep">│</span>
      <button
        class="stat-item" :class="{ active: activeFilter === 'DRAFT' }"
        @click="activeFilter = 'DRAFT'"
      >
        <span class="stat-label">草稿</span>
        <span class="stat-value">{{ stats.draft }}</span>
      </button>
      <span class="stat-sep">│</span>
      <button
        class="stat-item" :class="{ active: activeFilter === 'ARCHIVED' }"
        @click="activeFilter = 'ARCHIVED'"
      >
        <span class="stat-label">已结束</span>
        <span class="stat-value">{{ stats.archived }}</span>
      </button>
    </div>

    <!-- Toolbar -->
    <div class="ec-toolbar">
      <div class="search-wrap">
        <span class="search-icon">🔍</span>
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索活动名称..."
          class="search-input"
        />
      </div>
      <span class="ec-total">{{ filteredCampaigns.length }} 个活动</span>
    </div>

    <!-- Content -->
    <div class="ec-content">
      <!-- Loading -->
      <div v-if="loading" class="state-loading">
        <div class="spinner" />
        <span>加载中...</span>
      </div>

      <!-- Empty -->
      <div v-else-if="filteredCampaigns.length === 0" class="state-empty">
        <div class="empty-icon">⭐</div>
        <p class="empty-title">{{ activeFilter === 'ALL' ? '暂无评选活动' : '暂无符合条件的活动' }}</p>
        <p class="empty-sub">创建评选活动，自动评定目标等级</p>
        <button v-if="activeFilter === 'ALL'" class="btn-primary" @click="goCreate">
          创建第一个评选活动
        </button>
      </div>

      <!-- Campaign list -->
      <div v-else class="campaign-list">
        <div
          v-for="c in filteredCampaigns"
          :key="c.id"
          class="campaign-card"
          :class="'st-' + c.status.toLowerCase()"
        >
          <!-- Card main -->
          <div class="card-main" @click="goEdit(c)">
            <!-- Icon area -->
            <div class="card-icon" :class="'icon-' + c.targetType.toLowerCase()">
              {{ getTargetIcon(c.targetType) }}
            </div>

            <!-- Info area -->
            <div class="card-info">
              <div class="card-top">
                <span class="card-name">{{ c.campaignName }}</span>
                <span class="status-badge" :style="{ background: getStatusConfig(c.status).color + '20', color: getStatusConfig(c.status).color }">
                  {{ getStatusConfig(c.status).label }}
                </span>
                <span class="period-badge">{{ getPeriodLabel(c.evaluationPeriod) }}</span>
              </div>

              <div class="card-meta">
                <span class="meta-item">
                  <span class="meta-label">目标类型</span>
                  <span class="meta-val">{{ getTargetLabel(c.targetType) }}</span>
                </span>
                <span class="meta-sep">·</span>
                <span class="meta-item">
                  <span class="meta-label">级别数</span>
                  <span class="meta-val">{{ getLevelCount(c) }}</span>
                </span>
                <span class="meta-sep">·</span>
                <span class="meta-item">
                  <span class="meta-label">条件数</span>
                  <span class="meta-val">{{ getConditionCount(c) }}</span>
                </span>
                <template v-if="c.lastExecutedAt">
                  <span class="meta-sep">·</span>
                  <span class="meta-item">
                    <span class="meta-label">上次执行</span>
                    <span class="meta-val">{{ formatDate(c.lastExecutedAt) }}</span>
                  </span>
                </template>
              </div>

              <div v-if="c.campaignDescription" class="card-desc">
                {{ c.campaignDescription }}
              </div>
            </div>
          </div>

          <!-- Card actions -->
          <div class="card-footer" @click.stop>
            <div class="card-actions">
              <button class="act-btn" @click.stop="goResults(c)" title="查看结果">
                查看结果
              </button>
              <button
                class="act-btn act-btn-primary"
                @click.stop="openExecDialog(c)"
                title="立即执行"
              >
                立即执行
              </button>
              <button class="act-btn" @click.stop="goEdit(c)" title="编辑">
                编辑
              </button>
              <button
                class="act-btn act-btn-icon"
                @click.stop="toggleDropdown(Number(c.id), $event)"
                title="更多"
              >
                ···
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Dropdown -->
    <Teleport to="body">
      <Transition name="dropdown">
        <div
          v-if="openDropdownId !== null"
          class="dropdown-menu"
          :style="{ top: dropdownPos.top, right: dropdownPos.right }"
          @click.stop
        >
          <template v-for="c in filteredCampaigns" :key="c.id">
            <template v-if="Number(c.id) === openDropdownId">
              <button class="danger" @click.stop="handleDelete(c)">
                🗑️ 删除
              </button>
            </template>
          </template>
        </div>
      </Transition>
    </Teleport>

    <!-- Execute Dialog -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="execDialogVisible"
          class="modal-mask"
          @mousedown="onMaskMouseDown"
          @click="onMaskClick($event, () => execDialogVisible = false)"
        >
          <div class="modal-box">
            <div class="modal-head">
              <h3>立即执行评选</h3>
              <button class="modal-close" @click="execDialogVisible = false">&times;</button>
            </div>
            <div class="modal-body">
              <div class="fld">
                <label>周期开始日期 <span class="req">*</span></label>
                <input v-model="execForm.cycleStart" type="date" />
              </div>
              <div class="fld">
                <label>周期结束日期 <span class="req">*</span></label>
                <input v-model="execForm.cycleEnd" type="date" />
              </div>
              <p class="exec-note">系统将查询该时间范围内的数据并计算各目标的评级结果。</p>
            </div>
            <div class="modal-foot">
              <button class="btn-ghost" @click="execDialogVisible = false">取消</button>
              <button class="btn-primary" :disabled="execLoading" @click="handleExecute">
                {{ execLoading ? '执行中...' : '开始执行' }}
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
.ec {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ==================== Header ==================== */
.ec-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px 14px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
}
.ec-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ec-header-icon {
  font-size: 20px;
}
.ec-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e2a3a;
  margin: 0;
}

/* ==================== Stats bar ==================== */
.ec-stats {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 10px 24px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
}
.stat-sep {
  color: #dce1e8;
  font-size: 14px;
  padding: 0 16px;
  user-select: none;
}
.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}
.stat-item:hover { background: #f4f6f9; }
.stat-item.active { background: #e8f0ff; }
.stat-label {
  font-size: 13px;
  color: #5a6474;
  font-weight: 500;
}
.stat-item.active .stat-label { color: #1a6dff; }
.stat-value {
  font-size: 13px;
  font-weight: 700;
  color: #1e2a3a;
}
.stat-value.active-val { color: #10b981; }
.stat-item.active .stat-value { color: #1a6dff; }

/* ==================== Toolbar ==================== */
.ec-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
}
.search-wrap {
  position: relative;
}
.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 13px;
  pointer-events: none;
  opacity: 0.5;
}
.search-input {
  height: 34px;
  width: 240px;
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
.ec-total {
  font-size: 12px;
  color: #b8c0cc;
}

/* ==================== Content ==================== */
.ec-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

/* ==================== Campaign list ==================== */
.campaign-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.campaign-card {
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 10px;
  border-left: 3px solid #dce1e8;
  overflow: hidden;
  transition: box-shadow 0.18s, border-color 0.18s;
}
.campaign-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border-color: #c8d4e3;
}

.campaign-card.st-active { border-left-color: #10b981; }
.campaign-card.st-draft { border-left-color: #1a6dff; border-left-style: dashed; }
.campaign-card.st-paused { border-left-color: #f59e0b; }
.campaign-card.st-archived { border-left-color: #d1d5db; }

.card-main {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 20px;
  cursor: pointer;
}
.card-main:hover .card-name { color: #1a6dff; }

.card-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
  background: #f4f6f9;
}
.card-icon.icon-org { background: #e8f0ff; }
.card-icon.icon-place { background: #ecfdf5; }
.card-icon.icon-user { background: #f5f3ff; }

.card-info {
  flex: 1;
  min-width: 0;
}
.card-top {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}
.card-name {
  font-size: 15px;
  font-weight: 600;
  color: #1e2a3a;
  transition: color 0.15s;
}
.status-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 20px;
}
.period-badge {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 20px;
  background: #f4f6f9;
  color: #5a6474;
}
.card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.meta-label {
  font-size: 12px;
  color: #b8c0cc;
}
.meta-val {
  font-size: 12px;
  font-weight: 500;
  color: #5a6474;
}
.meta-sep {
  color: #dce1e8;
  font-size: 12px;
}
.card-desc {
  font-size: 12px;
  color: #8c95a3;
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Card footer */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 8px 16px;
  border-top: 1px solid #f0f2f5;
  background: #fafbfc;
}
.card-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}
.act-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  background: none;
  border: 1px solid #e8ecf0;
  border-radius: 5px;
  font-size: 12px;
  color: #5a6474;
  cursor: pointer;
  transition: all 0.12s;
  white-space: nowrap;
  font-family: inherit;
}
.act-btn:hover {
  background: #f4f6f9;
  color: #1a6dff;
  border-color: #c8d4e3;
}
.act-btn-primary {
  background: #1a6dff;
  color: #fff;
  border-color: #1a6dff;
}
.act-btn-primary:hover {
  background: #1558d6;
  color: #fff;
  border-color: #1558d6;
}
.act-btn-icon {
  padding: 5px 10px;
  font-size: 14px;
  letter-spacing: 2px;
}

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
.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e8ecf0;
  border-top-color: #1a6dff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.state-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  gap: 10px;
}
.empty-icon {
  font-size: 48px;
  opacity: 0.3;
}
.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: #6b7685;
  margin: 0;
}
.empty-sub {
  font-size: 12px;
  color: #b8c0cc;
  margin: 0;
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
  transition: background 0.15s;
  white-space: nowrap;
  font-family: inherit;
}
.btn-primary:hover { background: #1558d6; }
.btn-primary:disabled { background: #93b8ff; cursor: not-allowed; }
.btn-icon { font-size: 16px; line-height: 1; }

.btn-ghost {
  padding: 8px 16px;
  background: none;
  border: 1px solid #dce1e8;
  border-radius: 8px;
  font-size: 13px;
  color: #5a6474;
  cursor: pointer;
  transition: background 0.15s;
  font-family: inherit;
}
.btn-ghost:hover { background: #f4f6f9; }

/* ==================== Dropdown ==================== */
.dropdown-menu {
  position: fixed;
  min-width: 130px;
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 10px;
  padding: 4px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  z-index: 9999;
}
.dropdown-menu button {
  display: flex;
  align-items: center;
  gap: 7px;
  width: 100%;
  padding: 7px 11px;
  font-size: 13px;
  color: #3d4757;
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.12s;
  text-align: left;
  white-space: nowrap;
  font-family: inherit;
}
.dropdown-menu button:hover { background: #f4f6f9; }
.dropdown-menu button.danger { color: #d93025; }
.dropdown-menu button.danger:hover { background: #fef2f2; }

.dropdown-enter-active { transition: all 0.15s ease-out; }
.dropdown-leave-active { transition: all 0.1s ease-in; }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: translateY(-4px); }

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
  width: 460px;
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
.modal-head h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0;
}
.modal-close {
  background: none;
  border: none;
  font-size: 22px;
  color: #b8c0cc;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
}
.modal-close:hover { color: #5a6474; }
.modal-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 24px;
}
.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 24px 20px;
}
.fld label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: #5a6474;
  margin-bottom: 5px;
}
.fld input {
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
}
.fld input:focus {
  border-color: #7aadff;
  box-shadow: 0 0 0 3px rgba(26, 109, 255, 0.08);
}
.req { color: #d93025; }
.exec-note {
  font-size: 12px;
  color: #8c95a3;
  margin: 0;
  background: #f4f6f9;
  padding: 10px 12px;
  border-radius: 6px;
}

.modal-enter-active { transition: all 0.2s ease-out; }
.modal-leave-active { transition: all 0.15s ease-in; }
.modal-enter-from { opacity: 0; }
.modal-enter-from .modal-box { transform: translateY(12px) scale(0.97); }
.modal-leave-to { opacity: 0; }
.modal-leave-to .modal-box { transform: translateY(-8px) scale(0.98); }
</style>
