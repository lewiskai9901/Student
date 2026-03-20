<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Search, Copy, Upload, Archive, Ban, Trash2,
  Pencil, MoreHorizontal, LayoutGrid, FileText,
} from 'lucide-vue-next'
import { useInspTemplateStore } from '@/stores/insp/inspTemplateStore'
import { inspTemplateApi } from '@/api/insp/template'
import { TemplateStatusConfig, TargetTypeConfig, type TemplateStatus, type TargetType } from '@/types/insp/enums'
import type { TemplateSection } from '@/types/insp/template'

const router = useRouter()
const templateStore = useInspTemplateStore()

// ==================== State ====================
const loading = ref(false)
const rootSections = ref<TemplateSection[]>([])
const total = ref(0)
const childSectionsMap = ref<Map<number, TemplateSection[]>>(new Map())

const query = reactive({
  page: 1,
  size: 50,
  keyword: '',
  status: undefined as TemplateStatus | undefined,
  sort: 'updated' as 'updated' | 'name' | 'status',
})

// ==================== Stats ====================
const stats = computed(() => {
  const all = total.value
  const draft = rootSections.value.filter(s => s.status === 'DRAFT').length
  const published = rootSections.value.filter(s => s.status === 'PUBLISHED').length
  const archived = rootSections.value.filter(s => s.status === 'ARCHIVED').length
  return { all, draft, published, archived }
})

const activeFilter = ref<TemplateStatus | 'ALL'>('ALL')

const filteredSections = computed(() => {
  let list = [...rootSections.value]
  if (activeFilter.value !== 'ALL') {
    list = list.filter(s => s.status === activeFilter.value)
  }
  if (query.sort === 'name') {
    list = [...list].sort((a, b) => a.sectionName.localeCompare(b.sectionName))
  } else if (query.sort === 'status') {
    const order: Record<TemplateStatus, number> = { PUBLISHED: 0, DRAFT: 1, DEPRECATED: 2, ARCHIVED: 3 }
    list = [...list].sort((a, b) => (order[a.status] ?? 9) - (order[b.status] ?? 9))
  }
  return list
})

// ==================== Dropdown ====================
const openDropdownId = ref<number | null>(null)
const dropdownStyle = ref<Record<string, string>>({})

function toggleDropdown(id: number, event?: MouseEvent) {
  if (openDropdownId.value === id) { openDropdownId.value = null; return }
  openDropdownId.value = id
  if (event) {
    const btn = event.currentTarget as HTMLElement
    const rect = btn.getBoundingClientRect()
    dropdownStyle.value = {
      position: 'fixed',
      top: `${rect.bottom + 4}px`,
      right: `${window.innerWidth - rect.right}px`,
      left: 'auto',
    }
  }
}
function closeDropdowns() { openDropdownId.value = null }

