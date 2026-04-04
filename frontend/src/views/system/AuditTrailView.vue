<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 筛选栏 -->
    <div class="mb-4 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-end gap-4">
        <div class="w-36">
          <label class="mb-1 block text-sm text-gray-600">模块</label>
          <select
            v-model="queryParams.module"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          >
            <option :value="undefined">全部</option>
            <option v-for="m in MODULES" :key="m.value" :value="m.value">{{ m.label }}</option>
          </select>
        </div>
        <div class="w-32">
          <label class="mb-1 block text-sm text-gray-600">操作</label>
          <select
            v-model="queryParams.action"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          >
            <option :value="undefined">全部</option>
            <option v-for="a in ACTIONS" :key="a.value" :value="a.value">{{ a.label }}</option>
          </select>
        </div>
        <div class="w-36">
          <label class="mb-1 block text-sm text-gray-600">资源类型</label>
          <input
            v-model="queryParams.resourceType"
            type="text"
            placeholder="如 student"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            @keyup.enter="handleQuery"
          />
        </div>
        <div class="w-44">
          <label class="mb-1 block text-sm text-gray-600">操作人</label>
          <div class="relative">
            <input
              v-model="operatorKeyword"
              type="text"
              placeholder="搜索用户"
              class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
              @input="handleOperatorSearch"
              @focus="showOperatorDropdown = true"
            />
            <div
              v-if="showOperatorDropdown && operatorResults.length > 0"
              class="absolute z-10 mt-1 max-h-48 w-full overflow-auto rounded-lg border border-gray-200 bg-white shadow-lg"
            >
              <button
                v-for="u in operatorResults"
                :key="u.id"
                class="flex w-full items-center justify-between px-3 py-2 text-left text-sm hover:bg-blue-50"
                @click="selectOperator(u)"
              >
                <span class="text-gray-900">{{ u.realName || u.username }}</span>
                <span class="text-xs text-gray-400">{{ u.username }}</span>
              </button>
            </div>
          </div>
        </div>
        <div class="w-64">
          <label class="mb-1 block text-sm text-gray-600">时间范围</label>
          <div class="flex items-center gap-2">
            <input
              v-model="queryParams.startDate"
              type="date"
              class="h-9 w-full rounded-lg border border-gray-300 px-2 text-sm focus:border-blue-500 focus:outline-none"
            />
            <span class="text-gray-400">~</span>
            <input
              v-model="queryParams.endDate"
              type="date"
              class="h-9 w-full rounded-lg border border-gray-300 px-2 text-sm focus:border-blue-500 focus:outline-none"
            />
          </div>
        </div>
        <div class="flex gap-2">
          <button
            @click="handleQuery"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          >
            <Search class="h-4 w-4" />
            查询
          </button>
          <button
            @click="resetQuery"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            <RotateCcw class="h-4 w-4" />
            重置
          </button>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <div class="flex items-center gap-2">
          <span class="font-medium text-gray-900">审计日志</span>
          <span class="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-600">{{ total }} 条</span>
        </div>
      </div>

      <table class="w-full">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50">
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">时间</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">模块</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">操作</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">资源类型</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">资源ID</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">操作人</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">IP</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">描述</th>
          </tr>
        </thead>
        <tbody v-if="loading">
          <tr>
            <td colspan="8" class="py-16 text-center">
              <Loader2 class="mx-auto h-8 w-8 animate-spin text-blue-600" />
              <div class="mt-2 text-sm text-gray-500">加载中...</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else-if="list.length === 0">
          <tr>
            <td colspan="8" class="py-16 text-center">
              <FileText class="mx-auto h-12 w-12 text-gray-300" />
              <div class="mt-2 text-sm text-gray-500">暂无数据</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr
            v-for="row in list"
            :key="row.id"
            class="cursor-pointer border-b border-gray-100 hover:bg-gray-50"
            @click="handleRowClick(row)"
          >
            <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-500">{{ formatTime(row.createdAt || row.operationTime) }}</td>
            <td class="px-4 py-3">
              <span :class="moduleClass(row.module)">{{ row.module || '-' }}</span>
            </td>
            <td class="px-4 py-3">
              <span :class="actionClass(row.action)">{{ row.action || '-' }}</span>
            </td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ row.resourceType || '-' }}</td>
            <td class="px-4 py-3 font-mono text-xs text-gray-500">{{ row.resourceId || '-' }}</td>
            <td class="px-4 py-3 text-sm text-gray-700">{{ row.operatorName || row.operatorId || '-' }}</td>
            <td class="px-4 py-3 font-mono text-xs text-gray-500">{{ row.ip || row.ipAddress || '-' }}</td>
            <td class="max-w-[200px] truncate px-4 py-3 text-sm text-gray-600" :title="row.description">{{ row.description || '-' }}</td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <div class="text-sm text-gray-500">
          共 {{ total }} 条，第 {{ queryParams.pageNum }} / {{ Math.ceil(total / queryParams.pageSize) || 1 }} 页
        </div>
        <div class="flex items-center gap-2">
          <select
            v-model="queryParams.pageSize"
            @change="loadList"
            class="h-8 rounded border border-gray-300 px-2 text-sm"
          >
            <option v-for="size in [20, 50, 100]" :key="size" :value="size">{{ size }}条/页</option>
          </select>
          <div class="flex gap-1">
            <button
              @click="queryParams.pageNum = 1; loadList()"
              :disabled="queryParams.pageNum <= 1"
              class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50"
            >
              <ChevronsLeft class="h-4 w-4" />
            </button>
            <button
              @click="queryParams.pageNum--; loadList()"
              :disabled="queryParams.pageNum <= 1"
              class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50"
            >
              <ChevronLeft class="h-4 w-4" />
            </button>
            <button
              @click="queryParams.pageNum++; loadList()"
              :disabled="queryParams.pageNum >= Math.ceil(total / queryParams.pageSize)"
              class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50"
            >
              <ChevronRight class="h-4 w-4" />
            </button>
            <button
              @click="queryParams.pageNum = Math.ceil(total / queryParams.pageSize); loadList()"
              :disabled="queryParams.pageNum >= Math.ceil(total / queryParams.pageSize)"
              class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50"
            >
              <ChevronsRight class="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情抽屉 -->
    <Teleport to="body">
      <Transition name="drawer">
        <div v-if="drawerVisible" class="fixed inset-0 z-50 flex justify-end">
          <div class="fixed inset-0 bg-black/50" @click="drawerVisible = false"></div>
          <div class="relative w-full max-w-xl bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">审计详情</h3>
              <button @click="drawerVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div v-if="detailData" class="h-[calc(100vh-65px)] overflow-y-auto p-6">
              <!-- 基本信息 -->
              <div class="mb-6 grid grid-cols-2 gap-3">
                <div>
                  <div class="text-xs text-gray-400">时间</div>
                  <div class="text-sm text-gray-900">{{ formatTime(detailData.createdAt || detailData.operationTime) }}</div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">模块</div>
                  <div class="text-sm text-gray-900">{{ detailData.module || '-' }}</div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">操作</div>
                  <div class="text-sm text-gray-900">{{ detailData.action || '-' }}</div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">资源类型</div>
                  <div class="text-sm text-gray-900">{{ detailData.resourceType || '-' }}</div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">资源ID</div>
                  <div class="font-mono text-sm text-gray-900">{{ detailData.resourceId || '-' }}</div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">操作人</div>
                  <div class="text-sm text-gray-900">{{ detailData.operatorName || detailData.operatorId || '-' }}</div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">IP地址</div>
                  <div class="font-mono text-sm text-gray-900">{{ detailData.ip || detailData.ipAddress || '-' }}</div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">描述</div>
                  <div class="text-sm text-gray-900">{{ detailData.description || '-' }}</div>
                </div>
              </div>

              <!-- Before / After -->
              <div v-if="detailData.beforeData || detailData.beforeSnapshot" class="mb-4">
                <div class="mb-2 text-sm font-medium text-gray-700">变更前 (Before)</div>
                <pre class="max-h-60 overflow-auto rounded-lg bg-gray-50 p-3 text-xs text-gray-700">{{ formatJson(detailData.beforeData || detailData.beforeSnapshot) }}</pre>
              </div>
              <div v-if="detailData.afterData || detailData.afterSnapshot">
                <div class="mb-2 text-sm font-medium text-gray-700">变更后 (After)</div>
                <pre class="max-h-60 overflow-auto rounded-lg bg-gray-50 p-3 text-xs text-gray-700">{{ formatJson(detailData.afterData || detailData.afterSnapshot) }}</pre>
              </div>
              <div v-if="!detailData.beforeData && !detailData.beforeSnapshot && !detailData.afterData && !detailData.afterSnapshot" class="py-8 text-center text-sm text-gray-400">
                无变更快照数据
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, RotateCcw, X, Loader2, FileText,
  ChevronsLeft, ChevronLeft, ChevronRight, ChevronsRight
} from 'lucide-vue-next'
import { auditApi } from '@/api/audit'
import { getSimpleUserList } from '@/api/user'
import type { SimpleUser } from '@/types/user'

