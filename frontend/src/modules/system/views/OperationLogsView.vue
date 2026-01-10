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
        <div class="flex gap-3">
          <button
            :disabled="selectedIds.length === 0"
            @click="handleBatchDelete"
            class="flex items-center gap-2 rounded-lg bg-red-500 px-4 py-2 font-medium text-white shadow-md transition-all hover:bg-red-600 disabled:opacity-50"
          >
            <Trash2 class="h-5 w-5" />
            批量删除 ({{ selectedIds.length }})
          </button>
          <button
            @click="handleClearLogs"
            class="flex items-center gap-2 rounded-lg bg-amber-500 px-4 py-2 font-medium text-white shadow-md transition-all hover:bg-amber-600"
          >
            <Eraser class="h-5 w-5" />
            清空日志
          </button>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-4 gap-4">
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">日志总数</p>
            <p class="mt-1 text-2xl font-bold text-gray-900">{{ total }}</p>
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
            <p class="mt-1 text-2xl font-bold text-green-600">{{ stats.success }}</p>
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
            <p class="mt-1 text-2xl font-bold text-red-600">{{ stats.failed }}</p>
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
            <p class="mt-1 text-2xl font-bold text-blue-600">{{ stats.today }}</p>
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
          <label class="mb-1.5 block text-sm font-medium text-gray-700">用户名</label>
          <input
            v-model="queryParams.username"
            type="text"
            placeholder="请输入用户名"
            class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-500/20"
            @keyup.enter="handleQuery"
          />
        </div>
        <div class="min-w-[120px]">
          <label class="mb-1.5 block text-sm font-medium text-gray-700">操作模块</label>
          <select
            v-model="queryParams.operationModule"
            class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-500/20"
          >
            <option value="">全部模块</option>
            <option value="student">学生管理</option>
            <option value="class">班级管理</option>
            <option value="dormitory">宿舍管理</option>
            <option value="quantification">量化管理</option>
            <option value="system">系统管理</option>
          </select>
        </div>
        <div class="min-w-[120px]">
          <label class="mb-1.5 block text-sm font-medium text-gray-700">操作类型</label>
          <select
            v-model="queryParams.operationType"
            class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-500/20"
          >
            <option value="">全部类型</option>
            <option value="create">创建</option>
            <option value="update">更新</option>
            <option value="delete">删除</option>
            <option value="query">查询</option>
            <option value="login">登录</option>
          </select>
        </div>
        <div class="min-w-[120px]">
          <label class="mb-1.5 block text-sm font-medium text-gray-700">响应状态</label>
          <select
            v-model="queryParams.responseStatus"
            class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-500/20"
          >
            <option :value="null">全部状态</option>
            <option :value="200">成功(200)</option>
            <option :value="500">错误(500)</option>
            <option :value="401">未授权(401)</option>
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
              <th class="whitespace-nowrap px-4 py-3 text-left">
                <input
                  type="checkbox"
                  :checked="selectedIds.length === logList.length && logList.length > 0"
                  @change="handleSelectAll"
                  class="h-4 w-4 rounded border-gray-300 text-slate-600 focus:ring-slate-500"
                />
              </th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">用户</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">模块</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">类型</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">操作名称</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">方法</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">状态</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">耗时</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">IP地址</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">操作时间</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">操作</th>
            </tr>
          </thead>
          <tbody v-if="loading">
            <tr>
              <td colspan="11" class="py-20 text-center">
                <div class="flex flex-col items-center gap-3">
                  <Loader2 class="h-8 w-8 animate-spin text-slate-600" />
                  <span class="text-sm text-gray-500">加载中...</span>
                </div>
              </td>
            </tr>
          </tbody>
          <tbody v-else-if="logList.length === 0">
            <tr>
              <td colspan="11" class="py-20 text-center">
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
                <input
                  type="checkbox"
                  :checked="selectedIds.includes(row.id)"
                  @change="handleSelectRow(row)"
                  class="h-4 w-4 rounded border-gray-300 text-slate-600 focus:ring-slate-500"
                />
              </td>
              <td class="whitespace-nowrap px-4 py-3">
                <div class="flex items-center gap-2">
                  <div class="flex h-8 w-8 items-center justify-center rounded-full bg-slate-100 text-xs font-medium text-slate-600">
                    {{ (row.realName || row.username || '?').charAt(0) }}
                  </div>
                  <div>
                    <div class="font-medium text-gray-900">{{ row.realName || row.username }}</div>
                    <div class="text-xs text-gray-500">{{ row.username }}</div>
                  </div>
                </div>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span :class="getModuleClass(row.operationModule)">
                  {{ getModuleName(row.operationModule) }}
                </span>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span :class="getTypeClass(row.operationType)">
                  {{ getTypeName(row.operationType) }}
                </span>
              </td>
              <td class="max-w-xs truncate px-4 py-3 text-gray-600" :title="row.operationName">
                {{ row.operationName }}
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span :class="getMethodClass(row.requestMethod)">
                  {{ row.requestMethod }}
                </span>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span :class="getStatusClass(row.responseStatus)">
                  {{ row.responseStatus }}
                </span>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span :class="['text-sm', row.responseTime > 1000 ? 'text-red-600 font-medium' : 'text-gray-600']">
                  {{ row.responseTime }}ms
                </span>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">{{ row.ipAddress }}</td>
              <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-500">{{ row.createdAt }}</td>
              <td class="whitespace-nowrap px-4 py-3">
                <div class="flex items-center justify-center gap-1">
                  <button
                    @click="handleView(row)"
                    class="rounded-lg p-1.5 text-gray-500 transition-colors hover:bg-blue-100 hover:text-blue-600"
                    title="详情"
                  >
                    <Eye class="h-4 w-4" />
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

    <!-- 详情对话框 -->
    <Teleport to="body">
      <div v-if="detailVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50 backdrop-blur-sm" @click="detailVisible = false"></div>
        <div class="relative z-10 w-full max-w-3xl rounded-xl bg-white shadow-2xl">
          <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
            <h3 class="text-lg font-semibold text-gray-900">操作日志详情</h3>
            <button @click="detailVisible = false" class="rounded-lg p-1 hover:bg-gray-100">
              <X class="h-5 w-5 text-gray-500" />
            </button>
          </div>
          <div class="max-h-[70vh] overflow-y-auto p-6">
            <div class="grid grid-cols-2 gap-4">
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">日志ID</div>
                <div class="font-mono text-sm text-gray-900">{{ currentLog.id }}</div>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">用户</div>
                <div class="text-gray-900">{{ currentLog.realName }} ({{ currentLog.username }})</div>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">操作模块</div>
                <span :class="getModuleClass(currentLog.operationModule)">
                  {{ getModuleName(currentLog.operationModule) }}
                </span>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">操作类型</div>
                <span :class="getTypeClass(currentLog.operationType)">
                  {{ getTypeName(currentLog.operationType) }}
                </span>
              </div>
              <div class="col-span-2 rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">操作名称</div>
                <div class="text-gray-900">{{ currentLog.operationName }}</div>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">请求方法</div>
                <span :class="getMethodClass(currentLog.requestMethod)">{{ currentLog.requestMethod }}</span>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">响应状态</div>
                <span :class="getStatusClass(currentLog.responseStatus)">{{ currentLog.responseStatus }}</span>
              </div>
              <div class="col-span-2 rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">请求URL</div>
                <div class="font-mono text-sm text-gray-900 break-all">{{ currentLog.requestUrl }}</div>
              </div>
              <div class="col-span-2 rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">请求参数</div>
                <pre class="mt-2 max-h-40 overflow-auto rounded bg-gray-100 p-3 text-xs text-gray-800">{{ formatJson(currentLog.requestParams) }}</pre>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">响应时间</div>
                <div class="text-gray-900">{{ currentLog.responseTime }}ms</div>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">IP地址</div>
                <div class="text-gray-900">{{ currentLog.ipAddress }}</div>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">浏览器</div>
                <div class="text-gray-900">{{ currentLog.browser }}</div>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">操作系统</div>
                <div class="text-gray-900">{{ currentLog.os }}</div>
              </div>
              <div v-if="currentLog.errorMessage" class="col-span-2 rounded-lg bg-red-50 p-4">
                <div class="text-sm text-red-500 mb-1">错误信息</div>
                <div class="text-red-700">{{ currentLog.errorMessage }}</div>
              </div>
              <div class="col-span-2 rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">操作时间</div>
                <div class="text-gray-900">{{ currentLog.createdAt }}</div>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ScrollText,
  Search,
  RotateCcw,
  Eye,
  Trash2,
  X,
  Loader2,
  ChevronLeft,
  ChevronRight,
  CheckCircle,
  XCircle,
  Activity,
  Eraser
} from 'lucide-vue-next'
import { http } from '@/utils/request'

