import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  Dormitory,
  Building,
  BedAllocation,
  DormitoryStatus,
  GenderType,
  DormitoryQueryParams,
  CreateDormitoryRequest,
  UpdateDormitoryRequest,
  BatchCreateDormitoryRequest,
  AssignStudentRequest,
  SwapStudentsRequest,
  BuildingQueryParams,
  CreateBuildingRequest,
  UpdateBuildingRequest
} from '@/types/dormitory'
import { dormitoryApi, buildingApi } from '@/api/dormitory'

/**
 * 宿舍管理 Store (V2)
 * 基于DDD架构的宿舍管理状态管理
 */
export const useDormitoryStore = defineStore('dormitory-v2', () => {
  // ==================== 宿舍状态 ====================
  const dormitories = ref<Dormitory[]>([])
  const currentDormitory = ref<Dormitory | null>(null)
  const dormitoriesLoading = ref(false)
  const dormitoriesTotal = ref(0)

  // ==================== 楼宇状态 ====================
  const buildings = ref<Building[]>([])
  const currentBuilding = ref<Building | null>(null)
  const buildingsLoading = ref(false)
  const buildingsTotal = ref(0)

  // ==================== 床位状态 ====================
  const bedAllocations = ref<BedAllocation[]>([])
  const bedAllocationsLoading = ref(false)

  // ==================== 计算属性 ====================

  // 按状态过滤宿舍
  const dormitoriesByStatus = computed(() => {
    return (status: DormitoryStatus) => dormitories.value.filter(d => d.status === status)
  })

  // 正常状态的宿舍
  const normalDormitories = computed(() => dormitoriesByStatus.value(1))

  // 停用的宿舍
  const disabledDormitories = computed(() => dormitoriesByStatus.value(0))

  // 按性别过滤宿舍
  const dormitoriesByGender = computed(() => {
    return (genderType: GenderType) => dormitories.value.filter(d => d.genderType === genderType)
  })

  // 有空床位的宿舍
  const availableDormitories = computed(() =>
    dormitories.value.filter(d => d.status === 1 && d.occupiedBeds < d.bedCount)
  )

  // 统计信息
  const statistics = computed(() => {
    const totalBeds = dormitories.value.reduce((sum, d) => sum + (d.bedCount || 0), 0)
    const occupiedBeds = dormitories.value.reduce((sum, d) => sum + (d.occupiedBeds || 0), 0)
    return {
      totalRooms: dormitories.value.length,
      totalBeds,
      occupiedBeds,
      availableBeds: totalBeds - occupiedBeds,
      occupancyRate: totalBeds > 0 ? (occupiedBeds / totalBeds * 100).toFixed(1) : '0'
    }
  })

  // ==================== 宿舍操作 ====================

  /**
   * 加载宿舍列表
   */
  const loadDormitories = async (params?: DormitoryQueryParams) => {
    dormitoriesLoading.value = true
    try {
      const data = await dormitoryApi.getList(params)
      // request.ts 已解包，data 直接是响应数据
      if (data) {
        dormitories.value = data.records || []
        dormitoriesTotal.value = data.total || 0
      }
    } catch (error) {
      console.error('加载宿舍列表失败:', error)
    } finally {
      dormitoriesLoading.value = false
    }
  }

  /**
   * 获取宿舍详情
   */
  const getDormitory = async (id: number) => {
    try {
      const data = await dormitoryApi.getById(id)
      currentDormitory.value = data || null
      return data
    } catch (error) {
      console.error('获取宿舍详情失败:', error)
      return null
    }
  }

  /**
   * 根据楼宇获取宿舍列表
   */
  const getDormitoriesByBuilding = async (buildingId: number) => {
    try {
      const data = await dormitoryApi.getByBuilding(buildingId)
      return Array.isArray(data) ? data : []
    } catch (error) {
      console.error('根据楼宇获取宿舍失败:', error)
      return []
    }
  }

  /**
   * 获取有空床位的宿舍
   */
  const getAvailableDormitories = async (genderType?: number) => {
    try {
      const data = await dormitoryApi.getAvailable(genderType)
      return Array.isArray(data) ? data : []
    } catch (error) {
      console.error('获取有空床位的宿舍失败:', error)
      return []
    }
  }

  /**
   * 创建宿舍
   */
  const createDormitory = async (data: CreateDormitoryRequest) => {
    const result = await dormitoryApi.create(data)
    await loadDormitories()
    return result
  }

  /**
   * 更新宿舍
   */
  const updateDormitory = async (id: number, data: UpdateDormitoryRequest) => {
    const result = await dormitoryApi.update(id, data)
    await getDormitory(id)
    return result
  }

  /**
   * 删除宿舍
   */
  const deleteDormitory = async (id: number, force = false) => {
    const result = await dormitoryApi.delete(id, force)
    await loadDormitories()
    return result
  }

  /**
   * 批量删除宿舍
   */
  const deleteDormitories = async (ids: number[]) => {
    const result = await dormitoryApi.deleteBatch(ids)
    await loadDormitories()
    return result
  }

  /**
   * 批量创建宿舍
   */
  const batchCreateDormitories = async (data: BatchCreateDormitoryRequest) => {
    const result = await dormitoryApi.batchCreate(data)
    await loadDormitories()
    return result
  }

  /**
   * 检查宿舍编号是否存在
   */
  const existsDormitoryNo = async (buildingId: number, dormitoryNo: string, excludeId?: number) => {
    try {
      const data = await dormitoryApi.exists(buildingId, dormitoryNo, excludeId)
      return data || false
    } catch (error) {
      console.error('检查宿舍编号失败:', error)
      return false
    }
  }

  // ==================== 宿舍状态操作 ====================

  /**
   * 更新宿舍状态
   */
  const updateDormitoryStatus = async (id: number, status: DormitoryStatus) => {
    const result = await dormitoryApi.updateStatus(id, status)
    await getDormitory(id)
    return result
  }

  // ==================== 床位操作 ====================

  /**
   * 加载床位分配情况
   */
  const loadBedAllocations = async (dormitoryId: number) => {
    bedAllocationsLoading.value = true
    try {
      const data = await dormitoryApi.getBedAllocations(dormitoryId)
      bedAllocations.value = Array.isArray(data) ? data : []
      return bedAllocations.value
    } catch (error) {
      console.error('加载床位分配情况失败:', error)
      return []
    } finally {
      bedAllocationsLoading.value = false
    }
  }

  /**
   * 分配学生到宿舍
   */
  const assignStudent = async (data: AssignStudentRequest) => {
    const result = await dormitoryApi.assignStudent(data)
    if (currentDormitory.value) {
      await getDormitory(currentDormitory.value.id)
      await loadBedAllocations(currentDormitory.value.id)
    }
    return result
  }

  /**
   * 从宿舍移除学生
   */
  const removeStudent = async (studentId: number) => {
    const result = await dormitoryApi.removeStudent(studentId)
    if (currentDormitory.value) {
      await getDormitory(currentDormitory.value.id)
      await loadBedAllocations(currentDormitory.value.id)
    }
    return result
  }

  /**
   * 交换学生宿舍
   */
  const swapStudents = async (data: SwapStudentsRequest) => {
    const result = await dormitoryApi.swapStudents(data)
    await loadDormitories()
    return result
  }

  // ==================== 楼宇操作 ====================

  /**
   * 加载楼宇列表
   */
  const loadBuildings = async (params?: BuildingQueryParams) => {
    buildingsLoading.value = true
    try {
      const data = await buildingApi.getList(params)
      // request.ts 已解包，data 直接是响应数据
      if (data) {
        buildings.value = data.records || []
        buildingsTotal.value = data.total || 0
      }
    } catch (error) {
      console.error('加载楼宇列表失败:', error)
    } finally {
      buildingsLoading.value = false
    }
  }

  /**
   * 获取楼宇详情
   */
  const getBuilding = async (id: number) => {
    try {
      const data = await buildingApi.getById(id)
      currentBuilding.value = data || null
      return data
    } catch (error) {
      console.error('获取楼宇详情失败:', error)
      return null
    }
  }

  /**
   * 获取所有启用的楼宇
   */
  const getAllEnabledBuildings = async (buildingType?: number) => {
    try {
      const data = await buildingApi.getAllEnabled(buildingType)
      return Array.isArray(data) ? data : []
    } catch (error) {
      console.error('获取启用的楼宇失败:', error)
      return []
    }
  }

  /**
   * 创建楼宇
   */
  const createBuilding = async (data: CreateBuildingRequest) => {
    const result = await buildingApi.create(data)
    await loadBuildings()
    return result
  }

  /**
   * 更新楼宇
   */
  const updateBuilding = async (id: number, data: UpdateBuildingRequest) => {
    const result = await buildingApi.update(id, data)
    await getBuilding(id)
    return result
  }

  /**
   * 删除楼宇
   */
  const deleteBuilding = async (id: number) => {
    const result = await buildingApi.delete(id)
    await loadBuildings()
    return result
  }

  /**
   * 检查楼号是否存在
   */
  const existsBuildingNo = async (buildingNo: string, excludeId?: number) => {
    try {
      const data = await buildingApi.exists(buildingNo, excludeId)
      return data || false
    } catch (error) {
      console.error('检查楼号失败:', error)
      return false
    }
  }

  // ==================== 重置状态 ====================
  const reset = () => {
    dormitories.value = []
    currentDormitory.value = null
    dormitoriesTotal.value = 0
    buildings.value = []
    currentBuilding.value = null
    buildingsTotal.value = 0
    bedAllocations.value = []
  }

  return {
    // 宿舍状态
    dormitories,
    currentDormitory,
    dormitoriesLoading,
    dormitoriesTotal,

    // 楼宇状态
    buildings,
    currentBuilding,
    buildingsLoading,
    buildingsTotal,

    // 床位状态
    bedAllocations,
    bedAllocationsLoading,

    // 计算属性
    dormitoriesByStatus,
    normalDormitories,
    disabledDormitories,
    dormitoriesByGender,
    availableDormitories,
    statistics,

    // 宿舍操作
    loadDormitories,
    getDormitory,
    getDormitoriesByBuilding,
    getAvailableDormitories,
    createDormitory,
    updateDormitory,
    deleteDormitory,
    deleteDormitories,
    batchCreateDormitories,
    existsDormitoryNo,
    updateDormitoryStatus,

    // 床位操作
    loadBedAllocations,
    assignStudent,
    removeStudent,
    swapStudents,

    // 楼宇操作
    loadBuildings,
    getBuilding,
    getAllEnabledBuildings,
    createBuilding,
    updateBuilding,
    deleteBuilding,
    existsBuildingNo,

    // 工具方法
    reset
  }
})
