package com.school.management.domain.schedule.event;

import com.school.management.domain.schedule.model.SchedulePolicy;
import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * Domain event raised when a schedule policy is enabled or disabled.
 */
public class PolicyStatusChangedEvent extends BaseDomainEvent {

    private final Long policyId;
    private final String policyCode;
    private final boolean enabled;

    public PolicyStatusChangedEvent(SchedulePolicy policy) {
        super("SchedulePolicy", policy.getId());
        this.policyId = policy.getId();
        this.policyCode = policy.getPolicyCode();
        this.enabled = policy.isEnabled();
    }

    public Long getPolicyId() {
        return policyId;
    }

    public String getPolicyCode() {
        return policyCode;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
