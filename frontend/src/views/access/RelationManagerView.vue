<template>
  <div class="p-4">
    <!-- 模式切换: 场景 vs 数据表 -->
    <div class="flex items-center justify-between mb-4">
      <div>
        <h2 class="text-lg font-semibold">关系管理</h2>
        <p class="text-xs text-gray-500 mt-1">绑定人与班级、场所、其他人的关系</p>
      </div>
      <div class="flex gap-1 text-sm">
        <button :class="viewBtnCls(viewMode === 'scene')" @click="viewMode = 'scene'">业务场景</button>
        <button :class="viewBtnCls(viewMode === 'data')" @click="viewMode = 'data'">数据表</button>
      </div>
    </div>

    <!-- 场景视图 -->
    <div v-if="viewMode === 'scene'">
      <h3 class="text-sm font-medium text-gray-700 mb-3">常用场景</h3>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-3 mb-6">
        <button v-for="sc in scenes" :key="sc.code" class="scene-card" @click="openScene(sc.code)">
          <div class="scene-icon" :style="{ color: sc.color }">{{ sc.emoji }}</div>
          <div class="scene-title">{{ sc.title }}</div>
          <div class="scene-desc">{{ sc.desc }}</div>
        </button>
      </div>

      <h3 class="text-sm font-medium text-gray-700 mb-3">最近操作</h3>
      <div class="recent-list">
        <div v-for="r in recentRows" :key="r.id" class="recent-item">
          <span class="recent-when">{{ timeAgo(r.createdAt) }}</span>
          <span class="recent-actor">{{ displayName(r, 'subject') }}</span>
          <span class="recent-arrow">—{{ relationLabel(r.relation) }}→</span>
          <span class="recent-target">{{ displayName(r, 'resource') }}</span>
          <button class="recent-revoke" @click="revoke(r)">撤销</button>
        </div>
        <div v-if="!recentRows.length" class="text-center text-gray-400 py-8 text-sm">暂无关系数据</div>
      </div>
    </div>

    <!-- 数据表视图(原平铺表) -->
    <div v-else>
      <el-card class="mb-3" shadow="never">
        <el-form :inline="true" size="small">
          <el-form-item label="资源类型">
            <el-select v-model="filter.resourceType" clearable placeholder="全部" style="width: 120px" @change="load">
              <el-option v-for="t in ['org_unit','place','user']" :key="t" :label="typeLabel(t)" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="关系">
            <el-select v-model="filter.relation" clearable placeholder="全部" style="width: 150px" @change="load">
              <el-option v-for="r in relationOptions" :key="`${r.relationCode}:${r.fromType}:${r.toType}`"
                :label="`${r.relationName} (${r.relationCode})`" :value="r.relationCode" />
            </el-select>
          </el-form-item>
          <el-form-item label="主体类型">
            <el-select v-model="filter.subjectType" clearable placeholder="全部" style="width: 120px" @change="load">
              <el-option v-for="t in ['user','org_unit','place']" :key="t" :label="typeLabel(t)" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-table :data="rows" stripe size="small" v-loading="loading">
        <el-table-column label="主体" min-width="200">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ typeLabel(row.subjectType) }}</el-tag>
            <span class="ml-2">{{ displayName(row, 'subject') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="关系" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="relationTagType(row.relation)">{{ relationLabel(row.relation) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="资源" min-width="200">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ typeLabel(row.resourceType) }}</el-tag>
            <span class="ml-2">{{ displayName(row, 'resource') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="remark" show-overflow-tooltip />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" text @click="revoke(row)">撤销</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="mt-3 flex justify-end" :current-page="page" :page-size="pageSize" :total="total"
        layout="total, prev, pager, next" @current-change="p => { page = p; load() }" />
    </div>

    <!-- 向导 Dialog -->
    <el-dialog v-model="wizardVisible" :title="wizardTitle" width="560px" :close-on-click-modal="false">
      <div v-if="wizardCode" class="wizard-form">
        <!-- 步骤 1: 选择主体 (谁) -->
        <div class="field">
          <label class="field-label">{{ wizardMeta.subjectLabel }}</label>
          <el-select v-if="wizardMeta.subjectType === 'user'"
            v-model="wiz.subjectId" filterable remote :remote-method="searchUsers"
            :loading="searchLoading" placeholder="输入姓名/工号搜索..." style="width:100%" @change="onWizChange">
            <el-option v-for="u in userSearchResults" :key="u.id" :label="`${u.realName} (${u.username})`" :value="u.id" />
          </el-select>
          <el-cascader v-else-if="wizardMeta.subjectType === 'org_unit'"
            v-model="wiz.subjectId" :options="orgTreeOpts" :props="cascaderProps" placeholder="选择组织" style="width:100%" @change="onWizChange" />
        </div>

        <!-- 步骤 2: 选择资源 (对象) -->
        <div class="field">
          <label class="field-label">{{ wizardMeta.resourceLabel }}</label>
          <el-cascader v-if="wizardMeta.resourceType === 'org_unit'"
            v-model="wiz.resourceId" :options="orgTreeOpts" :props="cascaderProps"
            placeholder="选择组织" style="width:100%" @change="onWizChange" />
          <el-select v-else-if="wizardMeta.resourceType === 'user'"
            v-model="wiz.resourceId" filterable remote :remote-method="searchUsersForResource"
            :loading="searchLoading2" placeholder="输入姓名/工号搜索..." style="width:100%" @change="onWizChange">
            <el-option v-for="u in userSearchResults2" :key="u.id" :label="`${u.realName} (${u.username})`" :value="u.id" />
          </el-select>
          <el-select v-else-if="wizardMeta.resourceType === 'place'"
            v-model="wiz.resourceId" filterable remote :remote-method="searchPlaces"
            :loading="searchLoading3" placeholder="输入场所名称搜索..." style="width:100%" @change="onWizChange">
            <el-option v-for="p in placeSearchResults" :key="p.id" :label="p.placeName" :value="p.id" />
          </el-select>
        </div>

        <div class="field">
          <label class="field-label">备注（可选）</label>
          <el-input v-model="wiz.remark" placeholder="描述这个关系的用途" />
        </div>

        <!-- 预览 -->
        <div v-if="wiz.subjectId && wiz.resourceId" class="preview-panel">
          <div class="preview-label">预览</div>
          <div class="preview-line">
            <strong>{{ wiz.subjectName || '所选主体' }}</strong>
            将成为
            <strong>{{ wiz.resourceName || '所选资源' }}</strong>
            的<em>{{ relationLabel(wizardMeta.relation) }}</em>
          </div>
          <div v-if="existingConflict" class="preview-warning">
            ⚠️ 该资源已有 {{ existingConflict.count }} 个{{ relationLabel(wizardMeta.relation) }}，建议先撤销原有。
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="wizardVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!wiz.subjectId || !wiz.resourceId" @click="handleWizardSubmit">
          确认绑定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/utils/request'
import { accessRelationApi } from '@/api/accessRelation'
import { relationTypeApi, type RelationTypeDef } from '@/api/relationType'

const route = useRoute()
const viewMode = ref<'scene' | 'data'>('scene')

// ─── 场景定义 ───
const scenes = [
  { code: 'ASSIGN_CLASS_ADMIN', title: '指定班主任', desc: '给班级或年级绑定主管理员', emoji: '👨‍🏫', color: '#f59e0b' },
  { code: 'ADD_GUARDIAN',       title: '添加监护人',   desc: '为学生绑定家长监护关系',   emoji: '👨‍👩‍👧', color: '#2563eb' },
  { code: 'ASSIGN_PLACE_ADMIN', title: '场所负责人',   desc: '指定教室/宿舍/实验室负责人', emoji: '🏢', color: '#10b981' },
  { code: 'ADD_MEMBER',         title: '加入组织',     desc: '把用户加入某组织作为成员',  emoji: '👥', color: '#8b5cf6' },
  { code: 'PLACE_BELONGS_ORG',  title: '场所归属',     desc: '绑定场所到某组织',         emoji: '🏫', color: '#06b6d4' },
]

const sceneMap: Record<string, {
  relation: string; subjectType: string; resourceType: string;
  subjectLabel: string; resourceLabel: string;
}> = {
  ASSIGN_CLASS_ADMIN: { relation: 'admin',       subjectType: 'user',     resourceType: 'org_unit', subjectLabel: '选择老师（作为班主任）', resourceLabel: '选择班级/年级' },
  ADD_GUARDIAN:       { relation: 'guardian_of', subjectType: 'user',     resourceType: 'user',     subjectLabel: '选择家长',              resourceLabel: '选择学生' },
  ASSIGN_PLACE_ADMIN: { relation: 'admin',       subjectType: 'user',     resourceType: 'place',    subjectLabel: '选择负责人',            resourceLabel: '选择场所' },
  ADD_MEMBER:         { relation: 'member',      subjectType: 'user',     resourceType: 'org_unit', subjectLabel: '选择用户',              resourceLabel: '选择组织' },
  PLACE_BELONGS_ORG:  { relation: 'belongs_to',  subjectType: 'place',    resourceType: 'org_unit', subjectLabel: '选择场所',              resourceLabel: '归属组织' },
}

// ─── 向导状态 ───
const wizardVisible = ref(false)
const wizardCode = ref<string>('')
const wizardMeta = computed(() => wizardCode.value ? sceneMap[wizardCode.value] : null as any)
const wizardTitle = computed(() => wizardCode.value
  ? (scenes.find(s => s.code === wizardCode.value)?.title || '新建关系')
  : '')
const wiz = ref<any>({
  subjectId: null, subjectName: '',
  resourceId: null, resourceName: '',
  remark: ''
})
const existingConflict = ref<{ count: number } | null>(null)

function openScene(code: string) {
  wizardCode.value = code
  wiz.value = { subjectId: null, subjectName: '', resourceId: null, resourceName: '', remark: '' }
  existingConflict.value = null
  wizardVisible.value = true
}

async function onWizChange() {
  // 更新显示名字
  if (wiz.value.subjectId && wizardMeta.value?.subjectType === 'user') {
    const u = userSearchResults.value.find(x => x.id === wiz.value.subjectId)
    if (u) wiz.value.subjectName = u.realName
  } else if (wiz.value.subjectId && wizardMeta.value?.subjectType === 'org_unit') {
    wiz.value.subjectName = findOrgName(wiz.value.subjectId)
  }
  if (wiz.value.resourceId) {
    if (wizardMeta.value?.resourceType === 'org_unit') wiz.value.resourceName = findOrgName(wiz.value.resourceId)
    else if (wizardMeta.value?.resourceType === 'user') {
      const u = userSearchResults2.value.find(x => x.id === wiz.value.resourceId)
      if (u) wiz.value.resourceName = u.realName
    } else if (wizardMeta.value?.resourceType === 'place') {
      const p = placeSearchResults.value.find(x => x.id === wiz.value.resourceId)
      if (p) wiz.value.resourceName = p.placeName
    }
  }
  // 检测冲突: 同 resource+relation 已经有其他 subject
  if (wiz.value.resourceId && wizardMeta.value && ['admin', 'deputy'].includes(wizardMeta.value.relation)) {
    try {
      const resp: any = await http.get('/access-relations', {
        params: { resourceType: wizardMeta.value.resourceType, resourceId: wiz.value.resourceId }
      })
      const existing = (resp?.records || []).filter((r: any) =>
        r.relation === wizardMeta.value.relation && String(r.subjectId) !== String(wiz.value.subjectId))
      existingConflict.value = existing.length > 0 ? { count: existing.length } : null
    } catch { existingConflict.value = null }
  }
}

async function handleWizardSubmit() {
  const m = wizardMeta.value
  if (!m) return
  try {
    await accessRelationApi.create({
      resourceType: m.resourceType,
      resourceId: wiz.value.resourceId!,
      relation: m.relation,
      subjectType: m.subjectType,
      subjectId: wiz.value.subjectId!,
      accessLevel: 'FULL',
      remark: wiz.value.remark || undefined
    } as any)
    ElMessage.success('关系已创建')
    wizardVisible.value = false
    await Promise.all([loadRecent(), load()])
  } catch (e: any) {
    ElMessage.error('创建失败: ' + (e?.message || e))
  }
}

// ─── 搜索 ───
const searchLoading = ref(false)
const searchLoading2 = ref(false)
const searchLoading3 = ref(false)
const userSearchResults = ref<Array<{ id: any; realName: string; username: string }>>([])
const userSearchResults2 = ref<Array<{ id: any; realName: string; username: string }>>([])
const placeSearchResults = ref<Array<{ id: any; placeName: string }>>([])

async function searchUsers(kw: string) {
  if (!kw) { userSearchResults.value = []; return }
  searchLoading.value = true
  try {
    const resp: any = await http.get('/users', { params: { keyword: kw, pageNum: 1, pageSize: 20 } })
    const list = resp?.records || resp?.list || resp || []
    userSearchResults.value = list.map((u: any) => ({ id: u.id, realName: u.realName || u.real_name || '-', username: u.username }))
  } finally { searchLoading.value = false }
}
async function searchUsersForResource(kw: string) {
  if (!kw) { userSearchResults2.value = []; return }
  searchLoading2.value = true
  try {
    const resp: any = await http.get('/users', { params: { keyword: kw, pageNum: 1, pageSize: 20 } })
    const list = resp?.records || resp?.list || resp || []
    userSearchResults2.value = list.map((u: any) => ({ id: u.id, realName: u.realName || u.real_name || '-', username: u.username }))
  } finally { searchLoading2.value = false }
}
async function searchPlaces(kw: string) {
  if (!kw) { placeSearchResults.value = []; return }
  searchLoading3.value = true
  try {
    const resp: any = await http.get('/v9/places', { params: { keyword: kw, current: 1, size: 20 } })
    const list = resp?.records || resp || []
    placeSearchResults.value = list.map((p: any) => ({ id: p.id, placeName: p.placeName || p.place_name || '-' }))
  } finally { searchLoading3.value = false }
}

// ─── 组织树 (供 cascader) ───
const orgTreeRaw = ref<any[]>([])
const orgTreeOpts = computed(() => toCascaderNodes(orgTreeRaw.value))
const cascaderProps = { value: 'id', label: 'unitName', children: 'children', emitPath: false, checkStrictly: true }
function toCascaderNodes(list: any[]): any[] {
  return (list || []).map(n => ({
    id: n.id,
    unitName: n.unitName || n.name,
    children: n.children && n.children.length ? toCascaderNodes(n.children) : undefined
  }))
}
function findOrgName(id: any): string {
  function walk(list: any[]): string | null {
    for (const n of list) {
      if (String(n.id) === String(id)) return n.unitName || n.name
      if (n.children) { const r = walk(n.children); if (r) return r }
    }
    return null
  }
  return walk(orgTreeRaw.value) || String(id)
}

// ─── 数据表状态 ───
const loading = ref(false)
const rows = ref<any[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filter = ref({
  resourceType: (route.query.resourceType as string) || '',
  relation: (route.query.relation as string) || '',
  subjectType: (route.query.subjectType as string) || '',
  subjectId: route.query.subjectId ? Number(route.query.subjectId) : null,
  resourceId: route.query.resourceId ? Number(route.query.resourceId) : null
})
const relationOptions = ref<RelationTypeDef[]>([])

// 最近操作(场景视图)
const recentRows = ref<any[]>([])

// ─── 加载 ───
async function load() {
  loading.value = true
  try {
    const resp: any = await http.get('/access-relations', {
      params: {
        resourceType: filter.value.resourceType || undefined,
        subjectType: filter.value.subjectType || undefined,
        subjectId: filter.value.subjectId || undefined,
        resourceId: filter.value.resourceId || undefined,
        relation: filter.value.relation || undefined,
        page: page.value, size: pageSize.value
      }
    })
    rows.value = resp?.records || []
    total.value = Number(resp?.total) || rows.value.length
  } catch (e: any) { ElMessage.error('加载失败: ' + (e?.message || e)); rows.value = []; total.value = 0 }
  finally { loading.value = false }
}

async function loadRecent() {
  try {
    const resp: any = await http.get('/access-relations', { params: { page: 1, size: 8 } })
    recentRows.value = resp?.records || []
  } catch { recentRows.value = [] }
}

async function loadOrgTree() {
  try {
    const resp: any = await http.get('/org-units/tree')
    orgTreeRaw.value = Array.isArray(resp) ? resp : (resp?.data || [])
  } catch { orgTreeRaw.value = [] }
}

function resetFilter() {
  filter.value = { resourceType: '', relation: '', subjectType: '', subjectId: null, resourceId: null }
  page.value = 1
  load()
}

async function revoke(row: any) {
  await ElMessageBox.confirm(
    `撤销关系: ${displayName(row, 'subject')} —${relationLabel(row.relation)}→ ${displayName(row, 'resource')}`,
    '确认撤销', { type: 'warning' }
  )
  try {
    await accessRelationApi.delete(row.id)
    ElMessage.success('已撤销')
    await Promise.all([loadRecent(), viewMode.value === 'data' ? load() : Promise.resolve()])
  } catch (e: any) { ElMessage.error('撤销失败: ' + (e?.message || e)) }
}

// ─── 展示助手 ───
function typeLabel(t: string): string {
  return { user: '用户', org_unit: '组织', place: '场所' }[t as string] || t
}
function relationLabel(code: string): string {
  const def = relationOptions.value.find(r => r.relationCode === code)
  return def ? def.relationName : code
}
function relationTagType(relation: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const def = relationOptions.value.find(r => r.relationCode === relation)
  if (!def) return 'info'
  return ({
    OWNERSHIP: 'warning', MEMBERSHIP: 'primary', ASSOCIATION: 'success',
    DELEGATION: 'danger', SUBSCRIPTION: 'info'
  } as any)[def.category] || 'info'
}
function displayName(row: any, side: 'subject' | 'resource'): string {
  const meta = row.metadata || {}
  if (side === 'subject') return meta.subjectName || meta.userName || `#${row.subjectId}`
  if (row.resourceType === 'org_unit') return meta.orgUnitName || `#${row.resourceId}`
  if (row.resourceType === 'place') return meta.placeName || `#${row.resourceId}`
  if (row.resourceType === 'user') return meta.userName || meta.subjectName || `#${row.resourceId}`
  return `#${row.resourceId}`
}
function timeAgo(t?: string): string {
  if (!t) return '—'
  const d = new Date(t); const now = new Date(); const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return d.toISOString().substring(5, 10)
}
function viewBtnCls(active: boolean) {
  return `px-3 py-1.5 rounded border ${active ? 'bg-blue-50 border-blue-400 text-blue-600' : 'border-gray-200 text-gray-600'}`
}

onMounted(async () => {
  relationOptions.value = await relationTypeApi.list()
  await Promise.all([loadOrgTree(), loadRecent(), load()])
})
</script>

<style scoped>
.scene-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}
.scene-card:hover { border-color: #3b82f6; box-shadow: 0 2px 8px rgba(59,130,246,0.15); transform: translateY(-1px); }
.scene-icon { font-size: 28px; margin-bottom: 4px; }
.scene-title { font-size: 15px; font-weight: 600; color: #111827; }
.scene-desc { font-size: 12px; color: #6b7280; margin-top: 2px; }

.recent-list { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; }
.recent-item { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border-bottom: 1px solid #f3f4f6; font-size: 13px; }
.recent-item:last-child { border-bottom: none; }
.recent-when { width: 80px; color: #9ca3af; font-size: 12px; }
.recent-actor { font-weight: 500; color: #111827; }
.recent-arrow { color: #6b7280; font-size: 12px; margin: 0 4px; }
.recent-target { font-weight: 500; color: #111827; flex: 1; }
.recent-revoke { color: #ef4444; font-size: 12px; border: none; background: transparent; cursor: pointer; padding: 4px 8px; }
.recent-revoke:hover { background: #fef2f2; border-radius: 4px; }

.wizard-form { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 13px; color: #374151; font-weight: 500; }
.preview-panel { background: #f3f4f6; border-radius: 6px; padding: 12px; }
.preview-label { font-size: 12px; color: #6b7280; margin-bottom: 4px; }
.preview-line { font-size: 14px; color: #111827; }
.preview-line em { color: #2563eb; font-style: normal; font-weight: 500; margin: 0 2px; }
.preview-warning { margin-top: 8px; padding: 6px 10px; background: #fef3c7; color: #92400e; font-size: 12px; border-radius: 4px; }
</style>
