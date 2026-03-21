<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { msgConfigApi } from '@/api/message'
import type { MsgSubscriptionRule, MsgTemplate } from '@/types/message'
import { TargetModeConfig, ChannelConfig } from '@/types/message'

const router = useRouter()

// ==================== State ====================
const activeTab = ref<'rules' | 'templates'>('rules')

// Rules
const rules = ref<MsgSubscriptionRule[]>([])
const rulesLoading = ref(false)
const showRuleDialog = ref(false)
const editingRule = ref<Partial<MsgSubscriptionRule> | null>(null)
const ruleForm = ref<Partial<MsgSubscriptionRule>>({
  ruleName: '',
  eventCategory: '',
  eventType: '',
  targetMode: 'BY_ROLE',
  targetConfig: '',
  channel: 'IN_APP',
  templateId: null,
  isEnabled: true,
})

// Templates
const templates = ref<MsgTemplate[]>([])
const templatesLoading = ref(false)
const showTemplateDialog = ref(false)
const editingTemplate = ref<Partial<MsgTemplate> | null>(null)
const templateForm = ref<Partial<MsgTemplate>>({
  templateCode: '',
  templateName: '',
  titleTemplate: '',
  contentTemplate: '',
  isEnabled: true,
})

// Confirm
const showDeleteConfirm = ref(false)
const deleteTarget = ref<{ type: 'rule' | 'template'; id: number; name: string } | null>(null)

// Mask mousedown
const maskMouseDownTarget = ref<EventTarget | null>(null)

// ==================== Load ====================
async function loadRules() {
  rulesLoading.value = true
  try {
    rules.value = await msgConfigApi.getRules()
  } catch { /* silent */ } finally {
    rulesLoading.value = false
  }
}

async function loadTemplates() {
  templatesLoading.value = true
  try {
    templates.value = await msgConfigApi.getTemplates()
  } catch { /* silent */ } finally {
    templatesLoading.value = false
  }
}

// ==================== Rule Actions ====================
function openCreateRule() {
  editingRule.value = null
  ruleForm.value = {
    ruleName: '',
    eventCategory: '',
    eventType: '',
    targetMode: 'BY_ROLE',
    targetConfig: '',
    channel: 'IN_APP',
    templateId: null,
    isEnabled: true,
  }
  showRuleDialog.value = true
}

function openEditRule(rule: MsgSubscriptionRule) {
  editingRule.value = rule
  ruleForm.value = { ...rule }
  showRuleDialog.value = true
}

async function saveRule() {
  if (!ruleForm.value.ruleName?.trim()) return
  try {
    if (editingRule.value?.id) {
      const updated = await msgConfigApi.updateRule(editingRule.value.id, ruleForm.value)
      const idx = rules.value.findIndex(r => r.id === updated.id)
      if (idx >= 0) rules.value[idx] = updated
    } else {
      const created = await msgConfigApi.createRule(ruleForm.value)
      rules.value.push(created)
    }
    showRuleDialog.value = false
  } catch { /* silent */ }
}

async function toggleRule(rule: MsgSubscriptionRule) {
  try {
    const updated = await msgConfigApi.updateRule(rule.id, { isEnabled: !rule.isEnabled })
    const idx = rules.value.findIndex(r => r.id === updated.id)
    if (idx >= 0) rules.value[idx] = updated
  } catch { rule.isEnabled = !rule.isEnabled }
}

function confirmDeleteRule(rule: MsgSubscriptionRule) {
  deleteTarget.value = { type: 'rule', id: rule.id, name: rule.ruleName }
  showDeleteConfirm.value = true
}

// ==================== Template Actions ====================
function openCreateTemplate() {
  editingTemplate.value = null
  templateForm.value = {
    templateCode: '',
    templateName: '',
    titleTemplate: '',
    contentTemplate: '',
    isEnabled: true,
  }
  showTemplateDialog.value = true
}

function openEditTemplate(tpl: MsgTemplate) {
  if (tpl.isSystem) return
  editingTemplate.value = tpl
  templateForm.value = { ...tpl }
  showTemplateDialog.value = true
}

async function saveTemplate() {
  if (!templateForm.value.templateName?.trim() || !templateForm.value.titleTemplate?.trim()) return
  try {
    if (editingTemplate.value?.id) {
      const updated = await msgConfigApi.updateTemplate(editingTemplate.value.id, templateForm.value)
      const idx = templates.value.findIndex(t => t.id === updated.id)
      if (idx >= 0) templates.value[idx] = updated
    } else {
      const created = await msgConfigApi.createTemplate(templateForm.value)
      templates.value.push(created)
    }
    showTemplateDialog.value = false
  } catch { /* silent */ }
}

