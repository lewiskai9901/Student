import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  OrgUnit,
  OrgUnitType,
  OrgUnitTreeNode,
  SchoolClass,
  ClassStatus,
  TeacherAssignment
} from '@/types/organization'
import {
  orgUnitApi,
  schoolClassApi
} from '@/api/organization'

/**
 * 组织管理 Store (V2)
 * 基于DDD架构的组织管理状态管理
 */
export const useOrganizationStore = defineStore('organization-v2', () => {
  // ==================== 组织单元状态 ====================
  const orgUnits = ref<OrgUnit[]>([])
  const orgUnitTree = ref<OrgUnitTreeNode[]>([])
  const currentOrgUnit = ref<OrgUnit | null>(null)
  const orgUnitsLoading = ref(false)

  // ==================== 班级状态 ====================
  const classes = ref<SchoolClass[]>([])
  const currentClass = ref<SchoolClass | null>(null)
  const classesLoading = ref(false)
  const classesTotal = ref(0)

  // ==================== 计算属性 ====================

  // 按类型过滤组织单元
  const orgUnitsByType = computed(() => {
    return (type: OrgUnitType) => orgUnits.value.filter(u => u.unitType === type)
  })

  // 获取所有系部
  const departments = computed(() => orgUnitsByType.value('DEPARTMENT'))

  // 获取所有学院
  const colleges = computed(() => orgUnitsByType.value('COLLEGE'))

  // 活跃班级
  const activeClasses = computed(() =>
    classes.value.filter(c => c.status === 'ACTIVE')
  )

  // ==================== 组织单元操作 ====================

  /**
   * 加载组织树
   */
  const loadOrgUnitTree = async () => {
    orgUnitsLoading.value = true
    try {
      const data = await orgUnitApi.getTree()
      // request.ts 已解包，data 直接是响应数据
      orgUnitTree.value = Array.isArray(data) ? data : []
    } catch (error) {
      console.error('加载组织树失败:', error)
    } finally {
      orgUnitsLoading.value = false
    }
  }

  /**
   * 加载所有组织单元
   */
  const loadOrgUnits = async () => {
    orgUnitsLoading.value = true
    try {
      const data = await orgUnitApi.getList()
      // request.ts 已解包，data 直接是响应数据
      orgUnits.value = Array.isArray(data) ? data : []
    } catch (error) {
      console.error('加载组织单元列表失败:', error)
    } finally {
      orgUnitsLoading.value = false
    }
  }

  /**
   * 按类型加载组织单元
   */
  const loadOrgUnitsByType = async (type: OrgUnitType) => {
    orgUnitsLoading.value = true
    try {
      const data = await orgUnitApi.getByType(type)
      // request.ts 已解包，data 直接是响应数据
      const newUnits = Array.isArray(data) ? data : []
      // 更新对应类型的数据
      orgUnits.value = [
        ...orgUnits.value.filter(u => u.unitType !== type),
        ...newUnits
      ]
      return newUnits
    } catch (error) {
      console.error('按类型加载组织单元失败:', error)
      return []
    } finally {
      orgUnitsLoading.value = false
    }
  }

  /**
   * 获取组织单元详情
   */
  const getOrgUnit = async (id: number) => {
    try {
      const data = await orgUnitApi.getById(id)
      currentOrgUnit.value = data || null
      return data
    } catch (error) {
      console.error('获取组织单元详情失败:', error)
      return null
    }
  }

  /**
   * 获取子组织单元
   */
  const getChildren = async (parentId: number) => {
    try {
      const data = await orgUnitApi.getChildren(parentId)
      return Array.isArray(data) ? data : []
    } catch (error) {
      console.error('获取子组织单元失败:', error)
      return []
    }
  }

  /**
   * 创建组织单元
   */
  const createOrgUnit = async (data: Parameters<typeof orgUnitApi.create>[0]) => {
    const result = await orgUnitApi.create(data)
    await loadOrgUnitTree() // 刷新树
    return result
  }

  /**
   * 更新组织单元
   */
  const updateOrgUnit = async (id: number, data: Parameters<typeof orgUnitApi.update>[1]) => {
    const result = await orgUnitApi.update(id, data)
    await loadOrgUnitTree() // 刷新树
    return result
  }

  /**
   * 删除组织单元
   */
  const deleteOrgUnit = async (id: number) => {
    const result = await orgUnitApi.delete(id)
    await loadOrgUnitTree() // 刷新树
    return result
  }

  /**
   * 启用组织单元
   */
  const enableOrgUnit = async (id: number) => {
    const result = await orgUnitApi.enable(id)
    await loadOrgUnitTree()
    return result
  }

  /**
   * 禁用组织单元
   */
  const disableOrgUnit = async (id: number) => {
    const result = await orgUnitApi.disable(id)
    await loadOrgUnitTree()
    return result
  }

  // ==================== 班级操作 ====================

  /**
   * 加载班级列表
   */
  const loadClasses = async (params?: Parameters<typeof schoolClassApi.getList>[0]) => {
    classesLoading.value = true
    try {
      const data = await schoolClassApi.getList(params)
      // request.ts 已解包，data 直接是响应数据
      // 兼容两种响应格式：直接数组 或 分页对象 {items/records, total}
      if (Array.isArray(data)) {
        classes.value = data
        classesTotal.value = data.length
      } else if (data) {
        classes.value = data.items || data.records || []
        classesTotal.value = data.total || 0
      }
    } catch (error) {
      console.error('加载班级列表失败:', error)
    } finally {
      classesLoading.value = false
    }
  }

  /**
   * 按组织单元加载班级
   */
  const loadClassesByOrgUnit = async (orgUnitId: number) => {
    classesLoading.value = true
    try {
      const data = await schoolClassApi.getByOrgUnit(orgUnitId)
      // request.ts 已解包，data 直接是响应数据
      classes.value = Array.isArray(data) ? data : []
    } catch (error) {
      console.error('按组织单元加载班级失败:', error)
    } finally {
      classesLoading.value = false
    }
  }

  /**
   * 获取班级详情
   */
  const getClass = async (id: number) => {
    try {
      const data = await schoolClassApi.getById(id)
      currentClass.value = data || null
      return data
    } catch (error) {
      console.error('获取班级详情失败:', error)
      return null
    }
  }

  /**
   * 创建班级
   */
  const createClass = async (data: Parameters<typeof schoolClassApi.create>[0]) => {
    const result = await schoolClassApi.create(data)
    if (currentOrgUnit.value) {
      await loadClassesByOrgUnit(currentOrgUnit.value.id)
    }
    return result
  }

  /**
   * 更新班级
   */
  const updateClass = async (id: number, data: Parameters<typeof schoolClassApi.update>[1]) => {
    const result = await schoolClassApi.update(id, data)
    // 刷新当前班级
    await getClass(id)
    return result
  }

  /**
   * 激活班级
   */
  const activateClass = async (id: number) => {
    const result = await schoolClassApi.activate(id)
    await getClass(id)
    return result
  }

  /**
   * 毕业班级
   */
  const graduateClass = async (id: number) => {
    const result = await schoolClassApi.graduate(id)
    await getClass(id)
    return result
  }

  /**
   * 撤销班级
   */
  const dissolveClass = async (id: number) => {
    const result = await schoolClassApi.dissolve(id)
    await getClass(id)
    return result
  }

  /**
   * 分配班主任
   */
  const assignHeadTeacher = async (
    classId: number,
    teacherId: number,
    teacherName: string
  ) => {
    const result = await schoolClassApi.assignHeadTeacher(classId, {
      teacherId,
      teacherName
    })
    await getClass(classId)
    return result
  }

  /**
   * 分配副班主任
   */
  const assignDeputyHeadTeacher = async (
    classId: number,
    teacherId: number,
    teacherName: string
  ) => {
    const result = await schoolClassApi.assignDeputyHeadTeacher(classId, {
      teacherId,
      teacherName
    })
    await getClass(classId)
    return result
  }

  /**
   * 删除班级
   */
  const deleteClass = async (id: number) => {
    const result = await schoolClassApi.delete(id)
    if (currentOrgUnit.value) {
      await loadClassesByOrgUnit(currentOrgUnit.value.id)
    }
    return result
  }

  // ==================== 重置状态 ====================
  const reset = () => {
    orgUnits.value = []
    orgUnitTree.value = []
    currentOrgUnit.value = null
    classes.value = []
    currentClass.value = null
    classesTotal.value = 0
  }

  return {
    // 组织单元状态
    orgUnits,
    orgUnitTree,
    currentOrgUnit,
    orgUnitsLoading,

    // 班级状态
    classes,
    currentClass,
    classesLoading,
    classesTotal,

    // 计算属性
    orgUnitsByType,
    departments,
    colleges,
    activeClasses,

    // 组织单元操作
    loadOrgUnits,
    loadOrgUnitTree,
    loadOrgUnitsByType,
    getOrgUnit,
    getChildren,
    createOrgUnit,
    updateOrgUnit,
    deleteOrgUnit,
    enableOrgUnit,
    disableOrgUnit,

    // 班级操作
    loadClasses,
    loadClassesByOrgUnit,
    getClass,
    createClass,
    updateClass,
    activateClass,
    graduateClass,
    dissolveClass,
    assignHeadTeacher,
    assignDeputyHeadTeacher,
    deleteClass,

    // 工具方法
    reset
  }
})
