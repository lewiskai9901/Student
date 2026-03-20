/**
 * 我的班级 - 类型定义
 */

/**
 * 我的班级 - 列表项
 */
export interface MyClassItem {
  id: string | number
  classCode: string
  className: string
  shortName?: string
  currentSize: number
  standardSize: number
  status: 'PREPARING' | 'ACTIVE' | 'GRADUATED' | 'DISSOLVED'
  enrollmentYear: number
  majorName?: string
  orgUnitName?: string
  myRole: 'HEAD_TEACHER' | 'DEPUTY_HEAD_TEACHER' | 'SUBJECT_TEACHER' | 'COUNSELOR'
  weeklyRank?: number
  totalClasses?: number
  weeklyScore?: number
  scoreTrend?: number[]
}

/**
 * 成绩趋势项
 */
export interface ScoreTrendItem {
  date: string
  score: number
}

/**
 * 最近检查记录
 */
export interface RecentCheckRecord {
  id: number | string
  checkDate: string
  checkType: string
  score: number
  rank: number
}

/**
 * 班级概览数据
 */
export interface MyClassOverview {
  classId: number | string
  className: string
  studentCount: number
  maleCount: number
  femaleCount: number
  classRank: number
  totalClasses: number
  averageScore: number
  scoreTrend: number
  pendingAppeals: number
  scoreTrendList: ScoreTrendItem[]
  recentRecords: RecentCheckRecord[]
}

/**
 * 班级学生列表项
 */
export interface MyClassStudent {
  id: number | string
  studentNo: string
  name: string
  gender: '男' | '女'
  phone?: string
  dormitoryName?: string
  bedNo?: string
  status: 'ENROLLED' | 'SUSPENDED' | 'GRADUATED' | 'DROPPED'
}

/**
 * 宿舍分布信息
 */
export interface DormitoryDistribution {
  buildingId: number | string
  buildingName: string
  buildingType: 'MALE' | 'FEMALE' | 'MIXED'
  rooms: DormitoryRoom[]
  studentCount: number
}

/**
 * 宿舍房间信息
 */
export interface DormitoryRoom {
  dormitoryId: number | string
  roomNo: string
  floor: number
  studentCount: number
  students: DormitoryStudent[]
}

/**
 * 宿舍学生信息
 */
export interface DormitoryStudent {
  id: number | string
  name: string
  bedNo: string
}

/**
 * 角色显示配置
 */
export const MyRoleConfig: Record<string, { label: string; color: string }> = {
  HEAD_TEACHER: { label: '班主任', color: '#409EFF' },
  DEPUTY_HEAD_TEACHER: { label: '副班主任', color: '#67C23A' },
  SUBJECT_TEACHER: { label: '任课教师', color: '#909399' },
  COUNSELOR: { label: '辅导员', color: '#E6A23C' }
}

/**
 * 班级状态显示配置
 */
export const ClassStatusConfig: Record<string, { label: string; type: 'success' | 'warning' | 'info' | 'danger' }> = {
  PREPARING: { label: '筹建中', type: 'warning' },
  ACTIVE: { label: '正常', type: 'success' },
  GRADUATED: { label: '已毕业', type: 'info' },
  DISSOLVED: { label: '已解散', type: 'danger' }
}

/**
 * 学生状态显示配置
 */
export const StudentStatusConfig: Record<string, { label: string; type: 'success' | 'warning' | 'info' | 'danger' }> = {
  ENROLLED: { label: '在读', type: 'success' },
  SUSPENDED: { label: '休学', type: 'warning' },
  GRADUATED: { label: '已毕业', type: 'info' },
  DROPPED: { label: '退学', type: 'danger' }
}
