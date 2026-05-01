<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Search, Copy, Upload, Archive, Ban, Trash2,
  Pencil, MoreHorizontal, LayoutGrid, FileText,
} from 'lucide-vue-next'
import { useInspTemplateStore } from '@/stores/inspection/inspTemplateStore'
import { inspTemplateApi } from '@/api/inspection/template'
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

const dropdownPos = ref({ top: '0px', right: '0px' })

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

// 点击外部关闭
import { onUnmounted } from 'vue'
function onDocClick() { openDropdownId.value = null }
onMounted(() => document.addEventListener('click', onDocClick))
onUnmounted(() => document.removeEventListener('click', onDocClick))

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
    router.push(`/inspection/templates/${section.id}/edit`)
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  }
}

// ==================== Actions ====================
function goEdit(section: TemplateSection) {
  router.push(`/inspection/templates/${section.id}/edit`)
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
  <div class="insp-shell cfg" @click="closeDropdowns">
    <!-- Header (Audit Hub style) -->
    <header class="cfg-head">
      <div class="cfg-head__lead">
        <span class="insp-eyebrow">检查模板 · Inspection Templates</span>
        <h1 class="cfg-title">检查配置</h1>
      </div>
      <div class="cfg-head__stats">
        <button class="stat-pill" :class="{ 'is-active': activeFilter === 'ALL' }" @click="activeFilter = 'ALL'">
          <span class="stat-pill__num insp-num">{{ stats.all }}</span>
          <span class="stat-pill__label">全部</span>
        </button>
        <button class="stat-pill" :class="{ 'is-active': activeFilter === 'DRAFT' }" @click="activeFilter = 'DRAFT'">
          <span class="stat-pill__num insp-num">{{ stats.draft }}</span>
          <span class="stat-pill__label">草稿</span>
        </button>
        <button class="stat-pill" :class="{ 'is-active': activeFilter === 'PUBLISHED' }" @click="activeFilter = 'PUBLISHED'">
          <span class="stat-pill__num insp-num" :style="{ color: 'var(--insp-pass)' }">{{ stats.published }}</span>
          <span class="stat-pill__label">已发布</span>
        </button>
        <button class="stat-pill" :class="{ 'is-active': activeFilter === 'ARCHIVED' }" @click="activeFilter = 'ARCHIVED'">
          <span class="stat-pill__num insp-num">{{ stats.archived }}</span>
          <span class="stat-pill__label">已归档</span>
        </button>
        <button class="cfg-cta insp-btn insp-btn--accent" @click="goCreate">
          <Plus :size="13" />新建模板
        </button>
      </div>
    </header>

    <!-- Toolbar -->
    <div class="cfg-toolbar">
      <div class="cfg-toolbar__lead">
        <div class="cfg-search">
          <Search :size="12" class="cfg-search__icon" />
          <input
            v-model="query.keyword" type="text"
            placeholder="搜索模板名称..." class="cfg-search__input"
            @keyup.enter="handleSearch"
          />
        </div>
        <select v-model="query.sort" class="cfg-sort" @change="handleSortChange">
          <option value="updated">最近更新</option>
          <option value="name">名称</option>
          <option value="status">状态</option>
        </select>
      </div>
      <span class="cfg-total"><span class="insp-num">{{ filteredSections.length }}</span> 个模板</span>
    </div>

    <!-- List -->
    <section class="cfg-list">
      <div v-if="loading" class="cfg-state">加载中…</div>

      <div v-else-if="filteredSections.length === 0" class="cfg-empty">
        <p class="cfg-empty__title">
          {{ activeFilter === 'ALL' ? '暂无检查模板' : `暂无${TemplateStatusConfig[activeFilter as TemplateStatus]?.label}模板` }}
        </p>
        <p class="cfg-empty__sub">检查模板定义检查表单结构、字段和评分规则</p>
        <button v-if="activeFilter === 'ALL'" class="insp-btn insp-btn--accent" @click="goCreate">
          <Plus :size="13" /> 创建第一个模板
        </button>
      </div>

      <ul v-else class="tpl-rows">
        <li
          v-for="(sec, i) in filteredSections" :key="sec.id"
          class="tpl-row"
          @click="goEdit(sec)"
        >
          <span class="tpl-row__num insp-num">{{ String(i + 1).padStart(2, '0') }}</span>

          <div class="tpl-row__main">
            <div class="tpl-row__line1">
              <span class="tpl-row__name">{{ sec.sectionName }}</span>
              <span class="insp-chip"
                    :class="`insp-chip--${({DRAFT:'pending',PUBLISHED:'pass',DEPRECATED:'warn',ARCHIVED:'fail'} as any)[sec.status]}`">
                {{ TemplateStatusConfig[sec.status]?.label }}
              </span>
              <span class="tpl-row__version insp-num">v{{ sec.latestVersion }}</span>
            </div>
            <div class="tpl-row__meta">
              <span class="insp-num">{{ getSectionCount(sec.id) }} 分区</span>
              <span class="tpl-row__sep">·</span>
              <span class="insp-num">{{ formatDate(sec.updatedAt) }}</span>
              <template v-if="getTargetTypes(sec.id).length > 0">
                <span class="tpl-row__sep">·</span>
                <span v-for="tt in getTargetTypes(sec.id)" :key="tt" class="tpl-row__tt">
                  {{ TargetTypeConfig[tt]?.label }}
                </span>
              </template>
            </div>
          </div>

          <div class="tpl-row__actions" @click.stop>
            <button class="insp-btn insp-btn--sm" @click="goEdit(sec)">编辑</button>
            <button class="insp-btn insp-btn--sm" @click="handleDuplicate(sec)">复制</button>
            <button class="insp-btn insp-btn--sm insp-btn--ghost" @click.stop="toggleDropdown(Number(sec.id), $event)">
              <MoreHorizontal :size="13" />
            </button>
          </div>
        </li>
      </ul>
    </section>

    <!-- Dropdown Menu (Teleported to body) -->
    <Teleport to="body">
      <Transition name="dropdown">
        <div v-if="openDropdownId !== null" class="dropdown-menu" :style="{ top: dropdownPos.top, right: dropdownPos.right }" @click.stop>
          <template v-for="sec in filteredSections" :key="sec.id">
            <template v-if="Number(sec.id) === openDropdownId">
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
            </template>
          </template>
        </div>
      </Transition>
    </Teleport>

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
.cfg {
  padding: 12px 16px;
}

/* ─ Header ─────── */
.cfg-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: 12px 16px;
  margin-bottom: 10px;
}
.cfg-head__lead { display: flex; flex-direction: column; gap: 2px; }
.cfg-title {
  font-size: 17px;
  font-weight: 700;
  margin: 2px 0 0;
  color: var(--insp-ink-primary);
}
.cfg-head__stats {
  display: flex;
  align-items: center;
  gap: 6px;
}

