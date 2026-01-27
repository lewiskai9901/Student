package com.school.management.interfaces.rest.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class CreatePolicyRequest {
    @NotBlank(message = "策略名称不能为空")
    private String policyName;
    @NotBlank(message = "策略类型不能为空")
    private String policyType;
    @NotBlank(message = "轮询算法不能为空")
    private String rotationAlgorithm;
    private Long templateId;
    @NotEmpty(message = "检查员列表不能为空")
    private List<Long> inspectorPool;
    private String scheduleConfig;
    private List<String> excludedDates;
}
