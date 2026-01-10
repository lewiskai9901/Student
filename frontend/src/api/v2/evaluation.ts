/**
 * 综测评价 API - DDD架构适配
 *
 * 整合综测周期、荣誉类型、课程成绩、行为记录等
 */
import { http } from '@/utils/request'
import type { PageResult } from '@/types'

// ==================== 类型定义 ====================

/** 综测周期 */
export interface EvaluationPeriod {
  id?: number
  periodCode: string
  periodName: string
  semesterId: number
  academicYear?: string
  semesterType?: number
  status?: number
  dataCollectionStartTime?: string
  dataCollectionEndTime?: string
  applicationStartTime?: string
  applicationEndTime?: string
  reviewStartTime?: string
  reviewEndTime?: string
  publicityStartTime?: string
  publicityEndTime?: string
  isLocked?: number
  lockedAt?: string
  lockedBy?: number
  description?: string
  // 关联信息
  semesterName?: string
  statusText?: string
}

/** 荣誉类型 */
export interface HonorType {
  id?: number
  typeCode: string
  typeName: string
  category: string // COMPETITION/CERTIFICATE/TITLE/ACTIVITY/PUBLICATION/OTHER
  evaluationDimension: string // 影响维度
  description?: string
  requiredAttachments?: number // 是否必须上传附件: 1需要, 0不需要
  sortOrder?: number
  status?: number
  createdBy?: number
  createdAt?: string
  updatedBy?: number
  updatedAt?: string
  // 扩展信息
  levelConfigs?: HonorLevelConfig[]
}

/** 荣誉等级配置 */
export interface HonorLevelConfig {
  id?: number
  honorTypeId?: number
  levelCode: string // NATIONAL/PROVINCIAL/CITY/SCHOOL/DEPARTMENT
  levelName: string
  rankCode: string // FIRST/SECOND/THIRD/EXCELLENCE/PARTICIPATION
  rankName: string
  score: number
  maxCount?: number
  priority?: number
  sortOrder?: number
  status?: number
}

/** 学生荣誉申报 */
export interface HonorApplication {
  id?: number
  periodId: number
  studentId: number
  honorTypeId: number
  honorName: string
  honorLevel?: string
  achieveTime?: string
  description?: string
  materialUrls?: string
  bonusScore?: number
  affectDimension?: string
  status?: number
  applyTime?: string
  // 审核信息
  classReviewerId?: number
  classReviewTime?: string
  classReviewComment?: string
  deptReviewerId?: number
  deptReviewTime?: string
  deptReviewComment?: string
  schoolReviewerId?: number
  schoolReviewTime?: string
  schoolReviewComment?: string
  // 关联信息
  studentNo?: string
  studentName?: string
  className?: string
  departmentName?: string
  honorTypeName?: string
  periodName?: string
}

/** 课程 */
export interface Course {
  id?: number
  courseCode: string
  courseName: string
  courseType?: string
  credit?: number
  hours?: number
  semesterId?: number
  departmentId?: number
  teacherName?: string
  description?: string
  status?: number
  // 关联信息
  departmentName?: string
  semesterName?: string
}

/** 学生成绩 */
export interface StudentScore {
  id?: number
  studentId: number
  courseId: number
  semesterId: number
  usualScore?: number
  midtermScore?: number
  finalScore?: number
  totalScore?: number
  credit?: number
  gpa?: number
  status?: number
  isLocked?: number
  remark?: string
  // 关联信息
  studentNo?: string
  studentName?: string
  className?: string
  courseCode?: string
  courseName?: string
  semesterName?: string
}

/** 综测结果 */
export interface EvaluationResult {
  id?: number
  periodId: number
  studentId: number
  moralScore?: number
  intellectualScore?: number
  physicalScore?: number
  aestheticScore?: number
  laborScore?: number
  developmentScore?: number
  totalScore?: number
  classRank?: number
  gradeRank?: number
  status?: number
  // 关联信息
  studentNo?: string
  studentName?: string
  className?: string
  periodName?: string
}

/** 行为类型 */
export interface BehaviorType {
  id?: number
  behaviorCode: string
  behaviorName: string
  behaviorCategory: string // ATTENDANCE/DISCIPLINE/HYGIENE/STUDY/ACTIVITY/HONOR
  behaviorNature: number // 1正向, 2负向, 3中性
  defaultAffectScope: number // 1仅当事人, 2宿舍全员, 3班级全员
  description?: string
  sortOrder?: number
  status?: number
  createdBy?: number
  createdAt?: string
  updatedBy?: number
  updatedAt?: string
  // 额外显示字段
  effectCount?: number
}

