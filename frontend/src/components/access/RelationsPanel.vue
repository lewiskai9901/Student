<template>
  <div class="relations-panel">
    <div class="panel-header" v-if="showHeader">
      <div>
        <h3 class="panel-title">{{ title || '关系' }}</h3>
        <p class="panel-hint" v-if="hint">{{ hint }}</p>
      </div>
      <el-button v-if="!readonly" size="small" type="primary" plain @click="openAddDialog">
        <el-icon><Plus /></el-icon> 新建
      </el-button>
    </div>

    <!-- 关系指标条: 按关系聚合 + 容量/上限可视化 -->
    <div v-if="summaryPills.length" class="rel-summary">
      <button
        v-for="pill in summaryPills" :key="pill.key"
        class="rel-pill"
        :class="[pill.state, { 'is-active': filterRelation === pill.key }]"
        :title="pill.title"
        @click="togglePillFilter(pill.key)"
      >
        <span class="pill-label">{{ pill.label }}</span>
        <span class="pill-count">
          <b>{{ pill.current }}</b><span v-if="pill.max != null" class="pill-max">/{{ pill.max }}</span>
        </span>
        <span v-if="pill.max != null" class="pill-bar">
          <span class="pill-bar-fill" :style="{ width: pill.percent + '%' }"></span>
        </span>
      </button>
    </div>

    <!-- Toolbar: 搜索 + 类别筛选 + 新建 -->
    <div class="rel-toolbar">
      <input
        v-model="filterText"
        class="rel-search"
        type="text"
        placeholder="搜索对方名称"
      />
      <select v-model="filterCategory" class="rel-filter-select">
        <option value="">全部类别</option>
        <option value="OWNERSHIP">管理</option>
        <option value="MEMBERSHIP">成员/占用</option>
        <option value="ASSOCIATION">关联</option>
        <option value="DELEGATION">委托</option>
        <option value="SUBSCRIPTION">订阅</option>
      </select>
      <span v-if="filterRelation" class="rel-active-filter">
        筛选: {{ filterRelationLabel }}
        <button class="rel-clear-filter" @click="filterRelation = ''">×</button>
      </span>
      <span class="rel-count">{{ filteredRelations.length }} 条</span>
      <el-button v-if="!readonly" size="small" type="primary" plain @click="openAddDialog">
        <el-icon><Plus /></el-icon> 新建
      </el-button>
    </div>

    <!-- 表格视图 -->
    <div v-if="filteredRelations.length" class="rel-table-wrap">
      <table class="rel-table">
        <colgroup>
          <col style="width: 110px" />
          <col style="width: 28px" />
          <col />
          <col style="width: 58px" />
          <col style="width: 72px" />
          <col style="width: 150px" />
          <col style="width: 64px" />
        </colgroup>
        <thead>
          <tr>
            <th>关系</th>
            <th></th>
            <th>对方</th>
            <th>类型</th>
            <th>级别</th>
            <th>建立时间</th>
            <th v-if="!readonly"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in filteredRelations" :key="r.id" class="rel-row">
            <td>
              <el-tag size="small" :type="relationTagTypeOf(r)" effect="light">
                {{ relationLabelOfInstance(r) }}
              </el-tag>
            </td>
            <td class="rel-dir">
              <span :title="r._side === 'outward' ? '当前实体 > 对方' : '对方 > 当前实体'">
                {{ r._side === 'outward' ? '>' : '<' }}
              </span>
            </td>
            <td class="rel-other">{{ displayOther(r) }}</td>
            <td class="rel-othertype">{{ typeLabel(otherType(r)) }}</td>
            <td class="rel-level">{{ levelLabel(r.accessLevel) }}</td>
            <td class="rel-time">{{ formatTime(r.createdAt) }}</td>
            <td v-if="!readonly" class="text-right">
              <button class="rel-revoke" @click="revoke(r)">撤销</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else-if="!loading && !relations.length" class="empty-state">
      <div class="empty-icon"></div>
      <div class="empty-text">暂无关系</div>
      <el-button v-if="!readonly" size="small" type="primary" plain class="mt-2" @click="openAddDialog">
        添加第一条关系
      </el-button>
    </div>
    <div v-else-if="!loading" class="empty-state">
      <div class="empty-text" style="padding: 12px;">没有匹配的关系</div>
    </div>

    <!-- 添加 Dialog -->
    <el-dialog v-model="addVisible" :title="addDialogTitle" width="480px" append-to-body>
      <el-form label-width="100px" size="small">
        <el-form-item label="关系类型">
          <el-select v-model="form.relation" placeholder="选择关系" style="width:100%" @change="form.otherId = null">
            <el-option v-for="opt in availableRelations" :key="`${opt.relationCode}-${opt.direction}`"
              :label="`${opt.relationName} ${opt.direction === 'inward' ? '(作为资源)' : ''}`"
              :value="`${opt.relationCode}|${opt.direction}|${opt.otherType}`" />
          </el-select>
        </el-form-item>
        <el-form-item :label="form.direction === 'outward' ? '资源' : '主体'">
          <el-select v-if="currentOtherType === 'user'"
            v-model="form.otherId" filterable remote :remote-method="searchUsers"
            :loading="searchLoading" placeholder="搜索用户..." style="width:100%">
            <el-option v-for="u in userResults" :key="u.id"
              :label="`${u.realName} (${u.username})`" :value="u.id" />
          </el-select>
          <el-cascader v-else-if="currentOtherType === 'org_unit'"
            v-model="form.otherId" :options="orgTreeOpts" :props="cascaderProps"
            placeholder="选择组织" style="width:100%" />
          <el-select v-else-if="currentOtherType === 'place'"
            v-model="form.otherId" filterable remote :remote-method="searchPlaces"
            :loading="searchLoading2" placeholder="搜索场所..." style="width:100%">
            <el-option v-for="p in placeResults" :key="p.id" :label="p.placeName" :value="p.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!form.relation || !form.otherId" @click="submitAdd">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/utils/request'
