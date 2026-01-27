<template>
  <div class="dormitory-tab">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 无数据状态 -->
    <div v-else-if="!distribution.length" class="empty-container">
      <Building :size="64" class="empty-icon" />
      <h3>暂无宿舍分布数据</h3>
      <p>该班级学生尚未分配宿舍，请先在宿舍管理中为班级分配宿舍</p>
    </div>

    <!-- 宿舍分布内容 -->
    <template v-else>
      <!-- 统计摘要 -->
      <div class="summary-section">
        <div class="summary-content">
          <Building :size="20" class="summary-icon" />
          <span class="summary-text">
            本班学生分布在 <strong>{{ buildingCount }}</strong> 栋楼 /
            <strong>{{ roomCount }}</strong> 间宿舍
          </span>
        </div>
        <div class="flex items-center gap-3">
          <!-- 编辑模式切换 -->
          <el-switch
            v-if="canEdit"
            v-model="editMode"
            active-text="编辑模式"
            inactive-text=""
            class="edit-mode-switch"
          />
          <div class="view-toggle">
            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button value="visual">
                <LayoutGrid :size="14" />
                <span>可视化</span>
              </el-radio-button>
              <el-radio-button value="list">
                <List :size="14" />
                <span>列表</span>
              </el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </div>

      <!-- 编辑模式提示 -->
      <div v-if="editMode" class="edit-mode-banner">
        <div class="flex items-center gap-2">
          <Edit :size="16" />
          <span>编辑模式已开启，点击下方房间卡片可以管理学生床位分配</span>
        </div>
      </div>

      <!-- 可视化视图 -->
      <div v-if="viewMode === 'visual'" class="visual-view">
        <div
          v-for="building in distribution"
          :key="building.buildingId"
          class="building-card"
        >
          <!-- 楼栋头部 -->
          <div class="building-header">
            <div class="building-info">
              <div class="building-icon" :class="getBuildingTypeClass(building.buildingType)">
                <Building :size="20" />
              </div>
              <div class="building-details">
                <h4 class="building-name">{{ building.buildingName }}</h4>
                <div class="building-meta">
                  <el-tag
                    :type="getBuildingTypeTag(building.buildingType)"
                    size="small"
                    effect="light"
                  >
                    {{ getBuildingTypeLabel(building.buildingType) }}
                  </el-tag>
                  <span class="student-count">
                    <Users :size="14" />
                    {{ building.studentCount }} 人
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 房间网格 -->
          <div class="rooms-grid">
            <div
              v-for="room in building.rooms"
              :key="room.dormitoryId"
              class="room-item"
              @click="openRoomDrawer(room, building.buildingName)"
            >
              <div class="room-no">{{ room.roomNo }}</div>
              <div class="room-count">
                <Users :size="12" />
                {{ room.studentCount }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 列表视图 -->
      <div v-else class="list-view">
        <el-table
          :data="flatRoomList"
          stripe
          class="room-table"
          empty-text="暂无宿舍数据"
        >
          <el-table-column prop="buildingName" label="楼栋" width="150">
            <template #default="{ row }">
              <div class="building-cell">
                <el-tag
                  :type="getBuildingTypeTag(row.buildingType)"
                  size="small"
                  effect="light"
                >
                  {{ row.buildingName }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="roomNo" label="房间号" width="120" />
          <el-table-column prop="floor" label="楼层" width="80">
            <template #default="{ row }">
              {{ row.floor }}F
            </template>
          </el-table-column>
          <el-table-column label="入住学生" min-width="200">
            <template #default="{ row }">
              <span class="student-names">
                {{ row.students.map((s: DormitoryStudent) => s.name).join('、') || '-' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="studentCount" label="人数" width="80" align="center">
            <template #default="{ row }">
              <el-tag type="info" size="small">{{ row.studentCount }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                size="small"
                @click="openRoomDrawerFromList(row)"
              >
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <!-- 房间详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="drawerTitle"
      direction="rtl"
      size="400px"
    >
      <div v-if="selectedRoom" class="room-detail">
        <div class="room-info-card">
          <div class="info-row">
            <span class="info-label">房间号</span>
            <span class="info-value">{{ selectedRoom.roomNo }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">楼层</span>
            <span class="info-value">{{ selectedRoom.floor }}F</span>
          </div>
          <div class="info-row">
            <span class="info-label">入住人数</span>
            <span class="info-value">{{ selectedRoom.studentCount }} 人</span>
          </div>
        </div>

        <div class="students-section">
          <h4 class="section-title">
            <Users :size="16" />
            入住学生
          </h4>
          <el-table
            :data="selectedRoom.students"
            stripe
            size="small"
            class="students-table"
            empty-text="暂无学生"
          >
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="bedNo" label="床位号" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ row.bedNo || '-' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 编辑模式下显示管理按钮 -->
        <div v-if="editMode && canEdit" class="drawer-actions">
          <el-button type="primary" @click="openBedAssignmentForRoom">
            <UserPlus :size="16" class="mr-1" />
            管理床位分配
          </el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 学生床位分配对话框 -->
    <StudentBedAssignmentDialog
      v-model:visible="bedAssignmentVisible"
      :dormitory="selectedDormitoryForAssignment"
      :class-id="props.classId"
      @success="handleBedAssignmentSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Building, Users, LayoutGrid, List, UserPlus, Edit } from 'lucide-vue-next'
import { getClassDormitoryDistribution } from '@/api/myClass'
import { getDormitory } from '@/api/dormitory'
import type { DormitoryDistribution, DormitoryRoom, DormitoryStudent } from '@/types/myClass'
import StudentBedAssignmentDialog from '@/components/dormitory/StudentBedAssignmentDialog.vue'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  classId: string | number
}>()

const authStore = useAuthStore()

// 状态
const loading = ref(false)
const distribution = ref<DormitoryDistribution[]>([])
const viewMode = ref<'visual' | 'list'>('visual')
const editMode = ref(false)
const drawerVisible = ref(false)
const selectedRoom = ref<DormitoryRoom | null>(null)
const selectedBuildingName = ref('')
const bedAssignmentVisible = ref(false)
const selectedDormitoryForAssignment = ref<any>(null)

// 权限检查
const canEdit = computed(() => {
  return authStore.hasPermission('dormitory:student:assign') ||
         authStore.hasPermission('student:dormitory:edit')
})

// 计算属性
const buildingCount = computed(() => distribution.value.length)

const roomCount = computed(() => {
  return distribution.value.reduce((total, building) => total + building.rooms.length, 0)
})

const drawerTitle = computed(() => {
  if (!selectedRoom.value) return '房间详情'
  return `${selectedBuildingName.value} - ${selectedRoom.value.roomNo}`
})

// 扁平化房间列表（用于列表视图）
interface FlatRoom extends DormitoryRoom {
  buildingId: number
  buildingName: string
  buildingType: 'MALE' | 'FEMALE' | 'MIXED'
}

const flatRoomList = computed<FlatRoom[]>(() => {
  const list: FlatRoom[] = []
  distribution.value.forEach(building => {
    building.rooms.forEach(room => {
      list.push({
        ...room,
        buildingId: building.buildingId,
        buildingName: building.buildingName,
        buildingType: building.buildingType
      })
    })
  })
  return list
})

// 获取楼栋类型标签样式
const getBuildingTypeTag = (type: string): '' | 'success' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    MALE: '',
    FEMALE: 'danger',
    MIXED: 'warning'
  }
  return typeMap[type] || 'info'
}

// 获取楼栋类型显示文字
const getBuildingTypeLabel = (type: string): string => {
  const labelMap: Record<string, string> = {
    MALE: '男生宿舍',
    FEMALE: '女生宿舍',
    MIXED: '混合宿舍'
  }
  return labelMap[type] || '未知'
}

// 获取楼栋图标样式类
const getBuildingTypeClass = (type: string): string => {
  const classMap: Record<string, string> = {
    MALE: 'type-male',
    FEMALE: 'type-female',
    MIXED: 'type-mixed'
  }
  return classMap[type] || ''
}

// 打开房间详情抽屉
const openRoomDrawer = (room: DormitoryRoom, buildingName: string) => {
  selectedRoom.value = room
  selectedBuildingName.value = buildingName
  drawerVisible.value = true
}

// 从列表视图打开房间详情
const openRoomDrawerFromList = (row: FlatRoom) => {
  selectedRoom.value = {
    dormitoryId: row.dormitoryId,
    roomNo: row.roomNo,
    floor: row.floor,
    studentCount: row.studentCount,
    students: row.students
  }
  selectedBuildingName.value = row.buildingName
  drawerVisible.value = true
}

// 打开床位分配对话框
const openBedAssignmentDialog = async (room: DormitoryRoom | null) => {
  if (room && room.dormitoryId) {
    try {
      // 获取完整的宿舍详情（包括床位容量和性别类型）
      const dormitoryDetail = await getDormitory(room.dormitoryId)
      selectedDormitoryForAssignment.value = {
        id: room.dormitoryId,
        dormitoryNo: room.roomNo,
        buildingName: selectedBuildingName.value,
        floorNumber: room.floor,
        bedCapacity: dormitoryDetail.bedCapacity || 6,
        genderType: dormitoryDetail.genderType
      }
    } catch (error) {
      // 如果获取详情失败，使用基本信息
      selectedDormitoryForAssignment.value = {
        id: room.dormitoryId,
        dormitoryNo: room.roomNo,
        buildingName: selectedBuildingName.value,
        floorNumber: room.floor,
        bedCapacity: 6
      }
    }
  } else {
    // 打开空白分配（需要先选择宿舍）
    ElMessage.warning('请先选择一个宿舍房间')
    return
  }
  bedAssignmentVisible.value = true
}

// 从抽屉中打开床位分配
const openBedAssignmentForRoom = () => {
  if (selectedRoom.value) {
    openBedAssignmentDialog(selectedRoom.value)
    drawerVisible.value = false
  }
}

// 床位分配成功回调
const handleBedAssignmentSuccess = () => {
  loadData()
  ElMessage.success('床位分配成功')
}

// 加载数据
const loadData = async () => {
  console.log('[DormitoryTab] loadData called, classId:', props.classId, 'type:', typeof props.classId)
  if (!props.classId) {
    console.warn('[DormitoryTab] classId is empty, skipping load')
    return
  }

  loading.value = true
  try {
    console.log('[DormitoryTab] Calling getClassDormitoryDistribution with classId:', props.classId)
    distribution.value = await getClassDormitoryDistribution(props.classId)
    console.log('[DormitoryTab] Distribution data received:', distribution.value?.length, 'buildings')
    console.log('[DormitoryTab] Distribution data:', JSON.stringify(distribution.value, null, 2))
  } catch (error) {
    console.error('[DormitoryTab] Failed to load dormitory distribution:', error)
    ElMessage.error('加载宿舍分布数据失败')
    distribution.value = []
  } finally {
    loading.value = false
  }
}

// 监听 classId 变化
watch(() => props.classId, (newId) => {
  if (newId) {
    loadData()
  }
}, { immediate: false })

// 初始化加载
onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.dormitory-tab {
  padding: 0;
}

// 加载状态
.loading-container {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #ebeef5;
}

// 空状态
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background: white;
  border-radius: 12px;
  padding: 40px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #ebeef5;

  .empty-icon {
    color: #c0c4cc;
    margin-bottom: 16px;
  }

  h3 {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 8px;
  }

  p {
    font-size: 14px;
    color: #909399;
    margin: 0;
  }
}

// 统计摘要
.summary-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #ebeef5;

  @media (max-width: 576px) {
    flex-direction: column;
    gap: 12px;
  }
}

.summary-content {
  display: flex;
  align-items: center;
  gap: 8px;

  .summary-icon {
    color: #409eff;
  }

  .summary-text {
    font-size: 14px;
    color: #606266;

    strong {
      color: #409eff;
      font-weight: 600;
      margin: 0 2px;
    }
  }
}

.view-toggle {
  :deep(.el-radio-button__inner) {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

// 可视化视图
.visual-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.building-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #ebeef5;
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }
}

.building-header {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.building-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.building-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.type-male {
    background: #e8f4ff;
    color: #409eff;
  }

  &.type-female {
    background: #fef0f0;
    color: #f56c6c;
  }

  &.type-mixed {
    background: #fdf6ec;
    color: #e6a23c;
  }
}

.building-details {
  flex: 1;
}

.building-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 6px;
}

