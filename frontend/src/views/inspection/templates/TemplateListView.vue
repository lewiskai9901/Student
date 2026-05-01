<script setup lang="ts">
/**
 * TemplateListView — 检查模板档案 (Audit Console redesign)
 * 卷宗式行 · 状态计数标签栏 · 行尾操作集
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useInspTemplateStore } from '@/stores/inspection/inspTemplateStore'
import { TemplateStatusConfig, type TemplateStatus } from '@/types/insp/enums'
import type { TemplateSection } from '@/types/insp/template'

const router = useRouter()
const store = useInspTemplateStore()

const loading = ref(false)
const templates = ref<TemplateSection[]>([])
const total = ref(0)

const queryParams = reactive({
  page: 1,
  size: 50,
  keyword: '',
  status: undefined as TemplateStatus | undefined,
})

const filtered = computed(() => {
  const kw = queryParams.keyword.trim().toLowerCase()
  if (!kw) return templates.value
  return templates.value.filter((t: any) =>
    (t.templateName || t.sectionName || '').toLowerCase().includes(kw) ||
    (t.sectionCode || '').toLowerCase().includes(kw)
  )
})

const counts = computed(() => ({
  all: templates.value.length,
  DRAFT: templates.value.filter(t => t.status === 'DRAFT').length,
  PUBLISHED: templates.value.filter(t => t.status === 'PUBLISHED').length,
  DEPRECATED: templates.value.filter(t => t.status === 'DEPRECATED').length,
  ARCHIVED: templates.value.filter(t => t.status === 'ARCHIVED').length,
}))

async function loadData() {
  loading.value = true
  try {
    const result = await store.loadTemplates({
      page: 1, size: 200,
      status: queryParams.status,
    })
    templates.value = result.records
    total.value = result.total
  } catch (e: any) {
    ElMessage.error(e.message || '加载模板列表失败')
  } finally {
    loading.value = false
  }
}

function setStatusFilter(s?: TemplateStatus) {
  queryParams.status = s
  loadData()
}

function statusVariant(s: TemplateStatus): string {
  return ({ DRAFT: 'pending', PUBLISHED: 'pass', DEPRECATED: 'warn', ARCHIVED: 'fail' } as const)[s] || 'pending'
}

function fmtDate(s?: string | null) {
  if (!s) return '—'
  const d = new Date(s)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}

function goCreate() { router.push('/inspection/templates/create') }
function goEdit(tpl: TemplateSection) { router.push(`/inspection/templates/${tpl.id}/edit`) }

async function handlePublish(tpl: TemplateSection) {
  try {
    await ElMessageBox.confirm('发布后将创建不可变版本快照, 确认发布?', '确认发布模板', { type: 'warning' })
    await store.publish(tpl.id)
    ElMessage.success('发布成功')
    loadData()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '发布失败') }
}

async function handleDeprecate(tpl: TemplateSection) {
  try {
    await ElMessageBox.confirm('废弃后新项目无法使用此模板, 确认废弃?', '确认废弃模板', { type: 'warning' })
    await store.deprecate(tpl.id)
    ElMessage.success('已废弃')
    loadData()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
}

async function handleArchive(tpl: TemplateSection) {
  try {
    await ElMessageBox.confirm('归档后模板将不可见, 确认归档?', '确认归档模板', { type: 'warning' })
    await store.archive(tpl.id)
    ElMessage.success('已归档')
    loadData()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
}

async function handleDuplicate(tpl: TemplateSection) {
  try {
    await store.duplicate(tpl.id)
    ElMessage.success('复制成功')
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '复制失败') }
}

async function handleDelete(tpl: TemplateSection) {
  try {
    await ElMessageBox.confirm(`确认删除模板「${(tpl as any).templateName || tpl.sectionName}」?`, '确认删除', { type: 'warning' })
    await store.removeTemplate(tpl.id)
    ElMessage.success('已删除')
    loadData()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
}

onMounted(() => loadData())
</script>

<template>
  <div class="insp-shell template-archive">
    <!-- ── Editorial header ─────────── -->
    <header class="page-head">
      <div>
        <div class="insp-eyebrow">模板档案 / Template Archive</div>
        <h1 class="insp-display page-title">检查模板</h1>
      </div>
      <div class="head-stats">
        <div class="insp-stat">
          <span class="insp-stat__value">{{ counts.all }}</span>
          <span class="insp-stat__label">总数</span>
        </div>
        <div class="head-rule" />
        <div class="insp-stat">
          <span class="insp-stat__value" :style="{ color: 'var(--insp-pass)' }">{{ counts.PUBLISHED }}</span>
          <span class="insp-stat__label">已发布</span>
        </div>
        <div class="head-rule" />
        <div class="insp-stat">
          <span class="insp-stat__value">{{ counts.DRAFT }}</span>
          <span class="insp-stat__label">草稿</span>
        </div>
        <button class="head-cta insp-btn insp-btn--accent insp-btn--lg" @click="goCreate">
          <span>新建模板</span>
        </button>
      </div>
    </header>

    <hr class="insp-rule insp-rule--strong head-divider" />

    <!-- ── Filter rail ─────────── -->
    <nav class="filter-rail">
      <button class="filter-tab" :class="{ 'is-active': queryParams.status === undefined }" @click="setStatusFilter(undefined)">
        <span class="filter-tab__label">全部</span>
        <span class="filter-tab__count">{{ counts.all }}</span>
      </button>
      <button class="filter-tab" :class="{ 'is-active': queryParams.status === 'DRAFT' }" @click="setStatusFilter('DRAFT')">
        <span class="filter-tab__label">草稿</span>
        <span class="filter-tab__count">{{ counts.DRAFT }}</span>
      </button>
      <button class="filter-tab" :class="{ 'is-active': queryParams.status === 'PUBLISHED' }" @click="setStatusFilter('PUBLISHED')">
        <span class="filter-tab__label">已发布</span>
        <span class="filter-tab__count">{{ counts.PUBLISHED }}</span>
      </button>
      <button class="filter-tab" :class="{ 'is-active': queryParams.status === 'DEPRECATED' }" @click="setStatusFilter('DEPRECATED')">
        <span class="filter-tab__label">已废弃</span>
        <span class="filter-tab__count">{{ counts.DEPRECATED }}</span>
      </button>
      <button class="filter-tab" :class="{ 'is-active': queryParams.status === 'ARCHIVED' }" @click="setStatusFilter('ARCHIVED')">
        <span class="filter-tab__label">已归档</span>
        <span class="filter-tab__count">{{ counts.ARCHIVED }}</span>
      </button>
      <div class="filter-spacer" />
      <div class="filter-search">
        <input v-model="queryParams.keyword" placeholder="搜索模板名称 · 编码..." />
      </div>
    </nav>

    <!-- ── Archive register ─────────── -->
    <section v-loading="loading" class="archive">
      <article
        v-for="(tpl, i) in filtered" :key="tpl.id"
        class="archive-row"
        @click="goEdit(tpl)"
      >
        <!-- Index -->
        <div class="row-num insp-num">{{ String(i + 1).padStart(3, '0') }}</div>

        <!-- Name + meta -->
        <div class="row-meta">
          <div class="row-name-line">
            <span class="row-name">{{ (tpl as any).templateName || tpl.sectionName }}</span>
            <span class="insp-chip" :class="`insp-chip--${statusVariant(tpl.status)}`">
              {{ TemplateStatusConfig[tpl.status]?.label }}
            </span>
            <span v-if="tpl.latestVersion" class="version-tag insp-num">
              v{{ tpl.latestVersion }}
            </span>
          </div>
          <div class="row-code-line">
            <span class="row-code">{{ tpl.sectionCode }}</span>
            <span class="row-sep">·</span>
            <span class="insp-num">{{ fmtDate(tpl.updatedAt) }}</span>
          </div>
        </div>

        <!-- Description -->
        <div class="row-desc">{{ (tpl as any).description || '—' }}</div>

        <!-- Actions -->
        <div class="row-actions" @click.stop>
          <button v-if="tpl.status === 'DRAFT'" class="insp-btn insp-btn--sm" @click="handlePublish(tpl)">发布</button>
          <button v-if="tpl.status === 'PUBLISHED'" class="insp-btn insp-btn--sm" @click="handleDeprecate(tpl)">废弃</button>
          <button v-if="tpl.status === 'DEPRECATED' || tpl.status === 'PUBLISHED'" class="insp-btn insp-btn--sm" @click="handleArchive(tpl)">归档</button>
          <button class="insp-btn insp-btn--sm" @click="handleDuplicate(tpl)">复制</button>
          <button class="insp-btn insp-btn--sm insp-btn--ghost" @click="handleDelete(tpl)" title="删除">×</button>
        </div>
      </article>

      <div v-if="!loading && filtered.length === 0" class="empty">
        <div class="insp-stamp">无记录</div>
        <p class="empty-hint">{{ queryParams.keyword ? '未找到匹配的模板' : '尚未创建任何检查模板' }}</p>
        <button v-if="!queryParams.keyword" class="insp-btn insp-btn--accent insp-btn--lg" @click="goCreate">
          创建第一个模板
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.template-archive {
  padding: 32px 48px 64px;
  max-width: 1500px;
  margin: 0 auto;
  min-height: 100vh;
  background: var(--insp-bg-page);
}

/* ─ Header ─────── */
.page-head {
  display: flex; align-items: flex-end; justify-content: space-between;
  gap: var(--insp-sp-7);
  margin-bottom: var(--insp-sp-4);
}
.page-title { font-size: 44px; margin: 0; font-weight: 500; }
.head-stats {
  display: flex; align-items: center; gap: var(--insp-sp-6);
  padding: 4px 0;
}
.head-rule { width: 1px; height: 32px; background: var(--insp-border-default); }
.head-cta { margin-left: var(--insp-sp-5); }
.head-divider { margin: 0 0 var(--insp-sp-7); }