const MODULES = [
  { value: 'academic', label: '教务' },
  { value: 'student', label: '学生' },
  { value: 'teaching', label: '教学' },
  { value: 'enrollment', label: '招生' },
  { value: 'system', label: '系统' },
  { value: 'organization', label: '组织' },
  { value: 'inspection', label: '检查' },
]

const ACTIONS = [
  { value: 'CREATE', label: '新增' },
  { value: 'UPDATE', label: '修改' },
  { value: 'DELETE', label: '删除' },
  { value: 'PUBLISH', label: '发布' },
]

// State
const loading = ref(false)
const drawerVisible = ref(false)
const list = ref<any[]>([])
const total = ref(0)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
  module: undefined as string | undefined,
  action: undefined as string | undefined,
  resourceType: undefined as string | undefined,
  operatorId: undefined as number | undefined,
  startDate: undefined as string | undefined,
  endDate: undefined as string | undefined
})

// Operator search
const operatorKeyword = ref('')
const operatorResults = ref<SimpleUser[]>([])
const showOperatorDropdown = ref(false)

let searchTimer: ReturnType<typeof setTimeout> | null = null
const handleOperatorSearch = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    if (!operatorKeyword.value || operatorKeyword.value.length < 1) {
      operatorResults.value = []
      return
    }
    try {
      operatorResults.value = await getSimpleUserList(operatorKeyword.value)
    } catch {
      operatorResults.value = []
    }
  }, 300)
}

