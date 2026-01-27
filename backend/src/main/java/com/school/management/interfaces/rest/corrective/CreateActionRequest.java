package com.school.management.interfaces.rest.corrective;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateActionRequest {
    @NotBlank
    private String title;
    private String description;
    @NotNull
    private String source; // INSPECTION, APPEAL, MANUAL
    private Long sourceId;
    @NotNull
    private String severity; // MINOR, MODERATE, SEVERE, CRITICAL
    @NotNull
    private String category; // HYGIENE, DISCIPLINE, SAFETY, OTHER
    private Long classId;
    private Long assigneeId;
    private LocalDateTime deadline;
}
