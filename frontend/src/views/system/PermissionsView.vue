<template>
  <div class="flex h-full flex-col bg-gray-50">
    <!-- Header -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">权限目录</h1>
        <p class="mt-0.5 text-sm text-gray-500">系统所有权限点一览（由代码注解自动管理，不可手动编辑）</p>
      </div>
      <button
        @click="runSyncCheck"
        :disabled="syncing"
        class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
      >
        <Loader2 v-if="syncing" class="h-4 w-4 animate-spin" />
        <RefreshCw v-else class="h-4 w-4" />
        {{ syncing ? '检查中...' : '从代码同步检查' }}
      </button>
    </div>

    <!-- Stats bar -->
    <div class="flex items-center gap-1 border-b border-gray-100 bg-white px-6 py-2.5 text-sm text-gray-600">
      <span>权限总数 <strong class="text-gray-900">{{ totalCount }}</strong></span>
      <span class="mx-2 text-gray-300">|</span>
      <span>模块 <strong class="text-gray-900">{{ moduleCount }}</strong></span>
      <span class="mx-2 text-gray-300">|</span>
      <span>菜单 <strong class="text-gray-900">{{ menuCount }}</strong></span>
      <span class="mx-2 text-gray-300">|</span>
      <span>API/按钮 <strong class="text-gray-900">{{ apiCount }}</strong></span>
    </div>

    <!-- Search -->
    <div class="border-b border-gray-100 bg-white px-6 py-3">
      <div class="relative max-w-md">
        <Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索权限代码或名称..."
          class="h-9 w-full rounded-lg border border-gray-300 pl-9 pr-3 text-sm text-gray-900 placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex flex-1 items-center justify-center">
      <Loader2 class="h-8 w-8 animate-spin text-blue-600" />
    </div>

    <!-- Module cards -->
    <div v-else class="flex-1 space-y-4 overflow-y-auto px-6 py-5">
      <div
        v-for="group in filteredGroups"
        :key="group.module"
        class="overflow-hidden rounded-xl border border-gray-200 bg-white"
      >
        <!-- Module header -->
        <div
          class="flex cursor-pointer select-none items-center justify-between border-b border-gray-100 px-5 py-3 hover:bg-gray-50"
          @click="toggle(group.module)"
        >
          <div class="flex items-center gap-2">
            <span class="text-sm font-semibold text-gray-900">{{ group.label }}</span>
            <span class="text-xs text-gray-400">({{ group.module }})</span>
            <span class="rounded-full bg-gray-100 px-2 py-0.5 text-[11px] text-gray-500">{{ group.permissions.length }}个权限</span>
          </div>
          <ChevronDown
            class="h-4 w-4 text-gray-400 transition-transform duration-200"
            :class="{ '-rotate-180': expanded[group.module] }"
          />
        </div>

        <!-- Permission list (collapsible) -->
        <div v-if="expanded[group.module]" class="divide-y divide-gray-50">
          <div
            v-for="perm in group.permissions"
            :key="perm.permissionCode"
            class="flex items-center gap-3 px-5 py-2 hover:bg-gray-50/50"
          >
            <!-- Code -->
            <span class="w-72 shrink-0 truncate font-mono text-xs text-gray-500">{{ perm.permissionCode }}</span>
            <!-- Name -->
            <span class="flex-1 text-sm text-gray-900">{{ perm.permissionName || '-' }}</span>
            <!-- Type badge -->
            <span
              class="shrink-0 rounded px-2 py-0.5 text-[10px] font-medium"
              :class="typeClass(perm.type)"
            >{{ typeName(perm.type) }}</span>
            <!-- Status -->
            <span
              class="shrink-0 text-xs"
              :class="isEnabled(perm) ? 'text-emerald-500' : 'text-gray-300'"
            >{{ isEnabled(perm) ? '&#10003;' : '&#10005;' }}</span>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-if="filteredGroups.length === 0 && !loading" class="py-16 text-center">
        <Lock class="mx-auto h-12 w-12 text-gray-300" />
        <div class="mt-2 text-sm text-gray-500">
          {{ searchQuery ? '没有匹配的权限' : '暂无权限数据' }}
        </div>
      </div>
    </div>

    <!-- Sync check dialog -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="syncDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="syncDialogVisible = false"></div>
          <div class="relative w-full max-w-lg rounded-xl bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-base font-semibold text-gray-900">权限同步检查结果</h3>
              <button @click="syncDialogVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div v-if="syncResult" class="max-h-[60vh] overflow-y-auto px-6 py-4">
              <!-- Summary -->
              <div class="space-y-2 text-sm">
                <div class="flex items-center gap-2">
                  <span class="text-emerald-500">&#10003;</span>
                  <span class="text-gray-700">代码中的权限注解: <strong>{{ syncResult.codeAnnotationCount }}</strong> 个</span>
                </div>
                <div class="flex items-center gap-2">
                  <span class="text-emerald-500">&#10003;</span>
                  <span class="text-gray-700">数据库权限: <strong>{{ syncResult.dbPermissionCount }}</strong> 条</span>
                </div>
              </div>

              <!-- Missing in DB -->
              <div v-if="syncResult.missingInDbCount > 0" class="mt-4">
                <div class="flex items-center gap-1.5 text-sm font-medium text-amber-600">
                  <AlertTriangle class="h-4 w-4" />
                  代码中有但数据库缺失: {{ syncResult.missingInDbCount }} 条
                </div>
                <div class="mt-2 rounded-lg bg-amber-50 p-3">
                  <div
                    v-for="code in syncResult.missingInDb"
                    :key="code"
                    class="py-0.5 font-mono text-xs text-amber-800"
                  >{{ code }}</div>
                </div>
              </div>
              <div v-else class="mt-4 flex items-center gap-1.5 text-sm text-emerald-600">
                <CheckCircle class="h-4 w-4" />
                代码权限与数据库完全同步
              </div>

              <!-- Potentially obsolete in DB -->
              <div v-if="syncResult.potentiallyObsoleteCount > 0" class="mt-4">
                <div class="flex items-center gap-1.5 text-sm font-medium text-gray-500">
                  <Info class="h-4 w-4" />
                  数据库中可能过时的权限: {{ syncResult.potentiallyObsoleteCount }} 条
                </div>
                <div class="mt-2 max-h-40 overflow-y-auto rounded-lg bg-gray-50 p-3">
                  <div
                    v-for="code in syncResult.potentiallyObsoleteInDb"
                    :key="code"
                    class="py-0.5 font-mono text-xs text-gray-600"
                  >{{ code }}</div>
                </div>
              </div>
            </div>
            <div class="flex justify-end border-t border-gray-200 px-6 py-3">
              <button
                @click="syncDialogVisible = false"
                class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >关闭</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search,
  Lock,
  Loader2,
  ChevronDown,
  RefreshCw,
  X,
  AlertTriangle,
  CheckCircle,
  Info
} from 'lucide-vue-next'
import { getPermissions, checkPermissionSync } from '@/api/access'
import type { Permission } from '@/types'

