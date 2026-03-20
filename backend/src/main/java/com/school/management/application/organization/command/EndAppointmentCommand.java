package com.school.management.application.organization.command;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class EndAppointmentCommand {
    private LocalDate endDate;
    private String reason;
}
