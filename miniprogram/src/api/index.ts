/**
 * API 统一导出
 */

// 公共类型 - 只从 types.ts 导出 PageResult
export type { PageResult, PageParams } from './types'

// 认证相关
export * from './auth'

// 学生管理 - 排除 PageResult
export {
  StudentStatus,
  type Student,
  type StudentQueryParams,
  type StudentFormData,
  getStudentPage,
  getStudentById,
  getStudentByNo,
  createStudent,
  updateStudent,
  deleteStudent,
  deleteStudents,
  checkStudentNoExists,
  updateStudentStatus,
  assignDormitory,
  transferClass,
  resetPassword,
  countStudentsByClass,
  countStudentsByDormitory,
  searchStudents
} from './student'

// 班级管理 - 排除 PageResult
export {
  type Class,
  type ClassQueryParams,
  type ClassFormData,
  getClassList,
  getClassDetail,
  createClass,
  updateClass,
  deleteClass,
  batchDeleteClasses,
  getClassStudents,
  assignTeacher,
  assignClassroom,
  removeClassroom,
  getClassClassroom,
  addDormitory,
  removeDormitory,
  getClassDormitories,
  getAllClasses,
  getClassesByDepartment,
  getClassesByGrade
} from './class'

// 宿舍管理 - 排除 PageResult
export {
  type Building,
  type Dormitory,
  type BedInfo,
  type DormitoryQueryParams,
  type DormitoryFormData,
  type OccupancyStats,
  type BuildingStats,
  type DepartmentStats,
  getBuildingList,
  getBuildingDetail,
  getDormitoryList,
  getDormitoryDetail,
  createDormitory,
  updateDormitory,
  deleteDormitory,
  batchDeleteDormitories,
  getDormitoryBeds,
  assignBed,
  releaseBed,
  getDormitoryOccupancyStats,
  getBedAllocations,
  assignStudentToDormitory,
  removeStudentFromDormitory,
  swapStudentDormitory,
  getDormitoriesByBuildingId,
  getDormitoriesByDepartment,
  getAvailableRooms
} from './dormitory'

// 用户管理 - 排除 PageResult
export {
  type User,
  type UserQueryParams,
  type UserFormData,
  getUserPage,
  getUserList,
  getUserDetail,
  createUser,
  updateUser,
  deleteUser,
  batchDeleteUsers,
  resetUserPassword,
  updateUserStatus,
  checkUsernameExists,
  getUserRoles,
  assignUserRoles,
  getTeacherList
} from './user'

// 部门管理 - 排除 PageResult
export {
  type Department,
  type DepartmentQueryParams,
  type DepartmentFormData,
  getDepartmentList,
  getDepartmentDetail,
  createDepartment,
  updateDepartment,
  deleteDepartment,
  getEnabledDepartments,
  getDepartmentTree,
  getChildDepartments
} from './department'

// 年级管理 - 排除 PageResult
export {
  type Grade,
  type GradeQueryParams,
  type GradeFormData,
  getGradeList,
  getGradeDetail,
  createGrade,
  updateGrade,
  deleteGrade,
  getAllGrades,
  getEnabledGrades
} from './grade'

// 专业管理 - 排除 PageResult
export {
  type Major,
  type MajorQueryParams,
  type MajorFormData,
  getMajorList,
  getMajorDetail,
  createMajor,
  updateMajor,
  deleteMajor,
  getAllMajors,
  getMajorsByDepartment,
  getEnabledMajors
} from './major'

// 角色权限 - 排除 PageResult
export {
  type Role,
  type Permission,
  type RoleQueryParams,
  type RoleFormData,
  getRoleList,
  getRoleDetail,
  createRole,
  updateRole,
  deleteRole,
  getAllRoles,
  getRolePermissions,
  assignRolePermissions,
  getPermissionTree,
  getAllPermissions
} from './role'

// 量化模块 - 检查计划 (排除 PageResult)
export {
  PlanStatus,
  type CheckPlan,
  type PlanStatistics,
  type PlanQueryParams,
  type CreatePlanRequest,
  getCheckPlanList,
  getCheckPlanDetail,
  getCheckPlanStatistics,
  getTemplateSnapshot,
  getActivePlans,
  createCheckPlan,
  updateCheckPlan,
  deleteCheckPlan,
  updateCheckPlanStatus
} from './checkPlan'

// 量化模块 - 检查记录 (只导出唯一类型)
export {
  RecordStatus,
  type CheckRecord,
  type CheckRecordDetail,
  type MyClassDetail,
  type RecordQueryParams,
  type ClassStats,
  getCheckRecordList,
  getMyClassRecords,
  getRecordDetail,
  getMyClassDetail,
  getClassRanking,
  getClassHistory
} from './checkRecord'
// 从 checkRecord 导出唯一的类型
export type { DeductionItem, CategoryStats } from './checkRecord'

// 量化模块 - 检查模板 (排除重复的 PageResult 和 DeductionItem)
export {
  type CheckTemplate,
  type TemplateCategory,
  type ScoreRange,
  type TemplateQueryParams,
  getTemplateList,
  getAllTemplates,
  getEnabledTemplates,
  getTemplateById,
  getTemplateCategories,
  getCategoryItems,
  getTemplateWithDetails,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  updateTemplateStatus,
  copyTemplate
} from './checkTemplate'

// 量化模块 - 打分
export * from './scoring'

// 量化模块 - 申诉管理 (排除 PageResult)
export {
  AppealStatus,
  AppealType,
  type Appeal,
  type AppealStats,
  type AppealQueryParams,
  type CreateAppealRequest,
  type ReviewAppealRequest,
  getAppealList,
  getPendingAppeals,
  getPublicityAppeals,
  getAppealDetail,
  checkCanAppeal,
  createAppeal,
  reviewAppeal,
  withdrawAppeal,
  getAppealStatistics,
  getMyAppeals,
  getAppealSummary
} from './appeal'

// 量化模块 - 数据统计 (排除重复的 CategoryStats)
export {
  type StatisticsOverview,
  type RatingDistribution,
  type ClassRanking,
  type TrendData,
  type StatisticsQueryParams,
  getStatisticsOverview,
  getClassRankings,
  getDeductionTrend,
  getCheckCountTrend,
  getCategoryStatistics,
  getRatingDistribution,
  getClassStatistics,
  getStudentStatistics,
  exportStatisticsReport,
  getDashboardStats
} from './statistics'

// 公告管理 (排除 PageResult)
export {
  type Announcement,
  type AnnouncementQueryParams,
  type AnnouncementFormData,
  getAnnouncementList,
  getAnnouncementDetail,
  createAnnouncement,
  updateAnnouncement,
  deleteAnnouncement,
  publishAnnouncement,
  revokeAnnouncement,
  toggleTopAnnouncement,
  getLatestAnnouncements,
  getPublishedAnnouncements,
  incrementViewCount
} from './announcement'
