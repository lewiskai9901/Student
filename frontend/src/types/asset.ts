/**
 * 固定资产管理类型定义
 */

// ============ 枚举类型 ============

/** 资产状态 */
export enum AssetStatus {
  IN_USE = 1,     // 在用
  IDLE = 2,       // 闲置
  REPAIRING = 3,  // 维修中
  SCRAPPED = 4    // 已报废
}

export const AssetStatusMap: Record<AssetStatus, string> = {
  [AssetStatus.IN_USE]: '在用',
  [AssetStatus.IDLE]: '闲置',
  [AssetStatus.REPAIRING]: '维修中',
  [AssetStatus.SCRAPPED]: '已报废'
}

/** 位置类型 */
export enum LocationType {
  CLASSROOM = 'classroom',   // 教室
  DORMITORY = 'dormitory',   // 宿舍
  OFFICE = 'office',         // 办公室
  WAREHOUSE = 'warehouse',   // 仓库
  OTHER = 'other'            // 其他
}

export const LocationTypeMap: Record<LocationType, string> = {
  [LocationType.CLASSROOM]: '教室',
  [LocationType.DORMITORY]: '宿舍',
  [LocationType.OFFICE]: '办公室',
  [LocationType.WAREHOUSE]: '仓库',
  [LocationType.OTHER]: '其他'
}

/** 资产分类类型 */
export enum CategoryType {
  FIXED_ASSET = 1,   // 固定资产
  LOW_VALUE = 2,     // 低值易耗品
  CONSUMABLE = 3     // 消耗品
}

export const CategoryTypeMap: Record<CategoryType, string> = {
  [CategoryType.FIXED_ASSET]: '固定资产',
  [CategoryType.LOW_VALUE]: '低值易耗品',
  [CategoryType.CONSUMABLE]: '消耗品'
}

/** 管理模式 */
export enum ManagementMode {
  SINGLE_ITEM = 1,  // 单品管理
  BATCH = 2         // 批量管理
}

export const ManagementModeMap: Record<ManagementMode, string> = {
  [ManagementMode.SINGLE_ITEM]: '单品管理',
  [ManagementMode.BATCH]: '批量管理'
}

/** 维修类型 */
export enum MaintenanceType {
  REPAIR = 1,       // 维修
  MAINTENANCE = 2   // 保养
}

export const MaintenanceTypeMap: Record<MaintenanceType, string> = {
  [MaintenanceType.REPAIR]: '维修',
  [MaintenanceType.MAINTENANCE]: '保养'
}

/** 维修状态 */
export enum MaintenanceStatus {
  IN_PROGRESS = 1,  // 进行中
  COMPLETED = 2     // 已完成
}

export const MaintenanceStatusMap: Record<MaintenanceStatus, string> = {
  [MaintenanceStatus.IN_PROGRESS]: '进行中',
  [MaintenanceStatus.COMPLETED]: '已完成'
}

// ============ 数据模型 ============

/** 资产分类 */
export interface AssetCategory {
  id: number | string
  parentId: number | string | null
  categoryCode: string
  categoryName: string
  categoryType: CategoryType | null
  categoryTypeDesc: string | null
  defaultManagementMode: ManagementMode | null
  defaultManagementModeDesc: string | null
  supportsBatchManagement: boolean | null
  depreciationYears: number | null
  unit: string | null
  sortOrder: number
  remark: string | null
  assetCount: number | null
  children?: AssetCategory[]
  createdAt: string
  updatedAt: string
}

/** 资产 */
export interface Asset {
  id: number | string
  assetCode: string
  assetName: string
  categoryId: number | string
  categoryName: string | null
  categoryCode: string | null
  brand: string | null
  model: string | null
  unit: string
  quantity: number
  originalValue: number | null
  netValue: number | null
  purchaseDate: string | null
  warrantyDate: string | null
  supplier: string | null
  status: AssetStatus
  statusDesc: string | null
  managementMode: ManagementMode | null
  managementModeDesc: string | null
  /** 批量管理时的可用库存数量 */
  availableQuantity: number | null
  locationType: string | null
  locationTypeDesc: string | null
  locationId: number | string | null
  locationName: string | null
  responsibleUserId: number | string | null
  responsibleUserName: string | null
  remark: string | null
  createdBy: number | string | null
  createdAt: string
  updatedAt: string
  underWarranty: boolean | null
  // 折旧相关
  categoryType: number | null
  depreciationMethod: number | null
  residualValue: number | null
  accumulatedDepreciation: number | null
  usefulLife: number | null
  stockWarningThreshold: number | null
}