const queryParams = reactive({
  username: '',
  realName: '',
  operationModule: '',
  operationType: '',
  ipAddress: '',
  responseStatus: null as number | null,
  createdAtStart: '',
  createdAtEnd: '',
  pageNum: 1,
  pageSize: 10
})

const loading = ref(false)
const logList = ref<any[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])

const detailVisible = ref(false)
const currentLog = ref<any>({})

// 统计数据
const stats = computed(() => {
  return {
    success: logList.value.filter(l => l.responseStatus >= 200 && l.responseStatus < 300).length,
    failed: logList.value.filter(l => l.responseStatus >= 400).length,
    today: logList.value.length // 简化处理
  }
})

// 获取模块样式
const getModuleClass = (module: string) => {
  const classes: Record<string, string> = {
    student: 'rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700',
    class: 'rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700',
    dormitory: 'rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700',
    quantification: 'rounded-full bg-purple-100 px-2 py-0.5 text-xs font-medium text-purple-700',
    system: 'rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700'
  }
  return classes[module] || 'rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600'
}

const getModuleName = (module: string) => {
  const map: Record<string, string> = {
    student: '学生', class: '班级', dormitory: '宿舍',
    quantification: '量化', system: '系统', user: '用户', role: '角色'
  }
  return map[module] || module
}

