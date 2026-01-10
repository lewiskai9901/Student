<template>
  <div class="min-h-screen bg-gray-50/50 p-6">
    <!-- 页面头部 -->
    <div class="mb-6 rounded-xl bg-gradient-to-r from-teal-600 to-cyan-500 p-6 shadow-lg">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="flex items-center gap-3 text-2xl font-bold text-white">
            <Home class="h-8 w-8" />
            宿舍总览
          </h1>
          <p class="mt-1 text-teal-100">实时查看和管理所有宿舍的入住情况</p>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-4 gap-4">
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">总宿舍数</p>
            <p class="mt-1 text-2xl font-bold text-teal-600">{{ totalRooms }}</p>
            <p class="mt-0.5 text-xs text-gray-400">间</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-teal-100 text-teal-600">
            <DoorOpen class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-teal-500 to-cyan-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">入住率</p>
            <p class="mt-1 text-2xl font-bold" :class="occupancyRate >= 80 ? 'text-emerald-600' : occupancyRate >= 50 ? 'text-amber-600' : 'text-red-500'">
              {{ occupancyRate }}%
            </p>
            <p class="mt-0.5 text-xs" :class="occupancyRate >= 80 ? 'text-emerald-500' : occupancyRate >= 50 ? 'text-amber-500' : 'text-red-400'">
              {{ occupancyRate >= 80 ? '高' : occupancyRate >= 50 ? '中' : '低' }}
            </p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl" :class="occupancyRate >= 80 ? 'bg-emerald-100 text-emerald-600' : occupancyRate >= 50 ? 'bg-amber-100 text-amber-600' : 'bg-red-100 text-red-500'">
            <Percent class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full opacity-0 transition-opacity group-hover:opacity-100" :class="occupancyRate >= 80 ? 'bg-gradient-to-r from-emerald-500 to-green-400' : occupancyRate >= 50 ? 'bg-gradient-to-r from-amber-500 to-yellow-400' : 'bg-gradient-to-r from-red-500 to-orange-400'"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">可用床位</p>
            <p class="mt-1 text-2xl font-bold text-blue-600">{{ availableBeds }}</p>
            <p class="mt-0.5 text-xs text-gray-400">个</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-100 text-blue-600">
            <BedDouble class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-blue-500 to-indigo-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">已入住</p>
            <p class="mt-1 text-2xl font-bold text-purple-600">{{ totalBeds - availableBeds }}</p>
            <p class="mt-0.5 text-xs text-gray-400">人</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-purple-100 text-purple-600">
            <Users class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-purple-500 to-violet-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
    </div>

    <!-- 搜索筛选 -->
    <div class="mb-6 rounded-xl bg-white p-4 shadow-sm">
      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center gap-2">
          <Building2 class="h-4 w-4 text-gray-400" />
          <select
            v-model="filterForm.buildingId"
            @change="handleFilterChange"
            class="w-40 rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
          >
            <option :value="null">全部宿舍楼</option>
            <option v-for="building in buildingList" :key="building.id" :value="building.id">
              {{ building.buildingName }}
            </option>
          </select>
        </div>
        <select
          v-model="filterForm.floor"
          @change="handleFilterChange"
          class="w-28 rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
        >
          <option :value="null">全部楼层</option>
          <option v-for="floor in floorList" :key="floor" :value="floor">{{ floor }}层</option>
        </select>
        <select
          v-model="filterForm.genderType"
          @change="handleFilterChange"
          class="w-32 rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
        >
          <option :value="null">全部类型</option>
          <option :value="1">男生宿舍</option>
          <option :value="2">女生宿舍</option>
          <option :value="3">混合宿舍</option>
        </select>
        <select
          v-model="filterForm.occupancyStatus"
          @change="handleFilterChange"
          class="w-32 rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
        >
          <option :value="null">全部状态</option>
          <option value="empty">空置</option>
          <option value="partial">部分入住</option>
          <option value="full">满员</option>
        </select>
        <div class="flex items-center gap-2">
          <Search class="h-4 w-4 text-gray-400" />
          <input
            v-model="filterForm.roomNo"
            @keyup.enter="handleFilterChange"
            type="text"
            placeholder="房间号"
            class="w-32 rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
          />
        </div>
        <div class="flex gap-2">
          <button
            @click="handleFilterChange"
            class="flex items-center gap-1 rounded-lg bg-teal-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-teal-700"
          >
            <Search class="h-4 w-4" />
            搜索
          </button>
          <button
            @click="handleReset"
            class="flex items-center gap-1 rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-600 transition-colors hover:bg-gray-50"
          >
            <RefreshCw class="h-4 w-4" />
            重置
          </button>
        </div>
        <div class="ml-auto flex items-center gap-2 rounded-lg border border-gray-200 p-1">
          <button
            @click="viewMode = 'floor'"
            class="flex items-center gap-1 rounded-md px-3 py-1.5 text-sm transition-colors"
            :class="viewMode === 'floor' ? 'bg-teal-600 text-white' : 'text-gray-600 hover:bg-gray-100'"
          >
            <Layers class="h-4 w-4" />
            楼层
          </button>
          <button
            @click="viewMode = 'compact'"
            class="flex items-center gap-1 rounded-md px-3 py-1.5 text-sm transition-colors"
            :class="viewMode === 'compact' ? 'bg-teal-600 text-white' : 'text-gray-600 hover:bg-gray-100'"
          >
            <Grid3x3 class="h-4 w-4" />
            精简
          </button>
          <button
            @click="viewMode = 'grid'"
            class="flex items-center gap-1 rounded-md px-3 py-1.5 text-sm transition-colors"
            :class="viewMode === 'grid' ? 'bg-teal-600 text-white' : 'text-gray-600 hover:bg-gray-100'"
          >
            <LayoutGrid class="h-4 w-4" />
            网格
          </button>
          <button
            @click="viewMode = 'list'"
            class="flex items-center gap-1 rounded-md px-3 py-1.5 text-sm transition-colors"
            :class="viewMode === 'list' ? 'bg-teal-600 text-white' : 'text-gray-600 hover:bg-gray-100'"
          >
            <List class="h-4 w-4" />
            列表
          </button>
        </div>
      </div>
    </div>

    <!-- 宿舍内容区域 -->
    <div class="rounded-xl bg-white shadow-sm">
      <!-- 头部 -->
      <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
        <span class="text-lg font-semibold text-gray-900">宿舍列表 ({{ filteredRooms.length }})</span>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="py-20 text-center">
        <Loader2 class="mx-auto h-8 w-8 animate-spin text-teal-400" />
        <p class="mt-2 text-sm text-gray-500">加载中...</p>
      </div>

      <!-- 楼层分布图视图 -->
      <div v-else-if="viewMode === 'floor'" class="p-4">
        <div v-if="buildingFloorData.length > 0" class="space-y-4">
          <!-- 图例 -->
          <div class="flex flex-wrap items-center gap-4 rounded-lg bg-gray-50 px-4 py-2 text-xs">
            <span class="font-medium text-gray-600">图例：</span>
            <div class="flex items-center gap-1">
              <div class="h-3 w-3 rounded bg-emerald-500"></div>
              <span class="text-gray-600">满员</span>
            </div>
            <div class="flex items-center gap-1">
              <div class="h-3 w-3 rounded bg-blue-500"></div>
              <span class="text-gray-600">部分入住</span>
            </div>
            <div class="flex items-center gap-1">
              <div class="h-3 w-3 rounded border border-gray-300 bg-white"></div>
              <span class="text-gray-600">空置</span>
            </div>
            <div class="flex items-center gap-1">
              <div class="h-3 w-3 rounded bg-gray-300"></div>
              <span class="text-gray-600">停用/维修</span>
            </div>
          </div>

          <!-- 楼栋列表 -->
          <div
            v-for="building in buildingFloorData"
            :key="building.buildingName"
            class="overflow-hidden rounded-xl border border-gray-200 bg-white"
          >
            <!-- 楼栋标题 -->
            <div
              class="flex cursor-pointer items-center justify-between bg-gradient-to-r from-teal-50 to-cyan-50 px-4 py-3"
              @click="toggleBuildingExpand(building.buildingName)"
            >
              <div class="flex items-center gap-3">
                <component
                  :is="expandedBuildings.includes(building.buildingName) ? ChevronDown : ChevronRight"
                  class="h-5 w-5 text-teal-600"
                />
                <Building2 class="h-5 w-5 text-teal-600" />
                <span class="text-base font-semibold text-gray-900">{{ building.buildingName }}</span>
                <span
                  class="rounded-full px-2 py-0.5 text-xs font-medium"
                  :class="building.genderType === 1 ? 'bg-blue-100 text-blue-700' : building.genderType === 2 ? 'bg-pink-100 text-pink-700' : 'bg-gray-100 text-gray-700'"
                >
                  {{ building.genderType === 1 ? '男生楼' : building.genderType === 2 ? '女生楼' : '混合楼' }}
                </span>
              </div>
              <div class="flex items-center gap-4 text-sm">
                <span class="text-gray-500">共 <strong class="text-gray-900">{{ building.totalRooms }}</strong> 间</span>
                <span class="text-gray-500">入住率 <strong class="text-teal-600">{{ building.occupancyRate }}%</strong></span>
              </div>
            </div>

            <!-- 楼层内容 -->
            <div v-show="expandedBuildings.includes(building.buildingName)" class="divide-y divide-gray-100">
              <div
                v-for="floor in building.floors"
                :key="floor.floor"
                class="flex items-stretch"
              >
                <!-- 楼层标签 -->
                <div class="flex w-20 flex-shrink-0 items-center justify-center border-r border-gray-100 bg-gray-50">
                  <div class="text-center">
                    <div class="text-lg font-bold text-gray-700">{{ floor.floor }}F</div>
                    <div class="text-[10px] text-gray-400">{{ floor.rooms.length }}间</div>
                  </div>
                </div>
                <!-- 房间列表 -->
                <div class="flex flex-1 flex-wrap gap-1.5 p-3">
                  <div
                    v-for="room in floor.rooms"
                    :key="room.id"
                    class="group relative cursor-pointer rounded-md border px-2 py-1.5 text-center transition-all hover:shadow-md"
                    :class="getRoomStatusClass(room)"
                    :title="`${getFullRoomNo(room)} - ${room.currentOccupancy}/${room.maxOccupancy}人`"
                    @click="handleAddStudent(room)"
                  >
                    <div class="text-xs font-medium" :class="getRoomTextClass(room)">
                      {{ room.roomNo || room.dormitoryNo }}
                    </div>
                    <div class="text-[10px]" :class="getRoomSubTextClass(room)">
                      {{ room.currentOccupancy }}/{{ room.maxOccupancy }}
                    </div>
                    <!-- 悬浮提示 -->
                    <div class="pointer-events-none absolute bottom-full left-1/2 z-10 mb-2 hidden -translate-x-1/2 whitespace-nowrap rounded-lg bg-gray-900 px-3 py-2 text-xs text-white shadow-lg group-hover:block">
                      <div class="font-medium">{{ getFullRoomNo(room) }}</div>
                      <div class="mt-1 text-gray-300">
                        {{ room.maxOccupancy }}人间 · {{ room.currentOccupancy }}人入住
                      </div>
                      <div v-if="room.students && room.students.length > 0" class="mt-1 border-t border-gray-700 pt-1">
                        <div v-for="s in room.students.slice(0, 4)" :key="s.id" class="text-gray-300">
                          {{ s.bedNumber || '-' }}号床: {{ s.realName }}
                        </div>
                        <div v-if="room.students.length > 4" class="text-gray-400">...</div>
                      </div>
                      <div class="absolute left-1/2 top-full -translate-x-1/2 border-4 border-transparent border-t-gray-900"></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="py-20 text-center">
          <Home class="mx-auto h-12 w-12 text-gray-300" />
          <p class="mt-2 text-sm text-gray-500">暂无宿舍数据</p>
        </div>
      </div>

      <!-- 精简视图 -->
      <div v-else-if="viewMode === 'compact'" class="p-4">
        <div v-if="filteredRooms.length > 0" class="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-8">
          <div
            v-for="room in filteredRooms"
            :key="room.id"
            class="group relative cursor-pointer rounded-lg border p-2 transition-all hover:shadow-md"
            :class="[
              room.currentOccupancy >= room.maxOccupancy
                ? 'border-emerald-200 bg-emerald-50'
                : room.currentOccupancy > 0
                  ? 'border-blue-200 bg-blue-50'
                  : 'border-gray-200 bg-white'
            ]"
            @click="handleAddStudent(room)"
          >
            <!-- 房间号（楼号-房间号） -->
            <div class="mb-1.5 flex items-center justify-between">
              <span class="text-sm font-semibold text-gray-900">{{ getFullRoomNo(room) }}</span>
              <span
                class="rounded px-1 py-0.5 text-[10px] font-medium"
                :class="room.genderType === 1 ? 'bg-blue-100 text-blue-600' : room.genderType === 2 ? 'bg-pink-100 text-pink-600' : 'bg-gray-100 text-gray-600'"
              >
                {{ room.genderType === 1 ? '男' : room.genderType === 2 ? '女' : '混' }}
              </span>
            </div>
            <!-- 入住情况 -->
            <div class="mb-1.5 flex items-center gap-1">
              <div class="h-1.5 flex-1 overflow-hidden rounded-full bg-gray-200">
                <div
                  class="h-full rounded-full transition-all"
                  :class="room.currentOccupancy >= room.maxOccupancy ? 'bg-emerald-500' : room.currentOccupancy > 0 ? 'bg-blue-500' : 'bg-gray-300'"
                  :style="{ width: `${(room.currentOccupancy / room.maxOccupancy) * 100}%` }"
                ></div>
              </div>
              <span class="text-[10px] font-medium text-gray-500">{{ room.currentOccupancy }}/{{ room.maxOccupancy }}</span>
            </div>
            <!-- 学生列表 -->
            <div v-if="room.students && room.students.length > 0" class="space-y-0.5">
              <div
                v-for="(student, idx) in room.students.slice(0, room.maxOccupancy)"
                :key="student.id || idx"
                class="flex items-center gap-1"
              >
                <div
                  class="flex h-4 w-4 flex-shrink-0 items-center justify-center rounded-full text-[8px] font-medium"
                  :class="room.genderType === 1 ? 'bg-blue-200 text-blue-700' : room.genderType === 2 ? 'bg-pink-200 text-pink-700' : 'bg-gray-200 text-gray-700'"
                >
                  {{ extractBedNumber(student.bedNumber) || idx + 1 }}
                </div>
                <span class="truncate text-[11px] text-gray-700">{{ student.realName }}</span>
              </div>
            </div>
            <div v-else class="flex items-center justify-center gap-1 py-1 text-[10px] text-gray-400">
              <UserCircle2 class="h-3.5 w-3.5" />
              <span>空置</span>
            </div>
          </div>
        </div>
        <div v-else class="py-20 text-center">
          <Home class="mx-auto h-12 w-12 text-gray-300" />
          <p class="mt-2 text-sm text-gray-500">暂无宿舍数据</p>
        </div>
      </div>

      <!-- 网格视图 -->
      <div v-else-if="viewMode === 'grid'" class="p-6">
        <div v-if="filteredRooms.length > 0" class="grid auto-rows-fr grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          <DormitoryCard
            v-for="room in filteredRooms"
            :key="room.id"
            :room="room"
            @add-student="handleAddStudent"
            @remove-student="handleRemoveStudent"
            @swap-student="handleSwapStudent"
            @refresh="loadRoomList"
          />
        </div>
        <div v-else class="py-20 text-center">
          <Home class="mx-auto h-12 w-12 text-gray-300" />
          <p class="mt-2 text-sm text-gray-500">暂无宿舍数据</p>
        </div>
      </div>

      <!-- 列表视图 -->
      <div v-else class="overflow-x-auto">
        <table v-if="filteredRooms.length > 0" class="w-full">
          <thead>
            <tr class="border-b border-gray-100 bg-gray-50/50">
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">房间号</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">宿舍楼</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider text-gray-500">楼层</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider text-gray-500">类型</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider text-gray-500">入住情况</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">学生列表</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider text-gray-500">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr
              v-for="(row, index) in filteredRooms"
              :key="row.id"
              class="transition-colors hover:bg-gray-50/50"
              :style="{ animationDelay: `${index * 30}ms` }"
            >
              <td class="px-4 py-3">
                <span class="inline-flex items-center gap-1.5 font-medium text-gray-900">
                  <DoorOpen class="h-4 w-4 text-teal-500" />
                  {{ row.roomNo }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-600">{{ row.buildingName || '-' }}</td>
              <td class="px-4 py-3 text-center text-sm text-gray-600">{{ row.floor }}层</td>
              <td class="px-4 py-3 text-center">
                <span
                  class="inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium"
                  :class="row.genderType === 1 ? 'bg-blue-100 text-blue-700' : row.genderType === 2 ? 'bg-pink-100 text-pink-700' : 'bg-gray-100 text-gray-700'"
                >
                  {{ row.genderType === 1 ? '男生宿舍' : row.genderType === 2 ? '女生宿舍' : '混合宿舍' }}
                </span>
              </td>
              <td class="px-4 py-3 text-center">
                <div class="flex items-center justify-center gap-2">
                  <div class="h-2 w-20 overflow-hidden rounded-full bg-gray-200">
                    <div
                      class="h-full rounded-full transition-all"
                      :class="getOccupancyBarColor(row)"
                      :style="{ width: `${(row.currentOccupancy / row.maxOccupancy) * 100}%` }"
                    ></div>
                  </div>
                  <span class="text-sm text-gray-600">{{ row.currentOccupancy }}/{{ row.maxOccupancy }}</span>
                </div>
              </td>
              <td class="px-4 py-3">
                <div v-if="row.students && row.students.length > 0" class="flex flex-wrap gap-1">
                  <span
                    v-for="student in row.students.slice(0, 3)"
                    :key="student.id"
                    class="inline-flex items-center rounded-full bg-teal-50 px-2 py-0.5 text-xs font-medium text-teal-700"
                  >
                    {{ student.realName }}
                  </span>
                  <span v-if="row.students.length > 3" class="inline-flex items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-500">
                    +{{ row.students.length - 3 }}
                  </span>
                </div>
                <span v-else class="text-sm text-gray-400">暂无学生</span>
              </td>
              <td class="px-4 py-3 text-center">
                <div class="flex items-center justify-center gap-1">
                  <button
                    @click="handleViewRoom(row)"
                    class="rounded-lg p-1.5 text-gray-500 transition-colors hover:bg-gray-100"
                    title="查看"
                  >
                    <Eye class="h-4 w-4" />
                  </button>
                  <button
                    @click="handleAddStudent(row)"
                    :disabled="row.currentOccupancy >= row.maxOccupancy"
                    class="rounded-lg p-1.5 text-teal-600 transition-colors hover:bg-teal-50 disabled:cursor-not-allowed disabled:opacity-50"
                    title="添加学生"
                  >
                    <UserPlus class="h-4 w-4" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="py-20 text-center">
          <Home class="mx-auto h-12 w-12 text-gray-300" />
          <p class="mt-2 text-sm text-gray-500">暂无宿舍数据</p>
        </div>
      </div>
    </div>

    <!-- 添加学生对话框 -->
    <Teleport to="body">
      <div
        v-if="addStudentDialogVisible"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
        @click.self="addStudentDialogVisible = false"
      >
        <div class="w-full max-w-4xl animate-fade-in rounded-2xl bg-white shadow-2xl">
          <!-- 对话框头部 -->
          <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
            <h3 class="flex items-center gap-2 text-lg font-semibold text-gray-900">
              <UserPlus class="h-5 w-5 text-teal-600" />
              分配学生到 {{ currentRoom?.buildingName || '' }} - {{ currentRoom?.roomNo || '' }}
            </h3>
            <button
              @click="addStudentDialogVisible = false"
              class="rounded-lg p-1.5 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
            >
              <X class="h-5 w-5" />
            </button>
          </div>

          <!-- 对话框内容 -->
          <div class="max-h-[70vh] overflow-y-auto px-6 py-4">
            <!-- 顶部信息 -->
            <div class="mb-4 flex items-center gap-4 rounded-lg bg-teal-50 p-3">
              <Info class="h-5 w-5 flex-shrink-0 text-teal-600" />
              <div class="flex flex-wrap gap-4 text-sm text-teal-700">
                <span>床位号: <strong>{{ addStudentForm.bedNumber ? `${addStudentForm.bedNumber}号床` : '请先选择' }}</strong></span>
                <span class="text-teal-300">|</span>
                <span>当前入住: {{ currentRoom?.currentOccupancy || 0 }}/{{ currentRoom?.maxOccupancy || 0 }}人</span>
                <span class="text-teal-300">|</span>
                <span>可用床位: {{ availableBedNumbers.length }}个</span>
              </div>
            </div>

            <!-- 床位入住情况可视化 -->
            <div class="mb-4">
              <label class="mb-2 block text-sm font-medium text-gray-700">床位入住情况</label>
              <div class="rounded-lg border border-gray-200 p-4 bg-gray-50">
                <!-- 床位网格布局 -->
                <div class="grid gap-3" :class="currentRoom?.maxOccupancy <= 4 ? 'grid-cols-2' : (currentRoom?.maxOccupancy <= 6 ? 'grid-cols-3' : 'grid-cols-4')">
                  <div
                    v-for="bedNum in currentRoom?.maxOccupancy || 0"
                    :key="bedNum"
                    class="relative rounded-lg border-2 p-3 transition-all"
                    :class="getBedStatusClass(bedNum)"
                    @click="handleBedClick(bedNum)"
                  >
                    <!-- 床位号标签 -->
                    <div class="absolute -top-2 left-2 rounded bg-white px-1.5 text-xs font-bold" :class="getBedInfo(bedNum).occupied ? 'text-green-600' : 'text-gray-400'">
                      {{ bedNum }}号床
                    </div>

                    <!-- 床位内容 -->
                    <div class="mt-2 min-h-[60px] flex flex-col justify-center">
                      <template v-if="getBedInfo(bedNum).occupied">
                        <!-- 已入住 -->
                        <div class="flex items-center gap-2">
                          <div class="flex h-10 w-10 items-center justify-center rounded-full bg-green-100 text-sm font-medium text-green-700">
                            {{ getBedInfo(bedNum).student?.realName?.charAt(0) || '?' }}
                          </div>
                          <div class="flex-1 min-w-0">
                            <div class="truncate text-sm font-medium text-gray-900">{{ getBedInfo(bedNum).student?.realName }}</div>
                            <div class="truncate text-xs text-gray-500">{{ getBedInfo(bedNum).student?.studentNo }}</div>
                          </div>
                        </div>
                        <div class="mt-1 flex items-center gap-1 text-xs text-green-600">
                          <User class="h-3 w-3" />
                          <span>已入住</span>
                        </div>
                      </template>
                      <template v-else>
                        <!-- 空床位 -->
                        <div class="flex flex-col items-center justify-center text-gray-400">
                          <BedDouble class="h-8 w-8" />
                          <span class="mt-1 text-xs">空床位</span>
                          <span v-if="addStudentForm.bedNumber === bedNum" class="mt-1 text-xs text-teal-600 font-medium">已选择</span>
                        </div>
                      </template>
                    </div>

                    <!-- 选中指示器 -->
                    <div v-if="addStudentForm.bedNumber === bedNum && !getBedInfo(bedNum).occupied" class="absolute top-1 right-1">
                      <CheckCircle2 class="h-5 w-5 text-teal-600" />
                    </div>
                  </div>
                </div>

                <!-- 图例 -->
                <div class="mt-3 flex flex-wrap items-center gap-4 border-t border-gray-200 pt-3 text-xs text-gray-500">
                  <div class="flex items-center gap-1">
                    <div class="h-3 w-3 rounded border-2 border-green-400 bg-green-50"></div>
                    <span>已入住</span>
                  </div>
                  <div class="flex items-center gap-1">
                    <div class="h-3 w-3 rounded border-2 border-dashed border-gray-300 bg-white"></div>
                    <span>空床位</span>
                  </div>
                  <div class="flex items-center gap-1">
                    <div class="h-3 w-3 rounded border-2 border-teal-500 bg-teal-50"></div>
                    <span>当前选择</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="mb-4 border-t border-gray-100 pt-4">
              <!-- 工具栏 -->
              <div class="mb-4 flex flex-wrap items-center gap-3">
                <div class="flex items-center gap-1 rounded-lg border border-gray-200 p-1">
                  <button
                    @click="studentViewMode = 'table'"
                    class="rounded-md px-3 py-1 text-sm transition-colors"
                    :class="studentViewMode === 'table' ? 'bg-teal-600 text-white' : 'text-gray-600 hover:bg-gray-100'"
                  >
                    表格视图
                  </button>
                  <button
                    @click="studentViewMode = 'card'"
                    class="rounded-md px-3 py-1 text-sm transition-colors"
                    :class="studentViewMode === 'card' ? 'bg-teal-600 text-white' : 'text-gray-600 hover:bg-gray-100'"
                  >
                    卡片视图
                  </button>
                </div>
                <div class="flex items-center gap-2">
                  <Search class="h-4 w-4 text-gray-400" />
                  <input
                    v-model="studentSearchKeyword"
                    type="text"
                    placeholder="搜索学号或姓名"
                    class="w-40 rounded-lg border border-gray-200 px-3 py-1.5 text-sm focus:border-teal-500 focus:outline-none"
                  />
                </div>
                <select
                  v-model="studentStatusFilter"
                  class="rounded-lg border border-gray-200 px-3 py-1.5 text-sm focus:border-teal-500 focus:outline-none"
                >
                  <option value="all">全部</option>
                  <option value="unassigned">未分配</option>
                  <option value="currentDorm">本宿舍</option>
                  <option value="otherDorm">其他宿舍</option>
                </select>
                <div class="ml-auto text-sm text-gray-500">
                  共 {{ allStudentsForAssignment.length }} 人 | 本宿舍 {{ currentDormCount }} 人 | 未分配 {{ unassignedCount }} 人
                </div>
              </div>

              <!-- 绑定班级提示 -->
              <div v-if="boundClassNames" class="mb-4 rounded-lg border border-green-200 bg-green-50 px-3 py-2 text-xs text-green-700">
                <span class="font-medium">绑定班级: </span>{{ boundClassNames }}
              </div>

              <!-- 更换宿舍提示 -->
              <div class="mb-4 rounded-lg border border-blue-200 bg-blue-50 px-3 py-2 text-xs text-blue-700">
                <span class="font-medium">提示: </span>选择已分配宿舍的学生（橙色标记）将在确认时提示更换宿舍
              </div>

              <!-- 班级Tab -->
              <div v-if="studentClassTabs.length > 0" class="mb-4 flex flex-wrap gap-2">
                <button
                  v-for="tab in studentClassTabs"
                  :key="tab.classId"
                  @click="activeClassTab = tab.classId"
                  class="rounded-lg border px-3 py-1.5 text-sm transition-colors"
                  :class="activeClassTab === tab.classId ? 'border-teal-500 bg-teal-50 text-teal-700' : 'border-gray-200 text-gray-600 hover:border-teal-300'"
                >
                  {{ tab.className }} ({{ tab.unassignedCount }}/{{ tab.totalCount }})
                </button>
              </div>

              <!-- 表格视图 -->
              <div v-if="studentViewMode === 'table'" class="max-h-96 overflow-auto rounded-lg border border-gray-200">
                <table class="w-full">
                  <thead class="sticky top-0 bg-gray-50">
                    <tr class="border-b border-gray-200">
                      <th class="w-12 px-3 py-2"></th>
                      <th class="px-3 py-2 text-left text-xs font-semibold text-gray-500">姓名</th>
                      <th class="px-3 py-2 text-left text-xs font-semibold text-gray-500">学号</th>
                      <th class="px-3 py-2 text-left text-xs font-semibold text-gray-500">班级</th>
                      <th class="px-3 py-2 text-left text-xs font-semibold text-gray-500">性别</th>
                      <th class="px-3 py-2 text-left text-xs font-semibold text-gray-500">宿舍信息</th>
                      <th class="px-3 py-2 text-center text-xs font-semibold text-gray-500">状态</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-gray-100">
                    <tr
                      v-for="student in filteredStudentsForDisplay"
                      :key="student.id"
                      @click="handleStudentRowClick(student)"
                      class="cursor-pointer transition-colors"
                      :class="{
                        'bg-orange-50': addStudentForm.studentId === student.id && !!student.dormitoryId,
                        'bg-teal-50': addStudentForm.studentId === student.id && !student.dormitoryId,
                        'hover:bg-gray-50': addStudentForm.studentId !== student.id
                      }"
                    >
                      <td class="px-3 py-2">
                        <input
                          type="radio"
                          :checked="addStudentForm.studentId === student.id"
                          @change="addStudentForm.studentId = student.id"
                          class="h-4 w-4 focus:ring-teal-500"
                          :class="student.dormitoryId ? 'text-orange-500' : 'text-teal-600'"
                        />
                      </td>
                      <td class="px-3 py-2">
                        <div class="flex items-center gap-2">
                          <div class="flex h-8 w-8 items-center justify-center rounded-full bg-teal-100 text-sm font-medium text-teal-700">
                            {{ student.realName?.charAt(0) || '?' }}
                          </div>
                          <span class="text-sm font-medium text-gray-900">{{ student.realName }}</span>
                        </div>
                      </td>
                      <td class="px-3 py-2 text-sm text-gray-600">{{ student.studentNo }}</td>
                      <td class="px-3 py-2 text-sm text-gray-600">{{ student.className }}</td>
                      <td class="px-3 py-2 text-sm text-gray-600">{{ student.genderName }}</td>
                      <td class="px-3 py-2">
                        <span v-if="!student.dormitoryId" class="inline-flex items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600">
                          未分配
                        </span>
                        <div v-else class="text-sm">
                          <div class="text-gray-700">{{ student.buildingName }} {{ student.roomNo }}</div>
                          <div class="text-xs text-gray-400">{{ student.bedNumber }}号床</div>
                        </div>
                      </td>
                      <td class="px-3 py-2 text-center">
                        <span
                          class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium"
                          :class="!student.dormitoryId ? 'bg-emerald-100 text-emerald-700' : 'bg-orange-100 text-orange-700'"
                        >
                          {{ !student.dormitoryId ? '可分配' : '可更换' }}
                        </span>
                      </td>
                    </tr>
                    <tr v-if="filteredStudentsForDisplay.length === 0">
                      <td colspan="7" class="py-8 text-center">
                        <div class="text-sm text-gray-400">没有符合条件的学生</div>
                        <div v-if="allStudentsForAssignment.length === 0 && boundClassNames" class="mt-2 text-xs text-gray-400">
                          绑定班级「{{ boundClassNames }}」中没有符合性别要求的学生
                        </div>
                        <div v-else-if="allStudentsForAssignment.length === 0" class="mt-2 text-xs text-gray-400">
                          该宿舍尚未绑定班级或无符合条件的学生
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- 卡片视图 -->
              <div v-else class="max-h-96 overflow-auto">
                <div v-if="filteredStudentsForDisplay.length > 0" class="grid grid-cols-2 gap-3 md:grid-cols-3">
                  <div
                    v-for="student in filteredStudentsForDisplay"
                    :key="student.id"
                    @click="handleStudentCardClick(student)"
                    class="flex cursor-pointer items-center gap-3 rounded-lg border-2 p-3 transition-all"
                    :class="{
                      'border-blue-500 bg-blue-50': addStudentForm.studentId === student.id && student.dormitoryId === currentRoom?.id,
                      'border-orange-500 bg-orange-50': addStudentForm.studentId === student.id && student.dormitoryId && student.dormitoryId !== currentRoom?.id,
                      'border-teal-500 bg-teal-50': addStudentForm.studentId === student.id && !student.dormitoryId,
                      'border-blue-200 hover:border-blue-300 hover:bg-blue-50/50': student.dormitoryId === currentRoom?.id && addStudentForm.studentId !== student.id,
                      'border-orange-200 hover:border-orange-300 hover:bg-orange-50/50': student.dormitoryId && student.dormitoryId !== currentRoom?.id && addStudentForm.studentId !== student.id,
                      'border-gray-200 hover:border-teal-300 hover:bg-teal-50/50': !student.dormitoryId && addStudentForm.studentId !== student.id
                    }"
                  >
                    <div
                      class="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full text-sm font-medium"
                      :class="student.dormitoryId === currentRoom?.id ? 'bg-blue-100 text-blue-700' : (student.dormitoryId ? 'bg-orange-100 text-orange-700' : 'bg-teal-100 text-teal-700')"
                    >
                      {{ student.realName?.charAt(0) || '?' }}
                    </div>
                    <div class="min-w-0 flex-1">
                      <div class="truncate text-sm font-medium text-gray-900">{{ student.realName }}</div>
                      <div class="truncate text-xs text-gray-500">{{ student.studentNo }}</div>
                      <div v-if="student.dormitoryId === currentRoom?.id" class="truncate text-xs text-blue-600">
                        本宿舍 - {{ student.bedNumber }}号床
                      </div>
                      <div v-else-if="student.dormitoryId" class="truncate text-xs text-orange-600">
                        已分配: {{ student.buildingName }} {{ student.roomNo }} - {{ student.bedNumber }}号床
                      </div>
                      <span v-else class="inline-flex items-center rounded-full bg-emerald-100 px-1.5 py-0.5 text-xs font-medium text-emerald-700">
                        未分配
                      </span>
                    </div>
                    <CheckCircle2 v-if="addStudentForm.studentId === student.id" class="h-5 w-5 flex-shrink-0" :class="student.dormitoryId === currentRoom?.id ? 'text-blue-600' : (student.dormitoryId ? 'text-orange-600' : 'text-teal-600')" />
                  </div>
                </div>
                <div v-else class="py-8 text-center">
                  <div class="text-sm text-gray-400">没有符合条件的学生</div>
                  <div v-if="allStudentsForAssignment.length === 0 && boundClassNames" class="mt-2 text-xs text-gray-400">
                    绑定班级「{{ boundClassNames }}」中没有符合性别要求的学生
                  </div>
                  <div v-else-if="allStudentsForAssignment.length === 0" class="mt-2 text-xs text-gray-400">
                    该宿舍尚未绑定班级或无符合条件的学生
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 对话框底部 -->
          <div class="flex items-center justify-end gap-3 border-t border-gray-100 px-6 py-4">
            <button
              @click="addStudentDialogVisible = false"
              class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
            >
              取消
            </button>
            <button
              @click="handleAddSubmit"
              :disabled="!addStudentForm.studentId || !addStudentForm.bedNumber || submitLoading"
              class="flex items-center gap-2 rounded-lg bg-teal-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-teal-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
              <Check v-else class="h-4 w-4" />
              确定分配
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 学生交换对话框 -->
    <Teleport to="body">
      <div
        v-if="swapDialogVisible"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
        @click.self="swapDialogVisible = false"
      >
        <div class="w-full max-w-2xl animate-fade-in rounded-2xl bg-white shadow-2xl">
          <!-- 对话框头部 -->
          <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
            <h3 class="flex items-center gap-2 text-lg font-semibold text-gray-900">
              <ArrowRightLeft class="h-5 w-5 text-teal-600" />
              交换学生宿舍
            </h3>
            <button
              @click="swapDialogVisible = false"
              class="rounded-lg p-1.5 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
            >
              <X class="h-5 w-5" />
            </button>
          </div>

          <!-- 对话框内容 -->
          <div class="px-6 py-4">
            <div class="grid grid-cols-2 gap-6">
              <!-- 学生A -->
              <div class="rounded-lg bg-gray-50 p-4">
                <h4 class="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-700">
                  <User class="h-4 w-4" />
                  学生 A
                </h4>
                <div class="space-y-2 text-sm">
                  <div class="flex items-center justify-between">
                    <span class="text-gray-500">学生信息</span>
                    <span class="font-medium text-gray-900">{{ swapForm.studentA?.realName }} ({{ swapForm.studentA?.studentNo }})</span>
                  </div>
                  <div class="flex items-center justify-between">
                    <span class="text-gray-500">当前宿舍</span>
                    <span class="font-medium text-gray-900">{{ swapForm.studentA?.roomNo }}</span>
                  </div>
                  <div class="flex items-center justify-between">
                    <span class="text-gray-500">当前床位</span>
                    <span class="font-medium text-gray-900">{{ swapForm.studentA?.bedNumber }}号床</span>
                  </div>
                </div>
              </div>
              <!-- 学生B -->
              <div class="rounded-lg bg-gray-50 p-4">
                <h4 class="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-700">
                  <User class="h-4 w-4" />
                  学生 B
                </h4>
                <div class="space-y-2">
                  <label class="text-sm text-gray-500">选择要交换的学生</label>
                  <div class="relative">
                    <input
                      v-model="swapStudentKeyword"
                      type="text"
                      placeholder="搜索学生姓名..."
                      class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
                      @input="searchStudentsWithRoom(swapStudentKeyword)"
                    />
                  </div>
                  <div v-if="studentsWithRoom.length > 0" class="max-h-40 space-y-1 overflow-auto">
                    <button
                      v-for="student in studentsWithRoom"
                      :key="student.id"
                      @click="swapForm.studentBId = student.id"
                      class="w-full rounded-lg border p-2 text-left text-sm transition-colors"
                      :class="swapForm.studentBId === student.id ? 'border-teal-500 bg-teal-50' : 'border-gray-200 hover:bg-gray-50'"
                    >
                      <div class="font-medium text-gray-900">{{ student.realName }} ({{ student.studentNo }})</div>
                      <div class="text-xs text-gray-500">{{ student.roomNo }} - {{ student.bedNumber }}号床</div>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 对话框底部 -->
          <div class="flex items-center justify-end gap-3 border-t border-gray-100 px-6 py-4">
            <button
              @click="swapDialogVisible = false"
              class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
            >
              取消
            </button>
            <button
              @click="handleSwapSubmit"
              :disabled="!swapForm.studentBId || submitLoading"
              class="flex items-center gap-2 rounded-lg bg-teal-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-teal-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
              <Check v-else class="h-4 w-4" />
              确定交换
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Home,
  DoorOpen,
  Percent,
  BedDouble,
  Users,
  Building2,
  Search,
  RefreshCw,
  LayoutGrid,
  List,
  Eye,
  UserPlus,
  X,
  Check,
  Loader2,
  Info,
  CheckCircle2,
  ArrowRightLeft,
  User,
  Grid3x3,
  UserCircle2,
  Layers,
  ChevronDown,
  ChevronRight
} from 'lucide-vue-next'
import DormitoryCard from '@/components/dormitory/DormitoryCard.vue'
// V2 DDD API
import { getDormitories, assignStudentToDormitory, removeStudentFromDormitory, swapStudentDormitory } from '@/api/v2/dormitory'
import { getStudents } from '@/api/v2/student'

