/**
 * V2 API 模块索引 - DDD架构适配
 */

// 组织管理
export * from './organization'

// 量化检查
export * from './inspection'

// 权限管理
export * from './access'

// 学生管理
export * from './student'

// 宿舍管理
export * from './dormitory'

// 任务管理
export * from './task'

// 用户管理
export * from './user'

// 学期管理
export * from './semester'

// V4 检查会话
export * from './inspectionSession'

// 加分项配置
export * from './bonusItem'

// 班主任工作台
export * from './teacherDashboard'

// 系部排名
export * from './departmentRanking'

// 整改工单
export * from './correctiveAction'

// 学生行为
export * from './behavior'

// 排班管理
export * from './schedule'

// 数据分析
export * from './inspectionAnalytics'

// 数据导出
export * from './inspectionExport'

// 导出命名空间便于按模块调用
import * as organizationApi from './organization'
import * as inspectionApi from './inspection'
import * as accessApi from './access'
import * as studentApi from './student'
import * as dormitoryApiModule from './dormitory'
import * as taskApiModule from './task'
import * as userApiModule from './user'
import * as semesterApiModule from './semester'
import * as inspectionSessionApiModule from './inspectionSession'
import * as bonusItemApiModule from './bonusItem'
import * as teacherDashboardApiModule from './teacherDashboard'
import * as departmentRankingApiModule from './departmentRanking'
import * as correctiveActionApiModule from './correctiveAction'
import * as behaviorApiModule from './behavior'
import * as scheduleApiModule from './schedule'
import * as inspectionAnalyticsApiModule from './inspectionAnalytics'
import * as inspectionExportApiModule from './inspectionExport'

export const v2Api = {
  organization: organizationApi,
  inspection: inspectionApi,
  access: accessApi,
  student: studentApi,
  dormitory: dormitoryApiModule,
  task: taskApiModule,
  user: userApiModule,
  semester: semesterApiModule,
  inspectionSession: inspectionSessionApiModule,
  bonusItem: bonusItemApiModule,
  teacherDashboard: teacherDashboardApiModule,
  departmentRanking: departmentRankingApiModule,
  correctiveAction: correctiveActionApiModule,
  behavior: behaviorApiModule,
  schedule: scheduleApiModule,
  inspectionAnalytics: inspectionAnalyticsApiModule,
  inspectionExport: inspectionExportApiModule
}

export default v2Api
