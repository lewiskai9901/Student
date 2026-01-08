package com.school.management.infrastructure.external;

import java.util.List;
import java.util.Map;

/**
 * 通知服务接口
 * 用于发送各类通知（站内消息、微信推送、短信等）
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
     * 发送微信模板消息
     *
     * @param userId     用户ID
     * @param templateId 模板ID
     * @param data       模板数据
     * @param url        跳转链接
     */
    void sendWechatTemplate(Long userId, String templateId, Map<String, String> data, String url);

    /**
     * 发送短信
     *
     * @param phone   手机号
     * @param content 短信内容
     */
    void sendSms(String phone, String content);

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
    }
}
