<template>
  <div class="sp-section">
    <div class="sp-section-header">
      <h3 class="sp-section-title">分区权重</h3>
      <div class="sp-add-wrap" style="display:flex;gap:6px;">
        <button class="sp-btn-ghost sm" @click="$emit('sync-modules')" title="从子模板同步维度">
          同步子模板
        </button>
        <button class="sp-btn-primary sm" @click="showPresetPicker = !showPresetPicker">
          添加分区
        </button>
        <Transition name="sp-dropdown">
          <div v-if="showPresetPicker" class="sp-preset-panel" @click.stop>
            <div class="sp-preset-list">
              <button
                v-for="s in availableSections"
                :key="s.id"
                class="sp-preset-item"
                @click="addFromSection(s)"
              >
                {{ s.sectionName }}
              </button>
              <div v-if="availableSections.length === 0" class="sp-preset-empty">所有分区已添加</div>
            </div>
          </div>
        </Transition>
      </div>
    </div>

    <p class="sp-section-desc">各分区权重之和应为 100。点击分区可展开查看关联的检查项并配置项目权重。</p>

    <div v-if="dimensions.length === 0" class="sp-empty">
      暂无分区，点击上方按钮从模板结构中添加
    </div>

    <div v-else class="sp-dim-list">
      <div
        v-for="dim in dimensions"
        :key="dim.id"
        class="sp-dim-card group"
        :class="{ 'is-expanded': expandedDimId === dim.id }"
      >
        <div class="sp-dim-row" @click="toggleExpand(dim.id)">
          <div class="sp-dim-left">
            <span class="sp-dim-chevron" :class="{ rotated: expandedDimId === dim.id }">&#9656;</span>
            <span class="sp-dim-code">{{ dim.dimensionCode }}</span>
            <span class="sp-dim-name">{{ dim.dimensionName }}</span>
            <span v-if="dim.sourceType === 'MODULE'" class="sp-dim-module-tag">子模板</span>
            <span v-if="getItemsForDimension(dim.id).length > 0" class="sp-dim-item-count">
              {{ getItemsForDimension(dim.id).length }} 项
            </span>
          </div>
          <div class="sp-dim-right">
            <div class="sp-dim-stat">
              <div class="sp-dim-stat-label">权重</div>
              <div class="sp-dim-stat-value sp-weight-value">{{ dim.weight }}%</div>
            </div>
            <div class="sp-dim-stat">
              <div class="sp-dim-stat-label">基础分</div>
              <div class="sp-dim-stat-value">{{ dim.baseScore }}</div>
            </div>
            <div v-if="dim.passThreshold" class="sp-dim-stat">
              <div class="sp-dim-stat-label">及格线</div>
              <div class="sp-dim-stat-value sp-pass-value">{{ dim.passThreshold }}</div>
            </div>
            <div class="sp-dim-actions" @click.stop>
              <button class="sp-ic-s" @click="startEdit(dim)">
                <component :is="iconMap.Pencil" class="w-3.5 h-3.5" />
              </button>
              <button class="sp-ic-s danger" @click="$emit('delete', dim.id)">
                <component :is="iconMap.Trash2" class="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>

        <!-- Expanded: Item weights -->
        <div v-if="expandedDimId === dim.id" class="sp-dim-items">
          <div v-if="loadingItems" class="sp-dim-items-loading">加载中...</div>
          <template v-else>
            <table v-if="getItemsForDimension(dim.id).length > 0" class="sp-item-table">
              <thead>
                <tr>
                  <th style="width:35%">检查项</th>
                  <th style="width:15%">类型</th>
                  <th style="width:18%">权重</th>
                  <th style="width:18%">占比</th>
                  <th style="width:14%"></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in getItemsForDimension(dim.id)" :key="item.id">
                  <td>
                    <span class="sp-item-name">{{ item.itemName || item.itemCode }}</span>
                  </td>
                  <td>
                    <span class="sp-item-badge scored">评分</span>
                  </td>
                  <td>
                    <input
                      v-model.number="item.itemWeight"
                      type="number"
                      :min="0.01"
                      :max="100"
                      :step="0.1"
                      class="sp-item-weight-input"
                      @change="handleItemWeightChange(item)"
                    />
                  </td>
                  <td>
                    <span class="sp-item-pct">{{ getItemPct(dim.id, item) }}</span>
                  </td>
                  <td style="text-align:right">
                    <button class="sp-ic-s danger" title="移出分区" @click="handleRemoveFromDimension(item)">
                      <component :is="iconMap.Trash2" class="w-3 h-3" />
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-else class="sp-dim-items-empty">该分区下暂无检查项</div>

            <!-- Add item to dimension -->
            <div v-if="getUnassignedForPicker().length > 0" class="sp-dim-add-row">
              <select
                class="sp-assign-select"
                :value="''"
                @change="handleAssignDimension(($event.target as HTMLSelectElement).value, dim.id)"
              >
                <option value="" disabled>+ 添加检查项...</option>
                <option v-for="it in getUnassignedForPicker()" :key="it.id" :value="it.id">
                  {{ it.itemName || it.itemCode }}
                </option>
              </select>
            </div>
          </template>
        </div>
      </div>

      <!-- Unassigned items summary -->
      <div v-if="unassignedScoredItems.length > 0" class="sp-unassigned-hint">
        <span class="sp-unassigned-count">{{ unassignedScoredItems.length }}</span>
        个评分项未分配分区，展开分区可添加
      </div>
    </div>

    <!-- Weight Sum Bar -->
    <div v-if="dimensions.length > 0" class="sp-weight-row">
      <div class="sp-weight-bar-bg">
        <div
          class="sp-weight-bar-fill"
          :class="totalWeight === 100 ? 'is-ok' : totalWeight > 100 ? 'is-over' : 'is-under'"
          :style="{ width: Math.min(totalWeight, 100) + '%' }"
        />
      </div>
      <span class="sp-weight-total" :class="totalWeight === 100 ? 'is-ok' : 'is-bad'">
        {{ totalWeight }}%
      </span>
      <button v-if="totalWeight !== 100 && dimensions.length > 1" class="sp-redistribute-btn" @click="redistributeEvenly">
        均分
      </button>
    </div>

    <!-- Edit Dialog -->
    <Teleport to="body">
      <Transition name="sp-modal">
        <div v-if="editingDim" class="sp-mask" @mousedown="onMaskMouseDown" @click="onMaskClick">
          <div class="sp-modal" style="width:560px">
            <div class="sp-modal-head">
              <h3>编辑分区</h3>
              <button class="sp-modal-close" @click="closeDialog">&times;</button>
            </div>
            <div class="sp-modal-body sp-modal-scroll">
              <div class="sp-fld">
                <label>分区名称</label>
                <input v-model="form.dimensionName" placeholder="如 安全" />
              </div>
              <div class="sp-form-row">
                <div class="sp-fld" style="flex:1;">
                  <label>权重 (%)</label>
                  <input v-model.number="form.weight" type="number" min="0" max="100" />
                </div>
                <div class="sp-fld" style="flex:1;">
                  <label>基础分</label>
                  <input v-model.number="form.baseScore" type="number" />
                </div>
                <div class="sp-fld" style="flex:1;">
                  <label>及格线</label>
                  <input v-model.number="form.passThreshold" type="number" placeholder="可选" />
                </div>
              </div>
            </div>
            <div class="sp-modal-foot">
              <button class="sp-btn-ghost" @click="closeDialog">取消</button>
              <button class="sp-btn-primary" @click="handleSubmit">保存</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { Pencil, Trash2 } from 'lucide-vue-next'
