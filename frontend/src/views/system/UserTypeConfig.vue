<template>
  <div class="user-type-config">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户类型配置</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新增类型
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索类型名称或编码"
          clearable
          style="width: 300px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="filterCategory" placeholder="筛选分类" clearable style="width: 150px; margin-left: 10px">
          <el-option label="管理员" value="admin" />
          <el-option label="教职工" value="teacher" />
          <el-option label="学生" value="student" />
          <el-option label="外部人员" value="external" />
        </el-select>
      </div>

      <!-- 类型列表 -->
      <el-table
        :data="filteredUserTypes"
        style="width: 100%"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
        v-loading="loading"
      >
        <el-table-column prop="typeName" label="类型名称" min-width="180">
          <template #default="{ row }">
            <div class="type-name">
              <span
                class="type-icon"
                :style="{ backgroundColor: row.color || '#1890ff' }"
              >
                <component :is="getIconComponent(row.icon)" v-if="row.icon" />
              </span>
              <span>{{ row.typeName }}</span>
              <el-tag v-if="row.isSystem" size="small" type="info" style="margin-left: 8px">
                系统
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="typeCode" label="类型编码" width="140" />
        <el-table-column label="用户特性" width="320">
          <template #default="{ row }">
            <div class="feature-tags">
              <el-tag v-if="row.canLogin" size="small" type="primary">可登录</el-tag>
              <el-tag v-if="row.canBeInspector" size="small" type="success">检查员</el-tag>
              <el-tag v-if="row.canBeInspected" size="small" type="warning">可被检</el-tag>
              <el-tag v-if="row.canManageOrg" size="small" type="info">管理组织</el-tag>
              <el-tag v-if="row.canViewReports" size="small">查看报表</el-tag>
              <el-tag v-if="row.requiresClass" size="small" type="danger">需班级</el-tag>
              <el-tag v-if="row.requiresDormitory" size="small" type="danger">需宿舍</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="defaultRoleCodes" label="默认角色" width="120">
          <template #default="{ row }">
            <span v-if="row.defaultRoleCodes">{{ row.defaultRoleCodes }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.isEnabled"
              :disabled="row.isSystem"
              @change="handleToggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button
              link
              type="danger"
              @click="handleDelete(row)"
              :disabled="row.isSystem"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户类型' : '新增用户类型'"
      width="650px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="类型编码" prop="typeCode">
          <el-input
            v-model="formData.typeCode"
            placeholder="请输入类型编码（如 CLASS_TEACHER）"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model="formData.typeName" placeholder="请输入类型名称" />
        </el-form-item>
        <el-form-item label="父类型" prop="parentTypeCode">
          <el-tree-select
            v-model="formData.parentTypeCode"
            :data="parentTypeOptions"
            :props="{ label: 'typeName', value: 'typeCode', children: 'children' }"
            placeholder="请选择父类型（可选）"
            clearable
            check-strictly
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-select v-model="formData.icon" placeholder="请选择图标" clearable>
            <el-option v-for="icon in iconOptions" :key="icon" :label="icon" :value="icon">
              <div class="icon-option">
                <component :is="getIconComponent(icon)" />
                <span style="margin-left: 8px">{{ icon }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="颜色" prop="color">
          <el-color-picker v-model="formData.color" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="2"
            placeholder="请输入类型描述"
          />
        </el-form-item>

        <el-divider content-position="left">用户特性</el-divider>

        <el-form-item label="权限特性">
          <div class="feature-checkboxes">
            <el-checkbox v-model="formData.canLogin">可登录系统</el-checkbox>
            <el-checkbox v-model="formData.canBeInspector">可作为检查员</el-checkbox>
            <el-checkbox v-model="formData.canBeInspected">可被检查</el-checkbox>
            <el-checkbox v-model="formData.canManageOrg">可管理组织</el-checkbox>
            <el-checkbox v-model="formData.canViewReports">可查看报表</el-checkbox>
          </div>
        </el-form-item>

        <el-form-item label="归属要求">
          <div class="feature-checkboxes">
            <el-checkbox v-model="formData.requiresClass">需要班级归属</el-checkbox>
            <el-checkbox v-model="formData.requiresDormitory">需要宿舍归属</el-checkbox>
          </div>
        </el-form-item>

        <el-divider content-position="left">默认配置</el-divider>

        <el-form-item label="默认角色" prop="defaultRoleCodes">
          <el-input
            v-model="formData.defaultRoleCodes"
            placeholder="多个角色用逗号分隔，如: teacher,inspector"
          />
        </el-form-item>

        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, markRaw } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Search, User, UserFilled, Setting, Tools, Avatar,
  OfficeBuilding, Reading, Stamp, Service, View, House, Folder,
  Notebook, Document, Promotion, Suitcase
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { UserType, UserTypeTreeNode, CreateUserTypeRequest, UpdateUserTypeRequest } from '@/types/userType'
import { userTypeApi } from '@/api/userType'

// 图标组件映射
const iconComponents: Record<string, any> = {
  User: markRaw(User),
  UserFilled: markRaw(UserFilled),
  Setting: markRaw(Setting),
  Tools: markRaw(Tools),
  Avatar: markRaw(Avatar),
  OfficeBuilding: markRaw(OfficeBuilding),
  Reading: markRaw(Reading),
  Stamp: markRaw(Stamp),
  Service: markRaw(Service),
  View: markRaw(View),
  House: markRaw(House),
  Folder: markRaw(Folder),
  Notebook: markRaw(Notebook),
  Document: markRaw(Document),
  Promotion: markRaw(Promotion),
  Suitcase: markRaw(Suitcase)
}

const iconOptions = Object.keys(iconComponents)

const getIconComponent = (iconName: string | null) => {
  if (!iconName) return null
  return iconComponents[iconName] || null
}

// 状态
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const searchKeyword = ref('')
const filterCategory = ref('')

const userTypes = ref<UserType[]>([])
const userTypeTree = ref<UserTypeTreeNode[]>([])

const formRef = ref<FormInstance>()
const formData = ref<Partial<CreateUserTypeRequest>>({
  typeCode: '',
  typeName: '',
  parentTypeCode: undefined,
  icon: '',
  color: '#1890ff',
  description: '',
  canLogin: true,
  canBeInspector: false,
  canBeInspected: false,
  canManageOrg: false,
  canViewReports: false,
  requiresClass: false,
  requiresDormitory: false,
  defaultRoleCodes: '',
  sortOrder: 0
})

const formRules: FormRules = {
  typeCode: [
    { required: true, message: '请输入类型编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]*$/, message: '编码必须大写字母开头，只能包含大写字母、数字和下划线', trigger: 'blur' }
  ],
  typeName: [
    { required: true, message: '请输入类型名称', trigger: 'blur' }
  ]
}

// 计算属性
const filteredUserTypes = computed(() => {
  let result = userTypeTree.value

  // 关键词过滤
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = filterTreeByKeyword(result, keyword)
  }

  // 分类过滤
  if (filterCategory.value) {
    result = filterTreeByCategory(result, filterCategory.value)
  }

  return result
})

const parentTypeOptions = computed(() => {
  return userTypeTree.value
})

// 方法
function filterTreeByKeyword(tree: UserTypeTreeNode[], keyword: string): UserTypeTreeNode[] {
  return tree.filter(node => {
    const matchSelf = node.typeName.toLowerCase().includes(keyword) ||
                      node.typeCode.toLowerCase().includes(keyword)
    const matchChildren = node.children && node.children.length > 0
      ? filterTreeByKeyword(node.children, keyword).length > 0
      : false
    return matchSelf || matchChildren
  }).map(node => ({
    ...node,
    children: node.children ? filterTreeByKeyword(node.children, keyword) : []
  }))
}

function filterTreeByCategory(tree: UserTypeTreeNode[], category: string): UserTypeTreeNode[] {
  const categoryMap: Record<string, string> = {
    admin: 'ADMIN',
    teacher: 'TEACHER',
    student: 'STUDENT',
    external: 'EXTERNAL'
  }
  const rootCode = categoryMap[category]
  return tree.filter(node => node.typeCode === rootCode)
}

async function loadUserTypes() {
  loading.value = true
  try {
    const [allTypes, tree] = await Promise.all([
      userTypeApi.getAll(),
      userTypeApi.getTree()
    ])
    userTypes.value = allTypes
    userTypeTree.value = tree
  } catch (error) {
    ElMessage.error('加载用户类型失败')
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  isEdit.value = false
  formData.value = {
    typeCode: '',
    typeName: '',
    parentTypeCode: undefined,
    icon: '',
    color: '#1890ff',
    description: '',
    canLogin: true,
    canBeInspector: false,
    canBeInspected: false,
    canManageOrg: false,
    canViewReports: false,
    requiresClass: false,
    requiresDormitory: false,
    defaultRoleCodes: '',
    sortOrder: 0
  }
  dialogVisible.value = true
}

function handleEdit(row: UserType) {
  isEdit.value = true
  formData.value = {
    typeCode: row.typeCode,
    typeName: row.typeName,
    parentTypeCode: row.parentTypeCode || undefined,
    icon: row.icon || '',
    color: row.color || '#1890ff',
    description: row.description || '',
    canLogin: row.canLogin,
    canBeInspector: row.canBeInspector,
    canBeInspected: row.canBeInspected,
    canManageOrg: row.canManageOrg,
    canViewReports: row.canViewReports,
    requiresClass: row.requiresClass,
    requiresDormitory: row.requiresDormitory,
    defaultRoleCodes: row.defaultRoleCodes || '',
    sortOrder: row.sortOrder
  }
  // 保存当前编辑的ID
  ;(formData.value as any)._id = row.id
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      const id = (formData.value as any)._id
      await userTypeApi.update(id, formData.value as UpdateUserTypeRequest)
      ElMessage.success('更新成功')
    } else {
      await userTypeApi.create(formData.value as CreateUserTypeRequest)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadUserTypes()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: UserType) {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户类型"${row.typeName}"吗？`,
      '确认删除',
      { type: 'warning' }
    )
    await userTypeApi.delete(row.id)
    ElMessage.success('删除成功')
    await loadUserTypes()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function handleToggleStatus(row: UserType) {
  try {
    if (row.isEnabled) {
      await userTypeApi.enable(row.id)
      ElMessage.success('已启用')
    } else {
      await userTypeApi.disable(row.id)
      ElMessage.success('已禁用')
    }
  } catch (error: any) {
    // 恢复状态
    row.isEnabled = !row.isEnabled
    ElMessage.error(error.message || '操作失败')
  }
}

onMounted(() => {
  loadUserTypes()
})
</script>

<style scoped>
.user-type-config {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.type-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  color: white;
  font-size: 14px;
}

.feature-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.feature-checkboxes {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.text-muted {
  color: #999;
}

.icon-option {
  display: flex;
  align-items: center;
}
</style>
