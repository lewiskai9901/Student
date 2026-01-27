<template>
  <div class="space-y-6">
    <!-- 按系部分组显示 -->
    <div v-for="dept in assigneesByDepartment" :key="dept.departmentId" class="rounded-lg border border-gray-200 bg-white">
      <!-- 系部标题栏 -->
      <div class="flex items-center justify-between border-b border-gray-200 bg-gray-50 px-4 py-3">
        <div class="flex items-center gap-3">
          <span class="font-medium text-gray-900">{{ dept.departmentName || '未分配系部' }}</span>
          <span class="text-sm text-gray-500">
            {{ dept.completedCount }}/{{ dept.totalCount }} 已完成
          </span>
        </div>
        <!-- 进度条 -->
        <div class="flex items-center gap-2">
          <div class="h-2 w-24 overflow-hidden rounded-full bg-gray-200">
            <div
              class="h-full rounded-full bg-green-500 transition-all duration-300"
              :style="{ width: `${(dept.completedCount / dept.totalCount) * 100}%` }"
            ></div>
          </div>
          <span class="text-xs text-gray-500">
            {{ Math.round((dept.completedCount / dept.totalCount) * 100) }}%
          </span>
        </div>
      </div>

      <!-- 执行人卡片网格 -->
      <div class="grid grid-cols-4 gap-3 p-4 sm:grid-cols-6 md:grid-cols-8 lg:grid-cols-10">
        <div
          v-for="assignee in dept.assignees"
          :key="assignee.assigneeId"
          class="group relative"
          @mouseenter="showTooltip(assignee)"
          @mouseleave="hideTooltip"
        >
          <!-- 卡片 -->
          <div
            :class="[
              'flex h-12 w-full cursor-pointer items-center justify-center rounded-lg border-2 text-sm font-medium transition-all duration-200',
              getCardClass(assignee.status)
            ]"
            @click="$emit('cardClick', assignee)"
          >
            <span class="truncate px-1">{{ getShortName(assignee.assigneeName) }}</span>
          </div>

          <!-- Tooltip -->
          <div
            v-if="activeTooltip === assignee.assigneeId"
            class="absolute bottom-full left-1/2 z-50 mb-2 w-48 -translate-x-1/2 transform rounded-lg bg-gray-900 p-3 text-xs text-white shadow-lg"
          >
            <div class="mb-2 font-medium">{{ assignee.assigneeName }}</div>
            <div class="space-y-1 text-gray-300">
              <div class="flex justify-between">
                <span>状态:</span>
                <span :class="getStatusTextClass(assignee.status)">{{ assignee.statusText }}</span>
              </div>
              <div v-if="assignee.submittedAt" class="flex justify-between">
                <span>提交时间:</span>
                <span>{{ formatDate(assignee.submittedAt) }}</span>
              </div>
              <div v-if="assignee.completedAt" class="flex justify-between">
                <span>完成时间:</span>
                <span>{{ formatDate(assignee.completedAt) }}</span>
              </div>
              <div v-if="assignee.currentApprovalLevel > 0" class="flex justify-between">
                <span>审批进度:</span>
                <span>第{{ assignee.currentApprovalLevel }}级</span>
              </div>
            </div>
            <!-- 三角箭头 -->
            <div class="absolute -bottom-1 left-1/2 h-2 w-2 -translate-x-1/2 rotate-45 transform bg-gray-900"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 状态图例 -->
    <div class="flex flex-wrap items-center gap-4 rounded-lg border border-gray-200 bg-gray-50 px-4 py-3">
      <span class="text-sm font-medium text-gray-700">状态图例:</span>
      <div class="flex items-center gap-2">
        <div class="h-4 w-4 rounded border-2 border-orange-400 bg-orange-50"></div>
        <span class="text-xs text-gray-600">待接收</span>
      </div>
      <div class="flex items-center gap-2">
        <div class="h-4 w-4 rounded border-2 border-blue-400 bg-blue-50"></div>
        <span class="text-xs text-gray-600">进行中</span>
      </div>
      <div class="flex items-center gap-2">
        <div class="h-4 w-4 rounded border-2 border-amber-400 bg-amber-50"></div>
        <span class="text-xs text-gray-600">待审核</span>
      </div>
      <div class="flex items-center gap-2">
        <div class="h-4 w-4 rounded border-2 border-green-500 bg-green-100"></div>
        <span class="text-xs text-gray-600">已完成</span>
      </div>
      <div class="flex items-center gap-2">
        <div class="h-4 w-4 rounded border-2 border-red-400 bg-red-50"></div>
        <span class="text-xs text-gray-600">已打回</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { DepartmentAssigneesDTO, TaskAssigneeDetailDTO } from '@/api/task'

defineProps<{
  assigneesByDepartment: DepartmentAssigneesDTO[]
}>()

defineEmits<{
  cardClick: [assignee: TaskAssigneeDetailDTO]
}>()

const activeTooltip = ref<string | null>(null)

const showTooltip = (assignee: TaskAssigneeDetailDTO) => {
  activeTooltip.value = assignee.assigneeId
}

const hideTooltip = () => {
  activeTooltip.value = null
}

// 获取简短名称（最多显示2个字）
const getShortName = (name: string) => {
  if (!name) return '?'
  return name.length > 2 ? name.substring(0, 2) : name
}

// 根据状态获取卡片样式
const getCardClass = (status: number) => {
  const classes: Record<number, string> = {
    0: 'border-orange-400 bg-orange-50 text-orange-700 hover:bg-orange-100',      // 待接收
    1: 'border-blue-400 bg-blue-50 text-blue-700 hover:bg-blue-100',              // 进行中
    2: 'border-amber-400 bg-amber-50 text-amber-700 hover:bg-amber-100',          // 待审核
    3: 'border-green-500 bg-green-100 text-green-700 hover:bg-green-200',         // 已完成
    4: 'border-red-400 bg-red-50 text-red-700 hover:bg-red-100',                  // 已打回
    5: 'border-gray-300 bg-gray-100 text-gray-500',                               // 已取消
    6: 'border-purple-400 bg-purple-50 text-purple-700 hover:bg-purple-100'       // 审批中
  }
  return classes[status] || 'border-gray-300 bg-gray-100 text-gray-500'
}

// 获取状态文字颜色
const getStatusTextClass = (status: number) => {
  const classes: Record<number, string> = {
    0: 'text-orange-400',
    1: 'text-blue-400',
    2: 'text-amber-400',
    3: 'text-green-400',
    4: 'text-red-400',
    5: 'text-gray-400',
    6: 'text-purple-400'
  }
  return classes[status] || 'text-gray-400'
}

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return dateStr.substring(0, 16).replace('T', ' ')
}
</script>
