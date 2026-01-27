<template>
  <div class="p-6 bg-gray-50 min-h-full">
    <!-- 页面标题 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">我的任务</h1>
      <p class="mt-1 text-sm text-gray-500">查看分配给我的所有任务</p>
    </div>

    <!-- 状态筛选标签 -->
    <div class="mb-4 flex gap-2">
      <button
        v-for="tab in statusTabs"
        :key="tab.value"
        :class="[
          'rounded-full px-4 py-1.5 text-sm font-medium transition-colors',
          currentStatus === tab.value
            ? 'bg-blue-600 text-white'
            : 'bg-white text-gray-600 hover:bg-gray-100'
        ]"
        @click="changeStatus(tab.value)"
      >
        {{ tab.label }}
        <span
          v-if="tab.count > 0"
          :class="[
            'ml-1 rounded-full px-1.5 py-0.5 text-xs',
            currentStatus === tab.value ? 'bg-blue-500 text-white' : 'bg-gray-200 text-gray-600'
          ]"
        >
          {{ tab.count }}
        </span>
      </button>
    </div>

    <!-- 搜索和筛选栏 -->
    <div class="mb-4 bg-white rounded-lg p-4 border border-gray-200">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-3">
        <!-- 搜索框 -->
        <div class="md:col-span-2">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索任务标题或编号..."
            class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            @keyup.enter="handleSearch"
          />
        </div>

        <!-- 优先级筛选 -->
        <div>
          <select
            v-model="filterPriority"
            class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            @change="handleSearch"
          >
            <option :value="undefined">全部优先级</option>
            <option :value="1">紧急</option>
            <option :value="2">普通</option>
            <option :value="3">低</option>
          </select>
        </div>

        <!-- 截止日期筛选 -->
        <div>
          <select
            v-model="filterDueDate"
            class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            @change="handleSearch"
          >
            <option value="">全部时间</option>
            <option value="today">今天截止</option>
            <option value="week">本周截止</option>
            <option value="month">本月截止</option>
            <option value="overdue">已超期</option>
          </select>
        </div>
      </div>

      <div class="mt-3 flex items-center justify-between">
        <!-- 排序 -->
        <div class="flex items-center gap-2">
          <span class="text-xs text-gray-500">排序：</span>
          <button
            v-for="sort in sortOptions"
            :key="sort.value"
            :class="[
              'px-2 py-1 text-xs rounded transition-colors',
              sortBy === sort.value
                ? 'bg-blue-100 text-blue-600'
                : 'text-gray-600 hover:bg-gray-100'
            ]"
            @click="changeSortBy(sort.value)"
          >
            {{ sort.label }}
          </button>
        </div>

        <!-- 搜索按钮 -->
        <button
          class="rounded-md bg-blue-600 px-4 py-1.5 text-xs font-medium text-white hover:bg-blue-700"
          @click="handleSearch"
        >
          搜索
        </button>
      </div>
    </div>

    <!-- 任务列表 -->
    <div class="space-y-4">
      <div
        v-for="task in taskList"
        :key="task.id"
        class="rounded-lg border border-gray-200 bg-white p-4 hover:shadow-md transition-shadow cursor-pointer"
        @click="viewDetail(task)"
      >
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <div class="flex items-center gap-2">
              <h3 class="font-medium text-gray-900">{{ task.title }}</h3>
              <span :class="getPriorityClass(task.priority)" class="rounded px-2 py-0.5 text-xs">
                {{ task.priorityText }}
              </span>
              <span :class="getStatusClass(task.status)" class="rounded px-2 py-0.5 text-xs">
                {{ task.statusText }}
              </span>
              <span v-if="task.overdue" class="text-xs text-red-500">[超期]</span>
            </div>
            <p class="mt-1 text-sm text-gray-500 line-clamp-2">{{ task.description || '暂无描述' }}</p>

            <!-- 批量任务进度 -->
            <div v-if="task.totalAssignees && task.totalAssignees > 1" class="mt-2">
              <div class="flex items-center gap-2 text-xs">
                <span class="text-gray-600">执行进度:</span>
                <div class="flex-1 max-w-xs">
                  <div class="h-1.5 bg-gray-200 rounded-full overflow-hidden">
                    <div
                      class="h-full bg-blue-500 transition-all"
                      :style="{ width: `${(task.submittedAssignees || 0) / task.totalAssignees * 100}%` }"
                    ></div>
                  </div>
                </div>
                <span class="text-gray-600 font-medium">
                  {{ task.submittedAssignees || 0 }}/{{ task.totalAssignees }} 已提交
                </span>
              </div>
            </div>

            <div class="mt-2 flex items-center gap-4 text-xs text-gray-400">
              <span>编号: {{ task.taskCode }}</span>
              <span>创建人: {{ task.assignerName }}</span>
              <span v-if="task.dueDate">截止: {{ formatDate(task.dueDate) }}</span>
            </div>
          </div>
          <div class="ml-4 flex flex-col items-end gap-2">
            <button
              v-if="task.status === 0"
              class="rounded-md bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700"
              @click.stop="handleAccept(task)"
            >
              接收任务
            </button>
            <button
              v-else-if="task.status === 1 || task.status === 4"
              class="rounded-md bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700"
              @click.stop="viewDetail(task)"
            >
              提交任务
            </button>
            <span class="text-xs text-gray-400">{{ formatDate(task.createdAt) }}</span>
          </div>
        </div>
      </div>

      <div v-if="taskList.length === 0" class="rounded-lg border border-gray-200 bg-white py-12 text-center">
        <InboxIcon class="mx-auto h-12 w-12 text-gray-300" />
        <p class="mt-2 text-sm text-gray-500">暂无任务</p>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > queryParams.pageSize" class="mt-4 flex items-center justify-center gap-2">
      <button
        :disabled="queryParams.pageNum === 1"
        class="rounded border px-3 py-1 text-sm disabled:opacity-50"
        @click="queryParams.pageNum--; loadTaskList()"
      >
        上一页
      </button>
      <span class="text-sm text-gray-500">{{ queryParams.pageNum }} / {{ Math.ceil(total / queryParams.pageSize) }}</span>
      <button
        :disabled="queryParams.pageNum * queryParams.pageSize >= total"
        class="rounded border px-3 py-1 text-sm disabled:opacity-50"
        @click="queryParams.pageNum++; loadTaskList()"
      >
        下一页
      </button>
    </div>

    <!-- 任务详情对话框 -->
    <TaskDetailDialog
      v-model:visible="detailDialogVisible"
      :task-id="selectedTaskId"
      @refresh="loadTaskList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { InboxIcon } from '@heroicons/vue/24/outline'
