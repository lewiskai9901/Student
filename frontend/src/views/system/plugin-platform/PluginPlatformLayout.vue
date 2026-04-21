<template>
  <div class="pp-root">
    <!-- Header -->
    <header class="pp-header">
      <div class="pp-brand">
        <h1 class="pp-title">插件平台</h1>
        <span class="pp-subtitle">Explorer · Contributions · Context</span>
      </div>
      <button class="pp-search-trigger" @click="searchOpen = true">
        <Search :size="13" />
        <span>全局搜索</span>
        <kbd>{{ macShortcut ? '⌘' : 'Ctrl' }} K</kbd>
      </button>
      <div class="pp-header-actions">
        <span v-if="metricsInfo" class="pp-metric" :title="'各 Registrar 启动耗时'">
          <Timer :size="12" />
          启动 {{ metricsInfo }}
        </span>
        <button class="pp-btn" :class="{ loading }" @click="loadAll">
          <RefreshCw :size="13" :class="{ spinning: loading }" />
          刷新
        </button>
      </div>
    </header>

    <!-- Messaging alert -->
    <div v-if="!data.messagingHealth.healthy && data.messagingHealth.missingTables.length" class="pp-alert">
      <AlertTriangle :size="14" />
      消息事件管道不完整: <b>{{ data.messagingHealth.missingTables.join(' / ') }}</b>
      — triggerService.fire() 将静默 no-op, 需确认 V97 / V98 / V68 migration 是否 apply 完毕.
    </div>

    <!-- Three-column IDE shell -->
    <div class="pp-shell" :class="{ 'pp-shell-compact': compact }">
      <PluginExplorer
        :view="view"
        :selected-code="selectedCode"
        :selected-hook-key="selectedHookKey"
        :selected-resource="selectedResource"
        @change-view="onChangeView"
        @select-plugin="onSelectPlugin"
        @select-hook="onSelectHook"
        @select-resource="onSelectResource"
      />

      <main class="pp-main">
        <PluginDetail
          v-if="view === 'plugins'"
          :plugin-code="selectedCode"
          @jump-resource="onJumpResource"
        />
        <HookPointDetail
          v-else-if="view === 'hooks'"
          :hook-key="selectedHookKey"
        />
        <ResourceListDetail
          v-else
          :resource-type="selectedResource"
          :plugin-filter="selectedCode"
        />
      </main>

      <PluginContextPanel
        v-if="!compact"
        :view="view"
        :plugin-code="selectedCode"
        :hook-key="selectedHookKey"
        :resource-type="selectedResource"
        @enable="onEnablePlugin"
        @disable="onDisablePlugin"
        @uninstall="onUninstallPlugin"
        @health="onShowHealth"
      />
    </div>

    <!-- Health dialog -->
    <el-dialog v-model="healthDialog.show" :title="`健康检查 - ${healthDialog.code}`" width="500px">
      <div v-if="healthDialog.data" class="pp-health">
        <p><b>状态</b>:
          <span class="pp-chip" :class="healthDialog.data.status === 'HEALTHY' ? 'pp-chip-success' : 'pp-chip-warning'">
            {{ healthDialog.data.status }}
          </span>
        </p>
        <p><b>总贡献</b>: {{ healthDialog.data.totalContributions }}</p>
        <p><b>分项</b>:</p>
        <ul>
          <li v-for="(v, k) in healthDialog.data.contributions" :key="k">{{ k }}: {{ v }}</li>
        </ul>
        <p v-if="healthDialog.data.package">
          <b>版本</b>: v{{ healthDialog.data.package.version }}
        </p>
      </div>
    </el-dialog>

    <!-- Global search palette -->
    <GlobalSearchPalette
      v-model:open="searchOpen"
      @navigate="onSearchNavigate"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, provide, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshCw, AlertTriangle, Timer } from 'lucide-vue-next'
