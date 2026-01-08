<template>
  <view class="mine-page">
    <!-- 顶部个人信息区域 -->
    <view class="profile-header">
      <view class="header-bg"></view>

      <!-- 个人信息卡片 -->
      <view class="profile-card">
        <view class="avatar-section">
          <view class="avatar-wrapper">
            <image class="avatar" :src="userStore.userAvatar || '/static/default-avatar.png'" mode="aspectFill" />
          </view>
        </view>

        <view class="info-section">
          <text class="user-name">{{ userStore.userName || '未登录' }}</text>
          <text class="user-desc">{{ userDesc }}</text>
          <view class="user-tags">
            <view class="tag primary" v-if="userStore.isAdmin">
              <text>&#9881;</text>
              <text>管理员</text>
            </view>
            <view class="tag success" v-else-if="userStore.isTeacher">
              <text>&#128100;</text>
              <text>教师</text>
            </view>
            <view class="tag info" v-else>
              <text>&#128100;</text>
              <text>用户</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 功能菜单区域 -->
    <view class="menu-container">
      <!-- 量化管理组 -->
      <view class="menu-group">
        <view class="group-title">
          <text class="title-icon">&#128202;</text>
          <text>量化管理</text>
        </view>
        <view class="menu-list">
          <view class="menu-item" @click="goToMyRecords">
            <view class="item-icon myrecord">
              <text>&#128203;</text>
            </view>
            <view class="item-content">
              <text class="item-title">本班记录</text>
              <text class="item-desc">查看本班检查记录</text>
            </view>
            <text class="item-arrow">&#10148;</text>
          </view>
          <view class="menu-item" @click="goToAppeals">
            <view class="item-icon appeal">
              <text>&#128221;</text>
            </view>
            <view class="item-content">
              <text class="item-title">我的申诉</text>
              <text class="item-desc">查看申诉记录</text>
            </view>
            <text class="item-arrow">&#10148;</text>
          </view>
        </view>
      </view>

      <!-- 账户安全组 -->
      <view class="menu-group">
        <view class="group-title">
          <text class="title-icon">&#128274;</text>
          <text>账户安全</text>
        </view>
        <view class="menu-list">
          <view class="menu-item" @click="showPasswordModal = true">
            <view class="item-icon password">
              <text>&#128272;</text>
            </view>
            <view class="item-content">
              <text class="item-title">修改密码</text>
              <text class="item-desc">定期修改密码更安全</text>
            </view>
            <text class="item-arrow">&#10148;</text>
          </view>
        </view>
      </view>

      <!-- 系统设置组 -->
      <view class="menu-group">
        <view class="group-title">
          <text class="title-icon">&#9881;</text>
          <text>系统设置</text>
        </view>
        <view class="menu-list">
          <view class="menu-item" @click="clearCache">
            <view class="item-icon cache">
              <text>&#128465;</text>
            </view>
            <view class="item-content">
              <text class="item-title">清除缓存</text>
              <text class="item-desc">清理本地缓存数据</text>
            </view>
            <view class="item-extra">
              <text class="extra-text">{{ cacheSize }}</text>
              <text class="item-arrow">&#10148;</text>
            </view>
          </view>
          <view class="menu-item" @click="showAbout">
            <view class="item-icon about">
              <text>&#9432;</text>
            </view>
            <view class="item-content">
              <text class="item-title">关于我们</text>
              <text class="item-desc">了解更多应用信息</text>
            </view>
            <view class="item-extra">
              <text class="extra-text">v{{ appVersion }}</text>
              <text class="item-arrow">&#10148;</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 退出登录按钮 -->
    <view class="logout-section">
      <button class="logout-btn" @click="handleLogout">
        <text class="logout-icon">&#9211;</text>
        <text>退出登录</text>
      </button>
    </view>

    <!-- 底部安全区 -->
    <view class="safe-bottom"></view>

    <!-- 修改密码弹窗 -->
    <view class="modal-mask" v-if="showPasswordModal" @click="showPasswordModal = false">
      <view class="password-modal" @click.stop>
        <view class="modal-header">
          <text class="modal-icon">&#128272;</text>
          <text class="modal-title">修改密码</text>
        </view>

        <view class="modal-form">
          <view class="form-item">
            <text class="form-label">原密码</text>
            <input
              class="form-input"
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="请输入原密码"
            />
          </view>

          <view class="form-item">
            <text class="form-label">新密码</text>
            <input
              class="form-input"
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入新密码(6-20位)"
            />
          </view>

          <view class="form-item">
            <text class="form-label">确认密码</text>
            <input
              class="form-input"
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
            />
          </view>
        </view>

        <view class="modal-actions">
          <view class="cancel-btn" @click="showPasswordModal = false">
            <text>取消</text>
          </view>
          <view class="confirm-btn" @click="submitPassword">
            <text>{{ submitting ? '提交中...' : '确认修改' }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { logout, changePassword } from '@/api/auth'

const userStore = useUserStore()

// 状态
const cacheSize = ref('0KB')
const appVersion = ref('1.0.0')
const showPasswordModal = ref(false)
const submitting = ref(false)

// 密码表单
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 用户描述
const userDesc = computed(() => {
  if (userStore.isAdmin) return '系统管理员'
  if (userStore.isTeacher) return '教师'
  return '用户'
})

// 初始化
onShow(() => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  getCacheSize()
})