import type { ScoreDimension, CreateDimensionRequest, UpdateDimensionRequest } from '@/types/insp/scoring'
import type { TemplateItem, TemplateSection } from '@/types/insp/template'
import * as inspTemplateApi from '@/api/insp/template'
import { updateItem as updateItemApi } from '@/api/insp/template'

const iconMap = { Pencil, Trash2 }

const props = defineProps<{
  dimensions: ScoreDimension[]
  templateId: number
}>()

const emit = defineEmits<{
  create: [data: CreateDimensionRequest]
  update: [id: number, data: UpdateDimensionRequest]
  delete: [id: number]
  'sync-modules': []
}>()

const showPresetPicker = ref(false)
const editingDim = ref<ScoreDimension | null>(null)
const expandedDimId = ref<number | null>(null)

// ==================== Sections as dimension source ====================
const sections = ref<TemplateSection[]>([])

async function loadSections() {
  if (!props.templateId) return
  try {
    sections.value = await inspTemplateApi.getSections(props.templateId)
  } catch {
    // ignore
  }
}

watch(() => props.templateId, () => loadSections(), { immediate: true })

const availableSections = computed(() => {
  const usedCodes = new Set(props.dimensions.map(d => d.dimensionCode))
  return sections.value.filter(s => !usedCodes.has(s.sectionCode))
})

