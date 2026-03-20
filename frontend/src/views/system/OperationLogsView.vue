<template>
  <div class="min-h-screen bg-gray-50/50 p-6">
    <!-- 页面头部 -->
    <div class="mb-6 rounded-xl bg-gradient-to-r from-slate-700 to-slate-600 p-6 shadow-lg">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="flex items-center gap-3 text-2xl font-bold text-white">
            <ScrollText class="h-8 w-8" />
            操作日志
          </h1>
          <p class="mt-1 text-slate-300">查看系统操作记录与审计信息</p>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-4 gap-4">
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">日志总数</p>
            <p class="mt-1 text-2xl font-bold text-gray-900">{{ statsData.total }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-slate-100 text-slate-600">
            <ScrollText class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-slate-600 to-slate-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">成功操作</p>
            <p class="mt-1 text-2xl font-bold text-green-600">{{ statsData.success }}</p>
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
            <p class="text-sm font-medium text-gray-500">失败操作</p>
            <p class="mt-1 text-2xl font-bold text-red-600">{{ statsData.failed }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-red-100 text-red-600">
            <XCircle class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-red-500 to-rose-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">今日操作</p>
            <p class="mt-1 text-2xl font-bold text-blue-600">{{ statsData.today }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-100 text-blue-600">
            <Activity class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-blue-500 to-indigo-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="mb-6 rounded-xl bg-white p-5 shadow-sm">
      <div class="flex flex-wrap items-end gap-4">
        <div class="min-w-[140px]">
          <label class="mb-1.5 block text-sm font-medium text-gray-700">关键字</label>
          <input
            v-model="queryParams.keyword"
            type="text"
            placeholder="用户/资源名/描述"
            class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-500/20"
            @keyup.enter="handleQuery"
          />
        </div>
        <div class="min-w-[120px]">
          <label class="mb-1.5 block text-sm font-medium text-gray-700">模块</label>
          <select
            v-model="queryParams.module"
            class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-500/20"
          >
            <option value="">全部模块</option>
            <option value="organization">组织管理</option>
            <option value="place">场所管理</option>
            <option value="user">用户管理</option>
            <option value="access">权限管理</option>
            <option value="inspection">检查管理</option>
          </select>
        </div>
        <div class="min-w-[120px]">
          <label class="mb-1.5 block text-sm font-medium text-gray-700">操作类型</label>
          <select
            v-model="queryParams.action"
            class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-500/20"
          >
            <option value="">全部类型</option>
            <option value="CREATE">创建</option>
            <option value="UPDATE">更新</option>
            <option value="DELETE">删除</option>
            <option value="FREEZE">冻结</option>
            <option value="DISSOLVE">解散</option>
            <option value="ASSIGN">分配</option>
            <option value="CHECK_IN">入住</option>
            <option value="CHECK_OUT">退出</option>
            <option value="LOGIN">登录</option>
            <option value="LOGOUT">登出</option>
          </select>
        </div>
        <div class="min-w-[120px]">
          <label class="mb-1.5 block text-sm font-medium text-gray-700">结果</label>
          <select
            v-model="queryParams.result"
            class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-500/20"
          >
            <option value="">全部</option>
            <option value="SUCCESS">成功</option>
            <option value="FAILURE">失败</option>
          </select>
        </div>
        <div class="flex gap-2">
          <button
            @click="handleQuery"
            class="flex items-center gap-2 rounded-lg bg-gradient-to-r from-slate-700 to-slate-600 px-4 py-2 text-sm font-medium text-white shadow-sm transition-all hover:-translate-y-0.5 hover:shadow-md"
          >
            <Search class="h-4 w-4" />
            搜索
          </button>
          <button
            @click="resetQuery"
            class="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 transition-all hover:bg-gray-50"
          >
            <RotateCcw class="h-4 w-4" />
            重置
          </button>
        </div>
      </div>
    </div>

    <!-- 表格卡片 -->
    <div class="rounded-xl bg-white shadow-sm">
      <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4">
        <div class="flex items-center gap-3">
          <h3 class="font-semibold text-gray-900">日志列表</h3>
          <span class="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-700">
            {{ total }} 条记录
          </span>
        </div>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="border-b border-gray-100 bg-gray-50/50">
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">用户</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">模块</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">操作</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">资源</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">描述</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">方法</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">结果</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">IP</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">时间</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">详情</th>
            </tr>
          </thead>
          <tbody v-if="loading">
            <tr>
              <td colspan="10" class="py-20 text-center">
                <div class="flex flex-col items-center gap-3">
                  <Loader2 class="h-8 w-8 animate-spin text-slate-600" />
                  <span class="text-sm text-gray-500">加载中...</span>
                </div>
              </td>
            </tr>
          </tbody>
          <tbody v-else-if="logList.length === 0">
            <tr>
              <td colspan="10" class="py-20 text-center">
                <div class="flex flex-col items-center gap-3">
                  <ScrollText class="h-12 w-12 text-gray-300" />
                  <span class="text-sm text-gray-500">暂无日志数据</span>
                </div>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr
              v-for="(row, index) in logList"
              :key="row.id"
              class="animate-fade-in border-b border-gray-50 transition-colors hover:bg-slate-50/50"
              :style="{ animationDelay: `${index * 30}ms` }"
            >
              <td class="whitespace-nowrap px-4 py-3">
                <div class="flex items-center gap-2">
                  <div class="flex h-8 w-8 items-center justify-center rounded-full bg-slate-100 text-xs font-medium text-slate-600">
                    {{ (row.userName || '?').charAt(0) }}
                  </div>
                  <div class="font-medium text-gray-900">{{ row.userName || '-' }}</div>
                </div>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span :class="getModuleClass(row.module)">
                  {{ getModuleName(row.module) }}
                </span>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span :class="getActionClass(row.action)">
                  {{ getActionName(row.action) }}
                </span>
              </td>
              <td class="whitespace-nowrap px-4 py-3">
                <div class="text-sm text-gray-600">{{ row.resourceName || getResourceTypeName(row.resourceType) }}</div>
                <div v-if="row.resourceId" class="text-xs text-gray-400">{{ getResourceTypeName(row.resourceType) }} #{{ row.resourceId }}</div>
                <div v-else class="text-xs text-gray-400">{{ getResourceTypeName(row.resourceType) }}</div>
              </td>
              <td class="max-w-xs truncate px-4 py-3 text-gray-600" :title="row.actionLabel">
                {{ row.actionLabel || row.action }}
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span :class="getMethodClass(row.httpMethod)">
                  {{ row.httpMethod || '-' }}
                </span>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span :class="getResultClass(row.result)">
                  {{ row.result === 'SUCCESS' ? '成功' : row.result === 'FAILURE' ? '失败' : row.result }}
                </span>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">{{ row.sourceIp || '-' }}</td>
              <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-500">{{ formatTime(row.occurredAt) }}</td>
              <td class="whitespace-nowrap px-4 py-3">
                <button
                  @click="handleView(row)"
                  class="rounded-lg p-1.5 text-gray-500 transition-colors hover:bg-blue-100 hover:text-blue-600"
                  title="详情"
                >
                  <Eye class="h-4 w-4" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="flex items-center justify-between border-t border-gray-100 px-5 py-4">
        <div class="text-sm text-gray-500">
          共 {{ total }} 条记录，第 {{ queryParams.pageNum }} / {{ Math.ceil(total / queryParams.pageSize) }} 页
        </div>
        <div class="flex items-center gap-2">
          <button
            @click="queryParams.pageNum = 1; getLogList()"
            :disabled="queryParams.pageNum <= 1"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            首页
          </button>
          <button
            @click="queryParams.pageNum--; getLogList()"
            :disabled="queryParams.pageNum <= 1"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            <ChevronLeft class="h-4 w-4" />
          </button>
          <button
            @click="queryParams.pageNum++; getLogList()"
            :disabled="queryParams.pageNum >= Math.ceil(total / queryParams.pageSize)"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            <ChevronRight class="h-4 w-4" />
          </button>
          <button
            @click="queryParams.pageNum = Math.ceil(total / queryParams.pageSize); getLogList()"
            :disabled="queryParams.pageNum >= Math.ceil(total / queryParams.pageSize)"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            末页
          </button>
          <select
            v-model="queryParams.pageSize"
            @change="queryParams.pageNum = 1; getLogList()"
            class="rounded-lg border border-gray-300 px-2 py-1.5 text-sm"
          >
            <option :value="10">10条/页</option>
            <option :value="20">20条/页</option>
            <option :value="50">50条/页</option>
            <option :value="100">100条/页</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 详情对话框 -->
    <Teleport to="body">
      <div v-if="detailVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50 backdrop-blur-sm" @click="detailVisible = false"></div>
        <div class="relative z-10 w-full max-w-3xl rounded-xl bg-white shadow-2xl">
          <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
            <h3 class="text-lg font-semibold text-gray-900">事件详情</h3>
            <button @click="detailVisible = false" class="rounded-lg p-1 hover:bg-gray-100">
              <X class="h-5 w-5 text-gray-500" />
            </button>
          </div>
          <div class="max-h-[70vh] overflow-y-auto p-6">
            <div class="grid grid-cols-2 gap-4">
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">事件ID</div>
                <div class="font-mono text-sm text-gray-900">{{ currentLog.id }}</div>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">操作人</div>
                <div class="text-gray-900">{{ currentLog.userName || '-' }}</div>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">模块</div>
                <span :class="getModuleClass(currentLog.module)">
                  {{ getModuleName(currentLog.module) }}
                </span>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">操作</div>
                <span :class="getActionClass(currentLog.action)">
                  {{ getActionName(currentLog.action) }}
                </span>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">资源类型</div>
                <div class="text-gray-900">{{ getResourceTypeName(currentLog.resourceType) }}</div>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">资源ID</div>
                <div class="font-mono text-sm text-gray-900">{{ currentLog.resourceId || '-' }}</div>
              </div>
              <div v-if="currentLog.actionLabel" class="col-span-2 rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">操作描述</div>
                <div class="text-gray-900">{{ currentLog.actionLabel }}</div>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">请求方法</div>
                <span :class="getMethodClass(currentLog.httpMethod)">{{ currentLog.httpMethod || '-' }}</span>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">结果</div>
                <span :class="getResultClass(currentLog.result)">
                  {{ currentLog.result === 'SUCCESS' ? '成功' : currentLog.result === 'FAILURE' ? '失败' : currentLog.result }}
                </span>
              </div>
              <div v-if="currentLog.apiEndpoint" class="col-span-2 rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">API端点</div>
                <div class="font-mono text-sm text-gray-900 break-all">{{ currentLog.apiEndpoint }}</div>
              </div>
              <div v-if="currentLog.changedFields?.length" class="col-span-2 rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-2">变更字段</div>
                <div class="space-y-1">
                  <div v-for="(fc, i) in currentLog.changedFields" :key="i" class="text-xs text-gray-600">
                    <span class="font-medium text-gray-700">{{ getFieldLabel(fc.fieldName) }}</span>:
                    <span v-if="fc.oldValue" class="text-red-500 line-through">{{ fc.oldValue }}</span>
                    <span v-if="fc.oldValue && fc.newValue"> → </span>
                    <span v-if="fc.newValue" class="text-green-600">{{ fc.newValue }}</span>
                  </div>
                </div>
              </div>
              <div v-if="currentLog.reason" class="col-span-2 rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">原因</div>
                <div class="text-gray-900">{{ currentLog.reason }}</div>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">IP地址</div>
                <div class="text-gray-900">{{ currentLog.sourceIp || '-' }}</div>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">时间</div>
                <div class="text-gray-900">{{ formatTime(currentLog.occurredAt) }}</div>
              </div>
              <div v-if="currentLog.errorMessage" class="col-span-2 rounded-lg bg-red-50 p-4">
                <div class="text-sm text-red-500 mb-1">错误信息</div>
                <div class="text-red-700">{{ currentLog.errorMessage }}</div>
              </div>
            </div>
          </div>
          <div class="flex justify-end border-t border-gray-100 px-6 py-4">
            <button
              @click="detailVisible = false"
              class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
            >
              关闭
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ScrollText,
  Search,
  RotateCcw,
  Eye,
  X,
  Loader2,
  ChevronLeft,
  ChevronRight,
  CheckCircle,
  XCircle,
  Activity
} from 'lucide-vue-next'
import { listActivityEvents, getActivityStats } from '@/api/activityEvent'
import type { ActivityEvent, ActivityEventStats } from '@/types/activityEvent'

const queryParams = reactive({
  keyword: '',
  module: '',
  action: '',
  result: '',
  pageNum: 1,
  pageSize: 20
})

const loading = ref(false)
const logList = ref<ActivityEvent[]>([])
const total = ref(0)
const statsData = ref<ActivityEventStats>({ total: 0, success: 0, failed: 0, today: 0 })

const detailVisible = ref(false)
const currentLog = ref<ActivityEvent>({} as ActivityEvent)

// Module styles
const getModuleClass = (module: string) => {
  const classes: Record<string, string> = {
    organization: 'rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700',
    place: 'rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700',
    user: 'rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700',
    access: 'rounded-full bg-purple-100 px-2 py-0.5 text-xs font-medium text-purple-700',
    inspection: 'rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700'
  }
  return classes[module] || 'rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600'
}

const getModuleName = (module: string) => {
  const map: Record<string, string> = {
    organization: '组织', place: '场所', user: '用户',
    access: '权限', inspection: '检查', student: '学生'
  }
  return map[module] || module || '-'
}

// Action styles
const getActionClass = (action: string) => {
  const classes: Record<string, string> = {
    CREATE: 'rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700',
    UPDATE: 'rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700',
    DELETE: 'rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700',
    LOGIN: 'rounded-full bg-indigo-100 px-2 py-0.5 text-xs font-medium text-indigo-700',
    LOGOUT: 'rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600',
    FREEZE: 'rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700',
    ASSIGN: 'rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700',
    CHECK_IN: 'rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700',
    CHECK_OUT: 'rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700',
    TRANSFER: 'rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700',
    APPOINT: 'rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700'
  }
  return classes[action] || 'rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600'
}

const getActionName = (action: string) => {
  const map: Record<string, string> = {
    CREATE: '创建', UPDATE: '更新', DELETE: '删除', LOGIN: '登录',
    LOGOUT: '登出', FREEZE: '冻结', UNFREEZE: '解冻', DISSOLVE: '解散',
    MERGE: '合并', SPLIT: '拆分', ASSIGN: '分配', TRANSFER: '调岗',
    CHECK_IN: '入住', CHECK_OUT: '退出', APPOINT: '任命',
    IMPORT: '导入', EXPORT: '导出'
  }
  return map[action] || action || '-'
}

const getMethodClass = (method: string) => {
  const classes: Record<string, string> = {
    GET: 'rounded bg-blue-100 px-1.5 py-0.5 text-xs font-medium text-blue-700',
    POST: 'rounded bg-green-100 px-1.5 py-0.5 text-xs font-medium text-green-700',
    PUT: 'rounded bg-amber-100 px-1.5 py-0.5 text-xs font-medium text-amber-700',
    DELETE: 'rounded bg-red-100 px-1.5 py-0.5 text-xs font-medium text-red-700'
  }
  return classes[method] || 'rounded bg-gray-100 px-1.5 py-0.5 text-xs font-medium text-gray-600'
}

const getResultClass = (result: string) => {
  if (result === 'SUCCESS') return 'rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700'
  if (result === 'FAILURE') return 'rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700'
  return 'rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700'
}

const getResourceTypeName = (type: string) => {
  const map: Record<string, string> = {
    ORG_UNIT: '组织单元', PLACE: '场所', USER: '用户', ROLE: '角色',
    STUDENT: '学生', USER_POSITION: '人员岗位', POSITION: '岗位',
    SEMESTER: '学期', AUTH: '认证', INSPECTION: '检查',
    SCHEDULE: '日程', CLASS: '班级'
  }
  return map[type] || type || '-'
}

const getFieldLabel = (field: string) => {
  const map: Record<string, string> = {
    unitName: '名称', sortOrder: '排序', headcount: '编制人数',
    attributes: '属性', status: '状态', addMember: '添加成员',
    removeMember: '移除成员', mergedIntoId: '合并至', splitInto: '拆分为',
    positionName: '岗位名称', positionId: '岗位', jobLevel: '职级',
    reportsToId: '汇报对象', responsibilities: '职责', requirements: '任职要求',
    keyPosition: '关键岗位', userId: '用户', endDate: '结束日期',
    occupant: '入住人', username: '账号', positionNo: '位置号', orgUnitName: '所属组织',
    name: '名称', description: '描述', type: '类型', category: '分类',
    code: '编码', parentId: '上级', remark: '备注'
  }
  return map[field] || field
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const getLogList = async () => {
  loading.value = true
  try {
    const response = await listActivityEvents(queryParams)
    logList.value = response.records || []
    total.value = response.total || 0
  } catch (error) {
    ElMessage.error('获取日志列表失败')
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    statsData.value = await getActivityStats()
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getLogList()
}

const resetQuery = () => {
  Object.assign(queryParams, {
    keyword: '', module: '', action: '', result: '', pageNum: 1, pageSize: 20
  })
  getLogList()
}

const handleView = (row: ActivityEvent) => {
  currentLog.value = row
  detailVisible.value = true
}

onMounted(() => {
  getLogList()
  loadStats()
})
</script>

<style scoped>
@keyframes fade-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.animate-fade-in { animation: fade-in 0.3s ease-out forwards; opacity: 0; }
</style>
