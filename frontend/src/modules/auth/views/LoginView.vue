<template>
  <div class="min-h-screen flex">
    <!-- 左侧品牌区域 -->
    <div class="hidden lg:flex lg:w-1/2 xl:w-3/5 bg-gradient-to-br from-blue-600 via-blue-700 to-indigo-800 relative overflow-hidden">
      <!-- 背景装饰 -->
      <div class="absolute inset-0">
        <div class="absolute top-20 left-20 w-72 h-72 bg-white/5 rounded-full blur-3xl"></div>
        <div class="absolute bottom-20 right-20 w-96 h-96 bg-white/5 rounded-full blur-3xl"></div>
      </div>

      <!-- 品牌内容 -->
      <div class="relative z-10 flex flex-col justify-center px-12 xl:px-20">
        <div class="mb-12">
          <div class="flex items-center gap-4 mb-8">
            <div class="w-14 h-14 rounded-2xl bg-white/10 backdrop-blur flex items-center justify-center">
              <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
            </div>
          </div>
          <h1 class="text-4xl xl:text-5xl font-bold text-white leading-tight mb-4">{{ configStore.systemName }}</h1>
          <p class="text-xl text-white/70">现代化教育信息管理平台</p>
        </div>

        <!-- 特性列表 -->
        <div class="space-y-5">
          <div class="flex items-center gap-4 text-white/80">
            <div class="w-10 h-10 rounded-xl bg-white/10 flex items-center justify-center flex-shrink-0">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
            </div>
            <span class="text-lg">学生信息一站式管理</span>
          </div>
          <div class="flex items-center gap-4 text-white/80">
            <div class="w-10 h-10 rounded-xl bg-white/10 flex items-center justify-center flex-shrink-0">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
            </div>
            <span class="text-lg">量化考核智能分析</span>
          </div>
          <div class="flex items-center gap-4 text-white/80">
            <div class="w-10 h-10 rounded-xl bg-white/10 flex items-center justify-center flex-shrink-0">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
            </div>
            <span class="text-lg">宿舍资源可视化管理</span>
          </div>
        </div>
      </div>

      <!-- 底部版权 -->
      <div class="absolute bottom-8 left-12 xl:left-20 text-white/40 text-sm">
        {{ configStore.systemCopyright }}
      </div>
    </div>

    <!-- 右侧登录区域 -->
    <div class="w-full lg:w-1/2 xl:w-2/5 flex items-center justify-center p-8 bg-gray-50">
      <div class="w-full max-w-[400px]">
        <!-- 移动端 Logo -->
        <div class="lg:hidden text-center mb-10">
          <div class="inline-flex items-center gap-3 mb-4">
            <div class="w-11 h-11 rounded-xl bg-blue-600 flex items-center justify-center">
              <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
            </div>
            <span class="text-xl font-bold text-gray-900">{{ configStore.systemName }}</span>
          </div>
        </div>

        <!-- 欢迎文字 -->
        <div class="mb-8">
          <h2 class="text-2xl font-semibold text-gray-900">登录</h2>
          <p class="text-gray-500 mt-2">请输入您的账号信息</p>
        </div>

        <!-- 登录表单 -->
        <form @submit.prevent="handleLogin" class="space-y-5">
          <!-- 用户名 -->
          <div class="space-y-1.5">
            <label class="text-sm font-medium text-gray-700">用户名</label>
            <input
              v-model="loginForm.username"
              type="text"
              placeholder="请输入用户名"
              class="w-full h-11 px-4 rounded-lg border border-gray-300 bg-white text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
              :class="{ 'border-red-500 focus:ring-red-500': errors.username }"
            />
            <p v-if="errors.username" class="text-sm text-red-500">{{ errors.username }}</p>
          </div>

          <!-- 密码 -->
          <div class="space-y-1.5">
            <label class="text-sm font-medium text-gray-700">密码</label>
            <div class="relative">
              <input
                v-model="loginForm.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请输入密码"
                class="w-full h-11 px-4 pr-11 rounded-lg border border-gray-300 bg-white text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                :class="{ 'border-red-500 focus:ring-red-500': errors.password }"
              />
              <button
                type="button"
                @click="showPassword = !showPassword"
                class="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600 transition-colors"
              >
                <svg v-if="!showPassword" class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                <svg v-else class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                </svg>
              </button>
            </div>
            <p v-if="errors.password" class="text-sm text-red-500">{{ errors.password }}</p>
          </div>

          <!-- 记住我 -->
          <div class="flex items-center">
            <label class="flex items-center gap-2 cursor-pointer select-none">
              <input
                v-model="loginForm.rememberMe"
                type="checkbox"
                class="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 focus:ring-offset-0"
              />
              <span class="text-sm text-gray-600">记住登录状态</span>
            </label>
          </div>

          <!-- 登录按钮 -->
          <button
            type="submit"
            :disabled="loading"
            class="w-full h-11 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-medium transition-colors flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed mt-6"
          >
            <svg
              v-if="loading"
              class="animate-spin h-5 w-5"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
            >
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
            <span>{{ loading ? '登录中...' : '登录' }}</span>
          </button>
        </form>

        <!-- 底部版权（移动端） -->
        <div class="lg:hidden mt-12 text-center text-sm text-gray-400">
          <p>{{ configStore.systemCopyright }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useConfigStore } from '@/stores/config'
import type { LoginRequest } from '@/types/auth'

const router = useRouter()
const authStore = useAuthStore()
const configStore = useConfigStore()

// 状态
const loading = ref(false)
const showPassword = ref(false)

// 登录表单
const loginForm = reactive<LoginRequest>({
  username: '',
  password: '',
  rememberMe: false
})

// 错误信息
const errors = reactive({
  username: '',
  password: ''
})

// 表单验证
const validateForm = (): boolean => {
  errors.username = ''
  errors.password = ''

  let isValid = true

  if (!loginForm.username) {
    errors.username = '请输入用户名'
    isValid = false
  } else if (loginForm.username.length < 2 || loginForm.username.length > 20) {
    errors.username = '用户名长度在 2 到 20 个字符'
    isValid = false
  }

  if (!loginForm.password) {
    errors.password = '请输入密码'
    isValid = false
  } else if (loginForm.password.length < configStore.passwordMinLength) {
    errors.password = `密码长度不能少于 ${configStore.passwordMinLength} 个字符`
    isValid = false
  }

  return isValid
}

// 处理登录
const handleLogin = async () => {
  if (!validateForm()) return

  try {
    loading.value = true
    await authStore.loginAction(loginForm)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error: any) {
    ElMessage.error(error.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

// 组件挂载时加载系统配置
onMounted(async () => {
  await configStore.refreshConfig()
})
</script>
