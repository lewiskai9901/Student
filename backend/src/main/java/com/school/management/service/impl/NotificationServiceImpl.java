package com.school.management.service.impl;

import com.school.management.entity.CheckItemAppeal;
import com.school.management.entity.AppealApprovalRecord;
import com.school.management.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void sendAppealSubmittedNotification(CheckItemAppeal appeal, Long approverId) {
        log.info("发送申诉提交通知: appealId={}, approverId={}", appeal.getId(), approverId);
    }

    @Override
    public void sendAppealApprovedNotification(CheckItemAppeal appeal, AppealApprovalRecord approvalRecord) {
        log.info("发送申诉审批通知: appealId={}, status={}", appeal.getId(), approvalRecord.getApprovalStatus());
    }

    @Override
    public void sendAppealEffectiveNotification(CheckItemAppeal appeal) {
        log.info("发送申诉生效通知: appealId={}", appeal.getId());
    }

    @Override
    public void sendApprovalTimeoutReminder(AppealApprovalRecord approvalRecord) {
        log.info("发送审批超时提醒: recordId={}", approvalRecord.getId());
    }

    @Override
    public void sendApprovalReminder(AppealApprovalRecord record) {
        log.info("发送审批提醒: recordId={}", record.getId());
    }

    @Override
    public void sendPublicityStartNotification(CheckItemAppeal appeal) {
        log.info("发送公示开始通知: appealId={}", appeal.getId());
    }

    @Override
    public void sendBatchNotification(Long[] userIds, String title, String content) {
        log.info("批量发送通知: userCount={}", userIds.length);
    }
}
