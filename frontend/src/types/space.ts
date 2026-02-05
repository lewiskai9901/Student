/**
 * 统一场所管理类型定义
 */

// ========== 枚举类型 ==========

/** 场所类型 */
export type SpaceType = 'CAMPUS' | 'BUILDING' | 'FLOOR' | 'ROOM'

/** 房间类型 */
export type RoomType =
  | 'DORMITORY'      // 宿舍
  | 'CLASSROOM'      // 普通教室
  | 'LAB'            // 实验室
  | 'COMPUTER_ROOM'  // 机房
  | 'TRAINING'       // 实训室
  | 'MEETING'        // 会议室
  | 'OFFICE'         // 办公室
  | 'STORAGE'        // 仓库
  | 'OTHER'          // 其他

/** 楼栋类型 */
export type BuildingType = 'TEACHING' | 'DORMITORY' | 'OFFICE' | 'MIXED'

/** 场所状态 */
export type SpaceStatus = 0 | 1 | 2  // 0-禁用 1-正常 2-维护中

/** 性别类型 */
export type GenderType = 0 | 1 | 2  // 0-不限 1-男 2-女

/** 性别类型选项 */
export const GenderTypeOptions = [
  { value: 0, label: '不限' },
  { value: 1, label: '男' },
  { value: 2, label: '女' }
] as const

/** 占用者类型 */
export type OccupantType = 'STUDENT' | 'TEACHER' | 'STAFF'

// ========== 枚举选项 ==========

export interface EnumOption {
  code: string
  name: string
  [key: string]: any
}

export interface SpaceTypeOption extends EnumOption {
  level: number
}

export interface RoomTypeOption extends EnumOption {
  hasOccupancy: boolean
  hasGender: boolean
}

// ========== DTO类型 ==========

/** 场所DTO */
export interface SpaceDTO {
  id: number
  spaceCode: string
  spaceName: string
  spaceType: SpaceType
  categoryId?: number           // V10: 分类ID
  categoryName?: string         // V10: 分类名称
  roomType?: RoomType           // 兼容旧字段
  buildingType?: BuildingType   // 兼容旧字段

  // 楼号和房间号（V10: 改为数字类型）
  buildingNo?: number           // 楼号（数字）- BUILDING类型
  roomNo?: number               // 房间号（数字）- ROOM类型
  floorCount?: number           // 楼层数 - BUILDING类型
  parentBuildingNo?: number     // 所属楼栋的楼号（房间查询时用）

  // 层级
  parentId?: number
  parentName?: string
  path?: string
  level: number

  // 位置
  campusId?: number
  campusName?: string
  buildingId?: number
  buildingName?: string
  floorId?: number
  floorName?: string
  floorNumber?: number

  // 容量
  capacity?: number
  currentOccupancy?: number
  availableCapacity?: number
  occupancyRate?: number

  // 归属
  orgUnitId?: number
  orgUnitName?: string
  responsibleUserId?: number
  responsibleUserName?: string

  // 班级归属
  classId?: number
  className?: string
  classTeacherId?: number
  classTeacherName?: string
  classTeacherPhone?: string

  // 性别限制
  genderType?: GenderType
  genderTypeText?: string

  // 状态
  status: SpaceStatus
  statusText: string

  // 扩展
  attributes?: Record<string, any>
  description?: string

  // 子节点（树形结构用）
  children?: SpaceDTO[]

  // 扩展属性
  dormitoryExt?: DormitoryExtDTO
  classroomExt?: ClassroomExtDTO
  labExt?: LabExtDTO
  officeExt?: OfficeExtDTO

  // 审计
  createdAt?: string
  updatedAt?: string
}

/** 宿舍扩展DTO */
export interface DormitoryExtDTO {
  genderType?: GenderType
  genderTypeText?: string
  bedCount?: number
  facilities?: string
  assignedClassIds?: string
  assignedClassNames?: string
  supervisorId?: number
  supervisorName?: string
}

/** 教室扩展DTO */
export interface ClassroomExtDTO {
  classroomCategory?: string
  assignedClassId?: number
  assignedClassName?: string
  hasProjector?: boolean
  hasAirConditioner?: boolean
  hasComputer?: boolean
  equipmentInfo?: string
}

/** 实验室扩展DTO */
export interface LabExtDTO {
  labCategory?: string
  safetyLevel?: number
  majorId?: number
  majorName?: string
  equipmentList?: string
  safetyNotice?: string
}

/** 办公室扩展DTO */
export interface OfficeExtDTO {
  officeType?: string
  departmentId?: number
  departmentName?: string
  workstationCount?: number
  phoneNumber?: string
}

/** 场所占用者DTO */
export interface SpaceOccupantDTO {
  id: number
  spaceId: number
  spaceName?: string
  occupantType: OccupantType
  occupantId: number
  occupantName?: string
  occupantNo?: string
  positionNo?: number
  checkInDate?: string
  checkOutDate?: string
  status: number
  statusText: string
  remark?: string
  createdAt?: string

  // 额外信息
  className?: string
  departmentName?: string
  gender?: number
  phone?: string
}

