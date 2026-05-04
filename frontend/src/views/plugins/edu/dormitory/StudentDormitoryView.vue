<template>
  <div class="min-h-full bg-gray-50 p-6">
    <!-- Header -->
    <div class="mb-4">
      <h1 class="text-xl font-semibold text-gray-900">住宿管理</h1>
      <p class="mt-0.5 text-sm text-gray-500">管理入住、退宿、调换宿舍等操作</p>
    </div>

    <!-- Stat Bar -->
    <div class="mb-4 flex items-center gap-4 rounded-lg border border-gray-200 bg-white px-5 py-2.5">
      <span class="text-sm text-gray-500">在住 <span class="font-semibold text-emerald-600">{{ filteredRecords.length }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">楼栋 <span class="font-semibold text-gray-900">{{ buildingOptions.length }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">总记录 <span class="font-semibold text-gray-900">{{ allRecords.length }}</span></span>
    </div>

    <!-- Filter Bar -->
    <div class="mb-4 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-end gap-3">
        <div class="w-36">
          <label class="mb-1 block text-xs font-medium text-gray-600">姓名</label>
          <input
            v-model="query.name"
            type="text"
            placeholder="姓名"
            class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            @keyup.enter="applyFilter"
          />
        </div>
        <div class="w-40">
          <label class="mb-1 block text-xs font-medium text-gray-600">宿舍楼</label>
          <select
            v-model="query.buildingId"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            @change="handleBuildingChange"
          >
            <option :value="undefined">全部楼栋</option>
            <option v-for="b in buildingOptions" :key="b.id" :value="b.id">{{ b.placeName }}</option>
          </select>
        </div>
        <div class="flex gap-2">
          <button
            class="h-9 rounded-md bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
            @click="applyFilter"
          >查询</button>
          <button
            class="h-9 rounded-md border border-gray-300 px-4 text-sm font-medium text-gray-600 hover:bg-gray-50"
            @click="handleReset"
          >重置</button>
        </div>
        <div class="ml-auto flex gap-2">
          <button
            class="h-9 rounded-md bg-emerald-600 px-4 text-sm font-medium text-white hover:bg-emerald-700"
            @click="openCheckInDialog"
          >入住登记</button>
          <button
            class="h-9 rounded-md border border-gray-300 px-4 text-sm font-medium text-gray-600 hover:bg-gray-50"
            @click="openBatchCheckInDialog"
          >批量入住</button>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div v-if="loading" class="flex items-center justify-center py-20">
        <div class="h-6 w-6 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
      </div>
      <table v-else class="w-full text-sm">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50/80">
            <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500">姓名</th>
            <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500">组织</th>
            <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500">宿舍楼</th>
            <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500">房间号</th>
            <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500">床位</th>
            <th class="px-4 py-3 text-left text-xs font-semibold text-gray-500">入住日期</th>
            <th class="px-4 py-3 text-center text-xs font-semibold text-gray-500">状态</th>
            <th class="px-4 py-3 text-center text-xs font-semibold text-gray-500">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="paginatedRecords.length === 0">
            <td colspan="8" class="py-12 text-center text-sm text-gray-400">暂无住宿记录</td>
          </tr>
          <tr
            v-for="record in paginatedRecords"
            :key="record.id"
            class="border-b border-gray-100 hover:bg-gray-50/50"
          >
            <td class="px-4 py-3 font-medium text-gray-900">{{ record.occupantName || '-' }}</td>
            <td class="px-4 py-3 text-xs text-gray-600">{{ record.orgUnitName || '-' }}</td>
            <td class="px-4 py-3 text-xs text-gray-600">{{ record.buildingName || '-' }}</td>
            <td class="px-4 py-3 text-xs text-gray-600">{{ record.placeName || '-' }}</td>
            <td class="px-4 py-3 text-xs text-gray-600">{{ record.positionNo || '-' }}</td>
            <td class="px-4 py-3 text-xs text-gray-500">{{ formatDate(record.checkInTime) }}</td>
            <td class="px-4 py-3 text-center">
              <span
                class="inline-block rounded px-2 py-0.5 text-[10px] font-medium"
                :class="record.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'"
              >{{ record.status === 1 ? '在住' : '已退' }}</span>
            </td>
            <td class="px-4 py-3 text-center">
              <div class="flex items-center justify-center gap-1">
                <button
                  v-if="record.status === 1"
                  class="rounded px-2 py-0.5 text-xs text-amber-600 hover:bg-amber-50"
                  @click="openTransferDialog(record)"
                >调换</button>
                <button
                  v-if="record.status === 1"
                  class="rounded px-2 py-0.5 text-xs text-red-500 hover:bg-red-50"
                  @click="handleCheckOut(record)"
                >退宿</button>
                <button
                  class="rounded px-2 py-0.5 text-xs text-blue-600 hover:bg-blue-50"
                  @click="openHistoryDialog(record)"
                >历史</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <!-- Pagination -->
      <div v-if="filteredRecords.length > 0" class="flex items-center justify-between border-t border-gray-100 px-4 py-3">
        <span class="text-xs text-gray-500">共 {{ filteredRecords.length }} 条</span>
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="filteredRecords.length"
          :page-sizes="[20, 50, 100]"
          layout="sizes, prev, pager, next"
          small
        />
      </div>
    </div>

    <!-- Check-In Dialog (Single) -->
    <el-dialog
      v-model="checkInDialogVisible"
      title="入住登记"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="space-y-4">
        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">搜索学生</label>
          <input
            v-model="studentSearchQuery"
            type="text"
            placeholder="输入姓名或学号搜索..."
            class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            @input="handleStudentSearch"
          />
          <div v-if="studentSearchResults.length > 0" class="mt-2 max-h-40 overflow-y-auto rounded-md border border-gray-200">
            <div
              v-for="s in studentSearchResults"
              :key="s.id"
              class="cursor-pointer px-3 py-2 text-sm hover:bg-blue-50"
              :class="checkInData.occupantId === s.id ? 'bg-blue-50 font-medium' : ''"
              @click="selectStudent(s)"
            >
              <span class="text-gray-900">{{ s.realName || s.name }}</span>
              <span class="ml-2 text-xs text-gray-500">{{ s.studentNo }}</span>
              <span v-if="s.className" class="ml-2 text-xs text-gray-400">{{ s.className }}</span>
            </div>
          </div>
          <div v-if="checkInData.occupantName" class="mt-2 rounded-md bg-blue-50 px-3 py-2 text-sm text-blue-700">
            已选择: {{ checkInData.occupantName }} ({{ checkInData.username }})
          </div>
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">选择宿舍楼</label>
          <select
            v-model="checkInData.buildingId"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            @change="loadAvailableRooms"
          >
            <option :value="undefined">请选择楼栋</option>
            <option v-for="b in buildingOptions" :key="b.id" :value="b.id">{{ b.placeName }}</option>
          </select>
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">选择房间</label>
          <select
            v-model="checkInData.roomId"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            :disabled="!checkInData.buildingId"
          >
            <option :value="undefined">请选择房间</option>
            <option v-for="r in availableRooms" :key="r.id" :value="r.id">
              {{ r.placeName }} ({{ r.currentOccupancy || 0 }}/{{ r.capacity || 0 }})
            </option>
          </select>
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">床位号 (可选)</label>
          <input
            v-model="checkInData.positionNo"
            type="text"
            placeholder="如: 1, 2, 3..."
            class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">备注</label>
          <input
            v-model="checkInData.remark"
            type="text"
            placeholder="备注信息"
            class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="checkInDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" :disabled="!checkInData.occupantId || !checkInData.roomId" @click="handleSingleCheckIn">
            确认入住
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Batch Check-In Dialog -->
    <el-dialog
      v-model="batchCheckInDialogVisible"
      title="批量入住"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="space-y-4">
        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">选择宿舍楼</label>
          <select
            v-model="batchData.buildingId"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            @change="loadBatchAvailableRooms"
          >
            <option :value="undefined">请选择楼栋</option>
            <option v-for="b in buildingOptions" :key="b.id" :value="b.id">{{ b.placeName }}</option>
          </select>
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">选择房间</label>
          <select
            v-model="batchData.roomId"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            :disabled="!batchData.buildingId"
          >
            <option :value="undefined">请选择房间</option>
            <option v-for="r in batchAvailableRooms" :key="r.id" :value="r.id">
              {{ r.placeName }} ({{ r.currentOccupancy || 0 }}/{{ r.capacity || 0 }})
            </option>
          </select>
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">搜索并添加学生</label>
          <input
            v-model="batchStudentSearch"
            type="text"
            placeholder="输入姓名或学号搜索..."
            class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            @input="handleBatchStudentSearch"
          />
          <div v-if="batchStudentResults.length > 0" class="mt-2 max-h-32 overflow-y-auto rounded-md border border-gray-200">
            <div
              v-for="s in batchStudentResults"
              :key="s.id"
              class="cursor-pointer px-3 py-2 text-sm hover:bg-blue-50"
              @click="addBatchStudent(s)"
            >
              <span class="text-gray-900">{{ s.realName || s.name }}</span>
              <span class="ml-2 text-xs text-gray-500">{{ s.studentNo }}</span>
            </div>
          </div>
        </div>
        <!-- Selected students -->
        <div v-if="batchData.students.length > 0">
          <label class="mb-1 block text-xs font-medium text-gray-600">已选学生 ({{ batchData.students.length }}人)</label>
          <div class="flex flex-wrap gap-1.5">
            <span
              v-for="(s, i) in batchData.students"
              :key="s.id"
              class="inline-flex items-center gap-1 rounded-full bg-blue-100 px-2.5 py-1 text-xs text-blue-700"
            >
              {{ s.name }} ({{ s.studentNo }})
              <button class="ml-0.5 rounded-full p-0.5 hover:bg-blue-200" @click="batchData.students.splice(i, 1)">
                <span class="text-xs">&times;</span>
              </button>
            </span>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="batchCheckInDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" :disabled="batchData.students.length === 0 || !batchData.roomId" @click="handleBatchCheckIn">
            批量入住 ({{ batchData.students.length }}人)
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Transfer Dialog -->
    <el-dialog
      v-model="transferDialogVisible"
      title="调换宿舍"
      width="480px"
      :close-on-click-modal="false"
    >
      <div v-if="transferRecord" class="space-y-4">
        <div class="rounded-md bg-gray-50 px-3 py-2 text-sm">
          <span class="text-gray-500">当前:</span>
          <span class="ml-1 font-medium text-gray-900">{{ transferRecord.occupantName }}</span>
          <span class="ml-2 text-gray-500">{{ transferRecord.buildingName }} - {{ transferRecord.placeName }}</span>
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">目标楼栋</label>
          <select
            v-model="transferData.buildingId"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            @change="loadTransferRooms"
          >
            <option :value="undefined">请选择楼栋</option>
            <option v-for="b in buildingOptions" :key="b.id" :value="b.id">{{ b.placeName }}</option>
          </select>
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">目标房间</label>
          <select
            v-model="transferData.newRoomId"
            class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            :disabled="!transferData.buildingId"
          >
            <option :value="undefined">请选择房间</option>
            <option v-for="r in transferRooms" :key="r.id" :value="r.id">
              {{ r.placeName }} ({{ r.currentOccupancy || 0 }}/{{ r.capacity || 0 }})
            </option>
          </select>
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">新床位号 (可选)</label>
          <input
            v-model="transferData.newPositionNo"
            type="text"
            placeholder="如: 1, 2, 3..."
            class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-gray-600">调换原因</label>
          <input
            v-model="transferData.reason"
            type="text"
            placeholder="请输入调换原因"
            class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="transferDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" :disabled="!transferData.newRoomId" @click="handleTransfer">
            确认调换
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- History Dialog -->
    <el-dialog
      v-model="historyDialogVisible"
      :title="historyRecord ? historyRecord.occupantName + ' - 住宿历史' : '住宿历史'"
      width="640px"
    >
      <div v-if="historyLoading" class="flex justify-center py-8">
        <div class="h-5 w-5 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
      </div>
      <table v-else-if="historyRecords.length > 0" class="w-full text-sm">
        <thead>
          <tr class="border-b border-gray-100 bg-gray-50/50">
            <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">宿舍楼</th>
            <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">房间</th>
            <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">床位</th>
            <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">入住日期</th>
            <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">退宿日期</th>
            <th class="px-3 py-2 text-center text-xs font-medium text-gray-500">状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="h in historyRecords" :key="h.id" class="border-b border-gray-50">
            <td class="px-3 py-2 text-gray-700">{{ h.buildingName || '-' }}</td>
            <td class="px-3 py-2 text-gray-700">{{ h.placeName || '-' }}</td>
            <td class="px-3 py-2 text-gray-600">{{ h.positionNo || '-' }}</td>
            <td class="px-3 py-2 text-xs text-gray-500">{{ formatDate(h.checkInTime) }}</td>
            <td class="px-3 py-2 text-xs text-gray-500">{{ h.checkOutTime ? formatDate(h.checkOutTime) : '-' }}</td>
            <td class="px-3 py-2 text-center">
              <span
                class="rounded px-2 py-0.5 text-[10px] font-medium"
                :class="h.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'"
              >
                {{ h.status === 1 ? '在住' : '已退' }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="py-8 text-center text-sm text-gray-400">暂无历史记录</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { universalPlaceApi } from '@/api/universalPlace'
import type { PlaceTreeNode, PlaceOccupantWithPlace } from '@/types/universalPlace'
import { getStudents } from '@/api/student'

// ==================== State ====================

const loading = ref(false)
const submitLoading = ref(false)
const historyLoading = ref(false)

const allRecords = ref<PlaceOccupantWithPlace[]>([])
const pageNum = ref(1)
const pageSize = ref(20)

const query = reactive({
  name: undefined as string | undefined,
  username: undefined as string | undefined,
  buildingId: undefined as number | string | undefined
})

// Building options (dormitory buildings from place tree)
const buildingOptions = ref<PlaceTreeNode[]>([])
// Map: buildingId -> list of room placeIds (for querying occupants)
const buildingRoomIdsMap = ref<Map<number | string, (number | string)[]>>(new Map())
// All room IDs across all dorm buildings
const allDormRoomIds = ref<(number | string)[]>([])

// ==================== Computed ====================

const filteredRecords = computed(() => {
  let result = allRecords.value
  if (query.name) {
    const q = query.name.toLowerCase()
    result = result.filter(r => r.occupantName?.toLowerCase().includes(q))
  }
  if (query.username) {
    const q = query.username.toLowerCase()
    result = result.filter(r => r.username?.toLowerCase().includes(q))
  }
  if (query.buildingId) {
    const roomIds = buildingRoomIdsMap.value.get(query.buildingId) || []
    const roomIdSet = new Set(roomIds.map(String))
    result = result.filter(r => roomIdSet.has(String(r.placeId)))
  }
  return result
})

const paginatedRecords = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value
  return filteredRecords.value.slice(start, start + pageSize.value)
})

