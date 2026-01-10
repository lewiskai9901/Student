<template>
  <div class="min-h-screen bg-gray-50/50 p-6">
    <!-- 页面头部 -->
    <div class="mb-6 rounded-xl bg-gradient-to-r from-violet-600 to-purple-500 p-6 shadow-lg">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="flex items-center gap-3 text-2xl font-bold text-white">
            <Settings class="h-8 w-8" />
            系统配置
          </h1>
          <p class="mt-1 text-violet-100">管理系统参数和业务配置</p>
        </div>
        <div class="flex gap-3">
          <button
            @click="handleRefreshCache"
            class="flex items-center gap-2 rounded-lg bg-white/20 px-4 py-2 text-sm font-medium text-white backdrop-blur-sm transition-all hover:bg-white/30"
          >
            <RefreshCw class="h-4 w-4" />
            刷新缓存
          </button>
          <button
            @click="handleAdd"
            class="flex items-center gap-2 rounded-lg bg-white/95 px-4 py-2 font-medium text-violet-600 shadow-md transition-all hover:-translate-y-0.5 hover:bg-white hover:shadow-lg"
          >
            <Plus class="h-5 w-5" />
            新增配置
          </button>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-5 gap-4">
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">全部配置</p>
            <p class="mt-1 text-2xl font-bold text-violet-600">{{ allConfigs.length }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-violet-100 text-violet-600">
            <Database class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-violet-500 to-purple-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">系统配置</p>
            <p class="mt-1 text-2xl font-bold text-blue-600">{{ systemConfigs.length }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-100 text-blue-600">
            <Cpu class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-blue-500 to-blue-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">业务配置</p>
            <p class="mt-1 text-2xl font-bold text-emerald-600">{{ businessConfigs.length }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600">
            <Briefcase class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-emerald-500 to-emerald-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">微信配置</p>
            <p class="mt-1 text-2xl font-bold text-green-600">{{ wechatConfigs.length }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-green-100 text-green-600">
            <MessageCircle class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-green-500 to-green-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">界面配置</p>
            <p class="mt-1 text-2xl font-bold text-amber-600">{{ uiConfigs.length }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-amber-100 text-amber-600">
            <Palette class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-amber-500 to-amber-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
    </div>

    <!-- Tab 导航栏 -->
    <div class="mb-6 rounded-xl bg-white p-2 shadow-sm">
      <div class="flex gap-1">
        <button
          @click="activeGroup = 'all'"
          :class="[
            'flex items-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium transition-all',
            activeGroup === 'all'
              ? 'bg-violet-600 text-white shadow-sm'
              : 'text-gray-600 hover:bg-gray-100'
          ]"
        >
          <Database class="h-4 w-4" />
          全部配置
          <span class="ml-1 rounded-full bg-white/20 px-1.5 py-0.5 text-xs">{{ allConfigs.length }}</span>
        </button>
        <button
          @click="activeGroup = 'system'"
          :class="[
            'flex items-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium transition-all',
            activeGroup === 'system'
              ? 'bg-violet-600 text-white shadow-sm'
              : 'text-gray-600 hover:bg-gray-100'
          ]"
        >
          <Cpu class="h-4 w-4" />
          系统配置
        </button>
        <button
          @click="activeGroup = 'business'"
          :class="[
            'flex items-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium transition-all',
            activeGroup === 'business'
              ? 'bg-violet-600 text-white shadow-sm'
              : 'text-gray-600 hover:bg-gray-100'
          ]"
        >
          <Briefcase class="h-4 w-4" />
          业务配置
        </button>
        <button
          @click="activeGroup = 'wechat'"
          :class="[
            'flex items-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium transition-all',
            activeGroup === 'wechat'
              ? 'bg-violet-600 text-white shadow-sm'
              : 'text-gray-600 hover:bg-gray-100'
          ]"
        >
          <MessageCircle class="h-4 w-4" />
          微信配置
        </button>
        <button
          @click="activeGroup = 'ui'"
          :class="[
            'flex items-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium transition-all',
            activeGroup === 'ui'
              ? 'bg-violet-600 text-white shadow-sm'
              : 'text-gray-600 hover:bg-gray-100'
          ]"
        >
          <Palette class="h-4 w-4" />
          界面配置
        </button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="rounded-xl bg-white shadow-sm">
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="border-b border-gray-200 bg-gray-50/50">
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-600">配置标签</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-600">配置键</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-600">配置值</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider text-gray-600">类型</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-600">描述</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider text-gray-600">系统内置</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider text-gray-600">状态</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider text-gray-600">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr v-if="loading" class="animate-pulse">
              <td colspan="8" class="px-4 py-8 text-center text-gray-400">
                <div class="flex items-center justify-center gap-2">
                  <Loader2 class="h-5 w-5 animate-spin" />
                  加载中...
                </div>
              </td>
            </tr>
            <tr v-else-if="currentConfigs.length === 0">
              <td colspan="8" class="px-4 py-8 text-center text-gray-400">
                <Database class="mx-auto h-12 w-12 text-gray-300" />
                <p class="mt-2">暂无配置数据</p>
              </td>
            </tr>
            <tr
              v-for="(row, index) in currentConfigs"
              :key="row.id"
              class="animate-fade-in transition-colors hover:bg-gray-50/50"
              :style="{ animationDelay: `${index * 30}ms` }"
            >
              <td class="px-4 py-3.5">
                <div class="flex items-center gap-2">
                  <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-violet-100">
                    <Key class="h-4 w-4 text-violet-600" />
                  </div>
                  <span class="font-medium text-gray-900">{{ row.configLabel }}</span>
                </div>
              </td>
              <td class="px-4 py-3.5">
                <code class="rounded bg-gray-100 px-2 py-1 text-xs font-mono text-gray-700">{{ row.configKey }}</code>
              </td>
              <td class="max-w-xs px-4 py-3.5">
                <template v-if="row.configType === 'boolean'">
                  <span :class="[
                    'inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium',
                    row.configValue === 'true' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
                  ]">
                    {{ row.configValue === 'true' ? '是' : '否' }}
                  </span>
                </template>
                <template v-else-if="row.configType === 'json'">
                  <span class="inline-flex items-center gap-1 rounded-full bg-purple-100 px-2.5 py-1 text-xs font-medium text-purple-700">
                    <Code class="h-3 w-3" />
                    JSON 数据
                  </span>
                </template>
                <template v-else>
                  <span class="text-sm text-gray-700 truncate block max-w-[200px]" :title="row.configValue">
                    {{ row.configValue }}
                  </span>
                </template>
              </td>
              <td class="px-4 py-3.5 text-center">
                <span :class="[
                  'inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium',
                  getConfigTypeBadge(row.configType)
                ]">
                  {{ getConfigTypeText(row.configType) }}
                </span>
              </td>
              <td class="max-w-[200px] px-4 py-3.5">
                <span class="text-sm text-gray-500 truncate block" :title="row.configDesc">
                  {{ row.configDesc || '-' }}
                </span>
              </td>
              <td class="px-4 py-3.5 text-center">
                <span v-if="row.isSystem === 1" class="inline-flex items-center gap-1 rounded-full bg-red-100 px-2.5 py-1 text-xs font-medium text-red-700">
                  <Lock class="h-3 w-3" />
                  是
                </span>
                <span v-else class="inline-flex items-center rounded-full bg-gray-100 px-2.5 py-1 text-xs font-medium text-gray-500">
                  否
                </span>
              </td>
              <td class="px-4 py-3.5 text-center">
                <span :class="[
                  'inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium',
                  row.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                ]">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="px-4 py-3.5">
                <div class="flex items-center justify-center gap-1">
                  <button
                    @click="handleEdit(row)"
                    class="inline-flex items-center gap-1 rounded-lg px-2.5 py-1.5 text-xs font-medium text-blue-600 transition-colors hover:bg-blue-50"
                    title="编辑"
                  >
                    <Pencil class="h-3.5 w-3.5" />
                    编辑
                  </button>
                  <button
                    @click="handleDelete(row)"
                    :disabled="row.isSystem === 1"
                    :class="[
                      'inline-flex items-center gap-1 rounded-lg px-2.5 py-1.5 text-xs font-medium transition-colors',
                      row.isSystem === 1
                        ? 'cursor-not-allowed text-gray-400'
                        : 'text-red-600 hover:bg-red-50'
                    ]"
                    title="删除"
                  >
                    <Trash2 class="h-3.5 w-3.5" />
                    删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <Teleport to="body">
      <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50 backdrop-blur-sm" @click="dialogVisible = false"></div>
        <div class="relative w-full max-w-xl rounded-2xl bg-white p-6 shadow-2xl animate-fade-in">
          <div class="mb-6 flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-violet-100">
                <Settings class="h-5 w-5 text-violet-600" />
              </div>
              <div>
                <h3 class="text-lg font-semibold text-gray-900">{{ dialogTitle }}</h3>
                <p class="text-sm text-gray-500">{{ isEdit ? '修改系统配置信息' : '添加新的系统配置' }}</p>
              </div>
            </div>
            <button @click="dialogVisible = false" class="rounded-lg p-2 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600">
              <X class="h-5 w-5" />
            </button>
          </div>

          <form @submit.prevent="handleSubmit" class="space-y-4">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  配置键 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="formData.configKey"
                  type="text"
                  :disabled="isEdit"
                  placeholder="如: system.name"
                  :class="[
                    'w-full rounded-lg border px-4 py-2.5 text-sm font-mono transition-colors focus:outline-none focus:ring-1',
                    isEdit
                      ? 'border-gray-200 bg-gray-50 text-gray-500'
                      : 'border-gray-300 focus:border-violet-500 focus:ring-violet-500'
                  ]"
                />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  配置标签 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="formData.configLabel"
                  type="text"
                  placeholder="配置中文名称"
                  class="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-sm transition-colors focus:border-violet-500 focus:outline-none focus:ring-1 focus:ring-violet-500"
                />
              </div>
            </div>

            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">
                配置值 <span class="text-red-500">*</span>
              </label>
              <textarea
                v-if="formData.configType === 'json'"
                v-model="formData.configValue"
                rows="4"
                placeholder="请输入 JSON 格式的配置值"
                class="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-sm font-mono transition-colors focus:border-violet-500 focus:outline-none focus:ring-1 focus:ring-violet-500"
              ></textarea>
              <input
                v-else
                v-model="formData.configValue"
                type="text"
                placeholder="请输入配置值"
                class="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-sm transition-colors focus:border-violet-500 focus:outline-none focus:ring-1 focus:ring-violet-500"
              />
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  配置类型 <span class="text-red-500">*</span>
                </label>
                <div class="flex flex-wrap gap-3 py-2">
                  <label v-for="type in configTypes" :key="type.value" class="flex cursor-pointer items-center gap-2">
                    <input
                      v-model="formData.configType"
                      type="radio"
                      :value="type.value"
                      class="h-4 w-4 border-gray-300 text-violet-600 focus:ring-violet-500"
                    />
                    <span class="text-sm text-gray-700">{{ type.label }}</span>
                  </label>
                </div>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  配置分组 <span class="text-red-500">*</span>
                </label>
                <select
                  v-model="formData.configGroup"
                  class="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-sm transition-colors focus:border-violet-500 focus:outline-none focus:ring-1 focus:ring-violet-500"
                >
                  <option value="system">系统配置</option>
                  <option value="business">业务配置</option>
                  <option value="wechat">微信配置</option>
                  <option value="ui">界面配置</option>
                </select>
              </div>
            </div>

            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">配置描述</label>
              <textarea
                v-model="formData.configDesc"
                rows="2"
                placeholder="请输入配置描述"
                class="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-sm transition-colors focus:border-violet-500 focus:outline-none focus:ring-1 focus:ring-violet-500"
              ></textarea>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">排序号</label>
                <input
                  v-model.number="formData.sortOrder"
                  type="number"
                  min="0"
                  max="9999"
                  placeholder="0"
                  class="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-sm transition-colors focus:border-violet-500 focus:outline-none focus:ring-1 focus:ring-violet-500"
                />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">状态</label>
                <div class="flex gap-4 py-2">
                  <label class="flex cursor-pointer items-center gap-2">
                    <input
                      v-model="formData.status"
                      type="radio"
                      :value="1"
                      class="h-4 w-4 border-gray-300 text-violet-600 focus:ring-violet-500"
                    />
                    <span class="text-sm text-gray-700">启用</span>
                  </label>
                  <label class="flex cursor-pointer items-center gap-2">
                    <input
                      v-model="formData.status"
                      type="radio"
                      :value="0"
                      class="h-4 w-4 border-gray-300 text-violet-600 focus:ring-violet-500"
                    />
                    <span class="text-sm text-gray-700">禁用</span>
                  </label>
                </div>
              </div>
            </div>

            <div v-if="isEdit" class="rounded-lg bg-amber-50 p-3">
              <div class="flex items-center gap-2 text-sm text-amber-700">
                <AlertCircle class="h-4 w-4" />
                <span v-if="formData.isSystem === 1">系统内置配置，不可删除</span>
                <span v-else>用户自定义配置</span>
              </div>
            </div>

            <div class="flex justify-end gap-3 pt-4 border-t border-gray-200">
              <button
                type="button"
                @click="dialogVisible = false"
                class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
              >
                取消
              </button>
              <button
                type="submit"
                :disabled="submitLoading"
                class="inline-flex items-center gap-2 rounded-lg bg-violet-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-violet-700 disabled:opacity-50"
              >
                <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
                <Save v-else class="h-4 w-4" />
                确定
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Settings,
  Plus,
  RefreshCw,
  Database,
  Cpu,
  Briefcase,
  Palette,
  Key,
  Code,
  Lock,
  Pencil,
  Trash2,
  X,
  Save,
  Loader2,
  AlertCircle,
  MessageCircle
} from 'lucide-vue-next'
import { http } from '@/utils/request'