/** 学期 */
export interface Semester {
  id?: number
  semesterCode: string
  semesterName: string
  academicYear: string
  semesterType: number // 1-第一学期, 2-第二学期
  startDate?: string
  endDate?: string
  isCurrent?: number
  status?: number
  description?: string
  createTime?: string
  updateTime?: string
}

// ==================== 综测周期接口 ====================

/** 分页查询综测周期 */
export function pagePeriods(params: {
  pageNum?: number
  pageSize?: number
  periodCode?: string
  periodName?: string
  academicYear?: string
  status?: number
}) {
  return http.get<PageResult<EvaluationPeriod>>('/evaluation/periods', { params })
}

/** 获取当前进行中的周期 */
export function getCurrentPeriod() {
  return http.get<EvaluationPeriod>('/evaluation/periods/current')
}

/** 获取周期详情 */
export function getPeriodDetail(id: number) {
  return http.get<EvaluationPeriod>(`/evaluation/periods/${id}`)
}

/** 创建综测周期 */
export function createPeriod(data: EvaluationPeriod) {
  return http.post<number>('/evaluation/periods', data)
}

/** 更新综测周期 */
export function updatePeriod(id: number, data: EvaluationPeriod) {
  return http.put<void>(`/evaluation/periods/${id}`, data)
}

/** 删除综测周期 */
export function deletePeriod(id: number) {
  return http.delete<void>(`/evaluation/periods/${id}`)
}

/** 开始数据采集 */
export function startDataCollection(id: number) {
  return http.post<void>(`/evaluation/periods/${id}/start-data-collection`)
}

/** 开始荣誉申报 */
export function startApplication(id: number) {
  return http.post<void>(`/evaluation/periods/${id}/start-application`)
}

/** 开始审核 */
export function startReview(id: number) {
  return http.post<void>(`/evaluation/periods/${id}/start-review`)
}

/** 开始公示 */
export function startPublicity(id: number) {
  return http.post<void>(`/evaluation/periods/${id}/start-publicity`)
}

/** 结束周期 */
export function finishPeriod(id: number) {
  return http.post<void>(`/evaluation/periods/${id}/finish`)
}

/** 锁定周期 */
export function lockPeriod(id: number) {
  return http.post<void>(`/evaluation/periods/${id}/lock`)
}

/** 解锁周期 */
export function unlockPeriod(id: number) {
  return http.post<void>(`/evaluation/periods/${id}/unlock`)
}

/** 根据学年获取周期列表 */
export function getPeriodsByAcademicYear(academicYear: string) {
  return http.get<EvaluationPeriod[]>(`/evaluation/periods/academic-year/${academicYear}`)
}

// ==================== 荣誉申报接口 ====================

/** 分页查询荣誉申报 */
export function pageHonorApplications(params: {
  pageNum?: number
  pageSize?: number
  periodId?: number
  studentName?: string
  studentNo?: string
  honorTypeId?: number
  status?: number
  classId?: number
  departmentId?: number
}) {
  return http.get<PageResult<HonorApplication>>('/evaluation/honor-applications', { params })
}

/** 获取申报详情 */
export function getHonorApplicationDetail(id: number) {
  return http.get<HonorApplication>(`/evaluation/honor-applications/${id}`)
}

/** 提交荣誉申报 */
export function submitHonorApplication(data: HonorApplication) {
  return http.post<number>('/evaluation/honor-applications', data)
}

/** 更新荣誉申报 */
export function updateHonorApplication(id: number, data: HonorApplication) {
  return http.put<void>(`/evaluation/honor-applications/${id}`, data)
}

/** 撤回申报 */
export function withdrawHonorApplication(id: number) {
  return http.post<void>(`/evaluation/honor-applications/${id}/withdraw`)
}

/** 班级审核 */
export function classReviewHonorApplication(id: number, approved: boolean, comment?: string) {
  return http.post<void>(`/evaluation/honor-applications/${id}/class-review`, {
    approved,
    comment
  })
}

/** 院系审核 */
export function departmentReviewHonorApplication(id: number, approved: boolean, comment?: string) {
  return http.post<void>(`/evaluation/honor-applications/${id}/department-review`, {
    approved,
    comment
  })
}

/** 学校审核 */
export function schoolReviewHonorApplication(id: number, approved: boolean, comment?: string) {
  return http.post<void>(`/evaluation/honor-applications/${id}/school-review`, {
    approved,
    comment
  })
}

/** 批量审核 */
export function batchReviewHonorApplications(
  ids: number[],
  level: 'class' | 'department' | 'school',
  approved: boolean,
  comment?: string
) {
  return http.post<number>('/evaluation/honor-applications/batch-review', {
    ids,
    level,
    approved,
    comment
  })
}

