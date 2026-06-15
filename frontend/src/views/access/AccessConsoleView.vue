<template>
  <div class="flex h-[calc(100vh-56px)] flex-col bg-gray-50">
    <!-- 顶部 header -->
    <div class="flex h-12 items-center justify-between border-b border-gray-200 bg-white px-5">
      <div class="flex items-center gap-3">
        <ShieldCheck class="h-5 w-5 text-blue-600" />
        <h1 class="text-base font-semibold text-gray-900">访问控制</h1>
        <!-- 顶层区切换 -->
        <div class="ml-2 flex items-center gap-0.5 rounded-lg bg-gray-100 p-0.5">
          <button :class="segCls('role')" @click="mode = 'role'">角色权限</button>
          <button :class="segCls('catalog')" @click="mode = 'catalog'">权限目录</button>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <button
          v-if="mode === 'role' && activeTab === 'data'"
          class="flex h-8 items-center gap-1.5 rounded-md border border-gray-200 bg-white px-3 text-xs font-medium text-gray-600 hover:bg-gray-50"
          @click="showTemplates = true"
        >
          <Library class="h-3.5 w-3.5" />
          模板库
        </button>
      </div>
    </div>

    <!-- ═══════════ 角色权限模式 ═══════════ -->
    <div v-if="mode === 'role'" class="flex flex-1 overflow-hidden">
      <!-- 左栏: 角色列表 -->
      <div class="w-64 flex-shrink-0 border-r border-gray-200">
        <RoleExplorer ref="roleExplorerRef" v-model="selectedRoleId" @compare="onCompare" @create="showCreate = true" />
      </div>

      <!-- 中间 + 右栏 -->
      <div class="flex flex-1 flex-col overflow-hidden">
        <!-- 未选角色 -->
        <div v-if="!currentRole" class="flex flex-1 flex-col items-center justify-center text-gray-400">
          <Users class="mb-3 h-12 w-12 opacity-40" />
          <p class="text-sm">从左侧选择一个角色，或点击「新建」</p>
        </div>

        <template v-else>
          <!-- 角色标题 + Tab -->
          <div class="border-b border-gray-200 bg-white">
            <div class="flex items-center gap-2 px-5 pt-3">
              <Shield class="h-4 w-4 text-blue-500" />
              <span class="text-sm font-semibold text-gray-900">{{ currentRole.roleName }}</span>
              <code class="font-mono text-xs text-gray-400">{{ currentRole.roleCode }}</code>
              <span v-if="currentRole.isSystem" class="rounded bg-purple-100 px-1.5 py-0.5 text-[10px] font-medium text-purple-600">系统内置</span>
              <span v-if="currentRole.pluginEnabled === false" class="rounded bg-orange-100 px-1.5 py-0.5 text-[10px] font-medium text-orange-600">插件已禁用</span>
            </div>
            <div class="flex gap-1 px-4">
              <button :class="tabCls('info')" @click="activeTab = 'info'">基本信息</button>
              <button :class="tabCls('func')" @click="activeTab = 'func'">功能权限</button>
              <button :class="tabCls('data')" @click="activeTab = 'data'">数据权限</button>
            </div>
          </div>

          <!-- Tab 内容 -->
          <div class="flex flex-1 overflow-hidden">
            <div class="flex-1 overflow-hidden">
              <RoleBasicInfoPanel
                v-if="activeTab === 'info'"
                :key="'info-' + currentRole.id"
                :role="currentRole"
                @saved="onRoleSaved"
                @deleted="onRoleDeleted"
              />
              <RolePermissionGrantPanel
                v-else-if="activeTab === 'func'"
                :key="'func-' + currentRole.id"
                :role-id="currentRole.id"
                :readonly="currentRole.pluginEnabled === false"
                @saved="onRoleSaved"
              />
              <PermissionConfigurator
                v-else
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
            <!-- 右栏: 预览 (仅数据权限 tab) -->
            <div v-if="activeTab === 'data'" class="w-80 flex-shrink-0 border-l border-gray-200">
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
        </template>
      </div>
    </div>

    <!-- ═══════════ 权限目录模式 (只读全局字典) ═══════════ -->
    <div v-else class="min-h-0 flex-1 overflow-y-auto">
      <PermissionCatalog />
    </div>

    <!-- 模板库 -->
    <TemplateLibraryDialog v-model:visible="showTemplates" @apply="onApplyTemplate" />

    <!-- 新建角色 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showCreate" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="showCreate = false" />
          <div class="relative w-full max-w-md rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">新建角色</h3>
              <button class="rounded p-1 hover:bg-gray-100" @click="showCreate = false"><X class="h-5 w-5 text-gray-500" /></button>
            </div>
            <div class="space-y-4 p-6">
              <div>
                <label class="mb-1 block text-sm text-gray-600">角色编码 <span class="text-red-500">*</span></label>
                <input v-model="createForm.roleCode" placeholder="大写字母开头，如 DEPT_MANAGER" class="h-9 w-full rounded-lg border border-gray-300 px-3 font-mono text-sm focus:border-blue-500 focus:outline-none" />
                <p class="mt-1 text-xs text-gray-400">大写字母开头，仅大写字母/数字/下划线，长度 3-50</p>
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-600">角色名称 <span class="text-red-500">*</span></label>
                <input v-model="createForm.roleName" placeholder="请输入角色名称" class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-600">描述</label>
                <textarea v-model="createForm.description" rows="2" class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
            </div>
            <div class="flex justify-end gap-3 border-t border-gray-200 px-6 py-4">
              <button class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50" @click="showCreate = false">取消</button>
              <button :disabled="createLoading" class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50" @click="onCreateSubmit">
                <Loader2 v-if="createLoading" class="h-4 w-4 animate-spin" />
                创建
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 角色对比 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showCompareDialog" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="showCompareDialog = false" />
          <div class="relative w-full max-w-5xl rounded-xl bg-white shadow-2xl">
            <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4">
              <h3 class="text-lg font-semibold text-gray-900">角色对比</h3>
              <button class="rounded p-1.5 text-gray-400 hover:bg-gray-100" @click="showCompareDialog = false"><X class="h-5 w-5" /></button>
            </div>
            <div class="p-5">
              <div v-if="compareLoading" class="flex justify-center py-8"><Loader2 class="h-6 w-6 animate-spin text-blue-500" /></div>
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
                    <div class="flex items-center justify-between"><span class="text-gray-500">主决策</span><span class="font-medium" :class="diffClass(cd, 'primary')">{{ scopeLabel(cd.decision.primary) }}</span></div>
                    <div v-if="cd.decision.specializations && Object.keys(cd.decision.specializations).length" class="flex items-center justify-between">
                      <span class="text-gray-500">行业特化</span>
                      <span class="font-medium">{{ Object.entries(cd.decision.specializations).map(([g, s]) => `${g}:${scopeLabel(s)}`).join(', ') }}</span>
                    </div>
                    <div class="flex items-center justify-between"><span class="text-gray-500">业务跟随</span><span class="font-medium" :class="diffClass(cd, 'bizAutoFollow')">{{ cd.decision.bizAutoFollow ? '是' : '否' }}</span></div>
                    <div class="flex items-center justify-between"><span class="text-gray-500">模块数</span><span class="font-medium text-gray-700">{{ cd.mps.length }}</span></div>
                  </div>
                  <button class="mt-3 h-7 w-full rounded bg-blue-50 text-[11px] font-medium text-blue-600 hover:bg-blue-100" @click="jumpToRole(cd.role.id)">打开配置 ></button>
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
import { ref, computed, onMounted, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pluginPlatformApi } from '@/api/pluginPlatform'
import { ShieldCheck, Shield, Library, X, Loader2, Users } from 'lucide-vue-next'
import RoleExplorer from './data-permissions/components/RoleExplorer.vue'
import PermissionConfigurator from './data-permissions/components/PermissionConfigurator.vue'
import PreviewPanel from './data-permissions/components/PreviewPanel.vue'
import TemplateLibraryDialog from './data-permissions/components/TemplateLibraryDialog.vue'
import RoleBasicInfoPanel from './components/RoleBasicInfoPanel.vue'
import RolePermissionGrantPanel from './components/RolePermissionGrantPanel.vue'
import PermissionCatalog from '@/views/system/PermissionsView.vue'
import type { ModuleGroupItem } from './data-permissions/components/AdvancedModuleEditor.vue'
import { dataPermissionApi, getRolesPage, createRole, type RoleResponse } from '@/api/access'
import type { CreateRoleRequest } from '@/types'
import type { DataScopeOption, ModulePermission } from '@/types/access'
import type { SceneDecision, ScopeFallbackInfo } from './data-permissions/composables/useSceneTemplate'
import { moduleScopesToScene } from './data-permissions/composables/useSceneTemplate'
import { enabledScopeSpecializations } from './data-permissions/dataScopeSpecializations'
import { usePluginsStore } from '@/stores/plugins'
import type { RoleTemplate } from './data-permissions/composables/useTemplateLibrary'

