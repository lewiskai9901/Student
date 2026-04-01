<template>
  <div class="min-h-full bg-gray-50 p-6">
    <!-- Header -->
    <div class="mb-4">
      <h1 class="text-xl font-semibold text-gray-900">宿舍管理</h1>
      <p class="mt-0.5 text-sm text-gray-500">管理宿舍楼栋、房间与学生入住</p>
    </div>

    <!-- Stat Bar -->
    <div class="mb-4 flex items-center gap-4 rounded-lg border border-gray-200 bg-white px-5 py-2.5">
      <span class="text-sm text-gray-500">楼栋 <span class="font-semibold text-gray-900">{{ stats.buildingCount }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">房间 <span class="font-semibold text-gray-900">{{ stats.roomCount }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">总床位 <span class="font-semibold text-gray-900">{{ stats.totalCapacity }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">已入住 <span class="font-semibold text-gray-900">{{ stats.totalOccupancy }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">入住率 <span class="font-semibold" :class="occupancyRateClass">{{ stats.occupancyRate.toFixed(1) }}%</span></span>
    </div>

    <!-- Content: Building Cards + Room List -->
    <div class="flex gap-4">
      <!-- Left: Building List -->
      <div class="w-72 flex-shrink-0">
        <div class="rounded-lg border border-gray-200 bg-white">
          <div class="flex items-center justify-between border-b border-gray-100 px-4 py-3">
            <span class="text-sm font-medium text-gray-700">宿舍楼</span>
            <span class="text-xs text-gray-400">{{ filteredBuildings.length }}栋</span>
          </div>
          <!-- Gender filter + Search -->
          <div class="flex items-center gap-2 border-b border-gray-100 px-3 py-2">
            <div class="flex overflow-hidden rounded-md border border-gray-200 text-[11px]">
              <button class="px-2 py-1 transition-colors" :class="genderFilter === 'all' ? 'bg-blue-50 text-blue-600 font-medium' : 'text-gray-500 hover:bg-gray-50'" @click="genderFilter = 'all'">全部</button>
              <button class="border-l border-gray-200 px-2 py-1 transition-colors" :class="genderFilter === 'male' ? 'bg-blue-50 text-blue-600 font-medium' : 'text-gray-500 hover:bg-gray-50'" @click="genderFilter = 'male'">男</button>
              <button class="border-l border-gray-200 px-2 py-1 transition-colors" :class="genderFilter === 'female' ? 'bg-blue-50 text-blue-600 font-medium' : 'text-gray-500 hover:bg-gray-50'" @click="genderFilter = 'female'">女</button>
            </div>
            <input
              v-model="buildingSearch"
              type="text"
              placeholder="搜索..."
              class="h-7 flex-1 rounded-md border border-gray-200 px-2 text-xs focus:border-blue-500 focus:outline-none"
            />
          </div>
          <!-- Loading -->
          <div v-if="loading" class="flex items-center justify-center py-12">
            <div class="h-6 w-6 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
          </div>
          <!-- Building Cards -->
          <div v-else class="max-h-[calc(100vh-320px)] overflow-y-auto">
            <div
              v-for="building in filteredBuildings"
              :key="building.id"
              class="cursor-pointer border-b border-gray-50 px-4 py-3 transition-colors"
              :class="selectedBuildingId === building.id ? 'bg-blue-50' : 'hover:bg-gray-50'"
              @click="selectBuilding(building)"
            >
              <div class="flex items-center justify-between">
                <div class="min-w-0 flex-1">
                  <div class="text-sm font-medium text-gray-900">{{ building.placeName }}</div>
                  <div class="mt-0.5 flex items-center gap-2 text-[11px] text-gray-400">
                    <span>{{ (building.children || []).length }}层</span>
                    <span>{{ countBuildingRooms(building) }}间</span>
                    <span>{{ getBuildingCapacity(building) }}床</span>
                  </div>
                </div>
                <div class="ml-2 text-right">
                  <div class="text-xs font-medium" :class="getBuildingOccupancy(building) > 0 ? 'text-blue-600' : 'text-gray-400'">{{ getBuildingOccupancy(building) }}/{{ getBuildingCapacity(building) }}</div>
                  <div class="mt-0.5 h-1.5 w-14 overflow-hidden rounded-full bg-gray-200">
                    <div class="h-full rounded-full transition-all" :class="getOccupancyBarClass(building)" :style="{ width: getOccupancyPercent(building) + '%' }"></div>
                  </div>
                </div>
              </div>
              <!-- Gender badge -->
              <div class="mt-1 flex items-center gap-1.5">
                <span v-if="building.effectiveGender || building.gender" class="rounded px-1.5 py-0.5 text-[10px] font-medium" :class="genderBadgeClass(building.effectiveGender || building.gender)">
                  {{ genderLabel(building.effectiveGender || building.gender) }}
                </span>
              </div>
            </div>
            <div v-if="filteredBuildings.length === 0" class="py-8 text-center text-xs text-gray-400">
              暂无宿舍楼数据
            </div>
          </div>
        </div>
      </div>

      <!-- Right: Room List -->
      <div class="flex-1">
        <div class="rounded-lg border border-gray-200 bg-white">
          <!-- Room Header -->
          <div class="border-b border-gray-100 px-5 py-3">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <span class="text-sm font-medium text-gray-700">
                  {{ selectedBuilding ? selectedBuilding.placeName : '请选择宿舍楼' }}
                </span>
                <span v-if="selectedBuilding" class="text-xs text-gray-400">
                  共 {{ filteredRooms.length }} 间
                </span>
              </div>
              <div v-if="selectedBuilding" class="flex items-center gap-2">
                <!-- View mode switcher -->
                <div class="flex overflow-hidden rounded-md border border-gray-200 text-[11px]">
                  <button class="px-2 py-1 transition-colors" :class="roomViewMode === 'card' ? 'bg-blue-50 text-blue-600 font-medium' : 'text-gray-500 hover:bg-gray-50'" @click="roomViewMode = 'card'" title="卡片视图">卡片</button>
                  <button class="border-l border-gray-200 px-2 py-1 transition-colors" :class="roomViewMode === 'list' ? 'bg-blue-50 text-blue-600 font-medium' : 'text-gray-500 hover:bg-gray-50'" @click="roomViewMode = 'list'" title="列表视图">列表</button>
                  <button class="border-l border-gray-200 px-2 py-1 transition-colors" :class="roomViewMode === 'bed' ? 'bg-blue-50 text-blue-600 font-medium' : 'text-gray-500 hover:bg-gray-50'" @click="roomViewMode = 'bed'" title="床位图">床位</button>
                  <button class="border-l border-gray-200 px-2 py-1 transition-colors" :class="roomViewMode === 'stats' ? 'bg-blue-50 text-blue-600 font-medium' : 'text-gray-500 hover:bg-gray-50'" @click="roomViewMode = 'stats'" title="统计视图">统计</button>
                </div>
                <!-- Occupancy filter -->
                <div class="flex overflow-hidden rounded-md border border-gray-200 text-[11px]">
                  <button class="px-2 py-1 transition-colors" :class="occupancyFilter === 'all' ? 'bg-blue-50 text-blue-600 font-medium' : 'text-gray-500 hover:bg-gray-50'" @click="occupancyFilter = 'all'">全部</button>
                  <button class="border-l border-gray-200 px-2 py-1 transition-colors" :class="occupancyFilter === 'available' ? 'bg-blue-50 text-blue-600 font-medium' : 'text-gray-500 hover:bg-gray-50'" @click="occupancyFilter = 'available'">有空位</button>
                  <button class="border-l border-gray-200 px-2 py-1 transition-colors" :class="occupancyFilter === 'full' ? 'bg-blue-50 text-blue-600 font-medium' : 'text-gray-500 hover:bg-gray-50'" @click="occupancyFilter = 'full'">已满</button>
                  <button class="border-l border-gray-200 px-2 py-1 transition-colors" :class="occupancyFilter === 'empty' ? 'bg-blue-50 text-blue-600 font-medium' : 'text-gray-500 hover:bg-gray-50'" @click="occupancyFilter = 'empty'">空房</button>
                </div>
              </div>
            </div>
            <!-- Floor tabs -->
            <div v-if="selectedBuilding && floors.length > 0" class="mt-2 flex items-center gap-0 overflow-x-auto">
              <button
                class="relative whitespace-nowrap px-3 py-1.5 text-xs font-medium transition-colors"
                :class="selectedFloor === null ? 'text-blue-600' : 'text-gray-500 hover:text-gray-700'"
                @click="selectedFloor = null"
              >
                全部
                <div v-if="selectedFloor === null" class="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-600 rounded-full"></div>
              </button>
              <button
                v-for="floor in floors"
                :key="floor.id"
                class="relative whitespace-nowrap px-3 py-1.5 text-xs font-medium transition-colors"
                :class="selectedFloor === floor.id ? 'text-blue-600' : 'text-gray-500 hover:text-gray-700'"
                @click="selectedFloor = floor.id"
              >
                {{ floorShortName(floor) }}
                <span class="ml-1 text-[10px] text-gray-400">({{ countFloorRooms(floor) }})</span>
                <div v-if="selectedFloor === floor.id" class="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-600 rounded-full"></div>
              </button>
            </div>
          </div>

          <!-- Room Content -->
          <div v-if="!selectedBuilding" class="flex items-center justify-center py-20 text-sm text-gray-400">
            请从左侧选择宿舍楼查看房间
          </div>
          <div v-else-if="roomLoading" class="flex items-center justify-center py-20">
            <div class="h-6 w-6 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
          </div>
          <div v-else-if="filteredRooms.length === 0" class="flex items-center justify-center py-20 text-sm text-gray-400">
            暂无房间数据
          </div>

          <!-- ========== View: 卡片 ========== -->
          <div v-else-if="roomViewMode === 'card'" class="grid grid-cols-2 gap-3 p-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
            <div
              v-for="room in filteredRooms"
              :key="room.id"
              class="cursor-pointer rounded-lg border p-3 transition-all hover:shadow-md"
              :class="getRoomCardClass(room)"
              @click="openRoomDetail(room)"
            >
              <div class="flex items-center justify-between">
                <span class="text-sm font-semibold text-gray-900">{{ room.placeName }}</span>
                <span v-if="room.effectiveGender" class="rounded px-1 py-0.5 text-[9px] font-medium" :class="genderBadgeClass(room.effectiveGender)">
                  {{ genderLabel(room.effectiveGender) }}
                </span>
              </div>
              <div class="mt-1.5 flex items-center justify-between">
                <span class="text-xs text-gray-500">{{ room.currentOccupancy || 0 }}/{{ room.capacity || 0 }}</span>
                <span class="text-[10px]" :class="getRoomOccupancyTextClass(room)">{{ getRoomOccupancyLabel(room) }}</span>
              </div>
              <div class="mt-1.5 h-1 w-full overflow-hidden rounded-full bg-gray-200">
                <div class="h-full rounded-full transition-all" :class="getOccupancyBarClass(room)" :style="{ width: getOccupancyPercent(room) + '%' }"></div>
              </div>
              <div v-if="room.effectiveOrgUnitName" class="mt-1.5 truncate text-[10px] text-gray-400">{{ room.effectiveOrgUnitName }}</div>
            </div>
          </div>

          <!-- ========== View: 列表 ========== -->
          <div v-else-if="roomViewMode === 'list'" class="overflow-x-auto">
            <table class="w-full text-left text-xs">
              <thead>
                <tr class="border-b border-gray-200 bg-gray-50">
                  <th class="px-4 py-2.5 font-semibold text-gray-600">房间号</th>
                  <th class="px-4 py-2.5 font-semibold text-gray-600">楼层</th>
                  <th class="px-4 py-2.5 font-semibold text-gray-600 text-center">入住/容量</th>
                  <th class="px-4 py-2.5 font-semibold text-gray-600 text-center">入住率</th>
                  <th class="px-4 py-2.5 font-semibold text-gray-600 text-center">状态</th>
                  <th class="px-4 py-2.5 font-semibold text-gray-600">归属</th>
                  <th class="px-4 py-2.5 font-semibold text-gray-600 text-center">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="room in filteredRooms"
                  :key="room.id"
                  class="border-b border-gray-100 transition-colors hover:bg-gray-50"
                >
                  <td class="px-4 py-2.5 font-medium text-gray-900">{{ room.placeName }}</td>
                  <td class="px-4 py-2.5 text-gray-500">{{ getFloorName(room) }}</td>
                  <td class="px-4 py-2.5 text-center">
                    <span class="font-medium" :class="(room.currentOccupancy || 0) >= (room.capacity || 0) ? 'text-red-600' : 'text-gray-700'">
                      {{ room.currentOccupancy || 0 }}
                    </span>
                    <span class="text-gray-400">/{{ room.capacity || 0 }}</span>
                  </td>
                  <td class="px-4 py-2.5 text-center">
                    <div class="mx-auto flex items-center gap-1.5">
                      <div class="h-1.5 w-12 overflow-hidden rounded-full bg-gray-200">
                        <div class="h-full rounded-full transition-all" :class="getOccupancyBarClass(room)" :style="{ width: getOccupancyPercent(room) + '%' }"></div>
                      </div>
                      <span class="text-[10px] text-gray-400">{{ getOccupancyPercent(room) }}%</span>
                    </div>
                  </td>
                  <td class="px-4 py-2.5 text-center">
                    <span class="rounded-full px-2 py-0.5 text-[10px] font-medium" :class="getRoomStatusBadge(room)">
                      {{ getRoomOccupancyLabel(room) }}
                    </span>
                  </td>
                  <td class="px-4 py-2.5 text-gray-400">{{ room.effectiveOrgUnitName || '-' }}</td>
                  <td class="px-4 py-2.5 text-center">
                    <button class="text-blue-600 hover:text-blue-800" @click="openRoomDetail(room)">查看</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- ========== View: 床位图 ========== -->
          <div v-else-if="roomViewMode === 'bed'" class="p-4">
            <div v-for="room in filteredRooms" :key="room.id" class="mb-4 rounded-lg border border-gray-200 p-3">
              <div class="mb-2 flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <span class="text-sm font-semibold text-gray-900">{{ room.placeName }}</span>
                  <span class="text-[10px] text-gray-400">{{ room.currentOccupancy || 0 }}/{{ room.capacity || 0 }}</span>
                </div>
                <button class="text-xs text-blue-600 hover:text-blue-800" @click="openRoomDetail(room)">管理</button>
              </div>
              <!-- Bed grid -->
              <div class="flex flex-wrap gap-2">
                <div
                  v-for="bed in (room.capacity || 6)"
                  :key="bed"
                  class="flex h-14 w-20 flex-col items-center justify-center rounded-md border text-[10px] transition-colors"
                  :class="bed <= (room.currentOccupancy || 0) ? 'border-blue-300 bg-blue-50 text-blue-700' : 'border-dashed border-gray-300 bg-white text-gray-400'"
                >
                  <svg class="mb-0.5 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path v-if="bed <= (room.currentOccupancy || 0)" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V7M3 7l9-4 9 4" />
                  </svg>
                  <span>{{ bed <= (room.currentOccupancy || 0) ? bed + '号床' : '空床' }}</span>
                </div>
              </div>
            </div>
            <div v-if="filteredRooms.length > 20" class="py-4 text-center text-xs text-gray-400">
              显示前20间房间的床位图，如需查看更多请选择具体楼层
            </div>
          </div>

          <!-- ========== View: 统计 ========== -->
          <div v-else-if="roomViewMode === 'stats'" class="p-5 space-y-5">
            <!-- Floor occupancy comparison -->
            <div>
              <h4 class="mb-3 text-sm font-semibold text-gray-700">各楼层入住统计</h4>
              <div class="space-y-2">
                <div v-for="floor in floors" :key="floor.id" class="flex items-center gap-3">
                  <span class="w-16 text-right text-xs text-gray-500">{{ floorShortName(floor) }}</span>
                  <div class="flex-1 h-5 overflow-hidden rounded-full bg-gray-100">
                    <div
                      class="h-full rounded-full bg-blue-500 transition-all flex items-center justify-end pr-2"
                      :style="{ width: Math.max(getFloorOccupancyRate(floor), 2) + '%' }"
                    >
                      <span v-if="getFloorOccupancyRate(floor) > 15" class="text-[10px] text-white font-medium">{{ getFloorOccupancy(floor) }}/{{ getFloorCapacity(floor) }}</span>
                    </div>
                  </div>
                  <span class="w-12 text-right text-xs font-medium" :class="getFloorOccupancyRate(floor) >= 90 ? 'text-red-600' : getFloorOccupancyRate(floor) >= 60 ? 'text-amber-600' : 'text-gray-500'">
                    {{ getFloorOccupancyRate(floor).toFixed(0) }}%
                  </span>
                </div>
              </div>
            </div>
            <!-- Summary cards -->
            <div class="grid grid-cols-4 gap-3">
              <div class="rounded-lg border border-gray-200 p-3 text-center">
                <div class="text-2xl font-bold text-gray-900">{{ filteredRooms.length }}</div>
                <div class="mt-0.5 text-[11px] text-gray-500">房间总数</div>
              </div>
              <div class="rounded-lg border border-emerald-200 bg-emerald-50/50 p-3 text-center">
                <div class="text-2xl font-bold text-emerald-600">{{ filteredRooms.filter(r => (r.currentOccupancy || 0) > 0 && (r.currentOccupancy || 0) < (r.capacity || 0)).length }}</div>
                <div class="mt-0.5 text-[11px] text-gray-500">有空位</div>
              </div>
              <div class="rounded-lg border border-blue-200 bg-blue-50/50 p-3 text-center">
                <div class="text-2xl font-bold text-blue-600">{{ filteredRooms.filter(r => (r.currentOccupancy || 0) >= (r.capacity || 1)).length }}</div>
                <div class="mt-0.5 text-[11px] text-gray-500">已满房间</div>
              </div>
              <div class="rounded-lg border border-gray-200 p-3 text-center">
                <div class="text-2xl font-bold text-gray-400">{{ filteredRooms.filter(r => (r.currentOccupancy || 0) === 0).length }}</div>
                <div class="mt-0.5 text-[11px] text-gray-500">空房间</div>
              </div>
            </div>
            <!-- Room type distribution (by capacity) -->
            <div>
              <h4 class="mb-3 text-sm font-semibold text-gray-700">房间规格分布</h4>
              <div class="flex flex-wrap gap-2">
                <div
                  v-for="[cap, count] in roomCapacityDistribution"
                  :key="cap"
                  class="rounded-lg border border-gray-200 px-4 py-2 text-center"
                >
                  <div class="text-lg font-bold text-gray-900">{{ count }}</div>
                  <div class="text-[11px] text-gray-500">{{ cap }}人间</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Room Detail Dialog -->
    <el-dialog
      v-model="roomDetailVisible"
      :title="currentRoom?.placeName || '房间详情'"
      width="640px"
      :close-on-click-modal="false"
    >
      <div v-if="currentRoom" class="space-y-4">
        <!-- Room info -->
        <div class="flex items-center gap-4 text-sm text-gray-600">
          <span>编号 <strong class="text-gray-900">{{ currentRoom.placeCode }}</strong></span>
          <div class="h-3 w-px bg-gray-200" />
          <span>容量 <strong class="text-gray-900">{{ currentRoom.capacity || 0 }}</strong></span>
          <div class="h-3 w-px bg-gray-200" />
          <span>已入住 <strong class="text-gray-900">{{ currentRoom.currentOccupancy || 0 }}</strong></span>
          <div v-if="currentRoom.effectiveGender" class="h-3 w-px bg-gray-200" />
          <span v-if="currentRoom.effectiveGender" class="rounded px-1.5 py-0.5 text-xs font-medium" :class="genderBadgeClass(currentRoom.effectiveGender)">
            {{ genderLabel(currentRoom.effectiveGender) }}
          </span>
        </div>

        <!-- Occupants list -->
        <div>
          <div class="mb-2 flex items-center justify-between">
            <span class="text-sm font-medium text-gray-700">入住人员</span>
            <button
              v-if="(currentRoom.currentOccupancy || 0) < (currentRoom.capacity || 0)"
              class="rounded-md bg-blue-600 px-3 py-1 text-xs font-medium text-white hover:bg-blue-700"
              @click="openCheckInDialog"
            >
              办理入住
            </button>
          </div>

          <div v-if="occupantsLoading" class="flex justify-center py-4">
            <div class="h-5 w-5 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
          </div>
          <div v-else-if="occupants.length === 0" class="py-4 text-center text-xs text-gray-400">
            暂无入住人员
          </div>
          <table v-else class="w-full text-sm">
            <thead>
              <tr class="border-b border-gray-100 bg-gray-50/50">
                <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">位号</th>
                <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">姓名</th>
                <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">学号/工号</th>
                <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">组织</th>
                <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">入住时间</th>
                <th class="px-3 py-2 text-center text-xs font-medium text-gray-500">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="occ in occupants" :key="occ.id" class="border-b border-gray-50 hover:bg-gray-50/50">
                <td class="px-3 py-2 text-xs text-gray-600">{{ occ.positionNo || '-' }}</td>
                <td class="px-3 py-2 font-medium text-gray-900">{{ occ.occupantName || '-' }}</td>
                <td class="px-3 py-2 text-xs text-gray-600">{{ occ.username || '-' }}</td>
                <td class="px-3 py-2 text-xs text-gray-500">{{ occ.orgUnitName || '-' }}</td>
                <td class="px-3 py-2 text-xs text-gray-500">{{ formatDate(occ.checkInTime) }}</td>
                <td class="px-3 py-2 text-center">
                  <button
                    class="rounded px-2 py-0.5 text-xs text-red-500 hover:bg-red-50"
                    @click="handleCheckOut(occ)"
                  >退出</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </el-dialog>

    <!-- Check-In Dialog -->
    <el-dialog
      v-model="checkInDialogVisible"
      title="办理入住"
      width="480px"
      :close-on-click-modal="false"
    >
      <div class="space-y-4">
        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">入住人员类型</label>
          <select
            v-model="checkInForm.occupantType"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option value="STUDENT">学生</option>
            <option value="TEACHER">教师</option>
            <option value="STAFF">职工</option>
          </select>
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">搜索人员</label>
          <div class="relative">
            <input
              v-model="personSearchQuery"
              type="text"
              placeholder="输入姓名或学号/工号搜索..."
              class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              @input="handlePersonSearch"
            />
          </div>
          <!-- Search results -->
          <div v-if="personSearchResults.length > 0" class="mt-2 max-h-40 overflow-y-auto rounded-md border border-gray-200">
            <div
              v-for="person in personSearchResults"
              :key="person.id"
              class="cursor-pointer px-3 py-2 text-sm hover:bg-blue-50"
              :class="checkInForm.occupantId === person.id ? 'bg-blue-50 font-medium' : ''"
              @click="selectPerson(person)"
            >
              <span class="text-gray-900">{{ person.realName || person.name }}</span>
              <span class="ml-2 text-xs text-gray-500">{{ person.studentNo || person.username }}</span>
              <span v-if="person.className || person.orgUnitName" class="ml-2 text-xs text-gray-400">{{ person.className || person.orgUnitName }}</span>
            </div>
          </div>
          <div v-if="checkInForm.occupantName" class="mt-2 rounded-md bg-blue-50 px-3 py-2 text-sm text-blue-700">
            已选择: {{ checkInForm.occupantName }} ({{ checkInForm.username || '' }})
          </div>
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">床位号 (可选)</label>
          <input
            v-model="checkInForm.positionNo"
            type="text"
            placeholder="如: 1, 2, 3..."
            class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">备注 (可选)</label>
          <input
            v-model="checkInForm.remark"
            type="text"
            placeholder="备注信息"
            class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="checkInDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="checkInLoading" :disabled="!checkInForm.occupantId" @click="handleCheckIn">
            确认入住
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { universalPlaceApi } from '@/api/universalPlace'
import type { PlaceTreeNode, PlaceOccupant } from '@/types/universalPlace'
import { getStudents } from '@/api/student'

// ==================== State ====================

const loading = ref(false)
const roomLoading = ref(false)
const occupantsLoading = ref(false)
const checkInLoading = ref(false)

const buildings = ref<PlaceTreeNode[]>([])
const buildingSearch = ref('')
const genderFilter = ref<'all' | 'male' | 'female'>('all')
const selectedBuildingId = ref<number | string | null>(null)
const selectedBuilding = ref<PlaceTreeNode | null>(null)

const floors = ref<PlaceTreeNode[]>([])
const selectedFloor = ref<number | string | null>(null)
const rooms = ref<PlaceTreeNode[]>([])
const occupancyFilter = ref('all')
const roomViewMode = ref<'card' | 'list' | 'bed' | 'stats'>('card')

const roomDetailVisible = ref(false)
const currentRoom = ref<PlaceTreeNode | null>(null)
const occupants = ref<PlaceOccupant[]>([])

const checkInDialogVisible = ref(false)
const checkInForm = ref({
  occupantType: 'STUDENT' as string,
  occupantId: null as number | string | null,
  occupantName: '',
  username: '',
  orgUnitName: '',
  gender: undefined as number | undefined,
  positionNo: '',
  remark: ''
})
const personSearchQuery = ref('')
const personSearchResults = ref<any[]>([])

// ==================== Stats ====================

const stats = computed(() => {
  const buildingCount = buildings.value.length
  const roomCount = rooms.value.length
  // Aggregate from room level for accurate capacity (building capacity may be wrong)
  let totalCapacity = 0
  let totalOccupancy = 0

  if (rooms.value.length > 0) {
    // When a building is selected, use loaded rooms
    for (const r of rooms.value) {
      totalCapacity += r.capacity || 0
      totalOccupancy += r.currentOccupancy || 0
    }
  } else {
    // Fallback: sum from all buildings' children recursively
    for (const b of buildings.value) {
      const sumFromChildren = (children: any[]): { cap: number; occ: number } => {
        let cap = 0, occ = 0
        for (const c of children) {
          if (c.occupiable || c.hasCapacity) {
            cap += c.capacity || 0
            occ += c.currentOccupancy || 0
          }
          if (c.children?.length) {
            const sub = sumFromChildren(c.children)
            cap += sub.cap
            occ += sub.occ
          }
        }
        return { cap, occ }
      }
      if (b.children?.length) {
        const { cap, occ } = sumFromChildren(b.children)
        totalCapacity += cap
        totalOccupancy += occ
      }
    }
  }

  const occupancyRate = totalCapacity > 0 ? (totalOccupancy / totalCapacity) * 100 : 0

  return { buildingCount, roomCount, totalCapacity, totalOccupancy, occupancyRate }
})

const occupancyRateClass = computed(() => {
  const rate = stats.value.occupancyRate
  if (rate >= 90) return 'text-red-600'
  if (rate >= 70) return 'text-amber-600'
  return 'text-emerald-600'
})

// ==================== Computed ====================

const filteredBuildings = computed(() => {
  let result = buildings.value

  // Gender filter
  if (genderFilter.value === 'male') {
    result = result.filter(b => {
      const g = b.effectiveGender || b.gender
      return g === 1 || g === 'MALE'
    })
  } else if (genderFilter.value === 'female') {
    result = result.filter(b => {
      const g = b.effectiveGender || b.gender
      return g === 2 || g === 'FEMALE'
    })
  }

  // Search filter
  if (buildingSearch.value) {
    const q = buildingSearch.value.toLowerCase()
    result = result.filter(b =>
      b.placeName.toLowerCase().includes(q) || b.placeCode?.toLowerCase().includes(q)
    )
  }

  return result
})

const filteredRooms = computed(() => {
  let result = rooms.value

  // Floor filter
  if (selectedFloor.value) {
    result = result.filter(r => r.parentId === selectedFloor.value)
  }

  // Occupancy filter
  if (occupancyFilter.value === 'available') {
    result = result.filter(r => (r.currentOccupancy || 0) < (r.capacity || 0) && (r.currentOccupancy || 0) > 0)
  } else if (occupancyFilter.value === 'full') {
    result = result.filter(r => (r.capacity || 0) > 0 && (r.currentOccupancy || 0) >= (r.capacity || 0))
  } else if (occupancyFilter.value === 'empty') {
    result = result.filter(r => (r.currentOccupancy || 0) === 0)
  }

  return result
})

// ==================== Load Data ====================

const loadBuildings = async () => {
  loading.value = true
  try {
    const tree = await universalPlaceApi.getTree()
    // Find dormitory buildings from the tree
    // Dormitory buildings can be at various levels; we look for buildings with dormitory-related types
    const dormBuildings: PlaceTreeNode[] = []
    const isDormBuilding = (node: PlaceTreeNode): boolean => {
      const typeCode = (node.typeCode || '').toUpperCase()
      const typeName = (node.typeName || '').toLowerCase()
      const nameLower = (node.placeName || '').toLowerCase()
      // Match by typeCode first (most reliable)
      if (['DORM_BUILDING', 'DORMITORY', 'DORM'].includes(typeCode)) return true
      // Fallback to name/type string matching
      return typeName.includes('宿舍') || nameLower.includes('宿舍') || nameLower.includes('公寓')
    }
    const extractDormBuildings = (nodes: PlaceTreeNode[]) => {
      for (const node of nodes) {
        if (isDormBuilding(node)) {
          dormBuildings.push(node)
        }
        // Also check children recursively for campus -> building hierarchy
        if (node.children && node.children.length > 0) {
          extractDormBuildings(node.children)
        }
      }
    }
    extractDormBuildings(tree)
    buildings.value = dormBuildings
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '加载楼栋数据失败')
  } finally {
    loading.value = false
  }
}

const loadRooms = async (buildingId: number | string) => {
  roomLoading.value = true
  rooms.value = []
  floors.value = []
  selectedFloor.value = null
  try {
    const children = await universalPlaceApi.getChildren(buildingId)
    // Children might be floors (with their own room children) or directly rooms
    const floorNodes: PlaceTreeNode[] = []
    const roomNodes: PlaceTreeNode[] = []

    for (const child of children) {
      const typeLower = (child.typeCode || child.typeName || '').toLowerCase()
      if (typeLower.includes('floor') || typeLower.includes('楼层')) {
        floorNodes.push(child as PlaceTreeNode)
        // Load rooms from each floor
        try {
          const floorChildren = await universalPlaceApi.getChildren(child.id)
          for (const room of floorChildren) {
            roomNodes.push({ ...room, parentId: child.id } as PlaceTreeNode)
          }
        } catch {
          // Skip failed floor
        }
      } else {
        // Direct room children
        roomNodes.push(child as PlaceTreeNode)
      }
    }

    floors.value = floorNodes
    rooms.value = roomNodes
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '加载房间数据失败')
  } finally {
    roomLoading.value = false
  }
}

