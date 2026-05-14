import type { LongId } from '@/types/common'
import { http } from '@/utils/request'
import type {
  TeachingTask,
  TeachingTaskQueryParams,
  CourseSchedule,
  ScheduleEntry,
  ScheduleAdjustment,
  AdjustmentQueryParams,
  ExamBatch,
  ExamArrangement,
  ExamBatchQueryParams,
  GradeBatch,
  StudentGrade,
  GradeQueryParams,
  GradeStatistics,
  PageResult,
  SemesterOffering,
  ClassCourseAssignment,
  TeachingClass,
  TeachingClassMember,
  SchedulingConstraint,
  TimeMatrix,
  DetectedConflict,
  FeasibilityReport,
} from '@/types/teaching'


const BASE_URL = '/teaching'

// ==================== 教学任务 ====================

export const teachingTaskApi = {
  list: (params?: TeachingTaskQueryParams) =>
    http.get<PageResult<TeachingTask>>(`${BASE_URL}/tasks`, { params }),

  getById: (id: LongId | string) => http.get<TeachingTask>(`${BASE_URL}/tasks/${id}`),

  create: (data: Partial<TeachingTask>) =>
    http.post<TeachingTask>(`${BASE_URL}/tasks`, data),

  update: (id: LongId | string, data: Partial<TeachingTask>) =>
    http.put<TeachingTask>(`${BASE_URL}/tasks/${id}`, data),

  delete: (id: LongId | string) => http.delete(`${BASE_URL}/tasks/${id}`),

  assignTeachers: (taskId: LongId | string, teachers: { teacherId: LongId | string; role: number; weeklyHours?: number }[]) =>
    http.post(`${BASE_URL}/tasks/${taskId}/assign-teachers`, { teachers }),

  removeTeacher: (taskId: LongId | string, teacherId: LongId | string) =>
    http.delete(`${BASE_URL}/tasks/${taskId}/teachers/${teacherId}`),

  batchCreate: (semesterId: LongId | string, planId: LongId | string, orgUnitIds: (number | string)[]) =>
    http.post<TeachingTask[]>(`${BASE_URL}/tasks/batch-create`, { semesterId, planId, orgUnitIds }),

  updateStatus: (id: LongId | string, taskStatus: number) =>
    http.patch(`${BASE_URL}/tasks/${id}/status`, { taskStatus }),
}

// ==================== 排课配置 & 就绪检查 ====================

export const scheduleConfigApi = {
  get: (semesterId: LongId | string) =>
    http.get<any>(`${BASE_URL}/schedule-config`, { params: { semesterId } }),

  save: (data: { semesterId: LongId | string; periodsPerDay: number; scheduleDays: number[]; periods: any[] }) =>
    http.put(`${BASE_URL}/schedule-config`, data),

  checkReadiness: (semesterId: LongId | string) =>
    http.get<any>(`${BASE_URL}/schedule-readiness`, { params: { semesterId } }),
}

// ==================== 实况课表 & 课时统计 ====================

export const instanceApi = {
  list: (params: { semesterId: LongId | string; date?: string; weekNumber?: number; teacherId?: LongId | string; orgUnitId?: LongId | string; classroomId?: LongId | string }) =>
    http.get<any[]>(`${BASE_URL}/instances`, { params }),

  generate: (semesterId: LongId | string) =>
    http.post<any>(`${BASE_URL}/instances/generate`, { semesterId }),

  applyEvent: (eventId: LongId | string) =>
    http.post<any>(`${BASE_URL}/instances/apply-event`, { eventId }),

  substitute: (id: LongId | string, teacherId: LongId | string, reason?: string) =>
    http.post(`${BASE_URL}/instances/${id}/substitute`, { teacherId, reason }),

  cancel: (id: LongId | string, reason?: string) =>
    http.post(`${BASE_URL}/instances/${id}/cancel`, { reason }),

  restore: (id: LongId | string) =>
    http.post(`${BASE_URL}/instances/${id}/restore`),
}

export const hoursApi = {
  getStatistics: (params: { semesterId: LongId | string; groupBy: string; period?: string; weekNumber?: number; month?: number }) =>
    http.get<any>(`${BASE_URL}/statistics/hours`, { params }),
}

// ==================== 作息表 ====================

