<template>
  <div class="tm-page">
    <!-- Header -->
    <div class="tm-header">
      <div>
        <h1 class="tm-title">类型配置</h1>
        <div class="tm-stats">
          <span>组织类型 <b>{{ orgTypes.length }}</b></span>
          <span class="sep" />
          <span>场所类型 <b>{{ placeTypes.length }}</b></span>
          <span class="sep" />
          <span>用户类型 <b>{{ userTypes.length }}</b></span>
        </div>
      </div>
      <button class="tm-btn tm-btn-primary" @click="showCreateDialog">新建类型</button>
    </div>

    <!-- Tabs -->
    <div class="tm-tabs">
      <button :class="['tm-tab', { active: tab === 'ORG_UNIT' }]" @click="tab = 'ORG_UNIT'">组织类型</button>
      <button :class="['tm-tab', { active: tab === 'PLACE' }]" @click="tab = 'PLACE'">场所类型</button>
      <button :class="['tm-tab', { active: tab === 'USER' }]" @click="tab = 'USER'">用户类型</button>
    </div>

    <!-- Content -->
    <div class="tm-table-wrap">
      <table class="tm-table">
        <colgroup>
          <col style="width: 110px" />
          <col />
          <col style="width: 80px" />
          <col style="width: 100px" />
          <col style="width: 80px" />
          <col style="width: 80px" />
          <col style="width: 130px" />
        </colgroup>
        <thead>
          <tr>
            <th>编码</th>
            <th class="text-left">类型名称</th>
            <th>分类</th>
            <th>父类型</th>
            <th>来源</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7" class="tm-empty"><span class="tm-spin" style="display:inline-block;width:16px;height:16px;border:2px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;" /> 加载中...</td>
          </tr>
          <tr v-else-if="currentTypes.length === 0">
            <td colspan="7" class="tm-empty">暂无类型配置</td>
          </tr>
          <tr
            v-for="t in currentTypes" :key="t.id"
            :class="isPluginDisabled(t) ? 'row-disabled-by-plugin' : ''"
            :title="isPluginDisabled(t) ? '所属插件已禁用 — 此类型级联软失效' : undefined"
          >
            <td>
              <span class="tm-code">{{ t.typeCode }}</span>
              <span
                v-if="isPluginDisabled(t)"
                class="disabled-by-plugin-badge"
                title="所属插件已禁用"
              >插件禁用</span>
            </td>
            <td class="text-left" style="font-weight: 500;">{{ t.typeName }}</td>
            <td><span class="tm-chip tm-chip-gray">{{ categoryLabel(t.entityType, t.category) }}</span></td>
            <td style="font-size: 12px; color: #6b7280;">{{ t.parentTypeCode || '-' }}</td>
            <td>
              <span :class="['tm-chip', t.isPluginRegistered ? 'tm-chip-blue' : 'tm-chip-amber']">
                {{ t.isPluginRegistered ? '插件' : '自定义' }}
              </span>
            </td>
            <td>
              <span :class="['tm-chip', t.isEnabled ? 'tm-chip-green' : 'tm-chip-red']">
                {{ t.isEnabled ? '启用' : '禁用' }}
              </span>
            </td>
            <td>
              <button class="tm-action" @click="showEditDialog(t)">编辑</button>
              <button class="tm-action" @click="showSchemaDialog(t)">字段</button>
              <button v-if="!t.isPluginRegistered" class="tm-action tm-action-danger" @click="handleDelete(t)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Create/Edit Drawer -->
    <Transition name="tm-drawer">
      <div v-if="dialogVisible" class="tm-drawer-overlay" @click.self="dialogVisible = false">
        <div class="tm-drawer">
          <div class="tm-drawer-header">
            <h3 class="tm-drawer-title">{{ editingType ? '编辑类型' : '新建类型' }}</h3>
            <button class="tm-drawer-close" @click="dialogVisible = false">&times;</button>
          </div>
          <div class="tm-drawer-body">
            <div class="tm-section">
              <h4 class="tm-section-title">基本信息</h4>
              <div class="tm-hint" style="margin-bottom: 12px;">
                实体类型：<b>{{ entityTypeLabel(form.entityType) }}</b>
              </div>
              <div class="tm-fields tm-cols-2">
                <div class="tm-field">
                  <label class="tm-label">类型编码 <span class="req">*</span></label>
                  <input v-model="form.typeCode" class="tm-input" :disabled="!!editingType" placeholder="如 TEACHING_GROUP" />
                </div>
                <div class="tm-field">
                  <label class="tm-label">类型名称 <span class="req">*</span></label>
                  <input v-model="form.typeName" class="tm-input" placeholder="如 教研室" />
                </div>
              </div>
              <div class="tm-field">
                <label class="tm-label">分类</label>
                <select v-model="form.category" class="tm-field-select">
                  <option value="">— 请选择分类 —</option>
                  <option v-for="c in currentCategories" :key="c.code" :value="c.code">
                    {{ c.label }}
                  </option>
                </select>
                <div v-if="selectedCategoryHint" class="tm-hint">{{ selectedCategoryHint }}</div>
              </div>
              <div class="tm-field">
                <label class="tm-label">父类型</label>
                <select v-model="form.parentTypeCode" class="tm-field-select">
                  <option value="">— 顶级类型 —</option>
                  <option v-for="t in parentCandidates" :key="t.typeCode" :value="t.typeCode">
                    {{ t.typeName }}（{{ t.typeCode }}）
                  </option>
                </select>
              </div>
              <div class="tm-field">
                <label class="tm-label">允许的子类型</label>
                <div class="tm-checkgrid">
                  <label v-for="t in childCandidates" :key="t.typeCode" class="tm-checkitem">
                    <input type="checkbox" :value="t.typeCode" v-model="form.allowedChildCodesArr" />
                    <span>{{ t.typeName }}（{{ t.typeCode }}）</span>
                  </label>
                  <div v-if="childCandidates.length === 0" style="color:#9ca3af;font-size:12px;">暂无可选子类型</div>
                </div>
              </div>
            </div>
          </div>
          <div class="tm-drawer-footer">
            <button class="tm-btn tm-btn-secondary" @click="dialogVisible = false">取消</button>
            <button class="tm-btn tm-btn-primary" :disabled="saving" @click="handleSave">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Schema Editor Drawer -->
    <Transition name="tm-drawer">
      <div v-if="schemaVisible" class="tm-drawer-overlay" @click.self="schemaVisible = false">
        <div class="tm-drawer" style="width: 600px;">
          <div class="tm-drawer-header">
            <h3 class="tm-drawer-title">{{ schemaType?.typeName }} — 字段配置</h3>
            <button class="tm-drawer-close" @click="schemaVisible = false">&times;</button>
          </div>
          <div class="tm-drawer-body">
            <div class="tm-section">
              <div v-if="schemaFields.length === 0" style="color: #9ca3af; font-size: 13px; padding: 20px 0; text-align: center;">
                暂无扩展字段
              </div>
              <div v-for="(f, i) in schemaFields" :key="i" style="display: flex; align-items: center; gap: 8px; padding: 8px 0; border-bottom: 1px solid #f3f4f6;">
                <span style="font-size: 13px; font-weight: 500; flex: 1;">{{ f.label }} <span class="tm-code" style="margin-left: 4px;">{{ f.key }}</span></span>
                <span class="tm-chip tm-chip-gray">{{ f.type }}</span>
                <span v-if="f.required" class="tm-chip tm-chip-red">必填</span>
                <span :class="['tm-chip', f.system ? 'tm-chip-blue' : 'tm-chip-amber']">{{ f.system ? '系统' : '自定义' }}</span>
                <button v-if="!f.system" class="tm-action tm-action-danger" @click="removeCustomField(i)">删</button>
              </div>
            </div>

            <!-- Add custom field -->
            <div class="tm-section">
              <h4 class="tm-section-title">添加自定义字段</h4>
              <div class="tm-fields tm-cols-3">
                <div class="tm-field">
                  <label class="tm-label">字段Key</label>
                  <input v-model="newField.key" class="tm-input" placeholder="如 motto" />
                </div>
                <div class="tm-field">
                  <label class="tm-label">显示名</label>
                  <input v-model="newField.label" class="tm-input" placeholder="如 班训" />
                </div>
                <div class="tm-field">
                  <label class="tm-label">类型</label>
                  <select v-model="newField.type" class="tm-field-select">
                    <option value="text">文本</option>
                    <option value="number">数字</option>
                    <option value="date">日期</option>
                    <option value="boolean">布尔</option>
                    <option value="select">下拉选择</option>
                    <option value="textarea">多行文本</option>
                  </select>
                </div>
              </div>
              <button class="tm-btn tm-btn-secondary" style="margin-top: 8px;" @click="addCustomField">添加字段</button>
            </div>
          </div>
          <div class="tm-drawer-footer">
            <button class="tm-btn tm-btn-secondary" @click="schemaVisible = false">关闭</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { entityTypeApi, type CategoryOption } from '@/api/entityType'
