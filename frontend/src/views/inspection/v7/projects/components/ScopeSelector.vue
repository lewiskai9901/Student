<script setup lang="ts">
/**
 * ScopeSelector - Inspection scope selector (ORG/PLACE tree)
 *
 * Provides a tree-select interface for choosing org units or places
 * as the inspection scope.
 */
import { ref, computed, watch, onMounted } from 'vue'
import { ScopeTypeConfig, type ScopeType } from '@/types/insp/enums'
import { previewTargetCount } from '@/api/insp/project'

interface ScopeValue {
  scopeType: string
  scopeIds: number[]
}

interface TreeNode {
  id: number
  label: string
  children?: TreeNode[]
}

const props = withDefaults(defineProps<{
  modelValue: ScopeValue
  scopeType: 'ORG' | 'PLACE'
  targetType?: string
}>(), {
  scopeType: 'ORG',
})

const emit = defineEmits<{
  'update:modelValue': [value: ScopeValue]
}>()

// ---------- State ----------

const treeData = ref<TreeNode[]>([])
const loading = ref(false)
const filterText = ref('')
const treeRef = ref<any>(null)

const selectedIds = computed({
  get: () => props.modelValue.scopeIds ?? [],
  set: (ids: number[]) => {
    emit('update:modelValue', {
      scopeType: props.modelValue.scopeType,
      scopeIds: ids,
    })
  },
})

const selectedCount = computed(() => selectedIds.value.length)
const targetPreviewCount = ref<number | null>(null)
const previewLoading = ref(false)

const scopeLabel = computed(() => {
  return props.scopeType === 'ORG' ? '组织' : '场所'
})

// ---------- Tree Loading ----------

async function loadTreeData() {
  loading.value = true
  try {
    // Simulate loading tree data based on scope type
    // In production, this would call orgUnitApi.getTree() or placeApi.getTree()
    if (props.scopeType === 'ORG') {
      const { orgUnitApi } = await import('@/api/organization')
      const res = await orgUnitApi.getTree()
      treeData.value = mapOrgTree(res.data ?? res)
    } else {
      // Place tree loading
      treeData.value = []
    }
  } catch {
    treeData.value = []
  } finally {
    loading.value = false
  }
}

function mapOrgTree(nodes: any[]): TreeNode[] {
  if (!Array.isArray(nodes)) return []
  return nodes.map(n => ({
    id: n.id,
    label: n.name || n.unitName || n.label || `ID:${n.id}`,
    children: n.children ? mapOrgTree(n.children) : undefined,
  }))
}

// ---------- Tree Filter ----------

function filterNode(value: string, data: TreeNode): boolean {
  if (!value) return true
  return data.label.includes(value)
}

watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

// ---------- Check Change ----------

function handleCheckChange() {
  const checked = treeRef.value?.getCheckedKeys(false) ?? []
  selectedIds.value = checked
}

// ---------- Target Preview ----------

async function loadTargetPreview() {
  if (!props.targetType || selectedIds.value.length === 0) {
    targetPreviewCount.value = null
    return
  }
  previewLoading.value = true
  try {
    targetPreviewCount.value = await previewTargetCount({
      scopeType: props.scopeType,
      scopeConfig: JSON.stringify(selectedIds.value),
      targetType: props.targetType,
    })
  } catch {
    targetPreviewCount.value = null
  } finally {
    previewLoading.value = false
  }
}

watch(selectedIds, () => {
  loadTargetPreview()
}, { deep: true })

// ---------- Actions ----------

function clearSelection() {
  treeRef.value?.setCheckedKeys([])
  selectedIds.value = []
}

// ---------- Lifecycle ----------

onMounted(() => {
  loadTreeData()
})

watch(() => props.scopeType, () => {
  loadTreeData()
})
</script>

<template>
  <div class="scope-selector">
    <div class="flex items-center justify-between mb-2">
      <span class="text-sm font-medium text-gray-700">
        {{ scopeLabel }}范围选择
      </span>
      <div class="flex items-center gap-2">
        <el-tag v-if="selectedCount > 0" size="small" type="primary">
          已选 {{ selectedCount }} 项
        </el-tag>
        <el-button
          v-if="selectedCount > 0"
          link
          type="primary"
          size="small"
          @click="clearSelection"
        >
          清空
        </el-button>
      </div>
    </div>

    <el-input
      v-model="filterText"
      placeholder="搜索..."
      size="small"
      clearable
      class="mb-2"
    />

    <div
      v-loading="loading"
      class="border border-gray-200 rounded-md overflow-auto"
      style="max-height: 320px; min-height: 160px"
    >
      <el-tree
        ref="treeRef"
        :data="treeData"
        :props="{ children: 'children', label: 'label' }"
        show-checkbox
        node-key="id"
        :default-checked-keys="selectedIds"
        :filter-node-method="filterNode"
        @check="handleCheckChange"
      />
      <div
        v-if="!loading && treeData.length === 0"
        class="py-8 text-center text-sm text-gray-400"
      >
        暂无{{ scopeLabel }}数据
      </div>
    </div>

    <!-- 目标预览 -->
    <div v-if="targetType && selectedCount > 0" class="mt-2 text-sm text-gray-600">
      <span v-if="previewLoading" class="text-gray-400">计算目标数量...</span>
      <span v-else-if="targetPreviewCount !== null">
        预计检查目标: <strong>{{ targetPreviewCount }}</strong> 个
      </span>
    </div>
  </div>
</template>
