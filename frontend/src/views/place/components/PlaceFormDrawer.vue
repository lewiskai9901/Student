<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="handleClose"
    :title="dialogTitle"
    :width="dialogWidth"
    :close-on-click-modal="false"
    class="pf-dlg"
    destroy-on-close
    align-center
  >
    <!-- Type Selection Stage -->
    <div v-if="mode === 'create' && !selectedTypeCode" class="px-5 py-4">
      <div class="grid gap-2" :class="allowedTypes && allowedTypes.length > 3 ? 'grid-cols-3' : 'grid-cols-' + (allowedTypes?.length || 1)">
        <button v-for="type in allowedTypes" :key="type.typeCode" class="pf-type-card" @click="selectType(type)">
          <span class="font-medium text-gray-800 text-[13px]">{{ type.typeName }}</span>
          <span class="text-[10px] text-gray-400">
            <template v-if="type.features?.hasCapacity">容量</template>
            <template v-if="type.features?.bookable"> 预订</template>
            <template v-if="type.features?.occupiable"> 入住</template>
          </span>
        </button>
      </div>
    </div>

    <!-- Form -->
    <div v-else class="px-5 py-3">
      <!-- Context bar -->
      <div class="mb-3 flex items-center gap-2 text-xs text-gray-400">
        <span class="rounded bg-gray-100 px-1.5 py-0.5 text-[11px] font-medium text-gray-600">{{ selectedType?.typeName }}</span>
        <template v-if="mode === 'create' && parentPlace">
          <span>上级: {{ parentPlace.placeName }}</span>
        </template>
        <template v-if="mode === 'edit' && editData">
          <span>{{ getStatusLabel(editData.status) }}</span>
          <span v-if="editData.parentName">上级: {{ editData.parentName }}</span>
        </template>
        <button v-if="mode === 'create' && allowedTypes && allowedTypes.length > 1" class="ml-auto text-blue-500 hover:text-blue-600" @click="selectedTypeCode = ''">换类型</button>
      </div>

      <el-form ref="formRef" :model="formData" :rules="formRules" label-position="left" label-width="72px" class="pf-form">
        <!-- Row 1: Code + Name -->
        <div class="pf-row">
          <el-form-item label="编号" prop="placeCode" class="pf-col">
            <el-input v-model="formData.placeCode" placeholder="如 A栋" maxlength="50" />
          </el-form-item>
          <el-form-item label="名称" prop="placeName" class="pf-col">
            <el-input v-model="formData.placeName" placeholder="请输入名称" maxlength="50" />
          </el-form-item>
        </div>
        <!-- Row 2: Status + Gender -->
        <div class="pf-row">
          <el-form-item label="状态" prop="status" class="pf-col">
            <el-select v-model="formData.status" style="width: 100%">
              <el-option label="正常" :value="1" />
              <el-option label="维护中" :value="2" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="showGenderSection" label="性别限制" class="pf-col">
            <el-select v-model="formData.gender" placeholder="不设置" clearable style="width: 100%">
              <el-option v-for="opt in genderOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
        </div>
        <!-- Row 3: Capacity (conditional) -->
        <div v-if="showCapacitySection" class="pf-row">
          <el-form-item class="pf-col">
            <template #label>容量<span v-if="capacityUnit" class="text-[11px] text-gray-400 font-normal ml-0.5">({{ capacityUnit }})</span></template>
            <el-input-number v-model="formData.capacity" :min="0" :max="99999" controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>
        <!-- Row 4: Description -->
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="1" placeholder="选填" maxlength="500" />
        </el-form-item>

        <!-- Extended Attributes -->
        <template v-if="sortedAttributeFields.length > 0">
          <div class="pf-sep" />
          <div class="pf-section">{{ selectedType?.typeName }}属性</div>
          <div class="pf-row">
            <template v-for="field in sortedAttributeFields" :key="field.key">
              <div v-if="field.type === 'textarea'" class="pf-full">
                <label class="pf-attr-lbl">{{ field.label }}<span v-if="field.required" class="text-red-500 ml-0.5">*</span></label>
                <el-input v-model="attributeValues[field.key]" type="textarea" :rows="1" :placeholder="field.placeholder || ''" :maxlength="field.maxLength" />
              </div>
              <div v-else class="pf-col pf-attr-row">
                <label class="pf-attr-lbl">{{ field.label }}<span v-if="field.required" class="text-red-500 ml-0.5">*</span></label>
                <div class="flex-1 min-w-0">
                  <el-input v-if="field.type === 'string'" v-model="attributeValues[field.key]" :placeholder="field.placeholder || ''" :maxlength="field.maxLength" />
                  <el-input-number v-else-if="field.type === 'number'" v-model="attributeValues[field.key]" :min="field.min" :max="field.max" :step="field.step || 1" :precision="field.precision" controls-position="right" style="width: 100%" />
                  <el-select v-else-if="field.type === 'select'" v-model="attributeValues[field.key]" :placeholder="field.placeholder || '请选择'" :multiple="field.multiple" clearable style="width: 100%">
                    <el-option v-for="opt in field.options" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                  <el-switch v-else-if="field.type === 'boolean'" v-model="attributeValues[field.key]" />
                  <el-date-picker v-else-if="field.type === 'date'" v-model="attributeValues[field.key]" type="date" :placeholder="field.placeholder || '选择日期'" :format="field.format || 'YYYY-MM-DD'" :value-format="field.format || 'YYYY-MM-DD'" style="width: 100%" />
                </div>
              </div>
            </template>
          </div>
        </template>

        <!-- Relations -->
        <div class="pf-sep" />
        <div class="pf-section text-gray-400">关联（选填）</div>
        <div class="pf-row">
          <el-form-item label="所属组织" class="pf-col">
            <el-tree-select
              v-model="formData.orgUnitId"
              :data="orgOptions"
              placeholder="选择组织"
              clearable
              check-strictly
              :render-after-expand="false"
              :disabled="formData.clearOrgOverride"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="负责人" class="pf-col">
            <el-select v-model="formData.responsibleUserId" placeholder="选择负责人" clearable filterable style="width: 100%">
              <el-option v-for="user in userOptions" :key="user.id" :label="user.realName" :value="user.id" />
            </el-select>
          </el-form-item>
        </div>
        <!-- Org inheritance hint -->
        <div v-if="mode === 'edit' && editData?.isOrgInherited" class="mb-2 text-[11px] text-gray-400 pl-[72px]">
          ↑ 当前从父级继承: {{ editData.parentOrgUnitName || '未设置' }}
        </div>
        <!-- Clear override -->
        <div v-if="mode === 'edit' && editData?.orgUnitId" class="mb-2 pl-[72px]">
          <el-checkbox v-model="formData.clearOrgOverride" size="small">
            <span class="text-xs text-gray-500">清除组织覆盖，从父级继承</span>
          </el-checkbox>
        </div>
        <!-- Audit reason (edit only) -->
        <el-form-item v-if="mode === 'edit'" label="变更原因">
          <el-input v-model="formData.reason" placeholder="选填，用于审计" maxlength="200" />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="mode === 'create' && !selectedTypeCode" @click="handleSubmit">
          {{ mode === 'create' ? '创建' : '保存' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, markRaw, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  ArrowRight, School, OfficeBuilding, House, Menu, Location, Grid, Box
} from '@element-plus/icons-vue'
import { universalPlaceApi } from '@/api/universalPlace'
import type { PlaceTreeNode, UniversalPlaceType, CreatePlaceRequest, UpdatePlaceRequest, AttributeFieldDefinition } from '@/types/universalPlace'
import { orgUnitApi } from '@/api/organization'
import { userApi } from '@/api/user'

const props = defineProps<{
  visible: boolean
  mode: 'create' | 'edit'
  parentPlace?: PlaceTreeNode | null
  editData?: PlaceTreeNode | null
  allowedTypes?: UniversalPlaceType[]
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'success': []
}>()

const formRef = ref<FormInstance>()
const selectedTypeCode = ref('')
const submitting = ref(false)
const formData = ref({
  placeCode: '', placeName: '', description: '',
  status: 1 as number,
  capacity: undefined as number | undefined,
  gender: undefined as string | undefined,
  orgUnitId: undefined as number | undefined,
  responsibleUserId: undefined as number | undefined,
  clearOrgOverride: false as boolean,
  reason: '' as string
})
const attributeFields = ref<AttributeFieldDefinition[]>([])
const attributeValues = ref<Record<string, any>>({})

const formRules: FormRules = {
  placeCode: [{ required: true, message: '请输入编号', trigger: 'blur' }, { max: 50, message: '不超过50字', trigger: 'blur' }],
  placeName: [{ required: true, message: '请输入名称', trigger: 'blur' }, { min: 2, max: 50, message: '2-50字', trigger: 'blur' }]
}

const genderOptions = computed(() => {
  const pg = props.parentPlace?.effectiveGender
  const all = [{ value: '', label: '不设置(继承)' }, { value: 'MALE', label: '男' }, { value: 'FEMALE', label: '女' }, { value: 'MIXED', label: '混合' }]
  if (!pg || pg === 'MIXED') return all
  return all.filter(o => o.value === '' || o.value === pg)
})

const orgOptions = ref<any[]>([])
const userOptions = ref<any[]>([])

const mode = computed(() => props.mode)
const dialogTitle = computed(() => props.mode === 'create' && !selectedTypeCode.value ? '选择场所类型' : props.mode === 'create' ? '新建场所' : '编辑场所')
const dialogWidth = computed(() => props.mode === 'create' && !selectedTypeCode.value ? '420px' : '640px')

const selectedType = computed(() => {
  if (props.mode === 'edit' && props.editData) {
    return { typeCode: props.editData.typeCode, typeName: props.editData.typeName || '', icon: props.editData.typeIcon, features: { hasCapacity: !!props.editData.hasCapacity, bookable: !!props.editData.bookable, assignable: !!props.editData.assignable, occupiable: !!props.editData.occupiable }, capacityUnit: props.editData.capacityUnit } as unknown as UniversalPlaceType
  }
  return props.allowedTypes?.find(t => t.typeCode === selectedTypeCode.value)
})

const showCapacitySection = computed(() => selectedType.value?.features?.hasCapacity ?? false)
const showGenderSection = computed(() => !!selectedType.value)
const capacityUnit = computed(() => selectedType.value?.capacityUnit || '')
const sortedAttributeFields = computed(() => [...attributeFields.value].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0)))

