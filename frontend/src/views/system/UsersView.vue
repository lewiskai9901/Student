<template>
  <div class="tm-page">
    <!-- Header -->
    <div class="tm-header">
      <div>
        <h1 class="tm-title">用户管理</h1>
        <div class="tm-stats">
          <span>总数 <b>{{ total }}</b></span>
          <span class="sep" />
          <span><span class="dot dot-green" />启用 <b>{{ enabledCount }}</b></span>
          <span class="sep" />
          <span><span class="dot dot-gray" />禁用 <b>{{ disabledCount }}</b></span>
          <span class="sep" />
          <span>今日登录 <b>{{ todayLoginCount }}</b></span>
        </div>
      </div>
      <button class="tm-btn tm-btn-primary" @click="handleAdd">新增用户</button>
    </div>

    <!-- Filter Bar -->
    <div class="tm-filters">
      <input
        v-model="queryParams.username"
        type="text"
        placeholder="账号"
        class="tm-input filter-input"
        @keyup.enter="handleQuery"
      />
      <input
        v-model="queryParams.realName"
        type="text"
        placeholder="姓名"
        class="tm-input filter-input"
        @keyup.enter="handleQuery"
      />
      <input
        v-model="queryParams.phone"
        type="text"
        placeholder="手机号"
        class="tm-input filter-input"
        @keyup.enter="handleQuery"
      />
      <select v-model="queryParams.status" class="tm-select">
        <option :value="undefined">全部状态</option>
        <option :value="1">启用</option>
        <option :value="2">禁用</option>
      </select>
      <button class="tm-btn tm-btn-primary" style="padding: 7px 16px;" @click="handleQuery">查询</button>
      <button class="tm-btn-reset" @click="resetQuery">重置</button>
    </div>

    <!-- Batch Actions Bar -->
    <div v-if="selectedIds.length > 0" class="batch-bar">
      <span class="batch-info">已选 {{ selectedIds.length }} 项</span>
      <button class="tm-action tm-action-danger" @click="handleBatchDelete">批量删除</button>
    </div>

    <!-- Table -->
    <div class="tm-table-wrap">
      <table class="tm-table">
        <colgroup>
          <col style="width: 36px;" />
          <col style="width: 10%;" />
          <col style="width: 9%;" />
          <col style="width: 11%;" />
          <col style="width: 8%;" />
          <col style="width: 12%;" />
          <col style="width: 10%;" />
          <col style="width: 7%;" />
          <col style="width: 12%;" />
          <col style="width: 13%;" />
        </colgroup>
        <thead>
          <tr>
            <th>
              <input
                type="checkbox"
                :checked="isAllSelected"
                :indeterminate="isIndeterminate"
                @change="handleSelectAll"
                class="row-checkbox"
              />
            </th>
            <th class="text-left">账号</th>
            <th class="text-left">姓名</th>
            <th class="text-left">手机号</th>
            <th>用户类型</th>
            <th>角色</th>
            <th class="text-left">组织</th>
            <th>状态</th>
            <th class="text-left">最后登录</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="10" class="tm-empty">
              <span class="tm-spin" style="display: inline-block; width: 16px; height: 16px; border: 2px solid #e5e7eb; border-top-color: #2563eb; border-radius: 50%;" />
              加载中...
            </td>
          </tr>
          <tr v-else-if="userList.length === 0">
            <td colspan="10" class="tm-empty">暂无用户数据</td>
          </tr>
          <tr v-for="row in userList" :key="row.id">
            <td>
              <input
                type="checkbox"
                :checked="selectedIds.includes(row.id)"
                @change="handleSelectRow(row)"
                class="row-checkbox"
              />
            </td>
            <td class="text-left" style="font-weight: 500; color: #111827;">{{ row.username }}</td>
            <td class="text-left">{{ row.realName || '-' }}</td>
            <td class="text-left" style="color: #6b7280;">{{ row.phone || '-' }}</td>
            <td>
              <span v-if="getUserTypeName(row.userType)" class="tm-chip tm-chip-purple">{{ getUserTypeName(row.userType) }}</span>
              <span v-else style="color: #9ca3af;">-</span>
            </td>
            <td>
              <template v-if="row.roleNames && row.roleNames.length">
                <span
                  v-for="(role, index) in row.roleNames"
                  :key="index"
                  class="tm-chip tm-chip-blue"
                  style="margin: 1px 2px;"
                >{{ role }}</span>
              </template>
              <span v-else style="color: #9ca3af;">-</span>
            </td>
            <td class="text-left" style="color: #6b7280;">{{ row.orgUnitName || '-' }}</td>
            <td>
              <button
                @click="handleStatusChange(row)"
                :class="['tm-status', normalizeStatus(row.status) === 1 ? 'tm-status-active' : 'tm-status-inactive']"
              >
                <em class="tm-status-dot" />
                {{ normalizeStatus(row.status) === 1 ? '启用' : '禁用' }}
              </button>
            </td>
            <td class="text-left" style="font-size: 12px; color: #9ca3af;">{{ row.lastLoginTime || '-' }}</td>
            <td>
              <button class="tm-action" @click="handleEdit(row)">编辑</button>
              <button class="tm-action" @click="handleAssignRoles(row)">角色</button>
              <button class="tm-action" @click="handleResetPassword(row)">密码</button>
              <button class="tm-action tm-action-danger" @click="handleDelete(row)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="tm-pagination">
      <span class="tm-page-info">共 {{ total }} 条，第 {{ queryParams.pageNum }} / {{ Math.ceil(total / queryParams.pageSize) || 1 }} 页</span>
      <div class="tm-page-controls">
        <select
          class="tm-page-select"
          :value="queryParams.pageSize"
          @change="queryParams.pageSize = Number(($event.target as HTMLSelectElement).value); loadUserList()"
        >
          <option :value="10">10 条/页</option>
          <option :value="20">20 条/页</option>
          <option :value="50">50 条/页</option>
          <option :value="100">100 条/页</option>
        </select>
        <button
          class="tm-page-btn"
          :disabled="queryParams.pageNum <= 1"
          @click="queryParams.pageNum = 1; loadUserList()"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="11 17 6 12 11 7"/><polyline points="18 17 13 12 18 7"/></svg>
        </button>
        <button
          class="tm-page-btn"
          :disabled="queryParams.pageNum <= 1"
          @click="queryParams.pageNum--; loadUserList()"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <span class="tm-page-current">{{ queryParams.pageNum }}</span>
        <button
          class="tm-page-btn"
          :disabled="queryParams.pageNum >= Math.ceil(total / queryParams.pageSize)"
          @click="queryParams.pageNum++; loadUserList()"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
        <button
          class="tm-page-btn"
          :disabled="queryParams.pageNum >= Math.ceil(total / queryParams.pageSize)"
          @click="queryParams.pageNum = Math.ceil(total / queryParams.pageSize); loadUserList()"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="13 17 18 12 13 7"/><polyline points="6 17 11 12 6 7"/></svg>
        </button>
      </div>
    </div>

    <!-- 新增/编辑 Drawer -->
    <Teleport to="body">
      <Transition name="tm-drawer">
        <div v-if="dialogVisible" class="tm-drawer-overlay" @click.self="dialogVisible = false">
          <div class="tm-drawer" style="width: 620px;">
            <div class="tm-drawer-header">
              <h2 class="tm-drawer-title">{{ dialogTitle }}</h2>
              <button class="tm-drawer-close" @click="dialogVisible = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="tm-drawer-body">
              <!-- Section: 账号信息 -->
              <div class="tm-section">
                <h3 class="tm-section-title">账号信息</h3>
                <div class="tm-field">
                  <label class="tm-label">账号 <span class="req">*</span></label>
                  <input
                    v-model="formData.username"
                    type="text"
                    placeholder="字母开头，仅限英文、数字、下划线（3-30位）"
                    :disabled="isEdit"
                    :class="['tm-input', isEdit ? 'field-disabled' : '', usernameError ? 'field-error' : '']"
                    @input="validateUsername"
                  />
                  <p v-if="usernameError" class="error-hint">{{ usernameError }}</p>
                </div>
                <div v-if="!isEdit" class="tm-field">
                  <label class="tm-label">密码 <span class="req">*</span></label>
                  <input
                    v-model="formData.password"
                    type="password"
                    placeholder="请输入密码（6-20位）"
                    class="tm-input"
                  />
                </div>
              </div>

              <!-- Section: 基本信息 -->
              <div class="tm-section">
                <h3 class="tm-section-title">基本信息</h3>
                <div class="tm-fields tm-cols-2">
                  <div class="tm-field">
                    <label class="tm-label">姓名 <span class="req">*</span></label>
                    <input v-model="formData.realName" type="text" placeholder="请输入姓名" class="tm-input" />
                  </div>
                  <div class="tm-field">
                    <label class="tm-label">工号</label>
                    <input v-model="formData.employeeNo" type="text" placeholder="请输入工号" class="tm-input" />
                  </div>
                </div>
                <div class="tm-fields tm-cols-2">
                  <div class="tm-field">
                    <label class="tm-label">手机号</label>
                    <input
                      v-model="formData.phone"
                      type="text"
                      placeholder="请输入11位手机号"
                      :class="['tm-input', phoneError ? 'field-error' : '']"
                      @input="validatePhone"
                    />
                    <p v-if="phoneError" class="error-hint">{{ phoneError }}</p>
                  </div>
                  <div class="tm-field">
                    <label class="tm-label">邮箱</label>
                    <input v-model="formData.email" type="email" placeholder="请输入邮箱" class="tm-input" />
                  </div>
                </div>
                <div class="tm-fields tm-cols-2">
                  <div class="tm-field">
                    <label class="tm-label">性别</label>
                    <div class="tm-radios">
                      <label class="tm-radio" :class="{ active: formData.gender === 1 }">
                        <input type="radio" :value="1" v-model="formData.gender" /> 男
                      </label>
                      <label class="tm-radio" :class="{ active: formData.gender === 2 }">
                        <input type="radio" :value="2" v-model="formData.gender" /> 女
                      </label>
                    </div>
                  </div>
                  <div class="tm-field">
                    <label class="tm-label">出生日期</label>
                    <input v-model="formData.birthDate" type="date" class="tm-input" />
                  </div>
                </div>
                <div class="tm-field">
                  <label class="tm-label">身份证号</label>
                  <input v-model="formData.idCard" type="text" placeholder="请输入身份证号" class="tm-input" />
                </div>
              </div>

              <!-- Section: 组织信息 -->
              <div class="tm-section">
                <h3 class="tm-section-title">组织与类型</h3>
                <div class="tm-field">
                  <label class="tm-label">用户类型 <span class="req">*</span></label>
                  <select v-model="formData.userTypeCode" class="tm-field-select">
                    <option value="">请选择用户类型</option>
                    <option v-for="ut in userTypes" :key="ut.typeCode" :value="ut.typeCode">
                      {{ ut.typeName }}
                    </option>
                  </select>
                  <div v-if="selectedUserType" style="margin-top: 4px; display: flex; gap: 8px;">
                    <span v-if="selectedUserType.features?.requiresOrg" style="font-size: 11px; color: #d97706;">需关联组织</span>
                    <span v-if="selectedUserType.features?.requiresPlace" style="font-size: 11px; color: #d97706;">需关联场所</span>
                    <span v-if="selectedUserType.defaultRoleCodes?.length" style="font-size: 11px; color: #9ca3af;">默认角色: {{ selectedUserType.defaultRoleCodes.join(', ') }}</span>
                  </div>
                </div>
                <div class="tm-fields tm-cols-2">
                  <div class="tm-field">
                    <label class="tm-label">
                      所属组织
                      <span v-if="selectedUserType?.features?.requiresOrg" class="req">*</span>
                    </label>
                    <select v-model="formData.orgUnitId" class="tm-field-select">
                      <option :value="undefined">请选择</option>
                      <option v-for="org in flatOrgUnits" :key="org.id" :value="org.id">
                        {{ org.label }}
                      </option>
                    </select>
                  </div>
                  <div class="tm-field">
                    <label class="tm-label">
                      关联场所
                      <span v-if="selectedUserType?.features?.requiresPlace" class="req">*</span>
                    </label>
                    <select v-model="formData.placeId" class="tm-field-select">
                      <option :value="undefined">请选择场所</option>
                      <option v-for="place in allPlaces" :key="place.placeId" :value="place.placeId">
                        {{ place.placeName }}
                      </option>
                    </select>
                  </div>
                </div>
                <div class="tm-field">
                  <label class="tm-label">状态</label>
                  <div class="tm-radios">
                    <label class="tm-radio" :class="{ active: formData.status === 1 }">
                      <input type="radio" :value="1" v-model="formData.status" /> 启用
                    </label>
                    <label class="tm-radio" :class="{ active: formData.status === 2 }">
                      <input type="radio" :value="2" v-model="formData.status" /> 禁用
                    </label>
                  </div>
                </div>
              </div>

              <!-- Dynamic Extension Fields (from SPI plugins) -->
              <div v-if="userTypeSchema && userTypeSchema.fields?.length > 0" class="tm-section">
                <h3 class="tm-section-title">扩展属性</h3>
                <DynamicForm :schema="userTypeSchema" v-model="formData.attributes" />
              </div>
            </div>
            <div class="tm-drawer-footer">
              <button class="tm-btn tm-btn-secondary" @click="dialogVisible = false">取消</button>
              <button class="tm-btn tm-btn-primary" :disabled="submitLoading" @click="handleSubmit">
                <span v-if="submitLoading" class="tm-spin" style="display: inline-block; width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%;" />
                确定
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 分配角色 Drawer -->
    <Teleport to="body">
      <Transition name="tm-drawer">
        <div v-if="roleDialogVisible" class="tm-drawer-overlay" @click.self="roleDialogVisible = false">
          <div class="tm-drawer" style="width: 680px;">
            <div class="tm-drawer-header">
              <h2 class="tm-drawer-title">分配角色 - {{ currentUserName }}</h2>
              <button class="tm-drawer-close" @click="roleDialogVisible = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="tm-drawer-body">
              <!-- 当前角色列表 -->
              <div v-if="currentUserRolesDetailed.length > 0" class="tm-section">
                <h3 class="tm-section-title">当前角色</h3>
                <table class="role-table">
                  <thead>
                    <tr>
                      <th class="text-left">角色名</th>
                      <th class="text-left">作用域</th>
                      <th class="text-left">过期时间</th>
                      <th class="text-left">原因</th>
                      <th>状态</th>
                      <th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="(ur, idx) in currentUserRolesDetailed"
                      :key="idx"
                      :style="ur.isExpired ? 'opacity: 0.5' : ''"
                    >
                      <td class="text-left" style="font-weight: 500; color: #111827;">{{ ur.roleName }}</td>
                      <td class="text-left">
                        <span v-if="ur.scopeType === 'ALL'" class="tm-chip tm-chip-blue">全局</span>
                        <span v-else style="font-size: 12px;">{{ ur.scopeName || '未指定' }}</span>
                      </td>
                      <td class="text-left" style="color: #9ca3af;">{{ ur.expiresAt ? ur.expiresAt.substring(0, 10) : '永久' }}</td>
                      <td class="text-left" style="color: #9ca3af; font-size: 11px; max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" :title="ur.reason">
                        {{ ur.reason || '-' }}
                      </td>
                      <td>
                        <span v-if="ur.isExpired" class="tm-chip tm-chip-gray">已过期</span>
                        <span v-else class="tm-chip tm-chip-green">有效</span>
                      </td>
                      <td>
                        <button class="tm-action tm-action-danger" @click="handleRemoveSingleRole(ur)">移除</button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- 添加角色 -->
              <div class="tm-section">
                <h3 class="tm-section-title">角色选择</h3>
                <div class="role-hint">
                  勾选角色并选择作用域："全局"不限范围，"指定组织"限定到某个组织节点及其子节点
                </div>
                <div class="role-list">
                  <div
                    v-for="role in allRoles"
                    :key="role.id"
                    :class="['role-card', isRoleSelected(role.id) ? 'role-card-active' : '']"
                  >
                    <div class="role-card-header">
                      <input
                        type="checkbox"
                        :checked="isRoleSelected(role.id)"
                        @change="toggleRole(role.id)"
                        class="row-checkbox"
                      />
                      <span style="font-weight: 500; color: #111827;">{{ role.roleName }}</span>
                      <span v-if="role.roleCode" class="tm-code" style="margin-left: 4px;">{{ role.roleCode }}</span>
                    </div>
                    <!-- Scope + Expiry + Reason -->
                    <div v-if="isRoleSelected(role.id)" class="role-card-body">
                      <!-- Scope -->
                      <div class="scope-row">
                        <span class="scope-label">作用域</span>
                        <label class="scope-option">
                          <input type="radio" :name="'scope-' + role.id" value="ALL"
                            :checked="getRoleScope(role.id).scopeType === 'ALL'"
                            @change="updateRoleScopeType(role.id, 'ALL')" />
                          <span>全局</span>
                        </label>
                        <label class="scope-option">
                          <input type="radio" :name="'scope-' + role.id" value="ORG_UNIT"
                            :checked="getRoleScope(role.id).scopeType === 'ORG_UNIT'"
                            @change="updateRoleScopeType(role.id, 'ORG_UNIT')" />
                          <span>指定组织</span>
                        </label>
                        <select
                          :value="getRoleScope(role.id).scopeId"
                          @change="updateRoleScopeId(role.id, Number(($event.target as HTMLSelectElement).value))"
                          class="scope-select"
                          :disabled="getRoleScope(role.id).scopeType !== 'ORG_UNIT'"
                          :style="getRoleScope(role.id).scopeType !== 'ORG_UNIT' ? 'opacity: 0.3' : ''"
                        >
                          <option :value="0">请选择组织</option>
                          <option v-for="org in flatOrgUnits" :key="org.id" :value="org.id">
                            {{ org.label }}
                          </option>
                        </select>
                      </div>
                      <!-- Expiry & Reason -->
                      <div class="scope-row">
                        <span class="scope-label">过期时间</span>
                        <input
                          type="date"
                          :value="getRoleScope(role.id).expiresAt || ''"
                          @input="updateRoleExpiry(role.id, ($event.target as HTMLInputElement).value)"
                          class="scope-date-input"
                          placeholder="空=永久"
                        />
                        <span class="scope-label" style="margin-left: 8px;">原因</span>
                        <input
                          type="text"
                          :value="getRoleScope(role.id).reason || ''"
                          @input="updateRoleReason(role.id, ($event.target as HTMLInputElement).value)"
                          class="scope-text-input"
                          placeholder="可选"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="tm-drawer-footer">
              <button class="tm-btn tm-btn-secondary" @click="roleDialogVisible = false">取消</button>
              <button class="tm-btn tm-btn-primary" :disabled="roleSubmitLoading" @click="handleRoleSubmit">
                <span v-if="roleSubmitLoading" class="tm-spin" style="display: inline-block; width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%;" />
                确定
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 数据范围对话框已移除 - 数据权限现在在角色级别配置 -->
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'


