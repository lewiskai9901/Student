<template>
  <div class="dp-root">
    <!-- Header Card -->
    <div class="dp-card">
      <div class="dp-card-header">
        <div class="dp-name-row">
          <h2 class="dp-name">{{ node.unitName }}</h2>
          <code class="tm-code">{{ node.unitCode }}</code>
          <span class="tm-chip" :style="typeBadgeStyle">
            {{ node.typeName || node.unitType }}
          </span>
          <span
            class="tm-chip"
            :class="node.status === 'ACTIVE' ? 'tm-chip-green' : node.status === 'FROZEN' ? 'tm-chip-amber' : 'tm-chip-red'"
          >
            {{ node.statusLabel || node.status }}
          </span>
        </div>
        <div class="dp-actions">
          <button class="tm-btn tm-btn-secondary" style="padding: 4px 10px; font-size: 12px;" @click="emit('addChild', node)">
            <Plus style="width: 12px; height: 12px;" />
            子组织
          </button>
          <button class="tm-btn tm-btn-secondary" style="padding: 4px 10px; font-size: 12px;" @click="emit('edit', node)">
            <Pencil style="width: 12px; height: 12px;" />
            编辑
          </button>
          <!-- More menu -->
          <div v-if="node.status === 'ACTIVE' || node.status === 'FROZEN'" class="dp-dropdown-wrap">
            <button class="dp-more-btn" @click="moreMenuOpen = !moreMenuOpen">
              <MoreHorizontal style="width: 14px; height: 14px;" />
            </button>
            <Teleport to="body">
              <div v-if="moreMenuOpen" class="dp-dropdown-overlay" @click="moreMenuOpen = false"></div>
            </Teleport>
            <div v-if="moreMenuOpen" class="dp-dropdown-menu">
              <button v-if="node.status === 'ACTIVE'" class="dp-menu-item" @click="moreMenuOpen = false; emit('toggleStatus', node)">
                <Ban style="width: 14px; height: 14px; color: #f59e0b;" /> 冻结
              </button>
              <button v-if="node.status === 'FROZEN'" class="dp-menu-item" @click="moreMenuOpen = false; emit('toggleStatus', node)">
                <Check style="width: 14px; height: 14px; color: #10b981;" /> 解冻
              </button>
              <div v-if="node.status === 'ACTIVE'" class="dp-menu-divider"></div>
              <button v-if="node.status === 'ACTIVE'" class="dp-menu-item" @click="moreMenuOpen = false; emit('merge', node)">
                <Merge style="width: 14px; height: 14px; color: #3b82f6;" /> 合并到...
              </button>
              <button v-if="node.status === 'ACTIVE'" class="dp-menu-item" @click="moreMenuOpen = false; emit('split', node)">
                <Split style="width: 14px; height: 14px; color: #6366f1;" /> 拆分
              </button>
              <div v-if="node.status === 'ACTIVE'" class="dp-menu-divider"></div>
              <button v-if="node.status === 'ACTIVE'" class="dp-menu-item dp-menu-danger" @click="moreMenuOpen = false; emit('dissolve', node)">
                <XCircle style="width: 14px; height: 14px;" /> 解散
              </button>
              <div class="dp-menu-divider"></div>
              <button class="dp-menu-item" @click="moreMenuOpen = false; showExportDialog = true">
                <Download style="width: 14px; height: 14px; color: #6b7280;" /> 导出数据
              </button>
              <button class="dp-menu-item" @click="moreMenuOpen = false; showImportDialog = true">
                <Upload style="width: 14px; height: 14px; color: #6b7280;" /> 导入数据
              </button>
              <div class="dp-menu-divider"></div>
              <button class="dp-menu-item dp-menu-danger" @click="moreMenuOpen = false; emit('delete', node)">
                <Trash2 style="width: 14px; height: 14px;" /> 删除
              </button>
            </div>
          </div>
          <button
            v-else
            class="tm-btn" style="padding: 4px 10px; font-size: 12px; background: #fef2f2; color: #dc2626; border: 1px solid #fecaca;"
            @click="emit('delete', node)"
          >
            <Trash2 style="width: 12px; height: 12px;" />
            删除
          </button>
        </div>
      </div>

      <!-- Statistics Bar -->
      <div v-if="stats" class="dp-stat-bar">
        <span>成员 <b>{{ stats.belongingCount }}</b></span>
        <template v-if="stats.countByUserType && Object.keys(stats.countByUserType).length > 0">
          <span style="color: #d1d5db;">—</span>
          <template v-for="(cnt, typeCode, idx) in (stats.countByUserType || {})" :key="typeCode">
            <span v-if="idx > 0" style="color: #e5e7eb;">|</span>
            <span>{{ userTypeNameMap[typeCode as string] || typeCode }} <b>{{ cnt }}</b></span>
          </template>
        </template>
      </div>
    </div>

    <!-- Basic Info (promoted from tab) -->
    <div class="dp-card dp-basic-info">
      <div class="dp-info-inline">
        <span class="dp-info-kv"><em>编码</em> <code class="tm-code">{{ node.unitCode }}</code></span>
        <span class="dp-info-kv"><em>类型</em> <span class="tm-chip" :style="typeBadgeStyle">{{ node.typeName || node.unitType }}</span></span>
        <span class="dp-info-kv"><em>上级</em> {{ parentName || '顶级组织' }}</span>
        <span class="dp-info-kv"><em>编制</em> {{ node.headcount ?? '-' }}</span>
        <span class="dp-info-kv"><em>排序</em> {{ node.sortOrder }}</span>
      </div>
    </div>

    <!-- Tabs -->
    <div class="dp-card">
      <div class="tm-tabs">
        <button
          v-for="tab in allTabs"
          :key="tab.key"
          class="tm-tab"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <span v-if="tab.count !== undefined" class="dp-tab-count">{{ tab.count }}</span>
        </button>
      </div>

      <!-- Tab: 下级组织 -->
      <div v-if="activeTab === 'children'">
        <div class="dp-tab-toolbar">
          <span class="dp-tab-info">共 {{ children.length }} 个下级组织</span>
          <button class="tm-btn tm-btn-primary" style="padding: 4px 10px; font-size: 11px;" @click="emit('addChild', node)">
            <Plus style="width: 12px; height: 12px;" />
            添加
          </button>
        </div>
        <div v-if="children.length > 0" style="overflow-x: auto;">
          <table class="tm-table">
            <colgroup>
              <col />
              <col style="width: 120px;" />
              <col style="width: 100px;" />
              <col style="width: 80px;" />
              <col style="width: 100px;" />
            </colgroup>
            <thead>
              <tr>
                <th class="text-left">名称</th>
                <th class="text-left">编码</th>
                <th class="text-left">类型</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="child in children" :key="child.id">
                <td class="text-left">
                  <span style="font-weight: 500;">{{ child.unitName }}</span>
                  <span v-if="child.children?.length" class="tm-chip tm-chip-gray" style="margin-left: 6px;">
                    {{ child.children.length }} 子级
                  </span>
                </td>
                <td class="text-left">
                  <code class="tm-code">{{ child.unitCode }}</code>
                </td>
                <td class="text-left">{{ child.typeName || child.unitType }}</td>
                <td>
                  <span
                    class="tm-chip"
                    :class="child.status === 'ACTIVE' ? 'tm-chip-green' : child.status === 'FROZEN' ? 'tm-chip-amber' : 'tm-chip-red'"
                  >
                    {{ child.statusLabel || child.status }}
                  </span>
                </td>
                <td class="text-right">
                  <button class="tm-action" title="编辑" @click="emit('edit', child)">
                    <Pencil style="width: 13px; height: 13px;" />
                  </button>
                  <button
                    v-if="child.status === 'ACTIVE' || child.status === 'FROZEN'"
                    class="tm-action"
                    :title="child.status === 'ACTIVE' ? '冻结' : '解冻'"
                    @click="emit('toggleStatus', child)"
                  >
                    <Ban v-if="child.status === 'ACTIVE'" style="width: 13px; height: 13px;" />
                    <Check v-else style="width: 13px; height: 13px;" />
                  </button>
                  <button class="tm-action tm-action-danger" title="删除" @click="emit('delete', child)">
                    <Trash2 style="width: 13px; height: 13px;" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="dp-empty">
          <p>暂无下级组织</p>
          <button class="tm-btn tm-btn-secondary" style="margin-top: 10px; font-size: 12px;" @click="emit('addChild', node)">
            <Plus style="width: 14px; height: 14px;" />
            添加子组织
          </button>
        </div>
      </div>

      <!-- Tab: 成员 (仅本组织归属成员) -->
      <div v-if="activeTab === 'members'">
        <div class="dp-tab-toolbar">
          <span class="dp-tab-info">共 {{ belongingMembers.length }} 人</span>
          <button class="tm-btn tm-btn-secondary" style="padding: 4px 10px; font-size: 12px;" @click="showAddMember = true">
            <Plus style="width: 12px; height: 12px;" />
            添加成员
          </button>
        </div>
        <div v-if="membersLoading" class="dp-loading">
          <Loader2 style="width: 18px; height: 18px; color: #9ca3af;" class="tm-spin" />
          <span>加载中...</span>
        </div>
        <div v-else-if="mergedMembers.length > 0" style="overflow-x: auto;">
          <table class="tm-table">
            <colgroup>
              <col />
              <col style="width: 100px;" />
              <col style="width: 80px;" />
            </colgroup>
            <thead>
              <tr>
                <th class="text-left">姓名</th>
                <th class="text-left">身份</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="m in mergedMembers"
                :key="m.userId"
              >
                <td class="text-left">
                  <button class="dp-link" @click="openUserRelation(m)">{{ m.userName }}</button>
                </td>
                <td class="text-left">{{ userTypeNameMap[m.userTypeCode || ''] || m.userTypeCode || '-' }}</td>
                <td class="text-right">
                  <button class="tm-action tm-action-danger" title="移除成员" @click="handleRemoveMember(m)">
                    <Trash2 style="width: 13px; height: 13px;" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="dp-empty">
          <p>暂无成员</p>
        </div>
      </div>

      <!-- Tab: 关联场所 -->
      <div v-if="activeTab === 'places'">
        <div class="dp-tab-toolbar">
          <span class="dp-tab-info">共 {{ places.length }} 个关联场所</span>
          <button class="tm-btn tm-btn-secondary" style="padding: 4px 10px; font-size: 12px;" @click="showAddPlace = true">
            <Plus style="width: 12px; height: 12px;" />
            关联场所
          </button>
        </div>
        <div v-if="placesLoading" class="dp-loading">
          <Loader2 style="width: 18px; height: 18px; color: #9ca3af;" class="tm-spin" />
          <span>加载中...</span>
        </div>
        <div v-else-if="places.length > 0" style="overflow-x: auto;">
          <table class="tm-table">
            <colgroup>
              <col />
              <col style="width: 120px;" />
              <col style="width: 100px;" />
              <col style="width: 60px;" />
            </colgroup>
            <thead>
              <tr>
                <th class="text-left">场所名称</th>
                <th class="text-left">编码</th>
                <th class="text-left">关系类型</th>
                <th class="text-right"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in places" :key="s.id">
                <td class="text-left" style="font-weight: 500;">{{ s.metadata?.placeName || `场所#${s.resourceId}` }}</td>
                <td class="text-left">
                  <code class="tm-code">{{ s.metadata?.placeCode || '-' }}</code>
                </td>
                <td class="text-left">
                  <span class="tm-chip tm-chip-purple">{{ RelationLabels[s.relation] || s.relation }}</span>
                </td>
                <td class="text-right">
                  <button class="tm-action tm-action-danger" @click="handleRemovePlace(s)">移除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="dp-empty" style="padding: 16px;">
          暂无关联场所
        </div>
      </div>


      <!-- Tab: 变更日志 -->
      <div v-if="activeTab === 'changelog'">
        <ActivityTimeline
          resourceType="ORG_UNIT"
          :resourceId="node.id"
          :limit="50"
          title="变更日志"
        />
      </div>

      <!-- Tab: Extension attribute groups (each group = 1 tab) -->
      <div v-for="group in extensionGroups" :key="group.key" v-show="activeTab === group.key" class="dp-ext-tab-body">
        <DynamicForm
          :schema="{ fields: group.fields }"
          v-model="extensionAttrs"
          :disabled="true"
        />
      </div>
    </div>

    <!-- 添加成员 — 用户选择器弹窗 (Step 4) -->
    <UserSelectorDialog
      v-model:visible="showAddMember"
      title="添加成员到组织"
      :exclude-user-ids="belongingMembers.map(m => m.userId)"
      @confirm="handleAddMemberFromSelector"
    />


    <!-- 导出/导入弹窗 (Step 5) -->
    <OrgExportDialog
      v-model:visible="showExportDialog"
      :org-unit="node"
    />
    <OrgImportDialog
      v-model:visible="showImportDialog"
      :org-unit-id="node.id"
      @imported="Promise.all([loadMembers(), loadStats()])"
    />

    <!-- 用户关系抽屉 (Step 3) -->
    <UserRelationDrawer
      v-model:visible="showUserRelation"
      :user-id="userRelationUserId"
      :user-name="userRelationUserName"
      :tree-data="treeData"
      @changed="onUserRelationChanged"
    />

    <!-- 关联场所弹窗 -->
    <Teleport to="body">
      <div v-if="showAddPlace" class="dp-modal-overlay" @click.self="showAddPlace = false">
        <div class="dp-modal">
          <h3 class="dp-modal-title">关联场所到「{{ node.unitName }}」</h3>
          <div class="dp-modal-body">
            <div class="tm-field">
              <label class="tm-label">选择场所</label>
              <select v-model="addPlaceForm.placeId" class="tm-field-select">
                <option :value="0" disabled>请选择场所</option>
                <option v-for="s in placeOptions" :key="s.id" :value="s.id">
                  {{ s.placeName }} ({{ s.placeCode }})
                </option>
              </select>
            </div>
            <div class="tm-field">
              <label class="tm-label">关系类型</label>
              <select v-model="addPlaceForm.relationType" class="tm-field-select">
                <option value="PRIMARY">主管</option>
                <option value="SHARED">共享</option>
                <option value="MANAGED">托管</option>
              </select>
            </div>
            <div class="tm-field" style="display: flex; gap: 16px;">
              <label class="dp-checkbox-label">
                <input v-model="addPlaceForm.isPrimary" type="checkbox" />
                主归属
              </label>
              <label class="dp-checkbox-label">
                <input v-model="addPlaceForm.canInspect" type="checkbox" />
                可检查
              </label>
            </div>
          </div>
          <div class="dp-modal-footer">
            <button class="tm-btn tm-btn-secondary" @click="showAddPlace = false">取消</button>
            <button
              class="tm-btn tm-btn-primary"
              :disabled="!addPlaceForm.placeId || addPlaceSubmitting"
              @click="handleAddPlace"
            >
              {{ addPlaceSubmitting ? '关联中...' : '确定' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, reactive } from 'vue'
import {
  Plus,
  Pencil,
  Trash2,
  Ban,
  Check,
  XCircle,
  Loader2,
  MoreHorizontal,
  Merge,
  Split,
  Download,
  Upload,
} from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { DepartmentResponse } from '@/api/organization'
import DynamicForm from '@/components/extension/DynamicForm.vue'
import { entityTypeApi } from '@/api/entityType'
import UserRelationDrawer from './UserRelationDrawer.vue'
import UserSelectorDialog from '@/components/common/UserSelectorDialog.vue'
import OrgExportDialog from './OrgExportDialog.vue'
import OrgImportDialog from './OrgImportDialog.vue'
import type { AccessRelation } from '@/types/accessRelation'
import { RelationLabels } from '@/types/accessRelation'
import { accessRelationApi } from '@/api/accessRelation'
import type { SimpleUser } from '@/types/user'
import type { UniversalPlace, PlaceTreeNode } from '@/types/universalPlace'
import { universalPlaceApi } from '@/api/universalPlace'
import { orgMemberApi } from '@/api/orgMember'
import type { OrgStatistics } from '@/types/position'
import ActivityTimeline from '@/components/activity/ActivityTimeline.vue'

interface Props {
  node: DepartmentResponse
  treeData: DepartmentResponse[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  refresh: []
  addChild: [node: DepartmentResponse]
  edit: [node: DepartmentResponse]
  delete: [node: DepartmentResponse]
  toggleStatus: [node: DepartmentResponse]
  merge: [node: DepartmentResponse]
  split: [node: DepartmentResponse]
  dissolve: [node: DepartmentResponse]
}>()

// ==================== Tab state ====================
const activeTab = ref<string>('children')
const moreMenuOpen = ref(false)

// ==================== Color mapping by category (OrgCategory enum) ====================
const OrgCategoryColorMap: Record<string, string> = {
  ROOT: '#3b82f6',         // 蓝 - 根组织
  BRANCH: '#8b5cf6',       // 紫 - 分支机构
  FUNCTIONAL: '#10b981',   // 绿 - 职能部门
  GROUP: '#ef4444',        // 红 - 成员组（班级/小组）
  CONTAINER: '#f59e0b',    // 橙 - 容器（年级/学部）
}

const typeColor = computed(() => OrgCategoryColorMap[props.node.category || ''] || '#6b7280')

const typeIconStyle = computed(() => ({
  backgroundColor: `${typeColor.value}18`,
  color: typeColor.value
}))

const typeBadgeStyle = computed(() => ({
  backgroundColor: `${typeColor.value}12`,
  color: typeColor.value
}))

// Find parent name
const findParentName = (nodes: DepartmentResponse[], parentId: number | null): string => {
  if (!parentId) return ''
  for (const n of nodes) {
    if (n.id === parentId) return n.unitName
    if (n.children) {
      const found = findParentName(n.children, parentId)
      if (found) return found
    }
  }
  return ''
}

const parentName = computed(() => findParentName(props.treeData, props.node.parentId))

// Children
const children = computed(() => props.node.children || [])

const getChildIconStyle = (child: DepartmentResponse) => {
  const color = OrgCategoryColorMap[child.category || ''] || '#6b7280'
  return {
    backgroundColor: `${color}18`,
    color: color
  }
}

// ==================== Statistics ====================
const stats = ref<OrgStatistics | null>(null)

const loadStats = async () => {
  try {
    stats.value = await orgMemberApi.getOrgStatistics(props.node.id)
  } catch (e: any) {
    console.error('Failed to load stats', e)
  }
}

// ==================== Members data ====================
interface OrgMemberItem {
  userId: string | number
  userName: string
  userTypeCode?: string
  userTypeName?: string
  membershipType?: string
  primaryOrgUnitId?: string | number
  primaryOrgUnitName?: string
}

const belongingMembers = ref<OrgMemberItem[]>([])
const membersLoading = ref(false)

const loadMembers = async () => {
  membersLoading.value = true
  try {
    belongingMembers.value = await orgMemberApi.getBelongingMembers(props.node.id)
  } catch (e: any) {
    console.error('Failed to load members', e)
  } finally {
    membersLoading.value = false
  }
}

// ==================== User type name map ====================
const userTypeNameMap = ref<Record<string, string>>({})

const loadUserTypes = async () => {
  try {
    const types = await entityTypeApi.list('USER').then(r => ((r as any).data || r || []).map((t: any) => ({ typeCode: t.typeCode, typeName: t.typeName })))
    const map: Record<string, string> = {}
    for (const t of types) {
      map[t.typeCode] = t.typeName
    }
    userTypeNameMap.value = map
  } catch (e) {
    console.error('Failed to load user types', e)
  }
}
loadUserTypes()

// ==================== Merged members (unified list) ====================
interface MergedMember {
  userId: string | number
  userName: string
  userTypeCode?: string
}

const mergedMembers = computed<MergedMember[]>(() => {
  return belongingMembers.value.map(m => ({
    userId: m.userId,
    userName: m.userName,
    userTypeCode: m.userTypeCode,
  }))
})


// ==================== Places data ====================
const places = ref<AccessRelation[]>([])
const placesLoading = ref(false)

const loadPlaces = async () => {
  placesLoading.value = true
  try {
    places.value = await accessRelationApi.getBySubject('org_unit', props.node.id, 'place')
  } catch (e: any) {
    console.error('Failed to load places', e)
  } finally {
    placesLoading.value = false
  }
}

// ==================== Extension groups as tabs ====================
const extensionGroups = computed(() => {
  const fields = extensionSchema.value?.fields || []
  const groups = new Map<string, any[]>()
  for (const f of fields) {
    const g = f.group || '扩展属性'
    if (!groups.has(g)) groups.set(g, [])
    groups.get(g)!.push(f)
  }
  return Array.from(groups.entries()).map(([name, fields], i) => ({
    key: `ext_${i}`,
    label: name,
    fields,
  }))
})


// ==================== Tab counts ====================
const baseTabs = computed(() => [
  { key: 'children', label: '下级组织', count: children.value.length },
  { key: 'members', label: '成员', count: belongingMembers.value.length || undefined },
  { key: 'places', label: '关联场所', count: places.value.length },
  { key: 'changelog', label: '变更日志', count: undefined },
])

const allTabs = computed(() => [
  ...baseTabs.value,
  ...extensionGroups.value.map(g => ({ key: g.key, label: g.label, count: undefined })),
])

// ==================== Load data on node change ====================
// Extension schema from entity_type_configs (SPI plugin)
const extensionSchema = ref<{ fields: any[] } | null>(null)
const extensionAttrs = ref<Record<string, any>>({})

async function loadExtensionSchema() {
  extensionSchema.value = null
  extensionAttrs.value = {}
  const typeCode = props.node.unitType
  if (!typeCode) return
  try {
    const res = await entityTypeApi.get('ORG_UNIT', typeCode)
    const data = (res as any).data || res
    if (data?.metadataSchema) {
      const schema = typeof data.metadataSchema === 'string' ? JSON.parse(data.metadataSchema) : data.metadataSchema
      if (schema?.fields?.length > 0) {
        extensionSchema.value = schema
        extensionAttrs.value = props.node.attributes || {}
      }
    }
  } catch { /* no plugin */ }
}

watch(
  () => props.node.id,
  () => {
    belongingMembers.value = []
    places.value = []
    stats.value = null
    loadMembers()
    loadPlaces()
    loadStats()
    loadExtensionSchema()
  },
  { immediate: true }
)

// ==================== Add Member (直接添加到组织, using UserSelectorDialog) ====================
const showAddMember = ref(false)

const handleAddMemberFromSelector = async (users: SimpleUser[]) => {
  if (users.length === 0) return
  const user = users[0]
  // Step 2: Check if user already belongs to another org
  if (user.primaryOrgUnitId && String(user.primaryOrgUnitId) !== String(props.node.id)) {
    try {
      await ElMessageBox.confirm(
        `该用户当前归属于「${user.primaryOrgUnitName || '其他组织'}」，确定要变更到本组织吗？`,
        '变更组织提示',
        { type: 'warning' }
      )
    } catch {
      return // user cancelled
    }
  }
  try {
    await orgMemberApi.addMember(props.node.id, user.id)
    ElMessage.success('成员添加成功')
    await Promise.all([loadMembers(), loadStats()])
  } catch (e: any) {
    ElMessage.error(e.message || '添加失败')
  }
}

const handleRemoveMember = async (m: MergedMember) => {
  try {
    await ElMessageBox.confirm(
      `确定要将「${m.userName}」从本组织移除吗？`,
      '确认移除成员',
      { type: 'warning' }
    )
    await orgMemberApi.removeMember(props.node.id, m.userId)
    ElMessage.success('已移除成员')
    await Promise.all([loadMembers(), loadStats()])
  } catch {
    // cancelled
  }
}


// ==================== Add Place ====================
const showAddPlace = ref(false)
const addPlaceSubmitting = ref(false)
const placeOptions = ref<UniversalPlace[]>([])
const addPlaceForm = reactive({
  placeId: 0,
  relationType: 'PRIMARY',
  isPrimary: false,
  canInspect: false
})

const flattenTree = (nodes: PlaceTreeNode[]): UniversalPlace[] => {
  const result: UniversalPlace[] = []
  const walk = (list: PlaceTreeNode[]) => {
    for (const n of list) {
      result.push(n)
      if (n.children?.length) walk(n.children)
    }
  }
  walk(nodes)
  return result
}

watch(showAddPlace, async (show) => {
  if (show && placeOptions.value.length === 0) {
    try {
      const tree = await universalPlaceApi.getTree()
      placeOptions.value = flattenTree(tree)
    } catch (e) {
      console.error('Failed to load places', e)
    }
  }
  if (show) {
    addPlaceForm.placeId = 0
    addPlaceForm.relationType = 'PRIMARY'
    addPlaceForm.isPrimary = false
    addPlaceForm.canInspect = false
  }
})

const handleAddPlace = async () => {
  if (!addPlaceForm.placeId) return
  addPlaceSubmitting.value = true
  try {
    await accessRelationApi.create({
      resourceType: 'place',
      resourceId: addPlaceForm.placeId,
      subjectType: 'org_unit',
      subjectId: props.node.id,
      relation: addPlaceForm.relationType,
      metadata: {
        isPrimary: addPlaceForm.isPrimary,
        canInspect: addPlaceForm.canInspect
      }
    })
    ElMessage.success('场所关联成功')
    showAddPlace.value = false
    await loadPlaces()
  } catch (e: any) {
    ElMessage.error(e.message || '关联失败')
  } finally {
    addPlaceSubmitting.value = false
  }
}

const handleRemovePlace = async (s: AccessRelation) => {
  try {
    await ElMessageBox.confirm(
      `确定要移除场所「${s.metadata?.placeName || '场所#' + s.resourceId}」的关联吗？`,
      '确认移除',
      { type: 'warning' }
    )
    await accessRelationApi.delete(s.id)
    ElMessage.success('已移除关联')
    await loadPlaces()
  } catch {
    // cancelled
  }
}

// ==================== Export / Import Dialogs (Step 5) ====================
const showExportDialog = ref(false)
const showImportDialog = ref(false)

// ==================== User Relation Drawer (Step 3) ====================
const showUserRelation = ref(false)
const userRelationUserId = ref<number | string | null>(null)
const userRelationUserName = ref('')

const openUserRelation = (m: MergedMember) => {
  userRelationUserId.value = m.userId
  userRelationUserName.value = m.userName
  showUserRelation.value = true
}

const onUserRelationChanged = () => {
  loadMembers()
  loadStats()
}
</script>

<style scoped>
@import '@/styles/teaching-ui.css';

.dp-root { display: flex; flex-direction: column; gap: 8px; }

/* Header card */
.dp-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
}
.dp-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
}
.dp-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.dp-name {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dp-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.dp-stat-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  border-top: 1px solid #f3f4f6;
  padding: 4px 12px;
  font-size: 12px;
  color: #6b7280;
}
.dp-stat-bar b { font-weight: 600; color: #111827; }

/* Section label */
.dp-section-label {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #9ca3af;
  margin: 0 0 12px;
}

/* Tab extras */
.dp-tab-count {
  display: inline-block;
  margin-left: 4px;
  padding: 1px 6px;
  border-radius: 99px;
  background: #f3f4f6;
  font-size: 10px;
  font-weight: 500;
  color: #6b7280;
}
.dp-tab-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 12px;
  border-bottom: 1px solid #f3f4f6;
}
.dp-tab-info { font-size: 12px; color: #6b7280; }

/* Empty / loading */
.dp-empty {
  text-align: center;
  padding: 40px 0;
  color: #9ca3af;
  font-size: 13px;
}
.dp-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 0;
  font-size: 13px;
  color: #9ca3af;
}

