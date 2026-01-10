<template>
  <div class="space-y-6">
    <!-- 顶部欢迎区域 -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-semibold text-gray-900">{{ getGreeting() }}，{{ authStore.userName || '用户' }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ currentDate }} {{ currentTime }}</p>
      </div>
      <div class="flex items-center gap-2">
        <span class="inline-flex items-center gap-1.5 rounded-full bg-green-50 px-3 py-1 text-xs font-medium text-green-700">
          <span class="h-1.5 w-1.5 rounded-full bg-green-500"></span>
          系统运行正常
        </span>
      </div>
    </div>

    <!-- 统计卡片 - 使用设计系统组件 -->
    <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <!-- 学生总数 -->
      <StatCard
        title="学生总数"
        :value="animatedStats.studentCount"
        :icon="Users"
        subtitle="在校学生"
        :trend="12.5"
        color="blue"
        clickable
        @click="goToPage('/student-affairs/students')"
      />

      <!-- 班级总数 -->
      <StatCard
        title="班级总数"
        :value="animatedStats.classCount"
        :icon="GraduationCap"
        subtitle="教学班级"
        :trend="5.2"
        color="purple"
        clickable
        @click="goToPage('/student-affairs/classes')"
      />

      <!-- 宿舍信息 -->
      <StatCard
        title="宿舍总数"
        :value="animatedStats.dormitoryCount"
        :icon="Building2"
        :subtitle="`入住率 ${statistics.occupancyRate}%`"
        :progress="statistics.occupancyRate"
        :trend="2.8"
        color="emerald"
        clickable
        @click="goToPage('/dormitory/rooms')"
      />

      <!-- 今日检查 -->
      <StatCard
        title="今日检查"
        :value="animatedStats.todayCheckCount"
        :icon="ClipboardCheck"
        :subtitle="`完成 ${statistics.completedChecks} · 待处理 ${statistics.pendingChecks}`"
        :progress="statistics.completionRate"
        :trend="-1.5"
        color="orange"
        clickable
        @click="goToPage('/quantification/daily-checks')"
      />
    </div>

    <!-- 主内容区域 -->
    <div class="grid gap-6 xl:grid-cols-3">
      <!-- 检查得分趋势 -->
      <div class="xl:col-span-2 rounded-xl bg-white border border-gray-200">
        <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4">
          <div>
            <h3 class="font-medium text-gray-900">检查得分趋势</h3>
            <p class="text-xs text-gray-500 mt-0.5">各班级量化考核平均分走势</p>
          </div>
          <div class="flex items-center rounded-lg bg-gray-100 p-0.5">
            <button
              v-for="period in chartPeriods"
              :key="period.value"
              @click="changeChartPeriod(period.value)"
              :class="[
                'rounded-md px-3 py-1 text-xs font-medium transition-all',
                chartPeriod === period.value
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-500 hover:text-gray-700'
              ]"
            >
              {{ period.label }}
            </button>
          </div>
        </div>
        <div class="p-5">
          <div v-if="chartData.length > 0" class="relative">
            <!-- Y轴标签 -->
            <div class="absolute left-0 top-0 flex h-40 w-8 flex-col justify-between text-xs text-gray-400">
              <span>100</span>
              <span>75</span>
              <span>50</span>
              <span>25</span>
              <span>0</span>
            </div>
            <!-- 图表区域 -->
            <div class="ml-10">
              <div class="absolute left-10 right-0 top-0 flex h-40 flex-col justify-between pointer-events-none">
                <div v-for="i in 5" :key="i" class="h-px bg-gray-100"></div>
              </div>
              <div class="relative flex h-40 items-end justify-around gap-2">
                <div
                  v-for="(item, index) in chartData"
                  :key="index"
                  class="group relative flex-1 max-w-10"
                >
                  <div
                    class="relative mx-auto w-full rounded-t bg-blue-500 transition-all hover:bg-blue-600"
                    :style="{ height: `${(item.score / 100) * 160}px` }"
                  >
                  </div>
                  <div class="pointer-events-none absolute -top-7 left-1/2 -translate-x-1/2 rounded bg-gray-800 px-2 py-0.5 text-xs text-white opacity-0 transition-opacity group-hover:opacity-100 whitespace-nowrap z-10">
                    {{ item.score }}分
                  </div>
                </div>
              </div>
              <div class="mt-2 flex justify-around">
                <span v-for="(item, index) in chartData" :key="index" class="flex-1 max-w-10 text-center text-xs text-gray-400">
                  {{ item.date }}
                </span>
              </div>
            </div>
          </div>
          <div v-else class="flex h-40 flex-col items-center justify-center text-gray-400">
            <BarChart3 class="mb-2 h-8 w-8 opacity-50" />
            <p class="text-sm">暂无数据</p>
          </div>
        </div>
      </div>

      <!-- 检查完成率 -->
      <div class="rounded-xl bg-white border border-gray-200">
        <div class="border-b border-gray-100 px-5 py-4">
          <h3 class="font-medium text-gray-900">检查完成率</h3>
          <p class="text-xs text-gray-500 mt-0.5">本周各类检查完成情况</p>
        </div>
        <div class="p-5">
          <div class="flex flex-col items-center">
            <!-- 环形进度图 -->
            <div class="relative h-28 w-28">
              <svg class="h-full w-full -rotate-90 transform" viewBox="0 0 112 112">
                <circle cx="56" cy="56" r="48" class="fill-none stroke-gray-100" stroke-width="8" />
                <circle
                  cx="56"
                  cy="56"
                  r="48"
                  class="fill-none stroke-blue-500"
                  stroke-width="8"
                  stroke-linecap="round"
                  :stroke-dasharray="`${statistics.completionRate * 3.015} 301.5`"
                  style="transition: stroke-dasharray 1s ease-out"
                />
              </svg>
              <div class="absolute inset-0 flex flex-col items-center justify-center">
                <span class="text-xl font-semibold text-gray-900">{{ statistics.completionRate }}%</span>
              </div>
            </div>
            <!-- 分类统计 -->
            <div class="mt-5 w-full space-y-2">
              <div v-for="item in checkCategories" :key="item.name" class="flex items-center gap-2">
                <span class="h-2 w-2 rounded-full flex-shrink-0" :style="{ backgroundColor: item.color }"></span>
                <span class="flex-1 text-xs text-gray-600">{{ item.name }}</span>
                <span class="text-xs font-medium text-gray-900">{{ item.value }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部区域 -->
    <div class="grid gap-6 xl:grid-cols-3">
      <!-- 最近检查记录 -->
      <div class="xl:col-span-2 rounded-xl bg-white border border-gray-200">
        <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4">
          <h3 class="font-medium text-gray-900">最近检查记录</h3>
          <button
            @click="viewAllRecords"
            class="flex items-center gap-1 text-xs font-medium text-blue-600 hover:text-blue-700"
          >
            查看全部
            <ChevronRight class="h-4 w-4" />
          </button>
        </div>
        <div class="divide-y divide-gray-100">
          <div
            v-for="record in recentRecords"
            :key="record.id"
            class="flex items-center justify-between px-5 py-3 hover:bg-gray-50 transition-colors"
          >
            <div class="flex items-center gap-3 min-w-0">
              <div
                :class="[
                  'flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-lg',
                  record.scoreRate >= 90 ? 'bg-green-50 text-green-600' :
                  record.scoreRate >= 80 ? 'bg-amber-50 text-amber-600' :
                  'bg-red-50 text-red-600'
                ]"
              >
                <ClipboardCheck class="h-4 w-4" />
              </div>
              <div class="min-w-0">
                <p class="truncate text-sm font-medium text-gray-900">{{ record.typeName }}</p>
                <p class="text-xs text-gray-500">
                  {{ record.targetName }} · {{ formatTimeAgo(record.createdAt) }}
                </p>
              </div>
            </div>
            <span
              :class="[
                'rounded-full px-2.5 py-0.5 text-xs font-medium',
                record.scoreRate >= 90 ? 'bg-green-50 text-green-700' :
                record.scoreRate >= 80 ? 'bg-amber-50 text-amber-700' :
                'bg-red-50 text-red-700'
              ]"
            >
              {{ record.totalScore }}分
            </span>
          </div>
        </div>
        <div v-if="recentRecords.length === 0" class="flex flex-col items-center justify-center py-10 text-gray-400">
          <FileText class="mb-2 h-8 w-8 opacity-50" />
          <p class="text-sm">暂无检查记录</p>
        </div>
      </div>

      <!-- 快捷操作 -->
      <div class="rounded-xl bg-white border border-gray-200">
        <div class="border-b border-gray-100 px-5 py-4">
          <h3 class="font-medium text-gray-900">快捷操作</h3>
        </div>
        <div class="p-4">
          <div class="grid grid-cols-3 gap-2">
            <button
              v-for="action in quickActions"
              :key="action.name"
              @click="goToPage(action.path)"
              class="flex flex-col items-center gap-2 rounded-lg p-3 transition-colors hover:bg-gray-50"
            >
              <div
                class="flex h-10 w-10 items-center justify-center rounded-lg"
                :style="{ backgroundColor: action.bgColor }"
              >
                <component :is="action.icon" class="h-5 w-5" :style="{ color: action.color }" />
              </div>
              <span class="text-xs text-gray-600">{{ action.name }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Users,
  GraduationCap,
  Building2,
  ClipboardCheck,
  ChevronRight,
  BarChart3,
  FileText,
  UserPlus,
  Settings,
  PieChart
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { getDashboardStatistics, type DashboardStatistics, type ChartDataItem, type CategoryItem, type RecentCheckRecord } from '@/api/v2/dashboard'
import StatCard from '@/components/design-system/cards/StatCard.vue'

const router = useRouter()
const authStore = useAuthStore()

const currentTime = ref('')
let timeInterval: number

// 加载状态
const loading = ref(false)

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

// 统计数据
const statistics = reactive({
  studentCount: 0,
  classCount: 0,
  dormitoryCount: 0,
  todayCheckCount: 0,
  occupancyRate: 0,
  completedChecks: 0,
  pendingChecks: 0,
  completionRate: 0
})

// 动画数字
const animatedStats = reactive({
  studentCount: 0,
  classCount: 0,
  dormitoryCount: 0,
  todayCheckCount: 0
})

// 图表周期选项
const chartPeriods = [
  { value: 7, label: '7天' },
  { value: 30, label: '30天' },
  { value: 90, label: '90天' }
]
const chartPeriod = ref(7)

// 图表数据
const chartData = ref<ChartDataItem[]>([])

// 检查分类
const checkCategories = ref<CategoryItem[]>([])

// 最近检查记录
const recentRecords = ref<RecentCheckRecord[]>([])

const quickActions = [
  { name: '学生管理', path: '/student-affairs/students', icon: Users, bgColor: '#dbeafe', color: '#2563eb' },
  { name: '班级管理', path: '/student-affairs/classes', icon: GraduationCap, bgColor: '#f3e8ff', color: '#9333ea' },
  { name: '日常检查', path: '/quantification/daily-checks', icon: ClipboardCheck, bgColor: '#dcfce7', color: '#16a34a' },
  { name: '新增学生', path: '/student-affairs/students', icon: UserPlus, bgColor: '#fef3c7', color: '#d97706' },
  { name: '数据统计', path: '/quantification/statistics', icon: PieChart, bgColor: '#fce7f3', color: '#db2777' },
  { name: '系统设置', path: '/system/users', icon: Settings, bgColor: '#e0e7ff', color: '#4f46e5' }
]

const formatTimeAgo = (dateStr: string) => {
  const now = new Date()
  const date = new Date(dateStr)
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  return `${Math.floor(hours / 24)}天前`
}

const animateNumber = (target: keyof typeof animatedStats, endValue: number, duration: number = 1000) => {
  const startTime = Date.now()
  const startValue = animatedStats[target]
  const diff = endValue - startValue

  const animate = () => {
    const elapsed = Date.now() - startTime
    const progress = Math.min(elapsed / duration, 1)
    const easeOut = 1 - Math.pow(1 - progress, 3)
    animatedStats[target] = Math.round(startValue + diff * easeOut)

    if (progress < 1) {
      requestAnimationFrame(animate)
    }
  }
  animate()
}

const goToPage = (path: string) => {
  router.push(path)
}

const viewAllRecords = () => {
  router.push('/quantification/records')
}

// 切换图表周期
const changeChartPeriod = (days: number) => {
  chartPeriod.value = days
  loadData()
}

// 加载数据
const loadData = async () => {
  try {
    loading.value = true
    const data = await getDashboardStatistics(chartPeriod.value)

    // 更新统计数据
    statistics.studentCount = data.studentCount
    statistics.classCount = data.classCount
    statistics.dormitoryCount = data.dormitoryCount
    statistics.todayCheckCount = data.todayCheckCount
    statistics.occupancyRate = data.occupancyRate
    statistics.completedChecks = data.completedChecks
    statistics.pendingChecks = data.pendingChecks
    statistics.completionRate = data.completionRate

    // 动画数字
    animateNumber('studentCount', data.studentCount)
    animateNumber('classCount', data.classCount)
    animateNumber('dormitoryCount', data.dormitoryCount)
    animateNumber('todayCheckCount', data.todayCheckCount)

    // 更新图表数据
    chartData.value = data.chartData || []

    // 更新检查分类
    checkCategories.value = data.checkCategories || []

    // 更新最近记录
    recentRecords.value = data.recentRecords || []
  } catch (error) {
    console.error('加载仪表盘数据失败:', error)
  } finally {
    loading.value = false
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
