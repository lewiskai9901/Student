package com.school.management.interfaces.rest.behavior;

import com.school.management.domain.behavior.model.BehaviorRecord;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BehaviorRecordResponse {
    private Long id;
    private Long studentId;
    private Long classId;
    private String behaviorType;
    private String source;
    private Long sourceId;
    private String category;
    private String title;
    private String detail;
    private BigDecimal deductionAmount;
    private String status;
    private Long recordedBy;
    private LocalDateTime recordedAt;
    private LocalDateTime notifiedAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private String resolutionNote;

    public static BehaviorRecordResponse fromDomain(BehaviorRecord record) {
        BehaviorRecordResponse resp = new BehaviorRecordResponse();
        resp.setId(record.getId());
        resp.setStudentId(record.getStudentId());
        resp.setClassId(record.getClassId());
        resp.setBehaviorType(record.getBehaviorType().name());
        resp.setSource(record.getSource().name());
        resp.setSourceId(record.getSourceId());
        resp.setCategory(record.getCategory().name());
        resp.setTitle(record.getTitle());
        resp.setDetail(record.getDetail());
        resp.setDeductionAmount(record.getDeductionAmount());
        resp.setStatus(record.getStatus().name());
        resp.setRecordedBy(record.getRecordedBy());
        resp.setRecordedAt(record.getRecordedAt());
        resp.setNotifiedAt(record.getNotifiedAt());
        resp.setAcknowledgedAt(record.getAcknowledgedAt());
        resp.setResolvedAt(record.getResolvedAt());
        resp.setResolutionNote(record.getResolutionNote());
        return resp;
    }
}
