<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Search, Copy, Upload, Archive, Ban, Trash2, MoreHorizontal,
  Library, ListTree, Award, Tag, ArrowRight, List, Rows3,
  Check, Download, X, Grid3x3, AlertTriangle, CheckCircle2,
} from 'lucide-vue-next'
import { useInspTemplateStore } from '@/stores/inspection/inspTemplateStore'
import { inspTemplateApi } from '@/api/inspection/template'
import { useSearchHighlight } from '@/composables/useSearchHighlight'
import { useKbdHint } from '@/composables/useKbdHint'
import { TemplateStatusConfig, TargetTypeConfig, type TemplateStatus, type TargetType } from '@/types/insp/enums'
import type { TemplateSection } from '@/types/insp/template'

const router = useRouter()
const templateStore = useInspTemplateStore()

// ==================== 快捷入口 (替代假 tab — 4 个 ↑ 不再伪装成 tab) ====================
interface QuickAccess {
  key: string
  title: string
  desc: string
  icon: any
  to: string
  count?: number
}
const quickAccess = computed<QuickAccess[]>(() => [
  { key: 'library',    title: '检查项库',   desc: '可复用的检查项条目',      icon: Library,  to: '/inspection/library' },
  { key: 'profiles',   title: '评分方案',   desc: '维度权重、聚合规则',      icon: Award,    to: '/inspection/scoring-profiles' },
  { key: 'grades',     title: '等级方案',   desc: '分值到等级的映射',        icon: ListTree, to: '/inspection/grade-schemes' },
  { key: 'categories', title: '问题类目',   desc: '违规分类与整改归类',      icon: Tag,      to: '/inspection/issue-categories' },
])

// ==================== 视图模式 ====================
type ViewMode = 'compact' | 'standard' | 'grid'
const viewMode = ref<ViewMode>((localStorage.getItem('insp_cfg_view_mode') as ViewMode) || 'compact')
function setViewMode(m: ViewMode) {
  viewMode.value = m
  localStorage.setItem('insp_cfg_view_mode', m)
}

// 模板缩略色: 用名称 hash 派生 HSL, 同名稳定, 不同模板色相分散
function thumbGradient(name: string): string {
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = ((hash << 5) - hash + name.charCodeAt(i)) | 0
  const h = Math.abs(hash) % 360
  return `linear-gradient(135deg, hsl(${h}, 65%, 55%) 0%, hsl(${(h + 35) % 360}, 60%, 45%) 100%)`
}

// ==================== 搜索高亮 (J7: composable) ====================
const { highlightHtml } = useSearchHighlight()

// ==================== Hover 预览悬浮卡 (S+ 标志) ====================
interface PreviewData {
  sections: TemplateSection[]
  itemTotal: number
  scoringModes: Record<string, number>
  loading: boolean
}
const previewVisible = ref(false)
const previewTarget = ref<TemplateSection | null>(null)
const previewData = ref<PreviewData>({ sections: [], itemTotal: 0, scoringModes: {}, loading: false })
const previewPos = ref({ top: '0px', left: '0px', placement: 'right' as 'right' | 'left' | 'bottom' })
let previewShowTimer: any = null
let previewHideTimer: any = null
const previewCache = new Map<LongId, PreviewData>()

async function showPreview(sec: TemplateSection, e: MouseEvent) {
  clearTimeout(previewHideTimer)
  clearTimeout(previewShowTimer)
  const target = e.currentTarget as HTMLElement
  previewShowTimer = setTimeout(async () => {
    previewTarget.value = sec
    // 定位: 卡片右侧, 不够空间放下方
    const rect = target.getBoundingClientRect()
    const popoverWidth = 320
    if (rect.right + popoverWidth + 16 < window.innerWidth) {
      previewPos.value = { top: `${rect.top}px`, left: `${rect.right + 8}px`, placement: 'right' }
    } else if (rect.left > popoverWidth + 16) {
      previewPos.value = { top: `${rect.top}px`, left: `${rect.left - popoverWidth - 8}px`, placement: 'left' }
    } else {
      previewPos.value = { top: `${rect.bottom + 8}px`, left: `${rect.left}px`, placement: 'bottom' }
    }
    previewVisible.value = true
    // cache hit?
    const cached = previewCache.get(sec.id)
    if (cached) { previewData.value = cached; return }
    previewData.value = { sections: [], itemTotal: 0, scoringModes: {}, loading: true }
    try {
      const sections = await inspTemplateApi.getSections(sec.id) || []
      const leaves = sections.filter((c: any) => !c.parentSectionId || c.parentSectionId === sec.id)
      let itemTotal = 0
      const modes: Record<string, number> = {}
      for (const leaf of leaves) {
        try {
          const items = await inspTemplateApi.getItems(leaf.id)
          itemTotal += (items || []).length
          for (const it of (items || [])) {
            const m = ((it as any).scoringMode || 'OTHER') as string
            modes[m] = (modes[m] || 0) + 1
          }
        } catch { /* skip */ }
      }
      const data: PreviewData = { sections: leaves, itemTotal, scoringModes: modes, loading: false }
      previewCache.set(sec.id, data)
      // 仅当鼠标仍 hover 在同一目标上才展示
      if (previewTarget.value && previewTarget.value.id === sec.id) {
        previewData.value = data
      }
    } catch { previewData.value.loading = false }
  }, 600)
}
function hidePreview() {
  clearTimeout(previewShowTimer)
  previewHideTimer = setTimeout(() => { previewVisible.value = false }, 100)
}
function keepPreview() { clearTimeout(previewHideTimer) }

