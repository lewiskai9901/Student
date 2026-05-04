<template>
  <div class="flex h-full flex-col bg-white">
    <!-- 顶部: 类型标识 + 操作 -->
    <div class="flex items-center gap-3 border-b border-gray-200 px-5 py-3">
      <component :is="entityIcon" class="h-6 w-6" :class="entityIconColor" />
      <div class="min-w-0 flex-1">
        <div class="flex items-center gap-2">
          <h2 class="truncate text-base font-semibold text-gray-800">
            {{ isNew ? '新建类型' : form.typeName || '(未命名)' }}
          </h2>
          <span class="inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-medium"
                :class="originBadgeClass">
            {{ originLabel }}
          </span>
          <span v-if="!isNew" class="rounded-full bg-gray-100 px-2 py-0.5 text-[10px] text-gray-600">
            {{ ENTITY_LABELS[form.entityType] }}
          </span>
        </div>
        <div v-if="!isNew" class="mt-0.5 font-mono text-xs text-gray-400">{{ form.typeCode }}</div>
      </div>
      <div v-if="!isNew" class="flex items-center gap-2">
        <button class="tm-btn tm-btn-secondary" @click="handleCancel">放弃修改</button>
        <button class="tm-btn tm-btn-primary" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
      <div v-else class="flex items-center gap-2">
        <button class="tm-btn tm-btn-secondary" @click="$emit('cancel')">取消</button>
        <button class="tm-btn tm-btn-primary" :disabled="saving" @click="handleSave">
          {{ saving ? '创建中...' : '创建' }}
        </button>
      </div>
    </div>

    <!-- 插件禁用 banner -->
    <div v-if="isPluginDisabled"
         class="flex items-center gap-2 border-b border-amber-200 bg-amber-50 px-5 py-2 text-xs text-amber-800">
      <AlertTriangle class="h-4 w-4 flex-shrink-0" />
      <span>所属插件已禁用, 可查看但不可编辑或保存. 请先在插件平台启用.</span>
    </div>

    <!-- 关系图 -->
    <div v-if="!isNew && (parentType || childTypes.length)" class="border-b border-gray-100 bg-gray-50/60 px-5 py-3">
      <div class="mb-1 text-[10px] font-medium uppercase tracking-wide text-gray-500">类型关系</div>
      <div class="flex items-center gap-3 overflow-x-auto">
        <button v-if="parentType"
                class="flex-shrink-0 rounded-md border border-gray-200 bg-white px-3 py-1.5 text-xs hover:border-blue-300 hover:bg-blue-50"
                @click="$emit('select', parentType)">
          <span class="text-[10px] text-gray-400">父类型</span>
          <div class="font-medium text-gray-700">{{ parentType.typeName }}</div>
        </button>
        <ArrowRight v-if="parentType" class="h-4 w-4 flex-shrink-0 text-gray-300" />
        <div class="flex-shrink-0 rounded-md border-2 border-blue-500 bg-blue-50 px-3 py-1.5 text-xs">
          <span class="text-[10px] text-blue-600">当前</span>
          <div class="font-semibold text-blue-700">{{ form.typeName || '-' }}</div>
        </div>
        <template v-if="childTypes.length">
          <ArrowRight class="h-4 w-4 flex-shrink-0 text-gray-300" />
          <div class="flex flex-wrap items-center gap-1">
            <button v-for="c in childTypes" :key="c.typeCode"
                    class="rounded-md border border-gray-200 bg-white px-2.5 py-1 text-[11px] hover:border-blue-300 hover:bg-blue-50"
                    @click="$emit('select', c)">
              {{ c.typeName }}
            </button>
          </div>
        </template>
      </div>
    </div>

    <!-- Tab bar -->
    <div class="flex border-b border-gray-200 px-5">
      <button v-for="t in tabs" :key="t.key"
              class="relative px-3 py-2 text-xs font-medium transition-colors"
              :class="activeTab === t.key
                ? 'text-blue-600'
                : 'text-gray-500 hover:text-gray-700'"
              @click="activeTab = t.key">
        {{ t.label }}
        <span v-if="t.badge != null" class="ml-1 rounded bg-gray-100 px-1 text-[10px] text-gray-600">{{ t.badge }}</span>
        <span v-if="activeTab === t.key" class="absolute bottom-0 left-0 h-0.5 w-full bg-blue-500"></span>
      </button>
    </div>

    <!-- Tab content -->
    <div class="flex-1 overflow-y-auto px-5 py-4">
      <!-- 基本信息 -->
      <div v-if="activeTab === 'basic'" class="space-y-4">
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="mb-1 block text-xs font-medium text-gray-600">
              编码 <span class="text-red-500">*</span>
            </label>
            <input v-model="form.typeCode"
                   class="tm-input" :disabled="!isNew"
                   placeholder="如 TEACHING_GROUP" />
            <div v-if="!isNew" class="mt-1 text-[10px] text-gray-400">编码不可修改</div>
          </div>
          <div>
            <label class="mb-1 flex items-center gap-1 text-xs font-medium text-gray-600">
              名称 <span class="text-red-500">*</span>
              <span v-if="isOverridden('typeName')"
                    class="ml-1 inline-flex cursor-help items-center gap-0.5 rounded-full bg-indigo-100 px-1.5 py-0 text-[9px] font-semibold text-indigo-700"
                    title="已被管理员覆写, 不受插件重启影响">
                编辑 已覆写
              </span>
              <button v-if="isOverridden('typeName')"
                      class="ml-auto text-[10px] font-normal text-blue-600 hover:underline"
                      @click="resetField('typeName')">
                恢复插件默认
              </button>
            </label>
            <input v-model="form.typeName" class="tm-input"
                   :disabled="isPluginDisabled"
                   placeholder="如 教研室" />
          </div>
        </div>

        <div v-if="isNew">
          <label class="mb-1 block text-xs font-medium text-gray-600">实体类型</label>
          <div class="flex gap-2">
            <button v-for="et in ['ORG_UNIT', 'PLACE', 'USER']" :key="et"
                    class="flex items-center gap-1.5 rounded-md border px-3 py-1.5 text-xs transition-colors"
                    :class="form.entityType === et
                      ? 'border-blue-500 bg-blue-50 text-blue-700'
                      : 'border-gray-200 text-gray-600 hover:border-gray-300'"
                    @click="form.entityType = et">
              {{ ENTITY_LABELS[et] }}
            </button>
          </div>
        </div>

        <div>
          <label class="mb-1 flex items-center gap-1 text-xs font-medium text-gray-600">
            分类
            <span v-if="isOverridden('category')"
                  class="ml-1 inline-flex cursor-help items-center gap-0.5 rounded-full bg-indigo-100 px-1.5 py-0 text-[9px] font-semibold text-indigo-700"
                  title="已被管理员覆写, 不受插件重启影响">
              编辑 已覆写
            </span>
            <button v-if="isOverridden('category')"
                    class="ml-auto text-[10px] font-normal text-blue-600 hover:underline"
                    @click="resetField('category')">
              恢复插件默认
            </button>
          </label>
          <select v-model="form.category" class="tm-input"
                  :disabled="isPluginDisabled"
                  @change="onCategoryChange">
            <option value="">— 请选择分类 —</option>
            <option v-for="c in currentCategories" :key="c.code" :value="c.code">
              {{ c.label }}
            </option>
          </select>
          <div v-if="selectedCategoryHint" class="mt-1 text-[11px] text-blue-600">
            {{ selectedCategoryHint }}
          </div>
          <div v-if="form.category" class="mt-1 text-[10px] text-gray-400">
            分类决定业务能力 (数据权限边界/考勤/排课 等), 切换会重置 features 为该分类默认值
          </div>
        </div>
      </div>

      <!-- 字段定义 -->
      <div v-else-if="activeTab === 'fields'" class="space-y-3">
        <div v-if="form.metadataSchema.fields.length === 0"
             class="rounded-md border border-dashed border-gray-200 py-8 text-center text-xs text-gray-400">
          暂无扩展字段
        </div>
        <div v-else class="space-y-2">
          <div v-for="(f, i) in form.metadataSchema.fields" :key="i"
               class="flex items-center gap-2 rounded-md border border-gray-200 bg-white px-3 py-2">
            <div class="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded bg-gray-100">
              <component :is="fieldTypeIcon(f.type)" class="h-3.5 w-3.5 text-gray-500" />
            </div>
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-2">
                <span class="truncate text-sm font-medium text-gray-700">{{ f.label }}</span>
                <span class="rounded bg-gray-100 px-1.5 py-0.5 font-mono text-[10px] text-gray-500">{{ f.key }}</span>
                <span class="rounded bg-gray-100 px-1.5 py-0.5 text-[10px] text-gray-600">{{ f.type }}</span>
              </div>
              <div class="mt-0.5 flex items-center gap-2 text-[10px] text-gray-400">
                <span v-if="f.group">分组: {{ f.group }}</span>
                <span v-if="f.required" class="rounded bg-red-50 px-1 text-red-600">必填</span>
                <span :class="f.system ? 'text-blue-500' : 'text-amber-600'">
                  {{ f.system ? '系统字段' : '自定义' }}
                </span>
              </div>
            </div>
            <button v-if="!f.system"
                    class="rounded p-1 text-gray-400 hover:bg-red-50 hover:text-red-600"
                    :disabled="isPluginDisabled"
                    @click="removeField(i)">
              <Trash2 class="h-3.5 w-3.5" />
            </button>
          </div>
        </div>

        <!-- 添加字段 -->
        <div v-if="!isNew && !isPluginDisabled" class="rounded-md border border-gray-200 bg-gray-50 p-3">
          <div class="mb-2 text-xs font-medium text-gray-600">+ 添加自定义字段</div>
          <div class="grid grid-cols-[1fr_1fr_110px_80px] gap-2">
            <input v-model="newField.key" class="tm-input" placeholder="字段 key (如 motto)" />
            <input v-model="newField.label" class="tm-input" placeholder="显示名 (如 班训)" />
            <select v-model="newField.type" class="tm-input">
              <option value="text">文本</option>
              <option value="number">数字</option>
              <option value="date">日期</option>
              <option value="boolean">布尔</option>
              <option value="select">下拉</option>
              <option value="textarea">多行</option>
            </select>
            <button class="h-8 rounded-md bg-blue-600 text-xs font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                    :disabled="!newField.key || !newField.label"
                    @click="addField">添加</button>
          </div>
        </div>
      </div>

      <!-- 父子关系 -->
      <div v-else-if="activeTab === 'hierarchy'" class="space-y-4">
        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">父类型</label>
          <select v-model="form.parentTypeCode" class="tm-input"
                  :disabled="isPluginDisabled">
            <option value="">— 顶级类型 (无父) —</option>
            <option v-for="t in parentCandidates" :key="t.typeCode" :value="t.typeCode">
              {{ t.typeName }} ({{ t.typeCode }})
            </option>
          </select>
          <div class="mt-1 text-[11px] text-gray-400">
            决定本类型在组织/场所/用户树中的归属位置
          </div>
        </div>

        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">
            允许的子类型
            <span class="ml-1 text-[10px] text-gray-400">({{ form.allowedChildCodesArr.length }} 项)</span>
          </label>
          <div class="max-h-80 overflow-y-auto rounded-md border border-gray-200 bg-gray-50 p-2">
            <div v-if="childCandidates.length === 0"
                 class="py-4 text-center text-xs text-gray-400">暂无可选子类型</div>
            <label v-else v-for="t in childCandidates" :key="t.typeCode"
                   class="flex cursor-pointer items-center gap-2 rounded px-2 py-1 text-xs hover:bg-white">
              <input type="checkbox"
                     :value="t.typeCode"
                     v-model="form.allowedChildCodesArr"
                     :disabled="isPluginDisabled"
                     class="h-3.5 w-3.5" />
              <span class="flex-1">{{ t.typeName }}</span>
              <span class="font-mono text-[10px] text-gray-400">{{ t.typeCode }}</span>
            </label>
          </div>
        </div>
      </div>

      <!-- JSON 视图 -->
      <div v-else-if="activeTab === 'json'" class="space-y-3">
        <div class="rounded-md border border-gray-200 bg-gray-50 p-3">
          <div class="mb-1 text-[10px] font-medium uppercase text-gray-500">metadata_schema (只读)</div>
          <pre class="overflow-x-auto font-mono text-[11px] text-gray-700">{{ schemaJson }}</pre>
        </div>
        <div class="rounded-md border border-gray-200 bg-gray-50 p-3">
          <div class="mb-1 text-[10px] font-medium uppercase text-gray-500">features (只读)</div>
          <pre class="overflow-x-auto font-mono text-[11px] text-gray-700">{{ featuresJson }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  Building2, MapPin, User,
  AlertTriangle, ArrowRight, Trash2,
  Type, Hash, Calendar, ToggleLeft, List, AlignLeft,
} from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { entityTypeApi, type EntityTypeConfig, type CategoryOption } from '@/api/entityType'
import { http } from '@/utils/request'