/* ─ Filter rail ─────── */
.filter-rail {
  display: flex; align-items: center; gap: 0;
  margin-bottom: var(--insp-sp-5);
  border-bottom: 1px solid var(--insp-border-subtle);
}
.filter-tab {
  position: relative;
  display: inline-flex; align-items: baseline; gap: var(--insp-sp-2);
  padding: 10px 18px 12px;
  border: 0; background: transparent; cursor: pointer;
  font-family: inherit; font-size: var(--insp-text-md); font-weight: 500;
  color: var(--insp-ink-tertiary);
  transition: color var(--insp-t-fast);
}
.filter-tab:hover { color: var(--insp-ink-primary); }
.filter-tab.is-active { color: var(--insp-ink-primary); }
.filter-tab.is-active::after {
  content: ''; position: absolute; left: 18px; right: 18px; bottom: -1px;
  height: 2px; background: var(--insp-accent);
}
.filter-tab__count {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs); font-weight: 500;
  color: var(--insp-ink-quaternary);
}
.filter-tab.is-active .filter-tab__count { color: var(--insp-accent); }
.filter-spacer { flex: 1; }
.filter-search input {
  padding: 6px 12px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  font-family: inherit;
  font-size: var(--insp-text-sm);
  width: 240px;
  background: var(--insp-bg-surface);
  color: var(--insp-ink-primary);
}
.filter-search input:focus {
  outline: none;
  border-color: var(--insp-accent);
  box-shadow: 0 0 0 3px var(--insp-accent-paler);
}