// ==================== State ====================
const loading = ref(false)
const rootSections = ref<TemplateSection[]>([])
const total = ref(0)
const childSectionsMap = ref<Map<LongId, TemplateSection[]>>(new Map())
// V20260524 Bug#8: 缓存创建者 id → name, 避免列表显示 "#1" 而无姓名
const creatorNamesMap = ref<Map<string, string>>(new Map())
// P1-160: 使用计数 (rootSectionId > 在用项目数)
const usageMap = ref<Record<LongId, number>>({})

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

// ==================== 批量选择 ====================
const selectedIds = ref<Set<LongId>>(new Set())
const allSelectedInView = computed(() =>
  filteredSections.value.length > 0 &&
  filteredSections.value.every(s => selectedIds.value.has(s.id))
)
const someSelectedInView = computed(() =>
  !allSelectedInView.value &&
  filteredSections.value.some(s => selectedIds.value.has(s.id))
)
function toggleSelect(id: LongId, e?: Event) {
  e?.stopPropagation()
  const n = id
  const next = new Set(selectedIds.value)
  if (next.has(n)) next.delete(n); else next.add(n)
  selectedIds.value = next
}
function toggleSelectAll() {
  if (allSelectedInView.value) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(filteredSections.value.map(s => s.id))
  }
}
function clearSelection() { selectedIds.value = new Set() }

// ==================== 批量发布预校验 ====================
interface PrecheckRow {
  id: LongId
  name: string
  ok: boolean
  reason?: string
  sectionCount: number
  itemCount: number
}
const precheckDialogVisible = ref(false)
const precheckRunning = ref(false)
const precheckRows = ref<PrecheckRow[]>([])
const precheckPublishing = ref(false)

