<template>
  <div class="pm-root">
    <!-- Left Sidebar -->
    <div class="pm-sidebar">
      <PlaceSidebar
        :tree-data="treeData"
        :selected-id="selectedNodeId"
        @select="handleSelectNode"
        @add-root="handleAddRoot"
      />
    </div>

    <!-- Right Content Panel -->
    <div class="pm-main">
      <!-- Top Header Bar -->
      <header class="tm-header">
        <div>
          <h1 class="tm-title">场所管理</h1>
          <div class="tm-stats" style="margin-top: 4px;">管理场地与设施结构</div>
        </div>
        <router-link to="/system/place-types" class="tm-btn tm-btn-secondary" style="text-decoration: none;">
          <Settings style="width: 16px; height: 16px;" />
          类型配置
        </router-link>
      </header>

      <!-- Stat Bar -->
      <div class="tm-stats-bar">
        <span class="tm-stats">总数 <b>{{ stats.totalCount }}</b></span>
        <i class="tm-stats sep" />
        <span class="tm-stats">容量 <b>{{ stats.totalCapacity }}</b></span>
        <i class="tm-stats sep" />
        <span class="tm-stats">占用 <b>{{ stats.totalOccupancy }}</b></span>
        <i class="tm-stats sep" />
        <span class="tm-stats">占用率 <b>{{ stats.occupancyRate?.toFixed(1) || 0 }}%</b></span>
      </div>

      <!-- Main Content Area -->
      <div class="pm-content">
        <!-- Loading State -->
        <div v-if="loading" class="pm-loading">
          <div class="pm-spinner tm-spin"></div>
        </div>

        <!-- Detail Panel (when node selected) -->
        <div v-else-if="selectedNode" class="pm-detail-area">
          <!-- Detail Card -->
          <div class="pm-card">
            <!-- Header (compact) -->
            <div class="pm-card-header">
              <div class="pm-card-left">
                <h2 class="pm-card-name">{{ selectedNode.placeName }}</h2>
                <span class="tm-chip" :class="statusBadgeClass(selectedNode.status)">{{ getStatusLabel(selectedNode.status) }}</span>
                <span v-if="genderLabel(selectedNode)" class="tm-chip" :class="genderBadgeClass(selectedNode)">{{ genderLabel(selectedNode) }}</span>
              </div>
              <div class="pm-card-actions">
                <button class="tm-action" @click="handleEdit(selectedNode)">
                  <Pencil style="width: 12px; height: 12px;" /> 编辑
                </button>
                <button v-if="!selectedNode.leaf" class="tm-action" @click="handleAddChild(selectedNode)">
                  <Plus style="width: 12px; height: 12px;" /> 子场所
                </button>
                <button
                  v-if="selectedNode.occupiable || selectedNode.hasCapacity || selectedNode.leaf || selectedNode.attributes?.layout"
                  class="tm-action"
                  @click="showFloorPlanDialog = true"
                >
                  <MapPin style="width: 12px; height: 12px;" /> 平面图
                </button>
                <div class="pm-dropdown-wrap" ref="dropdownRef">
                  <button class="pm-more-btn" @click="showDropdown = !showDropdown">
                    <MoreHorizontal style="width: 14px; height: 14px;" />
                  </button>
                  <div v-if="showDropdown" class="pm-dropdown">
                    <button v-if="selectedNode.status === 1" class="pm-dropdown-item" @click="handleCommand('maintenance')"><Wrench style="width: 12px; height: 12px;" /> 设为维护中</button>
                    <button v-if="selectedNode.status !== 1" class="pm-dropdown-item" @click="handleCommand('enable')"><CheckCircle style="width: 12px; height: 12px;" /> 恢复正常</button>
                    <button v-if="selectedNode.status === 1" class="pm-dropdown-item pm-dropdown-sep" @click="handleCommand('disable')"><XCircle style="width: 12px; height: 12px;" /> 停用</button>
                    <button class="pm-dropdown-item pm-dropdown-sep pm-dropdown-danger" @click="handleCommand('delete')"><Trash2 style="width: 12px; height: 12px;" /> 删除</button>
                  </div>
                </div>
              </div>
            </div>

          </div>

          <!-- Tabs -->
          <div class="pm-card" style="margin-top: 12px;">
            <div class="tm-tabs" style="padding: 0 20px;">
              <button
                v-for="tab in placeTabs"
                :key="tab.key"
                class="tm-tab"
                :class="{ active: activePlaceTab === tab.key }"
                @click="activePlaceTab = tab.key"
              >
                {{ tab.label }}
                <span
                  v-if="tab.count !== undefined && tab.count > 0"
                  class="pm-tab-count"
                >{{ tab.count }}</span>
              </button>
            </div>

            <!-- Tab: 子场所 -->
            <div v-if="activePlaceTab === 'children'">
              <div v-if="childPlaces.length > 0" class="pm-child-grid">
                <button
                  v-for="child in childPlaces"
                  :key="child.id"
                  class="pm-child-chip"
                  @click="selectPlace(child)"
                >
                  <span class="pm-child-name">{{ child.placeName }}</span>
                  <span class="pm-child-type">{{ child.typeName }}</span>
                  <span v-if="child.capacity" class="pm-child-cap">{{ child.currentOccupancy || 0 }}/{{ child.capacity }}</span>
                </button>
              </div>
              <div v-else class="pm-empty-hint">暂无子场所</div>
            </div>

            <!-- Tab: 入住管理 -->
            <div v-if="activePlaceTab === 'occupants' && showOccupantPanel">
              <div class="pm-tab-toolbar">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <span v-if="selectedNode?.capacity" style="font-size: 11px; color: #9ca3af;">
                    容量 <span style="font-weight: 500; color: #4b5563;">{{ occupants.length }}/{{ selectedNode.capacity }}</span>{{ selectedNode.capacityUnit ? selectedNode.capacityUnit : '人' }}
                  </span>
                  <!-- View mode toggle -->
                  <div v-if="selectedNode?.capacity" class="pm-view-toggle">
                    <button
                      class="pm-toggle-btn"
                      :class="{ active: occupantViewMode === 'list' }"
                      @click="occupantViewMode = 'list'"
                    >列表</button>
                    <button
                      class="pm-toggle-btn"
                      :class="{ active: occupantViewMode === 'grid' }"
                      @click="occupantViewMode = 'grid'"
                    >网格</button>
                    <button
                      v-if="selectedNode?.attributes?.layout"
                      class="pm-toggle-btn"
                      :class="{ active: occupantViewMode === 'floor' }"
                      @click="occupantViewMode = 'floor'"
                    >平面</button>
                  </div>
                </div>
                <button class="tm-btn tm-btn-primary" style="padding: 4px 10px; font-size: 11px;" @click="openCheckInDialog">
                  <Plus style="width: 12px; height: 12px;" /> 入住
                </button>
              </div>

              <!-- Floor Plan View -->
              <div v-if="occupantViewMode === 'floor' && selectedNode?.attributes?.layout" class="pm-section-border" style="height: 400px;">
                <FloorPlanEditor
                  :place-id="selectedNode.id"
                  :initial-layout="selectedNode.attributes.layout"
                  :occupants="occupants"
                  mode="view"
                />
              </div>

              <!-- Grid View -->
              <div v-else-if="occupantViewMode === 'grid' && selectedNode?.capacity" class="pm-section-border">
                <SeatGrid
                  :capacity="selectedNode.capacity"
                  :occupants="occupants"
                  :capacity-unit="selectedNode.capacityUnit"
                  @check-in="(posNo) => { openCheckInDialog(); }"
                  @select="(occ) => startSwap(occ)"
                />
              </div>

              <!-- Occupant Table (list view) -->
              <div v-else class="pm-section-border">
                <table v-if="occupants.length > 0" class="tm-table" style="table-layout: auto;">
                  <thead>
                    <tr>
                      <th class="text-left">位置</th>
                      <th class="text-left">姓名</th>
                      <th class="text-left">类型</th>
                      <th class="text-left">组织</th>
                      <th class="text-left">入住日期</th>
                      <th class="text-right">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="occ in occupants" :key="occ.id">
                      <td class="text-left" style="font-weight: 500;">{{ occ.positionNo || '-' }}</td>
                      <td class="text-left" style="color: #111827;">{{ occ.occupantName || '-' }}</td>
                      <td class="text-left" style="color: #9ca3af; white-space: nowrap;">{{ occupantTypeLabel(occ.occupantType) }}</td>
                      <td class="text-left" style="color: #6b7280;">{{ occ.orgUnitName || '-' }}</td>
                      <td class="text-left" style="color: #9ca3af; white-space: nowrap;">{{ formatDate(occ.checkInTime) }}</td>
                      <td class="text-right" style="white-space: nowrap;">
                        <button class="tm-action tm-action-danger" @click="handleCheckOut(occ)">退出</button>
                        <button v-if="occupants.length > 1" class="tm-action" style="color: #3b82f6;" @click="startSwap(occ)">交换</button>
                      </td>
                    </tr>
                  </tbody>
                </table>
                <div v-else class="pm-empty-hint" style="padding: 24px 0;">暂无入住记录</div>
              </div>

              <!-- Empty slots hint -->
              <div v-if="selectedNode?.capacity && occupants.length < selectedNode.capacity" class="pm-hint-bar">
                剩余 {{ selectedNode.capacity - occupants.length }} 个空位
              </div>

              <!-- History toggle -->
              <div class="pm-hint-bar">
                <button class="pm-link-btn" @click="toggleHistory">
                  {{ showHistory ? '收起历史记录' : '查看历史记录' }}
                </button>
              </div>

              <!-- History Table -->
              <div v-if="showHistory" class="pm-section-border">
                <table v-if="occupantHistory.length > 0" class="tm-table">
                  <colgroup>
                    <col style="width: 55px" />
                    <col style="width: 85px" />
                    <col style="width: 75px" />
                    <col style="width: 65px" />
                    <col />
                    <col style="width: 45px" />
                    <col style="width: 85px" />
                    <col style="width: 85px" />
                    <col style="width: 60px" />
                  </colgroup>
                  <thead>
                    <tr>
                      <th class="text-left">位置</th>
                      <th class="text-left">姓名</th>
                      <th class="text-left">类型</th>
                      <th class="text-left">组织</th>
                      <th class="text-left">性别</th>
                      <th class="text-left">入住时间</th>
                      <th class="text-left">退出时间</th>
                      <th class="text-left">状态</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="occ in occupantHistory" :key="occ.id">
                      <td class="text-left" style="color: #6b7280;">{{ occ.positionNo || '-' }}</td>
                      <td class="text-left" style="color: #374151;">{{ occ.occupantName || '-' }}</td>
                      <td class="text-left" style="color: #9ca3af;">{{ occupantTypeLabel(occ.occupantType) }}</td>
                      <td class="text-left" style="color: #9ca3af;">{{ occ.orgUnitName || '-' }}</td>
                      <td class="text-left">
                        <span v-if="occ.gender === 1" style="color: #3b82f6;">男</span>
                        <span v-else-if="occ.gender === 2" style="color: #ec4899;">女</span>
                        <span v-else style="color: #d1d5db;">-</span>
                      </td>
                      <td class="text-left" style="color: #9ca3af;">{{ formatDate(occ.checkInTime) }}</td>
                      <td class="text-left" style="color: #9ca3af;">{{ occ.checkOutTime ? formatDate(occ.checkOutTime) : '-' }}</td>
                      <td class="text-left">
                        <span :style="{ color: occ.status === 1 ? '#059669' : '#9ca3af' }">
                          {{ occ.status === 1 ? '在住' : '已退出' }}
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
                <div v-else class="pm-empty-hint">暂无历史记录</div>
              </div>
            </div>

            <!-- Tab: 关系 -->
            <div v-if="activePlaceTab === 'relations'" style="padding: 12px 16px;">
              <RelationsPanel
                entity-type="place"
                :entity-id="selectedNode.id"
                :resource-capacity="selectedNode.capacity"
                :resource-subtype="selectedNode.typeCode"
                :show-header="false"
              />
            </div>

            <!-- Tab: 基本信息 (系统字段) -->
            <div v-if="activePlaceTab === 'info'" class="pm-info-grid">
              <div>
                <dt class="pm-info-label">编号</dt>
                <dd class="pm-info-value"><code class="tm-code">{{ selectedNode.placeCode }}</code></dd>
              </div>
              <div>
                <dt class="pm-info-label">类型</dt>
                <dd class="pm-info-value">{{ selectedNode.typeName }}</dd>
              </div>
              <div>
                <dt class="pm-info-label">层级</dt>
                <dd class="pm-info-value">第{{ selectedNode.level }}级</dd>
              </div>
              <div>
                <dt class="pm-info-label">上级</dt>
                <dd class="pm-info-value">{{ selectedNodeParentName || '根节点' }}</dd>
              </div>
              <div class="pm-info-span2">
                <dt class="pm-info-label">所属部门</dt>
                <dd class="pm-info-value">{{ selectedNode.orgUnitName || selectedNode.effectiveOrgUnitName || '-' }}</dd>
              </div>
              <div class="pm-info-span2">
                <dt class="pm-info-label">负责人</dt>
                <dd class="pm-info-value">{{ selectedNode.responsibleUserName || selectedNode.effectiveResponsibleUserName || '-' }}</dd>
              </div>
              <div>
                <dt class="pm-info-label">性别限制</dt>
                <dd class="pm-info-value">{{ genderDisplay(selectedNode) }}</dd>
              </div>
              <div>
                <dt class="pm-info-label">容量</dt>
                <dd class="pm-info-value">{{ selectedNode.hasCapacity ? `${selectedNode.currentOccupancy || 0}/${selectedNode.capacity || '-'}` : '不适用' }}</dd>
              </div>
              <div class="pm-info-span2">
                <dt class="pm-info-label">路径</dt>
                <dd class="pm-info-value">{{ selectedNodePath }}</dd>
              </div>
              <div v-if="selectedNode.description" class="pm-info-span4">
                <dt class="pm-info-label">描述</dt>
                <dd class="pm-info-value" style="color: #6b7280;">{{ selectedNode.description }}</dd>
              </div>
            </div>

            <!-- Tab: 类型插件扩展字段分组 (每个 group 一个独立 tab) -->
            <div
              v-for="group in extensionGroups"
              :key="group.key"
              v-show="activePlaceTab === group.key"
              class="pm-ext-tab-body"
            >
              <DynamicForm
                :schema="{ fields: group.fields }"
                v-model="extensionAttrs"
                :disabled="true"
              />
            </div>

            <!-- Tab: 操作记录 -->
            <div v-if="activePlaceTab === 'logs'">
              <ActivityTimeline
                resourceType="PLACE"
                :resourceId="selectedNode.id"
                :limit="30"
                title="操作记录"
              />
            </div>

          </div><!-- end tabs card -->
        </div>

        <!-- Overview (when no node selected) -->
        <div v-else style="display: flex; flex-direction: column; gap: 16px;">
          <PlaceOverview
            :tree-data="treeData"
            :statistics="statistics"
          />
        </div>
      </div>
    </div>

    <!-- Form Dialog -->
    <PlaceFormDrawer
      v-model:visible="formDialogVisible"
      :mode="formMode"
      :parent-place="formParentPlace"
      :edit-data="formEditData"
      :allowed-types="formAllowedTypes"
      @success="handleFormSuccess"
    />

    <!-- Check-in Dialog -->
    <el-dialog v-model="showCheckInDialog" width="720px" :close-on-click-modal="false" destroy-on-close align-center class="ci-dialog">
      <template #header>
        <div class="ci-header">
          <div class="ci-header-left">
            <span class="ci-header-title">入住登记</span>
            <span class="ci-header-place">{{ selectedNode?.placeName }}</span>
            <span class="ci-header-type">{{ selectedNode?.typeName }}</span>
          </div>
          <div class="ci-header-right">
            <span v-if="selectedNode?.capacity" class="ci-header-cap">{{ selectedNode?.currentOccupancy || 0 }}<span class="ci-header-cap-sep">/</span>{{ selectedNode?.capacity }}</span>
            <span v-if="selectedNode?.capacity" class="ci-header-remain" :class="ciRemaining > 0 ? 'ci-remain-ok' : 'ci-remain-full'">剩余 {{ ciRemaining }}</span>
            <span v-if="genderLabel(selectedNode!)" class="ci-header-gender">{{ genderLabel(selectedNode!) }}</span>
          </div>
        </div>
      </template>

      <!-- Search + Batch Actions -->
      <div class="ci-actions">
        <el-select
          v-model="ciSearchUserId"
          filterable remote reserve-keyword clearable
          placeholder="输入姓名或账号搜索"
          :remote-method="handleUserSearch"
          :loading="userSearchLoading"
          class="ci-search"
          popper-class="ci-user-popper"
          @change="handleAddUserFromSearch"
        >
          <el-option v-for="user in userSearchList" :key="user.id" :label="`${user.realName} (${user.username})`" :value="user.id">
            <div class="ci-opt">
              <span class="ci-opt-name">{{ user.realName }}</span>
              <span class="ci-opt-user">{{ user.username }}</span>
              <span class="ci-opt-org">{{ user.orgUnitName || '-' }}</span>
              <span class="ci-opt-gender" :style="{ color: user.gender === 1 ? '#3b82f6' : user.gender === 2 ? '#ec4899' : '#d1d5db' }">{{ user.gender === 1 ? '男' : user.gender === 2 ? '女' : '-' }}</span>
            </div>
          </el-option>
        </el-select>
        <el-popover :visible="showOrgPicker" placement="bottom-end" :width="300" trigger="click">
          <template #reference>
            <button class="ci-batch-btn" @click="showOrgPicker = !showOrgPicker">
              <Users style="width: 14px; height: 14px;" /> 批量添加
            </button>
          </template>
          <div style="display: flex; flex-direction: column; gap: 8px; padding: 4px;">
            <el-tree-select v-model="ciBatchOrgId" :data="ciOrgTreeData" :props="{ label: 'unitName', value: 'id', children: 'children' }" placeholder="选择组织/班级" filterable check-strictly style="width: 100%" />
            <div style="display: flex; align-items: center; justify-content: flex-end; gap: 8px;">
              <span v-if="ciBatchOrgId" style="font-size: 12px; color: #9ca3af;">加载该组织下所有用户</span>
              <el-button type="primary" size="small" :disabled="!ciBatchOrgId" :loading="ciBatchLoading" @click="handleBatchAddByOrg">确定</el-button>
            </div>
          </div>
        </el-popover>
      </div>

      <!-- User List -->
      <div class="ci-list">
        <div class="ci-list-header">
          <span>已添加 <b>{{ ciPendingUsers.length }}</b> 人</span>
          <div style="display: flex; align-items: center; gap: 8px;">
            <button v-if="ciPendingUsers.length > 0" class="ci-auto-btn" @click="autoAssignPositions">自动分配位置</button>
            <button v-if="ciPendingUsers.length > 0" class="ci-clear-btn" @click="ciPendingUsers = []">清空</button>
          </div>
        </div>
        <div v-if="ciPendingUsers.length > 0" class="ci-list-body">
          <div class="ci-list-thead">
            <span class="ci-c-idx">#</span>
            <span class="ci-c-name">姓名</span>
            <span class="ci-c-user">账号</span>
            <span class="ci-c-org">部门</span>
            <span class="ci-c-gender">性别</span>
            <span class="ci-c-pos">位置号</span>
            <span class="ci-c-act"></span>
          </div>
          <div v-for="(pu, idx) in ciPendingUsers" :key="pu.userId" class="ci-list-row">
            <span class="ci-c-idx">{{ idx + 1 }}</span>
            <span class="ci-c-name ci-text-bold">{{ pu.realName }}</span>
            <span class="ci-c-user ci-text-dim">{{ pu.username }}</span>
            <span class="ci-c-org ci-text-dim">{{ pu.orgUnitName || '-' }}</span>
            <span class="ci-c-gender" :style="{ color: pu.gender === 1 ? '#3b82f6' : pu.gender === 2 ? '#ec4899' : '#9ca3af' }">{{ pu.gender === 1 ? '男' : pu.gender === 2 ? '女' : '-' }}</span>
            <span class="ci-c-pos">
              <select v-model="pu.positionNo" class="ci-pos-select">
                <option v-for="pos in getAvailablePositionsFor(pu.positionNo)" :key="pos" :value="pos">{{ pos }}</option>
              </select>
            </span>
            <span class="ci-c-act">
              <button class="ci-rm-btn" @click="ciPendingUsers.splice(idx, 1)"><XCircle style="width: 14px; height: 14px;" /></button>
            </span>
          </div>
        </div>
        <div v-else class="ci-list-empty">搜索或批量添加用户到入住列表</div>
      </div>

      <!-- Remark -->
      <input v-model="ciRemark" class="ci-remark" placeholder="备注（选填）" maxlength="200" />

      <template #footer>
        <div class="ci-footer">
          <el-button @click="showCheckInDialog = false">取消</el-button>
          <el-button type="primary" :loading="checkInLoading" :disabled="ciPendingUsers.length === 0" @click="handleBatchCheckIn">
            确认入住 ({{ ciPendingUsers.length }}人)
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Swap Dialog -->
    <el-dialog v-model="showSwapDialog" :title="' '" width="560px" :close-on-click-modal="false" class="sw-dialog" destroy-on-close align-center>
      <template #header><span /></template>
      <div class="sw-split">
        <div class="sw-left">
          <div class="sw-avatar"><Users style="width: 16px; height: 16px; color: #7c3aed;" /></div>
          <div class="sw-name">{{ swapSource?.occupantName || '-' }}</div>
          <div class="sw-tag">{{ occupantTypeLabel(swapSource?.occupantType) }}</div>
          <div class="sw-divider" />
          <div class="sw-rows">
            <div class="sw-row"><span class="sw-k">位置</span><span class="sw-v">{{ swapSource?.positionNo || '-' }}</span></div>
            <div v-if="swapSource?.checkInTime" class="sw-row"><span class="sw-k">入住</span><span class="sw-v">{{ formatDate(swapSource.checkInTime) }}</span></div>
          </div>
        </div>
        <div class="sw-right">
          <div class="sw-hd">选择交换对象</div>
          <div class="sw-list">
            <button v-for="occ in swapTargets" :key="occ.id" class="sw-card" @click="handleSwap(occ)">
              <div><span class="sw-card-name">{{ occ.occupantName }}</span><span class="sw-card-pos">位置 {{ occ.positionNo || '-' }}</span></div>
              <span class="sw-card-act">选择</span>
            </button>
          </div>
          <div v-if="swapTargets.length === 0" class="sw-empty">暂无可交换人员</div>
        </div>
      </div>
      <template #footer>
        <div class="sw-footer"><el-button @click="showSwapDialog = false">取消</el-button></div>
      </template>
    </el-dialog>

    <!-- Floor Plan Editor Dialog (fullscreen) -->
    <el-dialog
      v-model="showFloorPlanDialog"
      fullscreen
      destroy-on-close
      :show-close="false"
      class="fp-dialog"
    >
      <template #header>
        <div class="fp-header-bar">
          <div style="display: flex; align-items: center; gap: 8px;">
            <MapPin style="width: 16px; height: 16px; color: #6b7280;" />
            <span style="font-size: 13px; font-weight: 600; color: #1f2937;">平面图编辑 - {{ selectedNode?.placeName }}</span>
          </div>
          <div style="display: flex; align-items: center; gap: 8px;">
            <button class="tm-btn tm-btn-secondary" style="padding: 6px 12px; font-size: 12px;" @click="showFloorPlanDialog = false">取消</button>
            <button
              class="tm-btn tm-btn-primary"
              style="padding: 6px 12px; font-size: 12px;"
              :style="floorPlanSaving ? { opacity: '0.5', cursor: 'not-allowed' } : {}"
              :disabled="floorPlanSaving"
              @click="handleFloorPlanSave"
            >{{ floorPlanSaving ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </template>
      <div style="height: calc(100vh - 50px);">
        <FloorPlanEditor
          v-if="selectedNode"
          ref="floorPlanEditorRef"
          :place-id="selectedNode.id"
          :initial-layout="selectedNode.attributes?.layout || null"
          :occupants="occupants"
          mode="edit"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Users, Settings, MapPin,
  Plus, Pencil, MoreHorizontal, Wrench, CheckCircle,
  XCircle, Trash2
} from 'lucide-vue-next'
import PlaceSidebar from './components/PlaceSidebar.vue'
import PlaceOverview from './components/PlaceOverview.vue'
import PlaceFormDrawer from './components/PlaceFormDrawer.vue'
import SeatGrid from './components/SeatGrid.vue'
import FloorPlanEditor from './components/FloorPlanEditor.vue'
import ActivityTimeline from '@/components/activity/ActivityTimeline.vue'
import RelationsPanel from '@/components/access/RelationsPanel.vue'
import DynamicForm from '@/components/extension/DynamicForm.vue'
import { universalPlaceApi, type PlaceStatistics } from '@/api/universalPlace'
import { entityTypeApi } from '@/api/entityType'
import { getSimpleUserList, getUsersByOrgUnit } from '@/api/user'
import { getOrgUnitTree } from '@/api/organization'
import type { SimpleUser, User } from '@/types/user'
import type { OrgUnitTreeNode } from '@/types'
import type { PlaceTreeNode, UniversalPlace, UniversalPlaceType, PlaceOccupant } from '@/types/universalPlace'

// ========== Data ==========
const loading = ref(false)
const treeData = ref<PlaceTreeNode[]>([])
const selectedNodeId = ref<LongId | null>(null)
const childPlaces = ref<UniversalPlace[]>([])
const statistics = ref<PlaceStatistics | null>(null)
const showDropdown = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)
const activePlaceTab = ref<string>('info')

