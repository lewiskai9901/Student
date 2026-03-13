<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Copy, Upload, Archive, Ban, Trash2, Eye } from 'lucide-vue-next'
import { useInspTemplateStore } from '@/stores/insp/inspTemplateStore'
import { TemplateStatusConfig, type TemplateStatus } from '@/types/insp/enums'
import type { InspTemplate, TemplateCatalog } from '@/types/insp/template'
import InspEmptyState from '../shared/InspEmptyState.vue'

const router = useRouter()
const store = useInspTemplateStore()

// State
const loading = ref(false)
const templates = ref<InspTemplate[]>([])
const total = ref(0)
const catalogs = ref<TemplateCatalog[]>([])

const queryParams = reactive({
  page: 1,
  size: 20,
  keyword: '',
  status: undefined as TemplateStatus | undefined,
  catalogId: undefined as number | undefined,
})

// Computed
const statusOptions = computed(() =>
  Object.entries(TemplateStatusConfig).map(([key, val]) => ({
    value: key,
    label: val.label,
  }))
)

// Actions
async function loadData() {
  loading.value = true
  try {
    const result = await store.loadTemplates(queryParams)
    templates.value = result.records
    total.value = result.total
  } catch (e: any) {
    ElMessage.error(e.message || '加载模板列表失败')
  } finally {
    loading.value = false
  }
}

async function loadCatalogs() {
  try {
    catalogs.value = await store.loadCatalogs()
  } catch { /* ignore */ }
}

function handleSearch() {
  queryParams.page = 1
  loadData()
}

function resetQuery() {
  queryParams.keyword = ''
  queryParams.status = undefined
  queryParams.catalogId = undefined
  queryParams.page = 1
  loadData()
}

function handlePageChange(page: number) {
  queryParams.page = page
  loadData()
}

function goCreate() {
  router.push('/inspection/v7/templates/create')
}

function goEdit(tpl: InspTemplate) {
  router.push(`/inspection/v7/templates/${tpl.id}/edit`)
}

async function handlePublish(tpl: InspTemplate) {
  try {
    await ElMessageBox.confirm('发布后将创建不可变版本快照，确认发布？', '确认发布模板', { type: 'warning' })
    await store.publish(tpl.id)
    ElMessage.success('发布成功')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '发布失败')
  }
}

