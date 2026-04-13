<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Eye, Upload } from 'lucide-vue-next'
import { useInspTemplateStore } from '@/stores/insp/inspTemplateStore'
import { useTemplateEditor } from '@/composables/insp/useTemplateEditor'
import { http } from '@/utils/request'
import { TemplateStatusConfig, ItemTypeConfig, ScoringModeConfig, type ItemType, type ScoringMode, type TargetType } from '@/types/insp/enums'
import type { TemplateItem, ResponseSet } from '@/types/insp/template'
import SectionTree from './components/SectionTree.vue'
import ItemEditor from './components/ItemEditor.vue'
import ItemTypeSelector from './components/ItemTypeSelector.vue'
import TemplatePreview from './components/TemplatePreview.vue'
import InspErrorState from '../shared/InspErrorState.vue'
import CalcRuleChain from '../scoring/components/CalcRuleChain.vue'
import { useInspScoringStore } from '@/stores/insp/inspScoringStore'
import type { ScoringProfile, CreateGradeBandRequest, UpdateGradeBandRequest, CreateRuleRequest, UpdateRuleRequest } from '@/types/insp/scoring'
// ScoringPolicy type kept for potential future use

const route = useRoute()
const router = useRouter()
const tplStore = useInspTemplateStore()

const rootSectionId = computed(() => route.params.id ? Number(route.params.id) : 0)
const rootSectionIdRef = ref(rootSectionId.value)
const editor = useTemplateEditor(rootSectionIdRef)

const rootSection = computed(() => tplStore.currentRootSection)
const responseSets = ref<ResponseSet[]>([])
const loadError = ref<string | null>(null)
const selectedSectionId = ref<number | null>(null)
const selectedItem = ref<TemplateItem | null>(null)
const selectedKey = computed(() => {
  if (selectedItem.value) return `item:${selectedItem.value.id}`
  if (selectedSectionId.value != null) return `section:${selectedSectionId.value}`
  return null
})
const showPreview = ref(false)
const showItemTypeSelector = ref(false)
const addItemToSectionId = ref<number | null>(null)
const isReadonly = computed(() => rootSection.value?.status !== 'DRAFT')

const isRootSelected = computed(() => selectedSectionId.value != null && Number(selectedSectionId.value) === Number(rootSectionId.value))
const selectedSection = computed(() => {
  if (isRootSelected.value) return rootSection.value || null
  return editor.sections.value.find(s => Number(s.id) === Number(selectedSectionId.value)) || null
})
const isFirstLevel = computed(() => Number(selectedSection.value?.parentSectionId) === Number(rootSectionId.value))
const isLeaf = computed(() => selectedSection.value ? !editor.sections.value.some(s => Number(s.parentSectionId) === Number(selectedSection.value!.id)) : false)
const currentItems = computed(() => {
  if (!selectedSectionId.value) return []
  return editor.itemsBySection.value.get(String(selectedSectionId.value)) || []
})
const allItems = computed(() => { const r: TemplateItem[] = []; for (const l of editor.itemsBySection.value.values()) r.push(...l); return r })

// ===== Root info =====
const editingInfo = ref(false)
const infoForm = ref({ name: '', description: '', tags: '' })
function openEditInfo() {
  if (!rootSection.value || isReadonly.value) return
  infoForm.value = { name: rootSection.value.sectionName, description: rootSection.value.description || '', tags: parseTags(rootSection.value.tags) }
  editingInfo.value = true
}
async function saveInfo() {
  if (!rootSection.value) return
  try {
    await tplStore.editRootSection(rootSection.value.id, { name: infoForm.value.name, description: infoForm.value.description || undefined, tags: infoForm.value.tags || undefined })
    await tplStore.loadRootSection(rootSection.value.id); editingInfo.value = false; ElMessage.success('已保存')
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}

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
    await editor.editSection(Number(rootSection.value.id), {
      sectionName: rootForm.value.name,
      targetType: rootForm.value.targetType,
      targetTypeFilter: arrayToFilter(rootForm.value.targetTypeFilter),
    } as any)
    await tplStore.loadRootSection(Number(rootSection.value.id))
    rootInfoDirty.value = false; ElMessage.success('已保存')
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}

// ===== Section form (right panel when no item selected) =====
const sf = ref({ sectionName: '', targetType: null as TargetType | null, targetTypeFilter: [] as string[], weight: 100, isRepeatable: false, inputMode: 'INLINE' as 'INLINE' | 'EVENT_STREAM' })
const sfDirty = ref(false)