// ========== Place Types (for schema lookups) ==========
const allPlaceTypes = ref<UniversalPlaceType[]>([])

// ========== Form ==========
const formDialogVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const formParentPlace = ref<PlaceTreeNode | null>(null)
const formEditData = ref<PlaceTreeNode | null>(null)
const formAllowedTypes = ref<UniversalPlaceType[]>([])

// ========== Occupant Management ==========
const occupants = ref<PlaceOccupant[]>([])
const occupantHistory = ref<PlaceOccupant[]>([])
const showHistory = ref(false)
const showCheckInDialog = ref(false)
const checkInLoading = ref(false)
const occupantViewMode = ref<'list' | 'grid' | 'floor'>('list')

// ========== Floor Plan ==========
const showFloorPlanDialog = ref(false)
const floorPlanSaving = ref(false)
const floorPlanEditorRef = ref<InstanceType<typeof FloorPlanEditor> | null>(null)

// New batch check-in state
interface PendingUser {
  userId: LongId | string
  username: string
  realName: string
  userType?: string
  orgUnitName?: string
  gender?: number
  occupantType: string
  positionNo: string
}
const ciPendingUsers = ref<PendingUser[]>([])
const ciRemark = ref('')
const ciSearchUserId = ref<LongId | undefined>(undefined)
const userSearchList = ref<SimpleUser[]>([])
const userSearchLoading = ref(false)
const showOrgPicker = ref(false)
const ciOrgTreeData = ref<OrgUnitTreeNode[]>([])
const ciBatchOrgId = ref<LongId | undefined>(undefined)
const ciBatchLoading = ref(false)

