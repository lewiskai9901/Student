<script setup lang="ts">
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Save, Eye, Upload, Plus, Pencil, Trash2, GripVertical,
  ShieldCheck, CheckCircle2, AlertTriangle, XCircle,
} from 'lucide-vue-next'
import { useInspTemplateStore } from '@/stores/insp/inspTemplateStore'
import { useInspScoringStore } from '@/stores/insp/inspScoringStore'
import { scoringProfileApi } from '@/api/insp/scoring'
import { useTemplateEditor } from '@/composables/insp/useTemplateEditor'
import { ItemTypeConfig, ScoringModeConfig, TemplateStatusConfig, type ItemType, type ScoringMode } from '@/types/insp/enums'
import type { TemplateSection, TemplateItem, ResponseSet } from '@/types/insp/template'
import type {
  ScoringProfile,
  CreateDimensionRequest, UpdateDimensionRequest,
  CreateGradeBandRequest, UpdateGradeBandRequest,
  CreateRuleRequest, UpdateRuleRequest,
  UpdateAdvancedSettingsRequest,
} from '@/types/insp/scoring'
import SectionTree from './components/SectionTree.vue'
import ItemEditor from './components/ItemEditor.vue'
import ItemTypeSelector from './components/ItemTypeSelector.vue'
import TemplatePreview from './components/TemplatePreview.vue'
import ConditionBuilder from '@/components/insp/ConditionBuilder.vue'
import ModuleRefPanel from './components/ModuleRefPanel.vue'
import InspErrorState from '../shared/InspErrorState.vue'

// Scoring sub-components
import DimensionTable from '../scoring/components/DimensionTable.vue'
import GradeBandEditor from '../scoring/components/GradeBandEditor.vue'
import CalcRuleChain from '../scoring/components/CalcRuleChain.vue'
import ScoreSimulator from '../scoring/components/ScoreSimulator.vue'
import VersionHistory from '../scoring/components/VersionHistory.vue'
import AdvancedScoringSettings from '../scoring/components/AdvancedScoringSettings.vue'

const route = useRoute()
const router = useRouter()
const tplStore = useInspTemplateStore()
const scoringStore = useInspScoringStore()

// ==================== Top-level tab ====================
const activeView = ref<'structure' | 'modules' | 'scoring'>('structure')

// Template ID from route
const isCreateMode = computed(() => !route.params.id)
const templateId = computed(() => route.params.id ? Number(route.params.id) : 0)
const templateIdRef = ref(templateId.value)

// Editor composable
const editor = useTemplateEditor(templateIdRef)

// Local state
const template = computed(() => tplStore.currentTemplate)
const responseSets = ref<ResponseSet[]>([])
const loadError = ref<string | null>(null)
const selectedSectionId = ref<number | null>(null)
const selectedItem = ref<TemplateItem | null>(null)
const showPreview = ref(false)
const showItemTypeSelector = ref(false)
const addItemToSectionId = ref<number | null>(null)

// Template info editing
const editingInfo = ref(false)
const infoForm = ref({ templateName: '', description: '', tags: '' })

// Current section items
const currentSectionItems = computed(() => {
  if (!selectedSectionId.value) return []
  return editor.itemsBySection.value.get(selectedSectionId.value) || []
})

// All items across all sections
const allItems = computed(() => {
  const items: TemplateItem[] = []
  for (const list of editor.itemsBySection.value.values()) {
    items.push(...list)
  }
  return items
})

const isReadonly = computed(() => template.value?.status !== 'DRAFT')

// ==================== Scoring State ====================

const scoringLoading = ref(false)
const scoringProfile = ref<ScoringProfile | null>(null)
const scoringDirty = ref(false)
const scoringForm = reactive({
  maxScore: 100,
  minScore: 0,
  precisionDigits: 2,
})

