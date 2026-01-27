package com.school.management.application.behavior.query;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BehaviorProfileDTO {
    private Long studentId;
    private long totalViolations;
    private long totalCommendations;
    private long recentViolations;
    private String riskLevel;
    private String trend;
}