import { accessRelationApi } from '@/api/accessRelation'
import { relationTypeApi, type RelationTypeDef } from '@/api/relationType'

const props = withDefaults(defineProps<{
  /** 当前主体/资源类型: user / org_unit / place */
  entityType: string
  /** 当前主体/资源 ID */
  entityId: number | string
  /** 只显示,不允许操作 */
  readonly?: boolean
  /** 标题 */
  title?: string
  /** 说明文字 */
  hint?: string
  /** 是否展示标题栏 */
  showHeader?: boolean
  /** 只看某些 relation (不传=全部) */
  relationFilter?: string[]
  /** 资源容量 (用于 capacityBound 关系的上限展示,如 place.capacity) */
  resourceCapacity?: number | null
  /** 资源子类型码 (用于 maxBySubtype 查找,如 org_unit.unitType / place.typeCode) */
  resourceSubtype?: string | null
}>(), {
  readonly: false,
  showHeader: true,
  relationFilter: () => []
})

const emit = defineEmits<{ 'change': [] }>()

const relations = ref<any[]>([])
const loading = ref(false)
const filterText = ref('')
const filterCategory = ref('')
const filterRelation = ref('')  // pill 激活筛选(格式同 grouped key)

function togglePillFilter(key: string) {
  filterRelation.value = filterRelation.value === key ? '' : key
}
const filterRelationLabel = computed(() => {
  if (!filterRelation.value) return ''
  return relationLabelByKey(filterRelation.value)
})

