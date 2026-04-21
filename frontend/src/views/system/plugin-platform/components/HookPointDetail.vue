<template>
  <div v-if="!hook" class="hp-empty">
    <Webhook :size="32" />
    <p>请从左侧选择一个 Hook Point</p>
  </div>

  <div v-else class="hp-root">
    <header class="hp-head">
      <div>
        <h2 class="hp-title">
          <code class="hp-code">{{ subjectTypeLabel(hook.entityType) }}</code>
          <span class="hp-sep">/</span>
          <code class="hp-code hp-code-phase">{{ hook.phase }}</code>
        </h2>
        <p class="hp-desc">Core 开放的 hook point — 插件可通过 Policy SPI 监听此点</p>
      </div>
      <span class="hp-badge">
        <span class="hp-badge-num">{{ listeners.length }}</span>
        监听者
      </span>
    </header>

    <!-- Listeners -->
    <section class="hp-sec">
      <div class="hp-sec-head">
        <ShieldCheck :size="13" />
        监听策略
      </div>
      <div v-if="!listeners.length" class="hp-empty-inline">暂无策略监听此 hook point</div>
      <table v-else class="hp-table">
        <thead>
          <tr><th>代码</th><th>名称</th><th>源类</th><th>来源插件</th></tr>
        </thead>
        <tbody>
          <tr v-for="p in listeners" :key="p.code">
            <td><code class="hp-mono hp-mono-blue">{{ p.code }}</code></td>
            <td>{{ p.name }}</td>
            <td><code class="hp-mono">{{ shortClass(p.sourceClass) }}</code></td>
            <td>
              <span class="hp-chip" :style="industryChipStyle(p.sourcePlugin)">
                {{ industryLabel(p.sourcePlugin) || p.sourcePlugin }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- Context schema -->
    <section v-if="contextKeys.length" class="hp-sec">
      <div class="hp-sec-head">
        <FileCode :size="13" />
        Context Schema
      </div>
      <table class="hp-table">
        <thead><tr><th>字段</th><th>类型</th></tr></thead>
        <tbody>
          <tr v-for="k in contextKeys" :key="k">
            <td><code class="hp-mono hp-mono-blue">{{ k }}</code></td>
            <td><span class="hp-muted">{{ hook.contextSchema?.[k] || '-' }}</span></td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- Related trigger points -->
    <section v-if="relatedTriggers.length" class="hp-sec">
      <div class="hp-sec-head">
        <Zap :size="13" />
        同领域触发点
        <span class="hp-count">{{ relatedTriggers.length }}</span>
      </div>
      <table class="hp-table">
        <thead><tr><th>模块</th><th>触发点</th><th>名称</th></tr></thead>
        <tbody>
          <tr v-for="t in relatedTriggers" :key="t.point_code">
            <td><span class="hp-chip hp-chip-info">{{ t.module_name || t.module_code }}</span></td>
            <td><code class="hp-mono hp-mono-blue">{{ t.point_code }}</code></td>
            <td>{{ t.point_name }}</td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import { Webhook, ShieldCheck, FileCode, Zap } from 'lucide-vue-next'
import {
  subjectTypeLabel, shortClass, industryChipStyle, industryLabel,
  type PluginData
} from '../helpers'

const props = defineProps<{ hookKey: string }>()
const data = inject<PluginData>('pluginData')!

const hook = computed(() => {
  if (!props.hookKey) return null
  const [entityType, phase] = props.hookKey.split('/')
  return data.hookPoints.find(h => h.entityType === entityType && h.phase === phase) || null
})

const listeners = computed(() => {
  if (!hook.value) return []
  return data.policies.filter(p => (p.supports || []).includes(props.hookKey))
})

const contextKeys = computed(() => {
  if (!hook.value?.contextSchema) return []
  return Object.keys(hook.value.contextSchema)
})

// Related trigger points: match by entityType prefix
const relatedTriggers = computed(() => {
  if (!hook.value) return []
  const entity = String(hook.value.entityType || '').toLowerCase()
  return data.triggerPoints.filter(t => {
    const m = String(t.module_code || '').toLowerCase()
    return m === entity || m.includes(entity)
  })
})
</script>

<style scoped>
.hp-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 80px 20px; color: #9ca3af; gap: 10px; font-size: 12px;
}
.hp-root {
  display: flex; flex-direction: column; gap: 10px;
  padding: 14px 16px; height: 100%; overflow: auto;
}
.hp-head {
  display: flex; align-items: center; justify-content: space-between;
  padding-bottom: 8px; border-bottom: 1px solid #f3f4f6;
}
.hp-title {
  margin: 0; display: flex; align-items: center; gap: 6px;
  font-size: 15px; font-weight: 700;
}
.hp-code {
  font-family: 'JetBrains Mono', Menlo, monospace;
  font-size: 13px; background: #eff6ff; color: #2563eb;
  padding: 2px 8px; border-radius: 4px;
}
.hp-code-phase { background: #fffbeb; color: #d97706; }
.hp-sep { color: #d1d5db; }
.hp-desc { margin: 4px 0 0; font-size: 11px; color: #9ca3af; }
.hp-badge {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 3px 9px; background: #eff6ff; color: #2563eb;
  border-radius: 10px; font-size: 11px; font-weight: 500;
}
.hp-badge-num { font-weight: 700; }

.hp-sec { display: flex; flex-direction: column; gap: 4px; }
.hp-sec-head {
  display: flex; align-items: center; gap: 6px;
  font-size: 11px; font-weight: 700; color: #4b5563;
  text-transform: uppercase; letter-spacing: 0.3px;
  padding: 4px 0;
}
.hp-count {
  margin-left: auto; font-size: 10px; font-weight: 600;
  padding: 1px 7px; border-radius: 10px;
  background: #e5e7eb; color: #4b5563;
  text-transform: none; letter-spacing: 0;
}

.hp-table { width: 100%; border-collapse: collapse; font-size: 12px; background: #fff; border: 1px solid #f3f4f6; border-radius: 6px; overflow: hidden; }
.hp-table thead th {
  text-align: left; font-size: 10px; color: #6b7280;
  font-weight: 500; text-transform: uppercase; letter-spacing: 0.3px;
  padding: 6px 12px; border-bottom: 1px solid #f3f4f6;
  background: #fafbfc;
}
.hp-table tbody td {
  padding: 7px 12px; font-size: 12px;
  color: #111827; border-bottom: 1px solid #f9fafb;
}
.hp-table tbody tr:last-child td { border-bottom: none; }
.hp-mono {
  font-family: 'JetBrains Mono', Menlo, monospace;
  font-size: 10px; background: #f3f4f6; color: #4b5563;
  padding: 1px 5px; border-radius: 3px;
}
.hp-mono-blue { background: #eff6ff; color: #2563eb; font-size: 11px; }
.hp-muted { color: #9ca3af; font-size: 11px; }
.hp-chip {
  display: inline-flex; align-items: center;
  font-size: 11px; font-weight: 500;
  padding: 2px 8px; border-radius: 10px;
  border: 1px solid;
}
.hp-chip-info { color: #6b7280; border-color: #e5e7eb; background: #f9fafb; }
.hp-empty-inline {
  padding: 14px 12px; color: #9ca3af;
  font-size: 11px; text-align: center;
  background: #fafbfc; border-radius: 6px; border: 1px solid #f3f4f6;
}
</style>
