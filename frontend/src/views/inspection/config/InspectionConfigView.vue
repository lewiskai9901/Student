<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Search, Copy, Upload, Archive, Ban, Trash2,
  Pencil, MoreHorizontal, LayoutGrid, FileText,
  Library, ListTree, Award, Tag, ArrowRight, List, Rows3,
} from 'lucide-vue-next'
import { useInspTemplateStore } from '@/stores/inspection/inspTemplateStore'
import { inspTemplateApi } from '@/api/inspection/template'
import { TemplateStatusConfig, TargetTypeConfig, type TemplateStatus, type TargetType } from '@/types/insp/enums'
import type { TemplateSection } from '@/types/insp/template'

const router = useRouter()
const templateStore = useInspTemplateStore()

// ==================== 快捷入口 (替代假 tab — 4 个 ↗ 不再伪装成 tab) ====================
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
type ViewMode = 'compact' | 'standard'
const viewMode = ref<ViewMode>((localStorage.getItem('insp_cfg_view_mode') as ViewMode) || 'compact')
function setViewMode(m: ViewMode) {
  viewMode.value = m
  localStorage.setItem('insp_cfg_view_mode', m)
}

// ==================== State ====================
const loading = ref(false)
const rootSections = ref<TemplateSection[]>([])
const total = ref(0)
const childSectionsMap = ref<Map<number, TemplateSection[]>>(new Map())
// P1-160: 使用计数 (rootSectionId → 在用项目数)
const usageMap = ref<Record<number, number>>({})

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

    // P1-160: 批量取使用计数 — 一次请求, 不用 N+1
    try {
      const ids = result.records.map(r => Number(r.id))
      usageMap.value = await inspTemplateApi.getRootSectionUsage(ids)
    } catch { usageMap.value = {} }

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
  const inUse = usageMap.value[Number(section.id)] || 0
  const tip = inUse > 0
    ? `⚠ 此模板正被 ${inUse} 个进行中的项目引用. 废弃后这些项目将继续运行 (使用快照), 但新项目无法选用此模板. 确认?`
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
  const inUse = usageMap.value[Number(section.id)] || 0
  const tip = inUse > 0
    ? `⚠ 此模板正被 ${inUse} 个项目引用. 归档后将从默认列表隐藏. 确认?`
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
        </div>
      </div>
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

      <ul v-else class="tpl-rows" :class="`tpl-rows--${viewMode}`">
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
                    :class="`insp-chip--${({DRAFT:'pending',PUBLISHED:'pass',DEPRECATED:'warn',ARCHIVED:'pending'} as any)[sec.status]}`">
                {{ TemplateStatusConfig[sec.status]?.label }}
              </span>
              <span class="tpl-row__version insp-num">v{{ sec.latestVersion }}</span>
              <template v-if="getTargetTypes(sec.id).length > 0">
                <span v-for="tt in getTargetTypes(sec.id)" :key="tt" class="tpl-row__tt">
                  {{ TargetTypeConfig[tt]?.label }}
                </span>
              </template>
              <template v-if="usageMap[Number(sec.id)] > 0">
                <span class="tpl-row__usage" :title="`此模板正在被 ${usageMap[Number(sec.id)]} 个项目引用`">
                  <span class="insp-num">{{ usageMap[Number(sec.id)] }}</span> 项目在用
                </span>
              </template>
            </div>
            <div class="tpl-row__meta">
              <span class="tpl-row__metric"><span class="insp-num">{{ getSectionCount(sec.id) }}</span> 分区</span>
              <span class="tpl-row__sep">·</span>
              <span class="tpl-row__date" :title="sec.updatedAt">更新于 <span class="insp-num">{{ formatDate(sec.updatedAt) }}</span></span>
              <template v-if="(sec as any).createdBy">
                <span class="tpl-row__sep">·</span>
                <span class="tpl-row__owner">创建人 <span class="insp-num">#{{ (sec as any).createdBy }}</span></span>
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

/* ─ 快捷入口 (替代假 tab) ─────── */
.cfg-quick {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 14px;
}
.cfg-quick__card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  cursor: pointer;
  transition: all var(--insp-t-fast);
  text-align: left;
  font-family: inherit;
}
.cfg-quick__card:hover {
  border-color: var(--insp-accent);
  background: var(--insp-accent-paler);
}
.cfg-quick__card:hover .cfg-quick__chev { color: var(--insp-accent); transform: translateX(2px); }
.cfg-quick__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px; height: 28px;
  background: var(--insp-bg-subtle);
  color: var(--insp-ink-secondary);
  border-radius: var(--insp-radius-sm);
  flex-shrink: 0;
}
.cfg-quick__card:hover .cfg-quick__icon {
  background: var(--insp-accent);
  color: white;
}
.cfg-quick__body {
  display: flex;
  flex-direction: column;
  gap: 1px;
  flex: 1;
  min-width: 0;
}
.cfg-quick__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--insp-ink-primary);
  line-height: 1.2;
}
.cfg-quick__desc {
  font-size: 10px;
  color: var(--insp-ink-tertiary);
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.cfg-quick__chev {
  color: var(--insp-ink-quaternary);
  flex-shrink: 0;
  transition: all var(--insp-t-fast);
}

/* 主区块标题 */
.cfg-section-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 10px;
  padding: 0 2px;
}
.cfg-section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--insp-ink-primary);
  margin: 0;
  letter-spacing: -0.01em;
}
.cfg-section-hint {
  font-size: 11px;
  color: var(--insp-ink-tertiary);
}

