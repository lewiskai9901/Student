import { http } from '@/utils/request'

/**
 * 检查记录 API (使用新版 /check-records 接口)
 */

// 手动触发转换（从日常检查生成检查记录）
export function convertDailyCheck(dailyCheckId: number | string) {
  return http.post(`/quantification/check-records/convert/${dailyCheckId}`)
}

// 查询检查记录列表(管理员)
export function getCheckRecordsList(params: {
  pageNum?: number
  pageSize?: number
  startDate?: string
  endDate?: string
  checkerId?: number
  status?: number
  planId?: string | number
}) {
  return http.get('/quantification/check-records/list', { params })
}

// 查询检查记录列表(班主任) - 使用相同接口，后端按角色过滤
export function getMyCheckRecords(params: {
  pageNum?: number
  pageSize?: number
  startDate?: string
  endDate?: string
}) {
  return http.get('/quantification/check-records/my-records', { params })
}

// 查询检查记录详情(管理员) - 包含班级统计
export function getCheckRecordDetail(id: string) {
  return http.get(`/quantification/check-records/${id}/detail`)
}

// 查询本班详情(班主任)
export function getMyClassDetail(id: string) {
  return http.get(`/quantification/check-records/${id}/my-class`)
}

// 查询班级历史
export function getClassHistory(classId: string, limit?: number) {
  return http.get(`/quantification/check-records/class/${classId}/history`, {
    params: { limit }
  })
}

// 导出检查记录
export function exportCheckRecord(id: string) {
  return http.get(`/quantification/check-records/${id}/export`, {
    responseType: 'blob'
  })
}

// 删除检查记录
export function deleteCheckRecord(id: string) {
  return http.delete(`/quantification/check-records/${id}/detail`)
}

// 发布检查记录
export function publishCheckRecord(id: string, configId?: number) {
  return http.post(`/quantification/check-records/${id}/publish`, configId ? { configId } : {})
}

// 撤回发布
export function unpublishCheckRecord(id: string) {
  return http.post(`/quantification/check-records/${id}/unpublish`)
}

// 预览评级结果 - 获取班级统计
export function previewRatings(id: string, configId?: number) {
  return http.get(`/quantification/check-records/${id}/preview-ratings`, {
    params: configId ? { configId } : {}
  })
}

// 获取班级排名列表（含加权分数）
export function getClassRanking(recordId: string, params?: {
  sortBy?: 'weighted' | 'original'  // 排序方式：weighted=加权分数, original=原始分数
  departmentId?: string | number
  gradeLevel?: number
}) {
  return http.get(`/quantification/check-records/${recordId}/ranking`, { params })
}

// 获取扣分明细列表
export function getDeductionList(recordId: string, classId?: string | number) {
  return http.get(`/quantification/check-records/${recordId}/deductions`, {
    params: classId ? { classId } : {}
  })
}

// 重新计算检查记录统计
export function recalculateRecord(id: string, reason?: string) {
  return http.post(`/quantification/check-records/${id}/recalculate`, null, {
    params: { reason: reason || '手动重算' }
  })
}

// 获取检查记录的加权配置详情
export function getWeightConfigDetail(recordId: string) {
  return http.get(`/quantification/check-records/${recordId}/weight-config`)
}

// 获取检查记录的加权配置树形结构（含3级继承关系）
export function getWeightConfigTree(recordId: string) {
  return http.get(`/quantification/check-records/${recordId}/weight-config-tree`)
}

// TypeScript 接口定义
// 注意: 所有ID字段都是string类型,因为后端序列化Long为String避免JS精度丢失

// 扣分明细（前置定义）
export interface DeductionDetailBase {
  id: string
  categoryName: string
  deductionItemName: string
  actualScore: number
  checkRound?: number
  personCount?: number
  linkType?: number
  linkId?: string
  linkCode?: string
  linkName?: string
  photoUrlList?: string[]
  photoCount?: number
  remark?: string
  appealStatus?: number
  studentNameList?: string[]
}

// 类别统计（前置定义）
export interface CheckRecordCategoryStatsDTO {
  categoryId?: string
  categoryCode?: string
  categoryName: string
  categoryType?: string
  categoryTypeName?: string
  checkRound?: number
  deductionCount?: number
  totalScore: number
  weightedTotalScore?: number  // 加权后扣分
  deductions?: DeductionDetailBase[]
}

export interface CheckRecord {
  id: string  // Long类型从后端序列化为string
  recordCode: string
  checkName: string
  checkDate: string
  checkerName: string
  totalClasses: number
  totalScore: number
  avgScore: number
  status: number
  publishTime: string
}