.stat-pill {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
  min-width: 60px;
  padding: 4px 12px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--insp-radius-sm);
  cursor: pointer;
  transition: all var(--insp-t-fast);
  font-family: inherit;
}
.stat-pill:hover {
  background: var(--insp-bg-subtle);
}
.stat-pill.is-active {
  background: var(--insp-accent-paler);
  border-color: var(--insp-accent-pale);
}
.stat-pill__num {
  font-family: var(--insp-font-mono);
  font-size: 17px;
  font-weight: 700;
  line-height: 1;
  color: var(--insp-ink-primary);
}
.stat-pill.is-active .stat-pill__num { color: var(--insp-accent); }
.stat-pill__label {
  font-size: 10px;
  color: var(--insp-ink-tertiary);
  letter-spacing: 0.04em;
  text-transform: uppercase;
  margin-top: 2px;
}

.cfg-cta { margin-left: 6px; }

/* ─ Toolbar ─────── */
.cfg-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: 8px 14px;
  margin-bottom: 10px;
}
.cfg-toolbar__lead {
  display: flex;
  align-items: center;
  gap: 8px;
}
.cfg-search {
  position: relative;
  display: flex;
  align-items: center;
}
.cfg-search__icon {
  position: absolute;
  left: 8px;
  color: var(--insp-ink-tertiary);
}
.cfg-search__input {
  height: 26px;
  padding: 0 10px 0 24px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-sm);
  font-size: 12px;
  font-family: inherit;
  width: 220px;
  background: var(--insp-bg-surface);
}
.cfg-search__input:focus {
  outline: none;
  border-color: var(--insp-accent);
  box-shadow: 0 0 0 3px var(--insp-accent-paler);
}
.cfg-sort {
  height: 26px;
  padding: 0 24px 0 8px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-sm);
  font-size: 12px;
  font-family: inherit;
  background: var(--insp-bg-surface) url("data:image/svg+xml,%3Csvg width='10' height='6' viewBox='0 0 10 6' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%238A8A82' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E") right 8px center no-repeat;
  appearance: none;
  cursor: pointer;
}
.cfg-total {
  font-size: 12px;
  color: var(--insp-ink-tertiary);
}

/* ─ List ─────── */
.cfg-list {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  overflow: hidden;
}

.cfg-state {
  padding: 60px 20px;
  text-align: center;
  font-size: 13px;
  color: var(--insp-ink-tertiary);
}

.cfg-empty {
  padding: 80px 20px;
  text-align: center;
}
.cfg-empty__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--insp-ink-primary);
  margin: 0 0 6px;
}
.cfg-empty__sub {
  font-size: 12px;
  color: var(--insp-ink-tertiary);
  margin: 0 0 16px;
}

