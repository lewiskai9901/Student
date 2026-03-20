<template>
  <div class="min-h-screen relative">
    <!-- 全屏模式背景 -->
    <template v-if="isFullScreen">
      <!-- 全屏背景容器 -->
      <div class="absolute inset-0 overflow-hidden" :style="fullScreenBackgroundStyle">
        <!-- 背景视频 -->
        <video
          v-if="loginConfig.backgroundMode === 'video' && loginConfig.backgroundVideo"
          :src="loginConfig.backgroundVideo"
          class="absolute inset-0 w-full h-full object-cover"
          autoplay
          loop
          muted
          playsinline
        />

        <!-- 背景轮播 -->
        <el-carousel
          v-else-if="useCarousel"
          :interval="loginConfig.backgroundCarousel.interval * 1000"
          :arrow="'never'"
          :indicator-position="'none'"
          :autoplay="true"
          class="absolute inset-0 w-full h-full carousel-fullscreen"
          :type="loginConfig.backgroundCarousel.transition === 'slide' ? 'card' : undefined"
        >
          <el-carousel-item
            v-for="item in loginConfig.backgroundCarousel.items"
            :key="item.id"
            class="w-full h-full"
          >
            <video
              v-if="item.type === 'video'"
              :src="item.url"
              class="w-full h-full object-cover"
              autoplay
              loop
              muted
              playsinline
            />
            <img
              v-else
              :src="item.url"
              class="w-full h-full object-cover"
            />
          </el-carousel-item>
        </el-carousel>

        <!-- 背景图片/GIF (非轮播模式) -->
        <img
          v-else-if="loginConfig.backgroundMode === 'image' && loginConfig.backgroundImage"
          :src="loginConfig.backgroundImage"
          class="absolute inset-0 w-full h-full object-cover"
        />

        <!-- 内容遮罩 -->
        <div
          v-if="loginConfig.backgroundMode !== 'gradient'"
          class="absolute inset-0"
          :style="overlayStyle"
        ></div>

        <!-- 装饰图片 -->
        <img
          v-for="decImage in loginConfig.decorationImages"
          :key="decImage.id"
          :src="decImage.url"
          :style="getDecorationStyle(decImage)"
          class="decoration-image"
        />
      </div>

      <!-- 全屏模式内容布局 -->
      <div class="relative z-10 min-h-screen flex">
        <!-- 左侧品牌区域 -->
        <div class="hidden lg:block lg:w-1/2 xl:w-3/5 relative">
          <!-- 品牌内容 - 绝对定位支持自由拖拽 -->
          <div
            class="flex flex-col items-center px-8"
            :style="textPositionStyle"
          >
            <!-- Logo/标题 -->
            <div v-if="loginConfig.showLogo && loginConfig.showTitle" class="mb-6">
              <img
                v-if="configStore.systemLogo && configStore.systemLogo !== '/logo.png'"
                :src="configStore.systemLogo"
                class="h-16 w-auto"
                alt="Logo"
              />
              <div v-else class="tracking-wide drop-shadow-lg" :style="titleStyle" v-html="formattedTitle"></div>
            </div>

            <!-- 标语 -->
            <p
              v-if="loginConfig.showSubtitle"
              class="text-xl text-white/90 text-center max-w-md drop-shadow-lg"
              v-html="formattedSubtitle"
            ></p>
          </div>

          <!-- 底部版权 -->
          <div v-if="loginConfig.showCopyright" class="absolute bottom-6 left-0 right-0 text-center text-white/60 text-sm drop-shadow">
            {{ copyrightText }}
          </div>
        </div>

        <!-- 右侧登录区域 - 全屏模式 -->
        <div class="w-full lg:w-1/2 xl:w-2/5 flex items-center justify-center p-8">
          <div
            class="w-full max-w-[420px] p-8"
            :class="formContainerClass"
            :style="formContainerStyle"
          >
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
              <h2 :class="['text-2xl font-semibold', isMinimalStyle ? 'text-white' : 'text-gray-900']">登录</h2>
              <p :class="[isMinimalStyle ? 'text-white/70' : 'text-gray-500', 'mt-2']">请输入您的账号信息</p>
            </div>

            <!-- 登录表单 -->
            <form @submit.prevent="handleLogin" class="space-y-5">
              <!-- 账号 -->
              <div class="space-y-1.5">
                <label :class="['text-sm font-medium', isMinimalStyle ? 'text-white/90' : 'text-gray-700']">账号</label>
                <input
                  v-model="loginForm.username"
                  type="text"
                  placeholder="请输入账号"
                  :class="[
                    'w-full h-11 px-4 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all',
                    isMinimalStyle
                      ? 'bg-white/10 border border-white/30 text-white placeholder:text-white/50'
                      : 'border border-gray-300 bg-white/90 text-gray-900 placeholder:text-gray-400',
                    errors.username ? 'border-red-500 focus:ring-red-500' : ''
                  ]"
                />
                <p v-if="errors.username" class="text-sm text-red-500">{{ errors.username }}</p>
              </div>

              <!-- 密码 -->
              <div class="space-y-1.5">
                <label :class="['text-sm font-medium', isMinimalStyle ? 'text-white/90' : 'text-gray-700']">密码</label>
                <div class="relative">
                  <input
                    v-model="loginForm.password"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="请输入密码"
                    :class="[
                      'w-full h-11 px-4 pr-11 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all',
                      isMinimalStyle
                        ? 'bg-white/10 border border-white/30 text-white placeholder:text-white/50'
                        : 'border border-gray-300 bg-white/90 text-gray-900 placeholder:text-gray-400',
                      errors.password ? 'border-red-500 focus:ring-red-500' : ''
                    ]"
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
                  <span :class="['text-sm', isMinimalStyle ? 'text-white/80' : 'text-gray-600']">记住登录状态</span>
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
            <div v-if="loginConfig.showCopyright" class="lg:hidden mt-8 text-center text-sm" :class="isMinimalStyle ? 'text-white/60' : 'text-gray-400'">
              <p>{{ copyrightText }}</p>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 半屏模式 (原有布局) -->
    <template v-else>
      <div class="min-h-screen flex">
        <!-- 左侧品牌区域 -->
        <div
          class="hidden lg:flex lg:w-1/2 xl:w-3/5 relative overflow-hidden"
          :style="brandBackgroundStyle"
        >
          <!-- 背景视频 -->
          <video
            v-if="loginConfig.backgroundMode === 'video' && loginConfig.backgroundVideo"
            :src="loginConfig.backgroundVideo"
            class="absolute inset-0 w-full h-full object-cover"
            autoplay
            loop
            muted
            playsinline
          />

          <!-- 背景轮播 -->
          <el-carousel
            v-else-if="useCarousel"
            :interval="loginConfig.backgroundCarousel.interval * 1000"
            :arrow="'never'"
            :indicator-position="'none'"
            :autoplay="true"
            class="absolute inset-0 w-full h-full carousel-fullscreen"
            :type="loginConfig.backgroundCarousel.transition === 'slide' ? 'card' : undefined"
          >
            <el-carousel-item
              v-for="item in loginConfig.backgroundCarousel.items"
              :key="item.id"
              class="w-full h-full"
            >
              <video
                v-if="item.type === 'video'"
                :src="item.url"
                class="w-full h-full object-cover"
                autoplay
                loop
                muted
                playsinline
              />
              <img
                v-else
                :src="item.url"
                class="w-full h-full object-cover"
              />
            </el-carousel-item>
          </el-carousel>

          <!-- 背景图片/GIF (非轮播模式) -->
          <img
            v-else-if="loginConfig.backgroundMode === 'image' && loginConfig.backgroundImage"
            :src="loginConfig.backgroundImage"
            class="absolute inset-0 w-full h-full object-cover"
          />

          <!-- 内容遮罩 -->
          <div
            v-if="loginConfig.backgroundMode !== 'gradient'"
            class="absolute inset-0"
            :style="overlayStyle"
          ></div>

          <!-- 装饰图片 -->
          <img
            v-for="decImage in loginConfig.decorationImages"
            :key="decImage.id"
            :src="decImage.url"
            :style="getDecorationStyle(decImage)"
            class="decoration-image"
          />

          <!-- 品牌内容 - 绝对定位支持自由拖拽 -->
          <div
            class="z-10 flex flex-col items-center px-8"
            :style="textPositionStyle"
          >
            <!-- Logo/标题 -->
            <div v-if="loginConfig.showLogo && loginConfig.showTitle" class="mb-6">
              <img
                v-if="configStore.systemLogo && configStore.systemLogo !== '/logo.png'"
                :src="configStore.systemLogo"
                class="h-16 w-auto"
                alt="Logo"
              />
              <div v-else class="tracking-wide" :style="titleStyle" v-html="formattedTitle"></div>
            </div>

            <!-- 标语 - 支持高亮关键词 -->
            <p
              v-if="loginConfig.showSubtitle"
              class="text-xl text-white/90 text-center max-w-md"
              v-html="formattedSubtitle"
            ></p>
          </div>

          <!-- 底部版权 -->
          <div v-if="loginConfig.showCopyright" class="absolute bottom-6 left-0 right-0 text-center text-white/40 text-sm">
            {{ copyrightText }}
          </div>
        </div>

        <!-- 右侧登录区域 -->
        <div
          class="w-full lg:w-1/2 xl:w-2/5 flex items-center justify-center p-8"
          :style="formBackgroundStyle"
        >
          <div class="w-full max-w-[400px]" :style="formContainerStyle" :class="formContainerClass">
            <!-- 移动端 Logo -->
            <div class="lg:hidden text-center mb-10">
              <div class="inline-flex items-center gap-3 mb-4">
                <div class="w-11 h-11 rounded-xl bg-blue-600 flex items-center justify-center">
                  <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                  </svg>
                </div>
                <span :class="['text-xl font-bold', isMinimalStyle ? 'text-white' : 'text-gray-900']">{{ configStore.systemName }}</span>
              </div>
            </div>

            <!-- 欢迎文字 -->
            <div class="mb-8">
              <h2 :class="['text-2xl font-semibold', isMinimalStyle ? 'text-white' : 'text-gray-900']">登录</h2>
              <p :class="[isMinimalStyle ? 'text-white/70' : 'text-gray-500', 'mt-2']">请输入您的账号信息</p>
            </div>

            <!-- 登录表单 -->
            <form @submit.prevent="handleLogin" class="space-y-5">
              <!-- 账号 -->
              <div class="space-y-1.5">
                <label :class="['text-sm font-medium', isMinimalStyle ? 'text-white/90' : 'text-gray-700']">账号</label>
                <input
                  v-model="loginForm.username"
                  type="text"
                  placeholder="请输入账号"
                  :class="[
                    'w-full h-11 px-4 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all',
                    isMinimalStyle
                      ? 'bg-white/10 border border-white/30 text-white placeholder:text-white/50'
                      : 'border border-gray-300 bg-white text-gray-900 placeholder:text-gray-400',
                    errors.username ? 'border-red-500 focus:ring-red-500' : ''
                  ]"
                />
                <p v-if="errors.username" class="text-sm text-red-500">{{ errors.username }}</p>
              </div>

              <!-- 密码 -->
              <div class="space-y-1.5">
                <label :class="['text-sm font-medium', isMinimalStyle ? 'text-white/90' : 'text-gray-700']">密码</label>
                <div class="relative">
                  <input
                    v-model="loginForm.password"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="请输入密码"
                    :class="[
                      'w-full h-11 px-4 pr-11 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all',
                      isMinimalStyle
                        ? 'bg-white/10 border border-white/30 text-white placeholder:text-white/50'
                        : 'border border-gray-300 bg-white text-gray-900 placeholder:text-gray-400',
                      errors.password ? 'border-red-500 focus:ring-red-500' : ''
                    ]"
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
                  <span :class="['text-sm', isMinimalStyle ? 'text-white/80' : 'text-gray-600']">记住登录状态</span>
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
            <div v-if="loginConfig.showCopyright" class="lg:hidden mt-12 text-center text-sm" :class="isMinimalStyle ? 'text-white/60' : 'text-gray-400'">
              <p>{{ copyrightText }}</p>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useConfigStore } from '@/stores/config'
