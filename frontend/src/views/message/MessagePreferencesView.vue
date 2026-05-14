<script setup lang="ts">
/**
 * MessagePreferencesView — S+3 用户消息偏好设置
 *
 * 三段式:
 *   1. 全局偏好 (event_type_code = NULL): 默认通道 + 静默时段
 *   2. 按事件类型行级覆盖
 *   3. 测试发送 (调 dry-run 看会被哪些规则匹配)
 */
import type { LongId } from '@/types/common'
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Trash2, Save } from 'lucide-vue-next'
import { http } from '@/utils/request'

interface Preference {
  id?: LongId
  eventTypeCode: string | null
  channels: string[]
  quietHoursStart: string | null
  quietHoursEnd: string | null
  enabled: boolean
}

const CHANNEL_OPTIONS = [
  { code: 'IN_APP', label: '站内消息', desc: '系统消息中心' },
  { code: 'EMAIL', label: '邮件', desc: '需配置邮箱' },
  { code: 'SMS', label: '短信', desc: '需配置手机号' },
  { code: 'WEBHOOK', label: 'Webhook', desc: '推到外部 URL' },
]

const loading = ref(false)
const preferences = ref<Preference[]>([])
const eventTypeOptions = ref<{ typeCode: string; typeName: string }[]>([])

// 全局偏好 (eventTypeCode = null) 单独提取
const globalPref = computed<Preference>(() => {
  const found = preferences.value.find(p => !p.eventTypeCode)
  return found || { eventTypeCode: null, channels: ['IN_APP'], quietHoursStart: null, quietHoursEnd: null, enabled: true }
})
const overrides = computed(() => preferences.value.filter(p => p.eventTypeCode))

const showAddRow = ref(false)
const newOverride = ref<Preference>({
  eventTypeCode: '',
  channels: ['IN_APP'],
  quietHoursStart: null,
  quietHoursEnd: null,
  enabled: true,
})

async function load() {
  loading.value = true
  try {
    const list: any[] = await http.get('/message/preferences')
    preferences.value = list.map(r => ({
      id: r.id,
      eventTypeCode: r.event_type_code,
      channels: safeParseChannels(r.channels),
      quietHoursStart: r.quiet_hours_start,
      quietHoursEnd: r.quiet_hours_end,
      enabled: r.enabled === 1,
    }))
    // 加载事件类型字典 (用 /entity-event-types)
    try {
      const types: any[] = await http.get('/entity-event-types')
      eventTypeOptions.value = (types || []).map(t => ({ typeCode: t.typeCode, typeName: t.typeName }))
    } catch {
      eventTypeOptions.value = []
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '加载偏好失败')
  } finally {
    loading.value = false
  }
}

function safeParseChannels(s: string | string[]): string[] {
  if (Array.isArray(s)) return s
  if (!s) return ['IN_APP']
  try { return JSON.parse(s) } catch { return ['IN_APP'] }
}

async function saveGlobal() {
  try {
    await http.put('/message/preferences', {
      eventTypeCode: null,
      channels: globalPref.value.channels,
      quietHoursStart: globalPref.value.quietHoursStart,
      quietHoursEnd: globalPref.value.quietHoursEnd,
      enabled: globalPref.value.enabled,
    })
    ElMessage.success('已保存全局偏好')
    load()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  }
}

async function saveOverride(p: Preference) {
  try {
    await http.put('/message/preferences', {
      eventTypeCode: p.eventTypeCode,
      channels: p.channels,
      quietHoursStart: p.quietHoursStart,
      quietHoursEnd: p.quietHoursEnd,
      enabled: p.enabled,
    })
    ElMessage.success('已保存')
    load()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  }
}