interface Props {
  type: EntityTypeConfig | null
  isNew: boolean
  allTypes: EntityTypeConfig[]
  categoryMap: Record<string, CategoryOption[]>
}
const props = defineProps<Props>()
const emit = defineEmits<{
  saved: [t: EntityTypeConfig]
  cancel: []
  select: [t: EntityTypeConfig]
}>()

const ENTITY_LABELS: Record<string, string> = {
  ORG_UNIT: '组织类型', PLACE: '场所类型', USER: '用户类型',
}

const activeTab = ref<'basic' | 'fields' | 'hierarchy' | 'json'>('basic')
const saving = ref(false)

interface FormState {
  entityType: string
  typeCode: string
  typeName: string
  category: string
  parentTypeCode: string
  allowedChildCodesArr: string[]
  metadataSchema: { fields: any[] }
  features: Record<string, boolean>
}

const form = ref<FormState>(emptyForm())
const newField = ref({ key: '', label: '', type: 'text' })

function emptyForm(): FormState {
  return {
    entityType: 'ORG_UNIT',
    typeCode: '',
    typeName: '',
    category: '',
    parentTypeCode: '',
    allowedChildCodesArr: [],
    metadataSchema: { fields: [] },
    features: {},
  }
}

watch(() => props.type, (t) => {
  if (!t) { form.value = emptyForm(); return }
  const schema = typeof t.metadataSchema === 'string'
    ? JSON.parse(t.metadataSchema || '{"fields":[]}')
    : t.metadataSchema || { fields: [] }
  const children = t.allowedChildTypeCodes
    ? (typeof t.allowedChildTypeCodes === 'string' ? JSON.parse(t.allowedChildTypeCodes as any) : t.allowedChildTypeCodes)
    : []
  const features = typeof t.features === 'string'
    ? JSON.parse(t.features || '{}')
    : t.features || {}
  form.value = {
    entityType: t.entityType,
    typeCode: t.typeCode,
    typeName: t.typeName,
    category: t.category || '',
    parentTypeCode: t.parentTypeCode || '',
    allowedChildCodesArr: Array.isArray(children) ? children : [],
    metadataSchema: schema,
    features,
  }
  activeTab.value = 'basic'
}, { immediate: true })