// ==================== Building Capacity Helpers ====================

function sumCapacity(children: any[]): number {
  let total = 0
  for (const c of children) {
    if (c.occupiable || c.hasCapacity) total += c.capacity || 0
    if (c.children?.length) total += sumCapacity(c.children)
  }
  return total
}

function sumOccupancy(children: any[]): number {
  let total = 0
  for (const c of children) {
    if (c.occupiable || c.hasCapacity) total += c.currentOccupancy || 0
    if (c.children?.length) total += sumOccupancy(c.children)
  }
  return total
}

function getBuildingCapacity(building: PlaceTreeNode): number {
  // Prefer aggregating from children (rooms) for accuracy
  if (building.children?.length) return sumCapacity(building.children)
  return building.capacity || 0
}

function getBuildingOccupancy(building: PlaceTreeNode): number {
  if (building.children?.length) return sumOccupancy(building.children)
  return building.currentOccupancy || 0
}

function countBuildingRooms(building: PlaceTreeNode): number {
  let count = 0
  for (const floor of (building.children || [])) {
    count += (floor.children || []).length
  }
  return count
}

function floorShortName(floor: PlaceTreeNode): string {
  // Extract short name like "1层" from "男1栋1层"
  const name = floor.placeName || ''
  const match = name.match(/(\d+)\s*[层楼F]/i)
  if (match) return match[1] + '层'
  return name
}