/* ─ Template rows ─────── */
.tpl-rows {
  list-style: none;
  margin: 0;
  padding: 0;
}
.tpl-row {
  display: grid;
  grid-template-columns: 40px 1fr auto;
  gap: 12px;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--insp-border-subtle);
  cursor: pointer;
  transition: background var(--insp-t-fast);
}
.tpl-row:last-child { border-bottom: 0; }
.tpl-row:hover { background: var(--insp-bg-subtle); }

.tpl-row__num {
  font-family: var(--insp-font-mono);
  font-size: 12px;
  font-weight: 600;
  color: var(--insp-ink-quaternary);
}

.tpl-row__main { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.tpl-row__line1 {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.tpl-row__name {
  font-size: 13px;
  font-weight: 600;
  color: var(--insp-ink-primary);
}
.tpl-row__version {
  font-family: var(--insp-font-mono);
  font-size: 11px;
  font-weight: 600;
  color: var(--insp-accent);
  padding: 1px 6px;
  border: 1px solid var(--insp-accent-pale);
  border-radius: 3px;
}
.tpl-row__meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--insp-ink-tertiary);
  flex-wrap: wrap;
}
.tpl-row__sep { color: var(--insp-ink-quaternary); }
.tpl-row__tt {
  display: inline-flex;
  align-items: center;
  padding: 1px 6px;
  background: var(--insp-bg-subtle);
  border-radius: 3px;
  color: var(--insp-ink-secondary);
}

.tpl-row__actions {
  display: flex;
  gap: 4px;
}

/* ─ Dropdown ─────── */
.dropdown-menu {
  position: fixed;
  z-index: 9999;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  box-shadow: var(--insp-shadow-md);
  min-width: 140px;
  padding: 4px;
}
.dropdown-menu button {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 7px 10px;
  background: transparent;
  border: 0;
  border-radius: var(--insp-radius-sm);
  font-family: inherit;
  font-size: 12px;
  color: var(--insp-ink-secondary);
  cursor: pointer;
  text-align: left;
}
.dropdown-menu button:hover { background: var(--insp-bg-subtle); color: var(--insp-ink-primary); }
.dropdown-menu button.warn:hover { background: var(--insp-warn-pale); color: var(--insp-warn); }
.dropdown-menu button.danger:hover { background: var(--insp-fail-pale); color: var(--insp-fail); }
.dropdown-divider {
  height: 1px;
  background: var(--insp-border-subtle);
  margin: 4px 2px;
}

.dropdown-enter-active, .dropdown-leave-active { transition: opacity var(--insp-t-fast); }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; }

/* ─ Modal ─────── */
.modal-mask {
  position: fixed; inset: 0; z-index: 9000;
  background: var(--insp-bg-overlay);
  display: flex; align-items: center; justify-content: center;
}
.modal-box {
  width: 480px;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-strong);
  border-radius: var(--insp-radius-lg);
  box-shadow: var(--insp-shadow-lg);
}
.modal-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--insp-border-subtle);
}
.modal-head h3 {
  font-size: 14px;
  font-weight: 600;
  color: var(--insp-ink-primary);
  margin: 0;
}
.modal-close {
  background: transparent;
  border: 0;
  font-size: 18px;
  color: var(--insp-ink-tertiary);
  cursor: pointer;
}
.modal-body { padding: 14px 16px; display: flex; flex-direction: column; gap: 12px; }
.modal-foot {
  display: flex; justify-content: flex-end; gap: 8px;
  padding: 10px 16px;
  border-top: 1px solid var(--insp-border-subtle);
}
.fld {
  display: flex; flex-direction: column; gap: 4px;
}
.fld label {
  font-size: 12px;
  font-weight: 500;
  color: var(--insp-ink-secondary);
}
.fld .req { color: var(--insp-fail); }
.fld input, .fld textarea {
  padding: 6px 10px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-sm);
  font-family: inherit;
  font-size: 13px;
  background: var(--insp-bg-surface);
  resize: vertical;
}
.fld input:focus, .fld textarea:focus {
  outline: none;
  border-color: var(--insp-accent);
  box-shadow: 0 0 0 3px var(--insp-accent-paler);
}
.btn-primary, .btn-ghost {
  height: 28px;
  padding: 0 12px;
  border-radius: var(--insp-radius-sm);
  font-family: inherit;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--insp-t-fast);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.btn-primary {
  background: var(--insp-accent);
  color: white;
  border: 1px solid var(--insp-accent);
}
.btn-primary:hover { background: var(--insp-accent-hover); }
.btn-ghost {
  background: var(--insp-bg-surface);
  color: var(--insp-ink-secondary);
  border: 1px solid var(--insp-border-strong);
}
.btn-ghost:hover { border-color: var(--insp-accent); color: var(--insp-accent); }

.modal-enter-active, .modal-leave-active { transition: opacity var(--insp-t-fast); }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>

