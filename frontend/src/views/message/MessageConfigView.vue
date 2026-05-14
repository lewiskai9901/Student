<template>
  <div class="cfg-page insp-shell">
    <!-- Header (token 化, 浅色 InspCard) -->
    <header class="cfg-header">
      <div class="cfg-head__lead">
        <span class="insp-eyebrow">订阅与模板 · Subscriptions & Templates</span>
        <h1 class="cfg-title">订阅与模板</h1>
        <div class="stats-row">
          <span class="stat">规则 <b class="insp-num">{{ rules.length }}</b></span>
          <i class="stat-sep" />
          <span class="stat">模板 <b class="insp-num">{{ templates.length }}</b></span>
        </div>
      </div>
      <button class="insp-btn insp-btn--accent" @click="activeTab === 'rules' ? openRuleDrawer() : openTemplateDrawer()">
        <Plus :size="13" />
        {{ activeTab === 'rules' ? '新建规则' : '新建模板' }}
      </button>
    </header>

    <!-- M5.2: 消息管道健康指示 -->
    <div v-if="!messagingHealth.healthy && messagingHealth.missingTables.length" class="health-alert">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" style="flex-shrink: 0;">
        <path d="M12 9v4m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"
              stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <span>
        消息事件管道不完整: <b>{{ messagingHealth.missingTables.join(' / ') }}</b>
        — 事件触发将静默 no-op, 请检查 V97/V98/V68 migration 是否 apply.
      </span>
    </div>

    <!-- Tab Bar -->
    <div class="tab-bar">
      <button class="tab-btn" :class="{ active: activeTab === 'rules' }" @click="activeTab = 'rules'">
        订阅规则
        <span class="tab-count">{{ rules.length }}</span>
      </button>
      <button class="tab-btn" :class="{ active: activeTab === 'templates' }" @click="activeTab = 'templates'">
        消息模板
        <span class="tab-count">{{ templates.length }}</span>
      </button>
    </div>

    <!-- Rules Table -->
    <div v-show="activeTab === 'rules'" class="table-container" v-loading="rulesLoading">
      <table class="cfg-table">
        <colgroup>
          <col style="width: 180px" />
          <col style="width: 160px" />
          <col style="width: 120px" />
          <col />
          <col style="width: 90px" />
          <col style="width: 80px" />
          <col style="width: 100px" />
        </colgroup>
        <thead>
          <tr>
            <th>规则名称</th>
            <th>事件匹配</th>
            <th>通知对象</th>
            <th>对象配置</th>
            <th>渠道</th>
            <th>状态</th>
            <th class="text-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="rule in rules" :key="rule.id" class="cfg-row">
            <td class="cell-name">{{ rule.ruleName }}</td>
            <td>
              <code class="code-badge">{{ [rule.eventCategory, rule.eventType].filter(Boolean).join(' / ') || '全部' }}</code>
            </td>
            <td>{{ getTargetModeLabel(rule.targetMode) }}</td>
            <td class="cell-config">
              <template v-if="rule.targetMode === 'BY_ROLE'">
                <span v-for="r in parseRoleList(rule.targetConfig)" :key="r" class="cfg-chip cfg-chip--role">
                  {{ getRoleLabel(r) }}
                </span>
                <span v-if="parseRoleList(rule.targetConfig).length === 0" class="cell-dim">未指定</span>
              </template>
              <template v-else-if="rule.targetMode === 'BY_RELATION'">
                <span class="cfg-chip cfg-chip--relation">
                  {{ getRelationLabel(rule.targetConfig) }}
                </span>
              </template>
              <template v-else-if="rule.targetMode === 'BY_FEATURE'">
                <span class="cfg-chip cfg-chip--feature">
                  按能力 · {{ getFeatureLabel(rule.targetConfig) }}
                </span>
              </template>
              <template v-else-if="rule.targetMode === 'BY_SUBJECT'">
                <span class="cell-dim">事件主体本人</span>
              </template>
              <template v-else>
                <span class="cell-dim">{{ rule.targetConfig || '--' }}</span>
              </template>
            </td>
            <td>
              <span class="channel-chip" :class="'ch-' + rule.channel">{{ getChannelLabel(rule.channel) }}</span>
            </td>
            <td @click.stop>
              <button class="status-toggle" :class="rule.isEnabled ? 'is-active' : 'is-inactive'" @click="toggleRule(rule)">
                <em class="toggle-dot" />
                {{ rule.isEnabled ? '启用' : '禁用' }}
              </button>
            </td>
            <td class="text-right" @click.stop>
              <button class="action-btn" @click="openRuleDrawer(rule)">编辑</button>
              <button class="action-btn action-danger" @click="handleDeleteRule(rule)">删除</button>
            </td>
          </tr>
          <tr v-if="!rulesLoading && rules.length === 0">
            <td colspan="7" class="empty-cell">暂无订阅规则</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Templates Table -->
    <div v-show="activeTab === 'templates'" class="table-container" v-loading="templatesLoading">
      <table class="cfg-table">
        <colgroup>
          <col style="width: 120px" />
          <col style="width: 140px" />
          <col style="width: 200px" />
          <col />
          <col style="width: 70px" />
          <col style="width: 100px" />
        </colgroup>
        <thead>
          <tr>
            <th>编码</th>
            <th>模板名称</th>
            <th>标题模板</th>
            <th>内容模板</th>
            <th>状态</th>
            <th class="text-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="tpl in templates" :key="tpl.id" class="cfg-row">
            <td><code class="code-badge">{{ tpl.templateCode }}</code></td>
            <td class="cell-name">
              {{ tpl.templateName }}
              <span v-if="tpl.isSystem" class="system-badge">系统</span>
            </td>
            <td class="cell-tpl">{{ tpl.titleTemplate }}</td>
            <td class="cell-tpl cell-content">{{ tpl.contentTemplate || '--' }}</td>
            <td>
              <span class="status-dot" :class="tpl.isEnabled ? 'dot-on' : 'dot-off'" />
              {{ tpl.isEnabled ? '启用' : '禁用' }}
            </td>
            <td class="text-right" @click.stop>
              <template v-if="!tpl.isSystem">
                <button class="action-btn" @click="openTemplateDrawer(tpl)">编辑</button>
                <button class="action-btn action-danger" @click="handleDeleteTemplate(tpl)">删除</button>
              </template>
              <span v-else class="text-muted">--</span>
            </td>
          </tr>
          <tr v-if="!templatesLoading && templates.length === 0">
            <td colspan="6" class="empty-cell">暂无消息模板</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Rule Wizard Drawer -->
    <Teleport to="body">
      <Transition name="drawer">
        <div v-if="showRuleDrawer" class="drawer-overlay" @click.self="showRuleDrawer = false">
          <div class="drawer-panel">
            <div class="drawer-header">
              <h2 class="drawer-title">{{ editingRule ? '编辑通知规则' : '新建通知规则' }}</h2>
              <button class="drawer-close" @click="showRuleDrawer = false">
                <svg width="18" height="18" viewBox="0 0 18 18"><path d="M4.5 4.5l9 9M13.5 4.5l-9 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              </button>
            </div>
            <div class="drawer-body">
              <!-- Step 1: 当发生什么事件 -->
              <section class="form-section">
                <h3 class="section-title">
                  <span class="step-num">1</span> 当发生什么事件？
                </h3>
                <div class="field">
                  <label class="field-label">事件类别</label>
                  <select v-model="ruleForm.eventCategory" class="field-select" @change="ruleForm.eventType = ''">
                    <option value="">全部类别（匹配所有事件）</option>
                    <option v-for="cat in eventCategories" :key="cat.code" :value="cat.code">
                      {{ cat.name }}
                    </option>
                  </select>
                </div>
                <div v-if="ruleForm.eventCategory" class="field">
                  <label class="field-label">事件类型</label>
                  <select v-model="ruleForm.eventType" class="field-select">
                    <option value="">该类别下全部类型</option>
                    <option v-for="et in filteredEventTypes" :key="et.typeCode" :value="et.typeCode">
                      {{ et.typeName }}
                    </option>
                  </select>
                </div>
                <div v-if="ruleForm.eventCategory" class="wizard-preview">
                  匹配：{{ selectedCategoryName }}{{ ruleForm.eventType ? ' > ' + selectedTypeName : '（所有类型）' }}
                </div>
              </section>

              <!-- Step 2: 通知谁 -->
              <section class="form-section">
                <h3 class="section-title">
                  <span class="step-num">2</span> 通知谁？
                </h3>
                <div class="field">
                  <div class="radio-group">
                    <label v-for="(cfg, key) in TargetModeConfig" :key="key"
                           class="radio-item" :class="{ active: ruleForm.targetMode === key }">
                      <input type="radio" :value="key" v-model="ruleForm.targetMode" />
                      {{ cfg.label }}
                    </label>
                  </div>
                  <p class="field-hint">{{ TargetModeConfig[ruleForm.targetMode || 'BY_ROLE']?.description }}</p>
                </div>

                <!-- BY_ROLE: 角色复选框 -->
                <div v-if="ruleForm.targetMode === 'BY_ROLE'" class="field">
                  <label class="field-label">选择角色</label>
                  <div class="role-checks">
                    <label v-for="role in availableRoles" :key="role.roleCode"
                           class="role-check" :class="{ checked: selectedRoleCodes.includes(role.roleCode) }">
                      <input type="checkbox" :value="role.roleCode"
                             :checked="selectedRoleCodes.includes(role.roleCode)"
                             @change="toggleRole(role.roleCode)" />
                      {{ role.roleName }}
                    </label>
                  </div>
                </div>

                <!-- BY_SUBJECT: 无额外配置 -->
                <div v-if="ruleForm.targetMode === 'BY_SUBJECT'" class="wizard-preview">
                  将通知事件主体本人（仅当主体类型为 USER 时生效）
                </div>

                <!-- BY_RELATION: 基于 access_relations 关系导航 -->
                <div v-if="ruleForm.targetMode === 'BY_RELATION'" class="field">
                  <label class="field-label">关系</label>
                  <select v-model="relationConfig.relation" class="field-input" @change="syncRelationConfig">
                    <option value="">请选择关系</option>
                    <option v-for="r in relationTypeOptions" :key="`${r.relationCode}:${r.fromType}:${r.toType}`"
                      :value="r.relationCode">{{ r.relationCode }} — {{ r.relationName }}（{{ r.fromType }} > {{ r.toType }}）</option>
                  </select>
                  <label class="field-label" style="margin-top:8px">资源类型</label>
                  <select v-model="relationConfig.resource_type" class="field-input" @change="syncRelationConfig">
                    <option value="org_unit">org_unit（组织）</option>
                    <option value="place">place（场所）</option>
                    <option value="user">user（用户）</option>
                  </select>
                  <label class="field-label" style="margin-top:8px">方向</label>
                  <select v-model="relationConfig.direction" class="field-input" @change="syncRelationConfig">
                    <option value="inward">inward — 查"对该主体有某关系的用户"</option>
                    <option value="outward">outward — 查"主体对外有某关系的用户"</option>
                  </select>
                  <p class="field-hint">最终配置: {{ ruleForm.targetConfig }}</p>
                </div>

                <!-- BY_FEATURE: 类型能力筛选 -->
                <div v-if="ruleForm.targetMode === 'BY_FEATURE'" class="field">
                  <label class="field-label">能力（features，多选即 AND）</label>
                  <div class="role-checks">
                    <label v-for="f in commonFeatures" :key="f"
                           class="role-check" :class="{ checked: selectedFeatures.includes(f) }">
                      <input type="checkbox" :value="f"
                             :checked="selectedFeatures.includes(f)"
                             @change="toggleFeature(f)" />
                      {{ f }}
                    </label>
                  </div>
                  <p class="field-hint">将命中 entity_type_configs 里 features 包含这些标记的所有用户</p>
                </div>
              </section>

              <!-- Step 3: 消息内容 -->
              <section class="form-section">
                <h3 class="section-title">
                  <span class="step-num">3</span> 消息内容
                </h3>
                <div class="field">
                  <label class="field-label">使用已有模板</label>
                  <select v-model="ruleForm.templateId" class="field-select" @change="onTemplateSelect">
                    <option :value="null">自定义内容</option>
                    <option v-for="tpl in templates" :key="tpl.id" :value="tpl.id">{{ tpl.templateName }}</option>
                  </select>
                </div>
                <template v-if="!ruleForm.templateId">
                  <div class="field">
                    <label class="field-label">通知标题</label>
                    <input v-model="customTitle" class="field-input"
                           placeholder="如: {{subjectName}} 发生{{eventLabel}}" />
                  </div>
                  <div class="field">
                    <label class="field-label">通知内容</label>
                    <textarea v-model="customContent" class="field-textarea" rows="3"
                              placeholder="如: {{subjectName}} 于 {{time}} 被记录{{eventLabel}}" />
                  </div>
                  <div class="var-hint">
                    可用变量:
                    <code v-pre>{{subjectName}}</code>
                    <code v-pre>{{eventLabel}}</code>
                    <code v-pre>{{time}}</code>
                    <code v-pre>{{className}}</code>
                    <code v-pre>{{score}}</code>
                  </div>
                </template>
                <div v-else class="wizard-preview">
                  将使用模板「{{ getTemplateName(ruleForm.templateId) }}」格式化消息
                </div>
              </section>

              <!-- Step 4: 渠道 + 名称 -->
              <section class="form-section">
                <h3 class="section-title">
                  <span class="step-num">4</span> 通知渠道
                </h3>
                <div class="field">
                  <div class="radio-group">
                    <label v-for="(cfg, key) in ChannelConfig" :key="key"
                           class="radio-item" :class="{ active: ruleForm.channel === key }">
                      <input type="radio" :value="key" v-model="ruleForm.channel" />
                      {{ cfg.label }}
                    </label>
                  </div>
                </div>
                <div class="field">
                  <label class="field-label">规则名称</label>
                  <input v-model="ruleForm.ruleName" class="field-input"
                         :placeholder="autoRuleName" />
                  <p class="field-hint">留空将自动生成</p>
                </div>
              </section>
            </div>
            <div v-if="previewResult" class="preview-panel" :class="{ 'has-warn': !!previewResult.warning }">
              <div class="preview-head">
                <span class="preview-title">预览结果</span>
                <strong class="preview-count">{{ previewResult.previewable ? previewResult.totalCount + ' 人命中' : '需事件上下文' }}</strong>
              </div>
              <div v-if="previewResult.warning" class="preview-warn">{{ previewResult.warning }}</div>
              <div v-if="previewResult.sampleUsers?.length" class="preview-samples">
                <span class="preview-samples-label">样本：</span>
                <span v-for="u in previewResult.sampleUsers" :key="u.id" class="preview-sample">
                  {{ u.realName || u.username }}
                </span>
                <span v-if="previewResult.totalCount > previewResult.sampleUsers.length" class="preview-more">
                  ...等 {{ previewResult.totalCount }} 人
                </span>
              </div>
            </div>
            <div class="drawer-footer">
              <button class="btn-cancel" @click="showRuleDrawer = false">取消</button>
              <button class="btn-preview" :disabled="previewLoading" @click="previewCurrentRule">
                {{ previewLoading ? '计算中...' : '预览命中' }}
              </button>
              <button class="btn-save" @click="saveRule">保存规则</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Template Drawer -->
    <Teleport to="body">
      <Transition name="drawer">
        <div v-if="showTemplateDrawer" class="drawer-overlay" @click.self="showTemplateDrawer = false">
          <div class="drawer-panel">
            <div class="drawer-header">
              <h2 class="drawer-title">{{ editingTemplate ? '编辑消息模板' : '新建消息模板' }}</h2>
              <button class="drawer-close" @click="showTemplateDrawer = false">
                <svg width="18" height="18" viewBox="0 0 18 18"><path d="M4.5 4.5l9 9M13.5 4.5l-9 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              </button>
            </div>
            <div class="drawer-body">
              <section class="form-section">
                <h3 class="section-title">基本信息</h3>
                <div class="field-grid cols-2">
                  <div class="field">
                    <label class="field-label">模板编码</label>
                    <input v-model="templateForm.templateCode" placeholder="如: violation_notify" class="field-input" />
                  </div>
                  <div class="field">
                    <label class="field-label">模板名称 <span class="req">*</span></label>
                    <input v-model="templateForm.templateName" placeholder="如: 违规通知" class="field-input" />
                  </div>
                </div>
              </section>

              <section class="form-section">
                <h3 class="section-title">模板内容</h3>
                <div class="field">
                  <label class="field-label">标题模板 <span class="req">*</span></label>
                  <input v-model="templateForm.titleTemplate" placeholder="如: {{subjectName}} 发生违规" class="field-input" />
                </div>
                <div class="field">
                  <label class="field-label">内容模板</label>
                  <textarea v-model="templateForm.contentTemplate" rows="4" placeholder="如: {{subjectName}} 于 {{time}} 被记录违规" class="field-textarea" />
                </div>
                <div class="var-hint">
                  可用变量: <code v-pre>{{subjectName}}</code> <code v-pre>{{time}}</code> <code v-pre>{{reason}}</code> <code v-pre>{{grade}}</code> <code v-pre>{{operatorName}}</code>
                </div>
              </section>
            </div>
            <div class="drawer-footer">
              <button class="btn-cancel" @click="showTemplateDrawer = false">取消</button>
              <button class="btn-save" @click="saveTemplate">保存模板</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from 'lucide-vue-next'
