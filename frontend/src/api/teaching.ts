import request from '@/utils/request'
import type {
  AcademicYear,
  Semester,
  TeachingWeek,
  AcademicEvent,
  Course,
  CourseQueryParams,
  CurriculumPlan,
  PlanCourse,
  CurriculumPlanQueryParams,
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
} from '@/types/teaching'
import type { Classroom, TeachingBuilding, ClassroomQueryParams } from '@/types/teaching'

const BASE_URL = '/teaching'

// ==================== 教学楼管理 ====================

export interface TeachingBuildingWithStats extends TeachingBuilding {
  totalClassrooms?: number
  usedClassrooms?: number
  totalCapacity?: number
}

export const getTeachingBuildings = async (): Promise<TeachingBuildingWithStats[]> => {
  // 使用 /teaching/buildings/enabled 获取所有启用的教学楼
  return request.get<TeachingBuildingWithStats[]>('/teaching/buildings/enabled', {
    params: { buildingType: 1 } // buildingType=1 for teaching buildings
  })
}

export const getTeachingBuildingById = (id: number | string): Promise<TeachingBuildingWithStats> => {
  return request.get(`/teaching/buildings/${id}`)
}

// ==================== 教室管理 ====================

export interface ClassroomWithDetails extends Classroom {
  buildingNo?: string
  usageRate?: number
}

export const getClassrooms = async (params?: ClassroomQueryParams): Promise<{ records: ClassroomWithDetails[]; total: number }> => {
  return request.get('/teaching/classrooms', { params })
}

export const getClassroomsByBuilding = async (buildingId: number | string): Promise<ClassroomWithDetails[]> => {
  const res = await request.get<{ records: ClassroomWithDetails[]; total: number }>('/teaching/classrooms', {
    params: { buildingId, pageNum: 1, pageSize: 500 }
  })
  return res.records || []
}

export const getClassroomById = (id: number | string): Promise<ClassroomWithDetails> => {
  return request.get(`/teaching/classrooms/${id}`)
}

export const createClassroom = (data: Partial<Classroom>): Promise<Classroom> => {
  return request.post('/teaching/classrooms', data)
}

export const updateClassroom = (id: number | string, data: Partial<Classroom>): Promise<Classroom> => {
  return request.put(`/teaching/classrooms/${id}`, data)
}

export const deleteClassroom = (id: number | string): Promise<void> => {
  return request.delete(`/teaching/classrooms/${id}`)
}

export const assignClassToClassroom = (classroomId: number | string, classId: number | string): Promise<Classroom> => {
  return request.post(`/teaching/classrooms/${classroomId}/assign-class`, null, {
    params: { classId }
  })
}

export const unassignClassFromClassroom = (classroomId: number | string): Promise<Classroom> => {
  return request.post(`/teaching/classrooms/${classroomId}/assign-class`, null, {
    params: { classId: null }
  })
}

// ==================== 学年管理 ====================

export const academicYearApi = {
  list: () => request.get<AcademicYear[]>(`${BASE_URL}/academic-years`),

  getById: (id: number) => request.get<AcademicYear>(`${BASE_URL}/academic-years/${id}`),

  getCurrent: () => request.get<AcademicYear>(`${BASE_URL}/academic-years/current`),

  create: (data: Partial<AcademicYear>) =>
    request.post<AcademicYear>(`${BASE_URL}/academic-years`, data),

  update: (id: number, data: Partial<AcademicYear>) =>
    request.put<AcademicYear>(`${BASE_URL}/academic-years/${id}`, data),

  delete: (id: number) => request.delete(`${BASE_URL}/academic-years/${id}`),

  setCurrent: (id: number) =>
    request.post(`${BASE_URL}/academic-years/${id}/set-current`),
}

// ==================== 学期管理 ====================

