<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Eye, Upload, FileText, Layers, X } from 'lucide-vue-next'
import { useInspTemplateStore } from '@/stores/inspection/inspTemplateStore'
import { useTemplateEditor } from '@/composables/inspection/useTemplateEditor'
import { http } from '@/utils/request'
import { TemplateStatusConfig, type ItemType, type ScoringMode, type TargetType } from '@/types/insp/enums'
import type { TemplateItem, ResponseSet } from '@/types/insp/template'
import SectionTree from './components/SectionTree.vue'
import ItemEditor from './components/ItemEditor.vue'
import ItemTypeSelector from './components/ItemTypeSelector.vue'
import TemplatePreview from './components/TemplatePreview.vue'
import InspErrorState from '../shared/InspErrorState.vue'
// ScoringPolicy type kept for potential future use

const route = useRoute()
const router = useRouter()
const tplStore = useInspTemplateStore()

const rootSectionId = computed(() => route.params.id ? String(route.params.id) : '')
const rootSectionIdRef = ref(rootSectionId.value)
const editor = useTemplateEditor(rootSectionIdRef)

const rootSection = computed(() => tplStore.currentRootSection)
const responseSets = ref<ResponseSet[]>([])
const loadError = ref<string | null>(null)
const loading = ref(true)
const selectedSectionId = ref<LongId | null>(null)
const selectedItem = ref<TemplateItem | null>(null)
const selectedKey = computed(() => {
  if (selectedItem.value) return `item:${selectedItem.value.id}`
  if (selectedSectionId.value != null) return `section:${selectedSectionId.value}`
  return null
})
const showPreview = ref(false)
const showItemTypeSelector = ref(false)
const addItemToSectionId = ref<LongId | null>(null)
const isReadonly = computed(() => rootSection.value?.status !== 'DRAFT')

const isRootSelected = computed(() => selectedSectionId.value != null && selectedSectionId.value === rootSectionId.value)
const selectedSection = computed(() => {
  if (isRootSelected.value) return rootSection.value || null
  return editor.sections.value.find(s => s.id === selectedSectionId.value) || null
})
const allItems = computed(() => { const r: TemplateItem[] = []; for (const l of editor.itemsBySection.value.values()) r.push(...l); return r })

// V20260524 Bug#2: 选中子分区的统计 — 子分区数 + 检查项数 KPI
const childSectionCount = computed(() => {
  if (selectedSectionId.value == null) return 0
  return editor.sections.value.filter(s => String(s.parentSectionId) === String(selectedSectionId.value)).length
})
const itemCountOfSelected = computed(() => {
  if (selectedSectionId.value == null) return 0
  return (editor.itemsBySection.value.get(String(selectedSectionId.value)) || []).length
})

