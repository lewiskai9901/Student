import { http } from '@/utils/request'
import type {
  TeachingTask,
  TeachingTaskQueryParams,
  CourseSchedule,
  ScheduleEntry,
  AutoScheduleParams,
  AutoScheduleResult,
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
  Classroom,
  TeachingBuilding,
  ClassroomQueryParams,
} from '@/types/teaching'


const BASE_URL = '/teaching'

// ==================== 教学楼管理 ====================

export interface TeachingBuildingWithStats extends TeachingBuilding {
  totalClassrooms?: number
  usedClassrooms?: number
  totalCapacity?: number
}

export const getTeachingBuildings = async (): Promise<TeachingBuildingWithStats[]> => {
  // 使用 /teaching/buildings/enabled 获取所有启用的教学楼
  return http.get<TeachingBuildingWithStats[]>('/teaching/buildings/enabled', {
    params: { buildingType: 1 } // buildingType=1 for teaching buildings
  })
}

export const getTeachingBuildingById = (id: number | string): Promise<TeachingBuildingWithStats> => {
  return http.get(`/teaching/buildings/${id}`)
}

// ==================== 教室管理 ====================

export interface ClassroomWithDetails extends Classroom {
  buildingNo?: string
  usageRate?: number
}

export const getClassrooms = async (params?: ClassroomQueryParams): Promise<{ records: ClassroomWithDetails[]; total: number }> => {
  return http.get('/teaching/classrooms', { params })
}

export const getClassroomsByBuilding = async (buildingId: number | string): Promise<ClassroomWithDetails[]> => {
  const res = await http.get<{ records: ClassroomWithDetails[]; total: number }>('/teaching/classrooms', {
    params: { buildingId, pageNum: 1, pageSize: 500 }
  })
  return res.records || []
}

export const getClassroomById = (id: number | string): Promise<ClassroomWithDetails> => {
  return http.get(`/teaching/classrooms/${id}`)
}

export const createClassroom = (data: Partial<Classroom>): Promise<Classroom> => {
  return http.post('/teaching/classrooms', data)
}

export const updateClassroom = (id: number | string, data: Partial<Classroom>): Promise<Classroom> => {
  return http.put(`/teaching/classrooms/${id}`, data)
}

export const deleteClassroom = (id: number | string): Promise<void> => {
  return http.delete(`/teaching/classrooms/${id}`)
}

export const assignClassToClassroom = (classroomId: number | string, classId: number | string): Promise<Classroom> => {
  return http.post(`/teaching/classrooms/${classroomId}/assign-class`, null, {
    params: { classId }
  })
}

export const unassignClassFromClassroom = (classroomId: number | string): Promise<Classroom> => {
  return http.post(`/teaching/classrooms/${classroomId}/assign-class`, null, {
    params: { classId: null }
  })
}

// ==================== 教学任务 ====================

export const teachingTaskApi = {
  list: (params?: TeachingTaskQueryParams) =>
    http.get<PageResult<TeachingTask>>(`${BASE_URL}/tasks`, { params }),

  getById: (id: number | string) => http.get<TeachingTask>(`${BASE_URL}/tasks/${id}`),

  create: (data: Partial<TeachingTask>) =>
    http.post<TeachingTask>(`${BASE_URL}/tasks`, data),

  update: (id: number | string, data: Partial<TeachingTask>) =>
    http.put<TeachingTask>(`${BASE_URL}/tasks/${id}`, data),

  delete: (id: number | string) => http.delete(`${BASE_URL}/tasks/${id}`),

  assignTeachers: (taskId: number | string, teacherIds: (number | string)[], mainTeacherId: number | string) =>
    http.post(`${BASE_URL}/tasks/${taskId}/assign-teachers`, { teacherIds, mainTeacherId }),

  removeTeacher: (taskId: number | string, teacherId: number | string) =>
    http.delete(`${BASE_URL}/tasks/${taskId}/teachers/${teacherId}`),

  batchCreate: (semesterId: number | string, planId: number | string, classIds: (number | string)[]) =>
    http.post<TeachingTask[]>(`${BASE_URL}/tasks/batch-create`, { semesterId, planId, classIds }),

  updateStatus: (id: number | string, status: number) =>
    http.patch(`${BASE_URL}/tasks/${id}/status`, { status }),
}

