<template>
  <div class="dept-node">
    <!-- 当前节点 -->
    <div
      class="dept-row"
      :class="{ disabled: !dept.isEnabled }"
      :style="{ paddingLeft: `${level * 24 + 16}px` }"
    >
      <!-- 展开/收起按钮 -->
      <span
        class="expand-btn"
        :class="{ expanded: isExpanded, hidden: !hasChildren }"
        @click="$emit('toggle', dept.id)"
      >
        <ChevronRight class="expand-icon" />
      </span>

      <!-- 图标 -->
      <span class="dept-icon" :class="iconClass">
        <School v-if="level === 0" />
        <Building2 v-else />
      </span>

      <!-- 名称 -->
      <span class="dept-name">{{ dept.unitName }}</span>

      <!-- 编码 -->
      <code class="dept-code">{{ dept.unitCode }}</code>

      <!-- 禁用标签 -->
      <span v-if="!dept.isEnabled" class="disabled-tag">已禁用</span>

      <!-- 操作按钮 -->
      <div class="actions">
        <button class="action-btn" title="添加子部门" @click.stop="$emit('add-child', dept)">
          <Plus />
        </button>
        <button class="action-btn" title="编辑" @click.stop="$emit('edit', dept)">
          <Pencil />
        </button>
        <button
          class="action-btn"
          :class="dept.isEnabled ? 'btn-warning' : 'btn-success'"
          :title="dept.isEnabled ? '禁用' : '启用'"
          @click.stop="$emit('toggle-status', dept)"
        >
          <Ban v-if="dept.isEnabled" />
          <Check v-else />
        </button>
        <button class="action-btn btn-danger" title="删除" @click.stop="$emit('delete', dept)">
          <Trash2 />
        </button>
      </div>
    </div>

    <!-- 子节点 -->
    <template v-if="hasChildren && isExpanded">
      <DeptTreeNode
        v-for="child in dept.children"
        :key="child.id"
        :dept="child"
        :level="level + 1"
        :expanded-keys="expandedKeys"
        @toggle="(id) => $emit('toggle', id)"
        @edit="(d) => $emit('edit', d)"
        @add-child="(d) => $emit('add-child', d)"
        @delete="(d) => $emit('delete', d)"
        @toggle-status="(d) => $emit('toggle-status', d)"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { School, Building2, ChevronRight, Plus, Pencil, Trash2, Ban, Check } from 'lucide-vue-next'
import type { DepartmentResponse } from '@/api/organization'

interface Props {
  dept: DepartmentResponse
  level: number
  expandedKeys: Set<number>
}

const props = defineProps<Props>()

defineEmits<{
  toggle: [id: number]
  edit: [dept: DepartmentResponse]
  'add-child': [dept: DepartmentResponse]
  delete: [dept: DepartmentResponse]
  'toggle-status': [dept: DepartmentResponse]
}>()

const hasChildren = computed(() => props.dept.children && props.dept.children.length > 0)
const isExpanded = computed(() => props.expandedKeys.has(props.dept.id))

const iconClass = computed(() => {
  if (props.level === 0) return 'icon-primary'
  return props.level === 1 ? 'icon-blue' : 'icon-gray'
})
</script>

<style scoped>
.dept-node {
  border-bottom: 1px solid #f0f0f0;
}

.dept-node:last-child {
  border-bottom: none;
}

.dept-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  transition: background-color 0.15s;
}

.dept-row:hover {
  background: #fafafa;
}

.dept-row.disabled {
  opacity: 0.5;
}

/* 展开按钮 */
.expand-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: #bbb;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.expand-btn:hover {
  color: #666;
}

.expand-btn.expanded .expand-icon {
  transform: rotate(90deg);
}

.expand-btn.hidden {
  visibility: hidden;
}

.expand-icon {
  width: 16px;
  height: 16px;
  transition: transform 0.2s;
}

/* 部门图标 */
.dept-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  flex-shrink: 0;
}

.dept-icon svg {
  width: 18px;
  height: 18px;
}

.icon-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.icon-blue {
  background: #e8f4fc;
  color: #3b82f6;
}

.icon-gray {
  background: #f3f4f6;
  color: #6b7280;
}

/* 部门名称 */
.dept-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
}

/* 部门编码 */
.dept-code {
  font-size: 12px;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
  font-family: ui-monospace, monospace;
}

/* 禁用标签 */
.disabled-tag {
  font-size: 11px;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 10px;
}

/* 操作按钮 */
.actions {
  display: flex;
  gap: 4px;
  margin-left: auto;
  opacity: 0;
  transition: opacity 0.15s;
}

.dept-row:hover .actions {
  opacity: 1;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  background: transparent;
  border-radius: 6px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
}

.action-btn svg {
  width: 16px;
  height: 16px;
}

.action-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.action-btn.btn-warning:hover {
  background: #fef3c7;
  color: #d97706;
}

.action-btn.btn-success:hover {
  background: #d1fae5;
  color: #059669;
}

.action-btn.btn-danger:hover {
  background: #fee2e2;
  color: #dc2626;
}
</style>
