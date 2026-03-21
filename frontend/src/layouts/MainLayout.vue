<template>
  <div class="flex h-screen bg-gradient-to-br from-gray-50 to-gray-100/50">
    <!-- 侧边栏 -->
    <aside
      :class="[
        'flex flex-col bg-white shadow-xl transition-all duration-300 relative',
        isCollapse ? 'w-[72px]' : 'w-[260px]'
      ]"
    >
      <!-- 装饰性渐变条 -->
      <div class="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-blue-600 via-indigo-600 to-purple-600"></div>

      <!-- Logo区域 - 优化 -->
      <div class="h-16 flex items-center px-5 border-b border-gray-100/80">
        <div class="flex items-center gap-3 min-w-0">
          <div class="relative w-10 h-10 rounded-xl bg-gradient-to-br from-blue-600 via-indigo-600 to-blue-700 flex items-center justify-center flex-shrink-0 shadow-lg shadow-blue-500/30">
            <!-- 光晕效果 -->
            <div class="absolute inset-0 rounded-xl bg-gradient-to-br from-blue-400 to-indigo-500 opacity-0 blur-md group-hover:opacity-30 transition-opacity"></div>
            <svg class="w-5 h-5 text-white relative z-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          </div>
          <div v-if="!isCollapse" class="flex flex-col min-w-0">
            <span class="font-bold text-gray-900 truncate text-base">{{ configStore.systemName }}</span>
            <span class="text-xs text-gray-500">Student Management</span>
          </div>
        </div>
      </div>

      <!-- 菜单区域 -->
      <nav class="flex-1 overflow-y-auto py-3 px-3">
        <div class="space-y-0.5">
          <template v-for="(item, index) in menuList" :key="item.path">
            <!-- 分组分隔线 -->
            <div v-if="showSeparator(index)" class="my-2 mx-1 border-t border-gray-200/60"></div>
            <!-- 有子菜单的项（单个子项时直接导航） -->
            <div v-if="item.children && item.children.length > 0">
              <!-- 单个子项：直接导航 -->
              <router-link
                v-if="item.children.length === 1"
                :to="item.children[0].path"
                :class="[
                  'group relative flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200',
                  isCollapse ? 'justify-center' : '',
                  currentRoute === item.children[0].path
                    ? 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white font-semibold shadow-lg shadow-blue-500/30'
                    : 'text-gray-600 hover:bg-gradient-to-r hover:from-gray-50 hover:to-gray-100/50 hover:text-gray-900'
                ]"
              >
                <div v-if="currentRoute === item.children[0].path && !isCollapse" class="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-8 bg-white rounded-r-full"></div>
                <component :is="getIcon(item.icon)" class="w-5 h-5 flex-shrink-0 transition-transform group-hover:scale-110" />
                <span v-if="!isCollapse" class="text-sm font-medium">{{ item.title }}</span>
              </router-link>
              <!-- 多个子项：展开子菜单 -->
              <button
                v-else
                @click="toggleSubmenu(item.path)"
                :class="[
                  'group relative w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-gray-600 hover:bg-gradient-to-r hover:from-gray-50 hover:to-gray-100/50 hover:text-gray-900 transition-all duration-200',
                  isCollapse ? 'justify-center' : '',
                  isSubmenuActive(item) ? 'bg-gradient-to-r from-blue-50 to-indigo-50/50 text-blue-700 font-medium' : ''
                ]"
              >
                <component :is="getIcon(item.icon)" class="w-5 h-5 flex-shrink-0 transition-transform group-hover:scale-110" />
                <span v-if="!isCollapse" class="flex-1 text-left text-sm font-medium">{{ item.title }}</span>
                <svg
                  v-if="!isCollapse"
                  :class="['w-4 h-4 transition-all duration-300 text-gray-400', openSubmenus.includes(item.path) ? 'rotate-180 text-blue-600' : 'group-hover:text-gray-600']"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                </svg>
              </button>
              <!-- 子菜单 - 支持三级菜单（仅多子项时显示） -->
              <div
                v-if="!isCollapse && item.children.length > 1"
                v-show="openSubmenus.includes(item.path)"
                class="mt-1 ml-2 space-y-0.5 border-l-2 border-gray-100 pl-2"
              >
                <template v-for="child in item.children" :key="child.path">
                  <!-- 三级菜单：子项有children -->
                  <div v-if="child.children && child.children.length > 0">
                    <button
                      @click="toggleSubmenu(child.path)"
                      :class="[
                        'group relative w-full flex items-center gap-2 pl-9 pr-3 py-2 rounded-xl text-sm transition-all duration-200',
                        isSubmenuActive(child) ? 'bg-blue-50/50 text-blue-700 font-medium' : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                      ]"
                    >
                      <span class="flex-1 text-left">{{ child.title }}</span>
                      <svg
                        :class="['w-3.5 h-3.5 transition-all duration-300 text-gray-400', openSubmenus.includes(child.path) ? 'rotate-180 text-blue-600' : '']"
                        fill="none" stroke="currentColor" viewBox="0 0 24 24"
                      >
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                      </svg>
                    </button>
                    <!-- 三级子菜单 -->
                    <div
                      v-show="openSubmenus.includes(child.path)"
                      class="mt-0.5 ml-3 space-y-0.5 border-l border-gray-100 pl-2"
                    >
                      <router-link
                        v-for="grandChild in child.children"
                        :key="grandChild.path"
                        :to="grandChild.path"
                        :class="[
                          'relative flex items-center gap-2 pl-6 pr-3 py-2 rounded-lg text-sm transition-all duration-200',
                          currentRoute === grandChild.path
                            ? 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white font-semibold shadow-md shadow-blue-500/20'
                            : 'text-gray-500 hover:bg-gray-50 hover:text-gray-900'
                        ]"
                      >
                        <span>{{ grandChild.title }}</span>
                      </router-link>
                    </div>
                  </div>
                  <!-- 二级菜单：子项无children -->
                  <router-link
                    v-else
                    :to="child.path"
                    :class="[
                      'relative flex items-center gap-3 pl-9 pr-3 py-2.5 rounded-xl text-sm transition-all duration-200',
                      currentRoute === child.path
                        ? 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white font-semibold shadow-lg shadow-blue-500/30'
                        : 'text-gray-600 hover:bg-gradient-to-r hover:from-gray-50 hover:to-gray-100/50 hover:text-gray-900'
                    ]"
                  >
                    <!-- 激活指示器 -->
                    <div v-if="currentRoute === child.path" class="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-6 bg-white rounded-r-full"></div>
                    <span>{{ child.title }}</span>
                  </router-link>
                </template>
              </div>
            </div>

            <!-- 无子菜单的项 - 优化 -->
            <router-link
              v-else
              :to="item.path"
              :class="[
                'group relative flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200',
                isCollapse ? 'justify-center' : '',
                currentRoute === item.path
                  ? 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white font-semibold shadow-lg shadow-blue-500/30'
                  : 'text-gray-600 hover:bg-gradient-to-r hover:from-gray-50 hover:to-gray-100/50 hover:text-gray-900'
              ]"
            >
              <!-- 激活指示器 -->
              <div v-if="currentRoute === item.path && !isCollapse" class="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-8 bg-white rounded-r-full"></div>
              <component :is="getIcon(item.icon)" class="w-5 h-5 flex-shrink-0 transition-transform group-hover:scale-110" />
              <span v-if="!isCollapse" class="flex-1 text-sm font-medium">{{ item.title }}</span>
              <UnreadBadge v-if="!isCollapse && item.path === '/messages'" />
            </router-link>
          </template>
        </div>
      </nav>

      <!-- 底部折叠按钮 - 优化 -->
      <div class="p-3 border-t border-gray-100/80 bg-gradient-to-t from-gray-50/50 to-transparent">
        <button
          @click="toggleCollapse"
          class="group w-full flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl text-gray-500 hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 hover:text-blue-600 transition-all duration-200"
        >
          <svg
            :class="['w-5 h-5 transition-all duration-300 group-hover:scale-110', isCollapse ? 'rotate-180' : '']"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
          </svg>
          <span v-if="!isCollapse" class="text-sm font-medium">收起侧边栏</span>
        </button>
      </div>
    </aside>

    <!-- 主内容区域 -->
    <div class="flex-1 flex flex-col overflow-hidden">
      <!-- 顶部导航 - 优化 -->
      <header class="h-16 bg-white/80 backdrop-blur-md border-b border-gray-200/50 shadow-sm flex items-center justify-between px-6 relative z-40">
        <!-- 左侧：面包屑 -->
        <div class="flex items-center gap-4">
          <Breadcrumb />
        </div>

        <!-- 右侧：用户信息 - 优化 -->
        <div class="flex items-center gap-3">
          <!-- 通知按钮 - 优化 -->
          <button class="group relative p-2.5 rounded-xl text-gray-400 hover:bg-gradient-to-br hover:from-blue-50 hover:to-indigo-50 hover:text-blue-600 transition-all duration-200">
            <svg class="w-5 h-5 transition-transform group-hover:scale-110 group-hover:rotate-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
            <!-- 红点优化 - 带动画 -->
            <span class="absolute top-2 right-2 flex h-2 w-2">
              <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
              <span class="relative inline-flex rounded-full h-2 w-2 bg-red-500 ring-2 ring-white"></span>
            </span>
          </button>

          <!-- 分隔线 -->
          <div class="h-6 w-px bg-gradient-to-b from-transparent via-gray-200 to-transparent mx-1"></div>

          <!-- 用户下拉菜单 - 优化 -->
          <div class="relative" ref="userMenuRef">
            <button
              @click="showUserMenu = !showUserMenu"
              class="group flex items-center gap-2.5 px-3 py-2 rounded-xl hover:bg-gradient-to-r hover:from-gray-50 hover:to-gray-100/50 transition-all duration-200"
            >
              <div class="relative w-9 h-9 rounded-xl bg-gradient-to-br from-blue-600 to-indigo-600 flex items-center justify-center text-white font-semibold text-sm shadow-md shadow-blue-500/30 ring-2 ring-white group-hover:ring-blue-100 transition-all">
                <div class="absolute inset-0 rounded-xl bg-gradient-to-br from-blue-400 to-indigo-500 opacity-0 blur group-hover:opacity-50 transition-opacity"></div>
                <span class="relative z-10">{{ userStore.userName?.charAt(0)?.toUpperCase() || 'U' }}</span>
              </div>
              <div class="hidden sm:block text-left">
                <div class="text-sm font-semibold text-gray-900 group-hover:text-blue-700 transition-colors">{{ userStore.userName }}</div>
                <div class="text-xs text-gray-500">系统管理员</div>
              </div>
              <svg class="w-4 h-4 text-gray-400 hidden sm:block group-hover:text-blue-600 transition-all" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            <!-- 下拉菜单 - 优化 -->
            <Transition
              enter-active-class="transition ease-out duration-150"
              enter-from-class="transform opacity-0 scale-95 -translate-y-2"
              enter-to-class="transform opacity-100 scale-100 translate-y-0"
              leave-active-class="transition ease-in duration-100"
              leave-from-class="transform opacity-100 scale-100 translate-y-0"
              leave-to-class="transform opacity-0 scale-95 -translate-y-2"
            >
              <div
                v-if="showUserMenu"
                class="absolute right-0 mt-3 w-64 bg-white/95 backdrop-blur-xl rounded-2xl shadow-2xl shadow-gray-900/10 ring-1 ring-gray-900/5 py-2 z-50"
              >
                <!-- 用户信息头部 - 优化 -->
                <div class="px-4 py-4 border-b border-gray-100/80 bg-gradient-to-br from-blue-50/50 to-transparent rounded-t-2xl">
                  <div class="flex items-center gap-3">
                    <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-600 to-indigo-600 flex items-center justify-center text-white font-bold text-base shadow-lg shadow-blue-500/30">
                      {{ userStore.userName?.charAt(0)?.toUpperCase() || 'U' }}
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="font-semibold text-gray-900 truncate">{{ userStore.userName }}</div>
                      <div class="text-xs text-gray-500 mt-0.5">系统管理员</div>
                    </div>
                  </div>
                </div>

                <div class="py-2 px-2">
                  <button
                    @click="handleCommand('profile')"
                    class="group w-full flex items-center gap-3 px-3 py-2.5 text-sm text-gray-700 hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 hover:text-blue-700 rounded-xl transition-all duration-200"
                  >
                    <div class="w-8 h-8 rounded-lg bg-gray-100 group-hover:bg-blue-100 flex items-center justify-center text-gray-500 group-hover:text-blue-600 transition-all">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                      </svg>
                    </div>
                    <span class="font-medium">个人资料</span>
                  </button>
                  <button
                    @click="handleCommand('password')"
                    class="group w-full flex items-center gap-3 px-3 py-2.5 text-sm text-gray-700 hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 hover:text-blue-700 rounded-xl transition-all duration-200"
                  >
                    <div class="w-8 h-8 rounded-lg bg-gray-100 group-hover:bg-blue-100 flex items-center justify-center text-gray-500 group-hover:text-blue-600 transition-all">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
                      </svg>
                    </div>
                    <span class="font-medium">修改密码</span>
                  </button>
                </div>

                <div class="border-t border-gray-100/80 py-2 px-2">
                  <button
                    @click="handleCommand('logout')"
                    class="group w-full flex items-center gap-3 px-3 py-2.5 text-sm text-red-600 hover:bg-gradient-to-r hover:from-red-50 hover:to-pink-50 rounded-xl transition-all duration-200"
                  >
                    <div class="w-8 h-8 rounded-lg bg-red-50 group-hover:bg-red-100 flex items-center justify-center text-red-500 group-hover:text-red-600 transition-all">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                      </svg>
                    </div>
                    <span class="font-semibold">退出登录</span>
                  </button>
                </div>
              </div>
            </Transition>
          </div>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="flex-1 overflow-auto">
        <div class="p-6 min-h-full">
          <router-view v-slot="{ Component }">
            <Transition name="fade" mode="out-in">
              <component :is="Component" />
            </Transition>
          </router-view>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useConfigStore } from '@/stores/config'