// ==================== Check-In ====================

const checkInDialogVisible = ref(false)
const checkInData = reactive({
  occupantId: undefined as number | string | undefined,
  occupantName: '',
  username: '',
  buildingId: undefined as number | string | undefined,
  roomId: undefined as number | string | undefined,
  positionNo: '',
  remark: ''
})
const studentSearchQuery = ref('')
const studentSearchResults = ref<any[]>([])
const availableRooms = ref<PlaceTreeNode[]>([])

// ==================== Batch Check-In ====================

const batchCheckInDialogVisible = ref(false)
const batchData = reactive({
  buildingId: undefined as number | string | undefined,
  roomId: undefined as number | string | undefined,
  students: [] as Array<{ id: number | string; name: string; studentNo: string }>
})
const batchStudentSearch = ref('')
const batchStudentResults = ref<any[]>([])
const batchAvailableRooms = ref<PlaceTreeNode[]>([])

// ==================== Transfer ====================

const transferDialogVisible = ref(false)
const transferRecord = ref<PlaceOccupantWithPlace | null>(null)
const transferData = reactive({
  buildingId: undefined as number | string | undefined,
  newRoomId: undefined as number | string | undefined,
  newPositionNo: '',
  reason: ''
})
const transferRooms = ref<PlaceTreeNode[]>([])

