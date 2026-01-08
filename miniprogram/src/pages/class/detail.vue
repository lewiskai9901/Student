<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <u-icon name="arrow-left" size="20" color="#fff" />
        </view>
        <text class="nav-title">班级详情</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <!-- 内容区域 -->
    <scroll-view scroll-y class="content" :style="{ marginTop: navBarHeight + 'px' }">
      <!-- 加载中 -->
      <view v-if="loading" class="loading-wrapper">
        <u-loading-icon size="40" color="#667eea" />
      </view>

      <template v-else-if="classInfo">
        <!-- 头部信息 -->
        <view class="header-section">
          <view class="class-icon">
            <u-icon name="home" size="40" color="#667eea" />
          </view>
          <view class="class-info">
            <view class="name-row">
              <text class="name">{{ classInfo.className }}</text>
              <StatusTag :type="classInfo.status === 1 ? 'success' : 'default'" :text="classInfo.status === 1 ? '正常' : '停用'" />
            </view>
            <text class="code">班级编码：{{ classInfo.classCode }}</text>
          </view>
        </view>

        <!-- 统计卡片 -->
        <view class="stats-section">
          <view class="stat-card">
            <text class="stat-value">{{ classInfo.studentCount || 0 }}</text>
            <text class="stat-label">学生总数</text>
          </view>
          <view class="stat-card">
            <text class="stat-value male">{{ classInfo.maleCount || 0 }}</text>
            <text class="stat-label">男生人数</text>
          </view>
          <view class="stat-card">
            <text class="stat-value female">{{ classInfo.femaleCount || 0 }}</text>
            <text class="stat-label">女生人数</text>
          </view>
        </view>

        <!-- 详细信息 -->
        <view class="info-section">
          <view class="section-title">基本信息</view>
          <view class="info-list">
            <view class="info-item">
              <text class="label">所属院系</text>
              <text class="value">{{ classInfo.departmentName || '-' }}</text>
            </view>
            <view class="info-item">
              <text class="label">专业</text>
              <text class="value">{{ classInfo.majorName || '-' }}</text>
            </view>
            <view class="info-item">
              <text class="label">年级</text>
              <text class="value">{{ classInfo.gradeName || '-' }}</text>
            </view>
            <view class="info-item">
              <text class="label">入学年份</text>
              <text class="value">{{ classInfo.enrollmentYear || '-' }}</text>
            </view>
            <view class="info-item">
              <text class="label">最大容量</text>
              <text class="value">{{ classInfo.maxCapacity || '-' }}人</text>
            </view>
          </view>
        </view>

        <view class="info-section">
          <view class="section-title">班主任信息</view>
          <view class="teacher-card" v-if="classInfo.headTeacherName">
            <view class="teacher-avatar">
              <u-icon name="account-fill" size="32" color="#667eea" />
            </view>
            <view class="teacher-info">
              <text class="teacher-name">{{ classInfo.headTeacherName }}</text>
              <text class="teacher-label">班主任</text>
            </view>
            <view class="teacher-action" @click="callTeacher" v-if="classInfo.headTeacherPhone">
              <u-icon name="phone" size="20" color="#667eea" />
            </view>
          </view>
          <view v-else class="no-teacher">
            <text>暂未分配班主任</text>
          </view>
        </view>

        <view class="info-section" v-if="classInfo.classroomName">
          <view class="section-title">教室信息</view>
          <view class="info-list">
            <view class="info-item">
              <text class="label">所在教室</text>
              <text class="value">{{ classInfo.classroomName }}</text>
            </view>
          </view>
        </view>

        <!-- 快捷操作 -->
        <view class="action-section">
          <view class="action-card" @click="goToStudents">
            <view class="action-content">
              <view class="action-icon">
                <u-icon name="account" size="24" color="#667eea" />
              </view>
              <view class="action-text">
                <text class="action-title">班级学生</text>
                <text class="action-desc">查看班级学生列表</text>
              </view>
            </view>
            <u-icon name="arrow-right" size="18" color="#9CA3AF" />
          </view>

          <view class="action-card" @click="goToCheckRecords">
            <view class="action-content">
              <view class="action-icon">
                <u-icon name="file-text" size="24" color="#10B981" />
              </view>
              <view class="action-text">
                <text class="action-title">检查记录</text>
                <text class="action-desc">查看班级检查记录</text>
              </view>
            </view>
            <u-icon name="arrow-right" size="18" color="#9CA3AF" />
          </view>

          <view class="action-card" @click="goToDormitories">
            <view class="action-content">
              <view class="action-icon">
                <u-icon name="home" size="24" color="#F59E0B" />
              </view>
              <view class="action-text">
                <text class="action-title">班级宿舍</text>
                <text class="action-desc">查看班级宿舍分配</text>
              </view>
            </view>
            <u-icon name="arrow-right" size="18" color="#9CA3AF" />
          </view>
        </view>
      </template>

      <EmptyState v-else type="error" text="班级信息不存在" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getClassDetail, type Class } from '@/api/class'
import StatusTag from '@/components/common/StatusTag.vue'
import EmptyState from '@/components/common/EmptyState.vue'