function getStatusLabel(s?: number) { return s === 0 ? '停用' : s === 2 ? '维护中' : '正常' }

function selectType(type: UniversalPlaceType) { selectedTypeCode.value = type.typeCode; loadAttributeFields(type) }

function parseMetadataSchema(schemaStr?: string): AttributeFieldDefinition[] {
  if (!schemaStr) return []
  try {
    const parsed = JSON.parse(schemaStr)
    return parsed?.fields || []
  } catch { return [] }
}

function loadAttributeFields(type: UniversalPlaceType | undefined) {
  if (!type) { attributeFields.value = []; attributeValues.value = {}; return }
  const full = props.allowedTypes?.find(t => t.typeCode === type.typeCode) || type
  const fields = parseMetadataSchema(full.metadataSchema)
  if (fields.length > 0) {
    attributeFields.value = fields
    const v: Record<string, any> = {}
    for (const f of fields) { if (f.defaultValue != null) v[f.key] = f.defaultValue; else if (f.type === 'boolean') v[f.key] = false }
    attributeValues.value = v
  } else { attributeFields.value = []; attributeValues.value = {} }
}

function loadAttributeFieldsForEdit() {
  if (!props.editData) return
  const full = props.allowedTypes?.find(t => t.typeCode === props.editData!.typeCode)
  attributeFields.value = parseMetadataSchema(full?.metadataSchema)
  if (props.editData.attributes) attributeValues.value = { ...props.editData.attributes }
}

