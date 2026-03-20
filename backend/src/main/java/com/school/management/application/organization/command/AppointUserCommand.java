package com.school.management.application.organization.command;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class AppointUserCommand {
    private Long userId;
    private Long positionId;
    private boolean primary;
    private String appointmentType;  // FORMAL/ACTING/CONCURRENT/PROBATION
    private LocalDate startDate;
    private String reason;
    private Long createdBy;
}