import { generateMenuFromRoutes, sortMenuItems } from '@/utils/menu-generator'
import Breadcrumb from '@/components/common/Breadcrumb.vue'
import UnreadBadge from '@/components/message/UnreadBadge.vue'
import type { MenuItem } from '@/types/common'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const configStore = useConfigStore()

const isCollapse = ref(false)
const showUserMenu = ref(false)
const userMenuRef = ref<HTMLElement | null>(null)
const openSubmenus = ref<string[]>([])
const userStore = authStore

// 当前路由
const currentRoute = computed(() => route.path)

// 判断子菜单是否有活跃项
const isSubmenuActive = (item: MenuItem): boolean => {
  if (!item.children) return false
  return item.children.some(child => {
    // 直接匹配
    if (currentRoute.value === child.path) return true
    // 递归检查子菜单
    if (child.children && child.children.length > 0) {
      return isSubmenuActive(child)
    }
    return false
  })
}

// 判断是否显示分组分隔线
const showSeparator = (index: number): boolean => {
  if (index === 0) return false
  const prev = menuList.value[index - 1]
  const curr = menuList.value[index]
  return !!(prev.group && curr.group && prev.group !== curr.group)
}

// 从路由自动生成菜单
const menuList = computed<MenuItem[]>(() => {
  const mainRoute = router.getRoutes().find(r => r.path === '/')
  if (!mainRoute || !mainRoute.children) {
    return []
  }
  const menus = generateMenuFromRoutes(
    mainRoute.children,
    {
      authCheck: (permission) => authStore.hasPermission(permission || ''),
      userInfo: authStore.user
    }
  )
  return sortMenuItems(menus)
})

