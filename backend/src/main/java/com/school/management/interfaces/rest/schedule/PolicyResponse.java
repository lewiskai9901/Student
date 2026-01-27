package com.school.management.interfaces.rest.schedule;

import com.school.management.domain.schedule.model.SchedulePolicy;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PolicyResponse {
    private Long id;
    private String policyCode;
    private String policyName;
    private String policyType;
    private String rotationAlgorithm;
    private Long templateId;
    private List<Long> inspectorPool;
    private String scheduleConfig;
    private List<String> excludedDates;
    private boolean enabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PolicyResponse fromDomain(SchedulePolicy policy) {
        PolicyResponse response = new PolicyResponse();
        response.setId(policy.getId());
        response.setPolicyCode(policy.getPolicyCode());
        response.setPolicyName(policy.getPolicyName());
        response.setPolicyType(policy.getPolicyType().name());
        response.setRotationAlgorithm(policy.getRotationAlgorithm().name());
        response.setTemplateId(policy.getTemplateId());
        response.setInspectorPool(policy.getInspectorPool());
        response.setScheduleConfig(policy.getScheduleConfig());
        response.setExcludedDates(policy.getExcludedDates());
        response.setEnabled(policy.isEnabled());
        response.setCreatedBy(policy.getCreatedBy());
        response.setCreatedAt(policy.getCreatedAt());
        response.setUpdatedAt(policy.getUpdatedAt());
        return response;
    }
}