import { http } from '@/utils/request'
import { pluginPlatformApi, type PluginHealthInfo } from '@/api/pluginPlatform'
import PluginExplorer from './components/PluginExplorer.vue'
import PluginDetail from './components/PluginDetail.vue'
import HookPointDetail from './components/HookPointDetail.vue'
import ResourceListDetail from './components/ResourceListDetail.vue'
import PluginContextPanel from './components/PluginContextPanel.vue'
import GlobalSearchPalette from './components/GlobalSearchPalette.vue'
import type { PluginData, ResourceKey } from './helpers'
import { inferIndustry, inferIndustryFromRegisteredBy, resolveIndustry, relationIndustry } from './helpers'

// ───────── Router state ─────────
const route = useRoute()
const router = useRouter()

type ViewKind = 'plugins' | 'hooks' | 'resources'
const view = ref<ViewKind>((route.query.view as ViewKind) || 'plugins')
const selectedCode = ref<string>(String(route.query.code || ''))
const selectedHookKey = ref<string>(
  route.query.entity && route.query.phase ? `${route.query.entity}/${route.query.phase}` : ''
)
const selectedResource = ref<ResourceKey>((route.query.type as ResourceKey) || 'types')

function syncUrl() {
  const q: Record<string, string> = { view: view.value }
  if (view.value === 'plugins' && selectedCode.value) q.code = selectedCode.value
  if (view.value === 'hooks' && selectedHookKey.value) {
    const [entity, phase] = selectedHookKey.value.split('/')
    if (entity) q.entity = entity
    if (phase) q.phase = phase
  }
  if (view.value === 'resources') {
    q.type = selectedResource.value
    if (selectedCode.value) q.pluginCode = selectedCode.value
  }
  router.replace({ query: q }).catch(() => { /* swallow duplicate nav */ })
}
watch([view, selectedCode, selectedHookKey, selectedResource], syncUrl)

function onChangeView(v: ViewKind) {
  view.value = v
  // auto-select first plausible target in this view
  if (v === 'plugins' && !selectedCode.value && data.industries.length) {
    selectedCode.value = data.industries[0].code
  } else if (v === 'hooks' && !selectedHookKey.value && data.hookPoints.length) {
    const h = data.hookPoints[0]
    selectedHookKey.value = `${h.entityType}/${h.phase}`
  }
}
function onSelectPlugin(code: string) {
  view.value = 'plugins'
  selectedCode.value = code
}
function onSelectHook(key: string) {
  view.value = 'hooks'
  selectedHookKey.value = key
}
function onSelectResource(key: ResourceKey) {
  view.value = 'resources'
  selectedResource.value = key
}
function onJumpResource(payload: { type: ResourceKey; pluginCode?: string }) {
  view.value = 'resources'
  selectedResource.value = payload.type
  if (payload.pluginCode !== undefined) selectedCode.value = payload.pluginCode
}

// ───────── Data store (shared via provide) ─────────
const loading = ref(false)
const data = reactive<PluginData>({
  industries: [],
  types: [],
  relations: [],
  events: [],
  permissions: [],
  roles: [],
  policies: [],
  hookPoints: [],
  dataScopes: [],
  triggerPoints: [],
  subscriptionRules: [],
  targetModes: [],
  messagingHealth: { healthy: true, missingTables: [] },
  metrics: null
})

provide('pluginData', data)

