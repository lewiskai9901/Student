<template>
  <div class="p-6 bg-gray-50 min-h-full">
    <!-- 页面标题 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">任务管理</h1>
      <p class="mt-1 text-sm text-gray-500">管理和分配工作任务</p>
    </div>

    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
      <StatCard
        title="待接收"
        :value="statistics.pendingCount"
        :icon="Clock"
        subtitle="待处理"
        color="orange"
      />
      <StatCard
        title="进行中"
        :value="statistics.inProgressCount"
        :icon="Play"
        subtitle="执行中"
        color="blue"
      />
      <StatCard
        title="待审核"
        :value="statistics.submittedCount"
        :icon="FileCheck"
        subtitle="待审批"
        color="amber"
      />
      <StatCard
        title="已完成"
        :value="statistics.completedCount"
        :icon="CheckCircle2"
        subtitle="已结束"
        color="emerald"
      />
      <StatCard
        title="完成率"
        :value="`${statistics.completionRate?.toFixed(1)}%`"
        :icon="TrendingUp"
        subtitle="完成进度"
        color="purple"
      />
    </div>

    <!-- 搜索和操作 -->
    <div class="mb-4 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-end gap-3">
        <div class="w-48">
          <label class="mb-1 block text-xs font-medium text-gray-600">关键词</label>
          <input
            v-model="queryParams.keyword"
            type="text"
            placeholder="任务标题/编号"
            class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
        <div class="w-32">
          <label class="mb-1 block text-xs font-medium text-gray-600">状态</label>
          <select
            v-model="queryParams.status"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm"
          >
            <option :value="undefined">全部</option>
            <option :value="0">待接收</option>
            <option :value="1">进行中</option>
            <option :value="2">待审核</option>
            <option :value="3">已完成</option>
            <option :value="4">已打回</option>
          </select>
        </div>
        <div class="w-32">
          <label class="mb-1 block text-xs font-medium text-gray-600">优先级</label>
          <select
            v-model="queryParams.priority"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm"
          >
            <option :value="undefined">全部</option>
            <option :value="1">紧急</option>
            <option :value="2">普通</option>
            <option :value="3">低</option>
          </select>
        </div>
        <button
          class="h-9 rounded-md bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          @click="handleSearch"
        >
          搜索
        </button>
        <button
          class="h-9 rounded-md border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
          @click="handleReset"
        >
          重置
        </button>
        <div class="flex-1"></div>
        <button
          class="h-9 rounded-md bg-green-600 px-4 text-sm font-medium text-white hover:bg-green-700"
          @click="openCreateDialog"
        >
          + 创建任务
        </button>
      </div>
    </div>

    <!-- 任务列表 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <table class="min-w-full divide-y divide-gray-200">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500">任务编号</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500">任务标题</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500">优先级</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500">状态</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500">执行人</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500">截止时间</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500">创建时间</th>
            <th class="px-4 py-3 text-center text-xs font-medium text-gray-500">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-200 bg-white">
          <tr v-for="task in taskList" :key="task.id" class="hover:bg-gray-50">
            <td class="px-4 py-3 text-sm text-blue-600">{{ task.taskCode }}</td>
            <td class="px-4 py-3 text-sm text-gray-900">
              {{ task.title }}
              <span v-if="task.overdue" class="ml-1 text-xs text-red-500">[超期]</span>
            </td>
            <td class="px-4 py-3">
              <span :class="getPriorityClass(task.priority)" class="rounded px-2 py-0.5 text-xs">
                {{ task.priorityText }}
              </span>
            </td>
            <td class="px-4 py-3">
              <span :class="getStatusClass(task.status)" class="rounded px-2 py-0.5 text-xs">
                {{ task.statusText }}
              </span>
            </td>
            <td class="px-4 py-3 text-sm text-gray-600">
              <template v-if="task.totalAssignees && task.totalAssignees > 1">
                <div class="flex items-center gap-2">
                  <div class="h-1.5 w-16 overflow-hidden rounded-full bg-gray-200">
                    <div
                      class="h-full rounded-full bg-green-500"
                      :style="{ width: `${((task.completedAssignees || 0) / task.totalAssignees) * 100}%` }"
                    ></div>
                  </div>
                  <span class="text-xs">{{ task.completedAssignees || 0 }}/{{ task.totalAssignees }}</span>
                </div>
              </template>
              <template v-else>
                {{ task.assigneeName || '-' }}
              </template>
            </td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ formatDate(task.dueDate) }}</td>
            <td class="px-4 py-3 text-sm text-gray-500">{{ formatDate(task.createdAt) }}</td>
            <td class="px-4 py-3 text-center">
              <button
                class="text-sm text-blue-600 hover:text-blue-800"
                @click="viewDetail(task)"
              >
                详情
              </button>
            </td>
          </tr>
          <tr v-if="taskList.length === 0">
            <td colspan="8" class="px-4 py-8 text-center text-sm text-gray-500">暂无数据</td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <p class="text-sm text-gray-500">共 {{ total }} 条记录</p>
        <div class="flex gap-2">
          <button
            :disabled="queryParams.pageNum === 1"
            class="rounded border px-3 py-1 text-sm disabled:opacity-50"
            @click="queryParams.pageNum--; loadTaskList()"
          >
            上一页
          </button>
          <button
            :disabled="queryParams.pageNum * queryParams.pageSize >= total"
            class="rounded border px-3 py-1 text-sm disabled:opacity-50"
            @click="queryParams.pageNum++; loadTaskList()"
          >
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 创建任务对话框 -->
    <TaskCreateDialog
      v-model:visible="createDialogVisible"
      @success="loadTaskList"
    />

    <!-- 任务详情对话框 -->
    <TaskDetailDialog
      v-model:visible="detailDialogVisible"
      :task-id="selectedTaskId"
      @refresh="loadTaskList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Clock, Play, CheckCircle2, FileCheck, TrendingUp } from 'lucide-vue-next'
