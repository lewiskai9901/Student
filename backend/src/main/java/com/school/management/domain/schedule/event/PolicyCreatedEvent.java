package com.school.management.domain.schedule.event;

import com.school.management.domain.schedule.model.PolicyType;
import com.school.management.domain.schedule.model.RotationAlgorithm;
import com.school.management.domain.schedule.model.SchedulePolicy;
import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * Domain event raised when a schedule policy is created.
 */
public class PolicyCreatedEvent extends BaseDomainEvent {

    private final Long policyId;
    private final String policyCode;
    private final String policyName;
    private final PolicyType policyType;
    private final RotationAlgorithm rotationAlgorithm;
    private final Long templateId;
    private final Long createdBy;

    public PolicyCreatedEvent(SchedulePolicy policy) {
        super("SchedulePolicy", policy.getId());
        this.policyId = policy.getId();
        this.policyCode = policy.getPolicyCode();
        this.policyName = policy.getPolicyName();
        this.policyType = policy.getPolicyType();
        this.rotationAlgorithm = policy.getRotationAlgorithm();
        this.templateId = policy.getTemplateId();
        this.createdBy = policy.getCreatedBy();
    }

    public Long getPolicyId() {
        return policyId;
    }

    public String getPolicyCode() {
        return policyCode;
    }

    public String getPolicyName() {
        return policyName;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    public RotationAlgorithm getRotationAlgorithm() {
        return rotationAlgorithm;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }
}