export const periodConfigApi = {
  list: (semesterId: LongId | string) =>
    http.get<any[]>('/calendar/period-configs', { params: { semesterId } }),

  create: (data: any) =>
    http.post<any>('/calendar/period-configs', data),

  update: (id: LongId | string, data: any) =>
    http.put('/calendar/period-configs/' + id, data),

  delete: (id: LongId | string) =>
    http.delete('/calendar/period-configs/' + id),

  initFromPrevious: (semesterId: LongId | string) =>
    http.post<any>('/calendar/period-configs/init-from-previous', { semesterId }),
}

// ==================== 排课管理 ====================

export const scheduleApi = {
  // ---- 排课方案 (CourseSchedule / schedule-plans) ----
  list: (params?: { semesterId?: LongId | string; status?: number }) =>
    http.get<CourseSchedule[]>(`${BASE_URL}/schedule-plans`, { params }),

  getById: (id: LongId | string) => http.get<CourseSchedule>(`${BASE_URL}/schedule-plans/${id}`),

  create: (data: Partial<CourseSchedule>) =>
    http.post<CourseSchedule>(`${BASE_URL}/schedule-plans`, data),

  update: (id: LongId | string, data: Partial<CourseSchedule>) =>
    http.put<CourseSchedule>(`${BASE_URL}/schedule-plans/${id}`, data),

  delete: (id: LongId | string) => http.delete(`${BASE_URL}/schedule-plans/${id}`),

  publish: (id: LongId | string) =>
    http.post(`${BASE_URL}/schedule-plans/${id}/publish`),

  archive: (id: LongId | string) =>
    http.post(`${BASE_URL}/schedule-plans/${id}/archive`),

  // ---- 课表条目 (ScheduleEntry / schedules) ----
  getEntries: (scheduleId: LongId | string) =>
    http.get<ScheduleEntry[]>(`${BASE_URL}/schedules`, { params: { scheduleId } }),

  addEntry: (_scheduleId: LongId | string, data: Partial<ScheduleEntry>) =>
    http.post<ScheduleEntry>(`${BASE_URL}/schedules`, data),

  updateEntry: (_scheduleId: LongId | string, entryId: LongId | string, data: Partial<ScheduleEntry>) =>
    http.put<ScheduleEntry>(`${BASE_URL}/schedules/${entryId}`, data),

  deleteEntry: (_scheduleId: LongId | string, entryId: LongId | string) =>
    http.delete(`${BASE_URL}/schedules/${entryId}`),

  // ---- 智能排课 ----
  autoSchedule: (params: { semesterId: LongId | string; maxIterations?: number; populationSize?: number; scheduleId?: LongId | string; mutationRate?: number }) =>
    http.post<any>(`${BASE_URL}/schedules/auto-schedule`, params),

  // ---- 拖拽移动 / 冲突检测 ----
  moveEntry: (id: LongId | string, data: { semesterId: LongId | string; dayOfWeek: number; periodStart: number; classroomId?: LongId }) =>
    http.post(`${BASE_URL}/schedules/${id}/move`, data),

  checkMoveConflict: (data: { entryId: LongId; semesterId: LongId | string; dayOfWeek: number; periodStart: number }) =>
    http.post(`${BASE_URL}/schedules/check-move-conflict`, data),

  // ---- 按维度查询课表 ----
  getByClass: (orgUnitId: LongId | string, semesterId: LongId | string) =>
    http.get<ScheduleEntry[]>(`${BASE_URL}/schedules/by-class/${orgUnitId}`, { params: { semesterId } }),

  getByTeacher: (teacherId: LongId | string, semesterId: LongId | string) =>
    http.get<ScheduleEntry[]>(`${BASE_URL}/schedules/by-teacher/${teacherId}`, { params: { semesterId } }),

  getByClassroom: (classroomId: LongId | string, semesterId: LongId | string) =>
    http.get<ScheduleEntry[]>(`${BASE_URL}/schedules/by-classroom/${classroomId}`, { params: { semesterId } }),

  // ---- 导出 ----
  exportClassSchedule: (semesterId: LongId | string, orgUnitId: LongId | string) =>
    http.get(`${BASE_URL}/schedules/export/class/${orgUnitId}`, { params: { semesterId }, responseType: 'blob' }),

  exportTeacherSchedule: (semesterId: LongId | string, teacherId: LongId | string) =>
    http.get(`${BASE_URL}/schedules/export/teacher/${teacherId}`, { params: { semesterId }, responseType: 'blob' }),
}

