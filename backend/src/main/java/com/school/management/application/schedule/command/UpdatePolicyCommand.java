package com.school.management.application.schedule.command;

import java.util.List;

public class UpdatePolicyCommand {
    private Long policyId;
    private String policyName;
    private String policyType;
    private String rotationAlgorithm;
    private Long templateId;
    private List<Long> inspectorPool;
    private String scheduleConfig;
    private List<String> excludedDates;
    private Long updatedBy;

    // Private constructor for Builder
    private UpdatePolicyCommand() {}

    public static Builder builder() { return new Builder(); }

    // Getters
    public Long getPolicyId() { return policyId; }
    public String getPolicyName() { return policyName; }
    public String getPolicyType() { return policyType; }
    public String getRotationAlgorithm() { return rotationAlgorithm; }
    public Long getTemplateId() { return templateId; }
    public List<Long> getInspectorPool() { return inspectorPool; }
    public String getScheduleConfig() { return scheduleConfig; }
    public List<String> getExcludedDates() { return excludedDates; }
    public Long getUpdatedBy() { return updatedBy; }

    public static class Builder {
        private final UpdatePolicyCommand cmd = new UpdatePolicyCommand();
        public Builder policyId(Long v) { cmd.policyId = v; return this; }
        public Builder policyName(String v) { cmd.policyName = v; return this; }
        public Builder policyType(String v) { cmd.policyType = v; return this; }
        public Builder rotationAlgorithm(String v) { cmd.rotationAlgorithm = v; return this; }
        public Builder templateId(Long v) { cmd.templateId = v; return this; }
        public Builder inspectorPool(List<Long> v) { cmd.inspectorPool = v; return this; }
        public Builder scheduleConfig(String v) { cmd.scheduleConfig = v; return this; }
        public Builder excludedDates(List<String> v) { cmd.excludedDates = v; return this; }
        public Builder updatedBy(Long v) { cmd.updatedBy = v; return this; }
        public UpdatePolicyCommand build() { return cmd; }
    }
}
