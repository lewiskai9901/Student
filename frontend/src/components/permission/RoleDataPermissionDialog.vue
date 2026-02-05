<!--
  角色数据权限配置对话框
  用于配置角色在各个数据模块的数据访问范围
-->
<template>
  <el-dialog
    v-model="visible"
    title="数据权限配置"
    width="900px"
    :close-on-click-modal="false"
    @open="handleOpen"
  >
    <!-- 角色信息 -->
    <div class="mb-4 p-3 bg-blue-50 rounded-lg flex items-center gap-3">
      <Shield class="h-5 w-5 text-blue-600" />
      <div>
        <span class="font-medium text-gray-900">{{ role?.roleName }}</span>
        <span class="text-gray-500 text-sm ml-2">({{ role?.roleCode }})</span>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <Loader2 class="h-8 w-8 animate-spin text-blue-600" />
      <span class="ml-3 text-gray-500">加载中...</span>
    </div>

    <!-- 数据权限配置 -->
    <div v-else class="space-y-6">
      <div
        v-for="domain in groupedModules"
        :key="domain.domainCode"
        class="border border-gray-200 rounded-lg overflow-hidden"
      >
        <!-- 领域标题 -->
        <div class="bg-gray-50 px-4 py-2 border-b border-gray-200">
          <h4 class="font-medium text-gray-700">{{ domain.domainName }}</h4>
        </div>

        <!-- 模块列表 -->
        <div class="divide-y divide-gray-100">
          <div
            v-for="module in domain.modules"
            :key="module.moduleCode"
            class="px-4 py-3 flex items-center justify-between hover:bg-gray-50"
          >
            <!-- 模块名称 -->
            <div class="flex items-center gap-2 min-w-[150px]">
              <Database class="h-4 w-4 text-gray-400" />
              <span class="text-gray-700">{{ module.moduleName }}</span>
            </div>

            <!-- 数据范围选择 -->
            <div class="flex items-center gap-4">
              <el-select
                v-model="permissionMap[module.moduleCode].scopeCode"
                placeholder="选择数据范围"
                class="w-48"
                @change="(val: string) => handleScopeChange(module.moduleCode, val)"
              >
                <el-option
                  v-for="scope in scopeOptions"
                  :key="scope.scopeCode"
                  :label="scope.scopeName"
                  :value="scope.scopeCode"
                >
                  <div class="flex items-center gap-2">
                    <component :is="getScopeIcon(scope.scopeCode)" class="h-4 w-4 text-gray-400" />
                    <span>{{ scope.scopeName }}</span>
                  </div>
                </el-option>
              </el-select>

              <!-- 自定义范围配置按钮 -->
              <el-button
                v-if="permissionMap[module.moduleCode].scopeCode === 'CUSTOM'"
                type="primary"
                link
                @click="openScopeItemDialog(module)"
              >
                <Settings class="h-4 w-4 mr-1" />
                配置 ({{ permissionMap[module.moduleCode].scopeItems?.length || 0 }})
              </el-button>
              <span v-else class="text-gray-400 text-sm w-24">-</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="groupedModules.length === 0" class="text-center py-12 text-gray-500">
        <Database class="h-12 w-12 mx-auto text-gray-300 mb-2" />
        <p>暂无数据模块配置</p>
      </div>
    </div>

    <!-- 底部按钮 -->
    <template #footer>
      <div class="flex justify-end gap-3">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          保存配置
        </el-button>
      </div>
    </template>

    <!-- 自定义范围配置对话框 -->
    <CustomScopeItemDialog
      v-model="scopeItemDialogVisible"
      :module="currentModule"
      :items="currentScopeItems"
      @save="handleScopeItemsSave"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Shield,
  Database,
  Loader2,
  Settings,
  Globe,
  Building2,
  Users,
  User
} from 'lucide-vue-next'
import { roleDataPermissionApiV2 } from '@/api/access'
import CustomScopeItemDialog from './CustomScopeItemDialog.vue'

