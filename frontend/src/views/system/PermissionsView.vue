<template>
  <div class="permissions-redesign">
    <!-- Header -->
    <header class="ph-header">
      <div>
        <h1 class="ph-title">权限目录</h1>
        <p class="ph-subtitle">按行业插件分组展示所有权限点 (代码注解自动声明, 管理员只读)</p>
      </div>
      <div class="ph-actions">
        <button class="ph-btn" @click="runSyncCheck" :disabled="syncing">
          <Loader2 v-if="syncing" class="spinning" :size="14" />
          <RefreshCw v-else :size="14" />
          {{ syncing ? '检查中...' : '同步检查' }}
        </button>
      </div>
    </header>

    <!-- Stats bar -->
    <div class="ph-stats">
      <span class="ph-stat-total">总数 <b>{{ totalCount }}</b></span>
      <span class="ph-sep">·</span>
      <template v-for="(g, i) in industryGroups" :key="g.industryCode">
        <span v-if="i > 0" class="ph-sep">·</span>
        <span :class="['ph-stat', { 'ph-stat-disabled': !g.pluginEnabled }]">
          <span class="ph-stat-dot" :style="{ background: industryColor(g.industryCode) }"></span>
          {{ g.industryLabel }} <b>{{ g.total }}</b>
          <AlertTriangle v-if="!g.pluginEnabled" :size="11" class="ph-stat-warn" />
        </span>
      </template>
    </div>

    <!-- Filters -->
    <div class="ph-filters">
      <div class="ph-search-wrap">
        <Search :size="14" class="ph-search-icon" />
        <input
          v-model="searchQuery"
          class="ph-search"
          placeholder="搜索权限码或名称..."
        />
      </div>
      <select v-model="typeFilter" class="ph-select">
        <option value="">全部类型</option>
        <option value="MENU">菜单</option>
        <option value="OPERATION">按钮</option>
        <option value="API">API</option>
        <option value="DATA">数据</option>
      </select>
      <label class="ph-checkbox">
        <input type="checkbox" v-model="showOnlyDisabled" />
        <span>仅显示禁用</span>
      </label>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="ph-loading">
      <Loader2 class="spinning" :size="28" />
    </div>

    <!-- Industry groups -->
    <div v-else class="ph-body">
      <div
        v-for="ig in filteredIndustryGroups"
        :key="ig.industryCode"
        :class="['ig-card', { 'ig-disabled': !ig.pluginEnabled }]"
      >
        <!-- Industry header -->
        <div class="ig-head" :style="{ borderLeftColor: industryColor(ig.industryCode) }">
          <div class="ig-head-main">
            <span
              class="ig-icon"
              :style="{
                background: industryColor(ig.industryCode) + '18',
                color: industryColor(ig.industryCode)
              }"
            >
              <component :is="industryIcon(ig.industryCode)" :size="16" />
            </span>
            <div class="ig-head-text">
              <h2 class="ig-title">{{ ig.industryLabel }}</h2>
              <p class="ig-subtitle">
                <code class="ig-code">{{ ig.industryCode }}</code>
                <span class="ig-dot">·</span>
                <b>{{ ig.total }}</b> 个权限
                <span class="ig-dot">·</span>
                <b>{{ ig.modules.length }}</b> 模块
              </p>
            </div>
          </div>
          <div class="ig-head-status">
            <span v-if="ig.pluginEnabled" class="ig-tag ig-tag-ok">
              <CheckCircle :size="11" /> 启用
            </span>
            <span v-else class="ig-tag ig-tag-bad">
              <AlertTriangle :size="11" /> 插件已禁用
            </span>
            <button
              v-if="!ig.pluginEnabled && ig.industryCode !== 'CUSTOM' && ig.industryCode !== 'CORE'"
              class="ig-btn-enable"
              @click="onEnablePlugin(ig.industryCode)"
              :disabled="enabling === ig.industryCode"
            >
              <Loader2 v-if="enabling === ig.industryCode" class="spinning" :size="12" />
              {{ enabling === ig.industryCode ? '启用中...' : '一键启用' }}
            </button>
          </div>
        </div>

        <!-- Disabled banner -->
        <div v-if="!ig.pluginEnabled" class="ig-alert">
          <AlertTriangle :size="13" />
          <span>
            下列 <b>{{ ig.total }}</b> 条权限级联失效, 绑定它们的用户实际已无权限. 启用插件后自动恢复.
          </span>
        </div>

        <!-- Modules -->
        <div class="ig-modules">
          <div v-for="mg in ig.modules" :key="mg.moduleCode" class="mg-card">
            <div
              class="mg-head"
              @click="toggle(ig.industryCode + ':' + mg.moduleCode)"
            >
              <ChevronDown
                :size="13"
                class="mg-chevron"
                :class="{ 'mg-chevron-open': expanded[ig.industryCode + ':' + mg.moduleCode] }"
              />
              <span class="mg-name">{{ mg.moduleLabel }}</span>
              <code class="mg-modcode">{{ mg.moduleCode }}</code>
              <span class="mg-count">{{ mg.permissions.length }}</span>
            </div>
            <div
              v-if="expanded[ig.industryCode + ':' + mg.moduleCode]"
              class="mg-perms"
            >
              <div
                v-for="p in mg.permissions"
                :key="p.permissionCode"
                :class="['p-row', { 'p-disabled': (p as any).pluginEnabled === false }]"
                :title="(p as any).pluginEnabled === false ? '所属插件已禁用 — 级联软失效' : undefined"
              >
                <code class="p-code">{{ p.permissionCode }}</code>
                <span class="p-name">{{ p.permissionName || '-' }}</span>
                <span v-if="(p as any).pluginEnabled === false" class="p-badge-disabled">
                  插件禁用
                </span>
                <span class="p-type" :class="'p-type-' + (p.type || '').toLowerCase()">
                  {{ typeName(p.type) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty -->
      <div v-if="!filteredIndustryGroups.length" class="ph-empty">
        <Lock :size="32" />
        <p>{{ hasFilter ? '无匹配权限' : '暂无权限数据' }}</p>
      </div>
    </div>

    <!-- Sync check dialog (retained from original) -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="syncDialogVisible" class="sync-modal">
          <div class="sync-backdrop" @click="syncDialogVisible = false"></div>
          <div class="sync-dialog">
            <div class="sync-head">
              <h3>权限同步检查结果</h3>
              <button class="sync-close" @click="syncDialogVisible = false">
                <X :size="16" />
              </button>
            </div>
            <div v-if="syncResult" class="sync-body">
              <div class="sync-summary">
                <div class="sync-line">
                  <CheckCircle :size="14" class="sync-ok" />
                  <span>代码中的权限注解: <b>{{ syncResult.codeAnnotationCount }}</b> 个</span>
                </div>
                <div class="sync-line">
                  <CheckCircle :size="14" class="sync-ok" />
                  <span>数据库权限: <b>{{ syncResult.dbPermissionCount }}</b> 条</span>
                </div>
              </div>
              <div v-if="syncResult.missingInDbCount > 0" class="sync-section">
                <div class="sync-section-title sync-warn">
                  <AlertTriangle :size="14" />
                  代码中有但数据库缺失: {{ syncResult.missingInDbCount }} 条
                </div>
                <div class="sync-codes sync-codes-warn">
                  <div v-for="code in syncResult.missingInDb" :key="code">{{ code }}</div>
                </div>
              </div>
              <div v-else class="sync-line sync-ok" style="margin-top: 16px">
                <CheckCircle :size="14" />
                代码权限与数据库完全同步
              </div>
              <div v-if="syncResult.potentiallyObsoleteCount > 0" class="sync-section">
                <div class="sync-section-title sync-info">
                  <Info :size="14" />
                  数据库中可能过时的权限: {{ syncResult.potentiallyObsoleteCount }} 条
                </div>
                <div class="sync-codes sync-codes-info">
                  <div v-for="code in syncResult.potentiallyObsoleteInDb" :key="code">{{ code }}</div>
                </div>
              </div>
            </div>
            <div class="sync-foot">
              <button class="ph-btn" @click="syncDialogVisible = false">关闭</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Lock,
  Loader2,
  ChevronDown,
  RefreshCw,
  X,
  AlertTriangle,
  CheckCircle,
  Info,
  Shield,
  GraduationCap,
  HeartPulse,
  Users,
  Settings,
} from 'lucide-vue-next'
import { getPermissions, checkPermissionSync } from '@/api/access'
import { pluginPlatformApi } from '@/api/pluginPlatform'
import type { Permission } from '@/types'

// ---- Module label mapping (二级分组标签) ----
const MODULE_LABELS: Record<string, string> = {
  academic: '学术管理',
  analytics: '数据分析',
  asset: '资产管理',
  calendar: '校历管理',
  dormitory: '宿舍管理',
  enrollment: '招生管理',
  insp: '检查平台',
  inspection: '检查通用',
  patient: '患者管理',
  place: '场所管理',
  plugin: '插件管理',
  'plugin-platform': '插件平台',
  role: '角色管理',
  schedule: '排班管理',
  student: '学生管理',
  system: '系统管理',
  task: '任务管理',
  teacher: '教师档案',
  teaching: '教学管理',
  tenant: '租户管理',
  user: '用户管理',
  ward: '病房管理',
}

// ---- Module label override by industry (上下文化二级分组标签) ----
// 某些 module 在不同 industry 下含义不同, 这里按 industry override.
// 例如 EDU 下的 system:* 实际是 building / dormitory_building / semester, 非通用系统管理.
const MODULE_LABELS_BY_INDUSTRY: Record<string, Record<string, string>> = {
  EDU: {
    system: '学校设施/学期',
  },
}

function getModuleLabel(moduleCode: string, industry: string): string {
  return (
    MODULE_LABELS_BY_INDUSTRY[industry]?.[moduleCode] ||
    MODULE_LABELS[moduleCode] ||
    moduleCode
  )
}

// ---- Industry labels (一级分组) ----
const INDUSTRY_LABELS: Record<string, string> = {
  CORE: '通用核心',
  EDU: '教育行业',
  HEALTH: '医疗行业',
  CARE: '养老行业',
  CUSTOM: '自定义',
}

const INDUSTRY_ORDER = ['CORE', 'EDU', 'HEALTH', 'CARE', 'CUSTOM']

function industryColor(code: string): string {
  const map: Record<string, string> = {
    CORE: '#2563eb',
    EDU: '#d97706',
    HEALTH: '#be185d',
    CARE: '#059669',
    CUSTOM: '#6b7280',
  }
  return map[code] || '#6b7280'
}

function industryIcon(code: string) {
  const map: Record<string, any> = {
    CORE: Shield,
    EDU: GraduationCap,
    HEALTH: HeartPulse,
    CARE: Users,
    CUSTOM: Settings,
  }
  return map[code] || Settings
}

// ---- Type display helpers ----
type PermType = 'MENU' | 'OPERATION' | 'API' | 'DATA' | 'BUTTON' | string

function typeName(type: PermType): string {
  const map: Record<string, string> = {
    MENU: '菜单',
    OPERATION: '按钮',
    BUTTON: '按钮',
    API: 'API',
    DATA: '数据',
  }
  return map[type] || type || '未知'
}

// ---- State ----
const loading = ref(false)
const searchQuery = ref('')
const typeFilter = ref('')
const showOnlyDisabled = ref(false)
const allPermissions = ref<Permission[]>([])
const expanded = reactive<Record<string, boolean>>({})
const enabling = ref<string>('')

// ---- Sync check state ----
const syncing = ref(false)
const syncDialogVisible = ref(false)
const syncResult = ref<{
  codeAnnotationCount: number
  dbPermissionCount: number
  missingInDb: string[]
  missingInDbCount: number
  potentiallyObsoleteInDb: string[]
  potentiallyObsoleteCount: number
} | null>(null)

// ---- Grouping types ----
interface ModuleGroup {
  moduleCode: string
  moduleLabel: string
  permissions: Permission[]
}

interface IndustryGroup {
  industryCode: string
  industryLabel: string
  pluginEnabled: boolean
  total: number
  modules: ModuleGroup[]
}

function flattenTree(nodes: Permission[]): Permission[] {
  const result: Permission[] = []
  for (const node of nodes || []) {
    result.push(node)
    if (node.children?.length) {
      result.push(...flattenTree(node.children))
    }
  }
  return result
}

function groupBy<T>(arr: T[], keyFn: (item: T) => string): Record<string, T[]> {
  const out: Record<string, T[]> = {}
  for (const item of arr) {
    const k = keyFn(item)
    if (!out[k]) out[k] = []
    out[k].push(item)
  }
  return out
}

// ---- Industry groups (一级分组: 基于全量数据, 不含过滤) ----
const industryGroups = computed<IndustryGroup[]>(() => {
  const flat = flattenTree(allPermissions.value)
  const byIndustry = groupBy(flat, (p: any) => p.industry || 'CUSTOM')
  const result: IndustryGroup[] = []

  for (const [industry, perms] of Object.entries(byIndustry)) {
    const byModule = groupBy(
      perms,
      p => p.permissionCode?.split(':')[0] || 'other'
    )
    // 任一条 pluginEnabled=false 则视为整行业插件禁用
    const anyDisabled = perms.some((p: any) => p.pluginEnabled === false)
    const modules: ModuleGroup[] = Object.entries(byModule)
      .map(([m, ps]) => ({
        moduleCode: m,
        moduleLabel: getModuleLabel(m, industry),
        permissions: ps.sort((a, b) =>
          a.permissionCode.localeCompare(b.permissionCode)
        ),
      }))
      .sort((a, b) => a.moduleLabel.localeCompare(b.moduleLabel, 'zh-CN'))

    result.push({
      industryCode: industry,
      industryLabel: INDUSTRY_LABELS[industry] || industry,
      pluginEnabled: !anyDisabled,
      total: perms.length,
      modules,
    })
  }

  result.sort((a, b) => {
    const ia = INDUSTRY_ORDER.indexOf(a.industryCode)
    const ib = INDUSTRY_ORDER.indexOf(b.industryCode)
    const sa = ia === -1 ? 99 : ia
    const sb = ib === -1 ? 99 : ib
    return sa - sb
  })
  return result
})

const hasFilter = computed(
  () => !!searchQuery.value || !!typeFilter.value || showOnlyDisabled.value
)

// ---- Filtered industry groups (应用 search / type / disabled 过滤) ----
const filteredIndustryGroups = computed<IndustryGroup[]>(() => {
  const q = searchQuery.value.toLowerCase().trim()
  const t = typeFilter.value
  const onlyDisabled = showOnlyDisabled.value

  return industryGroups.value
    .map(ig => {
      const modules = ig.modules
        .map(mg => ({
          ...mg,
          permissions: mg.permissions.filter(p => {
            if (onlyDisabled && (p as any).pluginEnabled !== false) return false
            if (t && p.type !== t) return false
            if (q) {
              const code = (p.permissionCode || '').toLowerCase()
              const name = (p.permissionName || '').toLowerCase()
              if (!code.includes(q) && !name.includes(q)) return false
            }
            return true
          }),
        }))
        .filter(mg => mg.permissions.length > 0)
      return { ...ig, modules }
    })
    .filter(ig => ig.modules.length > 0)
})

// ---- Stats ----
const totalCount = computed(() =>
  industryGroups.value.reduce((s, g) => s + g.total, 0)
)

// ---- Actions ----
function toggle(key: string) {
  expanded[key] = !expanded[key]
}

async function loadPermissions() {
  loading.value = true
  try {
    const data = await getPermissions({ includeDisabled: true } as any)
    allPermissions.value = data || []
    // 默认展开每个行业的第一个 module
    for (const ig of industryGroups.value) {
      for (const mg of ig.modules) {
        const k = ig.industryCode + ':' + mg.moduleCode
        if (!(k in expanded)) expanded[k] = true
      }
    }
  } catch (error) {
    ElMessage.error('加载权限列表失败')
  } finally {
    loading.value = false
  }
}

async function runSyncCheck() {
  syncing.value = true
  try {
    const data = await checkPermissionSync()
    syncResult.value = data
    syncDialogVisible.value = true
  } catch (error) {
    ElMessage.error('同步检查失败，请确认后端服务正常运行')
  } finally {
    syncing.value = false
  }
}

async function onEnablePlugin(industryCode: string) {
  try {
    await ElMessageBox.confirm(
      `确认启用 ${industryCode} 插件? 将级联恢复所有被禁的 ${industryCode} 贡献项 (角色/权限/类型/关系等).`,
      '确认启用',
      { type: 'info', confirmButtonText: '启用', cancelButtonText: '取消' }
    )
    enabling.value = industryCode
    await pluginPlatformApi.enable(industryCode)
    ElMessage.success(`${industryCode} 插件已启用`)
    await loadPermissions()
  } catch (e: any) {
    if (e !== 'cancel' && e?.message !== 'cancel') {
      ElMessage.error('启用失败: ' + (e?.message || e))
    }
  } finally {
    enabling.value = ''
  }
}

onMounted(() => {
  loadPermissions()
})
</script>

<style scoped>
.permissions-redesign {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f9fafb;
  overflow: hidden;
}

/* ========== Header ========== */
.ph-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}
.ph-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}
.ph-subtitle {
  margin: 2px 0 0;
  font-size: 13px;
  color: #6b7280;
}
.ph-actions {
  display: flex;
  gap: 8px;
}
.ph-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  color: #374151;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}
.ph-btn:hover:not(:disabled) {
  background: #f3f4f6;
}
.ph-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.spinning {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ========== Stats bar ========== */
.ph-stats {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px 10px;
  padding: 10px 24px;
  background: #fff;
  border-bottom: 1px solid #f3f4f6;
  font-size: 13px;
  color: #6b7280;
}
.ph-stat-total b { color: #111827; font-weight: 600; }
.ph-sep { color: #d1d5db; }
.ph-stat {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.ph-stat b {
  color: #111827;
  font-weight: 600;
}
.ph-stat-disabled {
  color: #9ca3af;
}
.ph-stat-disabled b {
  color: #6b7280;
}
.ph-stat-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.ph-stat-warn {
  color: #d97706;
  margin-left: 2px;
}

/* ========== Filters ========== */
.ph-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 24px;
  background: #fff;
  border-bottom: 1px solid #f3f4f6;
}
.ph-search-wrap {
  position: relative;
  flex: 1;
  max-width: 360px;
}
.ph-search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  pointer-events: none;
}
.ph-search {
  width: 100%;
  height: 34px;
  padding: 0 12px 0 32px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 13px;
  color: #111827;
  outline: none;
  transition: border-color 0.15s;
}
.ph-search:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}
.ph-select {
  height: 34px;
  padding: 0 28px 0 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 13px;
  color: #374151;
  background: #fff;
  cursor: pointer;
  outline: none;
}
.ph-checkbox {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  user-select: none;
}
.ph-checkbox input {
  width: 14px;
  height: 14px;
  cursor: pointer;
  accent-color: #2563eb;
}

/* ========== Body ========== */
.ph-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px 32px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.ph-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
}

