<template>
  <div class="space-y-5">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold text-gray-900">{{ getGreeting() }}，{{ authStore.userName || '用户' }}</h1>
        <p class="mt-0.5 text-sm text-gray-500">{{ currentDate }} {{ currentTime }}</p>
      </div>
      <span class="inline-flex items-center gap-1.5 rounded-full bg-green-50 px-3 py-1 text-xs font-medium text-green-700">
        <span class="h-1.5 w-1.5 rounded-full bg-green-500"></span>
        {{ t('dashboard.systemNormal') }}
      </span>
    </div>

    <!-- Organization stats bar (通用核心) -->
    <div class="rounded-lg border border-gray-200 bg-white px-5 py-4">
      <div class="mb-2 text-sm font-medium text-gray-500">组织概览</div>
      <div class="flex items-center gap-0 divide-x divide-gray-200 text-center">
        <div class="flex-1 cursor-pointer px-3 transition-colors hover:bg-gray-50" @click="goTo('/organization/units')">
          <div class="text-2xl font-semibold text-gray-900">{{ org.orgUnitCount }}</div>
          <div class="mt-0.5 text-xs text-gray-500">组织单元</div>
        </div>
        <div class="flex-1 px-3">
          <div class="text-2xl font-semibold text-gray-900">{{ system.totalUsers }}</div>
          <div class="mt-0.5 text-xs text-gray-500">用户总数</div>
        </div>
        <div class="flex-1 px-3">
          <div class="text-2xl font-semibold text-gray-900">{{ system.todayLoginCount }}</div>
          <div class="mt-0.5 text-xs text-gray-500">今日登录</div>
        </div>
      </div>
    </div>

    <!-- 行业整行卡 (插件经 registerDashboardCards 注册, 数据来自对应 overview 分区) -->
    <component
      v-for="card in fullCards"
      :key="card.code"
      :is="card.comp"
      :data="overviewExtra[card.sectionKey]"
    />

    <!-- Middle row: 行业半宽卡 + Inspection (通用核心) -->
    <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
      <component
        v-for="card in halfCards"
        :key="card.code"
        :is="card.comp"
        :data="overviewExtra[card.sectionKey]"
      />

      <!-- Inspection card -->
      <div class="rounded-lg border border-gray-200 bg-white px-5 py-4">
        <div class="mb-3 text-sm font-medium text-gray-500">检查平台</div>
        <div class="flex items-baseline gap-6 text-sm">
          <div>
            <span class="text-gray-500">进行中项目</span>
            <span class="ml-1.5 text-lg font-semibold text-gray-900">{{ inspection.activeProjectCount }}</span>
          </div>
          <span class="text-gray-300">|</span>
          <div>
            <span class="text-gray-500">待处理任务</span>
            <span class="ml-1.5 text-lg font-semibold" :class="inspection.pendingTaskCount > 0 ? 'text-blue-600' : 'text-gray-900'">{{ inspection.pendingTaskCount }}</span>
          </div>
          <span class="text-gray-300">|</span>
          <div>
            <span class="text-gray-500">待整改</span>
            <span class="ml-1.5 text-lg font-semibold" :class="inspection.correctiveOpenCount > 0 ? 'text-red-600' : 'text-gray-900'">{{ inspection.correctiveOpenCount }}</span>
          </div>
        </div>
        <!-- Quick links -->
        <div class="mt-4 flex gap-2">
          <button
            @click="goTo('/inspection/projects')"
            class="rounded-md bg-gray-50 px-3 py-1.5 text-xs font-medium text-gray-600 transition-colors hover:bg-gray-100"
          >检查项目</button>
          <button
            @click="goTo('/inspection/tasks')"
            class="rounded-md bg-gray-50 px-3 py-1.5 text-xs font-medium text-gray-600 transition-colors hover:bg-gray-100"
          >检查任务</button>
          <button
            @click="goTo('/inspection/templates')"
            class="rounded-md bg-gray-50 px-3 py-1.5 text-xs font-medium text-gray-600 transition-colors hover:bg-gray-100"
          >模板管理</button>
        </div>
      </div>
    </div>

    <!-- 受检主体个人成绩单 widget -->
    <MyInspectionWidget />

    <!-- Quick actions (核心入口 + 插件注册入口, 统一按权限与路由存在性过滤) -->
    <div class="rounded-lg border border-gray-200 bg-white px-5 py-4">
      <div class="mb-3 text-sm font-medium text-gray-500">快捷入口</div>
      <div class="flex flex-wrap gap-2">
        <button
          v-for="action in quickActions"
          :key="action.path"
          @click="goTo(action.path)"
          class="rounded-md border border-gray-200 bg-white px-4 py-2 text-sm text-gray-700 transition-colors hover:border-blue-300 hover:bg-blue-50 hover:text-blue-600"
        >{{ action.label }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, defineAsyncComponent } from 'vue'
