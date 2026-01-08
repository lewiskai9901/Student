<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <u-icon name="arrow-left" size="20" color="#fff" />
        </view>
        <text class="nav-title">学生详情</text>
        <view class="nav-actions">
          <view class="action-btn" @click="goToEdit" v-if="canEdit">
            <u-icon name="edit-pen" size="20" color="#fff" />
          </view>
        </view>
      </view>
    </view>

    <!-- 内容区域 -->
    <scroll-view scroll-y class="content" :style="{ marginTop: navBarHeight + 'px' }">
      <!-- 加载中 -->
      <view v-if="loading" class="loading-wrapper">
        <u-loading-icon size="40" color="#667eea" />
      </view>

      <template v-else-if="student">
        <!-- 头部信息 -->
        <view class="header-section">
          <view class="avatar-wrapper">
            <image
              class="avatar"
              :src="student.avatar || '/static/images/avatar-default.png'"
              mode="aspectFill"
            />
            <view class="gender-badge" :class="student.gender === 1 ? 'male' : 'female'">
              <u-icon :name="student.gender === 1 ? 'man' : 'woman'" size="14" color="#fff" />
            </view>
          </view>
          <view class="basic-info">
            <view class="name-row">
              <text class="name">{{ student.realName }}</text>
              <StatusTag :type="statusType" :text="statusText" />
            </view>
            <text class="student-no">学号：{{ student.studentNo }}</text>
          </view>
        </view>

        <!-- 详细信息 -->
        <view class="info-section">
          <view class="section-title">基本信息</view>
          <view class="info-list">
            <view class="info-item">
              <text class="label">性别</text>
              <text class="value">{{ student.gender === 1 ? '男' : '女' }}</text>
            </view>
            <view class="info-item">
              <text class="label">出生日期</text>
              <text class="value">{{ student.birthDate || '-' }}</text>
            </view>
            <view class="info-item">
              <text class="label">身份证号</text>
              <text class="value">{{ maskIdCard(student.identityCard) }}</text>
            </view>
            <view class="info-item">
              <text class="label">入学日期</text>
              <text class="value">{{ student.enrollmentDate || '-' }}</text>
            </view>
          </view>
        </view>

        <view class="info-section">
          <view class="section-title">联系方式</view>
          <view class="info-list">
            <view class="info-item" @click="makeCall" v-if="student.phone">
              <text class="label">手机号码</text>
              <view class="value-action">
                <text class="value">{{ student.phone }}</text>
                <u-icon name="phone" size="18" color="#667eea" />
              </view>
            </view>
            <view class="info-item">
              <text class="label">电子邮箱</text>
              <text class="value">{{ student.email || '-' }}</text>
            </view>
          </view>
        </view>

        <view class="info-section">
          <view class="section-title">学籍信息</view>
          <view class="info-list">
            <view class="info-item">
              <text class="label">所属院系</text>
              <text class="value">{{ student.departmentName || '-' }}</text>
            </view>
            <view class="info-item">
              <text class="label">专业</text>
              <text class="value">{{ student.majorName || '-' }}</text>
            </view>
            <view class="info-item">
              <text class="label">年级</text>
              <text class="value">{{ student.gradeName || '-' }}</text>
            </view>
            <view class="info-item">
              <text class="label">班级</text>
              <text class="value">{{ student.className || '-' }}</text>
            </view>
          </view>
        </view>

        <view class="info-section" v-if="student.dormitoryName">
          <view class="section-title">宿舍信息</view>
          <view class="info-list">
            <view class="info-item">
              <text class="label">宿舍</text>
              <text class="value">{{ student.dormitoryName }}</text>
            </view>
            <view class="info-item" v-if="student.bedNumber">
              <text class="label">床位</text>
              <text class="value">{{ student.bedNumber }}号床</text>
            </view>
          </view>
        </view>

        <!-- 快捷操作 -->
        <view class="action-section" v-if="canEdit">
          <view class="action-grid">
            <view class="action-item" @click="goToAssignDorm">
              <view class="action-icon">
                <u-icon name="home" size="24" color="#667eea" />
              </view>
              <text class="action-text">分配宿舍</text>
            </view>
            <view class="action-item" @click="goToTransfer">
              <view class="action-icon">
                <u-icon name="arrow-right" size="24" color="#10B981" />
              </view>
              <text class="action-text">转班</text>
            </view>
            <view class="action-item" @click="resetPwd">
              <view class="action-icon">
                <u-icon name="lock" size="24" color="#F59E0B" />
              </view>
              <text class="action-text">重置密码</text>
            </view>
            <view class="action-item" @click="confirmDelete">
              <view class="action-icon">
                <u-icon name="trash" size="24" color="#EF4444" />
              </view>
              <text class="action-text">删除</text>
            </view>
          </view>
        </view>
      </template>

      <EmptyState v-else type="error" text="学生信息不存在" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getStudentById, deleteStudent, resetPassword, type Student } from '@/api/student'