async function handleDeprecate(tpl: InspTemplate) {
  try {
    await ElMessageBox.confirm('废弃后新项目无法使用此模板，确认废弃？', '确认废弃模板', { type: 'warning' })
    await store.deprecate(tpl.id)
    ElMessage.success('已废弃')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

async function handleArchive(tpl: InspTemplate) {
  try {
    await ElMessageBox.confirm('归档后模板将不可见，确认归档？', '确认归档模板', { type: 'warning' })
    await store.archive(tpl.id)
    ElMessage.success('已归档')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

async function handleDuplicate(tpl: InspTemplate) {
  try {
    await store.duplicate(tpl.id)
    ElMessage.success('复制成功')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '复制失败')
  }
}

async function handleDelete(tpl: InspTemplate) {
  try {
    await ElMessageBox.confirm(`确认删除模板「${tpl.templateName}」？`, '确认删除', { type: 'warning' })
    await store.removeTemplate(tpl.id)
    ElMessage.success('已删除')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

function getStatusStyle(status: TemplateStatus) {
  const cfg = TemplateStatusConfig[status]
  return cfg ? { color: cfg.color } : {}
}

onMounted(() => {
  loadData()
  loadCatalogs()
})
</script>

<template>
  <div class="p-6">
    <!-- Header -->
    <div class="mb-5 flex items-center justify-between">
      <h2 class="text-lg font-semibold text-gray-800">模板列表</h2>
      <button
        class="flex items-center gap-1.5 rounded-md bg-blue-500 px-4 py-2 text-sm text-white transition hover:bg-blue-600"
        @click="goCreate"
      >
        <Plus :size="16" />
        创建模板
      </button>
    </div>

    <!-- Filters -->
    <div class="mb-4 flex flex-wrap items-center gap-3">
      <div class="relative">
        <Search :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
        <input
          v-model="queryParams.keyword"
          type="text"
          placeholder="搜索模板名称..."
          class="h-9 rounded-md border border-gray-300 pl-9 pr-3 text-sm outline-none transition focus:border-blue-400 focus:ring-1 focus:ring-blue-100"
          @keyup.enter="handleSearch"
        />
      </div>
      <select
        v-model="queryParams.status"
        class="h-9 rounded-md border border-gray-300 px-3 text-sm outline-none focus:border-blue-400"
        @change="handleSearch"
      >
        <option :value="undefined">全部状态</option>
        <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
      <select
        v-model="queryParams.catalogId"
        class="h-9 rounded-md border border-gray-300 px-3 text-sm outline-none focus:border-blue-400"
        @change="handleSearch"
      >
        <option :value="undefined">全部分类</option>
        <option v-for="cat in catalogs" :key="cat.id" :value="cat.id">{{ cat.catalogName }}</option>
      </select>
      <button class="h-9 rounded-md border border-gray-300 px-3 text-sm text-gray-500 hover:bg-gray-50" @click="resetQuery">
        重置
      </button>
    </div>

    <!-- Table -->
    <div v-if="loading" class="py-16 text-center text-gray-400">加载中...</div>
    <InspEmptyState
      v-else-if="templates.length === 0"
      title="暂无模板"
      description="创建第一个检查模板开始使用"
      action-label="创建模板"
      @action="goCreate"
    />
    <div v-else class="overflow-hidden rounded-lg border border-gray-200">
      <table class="w-full text-left text-sm">
        <thead class="border-b border-gray-200 bg-gray-50">
          <tr>
            <th class="px-4 py-3 font-medium text-gray-600">模板名称</th>
            <th class="px-4 py-3 font-medium text-gray-600">编码</th>
            <th class="px-4 py-3 font-medium text-gray-600">状态</th>
            <th class="px-4 py-3 font-medium text-gray-600">版本</th>
            <th class="px-4 py-3 font-medium text-gray-600">使用次数</th>
            <th class="px-4 py-3 font-medium text-gray-600">更新时间</th>
            <th class="px-4 py-3 font-medium text-gray-600">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="tpl in templates"
            :key="tpl.id"
            class="group border-b border-gray-100 transition hover:bg-blue-50/30"
          >
            <td class="px-4 py-3">
              <span class="cursor-pointer font-medium text-blue-600 hover:underline" @click="goEdit(tpl)">
                {{ tpl.templateName }}
              </span>
              <span v-if="tpl.description" class="ml-2 text-xs text-gray-400">{{ tpl.description }}</span>
            </td>
            <td class="px-4 py-3 text-gray-500">{{ tpl.templateCode }}</td>
            <td class="px-4 py-3">
              <span class="inline-block rounded px-2 py-0.5 text-xs font-medium" :style="getStatusStyle(tpl.status)">
                {{ TemplateStatusConfig[tpl.status]?.label }}
              </span>
            </td>
            <td class="px-4 py-3 text-gray-500">v{{ tpl.latestVersion }}</td>
            <td class="px-4 py-3 text-gray-500">{{ tpl.useCount }}</td>
            <td class="px-4 py-3 text-gray-400 text-xs">{{ tpl.updatedAt?.slice(0, 16) }}</td>
            <td class="px-4 py-3">
              <div class="flex items-center gap-2 opacity-0 transition group-hover:opacity-100">
                <button class="text-gray-400 hover:text-blue-500" title="编辑" @click="goEdit(tpl)">
                  <Eye :size="16" />
                </button>
                <button
                  v-if="tpl.status === 'DRAFT'"
                  class="text-gray-400 hover:text-green-500"
                  title="发布"
                  @click="handlePublish(tpl)"
                >
                  <Upload :size="16" />
                </button>
                <button class="text-gray-400 hover:text-blue-500" title="复制" @click="handleDuplicate(tpl)">
                  <Copy :size="16" />
                </button>
                <button
                  v-if="tpl.status === 'PUBLISHED'"
                  class="text-gray-400 hover:text-yellow-500"
                  title="废弃"
                  @click="handleDeprecate(tpl)"
                >
                  <Ban :size="16" />
                </button>
                <button
                  v-if="tpl.status !== 'ARCHIVED'"
                  class="text-gray-400 hover:text-gray-600"
                  title="归档"
                  @click="handleArchive(tpl)"
                >
                  <Archive :size="16" />
                </button>
                <button
                  v-if="tpl.status === 'DRAFT'"
                  class="text-gray-400 hover:text-red-500"
                  title="删除"
                  @click="handleDelete(tpl)"
                >
                  <Trash2 :size="16" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div v-if="total > queryParams.size" class="mt-4 flex items-center justify-between text-sm text-gray-500">
      <span>共 {{ total }} 条</span>
      <div class="flex items-center gap-1">
        <button
          class="rounded border px-3 py-1 hover:bg-gray-50 disabled:opacity-50"
          :disabled="queryParams.page <= 1"
          @click="handlePageChange(queryParams.page - 1)"
        >
          上一页
        </button>
        <span class="px-3">{{ queryParams.page }} / {{ Math.ceil(total / queryParams.size) }}</span>
        <button
          class="rounded border px-3 py-1 hover:bg-gray-50 disabled:opacity-50"
          :disabled="queryParams.page >= Math.ceil(total / queryParams.size)"
          @click="handlePageChange(queryParams.page + 1)"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>
