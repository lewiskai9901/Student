<template>
  <div class="flex h-full flex-col overflow-y-auto p-6">
    <div class="mx-auto w-full max-w-2xl">
      <!-- 插件禁用 banner -->
      <div
        v-if="role.pluginEnabled === false"
        class="mb-4 flex items-center gap-2 rounded-lg border border-orange-200 bg-orange-50 px-4 py-2.5 text-sm text-orange-700"
      >
        <AlertTriangle class="h-4 w-4 flex-shrink-0" />
        该角色所属插件已禁用，配置只读。请先在插件平台启用对应行业插件。
      </div>

      <div class="rounded-xl border border-gray-200 bg-white p-6">
        <h3 class="mb-5 flex items-center gap-2 text-base font-semibold text-gray-900">
          <Shield class="h-5 w-5 text-blue-600" />
          基本信息
          <span
            v-if="role.isSystem"
            class="rounded bg-purple-100 px-1.5 py-0.5 text-xs font-medium text-purple-600"
          >系统内置</span>
        </h3>

        <div class="space-y-4">
          <div>
            <label class="mb-1 block text-sm text-gray-600">角色编码</label>
            <input
              :value="role.roleCode"
              disabled
              class="h-9 w-full rounded-lg border border-gray-200 bg-gray-50 px-3 font-mono text-sm text-gray-500"
            />
            <p class="mt-1 text-xs text-gray-400">编码创建后不可修改</p>
          </div>
          <div>
            <label class="mb-1 block text-sm text-gray-600">角色名称 <span class="text-red-500">*</span></label>
            <input
              v-model="form.roleName"
              :disabled="readonly"
              type="text"
              placeholder="请输入角色名称"
              class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none disabled:bg-gray-50"
            />
          </div>
          <div>
            <label class="mb-1 block text-sm text-gray-600">描述</label>
            <textarea
              v-model="form.description"
              :disabled="readonly"
              rows="3"
              placeholder="角色用途说明"
              class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none disabled:bg-gray-50"
            />
          </div>
          <div class="flex items-center gap-6">
            <div class="w-32">
              <label class="mb-1 block text-sm text-gray-600">排序</label>
              <input
                v-model.number="form.sortOrder"
                :disabled="readonly"
                type="number"
                class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none disabled:bg-gray-50"
              />
            </div>
            <label class="mt-5 flex cursor-pointer items-center gap-2 text-sm text-gray-700">
              <input type="checkbox" v-model="form.enabled" :disabled="readonly" class="h-4 w-4 rounded border-gray-300" />
              启用该角色
            </label>
          </div>
        </div>

        <div class="mt-6 flex items-center justify-between border-t border-gray-100 pt-5">
          <button
            v-if="!role.isSystem"
            :disabled="readonly || deleting"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-red-200 px-4 text-sm font-medium text-red-600 hover:bg-red-50 disabled:opacity-50"
            @click="onDelete"
          >
            <Trash2 class="h-4 w-4" />
            删除角色
          </button>
          <span v-else class="text-xs text-gray-400">系统内置角色不可删除</span>
          <button
            :disabled="readonly || saving"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            @click="onSave"
          >
            <Loader2 v-if="saving" class="h-4 w-4 animate-spin" />
            保存
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Shield, AlertTriangle, Trash2, Loader2 } from 'lucide-vue-next'
import { updateRole, deleteRole, type RoleResponse } from '@/api/access'
import type { UpdateRoleRequest } from '@/types'

const props = defineProps<{ role: RoleResponse }>()
const emit = defineEmits<{ saved: []; deleted: [] }>()

const readonly = computed(() => props.role.pluginEnabled === false)
const saving = ref(false)
const deleting = ref(false)

const form = reactive({
  roleName: '',
  description: '',
  sortOrder: 0,
  enabled: true,
})

watch(
  () => props.role,
  (r) => {
    form.roleName = r.roleName || ''
    form.description = r.description || ''
    form.sortOrder = (r as any).level || 0
    form.enabled = r.isEnabled !== false
  },
  { immediate: true }
)

async function onSave() {
  if (!form.roleName.trim()) {
    ElMessage.error('请填写角色名称')
    return
  }
  saving.value = true
  try {
    const data: UpdateRoleRequest = {
      roleName: form.roleName.trim(),
      description: form.description,
      level: form.sortOrder,
      isEnabled: form.enabled,
    }
    await updateRole(props.role.id, data)
    ElMessage.success('保存成功')
    emit('saved')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function onDelete() {
  try {
    await ElMessageBox.confirm(`确定删除角色"${props.role.roleName}"吗?`, '删除确认', { type: 'warning' })
    deleting.value = true
    await deleteRole(props.role.id)
    ElMessage.success('删除成功')
    emit('deleted')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  } finally {
    deleting.value = false
  }
}
</script>
