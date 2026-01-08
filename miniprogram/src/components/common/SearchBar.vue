<template>
  <view class="search-bar">
    <view class="search-input-wrapper">
      <u-icon name="search" color="#9CA3AF" size="18" />
      <input
        class="search-input"
        type="text"
        :placeholder="placeholder"
        :value="modelValue"
        @input="onInput"
        @confirm="onSearch"
        confirm-type="search"
      />
      <u-icon
        v-if="modelValue"
        name="close-circle-fill"
        color="#9CA3AF"
        size="18"
        @click="onClear"
      />
    </view>
    <view v-if="showSearchBtn" class="search-btn" @click="onSearch">
      搜索
    </view>
  </view>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '请输入搜索关键词'
  },
  showSearchBtn: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'search', 'clear'])

const onInput = (e: any) => {
  emit('update:modelValue', e.detail.value)
}

const onSearch = () => {
  emit('search', props.modelValue)
}

const onClear = () => {
  emit('update:modelValue', '')
  emit('clear')
}
</script>

<style lang="scss" scoped>
.search-bar {
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  background: #fff;

  .search-input-wrapper {
    flex: 1;
    display: flex;
    align-items: center;
    padding: 16rpx 24rpx;
    background: #F3F4F6;
    border-radius: 40rpx;
    gap: 16rpx;

    .search-input {
      flex: 1;
      font-size: 28rpx;
      color: #1F2937;
    }
  }

  .search-btn {
    margin-left: 20rpx;
    padding: 16rpx 32rpx;
    font-size: 28rpx;
    color: #fff;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 40rpx;
  }
}
</style>
