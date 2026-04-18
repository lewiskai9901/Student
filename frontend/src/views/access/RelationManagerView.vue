<template>
  <div class="p-4">
    <div class="flex items-center justify-between mb-4">
      <div>
        <h2 class="text-lg font-semibold">关系管理</h2>
        <p class="text-xs text-gray-500 mt-1">统一查询/创建/撤销 access_relations,所有关系必须来自字典</p>
      </div>
      <el-button type="primary" size="small" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon> 新建关系
      </el-button>
    </div>

    <!-- 筛选 -->
    <el-card class="mb-3" shadow="never">
      <el-form :inline="true" size="small">
        <el-form-item label="资源类型">
          <el-select v-model="filter.resourceType" clearable placeholder="全部" style="width: 120px" @change="load">
            <el-option v-for="t in ['org_unit', 'place', 'user']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="关系">
          <el-select v-model="filter.relation" clearable placeholder="全部" style="width: 150px" @change="load">
            <el-option v-for="r in relationOptions" :key="r.relationCode" :label="`${r.relationCode} (${r.relationName})`" :value="r.relationCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="主体类型">
          <el-select v-model="filter.subjectType" clearable placeholder="全部" style="width: 120px" @change="load">
            <el-option v-for="t in ['user', 'org_unit', 'place']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" plain @click="load">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 关系列表 -->
    <el-table :data="rows" stripe size="small" v-loading="loading">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="主体" min-width="220">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ subjectTypeLabel(row.subjectType) }}</el-tag>
          <span class="ml-2">{{ displayName(row, 'subject') }}</span>
          <div class="text-xs text-gray-400 font-mono">{{ shortId(row.subjectId) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="关系" prop="relation" width="150">
        <template #default="{ row }">
          <el-tag size="small" :type="relationTagType(row.relation)">{{ relationLabel(row.relation) }}</el-tag>
          <div class="text-xs text-gray-400">{{ row.relation }}</div>
        </template>
      </el-table-column>
      <el-table-column label="资源" min-width="220">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ resourceTypeLabel(row.resourceType) }}</el-tag>
          <span class="ml-2">{{ displayName(row, 'resource') }}</span>
          <div class="text-xs text-gray-400 font-mono">{{ shortId(row.resourceId) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="级别" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="levelTagType(row.accessLevel)">{{ levelLabel(row.accessLevel) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生效期" width="180">
        <template #default="{ row }">
          <div class="text-xs">
            <span class="text-gray-500">{{ formatTime(row.validFrom) || '—' }}</span>
            <span class="mx-1 text-gray-300">至</span>
            <span :class="row.validTo ? 'text-orange-500' : 'text-green-600'">{{ row.validTo ? formatTime(row.validTo) : '永久' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" size="small" text @click="revoke(row)">撤销</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="mt-3 flex justify-end"
      :current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="p => { page = p; load() }"
    />

    <!-- 创建关系 Dialog -->
    <el-dialog v-model="showCreateDialog" title="新建关系" width="520px">
      <el-form :model="newRel" label-width="100px" size="small">
        <el-form-item label="主体类型">
          <el-select v-model="newRel.subjectType" placeholder="选择主体类型" @change="onTypeChange">
            <el-option v-for="t in fromTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="主体 ID">
          <el-input v-model.number="newRel.subjectId" type="number" placeholder="输入用户/组织/场所 ID" />
        </el-form-item>
        <el-form-item label="关系">
          <el-select v-model="newRel.relation" placeholder="选择关系" filterable>
            <el-option v-for="r in filteredRelations" :key="`${r.relationCode}:${r.fromType}:${r.toType}`"
              :label="`${r.relationCode} - ${r.relationName}`" :value="r.relationCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="资源类型">
          <el-select v-model="newRel.resourceType" placeholder="选择资源类型" @change="onTypeChange">
            <el-option v-for="t in toTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="资源 ID">
          <el-input v-model.number="newRel.resourceId" type="number" placeholder="输入资源 ID" />
        </el-form-item>
        <el-form-item label="访问级别">
          <el-select v-model="newRel.accessLevel">
            <el-option label="FULL" value="FULL" />
            <el-option label="READ_ONLY" value="READ_ONLY" />
            <el-option label="OWNER" value="OWNER" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="newRel.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { accessRelationApi } from '@/api/accessRelation'
import { relationTypeApi, type RelationTypeDef } from '@/api/relationType'

const loading = ref(false)
const rows = ref<any[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const filter = ref({
  resourceType: '',
  relation: '',
  subjectType: ''
})

const relationOptions = ref<RelationTypeDef[]>([])

const showCreateDialog = ref(false)
const newRel = ref({
  subjectType: 'user',
  subjectId: null as number | null,
  relation: '',
  resourceType: 'org_unit',
  resourceId: null as number | null,
  accessLevel: 'FULL',
  remark: ''
})

const fromTypes = computed(() => {
  const s = new Set(relationOptions.value.map(r => r.fromType))
  return Array.from(s).sort()
})
const toTypes = computed(() => {
  const s = new Set(relationOptions.value.map(r => r.toType))
  return Array.from(s).sort()
})

const filteredRelations = computed(() => {
  return relationOptions.value.filter(r =>
    (!newRel.value.subjectType || r.fromType === newRel.value.subjectType) &&
    (!newRel.value.resourceType || r.toType === newRel.value.resourceType)
  )
})

function onTypeChange() {
  // 主体/资源类型变化时,清除不兼容的关系选择
  const valid = filteredRelations.value.some(r => r.relationCode === newRel.value.relation)
  if (!valid) newRel.value.relation = ''
}

function relationTagType(relation: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const def = relationOptions.value.find(r => r.relationCode === relation)
  if (!def) return 'info'
  return {
    OWNERSHIP: 'warning' as const,
    MEMBERSHIP: 'primary' as const,
    ASSOCIATION: 'success' as const,
    DELEGATION: 'danger' as const,
    SUBSCRIPTION: 'info' as const
  }[def.category] || 'info'
}

function relationLabel(code: string): string {
  const def = relationOptions.value.find(r => r.relationCode === code)
  return def ? def.relationName : code
}

function subjectTypeLabel(t: string): string {
  return { user: '用户', org_unit: '组织', place: '场所' }[t as string] || t
}
const resourceTypeLabel = subjectTypeLabel

function levelTagType(level: string): 'primary' | 'success' | 'warning' {
  return {
    READ_ONLY: 'primary' as const,
    FULL: 'success' as const,
    OWNER: 'warning' as const
  }[level as string] || 'success'
}
function levelLabel(level: string): string {
  return { READ_ONLY: '只读', FULL: '读写', OWNER: '所有者' }[level as string] || (level || 'FULL')
}

function displayName(row: any, side: 'subject' | 'resource'): string {
  const meta = row.metadata || {}
  if (side === 'subject') {
    return meta.subjectName || meta.userName || '—'
  }
  // resource 根据 resourceType 选
  if (row.resourceType === 'org_unit') return meta.orgUnitName || '—'
  if (row.resourceType === 'place') return meta.placeName || '—'
  if (row.resourceType === 'user') return meta.userName || meta.subjectName || '—'
  return '—'
}

function shortId(id: number | string | null): string {
  if (id == null) return ''
  const s = String(id)
  return s.length > 8 ? `…${s.slice(-6)}` : s
}

function formatTime(t: string | null): string {
  if (!t) return ''
  return t.replace('T', ' ').substring(5, 16)  // 去掉年份,紧凑显示
}

function resetFilter() {
  filter.value = { resourceType: '', relation: '', subjectType: '' }
  page.value = 1
  load()
}

async function load() {
  loading.value = true
  try {
    const { http } = await import('@/utils/request')
    const resp: any = await http.get('/access-relations', {
      params: {
        resourceType: filter.value.resourceType || undefined,
        subjectType: filter.value.subjectType || undefined,
        relation: filter.value.relation || undefined,
        page: page.value,
        size: pageSize.value
      }
    })
    rows.value = resp?.records || []
    total.value = Number(resp?.total) || rows.value.length
  } catch (e: any) {
    ElMessage.error('加载关系失败: ' + (e?.message || e))
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!newRel.value.subjectType || !newRel.value.subjectId ||
      !newRel.value.relation ||
      !newRel.value.resourceType || !newRel.value.resourceId) {
    ElMessage.warning('请填写所有必填字段')
    return
  }
  try {
    await accessRelationApi.create({
      resourceType: newRel.value.resourceType,
      resourceId: newRel.value.resourceId!,
      relation: newRel.value.relation,
      subjectType: newRel.value.subjectType,
      subjectId: newRel.value.subjectId!,
      accessLevel: newRel.value.accessLevel,
      remark: newRel.value.remark
    } as any)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    await load()
  } catch (e: any) {
    ElMessage.error('创建失败: ' + (e?.message || e))
  }
}

async function revoke(row: any) {
  await ElMessageBox.confirm(
    `确认撤销关系?\n${row.subjectType}:${row.subjectId} -[${row.relation}]-> ${row.resourceType}:${row.resourceId}`,
    '撤销确认', { type: 'warning' }
  )
  try {
    await accessRelationApi.delete(row.id)
    ElMessage.success('已撤销')
    await load()
  } catch (e: any) {
    ElMessage.error('撤销失败: ' + (e?.message || e))
  }
}

onMounted(async () => {
  relationOptions.value = await relationTypeApi.list()
  await load()
})
</script>

<style scoped>
</style>
