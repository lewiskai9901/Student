// 量化管理 1.0 版本 - 类型定义

// ========== 量化类型模块 ==========

/**
 * 量化类型
 */
export interface QuantificationType {
  id: string | number
  typeName: string
  typeCode: string
  checkFrequency: number // 检查频率: 1每天 2每周 3每月 4不定期
  timesPerDay?: number // 每天检查次数
  deductMode: number // 扣分模式: 1固定扣分 2按人数扣分 3区间扣分
  deductConfig?: string // 扣分配置JSON
  isActive: number // 是否启用: 1启用 0禁用
  createdAt?: string
  updatedAt?: string
}

/**
 * 扣分配置
 */
export interface DeductConfig {
  items: DeductItem[]
}

/**
 * 扣分项
 */
export interface DeductItem {
  name: string // 扣分项名称
  code: string // 扣分项编码
  score?: number // 固定扣分分数（模式1使用）
  baseScore?: number // 基础扣分分数（模式2使用）
  perPersonScore?: number // 每人扣分分数（模式2使用）
  ranges?: ScoreRange[] // 区间扣分配置（模式3使用）
}

/**
 * 区间扣分配置
 */
export interface ScoreRange {
  min: number // 区间最小值
  max: number // 区间最大值
  score: number // 该区间对应的扣分分数
  description?: string // 区间描述
}

/**
 * 量化类型查询参数
 */
export interface QuantificationTypeQueryParams {
  typeName?: string
  typeCode?: string
  checkFrequency?: number
  isActive?: number
  pageNum?: number
  pageSize?: number
}

/**
 * 量化类型表单数据
 */
export interface QuantificationTypeFormData {
  typeName: string
  typeCode: string
  checkFrequency: number
  timesPerDay?: number
  deductMode: number
  deductConfig?: string
  isActive: number
}

// ========== 量化记录模块 ==========

/**
 * 量化记录
 */
export interface QuantificationRecord {
  id: string | number
  typeId: string | number
  typeName?: string
  typeCode?: string
  classId: string | number
  className: string
  checkDate: string
  checkTime?: string
  checkerId?: string | number
  checkerName?: string
  deductScore: number
  personCount?: number
  deductReason?: string
  evidenceImages?: string
  status: number // 状态: 1待审核 2已确认 3已驳回
  reviewerId?: string | number
  reviewerName?: string
  reviewTime?: string
  rejectReason?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 量化记录查询参数
 */
export interface QuantificationRecordQueryParams {
  typeId?: string | number
  classId?: string | number
  startDate?: string
  endDate?: string
  status?: number
  checkerName?: string
  pageNum?: number
  pageSize?: number
}

/**
 * 量化记录表单数据
 */
export interface QuantificationRecordFormData {
  typeId: string | number
  classId: string | number
  className?: string
  checkDate: string
  checkTime?: string
  checkerName?: string
  deductScore: number
  personCount?: number
  deductReason?: string
  evidenceImages?: string
}

// ========== 向后兼容的类型别名 (2.0) ==========

/**
 * 检查类别 (兼容量化2.0)
 * 映射到量化类型
 */
export type CheckCategory = QuantificationType

/**
 * 扣分项 (兼容量化2.0)
 * 在1.0版本中不再使用独立的扣分项，而是使用deductConfig配置
 */
export interface DeductionItem {
  id: string | number
  categoryId?: string | number
  typeId?: string | number
  itemName: string
  itemCode?: string
  // 模式1: 固定扣分
  fixedScore?: number
  // 模式2: 按人次扣分 - 基础分
  baseScore?: number
  // 模式2: 按人次扣分 - 每人扣分
  perPersonScore?: number
  // 模式3: 区间扣分配置
  rangeConfig?: string
  // 兼容旧字段
  deductionScore?: number
  deductionMode: number
  maxDeduction?: number
  description?: string
  allowPhoto?: number
  sortOrder?: number
  status?: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 类别表单数据 (兼容量化2.0)
 */
export type CategoryFormData = QuantificationTypeFormData

/**
 * 扣分项表单数据 (兼容量化2.0)
 */
export interface DeductionItemFormData {
  categoryId: string | number | null
  itemName: string
  itemCode: string
  deductionScore: number
  deductionMode: number
  maxDeduction?: number
  description?: string
  allowPhoto: number
  sortOrder: number
  status: number
}

/**
 * 每日检查记录 (兼容量化2.0)
 * 映射到量化记录
 */
export type DailyRecord = QuantificationRecord

/**
 * 班级检查记录 (兼容量化2.0)
 */
export interface ClassCheck {
  id: string | number
  dailyRecordId: string | number
  classId: string | number
  className: string
  totalDeduction: number
  remarks?: string
  checkDetails?: CheckDetail[]
}

/**
 * 检查明细 (兼容量化2.0)
 */
export interface CheckDetail {
  id: string | number
  classCheckId: string | number
  categoryId: string | number
  categoryName: string
  deductionItemId?: string | number | null
  deductionItemName?: string
  deductionScore: number
  remarks?: string
  checkTime?: string
  photos?: CheckPhoto[]
}

/**
 * 检查照片
 */
export interface CheckPhoto {
  id: string | number
  checkDetailId: string | number
  photoUrl: string
  photoName?: string
  fileSize?: number
  uploadTime?: string
}

/**
 * 创建每日检查记录请求 (兼容量化2.0)
 */
export interface CreateDailyRecordRequest {
  gradeId: string | number
  checkDate: string
  remarks?: string
}

/**
 * 扣分请求 (兼容量化2.0)
 */
export interface CheckDeductionRequest {
  classCheckId: string | number
  categoryId: string | number
  deductionItemIds: (string | number)[]
  customScores?: Record<string | number, number>
  personCounts?: Record<string | number, number>
  remarks?: string
  checkTime?: string
  photoUrls?: string[]
}

/**
 * 每日记录查询参数 (兼容量化2.0)
 */
export type DailyRecordQueryParams = QuantificationRecordQueryParams

// ========== 统计分析模块 ==========

/**
 * 班级排名数据
 */
export interface ClassRanking {
  classId: string | number
  className: string
  totalDeduction: number
  checkCount: number
  avgDeduction: number
  ranking: number
}

/**
 * 扣分频次分析
 */
export interface DeductionFrequency {
  deductionItemId: string | number
  itemName: string
  categoryName: string
  frequency: number
  totalScore: number
  affectedClasses: number
}

/**
 * 扣分分类占比
 */
export interface CategoryRatio {
  categoryId: string | number
  categoryName: string
  deductionCount: number
  totalDeduction: number
  percentage: number
}

/**
 * 趋势数据
 */
export interface TrendData {
  date: string
  totalDeduction: number
  checkCount: number
  avgDeduction: number
}

/**
 * 统计查询参数
 */
export interface StatisticsQueryParams {
  gradeId: string | number
  startDate: string
  endDate: string
}