// 系统信息
const statusBarHeight = ref(0)
const navBarHeight = ref(88)

// 数据
const loading = ref(true)
const classInfo = ref<Class | null>(null)
const classId = ref<string>('')

onMounted(() => {
  initSystemInfo()
  loadData()
})

const initSystemInfo = () => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navBarHeight.value = statusBarHeight.value + 44
}

const loadData = async () => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  classId.value = (currentPage as any).options?.id

  if (!classId.value) {
    loading.value = false
    return
  }

  try {
    const res = await getClassDetail(classId.value)
    classInfo.value = res
  } catch (error: any) {
    console.error('加载班级详情失败', error)
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  uni.navigateBack()
}

const callTeacher = () => {
  // TODO: 获取班主任电话并拨打
  uni.showToast({ title: '功能开发中', icon: 'none' })
}

const goToStudents = () => {
  uni.navigateTo({
    url: `/pages/class/students?id=${classId.value}&name=${classInfo.value?.className}`
  })
}

const goToCheckRecords = () => {
  uni.navigateTo({
    url: `/pages/check/record/index?classId=${classId.value}`
  })
}

const goToDormitories = () => {
  uni.showToast({ title: '功能开发中', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.page-container {
  min-height: 100vh;
  background: #F3F4F6;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .nav-content {
    display: flex;
    align-items: center;
    height: 88rpx;
    padding: 0 24rpx;

    .nav-back, .nav-placeholder {
      width: 64rpx;
      height: 64rpx;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .nav-title {
      flex: 1;
      font-size: 36rpx;
      font-weight: 600;
      color: #fff;
      text-align: center;
    }
  }
}

.content {
  height: calc(100vh - var(--nav-height));

  .loading-wrapper {
    display: flex;
    justify-content: center;
    padding: 100rpx 0;
  }
}

.header-section {
  display: flex;
  align-items: center;
  padding: 40rpx 32rpx;
  background: #fff;
  margin-bottom: 20rpx;

  .class-icon {
    width: 100rpx;
    height: 100rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(102, 126, 234, 0.1);
    border-radius: 20rpx;
    margin-right: 24rpx;
  }

  .class-info {
    flex: 1;

    .name-row {
      display: flex;
      align-items: center;
      gap: 16rpx;
      margin-bottom: 12rpx;

      .name {
        font-size: 36rpx;
        font-weight: 600;
        color: #1F2937;
      }
    }

    .code {
      font-size: 26rpx;
      color: #6B7280;
    }
  }
}

.stats-section {
  display: flex;
  gap: 20rpx;
  padding: 0 24rpx;
  margin-bottom: 20rpx;

  .stat-card {
    flex: 1;
    background: #fff;
    border-radius: 16rpx;
    padding: 24rpx;
    display: flex;
    flex-direction: column;
    align-items: center;

    .stat-value {
      font-size: 40rpx;
      font-weight: 600;
      color: #667eea;

      &.male {
        color: #3B82F6;
      }
      &.female {
        color: #EC4899;
      }
    }

    .stat-label {
      font-size: 22rpx;
      color: #9CA3AF;
      margin-top: 8rpx;
    }
  }
}

.info-section {
  background: #fff;
  margin-bottom: 20rpx;
  padding: 24rpx 32rpx;

  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #1F2937;
    margin-bottom: 24rpx;
    padding-left: 16rpx;
    border-left: 6rpx solid #667eea;
  }

  .info-list {
    .info-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 20rpx 0;
      border-bottom: 1rpx solid #F3F4F6;

      &:last-child {
        border-bottom: none;
      }

      .label {
        font-size: 28rpx;
        color: #6B7280;
      }

      .value {
        font-size: 28rpx;
        color: #1F2937;
      }
    }
  }

  .teacher-card {
    display: flex;
    align-items: center;
    padding: 20rpx;
    background: #F9FAFB;
    border-radius: 12rpx;

    .teacher-avatar {
      width: 72rpx;
      height: 72rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(102, 126, 234, 0.1);
      border-radius: 50%;
      margin-right: 20rpx;
    }

    .teacher-info {
      flex: 1;

      .teacher-name {
        font-size: 30rpx;
        font-weight: 500;
        color: #1F2937;
        display: block;
      }

      .teacher-label {
        font-size: 24rpx;
        color: #9CA3AF;
      }
    }

    .teacher-action {
      width: 64rpx;
      height: 64rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(102, 126, 234, 0.1);
      border-radius: 50%;
    }
  }

  .no-teacher {
    text-align: center;
    padding: 32rpx;
    color: #9CA3AF;
    font-size: 28rpx;
  }
}

.action-section {
  padding: 0 24rpx 40rpx;

  .action-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #fff;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 16rpx;

    .action-content {
      display: flex;
      align-items: center;

      .action-icon {
        width: 72rpx;
        height: 72rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #F3F4F6;
        border-radius: 16rpx;
        margin-right: 20rpx;
      }

      .action-text {
        .action-title {
          font-size: 30rpx;
          font-weight: 500;
          color: #1F2937;
          display: block;
        }

        .action-desc {
          font-size: 24rpx;
          color: #9CA3AF;
        }
      }
    }
  }
}
</style>