// 关系指标胶囊:按 (relationCode + subjectType + resourceType) 聚合,带容量/上限
const summaryPills = computed(() => {
  const map = new Map<string, { key: string; def: any; instances: any[]; sample: any }>()
  for (const r of relations.value) {
    const key = `${r.relation}|${r.subjectType}|${r.resourceType}`
    if (!map.has(key)) {
      map.set(key, { key, def: findRelationDef(r), instances: [], sample: r })
    }
    map.get(key)!.instances.push(r)
  }
  return Array.from(map.values()).map(g => {
    const def = g.def
    const current = g.instances.length
    let max: number | null = null
    let hint = ''
    if (def?.capacityBound && props.resourceCapacity != null) {
      max = Number(props.resourceCapacity)
      hint = '受资源容量限制'
    } else if (def?.maxBySubtype && props.resourceSubtype && def.maxBySubtype[props.resourceSubtype] != null) {
      max = Number(def.maxBySubtype[props.resourceSubtype])
      hint = `${props.resourceSubtype} 类型最多 ${max} 个`
    } else if (def?.maxPerResource != null) {
      max = Number(def.maxPerResource)
      hint = `每资源最多 ${max} 个`
    }
    let state = 'normal'
    let percent = 0
    if (max != null) {
      percent = Math.min(100, Math.round(current / max * 100))
      if (current >= max) state = 'full'
      else if (current >= max * 0.8) state = 'warn'
    } else {
      state = 'unbounded'
    }
    return {
      key: g.key,
      label: def?.relationName || g.sample.relation,
      current, max, percent, state,
      title: hint || '无上限限制'
    }
  })
})

// 过滤 + 按时间倒序排列
const filteredRelations = computed(() => {
  let list = [...relations.value].sort((a, b) => {
    const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0
    const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0
    return tb - ta
  })
  const kw = filterText.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(r =>
      (r.relation || '').toLowerCase().includes(kw) ||
      displayOther(r).toLowerCase().includes(kw)
    )
  }
  if (filterCategory.value) {
    list = list.filter(r => findRelationDef(r)?.category === filterCategory.value)
  }
  if (filterRelation.value) {
    list = list.filter(r =>
      `${r.relation}|${r.subjectType}|${r.resourceType}` === filterRelation.value
    )
  }
  return list
})

function relationTagTypeOf(r: any): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const def = findRelationDef(r)
  if (!def) return 'info'
  return ({
    OWNERSHIP: 'warning', MEMBERSHIP: 'primary', ASSOCIATION: 'success',
    DELEGATION: 'danger', SUBSCRIPTION: 'info'
  } as any)[def.category] || 'info'
}

function levelLabel(lvl: any): string {
  if (!lvl) return '-'
  const s = String(lvl).toUpperCase()
  return ({ FULL: '完全', VIEW: '只读', MANAGE: '管理', LIMITED: '受限' } as any)[s] || s
}

function formatTime(ts: any): string {
  if (!ts) return '-'
  const d = new Date(ts)
  if (isNaN(d.getTime())) return String(ts)
  const now = Date.now()
  const diffSec = Math.round((now - d.getTime()) / 1000)
  if (diffSec < 60) return '刚刚'
  if (diffSec < 3600) return `${Math.floor(diffSec / 60)} 分钟前`
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)} 小时前`
  if (diffSec < 86400 * 7) return `${Math.floor(diffSec / 86400)} 天前`
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
const allRelationTypes = ref<RelationTypeDef[]>([])

// ─── 加载当前实体的所有关系 (两个方向合并) ───
async function load() {
  loading.value = true
  try {
    const [asResource, asSubject] = await Promise.all([
      http.get('/access-relations', { params: { resourceType: props.entityType, resourceId: props.entityId } }) as any,
      http.get('/access-relations', { params: { subjectType: props.entityType, subjectId: props.entityId } }) as any
    ])
    const rs1 = (asResource?.records || []).map((r: any) => ({ ...r, _side: 'inward' }))
    const rs2 = (asSubject?.records || []).map((r: any) => ({ ...r, _side: 'outward' }))
    let merged = [...rs1, ...rs2]
    if (props.relationFilter.length) {
      merged = merged.filter(r => props.relationFilter.includes(r.relation))
    }
    relations.value = merged
  } catch (e: any) {
    ElMessage.error('加载关系失败: ' + (e?.message || e))
    relations.value = []
  } finally { loading.value = false }
}

// 监听 entityId 变化重新加载
watch(() => [props.entityType, props.entityId], load)

// ─── 分组 (按 relationCode + subjectType + resourceType,避免同 code 不同 type 合并) ───
const grouped = computed(() => {
  const m: Record<string, any[]> = {}
  for (const r of relations.value) {
    const key = `${r.relation}|${r.subjectType}|${r.resourceType}`
    if (!m[key]) m[key] = []
    m[key].push(r)
  }
  return m
})
// 从组 key 拿到第一个 relation 实例,用于 label / tag 查询
function groupSample(groupKey: string): any {
  return grouped.value[groupKey]?.[0]
}

// ─── 展示对方 ───
function otherType(r: any): string {
  return r._side === 'inward' ? r.subjectType : r.resourceType
}
function displayOther(r: any): string {
  const meta = r.metadata || {}
  if (r._side === 'inward') {
    // 对方是 subject
    if (r.subjectType === 'user') return meta.subjectName || meta.userName || `#${r.subjectId}`
    if (r.subjectType === 'org_unit') return meta.subjectName || meta.orgUnitName || `#${r.subjectId}`
    if (r.subjectType === 'place') return meta.subjectName || meta.placeName || `#${r.subjectId}`
  } else {
    // 对方是 resource
    if (r.resourceType === 'user') return meta.userName || meta.subjectName || `#${r.resourceId}`
    if (r.resourceType === 'org_unit') return meta.orgUnitName || `#${r.resourceId}`
    if (r.resourceType === 'place') return meta.placeName || `#${r.resourceId}`
  }
  return '未知'
}

