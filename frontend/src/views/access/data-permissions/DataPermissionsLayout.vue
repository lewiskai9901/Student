<template>
  <div class="flex h-[calc(100vh-56px)] flex-col bg-gray-50">
    <!-- 顶部 header -->
    <div
      class="flex h-12 items-center justify-between border-b border-gray-200 bg-white px-5"
    >
      <div class="flex items-center gap-3">
        <Shield class="h-5 w-5 text-blue-600" />
        <h1 class="text-base font-semibold text-gray-900">数据权限管理</h1>
        <span class="text-xs text-gray-400">角色级数据范围配置</span>
      </div>
      <div class="flex items-center gap-2">
        <button
          class="flex h-8 items-center gap-1.5 rounded-md border border-gray-200 bg-white px-3 text-xs font-medium text-gray-600 hover:bg-gray-50"
          @click="showTemplates = true"
        >
          <Library class="h-3.5 w-3.5" />
          模板库
        </button>
        <button
          class="flex h-8 items-center gap-1.5 rounded-md border border-gray-200 bg-white px-3 text-xs font-medium text-gray-600 hover:bg-gray-50"
          @click="showCompare = !showCompare"
          :class="{ 'border-blue-500 text-blue-600': showCompare }"
        >
          <Columns3 class="h-3.5 w-3.5" />
          批量对比
        </button>
      </div>
    </div>

    <!-- 三栏布局 -->
    <div class="flex flex-1 overflow-hidden">
      <!-- 左栏: 角色列表 -->
      <div class="w-64 flex-shrink-0 border-r border-gray-200">
        <RoleExplorer
          v-model="selectedRoleId"
          @compare="onCompare"
        />
      </div>

      <!-- 中间主区: 配置器 -->
      <div class="flex-1 overflow-hidden">
        <PermissionConfigurator
          ref="configuratorRef"
          :current-role="currentRole"
          :grouped-modules="groupedModules"
          :advanced-grouped-modules="advancedGroupedModules"
          :filter-meta="filterMeta"
          :data-scope-options="dataScopeOptions"
          :module-name-map="moduleNameMap"
          @open-templates="showTemplates = true"
          @config-loaded="onConfigLoaded"
          @config-changed="onConfigChanged"
          @saved="onSaved"
          @enable-industry="onEnableIndustry"
        />
      </div>

      <!-- 右栏: 预览 -->
      <div class="w-80 flex-shrink-0 border-l border-gray-200">
        <PreviewPanel
          :decision="currentDecision"
          :module-permissions="currentModulePermissions"
          :data-scope-options="dataScopeOptions"
          :module-name-map="moduleNameMap"
          :total-modules="totalModules"
          :fallbacks="currentFallbacks"
        />
      </div>
    </div>

    <!-- 模板库 -->
    <TemplateLibraryDialog v-model:visible="showTemplates" @apply="onApplyTemplate" />

    <!-- 对比对话框 (MVP 简化: 并排显示选中角色的 scene 摘要) -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showCompareDialog" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="showCompareDialog = false" />
          <div class="relative w-full max-w-5xl rounded-xl bg-white shadow-2xl">
            <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4">
              <h3 class="text-lg font-semibold text-gray-900">角色对比</h3>
              <button class="rounded p-1.5 text-gray-400 hover:bg-gray-100" @click="showCompareDialog = false">
                <X class="h-5 w-5" />
              </button>
            </div>
            <div class="p-5">
              <div v-if="compareLoading" class="flex justify-center py-8">
                <Loader2 class="h-6 w-6 animate-spin text-blue-500" />
              </div>
              <div v-else class="grid gap-4" :style="{ gridTemplateColumns: `repeat(${compareData.length}, 1fr)` }">
                <div v-for="cd in compareData" :key="cd.role.id" class="rounded-lg border border-gray-200 p-3">
                  <div class="mb-2 flex items-center gap-2">
                    <Shield class="h-4 w-4 text-blue-500" />
                    <div class="min-w-0">
                      <div class="truncate text-sm font-semibold text-gray-900">{{ cd.role.roleName }}</div>
                      <div class="truncate font-mono text-[10px] text-gray-400">{{ cd.role.roleCode }}</div>
                    </div>
                  </div>
                  <div class="space-y-1.5 text-xs text-gray-700">
                    <div class="flex items-center justify-between">
                      <span class="text-gray-500">主决策</span>
                      <span class="font-medium" :class="diffClass(cd, 'primary')">
                        {{ scopeLabel(cd.decision.primary) }}
                      </span>
                    </div>
                    <div class="flex items-center justify-between">
                      <span class="text-gray-500">学生特化</span>
                      <span class="font-medium" :class="diffClass(cd, 'studentScope')">
                        {{ cd.decision.studentScope || '—' }}
                      </span>
                    </div>
                    <div class="flex items-center justify-between">
                      <span class="text-gray-500">业务跟随</span>
                      <span class="font-medium" :class="diffClass(cd, 'bizAutoFollow')">
                        {{ cd.decision.bizAutoFollow ? '是' : '否' }}
                      </span>
                    </div>
                    <div class="flex items-center justify-between">
                      <span class="text-gray-500">模块数</span>
                      <span class="font-medium text-gray-700">{{ cd.mps.length }}</span>
                    </div>
                  </div>
                  <button
                    class="mt-3 h-7 w-full rounded bg-blue-50 text-[11px] font-medium text-blue-600 hover:bg-blue-100"
                    @click="jumpToRole(cd.role.id)"
                  >
                    打开配置 >
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, computed, onMounted, watch, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pluginPlatformApi } from '@/api/pluginPlatform'
import { Shield, Library, Columns3, X, Loader2 } from 'lucide-vue-next'
import RoleExplorer from './components/RoleExplorer.vue'
import PermissionConfigurator from './components/PermissionConfigurator.vue'
import PreviewPanel from './components/PreviewPanel.vue'
import TemplateLibraryDialog from './components/TemplateLibraryDialog.vue'
import type { ModuleGroupItem } from './components/AdvancedModuleEditor.vue'
import {
  dataPermissionApi,
  getRolesPage,
  type RoleResponse,
} from '@/api/access'
import type {
  DataScopeOption,
  ModulePermission,
} from '@/types/access'
import type { SceneDecision, PrimaryScope, ScopeFallbackInfo } from './composables/useSceneTemplate'
import { moduleScopesToScene } from './composables/useSceneTemplate'
import type { RoleTemplate } from './composables/useTemplateLibrary'

