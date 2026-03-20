<script setup lang="ts">
import { computed } from 'vue'
import type { TemplateSection, TemplateItem } from '@/types/insp/template'
import { TargetTypeConfig, ItemTypeConfig, ScoringModeConfig, type TargetType, type ItemType, type ScoringMode } from '@/types/insp/enums'

const props = defineProps<{
  sections: TemplateSection[]
  itemsBySection: Map<number, TemplateItem[]>
  selectedId: string | null  // "section:123" or "item:456"
  readonly?: boolean
  rootSectionId?: number
  rootSection?: TemplateSection | null
}>()

const emit = defineEmits<{
  selectSection: [sectionId: number]
  selectItem: [item: TemplateItem]
  addChild: [parentSectionId: number]
  addRef: [parentSectionId: number]
  addItem: [sectionId: number]
  deleteSection: [sectionId: number]
  deleteItem: [item: TemplateItem]
}>()

// Build tree
interface TreeNode {
  type: 'section' | 'item'
  section?: TemplateSection
  item?: TemplateItem
  children: TreeNode[]
  depth: number
  key: string
}

const tree = computed<TreeNode[]>(() => {
  const childrenMap = new Map<string, TemplateSection[]>()
  for (const s of props.sections) {
    const key = String(s.parentSectionId ?? 'null')
    if (!childrenMap.has(key)) childrenMap.set(key, [])
    childrenMap.get(key)!.push(s)
  }
  for (const list of childrenMap.values()) {
    list.sort((a, b) => a.sortOrder - b.sortOrder)
  }

  function buildNodes(parentId: number | null, depth: number): TreeNode[] {
    const key = String(parentId ?? 'null')
    const childSections = childrenMap.get(key) || []
    const nodes: TreeNode[] = []

    for (const s of childSections) {
      const sectionChildren = buildNodes(s.id, depth + 1)
      // If no child sections, show items as leaves
      const items = sectionChildren.length === 0
        ? (props.itemsBySection.get(s.id) || []).map(item => ({
            type: 'item' as const,
            item,
            children: [],
            depth: depth + 1,
            key: `item:${item.id}`,
          }))
        : []

      nodes.push({
        type: 'section',
        section: s,
        children: [...sectionChildren, ...items],
        depth,
        key: `section:${s.id}`,
      })
    }
    return nodes
  }

  const startId = props.rootSectionId ?? null
  return buildNodes(startId, 0)
})

// Flatten
function flattenTree(nodes: TreeNode[]): TreeNode[] {
  const result: TreeNode[] = []
  for (const node of nodes) {
    result.push({ ...node, depth: node.depth + 1 })
    result.push(...flattenTree(node.children).map(n => ({ ...n })))
  }
  return result
}

const flatNodes = computed(() => flattenTree(tree.value))

function isFirstLevel(section: TemplateSection): boolean {
  if (props.rootSectionId) return Number(section.parentSectionId) === Number(props.rootSectionId)
  return section.parentSectionId === null
}

function hasChildren(sectionId: number): boolean {
  return props.sections.some(s => Number(s.parentSectionId) === Number(sectionId))
}

function getItemLabel(item: TemplateItem): string {
  if (item.isScored && item.scoringConfig) {
    try {
      const mode = JSON.parse(item.scoringConfig).mode as ScoringMode
      return ScoringModeConfig[mode]?.label || ItemTypeConfig[item.itemType as ItemType]?.label || item.itemType
    } catch {}
  }
  return ItemTypeConfig[item.itemType as ItemType]?.label || item.itemType
}
</script>

