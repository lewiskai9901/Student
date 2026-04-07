<template>
  <div class="trg-page">
    <!-- Header -->
    <header class="trg-header">
      <div class="header-left">
        <h1 class="page-title">事件触发器</h1>
        <div class="stats-row">
          <span class="stat">总数 <b>{{ totalCount }}</b></span>
          <i class="stat-sep" />
          <span class="stat"><em class="dot dot-active" /> 启用 <b class="c-enabled">{{ enabledCount }}</b></span>
          <i class="stat-sep" />
          <span class="stat"><em class="dot dot-inactive" /> 禁用 <b>{{ disabledCount }}</b></span>
        </div>
      </div>
      <button class="btn-create" @click="openTriggerDialog()">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M7 1v12M1 7h12" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
        新建触发器
      </button>
    </header>

    <!-- Filter Bar -->
    <div class="filter-bar">
      <div class="search-box">
        <svg class="search-icon" width="15" height="15" viewBox="0 0 15 15" fill="none"><circle cx="6.5" cy="6.5" r="5" stroke="currentColor" stroke-width="1.5"/><path d="m10 10 4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
        <input
          v-model="searchKeyword"
          placeholder="搜索触发器名称..."
          class="search-input"
          @keyup.enter="filterTriggers"
        />
      </div>
      <div class="filter-group">
        <select v-model="filterModule" class="filter-select" @change="filterTriggers">
          <option value="">全部模块</option>
          <option v-for="m in moduleOptions" :key="m.code" :value="m.code">{{ m.name }}</option>
        </select>
        <select v-model="filterEventType" class="filter-select" @change="filterTriggers">
          <option value="">全部事件类型</option>
          <option v-for="et in eventTypes" :key="et.typeCode" :value="et.typeCode">{{ et.typeName }}</option>
        </select>
        <button v-if="hasFilters" class="btn-reset" @click="resetFilters">清除筛选</button>
      </div>
    </div>

    <!-- Trigger Table -->
    <div class="table-container" v-loading="loading">
      <table class="trg-table">
        <colgroup>
          <col style="width: 160px" />
          <col style="width: 90px" />
          <col style="width: 120px" />
          <col />
          <col style="width: 120px" />
          <col style="width: 80px" />
          <col style="width: 70px" />
          <col style="width: 100px" />
        </colgroup>
        <thead>
          <tr>
            <th>名称</th>
            <th>模块</th>
            <th>触发点</th>
            <th>条件摘要</th>
            <th>事件类型</th>
            <th>主体类型</th>
            <th>状态</th>
            <th class="text-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in filteredTriggers" :key="row.id" class="trg-row">
            <td class="cell-name">{{ row.name }}</td>
            <td class="cell-module">{{ getModuleNameByTrigger(row.triggerPointCode) }}</td>
            <td>
              <code class="code-badge" :title="row.triggerPointCode">{{ getTriggerPointName(row.triggerPointCode) }}</code>
            </td>
            <td class="cell-condition">{{ formatCondition(row.conditionJson, row.triggerPointCode) }}</td>
            <td>
              <span class="evt-type-chip" :class="getEventTypeClass(row)">
                <em class="dot" :style="{ background: getEventTypeDotColor(row) }" />
                {{ row.eventTypeMode === 'FIXED' ? getEventTypeName(row.eventTypeCode) : '(动态)' }}
              </span>
            </td>
            <td>{{ formatSubjects(row.subjectsJson) }}</td>
            <td @click.stop>
              <button
                class="status-toggle"
                :class="row.isEnabled ? 'is-active' : 'is-inactive'"
                @click="toggleEnabled(row)"
              >
                <em class="toggle-dot" />
                {{ row.isEnabled ? '启用' : '禁用' }}
              </button>
            </td>
            <td class="text-right" @click.stop>
              <button class="action-btn" @click="openTriggerDialog(row)">编辑</button>
              <button class="action-btn action-danger" @click="handleDelete(row)">删除</button>
            </td>
          </tr>
          <tr v-if="!loading && filteredTriggers.length === 0">
            <td colspan="8" class="empty-cell">暂无触发器数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Create/Edit Dialog -->
    <Teleport to="body">
      <Transition name="drawer">
        <div v-if="dialogVisible" class="drawer-overlay" @click.self="dialogVisible = false">
          <div class="drawer-panel">
            <div class="drawer-header">
              <h2 class="drawer-title">{{ triggerForm.id ? '编辑触发器' : '新建触发器' }}</h2>
              <button class="drawer-close" @click="dialogVisible = false">
                <svg width="18" height="18" viewBox="0 0 18 18"><path d="M4.5 4.5l9 9M13.5 4.5l-9 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              </button>
            </div>

            <div class="drawer-body">
              <!-- Section 1: 基本信息 -->
              <section class="form-section">
                <h3 class="section-title">基本信息</h3>
                <div class="field">
                  <label class="field-label">触发器名称 <span class="req">*</span></label>
                  <input v-model="triggerForm.name" placeholder="如：宿舍卫生不合格触发" class="field-input" />
                </div>
                <div class="field">
                  <label class="field-label">描述</label>
                  <input v-model="triggerForm.description" placeholder="可选描述" class="field-input" />
                </div>
              </section>

              <!-- Section 2: 触发来源 -->
              <section class="form-section">
                <h3 class="section-title">触发来源</h3>
                <div class="field-grid cols-2">
                  <div class="field">
                    <label class="field-label">模块</label>
                    <select v-model="selectedModule" class="field-select" @change="onModuleChange">
                      <option value="">请选择模块</option>
                      <option v-for="m in moduleOptions" :key="m.code" :value="m.code">{{ m.name }}</option>
                    </select>
                  </div>
                  <div class="field">
                    <label class="field-label">触发点 <span class="req">*</span></label>
                    <select v-model="triggerForm.triggerPointCode" class="field-select" @change="onTriggerPointChange">
                      <option value="">选择触发点</option>
                      <option v-for="tp in filteredTriggerPoints" :key="tp.pointCode" :value="tp.pointCode">
                        {{ tp.pointName }}
                      </option>
                    </select>
                  </div>
                </div>
                <div v-if="currentContextSchema && Object.keys(currentContextSchema).length > 0" class="context-hint">
                  <span class="context-label">上下文字段:</span>
                  <code v-for="(def, key) in currentContextSchema" :key="key" class="context-field" :title="String(key)">{{ getFieldLabel(def, String(key)) }}</code>
                </div>
              </section>

              <!-- Section 3: 触发条件 -->
              <section class="form-section">
                <h3 class="section-title">触发条件</h3>
                <div class="condition-builder">
                  <div v-for="(cond, idx) in conditions" :key="idx" class="cond-row">
                    <select v-model="cond.field" class="cond-select cond-field">
                      <option value="">字段</option>
                      <option v-for="(def, key) in currentContextSchema" :key="key" :value="key">{{ getFieldLabel(def, String(key)) }}</option>
                    </select>
                    <select v-model="cond.operator" class="cond-select cond-op">
                      <option v-for="op in OPERATORS" :key="op.value" :value="op.value">{{ op.label }}</option>
                    </select>
                    <input v-model="cond.value" class="cond-input" placeholder="值" />
                    <button class="cond-del" @click="conditions.splice(idx, 1)">
                      <svg width="12" height="12" viewBox="0 0 12 12"><path d="M3 3l6 6M9 3l-6 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                    </button>
                  </div>
                  <button class="cond-add" @click="conditions.push({ field: '', operator: '==', value: '' })">
                    <svg width="12" height="12" viewBox="0 0 12 12"><path d="M6 2v8M2 6h8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                    添加条件
                  </button>
                </div>
              </section>

              <!-- Section 4: 生成事件 -->
              <section class="form-section">
                <h3 class="section-title">生成事件</h3>
                <div class="field">
                  <label class="field-label">事件类型</label>
                  <div class="radio-group">
                    <label class="radio-item" :class="{ active: triggerForm.eventTypeMode === 'FIXED' }">
                      <input type="radio" value="FIXED" v-model="triggerForm.eventTypeMode" /> 固定类型
                    </label>
                    <label v-if="dynamicEventTypeField" class="radio-item" :class="{ active: triggerForm.eventTypeMode === 'DYNAMIC' }">
                      <input type="radio" value="DYNAMIC" v-model="triggerForm.eventTypeMode" />
                      动态 <span class="dynamic-hint">(来源: {{ dynamicEventTypeField.label }})</span>
                    </label>
                  </div>
                </div>
                <div v-if="triggerForm.eventTypeMode === 'FIXED'" class="field">
                  <label class="field-label">选择事件类型</label>
                  <select v-model="triggerForm.eventTypeCode" class="field-select">
                    <option value="">选择事件类型</option>
                    <option v-for="et in eventTypes" :key="et.typeCode" :value="et.typeCode">
                      {{ et.categoryName }} / {{ et.typeName }}
                    </option>
                  </select>
                </div>
                <div v-else-if="triggerForm.eventTypeMode === 'DYNAMIC'" class="dynamic-info">
                  运行时将从上下文字段「{{ dynamicEventTypeField?.label }}」自动获取事件类型编码
                </div>
              </section>

              <!-- Section 5: 主体配置 -->
              <section class="form-section">
                <h3 class="section-title">主体配置 <span class="section-hint">勾选后，事件将同时记录到对应主体名下</span></h3>

                <!-- 自动识别的主体 -->
                <div v-if="detectedSubjects.length" class="subject-cards">
                  <label v-for="ds in detectedSubjects" :key="ds.type"
                         class="subject-card" :class="{ checked: isSubjectEnabled(ds.type) }">
                    <input type="checkbox" :checked="isSubjectEnabled(ds.type)" @change="toggleSubject(ds)" />
                    <span class="subject-card-body">
                      <span class="subject-card-type">{{ getSubjectLabel(ds.type) }}</span>
                      <span class="subject-card-fields">{{ ds.idLabel }} / {{ ds.nameLabel || '无名称字段' }}</span>
                    </span>
                    <svg v-if="isSubjectEnabled(ds.type)" class="subject-check" width="16" height="16" viewBox="0 0 16 16">
                      <circle cx="8" cy="8" r="7" fill="#2563eb"/>
                      <path d="M5 8l2 2 4-4" stroke="#fff" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                  </label>
                </div>

                <!-- 无可识别主体时的提示 -->
                <div v-if="!detectedSubjects.length && triggerForm.triggerPointCode" class="subject-empty">
                  当前触发点无可识别的主体字段，请手动添加
                </div>

                <!-- 已选主体预览（显示手动添加的非自动识别项） -->
                <div v-for="(subj, idx) in manualSubjects" :key="'m'+idx" class="subject-row">
                  <div class="subject-header">
                    <span class="subject-index">自定义主体</span>
                    <button class="cond-del" @click="removeManualSubject(idx)">
                      <svg width="12" height="12" viewBox="0 0 12 12"><path d="M3 3l6 6M9 3l-6 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                    </button>
                  </div>
                  <div class="subject-fields">
                    <div class="field">
                      <label class="field-label-sm">类型</label>
                      <div class="radio-group compact">
                        <label v-for="st in SUBJECT_TYPES" :key="st.value" class="radio-item" :class="{ active: subj.type === st.value }">
                          <input type="radio" :value="st.value" v-model="subj.type" /> {{ st.label }}
                        </label>
                      </div>
                    </div>
                    <div class="field-grid cols-2">
                      <div class="field">
                        <label class="field-label-sm">ID来源</label>
                        <select v-model="subj.idSource" class="field-select">
                          <option value="">从上下文选择</option>
                          <option v-for="(def, key) in currentContextSchema" :key="key" :value="key">{{ getFieldLabel(def, String(key)) }}</option>
                        </select>
                      </div>
                      <div class="field">
                        <label class="field-label-sm">名称来源</label>
                        <select v-model="subj.nameSource" class="field-select">
                          <option value="">从上下文选择</option>
                          <option v-for="(def, key) in currentContextSchema" :key="key" :value="key">{{ getFieldLabel(def, String(key)) }}</option>
                        </select>
                      </div>
                    </div>
                  </div>
                </div>

                <button class="cond-add" @click="subjects.push({ type: 'USER', idSource: '', nameSource: '' })">
                  <svg width="12" height="12" viewBox="0 0 12 12"><path d="M6 2v8M2 6h8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                  手动添加主体
                </button>
              </section>
            </div>

            <div class="drawer-footer">
              <button class="btn-cancel" @click="dialogVisible = false">取消</button>
              <button class="btn-save" :disabled="saving" @click="handleSave">
                <svg v-if="saving" class="spin" width="14" height="14" viewBox="0 0 14 14"><circle cx="7" cy="7" r="5.5" stroke="currentColor" stroke-width="1.5" fill="none" stroke-dasharray="20 12" /></svg>
                {{ saving ? '保存中...' : '保存触发器' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { eventTriggerApi, triggerPointApi, eventTypeApi } from '@/api/event'
import type { EventTrigger, TriggerPoint, EventType, ConditionOperator, SubjectConfig } from '@/types/event'
import { CONDITION_OPERATORS, SUBJECT_TYPES } from '@/types/event'

// ==================== State ====================
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const triggers = ref<EventTrigger[]>([])
const triggerPoints = ref<TriggerPoint[]>([])
const eventTypes = ref<EventType[]>([])

const searchKeyword = ref('')
const filterModule = ref('')
const filterEventType = ref('')

const OPERATORS = CONDITION_OPERATORS

// ==================== Computed ====================
const totalCount = computed(() => triggers.value.length)
const enabledCount = computed(() => triggers.value.filter(t => t.isEnabled).length)
const disabledCount = computed(() => totalCount.value - enabledCount.value)
const hasFilters = computed(() => searchKeyword.value || filterModule.value || filterEventType.value)

const moduleOptions = computed(() => {
  const map = new Map<string, string>()
  for (const tp of triggerPoints.value) {
    if (tp.moduleCode && !map.has(tp.moduleCode)) {
      map.set(tp.moduleCode, tp.moduleName || tp.moduleCode)
    }
  }
  return Array.from(map.entries()).map(([code, name]) => ({ code, name })).sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
})

function getTriggerPointName(code: string): string {
  const tp = triggerPoints.value.find(t => t.pointCode === code)
  return tp?.pointName || code
}

function getEventTypeName(code: string | null | undefined): string {
  if (!code) return '--'
  const et = eventTypes.value.find(e => e.typeCode === code)
  return et ? et.typeName : code
}

function getSubjectLabel(type: string): string {
  const st = SUBJECT_TYPES.find(s => s.value === type)
  return st?.label || type
}

function formatSubjects(json: SubjectConfig[] | string | null | undefined): string {
  if (!json) return '--'
  let arr: SubjectConfig[] = json as any
  if (typeof json === 'string') {
    try { arr = JSON.parse(json) } catch { return '--' }
  }
  if (!Array.isArray(arr) || !arr.length) return '--'
  return arr.map(s => getSubjectLabel(s.type)).join(' + ')
}

function getModuleNameByTrigger(triggerPointCode: string): string {
  const tp = triggerPoints.value.find(t => t.pointCode === triggerPointCode)
  return tp?.moduleName || tp?.moduleCode || '--'
}

const filteredTriggers = computed(() => {
  let list = triggers.value
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    list = list.filter(t => t.name.toLowerCase().includes(kw))
  }
  if (filterModule.value) {
    const pointCodes = triggerPoints.value
      .filter(tp => tp.moduleCode === filterModule.value)
      .map(tp => tp.pointCode)
    list = list.filter(t => pointCodes.includes(t.triggerPointCode))
  }
  if (filterEventType.value) {
    list = list.filter(t => t.eventTypeCode === filterEventType.value)
  }
  return list
})

// ==================== Selected module for dialog ====================
const selectedModule = ref('')

const filteredTriggerPoints = computed(() => {
  if (!selectedModule.value) return triggerPoints.value
  return triggerPoints.value.filter(tp => tp.moduleCode === selectedModule.value)
})

interface ContextFieldDef {
  type?: string
  label?: string
  subject?: string  // USER / ORG_UNIT / PLACE
  role?: string     // id / name
}

const currentContextSchema = computed<Record<string, ContextFieldDef | string>>(() => {
  if (!triggerForm.triggerPointCode) return {}
  const tp = triggerPoints.value.find(p => p.pointCode === triggerForm.triggerPointCode)
  if (!tp || !tp.contextSchema) return {}
  if (typeof tp.contextSchema === 'string') {
    try { return JSON.parse(tp.contextSchema) } catch { return {} }
  }
  return tp.contextSchema as Record<string, ContextFieldDef | string>
})

/** 从 contextSchema 自动识别动态事件类型字段 */
const dynamicEventTypeField = computed<{ key: string; label: string } | null>(() => {
  for (const [key, def] of Object.entries(currentContextSchema.value)) {
    if (typeof def === 'object' && def.role === 'eventType') {
      return { key, label: def.label || key }
    }
  }
  return null
})

/** 从 contextSchema 自动识别可用主体 */
interface DetectedSubject {
  type: string      // USER / ORG_UNIT / PLACE
  idField: string
  idLabel: string
  nameField: string
  nameLabel: string
}

const detectedSubjects = computed<DetectedSubject[]>(() => {
  const schema = currentContextSchema.value
  const groups = new Map<string, DetectedSubject>()
  for (const [key, def] of Object.entries(schema)) {
    if (typeof def !== 'object' || !def.subject || !def.role) continue
    let g = groups.get(def.subject)
    if (!g) {
      g = { type: def.subject, idField: '', idLabel: '', nameField: '', nameLabel: '' }
      groups.set(def.subject, g)
    }
    if (def.role === 'id') { g.idField = key; g.idLabel = def.label || key }
    if (def.role === 'name') { g.nameField = key; g.nameLabel = def.label || key }
  }
  return Array.from(groups.values()).filter(g => g.idField)
})

function isSubjectEnabled(type: string): boolean {
  return subjects.value.some(s => s.type === type)
}

function toggleSubject(ds: DetectedSubject) {
  const idx = subjects.value.findIndex(s => s.type === ds.type)
  if (idx >= 0) {
    subjects.value.splice(idx, 1)
  } else {
    subjects.value.push({ type: ds.type, idSource: ds.idField, nameSource: ds.nameField })
  }
}

/** 手动添加的主体 = subjects 中不在自动识别列表里的项 */
const manualSubjects = computed(() => {
  const detectedTypes = new Set(detectedSubjects.value.map(d => d.type))
  return subjects.value.filter(s => !detectedTypes.has(s.type))
})

function removeManualSubject(manualIdx: number) {
  const detectedTypes = new Set(detectedSubjects.value.map(d => d.type))
  let count = 0
  for (let i = 0; i < subjects.value.length; i++) {
    if (!detectedTypes.has(subjects.value[i].type)) {
      if (count === manualIdx) {
        subjects.value.splice(i, 1)
        return
      }
      count++
    }
  }
}

/** 从 contextSchema 的值中提取中文 label */
function getFieldLabel(def: ContextFieldDef | string | unknown, key: string): string {
  if (!def) return key
  if (typeof def === 'string') return def || key
  if (typeof def === 'object' && def !== null && 'label' in (def as any)) return (def as any).label || key
  return key
}

// ==================== Form ====================
const triggerForm = reactive({
  id: null as number | null,
  name: '',
  description: '',
  triggerPointCode: '',
  eventTypeMode: 'FIXED' as 'FIXED' | 'DYNAMIC',
  eventTypeCode: '' as string | null,
  eventTypeSource: '' as string | null,
})

const conditions = ref<Array<{ field: string; operator: ConditionOperator; value: string }>>([])
const subjects = ref<Array<{ type: string; idSource: string; nameSource: string }>>([
  { type: 'USER', idSource: '', nameSource: '' }
])

// ==================== Helpers ====================
function formatCondition(json: any, triggerPointCode?: string): string {
  if (!json) return '--'
  let obj = json
  if (typeof json === 'string') {
    try { obj = JSON.parse(json) } catch { return json }
  }
  const items = obj.all || obj.any || []
  if (!items.length) return '--'
  // Try to resolve field names to Chinese labels from contextSchema
  let schema: Record<string, any> = {}
  if (triggerPointCode) {
    const tp = triggerPoints.value.find(p => p.pointCode === triggerPointCode)
    if (tp?.contextSchema) {
      try {
        schema = typeof tp.contextSchema === 'string' ? JSON.parse(tp.contextSchema) : tp.contextSchema as Record<string, any>
      } catch { /* ignore */ }
    }
  }
  return items.map((c: any) => {
    const def = schema[c.field]
    const label = def ? (typeof def === 'object' ? def.label || c.field : def) : c.field
    return `${label} ${c.operator} ${c.value}`
  }).join(' & ')
}

function getEventTypeClass(row: EventTrigger): string {
  if (row.eventTypeMode === 'DYNAMIC') return 'evt-dynamic'
  return ''
}

function getEventTypeDotColor(row: EventTrigger): string {
  if (row.eventTypeMode === 'DYNAMIC') return '#a78bfa'
  const et = eventTypes.value.find(e => e.typeCode === row.eventTypeCode)
  if (!et) return '#9ca3af'
  const pol = et.polarity || 'NEUTRAL'
  return { POSITIVE: '#22c55e', NEGATIVE: '#ef4444', NEUTRAL: '#9ca3af', INFO: '#3b82f6' }[pol] || '#9ca3af'
}

function onModuleChange() {
  triggerForm.triggerPointCode = ''
}

function onTriggerPointChange() {
  // Reset conditions and subjects when trigger point changes — fields may differ
  conditions.value = []
  subjects.value = []
  // If current mode is DYNAMIC but new trigger point has no dynamic field, reset to FIXED
  if (triggerForm.eventTypeMode === 'DYNAMIC' && !dynamicEventTypeField.value) {
    triggerForm.eventTypeMode = 'FIXED'
  }
}

// ==================== Load ====================
async function loadAll() {
  loading.value = true
  try {
    const [tList, tpList, etList] = await Promise.all([
      eventTriggerApi.list(),
      triggerPointApi.list(),
      eventTypeApi.list(),
    ])
    triggers.value = tList as any
    triggerPoints.value = tpList as any
    eventTypes.value = etList as any
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function filterTriggers() { /* reactive computed handles it */ }
function resetFilters() {
  searchKeyword.value = ''
  filterModule.value = ''
  filterEventType.value = ''
}

// ==================== Dialog ====================
function openTriggerDialog(existing?: EventTrigger) {
  if (existing) {
    triggerForm.id = existing.id
    triggerForm.name = existing.name
    triggerForm.description = existing.description || ''
    triggerForm.triggerPointCode = existing.triggerPointCode
    triggerForm.eventTypeMode = existing.eventTypeMode
    triggerForm.eventTypeCode = existing.eventTypeCode || ''
    triggerForm.eventTypeSource = existing.eventTypeSource || ''

    // Parse conditions
    let condObj = existing.conditionJson
    if (typeof condObj === 'string') {
      try { condObj = JSON.parse(condObj) } catch { condObj = null }
    }
    if (condObj && typeof condObj === 'object') {
      const items = (condObj as any).all || (condObj as any).any || []
      conditions.value = items.map((c: any) => ({
        field: c.field || '',
        operator: c.operator || '==',
        value: String(c.value ?? ''),
      }))
    } else {
      conditions.value = []
    }

    // Parse subjects
    let subjArr = existing.subjectsJson
    if (typeof subjArr === 'string') {
      try { subjArr = JSON.parse(subjArr) } catch { subjArr = null }
    }
    if (Array.isArray(subjArr) && subjArr.length) {
      subjects.value = subjArr.map((s: any) => ({
        type: s.type || 'USER',
        idSource: s.idSource || '',
        nameSource: s.nameSource || '',
      }))
    } else {
      subjects.value = [{ type: 'USER', idSource: '', nameSource: '' }]
    }

    // Set module from trigger point
    const tp = triggerPoints.value.find(p => p.pointCode === existing.triggerPointCode)
    selectedModule.value = tp?.moduleCode || ''
  } else {
    triggerForm.id = null
    triggerForm.name = ''
    triggerForm.description = ''
    triggerForm.triggerPointCode = ''
    triggerForm.eventTypeMode = 'FIXED'
    triggerForm.eventTypeCode = ''
    triggerForm.eventTypeSource = ''
    conditions.value = []
    subjects.value = [{ type: 'USER', idSource: '', nameSource: '' }]
    selectedModule.value = ''
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!triggerForm.name.trim()) { ElMessage.warning('请填写触发器名称'); return }
  if (!triggerForm.triggerPointCode) { ElMessage.warning('请选择触发点'); return }

  const validSubjects = subjects.value.filter(s => s.idSource)
  if (!validSubjects.length) { ElMessage.warning('请至少配置一个主体'); return }

  saving.value = true
  try {
    const conditionJson = conditions.value.length > 0
      ? JSON.stringify({ all: conditions.value.filter(c => c.field).map(c => ({ field: c.field, operator: c.operator, value: parseConditionValue(c.value) })) })
      : null

    const data: Record<string, any> = {
      name: triggerForm.name.trim(),
      description: triggerForm.description || undefined,
      triggerPointCode: triggerForm.triggerPointCode,
      eventTypeMode: triggerForm.eventTypeMode,
      eventTypeCode: triggerForm.eventTypeMode === 'FIXED' ? (triggerForm.eventTypeCode || undefined) : undefined,
      eventTypeSource: triggerForm.eventTypeMode === 'DYNAMIC' ? (dynamicEventTypeField.value?.key || triggerForm.eventTypeSource || undefined) : undefined,
      subjectsJson: validSubjects,
      conditionJson: conditionJson ? JSON.parse(conditionJson) : undefined,
    }

    if (triggerForm.id) {
      await eventTriggerApi.update(triggerForm.id, data)
      ElMessage.success('更新成功')
    } else {
      await eventTriggerApi.create(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadAll()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function parseConditionValue(val: string): unknown {
  if (val === 'true') return true
  if (val === 'false') return false
  const num = Number(val)
  if (!isNaN(num) && val.trim() !== '') return num
  return val
}

async function toggleEnabled(row: EventTrigger) {
  try {
    if (row.isEnabled) {
      await eventTriggerApi.disable(row.id)
    } else {
      await eventTriggerApi.enable(row.id)
    }
    ElMessage.success(row.isEnabled ? '已禁用' : '已启用')
    await loadAll()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleDelete(row: EventTrigger) {
  try {
    await ElMessageBox.confirm(`确定删除触发器「${row.name}」？`, '确认删除', { type: 'warning' })
    await eventTriggerApi.delete(row.id)
    ElMessage.success('已删除')
    await loadAll()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

// ==================== Lifecycle ====================
onMounted(() => { loadAll() })
</script>

<style scoped>
/* ============================================
   Event Trigger View — CourseListView Style
   ============================================ */
.trg-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f8f9fb;
  font-family: 'DM Sans', sans-serif;
}

/* Header */
.trg-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 20px 24px 16px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
}
.page-title {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.025em;
  margin: 0;
}
.stats-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}
.stat { font-size: 12.5px; color: #6b7280; }
.stat b { font-weight: 600; }
.c-enabled { color: #16a34a; }
.stat-sep { display: block; width: 1px; height: 10px; background: #d1d5db; }
.dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 2px; vertical-align: middle; }
.dot-active { background: #10b981; }
.dot-inactive { background: #9ca3af; }

.btn-create {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #111827;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.btn-create:hover { background: #1f2937; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.15); }

/* Filter Bar */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 24px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
}
.search-box {
  position: relative;
  flex: 0 0 240px;
}
.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  pointer-events: none;
}
.search-input {
  width: 100%;
  padding: 7px 10px 7px 32px;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  font-size: 13px;
  font-family: inherit;
  color: #374151;
  background: #f9fafb;
  transition: all 0.15s;
  outline: none;
}
.search-input:focus { border-color: #2563eb; background: #fff; box-shadow: 0 0 0 3px rgba(37,99,235,0.08); }
.search-input::placeholder { color: #9ca3af; }

.filter-group { display: flex; gap: 8px; align-items: center; }
.filter-select {
  padding: 7px 28px 7px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  font-size: 13px;
  font-family: inherit;
  color: #374151;
  background: #f9fafb url("data:image/svg+xml,%3Csvg width='10' height='6' viewBox='0 0 10 6' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%239ca3af' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E") right 10px center no-repeat;
  appearance: none;
  cursor: pointer;
  outline: none;
  transition: border-color 0.15s;
}
.filter-select:focus { border-color: #2563eb; }

.btn-reset {
  padding: 7px 12px;
  font-size: 12px;
  color: #6b7280;
  background: none;
  border: 1px dashed #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.btn-reset:hover { color: #374151; border-color: #9ca3af; }

/* Table */
.table-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}
.trg-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: separate;
  border-spacing: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}
.trg-table th,
.trg-table td {
  padding: 10px 12px;
  vertical-align: middle;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.trg-table th {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #6b7280;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  user-select: none;
}
.trg-table td {
  font-size: 13px;
  color: #374151;
  border-bottom: 1px solid #f3f4f6;
}
.trg-row { transition: background 0.1s; }
.trg-row:hover { background: #f0f5ff; }
.trg-row:last-child td { border-bottom: none; }
.text-right { text-align: right !important; }

.cell-name { font-weight: 500; color: #111827; }
.cell-module { font-size: 12px; color: #6b7280; }
.cell-condition { font-size: 12px; color: #6b7280; }

.code-badge {
  display: inline-block;
  padding: 2px 7px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  color: #475569;
}

.evt-type-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 500;
}
.evt-dynamic { color: #7c3aed; }

.status-toggle {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px 3px 6px;
  border: 1px solid;
  border-radius: 99px;
  font-size: 11.5px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.15s;
  background: #fff;
}
.status-toggle.is-active { border-color: #bbf7d0; color: #16a34a; }
.status-toggle.is-active .toggle-dot { background: #22c55e; }
.status-toggle.is-inactive { border-color: #e5e7eb; color: #6b7280; }
.status-toggle.is-inactive .toggle-dot { background: #9ca3af; }
.toggle-dot { width: 6px; height: 6px; border-radius: 50%; }

.action-btn {
  padding: 4px 8px;
  font-size: 12px;
  color: #4b5563;
  background: none;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.1s;
}
.action-btn:hover { background: #f3f4f6; color: #111827; }
.action-danger:hover { background: #fef2f2; color: #dc2626; }
.empty-cell { text-align: center; padding: 40px 0 !important; color: #9ca3af; font-size: 13px; }

/* ============================================
   Drawer
   ============================================ */
.drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(0,0,0,0.3);
  backdrop-filter: blur(2px);
  display: flex;
  justify-content: flex-end;
}
.drawer-panel {
  width: 620px;
  max-width: 90vw;
  height: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
  box-shadow: -8px 0 32px rgba(0,0,0,0.12);
}
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #e8eaed;
}
.drawer-title {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 17px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}
.drawer-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: #f3f4f6;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
}
.drawer-close:hover { background: #e5e7eb; color: #111827; }

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

/* Form sections */
.form-section {
  padding: 16px 24px;
  border-bottom: 1px solid #f3f4f6;
}
.form-section:last-child { border-bottom: none; }
.section-title {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #6b7280;
  margin: 0 0 12px 0;
}
.section-optional { font-weight: 400; color: #9ca3af; text-transform: none; letter-spacing: 0; }

.field { margin-bottom: 12px; }
.field:last-child { margin-bottom: 0; }
.field-grid { display: grid; gap: 12px; }
.field-grid .field { margin-bottom: 0; }
.cols-2 { grid-template-columns: 1fr 1fr; }

.field-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 5px;
}
.req { color: #ef4444; }

.field-input, .field-select {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  font-size: 13px;
  font-family: inherit;
  color: #111827;
  background: #fafafa;
  outline: none;
  transition: border-color 0.15s;
}
.field-input:focus, .field-select:focus { border-color: #2563eb; background: #fff; box-shadow: 0 0 0 3px rgba(37,99,235,0.08); }
.field-select {
  appearance: none;
  background: #fafafa url("data:image/svg+xml,%3Csvg width='10' height='6' viewBox='0 0 10 6' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%239ca3af' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E") right 10px center no-repeat;
  padding-right: 28px;
  cursor: pointer;
}

.radio-group { display: flex; gap: 8px; flex-wrap: wrap; }
.radio-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
}
.radio-item input { display: none; }
.radio-item.active { border-color: #2563eb; color: #2563eb; background: #eff6ff; }
.dynamic-hint { font-size: 10px; color: #9ca3af; font-weight: 400; margin-left: 2px; }
.radio-item.active .dynamic-hint { color: #60a5fa; }
.dynamic-info {
  padding: 8px 12px;
  background: #f0f7ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  font-size: 12px;
  color: #1e40af;
}

/* Context hint */
.context-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  padding: 8px 10px;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
  border-radius: 6px;
  margin-top: 8px;
}
.context-label { font-size: 11px; color: #64748b; font-weight: 600; }
.context-field {
  display: inline-block;
  padding: 1px 6px;
  background: #e0f2fe;
  border-radius: 3px;
  font-size: 11px;
  color: #0369a1;
}

/* Condition builder */
.condition-builder { display: flex; flex-direction: column; gap: 6px; }
.cond-row {
  display: flex;
  gap: 6px;
  align-items: center;
}
.cond-select {
  padding: 6px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  font-family: inherit;
  color: #374151;
  background: #fafafa;
  outline: none;
  appearance: none;
  cursor: pointer;
}
.cond-field { flex: 1; min-width: 0; }
.cond-op { width: 90px; flex-shrink: 0; }
.cond-input {
  flex: 1;
  min-width: 0;
  padding: 6px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  font-family: inherit;
  color: #374151;
  background: #fafafa;
  outline: none;
}
.cond-input:focus, .cond-select:focus { border-color: #2563eb; }
.cond-del {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 4px;
  background: none;
  color: #d1d5db;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.15s;
}
.cond-del:hover { background: #fef2f2; color: #ef4444; }
.cond-add {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border: 1px dashed #d1d5db;
  border-radius: 6px;
  font-size: 12px;
  color: #6b7280;
  background: none;
  cursor: pointer;
  font-family: inherit;
  align-self: flex-start;
  transition: all 0.15s;
}
.cond-add:hover { color: #2563eb; border-color: #93c5fd; background: #eff6ff; }

/* Subject config */
.section-hint {
  font-weight: 400;
  color: #9ca3af;
  text-transform: none;
  letter-spacing: 0;
  font-size: 10px;
  margin-left: 6px;
}

/* Auto-detected subject cards */
.subject-cards {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 10px;
}
.subject-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1.5px solid #e5e7eb;
  border-radius: 8px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.15s;
  user-select: none;
}
.subject-card input[type="checkbox"] { display: none; }
.subject-card:hover { border-color: #93c5fd; background: #f0f7ff; }
.subject-card.checked { border-color: #2563eb; background: #eff6ff; }
.subject-card-body { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.subject-card-type { font-size: 13px; font-weight: 600; color: #111827; }
.subject-card.checked .subject-card-type { color: #1d4ed8; }
.subject-card-fields { font-size: 11px; color: #6b7280; }
.subject-check { flex-shrink: 0; }
.subject-empty {
  padding: 12px;
  text-align: center;
  font-size: 12px;
  color: #9ca3af;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  margin-bottom: 10px;
}

/* Manual subject rows (fallback) */
.subject-row {
  padding: 10px 12px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  margin-bottom: 8px;
}
.subject-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.subject-index {
  font-size: 11px;
  font-weight: 600;
  color: #6b7280;
}
.subject-fields { display: flex; flex-direction: column; gap: 8px; }
.field-label-sm {
  display: block;
  font-size: 11px;
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 4px;
}
.radio-group.compact { gap: 6px; }
.radio-group.compact .radio-item { padding: 3px 10px; font-size: 11px; }

/* Drawer footer */
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 24px;
  border-top: 1px solid #e8eaed;
}
.btn-cancel {
  padding: 8px 16px;
  background: #f3f4f6;
  color: #6b7280;
  border: none;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.btn-cancel:hover { background: #e5e7eb; }
.btn-save {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #111827;
  color: #fff;
  border: none;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.btn-save:hover { background: #1f2937; }
.btn-save:disabled { opacity: 0.5; cursor: not-allowed; }

@keyframes spin { to { transform: rotate(360deg); } }
.spin { animation: spin 1s linear infinite; }

/* Drawer transitions */
.drawer-enter-active, .drawer-leave-active { transition: opacity 0.25s; }
.drawer-enter-active .drawer-panel, .drawer-leave-active .drawer-panel { transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1); }
.drawer-enter-from, .drawer-leave-to { opacity: 0; }
.drawer-enter-from .drawer-panel { transform: translateX(100%); }
.drawer-leave-to .drawer-panel { transform: translateX(100%); }
</style>