import { http } from '@/utils/request'

const tab = ref<'ORG_UNIT' | 'PLACE' | 'USER'>('ORG_UNIT')
const allTypes = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingType = ref<any>(null)
const form = ref<any>({ entityType: 'ORG_UNIT' })

const schemaVisible = ref(false)
const schemaType = ref<any>(null)
const schemaFields = ref<any[]>([])
const newField = ref({ key: '', label: '', type: 'text' })

const categoryMap = ref<Record<string, CategoryOption[]>>({ ORG_UNIT: [], PLACE: [], USER: [] })
const currentCategories = computed(() => categoryMap.value[form.value.entityType] || [])
function categoryLabel(entityType: string, code?: string) {
  if (!code) return '-'
  const opt = (categoryMap.value[entityType] || []).find(c => c.code === code)
  return opt ? opt.label : code
}
const selectedCategoryHint = computed(() => {
  const opt = currentCategories.value.find(c => c.code === form.value.category)
  if (!opt) return ''
  const enabled = Object.entries(opt.defaultFeatures || {}).filter(([, v]) => v).map(([k]) => k)
  return enabled.length ? `默认启用：${enabled.join('、')}` : '此分类默认不启用任何 feature'
})

const orgTypes = computed(() => allTypes.value.filter(t => t.entityType === 'ORG_UNIT'))
const placeTypes = computed(() => allTypes.value.filter(t => t.entityType === 'PLACE'))
const userTypes = computed(() => allTypes.value.filter(t => t.entityType === 'USER'))
const currentTypes = computed(() => allTypes.value.filter(t => t.entityType === tab.value))