// 获取类型样式
const getTypeClass = (type: string) => {
  const classes: Record<string, string> = {
    create: 'rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700',
    update: 'rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700',
    delete: 'rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700',
    query: 'rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700',
    login: 'rounded-full bg-indigo-100 px-2 py-0.5 text-xs font-medium text-indigo-700'
  }
  return classes[type] || 'rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600'
}

const getTypeName = (type: string) => {
  const map: Record<string, string> = {
    create: '创建', update: '更新', delete: '删除', query: '查询', login: '登录', logout: '登出'
  }
  return map[type] || type
}

// 获取方法样式
const getMethodClass = (method: string) => {
  const classes: Record<string, string> = {
    GET: 'rounded bg-blue-100 px-1.5 py-0.5 text-xs font-medium text-blue-700',
    POST: 'rounded bg-green-100 px-1.5 py-0.5 text-xs font-medium text-green-700',
    PUT: 'rounded bg-amber-100 px-1.5 py-0.5 text-xs font-medium text-amber-700',
    DELETE: 'rounded bg-red-100 px-1.5 py-0.5 text-xs font-medium text-red-700'
  }
  return classes[method] || 'rounded bg-gray-100 px-1.5 py-0.5 text-xs font-medium text-gray-600'
}

// 获取状态样式
const getStatusClass = (status: number) => {
  if (status >= 200 && status < 300) return 'rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700'
  if (status >= 400 && status < 500) return 'rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700'
  if (status >= 500) return 'rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700'
  return 'rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600'
}

const formatJson = (jsonStr: string) => {
  if (!jsonStr) return '-'
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2)
  } catch {
    return jsonStr
  }
}

const getLogList = async () => {
  loading.value = true
  try {
    const response = await http.get<{ records: any[]; total: number }>('/system/operation-logs', { params: queryParams })
    logList.value = response.records || []
    total.value = response.total || 0
  } catch (error) {
    ElMessage.error('获取日志列表失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getLogList()
}

const resetQuery = () => {
  Object.assign(queryParams, {
    username: '', realName: '', operationModule: '', operationType: '',
    ipAddress: '', responseStatus: null, pageNum: 1, pageSize: 10
  })
  getLogList()
}

const handleSelectAll = (e: Event) => {
  const target = e.target as HTMLInputElement
  selectedIds.value = target.checked ? logList.value.map(l => l.id) : []
}

const handleSelectRow = (row: any) => {
  const idx = selectedIds.value.indexOf(row.id)
  if (idx > -1) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(row.id)
  }
}

const handleView = (row: any) => {
  currentLog.value = row
  detailVisible.value = true
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定删除该操作日志吗？', '提示', { type: 'warning' })
    await http.delete(`/system/operation-logs/${row.id}`)
    ElMessage.success('删除成功')
    getLogList()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条日志吗？`, '提示', { type: 'warning' })
    await http.delete('/system/operation-logs/batch', { data: selectedIds.value })
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    getLogList()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error('批量删除失败')
  }
}

const handleClearLogs = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入要清空多少天前的日志', '清空日志', {
      inputPattern: /^\d+$/,
      inputErrorMessage: '请输入有效的天数',
      inputPlaceholder: '例如: 30'
    })
    const days = parseInt(value)
    const beforeDate = new Date()
    beforeDate.setDate(beforeDate.getDate() - days)
    await http.delete('/system/operation-logs/clear', { params: { beforeDate: beforeDate.toISOString() } })
    ElMessage.success(`成功清空 ${days} 天前的日志`)
    getLogList()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error('清空日志失败')
  }
}

onMounted(() => {
  getLogList()
})
</script>

<style scoped>
@keyframes fade-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.animate-fade-in { animation: fade-in 0.3s ease-out forwards; opacity: 0; }
</style>
