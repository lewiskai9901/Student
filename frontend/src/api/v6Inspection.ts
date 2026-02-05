/**
 * V6检查系统API
 */
import request from '@/utils/request'
import type {
  InspectionProject,
  InspectionTask,
  InspectionTarget,
  CreateProjectRequest,
  UpdateProjectConfigRequest,
  PublishProjectRequest,
  PagedResponse,
  ProjectOptions,
  ProjectQueryParams,
  TaskQueryParams
} from '@/types/v6Inspection'

const BASE_URL = '/v6/inspection-projects'
const TASK_URL = '/v6/inspection-tasks'
const TEMPLATE_URL = '/inspection-templates'
const TEMPLATE_ITEM_URL = '/v6/template-items'

// ========== 模板管理API ==========

export const v6InspectionApi = {
  /**
   * 获取所有模板
   */
  getTemplates(): Promise<any> {
    return request.get(TEMPLATE_URL)
  },

  /**
   * 获取模板详情
   */
  getTemplate(id: number): Promise<any> {
    return request.get(`${TEMPLATE_URL}/${id}`)
  },

  /**
   * 创建模板
   */
  createTemplate(data: any): Promise<any> {
    return request.post(TEMPLATE_URL, data)
  },

  /**
   * 更新模板
   */
  updateTemplate(id: number, data: any): Promise<any> {
    return request.put(`${TEMPLATE_URL}/${id}`, data)
  },

  /**
   * 发布模板
   */
  publishTemplate(id: number): Promise<any> {
    return request.put(`${TEMPLATE_URL}/${id}/publish`)
  },

  /**
   * 获取模板的所有类别
   */
  getCategories(templateId: number): Promise<any> {
    return request.get(`${TEMPLATE_ITEM_URL}/templates/${templateId}/categories`)
  },

  /**
   * 创建类别
   */
  createCategory(data: any): Promise<any> {
    return request.post(`${TEMPLATE_ITEM_URL}/categories`, data)
  },

  /**
   * 更新类别
   */
  updateCategory(categoryId: number, data: any): Promise<any> {
    return request.put(`${TEMPLATE_ITEM_URL}/categories/${categoryId}`, data)
  },

  /**
   * 删除类别
   */
  deleteCategory(categoryId: number): Promise<any> {
    return request.delete(`${TEMPLATE_ITEM_URL}/categories/${categoryId}`)
  },

  /**
   * 获取类别下的检查项
   */
  getItems(categoryId: number): Promise<any> {
    return request.get(`${TEMPLATE_ITEM_URL}/categories/${categoryId}/items`)
  },

  /**
   * 创建检查项
   */
  createItem(data: any): Promise<any> {
    return request.post(`${TEMPLATE_ITEM_URL}/items`, data)
  },

  /**
   * 更新检查项
   */
  updateItem(itemId: number, data: any): Promise<any> {
    return request.put(`${TEMPLATE_ITEM_URL}/items/${itemId}`, data)
  },

  /**
   * 删除检查项
   */
  deleteItem(itemId: number): Promise<any> {
    return request.delete(`${TEMPLATE_ITEM_URL}/items/${itemId}`)
  }
}

// ========== 检查项目API ==========

export const v6ProjectApi = {
  /**
   * 创建项目
   */
  create(data: CreateProjectRequest): Promise<InspectionProject> {
    return request.post(BASE_URL, data)
  },

  /**
   * 更新项目配置
   */
  updateConfig(id: number, data: UpdateProjectConfigRequest): Promise<InspectionProject> {
    return request.put(`${BASE_URL}/${id}/config`, data)
  },

  /**
   * 发布项目
   */
  publish(id: number, data?: PublishProjectRequest): Promise<InspectionProject> {
    return request.post(`${BASE_URL}/${id}/publish`, data || {})
  },

  /**
   * 暂停项目
   */
  pause(id: number): Promise<InspectionProject> {
    return request.post(`${BASE_URL}/${id}/pause`)
  },

  /**
   * 恢复项目
   */
  resume(id: number): Promise<InspectionProject> {
    return request.post(`${BASE_URL}/${id}/resume`)
  },

  /**
   * 完成项目
   */
  complete(id: number): Promise<InspectionProject> {
    return request.post(`${BASE_URL}/${id}/complete`)
  },

  /**
   * 归档项目
   */
  archive(id: number): Promise<InspectionProject> {
    return request.post(`${BASE_URL}/${id}/archive`)
  },

  /**
   * 获取项目详情
   */
  getById(id: number): Promise<InspectionProject> {
    return request.get(`${BASE_URL}/${id}`)
  },

  /**
   * 分页查询项目
   */
  list(params?: ProjectQueryParams): Promise<PagedResponse<InspectionProject>> {
    return request.get(BASE_URL, { params })
  },

  /**
   * 删除项目
   */
  delete(id: number): Promise<void> {
    return request.delete(`${BASE_URL}/${id}`)
  },

  /**
   * 获取枚举选项
   */
  getOptions(): Promise<ProjectOptions> {
    return request.get(`${BASE_URL}/options`)
  }
}