async function deleteOverride(p: Preference) {
  if (!p.id) return
  try {
    await http.delete(`/message/preferences/${p.id}`)
    ElMessage.success('已删除')
    load()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

async function addOverride() {
  if (!newOverride.value.eventTypeCode) {
    ElMessage.warning('请选择事件类型')
    return
  }
  await saveOverride(newOverride.value)
  showAddRow.value = false
  newOverride.value = { eventTypeCode: '', channels: ['IN_APP'], quietHoursStart: null, quietHoursEnd: null, enabled: true }
}

function toggleChannel(p: Preference, code: string) {
  const idx = p.channels.indexOf(code)
  if (idx >= 0) p.channels.splice(idx, 1)
  else p.channels.push(code)
}

function getEventTypeName(code: string): string {
  return eventTypeOptions.value.find(t => t.typeCode === code)?.typeName || code
}

onMounted(load)
</script>

<template>
  <div class="prf-page insp-shell">
    <header class="prf-header">
      <div class="prf-head__lead">
        <span class="insp-eyebrow">消息偏好 · Notification Preferences</span>
        <h1 class="prf-title">消息偏好设置</h1>
        <p class="prf-sub">控制不同事件类型在哪些通道接收消息, 以及静默时段</p>
      </div>
    </header>

    <!-- 全局偏好 -->
    <section class="prf-card">
      <header class="prf-card-head">
        <h3>全局默认偏好</h3>
        <button class="insp-btn insp-btn--accent insp-btn--sm" @click="saveGlobal">
          <Save :size="13" />保存
        </button>
      </header>
      <div class="prf-card-body">
        <div class="prf-field">
          <label>接收通道 <span class="hint">(空表示所有事件不发任何通道)</span></label>
          <div class="ch-grid">
            <button v-for="ch in CHANNEL_OPTIONS" :key="ch.code"
                    class="ch-card"
                    :class="{ 'is-on': globalPref.channels.includes(ch.code) }"
                    @click="toggleChannel(globalPref, ch.code)">
              <span class="ch-card__name">{{ ch.label }}</span>
              <span class="ch-card__desc">{{ ch.desc }}</span>
            </button>
          </div>
        </div>

        <div class="prf-field">
          <label>静默时段 <span class="hint">(此时段内全部通道关闭, 跨天填写如 22:00 - 08:00)</span></label>
          <div class="quiet-row">
            <input v-model="globalPref.quietHoursStart" type="time" class="insp-input" />
            <span class="quiet-sep">></span>
            <input v-model="globalPref.quietHoursEnd" type="time" class="insp-input" />
            <button v-if="globalPref.quietHoursStart || globalPref.quietHoursEnd"
                    class="insp-btn insp-btn--sm insp-btn--ghost"
                    @click="globalPref.quietHoursStart = null; globalPref.quietHoursEnd = null">
              清除
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- 按事件类型覆盖 -->
    <section class="prf-card">
      <header class="prf-card-head">
        <h3>按事件类型覆盖</h3>
        <button class="insp-btn insp-btn--sm" @click="showAddRow = !showAddRow">
          <Plus :size="13" />添加覆盖
        </button>
      </header>
      <div class="prf-card-body">
        <div v-if="overrides.length === 0 && !showAddRow" class="prf-empty">
          全部事件按全局默认接收. 点击 "添加覆盖" 为特定事件设置不同通道.
        </div>

        <table v-if="overrides.length > 0 || showAddRow" class="prf-table">
          <thead>
            <tr>
              <th>事件类型</th>
              <th>通道</th>
              <th>静默时段</th>
              <th>状态</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in overrides" :key="p.id">
              <td>
                <span class="evt-name">{{ getEventTypeName(p.eventTypeCode!) }}</span>
                <code class="evt-code">{{ p.eventTypeCode }}</code>
              </td>
              <td>
                <button v-for="ch in CHANNEL_OPTIONS" :key="ch.code"
                        class="ch-chip"
                        :class="{ 'is-on': p.channels.includes(ch.code) }"
                        @click="toggleChannel(p, ch.code)">
                  {{ ch.label }}
                </button>
              </td>
              <td>
                <input v-model="p.quietHoursStart" type="time" class="insp-input quiet-mini" />
                <span class="quiet-sep">></span>
                <input v-model="p.quietHoursEnd" type="time" class="insp-input quiet-mini" />
              </td>
              <td>
                <button class="status-toggle" :class="p.enabled ? 'is-active' : 'is-inactive'"
                        @click="p.enabled = !p.enabled">
                  {{ p.enabled ? '启用' : '禁用' }}
                </button>
              </td>
              <td class="text-right">
                <button class="insp-btn insp-btn--sm" @click="saveOverride(p)">保存</button>
                <button class="insp-btn insp-btn--sm insp-btn--danger" @click="deleteOverride(p)">
                  <Trash2 :size="11" />
                </button>
              </td>
            </tr>

            <tr v-if="showAddRow" class="prf-add-row">
              <td>
                <select v-model="newOverride.eventTypeCode" class="insp-input">
                  <option value="">选择事件类型...</option>
                  <option v-for="t in eventTypeOptions" :key="t.typeCode" :value="t.typeCode">
                    {{ t.typeName }} ({{ t.typeCode }})
                  </option>
                </select>
              </td>
              <td>
                <button v-for="ch in CHANNEL_OPTIONS" :key="ch.code"
                        class="ch-chip"
                        :class="{ 'is-on': newOverride.channels.includes(ch.code) }"
                        @click="toggleChannel(newOverride, ch.code)">
                  {{ ch.label }}
                </button>
              </td>
              <td>
                <input v-model="newOverride.quietHoursStart" type="time" class="insp-input quiet-mini" />
                <span class="quiet-sep">></span>
                <input v-model="newOverride.quietHoursEnd" type="time" class="insp-input quiet-mini" />
              </td>
              <td>—</td>
              <td class="text-right">
                <button class="insp-btn insp-btn--sm insp-btn--accent" @click="addOverride">添加</button>
                <button class="insp-btn insp-btn--sm insp-btn--ghost" @click="showAddRow = false">取消</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.prf-page { padding: var(--insp-sp-3) var(--insp-sp-4); }
.prf-header {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: var(--insp-sp-3) var(--insp-sp-4);
  margin-bottom: var(--insp-sp-3);
}
.prf-head__lead { display: flex; flex-direction: column; gap: 2px; }
.prf-title {
  font-family: var(--insp-font-display);
  font-size: var(--insp-text-h1);
  font-weight: var(--insp-fw-bold);
  color: var(--insp-ink-primary);
  margin: 2px 0 0;
}
.prf-sub {
  margin: var(--insp-sp-1) 0 0;
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-tertiary);
}

.prf-card {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  margin-bottom: var(--insp-sp-3);
}
.prf-card-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: var(--insp-sp-3) var(--insp-sp-4);
  border-bottom: 1px solid var(--insp-border-subtle);
}
.prf-card-head h3 {
  margin: 0;
  font-size: var(--insp-text-md);
  font-weight: var(--insp-fw-semibold);
  color: var(--insp-ink-primary);
}
.prf-card-body { padding: var(--insp-sp-3) var(--insp-sp-4); }