import type { LoginRequest } from '@/types/auth'
import type { FeatureIcon, CustomFont, DecorationImage } from '@/types/loginCustomization'
import { getFontFamily, getFontWeight } from '@/types/loginCustomization'
import {
  Users,
  BarChart3,
  Building2,
  BookOpenCheck,
  Shield,
  Clock,
  Star,
  Check
} from 'lucide-vue-next'

const router = useRouter()
const authStore = useAuthStore()
const configStore = useConfigStore()

// 登录页配置
const loginConfig = computed(() => configStore.loginConfig)

// 是否全屏模式
const isFullScreen = computed(() => loginConfig.value.displayMode === 'fullScreen')

// 全屏模式背景样式
const fullScreenBackgroundStyle = computed(() => {
  if (loginConfig.value.backgroundMode === 'gradient') {
    return {
      background: `linear-gradient(to bottom right, ${loginConfig.value.gradientFrom}, ${loginConfig.value.gradientVia}, ${loginConfig.value.gradientTo})`
    }
  }
  return {
    background: '#1f2937'
  }
})

// 全屏模式表单样式
const fullScreenFormStyle = computed(() => {
  const opacity = loginConfig.value.formBgOpacity / 100
  return {
    backgroundColor: `rgba(255, 255, 255, ${opacity})`,
    boxShadow: opacity < 1 ? '0 25px 50px -12px rgba(0, 0, 0, 0.25)' : undefined
  }
})