import { msgConfigApi } from '@/api/message'
import { listEntityEventTypes } from '@/api/entityEvent'
import { getRoles } from '@/api/access'
import { http } from '@/utils/request'
import type { MsgSubscriptionRule, MsgTemplate } from '@/types/message'
import type { EntityEventType } from '@/types/entityEvent'
import { TargetModeConfig, ChannelConfig } from '@/types/message'

// ==================== State ====================
const activeTab = ref<'rules' | 'templates'>('rules')
const rulesLoading = ref(false)
const templatesLoading = ref(false)
const rules = ref<MsgSubscriptionRule[]>([])
const templates = ref<MsgTemplate[]>([])

// Event types + Roles for wizard
const allEventTypes = ref<EntityEventType[]>([])
const availableRoles = ref<Array<{ roleCode: string; roleName: string }>>([])

// Rule wizard drawer
const showRuleDrawer = ref(false)
const editingRule = ref<MsgSubscriptionRule | null>(null)
const ruleForm = ref<Partial<MsgSubscriptionRule>>({})
const selectedRoleCodes = ref<string[]>([])
const customTitle = ref('')
const customContent = ref('')
const previewResult = ref<import('@/api/message').RulePreviewResult | null>(null)
const previewLoading = ref(false)

// Template drawer
const showTemplateDrawer = ref(false)
const editingTemplate = ref<MsgTemplate | null>(null)
const templateForm = ref<Partial<MsgTemplate>>({})

