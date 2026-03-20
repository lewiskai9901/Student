/**
 * 批量作业API
 * 对标: AWS S3 Batch Operations API
 */

import request from '@/utils/request'
import type {
  BatchJobDTO,
  BatchAssignOrgRequest,
  BatchAssignResponsibleRequest,
  BatchChangeStatusRequest,
  BatchJobSubmitResponse
} from '@/types/batchJob'

/**
 * 提交批量分配组织Job
 */
export function submitBatchAssignOrg(data: BatchAssignOrgRequest): Promise<BatchJobSubmitResponse> {
  return request({
    url: '/v9/places/batch-jobs/assign-org',
    method: 'post',
    data
  })
}

/**
 * 提交批量分配负责人Job
 */
export function submitBatchAssignResponsible(data: BatchAssignResponsibleRequest): Promise<BatchJobSubmitResponse> {
  return request({
    url: '/v9/places/batch-jobs/assign-responsible',
    method: 'post',
    data
  })
}

/**
 * 提交批量变更状态Job
 */
export function submitBatchChangeStatus(data: BatchChangeStatusRequest): Promise<BatchJobSubmitResponse> {
  return request({
    url: '/v9/places/batch-jobs/change-status',
    method: 'post',
    data
  })
}

/**
 * 查询Job状态
 */
export function getJobStatus(jobId: string): Promise<BatchJobDTO> {
  return request({
    url: `/v9/places/batch-jobs/${jobId}`,
    method: 'get'
  })
}

/**
 * 查询我的最近Job列表
 */
export function getMyRecentJobs(limit: number = 20): Promise<BatchJobDTO[]> {
  return request({
    url: '/v9/places/batch-jobs/my-recent',
    method: 'get',
    params: { limit }
  })
}

/**
 * 取消Job
 */
export function cancelJob(jobId: string): Promise<void> {
  return request({
    url: `/v9/places/batch-jobs/${jobId}/cancel`,
    method: 'post'
  })
}
