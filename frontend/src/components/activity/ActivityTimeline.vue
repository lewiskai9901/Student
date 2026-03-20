<template>
  <div>
    <div v-if="title" class="flex items-center justify-between px-6 py-3 border-b border-gray-50">
      <span class="text-sm font-medium text-gray-700">{{ title }}</span>
      <span class="text-xs text-gray-400">{{ events.length }} 条</span>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-10">
      <Loader2 class="h-5 w-5 animate-spin text-gray-400" />
      <span class="ml-2 text-sm text-gray-500">加载中...</span>
    </div>

    <div v-else-if="events.length > 0" class="divide-y divide-gray-50">
      <div
        v-for="evt in events"
        :key="evt.id"
        class="flex gap-4 px-6 py-3"
      >
        <!-- Action badge -->
        <div class="flex-shrink-0 pt-0.5">
          <span
            class="inline-flex rounded-full px-2 py-0.5 text-[10px] font-medium"
            :style="{
              backgroundColor: getActionColor(evt.action) + '18',
              color: getActionColor(evt.action)
            }"
          >
            {{ evt.actionLabel || getActionLabel(evt.action) }}
          </span>
        </div>

        <!-- Content -->
        <div class="min-w-0 flex-1">
          <!-- Changed fields -->
          <div v-if="evt.changedFields?.length" class="space-y-1">
            <div
              v-for="(fc, i) in evt.changedFields"
              :key="i"
              class="text-xs text-gray-600"
            >
              <span class="font-medium text-gray-700">{{ getFieldLabel(fc.fieldName) }}</span>:
              <span v-if="fc.oldValue" class="text-red-500 line-through">{{ fc.oldValue }}</span>
              <span v-if="fc.oldValue && fc.newValue"> → </span>
              <span v-if="fc.newValue" class="text-green-600">{{ fc.newValue }}</span>
            </div>
          </div>
          <div v-else-if="evt.actionLabel" class="text-xs text-gray-600">
            {{ evt.actionLabel }}
          </div>
          <div v-else class="text-xs text-gray-600">
            {{ getActionLabel(evt.action) }}{{ evt.resourceName ? ': ' + evt.resourceName : '' }}
          </div>

          <!-- Reason -->
          <div v-if="evt.reason" class="mt-1 text-xs text-gray-500">
            原因: {{ evt.reason }}
          </div>

          <!-- Error -->
          <div v-if="evt.result === 'FAILURE' && evt.errorMessage" class="mt-1 text-xs text-red-500">
            {{ evt.errorMessage }}
          </div>
        </div>

        <!-- Timestamp & user -->
        <div class="flex-shrink-0 text-right">
          <div class="text-xs text-gray-500">{{ evt.userName || '-' }}</div>
          <div class="text-[10px] text-gray-400">{{ formatTime(evt.occurredAt) }}</div>
        </div>
      </div>
    </div>

    <div v-else class="flex flex-col items-center py-10">
      <FolderOpen class="h-10 w-10 text-gray-300" />
      <p class="mt-2 text-sm text-gray-500">暂无操作记录</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { Loader2, FolderOpen } from 'lucide-vue-next'
import { getResourceTimeline } from '@/api/activityEvent'
import type { ActivityEvent } from '@/types/activityEvent'

const props = withDefaults(defineProps<{
  resourceType: string
  resourceId: string | number
  limit?: number
  title?: string
}>(), {
  limit: 30,
  title: '操作记录'
})

const loading = ref(false)
const events = ref<ActivityEvent[]>([])

const loadEvents = async () => {
  if (!props.resourceId) return
  loading.value = true
  try {
    events.value = await getResourceTimeline(props.resourceType, props.resourceId, props.limit)
  } catch (e) {
    console.error('加载活动事件失败', e)
    events.value = []
  } finally {
    loading.value = false
  }
}

// Reload when resourceId changes
watch(() => props.resourceId, () => {
  loadEvents()
})

onMounted(() => {
  loadEvents()
})

// Action color mapping
const ACTION_COLORS: Record<string, string> = {
  CREATE: '#67C23A',
  UPDATE: '#E6A23C',
  DELETE: '#F56C6C',
  FREEZE: '#909399',
  UNFREEZE: '#67C23A',
  DISSOLVE: '#F56C6C',
  MERGE: '#409EFF',
  SPLIT: '#409EFF',
  ASSIGN: '#409EFF',
  LOGIN: '#409EFF',
  LOGOUT: '#909399',
  CHECK_IN: '#67C23A',
  CHECK_OUT: '#F56C6C',
  TRANSFER: '#409EFF',
  APPOINT: '#409EFF'
}

const ACTION_LABELS: Record<string, string> = {
  CREATE: '创建',
  UPDATE: '更新',
  DELETE: '删除',
  FREEZE: '冻结',
  UNFREEZE: '解冻',
  DISSOLVE: '解散',
  MERGE: '合并',
  SPLIT: '拆分',
  ASSIGN: '分配',
  LOGIN: '登录',
  LOGOUT: '登出',
  IMPORT: '导入',
  EXPORT: '导出',
  CHECK_IN: '入住',
  CHECK_OUT: '退出',
  TRANSFER: '调岗',
  APPOINT: '任命'
}

const FIELD_LABELS: Record<string, string> = {
  // 组织
  unitName: '名称',
  sortOrder: '排序',
  headcount: '编制人数',
  attributes: '属性',
  status: '状态',
  addMember: '添加成员',
  removeMember: '移除成员',
  mergedIntoId: '合并至',
  splitInto: '拆分为',
  // 岗位
  positionName: '岗位名称',
  positionId: '岗位',
  jobLevel: '职级',
  reportsToId: '汇报对象',
  responsibilities: '职责',
  requirements: '任职要求',
  keyPosition: '关键岗位',
  // 用户
  userId: '用户',
  endDate: '结束日期',
  // 场所入住
  occupant: '入住人',
  username: '账号',
  positionNo: '位置号',
  orgUnitName: '所属组织',
  // 通用
  name: '名称',
  description: '描述',
  type: '类型',
  category: '分类',
  code: '编码',
  parentId: '上级',
  remark: '备注'
}

const getActionColor = (action: string) => ACTION_COLORS[action] || '#909399'
const getActionLabel = (action: string) => ACTION_LABELS[action] || action
const getFieldLabel = (field: string) => FIELD_LABELS[field] || field

const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

// Expose reload method for parent
defineExpose({ reload: loadEvents })
</script>