function countFloorRooms(floor: PlaceTreeNode): number {
  return rooms.value.filter(r => r.parentId === floor.id).length
}

function getFloorName(room: PlaceTreeNode): string {
  const floor = floors.value.find(f => f.id === room.parentId)
  return floor ? floorShortName(floor) : '-'
}

function getRoomStatusBadge(room: PlaceTreeNode): string {
  const occ = room.currentOccupancy || 0
  const cap = room.capacity || 0
  if (occ >= cap && cap > 0) return 'bg-red-100 text-red-700'
  if (occ > 0) return 'bg-blue-100 text-blue-700'
  return 'bg-gray-100 text-gray-500'
}

function getFloorCapacity(floor: PlaceTreeNode): number {
  return rooms.value.filter(r => r.parentId === floor.id).reduce((sum, r) => sum + (r.capacity || 0), 0)
}

function getFloorOccupancy(floor: PlaceTreeNode): number {
  return rooms.value.filter(r => r.parentId === floor.id).reduce((sum, r) => sum + (r.currentOccupancy || 0), 0)
}

function getFloorOccupancyRate(floor: PlaceTreeNode): number {
  const cap = getFloorCapacity(floor)
  if (cap === 0) return 0
  return (getFloorOccupancy(floor) / cap) * 100
}