export const semesterApi = {
  list: (yearId?: number) => {
    const params = yearId ? { yearId } : {}
    return request.get<Semester[]>(`${BASE_URL}/semesters`, { params })
  },

  getById: (id: number) => request.get<Semester>(`${BASE_URL}/semesters/${id}`),

  getCurrent: () => request.get<Semester>(`${BASE_URL}/semesters/current`),

  create: (data: Partial<Semester>) =>
    request.post<Semester>(`${BASE_URL}/semesters`, data),

  update: (id: number, data: Partial<Semester>) =>
    request.put<Semester>(`${BASE_URL}/semesters/${id}`, data),

  delete: (id: number) => request.delete(`${BASE_URL}/semesters/${id}`),

  setCurrent: (id: number) =>
    request.post(`${BASE_URL}/semesters/${id}/set-current`),

  getWeeks: (semesterId: number) =>
    request.get<TeachingWeek[]>(`${BASE_URL}/semesters/${semesterId}/weeks`),

  generateWeeks: (semesterId: number) =>
    request.post<TeachingWeek[]>(`${BASE_URL}/semesters/${semesterId}/generate-weeks`),
}

// ==================== 校历事件 ====================

export const academicEventApi = {
  list: (params: { yearId?: number; semesterId?: number; eventType?: number }) =>
    request.get<AcademicEvent[]>(`${BASE_URL}/events`, { params }),

  getById: (id: number) => request.get<AcademicEvent>(`${BASE_URL}/events/${id}`),

  create: (data: Partial<AcademicEvent>) =>
    request.post<AcademicEvent>(`${BASE_URL}/events`, data),

  update: (id: number, data: Partial<AcademicEvent>) =>
    request.put<AcademicEvent>(`${BASE_URL}/events/${id}`, data),

  delete: (id: number) => request.delete(`${BASE_URL}/events/${id}`),
}

// ==================== 课程管理 ====================

export const courseApi = {
  list: (params?: CourseQueryParams) =>
    request.get<PageResult<Course>>(`${BASE_URL}/courses`, { params }),

  listAll: () => request.get<Course[]>(`${BASE_URL}/courses/all`),

  getById: (id: number) => request.get<Course>(`${BASE_URL}/courses/${id}`),

  getByCode: (code: string) => request.get<Course>(`${BASE_URL}/courses/code/${code}`),

  create: (data: Partial<Course>) =>
    request.post<Course>(`${BASE_URL}/courses`, data),

  update: (id: number, data: Partial<Course>) =>
    request.put<Course>(`${BASE_URL}/courses/${id}`, data),

  delete: (id: number) => request.delete(`${BASE_URL}/courses/${id}`),

  updateStatus: (id: number, status: number) =>
    request.patch(`${BASE_URL}/courses/${id}/status`, { status }),
}

// ==================== 培养方案 ====================

export const curriculumPlanApi = {
  list: (params?: CurriculumPlanQueryParams) =>
    request.get<PageResult<CurriculumPlan>>(`${BASE_URL}/curriculum-plans`, { params }),

  getById: (id: number) => request.get<CurriculumPlan>(`${BASE_URL}/curriculum-plans/${id}`),

  create: (data: Partial<CurriculumPlan>) =>
    request.post<CurriculumPlan>(`${BASE_URL}/curriculum-plans`, data),

  update: (id: number, data: Partial<CurriculumPlan>) =>
    request.put<CurriculumPlan>(`${BASE_URL}/curriculum-plans/${id}`, data),

  delete: (id: number) => request.delete(`${BASE_URL}/curriculum-plans/${id}`),

  publish: (id: number) =>
    request.post(`${BASE_URL}/curriculum-plans/${id}/publish`),

  deprecate: (id: number) =>
    request.post(`${BASE_URL}/curriculum-plans/${id}/deprecate`),

  getCourses: (planId: number) =>
    request.get<PlanCourse[]>(`${BASE_URL}/curriculum-plans/${planId}/courses`),

  addCourse: (planId: number, data: Partial<PlanCourse>) =>
    request.post<PlanCourse>(`${BASE_URL}/curriculum-plans/${planId}/courses`, data),

  updateCourse: (planId: number, courseId: number, data: Partial<PlanCourse>) =>
    request.put<PlanCourse>(`${BASE_URL}/curriculum-plans/${planId}/courses/${courseId}`, data),

  removeCourse: (planId: number, courseId: number) =>
    request.delete(`${BASE_URL}/curriculum-plans/${planId}/courses/${courseId}`),

  copyPlan: (id: number, newVersion: string) =>
    request.post<CurriculumPlan>(`${BASE_URL}/curriculum-plans/${id}/copy`, { newVersion }),
}

