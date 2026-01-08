package com.school.management.entity.task;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.school.management.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 站内消息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "system_messages", autoResultMap = true)
public class SystemMessage extends BaseEntity {

    /**
     * 消息类型: TASK_ASSIGN-任务分配, TASK_REMIND-任务提醒, TASK_APPROVE-审批通知, SYSTEM-系统通知
     */
    private String messageType;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送人ID(系统消息为空)
     */
    private Long senderId;

    /**
     * 发送人姓名
     */
    private String senderName;

    /**
     * 接收人ID
     */
    private Long receiverId;

    /**
     * 接收人姓名
     */
    private String receiverName;

    /**
     * 是否已读: 0-未读, 1-已读
     */
    private Integer isRead;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

    /**
     * 业务类型: TASK-任务
     */
    private String businessType;

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 额外数据
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraData;
}