/* ─ 视图模式切换 ─────── */
.cfg-toolbar__right {
  display: flex; align-items: center; gap: 10px;
}
.cfg-view-toggle {
  display: inline-flex;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-sm);
  overflow: hidden;
}
.cfg-view-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 26px; height: 26px;
  background: transparent;
  border: 0;
  color: var(--insp-ink-tertiary);
  cursor: pointer;
  transition: all var(--insp-t-fast);
}
.cfg-view-btn:hover { color: var(--insp-ink-primary); }
.cfg-view-btn.is-active {
  background: var(--insp-accent);
  color: white;
}
.cfg-view-btn + .cfg-view-btn {
  border-left: 1px solid var(--insp-border-default);
}
.cfg-view-btn.is-active + .cfg-view-btn,
.cfg-view-btn:has(+ .is-active),
.cfg-view-toggle .is-active { border-left-color: var(--insp-accent); }

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
.tpl-row__usage {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 1px 6px;
  background: var(--insp-info-pale, var(--insp-bg-subtle));
  border-radius: 3px;
  color: var(--insp-info);
  font-weight: 500;
  font-size: 10px;
}
.tpl-row__metric { color: var(--insp-ink-secondary); font-weight: 500; }
.tpl-row__date { color: var(--insp-ink-tertiary); }
.tpl-row__owner { color: var(--insp-ink-tertiary); }
.tpl-row__code {
  font-family: var(--insp-font-mono);
  font-size: 10px;
  color: var(--insp-ink-quaternary);
  padding: 0 4px;
  background: var(--insp-bg-subtle);
  border-radius: 2px;
}

/* 紧凑模式 */
.tpl-rows--compact .tpl-row {
  padding: 7px 14px;
  grid-template-columns: 32px 1fr auto;
  gap: 10px;
}
.tpl-rows--compact .tpl-row__main { gap: 2px; }
.tpl-rows--compact .tpl-row__line1 { gap: 6px; }
.tpl-rows--compact .tpl-row__name { font-size: 12.5px; }
.tpl-rows--compact .tpl-row__meta { font-size: 10.5px; gap: 5px; }
.tpl-rows--compact .tpl-row__num { font-size: 11px; }

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
.modal-box--wide { width: 640px; }

/* Versions list */
.ver-empty {
  padding: 30px 0; text-align: center;
  color: var(--insp-ink-tertiary); font-size: 12px;
}
.ver-list {
  list-style: none; margin: 0; padding: 0;
  display: flex; flex-direction: column; gap: 8px;
}
.ver-item {
  border: 1px solid var(--insp-border-subtle);
  border-radius: var(--insp-radius-sm);
  padding: 10px 12px;
  background: var(--insp-bg-page);
}
.ver-item__head { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.ver-num {
  font-family: var(--insp-font-mono);
  font-size: 14px; font-weight: 700;
  color: var(--insp-accent);
}
.ver-badge {
  font-size: 10px; padding: 1px 6px;
  border-radius: 3px;
  background: var(--insp-pass-pale, #dcfce7);
  color: var(--insp-pass);
  font-weight: 600;
}
.ver-time { font-size: 11px; color: var(--insp-ink-tertiary); margin-left: auto; }
.ver-item__meta { display: flex; gap: 12px; font-size: 11px; color: var(--insp-ink-tertiary); }
.ver-meta-item .insp-num { color: var(--insp-ink-secondary); font-weight: 600; }
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
.fld-hint {
  font-size: 11px;
  color: var(--insp-ink-tertiary);
  margin: 2px 0 0;
}
.seg {
  display: inline-flex; gap: 0;
  background: var(--insp-bg-subtle);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-sm);
  padding: 2px;
}
.seg-btn {
  padding: 4px 14px;
  background: transparent; border: 0;
  font-family: inherit; font-size: 12px; font-weight: 500;
  color: var(--insp-ink-tertiary);
  border-radius: 4px;
  cursor: pointer; transition: all var(--insp-t-fast);
}
.seg-btn:hover { color: var(--insp-ink-primary); }
.seg-btn.is-on {
  background: var(--insp-bg-surface);
  color: var(--insp-accent);
  font-weight: 600;
  box-shadow: var(--insp-shadow-xs);
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