const roomCapacityDistribution = computed(() => {
  const map = new Map<number, number>()
  for (const r of filteredRooms.value) {
    const cap = r.capacity || 0
    if (cap > 0) map.set(cap, (map.get(cap) || 0) + 1)
  }
  return Array.from(map.entries()).sort((a, b) => a[0] - b[0])
})

// ==================== Selection ====================

const selectBuilding = (building: PlaceTreeNode) => {
  selectedBuildingId.value = building.id
  selectedBuilding.value = building
  loadRooms(building.id)
}

// ==================== Room Detail ====================

const openRoomDetail = async (room: PlaceTreeNode) => {
  currentRoom.value = room
  roomDetailVisible.value = true
  await loadOccupants(room.id)
}

const loadOccupants = async (placeId: number | string) => {
  occupantsLoading.value = true
  try {
    occupants.value = await universalPlaceApi.getOccupants(placeId)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '加载入住人员失败')
    occupants.value = []
  } finally {
    occupantsLoading.value = false
  }
}

const handleCheckOut = async (occ: PlaceOccupant) => {
  if (!currentRoom.value) return
  try {
    await ElMessageBox.confirm(
      `确定要将 ${occ.occupantName || '该人员'} 从 ${currentRoom.value.placeName} 退出吗？`,
      '确认退出',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await universalPlaceApi.checkOut(currentRoom.value.id, occ.id)
    ElMessage.success('已退出')
    await loadOccupants(currentRoom.value.id)
    // Refresh building data
    if (selectedBuildingId.value) {
      await loadRooms(selectedBuildingId.value)
    }
    loadBuildings()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '操作失败')
    }
  }
}

