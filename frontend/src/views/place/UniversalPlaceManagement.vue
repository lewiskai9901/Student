<template>
  <div class="flex h-full bg-gray-50">
    <!-- Left Sidebar -->
    <div class="flex w-[280px] flex-shrink-0 flex-col border-r border-gray-200 bg-white">
      <PlaceSidebar
        :tree-data="treeData"
        :selected-id="selectedNodeId"
        @select="handleSelectNode"
        @add-root="handleAddRoot"
      />
    </div>

    <!-- Right Content Panel -->
    <div class="flex flex-1 flex-col overflow-hidden">
      <!-- Top Header Bar -->
      <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
        <div>
          <h1 class="text-lg font-semibold text-gray-900">场所管理</h1>
          <p class="mt-0.5 text-sm text-gray-500">管理学校的场地与设施结构</p>
        </div>
        <router-link
          to="/system/place-types"
          class="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
        >
          <Settings class="h-4 w-4" />
          类型配置
        </router-link>
      </div>

      <!-- Stat Bar -->
      <div class="flex items-center gap-4 border-b border-gray-200 bg-white px-6 py-2.5">
        <span class="text-sm text-gray-500">总数 <span class="font-semibold text-gray-900">{{ stats.totalCount }}</span></span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">容量 <span class="font-semibold text-gray-900">{{ stats.totalCapacity }}</span></span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">占用 <span class="font-semibold text-gray-900">{{ stats.totalOccupancy }}</span></span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">占用率 <span class="font-semibold text-gray-900">{{ stats.occupancyRate?.toFixed(1) || 0 }}%</span></span>
      </div>

      <!-- Main Content Area -->
      <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
        <!-- Loading State -->
        <div v-if="loading" class="flex items-center justify-center py-20">
          <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
        </div>

        <!-- Detail Panel (when node selected) -->
        <div v-else-if="selectedNode" class="space-y-3">
          <!-- Detail Card -->
          <div class="rounded-xl border border-gray-200 bg-white">
            <!-- Header (compact) -->
            <div class="flex items-center justify-between px-5 py-3">
              <div class="flex items-center gap-2 min-w-0">
                <h2 class="truncate text-[15px] font-semibold text-gray-900">{{ selectedNode.placeName }}</h2>
                <span class="rounded px-1.5 py-0.5 text-[10px] font-medium flex-shrink-0" :class="statusBadgeClass(selectedNode.status)">{{ getStatusLabel(selectedNode.status) }}</span>
                <span v-if="genderLabel(selectedNode)" class="flex-shrink-0 text-[10px] font-medium px-1.5 py-0.5 rounded" :class="genderBadgeClass(selectedNode)">{{ genderLabel(selectedNode) }}</span>
              </div>
              <div class="flex items-center gap-1.5 flex-shrink-0">
                <button class="inline-flex items-center gap-1 rounded-md border border-gray-200 px-2.5 py-1 text-xs font-medium text-gray-600 hover:bg-gray-50" @click="handleEdit(selectedNode)">
                  <Pencil class="h-3 w-3" /> 编辑
                </button>
                <button v-if="!selectedNode.leaf" class="inline-flex items-center gap-1 rounded-md border border-gray-200 px-2.5 py-1 text-xs font-medium text-gray-600 hover:bg-gray-50" @click="handleAddChild(selectedNode)">
                  <Plus class="h-3 w-3" /> 子场所
                </button>
                <button
                  v-if="selectedNode.occupiable || selectedNode.hasCapacity || selectedNode.leaf || selectedNode.attributes?.layout"
                  class="inline-flex items-center gap-1 rounded-md border border-gray-200 px-2.5 py-1 text-xs font-medium text-gray-600 hover:bg-gray-50"
                  @click="showFloorPlanDialog = true"
                >
                  <MapPin class="h-3 w-3" /> 平面图
                </button>
                <div class="relative" ref="dropdownRef">
                  <button class="inline-flex h-6 w-6 items-center justify-center rounded-md border border-gray-200 text-gray-400 hover:bg-gray-50" @click="showDropdown = !showDropdown">
                    <MoreHorizontal class="h-3.5 w-3.5" />
                  </button>
                  <div v-if="showDropdown" class="absolute right-0 top-full z-10 mt-1 w-36 rounded-lg border border-gray-200 bg-white py-1 shadow-lg">
                    <button v-if="selectedNode.status === 1" class="flex w-full items-center gap-2 px-3 py-1.5 text-xs text-gray-700 hover:bg-gray-50" @click="handleCommand('maintenance')"><Wrench class="h-3 w-3" /> 设为维护中</button>
                    <button v-if="selectedNode.status !== 1" class="flex w-full items-center gap-2 px-3 py-1.5 text-xs text-gray-700 hover:bg-gray-50" @click="handleCommand('enable')"><CheckCircle class="h-3 w-3" /> 恢复正常</button>
                    <button v-if="selectedNode.status === 1" class="flex w-full items-center gap-2 border-t border-gray-100 px-3 py-1.5 text-xs text-gray-700 hover:bg-gray-50" @click="handleCommand('disable')"><XCircle class="h-3 w-3" /> 停用</button>
                    <button class="flex w-full items-center gap-2 border-t border-gray-100 px-3 py-1.5 text-xs text-red-500 hover:bg-red-50" @click="handleCommand('delete')"><Trash2 class="h-3 w-3" /> 删除</button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Compact stats -->
            <div class="flex items-center gap-4 border-t border-gray-100 px-5 py-2 text-xs text-gray-600">
              <span>编号 <strong class="text-gray-900">{{ selectedNode.placeCode }}</strong></span>
              <div class="h-3 w-px bg-gray-200" />
              <span>类型 <strong class="text-gray-900">{{ selectedNode.typeName }}</strong></span>
              <div class="h-3 w-px bg-gray-200" />
              <span v-if="selectedNode.hasCapacity">容量 <strong class="text-gray-900">{{ selectedNode.currentOccupancy || 0 }}/{{ selectedNode.capacity || '-' }}</strong></span>
              <span v-else>容量 <strong class="text-gray-300">不适用</strong></span>
            </div>
          </div>

          <!-- Tabs -->
          <div class="rounded-xl border border-gray-200 bg-white">
            <div class="flex border-b border-gray-200">
              <button
                v-for="tab in placeTabs"
                :key="tab.key"
                :class="[
                  'relative px-4 py-2.5 text-xs font-medium transition-colors',
                  activePlaceTab === tab.key
                    ? 'text-blue-600'
                    : 'text-gray-500 hover:text-gray-700'
                ]"
                @click="activePlaceTab = tab.key"
              >
                {{ tab.label }}
                <span
                  v-if="tab.count !== undefined && tab.count > 0"
                  class="ml-1 rounded-full bg-gray-100 px-1.5 py-0.5 text-[10px] font-medium text-gray-600"
                >{{ tab.count }}</span>
                <div
                  v-if="activePlaceTab === tab.key"
                  class="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-600"
                ></div>
              </button>
            </div>

            <!-- Tab: 子场所 -->
            <div v-if="activePlaceTab === 'children'">
              <div v-if="childPlaces.length > 0" class="flex flex-wrap gap-1.5 px-5 py-3">
                <button
                  v-for="child in childPlaces"
                  :key="child.id"
                  class="inline-flex items-center gap-1 rounded border border-gray-200 bg-gray-50 px-2 py-1 text-[11px] transition-colors hover:border-blue-300 hover:bg-blue-50"
                  @click="selectPlace(child)"
                >
                  <span class="font-medium text-gray-700">{{ child.placeName }}</span>
                  <span class="text-gray-400">{{ child.typeName }}</span>
                  <span v-if="child.capacity" class="rounded bg-gray-200 px-1 py-px text-[10px] text-gray-600">{{ child.currentOccupancy || 0 }}/{{ child.capacity }}</span>
                </button>
              </div>
              <div v-else class="px-5 py-4 text-center text-xs text-gray-400">暂无子场所</div>
            </div>

            <!-- Tab: 关联组织 -->
            <div v-if="activePlaceTab === 'orgs'">
              <div class="flex items-center justify-end border-b border-gray-50 px-5 py-2.5">
                <button
                  class="inline-flex items-center gap-1 rounded-md bg-blue-600 px-2.5 py-1 text-[11px] font-medium text-white transition-colors hover:bg-blue-700"
                  @click="openAddOrgRelDialog"
                >
                  <Plus class="h-3 w-3" /> 添加
                </button>
              </div>
              <table v-if="placeOrgRelations.length > 0" class="w-full text-xs">
                <thead>
                  <tr class="border-b border-gray-100 bg-gray-50/50">
                    <th class="px-4 py-2 text-left font-medium text-gray-500">组织名称</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">关系类型</th>
                    <th class="w-12 px-2 py-2 text-right font-medium text-gray-400"></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="rel in placeOrgRelations" :key="rel.id" class="border-b border-gray-50 hover:bg-gray-50/50">
                    <td class="px-4 py-2 font-medium text-gray-800">{{ rel.metadata?.subjectName || `组织#${rel.subjectId}` }}</td>
                    <td class="px-4 py-2">
                      <span class="rounded px-1.5 py-0.5 text-[10px] font-medium" :class="relTypeBadge(rel.metadata?.relationType)">
                        {{ RelationLabels[rel.relation] || rel.relation }}
                      </span>
                    </td>
                    <td class="whitespace-nowrap px-2 py-2 text-right">
                      <button
                        class="rounded px-1.5 py-0.5 text-[10px] font-medium text-red-400 transition-colors hover:bg-red-50 hover:text-red-500"
                        @click="handleDeleteOrgRelation(rel)"
                      >移除</button>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div v-else class="px-5 py-4 text-center text-xs text-gray-400">暂无关联组织</div>
            </div>

            <!-- Tab: 入住管理 -->
            <div v-if="activePlaceTab === 'occupants' && showOccupantPanel">
            <div class="flex items-center justify-between border-b border-gray-50 px-5 py-2.5">
              <div class="flex items-center gap-2">
                <span v-if="selectedNode?.capacity" class="text-[11px] text-gray-400">
                  容量 <span class="font-medium text-gray-600">{{ occupants.length }}/{{ selectedNode.capacity }}</span>{{ selectedNode.capacityUnit ? selectedNode.capacityUnit : '人' }}
                </span>
                <!-- View mode toggle -->
                <div v-if="selectedNode?.capacity" class="ml-2 flex rounded border border-gray-200 overflow-hidden">
                  <button
                    class="px-1.5 py-0.5 text-[10px]"
                    :class="occupantViewMode === 'list' ? 'bg-blue-50 text-blue-600 font-medium' : 'text-gray-400 hover:bg-gray-50'"
                    @click="occupantViewMode = 'list'"
                  >列表</button>
                  <button
                    class="px-1.5 py-0.5 text-[10px] border-l border-gray-200"
                    :class="occupantViewMode === 'grid' ? 'bg-blue-50 text-blue-600 font-medium' : 'text-gray-400 hover:bg-gray-50'"
                    @click="occupantViewMode = 'grid'"
                  >网格</button>
                  <button
                    v-if="selectedNode?.attributes?.layout"
                    class="px-1.5 py-0.5 text-[10px] border-l border-gray-200"
                    :class="occupantViewMode === 'floor' ? 'bg-blue-50 text-blue-600 font-medium' : 'text-gray-400 hover:bg-gray-50'"
                    @click="occupantViewMode = 'floor'"
                  >平面</button>
                </div>
              </div>
              <button
                class="inline-flex items-center gap-1 rounded-md bg-blue-600 px-2.5 py-1 text-[11px] font-medium text-white transition-colors hover:bg-blue-700"
                @click="openCheckInDialog"
              >
                <Plus class="h-3 w-3" /> 入住
              </button>
            </div>

            <!-- Floor Plan View -->
            <div v-if="occupantViewMode === 'floor' && selectedNode?.attributes?.layout" class="border-t border-gray-100" style="height: 400px;">
              <FloorPlanEditor
                :place-id="selectedNode.id"
                :initial-layout="selectedNode.attributes.layout"
                :occupants="occupants"
                mode="view"
              />
            </div>

            <!-- Grid View -->
            <div v-else-if="occupantViewMode === 'grid' && selectedNode?.capacity" class="border-t border-gray-100">
              <SeatGrid
                :capacity="selectedNode.capacity"
                :occupants="occupants"
                :capacity-unit="selectedNode.capacityUnit"
                @check-in="(posNo) => { openCheckInDialog(); }"
                @select="(occ) => startSwap(occ)"
              />
            </div>

            <!-- Occupant Table (list view) -->
            <div v-else class="border-t border-gray-100">
              <table v-if="occupants.length > 0" class="w-full text-xs">
                <thead>
                  <tr class="border-b border-gray-100 bg-gray-50/50">
                    <th class="px-4 py-2 text-left font-medium text-gray-500">位置</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">账号</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">姓名</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">类型</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">部门</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">性别</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">入住时间</th>
                    <th class="px-4 py-2 text-right font-medium text-gray-500">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="occ in occupants" :key="occ.id" class="border-b border-gray-50 hover:bg-gray-50/50">
                    <td class="px-4 py-2 font-medium text-gray-700">{{ occ.positionNo || '-' }}</td>
                    <td class="px-4 py-2 text-gray-600">{{ occ.username || '-' }}</td>
                    <td class="px-4 py-2 text-gray-900">{{ occ.occupantName || '-' }}</td>
                    <td class="px-4 py-2 text-gray-500">{{ occ.userTypeName || '-' }}</td>
                    <td class="px-4 py-2 text-gray-500">{{ occ.orgUnitName || '-' }}</td>
                    <td class="px-4 py-2">
                      <span v-if="occ.gender === 1" class="text-blue-500">男</span>
                      <span v-else-if="occ.gender === 2" class="text-pink-500">女</span>
                      <span v-else class="text-gray-300">-</span>
                    </td>
                    <td class="px-4 py-2 text-gray-500">{{ formatDate(occ.checkInTime) }}</td>
                    <td class="px-4 py-2 text-right">
                      <div class="flex items-center justify-end gap-1">
                        <button
                          class="rounded px-1.5 py-0.5 text-[10px] font-medium text-red-500 transition-colors hover:bg-red-50"
                          @click="handleCheckOut(occ)"
                        >退出</button>
                        <button
                          v-if="occupants.length > 1"
                          class="rounded px-1.5 py-0.5 text-[10px] font-medium text-blue-500 transition-colors hover:bg-blue-50"
                          @click="startSwap(occ)"
                        >交换</button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div v-else class="px-5 py-6 text-center text-xs text-gray-400">
                暂无入住记录
              </div>
            </div>

            <!-- Empty slots hint -->
            <div v-if="selectedNode?.capacity && occupants.length < selectedNode.capacity" class="border-t border-gray-100 px-5 py-2 text-[11px] text-gray-400">
              剩余 {{ selectedNode.capacity - occupants.length }} 个空位
            </div>

            <!-- History toggle -->
            <div class="border-t border-gray-100 px-5 py-2">
              <button
                class="text-[11px] font-medium text-blue-500 hover:text-blue-600"
                @click="toggleHistory"
              >
                {{ showHistory ? '收起历史记录' : '查看历史记录' }}
              </button>
            </div>

            <!-- History Table -->
            <div v-if="showHistory" class="border-t border-gray-100">
              <table v-if="occupantHistory.length > 0" class="w-full text-xs">
                <thead>
                  <tr class="border-b border-gray-100 bg-gray-50/50">
                    <th class="px-4 py-2 text-left font-medium text-gray-500">位置</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">账号</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">姓名</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">类型</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">部门</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">性别</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">入住时间</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">退出时间</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="occ in occupantHistory" :key="occ.id" class="border-b border-gray-50">
                    <td class="px-4 py-2 text-gray-600">{{ occ.positionNo || '-' }}</td>
                    <td class="px-4 py-2 text-gray-600">{{ occ.username || '-' }}</td>
                    <td class="px-4 py-2 text-gray-700">{{ occ.occupantName || '-' }}</td>
                    <td class="px-4 py-2 text-gray-500">{{ occ.userTypeName || '-' }}</td>
                    <td class="px-4 py-2 text-gray-500">{{ occ.orgUnitName || '-' }}</td>
                    <td class="px-4 py-2">
                      <span v-if="occ.gender === 1" class="text-blue-500">男</span>
                      <span v-else-if="occ.gender === 2" class="text-pink-500">女</span>
                      <span v-else class="text-gray-300">-</span>
                    </td>
                    <td class="px-4 py-2 text-gray-500">{{ formatDate(occ.checkInTime) }}</td>
                    <td class="px-4 py-2 text-gray-500">{{ occ.checkOutTime ? formatDate(occ.checkOutTime) : '-' }}</td>
                    <td class="px-4 py-2">
                      <span :class="occ.status === 1 ? 'text-emerald-600' : 'text-gray-400'">
                        {{ occ.status === 1 ? '在住' : '已退出' }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div v-else class="px-5 py-4 text-center text-xs text-gray-400">
                暂无历史记录
              </div>
            </div>
          </div>

            <!-- Tab: 预订管理 -->
            <div v-if="activePlaceTab === 'bookings' && showBookingPanel">
            <div class="flex items-center justify-end gap-2 border-b border-gray-50 px-5 py-2.5">
                <button
                  class="text-[11px] font-medium"
                  :class="bookingShowAll ? 'text-blue-500' : 'text-gray-400'"
                  @click="bookingShowAll = !bookingShowAll; loadBookings(selectedNode!.id)"
                >{{ bookingShowAll ? '仅活跃' : '全部' }}</button>
                <button
                  class="inline-flex items-center gap-1 rounded-md bg-blue-600 px-2.5 py-1 text-[11px] font-medium text-white transition-colors hover:bg-blue-700"
                  @click="openBookingDialog"
                >
                  <Plus class="h-3 w-3" /> 新建预订
                </button>
            </div>
            <div class="border-t border-gray-100">
              <table v-if="bookings.length > 0" class="w-full text-xs">
                <thead>
                  <tr class="border-b border-gray-100 bg-gray-50/50">
                    <th class="px-4 py-2 text-left font-medium text-gray-500">预订人</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">标题</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">开始时间</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">结束时间</th>
                    <th class="px-4 py-2 text-left font-medium text-gray-500">状态</th>
                    <th class="w-16 px-4 py-2 text-right font-medium text-gray-400"></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="bk in bookings" :key="bk.id" class="border-b border-gray-50 hover:bg-gray-50/50">
                    <td class="px-4 py-2 font-medium text-gray-800">{{ bk.bookerName || '-' }}</td>
                    <td class="px-4 py-2 text-gray-700">{{ bk.title || '-' }}</td>
                    <td class="px-4 py-2 text-gray-500">{{ formatDateTime(bk.startTime) }}</td>
                    <td class="px-4 py-2 text-gray-500">{{ formatDateTime(bk.endTime) }}</td>
                    <td class="px-4 py-2">
                      <span class="rounded px-1.5 py-0.5 text-[10px] font-medium" :class="bookingStatusClass(bk.status)">
                        {{ bookingStatusLabel(bk.status) }}
                      </span>
                    </td>
                    <td class="px-4 py-2 text-right space-x-1">
                      <button
                        v-if="bk.status === 1 || bk.status === 2"
                        class="rounded px-1.5 py-0.5 text-[10px] font-medium text-blue-500 transition-colors hover:bg-blue-50 hover:text-blue-600"
                        @click="openSeatArrangement(bk)"
                      >排座</button>
                      <button
                        v-if="bk.status === 1"
                        class="rounded px-1.5 py-0.5 text-[10px] font-medium text-red-400 transition-colors hover:bg-red-50 hover:text-red-500"
                        @click="handleCancelBooking(bk)"
                      >取消</button>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div v-else class="px-5 py-4 text-center text-xs text-gray-400">
                暂无预订记录
              </div>
            </div>
            </div>

            <!-- Tab: 基本信息 -->
            <div v-if="activePlaceTab === 'info'" class="grid grid-cols-2 gap-x-6 gap-y-3 px-5 py-4 text-xs lg:grid-cols-4">
              <div>
                <dt class="font-medium text-gray-500">编号</dt>
                <dd class="mt-0.5 text-gray-900">{{ selectedNode.placeCode }}</dd>
              </div>
              <div>
                <dt class="font-medium text-gray-500">类型</dt>
                <dd class="mt-0.5 text-gray-900">{{ selectedNode.typeName }}</dd>
              </div>
              <div>
                <dt class="font-medium text-gray-500">层级</dt>
                <dd class="mt-0.5 text-gray-900">第{{ selectedNode.level }}级</dd>
              </div>
              <div>
                <dt class="font-medium text-gray-500">上级</dt>
                <dd class="mt-0.5 text-gray-900">{{ selectedNodeParentName || '(根节点)' }}</dd>
              </div>
              <div class="col-span-2">
                <dt class="font-medium text-gray-500">所属部门</dt>
                <dd class="mt-0.5 text-gray-900">{{ selectedNode.orgUnitName || selectedNode.effectiveOrgUnitName || '-' }}</dd>
              </div>
              <div class="col-span-2">
                <dt class="font-medium text-gray-500">负责人</dt>
                <dd class="mt-0.5 text-gray-900">{{ selectedNode.responsibleUserName || selectedNode.effectiveResponsibleUserName || '-' }}</dd>
              </div>
              <div>
                <dt class="font-medium text-gray-500">性别限制</dt>
                <dd class="mt-0.5 text-gray-900">{{ genderDisplay(selectedNode) }}</dd>
              </div>
              <div>
                <dt class="font-medium text-gray-500">容量</dt>
                <dd class="mt-0.5 text-gray-900">{{ selectedNode.hasCapacity ? `${selectedNode.currentOccupancy || 0}/${selectedNode.capacity || '-'}` : '不适用' }}</dd>
              </div>
              <div class="col-span-2">
                <dt class="font-medium text-gray-500">路径</dt>
                <dd class="mt-0.5 text-gray-900">{{ selectedNodePath }}</dd>
              </div>
              <div v-if="selectedNode.description" class="col-span-4">
                <dt class="font-medium text-gray-500">描述</dt>
                <dd class="mt-0.5 text-gray-600">{{ selectedNode.description }}</dd>
              </div>
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
        <div v-else class="space-y-4">
          <CapacityAlertBanner
            :refreshInterval="60"
            :collapsible="true"
            :defaultExpanded="false"
          />
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
              <span class="ci-opt-gender" :class="user.gender === 1 ? 'text-blue-500' : user.gender === 2 ? 'text-pink-500' : 'text-gray-300'">{{ user.gender === 1 ? '男' : user.gender === 2 ? '女' : '-' }}</span>
            </div>
          </el-option>
        </el-select>
        <el-popover :visible="showOrgPicker" placement="bottom-end" :width="300" trigger="click">
          <template #reference>
            <button class="ci-batch-btn" @click="showOrgPicker = !showOrgPicker">
              <Users class="h-3.5 w-3.5" /> 批量添加
            </button>
          </template>
          <div class="space-y-2 p-1">
            <el-tree-select v-model="ciBatchOrgId" :data="ciOrgTreeData" :props="{ label: 'unitName', value: 'id', children: 'children' }" placeholder="选择组织/班级" filterable check-strictly style="width: 100%" />
            <div class="flex items-center justify-end gap-2">
              <span v-if="ciBatchOrgId" class="text-xs text-gray-400">加载该组织下所有用户</span>
              <el-button type="primary" size="small" :disabled="!ciBatchOrgId" :loading="ciBatchLoading" @click="handleBatchAddByOrg">确定</el-button>
            </div>
          </div>
        </el-popover>
      </div>

      <!-- User List -->
      <div class="ci-list">
        <div class="ci-list-header">
          <span>已添加 <b>{{ ciPendingUsers.length }}</b> 人</span>
          <div class="flex items-center gap-2">
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
            <span class="ci-c-gender" :class="pu.gender === 1 ? 'text-blue-500' : pu.gender === 2 ? 'text-pink-500' : 'ci-text-dim'">{{ pu.gender === 1 ? '男' : pu.gender === 2 ? '女' : '-' }}</span>
            <span class="ci-c-pos">
              <select v-model="pu.positionNo" class="ci-pos-select">
                <option v-for="pos in getAvailablePositionsFor(pu.positionNo)" :key="pos" :value="pos">{{ pos }}</option>
              </select>
            </span>
            <span class="ci-c-act">
              <button class="ci-rm-btn" @click="ciPendingUsers.splice(idx, 1)"><XCircle class="h-3.5 w-3.5" /></button>
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
          <div class="sw-avatar"><Users class="h-4 w-4 text-purple-600" /></div>
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

    <!-- Create Booking Dialog -->
    <el-dialog v-model="showBookingDialog" title="新建预订" width="480px" :close-on-click-modal="false" destroy-on-close align-center>
      <el-form label-position="top" class="space-y-3">
        <el-form-item label="标题">
          <el-input v-model="bookingForm.title" placeholder="预订标题（选填）" maxlength="100" />
        </el-form-item>
        <el-form-item label="时间段" required>
          <el-date-picker
            v-model="bookingForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="参会人">
          <div class="w-full space-y-2">
            <div class="flex gap-2">
              <el-select
                v-model="bookingForm.attendeeIds"
                multiple
                filterable
                remote
                :remote-method="searchAttendees"
                :loading="attendeeSearching"
                placeholder="输入姓名搜索..."
                style="flex: 1"
                collapse-tags
                collapse-tags-tooltip
                :max-collapse-tags="3"
              >
                <el-option
                  v-for="u in attendeeOptions"
                  :key="u.id"
                  :value="u.id"
                  :label="u.realName || u.username"
                >
                  <span>{{ u.realName || u.username }}</span>
                  <span v-if="u.orgUnitName" class="ml-2 text-xs text-gray-400">{{ u.orgUnitName }}</span>
                </el-option>
              </el-select>
              <el-popover trigger="click" :width="260" placement="bottom-end">
                <template #reference>
                  <el-button>按组织添加</el-button>
                </template>
                <el-tree-select
                  v-model="batchOrgUnitId"
                  :data="ciOrgTreeData"
                  :props="{ label: 'unitName', value: 'id', children: 'children' }"
                  placeholder="选择组织"
                  filterable
                  check-strictly
                  style="width: 100%"
                  @change="handleBatchAddAttendeesByOrg"
                />
              </el-popover>
            </div>
            <div v-if="bookingForm.attendeeIds.length > 0" class="text-xs text-gray-400">
              已选 {{ bookingForm.attendeeIds.length }} 人
            </div>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="bookingForm.remark" type="textarea" :rows="2" placeholder="选填" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="showBookingDialog = false">取消</el-button>
          <el-button type="primary" :loading="bookingSubmitting" :disabled="!bookingForm.timeRange || bookingForm.timeRange.length < 2" @click="handleCreateBooking">确认预订</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Add Org Relation Dialog -->
    <el-dialog v-model="showAddOrgRelDialog" title="添加关联组织" width="440px" :close-on-click-modal="false" destroy-on-close align-center>
      <el-form label-position="top" class="space-y-3">
        <el-form-item label="选择组织" required>
          <el-tree-select
            v-model="addOrgRelForm.orgUnitId"
            :data="ciOrgTreeData"
            :props="{ label: 'unitName', value: 'id', children: 'children' }"
            placeholder="选择组织"
            filterable
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="关系类型">
          <el-radio-group v-model="addOrgRelForm.relationType">
            <el-radio value="PRIMARY">主归属</el-radio>
            <el-radio value="SHARED">共用</el-radio>
            <el-radio value="MANAGED">托管</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="showAddOrgRelDialog = false">取消</el-button>
          <el-button type="primary" :loading="addOrgRelLoading" :disabled="!addOrgRelForm.orgUnitId" @click="handleAddOrgRelation">确认</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Booking Seat Arrangement Dialog -->
    <BookingSeatDialog
      v-model:visible="showSeatDialog"
      :booking="seatDialogBooking"
      :place-layout="selectedNode?.attributes?.layout || null"
    />

    <!-- Floor Plan Editor Dialog (fullscreen) -->
    <el-dialog
      v-model="showFloorPlanDialog"
      fullscreen
      destroy-on-close
      :show-close="false"
      class="fp-dialog"
    >
      <template #header>
        <div class="flex items-center justify-between px-4 py-2 border-b border-gray-200">
          <div class="flex items-center gap-2">
            <MapPin class="h-4 w-4 text-gray-500" />
            <span class="text-sm font-semibold text-gray-800">平面图编辑 - {{ selectedNode?.placeName }}</span>
          </div>
          <div class="flex items-center gap-2">
            <button
              class="rounded-md border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50"
              @click="showFloorPlanDialog = false"
            >取消</button>
            <button
              class="rounded-md bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700"
              :class="{ 'opacity-50 cursor-not-allowed': floorPlanSaving }"
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
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
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
import BookingSeatDialog from './components/BookingSeatDialog.vue'
import CapacityAlertBanner from '@/components/place/CapacityAlertBanner.vue'
import ActivityTimeline from '@/components/activity/ActivityTimeline.vue'
import { universalPlaceApi, type PlaceStatistics } from '@/api/universalPlace'
import { universalPlaceTypeApi } from '@/api/universalPlaceType'
import { getSimpleUserList, getUsersByOrgUnit } from '@/api/user'
import { getOrgUnitTree } from '@/api/organization'
import { accessRelationApi } from '@/api/accessRelation'
import type { AccessRelation } from '@/types/accessRelation'
import { RelationLabels } from '@/types/accessRelation'
import type { SimpleUser, User } from '@/types/user'
import type { OrgUnitTreeNode } from '@/types'
import type { PlaceTreeNode, UniversalPlace, UniversalPlaceType, PlaceOccupant, PlaceBooking } from '@/types/universalPlace'

// ========== Data ==========
const loading = ref(false)
const treeData = ref<PlaceTreeNode[]>([])
const selectedNodeId = ref<number | null>(null)
const childPlaces = ref<UniversalPlace[]>([])
const statistics = ref<PlaceStatistics | null>(null)
const showDropdown = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)
const activePlaceTab = ref<string>('children')

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
  userId: number
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
const ciSearchUserId = ref<number | undefined>(undefined)
const userSearchList = ref<SimpleUser[]>([])
const userSearchLoading = ref(false)
const showOrgPicker = ref(false)
const ciOrgTreeData = ref<OrgUnitTreeNode[]>([])
const ciBatchOrgId = ref<number | undefined>(undefined)
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

