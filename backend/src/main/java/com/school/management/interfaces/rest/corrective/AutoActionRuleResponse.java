package com.school.management.interfaces.rest.corrective;

import com.school.management.domain.corrective.model.AutoActionRule;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AutoActionRuleResponse {
    private Long id;
    private String ruleCode;
    private String ruleName;
    private String triggerType;
    private String triggerCondition;
    private String severity;
    private String category;
    private int deadlineHours;
    private boolean autoAssign;
    private boolean enabled;
    private LocalDateTime createdAt;

    public static AutoActionRuleResponse fromDomain(AutoActionRule rule) {
        AutoActionRuleResponse resp = new AutoActionRuleResponse();
        resp.setId(rule.getId());
        resp.setRuleCode(rule.getRuleCode());
        resp.setRuleName(rule.getRuleName());
        resp.setTriggerType(rule.getTriggerType());
        resp.setTriggerCondition(rule.getTriggerCondition());
        resp.setSeverity(rule.getSeverity().name());
        resp.setCategory(rule.getCategory().name());
        resp.setDeadlineHours(rule.getDeadlineHours());
        resp.setAutoAssign(rule.isAutoAssign());
        resp.setEnabled(rule.isEnabled());
        resp.setCreatedAt(rule.getCreatedAt());
        return resp;
    }
}
