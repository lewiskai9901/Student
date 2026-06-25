<template>
  <div class="flex h-full flex-col">
    <!-- ── 头部 ── -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-5 py-3">
      <div class="min-w-0">
        <h2 class="truncate text-base font-semibold text-gray-900">
          {{ currentRole?.roleName || '未选择角色' }}
        </h2>
        <p class="truncate text-[11px] text-gray-500">
          {{ currentRole?.roleCode || '请在左侧选择要配置的角色' }}
        </p>
      </div>
      <div v-if="currentRole" class="flex items-center gap-2">
        <button
          class="flex h-8 items-center gap-1 rounded-md border border-gray-200 bg-white px-3 text-xs font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-40"
          :disabled="roleDisabled"
          @click="templateDialogOpen = true"
        >
          模板
          <ChevronDown class="h-3 w-3" />
        </button>
        <button
          class="flex h-8 items-center gap-1.5 rounded-md border border-gray-200 bg-white px-3 text-xs font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-40"
          :disabled="roleDisabled"
          @click="handleReset"
        >
          重置
        </button>
        <button
          class="flex h-8 items-center gap-1.5 rounded-md bg-blue-600 px-4 text-xs font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          :disabled="saving || roleDisabled"
          @click="handleSave"
        >
          保存
        </button>
      </div>
    </div>

    <div v-if="!currentRole" class="flex flex-1 items-center justify-center text-sm text-gray-400">
      请在左侧选择一个角色
    </div>
    <div v-else-if="loading" class="flex flex-1 items-center justify-center text-sm text-gray-400">
      加载中…
    </div>

    <div v-else class="flex flex-1 overflow-hidden">
      <!-- 配置区: 全部资源, 一套关系规则逐资源配置 -->
      <div class="flex-1 overflow-y-auto px-5 py-4">
        <div class="mb-3 flex items-center gap-2">
          <h3 class="text-sm font-semibold text-gray-900">数据范围 · 按关系逐资源配置</h3>
          <span class="text-[11px] text-gray-400">— 每个资源 = 满足任一关系条件即可见（我创建的 / 和我有某关系的组织 / 全部 / 由插件解析）</span>
        </div>
        <input
          v-model="search"
          placeholder="搜索资源…"
          class="mb-3 h-8 w-full rounded-md border border-gray-300 px-3 text-xs outline-none focus:border-blue-500"
        />

        <div v-for="(mods, industry) in groupedModules" :key="industry" class="mb-4">
          <div class="mb-1.5 px-1 text-[11px] font-semibold uppercase tracking-wide text-gray-400">
            {{ industry }} <span class="font-normal normal-case">({{ mods.length }})</span>
          </div>
          <div class="space-y-2">
            <div
              v-for="m in mods"
              :key="m.code"
              class="rounded-md border border-gray-200 bg-white p-3"
              :class="{ 'opacity-60': m.pluginEnabled === false }"
            >
              <div class="mb-2 text-xs font-medium text-gray-800">
                {{ m.name }}
                <span v-if="m.pluginEnabled === false" class="ml-1 text-[10px] text-orange-500">(插件已禁用)</span>
              </div>
              <MultiGrantEditor
                :module-code="m.code"
                :model-value="grantsOf(m.code)"
                :disabled="roleDisabled || m.pluginEnabled === false"
                :as-user-id="simulateUserId"
                @update:model-value="(v: RelationGrant[]) => setGrants(m.code, v)"
              />
            </div>
          </div>
        </div>
        <div v-if="props.modules.length === 0" class="py-8 text-center text-xs text-gray-400">
          当前无可配置的资源
        </div>
      </div>

      <!-- 模拟区 -->
      <aside class="w-80 flex-shrink-0 overflow-y-auto border-l border-gray-200 bg-gray-50 px-4 py-4">
        <div class="rounded-md border border-blue-200 bg-blue-50 px-3 py-3">
          <div class="mb-2 flex items-center justify-between">
            <span class="flex items-center gap-1 text-[11px] font-medium text-blue-700">
              <User class="h-3 w-3" />
              模拟用户
            </span>
            <button
              v-if="simulateResults.length"
              class="text-[10px] text-blue-500 hover:underline"
              @click="resetSimulation"
            >
              清空
            </button>
          </div>
          <div class="mb-2 flex items-center gap-2">
            <input
              v-model="simulateUserId"
              type="text"
              inputmode="numeric"
              placeholder="输入用户 ID (如 1)"
              class="h-7 flex-1 rounded border border-gray-300 px-2 text-[11px] outline-none focus:border-blue-500"
              @keyup.enter="runSimulate"
            />
            <button
              class="h-7 rounded bg-blue-600 px-3 text-[11px] font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              :disabled="!simulateUserId || simulating"
              @click="runSimulate"
            >
              <Loader2 v-if="simulating" class="inline h-2.5 w-2.5 animate-spin" />
              <span v-else>模拟</span>
            </button>
          </div>
          <p class="mb-1.5 text-[10px] text-blue-600">按当前编辑中的范围模拟 — 保存后才会真正生效</p>
          <div v-if="simulateError" class="text-[10.5px] text-red-600">{{ simulateError }}</div>
          <div v-else-if="simulateResults.length" class="space-y-1">
            <div v-for="r in topResults" :key="r.moduleCode" class="flex items-start gap-1 text-[10.5px]">
              <span class="w-20 flex-shrink-0 truncate font-medium text-blue-900">{{ moduleName(r.moduleCode) }}</span>
              <span class="flex-1">
                <span v-if="r.accessibleCount >= 0" class="font-semibold text-blue-800">{{ r.accessibleCount }} 条</span>
                <span v-else class="italic text-amber-600">{{ r.note || '未支持' }}</span>
                <span v-if="r.samples?.length" class="ml-1 text-[10px] text-gray-500">· {{ r.samples.map(s => s.name || s.id).join(', ') }}</span>
              </span>
            </div>
            <button
              v-if="simulateResults.length > 5"
              class="mt-1 w-full text-[10px] text-blue-500 hover:underline"
              @click="expandAll = !expandAll"
            >
              {{ expandAll ? '收起' : `展开全部 ${simulateResults.length} 个资源` }}
            </button>
          </div>
          <div v-else class="text-[10px] text-blue-600">输入用户 ID 预览此角色下该用户实际能访问的数据</div>
        </div>
      </aside>
    </div>

    <TemplateLibraryDialog
      :visible="templateDialogOpen"
      @update:visible="templateDialogOpen = $event"
      @apply="applyTemplate"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChevronDown, User, Loader2 } from 'lucide-vue-next'
