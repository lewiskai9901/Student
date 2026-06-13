<template>
  <Teleport to="body">
    <Transition name="tm-drawer">
      <div v-if="visible" class="tm-drawer-overlay" @click.self="close">
        <div class="tm-drawer" style="width: 560px;">
          <div class="tm-drawer-header">
            <h2 class="tm-drawer-title">{{ dialogTitle }}</h2>
            <button class="tm-drawer-close" @click="close">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div class="tm-drawer-body">
            <!-- 基本信息 -->
            <div class="tm-section">
              <h3 class="tm-section-title">基本信息</h3>
              <div class="tm-field">
                <label class="tm-label">组织名称 <span class="req">*</span></label>
                <input
                  v-model="formData.unitName"
                  type="text"
                  maxlength="50"
                  placeholder="请输入组织名称"
                  class="tm-input"
                  @input="handleNameInput"
                />
              </div>
              <div class="tm-field">
                <label class="tm-label">组织类型 <span class="req">*</span></label>
                <div v-if="typesLoading" class="of-loading">加载类型中...</div>
                <div v-else-if="availableTypes.length > 0" class="of-chips">
                  <button
                    v-for="t in availableTypes"
                    :key="t.typeCode"
                    type="button"
                    class="of-chip"
                    :class="{ active: formData.unitType === t.typeCode }"
                    :disabled="isEdit"
                    @click="selectType(t)"
                  >
                    <Building2 class="h-3 w-3" />
                    {{ t.typeName }}
                  </button>
                </div>
                <p v-else class="of-empty">暂无可用类型</p>
                <p v-if="typeError" class="error-hint">{{ typeError }}</p>
              </div>
              <div class="tm-field">
                <label class="tm-label">组织编码</label>
                <div class="of-code-row">
                  <code class="of-code">{{ isEdit ? formData.unitCode : (formData.unitCode || '选择类型后自动生成') }}</code>
                  <span v-if="isEdit" class="of-code-note">编码不可修改</span>
                  <button
                    v-else-if="formData.unitType"
                    type="button"
                    class="of-code-regen"
                    title="重新生成编码"
                    @click="regenerateCode"
                  >
                    <RefreshCw class="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            </div>

            <!-- 层级关系 -->
            <div class="tm-section">
              <h3 class="tm-section-title">层级关系</h3>
              <div class="tm-field">
                <label class="tm-label">上级组织</label>
                <el-tree-select
                  v-model="formData.parentId"
                  :data="parentOptions"
                  :props="treeSelectProps"
                  placeholder="留空为顶级组织"
                  clearable
                  check-strictly
                  :render-after-expand="false"
                  style="width: 100%"
                >
                  <template #default="{ data }">
                    <div class="flex items-center gap-2">
                      <span>{{ data.unitName }}</span>
                      <span class="text-xs text-gray-400">({{ data.unitCode }})</span>
                    </div>
                  </template>
                </el-tree-select>
              </div>
            </div>

            <!-- 初始岗位 (仅新建且类型有模板时) -->
            <div v-if="!isEdit && positionTemplates.length > 0" class="tm-section">
              <h3 class="tm-section-title">初始岗位</h3>
              <p class="tm-section-hint">从模板中选择要创建的岗位，可设置编制人数</p>
              <div class="of-pos-list">
                <div
                  v-for="(pos, idx) in positionSelections"
                  :key="idx"
                  class="of-pos-row"
                  :class="{ active: pos.selected }"
                >
                  <input type="checkbox" v-model="pos.selected" class="row-checkbox" />
                  <span class="of-pos-name" :class="{ active: pos.selected }">{{ pos.positionName }}</span>
                  <div v-if="pos.selected" class="of-pos-hc">
                    <span class="of-pos-hc-label">编制</span>
                    <el-input-number
                      v-model="pos.headcount"
                      :min="1"
                      :max="99"
                      size="small"
                      controls-position="right"
                      style="width: 80px"
                    />
                  </div>
                </div>
              </div>
            </div>

            <!-- 扩展属性 (插件 metadata_schema) -->
            <div v-if="typeSchema && typeSchema.fields?.length > 0" class="tm-section">
              <h3 class="tm-section-title">扩展属性</h3>
              <DynamicForm :schema="typeSchema" v-model="formData.attributes" />
            </div>
          </div>
          <div class="tm-drawer-footer">
            <button class="tm-btn tm-btn-secondary" @click="close">取消</button>
            <button class="tm-btn tm-btn-primary" :disabled="submitting" @click="handleSubmit">
              {{ isEdit ? '保存修改' : '创建组织' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Building2, RefreshCw } from 'lucide-vue-next'
import DynamicForm from '@/components/extension/DynamicForm.vue'
import { entityTypeApi } from '@/api/entityType'
import {
  orgUnitApi,
  type DepartmentResponse,
  type OrgUnitTypeConfig
} from '@/api/organization'

interface Props {
  visible: boolean
  department?: DepartmentResponse | null
  parentDepartment?: DepartmentResponse | null
  allDepartments: DepartmentResponse[]
}

const props = withDefaults(defineProps<Props>(), {
  department: null,
  parentDepartment: null
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: [createdId?: LongId]
}>()

const submitting = ref(false)
const typesLoading = ref(false)
const availableTypes = ref<OrgUnitTypeConfig[]>([])
const typeError = ref('')

const formData = reactive({
  unitName: '',
  unitCode: '',
  unitType: '',
  parentId: null as LongId | null,
  sortOrder: 0,
  attributes: {} as Record<string, any>,
})

// Dynamic schema from entity_type_configs (loaded when type is selected)
const typeSchema = ref<{ fields: any[] } | null>(null)

// Position selection state
interface PositionSelection {
  positionName: string
  selected: boolean
  headcount: number
}
const positionSelections = ref<PositionSelection[]>([])

const isEdit = computed(() => !!props.department)

// Get position templates from the currently selected type
const positionTemplates = computed(() => {
  if (!formData.unitType) return []
  const selectedTypeConfig = availableTypes.value.find(t => t.typeCode === formData.unitType)
  return selectedTypeConfig?.defaultPositions || []
})

const dialogTitle = computed(() => {
  if (isEdit.value) return '编辑组织'
  // 显示选中的类型名，如 "新建年级 — 经济与信息技术系"
  const selectedType = availableTypes.value.find(t => t.typeCode === formData.unitType)
  const typeName = selectedType?.typeName || '子组织'
  if (props.parentDepartment) return `新建${typeName} — ${props.parentDepartment.unitName}`
  return '新建组织'
})

const treeSelectProps = {
  value: 'id',
  label: 'unitName',
  children: 'children'
}

// --- Code generation ---
const CODE_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'

function generateCode(typeCode: string): string {
  let suffix = ''
  for (let i = 0; i < 6; i++) {
    suffix += CODE_CHARS[Math.floor(Math.random() * CODE_CHARS.length)]
  }
  return `${typeCode}_${suffix}`
}

function regenerateCode() {
  if (formData.unitType) {
    formData.unitCode = generateCode(formData.unitType)
  }
}

function selectType(t: OrgUnitTypeConfig) {
  if (isEdit.value) return
  formData.unitType = t.typeCode
  typeError.value = ''
  formData.unitCode = generateCode(t.typeCode)
  buildPositionSelections(t)
  // Load dynamic schema from entity_type_configs
  loadTypeSchema(t.typeCode)
}

async function loadTypeSchema(typeCode: string) {
  typeSchema.value = null
  formData.attributes = {}
  try {
    const res = await entityTypeApi.get('ORG_UNIT', typeCode)
    const data = (res as any).data || res
    if (data?.metadataSchema) {
      const schema = typeof data.metadataSchema === 'string' ? JSON.parse(data.metadataSchema) : data.metadataSchema
      if (schema?.fields?.length > 0) {
        typeSchema.value = schema
        // Apply default values
        for (const f of schema.fields) {
          if (f.defaultValue !== undefined) formData.attributes[f.key] = f.defaultValue
          if (f.config?.default !== undefined) formData.attributes[f.key] = f.config.default
        }
      }
    }
  } catch { /* Plugin not registered for this type, no dynamic fields */ }
}

function buildPositionSelections(typeConfig: OrgUnitTypeConfig) {
  const templates = typeConfig.defaultPositions || []
  positionSelections.value = templates.map(tpl => ({
    positionName: tpl.positionName,
    selected: true,  // All selected by default
    headcount: 1
  }))
}

function handleNameInput() {
  // Clear type error when user starts filling the form
  if (typeError.value && formData.unitType) {
    typeError.value = ''
  }
}

// --- Parent options ---
const parentOptions = computed(() => {
  if (!props.allDepartments.length) return []

  const excludeIds = new Set<LongId>()
  if (isEdit.value && props.department) {
    const collectIds = (node: DepartmentResponse) => {
      excludeIds.add(node.id)
      node.children?.forEach(collectIds)
    }
    collectIds(props.department)
  }

  const filterDepts = (items: DepartmentResponse[]): DepartmentResponse[] => {
    return items
      .filter(item => !excludeIds.has(item.id))
      .map(item => ({
        ...item,
        children: item.children ? filterDepts(item.children) : undefined
      }))
  }

  return filterDepts(props.allDepartments)
})

// --- Load types (from entity_type_configs, single source of truth) ---
const loadTypes = async () => {
  typesLoading.value = true
  try {
    if (props.department) {
      // Edit mode: show only the current type (locked)
      const typeCode = props.department.unitType
      const typeName = props.department.typeName || typeCode
      availableTypes.value = [{ id: '', typeCode, typeName, category: '', parentTypeCode: '', defaultPositions: [] } as any]
    } else if (props.parentDepartment) {
      // Add child: get allowed children from parent type
      const parentTypeCode = props.parentDepartment.unitType
      const res = await entityTypeApi.getAllowedChildren('ORG_UNIT', parentTypeCode)
      const data = (res as any).data || res || []
      availableTypes.value = data.map((t: any) => ({
        typeCode: t.typeCode, typeName: t.typeName, category: t.category,
        parentTypeCode: t.parentTypeCode, defaultPositions: [],
      }))
    } else {
      // Root creation: show top-level types (no parentTypeCode)
      const res = await entityTypeApi.list('ORG_UNIT')
      const data = (res as any).data || res || []
      availableTypes.value = data
        .filter((t: any) => !t.parentTypeCode)
        .map((t: any) => ({
          typeCode: t.typeCode, typeName: t.typeName, category: t.category,
          parentTypeCode: t.parentTypeCode, defaultPositions: [],
        }))
    }
    // Auto-select if only one type available
    if (!isEdit.value && availableTypes.value.length === 1) {
      selectType(availableTypes.value[0])
    }
  } catch {
    ElMessage.error('加载组织类型失败')
    availableTypes.value = []
  } finally {
    typesLoading.value = false
  }
}

// --- Watch dialog ---
watch(() => props.visible, async (val) => {
  if (val) {
    await loadTypes()

    if (props.department) {
      // Edit mode
      formData.unitName = props.department.unitName
      formData.unitCode = props.department.unitCode
      formData.unitType = props.department.unitType
      formData.parentId = props.department.parentId || null
      formData.sortOrder = props.department.sortOrder ?? 0
      // Load extension schema and populate existing attributes
      if (formData.unitType) {
        await loadTypeSchema(formData.unitType)
        if (props.department.attributes) {
          formData.attributes = { ...formData.attributes, ...props.department.attributes }
        }
      }
    } else if (props.parentDepartment) {
      // Add child mode
      formData.parentId = props.parentDepartment.id
    }
  } else {
    // 关闭时重置 (tm-drawer 无 el-dialog 的 @closed 钩子, 改由 visible→false 触发)
    handleClosed()
  }
})

/** 关闭抽屉 (取消 / 遮罩点击 / 关闭按钮) */
const close = () => emit('update:visible', false)

// --- Reset ---
const handleClosed = () => {
  Object.assign(formData, {
    unitName: '',
    unitCode: '',
    unitType: '',
    parentId: null,
    sortOrder: 0,
    attributes: {},
  })
  typeSchema.value = null
  availableTypes.value = []
  typeError.value = ''
  positionSelections.value = []
}

// --- Submit ---
const handleSubmit = async () => {
  // Validate
  if (!formData.unitName.trim()) {
    ElMessage.error('请输入组织名称')
    return
  }
  if (formData.unitName.trim().length < 2) {
    ElMessage.error('组织名称至少2个字符')
    return
  }
  if (!isEdit.value) {
    if (!formData.unitType) {
      typeError.value = '请选择组织类型'
      return
    }
    if (!formData.unitCode) {
      ElMessage.error('编码生成异常，请重试')
      return
    }
  }

  submitting.value = true
  try {
    if (isEdit.value && props.department) {
      await orgUnitApi.update(props.department.id, {
        unitName: formData.unitName,
        sortOrder: formData.sortOrder,
        attributes: Object.keys(formData.attributes).length > 0 ? formData.attributes : undefined,
      })
      ElMessage.success('组织更新成功')
    } else {
      // Collect selected positions
      const selectedPositions = positionSelections.value
        .filter(p => p.selected && p.positionName.trim())
        .map(p => ({ positionName: p.positionName, headcount: p.headcount }))

      const created = await orgUnitApi.create({
        unitName: formData.unitName,
        unitCode: formData.unitCode,
        unitType: formData.unitType,
        parentId: formData.parentId || undefined,
        selectedPositions: selectedPositions.length > 0 ? selectedPositions : undefined,
        attributes: Object.keys(formData.attributes).length > 0 ? formData.attributes : undefined,
      })
      ElMessage.success('组织创建成功')
      emit('update:visible', false)
      emit('success', created?.id ?? undefined)
      return
    }

    emit('update:visible', false)
    emit('success')
  } catch (error: any) {
    ElMessage.error(error.message || (isEdit.value ? '更新失败' : '创建失败'))
  } finally {
    submitting.value = false
  }
}
</script>

<style>
@import '@/styles/teaching-ui.css';
</style>

<style scoped>
.error-hint { margin: 3px 0 0; font-size: 11px; color: #ef4444; }
.tm-section-hint { margin: 0 0 8px; font-size: 11px; color: #9ca3af; }
.of-loading, .of-empty { font-size: 13px; color: #9ca3af; }

/* 类型选择 chips */
.of-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.of-chip {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 12px; border-radius: 7px; font-size: 13px;
  border: 1px solid #e5e7eb; background: #fff; color: #4b5563;
  cursor: pointer; transition: all 0.15s;
}
.of-chip:hover:not(:disabled) { border-color: #d1d5db; background: #f9fafb; }
.of-chip.active { border-color: #93c5fd; background: #eff6ff; color: #1d4ed8; box-shadow: 0 0 0 1px #bfdbfe; }
.of-chip:disabled { cursor: not-allowed; opacity: 0.6; }

/* 编码行 */
.of-code-row { display: flex; align-items: center; gap: 8px; }
.of-code {
  font-family: 'JetBrains Mono', Menlo, monospace; font-size: 13px;
  color: #6b7280; background: #f3f4f6; padding: 6px 10px; border-radius: 6px;
}
.of-code-note { font-size: 11px; color: #9ca3af; }
.of-code-regen {
  display: inline-flex; padding: 4px; border-radius: 5px;
  color: #9ca3af; cursor: pointer; transition: all 0.15s;
}
.of-code-regen:hover { background: #f3f4f6; color: #4b5563; }

/* 岗位列表 */
.of-pos-list { display: flex; flex-direction: column; gap: 8px; }
.of-pos-row {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 12px; border-radius: 7px; border: 1px solid #e5e7eb;
  background: #fff; transition: all 0.15s;
}
.of-pos-row.active { border-color: #bfdbfe; background: #eff6ff; }
.of-pos-name { flex: 1; font-size: 13px; color: #6b7280; }
.of-pos-name.active { color: #1f2937; }
.of-pos-hc { display: flex; align-items: center; gap: 6px; }
.of-pos-hc-label { font-size: 11px; color: #9ca3af; }
.row-checkbox { width: 15px; height: 15px; border-radius: 3px; cursor: pointer; accent-color: #2563eb; }
</style>
