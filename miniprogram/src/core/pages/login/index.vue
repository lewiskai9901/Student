<script setup lang="ts">
import { ref } from 'vue'
import { useAuth } from '../../stores/auth'
import { HttpError } from '../../api/request'

declare const uni: any

const auth = useAuth()
const username = ref('')
const password = ref('')
const loading = ref(false)
const err = ref('')

async function submit() {
  if (!username.value || !password.value) {
    err.value = '请输入账号和密码'
    return
  }
  err.value = ''
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    uni.reLaunch({ url: '/core/pages/index/index' })
  } catch (e) {
    if (e instanceof HttpError) {
      const body = e.body as { message?: string } | undefined
      err.value = body?.message ?? `登录失败 (HTTP ${e.statusCode})`
    } else {
      err.value = '登录失败,请检查网络'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="login">
    <view class="title">通用组织管理平台</view>
    <view class="subtitle">移动工作台</view>
    <view class="form">
      <wd-input v-model="username" placeholder="账号" clearable />
      <wd-input v-model="password" placeholder="密码" type="password" clearable />
      <view v-if="err" class="err">{{ err }}</view>
      <wd-button block type="primary" :loading="loading" @click="submit">登录</wd-button>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.login { padding: 80rpx 48rpx; }
.title { font-size: 44rpx; font-weight: 700; color: #1a2840; }
.subtitle { font-size: 28rpx; color: #5a6a7a; margin-top: 8rpx; margin-bottom: 80rpx; }
.form { display: flex; flex-direction: column; gap: 24rpx; }
.err { color: #e0592a; font-size: 26rpx; }
</style>