/** 场所统计DTO */
export interface SpaceStatisticsDTO {
  totalBuildings: number
  totalRooms: number
  totalCapacity: number
  totalOccupancy: number
  occupancyRate: number

  byRoomType?: RoomTypeStats[]
  byBuildingType?: BuildingTypeStats[]
}

export interface RoomTypeStats {
  roomType: string
  roomTypeName: string
  count: number
  capacity: number
  occupancy: number
  occupancyRate: number
}

export interface BuildingTypeStats {
  buildingType: string
  buildingTypeName: string
  count: number
  roomCount: number
}

// ========== 请求类型 ==========

/** 创建场所请求 */
export interface CreateSpaceRequest {
  spaceType: SpaceType
  spaceCode?: string
  spaceName: string
  categoryId?: number         // V10: 分类ID
  roomType?: RoomType         // 兼容旧字段
  buildingType?: BuildingType // 兼容旧字段
  buildingNo?: number         // V10: 楼号（数字，仅BUILDING类型）
  roomNo?: number             // V10: 房间号（数字，仅ROOM类型）
  floorCount?: number         // V10: 楼层数（仅BUILDING类型）
  parentId?: number
  floorNumber?: number
  capacity?: number
  orgUnitId?: number
  classId?: number
  responsibleUserId?: number
  genderType?: GenderType
  description?: string
  attributes?: Record<string, any>
}

/** 更新场所请求 */
export interface UpdateSpaceRequest {
  spaceName?: string
  description?: string
  categoryId?: number         // V10: 分类ID
  buildingNo?: number         // V10: 楼号（数字，仅BUILDING类型）
  roomNo?: number             // V10: 房间号（数字，仅ROOM类型）
  floorCount?: number         // V10: 楼层数（仅BUILDING类型）
  capacity?: number
  orgUnitId?: number
  classId?: number
  responsibleUserId?: number
  genderType?: GenderType
  attributes?: Record<string, any>
}

/** 入住请求 */
export interface CheckInRequest {
  occupantType: OccupantType
  occupantId: number
  positionNo?: number
  remark?: string
}

/** 批量分配组织单元请求 */
export interface BatchAssignOrgUnitRequest {
  spaceIds: number[]
  orgUnitId: number  // 0 表示取消分配
}

/** 场所查询条件 */
export interface SpaceQueryParams {
  spaceType?: SpaceType
  roomType?: RoomType
  buildingType?: BuildingType
  buildingId?: number
  floorNumber?: number
  orgUnitId?: number
  status?: SpaceStatus
  keyword?: string
  page?: number
  pageSize?: number
}

// ========== 分页响应 ==========

export interface SpacePageResult {
  list: SpaceDTO[]
  total: number
  page: number
  pageSize: number
}

// ========== 类型配置 ==========

export interface SpaceTypeConfig {
  id: number
  typeCode: string
  typeName: string
  parentType: string
  hasCapacity: boolean
  hasOccupancy: boolean
  hasGender: boolean
  icon?: string
  color?: string
  sortOrder: number
  enabled: boolean
  createdAt?: string
}

// ========== 辅助函数 ==========

/** 获取场所类型显示名称 */
export function getSpaceTypeName(type: SpaceType): string {
  const names: Record<SpaceType, string> = {
    CAMPUS: '校区',
    BUILDING: '楼栋',
    FLOOR: '楼层',
    ROOM: '房间'
  }
  return names[type] || type
}

/** 获取房间类型显示名称 */
export function getRoomTypeName(type: RoomType): string {
  const names: Record<RoomType, string> = {
    DORMITORY: '宿舍',
    CLASSROOM: '教室',
    LAB: '实验室',
    COMPUTER_ROOM: '机房',
    TRAINING: '实训室',
    MEETING: '会议室',
    OFFICE: '办公室',
    STORAGE: '仓库',
    OTHER: '其他'
  }
  return names[type] || type
}

/** 获取楼栋类型显示名称 */
export function getBuildingTypeName(type: BuildingType): string {
  const names: Record<BuildingType, string> = {
    TEACHING: '教学楼',
    DORMITORY: '宿舍楼',
    OFFICE: '办公楼',
    MIXED: '综合楼'
  }
  return names[type] || type
}

/** 获取状态显示名称 */
export function getStatusName(status: SpaceStatus): string {
  const names: Record<SpaceStatus, string> = {
    0: '已禁用',
    1: '正常',
    2: '维护中'
  }
  return names[status] || '未知'
}

/** 获取状态样式类 */
export function getStatusClass(status: SpaceStatus): string {
  const classes: Record<SpaceStatus, string> = {
    0: 'bg-gray-100 text-gray-600',
    1: 'bg-green-100 text-green-700',
    2: 'bg-amber-100 text-amber-700'
  }
  return classes[status] || ''
}

/** 获取性别类型显示名称 */
export function getGenderTypeName(type?: GenderType): string {
  if (type === undefined || type === null) return '不限'
  const names: Record<GenderType, string> = {
    0: '不限',
    1: '男',
    2: '女'
  }
  return names[type] || '不限'
}

