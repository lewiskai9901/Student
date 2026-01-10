<template>
  <div class="p-4">
    <!-- 提示信息 -->
    <div class="mb-4 flex items-start gap-3 rounded-xl bg-blue-50 p-4">
      <Megaphone class="mt-0.5 h-5 w-5 flex-shrink-0 text-blue-600" />
      <div>
        <p class="font-medium text-blue-800">
          以下 <span class="text-lg font-bold text-blue-600">{{ publicityList.length }}</span> 个申诉正在公示期
        </p>
        <p class="mt-1 text-sm text-blue-600">公示期结束后将自动生效</p>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="overflow-hidden rounded-xl border border-gray-200">
      <!-- 加载状态 -->
      <div v-if="loading" class="flex items-center justify-center py-16">
        <Loader2 class="h-8 w-8 animate-spin text-blue-600" />
      </div>

      <!-- 空状态 -->
      <div v-else-if="publicityList.length === 0" class="flex flex-col items-center justify-center py-16 text-gray-400">
        <FileSearch class="h-12 w-12" />
        <p class="mt-3 text-sm">暂无公示中的申诉</p>
      </div>

      <!-- 表格 -->
      <table v-else class="w-full">
        <thead>
          <tr class="bg-gray-50 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
            <th class="px-4 py-3">申诉编号</th>
            <th class="px-4 py-3">年级</th>
            <th class="px-4 py-3">班级</th>
            <th class="px-4 py-3">申请人</th>
            <th class="px-4 py-3">申诉原因</th>
            <th class="px-4 py-3 text-center">原始分数</th>
            <th class="px-4 py-3 text-center">调整后分数</th>
            <th class="px-4 py-3">公示截止时间</th>
            <th class="px-4 py-3 text-center">剩余时间</th>
            <th class="px-4 py-3 text-center">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr
            v-for="(row, index) in publicityList"
            :key="row.id"
            class="transition-colors hover:bg-gray-50"
            :style="{ animationDelay: `${index * 30}ms` }"
          >
            <td class="px-4 py-3 text-sm font-medium text-gray-900">{{ row.appealCode }}</td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ row.gradeName }}</td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ row.className }}</td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ row.applicantName }}</td>
            <td class="px-4 py-3 text-sm text-gray-600">
              <span class="line-clamp-1" :title="row.reason">{{ row.reason }}</span>
            </td>
            <td class="px-4 py-3 text-center">
              <span class="rounded-full bg-red-100 px-2 py-1 text-xs font-semibold text-red-700">
                {{ row.originalScore }}
              </span>
            </td>
            <td class="px-4 py-3 text-center">
              <span class="rounded-full bg-green-100 px-2 py-1 text-xs font-semibold text-green-700">
                {{ row.adjustedScore }}
              </span>
            </td>
            <td class="px-4 py-3 text-sm text-gray-500">
              <span class="rounded-lg bg-amber-100 px-2 py-1 text-xs font-medium text-amber-700">
                {{ formatDate(row.publicityEndTime) }}
              </span>
            </td>
            <td class="px-4 py-3 text-center">
              <span
                class="inline-flex items-center gap-1 text-sm font-medium"
                :class="getTimeLeftColorClass(row.publicityEndTime)"
              >
                <Clock class="h-4 w-4" />
                {{ getTimeLeft(row.publicityEndTime) }}
              </span>
            </td>
            <td class="px-4 py-3 text-center">
              <button
                @click="handleView(row)"
                class="text-sm text-blue-600 hover:text-blue-700"
              >
                查看详情
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getPublicityAppeals } from '@/api/v2/appeal'
import { ElMessage } from 'element-plus'
import {
  Megaphone,
  Loader2,
  FileSearch,
  Clock
} from 'lucide-vue-next'

const loading = ref(false)
const publicityList = ref<any[]>([])
let refreshTimer: ReturnType<typeof setInterval> | null = null

// 加载公示中的申诉
const loadPublicity = async () => {
  loading.value = true
  try {
    const res = await getPublicityAppeals()
    publicityList.value = res || []
  } catch (error) {
    console.error('加载公示列表失败:', error)
    ElMessage.error('加载公示列表失败')
  } finally {
    loading.value = false
  }
}

// 查看详情
const handleView = (row: any) => {
  ElMessage.info(`查看申诉详情: ${row.appealCode}`)
}

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

// 获取剩余时间
const getTimeLeft = (endTime: string) => {
  if (!endTime) return '-'

  const end = new Date(endTime).getTime()
  const now = new Date().getTime()
  const diff = end - now

  if (diff <= 0) return '已结束'

  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))

  if (days > 0) return `${days}天${hours}小时`
  if (hours > 0) return `${hours}小时${minutes}分钟`
  return `${minutes}分钟`
}

// 获取剩余时间颜色
const getTimeLeftColorClass = (endTime: string) => {
  if (!endTime) return 'text-gray-500'

  const end = new Date(endTime).getTime()
  const now = new Date().getTime()
  const diff = end - now
  const hours = diff / (1000 * 60 * 60)

  if (hours <= 0) return 'text-red-600'
  if (hours <= 24) return 'text-amber-600'
  return 'text-green-600'
}

onMounted(() => {
  loadPublicity()
  // 每分钟刷新一次
  refreshTimer = setInterval(loadPublicity, 60000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
</style>
