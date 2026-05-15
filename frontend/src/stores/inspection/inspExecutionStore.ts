/**
 * 检查平台 - 执行引擎 Store
 */
import type { LongId } from '@/types/common'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {
  InspProject,
  ProjectInspector,
  InspTask,
  InspSubmission,
  SubmissionDetail,
  InspEvidence,
  CreateProjectRequest,
  UpdateProjectRequest,
  PublishProjectRequest,
  AddInspectorRequest,
  ClaimTaskRequest,
  ReviewTaskRequest,
  AssignTaskRequest,
  CompleteSubmissionRequest,
  SaveFormDataRequest,
} from '@/types/insp/project'
import type { ProjectStatus } from '@/types/insp/enums'
import {
  getProjects,
  getProject,
  createProject,
  updateProject as updateProjectApi,
  deleteProject as deleteProjectApi,
  publishProject as publishProjectApi,
  pauseProject as pauseProjectApi,
  resumeProject as resumeProjectApi,
  completeProject as completeProjectApi,
  archiveProject as archiveProjectApi,
  getInspectors,
  addInspector as addInspectorApi,
  removeInspector as removeInspectorApi,
} from '@/api/inspection/project'
import {
  getTasks,
  getTask,
  getAvailableTasks,
  getMyTasks,
  claimTask as claimTaskApi,
  startTask as startTaskApi,
  submitTask as submitTaskApi,
  withdrawTask as withdrawTaskApi,
  reviewTask as reviewTaskApi,
  publishTask as publishTaskApi,
  cancelTask as cancelTaskApi,
  assignTask as assignTaskApi,
} from '@/api/inspection/task'
import {
  getSubmissions,
  getSubmission,
  lockSubmission as lockSubmissionApi,
  unlockSubmission as unlockSubmissionApi,
  startFilling as startFillingApi,
  saveFormData as saveFormDataApi,
  completeSubmission as completeSubmissionApi,
  skipSubmission as skipSubmissionApi,
  getDetails,
  getEvidence,
  updateDetailResponse as updateDetailResponseApi,
  updateDetailRemark as updateDetailRemarkApi,
  flagDetail as flagDetailApi,
  unflagDetail as unflagDetailApi,
} from '@/api/inspection/submission'
import type { UpdateDetailResponseRequest, FlagDetailRequest } from '@/types/insp/project'
import { getSections } from '@/api/inspection/template'

export interface SectionCondition {
  sectionId: LongId | string
  conditionLogic: string | null
}