const route = useRoute()
const router = useRouter()

const mode = ref<'role' | 'catalog'>('role')
const activeTab = ref<'info' | 'func' | 'data'>('info')

function segCls(m: string) {
  return [
    'rounded-md px-3 py-1 text-xs font-medium transition',
    mode.value === m ? 'bg-white text-blue-600 shadow-sm' : 'text-gray-500 hover:text-gray-700',
  ]
}
function tabCls(t: string) {
  return [
    '-mb-px border-b-2 px-3 py-2 text-sm font-medium transition',
    activeTab.value === t ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700',
  ]
}

const configuratorRef = ref<InstanceType<typeof PermissionConfigurator> | null>(null)
const roleExplorerRef = ref<InstanceType<typeof RoleExplorer> | null>(null)
const showTemplates = ref(false)
const showCompareDialog = ref(false)
const compareLoading = ref(false)

const groupedModules = ref<Record<string, ModuleGroupItem[]>>({})
const advancedGroupedModules = ref<Record<string, ModuleGroupItem[]>>({})
const filterMeta = ref<{ filtered: boolean; filterRule?: string; totalRelevant?: number; totalAdvanced?: number; roleIndustry?: string; rolePermModules?: string[] }>({ filtered: false })
const moduleNameMap = ref<Record<string, string>>({})
const pluginsStore = usePluginsStore()
const dataScopeOptions = ref<DataScopeOption[]>([])
const allRoles = ref<RoleResponse[]>([])