// ========== 检查任务API ==========

export const v6TaskApi = {
  /**
   * 手动生成任务
   */
  generate(projectId: number, date: string): Promise<InspectionTask[]> {
    return request.post(`${TASK_URL}/generate`, null, {
      params: { projectId, date }
    })
  },

  /**
   * 批量生成任务
   */
  generateBatch(projectId: number, startDate: string, endDate: string): Promise<InspectionTask[]> {
    return request.post(`${TASK_URL}/generate-batch`, null, {
      params: { projectId, startDate, endDate }
    })
  },

  /**
   * 获取任务详情
   */
  getById(id: number): Promise<InspectionTask> {
    return request.get(`${TASK_URL}/${id}`)
  },

  /**
   * 分页查询任务
   */
  list(params?: TaskQueryParams): Promise<PagedResponse<InspectionTask>> {
    return request.get(TASK_URL, { params })
  },

  /**
   * 获取可领取的任务
   */
  getAvailable(date?: string): Promise<InspectionTask[]> {
    return request.get(`${TASK_URL}/available`, { params: { date } })
  },

  /**
   * 获取我的任务
   */
  getMyTasks(): Promise<InspectionTask[]> {
    return request.get(`${TASK_URL}/my-tasks`)
  },

  /**
   * 领取任务
   */
  claim(id: number): Promise<InspectionTask> {
    return request.post(`${TASK_URL}/${id}/claim`)
  },

  /**
   * 开始任务
   */
  start(id: number): Promise<InspectionTask> {
    return request.post(`${TASK_URL}/${id}/start`)
  },

  /**
   * 提交任务
   */
  submit(id: number): Promise<InspectionTask> {
    return request.post(`${TASK_URL}/${id}/submit`)
  },

  /**
   * 审核任务
   */
  review(id: number): Promise<InspectionTask> {
    return request.post(`${TASK_URL}/${id}/review`)
  },

  /**
   * 发布任务
   */
  publish(id: number): Promise<InspectionTask> {
    return request.post(`${TASK_URL}/${id}/publish`)
  },

  /**
   * 取消任务
   */
  cancel(id: number): Promise<InspectionTask> {
    return request.post(`${TASK_URL}/${id}/cancel`)
  },

  /**
   * 获取任务的检查目标
   */
  getTargets(taskId: number): Promise<InspectionTarget[]> {
    return request.get(`${TASK_URL}/${taskId}/targets`)
  },

  /**
   * 锁定目标
   */
  lockTarget(targetId: number): Promise<InspectionTarget> {
    return request.post(`${TASK_URL}/targets/${targetId}/lock`)
  },

  /**
   * 解锁目标
   */
  unlockTarget(targetId: number): Promise<InspectionTarget> {
    return request.post(`${TASK_URL}/targets/${targetId}/unlock`)
  },

  /**
   * 完成目标检查
   */
  completeTarget(targetId: number): Promise<InspectionTarget> {
    return request.post(`${TASK_URL}/targets/${targetId}/complete`)
  },

  /**
   * 跳过目标
   */
  skipTarget(targetId: number, reason: string): Promise<InspectionTarget> {
    return request.post(`${TASK_URL}/targets/${targetId}/skip`, { reason })
  },

  /**
   * 添加扣分
   */
  addDeduction(targetId: number, deduction: number): Promise<void> {
    return request.post(`${TASK_URL}/targets/${targetId}/deductions`, { deduction })
  },

  /**
   * 添加加分
   */
  addBonus(targetId: number, bonus: number): Promise<void> {
    return request.post(`${TASK_URL}/targets/${targetId}/bonuses`, { bonus })
  }
}

// ========== 检查员分配API ==========

const ASSIGNMENT_URL = '/v6/inspector-assignments'