// 数据
const loading = ref(false)
const submitLoading = ref(false)
const allConfigs = ref<any[]>([])
const activeGroup = ref('all')

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)

// 配置类型选项
const configTypes = [
  { label: '字符串', value: 'string' },
  { label: '数字', value: 'number' },
  { label: '布尔值', value: 'boolean' },
  { label: 'JSON', value: 'json' }
]

// 表单数据
const formData = reactive({
  id: null as number | null,
  configKey: '',
  configValue: '',
  configType: 'string',
  configGroup: 'system',
  configLabel: '',
  configDesc: '',
  isSystem: 0,
  sortOrder: 0,
  status: 1
})

// 分组配置数据
const systemConfigs = computed(() =>
  allConfigs.value.filter(c => c.configGroup === 'system')
)
const businessConfigs = computed(() =>
  allConfigs.value.filter(c => c.configGroup === 'business')
)
const wechatConfigs = computed(() =>
  allConfigs.value.filter(c => c.configGroup === 'wechat')
)
const uiConfigs = computed(() =>
  allConfigs.value.filter(c => c.configGroup === 'ui')
)

// 当前显示的配置
const currentConfigs = computed(() => {
  switch (activeGroup.value) {
    case 'system':
      return systemConfigs.value
    case 'business':
      return businessConfigs.value
    case 'wechat':
      return wechatConfigs.value
    case 'ui':
      return uiConfigs.value
    default:
      return allConfigs.value
  }
})

