<template>
  <div v-if="!plugin" class="pd-empty">
    <Package :size="32" />
    <p>请从左侧选择一个插件</p>
  </div>

  <div v-else class="pd-root">
    <!-- Header -->
    <header class="pd-head">
      <div class="pd-head-main">
        <span class="pd-dot" :style="{ background: industryColor(plugin.code) }"></span>
        <div>
          <h2 class="pd-title">
            {{ plugin.label }}
            <span class="pd-code">{{ plugin.code }}</span>
          </h2>
          <div class="pd-meta">
            <span v-if="plugin.version">v{{ plugin.version }}</span>
            <span v-if="plugin.manifestClass" class="pd-mono">{{ shortClass(plugin.manifestClass) }}</span>
            <span :class="plugin.enabled === false ? 'pd-chip pd-chip-info' : 'pd-chip pd-chip-success'">
              {{ plugin.enabled === false ? '已禁用' : (plugin.isPlugin === false ? '非插件' : '已启用') }}
            </span>
          </div>
        </div>
      </div>
    </header>

    <!-- Phase 2: 插件禁用警示 banner -->
    <div v-if="plugin.enabled === false" class="pd-alert">
      <AlertTriangle :size="16" class="pd-alert-icon" />
      <div class="pd-alert-body">
        <b>{{ plugin.label }} 已禁用</b>
        <span class="pd-alert-desc">
          — {{ counts.types }} 类型 · {{ counts.relations }} 关系 · {{ counts.events }} 事件 ·
          {{ counts.roles }} 角色 · {{ counts.permissions }} 权限
          已级联软失效<br />
          <span class="pd-alert-note">
            权限计算 / fire 事件 / 前端路由不生效; 管理员视图保留灰显 (点击"重新启用"可恢复)
          </span>
        </span>
      </div>
      <button class="pd-alert-btn" @click="emit('enable-plugin', plugin.code)" title="恢复插件并级联解冻所有贡献">
        重新启用
      </button>
    </div>

    <!-- Contribution summary -->
    <div class="pd-summary">
      <span class="pd-sum-item"><b>{{ counts.types }}</b> 类型</span>
      <span class="pd-sum-sep">·</span>
      <span class="pd-sum-item"><b>{{ counts.relations }}</b> 关系</span>
      <span class="pd-sum-sep">·</span>
      <span class="pd-sum-item"><b>{{ counts.events }}</b> 事件</span>
      <span class="pd-sum-sep">·</span>
      <span class="pd-sum-item"><b>{{ counts.triggerPoints }}</b> 触发点</span>
      <span class="pd-sum-sep">·</span>
      <span class="pd-sum-item"><b>{{ counts.policies }}</b> 策略</span>
      <span class="pd-sum-sep">·</span>
      <span class="pd-sum-item"><b>{{ counts.dataScopes }}</b> 数据维度</span>
      <span class="pd-sum-sep">·</span>
      <span class="pd-sum-item"><b>{{ counts.permissions }}</b> 权限</span>
      <span class="pd-sum-sep">·</span>
      <span class="pd-sum-item"><b>{{ counts.roles }}</b> 角色</span>
    </div>

    <!-- Sections -->
    <div class="pd-sections">
      <!-- Types (default open) -->
      <section class="pd-sec">
        <button class="pd-sec-head" @click="toggle('types')">
          <component :is="openMap.types ? ChevronDown : ChevronRight" :size="13" />
          <LayoutGrid :size="13" class="pd-sec-icon" />
          <span class="pd-sec-title">类型</span>
          <span class="pd-sec-count">{{ counts.types }}</span>
        </button>
        <div v-if="openMap.types" class="pd-sec-body">
          <table v-if="pluginTypes.length" class="pd-table">
            <thead>
              <tr><th>类型码</th><th>名称</th><th>实体</th><th>字段</th><th>特性</th></tr>
            </thead>
            <tbody>
              <tr v-for="t in pluginTypes.slice(0, 5)" :key="t.id">
                <td><code class="pd-mono pd-mono-blue">{{ t.typeCode }}</code></td>
                <td>{{ t.typeName }}</td>
                <td><span class="pd-muted">{{ subjectTypeLabel(t.entityType) }}</span></td>
                <td>{{ countFields(t) }}</td>
                <td>
                  <span v-for="f in topFeatures(t)" :key="f" class="pd-feat">{{ f }}</span>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="pd-empty-inline">无</div>
          <button v-if="pluginTypes.length > 5" class="pd-more"
                  @click="emit('jump-resource', { type: 'types', pluginCode: plugin!.code })">
            查看全部 {{ pluginTypes.length }} →
          </button>
        </div>
      </section>

      <!-- Relations -->
      <section class="pd-sec">
        <button class="pd-sec-head" @click="toggle('relations')">
          <component :is="openMap.relations ? ChevronDown : ChevronRight" :size="13" />
          <Link2 :size="13" class="pd-sec-icon" />
          <span class="pd-sec-title">关系</span>
          <span class="pd-sec-count">{{ counts.relations }}</span>
        </button>
        <div v-if="openMap.relations" class="pd-sec-body">
          <table v-if="pluginRelations.length" class="pd-table">
            <thead>
              <tr><th>关系码</th><th>名称</th><th>方向</th><th>类别</th><th>关系链</th></tr>
            </thead>
            <tbody>
              <tr v-for="r in pluginRelations.slice(0, 5)" :key="r.relationCode + r.fromType + r.toType">
                <td><code class="pd-mono pd-mono-blue">{{ r.relationCode }}</code></td>
                <td>{{ r.relationName }}</td>
                <td>
                  <span class="pd-muted">{{ subjectTypeLabel(r.fromType) }}</span>
                  <span class="pd-arrow">→</span>
                  <span class="pd-muted">{{ subjectTypeLabel(r.toType) }}</span>
                </td>
                <td>
                  <span class="pd-chip" :class="'pd-chip-' + categoryTagType(r.category)">
                    {{ categoryLabel(r.category) }}
                  </span>
                </td>
                <td>
                  <div v-if="parseImplied(r.impliedRelations).length">
                    <div v-for="(imp, i) in parseImplied(r.impliedRelations)" :key="i" class="pd-implied">
                      → {{ imp.relation }}
                    </div>
                  </div>
                  <span v-else class="pd-muted">—</span>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="pd-empty-inline">无</div>
          <button v-if="pluginRelations.length > 5" class="pd-more"
                  @click="emit('jump-resource', { type: 'relations', pluginCode: plugin!.code })">
            查看全部 {{ pluginRelations.length }} →
          </button>
        </div>
      </section>

      <!-- Trigger points (always full, with listener reverse lookup) -->
      <section class="pd-sec">
        <button class="pd-sec-head" @click="toggle('triggerPoints')">
          <component :is="openMap.triggerPoints ? ChevronDown : ChevronRight" :size="13" />
          <Zap :size="13" class="pd-sec-icon" />
          <span class="pd-sec-title">触发点</span>
          <span class="pd-sec-count">{{ counts.triggerPoints }}</span>
        </button>
        <div v-if="openMap.triggerPoints" class="pd-sec-body">
          <table v-if="pluginTriggerPoints.length" class="pd-table">
            <thead>
              <tr><th>模块</th><th>触发点</th><th>名称</th><th>Context</th><th>订阅者</th></tr>
            </thead>
            <tbody>
              <tr v-for="t in pluginTriggerPoints" :key="t.point_code">
                <td><span class="pd-chip pd-chip-info">{{ t.module_name || t.module_code }}</span></td>
                <td><code class="pd-mono pd-mono-blue">{{ t.point_code }}</code></td>
                <td>{{ t.point_name }}</td>
                <td>
                  <template v-for="(v, k) in parseSchema(t.context_schema)" :key="k">
                    <span class="pd-feat" :title="`${k}: ${v}`">{{ k }}</span>
                  </template>
                </td>
                <td>{{ t.trigger_count ?? 0 }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="pd-empty-inline">无</div>
        </div>
      </section>

      <!-- Events -->
      <section class="pd-sec pd-sec-collapsed">
        <button class="pd-sec-head" @click="toggle('events')">
          <component :is="openMap.events ? ChevronDown : ChevronRight" :size="13" />
          <Bell :size="13" class="pd-sec-icon" />
          <span class="pd-sec-title">事件类型</span>
          <span class="pd-sec-count">{{ counts.events }}</span>
        </button>
        <div v-if="openMap.events" class="pd-sec-body">
          <table v-if="pluginEvents.length" class="pd-table">
            <thead>
              <tr><th>代码</th><th>名称</th><th>类别</th><th>极性</th></tr>
            </thead>
            <tbody>
              <tr v-for="e in pluginEvents.slice(0, 5)" :key="e.typeCode">
                <td><code class="pd-mono pd-mono-blue">{{ e.typeCode }}</code></td>
                <td>{{ e.typeName }}</td>
                <td><span class="pd-muted">{{ e.categoryName || e.categoryCode }}</span></td>
                <td>
                  <span class="pd-chip" :class="'pd-chip-' + polarityTagType(e.categoryPolarity)">
                    {{ polarityLabel(e.categoryPolarity) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="pd-empty-inline">无</div>
          <button v-if="pluginEvents.length > 5" class="pd-more"
                  @click="emit('jump-resource', { type: 'events', pluginCode: plugin!.code })">
            查看全部 {{ pluginEvents.length }} →
          </button>
        </div>
      </section>

      <!-- Policies -->
      <section class="pd-sec pd-sec-collapsed">
        <button class="pd-sec-head" @click="toggle('policies')">
          <component :is="openMap.policies ? ChevronDown : ChevronRight" :size="13" />
          <ShieldCheck :size="13" class="pd-sec-icon" />
          <span class="pd-sec-title">策略</span>
          <span class="pd-sec-count">{{ counts.policies }}</span>
        </button>
        <div v-if="openMap.policies" class="pd-sec-body">
          <table v-if="pluginPolicies.length" class="pd-table">
            <thead>
              <tr><th>代码</th><th>名称</th><th>作用点</th><th>源类</th></tr>
            </thead>
            <tbody>
              <tr v-for="p in pluginPolicies" :key="p.code">
                <td><code class="pd-mono pd-mono-blue">{{ p.code }}</code></td>
                <td>{{ p.name }}</td>
                <td>
                  <span v-for="h in (p.supports || [])" :key="h" class="pd-feat">{{ h }}</span>
                  <span v-if="!(p.supports || []).length" class="pd-muted">—</span>
                </td>
                <td><code class="pd-mono">{{ shortClass(p.sourceClass) }}</code></td>
              </tr>
            </tbody>
          </table>
          <div v-else class="pd-empty-inline">无</div>
        </div>
      </section>

      <!-- Data Scopes -->
      <section class="pd-sec pd-sec-collapsed">
        <button class="pd-sec-head" @click="toggle('dataScopes')">
          <component :is="openMap.dataScopes ? ChevronDown : ChevronRight" :size="13" />
          <Filter :size="13" class="pd-sec-icon" />
          <span class="pd-sec-title">数据维度</span>
          <span class="pd-sec-count">{{ counts.dataScopes }}</span>
        </button>
        <div v-if="openMap.dataScopes" class="pd-sec-body">
          <table v-if="pluginDataScopes.length" class="pd-table">
            <thead><tr><th>代码</th><th>名称</th><th>描述</th></tr></thead>
            <tbody>
              <tr v-for="d in pluginDataScopes" :key="d.scopeCode || d.code">
                <td><code class="pd-mono pd-mono-blue">{{ d.scopeCode || d.code }}</code></td>
                <td>{{ d.scopeName || d.name }}</td>
                <td><span class="pd-muted">{{ d.description || '—' }}</span></td>
              </tr>
            </tbody>
          </table>
          <div v-else class="pd-empty-inline">无</div>
        </div>
      </section>

      <!-- Permissions (grouped by module) -->
      <section class="pd-sec pd-sec-collapsed">
        <button class="pd-sec-head" @click="toggle('permissions')">
          <component :is="openMap.permissions ? ChevronDown : ChevronRight" :size="13" />
          <Shield :size="13" class="pd-sec-icon" />
          <span class="pd-sec-title">权限</span>
          <span class="pd-sec-count">{{ counts.permissions }}</span>
        </button>
        <div v-if="openMap.permissions" class="pd-sec-body">
          <div v-for="grp in groupedPermissions" :key="grp.module" class="pd-group">
            <div class="pd-group-head">
              <span>{{ permissionModuleLabel(grp.module) }}</span>
              <code class="pd-mono">{{ grp.module }}</code>
              <span class="pd-group-count">{{ grp.items.length }}</span>
            </div>
            <table class="pd-table">
              <tbody>
                <tr v-for="p in grp.items.slice(0, 5)" :key="p.id">
                  <td><code class="pd-mono pd-mono-blue">{{ p.code || p.permissionCode }}</code></td>
                  <td>{{ p.name || p.permissionName }}</td>
                  <td><span class="pd-muted">{{ permissionTypeLabel(p.type || p.permissionType) }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
          <button v-if="counts.permissions > 10" class="pd-more"
                  @click="emit('jump-resource', { type: 'permissions', pluginCode: plugin!.code })">
            查看全部 {{ counts.permissions }} →
          </button>
        </div>
      </section>

      <!-- Roles -->
      <section class="pd-sec pd-sec-collapsed">
        <button class="pd-sec-head" @click="toggle('roles')">
          <component :is="openMap.roles ? ChevronDown : ChevronRight" :size="13" />
          <UserCog :size="13" class="pd-sec-icon" />
          <span class="pd-sec-title">角色</span>
          <span class="pd-sec-count">{{ counts.roles }}</span>
        </button>
        <div v-if="openMap.roles" class="pd-sec-body">
          <table v-if="pluginRoles.length" class="pd-table">
            <thead><tr><th>代码</th><th>名称</th><th>类型</th><th>级别</th><th>权限数</th></tr></thead>
            <tbody>
              <tr v-for="r in pluginRoles.slice(0, 5)" :key="r.id">
                <td><code class="pd-mono pd-mono-blue">{{ r.roleCode }}</code></td>
                <td>{{ r.roleName }}</td>
                <td><span class="pd-muted">{{ roleTypeLabel(r.roleType) }}</span></td>
                <td><span class="pd-muted">{{ r.level ?? '-' }}</span></td>
                <td>{{ r.permissionCount ?? r.permissionIds?.length ?? 0 }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="pd-empty-inline">无</div>
          <button v-if="pluginRoles.length > 5" class="pd-more"
                  @click="emit('jump-resource', { type: 'roles', pluginCode: plugin!.code })">
            查看全部 {{ pluginRoles.length }} →
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, reactive } from 'vue'
import {
  Package, ChevronDown, ChevronRight, LayoutGrid, Link2, Bell, Zap,
  ShieldCheck, Filter, Shield, UserCog, AlertTriangle
} from 'lucide-vue-next'
import {
  industryColor, shortClass, subjectTypeLabel, countFields, topFeatures,
  parseImplied, parseSchema, categoryTagType, categoryLabel, polarityTagType,
  polarityLabel, permissionTypeLabel, permissionModuleLabel, roleTypeLabel,
  resolveIndustry, relationIndustry, moduleCodeToIndustry, parseDataScopeSource,
  type PluginData, type ResourceKey
} from '../helpers'

const props = defineProps<{ pluginCode: string }>()
const emit = defineEmits<{
  (e: 'jump-resource', payload: { type: ResourceKey; pluginCode?: string }): void
  (e: 'enable-plugin', pluginCode: string): void
}>()

const data = inject<PluginData>('pluginData')!

const plugin = computed(() => data.industries.find(i => i.code === props.pluginCode) || null)

// ── Filter resource lists by current plugin ──
const pluginTypes = computed(() =>
  data.types.filter(t => resolveIndustry(t, 'pluginClass') === props.pluginCode)
)
const pluginRelations = computed(() =>
  data.relations.filter(r => relationIndustry(r) === props.pluginCode)
)
const pluginEvents = computed(() =>
  data.events.filter(e => resolveIndustry(e) === props.pluginCode)
)
const pluginPermissions = computed(() =>
  data.permissions.filter((p: any) => resolveIndustry(p) === props.pluginCode)
)
const pluginRoles = computed(() =>
  data.roles.filter((r: any) => resolveIndustry(r) === props.pluginCode)
)
const pluginPolicies = computed(() =>
  data.policies.filter(p => p.sourcePlugin === props.pluginCode)
)
const pluginDataScopes = computed(() =>
  data.dataScopes.filter(d => parseDataScopeSource(d.source) === props.pluginCode)
)
const pluginTriggerPoints = computed(() =>
  data.triggerPoints.filter(t => moduleCodeToIndustry(t.module_code) === props.pluginCode)
)

const counts = computed(() => ({
  types: pluginTypes.value.length,
  relations: pluginRelations.value.length,
  events: pluginEvents.value.length,
  permissions: pluginPermissions.value.length,
  roles: pluginRoles.value.length,
  policies: pluginPolicies.value.length,
  dataScopes: pluginDataScopes.value.length,
  triggerPoints: pluginTriggerPoints.value.length
}))

const groupedPermissions = computed(() => {
  const m = new Map<string, any>()
  for (const p of pluginPermissions.value) {
    const code = p.code || p.permissionCode || ''
    const moduleName = code.split(':')[0] || 'unknown'
    if (!m.has(moduleName)) m.set(moduleName, { module: moduleName, items: [] })
    m.get(moduleName).items.push(p)
  }
  return Array.from(m.values())
    .sort((a, b) => a.module.localeCompare(b.module))
    .slice(0, 5)
})

// Section open/close map (first three default open)
const openMap = reactive<Record<string, boolean>>({
  types: true,
  relations: true,
  triggerPoints: true,
  events: false,
  policies: false,
  dataScopes: false,
  permissions: false,
  roles: false
})
function toggle(k: string) { openMap[k] = !openMap[k] }
</script>

<style scoped>
.pd-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 80px 20px; color: #9ca3af; gap: 10px; font-size: 12px;
}
.pd-root {
  display: flex; flex-direction: column; gap: 10px;
  padding: 14px 16px; height: 100%; overflow: auto;
}
.pd-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.pd-head-main { display: flex; align-items: center; gap: 10px; }
.pd-dot { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; }
.pd-title {
  font-size: 15px; font-weight: 700; color: #111827; margin: 0;
  display: flex; align-items: center; gap: 8px;
}
.pd-code {
  font-size: 10px; font-weight: 600;
  padding: 2px 6px; background: #f3f4f6;
  color: #4b5563; border-radius: 3px;
  font-family: Menlo, monospace;
}
.pd-meta { display: flex; align-items: center; gap: 10px; margin-top: 3px; font-size: 11px; color: #6b7280; }
.pd-mono {
  font-family: 'JetBrains Mono', Menlo, monospace;
  font-size: 10px;
  background: #f3f4f6; color: #4b5563;
  padding: 1px 5px; border-radius: 3px;
}
.pd-mono-blue { background: #eff6ff; color: #2563eb; font-size: 11px; }

.pd-summary {
  display: flex; align-items: center; flex-wrap: wrap; gap: 6px;
  padding: 8px 12px; background: #fafbfc;
  border: 1px solid #f3f4f6; border-radius: 6px;
  font-size: 12px; color: #4b5563;
}
.pd-sum-item b { color: #111827; font-weight: 700; margin-right: 2px; }
.pd-sum-sep { color: #d1d5db; }

.pd-sections { display: flex; flex-direction: column; gap: 6px; }
.pd-sec {
  border: 1px solid #e5e7eb; border-radius: 6px; overflow: hidden; background: #fff;
}
.pd-sec-head {
  display: flex; align-items: center; gap: 6px; width: 100%;
  padding: 8px 12px; background: #fafbfc; border: none;
  cursor: pointer; text-align: left;
  font-size: 12px; color: #111827; font-weight: 600;
  border-bottom: 1px solid transparent;
  transition: background 0.1s;
}
.pd-sec-head:hover { background: #f3f4f6; }
.pd-sec-icon { color: #6b7280; }
.pd-sec-title { flex: 1; }
.pd-sec-count {
  font-size: 10px; font-weight: 600;
  padding: 1px 7px; border-radius: 10px;
  background: #e5e7eb; color: #4b5563;
}
.pd-sec-body { padding: 4px 0; }

.pd-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.pd-table thead th {
  text-align: left; font-size: 10px; color: #6b7280;
  font-weight: 500; text-transform: uppercase; letter-spacing: 0.3px;
  padding: 6px 12px;
  border-bottom: 1px solid #f3f4f6;
  background: transparent;
}
.pd-table tbody td {
  padding: 6px 12px; font-size: 12px;
  color: #111827; border-bottom: 1px solid #f9fafb;
  vertical-align: middle;
}
.pd-muted { color: #9ca3af; font-size: 11px; }
.pd-arrow { margin: 0 4px; color: #d1d5db; font-weight: 600; }
.pd-feat {
  display: inline-block; font-size: 10px; color: #6b7280;
  background: #f3f4f6; padding: 1px 6px; border-radius: 3px;
  margin-right: 3px;
}
.pd-implied { font-size: 10px; color: #7c3aed; line-height: 1.3; }
.pd-chip {
  display: inline-flex; align-items: center;
  font-size: 10px; font-weight: 500;
  padding: 1px 7px; border-radius: 10px;
  border: 1px solid; line-height: 1.4;
}
.pd-chip-primary { color: #2563eb; border-color: #93c5fd; background: #eff6ff; }
.pd-chip-success { color: #059669; border-color: #a7f3d0; background: #ecfdf5; }
.pd-chip-warning { color: #d97706; border-color: #fcd34d; background: #fffbeb; }
.pd-chip-danger  { color: #dc2626; border-color: #fca5a5; background: #fef2f2; }
.pd-chip-info    { color: #6b7280; border-color: #e5e7eb; background: #f9fafb; }

.pd-empty-inline {
  padding: 14px 12px; color: #9ca3af; font-size: 11px; text-align: center;
}
.pd-more {
  display: block; margin: 6px 12px;
  padding: 4px 10px; border: 1px dashed #d1d5db;
  background: transparent; color: #2563eb; font-size: 11px;
  cursor: pointer; border-radius: 4px;
}
.pd-more:hover { border-color: #2563eb; background: #eff6ff; }

.pd-group {
  border-top: 1px solid #f3f4f6; padding: 4px 0 6px;
}
.pd-group:first-child { border-top: none; }
.pd-group-head {
  display: flex; align-items: center; gap: 6px;
  padding: 4px 12px;
  font-size: 11px; color: #4b5563; font-weight: 600;
}
.pd-group-count {
  margin-left: auto; font-size: 10px; color: #6b7280;
  background: #f3f4f6; padding: 1px 6px; border-radius: 4px;
}

/* Phase 2: 插件禁用警示 banner */
.pd-alert {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 10px 14px;
  border-radius: 8px;
  background: #fef3c7;
  border: 1px solid #fcd34d;
  color: #92400e;
  font-size: 12px;
  line-height: 1.5;
}
.pd-alert-icon { flex-shrink: 0; margin-top: 2px; color: #d97706; }
.pd-alert-body { flex: 1; }
.pd-alert-body b { color: #78350f; font-weight: 700; }
.pd-alert-desc { color: #92400e; }
.pd-alert-note {
  display: inline-block; margin-top: 4px;
  font-size: 11px; color: #78350f; opacity: 0.85;
}
.pd-alert-btn {
  margin-left: auto; flex-shrink: 0;
  padding: 5px 12px;
  background: #d97706; color: #fff;
  border: none; border-radius: 4px;
  font-size: 11px; font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.pd-alert-btn:hover { background: #b45309; }
</style>