// ==================== 排课管理 ====================

export const scheduleApi = {
  list: (params?: { semesterId?: number | string; status?: number }) =>
    http.get<CourseSchedule[]>(`${BASE_URL}/schedules`, { params }),

  getById: (id: number | string) => http.get<CourseSchedule>(`${BASE_URL}/schedules/${id}`),

  create: (data: Partial<CourseSchedule>) =>
    http.post<CourseSchedule>(`${BASE_URL}/schedules`, data),

  update: (id: number | string, data: Partial<CourseSchedule>) =>
    http.put<CourseSchedule>(`${BASE_URL}/schedules/${id}`, data),

  delete: (id: number | string) => http.delete(`${BASE_URL}/schedules/${id}`),

  publish: (id: number | string) =>
    http.post(`${BASE_URL}/schedules/${id}/publish`),

  archive: (id: number | string) =>
    http.post(`${BASE_URL}/schedules/${id}/archive`),

  // 课表条目
  getEntries: (scheduleId: number | string) =>
    http.get<ScheduleEntry[]>(`${BASE_URL}/schedules/${scheduleId}/entries`),

  addEntry: (scheduleId: number | string, data: Partial<ScheduleEntry>) =>
    http.post<ScheduleEntry>(`${BASE_URL}/schedules/${scheduleId}/entries`, data),

  updateEntry: (scheduleId: number | string, entryId: number | string, data: Partial<ScheduleEntry>) =>
    http.put<ScheduleEntry>(`${BASE_URL}/schedules/${scheduleId}/entries/${entryId}`, data),

  deleteEntry: (scheduleId: number | string, entryId: number | string) =>
    http.delete(`${BASE_URL}/schedules/${scheduleId}/entries/${entryId}`),

  // 智能排课
  autoSchedule: (params: { semesterId: number | string; maxIterations?: number; populationSize?: number; scheduleId?: number | string; mutationRate?: number }) =>
    http.post<any>(`${BASE_URL}/schedules/auto-schedule`, params),

  // 冲突检测
  checkConflicts: (scheduleId: number | string) =>
    http.get<{ conflicts: any[] }>(`${BASE_URL}/schedules/${scheduleId}/conflicts`),

  // 拖拽移动课表条目
  moveEntry: (id: number | string, data: { semesterId: number | string; dayOfWeek: number; periodStart: number; classroomId?: number }) =>
    http.post(`${BASE_URL}/schedules/${id}/move`, data),

  // 检查移动冲突
  checkMoveConflict: (data: { entryId: number; semesterId: number | string; dayOfWeek: number; periodStart: number }) =>
    http.post(`${BASE_URL}/schedules/check-move-conflict`, data),

  // 按班级/教师/教室查询
  getByClass: (classId: number | string, semesterId: number | string) =>
    http.get<ScheduleEntry[]>(`${BASE_URL}/schedules/by-class/${classId}`, { params: { semesterId } }),

  getByTeacher: (teacherId: number | string, semesterId: number | string) =>
    http.get<ScheduleEntry[]>(`${BASE_URL}/schedules/by-teacher/${teacherId}`, { params: { semesterId } }),

  getByClassroom: (classroomId: number | string, semesterId: number | string) =>
    http.get<ScheduleEntry[]>(`${BASE_URL}/schedules/by-classroom/${classroomId}`, { params: { semesterId } }),

  // 导出课表
  exportClassSchedule: (semesterId: number | string, classId: number | string) =>
    http.get(`${BASE_URL}/schedules/export/class/${classId}`, { params: { semesterId }, responseType: 'blob' }),

  exportTeacherSchedule: (semesterId: number | string, teacherId: number | string) =>
    http.get(`${BASE_URL}/schedules/export/teacher/${teacherId}`, { params: { semesterId }, responseType: 'blob' }),
}

// ==================== 调课管理 ====================