// 获取配置类型文本
const getConfigTypeText = (type: string) => {
  const texts: Record<string, string> = {
    string: '字符串',
    number: '数字',
    boolean: '布尔值',
    json: 'JSON'
  }
  return texts[type] || type
}

// 获取配置类型徽章样式
const getConfigTypeBadge = (type: string) => {
  const badges: Record<string, string> = {
    string: 'bg-blue-100 text-blue-700',
    number: 'bg-green-100 text-green-700',
    boolean: 'bg-amber-100 text-amber-700',
    json: 'bg-purple-100 text-purple-700'
  }
  return badges[type] || 'bg-gray-100 text-gray-600'
}

// 获取配置列表
const loadConfigs = async () => {
  loading.value = true
  try {
    const response = await http.get<{ records: any[] }>('/system/configs', {
      params: { pageNum: 1, pageSize: 1000 }
    })
    allConfigs.value = response.records || []
  } catch (error) {
    console.error('获取配置列表失败:', error)
    ElMessage.error('获取配置列表失败')
  } finally {
    loading.value = false
  }
}

// 刷新缓存
const handleRefreshCache = async () => {
  try {
    await http.post('/system/configs/refresh')
    ElMessage.success('缓存刷新成功')
  } catch (error) {
    console.error('刷新缓存失败:', error)
    ElMessage.error('刷新缓存失败')
  }
}