import MultiGrantEditor from './MultiGrantEditor.vue'
import TemplateLibraryDialog from './TemplateLibraryDialog.vue'
import {
  dataPermissionApi,
  dataPermissionSimulateApi,
  type RoleResponse,
  type SimulateResult,
} from '@/api/access'
import type { ModulePermission, RelationGrant } from '@/types/access'
import type { ScopeSpecVM } from './ScopeBuilder.vue'
import { specToCommand, presetCodeToSpec } from '../composables/scopePolicy'
import { permissionToGrants } from '../composables/scopeRelation'
import { sceneToModuleScopes } from '../composables/useSceneTemplate'
import type { RoleTemplate } from '../composables/useTemplateLibrary'
import { enabledScopeSpecializations } from '../dataScopeSpecializations'
import { usePluginsStore } from '@/stores/plugins'

/** 一个资源 (数据模块) + 能力声明。 */
export interface ModuleCapability {
  code: string
  name: string
  industry?: string
  allowedScopes?: string[] | null
  relationFilterable?: boolean
  typeEntity?: string | null
  pluginEnabled?: boolean
}

interface Props {
  currentRole: RoleResponse | null
  modules: ModuleCapability[]
}
const props = defineProps<Props>()
const emit = defineEmits<{ saved: [] }>()

const loading = ref(false)
const saving = ref(false)
const search = ref('')

/**
 * 逐资源 spec (relationGrants=关系条件, 一切皆关系; 同时保全 axis②③ 排除关系/类型过滤, 不丢)。
 * key=moduleCode。编辑器只动 relationGrants, 其余字段透传 (避免高级配置丢失)。
 */
const specByCode = ref<Record<string, ScopeSpecVM>>({})

/** 上次 getConfig 原始列表 (未托管模块直通保留用)。 */
const loadedPermissions = ref<ModulePermission[]>([])

const templateDialogOpen = ref(false)
const pluginsStore = usePluginsStore()
const roleDisabled = computed(() => props.currentRole?.pluginEnabled === false)

const moduleByCode = computed<Map<string, ModuleCapability>>(
  () => new Map(props.modules.map(m => [m.code, m]))
)
function moduleName(code: string): string {
  return moduleByCode.value.get(code)?.name || code
}

const groupedModules = computed<Record<string, ModuleCapability[]>>(() => {
  const kw = search.value.trim().toLowerCase()
  const out: Record<string, ModuleCapability[]> = {}
  for (const m of props.modules) {
    if (kw && !m.name.toLowerCase().includes(kw) && !m.code.toLowerCase().includes(kw)) continue
    const ind = m.industry || 'CORE'
    ;(out[ind] ||= []).push(m)
  }
  return out
})

function grantsOf(code: string): RelationGrant[] {
  const g = specByCode.value[code]?.relationGrants
  return g && g.length ? g : [{ relation: 'creator', subject: 'SELF' }]
}
function setGrants(code: string, grants: RelationGrant[]) {
  specByCode.value = {
    ...specByCode.value,
    [code]: { ...(specByCode.value[code] || {}), relationGrants: grants },
  }
}