export const adjustmentApi = {
  list: (params?: AdjustmentQueryParams) =>
    http.get<PageResult<ScheduleAdjustment>>(`${BASE_URL}/adjustments`, { params }),

  getById: (id: number | string) => http.get<ScheduleAdjustment>(`${BASE_URL}/adjustments/${id}`),

  apply: (data: {
    entryId: number | string
    adjustmentType: number
    newClassroomId?: number | string
    newDayOfWeek?: number
    newPeriodStart?: number
    newPeriodEnd?: number
    newWeek?: number
    reason: string
  }) => http.post<ScheduleAdjustment>(`${BASE_URL}/adjustments`, data),

  approve: (id: number | string, remark?: string) =>
    http.post(`${BASE_URL}/adjustments/${id}/approve`, { remark }),

  reject: (id: number | string, remark: string) =>
    http.post(`${BASE_URL}/adjustments/${id}/reject`, { remark }),

  execute: (id: number | string) =>
    http.post(`${BASE_URL}/adjustments/${id}/execute`),

  cancel: (id: number | string) =>
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

  getBatch: (id: number | string) => http.get<ExamBatch>(`${BASE_URL}/examinations/batches/${id}`),

  createBatch: (data: Partial<ExamBatch>) =>
    http.post<ExamBatch>(`${BASE_URL}/examinations/batches`, data),

  updateBatch: (id: number | string, data: Partial<ExamBatch>) =>
    http.put<ExamBatch>(`${BASE_URL}/examinations/batches/${id}`, data),

  deleteBatch: (id: number | string) => http.delete(`${BASE_URL}/examinations/batches/${id}`),

  publishBatch: (id: number | string) =>
    http.post(`${BASE_URL}/examinations/batches/${id}/publish`),

  // 考试安排
  getArrangements: (batchId: number | string) =>
    http.get<ExamArrangement[]>(`${BASE_URL}/examinations/batches/${batchId}/arrangements`),

  createArrangement: (batchId: number | string, data: Partial<ExamArrangement>) =>
    http.post<ExamArrangement>(`${BASE_URL}/examinations/batches/${batchId}/arrangements`, data),

  updateArrangement: (batchId: number | string, arrangementId: number | string, data: Partial<ExamArrangement>) =>
    http.put<ExamArrangement>(`${BASE_URL}/examinations/batches/${batchId}/arrangements/${arrangementId}`, data),

  deleteArrangement: (batchId: number | string, arrangementId: number | string) =>
    http.delete(`${BASE_URL}/examinations/batches/${batchId}/arrangements/${arrangementId}`),

  // 考场分配
  assignRooms: (arrangementId: number | string, rooms: { classroomId: number | string; capacity: number }[]) =>
    http.post(`${BASE_URL}/examinations/arrangements/${arrangementId}/rooms`, { rooms }),

  // 监考教师
  assignInvigilators: (roomId: number | string, teacherIds: (number | string)[], mainTeacherId: number | string) =>
    http.post(`${BASE_URL}/examinations/rooms/${roomId}/invigilators`, { teacherIds, mainTeacherId }),

  // 查询教师监考安排
  getTeacherExams: (teacherId: number | string, semesterId: number | string) =>
    http.get<ExamArrangement[]>(`${BASE_URL}/examinations/by-teacher/${teacherId}`, { params: { semesterId } }),

  // 查询学生考试安排
  getStudentExams: (studentId: number | string, semesterId: number | string) =>
    http.get<ExamArrangement[]>(`${BASE_URL}/examinations/by-student/${studentId}`, { params: { semesterId } }),
}

// ==================== 成绩管理 ====================