// ==================== Check In ====================

const openCheckInDialog = () => {
  checkInForm.value = {
    occupantType: 'STUDENT',
    occupantId: null,
    occupantName: '',
    username: '',
    orgUnitName: '',
    gender: undefined,
    positionNo: '',
    remark: ''
  }
  personSearchQuery.value = ''
  personSearchResults.value = []
  checkInDialogVisible.value = true
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
const handlePersonSearch = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    if (!personSearchQuery.value || personSearchQuery.value.length < 1) {
      personSearchResults.value = []
      return
    }
    try {
      if (checkInForm.value.occupantType === 'STUDENT') {
        const result = await getStudents({
          name: personSearchQuery.value,
          pageNum: 1,
          pageSize: 10
        })
        personSearchResults.value = (result.records || []).map((s: any) => ({
          id: s.id,
          realName: s.realName || s.name,
          studentNo: s.studentNo,
          className: s.className,
          gender: s.gender
        }))
      }
      // For TEACHER/STAFF, would use user API - not adding here to keep scope
    } catch {
      personSearchResults.value = []
    }
  }, 300)
}

const selectPerson = (person: any) => {
  checkInForm.value.occupantId = person.id
  checkInForm.value.occupantName = person.realName || person.name
  checkInForm.value.username = person.studentNo || person.username || ''
  checkInForm.value.orgUnitName = person.className || person.orgUnitName || ''
  checkInForm.value.gender = person.gender
  personSearchResults.value = []
}