// ==================== Computed: Event categories & types ====================
const eventCategories = computed(() => {
  const map = new Map<string, string>()
  for (const et of allEventTypes.value) {
    if (et.categoryCode && !map.has(et.categoryCode)) {
      map.set(et.categoryCode, et.categoryName || et.categoryCode)
    }
  }
  return Array.from(map.entries()).map(([code, name]) => ({ code, name }))
})

const filteredEventTypes = computed(() => {
  if (!ruleForm.value.eventCategory) return []
  return allEventTypes.value.filter(et => et.categoryCode === ruleForm.value.eventCategory)
})

const selectedCategoryName = computed(() => {
  const cat = eventCategories.value.find(c => c.code === ruleForm.value.eventCategory)
  return cat?.name || ruleForm.value.eventCategory || '全部'
})

const selectedTypeName = computed(() => {
  const et = allEventTypes.value.find(t => t.typeCode === ruleForm.value.eventType)
  return et?.typeName || ruleForm.value.eventType || ''
})

const autoRuleName = computed(() => {
  const cat = selectedCategoryName.value
  const type = selectedTypeName.value
  const roles = selectedRoleCodes.value.map(code => {
    const r = availableRoles.value.find(ro => ro.roleCode === code)
    return r?.roleName || code
  }).join('+')
  const target = ruleForm.value.targetMode === 'BY_ROLE' && roles
    ? `通知${roles}`
    : ruleForm.value.targetMode === 'BY_RELATION'
    ? `通知${relationConfig.value.relation || '关联人'}`
    : ruleForm.value.targetMode === 'BY_FEATURE'
    ? '按能力通知'
    : '通知主体'
  return `${type || cat}${target}`
})