// ==================== History ====================

const historyDialogVisible = ref(false)
const historyRecord = ref<PlaceOccupantWithPlace | null>(null)
const historyRecords = ref<PlaceOccupantWithPlace[]>([])

// ==================== Load Data ====================

const loadBuildings = async () => {
  try {
    const tree = await universalPlaceApi.getTree()
    const dormBuildings: PlaceTreeNode[] = []
    const isDormBuilding = (node: PlaceTreeNode): boolean => {
      const typeCode = (node.typeCode || '').toUpperCase()
      const typeName = (node.typeName || '').toLowerCase()
      const nameLower = (node.placeName || '').toLowerCase()
      if (['DORM_BUILDING', 'DORMITORY', 'DORM'].includes(typeCode)) return true
      return typeName.includes('宿舍') || nameLower.includes('宿舍') || nameLower.includes('公寓')
    }
    const extract = (nodes: PlaceTreeNode[]) => {
      for (const node of nodes) {
        if (isDormBuilding(node)) {
          dormBuildings.push(node)
        }
        if (node.children?.length) extract(node.children)
      }
    }
    extract(tree)
    buildingOptions.value = dormBuildings
  } catch {
    buildingOptions.value = []
  }
}

/**
 * Collect all room IDs under dormitory buildings, building the buildingRoomIdsMap.
 * Then load all occupants for those rooms.
 */