// 当前角色 id (同步 URL). 保持字符串 — snowflake id 超 2^53, 不能 Number()。
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
  return allRoles.value.find((r) => String(r.id) === String(id)) || null
})

const currentModulePermissions = ref<ModulePermission[]>([])
const currentDecision = ref<SceneDecision>({ primary: 'SELF', bizAutoFollow: true })
const currentFallbacks = ref<ScopeFallbackInfo[]>([])
const totalModules = computed(() => Object.values(groupedModules.value).reduce((s, l) => s + l.length, 0))

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
      typeField: (m as any).typeField ?? null,
      typeEntity: (m as any).typeEntity ?? null,
    })
  }
  const order = ['CORE', 'EDU', 'HEALTH', 'CARE', 'CUSTOM']
  const ordered: Record<string, ModuleGroupItem[]> = {}
  for (const ind of order) if (byIndustry[ind]?.length) ordered[ind] = byIndustry[ind]
  for (const [ind, list] of Object.entries(byIndustry)) if (!(ind in ordered)) ordered[ind] = list
  return ordered
}

async function loadMeta() {
  try {
    dataScopeOptions.value = await dataPermissionApi.getScopes()
    await loadModulesForRole(null)
  } catch (e) {
    ElMessage.error('加载元数据失败')
  }
}