async function handleSubmit() {
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    const attrs: Record<string, any> = {}
    for (const [k, v] of Object.entries(attributeValues.value)) { if (v !== undefined && v !== null && v !== '') attrs[k] = v }
    const hasAttrs = Object.keys(attrs).length > 0
    if (props.mode === 'create') {
      await universalPlaceApi.create({ placeCode: formData.value.placeCode, placeName: formData.value.placeName, typeCode: selectedTypeCode.value, description: formData.value.description || undefined, parentId: props.parentPlace?.id, status: formData.value.status, capacity: formData.value.capacity, gender: formData.value.gender || undefined, orgUnitId: formData.value.orgUnitId, responsibleUserId: formData.value.responsibleUserId, attributes: hasAttrs ? attrs : undefined })
      ElMessage.success('创建成功')
    } else {
      await universalPlaceApi.update(props.editData!.id, {
        placeCode: formData.value.placeCode,
        placeName: formData.value.placeName,
        description: formData.value.description || undefined,
        status: formData.value.status,
        capacity: formData.value.capacity,
        gender: formData.value.gender || undefined,
        orgUnitId: formData.value.orgUnitId,
        responsibleUserId: formData.value.responsibleUserId,
        clearOrgOverride: formData.value.clearOrgOverride,
        reason: formData.value.reason || undefined,
        attributes: hasAttrs ? attrs : undefined
      })
      ElMessage.success('更新成功')
    }
    emit('success'); handleClose()
  } catch { /* 错误已由 axios 拦截器统一处理 */ }
  finally { submitting.value = false }
}