function handleAddUserFromSearch(userId: number) {
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

// ========== Place-Org Relation Management ==========
const placeOrgRelations = ref<AccessRelation[]>([])
const showAddOrgRelDialog = ref(false)
const addOrgRelLoading = ref(false)
const addOrgRelForm = ref({
  orgUnitId: undefined as number | undefined,
  relationType: 'SHARED' as string,
  canUse: true,
  canManage: false,
  canInspect: false
})

function relTypeLabel(type?: string): string {
  const map: Record<string, string> = { PRIMARY: '主归属', SHARED: '共用', MANAGED: '托管' }
  return type ? (map[type] || type) : '未知'
}

function relTypeBadge(type?: string): string {
  if (type === 'PRIMARY') return 'bg-blue-50 text-blue-600'
  if (type === 'SHARED') return 'bg-purple-50 text-purple-600'
  if (type === 'MANAGED') return 'bg-amber-50 text-amber-600'
  return 'bg-gray-50 text-gray-500'
}

async function loadPlaceOrgRelations(placeId: number) {
  try {
    const all = await accessRelationApi.getByResource('place', placeId)
    placeOrgRelations.value = all.filter(r => r.subjectType === 'org_unit')
  } catch {
    placeOrgRelations.value = []
  }
}

async function openAddOrgRelDialog() {
  // Reset form
  addOrgRelForm.value = {
    orgUnitId: undefined,
    relationType: 'SHARED',
    canUse: true,
    canManage: false,
    canInspect: false
  }
  // Load org tree (lazy load once, shared with check-in dialog)
  if (ciOrgTreeData.value.length === 0) {
    try {
      ciOrgTreeData.value = await getOrgUnitTree()
    } catch { /* non-critical */ }
  }
  showAddOrgRelDialog.value = true
}

async function handleAddOrgRelation() {
  if (!selectedNode.value || !addOrgRelForm.value.orgUnitId) return
  addOrgRelLoading.value = true
  try {
    await accessRelationApi.create({
      resourceType: 'place',
      resourceId: selectedNode.value.id,
      relation: addOrgRelForm.value.relationType === 'PRIMARY' ? 'owner' : addOrgRelForm.value.relationType === 'MANAGED' ? 'manager' : 'user',
      subjectType: 'org_unit',
      subjectId: addOrgRelForm.value.orgUnitId!,
      accessLevel: addOrgRelForm.value.relationType === 'PRIMARY' || addOrgRelForm.value.relationType === 'MANAGED' ? 2 : 1,
      metadata: {
        relationType: addOrgRelForm.value.relationType,
        canUse: addOrgRelForm.value.canUse,
        canManage: addOrgRelForm.value.canManage,
        canAssign: false,
        canInspect: addOrgRelForm.value.canInspect
      }
    })
    ElMessage.success('添加成功')
    showAddOrgRelDialog.value = false
    addOrgRelForm.value = {
      orgUnitId: undefined,
      relationType: 'SHARED',
      canUse: true,
      canManage: false,
      canInspect: false
    }
    await loadPlaceOrgRelations(selectedNode.value.id)
  } catch {
    /* axios 拦截器已处理 */
  } finally {
    addOrgRelLoading.value = false
  }
}

async function handleDeleteOrgRelation(rel: AccessRelation) {
  try {
    await ElMessageBox.confirm(
      `确定要移除该组织的使用关系吗？`,
      '确认移除',
      { type: 'warning', confirmButtonText: '移除', cancelButtonText: '取消' }
    )
    await accessRelationApi.delete(rel.id)
    ElMessage.success('移除成功')
    if (selectedNode.value) {
      await loadPlaceOrgRelations(selectedNode.value.id)
    }
  } catch (error: any) {
    if (error !== 'cancel') { /* axios 拦截器已处理 */ }
  }
}

// ========== Booking Management ==========
const bookings = ref<PlaceBooking[]>([])
const bookingShowAll = ref(false)
const showBookingDialog = ref(false)
const bookingSubmitting = ref(false)
const bookingForm = ref({
  title: '',
  timeRange: null as string[] | null,
  attendeeIds: [] as (number | string)[],
  remark: ''
})
const attendeeOptions = ref<SimpleUser[]>([])
const attendeeSearching = ref(false)
const batchOrgUnitId = ref<number | null>(null)

const showBookingPanel = computed(() => {
  const node = selectedNode.value
  if (!node) return false
  return !!node.bookable
})

async function loadBookings(placeId: number) {
  try {
    bookings.value = await universalPlaceApi.getPlaceBookings(placeId, !bookingShowAll.value)
  } catch {
    bookings.value = []
  }
}

async function openBookingDialog() {
  bookingForm.value = { title: '', timeRange: null, attendeeIds: [], remark: '' }
  attendeeOptions.value = []
  batchOrgUnitId.value = null
  showBookingDialog.value = true
}

async function searchAttendees(keyword: string) {
  if (!keyword || keyword.trim().length < 1) {
    attendeeOptions.value = []
    return
  }
  attendeeSearching.value = true
  try {
    const users = await getSimpleUserList(keyword.trim())
    // Merge with existing selected options to keep them visible
    const existingMap = new Map(attendeeOptions.value.map(u => [String(u.id), u]))
    for (const u of users) existingMap.set(String(u.id), u)
    attendeeOptions.value = [...existingMap.values()]
  } catch {
    // keep existing
  } finally {
    attendeeSearching.value = false
  }
}

async function handleBatchAddAttendeesByOrg(orgId: number | null) {
  if (!orgId) return
  try {
    const users = await getUsersByOrgUnit(orgId)
    // Merge into attendeeOptions + attendeeIds
    const existingMap = new Map(attendeeOptions.value.map(u => [String(u.id), u]))
    const existingIds = new Set(bookingForm.value.attendeeIds.map(String))
    let added = 0
    for (const u of users) {
      const key = String(u.id)
      if (!existingMap.has(key)) {
        existingMap.set(key, { id: u.id, username: u.username, realName: u.realName, orgUnitName: u.orgUnitName })
      }
      if (!existingIds.has(key)) {
        bookingForm.value.attendeeIds.push(u.id)
        existingIds.add(key)
        added++
      }
    }
    attendeeOptions.value = [...existingMap.values()]
    ElMessage.success(`已添加 ${added} 人（该组织共 ${users.length} 人）`)
  } catch {
    ElMessage.error('获取组织用户失败')
  } finally {
    batchOrgUnitId.value = null
  }
}

async function handleCreateBooking() {
  if (!selectedNode.value || !bookingForm.value.timeRange || bookingForm.value.timeRange.length < 2) return
  bookingSubmitting.value = true
  try {
    await universalPlaceApi.createBooking(selectedNode.value.id, {
      title: bookingForm.value.title || undefined,
      startTime: bookingForm.value.timeRange[0],
      endTime: bookingForm.value.timeRange[1],
      attendeeIds: bookingForm.value.attendeeIds.length > 0 ? bookingForm.value.attendeeIds : undefined,
      remark: bookingForm.value.remark || undefined
    })
    ElMessage.success('预订成功')
    showBookingDialog.value = false
    await loadBookings(selectedNode.value.id)
  } catch {
    /* axios interceptor handles error */
  } finally {
    bookingSubmitting.value = false
  }
}

async function handleCancelBooking(bk: PlaceBooking) {
  try {
    await ElMessageBox.confirm(
      `确定要取消预订"${bk.title || '无标题'}"吗？`,
      '取消预订',
      { type: 'warning', confirmButtonText: '取消预订', cancelButtonText: '返回' }
    )
    await universalPlaceApi.cancelBooking(bk.id)
    ElMessage.success('预订已取消')
    if (selectedNode.value) {
      await loadBookings(selectedNode.value.id)
    }
  } catch (error: any) {
    if (error !== 'cancel') { /* axios interceptor handles */ }
  }
}

// ========== Seat Arrangement ==========
const showSeatDialog = ref(false)
const seatDialogBooking = ref<PlaceBooking | null>(null)

function openSeatArrangement(bk: PlaceBooking) {
  seatDialogBooking.value = bk
  showSeatDialog.value = true
}

function bookingStatusLabel(status: number): string {
  const map: Record<number, string> = { 0: '已取消', 1: '待使用', 2: '使用中', 3: '已完成' }
  return map[status] || '未知'
}

function bookingStatusClass(status: number): string {
  const map: Record<number, string> = {
    0: 'bg-gray-100 text-gray-500',
    1: 'bg-blue-50 text-blue-600',
    2: 'bg-emerald-50 text-emerald-600',
    3: 'bg-gray-100 text-gray-500'
  }
  return map[status] || 'bg-gray-100 text-gray-500'
}

function formatDateTime(dateStr?: string) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// ========== Computed ==========
const findNode = (nodes: PlaceTreeNode[], id: number): PlaceTreeNode | null => {
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
const buildPath = (nodes: PlaceTreeNode[], targetId: number, ancestors: string[] = []): string[] | null => {
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
 * 根据类型 schema 解析属性展示列表（label + 格式化 value）
 */
const resolvedAttributes = computed(() => {
  const node = selectedNode.value
  if (!node) return []
  const typeConfig = allPlaceTypes.value.find(t => t.typeCode === node.typeCode)
  let fields: any[] | undefined
  if (typeConfig?.metadataSchema) {
    try { fields = JSON.parse(typeConfig.metadataSchema)?.fields } catch { fields = undefined }
  }
  if (!fields || fields.length === 0) {
    // 无 schema，退化为 raw key/value
    if (node.attributes && Object.keys(node.attributes).length > 0) {
      return Object.entries(node.attributes).map(([key, value]) => ({
        key, label: key, value: String(value ?? ''), isEmpty: false
      }))
    }
    return []
  }
  // 按 sortOrder 排序，过滤 showInDetail !== false
  return fields
    .filter(f => f.showInDetail !== false)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    .map(f => {
      const raw = node.attributes?.[f.key]
      const hasValue = raw !== undefined && raw !== null && raw !== ''
      let displayValue = '未设置'
      if (hasValue) {
        displayValue = String(raw)
        // select 类型展示 label
        if (f.type === 'select' && f.options) {
          const opt = f.options.find(o => o.value === String(raw))
          if (opt) displayValue = opt.label
        }
        if (f.type === 'boolean') {
          displayValue = raw ? '是' : '否'
        }
      }
      return { key: f.key, label: f.label, value: displayValue, isEmpty: !hasValue }
    })
})

const showOccupantPanel = computed(() => {
  const node = selectedNode.value
  if (!node) return false
  return node.occupiable || node.hasCapacity
})

const placeTabs = computed(() => {
  const tabs: { key: string; label: string; count?: number }[] = [
    { key: 'children', label: '子场所', count: childPlaces.value.length },
    { key: 'orgs', label: '关联组织', count: placeOrgRelations.value.length },
  ]
  if (showOccupantPanel.value) {
    tabs.push({ key: 'occupants', label: '入住管理', count: occupants.value.length })
  }
  if (showBookingPanel.value) {
    tabs.push({ key: 'bookings', label: '预订管理', count: bookings.value.filter((b: any) => b.status === 1 || b.status === 2).length })
  }
  tabs.push({ key: 'info', label: '基本信息' })
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
    allPlaceTypes.value = await universalPlaceTypeApi.getAll()
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

async function loadChildPlaces(parentId: number) {
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
  placeOrgRelations.value = []
  bookings.value = []
  loadChildPlaces(node.id)
  loadPlaceOrgRelations(node.id)
  // Load occupants if this node supports them
  if (node.occupiable || node.hasCapacity) {
    loadOccupants(node.id)
  }
  // Load bookings if bookable
  if (node.bookable) {
    loadBookings(node.id)
  }
}

function selectPlace(place: UniversalPlace) {
  const node = findNode(treeData.value, place.id)
  if (node) {
    selectedNodeId.value = node.id
    showHistory.value = false
    occupants.value = []
    occupantHistory.value = []
    placeOrgRelations.value = []
    bookings.value = []
    loadChildPlaces(place.id)
    loadPlaceOrgRelations(place.id)
    if (node.occupiable || node.hasCapacity) {
      loadOccupants(place.id)
    }
    if (node.bookable) {
      loadBookings(place.id)
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

async function changeStatus(id: number, status: number) {
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
async function loadOccupants(placeId: number) {
  try {
    occupants.value = await universalPlaceApi.getOccupants(placeId)
  } catch {
    occupants.value = []
  }
}

async function loadOccupantHistory(placeId: number) {
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

<style scoped>
.detail-cell {
  display: flex;
  align-items: baseline;
  gap: 6px;
  border-bottom: 1px solid #f3f4f6;
  padding: 5px 10px;
  min-height: 28px;
}
.detail-label {
  flex-shrink: 0;
  font-size: 11px;
  color: #9ca3af;
  white-space: nowrap;
}
.detail-val {
  font-size: 12px;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.detail-link {
  font-size: 11px;
  color: #3b82f6;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}
.detail-link:hover {
  color: #2563eb;
  text-decoration: underline;
}
/* ===== Check-in Dialog ===== */
.ci-dialog :deep(.el-dialog) { border-radius: 12px; }
.ci-dialog :deep(.el-dialog__header) { padding: 0; margin: 0; }
.ci-dialog :deep(.el-dialog__headerbtn) { top: 14px; right: 16px; z-index: 2; }
.ci-dialog :deep(.el-dialog__body) { padding: 14px 20px 8px; }
.ci-dialog :deep(.el-dialog__footer) { padding: 10px 20px 14px; }

/* header */
.ci-header { display: flex; align-items: center; justify-content: space-between; padding: 14px 40px 12px 20px; border-bottom: 1px solid #f3f4f6; }
.ci-header-left { display: flex; align-items: center; gap: 10px; }
.ci-header-title { font-size: 15px; font-weight: 600; color: #111827; }
.ci-header-place { font-size: 13px; color: #374151; font-weight: 500; }
.ci-header-type { font-size: 11px; color: #9ca3af; }
.ci-header-right { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.ci-header-cap { font-weight: 600; color: #374151; }
.ci-header-cap-sep { color: #d1d5db; margin: 0 1px; }
.ci-header-remain { font-weight: 500; padding: 1px 6px; border-radius: 4px; font-size: 11px; }
.ci-remain-ok { color: #059669; background: #ecfdf5; }
.ci-remain-full { color: #dc2626; background: #fef2f2; }
.ci-header-gender { font-size: 10px; font-weight: 500; color: #3b82f6; background: #eff6ff; padding: 1px 6px; border-radius: 4px; }

/* search + actions */
.ci-actions { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.ci-search { flex: 1; }
.ci-batch-btn { display: inline-flex; align-items: center; gap: 4px; padding: 0 12px; height: 32px; font-size: 12px; font-weight: 500; color: #374151; background: #fff; border: 1px solid #e5e7eb; border-radius: 6px; cursor: pointer; white-space: nowrap; transition: all 0.15s; }
.ci-batch-btn:hover { border-color: #3b82f6; color: #3b82f6; }

/* dropdown option row */
.ci-opt { display: grid; grid-template-columns: 64px 80px 1fr 32px; gap: 6px; align-items: center; font-size: 12px; line-height: 1.5; }
.ci-opt-name { color: #111827; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ci-opt-user { color: #6b7280; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ci-opt-org  { color: #9ca3af; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ci-opt-gender { text-align: center; font-size: 11px; }

/* list container */
.ci-list { border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden; }
.ci-list-header { display: flex; align-items: center; justify-content: space-between; padding: 6px 12px; background: #f9fafb; border-bottom: 1px solid #f3f4f6; font-size: 12px; color: #6b7280; }
.ci-list-header b { color: #111827; }
.ci-auto-btn { font-size: 11px; color: #3b82f6; background: none; border: none; cursor: pointer; padding: 0; }
.ci-auto-btn:hover { text-decoration: underline; }
.ci-clear-btn { font-size: 11px; color: #ef4444; background: none; border: none; cursor: pointer; padding: 0; }
.ci-clear-btn:hover { text-decoration: underline; }
.ci-list-body { max-height: 260px; overflow-y: auto; }
.ci-list-empty { padding: 32px 0; text-align: center; font-size: 12px; color: #9ca3af; }

/* list grid rows */
.ci-list-thead, .ci-list-row { display: grid; grid-template-columns: 28px 70px 80px 1fr 36px 72px 28px; align-items: center; padding: 0 12px; font-size: 12px; }
.ci-list-thead { height: 28px; background: #f9fafb; border-bottom: 1px solid #f3f4f6; color: #9ca3af; font-size: 11px; font-weight: 500; position: sticky; top: 0; z-index: 1; }
.ci-list-row { height: 34px; border-bottom: 1px solid #fafafa; transition: background 0.1s; }
.ci-list-row:hover { background: #f9fafb; }
.ci-list-row:last-child { border-bottom: none; }
.ci-c-idx { text-align: center; color: #d1d5db; font-size: 11px; }
.ci-c-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ci-c-user { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ci-c-org { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ci-c-gender { text-align: center; font-size: 11px; }
.ci-c-pos { }
.ci-c-act { display: flex; justify-content: center; }
.ci-text-bold { color: #111827; font-weight: 500; }
.ci-text-dim { color: #9ca3af; }
.ci-pos-select { width: 100%; height: 22px; padding: 0 2px; font-size: 11px; border: 1px solid #e5e7eb; border-radius: 4px; outline: none; color: #374151; background: #fff; cursor: pointer; }
.ci-pos-select:focus { border-color: #93c5fd; }
.ci-rm-btn { color: #d1d5db; background: none; border: none; cursor: pointer; padding: 0; display: flex; transition: color 0.15s; }
.ci-rm-btn:hover { color: #ef4444; }

/* remark */
.ci-remark { display: block; width: 100%; margin-top: 10px; padding: 6px 10px; font-size: 12px; color: #374151; border: 1px solid #e5e7eb; border-radius: 6px; outline: none; }
.ci-remark::placeholder { color: #d1d5db; }
.ci-remark:focus { border-color: #93c5fd; }
.ci-footer { display: flex; justify-content: flex-end; gap: 8px; }

/* ===== Swap Dialog ===== */
.sw-dialog :deep(.el-dialog) { border-radius: 16px; overflow: hidden; box-shadow: 0 20px 60px rgba(0,0,0,0.13); }
.sw-dialog :deep(.el-dialog__header) { display: none; }
.sw-dialog :deep(.el-dialog__body) { padding: 0; }
.sw-dialog :deep(.el-dialog__footer) { padding: 12px 24px; border-top: 1px solid #f0f0f0; }
.sw-split { display: flex; }
.sw-left { width: 170px; flex-shrink: 0; display: flex; flex-direction: column; align-items: center; padding: 28px 16px 16px; background: linear-gradient(160deg, rgba(139,92,246,0.05), rgba(139,92,246,0.14)); border-right: 1px solid rgba(0,0,0,0.05); }
.sw-avatar { width: 42px; height: 42px; border-radius: 10px; background: rgba(255,255,255,0.85); display: flex; align-items: center; justify-content: center; box-shadow: 0 1px 4px rgba(0,0,0,0.06); margin-bottom: 8px; }
.sw-name { font-size: 15px; font-weight: 600; color: #1f2937; text-align: center; margin-bottom: 4px; }
.sw-tag { font-size: 11px; font-weight: 500; padding: 2px 8px; border-radius: 8px; background: rgba(255,255,255,0.7); color: #6b7280; margin-bottom: 12px; }
.sw-divider { width: 100%; height: 1px; background: rgba(0,0,0,0.07); margin-bottom: 12px; }
.sw-rows { width: 100%; }
.sw-row { display: flex; align-items: baseline; gap: 6px; margin-bottom: 6px; font-size: 13px; }
.sw-k { color: #9ca3af; flex-shrink: 0; }
.sw-v { color: #374151; font-weight: 500; }
.sw-right { flex: 1; min-width: 0; padding: 24px 28px; max-height: 50vh; overflow-y: auto; }
.sw-hd { font-size: 15px; font-weight: 600; color: #1f2937; margin-bottom: 16px; }
.sw-list { display: flex; flex-direction: column; gap: 8px; }
.sw-card { display: flex; align-items: center; justify-content: space-between; padding: 10px 14px; border: 1.5px solid #e5e7eb; border-radius: 10px; background: #fff; cursor: pointer; transition: all 0.15s; }
.sw-card:hover { border-color: #8b5cf6; background: #faf5ff; }
.sw-card-name { font-size: 14px; font-weight: 600; color: #1f2937; margin-right: 8px; }
.sw-card-pos { font-size: 12px; color: #9ca3af; }
.sw-card-act { font-size: 12px; font-weight: 500; color: #8b5cf6; }
.sw-empty { padding: 24px 0; text-align: center; font-size: 13px; color: #9ca3af; }
.sw-footer { display: flex; justify-content: flex-end; gap: 8px; }
</style>

<style>
/* Non-scoped: Element Plus teleports popper to body */
.ci-user-popper { min-width: 380px !important; }
.ci-user-popper .el-select-dropdown__item { padding: 5px 12px; height: auto; line-height: 1.5; }
/* Floor plan dialog: remove body padding */
.fp-dialog .el-dialog__header { padding: 0 !important; margin: 0 !important; }
.fp-dialog .el-dialog__body { padding: 0 !important; }
.fp-dialog .el-dialog__headerbtn { display: none; }
</style>
