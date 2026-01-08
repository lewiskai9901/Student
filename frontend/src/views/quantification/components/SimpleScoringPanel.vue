<template>
  <div :class="compact ? 'space-y-1' : 'space-y-2'">
    <!-- 固定扣分模式 -->
    <template v-if="item.deductMode === 1">
      <button
        @click="handleToggleFixed"
        :class="[
          'flex w-full items-center justify-center gap-2 rounded-lg py-2 text-sm font-semibold transition-all',
          isDeducted
            ? 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            : 'bg-red-500 text-white hover:bg-red-600 shadow-sm'
        ]"
      >
        <component :is="isDeducted ? X : Minus" class="h-4 w-4" />
        {{ isDeducted ? '取消扣分' : `扣 ${item.fixedScore} 分` }}
      </button>
    </template>

    <!-- 按人次扣分模式 -->
    <template v-else-if="item.deductMode === 2">
      <div class="flex items-center gap-2">
        <div class="flex flex-1 items-center rounded-lg border border-gray-200 bg-white">
          <button
            @click="decrementCount"
            :disabled="localPersonCount <= 0"
            class="flex h-8 w-8 items-center justify-center text-gray-500 hover:text-gray-900 disabled:opacity-30"
          >
            <Minus class="h-4 w-4" />
          </button>
          <input
            v-model.number="localPersonCount"
            type="number"
            min="0"
            class="h-8 w-full flex-1 border-none bg-transparent text-center text-sm font-bold text-gray-900 focus:outline-none"
            @change="handleCountChange"
          />
          <button
            @click="incrementCount"
            class="flex h-8 w-8 items-center justify-center text-gray-500 hover:text-gray-900"
          >
            <Plus class="h-4 w-4" />
          </button>
        </div>
        <span class="text-xs text-gray-500">人</span>
        <span v-if="currentScore > 0" class="text-sm font-bold text-red-600">-{{ currentScore }}</span>
      </div>
    </template>

    <!-- 区间扣分模式 -->
    <template v-else-if="item.deductMode === 3">
      <div class="space-y-1">
        <input
          v-model.number="localRangeScore"
          type="range"
          :min="parseRangeMin"
          :max="parseRangeMax"
          :step="0.5"
          class="h-2 w-full cursor-pointer appearance-none rounded-full bg-gray-200"
          style="accent-color: #ef4444;"
          @change="handleRangeChange"
        />
        <div class="flex items-center justify-between">
          <span class="text-xs text-gray-400">{{ parseRangeMin }}</span>
          <span class="text-sm font-bold text-red-600">{{ localRangeScore > 0 ? `-${localRangeScore}` : '0' }}</span>
          <span class="text-xs text-gray-400">{{ parseRangeMax }}</span>
        </div>
      </div>
    </template>

    <!-- 附加操作（照片、备注） -->
    <div v-if="isDeducted && !compact" class="flex gap-1.5 pt-1">
      <button
        v-if="item.allowPhoto"
        @click="showPhotoDialog = true"
        :class="[
          'flex flex-1 items-center justify-center gap-1 rounded-lg py-1.5 text-xs font-medium transition-colors',
          photos.length > 0 ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
        ]"
      >
        <Camera class="h-3 w-3" />
        {{ photos.length > 0 ? `${photos.length}张` : '照片' }}
      </button>
      <button
        v-if="item.allowRemark"
        @click="showRemarkDialog = true"
        :class="[
          'flex flex-1 items-center justify-center gap-1 rounded-lg py-1.5 text-xs font-medium transition-colors',
          localRemark ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
        ]"
      >
        <MessageSquare class="h-3 w-3" />
        {{ localRemark ? '已备注' : '备注' }}
      </button>
    </div>

    <!-- 备注对话框 -->
    <Teleport to="body">
      <div v-if="showRemarkDialog" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="showRemarkDialog = false"></div>
        <div class="relative w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl">
          <h3 class="mb-4 text-lg font-semibold text-gray-900">添加备注</h3>
          <textarea
            v-model="tempRemark"
            rows="4"
            class="w-full rounded-lg border border-gray-200 p-3 text-sm focus:border-blue-500 focus:outline-none"
            placeholder="请输入备注内容..."
          ></textarea>
          <div class="mt-4 flex justify-end gap-3">
            <button
              @click="showRemarkDialog = false"
              class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              取消
            </button>
            <button
              @click="confirmRemark"
              class="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
              确定
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 照片对话框（简化版） -->
    <Teleport to="body">
      <div v-if="showPhotoDialog" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="showPhotoDialog = false"></div>
        <div class="relative w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl">
          <h3 class="mb-4 text-lg font-semibold text-gray-900">上传照片</h3>
          <div class="text-center py-8 text-gray-500">
            <Camera class="h-12 w-12 mx-auto mb-2 text-gray-300" />
            <p class="text-sm">照片上传功能</p>
            <p class="text-xs text-gray-400 mt-1">请在完整打分页面中上传照片</p>
          </div>
          <div class="mt-4 flex justify-end">
            <button
              @click="showPhotoDialog = false"
              class="rounded-lg bg-gray-100 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-200"
            >
              关闭
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Minus, Plus, X, Camera, MessageSquare } from 'lucide-vue-next'
import type { DeductionItem } from '@/api/deductionItems'

