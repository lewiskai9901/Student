package com.school.management.domain.teaching.model.scheduling;

import com.school.management.domain.shared.Entity;
import lombok.Getter;

@Getter
public class ScheduleConflictRecord implements Entity<Long> {
    private Long id;
    private Long semesterId;
    private String detectionBatch;
    private Integer conflictCategory;
    private String conflictType;
    private Integer severity;
    private String description;
    private String detail; // JSON
    private Long entryId1;
    private Long entryId2;
    private Long constraintId;
    private Integer resolutionStatus; // 0pending 1resolved 2ignored
    private String resolutionNote;
    private Long resolvedBy;

    protected ScheduleConflictRecord() {}

    @Override
    public Long getId() { return id; }

    public static ScheduleConflictRecord create(Long semesterId, String detectionBatch,
            Integer conflictCategory, String conflictType, Integer severity,
            String description, String detail, Long entryId1, Long entryId2, Long constraintId) {
        ScheduleConflictRecord r = new ScheduleConflictRecord();
        r.semesterId = semesterId;
        r.detectionBatch = detectionBatch;
        r.conflictCategory = conflictCategory;
        r.conflictType = conflictType;
        r.severity = severity;
        r.description = description;
        r.detail = detail;
        r.entryId1 = entryId1;
        r.entryId2 = entryId2;
        r.constraintId = constraintId;
        r.resolutionStatus = 0;
        return r;
    }

    public void resolve(String note, Long resolvedBy) {
        this.resolutionStatus = 1;
        this.resolutionNote = note;
        this.resolvedBy = resolvedBy;
    }

    public void ignore(String note, Long resolvedBy) {
        this.resolutionStatus = 2;
        this.resolutionNote = note;
        this.resolvedBy = resolvedBy;
    }
}