const ciRemaining = computed(() => {
  const node = selectedNode.value
  if (!node?.capacity) return 0
  return Math.max(0, node.capacity - (node.currentOccupancy || 0) - ciPendingUsers.value.length)
})

/** 已占用的位置号集合（当前在住 + 待入住列表中已分配的） */
const occupiedPositions = computed(() => {
  const set = new Set<string>()
  for (const occ of occupants.value) {
    if (occ.positionNo) set.add(occ.positionNo)
  }
  for (const pu of ciPendingUsers.value) {
    if (pu.positionNo) set.add(pu.positionNo)
  }
  return set
})

/** 格式化位置号为两位 */
function fmtPos(n: number): string {
  return String(n).padStart(2, '0')
}

/** 获取下一个可用位置号 */
function getNextAvailablePosition(): string {
  const cap = selectedNode.value?.capacity || 99
  for (let i = 1; i <= cap; i++) {
    const pos = fmtPos(i)
    if (!occupiedPositions.value.has(pos)) return pos
  }
  return ''
}

/** 获取某个用户可选的位置列表（包含自己当前已选的） */
function getAvailablePositionsFor(currentPos: string): string[] {
  const cap = selectedNode.value?.capacity || 99
  const result: string[] = []
  for (let i = 1; i <= cap; i++) {
    const pos = fmtPos(i)
    if (pos === currentPos || !occupiedPositions.value.has(pos)) {
      result.push(pos)
    }
  }
  return result
}

