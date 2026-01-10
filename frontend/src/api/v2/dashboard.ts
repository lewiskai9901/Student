import { http } from '@/utils/request'

// 仪表盘统计数据类型
export interface DashboardStatistics {
  studentCount: number
  classCount: number
  dormitoryCount: number
  todayCheckCount: number
  occupancyRate: number
  completedChecks: number
  pendingChecks: number
  completionRate: number
  chartData: ChartDataItem[]
  checkCategories: CategoryItem[]
  recentRecords: RecentCheckRecord[]
}

export interface ChartDataItem {
  date: string
  score: number
}

export interface CategoryItem {
  name: string
  value: number
  color: string
}

export interface RecentCheckRecord {
  id: number
  typeName: string
  targetName: string
  totalScore: number
  scoreRate: number
  createdAt: string
}

// 获取仪表盘统计数据
export function getDashboardStatistics(days: number = 7): Promise<DashboardStatistics> {
  return http.get<DashboardStatistics>('/dashboard/statistics', { params: { days } })
}