// ==================== Helpers ====================
function getTargetModeLabel(mode: MsgSubscriptionRule['targetMode']): string {
  return TargetModeConfig[mode]?.label ?? mode
}

function getChannelLabel(ch: MsgSubscriptionRule['channel']): string {
  return ChannelConfig[ch]?.label ?? ch
}

function getTemplateName(id: LongId | null): string {
  if (!id) return '-'
  return templates.value.find(t => t.id === id)?.templateName ?? String(id)
}

// ==================== S-4: 订阅规则配置可视化 ====================
function safeJsonParse(s: string | null | undefined): any {
  if (!s) return null
  try { return JSON.parse(s) } catch { return null }
}

/** BY_ROLE: 解析角色码列表. 支持 ["ADMIN"] 或 {"__list__":["ADMIN"]} 两种历史契约 */
function parseRoleList(targetConfig: string | null | undefined): string[] {
  const v = safeJsonParse(targetConfig || '')
  if (Array.isArray(v)) return v
  if (v && Array.isArray(v.__list__)) return v.__list__
  return []
}

function getRoleLabel(roleCode: string): string {
  return availableRoles.value.find(r => r.roleCode === roleCode)?.roleName || roleCode
}

/** BY_RELATION: 解析关系导航配置, 转中文 */
const RELATION_LABELS: Record<string, string> = {
  guardian_of: '监护人',
  admin: '管理员',
  member_of: '成员',
  in_class: '所在班级',
  teacher_of: '任课教师',
  manages: '管理',
}
const DIRECTION_LABELS: Record<string, string> = {
  inward: '入向',
  outward: '出向',
}
const RESOURCE_TYPE_LABELS: Record<string, string> = {
  user: '用户', org_unit: '组织', place: '场所',
}