// 品牌区域背景样式（半屏模式）
const brandBackgroundStyle = computed(() => {
  if (loginConfig.value.backgroundMode === 'gradient') {
    return {
      background: `linear-gradient(to bottom right, ${loginConfig.value.gradientFrom}, ${loginConfig.value.gradientVia}, ${loginConfig.value.gradientTo})`
    }
  }
  return {
    background: '#1f2937'
  }
})

// 遮罩样式
const overlayStyle = computed(() => ({
  backgroundColor: `rgba(0, 0, 0, ${loginConfig.value.overlayOpacity / 100})`
}))

// 文字位置样式 - 支持自由拖拽定位
const textPositionStyle = computed(() => ({
  position: 'absolute' as const,
  left: `${loginConfig.value.textPositionX}%`,
  top: `${loginConfig.value.textPositionY}%`,
  transform: 'translate(-50%, -50%)'
}))

// 表单背景样式（半屏模式）
const formBackgroundStyle = computed(() => {
  const opacity = loginConfig.value.formBgOpacity / 100
  return {
    backgroundColor: `rgba(249, 250, 251, ${opacity})`
  }
})

// 格式化标语 - 支持用 **关键词** 语法高亮
const formattedSubtitle = computed(() => {
  const text = loginConfig.value.subtitle || ''
  return text.replace(/\*\*(.+?)\*\*/g, '<span class="text-blue-400 font-semibold">$1</span>')
})