// ==================== 教学任务 ====================

export const teachingTaskApi = {
  list: (params?: TeachingTaskQueryParams) =>
    request.get<PageResult<TeachingTask>>(`${BASE_URL}/tasks`, { params }),

  getById: (id: number) => request.get<TeachingTask>(`${BASE_URL}/tasks/${id}`),

  create: (data: Partial<TeachingTask>) =>
    request.post<TeachingTask>(`${BASE_URL}/tasks`, data),

  update: (id: number, data: Partial<TeachingTask>) =>
    request.put<TeachingTask>(`${BASE_URL}/tasks/${id}`, data),

  delete: (id: number) => request.delete(`${BASE_URL}/tasks/${id}`),

  assignTeachers: (taskId: number, teacherIds: number[], mainTeacherId: number) =>
    request.post(`${BASE_URL}/tasks/${taskId}/assign-teachers`, { teacherIds, mainTeacherId }),

  removeTeacher: (taskId: number, teacherId: number) =>
    request.delete(`${BASE_URL}/tasks/${taskId}/teachers/${teacherId}`),

  batchCreate: (semesterId: number, planId: number, classIds: number[]) =>
    request.post<TeachingTask[]>(`${BASE_URL}/tasks/batch-create`, { semesterId, planId, classIds }),

  updateStatus: (id: number, status: number) =>
    request.patch(`${BASE_URL}/tasks/${id}/status`, { status }),
}

// ==================== 排课管理 ====================

export const scheduleApi = {
  list: (params?: { semesterId?: number; status?: number }) =>
    request.get<CourseSchedule[]>(`${BASE_URL}/schedules`, { params }),

  getById: (id: number) => request.get<CourseSchedule>(`${BASE_URL}/schedules/${id}`),

  create: (data: Partial<CourseSchedule>) =>
    request.post<CourseSchedule>(`${BASE_URL}/schedules`, data),

  update: (id: number, data: Partial<CourseSchedule>) =>
    request.put<CourseSchedule>(`${BASE_URL}/schedules/${id}`, data),

  delete: (id: number) => request.delete(`${BASE_URL}/schedules/${id}`),

  publish: (id: number) =>
    request.post(`${BASE_URL}/schedules/${id}/publish`),

  archive: (id: number) =>
    request.post(`${BASE_URL}/schedules/${id}/archive`),

  // 课表条目
  getEntries: (scheduleId: number) =>
    request.get<ScheduleEntry[]>(`${BASE_URL}/schedules/${scheduleId}/entries`),

  addEntry: (scheduleId: number, data: Partial<ScheduleEntry>) =>
    request.post<ScheduleEntry>(`${BASE_URL}/schedules/${scheduleId}/entries`, data),

  updateEntry: (scheduleId: number, entryId: number, data: Partial<ScheduleEntry>) =>
    request.put<ScheduleEntry>(`${BASE_URL}/schedules/${scheduleId}/entries/${entryId}`, data),

  deleteEntry: (scheduleId: number, entryId: number) =>
    request.delete(`${BASE_URL}/schedules/${scheduleId}/entries/${entryId}`),

  // 智能排课
  autoSchedule: (params: AutoScheduleParams) =>
    request.post<AutoScheduleResult>(`${BASE_URL}/schedules/${params.scheduleId}/auto-schedule`, params),

  // 冲突检测
  checkConflicts: (scheduleId: number) =>
    request.get<{ conflicts: any[] }>(`${BASE_URL}/schedules/${scheduleId}/conflicts`),

  // 按班级/教师/教室查询
  getByClass: (classId: number, semesterId: number) =>
    request.get<ScheduleEntry[]>(`${BASE_URL}/schedules/by-class/${classId}`, { params: { semesterId } }),

  getByTeacher: (teacherId: number, semesterId: number) =>
    request.get<ScheduleEntry[]>(`${BASE_URL}/schedules/by-teacher/${teacherId}`, { params: { semesterId } }),

  getByClassroom: (classroomId: number, semesterId: number) =>
    request.get<ScheduleEntry[]>(`${BASE_URL}/schedules/by-classroom/${classroomId}`, { params: { semesterId } }),
}