import { getOrgUnitTree } from '@/api/organization'
import { universalPlaceApi } from '@/api/universalPlace'
import type { UserType as UserTypeItem } from '@/types/userType'
import type { OrgUnitTreeNode } from '@/types'
// User API
import {
  getUserPage,
  createUser,
  updateUser,
  deleteUser,
  batchDeleteUsers,
  resetPassword,
  updateUserStatus,
  getUserRoleAssignments,
  assignRoles
} from '@/api/user'
import type {
  UserQueryParams,
  UserFormData,
  UserListItem
} from '@/types/user'
import { getAllRoles, removeUserRoleWithScope } from '@/api/access'
import type { RoleResponse as Role } from '@/api/access'
import { useConfigStore } from '@/stores/config'
import DynamicForm from '@/components/extension/DynamicForm.vue'
import { entityTypeApi } from '@/api/entityType'

const configStore = useConfigStore()
const loading = ref(false)
const submitLoading = ref(false)
const roleSubmitLoading = ref(false)
const dialogVisible = ref(false)
const roleDialogVisible = ref(false)
// dataScopeDialogVisible 已移除 - 数据权限现在在角色级别配置
const isEdit = ref(false)
const selectedIds = ref<(number | string)[]>([])
const currentUserId = ref<number | string>()
const usernameError = ref('')
const phoneError = ref('')

