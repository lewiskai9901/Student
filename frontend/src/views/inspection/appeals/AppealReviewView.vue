<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as appealApi from '@/api/inspection/appeal'
import type { InspAppeal } from '@/types/insp/appeal'

const loading = ref(false)
const submitting = ref(false)
const appeals = ref<InspAppeal[]>([])
const selectedId = ref<LongId | null>(null)

const selected = computed(() => appeals.value.find(a => a.id === selectedId.value) ?? null)

// Forms
const approveForm = ref({ comment: '', finalAdjustment: undefined as number | undefined })
const rejectComment = ref('')
const showRejectInline = ref(false)

async function loadData() {
  loading.value = true
  try {
    appeals.value = await appealApi.getPendingAppeals()
    // auto-select first if none chosen
    if (!selectedId.value && appeals.value.length > 0) {
      selectFirst()
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function selectFirst() {
  if (appeals.value.length) {
    selectedId.value = appeals.value[0].id
  } else {
    selectedId.value = null
  }
}

watch(selected, (a) => {
  if (a) {
    approveForm.value = { comment: '', finalAdjustment: a.expectedAdjustment ?? undefined }
    rejectComment.value = ''
    showRejectInline.value = false
  }
})

function fmtTime(s?: string) {
  if (!s) return '—'
  const d = new Date(s)
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

async function submitApprove() {
  if (!selected.value) return
  submitting.value = true
  try {
    await appealApi.approveAppeal(selected.value.id, {
      comment: approveForm.value.comment,
      finalAdjustment: approveForm.value.finalAdjustment,
    })
    ElMessage.success('已通过, 扣分已自动调整')
    moveToNext()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function submitReject() {
  if (!selected.value) return
  if (!rejectComment.value.trim()) {
    ElMessage.warning('驳回必须填写理由')
    return
  }
  submitting.value = true
  try {
    await appealApi.rejectAppeal(selected.value.id, { comment: rejectComment.value })
    ElMessage.success('已驳回')
    moveToNext()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

function moveToNext() {
  const remaining = appeals.value.filter(a => a.id !== selectedId.value)
  appeals.value = remaining
  if (remaining.length) {
    selectedId.value = remaining[0].id
  } else {
    selectedId.value = null
    loadData()
  }
}

// ── Keyboard hotkeys ──
function onKeyDown(e: KeyboardEvent) {
  if ((e.target as HTMLElement)?.tagName === 'INPUT' || (e.target as HTMLElement)?.tagName === 'TEXTAREA') return
  if (e.metaKey || e.ctrlKey || e.altKey) return
  const idx = appeals.value.findIndex(a => a.id === selectedId.value)
  switch (e.key) {
    case 'j':
    case 'ArrowDown':
      if (idx < appeals.value.length - 1) selectedId.value = appeals.value[idx + 1].id
      e.preventDefault()
      break
    case 'k':
    case 'ArrowUp':
      if (idx > 0) selectedId.value = appeals.value[idx - 1].id
      e.preventDefault()
      break
    case 'a':
      if (selected.value) submitApprove()
      e.preventDefault()
      break
    case 'r':
      showRejectInline.value = true
      e.preventDefault()
      break
    case 'Escape':
      showRejectInline.value = false
      break
  }
}

onMounted(() => {
  loadData()
  window.addEventListener('keydown', onKeyDown)
})
onUnmounted(() => window.removeEventListener('keydown', onKeyDown))
</script>

<template>
  <div class="insp-shell appeal-review">
    <!-- ── Top operator bar ─────────────────────────────── -->
    <header class="op-bar">
      <div class="op-bar__lead">
        <span class="insp-stamp">审核台 · 待审 {{ appeals.length }}</span>
        <h1 class="op-title">申诉审核工作台</h1>
      </div>
      <div class="op-bar__right">
        <div class="kbd-tray">
          <span class="kbd-pair"><kbd class="insp-kbd">J</kbd><kbd class="insp-kbd">K</kbd> 切换</span>
          <span class="kbd-pair"><kbd class="insp-kbd insp-kbd--inverted">A</kbd> 通过</span>
          <span class="kbd-pair"><kbd class="insp-kbd">R</kbd> 驳回</span>
        </div>
        <button class="insp-btn insp-btn--ghost" @click="loadData">刷新</button>
      </div>
    </header>

    <!-- ── Two-pane layout ─────────────────────────────── -->
    <div class="pane">
      <!-- Left: queue ─────────── -->
      <aside class="queue" v-loading="loading">
        <div class="queue-meta">
          <span class="insp-caps">待审清单</span>
          <span class="queue-meta__count insp-num">{{ appeals.length }}</span>
        </div>
        <ol class="queue-list">
          <li
            v-for="(a, i) in appeals" :key="a.id"
            class="queue-item"
            :class="{ 'is-selected': selectedId === a.id }"
            @click="selectedId = a.id"
          >
            <span class="queue-item__idx insp-num">{{ String(i + 1).padStart(2, '0') }}</span>
            <div class="queue-item__body">
              <div class="queue-item__code">{{ a.appealCode }}</div>
              <div class="queue-item__reason">{{ a.reason }}</div>
              <div class="queue-item__foot">
                <span class="queue-item__user">{{ a.submitterName || `用户#${a.submitterUserId}` }}</span>
                <span class="queue-item__sep">·</span>
                <span class="queue-item__time">{{ fmtTime(a.createdAt) }}</span>
              </div>
            </div>
            <span v-if="a.expectedAdjustment != null" class="queue-item__exp insp-num">
              +{{ Number(a.expectedAdjustment).toFixed(1) }}
            </span>
          </li>
          <li v-if="!loading && appeals.length === 0" class="queue-empty">
            <div class="insp-stamp">无待审</div>
          </li>
        </ol>
      </aside>

      <!-- Right: detail + verdict ─────────── -->
      <main class="detail">
        <template v-if="selected">
          <!-- Detail header -->
          <div class="detail-head">
            <div class="detail-head__lead">
              <div class="insp-eyebrow">案件 · 当前正在审理</div>
              <h2 class="detail-code">{{ selected.appealCode }}</h2>
              <div class="detail-meta">
                <span>{{ selected.submitterName || `用户#${selected.submitterUserId}` }} 提交</span>
                <span class="dot">·</span>
                <span>{{ fmtTime(selected.createdAt) }}</span>
                <span v-if="selected.taskId" class="dot">·</span>
                <span v-if="selected.taskId" class="insp-num">任务#{{ selected.taskId }}</span>
              </div>
            </div>
            <div class="detail-head__right">
              <div v-if="selected.expectedAdjustment != null" class="expected-tag">
                <span class="expected-tag__label">期望调整</span>
                <span class="expected-tag__value insp-num">+{{ Number(selected.expectedAdjustment).toFixed(1) }}</span>
              </div>
            </div>
          </div>

          <hr class="insp-rule" />

          <!-- Reason block — typographic emphasis -->
          <section class="reason-block">
            <div class="insp-caps reason-block__label">申诉理由</div>
            <p class="reason-block__text">{{ selected.reason }}</p>
          </section>

          <!-- Optional attachments -->
          <section v-if="selected.attachments" class="attachments-block">
            <div class="insp-caps">证据附件</div>
            <p class="attachments-text">{{ selected.attachments }}</p>
          </section>

          <hr class="insp-rule insp-rule--strong" />

          <!-- Verdict panel — the action focal point -->
          <section class="verdict">
            <div class="verdict-head">
              <span class="insp-eyebrow">裁决</span>
              <span class="verdict-hint">
                <kbd class="insp-kbd insp-kbd--inverted">A</kbd> 通过 · <kbd class="insp-kbd">R</kbd> 驳回
              </span>
            </div>

            <!-- Approve form (always visible inline) -->
            <div class="verdict-form" v-if="!showRejectInline">
              <label class="verdict-field">
                <span class="insp-caps">实际调整</span>
                <input
                  type="number" step="0.5"
                  v-model.number="approveForm.finalAdjustment"
                  class="adj-input insp-num"
                />
                <span class="verdict-field__hint">
                  扣分按此值调整 · DEDUCTION 退回扣分 · 其它直接覆盖
                </span>
              </label>

              <label class="verdict-field">
                <span class="insp-caps">审核备注</span>
                <textarea
                  v-model="approveForm.comment" rows="2"
                  class="comment-input"
                  placeholder="可选"
                />
              </label>

              <div class="verdict-actions">
                <button class="insp-btn" @click="showRejectInline = true">
                  <kbd class="insp-kbd">R</kbd> 改为驳回
                </button>
                <button
                  class="insp-btn insp-btn--accent"
                  :disabled="submitting" @click="submitApprove"
                >
                  <kbd class="insp-kbd insp-kbd--inverted">A</kbd> 通过申诉
                </button>
              </div>
            </div>

            <!-- Reject form (inline panel) -->
            <div v-else class="verdict-form verdict-form--reject">
              <div class="reject-head">
                <span class="insp-stamp">驳回</span>
                <span class="reject-warn">驳回必须填写明确理由让申诉人理解</span>
              </div>
              <textarea
                v-model="rejectComment" rows="4"
                class="comment-input comment-input--lg"
                placeholder="例: 监控录像已查证, 当时教室地面确有积水未及时清理"
                autofocus
              />
              <div class="verdict-actions">
                <button class="insp-btn" @click="showRejectInline = false">取消</button>
                <button
                  class="insp-btn insp-btn--primary"
                  :disabled="submitting" @click="submitReject"
                >确认驳回</button>
              </div>
            </div>
          </section>
        </template>

        <!-- Empty state when no selection -->
        <div v-else class="detail-empty">
          <div class="insp-stamp">无待审</div>
          <p class="detail-empty__hint">
            待审清单为空 · 全部申诉都已处理
          </p>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.appeal-review {
  height: calc(100vh - 0px);
  display: flex;
  flex-direction: column;
  background: var(--insp-bg-page);
}

/* ─ Operator bar ─────── */
.op-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--insp-sp-5);
  padding: var(--insp-sp-4) var(--insp-sp-7);
  background: var(--insp-bg-surface);
  border-bottom: 1px solid var(--insp-border-default);
  flex-shrink: 0;
}

.op-bar__lead {
  display: flex;
  align-items: center;
  gap: var(--insp-sp-5);
}

.op-title {
  font-family: var(--insp-font-display);
  font-size: 22px;
  font-weight: 500;
  margin: 0;
  letter-spacing: var(--insp-tracking-display);
  color: var(--insp-ink-primary);
}

.op-bar__right {
  display: flex;
  align-items: center;
  gap: var(--insp-sp-5);
}

.kbd-tray {
  display: flex;
  align-items: center;
  gap: var(--insp-sp-4);
  padding-right: var(--insp-sp-4);
  border-right: 1px solid var(--insp-border-subtle);
}

.kbd-pair {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-tertiary);
}

/* ─ Two-pane ─────── */
.pane {
  display: grid;
  grid-template-columns: 380px 1fr;
  flex: 1;
  min-height: 0;
}

/* ─ Queue ─────── */
.queue {
  border-right: 1px solid var(--insp-border-subtle);
  background: var(--insp-bg-surface);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.queue-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: var(--insp-sp-4) var(--insp-sp-5) var(--insp-sp-3);
  border-bottom: 1px solid var(--insp-border-subtle);
}

.queue-meta__count {
  font-size: var(--insp-text-md);
  font-weight: 600;
  color: var(--insp-accent);
}

.queue-list {
  flex: 1;
  overflow-y: auto;
  list-style: none;
  margin: 0;
  padding: 0;
}

.queue-item {
  display: grid;
  grid-template-columns: 40px 1fr auto;
  gap: var(--insp-sp-3);
  align-items: start;
  padding: var(--insp-sp-4) var(--insp-sp-5);
  border-bottom: 1px solid var(--insp-border-subtle);
  cursor: pointer;
  transition: background var(--insp-t-fast);
  position: relative;
}

.queue-item:hover { background: var(--insp-bg-subtle); }

.queue-item.is-selected {
  background: var(--insp-bg-subtle);
}

.queue-item.is-selected::before {
  content: '';
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 3px;
  background: var(--insp-accent);
}

.queue-item__idx {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs);
  font-weight: 500;
  color: var(--insp-ink-quaternary);
  padding-top: 2px;
}

.queue-item__body {
  min-width: 0;
}

.queue-item__code {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-sm);
  font-weight: 600;
  color: var(--insp-ink-primary);
}

.queue-item__reason {
  margin-top: 4px;
  font-size: var(--insp-text-sm);
  line-height: var(--insp-leading-snug);
  color: var(--insp-ink-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.queue-item__foot {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-tertiary);
}

.queue-item__sep { color: var(--insp-ink-quaternary); }

.queue-item__time {
  font-family: var(--insp-font-mono);
}

.queue-item__exp {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-md);
  font-weight: 600;
  color: var(--insp-pass);
  padding-top: 2px;
}

.queue-empty {
  padding: var(--insp-sp-8) var(--insp-sp-5);
  text-align: center;
}

/* ─ Detail pane ─────── */
.detail {
  overflow-y: auto;
  padding: var(--insp-sp-7) var(--insp-sp-9);
  max-width: 880px;
  width: 100%;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--insp-sp-5);
  margin-bottom: var(--insp-sp-5);
}

.detail-code {
  font-family: var(--insp-font-display);
  font-size: 36px;
  font-weight: 500;
  letter-spacing: var(--insp-tracking-display);
  margin: 4px 0 var(--insp-sp-2);
  color: var(--insp-ink-primary);
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-secondary);
}

.detail-meta .dot {
  color: var(--insp-ink-quaternary);
}

.expected-tag {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  padding: var(--insp-sp-3) var(--insp-sp-4);
  border: 1px solid var(--insp-pass);
  background: var(--insp-pass-pale);
  border-radius: 0;
}

.expected-tag__label {
  font-size: var(--insp-text-2xs);
  font-weight: 600;
  letter-spacing: var(--insp-tracking-caps);
  text-transform: uppercase;
  color: var(--insp-pass);
}

.expected-tag__value {
  font-family: var(--insp-font-mono);
  font-size: 28px;
  font-weight: 600;
  color: var(--insp-pass);
  letter-spacing: -0.02em;
  line-height: 1;
}

/* ─ Reason block ─────── */
.reason-block {
  padding: var(--insp-sp-6) 0;
}

.reason-block__label {
  margin-bottom: var(--insp-sp-3);
}

.reason-block__text {
  font-family: var(--insp-font-display);
  font-size: 22px;
  font-weight: 400;
  line-height: var(--insp-leading-relaxed);
  color: var(--insp-ink-primary);
  letter-spacing: var(--insp-tracking-tight);
  margin: 0;
}

.attachments-block {
  padding: 0 0 var(--insp-sp-6);
}

.attachments-text {
  margin-top: var(--insp-sp-2);
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-secondary);
  word-break: break-all;
}

