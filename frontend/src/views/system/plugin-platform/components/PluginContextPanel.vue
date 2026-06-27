<template>
  <aside class="cx-root">
    <!-- Messaging health banner (always at top) -->
    <div class="cx-health" :class="data.messagingHealth.healthy ? 'cx-health-ok' : 'cx-health-bad'">
      <component :is="data.messagingHealth.healthy ? ShieldCheck : AlertTriangle" :size="13" />
      <div class="cx-health-body">
        <div class="cx-health-title">消息管道</div>
        <div class="cx-health-sub">
          {{ data.messagingHealth.healthy ? '正常' : `缺失: ${data.messagingHealth.missingTables.join(', ')}` }}
        </div>
      </div>
    </div>

    <!-- Plugin context -->
    <template v-if="view === 'plugins' && plugin">
      <div class="cx-section">
        <div class="cx-section-title">
          <span class="cx-dot" :style="{ background: industryColor(plugin.code) }"></span>
          {{ plugin.label }}
        </div>
        <dl class="cx-kv">
          <dt>状态</dt>
          <dd>
            <span class="cx-chip" :class="plugin.enabled === false ? 'cx-chip-info' : 'cx-chip-success'">
              {{ plugin.enabled === false ? '已禁用' : (plugin.isPlugin === false ? '非插件' : '已启用') }}
            </span>
          </dd>
          <dt v-if="plugin.version">版本</dt>
          <dd v-if="plugin.version">v{{ plugin.version }}</dd>
          <dt v-if="plugin.manifestClass">Manifest</dt>
          <dd v-if="plugin.manifestClass">
            <code class="cx-mono" :title="plugin.manifestClass">{{ shortClass(plugin.manifestClass) }}</code>
          </dd>
          <dt v-if="plugin.dependsOn?.length">依赖</dt>
          <dd v-if="plugin.dependsOn?.length">
            <span v-for="d in plugin.dependsOn" :key="d" class="cx-dep">{{ d }}</span>
          </dd>
          <dt v-if="plugin.installedAt">安装于</dt>
          <dd v-if="plugin.installedAt">{{ formatDateShort(plugin.installedAt) }}</dd>
          <dt v-if="plugin.lastStartedAt">最近启动</dt>
          <dd v-if="plugin.lastStartedAt">{{ formatDateShort(plugin.lastStartedAt) }}</dd>
          <dt v-if="plugin.uninstallPolicy">卸载策略</dt>
          <dd v-if="plugin.uninstallPolicy" class="cx-small">{{ plugin.uninstallPolicy }}</dd>
        </dl>
      </div>

      <!-- 治理操作已上移至主区 PluginDetail 头部 (任意屏宽可用); 此侧栏仅作信息展示 -->
    </template>

    <!-- Hook context -->
    <template v-else-if="view === 'hooks' && hook">
      <div class="cx-section">
        <div class="cx-section-title">
          <Webhook :size="12" />
          Hook Point
        </div>
        <dl class="cx-kv">
          <dt>实体</dt>
          <dd><code class="cx-mono">{{ hook.entityType }}</code></dd>
          <dt>阶段</dt>
          <dd><code class="cx-mono cx-mono-amber">{{ hook.phase }}</code></dd>
          <dt>监听者</dt>
          <dd><b>{{ hookListeners }}</b> 个策略</dd>
        </dl>
      </div>
    </template>

    <!-- Resource context -->
    <template v-else-if="view === 'resources'">
      <!-- 选中某"类型" → 展示其字段与特性明细 -->
      <template v-if="resourceType === 'types' && selectedItem">
        <div class="cx-section">
          <div class="cx-section-title">
            <LayoutGrid :size="12" />
            {{ selectedItem.typeName }}
          </div>
          <dl class="cx-kv">
            <dt>类型码</dt>
            <dd><code class="cx-mono">{{ selectedItem.typeCode }}</code></dd>
            <dt>实体</dt>
            <dd>{{ subjectTypeLabel(selectedItem.entityType) }}</dd>
            <dt v-if="selectedItem.category">分类</dt>
            <dd v-if="selectedItem.category" :title="selectedItem.category">{{ typeCategoryLabel(selectedItem.category) }}</dd>
            <dt>来源</dt>
            <dd>
              <code v-if="selectedItem.pluginClass" class="cx-mono" :title="selectedItem.pluginClass">{{ shortClass(selectedItem.pluginClass) }}</code>
              <span v-else class="cx-small">自定义</span>
            </dd>
          </dl>
        </div>

        <!-- 字段明细 -->
        <div class="cx-section">
          <div class="cx-section-title">
            字段 <span class="cx-cnt">{{ typeFields.length }}</span>
          </div>
          <div v-if="!typeFields.length" class="cx-small">该类型暂无扩展字段</div>
          <div v-for="f in typeFields" :key="f.key" class="cx-field">
            <div class="cx-field-top">
              <span class="cx-field-label">{{ f.label || f.key }}</span>
              <span v-if="f.required" class="cx-tag cx-tag-req">必填</span>
              <span v-if="f.system" class="cx-tag cx-tag-sys">系统</span>
            </div>
            <div class="cx-field-sub">
              <code class="cx-mono">{{ f.key }}</code>
              <span class="cx-ftype">{{ fieldTypeLabel(f.type) }}</span>
              <span v-if="f.group" class="cx-fgroup">{{ f.group }}</span>
            </div>
          </div>
        </div>

        <!-- 特性明细 (中文说明) -->
        <div class="cx-section">
          <div class="cx-section-title">
            特性 <span class="cx-cnt">{{ typeFeatures.length }}</span>
          </div>
          <div v-if="!typeFeatures.length" class="cx-small">未声明特性</div>
          <div v-for="k in typeFeatures" :key="k" class="cx-feat-item" :title="featureDesc(k) || k">
            <div class="cx-feat-top">
              <span class="cx-feat-label">{{ featureLabel(k) }}</span>
              <code class="cx-mono cx-feat-key">{{ k }}</code>
            </div>
            <div v-if="featureDesc(k)" class="cx-feat-desc">{{ featureDesc(k) }}</div>
          </div>
        </div>
      </template>

      <!-- 否则: 资源总览 + 操作提示 -->
      <template v-else>
        <div class="cx-section">
          <div class="cx-section-title">
            <LayoutGrid :size="12" />
            资源类型
          </div>
          <dl class="cx-kv">
            <dt>当前</dt>
            <dd>{{ resourceLabel }}</dd>
            <dt>总数</dt>
            <dd><b>{{ resourceCount }}</b></dd>
          </dl>
          <p class="cx-small">
            {{ resourceType === 'types' ? '点击左侧表格中的类型, 这里展示它的字段与特性明细。' : '通过 Explorer 切换查看不同资源; 支持按插件过滤。' }}
          </p>
        </div>
      </template>
    </template>

    <template v-else>
      <div class="cx-section cx-section-empty">
        <p>选择 Explorer 中的条目以查看详情</p>
      </div>
    </template>

    <!-- Metrics footer -->
    <div v-if="data.metrics?.totalDurationMs != null" class="cx-metrics">
      <div class="cx-metrics-title">
        <Timer :size="11" /> 启动耗时
      </div>
      <div class="cx-metrics-total">{{ formatMs(data.metrics.totalDurationMs) }}</div>
      <div v-if="topRegistrars.length" class="cx-metrics-list">
        <div v-for="r in topRegistrars" :key="r.name" class="cx-metrics-row">
          <span class="cx-metrics-name">{{ r.name }}</span>
          <span class="cx-metrics-ms">{{ r.durationMs }}ms</span>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import {
  Webhook, LayoutGrid,
  ShieldCheck, AlertTriangle, Timer
} from 'lucide-vue-next'
import {
  industryColor, shortClass, formatDateShort, RESOURCE_TYPES,
  parseTypeFields, allFeatures, featureLabel, featureDesc, fieldTypeLabel,
  typeCategoryLabel, subjectTypeLabel,
  type PluginData, type ResourceKey
} from '../helpers'

