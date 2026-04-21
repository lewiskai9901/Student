<template>
  <div class="rl-root">
    <header class="rl-head">
      <div class="rl-head-left">
        <component :is="activeIcon" :size="15" />
        <h2 class="rl-title">{{ activeLabel }}</h2>
        <span class="rl-count">{{ filtered.length }}</span>
      </div>
      <div class="rl-head-right">
        <input
          v-model="searchText"
          type="text"
          class="rl-search"
          :placeholder="`搜索 ${activeLabel}...`"
        />
        <span v-if="pluginFilter" class="rl-filter-chip">
          <component :is="activeIcon" :size="11" />
          按插件 {{ pluginFilter }}
          <button class="rl-chip-close" @click="$emit('clear-filter')">✕</button>
        </span>
      </div>
    </header>

    <!-- Types -->
    <div v-if="resourceType === 'types'" class="rl-body">
      <div v-for="grp in groupedTypes" :key="grp.key" class="rl-group">
        <div class="rl-group-head">
          <span>{{ subjectTypeLabel(grp.entityType) }}</span>
          <span class="rl-group-count">{{ grp.items.length }}</span>
        </div>
        <table class="rl-table">
          <thead>
            <tr><th>类型码</th><th>名称</th><th>字段</th><th>特性</th><th>插件</th><th>行业</th></tr>
          </thead>
          <tbody>
            <tr v-for="t in grp.items" :key="t.id">
              <td><code class="rl-mono rl-mono-blue">{{ t.typeCode }}</code></td>
              <td>{{ t.typeName }}</td>
              <td>{{ countFields(t) }}</td>
              <td><span v-for="f in topFeatures(t)" :key="f" class="rl-feat">{{ f }}</span></td>
              <td>
                <code v-if="t.pluginClass" class="rl-mono">{{ shortClass(t.pluginClass) }}</code>
                <span v-else class="rl-muted">自定义</span>
              </td>
              <td>
                <span class="rl-chip" :style="industryChipStyle(resolveIndustry(t, 'pluginClass'))">
                  {{ industryLabel(resolveIndustry(t, 'pluginClass')) || '—' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="!filtered.length" class="rl-empty">无匹配</div>
    </div>

    <!-- Relations -->
    <table v-else-if="resourceType === 'relations'" class="rl-table">
      <thead>
        <tr><th>代码</th><th>名称</th><th>方向</th><th>类别</th><th>关系链</th><th>层级</th><th>来源</th><th>行业</th></tr>
      </thead>
      <tbody>
        <tr v-for="r in filtered" :key="r.relationCode + r.fromType + r.toType">
          <td><code class="rl-mono rl-mono-blue">{{ r.relationCode }}</code></td>
          <td>{{ r.relationName }}</td>
          <td>
            <span class="rl-muted">{{ subjectTypeLabel(r.fromType) }}</span>
            <span class="rl-arrow">→</span>
            <span class="rl-muted">{{ subjectTypeLabel(r.toType) }}</span>
          </td>
          <td>
            <span class="rl-chip" :class="'rl-chip-' + categoryTagType(r.category)">
              {{ categoryLabel(r.category) }}
            </span>
          </td>
          <td>
            <div v-if="parseImplied(r.impliedRelations).length">
              <div v-for="(imp, i) in parseImplied(r.impliedRelations)" :key="i" class="rl-implied">
                → {{ imp.relation }} <span class="rl-muted">on</span> {{ subjectTypeLabel(imp.targetType) }}
              </div>
            </div>
            <span v-else class="rl-muted">—</span>
          </td>
          <td>
            <span class="rl-chip" :class="'rl-chip-' + tierTagType(r.tier)">{{ tierLabel(r.tier) }}</span>
          </td>
          <td><code class="rl-mono">{{ r.registeredBy }}</code></td>
          <td>
            <span class="rl-chip" :style="industryChipStyle(relationIndustry(r))">
              {{ industryLabel(relationIndustry(r)) || '—' }}
            </span>
          </td>
        </tr>
        <tr v-if="!filtered.length"><td colspan="8" class="rl-empty-cell">无匹配</td></tr>
      </tbody>
    </table>

    <!-- Events -->
    <table v-else-if="resourceType === 'events'" class="rl-table">
      <thead>
        <tr><th>代码</th><th>名称</th><th>类别</th><th>极性</th><th>适用主体</th><th>行业</th></tr>
      </thead>
      <tbody>
        <tr v-for="e in filtered" :key="e.typeCode">
          <td><code class="rl-mono rl-mono-blue">{{ e.typeCode }}</code></td>
          <td>
            <span v-if="e.icon" class="rl-evt-dot" :style="{ background: (e.color || '#6b7280') }"></span>
            {{ e.typeName }}
          </td>
          <td><span class="rl-muted">{{ e.categoryName || e.categoryCode }}</span></td>
          <td>
            <span class="rl-chip" :class="'rl-chip-' + polarityTagType(e.categoryPolarity)">
              {{ polarityLabel(e.categoryPolarity) }}
            </span>
          </td>
          <td>
            <span v-for="s in parseSubjects(e.applicableSubjects)" :key="s" class="rl-feat">
              {{ subjectTypeLabel(s) }}
            </span>
          </td>
          <td>
            <span class="rl-chip" :style="industryChipStyle(resolveIndustry(e))">
              {{ industryLabel(resolveIndustry(e)) || '—' }}
            </span>
          </td>
        </tr>
        <tr v-if="!filtered.length"><td colspan="6" class="rl-empty-cell">无匹配</td></tr>
      </tbody>
    </table>

    <!-- Permissions (grouped) -->
    <div v-else-if="resourceType === 'permissions'" class="rl-body">
      <div v-for="grp in groupedPermissions" :key="grp.module" class="rl-group">
        <div class="rl-group-head">
          <span>{{ permissionModuleLabel(grp.module) }}</span>
          <code class="rl-mono">{{ grp.module }}</code>
          <span class="rl-group-count">{{ grp.items.length }}</span>
        </div>
        <table class="rl-table">
          <thead>
            <tr><th>代码</th><th>名称</th><th>类型</th><th>范围</th><th>行业</th></tr>
          </thead>
          <tbody>
            <tr v-for="p in grp.items" :key="p.id">
              <td><code class="rl-mono rl-mono-blue">{{ p.code || p.permissionCode }}</code></td>
              <td>{{ p.name || p.permissionName }}</td>
              <td><span class="rl-muted">{{ permissionTypeLabel(p.type || p.permissionType) }}</span></td>
              <td><span class="rl-muted">{{ permissionScopeLabel(p.scope) }}</span></td>
              <td>
                <span class="rl-chip" :style="industryChipStyle(resolveIndustry(p))">
                  {{ industryLabel(resolveIndustry(p)) || '—' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="!filtered.length" class="rl-empty">无匹配</div>
    </div>

    <!-- Roles -->
    <table v-else-if="resourceType === 'roles'" class="rl-table">
      <thead>
        <tr><th>代码</th><th>名称</th><th>类型</th><th>级别</th><th>权限数</th><th>行业</th></tr>
      </thead>
      <tbody>
        <tr v-for="r in filtered" :key="r.id">
          <td><code class="rl-mono rl-mono-blue">{{ r.roleCode }}</code></td>
          <td>{{ r.roleName }}</td>
          <td><span class="rl-muted">{{ roleTypeLabel(r.roleType) }}</span></td>
          <td><span class="rl-muted">{{ r.level ?? '-' }}</span></td>
          <td>{{ r.permissionCount ?? r.permissionIds?.length ?? 0 }}</td>
          <td>
            <span class="rl-chip" :style="industryChipStyle(resolveIndustry(r))">
              {{ industryLabel(resolveIndustry(r)) || '—' }}
            </span>
          </td>
        </tr>
        <tr v-if="!filtered.length"><td colspan="6" class="rl-empty-cell">无匹配</td></tr>
      </tbody>
    </table>

    <!-- Policies -->
    <table v-else-if="resourceType === 'policies'" class="rl-table">
      <thead>
        <tr><th>代码</th><th>名称</th><th>作用点</th><th>源类</th><th>插件</th></tr>
      </thead>
      <tbody>
        <tr v-for="p in filtered" :key="p.code">
          <td><code class="rl-mono rl-mono-blue">{{ p.code }}</code></td>
          <td>{{ p.name }}</td>
          <td>
            <span v-for="h in (p.supports || [])" :key="h" class="rl-feat">{{ h }}</span>
            <span v-if="!(p.supports || []).length" class="rl-muted">无匹配点</span>
          </td>
          <td><code class="rl-mono">{{ shortClass(p.sourceClass) }}</code></td>
          <td>
            <span class="rl-chip" :style="industryChipStyle(p.sourcePlugin)">
              {{ industryLabel(p.sourcePlugin) || p.sourcePlugin }}
            </span>
          </td>
        </tr>
        <tr v-if="!filtered.length"><td colspan="5" class="rl-empty-cell">无匹配</td></tr>
      </tbody>
    </table>

    <!-- DataScopes -->
    <table v-else-if="resourceType === 'dataScopes'" class="rl-table">
      <thead>
        <tr><th>代码</th><th>名称</th><th>描述</th><th>来源</th></tr>
      </thead>
      <tbody>
        <tr v-for="d in filtered" :key="d.scopeCode || d.code">
          <td><code class="rl-mono rl-mono-blue">{{ d.scopeCode || d.code }}</code></td>
          <td>{{ d.scopeName || d.name }}</td>
          <td><span class="rl-muted">{{ d.description || '—' }}</span></td>
          <td>
            <span class="rl-chip" :style="industryChipStyle(parseDataScopeSource(d.source))">
              {{ d.source || 'CORE' }}
            </span>
          </td>
        </tr>
        <tr v-if="!filtered.length"><td colspan="4" class="rl-empty-cell">无匹配</td></tr>
      </tbody>
    </table>

    <!-- Trigger Points -->
    <table v-else-if="resourceType === 'triggerPoints'" class="rl-table">
      <thead>
        <tr><th>模块</th><th>触发点</th><th>名称</th><th>描述</th><th>Context</th><th>触发器数</th><th>状态</th></tr>
      </thead>
      <tbody>
        <tr v-for="t in filtered" :key="t.point_code">
          <td><span class="rl-chip rl-chip-info">{{ t.module_name || t.module_code }}</span></td>
          <td><code class="rl-mono rl-mono-blue">{{ t.point_code }}</code></td>
          <td>{{ t.point_name }}</td>
          <td><span class="rl-muted">{{ t.description || '—' }}</span></td>
          <td>
            <template v-for="(v, k) in parseSchema(t.context_schema)" :key="k">
              <span class="rl-feat" :title="`${k}: ${v}`">{{ k }}</span>
            </template>
          </td>
          <td><b>{{ t.trigger_count ?? 0 }}</b></td>
          <td>
            <span :class="Number(t.is_enabled) === 1 ? 'rl-chip rl-chip-success' : 'rl-chip rl-chip-info'">
              {{ Number(t.is_enabled) === 1 ? '启用' : '禁用' }}
            </span>
          </td>
        </tr>
        <tr v-if="!filtered.length"><td colspan="7" class="rl-empty-cell">无匹配</td></tr>
      </tbody>
    </table>

    <!-- Subscription Rules -->
    <table v-else-if="resourceType === 'subscriptionRules'" class="rl-table">
      <thead>
        <tr><th>规则名</th><th>事件匹配</th><th>目标</th><th>渠道</th><th>租户</th><th>状态</th></tr>
      </thead>
      <tbody>
        <tr v-for="r in filtered" :key="r.id">
          <td>{{ r.rule_name || '—' }}</td>
          <td><code class="rl-mono">{{ [r.event_category, r.event_type].filter(Boolean).join(' / ') || '全部' }}</code></td>
          <td><span class="rl-chip rl-chip-primary">{{ r.target_mode }}</span></td>
          <td><span class="rl-muted">{{ r.channel || 'IN_APP' }}</span></td>
          <td><span class="rl-muted">T-{{ r.tenant_id ?? 0 }}</span></td>
          <td>
            <span :class="Number(r.is_enabled) === 1 ? 'rl-chip rl-chip-success' : 'rl-chip rl-chip-info'">
              {{ Number(r.is_enabled) === 1 ? '启用' : '禁用' }}
            </span>
          </td>
        </tr>
        <tr v-if="!filtered.length"><td colspan="6" class="rl-empty-cell">无匹配</td></tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import {
  LayoutGrid, Link2, Bell, Shield, UserCog,
  ShieldCheck, Filter, Zap, BellRing
} from 'lucide-vue-next'
import {
  RESOURCE_TYPES, subjectTypeLabel, industryChipStyle, industryLabel,
  resolveIndustry, relationIndustry, countFields, topFeatures, shortClass,
  categoryTagType, categoryLabel, tierTagType, tierLabel, parseImplied,
  polarityTagType, polarityLabel, parseSubjects, permissionTypeLabel,
  permissionScopeLabel, roleTypeLabel, permissionModuleLabel, parseSchema,
  parseDataScopeSource, moduleCodeToIndustry,
  type PluginData, type ResourceKey
} from '../helpers'

const props = defineProps<{
  resourceType: ResourceKey
  pluginFilter: string
}>()

defineEmits<{
  (e: 'clear-filter'): void
}>()

const data = inject<PluginData>('pluginData')!
const searchText = ref('')

const iconMap: Record<ResourceKey, any> = {
  types: LayoutGrid, relations: Link2, events: Bell,
  permissions: Shield, roles: UserCog, policies: ShieldCheck,
  dataScopes: Filter, triggerPoints: Zap, subscriptionRules: BellRing
}
const activeIcon = computed(() => iconMap[props.resourceType])
const activeLabel = computed(() => RESOURCE_TYPES.find(r => r.key === props.resourceType)?.label || '')

function matchesSearch(row: any, fields: string[]): boolean {
  const kw = searchText.value.trim().toLowerCase()
  if (!kw) return true
  return fields.some(f => String((row as any)[f] || '').toLowerCase().includes(kw))
}

function matchesPlugin(row: any, kind: ResourceKey): boolean {
  if (!props.pluginFilter) return true
  switch (kind) {
    case 'types':       return resolveIndustry(row, 'pluginClass') === props.pluginFilter
    case 'relations':   return relationIndustry(row) === props.pluginFilter
    case 'events':      return resolveIndustry(row) === props.pluginFilter
    case 'permissions': return resolveIndustry(row) === props.pluginFilter
    case 'roles':       return resolveIndustry(row) === props.pluginFilter
    case 'policies':    return row.sourcePlugin === props.pluginFilter
    case 'dataScopes':  return parseDataScopeSource(row.source) === props.pluginFilter
    case 'triggerPoints':     return moduleCodeToIndustry(row.module_code) === props.pluginFilter
    case 'subscriptionRules': return props.pluginFilter === 'CORE'
    default: return true
  }
}

const filtered = computed<any[]>(() => {
  switch (props.resourceType) {
    case 'types':
      return data.types
        .filter(t => matchesPlugin(t, 'types'))
        .filter(t => matchesSearch(t, ['typeCode', 'typeName', 'pluginClass', 'entityType']))
    case 'relations':
      return data.relations
        .filter(r => matchesPlugin(r, 'relations'))
        .filter(r => matchesSearch(r, ['relationCode', 'relationName', 'registeredBy']))
    case 'events':
      return data.events
        .filter(e => matchesPlugin(e, 'events'))
        .filter(e => matchesSearch(e, ['typeCode', 'typeName', 'categoryCode']))
    case 'permissions':
      return data.permissions
        .filter(p => matchesPlugin(p, 'permissions'))
        .filter(p => matchesSearch(p, ['code', 'permissionCode', 'name', 'permissionName', 'module']))
    case 'roles':
      return data.roles
        .filter(r => matchesPlugin(r, 'roles'))
        .filter(r => matchesSearch(r, ['roleCode', 'roleName', 'roleType']))
    case 'policies':
      return data.policies
        .filter(p => matchesPlugin(p, 'policies'))
        .filter(p => matchesSearch(p, ['code', 'name', 'sourceClass']))
    case 'dataScopes':
      return data.dataScopes
        .filter(d => matchesPlugin(d, 'dataScopes'))
        .filter(d => matchesSearch(d, ['scopeCode', 'code', 'scopeName', 'name', 'description']))
    case 'triggerPoints':
      return data.triggerPoints
        .filter(t => matchesPlugin(t, 'triggerPoints'))
        .filter(t => matchesSearch(t, ['point_code', 'point_name', 'module_code', 'module_name']))
    case 'subscriptionRules':
      return data.subscriptionRules
        .filter(r => matchesPlugin(r, 'subscriptionRules'))
        .filter(r => matchesSearch(r, ['rule_name', 'event_type', 'event_category', 'target_mode']))
    default: return []
  }
})

const groupedTypes = computed(() => {
  const m = new Map<string, any>()
  for (const t of filtered.value) {
    const key = t.entityType || 'OTHER'
    if (!m.has(key)) m.set(key, { key, entityType: key, items: [] })
    m.get(key).items.push(t)
  }
  return Array.from(m.values())
})

const groupedPermissions = computed(() => {
  const m = new Map<string, any>()
  for (const p of filtered.value) {
    const code = p.code || p.permissionCode || ''
    const moduleName = code.split(':')[0] || 'unknown'
    if (!m.has(moduleName)) m.set(moduleName, { module: moduleName, items: [] })
    m.get(moduleName).items.push(p)
  }
  return Array.from(m.values()).sort((a, b) => a.module.localeCompare(b.module))
})
</script>

<style scoped>
.rl-root {
  display: flex; flex-direction: column; gap: 10px;
  padding: 14px 16px; height: 100%; overflow: auto;
}
.rl-head {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding-bottom: 8px; border-bottom: 1px solid #f3f4f6;
}
.rl-head-left { display: flex; align-items: center; gap: 8px; }
.rl-head-right { display: flex; align-items: center; gap: 8px; }
.rl-title { margin: 0; font-size: 14px; font-weight: 700; color: #111827; }
.rl-count {
  font-size: 10px; font-weight: 600;
  padding: 2px 8px; border-radius: 10px;
  background: #eff6ff; color: #2563eb;
}
.rl-search {
  width: 240px; height: 28px; padding: 0 10px;
  font-size: 12px; border: 1px solid #d1d5db;
  border-radius: 5px; outline: none;
}
.rl-search:focus { border-color: #2563eb; }
.rl-filter-chip {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 2px 8px; background: #eff6ff; color: #2563eb;
  font-size: 11px; border: 1px solid #93c5fd; border-radius: 10px;
}
.rl-chip-close {
  background: transparent; border: none; color: inherit;
  cursor: pointer; font-size: 11px;
}
.rl-body { display: flex; flex-direction: column; gap: 10px; }

.rl-group {
  border: 1px solid #f3f4f6; border-radius: 6px; overflow: hidden; background: #fff;
}
.rl-group-head {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 12px;
  background: #fafbfc; border-bottom: 1px solid #f3f4f6;
  font-size: 11px; font-weight: 600; color: #4b5563;
}
.rl-group-count {
  margin-left: auto; font-size: 10px; color: #6b7280;
  padding: 1px 6px; border-radius: 4px; background: #fff; border: 1px solid #e5e7eb;
}

.rl-table {
  width: 100%; border-collapse: collapse; font-size: 12px;
  background: #fff; border: 1px solid #f3f4f6; border-radius: 6px; overflow: hidden;
}
.rl-group .rl-table { border: none; border-radius: 0; }
.rl-table thead th {
  text-align: left; font-size: 10px; color: #6b7280;
  font-weight: 500; text-transform: uppercase; letter-spacing: 0.3px;
  padding: 7px 12px; border-bottom: 1px solid #e5e7eb;
  background: #fafbfc;
}
.rl-table tbody td {
  padding: 7px 12px; font-size: 12px;
  color: #111827; border-bottom: 1px solid #f9fafb;
  vertical-align: middle;
}
.rl-table tbody tr:hover { background: #fafbfc; }

.rl-mono {
  font-family: 'JetBrains Mono', Menlo, monospace;
  font-size: 10px; background: #f3f4f6; color: #4b5563;
  padding: 1px 5px; border-radius: 3px;
}
.rl-mono-blue { background: #eff6ff; color: #2563eb; font-size: 11px; }
.rl-muted { color: #9ca3af; font-size: 11px; }
.rl-arrow { margin: 0 4px; color: #d1d5db; font-weight: 600; }
.rl-feat {
  display: inline-block; font-size: 10px; color: #6b7280;
  background: #f3f4f6; padding: 1px 6px; border-radius: 3px;
  margin-right: 3px;
}
.rl-implied { font-size: 10px; color: #7c3aed; line-height: 1.3; }
.rl-evt-dot {
  display: inline-block; width: 6px; height: 6px; border-radius: 50%;
  margin-right: 5px;
}
.rl-chip {
  display: inline-flex; align-items: center;
  font-size: 10px; font-weight: 500;
  padding: 1px 7px; border-radius: 10px; border: 1px solid;
}
.rl-chip-primary { color: #2563eb; border-color: #93c5fd; background: #eff6ff; }
.rl-chip-success { color: #059669; border-color: #a7f3d0; background: #ecfdf5; }
.rl-chip-warning { color: #d97706; border-color: #fcd34d; background: #fffbeb; }
.rl-chip-danger  { color: #dc2626; border-color: #fca5a5; background: #fef2f2; }
.rl-chip-info    { color: #6b7280; border-color: #e5e7eb; background: #f9fafb; }
.rl-empty {
  text-align: center; padding: 40px 12px;
  color: #9ca3af; font-size: 12px;
}
.rl-empty-cell {
  text-align: center; padding: 24px 12px;
  color: #9ca3af; font-size: 12px;
}
</style>