/** 资产变更历史 */
export interface AssetHistory {
  id: number | string
  assetId: number | string
  changeType: string
  changeTypeDesc: string | null
  changeContent: string | null
  oldLocationType: string | null
  oldLocationId: number | string | null
  oldLocationName: string | null
  newLocationType: string | null
  newLocationId: number | string | null
  newLocationName: string | null
  operatorId: number | string
  operatorName: string | null
  operateTime: string
  remark: string | null
}

/** 资产维修记录 */
export interface AssetMaintenance {
  id: number | string
  assetId: number | string
  assetCode: string | null
  assetName: string | null
  maintenanceType: MaintenanceType
  maintenanceTypeDesc: string | null
  faultDesc: string | null
  startDate: string | null
  endDate: string | null
  cost: number | null
  maintainer: string | null
  result: string | null
  status: MaintenanceStatus
  statusDesc: string | null
  createdBy: number | string | null
  createdAt: string
}

/** 资产统计 */
export interface AssetStatistics {
  totalCount: number
  inUseCount: number
  idleCount: number
  repairingCount: number
  scrappedCount: number
  totalOriginalValue?: number
  totalNetValue?: number
  categoryStatistics?: CategoryStatistics[]
  locationStatistics?: LocationStatistics[]
}

export interface CategoryStatistics {
  categoryId: number | string
  categoryName: string
  count: number
  totalValue: number | null
}

export interface LocationStatistics {
  locationType: string
  locationTypeDesc: string
  count: number
}

// ============ 请求参数 ============

