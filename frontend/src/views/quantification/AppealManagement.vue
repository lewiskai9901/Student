<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <div @click="activeTab = 'my'">
        <StatCard
          title="我的申诉"
          :value="stats.myAppeals"
          :icon="FileEdit"
          subtitle="我发起的"
          color="blue"
          clickable
        />
      </div>
      <div @click="activeTab = 'pending'">
        <StatCard
          title="待我审核"
          :value="stats.pendingReview"
          :icon="Clock"
          subtitle="待处理"
          color="orange"
          clickable
        />
      </div>
      <div @click="activeTab = 'publicity'">
        <StatCard
          title="公示中"
          :value="stats.inPublicity"
          :icon="Megaphone"
          subtitle="正在公示"
          color="emerald"
          clickable
        />
      </div>
      <div v-if="isAdmin" @click="activeTab = 'all'">
        <StatCard
          title="全部申诉"
          :value="stats.totalAppeals"
          :icon="ListChecks"
          subtitle="总数统计"
          color="purple"
          clickable
        />
      </div>
      <div v-if="!isAdmin" @click="activeTab = 'statistics'">
        <StatCard
          title="统计分析"
          value="-"
          :icon="BarChart3"
          subtitle="数据分析"
          color="purple"
          clickable
        />
      </div>
    </div>

    <!-- Tab 导航栏 -->
    <div class="mb-6 rounded-lg border border-gray-200 bg-white p-1">
      <div class="flex gap-1">
        <button
          @click="activeTab = 'my'"
          :class="['flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium', activeTab === 'my' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100']"
        >
          <FileEdit class="h-4 w-4" />
          我的申诉
        </button>
        <button
          @click="activeTab = 'pending'"
          :class="['flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium', activeTab === 'pending' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100']"
        >
          <Clock class="h-4 w-4" />
          待我审核
          <span v-if="stats.pendingReview > 0" class="rounded-full bg-red-500 px-1.5 py-0.5 text-xs text-white">
            {{ stats.pendingReview }}
          </span>
        </button>
        <button
          @click="activeTab = 'publicity'"
          :class="['flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium', activeTab === 'publicity' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100']"
        >
          <Megaphone class="h-4 w-4" />
          公示中
        </button>
        <button
          v-if="isAdmin"
          @click="activeTab = 'all'"
          :class="['flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium', activeTab === 'all' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100']"
        >
          <ListChecks class="h-4 w-4" />
          全部申诉
        </button>
        <button
          @click="activeTab = 'statistics'"
          :class="['flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium', activeTab === 'statistics' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100']"
        >
          <BarChart3 class="h-4 w-4" />
          统计分析
        </button>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <AppealList v-if="activeTab === 'my'" query-type="my" />
      <AppealPendingList v-else-if="activeTab === 'pending'" />
      <AppealPublicityList v-else-if="activeTab === 'publicity'" />
      <AppealList v-else-if="activeTab === 'all' && isAdmin" query-type="all" />
      <AppealStatistics v-else-if="activeTab === 'statistics'" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { FileEdit, Clock, Megaphone, ListChecks, BarChart3 } from 'lucide-vue-next'
import AppealList from './components/AppealList.vue'
import AppealPendingList from './components/AppealPendingList.vue'
import AppealPublicityList from './components/AppealPublicityList.vue'
import AppealStatistics from './components/AppealStatistics.vue'
import { useAuthStore } from '@/stores/auth'
import { getAppealSummary } from '@/api/appeal'

const authStore = useAuthStore()
const activeTab = ref('my')
const isAdmin = ref(authStore.hasPermission('appeal:manage:all'))

const stats = reactive({
  myAppeals: 0,
  pendingReview: 0,
  inPublicity: 0,
  totalAppeals: 0
})

const loadStats = async () => {
  try {
    const summary = await getAppealSummary()
    stats.myAppeals = summary.myAppeals
    stats.pendingReview = summary.pendingReview
    stats.inPublicity = summary.inPublicity
    stats.totalAppeals = summary.totalAppeals
  } catch (error) {
    console.error('加载申诉统计数据失败:', error)
  }
}

onMounted(() => loadStats())
</script>