function getRelationLabel(targetConfig: string | null | undefined): string {
  const cfg = safeJsonParse(targetConfig || '')
  if (!cfg) return '关系导航 (未配置)'
  const rel = RELATION_LABELS[cfg.relation] || cfg.relation || '?'
  const dir = DIRECTION_LABELS[cfg.direction] || cfg.direction || ''
  const rt = RESOURCE_TYPE_LABELS[cfg.resource_type] || cfg.resource_type || ''
  return [dir, rt, '·', rel].filter(Boolean).join(' ')
}

/** BY_FEATURE: 能力码 */
function getFeatureLabel(targetConfig: string | null | undefined): string {
  const cfg = safeJsonParse(targetConfig || '')
  if (!cfg) return '未配置'
  if (typeof cfg === 'string') return cfg
  return cfg.feature || cfg.code || JSON.stringify(cfg).slice(0, 30)
}

function toggleRole(code: string) {
  const idx = selectedRoleCodes.value.indexOf(code)
  if (idx >= 0) selectedRoleCodes.value.splice(idx, 1)
  else selectedRoleCodes.value.push(code)
}

// ===== v3 BY_RELATION / BY_FEATURE 支持 =====
const relationTypeOptions = ref<Array<{relationCode:string;fromType:string;toType:string;relationName:string}>>([])
const relationConfig = ref({ relation: '', resource_type: 'org_unit', direction: 'inward' })
const commonFeatures = ['isLearner', 'isStaff', 'receivesPersonalGrade', 'hasGuardian', 'attendanceTracked',
                        'canBeAdminOfOrg', 'canBeResponsibleForPlace', 'canLogin']
const selectedFeatures = ref<string[]>([])

function syncRelationConfig() {
  ruleForm.value.targetConfig = JSON.stringify({
    relation: relationConfig.value.relation,
    resource_type: relationConfig.value.resource_type,
    direction: relationConfig.value.direction
  })
}

function toggleFeature(f: string) {
  const idx = selectedFeatures.value.indexOf(f)
  if (idx >= 0) selectedFeatures.value.splice(idx, 1)
  else selectedFeatures.value.push(f)
  ruleForm.value.targetConfig = JSON.stringify({ features: selectedFeatures.value })
}

async function loadRelationTypes() {
  try {
    const { http } = await import('@/utils/request')
    relationTypeOptions.value = await http.get('/relation-types') as any
  } catch { /* silent */ }
}
loadRelationTypes()

function onTemplateSelect() {
  // Clear custom fields when selecting a template
  if (ruleForm.value.templateId) {
    customTitle.value = ''
    customContent.value = ''
  }
}

// ==================== Load ====================
async function loadRules() {
  rulesLoading.value = true
  try { rules.value = await msgConfigApi.getRules() }
  catch (e: any) { ElMessage.error(e.message || '加载规则失败') }
  finally { rulesLoading.value = false }
}

async function loadTemplates() {
  templatesLoading.value = true
  try { templates.value = await msgConfigApi.getTemplates() }
  catch (e: any) { ElMessage.error(e.message || '加载模板失败') }
  finally { templatesLoading.value = false }
}

async function loadEventTypes() {
  try { allEventTypes.value = await listEntityEventTypes() } catch { /* silent */ }
}

async function loadRoles() {
  try {
    const data = await getRoles({})
    availableRoles.value = (data as any[])
      .filter((r: any) => r.roleCode !== 'SUPER_ADMIN')
      .map((r: any) => ({ roleCode: r.roleCode, roleName: r.roleName }))
  } catch { /* silent */ }
}

// ==================== Rule Wizard Actions ====================
function openRuleDrawer(existing?: MsgSubscriptionRule) {
  previewResult.value = null
  if (existing) {
    editingRule.value = existing
    ruleForm.value = { ...existing }
    if (existing.targetMode === 'BY_ROLE' && existing.targetConfig) {
      try { selectedRoleCodes.value = JSON.parse(existing.targetConfig) }
      catch { selectedRoleCodes.value = existing.targetConfig.split(',').map(s => s.trim()) }
    } else {
      selectedRoleCodes.value = []
    }
    if (existing.targetMode === 'BY_RELATION' && existing.targetConfig) {
      try {
        const cfg = JSON.parse(existing.targetConfig)
        relationConfig.value = {
          relation: cfg.relation || '',
          resource_type: cfg.resource_type || 'org_unit',
          direction: cfg.direction || 'inward'
        }
      } catch { /* ignore */ }
    }
    if (existing.targetMode === 'BY_FEATURE' && existing.targetConfig) {
      try {
        const cfg = JSON.parse(existing.targetConfig)
        selectedFeatures.value = cfg.features || []
      } catch { selectedFeatures.value = [] }
    } else {
      selectedFeatures.value = []
    }
    customTitle.value = ''
    customContent.value = ''
  } else {
    editingRule.value = null
    ruleForm.value = {
      ruleName: '', eventCategory: '', eventType: '',
      targetMode: 'BY_ROLE', targetConfig: '', channel: 'IN_APP',
      templateId: null, isEnabled: true,
    }
    selectedRoleCodes.value = []
    selectedFeatures.value = []
    relationConfig.value = { relation: '', resource_type: 'org_unit', direction: 'inward' }
    customTitle.value = ''
    customContent.value = ''
  }
  showRuleDrawer.value = true
}