/** 获取学生申报列表 */
export function getStudentHonorApplications(studentId: number, periodId?: number) {
  return http.get<HonorApplication[]>(`/evaluation/honor-applications/student/${studentId}`, {
    params: { periodId }
  })
}

/** 获取我的申报列表 */
export function getMyHonorApplications(periodId?: number) {
  return http.get<HonorApplication[]>('/evaluation/honor-applications/my', {
    params: { periodId }
  })
}

/** 获取待审核列表 */
export function getPendingReviewList(level: 'class' | 'department' | 'school') {
  return http.get<HonorApplication[]>('/evaluation/honor-applications/pending-review', {
    params: { level }
  })
}

/** 检查是否可申报 */
export function checkCanApplyHonor(studentId: number, honorTypeId: number, periodId: number) {
  return http.get<{ canApply: boolean; reason: string }>(
    '/evaluation/honor-applications/check-can-apply',
    {
      params: { studentId, honorTypeId, periodId }
    }
  )
}

/** 获取可申报的荣誉类型 */
export function getAvailableHonorTypes(studentId: number, periodId: number) {
  return http.get<HonorType[]>('/evaluation/honor-applications/available-honor-types', {
    params: { studentId, periodId }
  })
}

/** 获取审核统计 */
export function getHonorReviewStatistics(periodId: number) {
  return http.get<Record<string, number>>(`/evaluation/honor-applications/statistics/${periodId}`)
}

// ==================== 课程接口 ====================

/** 分页查询课程 */
export function pageCourses(params: {
  pageNum?: number
  pageSize?: number
  courseCode?: string
  courseName?: string
  courseType?: string
  semesterId?: number
  departmentId?: number
  status?: number
}) {
  return http.get<PageResult<Course>>('/evaluation/courses', { params })
}

/** 获取课程详情 */
export function getCourseDetail(id: number) {
  return http.get<Course>(`/evaluation/courses/${id}`)
}

/** 创建课程 */
export function createCourse(data: Course) {
  return http.post<number>('/evaluation/courses', data)
}

/** 更新课程 */
export function updateCourse(id: number, data: Course) {
  return http.put<void>(`/evaluation/courses/${id}`, data)
}

/** 删除课程 */
export function deleteCourse(id: number) {
  return http.delete<void>(`/evaluation/courses/${id}`)
}

/** 根据学期获取课程列表 */
export function getCoursesBySemester(semesterId: number) {
  return http.get<Course[]>(`/evaluation/courses/semester/${semesterId}`)
}

/** 根据班级获取课程列表 */
export function getCoursesByClass(classId: number, semesterId?: number) {
  return http.get<Course[]>(`/evaluation/courses/class/${classId}`, {
    params: { semesterId }
  })
}

/** 获取课程类型列表 */
export function getCourseTypes() {
  return http.get<{ value: string; label: string }[]>('/evaluation/courses/types')
}

// ==================== 成绩接口 ====================

/** 分页查询成绩 */
export function pageScores(params: {
  pageNum?: number
  pageSize?: number
  studentName?: string
  studentNo?: string
  courseId?: number
  semesterId?: number
  classId?: number
  status?: number
}) {
  return http.get<PageResult<StudentScore>>('/evaluation/scores', { params })
}

/** 获取成绩详情 */
export function getScoreDetail(id: number) {
  return http.get<StudentScore>(`/evaluation/scores/${id}`)
}

/** 录入成绩 */
export function inputScore(data: StudentScore) {
  return http.post<number>('/evaluation/scores', data)
}

/** 批量录入成绩 */
export function batchInputScores(scores: StudentScore[]) {
  return http.post<number>('/evaluation/scores/batch', scores)
}

/** 更新成绩 */
export function updateScore(id: number, data: StudentScore) {
  return http.put<void>(`/evaluation/scores/${id}`, data)
}

/** 删除成绩 */
export function deleteScore(id: number) {
  return http.delete<void>(`/evaluation/scores/${id}`)
}

/** 获取学生学期成绩 */
export function getStudentScores(studentId: number, semesterId: number) {
  return http.get<StudentScore[]>(`/evaluation/scores/student/${studentId}`, {
    params: { semesterId }
  })
}

/** 获取我的成绩 */
export function getMyScores(semesterId?: number) {
  return http.get<StudentScore[]>('/evaluation/scores/my', {
    params: { semesterId }
  })
}

/** 计算学生GPA */
export function calculateStudentGPA(studentId: number, semesterId: number) {
  return http.get<{ gpa: number; average: number; weightedAverage: number }>(
    `/evaluation/scores/student/${studentId}/gpa`,
    { params: { semesterId } }
  )
}

