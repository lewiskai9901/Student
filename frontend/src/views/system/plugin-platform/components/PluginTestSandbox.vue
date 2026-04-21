<template>
  <div class="sb-root">
    <header class="sb-head">
      <div>
        <h2 class="sb-title">
          <FlaskConical :size="16" />
          测试沙箱
        </h2>
        <p class="sb-desc">一站式触发各扩展点, 验证插件工作状态</p>
      </div>
      <div class="sb-actions">
        <button class="sb-btn" @click="onSeedDemo">
          <Sprout :size="12" /> 播种样本
        </button>
        <button class="sb-btn sb-btn-danger" @click="onReset">
          <Trash2 :size="12" /> 重置沙箱
        </button>
      </div>
    </header>

    <!-- Sandbox cards grid -->
    <div class="sb-grid">
      <!-- Card 1: Policy check -->
      <section class="sb-card">
        <div class="sb-card-head">
          <ShieldCheck :size="14" />
          Policy Hook 触发
        </div>
        <div class="sb-card-body">
          <label class="sb-label">实体类型</label>
          <select v-model="policyForm.entityType" class="sb-input">
            <option v-for="e in uniqueEntityTypes" :key="e" :value="e">{{ entityLabel(e) }} ({{ e }})</option>
          </select>

          <label class="sb-label">Phase</label>
          <select v-model="policyForm.phase" class="sb-input">
            <option v-for="p in availablePhases" :key="p" :value="p">{{ phaseLabel(p) }} ({{ p }})</option>
          </select>

          <label class="sb-label">Payload JSON</label>
          <textarea
            v-model="policyForm.payloadJson"
            class="sb-textarea"
            rows="3"
            placeholder='{"currentOccupancy": 2, "placeId": 1}'
          ></textarea>

          <button class="sb-btn sb-btn-primary" @click="onPolicyCheck">
            <Play :size="12" /> check
          </button>
        </div>
      </section>

      <!-- Card 2: TriggerPoint fire -->
      <section class="sb-card">
        <div class="sb-card-head">
          <Zap :size="14" />
          TriggerPoint fire
        </div>
        <div class="sb-card-body">
          <label class="sb-label">触发点</label>
          <select v-model="triggerForm.code" class="sb-input">
            <option v-for="t in data.triggerPoints" :key="t.point_code || t.pointCode" :value="t.point_code || t.pointCode">
              {{ t.point_name || t.pointName }} ({{ t.point_code || t.pointCode }})
            </option>
          </select>

          <label class="sb-label">Context JSON</label>
          <textarea
            v-model="triggerForm.contextJson"
            class="sb-textarea"
            rows="3"
            placeholder='{"studentId":1,"score":60}'
          ></textarea>

          <button class="sb-btn sb-btn-primary" @click="onTriggerFire">
            <Play :size="12" /> fire
          </button>
          <p class="sb-hint">⚠️ 实际 fire 会写 entity_events + msg_notifications</p>
        </div>
      </section>

      <!-- Card 3: Relation find -->
      <section class="sb-card">
        <div class="sb-card-head">
          <Link2 :size="14" />
          关系 implied 展开
        </div>
        <div class="sb-card-body">
          <label class="sb-label">resource</label>
          <div class="sb-row">
            <select v-model="relForm.resourceType" class="sb-input sb-flex">
              <option value="user">user</option>
              <option value="place">place</option>
              <option value="org_unit">org_unit</option>
            </select>
            <input v-model.number="relForm.resourceId" type="number" class="sb-input sb-w90" placeholder="id" />
          </div>

          <label class="sb-label">relation</label>
          <select v-model="relForm.relation" class="sb-input">
            <option v-for="r in data.relations" :key="r.relationCode || r.relation_code" :value="r.relationCode || r.relation_code">
              {{ r.relationName || r.relation_name }} ({{ r.relationCode || r.relation_code }})
            </option>
          </select>

          <label class="sb-checkbox">
            <input type="checkbox" v-model="relForm.expandImplied" /> expandImplied (走 BFS)
          </label>

          <button class="sb-btn sb-btn-primary" @click="onRelationFind">
            <Play :size="12" /> find subjects
          </button>
        </div>
      </section>

      <!-- Card 4: DataScope resolve -->
      <section class="sb-card">
        <div class="sb-card-head">
          <Filter :size="14" />
          DataScope resolve
        </div>
        <div class="sb-card-body">
          <label class="sb-label">维度</label>
          <select v-model="scopeForm.dimCode" class="sb-input">
            <option v-for="d in data.dataScopes" :key="d.scopeCode || d.scope_code || d.code" :value="d.scopeCode || d.scope_code || d.code">
              {{ d.scopeName || d.scope_name || d.name }} ({{ d.scopeCode || d.scope_code || d.code }})
            </option>
          </select>

          <label class="sb-label">userId / resourceType</label>
          <div class="sb-row">
            <input v-model.number="scopeForm.userId" type="number" class="sb-input sb-w90" placeholder="userId" />
            <input v-model="scopeForm.resourceType" class="sb-input sb-flex" placeholder="resourceType" />
          </div>

          <button class="sb-btn sb-btn-primary" @click="onScopeResolve">
            <Play :size="12" /> resolve
          </button>
        </div>
      </section>

      <!-- Card 5: TargetMode resolve -->
      <section class="sb-card">
        <div class="sb-card-head">
          <Users :size="14" />
          TargetMode resolve
        </div>
        <div class="sb-card-body">
          <label class="sb-label">模式</label>
          <select v-model="targetForm.modeCode" class="sb-input">
            <option value="BY_SUBJECT">主体本人 (BY_SUBJECT)</option>
            <option value="BY_ROLE">按角色 (BY_ROLE)</option>
            <option value="BY_RELATION">按关系 (BY_RELATION)</option>
            <option value="BY_FEATURE">按能力 (BY_FEATURE)</option>
          </select>

          <label class="sb-label">config JSON</label>
          <textarea
            v-model="targetForm.configJson"
            class="sb-textarea"
            rows="2"
            placeholder='{"roleCode":"TEACHER"}'
          ></textarea>

          <label class="sb-label">event JSON</label>
          <textarea
            v-model="targetForm.eventJson"
            class="sb-textarea"
            rows="2"
            placeholder='{"subjectId":1}'
          ></textarea>

          <button class="sb-btn sb-btn-primary" @click="onTargetResolve">
            <Play :size="12" /> resolve
          </button>
        </div>
      </section>
    </div>

    <!-- Log console -->
    <section class="sb-log">
      <div class="sb-log-head">
        <Terminal :size="14" />
        执行日志 ({{ logs.length }})
        <button class="sb-btn sb-btn-xs" @click="logs = []" style="margin-left:auto;">清空</button>
      </div>
      <div class="sb-log-body">
        <div v-if="!logs.length" class="sb-log-empty">点击任一卡片的按钮开始测试...</div>
        <div v-for="(l, i) in logs" :key="i" class="sb-log-line" :class="'sb-log-' + l.level">
          <span class="sb-log-time">{{ l.time }}</span>
          <span class="sb-log-icon">{{ l.level === 'ok' ? '✓' : l.level === 'err' ? '✗' : '▶' }}</span>
          <span class="sb-log-kind">{{ l.kind }}</span>
          <span class="sb-log-msg">{{ l.msg }}</span>
          <pre v-if="l.detail" class="sb-log-detail">{{ l.detail }}</pre>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, inject } from 'vue'