/** 资产查询条件 */
export interface AssetQueryCriteria {
  categoryId?: number | string
  status?: AssetStatus
  locationType?: string
  locationId?: number | string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

/** 创建资产请求 */
export interface CreateAssetRequest {
  assetName: string
  categoryId: number | string
  brand?: string
  model?: string
  unit: string
  quantity?: number
  /** 管理模式: 1-单品管理, 2-批量管理 */
  managementMode?: ManagementMode
  originalValue?: number
  netValue?: number
  purchaseDate?: string
  warrantyDate?: string
  supplier?: string
  locationType?: string
  locationId?: number | string
  locationName?: string
  responsibleUserId?: number | string
  responsibleUserName?: string
  remark?: string
}

/** 更新资产请求 */
export interface UpdateAssetRequest {
  assetName?: string
  categoryId?: number | string
  brand?: string
  model?: string
  unit?: string
  quantity?: number
  originalValue?: number
  netValue?: number
  purchaseDate?: string
  warrantyDate?: string
  supplier?: string
  responsibleUserId?: number | string
  responsibleUserName?: string
  remark?: string
}

/** 调拨资产请求 */
export interface TransferAssetRequest {
  locationType: string
  locationId: number | string
  locationName: string
  responsibleUserId?: number | string
  responsibleUserName?: string
  remark?: string
}

/** 报废资产请求 */
export interface ScrapAssetRequest {
  reason?: string
}

/** 创建分类请求 */
export interface CreateCategoryRequest {
  parentId?: number | string
  categoryCode: string
  categoryName: string
  categoryType?: CategoryType
  /** 默认管理模式: 1-单品管理, 2-批量管理 */
  defaultManagementMode?: ManagementMode
  depreciationYears?: number
  unit?: string
  sortOrder?: number
  remark?: string
}

/** 创建维修记录请求 */
export interface CreateMaintenanceRequest {
  maintenanceType: MaintenanceType
  faultDesc?: string
  maintainer?: string
}

/** 完成维修请求 */
export interface CompleteMaintenanceRequest {
  result?: string
  cost?: number
  maintainer?: string
}

/** 批量入库请求 */
export interface BatchCreateAssetRequest {
  assetName: string
  categoryId: number | string
  brand?: string
  model?: string
  unit: string
  /** 入库数量 (1-1000) */
  quantity: number
  /** 单价 */
  originalValue?: number
  purchaseDate?: string
  warrantyDate?: string
  supplier?: string
  locationType?: string
  locationId?: number | string
  locationName?: string
  responsibleUserId?: number | string
  responsibleUserName?: string
  remark?: string
}

/** 批量入库结果 */
export interface BatchCreateResult {
  /** 入库总数 */
  totalCount: number
  /** 成功数量 */
  successCount: number
  /** 首个资产编号 */
  firstAssetCode: string
  /** 末个资产编号 */
  lastAssetCode: string
  /** 生成的资产ID列表 */
  assetIds: (number | string)[]
  /** 总价值 */
  totalValue: number | null
}

/** 批量调拨请求 */
export interface BatchTransferAssetRequest {
  /** 要调拨的资产ID列表 */
  assetIds: (number | string)[]
  /** 目标位置类型 */
  locationType: string
  /** 目标位置ID */
  locationId: number | string
  /** 目标位置名称 */
  locationName: string
  /** 新责任人ID (可选) */
  responsibleUserId?: number | string
  /** 新责任人姓名 (可选) */
  responsibleUserName?: string
  /** 调拨原因/备注 */
  remark?: string
}

/** 批量调拨结果 */
export interface BatchTransferResult {
  /** 调拨总数 */
  totalCount: number
  /** 成功数量 */
  successCount: number
  /** 失败数量 */
  failedCount: number
  /** 成功调拨的资产ID列表 */
  successAssetIds: (number | string)[]
  /** 失败的资产信息 */
  failedAssets: FailedAsset[]
  /** 目标位置名称 */
  targetLocationName: string
}

export interface FailedAsset {
  assetId: number | string
  assetCode?: string
  assetName?: string
  reason: string
}

/** Excel导入结果 */
export interface ImportResult {
  /** 总行数 */
  totalCount: number
  /** 成功数 */
  successCount: number
  /** 失败数 */
  failCount: number
  /** 错误明细 */
  errors: ImportError[]
}

export interface ImportError {
  /** 行号 */
  rowNum: number
  /** 资产名称 */
  assetName?: string
  /** 错误原因 */
  errorMessage: string
}

// ============ 借用相关 ============

/** 借用类型 */
export enum BorrowType {
  USE = 1,      // 领用（无需归还）
  BORROW = 2    // 借用（需归还）
}

export const BorrowTypeMap: Record<BorrowType, string> = {
  [BorrowType.USE]: '领用',
  [BorrowType.BORROW]: '借用'
}

/** 借用状态 */
export enum BorrowStatus {
  BORROWED = 1,   // 借出中
  RETURNED = 2,   // 已归还
  OVERDUE = 3,    // 已逾期
  CANCELLED = 4   // 已取消
}

export const BorrowStatusMap: Record<BorrowStatus, string> = {
  [BorrowStatus.BORROWED]: '借出中',
  [BorrowStatus.RETURNED]: '已归还',
  [BorrowStatus.OVERDUE]: '已逾期',
  [BorrowStatus.CANCELLED]: '已取消'
}

/** 归还状况 */
export enum ReturnCondition {
  GOOD = 'good',
  DAMAGED = 'damaged',
  LOST = 'lost'
}

export const ReturnConditionMap: Record<ReturnCondition, string> = {
  [ReturnCondition.GOOD]: '完好',
  [ReturnCondition.DAMAGED]: '损坏',
  [ReturnCondition.LOST]: '丢失'
}

/** 资产借用记录 */
export interface AssetBorrow {
  id: number | string
  borrowNo: string
  borrowType: number
  borrowTypeDesc: string | null

  // 资产信息
  assetId: number | string
  assetCode: string
  assetName: string
  quantity: number

  // 借用人信息
  borrowerId: number | string
  borrowerName: string
  borrowerDept: string | null
  borrowerPhone: string | null

  // 时间信息
  borrowDate: string
  expectedReturnDate: string | null
  actualReturnDate: string | null

  // 归还信息
  returnCondition: string | null
  returnConditionDesc: string | null
  returnRemark: string | null
  returnerId: number | string | null
  returnerName: string | null

  // 申请信息
  purpose: string | null
  status: number
  statusDesc: string | null

  // 操作信息
  operatorId: number | string
  operatorName: string

  // 计算字段
  overdue: boolean | null
  overdueDays: number | null
  remainingDays: number | null