function autoAssignPositions() {
  const cap = selectedNode.value?.capacity || 99
  const usedPositions = new Set<string>()
  // Include currently occupied positions
  for (const occ of occupants.value) {
    if (occ.positionNo) usedPositions.add(occ.positionNo)
  }
  // Include already-assigned positions in pending list
  for (const pu of ciPendingUsers.value) {
    if (pu.positionNo) usedPositions.add(pu.positionNo)
  }
  let nextPos = 1
  for (const user of ciPendingUsers.value) {
    if (!user.positionNo) {
      while (usedPositions.has(fmtPos(nextPos)) && nextPos <= cap) nextPos++
      if (nextPos <= cap) {
        user.positionNo = fmtPos(nextPos)
        usedPositions.add(fmtPos(nextPos))
        nextPos++
      }
    }
  }
}

function userTypeToOccupantType(userType?: string): string {
  if (!userType) return 'OTHER'
  if (userType === '学生') return 'STUDENT'
  if (userType === '教师') return 'TEACHER'
  if (userType === '管理员') return 'ADMIN'
  return 'OTHER'
}

async function handleUserSearch(keyword: string) {
  if (!keyword || keyword.length < 1) {
    userSearchList.value = []
    return
  }
  userSearchLoading.value = true
  try {
    userSearchList.value = await getSimpleUserList(keyword) as SimpleUser[]
  } catch {
    userSearchList.value = []
  } finally {
    userSearchLoading.value = false
  }
}