async function loadAll() {
  loading.value = true
  try {
    const typeReqs = ['USER', 'ORG_UNIT', 'PLACE'].map(et =>
      http.get('/entity-type-configs', { params: { entityType: et } }).catch(() => [])
    )
    const [ov, r, e, p, ro, pol, ds, tps, srs, tms, mh, mt, ...typeResults] = await Promise.all([
      http.get('/plugin-platform/overview').catch(() => null),
      http.get('/relation-types').catch(() => []),
      http.get('/event/types').catch(() => []),
      http.get('/permissions', { params: { pageSize: 1000 } }).catch(() => []),
      http.get('/roles', { params: { pageSize: 200 } }).catch(() => []),
      http.get('/plugin-platform/policies').catch(() => ({ policies: [], hookPoints: [] })),
      http.get('/roles/data-permissions/scopes').catch(() => []),
      http.get('/plugin-platform/trigger-points').catch(() => []),
      http.get('/plugin-platform/subscription-rules').catch(() => []),
      http.get('/plugin-platform/target-modes').catch(() => []),
      http.get('/plugin-platform/messaging-health').catch(() => ({ healthy: true, missingTables: [] })),
      http.get('/plugin-platform/metrics').catch(() => null),
      ...typeReqs
    ])

    data.types = typeResults.flatMap((t: any) => Array.isArray(t) ? t : (t?.records || t?.list || []))
    data.relations = Array.isArray(r) ? r : ((r as any)?.records || (r as any)?.list || [])
    const eventRaw: any = Array.isArray(e) ? e : ((e as any)?.records || (e as any)?.list || [])
    data.events = eventRaw.flatMap((g: any) => Array.isArray(g?.types) ? g.types : [g])
    data.permissions = Array.isArray(p) ? p : ((p as any)?.records || (p as any)?.list || [])
    data.roles = Array.isArray(ro) ? ro : ((ro as any)?.records || (ro as any)?.list || [])
    data.policies = Array.isArray((pol as any)?.policies) ? (pol as any).policies : []
    data.hookPoints = Array.isArray((pol as any)?.hookPoints) ? (pol as any).hookPoints : []
    data.dataScopes = Array.isArray(ds) ? ds : ((ds as any)?.records || [])
    data.triggerPoints = Array.isArray(tps) ? tps : ((tps as any)?.records || [])
    data.subscriptionRules = Array.isArray(srs) ? srs : ((srs as any)?.records || [])
    data.targetModes = Array.isArray(tms) ? tms : ((tms as any)?.records || [])
    const mhObj: any = mh ?? {}
    data.messagingHealth = {
      healthy: Boolean(mhObj.healthy),
      missingTables: Array.isArray(mhObj.missingTables) ? mhObj.missingTables : []
    }
    data.metrics = mt as any

    // Industries list (prefer overview summary)
    const ovAny: any = ov
    if (ovAny?.industries?.length) {
      data.industries = ovAny.industries.map((ind: any) => ({
        code: ind.code,
        label: ind.name || ind.code,
        version: ind.version,
        enabled: ind.enabled,
        dependsOn: typeof ind.dependsOn === 'string'
          ? JSON.parse(ind.dependsOn || '[]')
          : (ind.dependsOn || []),
        manifestClass: ind.manifestClass,
        uninstallPolicy: ind.uninstallPolicy,
        installedAt: ind.installedAt,
        lastStartedAt: ind.lastStartedAt,
        types: ind.stats?.types || 0,
        relations: ind.stats?.relations || 0,
        events: ind.stats?.events || 0,
        roles: ind.stats?.roles || 0,
        permissions: ind.stats?.permissions || 0,
        triggerPoints: ind.stats?.triggerPoints || 0,
        policies: ind.stats?.policies || 0,
        dataScopes: ind.stats?.dataScopes || 0,
        isPlugin: true
      }))
    } else {
      // Fallback: aggregate from local data
      const m = new Map<string, any>()
      const add = (code: string) => {
        if (!m.has(code)) m.set(code, {
          code, label: code, version: '', enabled: true, dependsOn: [],
          types: 0, relations: 0, events: 0, roles: 0, permissions: 0,
          triggerPoints: 0, policies: 0, dataScopes: 0, isPlugin: true
        })
        return m.get(code)!
      }
      for (const t of data.types) add(inferIndustry(t.pluginClass) || (t.isPluginRegistered ? 'CORE' : 'CUSTOM')).types++
      for (const rel of data.relations) add(relationIndustry(rel)).relations++
      for (const ev of data.events) add(resolveIndustry(ev) || 'CORE').events++
      for (const perm of data.permissions) add(resolveIndustry(perm) || 'CORE').permissions++
      for (const rol of data.roles) add(resolveIndustry(rol) || 'CORE').roles++
      data.industries = Array.from(m.values())
    }

    // Ensure CUSTOM bucket
    const hasCustom = data.industries.some(i => i.code === 'CUSTOM')
    if (!hasCustom) {
      const customTypes = data.types.filter(x => resolveIndustry(x, 'pluginClass') === 'CUSTOM').length
      const customRelations = data.relations.filter(x => relationIndustry(x) === 'CUSTOM').length
      const customEvents = data.events.filter(x => resolveIndustry(x) === 'CUSTOM').length
      const customPerms = data.permissions.filter(x => resolveIndustry(x) === 'CUSTOM').length
      const customRoles = data.roles.filter(x => resolveIndustry(x) === 'CUSTOM').length
      data.industries.push({
        code: 'CUSTOM',
        label: '自定义',
        version: '-',
        enabled: true,
        dependsOn: [],
        manifestClass: null,
        uninstallPolicy: '管理员本地创建, 独立于插件生命周期',
        installedAt: null,
        lastStartedAt: null,
        types: customTypes,
        relations: customRelations,
        events: customEvents,
        roles: customRoles,
        permissions: customPerms,
        triggerPoints: 0,
        policies: 0,
        dataScopes: 0,
        isPlugin: false
      })
    }

    // Sort: CORE first, CUSTOM last
    data.industries.sort((a, b) => {
      if (a.code === 'CORE') return -1
      if (b.code === 'CORE') return 1
      if (a.code === 'CUSTOM') return 1
      if (b.code === 'CUSTOM') return -1
      return a.code.localeCompare(b.code)
    })

    // Auto-select first plugin if none picked
    if (view.value === 'plugins' && !selectedCode.value && data.industries.length) {
      selectedCode.value = data.industries[0].code
    }
    if (view.value === 'hooks' && !selectedHookKey.value && data.hookPoints.length) {
      const h = data.hookPoints[0]
      selectedHookKey.value = `${h.entityType}/${h.phase}`
    }
    syncUrl()
  } catch (err: any) {
    ElMessage.error('加载失败: ' + (err?.message || err))
  } finally {
    loading.value = false
  }
}