watch(() => props.isNew, (isNew) => {
  if (isNew) {
    form.value = emptyForm()
    activeTab.value = 'basic'
  }
})

const isPluginDisabled = computed(() => {
  const t = props.type
  if (!t) return false
  const v = t.pluginEnabled
  return v === false || v === 0
})

const entityIcon = computed(() =>
  ({ ORG_UNIT: Building2, PLACE: MapPin, USER: User } as any)[form.value.entityType] || Building2)
const entityIconColor = computed(() =>
  ({ ORG_UNIT: 'text-blue-500', PLACE: 'text-emerald-500', USER: 'text-amber-500' } as any)[form.value.entityType] || 'text-gray-500')

const originLabel = computed(() => props.isNew
  ? '待创建'
  : (props.type?.isPluginRegistered ? '插件' : '自定义'))
const originBadgeClass = computed(() => props.isNew
  ? 'bg-gray-100 text-gray-600'
  : (props.type?.isPluginRegistered ? 'bg-blue-100 text-blue-700' : 'bg-amber-100 text-amber-700'))

const currentCategories = computed(() => props.categoryMap[form.value.entityType] || [])
const selectedCategoryHint = computed(() => {
  const opt = currentCategories.value.find(c => c.code === form.value.category)
  if (!opt) return ''
  const enabled = Object.entries(opt.defaultFeatures || {}).filter(([, v]) => v).map(([k]) => k)
  return enabled.length ? `默认启用: ${enabled.join('、')}` : '此分类默认不启用任何 feature'
})