const queryParams = reactive<UserQueryParams & { pageNum: number; pageSize: number }>({
  pageNum: 1,
  pageSize: configStore.defaultPageSize
})

const userList = ref<UserListItem[]>([])
const total = ref(0)
const allRoles = ref<Role[]>([])
// 角色分配（含作用域、过期时间、原因）
interface RoleAssignmentLocal {
  roleId: string | number
  scopeType: string
  scopeId: number | string
  expiresAt?: string
  reason?: string
}
const roleAssignments = ref<RoleAssignmentLocal[]>([])
const currentUserName = ref('')

// 当前用户的角色详细信息（用于显示表格）
interface UserRoleDetailed {
  roleId: string | number
  roleName: string
  roleCode?: string
  scopeType: string
  scopeId: number | string
  scopeName?: string
  expiresAt?: string
  isExpired: boolean
  assignedAt?: string
  reason?: string
}
const currentUserRolesDetailed = ref<UserRoleDetailed[]>([])
const userTypes = ref<UserTypeItem[]>([])
const orgTree = ref<OrgUnitTreeNode[]>([])
// 全量场所列表（独立于组织）
interface PlaceItem { placeId: number; placeName: string }
const allPlaces = ref<PlaceItem[]>([])

// 当前选中的用户类型
const selectedUserType = computed<UserTypeItem | null>(() => {
  if (!formData.userTypeCode) return null
  return userTypes.value.find(ut => ut.typeCode === formData.userTypeCode) || null
})