function handleAddUserFromSearch(userId: LongId) {
  if (!userId) return
  const user = userSearchList.value.find(u => String(u.id) === String(userId))
  if (!user) return
  // Prevent duplicate
  if (ciPendingUsers.value.some(p => String(p.userId) === String(user.id))) {
    ElMessage.warning(`${user.realName} 已在列表中`)
    ciSearchUserId.value = undefined
    return
  }
  ciPendingUsers.value.push({
    userId: user.id,
    username: user.username,
    realName: user.realName,
    userType: user.userType,
    orgUnitName: user.orgUnitName,
    gender: user.gender,
    occupantType: userTypeToOccupantType(user.userType),
    positionNo: getNextAvailablePosition()
  })
  ciSearchUserId.value = undefined
  userSearchList.value = []
}

async function handleBatchAddByOrg() {
  if (!ciBatchOrgId.value) return
  ciBatchLoading.value = true
  try {
    const users = await getUsersByOrgUnit(ciBatchOrgId.value) as User[]
    let added = 0
    for (const user of users) {
      if (!ciPendingUsers.value.some(p => String(p.userId) === String(user.id))) {
        ciPendingUsers.value.push({
          userId: user.id,
          username: user.username,
          realName: user.realName,
          userType: user.userType,
          orgUnitName: user.orgUnitName,
          gender: user.gender,
          occupantType: userTypeToOccupantType(user.userType),
          positionNo: getNextAvailablePosition()
        })
        added++
      }
    }
    if (added > 0) {
      ElMessage.success(`已添加 ${added} 人`)
    } else if (users.length === 0) {
      ElMessage.warning('该组织下没有用户')
    } else {
      ElMessage.info('所有用户已在列表中')
    }
    showOrgPicker.value = false
    ciBatchOrgId.value = undefined
  } catch {
    ElMessage.error('加载用户失败')
  } finally {
    ciBatchLoading.value = false
  }
}

