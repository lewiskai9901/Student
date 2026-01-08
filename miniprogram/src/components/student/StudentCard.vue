<template>
  <view class="student-card" @click="onClick">
    <view class="card-header">
      <view class="avatar-wrapper">
        <image
          class="avatar"
          :src="student.avatar || '/static/images/avatar-default.png'"
          mode="aspectFill"
        />
        <view class="gender-icon" :class="student.gender === 1 ? 'male' : 'female'">
          <u-icon :name="student.gender === 1 ? 'man' : 'woman'" size="12" color="#fff" />
        </view>
      </view>
      <view class="info">
        <view class="name-row">
          <text class="name">{{ student.realName }}</text>
          <StatusTag :type="statusType" :text="statusText" />
        </view>
        <text class="student-no">学号：{{ student.studentNo }}</text>
      </view>
    </view>

    <view class="card-body">
      <view class="info-item">
        <u-icon name="home" size="14" color="#9CA3AF" />
        <text class="label">班级：</text>
        <text class="value">{{ student.className || '-' }}</text>
      </view>
      <view class="info-item">
        <u-icon name="account" size="14" color="#9CA3AF" />
        <text class="label">院系：</text>
        <text class="value">{{ student.departmentName || '-' }}</text>
      </view>
      <view v-if="student.dormitoryName" class="info-item">
        <u-icon name="map" size="14" color="#9CA3AF" />
        <text class="label">宿舍：</text>
        <text class="value">{{ student.dormitoryName }} {{ student.bedNumber ? `(${student.bedNumber}床)` : '' }}</text>
      </view>
      <view v-if="student.phone" class="info-item">
        <u-icon name="phone" size="14" color="#9CA3AF" />
        <text class="label">电话：</text>
        <text class="value">{{ student.phone }}</text>
      </view>
    </view>

    <view v-if="showActions" class="card-actions">
      <view class="action-btn" @click.stop="onEdit">
        <u-icon name="edit-pen" size="16" color="#667eea" />
        <text>编辑</text>
      </view>
      <view class="action-btn" @click.stop="onCall" v-if="student.phone">
        <u-icon name="phone" size="16" color="#10B981" />
        <text>拨号</text>
      </view>
      <view class="action-btn" @click.stop="onMore">
        <u-icon name="more-dot-fill" size="16" color="#6B7280" />
        <text>更多</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, defineProps, defineEmits } from 'vue'
import type { Student } from '@/api/student'
import StatusTag from '@/components/common/StatusTag.vue'

const props = defineProps<{
  student: Student
  showActions?: boolean
}>()

const emit = defineEmits(['click', 'edit', 'call', 'more'])

const statusType = computed(() => {
  switch (props.student.status) {
    case 1: return 'success'  // 在籍
    case 2: return 'info'     // 已毕业
    case 3: return 'warning'  // 休学
    case 4: return 'error'    // 退学
    default: return 'default'
  }
})

const statusText = computed(() => {
  switch (props.student.status) {
    case 1: return '在籍'
    case 2: return '已毕业'
    case 3: return '休学'
    case 4: return '退学'
    default: return '未知'
  }
})

const onClick = () => {
  emit('click', props.student)
}

const onEdit = () => {
  emit('edit', props.student)
}

const onCall = () => {
  if (props.student.phone) {
    uni.makePhoneCall({
      phoneNumber: props.student.phone
    })
  }
  emit('call', props.student)
}

const onMore = () => {
  emit('more', props.student)
}
</script>

<style lang="scss" scoped>
.student-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);

  .card-header {
    display: flex;
    align-items: center;
    margin-bottom: 20rpx;

    .avatar-wrapper {
      position: relative;
      margin-right: 20rpx;

      .avatar {
        width: 88rpx;
        height: 88rpx;
        border-radius: 50%;
        background: #F3F4F6;
      }

      .gender-icon {
        position: absolute;
        right: -4rpx;
        bottom: 0;
        width: 28rpx;
        height: 28rpx;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;

        &.male {
          background: #3B82F6;
        }
        &.female {
          background: #EC4899;
        }
      }
    }

    .info {
      flex: 1;

      .name-row {
        display: flex;
        align-items: center;
        gap: 12rpx;
        margin-bottom: 8rpx;

        .name {
          font-size: 32rpx;
          font-weight: 600;
          color: #1F2937;
        }
      }

      .student-no {
        font-size: 24rpx;
        color: #6B7280;
      }
    }
  }

  .card-body {
    .info-item {
      display: flex;
      align-items: center;
      gap: 8rpx;
      margin-bottom: 12rpx;

      &:last-child {
        margin-bottom: 0;
      }

      .label {
        font-size: 24rpx;
        color: #9CA3AF;
      }

      .value {
        font-size: 24rpx;
        color: #4B5563;
      }
    }
  }

  .card-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 32rpx;
    margin-top: 20rpx;
    padding-top: 20rpx;
    border-top: 1rpx solid #F3F4F6;

    .action-btn {
      display: flex;
      align-items: center;
      gap: 8rpx;

      text {
        font-size: 24rpx;
        color: #6B7280;
      }
    }
  }
}
</style>
