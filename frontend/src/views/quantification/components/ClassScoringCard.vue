<template>
  <div
    :class="[
      'overflow-hidden rounded-xl border transition-all',
      hasDeduction
        ? 'border-red-300 bg-gradient-to-br from-red-50 to-orange-50 shadow-md'
        : 'border-gray-200 bg-white hover:border-gray-300 hover:shadow-sm'
    ]"
  >
    <!-- 班级头部 -->
    <div :class="[
      'flex items-center justify-between px-4 py-3 border-b',
      hasDeduction ? 'border-red-200 bg-red-100/50' : 'border-gray-100 bg-gray-50'
    ]">
      <div class="flex items-center gap-2">
        <div :class="[
          'flex h-8 w-8 items-center justify-center rounded-lg',
          hasDeduction ? 'bg-red-500' : 'bg-gray-400'
        ]">
          <School class="h-4 w-4 text-white" />
        </div>
        <div>
          <div class="text-sm font-semibold text-gray-900">{{ classInfo.className }}</div>
          <div class="text-xs text-gray-500">{{ classInfo.studentCount || 0 }}人</div>
        </div>
      </div>
      <!-- 扣分汇总 -->
      <div v-if="totalScore > 0" class="text-right">
        <div class="text-lg font-bold text-red-600">-{{ totalScore }}</div>
        <div class="text-[10px] text-gray-500">本班扣分</div>
      </div>
    </div>

    <!-- 打分区域 -->
    <div class="p-3">
      <!-- 无关联资源：直接打分 -->
      <template v-if="!hasLinkResources">
        <SimpleScoringPanel
          :item="item"
          :class-id="classInfo.classId"
          :category-id="category?.categoryId"
          :detail="getDetailForLink(0)"
          :check-round="checkRound"
          @toggle="handleToggle"
          @update="handleUpdate"
        />
      </template>

      <!-- 有关联资源：显示资源列表 -->
      <template v-else>
        <div class="space-y-2">
          <div
            v-for="link in linkResources"
            :key="link.id"
            :class="[
              'rounded-lg border p-2 transition-all',
              isLinkDeducted(link.id)
                ? 'border-red-200 bg-red-50'
                : 'border-gray-100 bg-gray-50 hover:border-gray-200'
            ]"
          >
            <div class="flex items-center justify-between mb-2">
              <div class="flex items-center gap-2">
                <Home v-if="link.type === 1" class="h-4 w-4 text-purple-500" />
                <Building v-else class="h-4 w-4 text-blue-500" />
                <span class="text-sm font-medium text-gray-700">{{ link.no }}</span>
              </div>
              <span v-if="getLinkScore(link.id) > 0" class="text-sm font-bold text-red-600">
                -{{ getLinkScore(link.id) }}
              </span>
            </div>
            <SimpleScoringPanel
              :item="item"
              :class-id="classInfo.classId"
              :category-id="category?.categoryId"
              :link-id="link.id"
              :link-type="link.type"
              :link-no="link.no"
              :detail="getDetailForLink(link.id)"
              :check-round="checkRound"
              :compact="true"
              @toggle="handleToggle"
              @update="handleUpdate"
            />
          </div>
        </div>
        <!-- 无资源提示 -->
        <div v-if="linkResources.length === 0" class="text-center py-4 text-sm text-gray-400">
          <Home class="h-8 w-8 mx-auto mb-2 text-gray-300" />
          暂无关联资源
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { School, Home, Building } from 'lucide-vue-next'
import type { DeductionItem } from '@/api/deductionItems'
import SimpleScoringPanel from './SimpleScoringPanel.vue'

interface ClassInfo {
  classId: number
  className: string
  studentCount?: number
}

interface LinkResource {
  id: number
  no: string
  type: number // 1=宿舍, 2=教室
}

interface Props {
  classInfo: ClassInfo
  item: DeductionItem
  category: any
  linkResources: LinkResource[]
  scoringDetails: any[]
  checkRound: number
}

const props = defineProps<Props>()
const emit = defineEmits<{
  toggle: [detail: any, isAdd: boolean]
  update: [detail: any]
}>()

// 是否有关联资源
const hasLinkResources = computed(() => {
  return props.category?.linkType && props.category.linkType > 0
})

// 是否有扣分
const hasDeduction = computed(() => {
  return props.scoringDetails.length > 0
})

// 总扣分
const totalScore = computed(() => {
  return props.scoringDetails.reduce((sum, d) => sum + (d.deductScore || 0), 0).toFixed(1)
})

// 获取指定关联资源的扣分详情
const getDetailForLink = (linkId: number) => {
  return props.scoringDetails.find(d => {
    const detailLinkId = d.dormitoryId || d.classroomId || d.linkId || 0
    return String(detailLinkId) === String(linkId)
  }) || null
}

// 关联资源是否已扣分
const isLinkDeducted = (linkId: number) => {
  return !!getDetailForLink(linkId)
}

// 获取关联资源的扣分
const getLinkScore = (linkId: number) => {
  const detail = getDetailForLink(linkId)
  return detail ? detail.deductScore : 0
}

// 处理扣分切换
const handleToggle = (detail: any, isAdd: boolean) => {
  emit('toggle', detail, isAdd)
}

// 处理扣分更新
const handleUpdate = (detail: any) => {
  emit('update', detail)
}
</script>