const loadAllOccupants = async () => {
  loading.value = true
  try {
    const roomMap = new Map<number | string, (number | string)[]>()
    const allRoomIds: (number | string)[] = []

    for (const building of buildingOptions.value) {
      const roomIds = await collectRoomIds(building.id)
      roomMap.set(building.id, roomIds)
      allRoomIds.push(...roomIds)
    }
    buildingRoomIdsMap.value = roomMap
    allDormRoomIds.value = allRoomIds

    if (allRoomIds.length === 0) {
      allRecords.value = []
      return
    }

    // Use the new backend endpoint to get all occupants with place info
    allRecords.value = await universalPlaceApi.getOccupantsForPlaces(
      allRoomIds as number[],
      'STUDENT'
    )
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '加载数据失败')
    allRecords.value = []
  } finally {
    loading.value = false
  }
}

/**
 * Collect room place IDs under a building (handles floor intermediary level)
 */
const collectRoomIds = async (buildingId: number | string): Promise<(number | string)[]> => {
  try {
    const children = await universalPlaceApi.getChildren(buildingId)
    const roomIds: (number | string)[] = []
    for (const child of children) {
      const typeLower = (child.typeCode || child.typeName || '').toLowerCase()
      if (typeLower.includes('floor') || typeLower.includes('楼层')) {
        try {
          const floorChildren = await universalPlaceApi.getChildren(child.id)
          for (const room of floorChildren) {
            roomIds.push(room.id)
          }
        } catch { /* skip */ }
      } else {
        roomIds.push(child.id)
      }
    }
    return roomIds
  } catch {
    return []
  }
}

