<template>
  <div class="flex h-full flex-col">
    <!-- 头部 -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-5 py-3">
      <div class="flex min-w-0 items-center gap-3">
        <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-blue-500 to-blue-600">
          <Shield class="h-5 w-5 text-white" />
        </div>
        <div class="min-w-0">
          <h2 class="truncate text-base font-semibold text-gray-900">
            {{ currentRole?.roleName || '未选择角色' }}
          </h2>
          <p class="truncate text-[11px] text-gray-500">
            {{ currentRole?.roleCode || '请在左侧选择要配置的角色' }}
          </p>
        </div>
      </div>
      <div v-if="currentRole" class="flex items-center gap-2">
        <button
          class="flex h-9 items-center gap-1.5 rounded-md border border-gray-200 bg-white px-3 text-xs font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
          :disabled="isRoleDisabled"
          :title="isRoleDisabled ? '所属插件已禁用, 请先启用' : ''"
          @click="$emit('open-templates')"
        >
          <Library class="h-3.5 w-3.5" />
          模板库
        </button>
        <button
          class="flex h-9 items-center gap-1.5 rounded-md border border-gray-200 bg-white px-3 text-xs font-medium text-gray-600 hover:bg-red-50 hover:text-red-600 hover:border-red-200 disabled:opacity-40 disabled:cursor-not-allowed"
          :disabled="isRoleDisabled"
          :title="isRoleDisabled ? '所属插件已禁用, 请先启用' : ''"
          @click="handleReset"
        >
          <Trash2 class="h-3.5 w-3.5" />
          重置
        </button>
        <button
          class="flex h-9 items-center gap-1.5 rounded-md bg-blue-600 px-4 text-xs font-medium text-white hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="saving || isRoleDisabled"
          :title="isRoleDisabled ? '所属插件已禁用, 请先启用' : ''"
          @click="handleSave"
        >
          <Loader2 v-if="saving" class="h-3.5 w-3.5 animate-spin" />
          <Save v-else class="h-3.5 w-3.5" />
          保存
        </button>
      </div>
    </div>

    <!-- 插件禁用警示 banner -->
    <div
      v-if="currentRole && isRoleDisabled"
      class="flex items-start gap-2 border-b border-amber-200 bg-amber-50 px-5 py-3"
    >
      <AlertTriangle class="mt-0.5 h-4 w-4 flex-shrink-0 text-amber-600" />
      <div class="flex-1 text-xs text-amber-900">
        <b>"{{ currentRole.roleName }}"</b> 所属插件 <b>{{ currentRole.industry }}</b> 已禁用.
        可以查看配置, 但<b>无法编辑或保存</b>. 请先在插件平台启用 {{ currentRole.industry }} 插件.
      </div>
      <button
        class="flex-shrink-0 rounded-md border border-amber-300 bg-white px-2.5 py-1 text-[11px] font-medium text-amber-700 hover:bg-amber-100"
        @click="$emit('enable-industry', currentRole.industry)"
      >
        一键启用
      </button>
    </div>

    <div v-if="!currentRole" class="flex flex-1 items-center justify-center">
      <div class="text-center">
        <Shield class="mx-auto h-12 w-12 text-gray-300" />
        <p class="mt-3 text-sm text-gray-500">请在左侧选择一个角色</p>
      </div>
    </div>

    <div v-else-if="loading" class="flex flex-1 items-center justify-center">
      <Loader2 class="h-8 w-8 animate-spin text-blue-600" />
    </div>

    <div v-else class="flex-1 overflow-y-auto px-5 py-4">
      <!-- 模式切换 tab -->
      <div class="mb-4 inline-flex rounded-lg border border-gray-200 bg-gray-50 p-1">
        <button
          class="rounded-md px-4 py-1.5 text-xs font-medium transition"
          :class="mode === 'scene' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'"
          @click="mode = 'scene'"
        >
          <Sparkles class="mr-1 inline h-3 w-3" />
          场景模板 <span class="text-[10px] text-gray-400">(推荐)</span>
        </button>
        <button
          class="rounded-md px-4 py-1.5 text-xs font-medium transition"
          :class="mode === 'advanced' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'"
          @click="mode = 'advanced'"
        >
          <Settings2 class="mr-1 inline h-3 w-3" />
          高级 · {{ totalModules }} 模块
        </button>
      </div>

      <!-- 模式内容 -->
      <div v-if="mode === 'scene'">
        <SceneTemplatePanel
          :decision="decision"
          :edu-enabled="eduEnabled"
          @update:decision="onDecisionUpdate"
        />
      </div>

      <div v-else>
        <AdvancedModuleEditor
          :grouped-modules="groupedModules"
          :advanced-grouped-modules="advancedGroupedModules"
          :filter-meta="filterMeta"
          :module-permissions="modulePermissions"
          :data-scope-options="dataScopeOptions"
          @update:module-permissions="onModulePermissionsUpdate"
          @batch-set="onBatchSet"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Shield,
  Library,
  Loader2,
  Save,
  Trash2,
  Sparkles,
  Settings2,
  AlertTriangle,
} from 'lucide-vue-next'
import SceneTemplatePanel from './SceneTemplatePanel.vue'
import AdvancedModuleEditor, { type ModuleGroupItem } from './AdvancedModuleEditor.vue'
import { dataPermissionApi, type RoleResponse } from '@/api/access'
import type { ModulePermission, DataScopeOption } from '@/types/access'
import {
  sceneToModuleScopes,
  moduleScopesToScene,
  applySceneToModules,
  type SceneDecision,
} from '../composables/useSceneTemplate'