// ==================== 自习课填充 ====================

export const selfStudyApi = {
  fill: (data: { semesterId: LongId | string; maxPeriods?: number; maxWeekday?: number; startWeek?: number; endWeek?: number }) =>
    http.post<{ inserted: number; classCount: number }>(`${BASE_URL}/self-study/fill`, data),

  clear: (semesterId: LongId | string) =>
    http.delete<{ cleared: number }>(`${BASE_URL}/self-study/clear`, { params: { semesterId } }),
}

// ==================== 调课管理 ====================

export const adjustmentApi = {
  list: (params?: AdjustmentQueryParams) =>
    http.get<PageResult<ScheduleAdjustment>>(`${BASE_URL}/adjustments`, { params }),

  getById: (id: LongId | string) => http.get<ScheduleAdjustment>(`${BASE_URL}/adjustments/${id}`),

  apply: (data: {
    entryId: LongId | string
    adjustmentType: number
    newClassroomId?: LongId | string
    newDayOfWeek?: number
    newPeriodStart?: number
    newPeriodEnd?: number
    newWeek?: number
    reason: string
  }) => http.post<ScheduleAdjustment>(`${BASE_URL}/adjustments`, data),

  approve: (id: LongId | string, remark?: string) =>
    http.post(`${BASE_URL}/adjustments/${id}/approve`, { remark }),

  reject: (id: LongId | string, remark: string) =>
    http.post(`${BASE_URL}/adjustments/${id}/reject`, { remark }),

  execute: (id: LongId | string) =>
    http.post(`${BASE_URL}/adjustments/${id}/execute`),

  cancel: (id: LongId | string) =>
    http.post(`${BASE_URL}/adjustments/${id}/cancel`),

  getMyApplications: (params?: { status?: number; page?: number; size?: number }) =>
    http.get<PageResult<ScheduleAdjustment>>(`${BASE_URL}/adjustments/my-applications`, { params }),

  getPendingApprovals: (params?: { page?: number; size?: number }) =>
    http.get<PageResult<ScheduleAdjustment>>(`${BASE_URL}/adjustments/pending-approvals`, { params }),
}

// ==================== 考试管理 ====================

export const examApi = {
  // 考试批次
  listBatches: (params?: ExamBatchQueryParams) =>
    http.get<PageResult<ExamBatch>>(`${BASE_URL}/examinations/batches`, { params }),

  getBatch: (id: LongId | string) => http.get<ExamBatch>(`${BASE_URL}/examinations/batches/${id}`),

  createBatch: (data: Partial<ExamBatch>) =>
    http.post<ExamBatch>(`${BASE_URL}/examinations/batches`, data),

  updateBatch: (id: LongId | string, data: Partial<ExamBatch>) =>
    http.put<ExamBatch>(`${BASE_URL}/examinations/batches/${id}`, data),

  deleteBatch: (id: LongId | string) => http.delete(`${BASE_URL}/examinations/batches/${id}`),

  publishBatch: (id: LongId | string) =>
    http.post(`${BASE_URL}/examinations/batches/${id}/publish`),

  // 冲突检测
  detectConflicts: (batchId: LongId | string) =>
    http.get<{ type: string; description: string; arrangement1Id: LongId; arrangement2Id: LongId }[]>(
      `${BASE_URL}/examinations/batches/${batchId}/conflicts`
    ),

  // 考试安排
  getArrangements: (batchId: LongId | string) =>
    http.get<ExamArrangement[]>(`${BASE_URL}/examinations/batches/${batchId}/arrangements`),

  createArrangement: (batchId: LongId | string, data: Partial<ExamArrangement>) =>
    http.post<ExamArrangement>(`${BASE_URL}/examinations/batches/${batchId}/arrangements`, data),

  updateArrangement: (batchId: LongId | string, arrangementId: LongId | string, data: Partial<ExamArrangement>) =>
    http.put<ExamArrangement>(`${BASE_URL}/examinations/batches/${batchId}/arrangements/${arrangementId}`, data),

  deleteArrangement: (batchId: LongId | string, arrangementId: LongId | string) =>
    http.delete(`${BASE_URL}/examinations/batches/${batchId}/arrangements/${arrangementId}`),

  // 考场分配
  assignRooms: (arrangementId: LongId | string, rooms: { classroomId: LongId | string; capacity: number }[]) =>
    http.post(`${BASE_URL}/examinations/arrangements/${arrangementId}/rooms`, { rooms }),

  // 监考教师
  assignInvigilators: (roomId: LongId | string, teacherIds: (number | string)[], mainTeacherId: LongId | string) =>
    http.post(`${BASE_URL}/examinations/rooms/${roomId}/invigilators`, { teacherIds, mainTeacherId }),

  // 查询教师监考安排
  getTeacherExams: (teacherId: LongId | string, semesterId: LongId | string) =>
    http.get<ExamArrangement[]>(`${BASE_URL}/examinations/by-teacher/${teacherId}`, { params: { semesterId } }),

  // 查询学生考试安排
  getStudentExams: (studentId: LongId | string, semesterId: LongId | string) =>
    http.get<ExamArrangement[]>(`${BASE_URL}/examinations/by-student/${studentId}`, { params: { semesterId } }),
}