.prf-field { margin-bottom: var(--insp-sp-4); }
.prf-field label {
  display: block;
  font-size: var(--insp-text-sm);
  font-weight: var(--insp-fw-medium);
  color: var(--insp-ink-secondary);
  margin-bottom: var(--insp-sp-2);
}
.prf-field .hint { color: var(--insp-ink-quaternary); font-weight: var(--insp-fw-regular); margin-left: var(--insp-sp-1); }

.ch-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--insp-sp-2); }
.ch-card {
  display: flex; flex-direction: column; align-items: flex-start; gap: 2px;
  padding: var(--insp-sp-3);
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  cursor: pointer; font-family: inherit; text-align: left;
  transition: all var(--insp-t-fast);
}
.ch-card:hover { border-color: var(--insp-border-strong); }
.ch-card.is-on {
  background: var(--insp-accent-paler);
  border-color: var(--insp-accent);
}
.ch-card__name {
  font-size: var(--insp-text-md);
  font-weight: var(--insp-fw-semibold);
  color: var(--insp-ink-primary);
}
.ch-card.is-on .ch-card__name { color: var(--insp-accent); }
.ch-card__desc {
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-tertiary);
}

.quiet-row { display: flex; align-items: center; gap: var(--insp-sp-2); }
.quiet-sep { color: var(--insp-ink-tertiary); }
.quiet-mini { width: 90px; }

.prf-empty {
  padding: var(--insp-sp-5);
  text-align: center;
  color: var(--insp-ink-tertiary);
  font-size: var(--insp-text-sm);
}

.prf-table { width: 100%; border-collapse: collapse; }
.prf-table th {
  text-align: left;
  padding: var(--insp-sp-2) var(--insp-sp-3);
  font-size: var(--insp-text-xs);
  font-weight: var(--insp-fw-semibold);
  color: var(--insp-ink-tertiary);
  text-transform: uppercase;
  letter-spacing: var(--insp-tracking-caps);
  background: var(--insp-bg-subtle);
  border-bottom: 1px solid var(--insp-border-subtle);
}
.prf-table td {
  padding: var(--insp-sp-3);
  border-bottom: 1px solid var(--insp-border-subtle);
  font-size: var(--insp-text-sm);
  vertical-align: middle;
}
.prf-table tr:last-child td { border-bottom: 0; }
.prf-add-row { background: var(--insp-bg-subtle); }

.evt-name { font-weight: var(--insp-fw-medium); color: var(--insp-ink-primary); margin-right: var(--insp-sp-2); }
.evt-code {
  display: inline-block;
  padding: 1px var(--insp-sp-1);
  background: var(--insp-bg-sunken);
  border-radius: var(--insp-radius-sm);
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-tertiary);
}

.ch-chip {
  display: inline-flex; align-items: center;
  height: var(--insp-h-xs); padding: 0 var(--insp-sp-2);
  margin-right: var(--insp-sp-1);
  background: var(--insp-bg-subtle);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-sm);
  font-family: inherit; font-size: var(--insp-text-xs);
  color: var(--insp-ink-tertiary);
  cursor: pointer; transition: all var(--insp-t-fast);
}
.ch-chip.is-on {
  background: var(--insp-accent-paler);
  border-color: var(--insp-accent);
  color: var(--insp-accent);
  font-weight: var(--insp-fw-medium);
}

.status-toggle {
  display: inline-flex; align-items: center;
  height: var(--insp-h-xs); padding: 0 var(--insp-sp-2);
  border-radius: var(--insp-radius-pill);
  font-family: inherit; font-size: var(--insp-text-xs);
  cursor: pointer; border: 1px solid;
}
.status-toggle.is-active {
  background: var(--insp-pass-pale);
  border-color: var(--insp-pass-border);
  color: var(--insp-pass);
}
.status-toggle.is-inactive {
  background: var(--insp-bg-subtle);
  border-color: var(--insp-border-default);
  color: var(--insp-ink-tertiary);
}

.text-right { text-align: right; }
</style>