// 新增配置
const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增配置'

  Object.assign(formData, {
    id: null,
    configKey: '',
    configValue: '',
    configType: 'string',
    configGroup: activeGroup.value === 'all' ? 'system' : activeGroup.value,
    configLabel: '',
    configDesc: '',
    isSystem: 0,
    sortOrder: 0,
    status: 1
  })

  dialogVisible.value = true
}

// 编辑配置
const handleEdit = (row: any) => {
  isEdit.value = true
  dialogTitle.value = '编辑配置'

  Object.assign(formData, {
    id: row.id,
    configKey: row.configKey,
    configValue: row.configValue,
    configType: row.configType,
    configGroup: row.configGroup,
    configLabel: row.configLabel,
    configDesc: row.configDesc || '',
    isSystem: row.isSystem,
    sortOrder: row.sortOrder || 0,
    status: row.status
  })

  dialogVisible.value = true
}

// 删除配置
const handleDelete = async (row: any) => {
  if (row.isSystem === 1) {
    ElMessage.warning('系统内置配置不允许删除')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定删除配置 "${row.configLabel}" 吗?`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await http.delete(`/system/configs/${row.id}`)
    ElMessage.success('删除成功')
    loadConfigs()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formData.configKey) {
    ElMessage.error('请输入配置键')
    return
  }
  if (!formData.configLabel) {
    ElMessage.error('请输入配置标签')
    return
  }
  if (!formData.configValue) {
    ElMessage.error('请输入配置值')
    return
  }

  submitLoading.value = true
  try {
    const data = {
      configKey: formData.configKey,
      configValue: formData.configValue,
      configType: formData.configType,
      configGroup: formData.configGroup,
      configLabel: formData.configLabel,
      configDesc: formData.configDesc,
      sortOrder: formData.sortOrder,
      status: formData.status
    }

    if (isEdit.value) {
      await http.put(`/system/configs/${formData.id}`, data)
      ElMessage.success('更新成功')
    } else {
      await http.post('/system/configs', data)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadConfigs()
  } catch (error: any) {
    console.error('提交失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 初始化
onMounted(() => {
  loadConfigs()
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