async function previewCurrentRule() {
  const mode = ruleForm.value.targetMode
  if (!mode) { ElMessage.warning('请先选择通知对象模式'); return }
  let targetConfig = ruleForm.value.targetConfig || ''
  if (mode === 'BY_ROLE') {
    if (!selectedRoleCodes.value.length) { ElMessage.warning('请至少选择一个角色'); return }
    targetConfig = JSON.stringify(selectedRoleCodes.value)
  } else if (mode === 'BY_FEATURE') {
    if (!selectedFeatures.value.length) { ElMessage.warning('请至少选择一个能力'); return }
    targetConfig = JSON.stringify({ features: selectedFeatures.value })
  } else if (mode === 'BY_RELATION') {
    if (!relationConfig.value.relation) { ElMessage.warning('请选择关系'); return }
    targetConfig = JSON.stringify(relationConfig.value)
  }
  previewLoading.value = true
  try {
    previewResult.value = await msgConfigApi.previewRule({ targetMode: mode, targetConfig })
  } catch (e: any) {
    ElMessage.error('预览失败: ' + (e?.message || ''))
  } finally {
    previewLoading.value = false
  }
}

async function saveRule() {
  // Build targetConfig from wizard state (v3: 4 modes only)
  if (ruleForm.value.targetMode === 'BY_ROLE') {
    if (!selectedRoleCodes.value.length) { ElMessage.warning('请至少选择一个角色'); return }
    ruleForm.value.targetConfig = JSON.stringify(selectedRoleCodes.value)
  } else if (ruleForm.value.targetMode === 'BY_FEATURE') {
    if (!selectedFeatures.value.length) { ElMessage.warning('请至少选择一个能力'); return }
    ruleForm.value.targetConfig = JSON.stringify({ features: selectedFeatures.value })
  } else if (ruleForm.value.targetMode === 'BY_RELATION') {
    if (!relationConfig.value.relation) { ElMessage.warning('请选择关系'); return }
    ruleForm.value.targetConfig = JSON.stringify(relationConfig.value)
  } else {
    // BY_SUBJECT: 无需 targetConfig
    ruleForm.value.targetConfig = ''
  }

  // Auto-generate rule name if empty
  if (!ruleForm.value.ruleName?.trim()) {
    ruleForm.value.ruleName = autoRuleName.value
  }

  // If custom template content, create a template first
  let templateId = ruleForm.value.templateId
  if (!templateId && (customTitle.value.trim() || customContent.value.trim())) {
    try {
      const code = 'RULE_TPL_' + Date.now()
      const created = await msgConfigApi.createTemplate({
        templateCode: code,
        templateName: ruleForm.value.ruleName + ' 模板',
        titleTemplate: customTitle.value || '{{subjectName}} - {{eventLabel}}',
        contentTemplate: customContent.value || '',
        isEnabled: true,
      })
      templateId = created.id
      ruleForm.value.templateId = templateId
      await loadTemplates()
    } catch (e: any) {
      ElMessage.error('创建模板失败: ' + (e.message || ''))
      return
    }
  }

  try {
    if (editingRule.value?.id) {
      await msgConfigApi.updateRule(editingRule.value.id, ruleForm.value)
      ElMessage.success('更新成功')
    } else {
      await msgConfigApi.createRule(ruleForm.value)
      ElMessage.success('创建成功')
    }
    showRuleDrawer.value = false
    await loadRules()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}

async function toggleRule(rule: MsgSubscriptionRule) {
  try {
    await msgConfigApi.updateRule(rule.id, { isEnabled: !rule.isEnabled })
    ElMessage.success(rule.isEnabled ? '已禁用' : '已启用')
    await loadRules()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
}

async function handleDeleteRule(rule: MsgSubscriptionRule) {
  try {
    await ElMessageBox.confirm(`确定删除规则「${rule.ruleName}」？`, '确认删除', { type: 'warning' })
    await msgConfigApi.deleteRule(rule.id)
    ElMessage.success('已删除')
    await loadRules()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
}

// ==================== Template Actions ====================
function openTemplateDrawer(existing?: MsgTemplate) {
  if (existing) {
    editingTemplate.value = existing
    templateForm.value = { ...existing }
  } else {
    editingTemplate.value = null
    templateForm.value = {
      templateCode: '', templateName: '',
      titleTemplate: '', contentTemplate: '', isEnabled: true,
    }
  }
  showTemplateDrawer.value = true
}

async function saveTemplate() {
  if (!templateForm.value.templateName?.trim()) { ElMessage.warning('请填写模板名称'); return }
  if (!templateForm.value.titleTemplate?.trim()) { ElMessage.warning('请填写标题模板'); return }
  try {
    if (editingTemplate.value?.id) {
      await msgConfigApi.updateTemplate(editingTemplate.value.id, templateForm.value)
      ElMessage.success('更新成功')
    } else {
      await msgConfigApi.createTemplate(templateForm.value)
      ElMessage.success('创建成功')
    }
    showTemplateDrawer.value = false
    await loadTemplates()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}

async function handleDeleteTemplate(tpl: MsgTemplate) {
  try {
    await ElMessageBox.confirm(`确定删除模板「${tpl.templateName}」？`, '确认删除', { type: 'warning' })
    await msgConfigApi.deleteTemplate(tpl.id)
    ElMessage.success('已删除')
    await loadTemplates()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
}

// ==================== Messaging Health (M5.2) ====================
const messagingHealth = ref<{ healthy: boolean; missingTables: string[] }>({ healthy: true, missingTables: [] })
async function loadMessagingHealth() {
  try {
    const data: any = await http.get('/plugin-platform/messaging-health')
    messagingHealth.value = {
      healthy: Boolean(data?.healthy),
      missingTables: Array.isArray(data?.missingTables) ? data.missingTables : [],
    }
  } catch {
    // 健康接口失败默认认为健康,避免白屏
    messagingHealth.value = { healthy: true, missingTables: [] }
  }
}

// ==================== Lifecycle ====================
onMounted(() => {
  loadRules(); loadTemplates(); loadEventTypes(); loadRoles(); loadMessagingHealth()
})
</script>

<style scoped>
/* ============================================
   Message Config View — EventTriggerView Style
   ============================================ */
.cfg-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--insp-bg-page);
  font-family: var(--insp-font-body);
}

/* Header (token 化) */
.cfg-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--insp-sp-4);
  padding: var(--insp-sp-3) var(--insp-sp-4);
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  margin: var(--insp-sp-3) var(--insp-sp-4) var(--insp-sp-3);
}
.cfg-head__lead { display: flex; flex-direction: column; gap: 2px; }
.cfg-title {
  font-family: var(--insp-font-display);
  font-size: var(--insp-text-h1);
  font-weight: var(--insp-fw-bold);
  color: var(--insp-ink-primary);
  letter-spacing: var(--insp-tracking-tight);
  margin: 2px 0 0;
  line-height: var(--insp-leading-tight);
}
.stats-row {
  display: flex;
  align-items: center;
  gap: var(--insp-sp-2);
  margin-top: var(--insp-sp-1);
}
.stat { font-size: var(--insp-text-sm); color: var(--insp-ink-tertiary); }
.stat b { font-weight: var(--insp-fw-semibold); color: var(--insp-ink-primary); }
.stat-sep {
  display: block;
  width: 1px; height: 10px;
  background: var(--insp-border-strong);
}

/* M5.2: 健康 banner (token 化) */
.health-alert {
  display: flex;
  align-items: center;
  gap: var(--insp-sp-2);
  margin: 0 var(--insp-sp-4) var(--insp-sp-3);
  padding: var(--insp-sp-3);
  background: var(--insp-fail-pale);
  border: 1px solid var(--insp-fail-border);
  border-radius: var(--insp-radius-md);
  color: var(--insp-fail);
  font-size: var(--insp-text-sm);
  line-height: var(--insp-leading-normal);
}
.health-alert b { font-weight: var(--insp-fw-semibold); }

/* Tab Bar */
.tab-bar {
  display: flex;
  gap: 0;
  padding: 0 var(--insp-sp-4);
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  margin: 0 var(--insp-sp-4) var(--insp-sp-3);
}
.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--insp-sp-1);
  padding: var(--insp-sp-2) var(--insp-sp-3);
  font-size: var(--insp-text-sm);
  font-weight: var(--insp-fw-medium);
  color: var(--insp-ink-tertiary);
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  font-family: inherit;
  transition: color var(--insp-t-fast), border-color var(--insp-t-fast);
}
.tab-btn:hover { color: var(--insp-ink-primary); }
.tab-btn.active {
  color: var(--insp-accent);
  border-bottom-color: var(--insp-accent);
  font-weight: var(--insp-fw-semibold);
}
.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 var(--insp-sp-1);
  font-size: 10px;
  font-weight: var(--insp-fw-semibold);
  background: var(--insp-bg-subtle);
  color: var(--insp-ink-tertiary);
  border-radius: var(--insp-radius-pill);
}
.tab-btn.active .tab-count {
  background: var(--insp-accent);
  color: #fff;
}

