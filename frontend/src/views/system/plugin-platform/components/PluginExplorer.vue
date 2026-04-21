<template>
  <aside class="ex-root">
    <!-- Tab switcher -->
    <div class="ex-tabs">
      <button
        v-for="t in tabs"
        :key="t.key"
        class="ex-tab"
        :class="{ active: view === t.key }"
        @click="emit('change-view', t.key)"
      >
        <component :is="t.icon" :size="13" />
        {{ t.label }}
      </button>
    </div>

    <!-- Plugins view -->
    <div v-if="view === 'plugins'" class="ex-body">
      <div class="ex-section-title">行业包 <span class="ex-count">{{ data.industries.length }}</span></div>
      <button
        v-for="ind in data.industries"
        :key="ind.code"
        class="ex-item"
        :class="{ active: selectedCode === ind.code }"
        @click="emit('select-plugin', ind.code)"
      >
        <span class="ex-dot" :style="{ background: industryColor(ind.code) }"></span>
        <span class="ex-item-main">
          <span class="ex-item-name">{{ ind.label }}</span>
          <span v-if="ind.version" class="ex-item-sub">v{{ ind.version }}</span>
        </span>
        <span class="ex-item-count">{{ pluginContribTotal(ind) }}</span>
      </button>
    </div>

    <!-- Hook points view -->
    <div v-else-if="view === 'hooks'" class="ex-body">
      <div class="ex-section-title">Hook Points <span class="ex-count">{{ data.hookPoints.length }}</span></div>
      <div v-if="!data.hookPoints.length" class="ex-empty">暂无 hook points</div>
      <template v-for="grp in hookGroups" :key="grp.entityType">
        <div class="ex-hook-group-title">{{ subjectTypeLabel(grp.entityType) }}</div>
        <button
          v-for="h in grp.items"
          :key="h.entityType + '/' + h.phase"
          class="ex-item"
          :class="{ active: selectedHookKey === (h.entityType + '/' + h.phase) }"
          @click="emit('select-hook', h.entityType + '/' + h.phase)"
        >
          <Webhook :size="11" class="ex-icon-muted" />
          <span class="ex-item-main">
            <span class="ex-item-name">{{ h.phase }}</span>
          </span>
          <span class="ex-item-count">{{ listenersFor(h).length || '' }}</span>
        </button>
      </template>
    </div>

    <!-- Resources view -->
    <div v-else class="ex-body">
      <div class="ex-section-title">资源类型</div>
      <button
        v-for="r in RESOURCE_TYPES"
        :key="r.key"
        class="ex-item"
        :class="{ active: selectedResource === r.key }"
        @click="emit('select-resource', r.key)"
      >
        <component :is="resourceIcons[r.key]" :size="12" class="ex-icon-muted" />
        <span class="ex-item-main">
          <span class="ex-item-name">{{ r.label }}</span>
        </span>
        <span class="ex-item-count">{{ countFor(r.key) }}</span>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import {
  Package, Webhook, LayoutGrid, Link2, Bell, Shield, UserCog,
  ShieldCheck, Filter, Zap, BellRing
} from 'lucide-vue-next'
import { RESOURCE_TYPES, industryColor, subjectTypeLabel, type PluginData, type ResourceKey } from '../helpers'

const props = defineProps<{
  view: 'plugins' | 'hooks' | 'resources'
  selectedCode: string
  selectedHookKey: string
  selectedResource: ResourceKey
}>()

const emit = defineEmits<{
  (e: 'change-view', v: 'plugins' | 'hooks' | 'resources'): void
  (e: 'select-plugin', code: string): void
  (e: 'select-hook', key: string): void
  (e: 'select-resource', key: ResourceKey): void
}>()

const data = inject<PluginData>('pluginData')!

const tabs = [
  { key: 'plugins' as const, label: '插件', icon: Package },
  { key: 'hooks' as const, label: 'Hook', icon: Webhook },
  { key: 'resources' as const, label: '资源', icon: LayoutGrid }
]

const resourceIcons: Record<ResourceKey, any> = {
  types: LayoutGrid,
  relations: Link2,
  events: Bell,
  permissions: Shield,
  roles: UserCog,
  policies: ShieldCheck,
  dataScopes: Filter,
  triggerPoints: Zap,
  subscriptionRules: BellRing
}

