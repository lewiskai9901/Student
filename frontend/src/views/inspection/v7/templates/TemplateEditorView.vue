<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Eye, Upload, Plus, Trash2, GripVertical, Settings2 } from 'lucide-vue-next'
import { useInspTemplateStore } from '@/stores/insp/inspTemplateStore'
import { inspTemplateApi } from '@/api/insp/template'
import { useTemplateEditor } from '@/composables/insp/useTemplateEditor'
import { http } from '@/utils/request'
import { TemplateStatusConfig, ItemTypeConfig, ScoringModeConfig, TargetTypeConfig, type ItemType, type ScoringMode, type TargetType } from '@/types/insp/enums'
import type { TemplateSection, TemplateItem, ResponseSet } from '@/types/insp/template'
import SectionTree from './components/SectionTree.vue'
import ItemEditor from './components/ItemEditor.vue'
import ItemTypeSelector from './components/ItemTypeSelector.vue'
import TemplatePreview from './components/TemplatePreview.vue'
import InspErrorState from '../shared/InspErrorState.vue'
import DimensionTable from '../scoring/components/DimensionTable.vue'
import GradeBandEditor from '../scoring/components/GradeBandEditor.vue'
import CalcRuleChain from '../scoring/components/CalcRuleChain.vue'
import { useInspScoringStore } from '@/stores/insp/inspScoringStore'
import type { ScoringProfile, CreateGradeBandRequest, UpdateGradeBandRequest, CreateRuleRequest, UpdateRuleRequest } from '@/types/insp/scoring'

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
  // Try both number and original key since itemsBySection keys may be string or number
  return editor.itemsBySection.value.get(selectedSectionId.value) || editor.itemsBySection.value.get(Number(selectedSectionId.value)) || []
})
const allItems = computed(() => { const r: TemplateItem[] = []; for (const l of editor.itemsBySection.value.values()) r.push(...l); return r })

// ===== Root info =====
const editingInfo = ref(false)
const infoForm = ref({ name: '', description: '', tags: '' })
function openEditInfo() {
  if (!rootSection.value || isReadonly.value) return
  infoForm.value = { name: rootSection.value.sectionName, description: rootSection.value.description || '', tags: rootSection.value.tags || '' }
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
const rootForm = ref({ name: '', description: '', tags: '', targetType: null as TargetType | null, targetSourceMode: null as string | null, targetTypeFilter: [] as string[] })
const rootInfoDirty = ref(false)

watch([isRootSelected, rootSection], ([sel, root]) => {
  if (sel && root) {
    rootForm.value = {
      name: root.sectionName, description: root.description || '', tags: root.tags || '',
      targetType: (root.targetType as TargetType | null) || null,
      targetSourceMode: root.targetSourceMode || null,
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
      targetSourceMode: rootForm.value.targetSourceMode,
      targetTypeFilter: arrayToFilter(rootForm.value.targetTypeFilter),
    } as any)
    await tplStore.loadRootSection(Number(rootSection.value.id))
    rootInfoDirty.value = false; ElMessage.success('已保存')
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}

// ===== Section form (right panel when no item selected) =====
const sf = ref({ sectionName: '', targetType: null as TargetType | null, targetSourceMode: null as string | null, targetTypeFilter: [] as string[], weight: 100, isRepeatable: false })
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
    sf.value = { sectionName: s.sectionName, targetType: s.targetType as TargetType | null, targetSourceMode: s.targetSourceMode || null, targetTypeFilter: filterArr, weight: s.weight, isRepeatable: s.isRepeatable }
    sfDirty.value = false
    loadTypeFilterOptions(s.targetType as string | null)
  }
})

function markDirty() { sfDirty.value = true }

