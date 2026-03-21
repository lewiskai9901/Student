package com.school.management.domain.message.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 消息订阅规则领域模型
 * 定义哪类事件发生时通知哪些用户
 */
@Getter
@Builder
public class MsgSubscriptionRule {

    private Long id;
    private Long tenantId;
    private String ruleName;
    /** 事件大类，null 表示匹配全部 */
    private String eventCategory;
    /** 事件类型，null 表示大类下全部 */
    private String eventType;
    /** BY_ROLE / BY_ORG_ADMIN / BY_USER / BY_RELATED */
    private String targetMode;
    /** JSON 配置：角色编码列表 / 用户ID列表 / 组织ID列表 */
    private String targetConfig;
    /** IN_APP / EMAIL / WECHAT */
    private String channel;
    private Long templateId;
    private Integer isEnabled;
    private Integer sortOrder;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 判断此规则是否匹配给定事件
     */
    public boolean matches(String category, String type) {
        // eventCategory 为 null 表示匹配全部
        if (this.eventCategory != null && !this.eventCategory.equals(category)) {
            return false;
        }
        // eventType 为 null 表示匹配大类下全部
        if (this.eventType != null && !this.eventType.equals(type)) {
            return false;
        }
        return true;
    }
}