interface FilterMeta {
  filtered: boolean
  filterRule?: string
  totalRelevant?: number
  totalAdvanced?: number
  roleIndustry?: string
  rolePermModules?: string[]
}

interface Props {
  currentRole: RoleResponse | null
  groupedModules: Record<string, ModuleGroupItem[]>
  advancedGroupedModules?: Record<string, ModuleGroupItem[]>
  filterMeta?: FilterMeta
  dataScopeOptions: DataScopeOption[]
  moduleNameMap: Record<string, string>
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'open-templates': []
  'config-loaded': [modulePermissions: ModulePermission[], decision: SceneDecision]
  'config-changed': [modulePermissions: ModulePermission[], decision: SceneDecision]
  'saved': []
  'enable-industry': [industry: string]
}>()

const isRoleDisabled = computed(() => props.currentRole?.pluginEnabled === false)

const loading = ref(false)
const saving = ref(false)
const mode = ref<'scene' | 'advanced'>('scene')

const decision = ref<SceneDecision>({ primary: 'SELF', bizAutoFollow: true })
const modulePermissions = ref<ModulePermission[]>([])

const totalModules = computed(() => {
  const rel = Object.values(props.groupedModules).reduce((sum, list) => sum + list.length, 0)
  const adv = Object.values(props.advancedGroupedModules ?? {}).reduce((sum, list) => sum + list.length, 0)
  return rel + adv
})

const eduEnabled = computed(() => {
  const list = props.groupedModules['EDU']
  return !!list && list.some(m => m.pluginEnabled !== false)
})

const flatModules = computed(() => {
  const out: { code: string; industry: string }[] = []
  for (const [ind, list] of Object.entries(props.groupedModules)) {
    for (const m of list) {
      out.push({ code: m.code, industry: ind })
    }
  }
  // 也纳入 advanced 列表, 确保 applyScene 时 advanced 模块被显式写入 SELF
  for (const [ind, list] of Object.entries(props.advancedGroupedModules ?? {})) {
    for (const m of list) {
      out.push({ code: m.code, industry: ind })
    }
  }
  return out
})

/** "相关" 模块的 code 集合 — 传给 applySceneToModules 用作 relevantCodes 参数 */
const relevantCodes = computed<Set<string>>(() => {
  const s = new Set<string>()
  for (const list of Object.values(props.groupedModules)) {
    for (const m of list) s.add(m.code)
  }
  return s
})