/* ─ Archive rows ─────── */
.archive { display: flex; flex-direction: column; }

.archive-row {
  display: grid;
  grid-template-columns: 56px 320px 1fr 280px;
  gap: var(--insp-sp-5);
  align-items: start;
  padding: var(--insp-sp-5) 0;
  border-bottom: 1px solid var(--insp-border-subtle);
  cursor: pointer;
  transition: background var(--insp-t-fast);
}
.archive-row:hover {
  background: linear-gradient(to right, var(--insp-bg-subtle), transparent 80%);
}

.row-num {
  font-size: 26px; font-weight: 500;
  color: var(--insp-ink-quaternary);
  letter-spacing: -0.02em; line-height: 1;
  padding-top: 2px;
}

.row-meta { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.row-name-line {
  display: flex; align-items: center; gap: var(--insp-sp-2);
  flex-wrap: wrap;
}
.row-name {
  font-family: var(--insp-font-display);
  font-size: 18px; font-weight: 500;
  color: var(--insp-ink-primary);
  letter-spacing: var(--insp-tracking-tight);
}
.version-tag {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs); font-weight: 600;
  color: var(--insp-accent);
  padding: 1px 6px;
  border: 1px solid var(--insp-accent);
  border-radius: var(--insp-radius-sm);
}
.row-code-line {
  display: flex; align-items: center; gap: 6px;
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-tertiary);
}
.row-code {
  font-family: var(--insp-font-mono);
  font-weight: 500;
  color: var(--insp-ink-secondary);
}
.row-sep { color: var(--insp-ink-quaternary); }

.row-desc {
  font-size: var(--insp-text-md);
  line-height: var(--insp-leading-snug);
  color: var(--insp-ink-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.row-actions {
  display: flex; gap: var(--insp-sp-2);
  justify-content: flex-end;
  align-items: flex-start;
  flex-wrap: wrap;
  padding-top: 2px;
}

/* ─ Empty ─────── */
.empty {
  padding: 80px 0; text-align: center;
}
.empty-hint {
  margin: var(--insp-sp-4) 0 var(--insp-sp-5);
  color: var(--insp-ink-tertiary);
  font-size: var(--insp-text-sm);
}

/* ─ Responsive ─────── */
@media (max-width: 1100px) {
  .archive-row { grid-template-columns: 44px 1fr 280px; }
  .row-desc { display: none; }
}
@media (max-width: 800px) {
  .template-archive { padding: 20px 16px 64px; }
  .page-title { font-size: 32px; }
  .archive-row { grid-template-columns: 44px 1fr; gap: var(--insp-sp-3); }
  .row-actions { grid-column: 2; }
}
</style>