/* ─ Verdict panel ─────── */
.verdict {
  margin-top: var(--insp-sp-6);
}

.verdict-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: var(--insp-sp-4);
}

.verdict-hint {
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-tertiary);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.verdict-form {
  display: flex;
  flex-direction: column;
  gap: var(--insp-sp-5);
}

.verdict-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.verdict-field__hint {
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-tertiary);
}

.adj-input {
  width: 200px;
  padding: 10px 14px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  font-family: var(--insp-font-mono);
  font-size: 22px;
  font-weight: 600;
  color: var(--insp-ink-primary);
  background: var(--insp-bg-surface);
  transition: border-color var(--insp-t-fast);
}

.adj-input:focus {
  outline: none;
  border-color: var(--insp-accent);
  box-shadow: 0 0 0 3px var(--insp-accent-paler);
}

.comment-input {
  padding: 10px 14px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  font-family: inherit;
  font-size: var(--insp-text-md);
  line-height: var(--insp-leading-snug);
  color: var(--insp-ink-primary);
  background: var(--insp-bg-surface);
  resize: vertical;
  transition: border-color var(--insp-t-fast);
}

.comment-input:focus {
  outline: none;
  border-color: var(--insp-accent);
  box-shadow: 0 0 0 3px var(--insp-accent-paler);
}

.comment-input--lg {
  font-size: var(--insp-text-md);
}

.verdict-form--reject {
  padding: var(--insp-sp-5);
  background: var(--insp-fail-pale);
  border: 1px solid var(--insp-fail);
  border-radius: var(--insp-radius-md);
}

.reject-head {
  display: flex;
  align-items: center;
  gap: var(--insp-sp-3);
}

.reject-warn {
  font-size: var(--insp-text-sm);
  color: var(--insp-fail);
  font-weight: 500;
}

.verdict-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--insp-sp-3);
  padding-top: var(--insp-sp-2);
}

.verdict-actions .insp-btn .insp-kbd {
  margin-right: 4px;
}

/* ─ Empty state ─────── */
.detail-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  padding: var(--insp-sp-10);
}

.detail-empty__hint {
  margin-top: var(--insp-sp-4);
  color: var(--insp-ink-tertiary);
  font-size: var(--insp-text-md);
}

@media (max-width: 1100px) {
  .pane { grid-template-columns: 320px 1fr; }
  .detail { padding: var(--insp-sp-5) var(--insp-sp-6); }
}

@media (max-width: 800px) {
  .pane { grid-template-columns: 1fr; }
  .queue { display: none; }
  .kbd-tray { display: none; }
}
</style>