import { getMyTasks, acceptTask, type TaskDTO } from '@/api/task'
import TaskDetailDialog from './components/TaskDetailDialog.vue'

// 状态标签
const statusTabs = ref([
  { label: '全部', value: undefined as number | undefined, count: 0 },
  { label: '待接收', value: 0, count: 0 },
  { label: '进行中', value: 1, count: 0 },
  { label: '待审核', value: 2, count: 0 },
  { label: '已打回', value: 4, count: 0 },
  { label: '已完成', value: 3, count: 0 }
])

const currentStatus = ref<number | undefined>(undefined)

// 搜索和筛选
const searchKeyword = ref('')
const filterPriority = ref<number | undefined>(undefined)
const filterDueDate = ref('')
const sortBy = ref('createdAt')

// 排序选项
const sortOptions = [
  { label: '创建时间', value: 'createdAt' },
  { label: '截止时间', value: 'dueDate' },
  { label: '优先级', value: 'priority' }
]

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  status: undefined as number | undefined
})

// 数据
const taskList = ref<TaskDTO[]>([])
const total = ref(0)

// 对话框
const detailDialogVisible = ref(false)
const selectedTaskId = ref<number>()

// 切换状态
const changeStatus = (status: number | undefined) => {
  currentStatus.value = status
  queryParams.status = status
  queryParams.pageNum = 1
  loadTaskList()
}

// 加载任务列表
const loadTaskList = async () => {
  try {
    const res = await getMyTasks(queryParams)
    let records = res.records

    // 客户端筛选（关键词搜索）
    if (searchKeyword.value) {
      const keyword = searchKeyword.value.toLowerCase()
      records = records.filter(task =>
        task.title.toLowerCase().includes(keyword) ||
        task.taskCode.toLowerCase().includes(keyword)
      )
    }

    // 客户端筛选（优先级）
    if (filterPriority.value !== undefined) {
      records = records.filter(task => task.priority === filterPriority.value)
    }

    // 客户端筛选（截止日期）
    if (filterDueDate.value) {
      const now = new Date()
      const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      const weekEnd = new Date(today.getTime() + 7 * 24 * 60 * 60 * 1000)
      const monthEnd = new Date(now.getFullYear(), now.getMonth() + 1, 0)

      records = records.filter(task => {
        if (!task.dueDate) return false
        const dueDate = new Date(task.dueDate)

        switch (filterDueDate.value) {
          case 'today':
            return dueDate >= today && dueDate < new Date(today.getTime() + 24 * 60 * 60 * 1000)
          case 'week':
            return dueDate >= today && dueDate <= weekEnd
          case 'month':
            return dueDate >= today && dueDate <= monthEnd
          case 'overdue':
            return task.overdue === true
          default:
            return true
        }
      })
    }

    // 客户端排序
    records = [...records].sort((a, b) => {
      switch (sortBy.value) {
        case 'dueDate':
          if (!a.dueDate) return 1
          if (!b.dueDate) return -1
          return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime()
        case 'priority':
          return a.priority - b.priority
        case 'createdAt':
        default:
          return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      }
    })

    taskList.value = records
    total.value = records.length
  } catch (error) {
    console.error('加载任务列表失败', error)
  }
}

// 搜索
const handleSearch = () => {
  queryParams.pageNum = 1
  loadTaskList()
}

// 切换排序
const changeSortBy = (sort: string) => {
  sortBy.value = sort
  loadTaskList()
}

// 接收任务
const handleAccept = async (task: TaskDTO) => {
  try {
    await acceptTask(task.id)
    loadTaskList()
  } catch (error) {
    console.error('接收任务失败', error)
    alert('接收任务失败')
  }
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
    5: 'bg-gray-100 text-gray-800'
  }
  return classes[status] || 'bg-gray-100 text-gray-800'
}

onMounted(() => {
  loadTaskList()
})
</script>