// Health checks
interface HealthCheck { key: string; label: string; status: 'ok' | 'warn' | 'error' }
const healthChecks = computed<HealthCheck[]>(() => {
  const checks: HealthCheck[] = []
  const dimCount = scoringStore.dimensions.length
  checks.push({ key: 'dims', label: dimCount > 0 ? `${dimCount} 个分区` : '未配置分区权重', status: dimCount > 0 ? 'ok' : 'error' })
  if (dimCount > 0) {
    const totalWeight = scoringStore.dimensions.reduce((s, d) => s + d.weight, 0)
    checks.push({ key: 'weight', label: totalWeight === 100 ? '分区权重合计 100%' : `分区权重合计 ${totalWeight}%`, status: totalWeight === 100 ? 'ok' : 'error' })
  }
  const bandCount = scoringStore.gradeBands.length
  checks.push({ key: 'grades', label: bandCount > 0 ? `${bandCount} 个等级区间` : '未配置等级映射', status: bandCount > 0 ? 'ok' : 'warn' })
  if (bandCount > 0) {
    const globalBands = scoringStore.gradeBands.filter(b => !b.dimensionId).sort((a, b) => a.minScore - b.minScore)
    let hasGap = false
    for (let i = 0; i < globalBands.length - 1; i++) {
      if (globalBands[i + 1].minScore - globalBands[i].maxScore > 0.01) { hasGap = true; break }
    }
    if (globalBands.length > 0) {
      checks.push({ key: 'coverage', label: hasGap ? '等级区间存在间隙' : '等级区间覆盖完整', status: hasGap ? 'warn' : 'ok' })
    }
  }
  const ruleCount = scoringStore.rules.length
  const enabledRules = scoringStore.rules.filter(r => r.isEnabled).length
  checks.push({ key: 'rules', label: ruleCount > 0 ? `${enabledRules}/${ruleCount} 条规则已启用` : '未配置计算规则', status: ruleCount > 0 ? 'ok' : 'warn' })
  return checks
})

// ==================== Load Data ====================

async function loadTemplate() {
  try {
    await tplStore.loadTemplate(templateId.value)
    templateIdRef.value = templateId.value
    loadError.value = null
  } catch (e: any) {
    loadError.value = e.message || '加载模板失败'
  }
}

async function loadResponseSets() {
  try { responseSets.value = await tplStore.loadResponseSets() } catch { /* ignore */ }
}

async function loadScoringProfile() {
  if (!templateId.value) return
  scoringLoading.value = true
  try {
    let p = await scoringStore.loadProfileByTemplate(templateId.value)
    if (!p) {
      // 自动创建默认评分配置
      p = await scoringStore.createProfile(templateId.value)
    }
    scoringProfile.value = p
    await Promise.all([
      scoringStore.loadDimensions(p.id),
      scoringStore.loadGradeBands(p.id),
      scoringStore.loadRules(p.id),
    ])
    syncScoringForm(p)
  } finally { scoringLoading.value = false }
}

function syncScoringForm(p: ScoringProfile) {
  scoringForm.maxScore = p.maxScore
  scoringForm.minScore = p.minScore
  scoringForm.precisionDigits = p.precisionDigits
}

watch(() => scoringStore.currentProfile, (p) => {
  scoringProfile.value = p ?? null
  if (p) syncScoringForm(p)
})

// ==================== Template Info ====================

function openEditInfo() {
  if (!template.value || isReadonly.value) return
  infoForm.value = {
    templateName: template.value.templateName,
    description: template.value.description || '',
    tags: template.value.tags || '',
  }
  editingInfo.value = true
}