import {
  FlaskConical, Sprout, Trash2, ShieldCheck, Zap, Link2,
  Filter, Users, Play, Terminal
} from 'lucide-vue-next'
import { http } from '@/utils/request'
import { phaseLabel, subjectTypeLabel, type PluginData } from '../helpers'
import { ElMessage } from 'element-plus'

const data = inject<PluginData>('pluginData')!

// Forms
const policyForm = ref({
  entityType: 'place',
  phase: 'AFTER_CHECKIN',
  payloadJson: '{"currentOccupancy": 2, "placeId": 1}'
})
const triggerForm = ref({ code: '', contextJson: '{}' })
const relForm = ref({
  resourceType: 'place',
  resourceId: 1,
  relation: 'manages',
  expandImplied: true
})
const scopeForm = ref({ dimCode: 'BY_CLASS', userId: 1, resourceType: 'user' })
const targetForm = ref({
  modeCode: 'BY_ROLE',
  configJson: '{"roleCode":"SUPER_ADMIN"}',
  eventJson: '{}'
})

// Derived
const uniqueEntityTypes = computed(() =>
  [...new Set(data.hookPoints.map(h => h.entityType))]
)
const availablePhases = computed(() =>
  data.hookPoints
    .filter(h => h.entityType === policyForm.value.entityType)
    .map(h => h.phase)
)