// ───────── Plugin governance actions ─────────
const healthDialog = ref<{ show: boolean; code: string; data: PluginHealthInfo | null }>({
  show: false, code: '', data: null
})

async function onEnablePlugin(code: string) {
  try {
    await pluginPlatformApi.enable(code)
    ElMessage.success(`${code} 已启用`)
    await loadAll()
  } catch (e: any) {
    ElMessage.error('启用失败: ' + (e?.message || e))
  }
}
async function onDisablePlugin(code: string) {
  try {
    await ElMessageBox.confirm(`确认禁用插件 ${code}?`, '提示', { type: 'warning' })
    await pluginPlatformApi.disable(code)
    ElMessage.success(`${code} 已禁用`)
    await loadAll()
  } catch (e: any) {
    if (e === 'cancel') return
    ElMessage.error('禁用失败: ' + (e?.message || e))
  }
}
async function onUninstallPlugin(code: string) {
  try {
    await ElMessageBox.confirm(
      `确认卸载插件 ${code}? 这将级联软删此插件贡献的所有资源 (types/roles/perms 等). CUSTOM 资源不受影响.`,
      '危险操作', { type: 'error', confirmButtonText: '确认卸载', cancelButtonText: '取消' })
    const dat: any = await pluginPlatformApi.uninstall(code)
    ElMessage.success(dat?.message || `${code} 已卸载`)
    await loadAll()
  } catch (e: any) {
    if (e === 'cancel') return
    ElMessage.error('卸载失败: ' + (e?.message || e))
  }
}
async function onShowHealth(code: string) {
  try {
    const dat = await pluginPlatformApi.health(code)
    healthDialog.value = { show: true, code, data: dat as PluginHealthInfo }
  } catch (e: any) {
    ElMessage.error('健康检查失败: ' + (e?.message || e))
  }
}

// ───────── Search palette ─────────
const searchOpen = ref(false)
const macShortcut = /Mac|iPad|iPhone/i.test(navigator.platform)
function onKeyDown(ev: KeyboardEvent) {
  if ((ev.metaKey || ev.ctrlKey) && (ev.key === 'k' || ev.key === 'K')) {
    ev.preventDefault()
    searchOpen.value = true
  }
}
onMounted(() => {
  window.addEventListener('keydown', onKeyDown)
  loadAll()
})
onUnmounted(() => window.removeEventListener('keydown', onKeyDown))

