<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Search, Copy, Upload, Archive, Ban, Trash2,
  Pencil, ChevronRight, ChevronDown,
  FolderTree, ListChecks, PanelLeftClose, PanelLeft,
  Settings2, MoreHorizontal, ExternalLink,
  FolderOpen, Layers,
} from 'lucide-vue-next'
import { useInspTemplateStore } from '@/stores/insp/inspTemplateStore'
import { catalogApi } from '@/api/insp/catalog'
import { responseSetApi } from '@/api/insp/responseSet'
import { TemplateStatusConfig, TargetTypeConfig, type TemplateStatus, type TargetType } from '@/types/insp/enums'
import type { InspTemplate, TemplateCatalog, TemplateCatalogTreeNode, ResponseSet, ResponseSetOption, CreateCatalogRequest, UpdateCatalogRequest, CreateOptionRequest, UpdateOptionRequest } from '@/types/insp/template'

const router = useRouter()
const templateStore = useInspTemplateStore()

// ==================== Shared State ====================
const activeTab = ref<'templates' | 'response-sets'>('templates')

// ==================== Tab 1: Template + Catalog ====================
const tplLoading = ref(false)
const templates = ref<InspTemplate[]>([])
const tplTotal = ref(0)
const catalogTree = ref<TemplateCatalogTreeNode[]>([])
const flatCatalogs = ref<TemplateCatalog[]>([])
const expandedCatalogIds = ref<Set<number>>(new Set())
const selectedCatalogId = ref<number | undefined>(undefined)
const sidebarCollapsed = ref(false)

const tplQuery = reactive({
  page: 1, size: 20,
  keyword: '',
  status: undefined as TemplateStatus | undefined,
  catalogId: undefined as number | undefined,
})

// Catalog dialog
const catalogDialogVisible = ref(false)
const catalogDialogTitle = ref('创建分类')
const editingCatalogId = ref<number | null>(null)
const catalogForm = ref({
  catalogCode: '', catalogName: '', parentId: null as number | null,
  description: '', sortOrder: 0, isEnabled: true,
})

// Create template dialog
const createTplDialogVisible = ref(false)
const createTplForm = ref({ templateName: '', description: '', targetType: 'ORG' as TargetType })

// Modal close guard: only close when both mousedown and mouseup happen on mask
const maskMouseDownTarget = ref<EventTarget | null>(null)
function onMaskMouseDown(e: MouseEvent) { maskMouseDownTarget.value = e.target }
function onMaskClick(e: MouseEvent, closeFn: () => void) {
  if (e.target === e.currentTarget && maskMouseDownTarget.value === e.currentTarget) closeFn()
  maskMouseDownTarget.value = null
}

// Template action dropdown
const openDropdownId = ref<number | null>(null)
function toggleDropdown(id: number) {
  openDropdownId.value = openDropdownId.value === id ? null : id
}
function closeDropdowns() { openDropdownId.value = null }

async function loadTemplates() {
  tplLoading.value = true
  try {
    tplQuery.catalogId = selectedCatalogId.value
    const result = await templateStore.loadTemplates(tplQuery)
    templates.value = result.records
    tplTotal.value = result.total
  } catch (e: any) {
    ElMessage.error(e.message || '加载模板失败')
  } finally {
    tplLoading.value = false
  }
}

async function loadCatalogs() {
  try {
    const [list, tree] = await Promise.all([catalogApi.getList(), catalogApi.getTree()])
    flatCatalogs.value = list
    catalogTree.value = tree
    tree.forEach((n: TemplateCatalogTreeNode) => {
      if (n.children?.length) expandedCatalogIds.value.add(n.id)
    })
  } catch { /* ignore */ }
}

function selectCatalog(id: number | undefined) {
  selectedCatalogId.value = selectedCatalogId.value === id ? undefined : id
  tplQuery.page = 1
  loadTemplates()
}

function toggleCatalogExpand(id: number) {
  expandedCatalogIds.value.has(id)
    ? expandedCatalogIds.value.delete(id)
    : expandedCatalogIds.value.add(id)
}

function handleTplSearch() { tplQuery.page = 1; loadTemplates() }
function resetTplQuery() {
  tplQuery.keyword = ''; tplQuery.status = undefined
  selectedCatalogId.value = undefined; tplQuery.page = 1
  loadTemplates()
}
function handleTplPageChange(page: number) { tplQuery.page = page; loadTemplates() }

function goCreateTemplate() {
  createTplForm.value = { templateName: '', description: '', targetType: 'ORG' }
  createTplDialogVisible.value = true
}
async function handleCreateTemplate() {
  const name = createTplForm.value.templateName.trim()
  if (!name) { ElMessage.warning('请输入模板名称'); return }
  try {
    const tpl = await templateStore.addTemplate({ templateName: name, description: createTplForm.value.description, targetType: createTplForm.value.targetType })
    createTplDialogVisible.value = false
    router.push(`/inspection/v7/templates/${tpl.id}/edit`)
  } catch (e: any) {
    ElMessage.error(e.message || '创建模板失败')
  }
}
function goEditTemplate(tpl: InspTemplate) { router.push(`/inspection/v7/templates/${tpl.id}/edit`) }