const loadRoomsForBuilding = async (buildingId: number | string): Promise<PlaceTreeNode[]> => {
  try {
    const children = await universalPlaceApi.getChildren(buildingId)
    const roomList: PlaceTreeNode[] = []
    for (const child of children) {
      const typeLower = (child.typeCode || child.typeName || '').toLowerCase()
      if (typeLower.includes('floor') || typeLower.includes('楼层')) {
        try {
          const floorChildren = await universalPlaceApi.getChildren(child.id)
          for (const room of floorChildren) {
            if ((room.capacity || 0) > (room.currentOccupancy || 0)) {
              roomList.push(room as PlaceTreeNode)
            }
          }
        } catch { /* skip */ }
      } else {
        if ((child.capacity || 0) > (child.currentOccupancy || 0)) {
          roomList.push(child as PlaceTreeNode)
        }
      }
    }
    return roomList
  } catch {
    return []
  }
}

// ==================== Handlers ====================

const applyFilter = () => {
  pageNum.value = 1
}

const handleReset = () => {
  query.name = undefined
  query.username = undefined
  query.buildingId = undefined
  pageNum.value = 1
}

const handleBuildingChange = () => {
  pageNum.value = 1
}

// Single check-in
const openCheckInDialog = () => {
  checkInData.occupantId = undefined
  checkInData.occupantName = ''
  checkInData.username = ''
  checkInData.buildingId = undefined
  checkInData.roomId = undefined
  checkInData.positionNo = ''
  checkInData.remark = ''
  studentSearchQuery.value = ''
  studentSearchResults.value = []
  availableRooms.value = []
  checkInDialogVisible.value = true
}

