/**
 * 检查记录API
 * 参考Web端实现，与后端接口保持一致
 */
import { get } from '@/utils/request'

// 记录状态
export enum RecordStatus {
  PUBLISHED = 1,  // 已发布
  ARCHIVED = 2    // 已归档
}

// 扣分明细项
export interface DeductionItem {
  id: string | number
  categoryId?: string | number
  categoryName: string
  deductionItemName: string   // 扣分项名称（后端字段名）
  itemName?: string           // 扣分项名称（兼容旧字段）
  actualScore: number         // 实际扣分（后端字段名）
  deductScore?: number        // 扣分（兼容旧字段）
  checkRound?: number         // 检查轮次
  personCount?: number        // 涉及人数
  linkType?: number           // 关联类型：0=无 1=宿舍 2=教室
  linkId?: string | number
  linkCode?: string           // 关联编号（宿舍号/教室号）
  linkNo?: string             // 兼容旧字段
  linkName?: string           // 关联名称
  photoUrlList?: string[]     // 照片列表（后端字段名）
  photoUrls?: string[]        // 照片列表（兼容旧字段）
  photoCount?: number         // 照片数量
  remark?: string             // 备注
  appealStatus?: number       // 申诉状态：0=未申诉 1=申诉中 2=已通过 3=已驳回
  studentNameList?: string[]  // 学生姓名列表（后端字段名）
  studentNames?: string[]     // 学生姓名列表（兼容旧字段）
}

// 类别统计
export interface CategoryStats {
  categoryId?: string | number
  categoryCode?: string
  categoryName: string
  categoryType?: string
  categoryTypeName?: string
  checkRound?: number
  deductionCount?: number     // 扣分项数
  totalScore: number          // 该类别总扣分
  weightedTotalScore?: number // 加权后扣分
  deductions?: DeductionItem[] // 该类别下的扣分明细
}

// 班级统计信息（与后端 CheckRecordClassStatsDTO 对应）
export interface ClassStats {
  id?: string | number        // 统计记录ID
  classId: string | number    // 班级ID
  className: string           // 班级名称
  teacherName?: string        // 班主任姓名
  teacherPhone?: string       // 班主任电话
  gradeId?: string | number   // 年级ID
  gradeName?: string          // 年级名称
  departmentId?: string | number
  departmentName?: string
  classSize?: number          // 班级人数

  // 扣分统计
  totalScore: number          // 总扣分
  deductionCount?: number     // 扣分项数
  hygieneScore?: number       // 卫生扣分
  disciplineScore?: number    // 纪律扣分
  attendanceScore?: number    // 考勤扣分
  dormitoryScore?: number     // 宿舍扣分
  otherScore?: number         // 其他扣分

  // 排名信息
  overallRanking?: number     // 全校排名
  gradeRanking?: number       // 年级排名
  departmentRanking?: number  // 院系排名
  scoreLevel?: string         // 评分等级

  // 加权信息
  weightEnabled?: boolean     // 是否启用加权
  weightFactor?: number       // 加权系数
  standardSize?: number       // 标准人数
  weightMode?: string         // 加权模式
  weightModeDesc?: string     // 加权模式描述
  weightedTotalScore?: number // 加权后总扣分

  // 多配置加权
  multiConfigEnabled?: boolean // 是否启用多配置加权
  multiWeightConfigs?: {       // 多配置列表
    configId?: string | number
    configName?: string
    colorCode?: string
    weightFactor?: number
    standardSize?: number
    applyCategoryNames?: string[]
    isDefault?: boolean
  }[]

  // 对比分析
  vsAvgDiff?: number          // 与平均分差值
  vsLastDiff?: number         // 与上次检查差值
  trend?: string              // 趋势：UP/DOWN/STABLE

  // 申诉统计
  appealCount?: number        // 申诉总数
  appealApproved?: number     // 已通过申诉数
  appealPending?: number      // 待处理申诉数

  // 关联数据
  deductions?: DeductionItem[]      // 扣分明细列表
  categoryStats?: CategoryStats[]   // 类别统计列表
}

// 检查记录详情（与后端 CheckRecordDTO 对应）
// 后端直接返回完整DTO，包含记录信息和classStats
export interface CheckRecordDetail {
  id: string | number
  recordCode: string
  dailyCheckId?: string | number
  checkName: string
  checkDate: string
  checkType?: number
  checkTypeName?: string
  checkerId?: string | number
  checkerName: string
  templateId?: string | number
  templateName?: string

  // 统计信息
  totalClasses: number
  totalDeductionCount?: number
  totalDeductionScore?: number
  totalScore?: number           // 兼容旧字段
  avgScore?: number
  maxScore?: number
  minScore?: number

  // 状态
  status: RecordStatus
  statusName?: string
  publishTime?: string
  archiveTime?: string

  // 申诉统计
  totalAppealCount?: number
  appealPendingCount?: number
  appealApprovedCount?: number
  appealRejectedCount?: number

  // 关联数据 - 班级统计列表
  classStats?: ClassStats[]

  createdAt?: string
  updatedAt?: string
}

// 检查记录列表项（简化版，用于列表展示）
export interface CheckRecord {
  id: string | number
  planId?: string | number
  dailyCheckId?: string | number
  recordCode: string
  checkName: string
  checkDate: string
  checkType?: number
  checkerId?: string | number
  checkerName: string
  totalClasses: number
  totalDeductionCount?: number
  totalDeductionScore?: number
  totalScore?: number           // 兼容旧字段
  avgScore?: number
  maxScore?: number
  minScore?: number
  status: RecordStatus
  publishTime?: string
  createdAt?: string
}

// 本班详情响应（班主任视角）
export interface MyClassDetail extends CheckRecordDetail {
  myClassStats?: ClassStats     // 本班统计数据
}

// 分页结果
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 查询参数
export interface RecordQueryParams {
  planId?: number | string  // 支持字符串，避免大数字精度丢失
  startDate?: string
  endDate?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

/**
 * 获取检查记录列表（管理员）
 */
export function getCheckRecordList(params: RecordQueryParams = {}) {
  return get<PageResult<CheckRecord>>('/quantification/check-records/list', {
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 10,
    ...params
  })
}

/**
 * 获取本班检查记录列表（班主任）
 */
export function getMyClassRecords(params: RecordQueryParams = {}) {
  return get<PageResult<CheckRecord>>('/quantification/check-records/my-records', {
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 10,
    ...params
  })
}

/**
 * 获取检查记录详情（管理员视角）
 * 后端直接返回 CheckRecordDTO，包含记录信息和 classStats 列表
 */
export function getRecordDetail(id: string | number) {
  return get<CheckRecordDetail>(`/quantification/check-records/${id}/detail`)
}

/**
 * 获取本班检查详情（班主任视角）
 */
export function getMyClassDetail(id: string | number) {
  return get<MyClassDetail>(`/quantification/check-records/${id}/my-class`)
}

/**
 * 获取班级排名
 */
export function getClassRanking(recordId: string | number) {
  return get<ClassStats[]>(`/quantification/check-records/${recordId}/class-ranking`)
}

/**
 * 获取班级历史记录
 */
export function getClassHistory(classId: number, params?: { pageNum?: number; pageSize?: number }) {
  return get<PageResult<any>>(`/quantification/check-records/class/${classId}/history`, params)
}
