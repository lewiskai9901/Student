<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Type, AlignLeft, FileText, Hash, SlidersHorizontal,
  ChevronDown, ListChecks, CheckSquare, Circle,
  Calendar, Clock, CalendarClock,
  Camera, Video, PenTool, Upload,
  MapPin, ScanLine,
} from 'lucide-vue-next'
import {
  ItemTypeConfig, ItemTypeGroups,
  ScoringModeConfig,
  type ItemType, type ScoringMode,
} from '@/types/insp/enums'

const emit = defineEmits<{
  select: [type: ItemType | null, isScored: boolean, scoringMode?: ScoringMode]
}>()

type ItemCategory = 'scored' | 'capture'
const category = ref<ItemCategory>('scored')

const iconMap: Record<string, any> = {
  Type, AlignLeft, FileText, Hash, SlidersHorizontal,
  ChevronDown, ListChecks, CheckSquare, Circle,
  Calendar, Clock, CalendarClock,
  Camera, Video, PenTool, Upload,
  MapPin, ScanLine,
}

// 评分模式分组
const scoringModeGroups = computed(() => [
  {
    label: '基础',
    modes: (['PASS_FAIL', 'DEDUCTION', 'ADDITION', 'DIRECT'] as ScoringMode[]).map(k => ({
      key: k, ...ScoringModeConfig[k],
    })),
  },
  {
    label: '选择类',
    modes: (['LEVEL', 'SCORE_TABLE', 'TIERED_DEDUCTION', 'RATING_SCALE'] as ScoringMode[]).map(k => ({
      key: k, ...ScoringModeConfig[k],
    })),
  },
  {
    label: '高级',
    modes: (['CUMULATIVE', 'WEIGHTED_MULTI', 'RISK_MATRIX', 'THRESHOLD', 'FORMULA'] as ScoringMode[]).map(k => ({
      key: k, ...ScoringModeConfig[k],
    })),
  },
])

// 特殊评分项类型（既有评分又有特殊采集方式）
const specialScoredTypes = [
  { type: 'VIOLATION_RECORD' as ItemType, label: '违纪记录', description: '逐条记录事件，按次数扣分', scoringMode: 'DEDUCTION' as ScoringMode },
  { type: 'PERSON_SCORE' as ItemType, label: '逐人评分', description: '对目标中每个人逐一打分', scoringMode: 'DIRECT' as ScoringMode },
]

function selectSpecialType(item: typeof specialScoredTypes[0]) {
  emit('select', item.type, true, item.scoringMode)
}

// 采集类型分组
const captureGrouped = computed(() => {
  return ItemTypeGroups.map(group => ({
    group,
    items: (Object.entries(ItemTypeConfig) as [ItemType, typeof ItemTypeConfig[ItemType]][])
      .filter(([, info]) => info.group === group)
      .map(([type, info]) => ({ type, ...info })),
  }))
})

function selectScoringMode(mode: ScoringMode) {
  // 评分项不需要 itemType，传 null 给上游处理
  emit('select', null, true, mode)
}

function selectCaptureType(type: ItemType) {
  emit('select', type, false)
}
</script>

<template>
  <div class="space-y-4">
    <!-- Category toggle -->
    <div class="flex rounded-lg bg-gray-100 p-1">
      <button
        class="flex-1 rounded-md px-4 py-2 text-sm font-medium transition-all"
        :class="category === 'scored'
          ? 'bg-white text-blue-600 shadow-sm'
          : 'text-gray-500 hover:text-gray-700'"
        @click="category = 'scored'"
      >
        评分项
      </button>
      <button
        class="flex-1 rounded-md px-4 py-2 text-sm font-medium transition-all"
        :class="category === 'capture'
          ? 'bg-white text-green-600 shadow-sm'
          : 'text-gray-500 hover:text-gray-700'"
        @click="category = 'capture'"
      >
        采集项
      </button>
    </div>

    <!-- Scored: Scoring mode cards -->
    <template v-if="category === 'scored'">
      <p class="text-xs text-gray-400">检查员做判断，系统计分</p>
      <div v-for="g in scoringModeGroups" :key="g.label">
        <div class="mb-2 text-xs font-medium uppercase tracking-wider text-gray-400">{{ g.label }}</div>
        <div class="grid grid-cols-2 gap-1.5">
          <button
            v-for="m in g.modes"
            :key="m.key"
            class="flex flex-col gap-0.5 rounded-md border border-gray-200 px-3 py-2 text-left transition hover:border-blue-300 hover:bg-blue-50"
            @click="selectScoringMode(m.key)"
          >
            <span class="text-sm text-gray-700">{{ m.label }}</span>
            <span class="text-[10px] leading-tight text-gray-400">{{ m.description }}</span>
          </button>
        </div>
      </div>
      <!-- Special scored types -->
      <div class="mb-2 text-xs font-medium uppercase tracking-wider text-gray-400">特殊</div>
      <div class="grid grid-cols-2 gap-1.5">
        <button
          v-for="st in specialScoredTypes"
          :key="st.type"
          class="flex flex-col gap-0.5 rounded-md border border-orange-200 px-3 py-2 text-left transition hover:border-orange-400 hover:bg-orange-50"
          @click="selectSpecialType(st)"
        >
          <span class="text-sm text-gray-700">{{ st.label }}</span>
          <span class="text-[10px] leading-tight text-gray-400">{{ st.description }}</span>
        </button>
      </div>
    </template>

    <!-- Capture: Data capture type cards -->
    <template v-else>
      <p class="text-xs text-gray-400">检查员填信息，不计分</p>
      <div v-for="g in captureGrouped" :key="g.group">
        <div class="mb-2 text-xs font-medium uppercase tracking-wider text-gray-400">{{ g.group }}</div>
        <div class="grid grid-cols-3 gap-1.5">
          <button
            v-for="item in g.items"
            :key="item.type"
            class="flex items-center gap-2 rounded-md border border-gray-200 px-2.5 py-2 text-left text-sm transition hover:border-green-300 hover:bg-green-50"
            @click="selectCaptureType(item.type)"
          >
            <component :is="iconMap[item.icon]" :size="14" class="shrink-0 text-gray-500" />
            <span class="truncate text-gray-700">{{ item.label }}</span>
          </button>
        </div>
      </div>
    </template>
  </div>
</template>