let studentSearchTimer: ReturnType<typeof setTimeout> | null = null
const handleStudentSearch = () => {
  if (studentSearchTimer) clearTimeout(studentSearchTimer)
  studentSearchTimer = setTimeout(async () => {
    if (!studentSearchQuery.value) {
      studentSearchResults.value = []
      return
    }
    try {
      const res = await getStudents({ name: studentSearchQuery.value, pageNum: 1, pageSize: 10 })
      studentSearchResults.value = (res.records || []).map((s: any) => ({
        id: s.id,
        realName: s.realName || s.name,
        studentNo: s.studentNo,
        className: s.className
      }))
    } catch {
      studentSearchResults.value = []
    }
  }, 300)
}

const selectStudent = (s: any) => {
  checkInData.occupantId = s.id
  checkInData.occupantName = s.realName || s.name
  checkInData.username = s.studentNo
  studentSearchResults.value = []
}

const loadAvailableRooms = async () => {
  checkInData.roomId = undefined
  if (!checkInData.buildingId) {
    availableRooms.value = []
    return
  }
  availableRooms.value = await loadRoomsForBuilding(checkInData.buildingId)
}

const handleSingleCheckIn = async () => {
  if (!checkInData.occupantId || !checkInData.roomId) return
  submitLoading.value = true
  try {
    await universalPlaceApi.checkIn(checkInData.roomId, {
      occupantType: 'STUDENT',
      occupantId: checkInData.occupantId,
      occupantName: checkInData.occupantName,
      username: checkInData.username,
      positionNo: checkInData.positionNo || undefined,
      remark: checkInData.remark || undefined
    })
    ElMessage.success('入住成功')
    checkInDialogVisible.value = false
    await loadAllOccupants()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '入住失败')
  } finally {
    submitLoading.value = false
  }
}

// Batch check-in
const openBatchCheckInDialog = () => {
  batchData.buildingId = undefined
  batchData.roomId = undefined
  batchData.students = []
  batchStudentSearch.value = ''
  batchStudentResults.value = []
  batchAvailableRooms.value = []
  batchCheckInDialogVisible.value = true
}

const loadBatchAvailableRooms = async () => {
  batchData.roomId = undefined
  if (!batchData.buildingId) {
    batchAvailableRooms.value = []
    return
  }
  batchAvailableRooms.value = await loadRoomsForBuilding(batchData.buildingId)
}

let batchSearchTimer: ReturnType<typeof setTimeout> | null = null
const handleBatchStudentSearch = () => {
  if (batchSearchTimer) clearTimeout(batchSearchTimer)
  batchSearchTimer = setTimeout(async () => {
    if (!batchStudentSearch.value) {
      batchStudentResults.value = []
      return
    }
    try {
      const res = await getStudents({ name: batchStudentSearch.value, pageNum: 1, pageSize: 10 })
      batchStudentResults.value = (res.records || []).map((s: any) => ({
        id: s.id,
        realName: s.realName || s.name,
        studentNo: s.studentNo
      }))
    } catch {
      batchStudentResults.value = []
    }
  }, 300)
}

