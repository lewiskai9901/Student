<template>
  <Teleport to="body">
    <div v-if="open" class="sp-backdrop" @click.self="close">
      <div class="sp-panel" @keydown="onKey">
        <div class="sp-head">
          <Search :size="14" />
          <input
            ref="inputRef"
            v-model="query"
            type="text"
            class="sp-input"
            placeholder="搜索插件 / 类型 / 关系 / 触发点 / 事件 / 策略 / 权限 / 角色"
            @input="activeIdx = 0"
          />
          <kbd>Esc</kbd>
        </div>

        <div class="sp-body">
          <div v-if="!query.trim()" class="sp-hint">
            开始输入以搜索全平台资源 · {{ flatTotal }} 条可索引
          </div>
          <div v-else-if="!totalMatches" class="sp-empty">
            未找到 "{{ query }}"
          </div>
          <template v-else>
            <div v-for="grp in groups" :key="grp.kind">
              <div v-if="grp.items.length" class="sp-group">
                <div class="sp-group-head">
                  <component :is="groupIcon(grp.kind)" :size="11" />
                  {{ grp.label }}
                  <span class="sp-group-count">{{ grp.items.length }}</span>
                </div>
                <button
                  v-for="(it, i) in grp.items"
                  :key="grp.kind + '-' + i"
                  class="sp-item"
                  :class="{ active: flatIdx(grp.kind, i) === activeIdx }"
                  @mouseenter="activeIdx = flatIdx(grp.kind, i)"
                  @click="activate(it)"
                >
                  <span class="sp-item-main">{{ it.label }}</span>
                  <code class="sp-item-code">{{ it.code }}</code>
                </button>
              </div>
            </div>
          </template>
        </div>

        <div class="sp-foot">
          <span><kbd>↑</kbd><kbd>↓</kbd> 导航</span>
          <span><kbd>Enter</kbd> 跳转</span>
          <span><kbd>Esc</kbd> 关闭</span>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, ref, watch } from 'vue'
import {
  Search, Package, LayoutGrid, Link2, Zap, Bell,
  ShieldCheck, Shield, UserCog
} from 'lucide-vue-next'
import type { PluginData, ResourceKey } from '../helpers'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'navigate', payload: { kind: string; code?: string; key?: string; resource?: ResourceKey }): void
}>()

const data = inject<PluginData>('pluginData')!

const query = ref('')
const activeIdx = ref(0)
const inputRef = ref<HTMLInputElement | null>(null)

function close() { emit('update:open', false) }

watch(() => props.open, (v) => {
  if (v) {
    query.value = ''
    activeIdx.value = 0
    nextTick(() => inputRef.value?.focus())
  }
})

// ── Index builders ──
type Item = { kind: string; label: string; code: string; data: any }

function match(text: string | undefined, q: string): boolean {
  return !!text && text.toLowerCase().includes(q)
}

const filteredPlugins = computed<Item[]>(() => {
  const q = query.value.toLowerCase().trim()
  if (!q) return []
  return data.industries
    .filter(ind => match(ind.code, q) || match(ind.label, q) || match(ind.manifestClass, q))
    .map(ind => ({ kind: 'plugin', label: ind.label, code: ind.code, data: ind }))
})

const filteredTypes = computed<Item[]>(() => {
  const q = query.value.toLowerCase().trim()
  if (!q) return []
  return data.types
    .filter(t => match(t.typeCode, q) || match(t.typeName, q))
    .slice(0, 8)
    .map(t => ({ kind: 'type', label: t.typeName || t.typeCode, code: t.typeCode, data: t }))
})

const filteredRelations = computed<Item[]>(() => {
  const q = query.value.toLowerCase().trim()
  if (!q) return []
  return data.relations
    .filter(r => match(r.relationCode, q) || match(r.relationName, q))
    .slice(0, 8)
    .map(r => ({ kind: 'relation', label: r.relationName || r.relationCode, code: r.relationCode, data: r }))
})

const filteredTriggers = computed<Item[]>(() => {
  const q = query.value.toLowerCase().trim()
  if (!q) return []
  return data.triggerPoints
    .filter(t => match(t.point_code, q) || match(t.point_name, q))
    .slice(0, 8)
    .map(t => ({ kind: 'trigger', label: t.point_name || t.point_code, code: t.point_code, data: t }))
})

const filteredEvents = computed<Item[]>(() => {
  const q = query.value.toLowerCase().trim()
  if (!q) return []
  return data.events
    .filter(e => match(e.typeCode, q) || match(e.typeName, q))
    .slice(0, 8)
    .map(e => ({ kind: 'event', label: e.typeName || e.typeCode, code: e.typeCode, data: e }))
})

const filteredPolicies = computed<Item[]>(() => {
  const q = query.value.toLowerCase().trim()
  if (!q) return []
  return data.policies
    .filter(p => match(p.code, q) || match(p.name, q))
    .slice(0, 6)
    .map(p => ({ kind: 'policy', label: p.name || p.code, code: p.code, data: p }))
})

const filteredPermissions = computed<Item[]>(() => {
  const q = query.value.toLowerCase().trim()
  if (!q) return []
  return data.permissions
    .filter((p: any) => match(p.code || p.permissionCode, q) || match(p.name || p.permissionName, q))
    .slice(0, 6)
    .map((p: any) => ({
      kind: 'permission',
      label: p.name || p.permissionName || '(未命名)',
      code: p.code || p.permissionCode,
      data: p
    }))
})

const filteredRoles = computed<Item[]>(() => {
  const q = query.value.toLowerCase().trim()
  if (!q) return []
  return data.roles
    .filter((r: any) => match(r.roleCode, q) || match(r.roleName, q))
    .slice(0, 6)
    .map((r: any) => ({ kind: 'role', label: r.roleName || r.roleCode, code: r.roleCode, data: r }))
})

