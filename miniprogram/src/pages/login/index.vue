<template>
  <view class="page">
    <!-- 顶部安全区 -->
    <view class="safe-top"></view>

    <!-- 品牌区域 -->
    <view class="brand-section">
      <view class="logo">
        <text class="logo-text">S</text>
      </view>
      <text class="brand-name">学生管理系统</text>
      <text class="brand-desc">量化检查管理平台</text>
    </view>

    <!-- 登录表单 -->
    <view class="form-section">
      <!-- 标签切换 -->
      <view class="tab-bar">
        <view
          class="tab-item"
          :class="{ active: loginType === 'account' }"
          @click="loginType = 'account'"
        >
          <text>账号登录</text>
        </view>
        <view
          class="tab-item"
          :class="{ active: loginType === 'wx' }"
          @click="loginType = 'wx'"
        >
          <text>微信登录</text>
        </view>
      </view>

      <!-- 账号登录表单 -->
      <view v-if="loginType === 'account'" class="form-content">
        <view class="input-group">
          <text class="input-label">用户名</text>
          <input
            v-model="formData.username"
            type="text"
            placeholder="请输入用户名或学号"
            class="input-field"
            maxlength="50"
          />
        </view>

        <view class="input-group">
          <text class="input-label">密码</text>
          <view class="input-wrap">
            <input
              v-model="formData.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              class="input-field"
              maxlength="50"
            />
            <view class="input-suffix" @click="showPassword = !showPassword">
              <text>{{ showPassword ? '隐藏' : '显示' }}</text>
            </view>
          </view>
        </view>

        <view class="form-options">
          <view class="remember-wrap" @click="rememberMe = !rememberMe">
            <view class="checkbox" :class="{ checked: rememberMe }"></view>
            <text>记住账号</text>
          </view>
        </view>

        <button
          class="submit-btn"
          :class="{ disabled: loading || !canSubmit }"
          :loading="loading"
          @click="handleAccountLogin"
        >
          {{ loading ? '' : '登 录' }}
        </button>
      </view>

      <!-- 微信登录 -->
      <view v-else class="wx-content">
        <view class="wx-desc">
          <text>使用微信账号快捷登录</text>
          <text class="wx-tip">首次登录需绑定系统账号</text>
        </view>
        <button
          class="wx-btn"
          :loading="loading"
          @click="handleWxLogin"
        >
          {{ loading ? '' : '微信一键登录' }}
        </button>
      </view>
    </view>

    <!-- 底部协议 -->
    <view class="footer-section">
      <text class="footer-text">登录即表示同意</text>
      <text class="footer-link" @click="showAgreement('user')">《用户协议》</text>
      <text class="footer-text">和</text>
      <text class="footer-link" @click="showAgreement('privacy')">《隐私政策》</text>
    </view>

    <!-- 绑定弹窗 -->
    <view class="modal-overlay" v-if="showBindModal" @click.self="cancelBind">
      <view class="modal-content">
        <view class="modal-header">
          <text class="modal-title">绑定系统账号</text>
          <text class="modal-desc">首次使用微信登录，请绑定您的系统账号</text>
        </view>

        <view class="modal-body">
          <view class="input-group">
            <text class="input-label">用户名</text>
            <input
              v-model="bindData.username"
              type="text"
              placeholder="请输入用户名/学号"
              class="input-field"
              maxlength="50"
            />
          </view>

          <view class="input-group">
            <text class="input-label">密码</text>
            <input
              v-model="bindData.password"
              :type="showBindPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              class="input-field"
              maxlength="50"
            />
          </view>
        </view>

        <view class="modal-footer">
          <button class="btn-cancel" @click="cancelBind">取消</button>
          <button
            class="btn-confirm"
            :loading="bindLoading"
            :disabled="bindLoading || !canBind"
            @click="handleBind"
          >
            确认绑定
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { login, wxLogin, bindWxAccount, getUserInfo } from '@/api/auth'
import { getStorage, setStorage, removeStorage } from '@/utils/storage'

const userStore = useUserStore()

// 状态
const loginType = ref<'account' | 'wx'>('account')
const loading = ref(false)
const showPassword = ref(false)
const rememberMe = ref(true)

// 绑定弹窗状态
const showBindModal = ref(false)
const bindLoading = ref(false)
const showBindPassword = ref(false)
const wxCode = ref('')

// 表单数据
const formData = ref({
  username: '',
  password: ''
})

const bindData = ref({
  username: '',
  password: ''
})