// ==================== 调课管理 ====================

export const adjustmentApi = {
  list: (params?: AdjustmentQueryParams) =>
    request.get<PageResult<ScheduleAdjustment>>(`${BASE_URL}/adjustments`, { params }),

  getById: (id: number) => request.get<ScheduleAdjustment>(`${BASE_URL}/adjustments/${id}`),

  apply: (data: {
    entryId: number
    adjustmentType: number
    newClassroomId?: number
    newDayOfWeek?: number
    newPeriodStart?: number
    newPeriodEnd?: number
    newWeek?: number
    reason: string
  }) => request.post<ScheduleAdjustment>(`${BASE_URL}/adjustments`, data),

  approve: (id: number, remark?: string) =>
    request.post(`${BASE_URL}/adjustments/${id}/approve`, { remark }),

  reject: (id: number, remark: string) =>
    request.post(`${BASE_URL}/adjustments/${id}/reject`, { remark }),

  execute: (id: number) =>
    request.post(`${BASE_URL}/adjustments/${id}/execute`),

  cancel: (id: number) =>
    request.post(`${BASE_URL}/adjustments/${id}/cancel`),

  getMyApplications: (params?: { status?: number; page?: number; size?: number }) =>
    request.get<PageResult<ScheduleAdjustment>>(`${BASE_URL}/adjustments/my-applications`, { params }),

  getPendingApprovals: (params?: { page?: number; size?: number }) =>
    request.get<PageResult<ScheduleAdjustment>>(`${BASE_URL}/adjustments/pending-approvals`, { params }),
}

// ==================== 考试管理 ====================

export const examApi = {
  // 考试批次
  listBatches: (params?: ExamBatchQueryParams) =>
    request.get<PageResult<ExamBatch>>(`${BASE_URL}/examinations/batches`, { params }),

  getBatch: (id: number) => request.get<ExamBatch>(`${BASE_URL}/examinations/batches/${id}`),

  createBatch: (data: Partial<ExamBatch>) =>
    request.post<ExamBatch>(`${BASE_URL}/examinations/batches`, data),

  updateBatch: (id: number, data: Partial<ExamBatch>) =>
    request.put<ExamBatch>(`${BASE_URL}/examinations/batches/${id}`, data),

  deleteBatch: (id: number) => request.delete(`${BASE_URL}/examinations/batches/${id}`),

  publishBatch: (id: number) =>
    request.post(`${BASE_URL}/examinations/batches/${id}/publish`),

  // 考试安排
  getArrangements: (batchId: number) =>
    request.get<ExamArrangement[]>(`${BASE_URL}/examinations/batches/${batchId}/arrangements`),

  createArrangement: (batchId: number, data: Partial<ExamArrangement>) =>
    request.post<ExamArrangement>(`${BASE_URL}/examinations/batches/${batchId}/arrangements`, data),

  updateArrangement: (batchId: number, arrangementId: number, data: Partial<ExamArrangement>) =>
    request.put<ExamArrangement>(`${BASE_URL}/examinations/batches/${batchId}/arrangements/${arrangementId}`, data),

  deleteArrangement: (batchId: number, arrangementId: number) =>
    request.delete(`${BASE_URL}/examinations/batches/${batchId}/arrangements/${arrangementId}`),

  // 考场分配
  assignRooms: (arrangementId: number, rooms: { classroomId: number; capacity: number }[]) =>
    request.post(`${BASE_URL}/examinations/arrangements/${arrangementId}/rooms`, { rooms }),

  // 监考教师
  assignInvigilators: (roomId: number, teacherIds: number[], mainTeacherId: number) =>
    request.post(`${BASE_URL}/examinations/rooms/${roomId}/invigilators`, { teacherIds, mainTeacherId }),

  // 查询教师监考安排
  getTeacherExams: (teacherId: number, semesterId: number) =>
    request.get<ExamArrangement[]>(`${BASE_URL}/examinations/by-teacher/${teacherId}`, { params: { semesterId } }),

  // 查询学生考试安排
  getStudentExams: (studentId: number, semesterId: number) =>
    request.get<ExamArrangement[]>(`${BASE_URL}/examinations/by-student/${studentId}`, { params: { semesterId } }),
}

