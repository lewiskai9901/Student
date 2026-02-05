<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑组织' : '新建组织'"
    width="520px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
    @closed="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="80px"
      class="department-form"
    >
      <!-- 组织名称 -->
      <el-form-item label="组织名称" prop="unitName">
        <el-input
          v-model="formData.unitName"
          placeholder="请输入组织名称"
          maxlength="50"
          show-word-limit
        >
          <template #prefix>
            <Building2 class="h-4 w-4 text-gray-400" />
          </template>
        </el-input>
      </el-form-item>

      <!-- 组织编码 -->
      <el-form-item label="组织编码" :prop="isEdit ? '' : 'unitCode'">
        <el-input
          v-model="formData.unitCode"
          :placeholder="isEdit ? '' : '请输入组织编码'"
          :disabled="isEdit"
          maxlength="30"
        >
          <template #prefix>
            <Hash class="h-4 w-4 text-gray-400" />
          </template>
        </el-input>
        <div v-if="!isEdit" class="mt-1 text-xs text-gray-400">编码用于系统标识，建议使用字母、数字、下划线</div>
      </el-form-item>

      <!-- 组织类别 -->
      <el-form-item label="组织类别" prop="unitCategory">
        <el-radio-group v-model="formData.unitCategory">
          <el-radio value="ACADEMIC">教学单位</el-radio>
          <el-radio value="FUNCTIONAL">职能部门</el-radio>
          <el-radio value="ADMINISTRATIVE">行政单位</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 上级组织 -->
      <el-form-item label="上级组织" prop="parentId">
        <el-tree-select
          v-model="formData.parentId"
          :data="parentOptions"
          :props="treeProps"
          placeholder="请选择上级组织（留空为顶级组织）"
          clearable
          check-strictly
          :render-after-expand="false"
          style="width: 100%"
        >
          <template #default="{ data }">
            <div class="flex items-center gap-2">
              <Building2 class="h-4 w-4 text-gray-400" />
              <span>{{ data.unitName }}</span>
              <span class="text-xs text-gray-400">({{ data.unitCode }})</span>
            </div>
          </template>
        </el-tree-select>
      </el-form-item>

      <!-- 启用状态 -->
      <el-form-item label="启用状态">
        <el-switch
          v-model="formData.isEnabled"
          active-text="启用"
          inactive-text="禁用"
          inline-prompt
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="flex items-center justify-end gap-3">
        <el-button @click="$emit('update:visible', false)">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '创建组织' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Building2, Hash } from 'lucide-vue-next'
import { orgUnitApi, type DepartmentResponse } from '@/api/organization'

interface Props {
  visible: boolean
  department?: DepartmentResponse | null
  parentDepartment?: DepartmentResponse | null
  allDepartments: DepartmentResponse[]
}

const props = withDefaults(defineProps<Props>(), {
  department: null,
  parentDepartment: null,
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'success': []
}>()

const formRef = ref<FormInstance>()
const loading = ref(false)

// 表单数据
const formData = reactive({
  unitName: '',
  unitCode: '',
  unitCategory: 'ACADEMIC' as string,
  parentId: null as number | null,
  isEnabled: true,
})

// 是否编辑模式
const isEdit = computed(() => !!props.department)

// 树形选择器配置
const treeProps = {
  value: 'id',
  label: 'unitName',
  children: 'children',
}

// 构建父部门选项（排除当前编辑的部门及其子部门）
const parentOptions = computed(() => {
  if (!props.allDepartments.length) return []

  const filterDepartments = (items: DepartmentResponse[]): DepartmentResponse[] => {
    return items
      .filter(item => {
        // 编辑时排除自己及子部门
        if (isEdit.value && props.department) {
          return item.id !== props.department.id
        }
        return true
      })
      .map(item => ({
        ...item,
        children: item.children ? filterDepartments(item.children) : undefined,
      }))
  }

  return filterDepartments(props.allDepartments)
})

// 表单验证规则
const rules: FormRules = {
  unitName: [
    { required: true, message: '请输入组织名称', trigger: 'blur' },
    { min: 2, max: 50, message: '组织名称长度为2-50个字符', trigger: 'blur' },
  ],
  unitCode: [
    { required: true, message: '请输入组织编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '编码只能包含字母、数字、下划线', trigger: 'blur' },
  ],
}

// 监听对话框显示
watch(() => props.visible, (val) => {
  if (val) {
    if (props.department) {
      // 编辑模式：填充数据
      formData.unitName = props.department.unitName
      formData.unitCode = props.department.unitCode
      formData.unitCategory = props.department.unitCategory || 'ACADEMIC'
      formData.parentId = props.department.parentId || null
      formData.isEnabled = props.department.isEnabled !== false
    } else if (props.parentDepartment) {
      // 新建子部门：设置父部门
      formData.parentId = props.parentDepartment.id
    }
  }
})

// 重置表单
const handleClose = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    unitName: '',
    unitCode: '',
    unitCategory: 'ACADEMIC',
    parentId: null,
    isEnabled: true,
  })
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    if (isEdit.value && props.department) {
      // 更新组织
      await orgUnitApi.update(props.department.id, {
        unitName: formData.unitName,
        unitCategory: formData.unitCategory as any,
      })
      ElMessage.success('组织更新成功')
    } else {
      // 创建组织
      await orgUnitApi.create({
        unitName: formData.unitName,
        unitCode: formData.unitCode,
        unitCategory: formData.unitCategory as any,
        parentId: formData.parentId || undefined,
      })
      ElMessage.success('组织创建成功')
    }

    emit('update:visible', false)
    emit('success')
  } catch (error: any) {
    ElMessage.error(error.message || (isEdit.value ? '更新失败' : '创建失败'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.department-form :deep(.el-input__prefix) {
  margin-right: 8px;
}

.department-form :deep(.el-divider__text) {
  background-color: var(--el-dialog-bg-color);
}
</style>