// 是否可提交
const canSubmit = computed(() => {
  return formData.value.username.trim() && formData.value.password.trim()
})

const canBind = computed(() => {
  return bindData.value.username.trim() && bindData.value.password.trim() && wxCode.value
})

// 初始化
onMounted(() => {
  const savedUsername = getStorage<string>('remembered_username')
  if (savedUsername) {
    formData.value.username = savedUsername
    rememberMe.value = true
  }
})

// 账号密码登录
async function handleAccountLogin() {
  if (!canSubmit.value) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' })
    return
  }

  loading.value = true
  try {
    const loginRes = await login({
      username: formData.value.username.trim(),
      password: formData.value.password
    })

    // 同步 token 到 userStore（确保 isLoggedIn 状态正确）
    userStore.setToken(loginRes.accessToken, loginRes.refreshToken)

    if (rememberMe.value) {
      setStorage('remembered_username', formData.value.username.trim())
    } else {
      removeStorage('remembered_username')
    }

    if (loginRes.userInfo) {
      userStore.setUserInfo(loginRes.userInfo)
    } else {
      const userInfo = await getUserInfo()
      userStore.setUserInfo(userInfo)
    }

    uni.showToast({ title: '登录成功', icon: 'success' })

    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' })
    }, 1000)
  } catch (error: any) {
    console.error('登录失败:', error)
    uni.showToast({ title: error.message || '登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 微信登录
async function handleWxLogin() {
  loading.value = true
  try {
    const loginResult = await uni.login({ provider: 'weixin' })

    if (!loginResult.code) {
      throw new Error('获取微信授权失败')
    }

    const wxLoginRes = await wxLogin({ code: loginResult.code })

    if (wxLoginRes.bound && wxLoginRes.loginResponse) {
      // 同步 token 到 userStore
      userStore.setToken(
        wxLoginRes.loginResponse.accessToken,
        wxLoginRes.loginResponse.refreshToken
      )

      if (wxLoginRes.loginResponse.userInfo) {
        userStore.setUserInfo(wxLoginRes.loginResponse.userInfo)
      } else {
        const userInfo = await getUserInfo()
        userStore.setUserInfo(userInfo)
      }

      uni.showToast({ title: '登录成功', icon: 'success' })

      setTimeout(() => {
        uni.switchTab({ url: '/pages/index/index' })
      }, 1000)
    } else {
      const newLoginResult = await uni.login({ provider: 'weixin' })
      wxCode.value = newLoginResult.code
      showBindModal.value = true
    }
  } catch (error: any) {
    console.error('微信登录失败:', error)
    uni.showToast({ title: error.message || '微信登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function cancelBind() {
  showBindModal.value = false
  wxCode.value = ''
  bindData.value = { username: '', password: '' }
}

async function handleBind() {
  if (!canBind.value) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' })
    return
  }

  bindLoading.value = true
  try {
    const bindRes = await bindWxAccount({
      code: wxCode.value,
      username: bindData.value.username.trim(),
      password: bindData.value.password
    })

    // 同步 token 到 userStore
    userStore.setToken(bindRes.accessToken, bindRes.refreshToken)

    if (bindRes.userInfo) {
      userStore.setUserInfo(bindRes.userInfo)
    } else {
      const userInfo = await getUserInfo()
      userStore.setUserInfo(userInfo)
    }

    showBindModal.value = false
    uni.showToast({ title: '绑定成功', icon: 'success' })

    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' })
    }, 1000)
  } catch (error: any) {
    console.error('绑定失败:', error)
    uni.showToast({ title: error.message || '绑定失败', icon: 'none' })
  } finally {
    bindLoading.value = false
  }
}

function showAgreement(type: 'user' | 'privacy') {
  uni.showToast({ title: '协议功能开发中', icon: 'none' })
}
</script>

<style lang="scss" scoped>
// 变量
$primary: #4F46E5;
$primary-light: #EEF2FF;
$text-primary: #111827;
$text-secondary: #6B7280;
$text-muted: #9CA3AF;
$border: #E5E7EB;
$bg: #F9FAFB;
$white: #FFFFFF;
$success: #059669;

.page {
  min-height: 100vh;
  background: $bg;
  display: flex;
  flex-direction: column;
}

.safe-top {
  height: env(safe-area-inset-top);
}

// 品牌区域
.brand-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 40rpx 60rpx;
}

.logo {
  width: 120rpx;
  height: 120rpx;
  background: $primary;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32rpx;

  .logo-text {
    font-size: 60rpx;
    font-weight: 700;
    color: $white;
  }
}

.brand-name {
  font-size: 40rpx;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 8rpx;
}

.brand-desc {
  font-size: 26rpx;
  color: $text-muted;
}

// 表单区域
.form-section {
  flex: 1;
  margin: 0 32rpx;
  background: $white;
  border-radius: 16rpx;
  border: 1rpx solid $border;
  overflow: hidden;
}

// 标签栏
.tab-bar {
  display: flex;
  border-bottom: 1rpx solid $border;
}

.tab-item {
  flex: 1;
  padding: 28rpx;
  text-align: center;
  position: relative;

  text {
    font-size: 28rpx;
    color: $text-secondary;
  }

  &.active {
    text {
      color: $primary;
      font-weight: 500;
    }

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 80rpx;
      height: 4rpx;
      background: $primary;
      border-radius: 2rpx;
    }
  }
}

// 表单内容
.form-content,
.wx-content {
  padding: 40rpx 32rpx;
}

.input-group {
  margin-bottom: 32rpx;

  .input-label {
    display: block;
    font-size: 26rpx;
    color: $text-secondary;
    margin-bottom: 12rpx;
  }
}

.input-wrap {
  display: flex;
  align-items: center;
}

.input-field {
  flex: 1;
  height: 96rpx;
  padding: 0 24rpx;
  background: $bg;
  border: 1rpx solid $border;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: $text-primary;

  &::placeholder {
    color: $text-muted;
  }
}

.input-suffix {
  margin-left: -100rpx;
  padding: 0 24rpx;
  z-index: 1;

  text {
    font-size: 24rpx;
    color: $primary;
  }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40rpx;
}

.remember-wrap {
  display: flex;
  align-items: center;
  gap: 12rpx;

  .checkbox {
    width: 36rpx;
    height: 36rpx;
    border: 2rpx solid $border;
    border-radius: 6rpx;

    &.checked {
      background: $primary;
      border-color: $primary;
      position: relative;

      &::after {
        content: '';
        position: absolute;
        top: 6rpx;
        left: 12rpx;
        width: 8rpx;
        height: 16rpx;
        border: 2rpx solid $white;
        border-top: none;
        border-left: none;
        transform: rotate(45deg);
      }
    }
  }

  text {
    font-size: 26rpx;
    color: $text-secondary;
  }
}

.submit-btn {
  width: 100%;
  height: 96rpx;
  background: $primary;
  border-radius: 12rpx;
  font-size: 32rpx;
  font-weight: 500;
  color: $white;
  border: none;

  &.disabled {
    background: $border;
    color: $text-muted;
  }
}

// 微信登录
.wx-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 32rpx;
}