const ENTITY_TYPE_LABELS: Record<string, string> = { ORG_UNIT: '组织类型', PLACE: '场所类型', USER: '用户类型' }
function entityTypeLabel(et: string) { return ENTITY_TYPE_LABELS[et] || et }

// 父类型/子类型候选：同 entityType 下、排除自己
const parentCandidates = computed(() =>
  allTypes.value.filter(t =>
    t.entityType === form.value.entityType &&
    t.typeCode !== form.value.typeCode
  )
)
const childCandidates = computed(() =>
  allTypes.value.filter(t =>
    t.entityType === form.value.entityType &&
    t.typeCode !== form.value.typeCode
  )
)

/** pluginEnabled 从后端返回的 tinyint 字段可能是 0/1 (number) 或 boolean */
function isPluginDisabled(t: any): boolean {
  const v = t?.pluginEnabled
  if (v === false) return true
  if (v === 0) return true
  return false
}

async function loadAll() {
  loading.value = true
  try {
    // includeDisabled=true: 管理员视角包含所属插件被禁的类型 (灰显显示)
    const results = await Promise.all([
      entityTypeApi.list('ORG_UNIT', true),
      entityTypeApi.list('PLACE', true),
      entityTypeApi.list('USER', true),
    ])
    allTypes.value = [
      ...((results[0] as any).data || results[0] || []),
      ...((results[1] as any).data || results[1] || []),
      ...((results[2] as any).data || results[2] || []),
    ]
  } catch { allTypes.value = [] } finally { loading.value = false }
}

async function loadCategories() {
  const types: Array<'ORG_UNIT' | 'PLACE' | 'USER'> = ['ORG_UNIT', 'PLACE', 'USER']
  const results = await Promise.allSettled(types.map(t => entityTypeApi.getCategories(t)))
  const map: Record<string, CategoryOption[]> = { ORG_UNIT: [], PLACE: [], USER: [] }
  results.forEach((r, i) => {
    if (r.status === 'fulfilled') {
      const v: any = r.value
      map[types[i]] = (Array.isArray(v) ? v : (v?.data || [])) as CategoryOption[]
    } else {
      console.warn(`[EntityTypeConfig] load categories ${types[i]} failed:`, r.reason)
    }
  })
  categoryMap.value = map
}