async function batchPublish() {
  const ids = Array.from(selectedIds.value).filter(id => {
    const s = rootSections.value.find(x => x.id === id)
    return s?.status === 'DRAFT'
  })
  if (ids.length === 0) { ElMessage.warning('当前选中的模板里没有可发布的草稿'); return }

  // 打开校验对话, 边校验边显示进度
  precheckDialogVisible.value = true
  precheckRunning.value = true
  precheckRows.value = []
  for (const id of ids) {
    const sec = rootSections.value.find(x => x.id === id)!
    const row: PrecheckRow = {
      id, name: sec.sectionName, ok: false,
      sectionCount: 0, itemCount: 0,
    }
    try {
      const children = await inspTemplateApi.getSections(id as any)
      const leaves = (children || []).filter((c: any) => !c.parentSectionId || c.parentSectionId === id)
      row.sectionCount = leaves.length
      let itemTotal = 0
      for (const leaf of leaves) {
        try {
          const items = await inspTemplateApi.getItems(leaf.id)
          itemTotal += (items || []).length
        } catch { /* skip */ }
      }
      row.itemCount = itemTotal
      if (row.sectionCount === 0) row.reason = '无任何分区'
      else if (row.itemCount === 0) row.reason = '所有分区均无检查项'
      else row.ok = true
    } catch (e: any) {
      row.reason = e?.message || '加载失败'
    }
    precheckRows.value = [...precheckRows.value, row]
  }
  precheckRunning.value = false
}
async function confirmBatchPublishAfterPrecheck() {
  const okIds = precheckRows.value.filter(r => r.ok).map(r => r.id)
  if (okIds.length === 0) {
    ElMessage.warning('没有可发布的模板')
    return
  }
  precheckPublishing.value = true
  let ok = 0, fail = 0
  for (const id of okIds) {
    try { await templateStore.publish(id as any); ok++ } catch { fail++ }
  }
  precheckPublishing.value = false
  precheckDialogVisible.value = false
  ElMessage.success(`成功 ${ok} 条${fail > 0 ? `, 失败 ${fail} 条` : ''}`)
  clearSelection()
  loadTemplates()
}
async function batchArchive() {
  const ids = Array.from(selectedIds.value)
  if (ids.length === 0) return
  try {
    await ElMessageBox.confirm(`将批量归档 ${ids.length} 个模板, 归档后从默认列表隐藏. 确认?`, '批量归档', { type: 'warning' })
    let ok = 0, fail = 0
    for (const id of ids) {
      try { await templateStore.archive(id as any); ok++ } catch { fail++ }
    }
    ElMessage.success(`已归档 ${ok} 条${fail > 0 ? `, 失败 ${fail} 条` : ''}`)
    clearSelection()
    loadTemplates()
  } catch { /* cancel */ }
}
async function batchDuplicate() {
  const ids = Array.from(selectedIds.value)
  if (ids.length === 0) return
  let ok = 0, fail = 0
  for (const id of ids) {
    try { await templateStore.duplicate(id as any); ok++ } catch { fail++ }
  }
  ElMessage.success(`复制 ${ok} 条${fail > 0 ? `, 失败 ${fail} 条` : ''}`)
  clearSelection()
  loadTemplates()
}
function batchExport() {
  const ids = Array.from(selectedIds.value)
  const rows = filteredSections.value.filter(s => ids.includes(s.id))
  const csv = ['ID,名称,状态,版本,模板编码,创建时间,更新时间', ...rows.map(s =>
    [s.id, s.sectionName, s.status, `v${s.latestVersion}`, s.sectionCode || '', s.createdAt || '', s.updatedAt || ''].join(',')
  )].join('\n')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `templates-${new Date().toISOString().slice(0,10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success(`已导出 ${rows.length} 条`)
}

// ==================== Dropdown ====================
const openDropdownId = ref<LongId | null>(null)
const dropdownStyle = ref<Record<string, string>>({})

const dropdownPos = ref({ top: '0px', right: '0px' })

function toggleDropdown(id: LongId, event?: MouseEvent) {
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

// ==================== 全局键盘快捷键 ====================
const focusedIdx = ref<number>(-1)
const flatRows = computed(() => filteredSections.value)
function focusNext() {
  focusedIdx.value = Math.min(focusedIdx.value + 1, flatRows.value.length - 1)
  scrollFocusedIntoView()
}
function focusPrev() {
  focusedIdx.value = Math.max(0, focusedIdx.value - 1)
  scrollFocusedIntoView()
}
function scrollFocusedIntoView() {
  const sec = flatRows.value[focusedIdx.value]
  if (!sec) return
  nextTick(() => {
    const el = document.querySelector(`[data-section-id="${sec.id}"]`)
    el?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  })
}
function cycleViewMode() {
  const order: ViewMode[] = ['compact', 'standard', 'grid']
  const i = order.indexOf(viewMode.value)
  setViewMode(order[(i + 1) % order.length])
}
function onGlobalKey(e: KeyboardEvent) {
  const t = e.target as HTMLElement
  const tag = t?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || t?.isContentEditable) return
  if (createDialogVisible.value || versionsDialogVisible.value || precheckDialogVisible.value) return
  const cmdKey = e.metaKey || e.ctrlKey
  // CmdA 全选
  if (cmdKey && e.key === 'a') { e.preventDefault(); toggleSelectAll(); return }
  // CmdE 导出选中
  if (cmdKey && e.key === 'e') {
    if (selectedIds.value.size > 0) { e.preventDefault(); batchExport() }
    return
  }
  // CmdP 发布选中
  if (cmdKey && e.key === 'p') {
    if (selectedIds.value.size > 0) { e.preventDefault(); batchPublish() }
    return
  }
  if (cmdKey || e.altKey) return
  switch (e.key) {
    case 'j':
    case 'ArrowDown': e.preventDefault(); focusNext(); break
    case 'k':
    case 'ArrowUp': e.preventDefault(); focusPrev(); break
    case ' ': {
      const sec = flatRows.value[focusedIdx.value]
      if (sec) { e.preventDefault(); toggleSelect(sec.id) }
      break
    }
    case 'Enter':
    case 'e': {
      const sec = flatRows.value[focusedIdx.value]
      if (sec) { e.preventDefault(); goEdit(sec) }
      break
    }
    case '/': e.preventDefault(); (document.querySelector('.cfg-search__input') as HTMLElement)?.focus(); break
    case 'v': e.preventDefault(); cycleViewMode(); break
    case 'Escape': clearSelection(); focusedIdx.value = -1; break
  }
}
import { nextTick } from 'vue'
// J7: composable
const { showKbdHint, dismissKbdHint } = useKbdHint('insp_cfg_kbd_hint_dismissed')

onMounted(() => {
  document.addEventListener('click', onDocClick)
  window.addEventListener('keydown', onGlobalKey)
})
onUnmounted(() => {
  document.removeEventListener('click', onDocClick)
  window.removeEventListener('keydown', onGlobalKey)
})

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

    // P1-160: 批量取使用计数 — 一次请求, 不用 N+1
    try {
      const ids = result.records.map(r => r.id)
      usageMap.value = await inspTemplateApi.getRootSectionUsage(ids)
    } catch { usageMap.value = {} }

    // Load first-level children for each root section to show target type tags
    // V20260524 Bug#7: 保留全部子孙分区 (旧版只存一级直接 children, 导致列表"5 分区"与编辑器"7 分区"不一致)
    const map = new Map<LongId, TemplateSection[]>()
    await Promise.all(result.records.map(async (root) => {
      try {
        const allDescendants = await inspTemplateApi.getSections(root.id)
        map.set(root.id, allDescendants)
      } catch { /* ignore */ }
    }))
    childSectionsMap.value = map

    // V20260524 Bug#8: 批量解析 createdBy 用户姓名
    const creatorIds = new Set<string>()
    for (const r of result.records) {
      const cid = (r as any).createdBy
      if (cid != null) creatorIds.add(String(cid))
    }
    await Promise.all(Array.from(creatorIds).map(async (uid) => {
      if (creatorNamesMap.value.has(uid)) return
      try {
        const u: any = await (await import('@/api/user')).getUser(uid as any)
        creatorNamesMap.value.set(uid, u?.realName || u?.username || `#${uid}`)
      } catch { creatorNamesMap.value.set(uid, `#${uid}`) }
    }))
    creatorNamesMap.value = new Map(creatorNamesMap.value)
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