const parentCandidates = computed(() =>
  props.allTypes.filter(t =>
    t.entityType === form.value.entityType && t.typeCode !== form.value.typeCode
  )
)
const childCandidates = computed(() =>
  props.allTypes.filter(t =>
    t.entityType === form.value.entityType && t.typeCode !== form.value.typeCode
  )
)

const parentType = computed(() => {
  if (!form.value.parentTypeCode) return null
  return props.allTypes.find(t =>
    t.entityType === form.value.entityType && t.typeCode === form.value.parentTypeCode
  ) || null
})
const childTypes = computed(() => {
  if (!form.value.typeCode) return []
  return props.allTypes.filter(t =>
    t.entityType === form.value.entityType && t.parentTypeCode === form.value.typeCode
  )
})

const tabs = computed(() => [
  { key: 'basic',     label: '基本信息', badge: null },
  { key: 'fields',    label: '字段定义', badge: form.value.metadataSchema.fields.length },
  { key: 'hierarchy', label: '父子关系', badge: form.value.allowedChildCodesArr.length },
  { key: 'json',      label: 'JSON',    badge: null },
])

const schemaJson = computed(() => JSON.stringify(form.value.metadataSchema, null, 2))
const featuresJson = computed(() => JSON.stringify(form.value.features, null, 2))