// 加载状态
const loading = ref(false)
const searchLoading = ref(false)
const submitLoading = ref(false)

// 视图模式
const viewMode = ref('grid')

// 楼层视图展开的楼栋
const expandedBuildings = ref<string[]>([])

// 筛选表单
const filterForm = reactive({
  buildingId: null as string | null,
  floor: null as number | null,
  genderType: null as number | null,
  occupancyStatus: null as string | null,
  roomNo: ''
})

// 数据
const roomList = ref<any[]>([])
const studentsWithRoom = ref<any[]>([])
const studentSearchKeyword = ref('')
const swapStudentKeyword = ref('')

// 新的学生分配相关变量
const allStudentsForAssignment = ref<any[]>([])
const studentViewMode = ref('table')
const studentStatusFilter = ref('all')
const activeClassTab = ref<any>(null)
const boundClassNames = ref<string>('')  // 宿舍绑定的班级名称

// 宿舍楼列表
const buildingList = computed(() => {
  const buildingsMap = new Map()
  roomList.value.forEach(room => {
    let buildingName = room.buildingName && room.buildingName.trim() !== '' ? room.buildingName : null
    if (!buildingName && room.roomNo) {
      const match = room.roomNo.match(/^(.+?)#/)
      if (match) {
        buildingName = match[1] + '#'
      }
    }
    if (buildingName) {
      if (!buildingsMap.has(buildingName)) {
        buildingsMap.set(buildingName, {
          id: buildingName,
          buildingName: buildingName
        })
      }
    }
  })
  return Array.from(buildingsMap.values())
})

// 楼层列表
const floorList = computed(() => {
  const floors = new Set<number>()
  let rooms = roomList.value
  if (filterForm.buildingId) {
    rooms = rooms.filter(room => {
      let roomBuildingName = room.buildingName && room.buildingName.trim() !== '' ? room.buildingName : null
      if (!roomBuildingName && room.roomNo) {
        const match = room.roomNo.match(/^(.+?)#/)
        if (match) {
          roomBuildingName = match[1] + '#'
        }
      }
      return roomBuildingName === filterForm.buildingId
    })
  }
  rooms.forEach(room => {
    if (room.floor || room.floorNumber) {
      floors.add(room.floor || room.floorNumber)
    }
  })
  return Array.from(floors).sort((a, b) => a - b)
})

// 统计数据
const totalRooms = computed(() => roomList.value.length)
const totalBeds = computed(() => roomList.value.reduce((sum, room) => sum + room.maxOccupancy, 0))
const occupancyRate = computed(() => {
  if (totalRooms.value === 0) return 0
  const occupiedBeds = roomList.value.reduce((sum, room) => sum + room.currentOccupancy, 0)
  return totalBeds.value > 0 ? Math.round((occupiedBeds / totalBeds.value) * 100) : 0
})
const availableBeds = computed(() => {
  return roomList.value.reduce((sum, room) => sum + (room.maxOccupancy - room.currentOccupancy), 0)
})

// 过滤后的房间列表
const filteredRooms = computed(() => {
  let rooms = [...roomList.value]

  if (filterForm.buildingId) {
    rooms = rooms.filter(room => {
      let roomBuildingName = room.buildingName && room.buildingName.trim() !== '' ? room.buildingName : null
      if (!roomBuildingName && room.roomNo) {
        const match = room.roomNo.match(/^(.+?)#/)
        if (match) {
          roomBuildingName = match[1] + '#'
        }
      }
      return roomBuildingName === filterForm.buildingId
    })
  }

  if (filterForm.floor) {
    rooms = rooms.filter(room => room.floor === filterForm.floor)
  }

  if (filterForm.genderType) {
    rooms = rooms.filter(room => room.genderType === filterForm.genderType)
  }

  if (filterForm.occupancyStatus) {
    rooms = rooms.filter(room => {
      const rate = room.currentOccupancy / room.maxOccupancy
      if (filterForm.occupancyStatus === 'empty') return rate === 0
      if (filterForm.occupancyStatus === 'partial') return rate > 0 && rate < 1
      if (filterForm.occupancyStatus === 'full') return rate === 1
      return true
    })
  }

  if (filterForm.roomNo) {
    rooms = rooms.filter(room => room.roomNo.includes(filterForm.roomNo))
  }

  return rooms
})

// 楼层分布数据
const buildingFloorData = computed(() => {
  const buildingMap = new Map<string, {
    buildingName: string
    genderType: number
    totalRooms: number
    occupiedBeds: number
    totalBeds: number
    occupancyRate: number
    floors: { floor: number; rooms: any[] }[]
  }>()

  filteredRooms.value.forEach(room => {
    // 获取楼栋名称
    let buildingName = room.buildingName && room.buildingName.trim() !== '' ? room.buildingName : '未知楼栋'
    if (buildingName === '未知楼栋' && room.roomNo) {
      const match = room.roomNo.match(/^(.+?)#/)
      if (match) {
        buildingName = match[1] + '#'
      }
    }

    // 初始化楼栋数据
    if (!buildingMap.has(buildingName)) {
      buildingMap.set(buildingName, {
        buildingName,
        genderType: room.genderType || 3,
        totalRooms: 0,
        occupiedBeds: 0,
        totalBeds: 0,
        occupancyRate: 0,
        floors: []
      })
    }

    const building = buildingMap.get(buildingName)!
    building.totalRooms++
    building.occupiedBeds += room.currentOccupancy || 0
    building.totalBeds += room.maxOccupancy || 0

    // 获取楼层
    const floorNum = room.floor || room.floorNumber || 1
    let floor = building.floors.find(f => f.floor === floorNum)
    if (!floor) {
      floor = { floor: floorNum, rooms: [] }
      building.floors.push(floor)
    }
    floor.rooms.push(room)
  })

  // 计算入住率并排序
  const result = Array.from(buildingMap.values()).map(building => {
    building.occupancyRate = building.totalBeds > 0
      ? Math.round((building.occupiedBeds / building.totalBeds) * 100)
      : 0
    // 楼层按楼层号排序
    building.floors.sort((a, b) => a.floor - b.floor)
    // 每层房间按房间号排序
    building.floors.forEach(floor => {
      floor.rooms.sort((a, b) => {
        const aNo = a.roomNo || a.dormitoryNo || ''
        const bNo = b.roomNo || b.dormitoryNo || ''
        return aNo.localeCompare(bNo, 'zh-CN', { numeric: true })
      })
    })
    return building
  })

  // 楼栋按名称排序
  result.sort((a, b) => a.buildingName.localeCompare(b.buildingName, 'zh-CN', { numeric: true }))

  // 默认展开第一个楼栋
  if (result.length > 0 && expandedBuildings.value.length === 0) {
    expandedBuildings.value = [result[0].buildingName]
  }

  return result
})

// 切换楼栋展开/收起
const toggleBuildingExpand = (buildingName: string) => {
  const idx = expandedBuildings.value.indexOf(buildingName)
  if (idx >= 0) {
    expandedBuildings.value.splice(idx, 1)
  } else {
    expandedBuildings.value.push(buildingName)
  }
}

// 房间状态样式
const getRoomStatusClass = (room: any) => {
  if (room.status === 0) {
    // 停用/维修
    return 'border-gray-300 bg-gray-200'
  }
  const rate = room.currentOccupancy / room.maxOccupancy
  if (rate === 0) return 'border-gray-300 bg-white hover:border-blue-400'
  if (rate < 1) return 'border-blue-300 bg-blue-100 hover:border-blue-500'
  return 'border-emerald-300 bg-emerald-100 hover:border-emerald-500'
}

const getRoomTextClass = (room: any) => {
  if (room.status === 0) return 'text-gray-500'
  const rate = room.currentOccupancy / room.maxOccupancy
  if (rate === 0) return 'text-gray-700'
  if (rate < 1) return 'text-blue-700'
  return 'text-emerald-700'
}

const getRoomSubTextClass = (room: any) => {
  if (room.status === 0) return 'text-gray-400'
  const rate = room.currentOccupancy / room.maxOccupancy
  if (rate === 0) return 'text-gray-500'
  if (rate < 1) return 'text-blue-600'
  return 'text-emerald-600'
}

// 添加学生对话框
const addStudentDialogVisible = ref(false)
const currentRoom = ref<any>(null)
const addStudentForm = reactive({
  studentId: null as number | null,
  bedNumber: null as number | null
})

// 解析床位号，从 "床位1" 或 "1" 格式提取数字
const parseBedNumber = (bedNumber: any): string | null => {
  if (!bedNumber) return null
  const str = String(bedNumber)
  // 尝试匹配 "床位X" 格式
  const match = str.match(/床位(\d+)/)
  if (match) return match[1]
  // 如果是纯数字，直接返回
  if (/^\d+$/.test(str)) return str
  return null
}

// 可用床位号
const availableBedNumbers = computed(() => {
  if (!currentRoom.value) return []
  const occupied = (currentRoom.value.students || [])
    .map((s: any) => parseBedNumber(s.bedNumber))
    .filter((num): num is string => num !== null)
  return Array.from({ length: currentRoom.value.maxOccupancy }, (_, i) => i + 1)
    .filter(num => !occupied.includes(String(num)))
})

// 获取床位信息
const getBedInfo = (bedNum: number) => {
  if (!currentRoom.value) return { occupied: false, student: null }
  const students = currentRoom.value.students || []
  const student = students.find((s: any) => {
    const parsed = parseBedNumber(s.bedNumber)
    return parsed === String(bedNum)
  })
  return {
    occupied: !!student,
    student: student || null
  }
}

// 获取床位状态样式
const getBedStatusClass = (bedNum: number) => {
  const info = getBedInfo(bedNum)
  if (info.occupied) {
    return 'border-green-400 bg-green-50 cursor-default'
  }
  if (addStudentForm.bedNumber === bedNum) {
    return 'border-teal-500 bg-teal-50 cursor-pointer'
  }
  return 'border-dashed border-gray-300 bg-white cursor-pointer hover:border-teal-300 hover:bg-teal-50/50'
}

// 处理床位点击
const handleBedClick = (bedNum: number) => {
  const info = getBedInfo(bedNum)
  if (info.occupied) {
    // 已入住的床位不能选择
    return
  }
  addStudentForm.bedNumber = bedNum
}

// 班级Tab列表
const studentClassTabs = computed(() => {
  const classMap = new Map()
  allStudentsForAssignment.value.forEach(student => {
    if (student.classId && student.className) {
      if (!classMap.has(student.classId)) {
        classMap.set(student.classId, {
          classId: student.classId,
          className: student.className,
          totalCount: 0,
          unassignedCount: 0
        })
      }
      const classInfo = classMap.get(student.classId)
      classInfo.totalCount++
      if (!student.dormitoryId) {
        classInfo.unassignedCount++
      }
    }
  })
  return Array.from(classMap.values())
})

// 未分配学生数量
const unassignedCount = computed(() => {
  return allStudentsForAssignment.value.filter(s => !s.dormitoryId).length
})

// 本宿舍学生数量
const currentDormCount = computed(() => {
  if (!currentRoom.value?.id) return 0
  return allStudentsForAssignment.value.filter(s => s.dormitoryId === currentRoom.value.id).length
})

// 过滤后用于显示的学生列表
const filteredStudentsForDisplay = computed(() => {
  let students = [...allStudentsForAssignment.value]

  if (activeClassTab.value && studentClassTabs.value.length > 0) {
    students = students.filter(s => s.classId === activeClassTab.value)
  }

  if (studentStatusFilter.value === 'unassigned') {
    students = students.filter(s => !s.dormitoryId)
  } else if (studentStatusFilter.value === 'currentDorm') {
    students = students.filter(s => s.dormitoryId === currentRoom.value?.id)
  } else if (studentStatusFilter.value === 'otherDorm') {
    students = students.filter(s => s.dormitoryId && s.dormitoryId !== currentRoom.value?.id)
  }

  if (studentSearchKeyword.value.trim()) {
    const keyword = studentSearchKeyword.value.toLowerCase()
    students = students.filter(student => {
      const matchName = student.realName?.toLowerCase().includes(keyword)
      const matchNo = student.studentNo?.toLowerCase().includes(keyword)
      const matchClass = student.className?.toLowerCase().includes(keyword)
      return matchName || matchNo || matchClass
    })
  }

  return students
})

// 交换对话框
const swapDialogVisible = ref(false)
const swapForm = reactive({
  studentA: null as any,
  studentBId: null as number | null
})

// 提取床位号中的数字部分
const extractBedNumber = (bedNumber: any): string | number => {
  if (!bedNumber) return ''
  // 如果本身就是数字，直接返回
  if (typeof bedNumber === 'number') return bedNumber
  // 从字符串中提取数字（如 "床位1" -> "1", "1号床" -> "1"）
  const match = String(bedNumber).match(/\d+/)
  return match ? match[0] : ''
}

const getFullRoomNo = (room: any) => {
  // 如果有 buildingNo，使用 buildingNo-roomNo 格式
  if (room.buildingNo) {
    return `${room.buildingNo}-${room.roomNo || room.dormitoryNo}`
  }
  // 如果有 buildingName，尝试提取楼号
  if (room.buildingName) {
    // 尝试从 buildingName 提取数字（如 "27#宿舍楼" -> "27"）
    const match = room.buildingName.match(/^(\d+)/)
    if (match) {
      return `${match[1]}-${room.roomNo || room.dormitoryNo}`
    }
    // 如果是其他格式，如 "A栋" -> "A"
    const letterMatch = room.buildingName.match(/^([A-Za-z])/)
    if (letterMatch) {
      return `${letterMatch[1]}-${room.roomNo || room.dormitoryNo}`
    }
  }
  // 默认只返回房间号
  return room.roomNo || room.dormitoryNo
}

// 获取入住进度条颜色
const getOccupancyBarColor = (room: any) => {
  const rate = room.currentOccupancy / room.maxOccupancy
  if (rate === 0) return 'bg-gray-300'
  if (rate < 0.5) return 'bg-amber-400'
  if (rate < 1) return 'bg-teal-500'
  return 'bg-emerald-500'
}

// 加载房间列表 - V2 API
const loadRoomList = async () => {
  loading.value = true
  try {
    const response = await getDormitories({ pageNum: 1, pageSize: 1000 })
    roomList.value = response.records || []
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '加载宿舍列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索已分配宿舍的学生
const searchStudentsWithRoom = async (query: string) => {
  if (!query) {
    studentsWithRoom.value = []
    return
  }

  searchLoading.value = true
  try {
    // V2: name 代替 realName
    const response = await getStudents({
      name: query,
      pageNum: 1,
      pageSize: 20
    })
    // V2 暂无 hasRoom 过滤，前端过滤有宿舍的学生
    studentsWithRoom.value = (response.records || []).filter((s: any) => s.dormitoryId)
  } catch {
    // 搜索失败
  } finally {
    searchLoading.value = false
  }
}

// 筛选变化
const handleFilterChange = () => {
  // 筛选逻辑在computed中自动处理
}

// 重置筛选
const handleReset = () => {
  Object.assign(filterForm, {
    buildingId: null,
    floor: null,
    genderType: null,
    occupancyStatus: null,
    roomNo: ''
  })
}

// 查看房间详情
const handleViewRoom = (room: any) => {
  currentRoom.value = room
}

// 添加学生
const handleAddStudent = async (room: any) => {
  currentRoom.value = room
  addStudentDialogVisible.value = true

  studentSearchKeyword.value = ''
  studentStatusFilter.value = 'all'
  activeClassTab.value = null

  searchLoading.value = true
  boundClassNames.value = ''  // 重置绑定班级名称
  try {
    // 先获取宿舍详情，获取绑定的班级信息
    const { getDormitoryDetail } = await import('@/api/dormitory')
    const dormitoryDetail = await getDormitoryDetail(room.id)

    // 保存绑定班级名称
    boundClassNames.value = dormitoryDetail?.assignedClassNames || ''

    // 获取该宿舍绑定的班级ID列表
    const assignedClassIds = dormitoryDetail?.assignedClassIds
    let allowedClassIds: string[] = []
    if (assignedClassIds) {
      allowedClassIds = String(assignedClassIds).split(',').map(id => id.trim()).filter(id => id)
    }

    // 如果宿舍没有绑定任何班级，则提示并不允许分配
    if (allowedClassIds.length === 0) {
      ElMessage.warning('该宿舍尚未绑定班级，请先在班级管理中为班级分配宿舍')
      addStudentDialogVisible.value = false
      searchLoading.value = false
      return
    }

    // V2 API
    const response = await getStudents({
      pageNum: 1,
      pageSize: 500
    })

    let students = response.records || []

    // 按宿舍性别类型过滤
    const genderType = room.genderType || dormitoryDetail?.genderType
    if (genderType === 1) {
      students = students.filter((s: any) => s.gender === 1)
    } else if (genderType === 2) {
      students = students.filter((s: any) => s.gender === 2)
    }

    // 按绑定的班级过滤 - 只显示班级属于该宿舍绑定班级的学生
    // 但是已经在当前宿舍的学生始终显示（以便管理）
    const currentDormitoryId = room.id
    students = students.filter((s: any) => {
      // 已在当前宿舍的学生始终显示
      if (s.dormitoryId === currentDormitoryId) return true
      // 其他学生需要满足班级条件
      if (!s.classId) return false
      return allowedClassIds.includes(String(s.classId))
    })

    allStudentsForAssignment.value = students

    if (studentClassTabs.value.length > 0) {
      activeClassTab.value = studentClassTabs.value[0].classId
    }
  } catch {
    ElMessage.error('加载学生列表失败')
  } finally {
    searchLoading.value = false
  }
}

// 移除学生
const handleRemoveStudent = async (student: any, room: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要将学生 "${student.realName}" 从宿舍 "${room.roomNo}" 移除吗?`,
      '移除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await removeStudentFromDormitory(student.id)
    ElMessage.success('移除成功')
    await loadRoomList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '移除失败')
    }
  }
}

// 交换学生
const handleSwapStudent = (student: any) => {
  swapForm.studentA = student
  swapDialogVisible.value = true
}

// 提交添加学生
const handleAddSubmit = async () => {
  if (!addStudentForm.studentId || !addStudentForm.bedNumber) {
    ElMessage.warning('请选择学生和床位号')
    return
  }

  // 获取选中的学生信息
  const selectedStudent = allStudentsForAssignment.value.find(
    s => s.id === addStudentForm.studentId
  )

  // 如果学生已分配宿舍，弹出确认对话框
  if (selectedStudent?.dormitoryId) {
    try {
      await ElMessageBox.confirm(
        `学生「${selectedStudent.realName}」(${selectedStudent.studentNo}) 当前已分配在:\n\n` +
        `${selectedStudent.buildingName || ''} ${selectedStudent.roomNo || ''} - ${selectedStudent.bedNumber || ''}号床\n\n` +
        `确定要将该学生更换到:\n\n` +
        `${currentRoom.value.buildingName || ''} ${currentRoom.value.roomNo || ''} - ${addStudentForm.bedNumber}号床 吗？`,
        '更换宿舍确认',
        {
          confirmButtonText: '确定更换',
          cancelButtonText: '取消',
          type: 'warning',
          dangerouslyUseHTMLString: false
        }
      )
    } catch {
      // 用户取消
      return
    }
  }

  submitLoading.value = true
  try {
    const oldDormitoryId = selectedStudent?.dormitoryId
    const oldBedNumber = selectedStudent?.bedNumber

    // 如果学生已有宿舍，先移除原宿舍分配
    if (oldDormitoryId) {
      await removeStudentFromDormitory(selectedStudent.id)
    }

    // 分配到新宿舍
    await assignStudentToDormitory({
      studentId: addStudentForm.studentId,
      dormitoryId: currentRoom.value.id,
      bedNumber: addStudentForm.bedNumber
    })

    // 局部更新数据，避免页面刷新
    // 1. 更新当前房间的学生列表和入住人数
    const targetRoom = roomList.value.find(r => r.id === currentRoom.value.id)
    if (targetRoom) {
      // 添加新学生到房间
      if (!targetRoom.students) {
        targetRoom.students = []
      }
      targetRoom.students.push({
        id: selectedStudent?.id,
        realName: selectedStudent?.realName,
        studentNo: selectedStudent?.studentNo,
        bedNumber: addStudentForm.bedNumber
      })
      targetRoom.currentOccupancy = (targetRoom.currentOccupancy || 0) + 1
    }

    // 2. 如果是更换宿舍，更新原房间的数据
    if (oldDormitoryId) {
      const oldRoom = roomList.value.find(r => r.id === oldDormitoryId)
      if (oldRoom) {
        // 从原房间移除学生
        if (oldRoom.students) {
          oldRoom.students = oldRoom.students.filter(s => s.id !== selectedStudent?.id)
        }
        oldRoom.currentOccupancy = Math.max(0, (oldRoom.currentOccupancy || 1) - 1)
      }
    }

    // 3. 更新学生列表中该学生的分配状态
    if (selectedStudent) {
      selectedStudent.dormitoryId = currentRoom.value.id
      selectedStudent.buildingName = currentRoom.value.buildingName
      selectedStudent.roomNo = currentRoom.value.roomNo
      selectedStudent.bedNumber = addStudentForm.bedNumber
    }

    // 4. 更新 currentRoom 的引用（确保界面同步）
    if (targetRoom) {
      currentRoom.value = { ...targetRoom }
    }

    ElMessage.success(oldDormitoryId ? '宿舍更换成功' : '分配成功')
    addStudentDialogVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败')
    // 出错时刷新数据以确保一致性
    await loadRoomList()
  } finally {
    submitLoading.value = false
  }
}

// 提交交换
const handleSwapSubmit = async () => {
  if (!swapForm.studentBId) {
    ElMessage.warning('请选择要交换的学生')
    return
  }

  submitLoading.value = true
  try {
    await swapStudentDormitory({
      studentAId: swapForm.studentA.id,
      studentBId: swapForm.studentBId
    })
    ElMessage.success('交换成功')
    swapDialogVisible.value = false
    await loadRoomList()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '交换失败')
  } finally {
    submitLoading.value = false
  }
}

// 表格行点击处理
const handleStudentRowClick = (student: any) => {
  // 允许选择已分配宿舍的学生（提交时会提示确认更换）
  addStudentForm.studentId = student.id
}

// 卡片点击处理
const handleStudentCardClick = (student: any) => {
  // 允许选择已分配宿舍的学生（提交时会提示确认更换）
  addStudentForm.studentId = student.id
}

// 初始化
onMounted(() => {
  loadRoomList()
})
</script>

<style scoped>
@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in {
  animation: fade-in 0.2s ease-out;
}

tbody tr {
  animation: fade-in 0.3s ease-out both;
}
</style>