const showSwapDialog = ref(false)
const swapSource = ref<PlaceOccupant | null>(null)

// 预订子系统已移除 (2026-06-13): 前端 UI 完整但后端零端点 (7 API 全 404),
// 且无 place 类型开启 bookable → 不可达死代码。详见删除 commit。

function formatDateTime(dateStr?: string) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}


// ========== Computed ==========
const findNode = (nodes: PlaceTreeNode[], id: LongId | string): PlaceTreeNode | null => {
  for (const node of nodes) {
    if (String(node.id) === String(id)) return node
    if (node.children) {
      const found = findNode(node.children, id)
      if (found) return found
    }
  }
  return null
}

// Build path from tree by walking ancestors
const buildPath = (nodes: PlaceTreeNode[], targetId: LongId | string, ancestors: string[] = []): string[] | null => {
  for (const node of nodes) {
    const current = [...ancestors, node.placeName]
    if (String(node.id) === String(targetId)) return current
    if (node.children) {
      const found = buildPath(node.children, targetId, current)
      if (found) return found
    }
  }
  return null
}

const selectedNodePath = computed(() => {
  if (!selectedNodeId.value) return ''
  const parts = buildPath(treeData.value, selectedNodeId.value)
  return parts ? parts.join(' / ') : selectedNode.value?.placeName || ''
})

const selectedNodeParentName = computed(() => {
  const node = selectedNode.value
  if (!node) return ''
  if (node.parentName) return node.parentName
  if (!node.parentId) return ''
  const parent = findNode(treeData.value, node.parentId)
  return parent?.placeName || ''
})

const selectedNode = computed(() => {
  if (!selectedNodeId.value) return null
  return findNode(treeData.value, selectedNodeId.value)
})

const stats = computed(() => statistics.value || {
  totalCount: 0,
  totalCapacity: 0,
  totalOccupancy: 0,
  occupancyRate: 0
})

/**
 * 扩展字段分组 (entity_type_configs.metadataSchema > 按 field.group 分组)
 * 在"基本信息"tab 底部以只读 DynamicForm 形式渲染
 */
const extensionSchema = computed<{ fields: any[] } | null>(() => {
  const node = selectedNode.value
  if (!node) return null
  const cfg = allPlaceTypes.value.find(t => t.typeCode === node.typeCode)
  if (!cfg?.metadataSchema) return null
  const schema = typeof cfg.metadataSchema === 'string'
    ? (() => { try { return JSON.parse(cfg.metadataSchema as any) } catch { return null } })()
    : cfg.metadataSchema
  return (schema as any)?.fields?.length ? schema : null
})

const extensionAttrs = computed({
  get: () => selectedNode.value?.attributes || {},
  set: () => { /* disabled, ignored */ }
})

// 与系统 tab 重名的 group 自动加类型前缀(如"宿舍·基本信息")
const PM_SYSTEM_TAB_LABELS = new Set(['基本信息', '子场所', '入住管理', '预订管理', '关系', '操作记录'])
const extensionGroups = computed(() => {
  const fields = extensionSchema.value?.fields || []
  const visible = fields.filter((f: any) => f.showInDetail !== false)
  const groups = new Map<string, any[]>()
  for (const f of visible) {
    const g = f.group || '扩展属性'
    if (!groups.has(g)) groups.set(g, [])
    groups.get(g)!.push(f)
  }
  const typePrefix = selectedNode.value?.typeName || '扩展'
  return Array.from(groups.entries()).map(([name, fs], i) => ({
    key: `ext_${i}`,
    label: PM_SYSTEM_TAB_LABELS.has(name) ? `${typePrefix}·${name}` : name,
    fields: fs.sort((a: any, b: any) => (a.sortOrder || 0) - (b.sortOrder || 0))
  }))
})