const route = useRoute()
const router = useRouter()

const configuratorRef = ref<InstanceType<typeof PermissionConfigurator> | null>(null)
const showTemplates = ref(false)
const showCompare = ref(false)
const showCompareDialog = ref(false)
const compareLoading = ref(false)

const groupedModules = ref<Record<string, ModuleGroupItem[]>>({})
const advancedGroupedModules = ref<Record<string, ModuleGroupItem[]>>({})
const filterMeta = ref<{
  filtered: boolean
  filterRule?: string
  totalRelevant?: number
  totalAdvanced?: number
  roleIndustry?: string
  rolePermModules?: string[]
}>({ filtered: false })
const moduleNameMap = ref<Record<string, string>>({})
const dataScopeOptions = ref<DataScopeOption[]>([])
const allRoles = ref<RoleResponse[]>([])

// 当前角色 id (同步 URL)
// 保持字符串形式. 后端 snowflake id 超过 JS Number 2^53 精度, 不能 Number() 转.
const selectedRoleId = computed<string | null>({
  get() {
    const raw = route.query.role
    if (raw == null || raw === '') return null
    return String(raw)
  },
  set(v) {
    const next: Record<string, any> = { ...route.query }
    if (v == null) delete next.role
    else next.role = String(v)
    router.replace({ query: next })
  },
})

const currentRole = computed<RoleResponse | null>(() => {
  const id = selectedRoleId.value
  if (id == null) return null
  return allRoles.value.find(r => String(r.id) === String(id)) || null
})

// 当前配置状态 (供预览实时显示)
const currentModulePermissions = ref<ModulePermission[]>([])
const currentDecision = ref<SceneDecision>({ primary: 'SELF', bizAutoFollow: true })
const currentFallbacks = ref<ScopeFallbackInfo[]>([])

const totalModules = computed(() =>
  Object.values(groupedModules.value).reduce((s, l) => s + l.length, 0)
)

function groupByIndustry(mods: any[]): Record<string, ModuleGroupItem[]> {
  const byIndustry: Record<string, ModuleGroupItem[]> = {}
  for (const m of mods) {
    const industry = (m as any).industry || inferIndustry(m.domainCode)
    if (!byIndustry[industry]) byIndustry[industry] = []
    byIndustry[industry].push({
      code: m.moduleCode,
      name: m.moduleName,
      industry,
      pluginEnabled: (m as any).pluginEnabled !== false,
      allowedScopes: (m as any).allowedScopes ?? null,
    })
  }
  const order = ['CORE', 'EDU', 'HEALTH', 'CARE', 'CUSTOM']
  const ordered: Record<string, ModuleGroupItem[]> = {}
  for (const ind of order) if (byIndustry[ind]?.length) ordered[ind] = byIndustry[ind]
  for (const [ind, list] of Object.entries(byIndustry))
    if (!(ind in ordered)) ordered[ind] = list
  return ordered
}