// 根据 typeCode 获取用户类型名称
const getUserTypeName = (typeCode?: string): string => {
  if (!typeCode) return ''
  const ut = userTypes.value.find(t => t.typeCode === typeCode)
  return ut?.typeName || typeCode
}

// 将组织树扁平化为列表（带缩进标记）
interface FlatOrgItem { id: number | string; label: string }
const flatOrgUnits = computed<FlatOrgItem[]>(() => {
  const result: FlatOrgItem[] = []
  const flatten = (nodes: OrgUnitTreeNode[], depth: number) => {
    for (const node of nodes) {
      const prefix = '\u00A0\u00A0'.repeat(depth)
      result.push({ id: node.id, label: prefix + (node.unitName || node.label || '') })
      if (node.children?.length) flatten(node.children, depth + 1)
    }
  }
  flatten(orgTree.value, 0)
  return result
})

// 将 status 统一为数字：1=启用, 2=禁用
const normalizeStatus = (status: any): number => {
  if (status === '启用' || status === 1) return 1
  if (status === '禁用' || status === 2) return 2
  return Number(status) || 1
}

// 统计数据
const enabledCount = computed(() => userList.value.filter(u => normalizeStatus(u.status) === 1).length)
const disabledCount = computed(() => userList.value.filter(u => normalizeStatus(u.status) === 2).length)
const todayLoginCount = ref(0)