.building-meta {
  display: flex;
  align-items: center;
  gap: 12px;

  .student-count {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: #909399;
  }
}

// 房间网格
.rooms-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 12px;
}

.room-item {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid transparent;

  &:hover {
    background: #e8f4ff;
    border-color: #409eff;
    transform: translateY(-2px);

    .room-no {
      color: #409eff;
    }
  }

  .room-no {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 4px;
    transition: color 0.3s;
  }

  .room-count {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    font-size: 12px;
    color: #909399;
  }
}

// 列表视图
.list-view {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #ebeef5;
}

.room-table {
  margin-top: 0;
}

.building-cell {
  display: flex;
  align-items: center;
}

.student-names {
  color: #606266;
  font-size: 13px;
}

// 房间详情抽屉
.room-detail {
  padding: 0;
}

.room-info-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;

  &:not(:last-child) {
    border-bottom: 1px dashed #dcdfe6;
  }

  .info-label {
    font-size: 14px;
    color: #909399;
  }

  .info-value {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
  }
}

.students-section {
  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 12px;
  }
}

.students-table {
  :deep(.el-table__header) {
    th {
      background: #f5f7fa;
    }
  }
}

// 编辑模式相关样式
.edit-mode-switch {
  margin-right: 8px;
}

.edit-mode-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #409eff10, #67c23a10);
  border: 1px solid #409eff30;
  border-radius: 12px;
  padding: 12px 20px;
  margin-bottom: 16px;
  color: #409eff;
  font-size: 14px;
}

.drawer-actions {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: center;
}

// 编辑模式下房间项可点击样式增强
.room-item.edit-mode {
  border: 2px dashed #409eff;

  &:hover {
    background: #409eff10;
    border-style: solid;
  }
}

.mt-4 {
  margin-top: 16px;
}

.mr-1 {
  margin-right: 4px;
}

.flex {
  display: flex;
}

.items-center {
  align-items: center;
}

.gap-2 {
  gap: 8px;
}

.gap-3 {
  gap: 12px;
}
</style>