// 类型选项列表（根据 targetType 动态加载）
interface TypeOption { code: string; name: string }
const typeFilterOptions = ref<TypeOption[]>([])

async function loadTypeFilterOptions(targetType: string | null) {
  typeFilterOptions.value = []
  if (!targetType) return
  try {
    if (targetType === 'ORG') {
      const types = await http.get<any[]>('/org-types')
      typeFilterOptions.value = (types || []).map((t: any) => ({ code: t.typeCode || t.code, name: t.typeName || t.name }))
    } else if (targetType === 'USER') {
      const types = await http.get<any[]>('/user-types')
      typeFilterOptions.value = (types || []).map((t: any) => ({ code: t.typeCode || t.code, name: t.typeName || t.name }))
    } else if (targetType === 'PLACE') {
      const types = await http.get<any[]>('/place-types')
      typeFilterOptions.value = (types || []).map((t: any) => ({ code: t.typeCode || t.code, name: t.typeName || t.name }))
    }
  } catch { /* ignore */ }
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
    sf.value = { sectionName: s.sectionName, targetType: s.targetType as TargetType | null, targetTypeFilter: filterArr, weight: s.weight, isRepeatable: s.isRepeatable, inputMode: s.inputMode || 'INLINE' }
    sfDirty.value = false
    loadTypeFilterOptions(s.targetType as string | null)
  }
})

function markDirty() { sfDirty.value = true }