const formData = reactive<UserFormData & { placeId?: number; attributes: Record<string, any> }>({
  username: '',
  realName: '',
  password: '',
  phone: '',
  email: '',
  employeeNo: '',
  gender: 1,
  birthDate: '',
  idCard: '',
  userTypeCode: '',
  orgUnitId: undefined,
  placeId: undefined,
  status: 1,
  attributes: {} as Record<string, any>,
})

// Dynamic schema for user type (loaded from entity_type_configs)
const userTypeSchema = ref<{ fields: any[] } | null>(null)

// Watch userTypeCode changes to load schema
watch(() => formData.userTypeCode, async (newCode) => {
  userTypeSchema.value = null
  formData.attributes = {}
  if (!newCode) return
  try {
    const res = await entityTypeApi.get('USER', newCode)
    const data = (res as any).data || res
    if (data?.metadataSchema) {
      const schema = typeof data.metadataSchema === 'string' ? JSON.parse(data.metadataSchema) : data.metadataSchema
      if (schema?.fields?.length > 0) userTypeSchema.value = schema
    }
  } catch { /* no plugin for this user type */ }
})

const validateUsername = () => {
  const val = formData.username
  if (!val) {
    usernameError.value = ''
    return
  }
  if (!/^[a-zA-Z]/.test(val)) {
    usernameError.value = '必须以英文字母开头'
  } else if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(val)) {
    usernameError.value = '只能包含英文字母、数字和下划线'
  } else if (val.length < 3) {
    usernameError.value = '至少3个字符'
  } else if (val.length > 30) {
    usernameError.value = '最多30个字符'
  } else {
    usernameError.value = ''
  }
}

const validatePhone = () => {
  const val = formData.phone
  if (!val) {
    phoneError.value = ''
    return
  }
  if (!/^1[3-9]\d{9}$/.test(val)) {
    phoneError.value = '请输入正确的11位手机号'
  } else {
    phoneError.value = ''
  }
}

const dialogTitle = computed(() => (isEdit.value ? '编辑用户' : '新增用户'))