/** V20260524 Bug#7: 返回直接 children (一级子分区) — 给 target type tags 用 */
function getFirstLevelChildren(rootId: LongId): TemplateSection[] {
  const all = childSectionsMap.value.get(rootId) || []
  return all.filter(c => c.parentSectionId === rootId)
}

/** V20260524 Bug#7: 返回所有子孙分区 — 给计数显示用, 与编辑器顶栏一致 */
function getAllDescendants(rootId: LongId): TemplateSection[] {
  return childSectionsMap.value.get(rootId) || []
}

function getTargetTypes(rootId: LongId): TargetType[] {
  // V20260524 Bug#10: root.targetType 也算入 — 旧逻辑只看 children 导致单层模板显示"待配置"
  const types = new Set<TargetType>()
  const root = rootSections.value.find(s => String(s.id) === String(rootId))
  if (root?.targetType) types.add(root.targetType as TargetType)
  const children = getFirstLevelChildren(rootId)
  for (const c of children) {
    if (c.targetType) types.add(c.targetType as TargetType)
  }
  return Array.from(types)
}

function getSectionCount(rootId: LongId): number {
  return getAllDescendants(rootId).length
}

function handleSearch() { query.page = 1; loadTemplates() }
function handleSortChange() { /* already reactive via computed */ }

// ==================== Create ====================
const createDialogVisible = ref(false)
const createForm = ref({ name: '', description: '', targetType: 'ORG' as 'ORG' | 'USER' | 'PLACE' })
const maskMouseDownTarget = ref<EventTarget | null>(null)

function onMaskMouseDown(e: MouseEvent) { maskMouseDownTarget.value = e.target }
function onMaskClick(e: MouseEvent, closeFn: () => void) {
  if (e.target === e.currentTarget && maskMouseDownTarget.value === e.currentTarget) closeFn()
  maskMouseDownTarget.value = null
}

function goCreate() {
  createForm.value = { name: '', description: '', targetType: 'ORG' }
  createDialogVisible.value = true
}

