package com.school.management.interfaces.rest.message;

import lombok.Data;

/**
 * 订阅规则预览请求：不保存规则，仅计算当前 (mode, config) 会命中多少用户
 */
@Data
public class PreviewRuleRequest {
    private String targetMode;
    private String targetConfig;
}
