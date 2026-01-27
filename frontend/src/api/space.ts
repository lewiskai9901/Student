/**
 * 统一场所管理API
 */
import request from '@/utils/request'
import type {
  SpaceDTO,
  SpaceOccupantDTO,
  SpacePageResult,
  SpaceTypeConfig,
  CreateSpaceRequest,
  UpdateSpaceRequest,
  CheckInRequest,
  BatchAssignOrgUnitRequest,
  SpaceQueryParams,
  SpaceType,
  RoomType,
  BuildingType,
  SpaceStatus,
  EnumOption,
  SpaceTypeOption,
  RoomTypeOption
} from '@/types/space'

const BASE_URL = '/spaces'

// ========== 基础CRUD ==========

/** 创建场所 */
export function createSpace(data: CreateSpaceRequest) {
  return request.post<number>(`${BASE_URL}`, data)
}

/** 更新场所 */
export function updateSpace(id: number, data: UpdateSpaceRequest) {
  return request.put<void>(`${BASE_URL}/${id}`, data)
}

/** 删除场所 */
export function deleteSpace(id: number, force = false) {
  return request.delete<void>(`${BASE_URL}/${id}`, { params: { force } })
}

/** 获取场所详情 */
export function getSpaceById(id: number) {
  return request.get<SpaceDTO>(`${BASE_URL}/${id}`)
}

// ========== 状态管理 ==========

/** 变更场所状态 */
export function changeSpaceStatus(id: number, status: SpaceStatus) {
  return request.put<void>(`${BASE_URL}/${id}/status`, null, { params: { status } })
}

/** 启用场所 */
export function enableSpace(id: number) {
  return request.put<void>(`${BASE_URL}/${id}/enable`)
}

/** 禁用场所 */
export function disableSpace(id: number) {
  return request.put<void>(`${BASE_URL}/${id}/disable`)
}

/** 设置维护状态 */
export function startMaintenance(id: number) {
  return request.put<void>(`${BASE_URL}/${id}/maintenance`)
}

// ========== 入住/退出 ==========

/** 入住 */
export function checkIn(spaceId: number, data: CheckInRequest) {
  return request.post<number>(`${BASE_URL}/${spaceId}/check-in`, data)
}

/** 退出 */
export function checkOut(spaceId: number, occupantRecordId: number) {
  return request.post<void>(`${BASE_URL}/${spaceId}/check-out/${occupantRecordId}`)
}

/** 获取场所占用者列表 */
export function getOccupants(spaceId: number) {
  return request.get<SpaceOccupantDTO[]>(`${BASE_URL}/${spaceId}/occupants`)
}

/** 获取场所占用历史 */
export function getOccupantHistory(spaceId: number) {
  return request.get<SpaceOccupantDTO[]>(`${BASE_URL}/${spaceId}/occupant-history`)
}

// ========== 批量操作 ==========

/** 批量分配组织单元 */
export function batchAssignOrgUnit(data: BatchAssignOrgUnitRequest) {
  return request.post<void>(`${BASE_URL}/batch/assign-org-unit`, data)
}

/** 批量分配班级 */
export function batchAssignClass(spaceIds: number[], classId: number) {
  return request.post<void>(`${BASE_URL}/batch/assign-class`, { spaceIds, classId })
}

// ========== 班级分配管理 ==========

/** 设置场所归属班级 */
export function assignSpaceToClass(spaceId: number, classId: number) {
  return request.put<void>(`${BASE_URL}/${spaceId}/class`, null, { params: { classId } })
}

/** 取消场所班级分配 */
export function unassignSpaceFromClass(spaceId: number) {
  return request.delete<void>(`${BASE_URL}/${spaceId}/class`)
}

/** 设置性别限制 */
export function setGenderType(spaceId: number, genderType: number) {
  return request.put<void>(`${BASE_URL}/${spaceId}/gender-type`, null, { params: { genderType } })
}

/** 获取性别类型枚举 */
export function getGenderTypes() {
  return request.get<EnumOption[]>(`${BASE_URL}/enums/gender-types`)
}

// ========== 树形查询 ==========

/** 获取场所树 */
export function getSpaceTree(buildingType?: BuildingType, includeStatistics = false) {
  return request.get<SpaceDTO[]>(`${BASE_URL}/tree`, {
    params: { buildingType, includeStatistics }
  })
}

/** 获取子节点 */
export function getChildren(parentId: number) {
  return request.get<SpaceDTO[]>(`${BASE_URL}/${parentId}/children`)
}

/** 获取祖先链 */
export function getAncestors(id: number) {
  return request.get<SpaceDTO[]>(`${BASE_URL}/${id}/ancestors`)
}

// ========== 列表查询 ==========

/** 获取楼宇列表 */
export function getBuildings(buildingType?: BuildingType, status?: SpaceStatus) {
  return request.get<SpaceDTO[]>(`${BASE_URL}/buildings`, {
    params: { buildingType, status }
  })
}