watch(
  () => props.currentRole?.id,
  async (id) => {
    if (id != null) {
      await loadConfig()
    } else {
      modulePermissions.value = []
      decision.value = { primary: 'SELF', bizAutoFollow: true }
    }
  },
  { immediate: true }
)

async function loadConfig() {
  if (!props.currentRole) return
  loading.value = true
  try {
    const config = await dataPermissionApi.getConfig(props.currentRole.id)
    modulePermissions.value = config.modulePermissions || []
    decision.value = moduleScopesToScene(modulePermissions.value)
    emit('config-loaded', modulePermissions.value, decision.value)
  } catch (e: any) {
    ElMessage.error('加载数据权限失败: ' + (e?.message || e))
  } finally {
    loading.value = false
  }
}

function onDecisionUpdate(next: SceneDecision) {
  decision.value = next
  // 场景模式下同步应用到 modulePermissions (实时映射)
  if (mode.value === 'scene') {
    modulePermissions.value = applySceneToModules(next, flatModules.value, modulePermissions.value, relevantCodes.value)
  }
  emit('config-changed', modulePermissions.value, decision.value)
}

function onModulePermissionsUpdate(next: ModulePermission[]) {
  modulePermissions.value = next
  // 高级模式修改时反推 scene (让预览区同步)
  decision.value = moduleScopesToScene(next)
  emit('config-changed', modulePermissions.value, decision.value)
}

function onBatchSet(scopeCode: string) {
  const next: ModulePermission[] = modulePermissions.value.map(mp => ({
    ...mp,
    scopeCode,
    scopeItems: scopeCode !== 'CUSTOM' ? [] : mp.scopeItems,
  }))
  // 如果原本无条目 (新角色), 用 flatModules 补齐
  if (next.length === 0) {
    for (const m of flatModules.value) {
      next.push({ moduleCode: m.code, scopeCode, scopeItems: [] })
    }
  }
  modulePermissions.value = next
  decision.value = moduleScopesToScene(next)
  emit('config-changed', modulePermissions.value, decision.value)
  ElMessage.success(`已批量设为 ${scopeName(scopeCode)}`)
}

function scopeName(code: string) {
  return props.dataScopeOptions.find(s => s.scopeCode === code)?.scopeName || code
}

async function handleSave() {
  if (!props.currentRole) return
  // Guard: 禁用插件的模块不允许配置非 SELF
  const violators = modulePermissions.value.filter(mp => {
    for (const list of Object.values(props.groupedModules)) {
      const mod = list.find(x => x.code === mp.moduleCode)
      if (mod && mod.pluginEnabled === false && mp.scopeCode !== 'SELF') return true
    }
    return false
  })
  if (violators.length) {
    ElMessage.warning(
      `${violators.length} 个模块所属插件已禁用, 请先启用插件或将其 scope 设为"仅本人"`
    )
    return
  }

  saving.value = true
  try {
    await dataPermissionApi.saveConfig(props.currentRole.id, {
      roleId: props.currentRole.id,
      roleName: props.currentRole.roleName,
      modulePermissions: modulePermissions.value,
    })
    ElMessage.success('数据权限保存成功')
    emit('saved')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleReset() {
  try {
    await ElMessageBox.confirm('确定重置所有配置为 SELF? 此操作需保存后生效.', '重置确认', {
      type: 'warning',
    })
    modulePermissions.value = modulePermissions.value.map(mp => ({
      ...mp,
      scopeCode: 'SELF',
      scopeItems: [],
    }))
    decision.value = { primary: 'SELF', bizAutoFollow: true }
    emit('config-changed', modulePermissions.value, decision.value)
  } catch (e) {
    /* cancelled */
  }
}

/** 由父组件调用: 从模板应用 scene */
function applyTemplateScene(scene: SceneDecision) {
  decision.value = { ...scene }
  modulePermissions.value = applySceneToModules(decision.value, flatModules.value, modulePermissions.value)
  mode.value = 'scene'
  emit('config-changed', modulePermissions.value, decision.value)
  ElMessage.success('模板已应用, 点击保存生效')
}

defineExpose({ applyTemplateScene, reload: loadConfig })
</script>