const props = defineProps<{
  view: 'plugins' | 'hooks' | 'resources'
  pluginCode: string
  hookKey: string
  resourceType: ResourceKey
  selectedItem?: any
}>()

/** 选中类型的字段定义 (右侧明细)。 */
const typeFields = computed(() => parseTypeFields(props.selectedItem))
/** 选中类型的全部启用特性 key。 */
const typeFeatures = computed(() => allFeatures(props.selectedItem))

const data = inject<PluginData>('pluginData')!

const plugin = computed(() => data.industries.find(i => i.code === props.pluginCode) || null)
const hook = computed(() => {
  if (!props.hookKey) return null
  const [entityType, phase] = props.hookKey.split('/')
  return data.hookPoints.find(h => h.entityType === entityType && h.phase === phase) || null
})
const hookListeners = computed(() => {
  if (!props.hookKey) return 0
  return data.policies.filter(p => (p.supports || []).includes(props.hookKey)).length
})
const resourceLabel = computed(() =>
  RESOURCE_TYPES.find(r => r.key === props.resourceType)?.label || ''
)
const resourceCount = computed(() => {
  const map: Record<ResourceKey, number> = {
    types: data.types.length, relations: data.relations.length,
    events: data.events.length, permissions: data.permissions.length,
    roles: data.roles.length, policies: data.policies.length,
    dataScopes: data.dataScopes.length, triggerPoints: data.triggerPoints.length,
    dataResources: data.dataResources.length, resourceRelations: data.resourceRelations.length,
    subscriptionRules: data.subscriptionRules.length
  }
  return map[props.resourceType] ?? 0
})