/* Link button */
.dp-link {
  background: none;
  border: none;
  padding: 0;
  font-size: 13px;
  font-weight: 500;
  color: #2563eb;
  cursor: pointer;
  font-family: inherit;
}
.dp-link:hover { color: #1d4ed8; text-decoration: underline; }

/* Info grid (basic info tab) */
.dp-info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px 24px;
  padding: 16px 20px;
}
.dp-info-item dt {
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 4px;
}
.dp-info-item dd {
  font-size: 13px;
  color: #111827;
  margin: 0;
}

/* More menu (native dropdown replacing el-dropdown) */
.dp-dropdown-wrap { position: relative; }
.dp-more-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #9ca3af;
  cursor: pointer;
  transition: background 0.1s;
}
.dp-more-btn:hover { background: #f9fafb; color: #6b7280; }
.dp-dropdown-overlay {
  position: fixed;
  inset: 0;
  z-index: 1999;
}
.dp-dropdown-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 4px);
  z-index: 2000;
  min-width: 160px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  padding: 4px 0;
}
.dp-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 7px 14px;
  font-size: 13px;
  font-family: inherit;
  color: #374151;
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  transition: background 0.1s;
}
.dp-menu-item:hover { background: #f3f4f6; }
.dp-menu-danger { color: #dc2626; }
.dp-menu-danger:hover { background: #fef2f2; }
.dp-menu-divider { height: 1px; background: #f3f4f6; margin: 4px 0; }

/* Modal (native dialog replacing fixed Tailwind modals) */
.dp-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.35);
  backdrop-filter: blur(2px);
}
.dp-modal {
  width: 480px;
  max-width: 90vw;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.12);
}
.dp-modal-title {
  padding: 20px 24px 0;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}
.dp-modal-body {
  padding: 16px 24px;
}
.dp-modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 24px 20px;
}

