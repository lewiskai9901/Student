package com.school.management.interfaces.rest.message;

import lombok.Data;

/**
 * 创建/更新消息模板请求体
 */
@Data
public class CreateTemplateRequest {

    private String templateCode;
    private String templateName;
    private String titleTemplate;
    private String contentTemplate;
    private Integer isEnabled;
}