export const v6AssignmentApi = {
  /**
   * 获取项目检查员配置
   */
  getProjectInspectors(projectId: number): Promise<any[]> {
    return request.get(`${ASSIGNMENT_URL}/projects/${projectId}/inspectors`)
  },

  /**
   * 添加项目检查员
   */
  addProjectInspector(projectId: number, data: any): Promise<any> {
    return request.post(`${ASSIGNMENT_URL}/projects/${projectId}/inspectors`, data)
  },

  /**
   * 批量添加项目检查员
   */
  addProjectInspectors(projectId: number, inspectors: any[]): Promise<void> {
    return request.post(`${ASSIGNMENT_URL}/projects/${projectId}/inspectors/batch`, inspectors)
  },

  /**
   * 移除项目检查员
   */
  removeProjectInspector(projectId: number, inspectorId: number): Promise<void> {
    return request.delete(`${ASSIGNMENT_URL}/projects/${projectId}/inspectors/${inspectorId}`)
  },

  /**
   * 获取任务检查员分配
   */
  getTaskAssignments(taskId: number): Promise<any[]> {
    return request.get(`${ASSIGNMENT_URL}/tasks/${taskId}/inspectors`)
  },

  /**
   * 分配检查员到任务
   */
  assignInspector(taskId: number, data: any): Promise<any> {
    return request.post(`${ASSIGNMENT_URL}/tasks/${taskId}/inspectors`, data)
  },

  /**
   * 批量分配检查员
   */
  assignInspectors(taskId: number, assignments: any[]): Promise<void> {
    return request.post(`${ASSIGNMENT_URL}/tasks/${taskId}/inspectors/batch`, assignments)
  },

  /**
   * 接受任务分配
   */
  acceptAssignment(assignmentId: number): Promise<void> {
    return request.post(`${ASSIGNMENT_URL}/assignments/${assignmentId}/accept`)
  },

  /**
   * 拒绝任务分配
   */
  declineAssignment(assignmentId: number): Promise<void> {
    return request.post(`${ASSIGNMENT_URL}/assignments/${assignmentId}/decline`)
  },

  /**
   * 自动分配检查员
   */
  autoAssign(taskId: number, projectId: number): Promise<void> {
    return request.post(`${ASSIGNMENT_URL}/tasks/${taskId}/auto-assign`, null, {
      params: { projectId }
    })
  }
}

// ========== 汇总统计API ==========

const SUMMARY_URL = '/v6/inspection-summaries'

export interface SummaryQueryParams {
  projectId: number
  startDate: string
  endDate: string
}

export interface TrendQueryParams extends SummaryQueryParams {
  targetId: number
  targetType: string
}

export const v6SummaryApi = {
  /**
   * 手动生成日汇总
   */
  generateDaily(projectId: number, date: string): Promise<void> {
    return request.post(`${SUMMARY_URL}/projects/${projectId}/daily`, null, {
      params: { date }
    })
  },

  /**
   * 手动生成周汇总
   */
  generateWeekly(projectId: number, year: number, weekNumber: number): Promise<void> {
    return request.post(`${SUMMARY_URL}/projects/${projectId}/weekly`, null, {
      params: { year, weekNumber }
    })
  },

  /**
   * 手动生成月汇总
   */
  generateMonthly(projectId: number, year: number, month: number): Promise<void> {
    return request.post(`${SUMMARY_URL}/projects/${projectId}/monthly`, null, {
      params: { year, month }
    })
  },

  /**
   * 获取日排名
   */
  getDailyRanking(projectId: number, date: string, targetType?: string): Promise<any[]> {
    return request.get(`${SUMMARY_URL}/projects/${projectId}/daily-ranking`, {
      params: { date, targetType }
    })
  },

  /**
   * 获取组织汇总
   */
  getOrgSummary(params: SummaryQueryParams): Promise<any[]> {
    return request.get(`${SUMMARY_URL}/projects/${params.projectId}/org-summary`, {
      params: {
        startDate: params.startDate,
        endDate: params.endDate
      }
    })
  },

  /**
   * 获取班级汇总
   */
  getClassSummary(params: SummaryQueryParams): Promise<any[]> {
    return request.get(`${SUMMARY_URL}/projects/${params.projectId}/class-summary`, {
      params: {
        startDate: params.startDate,
        endDate: params.endDate
      }
    })
  },

  /**
   * 获取趋势数据
   */
  getTrend(params: TrendQueryParams): Promise<any[]> {
    return request.get(`${SUMMARY_URL}/projects/${params.projectId}/trend`, {
      params: {
        targetId: params.targetId,
        targetType: params.targetType,
        startDate: params.startDate,
        endDate: params.endDate
      }
    })
  }
}

// ========== 检查执行API ==========

const EXECUTION_URL = '/v6/inspection-execution'

export interface AddDeductionRequest {
  targetId: number
  categoryId?: number
  categoryCode?: string
  categoryName?: string
  itemId?: number
  itemCode?: string
  itemName: string
  score: number
  quantity?: number
  remark?: string
}