function fieldTypeIcon(t: string) {
  return { text: Type, number: Hash, date: Calendar, boolean: ToggleLeft, select: List, textarea: AlignLeft }[t] || Type
}

const overriddenSet = computed<Set<string>>(() => {
  const raw = props.type?.overriddenFields
  if (!raw) return new Set()
  try {
    const arr = typeof raw === 'string' ? JSON.parse(raw) : raw
    return new Set(Array.isArray(arr) ? arr : [])
  } catch {
    return new Set()
  }
})

function isOverridden(field: string): boolean {
  if (!props.type?.isPluginRegistered) return false
  return overriddenSet.value.has(field)
}

async function resetField(field: string) {
  if (!props.type?.id) return
  try {
    await ElMessageBox.confirm(
      `恢复字段 "${field}" 为插件默认值?\n当前管理员修改会丢失。`,
      '恢复确认', { type: 'warning' }
    )
    await entityTypeApi.resetField(props.type.id, field)
    ElMessage.success('已恢复为插件默认')
    emit('saved', props.type!)
  } catch (e: any) {
    if (e === 'cancel' || e?.message === 'cancel') return
    ElMessage.error('恢复失败')
  }
}

async function onCategoryChange() {
  if (!props.type || props.isNew) return
  const cat = form.value.category
  const originalCat = props.type.category || ''
  if (cat === originalCat) return
  try {
    await ElMessageBox.confirm(
      `切换分类为 "${cat}" ?\n\n`
      + `• 业务能力 (数据权限边界/考勤/排课 等) 将按新分类默认重置\n`
      + `• 此改动会被标记为"管理员覆写", 插件重启不会覆盖\n`
      + `• 对于插件类型, 新分类可能与插件原意不一致, 请谨慎`,
      '切换分类', { type: 'warning', confirmButtonText: '确认切换', cancelButtonText: '取消' }
    )
    // 用新 category 的默认 features 重置 form.features
    const opt = currentCategories.value.find(c => c.code === cat)
    form.value.features = opt?.defaultFeatures ? { ...opt.defaultFeatures } : {}
  } catch (e: any) {
    // 取消 > 回滚 category
    form.value.category = originalCat
  }
}

