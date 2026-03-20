<template>
  <div class="p-4">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-lg font-medium">租户管理</h2>
      <el-button type="primary" size="small" @click="showCreateDialog">
        新建租户
      </el-button>
    </div>

    <!-- Table -->
    <el-table :data="tenants" v-loading="loading" border size="small" class="w-full">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="tenantCode" label="租户编码" width="150" />
      <el-table-column prop="tenantName" label="租户名称" min-width="200" />
      <el-table-column prop="domain" label="绑定域名" min-width="180" />
      <el-table-column prop="enabled" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
            {{ row.enabled ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-popconfirm title="确认删除此租户？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button type="danger" link size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑租户' : '新建租户'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="租户编码" prop="tenantCode">
          <el-input v-model="form.tenantCode" :disabled="isEditing" placeholder="唯一标识" />
        </el-form-item>
        <el-form-item label="租户名称" prop="tenantName">
          <el-input v-model="form.tenantName" placeholder="租户显示名称" />
        </el-form-item>
        <el-form-item label="绑定域名" prop="domain">
          <el-input v-model="form.domain" placeholder="可选，如 school.example.com" />
        </el-form-item>
        <el-form-item v-if="isEditing" label="状态">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { listTenants, createTenant, updateTenant, deleteTenant } from '@/api/tenant'
import type { Tenant } from '@/types/tenant'

const loading = ref(false)
const saving = ref(false)
const tenants = ref<Tenant[]>([])
const dialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = ref({
  tenantCode: '',
  tenantName: '',
  domain: '',
  enabled: true
})

const rules: FormRules = {
  tenantCode: [{ required: true, message: '请输入租户编码', trigger: 'blur' }],
  tenantName: [{ required: true, message: '请输入租户名称', trigger: 'blur' }]
}

async function loadTenants() {
  loading.value = true
  try {
    tenants.value = await listTenants()
  } catch (e) {
    ElMessage.error('加载租户列表失败')
  } finally {
    loading.value = false
  }
}

function showCreateDialog() {
  isEditing.value = false
  editingId.value = null
  form.value = { tenantCode: '', tenantName: '', domain: '', enabled: true }
  dialogVisible.value = true
}

function showEditDialog(tenant: Tenant) {
  isEditing.value = true
  editingId.value = tenant.id
  form.value = {
    tenantCode: tenant.tenantCode,
    tenantName: tenant.tenantName,
    domain: tenant.domain || '',
    enabled: tenant.enabled
  }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEditing.value && editingId.value) {
      await updateTenant(editingId.value, {
        tenantName: form.value.tenantName,
        domain: form.value.domain || undefined,
        enabled: form.value.enabled
      })
      ElMessage.success('更新成功')
    } else {
      await createTenant({
        tenantCode: form.value.tenantCode,
        tenantName: form.value.tenantName,
        domain: form.value.domain || undefined
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadTenants()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await deleteTenant(id)
    ElMessage.success('删除成功')
    await loadTenants()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onMounted(loadTenants)
</script>