/** 获取班级成绩统计 */
export function getClassScoreStatistics(classId: number, semesterId: number, courseId?: number) {
  return http.get<Record<string, number>>(`/evaluation/scores/statistics/class/${classId}`, {
    params: { semesterId, courseId }
  })
}

/** 获取课程成绩排名 */
export function getCourseScoreRanking(courseId: number, semesterId: number, limit?: number) {
  return http.get<StudentScore[]>(`/evaluation/scores/ranking/course/${courseId}`, {
    params: { semesterId, limit }
  })
}

/** 锁定成绩 */
export function lockScores(semesterId: number, courseId?: number) {
  return http.post<void>('/evaluation/scores/lock', null, {
    params: { semesterId, courseId }
  })
}

/** 解锁成绩 */
export function unlockScores(semesterId: number, courseId?: number) {
  return http.post<void>('/evaluation/scores/unlock', null, {
    params: { semesterId, courseId }
  })
}

// ==================== 综测结果接口 ====================

/** 分页查询综测结果 */
export function pageEvaluationResults(params: {
  pageNum?: number
  pageSize?: number
  periodId?: number
  studentName?: string
  studentNo?: string
  classId?: number
  departmentId?: number
}) {
  return http.get<PageResult<EvaluationResult>>('/evaluation/results', { params })
}

/** 获取综测结果详情 */
export function getEvaluationResultDetail(id: number) {
  return http.get<EvaluationResult>(`/evaluation/results/${id}`)
}

/** 获取学生综测结果 */
export function getStudentEvaluationResult(studentId: number, periodId: number) {
  return http.get<EvaluationResult>('/evaluation/results/student', {
    params: { studentId, periodId }
  })
}

/** 获取我的综测结果 */
export function getMyEvaluationResult(periodId: number) {
  return http.get<EvaluationResult>('/evaluation/results/my', {
    params: { periodId }
  })
}

/** 计算学生综测 */
export function calculateStudentEvaluation(periodId: number, studentId: number) {
  return http.post<EvaluationResult>('/evaluation/results/calculate/student', null, {
    params: { periodId, studentId }
  })
}

/** 计算班级综测 */
export function calculateClassEvaluation(periodId: number, classId: number) {
  return http.post<EvaluationResult[]>('/evaluation/results/calculate/class', null, {
    params: { periodId, classId }
  })
}

/** 计算年级综测 */
export function calculateGradeEvaluation(periodId: number, gradeId: number) {
  return http.post<number>('/evaluation/results/calculate/grade', null, {
    params: { periodId, gradeId }
  })
}

/** 重新计算 */
export function recalculateEvaluation(resultId: number) {
  return http.post<EvaluationResult>(`/evaluation/results/${resultId}/recalculate`)
}

/** 计算排名 */
export function calculateRankings(periodId: number) {
  return http.post<void>('/evaluation/results/calculate-rankings', null, {
    params: { periodId }
  })
}

/** 获取班级综测排名 */
export function getClassEvaluationRanking(periodId: number, classId: number) {
  return http.get<EvaluationResult[]>('/evaluation/results/ranking/class', {
    params: { periodId, classId }
  })
}

/** 获取年级综测排名 */
export function getGradeEvaluationRanking(periodId: number, gradeId: number, limit?: number) {
  return http.get<EvaluationResult[]>('/evaluation/results/ranking/grade', {
    params: { periodId, gradeId, limit }
  })
}

// ==================== 行为类型接口 ====================

/** 分页查询行为类型 */
export function pageBehaviorTypes(params: {
  pageNum?: number
  pageSize?: number
  behaviorCode?: string
  behaviorName?: string
  behaviorCategory?: string
  behaviorNature?: number
  status?: number
}) {
  return http.get<PageResult<BehaviorType>>('/evaluation/behavior-types', { params })
}

/** 获取行为类型详情 */
export function getBehaviorTypeDetail(id: number) {
  return http.get<BehaviorType>(`/evaluation/behavior-types/${id}`)
}

/** 创建行为类型 */
export function createBehaviorType(data: BehaviorType) {
  return http.post<number>('/evaluation/behavior-types', data)
}

/** 更新行为类型 */
export function updateBehaviorType(id: number, data: BehaviorType) {
  return http.put<void>(`/evaluation/behavior-types/${id}`, data)
}

/** 删除行为类型 */
export function deleteBehaviorType(id: number) {
  return http.delete<void>(`/evaluation/behavior-types/${id}`)
}

