package com.school.management.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 站内消息DTO
 */
@Data
public class SystemMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息类型文本
     */
    private String messageTypeText;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送人ID
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
     * 是否已读
     */
    private Integer isRead;

    /**
     * 阅读时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 额外数据
     */
    private Map<String, Object> extraData;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