// 加载全量场所列表（独立于组织）
const loadAllPlaces = async () => {
  try {
    const tree = await universalPlaceApi.getTree()
    const result: PlaceItem[] = []
    const flatten = (nodes: any[], depth: number) => {
      for (const n of nodes) {
        const prefix = '\u00A0\u00A0'.repeat(depth)
        result.push({ placeId: n.id, placeName: prefix + (n.placeName || n.name || '') })
        if (n.children?.length) flatten(n.children, depth + 1)
      }
    }
    flatten(tree, 0)
    allPlaces.value = result
  } catch (error) {
    console.error('加载场所列表失败:', error)
  }
}

const isAllSelected = computed(() => {
  return userList.value.length > 0 && selectedIds.value.length === userList.value.length
})

const isIndeterminate = computed(() => {
  return selectedIds.value.length > 0 && selectedIds.value.length < userList.value.length
})

const loadUserList = async () => {
  loading.value = true
  try {
    const res = await getUserPage(queryParams)
    const records = (res.records || [])
    records.forEach((u: any) => { u.status = normalizeStatus(u.status) })
    userList.value = records as any
    total.value = res.total || 0
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const loadAllRoles = async () => {
  try {
    allRoles.value = await getAllRoles()
  } catch (error) {
    console.error('加载角色列表失败:', error)
  }
}

const loadUserTypes = async () => {
  try {
    // 从 entity_type_configs 统一读取用户类型
    const res = await entityTypeApi.list('USER')
    const data = (res as any).data || res || []
    userTypes.value = data.map((t: any) => ({
      typeCode: t.typeCode, typeName: t.typeName, category: t.category,
      features: typeof t.features === 'string' ? JSON.parse(t.features) : t.features || {},
      defaultRoleCodes: [],
    }))
  } catch (error) {
    console.error('加载用户类型失败:', error)
  }
}

const loadOrgTree = async () => {
  try {
    orgTree.value = await getOrgUnitTree()
  } catch (error) {
    console.error('加载组织树失败:', error)
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  loadUserList()
}

const resetQuery = () => {
  Object.assign(queryParams, {
    pageNum: 1,
    pageSize: configStore.defaultPageSize,
    username: undefined,
    realName: undefined,
    phone: undefined,
    status: undefined
  })
  loadUserList()
}

const handleSelectAll = (event: Event) => {
  const checked = (event.target as HTMLInputElement).checked
  if (checked) {
    selectedIds.value = userList.value.map(item => item.id)
  } else {
    selectedIds.value = []
  }
}

const handleSelectRow = (row: UserListItem) => {
  const id = row.id
  const index = selectedIds.value.indexOf(id)
  if (index > -1) {
    selectedIds.value.splice(index, 1)
  } else {
    selectedIds.value.push(id)
  }
}

const handleAdd = () => {
  isEdit.value = false
  usernameError.value = ''
  phoneError.value = ''
  Object.assign(formData, {
    username: '',
    realName: '',
    password: '',
    phone: '',
    email: '',
    employeeNo: '',
    gender: 1,
    birthDate: '',
    idCard: '',
    userTypeCode: '',
    orgUnitId: undefined,
    placeId: undefined,
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = async (row: UserListItem) => {
  if (!row.id) {
    ElMessage.error('用户ID无效')
    return
  }
  isEdit.value = true
  usernameError.value = ''
  phoneError.value = ''
  Object.assign(formData, {
    username: row.username,
    realName: row.realName,
    phone: row.phone || '',
    email: row.email || '',
    employeeNo: row.employeeNo || '',
    gender: row.gender,
    birthDate: row.birthDate ? String(row.birthDate).substring(0, 10) : '',
    idCard: row.idCard || '',
    userTypeCode: row.userType || '',
    orgUnitId: row.orgUnitId || undefined,
    status: normalizeStatus(row.status)
  })
  currentUserId.value = row.id
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formData.username) {
    ElMessage.error('请输入账号')
    return
  }
  if (!isEdit.value && !/^[a-zA-Z][a-zA-Z0-9_]*$/.test(formData.username)) {
    ElMessage.error('账号只能包含英文字母、数字和下划线，且必须以字母开头')
    return
  }
  if (!isEdit.value && (formData.username.length < 3 || formData.username.length > 30)) {
    ElMessage.error('账号长度须为3-30位')
    return
  }
  if (!isEdit.value && !formData.password) {
    ElMessage.error('请输入密码')
    return
  }
  if (!formData.realName) {
    ElMessage.error('请输入姓名')
    return
  }
  if (formData.phone && !/^1[3-9]\d{9}$/.test(formData.phone)) {
    ElMessage.error('手机号格式不正确')
    return
  }
  if (usernameError.value || phoneError.value) {
    ElMessage.error(usernameError.value || phoneError.value)
    return
  }
  if (!formData.userTypeCode) {
    ElMessage.error('请选择用户类型')
    return
  }
  if (selectedUserType.value?.features?.requiresOrg && !formData.orgUnitId) {
    ElMessage.error('该用户类型需要关联组织')
    return
  }
  if (selectedUserType.value?.features?.requiresPlace && !formData.placeId) {
    ElMessage.error('该用户类型需要关联场所')
    return
  }
  try {
    submitLoading.value = true
    // 清理空字符串为 undefined，避免后端 @Pattern 对空串校验失败
    const payload = { ...formData } as any
    if (!payload.phone) payload.phone = undefined
    if (!payload.email) payload.email = undefined
    if (!payload.employeeNo) payload.employeeNo = undefined
    if (!payload.idCard) payload.idCard = undefined
    if (!payload.birthDate) payload.birthDate = undefined
    if (isEdit.value && currentUserId.value) {
      await updateUser(currentUserId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createUser(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    queryParams.pageNum = 1
    loadUserList()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row: UserListItem) => {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确定要删除用户"${row.realName}"吗?`, '删除确认', { type: 'warning' })
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    loadUserList()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的${selectedIds.value.length}个用户吗?`, '批量删除', { type: 'warning' })
    await batchDeleteUsers(selectedIds.value)
    ElMessage.success('删除成功')
    selectedIds.value = []
    loadUserList()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

const handleStatusChange = async (row: UserListItem) => {
  if (!row.id) return
  const currentStatus = normalizeStatus(row.status)
  const newStatus = currentStatus === 1 ? 2 : 1
  try {
    await updateUserStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success('状态已更新')
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败')
    loadUserList()
  }
}

const handleResetPassword = async (row: UserListItem) => {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确定要重置用户"${row.realName}"的密码吗?`, '重置密码', { type: 'warning' })
    const newPassword = await resetPassword(row.id)
    ElMessageBox.alert(
      `新密码为: <strong style="font-size:16px;color:#409EFF">${newPassword || 'admin123'}</strong><br/><span style="color:#999;font-size:12px">请妥善保管并通知用户</span>`,
      '密码重置成功',
      { dangerouslyUseHTMLString: true, confirmButtonText: '我已记住' }
    )
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '重置失败')
  }
}

// ==================== 角色 scope 辅助方法 ====================

const isRoleSelected = (roleId: string | number) => {
  return roleAssignments.value.some(a => String(a.roleId) === String(roleId))
}

const getRoleScope = (roleId: string | number) => {
  return roleAssignments.value.find(a => String(a.roleId) === String(roleId)) || { scopeType: 'ALL', scopeId: 0, expiresAt: '', reason: '' }
}

const toggleRole = (roleId: string | number) => {
  const idx = roleAssignments.value.findIndex(a => String(a.roleId) === String(roleId))
  if (idx > -1) {
    roleAssignments.value.splice(idx, 1)
  } else {
    roleAssignments.value.push({ roleId: String(roleId), scopeType: 'ALL', scopeId: 0, expiresAt: '', reason: '' })
  }
}

const updateRoleScopeType = (roleId: string | number, scopeType: string) => {
  const a = roleAssignments.value.find(a => String(a.roleId) === String(roleId))
  if (a) {
    a.scopeType = scopeType
    if (scopeType === 'ALL') a.scopeId = 0
  }
}

const updateRoleScopeId = (roleId: string | number, scopeId: number) => {
  const a = roleAssignments.value.find(a => String(a.roleId) === String(roleId))
  if (a) a.scopeId = scopeId
}

const updateRoleExpiry = (roleId: string | number, expiresAt: string) => {
  const a = roleAssignments.value.find(a => String(a.roleId) === String(roleId))
  if (a) a.expiresAt = expiresAt || undefined
}

const updateRoleReason = (roleId: string | number, reason: string) => {
  const a = roleAssignments.value.find(a => String(a.roleId) === String(roleId))
  if (a) a.reason = reason || undefined
}

// ==================== 角色分配操作 ====================

const handleAssignRoles = async (row: UserListItem) => {
  if (!row.id) return
  try {
    currentUserId.value = row.id
    currentUserName.value = row.realName || row.username || ''
    const existing = await getUserRoleAssignments(row.id)
    const existingArr = Array.isArray(existing) ? existing : []

    // Build detailed roles table
    const now = new Date().toISOString()
    currentUserRolesDetailed.value = existingArr.map((a: any) => {
      const roleName = a.roleName || allRoles.value.find(r => String(r.id) === String(a.roleId || a.id))?.roleName || '未知角色'
      const roleCode = a.roleCode || allRoles.value.find(r => String(r.id) === String(a.roleId || a.id))?.roleCode || ''
      const scopeName = a.scopeName || (a.scopeType === 'ORG_UNIT' ? findOrgName(a.scopeId) : undefined)
      return {
        roleId: String(a.roleId || a.id),
        roleName,
        roleCode,
        scopeType: a.scopeType || 'ALL',
        scopeId: a.scopeId || 0,
        scopeName,
        expiresAt: a.expiresAt,
        isExpired: a.expiresAt ? a.expiresAt < now : false,
        assignedAt: a.assignedAt,
        reason: a.reason
      }
    })

    roleAssignments.value = existingArr.map((a: any) => ({
      roleId: String(a.roleId || a.id),
      scopeType: a.scopeType || 'ALL',
      scopeId: a.scopeId || 0,
      expiresAt: a.expiresAt ? String(a.expiresAt).substring(0, 10) : undefined,
      reason: a.reason || undefined
    }))
    roleDialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载角色失败')
  }
}

// 根据 orgId 查找组织名称
const findOrgName = (orgId: number | string): string => {
  if (!orgId || orgId === 0) return ''
  const org = flatOrgUnits.value.find(o => o.id === Number(orgId))
  return org?.label?.trim() || ''
}

// 移除单个角色分配
const handleRemoveSingleRole = async (ur: UserRoleDetailed) => {
  if (!currentUserId.value) return
  try {
    await ElMessageBox.confirm(`确定移除角色"${ur.roleName}"吗？`, '移除确认', { type: 'warning' })
    await removeUserRoleWithScope(currentUserId.value, ur.roleId, ur.scopeType, ur.scopeId)
    ElMessage.success('角色已移除')
    // Reload the role dialog
    const row = userList.value.find(u => u.id === currentUserId.value)
    if (row) await handleAssignRoles(row)
    loadUserList()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '移除失败')
  }
}

const handleRoleSubmit = async () => {
  if (!currentUserId.value) return
  // 校验：ORG_UNIT scope 必须选了具体组织
  const invalid = roleAssignments.value.find(a => a.scopeType === 'ORG_UNIT' && (!a.scopeId || a.scopeId === 0))
  if (invalid) {
    const role = allRoles.value.find(r => String(r.id) === String(invalid.roleId))
    ElMessage.error(`角色"${role?.roleName || invalid.roleId}"选择了指定组织但未选择具体组织`)
    return
  }
  try {
    roleSubmitLoading.value = true
    // Include expiresAt and reason in the assignment payload
    const payload = roleAssignments.value.map(a => ({
      roleId: a.roleId,
      scopeType: a.scopeType || 'ALL',
      scopeId: a.scopeId || 0,
      expiresAt: a.expiresAt || undefined,
      reason: a.reason || undefined
    }))
    await assignRoles(currentUserId.value, payload)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    loadUserList()
  } catch (error: any) {
    ElMessage.error(error.message || '分配失败')
  } finally {
    roleSubmitLoading.value = false
  }
}

// handleDataScope 已移除 - 数据权限现在在角色级别配置

onMounted(() => {
  loadUserList()
  loadAllRoles()
  loadUserTypes()
  loadOrgTree()
  loadAllPlaces()
})
</script>

<style>
@import '@/styles/teaching-ui.css';
</style>

<style scoped>
/* ============================================
   UsersView — Page-specific overrides
   ============================================ */

/* Filter bar input sizing */
.filter-input {
  width: 150px;
  flex: none;
  padding: 7px 10px;
}

/* Batch actions bar */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 24px;
  background: #fefce8;
  border-bottom: 1px solid #fde68a;
  font-size: 13px;
}
.batch-info {
  font-size: 12px;
  color: #92400e;
  font-weight: 500;
}

/* Checkbox styling */
.row-checkbox {
  width: 15px;
  height: 15px;
  border-radius: 3px;
  cursor: pointer;
  accent-color: #2563eb;
}

/* Field states in drawer */
.field-disabled {
  background: #f3f4f6 !important;
  color: #9ca3af !important;
  cursor: not-allowed;
}
.field-error {
  border-color: #fca5a5 !important;
  background: #fef2f2 !important;
}
.error-hint {
  margin: 3px 0 0;
  font-size: 11px;
  color: #ef4444;
}

/* Role table (inside drawer) */
.role-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}
.role-table th {
  padding: 7px 10px;
  font-size: 10.5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #6b7280;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  text-align: center;
}
.role-table td {
  padding: 7px 10px;
  font-size: 12px;
  color: #374151;
  border-bottom: 1px solid #f3f4f6;
  text-align: center;
}
.role-table .text-left { text-align: left !important; }
.role-table tbody tr:last-child td { border-bottom: none; }

/* Role hint */
.role-hint {
  padding: 8px 12px;
  margin-bottom: 12px;
  background: #eff6ff;
  border-radius: 6px;
  font-size: 12px;
  color: #1d4ed8;
  line-height: 1.5;
}

/* Role list (card-based) */
.role-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 400px;
  overflow-y: auto;
}
.role-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px 12px;
  transition: all 0.15s;
}
.role-card-active {
  border-color: #93c5fd;
  background: #eff6ff;
}
.role-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.role-card-body {
  margin-top: 10px;
  padding-left: 23px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* Scope row (inside role card) */
.scope-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  flex-wrap: wrap;
}
.scope-label {
  font-size: 11px;
  color: #6b7280;
  flex-shrink: 0;
  width: 50px;
}
.scope-option {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: #374151;
  cursor: pointer;
}
.scope-option input {
  accent-color: #2563eb;
}
.scope-select {
  padding: 4px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 5px;
  font-size: 11px;
  font-family: inherit;
  color: #374151;
  background: #fff;
  outline: none;
  max-width: 200px;
  transition: opacity 0.15s;
}
.scope-date-input {
  padding: 4px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 5px;
  font-size: 11px;
  font-family: inherit;
  color: #374151;
  background: #fff;
  outline: none;
}
.scope-text-input {
  padding: 4px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 5px;
  font-size: 11px;
  font-family: inherit;
  color: #374151;
  background: #fff;
  outline: none;
  width: 140px;
}
</style>