const handleCheckIn = async () => {
  if (!currentRoom.value || !checkInForm.value.occupantId) return
  checkInLoading.value = true
  try {
    await universalPlaceApi.checkIn(currentRoom.value.id, {
      occupantType: checkInForm.value.occupantType,
      occupantId: checkInForm.value.occupantId,
      occupantName: checkInForm.value.occupantName,
      username: checkInForm.value.username,
      orgUnitName: checkInForm.value.orgUnitName,
      gender: checkInForm.value.gender,
      positionNo: checkInForm.value.positionNo || undefined,
      remark: checkInForm.value.remark || undefined
    })
    ElMessage.success('入住成功')
    checkInDialogVisible.value = false
    await loadOccupants(currentRoom.value.id)
    // Refresh
    if (selectedBuildingId.value) {
      await loadRooms(selectedBuildingId.value)
    }
    loadBuildings()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '入住失败')
  } finally {
    checkInLoading.value = false
  }
}

// ==================== Helpers ====================

const getOccupancyPercent = (place: PlaceTreeNode) => {
  // For buildings, aggregate from children
  const cap = place.children?.length ? getBuildingCapacity(place) : (place.capacity || 0)
  const occ = place.children?.length ? getBuildingOccupancy(place) : (place.currentOccupancy || 0)
  if (!cap || cap === 0) return 0
  return Math.min(100, Math.round((occ / cap) * 100))
}

