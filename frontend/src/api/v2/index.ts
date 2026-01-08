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

// 导出命名空间便于按模块调用
import * as organizationApi from './organization'
import * as inspectionApi from './inspection'
import * as accessApi from './access'
import * as studentApi from './student'
import * as dormitoryApiModule from './dormitory'
import * as taskApiModule from './task'

export const v2Api = {
  organization: organizationApi,
  inspection: inspectionApi,
  access: accessApi,
  student: studentApi,
  dormitory: dormitoryApiModule,
  task: taskApiModule
}

export default v2Api
