<template>
  <view class="login-page">
    <view class="login-header">
      <view class="login-logo">
        <wd-icon name="user" size="60rpx" color="#3a7bd5" />
      </view>
      <text class="login-title">通用管理平台</text>
      <text class="login-subtitle">登录以继续使用</text>
    </view>

    <view class="login-form">
      <view class="form-item">
        <wd-input
          v-model="form.username"
          placeholder="请输入用户名"
          clearable
          prefix-icon="user"
        />
      </view>
      <view class="form-item">
        <wd-input
          v-model="form.password"
          placeholder="请输入密码"
          show-password
          clearable
          prefix-icon="lock"
        />
      </view>

      <wd-button
        type="primary"
        block
        :loading="loading"
        :disabled="!canSubmit"
        custom-class="login-btn"
        @click="handleLogin"
      >
        登录
      </wd-button>

      <view class="login-divider">
        <view class="divider-line" />
        <text class="divider-text">其他登录方式</text>
        <view class="divider-line" />
      </view>

      <wd-button
        block
        plain
        custom-class="wechat-btn"
        @click="handleWechatLogin"
      >
        微信快捷登录
      </wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const canSubmit = computed(() => form.username.trim() && form.password.trim())

async function handleLogin() {
  if (!canSubmit.value || loading.value) return
  loading.value = true
  try {
    await authStore.login({ username: form.username.trim(), password: form.password })
    uni.switchTab({ url: '/pages/index/index' })
  } catch {
    // Error toast handled by request layer
  } finally {
    loading.value = false
  }
}

function handleWechatLogin() {
  uni.showToast({ title: '微信登录开发中', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: $bg-page;
  padding: $spacing-page;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-header {
  text-align: center;
  margin-bottom: 80rpx;
}

.login-logo {
  width: 120rpx;
  height: 120rpx;
  border-radius: 30rpx;
  background: $color-primary-light;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24rpx;
}

.login-title {
  display: block;
  font-size: $font-xl;
  font-weight: 700;
  color: $text-primary;
}

.login-subtitle {
  display: block;
  font-size: $font-sm;
  color: $text-placeholder;
  margin-top: 8rpx;
}

.login-form {
  background: $bg-card;
  border-radius: $radius-card;
  padding: $spacing-lg;
  box-shadow: $shadow-card;
}

.form-item {
  margin-bottom: $spacing-md;
}

.login-btn {
  margin-top: $spacing-lg;
  height: 88rpx;
  border-radius: $radius-btn !important;
}

.login-divider {
  display: flex;
  align-items: center;
  margin: $spacing-xl 0;
}

.divider-line {
  flex: 1;
  height: 1px;
  background: $border-light;
}

.divider-text {
  font-size: $font-xs;
  color: $text-placeholder;
  padding: 0 $spacing-md;
}

.wechat-btn {
  border-radius: $radius-btn !important;
}
</style>