// 格式化标题 - 支持 \n 换行
const formattedTitle = computed(() => {
  const text = loginConfig.value.title || configStore.systemName || ''
  // 将 \n 转换为 <br> 标签
  return text.replace(/\\n/g, '<br>')
})

// 获取自定义字体的fontFamily
const getCustomFontFamily = (fontId: string): string | null => {
  if (!fontId) return null
  const font = loginConfig.value.customFonts.find(f => f.id === fontId)
  return font?.fontFamily || null
}

// 标题样式
const titleStyle = computed(() => {
  // 优先使用自定义字体
  const customFontFamily = getCustomFontFamily(loginConfig.value.titleCustomFontId)

  const style: Record<string, string> = {
    fontFamily: customFontFamily || getFontFamily(loginConfig.value.titleFontFamily),
    fontSize: `${loginConfig.value.titleFontSize}px`,
    fontWeight: String(getFontWeight(loginConfig.value.titleFontWeight))
  }

  if (loginConfig.value.titleTextShadow) {
    style.textShadow = '0 4px 8px rgba(0, 0, 0, 0.3)'
  }

  if (loginConfig.value.titleGradient) {
    style.background = `linear-gradient(135deg, ${loginConfig.value.titleGradientFrom}, ${loginConfig.value.titleGradientTo})`
    style.WebkitBackgroundClip = 'text'
    style.WebkitTextFillColor = 'transparent'
    style.backgroundClip = 'text'
  } else {
    style.color = 'white'
  }

  return style
})