/** 根据类别获取行为类型 */
export function getBehaviorTypesByCategory(category: string) {
  return http.get<BehaviorType[]>(`/evaluation/behavior-types/category/${category}`)
}

/** 获取行为类型树 */
export function getBehaviorTypeTree() {
  return http.get<BehaviorType[]>('/evaluation/behavior-types/tree')
}

/** 获取所有行为类别 */
export function getAllBehaviorCategories() {
  return http.get<{ value: string; label: string }[]>('/evaluation/behavior-types/categories')
}

// ==================== 学期接口 ====================

/** 分页查询学期 */
export function pageSemesters(params: {
  pageNum?: number
  pageSize?: number
  semesterCode?: string
  semesterName?: string
  academicYear?: string
  semesterType?: number
  status?: number
}) {
  return http.get<PageResult<Semester>>('/evaluation/semesters', { params })
}

/** 获取当前学期 */
export function getCurrentSemester() {
  return http.get<Semester>('/evaluation/semesters/current')
}

/** 获取学期详情 */
export function getSemesterDetail(id: number) {
  return http.get<Semester>(`/evaluation/semesters/${id}`)
}

/** 创建学期 */
export function createSemester(data: Semester) {
  return http.post<number>('/evaluation/semesters', data)
}

/** 更新学期 */
export function updateSemester(id: number, data: Semester) {
  return http.put<void>(`/evaluation/semesters/${id}`, data)
}

/** 删除学期 */
export function deleteSemester(id: number) {
  return http.delete<void>(`/evaluation/semesters/${id}`)
}

/** 根据学年获取学期列表 */
export function getSemestersByAcademicYear(academicYear: string) {
  return http.get<Semester[]>(`/evaluation/semesters/academic-year/${academicYear}`)
}

/** 获取所有学年列表 */
export function getAllAcademicYears() {
  return http.get<string[]>('/evaluation/semesters/academic-years')
}

/** 设置为当前学期 */
export function setCurrentSemester(id: number) {
  return http.post<void>(`/evaluation/semesters/${id}/set-current`)
}

/** 获取所有启用的学期列表（下拉选择） */
export function listAllSemesters() {
  return http.get<Semester[]>('/evaluation/semesters/list')
}

// ==================== 荣誉类型接口 ====================

/** 分页查询荣誉类型 */
export function pageHonorTypes(params: {
  pageNum?: number
  pageSize?: number
  typeCode?: string
  typeName?: string
  category?: string
  status?: number
}) {
  return http.get<PageResult<HonorType>>('/evaluation/honor-types', { params })
}

/** 获取荣誉类型详情 */
export function getHonorTypeDetail(id: number) {
  return http.get<HonorType>(`/evaluation/honor-types/${id}`)
}

/** 创建荣誉类型 */
export function createHonorType(data: HonorType) {
  return http.post<number>('/evaluation/honor-types', data)
}

/** 更新荣誉类型 */
export function updateHonorType(id: number, data: HonorType) {
  return http.put<void>(`/evaluation/honor-types/${id}`, data)
}

/** 删除荣誉类型 */
export function deleteHonorType(id: number) {
  return http.delete<void>(`/evaluation/honor-types/${id}`)
}

/** 根据类别获取荣誉类型 */
export function getHonorTypesByCategory(category: string) {
  return http.get<HonorType[]>(`/evaluation/honor-types/category/${category}`)
}

/** 获取所有启用的荣誉类型 */
export function listAllHonorTypes() {
  return http.get<HonorType[]>('/evaluation/honor-types/list')
}

// ==================== 综测维度配置接口 ====================

/** 综测维度配置 */
export interface EvaluationDimension {
  id?: number
  dimensionCode: string // MORAL/INTELLECTUAL/PHYSICAL/AESTHETIC/LABOR/DEVELOPMENT
  dimensionName: string
  weight: number // 权重百分比
  baseScore: number // 基础分
  maxBonusScore: number // 奖励分上限
  minTotalScore: number // 维度最低分
  maxTotalScore: number // 维度最高分
  calculationFormula?: string // 计算公式说明
  sortOrder?: number
  status?: number
  createdAt?: string
  updatedAt?: string
}

/** 获取所有启用的维度配置 */
export function listAllDimensions() {
  return http.get<EvaluationDimension[]>('/evaluation/dimensions')
}

/** 获取维度配置详情 */
export function getDimensionByCode(code: string) {
  return http.get<EvaluationDimension>(`/evaluation/dimensions/${code}`)
}

/** 更新维度配置 */
export function updateDimension(id: number, data: EvaluationDimension) {
  return http.put<void>(`/evaluation/dimensions/${id}`, data)
}