export interface ClassStats {
  id?: string  // 班级统计记录ID
  classId?: string | number  // Long类型
  className: string
  teacherName?: string
  teacherPhone?: string
  gradeName?: string
  gradeId?: string
  departmentId?: string
  departmentName?: string
  classSize?: number  // 班级人数
  totalScore: number
  deductionCount?: number
  hygieneScore?: number
  disciplineScore?: number
  attendanceScore?: number
  dormitoryScore?: number
  otherScore?: number

  // 排名信息
  overallRanking?: number  // 全校排名
  gradeRanking?: number    // 年级排名
  departmentRanking?: number  // 院系排名
  scoreLevel?: string      // 评分等级

  // 加权信息
  weightEnabled?: boolean  // 是否启用加权
  weightFactor?: number    // 加权系数
  standardSize?: number    // 标准人数
  weightMode?: string      // 加权模式: STANDARD(标准人数/班级人数) 或 PER_CAPITA(班级人数/标准人数)
  weightModeDesc?: string  // 加权模式描述
  weightedTotalScore?: number  // 加权后总扣分
  weightedHygieneScore?: number  // 卫生类加权扣分
  weightedDisciplineScore?: number  // 纪律类加权扣分
  weightedAttendanceScore?: number  // 考勤类加权扣分
  weightedDormitoryScore?: number  // 宿舍类加权扣分
  weightedOtherScore?: number  // 其他类加权扣分
  weightCalculationDetails?: string  // 加权计算详情

  // 加权后排名
  weightedOverallRanking?: number
  weightedGradeRanking?: number
  weightedDepartmentRanking?: number

  // 对比分析
  vsAvgDiff?: number  // 与平均分差值
  vsLastDiff?: number  // 与上次检查差值
  trend?: string  // 趋势：UP/DOWN/STABLE

  // 申诉统计
  appealCount?: number
  appealApproved?: number
  appealPending?: number

  // 关联数据
  deductions?: DeductionDetail[]
  categoryStats?: CheckRecordCategoryStatsDTO[]

  // 多配置加权信息
  multiConfigEnabled?: boolean  // 是否启用多配置模式
  multiWeightConfigs?: MultiWeightConfigInfo[]  // 多配置列表
}

// 多配置加权信息接口
export interface MultiWeightConfigInfo {
  configId?: string
  configName?: string
  colorCode?: string
  colorName?: string
  isDefault?: boolean
  applyCategoryIds?: string[]
  applyCategoryNames?: string[]
  weightMode?: string
  weightModeDesc?: string
  standardSizeMode?: string
  standardSizeModeDesc?: string  // 标准人数获取方式描述（如：固定值、目标平均）
  standardSize?: number
  weightFactor?: number
  minWeight?: number  // 系数下限
  maxWeight?: number  // 系数上限
  originalScore?: number
  weightedScore?: number
}

export interface DeductionDetail {
  id: string  // Long类型从后端序列化为string
  categoryName: string
  deductionItemName: string
  actualScore: number
  checkRound?: number  // 检查轮次
  personCount?: number
  linkType?: number  // 关联类型：0=无 1=宿舍 2=教室
  linkId?: string
  linkCode?: string
  linkName?: string
  photoUrlList?: string[]
  photoCount?: number
  remark?: string
  appealStatus?: number  // 0=未申诉 1=申诉中 2=已通过 3=已驳回
  studentNameList?: string[]  // 关联学生姓名列表
}

export interface ItemDetail {
  id: string  // Long类型从后端序列化为string
  categoryName: string
  itemName: string
  deductScore: number
  personCount?: number
  linkInfo?: string  // 格式: "宿舍:202" 或 "教室:301"
  photoUrls: string[]
  remark?: string
  appealStatus: number
}

export interface CheckRecordDetail {
  id: string  // Long类型从后端序列化为string
  planId?: string  // 检查计划ID
  recordCode: string
  checkName: string
  checkDate: string
  checkerName: string
  totalClasses: number
  totalScore: number
  avgScore: number
  status: number
  publishTime: string
  classStats?: ClassStats[]  // 管理员看到所有班级
  myClassStats?: ClassStats  // 班主任看到本班
}

// 发布检查记录响应
export interface PublishCheckRecordResponse {
  recordId: string
  publishTime: string
  publisherId: string
  publisherName: string
  configId: number
  configName: string
  totalClasses: number
  ratingResults: RatingResult[]
}

// 评级结果
export interface RatingResult {
  classId: string
  className: string
  totalScore: number
  ranking: number
  levelName: string
  levelColor: string
  percentageRank: number
  rewardPoints: number
}
