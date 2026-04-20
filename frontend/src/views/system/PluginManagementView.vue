<template>
  <div class="pp-page">
    <!-- Header -->
    <div class="pp-header">
      <div>
        <h1 class="pp-title">插件平台</h1>
        <p class="pp-subtitle">
          所有由业务插件声明的类型、关系、事件、权限、角色 — 通用核心 + 行业整包
        </p>
      </div>
      <el-button type="primary" plain size="small" @click="loadAll" :loading="loading">
        <el-icon><Refresh /></el-icon> 刷新
      </el-button>
    </div>

    <!-- Stats row -->
    <div class="pp-stats">
      <div class="stat-card" :class="{ active: activeTab === 'types' }" @click="activeTab = 'types'">
        <div class="stat-icon" style="background: #dbeafe; color: #2563eb;">
          <LayoutGrid />
        </div>
        <div class="stat-body">
          <div class="stat-label">类型</div>
          <div class="stat-count"><b>{{ typeStats.total }}</b><span v-if="typeStats.total !== typeStats.plugin">/{{ typeStats.plugin }} 插件</span></div>
        </div>
      </div>
      <div class="stat-card" :class="{ active: activeTab === 'relations' }" @click="activeTab = 'relations'">
        <div class="stat-icon" style="background: #fae8ff; color: #a21caf;">
          <Link2 />
        </div>
        <div class="stat-body">
          <div class="stat-label">关系</div>
          <div class="stat-count"><b>{{ relationStats.total }}</b><span v-if="relationStats.total !== relationStats.plugin">/{{ relationStats.plugin }} 插件</span></div>
        </div>
      </div>
      <div class="stat-card" :class="{ active: activeTab === 'events' }" @click="activeTab = 'events'">
        <div class="stat-icon" style="background: #fef3c7; color: #d97706;">
          <Bell />
        </div>
        <div class="stat-body">
          <div class="stat-label">事件类型</div>
          <div class="stat-count"><b>{{ eventStats.total }}</b><span v-if="eventStats.total !== eventStats.plugin">/{{ eventStats.plugin }} 插件</span></div>
        </div>
      </div>
      <div class="stat-card" :class="{ active: activeTab === 'permissions' }" @click="activeTab = 'permissions'">
        <div class="stat-icon" style="background: #d1fae5; color: #047857;">
          <Shield />
        </div>
        <div class="stat-body">
          <div class="stat-label">权限</div>
          <div class="stat-count"><b>{{ permissionStats.total }}</b><span v-if="permissionStats.total !== permissionStats.plugin">/{{ permissionStats.plugin }} 插件</span></div>
        </div>
      </div>
      <div class="stat-card" :class="{ active: activeTab === 'roles' }" @click="activeTab = 'roles'">
        <div class="stat-icon" style="background: #e0e7ff; color: #4338ca;">
          <UserCog />
        </div>
        <div class="stat-body">
          <div class="stat-label">角色</div>
          <div class="stat-count"><b>{{ roleStats.total }}</b><span v-if="roleStats.total !== roleStats.plugin">/{{ roleStats.plugin }} 插件</span></div>
        </div>
      </div>
      <div class="stat-card" :class="{ active: activeTab === 'industries' }" @click="activeTab = 'industries'">
        <div class="stat-icon" style="background: #ffe4e6; color: #be123c;">
          <Package />
        </div>
        <div class="stat-body">
          <div class="stat-label">行业插件包</div>
          <div class="stat-count"><b>{{ industries.length }}</b><span> 个</span></div>
        </div>
      </div>
      <div class="stat-card" :class="{ active: activeTab === 'tenants' }" @click="activeTab = 'tenants'; loadTenantPlugins()">
        <div class="stat-icon" style="background: #f0fdf4; color: #166534;">
          <Building2 />
        </div>
        <div class="stat-body">
          <div class="stat-label">租户插件</div>
          <div class="stat-count"><b>{{ tenantPluginList.length }}</b><span> 条</span></div>
        </div>
      </div>
    </div>

    <!-- Main area -->
    <div class="pp-main">
      <!-- Sidebar: Industry groups -->
      <aside class="pp-sidebar">
        <div class="pp-sidebar-title">行业包</div>
        <button
          class="pp-industry"
          :class="{ active: industryFilter === '' }"
          @click="industryFilter = ''"
        >
          <span class="pp-industry-dot" style="background: #6b7280;"></span>
          <span class="pp-industry-name">全部</span>
          <span class="pp-industry-count">{{ totalPluginCount }}</span>
        </button>
        <button
          v-for="ind in industries"
          :key="ind.code"
          class="pp-industry"
          :class="{ active: industryFilter === ind.code }"
          @click="industryFilter = ind.code"
        >
          <span class="pp-industry-dot" :style="{ background: industryColor(ind.code) }"></span>
          <span class="pp-industry-name">{{ ind.label }}</span>
          <span class="pp-industry-count">{{ ind.pluginCount }}</span>
        </button>
        <!-- 层级筛选仅对"关系"有效 — 其他 Tab 没有 tier 字段 -->
        <template v-if="activeTab === 'relations'">
          <div class="pp-sidebar-title" style="margin-top: 16px;" title="CORE=通用核心关系 / COMMON_EXT=通用扩展 / DOMAIN=行业垂直">
            关系层级
          </div>
          <button
            class="pp-industry"
            :class="{ active: tierFilter === '' }"
            @click="tierFilter = ''"
          >
            <span class="pp-industry-name">全部</span>
          </button>
          <button
            v-for="tier in tiers"
            :key="tier.code"
            class="pp-industry"
            :class="{ active: tierFilter === tier.code }"
            @click="tierFilter = tier.code"
          >
            <span class="pp-chip" :class="'pp-chip-' + tierTagType(tier.code)">{{ tier.label }}</span>
          </button>
        </template>
      </aside>

      <!-- Content -->
      <section class="pp-content">
        <!-- Search bar -->
        <div class="pp-toolbar">
          <input
            v-model="searchText"
            type="text"
            class="pp-search"
            placeholder="搜索名称 / 代码 / 插件类"
          />
          <span class="pp-count">{{ filteredCount }} 条</span>
        </div>

        <!-- Tab: Types -->
        <div v-if="activeTab === 'types'" class="pp-list">
          <div v-if="!filteredTypes.length" class="pp-empty">暂无类型</div>
          <div v-for="grp in groupedTypes" :key="grp.key" class="pp-group">
            <div class="pp-group-header">
              <span class="pp-group-title">{{ subjectTypeLabel(grp.entityType) }}</span>
              <span class="pp-group-badge">{{ grp.items.length }}</span>
            </div>
            <table class="pp-table">
              <thead>
                <tr>
                  <th>类型</th>
                  <th>类型码</th>
                  <th>字段数</th>
                  <th>特性</th>
                  <th>来源插件</th>
                  <th>行业</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="t in grp.items" :key="t.id">
                  <td>
                    <span class="pp-icon-circle" :style="{ background: t.uiConfig?.color || '#9ca3af' }">
                      {{ (t.typeName || t.typeCode || '?').charAt(0) }}
                    </span>
                    {{ t.typeName }}
                  </td>
                  <td><code class="pp-code">{{ t.typeCode }}</code></td>
                  <td>{{ countFields(t) }}</td>
                  <td>
                    <span v-for="f in topFeatures(t)" :key="f" class="pp-feat-chip">{{ f }}</span>
                  </td>
                  <td>
                    <code v-if="t.pluginClass" class="pp-code pp-code-thin">{{ shortClass(t.pluginClass) }}</code>
                    <span v-else class="pp-muted">自定义</span>
                  </td>
                  <td>
                    <span class="pp-industry-chip" :style="industryChipStyle(resolveIndustry(t, 'pluginClass'))">
                      {{ industryLabel(resolveIndustry(t, 'pluginClass')) || '—' }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Tab: Relations -->
        <div v-if="activeTab === 'relations'" class="pp-list">
          <div v-if="!filteredRelations.length" class="pp-empty">暂无关系</div>
          <table v-else class="pp-table">
            <thead>
              <tr>
                <th>关系码</th>
                <th>名称</th>
                <th>方向</th>
                <th>类别</th>
                <th>约束</th>
                <th>层级</th>
                <th>来源插件</th>
                <th>行业</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in filteredRelations" :key="r.relationCode + r.fromType + r.toType">
                <td><code class="pp-code">{{ r.relationCode }}</code></td>
                <td>{{ r.relationName }}</td>
                <td>
                  <span class="pp-muted">{{ subjectTypeLabel(r.fromType) }}</span>
                  <span class="pp-arrow">→</span>
                  <span class="pp-muted">{{ subjectTypeLabel(r.toType) }}</span>
                </td>
                <td>
                  <span class="pp-chip" :class="'pp-chip-' + categoryTagType(r.category)">
                    {{ categoryLabel(r.category) }}
                  </span>
                </td>
                <td>
                  <span v-if="r.capacityBound" class="pp-chip pp-chip-warning">容量绑定</span>
                  <span v-else-if="r.maxPerResource" class="pp-chip pp-chip-primary">≤ {{ r.maxPerResource }}</span>
                  <span v-else-if="r.maxBySubtype" class="pp-chip pp-chip-primary" :title="JSON.stringify(r.maxBySubtype)">按子类型</span>
                  <span v-else class="pp-muted">无限</span>
                </td>
                <td>
                  <span class="pp-chip" :class="'pp-chip-' + tierTagType(r.tier)">{{ tierLabel(r.tier) }}</span>
                </td>
                <td><code class="pp-code pp-code-thin">{{ r.registeredBy }}</code></td>
                <td>
                  <span class="pp-industry-chip" :style="industryChipStyle(relationIndustry(r))">
                    {{ industryLabel(relationIndustry(r)) || '—' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Tab: Events -->
        <div v-if="activeTab === 'events'" class="pp-list">
          <div v-if="!filteredEvents.length" class="pp-empty">暂无事件类型</div>
          <table v-else class="pp-table">
            <thead>
              <tr>
                <th>代码</th>
                <th>名称</th>
                <th>类别</th>
                <th>极性</th>
                <th>适用主体</th>
                <th>来源插件</th>
                <th>行业</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="e in filteredEvents" :key="e.typeCode">
                <td><code class="pp-code">{{ e.typeCode }}</code></td>
                <td>
                  <span v-if="e.icon" class="pp-evt-icon" :style="{ background: (e.color || '#6b7280') + '22', color: e.color || '#6b7280' }">●</span>
                  {{ e.typeName }}
                </td>
                <td><span class="pp-muted">{{ e.categoryName || e.categoryCode }}</span></td>
                <td>
                  <span class="pp-chip" :class="'pp-chip-' + polarityTagType(e.categoryPolarity)">
                    {{ polarityLabel(e.categoryPolarity) }}
                  </span>
                </td>
                <td>
                  <span v-for="s in parseSubjects(e.applicableSubjects)" :key="s" class="pp-feat-chip">{{ subjectTypeLabel(s) }}</span>
                </td>
                <td>
                  <code v-if="e.pluginClass" class="pp-code pp-code-thin">{{ shortClass(e.pluginClass) }}</code>
                  <span v-else-if="e.isSystem" class="pp-muted">(历史数据)</span>
                  <span v-else class="pp-muted">自定义</span>
                </td>
                <td>
                  <span class="pp-industry-chip" :style="industryChipStyle(resolveIndustry(e))">
                    {{ industryLabel(resolveIndustry(e)) || '—' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Tab: Permissions -->
        <div v-if="activeTab === 'permissions'" class="pp-list">
          <div v-if="!filteredPermissions.length" class="pp-empty">暂无权限</div>
          <div v-for="grp in groupedPermissions" :key="grp.module" class="pp-group">
            <div class="pp-group-header">
              <span class="pp-group-title">
                {{ permissionModuleLabel(grp.module) }}
                <code class="pp-code pp-code-thin" style="margin-left: 6px;">{{ grp.module }}</code>
              </span>
              <span class="pp-group-badge">{{ grp.items.length }}</span>
            </div>
            <table class="pp-table">
              <thead>
                <tr>
                  <th>权限码</th>
                  <th>名称</th>
                  <th>类型</th>
                  <th>范围</th>
                  <th>来源插件</th>
                  <th>行业</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in grp.items" :key="p.id">
                  <td><code class="pp-code">{{ p.code || p.permissionCode }}</code></td>
                  <td>{{ p.name || p.permissionName }}</td>
                  <td><span class="pp-muted">{{ permissionTypeLabel(p.type || p.permissionType) }}</span></td>
                  <td><span class="pp-muted">{{ permissionScopeLabel(p.scope) }}</span></td>
                  <td>
                    <code v-if="p.pluginClass" class="pp-code pp-code-thin">{{ shortClass(p.pluginClass) }}</code>
                    <span v-else class="pp-muted">自定义</span>
                  </td>
                  <td>
                    <span class="pp-industry-chip" :style="industryChipStyle(resolveIndustry(p))">
                      {{ industryLabel(resolveIndustry(p)) || '—' }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Tab: Roles -->
        <div v-if="activeTab === 'roles'" class="pp-list">
          <div v-if="!filteredRoles.length" class="pp-empty">暂无角色</div>
          <table v-else class="pp-table">
            <thead>
              <tr>
                <th>代码</th>
                <th>名称</th>
                <th>类型</th>
                <th>级别</th>
                <th>权限数</th>
                <th>来源插件</th>
                <th>行业</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in filteredRoles" :key="r.id">
                <td><code class="pp-code">{{ r.roleCode }}</code></td>
                <td>{{ r.roleName }}</td>
                <td><span class="pp-muted">{{ roleTypeLabel(r.roleType) }}</span></td>
                <td><span class="pp-muted">{{ r.level ?? '-' }}</span></td>
                <td>{{ r.permissionCount ?? r.permissionIds?.length ?? 0 }}</td>
                <td>
                  <code v-if="r.pluginClass" class="pp-code pp-code-thin">{{ shortClass(r.pluginClass) }}</code>
                  <span v-else class="pp-muted">自定义</span>
                </td>
                <td>
                  <span class="pp-industry-chip" :style="industryChipStyle(resolveIndustry(r))">
                    {{ industryLabel(resolveIndustry(r)) || '—' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Tab: Industries -->
        <div v-if="activeTab === 'industries'" class="pp-industries-grid">
          <div v-for="ind in industries" :key="ind.code" class="pp-industry-card" :class="{ 'pp-industry-card-custom': ind.code === 'CUSTOM' }">
            <div class="pp-industry-card-header" :style="{ background: industryColor(ind.code) + '20' }">
              <div class="pp-industry-card-dot" :style="{ background: industryColor(ind.code) }"></div>
              <div>
                <div class="pp-industry-card-name">{{ ind.label }}</div>
                <div class="pp-industry-card-sub">
                  {{ ind.code }}
                  <span v-if="ind.isPlugin !== false"> · v{{ ind.version || '1.0.0' }}</span>
                  <span v-else> · 本地</span>
                </div>
              </div>
              <el-tag size="small" :type="ind.code === 'CUSTOM' ? 'info' : (ind.enabled !== false ? 'success' : 'info')" effect="light" style="margin-left: auto;">
                {{ ind.code === 'CUSTOM' ? '非插件' : (ind.enabled !== false ? '已启用' : '已禁用') }}
              </el-tag>
            </div>
            <div class="pp-industry-card-body">
              <div class="pp-contrib">
                <span class="pp-contrib-label">类型</span>
                <b>{{ ind.types }}</b>
              </div>
              <div class="pp-contrib">
                <span class="pp-contrib-label">关系</span>
                <b>{{ ind.relations }}</b>
              </div>
              <div class="pp-contrib">
                <span class="pp-contrib-label">事件</span>
                <b>{{ ind.events }}</b>
              </div>
              <div class="pp-contrib">
                <span class="pp-contrib-label">角色</span>
                <b>{{ ind.roles || 0 }}</b>
              </div>
              <div class="pp-contrib">
                <span class="pp-contrib-label">权限</span>
                <b>{{ ind.permissions || 0 }}</b>
              </div>
              <div v-if="ind.pluginCount" class="pp-contrib">
                <span class="pp-contrib-label">插件类</span>
                <b>{{ ind.pluginCount }}</b>
              </div>
            </div>
            <div class="pp-industry-card-foot">
              <div class="pp-muted" style="font-size: 11px;">
                依赖: <span v-if="!ind.dependsOn?.length">无</span>
                <template v-else>{{ ind.dependsOn.join(', ') }}</template>
              </div>
              <div v-if="ind.lastStartedAt" class="pp-muted" style="font-size: 10px; margin-top: 2px;">
                最近启动 {{ formatDateShort(ind.lastStartedAt) }}
              </div>
              <div v-if="ind.manifestClass" class="pp-muted" style="font-size: 10px; margin-top: 2px;">
                <code style="background: #f3f4f6; padding: 1px 4px; border-radius: 3px;">{{ shortClass(ind.manifestClass) }}</code>
              </div>
              <!-- 治理操作按钮 (CORE / CUSTOM 不提供, 避免误操作) -->
              <div v-if="ind.code !== 'CORE' && ind.code !== 'CUSTOM'" class="pp-industry-actions" style="margin-top: 8px; display: flex; gap: 6px;">
                <el-button v-if="ind.enabled" size="small" @click.stop="onDisablePlugin(ind.code)">禁用</el-button>
                <el-button v-else size="small" type="primary" @click.stop="onEnablePlugin(ind.code)">启用</el-button>
                <el-button size="small" @click.stop="onShowHealth(ind.code)">健康检查</el-button>
                <el-button size="small" type="danger" plain @click.stop="onUninstallPlugin(ind.code)">卸载</el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab: Tenants (Phase 5 多租户管理) -->
        <div v-if="activeTab === 'tenants'" class="pp-tenants">
          <div class="pp-tenants-header">
            <span style="font-size: 12px; color: #6b7280;">租户 ID:</span>
            <input v-model.number="tenantSelect" type="number" min="1"
                   class="pp-tenant-input" @change="loadTenantPlugins" />
            <el-button size="small" @click="loadTenantPlugins">加载</el-button>
            <span class="pp-muted" style="margin-left: 16px;">
              显示租户 {{ tenantSelect }} 的插件启用状态 (全局安装 × 租户开关 = 生效)
            </span>
          </div>
          <table class="pp-table" style="margin-top: 12px;">
            <thead>
              <tr>
                <th>插件码</th>
                <th>名称</th>
                <th>版本</th>
                <th>全局安装</th>
                <th>租户启用</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in tenantPluginList" :key="p.code">
                <td><code class="pp-code">{{ p.code }}</code></td>
                <td>{{ p.name }}</td>
                <td><span class="pp-muted">v{{ p.version }}</span></td>
                <td>
                  <span class="pp-chip" :class="Number(p.globalEnabled) === 1 ? 'pp-chip-success' : 'pp-chip-info'">
                    {{ Number(p.globalEnabled) === 1 ? '已装' : '未装' }}
                  </span>
                </td>
                <td>
                  <span class="pp-chip" :class="Number(p.tenantEnabled) === 1 ? 'pp-chip-success' : 'pp-chip-info'">
                    {{ Number(p.tenantEnabled) === 1 ? '启用' : '禁用' }}
                  </span>
                </td>
                <td>
                  <el-button v-if="p.code === 'CORE'" size="small" disabled>CORE 锁定</el-button>
                  <el-button v-else-if="Number(p.tenantEnabled) === 1" size="small"
                             @click="onTenantDisable(p.code)">禁用</el-button>
                  <el-button v-else size="small" type="primary"
                             @click="onTenantEnable(p.code)">启用</el-button>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="!tenantPluginList.length" class="pp-empty">暂无数据</div>
        </div>
      </section>
    </div>

    <!-- 健康检查对话框 -->
    <el-dialog v-model="healthDialog.show" :title="`健康检查 - ${healthDialog.code}`" width="500px">
      <div v-if="healthDialog.data" class="pp-health">
        <p><b>状态</b>: <span class="pp-chip" :class="healthDialog.data.status === 'HEALTHY' ? 'pp-chip-success' : 'pp-chip-warning'">{{ healthDialog.data.status }}</span></p>
        <p><b>总贡献</b>: {{ healthDialog.data.totalContributions }}</p>
        <p><b>分项</b>:</p>
        <ul>
          <li v-for="(v, k) in healthDialog.data.contributions" :key="k">
            {{ k }}: {{ v }}
          </li>
        </ul>
        <p v-if="healthDialog.data.package"><b>版本</b>: v{{ healthDialog.data.package.version }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { LayoutGrid, Link2, Bell, Shield, UserCog, Package, Building2 } from 'lucide-vue-next'
import { pluginPlatformApi, type TenantPluginRow, type PluginHealthInfo } from '@/api/pluginPlatform'
import { ElMessageBox } from 'element-plus'
import { http } from '@/utils/request'

// ───────── State ─────────
const loading = ref(false)
const activeTab = ref<'types' | 'relations' | 'events' | 'permissions' | 'roles' | 'industries' | 'tenants'>('types')

// ═══ Phase 6 生命周期治理 + Phase 5 租户管理 ═══
const tenantSelect = ref(1)
const tenantPluginList = ref<TenantPluginRow[]>([])
const healthDialog = ref<{ show: boolean; code: string; data: PluginHealthInfo | null }>({
  show: false, code: '', data: null
})

async function loadTenantPlugins() {
  try {
    const data = await pluginPlatformApi.tenantPlugins(tenantSelect.value)
    tenantPluginList.value = Array.isArray(data) ? data : []
  } catch (e: any) {
    ElMessage.error('加载租户插件失败: ' + (e?.message || e))
    tenantPluginList.value = []
  }
}

async function onEnablePlugin(code: string) {
  try {
    await pluginPlatformApi.enable(code)
    ElMessage.success(`${code} 已启用`)
    await loadAll()
  } catch (e: any) {
    ElMessage.error('启用失败: ' + (e?.message || e))
  }
}

async function onDisablePlugin(code: string) {
  try {
    await ElMessageBox.confirm(`确认禁用插件 ${code}?`, '提示', { type: 'warning' })
    await pluginPlatformApi.disable(code)
    ElMessage.success(`${code} 已禁用`)
    await loadAll()
  } catch (e: any) {
    if (e === 'cancel') return
    ElMessage.error('禁用失败: ' + (e?.message || e))
  }
}

async function onUninstallPlugin(code: string) {
  try {
    await ElMessageBox.confirm(
      `确认卸载插件 ${code}? 这将级联软删此插件贡献的所有资源 (types/roles/perms 等). CUSTOM 资源不受影响.`,
      '危险操作', { type: 'error', confirmButtonText: '确认卸载', cancelButtonText: '取消' })
    const data: any = await pluginPlatformApi.uninstall(code)
    ElMessage.success(data?.message || `${code} 已卸载`)
    await loadAll()
  } catch (e: any) {
    if (e === 'cancel') return
    ElMessage.error('卸载失败: ' + (e?.message || e))
  }
}

async function onShowHealth(code: string) {
  try {
    const data = await pluginPlatformApi.health(code)
    healthDialog.value = { show: true, code, data: data as PluginHealthInfo }
  } catch (e: any) {
    ElMessage.error('健康检查失败: ' + (e?.message || e))
  }
}

async function onTenantEnable(code: string) {
  try {
    await pluginPlatformApi.enableForTenant(tenantSelect.value, code, '')
    ElMessage.success(`租户 ${tenantSelect.value} 的 ${code} 已启用`)
    await loadTenantPlugins()
  } catch (e: any) {
    ElMessage.error('启用失败: ' + (e?.message || e))
  }
}

async function onTenantDisable(code: string) {
  try {
    await ElMessageBox.confirm(`确认为租户 ${tenantSelect.value} 禁用 ${code}?`, '提示', { type: 'warning' })
    await pluginPlatformApi.disableForTenant(tenantSelect.value, code, '')
    ElMessage.success(`租户 ${tenantSelect.value} 的 ${code} 已禁用`)
    await loadTenantPlugins()
  } catch (e: any) {
    if (e === 'cancel') return
    ElMessage.error('禁用失败: ' + (e?.message || e))
  }
}
// 切换 Tab 时清掉层级筛选(它只对关系有意义)
watch(activeTab, () => { tierFilter.value = '' })
const industryFilter = ref('')
const tierFilter = ref('')
const searchText = ref('')

const types = ref<any[]>([])
const relations = ref<any[]>([])
const events = ref<any[]>([])
const permissions = ref<any[]>([])
const roles = ref<any[]>([])

// 新 API overview 数据(权威行业包信息)
const overview = ref<any>(null)

// ───────── Loading ─────────
async function loadAll() {
  loading.value = true
  try {
    // entity-type-configs 要求 entityType 参数,分别取 USER/ORG_UNIT/PLACE 三类合并
    const typeReqs = ['USER', 'ORG_UNIT', 'PLACE'].map(et =>
      http.get('/entity-type-configs', { params: { entityType: et } }).catch(() => [])
    )
    const [ov, r, e, p, ro, ...typeResults] = await Promise.all([
      http.get('/plugin-platform/overview').catch(() => null),
      http.get('/relation-types').catch(() => []),
      // /event/types 返回 [{categoryCode, categoryName, types:[...]}, ...] 需要打平
      http.get('/event/types').catch(() => []),
      http.get('/permissions', { params: { pageSize: 1000 } }).catch(() => []),
      http.get('/roles', { params: { pageSize: 200 } }).catch(() => []),
      ...typeReqs
    ])
    overview.value = ov
    types.value = typeResults.flatMap((t: any) =>
      Array.isArray(t) ? t : (t?.records || t?.list || [])
    )
    relations.value = Array.isArray(r) ? r : (r?.records || r?.list || [])
    // 事件 API 返回分组结构,打平为类型列表
    const eventRaw: any = Array.isArray(e) ? e : (e?.records || e?.list || [])
    events.value = eventRaw.flatMap((g: any) => Array.isArray(g?.types) ? g.types : [g])
    permissions.value = Array.isArray(p) ? p : (p?.records || p?.list || [])
    roles.value = Array.isArray(ro) ? ro : (ro?.records || ro?.list || [])
  } catch (err: any) {
    ElMessage.error('加载失败: ' + (err?.message || err))
  } finally {
    loading.value = false
  }
}
onMounted(loadAll)

// ───────── Stats ─────────
// 优先用 overview summary(权威),回退到前端聚合
const typeStats = computed(() => ({
  total: overview.value?.summary?.types ?? types.value.length,
  plugin: types.value.filter(t => t.isPluginRegistered || t.pluginClass).length
}))
const relationStats = computed(() => ({
  total: overview.value?.summary?.relations ?? relations.value.length,
  plugin: relations.value.filter(r => r.registeredBy && r.registeredBy !== 'admin').length
}))
const eventStats = computed(() => ({
  total: overview.value?.summary?.events ?? events.value.length,
  // 插件声明 = industry+plugin_class(已迁移) 或 is_system(历史 core 数据)
  plugin: events.value.filter(e => e.pluginClass || e.industry || e.isSystem).length
}))
const permissionStats = computed(() => ({
  total: overview.value?.summary?.permissions ?? permissions.value.length,
  plugin: permissions.value.filter((p: any) => p.pluginClass || p.industry).length
}))
const roleStats = computed(() => ({
  total: overview.value?.summary?.roles ?? roles.value.length,
  plugin: roles.value.filter((r: any) => r.pluginClass || r.industry).length
}))

// ───────── Industries aggregation ─────────
// 优先使用新 overview API 的权威行业包数据;失败则回退到前端聚合
// Phase 1: 优先用 origin 字段解析 (PLUGIN:CORE@1.0.0 / TENANT:CUSTOM#<id>)
// 向下兼容: 回退到 industry / pluginClass (旧数据)
function parseOrigin(origin?: string): { kind: 'PLUGIN'|'TENANT'|'UNKNOWN', code: string, version?: string } {
  if (!origin) return { kind: 'UNKNOWN', code: '' }
  const m1 = origin.match(/^PLUGIN:([A-Z_]+)@([\w\.\-]+)(?::.*)?$/)
  if (m1) return { kind: 'PLUGIN', code: m1[1], version: m1[2] }
  if (origin.startsWith('TENANT:CUSTOM')) return { kind: 'TENANT', code: 'CUSTOM' }
  return { kind: 'UNKNOWN', code: origin }
}

// 行业解析:origin 优先 → 旧 industry 字段 → 通过 pluginClass 推断 → CUSTOM
// 严格的"属于插件的 industry"只返回 CORE/EDU/HEALTH/CARE 等真实行业包;
// CUSTOM 是独立类别,不归入任何行业包
function resolveIndustry(x: any, from?: string): string {
  // Phase 1 首选 origin
  if (x.origin) {
    const parsed = parseOrigin(x.origin)
    if (parsed.kind === 'PLUGIN') return parsed.code
    if (parsed.kind === 'TENANT') return 'CUSTOM'
  }
  // 回退路径 (向下兼容)
  if (x.industry) return x.industry
  if (from === 'registeredBy') {
    return x.registeredBy === 'CORE' ? 'CORE' : inferIndustryFromRegisteredBy(x.registeredBy)
  }
  if (x.pluginClass) return inferIndustry(x.pluginClass) || ''
  return 'CUSTOM'
}

// 按当前 Tab 动态给出行业维度计数:sidebar 显示的数字 = 此行业在当前 Tab 下可见的条目数
function countByIndustry(code: string): number {
  switch (activeTab.value) {
    case 'types':       return types.value.filter(x => resolveIndustry(x, 'pluginClass') === code).length
    case 'relations':   return relations.value.filter(x => relationIndustry(x) === code).length
    case 'events':      return events.value.filter(x => resolveIndustry(x) === code).length
    case 'permissions': return permissions.value.filter(x => resolveIndustry(x) === code).length
    case 'roles':       return roles.value.filter(x => resolveIndustry(x) === code).length
    default:
      // industries tab / fallback:总和 = 所有维度求和
      return types.value.filter(x => resolveIndustry(x, 'pluginClass') === code).length
        + relations.value.filter(x => relationIndustry(x) === code).length
        + events.value.filter(x => resolveIndustry(x) === code).length
        + permissions.value.filter(x => resolveIndustry(x) === code).length
        + roles.value.filter(x => resolveIndustry(x) === code).length
  }
}

const industries = computed(() => {
  if (overview.value?.industries?.length) {
    const list: any[] = overview.value.industries.map((ind: any) => ({
      code: ind.code,
      label: ind.name || industryLabel(ind.code),
      version: ind.version,
      enabled: ind.enabled,
      dependsOn: typeof ind.dependsOn === 'string' ? JSON.parse(ind.dependsOn || '[]') : (ind.dependsOn || []),
      manifestClass: ind.manifestClass,
      uninstallPolicy: ind.uninstallPolicy,
      installedAt: ind.installedAt,
      lastStartedAt: ind.lastStartedAt,
      types: ind.stats?.types || 0,
      relations: ind.stats?.relations || 0,
      events: ind.stats?.events || 0,
      roles: ind.stats?.roles || 0,
      permissions: ind.stats?.permissions || 0,
      pluginCount: countByIndustry(ind.code),
      isPlugin: true
    }))
    // 合成"自定义"虚拟条目 — 不是插件包,但在 sidebar 占位以便筛选/统计
    const customTypes = types.value.filter(x => resolveIndustry(x, 'pluginClass') === 'CUSTOM').length
    const customRelations = relations.value.filter(x => relationIndustry(x) === 'CUSTOM').length
    const customEvents = events.value.filter(x => resolveIndustry(x) === 'CUSTOM').length
    const customPerms = permissions.value.filter((x: any) => resolveIndustry(x) === 'CUSTOM').length
    const customRoles = roles.value.filter((x: any) => resolveIndustry(x) === 'CUSTOM').length
    const customTotal = customTypes + customRelations + customEvents + customPerms + customRoles
    // 始终显示 CUSTOM 卡(即使暂时为 0),传达"此类别存在"的信息
    list.push({
      code: 'CUSTOM',
      label: '自定义',
      version: '-',
      enabled: true,
      dependsOn: [],
      manifestClass: null,
      uninstallPolicy: '管理员本地创建,独立于插件生命周期',
      installedAt: null,
      lastStartedAt: null,
      types: customTypes,
      relations: customRelations,
      events: customEvents,
      roles: customRoles,
      permissions: customPerms,
      pluginCount: countByIndustry('CUSTOM'),
      isPlugin: false
    })
    return list.sort((a: any, b: any) => {
      if (a.code === 'CORE') return -1
      if (b.code === 'CORE') return 1
      if (a.code === 'CUSTOM') return 1  // CUSTOM 永远排最后
      if (b.code === 'CUSTOM') return -1
      return a.code.localeCompare(b.code)
    })
  }
  // Fallback 聚合(老逻辑)
  const m = new Map<string, any>()
  const add = (code: string, label: string) => {
    if (!m.has(code)) m.set(code, { code, label, types: 0, relations: 0, events: 0, pluginCount: 0, classes: new Set() })
    return m.get(code)!
  }
  for (const t of types.value) {
    const code = inferIndustry(t.pluginClass) || (t.isPluginRegistered ? 'CORE' : 'CUSTOM')
    const g = add(code, industryLabel(code))
    g.types++
    if (t.pluginClass) g.classes.add(t.pluginClass)
  }
  for (const r of relations.value) {
    const code = r.registeredBy === 'CORE' ? 'CORE' : inferIndustryFromRegisteredBy(r.registeredBy)
    const g = add(code, industryLabel(code))
    g.relations++
    if (r.registeredBy) g.classes.add(r.registeredBy)
  }
  for (const ind of m.values()) {
    ind.pluginCount = ind.classes.size
    delete ind.classes
  }
  return Array.from(m.values()).sort((a, b) => {
    if (a.code === 'CORE') return -1
    if (b.code === 'CORE') return 1
    return a.code.localeCompare(b.code)
  })
})

// "全部" 条目数 = 当前 Tab 的原始条目总数(不只是已分类到行业的,还包括未归属的)
const totalPluginCount = computed(() => {
  switch (activeTab.value) {
    case 'types':       return types.value.length
    case 'relations':   return relations.value.length
    case 'events':      return events.value.length
    case 'permissions': return permissions.value.length
    case 'roles':       return roles.value.length
    default: return industries.value.reduce((s, i) => s + i.pluginCount, 0)
  }
})

const tiers = [
  { code: 'CORE', label: '通用核心' },
  { code: 'COMMON_EXT', label: '通用扩展' },
  { code: 'DOMAIN', label: '行业垂直' }
]

// ───────── Helpers ─────────
function inferIndustry(pluginClass?: string): string {
  if (!pluginClass) return ''
  if (pluginClass.includes('.core.') || pluginClass.endsWith('CorePlugin')) return 'CORE'
  if (pluginClass.includes('.education.')) return 'EDU'
  if (pluginClass.includes('.healthcare.')) return 'HEALTH'
  if (pluginClass.includes('.eldercare.')) return 'CARE'
  return ''
}
function inferIndustryFromRegisteredBy(registeredBy?: string): string {
  if (!registeredBy) return 'CUSTOM'
  if (registeredBy === 'CORE') return 'CORE'
  if (registeredBy.toLowerCase().includes('education')) return 'EDU'
  if (registeredBy.toLowerCase().includes('health')) return 'HEALTH'
  return 'CUSTOM'
}
function industryLabel(code?: string): string {
  if (!code) return ''
  return ({
    CORE: '通用核心',
    EDU: '教育行业',
    HEALTH: '医疗行业',
    CARE: '养老行业',
    CUSTOM: '自定义'
  } as any)[code] || code
}
// 主体/实体类型中文(USER / ORG_UNIT / PLACE → 用户 / 组织 / 场所)
function subjectTypeLabel(code?: string): string {
  if (!code) return ''
  return ({
    USER: '用户',
    ORG_UNIT: '组织',
    ORG: '组织',
    PLACE: '场所',
    OTHER: '其他'
  } as any)[code] || code
}
// 权限类型 MENU/OPERATION/DATA → 菜单/操作/数据
function permissionTypeLabel(type?: string): string {
  if (!type) return '-'
  return ({
    MENU: '菜单',
    OPERATION: '操作',
    BUTTON: '按钮',
    API: '接口',
    DATA: '数据'
  } as any)[type] || type
}
// 权限范围 PUBLIC/SELF/MANAGEMENT → 公开/本人/管理
function permissionScopeLabel(scope?: string): string {
  if (!scope) return '-'
  return ({
    PUBLIC: '公开',
    SELF: '本人',
    MANAGEMENT: '管理',
    SYSTEM: '系统'
  } as any)[scope] || scope
}
// 角色类型 PRESET/CUSTOM/SUPER_ADMIN → 预置/自定义/超级管理
function roleTypeLabel(type?: string): string {
  if (!type) return '-'
  return ({
    PRESET: '预置',
    CUSTOM: '自定义',
    SUPER_ADMIN: '超级管理',
    SYSTEM_ADMIN: '系统管理',
    SYSTEM: '系统'
  } as any)[type] || type
}
// 关系层级 CORE/COMMON_EXT/DOMAIN → 通用核心/通用扩展/行业垂直
function tierLabel(tier?: string): string {
  if (!tier) return '-'
  return ({
    CORE: '通用核心',
    COMMON_EXT: '通用扩展',
    DOMAIN: '行业垂直'
  } as any)[tier] || tier
}
function industryColor(code: string): string {
  return ({
    CORE: '#2563eb',
    EDU: '#d97706',
    HEALTH: '#be185d',
    CARE: '#059669',
    CUSTOM: '#6b7280'
  } as any)[code] || '#6b7280'
}
// 浅底 + 彩色边框 + 彩色文字 chip 样式(比 el-tag :color 实心背景柔和)
function industryChipStyle(code?: string): Record<string, string> {
  if (!code) return {
    color: '#94a3b8', borderColor: '#e2e8f0', background: '#f8fafc'
  }
  const c = industryColor(code)
  return {
    color: c,
    borderColor: c + '60',
    background: c + '12'
  }
}
// 关系行业推断:origin 优先, 回退 registeredBy + 代码推断
function relationIndustry(r: any): string {
  if (r.origin) {
    const parsed = parseOrigin(r.origin)
    if (parsed.kind === 'PLUGIN') return parsed.code
    if (parsed.kind === 'TENANT') return 'CUSTOM'
  }
  if (r.industry) return r.industry
  if (!r.registeredBy) return 'CUSTOM'
  if (r.registeredBy === 'CORE') return 'CORE'
  if (r.registeredBy === 'admin' || r.registeredBy === 'CUSTOM') return 'CUSTOM'
  const rb = String(r.registeredBy || '').toLowerCase()
  if (rb.includes('education') || rb.includes('edu')) return 'EDU'
  if (rb.includes('health')) return 'HEALTH'
  if (rb.includes('care')) return 'CARE'
  const code = String(r.relationCode || '').toLowerCase()
  if (/student|class|dorm|teacher|parent|academic|counselor|grade/.test(code)) return 'EDU'
  return 'CORE'
}
function tierTagType(tier?: string): 'primary' | 'success' | 'warning' | 'info' {
  return ({ CORE: 'primary', COMMON_EXT: 'warning', DOMAIN: 'success' } as any)[tier || ''] || 'info'
}
function categoryTagType(cat?: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  return ({
    OWNERSHIP: 'warning', MEMBERSHIP: 'primary', ASSOCIATION: 'success',
    DELEGATION: 'danger', SUBSCRIPTION: 'info'
  } as any)[cat || ''] || 'info'
}
function categoryLabel(cat?: string): string {
  return ({ OWNERSHIP: '管理', MEMBERSHIP: '成员', ASSOCIATION: '关联', DELEGATION: '委托', SUBSCRIPTION: '订阅' } as any)[cat || ''] || cat || '-'
}
function polarityTagType(p?: string): 'success' | 'warning' | 'danger' | 'info' {
  return ({ POSITIVE: 'success', NEGATIVE: 'danger', NEUTRAL: 'info' } as any)[p || ''] || 'info'
}
function polarityLabel(p?: string): string {
  return ({ POSITIVE: '正向', NEGATIVE: '负向', NEUTRAL: '中性' } as any)[p || ''] || p || '-'
}
function shortClass(fqcn: string): string {
  return fqcn.split('.').pop() || fqcn
}
function formatDateShort(ts: string | null | undefined): string {
  if (!ts) return '-'
  const d = new Date(ts)
  if (isNaN(d.getTime())) return String(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getMonth() + 1}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
function countFields(t: any): number {
  try {
    const schema = typeof t.metadataSchema === 'string' ? JSON.parse(t.metadataSchema) : t.metadataSchema
    return schema?.fields?.length || 0
  } catch { return 0 }
}
function topFeatures(t: any): string[] {
  const f = t.features
  if (!f) return []
  const obj = typeof f === 'string' ? (() => { try { return JSON.parse(f) } catch { return {} } })() : f
  return Object.entries(obj).filter(([_, v]) => v === true).map(([k]) => k).slice(0, 3)
}
function parseSubjects(raw: any): string[] {
  if (!raw) return []
  if (Array.isArray(raw)) return raw
  try { return JSON.parse(raw) } catch { return [String(raw)] }
}

// ───────── Filtering ─────────
function matchesFilters(row: any, opts: { industryFrom?: string; tierField?: string; searchFields?: string[] }) {
  if (industryFilter.value) {
    // 用统一的 resolveIndustry 推断,关系用 relationIndustry
    const ind = opts.industryFrom === 'registeredBy' ? relationIndustry(row) : resolveIndustry(row, opts.industryFrom)
    if (ind !== industryFilter.value) return false
  }
  if (tierFilter.value && opts.tierField) {
    if ((row as any)[opts.tierField] !== tierFilter.value) return false
  }
  if (searchText.value.trim()) {
    const kw = searchText.value.trim().toLowerCase()
    const fields = (opts.searchFields || []).map(f => String((row as any)[f] || '').toLowerCase())
    if (!fields.some(f => f.includes(kw))) return false
  }
  return true
}

const filteredTypes = computed(() => types.value.filter(t =>
  matchesFilters(t, { industryFrom: 'pluginClass', searchFields: ['typeCode', 'typeName', 'pluginClass', 'entityType'] })
))
const filteredRelations = computed(() => relations.value.filter(r =>
  matchesFilters(r, { industryFrom: 'registeredBy', tierField: 'tier', searchFields: ['relationCode', 'relationName', 'registeredBy'] })
))
const filteredEvents = computed(() => events.value.filter(e =>
  matchesFilters(e, { searchFields: ['typeCode', 'typeName', 'categoryCode'] })
))
const filteredPermissions = computed(() => permissions.value.filter(p =>
  matchesFilters(p, { searchFields: ['code', 'permissionCode', 'name', 'permissionName', 'module'] })
))
const filteredRoles = computed(() => roles.value.filter(r =>
  matchesFilters(r, { searchFields: ['roleCode', 'roleName', 'roleType'] })
))

const filteredCount = computed(() => {
  return ({
    types: filteredTypes.value.length,
    relations: filteredRelations.value.length,
    events: filteredEvents.value.length,
    permissions: filteredPermissions.value.length,
    roles: filteredRoles.value.length,
    industries: industries.value.length
  } as any)[activeTab.value] || 0
})

// Grouped types by entityType + tier indicator
const groupedTypes = computed(() => {
  const m = new Map<string, any>()
  for (const t of filteredTypes.value) {
    const key = t.entityType || 'OTHER'
    if (!m.has(key)) m.set(key, {
      key, entityType: key,
      tier: t.isPluginRegistered ? 'CORE' : 'CUSTOM',
      tierLabel: t.isPluginRegistered ? '插件注册' : '管理员自定义',
      items: []
    })
    m.get(key).items.push(t)
  }
  return Array.from(m.values())
})

// 权限模块英文 → 中文 (permission code prefix → 模块名)
const PERMISSION_MODULE_LABELS: Record<string, string> = {
  academic: '学术',
  access: '权限',
  asset: '资产',
  attendance: '考勤',
  check: '打卡',
  dashboard: '首页',
  discipline: '纪律',
  dormitory: '宿舍',
  evaluation: '评价',
  event: '事件',
  file: '文件',
  grade: '成绩',
  inspection: '检查',
  log: '日志',
  member: '成员',
  message: '消息',
  my: '我的',
  organization: '组织',
  permission: '权限',
  place: '场所',
  plugin: '插件',
  public: '公开',
  rating: '评分',
  relation: '关系',
  role: '角色',
  schedule: '课表',
  scoring: '评分',
  student: '学生',
  system: '系统',
  tag: '标签',
  task: '任务',
  teacher: '教师',
  teaching: '教务',
  template: '模板',
  tenant: '租户',
  user: '用户',
  weight: '权重',
  weixin: '微信',
  workflow: '流程'
}
function permissionModuleLabel(module: string): string {
  return PERMISSION_MODULE_LABELS[module] || module
}
// Grouped permissions by module
const groupedPermissions = computed(() => {
  const m = new Map<string, any>()
  for (const p of filteredPermissions.value) {
    const code = p.code || p.permissionCode || ''
    const moduleName = code.split(':')[0] || 'unknown'
    if (!m.has(moduleName)) m.set(moduleName, { module: moduleName, items: [] })
    m.get(moduleName).items.push(p)
  }
  return Array.from(m.values()).sort((a, b) => a.module.localeCompare(b.module))
})
</script>

<style scoped>
.pp-page { padding: 20px; max-width: 1600px; margin: 0 auto; }
.pp-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.pp-title { font-size: 18px; font-weight: 700; color: #111827; margin: 0; }
.pp-subtitle { font-size: 12px; color: #6b7280; margin: 4px 0 0; max-width: 700px; }

/* Stats row */
.pp-stats { display: grid; grid-template-columns: repeat(6, 1fr); gap: 10px; margin-bottom: 16px; }
.stat-card {
  display: flex; align-items: center; gap: 10px;
  background: #fff; border: 1px solid #e5e7eb; border-radius: 8px;
  padding: 12px 14px; cursor: pointer; transition: all 0.15s;
}
.stat-card:hover { border-color: #93c5fd; }
.stat-card.active { border-color: #2563eb; box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12); }
.stat-icon { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; }
.stat-icon :deep(svg) { width: 18px; height: 18px; }
.stat-body { flex: 1; min-width: 0; }
.stat-label { font-size: 11px; color: #6b7280; font-weight: 500; }
.stat-count { font-size: 16px; color: #111827; margin-top: 2px; line-height: 1.2; }
.stat-count b { font-weight: 700; margin-right: 3px; }
.stat-count span { font-size: 11px; color: #9ca3af; font-weight: 400; }

/* Main */
.pp-main { display: grid; grid-template-columns: 200px 1fr; gap: 16px; }

/* Sidebar */
.pp-sidebar { display: flex; flex-direction: column; gap: 2px; }
.pp-sidebar-title {
  font-size: 10px; font-weight: 700; text-transform: uppercase;
  color: #9ca3af; letter-spacing: 0.5px; padding: 8px 8px 4px;
}
.pp-industry {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 10px; border: none; background: transparent;
  border-radius: 5px; cursor: pointer; text-align: left;
  font-size: 12px; color: #374151; transition: background 0.1s;
}
.pp-industry:hover { background: #f3f4f6; }
.pp-industry.active { background: #eff6ff; color: #2563eb; font-weight: 500; }
.pp-industry-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.pp-industry-name { flex: 1; }
.pp-industry-count { font-size: 10px; color: #9ca3af; }

/* Content */
.pp-content {
  background: #fff; border: 1px solid #e5e7eb; border-radius: 8px;
  padding: 14px; min-height: 400px;
}
.pp-toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.pp-search {
  flex: 1; height: 30px; padding: 0 12px;
  border: 1px solid #e5e7eb; border-radius: 5px;
  font-size: 12px; outline: none;
}
.pp-search:focus { border-color: #2563eb; }
.pp-count { font-size: 11px; color: #9ca3af; }

/* List */
.pp-list { display: flex; flex-direction: column; gap: 14px; }
.pp-group { border: 1px solid #f3f4f6; border-radius: 6px; overflow: hidden; }
.pp-group-header {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; background: #f9fafb; border-bottom: 1px solid #f3f4f6;
}
.pp-group-title {
  font-size: 11px; font-weight: 700; color: #4b5563;
  letter-spacing: 0.5px; text-transform: uppercase;
}
.pp-group-title code {
  text-transform: none; letter-spacing: normal;
}
.pp-group-badge {
  font-size: 10px; color: #6b7280; background: #fff;
  padding: 2px 6px; border-radius: 4px; border: 1px solid #e5e7eb;
  margin-left: auto;
}

/* Table */
.pp-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.pp-table thead th {
  text-align: left; font-weight: 500; color: #6b7280; font-size: 11px;
  text-transform: uppercase; letter-spacing: 0.3px;
  padding: 10px 12px; border-bottom: 1px solid #e5e7eb;
  background: #fafbfc;
}
.pp-table tbody td {
  padding: 8px 12px; border-bottom: 1px solid #f3f4f6;
  color: #111827; vertical-align: middle;
}
.pp-table tbody tr:hover { background: #f9fafb; }
.pp-code {
  font-family: 'JetBrains Mono', Menlo, monospace;
  font-size: 11px; color: #2563eb;
  background: #eff6ff; padding: 1px 6px; border-radius: 3px;
}
.pp-code-thin { color: #6b7280; background: #f9fafb; }
.pp-muted { color: #9ca3af; font-size: 11px; }
.pp-arrow { margin: 0 4px; color: #d1d5db; font-weight: 600; }
.pp-icon-circle {
  display: inline-flex; align-items: center; justify-content: center;
  width: 20px; height: 20px; border-radius: 50%;
  color: #fff; font-weight: 600; font-size: 10px;
  margin-right: 6px;
}
.pp-evt-icon {
  display: inline-flex; width: 14px; height: 14px;
  border-radius: 50%; margin-right: 6px;
  align-items: center; justify-content: center; font-size: 6px;
}
.pp-feat-chip {
  display: inline-block; font-size: 10px; color: #6b7280;
  background: #f3f4f6; padding: 1px 6px; border-radius: 3px;
  margin-right: 3px;
}
.pp-bound-chip {
  display: inline-block; font-size: 10px; font-weight: 500;
  padding: 2px 6px; border-radius: 3px;
}
.pp-bound-cap { background: #fef3c7; color: #92400e; }
.pp-bound-max { background: #e0e7ff; color: #3730a3; }

/* 统一 chip 基础样式 — 所有分类标签/行业标签/层级/极性都用此基座,风格一致 */
.pp-industry-chip,
.pp-chip {
  display: inline-flex; align-items: center;
  font-size: 11px; font-weight: 500; letter-spacing: 0.2px;
  padding: 2px 8px; border-radius: 10px;
  border: 1px solid;
  line-height: 1.5;
  white-space: nowrap;
}
/* 预定义语义色板 — 对应 tierTagType / categoryTagType 等返回的 type */
.pp-chip-primary { color: #2563eb; border-color: #93c5fd; background: #eff6ff; }
.pp-chip-success { color: #059669; border-color: #a7f3d0; background: #ecfdf5; }
.pp-chip-warning { color: #d97706; border-color: #fcd34d; background: #fffbeb; }
.pp-chip-danger  { color: #dc2626; border-color: #fca5a5; background: #fef2f2; }
.pp-chip-info    { color: #6b7280; border-color: #e5e7eb; background: #f9fafb; }

.pp-tenants {
  padding: 4px 0;
}
.pp-tenants-header {
  display: flex; align-items: center; gap: 10px; padding: 8px 0;
}
.pp-tenant-input {
  width: 80px; height: 30px; padding: 0 10px;
  border: 1px solid #e5e7eb; border-radius: 5px; font-size: 13px; outline: none;
}
.pp-tenant-input:focus { border-color: #2563eb; }
.pp-industry-actions {
  padding-top: 6px; border-top: 1px dashed #f3f4f6;
}
.pp-health ul {
  list-style: circle inside; margin: 4px 0 8px 0; padding: 0;
}
.pp-health li { font-size: 13px; padding: 2px 0; }

.pp-empty {
  text-align: center; padding: 40px 12px;
  color: #9ca3af; font-size: 13px;
}

/* Industry cards */
.pp-industries-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}
.pp-industry-card {
  border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden;
  background: #fff; transition: all 0.15s;
}
.pp-industry-card:hover { border-color: #93c5fd; box-shadow: 0 4px 12px rgba(0,0,0,0.04); }
.pp-industry-card-header {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px;
}
.pp-industry-card-dot { width: 10px; height: 10px; border-radius: 50%; }
.pp-industry-card-name { font-size: 13px; font-weight: 600; color: #111827; }
.pp-industry-card-sub { font-size: 10px; color: #9ca3af; margin-top: 1px; }
.pp-industry-card-body {
  display: grid; grid-template-columns: 1fr 1fr;
  gap: 2px; padding: 10px 14px;
  background: #fafbfc;
}
.pp-contrib {
  display: flex; justify-content: space-between; align-items: baseline;
  padding: 4px 0; font-size: 12px;
}
.pp-contrib-label { color: #6b7280; }
.pp-contrib b { color: #111827; font-weight: 600; }
.pp-industry-card-foot { padding: 8px 14px; border-top: 1px solid #f3f4f6; }
</style>