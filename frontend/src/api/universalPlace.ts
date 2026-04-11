import request from '@/utils/request'
import type {
  UniversalPlace,
  PlaceTreeNode,
  CreatePlaceRequest,
  UpdatePlaceRequest,
  UniversalPlaceType,
  PlaceOccupant,
  PlaceOccupantWithPlace,
  CheckInRequest,
  PlaceBooking,
  CreateBookingRequest,
  BookingSeatAssignment,
  SaveSeatAssignmentRequest
} from '@/types/universalPlace'

const BASE_URL = '/v9/places'

/**
 * 空间统计数据
 */
export interface OrgPlaceStats {
  orgUnitId: number
  orgUnitName: string
  placeCount: number
  totalCapacity: number
  totalOccupancy: number
  occupancyRate: number
}

export interface PlaceStatistics {
  totalCount: number
  totalCapacity: number
  totalOccupancy: number
  countByType: Record<string, number>
  countByStatus: Record<number, number>
  occupancyRate: number
  statsByOrg?: OrgPlaceStats[]
}

/**
 * 通用空间管理 API
 */
/**
 * 将树结构扁平化为列表（递归遍历 children）
 */
function flattenTree(nodes: PlaceTreeNode[]): PlaceTreeNode[] {
  const result: PlaceTreeNode[] = []
  function walk(list: PlaceTreeNode[]) {
    for (const n of list) {
      result.push(n)
      if (n.children && n.children.length > 0) walk(n.children)
    }
  }
  walk(nodes)
  return result
}