function confirmDeleteTemplate(tpl: MsgTemplate) {
  if (tpl.isSystem) return
  deleteTarget.value = { type: 'template', id: tpl.id, name: tpl.templateName }
  showDeleteConfirm.value = true
}

// ==================== Delete ====================
async function executeDelete() {
  if (!deleteTarget.value) return
  try {
    if (deleteTarget.value.type === 'rule') {
      await msgConfigApi.deleteRule(deleteTarget.value.id)
      rules.value = rules.value.filter(r => r.id !== deleteTarget.value!.id)
    } else {
      await msgConfigApi.deleteTemplate(deleteTarget.value.id)
      templates.value = templates.value.filter(t => t.id !== deleteTarget.value!.id)
    }
  } catch { /* silent */ } finally {
    showDeleteConfirm.value = false
    deleteTarget.value = null
  }
}

// ==================== Helpers ====================
function getTargetModeLabel(mode: MsgSubscriptionRule['targetMode']): string {
  return TargetModeConfig[mode]?.label ?? mode
}

function getChannelLabel(ch: MsgSubscriptionRule['channel']): string {
  return ChannelConfig[ch]?.label ?? ch
}

function getTemplateName(id: number | null): string {
  if (!id) return '-'
  return templates.value.find(t => t.id === id)?.templateName ?? String(id)
}

function onMaskMouseDown(e: MouseEvent) { maskMouseDownTarget.value = e.target }
function onMaskClick(e: MouseEvent, closeFn: () => void) {
  if (e.target === e.currentTarget && maskMouseDownTarget.value === e.currentTarget) closeFn()
  maskMouseDownTarget.value = null
}

// ==================== Lifecycle ====================
onMounted(() => {
  loadRules()
  loadTemplates()
})
</script>