function typeLabel(t: string): string {
  return { user: '用户', org_unit: '组织', place: '场所' }[t as string] || t
}
function findRelationDef(r: any) {
  if (!r) return undefined
  return allRelationTypes.value.find(
    d => d.relationCode === r.relation && d.fromType === r.subjectType && d.toType === r.resourceType
  ) || allRelationTypes.value.find(d => d.relationCode === r.relation)
}
function relationLabelByKey(groupKey: string): string {
  const def = findRelationDef(groupSample(groupKey))
  return def?.relationName || groupKey.split('|')[0]
}
function relationTagTypeByKey(groupKey: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const def = findRelationDef(groupSample(groupKey))
  if (!def) return 'info'
  return ({
    OWNERSHIP: 'warning', MEMBERSHIP: 'primary', ASSOCIATION: 'success',
    DELEGATION: 'danger', SUBSCRIPTION: 'info'
  } as any)[def.category] || 'info'
}
function relationLabelOfInstance(r: any): string {
  return findRelationDef(r)?.relationName || r.relation
}

async function revoke(r: any) {
  await ElMessageBox.confirm(
    `撤销该关系: ${relationLabelOfInstance(r)} > ${displayOther(r)}`,
    '确认撤销', { type: 'warning' }
  )
  try {
    await accessRelationApi.delete(r.id)
    ElMessage.success('已撤销')
    emit('change')
    await load()
  } catch (e: any) { ElMessage.error('撤销失败: ' + (e?.message || e)) }
}

// ─── 添加 Dialog ───
const addVisible = ref(false)
const addDialogTitle = computed(() => `为${typeLabel(props.entityType)}添加关系`)
const form = ref<{ relation: string; otherId: any; direction: string }>({
  relation: '', otherId: null, direction: 'outward'
})

// 可用关系: 根据当前 entityType 过滤所有可能的 relation_types
// 每条关系有两种可能方向 (当前实体作 subject 或 resource)
const availableRelations = computed(() => {
  const result: any[] = []
  for (const rt of allRelationTypes.value) {
    if (rt.fromType === props.entityType) {
      // 当前实体可以作为 subject > outward
      result.push({
        relationCode: rt.relationCode,
        relationName: rt.relationName,
        direction: 'outward',
        otherType: rt.toType
      })
    }
    if (rt.toType === props.entityType && rt.fromType !== props.entityType) {
      // 当前实体可以作为 resource > inward (排除自反的重复)
      result.push({
        relationCode: rt.relationCode,
        relationName: `${rt.relationName} (反向)`,
        direction: 'inward',
        otherType: rt.fromType
      })
    }
  }
  return result
})

const currentOtherType = computed(() => {
  if (!form.value.relation) return ''
  return form.value.relation.split('|')[2] || ''
})

