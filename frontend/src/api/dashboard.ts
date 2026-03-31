import { http } from '@/utils/request'

// Dashboard overview response types
export interface OrgStats {
  orgUnitCount: number
  majorCount: number
  classCount: number
  studentCount: number
  teacherCount: number
}

export interface TeachingStats {
  currentSemester: string
  courseCount: number
  taskCount: number
  scheduledRate: number
  unscheduledCount: number
}

export interface InspectionStats {
  activeProjectCount: number
  pendingTaskCount: number
  correctiveOpenCount: number
}

export interface SystemStats {
  totalUsers: number
  todayLoginCount: number
}

export interface DashboardOverview {
  organization: OrgStats
  teaching: TeachingStats
  inspection: InspectionStats
  system: SystemStats
}

// Fetch dashboard overview
export function getDashboardOverview(): Promise<DashboardOverview> {
  return http.get<DashboardOverview>('/dashboard/overview')
}