/** 获取占用者类型显示名称 */
export function getOccupantTypeName(type: OccupantType): string {
  const names: Record<OccupantType, string> = {
    STUDENT: '学生',
    TEACHER: '教师',
    STAFF: '职工'
  }
  return names[type] || type
}

/** 计算入住率颜色 */
export function getOccupancyRateColor(rate: number): string {
  if (rate >= 90) return 'text-red-600'
  if (rate >= 70) return 'text-amber-600'
  if (rate >= 50) return 'text-blue-600'
  return 'text-green-600'
}

// ========== 空间类型配置（用于UI显示） ==========

/** 房间类型配置 */
export interface RoomTypeConfig {
  label: string
  icon: string
  color: string
  bgColor: string
  textColor: string
}

/** 房间类型配置映射 */
export const ROOM_TYPE_CONFIG: Record<RoomType, RoomTypeConfig> = {
  DORMITORY: {
    label: '宿舍',
    icon: 'Bed',
    color: 'teal',
    bgColor: 'bg-teal-100',
    textColor: 'text-teal-700'
  },
  CLASSROOM: {
    label: '教室',
    icon: 'School',
    color: 'blue',
    bgColor: 'bg-blue-100',
    textColor: 'text-blue-700'
  },
  LAB: {
    label: '实验室',
    icon: 'FlaskConical',
    color: 'purple',
    bgColor: 'bg-purple-100',
    textColor: 'text-purple-700'
  },
  COMPUTER_ROOM: {
    label: '机房',
    icon: 'Monitor',
    color: 'cyan',
    bgColor: 'bg-cyan-100',
    textColor: 'text-cyan-700'
  },
  TRAINING: {
    label: '实训室',
    icon: 'Wrench',
    color: 'orange',
    bgColor: 'bg-orange-100',
    textColor: 'text-orange-700'
  },
  MEETING: {
    label: '会议室',
    icon: 'Users',
    color: 'indigo',
    bgColor: 'bg-indigo-100',
    textColor: 'text-indigo-700'
  },
  OFFICE: {
    label: '办公室',
    icon: 'Briefcase',
    color: 'slate',
    bgColor: 'bg-slate-100',
    textColor: 'text-slate-700'
  },
  STORAGE: {
    label: '仓库',
    icon: 'Package',
    color: 'amber',
    bgColor: 'bg-amber-100',
    textColor: 'text-amber-700'
  },
  OTHER: {
    label: '其他',
    icon: 'MoreHorizontal',
    color: 'gray',
    bgColor: 'bg-gray-100',
    textColor: 'text-gray-700'
  }
}

/** 获取房间类型配置 */
export function getRoomTypeConfig(type: RoomType): RoomTypeConfig {
  return ROOM_TYPE_CONFIG[type] || ROOM_TYPE_CONFIG.OTHER
}

// ========== 分配规则配置 ==========

/** 房间类型分配规则 */
export interface RoomTypeAllocationRule {
  /** 是否需要分配给部门 */
  needsOrgUnit: boolean
  /** 是否需要分配给班级 */
  needsClass: boolean
  /** 是否支持入住（需要管理占用者） */
  supportsOccupancy: boolean
  /** 分配层级描述 */
  allocationDesc: string
}

/** 房间类型分配规则映射 */
export const ROOM_TYPE_ALLOCATION_RULES: Record<RoomType, RoomTypeAllocationRule> = {
  DORMITORY: {
    needsOrgUnit: true,
    needsClass: true,
    supportsOccupancy: true,
    allocationDesc: '部门 → 班级 → 学生入住'
  },
  CLASSROOM: {
    needsOrgUnit: true,
    needsClass: true,
    supportsOccupancy: false,
    allocationDesc: '部门 → 班级（固定教室）'
  },
  LAB: {
    needsOrgUnit: true,
    needsClass: false,
    supportsOccupancy: false,
    allocationDesc: '部门管理'
  },
  COMPUTER_ROOM: {
    needsOrgUnit: true,
    needsClass: false,
    supportsOccupancy: false,
    allocationDesc: '部门管理'
  },
  TRAINING: {
    needsOrgUnit: true,
    needsClass: false,
    supportsOccupancy: false,
    allocationDesc: '部门管理'
  },
  MEETING: {
    needsOrgUnit: true,
    needsClass: false,
    supportsOccupancy: false,
    allocationDesc: '部门管理'
  },
  OFFICE: {
    needsOrgUnit: true,
    needsClass: false,
    supportsOccupancy: true,
    allocationDesc: '部门 → 人员入驻'
  },
  STORAGE: {
    needsOrgUnit: true,
    needsClass: false,
    supportsOccupancy: false,
    allocationDesc: '部门管理'
  },
  OTHER: {
    needsOrgUnit: true,
    needsClass: false,
    supportsOccupancy: false,
    allocationDesc: '部门管理'
  }
}

/** 获取房间类型分配规则 */
export function getRoomTypeAllocationRule(type: RoomType): RoomTypeAllocationRule {
  return ROOM_TYPE_ALLOCATION_RULES[type] || ROOM_TYPE_ALLOCATION_RULES.OTHER
}