function showCreateDialog() {
  editingType.value = null
  form.value = { entityType: tab.value, typeCode: '', typeName: '', category: '', parentTypeCode: '', allowedChildCodesArr: [] }
  dialogVisible.value = true
}

function showEditDialog(t: any) {
  editingType.value = t
  const children = t.allowedChildTypeCodes
    ? (typeof t.allowedChildTypeCodes === 'string' ? JSON.parse(t.allowedChildTypeCodes) : t.allowedChildTypeCodes)
    : []
  form.value = {
    entityType: t.entityType,
    typeCode: t.typeCode,
    typeName: t.typeName,
    category: t.category || '',
    parentTypeCode: t.parentTypeCode || '',
    allowedChildCodesArr: Array.isArray(children) ? children : [],
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.typeCode?.trim() || !form.value.typeName?.trim()) { ElMessage.warning('请填写编码和名称'); return }
  saving.value = true
  try {
    const payload = {
      entityType: form.value.entityType,
      typeCode: form.value.typeCode.trim(),
      typeName: form.value.typeName.trim(),
      category: form.value.category || null,
      parentTypeCode: form.value.parentTypeCode || null,
      allowedChildTypeCodes: Array.isArray(form.value.allowedChildCodesArr) ? form.value.allowedChildCodesArr : [],
      metadataSchema: '{"fields":[]}',
      features: '{}',
      isPluginRegistered: false,
      isEnabled: true,
    }
    if (editingType.value) {
      await http.put(`/entity-type-configs/${editingType.value.id}`, payload)
    } else {
      await http.post('/entity-type-configs', payload)
    }
    ElMessage.success('保存成功'); dialogVisible.value = false; loadAll()
  } catch { ElMessage.error('保存失败') } finally { saving.value = false }
}

async function handleDelete(t: any) {
  if (t.isPluginRegistered) { ElMessage.warning('插件注册的类型不能删除'); return }
  await ElMessageBox.confirm(`确定删除类型"${t.typeName}"？`, '删除', { type: 'warning' })
  try {
    await http.delete(`/entity-type-configs/${t.id}`)
    ElMessage.success('已删除'); loadAll()
  } catch { ElMessage.error('删除失败') }
}

function showSchemaDialog(t: any) {
  schemaType.value = t
  const schema = typeof t.metadataSchema === 'string' ? JSON.parse(t.metadataSchema || '{"fields":[]}') : t.metadataSchema || { fields: [] }
  schemaFields.value = schema.fields || []
  newField.value = { key: '', label: '', type: 'text' }
  schemaVisible.value = true
}

async function addCustomField() {
  if (!newField.value.key || !newField.value.label) { ElMessage.warning('请填写字段Key和显示名'); return }
  try {
    await entityTypeApi.addCustomField(schemaType.value.id, { key: newField.value.key, label: newField.value.label, type: newField.value.type, group: '自定义', required: false, system: false })
    ElMessage.success('已添加')
    schemaFields.value.push({ ...newField.value, system: false, required: false, group: '自定义' })
    newField.value = { key: '', label: '', type: 'text' }
    loadAll()
  } catch { ElMessage.error('添加失败') }
}

async function removeCustomField(idx: number) {
  const field = schemaFields.value[idx]
  if (field.system) { ElMessage.warning('系统字段不能删除'); return }
  try {
    await entityTypeApi.removeCustomField(schemaType.value.id, field.key)
    schemaFields.value.splice(idx, 1)
    ElMessage.success('已删除'); loadAll()
  } catch { ElMessage.error('删除失败') }
}

onMounted(() => { loadAll(); loadCategories() })
</script>

<style>
@import '@/styles/teaching-ui.css';

.tm-checkgrid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 8px 12px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  max-height: 200px;
  overflow-y: auto;
  background: #fafafa;
}
.tm-checkitem {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  user-select: none;
}
.tm-checkitem input[type="checkbox"] {
  margin: 0;
  cursor: pointer;
}

/* 插件级联禁用的类型行: 灰显 */
.row-disabled-by-plugin > td {
  opacity: 0.55;
  background-color: #fafaf9;
}
.row-disabled-by-plugin:hover > td {
  background-color: #fef3c7 !important;
}

.disabled-by-plugin-badge {
  display: inline-block;
  padding: 1px 7px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  color: #a16207;
  background: #fef3c7;
  border: 1px solid #fcd34d;
  line-height: 1.4;
  cursor: help;
  margin-left: 4px;
}
</style>