export const gradeApi = {
  // 成绩批次
  listBatches: (params?: GradeQueryParams) =>
    http.get<PageResult<GradeBatch>>(`${BASE_URL}/grades/batches`, { params }),

  getBatch: (id: number | string) => http.get<GradeBatch>(`${BASE_URL}/grades/batches/${id}`),

  createBatch: (data: Partial<GradeBatch>) =>
    http.post<GradeBatch>(`${BASE_URL}/grades/batches`, data),

  updateBatch: (id: number | string, data: Partial<GradeBatch>) =>
    http.put<GradeBatch>(`${BASE_URL}/grades/batches/${id}`, data),

  deleteBatch: (id: number | string) => http.delete(`${BASE_URL}/grades/batches/${id}`),

  submitBatch: (id: number | string) =>
    http.post(`${BASE_URL}/grades/batches/${id}/submit`),

  approveBatch: (id: number | string) =>
    http.post(`${BASE_URL}/grades/batches/${id}/approve`),

  publishBatch: (id: number | string) =>
    http.post(`${BASE_URL}/grades/batches/${id}/publish`),

  // 成绩录入
  getGrades: (batchId: number | string) =>
    http.get<StudentGrade[]>(`${BASE_URL}/grades/batches/${batchId}/grades`),

  recordGrade: (batchId: number | string, data: {
    studentId: number | string
    totalScore?: number
    items?: { itemName: string; score: number; weight: number }[]
    remark?: string
  }) => http.post<StudentGrade>(`${BASE_URL}/grades/batches/${batchId}/grades`, data),

  updateGrade: (gradeId: number | string, data: {
    totalScore?: number
    items?: { itemName: string; score: number; weight: number }[]
    remark?: string
  }) => http.put<StudentGrade>(`${BASE_URL}/grades/${gradeId}`, data),

  batchRecordGrades: (batchId: number | string, grades: {
    studentId: number | string
    totalScore: number
    remark?: string
  }[]) => http.post(`${BASE_URL}/grades/batches/${batchId}/batch-record`, { grades }),

  // 成绩查询
  getStudentGrades: (studentId: number | string, params?: { semesterId?: number | string; courseId?: number | string }) =>
    http.get<StudentGrade[]>(`${BASE_URL}/grades/by-student/${studentId}`, { params }),

  getClassGrades: (classId: number | string, params?: { semesterId?: number | string; courseId?: number | string }) =>
    http.get<StudentGrade[]>(`${BASE_URL}/grades/by-class/${classId}`, { params }),

  // 成绩统计
  getStatistics: (params: { batchId?: number | string; classId?: number | string; courseId?: number | string; semesterId?: number | string }) =>
    http.get<GradeStatistics>(`${BASE_URL}/grades/statistics`, { params }),

  // 成绩排名
  getRanking: (params: { classId: number | string; semesterId: number | string; courseId?: number | string }) =>
    http.get<{ studentId: number | string; studentName: string; totalScore: number; rank: number }[]>(
      `${BASE_URL}/grades/ranking`, { params }
    ),

  // 导出成绩（按批次）
  exportGrades: (batchId: number | string) =>
    http.get(`${BASE_URL}/grades/batches/${batchId}/export`, { responseType: 'blob' }),

  // 导出成绩（按学期/班级/课程筛选）
  exportGradesByFilter: (params: { semesterId: number | string; classId?: number; courseId?: number }) =>
    http.get(`${BASE_URL}/grades/export`, { params, responseType: 'blob' }),

  // 导入成绩模板
  getImportTemplate: (batchId: number | string) =>
    http.get(`${BASE_URL}/grades/batches/${batchId}/import-template`, { responseType: 'blob' }),

  // 导入成绩
  importGrades: (batchId: number | string, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post(`${BASE_URL}/grades/batches/${batchId}/import`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

// =====================================================
// 开课管理 API
// =====================================================

export const offeringApi = {
  list: (semesterId: number | string) =>
    http.get<SemesterOffering[]>(`${BASE_URL}/offerings`, { params: { semesterId } }),
  create: (data: Partial<SemesterOffering>) =>
    http.post<SemesterOffering>(`${BASE_URL}/offerings`, data),
  update: (id: number | string, data: Partial<SemesterOffering>) =>
    http.put<SemesterOffering>(`${BASE_URL}/offerings/${id}`, data),
  delete: (id: number | string) =>
    http.delete(`${BASE_URL}/offerings/${id}`),
  confirm: (id: number | string) =>
    http.post(`${BASE_URL}/offerings/${id}/confirm`),
  importFromPlan: (data: { semesterId: number; planId: number; classIds?: number[] }) =>
    http.post(`${BASE_URL}/offerings/import-from-plan`, data),
}

export const classAssignmentApi = {
  list: (semesterId: number | string, classId?: number | string) =>
    http.get<ClassCourseAssignment[]>(`${BASE_URL}/class-assignments`, { params: { semesterId, classId } }),
  create: (data: Partial<ClassCourseAssignment>) =>
    http.post<ClassCourseAssignment>(`${BASE_URL}/class-assignments`, data),
  delete: (id: number | string) =>
    http.delete(`${BASE_URL}/class-assignments/${id}`),
  batchConfirm: (semesterId: number | string, classId: number | string) =>
    http.post(`${BASE_URL}/class-assignments/batch-confirm`, { semesterId, classId }),
}

// =====================================================
// 教学班 API
// =====================================================

export const teachingClassApi = {
  list: (semesterId: number | string) =>
    http.get<TeachingClass[]>(`${BASE_URL}/teaching-classes`, { params: { semesterId } }),
  getById: (id: number | string) =>
    http.get<TeachingClass>(`${BASE_URL}/teaching-classes/${id}`),
  create: (data: Partial<TeachingClass>) =>
    http.post<TeachingClass>(`${BASE_URL}/teaching-classes`, data),
  update: (id: number | string, data: Partial<TeachingClass>) =>
    http.put<TeachingClass>(`${BASE_URL}/teaching-classes/${id}`, data),
  delete: (id: number | string) =>
    http.delete(`${BASE_URL}/teaching-classes/${id}`),
  autoGenerate: (semesterId: number | string) =>
    http.post(`${BASE_URL}/teaching-classes/auto-generate`, { semesterId }),
  getMembers: (id: number | string) =>
    http.get<TeachingClassMember[]>(`${BASE_URL}/teaching-classes/${id}/members`),
  addMembers: (id: number | string, members: Partial<TeachingClassMember>[]) =>
    http.post(`${BASE_URL}/teaching-classes/${id}/members`, members),
  removeMembers: (id: number | string, memberIds: number[]) =>
    http.delete(`${BASE_URL}/teaching-classes/${id}/members`, { data: memberIds }),
}

// =====================================================
// 约束管理 API
// =====================================================

export const constraintApi = {
  list: (params: { semesterId: number | string; level?: number; targetId?: number | string }) =>
    http.get<SchedulingConstraint[]>(`${BASE_URL}/constraints`, { params }),
  create: (data: Partial<SchedulingConstraint>) =>
    http.post<SchedulingConstraint>(`${BASE_URL}/constraints`, data),
  update: (id: number | string, data: Partial<SchedulingConstraint>) =>
    http.put<SchedulingConstraint>(`${BASE_URL}/constraints/${id}`, data),
  delete: (id: number | string) =>
    http.delete(`${BASE_URL}/constraints/${id}`),
  enable: (id: number | string) =>
    http.post(`${BASE_URL}/constraints/${id}/enable`),
  disable: (id: number | string) =>
    http.post(`${BASE_URL}/constraints/${id}/disable`),
  getTimeMatrix: (params: { semesterId: number | string; level: number; targetId?: number | string }) =>
    http.get<TimeMatrix>(`${BASE_URL}/constraints/time-matrix`, { params }),
  batchImport: (semesterId: number | string, constraints: Partial<SchedulingConstraint>[]) =>
    http.post(`${BASE_URL}/constraints/batch-import`, { semesterId, constraints }),
}

// =====================================================
// 冲突检测 API
// =====================================================

export const conflictApi = {
  feasibilityCheck: (semesterId: number | string) =>
    http.post<FeasibilityReport>(`${BASE_URL}/conflicts/feasibility-check`, null, { params: { semesterId } }),
  detect: (semesterId: number | string) =>
    http.post<DetectedConflict[]>(`${BASE_URL}/conflicts/detect`, null, { params: { semesterId } }),
  list: (params: { semesterId: number | string; status?: number }) =>
    http.get<DetectedConflict[]>(`${BASE_URL}/conflicts`, { params }),
  resolve: (id: number | string, note: string) =>
    http.post(`${BASE_URL}/conflicts/${id}/resolve`, { note }),
  ignore: (id: number | string, note: string) =>
    http.post(`${BASE_URL}/conflicts/${id}/ignore`, { note }),
}