import { hasPermission, PermissionCode } from '@/utils/permission'
import StatusTag from '@/components/common/StatusTag.vue'
import EmptyState from '@/components/common/EmptyState.vue'

// 系统信息
const statusBarHeight = ref(0)
const navBarHeight = ref(88)

// 权限
const canEdit = computed(() => hasPermission(PermissionCode.STUDENT_UPDATE))

// 数据
const loading = ref(true)
const student = ref<Student | null>(null)
const studentId = ref<string>('')

const statusType = computed(() => {
  switch (student.value?.status) {
    case 1: return 'success'
    case 2: return 'info'
    case 3: return 'warning'
    case 4: return 'error'
    default: return 'default'
  }
})

const statusText = computed(() => {
  switch (student.value?.status) {
    case 1: return '在籍'
    case 2: return '已毕业'
    case 3: return '休学'
    case 4: return '退学'
    default: return '未知'
  }
})

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
  studentId.value = (currentPage as any).options?.id

  if (!studentId.value) {
    loading.value = false
    return
  }

  try {
    const res = await getStudentById(studentId.value)
    student.value = res
  } catch (error: any) {
    console.error('加载学生详情失败', error)
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const maskIdCard = (idCard?: string) => {
  if (!idCard) return '-'
  if (idCard.length < 8) return idCard
  return idCard.substring(0, 4) + '****' + idCard.substring(idCard.length - 4)
}

const goBack = () => {
  uni.navigateBack()
}

const goToEdit = () => {
  uni.navigateTo({
    url: `/pages/student/edit?id=${studentId.value}`
  })
}

const makeCall = () => {
  if (student.value?.phone) {
    uni.makePhoneCall({
      phoneNumber: student.value.phone
    })
  }
}

const goToAssignDorm = () => {
  uni.showToast({ title: '功能开发中', icon: 'none' })
}

const goToTransfer = () => {
  uni.showToast({ title: '功能开发中', icon: 'none' })
}

const resetPwd = () => {
  uni.showModal({
    title: '重置密码',
    content: '确定要重置该学生的密码吗？重置后密码将变为默认密码。',
    success: async (res) => {
      if (res.confirm) {
        try {
          await resetPassword(studentId.value, '123456')
          uni.showToast({ title: '密码已重置', icon: 'success' })
        } catch (error: any) {
          uni.showToast({ title: error.message || '重置失败', icon: 'none' })
        }
      }
    }
  })
}

const confirmDelete = () => {
  uni.showModal({
    title: '确认删除',
    content: `确定要删除学生"${student.value?.realName}"吗？此操作不可恢复！`,
    confirmColor: '#EF4444',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteStudent(studentId.value)
          uni.showToast({ title: '删除成功', icon: 'success' })
          setTimeout(() => {
            uni.navigateBack()
          }, 1500)
        } catch (error: any) {
          uni.showToast({ title: error.message || '删除失败', icon: 'none' })
        }
      }
    }
  })
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

    .nav-back {
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

    .nav-actions {
      .action-btn {
        width: 64rpx;
        height: 64rpx;
        display: flex;
        align-items: center;
        justify-content: center;
      }
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

  .avatar-wrapper {
    position: relative;
    margin-right: 24rpx;

    .avatar {
      width: 120rpx;
      height: 120rpx;
      border-radius: 50%;
      background: #F3F4F6;
    }

    .gender-badge {
      position: absolute;
      right: -4rpx;
      bottom: 0;
      width: 36rpx;
      height: 36rpx;
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

  .basic-info {
    flex: 1;

    .name-row {
      display: flex;
      align-items: center;
      gap: 16rpx;
      margin-bottom: 12rpx;

      .name {
        font-size: 40rpx;
        font-weight: 600;
        color: #1F2937;
      }
    }

    .student-no {
      font-size: 28rpx;
      color: #6B7280;
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

      .value-action {
        display: flex;
        align-items: center;
        gap: 12rpx;
      }
    }
  }
}

.action-section {
  background: #fff;
  padding: 32rpx;
  margin-bottom: 40rpx;

  .action-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 24rpx;

    .action-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12rpx;

      .action-icon {
        width: 80rpx;
        height: 80rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #F3F4F6;
        border-radius: 16rpx;
      }

      .action-text {
        font-size: 24rpx;
        color: #4B5563;
      }
    }
  }
}
</style>
