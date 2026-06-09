<template>
  <div class="rounded-lg border border-gray-200 bg-gray-50/50 p-3">
    <div class="mb-2 flex items-center justify-between">
      <span class="text-xs text-gray-600">选择可访问的数据范围</span>
      <div class="flex items-center gap-1">
        <button class="action-btn" @click="expandAll">全部展开</button>
        <button class="action-btn" @click="collapseAll">全部折叠</button>
        <button class="action-btn" @click="clearAll">清空</button>
      </div>
    </div>

    <div
      class="max-h-60 overflow-y-auto rounded-md border border-gray-200 bg-white"
      :class="{ 'opacity-60': loading }"
    >
      <div v-if="loading" class="flex items-center justify-center py-6">
        <Loader2 class="h-5 w-5 animate-spin text-gray-400" />
      </div>
      <div v-else-if="treeData.length === 0" class="py-6 text-center text-xs text-gray-400">
        暂无数据
      </div>
      <div v-else class="p-2">
        <OrgTreeNode
          v-for="node in treeData"
          :key="String(node.id)"
          :node="node"
          :selected="selectedSet"
          :expanded="expanded"
          @toggle-select="toggleSelect"
          @toggle-expand="toggleExpand"
        />
      </div>
    </div>

    <!-- 统计 (通用, 不区分组织类型) -->
    <div class="mt-2 flex items-center justify-between rounded bg-blue-50 px-2 py-1.5">
      <span class="flex items-center gap-1 text-[11px]">
        <Building2 class="h-3 w-3 text-blue-500" />
        <span class="text-gray-600">已选</span>
        <span class="font-semibold text-blue-600">{{ orgIds.length }}</span>
        <span class="text-gray-600">个组织单元</span>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import type { OrgUnitTreeNode } from '@/types/organization'
import { ref, computed, onMounted } from 'vue'
import { Loader2, Building2 } from 'lucide-vue-next'
import { getOrgUnitTree } from '@/api/organization'
import OrgTreeNode from './OrgTreeNode.vue'

/** 树节点 (来自 /org-units/tree, 类名标签由后端 typeName 数据字典驱动) */
export interface ScopeTreeNode {
  id: LongId | string
  name: string
  /** 类型显示名 (来自 org_unit_types 数据字典 typeName); 无则回退原始 type code */
  typeLabel: string
  children: ScopeTreeNode[]
}

interface Props {
  orgIds: (number | string)[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:orgIds': [value: (number | string)[]]
}>()

const loading = ref(false)
const treeData = ref<ScopeTreeNode[]>([])
const expanded = ref<Set<string>>(new Set())

const selectedSet = computed(() => new Set(props.orgIds.map(x => String(x))))

function toggleSelect(id: LongId | string) {
  const arr = [...props.orgIds]
  const idx = arr.findIndex(x => String(x) === String(id))
  if (idx > -1) arr.splice(idx, 1)
  else arr.push(id)
  emit('update:orgIds', arr)
}

function toggleExpand(id: LongId | string) {
  const key = String(id)
  if (expanded.value.has(key)) expanded.value.delete(key)
  else expanded.value.add(key)
}

function expandAll() {
  const walk = (nodes: ScopeTreeNode[]) => {
    for (const n of nodes) {
      if (n.children.length) {
        expanded.value.add(String(n.id))
        walk(n.children)
      }
    }
  }
  walk(treeData.value)
}

function collapseAll() {
  expanded.value.clear()
}

function clearAll() {
  emit('update:orgIds', [])
}

function mapNode(n: OrgUnitTreeNode): ScopeTreeNode {
  return {
    id: n.id,
    name: n.unitName || n.name || n.label || '',
    // 标签文本来自后端类型字典 (typeName), 核心不硬编码任何类型名
    typeLabel: n.typeName || n.unitType || '',
    children: (n.children || []).map(mapNode),
  }
}

async function buildTree() {
  loading.value = true
  try {
    const orgTree = await getOrgUnitTree()
    treeData.value = orgTree.map(mapNode)
    // 默认展开根节点
    treeData.value.forEach(n => {
      if (n.children.length) expanded.value.add(String(n.id))
    })
  } catch (e) {
    console.error('加载组织树失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  buildTree()
})
</script>

<style scoped>
.action-btn {
  padding: 2px 6px;
  font-size: 10px;
  color: #6b7280;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  cursor: pointer;
}
.action-btn:hover {
  background: #f9fafb;
}
</style>