export const useInspExecutionStore = defineStore('inspExecution', () => {
  // ========== State ==========

  const projects = ref<InspProject[]>([])
  const currentProject = ref<InspProject | null>(null)
  const inspectors = ref<ProjectInspector[]>([])

  const tasks = ref<InspTask[]>([])
  const currentTask = ref<InspTask | null>(null)
  const myTasks = ref<InspTask[]>([])
  const availableTasks = ref<InspTask[]>([])

  const submissions = ref<InspSubmission[]>([])
  const currentSubmission = ref<InspSubmission | null>(null)
  const details = ref<SubmissionDetail[]>([])
  const evidence = ref<InspEvidence[]>([])
  const sectionConditions = ref<SectionCondition[]>([])

  // ========== Project Actions ==========

  async function loadProjects(status?: ProjectStatus): Promise<InspProject[]> {
    const list = await getProjects(status ? { status } : undefined)
    projects.value = list
    return list
  }

  async function loadProject(id: LongId): Promise<InspProject> {
    const p = await getProject(id)
    currentProject.value = p
    return p
  }

  async function addProject(data: CreateProjectRequest): Promise<InspProject> {
    return await createProject(data)
  }

  async function editProject(id: LongId, data: UpdateProjectRequest): Promise<InspProject> {
    return await updateProjectApi(id, data)
  }

  async function removeProject(id: LongId): Promise<void> {
    await deleteProjectApi(id)
  }

  async function publishProject(id: LongId, data: PublishProjectRequest): Promise<InspProject> {
    return await publishProjectApi(id, data)
  }

  async function pauseProject(id: LongId): Promise<InspProject> {
    return await pauseProjectApi(id)
  }

  async function resumeProject(id: LongId): Promise<InspProject> {
    return await resumeProjectApi(id)
  }

  async function completeProject(id: LongId): Promise<InspProject> {
    return await completeProjectApi(id)
  }

  async function archiveProject(id: LongId): Promise<InspProject> {
    return await archiveProjectApi(id)
  }

  async function loadInspectors(projectId: LongId): Promise<ProjectInspector[]> {
    const list = await getInspectors(projectId)
    inspectors.value = list
    return list
  }

  async function addInspector(projectId: LongId, data: AddInspectorRequest): Promise<ProjectInspector> {
    return await addInspectorApi(projectId, data)
  }

  async function removeInspector(projectId: LongId, inspectorId: LongId): Promise<void> {
    await removeInspectorApi(projectId, inspectorId)
  }

  // ========== Task Actions ==========

  async function loadTasks(projectId?: LongId): Promise<InspTask[]> {
    const list = await getTasks(projectId ? { projectId } : undefined)
    tasks.value = list
    return list
  }

  async function loadTask(id: LongId): Promise<InspTask> {
    const t = await getTask(id)
    currentTask.value = t
    return t
  }

  async function loadAvailableTasks(): Promise<InspTask[]> {
    const list = await getAvailableTasks()
    availableTasks.value = list
    return list
  }

  async function loadMyTasks(): Promise<InspTask[]> {
    const list = await getMyTasks()
    myTasks.value = list
    return list
  }

  async function claimTask(id: LongId, data: ClaimTaskRequest): Promise<InspTask> {
    return await claimTaskApi(id, data)
  }

  async function startTask(id: LongId): Promise<InspTask> {
    return await startTaskApi(id)
  }

  async function submitTask(id: LongId): Promise<InspTask> {
    return await submitTaskApi(id)
  }

  async function withdrawTask(id: LongId): Promise<InspTask> {
    return await withdrawTaskApi(id)
  }

  async function reviewTask(id: LongId, data: ReviewTaskRequest): Promise<InspTask> {
    return await reviewTaskApi(id, data)
  }

  async function publishTask(id: LongId): Promise<InspTask> {
    return await publishTaskApi(id)
  }

  async function cancelTask(id: LongId): Promise<InspTask> {
    return await cancelTaskApi(id)
  }

  async function assignTask(id: LongId, data: AssignTaskRequest): Promise<InspTask> {
    return await assignTaskApi(id, data)
  }

  // ========== Submission Actions ==========

  async function loadSubmissions(taskId: LongId): Promise<InspSubmission[]> {
    const list = await getSubmissions({ taskId })
    submissions.value = list
    return list
  }

  async function loadSubmission(id: LongId): Promise<InspSubmission> {
    const s = await getSubmission(id)
    currentSubmission.value = s
    return s
  }

  async function lockSubmission(id: LongId): Promise<InspSubmission> {
    return await lockSubmissionApi(id)
  }

  async function unlockSubmission(id: LongId): Promise<InspSubmission> {
    return await unlockSubmissionApi(id)
  }

  async function startFillingSubmission(id: LongId): Promise<InspSubmission> {
    return await startFillingApi(id)
  }

  async function saveFormData(id: LongId, data: SaveFormDataRequest): Promise<InspSubmission> {
    return await saveFormDataApi(id, data)
  }

  async function completeSubmission(id: LongId, data: CompleteSubmissionRequest): Promise<InspSubmission> {
    return await completeSubmissionApi(id, data)
  }

  async function skipSubmission(id: LongId): Promise<InspSubmission> {
    return await skipSubmissionApi(id)
  }

  async function loadDetails(submissionId: LongId): Promise<SubmissionDetail[]> {
    const list = await getDetails(submissionId)
    details.value = list
    return list
  }

  async function loadEvidence(submissionId: LongId): Promise<InspEvidence[]> {
    const list = await getEvidence(submissionId)
    evidence.value = list
    return list
  }

  async function updateDetailResponse(detailId: LongId, data: UpdateDetailResponseRequest): Promise<SubmissionDetail> {
    return await updateDetailResponseApi(detailId, data)
  }

  async function updateDetailRemark(detailId: LongId, remark: string): Promise<SubmissionDetail> {
    return await updateDetailRemarkApi(detailId, remark)
  }

  async function flagDetail(detailId: LongId, data: FlagDetailRequest): Promise<SubmissionDetail> {
    return await flagDetailApi(detailId, data)
  }

  async function unflagDetail(detailId: LongId): Promise<SubmissionDetail> {
    return await unflagDetailApi(detailId)
  }

  // ========== Section Conditions ==========

  async function loadSectionConditions(projectId: LongId): Promise<SectionCondition[]> {
    try {
      const project = await getProject(projectId)
      if (project.rootSectionId) {
        const sections = await getSections(project.rootSectionId)
        sectionConditions.value = sections
          .map(s => ({ sectionId: s.id, conditionLogic: null }))
      } else {
        sectionConditions.value = []
      }
    } catch (e) {
      console.warn('[ConditionLogic] Failed to load section conditions:', e)
      sectionConditions.value = []
    }
    return sectionConditions.value
  }

  return {
    // State
    projects, currentProject, inspectors,
    tasks, currentTask, myTasks, availableTasks,
    submissions, currentSubmission, details, evidence, sectionConditions,
    // Project Actions
    loadProjects, loadProject, addProject, editProject, removeProject,
    publishProject, pauseProject, resumeProject, completeProject, archiveProject,
    loadInspectors, addInspector, removeInspector,
    // Task Actions
    loadTasks, loadTask, loadAvailableTasks, loadMyTasks,
    claimTask, startTask, submitTask, withdrawTask, reviewTask, publishTask, cancelTask, assignTask,
    // Submission Actions
    loadSubmissions, loadSubmission,
    lockSubmission, unlockSubmission, startFillingSubmission,
    saveFormData, completeSubmission, skipSubmission,
    loadDetails, loadEvidence,
    updateDetailResponse, updateDetailRemark, flagDetail, unflagDetail,
    loadSectionConditions,
  }
})