function pluginContribTotal(ind: any): number {
  return (ind.types || 0) + (ind.relations || 0) + (ind.events || 0)
    + (ind.roles || 0) + (ind.permissions || 0)
    + (ind.triggerPoints || 0) + (ind.policies || 0) + (ind.dataScopes || 0)
}

function listenersFor(h: any): any[] {
  const key = h.entityType + '/' + h.phase
  return data.policies.filter(p => (p.supports || []).includes(key))
}

const hookGroups = computed(() => {
  const m = new Map<string, any>()
  for (const h of data.hookPoints) {
    if (!m.has(h.entityType)) m.set(h.entityType, { entityType: h.entityType, items: [] })
    m.get(h.entityType).items.push(h)
  }
  return Array.from(m.values())
})

function countFor(key: ResourceKey): number {
  const map: Record<ResourceKey, number> = {
    types: data.types.length,
    relations: data.relations.length,
    events: data.events.length,
    permissions: data.permissions.length,
    roles: data.roles.length,
    policies: data.policies.length,
    dataScopes: data.dataScopes.length,
    triggerPoints: data.triggerPoints.length,
    subscriptionRules: data.subscriptionRules.length
  }
  return map[key]
}

// keep linter happy for props read-only
void props
</script>

<style scoped>
.ex-root {
  display: flex; flex-direction: column;
  background: #fff; border: 1px solid #e5e7eb; border-radius: 8px;
  min-height: 0; overflow: hidden;
}
.ex-tabs {
  display: grid; grid-template-columns: 1fr 1fr 1fr;
  border-bottom: 1px solid #e5e7eb;
}
.ex-tab {
  display: inline-flex; align-items: center; justify-content: center; gap: 4px;
  padding: 8px 4px; border: none; background: transparent;
  font-size: 11px; color: #6b7280; cursor: pointer;
  transition: all 0.15s;
  border-bottom: 2px solid transparent;
}
.ex-tab:hover { color: #2563eb; background: #f9fafb; }
.ex-tab.active {
  color: #2563eb; border-bottom-color: #2563eb;
  font-weight: 600; background: #eff6ff;
}

.ex-body {
  flex: 1; overflow: auto;
  padding: 6px 6px 10px;
}
.ex-section-title {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 10px 4px;
  font-size: 10px; font-weight: 700; text-transform: uppercase;
  color: #9ca3af; letter-spacing: 0.5px;
}
.ex-count {
  font-size: 10px; font-weight: 500;
  color: #6b7280; background: #f3f4f6;
  padding: 1px 6px; border-radius: 4px;
  text-transform: none; letter-spacing: 0;
}
.ex-hook-group-title {
  padding: 6px 10px 2px;
  font-size: 10px; color: #9ca3af;
  text-transform: uppercase; letter-spacing: 0.3px;
}
.ex-item {
  display: flex; align-items: center; gap: 8px;
  width: 100%; padding: 6px 10px;
  border: none; background: transparent;
  border-radius: 5px; cursor: pointer; text-align: left;
  font-size: 12px; color: #374151;
  border-left: 2px solid transparent;
  transition: background 0.1s;
}
.ex-item:hover { background: #f3f4f6; }
.ex-item.active {
  background: #eff6ff; color: #2563eb;
  border-left-color: #2563eb;
}
.ex-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.ex-icon-muted { color: #9ca3af; flex-shrink: 0; }
.ex-item.active .ex-icon-muted { color: #2563eb; }
.ex-item-main { flex: 1; min-width: 0; display: flex; align-items: center; gap: 6px; overflow: hidden; }
.ex-item-name {
  font-size: 12px; line-height: 1.4;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.ex-item-sub { font-size: 10px; color: #9ca3af; }
.ex-item-count {
  font-size: 10px; color: #9ca3af;
  background: #f3f4f6; padding: 0 5px;
  border-radius: 4px; min-width: 18px; text-align: center;
}
.ex-item.active .ex-item-count { background: #dbeafe; color: #2563eb; }
.ex-empty {
  padding: 16px 10px; color: #9ca3af;
  font-size: 11px; text-align: center;
}
</style>