// ==================== 成绩管理 ====================

export const gradeApi = {
  // 成绩批次
  listBatches: (params?: GradeQueryParams) =>
    request.get<PageResult<GradeBatch>>(`${BASE_URL}/grades/batches`, { params }),

  getBatch: (id: number) => request.get<GradeBatch>(`${BASE_URL}/grades/batches/${id}`),

  createBatch: (data: Partial<GradeBatch>) =>
    request.post<GradeBatch>(`${BASE_URL}/grades/batches`, data),

  updateBatch: (id: number, data: Partial<GradeBatch>) =>
    request.put<GradeBatch>(`${BASE_URL}/grades/batches/${id}`, data),

  deleteBatch: (id: number) => request.delete(`${BASE_URL}/grades/batches/${id}`),

  submitBatch: (id: number) =>
    request.post(`${BASE_URL}/grades/batches/${id}/submit`),

  approveBatch: (id: number) =>
    request.post(`${BASE_URL}/grades/batches/${id}/approve`),

  publishBatch: (id: number) =>
    request.post(`${BASE_URL}/grades/batches/${id}/publish`),

  // 成绩录入
  getGrades: (batchId: number) =>
    request.get<StudentGrade[]>(`${BASE_URL}/grades/batches/${batchId}/grades`),

  recordGrade: (batchId: number, data: {
    studentId: number
    totalScore?: number
    items?: { itemName: string; score: number; weight: number }[]
    remark?: string
  }) => request.post<StudentGrade>(`${BASE_URL}/grades/batches/${batchId}/grades`, data),

  updateGrade: (gradeId: number, data: {
    totalScore?: number
    items?: { itemName: string; score: number; weight: number }[]
    remark?: string
  }) => request.put<StudentGrade>(`${BASE_URL}/grades/${gradeId}`, data),

  batchRecordGrades: (batchId: number, grades: {
    studentId: number
    totalScore: number
    remark?: string
  }[]) => request.post(`${BASE_URL}/grades/batches/${batchId}/batch-record`, { grades }),

  // 成绩查询
  getStudentGrades: (studentId: number, params?: { semesterId?: number; courseId?: number }) =>
    request.get<StudentGrade[]>(`${BASE_URL}/grades/by-student/${studentId}`, { params }),

  getClassGrades: (classId: number, params?: { semesterId?: number; courseId?: number }) =>
    request.get<StudentGrade[]>(`${BASE_URL}/grades/by-class/${classId}`, { params }),

  // 成绩统计
  getStatistics: (params: { batchId?: number; classId?: number; courseId?: number; semesterId?: number }) =>
    request.get<GradeStatistics>(`${BASE_URL}/grades/statistics`, { params }),

  // 成绩排名
  getRanking: (params: { classId: number; semesterId: number; courseId?: number }) =>
    request.get<{ studentId: number; studentName: string; totalScore: number; rank: number }[]>(
      `${BASE_URL}/grades/ranking`, { params }
    ),

  // 导出成绩
  exportGrades: (batchId: number) =>
    request.get(`${BASE_URL}/grades/batches/${batchId}/export`, { responseType: 'blob' }),

  // 导入成绩模板
  getImportTemplate: (batchId: number) =>
    request.get(`${BASE_URL}/grades/batches/${batchId}/import-template`, { responseType: 'blob' }),

  // 导入成绩
  importGrades: (batchId: number, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post(`${BASE_URL}/grades/batches/${batchId}/import`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