async function addField() {
  if (!props.type?.id) return
  if (!newField.value.key || !newField.value.label) {
    ElMessage.warning('请填写字段 key 和显示名'); return
  }
  try {
    await entityTypeApi.addCustomField(props.type.id, {
      key: newField.value.key,
      label: newField.value.label,
      type: newField.value.type,
      group: '自定义', required: false, system: false,
    })
    form.value.metadataSchema.fields.push({
      ...newField.value, system: false, required: false, group: '自定义',
    })
    newField.value = { key: '', label: '', type: 'text' }
    ElMessage.success('已添加')
    emit('saved', props.type)
  } catch {
    ElMessage.error('添加失败')
  }
}

async function removeField(idx: number) {
  const field = form.value.metadataSchema.fields[idx]
  if (field.system) { ElMessage.warning('系统字段不可删除'); return }
  if (!props.type?.id) return
  try {
    await ElMessageBox.confirm(`确定删除字段 "${field.label}" ?`, '删除自定义字段', { type: 'warning' })
    await entityTypeApi.removeCustomField(props.type.id, field.key)
    form.value.metadataSchema.fields.splice(idx, 1)
    ElMessage.success('已删除')
    emit('saved', props.type)
  } catch (e: any) {
    if (e === 'cancel' || e?.message === 'cancel') return
    ElMessage.error('删除失败')
  }
}

async function handleSave() {
  if (!form.value.typeCode?.trim() || !form.value.typeName?.trim()) {
    ElMessage.warning('请填写编码和名称'); return
  }
  saving.value = true
  try {
    const payload: Record<string, any> = {
      entityType: form.value.entityType,
      typeCode: form.value.typeCode.trim(),
      typeName: form.value.typeName.trim(),
      category: form.value.category || null,
      parentTypeCode: form.value.parentTypeCode || null,
      allowedChildTypeCodes: form.value.allowedChildCodesArr,
      metadataSchema: JSON.stringify(form.value.metadataSchema),
      features: JSON.stringify(form.value.features),
      isPluginRegistered: props.type?.isPluginRegistered ?? false,
      isEnabled: props.type?.isEnabled ?? true,
    }
    if (props.isNew) {
      const res: any = await http.post('/entity-type-configs', payload)
      ElMessage.success('创建成功')
      emit('saved', res.data || res)
    } else {
      await http.put(`/entity-type-configs/${props.type!.id}`, payload)
      ElMessage.success('保存成功')
      emit('saved', props.type!)
    }
  } catch {
    ElMessage.error(props.isNew ? '创建失败' : '保存失败')
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  // Re-trigger watch to reset form from prop
  const t = props.type
  if (t) {
    const tmp = t; // trigger watcher via reassign
    (props as any).type = null
    setTimeout(() => ((props as any).type = tmp), 0)
  }
}
</script>

<style scoped>
@import '@/styles/teaching-ui.css';
.tm-input {
  width: 100%;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 13px;
  color: #374151;
  background: white;
}
.tm-input:focus { outline: none; border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.1); }
.tm-input:disabled { background: #f9fafb; color: #9ca3af; cursor: not-allowed; }
</style>
