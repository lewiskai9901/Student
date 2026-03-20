package com.school.management.application.organization.command;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class TransferUserCommand {
    private Long userId;
    private Long fromPositionId;
    private Long toPositionId;
    private LocalDate transferDate;
    private String reason;
    private Long operatedBy;
}