function handleClose() { emit('update:visible', false) }
function resetForm() {
  selectedTypeCode.value = ''
  formData.value = {
    placeCode: '', placeName: '', description: '',
    status: 1,
    capacity: undefined,
    gender: undefined,
    orgUnitId: undefined,
    responsibleUserId: undefined,
    clearOrgOverride: false,
    reason: ''
  }
  attributeFields.value = []; attributeValues.value = {}
  formRef.value?.resetFields()
}

async function loadOptions() {
  try { const d = await orgUnitApi.getTree(); orgOptions.value = transformOrgTree(d) } catch {}
  try { userOptions.value = (await userApi.getSimpleList()) || [] } catch {}
}
function transformOrgTree(nodes: any[]): any[] {
  return nodes.map(n => ({ value: n.id, label: n.unitName || n.name, children: n.children ? transformOrgTree(n.children) : undefined }))
}

watch(() => props.visible, (val) => {
  if (val) {
    resetForm(); loadOptions()
    if (props.mode === 'edit' && props.editData) {
      nextTick(() => {
        formData.value = {
          placeCode: props.editData!.placeCode || '',
          placeName: props.editData!.placeName,
          description: props.editData!.description || '',
          status: props.editData!.status ?? 1,
          capacity: props.editData!.capacity,
          gender: props.editData!.gender || undefined,
          orgUnitId: props.editData!.orgUnitId,
          responsibleUserId: props.editData!.responsibleUserId,
          clearOrgOverride: false,
          reason: ''
        }
        loadAttributeFieldsForEdit()
      })
    } else if (props.mode === 'create' && props.allowedTypes?.length === 1) {
      selectedTypeCode.value = props.allowedTypes[0].typeCode; loadAttributeFields(props.allowedTypes[0])
    }
  }
})
watch(() => props.allowedTypes, (t) => {
  if (t && t.length > 0 && props.visible && props.mode === 'edit' && props.editData) loadAttributeFieldsForEdit()
}, { deep: true })
</script>

<style scoped>
/* Dialog */
.pf-dlg :deep(.el-dialog) { border-radius: 10px; }
.pf-dlg :deep(.el-dialog__header) { padding: 12px 20px 0; }
.pf-dlg :deep(.el-dialog__title) { font-size: 14px; font-weight: 600; color: #1f2937; }
.pf-dlg :deep(.el-dialog__body) { padding: 0; }
.pf-dlg :deep(.el-dialog__footer) { padding: 10px 20px; border-top: 1px solid #f3f4f6; }

/* Type card */
.pf-type-card {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  padding: 12px 8px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  cursor: pointer; background: #fff; transition: all 0.15s;
}
.pf-type-card:hover { border-color: #3b82f6; background: #eff6ff; }

/* Form layout */
.pf-form :deep(.el-form-item) { margin-bottom: 10px; }
.pf-form :deep(.el-form-item__label) { font-size: 12px; color: #6b7280; font-weight: 500; line-height: 30px; }
.pf-form :deep(.el-input__wrapper),
.pf-form :deep(.el-textarea__inner),
.pf-form :deep(.el-select__wrapper) { border-radius: 6px; }
.pf-form :deep(.el-input),
.pf-form :deep(.el-select),
.pf-form :deep(.el-input-number) { --el-component-size: 30px; }
.pf-form :deep(.el-textarea__inner) { min-height: 30px !important; }

.pf-row { display: grid; grid-template-columns: 1fr 1fr; gap: 0 12px; }
.pf-col { min-width: 0; }
.pf-full { grid-column: 1 / -1; margin-bottom: 10px; }
.pf-sep { height: 1px; background: #f3f4f6; margin: 4px 0 8px; }
.pf-section { font-size: 11px; font-weight: 600; color: #6b7280; margin-bottom: 8px; }

/* Attribute inline */
.pf-attr-row { display: flex; align-items: center; gap: 0; margin-bottom: 10px; }
.pf-attr-lbl { font-size: 12px; font-weight: 500; color: #6b7280; white-space: nowrap; width: 72px; flex-shrink: 0; padding-right: 8px; }
</style>