async function saveSection() {
  if (!selectedSection.value) return
  try {
    await editor.editSection(selectedSection.value.id, { sectionName: sf.value.sectionName, targetType: sf.value.targetType, targetTypeFilter: arrayToFilter(sf.value.targetTypeFilter), weight: sf.value.weight, isRepeatable: sf.value.isRepeatable, inputMode: sf.value.inputMode } as any)
    sfDirty.value = false; ElMessage.success('已保存')
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}

// ===== Section CRUD =====
async function handleAddSection(parentSectionId?: number) {
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
async function handleRemoveSection(id: number) {
  if (isReadonly.value) return
  try { await ElMessageBox.confirm('确认删除？', '删除', { type: 'warning' }); await editor.removeSection(id); if (selectedSectionId.value === id) { selectedSectionId.value = null; selectedItem.value = null } }
  catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
}

// ===== Item CRUD =====
function selectSection(id: number) {
  selectedSectionId.value = Number(id); selectedItem.value = null
  loadScoringForSection(Number(id))
}
function selectItem(item: TemplateItem) {
  selectedItem.value = item
  selectedSectionId.value = Number(item.sectionId)
  showScoring.value = false
}
function openAddItem(sectionId: number) { addItemToSectionId.value = sectionId; showItemTypeSelector.value = true }
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
  try { await editor.editItem(selectedItem.value.id, data); const items = editor.itemsBySection.value.get(selectedItem.value.sectionId) || []; selectedItem.value = items.find(i => i.id === selectedItem.value!.id) || null; ElMessage.success('已保存') }
  catch (e: any) { ElMessage.error(e.message || '保存失败') }
}
async function handleDeleteItem(item: TemplateItem) {
  if (isReadonly.value) return
  try { await editor.removeItem(item.id); if (selectedItem.value?.id === item.id) selectedItem.value = null; ElMessage.success('已删除') }
  catch (e: any) { ElMessage.error(e.message || '删除失败') }
}

// ===== Inline Scoring =====
const scoringStore = useInspScoringStore()
const scoringProfile = ref<ScoringProfile | null>(null)
const showScoring = ref(true)
const scoringLoading = ref(false)

// 展开汇总规则时才加载
const scoringSectionId = ref<number | null>(null)

async function loadScoringForSection(sectionId: number) {
  if (scoringSectionId.value === sectionId && scoringProfile.value) return
  scoringSectionId.value = sectionId
  scoringLoading.value = true
  scoringProfile.value = null
  try {
    let p = await scoringStore.loadProfileBySection(sectionId)
    if (!p) {
      try { p = await scoringStore.createProfile(sectionId) } catch { /* already exists race */ p = await scoringStore.loadProfileBySection(sectionId) }
    }
    if (p) {
      scoringProfile.value = p
      await scoringStore.syncDimensions(p.id)
      await Promise.all([
        scoringStore.loadGradeBands(p.id),
        scoringStore.loadRules(p.id),
      ])
    }
  } catch (e) { console.warn('Load scoring profile failed', e); scoringProfile.value = null }
  finally { scoringLoading.value = false }
}

async function saveScoringBasic() {
  if (!scoringProfile.value) return
  try {
    const updated = await scoringStore.updateProfile(scoringProfile.value.id, {
      maxScore: scoringProfile.value.maxScore,
      minScore: scoringProfile.value.minScore,
      precisionDigits: scoringProfile.value.precisionDigits,
    })
    if (updated) scoringProfile.value = updated
  } catch (e) { console.warn('Save scoring basic settings failed', e) }
}

function toggleScoring() {
  showScoring.value = !showScoring.value
  if (showScoring.value && selectedSectionId.value != null) {
    loadScoringForSection(Number(selectedSectionId.value))
  }
}

async function handleCreateGradeBand(data: CreateGradeBandRequest) {
  if (!scoringProfile.value) return
  await scoringStore.createGradeBand(scoringProfile.value.id, data)
}
async function handleUpdateGradeBand(id: number, data: UpdateGradeBandRequest) {
  if (!scoringProfile.value) return
  await scoringStore.updateGradeBand(scoringProfile.value.id, id, data)
}
async function handleDeleteGradeBand(id: number) {
  if (!scoringProfile.value) return
  await scoringStore.deleteGradeBand(scoringProfile.value.id, id)
}
async function handleCreateRule(data: CreateRuleRequest) {
  if (!scoringProfile.value) return
  await scoringStore.createRule(scoringProfile.value.id, data)
}
async function handleUpdateRule(id: number, data: UpdateRuleRequest) {
  if (!scoringProfile.value) return
  await scoringStore.updateRule(scoringProfile.value.id, id, data)
}
async function handleDeleteRule(id: number) {
  if (!scoringProfile.value) return
  await scoringStore.deleteRule(scoringProfile.value.id, id)
}
async function handleApplyGradePreset(presetKey: string) {
  // handled by GradeBandEditor internally
}

// ===== Grade mapping mode + validation =====
const gradeEnabled = ref(false)
const gradingMode = ref<'SCORE' | 'RANK' | 'PERCENT'>('SCORE')

function switchGradeMode(mode: 'SCORE' | 'RANK' | 'PERCENT') {
  if (scoringStore.gradeBands.length > 0) return
  gradingMode.value = mode
}

// Direction helpers: maxScore encodes direction (100=SCORE, 1=TOP, -1=BOTTOM)
function getBandDirection(band: any): 'TOP' | 'BOTTOM' {
  return Number(band.maxScore) === -1 ? 'BOTTOM' : 'TOP'
}
function setBandDirection(band: any, dir: string) {
  band.maxScore = dir === 'BOTTOM' ? -1 : 1
  updateGradeBand(band)
}

const sortedGradeBands = computed(() => {
  const bands = [...scoringStore.gradeBands]
  if (gradingMode.value === 'SCORE') {
    bands.sort((a, b) => b.minScore - a.minScore) // 高分在前
  } else {
    bands.sort((a, b) => a.minScore - b.minScore) // 小数在前
  }
  return bands
})

// 冲突检测
const gradeConflict = computed(() => {
  const bands = scoringStore.gradeBands
  if (bands.length < 2) return null

  if (gradingMode.value === 'SCORE') {
    // 分数模式：检查区间重叠和间隙
    const values = bands.map(b => b.minScore).sort((a, b) => a - b)
    for (let i = 0; i < values.length - 1; i++) {
      if (values[i] === values[i + 1]) return `存在重复的阈值: ${values[i]}`
    }
    const sorted = bands.map(b => b.minScore).sort((a, b) => b - a)
    if (sorted[sorted.length - 1] > 0) return `最低等级的阈值应为 0（当前为 ${sorted[sorted.length - 1]}%）`
  }

  if (gradingMode.value === 'RANK') {
    // 排名模式：同方向检查重复值
    const topBands = bands.filter(b => Number(b.maxScore) !== -1)
    const bottomBands = bands.filter(b => Number(b.maxScore) === -1)
    const topVals = topBands.map(b => b.minScore).sort((a, b) => a - b)
    for (let i = 0; i < topVals.length - 1; i++) {
      if (topVals[i] === topVals[i + 1]) return `前${topVals[i]}名 存在重复`
    }
    const bottomVals = bottomBands.map(b => b.minScore).sort((a, b) => a - b)
    for (let i = 0; i < bottomVals.length - 1; i++) {
      if (bottomVals[i] === bottomVals[i + 1]) return `后${bottomVals[i]}名 存在重复`
    }
  }

  if (gradingMode.value === 'PERCENT') {
    // 百分比模式：同方向检查重复，前+后>100则冲突
    const topBands = bands.filter(b => Number(b.maxScore) !== -1)
    const bottomBands = bands.filter(b => Number(b.maxScore) === -1)
    const topVals = topBands.map(b => b.minScore).sort((a, b) => a - b)
    for (let i = 0; i < topVals.length - 1; i++) {
      if (topVals[i] === topVals[i + 1]) return `前${topVals[i]}% 存在重复`
    }
    const bottomVals = bottomBands.map(b => b.minScore).sort((a, b) => a - b)
    for (let i = 0; i < bottomVals.length - 1; i++) {
      if (bottomVals[i] === bottomVals[i + 1]) return `后${bottomVals[i]}% 存在重复`
    }
    const maxTop = topVals.length > 0 ? topVals[topVals.length - 1] : 0
    const maxBottom = bottomVals.length > 0 ? bottomVals[bottomVals.length - 1] : 0
    if (maxTop + maxBottom > 100) return `前${maxTop}% + 后${maxBottom}% 超过100%，存在重叠`
  }

  return null
})

// 开关切换
async function handleGradeToggle() {
  if (!gradeEnabled.value && scoringStore.gradeBands.length > 0) {
    // 关闭时清空
    if (!scoringProfile.value) return
    for (const b of [...scoringStore.gradeBands]) {
      await scoringStore.deleteGradeBand(scoringProfile.value.id, b.id)
    }
  }
}

// 加载时检测是否已有等级
watch(() => scoringStore.gradeBands.length, (len) => {
  if (len > 0) gradeEnabled.value = true
}, { immediate: true })

// ===== Inline grade band editing =====
async function addGradeBand() {
  if (!scoringProfile.value) return
  const maxScore = gradingMode.value === 'SCORE' ? 100 : 1 // 1=TOP direction by default
  await scoringStore.createGradeBand(scoringProfile.value.id, {
    gradeCode: '', gradeName: '', minScore: 0, maxScore,
  })
}
async function updateGradeBand(band: any) {
  if (!scoringProfile.value) return
  await scoringStore.updateGradeBand(scoringProfile.value.id, band.id, {
    gradeName: band.gradeName, minScore: band.minScore, maxScore: band.maxScore,
  })
}
async function deleteGradeBand(bandId: number) {
  if (!scoringProfile.value) return
  await scoringStore.deleteGradeBand(scoringProfile.value.id, bandId)
}
async function addCalcRule() {
  if (!scoringProfile.value) return
  await scoringStore.createRule(scoringProfile.value.id, {
    ruleCode: 'R' + Date.now().toString(36), ruleName: '', ruleType: 'VETO',
    config: '{}', isEnabled: true, priority: scoringStore.rules.length + 1,
  })
}

// ===== Grade presets =====
async function applyGradePreset(preset: string) {
  if (!scoringProfile.value) return
  // 清除现有
  for (const b of [...scoringStore.gradeBands]) {
    await scoringStore.deleteGradeBand(scoringProfile.value!.id, b.id)
  }
  const presets: Record<string, Array<{ code: string; name: string; min: number; max: number }>> = {
    five: [
      { code: 'A', name: '优秀', min: 90, max: 100 },
      { code: 'B', name: '良好', min: 80, max: 89.99 },
      { code: 'C', name: '中等', min: 70, max: 79.99 },
      { code: 'D', name: '及格', min: 60, max: 69.99 },
      { code: 'F', name: '不及格', min: 0, max: 59.99 },
    ],
    pass: [
      { code: 'P', name: '通过', min: 60, max: 100 },
      { code: 'F', name: '不通过', min: 0, max: 59.99 },
    ],
    three: [
      { code: 'A', name: '优秀', min: 85, max: 100 },
      { code: 'B', name: '合格', min: 60, max: 84.99 },
      { code: 'C', name: '不合格', min: 0, max: 59.99 },
    ],
  }
  for (const b of (presets[preset] || [])) {
    await scoringStore.createGradeBand(scoringProfile.value!.id, {
      gradeCode: b.code, gradeName: b.name, minScore: b.min, maxScore: b.max,
    })
  }
  gradingMode.value = 'SCORE'
}



// ===== Unlock for editing (to create new version) =====
async function handleUnlockForEdit() {
  if (!rootSection.value) return
  try {
    await ElMessageBox.confirm(
      '解锁后可以编辑模板内容，编辑完成后重新发布即生成新版本。已使用当前版本的项目不受影响。',
      '编辑模板', { type: 'info', confirmButtonText: '解锁编辑' }
    )
    await http.put(`/v7/insp/sections/${rootSection.value.id}/status`, { status: 'DRAFT' })
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
  try { await tplStore.loadRootSection(rootSectionId.value); rootSectionIdRef.value = rootSectionId.value; loadError.value = null } catch (e: any) { loadError.value = e.message || '加载失败' }
  try { responseSets.value = await tplStore.loadResponseSets() } catch (e) { console.warn('Load response sets failed', e) }
  // 默认选中根节点并加载汇总规则
  selectedSectionId.value = rootSectionId.value
  loadScoringForSection(rootSectionId.value)
}
onMounted(() => {
  if (!rootSectionId.value) router.replace('/inspection/v7/config'); else loadData()
})

function getItemTypeLabel(item: TemplateItem) {
  if (item.isScored && item.scoringConfig) { try { const c = JSON.parse(item.scoringConfig); return ScoringModeConfig[c.mode as ScoringMode]?.label || ItemTypeConfig[item.itemType]?.label } catch (e) { console.warn('JSON parse failed', e) } }
  return ItemTypeConfig[item.itemType]?.label || item.itemType
}
</script>

<template>
  <div class="te-root">
    <InspErrorState v-if="loadError" :message="loadError" @retry="loadData" />
    <div v-else-if="!rootSection" class="te-loading">加载中...</div>

    <template v-else>
      <!-- ===== Top bar ===== -->
      <header class="te-header">
        <div class="te-header-left">
          <button class="te-icon-btn" @click="router.push('/inspection/v7/config')"><ArrowLeft :size="16" /></button>
          <div class="te-header-info">
            <span class="te-header-name">{{ rootSection.sectionName }}</span>
            <span class="te-header-status" :style="{ color: TemplateStatusConfig[rootSection.status]?.color }">{{ TemplateStatusConfig[rootSection.status]?.label }}</span>
            <span class="te-header-code">{{ rootSection.sectionCode }} · v{{ rootSection.latestVersion }}</span>
          </div>
        </div>
        <div class="te-header-actions">
          <template v-if="isReadonly">
            <span class="te-readonly-hint">已发布 v{{ rootSection.latestVersion }}</span>
            <button class="te-btn te-btn-primary" @click="handleUnlockForEdit">解锁编辑</button>
          </template>
          <button class="te-btn te-btn-ghost" @click="showPreview = !showPreview"><Eye :size="13" />{{ showPreview ? '编辑' : '预览' }}</button>
          <button v-if="rootSection.status === 'DRAFT'" class="te-btn te-btn-green" @click="handlePublish"><Upload :size="13" />发布</button>
        </div>
      </header>

      <!-- ===== 2-Column body ===== -->
      <div class="te-body">
        <TemplatePreview v-if="showPreview" :sections="editor.sections.value" :items-by-section="editor.itemsBySection.value" class="flex-1" @close="showPreview = false" />

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
                <ItemEditor :item="selectedItem" :response-sets="responseSets" :all-items="allItems" @save="handleSaveItem" @cancel="selectedItem = null" />
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
                          class="te-filter-tag" :class="{ active: (isRootSelected ? rootForm.targetTypeFilter : sf.targetTypeFilter).includes(opt.code) }">
                          <input type="checkbox" :value="opt.code"
                            :checked="(isRootSelected ? rootForm.targetTypeFilter : sf.targetTypeFilter).includes(opt.code)"
                            @change="(e: Event) => {
                              const arr = isRootSelected ? rootForm.targetTypeFilter : sf.targetTypeFilter
                              const checked = (e.target as HTMLInputElement).checked
                              if (checked) arr.push(opt.code); else arr.splice(arr.indexOf(opt.code), 1)
                              if (isRootSelected) rootInfoDirty = true; else markDirty()
                            }"
                            :disabled="isReadonly" />
                          <span>{{ opt.name }}</span>
                        </label>
                      </div>
                    </div>
                  </div>
                  <div v-if="isRootSelected && !rootForm.targetType" class="te-target-error">
                    必须设置检查对象类型
                  </div>
                </div>

                <!-- ── 汇总规则（带标题分割线） ── -->
                <div class="te-divider-title"><span>汇总规则</span></div>

                <div v-if="scoringLoading" class="te-scoring-loading">加载中...</div>
                <template v-else-if="scoringProfile">
                  <!-- 基础数值 -->
                  <div class="te-flat-group">
                    <div class="te-inline-row">
                      <div class="te-prop-field" style="flex:1">
                        <label>满分</label>
                        <input v-model.number="scoringProfile.maxScore" type="number" @change="saveScoringBasic" :disabled="isReadonly" />
                      </div>
                      <div class="te-prop-field" style="flex:1">
                        <label>最低分</label>
                        <input v-model.number="scoringProfile.minScore" type="number" @change="saveScoringBasic" :disabled="isReadonly" />
                      </div>
                      <div class="te-prop-field" style="flex:1">
                        <label>精度</label>
                        <input v-model.number="scoringProfile.precisionDigits" type="number" min="0" max="4" @change="saveScoringBasic" :disabled="isReadonly" />
                      </div>
                    </div>
                  </div>


                  <!-- 即时规则 -->
                  <div class="te-divider-title te-divider-title--sub"><span>即时规则</span></div>
                  <div class="te-scoring-block">
                    <div class="te-scoring-block-head">
                      <span class="te-scoring-block-title"></span>
                      <button v-if="!isReadonly" class="te-add-btn" @click="addCalcRule">+</button>
                    </div>
                    <div v-if="scoringStore.rules.length === 0" class="te-scoring-empty">暂无规则</div>
                    <CalcRuleChain v-else
                      :rules="scoringStore.rules"
                      @create="handleCreateRule"
                      @update="handleUpdateRule"
                      @delete="handleDeleteRule"
                    />
                  </div>
                </template>
              </div>
            </template>

            <div v-else class="te-props-empty">选择左侧分区或字段</div>
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
.te-root { display: flex; flex-direction: column; height: 100%; background: #f5f6f8; }
.te-loading { display: flex; align-items: center; justify-content: center; flex: 1; font-size: 13px; color: #9ca3af; }

/* Header */
.te-header { display: flex; align-items: center; justify-content: space-between; padding: 8px 16px; background: #fff; border-bottom: 1px solid #e5e7eb; flex-shrink: 0; }
.te-header-left { display: flex; align-items: center; gap: 10px; }
.te-header-info { display: flex; align-items: baseline; gap: 8px; }
.te-header-name { font-size: 14px; font-weight: 600; color: #111827; }
.te-header-status { font-size: 11px; font-weight: 500; }
.te-header-code { font-size: 11px; color: #9ca3af; }

/* Buttons */
.te-icon-btn { background: none; border: none; padding: 6px; color: #9ca3af; cursor: pointer; border-radius: 8px; display: flex; }
.te-icon-btn:hover { color: #2563eb; background: #eff6ff; }
.te-btn { display: inline-flex; align-items: center; gap: 4px; padding: 5px 12px; border-radius: 7px; font-size: 12px; font-weight: 500; cursor: pointer; border: none; white-space: nowrap; transition: all 0.15s; }
.te-btn-primary { background: #2563eb; color: #fff; }
.te-btn-primary:hover { background: #1d4ed8; }
.te-btn-green { background: #059669; color: #fff; }
.te-btn-green:hover { background: #047857; }
.te-btn-ghost { background: transparent; border: 1px solid #e5e7eb; color: #6b7280; }
.te-btn-ghost:hover { background: #f9fafb; }
.te-btn-sm { padding: 3px 8px; font-size: 11px; }

/* 2-Column body */
.te-body { display: flex; flex: 1; overflow: hidden; }
.te-col-tree { width: 260px; flex-shrink: 0; border-right: 1px solid #e5e7eb; background: #fff; overflow-y: auto; }
.te-col-main { flex: 1; overflow: hidden; display: flex; flex-direction: column; background: #fff; }

/* Props header (field properties) */
.te-props-head { display: flex; align-items: center; justify-content: space-between; padding: 7px 14px; background: #fff; border-bottom: 1px solid #e8ecf0; flex-shrink: 0; }
.te-props-head span:first-child { font-size: 12px; font-weight: 600; color: #111827; }
.te-props-path { font-size: 11px; color: #9ca3af; font-weight: 400; margin-left: 8px; }
.te-props-scroll { flex: 1; overflow-y: auto; padding: 10px 14px; display: flex; flex-direction: column; gap: 8px; align-content: flex-start; }
.te-props-empty { display: flex; align-items: center; justify-content: center; flex: 1; font-size: 12px; color: #9ca3af; }

/* ======= Section properties panel (redesigned) ======= */

/* Section head bar */
.te-sec-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px 8px;
  flex-shrink: 0;
  border-bottom: 1px solid #e8ecf0;
}
.te-sec-head-title { font-size: 12px; font-weight: 600; color: #111827; letter-spacing: 0.01em; }
.te-sec-save-btn {
  padding: 4px 14px; border-radius: 5px; border: none;
  background: #1a6dff; color: #fff; font-size: 11px; font-weight: 500;
  cursor: pointer; transition: background 0.15s;
}
.te-sec-save-btn:hover { background: #1559d6; }

/* Scroll area */
.te-sec-scroll {
  flex: 1; overflow-y: auto; padding: 14px 16px 20px;
  display: flex; flex-direction: column; gap: 14px;
}

/* Flat group (no card border, just vertical gap) */
.te-flat-group { display: flex; flex-direction: column; gap: 8px; }

/* Inline row (horizontal fields + checkbox) */
.te-inline-row { display: flex; gap: 8px; align-items: flex-end; }

/* Compact checkbox */
.te-check-compact {
  display: flex; align-items: center; gap: 5px;
  font-size: 11px; color: #374151; cursor: pointer;
  white-space: nowrap; padding-bottom: 5px;
}
.te-check-compact input { accent-color: #1a6dff; }

/* Target strip (blue left border) */
.te-target-strip {
  display: flex; flex-direction: column; gap: 8px;
  padding: 10px 12px;
  border-left: 3px solid #1a6dff;
  background: #f8f9fb;
  border-radius: 0 6px 6px 0;
}

/* Target row: select + filter tags on one line */
.te-target-row { display: flex; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
.te-target-select { flex: 0 0 auto; min-width: 140px; }
.te-target-filter { flex: 1; min-width: 120px; }

/* Error for unset root targetType */
.te-target-error {
  font-size: 10px; color: #ef4444; padding: 3px 8px;
  background: #fef2f2; border-radius: 4px; border-left: 2px solid #ef4444;
}

/* Readonly hint */
.te-readonly-hint { font-size: 11px; color: #9ca3af; padding: 2px 8px; background: #f4f6f9; border-radius: 4px; }
.te-header-actions { display: flex; align-items: center; gap: 8px; }

/* Divider with title */
.te-divider-title {
  display: flex; align-items: center; gap: 10px;
  font-size: 11px; font-weight: 600; color: #6b7280;
  letter-spacing: 0.02em;
}
.te-divider-title::after {
  content: ''; flex: 1; height: 1px; background: #e8ecf0;
}
.te-divider-title--sub { font-size: 10px; font-weight: 500; color: #9ca3af; }

/* Scoring loading */
.te-scoring-loading { font-size: 11px; color: #9ca3af; padding: 8px 0; text-align: center; }

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
.te-filter-tag input[type="checkbox"] { display: none; }

/* Modal */
.te-modal-mask { position: fixed; inset: 0; z-index: 50; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.35); }
.te-modal { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 20px 60px rgba(0,0,0,0.15); overflow-y: auto; }

/* ======= Scoring blocks ======= */
.te-scoring-block { display: flex; flex-direction: column; gap: 6px; }
.te-scoring-block-head { display: flex; align-items: center; justify-content: space-between; }
.te-scoring-block-title { font-size: 11px; font-weight: 600; color: #374151; }
.te-scoring-empty { font-size: 11px; color: #b8c0cc; padding: 4px 0; }

/* Toggle switch */
.te-toggle { position: relative; display: inline-block; width: 28px; height: 16px; flex-shrink: 0; }
.te-toggle input { opacity: 0; width: 0; height: 0; }
.te-toggle-slider {
  position: absolute; cursor: pointer; inset: 0;
  background: #d1d5db; border-radius: 16px; transition: 0.2s;
}
.te-toggle-slider::before {
  content: ''; position: absolute; height: 12px; width: 12px;
  left: 2px; bottom: 2px; background: #fff; border-radius: 50%; transition: 0.2s;
}
.te-toggle input:checked + .te-toggle-slider { background: #1a6dff; }
.te-toggle input:checked + .te-toggle-slider::before { transform: translateX(12px); }

/* Grade mode bar (square button group) */
.te-grade-mode-bar { display: flex; gap: 0; }
.te-mode-btn {
  font-size: 10px; padding: 4px 12px;
  border: 1px solid #e8ecf0; color: #6b7280;
  background: #fff; cursor: pointer;
  transition: all 0.12s; white-space: nowrap;
  margin-left: -1px;
}
.te-mode-btn:first-child { border-radius: 5px 0 0 5px; margin-left: 0; }
.te-mode-btn:last-child { border-radius: 0 5px 5px 0; }
.te-mode-btn:hover:not(:disabled) { color: #1a6dff; border-color: #93c5fd; z-index: 1; }
.te-mode-btn.active {
  background: #1a6dff; color: #fff; border-color: #1a6dff;
  z-index: 2; font-weight: 500;
}
.te-mode-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.te-mode-lock-hint { font-size: 9px; color: #f59e0b; }

/* Grade presets */
.te-grade-presets { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.te-preset-chip {
  font-size: 10px; padding: 3px 10px; border-radius: 4px;
  border: 1px solid #e8ecf0; color: #5a6474;
  background: #f8f9fb; cursor: pointer;
  transition: all 0.12s; white-space: nowrap;
}
.te-preset-chip:hover { border-color: #93c5fd; color: #1a6dff; background: #eef4ff; }

/* Add inline button */
.te-add-btn {
  width: 20px; height: 20px; border-radius: 4px;
  border: 1px dashed #d1d5db; background: none; color: #9ca3af;
  font-size: 14px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.12s;
}
.te-add-btn:hover { border-color: #1a6dff; color: #1a6dff; background: #eef4ff; }

/* ======= Grade band table (compact rows) ======= */
.te-grade-table { display: flex; flex-direction: column; gap: 1px; }
.te-grade-row {
  display: flex; align-items: center; gap: 4px;
  padding: 3px 0;
  border-bottom: 1px solid #f2f3f5;
}
.te-grade-row:last-child { border-bottom: none; }

/* Grade inputs */
.te-gr-code {
  width: 34px; border: 1px solid #e8ecf0; border-radius: 4px;
  padding: 3px 4px; font-size: 11px; font-weight: 600;
  text-align: center; color: #1a6dff; outline: none;
  background: #f8f9fb;
}
.te-gr-code:focus { border-color: #93c5fd; background: #fff; }
.te-gr-name {
  flex: 1; border: 1px solid #e8ecf0; border-radius: 4px;
  padding: 3px 6px; font-size: 11px; color: #111827;
  outline: none; min-width: 0;
}
.te-gr-name:focus { border-color: #93c5fd; }
.te-gr-sym { font-size: 10px; color: #9ca3af; flex-shrink: 0; padding: 0 1px; }
.te-gr-val {
  width: 48px; border: 1px solid #e8ecf0; border-radius: 4px;
  padding: 3px 4px; font-size: 11px; text-align: center;
  color: #111827; outline: none;
}
.te-gr-val:focus { border-color: #93c5fd; }
.te-gr-unit { font-size: 10px; color: #9ca3af; flex-shrink: 0; }

/* Direction select (RANK/PERCENT mode per-row) */
.te-gr-dir {
  width: 44px; border: 1px solid #e8ecf0; border-radius: 4px;
  padding: 2px 2px; font-size: 10px; color: #374151;
  outline: none; background: #f8f9fb; cursor: pointer;
  -webkit-appearance: none; appearance: none;
  text-align: center;
}
.te-gr-dir:focus { border-color: #93c5fd; }

/* Delete */
.te-gr-del {
  background: none; border: none; color: #d1d5db;
  font-size: 13px; cursor: pointer; padding: 0 2px;
  opacity: 0; transition: all 0.1s; line-height: 1;
}
.te-grade-row:hover .te-gr-del { opacity: 1; }
.te-gr-del:hover { color: #ef4444; }

/* Grade conflict warning */
.te-grade-warn {
  font-size: 10px; color: #ef4444;
  padding: 4px 8px; background: #fef2f2;
  border-radius: 4px; border-left: 2px solid #ef4444;
}

/* Grade add button */
.te-grade-add-btn {
  font-size: 10px; color: #1a6dff; background: none;
  border: 1px dashed #dce1e8; border-radius: 4px;
  padding: 4px 10px; cursor: pointer; transition: all 0.12s;
  align-self: flex-start;
}
.te-grade-add-btn:hover { border-color: #1a6dff; background: #eef4ff; }
</style>
