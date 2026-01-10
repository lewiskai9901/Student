<template>
  <div class="min-h-screen bg-gray-50/50 p-6">
    <!-- 页面头部 -->
    <div class="mb-6 rounded-xl bg-gradient-to-r from-orange-500 to-amber-500 p-6 shadow-lg">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="flex items-center gap-3 text-2xl font-bold text-white">
            <Bell class="h-8 w-8" />
            公告管理
          </h1>
          <p class="mt-1 text-orange-100">发布和管理系统公告通知</p>
        </div>
        <div class="flex gap-3">
          <button
            @click="loadAnnouncements"
            class="flex items-center gap-2 rounded-lg bg-white/20 px-4 py-2 font-medium text-white transition-all hover:bg-white/30"
          >
            <RefreshCw class="h-5 w-5" />
            刷新
          </button>
          <button
            @click="handleAdd"
            class="flex items-center gap-2 rounded-lg bg-white/95 px-4 py-2 font-medium text-orange-600 shadow-md transition-all hover:-translate-y-0.5 hover:bg-white hover:shadow-lg"
          >
            <Plus class="h-5 w-5" />
            发布公告
          </button>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-4 gap-4">
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">公告总数</p>
            <p class="mt-1 text-2xl font-bold text-gray-900">{{ total }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-orange-100 text-orange-600">
            <Bell class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-orange-500 to-amber-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">已发布</p>
            <p class="mt-1 text-2xl font-bold text-green-600">{{ stats.published }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-green-100 text-green-600">
            <CheckCircle class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-green-500 to-emerald-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">草稿箱</p>
            <p class="mt-1 text-2xl font-bold text-gray-500">{{ stats.draft }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-gray-100 text-gray-500">
            <FileEdit class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-gray-400 to-gray-300 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">置顶公告</p>
            <p class="mt-1 text-2xl font-bold text-red-600">{{ stats.pinned }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-red-100 text-red-600">
            <Pin class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-red-500 to-rose-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
    </div>

    <!-- 表格卡片 -->
    <div class="rounded-xl bg-white shadow-sm">
      <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4">
        <div class="flex items-center gap-3">
          <h3 class="font-semibold text-gray-900">公告列表</h3>
          <span class="rounded-full bg-orange-100 px-2.5 py-0.5 text-xs font-medium text-orange-700">
            {{ total }} 条记录
          </span>
        </div>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="border-b border-gray-100 bg-gray-50/50">
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">标题</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">类型</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">优先级</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">发布人</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">发布时间</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">浏览</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">状态</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">置顶</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">操作</th>
            </tr>
          </thead>
          <tbody v-if="loading">
            <tr>
              <td colspan="9" class="py-20 text-center">
                <div class="flex flex-col items-center gap-3">
                  <Loader2 class="h-8 w-8 animate-spin text-orange-600" />
                  <span class="text-sm text-gray-500">加载中...</span>
                </div>
              </td>
            </tr>
          </tbody>
          <tbody v-else-if="announcements.length === 0">
            <tr>
              <td colspan="9" class="py-20 text-center">
                <div class="flex flex-col items-center gap-3">
                  <Bell class="h-12 w-12 text-gray-300" />
                  <span class="text-sm text-gray-500">暂无公告数据</span>
                </div>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr
              v-for="(row, index) in announcements"
              :key="row.id"
              class="animate-fade-in border-b border-gray-50 transition-colors hover:bg-orange-50/30"
              :style="{ animationDelay: `${index * 50}ms` }"
            >
              <td class="max-w-xs px-4 py-3">
                <div class="flex items-center gap-3">
                  <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-orange-100">
                    <Bell class="h-5 w-5 text-orange-600" />
                  </div>
                  <span class="truncate font-medium text-gray-900" :title="row.title">{{ row.title }}</span>
                </div>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span :class="getTypeClass(row.announcementType)">
                  {{ getTypeText(row.announcementType) }}
                </span>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span :class="getPriorityClass(row.priority)">
                  {{ getPriorityText(row.priority) }}
                </span>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-gray-600">{{ row.publisherName }}</td>
              <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-500">{{ row.publishTime }}</td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span class="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">
                  {{ row.viewCount || 0 }}
                </span>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span :class="['rounded-full px-2.5 py-0.5 text-xs font-medium', row.isPublished === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600']">
                  {{ row.isPublished === 1 ? '已发布' : '草稿' }}
                </span>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <Pin v-if="row.isPinned === 1" class="mx-auto h-5 w-5 text-red-500" />
                <span v-else class="text-gray-300">-</span>
              </td>
              <td class="whitespace-nowrap px-4 py-3">
                <div class="flex items-center justify-center gap-1">
                  <button
                    v-if="row.isPublished === 0"
                    @click="handlePublish(row)"
                    class="rounded-lg p-1.5 text-gray-500 transition-colors hover:bg-green-100 hover:text-green-600"
                    title="发布"
                  >
                    <Send class="h-4 w-4" />
                  </button>
                  <button
                    v-else
                    @click="handleRevoke(row)"
                    class="rounded-lg p-1.5 text-gray-500 transition-colors hover:bg-amber-100 hover:text-amber-600"
                    title="撤回"
                  >
                    <Undo2 class="h-4 w-4" />
                  </button>
                  <button
                    @click="handlePin(row)"
                    class="rounded-lg p-1.5 text-gray-500 transition-colors hover:bg-red-100 hover:text-red-600"
                    :title="row.isPinned === 1 ? '取消置顶' : '置顶'"
                  >
                    <Pin class="h-4 w-4" />
                  </button>
                  <button
                    @click="handleEdit(row)"
                    class="rounded-lg p-1.5 text-gray-500 transition-colors hover:bg-orange-100 hover:text-orange-600"
                    title="编辑"
                  >
                    <Pencil class="h-4 w-4" />
                  </button>
                  <button
                    @click="handleDelete(row)"
                    class="rounded-lg p-1.5 text-gray-500 transition-colors hover:bg-red-100 hover:text-red-600"
                    title="删除"
                  >
                    <Trash2 class="h-4 w-4" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="flex items-center justify-between border-t border-gray-100 px-5 py-4">
        <div class="text-sm text-gray-500">
          共 {{ total }} 条记录，第 {{ pageNum }} / {{ Math.ceil(total / pageSize) }} 页
        </div>
        <div class="flex items-center gap-2">
          <button
            @click="pageNum = 1; loadAnnouncements()"
            :disabled="pageNum <= 1"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            首页
          </button>
          <button
            @click="pageNum--; loadAnnouncements()"
            :disabled="pageNum <= 1"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            <ChevronLeft class="h-4 w-4" />
          </button>
          <button
            @click="pageNum++; loadAnnouncements()"
            :disabled="pageNum >= Math.ceil(total / pageSize)"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            <ChevronRight class="h-4 w-4" />
          </button>
          <button
            @click="pageNum = Math.ceil(total / pageSize); loadAnnouncements()"
            :disabled="pageNum >= Math.ceil(total / pageSize)"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            末页
          </button>
          <select
            v-model="pageSize"
            @change="pageNum = 1; loadAnnouncements()"
            class="pagination-select"
          >
            <option :value="10">10条/页</option>
            <option :value="20">20条/页</option>
            <option :value="50">50条/页</option>
            <option :value="100">100条/页</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <Teleport to="body">
      <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50 backdrop-blur-sm" @click="dialogVisible = false"></div>
        <div class="relative z-10 w-full max-w-2xl rounded-xl bg-white shadow-2xl">
          <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
            <h3 class="text-lg font-semibold text-gray-900">{{ dialogTitle }}</h3>
            <button @click="dialogVisible = false" class="rounded-lg p-1 hover:bg-gray-100">
              <X class="h-5 w-5 text-gray-500" />
            </button>
          </div>
          <div class="max-h-[70vh] overflow-y-auto p-6">
            <div class="space-y-4">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  标题 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="formData.title"
                  type="text"
                  placeholder="请输入公告标题"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-orange-500 focus:outline-none focus:ring-2 focus:ring-orange-500/20"
                />
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">类型</label>
                  <div class="flex gap-4 py-2">
                    <label class="flex cursor-pointer items-center gap-2">
                      <input v-model="formData.announcementType" type="radio" value="notice" class="h-4 w-4 border-gray-300 text-orange-600" />
                      <span class="text-sm text-gray-700">通知</span>
                    </label>
                    <label class="flex cursor-pointer items-center gap-2">
                      <input v-model="formData.announcementType" type="radio" value="announcement" class="h-4 w-4 border-gray-300 text-orange-600" />
                      <span class="text-sm text-gray-700">公告</span>
                    </label>
                    <label class="flex cursor-pointer items-center gap-2">
                      <input v-model="formData.announcementType" type="radio" value="warning" class="h-4 w-4 border-gray-300 text-orange-600" />
                      <span class="text-sm text-gray-700">警告</span>
                    </label>
                  </div>
                </div>
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">优先级</label>
                  <div class="flex gap-4 py-2">
                    <label class="flex cursor-pointer items-center gap-2">
                      <input v-model="formData.priority" type="radio" :value="1" class="h-4 w-4 border-gray-300 text-orange-600" />
                      <span class="text-sm text-gray-700">普通</span>
                    </label>
                    <label class="flex cursor-pointer items-center gap-2">
                      <input v-model="formData.priority" type="radio" :value="2" class="h-4 w-4 border-gray-300 text-orange-600" />
                      <span class="text-sm text-gray-700">重要</span>
                    </label>
                    <label class="flex cursor-pointer items-center gap-2">
                      <input v-model="formData.priority" type="radio" :value="3" class="h-4 w-4 border-gray-300 text-orange-600" />
                      <span class="text-sm text-gray-700">紧急</span>
                    </label>
                  </div>
                </div>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  内容 <span class="text-red-500">*</span>
                </label>
                <textarea
                  v-model="formData.content"
                  rows="6"
                  placeholder="请输入公告内容"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-orange-500 focus:outline-none focus:ring-2 focus:ring-orange-500/20"
                ></textarea>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">开始时间</label>
                  <input
                    v-model="formData.startTime"
                    type="datetime-local"
                    class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-orange-500 focus:outline-none focus:ring-2 focus:ring-orange-500/20"
                  />
                </div>
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">结束时间</label>
                  <input
                    v-model="formData.endTime"
                    type="datetime-local"
                    class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-orange-500 focus:outline-none focus:ring-2 focus:ring-orange-500/20"
                  />
                </div>
              </div>
            </div>
          </div>
          <div class="flex justify-end gap-3 border-t border-gray-100 px-6 py-4">
            <button
              @click="dialogVisible = false"
              class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
            >
              取消
            </button>
            <button
              @click="handleSubmit"
              class="rounded-lg bg-gradient-to-r from-orange-500 to-amber-500 px-4 py-2 text-sm font-medium text-white transition-all hover:-translate-y-0.5 hover:shadow-md"
            >
              确定
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell,
  Plus,
  RefreshCw,
  Pencil,
  Trash2,
  X,
  Loader2,
  ChevronLeft,
  ChevronRight,
  CheckCircle,
  FileEdit,
  Pin,
  Send,
  Undo2
} from 'lucide-vue-next'
import { http } from '@/utils/request'

const loading = ref(false)
const announcements = ref<any[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 统计数据
const stats = computed(() => {
  return {
    published: announcements.value.filter(a => a.isPublished === 1).length,
    draft: announcements.value.filter(a => a.isPublished === 0).length,
    pinned: announcements.value.filter(a => a.isPinned === 1).length
  }
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)

const formData = reactive({
  id: null as number | null,
  title: '',
  content: '',
  announcementType: 'notice',
  priority: 1,
  startTime: null as string | null,
  endTime: null as string | null
})

// 获取类型样式
const getTypeClass = (type: string) => {
  const classes: Record<string, string> = {
    notice: 'rounded-full bg-blue-100 px-2.5 py-0.5 text-xs font-medium text-blue-700',
    announcement: 'rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-700',
    warning: 'rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-medium text-amber-700'
  }
  return classes[type] || classes.notice
}

const getTypeText = (type: string) => {
  const texts: Record<string, string> = { notice: '通知', announcement: '公告', warning: '警告' }
  return texts[type] || '通知'
}

// 获取优先级样式
const getPriorityClass = (priority: number) => {
  const classes: Record<number, string> = {
    1: 'rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-medium text-gray-600',
    2: 'rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-medium text-amber-700',
    3: 'rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-medium text-red-700'
  }
  return classes[priority] || classes[1]
}

const getPriorityText = (priority: number) => {
  const texts: Record<number, string> = { 1: '普通', 2: '重要', 3: '紧急' }
  return texts[priority] || '普通'
}

const loadAnnouncements = async () => {
  loading.value = true
  try {
    const response = await http.get<{ records: any[]; total: number }>('/system/announcements', {
      params: { pageNum: pageNum.value, pageSize: pageSize.value }
    })
    announcements.value = response.records || []
    total.value = response.total || 0
  } catch (error) {
    ElMessage.error('获取公告列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '发布公告'
  Object.assign(formData, {
    id: null,
    title: '',
    content: '',
    announcementType: 'notice',
    priority: 1,
    startTime: null,
    endTime: null
  })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  dialogTitle.value = '编辑公告'
  Object.assign(formData, {
    id: row.id,
    title: row.title,
    content: row.content,
    announcementType: row.announcementType,
    priority: row.priority,
    startTime: row.startTime,
    endTime: row.endTime
  })
  dialogVisible.value = true
}

const handlePublish = async (row: any) => {
  try {
    await http.post(`/system/announcements/${row.id}/publish`)
    ElMessage.success('发布成功')
    loadAnnouncements()
  } catch (error) {
    ElMessage.error('发布失败')
  }
}

const handleRevoke = async (row: any) => {
  try {
    await http.post(`/system/announcements/${row.id}/revoke`)
    ElMessage.success('撤回成功')
    loadAnnouncements()
  } catch (error) {
    ElMessage.error('撤回失败')
  }
}

const handlePin = async (row: any) => {
  try {
    await http.post(`/system/announcements/${row.id}/pin`, null, {
      params: { pinned: row.isPinned !== 1 }
    })
    ElMessage.success('操作成功')
    loadAnnouncements()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定删除公告 "${row.title}" 吗?`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await http.delete(`/system/announcements/${row.id}`)
    ElMessage.success('删除成功')
    loadAnnouncements()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  if (!formData.title) {
    ElMessage.error('请输入标题')
    return
  }
  if (!formData.content) {
    ElMessage.error('请输入内容')
    return
  }

  try {
    if (isEdit.value) {
      await http.put(`/system/announcements/${formData.id}`, formData)
      ElMessage.success('更新成功')
    } else {
      await http.post('/system/announcements', formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadAnnouncements()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

onMounted(() => {
  loadAnnouncements()
})
</script>

<style scoped>
@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in {
  animation: fade-in 0.3s ease-out forwards;
  opacity: 0;
}
</style>