const pendingAutoAssignSectionId = ref<number | null>(null)

function addFromSection(section: TemplateSection) {
  pendingAutoAssignSectionId.value = section.id
  const newCount = props.dimensions.length + 1
  const evenWeight = Math.round(100 / newCount)
  emit('create', {
    dimensionCode: section.sectionCode,
    dimensionName: section.sectionName,
    weight: evenWeight,
    baseScore: 100,
    passThreshold: null,
  })
  showPresetPicker.value = false
  // Redistribute existing dimensions' weights evenly
  redistributeWeights(newCount)
}

// Auto-assign scored items when a new dimension is created from a section
watch(() => props.dimensions.length, async (newLen, oldLen) => {
  if (newLen > oldLen && pendingAutoAssignSectionId.value) {
    const sectionId = pendingAutoAssignSectionId.value
    pendingAutoAssignSectionId.value = null
    // Find the newly created dimension (last one)
    const newDim = props.dimensions[props.dimensions.length - 1]
    if (!newDim) return
    // Get scored items from that section
    const sectionItems = allItems.value.filter(
      it => Number(it.sectionId) === Number(sectionId) && it.isScored && !it.dimensionId
    )
    for (const item of sectionItems) {
      try {
        await updateItemApi(item.id, { dimensionId: newDim.id, itemWeight: item.itemWeight || 100 })
        item.dimensionId = newDim.id
        if (!item.itemWeight) item.itemWeight = 100
      } catch (e) {
        console.error('Failed to auto-assign item to dimension', e)
      }
    }
  }
})

async function redistributeWeights(totalCount: number) {
  const evenWeight = Math.round(100 / totalCount)
  const remainder = 100 - evenWeight * totalCount
  for (let i = 0; i < props.dimensions.length; i++) {
    const dim = props.dimensions[i]
    // Last one gets remainder to ensure sum = 100
    const w = i === 0 ? evenWeight + remainder : evenWeight
    if (dim.weight !== w) {
      emit('update', dim.id, {
        dimensionName: dim.dimensionName,
        weight: w,
        baseScore: dim.baseScore,
        passThreshold: dim.passThreshold,
      })
    }
  }
}

function redistributeEvenly() {
  const count = props.dimensions.length
  if (count === 0) return
  const evenWeight = Math.round(100 / count)
  const remainder = 100 - evenWeight * count
  for (let i = 0; i < props.dimensions.length; i++) {
    const dim = props.dimensions[i]
    const w = i === 0 ? evenWeight + remainder : evenWeight
    emit('update', dim.id, {
      dimensionName: dim.dimensionName,
      weight: w,
      baseScore: dim.baseScore,
      passThreshold: dim.passThreshold,
    })
  }
}

