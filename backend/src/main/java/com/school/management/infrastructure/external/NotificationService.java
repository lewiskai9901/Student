package com.school.management.infrastructure.external;

import java.util.List;

/**
 * 通知服务接口 —— 站内消息。
 * (微信/短信方法已删: 原为空壳/假发送, 零调用方; 真实多通道走 application/message。)
 */
public interface NotificationService {

    /**
     * 发送站内消息
     *
     * @param userId  接收用户ID
     * @param title   消息标题
     * @param content 消息内容
     * @param type    消息类型
     */
    void sendInAppMessage(Long userId, String title, String content, String type);

    /**
     * 批量发送站内消息
     *
     * @param userIds 接收用户ID列表
     * @param title   消息标题
     * @param content 消息内容
     * @param type    消息类型
     */
    void sendInAppMessageBatch(List<Long> userIds, String title, String content, String type);

    /**
     * 消息类型常量
     */
    interface MessageType {
        String TASK_ASSIGNED = "TASK_ASSIGNED";
        String TASK_REMINDER = "TASK_REMINDER";
        String TASK_APPROVED = "TASK_APPROVED";
        String TASK_REJECTED = "TASK_REJECTED";
        String INSPECTION_PUBLISHED = "INSPECTION_PUBLISHED";
        String APPEAL_SUBMITTED = "APPEAL_SUBMITTED";
        String APPEAL_RESULT = "APPEAL_RESULT";
        String SYSTEM_NOTICE = "SYSTEM_NOTICE";
        String CORRECTIVE_ACTION_ASSIGNED = "CORRECTIVE_ACTION_ASSIGNED";
        String CORRECTIVE_ACTION_OVERDUE = "CORRECTIVE_ACTION_OVERDUE";
        String CORRECTIVE_ACTION_VERIFIED = "CORRECTIVE_ACTION_VERIFIED";
        String BEHAVIOR_ALERT = "BEHAVIOR_ALERT";
        String SCHEDULE_EXECUTION_COMPLETED = "SCHEDULE_EXECUTION_COMPLETED";
        String SCHEDULE_EXECUTION_FAILED = "SCHEDULE_EXECUTION_FAILED";
    }
}