// ---- Module label mapping ----
const MODULE_LABELS: Record<string, string> = {
  academic: '学术管理',
  analytics: '数据分析',
  asset: '资产管理',
  calendar: '校历管理',
  dormitory: '宿舍管理',
  enrollment: '招生管理',
  insp: '检查平台(V7)',
  inspection: '检查通用',
  place: '场所管理',
  schedule: '排班管理',
  student: '学生管理',
  system: '系统管理',
  task: '任务管理',
  teacher: '教师档案',
  teaching: '教学管理',
}

// ---- Type display helpers ----
type PermType = 'MENU' | 'OPERATION' | 'API' | 'DATA' | 'BUTTON' | string

function typeName(type: PermType): string {
  const map: Record<string, string> = { MENU: '菜单', OPERATION: '按钮', BUTTON: '按钮', API: 'API', DATA: '数据' }
  return map[type] || type || '未知'
}

function typeClass(type: PermType): string {
  const map: Record<string, string> = {
    MENU: 'bg-blue-50 text-blue-600',
    OPERATION: 'bg-amber-50 text-amber-600',
    BUTTON: 'bg-amber-50 text-amber-600',
    API: 'bg-emerald-50 text-emerald-600',
    DATA: 'bg-purple-50 text-purple-600',
  }
  return map[type] || 'bg-gray-100 text-gray-500'
}