// Props
const props = defineProps<{
  role: {
    id: string | number
    roleCode: string
    roleName: string
  } | null
}>()

// Model
const visible = defineModel<boolean>({ default: false })

// 类型导入
import type {
  DomainWithModules,
  DataScopeOption,
  ScopeItem
} from '@/types/access'

// 状态
const loading = ref(false)
const saving = ref(false)
const groupedModules = ref<DomainWithModules[]>([])
const scopeOptions = ref<DataScopeOption[]>([])
const permissionMap = reactive<Record<string, { scopeCode: string; scopeItems: ScopeItem[] }>>({})

// 自定义范围对话框
const scopeItemDialogVisible = ref(false)
const currentModule = ref<{ moduleCode: string; moduleName: string } | null>(null)
const currentScopeItems = computed<ScopeItem[]>(() => {
  if (!currentModule.value) return []
  return permissionMap[currentModule.value.moduleCode]?.scopeItems || []
})

// 获取范围图标
function getScopeIcon(scopeCode: string) {
  const icons: Record<string, any> = {
    'ALL': Globe,
    'DEPARTMENT_AND_BELOW': Building2,
    'DEPARTMENT': Building2,
    'CUSTOM': Settings,
    'SELF': User
  }
  return icons[scopeCode] || Database
}

// 打开对话框时加载数据
async function handleOpen() {
  if (!props.role?.id) return

  loading.value = true
  try {
    // 并行加载模块列表、范围选项、当前配置
    const [modulesRes, scopesRes, configRes] = await Promise.all([
      roleDataPermissionApiV2.getModules(),
      roleDataPermissionApiV2.getScopes(),
      roleDataPermissionApiV2.getConfig(props.role.id)
    ])

    // 设置数据
    groupedModules.value = modulesRes || []
    scopeOptions.value = scopesRes || []

    // 初始化权限映射
    Object.keys(permissionMap).forEach(key => delete permissionMap[key])

    // 先用默认值初始化所有模块
    for (const domain of groupedModules.value) {
      for (const module of domain.modules) {
        permissionMap[module.moduleCode] = {
          scopeCode: 'SELF',  // 默认仅本人
          scopeItems: []
        }
      }
    }

    // 用配置数据覆盖
    if (configRes?.modulePermissions) {
      for (const mp of configRes.modulePermissions) {
        if (permissionMap[mp.moduleCode]) {
          permissionMap[mp.moduleCode] = {
            scopeCode: mp.scopeCode || 'SELF',
            scopeItems: mp.scopeItems || []
          }
        }
      }
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

// 范围变更时清空自定义项
function handleScopeChange(moduleCode: string, scopeCode: string) {
  if (scopeCode !== 'CUSTOM') {
    permissionMap[moduleCode].scopeItems = []
  }
}

// 打开自定义范围配置
function openScopeItemDialog(module: { moduleCode: string; moduleName: string }) {
  currentModule.value = module
  scopeItemDialogVisible.value = true
}

// 保存自定义范围项
function handleScopeItemsSave(items: ScopeItem[]) {
  if (currentModule.value) {
    permissionMap[currentModule.value.moduleCode].scopeItems = items
  }
  scopeItemDialogVisible.value = false
}

// 保存配置
async function handleSave() {
  if (!props.role?.id) return

  saving.value = true
  try {
    const modulePermissions = Object.entries(permissionMap).map(([moduleCode, config]) => ({
      moduleCode,
      scopeCode: config.scopeCode,
      scopeItems: config.scopeCode === 'CUSTOM' ? config.scopeItems : []
    }))

    await roleDataPermissionApiV2.saveConfig(props.role.id, {
      roleId: props.role.id,
      roleName: props.role.roleName,
      modulePermissions
    })

    ElMessage.success('保存成功')
    visible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>
