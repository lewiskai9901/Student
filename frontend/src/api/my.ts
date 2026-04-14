import { http } from '@/utils/request'
import type {
  DashboardSummary,
  TodayLesson,
  MyClass,
  SubstituteTask
} from '@/types/my'

// 首页头部统计
export function getMyDashboardSummary(): Promise<DashboardSummary> {
  return http.get<DashboardSummary>('/my/dashboard/summary')
}

// 今日课表 (date 不传默认今天，格式 YYYY-MM-DD)
export function getMyTodaySchedule(date?: string): Promise<TodayLesson[]> {
  return http.get<TodayLesson[]>('/my/schedule/today', date ? { params: { date } } : undefined)
}

// 我授课/班主任的班级列表
export function getMyClasses(): Promise<MyClass[]> {
  return http.get<MyClass[]>('/my/classes')
}

// 分配给我的代课请求
export function getMySubstituteTasks(): Promise<SubstituteTask[]> {
  return http.get<SubstituteTask[]>('/my/tasks/substitute')
}