/* Table */
.table-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}
.cfg-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: separate;
  border-spacing: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}
.cfg-table th,
.cfg-table td {
  padding: 10px 12px;
  vertical-align: middle;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cfg-table th {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #6b7280;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  user-select: none;
}
.cfg-table td {
  font-size: 13px;
  color: #374151;
  border-bottom: 1px solid #f3f4f6;
}
.cfg-row { transition: background 0.1s; }
.cfg-row:hover { background: #f0f5ff; }
.cfg-row:last-child td { border-bottom: none; }
.text-right { text-align: right !important; }
.text-muted { color: #d1d5db; font-size: 12px; }

.cell-name { font-weight: 500; color: var(--insp-ink-primary); }
.cell-config { font-size: var(--insp-text-sm); color: var(--insp-ink-tertiary); }
.cell-dim { color: var(--insp-ink-quaternary); font-size: var(--insp-text-sm); }

/* S-4: 配置可视化 chip */
.cfg-chip {
  display: inline-flex; align-items: center;
  padding: 1px var(--insp-sp-2);
  margin-right: var(--insp-sp-1);
  font-size: var(--insp-text-xs);
  font-weight: var(--insp-fw-medium);
  border-radius: var(--insp-radius-sm);
  border: 1px solid;
  white-space: nowrap;
}
.cfg-chip--role {
  background: var(--insp-info-pale);
  color: var(--insp-info);
  border-color: var(--insp-info-border);
}
.cfg-chip--relation {
  background: var(--insp-pass-pale);
  color: var(--insp-pass);
  border-color: var(--insp-pass-border);
}
.cfg-chip--feature {
  background: var(--insp-warn-pale);
  color: var(--insp-warn);
  border-color: var(--insp-warn-border);
}
.cell-tpl { font-size: 12px; color: #6b7280; font-family: monospace; }
.cell-content { max-width: 0; }

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

.system-badge {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 5px;
  font-size: 10px;
  font-weight: 600;
  color: #6b7280;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 3px;
  vertical-align: middle;
}

.channel-chip {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}
.ch-IN_APP { background: var(--insp-info-pale); color: var(--insp-info); }
.ch-EMAIL  { background: var(--insp-pass-pale); color: var(--insp-pass); }
.ch-WECHAT { background: #fefce8; color: #a16207; }

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 4px;
  vertical-align: middle;
}
.dot-on { background: #10b981; }
.dot-off { background: #9ca3af; }

.empty-cell {
  text-align: center;
  padding: 40px 12px !important;
  color: #9ca3af;
  font-size: 13px;
}

/* Status Toggle */
.status-toggle {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px 3px 6px;
  border: 1px solid;
  border-radius: 99px;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  background: none;
  font-family: inherit;
  transition: all 0.15s;
}
.toggle-dot {
  display: block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  transition: background 0.15s;
}
.is-active { border-color: #bbf7d0; color: #15803d; }
.is-active .toggle-dot { background: #22c55e; }
.is-inactive { border-color: #e5e7eb; color: #9ca3af; }
.is-inactive .toggle-dot { background: #d1d5db; }

/* Action Buttons */
.action-btn {
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  background: none;
  border: 1px solid #e5e7eb;
  border-radius: 5px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
  margin-left: 4px;
}
.action-btn:first-child { margin-left: 0; }
.action-btn:hover { color: var(--insp-ink-primary); border-color: #9ca3af; background: #f9fafb; }
.action-danger:hover { color: #ef4444; border-color: #fca5a5; background: #fef2f2; }

/* Drawer */
.drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.3);
  z-index: 2000;
  display: flex;
  justify-content: flex-end;
}
.drawer-panel {
  width: 480px;
  max-width: 90vw;
  height: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
  box-shadow: -8px 0 30px rgba(0,0,0,0.12);
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
  color: var(--insp-ink-primary);
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
.drawer-close:hover { background: #e5e7eb; color: var(--insp-ink-primary); }

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
.field-hint {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
}

.field-input, .field-select, .field-textarea {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  font-size: 13px;
  font-family: inherit;
  color: var(--insp-ink-primary);
  background: #fafafa;
  outline: none;
  transition: border-color 0.15s;
}
.field-input:focus, .field-select:focus, .field-textarea:focus {
  border-color: #2563eb;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(37,99,235,0.08);
}
.field-select {
  appearance: none;
  background: #fafafa url("data:image/svg+xml,%3Csvg width='10' height='6' viewBox='0 0 10 6' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%239ca3af' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E") right 10px center no-repeat;
  padding-right: 28px;
  cursor: pointer;
}
.field-textarea {
  resize: vertical;
  min-height: 80px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
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

.var-hint {
  padding: 8px 10px;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
  border-radius: 6px;
  font-size: 11px;
  color: #64748b;
}
.var-hint code {
  display: inline-block;
  padding: 1px 5px;
  background: #e0f2fe;
  border-radius: 3px;
  font-size: 10px;
  color: #0369a1;
  margin: 0 2px;
}

/* Drawer footer */
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 24px;
  border-top: 1px solid #e8eaed;
  background: #fff;
}
.btn-cancel {
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.btn-cancel:hover { background: #f9fafb; }
.btn-save {
  height: var(--insp-h-md);
  padding: 0 var(--insp-sp-4);
  font-size: var(--insp-text-sm);
  font-weight: var(--insp-fw-medium);
  color: #fff;
  background: var(--insp-accent);
  border: 1px solid var(--insp-accent);
  border-radius: var(--insp-radius-md);
  cursor: pointer;
  font-family: inherit;
  transition: background var(--insp-t-fast);
}
.btn-save:hover { background: var(--insp-accent-hover); }
.btn-preview {
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.btn-preview:hover:not(:disabled) { background: #e5e7eb; }
.btn-preview:disabled { opacity: 0.6; cursor: not-allowed; }

.preview-panel {
  margin: 0 24px 12px;
  padding: 12px 14px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 8px;
  font-size: 12px;
}
.preview-panel.has-warn {
  background: #fffbeb;
  border-color: #fcd34d;
}
.preview-head { display: flex; justify-content: space-between; align-items: center; }
.preview-title { color: #64748b; }
.preview-count { color: #0369a1; font-size: 14px; }
.preview-panel.has-warn .preview-count { color: #b45309; }
.preview-warn { margin-top: 6px; color: #b45309; line-height: 1.5; }
.preview-samples { margin-top: 8px; color: #475569; line-height: 1.7; }
.preview-samples-label { color: #94a3b8; }
.preview-sample {
  display: inline-block;
  padding: 2px 8px;
  margin: 0 4px 2px 0;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  color: #334155;
}
.preview-more { color: #94a3b8; margin-left: 4px; }

/* Wizard step numbers */
.step-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: var(--insp-radius-pill);
  background: var(--insp-accent);
  color: #fff;
  font-size: 10px;
  font-weight: var(--insp-fw-bold);
  margin-right: var(--insp-sp-1);
}

/* Wizard preview text */
.wizard-preview {
  padding: 8px 12px;
  background: #f0f7ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  font-size: 12px;
  color: #1e40af;
  margin-top: 8px;
}

/* Role checkboxes */
.role-checks {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.role-check {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border: 1.5px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
  user-select: none;
}
.role-check input { display: none; }
.role-check:hover { border-color: #93c5fd; background: #f0f7ff; }
.role-check.checked { border-color: #2563eb; color: #1d4ed8; background: #eff6ff; }

/* Drawer transition */
.drawer-enter-active, .drawer-leave-active {
  transition: opacity 0.2s ease;
}
.drawer-enter-active .drawer-panel, .drawer-leave-active .drawer-panel {
  transition: transform 0.25s ease;
}
.drawer-enter-from, .drawer-leave-to { opacity: 0; }
.drawer-enter-from .drawer-panel { transform: translateX(100%); }
.drawer-leave-to .drawer-panel { transform: translateX(100%); }
</style>
