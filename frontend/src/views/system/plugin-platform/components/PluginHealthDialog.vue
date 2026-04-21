<template>
  <Teleport to="body">
    <Transition name="hd-fade">
      <div v-if="visible" class="hd-mask" @mousedown="maskDown" @click="maskClick">
        <div class="hd-dialog">
          <!-- Header -->
          <div class="hd-head">
            <h2>健康检查 · <code>{{ code }}</code></h2>
            <button class="hd-close" @click="$emit('close')"><X :size="16" /></button>
          </div>

          <!-- Body -->
          <div v-if="loading" class="hd-loading">加载中…</div>

          <div v-else-if="error" class="hd-loading hd-loading-bad">
            {{ error }}
          </div>

          <div v-else-if="data" class="hd-body">
            <!-- 状态大卡 -->
            <div class="hd-status" :class="'hd-status-' + healthyClass">
              <div class="hd-status-icon">
                <CheckCircle2 v-if="healthy" :size="36" />
                <AlertCircle v-else :size="36" />
              </div>
              <div class="hd-status-body">
                <h3>{{ healthyText }}</h3>
                <p>{{ data.name }} · v{{ data.version || '1.0.0' }}</p>
              </div>
            </div>

            <!-- 总览 -->
            <div class="hd-meta">
              <span><b>{{ data.totalContributions }}</b> 项贡献</span>
              <span class="hd-dot">·</span>
              <span v-if="data.lastStartedAt">上次启动 {{ formatTime(data.lastStartedAt) }}</span>
              <span v-else class="hd-muted">未启动</span>
            </div>

            <!-- 贡献分项 -->
            <section class="hd-sec">
              <div class="hd-sec-head">贡献分项</div>
              <div v-if="mainContributions.length" class="hd-contrib-list">
                <div v-for="c in mainContributions" :key="c.key" class="hd-contrib">
                  <component :is="c.icon" :size="15" :style="{color: c.color}" />
                  <span class="hd-contrib-label">{{ c.label }}</span>
                  <span class="hd-contrib-count">{{ c.count }}</span>
                  <div v-if="c.samples?.length" class="hd-samples">
                    └─ {{ c.samples.slice(0, 3).join(' · ') }}
                  </div>
                </div>
              </div>
              <div v-else class="hd-muted hd-empty">无任何贡献</div>
              <!-- 零值 compact line -->
              <div v-if="zeroContributions.length" class="hd-zero-line">
                ({{ zeroContributions.map(z => z.label + ' 0').join(' · ') }})
              </div>
            </section>

            <!-- 依赖链 -->
            <section v-if="data.dependencies?.length" class="hd-sec">
              <div class="hd-sec-head">依赖链</div>
              <div class="hd-dep-list">
                <div v-for="dep in data.dependencies" :key="dep.code" class="hd-dep">
                  <CheckCircle2 v-if="dep.status === 'HEALTHY'" :size="13" class="hd-dep-ok" />
                  <AlertCircle v-else :size="13" class="hd-dep-bad" />
                  <code>{{ dep.code }}</code>
                  <span class="hd-muted">v{{ dep.version || '?' }}</span>
                  <span :class="dep.status === 'HEALTHY' ? 'hd-muted' : 'hd-bad-text'">
                    {{ depStatusText(dep.status) }}
                  </span>
                </div>
              </div>
            </section>

            <!-- 警告 -->
            <section v-if="data.warnings?.length" class="hd-sec">
              <div class="hd-sec-head hd-sec-warn">
                警告 <span class="hd-badge-warn">{{ data.warnings.length }}</span>
              </div>
              <ul class="hd-warn-list">
                <li v-for="(w, i) in data.warnings" :key="i">{{ w }}</li>
              </ul>
            </section>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  X, CheckCircle2, AlertCircle,
  Package, Link2, Bell, Shield, Menu as MenuIcon,
  ShieldCheck, Filter, UserCog, Crosshair
} from 'lucide-vue-next'
import { pluginPlatformApi, type PluginHealthInfo } from '@/api/pluginPlatform'

const props = defineProps<{ code: string; visible: boolean }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const data = ref<PluginHealthInfo | null>(null)
const loading = ref(false)
const error = ref<string>('')
const maskTarget = ref<EventTarget | null>(null)

function maskDown(e: MouseEvent) { maskTarget.value = e.target }
function maskClick(e: MouseEvent) {
  if (e.target === e.currentTarget && maskTarget.value === e.currentTarget) emit('close')
  maskTarget.value = null
}

watch(() => props.visible, async v => {
  if (v && props.code) {
    loading.value = true
    error.value = ''
    data.value = null
    try {
      const res = await pluginPlatformApi.health(props.code)
      data.value = res as PluginHealthInfo
    } catch (e: any) {
      error.value = '加载失败: ' + (e?.message || e)
    } finally {
      loading.value = false
    }
  }
})

const healthy = computed(() => data.value?.status === 'HEALTHY')
const healthyClass = computed(() => healthy.value ? 'ok' : 'bad')
const healthyText = computed(() => {
  if (!data.value) return ''
  const map: Record<string, string> = {
    HEALTHY: '健康',
    UNHEALTHY: '异常',
    NO_CONTRIBUTIONS: '无贡献'
  }
  return map[data.value.status] || data.value.status
})

function depStatusText(s: string) {
  return ({ HEALTHY: '健康', DISABLED: '已禁用', MISSING: '缺失' } as Record<string, string>)[s] || s
}

