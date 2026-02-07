/**
 * V2 API 模块索引 - DDD架构适配
 */

// 组织管理
export * from './organization'

// 量化检查
// export * from './inspection' // TODO: module not yet implemented

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
// export * from './inspectionSession' // TODO: module not yet implemented

// 加分项配置
export * from './bonusItem'

// 班主任工作台
export * from './teacherDashboard'

// 组织排名 (新)
export * from './orgRanking'

// 系部排名 (已废弃，使用 orgRanking)
export * from './departmentRanking'

// 整改工单
// export * from './correctiveAction' // TODO: module not yet implemented

// 学生行为
// export * from './behavior' // TODO: module not yet implemented

// 排班管理
// export * from './schedule' // TODO: module not yet implemented

// 数据分析
// export * from './inspectionAnalytics' // TODO: module not yet implemented

// 数据导出
// export * from './inspectionExport' // TODO: module not yet implemented

// 导出命名空间便于按模块调用
import * as organizationApi from './organization'
// import * as inspectionApi from './inspection' // TODO: module not yet implemented
import * as accessApi from './access'
import * as studentApi from './student'
import * as dormitoryApiModule from './dormitory'
import * as taskApiModule from './task'
import * as userApiModule from './user'
import * as semesterApiModule from './semester'
// import * as inspectionSessionApiModule from './inspectionSession' // TODO: module not yet implemented
import * as bonusItemApiModule from './bonusItem'
import * as teacherDashboardApiModule from './teacherDashboard'
import * as orgRankingApiModule from './orgRanking'
import * as departmentRankingApiModule from './departmentRanking'
// import * as correctiveActionApiModule from './correctiveAction' // TODO: module not yet implemented
// import * as behaviorApiModule from './behavior' // TODO: module not yet implemented
// import * as scheduleApiModule from './schedule' // TODO: module not yet implemented
// import * as inspectionAnalyticsApiModule from './inspectionAnalytics' // TODO: module not yet implemented
// import * as inspectionExportApiModule from './inspectionExport' // TODO: module not yet implemented

export const v2Api = {
  organization: organizationApi,
  // inspection: inspectionApi, // TODO: module not yet implemented
  access: accessApi,
  student: studentApi,
  dormitory: dormitoryApiModule,
  task: taskApiModule,
  user: userApiModule,
  semester: semesterApiModule,
  // inspectionSession: inspectionSessionApiModule, // TODO: module not yet implemented
  bonusItem: bonusItemApiModule,
  teacherDashboard: teacherDashboardApiModule,
  orgRanking: orgRankingApiModule,
  /** @deprecated 使用 orgRanking */
  departmentRanking: departmentRankingApiModule,
  // correctiveAction: correctiveActionApiModule, // TODO: module not yet implemented
  // behavior: behaviorApiModule, // TODO: module not yet implemented
  // schedule: scheduleApiModule, // TODO: module not yet implemented
  // inspectionAnalytics: inspectionAnalyticsApiModule, // TODO: module not yet implemented
  // inspectionExport: inspectionExportApiModule // TODO: module not yet implemented
}

export default v2Api
