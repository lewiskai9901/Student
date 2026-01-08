package com.school.management.domain.inspection.model;

import com.school.management.domain.shared.Entity;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * AppealApproval entity.
 * Records each approval action in the appeal workflow.
 */
@Builder
public class AppealApproval extends Entity<Long> {

    private Long id;
    private Long appealId;
    private Long reviewerId;
    private String reviewLevel;
    private String action;
    private String comment;
    private LocalDateTime reviewedAt;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAppealId() {
        return appealId;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public String getReviewLevel() {
        return reviewLevel;
    }

    public String getAction() {
        return action;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }
}