const groups = computed(() => [
  { kind: 'plugin', label: '插件', items: filteredPlugins.value },
  { kind: 'type', label: '类型', items: filteredTypes.value },
  { kind: 'relation', label: '关系', items: filteredRelations.value },
  { kind: 'trigger', label: '触发点', items: filteredTriggers.value },
  { kind: 'event', label: '事件类型', items: filteredEvents.value },
  { kind: 'policy', label: '策略', items: filteredPolicies.value },
  { kind: 'permission', label: '权限', items: filteredPermissions.value },
  { kind: 'role', label: '角色', items: filteredRoles.value }
])

const totalMatches = computed(() => groups.value.reduce((s, g) => s + g.items.length, 0))
const flatList = computed(() => groups.value.flatMap(g => g.items.map(it => ({ ...it, _group: g.kind }))))
const flatTotal = computed(() =>
  data.industries.length + data.types.length + data.relations.length +
  data.triggerPoints.length + data.events.length + data.policies.length +
  data.permissions.length + data.roles.length
)

function flatIdx(kind: string, inGroup: number): number {
  let acc = 0
  for (const g of groups.value) {
    if (g.kind === kind) return acc + inGroup
    acc += g.items.length
  }
  return -1
}

function groupIcon(kind: string) {
  return ({
    plugin: Package, type: LayoutGrid, relation: Link2,
    trigger: Zap, event: Bell, policy: ShieldCheck,
    permission: Shield, role: UserCog
  } as Record<string, any>)[kind] || LayoutGrid
}

function onKey(ev: KeyboardEvent) {
  if (ev.key === 'Escape') { close(); return }
  if (ev.key === 'ArrowDown') {
    ev.preventDefault()
    activeIdx.value = Math.min(activeIdx.value + 1, Math.max(0, totalMatches.value - 1))
  } else if (ev.key === 'ArrowUp') {
    ev.preventDefault()
    activeIdx.value = Math.max(0, activeIdx.value - 1)
  } else if (ev.key === 'Enter') {
    ev.preventDefault()
    const it = flatList.value[activeIdx.value]
    if (it) activate(it)
  }
}

function activate(it: Item) {
  if (it.kind === 'plugin') {
    emit('navigate', { kind: 'plugin', code: it.code })
  } else if (it.kind === 'trigger') {
    emit('navigate', { kind: 'resource', resource: 'triggerPoints' })
  } else if (it.kind === 'type') {
    emit('navigate', { kind: 'resource', resource: 'types' })
  } else if (it.kind === 'relation') {
    emit('navigate', { kind: 'resource', resource: 'relations' })
  } else if (it.kind === 'event') {
    emit('navigate', { kind: 'resource', resource: 'events' })
  } else if (it.kind === 'policy') {
    emit('navigate', { kind: 'resource', resource: 'policies' })
  } else if (it.kind === 'permission') {
    emit('navigate', { kind: 'resource', resource: 'permissions' })
  } else if (it.kind === 'role') {
    emit('navigate', { kind: 'resource', resource: 'roles' })
  }
}
</script>

<style scoped>
.sp-backdrop {
  position: fixed; inset: 0; z-index: 3000;
  background: rgba(15, 23, 42, 0.35);
  display: flex; justify-content: center;
  padding-top: 12vh;
}
.sp-panel {
  width: 520px; max-height: 70vh;
  background: #fff; border-radius: 10px;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.25);
  display: flex; flex-direction: column;
  overflow: hidden;
}
.sp-head {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid #f3f4f6;
  color: #6b7280;
}
.sp-input {
  flex: 1; border: none; outline: none;
  font-size: 14px; color: #111827;
  background: transparent;
}
.sp-head kbd {
  font-size: 10px; padding: 1px 6px;
  border: 1px solid #d1d5db; border-radius: 3px;
  background: #f9fafb; color: #6b7280;
  font-family: Menlo, monospace;
}

.sp-body {
  flex: 1; overflow: auto;
  padding: 6px 6px 10px;
  min-height: 140px;
}
.sp-hint, .sp-empty {
  padding: 30px 14px; text-align: center;
  color: #9ca3af; font-size: 12px;
}
.sp-group {
  margin-bottom: 6px;
}
.sp-group-head {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 10px;
  font-size: 10px; font-weight: 700; color: #6b7280;
  text-transform: uppercase; letter-spacing: 0.5px;
}
.sp-group-count {
  margin-left: auto; font-size: 10px; color: #9ca3af;
  background: #f3f4f6; padding: 1px 6px;
  border-radius: 4px; text-transform: none; letter-spacing: 0;
}
.sp-item {
  display: flex; align-items: center; gap: 8px; width: 100%;
  padding: 6px 12px; border: none; background: transparent;
  text-align: left; cursor: pointer; font-size: 12px;
  color: #111827;
}
.sp-item.active {
  background: #eff6ff; color: #2563eb;
}
.sp-item-main { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sp-item-code {
  font-family: 'JetBrains Mono', Menlo, monospace;
  font-size: 10px; padding: 1px 6px;
  background: #f3f4f6; color: #6b7280;
  border-radius: 3px;
}
.sp-item.active .sp-item-code { background: #dbeafe; color: #2563eb; }

.sp-foot {
  display: flex; gap: 14px;
  padding: 8px 14px; border-top: 1px solid #f3f4f6;
  background: #fafbfc;
  font-size: 10px; color: #9ca3af;
}
.sp-foot kbd {
  font-size: 10px; padding: 1px 5px;
  border: 1px solid #d1d5db; border-radius: 3px;
  background: #fff; color: #6b7280;
  font-family: Menlo, monospace;
  margin-right: 3px;
}
</style>