.wx-desc {
  text-align: center;
  margin-bottom: 48rpx;

  text {
    display: block;
    font-size: 28rpx;
    color: $text-secondary;
  }

  .wx-tip {
    font-size: 24rpx;
    color: $text-muted;
    margin-top: 12rpx;
  }
}

.wx-btn {
  width: 100%;
  height: 96rpx;
  background: $success;
  border-radius: 12rpx;
  font-size: 32rpx;
  font-weight: 500;
  color: $white;
  border: none;
}

// 底部协议
.footer-section {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40rpx;
  flex-wrap: wrap;

  .footer-text {
    font-size: 24rpx;
    color: $text-muted;
  }

  .footer-link {
    font-size: 24rpx;
    color: $primary;
  }
}

// 弹窗
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-content {
  width: 640rpx;
  background: $white;
  border-radius: 16rpx;
  overflow: hidden;
}

.modal-header {
  padding: 40rpx 32rpx 24rpx;
  text-align: center;

  .modal-title {
    display: block;
    font-size: 34rpx;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 12rpx;
  }

  .modal-desc {
    display: block;
    font-size: 26rpx;
    color: $text-muted;
  }
}

.modal-body {
  padding: 24rpx 32rpx;
}

.modal-footer {
  display: flex;
  padding: 24rpx 32rpx 40rpx;
  gap: 24rpx;

  button {
    flex: 1;
    height: 88rpx;
    border-radius: 12rpx;
    font-size: 28rpx;
    font-weight: 500;
    border: none;
  }

  .btn-cancel {
    background: $bg;
    color: $text-secondary;
  }

  .btn-confirm {
    background: $primary;
    color: $white;

    &[disabled] {
      background: $border;
      color: $text-muted;
    }
  }
}
</style>
