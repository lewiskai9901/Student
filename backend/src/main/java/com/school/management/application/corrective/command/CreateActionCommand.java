package com.school.management.application.corrective.command;

import com.school.management.domain.corrective.model.ActionCategory;
import com.school.management.domain.corrective.model.ActionSeverity;
import com.school.management.domain.corrective.model.ActionSource;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CreateActionCommand {
    private String title;
    private String description;
    private ActionSource source;
    private Long sourceId;
    private ActionSeverity severity;
    private ActionCategory category;
    private Long classId;
    private Long assigneeId;
    private LocalDateTime deadline;
    private Long createdBy;
}