<template>
  <div class="cfg">
    <!-- Header -->
    <div class="cfg-header">
      <div class="cfg-header-left">
        <button class="back-btn" @click="router.back()">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <h1 class="cfg-title">消息配置</h1>
      </div>
    </div>

    <!-- Tabs -->
    <div class="cfg-tabs">
      <button class="tab-btn" :class="{ active: activeTab === 'rules' }" @click="activeTab = 'rules'">
        订阅规则
      </button>
      <button class="tab-btn" :class="{ active: activeTab === 'templates' }" @click="activeTab = 'templates'">
        消息模板
      </button>
    </div>

    <!-- Content -->
    <div class="cfg-content">

      <!-- ==================== Rules Tab ==================== -->
      <div v-if="activeTab === 'rules'">
        <div v-if="rulesLoading" class="state-loading">
          <div class="spinner" /><span>加载中...</span>
        </div>

        <template v-else>
          <div v-if="rules.length === 0" class="state-empty">
            <p class="empty-title">暂无订阅规则</p>
            <p class="empty-sub">添加规则以自动通知相关用户</p>
          </div>

          <div v-else class="rule-list">
            <div v-for="rule in rules" :key="rule.id" class="rule-card">
              <div class="rule-card-head">
                <span class="rule-name">{{ rule.ruleName }}</span>
                <div class="rule-head-right">
                  <span class="toggle-label">{{ rule.isEnabled ? '启用' : '禁用' }}</span>
                  <button
                    class="toggle-btn"
                    :class="{ on: rule.isEnabled }"
                    @click="toggleRule(rule)"
                    :title="rule.isEnabled ? '点击禁用' : '点击启用'"
                  >
                    <span class="toggle-knob" />
                  </button>
                </div>
              </div>
              <div class="rule-meta">
                <span v-if="rule.eventCategory || rule.eventType" class="meta-seg">
                  事件: {{ [rule.eventCategory, rule.eventType].filter(Boolean).join('·') }}
                </span>
                <span class="meta-arrow">→</span>
                <span class="meta-seg">通知: {{ getTargetModeLabel(rule.targetMode) }}</span>
                <template v-if="rule.targetConfig">
                  <span class="meta-bracket">({{ rule.targetConfig }})</span>
                </template>
              </div>
              <div class="rule-footer">
                <div class="rule-tags">
                  <span class="tag tag-channel">{{ getChannelLabel(rule.channel) }}</span>
                  <span v-if="rule.templateId" class="tag tag-template">模板: {{ getTemplateName(rule.templateId) }}</span>
                </div>
                <div class="rule-actions">
                  <button class="act-btn" @click="openEditRule(rule)">编辑</button>
                  <button class="act-btn danger" @click="confirmDeleteRule(rule)">删除</button>
                </div>
              </div>
            </div>
          </div>

          <button class="btn-add" @click="openCreateRule">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="14" height="14">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            添加规则
          </button>
        </template>
      </div>

      <!-- ==================== Templates Tab ==================== -->
      <div v-if="activeTab === 'templates'">
        <div v-if="templatesLoading" class="state-loading">
          <div class="spinner" /><span>加载中...</span>
        </div>

        <template v-else>
          <div v-if="templates.length === 0" class="state-empty">
            <p class="empty-title">暂无消息模板</p>
            <p class="empty-sub">添加模板以定制消息格式</p>
          </div>

          <div v-else class="tpl-list">
            <div v-for="tpl in templates" :key="tpl.id" class="tpl-card" :class="{ system: tpl.isSystem }">
              <div class="tpl-card-head">
                <span class="tpl-name">{{ tpl.templateName }}</span>
                <div class="tpl-badges">
                  <span v-if="tpl.isSystem" class="badge badge-system">系统</span>
                  <span class="badge" :class="tpl.isEnabled ? 'badge-on' : 'badge-off'">
                    {{ tpl.isEnabled ? '启用' : '禁用' }}
                  </span>
                </div>
              </div>
              <div class="tpl-row">
                <span class="tpl-field-label">标题:</span>
                <span class="tpl-field-val">{{ tpl.titleTemplate }}</span>
              </div>
              <div v-if="tpl.contentTemplate" class="tpl-row">
                <span class="tpl-field-label">内容:</span>
                <span class="tpl-field-val tpl-content-preview">{{ tpl.contentTemplate }}</span>
              </div>
              <div v-if="!tpl.isSystem" class="tpl-actions">
                <button class="act-btn" @click="openEditTemplate(tpl)">编辑</button>
                <button class="act-btn danger" @click="confirmDeleteTemplate(tpl)">删除</button>
              </div>
            </div>
          </div>

          <button class="btn-add" @click="openCreateTemplate">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="14" height="14">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            添加模板
          </button>
        </template>
      </div>
    </div>

    <!-- ==================== Rule Dialog ==================== -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="showRuleDialog"
          class="modal-mask"
          @mousedown="onMaskMouseDown"
          @click="onMaskClick($event, () => showRuleDialog = false)"
        >
          <div class="modal-box">
            <div class="modal-head">
              <h3>{{ editingRule ? '编辑订阅规则' : '新建订阅规则' }}</h3>
              <button class="modal-close" @click="showRuleDialog = false">&times;</button>
            </div>
            <div class="modal-body">
              <div class="fld">
                <label>规则名称 <span class="req">*</span></label>
                <input v-model="ruleForm.ruleName" placeholder="如：违规通知" />
              </div>
              <div class="fld-row">
                <div class="fld">
                  <label>事件类别</label>
                  <input v-model="ruleForm.eventCategory" placeholder="如：检查" />
                </div>
                <div class="fld">
                  <label>事件类型</label>
                  <input v-model="ruleForm.eventType" placeholder="如：违规记录" />
                </div>
              </div>
              <div class="fld">
                <label>通知对象</label>
                <select v-model="ruleForm.targetMode">
                  <option v-for="(cfg, key) in TargetModeConfig" :key="key" :value="key">
                    {{ cfg.label }} — {{ cfg.description }}
                  </option>
                </select>
              </div>
              <div class="fld">
                <label>对象配置</label>
                <input v-model="ruleForm.targetConfig" placeholder="如：班主任（按角色时填角色名）" />
              </div>
              <div class="fld">
                <label>通知渠道</label>
                <select v-model="ruleForm.channel">
                  <option v-for="(cfg, key) in ChannelConfig" :key="key" :value="key">
                    {{ cfg.label }}
                  </option>
                </select>
              </div>
              <div class="fld">
                <label>消息模板</label>
                <select v-model="ruleForm.templateId">
                  <option :value="null">（不指定）</option>
                  <option v-for="tpl in templates" :key="tpl.id" :value="tpl.id">
                    {{ tpl.templateName }}
                  </option>
                </select>
              </div>
              <div class="fld fld-toggle">
                <label>启用状态</label>
                <button
                  class="toggle-btn"
                  :class="{ on: ruleForm.isEnabled }"
                  @click="ruleForm.isEnabled = !ruleForm.isEnabled"
                >
                  <span class="toggle-knob" />
                </button>
              </div>
            </div>
            <div class="modal-foot">
              <button class="btn-ghost" @click="showRuleDialog = false">取消</button>
              <button class="btn-primary" @click="saveRule">保存</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ==================== Template Dialog ==================== -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="showTemplateDialog"
          class="modal-mask"
          @mousedown="onMaskMouseDown"
          @click="onMaskClick($event, () => showTemplateDialog = false)"
        >
          <div class="modal-box">
            <div class="modal-head">
              <h3>{{ editingTemplate ? '编辑消息模板' : '新建消息模板' }}</h3>
              <button class="modal-close" @click="showTemplateDialog = false">&times;</button>
            </div>
            <div class="modal-body">
              <div class="fld-row">
                <div class="fld">
                  <label>模板编码</label>
                  <input v-model="templateForm.templateCode" placeholder="如：violation_notify" />
                </div>
                <div class="fld">
                  <label>模板名称 <span class="req">*</span></label>
                  <input v-model="templateForm.templateName" placeholder="如：违规通知" />
                </div>
              </div>
              <div class="fld">
                <label>标题模板 <span class="req">*</span></label>
                <input v-model="templateForm.titleTemplate" placeholder="如：{{subjectName}} 发生违规" />
              </div>
              <div class="fld">
                <label>内容模板</label>
                <textarea v-model="templateForm.contentTemplate" rows="4" placeholder="如：{{subjectName}} 于 {{time}} 被记录违规：{{reason}}" />
              </div>
              <div class="fld hint-text">
                <span>支持变量：&#123;&#123;subjectName&#125;&#125;、&#123;&#123;time&#125;&#125;、&#123;&#123;reason&#125;&#125; 等</span>
              </div>
            </div>
            <div class="modal-foot">
              <button class="btn-ghost" @click="showTemplateDialog = false">取消</button>
              <button class="btn-primary" @click="saveTemplate">保存</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ==================== Delete Confirm ==================== -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="showDeleteConfirm"
          class="modal-mask"
          @mousedown="onMaskMouseDown"
          @click="onMaskClick($event, () => showDeleteConfirm = false)"
        >
          <div class="modal-box modal-box-sm">
            <div class="modal-head">
              <h3>确认删除</h3>
              <button class="modal-close" @click="showDeleteConfirm = false">&times;</button>
            </div>
            <div class="modal-body">
              <p class="confirm-text">确认删除「{{ deleteTarget?.name }}」？此操作不可恢复。</p>
            </div>
            <div class="modal-foot">
              <button class="btn-ghost" @click="showDeleteConfirm = false">取消</button>
              <button class="btn-danger" @click="executeDelete">删除</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.cfg {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* Header */
.cfg-header {
  display: flex;
  align-items: center;
  padding: 16px 24px 14px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
}
.cfg-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: none;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  color: #6b7685;
  cursor: pointer;
  transition: background 0.15s;
}
.back-btn:hover { background: #f4f6f9; color: #1a6dff; }
.cfg-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0;
}