const getOccupancyBarClass = (place: PlaceTreeNode) => {
  const pct = getOccupancyPercent(place)
  if (pct >= 100) return 'bg-red-500'
  if (pct >= 80) return 'bg-amber-500'
  if (pct > 0) return 'bg-blue-500'
  return 'bg-gray-300'
}

const getRoomCardClass = (room: PlaceTreeNode) => {
  const pct = getOccupancyPercent(room)
  if (room.status === 0) return 'border-gray-300 bg-gray-50 opacity-60'
  if (room.status === 2) return 'border-amber-300 bg-amber-50'
  if (pct >= 100) return 'border-emerald-300 bg-emerald-50'
  if (pct > 0) return 'border-blue-200 bg-blue-50/30'
  return 'border-gray-200 bg-white'
}

const getRoomOccupancyLabel = (room: PlaceTreeNode) => {
  if (room.status === 0) return '停用'
  if (room.status === 2) return '维护中'
  const cap = room.capacity || 0
  const occ = room.currentOccupancy || 0
  if (cap === 0) return '-'
  if (occ >= cap) return '已满'
  if (occ === 0) return '空房'
  return `余${cap - occ}`
}

const getRoomOccupancyTextClass = (room: PlaceTreeNode) => {
  if (room.status !== 1) return 'text-gray-400'
  const cap = room.capacity || 0
  const occ = room.currentOccupancy || 0
  if (occ >= cap) return 'text-emerald-600 font-medium'
  if (occ === 0) return 'text-gray-400'
  return 'text-blue-600'
}

const genderBadgeClass = (gender?: string) => {
  if (gender === 'MALE') return 'bg-blue-100 text-blue-700'
  if (gender === 'FEMALE') return 'bg-pink-100 text-pink-700'
  return 'bg-gray-100 text-gray-600'
}

const genderLabel = (gender?: string) => {
  if (gender === 'MALE') return '男'
  if (gender === 'FEMALE') return '女'
  if (gender === 'MIXED') return '混'
  return ''
}

const statusBadgeClass = (status?: number) => {
  if (status === 1) return 'bg-green-100 text-green-700'
  if (status === 2) return 'bg-amber-100 text-amber-700'
  return 'bg-gray-100 text-gray-600'
}

const statusLabel = (status?: number) => {
  if (status === 1) return '正常'
  if (status === 2) return '维护中'
  return '停用'
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  try {
    return new Date(dateStr).toLocaleDateString('zh-CN')
  } catch {
    return dateStr
  }
}

// ==================== Watch ====================

watch(selectedFloor, () => {
  // Filter reacts automatically via computed
})

// ==================== Lifecycle ====================

onMounted(() => {
  loadBuildings()
})
</script>