async function handlePublish(tpl: InspTemplate) {
  try {
    await ElMessageBox.confirm('发布后将创建不可变版本快照，确认发布？', '确认发布', { type: 'warning' })
    await templateStore.publish(tpl.id); ElMessage.success('发布成功'); loadTemplates()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '发布失败') }
  closeDropdowns()
}
async function handleDeprecate(tpl: InspTemplate) {
  try {
    await ElMessageBox.confirm('废弃后新项目无法使用此模板', '确认废弃', { type: 'warning' })
    await templateStore.deprecate(tpl.id); ElMessage.success('已废弃'); loadTemplates()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
  closeDropdowns()
}
async function handleArchive(tpl: InspTemplate) {
  try {
    await ElMessageBox.confirm('归档后模板将不可见', '确认归档', { type: 'warning' })
    await templateStore.archive(tpl.id); ElMessage.success('已归档'); loadTemplates()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
  closeDropdowns()
}
async function handleDuplicate(tpl: InspTemplate) {
  try { await templateStore.duplicate(tpl.id); ElMessage.success('复制成功'); loadTemplates() }
  catch (e: any) { ElMessage.error(e.message || '复制失败') }
  closeDropdowns()
}
async function handleDeleteTemplate(tpl: InspTemplate) {
  try {
    await ElMessageBox.confirm(`确认删除「${tpl.templateName}」？`, '确认删除', { type: 'warning' })
    await templateStore.removeTemplate(tpl.id); ElMessage.success('已删除'); loadTemplates()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
  closeDropdowns()
}

// Catalog CRUD
function openCreateCatalog(parentId?: number) {
  editingCatalogId.value = null; catalogDialogTitle.value = '创建分类'
  catalogForm.value = { catalogCode: '', catalogName: '', parentId: parentId || null, description: '', sortOrder: 0, isEnabled: true }
  catalogDialogVisible.value = true
}
function openEditCatalog(cat: TemplateCatalog) {
  editingCatalogId.value = cat.id; catalogDialogTitle.value = '编辑分类'
  catalogForm.value = { catalogCode: cat.catalogCode, catalogName: cat.catalogName, parentId: cat.parentId, description: cat.description || '', sortOrder: cat.sortOrder, isEnabled: cat.isEnabled }
  catalogDialogVisible.value = true
}
async function handleSaveCatalog() {
  if (!catalogForm.value.catalogName.trim()) { ElMessage.warning('请输入分类名称'); return }
  try {
    if (editingCatalogId.value) {
      await catalogApi.update(editingCatalogId.value, { catalogName: catalogForm.value.catalogName, description: catalogForm.value.description || undefined, parentId: catalogForm.value.parentId, sortOrder: catalogForm.value.sortOrder, isEnabled: catalogForm.value.isEnabled } as UpdateCatalogRequest)
    } else {
      if (!catalogForm.value.catalogCode.trim()) { ElMessage.warning('请输入编码'); return }
      await catalogApi.create({ catalogCode: catalogForm.value.catalogCode, catalogName: catalogForm.value.catalogName, parentId: catalogForm.value.parentId, description: catalogForm.value.description || undefined, sortOrder: catalogForm.value.sortOrder } as CreateCatalogRequest)
    }
    ElMessage.success('保存成功'); catalogDialogVisible.value = false; loadCatalogs()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}
async function handleDeleteCatalog(cat: TemplateCatalog) {
  try {
    await ElMessageBox.confirm(`确认删除「${cat.catalogName}」？`, '确认删除', { type: 'warning' })
    await catalogApi.delete(cat.id); ElMessage.success('已删除')
    if (selectedCatalogId.value === cat.id) { selectedCatalogId.value = undefined }
    loadCatalogs(); loadTemplates()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
}

const statusOptions = computed(() =>
  Object.entries(TemplateStatusConfig).map(([key, val]) => ({ value: key, label: val.label }))
)

function getCatalogName(id: number | undefined) {
  if (!id) return ''
  const cat = flatCatalogs.value.find(c => c.id === id)
  return cat?.catalogName || ''
}

function formatTime(t?: string) {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 16)
}

// ==================== Tab 2: Response Sets ====================
const rsLoading = ref(false)
const rsSets = ref<ResponseSet[]>([])
const rsTotal = ref(0)
const rsQuery = reactive({ page: 1, size: 20, keyword: '' })
const rsSelectedSet = ref<ResponseSet | null>(null)
const rsOptions = ref<ResponseSetOption[]>([])
const rsOptionsLoading = ref(false)

// Set dialog
const rsSetDialogVisible = ref(false)
const rsSetDialogTitle = ref('创建选项集')
const rsEditingSetId = ref<number | null>(null)
const rsSetForm = ref({ setCode: '', setName: '', isGlobal: true })

// Option dialog
const rsOptionDialogVisible = ref(false)
const rsEditingOptionId = ref<number | null>(null)
const rsOptionForm = ref({ optionValue: '', optionLabel: '', optionColor: '#409EFF', score: 0, isFlagged: false, sortOrder: 0 })

async function loadResponseSets() {
  rsLoading.value = true
  try {
    const result = await responseSetApi.getList(rsQuery)
    rsSets.value = result.records; rsTotal.value = result.total
  } catch (e: any) { ElMessage.error(e.message || '加载选项集失败') }
  finally { rsLoading.value = false }
}
function handleRsSearch() { rsQuery.page = 1; loadResponseSets() }

async function selectResponseSet(s: ResponseSet) {
  rsSelectedSet.value = s; rsOptionsLoading.value = true
  try { rsOptions.value = await responseSetApi.getOptions(s.id) }
  catch (e: any) { ElMessage.error(e.message || '加载选项失败') }
  finally { rsOptionsLoading.value = false }
}

function openCreateSet() {
  rsEditingSetId.value = null; rsSetDialogTitle.value = '创建选项集'
  rsSetForm.value = { setCode: '', setName: '', isGlobal: true }; rsSetDialogVisible.value = true
}
function openEditSet(s: ResponseSet) {
  rsEditingSetId.value = s.id; rsSetDialogTitle.value = '编辑选项集'
  rsSetForm.value = { setCode: s.setCode, setName: s.setName, isGlobal: s.isGlobal }; rsSetDialogVisible.value = true
}
async function handleSaveSet() {
  if (!rsSetForm.value.setName.trim()) { ElMessage.warning('请输入名称'); return }
  try {
    if (rsEditingSetId.value) {
      await responseSetApi.update(rsEditingSetId.value, { setName: rsSetForm.value.setName, isGlobal: rsSetForm.value.isGlobal })
    } else {
      if (!rsSetForm.value.setCode.trim()) { ElMessage.warning('请输入编码'); return }
      await responseSetApi.create({ setCode: rsSetForm.value.setCode, setName: rsSetForm.value.setName, isGlobal: rsSetForm.value.isGlobal })
    }
    ElMessage.success('保存成功'); rsSetDialogVisible.value = false; loadResponseSets()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}
async function handleDeleteSet(s: ResponseSet) {
  try {
    await ElMessageBox.confirm(`确认删除「${s.setName}」？`, '确认删除', { type: 'warning' })
    await responseSetApi.delete(s.id); ElMessage.success('已删除')
    if (rsSelectedSet.value?.id === s.id) { rsSelectedSet.value = null; rsOptions.value = [] }
    loadResponseSets()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
}

function openCreateOption() {
  if (!rsSelectedSet.value) return
  rsEditingOptionId.value = null
  rsOptionForm.value = { optionValue: '', optionLabel: '', optionColor: '#409EFF', score: 0, isFlagged: false, sortOrder: rsOptions.value.length + 1 }
  rsOptionDialogVisible.value = true
}
function openEditOption(opt: ResponseSetOption) {
  rsEditingOptionId.value = opt.id
  rsOptionForm.value = { optionValue: opt.optionValue, optionLabel: opt.optionLabel, optionColor: opt.optionColor || '#409EFF', score: opt.score || 0, isFlagged: opt.isFlagged, sortOrder: opt.sortOrder }
  rsOptionDialogVisible.value = true
}
async function handleSaveOption() {
  if (!rsSelectedSet.value) return
  if (!rsOptionForm.value.optionValue.trim() || !rsOptionForm.value.optionLabel.trim()) { ElMessage.warning('请填写选项值和标签'); return }
  try {
    if (rsEditingOptionId.value) {
      await responseSetApi.updateOption(rsSelectedSet.value.id, rsEditingOptionId.value, { optionLabel: rsOptionForm.value.optionLabel, optionColor: rsOptionForm.value.optionColor, score: rsOptionForm.value.score, isFlagged: rsOptionForm.value.isFlagged, sortOrder: rsOptionForm.value.sortOrder } as UpdateOptionRequest)
    } else {
      await responseSetApi.addOption(rsSelectedSet.value.id, { optionValue: rsOptionForm.value.optionValue, optionLabel: rsOptionForm.value.optionLabel, optionColor: rsOptionForm.value.optionColor, score: rsOptionForm.value.score, isFlagged: rsOptionForm.value.isFlagged, sortOrder: rsOptionForm.value.sortOrder } as CreateOptionRequest)
    }
    ElMessage.success('保存成功'); rsOptionDialogVisible.value = false; selectResponseSet(rsSelectedSet.value)
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}
async function handleDeleteOption(opt: ResponseSetOption) {
  if (!rsSelectedSet.value) return
  try {
    await responseSetApi.deleteOption(rsSelectedSet.value.id, opt.id)
    ElMessage.success('已删除'); selectResponseSet(rsSelectedSet.value)
  } catch (e: any) { ElMessage.error(e.message || '删除失败') }
}

// ==================== Lifecycle ====================
onMounted(() => {
  loadTemplates()
  loadCatalogs()
  loadResponseSets()
})
</script>

<template>
  <div class="cfg" @click="closeDropdowns">
    <!-- ===== Tab Bar ===== -->
    <div class="cfg-tabbar">
      <div class="cfg-tabs">
        <button
          class="cfg-tab" :class="{ active: activeTab === 'templates' }"
          @click="activeTab = 'templates'"
        >
          <Layers :size="16" />
          模板管理
          <span class="cfg-tab-badge">{{ tplTotal }}</span>
        </button>
        <button
          class="cfg-tab" :class="{ active: activeTab === 'response-sets' }"
          @click="activeTab = 'response-sets'"
        >
          <ListChecks :size="16" />
          选项集
          <span class="cfg-tab-badge">{{ rsTotal }}</span>
        </button>
      </div>
    </div>

    <!-- ==================== Tab 1: Template Management ==================== -->
    <div v-show="activeTab === 'templates'" class="cfg-body tpl-layout">
      <!-- Catalog Sidebar -->
      <aside class="cat-side" :class="{ collapsed: sidebarCollapsed }">
        <div v-if="!sidebarCollapsed" class="cat-side-inner">
          <div class="cat-head">
            <span class="cat-head-label">模板分类</span>
            <div class="cat-head-acts">
              <button class="ic-btn" title="新建分类" @click.stop="openCreateCatalog()"><Plus :size="15" /></button>
              <button class="ic-btn" title="收起" @click="sidebarCollapsed = true"><PanelLeftClose :size="15" /></button>
            </div>
          </div>

          <div class="cat-list">
            <!-- All -->
            <div
              class="cat-node" :class="{ sel: !selectedCatalogId }"
              @click="selectCatalog(undefined)"
            >
              <FolderOpen :size="15" class="cat-icon" />
              <span class="cat-label">全部模板</span>
            </div>

            <!-- Tree -->
            <template v-for="node in catalogTree" :key="node.id">
              <div class="cat-node" :class="{ sel: selectedCatalogId === node.id }">
                <div class="cat-node-left" @click="selectCatalog(node.id)">
                  <button
                    v-if="node.children?.length"
                    class="cat-expand"
                    @click.stop="toggleCatalogExpand(node.id)"
                  >
                    <ChevronDown v-if="expandedCatalogIds.has(node.id)" :size="13" />
                    <ChevronRight v-else :size="13" />
                  </button>
                  <FolderTree v-else :size="14" class="cat-icon-leaf" />
                  <span class="cat-label">{{ node.catalogName }}</span>
                </div>
                <div class="cat-node-acts">
                  <button class="ic-btn-s" @click.stop="openCreateCatalog(node.id)" title="添加子分类"><Plus :size="13" /></button>
                  <button class="ic-btn-s" @click.stop="openEditCatalog(node)" title="编辑"><Pencil :size="13" /></button>
                  <button class="ic-btn-s danger" @click.stop="handleDeleteCatalog(node)" title="删除"><Trash2 :size="13" /></button>
                </div>
              </div>
              <!-- Children -->
              <template v-if="expandedCatalogIds.has(node.id) && node.children">
                <div
                  v-for="child in node.children" :key="child.id"
                  class="cat-node child" :class="{ sel: selectedCatalogId === child.id }"
                >
                  <div class="cat-node-left" @click="selectCatalog(child.id)">
                    <span class="cat-label">{{ child.catalogName }}</span>
                  </div>
                  <div class="cat-node-acts">
                    <button class="ic-btn-s" @click.stop="openEditCatalog(child)"><Pencil :size="13" /></button>
                    <button class="ic-btn-s danger" @click.stop="handleDeleteCatalog(child)"><Trash2 :size="13" /></button>
                  </div>
                </div>
              </template>
            </template>
          </div>
        </div>

        <div v-else class="cat-side-collapsed">
          <button class="ic-btn" title="展开分类" @click="sidebarCollapsed = false">
            <PanelLeft :size="16" />
          </button>
        </div>
      </aside>

      <!-- Template List -->
      <section class="tpl-main">
        <!-- Toolbar -->
        <div class="tpl-bar">
          <div class="tpl-bar-left">
            <div class="tpl-search">
              <Search :size="14" class="tpl-search-icon" />
              <input
                v-model="tplQuery.keyword" type="text" placeholder="搜索模板名称..."
                @keyup.enter="handleTplSearch"
              />
            </div>
            <select v-model="tplQuery.status" class="tpl-select" @change="handleTplSearch">
              <option :value="undefined">全部状态</option>
              <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
            <button v-if="tplQuery.keyword || tplQuery.status || selectedCatalogId" class="tpl-reset" @click="resetTplQuery">重置</button>
          </div>
          <button class="btn-primary" @click="goCreateTemplate">
            <Plus :size="15" /> 新建模板
          </button>
        </div>

        <!-- Active filter hint -->
        <div v-if="selectedCatalogId" class="tpl-filter-hint">
          当前分类：<strong>{{ getCatalogName(selectedCatalogId) }}</strong>
          <button @click="selectCatalog(undefined)">清除筛选</button>
        </div>

        <!-- Template Cards -->
        <div v-if="tplLoading" class="state-loading">
          <div class="spinner" />
          <span>加载中...</span>
        </div>
        <div v-else-if="templates.length === 0" class="state-empty">
          <Layers :size="36" />
          <p>暂无模板数据</p>
          <button class="btn-primary sm" @click="goCreateTemplate">创建第一个模板</button>
        </div>
        <div v-else class="tpl-cards">
          <div v-for="tpl in templates" :key="tpl.id" class="tpl-card" @click="goEditTemplate(tpl)">
            <div class="tpl-card-body">
              <!-- Left: info -->
              <div class="tpl-card-info">
                <div class="tpl-card-top">
                  <h3 class="tpl-card-name">{{ tpl.templateName }}</h3>
                  <span
                    class="tpl-status-tag"
                    :class="'st-' + tpl.status.toLowerCase()"
                  >{{ TemplateStatusConfig[tpl.status]?.label }}</span>
                </div>
                <div class="tpl-card-meta">
                  <span class="meta-item">{{ tpl.templateCode }}</span>
                  <span class="meta-sep">|</span>
                  <span class="meta-item tpl-target-tag">{{ TargetTypeConfig[tpl.targetType]?.label || '组织' }}</span>
                  <span class="meta-sep">|</span>
                  <span class="meta-item">v{{ tpl.latestVersion }}</span>
                  <span class="meta-sep">|</span>
                  <span class="meta-item">{{ formatTime(tpl.updatedAt) }}</span>
                  <template v-if="tpl.catalogId">
                    <span class="meta-sep">|</span>
                    <span class="meta-item cat-tag">{{ getCatalogName(tpl.catalogId) }}</span>
                  </template>
                </div>
              </div>
              <!-- Right: actions -->
              <div class="tpl-card-right">
                <div class="tpl-actions" @click.stop>
                  <button class="ic-btn" title="编辑" @click="goEditTemplate(tpl)"><ExternalLink :size="16" /></button>
                  <div class="dropdown-wrap">
                    <button class="ic-btn" @click.stop="toggleDropdown(tpl.id)"><MoreHorizontal :size="16" /></button>
                    <Transition name="dropdown">
                      <div v-if="openDropdownId === tpl.id" class="dropdown-menu">
                        <button v-if="tpl.status === 'DRAFT'" @click="handlePublish(tpl)"><Upload :size="14" /> 发布</button>
                        <button @click="handleDuplicate(tpl)"><Copy :size="14" /> 复制</button>
                        <button v-if="tpl.status === 'PUBLISHED'" class="warn" @click="handleDeprecate(tpl)"><Ban :size="14" /> 废弃</button>
                        <button v-if="tpl.status !== 'ARCHIVED'" @click="handleArchive(tpl)"><Archive :size="14" /> 归档</button>
                        <button v-if="tpl.status === 'DRAFT'" class="danger" @click="handleDeleteTemplate(tpl)"><Trash2 :size="14" /> 删除</button>
                      </div>
                    </Transition>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="tplTotal > tplQuery.size" class="tpl-pager">
          <span>共 {{ tplTotal }} 条</span>
          <div class="pager-nav">
            <button :disabled="tplQuery.page <= 1" @click="handleTplPageChange(tplQuery.page - 1)">上一页</button>
            <span class="pager-cur">{{ tplQuery.page }} / {{ Math.ceil(tplTotal / tplQuery.size) }}</span>
            <button :disabled="tplQuery.page >= Math.ceil(tplTotal / tplQuery.size)" @click="handleTplPageChange(tplQuery.page + 1)">下一页</button>
          </div>
        </div>
      </section>
    </div>

    <!-- ==================== Tab 2: Response Sets ==================== -->
    <div v-show="activeTab === 'response-sets'" class="cfg-body rs-layout">
      <!-- Left: Set list -->
      <aside class="rs-side">
        <div class="rs-side-head">
          <span>选项集列表</span>
          <button class="ic-btn" title="创建选项集" @click="openCreateSet"><Plus :size="15" /></button>
        </div>
        <div class="rs-side-search">
          <Search :size="13" class="rs-search-icon" />
          <input v-model="rsQuery.keyword" placeholder="搜索选项集..." @keyup.enter="handleRsSearch" />
        </div>
        <div v-if="rsLoading" class="state-loading compact"><span>加载中...</span></div>
        <div v-else-if="rsSets.length === 0" class="state-empty compact">
          <p>暂无选项集</p>
          <button class="btn-primary sm" @click="openCreateSet">创建选项集</button>
        </div>
        <div v-else class="rs-list">
          <div
            v-for="s in rsSets" :key="s.id"
            class="rs-item" :class="{ sel: rsSelectedSet?.id === s.id }"
            @click="selectResponseSet(s)"
          >
            <div class="rs-item-body">
              <span class="rs-item-name">{{ s.setName }}</span>
              <span class="rs-item-code">{{ s.setCode }}<template v-if="s.isGlobal"> · 全局</template></span>
            </div>
            <div class="rs-item-acts">
              <button class="ic-btn-s" @click.stop="openEditSet(s)"><Pencil :size="14" /></button>
              <button class="ic-btn-s danger" @click.stop="handleDeleteSet(s)"><Trash2 :size="14" /></button>
            </div>
          </div>
        </div>
      </aside>

      <!-- Right: Options panel -->
      <section class="rs-main">
        <template v-if="rsSelectedSet">
          <div class="rs-main-head">
            <div>
              <h3>{{ rsSelectedSet.setName }}</h3>
              <span class="rs-main-code">{{ rsSelectedSet.setCode }}</span>
            </div>
            <button class="btn-primary sm" @click="openCreateOption"><Plus :size="14" /> 添加选项</button>
          </div>
          <div v-if="rsOptionsLoading" class="state-loading compact"><span>加载中...</span></div>
          <div v-else-if="rsOptions.length === 0" class="state-empty compact"><p>暂无选项，点击右上角添加</p></div>
          <div v-else class="rs-opts">
            <div v-for="(opt, idx) in rsOptions" :key="opt.id" class="rs-opt">
              <div class="rs-opt-left">
                <span class="rs-opt-idx">{{ idx + 1 }}</span>
                <span class="rs-opt-color" :style="{ background: opt.optionColor || '#cbd5e1' }" />
                <div class="rs-opt-info">
                  <span class="rs-opt-label">{{ opt.optionLabel }}</span>
                  <span class="rs-opt-value">{{ opt.optionValue }}</span>
                </div>
              </div>
              <div class="rs-opt-right">
                <span v-if="opt.score" class="rs-opt-score">{{ opt.score }}分</span>
                <span v-if="opt.isFlagged" class="rs-opt-flag">标记</span>
                <button class="ic-btn-s" @click="openEditOption(opt)"><Pencil :size="14" /></button>
                <button class="ic-btn-s danger" @click="handleDeleteOption(opt)"><Trash2 :size="14" /></button>
              </div>
            </div>
          </div>
        </template>
        <div v-else class="state-empty full">
          <ListChecks :size="36" />
          <p>选择左侧选项集查看详情</p>
        </div>
      </section>
    </div>

    <!-- ==================== Dialogs ==================== -->

    <!-- Catalog Dialog -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="catalogDialogVisible" class="modal-mask" @mousedown="onMaskMouseDown" @click="onMaskClick($event, () => catalogDialogVisible = false)">
          <div class="modal-box">
            <div class="modal-head">
              <h3>{{ catalogDialogTitle }}</h3>
              <button class="modal-close" @click="catalogDialogVisible = false">&times;</button>
            </div>
            <div class="modal-body">
              <div v-if="!editingCatalogId" class="fld">
                <label>编码 <span class="req">*</span></label>
                <input v-model="catalogForm.catalogCode" placeholder="唯一编码，如 DORM" />
              </div>
              <div class="fld">
                <label>名称 <span class="req">*</span></label>
                <input v-model="catalogForm.catalogName" placeholder="分类名称" />
              </div>
              <div class="fld">
                <label>描述</label>
                <textarea v-model="catalogForm.description" rows="2" placeholder="可选描述" />
              </div>
              <div class="fld-row">
                <div class="fld" style="flex:1">
                  <label>排序</label>
                  <input v-model.number="catalogForm.sortOrder" type="number" />
                </div>
                <label v-if="editingCatalogId" class="ck-label">
                  <input v-model="catalogForm.isEnabled" type="checkbox" /> 启用
                </label>
              </div>
            </div>
            <div class="modal-foot">
              <button class="btn-ghost" @click="catalogDialogVisible = false">取消</button>
              <button class="btn-primary" @click="handleSaveCatalog">保存</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Create Template Dialog -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="createTplDialogVisible" class="modal-mask" @mousedown="onMaskMouseDown" @click="onMaskClick($event, () => createTplDialogVisible = false)">
          <div class="modal-box">
            <div class="modal-head">
              <h3>新建模板</h3>
              <button class="modal-close" @click="createTplDialogVisible = false">&times;</button>
            </div>
            <div class="modal-body">
              <div class="fld">
                <label>模板名称 <span class="req">*</span></label>
                <input v-model="createTplForm.templateName" placeholder="如 宿舍卫生检查模板" />
              </div>
              <div class="fld">
                <label>目标类型 <span class="req">*</span></label>
                <select v-model="createTplForm.targetType" style="width:100%;border:1px solid #d1d5db;border-radius:6px;padding:8px 10px;font-size:13px;outline:none;">
                  <option v-for="(cfg, key) in TargetTypeConfig" :key="key" :value="key">{{ cfg.label }}</option>
                </select>
              </div>
              <div class="fld">
                <label>描述</label>
                <textarea v-model="createTplForm.description" rows="3" placeholder="可选，简要说明模板用途" style="width:100%;resize:vertical;border:1px solid #d1d5db;border-radius:6px;padding:8px 10px;font-size:13px;outline:none;" />
              </div>
            </div>
            <div class="modal-foot">
              <button class="btn-ghost" @click="createTplDialogVisible = false">取消</button>
              <button class="btn-primary" @click="handleCreateTemplate">创建</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Response Set Dialog -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="rsSetDialogVisible" class="modal-mask" @mousedown="onMaskMouseDown" @click="onMaskClick($event, () => rsSetDialogVisible = false)">
          <div class="modal-box">
            <div class="modal-head">
              <h3>{{ rsSetDialogTitle }}</h3>
              <button class="modal-close" @click="rsSetDialogVisible = false">&times;</button>
            </div>
            <div class="modal-body">
              <div v-if="!rsEditingSetId" class="fld">
                <label>编码 <span class="req">*</span></label>
                <input v-model="rsSetForm.setCode" placeholder="如 PASS_FAIL" />
              </div>
              <div class="fld">
                <label>名称 <span class="req">*</span></label>
                <input v-model="rsSetForm.setName" placeholder="选项集名称" />
              </div>
              <label class="ck-label"><input v-model="rsSetForm.isGlobal" type="checkbox" /> 全局选项集</label>
            </div>
            <div class="modal-foot">
              <button class="btn-ghost" @click="rsSetDialogVisible = false">取消</button>
              <button class="btn-primary" @click="handleSaveSet">保存</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Option Dialog -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="rsOptionDialogVisible" class="modal-mask" @mousedown="onMaskMouseDown" @click="onMaskClick($event, () => rsOptionDialogVisible = false)">
          <div class="modal-box">
            <div class="modal-head">
              <h3>{{ rsEditingOptionId ? '编辑选项' : '添加选项' }}</h3>
              <button class="modal-close" @click="rsOptionDialogVisible = false">&times;</button>
            </div>
            <div class="modal-body">
              <div class="fld">
                <label>选项值 <span class="req">*</span></label>
                <input v-model="rsOptionForm.optionValue" :disabled="!!rsEditingOptionId" placeholder="如 PASS, FAIL" />
              </div>
              <div class="fld">
                <label>显示标签 <span class="req">*</span></label>
                <input v-model="rsOptionForm.optionLabel" placeholder="如 合格, 不合格" />
              </div>
              <div class="fld-row three">
                <div class="fld">
                  <label>颜色</label>
                  <input v-model="rsOptionForm.optionColor" type="color" class="color-ipt" />
                </div>
                <div class="fld">
                  <label>分值</label>
                  <input v-model.number="rsOptionForm.score" type="number" />
                </div>
                <div class="fld">
                  <label>排序</label>
                  <input v-model.number="rsOptionForm.sortOrder" type="number" />
                </div>
              </div>
              <label class="ck-label"><input v-model="rsOptionForm.isFlagged" type="checkbox" /> 标记为问题项</label>
            </div>
            <div class="modal-foot">
              <button class="btn-ghost" @click="rsOptionDialogVisible = false">取消</button>
              <button class="btn-primary" @click="handleSaveOption">保存</button>
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
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

/* ==================== Tab Bar ==================== */
.cfg-tabbar {
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  padding: 0 24px;
}
.cfg-tabs {
  display: flex;
  gap: 0;
}
.cfg-tab {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 14px 20px;
  font-size: 14px;
  font-weight: 500;
  color: #8c95a3;
  background: none;
  border: none;
  border-bottom: 2.5px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}
.cfg-tab:hover { color: #505a68; }
.cfg-tab.active {
  color: #1a6dff;
  border-bottom-color: #1a6dff;
}
.cfg-tab-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: 10px;
  background: #eef1f5;
  color: #8c95a3;
  min-width: 20px;
  text-align: center;
}
.cfg-tab.active .cfg-tab-badge {
  background: #e8f0ff;
  color: #1a6dff;
}

/* ==================== Body ==================== */
.cfg-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* ==================== Catalog Sidebar ==================== */
.cat-side {
  width: 260px;
  background: #fff;
  border-right: 1px solid #e8ecf0;
  flex-shrink: 0;
  display: flex;
  transition: width 0.25s cubic-bezier(0.4,0,0.2,1);
}
.cat-side.collapsed { width: 44px; }
.cat-side-inner {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.cat-side-collapsed {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 14px;
}
.cat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 12px;
}
.cat-head-label {
  font-size: 13px;
  font-weight: 600;
  color: #1e2a3a;
  letter-spacing: 0.02em;
}
.cat-head-acts {
  display: flex;
  gap: 2px;
}
.cat-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 10px 16px;
}

/* Catalog nodes */
.cat-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  margin-bottom: 2px;
}
.cat-node:hover { background: #f4f6f9; }
.cat-node.sel {
  background: #e8f0ff;
  color: #1a6dff;
}
.cat-node.child {
  padding-left: 36px;
}
.cat-node-left {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
}
.cat-icon { color: #8c95a3; flex-shrink: 0; }
.cat-node.sel .cat-icon { color: #1a6dff; }
.cat-icon-leaf { color: #b8c0cc; flex-shrink: 0; }
.cat-label {
  font-size: 13px;
  font-weight: 500;
  color: #3d4757;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.cat-node.sel .cat-label { color: #1a6dff; font-weight: 600; }
.cat-expand {
  background: none; border: none; padding: 2px; cursor: pointer;
  color: #b8c0cc; display: flex; align-items: center; flex-shrink: 0;
}
.cat-expand:hover { color: #5a6474; }
.cat-node-acts {
  display: flex;
  gap: 1px;
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
}
.cat-node:hover .cat-node-acts { opacity: 1; }

/* ==================== Template Main ==================== */
.tpl-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 18px 24px;
  overflow: hidden;
  min-width: 0;
}

/* Toolbar */
.tpl-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 12px;
}
.tpl-bar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.tpl-search {
  position: relative;
}
.tpl-search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #b8c0cc;
  pointer-events: none;
}
.tpl-search input {
  height: 34px;
  width: 220px;
  border: 1px solid #dce1e8;
  border-radius: 8px;
  padding: 0 12px 0 32px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  background: #fff;
  color: #3d4757;
}
.tpl-search input::placeholder { color: #b8c0cc; }
.tpl-search input:focus {
  border-color: #7aadff;
  box-shadow: 0 0 0 3px rgba(26,109,255,0.08);
}
.tpl-select {
  height: 34px;
  border: 1px solid #dce1e8;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 13px;
  outline: none;
  background: #fff;
  color: #3d4757;
  cursor: pointer;
}
.tpl-reset {
  background: none;
  border: none;
  font-size: 12px;
  color: #8c95a3;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}
.tpl-reset:hover { color: #1a6dff; background: #f0f4ff; }

/* Filter hint */
.tpl-filter-hint {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 6px 14px;
  background: #e8f0ff;
  border-radius: 8px;
  font-size: 12px;
  color: #1a6dff;
}
.tpl-filter-hint strong { font-weight: 600; }
.tpl-filter-hint button {
  background: none; border: none;
  font-size: 11px; color: #5a8fff; cursor: pointer;
  text-decoration: underline;
}

/* ==================== Template Cards ==================== */
.tpl-cards {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 4px;
}
.tpl-card {
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.tpl-card:hover {
  border-color: #c8d4e3;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}
.tpl-card-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  gap: 16px;
}
.tpl-card-info {
  flex: 1;
  min-width: 0;
}
.tpl-card-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.tpl-card-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tpl-card:hover .tpl-card-name { color: #1a6dff; }

/* Status tags */
.tpl-status-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 12px;
  white-space: nowrap;
  flex-shrink: 0;
  letter-spacing: 0.02em;
}
.st-draft { background: #f0f2f5; color: #6b7685; }
.st-published { background: #e6f7ee; color: #0d9148; }
.st-deprecated { background: #fff3e0; color: #c47a00; }
.st-archived { background: #f0f2f5; color: #8c95a3; }

/* Meta line */
.tpl-card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.meta-item {
  font-size: 12px;
  color: #8c95a3;
}
.meta-sep {
  color: #dce1e8;
  font-size: 11px;
}
.cat-tag {
  background: #f4f6f9;
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 11px;
  color: #6b7685;
}
.tpl-target-tag {
  background: #f0f4ff;
  color: #1a6dff;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 500;
}

/* Card right */
.tpl-card-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

/* Template actions */
.tpl-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Dropdown */
.dropdown-wrap {
  position: relative;
}
.dropdown-menu {
  position: absolute;
  right: 0;
  top: 100%;
  margin-top: 4px;
  min-width: 130px;
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 10px;
  padding: 4px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
  z-index: 100;
}
.dropdown-menu button {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 12px;
  font-size: 13px;
  color: #3d4757;
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.12s;
  text-align: left;
}
.dropdown-menu button:hover { background: #f4f6f9; }
.dropdown-menu button.warn { color: #c47a00; }
.dropdown-menu button.warn:hover { background: #fff7e6; }
.dropdown-menu button.danger { color: #d93025; }
.dropdown-menu button.danger:hover { background: #fef2f2; }

.dropdown-enter-active { transition: all 0.15s ease-out; }
.dropdown-leave-active { transition: all 0.1s ease-in; }
.dropdown-enter-from, .dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* Pagination */
.tpl-pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 14px;
  font-size: 12px;
  color: #8c95a3;
}
.pager-nav {
  display: flex;
  align-items: center;
  gap: 6px;
}
.pager-nav button {
  padding: 5px 12px;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  background: #fff;
  font-size: 12px;
  cursor: pointer;
  color: #3d4757;
  transition: all 0.15s;
}
.pager-nav button:not(:disabled):hover { border-color: #7aadff; color: #1a6dff; }
.pager-nav button:disabled { opacity: 0.35; cursor: default; }
.pager-cur { font-weight: 500; color: #3d4757; }

/* ==================== Response Sets Layout ==================== */
.rs-layout { display: flex; }
.rs-side {
  width: 300px;
  background: #fff;
  border-right: 1px solid #e8ecf0;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}
.rs-side-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 10px;
  font-size: 13px;
  font-weight: 600;
  color: #1e2a3a;
}
.rs-side-search {
  position: relative;
  padding: 0 12px 10px;
}
.rs-search-icon {
  position: absolute;
  left: 22px;
  top: 50%;
  transform: translateY(-55%);
  color: #b8c0cc;
  pointer-events: none;
}
.rs-side-search input {
  width: 100%;
  height: 32px;
  border: 1px solid #dce1e8;
  border-radius: 8px;
  padding: 0 12px 0 30px;
  font-size: 13px;
  outline: none;
  background: #fff;
  color: #3d4757;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.rs-side-search input::placeholder { color: #b8c0cc; }
.rs-side-search input:focus {
  border-color: #7aadff;
  box-shadow: 0 0 0 3px rgba(26,109,255,0.08);
}
.rs-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 12px;
}
.rs-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  margin-bottom: 2px;
}
.rs-item:hover { background: #f4f6f9; }
.rs-item.sel { background: #e8f0ff; }
.rs-item-body {
  min-width: 0;
  flex: 1;
}
.rs-item-name {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #3d4757;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rs-item.sel .rs-item-name { color: #1a6dff; }
.rs-item-code {
  font-size: 11px;
  color: #b8c0cc;
}
.rs-item-acts {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
}
.rs-item:hover .rs-item-acts { opacity: 1; }

/* RS Main */
.rs-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 18px 24px;
  overflow: hidden;
}
.rs-main-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.rs-main-head h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0;
}
.rs-main-code {
  font-size: 12px;
  color: #b8c0cc;
}
.rs-opts {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.rs-opt {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 8px;
  transition: border-color 0.15s;
}
.rs-opt:hover { border-color: #c8d4e3; }
.rs-opt-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.rs-opt-idx {
  font-size: 12px;
  font-weight: 600;
  color: #b8c0cc;
  width: 20px;
  text-align: center;
}
.rs-opt-color {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1px solid rgba(0,0,0,0.06);
  flex-shrink: 0;
}
.rs-opt-info {
  display: flex;
  flex-direction: column;
}
.rs-opt-label {
  font-size: 13px;
  font-weight: 500;
  color: #1e2a3a;
}
.rs-opt-value {
  font-size: 11px;
  color: #b8c0cc;
}
.rs-opt-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.rs-opt-score {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  background: #e8f0ff;
  color: #1a6dff;
}
.rs-opt-flag {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  background: #fef2f2;
  color: #d93025;
}

/* ==================== Shared ==================== */

/* Buttons */
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
.btn-primary.sm { padding: 6px 12px; font-size: 12px; border-radius: 6px; }
.btn-ghost {
  padding: 8px 16px;
  background: none;
  border: 1px solid #dce1e8;
  border-radius: 8px;
  font-size: 13px;
  color: #5a6474;
  cursor: pointer;
}
.btn-ghost:hover { background: #f4f6f9; }

.ic-btn {
  background: none; border: none; padding: 5px;
  color: #8c95a3; cursor: pointer; border-radius: 6px;
  display: flex; align-items: center;
  transition: all 0.12s;
}
.ic-btn:hover { color: #1a6dff; background: #f0f4ff; }
.ic-btn-s {
  background: none; border: none; padding: 3px;
  color: #b8c0cc; cursor: pointer; border-radius: 4px;
  display: flex; align-items: center;
  transition: all 0.12s;
}
.ic-btn-s:hover { color: #1a6dff; }
.ic-btn-s.danger:hover { color: #d93025; }

/* States */
.state-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 48px 0;
  color: #b8c0cc;
  font-size: 13px;
}
.state-loading.compact { padding: 24px 0; }
.spinner {
  width: 18px; height: 18px;
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
  padding: 48px 0;
  gap: 12px;
  color: #b8c0cc;
}
.state-empty p {
  font-size: 13px;
  color: #8c95a3;
  margin: 0;
}
.state-empty.compact { padding: 24px 0; }
.state-empty.full { height: 100%; }

/* ==================== Modals ==================== */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15,23,42,0.4);
  backdrop-filter: blur(2px);
}
.modal-box {
  width: 460px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 24px 64px rgba(0,0,0,0.18);
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
  background: none; border: none;
  font-size: 22px; color: #b8c0cc; cursor: pointer;
  padding: 0 4px; line-height: 1;
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

/* Form */
.fld label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: #5a6474;
  margin-bottom: 5px;
}
.fld input, .fld textarea {
  width: 100%;
  border: 1px solid #dce1e8;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  color: #1e2a3a;
  background: #fff;
}
.fld input::placeholder, .fld textarea::placeholder { color: #b8c0cc; }
.fld input:focus, .fld textarea:focus {
  border-color: #7aadff;
  box-shadow: 0 0 0 3px rgba(26,109,255,0.08);
}
.fld input:disabled { background: #f4f6f9; color: #8c95a3; }
.fld .color-ipt { height: 38px; padding: 4px; cursor: pointer; }
.fld-row { display: flex; gap: 12px; align-items: flex-end; }
.fld-row.three { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; }
.req { color: #d93025; }
.ck-label {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  color: #3d4757;
  cursor: pointer;
}

/* Modal transition */
.modal-enter-active { transition: all 0.2s ease-out; }
.modal-leave-active { transition: all 0.15s ease-in; }
.modal-enter-from { opacity: 0; }
.modal-enter-from .modal-box { transform: translateY(12px) scale(0.97); }
.modal-leave-to { opacity: 0; }
.modal-leave-to .modal-box { transform: translateY(-8px) scale(0.98); }
</style>
