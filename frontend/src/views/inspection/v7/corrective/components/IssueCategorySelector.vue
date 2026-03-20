<script setup lang="ts">
/**
 * IssueCategorySelector - Issue category tree selector
 *
 * Hierarchical category selection using el-tree-select.
 * Displays category code + name for each node.
 */
import { computed } from 'vue'
import type { IssueCategory } from '@/types/insp/platform'

const props = defineProps<{
  modelValue: number
  categories: IssueCategory[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number]
}>()

interface TreeNode {
  value: number
  label: string
  disabled: boolean
  children?: TreeNode[]
}

/** Recursively build tree data from flat/nested category list */
function buildTree(items: IssueCategory[]): TreeNode[] {
  return items.map((item) => {
    const node: TreeNode = {
      value: item.id,
      label: item.categoryCode ? `[${item.categoryCode}] ${item.categoryName}` : item.categoryName,
      disabled: !item.isEnabled,
    }
    if (item.children && item.children.length > 0) {
      node.children = buildTree(item.children)
    }
    return node
  })
}

const treeData = computed(() => buildTree(props.categories || []))

function handleChange(val: number) {
  emit('update:modelValue', val)
}

/** Find the selected category by ID for display */
function findCategory(items: IssueCategory[], id: number): IssueCategory | null {
  for (const item of items) {
    if (item.id === id) return item
    if (item.children) {
      const found = findCategory(item.children, id)
      if (found) return found
    }
  }
  return null
}

const selectedCategory = computed(() => {
  if (!props.modelValue) return null
  return findCategory(props.categories || [], props.modelValue)
})
</script>

<template>
  <div class="issue-category-selector">
    <el-tree-select
      :model-value="modelValue"
      :data="treeData"
      placeholder="请选择问题分类"
      check-strictly
      filterable
      clearable
      :render-after-expand="false"
      :props="{ label: 'label', value: 'value', children: 'children', disabled: 'disabled' }"
      style="width: 100%"
      @update:model-value="handleChange"
    />

    <!-- Selected category info -->
    <div v-if="selectedCategory" class="selected-info text-xs text-gray-400 mt-1">
      <span v-if="selectedCategory.categoryCode">
        编码: {{ selectedCategory.categoryCode }}
      </span>
      <span v-if="!selectedCategory.isEnabled" class="text-red-400 ml-2">
        (已禁用)
      </span>
    </div>
  </div>
</template>

<style scoped>
.issue-category-selector {
  width: 100%;
}

.selected-info {
  line-height: 1.4;
}
</style>
