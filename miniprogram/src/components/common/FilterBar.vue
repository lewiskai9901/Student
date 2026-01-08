<template>
  <view class="filter-bar">
    <scroll-view scroll-x class="filter-scroll">
      <view class="filter-items">
        <view
          v-for="(item, index) in filters"
          :key="index"
          class="filter-item"
          :class="{ active: item.value !== undefined && item.value !== '' && item.value !== null }"
          @click="onFilterClick(item)"
        >
          <text class="filter-label">{{ item.label }}</text>
          <text v-if="item.value && item.displayValue" class="filter-value">{{ item.displayValue }}</text>
          <u-icon name="arrow-down" size="12" :color="item.value ? '#667eea' : '#9CA3AF'" />
        </view>
      </view>
    </scroll-view>
    <view v-if="hasActiveFilter" class="filter-clear" @click="onClearAll">
      <u-icon name="close" size="14" color="#667eea" />
      <text>清除</text>
    </view>
  </view>

  <!-- 筛选弹出层 -->
  <u-popup v-model:show="showPopup" mode="bottom" round="20" :closeable="true">
    <view class="filter-popup">
      <view class="popup-title">{{ currentFilter?.label }}</view>
      <view class="popup-content">
        <view
          v-for="option in currentFilter?.options || []"
          :key="option.value"
          class="option-item"
          :class="{ active: option.value === currentFilter?.value }"
          @click="onOptionSelect(option)"
        >
          <text>{{ option.label }}</text>
          <u-icon v-if="option.value === currentFilter?.value" name="checkmark" color="#667eea" size="18" />
        </view>
      </view>
    </view>
  </u-popup>
</template>

<script setup lang="ts">
import { ref, computed, defineProps, defineEmits } from 'vue'

export interface FilterOption {
  label: string
  value: any
}

export interface FilterItem {
  key: string
  label: string
  value?: any
  displayValue?: string
  options: FilterOption[]
}

const props = defineProps<{
  filters: FilterItem[]
}>()

const emit = defineEmits(['change', 'clear'])

const showPopup = ref(false)
const currentFilter = ref<FilterItem | null>(null)

const hasActiveFilter = computed(() => {
  return props.filters.some(f => f.value !== undefined && f.value !== '' && f.value !== null)
})

const onFilterClick = (filter: FilterItem) => {
  currentFilter.value = filter
  showPopup.value = true
}

const onOptionSelect = (option: FilterOption) => {
  if (currentFilter.value) {
    const filter = currentFilter.value
    emit('change', {
      key: filter.key,
      value: option.value,
      label: option.label
    })
  }
  showPopup.value = false
}

const onClearAll = () => {
  emit('clear')
}
</script>

<style lang="scss" scoped>
.filter-bar {
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #F3F4F6;

  .filter-scroll {
    flex: 1;
    white-space: nowrap;
  }

  .filter-items {
    display: inline-flex;
    gap: 16rpx;
  }

  .filter-item {
    display: inline-flex;
    align-items: center;
    padding: 12rpx 20rpx;
    background: #F3F4F6;
    border-radius: 32rpx;
    gap: 8rpx;

    &.active {
      background: rgba(102, 126, 234, 0.1);
      border: 1rpx solid #667eea;
    }

    .filter-label {
      font-size: 24rpx;
      color: #4B5563;
    }

    .filter-value {
      font-size: 24rpx;
      color: #667eea;
      max-width: 120rpx;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .filter-clear {
    display: flex;
    align-items: center;
    margin-left: 16rpx;
    padding: 12rpx 16rpx;
    font-size: 24rpx;
    color: #667eea;
    gap: 4rpx;
  }
}

.filter-popup {
  padding: 32rpx;
  max-height: 60vh;

  .popup-title {
    font-size: 32rpx;
    font-weight: 600;
    color: #1F2937;
    text-align: center;
    margin-bottom: 32rpx;
  }

  .popup-content {
    max-height: 50vh;
    overflow-y: auto;
  }

  .option-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 24rpx 16rpx;
    font-size: 28rpx;
    color: #4B5563;
    border-bottom: 1rpx solid #F3F4F6;

    &:last-child {
      border-bottom: none;
    }

    &.active {
      color: #667eea;
      font-weight: 500;
    }
  }
}
</style>