// 图标映射
const iconComponents: Record<string, any> = {
  House: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' })
  ]),
  UserFilled: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z' })
  ]),
  School: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M12 14l9-5-9-5-9 5 9 5z' }),
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M12 14l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z' })
  ]),
  OfficeBuilding: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4' })
  ]),
  DataAnalysis: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z' })
  ]),
  Setting: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z' }),
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M15 12a3 3 0 11-6 0 3 3 0 016 0z' })
  ]),
  Reading: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253' })
  ]),
  Bell: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9' })
  ]),
  Management: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z' })
  ]),
  // 权限管理图标
  Lock: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z' })
  ]),
  // 任务管理图标
  Tickets: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z' })
  ]),
  // 量化检查图标
  DocumentChecked: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z' })
  ]),
  // 申诉管理图标
  ChatDotRound: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z' })
  ]),
  // 评级统计图标
  TrendCharts: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M13 7h8m0 0v8m0-8l-8 8-4-4-6 6' })
  ]),
  // 荣誉徽章图标
  Medal: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z' })
  ]),
  // 班级荣誉图标
  Trophy: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M12 15l-2 5-2-1 1-4m6 0l2 5 2-1-1-4m-6-3a3 3 0 110-6 3 3 0 010 6zm-7 1h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v5a2 2 0 002 2z' })
  ]),
  // 笔记本图标
  Notebook: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01' })
  ]),
  // 组织架构图标（树形层级）
  Hierarchy: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('rect', { x: '9', y: '2', width: '6', height: '4', rx: '1', 'stroke-width': '2' }),
    h('rect', { x: '2', y: '18', width: '6', height: '4', rx: '1', 'stroke-width': '2' }),
    h('rect', { x: '16', y: '18', width: '6', height: '4', rx: '1', 'stroke-width': '2' }),
    h('path', { 'stroke-width': '2', d: 'M12 6v4m0 0H5m7 0h7m-14 0v8m14-8v8' })
  ]),
  // 地图标记图标
  MapPin: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z' }),
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M15 11a3 3 0 11-6 0 3 3 0 016 0z' })
  ]),
  // 带勾清单图标
  ClipboardCheck: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4' })
  ]),
  // 毕业帽图标
  GraduationCap: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M12 3L1 9l11 6 9-4.91V17M5 13.18v4L12 21l7-3.82v-4' })
  ]),
  // 包裹图标
  Package: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4' })
  ]),
  // 盾牌图标
  Shield: () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z' })
  ]),
}