const addBatchStudent = (s: any) => {
  if (batchData.students.some(item => item.id === s.id)) {
    ElMessage.warning('该学生已添加')
    return
  }
  batchData.students.push({
    id: s.id,
    name: s.realName || s.name,
    studentNo: s.studentNo
  })
  batchStudentSearch.value = ''
  batchStudentResults.value = []
}

const handleBatchCheckIn = async () => {
  if (!batchData.roomId || batchData.students.length === 0) return
  submitLoading.value = true
  try {
    const requests = batchData.students.map(s => ({
      occupantType: 'STUDENT',
      occupantId: s.id,
      occupantName: s.name,
      username: s.studentNo
    }))
    await universalPlaceApi.batchCheckIn(batchData.roomId, requests)
    ElMessage.success(`成功入住 ${batchData.students.length} 人`)
    batchCheckInDialogVisible.value = false
    await loadAllOccupants()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '批量入住失败')
  } finally {
    submitLoading.value = false
  }
}

// Check out - uses universalPlaceApi
const handleCheckOut = async (record: PlaceOccupantWithPlace) => {
  try {
    await ElMessageBox.confirm(
      `确定要将 ${record.occupantName} 从 ${record.placeName || '宿舍'} 退宿吗？`,
      '确认退宿',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await universalPlaceApi.checkOut(record.placeId, record.id)
    ElMessage.success('退宿成功')
    await loadAllOccupants()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '退宿失败')
    }
  }
}

// Transfer - implemented as checkOut old + checkIn new
const openTransferDialog = (record: PlaceOccupantWithPlace) => {
  transferRecord.value = record
  transferData.buildingId = undefined
  transferData.newRoomId = undefined
  transferData.newPositionNo = ''
  transferData.reason = ''
  transferRooms.value = []
  transferDialogVisible.value = true
}

const loadTransferRooms = async () => {
  transferData.newRoomId = undefined
  if (!transferData.buildingId) {
    transferRooms.value = []
    return
  }
  transferRooms.value = await loadRoomsForBuilding(transferData.buildingId)
}

const handleTransfer = async () => {
  if (!transferRecord.value || !transferData.newRoomId) return
  submitLoading.value = true
  try {
    // Step 1: Check out from current room
    await universalPlaceApi.checkOut(transferRecord.value.placeId, transferRecord.value.id)
    // Step 2: Check in to new room
    await universalPlaceApi.checkIn(transferData.newRoomId, {
      occupantType: transferRecord.value.occupantType || 'STUDENT',
      occupantId: transferRecord.value.occupantId,
      occupantName: transferRecord.value.occupantName,
      username: transferRecord.value.username,
      positionNo: transferData.newPositionNo || undefined,
      remark: transferData.reason ? `调换: ${transferData.reason}` : undefined
    })
    ElMessage.success('调换成功')
    transferDialogVisible.value = false
    await loadAllOccupants()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '调换失败')
  } finally {
    submitLoading.value = false
  }
}

// History - uses universalPlaceApi occupant history by occupant
const openHistoryDialog = async (record: PlaceOccupantWithPlace) => {
  historyRecord.value = record
  historyDialogVisible.value = true
  historyLoading.value = true
  try {
    historyRecords.value = await universalPlaceApi.getOccupantHistoryByOccupant(
      record.occupantType || 'STUDENT',
      record.occupantId
    )
  } catch {
    historyRecords.value = []
  } finally {
    historyLoading.value = false
  }
}

// ==================== Helpers ====================

const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  try {
    return new Date(dateStr).toLocaleDateString('zh-CN')
  } catch {
    return dateStr
  }
}

// ==================== Lifecycle ====================

onMounted(async () => {
  await loadBuildings()
  await loadAllOccupants()
})
</script>