async function loadMeta() {
  try {
    const scopes = await dataPermissionApi.getScopes()
    dataScopeOptions.value = scopes
    // 先加载无过滤的全部模块作为兜底 (当无角色选中时展示)
    await loadModulesForRole(null)
  } catch (e) {
    ElMessage.error('加载元数据失败')
  }
}

async function loadModulesForRole(roleId: string | number | null) {
  try {
    const data = await dataPermissionApi.getModulesForRole({
      roleId: roleId ?? undefined,
      includeDisabled: true,
    })
    groupedModules.value = groupByIndustry(data.relevant)
    advancedGroupedModules.value = groupByIndustry(data.advanced)
    filterMeta.value = data.meta
    const nameMap: Record<string, string> = {}
    for (const m of [...data.relevant, ...data.advanced]) {
      nameMap[m.moduleCode] = m.moduleName
    }
    moduleNameMap.value = nameMap
  } catch (e) {
    ElMessage.error('加载模块失败')
  }
}

// 角色变化时刷新模块分流
watch(selectedRoleId, (id) => {
  loadModulesForRole(id)
})

function inferIndustry(domain: string): string {
  const d = (domain || '').toLowerCase()
  if (d === 'core' || d === 'inspection') return 'CORE'
  if (d === 'education' || d === 'edu') return 'EDU'
  if (d === 'healthcare' || d === 'health') return 'HEALTH'
  if (d === 'care' || d === 'elderly') return 'CARE'
  return 'CUSTOM'
}

async function loadRoles() {
  try {
    const res = await getRolesPage({ pageNum: 1, pageSize: 500, includeDisabled: true })
    allRoles.value = res.records
  } catch (e) {
    console.error(e)
  }
}

function onConfigLoaded(mps: ModulePermission[], d: SceneDecision, fbs: ScopeFallbackInfo[]) {
  currentModulePermissions.value = mps
  currentDecision.value = d
  currentFallbacks.value = fbs
}

function onConfigChanged(mps: ModulePermission[], d: SceneDecision, fbs: ScopeFallbackInfo[]) {
  currentModulePermissions.value = mps
  currentDecision.value = d
  currentFallbacks.value = fbs
}

function onSaved() {
  // no-op MVP
}

async function onEnableIndustry(industry: string) {
  if (!industry || industry === 'CORE' || industry === 'CUSTOM') return
  try {
    await ElMessageBox.confirm(
      `启用 ${industry} 插件? 其所有角色/权限/类型等贡献将级联恢复.`,
      '确认', { type: 'info' }
    )
    await pluginPlatformApi.enable(industry)
    ElMessage.success(`${industry} 已启用`)
    await loadRoles()  // 刷新 roles pluginEnabled 状态
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('启用失败: ' + (e?.message || e))
  }
}

function onApplyTemplate(tpl: RoleTemplate) {
  if (!currentRole.value) {
    ElMessage.warning('请先选择角色')
    return
  }
  configuratorRef.value?.applyTemplateScene(tpl.scene)
}

// ---- 对比 ----
interface CompareItem {
  role: RoleResponse
  mps: ModulePermission[]
  decision: SceneDecision
}
const compareData = ref<CompareItem[]>([])

async function onCompare(ids: (number | string)[]) {
  if (ids.length < 2) {
    ElMessage.warning('至少选 2 个角色')
    return
  }
  showCompareDialog.value = true
  compareLoading.value = true
  try {
    const list: CompareItem[] = []
    for (const id of ids) {
      const role = allRoles.value.find(r => String(r.id) === String(id))
      if (!role) continue
      const cfg = await dataPermissionApi.getConfig(id)
      const mps = cfg.modulePermissions || []
      list.push({ role, mps, decision: moduleScopesToScene(mps) })
    }
    compareData.value = list
  } catch (e) {
    ElMessage.error('加载对比数据失败')
  } finally {
    compareLoading.value = false
  }
}

function diffClass(cd: CompareItem, field: keyof SceneDecision): string {
  const value = (cd.decision as any)[field]
  const allSame = compareData.value.every(
    c => String((c.decision as any)[field] ?? '') === String(value ?? '')
  )
  return allSame ? 'text-gray-700' : 'text-orange-600'
}

function scopeLabel(code: PrimaryScope): string {
  const m: Record<string, string> = {
    ALL: '全部',
    DEPARTMENT_AND_BELOW: '部门及以下',
    DEPARTMENT: '仅本部门',
    SELF: '仅本人',
    CUSTOM: '自定义',
  }
  return m[code] || code
}

function jumpToRole(id: LongId | string) {
  showCompareDialog.value = false
  selectedRoleId.value = String(id)
}

onMounted(async () => {
  await Promise.all([loadMeta(), loadRoles()])
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