async function saveSection() {
  if (!selectedSection.value) return
  try {
    await editor.editSection(selectedSection.value.id, { sectionName: sf.value.sectionName, targetType: sf.value.targetType, targetSourceMode: sf.value.targetSourceMode, targetTypeFilter: arrayToFilter(sf.value.targetTypeFilter), weight: sf.value.weight, isRepeatable: sf.value.isRepeatable } as any)
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

// ===== Ref sections =====
const showRefPicker = ref(false)
const refPickerSections = ref<TemplateSection[]>([])
async function handleAddRef() {
  if (isReadonly.value) return
  try { const r = await inspTemplateApi.getList({ page: 1, size: 100, status: 'PUBLISHED' }); refPickerSections.value = r.records.filter((t: TemplateSection) => t.id !== rootSectionId.value); showRefPicker.value = true } catch {}
}
async function handlePickRef(sec: TemplateSection) {
  try { await editor.addRefSection(sec.id, selectedSectionId.value); showRefPicker.value = false; ElMessage.success(`已引用「${sec.sectionName}」`) }
  catch (e: any) { ElMessage.error(e.message || '添加失败') }
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
  } catch { scoringProfile.value = null }
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
  } catch {}
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
  // Simple preset application - will be handled by GradeBandEditor internally
}

// ===== Publish =====
async function handlePublish() {
  if (!rootSection.value || rootSection.value.status !== 'DRAFT') return
  try { await ElMessageBox.confirm('确认发布？', '发布', { type: 'warning' }); await tplStore.publish(rootSection.value.id); await tplStore.loadRootSection(rootSection.value.id); ElMessage.success('已发布') }
  catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '发布失败') }
}

// ===== Init =====
async function loadData() {
  try { await tplStore.loadRootSection(rootSectionId.value); rootSectionIdRef.value = rootSectionId.value; loadError.value = null } catch (e: any) { loadError.value = e.message || '加载失败' }
  try { responseSets.value = await tplStore.loadResponseSets() } catch {}
  // 默认选中根节点并加载汇总规则
  selectedSectionId.value = rootSectionId.value
  loadScoringForSection(rootSectionId.value)
}
onMounted(() => { if (!rootSectionId.value) router.replace('/inspection/v7/config'); else loadData() })