watch(() => form.value.relation, (val) => {
  if (val) {
    const parts = val.split('|')
    form.value.direction = parts[1] || 'outward'
  }
})

function openAddDialog() {
  form.value = { relation: '', otherId: null, direction: 'outward' }
  userResults.value = []
  placeResults.value = []
  addVisible.value = true
}

async function submitAdd() {
  if (!form.value.relation || !form.value.otherId) { ElMessage.warning('请填写完整'); return }
  const [code, direction, otherType] = form.value.relation.split('|')
  try {
    const payload: any = {
      relation: code,
      accessLevel: 'FULL'
    }
    if (direction === 'outward') {
      // 当前实体作 subject, other 作 resource
      payload.subjectType = props.entityType
      payload.subjectId = props.entityId
      payload.resourceType = otherType
      payload.resourceId = form.value.otherId
    } else {
      payload.subjectType = otherType
      payload.subjectId = form.value.otherId
      payload.resourceType = props.entityType
      payload.resourceId = props.entityId
    }
    await accessRelationApi.create(payload as any)
    ElMessage.success('关系已添加')
    addVisible.value = false
    emit('change')
    await load()
  } catch (e: any) { ElMessage.error('添加失败: ' + (e?.message || e)) }
}

// ─── 搜索 ───
const searchLoading = ref(false)
const searchLoading2 = ref(false)
const userResults = ref<any[]>([])
const placeResults = ref<any[]>([])
async function searchUsers(kw: string) {
  if (!kw) { userResults.value = []; return }
  searchLoading.value = true
  try {
    const resp: any = await http.get('/users/simple', { params: { keyword: kw } })
    const list = Array.isArray(resp) ? resp : (resp?.records || resp?.list || [])
    userResults.value = list.map((u: any) => ({
      id: u.id, realName: u.realName || u.real_name || u.name || '-', username: u.username || ''
    }))
  } finally { searchLoading.value = false }
}
async function searchPlaces(kw: string) {
  if (!kw) { placeResults.value = []; return }
  searchLoading2.value = true
  try {
    const resp: any = await http.get('/v9/places', { params: { keyword: kw, current: 1, size: 20 } })
    placeResults.value = (resp?.records || []).map((p: any) => ({ id: p.id, placeName: p.placeName || p.place_name }))
  } finally { searchLoading2.value = false }
}

// ─── 组织树 ───
const orgTreeRaw = ref<any[]>([])
const orgTreeOpts = computed(() => toCascaderNodes(orgTreeRaw.value))
const cascaderProps = { value: 'id', label: 'unitName', children: 'children', emitPath: false, checkStrictly: true }
function toCascaderNodes(list: any[]): any[] {
  return (list || []).map(n => ({
    id: n.id, unitName: n.unitName || n.name,
    children: n.children?.length ? toCascaderNodes(n.children) : undefined
  }))
}
async function loadOrgTree() {
  try {
    const resp: any = await http.get('/org-units/tree')
    orgTreeRaw.value = Array.isArray(resp) ? resp : (resp?.data || [])
  } catch { orgTreeRaw.value = [] }
}

onMounted(async () => {
  allRelationTypes.value = await relationTypeApi.list()
  await Promise.all([loadOrgTree(), load()])
})
</script>

