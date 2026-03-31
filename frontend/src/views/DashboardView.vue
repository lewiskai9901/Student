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
        系统运行正常
      </span>
    </div>

    <!-- Organization stats bar -->
    <div class="rounded-lg border border-gray-200 bg-white px-5 py-4">
      <div class="mb-2 text-sm font-medium text-gray-500">组织概览</div>
      <div class="flex items-center gap-0 divide-x divide-gray-200 text-center">
        <div class="flex-1 cursor-pointer px-3 transition-colors hover:bg-gray-50" @click="goTo('/organization/units')">
          <div class="text-2xl font-semibold text-gray-900">{{ org.orgUnitCount }}</div>
          <div class="mt-0.5 text-xs text-gray-500">组织单元</div>
        </div>
        <div class="flex-1 cursor-pointer px-3 transition-colors hover:bg-gray-50" @click="goTo('/organization/academic/majors')">
          <div class="text-2xl font-semibold text-gray-900">{{ org.majorCount }}</div>
          <div class="mt-0.5 text-xs text-gray-500">专业</div>
        </div>
        <div class="flex-1 cursor-pointer px-3 transition-colors hover:bg-gray-50" @click="goTo('/organization/classes')">
          <div class="text-2xl font-semibold text-gray-900">{{ org.classCount }}</div>
          <div class="mt-0.5 text-xs text-gray-500">班级</div>
        </div>
        <div class="flex-1 cursor-pointer px-3 transition-colors hover:bg-gray-50" @click="goTo('/organization/students')">
          <div class="text-2xl font-semibold text-gray-900">{{ org.studentCount }}</div>
          <div class="mt-0.5 text-xs text-gray-500">学生</div>
        </div>
        <div class="flex-1 cursor-pointer px-3 transition-colors hover:bg-gray-50" @click="goTo('/access/users')">
          <div class="text-2xl font-semibold text-gray-900">{{ org.teacherCount }}</div>
          <div class="mt-0.5 text-xs text-gray-500">教师</div>
        </div>
      </div>
    </div>

    <!-- Middle row: Teaching + Inspection -->
    <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
      <!-- Teaching card -->
      <div class="rounded-lg border border-gray-200 bg-white px-5 py-4">
        <div class="mb-3 flex items-center justify-between">
          <span class="text-sm font-medium text-gray-500">本学期教学</span>
          <span class="rounded bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-600">{{ teaching.currentSemester }}</span>
        </div>
        <div class="flex items-baseline gap-6 text-sm">
          <div>
            <span class="text-gray-500">开设课程</span>
            <span class="ml-1.5 text-lg font-semibold text-gray-900">{{ teaching.courseCount }}</span>
          </div>
          <span class="text-gray-300">|</span>
          <div>
            <span class="text-gray-500">教学任务</span>
            <span class="ml-1.5 text-lg font-semibold text-gray-900">{{ teaching.taskCount }}</span>
          </div>
          <span class="text-gray-300">|</span>
          <div>
            <span class="text-gray-500">未排课</span>
            <span class="ml-1.5 text-lg font-semibold" :class="teaching.unscheduledCount > 0 ? 'text-amber-600' : 'text-gray-900'">{{ teaching.unscheduledCount }}</span>
          </div>
        </div>
        <!-- Schedule rate progress bar -->
        <div class="mt-3">
          <div class="mb-1 flex items-center justify-between text-xs">
            <span class="text-gray-500">排课进度</span>
            <span class="font-medium text-gray-700">{{ teaching.scheduledRate }}%</span>
          </div>
          <div class="h-2 w-full overflow-hidden rounded-full bg-gray-100">
            <div
              class="h-full rounded-full transition-all duration-700"
              :class="teaching.scheduledRate >= 90 ? 'bg-green-500' : teaching.scheduledRate >= 60 ? 'bg-blue-500' : 'bg-amber-500'"
              :style="{ width: teaching.scheduledRate + '%' }"
            ></div>
          </div>
        </div>
      </div>

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
            @click="goTo('/inspection/v7/projects')"
            class="rounded-md bg-gray-50 px-3 py-1.5 text-xs font-medium text-gray-600 transition-colors hover:bg-gray-100"
          >检查项目</button>
          <button
            @click="goTo('/inspection/v7/tasks')"
            class="rounded-md bg-gray-50 px-3 py-1.5 text-xs font-medium text-gray-600 transition-colors hover:bg-gray-100"
          >检查任务</button>
          <button
            @click="goTo('/inspection/v7/templates')"
            class="rounded-md bg-gray-50 px-3 py-1.5 text-xs font-medium text-gray-600 transition-colors hover:bg-gray-100"
          >模板管理</button>
        </div>
      </div>
    </div>

    <!-- System stats bar -->
    <div class="rounded-lg border border-gray-200 bg-white px-5 py-4">
      <div class="mb-2 text-sm font-medium text-gray-500">系统状态</div>
      <div class="flex items-baseline gap-6 text-sm">
        <div>
          <span class="text-gray-500">用户总数</span>
          <span class="ml-1.5 text-lg font-semibold text-gray-900">{{ system.totalUsers }}</span>
        </div>
        <span class="text-gray-300">|</span>
        <div>
          <span class="text-gray-500">今日登录</span>
          <span class="ml-1.5 text-lg font-semibold text-gray-900">{{ system.todayLoginCount }}</span>
        </div>
      </div>
    </div>

    <!-- Quick actions -->
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
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getDashboardOverview, type DashboardOverview } from '@/api/dashboard'

const router = useRouter()
const authStore = useAuthStore()

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

// Reactive stats - initialize all to 0
const org = reactive({
  orgUnitCount: 0,
  majorCount: 0,
  classCount: 0,
  studentCount: 0,
  teacherCount: 0
})

const teaching = reactive({
  currentSemester: '--',
  courseCount: 0,
  taskCount: 0,
  scheduledRate: 0,
  unscheduledCount: 0
})

const inspection = reactive({
  activeProjectCount: 0,
  pendingTaskCount: 0,
  correctiveOpenCount: 0
})

const system = reactive({
  totalUsers: 0,
  todayLoginCount: 0
})

const quickActions = [
  { label: '组织架构', path: '/organization/units' },
  { label: '班级管理', path: '/organization/classes' },
  { label: '学生管理', path: '/organization/students' },
  { label: '用户管理', path: '/access/users' },
  { label: '角色管理', path: '/access/roles' },
  { label: '课程管理', path: '/teaching/courses' },
  { label: '检查项目', path: '/inspection/v7/projects' },
  { label: '模板管理', path: '/inspection/v7/templates' },
  { label: '系统配置', path: '/settings/configs' },
]

const goTo = (path: string) => {
  router.push(path)
}

const loadData = async () => {
  try {
    const data: DashboardOverview = await getDashboardOverview()

    // Organization
    Object.assign(org, data.organization)

    // Teaching
    Object.assign(teaching, data.teaching)

    // Inspection
    Object.assign(inspection, data.inspection)

    // System
    Object.assign(system, data.system)
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
