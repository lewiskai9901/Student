<template>
  <view class="room-card" @click="onClick">
    <view class="card-header">
      <view class="room-info">
        <text class="room-no">{{ room.roomNo }}</text>
        <text class="building-name">{{ room.buildingName }}</text>
      </view>
      <StatusTag :type="statusType" :text="occupancyText" />
    </view>

    <view class="card-body">
      <view class="bed-grid">
        <view
          v-for="i in room.bedCapacity"
          :key="i"
          class="bed-item"
          :class="{ occupied: i <= (room.occupiedBeds || 0) }"
        >
          <u-icon
            :name="i <= (room.occupiedBeds || 0) ? 'account-fill' : 'account'"
            size="20"
            :color="i <= (room.occupiedBeds || 0) ? '#667eea' : '#D1D5DB'"
          />
        </view>
      </view>
      <view class="occupancy-info">
        <text class="occupied">{{ room.occupiedBeds || 0 }}</text>
        <text class="separator">/</text>
        <text class="total">{{ room.bedCapacity }}</text>
        <text class="label">床位</text>
      </view>
    </view>

    <view class="card-footer">
      <view class="info-item">
        <u-icon name="map" size="14" color="#9CA3AF" />
        <text>{{ room.floor }}楼</text>
      </view>
      <view v-if="room.departmentName" class="info-item">
        <u-icon name="account" size="14" color="#9CA3AF" />
        <text>{{ room.departmentName }}</text>
      </view>
      <view v-if="room.facilities" class="info-item">
        <u-icon name="setting" size="14" color="#9CA3AF" />
        <text>{{ room.facilities }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, defineProps, defineEmits } from 'vue'
import type { Dormitory } from '@/api/dormitory'
import StatusTag from '@/components/common/StatusTag.vue'

const props = defineProps<{
  room: Dormitory
}>()

const emit = defineEmits(['click'])

const statusType = computed(() => {
  const occupied = props.room.occupiedBeds || 0
  const capacity = props.room.bedCapacity
  if (occupied === 0) return 'success'
  if (occupied < capacity) return 'warning'
  return 'error'
})

const occupancyText = computed(() => {
  const occupied = props.room.occupiedBeds || 0
  const capacity = props.room.bedCapacity
  if (occupied === 0) return '空闲'
  if (occupied < capacity) return '部分入住'
  return '满员'
})

const onClick = () => {
  emit('click', props.room)
}
</script>

<style lang="scss" scoped>
.room-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20rpx;

    .room-info {
      display: flex;
      align-items: baseline;
      gap: 12rpx;

      .room-no {
        font-size: 32rpx;
        font-weight: 600;
        color: #1F2937;
      }

      .building-name {
        font-size: 24rpx;
        color: #6B7280;
      }
    }
  }

  .card-body {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20rpx;
    background: #F9FAFB;
    border-radius: 12rpx;
    margin-bottom: 16rpx;

    .bed-grid {
      display: flex;
      gap: 16rpx;

      .bed-item {
        width: 48rpx;
        height: 48rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #fff;
        border-radius: 8rpx;
        border: 1rpx solid #E5E7EB;

        &.occupied {
          background: rgba(102, 126, 234, 0.1);
          border-color: #667eea;
        }
      }
    }

    .occupancy-info {
      display: flex;
      align-items: baseline;
      gap: 4rpx;

      .occupied {
        font-size: 36rpx;
        font-weight: 600;
        color: #667eea;
      }

      .separator {
        font-size: 24rpx;
        color: #9CA3AF;
      }

      .total {
        font-size: 28rpx;
        color: #4B5563;
      }

      .label {
        font-size: 22rpx;
        color: #9CA3AF;
        margin-left: 8rpx;
      }
    }
  }

  .card-footer {
    display: flex;
    align-items: center;
    gap: 24rpx;

    .info-item {
      display: flex;
      align-items: center;
      gap: 6rpx;

      text {
        font-size: 22rpx;
        color: #6B7280;
      }
    }
  }
}
</style>
