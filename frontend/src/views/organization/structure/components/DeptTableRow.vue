<template>
  <!-- 当前行 -->
  <tr class="hover:bg-gray-50">
    <!-- 部门名称 -->
    <td class="px-4 py-3">
      <div class="flex items-center gap-2" :style="{ paddingLeft: `${level * 20}px` }">
        <!-- 展开按钮 -->
        <button
          :class="[
            'flex h-5 w-5 items-center justify-center rounded transition-transform',
            hasChildren ? 'text-gray-400 hover:text-gray-600' : 'invisible'
          ]"
          @click="$emit('toggle', dept.id)"
        >
          <ChevronRight :class="['h-4 w-4 transition-transform', isExpanded ? 'rotate-90' : '']" />
        </button>
        <!-- 图标 -->
        <div
          :class="[
            'flex h-8 w-8 items-center justify-center rounded-lg',
            level === 0 ? 'bg-blue-100 text-blue-600' : 'bg-gray-100 text-gray-600'
          ]"
        >
          <Building2 class="h-4 w-4" />
        </div>
        <!-- 名称 -->
        <span class="text-sm font-medium text-gray-900">{{ dept.unitName }}</span>
      </div>
    </td>
    <!-- 部门编码 -->
    <td class="px-4 py-3">
      <span class="rounded bg-gray-100 px-2 py-0.5 text-xs font-mono text-gray-600">{{ dept.unitCode }}</span>
    </td>
    <!-- 类别 -->
    <td class="px-4 py-3">
      <span
        :class="[
          'inline-flex items-center rounded px-2 py-0.5 text-xs font-medium',
          categoryStyle
        ]"
      >
        {{ categoryLabel }}
      </span>
    </td>
    <!-- 负责人 -->
    <td class="px-4 py-3 text-sm text-gray-600">{{ dept.leaderName || '-' }}</td>
    <!-- 状态 -->
    <td class="px-4 py-3 text-center">
      <span
        :class="[
          'inline-flex items-center rounded px-2 py-0.5 text-xs font-medium',
          dept.isEnabled ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'
        ]"
      >
        {{ dept.isEnabled ? '启用' : '禁用' }}
      </span>
    </td>
    <!-- 操作 -->
    <td class="px-4 py-3 text-right">
      <div class="flex items-center justify-end gap-1">
        <button
          class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-blue-600"
          title="添加子部门"
          @click="$emit('add-child', dept)"
        >
          <Plus class="h-4 w-4" />
        </button>
        <button
          class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-blue-600"
          title="编辑"
          @click="$emit('edit', dept)"
        >
          <Pencil class="h-4 w-4" />
        </button>
        <button
          :class="[
            'rounded p-1.5 text-gray-400',
            dept.isEnabled ? 'hover:bg-orange-50 hover:text-orange-600' : 'hover:bg-green-50 hover:text-green-600'
          ]"
          :title="dept.isEnabled ? '禁用' : '启用'"
          @click="$emit('toggle-status', dept)"
        >
          <Ban v-if="dept.isEnabled" class="h-4 w-4" />
          <Check v-else class="h-4 w-4" />
        </button>
        <button
          class="rounded p-1.5 text-gray-400 hover:bg-red-50 hover:text-red-600"
          title="删除"
          @click="$emit('delete', dept)"
        >
          <Trash2 class="h-4 w-4" />
        </button>
      </div>
    </td>
  </tr>
  <!-- 子节点 -->
  <template v-if="hasChildren && isExpanded">
    <DeptTableRow
      v-for="child in dept.children"
      :key="child.id"
      :dept="child"
      :level="level + 1"
      :expanded-keys="expandedKeys"
      @toggle="(id) => $emit('toggle', id)"
      @add-child="(d) => $emit('add-child', d)"
      @edit="(d) => $emit('edit', d)"
      @toggle-status="(d) => $emit('toggle-status', d)"
      @delete="(d) => $emit('delete', d)"
    />
  </template>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Building2, ChevronRight, Plus, Pencil, Trash2, Ban, Check } from 'lucide-vue-next'
import type { DepartmentResponse } from '@/api/organization'

interface Props {
  dept: DepartmentResponse
  level: number
  expandedKeys: Set<number>
}

const props = defineProps<Props>()

defineEmits<{
  toggle: [id: number]
  'add-child': [dept: DepartmentResponse]
  edit: [dept: DepartmentResponse]
  'toggle-status': [dept: DepartmentResponse]
  delete: [dept: DepartmentResponse]
}>()

const hasChildren = computed(() => props.dept.children && props.dept.children.length > 0)
const isExpanded = computed(() => props.expandedKeys.has(props.dept.id))

// 类别映射
const CATEGORY_MAP: Record<string, { label: string; style: string }> = {
  ACADEMIC: { label: '教学单位', style: 'bg-blue-50 text-blue-700' },
  FUNCTIONAL: { label: '职能部门', style: 'bg-purple-50 text-purple-700' },
  ADMINISTRATIVE: { label: '行政单位', style: 'bg-amber-50 text-amber-700' },
}
const categoryLabel = computed(() => CATEGORY_MAP[props.dept.unitCategory || 'ACADEMIC']?.label || '教学单位')
const categoryStyle = computed(() => CATEGORY_MAP[props.dept.unitCategory || 'ACADEMIC']?.style || 'bg-gray-50 text-gray-700')
</script>
