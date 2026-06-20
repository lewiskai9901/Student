<template>
  <div class="flex h-full flex-col">
    <!-- ── 头部: 角色 + 模板 + 重置 + 保存 ── -->
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
        <!-- 模板库: 选模板 → 载入为 默认+例外 (不自动保存) -->
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

    <!-- 无角色 / 加载 -->
    <div v-if="!currentRole" class="flex flex-1 items-center justify-center text-sm text-gray-400">
      请在左侧选择一个角色
    </div>
    <div v-else-if="loading" class="flex flex-1 items-center justify-center text-sm text-gray-400">
      加载中…
    </div>

    <!-- 主体: 配置 (左) + 预览 (右) -->
    <div v-else class="flex flex-1 overflow-hidden">
      <!-- 配置区 -->
      <div class="flex-1 overflow-y-auto px-5 py-4 space-y-5">
        <!-- ① 默认范围 (仅轴① 看哪些组织的数据, 适用所有资源) -->
        <section class="rounded-lg border border-gray-200 bg-white p-4">
          <div class="mb-1 flex items-center gap-2">
            <h3 class="text-sm font-semibold text-gray-900">默认范围</h3>
          </div>
          <p class="mb-2 text-[11px] leading-relaxed text-gray-500">
            先设一个对所有功能都生效的范围；个别功能要不一样，在下面「添加例外」单独设。
          </p>
          <ScopeBuilder
            :model-value="defaultSpec"
            axis-only-org
            :disabled="roleDisabled"
            @update:model-value="(v: ScopeSpecVM) => (defaultSpec = v)"
          />
        </section>

        <!-- ② 资源例外 (完整三轴, 只列与默认不同的) -->
        <section class="rounded-lg border border-gray-200 bg-white p-4">
          <div class="mb-3 flex items-center justify-between">
            <div class="flex items-center gap-2">
              <h3 class="text-sm font-semibold text-gray-900">资源例外</h3>
              <span class="text-[11px] text-gray-400">— 个别功能想和默认不同，在这里单独配（只列出与默认不同的）</span>
            </div>
            <button
              class="flex h-7 items-center gap-1 rounded-md border border-blue-200 bg-blue-50 px-2.5 text-[11px] font-medium text-blue-600 hover:bg-blue-100 disabled:opacity-40"
              :disabled="roleDisabled || addableModules.length === 0"
              @click="pickerOpen = true"
            >
              <Plus class="h-3 w-3" />
              添加例外
            </button>
          </div>

          <div v-if="exceptions.length === 0" class="py-3 text-center text-[11px] text-gray-400">
            暂无例外 — 所有资源跟随默认范围
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="(ex, i) in exceptions"
              :key="ex.moduleCode"
              class="rounded-md border border-gray-200 bg-gray-50/60 p-3"
            >
              <div class="mb-2 flex items-center justify-between">
                <span class="text-xs font-medium text-gray-800">
                  {{ moduleName(ex.moduleCode) }}
                </span>
                <button
                  class="flex h-6 w-6 items-center justify-center rounded text-gray-400 hover:bg-red-50 hover:text-red-500 disabled:opacity-40"
                  :disabled="roleDisabled"
                  title="移除例外"
                  @click="removeException(i)"
                >
                  <X class="h-3.5 w-3.5" />
                </button>
              </div>
              <!-- R3/R4 多锚点 (>1 grant): 只读卡 + 改为单一范围 (ScopeBuilder 暂不编辑多 grant) -->
              <div v-if="isMultiGrantSpec(ex.spec)" class="space-y-2">
                <div class="flex flex-wrap gap-1.5">
                  <span
                    v-for="(g, gi) in ex.spec.relationGrants"
                    :key="gi"
                    class="inline-flex items-center rounded bg-indigo-50 px-2 py-0.5 text-[11px] text-indigo-700"
                  >
                    {{ grantLabel(g) }}
                  </span>
                </div>
                <div class="flex items-center gap-2 text-[11px] text-gray-500">
                  <span>多锚点 (满足任一即可见)</span>
                  <button
                    class="text-indigo-600 hover:underline disabled:opacity-40"
                    :disabled="roleDisabled"
                    @click="simplifyToSingle(i)"
                  >
                    改为单一范围
                  </button>
                </div>
              </div>
              <ScopeBuilder
                v-else
                :model-value="ex.spec"
                :capabilities="capabilityOf(ex.moduleCode)"
                :disabled="roleDisabled"
                @update:model-value="(v: ScopeSpecVM) => updateException(i, v)"
              />
            </div>
          </div>

          <!-- 其余资源跟随默认 -->
          <div class="mt-3 border-t border-gray-100 pt-3">
            <div class="flex items-center justify-between">
              <span class="text-[11px] text-gray-500">
                其余 {{ followerModules.length }} 个资源 → 跟随默认
              </span>
              <button
                class="text-[11px] text-blue-500 hover:underline"
                @click="showFollowers = !showFollowers"
              >
                {{ showFollowers ? '收起' : '展开全部' }}
              </button>
            </div>
            <ul v-if="showFollowers" class="mt-2 max-h-48 space-y-1 overflow-y-auto">
              <li
                v-for="m in followerModules"
                :key="m.code"
                class="flex items-center justify-between text-[11px] text-gray-600"
              >
                <span class="truncate">{{ m.name }}</span>
                <span class="ml-2 flex-shrink-0 text-gray-400">{{ followerEffectiveLine(m.code) }}</span>
              </li>
            </ul>
          </div>
        </section>
      </div>

      <!-- 预览区 -->
      <aside class="w-80 flex-shrink-0 overflow-y-auto border-l border-gray-200 bg-gray-50 px-4 py-4">
        <h3 class="mb-1 text-sm font-semibold text-gray-900">预览 · 此角色实际能看到</h3>
        <p class="mb-3 text-[11px] text-gray-500">逐资源自然语言</p>

        <div class="space-y-2">
          <!-- 默认行 -->
          <div class="rounded-md border border-gray-200 bg-white px-3 py-2">
            <div class="text-[11px] font-medium text-gray-500">默认</div>
            <div class="text-xs text-gray-800">{{ defaultPreviewLine }}</div>
          </div>
          <!-- 例外逐行 -->
          <div
            v-for="ex in exceptions"
            :key="ex.moduleCode"
            class="rounded-md border border-amber-200 bg-amber-50 px-3 py-2"
          >
            <div class="text-[11px] font-medium text-amber-700">{{ moduleName(ex.moduleCode) }}</div>
            <div class="text-xs text-amber-900">{{ previewLineFor(ex.spec) }}</div>
          </div>
          <!-- 其余 -->
          <div class="rounded-md border border-gray-200 bg-white px-3 py-2">
            <div class="text-[11px] font-medium text-gray-500">其余资源</div>
            <div class="text-xs text-gray-600">→ {{ defaultPreviewLine }}</div>
          </div>
        </div>

        <!-- 模拟用户: 输入用户 ID → 看此角色下该用户实际能访问的数据 -->
        <div class="mt-4 rounded-md border border-blue-200 bg-blue-50 px-3 py-3">
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
              v-model.number="simulateUserId"
              type="number"
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

          <p class="mb-1.5 text-[10px] text-blue-600">
            按当前编辑中的范围模拟 — 保存后才会真正生效
          </p>

          <div v-if="simulateError" class="text-[10.5px] text-red-600">
            {{ simulateError }}
          </div>

          <div v-else-if="simulateResults.length" class="space-y-1">
            <div
              v-for="r in topResults"
              :key="r.moduleCode"
              class="flex items-start gap-1 text-[10.5px]"
            >
              <span class="w-20 flex-shrink-0 truncate font-medium text-blue-900">
                {{ moduleName(r.moduleCode) }}
              </span>
              <span class="flex-1">
                <span v-if="r.accessibleCount >= 0" class="font-semibold text-blue-800">
                  {{ r.accessibleCount }} 条
                </span>
                <span v-else class="italic text-amber-600">
                  {{ r.note || '未支持' }}
                </span>
                <span v-if="r.samples?.length" class="ml-1 text-[10px] text-gray-500">
                  · {{ r.samples.map(s => s.name || s.id).join(', ') }}
                </span>
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

          <div v-else class="text-[10px] text-blue-600">
            输入用户 ID 预览此角色下该用户实际能访问的数据
          </div>
        </div>
      </aside>
    </div>

    <!-- 例外资源选择器 -->
    <el-dialog v-model="pickerOpen" title="添加资源例外" width="480px">
      <el-input
        v-model="pickerSearch"
        placeholder="搜索资源"
        size="small"
        clearable
        class="mb-2"
      />
      <div class="max-h-80 overflow-y-auto">
        <div v-for="(mods, industry) in pickerGrouped" :key="industry" class="mb-2">
          <div class="px-1 py-1 text-[11px] font-medium uppercase tracking-wide text-gray-400">
            {{ industry }}
          </div>
          <button
            v-for="m in mods"
            :key="m.code"
            class="flex w-full items-center justify-between rounded px-2 py-1.5 text-left text-xs text-gray-700 hover:bg-blue-50"
            @click="addException(m.code)"
          >
            <span class="truncate">{{ m.name }}</span>
            <Plus class="h-3 w-3 flex-shrink-0 text-blue-400" />
          </button>
        </div>
        <div v-if="addableModules.length === 0" class="py-6 text-center text-xs text-gray-400">
          所有资源都已是例外
        </div>
      </div>
    </el-dialog>

    <!-- 模板库 -->
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
import { ChevronDown, Plus, X, User, Loader2 } from 'lucide-vue-next'
import ScopeBuilder, { type ScopeSpecVM, type ScopeCapabilities } from './ScopeBuilder.vue'
import TemplateLibraryDialog from './TemplateLibraryDialog.vue'
import {
  dataPermissionApi,
  dataPermissionSimulateApi,
  type RoleResponse,
  type SimulateResult,
} from '@/api/access'
import type { ModulePermission, RelationGrant } from '@/types/access'
import {
  inferDefaultAndExceptions,
  expandToCommands,
  clampSpecToAllowed,
  scopeCodeFromAxis1,
  presetCodeToSpec,
  type ResourceException,
} from '../composables/scopePolicy'
import { sceneToModuleScopes } from '../composables/useSceneTemplate'
import type { RoleTemplate } from '../composables/useTemplateLibrary'
import { enabledScopeSpecializations } from '../dataScopeSpecializations'
import { usePluginsStore } from '@/stores/plugins'
import { useScopeLabels } from '../composables/useScopeLabels'