// ==================== Load ====================
async function loadTemplates() {
  loading.value = true
  try {
    const result = await templateStore.loadRootSections({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: undefined, // load all, filter client-side for stats
    })
    rootSections.value = result.records
    total.value = result.total

    // Load first-level children for each root section to show target type tags
    const map = new Map<number, TemplateSection[]>()
    await Promise.all(result.records.map(async (root) => {
      try {
        const children = await inspTemplateApi.getSections(root.id)
        map.set(root.id, children.filter((c: TemplateSection) => c.parentSectionId === root.id))
      } catch { /* ignore */ }
    }))
    childSectionsMap.value = map
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function getFirstLevelChildren(rootId: number): TemplateSection[] {
  return childSectionsMap.value.get(rootId) || []
}

function getTargetTypes(rootId: number): TargetType[] {
  const children = getFirstLevelChildren(rootId)
  const types = new Set<TargetType>()
  for (const c of children) {
    if (c.targetType) types.add(c.targetType as TargetType)
  }
  return Array.from(types)
}

function getSectionCount(rootId: number): number {
  return getFirstLevelChildren(rootId).length
}

function handleSearch() { query.page = 1; loadTemplates() }
function handleSortChange() { /* already reactive via computed */ }

// ==================== Create ====================
const createDialogVisible = ref(false)
const createForm = ref({ name: '', description: '' })
const maskMouseDownTarget = ref<EventTarget | null>(null)

function onMaskMouseDown(e: MouseEvent) { maskMouseDownTarget.value = e.target }
function onMaskClick(e: MouseEvent, closeFn: () => void) {
  if (e.target === e.currentTarget && maskMouseDownTarget.value === e.currentTarget) closeFn()
  maskMouseDownTarget.value = null
}

function goCreate() {
  createForm.value = { name: '', description: '' }
  createDialogVisible.value = true
}

async function handleCreate() {
  const name = createForm.value.name.trim()
  if (!name) { ElMessage.warning('请输入模板名称'); return }
  try {
    const section = await templateStore.addRootSection({ name, description: createForm.value.description || undefined })
    createDialogVisible.value = false
    router.push(`/inspection/v7/templates/${section.id}/edit`)
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  }
}

// ==================== Actions ====================
function goEdit(section: TemplateSection) {
  router.push(`/inspection/v7/templates/${section.id}/edit`)
}

async function handlePublish(section: TemplateSection) {
  try {
    await ElMessageBox.confirm('发布后将创建不可变版本快照，确认发布？', '确认发布', { type: 'warning' })
    await templateStore.publish(section.id)
    ElMessage.success('发布成功')
    loadTemplates()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '发布失败') }
  closeDropdowns()
}

async function handleDeprecate(section: TemplateSection) {
  try {
    await ElMessageBox.confirm('废弃后新项目无法使用此模板', '确认废弃', { type: 'warning' })
    await templateStore.deprecate(section.id)
    ElMessage.success('已废弃')
    loadTemplates()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
  closeDropdowns()
}

async function handleArchive(section: TemplateSection) {
  try {
    await ElMessageBox.confirm('归档后将不可见', '确认归档', { type: 'warning' })
    await templateStore.archive(section.id)
    ElMessage.success('已归档')
    loadTemplates()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
  closeDropdowns()
}

async function handleDuplicate(section: TemplateSection) {
  try {
    await templateStore.duplicate(section.id)
    ElMessage.success('复制成功')
    loadTemplates()
  } catch (e: any) { ElMessage.error(e.message || '复制失败') }
  closeDropdowns()
}

async function handleDelete(section: TemplateSection) {
  try {
    await ElMessageBox.confirm(`确认删除「${section.sectionName}」？此操作不可恢复。`, '确认删除', { type: 'warning' })
    await templateStore.removeRootSection(section.id)
    ElMessage.success('已删除')
    loadTemplates()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
  closeDropdowns()
}

// ==================== Helpers ====================
function formatDate(t?: string) {
  if (!t) return '-'
  const d = new Date(t)
  return `${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getDate().toString().padStart(2, '0')}`
}

// ==================== Lifecycle ====================
onMounted(() => { loadTemplates() })
</script>

<template>
  <div class="cfg" @click="closeDropdowns">
    <!-- Header -->
    <div class="cfg-header">
      <div class="cfg-header-left">
        <LayoutGrid :size="18" class="cfg-header-icon" />
        <h1 class="cfg-title">检查配置</h1>
      </div>
      <button class="btn-primary" @click="goCreate">
        <Plus :size="14" />
        新建模板
      </button>
    </div>

    <!-- Stats bar -->
    <div class="cfg-stats">
      <button
        class="stat-item" :class="{ active: activeFilter === 'ALL' }"
        @click="activeFilter = 'ALL'"
      >
        <span class="stat-label">全部</span>
        <span class="stat-value">{{ stats.all }}</span>
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
        class="stat-item" :class="{ active: activeFilter === 'PUBLISHED' }"
        @click="activeFilter = 'PUBLISHED'"
      >
        <span class="stat-label">已发布</span>
        <span class="stat-value published">{{ stats.published }}</span>
      </button>
      <span class="stat-sep">│</span>
      <button
        class="stat-item" :class="{ active: activeFilter === 'ARCHIVED' }"
        @click="activeFilter = 'ARCHIVED'"
      >
        <span class="stat-label">已归档</span>
        <span class="stat-value">{{ stats.archived }}</span>
      </button>
    </div>

    <!-- Search + filter bar -->
    <div class="cfg-toolbar">
      <div class="cfg-toolbar-left">
        <div class="search-wrap">
          <Search :size="13" class="search-icon" />
          <input
            v-model="query.keyword"
            type="text"
            placeholder="搜索模板名称..."
            class="search-input"
            @keyup.enter="handleSearch"
          />
        </div>
        <select v-model="query.sort" class="filter-select" @change="handleSortChange">
          <option value="updated">最近更新</option>
          <option value="name">名称</option>
          <option value="status">状态</option>
        </select>
      </div>
      <span class="cfg-total">{{ filteredSections.length }} 个模板</span>
    </div>

    <!-- Card Grid -->
    <div class="cfg-content">
      <!-- Loading -->
      <div v-if="loading" class="state-loading">
        <div class="spinner" />
        <span>加载中...</span>
      </div>

      <!-- Empty -->
      <div v-else-if="filteredSections.length === 0" class="state-empty">
        <FileText :size="48" class="empty-icon" />
        <p class="empty-title">{{ activeFilter === 'ALL' ? '暂无检查模板' : `暂无${TemplateStatusConfig[activeFilter as TemplateStatus]?.label}模板` }}</p>
        <p class="empty-sub">检查模板定义检查表单结构、字段和评分规则</p>
        <button v-if="activeFilter === 'ALL'" class="btn-primary" @click="goCreate">
          <Plus :size="14" /> 创建第一个模板
        </button>
      </div>

      <!-- Grid -->
      <div v-else class="card-grid">
        <div
          v-for="sec in filteredSections"
          :key="sec.id"
          class="tpl-card"
          :class="['st-' + sec.status.toLowerCase()]"
          @click="goEdit(sec)"
        >
          <!-- Card header -->
          <div class="card-head">
            <div class="card-title-row">
              <span class="card-name">{{ sec.sectionName }}</span>
              <span class="status-dot" :class="'dot-' + sec.status.toLowerCase()" />
            </div>
          </div>

          <!-- Card body -->
          <div class="card-body">
            <div class="card-meta">
              <span class="meta-count">{{ getSectionCount(sec.id) }} 分区</span>
              <span class="meta-dot">·</span>
              <span class="meta-count">v{{ sec.latestVersion }}</span>
            </div>

            <!-- Target type tags -->
            <div class="card-tags" v-if="getTargetTypes(sec.id).length > 0">
              <span
                v-for="tt in getTargetTypes(sec.id)"
                :key="tt"
                class="target-tag"
                :class="'tt-' + tt.toLowerCase()"
              >{{ TargetTypeConfig[tt]?.label }}</span>
            </div>
            <div v-else class="card-tags">
              <span class="target-tag tt-none">通用</span>
            </div>
          </div>

          <!-- Card footer -->
          <div class="card-footer" @click.stop>
            <span class="card-time">{{ formatDate(sec.updatedAt) }} 更新</span>
            <div class="card-actions">
              <button
                class="act-btn"
                title="编辑"
                @click.stop="goEdit(sec)"
              >
                <Pencil :size="13" />
                编辑
              </button>
              <button
                class="act-btn"
                title="复制"
                @click.stop="handleDuplicate(sec)"
              >
                <Copy :size="13" />
                复制
              </button>
              <div class="dropdown-wrap">
                <button
                  class="act-btn act-btn-icon"
                  @click.stop="toggleDropdown(sec.id, $event)"
                >
                  <MoreHorizontal :size="14" />
                </button>
                <Transition name="dropdown">
                  <div v-if="openDropdownId === sec.id" class="dropdown-menu" :style="dropdownStyle">
                    <button v-if="sec.status === 'DRAFT'" @click.stop="handlePublish(sec)">
                      <Upload :size="13" /> 发布
                    </button>
                    <button v-if="sec.status === 'PUBLISHED'" class="warn" @click.stop="handleDeprecate(sec)">
                      <Ban :size="13" /> 废弃
                    </button>
                    <button v-if="sec.status !== 'ARCHIVED'" @click.stop="handleArchive(sec)">
                      <Archive :size="13" /> 归档
                    </button>
                    <div v-if="sec.status !== 'PUBLISHED'" class="dropdown-divider" />
                    <button v-if="sec.status !== 'PUBLISHED'" class="danger" @click.stop="handleDelete(sec)">
                      <Trash2 :size="13" /> 删除
                    </button>
                  </div>
                </Transition>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create Dialog -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="createDialogVisible"
          class="modal-mask"
          @mousedown="onMaskMouseDown"
          @click="onMaskClick($event, () => createDialogVisible = false)"
        >
          <div class="modal-box">
            <div class="modal-head">
              <h3>新建检查模板</h3>
              <button class="modal-close" @click="createDialogVisible = false">&times;</button>
            </div>
            <div class="modal-body">
              <div class="fld">
                <label>模板名称 <span class="req">*</span></label>
                <input
                  v-model="createForm.name"
                  placeholder="如：宿舍卫生全面检查"
                  @keyup.enter="handleCreate"
                />
              </div>
              <div class="fld">
                <label>描述</label>
                <textarea
                  v-model="createForm.description"
                  rows="3"
                  placeholder="可选，简要说明模板用途"
                />
              </div>
            </div>
            <div class="modal-foot">
              <button class="btn-ghost" @click="createDialogVisible = false">取消</button>
              <button class="btn-primary" @click="handleCreate">创建并编辑</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
/* ==================== Root ==================== */
.cfg {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ==================== Header ==================== */
.cfg-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px 14px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
}
.cfg-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.cfg-header-icon {
  color: #1a6dff;
}
.cfg-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0;
}

/* ==================== Stats bar ==================== */
.cfg-stats {
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
.stat-item:hover {
  background: #f4f6f9;
}
.stat-item.active {
  background: #e8f0ff;
}
.stat-label {
  font-size: 13px;
  color: #5a6474;
  font-weight: 500;
}
.stat-item.active .stat-label {
  color: #1a6dff;
}
.stat-value {
  font-size: 13px;
  font-weight: 700;
  color: #1e2a3a;
}
.stat-value.published {
  color: #10b981;
}
.stat-item.active .stat-value {
  color: #1a6dff;
}

/* ==================== Toolbar ==================== */
.cfg-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
  gap: 12px;
}
.cfg-toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.cfg-total {
  font-size: 12px;
  color: #b8c0cc;
}
.search-wrap {
  position: relative;
}
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
.search-input::placeholder {
  color: #b8c0cc;
}
.search-input:focus {
  border-color: #7aadff;
  box-shadow: 0 0 0 3px rgba(26, 109, 255, 0.08);
}
.filter-select {
  height: 34px;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  padding: 0 10px;
  font-size: 13px;
  outline: none;
  background: #fff;
  color: #3d4757;
  cursor: pointer;
  transition: border-color 0.2s;
}
.filter-select:focus {
  border-color: #7aadff;
}

/* ==================== Content area ==================== */
.cfg-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

/* ==================== Card Grid ==================== */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
}

/* ==================== Template Card ==================== */
.tpl-card {
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 10px;
  border-left: 3px solid #dce1e8;
  cursor: pointer;
  transition: transform 0.18s, box-shadow 0.18s, border-color 0.18s;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.tpl-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border-color: #c8d4e3;
}

/* Status-based left border */
.tpl-card.st-published {
  border-left-color: #10b981;
}
.tpl-card.st-draft {
  border-left-color: #1a6dff;
  border-left-style: dashed;
}
.tpl-card.st-deprecated {
  border-left-color: #f59e0b;
}
.tpl-card.st-archived {
  border-left-color: #d1d5db;
}

/* Card head */
.card-head {
  padding: 14px 16px 8px;
}
.card-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}
.card-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e2a3a;
  line-height: 1.4;
  flex: 1;
  min-width: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.tpl-card:hover .card-name {
  color: #1a6dff;
}

/* Status dot */
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 4px;
}
.dot-published { background: #10b981; }
.dot-draft { background: #94a3b8; }
.dot-deprecated { background: #f59e0b; }
.dot-archived { background: #d1d5db; }

/* Card body */
.card-body {
  padding: 2px 16px 10px;
  flex: 1;
}
.card-meta {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 8px;
}
.meta-count {
  font-size: 12px;
  color: #8c95a3;
}
.meta-dot {
  font-size: 11px;
  color: #c8d0db;
}
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

/* Target type tags */
.target-tag {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
}
.tt-org { background: #e8f0ff; color: #2563eb; }
.tt-place { background: #ecfdf5; color: #059669; }
.tt-user { background: #f5f3ff; color: #7c3aed; }
.tt-asset { background: #fff7ed; color: #d97706; }
.tt-composite { background: #fdf2f8; color: #be185d; }
.tt-none { background: #f4f6f9; color: #8c95a3; }

/* Card footer */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 12px 9px 16px;
  border-top: 1px solid #f0f2f5;
  background: #fafbfc;
}
.card-time {
  font-size: 11px;
  color: #b8c0cc;
}
.card-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}
.act-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 9px;
  background: none;
  border: none;
  border-radius: 5px;
  font-size: 12px;
  color: #6b7685;
  cursor: pointer;
  transition: all 0.12s;
  white-space: nowrap;
}
.act-btn:hover {
  background: #f0f2f5;
  color: #1a6dff;
}
.act-btn-icon {
  padding: 4px 6px;
}

/* Dropdown */
.dropdown-wrap { position: relative; }
.dropdown-menu {
  position: fixed;
  min-width: 130px;
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 10px;
  padding: 4px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  z-index: 200;
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
}
.dropdown-menu button:hover { background: #f4f6f9; }
.dropdown-menu button.warn { color: #c47a00; }
.dropdown-menu button.warn:hover { background: #fff7e6; }
.dropdown-menu button.danger { color: #d93025; }
.dropdown-menu button.danger:hover { background: #fef2f2; }
.dropdown-divider { height: 1px; background: #f0f2f5; margin: 3px 6px; }

.dropdown-enter-active { transition: all 0.15s ease-out; }
.dropdown-leave-active { transition: all 0.1s ease-in; }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: translateY(-4px); }

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
.empty-icon { color: #dce1e8; }
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
}
.btn-primary:hover { background: #1558d6; }
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
.fld input, .fld textarea {
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
.fld input::placeholder, .fld textarea::placeholder { color: #b8c0cc; }
.fld input:focus, .fld textarea:focus {
  border-color: #7aadff;
  box-shadow: 0 0 0 3px rgba(26, 109, 255, 0.08);
}
.req { color: #d93025; }

/* Modal transition */
.modal-enter-active { transition: all 0.2s ease-out; }
.modal-leave-active { transition: all 0.15s ease-in; }
.modal-enter-from { opacity: 0; }
.modal-enter-from .modal-box { transform: translateY(12px) scale(0.97); }
.modal-leave-to { opacity: 0; }
.modal-leave-to .modal-box { transform: translateY(-8px) scale(0.98); }
</style>