import { getTaskList, getTaskStatistics, type TaskDTO, type TaskStatisticsDTO } from '@/api/v2/task'
import TaskCreateDialog from './components/TaskCreateDialog.vue'
import TaskDetailDialog from './components/TaskDetailDialog.vue'
import StatCard from '@/components/design-system/cards/StatCard.vue'

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: undefined as number | undefined,
  priority: undefined as number | undefined
})

// 数据
const taskList = ref<TaskDTO[]>([])
const total = ref(0)
const statistics = ref<TaskStatisticsDTO>({
  totalCount: 0,
  pendingCount: 0,
  inProgressCount: 0,
  submittedCount: 0,
  completedCount: 0,
  rejectedCount: 0,
  cancelledCount: 0,
  overdueCount: 0,
  completionRate: 0,
  overdueRate: 0,
  pendingApprovalCount: 0
})

// 对话框
const createDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const selectedTaskId = ref<string | number>()

// 加载任务列表
const loadTaskList = async () => {
  try {
    const res = await getTaskList(queryParams)
    taskList.value = res.records
    total.value = res.total
  } catch (error) {
    console.error('加载任务列表失败', error)
  }
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    statistics.value = await getTaskStatistics()
  } catch (error) {
    console.error('加载统计失败', error)
  }
}

// 搜索
const handleSearch = () => {
  queryParams.pageNum = 1
  loadTaskList()
}

// 重置
const handleReset = () => {
  queryParams.keyword = ''
  queryParams.status = undefined
  queryParams.priority = undefined
  queryParams.pageNum = 1
  loadTaskList()
}

// 创建任务
const openCreateDialog = () => {
  createDialogVisible.value = true
}

// 查看详情
const viewDetail = (task: TaskDTO) => {
  selectedTaskId.value = task.id
  detailDialogVisible.value = true
}

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return dateStr.substring(0, 16).replace('T', ' ')
}

// 优先级样式
const getPriorityClass = (priority: number) => {
  const classes: Record<number, string> = {
    1: 'bg-red-100 text-red-800',
    2: 'bg-blue-100 text-blue-800',
    3: 'bg-gray-100 text-gray-800'
  }
  return classes[priority] || 'bg-gray-100 text-gray-800'
}

// 状态样式
const getStatusClass = (status: number) => {
  const classes: Record<number, string> = {
    0: 'bg-orange-100 text-orange-800',
    1: 'bg-blue-100 text-blue-800',
    2: 'bg-yellow-100 text-yellow-800',
    3: 'bg-green-100 text-green-800',
    4: 'bg-red-100 text-red-800',
    5: 'bg-gray-100 text-gray-800',
    6: 'bg-purple-100 text-purple-800'
  }
  return classes[status] || 'bg-gray-100 text-gray-800'
}

onMounted(() => {
  loadTaskList()
  loadStatistics()
})
</script>