/**
 * 一个资源 (数据模块) + 其能力声明 (扁平化, 来自 M1 getModulesForRole 暴露的字段).
 * 父组件 (P5) 把 getModulesForRole 的 relevant/advanced item 映射到本形状.
 */
export interface ModuleCapability {
  code: string
  name: string
  industry?: string
  /** 本资源支持的 preset scope 代码; null=全集 */
  allowedScopes?: string[] | null
  /** 轴② 关系过滤可用 */
  relationFilterable?: boolean
  /** 轴③ 类型选项来源实体 (USER/PLACE/ORG_UNIT); 非空=支持类型过滤 */
  typeEntity?: string | null
  /** 所属插件是否启用 */
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

const defaultSpec = ref<ScopeSpecVM>({ orgAnchor: 'SELF' })
const exceptions = ref<ResourceException[]>([])

/**
 * 上次 getConfig 载入的原始 per-resource 列表 (含 props.modules 之外的码).
 * 保存时用于"未托管模块直通保留" — 后端 saveRolePermissions 是 delete-all-then-insert,
 * 不回传的码会被软删, 故 UI 不管理的码必须原样回传, 否则静默丢配.
 */
const loadedPermissions = ref<ModulePermission[]>([])

const showFollowers = ref(false)
const pickerOpen = ref(false)
const pickerSearch = ref('')
const templateDialogOpen = ref(false)

const pluginsStore = usePluginsStore()

const roleDisabled = computed(() => props.currentRole?.pluginEnabled === false)

// ── 模块索引 / 能力查找 ──────────────────────────────────────
const moduleByCode = computed<Map<string, ModuleCapability>>(
  () => new Map(props.modules.map(m => [m.code, m]))
)
function moduleName(code: string): string {
  return moduleByCode.value.get(code)?.name || code
}
function capabilityOf(code: string): ScopeCapabilities {
  const m = moduleByCode.value.get(code)
  return {
    relationFilterable: m?.relationFilterable,
    typeEntity: m?.typeEntity,
    allowedScopes: m?.allowedScopes,
  }
}

const exceptionCodes = computed(() => new Set(exceptions.value.map(e => e.moduleCode)))

/** 跟随默认的资源 (非例外). */
const followerModules = computed<ModuleCapability[]>(() =>
  props.modules.filter(m => !exceptionCodes.value.has(m.code))
)

// ── 例外选择器: 可添加 (非例外) 的资源, 按行业分组 + 搜索 ──────
const addableModules = computed<ModuleCapability[]>(() => {
  const kw = pickerSearch.value.trim().toLowerCase()
  return props.modules.filter(m => {
    if (exceptionCodes.value.has(m.code)) return false
    if (!kw) return true
    return m.name.toLowerCase().includes(kw) || m.code.toLowerCase().includes(kw)
  })
})
const pickerGrouped = computed<Record<string, ModuleCapability[]>>(() => {
  const out: Record<string, ModuleCapability[]> = {}
  for (const m of addableModules.value) {
    const ind = m.industry || 'CORE'
    ;(out[ind] ||= []).push(m)
  }
  return out
})

// ── 标签字典 + 预览 compose ──────────────────────────────────
const { loadDicts, compose } = useScopeLabels()
loadDicts()

/** 一个 spec → 自然语言整句 (含轴①锚点措辞). */
function previewLineFor(spec: ScopeSpecVM): string {
  const mp: ModulePermission = {
    moduleCode: '',
    scopeCode: scopeCodeFromAxis1(spec),
    orgAnchor: spec.orgAnchor,
    anchorParam: spec.anchorParam,
    includeSubtree: spec.includeSubtree,
    subjectRelInclude: spec.subjectRelInclude,
    subjectRelExclude: spec.subjectRelExclude,
    typeFilter: spec.typeFilter,
  }
  return compose(mp, true) || '仅本人'
}
const defaultPreviewLine = computed(() => previewLineFor(defaultSpec.value))

/** 折叠列表里某跟随资源的 (钳制后) 有效范围一行. */
function followerEffectiveLine(code: string): string {
  const allowed = moduleByCode.value.get(code)?.allowedScopes
  const clamped = clampSpecToAllowed(defaultSpec.value, allowed)
  return previewLineFor(clamped)
}

// ── 例外增删改 ──────────────────────────────────────────────
function addException(code: string) {
  if (exceptionCodes.value.has(code)) return
  // 从当前默认范围 seed (用户在此基础上精修)
  exceptions.value.push({ moduleCode: code, spec: { ...defaultSpec.value } })
  pickerOpen.value = false
  pickerSearch.value = ''
}
function removeException(i: number) {
  exceptions.value.splice(i, 1)
}
function updateException(i: number, spec: ScopeSpecVM) {
  exceptions.value[i] = { ...exceptions.value[i], spec }
}

// ── R3/R4 多锚点 (relationGrants >1) 只读展示 + 改为单一 ──────────
const SUBJECT_LABELS: Record<string, string> = {
  SELF: '仅本人',
  MY_ORG: '本组织',
  ALL: '全部',
  RELATION: '关系派生',
  CUSTOM: '指定组织',
  PLUGIN_DIM: '插件维度',
}
const RELATION_LABELS: Record<string, string> = {
  creator: '创建者',
  owner_org: '所属组织',
  reviewer: '复核',
  inspected: '受检',
  member: '成员',
  admin: '管理',
}
function isMultiGrantSpec(spec: ScopeSpecVM): boolean {
  return (spec.relationGrants?.length ?? 0) > 1
}
function grantLabel(g: RelationGrant): string {
  const rel = RELATION_LABELS[g.relation] || g.relation
  const subj = SUBJECT_LABELS[g.subject] || g.subject
  return `${rel} → ${subj}${g.subtree ? '(含下级)' : ''}`
}
/** 清掉多 grant → 退回单一范围 (ScopeBuilder 接管, 以其首 grant 派生的轴① 为起点). */
function simplifyToSingle(i: number) {
  const ex = exceptions.value[i]
  updateException(i, { ...ex.spec, relationGrants: undefined })
}

// ── 模板载入 ────────────────────────────────────────────────
/**
 * 应用模板: 模板的 SceneDecision → 每资源 scope → ModulePermission[] → 推断回「默认 + 例外」.
 *
 * 复用旧 PermissionConfigurator.applyTemplateScene 的 sceneToModuleScopes/specs/relevantCodes 链路,
 * 但终点不再是扁平 modulePermissions, 而是过 inferDefaultAndExceptions 落成本 UI 的默认+例外模型,
 * 与 getConfig 载入路径完全一致. 仅载入不保存 — 用户复核后手动点保存.
 */
function applyTemplate(tpl: RoleTemplate) {
  // props.modules → sceneToModuleScopes 需要的 SimpleModule[] 形状
  const simpleModules = props.modules.map(m => ({
    code: m.code,
    industry: m.industry || 'CORE',
    allowedScopes: m.allowedScopes ?? null,
  }))
  // 本视图不区分 relevant/advanced — 所有托管资源都视作"相关", 模板主决策对其全量生效
  const relevantCodes = new Set(props.modules.map(m => m.code))
  const specs = enabledScopeSpecializations(pluginsStore.codes)

  const { scopes } = sceneToModuleScopes(tpl.scene, simpleModules, specs, relevantCodes)

  // scope 映射 → ModulePermission[]. preset scopeCode → 轴① spec 字段 (orgAnchor/includeSubtree...),
  // 否则 inferDefaultAndExceptions 的 toSpec 会因缺 orgAnchor 一律退化成 SELF.
  const mps: ModulePermission[] = props.modules.map(m => {
    const code = scopes[m.code]?.scopeCode || 'SELF'
    const axis1 = presetCodeToSpec(code)
    return {
      moduleCode: m.code,
      scopeCode: code,
      orgAnchor: axis1.orgAnchor,
      anchorParam: axis1.anchorParam,
      includeSubtree: axis1.includeSubtree,
      customOrgIds: axis1.customOrgIds,
      scopeItems: scopes[m.code]?.scopeItems,
    }
  })

  const inferred = inferDefaultAndExceptions(mps)
  defaultSpec.value = inferred.defaultSpec
  exceptions.value = inferred.exceptions
  ElMessage.info('模板已载入, 点击保存生效')
}

// ── 加载 / 重置 ─────────────────────────────────────────────
async function load() {
  if (!props.currentRole) return
  loading.value = true
  try {
    const config = await dataPermissionApi.getConfig(props.currentRole.id)
    const loaded = config.modulePermissions || []
    loadedPermissions.value = loaded
    const inferred = inferDefaultAndExceptions(loaded)
    defaultSpec.value = inferred.defaultSpec
    exceptions.value = inferred.exceptions
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
      defaultSpec.value = { orgAnchor: 'SELF' }
      exceptions.value = []
      loadedPermissions.value = []
    }
  },
  { immediate: true }
)