async function loadModulesForRole(roleId: LongId | null) {
  try {
    const data = await dataPermissionApi.getModulesForRole({ roleId: roleId ?? undefined, includeDisabled: true })
    groupedModules.value = groupByIndustry(data.relevant)
    advancedGroupedModules.value = groupByIndustry(data.advanced)
    filterMeta.value = data.meta
    const nameMap: Record<string, string> = {}
    for (const m of [...data.relevant, ...data.advanced]) nameMap[m.moduleCode] = m.moduleName
    moduleNameMap.value = nameMap
  } catch (e) {
    ElMessage.error('加载模块失败')
  }
}

watch(selectedRoleId, (id) => loadModulesForRole(id))

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
function onSaved() { /* no-op */ }

async function onEnableIndustry(industry: string) {
  if (!industry || industry === 'CORE' || industry === 'CUSTOM') return
  try {
    await ElMessageBox.confirm(`启用 ${industry} 插件? 其所有角色/权限/类型等贡献将级联恢复.`, '确认', { type: 'info' })
    await pluginPlatformApi.enable(industry)
    ElMessage.success(`${industry} 已启用`)
    await refreshRoles()
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

// ---- 角色 CRUD ----
const showCreate = ref(false)
const createLoading = ref(false)
const createForm = reactive({ roleCode: '', roleName: '', description: '' })

async function onCreateSubmit() {
  if (!createForm.roleName.trim()) { ElMessage.error('请填写角色名称'); return }
  if (!/^[A-Z][A-Z0-9_]{2,49}$/.test(createForm.roleCode)) {
    ElMessage.error('角色编码必须以大写字母开头，仅允许大写字母/数字/下划线，长度 3-50')
    return
  }
  createLoading.value = true
  try {
    const data: CreateRoleRequest = { roleCode: createForm.roleCode, roleName: createForm.roleName.trim(), description: createForm.description, level: 0 }
    const created = await createRole(data)
    ElMessage.success('创建成功')
    showCreate.value = false
    Object.assign(createForm, { roleCode: '', roleName: '', description: '' })
    await refreshRoles()
    if (created?.id != null) selectedRoleId.value = String(created.id)
  } catch (e: any) {
    ElMessage.error(e?.message || '创建失败')
  } finally {
    createLoading.value = false
  }
}

async function refreshRoles() {
  await loadRoles()
  roleExplorerRef.value?.reload()
}

async function onRoleSaved() { await refreshRoles() }
async function onRoleDeleted() {
  selectedRoleId.value = null as any
  activeTab.value = 'info'
  await refreshRoles()
}

// ---- 对比 ----
interface CompareItem { role: RoleResponse; mps: ModulePermission[]; decision: SceneDecision }
const compareData = ref<CompareItem[]>([])

async function onCompare(ids: (number | string)[]) {
  if (ids.length < 2) { ElMessage.warning('至少选 2 个角色'); return }
  showCompareDialog.value = true
  compareLoading.value = true
  try {
    const list: CompareItem[] = []
    for (const id of ids) {
      const role = allRoles.value.find((r) => String(r.id) === String(id))
      if (!role) continue
      const cfg = await dataPermissionApi.getConfig(String(id))
      const mps = cfg.modulePermissions || []
      list.push({ role, mps, decision: moduleScopesToScene(mps, enabledScopeSpecializations(pluginsStore.codes)) })
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
  const allSame = compareData.value.every((c) => String((c.decision as any)[field] ?? '') === String(value ?? ''))
  return allSame ? 'text-gray-700' : 'text-orange-600'
}

function scopeLabel(code: string): string {
  const fromDict = dataScopeOptions.value.find((s) => s.scopeCode === code)?.scopeName
  if (fromDict) return fromDict
  const m: Record<string, string> = { ALL: '全部', DEPARTMENT_AND_BELOW: '部门及以下', DEPARTMENT: '仅本部门', SELF: '仅本人', CUSTOM: '自定义' }
  return m[code] || code
}

function jumpToRole(id: LongId | string) {
  showCompareDialog.value = false
  mode.value = 'role'
  activeTab.value = 'data'
  selectedRoleId.value = String(id)
}

onMounted(async () => {
  await Promise.all([loadMeta(), loadRoles()])
})
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-from,
.modal-leave-to { opacity: 0; }
</style>