// ── 模板载入: 每资源 scope → grants ──
function applyTemplate(tpl: RoleTemplate) {
  const simpleModules = props.modules.map(m => ({
    code: m.code,
    industry: m.industry || 'CORE',
    allowedScopes: m.allowedScopes ?? null,
  }))
  const relevantCodes = new Set(props.modules.map(m => m.code))
  const specs = enabledScopeSpecializations(pluginsStore.codes)
  const { scopes } = sceneToModuleScopes(tpl.scene, simpleModules, specs, relevantCodes)
  const next: Record<string, ScopeSpecVM> = {}
  for (const m of props.modules) {
    // preset scopeCode → 轴① spec → grants (复用 permissionToGrants 的 legacy 派生)
    next[m.code] = { relationGrants: permissionToGrants(presetCodeToSpec(scopes[m.code]?.scopeCode || 'SELF') as ModulePermission) }
  }
  specByCode.value = next
  ElMessage.info('模板已载入, 点击保存生效')
}

// ── 加载 / 重置 ──
async function load() {
  if (!props.currentRole) return
  loading.value = true
  try {
    const config = await dataPermissionApi.getConfig(props.currentRole.id)
    const loaded = config.modulePermissions || []
    loadedPermissions.value = loaded
    const byCode = new Map(loaded.map(m => [m.moduleCode, m]))
    const next: Record<string, ScopeSpecVM> = {}
    for (const m of props.modules) {
      const mp = byCode.get(m.code)
      // relationGrants=关系条件; 同时透传 axis②③ (排除关系/类型过滤) 防丢
      next[m.code] = {
        relationGrants: permissionToGrants(mp),
        subjectRelInclude: mp?.subjectRelInclude,
        subjectRelExclude: mp?.subjectRelExclude,
        typeFilter: mp?.typeFilter,
      }
    }
    specByCode.value = next
  } catch (e: any) {
    ElMessage.error('加载数据权限失败: ' + (e?.message || e))
  } finally {
    loading.value = false
  }
}

watch(
  () => props.currentRole?.id,
  id => {
    if (id != null) load()
    else {
      specByCode.value = {}
      loadedPermissions.value = []
    }
  },
  { immediate: true }
)

async function handleReset() {
  try {
    await ElMessageBox.confirm('重置为已保存的配置? 未保存的修改将丢失.', '重置确认', { type: 'warning' })
    await load()
  } catch {
    /* cancelled */
  }
}

/** 逐资源 grants → per-resource commands (后端优先用 relationGrants)。 */
function buildCommands(): ModulePermission[] {
  const managedCodes = props.modules.map(m => m.code)
  // 全 spec 下发 (relationGrants + 透传的 axis②③); 后端优先用 relationGrants。
  const cmds = managedCodes.map(code =>
    specToCommand(code, specByCode.value[code] || { relationGrants: grantsOf(code) })
  )
  const managedSet = new Set(managedCodes)
  for (const mp of loadedPermissions.value) {
    if (!managedSet.has(mp.moduleCode)) {
      cmds.push({ ...mp })
      managedSet.add(mp.moduleCode)
    }
  }
  return cmds
}

async function handleSave() {
  if (!props.currentRole) return
  if (props.modules.length === 0) {
    ElMessage.warning('当前无可配置的资源, 已取消保存以避免误删已有配置')
    return
  }
  const cmds = buildCommands()
  // Guard: 禁用插件的资源不允许配置非"我创建的/SELF"
  const violators = cmds.filter(c => {
    const m = moduleByCode.value.get(c.moduleCode)
    return m?.pluginEnabled === false && c.scopeCode !== 'SELF'
  })
  if (violators.length) {
    ElMessage.warning(`${violators.length} 个资源所属插件已禁用, 请先启用插件或将其范围设为"我创建的"`)
    return
  }
  saving.value = true
  try {
    await dataPermissionApi.saveConfig(props.currentRole.id, {
      roleId: props.currentRole.id,
      roleName: props.currentRole.roleName,
      modulePermissions: cmds,
    })
    ElMessage.success('数据权限保存成功')
    emit('saved')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ── 模拟用户 ──
// 用户 id 是雪花 (19 位, 超 JS 安全整数) → 必须 string, 不能 v-model.number (会精度截断查错用户)。
const simulateUserId = ref<string>('')
const simulating = ref(false)
const simulateResults = ref<SimulateResult[]>([])
const simulateError = ref('')
const expandAll = ref(false)
const topResults = computed(() =>
  expandAll.value ? simulateResults.value : simulateResults.value.slice(0, 5)
)
function resetSimulation() {
  simulateResults.value = []
  simulateError.value = ''
  expandAll.value = false
}
async function runSimulate() {
  const uid = simulateUserId.value.trim()
  if (!uid) return
  if (props.modules.length === 0) {
    simulateError.value = '当前无可配置的资源, 无法模拟'
    return
  }
  const snapshot = buildCommands()
  simulating.value = true
  simulateError.value = ''
  try {
    const res = await dataPermissionSimulateApi.simulate({
      userId: uid,
      modulePermissions: snapshot.map(mp => ({
        moduleCode: mp.moduleCode,
        scopeCode: mp.scopeCode,
        scopeItems: mp.scopeItems as any,
      })),
    })
    simulateResults.value = res?.results || []
    if (res?.error) simulateError.value = res.error
  } catch (e: any) {
    simulateError.value = '模拟失败: ' + (e?.response?.data?.message || e?.message || String(e))
  } finally {
    simulating.value = false
  }
}

defineExpose({ reload: load })
</script>