async function handleReset() {
  try {
    await ElMessageBox.confirm('重置为已保存的配置? 未保存的修改将丢失.', '重置确认', {
      type: 'warning',
    })
    await load()
  } catch {
    /* cancelled */
  }
}

// ── 保存: 默认 + 例外 → per-resource commands (默认按各资源 allowedScopes 钳制) ──
async function handleSave() {
  if (!props.currentRole) return
  const managedCodes = props.modules.map(m => m.code)

  // 防呆: 无托管资源宇宙时, 后端 delete-all-then-insert 会把整个角色配置清空.
  // 绝不下发 [] — 此时大概率是 props.modules 尚未到位, 直接中止.
  if (managedCodes.length === 0) {
    ElMessage.warning('当前无可配置的资源, 已取消保存以避免误删已有配置')
    return
  }

  const allowedByCode = Object.fromEntries(props.modules.map(m => [m.code, m.allowedScopes]))
  const cmds = expandToCommands(defaultSpec.value, exceptions.value, managedCodes, allowedByCode)

  // 保留未托管模块: 后端 saveRolePermissions 是 delete-all-then-insert, 不回传的码会被软删.
  // 凡 loadedPermissions 里、本 UI 宇宙 (managedCodes) 之外的码, 原样直通保留, 防静默丢配.
  const managedSet = new Set(managedCodes)
  for (const mp of loadedPermissions.value) {
    if (!managedSet.has(mp.moduleCode)) {
      cmds.push({ ...mp })
      managedSet.add(mp.moduleCode) // 去重: 同码只保留一次
    }
  }

  // Guard: 禁用插件的资源不允许配置非 SELF (镜像 PermissionConfigurator)
  const violators = cmds.filter(c => {
    const m = moduleByCode.value.get(c.moduleCode)
    return m?.pluginEnabled === false && c.scopeCode !== 'SELF'
  })
  if (violators.length) {
    ElMessage.warning(
      `${violators.length} 个资源所属插件已禁用, 请先启用插件或将其范围设为"仅本人"`
    )
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

// ── 模拟用户: 给定 userId, 按当前编辑中的范围算该用户实际可见数据 ──────
// 后端 simulate 接受 (userId + modulePermissions 快照), 逐资源 COUNT + 取样;
// 这里用 expandToCommands 把"默认+例外"展开成与保存一致的 per-resource 快照,
// 故无需先保存即可预览未落库的配置.
const simulateUserId = ref<number | null>(null)
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
  if (!simulateUserId.value) return
  const managedCodes = props.modules.map(m => m.code)
  if (managedCodes.length === 0) {
    simulateError.value = '当前无可配置的资源, 无法模拟'
    return
  }
  const allowedByCode = Object.fromEntries(props.modules.map(m => [m.code, m.allowedScopes]))
  const snapshot = expandToCommands(
    defaultSpec.value,
    exceptions.value,
    managedCodes,
    allowedByCode
  )
  simulating.value = true
  simulateError.value = ''
  try {
    const res = await dataPermissionSimulateApi.simulate({
      userId: String(simulateUserId.value),
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