// 默认图标
const DefaultIcon = () => h('svg', { class: 'w-5 h-5', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
  h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M4 6h16M4 12h16M4 18h16' })
])

const getIcon = (iconName?: string) => {
  if (!iconName) return DefaultIcon
  return iconComponents[iconName] || DefaultIcon
}

// 切换子菜单
const toggleSubmenu = (path: string) => {
  const index = openSubmenus.value.indexOf(path)
  if (index > -1) {
    openSubmenus.value.splice(index, 1)
  } else {
    openSubmenus.value.push(path)
  }
}

// 切换侧边栏折叠状态
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

// 点击外部关闭菜单
const handleClickOutside = (event: MouseEvent) => {
  if (userMenuRef.value && !userMenuRef.value.contains(event.target as Node)) {
    showUserMenu.value = false
  }
}

// 处理用户菜单命令
const handleCommand = async (command: string) => {
  showUserMenu.value = false
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'password':
      router.push('/profile')
      break
    case 'logout':
      await ElMessageBox.confirm('确认退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await authStore.logoutAction()
      router.push('/login')
      break
  }
}

onMounted(async () => {
  document.addEventListener('click', handleClickOutside)
  await configStore.refreshConfig()

  // 自动展开当前路由所在的子菜单（支持三级菜单）
  menuList.value.forEach(item => {
    if (item.children) {
      // 检查二级菜单
      const directMatch = item.children.some(child => currentRoute.value === child.path)
      if (directMatch) {
        if (!openSubmenus.value.includes(item.path)) {
          openSubmenus.value.push(item.path)
        }
        return
      }
      // 检查三级菜单
      item.children.forEach(child => {
        if (child.children && child.children.some(grandChild => currentRoute.value === grandChild.path)) {
          // 展开一级菜单
          if (!openSubmenus.value.includes(item.path)) {
            openSubmenus.value.push(item.path)
          }
          // 展开二级菜单
          if (!openSubmenus.value.includes(child.path)) {
            openSubmenus.value.push(child.path)
          }
        }
      })
    }
  })
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