function getItemTypeLabel(item: TemplateItem) {
  if (item.isScored && item.scoringConfig) { try { const c = JSON.parse(item.scoringConfig); return ScoringModeConfig[c.mode as ScoringMode]?.label || ItemTypeConfig[item.itemType]?.label } catch {} }
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
        <div class="flex items-center gap-2">
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
              @add-ref="handleAddRef"
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
            <template v-else-if="(isRootSelected && rootSection) || (selectedSection && !selectedSection.refSectionId)">
              <div class="te-props-head">
                <span>{{ isRootSelected ? '根分区' : '分区' }}</span>
                <button v-if="isRootSelected ? rootInfoDirty : sfDirty"
                  class="te-btn te-btn-primary te-btn-sm"
                  @click="isRootSelected ? saveRootProps() : saveSection()">保存</button>
              </div>
              <div class="te-props-scroll">
                <!-- 基本信息 -->
                <div class="te-section-card">
                  <div class="te-row-2" v-if="isRootSelected">
                    <div class="te-prop-field" style="flex:2">
                      <label>名称</label>
                      <input v-model="rootForm.name" @input="rootInfoDirty = true" :disabled="isReadonly" />
                    </div>
                    <div class="te-prop-field" style="flex:1">
                      <label>标签</label>
                      <input v-model="rootForm.tags" placeholder="逗号分隔" @input="rootInfoDirty = true" :disabled="isReadonly" />
                    </div>
                  </div>
                  <div class="te-prop-field" v-if="isRootSelected">
                    <label>描述</label>
                    <input v-model="rootForm.description" @input="rootInfoDirty = true" :disabled="isReadonly" placeholder="可选" />
                  </div>
                  <div class="te-row-3" v-if="!isRootSelected">
                    <div class="te-prop-field" style="flex:2">
                      <label>名称</label>
                      <input v-model="sf.sectionName" @input="markDirty" :disabled="isReadonly" />
                    </div>
                    <div class="te-prop-field" style="flex:1">
                      <label>权重</label>
                      <input v-model.number="sf.weight" type="number" min="0" max="100" @input="markDirty" :disabled="isReadonly" />
                    </div>
                    <label class="te-prop-check-inline">
                      <input type="checkbox" v-model="sf.isRepeatable" @change="markDirty" :disabled="isReadonly" />
                      <span>可重复</span>
                    </label>
                  </div>
                </div>

                <!-- 检查目标 -->
                <div class="te-section-card">
                  <h4 class="te-card-title">检查目标</h4>
                  <div class="te-row-2">
                    <div class="te-prop-field">
                      <label>对象类型</label>
                      <select
                        :value="isRootSelected ? rootForm.targetType : sf.targetType"
                        @change="(e: Event) => {
                          const val = (e.target as HTMLSelectElement).value || null;
                          if (isRootSelected) { rootForm.targetType = val as any; rootInfoDirty = true }
                          else { sf.targetType = val as any; markDirty() }
                          loadTypeFilterOptions(val);
                          if (isRootSelected) rootForm.targetTypeFilter = []; else sf.targetTypeFilter = []
                        }"
                        :disabled="isReadonly">
                        <option :value="null">不设置</option>
                        <option value="ORG">组织</option>
                        <option value="PLACE">场所</option>
                        <option value="USER">人员</option>
                      </select>
                    </div>
                    <div v-if="(isRootSelected ? rootForm.targetType : sf.targetType)" class="te-prop-field">
                      <label>目标来源</label>
                      <select
                        :value="isRootSelected ? rootForm.targetSourceMode : sf.targetSourceMode"
                        @change="(e: Event) => {
                          const val = (e.target as HTMLSelectElement).value;
                          if (isRootSelected) { rootForm.targetSourceMode = val; rootInfoDirty = true }
                          else { sf.targetSourceMode = val; markDirty() }
                        }"
                        :disabled="isReadonly">
                        <option value="INDEPENDENT">独立选择</option>
                        <option value="PARENT_ASSOCIATED">父目标关联</option>
                      </select>
                    </div>
                  </div>
                  <div v-if="(isRootSelected ? rootForm.targetType : sf.targetType) && typeFilterOptions.length > 0" class="te-prop-field">
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

                <!-- 汇总规则 -->
                <div class="te-section-card">
                  <h4 class="te-card-title">汇总规则</h4>
                  <div v-if="scoringLoading" class="te-scoring-loading">加载中...</div>
                  <template v-else-if="scoringProfile">
                    <!-- 基础设置 -->
                    <div class="te-row-3">
                      <div class="te-prop-field" style="flex:1">
                        <label>最高分</label>
                        <input v-model.number="scoringProfile.maxScore" type="number" @change="saveScoringBasic" />
                      </div>
                      <div class="te-prop-field" style="flex:1">
                        <label>最低分</label>
                        <input v-model.number="scoringProfile.minScore" type="number" @change="saveScoringBasic" />
                      </div>
                      <div class="te-prop-field" style="flex:1">
                        <label>精度</label>
                        <input v-model.number="scoringProfile.precisionDigits" type="number" min="0" max="4" @change="saveScoringBasic" />
                      </div>
                    </div>
                    <DimensionTable
                      :dimensions="scoringStore.dimensions"
                      :template-id="Number(selectedSectionId)"
                    />
                    <GradeBandEditor
                      :grade-bands="scoringStore.gradeBands"
                      :dimensions="scoringStore.dimensions"
                      @create="handleCreateGradeBand"
                      @update="handleUpdateGradeBand"
                      @delete="handleDeleteGradeBand"
                      @apply-preset="handleApplyGradePreset"
                    />
                    <CalcRuleChain
                      :rules="scoringStore.rules"
                      @create="handleCreateRule"
                      @update="handleUpdateRule"
                      @delete="handleDeleteRule"
                    />
                  </template>
                </div>
              </div>
            </template>

            <!-- Ref section -->
            <template v-else-if="selectedSection?.refSectionId">
              <div class="te-props-head"><span>引用分区</span></div>
              <div class="te-props-scroll">
                <div class="te-section-card">
                  <p class="text-xs text-gray-500 mb-2">此分区引用自其他根分区，内容只读。</p>
                  <div class="te-prop-field"><label>名称</label><input :value="selectedSection.sectionName" disabled /></div>
                </div>
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

    <!-- Ref picker modal -->
    <Teleport to="body">
      <div v-if="showRefPicker" class="te-modal-mask" @click.self="showRefPicker = false">
        <div class="te-modal w-[440px] max-h-[60vh] flex flex-col">
          <div class="flex items-center justify-between px-4 py-3 border-b"><h3 class="text-sm font-semibold">选择引用</h3><button class="text-gray-400 text-lg" @click="showRefPicker = false">&times;</button></div>
          <div v-if="!refPickerSections.length" class="py-10 text-center text-sm text-gray-400">无可用分区</div>
          <div v-else class="overflow-y-auto p-2">
            <div v-for="s in refPickerSections" :key="s.id" class="px-3 py-2 rounded-lg cursor-pointer hover:bg-blue-50" @click="handlePickRef(s)">
              <div class="text-sm font-medium">{{ s.sectionName }}</div>
              <div class="text-xs text-gray-400">{{ s.sectionCode }}</div>
            </div>
          </div>
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
.te-col-main { flex: 1; overflow: hidden; display: flex; flex-direction: column; background: #f5f6f8; }

/* Props header */
.te-props-head { display: flex; align-items: center; justify-content: space-between; padding: 7px 14px; background: #fff; border-bottom: 1px solid #e8ecf0; flex-shrink: 0; }
.te-props-head span:first-child { font-size: 12px; font-weight: 600; color: #111827; }
.te-props-path { font-size: 11px; color: #9ca3af; font-weight: 400; margin-left: 8px; }
.te-props-scroll { flex: 1; overflow-y: auto; padding: 10px 14px; display: flex; flex-direction: column; gap: 8px; align-content: flex-start; }
.te-props-empty { display: flex; align-items: center; justify-content: center; flex: 1; font-size: 12px; color: #9ca3af; }

/* Section cards inside props */
.te-section-card { display: flex; flex-direction: column; gap: 8px; padding: 10px; border: 1px solid #e8ecf0; border-radius: 8px; background: #fff; }
.te-card-title { font-size: 11px; font-weight: 600; color: #374151; margin: 0; }
.te-card-title-row { display: flex; align-items: center; justify-content: space-between; }
.te-chevron { font-size: 11px; color: #9ca3af; transition: transform 0.15s; display: inline-block; }
.te-chevron.open { transform: rotate(90deg); }
.te-scoring-loading { font-size: 11px; color: #9ca3af; padding: 6px 0; text-align: center; }

/* Form fields */
.te-prop-field { display: flex; flex-direction: column; }
.te-prop-field label { font-size: 11px; font-weight: 500; color: #6b7280; margin-bottom: 2px; }
.te-prop-field input, .te-prop-field select, .te-prop-field textarea { width: 100%; border: 1px solid #e5e7eb; border-radius: 6px; padding: 4px 8px; font-size: 12px; outline: none; color: #111827; background: #fff; transition: border-color 0.15s; }
.te-prop-field input:focus, .te-prop-field select:focus, .te-prop-field textarea:focus { border-color: #93c5fd; box-shadow: 0 0 0 2px rgba(37,99,235,0.06); }
.te-prop-field input:disabled, .te-prop-field select:disabled, .te-prop-field textarea:disabled { background: #f9fafb; color: #9ca3af; }
.te-prop-field textarea { resize: vertical; font-family: inherit; }
.te-prop-hint { font-size: 10px; color: #9ca3af; margin-top: 2px; }
.te-row-2 { display: flex; gap: 8px; }
.te-row-2 > * { flex: 1; }
.te-row-3 { display: flex; gap: 8px; align-items: flex-end; }
.te-prop-row { display: flex; align-items: flex-end; gap: 8px; }
.te-prop-check { display: flex; align-items: center; gap: 5px; font-size: 11px; color: #374151; cursor: pointer; padding-bottom: 4px; white-space: nowrap; }
.te-prop-check input { accent-color: #2563eb; }
.te-prop-check-inline { display: flex; align-items: center; gap: 5px; font-size: 11px; color: #374151; cursor: pointer; white-space: nowrap; padding-bottom: 2px; }
.te-prop-check-inline input { accent-color: #2563eb; }
.te-prop-hint-inline { font-size: 11px; color: #9ca3af; font-weight: 400; }

/* Filter tags */
.te-filter-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.te-filter-tag { display: flex; align-items: center; gap: 4px; padding: 4px 10px; border: 1px solid #e5e7eb; border-radius: 6px; font-size: 12px; color: #6b7280; cursor: pointer; transition: all 0.15s; user-select: none; }
.te-filter-tag:hover { border-color: #93c5fd; }
.te-filter-tag.active { border-color: #2563eb; background: #eff6ff; color: #2563eb; }
.te-filter-tag input[type="checkbox"] { display: none; }

/* Modal */
.te-modal-mask { position: fixed; inset: 0; z-index: 50; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.35); }
.te-modal { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 20px 60px rgba(0,0,0,0.15); overflow-y: auto; }
</style>