// 装饰图片样式
const getDecorationStyle = (image: DecorationImage) => ({
  position: 'absolute' as const,
  left: `${image.positionX}%`,
  top: `${image.positionY}%`,
  width: `${image.width}%`,
  opacity: image.opacity / 100,
  transform: `translate(-50%, -50%) rotate(${image.rotation}deg)`,
  pointerEvents: 'none' as const,
  zIndex: 5
})

// 是否使用轮播背景
const useCarousel = computed(() =>
  loginConfig.value.backgroundCarousel.enabled &&
  loginConfig.value.backgroundMode === 'image' &&
  loginConfig.value.backgroundCarousel.items.length > 1
)

// 当前轮播索引
const carouselIndex = ref(0)

// 版权文字
const copyrightText = computed(() => {
  return loginConfig.value.copyrightText || configStore.systemCopyright
})

// 表单容器样式
const formContainerClass = computed(() => {
  switch (loginConfig.value.formStyle) {
    case 'classic':
      return 'form-classic'
    case 'modern':
      return 'form-modern'
    case 'minimal':
      return 'form-minimal'
    default:
      return 'form-modern'
  }
})

// 表单容器内联样式
const formContainerStyle = computed(() => {
  const opacity = loginConfig.value.formBgOpacity / 100

  switch (loginConfig.value.formStyle) {
    case 'classic':
      return {
        backgroundColor: `rgba(255, 255, 255, ${opacity})`,
        borderRadius: '12px',
        boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)'
      }
    case 'modern':
      return {
        backgroundColor: `rgba(255, 255, 255, ${opacity * 0.85})`,
        backdropFilter: 'blur(20px)',
        borderRadius: '24px',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
        border: '1px solid rgba(255, 255, 255, 0.2)'
      }
    case 'minimal':
      return {
        backgroundColor: 'transparent',
        border: 'none'
      }
    default:
      return {}
  }
})

// 是否使用极简模式
const isMinimalStyle = computed(() => loginConfig.value.formStyle === 'minimal')

// 获取特性图标组件（保留用于其他地方）
const getFeatureIcon = (icon: FeatureIcon) => {
  const iconMap: Record<FeatureIcon, any> = {
    users: Users,
    chart: BarChart3,
    building: Building2,
    book: BookOpenCheck,
    shield: Shield,
    clock: Clock,
    star: Star,
    check: Check
  }
  return iconMap[icon] || Star
}

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
    errors.username = '请输入账号'
    isValid = false
  } else if (loginForm.username.length < 2 || loginForm.username.length > 20) {
    errors.username = '账号长度在 2 到 20 个字符'
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
    ElMessage.error(error.message || '登录失败，请检查账号和密码')
  } finally {
    loading.value = false
  }
}

// 加载自定义字体
const loadCustomFonts = async (fonts: CustomFont[]) => {
  for (const font of fonts) {
    try {
      const fontFace = new FontFace(font.fontFamily, `url(${font.url})`)
      await fontFace.load()
      document.fonts.add(fontFace)
    } catch (error) {
      console.error('加载字体失败:', font.name, error)
    }
  }
}

// 组件挂载时加载系统配置
onMounted(async () => {
  await configStore.refreshConfig()
  // 加载自定义字体
  if (loginConfig.value.customFonts.length > 0) {
    await loadCustomFonts(loginConfig.value.customFonts)
  }
})
</script>

<style scoped>
/* 轮播组件全屏样式 */
.carousel-fullscreen {
  width: 100%;
  height: 100%;
}

.carousel-fullscreen :deep(.el-carousel__container) {
  height: 100%;
}

.carousel-fullscreen :deep(.el-carousel__item) {
  width: 100%;
  height: 100%;
}

/* 装饰图片样式 */
.decoration-image {
  max-width: none;
  height: auto;
}
</style>