const CONTRIB_META = [
  { key: 'types',         label: '实体类型', icon: Package,     color: '#2563eb' },
  { key: 'relations',     label: '关系类型', icon: Link2,       color: '#7c3aed' },
  { key: 'events',        label: '事件类型', icon: Bell,        color: '#d97706' },
  { key: 'triggerPoints', label: '触发点',   icon: Crosshair,   color: '#b45309' },
  { key: 'permissions',   label: '权限',     icon: Shield,      color: '#059669' },
  { key: 'roles',         label: '角色',     icon: UserCog,     color: '#4338ca' },
  { key: 'menus',         label: '菜单',     icon: MenuIcon,    color: '#6b7280' },
  { key: 'policies',      label: '策略',     icon: ShieldCheck, color: '#be185d' },
  { key: 'dataScopes',    label: '数据维度', icon: Filter,      color: '#9333ea' }
] as const

type ContribKey = typeof CONTRIB_META[number]['key']

const mainContributions = computed(() => {
  if (!data.value) return []
  return CONTRIB_META
    .map(m => ({
      ...m,
      count: (data.value!.contributions as Record<string, number>)[m.key] ?? 0,
      samples: (data.value!.samples as Record<string, string[]> | undefined)?.[m.key] || []
    }))
    .filter(c => c.count > 0)
})

const zeroContributions = computed(() => {
  if (!data.value) return []
  return CONTRIB_META
    .map(m => ({
      ...m,
      count: (data.value!.contributions as Record<string, number>)[m.key] ?? 0
    }))
    .filter(c => c.count === 0)
})

function formatTime(ts?: string): string {
  if (!ts) return '-'
  const d = new Date(ts)
  if (isNaN(d.getTime())) return ts
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getMonth() + 1}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<style scoped>
.hd-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center;
  z-index: 9000;
}
.hd-dialog {
  background: #fff; border-radius: 12px; overflow: hidden;
  width: 520px; max-height: 85vh; display: flex; flex-direction: column;
  box-shadow: 0 20px 50px rgba(0,0,0,0.2);
}
.hd-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 18px; border-bottom: 1px solid #f3f4f6;
  background: #fafbfc;
}
.hd-head h2 { margin: 0; font-size: 15px; font-weight: 600; color: #111827; }
.hd-head code { font-family: 'JetBrains Mono', Menlo, monospace; color: #2563eb; font-size: 14px; }
.hd-close {
  border: none; background: transparent; cursor: pointer;
  padding: 4px; border-radius: 4px; color: #6b7280;
}
.hd-close:hover { background: #f3f4f6; color: #111827; }

.hd-body { padding: 16px 18px; overflow-y: auto; display: flex; flex-direction: column; gap: 14px; }
.hd-loading { padding: 60px; text-align: center; color: #9ca3af; font-size: 13px; }
.hd-loading-bad { color: #b91c1c; }

.hd-status {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 16px; border-radius: 8px; border: 1px solid;
}
.hd-status-ok { background: #f0fdf4; border-color: #86efac; color: #166534; }
.hd-status-bad { background: #fef2f2; border-color: #fca5a5; color: #991b1b; }
.hd-status-icon { flex-shrink: 0; }
.hd-status-body h3 { margin: 0; font-size: 16px; font-weight: 700; }
.hd-status-body p { margin: 4px 0 0; font-size: 12px; opacity: 0.8; }

.hd-meta {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: #4b5563;
}
.hd-meta b { color: #111827; }
.hd-dot { color: #d1d5db; }
.hd-muted { color: #9ca3af; }
.hd-bad-text { color: #b91c1c; }
.hd-empty { font-size: 12px; padding: 6px 0; }

.hd-sec { display: flex; flex-direction: column; gap: 6px; }
.hd-sec-head {
  font-size: 10.5px; font-weight: 700; color: #4b5563;
  text-transform: uppercase; letter-spacing: 0.5px;
  padding: 4px 0;
  display: flex; align-items: center; gap: 8px;
}
.hd-sec-warn { color: #b45309; }
.hd-badge-warn {
  background: #fef3c7; color: #92400e; font-weight: 700;
  padding: 1px 8px; border-radius: 10px; font-size: 10px;
}

.hd-contrib-list { display: flex; flex-direction: column; gap: 8px; }
.hd-contrib {
  display: grid; grid-template-columns: 22px 1fr auto;
  align-items: center; column-gap: 10px; row-gap: 2px;
  padding: 6px 10px; background: #fafbfc; border-radius: 6px;
}
.hd-contrib-label { font-size: 13px; color: #111827; font-weight: 500; }
.hd-contrib-count { font-size: 16px; font-weight: 700; color: #2563eb; }
.hd-samples {
  grid-column: 2 / span 2;
  font-size: 11px; color: #6b7280; font-family: 'JetBrains Mono', Menlo, monospace;
  padding-top: 2px;
}
.hd-zero-line {
  font-size: 11px; color: #9ca3af; font-style: italic;
  padding: 4px 10px;
}

.hd-dep-list { display: flex; flex-direction: column; gap: 6px; }
.hd-dep {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 10px; background: #fafbfc; border-radius: 6px;
  font-size: 12px;
}
.hd-dep code {
  font-family: 'JetBrains Mono', Menlo, monospace;
  color: #2563eb; font-weight: 600;
}
.hd-dep-ok { color: #16a34a; }
.hd-dep-bad { color: #dc2626; }

.hd-warn-list { margin: 0; padding: 4px 0 4px 18px; color: #92400e; font-size: 12px; }
.hd-warn-list li { padding: 3px 0; }

.hd-fade-enter-active, .hd-fade-leave-active { transition: opacity 0.15s; }
.hd-fade-enter-from, .hd-fade-leave-to { opacity: 0; }
.hd-fade-enter-active .hd-dialog, .hd-fade-leave-active .hd-dialog {
  transition: transform 0.2s;
}
.hd-fade-enter-from .hd-dialog { transform: translateY(10px) scale(0.98); }
.hd-fade-leave-to .hd-dialog { transform: scale(0.98); }
</style>