const showOccupantPanel = computed(() => {
  const node = selectedNode.value
  if (!node) return false
  return node.occupiable || node.hasCapacity
})

const placeTabs = computed(() => {
  const tabs: { key: string; label: string; count?: number }[] = [
    { key: 'info', label: '基本信息' },
    { key: 'children', label: '子场所', count: childPlaces.value.length },
  ]
  if (showOccupantPanel.value) {
    tabs.push({ key: 'occupants', label: '入住管理', count: occupants.value.length })
  }
  // 类型插件声明的扩展字段按 group 追加独立 tab
  for (const group of extensionGroups.value) {
    tabs.push({ key: group.key, label: group.label })
  }
  tabs.push({ key: 'relations', label: '关系' })
  tabs.push({ key: 'logs', label: '操作记录' })
  return tabs
})

const swapTargets = computed(() => {
  if (!swapSource.value) return []
  return occupants.value.filter(o => o.id !== swapSource.value!.id)
})


// ========== Load Data ==========
async function loadData() {
  loading.value = true
  try {
    treeData.value = await universalPlaceApi.getTree()
  } catch {
    ElMessage.error('加载场所数据失败')
  } finally {
    loading.value = false
  }
}

async function loadPlaceTypes() {
  try {
    const result = await entityTypeApi.list('PLACE')
    allPlaceTypes.value = ((result as any).data || result || []) as UniversalPlaceType[]
  } catch {
    // 非关键错误
  }
}