/* Checkbox label */
.dp-checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
}

/* Basic info inline bar */
.dp-basic-info { padding: 6px 12px; }
.dp-info-inline {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 16px;
}
.dp-info-kv {
  font-size: 12px;
  color: #374151;
}
.dp-info-kv em {
  font-style: normal;
  color: #9ca3af;
  margin-right: 4px;
}

/* Extension tab body */
.dp-ext-tab-body { padding: 8px 12px; }
.dp-ext-tab-body .df-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2px 20px;
}
.dp-ext-tab-body .df-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 24px;
}
.dp-ext-tab-body .df-label {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 500;
  color: #9ca3af;
  white-space: nowrap;
  min-width: 48px;
  text-align: right;
}
.dp-ext-tab-body .df-value {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 500;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dp-ext-tab-body .df-value.df-empty {
  color: #d1d5db;
  font-weight: 400;
}

/* === DATA-COMPACT OVERRIDES === */
.dp-name { font-size: 15px; }
.dp-root :deep(.tm-tabs) { padding: 0 12px; gap: 12px; }
.dp-root :deep(.tm-tab) { padding: 8px 0; font-size: 12px; }
.dp-root :deep(.tm-table th) { padding: 5px 8px; font-size: 10px; }
.dp-root :deep(.tm-table td) { padding: 4px 8px; font-size: 12px; }
.dp-root :deep(.tm-table) { border-radius: 6px; }
.dp-empty { padding: 24px 0; }
.dp-loading { padding: 24px 0; }
.dp-info-grid { padding: 8px 12px; gap: 6px 16px; }
.dp-info-item dt { font-size: 11px; margin-bottom: 1px; }
.dp-info-item dd { font-size: 12px; }
</style>
