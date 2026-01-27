import request from '@/utils/request'
import type {
  AssetApproval,
  CreateApprovalRequest,
  ApprovalActionRequest
} from '@/types/asset'

const BASE_URL = '/asset-approvals'

export const assetApprovalApi = {
  /**
   * 创建审批申请
   */
  createApproval(data: CreateApprovalRequest): Promise<number> {
    return request.post(BASE_URL, data)
  },

  /**
   * 审批通过
   */
  approve(id: number, data?: ApprovalActionRequest): Promise<void> {
    return request.post(`${BASE_URL}/${id}/approve`, data || {})
  },

  /**
   * 审批拒绝
   */
  reject(id: number, data?: ApprovalActionRequest): Promise<void> {
    return request.post(`${BASE_URL}/${id}/reject`, data || {})
  },

  /**
   * 取消申请
   */
  cancel(id: number): Promise<void> {
    return request.post(`${BASE_URL}/${id}/cancel`)
  },

  /**
   * 获取审批详情
   */
  getApproval(id: number): Promise<AssetApproval> {
    return request.get(`${BASE_URL}/${id}`)
  },

  /**
   * 获取我的申请列表
   */
  getMyApprovals(): Promise<AssetApproval[]> {
    return request.get(`${BASE_URL}/my`)
  },

  /**
   * 获取待审批列表
   */
  getPendingApprovals(): Promise<AssetApproval[]> {
    return request.get(`${BASE_URL}/pending`)
  },

  /**
   * 分页查询审批列表
   */
  queryApprovals(params: {
    approvalType?: number
    status?: number
    applicantId?: number
    pageNum?: number
    pageSize?: number
  }): Promise<{
    records: AssetApproval[]
    total: number
    pageNum: number
    pageSize: number
  }> {
    return request.get(BASE_URL, { params })
  },

  /**
   * 获取待审批数量
   */
  countPending(): Promise<number> {
    return request.get(`${BASE_URL}/pending/count`)
  }
}