  createdAt: string
}

/** 创建借用请求 */
export interface CreateBorrowRequest {
  borrowType: BorrowType
  assetId: number | string
  quantity?: number
  borrowerId: number | string
  borrowerName: string
  borrowerDept?: string
  borrowerPhone?: string
  expectedReturnDate?: string
  purpose?: string
}

/** 归还借用请求 */
export interface ReturnBorrowRequest {
  returnCondition?: ReturnCondition
  returnRemark?: string
}

/** 借用统计 */
export interface BorrowStatistics {
  borrowedCount: number
  overdueCount: number
  usedCount: number
}

// ============ 盘点相关 ============

/** 盘点状态 */
export enum InventoryStatus {
  IN_PROGRESS = 1,  // 进行中
  COMPLETED = 2,    // 已完成
  CANCELLED = 3     // 已取消
}

export const InventoryStatusMap: Record<InventoryStatus, string> = {
  [InventoryStatus.IN_PROGRESS]: '进行中',
  [InventoryStatus.COMPLETED]: '已完成',
  [InventoryStatus.CANCELLED]: '已取消'
}

/** 盘点结果类型 */
export enum InventoryResultType {
  NORMAL = 1,   // 正常
  PROFIT = 2,   // 盘盈
  LOSS = 3      // 盘亏
}

export const InventoryResultTypeMap: Record<InventoryResultType, string> = {
  [InventoryResultType.NORMAL]: '正常',
  [InventoryResultType.PROFIT]: '盘盈',
  [InventoryResultType.LOSS]: '盘亏'
}

/** 盘点范围类型 */
export enum InventoryScopeType {
  ALL = 'all',            // 全部资产
  CATEGORY = 'category',  // 按分类
  LOCATION = 'location'   // 按位置
}

export const InventoryScopeTypeMap: Record<InventoryScopeType, string> = {
  [InventoryScopeType.ALL]: '全部资产',
  [InventoryScopeType.CATEGORY]: '按分类',
  [InventoryScopeType.LOCATION]: '按位置'
}

/** 资产盘点 */
export interface AssetInventory {
  id: number | string
  inventoryCode: string
  inventoryName: string
  scopeType: string | null
  scopeTypeDesc: string | null
  scopeValue: string | null
  startDate: string
  endDate: string
  status: number
  statusDesc: string | null
  totalCount: number
  checkedCount: number
  profitCount: number
  lossCount: number
  /** 进度百分比 (0-100) */
  progress: number
  createdBy: number | string | null
  createdAt: string
  details?: AssetInventoryDetail[]
}

/** 盘点明细 */
export interface AssetInventoryDetail {
  id: number | string
  inventoryId: number | string
  assetId: number | string
  assetCode: string | null
  assetName: string | null
  locationName: string | null
  expectedQuantity: number
  actualQuantity: number | null
  difference: number | null
  resultType: number | null
  resultTypeDesc: string | null
  checkTime: string | null
  checkerId: number | string | null
  checkerName: string | null
  remark: string | null
}

/** 创建盘点请求 */
export interface CreateInventoryRequest {
  inventoryName: string
  scopeType?: string
  scopeValue?: string
  startDate: string
  endDate: string
}

/** 更新盘点明细请求 */
export interface UpdateInventoryDetailRequest {
  actualQuantity: number
  remark?: string
}

/** 盘点统计 */
export interface InventoryStatistics {
  inProgressCount: number
  completedCount: number
}

// ============ 审批相关 ============

/** 审批类型 */
export enum ApprovalType {
  BORROW_APPLY = 1,   // 借用申请
  PROCUREMENT = 2,    // 采购申请
  SCRAP = 3,          // 报废申请
  TRANSFER = 4        // 调拨申请
}

export const ApprovalTypeMap: Record<ApprovalType, string> = {
  [ApprovalType.BORROW_APPLY]: '借用申请',
  [ApprovalType.PROCUREMENT]: '采购申请',
  [ApprovalType.SCRAP]: '报废申请',
  [ApprovalType.TRANSFER]: '调拨申请'
}

/** 审批状态 */
export enum ApprovalStatus {
  PENDING = 0,    // 待审批
  APPROVED = 1,   // 已通过
  REJECTED = 2,   // 已拒绝
  CANCELLED = 3   // 已取消
}

export const ApprovalStatusMap: Record<ApprovalStatus, string> = {
  [ApprovalStatus.PENDING]: '待审批',
  [ApprovalStatus.APPROVED]: '已通过',
  [ApprovalStatus.REJECTED]: '已拒绝',
  [ApprovalStatus.CANCELLED]: '已取消'
}

/** 资产审批 */
export interface AssetApproval {
  id: number | string
  approvalNo: string
  approvalType: ApprovalType
  approvalTypeDesc: string | null
  businessId: number | string | null
  assetId: number | string | null
  assetName: string | null
  applicantId: number | string
  applicantName: string | null
  applicantDept: string | null
  approverId: number | string | null
  approverName: string | null
  status: ApprovalStatus
  statusDesc: string | null
  applyReason: string | null
  applyQuantity: number | null
  applyAmount: number | null
  approvalRemark: string | null
  applyTime: string
  approvalTime: string | null
  expireTime: string | null
  createdAt: string
}

/** 创建审批请求 */
export interface CreateApprovalRequest {
  approvalType: ApprovalType
  businessId?: number | string
  assetId?: number | string
  assetName?: string
  applyReason?: string
  applyQuantity?: number
  applyAmount?: number
}

/** 审批操作请求 */
export interface ApprovalActionRequest {
  remark?: string
}

// ============ 预警相关 ============

/** 预警类型 */
export enum AlertType {
  OVERDUE = 1,          // 借用逾期
  NEAR_OVERDUE = 2,     // 即将逾期
  WARRANTY_EXPIRE = 3,  // 保修到期
  LOW_STOCK = 4         // 库存不足
}

export const AlertTypeMap: Record<AlertType, string> = {
  [AlertType.OVERDUE]: '借用逾期',
  [AlertType.NEAR_OVERDUE]: '即将逾期',
  [AlertType.WARRANTY_EXPIRE]: '保修到期',
  [AlertType.LOW_STOCK]: '库存不足'
}

/** 预警级别 */
export enum AlertLevel {
  NORMAL = 1,    // 普通
  IMPORTANT = 2, // 重要
  URGENT = 3     // 紧急
}

export const AlertLevelMap: Record<AlertLevel, string> = {
  [AlertLevel.NORMAL]: '普通',
  [AlertLevel.IMPORTANT]: '重要',
  [AlertLevel.URGENT]: '紧急'
}

/** 资产预警 */
export interface AssetAlert {
  id: number | string
  alertType: AlertType
  alertTypeDesc: string | null
  assetId: number | string | null
  assetCode: string | null
  assetName: string | null
  borrowId: number | string | null
  alertContent: string
  alertLevel: AlertLevel
  alertLevelDesc: string | null
  isRead: boolean
  isHandled: boolean
  handleRemark: string | null
  handleTime: string | null
  handlerId: number | string | null
  handlerName: string | null
  notifyUserId: number | string | null
  notifyUserName: string | null
  alertTime: string
  expireTime: string | null
  createdAt: string
}

/** 预警统计 */
export interface AlertStatistics {
  totalCount: number
  unreadCount: number
  unhandledCount: number
  overdueCount: number
  nearOverdueCount: number
  warrantyExpireCount: number
  lowStockCount: number
}

// ============ 折旧相关 ============

/** 折旧方法 */
export enum DepreciationMethod {
  STRAIGHT_LINE = 1,      // 直线法
  DECLINING_BALANCE = 2,  // 余额递减法
  NONE = 3                // 不计提
}

export const DepreciationMethodMap: Record<DepreciationMethod, string> = {
  [DepreciationMethod.STRAIGHT_LINE]: '直线法',
  [DepreciationMethod.DECLINING_BALANCE]: '余额递减法',
  [DepreciationMethod.NONE]: '不计提折旧'
}

/** 折旧记录 */
export interface AssetDepreciation {
  id: number | string
  assetId: number | string
  assetCode: string | null
  assetName: string | null
  depreciationMethod: DepreciationMethod
  depreciationYear: number
  depreciationMonth: number
  originalValue: number
  residualValue: number | null
  netValueBefore: number
  depreciationAmount: number
  accumulatedDepreciation: number
  netValueAfter: number
  depreciationRate: number | null
  usefulLife: number | null
  remark: string | null
  createdAt: string
}