function onSearchNavigate(payload: { kind: string; code?: string; key?: string; resource?: ResourceKey }) {
  searchOpen.value = false
  if (payload.kind === 'plugin' && payload.code) {
    view.value = 'plugins'
    selectedCode.value = payload.code
  } else if (payload.kind === 'hook' && payload.key) {
    view.value = 'hooks'
    selectedHookKey.value = payload.key
  } else if (payload.kind === 'resource' && payload.resource) {
    view.value = 'resources'
    selectedResource.value = payload.resource
  }
}

// ───────── Responsive compact mode ─────────
const compact = ref(false)
function onResize() { compact.value = window.innerWidth < 1200 }
onMounted(() => { onResize(); window.addEventListener('resize', onResize) })
onUnmounted(() => window.removeEventListener('resize', onResize))

// ───────── Metrics display ─────────
const metricsInfo = computed(() => {
  const ms = data.metrics?.totalDurationMs
  if (ms == null) return ''
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(2)}s`
})
</script>

<style scoped>
.pp-root {
  display: flex; flex-direction: column; gap: 10px;
  padding: 12px 16px; height: 100%; min-height: 100vh;
  background: #f8fafc;
  font-size: 13px; color: #111827;
}
.pp-header {
  display: flex; align-items: center; gap: 14px;
  background: #fff; border: 1px solid #e5e7eb; border-radius: 8px;
  padding: 8px 14px;
}
.pp-brand { display: flex; flex-direction: column; gap: 1px; }
.pp-title { font-size: 15px; font-weight: 700; color: #111827; margin: 0; }
.pp-subtitle { font-size: 10px; color: #9ca3af; letter-spacing: 0.3px; text-transform: uppercase; }
.pp-search-trigger {
  flex: 1; display: flex; align-items: center; gap: 8px;
  height: 28px; padding: 0 12px; max-width: 360px;
  background: #f3f4f6; border: 1px solid #e5e7eb; border-radius: 6px;
  color: #6b7280; font-size: 12px; cursor: pointer;
  transition: all 0.15s;
}
.pp-search-trigger:hover { border-color: #93c5fd; background: #eff6ff; color: #2563eb; }
.pp-search-trigger kbd {
  margin-left: auto; font-size: 10px;
  padding: 1px 6px; border-radius: 3px;
  border: 1px solid #d1d5db; background: #fff; color: #6b7280;
  font-family: Menlo, monospace;
}
.pp-header-actions { display: flex; align-items: center; gap: 8px; margin-left: auto; }
.pp-metric {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 11px; color: #6b7280;
  padding: 2px 8px; background: #f3f4f6; border-radius: 4px;
}
.pp-btn {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 4px 10px; height: 28px;
  border: 1px solid #d1d5db; background: #fff;
  font-size: 12px; color: #374151; cursor: pointer;
  border-radius: 5px; transition: all 0.15s;
}
.pp-btn:hover { border-color: #2563eb; color: #2563eb; }
.pp-btn.loading { opacity: 0.6; pointer-events: none; }
.spinning { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0) } to { transform: rotate(360deg) } }

.pp-alert {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 14px; border-radius: 6px;
  background: #fef2f2; color: #991b1b; border: 1px solid #fca5a5;
  font-size: 12px;
}
.pp-alert b { font-weight: 700; }

.pp-shell {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr) 280px;
  gap: 10px;
  flex: 1; min-height: 0;
}
.pp-shell-compact {
  grid-template-columns: 220px minmax(0, 1fr);
}
.pp-main {
  background: #fff; border: 1px solid #e5e7eb; border-radius: 8px;
  overflow: auto;
  min-width: 0;
}

.pp-health ul { list-style: circle inside; margin: 4px 0 8px 0; padding: 0; }
.pp-health li { font-size: 12px; padding: 2px 0; }
.pp-chip {
  display: inline-flex; align-items: center;
  font-size: 11px; font-weight: 500;
  padding: 2px 8px; border-radius: 10px; border: 1px solid;
}
.pp-chip-success { color: #059669; border-color: #a7f3d0; background: #ecfdf5; }
.pp-chip-warning { color: #d97706; border-color: #fcd34d; background: #fffbeb; }
</style>