onMounted(() => {
  try {
    const systemInfo = uni.getSystemInfoSync()
    appVersion.value = systemInfo.appVersion || '1.0.0'
  } catch (e) {
    console.error('获取系统信息失败', e)
  }
})

// 获取缓存大小
function getCacheSize() {
  try {
    const res = uni.getStorageInfoSync()
    const size = res.currentSize
    if (size < 1024) {
      cacheSize.value = size + 'KB'
    } else {
      cacheSize.value = (size / 1024).toFixed(2) + 'MB'
    }
  } catch (e) {
    cacheSize.value = '0KB'
  }
}

// 跳转本班记录
function goToMyRecords() {
  uni.navigateTo({ url: '/pages/check/record/index?viewMode=myclass' })
}

// 跳转申诉
function goToAppeals() {
  uni.navigateTo({ url: '/pages/appeal/index' })
}

// 显示关于
function showAbout() {
  uni.showModal({
    title: '关于我们',
    content: '学生管理系统 - 量化检查小程序\n版本: ' + appVersion.value + '\n\n本系统用于学校量化检查管理，支持检查计划管理、检查记录查询、申诉管理等功能。',
    showCancel: false,
    confirmText: '知道了',
    confirmColor: '#667eea'
  })
}

// 清除缓存
function clearCache() {
  uni.showModal({
    title: '提示',
    content: '确定要清除缓存吗？',
    confirmColor: '#667eea',
    success: (res) => {
      if (res.confirm) {
        try {
          // 保存登录状态
          const token = userStore.token
          const refreshToken = userStore.refreshToken
          const userInfo = userStore.userInfo

          uni.clearStorageSync()

          // 恢复登录状态
          if (token) userStore.setToken(token, refreshToken)
          if (userInfo) userStore.setUserInfo(userInfo)

          getCacheSize()
          uni.showToast({ title: '缓存已清除', icon: 'success' })
        } catch (e) {
          uni.showToast({ title: '清除失败', icon: 'none' })
        }
      }
    }
  })
}

