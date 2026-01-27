<template>
  <div class="org-tree-card">
    <!-- 部门卡片 -->
    <div
      class="group relative overflow-hidden rounded-xl border bg-white shadow-sm transition-all duration-200 hover:shadow-md"
      :class="[
        level === 0 ? 'border-indigo-200' : 'border-gray-200',
        isExpanded && hasChildren ? 'rounded-b-none' : ''
      ]"
    >
      <!-- 左侧彩色条 -->
      <div
        class="absolute left-0 top-0 h-full w-1"
        :class="levelColors[level % levelColors.length]"
      ></div>

      <div class="flex items-center justify-between p-4 pl-5">
        <div class="flex items-center gap-4">
          <!-- 展开/折叠按钮 -->
          <button
            v-if="hasChildren"
            @click="$emit('toggle', department.id)"
            class="flex h-8 w-8 items-center justify-center rounded-lg transition-colors hover:bg-gray-100"
          >
            <ChevronRight
              class="h-5 w-5 text-gray-400 transition-transform duration-200"
              :class="{ 'rotate-90': isExpanded }"
            />
          </button>
          <div v-else class="w-8"></div>

          <!-- 部门图标 -->
          <div
            class="flex h-12 w-12 items-center justify-center rounded-xl"
            :class="levelBgColors[level % levelBgColors.length]"
          >
            <component
              :is="level === 0 ? Building : Building2"
              class="h-6 w-6"
              :class="levelTextColors[level % levelTextColors.length]"
            />
          </div>

          <!-- 部门信息 -->
          <div>
            <div class="flex items-center gap-2">
              <span class="text-lg font-semibold text-gray-900">{{ department.unitName }}</span>
              <span class="rounded bg-gray-100 px-2 py-0.5 font-mono text-xs text-gray-500">
                {{ department.unitCode }}
              </span>
              <span
                v-if="!department.isEnabled"
                class="rounded bg-gray-200 px-2 py-0.5 text-xs text-gray-500"
              >
                已禁用
              </span>
            </div>
            <div class="mt-1 flex items-center gap-4 text-sm text-gray-500">
              <span v-if="department.leaderName" class="flex items-center gap-1">
                <User class="h-3.5 w-3.5" />
                {{ department.leaderName }}
              </span>
              <span v-if="department.phone" class="flex items-center gap-1">
                <Phone class="h-3.5 w-3.5" />
                {{ department.phone }}
              </span>
              <span v-if="department.email" class="flex items-center gap-1">
                <Mail class="h-3.5 w-3.5" />
                {{ department.email }}
              </span>
              <span v-if="hasChildren" class="flex items-center gap-1">
                <FolderTree class="h-3.5 w-3.5" />
                {{ department.children?.length }} 个子部门
              </span>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="flex items-center gap-2 opacity-0 transition-opacity group-hover:opacity-100">
          <!-- 状态开关 -->
          <button
            @click="$emit('status-change', department, !department.isEnabled)"
            class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors"
            :class="department.isEnabled ? 'bg-indigo-600' : 'bg-gray-300'"
          >
            <span
              class="inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform"
              :class="department.isEnabled ? 'translate-x-6' : 'translate-x-1'"
            />
          </button>

          <div class="mx-2 h-6 w-px bg-gray-200"></div>

          <button
            @click="$emit('add-child', department)"
            class="flex h-8 w-8 items-center justify-center rounded-lg text-gray-400 transition-colors hover:bg-indigo-50 hover:text-indigo-600"
            title="添加子部门"
          >
            <FolderPlus class="h-4 w-4" />
          </button>
          <button
            @click="$emit('edit', department)"
            class="flex h-8 w-8 items-center justify-center rounded-lg text-gray-400 transition-colors hover:bg-blue-50 hover:text-blue-600"
            title="编辑"
          >
            <Pencil class="h-4 w-4" />
          </button>
          <button
            @click="$emit('delete', department)"
            class="flex h-8 w-8 items-center justify-center rounded-lg text-gray-400 transition-colors hover:bg-red-50 hover:text-red-600"
            title="删除"
          >
            <Trash2 class="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>

    <!-- 子部门 - 带连接线 -->
    <Transition name="expand">
      <div v-if="hasChildren && isExpanded" class="relative ml-10 border-l-2 border-gray-200 pl-6">
        <div class="space-y-3 py-3">
          <OrgTreeCard
            v-for="(child, index) in department.children"
            :key="child.id"
            :department="child"
            :level="level + 1"
            :expanded-ids="expandedIds"
            @toggle="$emit('toggle', $event)"
            @edit="$emit('edit', $event)"
            @delete="$emit('delete', $event)"
            @add-child="$emit('add-child', $event)"
            @status-change="$emit('status-change', $event, $event)"
          >
            <!-- 连接线 -->
            <template #connector>
              <div
                class="absolute -left-6 top-1/2 h-0.5 w-6 bg-gray-200"
              ></div>
            </template>
          </OrgTreeCard>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Building,
  Building2,
  ChevronRight,
  User,
  Phone,
  Mail,
  FolderTree,
  FolderPlus,
  Pencil,
  Trash2
} from 'lucide-vue-next'
import type { DepartmentResponse } from '@/api/v2/organization'

interface Props {
  department: DepartmentResponse
  level: number
  expandedIds: Set<number>
}

const props = defineProps<Props>()

defineEmits<{
  toggle: [id: number]
  edit: [dept: DepartmentResponse]
  delete: [dept: DepartmentResponse]
  'add-child': [dept: DepartmentResponse]
  'status-change': [dept: DepartmentResponse, enabled: boolean]
}>()

// 层级颜色
const levelColors = [
  'bg-indigo-500',
  'bg-purple-500',
  'bg-cyan-500',
  'bg-amber-500',
  'bg-emerald-500',
]

const levelBgColors = [
  'bg-indigo-100',
  'bg-purple-100',
  'bg-cyan-100',
  'bg-amber-100',
  'bg-emerald-100',
]

const levelTextColors = [
  'text-indigo-600',
  'text-purple-600',
  'text-cyan-600',
  'text-amber-600',
  'text-emerald-600',
]

const hasChildren = computed(() => {
  return props.department.children && props.department.children.length > 0
})

const isExpanded = computed(() => {
  return props.expandedIds.has(props.department.id)
})
</script>

<style scoped>
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  max-height: 1000px;
}
</style>
