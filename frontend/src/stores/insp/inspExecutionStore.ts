/**
 * V7 检查平台 - 执行引擎 Store
 */
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
} from '@/api/insp/project'
import {
  getTasks,
  getTask,
  getAvailableTasks,
  getMyTasks,
  claimTask as claimTaskApi,
  startTask as startTaskApi,
  submitTask as submitTaskApi,
  reviewTask as reviewTaskApi,
  publishTask as publishTaskApi,
  cancelTask as cancelTaskApi,
  assignTask as assignTaskApi,
} from '@/api/insp/task'
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
} from '@/api/insp/submission'
import type { UpdateDetailResponseRequest, FlagDetailRequest } from '@/types/insp/project'
import { getSections } from '@/api/insp/template'

export interface SectionCondition {
  sectionId: number | string
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

  async function loadProject(id: number): Promise<InspProject> {
    const p = await getProject(id)
    currentProject.value = p
    return p
  }

  async function addProject(data: CreateProjectRequest): Promise<InspProject> {
    return await createProject(data)
  }

  async function editProject(id: number, data: UpdateProjectRequest): Promise<InspProject> {
    return await updateProjectApi(id, data)
  }

  async function removeProject(id: number): Promise<void> {
    await deleteProjectApi(id)
  }

  async function publishProject(id: number, data: PublishProjectRequest): Promise<InspProject> {
    return await publishProjectApi(id, data)
  }

  async function pauseProject(id: number): Promise<InspProject> {
    return await pauseProjectApi(id)
  }

  async function resumeProject(id: number): Promise<InspProject> {
    return await resumeProjectApi(id)
  }

  async function completeProject(id: number): Promise<InspProject> {
    return await completeProjectApi(id)
  }

  async function archiveProject(id: number): Promise<InspProject> {
    return await archiveProjectApi(id)
  }

  async function loadInspectors(projectId: number): Promise<ProjectInspector[]> {
    const list = await getInspectors(projectId)
    inspectors.value = list
    return list
  }

  async function addInspector(projectId: number, data: AddInspectorRequest): Promise<ProjectInspector> {
    return await addInspectorApi(projectId, data)
  }

  async function removeInspector(projectId: number, inspectorId: number): Promise<void> {
    await removeInspectorApi(projectId, inspectorId)
  }

  // ========== Task Actions ==========

  async function loadTasks(projectId?: number): Promise<InspTask[]> {
    const list = await getTasks(projectId ? { projectId } : undefined)
    tasks.value = list
    return list
  }

  async function loadTask(id: number): Promise<InspTask> {
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

  async function claimTask(id: number, data: ClaimTaskRequest): Promise<InspTask> {
    return await claimTaskApi(id, data)
  }

  async function startTask(id: number): Promise<InspTask> {
    return await startTaskApi(id)
  }

  async function submitTask(id: number): Promise<InspTask> {
    return await submitTaskApi(id)
  }

  async function reviewTask(id: number, data: ReviewTaskRequest): Promise<InspTask> {
    return await reviewTaskApi(id, data)
  }

  async function publishTask(id: number): Promise<InspTask> {
    return await publishTaskApi(id)
  }

  async function cancelTask(id: number): Promise<InspTask> {
    return await cancelTaskApi(id)
  }

  async function assignTask(id: number, data: AssignTaskRequest): Promise<InspTask> {
    return await assignTaskApi(id, data)
  }

  // ========== Submission Actions ==========

  async function loadSubmissions(taskId: number): Promise<InspSubmission[]> {
    const list = await getSubmissions({ taskId })
    submissions.value = list
    return list
  }

  async function loadSubmission(id: number): Promise<InspSubmission> {
    const s = await getSubmission(id)
    currentSubmission.value = s
    return s
  }

  async function lockSubmission(id: number): Promise<InspSubmission> {
    return await lockSubmissionApi(id)
  }

  async function unlockSubmission(id: number): Promise<InspSubmission> {
    return await unlockSubmissionApi(id)
  }

  async function startFillingSubmission(id: number): Promise<InspSubmission> {
    return await startFillingApi(id)
  }

  async function saveFormData(id: number, data: SaveFormDataRequest): Promise<InspSubmission> {
    return await saveFormDataApi(id, data)
  }

  async function completeSubmission(id: number, data: CompleteSubmissionRequest): Promise<InspSubmission> {
    return await completeSubmissionApi(id, data)
  }

  async function skipSubmission(id: number): Promise<InspSubmission> {
    return await skipSubmissionApi(id)
  }

  async function loadDetails(submissionId: number): Promise<SubmissionDetail[]> {
    const list = await getDetails(submissionId)
    details.value = list
    return list
  }

  async function loadEvidence(submissionId: number): Promise<InspEvidence[]> {
    const list = await getEvidence(submissionId)
    evidence.value = list
    return list
  }

  async function updateDetailResponse(detailId: number, data: UpdateDetailResponseRequest): Promise<SubmissionDetail> {
    return await updateDetailResponseApi(detailId, data)
  }

  async function updateDetailRemark(detailId: number, remark: string): Promise<SubmissionDetail> {
    return await updateDetailRemarkApi(detailId, remark)
  }

  async function flagDetail(detailId: number, data: FlagDetailRequest): Promise<SubmissionDetail> {
    return await flagDetailApi(detailId, data)
  }

  async function unflagDetail(detailId: number): Promise<SubmissionDetail> {
    return await unflagDetailApi(detailId)
  }

  // ========== Section Conditions ==========

  async function loadSectionConditions(projectId: number): Promise<SectionCondition[]> {
    try {
      const project = await getProject(projectId)
      if (project.rootSectionId) {
        const sections = await getSections(project.rootSectionId)
        sectionConditions.value = sections
          .map(s => ({ sectionId: s.id }))
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
    claimTask, startTask, submitTask, reviewTask, publishTask, cancelTask, assignTask,
    // Submission Actions
    loadSubmissions, loadSubmission,
    lockSubmission, unlockSubmission, startFillingSubmission,
    saveFormData, completeSubmission, skipSubmission,
    loadDetails, loadEvidence,
    updateDetailResponse, updateDetailRemark, flagDetail, unflagDetail,
    loadSectionConditions,
  }
})