<template>
  <div class="st-panel">
    <div class="st-list">
      <!-- Root node -->
      <div v-if="rootSection"
        class="st-node st-root-node"
        :class="{ selected: selectedId === `section:${rootSection.id}` }"
        @click="emit('selectSection', Number(rootSection.id))"
      >
        <div class="st-node-content">
          <span class="st-root-icon">◆</span>
          <span class="st-node-name st-root-name">{{ rootSection.sectionName || '未命名' }}</span>
          <span v-if="rootSection.targetType" class="st-tag st-tag-target">
            {{ TargetTypeConfig[rootSection.targetType as TargetType]?.label }}
          </span>
        </div>
        <div v-if="!readonly" class="st-node-actions">
          <button title="添加子分区" @click.stop="emit('addChild', Number(rootSection.id))">+</button>
        </div>
      </div>

      <!-- Child nodes (sections + items) -->
      <template v-for="node in flatNodes" :key="node.key">
        <!-- Section node -->
        <div v-if="node.type === 'section' && node.section"
          class="st-node"
          :class="{
            selected: selectedId === node.key,
            ref: !!node.section.refSectionId,
          }"
          :style="{ paddingLeft: `${4 + node.depth * 14}px` }"
          @click="emit('selectSection', Number(node.section!.id))"
        >
          <div class="st-node-content">
            <span class="st-indent-marker" />
            <span class="st-node-name">{{ node.section.sectionName || '未命名' }}</span>
            <span v-if="node.section.weight && node.section.weight !== 100" class="st-tag st-tag-weight">{{ node.section.weight }}</span>
            <span v-if="isFirstLevel(node.section) && node.section.targetType" class="st-tag st-tag-target">
              {{ TargetTypeConfig[node.section.targetType as TargetType]?.label }}
            </span>
            <span v-if="node.section.refSectionId" class="st-tag st-tag-ref">引用</span>
          </div>
          <div v-if="!readonly" class="st-node-actions">
            <button v-if="!node.section.refSectionId && !hasChildren(Number(node.section.id))"
              title="添加字段" class="item-btn"
              @click.stop="emit('addItem', Number(node.section!.id))">⊕</button>
            <button v-if="!node.section.refSectionId"
              title="添加子分区"
              @click.stop="emit('addChild', Number(node.section!.id))">+</button>
            <button title="删除" class="danger"
              @click.stop="emit('deleteSection', Number(node.section!.id))">&times;</button>
          </div>
        </div>

        <!-- Item (field) node -->
        <div v-else-if="node.type === 'item' && node.item"
          class="st-node st-item-node"
          :class="{ selected: selectedId === node.key }"
          :style="{ paddingLeft: `${4 + node.depth * 14}px` }"
          @click="emit('selectItem', node.item!)"
        >
          <div class="st-node-content">
            <span class="st-item-dot" :class="node.item.isScored ? 'scored' : 'capture'" />
            <span class="st-node-name st-item-name">{{ node.item.itemName || node.item.itemCode }}</span>
            <span class="st-tag" :class="node.item.isScored ? 'st-tag-scored' : 'st-tag-type'">{{ getItemLabel(node.item) }}</span>
          </div>
          <div v-if="!readonly" class="st-node-actions">
            <button title="删除" class="danger"
              @click.stop="emit('deleteItem', node.item!)">&times;</button>
          </div>
        </div>
      </template>

      <div v-if="flatNodes.length === 0 && rootSection" class="st-empty">
        点击根节点 + 添加分区
      </div>
    </div>
  </div>
</template>

<style scoped>
.st-panel { height: 100%; display: flex; flex-direction: column; }
.st-list { display: flex; flex-direction: column; gap: 1px; overflow-y: auto; flex: 1; padding: 4px; }
.st-empty { padding: 24px 0; text-align: center; font-size: 12px; color: #8c95a3; }

.st-node {
  display: flex; align-items: center; justify-content: space-between;
  padding: 5px 6px; border-radius: 5px; cursor: pointer;
  transition: all 0.1s; border: 1px solid transparent; min-height: 28px;
}
.st-node:hover { background: #f0f4ff; border-color: #dbeafe; }
.st-node.selected { background: #eff6ff; border-color: #93c5fd; }
.st-node.ref { border-left: 2px solid #7c3aed; }

/* Root */
.st-root-node { padding: 7px 8px; border-bottom: 1px solid #e8ecf0; border-radius: 6px 6px 0 0; margin-bottom: 2px; }
.st-root-node .st-node-name { font-weight: 600; }
.st-root-icon { font-size: 10px; color: #1a6dff; flex-shrink: 0; }

/* Item node */
.st-item-node { opacity: 0.85; }
.st-item-node:hover { opacity: 1; }
.st-item-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
.st-item-dot.scored { background: #2563eb; }
.st-item-dot.capture { background: #94a3b8; }
.st-item-name { font-size: 11px; color: #4b5563; }

/* Content */
.st-node-content { display: flex; align-items: center; gap: 4px; overflow: hidden; min-width: 0; flex: 1; }
.st-indent-marker { width: 5px; height: 5px; border-left: 1px solid #d1d5db; border-bottom: 1px solid #d1d5db; flex-shrink: 0; }
.st-node-name { font-size: 12px; color: #1e2a3a; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* Tags */
.st-tag { font-size: 9px; padding: 0px 4px; border-radius: 3px; font-weight: 500; flex-shrink: 0; line-height: 16px; }
.st-tag-weight { background: #f0f2f5; color: #5a6474; }
.st-tag-target { background: #e8f0ff; color: #1a6dff; }
.st-tag-ref { background: #f3e8ff; color: #7c3aed; }
.st-tag-type { background: #f8f9fb; color: #8c95a3; font-size: 9px; }
.st-tag-scored { background: #eff6ff; color: #2563eb; font-size: 9px; }

/* Actions */
.st-node-actions { display: flex; gap: 1px; opacity: 0; transition: opacity 0.1s; flex-shrink: 0; }
.st-node:hover .st-node-actions { opacity: 1; }
.st-node-actions button {
  background: none; border: none; padding: 1px 3px;
  font-size: 11px; color: #8c95a3; cursor: pointer; border-radius: 3px;
}
.st-node-actions button:hover { color: #1a6dff; background: #e8f0fe; }
.st-node-actions button.item-btn { font-size: 13px; }
.st-node-actions button.item-btn:hover { color: #059669; background: #ecfdf5; }
.st-node-actions button.danger:hover { color: #ef4444; background: #fef2f2; }
</style>