export const universalPlaceApi = {
  /**
   * 获取空间树
   */
  getTree(): Promise<PlaceTreeNode[]> {
    return request.get(`${BASE_URL}/tree`)
  },

  /**
   * 获取所有空间的扁平列表（内部调用 /tree 并展开）
   */
  async getFlatList(): Promise<PlaceTreeNode[]> {
    const tree: PlaceTreeNode[] = await request.get(`${BASE_URL}/tree`)
    return flattenTree(tree)
  },

  /**
   * 获取指定类型的空间树
   */
  getTreeByType(typeCode: string): Promise<PlaceTreeNode[]> {
    return request.get(`${BASE_URL}/tree/type/${typeCode}`)
  },

  /**
   * 获取空间详情
   */
  getById(id: number | string): Promise<UniversalPlace> {
    return request.get(`${BASE_URL}/${id}`)
  },

  /**
   * 根据编码获取空间
   */
  getByCode(placeCode: string): Promise<UniversalPlace> {
    return request.get(`${BASE_URL}/code/${placeCode}`)
  },

  /**
   * 获取子空间列表
   */
  getChildren(parentId: number | string): Promise<UniversalPlace[]> {
    return request.get(`${BASE_URL}/${parentId}/children`)
  },

  /**
   * 获取根空间列表
   */
  getRootChildren(): Promise<UniversalPlace[]> {
    return request.get(`${BASE_URL}/roots/children`)
  },

  /**
   * 获取允许创建的子类型（根空间）
   */
  getAllowedChildTypesForRoot(): Promise<UniversalPlaceType[]> {
    return request.get(`${BASE_URL}/allowed-child-types`)
  },

  /**
   * 获取允许创建的子类型
   */
  getAllowedChildTypes(parentId: number | string): Promise<UniversalPlaceType[]> {
    return request.get(`${BASE_URL}/${parentId}/allowed-child-types`)
  },

  /**
   * 获取统计数据
   */
  getStatistics(): Promise<PlaceStatistics> {
    return request.get(`${BASE_URL}/statistics`)
  },

  /**
   * 获取场所有效性别
   */
  getEffectiveGender(id: number | string): Promise<string> {
    return request.get(`${BASE_URL}/${id}/effective-gender`)
  },

  /**
   * 创建空间
   */
  create(data: CreatePlaceRequest): Promise<UniversalPlace> {
    return request.post(BASE_URL, data)
  },

  /**
   * 更新空间
   */
  update(id: number | string, data: UpdatePlaceRequest): Promise<UniversalPlace> {
    return request.put(`${BASE_URL}/${id}`, data)
  },

  /**
   * 更改空间状态
   */
  changeStatus(id: number | string, status: number): Promise<UniversalPlace> {
    return request.put(`${BASE_URL}/${id}/status`, { status })
  },

  /**
   * 删除空间
   */
  delete(id: number | string): Promise<void> {
    return request.delete(`${BASE_URL}/${id}`)
  },

  // ==================== 入住管理 ====================

  /**
   * 获取当前入住列表
   */
  getOccupants(placeId: number | string): Promise<PlaceOccupant[]> {
    return request.get(`${BASE_URL}/${placeId}/occupants`)
  },

  /**
   * 获取入住历史
   */
  getOccupantHistory(placeId: number | string): Promise<PlaceOccupant[]> {
    return request.get(`${BASE_URL}/${placeId}/occupant-history`)
  },

  /**
   * 入住
   */
  checkIn(placeId: number | string, data: CheckInRequest): Promise<PlaceOccupant> {
    return request.post(`${BASE_URL}/${placeId}/check-in`, data)
  },

  /**
   * 批量入住
   */
  batchCheckIn(placeId: number | string, data: CheckInRequest[]): Promise<PlaceOccupant[]> {
    return request.post(`${BASE_URL}/${placeId}/batch-check-in`, data)
  },

  /**
   * 退出
   */
  checkOut(placeId: number | string, recordId: number | string): Promise<void> {
    return request.post(`${BASE_URL}/${placeId}/check-out/${recordId}`)
  },

  /**
   * 交换位置
   */
  swapPositions(placeId: number | string, recordId1: number | string, recordId2: number | string): Promise<void> {
    return request.post(`${BASE_URL}/${placeId}/swap-positions`, { recordId1, recordId2 })
  },

  /**
   * 查询指定场所列表中的所有活跃占用记录（带场所信息）
   */
  getOccupantsForPlaces(placeIds?: (number | string)[], occupantType?: string): Promise<PlaceOccupantWithPlace[]> {
    return request.get(`${BASE_URL}/batch-occupants`, { params: { placeIds, occupantType } })
  },

  /**
   * 查询占用者的所有占用历史（带场所信息）
   */
  getOccupantHistoryByOccupant(occupantType: string, occupantId: number | string): Promise<PlaceOccupantWithPlace[]> {
    return request.get(`${BASE_URL}/occupant-history`, { params: { occupantType, occupantId } })
  },

  // ==================== 预订管理 ====================

  createBooking(placeId: number | string, data: CreateBookingRequest): Promise<PlaceBooking> {
    return request.post('/place-bookings', { placeId, ...data })
  },

  getPlaceBookings(placeId: number | string, activeOnly?: boolean): Promise<PlaceBooking[]> {
    return request.get('/place-bookings', { params: { placeId, activeOnly } })
  },

  getMyBookings(): Promise<PlaceBooking[]> {
    return request.get('/place-bookings/my')
  },

  cancelBooking(id: number | string): Promise<void> {
    return request.put(`/place-bookings/${id}/cancel`)
  },

  // ==================== 排座管理 ====================

  getBookingSeating(bookingId: number | string): Promise<BookingSeatAssignment[]> {
    return request.get(`/place-bookings/${bookingId}/seating`)
  },

  saveBookingSeating(bookingId: number | string, data: SaveSeatAssignmentRequest[]): Promise<BookingSeatAssignment[]> {
    return request.put(`/place-bookings/${bookingId}/seating`, data)
  },

  clearBookingSeating(bookingId: number | string): Promise<void> {
    return request.delete(`/place-bookings/${bookingId}/seating`)
  }
}

export default universalPlaceApi