function entityLabel(e: string) {
  return subjectTypeLabel(e)
}

// Log
interface LogLine {
  time: string
  level: 'ok' | 'err' | 'info'
  kind: string
  msg: string
  detail?: string
}
const logs = ref<LogLine[]>([])

function addLog(level: 'ok' | 'err' | 'info', kind: string, msg: string, detail?: unknown) {
  const d = new Date()
  const pad = (n: number) => n.toString().padStart(2, '0')
  const time = `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  logs.value.unshift({
    time, level, kind, msg,
    detail: detail !== undefined && detail !== null ? JSON.stringify(detail, null, 2) : undefined
  })
  if (logs.value.length > 30) logs.value = logs.value.slice(0, 30)
}

// Handlers
async function onPolicyCheck() {
  try {
    const payload = JSON.parse(policyForm.value.payloadJson || '{}')
    const r: any = await http.post('/plugin-platform/sandbox/policy/check', {
      entityType: policyForm.value.entityType,
      phase: policyForm.value.phase,
      payload
    })
    if (r.matched === 0) {
      addLog('info', 'Policy', `${policyForm.value.entityType}/${policyForm.value.phase} → 0 监听者匹配`)
    } else {
      addLog('ok', 'Policy',
        `${policyForm.value.entityType}/${policyForm.value.phase} → ${r.matched} 违规 (BLOCK=${r.hasBlock})`,
        r.violations)
    }
  } catch (e: any) {
    addLog('err', 'Policy', e?.message || String(e))
  }
}

async function onTriggerFire() {
  try {
    if (!triggerForm.value.code) {
      addLog('err', 'Trigger', '请先选择触发点')
      return
    }
    const context = JSON.parse(triggerForm.value.contextJson || '{}')
    const r: any = await http.post('/plugin-platform/sandbox/trigger/fire', {
      code: triggerForm.value.code,
      context
    })
    if (r.triggered) {
      addLog('ok', 'Trigger',
        `fire(${triggerForm.value.code}) → 新增事件 ${r.eventsCreated}, 通知 ${r.notificationsCreated}`)
    } else {
      addLog('err', 'Trigger', r.error || '触发失败')
    }
  } catch (e: any) {
    addLog('err', 'Trigger', e?.message || String(e))
  }
}

async function onRelationFind() {
  try {
    const r: any = await http.post('/plugin-platform/sandbox/relation/find', relForm.value)
    if (r.error) {
      addLog('err', 'Relation', r.error)
    } else {
      addLog('ok', 'Relation',
        `${relForm.value.relation} on ${relForm.value.resourceType}:${relForm.value.resourceId} → ${r.count} 个 subject${relForm.value.expandImplied ? ' (含 implied)' : ''}`,
        r.subjectIds)
    }
  } catch (e: any) {
    addLog('err', 'Relation', e?.message || String(e))
  }
}

async function onScopeResolve() {
  try {
    const r: any = await http.post('/plugin-platform/sandbox/datascope/resolve', scopeForm.value)
    addLog('ok', 'DataScope',
      `${scopeForm.value.dimCode}(user=${scopeForm.value.userId}, ${scopeForm.value.resourceType}) → ${r.count} ids ${r.interpretation}`,
      r.ids)
  } catch (e: any) {
    addLog('err', 'DataScope', e?.message || String(e))
  }
}

async function onTargetResolve() {
  try {
    const config = JSON.parse(targetForm.value.configJson || '{}')
    const event = JSON.parse(targetForm.value.eventJson || '{}')
    const r: any = await http.post('/plugin-platform/sandbox/target-mode/resolve', {
      modeCode: targetForm.value.modeCode, config, event
    })
    if (r.error) {
      addLog('err', 'TargetMode', r.error, r.available)
    } else {
      addLog('ok', 'TargetMode', `${targetForm.value.modeCode} → ${r.count} 用户`, r.userIds)
    }
  } catch (e: any) {
    addLog('err', 'TargetMode', e?.message || String(e))
  }
}

async function onSeedDemo() {
  try {
    const r: any = await http.post('/plugin-platform/sandbox/seed-demo', {})
    addLog('ok', 'Seed', r.hint || '样本数据创建完成', { placeId: r.placeId })
    ElMessage.success('样本已就绪, placeId=' + r.placeId)
  } catch (e: any) {
    addLog('err', 'Seed', e?.message || String(e))
  }
}

async function onReset() {
  try {
    const r: any = await http.delete('/plugin-platform/sandbox/reset')
    addLog('ok', 'Reset', `清理了 ${r.deletedPlaces} 条样本场所`)
    ElMessage.success('沙箱已重置')
  } catch (e: any) {
    addLog('err', 'Reset', e?.message || String(e))
  }
}
</script>

<style scoped>
.sb-root {
  display: flex; flex-direction: column; gap: 14px;
  padding: 14px 16px; height: 100%; overflow: auto;
}
.sb-head {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 10px; border-bottom: 1px solid #f3f4f6;
}
.sb-title {
  margin: 0; font-size: 15px; font-weight: 700; color: #111827;
  display: flex; align-items: center; gap: 6px;
}
.sb-desc { margin: 4px 0 0; font-size: 11px; color: #9ca3af; }
.sb-actions { display: flex; gap: 6px; }

.sb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 10px;
}
.sb-card {
  background: #fff; border: 1px solid #f3f4f6; border-radius: 8px; overflow: hidden;
}
.sb-card-head {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 12px; background: #fafbfc;
  border-bottom: 1px solid #f3f4f6;
  font-size: 11px; font-weight: 700; color: #4b5563;
  text-transform: uppercase; letter-spacing: 0.3px;
}
.sb-card-body {
  display: flex; flex-direction: column; gap: 4px;
  padding: 10px 12px;
}

.sb-label { font-size: 10px; color: #6b7280; font-weight: 500; margin-top: 4px; }
.sb-input, .sb-textarea {
  padding: 5px 8px; border: 1px solid #e5e7eb; border-radius: 4px;
  font-size: 11px; font-family: 'JetBrains Mono', Menlo, monospace; outline: none;
}
.sb-input:focus, .sb-textarea:focus { border-color: #2563eb; }
.sb-textarea { resize: vertical; }
.sb-row { display: flex; gap: 6px; }
.sb-flex { flex: 1; }
.sb-w90 { width: 90px; }
.sb-checkbox {
  display: flex; align-items: center; gap: 6px;
  font-size: 11px; color: #4b5563; margin-top: 4px; cursor: pointer;
}

.sb-btn {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 5px 10px; border: 1px solid #e5e7eb; background: #fff;
  color: #4b5563; border-radius: 4px; font-size: 11px;
  cursor: pointer; transition: all 0.15s;
}
.sb-btn:hover { border-color: #93c5fd; color: #2563eb; }
.sb-btn-primary {
  background: #2563eb; color: #fff; border-color: #2563eb;
  justify-content: center; margin-top: 8px;
}
.sb-btn-primary:hover { background: #1d4ed8; color: #fff; border-color: #1d4ed8; }
.sb-btn-danger { color: #991b1b; border-color: #fecaca; }
.sb-btn-danger:hover { background: #fef2f2; color: #991b1b; border-color: #fca5a5; }
.sb-btn-xs { padding: 2px 8px; font-size: 10px; }
.sb-hint { margin: 6px 0 0; font-size: 10px; color: #9ca3af; }

.sb-log {
  background: #0f172a; color: #e2e8f0;
  border-radius: 8px; overflow: hidden;
  display: flex; flex-direction: column;
  min-height: 180px; max-height: 300px;
}
.sb-log-head {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 12px; background: #1e293b;
  font-size: 11px; font-weight: 600;
}
.sb-log-body {
  flex: 1; overflow-y: auto; padding: 8px 12px;
  font-family: 'JetBrains Mono', Menlo, monospace; font-size: 11px;
}
.sb-log-empty { color: #64748b; text-align: center; padding: 40px 12px; }
.sb-log-line {
  display: flex; align-items: baseline; gap: 6px;
  padding: 2px 0; flex-wrap: wrap;
}
.sb-log-time { color: #64748b; font-size: 10px; }
.sb-log-icon { width: 12px; text-align: center; }
.sb-log-ok .sb-log-icon { color: #22c55e; }
.sb-log-err .sb-log-icon { color: #ef4444; }
.sb-log-info .sb-log-icon { color: #3b82f6; }
.sb-log-kind { color: #38bdf8; font-weight: 600; }
.sb-log-msg { color: #e2e8f0; flex: 1; min-width: 0; }
.sb-log-detail {
  margin: 4px 0 4px 20px;
  padding: 6px 10px; background: #1e293b;
  border-radius: 4px; color: #94a3b8;
  font-size: 10px; width: 100%; white-space: pre-wrap;
}
</style>
