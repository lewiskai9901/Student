package com.school.management.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.dto.InspectionSessionQueryRequest;
import com.school.management.dto.InspectionSessionRequest;
import com.school.management.dto.InspectionSessionResponse;

/**
 * 检查批次服务接口
 *
 * @author system
 * @since 2.0.0
 */
public interface InspectionSessionService {

    /**
     * 创建检查批次
     *
     * @param request 请求参数
     * @param userId  当前用户ID
     * @param userName 当前用户姓名
     * @return 检查批次ID
     */
    Long createInspectionSession(InspectionSessionRequest request, Long userId, String userName);

    /**
     * 查询检查批次列表
     *
     * @param request 查询参数
     * @return 分页结果
     */
    Page<InspectionSessionResponse> listInspectionSessions(InspectionSessionQueryRequest request);

    /**
     * 查询检查批次详情
     *
     * @param sessionId 批次ID
     * @return 详情
     */
    InspectionSessionResponse getInspectionSessionDetail(Long sessionId);

    /**
     * 更新检查批次
     *
     * @param sessionId 批次ID
     * @param request   请求参数
     * @return 是否成功
     */
    boolean updateInspectionSession(Long sessionId, InspectionSessionRequest request);

    /**
     * 删除检查批次
     *
     * @param sessionId 批次ID
     * @return 是否成功
     */
    boolean deleteInspectionSession(Long sessionId);

    /**
     * 提交审核
     *
     * @param sessionId 批次ID
     * @return 是否成功
     */
    boolean submitForReview(Long sessionId);

    /**
     * 审核通过
     *
     * @param sessionId    批次ID
     * @param reviewerId   审核人ID
     * @param reviewerName 审核人姓名
     * @return 是否成功
     */
    boolean approveInspectionSession(Long sessionId, Long reviewerId, String reviewerName);

    /**
     * 发布检查结果
     *
     * @param sessionId 批次ID
     * @return 是否成功
     */
    boolean publishInspectionSession(Long sessionId);

    /**
     * 生成检查批次编号
     *
     * @return 批次编号
     */
    String generateSessionCode();
}
