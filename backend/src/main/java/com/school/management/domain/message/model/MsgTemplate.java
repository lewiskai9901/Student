package com.school.management.domain.message.model;

import lombok.Builder;
import lombok.Getter;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 消息模板领域模型
 * 支持变量替换，变量格式：{{variableName}}
 */
@Getter
@Builder
public class MsgTemplate {

    private Long id;
    private Long tenantId;
    private String templateCode;
    private String templateName;
    private String titleTemplate;
    private String contentTemplate;
    private Integer isSystem;
    private Integer isEnabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 渲染模板，替换变量
     *
     * @param variables 变量映射 key=变量名, value=替换值
     * @return 渲染后的 {title, content}
     */
    public RenderedMessage render(Map<String, String> variables) {
        String title = replaceVariables(titleTemplate, variables);
        String content = contentTemplate != null ? replaceVariables(contentTemplate, variables) : null;
        return new RenderedMessage(title, content);
    }

    private String replaceVariables(String template, Map<String, String> variables) {
        if (template == null || variables == null) {
            return template;
        }
        // 模板内容本身可能包含 HTML 结构（由管理员可信地定义），
        // 但变量值来自外部输入，必须 HTML 转义以防 XSS。
        // 前端用 {{ }} 渲染时会自动转义，这是第二道防线；
        // 若未来改用 v-html 渲染富文本，此层转义已经就位。
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String raw = entry.getValue() != null ? entry.getValue() : "";
            String safeValue = HtmlUtils.htmlEscape(raw);
            result = result.replace(placeholder, safeValue);
        }
        return result;
    }

    /**
     * 渲染结果
     */
    @Getter
    public static class RenderedMessage {
        private final String title;
        private final String content;

        public RenderedMessage(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }
}