// ==================== S+ 顶栏 KPI 概览 ====================
const sectionsCount = computed(() => editor.sections.value.length)
const itemsCount = computed(() => allItems.value.length)
const scoredItemsCount = computed(() => allItems.value.filter(i => i.isScored).length)
const lastUpdated = computed(() => {
  const all = [
    rootSection.value?.updatedAt,
    ...editor.sections.value.map(s => s.updatedAt),
    ...allItems.value.map(i => (i as any).updatedAt),
  ].filter(Boolean) as string[]
  if (all.length === 0) return null
  return all.sort().reverse()[0]
})
function fmtTime(t?: string | null): string {
  if (!t) return '—'
  const d = new Date(t)
  return `${(d.getMonth()+1).toString().padStart(2,'0')}/${d.getDate().toString().padStart(2,'0')} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

// ==================== S+ 未保存改动指示 + 全局 CmdS ====================
const hasUnsavedChanges = computed(() => rootInfoDirty.value || sfDirty.value)

function onGlobalKeyTpl(e: KeyboardEvent) {
  const t = e.target as HTMLElement
  const tag = t?.tagName
  // CmdS 总是拦截 (即使在 input 内)
  if ((e.metaKey || e.ctrlKey) && e.key === 's') {
    e.preventDefault()
    if (isReadonly.value) {
      ElMessage.info('已发布版本不可直接编辑, 请先解锁')
      return
    }
    if (rootInfoDirty.value) saveRootProps()
    else if (sfDirty.value) saveSection()
    else ElMessage.info('当前无未保存改动')
    return
  }
  // 编辑态保护
  if (tag === 'INPUT' || tag === 'TEXTAREA' || t?.isContentEditable) return
  if (e.metaKey || e.ctrlKey || e.altKey) return
  if (e.key === 'Escape') {
    selectedItem.value = null
  } else if (e.key === 'p' && rootSection.value?.status === 'DRAFT') {
    e.preventDefault(); handlePublish()
  }
}
import { onUnmounted } from 'vue'
window.addEventListener('keydown', onGlobalKeyTpl)
onUnmounted(() => window.removeEventListener('keydown', onGlobalKeyTpl))

// 离开页面提醒 (有未保存改动)
function beforeUnloadHandler(e: BeforeUnloadEvent) {
  if (hasUnsavedChanges.value && !isReadonly.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}
window.addEventListener('beforeunload', beforeUnloadHandler)
onUnmounted(() => window.removeEventListener('beforeunload', beforeUnloadHandler))

// ===== Root props panel (when root is selected in tree) =====
const rootForm = ref({ name: '', description: '', tags: '', targetType: null as TargetType | null, targetTypeFilter: [] as string[] })
const rootInfoDirty = ref(false)

watch([isRootSelected, rootSection], ([sel, root]) => {
  if (sel && root) {
    rootForm.value = {
      name: root.sectionName, description: root.description || '', tags: parseTags(root.tags),
      targetType: (root.targetType as TargetType | null) || null,
      targetTypeFilter: parseFilterToArray(root.targetTypeFilter),
    }
    rootInfoDirty.value = false
    loadTypeFilterOptions(root.targetType as string | null)
  }
}, { immediate: true })

async function saveRootProps() {
  if (!rootSection.value) return
  try {
    // 保存基本信息
    await tplStore.editRootSection(rootSection.value.id, { name: rootForm.value.name, description: rootForm.value.description || undefined, tags: rootForm.value.tags || undefined })
    // 保存 targetType 等通过通用 section API
    await editor.editSection(rootSection.value.id, {
      sectionName: rootForm.value.name,
      targetType: rootForm.value.targetType,
      targetTypeFilter: arrayToFilter(rootForm.value.targetTypeFilter),
    } as any)
    await tplStore.loadRootSection(rootSection.value.id)
    rootInfoDirty.value = false; ElMessage.success('已保存')
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}

// ===== Section form (right panel when no item selected) =====
const sf = ref({ sectionName: '', targetType: null as TargetType | null, targetTypeFilter: [] as string[], isRepeatable: false, inputMode: 'INLINE' as 'INLINE' | 'EVENT_STREAM' })
const sfDirty = ref(false)

// 类型选项列表（根据 targetType 动态加载）
interface TypeOption { code: string; name: string }
const typeFilterOptions = ref<TypeOption[]>([])
const typeFilterError = ref<string | null>(null)

async function loadTypeFilterOptions(targetType: string | null) {
  typeFilterOptions.value = []
  typeFilterError.value = null
  if (!targetType) return
  try {
    // P1: P5 收敛后 user_types/place_types 表已 DROP, 统一走 entity_type_configs
    // ORG 仍保留独立 /org-types (org_types 是组织管理域独立表, 未并入 entity_type_configs)
    if (targetType === 'ORG') {
      const types = await http.get<any[]>('/org-types')
      typeFilterOptions.value = (types || []).map((t: any) => ({ code: t.typeCode || t.code, name: t.typeName || t.name }))
    } else if (targetType === 'USER' || targetType === 'PLACE') {
      const types = await http.get<any[]>('/entity-type-configs', { params: { entityType: targetType } })
      typeFilterOptions.value = (types || []).map((t: any) => ({ code: t.typeCode || t.code, name: t.displayName || t.typeName || t.name }))
    }
  } catch (e: any) {
    // 类型过滤可选, 失败不阻塞编辑, 但要明确提示而非静默隐藏功能区块
    typeFilterError.value = e?.message || '类型选项加载失败'
  }
}

// 解析 targetTypeFilter 字符串为数组
/** Parse tags from JSON string or comma-separated string to display format */
function parseTags(tags: string | null | undefined): string {
  if (!tags) return ''
  try {
    const arr = JSON.parse(tags)
    return Array.isArray(arr) ? arr.join(', ') : String(tags)
  } catch { return String(tags) }
}

function parseFilterToArray(filter: string | null): string[] {
  if (!filter) return []
  return filter.split('&&').map(s => s.trim()).filter(Boolean)
}
function arrayToFilter(arr: string[]): string | null {
  return arr.length > 0 ? arr.join(' && ') : null
}

watch(selectedSection, (s) => {
  if (s) {
    const filterArr = parseFilterToArray(s.targetTypeFilter)
    sf.value = { sectionName: s.sectionName, targetType: s.targetType as TargetType | null, targetTypeFilter: filterArr, isRepeatable: s.isRepeatable, inputMode: s.inputMode || 'INLINE' }
    sfDirty.value = false
    loadTypeFilterOptions(s.targetType as string | null)
  }
})

function markDirty() { sfDirty.value = true }

async function saveSection() {
  if (!selectedSection.value) return
  try {
    await editor.editSection(selectedSection.value.id, { sectionName: sf.value.sectionName, targetType: sf.value.targetType, targetTypeFilter: arrayToFilter(sf.value.targetTypeFilter), isRepeatable: sf.value.isRepeatable, inputMode: sf.value.inputMode } as any)
    sfDirty.value = false; ElMessage.success('已保存')
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}

// ===== Section CRUD =====
async function handleAddSection(parentSectionId?: LongId) {
  if (isReadonly.value) return
  try {
    const { value: name } = await ElMessageBox.prompt('请输入分区名称', '新建分区', {
      inputPlaceholder: '如：卫生检查',
      inputValidator: (v: string) => v?.trim() ? true : '名称不能为空',
    }) as any
    if (!name?.trim()) return
    const s = await editor.addSection(parentSectionId ?? rootSectionId.value, name.trim())
    selectedSectionId.value = s.id
    selectedItem.value = null
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '添加失败') }
}
/** 递归统计某分区下的子分区数与检查项数 */
function countSectionCascade(id: LongId): { sections: number; items: number } {
  let sections = 0
  let items = (editor.itemsBySection.value.get(String(id)) || []).length
  const children = editor.sections.value.filter(s => s.parentSectionId === id)
  for (const child of children) {
    sections += 1
    const sub = countSectionCascade(child.id)
    sections += sub.sections
    items += sub.items
  }
  return { sections, items }
}

async function handleRemoveSection(id: LongId) {
  if (isReadonly.value) return
  try {
    const target = editor.sections.value.find(s => s.id === id)
    const { sections, items } = countSectionCascade(id)
    let msg = `确认删除分区「${target?.sectionName || ''}」？`
    if (sections > 0 || items > 0) {
      const parts: string[] = []
      if (sections > 0) parts.push(`${sections} 个子分区`)
      if (items > 0) parts.push(`${items} 个检查项`)
      msg += `\n将同时删除其下 ${parts.join(' 和 ')}，此操作不可恢复。`
    }
    await ElMessageBox.confirm(msg, '删除分区', { type: 'warning', confirmButtonText: '确认删除' })
    await editor.removeSection(id)
    if (selectedSectionId.value === id) { selectedSectionId.value = null; selectedItem.value = null }
    ElMessage.success('已删除')
  }
  catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
}

// ===== Item CRUD =====
function selectSection(id: LongId) {
  selectedSectionId.value = id; selectedItem.value = null
}
function selectItem(item: TemplateItem) {
  selectedItem.value = item
  selectedSectionId.value = item.sectionId
}
function openAddItem(sectionId: LongId) { addItemToSectionId.value = sectionId; showItemTypeSelector.value = true }
const addingItem = ref(false)
async function handleSelectItemType(type: ItemType | null, isScored: boolean, scoringMode?: ScoringMode) {
  if (!addItemToSectionId.value || addingItem.value) return
  try {
    const { value: name } = await ElMessageBox.prompt('请输入字段名称', '添加字段', {
      inputPlaceholder: '如：地面清洁',
      inputValidator: (v: string) => v?.trim() ? true : '名称不能为空',
    }) as any
    if (!name?.trim()) return
    addingItem.value = true
    const item = await editor.addItem(addItemToSectionId.value, type, isScored, scoringMode, name.trim())
    selectedItem.value = item
    showItemTypeSelector.value = false
    ElMessage.success('已添加')
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '添加失败') }
  finally { addingItem.value = false }
}
async function handleSaveItem(data: Partial<TemplateItem>) {
  if (!selectedItem.value) return
  try { await editor.editItem(selectedItem.value.id, data); const items = editor.itemsBySection.value.get(String(selectedItem.value.sectionId)) || []; selectedItem.value = items.find(i => i.id === selectedItem.value!.id) || null; ElMessage.success('已保存') }
  catch (e: any) { ElMessage.error(e.message || '保存失败') }
}
async function handleDeleteItem(item: TemplateItem) {
  if (isReadonly.value) return
  try { await editor.removeItem(item.id); if (selectedItem.value?.id === item.id) selectedItem.value = null; ElMessage.success('已删除') }
  catch (e: any) { ElMessage.error(e.message || '删除失败') }
}

// 2026-05-24: 「汇总规则」UI + handlers 已删除. 评分方案 (ScoringProfile) 自 2026-05-23
// 重构起下沉为 (project, section) project-owned, 模板编辑器内无 projectId 上下文,
// 旧的 stub 函数全部产生死代码 + 用户看到空白 "汇总规则" 标题. 配置入口现位于
// 项目详情页「设置」Tab → 评分方案卡片.

// ===== Unlock for editing (to create new version) =====
async function handleUnlockForEdit() {
  if (!rootSection.value) return
  try {
    await ElMessageBox.confirm(
      '解锁后可以编辑模板内容，编辑完成后重新发布即生成新版本。已使用当前版本的项目不受影响。',
      '编辑模板', { type: 'info', confirmButtonText: '解锁编辑' }
    )
    await http.put(`/inspection/sections/${rootSection.value.id}/status`, { status: 'DRAFT' })
    await tplStore.loadRootSection(rootSection.value.id)
    ElMessage.success('已解锁，可以编辑')
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
}

// ===== Publish =====
async function handlePublish() {
  if (!rootSection.value || rootSection.value.status !== 'DRAFT') return
  // 校验根分区必须设置检查对象
  if (!rootSection.value.targetType) {
    ElMessage.error('请先设置根分区的检查对象类型')
    selectedSectionId.value = rootSectionId.value
    return
  }
  try { await ElMessageBox.confirm('确认发布？发布后不可直接编辑，需创建新版本。', '发布', { type: 'warning' }); await tplStore.publish(rootSection.value.id); await tplStore.loadRootSection(rootSection.value.id); ElMessage.success('已发布') }
  catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '发布失败') }
}

// ===== Init =====
async function loadData() {
  loading.value = true
  loadError.value = null
  try {
    await tplStore.loadRootSection(rootSectionId.value)
    rootSectionIdRef.value = rootSectionId.value
  } catch (e: any) {
    loadError.value = e?.message || '模板加载失败'
    loading.value = false
    return
  }
  loading.value = false
  try {
    responseSets.value = await tplStore.loadResponseSets()
  } catch (e: any) {
    // 选项集加载失败不阻塞模板编辑, 但要可见
    ElMessage.error('选项集加载失败: ' + (e?.message || '未知错误'))
  }
  // 默认选中根节点
  selectedSectionId.value = rootSectionId.value
}
onMounted(() => {
  if (!rootSectionId.value) router.replace('/inspection/config'); else loadData()
})
</script>

<template>
  <div class="te-root insp-shell">
    <InspErrorState v-if="loadError" :message="loadError" @retry="loadData" />
    <div v-else-if="loading || !rootSection" class="te-loading-wrap">
      <div class="te-skeleton te-skeleton--header" />
      <div class="te-skeleton-body">
        <div class="te-skeleton te-skeleton--tree" />
        <div class="te-skeleton-main">
          <div class="te-skeleton te-skeleton--line" style="width:40%" />
          <div class="te-skeleton te-skeleton--line" style="width:80%" />
          <div class="te-skeleton te-skeleton--line" style="width:60%" />
        </div>
      </div>
    </div>

    <template v-else>
      <!-- ===== Top bar (Audit Hub style) ===== -->
      <header class="te-header">
        <button class="te-back" @click="router.push('/inspection/config')" title="返回">
          <ArrowLeft :size="14" />
        </button>
        <div class="te-header-info">
          <span class="insp-eyebrow">检查模板 · {{ rootSection.sectionCode }}</span>
          <div class="te-name-line">
            <h1 class="te-header-name">{{ rootSection.sectionName }}</h1>
            <span class="insp-chip"
                  :class="`insp-chip--${({DRAFT:'pending',PUBLISHED:'pass',DEPRECATED:'warn',ARCHIVED:'fail'} as any)[rootSection.status]}`">
              {{ TemplateStatusConfig[rootSection.status]?.label }}
            </span>
            <span class="te-version insp-num">v{{ rootSection.latestVersion }}</span>
          </div>
        </div>
        <!-- KPI 概览 (S+) -->
        <div class="te-kpis">
          <div class="te-kpi" :title="`${sectionsCount} 个分区`">
            <Layers :size="13" class="te-kpi__icon" />
            <span class="te-kpi__num insp-num">{{ sectionsCount }}</span>
            <span class="te-kpi__label">分区</span>
          </div>
          <div class="te-kpi" :title="`${itemsCount} 个检查项, 其中 ${scoredItemsCount} 项参与评分`">
            <FileText :size="13" class="te-kpi__icon" />
            <span class="te-kpi__num insp-num">{{ itemsCount }}</span>
            <span class="te-kpi__label">
              检查项<span v-if="itemsCount !== scoredItemsCount" class="te-kpi__sub"> · {{ scoredItemsCount }} 评分</span>
            </span>
          </div>
          <div class="te-kpi te-kpi--time" v-if="lastUpdated" :title="lastUpdated">
            <span class="te-kpi__num insp-num">{{ fmtTime(lastUpdated) }}</span>
            <span class="te-kpi__label">最近更新</span>
          </div>
        </div>

        <div class="te-header-actions">
          <!-- 未保存改动指示 (S+) -->
          <span v-if="hasUnsavedChanges && !isReadonly" class="te-unsaved" title="按 CmdS 保存">
            <span class="te-unsaved__dot" />
            未保存改动
            <kbd class="insp-kbd">CmdS</kbd>
          </span>
          <template v-if="isReadonly">
            <span class="te-readonly-hint">
              <span class="insp-stamp">已发布 v{{ rootSection.latestVersion }}</span>
              发布后不可直接编辑
            </span>
            <button class="insp-btn insp-btn--accent" @click="handleUnlockForEdit">解锁编辑</button>
          </template>
          <button class="insp-btn" @click="showPreview = !showPreview">
            <Eye :size="13" />{{ showPreview ? '编辑' : '预览' }}
          </button>
          <button v-if="rootSection.status === 'DRAFT'" class="insp-btn insp-btn--accent" @click="handlePublish">
            <Upload :size="13" />发布
          </button>
        </div>
      </header>

      <!-- ===== 2-Column body ===== -->
      <div class="te-body">
        <TemplatePreview v-if="showPreview" :sections="editor.sections.value" :items-by-section="editor.itemsBySection.value" :response-sets="responseSets" class="flex-1" @close="showPreview = false" />

        <template v-else>
          <!-- LEFT: Section + Field tree -->
          <aside class="te-col-tree">
            <SectionTree
              :sections="editor.sections.value"
              :items-by-section="editor.itemsBySection.value"
              :selected-id="selectedKey"
              :readonly="isReadonly"
              :root-section-id="rootSectionId"
              :root-section="rootSection"
              @select-section="selectSection"
              @select-item="selectItem"
              @add-child="handleAddSection"
              @add-item="openAddItem"
              @delete-section="handleRemoveSection"
              @delete-item="handleDeleteItem"
            />
          </aside>

          <!-- RIGHT: Properties panel -->
          <main class="te-col-main">
            <!-- ======= Field properties ======= -->
            <template v-if="selectedItem">
              <div class="te-props-head">
                <span>字段属性</span>
                <span class="te-props-path">{{ selectedSection?.sectionName }} / {{ selectedItem.itemName }}</span>
              </div>
              <div class="te-props-scroll">
                <ItemEditor :item="selectedItem" :response-sets="responseSets" :all-items="allItems" :readonly="isReadonly" @save="handleSaveItem" @cancel="selectedItem = null" />
              </div>
            </template>

            <!-- ======= Section properties (root + child unified) ======= -->
            <template v-else-if="(isRootSelected && rootSection) || selectedSection">
              <div class="te-sec-head">
                <span class="te-sec-head-title">{{ isRootSelected ? '根分区属性' : '分区属性' }}</span>
                <button v-if="isRootSelected ? rootInfoDirty : sfDirty"
                  class="te-sec-save-btn"
                  @click="isRootSelected ? saveRootProps() : saveSection()">保存</button>
              </div>
              <div class="te-sec-scroll">

                <!-- ── 基本信息（平铺，无卡片） ── -->
                <div class="te-flat-group" v-if="isRootSelected">
                  <div class="te-row-2">
                    <div class="te-prop-field" style="flex:2">
                      <label>名称</label>
                      <input v-model="rootForm.name" @input="rootInfoDirty = true" :disabled="isReadonly" />
                    </div>
                    <div class="te-prop-field" style="flex:1">
                      <label>标签</label>
                      <input v-model="rootForm.tags" placeholder="逗号分隔" @input="rootInfoDirty = true" :disabled="isReadonly" />
                    </div>
                  </div>
                  <div class="te-prop-field">
                    <label>描述</label>
                    <input v-model="rootForm.description" @input="rootInfoDirty = true" :disabled="isReadonly" placeholder="可选" />
                  </div>
                </div>
                <div class="te-flat-group" v-if="!isRootSelected">
                  <div class="te-inline-row">
                    <div class="te-prop-field" style="flex:2">
                      <label>名称</label>
                      <input v-model="sf.sectionName" @input="markDirty" :disabled="isReadonly" />
                    </div>
                    <label class="te-check-compact">
                      <input type="checkbox" v-model="sf.isRepeatable" @change="markDirty" :disabled="isReadonly" />
                      <span>可重复</span>
                    </label>
                  </div>
                  <!-- V20260524 Bug#2: 分区代码 + KPI + 快捷操作 - 之前面板信息过贫 -->
                  <div class="te-sec-meta">
                    <div class="te-sec-meta-row">
                      <span class="te-sec-meta-label">编码</span>
                      <code class="te-sec-meta-code">{{ selectedSection?.sectionCode }}</code>
                    </div>
                    <div class="te-sec-meta-row">
                      <span class="te-sec-meta-label">子分区</span>
                      <span class="te-sec-meta-num">{{ childSectionCount }}</span>
                      <span class="te-sec-meta-label" style="margin-left: 12px">检查项</span>
                      <span class="te-sec-meta-num">{{ itemCountOfSelected }}</span>
                    </div>
                  </div>
                  <div v-if="!isReadonly" class="te-sec-actions">
                    <button class="te-sec-action-btn" @click="handleAddSection(selectedSectionId as LongId)">
                      <Layers :size="11" /> 添加子分区
                    </button>
                    <button class="te-sec-action-btn" @click="openAddItem(selectedSectionId as LongId)">
                      <FileText :size="11" /> 添加检查项
                    </button>
                    <button class="te-sec-action-btn te-sec-action-btn--danger" @click="handleRemoveSection(selectedSectionId as LongId)">
                      <X :size="11" /> 删除分区
                    </button>
                  </div>
                </div>

                <!-- ── 检查目标（蓝色左边线） ── -->
                <div class="te-target-strip">
                  <div class="te-target-row">
                    <div class="te-prop-field te-target-select">
                      <label>检查对象</label>
                      <select
                        :value="isRootSelected ? rootForm.targetType : (sf.targetType || 'INHERIT')"
                        @change="(e: Event) => {
                          const raw = (e.target as HTMLSelectElement).value;
                          const val = raw === 'INHERIT' ? null : (raw || null);
                          if (isRootSelected) {
                            rootForm.targetType = val as any; rootInfoDirty = true
                            rootForm.targetTypeFilter = []
                          } else {
                            sf.targetType = val as any; markDirty()
                            sf.targetTypeFilter = []
                          }
                          loadTypeFilterOptions(val)
                        }"
                        :disabled="isReadonly">
                        <template v-if="isRootSelected">
                          <option :value="null" disabled>请选择检查对象</option>
                        </template>
                        <template v-else>
                          <option value="INHERIT">对父目标直接打分</option>
                        </template>
                        <option value="ORG">组织</option>
                        <option value="PLACE">场所</option>
                        <option value="USER">人员</option>
                      </select>
                    </div>
                    <div v-if="(isRootSelected ? rootForm.targetType : sf.targetType) && typeFilterOptions.length > 0" class="te-prop-field te-target-filter">
                      <label>类型过滤</label>
                      <div class="te-filter-tags">
                        <label v-for="opt in typeFilterOptions" :key="opt.code"
                          class="te-filter-tag"
                          :class="{ active: (isRootSelected ? rootForm.targetTypeFilter : sf.targetTypeFilter).includes(opt.code), 'te-filter-tag--readonly': isReadonly }"
                          @click.prevent.stop="isReadonly ? null : (() => {
                            const arr = isRootSelected ? rootForm.targetTypeFilter : sf.targetTypeFilter
                            const idx = arr.indexOf(opt.code)
                            if (idx >= 0) arr.splice(idx, 1); else arr.push(opt.code)
                            if (isRootSelected) rootInfoDirty = true; else markDirty()
                          })()">
                          <input type="checkbox" :value="opt.code"
                            :checked="(isRootSelected ? rootForm.targetTypeFilter : sf.targetTypeFilter).includes(opt.code)"
                            :disabled="isReadonly"
                            tabindex="-1"
                            style="pointer-events:none" />
                          <span>{{ opt.name }}</span>
                        </label>
                      </div>
                    </div>
                  </div>
                  <div v-if="isRootSelected && !rootForm.targetType" class="te-target-error">
                    必须设置检查对象类型
                  </div>
                  <div v-else-if="typeFilterError" class="te-target-error">
                    类型选项加载失败：{{ typeFilterError }}
                    <button class="te-target-retry" @click="loadTypeFilterOptions(isRootSelected ? rootForm.targetType : sf.targetType)">重试</button>
                  </div>
                </div>

              </div>
            </template>

            <div v-else class="te-props-empty">
              <FileText :size="28" class="te-props-empty__icon" />
              <p class="te-props-empty__title">从左侧选择分区或检查项</p>
              <p class="te-props-empty__sub">选中后可在此编辑属性、评分规则与验证规则</p>
            </div>
          </main>
        </template>
      </div>
    </template>

    <!-- Item type selector modal -->
    <Teleport to="body">
      <div v-if="showItemTypeSelector" class="te-modal-mask" @click.self="showItemTypeSelector = false">
        <div class="te-modal w-[520px]">
          <h3 class="text-sm font-semibold mb-4">添加检查项</h3>
          <ItemTypeSelector @select="handleSelectItemType" />
          <div class="mt-4 flex justify-end"><button class="te-btn te-btn-ghost te-btn-sm" @click="showItemTypeSelector = false">取消</button></div>
        </div>
      </div>
    </Teleport>

  </div>
</template>

<style scoped>
.te-root { display: flex; flex-direction: column; height: 100%; background: var(--insp-bg-page); }

/* Loading skeleton */
.te-loading-wrap { display: flex; flex-direction: column; flex: 1; padding: 12px 16px; gap: 12px; }
.te-skeleton-body { display: flex; flex: 1; gap: 12px; }
.te-skeleton-main { flex: 1; display: flex; flex-direction: column; gap: 10px; padding-top: 8px; }
.te-skeleton {
  background: linear-gradient(90deg, var(--insp-bg-subtle) 25%, var(--insp-bg-sunken) 37%, var(--insp-bg-subtle) 63%);
  background-size: 400% 100%;
  border-radius: var(--insp-radius-sm);
  animation: te-skeleton-shimmer 1.4s ease infinite;
}
.te-skeleton--header { height: 48px; }
.te-skeleton--tree { width: 260px; flex-shrink: 0; }
.te-skeleton--line { height: 14px; }
@keyframes te-skeleton-shimmer {
  0% { background-position: 100% 0; }
  100% { background-position: 0 0; }
}
.te-target-retry {
  margin-left: auto; font-size: 11px; color: var(--insp-accent);
  background: none; border: none; cursor: pointer; padding: 0;
  text-decoration: underline;
}

/* Header (Audit Hub aligned) */
.te-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: var(--insp-bg-surface);
  border-bottom: 1px solid var(--insp-border-default);
  flex-shrink: 0;
}
.te-back {
  display: inline-flex;
  align-items: center; justify-content: center;
  width: 28px; height: 28px;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-strong);
  border-radius: var(--insp-radius-sm);
  color: var(--insp-ink-tertiary);
  cursor: pointer;
  transition: all var(--insp-t-fast);
}
.te-back:hover { color: var(--insp-accent); border-color: var(--insp-accent); }
.te-header-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}
.te-name-line {
  display: flex;
  align-items: center;
  gap: 8px;
}
.te-header-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--insp-ink-primary);
  margin: 0;
  letter-spacing: 0;
}
.te-version {
  font-family: var(--insp-font-mono);
  font-size: 11px;
  font-weight: 600;
  color: var(--insp-accent);
  padding: 1px 6px;
  border: 1px solid var(--insp-accent-pale);
  border-radius: 3px;
}
.te-readonly-hint {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--insp-ink-tertiary);
}

/* KPI 概览 (S+) */
.te-kpis {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-left: auto;
  padding: 0 14px;
  height: 38px;
  background: var(--insp-bg-subtle);
  border-radius: var(--insp-radius-md);
  border: 1px solid var(--insp-border-subtle);
}
.te-kpi {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--insp-ink-tertiary);
}
.te-kpi__icon { color: var(--insp-ink-quaternary); }
.te-kpi__num {
  font-family: var(--insp-font-mono);
  font-size: 13px;
  font-weight: 700;
  color: var(--insp-ink-primary);
  letter-spacing: -0.01em;
}
.te-kpi__label { font-size: 10px; letter-spacing: 0.04em; }
.te-kpi__sub { color: var(--insp-accent); font-weight: 600; }
.te-kpi--time .te-kpi__num {
  font-size: 11px;
  font-weight: 500;
  color: var(--insp-ink-secondary);
}

/* 未保存指示 (S+) */
.te-unsaved {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 9px;
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
  border: 1px solid rgba(245, 158, 11, 0.4);
  border-radius: 11px;
  font-size: 11px;
  font-weight: 600;
}
.te-unsaved__dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: #f59e0b;
  animation: unsaved-pulse 1.5s ease-in-out infinite;
}
@keyframes unsaved-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.2); }
}
.te-unsaved kbd {
  background: rgba(180, 83, 9, 0.15);
  border-color: rgba(180, 83, 9, 0.3);
  color: #b45309;
}

/* Buttons */
/* Buttons (token 化) */
.te-icon-btn {
  background: none;
  border: none;
  padding: var(--insp-sp-1);
  color: var(--insp-ink-tertiary);
  cursor: pointer;
  border-radius: var(--insp-radius-md);
  display: flex;
  transition: background var(--insp-t-fast), color var(--insp-t-fast);
}
.te-icon-btn:hover {
  color: var(--insp-accent);
  background: var(--insp-accent-paler);
}
.te-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--insp-sp-1);
  height: var(--insp-h-sm);
  padding: 0 var(--insp-sp-3);
  border-radius: var(--insp-radius-md);
  font-family: inherit;
  font-size: var(--insp-text-sm);
  font-weight: var(--insp-fw-medium);
  cursor: pointer;
  border: 1px solid transparent;
  white-space: nowrap;
  transition: background var(--insp-t-fast), border-color var(--insp-t-fast), color var(--insp-t-fast);
}
.te-btn-primary {
  background: var(--insp-accent);
  color: #fff;
  border-color: var(--insp-accent);
}
.te-btn-primary:hover { background: var(--insp-accent-hover); border-color: var(--insp-accent-hover); }
.te-btn-green {
  background: var(--insp-pass);
  color: #fff;
  border-color: var(--insp-pass);
}
.te-btn-green:hover { filter: brightness(0.95); }
.te-btn-ghost {
  background: transparent;
  border-color: var(--insp-border-default);
  color: var(--insp-ink-tertiary);
}
.te-btn-ghost:hover {
  background: var(--insp-bg-subtle);
  color: var(--insp-ink-primary);
  border-color: var(--insp-border-strong);
}
.te-btn-sm {
  height: var(--insp-h-xs);
  padding: 0 var(--insp-sp-2);
  font-size: var(--insp-text-xs);
}

/* 2-Column body (token 化) */
.te-body { display: flex; flex: 1; overflow: hidden; }
.te-col-tree {
  width: 260px;
  flex-shrink: 0;
  border-right: 1px solid var(--insp-border-default);
  background: var(--insp-bg-surface);
  overflow-y: auto;
}
.te-col-main {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: var(--insp-bg-surface);
}

/* Props header */
.te-props-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--insp-sp-2) var(--insp-sp-4);
  background: var(--insp-bg-surface);
  border-bottom: 1px solid var(--insp-border-subtle);
  flex-shrink: 0;
}
.te-props-head span:first-child {
  font-size: var(--insp-text-sm);
  font-weight: var(--insp-fw-semibold);
  color: var(--insp-ink-primary);
}
.te-props-path {
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-quaternary);
  margin-left: var(--insp-sp-2);
}
.te-props-scroll {
  flex: 1;
  overflow-y: auto;
  padding: var(--insp-sp-3) var(--insp-sp-4);
  display: flex;
  flex-direction: column;
  gap: var(--insp-sp-2);
}
.te-props-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: var(--insp-sp-1);
  color: var(--insp-ink-quaternary);
}
.te-props-empty__icon { color: var(--insp-ink-quaternary); margin-bottom: var(--insp-sp-2); }
.te-props-empty__title {
  margin: 0;
  font-size: var(--insp-text-sm);
  font-weight: var(--insp-fw-semibold);
  color: var(--insp-ink-secondary);
}
.te-props-empty__sub {
  margin: 0;
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-quaternary);
}

/* Section properties panel */
.te-sec-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--insp-sp-3) var(--insp-sp-4) var(--insp-sp-2);
  flex-shrink: 0;
  border-bottom: 1px solid var(--insp-border-subtle);
}
.te-sec-head-title {
  font-size: var(--insp-text-sm);
  font-weight: var(--insp-fw-semibold);
  color: var(--insp-ink-primary);
}
.te-sec-save-btn {
  display: inline-flex;
  align-items: center;
  height: var(--insp-h-sm);
  padding: 0 var(--insp-sp-3);
  border-radius: var(--insp-radius-sm);
  border: none;
  background: var(--insp-accent);
  color: #fff;
  font-family: inherit;
  font-size: var(--insp-text-xs);
  font-weight: var(--insp-fw-medium);
  cursor: pointer;
  transition: background var(--insp-t-fast);
}
.te-sec-save-btn:hover { background: var(--insp-accent-hover); }

.te-sec-scroll {
  flex: 1;
  overflow-y: auto;
  padding: var(--insp-sp-4);
  display: flex;
  flex-direction: column;
  gap: var(--insp-sp-3);
}

.te-flat-group { display: flex; flex-direction: column; gap: var(--insp-sp-2); }

/* V20260524 Bug#2: 分区元数据 + 快捷操作 */
.te-sec-meta {
  display: flex; flex-direction: column; gap: 4px;
  margin-top: 8px; padding: 8px 12px;
  background: var(--insp-bg-subtle); border-radius: 6px;
  font-size: 12px;
}
.te-sec-meta-row { display: flex; align-items: center; gap: 8px; }
.te-sec-meta-label { color: var(--insp-ink-tertiary); }
.te-sec-meta-code {
  font-family: 'SF Mono', Consolas, monospace; font-size: 11.5px;
  color: var(--insp-ink-primary); background: var(--insp-bg-surface);
  padding: 1px 6px; border-radius: 4px; border: 1px solid var(--insp-border-default);
}
.te-sec-meta-num { color: var(--insp-ink-primary); font-weight: 600; font-size: 13px; }
.te-sec-actions { display: flex; gap: 6px; margin-top: 6px; flex-wrap: wrap; }
.te-sec-action-btn {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 5px 10px; background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default); border-radius: 5px;
  font-size: 11.5px; color: var(--insp-ink-secondary); cursor: pointer;
  transition: all 0.12s;
}
.te-sec-action-btn:hover { border-color: #7aadff; color: #1a6dff; background: #eef4ff; }
.te-sec-action-btn--danger:hover { border-color: #fecaca; color: #dc2626; background: #fef2f2; }
.te-inline-row { display: flex; gap: var(--insp-sp-2); align-items: flex-end; }

.te-check-compact {
  display: flex;
  align-items: center;
  gap: var(--insp-sp-1);
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-secondary);
  cursor: pointer;
  white-space: nowrap;
  padding-bottom: var(--insp-sp-1);
}
.te-check-compact input { accent-color: var(--insp-accent); }

.te-target-strip {
  display: flex;
  flex-direction: column;
  gap: var(--insp-sp-2);
  padding: var(--insp-sp-3);
  border-left: 3px solid var(--insp-accent);
  background: var(--insp-bg-subtle);
  border-radius: 0 var(--insp-radius-md) var(--insp-radius-md) 0;
}

/* Target row: select + filter tags on one line */
.te-target-row { display: flex; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
.te-target-select { flex: 0 0 auto; min-width: 140px; }
.te-target-filter { flex: 1; min-width: 120px; }

/* Error for unset root targetType */
.te-target-error {
  display: flex; align-items: center;
  font-size: 10px; color: var(--insp-fail); padding: 3px 8px;
  background: var(--insp-fail-pale); border-radius: 4px;
  border-left: 2px solid var(--insp-fail);
}

/* Readonly hint */
.te-readonly-hint { font-size: 11px; color: var(--insp-ink-quaternary); padding: 2px 8px; background: var(--insp-bg-subtle); border-radius: 4px; }
.te-header-actions { display: flex; align-items: center; gap: 8px; }

/* Divider with title */
.te-divider-title {
  display: flex; align-items: center; gap: 10px;
  font-size: 11px; font-weight: 600; color: var(--insp-ink-tertiary);
  letter-spacing: 0.02em;
}
.te-divider-title::after {
  content: ''; flex: 1; height: 1px; background: var(--insp-border-default);
}
.te-divider-title--sub { font-size: 10px; font-weight: 500; color: var(--insp-ink-quaternary); }

/* Form fields */
.te-prop-field { display: flex; flex-direction: column; }
.te-prop-field label { font-size: 11px; font-weight: 500; color: #6b7280; margin-bottom: 2px; }
.te-prop-field input, .te-prop-field select, .te-prop-field textarea {
  width: 100%; border: 1px solid #e8ecf0; border-radius: 6px;
  padding: 5px 8px; font-size: 12px; outline: none;
  color: #111827; background: #fff; transition: border-color 0.15s;
}
.te-prop-field input:focus, .te-prop-field select:focus, .te-prop-field textarea:focus {
  border-color: #93c5fd; box-shadow: 0 0 0 2px rgba(26,109,255,0.06);
}
.te-prop-field input:disabled, .te-prop-field select:disabled, .te-prop-field textarea:disabled {
  background: #f9fafb; color: #9ca3af;
}
.te-prop-field textarea { resize: vertical; font-family: inherit; }
.te-prop-hint { font-size: 10px; color: #9ca3af; margin-top: 2px; }
.te-row-2 { display: flex; gap: 8px; }
.te-row-2 > * { flex: 1; }

/* Filter tags */
.te-filter-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.te-filter-tag {
  display: flex; align-items: center; gap: 4px;
  padding: 3px 10px; border: 1px solid #e8ecf0; border-radius: 4px;
  font-size: 11px; color: #6b7280; cursor: pointer;
  transition: all 0.15s; user-select: none;
}
.te-filter-tag:hover { border-color: #93c5fd; }
.te-filter-tag.active { border-color: #1a6dff; background: #eef4ff; color: #1a6dff; }
/* V20260524 Bug#6: readonly 时禁用 hover + click 视觉, 不允许伪切换 */
.te-filter-tag--readonly { cursor: not-allowed; opacity: 0.65; }
.te-filter-tag--readonly:hover { border-color: var(--insp-border-default); }
.te-filter-tag input[type="checkbox"] { display: none; }

/* Modal */
.te-modal-mask { position: fixed; inset: 0; z-index: 50; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.35); }
.te-modal { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 20px 60px rgba(0,0,0,0.15); overflow-y: auto; }

</style>
