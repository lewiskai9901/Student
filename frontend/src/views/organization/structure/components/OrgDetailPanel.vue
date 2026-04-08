<template>
  <div class="dp-root">
    <!-- Header Card -->
    <div class="dp-card">
      <div class="dp-card-header">
        <div class="dp-name-row">
          <h2 class="dp-name">{{ node.unitName }}</h2>
          <code class="tm-code">{{ node.unitCode }}</code>
          <span class="tm-chip" :style="typeBadgeStyle">
            {{ node.typeName || node.unitType }}
          </span>
          <span
            class="tm-chip"
            :class="node.status === 'ACTIVE' ? 'tm-chip-green' : node.status === 'FROZEN' ? 'tm-chip-amber' : 'tm-chip-red'"
          >
            {{ node.statusLabel || node.status }}
          </span>
        </div>
        <div class="dp-actions">
          <button class="tm-btn tm-btn-secondary" style="padding: 4px 10px; font-size: 12px;" @click="emit('addChild', node)">
            <Plus style="width: 12px; height: 12px;" />
            子组织
          </button>
          <button class="tm-btn tm-btn-secondary" style="padding: 4px 10px; font-size: 12px;" @click="emit('edit', node)">
            <Pencil style="width: 12px; height: 12px;" />
            编辑
          </button>
          <!-- More menu -->
          <div v-if="node.status === 'ACTIVE' || node.status === 'FROZEN'" class="dp-dropdown-wrap">
            <button class="dp-more-btn" @click="moreMenuOpen = !moreMenuOpen">
              <MoreHorizontal style="width: 14px; height: 14px;" />
            </button>
            <Teleport to="body">
              <div v-if="moreMenuOpen" class="dp-dropdown-overlay" @click="moreMenuOpen = false"></div>
            </Teleport>
            <div v-if="moreMenuOpen" class="dp-dropdown-menu">
              <button v-if="node.status === 'ACTIVE'" class="dp-menu-item" @click="moreMenuOpen = false; emit('toggleStatus', node)">
                <Ban style="width: 14px; height: 14px; color: #f59e0b;" /> 冻结
              </button>
              <button v-if="node.status === 'FROZEN'" class="dp-menu-item" @click="moreMenuOpen = false; emit('toggleStatus', node)">
                <Check style="width: 14px; height: 14px; color: #10b981;" /> 解冻
              </button>
              <div v-if="node.status === 'ACTIVE'" class="dp-menu-divider"></div>
              <button v-if="node.status === 'ACTIVE'" class="dp-menu-item" @click="moreMenuOpen = false; emit('merge', node)">
                <Merge style="width: 14px; height: 14px; color: #3b82f6;" /> 合并到...
              </button>
              <button v-if="node.status === 'ACTIVE'" class="dp-menu-item" @click="moreMenuOpen = false; emit('split', node)">
                <Split style="width: 14px; height: 14px; color: #6366f1;" /> 拆分
              </button>
              <div v-if="node.status === 'ACTIVE'" class="dp-menu-divider"></div>
              <button v-if="node.status === 'ACTIVE'" class="dp-menu-item dp-menu-danger" @click="moreMenuOpen = false; emit('dissolve', node)">
                <XCircle style="width: 14px; height: 14px;" /> 解散
              </button>
              <div class="dp-menu-divider"></div>
              <button class="dp-menu-item" @click="moreMenuOpen = false; showExportDialog = true">
                <Download style="width: 14px; height: 14px; color: #6b7280;" /> 导出数据
              </button>
              <button class="dp-menu-item" @click="moreMenuOpen = false; showImportDialog = true">
                <Upload style="width: 14px; height: 14px; color: #6b7280;" /> 导入数据
              </button>
              <div class="dp-menu-divider"></div>
              <button class="dp-menu-item dp-menu-danger" @click="moreMenuOpen = false; emit('delete', node)">
                <Trash2 style="width: 14px; height: 14px;" /> 删除
              </button>
            </div>
          </div>
          <button
            v-else
            class="tm-btn" style="padding: 4px 10px; font-size: 12px; background: #fef2f2; color: #dc2626; border: 1px solid #fecaca;"
            @click="emit('delete', node)"
          >
            <Trash2 style="width: 12px; height: 12px;" />
            删除
          </button>
        </div>
      </div>

      <!-- Statistics Bar -->
      <div v-if="stats" class="dp-stat-bar">
        <span>成员 <b>{{ stats.belongingCount }}</b></span>
        <template v-if="Object.keys(stats.countByUserType).length > 0">
          <span style="color: #d1d5db;">—</span>
          <template v-for="(cnt, typeCode, idx) in stats.countByUserType" :key="typeCode">
            <span v-if="idx > 0" style="color: #e5e7eb;">|</span>
            <span>{{ userTypeNameMap[typeCode as string] || typeCode }} <b>{{ cnt }}</b></span>
          </template>
        </template>
      </div>
    </div>

    <!-- Extension Attributes (from SPI plugin, if any) -->
    <div v-if="extensionSchema && extensionSchema.fields?.length > 0" class="dp-card" style="padding: 20px;">
      <h3 class="dp-section-label">扩展属性</h3>
      <DynamicForm :schema="extensionSchema" v-model="extensionAttrs" :disabled="true" />
    </div>

    <!-- Tabs -->
    <div class="dp-card">
      <div class="tm-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tm-tab"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <span v-if="tab.count !== undefined" class="dp-tab-count">{{ tab.count }}</span>
        </button>
      </div>

      <!-- Tab: 下级组织 -->
      <div v-if="activeTab === 'children'">
        <div class="dp-tab-toolbar">
          <span class="dp-tab-info">共 {{ children.length }} 个下级组织</span>
          <button class="tm-btn tm-btn-primary" style="padding: 4px 10px; font-size: 11px;" @click="emit('addChild', node)">
            <Plus style="width: 12px; height: 12px;" />
            添加
          </button>
        </div>
        <div v-if="children.length > 0" style="overflow-x: auto;">
          <table class="tm-table">
            <colgroup>
              <col />
              <col style="width: 120px;" />
              <col style="width: 100px;" />
              <col style="width: 80px;" />
              <col style="width: 100px;" />
            </colgroup>
            <thead>
              <tr>
                <th class="text-left">名称</th>
                <th class="text-left">编码</th>
                <th class="text-left">类型</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="child in children" :key="child.id">
                <td class="text-left">
                  <span style="font-weight: 500;">{{ child.unitName }}</span>
                  <span v-if="child.children?.length" class="tm-chip tm-chip-gray" style="margin-left: 6px;">
                    {{ child.children.length }} 子级
                  </span>
                </td>
                <td class="text-left">
                  <code class="tm-code">{{ child.unitCode }}</code>
                </td>
                <td class="text-left">{{ child.typeName || child.unitType }}</td>
                <td>
                  <span
                    class="tm-chip"
                    :class="child.status === 'ACTIVE' ? 'tm-chip-green' : child.status === 'FROZEN' ? 'tm-chip-amber' : 'tm-chip-red'"
                  >
                    {{ child.statusLabel || child.status }}
                  </span>
                </td>
                <td class="text-right">
                  <button class="tm-action" title="编辑" @click="emit('edit', child)">
                    <Pencil style="width: 13px; height: 13px;" />
                  </button>
                  <button
                    v-if="child.status === 'ACTIVE' || child.status === 'FROZEN'"
                    class="tm-action"
                    :title="child.status === 'ACTIVE' ? '冻结' : '解冻'"
                    @click="emit('toggleStatus', child)"
                  >
                    <Ban v-if="child.status === 'ACTIVE'" style="width: 13px; height: 13px;" />
                    <Check v-else style="width: 13px; height: 13px;" />
                  </button>
                  <button class="tm-action tm-action-danger" title="删除" @click="emit('delete', child)">
                    <Trash2 style="width: 13px; height: 13px;" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="dp-empty">
          <p>暂无下级组织</p>
          <button class="tm-btn tm-btn-secondary" style="margin-top: 10px; font-size: 12px;" @click="emit('addChild', node)">
            <Plus style="width: 14px; height: 14px;" />
            添加子组织
          </button>
        </div>
      </div>

      <!-- Tab: 成员 (仅本组织归属成员) -->
      <div v-if="activeTab === 'members'">
        <div class="dp-tab-toolbar">
          <span class="dp-tab-info">共 {{ belongingMembers.length }} 人</span>
          <button class="tm-btn tm-btn-secondary" style="padding: 4px 10px; font-size: 12px;" @click="showAddMember = true">
            <Plus style="width: 12px; height: 12px;" />
            添加成员
          </button>
        </div>
        <div v-if="membersLoading" class="dp-loading">
          <Loader2 style="width: 18px; height: 18px; color: #9ca3af;" class="tm-spin" />
          <span>加载中...</span>
        </div>
        <div v-else-if="mergedMembers.length > 0" style="overflow-x: auto;">
          <table class="tm-table">
            <colgroup>
              <col />
              <col style="width: 100px;" />
              <col />
              <col style="width: 90px;" />
              <col style="width: 80px;" />
            </colgroup>
            <thead>
              <tr>
                <th class="text-left">姓名</th>
                <th class="text-left">身份</th>
                <th class="text-left">岗位</th>
                <th class="text-left">任职方式</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="m in mergedMembers"
                :key="`${m.userId}-${m.userPositionId || 'no-pos'}`"
              >
                <td class="text-left">
                  <button class="dp-link" @click="openUserRelation(m)">{{ m.userName }}</button>
                </td>
                <td class="text-left">{{ userTypeNameMap[m.userTypeCode || ''] || m.userTypeCode || '-' }}</td>
                <td class="text-left">
                  <span v-if="m.localPositionName">
                    {{ m.localPositionName }}
                    <span v-if="m.isKeyPosition" class="tm-chip tm-chip-amber" style="margin-left: 4px;">关键</span>
                  </span>
                  <span v-else style="color: #9ca3af;">—</span>
                </td>
                <td class="text-left">
                  <span v-if="m.appointmentType" class="tm-chip" :class="appointmentTagClass(m.appointmentType)">
                    {{ AppointmentTypeLabels[m.appointmentType] || m.appointmentType }}
                  </span>
                  <span v-else style="color: #9ca3af;">—</span>
                </td>
                <td class="text-right">
                  <button v-if="m.userPositionId" class="tm-action" title="移除岗位" @click="handleRemoveStaff(m)">
                    <UserMinus style="width: 13px; height: 13px;" />
                  </button>
                  <button class="tm-action tm-action-danger" title="移除成员" @click="handleRemoveMember(m)">
                    <Trash2 style="width: 13px; height: 13px;" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="dp-empty">
          <p>暂无成员</p>
        </div>
      </div>

      <!-- Tab: 关联场所 -->
      <div v-if="activeTab === 'places'">
        <div class="dp-tab-toolbar">
          <span class="dp-tab-info">共 {{ places.length }} 个关联场所</span>
          <button class="tm-btn tm-btn-secondary" style="padding: 4px 10px; font-size: 12px;" @click="showAddPlace = true">
            <Plus style="width: 12px; height: 12px;" />
            关联场所
          </button>
        </div>
        <div v-if="placesLoading" class="dp-loading">
          <Loader2 style="width: 18px; height: 18px; color: #9ca3af;" class="tm-spin" />
          <span>加载中...</span>
        </div>
        <div v-else-if="places.length > 0" style="overflow-x: auto;">
          <table class="tm-table">
            <colgroup>
              <col />
              <col style="width: 120px;" />
              <col style="width: 100px;" />
              <col style="width: 60px;" />
            </colgroup>
            <thead>
              <tr>
                <th class="text-left">场所名称</th>
                <th class="text-left">编码</th>
                <th class="text-left">关系类型</th>
                <th class="text-right"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in places" :key="s.id">
                <td class="text-left" style="font-weight: 500;">{{ s.metadata?.placeName || `场所#${s.resourceId}` }}</td>
                <td class="text-left">
                  <code class="tm-code">{{ s.metadata?.placeCode || '-' }}</code>
                </td>
                <td class="text-left">
                  <span class="tm-chip tm-chip-purple">{{ RelationLabels[s.relation] || s.relation }}</span>
                </td>
                <td class="text-right">
                  <button class="tm-action tm-action-danger" @click="handleRemovePlace(s)">移除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="dp-empty" style="padding: 16px;">
          暂无关联场所
        </div>
      </div>

      <!-- Tab: 岗位编制 -->
      <div v-if="activeTab === 'positions'">
        <!-- 本级岗位编制 -->
        <div class="dp-tab-toolbar">
          <span class="dp-tab-info">本级岗位 {{ positions.length }} 个</span>
        </div>
        <div v-if="positionsLoading" class="dp-loading">
          <Loader2 style="width: 18px; height: 18px; color: #9ca3af;" class="tm-spin" />
          <span>加载中...</span>
        </div>
        <div v-else-if="positions.length > 0" style="overflow-x: auto;">
          <table class="tm-table">
            <colgroup>
              <col />
              <col style="width: 90px;" />
              <col />
              <col style="width: 70px;" />
              <col style="width: 60px;" />
            </colgroup>
            <thead>
              <tr>
                <th class="text-left">岗位名称</th>
                <th>编制/在岗</th>
                <th class="text-left">在岗人员</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in positions" :key="p.id">
                <td class="text-left">
                  <span style="font-weight: 500;">{{ p.positionName }}</span>
                  <span v-if="p.isKeyPosition" class="tm-chip tm-chip-amber" style="margin-left: 6px;">关键</span>
                </td>
                <td>
                  <span class="tm-mono" :class="staffingColor(p)">
                    {{ p.currentCount ?? 0 }}/{{ p.headcount }}
                  </span>
                </td>
                <td class="text-left">
                  <template v-if="getPositionHolders(p.id).length > 0">
                    <span v-for="(h, i) in getPositionHolders(p.id)" :key="h.userPositionId">{{ i > 0 ? ', ' : '' }}{{ h.userName }}<span
                        v-if="!belongingUserIds.has(String(h.userId)) && h.primaryOrgUnitName"
                        class="tm-chip tm-chip-amber"
                        style="margin-left: 2px;"
                        :title="'归属: ' + h.primaryOrgUnitName"
                      >来自{{ h.primaryOrgUnitName }}</span><span
                        v-else-if="!belongingUserIds.has(String(h.userId))"
                        class="tm-chip tm-chip-amber"
                        style="margin-left: 2px;"
                      >外部</span></span>
                  </template>
                  <span v-else style="color: #f59e0b;">(空缺)</span>
                </td>
                <td>
                  <span class="tm-chip" :class="p.enabled ? 'tm-chip-green' : 'tm-chip-red'">
                    {{ p.enabled ? '启用' : '禁用' }}
                  </span>
                </td>
                <td class="text-right">
                  <button class="tm-action" title="任命人员" @click="openAppointDialog(p)">
                    <UserPlus style="width: 13px; height: 13px;" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="dp-empty">
          <p>暂无岗位</p>
        </div>

        <!-- 子组织岗位汇总（可折叠） -->
        <div v-if="children.length > 0" style="border-top: 1px solid #f3f4f6;">
          <button class="dp-collapse-btn" @click="toggleChildPositions">
            <span>
              子组织岗位汇总
              <span v-if="recursiveMembers.length > 0" style="color: #9ca3af; margin-left: 4px;">({{ childOnlyMembers.length }}人)</span>
            </span>
            <ChevronDown
              style="width: 14px; height: 14px; color: #9ca3af; transition: transform 0.15s;"
              :style="{ transform: showChildPositions ? 'rotate(180deg)' : 'none' }"
            />
          </button>

          <template v-if="showChildPositions">
            <div v-if="recursiveLoading" class="dp-loading" style="padding: 24px 0;">
              <Loader2 style="width: 16px; height: 16px; color: #9ca3af;" class="tm-spin" />
              <span>加载中...</span>
            </div>
            <template v-else-if="childOnlyMembers.length > 0">
              <!-- 汇总统计条 -->
              <div class="dp-summary-bar">
                <span v-for="s in childPositionSummary" :key="s.positionName" class="dp-summary-item">
                  {{ s.positionName }} <b>{{ s.count }}</b>
                </span>
              </div>
              <!-- 明细表 -->
              <div style="overflow-x: auto;">
                <table class="tm-table">
                  <colgroup>
                    <col />
                    <col />
                    <col />
                    <col style="width: 90px;" />
                  </colgroup>
                  <thead>
                    <tr>
                      <th class="text-left">人员</th>
                      <th class="text-left">岗位</th>
                      <th class="text-left">所在组织</th>
                      <th>任职方式</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="m in childOnlyMembers" :key="m.userPositionId">
                      <td class="text-left">{{ m.userName }}</td>
                      <td class="text-left">{{ m.positionName }}</td>
                      <td class="text-left">{{ m.orgUnitName }}</td>
                      <td>
                        <span v-if="m.appointmentType" class="tm-chip" :class="appointmentTagClass(m.appointmentType)">
                          {{ AppointmentTypeLabels[m.appointmentType] || m.appointmentType }}
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </template>
            <div v-else class="dp-empty" style="padding: 24px;">
              子组织中暂无岗位人员
            </div>
          </template>
        </div>
      </div>

      <!-- Tab: 操作记录 -->
      <div v-if="activeTab === 'changelog'">
        <ActivityTimeline
          resourceType="ORG_UNIT"
          :resourceId="node.id"
          :limit="50"
          title="操作记录"
        />
      </div>

      <!-- Tab: 基本信息 -->
      <div v-if="activeTab === 'info'" class="dp-info-grid">
        <div class="dp-info-item">
          <dt>组织编码</dt>
          <dd><code class="tm-code">{{ node.unitCode }}</code></dd>
        </div>
        <div class="dp-info-item">
          <dt>组织类型</dt>
          <dd>
            <span class="tm-chip" :style="typeBadgeStyle">
              {{ node.typeName || node.unitType }}
            </span>
          </dd>
        </div>
        <div class="dp-info-item">
          <dt>编制人数</dt>
          <dd>{{ node.headcount ?? '-' }}</dd>
        </div>
        <div class="dp-info-item">
          <dt>上级组织</dt>
          <dd>{{ parentName || '顶级组织' }}</dd>
        </div>
        <div class="dp-info-item">
          <dt>当前状态</dt>
          <dd>
            <span
              class="tm-chip"
              :class="node.status === 'ACTIVE' ? 'tm-chip-green' : node.status === 'FROZEN' ? 'tm-chip-amber' : node.status === 'DISSOLVED' ? 'tm-chip-red' : 'tm-chip-gray'"
            >
              {{ node.statusLabel || node.status }}
            </span>
          </dd>
        </div>
        <div class="dp-info-item">
          <dt>排序号</dt>
          <dd>{{ node.sortOrder }}</dd>
        </div>
      </div>
    </div>

    <!-- 添加成员 — 用户选择器弹窗 (Step 4) -->
    <UserSelectorDialog
      v-model:visible="showAddMember"
      title="添加成员到组织"
      :exclude-user-ids="belongingMembers.map(m => m.userId)"
      @confirm="handleAddMemberFromSelector"
    />

    <!-- 任命到岗位弹窗 (从岗位 Tab 触发) -->
    <Teleport to="body">
      <div v-if="showAppointDialog" class="dp-modal-overlay" @click.self="showAppointDialog = false">
        <div class="dp-modal">
          <h3 class="dp-modal-title">
            任命人员到「{{ appointForm.positionName }}」
          </h3>
          <div class="dp-modal-body">
            <div class="tm-field">
              <label class="tm-label">选择用户</label>
              <div class="dp-user-pick" @click="showAppointUserSelector = true">
                <span v-if="appointSelectedUser">
                  {{ appointSelectedUser.realName }} ({{ appointSelectedUser.username }})
                </span>
                <span v-else style="color: #9ca3af;">点击选择用户</span>
              </div>
            </div>
            <div class="tm-field">
              <label class="tm-label">任命类型</label>
              <el-select v-model="appointForm.appointmentType" style="width: 100%">
                <el-option label="正式" value="FORMAL" />
                <el-option label="代理" value="ACTING" />
                <el-option label="兼职" value="CONCURRENT" />
                <el-option label="试用" value="PROBATION" />
              </el-select>
            </div>
            <div class="tm-field">
              <label class="tm-label">任职日期</label>
              <el-date-picker
                v-model="appointForm.startDate"
                type="date"
                placeholder="选择日期"
                style="width: 100%"
                value-format="YYYY-MM-DD"
              />
            </div>
            <div class="tm-field">
              <label class="dp-checkbox-label">
                <input v-model="appointForm.isPrimary" type="checkbox" />
                主岗
              </label>
            </div>
          </div>
          <div class="dp-modal-footer">
            <button class="tm-btn tm-btn-secondary" @click="showAppointDialog = false">取消</button>
            <button
              class="tm-btn tm-btn-primary"
              :disabled="!appointForm.userId || appointSubmitting"
              @click="handleAppoint"
            >
              {{ appointSubmitting ? '任命中...' : '确定' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
    <!-- 任命用户选择器 -->
    <UserSelectorDialog
      v-model:visible="showAppointUserSelector"
      title="选择要任命的用户"
      @confirm="handleAppointUserSelected"
    />

    <!-- 导出/导入弹窗 (Step 5) -->
    <OrgExportDialog
      v-model:visible="showExportDialog"
      :org-unit="node"
    />
    <OrgImportDialog
      v-model:visible="showImportDialog"
      :org-unit-id="node.id"
      @imported="Promise.all([loadMembers(), loadPositions(), loadStats()])"
    />

    <!-- 用户关系抽屉 (Step 3) -->
    <UserRelationDrawer
      v-model:visible="showUserRelation"
      :user-id="userRelationUserId"
      :user-name="userRelationUserName"
      :tree-data="treeData"
      @changed="onUserRelationChanged"
    />

    <!-- 关联场所弹窗 -->
    <Teleport to="body">
      <div v-if="showAddPlace" class="dp-modal-overlay" @click.self="showAddPlace = false">
        <div class="dp-modal">
          <h3 class="dp-modal-title">关联场所到「{{ node.unitName }}」</h3>
          <div class="dp-modal-body">
            <div class="tm-field">
              <label class="tm-label">选择场所</label>
              <select v-model="addPlaceForm.placeId" class="tm-field-select">
                <option :value="0" disabled>请选择场所</option>
                <option v-for="s in placeOptions" :key="s.id" :value="s.id">
                  {{ s.placeName }} ({{ s.placeCode }})
                </option>
              </select>
            </div>
            <div class="tm-field">
              <label class="tm-label">关系类型</label>
              <select v-model="addPlaceForm.relationType" class="tm-field-select">
                <option value="PRIMARY">主管</option>
                <option value="SHARED">共享</option>
                <option value="MANAGED">托管</option>
              </select>
            </div>
            <div class="tm-field" style="display: flex; gap: 16px;">
              <label class="dp-checkbox-label">
                <input v-model="addPlaceForm.isPrimary" type="checkbox" />
                主归属
              </label>
              <label class="dp-checkbox-label">
                <input v-model="addPlaceForm.canInspect" type="checkbox" />
                可检查
              </label>
            </div>
          </div>
          <div class="dp-modal-footer">
            <button class="tm-btn tm-btn-secondary" @click="showAddPlace = false">取消</button>
            <button
              class="tm-btn tm-btn-primary"
              :disabled="!addPlaceForm.placeId || addPlaceSubmitting"
              @click="handleAddPlace"
            >
              {{ addPlaceSubmitting ? '关联中...' : '确定' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, reactive } from 'vue'
import {
  Building2,
  Plus,
  Pencil,
  Trash2,
  Ban,
  Check,
  CheckCircle,
  XCircle,
  Users,
  FolderOpen,
  MapPin,
  Loader2,
  MoreHorizontal,
  Merge,
  Split,
  UserPlus,
  UserMinus,
  Download,
  Upload,
  ChevronDown,
} from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { DepartmentResponse } from '@/api/organization'
import DynamicForm from '@/components/extension/DynamicForm.vue'
import { entityTypeApi } from '@/api/entityType'
import UserRelationDrawer from './UserRelationDrawer.vue'
import UserSelectorDialog from '@/components/common/UserSelectorDialog.vue'
import OrgExportDialog from './OrgExportDialog.vue'
import OrgImportDialog from './OrgImportDialog.vue'
import type { AccessRelation } from '@/types/accessRelation'
import { RelationLabels } from '@/types/accessRelation'
import { accessRelationApi } from '@/api/accessRelation'
import type { SimpleUser } from '@/types/user'
import type { UniversalPlace, PlaceTreeNode } from '@/types/universalPlace'
import { getSimpleUserList } from '@/api/user'
import { universalPlaceApi } from '@/api/universalPlace'
import { positionApi, userPositionApi } from '@/api/position'
import type { Position, OrgStatistics } from '@/types/position'
import ActivityTimeline from '@/components/activity/ActivityTimeline.vue'
import { AppointmentTypeLabels } from '@/types/position'
import type { OrgMember } from '@/types/position'
import { getEnabledUserTypes } from '@/api/userType'

interface Props {
  node: DepartmentResponse
  treeData: DepartmentResponse[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  refresh: []
  addChild: [node: DepartmentResponse]
  edit: [node: DepartmentResponse]
  delete: [node: DepartmentResponse]
  toggleStatus: [node: DepartmentResponse]
  merge: [node: DepartmentResponse]
  split: [node: DepartmentResponse]
  dissolve: [node: DepartmentResponse]
}>()

// ==================== Tab state ====================
const activeTab = ref<'children' | 'members' | 'places' | 'positions' | 'changelog' | 'info'>('children')
const moreMenuOpen = ref(false)

// ==================== Color mapping by category (OrgCategory enum) ====================
const OrgCategoryColorMap: Record<string, string> = {
  ROOT: '#3b82f6',         // 蓝 - 根组织
  BRANCH: '#8b5cf6',       // 紫 - 分支机构
  FUNCTIONAL: '#10b981',   // 绿 - 职能部门
  GROUP: '#ef4444',        // 红 - 成员组（班级/小组）
  CONTAINER: '#f59e0b',    // 橙 - 容器（年级/学部）
}

const typeColor = computed(() => OrgCategoryColorMap[props.node.category || ''] || '#6b7280')

const typeIconStyle = computed(() => ({
  backgroundColor: `${typeColor.value}18`,
  color: typeColor.value
}))

const typeBadgeStyle = computed(() => ({
  backgroundColor: `${typeColor.value}12`,
  color: typeColor.value
}))

// Find parent name
const findParentName = (nodes: DepartmentResponse[], parentId: number | null): string => {
  if (!parentId) return ''
  for (const n of nodes) {
    if (n.id === parentId) return n.unitName
    if (n.children) {
      const found = findParentName(n.children, parentId)
      if (found) return found
    }
  }
  return ''
}

const parentName = computed(() => findParentName(props.treeData, props.node.parentId))

// Children
const children = computed(() => props.node.children || [])

const getChildIconStyle = (child: DepartmentResponse) => {
  const color = OrgCategoryColorMap[child.category || ''] || '#6b7280'
  return {
    backgroundColor: `${color}18`,
    color: color
  }
}

// ==================== Statistics ====================
const stats = ref<OrgStatistics | null>(null)

const loadStats = async () => {
  try {
    stats.value = await userPositionApi.getOrgStatistics(props.node.id)
  } catch (e: any) {
    console.error('Failed to load stats', e)
  }
}

// ==================== Members data ====================
const belongingMembers = ref<OrgMember[]>([])
const staffMembers = ref<OrgMember[]>([])
const membersLoading = ref(false)

const loadMembers = async () => {
  membersLoading.value = true
  try {
    const [belonging, staff] = await Promise.all([
      userPositionApi.getBelongingMembers(props.node.id),
      userPositionApi.getOrgMembers(props.node.id),
    ])
    belongingMembers.value = belonging
    staffMembers.value = staff
  } catch (e: any) {
    console.error('Failed to load members', e)
  } finally {
    membersLoading.value = false
  }
}

// ==================== User type name map ====================
const userTypeNameMap = ref<Record<string, string>>({})

const loadUserTypes = async () => {
  try {
    const types = await getEnabledUserTypes()
    const map: Record<string, string> = {}
    for (const t of types) {
      map[t.typeCode] = t.typeName
    }
    userTypeNameMap.value = map
  } catch (e) {
    console.error('Failed to load user types', e)
  }
}
loadUserTypes()

// ==================== Merged members (unified list) ====================
interface MergedMember {
  userId: string | number
  userName: string
  userTypeCode?: string
  localPositionName?: string     // position name in THIS org
  appointmentType?: string
  startDate?: string
  isPrimary: boolean
  isKeyPosition?: boolean
  isExternal: boolean            // belongs to another org
  userPositionId?: string | number  // for remove action
}

const mergedMembers = computed<MergedMember[]>(() => {
  const result: MergedMember[] = []

  // Group staff by userId for quick lookup
  const staffByUser = new Map<string, OrgMember[]>()
  for (const s of staffMembers.value) {
    const key = String(s.userId)
    if (!staffByUser.has(key)) staffByUser.set(key, [])
    staffByUser.get(key)!.push(s)
  }

  // Only belonging members (primary_org_unit_id = this org)
  for (const m of belongingMembers.value) {
    const staffEntries = staffByUser.get(String(m.userId)) || []
    if (staffEntries.length > 0) {
      // 用户在当前组织有岗位 → 显示当前组织的岗位信息
      for (const s of staffEntries) {
        result.push({
          userId: m.userId,
          userName: m.userName,
          userTypeCode: m.userTypeCode,
          localPositionName: s.positionName,
          appointmentType: s.appointmentType,
          startDate: s.startDate,
          isPrimary: s.isPrimary,
          isKeyPosition: s.isKeyPosition,
          isExternal: false,
          userPositionId: s.userPositionId,
        })
      }
    } else {
      // 用户在当前组织无岗位 → 使用 getBelongingMembers 返回的主岗信息
      result.push({
        userId: m.userId,
        userName: m.userName,
        userTypeCode: m.userTypeCode,
        localPositionName: m.positionName,
        appointmentType: m.appointmentType,
        startDate: m.startDate,
        isPrimary: m.isPrimary,
        isKeyPosition: m.isKeyPosition,
        isExternal: false,
        userPositionId: m.userPositionId,
      })
    }
  }

  return result
})

// Belonging user IDs set (for positions tab "外部" tag)
const belongingUserIds = computed(() => new Set(belongingMembers.value.map(m => String(m.userId))))

// Appointment type tag color
const appointmentTagClass = (type: string) => {
  switch (type) {
    case 'FORMAL': return 'bg-blue-50 text-blue-700'
    case 'ACTING': return 'bg-amber-50 text-amber-700'
    case 'CONCURRENT': return 'bg-purple-50 text-purple-700'
    case 'PROBATION': return 'bg-gray-100 text-gray-600'
    default: return 'bg-gray-100 text-gray-600'
  }
}

// ==================== Child org positions (collapsible panel) ====================
const showChildPositions = ref(false)
const recursiveMembers = ref<OrgMember[]>([])
const recursiveLoading = ref(false)
const recursiveLoaded = ref(false)

// 排除本级，只留子组织的成员
const childOnlyMembers = computed(() => {
  const currentOrgId = String(props.node.id)
  return recursiveMembers.value.filter(m => String(m.orgUnitId) !== currentOrgId)
})

// 子组织岗位汇总统计
const childPositionSummary = computed(() => {
  const countMap = new Map<string, number>()
  for (const m of childOnlyMembers.value) {
    const name = m.positionName || '未知岗位'
    countMap.set(name, (countMap.get(name) || 0) + 1)
  }
  return Array.from(countMap.entries())
    .map(([positionName, count]) => ({ positionName, count }))
    .sort((a, b) => b.count - a.count)
})

const loadRecursiveMembers = async () => {
  recursiveLoading.value = true
  try {
    recursiveMembers.value = await userPositionApi.getOrgMembersRecursive(props.node.id)
    recursiveLoaded.value = true
  } catch (e: any) {
    console.error('Failed to load recursive members', e)
  } finally {
    recursiveLoading.value = false
  }
}

const toggleChildPositions = () => {
  showChildPositions.value = !showChildPositions.value
  if (showChildPositions.value && !recursiveLoaded.value) {
    loadRecursiveMembers()
  }
}

// ==================== Places data ====================
const places = ref<AccessRelation[]>([])
const placesLoading = ref(false)

const loadPlaces = async () => {
  placesLoading.value = true
  try {
    places.value = await accessRelationApi.getBySubject('org_unit', props.node.id, 'place')
  } catch (e: any) {
    console.error('Failed to load places', e)
  } finally {
    placesLoading.value = false
  }
}

// ==================== Tab counts ====================
const tabs = computed(() => [
  { key: 'children' as const, label: '下级组织', count: children.value.length },
  { key: 'members' as const, label: '成员', count: belongingMembers.value.length || undefined },
  { key: 'positions' as const, label: '岗位编制', count: positions.value.length },
  { key: 'places' as const, label: '关联场所', count: places.value.length },
  { key: 'changelog' as const, label: '操作记录', count: undefined },
  { key: 'info' as const, label: '基本信息', count: undefined }
])

// ==================== Positions data ====================
const positions = ref<Position[]>([])
const positionsLoading = ref(false)

const loadPositions = async () => {
  positionsLoading.value = true
  try {
    positions.value = await positionApi.getByOrgUnit(props.node.id)
  } catch (e: any) {
    console.error('Failed to load positions', e)
  } finally {
    positionsLoading.value = false
  }
}

// Position tab helpers
const getPositionHolders = (positionId: number | string) => {
  return staffMembers.value.filter(m => String(m.positionId) === String(positionId))
}

const staffingColor = (p: Position) => {
  const current = p.currentCount ?? 0
  if (current > p.headcount) return 'text-red-600'
  if (current === p.headcount) return 'text-green-600'
  return 'text-orange-500'
}

// ==================== Load data on node change ====================
// Extension schema from entity_type_configs (SPI plugin)
const extensionSchema = ref<{ fields: any[] } | null>(null)
const extensionAttrs = ref<Record<string, any>>({})

async function loadExtensionSchema() {
  extensionSchema.value = null
  extensionAttrs.value = {}
  const typeCode = props.node.typeCode || props.node.unitType
  if (!typeCode) return
  try {
    const res = await entityTypeApi.get('ORG_UNIT', typeCode)
    const data = (res as any).data || res
    if (data?.metadataSchema) {
      const schema = typeof data.metadataSchema === 'string' ? JSON.parse(data.metadataSchema) : data.metadataSchema
      if (schema?.fields?.length > 0) {
        extensionSchema.value = schema
        extensionAttrs.value = props.node.attributes || {}
      }
    }
  } catch { /* no plugin */ }
}

watch(
  () => props.node.id,
  () => {
    belongingMembers.value = []
    staffMembers.value = []
    places.value = []
    positions.value = []
    stats.value = null
    showChildPositions.value = false
    recursiveMembers.value = []
    recursiveLoaded.value = false
    loadMembers()
    loadPlaces()
    loadPositions()
    loadStats()
    loadExtensionSchema()
  },
  { immediate: true }
)

// ==================== Add Member (直接添加到组织, using UserSelectorDialog) ====================
const showAddMember = ref(false)
const userOptions = ref<SimpleUser[]>([])

const handleAddMemberFromSelector = async (users: SimpleUser[]) => {
  if (users.length === 0) return
  const user = users[0]
  // Step 2: Check if user already belongs to another org
  if (user.primaryOrgUnitId && String(user.primaryOrgUnitId) !== String(props.node.id)) {
    try {
      await ElMessageBox.confirm(
        `该用户当前归属于「${user.primaryOrgUnitName || '其他组织'}」，确定要变更到本组织吗？`,
        '变更组织提示',
        { type: 'warning' }
      )
    } catch {
      return // user cancelled
    }
  }
  try {
    await userPositionApi.addMember(props.node.id, user.id)
    ElMessage.success('成员添加成功')
    await Promise.all([loadMembers(), loadPositions(), loadStats()])
  } catch (e: any) {
    ElMessage.error(e.message || '添加失败')
  }
}

const handleRemoveMember = async (m: MergedMember) => {
  try {
    await ElMessageBox.confirm(
      `确定要将「${m.userName}」从本组织移除吗？${m.userPositionId ? '同时会移除其所有岗位。' : ''}`,
      '确认移除成员',
      { type: 'warning' }
    )
    await userPositionApi.removeMember(props.node.id, m.userId)
    ElMessage.success('已移除成员')
    await Promise.all([loadMembers(), loadPositions(), loadStats()])
  } catch {
    // cancelled
  }
}

const handleRemoveStaff = async (m: MergedMember) => {
  if (!m.userPositionId) return
  try {
    await ElMessageBox.confirm(
      `确定要移除「${m.userName}」的岗位「${m.localPositionName}」吗？`,
      '确认移除岗位',
      { type: 'warning' }
    )
    await userPositionApi.endAppointment(m.userPositionId, {
      endDate: new Date().toISOString().slice(0, 10),
      reason: '管理员移除',
    })
    ElMessage.success('已移除岗位')
    await Promise.all([loadMembers(), loadPositions(), loadStats()])
  } catch {
    // cancelled
  }
}

// ==================== Appoint to Position (从岗位 Tab 触发) ====================
const showAppointDialog = ref(false)
const showAppointUserSelector = ref(false)
const appointSubmitting = ref(false)
const appointSelectedUser = ref<SimpleUser | null>(null)
const appointForm = reactive({
  positionId: null as number | string | null,
  positionName: '',
  userId: null as number | string | null,
  appointmentType: 'FORMAL',
  isPrimary: true,
  startDate: new Date().toISOString().slice(0, 10),
})

const openAppointDialog = (p: Position) => {
  appointForm.positionId = p.id
  appointForm.positionName = p.positionName
  appointForm.userId = null
  appointSelectedUser.value = null
  appointForm.appointmentType = 'FORMAL'
  appointForm.isPrimary = true
  appointForm.startDate = new Date().toISOString().slice(0, 10)
  showAppointDialog.value = true
}

const handleAppointUserSelected = (users: SimpleUser[]) => {
  if (users.length > 0) {
    const u = users[0]
    appointSelectedUser.value = u
    appointForm.userId = u.id
  }
}

const handleAppoint = async () => {
  if (!appointForm.userId || !appointForm.positionId) return
  appointSubmitting.value = true
  try {
    // 查询用户现有岗位，如有则提示"从哪里到哪里"
    const existingPositions = await userPositionApi.getByUser(appointForm.userId)
    const currentPositions = existingPositions.filter(p => p.isCurrent)
    if (currentPositions.length > 0) {
      const fromList = currentPositions
        .map(p => `「${p.orgUnitName || '?'}」${p.positionName || ''}(${AppointmentTypeLabels[p.appointmentType || ''] || p.appointmentType || ''})`)
        .join('、')
      const toLabel = `「${props.node.unitName}」${appointForm.positionName}`
      await ElMessageBox.confirm(
        `该用户当前任职于 ${fromList}，确定要任命到 ${toLabel} 吗？`,
        '任命确认',
        { type: 'warning' }
      )
    }

    await userPositionApi.appoint({
      userId: appointForm.userId,
      positionId: appointForm.positionId,
      isPrimary: appointForm.isPrimary,
      appointmentType: appointForm.appointmentType,
      startDate: appointForm.startDate,
    })
    ElMessage.success('任命成功')
    showAppointDialog.value = false
    await Promise.all([loadMembers(), loadPositions(), loadStats()])
  } catch (e: any) {
    if (e !== 'cancel' && e?.toString() !== 'cancel') {
      ElMessage.error(e.message || '任命失败')
    }
  } finally {
    appointSubmitting.value = false
  }
}

// ==================== Add Place ====================
const showAddPlace = ref(false)
const addPlaceSubmitting = ref(false)
const placeOptions = ref<UniversalPlace[]>([])
const addPlaceForm = reactive({
  placeId: 0,
  relationType: 'PRIMARY',
  isPrimary: false,
  canInspect: false
})

const flattenTree = (nodes: PlaceTreeNode[]): UniversalPlace[] => {
  const result: UniversalPlace[] = []
  const walk = (list: PlaceTreeNode[]) => {
    for (const n of list) {
      result.push(n)
      if (n.children?.length) walk(n.children)
    }
  }
  walk(nodes)
  return result
}

watch(showAddPlace, async (show) => {
  if (show && placeOptions.value.length === 0) {
    try {
      const tree = await universalPlaceApi.getTree()
      placeOptions.value = flattenTree(tree)
    } catch (e) {
      console.error('Failed to load places', e)
    }
  }
  if (show) {
    addPlaceForm.placeId = 0
    addPlaceForm.relationType = 'PRIMARY'
    addPlaceForm.isPrimary = false
    addPlaceForm.canInspect = false
  }
})

const handleAddPlace = async () => {
  if (!addPlaceForm.placeId) return
  addPlaceSubmitting.value = true
  try {
    await accessRelationApi.create({
      resourceType: 'place',
      resourceId: addPlaceForm.placeId,
      subjectType: 'org_unit',
      subjectId: props.node.id,
      relation: addPlaceForm.relationType,
      metadata: {
        isPrimary: addPlaceForm.isPrimary,
        canInspect: addPlaceForm.canInspect
      }
    })
    ElMessage.success('场所关联成功')
    showAddPlace.value = false
    await loadPlaces()
  } catch (e: any) {
    ElMessage.error(e.message || '关联失败')
  } finally {
    addPlaceSubmitting.value = false
  }
}

const handleRemovePlace = async (s: AccessRelation) => {
  try {
    await ElMessageBox.confirm(
      `确定要移除场所「${s.metadata?.placeName || '场所#' + s.resourceId}」的关联吗？`,
      '确认移除',
      { type: 'warning' }
    )
    await accessRelationApi.delete(s.id)
    ElMessage.success('已移除关联')
    await loadPlaces()
  } catch {
    // cancelled
  }
}

// ==================== Export / Import Dialogs (Step 5) ====================
const showExportDialog = ref(false)
const showImportDialog = ref(false)

// ==================== User Relation Drawer (Step 3) ====================
const showUserRelation = ref(false)
const userRelationUserId = ref<number | string | null>(null)
const userRelationUserName = ref('')

const openUserRelation = (m: MergedMember) => {
  userRelationUserId.value = m.userId
  userRelationUserName.value = m.userName
  showUserRelation.value = true
}

const onUserRelationChanged = () => {
  loadMembers()
  loadPositions()
  loadStats()
}
</script>

<style scoped>
@import '@/styles/teaching-ui.css';

.dp-root { display: flex; flex-direction: column; gap: 16px; }

/* Header card */
.dp-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}
.dp-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
}
.dp-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.dp-name {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dp-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.dp-stat-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  border-top: 1px solid #f3f4f6;
  padding: 8px 20px;
  font-size: 12px;
  color: #6b7280;
}
.dp-stat-bar b { font-weight: 600; color: #111827; }

/* Section label */
.dp-section-label {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #9ca3af;
  margin: 0 0 12px;
}

/* Tab extras */
.dp-tab-count {
  display: inline-block;
  margin-left: 4px;
  padding: 1px 6px;
  border-radius: 99px;
  background: #f3f4f6;
  font-size: 10px;
  font-weight: 500;
  color: #6b7280;
}
.dp-tab-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 20px;
  border-bottom: 1px solid #f3f4f6;
}
.dp-tab-info { font-size: 12px; color: #6b7280; }

/* Empty / loading */
.dp-empty {
  text-align: center;
  padding: 40px 0;
  color: #9ca3af;
  font-size: 13px;
}
.dp-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 0;
  font-size: 13px;
  color: #9ca3af;
}

/* Link button */
.dp-link {
  background: none;
  border: none;
  padding: 0;
  font-size: 13px;
  font-weight: 500;
  color: #2563eb;
  cursor: pointer;
  font-family: inherit;
}
.dp-link:hover { color: #1d4ed8; text-decoration: underline; }

/* Collapse button */
.dp-collapse-btn {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  color: #4b5563;
  font-family: inherit;
  text-align: left;
  transition: background 0.1s;
}
.dp-collapse-btn:hover { background: #f9fafb; }

/* Summary bar (child positions) */
.dp-summary-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 8px 20px;
  border-bottom: 1px solid #f3f4f6;
  background: #fafafa;
}
.dp-summary-item { font-size: 12px; color: #6b7280; }
.dp-summary-item b { font-weight: 600; color: #374151; }

/* Info grid (basic info tab) */
.dp-info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px 24px;
  padding: 16px 20px;
}
.dp-info-item dt {
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 4px;
}
.dp-info-item dd {
  font-size: 13px;
  color: #111827;
  margin: 0;
}

/* More menu (native dropdown replacing el-dropdown) */
.dp-dropdown-wrap { position: relative; }
.dp-more-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #9ca3af;
  cursor: pointer;
  transition: background 0.1s;
}
.dp-more-btn:hover { background: #f9fafb; color: #6b7280; }
.dp-dropdown-overlay {
  position: fixed;
  inset: 0;
  z-index: 1999;
}
.dp-dropdown-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 4px);
  z-index: 2000;
  min-width: 160px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  padding: 4px 0;
}
.dp-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 7px 14px;
  font-size: 13px;
  font-family: inherit;
  color: #374151;
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  transition: background 0.1s;
}
.dp-menu-item:hover { background: #f3f4f6; }
.dp-menu-danger { color: #dc2626; }
.dp-menu-danger:hover { background: #fef2f2; }
.dp-menu-divider { height: 1px; background: #f3f4f6; margin: 4px 0; }

/* Modal (native dialog replacing fixed Tailwind modals) */
.dp-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.35);
  backdrop-filter: blur(2px);
}
.dp-modal {
  width: 480px;
  max-width: 90vw;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.12);
}
.dp-modal-title {
  padding: 20px 24px 0;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}
.dp-modal-body {
  padding: 16px 24px;
}
.dp-modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 24px 20px;
}

/* User pick field */
.dp-user-pick {
  display: flex;
  align-items: center;
  min-height: 36px;
  padding: 6px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  font-size: 13px;
  color: #111827;
  background: #fafafa;
  cursor: pointer;
  transition: border-color 0.15s;
}
.dp-user-pick:hover { border-color: #2563eb; }

/* Checkbox label */
.dp-checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
}

/* Tailwind class equivalents used by appointmentTagClass & staffingColor */
.bg-blue-50 { background: #eff6ff; }
.text-blue-700 { color: #1d4ed8; }
.bg-amber-50 { background: #fffbeb; }
.text-amber-700 { color: #b45309; }
.bg-purple-50 { background: #f5f3ff; }
.text-purple-700 { color: #6d28d9; }
.bg-gray-100 { background: #f3f4f6; }
.text-gray-600 { color: #4b5563; }
.text-red-600 { color: #dc2626; }
.text-green-600 { color: #16a34a; }
.text-orange-500 { color: #f59e0b; }
</style>
