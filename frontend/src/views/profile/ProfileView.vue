<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <!-- 页面标题 -->
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-semibold text-gray-900">个人资料</h1>
    </div>

    <!-- 个人信息卡片 -->
    <div class="bg-white rounded-xl shadow-sm border border-gray-200">
      <div class="px-6 py-4 border-b border-gray-100">
        <h2 class="text-lg font-medium text-gray-900">基本信息</h2>
        <p class="text-sm text-gray-500 mt-1">管理您的个人信息</p>
      </div>

      <div class="p-6">
        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-width="100px"
          class="max-w-lg"
        >
          <el-form-item label="账号">
            <el-input v-model="userInfo.username" disabled />
            <div class="text-xs text-gray-400 mt-1">账号不可修改</div>
          </el-form-item>

          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
          </el-form-item>

          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="profileForm.gender">
              <el-radio :label="1">男</el-radio>
              <el-radio :label="2">女</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="手机号" prop="phone">
            <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSaveProfile" :loading="saving">
              保存修改
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 修改密码卡片 -->
    <div class="bg-white rounded-xl shadow-sm border border-gray-200">
      <div class="px-6 py-4 border-b border-gray-100">
        <h2 class="text-lg font-medium text-gray-900">修改密码</h2>
        <p class="text-sm text-gray-500 mt-1">定期更换密码可以提高账户安全性</p>
      </div>

      <div class="p-6">
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="100px"
          class="max-w-lg"
        >
          <el-form-item label="当前密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="请输入当前密码"
              show-password
            />
          </el-form-item>

          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入新密码（6-32个字符）"
              show-password
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleChangePassword" :loading="changingPassword">
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 账号信息卡片 -->
    <div class="bg-white rounded-xl shadow-sm border border-gray-200">
      <div class="px-6 py-4 border-b border-gray-100">
        <h2 class="text-lg font-medium text-gray-900">账号信息</h2>
        <p class="text-sm text-gray-500 mt-1">您的账号相关信息</p>
      </div>

      <div class="p-6">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="block text-sm font-medium text-gray-500 mb-1">所属组织</label>
            <div class="text-gray-900">{{ userInfo.orgUnit?.orgUnitName || '-' }}</div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-500 mb-1">所属班级</label>
            <div class="text-gray-900">{{ userInfo.classInfo?.className || '-' }}</div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-500 mb-1">角色</label>
            <div class="flex flex-wrap gap-2">
              <el-tag v-for="role in userInfo.roles" :key="role" size="small">{{ role }}</el-tag>
              <span v-if="!userInfo.roles?.length" class="text-gray-400">-</span>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-500 mb-1">账号状态</label>
            <el-tag :type="userInfo.status === 1 ? 'success' : 'danger'" size="small">
              {{ userInfo.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 我的角色与权限卡片 -->
    <div class="bg-white rounded-xl shadow-sm border border-gray-200">
      <div class="px-6 py-4 border-b border-gray-100">
        <h2 class="text-lg font-medium text-gray-900">我的角色与权限</h2>
        <p class="text-sm text-gray-500 mt-1">当前分配的角色及其作用域和权限详情</p>
      </div>

      <div class="p-6 space-y-6">
        <!-- 角色列表 -->
        <div>
          <h3 class="text-sm font-medium text-gray-700 mb-3">角色分配</h3>
          <div v-if="myRolesLoading" class="py-4 text-center text-sm text-gray-500">
            加载中...
          </div>
          <div v-else-if="myRoles.length === 0" class="py-4 text-center text-sm text-gray-400">
            暂无角色分配
          </div>
          <table v-else class="w-full text-sm">
            <thead>
              <tr class="border-b border-gray-200 bg-gray-50">
                <th class="px-3 py-2 text-left font-medium text-gray-600">角色名称</th>
                <th class="px-3 py-2 text-left font-medium text-gray-600">角色编码</th>
                <th class="px-3 py-2 text-left font-medium text-gray-600">作用域</th>
                <th class="px-3 py-2 text-left font-medium text-gray-600">作用域名称</th>
                <th class="px-3 py-2 text-left font-medium text-gray-600">过期时间</th>
                <th class="px-3 py-2 text-center font-medium text-gray-600">状态</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(role, idx) in myRoles"
                :key="idx"
                class="border-b border-gray-100"
                :class="role.isExpired ? 'opacity-50' : ''"
              >
                <td class="px-3 py-2 font-medium text-gray-900">{{ role.roleName }}</td>
                <td class="px-3 py-2">
                  <span class="rounded bg-gray-100 px-1.5 py-0.5 font-mono text-xs text-gray-600">{{ role.roleCode }}</span>
                </td>
                <td class="px-3 py-2 text-gray-600">{{ role.scopeType === 'ALL' ? '全局' : '指定组织' }}</td>
                <td class="px-3 py-2 text-gray-600">{{ role.scopeName || (role.scopeType === 'ALL' ? '-' : '-') }}</td>
                <td class="px-3 py-2 text-gray-500">{{ role.expiresAt ? role.expiresAt.substring(0, 10) : '永久' }}</td>
                <td class="px-3 py-2 text-center">
                  <span v-if="role.isExpired" class="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-500">已过期</span>
                  <span v-else class="rounded bg-green-100 px-1.5 py-0.5 text-xs text-green-600">有效</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 功能权限列表 -->
        <div>
          <h3 class="text-sm font-medium text-gray-700 mb-3">功能权限</h3>
          <div v-if="myPermissionsLoading" class="py-4 text-center text-sm text-gray-500">
            加载中...
          </div>
          <div v-else-if="myPermissionCodes.length === 0" class="py-4 text-center text-sm text-gray-400">
            暂无功能权限
          </div>
          <div v-else class="flex flex-wrap gap-2">
            <span
              v-for="code in myPermissionCodes"
              :key="code"
              class="rounded bg-blue-50 px-2 py-1 text-xs text-blue-700"
            >{{ code }}</span>
          </div>
          <div v-if="myPermissionCodes.length > 0" class="mt-2 text-xs text-gray-400">
            共 {{ myPermissionCodes.length }} 项权限
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getCurrentUser, updateProfile, changePassword } from '@/api/auth'
import type { UserInfo } from '@/types/auth'
import { useAuthStore } from '@/stores/auth'
import { getCurrentUserRoles, getCurrentUserPermissions } from '@/api/access'

const authStore = useAuthStore()

// 用户信息
const userInfo = ref<UserInfo>({
  userId: '',
  username: '',
  realName: '',
  status: 1,
  roles: [],
  permissions: []
})

// 表单引用
const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()

// 加载状态
const saving = ref(false)
const changingPassword = ref(false)

// 个人资料表单
const profileForm = reactive({
  realName: '',
  phone: '',
  email: '',
  gender: undefined as number | undefined
})

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单验证规则
const profileRules: FormRules = {
  realName: [
    { max: 50, message: '姓名长度不能超过50个字符', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

const passwordRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度必须在6-32个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 加载用户信息
const loadUserInfo = async () => {
  try {
    const data = await getCurrentUser()
    userInfo.value = data
    // 填充表单
    profileForm.realName = data.realName || ''
    profileForm.phone = data.phone || ''
    profileForm.email = data.email || ''
    profileForm.gender = data.gender
  } catch (error) {
    console.error('加载用户信息失败:', error)
    ElMessage.error('加载用户信息失败')
  }
}

// 保存个人资料
const handleSaveProfile = async () => {
  if (!profileFormRef.value) return

  try {
    await profileFormRef.value.validate()
    saving.value = true

    await updateProfile({
      realName: profileForm.realName || undefined,
      phone: profileForm.phone || undefined,
      email: profileForm.email || undefined,
      gender: profileForm.gender
    })

    ElMessage.success('保存成功')
    // 刷新用户信息
    await loadUserInfo()
    // 更新 store 中的用户名
    authStore.userName = profileForm.realName || userInfo.value.username
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('保存失败:', error)
      ElMessage.error(error.message || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordFormRef.value) return

  try {
    await passwordFormRef.value.validate()
    changingPassword.value = true

    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })

    ElMessage.success('密码修改成功')
    // 清空表单
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordFormRef.value.resetFields()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('修改密码失败:', error)
      ElMessage.error(error.message || '修改密码失败')
    }
  } finally {
    changingPassword.value = false
  }
}

// ==================== 我的角色与权限 ====================

interface MyRoleInfo {
  roleName: string
  roleCode: string
  scopeType: string
  scopeId: number | string
  scopeName?: string
  expiresAt?: string
  isExpired: boolean
}

const myRoles = ref<MyRoleInfo[]>([])
const myRolesLoading = ref(false)
const myPermissionCodes = ref<string[]>([])
const myPermissionsLoading = ref(false)

const loadMyRoles = async () => {
  myRolesLoading.value = true
  try {
    const roles = await getCurrentUserRoles()
    const now = new Date().toISOString()
    myRoles.value = (Array.isArray(roles) ? roles : []).map((r: any) => ({
      roleName: r.roleName || '',
      roleCode: r.roleCode || '',
      scopeType: r.scopeType || 'ALL',
      scopeId: r.scopeId || 0,
      scopeName: r.scopeName || '',
      expiresAt: r.expiresAt,
      isExpired: r.expiresAt ? r.expiresAt < now : false
    }))
  } catch (error) {
    console.error('加载角色失败:', error)
  } finally {
    myRolesLoading.value = false
  }
}

const loadMyPermissions = async () => {
  myPermissionsLoading.value = true
  try {
    const perms = await getCurrentUserPermissions()
    myPermissionCodes.value = (Array.isArray(perms) ? perms : []).map((p: any) =>
      typeof p === 'string' ? p : p.permissionCode || p.code || ''
    ).filter(Boolean)
  } catch (error) {
    console.error('加载权限失败:', error)
  } finally {
    myPermissionsLoading.value = false
  }
}

onMounted(() => {
  loadUserInfo()
  loadMyRoles()
  loadMyPermissions()
})
</script>