// 提交修改密码
async function submitPassword() {
  const { oldPassword, newPassword, confirmPassword } = passwordForm.value

  if (!oldPassword || !newPassword || !confirmPassword) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' })
    return
  }

  if (newPassword.length < 6 || newPassword.length > 20) {
    uni.showToast({ title: '新密码长度应为6-20位', icon: 'none' })
    return
  }

  if (newPassword !== confirmPassword) {
    uni.showToast({ title: '两次密码输入不一致', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    await changePassword({ oldPassword, newPassword })
    showPasswordModal.value = false
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    uni.showToast({ title: '密码修改成功', icon: 'success' })
  } catch (error: any) {
    uni.showToast({ title: error.message || '修改失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

// 退出登录
function handleLogout() {
  uni.showModal({
    title: '确认退出',
    content: '确定要退出登录吗？',
    confirmColor: '#f44336',
    success: async (res) => {
      if (res.confirm) {
        try {
          await logout()
        } catch (e) {
          // 忽略错误
        }
        userStore.logout()
        uni.reLaunch({ url: '/pages/login/index' })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.mine-page {
  min-height: 100vh;
  background: #f5f6fa;
}

// 顶部区域
.profile-header {
  position: relative;
  padding-top: calc(env(safe-area-inset-top) + 40rpx);
  padding-bottom: 60rpx;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 360rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

// 个人信息卡片
.profile-card {
  position: relative;
  z-index: 1;
  margin: 0 24rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx;
  box-shadow: 0 10rpx 40rpx rgba(0, 0, 0, 0.1);
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 24rpx;
}

.avatar-wrapper {
  .avatar {
    width: 160rpx;
    height: 160rpx;
    border-radius: 50%;
    border: 6rpx solid #fff;
    box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.15);
  }
}

.info-section {
  text-align: center;

  .user-name {
    display: block;
    font-size: 40rpx;
    font-weight: 700;
    color: #333;
    margin-bottom: 8rpx;
  }

  .user-desc {
    display: block;
    font-size: 28rpx;
    color: #999;
    margin-bottom: 20rpx;
  }
}

.user-tags {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 16rpx;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 20rpx;
  border-radius: 24rpx;
  font-size: 24rpx;

  &.primary {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
  }

  &.success {
    background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
    color: #fff;
  }

  &.info {
    background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
    color: #fff;
  }
}

// 功能菜单区域
.menu-container {
  padding: 0 24rpx;
}

.menu-group {
  margin-bottom: 24rpx;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 20rpx 16rpx;

  .title-icon {
    font-size: 32rpx;
  }

  text {
    font-size: 28rpx;
    font-weight: 600;
    color: #666;
  }
}

.menu-list {
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }

  &:active {
    background: #f9f9f9;
  }
}

.item-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
  font-size: 32rpx;

  &.myrecord {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  }

  &.appeal {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  }

  &.password {
    background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  }

  &.cache {
    background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  }

  &.about {
    background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  }
}

.item-content {
  flex: 1;

  .item-title {
    display: block;
    font-size: 30rpx;
    font-weight: 500;
    color: #333;
    margin-bottom: 6rpx;
  }

  .item-desc {
    display: block;
    font-size: 24rpx;
    color: #999;
  }
}

.item-extra {
  display: flex;
  align-items: center;
  gap: 8rpx;

  .extra-text {
    font-size: 26rpx;
    color: #999;
  }
}

.item-arrow {
  font-size: 24rpx;
  color: #ccc;
}

// 退出登录
.logout-section {
  padding: 40rpx 24rpx;
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  width: 100%;
  height: 100rpx;
  background: #fff;
  border: 2rpx solid #ffebee;
  border-radius: 50rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);

  .logout-icon {
    font-size: 36rpx;
    color: #f44336;
  }

  text {
    font-size: 32rpx;
    font-weight: 500;
    color: #f44336;
  }

  &:active {
    background: #ffebee;
  }
}

// 底部安全区
.safe-bottom {
  height: 140rpx;
}

// 弹窗
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.password-modal {
  width: 620rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx;
}

.modal-header {
  text-align: center;
  margin-bottom: 40rpx;

  .modal-icon {
    display: block;
    font-size: 64rpx;
    margin-bottom: 16rpx;
  }

  .modal-title {
    font-size: 36rpx;
    font-weight: 700;
    color: #333;
  }
}

.modal-form {
  .form-item {
    margin-bottom: 24rpx;

    .form-label {
      display: block;
      font-size: 28rpx;
      font-weight: 500;
      color: #666;
      margin-bottom: 12rpx;
    }

    .form-input {
      width: 100%;
      height: 96rpx;
      padding: 0 24rpx;
      background: #f5f6fa;
      border-radius: 16rpx;
      font-size: 30rpx;
      color: #333;
      box-sizing: border-box;
    }
  }
}

.modal-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 32rpx;
}

.cancel-btn,
.confirm-btn {
  flex: 1;
  height: 90rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 45rpx;
  font-size: 30rpx;
}

.cancel-btn {
  background: #f5f6fa;

  text {
    color: #666;
  }
}

.confirm-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  text {
    color: #fff;
    font-weight: 500;
  }
}
</style>