/** 分页查询场所 */
export function querySpaces(params: SpaceQueryParams) {
  return request.get<SpacePageResult>(`${BASE_URL}`, { params })
}

// ========== 枚举查询 ==========

/** 获取场所类型枚举 */
export function getSpaceTypes() {
  return request.get<SpaceTypeOption[]>(`${BASE_URL}/enums/space-types`)
}

/** 获取房间类型枚举 */
export function getRoomTypes() {
  return request.get<RoomTypeOption[]>(`${BASE_URL}/enums/room-types`)
}

/** 获取楼宇类型枚举 */
export function getBuildingTypes() {
  return request.get<EnumOption[]>(`${BASE_URL}/enums/building-types`)
}

/** 获取场所状态枚举 */
export function getStatuses() {
  return request.get<EnumOption[]>(`${BASE_URL}/enums/statuses`)
}

// ========== 场所类型配置 ==========

const CONFIG_URL = '/api/space-type-configs'

/** 获取所有场所类型配置 */
export function getAllTypeConfigs() {
  return request.get<SpaceTypeConfig[]>(CONFIG_URL)
}

/** 获取启用的场所类型配置 */
export function getEnabledTypeConfigs() {
  return request.get<SpaceTypeConfig[]>(`${CONFIG_URL}/enabled`)
}

/** 获取房间类型配置 */
export function getRoomTypeConfigs() {
  return request.get<SpaceTypeConfig[]>(`${CONFIG_URL}/room-types`)
}

/** 启用/禁用类型配置 */
export function toggleTypeConfig(id: number) {
  return request.put<void>(`${CONFIG_URL}/${id}/toggle`)
}

/** 更新类型配置排序 */
export function updateTypeConfigSort(id: number, sortOrder: number) {
  return request.put<void>(`${CONFIG_URL}/${id}/sort`, null, { params: { sortOrder } })
}

// ========== 便捷查询API ==========

/** 获取宿舍楼列表 */
export function getDormitoryBuildings(status?: SpaceStatus) {
  return getBuildings('DORMITORY', status)
}

/** 获取教学楼列表 */
export function getTeachingBuildings(status?: SpaceStatus) {
  return getBuildings('TEACHING', status)
}

/** 获取宿舍楼树（含楼层和房间） */
export function getDormitoryTree(includeStatistics = false) {
  return getSpaceTree('DORMITORY', includeStatistics)
}

/** 获取教学楼树（含楼层和房间） */
export function getTeachingTree(includeStatistics = false) {
  return getSpaceTree('TEACHING', includeStatistics)
}

/** 查询宿舍列表 */
export function queryDormitories(params: Omit<SpaceQueryParams, 'spaceType' | 'roomType'>) {
  return querySpaces({ ...params, spaceType: 'ROOM', roomType: 'DORMITORY' })
}

/** 查询教室列表 */
export function queryClassrooms(params: Omit<SpaceQueryParams, 'spaceType' | 'roomType'>) {
  return querySpaces({ ...params, spaceType: 'ROOM', roomType: 'CLASSROOM' })
}

/** 查询实验室列表 */
export function queryLabs(params: Omit<SpaceQueryParams, 'spaceType' | 'roomType'>) {
  return querySpaces({ ...params, spaceType: 'ROOM', roomType: 'LAB' })
}

/** 查询办公室列表 */
export function queryOffices(params: Omit<SpaceQueryParams, 'spaceType' | 'roomType'>) {
  return querySpaces({ ...params, spaceType: 'ROOM', roomType: 'OFFICE' })
}

// 默认导出
export default {
  // CRUD
  createSpace,
  updateSpace,
  deleteSpace,
  getSpaceById,
  // 状态
  changeSpaceStatus,
  enableSpace,
  disableSpace,
  startMaintenance,
  // 入住
  checkIn,
  checkOut,
  getOccupants,
  getOccupantHistory,
  // 批量
  batchAssignOrgUnit,
  batchAssignClass,
  // 班级分配
  assignSpaceToClass,
  unassignSpaceFromClass,
  setGenderType,
  // 树形
  getSpaceTree,
  getChildren,
  getAncestors,
  // 列表
  getBuildings,
  querySpaces,
  // 枚举
  getSpaceTypes,
  getRoomTypes,
  getBuildingTypes,
  getStatuses,
  getGenderTypes,
  // 配置
  getAllTypeConfigs,
  getEnabledTypeConfigs,
  getRoomTypeConfigs,
  toggleTypeConfig,
  updateTypeConfigSort,
  // 便捷
  getDormitoryBuildings,
  getTeachingBuildings,
  getDormitoryTree,
  getTeachingTree,
  queryDormitories,
  queryClassrooms,
  queryLabs,
  queryOffices
}
