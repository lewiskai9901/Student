/**
 * 打分功能类型定义
 */

// 扣分模式
export enum DeductionMode {
  FIXED = 1,        // 固定扣分
  PER_PERSON = 2,   // 按人数扣分
  RANGE = 3         // 范围扣分
}

// 关联类型
export enum LinkType {
  NONE = 0,         // 无关联
  DORMITORY = 1,    // 宿舍
  CLASSROOM = 2     // 教室
}

// 目标班级信息
export interface TargetClassInfo {
  classId: string | number
  className: string
  gradeId?: string | number
  gradeName?: string
  departmentId?: string | number
  departmentName?: string
  studentCount?: number
}

// 检查类别信息
export interface CategoryInfo {
  id: string | number
  categoryId: string | number
  categoryName: string
  categoryCode?: string
  linkType: LinkType
  checkRounds: number
  sortOrder?: number
  deductionItems: DeductionItemInfo[]
}

// 扣分项信息
export interface DeductionItemInfo {
  id: string | number
  itemName: string
  deductMode: DeductionMode
  fixedScore?: number
  baseScore?: number
  perPersonScore?: number
  rangeConfig?: string
  description?: string
  allowPhoto?: number
  allowRemark?: number
  allowStudents?: number
  sortOrder?: number
}

// 范围配置项
export interface ScoreRangeItem {
  minValue: number
  maxValue: number
  score: number
  label: string
}

// 宿舍信息
export interface DormitoryInfo {
  id: string | number
  dormitoryNo: string
  buildingName: string
  floor?: number
  currentCount?: number
}

// 教室信息
export interface ClassroomInfo {
  id: string | number
  classroomNo: string
  buildingName: string
  floor?: number
}

// 班级关联资源
export interface ClassLinkResource {
  classId: string | number
  className: string
  dormitories?: DormitoryInfo[]
  classrooms?: ClassroomInfo[]
}

// 关联资源信息
export interface LinkResourceInfo {
  linkType: LinkType
  classResources: ClassLinkResource[]
}

// 打分明细信息 (已存在的)
export interface ScoringDetailInfo {
  id?: string | number
  checkRound: number
  categoryId: string | number
  classId: string | number
  className?: string
  deductionItemId: string | number
  deductionItemName?: string
  deductMode?: DeductionMode
  linkType: LinkType
  linkId: string | number
  linkNo?: string
  deductScore: number
  personCount?: number
  studentIds?: string
  studentNames?: string
  description?: string
  remark?: string
  photoUrls?: string
  appealStatus?: number
}

// 打分初始化响应
export interface ScoringInitResponse {
  checkId: string | number
  checkName: string
  checkDate: string
  checkType?: number
  checkerId?: string | number
  checkerName?: string
  targetClasses: TargetClassInfo[]
  categories: CategoryInfo[]
  linkResources: Record<string, LinkResourceInfo>
  existingDetails: ScoringDetailInfo[]
}

// 保存打分请求 - 单个明细
export interface ScoringDetailRequest {
  checkRound?: number
  categoryId: string | number
  classId: string | number
  deductionItemId: string | number
  linkType: LinkType
  linkId: string | number
  deductScore: number
  personCount?: number
  studentIds?: string
  studentNames?: string
  description?: string
  remark?: string
  photoUrls?: string
}

// 保存打分请求
export interface SaveScoringRequest {
  checkId: string | number
  checkerId: string | number
  checkerName?: string
  details: ScoringDetailRequest[]
}

// 检查计划列表项
export interface CheckPlanItem {
  id: string | number
  planId?: string | number
  checkName: string
  checkDate: string
  checkType?: number
  checkTypeName?: string
  templateId?: string | number
  templateName?: string
  status: number
  statusName?: string
  totalClasses?: number
  scoredClasses?: number
  progress?: number
  createdAt?: string
}

// 本地打分变更记录
export interface ScoreChange {
  targetKey: string  // classId_categoryId_itemId_linkId_round
  categoryId: string | number
  classId: string | number
  deductionItemId: string | number
  linkType: LinkType
  linkId: string | number
  checkRound: number
  deductScore: number
  personCount?: number
  inputValue: number  // 用户输入的原始值
  timestamp: number
}

// 本地草稿
export interface ScoringDraft {
  checkId: string | number
  scores: ScoreChange[]
  pendingChanges: ScoreChange[]
  timestamp: number
}

// 分页结果
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
