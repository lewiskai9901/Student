package com.school.management.interfaces.rest.message;

import lombok.Data;

/**
 * 创建/更新订阅规则请求体
 */
@Data
public class CreateSubscriptionRuleRequest {

    private String ruleName;
    private String eventCategory;
    private String eventType;
    /** BY_ROLE / BY_ORG_ADMIN / BY_USER / BY_RELATED */
    private String targetMode;
    /** JSON 字符串 */
    private String targetConfig;
    /** IN_APP / EMAIL / WECHAT */
    private String channel;
    private Long templateId;
    private Integer isEnabled;
    private Integer sortOrder;
}