const selectOperator = (u: SimpleUser) => {
  queryParams.operatorId = Number(u.id)
  operatorKeyword.value = u.realName || u.username
  showOperatorDropdown.value = false
}

// Detail
const detailData = ref<any>(null)

// Methods
const formatTime = (t: string | undefined): string => {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
}

const formatJson = (data: any): string => {
  if (!data) return ''
  try {
    const parsed = typeof data === 'string' ? JSON.parse(data) : data
    return JSON.stringify(parsed, null, 2)
  } catch {
    return String(data)
  }
}

const moduleClass = (mod: string): string => {
  const map: Record<string, string> = {
    academic: 'rounded bg-blue-50 px-1.5 py-0.5 text-xs text-blue-600',
    student: 'rounded bg-green-50 px-1.5 py-0.5 text-xs text-green-600',
    teaching: 'rounded bg-purple-50 px-1.5 py-0.5 text-xs text-purple-600',
    enrollment: 'rounded bg-orange-50 px-1.5 py-0.5 text-xs text-orange-600',
    system: 'rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-600',
    organization: 'rounded bg-cyan-50 px-1.5 py-0.5 text-xs text-cyan-600',
    inspection: 'rounded bg-amber-50 px-1.5 py-0.5 text-xs text-amber-600',
  }
  return map[mod] || 'rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-500'
}

const actionClass = (action: string): string => {
  const map: Record<string, string> = {
    CREATE: 'rounded bg-green-50 px-1.5 py-0.5 text-xs text-green-600',
    UPDATE: 'rounded bg-blue-50 px-1.5 py-0.5 text-xs text-blue-600',
    DELETE: 'rounded bg-red-50 px-1.5 py-0.5 text-xs text-red-600',
    PUBLISH: 'rounded bg-purple-50 px-1.5 py-0.5 text-xs text-purple-600',
  }
  return map[action] || 'rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-500'
}

const loadList = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize
    }
    if (queryParams.module) params.module = queryParams.module
    if (queryParams.action) params.action = queryParams.action
    if (queryParams.resourceType) params.resourceType = queryParams.resourceType
    if (queryParams.operatorId) params.operatorId = queryParams.operatorId
    if (queryParams.startDate) params.startDate = queryParams.startDate
    if (queryParams.endDate) params.endDate = queryParams.endDate

    const res = await auditApi.list(params)
    list.value = res?.records || res || []
    total.value = res?.total || (Array.isArray(res) ? res.length : 0)
  } catch {
    ElMessage.error('加载审计日志失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  loadList()
}

const resetQuery = () => {
  queryParams.module = undefined
  queryParams.action = undefined
  queryParams.resourceType = undefined
  queryParams.operatorId = undefined
  queryParams.startDate = undefined
  queryParams.endDate = undefined
  operatorKeyword.value = ''
  queryParams.pageNum = 1
  loadList()
}

const handleRowClick = (row: any) => {
  detailData.value = row
  drawerVisible.value = true
}

// Close dropdown on outside click
const handleClickOutside = () => {
  setTimeout(() => { showOperatorDropdown.value = false }, 200)
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  loadList()
})
</script>

<style scoped>
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.drawer-enter-active, .drawer-leave-active { transition: all 0.3s ease; }
.drawer-enter-from, .drawer-leave-to { opacity: 0; transform: translateX(100%); }
</style>