// ---- Enabled check (backend returns isEnabled, frontend type has enabled) ----
function isEnabled(perm: any): boolean {
  return perm.isEnabled ?? perm.enabled ?? true
}

// ---- State ----
const loading = ref(false)
const searchQuery = ref('')
const allPermissions = ref<Permission[]>([])
const expanded = reactive<Record<string, boolean>>({})

// ---- Sync check state ----
const syncing = ref(false)
const syncDialogVisible = ref(false)
const syncResult = ref<{
  codeAnnotationCount: number
  dbPermissionCount: number
  missingInDb: string[]
  missingInDbCount: number
  potentiallyObsoleteInDb: string[]
  potentiallyObsoleteCount: number
} | null>(null)

// ---- Grouping logic ----
interface PermissionGroup {
  module: string
  label: string
  permissions: Permission[]
}

function flattenTree(nodes: Permission[]): Permission[] {
  const result: Permission[] = []
  for (const node of nodes) {
    result.push(node)
    if (node.children?.length) {
      result.push(...flattenTree(node.children))
    }
  }
  return result
}

const allGroups = computed<PermissionGroup[]>(() => {
  // Flatten tree into a flat list
  const flat = flattenTree(allPermissions.value)

  // Group by first segment of permission_code
  const groupMap = new Map<string, Permission[]>()
  for (const perm of flat) {
    const module = perm.permissionCode?.split(':')[0] || 'other'
    if (!groupMap.has(module)) groupMap.set(module, [])
    groupMap.get(module)!.push(perm)
  }

  // Sort groups by label
  const groups: PermissionGroup[] = []
  for (const [module, perms] of groupMap) {
    groups.push({
      module,
      label: MODULE_LABELS[module] || module,
      permissions: perms.sort((a, b) => a.permissionCode.localeCompare(b.permissionCode)),
    })
  }
  groups.sort((a, b) => a.label.localeCompare(b.label, 'zh-CN'))
  return groups
})

const filteredGroups = computed<PermissionGroup[]>(() => {
  const q = searchQuery.value.toLowerCase().trim()
  if (!q) return allGroups.value

  return allGroups.value
    .map(group => ({
      ...group,
      permissions: group.permissions.filter(
        p =>
          p.permissionCode.toLowerCase().includes(q) ||
          (p.permissionName && p.permissionName.toLowerCase().includes(q))
      ),
    }))
    .filter(group => group.permissions.length > 0)
})

// ---- Stats ----
const totalCount = computed(() => {
  return allGroups.value.reduce((sum, g) => sum + g.permissions.length, 0)
})
const moduleCount = computed(() => allGroups.value.length)
const menuCount = computed(() => {
  return allGroups.value.reduce(
    (sum, g) => sum + g.permissions.filter(p => p.type === 'MENU').length,
    0
  )
})
const apiCount = computed(() => {
  return allGroups.value.reduce(
    (sum, g) => sum + g.permissions.filter(p => p.type !== 'MENU').length,
    0
  )
})

// ---- Actions ----
function toggle(module: string) {
  expanded[module] = !expanded[module]
}

async function loadPermissions() {
  loading.value = true
  try {
    const data = await getPermissions()
    allPermissions.value = data || []
    // Expand all groups by default
    for (const group of allGroups.value) {
      if (!(group.module in expanded)) {
        expanded[group.module] = true
      }
    }
  } catch (error) {
    ElMessage.error('加载权限列表失败')
  } finally {
    loading.value = false
  }
}

async function runSyncCheck() {
  syncing.value = true
  try {
    const data = await checkPermissionSync()
    syncResult.value = data
    syncDialogVisible.value = true
  } catch (error) {
    ElMessage.error('同步检查失败，请确认后端服务正常运行')
  } finally {
    syncing.value = false
  }
}

onMounted(() => {
  loadPermissions()
})
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