/* ========== Industry card ========== */
.ig-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
  transition: all 0.2s;
}
.ig-card.ig-disabled {
  background: #fffbeb;
  border-color: #fcd34d;
}

/* Industry header */
.ig-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-left: 4px solid transparent;
  background: transparent;
}
.ig-head-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.ig-icon {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  flex-shrink: 0;
}
.ig-head-text { min-width: 0; }
.ig-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}
.ig-subtitle {
  margin: 2px 0 0;
  font-size: 12px;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.ig-subtitle b { color: #111827; font-weight: 600; }
.ig-code {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  background: #f3f4f6;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
  color: #374151;
}
.ig-dot { color: #d1d5db; }

.ig-head-status {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.ig-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
  line-height: 1.5;
}
.ig-tag-ok {
  background: #d1fae5;
  color: #065f46;
  border: 1px solid #a7f3d0;
}
.ig-tag-bad {
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fcd34d;
}
.ig-btn-enable {
  height: 26px;
  padding: 0 12px;
  border: 1px solid #d97706;
  background: #d97706;
  color: #fff;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: background 0.15s;
}
.ig-btn-enable:hover:not(:disabled) {
  background: #b45309;
  border-color: #b45309;
}
.ig-btn-enable:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Disabled banner */
.ig-alert {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 18px;
  background: #fef3c7;
  color: #92400e;
  font-size: 12px;
  border-top: 1px solid #fde68a;
  border-bottom: 1px solid #fde68a;
}
.ig-alert b { font-weight: 600; }

/* Modules */
.ig-modules {
  padding: 12px 18px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.mg-card {
  border: 1px solid #f3f4f6;
  border-radius: 8px;
  overflow: hidden;
  background: #fafafa;
}
.mg-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  user-select: none;
  background: #fafafa;
  transition: background 0.15s;
}
.mg-head:hover {
  background: #f3f4f6;
}
.mg-chevron {
  color: #9ca3af;
  transition: transform 0.2s;
}
.mg-chevron-open {
  transform: rotate(180deg);
  color: #2563eb;
}
.mg-name {
  font-size: 13px;
  font-weight: 500;
  color: #111827;
}
.mg-modcode {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  color: #9ca3af;
}
.mg-count {
  margin-left: auto;
  padding: 1px 8px;
  border-radius: 10px;
  background: #e5e7eb;
  color: #4b5563;
  font-size: 11px;
  font-weight: 500;
}

/* Permission rows */
.mg-perms {
  background: #fff;
  border-top: 1px solid #f3f4f6;
}
.p-row {
  display: grid;
  grid-template-columns: minmax(240px, 280px) 1fr auto auto;
  align-items: center;
  gap: 10px;
  padding: 7px 14px;
  font-size: 12px;
  border-bottom: 1px solid #fafafa;
  transition: background 0.1s;
}
.p-row:last-child { border-bottom: none; }
.p-row:hover { background: #f9fafb; }
.p-row.p-disabled {
  opacity: 0.55;
  background: #fafaf9;
}
.p-row.p-disabled:hover {
  background: #fef3c7;
  opacity: 0.8;
}
.p-code {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  color: #6b7280;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.p-name {
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.p-badge-disabled {
  padding: 1px 7px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  color: #a16207;
  background: #fef3c7;
  border: 1px solid #fcd34d;
  line-height: 1.4;
  cursor: help;
}
.p-type {
  padding: 1px 7px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 500;
  line-height: 1.5;
}
.p-type-menu      { background: #dbeafe; color: #1d4ed8; }
.p-type-operation { background: #fef3c7; color: #a16207; }
.p-type-button    { background: #fef3c7; color: #a16207; }
.p-type-api       { background: #d1fae5; color: #065f46; }
.p-type-data      { background: #ede9fe; color: #6d28d9; }

/* Empty */
.ph-empty {
  padding: 60px 16px;
  text-align: center;
  color: #9ca3af;
}
.ph-empty p {
  margin: 8px 0 0;
  font-size: 13px;
}

/* ========== Sync dialog ========== */
.sync-modal {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
}
.sync-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
}
.sync-dialog {
  position: relative;
  width: 100%;
  max-width: 520px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.2);
}
.sync-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #e5e7eb;
}
.sync-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}
.sync-close {
  border: none;
  background: transparent;
  padding: 4px;
  border-radius: 6px;
  cursor: pointer;
  color: #6b7280;
}
.sync-close:hover {
  background: #f3f4f6;
}
.sync-body {
  max-height: 60vh;
  overflow-y: auto;
  padding: 16px 20px;
}
.sync-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.sync-line {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #374151;
}
.sync-line b { color: #111827; font-weight: 600; }
.sync-ok { color: #10b981; }
.sync-warn { color: #d97706; }
.sync-info { color: #6b7280; }
.sync-section { margin-top: 16px; }
.sync-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
}
.sync-codes {
  margin-top: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  max-height: 160px;
  overflow-y: auto;
}
.sync-codes-warn { background: #fef3c7; }
.sync-codes-warn div { color: #92400e; }
.sync-codes-info { background: #f3f4f6; }
.sync-codes-info div { color: #4b5563; }
.sync-codes div {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  padding: 2px 0;
}
.sync-foot {
  display: flex;
  justify-content: flex-end;
  padding: 12px 20px;
  border-top: 1px solid #e5e7eb;
}

/* Modal transition */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
