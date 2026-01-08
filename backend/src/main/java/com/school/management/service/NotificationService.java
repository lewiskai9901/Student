package com.school.management.service;

import com.school.management.entity.CheckItemAppeal;
import com.school.management.entity.AppealApprovalRecord;

/**
 * 通知服务接口
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
public interface NotificationService {

    /**
     * 发送申诉提交通知
     * 通知审批人有新的申诉需要处理
     *
     * @param appeal 申诉
     * @param approverId 审批人ID
     */
    void sendAppealSubmittedNotification(CheckItemAppeal appeal, Long approverId);

    /**
     * 发送申诉审批通知
     * 通知申请人审批结果
     *
     * @param appeal 申诉
     * @param approvalRecord 审批记录
     */
    void sendAppealApprovedNotification(CheckItemAppeal appeal, AppealApprovalRecord approvalRecord);

    /**
     * 发送申诉生效通知
     * 通知申请人和相关人员申诉已生效
     *
     * @param appeal 申诉
     */
    void sendAppealEffectiveNotification(CheckItemAppeal appeal);

    /**
     * 发送审批超时提醒
     *
     * @param approvalRecord 审批记录
     */
    void sendApprovalTimeoutReminder(AppealApprovalRecord approvalRecord);

    /**
     * 发送审批超时提醒
     *
     * @param record 审批记录
     */
    void sendApprovalReminder(AppealApprovalRecord record);

    /**
     * 发送公示开始通知
     *
     * @param appeal 申诉
     */
    void sendPublicityStartNotification(CheckItemAppeal appeal);

    /**
     * 批量发送通知
     *
     * @param userIds 用户ID列表
     * @param title 标题
     * @param content 内容
     */
    void sendBatchNotification(Long[] userIds, String title, String content);
}