interface Props {
  item: DeductionItem
  classId: number
  categoryId?: number
  linkId?: number
  linkType?: number
  linkNo?: string
  detail: any
  checkRound: number
  compact?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  compact: false
})

const emit = defineEmits<{
  toggle: [detail: any, isAdd: boolean]
  update: [detail: any]
}>()

// 状态
const localPersonCount = ref(0)
const localRangeScore = ref(0)
const localRemark = ref('')
const photos = ref<string[]>([])
const showRemarkDialog = ref(false)
const showPhotoDialog = ref(false)
const tempRemark = ref('')

// 是否已扣分
const isDeducted = computed(() => !!props.detail)

// 当前扣分
const currentScore = computed(() => {
  if (props.item.deductMode === 1) {
    return isDeducted.value ? (props.item.fixedScore || 0) : 0
  } else if (props.item.deductMode === 2) {
    const baseScore = props.item.baseScore || 0
    const perPerson = props.item.scorePerPerson || props.item.perPersonScore || 0
    return baseScore + localPersonCount.value * perPerson
  } else if (props.item.deductMode === 3) {
    return localRangeScore.value
  }
  return 0
})

// 区间范围
const parseRangeMin = computed(() => {
  if (props.item.minScore !== undefined) return props.item.minScore
  return 0
})

const parseRangeMax = computed(() => {
  if (props.item.maxScore !== undefined) return props.item.maxScore
  return 10
})

// 同步已有数据
watch(() => props.detail, (data) => {
  if (data) {
    if (props.item.deductMode === 2) {
      localPersonCount.value = data.personCount || 0
    } else if (props.item.deductMode === 3) {
      localRangeScore.value = data.deductScore || 0
    }
    localRemark.value = data.remark || ''
    if (data.photoUrls) {
      try {
        photos.value = typeof data.photoUrls === 'string' ? JSON.parse(data.photoUrls) : data.photoUrls
      } catch (e) {
        photos.value = []
      }
    }
  } else {
    localPersonCount.value = 0
    localRangeScore.value = 0
    localRemark.value = ''
    photos.value = []
  }
}, { immediate: true, deep: true })

// 构建扣分详情
const buildDetail = () => {
  const baseScore = props.item.baseScore || 0
  const perPerson = props.item.scorePerPerson || props.item.perPersonScore || 0
  let deductScore = 0
  if (props.item.deductMode === 1) {
    deductScore = props.item.fixedScore || 0
  } else if (props.item.deductMode === 2) {
    deductScore = baseScore + localPersonCount.value * perPerson
  } else if (props.item.deductMode === 3) {
    deductScore = localRangeScore.value
  }

  return {
    categoryId: props.categoryId,
    deductionItemId: props.item.id,
    deductionItemName: props.item.itemName,
    deductMode: props.item.deductMode,
    classId: props.classId,
    deductScore,
    personCount: props.item.deductMode === 2 ? localPersonCount.value : undefined,
    linkType: props.linkType || 0,
    linkId: props.linkId || 0,
    dormitoryId: props.linkType === 1 ? props.linkId : undefined,
    dormitoryNo: props.linkType === 1 ? props.linkNo : undefined,
    classroomId: props.linkType === 2 ? props.linkId : undefined,
    classroomNo: props.linkType === 2 ? props.linkNo : undefined,
    photoUrls: photos.value.length > 0 ? JSON.stringify(photos.value) : null,
    remark: localRemark.value || null,
    checkRound: props.checkRound
  }
}

// 固定扣分切换
const handleToggleFixed = () => {
  emit('toggle', buildDetail(), !isDeducted.value)
}

// 人次变化
const incrementCount = () => {
  localPersonCount.value++
  handleCountChange()
}

const decrementCount = () => {
  if (localPersonCount.value > 0) {
    localPersonCount.value--
    handleCountChange()
  }
}

const handleCountChange = () => {
  if (localPersonCount.value <= 0) {
    localPersonCount.value = 0
    if (isDeducted.value) {
      emit('toggle', buildDetail(), false)
    }
  } else {
    emit('toggle', buildDetail(), true)
  }
}

// 区间变化
const handleRangeChange = () => {
  if (localRangeScore.value <= 0) {
    if (isDeducted.value) {
      emit('toggle', buildDetail(), false)
    }
  } else {
    emit('toggle', buildDetail(), true)
  }
}

// 备注确认
const confirmRemark = () => {
  localRemark.value = tempRemark.value
  showRemarkDialog.value = false
  if (isDeducted.value) {
    emit('update', buildDetail())
  }
}

// 打开备注对话框时同步内容
watch(showRemarkDialog, (visible) => {
  if (visible) {
    tempRemark.value = localRemark.value
  }
})
</script>

<style scoped>
input[type="number"]::-webkit-inner-spin-button,
input[type="number"]::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
input[type="number"] {
  -moz-appearance: textfield;
}

input[type="range"] {
  -webkit-appearance: none;
  height: 8px;
  border-radius: 4px;
  background: #e5e7eb;
}
input[type="range"]::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #ef4444;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.4);
}
</style>