async function saveInfo() {
  if (!template.value) return
  try {
    await tplStore.editTemplate(template.value.id, {
      templateName: infoForm.value.templateName,
      description: infoForm.value.description || undefined,
      tags: infoForm.value.tags || undefined,
    })
    await tplStore.loadTemplate(template.value.id)
    editingInfo.value = false
    ElMessage.success('已保存')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

// ==================== Section Operations ====================

async function handleAddSection() {
  if (isReadonly.value) return
  try {
    const section = await editor.addSection()
    selectedSectionId.value = section.id
    selectedItem.value = null
  } catch (e: any) {
    ElMessage.error(e.message || '添加分区失败')
  }
}

// ---- Section inline editing (right panel) ----
const sectionForm = reactive({
  sectionName: '',
  isRepeatable: false,
  conditionLogic: '',
})
const sectionFormDirty = ref(false)

const selectedSection = computed(() =>
  editor.sections.value.find(s => s.id === selectedSectionId.value) || null
)

watch(selectedSection, (s) => {
  if (s) {
    sectionForm.sectionName = s.sectionName
    sectionForm.isRepeatable = s.isRepeatable
    sectionForm.conditionLogic = s.conditionLogic || ''
    sectionFormDirty.value = false
  }
})

function markSectionDirty() { sectionFormDirty.value = true }

async function handleSaveSection() {
  if (!selectedSection.value) return
  try {
    await editor.editSection(selectedSection.value.id, {
      sectionName: sectionForm.sectionName,
      isRepeatable: sectionForm.isRepeatable,
      conditionLogic: sectionForm.conditionLogic || null,
    })
    sectionFormDirty.value = false
    ElMessage.success('分区已保存')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleRemoveSection(sectionId: number) {
  if (isReadonly.value) return
  try {
    await ElMessageBox.confirm('确认删除此分区及其所有字段？', '确认删除', { type: 'warning' })
    await editor.removeSection(sectionId)
    if (selectedSectionId.value === sectionId) {
      selectedSectionId.value = null
      selectedItem.value = null
    }
    ElMessage.success('分区已删除')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

function handleSelectSection(sectionId: number) {
  selectedSectionId.value = sectionId
  selectedItem.value = null
}

// ==================== Item Operations ====================

function openAddItem(sectionId: number) {
  if (isReadonly.value) return
  addItemToSectionId.value = sectionId
  showItemTypeSelector.value = true
}

async function handleSelectItemType(type: ItemType | null, isScored: boolean, scoringMode?: ScoringMode) {
  if (!addItemToSectionId.value) return
  try {
    const item = await editor.addItem(addItemToSectionId.value, type, isScored, scoringMode)
    selectedItem.value = item
    showItemTypeSelector.value = false
    ElMessage.success(isScored ? '评分项已添加' : '采集项已添加')
  } catch (e: any) {
    ElMessage.error(e.message || '添加字段失败')
  }
}

function handleSelectItem(item: TemplateItem) {
  selectedItem.value = item
}

async function handleSaveItem(data: Partial<TemplateItem>) {
  if (!selectedItem.value) return
  try {
    await editor.editItem(selectedItem.value.id, data)
    const items = editor.itemsBySection.value.get(selectedItem.value.sectionId) || []
    selectedItem.value = items.find(i => i.id === selectedItem.value!.id) || null
    ElMessage.success('字段已保存')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleRemoveItem(item: TemplateItem) {
  if (isReadonly.value) return
  try {
    await editor.removeItem(item.id)
    if (selectedItem.value?.id === item.id) selectedItem.value = null
    ElMessage.success('字段已删除')
  } catch (e: any) {
    ElMessage.error(e.message || '删除失败')
  }
}

// ==================== Publish ====================

async function handlePublish() {
  if (!template.value || template.value.status !== 'DRAFT') return
  try {
    await ElMessageBox.confirm('发布后将创建不可变版本快照，确认发布？', '确认发布', { type: 'warning' })
    await tplStore.publish(template.value.id)
    await tplStore.loadTemplate(template.value.id)
    ElMessage.success('发布成功')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '发布失败')
  }
}

// ==================== Scoring Handlers ====================

async function saveScoringProfile() {
  if (!scoringProfile.value) return
  await scoringStore.updateProfile(scoringProfile.value.id, {
    maxScore: scoringForm.maxScore,
    minScore: scoringForm.minScore,
    precisionDigits: scoringForm.precisionDigits,
  })
  scoringDirty.value = false
}

async function handleCreateDimension(data: CreateDimensionRequest) {
  if (!scoringProfile.value) return
  await scoringStore.createDimension(scoringProfile.value.id, data)
}
async function handleUpdateDimension(id: number, data: UpdateDimensionRequest) {
  if (!scoringProfile.value) return
  await scoringStore.updateDimension(scoringProfile.value.id, id, data)
}
async function handleDeleteDimension(id: number) {
  if (!scoringProfile.value) return
  await scoringStore.deleteDimension(scoringProfile.value.id, id)
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
async function handleApplyPreset(bands: CreateGradeBandRequest[]) {
  if (!scoringProfile.value) return
  for (const id of scoringStore.gradeBands.map(b => b.id)) {
    await scoringStore.deleteGradeBand(scoringProfile.value.id, id)
  }
  for (const band of bands) {
    await scoringStore.createGradeBand(scoringProfile.value.id, band)
  }
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

async function handleSyncModuleDimensions() {
  if (!scoringProfile.value) return
  try {
    await scoringProfileApi.syncDimensionsFromModules(scoringProfile.value.id)
    await scoringStore.loadDimensions(scoringProfile.value.id)
    ElMessage.success('已从子模板同步维度')
  } catch (e: any) {
    ElMessage.error(e.message || '同步失败')
  }
}

async function handleSaveAdvancedSettings(data: UpdateAdvancedSettingsRequest) {
  if (!scoringProfile.value) return
  await scoringStore.updateAdvancedSettings(scoringProfile.value.id, data)
  scoringProfile.value = scoringStore.currentProfile ?? null
}

async function handlePublishVersion(changeSummary: string) {
  if (!scoringProfile.value) return
  await scoringStore.publishVersion(scoringProfile.value.id, { changeSummary })
  scoringProfile.value = scoringStore.currentProfile ?? null
}

// ==================== Init ====================

onMounted(() => {
  if (isCreateMode.value) {
    router.replace('/inspection/v7/config')
  } else {
    loadTemplate()
    loadResponseSets()
    loadScoringProfile()
  }
})
</script>

<template>
  <div class="flex h-full flex-col">
    <!-- Error state -->
    <InspErrorState v-if="loadError" :message="loadError" @retry="loadTemplate" />

    <!-- Loading state -->
    <div v-else-if="!template" class="flex h-full items-center justify-center text-sm text-gray-400">
      加载中...
    </div>

    <!-- Editor -->
    <template v-else>
      <!-- Top bar -->
      <div class="te-topbar">
        <div class="te-topbar-left">
          <button class="te-back" @click="router.push('/inspection/v7/config')">
            <ArrowLeft :size="16" />
          </button>
          <div class="te-title-group">
            <div class="te-title-row">
              <h1
                class="te-title"
                :class="{ 'te-title-editable': !isReadonly }"
                @click="openEditInfo"
              >
                {{ template.templateName }}
              </h1>
              <span class="te-status" :style="{ color: TemplateStatusConfig[template.status]?.color }">
                {{ TemplateStatusConfig[template.status]?.label }}
              </span>
            </div>
            <div class="te-subtitle">{{ template.templateCode }} · v{{ template.latestVersion }}</div>
          </div>

          <!-- View tabs -->
          <div class="te-tabs">
            <button :class="['te-tab', activeView === 'structure' && 'active']" @click="activeView = 'structure'">
              模板结构
            </button>
            <button :class="['te-tab', activeView === 'modules' && 'active']" @click="activeView = 'modules'">
              子模板引用
            </button>
            <button :class="['te-tab', activeView === 'scoring' && 'active']" @click="activeView = 'scoring'">
              评分配置
              <span v-if="scoringProfile" class="te-tab-dot ok" />
              <span v-else class="te-tab-dot empty" />
            </button>
          </div>
        </div>

        <div class="te-topbar-right">
          <template v-if="activeView === 'structure'">
            <button class="te-btn-ghost" @click="showPreview = !showPreview">
              <Eye :size="14" />
              {{ showPreview ? '编辑' : '预览' }}
            </button>
          </template>
          <template v-if="activeView === 'scoring' && scoringProfile && scoringDirty">
            <button class="te-btn-primary" @click="saveScoringProfile">保存配置</button>
          </template>
          <button
            v-if="template.status === 'DRAFT'"
            class="te-btn-green"
            @click="handlePublish"
          >
            <Upload :size="14" /> 发布
          </button>
        </div>
      </div>

      <!-- Template info editing -->
      <div v-if="editingInfo" class="te-info-edit">
        <div class="te-info-row">
          <div class="flex-1">
            <label>模板名称</label>
            <input v-model="infoForm.templateName" />
          </div>
          <div class="flex-1">
            <label>描述</label>
            <input v-model="infoForm.description" />
          </div>
          <div class="w-48">
            <label>标签</label>
            <input v-model="infoForm.tags" placeholder="逗号分隔" />
          </div>
          <button class="te-btn-primary" @click="saveInfo"><Save :size="14" /></button>
          <button class="te-btn-ghost" @click="editingInfo = false">取消</button>
        </div>
      </div>

      <!-- ═══════ Main content ═══════ -->
      <div class="flex flex-1 overflow-hidden">

        <!-- ============ VIEW: 模板结构 ============ -->
        <template v-if="activeView === 'structure'">
          <!-- Preview mode -->
          <TemplatePreview
            v-if="showPreview"
            :sections="editor.sections.value"
            :items-by-section="editor.itemsBySection.value"
            class="flex-1"
            @close="showPreview = false"
          />

          <!-- Edit mode -->
          <template v-else>
            <!-- Left: Section tree -->
            <div class="te-tree-col">
              <SectionTree
                :sections="editor.sections.value"
                :selected-section-id="selectedSectionId"
                :readonly="isReadonly"
                @select="handleSelectSection"
                @add="handleAddSection"
                @remove="handleRemoveSection"
              />
            </div>

            <!-- Center: Items list -->
            <div class="te-items-col">
              <template v-if="selectedSectionId">
                <div class="te-items-header">
                  <h3 class="te-items-title">
                    {{ editor.sections.value.find(s => s.id === selectedSectionId)?.sectionName || '分区' }}
                    <span class="te-items-count">{{ currentSectionItems.length }} 个字段</span>
                  </h3>
                  <button v-if="!isReadonly" class="te-btn-primary te-btn-sm" @click="openAddItem(selectedSectionId!)">
                    <Plus :size="12" /> 添加字段
                  </button>
                </div>

                <div v-if="currentSectionItems.length === 0" class="te-items-empty">
                  暂无字段，点击上方按钮添加
                </div>
                <div v-else class="te-items-list">
                  <div
                    v-for="item in currentSectionItems"
                    :key="item.id"
                    class="te-item group"
                    :class="{ selected: selectedItem?.id === item.id }"
                    @click="handleSelectItem(item)"
                  >
                    <div class="te-item-left">
                      <GripVertical :size="12" class="te-item-grip" />
                      <div class="te-item-info">
                        <span class="te-item-name">{{ item.itemName || item.itemCode }}</span>
                        <span class="te-item-type">
                          <template v-if="item.isScored && item.scoringConfig">
                            {{ (() => { try { const c = JSON.parse(item.scoringConfig); return ScoringModeConfig[c.mode as ScoringMode]?.label || ItemTypeConfig[item.itemType]?.label } catch { return ItemTypeConfig[item.itemType]?.label } })() }}
                          </template>
                          <template v-else-if="!item.isScored">
                            {{ ItemTypeConfig[item.itemType]?.label }} · 采集
                          </template>
                          <template v-else>
                            {{ ItemTypeConfig[item.itemType]?.label }}
                          </template>
                        </span>
                      </div>
                    </div>
                    <div v-if="!isReadonly" class="te-item-actions">
                      <button @click.stop="handleSelectItem(item)"><Pencil :size="12" /></button>
                      <button class="danger" @click.stop="handleRemoveItem(item)"><Trash2 :size="12" /></button>
                    </div>
                  </div>
                </div>
              </template>
              <div v-else class="te-items-placeholder">← 选择一个分区查看字段</div>
            </div>

            <!-- Right: Editor panel -->
            <div class="te-editor-col">
              <!-- Item editor -->
              <ItemEditor
                v-if="selectedItem"
                :item="selectedItem"
                :response-sets="responseSets"
                :all-items="allItems"
                @save="handleSaveItem"
                @cancel="selectedItem = null"
              />

              <!-- Section editor (when section selected but no item) -->
              <div v-else-if="selectedSection && !selectedItem" class="te-sec-editor">
                <div class="te-sec-editor-head">
                  <h3>分区设置</h3>
                  <button
                    v-if="sectionFormDirty"
                    class="te-btn-primary te-btn-sm"
                    @click="handleSaveSection"
                  >保存</button>
                </div>

                <div class="te-sec-editor-body">
                  <!-- Name -->
                  <div class="te-sec-fld">
                    <label>分区名称</label>
                    <input
                      v-model="sectionForm.sectionName"
                      placeholder="请输入分区名称"
                      @input="markSectionDirty"
                    />
                  </div>

                  <!-- Repeatable -->
                  <label class="te-sec-check">
                    <input v-model="sectionForm.isRepeatable" type="checkbox" @change="markSectionDirty" />
                    <span>可重复</span>
                    <span class="te-sec-check-hint">允许检查时对该分区重复填写多次</span>
                  </label>

                  <!-- Condition logic -->
                  <div class="te-sec-cond">
                    <div class="te-sec-cond-head">
                      <span class="te-sec-cond-title">条件逻辑</span>
                      <span v-if="sectionForm.conditionLogic" class="te-sec-cond-badge">已配置</span>
                    </div>
                    <p class="te-sec-cond-desc">配置后，仅当条件满足时该分区才在检查中显示。不配置则始终显示。</p>
                    <ConditionBuilder
                      v-model="sectionForm.conditionLogic"
                      :all-items="allItems.map(i => ({ itemCode: i.itemCode, itemName: i.itemName }))"
                      :target-label="sectionForm.sectionName"
                      @update:model-value="markSectionDirty"
                    />
                    <button
                      v-if="sectionForm.conditionLogic"
                      class="te-sec-cond-clear"
                      @click="sectionForm.conditionLogic = ''; markSectionDirty()"
                    >清除条件</button>
                  </div>
                </div>
              </div>

              <div v-else class="te-items-placeholder">选择分区或字段进行编辑</div>
            </div>
          </template>
        </template>

        <!-- ============ VIEW: 子模板引用 ============ -->
        <template v-if="activeView === 'modules'">
          <div class="flex-1 overflow-y-auto bg-white">
            <ModuleRefPanel :template-id="templateId" :readonly="isReadonly" />
          </div>
        </template>

        <!-- ============ VIEW: 评分配置 ============ -->
        <template v-if="activeView === 'scoring'">
          <!-- Loading -->
          <div v-if="scoringLoading" class="flex-1 flex items-center justify-center">
            <div style="color:#8c95a3; font-size:13px;">加载评分配置...</div>
          </div>

          <!-- Scoring 2-column layout -->
          <template v-else>
            <!-- Left: scrollable config -->
            <div class="sp-left">
              <!-- Basic settings -->
              <div class="sp-card">
                <div class="sp-section-head"><h3 class="sp-section-title">基础设置</h3></div>
                <div class="grid grid-cols-3 gap-4">
                  <div class="sp-fld">
                    <label title="分数的绝对上限">最高分</label>
                    <input v-model.number="scoringForm.maxScore" type="number" @input="scoringDirty = true" />
                  </div>
                  <div class="sp-fld">
                    <label title="分数的绝对下限">最低分</label>
                    <input v-model.number="scoringForm.minScore" type="number" @input="scoringDirty = true" />
                  </div>
                  <div class="sp-fld">
                    <label title="最终分数保留的小数位数">精度</label>
                    <input v-model.number="scoringForm.precisionDigits" type="number" min="0" max="4" @input="scoringDirty = true" />
                  </div>
                </div>
              </div>

              <div class="sp-card">
                <DimensionTable
                  :dimensions="scoringStore.dimensions"
                  :template-id="templateId"
                  @create="handleCreateDimension"
                  @update="handleUpdateDimension"
                  @delete="handleDeleteDimension"
                  @sync-modules="handleSyncModuleDimensions"
                />
              </div>

              <div class="sp-card">
                <GradeBandEditor
                  :grade-bands="scoringStore.gradeBands"
                  :dimensions="scoringStore.dimensions"
                  @create="handleCreateGradeBand"
                  @update="handleUpdateGradeBand"
                  @delete="handleDeleteGradeBand"
                  @apply-preset="handleApplyPreset"
                />
              </div>

              <div class="sp-card">
                <CalcRuleChain
                  :rules="scoringStore.rules"
                  :template-id="templateId"
                  @create="handleCreateRule"
                  @update="handleUpdateRule"
                  @delete="handleDeleteRule"
                />
              </div>

              <div class="sp-card">
                <AdvancedScoringSettings
                  v-if="scoringProfile"
                  :profile="scoringProfile"
                  @save="handleSaveAdvancedSettings"
                />
              </div>
            </div>

            <!-- Right: sidebar -->
            <div class="sp-right">
              <div class="sp-health">
                <div class="sp-health-title">
                  <ShieldCheck :size="14" class="sp-health-icon" />
                  <span>配置检查</span>
                </div>
                <div class="sp-checks">
                  <div v-for="check in healthChecks" :key="check.key" class="sp-check" :class="check.status">
                    <component :is="check.status === 'ok' ? CheckCircle2 : check.status === 'warn' ? AlertTriangle : XCircle" :size="14" />
                    <span>{{ check.label }}</span>
                  </div>
                </div>
              </div>

              <VersionHistory
                v-if="scoringProfile"
                :versions="scoringStore.versions"
                :current-version="scoringProfile.currentVersion || 0"
                @publish="handlePublishVersion"
              />

              <ScoreSimulator
                v-if="scoringProfile"
                :profile="scoringProfile"
                :dimensions="scoringStore.dimensions"
                :grade-bands="scoringStore.gradeBands"
                :rules="scoringStore.rules"
                :template-id="scoringProfile.templateId"
              />
            </div>
          </template>
        </template>
      </div>
    </template>

    <!-- Item type selector dialog -->
    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="showItemTypeSelector"
          class="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
          @click.self="showItemTypeSelector = false"
        >
          <div class="max-h-[80vh] w-[520px] overflow-y-auto rounded-lg bg-white p-6 shadow-xl">
            <h3 class="mb-4 text-base font-semibold text-gray-800">添加检查项</h3>
            <ItemTypeSelector @select="handleSelectItemType" />
            <div class="mt-4 flex justify-end">
              <button class="te-btn-ghost" @click="showItemTypeSelector = false">取消</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

  </div>
</template>

<style scoped>
/* ═══════ Top bar ═══════ */
.te-topbar { display:flex; align-items:center; justify-content:space-between; padding:8px 16px; background:#fff; border-bottom:1px solid #e8ecf0; flex-shrink:0; }
.te-topbar-left { display:flex; align-items:center; gap:12px; }
.te-topbar-right { display:flex; align-items:center; gap:8px; }

.te-back { background:none; border:none; padding:5px; color:#8c95a3; cursor:pointer; border-radius:6px; display:flex; align-items:center; transition:all 0.12s; }
.te-back:hover { color:#1a6dff; background:#f0f4ff; }

.te-title-group { display:flex; flex-direction:column; gap:1px; }
.te-title-row { display:flex; align-items:center; gap:8px; }
.te-title { font-size:14px; font-weight:600; color:#1e2a3a; margin:0; }
.te-title-editable { cursor:pointer; }
.te-title-editable:hover { color:#1a6dff; }
.te-status { font-size:11px; font-weight:500; }
.te-subtitle { font-size:11px; color:#8c95a3; }

/* ═══════ Tabs ═══════ */
.te-tabs { display:flex; gap:2px; margin-left:16px; background:#f0f2f5; border-radius:7px; padding:2px; }
.te-tab { position:relative; display:inline-flex; align-items:center; gap:5px; padding:5px 14px; font-size:12px; font-weight:500; color:#5a6474; background:none; border:none; border-radius:5px; cursor:pointer; transition:all 0.15s; white-space:nowrap; }
.te-tab:hover { color:#1e2a3a; }
.te-tab.active { background:#fff; color:#1a6dff; font-weight:600; box-shadow:0 1px 3px rgba(0,0,0,0.06); }
.te-tab-dot { width:6px; height:6px; border-radius:50%; flex-shrink:0; }
.te-tab-dot.ok { background:#34d399; }
.te-tab-dot.empty { background:#d1d5db; }

/* ═══════ Buttons ═══════ */
.te-btn-primary { display:inline-flex; align-items:center; gap:4px; padding:6px 14px; background:#1a6dff; color:#fff; border:none; border-radius:7px; font-size:12px; font-weight:500; cursor:pointer; transition:background 0.15s; white-space:nowrap; }
.te-btn-primary:hover { background:#1558d6; }
.te-btn-primary.te-btn-sm { padding:4px 10px; font-size:11px; border-radius:6px; }
.te-btn-ghost { display:inline-flex; align-items:center; gap:4px; padding:6px 14px; background:none; border:1px solid #dce1e8; border-radius:7px; font-size:12px; color:#5a6474; cursor:pointer; transition:all 0.15s; }
.te-btn-ghost:hover { background:#f4f6f9; }
.te-btn-green { display:inline-flex; align-items:center; gap:4px; padding:6px 14px; background:#059669; color:#fff; border:none; border-radius:7px; font-size:12px; font-weight:500; cursor:pointer; transition:background 0.15s; }
.te-btn-green:hover { background:#047857; }

/* ═══════ Info edit ═══════ */
.te-info-edit { padding:10px 16px; border-bottom:1px solid #dbeafe; background:#eff6ff; }
.te-info-row { display:flex; align-items:flex-end; gap:10px; }
.te-info-edit label { display:block; font-size:11px; color:#5a6474; margin-bottom:3px; }
.te-info-edit input { width:100%; border:1px solid #dce1e8; border-radius:6px; padding:5px 10px; font-size:12px; outline:none; }
.te-info-edit input:focus { border-color:#7aadff; box-shadow:0 0 0 2px rgba(26,109,255,0.08); }

/* ═══════ Structure view columns ═══════ */
.te-tree-col { width:220px; flex-shrink:0; overflow-y:auto; border-right:1px solid #e8ecf0; background:#f8f9fb; padding:10px; }
.te-items-col { min-width:200px; flex:1; overflow-y:auto; border-right:1px solid #e8ecf0; padding:12px 16px; }
.te-editor-col { width:420px; flex-shrink:0; overflow-y:auto; background:#fff; }

.te-items-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:10px; }
.te-items-title { font-size:13px; font-weight:500; color:#1e2a3a; }
.te-items-count { font-size:11px; color:#8c95a3; margin-left:6px; font-weight:400; }
.te-items-empty { padding:40px 0; text-align:center; font-size:12px; color:#8c95a3; }
.te-items-placeholder { display:flex; align-items:center; justify-content:center; height:100%; font-size:12px; color:#8c95a3; }
.te-items-list { display:flex; flex-direction:column; gap:3px; }

.te-item { display:flex; align-items:center; justify-content:space-between; padding:8px 10px; border-radius:8px; border:1px solid #f0f2f5; cursor:pointer; transition:all 0.12s; }
.te-item:hover { border-color:#dce1e8; background:#f8f9fb; }
.te-item.selected { border-color:#93c5fd; background:#eff6ff; }
.te-item-left { display:flex; align-items:center; gap:8px; overflow:hidden; }
.te-item-grip { color:#d1d5db; flex-shrink:0; }
.te-item-info { min-width:0; }
.te-item-name { display:block; font-size:13px; color:#1e2a3a; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.te-item-type { display:block; font-size:11px; color:#8c95a3; }
.te-item-actions { display:flex; gap:3px; opacity:0; transition:opacity 0.12s; }
.group:hover .te-item-actions { opacity:1; }
.te-item-actions button { background:none; border:none; padding:3px; color:#8c95a3; cursor:pointer; border-radius:4px; display:flex; }
.te-item-actions button:hover { color:#1a6dff; background:#f0f4ff; }
.te-item-actions button.danger:hover { color:#ef4444; background:#fef2f2; }

/* ═══════ Scoring view (reuse sp-* from ScoringProfileEditor) ═══════ */
.sp-left { flex:1; overflow-y:auto; padding:16px; display:flex; flex-direction:column; gap:14px; min-width:0; background:#f4f6f9; }
.sp-right { width:360px; flex-shrink:0; border-left:1px solid #e8ecf0; background:#fff; overflow-y:auto; display:flex; flex-direction:column; }
.sp-card { background:#fff; border-radius:14px; box-shadow:0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.02); padding:18px; }
.sp-section-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:12px; }
.sp-section-title { font-size:13px; font-weight:600; color:#1e2a3a; margin:0; }
.sp-fld label { display:block; font-size:12px; font-weight:500; color:#5a6474; margin-bottom:4px; }
.sp-fld input, .sp-fld select { width:100%; border:1px solid #dce1e8; border-radius:8px; padding:7px 10px; font-size:13px; outline:none; transition:border-color 0.2s, box-shadow 0.2s; color:#1e2a3a; background:#fff; }
.sp-fld input:focus, .sp-fld select:focus { border-color:#7aadff; box-shadow:0 0 0 3px rgba(26,109,255,0.08); }

.sp-health { padding:14px; border-bottom:1px solid #eef0f3; }
.sp-health-title { display:flex; align-items:center; gap:6px; font-size:13px; font-weight:600; color:#1e2a3a; margin-bottom:10px; }
.sp-health-icon { color:#8c95a3; }
.sp-checks { display:flex; flex-direction:column; gap:5px; }
.sp-check { display:flex; align-items:center; gap:8px; font-size:12px; padding:4px 8px; border-radius:6px; }
.sp-check.ok { color:#059669; background:#f0fdf4; }
.sp-check.warn { color:#d97706; background:#fffbeb; }
.sp-check.error { color:#dc2626; background:#fef2f2; }

/* ═══════ Section editor (right panel) ═══════ */
.te-sec-editor { display:flex; flex-direction:column; height:100%; }
.te-sec-editor-head { display:flex; align-items:center; justify-content:space-between; padding:14px 16px; border-bottom:1px solid #e8ecf0; }
.te-sec-editor-head h3 { font-size:13px; font-weight:600; color:#1e2a3a; margin:0; }
.te-sec-editor-body { padding:16px; display:flex; flex-direction:column; gap:16px; overflow-y:auto; flex:1; }

.te-sec-fld label { display:block; font-size:12px; font-weight:500; color:#5a6474; margin-bottom:5px; }
.te-sec-fld input[type="text"], .te-sec-fld input:not([type]) { width:100%; border:1px solid #dce1e8; border-radius:8px; padding:8px 12px; font-size:13px; outline:none; transition:border-color 0.2s, box-shadow 0.2s; color:#1e2a3a; background:#fff; }
.te-sec-fld input:focus { border-color:#7aadff; box-shadow:0 0 0 3px rgba(26,109,255,0.08); }

.te-sec-check { display:flex; align-items:center; gap:8px; cursor:pointer; font-size:13px; color:#1e2a3a; padding:6px 0; }
.te-sec-check input[type="checkbox"] { accent-color:#1a6dff; width:15px; height:15px; }
.te-sec-check-hint { font-size:11px; color:#8c95a3; margin-left:4px; }

.te-sec-cond { border:1px solid #e8ecf0; border-radius:10px; padding:14px; background:#fafbfc; }
.te-sec-cond-head { display:flex; align-items:center; gap:8px; margin-bottom:4px; }
.te-sec-cond-title { font-size:12px; font-weight:600; color:#1e2a3a; }
.te-sec-cond-badge { font-size:10px; font-weight:500; color:#059669; background:#ecfdf5; padding:1px 6px; border-radius:4px; }
.te-sec-cond-desc { font-size:11px; color:#8c95a3; line-height:1.5; margin-bottom:10px; }
.te-sec-cond-clear { margin-top:8px; background:none; border:none; font-size:12px; color:#dc2626; cursor:pointer; padding:0; }
.te-sec-cond-clear:hover { text-decoration:underline; }

/* ═══════ Transitions ═══════ */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