// ==================== 成绩管理 ====================

export const gradeApi = {
  // 成绩批次
  listBatches: (params?: GradeQueryParams) =>
    http.get<PageResult<GradeBatch>>(`${BASE_URL}/grades/batches`, { params }),

  getBatch: (id: LongId | string) => http.get<GradeBatch>(`${BASE_URL}/grades/batches/${id}`),

  createBatch: (data: Partial<GradeBatch>) =>
    http.post<GradeBatch>(`${BASE_URL}/grades/batches`, data),

  updateBatch: (id: LongId | string, data: Partial<GradeBatch>) =>
    http.put<GradeBatch>(`${BASE_URL}/grades/batches/${id}`, data),

  deleteBatch: (id: LongId | string) => http.delete(`${BASE_URL}/grades/batches/${id}`),

  submitBatch: (id: LongId | string) =>
    http.post(`${BASE_URL}/grades/batches/${id}/submit`),

  approveBatch: (id: LongId | string) =>
    http.post(`${BASE_URL}/grades/batches/${id}/approve`),

  publishBatch: (id: LongId | string) =>
    http.post(`${BASE_URL}/grades/batches/${id}/publish`),

  // 成绩录入
  getGrades: (batchId: LongId | string) =>
    http.get<StudentGrade[]>(`${BASE_URL}/grades/batches/${batchId}/grades`),

  recordGrade: (batchId: LongId | string, data: {
    studentId: LongId | string
    totalScore?: number
    items?: { itemName: string; score: number; weight: number }[]
    remark?: string
  }) => http.post<StudentGrade>(`${BASE_URL}/grades/batches/${batchId}/grades`, data),

  updateGrade: (gradeId: LongId | string, data: {
    totalScore?: number
    items?: { itemName: string; score: number; weight: number }[]
    remark?: string
  }) => http.put<StudentGrade>(`${BASE_URL}/grades/${gradeId}`, data),

  batchRecordGrades: (batchId: LongId | string, grades: {
    studentId: LongId | string
    totalScore: number
    remark?: string
  }[]) => http.post(`${BASE_URL}/grades/batches/${batchId}/batch-record`, { grades }),

  // 成绩查询
  getStudentGrades: (studentId: LongId | string, params?: { semesterId?: LongId | string; courseId?: LongId | string }) =>
    http.get<StudentGrade[]>(`${BASE_URL}/grades/by-student/${studentId}`, { params }),

  getClassGrades: (orgUnitId: LongId | string, params?: { semesterId?: LongId | string; courseId?: LongId | string }) =>
    http.get<StudentGrade[]>(`${BASE_URL}/grades/by-class/${orgUnitId}`, { params }),

  // 成绩统计
  getStatistics: (params: { batchId?: LongId | string; orgUnitId?: LongId | string; courseId?: LongId | string; semesterId?: LongId | string }) =>
    http.get<GradeStatistics>(`${BASE_URL}/grades/statistics`, { params }),

  // 成绩排名
  getRanking: (params: { orgUnitId: LongId | string; semesterId: LongId | string; courseId?: LongId | string }) =>
    http.get<{ studentId: LongId | string; studentName: string; totalScore: number; rank: number }[]>(
      `${BASE_URL}/grades/ranking`, { params }
    ),

  // 导出成绩（按批次）
  exportGrades: (batchId: LongId | string) =>
    http.get(`${BASE_URL}/grades/batches/${batchId}/export`, { responseType: 'blob' }),

  // 导出成绩（按学期/班级/课程筛选）
  exportGradesByFilter: (params: { semesterId: LongId | string; orgUnitId?: LongId; courseId?: LongId }) =>
    http.get(`${BASE_URL}/grades/export`, { params, responseType: 'blob' }),

  // 导入成绩模板
  getImportTemplate: (batchId: LongId | string) =>
    http.get(`${BASE_URL}/grades/batches/${batchId}/import-template`, { responseType: 'blob' }),

  // 导入成绩
  importGrades: (batchId: LongId | string, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post(`${BASE_URL}/grades/batches/${batchId}/import`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  // 加权配置
  getWeightConfigs: (semesterId: LongId, courseId: LongId) =>
    http.get<{ componentType: number; weightPercent: number }[]>(`${BASE_URL}/grades/weight-configs`, { params: { semesterId, courseId } }),

  saveWeightConfigs: (data: { semesterId: LongId; courseId: LongId; configs: Array<{ componentType: number; weightPercent: number }> }) =>
    http.put(`${BASE_URL}/grades/weight-configs`, data),

  calculateOverall: (data: { semesterId: LongId; courseId: LongId }) =>
    http.post<{ batchId: LongId; calculated: number; skipped: number }>(`${BASE_URL}/grades/calculate-overall`, data),
}

// =====================================================
// 开课管理 API
// =====================================================

export const offeringApi = {
  list: (semesterId: LongId | string) =>
    http.get<SemesterOffering[]>(`${BASE_URL}/offerings`, { params: { semesterId } }),
  create: (data: Partial<SemesterOffering>) =>
    http.post<SemesterOffering>(`${BASE_URL}/offerings`, data),
  update: (id: LongId | string, data: Partial<SemesterOffering>) =>
    http.put<SemesterOffering>(`${BASE_URL}/offerings/${id}`, data),
  delete: (id: LongId | string) =>
    http.delete(`${BASE_URL}/offerings/${id}`),
  confirm: (id: LongId | string) =>
    http.post(`${BASE_URL}/offerings/${id}/confirm`),
  importFromPlan: (data: { semesterId: LongId; planId: LongId; orgUnitIds?: LongId[] }) =>
    http.post(`${BASE_URL}/offerings/import-from-plan`, data),
  generateTasks: (semesterId: LongId | string) =>
    http.post(`${BASE_URL}/offerings/generate-tasks`, { semesterId }),
}

export const classAssignmentApi = {
  list: (semesterId: LongId | string, orgUnitId?: LongId | string) =>
    http.get<ClassCourseAssignment[]>(`${BASE_URL}/class-assignments`, { params: { semesterId, orgUnitId } }),
  create: (data: Partial<ClassCourseAssignment>) =>
    http.post<ClassCourseAssignment>(`${BASE_URL}/class-assignments`, data),
  delete: (id: LongId | string) =>
    http.delete(`${BASE_URL}/class-assignments/${id}`),
  batchConfirm: (semesterId: LongId | string, orgUnitId: LongId | string) =>
    http.post(`${BASE_URL}/class-assignments/batch-confirm`, { semesterId, orgUnitId }),
}

// =====================================================
// 教学班 API
// =====================================================

export const teachingClassApi = {
  list: (semesterId: LongId | string) =>
    http.get<TeachingClass[]>(`${BASE_URL}/teaching-classes`, { params: { semesterId } }),
  getById: (id: LongId | string) =>
    http.get<TeachingClass>(`${BASE_URL}/teaching-classes/${id}`),
  create: (data: Partial<TeachingClass>) =>
    http.post<TeachingClass>(`${BASE_URL}/teaching-classes`, data),
  update: (id: LongId | string, data: Partial<TeachingClass>) =>
    http.put<TeachingClass>(`${BASE_URL}/teaching-classes/${id}`, data),
  delete: (id: LongId | string) =>
    http.delete(`${BASE_URL}/teaching-classes/${id}`),
  autoGenerate: (semesterId: LongId | string) =>
    http.post(`${BASE_URL}/teaching-classes/auto-generate`, { semesterId }),
  getMembers: (id: LongId | string) =>
    http.get<TeachingClassMember[]>(`${BASE_URL}/teaching-classes/${id}/members`),
  addMembers: (id: LongId | string, members: Partial<TeachingClassMember>[]) =>
    http.post(`${BASE_URL}/teaching-classes/${id}/members`, members),
  removeMembers: (id: LongId | string, memberIds: number[]) =>
    http.delete(`${BASE_URL}/teaching-classes/${id}/members`, { data: memberIds }),
}

// =====================================================
// 约束管理 API
// =====================================================

export const constraintApi = {
  list: (params: { semesterId: LongId | string; level?: number; targetId?: LongId | string }) =>
    http.get<SchedulingConstraint[]>(`${BASE_URL}/constraints`, { params }),
  create: (data: Partial<SchedulingConstraint>) =>
    http.post<SchedulingConstraint>(`${BASE_URL}/constraints`, data),
  update: (id: LongId | string, data: Partial<SchedulingConstraint>) =>
    http.put<SchedulingConstraint>(`${BASE_URL}/constraints/${id}`, data),
  delete: (id: LongId | string) =>
    http.delete(`${BASE_URL}/constraints/${id}`),
  enable: (id: LongId | string) =>
    http.post(`${BASE_URL}/constraints/${id}/enable`),
  disable: (id: LongId | string) =>
    http.post(`${BASE_URL}/constraints/${id}/disable`),
  getTimeMatrix: (params: { semesterId: LongId | string; level: number; targetId?: LongId | string }) =>
    http.get<TimeMatrix>(`${BASE_URL}/constraints/time-matrix`, { params }),
  batchImport: (semesterId: LongId | string, constraints: Partial<SchedulingConstraint>[]) =>
    http.post(`${BASE_URL}/constraints/batch-import`, { semesterId, constraints }),
}

// =====================================================
// 冲突检测 API
// =====================================================

export const conflictApi = {
  feasibilityCheck: (semesterId: LongId | string) =>
    http.post<FeasibilityReport>(`${BASE_URL}/conflicts/feasibility-check`, null, { params: { semesterId } }),
  detect: (semesterId: LongId | string) =>
    http.post<DetectedConflict[]>(`${BASE_URL}/conflicts/detect`, null, { params: { semesterId } }),
  list: (params: { semesterId: LongId | string; status?: number }) =>
    http.get<DetectedConflict[]>(`${BASE_URL}/conflicts`, { params }),
  resolve: (id: LongId | string, note: string) =>
    http.post(`${BASE_URL}/conflicts/${id}/resolve`, { note }),
  ignore: (id: LongId | string, note: string) =>
    http.post(`${BASE_URL}/conflicts/${id}/ignore`, { note }),
}

// ==================== 教务工作流 ====================

export const workflowApi = {
  /** 流水线统计 */
  getStats: (semesterId: LongId | string) =>
    http.get('/teaching/workflow/stats', { params: { semesterId } }),

  /** 自动生成年级-学期映射 */
  generateMappings: (semesterId: LongId | string) =>
    http.post<{ generated: number }>(`${BASE_URL}/workflow/cohort-mappings/generate`, { semesterId }),

  /** 查看年级-学期映射 */
  getMappings: (semesterId: LongId | string) =>
    http.get<any[]>(`${BASE_URL}/workflow/cohort-mappings`, { params: { semesterId } }),

  /** Step 1: 从培养方案导入开课计划 */
  generateOfferings: (semesterId: LongId | string) =>
    http.post<{ generated: number }>(`${BASE_URL}/workflow/generate-offerings`, { semesterId }),

  /** Step 2: 从开课批量生成教学任务 */
  generateTasks: (semesterId: LongId | string) =>
    http.post<{ generated: number }>(`${BASE_URL}/workflow/generate-tasks`, { semesterId }),

  /** Step 3: 从教学任务创建考试安排 */
  generateExams: (batchId: LongId | string, taskIds: (number | string)[]) =>
    http.post<{ generated: number }>(`${BASE_URL}/workflow/generate-exams`, { batchId, taskIds }),

  /** Step 4: 从考试批次创建成绩批次 */
  generateGradeBatch: (examBatchId: LongId | string) =>
    http.post<{ gradeBatchId: LongId }>(`${BASE_URL}/workflow/generate-grade-batch`, { examBatchId }),

  /** 一键学期初始化 (映射 + 开课 + 任务) */
  initializeSemester: (semesterId: LongId | string) =>
    http.post<{ mappings: number; offerings: number; tasks: number }>(`${BASE_URL}/workflow/initialize-semester`, { semesterId }),
}