async function loadStatistics() {
  try {
    statistics.value = await universalPlaceApi.getStatistics()
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

async function loadChildPlaces(parentId: LongId | string) {
  try {
    childPlaces.value = await universalPlaceApi.getChildren(parentId)
  } catch {
    childPlaces.value = []
  }
}

// ========== Event Handlers ==========
function handleSelectNode(node: PlaceTreeNode) {
  selectedNodeId.value = node.id
  showHistory.value = false
  occupants.value = []
  occupantHistory.value = []
  loadChildPlaces(node.id)
  // Load occupants if this node supports them
  if (node.occupiable || node.hasCapacity) {
    loadOccupants(node.id)
  }
}

function selectPlace(place: UniversalPlace) {
  const node = findNode(treeData.value, place.id)
  if (node) {
    selectedNodeId.value = node.id
    showHistory.value = false
    occupants.value = []
    occupantHistory.value = []
    loadChildPlaces(place.id)
    if (node.occupiable || node.hasCapacity) {
      loadOccupants(place.id)
    }
  }
}

async function handleAddRoot() {
  formMode.value = 'create'
  formParentPlace.value = null
  formEditData.value = null
  try {
    formAllowedTypes.value = await universalPlaceApi.getAllowedChildTypesForRoot()
    formDialogVisible.value = true
  } catch {
    ElMessage.error('获取可用类型失败')
  }
}

async function handleAddChild(parent: PlaceTreeNode) {
  formMode.value = 'create'
  formParentPlace.value = parent
  formEditData.value = null
  try {
    formAllowedTypes.value = await universalPlaceApi.getAllowedChildTypes(parent.id)
    if (formAllowedTypes.value.length === 0) {
      ElMessage.warning('该场所类型不允许创建子场所')
      return
    }
    formDialogVisible.value = true
  } catch {
    ElMessage.error('获取可用类型失败')
  }
}

function handleEdit(place: PlaceTreeNode) {
  formMode.value = 'edit'
  formParentPlace.value = null
  formEditData.value = place
  // 传入所有类型供编辑模式获取 schema
  formAllowedTypes.value = allPlaceTypes.value
  formDialogVisible.value = true
}

function handleFormSuccess() {
  loadData()
  loadStatistics()
  if (selectedNode.value) {
    loadChildPlaces(selectedNode.value.id)
  }
}

// ========== Status & More Actions ==========
function handleCommand(command: string) {
  showDropdown.value = false
  if (!selectedNode.value) return

  switch (command) {
    case 'maintenance':
      changeStatus(selectedNode.value.id, 2)
      break
    case 'enable':
      changeStatus(selectedNode.value.id, 1)
      break
    case 'disable':
      changeStatus(selectedNode.value.id, 0)
      break
    case 'delete':
      handleDelete(selectedNode.value)
      break
  }
}

async function changeStatus(id: LongId | string, status: number) {
  try {
    await universalPlaceApi.changeStatus(id, status)
    ElMessage.success('状态更新成功')
    loadData()
    loadStatistics()
  } catch { /* axios 拦截器已处理 */ }
}

async function handleDelete(place: PlaceTreeNode) {
  try {
    await ElMessageBox.confirm(
      `确定要删除场所"${place.placeName}"吗？此操作不可恢复。`,
      '确认删除',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await universalPlaceApi.delete(place.id)
    ElMessage.success('删除成功')
    selectedNodeId.value = null
    childPlaces.value = []
    loadData()
    loadStatistics()
  } catch (error: any) {
    if (error !== 'cancel') { /* axios 拦截器已处理 */ }
  }
}

// ========== Occupant Methods ==========
async function loadOccupants(placeId: LongId | string) {
  try {
    occupants.value = await universalPlaceApi.getOccupants(placeId)
  } catch {
    occupants.value = []
  }
}

async function loadOccupantHistory(placeId: LongId | string) {
  try {
    occupantHistory.value = await universalPlaceApi.getOccupantHistory(placeId)
  } catch {
    occupantHistory.value = []
  }
}

async function openCheckInDialog() {
  ciPendingUsers.value = []
  ciRemark.value = ''
  ciSearchUserId.value = undefined
  userSearchList.value = []
  showOrgPicker.value = false
  ciBatchOrgId.value = undefined
  // Load org tree for batch picker (lazy load once)
  if (ciOrgTreeData.value.length === 0) {
    try {
      ciOrgTreeData.value = await getOrgUnitTree()
    } catch { /* non-critical */ }
  }
  showCheckInDialog.value = true
}

async function handleBatchCheckIn() {
  if (!selectedNode.value || ciPendingUsers.value.length === 0) return

  checkInLoading.value = true
  try {
    const requests = ciPendingUsers.value.map((pu, idx) => ({
      occupantType: pu.occupantType,
      occupantId: pu.userId,
      occupantName: pu.realName,
      username: pu.username,
      orgUnitName: pu.orgUnitName,
      gender: pu.gender,
      positionNo: pu.positionNo,
      remark: ciRemark.value || undefined
    }))

    if (requests.length === 1) {
      await universalPlaceApi.checkIn(selectedNode.value.id, requests[0])
    } else {
      await universalPlaceApi.batchCheckIn(selectedNode.value.id, requests)
    }

    ElMessage.success(`入住成功 (${requests.length}人)`)
    showCheckInDialog.value = false
    ciPendingUsers.value = []
    ciRemark.value = ''
    ciSearchUserId.value = undefined
    userSearchList.value = []
    await refreshAfterOccupantChange()
  } catch {
    /* axios 拦截器已处理 */
  } finally {
    checkInLoading.value = false
  }
}

async function handleCheckOut(occ: PlaceOccupant) {
  if (!selectedNode.value) return
  try {
    await ElMessageBox.confirm(
      `确定要将"${occ.occupantName}"退出吗？`,
      '确认退出',
      { type: 'warning', confirmButtonText: '退出', cancelButtonText: '取消' }
    )
    await universalPlaceApi.checkOut(selectedNode.value.id, occ.id)
    ElMessage.success('退出成功')
    await refreshAfterOccupantChange()
  } catch (error: any) {
    if (error !== 'cancel') { /* axios 拦截器已处理 */ }
  }
}

function startSwap(occ: PlaceOccupant) {
  swapSource.value = occ
  showSwapDialog.value = true
}

async function handleSwap(target: PlaceOccupant) {
  if (!selectedNode.value || !swapSource.value) return
  try {
    await universalPlaceApi.swapPositions(selectedNode.value.id, swapSource.value.id, target.id)
    ElMessage.success('位置交换成功')
    showSwapDialog.value = false
    swapSource.value = null
    await loadOccupants(selectedNode.value.id)
  } catch {
    /* axios 拦截器已处理 */
  }
}

function toggleHistory() {
  showHistory.value = !showHistory.value
  if (showHistory.value && selectedNode.value) {
    loadOccupantHistory(selectedNode.value.id)
  }
}

async function refreshAfterOccupantChange() {
  if (!selectedNode.value) return
  await loadOccupants(selectedNode.value.id)
  loadData()
  loadStatistics()
  if (showHistory.value) {
    loadOccupantHistory(selectedNode.value.id)
  }
}

function formatDate(dateStr?: string) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

// ========== Utility ==========
function getStatusLabel(status: number) {
  switch (status) {
    case 0: return '停用'
    case 2: return '维护中'
    default: return '正常'
  }
}

function statusBadgeClass(status: number) {
  switch (status) {
    case 0: return 'bg-gray-100 text-gray-600'
    case 2: return 'bg-amber-100 text-amber-700'
    default: return 'bg-emerald-100 text-emerald-700'
  }
}

function occupantTypeLabel(type?: string): string {
  const map: Record<string, string> = { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员', EMPLOYEE: '员工' }
  return type ? (map[type] || type) : '未知'
}

function genderLabel(node: PlaceTreeNode): string {
  const g = node.effectiveGender || node.gender
  if (!g) return ''
  if (g === 'MALE') return '男'
  if (g === 'FEMALE') return '女'
  if (g === 'MIXED') return '混合'
  return ''
}

function genderBadgeClass(node: PlaceTreeNode): string {
  const g = node.effectiveGender || node.gender
  if (g === 'MALE') return 'bg-blue-50 text-blue-600'
  if (g === 'FEMALE') return 'bg-pink-50 text-pink-600'
  if (g === 'MIXED') return 'bg-purple-50 text-purple-600'
  return 'bg-gray-50 text-gray-500'
}

function genderDisplay(node: PlaceTreeNode): string {
  const own = node.gender
  const eff = node.effectiveGender
  if (!own && !eff) return '无'
  if (own) {
    const map: Record<string, string> = { MALE: '男', FEMALE: '女', MIXED: '混合' }
    return map[own] || own
  }
  if (eff) {
    const map: Record<string, string> = { MALE: '男(继承)', FEMALE: '女(继承)', MIXED: '混合(继承)' }
    return map[eff] || eff
  }
  return '无'
}

// ========== Floor Plan ==========
async function handleFloorPlanSave() {
  if (!selectedNode.value || !floorPlanEditorRef.value) return
  floorPlanSaving.value = true
  try {
    const layout = floorPlanEditorRef.value.getLayout()
    await universalPlaceApi.update(selectedNode.value.id, {
      attributes: { ...(selectedNode.value.attributes ?? {}), layout }
    })
    ElMessage.success('平面图已保存')
    showFloorPlanDialog.value = false
    await loadData()
  } catch {
    /* axios interceptor handles error */
  } finally {
    floorPlanSaving.value = false
  }
}

// Close dropdown on outside click
function handleClickOutside(e: MouseEvent) {
  if (dropdownRef.value && !dropdownRef.value.contains(e.target as Node)) {
    showDropdown.value = false
  }
}

// ========== Lifecycle ==========
onMounted(() => {
  loadData()
  loadStatistics()
  loadPlaceTypes()
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped src="./UniversalPlaceManagement.css"></style>

<style>
/* Non-scoped: Element Plus teleports popper to body */
.ci-user-popper { min-width: 380px !important; }
.ci-user-popper .el-select-dropdown__item { padding: 5px 12px; height: auto; line-height: 1.5; }
/* Floor plan dialog: remove body padding */
.fp-dialog .el-dialog__header { padding: 0 !important; margin: 0 !important; }
.fp-dialog .el-dialog__body { padding: 0 !important; }
.fp-dialog .el-dialog__headerbtn { display: none; }
</style>
