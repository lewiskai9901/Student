/**
 * V62 检查平台 - 模板编辑器 Composable
 *
 * 管理 section/item CRUD、自动保存、undo/redo
 * "模板" = 根 TemplateSection（parentSectionId=null）
 */
import { ref, computed, watch, type Ref } from 'vue'
import type { TemplateSection, TemplateItem } from '@/types/insp/template'
import type { ItemType, ScoringMode } from '@/types/insp/enums'
import {
  getSections,
  createSection,
  updateSection,
  deleteSection,
  reorderSections,
  updateSectionScoringConfig,
  getItems,
  createItem,
  updateItem,
  deleteItem,
  reorderItems,
} from '@/api/insp/template'

interface EditAction {
  type: 'addSection' | 'removeSection' | 'updateSection' | 'addItem' | 'removeItem' | 'updateItem' | 'reorderSections' | 'reorderItems'
  data: any
  undo: () => Promise<void>
}

export function useTemplateEditor(rootSectionId: Ref<number>) {
  const sections = ref<TemplateSection[]>([])
  const itemsBySection = ref<Map<string, TemplateItem[]>>(new Map())
  const isDirty = ref(false)
  const isSaving = ref(false)
  const isLoading = ref(false)
  const undoStack = ref<EditAction[]>([])
  const redoStack = ref<EditAction[]>([])

  const canUndo = computed(() => undoStack.value.length > 0)
  const canRedo = computed(() => redoStack.value.length > 0)

  const sectionTree = computed(() => {
    const map = new Map<number | null, TemplateSection[]>()
    for (const s of sections.value) {
      const key = s.parentSectionId ?? null
      if (!map.has(key)) map.set(key, [])
      map.get(key)!.push(s)
    }
    for (const list of map.values()) {
      list.sort((a, b) => a.sortOrder - b.sortOrder)
    }
    return map
  })

  // ==================== 数据加载 ====================

  async function loadSections() {
    if (!rootSectionId.value) return
    isLoading.value = true
    try {
      sections.value = await getSections(rootSectionId.value)
      // Load items for each section
      const itemMap = new Map<string, TemplateItem[]>()
      await Promise.all(
        sections.value.map(async (section) => {
          try {
            const items = await getItems(section.id)
            itemMap.set(String(section.id), items)
          } catch (e) {
            console.warn(`Failed to load items for section ${section.id}`, e)
            itemMap.set(String(section.id), [])
          }
        })
      )
      itemsBySection.value = itemMap
    } catch (e) {
      console.error('Failed to load sections', e)
    } finally {
      isLoading.value = false
    }
  }

  // Watch rootSectionId changes
  watch(rootSectionId, (newId) => {
    if (newId) {
      loadSections()
    } else {
      sections.value = []
      itemsBySection.value = new Map()
    }
  }, { immediate: true })

  // ==================== Section CRUD ====================

  async function addSection(parentSectionId?: number | null, sectionName?: string): Promise<TemplateSection> {
    const code = `S${Date.now().toString(36).toUpperCase()}`
    const section = await createSection({
      rootSectionId: rootSectionId.value,
      sectionCode: code,
      sectionName: sectionName || '',
      parentSectionId: parentSectionId ?? null,
      sortOrder: sections.value.filter(s =>
        parentSectionId ? s.parentSectionId === parentSectionId : !s.parentSectionId
      ).length,
    })
    sections.value.push(section)
    itemsBySection.value.set(String(section.id), [])
    isDirty.value = true
    return section
  }

  async function updateScoringConfigFn(sectionId: number, config: string): Promise<void> {
    await updateSectionScoringConfig(sectionId, config)
    const idx = sections.value.findIndex(s => s.id === sectionId)
    if (idx >= 0) sections.value[idx].scoringConfig = config
  }

  async function editSection(sectionId: number, data: Partial<TemplateSection>) {
    const section = await updateSection(sectionId, {
      sectionName: data.sectionName,
      targetType: data.targetType ?? undefined,
      targetSourceMode: data.targetSourceMode ?? undefined,
      targetTypeFilter: data.targetTypeFilter ?? undefined,
      weight: data.weight,
      isRepeatable: data.isRepeatable,
      inputMode: data.inputMode,
    })
    const idx = sections.value.findIndex(s => s.id === sectionId)
    if (idx >= 0) sections.value[idx] = section
    isDirty.value = true
  }

  async function removeSection(sectionId: number) {
    await deleteSection(sectionId)
    sections.value = sections.value.filter(s => s.id !== sectionId)
    itemsBySection.value.delete(String(sectionId))
    isDirty.value = true
  }

  async function sortSections(sectionIds: number[]) {
    await reorderSections(sectionIds)
    // Reorder local state
    const ordered: TemplateSection[] = []
    for (const id of sectionIds) {
      const s = sections.value.find(sec => sec.id === id)
      if (s) ordered.push(s)
    }
    sections.value = ordered
    isDirty.value = true
  }

  // ==================== Item CRUD ====================

  async function addItem(sectionId: number, itemType: ItemType | null, isScored: boolean, scoringMode?: ScoringMode, itemName?: string) {
    const code = `I${Date.now().toString(36).toUpperCase()}`
    const currentItems = itemsBySection.value.get(String(sectionId)) || []

    // Build scoring config for scored items
    let scoringConfig: string | undefined
    if (isScored && scoringMode) {
      scoringConfig = JSON.stringify({ mode: scoringMode })
    }

    const item = await createItem(sectionId, {
      itemCode: code,
      itemName: itemName || '',
      itemType: itemType ?? 'TEXT',
      isScored,
      scoringConfig,
      itemWeight: isScored ? 100 : 1,
      sortOrder: currentItems.length + 1,
    })
    currentItems.push(item)
    itemsBySection.value.set(String(sectionId), [...currentItems])
    isDirty.value = true
    return item
  }

  async function editItem(itemId: number, data: Partial<TemplateItem>) {
    // Preserve existing dimensionId if not explicitly provided
    let currentDimensionId: number | null | undefined = undefined
    if (data.dimensionId === undefined) {
      for (const [, items] of itemsBySection.value) {
        const found = items.find(i => i.id === itemId)
        if (found) {
          currentDimensionId = found.dimensionId
          break
        }
      }
    }

    const item = await updateItem(itemId, {
      itemName: data.itemName,
      description: data.description ?? undefined,
      itemType: data.itemType,
      config: data.config ?? undefined,
      validationRules: data.validationRules ?? undefined,
      responseSetId: data.responseSetId,
      scoringConfig: data.scoringConfig ?? undefined,
      dimensionId: data.dimensionId !== undefined ? data.dimensionId : currentDimensionId,
      helpContent: data.helpContent ?? undefined,
      isRequired: data.isRequired,
      isScored: data.isScored,
      requireEvidence: data.requireEvidence,
      conditionLogic: data.conditionLogic ?? undefined,
    })
    // Update in itemsBySection
    for (const [secId, items] of itemsBySection.value) {
      const idx = items.findIndex(i => i.id === itemId)
      if (idx >= 0) {
        items[idx] = item
        itemsBySection.value.set(secId, [...items])
        break
      }
    }
    isDirty.value = true
  }

  async function removeItem(itemId: number) {
    await deleteItem(itemId)
    for (const [secId, items] of itemsBySection.value) {
      const filtered = items.filter(i => i.id !== itemId)
      if (filtered.length !== items.length) {
        itemsBySection.value.set(secId, filtered)
        break
      }
    }
    isDirty.value = true
  }

  async function sortItems(sectionId: number, itemIds: number[]) {
    await reorderItems(sectionId, itemIds)
    const items = itemsBySection.value.get(String(sectionId)) || []
    const ordered: TemplateItem[] = []
    for (const id of itemIds) {
      const it = items.find(i => i.id === id)
      if (it) ordered.push(it)
    }
    itemsBySection.value.set(String(sectionId), ordered)
    isDirty.value = true
  }

  // ==================== Undo/Redo (simplified) ====================

  function undo() {
    const action = undoStack.value.pop()
    if (action) {
      action.undo()
      redoStack.value.push(action)
    }
  }

  function redo() {
    // Redo would re-apply the action. Simplified for now.
    redoStack.value.pop()
  }

  return {
    // State
    sections,
    itemsBySection,
    isDirty,
    isSaving,
    isLoading,
    canUndo,
    canRedo,
    sectionTree,
    // Section CRUD
    loadSections,
    addSection,
    editSection,
    removeSection,
    sortSections,
    updateScoringConfig: updateScoringConfigFn,
    // Item CRUD
    addItem,
    editItem,
    removeItem,
    sortItems,
    // Undo/Redo
    undo,
    redo,
  }
}