function formatMs(ms: number): string {
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(2)}s`
}

const topRegistrars = computed(() => {
  const m = data.metrics?.registrars
  if (!m) return []
  return Object.entries(m)
    .map(([name, v]) => ({ name: shortClass(name), durationMs: v.durationMs }))
    .sort((a, b) => b.durationMs - a.durationMs)
    .slice(0, 5)
})
</script>

<style scoped>
.cx-root {
  display: flex; flex-direction: column; gap: 10px;
  background: #fff; border: 1px solid #e5e7eb; border-radius: 8px;
  padding: 10px; overflow: auto;
  font-size: 12px;
}

.cx-health {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border-radius: 6px; border: 1px solid;
}
.cx-health-ok { background: #ecfdf5; border-color: #a7f3d0; color: #047857; }
.cx-health-bad { background: #fef2f2; border-color: #fca5a5; color: #991b1b; }
.cx-health-title { font-size: 11px; font-weight: 600; }
.cx-health-sub { font-size: 10px; opacity: 0.85; }

.cx-section {
  padding: 8px 4px; border-bottom: 1px solid #f3f4f6;
}
.cx-section:last-of-type { border-bottom: none; }
.cx-section-empty { color: #9ca3af; text-align: center; padding: 20px 4px; font-size: 11px; }
.cx-section-title {
  display: flex; align-items: center; gap: 6px;
  font-size: 11px; font-weight: 700; color: #111827;
  text-transform: uppercase; letter-spacing: 0.3px;
  margin-bottom: 6px;
}
.cx-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }

.cx-kv {
  display: grid; grid-template-columns: 54px 1fr;
  gap: 4px 10px; margin: 0; font-size: 11px;
}
.cx-kv dt { color: #6b7280; font-weight: 500; }
.cx-kv dd { margin: 0; color: #111827; word-break: break-word; }
.cx-small { color: #6b7280; font-size: 10px; line-height: 1.4; margin: 4px 0 0; }
.cx-mono {
  font-family: 'JetBrains Mono', Menlo, monospace;
  font-size: 10px; background: #f3f4f6; color: #4b5563;
  padding: 1px 5px; border-radius: 3px;
}
.cx-mono-amber { background: #fffbeb; color: #d97706; }
.cx-dep {
  display: inline-block; font-size: 10px;
  padding: 1px 6px; background: #eff6ff; color: #2563eb;
  border-radius: 3px; margin-right: 3px;
}

/* 类型明细: 字段 + 特性 */
.cx-cnt {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 16px; height: 15px; padding: 0 5px; margin-left: 4px;
  background: #eef2ff; color: #4338ca; border-radius: 8px;
  font-size: 10px; font-weight: 700;
}
.cx-field { padding: 5px 0; border-bottom: 1px dashed #f3f4f6; }
.cx-field:last-child { border-bottom: none; }
.cx-field-top { display: flex; align-items: center; gap: 5px; flex-wrap: wrap; }
.cx-field-label { font-size: 12px; font-weight: 600; color: #111827; }
.cx-field-sub { display: flex; align-items: center; gap: 6px; margin-top: 2px; flex-wrap: wrap; }
.cx-ftype { font-size: 10px; color: #2563eb; background: #eff6ff; padding: 0 5px; border-radius: 3px; white-space: nowrap; }
.cx-fgroup { font-size: 10px; color: #6b7280; }
.cx-tag { font-size: 9px; font-weight: 600; padding: 0 5px; border-radius: 3px; white-space: nowrap; }
.cx-tag-req { color: #dc2626; background: #fef2f2; }
.cx-tag-sys { color: #6b7280; background: #f3f4f6; }
.cx-feat-row {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  padding: 4px 0; border-bottom: 1px dashed #f3f4f6;
}
.cx-feat-row:last-child { border-bottom: none; }
.cx-feat-label { font-size: 11px; color: #111827; }
.cx-feat-key { flex-shrink: 0; }
.cx-feat-item { padding: 5px 0; border-bottom: 1px dashed #f3f4f6; }
.cx-feat-item:last-child { border-bottom: none; }
.cx-feat-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.cx-feat-desc { font-size: 10px; color: #6b7280; line-height: 1.4; margin-top: 2px; }

.cx-actions {
  display: flex; flex-direction: column; gap: 5px; padding: 4px;
}
.cx-btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 5px;
  padding: 5px 10px; border: 1px solid #d1d5db;
  background: #fff; color: #374151;
  font-size: 12px; cursor: pointer; border-radius: 5px;
  transition: all 0.15s;
}
.cx-btn:hover { border-color: #2563eb; color: #2563eb; }
.cx-btn-primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.cx-btn-primary:hover { background: #1d4ed8; color: #fff; }
.cx-btn-danger { color: #dc2626; border-color: #fecaca; }
.cx-btn-danger:hover { background: #fef2f2; border-color: #dc2626; color: #dc2626; }
.cx-locked {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 6px 10px; background: #f3f4f6; color: #9ca3af;
  font-size: 11px; border-radius: 5px;
}

.cx-chip {
  display: inline-flex; align-items: center;
  font-size: 10px; font-weight: 500;
  padding: 1px 7px; border-radius: 10px; border: 1px solid;
}
.cx-chip-success { color: #059669; border-color: #a7f3d0; background: #ecfdf5; }
.cx-chip-info    { color: #6b7280; border-color: #e5e7eb; background: #f9fafb; }

.cx-metrics {
  border-top: 1px dashed #e5e7eb; padding-top: 8px;
}
.cx-metrics-title {
  display: flex; align-items: center; gap: 4px;
  font-size: 10px; font-weight: 700; color: #6b7280;
  text-transform: uppercase; letter-spacing: 0.3px;
}
.cx-metrics-total {
  font-size: 15px; font-weight: 700;
  color: #111827; margin: 2px 0 6px;
}
.cx-metrics-list { display: flex; flex-direction: column; gap: 2px; }
.cx-metrics-row {
  display: flex; justify-content: space-between;
  padding: 2px 0; font-size: 10px;
}
.cx-metrics-name { color: #6b7280; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cx-metrics-ms { color: #111827; font-weight: 600; font-family: Menlo, monospace; }
</style>