async function handleCreate() {
  const name = createForm.value.name.trim()
  if (!name) { ElMessage.warning('请输入模板名称'); return }
  if (!createForm.value.targetType) { ElMessage.warning('请选择检查对象类型'); return }
  try {
    const section = await templateStore.addRootSection({
      name,
      description: createForm.value.description || undefined,
      targetType: createForm.value.targetType,
    })
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

// 版本管理弹窗状态
const versionsDialogVisible = ref(false)
const versionsTarget = ref<TemplateSection | null>(null)
const versionsList = ref<any[]>([])
async function goVersions(section: TemplateSection) {
  versionsTarget.value = section
  versionsList.value = []
  versionsDialogVisible.value = true
  try {
    const list = await inspTemplateApi.getVersions(section.id)
    versionsList.value = list || []
  } catch (e: any) {
    ElMessage.error(e.message || '加载版本失败')
  }
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
  const inUse = usageMap.value[section.id] || 0
  const tip = inUse > 0
    ? `! 此模板正被 ${inUse} 个进行中的项目引用. 废弃后这些项目将继续运行 (使用快照), 但新项目无法选用此模板. 确认?`
    : '废弃后新项目无法使用此模板. 确认?'
  try {
    await ElMessageBox.confirm(tip, '确认废弃', { type: 'warning' })
    await templateStore.deprecate(section.id)
    ElMessage.success('已废弃')
    loadTemplates()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
  closeDropdowns()
}

async function handleArchive(section: TemplateSection) {
  const inUse = usageMap.value[section.id] || 0
  const tip = inUse > 0
    ? `! 此模板正被 ${inUse} 个项目引用. 归档后将从默认列表隐藏. 确认?`
    : '归档后将不可见 (可在筛选中找回). 确认?'
  try {
    await ElMessageBox.confirm(tip, '确认归档', { type: 'warning' })
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
        <span class="insp-eyebrow">检查配置 · Inspection Configuration</span>
        <h1 class="cfg-title">检查配置中心</h1>
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

    <!-- 快捷入口 (4 卡片网格 — 替代之前的假 tab) -->
    <section class="cfg-quick">
      <button v-for="qa in quickAccess" :key="qa.key" class="cfg-quick__card" @click="router.push(qa.to)">
        <span class="cfg-quick__icon">
          <component :is="qa.icon" :size="14" />
        </span>
        <span class="cfg-quick__body">
          <span class="cfg-quick__title">{{ qa.title }}</span>
          <span class="cfg-quick__desc">{{ qa.desc }}</span>
        </span>
        <ArrowRight :size="12" class="cfg-quick__chev" />
      </button>
    </section>

    <!-- 主区块标题: 模板 (单 tab, 不再伪装) -->
    <div class="cfg-section-head">
      <h2 class="cfg-section-title">检查模板</h2>
      <span class="cfg-section-hint">定义检查表单结构、字段和评分规则</span>
    </div>

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
      <div class="cfg-toolbar__right">
        <span class="cfg-total"><span class="insp-num">{{ filteredSections.length }}</span> 个模板</span>
        <div class="cfg-view-toggle">
          <button class="cfg-view-btn" :class="{ 'is-active': viewMode === 'compact' }"
                  @click="setViewMode('compact')" title="紧凑模式">
            <Rows3 :size="13" />
          </button>
          <button class="cfg-view-btn" :class="{ 'is-active': viewMode === 'standard' }"
                  @click="setViewMode('standard')" title="标准模式">
            <List :size="13" />
          </button>
          <button class="cfg-view-btn" :class="{ 'is-active': viewMode === 'grid' }"
                  @click="setViewMode('grid')" title="网格模式">
            <Grid3x3 :size="13" />
          </button>
        </div>
      </div>
    </div>

    <!-- 批量操作工具栏 (仅当有选中) -->
    <Transition name="batch-bar">
      <div v-if="selectedIds.size > 0" class="cfg-batch-bar">
        <span class="cfg-batch-bar__count">
          已选 <strong class="insp-num">{{ selectedIds.size }}</strong> 条
        </span>
        <div class="cfg-batch-bar__rule" />
        <button class="cfg-batch-btn" @click="batchPublish" title="批量发布选中的草稿">
          <Upload :size="13" /> 发布
        </button>
        <button class="cfg-batch-btn" @click="batchDuplicate" title="批量复制为新草稿">
          <Copy :size="13" /> 复制
        </button>
        <button class="cfg-batch-btn" @click="batchArchive" title="批量归档">
          <Archive :size="13" /> 归档
        </button>
        <button class="cfg-batch-btn" @click="batchExport" title="导出 CSV">
          <Download :size="13" /> 导出 CSV
        </button>
        <button class="cfg-batch-btn cfg-batch-btn--ghost" @click="clearSelection" title="取消选中">
          <X :size="13" /> 取消
        </button>
      </div>
    </Transition>

    <!-- 键盘快捷键提示条 -->
    <div v-if="showKbdHint" class="cfg-kbd-hint">
      <span class="cfg-kbd-hint__group">
        <kbd class="insp-kbd">J</kbd><kbd class="insp-kbd">K</kbd> 浏览
      </span>
      <span class="cfg-kbd-hint__group">
        <kbd class="insp-kbd">Space</kbd> 选中
      </span>
      <span class="cfg-kbd-hint__group">
        <kbd class="insp-kbd">E</kbd> 编辑
      </span>
      <span class="cfg-kbd-hint__group">
        <kbd class="insp-kbd">V</kbd> 切视图
      </span>
      <span class="cfg-kbd-hint__group">
        <kbd class="insp-kbd">/</kbd> 搜索
      </span>
      <span class="cfg-kbd-hint__group">
        <kbd class="insp-kbd">CmdA</kbd> 全选
      </span>
      <span class="cfg-kbd-hint__group">
        <kbd class="insp-kbd">CmdE</kbd> 导出
      </span>
      <button class="cfg-kbd-hint__close" @click="dismissKbdHint" title="不再显示">×</button>
    </div>

    <!-- List -->
    <section class="cfg-list">
      <!-- 骨架占位 (S+ 加载体验) -->
      <div v-if="loading" class="cfg-skeleton" :class="`cfg-skeleton--${viewMode}`">
        <template v-if="viewMode === 'grid'">
          <div v-for="n in 6" :key="n" class="sk-card">
            <div class="sk-thumb" />
            <div class="sk-body">
              <div class="sk-line sk-line--lg" />
              <div class="sk-line sk-line--md" />
              <div class="sk-line sk-line--sm" />
            </div>
          </div>
        </template>
        <template v-else>
          <div v-for="n in 6" :key="n" class="sk-row">
            <div class="sk-line sk-line--lg" />
            <div class="sk-line sk-line--sm" />
          </div>
        </template>
      </div>

      <div v-else-if="filteredSections.length === 0" class="cfg-empty">
        <p class="cfg-empty__title">
          {{ activeFilter === 'ALL' ? '暂无检查模板' : `暂无${TemplateStatusConfig[activeFilter as TemplateStatus]?.label}模板` }}
        </p>
        <p class="cfg-empty__sub">检查模板定义检查表单结构、字段和评分规则</p>
        <button v-if="activeFilter === 'ALL'" class="insp-btn insp-btn--accent" @click="goCreate">
          <Plus :size="13" /> 创建第一个模板
        </button>
      </div>

      <div v-else class="tpl-list-wrap">
        <!-- 全选行 (仅当有项目时显示) -->
        <div class="tpl-select-all" :class="`tpl-select-all--${viewMode}`">
          <label class="tpl-checkbox" @click.stop>
            <input
              type="checkbox"
              :checked="allSelectedInView"
              :indeterminate.prop="someSelectedInView"
              @change="toggleSelectAll"
            />
            <span class="tpl-checkbox__box"><Check :size="10" /></span>
          </label>
          <span class="tpl-select-all__hint">
            {{ selectedIds.size > 0 ? `已选 ${selectedIds.size} / ${filteredSections.length}` : `选择全部 ${filteredSections.length} 条` }}
          </span>
        </div>

      <!-- 网格模式 (3 列卡片 + 缩略图) -->
      <div v-if="viewMode === 'grid'" class="tpl-grid">
        <article
          v-for="(sec, i) in filteredSections" :key="sec.id"
          :data-section-id="sec.id"
          class="tpl-card"
          :class="{
            'is-selected': selectedIds.has(sec.id),
            'is-focused': focusedIdx === i,
          }"
          @click="goEdit(sec)"
          @mouseenter="(e: any) => showPreview(sec, e)"
          @mouseleave="hidePreview"
        >
          <div class="tpl-card__thumb" :style="{ background: thumbGradient(sec.sectionName) }">
            <span class="tpl-card__thumb-letter">{{ sec.sectionName.slice(0, 1) }}</span>
            <span class="tpl-card__thumb-version insp-num">v{{ sec.latestVersion }}</span>
            <label class="tpl-checkbox tpl-card__checkbox" @click.stop>
              <input type="checkbox" :checked="selectedIds.has(sec.id)" @change="toggleSelect(sec.id)" />
              <span class="tpl-checkbox__box"><Check :size="10" /></span>
            </label>
            <span class="tpl-card__index insp-num">{{ String(i + 1).padStart(2, '0') }}</span>
          </div>
          <div class="tpl-card__body">
            <div class="tpl-card__title-line">
              <h3 class="tpl-card__title" v-html="highlightHtml(sec.sectionName, query.keyword)"></h3>
              <span class="insp-chip"
                    :class="`insp-chip--${({DRAFT:'pending',PUBLISHED:'pass',DEPRECATED:'warn',ARCHIVED:'pending'} as any)[sec.status]}`">
                {{ TemplateStatusConfig[sec.status]?.label }}
              </span>
            </div>
            <div class="tpl-card__metrics">
              <div class="tpl-card__metric">
                <span class="tpl-card__metric-num insp-num">{{ getSectionCount(sec.id) }}</span>
                <span class="tpl-card__metric-label">分区</span>
              </div>
              <div class="tpl-card__metric-rule" />
              <div class="tpl-card__metric">
                <span class="tpl-card__metric-num insp-num"
                      :style="{ color: usageMap[sec.id] > 0 ? 'var(--insp-info)' : '' }">
                  {{ usageMap[sec.id] || 0 }}
                </span>
                <span class="tpl-card__metric-label">在用</span>
              </div>
              <div class="tpl-card__metric-rule" />
              <div class="tpl-card__metric">
                <span class="tpl-card__metric-num insp-num">{{ formatDate(sec.updatedAt) }}</span>
                <span class="tpl-card__metric-label">更新</span>
              </div>
            </div>
            <div class="tpl-card__footer">
              <div class="tpl-card__tts">
                <span v-for="tt in getTargetTypes(sec.id)" :key="tt" class="tpl-row__tt">
                  {{ TargetTypeConfig[tt]?.label }}
                </span>
                <span v-if="getTargetTypes(sec.id).length === 0" class="tpl-card__no-tt">— 待配置 —</span>
              </div>
              <div class="tpl-card__actions" @click.stop>
                <button class="insp-btn insp-btn--xs" @click="goEdit(sec)">编辑</button>
                <button class="insp-btn insp-btn--xs insp-btn--ghost"
                        @click.stop="toggleDropdown(sec.id, $event)">
                  <MoreHorizontal :size="12" />
                </button>
              </div>
            </div>
          </div>
        </article>
      </div>

      <!-- 列表模式 (compact / standard) -->
      <ul v-else class="tpl-rows" :class="`tpl-rows--${viewMode}`">
        <li
          v-for="(sec, i) in filteredSections" :key="sec.id"
          :data-section-id="sec.id"
          class="tpl-row"
          :class="{
            'is-selected': selectedIds.has(sec.id),
            'is-focused': focusedIdx === i,
          }"
          @click="goEdit(sec)"
          @mouseenter="(e: any) => showPreview(sec, e)"
          @mouseleave="hidePreview"
        >
          <label class="tpl-checkbox tpl-row__checkbox" @click.stop>
            <input
              type="checkbox"
              :checked="selectedIds.has(sec.id)"
              @change="toggleSelect(sec.id)"
            />
            <span class="tpl-checkbox__box"><Check :size="10" /></span>
          </label>
          <span class="tpl-row__num insp-num">{{ String(i + 1).padStart(2, '0') }}</span>

          <div class="tpl-row__main">
            <div class="tpl-row__line1">
              <span class="tpl-row__name" v-html="highlightHtml(sec.sectionName, query.keyword)"></span>
              <span class="insp-chip"
                    :class="`insp-chip--${({DRAFT:'pending',PUBLISHED:'pass',DEPRECATED:'warn',ARCHIVED:'pending'} as any)[sec.status]}`">
                {{ TemplateStatusConfig[sec.status]?.label }}
              </span>
              <span class="tpl-row__version insp-num">v{{ sec.latestVersion }}</span>
              <template v-if="getTargetTypes(sec.id).length > 0">
                <span v-for="tt in getTargetTypes(sec.id)" :key="tt" class="tpl-row__tt">
                  {{ TargetTypeConfig[tt]?.label }}
                </span>
              </template>
              <template v-if="usageMap[sec.id] > 0">
                <span class="tpl-row__usage" :title="`此模板正在被 ${usageMap[sec.id]} 个项目引用`">
                  <span class="insp-num">{{ usageMap[sec.id] }}</span> 项目在用
                </span>
              </template>
            </div>
            <div class="tpl-row__meta">
              <span class="tpl-row__metric"><span class="insp-num">{{ getSectionCount(sec.id) }}</span> 分区</span>
              <span class="tpl-row__sep">·</span>
              <span class="tpl-row__date" :title="sec.updatedAt">更新于 <span class="insp-num">{{ formatDate(sec.updatedAt) }}</span></span>
              <template v-if="(sec as any).createdBy">
                <span class="tpl-row__sep">·</span>
                <span class="tpl-row__owner">创建人
                  <span class="insp-num">{{ creatorNamesMap.get(String((sec as any).createdBy)) || ('#' + (sec as any).createdBy) }}</span>
                </span>
              </template>
              <template v-if="sec.sectionCode">
                <span class="tpl-row__sep">·</span>
                <span class="tpl-row__code insp-num" :title="`模板编码 ${sec.sectionCode}`">{{ sec.sectionCode }}</span>
              </template>
            </div>
          </div>

          <div class="tpl-row__actions" @click.stop>
            <!-- 行内 primary action 按状态自适应 -->
            <button v-if="sec.status === 'DRAFT'"
              class="insp-btn insp-btn--sm insp-btn--accent"
              @click="handlePublish(sec)">发布</button>
            <button v-else-if="sec.status === 'PUBLISHED'"
              class="insp-btn insp-btn--sm"
              @click="goVersions(sec)">版本</button>
            <button v-else-if="sec.status === 'DEPRECATED'"
              class="insp-btn insp-btn--sm"
              @click="handleDuplicate(sec)">复制为新模板</button>
            <button class="insp-btn insp-btn--sm" @click="goEdit(sec)">编辑</button>
            <button class="insp-btn insp-btn--sm insp-btn--ghost" @click.stop="toggleDropdown(sec.id, $event)">
              <MoreHorizontal :size="13" />
            </button>
          </div>
        </li>
      </ul>
      </div>
    </section>

    <!-- Dropdown Menu (Teleported to body) -->
    <Teleport to="body">
      <Transition name="dropdown">
        <div v-if="openDropdownId !== null" class="dropdown-menu" :style="{ top: dropdownPos.top, right: dropdownPos.right }" @click.stop>
          <template v-for="sec in filteredSections" :key="sec.id">
            <template v-if="sec.id === openDropdownId">
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

    <!-- 模板悬浮预览卡 (S+ 标志) -->
    <Teleport to="body">
      <Transition name="popover">
        <div v-if="previewVisible && previewTarget"
             class="cfg-popover"
             :class="`cfg-popover--${previewPos.placement}`"
             :style="{ top: previewPos.top, left: previewPos.left }"
             @mouseenter="keepPreview"
             @mouseleave="hidePreview">
          <div class="cfg-popover__head">
            <span class="cfg-popover__chip"
                  :style="{ background: thumbGradient(previewTarget.sectionName) }">
              {{ previewTarget.sectionName.slice(0, 1) }}
            </span>
            <div class="cfg-popover__head-text">
              <div class="cfg-popover__title">{{ previewTarget.sectionName }}</div>
              <div class="cfg-popover__sub">
                v{{ previewTarget.latestVersion }} · {{ TemplateStatusConfig[previewTarget.status]?.label }}
              </div>
            </div>
          </div>
          <div class="cfg-popover__body">
            <template v-if="previewData.loading">
              <div class="cfg-popover__loading">
                <span class="sk-line sk-line--md" />
                <span class="sk-line sk-line--sm" />
                <span class="sk-line sk-line--md" />
              </div>
            </template>
            <template v-else>
              <!-- 评分模式分布 -->
              <div v-if="Object.keys(previewData.scoringModes).length > 0" class="cfg-popover__modes">
                <div class="cfg-popover__label">评分模式分布</div>
                <div class="cfg-popover__mode-bars">
                  <span v-for="(cnt, mode) in previewData.scoringModes" :key="mode"
                        class="cfg-popover__mode"
                        :title="`${mode} · ${cnt} 项`">
                    <span class="cfg-popover__mode-name">{{ mode }}</span>
                    <span class="cfg-popover__mode-count insp-num">{{ cnt }}</span>
                  </span>
                </div>
              </div>
              <!-- 分区树 -->
              <div class="cfg-popover__sections">
                <div class="cfg-popover__label">
                  分区结构
                  <span class="cfg-popover__total">
                    {{ previewData.sections.length }} 分区 · {{ previewData.itemTotal }} 检查项
                  </span>
                </div>
                <ul class="cfg-popover__sec-list">
                  <li v-for="s in previewData.sections.slice(0, 8)" :key="s.id" class="cfg-popover__sec-item">
                    <span class="cfg-popover__sec-dot" />
                    <span class="cfg-popover__sec-name">{{ s.sectionName }}</span>
                  </li>
                  <li v-if="previewData.sections.length > 8" class="cfg-popover__sec-more">
                    + {{ previewData.sections.length - 8 }} 更多
                  </li>
                  <li v-if="previewData.sections.length === 0" class="cfg-popover__sec-empty">
                    — 暂无分区, 点击编辑去添加 —
                  </li>
                </ul>
              </div>
            </template>
          </div>
          <div class="cfg-popover__foot">
            <span class="cfg-popover__hint">点击卡片进入编辑</span>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 批量发布预校验 Dialog (P3) -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="precheckDialogVisible" class="modal-mask"
             @mousedown="onMaskMouseDown"
             @click="onMaskClick($event, () => { if (!precheckRunning && !precheckPublishing) precheckDialogVisible = false })">
          <div class="modal-box modal-box--wide">
            <div class="modal-head">
              <h3>批量发布 · 预校验</h3>
              <button class="modal-close" :disabled="precheckRunning || precheckPublishing"
                      @click="precheckDialogVisible = false">&times;</button>
            </div>
            <div class="modal-body">
              <div class="precheck-summary">
                <span v-if="precheckRunning" class="precheck-summary__loading">
                  正在校验 <span class="insp-num">{{ precheckRows.length }}</span> 个模板…
                </span>
                <template v-else>
                  <span class="precheck-summary__ok">
                    <CheckCircle2 :size="14" />
                    可发布 <strong class="insp-num">{{ precheckRows.filter(r => r.ok).length }}</strong>
                  </span>
                  <span v-if="precheckRows.some(r => !r.ok)" class="precheck-summary__fail">
                    <AlertTriangle :size="14" />
                    待修复 <strong class="insp-num">{{ precheckRows.filter(r => !r.ok).length }}</strong>
                  </span>
                </template>
              </div>
              <ul class="precheck-list">
                <li v-for="row in precheckRows" :key="row.id" class="precheck-row" :class="{ 'is-fail': !row.ok }">
                  <span class="precheck-row__icon">
                    <CheckCircle2 v-if="row.ok" :size="14" style="color: var(--insp-pass)" />
                    <AlertTriangle v-else :size="14" style="color: var(--insp-warn)" />
                  </span>
                  <div class="precheck-row__main">
                    <div class="precheck-row__name">{{ row.name }}</div>
                    <div class="precheck-row__meta">
                      <span><span class="insp-num">{{ row.sectionCount }}</span> 分区</span>
                      <span class="precheck-row__sep">·</span>
                      <span><span class="insp-num">{{ row.itemCount }}</span> 检查项</span>
                      <template v-if="row.reason">
                        <span class="precheck-row__sep">·</span>
                        <span class="precheck-row__reason">{{ row.reason }}</span>
                      </template>
                    </div>
                  </div>
                </li>
              </ul>
            </div>
            <div class="modal-foot">
              <button class="btn-ghost" :disabled="precheckPublishing" @click="precheckDialogVisible = false">
                取消
              </button>
              <button class="insp-btn insp-btn--accent"
                      :disabled="precheckRunning || precheckPublishing || precheckRows.filter(r => r.ok).length === 0"
                      @click="confirmBatchPublishAfterPrecheck">
                {{ precheckPublishing ? '发布中…' : `发布 ${precheckRows.filter(r => r.ok).length} 条可用模板` }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Versions Dialog (P1-163) -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="versionsDialogVisible" class="modal-mask"
             @mousedown="onMaskMouseDown"
             @click="onMaskClick($event, () => versionsDialogVisible = false)">
          <div class="modal-box modal-box--wide">
            <div class="modal-head">
              <h3>版本历史 · {{ versionsTarget?.sectionName }}</h3>
              <button class="modal-close" @click="versionsDialogVisible = false">&times;</button>
            </div>
            <div class="modal-body">
              <div v-if="versionsList.length === 0" class="ver-empty">暂无已发布版本</div>
              <ul v-else class="ver-list">
                <li v-for="v in versionsList" :key="v.id" class="ver-item">
                  <div class="ver-item__head">
                    <span class="ver-num insp-num">v{{ v.version }}</span>
                    <span v-if="v.version === versionsTarget?.latestVersion" class="ver-badge">最新</span>
                    <span class="ver-time insp-num">{{ formatDate(v.createdAt) }}</span>
                  </div>
                  <div class="ver-item__meta">
                    <span class="ver-meta-item">快照大小 <span class="insp-num">{{
                      v.structureSnapshot ? Math.round(v.structureSnapshot.length / 1024) : 0
                    }} KB</span></span>
                    <span v-if="v.createdBy" class="ver-meta-item">创建人 <span class="insp-num">#{{ v.createdBy }}</span></span>
                  </div>
                </li>
              </ul>
            </div>
            <div class="modal-foot">
              <button class="btn-ghost" @click="versionsDialogVisible = false">关闭</button>
            </div>
          </div>
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
                <label>检查对象类型 <span class="req">*</span></label>
                <div class="seg">
                  <button type="button" class="seg-btn"
                    :class="{ 'is-on': createForm.targetType === 'ORG' }"
                    @click="createForm.targetType = 'ORG'">组织</button>
                  <button type="button" class="seg-btn"
                    :class="{ 'is-on': createForm.targetType === 'USER' }"
                    @click="createForm.targetType = 'USER'">人员</button>
                  <button type="button" class="seg-btn"
                    :class="{ 'is-on': createForm.targetType === 'PLACE' }"
                    @click="createForm.targetType = 'PLACE'">场所</button>
                </div>
                <p class="fld-hint">决定模板适用的检查对象, 创建后可在编辑器中调整子分区类型</p>
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

<style scoped src="./InspectionConfigView.css"></style>

