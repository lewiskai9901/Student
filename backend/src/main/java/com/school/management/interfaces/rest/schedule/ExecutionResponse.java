package com.school.management.interfaces.rest.schedule;

import com.school.management.domain.schedule.model.ScheduleExecution;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExecutionResponse {
    private Long id;
    private Long policyId;
    private LocalDate executionDate;
    private List<Long> assignedInspectors;
    private Long sessionId;
    private String status;
    private String failureReason;
    private LocalDateTime createdAt;

    public static ExecutionResponse fromDomain(ScheduleExecution execution) {
        ExecutionResponse response = new ExecutionResponse();
        response.setId(execution.getId());
        response.setPolicyId(execution.getPolicyId());
        response.setExecutionDate(execution.getExecutionDate());
        response.setAssignedInspectors(execution.getAssignedInspectors());
        response.setSessionId(execution.getSessionId());
        response.setStatus(execution.getStatus().name());
        response.setFailureReason(execution.getFailureReason());
        response.setCreatedAt(execution.getCreatedAt());
        return response;
    }
}