function onClickOutside(e: MouseEvent) {
  if (showPresetPicker.value) {
    const wrap = (e.target as HTMLElement)?.closest('.sp-add-wrap')
    if (!wrap) showPresetPicker.value = false
  }
}
onMounted(() => document.addEventListener('click', onClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', onClickOutside))

const form = ref({
  dimensionCode: '',
  dimensionName: '',
  weight: 100,
  baseScore: 100,
  passThreshold: null as number | null,
})

const totalWeight = computed(() => props.dimensions.reduce((sum, d) => sum + d.weight, 0))

// ==================== Modal close guard ====================
const maskMouseDownTarget = ref<EventTarget | null>(null)
function onMaskMouseDown(e: MouseEvent) { maskMouseDownTarget.value = e.target }
function onMaskClick(e: MouseEvent) {
  if (e.target === e.currentTarget && maskMouseDownTarget.value === e.currentTarget) closeDialog()
  maskMouseDownTarget.value = null
}

// ==================== Template items ====================
const allItems = ref<TemplateItem[]>([])
const loadingItems = ref(false)

async function loadItems() {
  if (!props.templateId) return
  loadingItems.value = true
  try {
    const sections = await inspTemplateApi.getSections(props.templateId)
    const items: TemplateItem[] = []
    for (const section of sections) {
      const sectionItems = await inspTemplateApi.getItems(section.id)
      items.push(...sectionItems)
    }
    allItems.value = items
  } catch (e) {
    console.error('Failed to load template items', e)
  } finally {
    loadingItems.value = false
  }
}

watch(() => props.templateId, () => loadItems(), { immediate: true })

function getItemsForDimension(dimId: number): TemplateItem[] {
  return allItems.value.filter(it => it.isScored && String(it.dimensionId) === String(dimId))
}

const unassignedScoredItems = computed(() =>
  allItems.value.filter(it => it.isScored && !it.dimensionId)
)

function getUnassignedForPicker(): TemplateItem[] {
  return allItems.value.filter(it => it.isScored && !it.dimensionId)
}

function getItemPct(dimId: number, item: TemplateItem): string {
  const items = getItemsForDimension(dimId)
  const total = items.reduce((s, it) => s + (it.itemWeight ?? 100), 0)
  if (total === 0) return '0%'
  return ((item.itemWeight ?? 100) / total * 100).toFixed(1) + '%'
}

// ==================== Item dimension assignment ====================
async function handleItemWeightChange(item: TemplateItem) {
  try {
    await updateItemApi(item.id, { itemWeight: item.itemWeight, dimensionId: item.dimensionId })
  } catch (e) {
    console.error('Failed to update item weight', e)
  }
}

async function handleAssignDimension(itemIdStr: string, dimId: number) {
  const itemId = Number(itemIdStr)
  if (!itemId) return
  const item = allItems.value.find(it => Number(it.id) === itemId)
  if (!item) return
  try {
    await updateItemApi(item.id, { dimensionId: dimId, itemWeight: item.itemWeight || 100 })
    item.dimensionId = dimId
    if (!item.itemWeight) item.itemWeight = 100
  } catch (e) {
    console.error('Failed to assign dimension', e)
  }
}

async function handleRemoveFromDimension(item: TemplateItem) {
  try {
    await updateItemApi(item.id, { dimensionId: null })
    item.dimensionId = null
  } catch (e) {
    console.error('Failed to remove from dimension', e)
  }
}

// ==================== Expand/collapse ====================
function toggleExpand(dimId: number) {
  expandedDimId.value = expandedDimId.value === dimId ? null : dimId
}

// ==================== Dimension CRUD ====================
function startEdit(dim: ScoreDimension) {
  editingDim.value = dim
  form.value = {
    dimensionCode: dim.dimensionCode,
    dimensionName: dim.dimensionName,
    weight: dim.weight,
    baseScore: dim.baseScore,
    passThreshold: dim.passThreshold,
  }
}

function closeDialog() {
  editingDim.value = null
  form.value = { dimensionCode: '', dimensionName: '', weight: 100, baseScore: 100, passThreshold: null }
}

function handleSubmit() {
  if (!editingDim.value) return
  emit('update', editingDim.value.id, {
    dimensionName: form.value.dimensionName,
    weight: form.value.weight,
    baseScore: form.value.baseScore,
    passThreshold: form.value.passThreshold,
  })
  closeDialog()
}
</script>

<style scoped>
/* ========== Section layout ========== */
.sp-section { display:flex; flex-direction:column; gap:12px; }
.sp-section-header { display:flex; align-items:center; justify-content:space-between; }

/* ========== Preset picker ========== */
.sp-add-wrap { position:relative; }
.sp-preset-panel { position:absolute; top:calc(100% + 6px); right:0; z-index:20; width:180px; background:#fff; border:1px solid #e8ecf0; border-radius:10px; box-shadow:0 8px 24px rgba(0,0,0,0.12); overflow:hidden; }
.sp-preset-list { padding:6px; display:flex; flex-direction:column; gap:2px; max-height:240px; overflow-y:auto; }
.sp-preset-item { display:block; width:100%; text-align:left; padding:7px 12px; font-size:13px; color:#1e2a3a; background:none; border:none; border-radius:6px; cursor:pointer; transition:background 0.1s; }
.sp-preset-item:hover { background:#f0f4ff; color:#1a6dff; }
.sp-preset-empty { padding:8px 12px; font-size:12px; color:#b8c0cc; text-align:center; }
.sp-preset-divider { height:1px; background:#f0f2f5; margin:0 6px; }
.sp-preset-custom { display:block; width:100%; text-align:left; padding:8px 12px; font-size:12px; color:#8c95a3; background:none; border:none; cursor:pointer; transition:color 0.1s; }
.sp-preset-custom:hover { color:#1a6dff; }

.sp-dropdown-enter-active { transition:all 0.15s ease-out; }
.sp-dropdown-leave-active { transition:all 0.1s ease-in; }
.sp-dropdown-enter-from { opacity:0; transform:translateY(-4px); }
.sp-dropdown-leave-to { opacity:0; transform:translateY(-4px); }
.sp-section-title { font-size:14px; font-weight:600; color:#1e2a3a; margin:0; }
.sp-section-desc { font-size:12px; color:#8c95a3; margin:0; }

/* ========== Modal ========== */
.sp-mask { position:fixed; inset:0; z-index:1000; display:flex; align-items:center; justify-content:center; background:rgba(15,23,42,0.4); backdrop-filter:blur(2px); }
.sp-modal { background:#fff; border-radius:14px; box-shadow:0 24px 64px rgba(0,0,0,0.18); overflow:hidden; display:flex; flex-direction:column; max-height:80vh; }
.sp-modal-head { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0; }
.sp-modal-head h3 { font-size:16px; font-weight:600; color:#1e2a3a; margin:0; }
.sp-modal-close { background:none; border:none; font-size:22px; color:#b8c0cc; cursor:pointer; padding:0 4px; line-height:1; }
.sp-modal-close:hover { color:#5a6474; }
.sp-modal-body { display:flex; flex-direction:column; gap:16px; padding:20px 24px; }
.sp-modal-scroll { flex:1; overflow-y:auto; }
.sp-modal-foot { display:flex; justify-content:flex-end; gap:10px; padding:0 24px 20px; }

/* ========== Animation ========== */
.sp-modal-enter-active { transition:all 0.2s ease-out; }
.sp-modal-leave-active { transition:all 0.15s ease-in; }
.sp-modal-enter-from { opacity:0; }
.sp-modal-enter-from .sp-modal { transform:translateY(12px) scale(0.97); }
.sp-modal-leave-to { opacity:0; }
.sp-modal-leave-to .sp-modal { transform:translateY(-8px) scale(0.98); }

/* ========== Buttons ========== */
.sp-btn-primary { display:inline-flex; align-items:center; gap:5px; padding:8px 16px; background:#1a6dff; color:#fff; border:none; border-radius:8px; font-size:13px; font-weight:500; cursor:pointer; transition:background 0.15s; white-space:nowrap; }
.sp-btn-primary:hover { background:#1558d6; }
.sp-btn-primary.sm { padding:6px 12px; font-size:12px; border-radius:6px; }
.sp-btn-ghost { padding:8px 16px; background:none; border:1px solid #dce1e8; border-radius:8px; font-size:13px; color:#5a6474; cursor:pointer; transition:background 0.15s; }
.sp-btn-ghost:hover { background:#f4f6f9; }
.sp-ic-s { background:none; border:none; padding:3px; color:#b8c0cc; cursor:pointer; border-radius:4px; display:flex; align-items:center; transition:all 0.12s; }
.sp-ic-s:hover { color:#1a6dff; }
.sp-ic-s.danger:hover { color:#d93025; }

/* ========== Form fields ========== */
.sp-fld label { display:block; font-size:12px; font-weight:500; color:#5a6474; margin-bottom:5px; }
.sp-fld input, .sp-fld select, .sp-fld textarea { width:100%; border:1px solid #dce1e8; border-radius:8px; padding:8px 12px; font-size:13px; outline:none; transition:border-color 0.2s, box-shadow 0.2s; color:#1e2a3a; background:#fff; }
.sp-fld input::placeholder, .sp-fld textarea::placeholder { color:#b8c0cc; }
.sp-fld input:focus, .sp-fld select:focus, .sp-fld textarea:focus { border-color:#7aadff; box-shadow:0 0 0 3px rgba(26,109,255,0.08); }
.sp-fld .help { font-size:11px; color:#8c95a3; margin-top:4px; }

/* ========== Form layout ========== */
.sp-form-row { display:flex; gap:12px; }

/* ========== Empty state ========== */
.sp-empty { text-align:center; padding:32px 0; color:#b8c0cc; font-size:13px; }

/* ========== Dimension list & cards ========== */
.sp-dim-list { display:flex; flex-direction:column; gap:8px; }

.sp-dim-card { border:1px solid #e8ecf0; border-radius:10px; border-left:3px solid transparent; transition:border-color 0.15s; }
.sp-dim-card:hover { border-left-color:#1a6dff; }
.sp-dim-card.is-expanded { border-left-color:#1a6dff; }

.sp-dim-row { display:flex; align-items:center; justify-content:space-between; padding:12px; cursor:pointer; }
.sp-dim-left { display:flex; align-items:center; gap:8px; flex:1; min-width:0; }
.sp-dim-right { display:flex; align-items:center; gap:16px; }

.sp-dim-chevron { font-size:11px; color:#8c95a3; transition:transform 0.2s; display:inline-block; }
.sp-dim-chevron.rotated { transform:rotate(90deg); }

.sp-dim-code { font-size:12px; font-family:monospace; padding:2px 8px; border-radius:4px; color:#5a6474; background:#f8f9fb; }
.sp-dim-name { font-size:13px; font-weight:500; color:#1e2a3a; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.sp-dim-module-tag { font-size:10px; font-weight:500; color:#7c3aed; background:#f5f3ff; padding:1px 6px; border-radius:4px; }
.sp-dim-item-count { font-size:11px; color:#8c95a3; background:#f4f6f9; padding:1px 6px; border-radius:8px; }

.sp-dim-stat { text-align:right; }
.sp-dim-stat-label { font-size:11px; color:#8c95a3; }
.sp-dim-stat-value { font-size:13px; font-weight:600; color:#374151; }
.sp-weight-value { color:#1a6dff; }
.sp-pass-value { color:#d97706; }

.sp-dim-actions { display:flex; align-items:center; gap:4px; opacity:0; transition:opacity 0.15s; }
.group:hover .sp-dim-actions { opacity:1; }

/* ========== Expanded items ========== */
.sp-dim-items { padding:0 12px 12px; border-top:1px solid #f0f2f5; }
.sp-dim-items-loading { font-size:12px; color:#8c95a3; padding:12px 0; text-align:center; }
.sp-dim-items-empty { font-size:12px; color:#b8c0cc; padding:12px 0; text-align:center; }

.sp-item-table { width:100%; border-collapse:collapse; font-size:12px; margin-top:8px; }
.sp-item-table th { text-align:left; font-size:11px; font-weight:500; color:#8c95a3; padding:6px 8px; background:#f8f9fb; border-bottom:1px solid #e8ecf0; }
.sp-item-table td { padding:6px 8px; border-bottom:1px solid #f0f2f5; }
.sp-item-name { font-size:12px; color:#1e2a3a; }
.sp-item-badge { font-size:11px; padding:1px 6px; border-radius:4px; }
.sp-item-badge.scored { background:#eff6ff; color:#2563eb; }
.sp-item-badge.capture { background:#ecfdf5; color:#059669; }

.sp-item-weight-input { width:72px; border:1px solid #dce1e8; border-radius:6px; padding:4px 8px; font-size:12px; outline:none; text-align:center; }
.sp-item-weight-input:focus { border-color:#7aadff; box-shadow:0 0 0 2px rgba(26,109,255,0.08); }

.sp-item-pct { font-size:12px; color:#5a6474; font-weight:500; }

/* ========== In-dimension add row ========== */
.sp-dim-add-row { padding:8px 0 0; }
.sp-assign-select { border:1px dashed #dce1e8; border-radius:6px; padding:4px 10px; font-size:12px; color:#8c95a3; outline:none; cursor:pointer; background:#fff; transition:border-color 0.15s; }
.sp-assign-select:hover { border-color:#7aadff; color:#5a6474; }
.sp-assign-select:focus { border-color:#7aadff; }

/* ========== Unassigned hint ========== */
.sp-unassigned-hint { font-size:12px; color:#8c95a3; padding:8px 12px; border:1px dashed #e8ecf0; border-radius:8px; }
.sp-unassigned-count { background:#fef3c7; color:#d97706; padding:1px 6px; border-radius:8px; font-size:11px; font-weight:500; }

/* ========== Weight progress bar ========== */
.sp-weight-row { display:flex; align-items:center; gap:8px; }
.sp-weight-bar-bg { flex:1; height:4px; background:#f0f1f3; border-radius:4px; overflow:hidden; }
.sp-weight-bar-fill { height:100%; border-radius:4px; transition:all 0.3s; }
.sp-weight-bar-fill.is-ok { background:#34d399; }
.sp-weight-bar-fill.is-over { background:#ef4444; }
.sp-weight-bar-fill.is-under { background:#eab308; }
.sp-weight-total { font-size:12px; font-weight:500; flex-shrink:0; }
.sp-weight-total.is-ok { color:#059669; }
.sp-weight-total.is-bad { color:#ef4444; }
.sp-redistribute-btn { padding:3px 8px; font-size:11px; color:#1a6dff; background:#f0f4ff; border:1px solid #dce5ff; border-radius:5px; cursor:pointer; transition:all 0.15s; white-space:nowrap; }
.sp-redistribute-btn:hover { background:#1a6dff; color:#fff; border-color:#1a6dff; }
</style>