export interface AddDeductionWithIndividualRequest extends AddDeductionRequest {
  individualType: string  // 'USER' | 'SPACE'
  individualId: number
  individualName: string
}

export interface AddBonusRequest extends AddDeductionRequest {}

export interface AddEvidenceRequest {
  fileName: string
  filePath: string
  fileUrl: string
  fileSize?: number
  fileType?: string
}

export interface InspectionDetail {
  id: number
  targetId: number
  categoryId?: number
  categoryCode?: string
  categoryName?: string
  itemId?: number
  itemCode?: string
  itemName: string
  scope: string  // 'WHOLE' | 'INDIVIDUAL'
  individualType?: string
  individualId?: number
  individualName?: string
  scoringMode: string
  score: number
  quantity: number
  totalScore: number
  gradeCode?: string
  gradeName?: string
  checklistChecked?: boolean
  remark?: string
  evidenceIds?: number[]
  createdBy?: number
  createdAt?: string
  updatedAt?: string
}

export interface InspectionEvidence {
  id: number
  detailId?: number
  targetId?: number
  fileName: string
  filePath: string
  fileUrl: string
  fileSize?: number
  fileType?: string
  latitude?: number
  longitude?: number
  uploadBy?: number
  uploadTime?: string
  createdAt?: string
}

export const v6ExecutionApi = {
  /**
   * 添加扣分明细
   */
  addDeduction(data: AddDeductionRequest): Promise<InspectionDetail> {
    return request.post(`${EXECUTION_URL}/details/deduction`, data)
  },

  /**
   * 添加带个体关联的扣分明细
   */
  addDeductionWithIndividual(data: AddDeductionWithIndividualRequest): Promise<InspectionDetail> {
    return request.post(`${EXECUTION_URL}/details/deduction-individual`, data)
  },

  /**
   * 添加加分明细
   */
  addBonus(data: AddBonusRequest): Promise<InspectionDetail> {
    return request.post(`${EXECUTION_URL}/details/bonus`, data)
  },

  /**
   * 更新明细
   */
  updateDetail(detailId: number, data: { score: number; quantity?: number; remark?: string }): Promise<InspectionDetail> {
    return request.put(`${EXECUTION_URL}/details/${detailId}`, data)
  },

  /**
   * 删除明细
   */
  deleteDetail(detailId: number): Promise<void> {
    return request.delete(`${EXECUTION_URL}/details/${detailId}`)
  },

  /**
   * 获取目标的明细列表
   */
  getDetailsByTarget(targetId: number): Promise<InspectionDetail[]> {
    return request.get(`${EXECUTION_URL}/targets/${targetId}/details`)
  },

  /**
   * 获取目标某类别的明细
   */
  getDetailsByTargetAndCategory(targetId: number, categoryId: number): Promise<InspectionDetail[]> {
    return request.get(`${EXECUTION_URL}/targets/${targetId}/categories/${categoryId}/details`)
  },

  /**
   * 获取个体的明细列表
   */
  getDetailsByIndividual(individualType: string, individualId: number): Promise<InspectionDetail[]> {
    return request.get(`${EXECUTION_URL}/individuals/${individualType}/${individualId}/details`)
  },

  /**
   * 获取明细详情
   */
  getDetail(detailId: number): Promise<InspectionDetail> {
    return request.get(`${EXECUTION_URL}/details/${detailId}`)
  },

  /**
   * 添加明细证据
   */
  addEvidenceForDetail(detailId: number, data: AddEvidenceRequest): Promise<InspectionEvidence> {
    return request.post(`${EXECUTION_URL}/details/${detailId}/evidences`, data)
  },

  /**
   * 添加目标整体证据
   */
  addEvidenceForTarget(targetId: number, data: AddEvidenceRequest): Promise<InspectionEvidence> {
    return request.post(`${EXECUTION_URL}/targets/${targetId}/evidences`, data)
  },

  /**
   * 删除证据
   */
  deleteEvidence(evidenceId: number): Promise<void> {
    return request.delete(`${EXECUTION_URL}/evidences/${evidenceId}`)
  },

  /**
   * 获取明细的证据列表
   */
  getEvidencesByDetail(detailId: number): Promise<InspectionEvidence[]> {
    return request.get(`${EXECUTION_URL}/details/${detailId}/evidences`)
  },

  /**
   * 获取目标的整体证据列表
   */
  getEvidencesByTarget(targetId: number): Promise<InspectionEvidence[]> {
    return request.get(`${EXECUTION_URL}/targets/${targetId}/evidences`)
  }
}

export default {
  project: v6ProjectApi,
  task: v6TaskApi,
  assignment: v6AssignmentApi,
  summary: v6SummaryApi,
  execution: v6ExecutionApi
}