/* Tabs */
.cfg-tabs {
  display: flex;
  align-items: center;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
  gap: 0;
}
.tab-btn {
  padding: 12px 20px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  font-size: 13px;
  font-weight: 500;
  color: #6b7685;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
  margin-bottom: -1px;
}
.tab-btn:hover { color: #1a6dff; }
.tab-btn.active {
  color: #1a6dff;
  border-bottom-color: #1a6dff;
}

/* Content */
.cfg-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

/* Rule card */
.rule-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}
.rule-card {
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 8px;
  padding: 14px 16px;
}
.rule-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.rule-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e2a3a;
}
.rule-head-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.toggle-label {
  font-size: 12px;
  color: #8c95a3;
}
.rule-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.meta-seg {
  font-size: 12px;
  color: #5a6474;
}
.meta-arrow {
  font-size: 12px;
  color: #b8c0cc;
}
.meta-bracket {
  font-size: 12px;
  color: #8c95a3;
}
.rule-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.rule-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.tag {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
}
.tag-channel { background: #e8f0ff; color: #2563eb; }
.tag-template { background: #ecfdf5; color: #059669; }
.rule-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

/* Template card */
.tpl-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}
.tpl-card {
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 8px;
  padding: 14px 16px;
}
.tpl-card.system {
  border-left: 3px solid #10b981;
}
.tpl-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.tpl-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e2a3a;
}
.tpl-badges {
  display: flex;
  gap: 6px;
}
.badge {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
}
.badge-system { background: #ecfdf5; color: #059669; }
.badge-on { background: #e8f0ff; color: #2563eb; }
.badge-off { background: #f4f6f9; color: #8c95a3; }
.tpl-row {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
  font-size: 12px;
  line-height: 1.5;
}
.tpl-field-label {
  color: #8c95a3;
  flex-shrink: 0;
}
.tpl-field-val {
  color: #3d4757;
}
.tpl-content-preview {
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  color: #6b7685;
}
.tpl-actions {
  display: flex;
  gap: 6px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f2f5;
}

/* Buttons */
.act-btn {
  padding: 4px 12px;
  background: none;
  border: 1px solid #dce1e8;
  border-radius: 5px;
  font-size: 12px;
  color: #5a6474;
  cursor: pointer;
  transition: background 0.12s, color 0.12s, border-color 0.12s;
}
.act-btn:hover { background: #f4f6f9; color: #1a6dff; border-color: #c8d0db; }
.act-btn.danger { color: #d93025; border-color: #fecaca; }
.act-btn.danger:hover { background: #fef2f2; color: #b91c1c; border-color: #f87171; }

.btn-add {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: none;
  border: 1px dashed #c8d0db;
  border-radius: 8px;
  font-size: 13px;
  color: #6b7685;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
  width: 100%;
  justify-content: center;
}
.btn-add:hover { background: #f4f6f9; color: #1a6dff; border-color: #7aadff; }

/* Toggle */
.toggle-btn {
  position: relative;
  width: 36px;
  height: 20px;
  background: #dce1e8;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
  padding: 0;
  flex-shrink: 0;
}
.toggle-btn.on { background: #1a6dff; }
.toggle-knob {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  background: #fff;
  border-radius: 50%;
  transition: transform 0.2s;
  display: block;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}
.toggle-btn.on .toggle-knob { transform: translateX(16px); }

/* States */
.state-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 60px 0;
  color: #b8c0cc;
  font-size: 13px;
}
.state-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0;
  gap: 6px;
}
.empty-title {
  font-size: 14px;
  font-weight: 600;
  color: #6b7685;
  margin: 0;
}
.empty-sub {
  font-size: 12px;
  color: #b8c0cc;
  margin: 0;
}
.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e8ecf0;
  border-top-color: #1a6dff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Modal */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(2px);
}
.modal-box {
  width: 500px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}
.modal-box-sm { width: 380px; }
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}
.modal-head h3 {
  font-size: 15px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0;
}
.modal-close {
  background: none;
  border: none;
  font-size: 22px;
  color: #b8c0cc;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
}
.modal-close:hover { color: #5a6474; }
.modal-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px 24px;
}
.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 24px 20px;
}

.fld { display: flex; flex-direction: column; gap: 4px; }
.fld label {
  font-size: 12px;
  font-weight: 500;
  color: #5a6474;
}
.fld input, .fld textarea, .fld select {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  padding: 7px 10px;
  font-size: 13px;
  outline: none;
  color: #1e2a3a;
  background: #fff;
  transition: border-color 0.2s, box-shadow 0.2s;
  font-family: inherit;
  resize: vertical;
}
.fld input::placeholder, .fld textarea::placeholder { color: #b8c0cc; }
.fld input:focus, .fld textarea:focus, .fld select:focus {
  border-color: #7aadff;
  box-shadow: 0 0 0 3px rgba(26, 109, 255, 0.08);
}
.fld-row { display: flex; gap: 12px; }
.fld-row .fld { flex: 1; }
.fld-toggle {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}
.hint-text {
  font-size: 11px;
  color: #8c95a3;
}
.confirm-text {
  font-size: 13px;
  color: #3d4757;
  margin: 0;
  line-height: 1.6;
}
.req { color: #d93025; }

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 20px;
  background: #1a6dff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-primary:hover { background: #1558d6; }
.btn-ghost {
  padding: 8px 20px;
  background: none;
  border: 1px solid #dce1e8;
  border-radius: 8px;
  font-size: 13px;
  color: #5a6474;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-ghost:hover { background: #f4f6f9; }
.btn-danger {
  padding: 8px 20px;
  background: #ef4444;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-danger:hover { background: #dc2626; }

/* Modal transitions */
.modal-enter-active { transition: all 0.2s ease-out; }
.modal-leave-active { transition: all 0.15s ease-in; }
.modal-enter-from { opacity: 0; }
.modal-enter-from .modal-box { transform: translateY(12px) scale(0.97); }
.modal-leave-to { opacity: 0; }
.modal-leave-to .modal-box { transform: translateY(-8px) scale(0.98); }
</style>