import type { Component } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { usePluginsStore } from '@/stores/plugins'
import { getOverview as getDashboardOverview } from '@/api-generated/sdk.gen'
import type { DashboardOverview } from '@/types/dashboard'
import {
  enabledDashboardCards,
  enabledDashboardShortcuts,
  type DashboardCardDef,
} from '@/views/dashboard/dashboardCards'
import MyInspectionWidget from '@/components/inspection/MyInspectionWidget.vue'

// F7 i18n 演示 — 后续按模块逐步把硬编码文案改为 t()
const { t } = useI18n()

const router = useRouter()
const authStore = useAuthStore()
const pluginsStore = usePluginsStore()

const currentTime = ref('')
let timeInterval: number

const getGreeting = () => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 17) return '下午好'
  if (hour < 19) return '傍晚好'
  return '晚上好'
}

const currentDate = computed(() => {
  const now = new Date()
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${weekdays[now.getDay()]}`
})

const updateTime = () => {
  const now = new Date()
  currentTime.value = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
}

// ── 通用核心分区 ──
const org = reactive({ orgUnitCount: 0 })

const inspection = reactive({
  activeProjectCount: 0,
  pendingTaskCount: 0,
  correctiveOpenCount: 0
})

const system = reactive({
  totalUsers: 0,
  todayLoginCount: 0
})

// ── 行业分区原始数据 (sectionKey → 分区 map), 传给插件注册卡 ──
const overviewExtra = ref<Record<string, Record<string, unknown>>>({})

// ── 插件注册卡 (懒加载组件实例化一次) ──
type ResolvedCard = DashboardCardDef & { comp: Component }
const pluginCards = computed<ResolvedCard[]>(() =>
  enabledDashboardCards(pluginsStore.codes).map((c) => ({
    ...c,
    comp: defineAsyncComponent(c.component as () => Promise<Component>),
  }))
)
const fullCards = computed(() => pluginCards.value.filter((c) => c.span === 'full'))
const halfCards = computed(() => pluginCards.value.filter((c) => c.span === 'half'))

// ── 快捷入口: 核心通用 + 插件注册, 统一过滤 ──
const coreQuickActions = [
  { label: '组织架构', path: '/organization/units', perm: 'system:org:view' },
  { label: '用户管理', path: '/system/users',       perm: 'system:user:view' },
  { label: '角色权限', path: '/access/console',     perm: 'system:role:view' },
  { label: '检查项目', path: '/inspection/projects',  perm: 'insp:project:view' },
  { label: '模板管理', path: '/inspection/templates', perm: 'insp:template:view' },
  { label: '系统配置', path: '/system/configs',     perm: 'system:config:view' },
]

// Phase 4A 扩展: 过滤掉路由未注册的入口 (如插件禁用后对应路径不存在)
const quickActions = computed(() =>
  [...coreQuickActions, ...enabledDashboardShortcuts(pluginsStore.codes)].filter(a => {
    if (a.perm && !authStore.hasPermission(a.perm)) return false
    const resolved = router.resolve(a.path)
    return resolved.matched[0]?.name !== 'NotFound'
  })
)

const goTo = (path: string) => {
  router.push(path)
}

const loadData = async () => {
  try {
    const res = await getDashboardOverview()
    const data = (res.data?.data ?? {}) as unknown as DashboardOverview

    Object.assign(org, data.organization)
    Object.assign(inspection, data.inspection)
    Object.assign(system, data.system)

    // 通用分区之外的全部分区原样存下, 供插件注册卡按 sectionKey 取用
    const coreSections = new Set(['organization', 'inspection', 'system'])
    const extra: Record<string, Record<string, unknown>> = {}
    for (const [key, value] of Object.entries(data)) {
      if (!coreSections.has(key) && value && typeof value === 'object') {
        extra[key] = value as Record<string, unknown>
      }
    }
    overviewExtra.value = extra
  } catch (error) {
    console.error('Failed to load dashboard data:', error)
  }
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 60000) as unknown as number
  loadData()
})

onUnmounted(() => {
  clearInterval(timeInterval)
})
</script>