<style scoped>
.relations-panel { background: #fff; }
.panel-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.panel-title { font-size: 14px; font-weight: 600; color: #111827; }
.panel-hint { font-size: 12px; color: #9ca3af; margin-top: 2px; }

/* 指标胶囊 */
.rel-summary {
  display: flex; flex-wrap: wrap; gap: 8px;
  margin-bottom: 10px;
}
.rel-pill {
  display: inline-flex; flex-direction: column; gap: 3px;
  min-width: 100px; padding: 8px 12px;
  background: #fff; border: 1px solid #e5e7eb; border-radius: 8px;
  cursor: pointer; transition: all 0.15s;
  font-family: inherit;
}
.rel-pill:hover { border-color: #93c5fd; box-shadow: 0 2px 6px rgba(59,130,246,0.08); }
.rel-pill.is-active { border-color: #3b82f6; background: #eff6ff; box-shadow: 0 0 0 2px rgba(59,130,246,0.15); }
.rel-pill .pill-label {
  font-size: 11px; color: #6b7280; font-weight: 500;
  line-height: 1;
}
.rel-pill .pill-count {
  font-size: 18px; font-weight: 700; color: #111827; line-height: 1.1;
}
.rel-pill .pill-max { font-size: 12px; color: #9ca3af; font-weight: 500; margin-left: 2px; }
.rel-pill .pill-bar {
  display: block; height: 3px; background: #f3f4f6; border-radius: 2px;
  overflow: hidden; margin-top: 2px;
}
.rel-pill .pill-bar-fill {
  display: block; height: 100%; background: #3b82f6; transition: width 0.2s;
}
.rel-pill.full { border-color: #fecaca; background: #fef2f2; }
.rel-pill.full .pill-bar-fill { background: #ef4444; }
.rel-pill.full .pill-count { color: #dc2626; }
.rel-pill.warn { border-color: #fde68a; background: #fffbeb; }
.rel-pill.warn .pill-bar-fill { background: #f59e0b; }
.rel-pill.unbounded .pill-count::after {
  content: '无上限'; font-size: 10px; font-weight: 500; color: #9ca3af;
  margin-left: 4px;
}

/* 筛选标签 */
.rel-active-filter {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 2px 8px 2px 10px; background: #eff6ff; color: #2563eb;
  border-radius: 10px; font-size: 11px;
}
.rel-clear-filter {
  border: none; background: transparent; color: #2563eb; cursor: pointer;
  font-size: 14px; padding: 0 4px; line-height: 1;
}

/* Toolbar */
.rel-toolbar {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 4px; margin-bottom: 6px;
  border-bottom: 1px solid #f3f4f6;
}
.rel-search {
  flex: 1; height: 28px; padding: 0 10px;
  border: 1px solid #e5e7eb; border-radius: 5px;
  font-size: 12px; outline: none; background: #fff;
  transition: border-color 0.15s;
}
.rel-search:focus { border-color: #3b82f6; }
.rel-filter-select {
  height: 28px; padding: 0 24px 0 10px;
  border: 1px solid #e5e7eb; border-radius: 5px;
  font-size: 12px; background: #fff; outline: none;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg width='10' height='6' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%239ca3af' fill='none' stroke-width='1.5' stroke-linecap='round'/%3E%3C/svg%3E");
  background-position: right 8px center; background-repeat: no-repeat;
}
.rel-count { font-size: 11px; color: #9ca3af; }

/* Table */
.rel-table-wrap { overflow-x: auto; }
.rel-table {
  width: 100%; border-collapse: collapse;
  font-size: 12px; color: #111827;
}
.rel-table thead th {
  font-weight: 500; text-align: left;
  padding: 8px 10px; border-bottom: 1px solid #e5e7eb;
  color: #6b7280; font-size: 11px;
  text-transform: uppercase; letter-spacing: 0.3px;
}
.rel-table tbody td {
  padding: 8px 10px; border-bottom: 1px solid #f3f4f6;
  vertical-align: middle;
}
.rel-row { transition: background 0.1s; }
.rel-row:hover { background: #f9fafb; }
.rel-dir { color: #9ca3af; font-weight: 600; font-size: 14px; text-align: center; }
.rel-other { font-weight: 500; color: #111827; }
.rel-othertype { color: #6b7280; font-size: 11px; }
.rel-level { color: #374151; font-size: 11px; }
.rel-time { color: #9ca3af; font-size: 11px; white-space: nowrap; }
.rel-revoke {
  background: transparent; border: none; color: #ef4444; cursor: pointer;
  font-size: 11px; padding: 4px 8px; border-radius: 4px;
}
.rel-revoke:hover { background: #fef2f2; }
.text-right { text-align: right; }

.empty-state { text-align: center; padding: 24px 12px; color: #9ca3af; }
.empty-icon { font-size: 32px; margin-bottom: 6px; }
.empty-text { font-size: 13px; }
</style>
