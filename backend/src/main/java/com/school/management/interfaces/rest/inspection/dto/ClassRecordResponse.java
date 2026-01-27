package com.school.management.interfaces.rest.inspection.dto;

import com.school.management.domain.inspection.model.ClassInspectionRecord;
import com.school.management.domain.inspection.model.InspectionBonus;
import com.school.management.domain.inspection.model.InspectionDeduction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ClassRecordResponse {

    private Long id;
    private Long sessionId;
    private Long classId;
    private String className;
    private Long orgUnitId;
    private String orgUnitName;
    private Integer baseScore;
    private BigDecimal totalDeduction;
    private BigDecimal bonusScore;
    private BigDecimal finalScore;
    private String status;
    private int deductionCount;
    private int checklistResponseCount;
    private LocalDateTime createdAt;
    private List<DeductionItem> deductions;
    private List<BonusRecord> bonuses;

    @Data
    public static class BonusRecord {
        private Long id;
        private Long bonusItemId;
        private BigDecimal bonusScore;
        private String reason;
        private LocalDateTime createdAt;
    }

    @Data
    public static class DeductionItem {
        private Long id;
        private String itemName;
        private String categoryName;
        private String spaceType;
        private String spaceName;
        private Integer personCount;
        private BigDecimal deductionAmount;
        private String inputSource;
        private String remark;
        private List<String> evidenceUrls;
    }

    public static ClassRecordResponse fromDomain(ClassInspectionRecord record) {
        ClassRecordResponse r = new ClassRecordResponse();
        r.setId(record.getId());
        r.setSessionId(record.getSessionId());
        r.setClassId(record.getClassId());
        r.setClassName(record.getClassName());
        r.setOrgUnitId(record.getOrgUnitId());
        r.setOrgUnitName(record.getOrgUnitName());
        r.setBaseScore(record.getBaseScore());
        r.setTotalDeduction(record.getTotalDeduction());
        r.setBonusScore(record.getBonusScore());
        r.setFinalScore(record.getFinalScore());
        r.setStatus(record.getStatus().name());
        r.setDeductionCount(record.getDeductions().size());
        r.setChecklistResponseCount(record.getChecklistResponses().size());
        r.setCreatedAt(record.getCreatedAt());
        r.setDeductions(record.getDeductions().stream()
            .map(ClassRecordResponse::toDeductionItem)
            .collect(Collectors.toList()));
        r.setBonuses(record.getBonuses().stream()
            .map(ClassRecordResponse::toBonusRecord)
            .collect(Collectors.toList()));
        return r;
    }

    private static BonusRecord toBonusRecord(InspectionBonus b) {
        BonusRecord record = new BonusRecord();
        record.setId(b.getId());
        record.setBonusItemId(b.getBonusItemId());
        record.setBonusScore(b.getBonusScore());
        record.setReason(b.getReason());
        record.setCreatedAt(b.getCreatedAt());
        return record;
    }

    private static DeductionItem toDeductionItem(InspectionDeduction d) {
        DeductionItem item = new DeductionItem();
        item.setId(d.getId());
        item.setItemName(d.getItemName());
        item.setCategoryName(d.getCategoryName());
        item.setSpaceType(d.getSpaceType() != null ? d.getSpaceType().name() : null);
        item.setSpaceName(d.getSpaceName());
        item.setPersonCount(d.getPersonCount());
        item.setDeductionAmount(d.getDeductionAmount());
        item.setInputSource(d.getInputSource() != null ? d.getInputSource().name() : null);
        item.setRemark(d.getRemark());
        item.setEvidenceUrls(d.getEvidenceUrls());
        return item;
    }
}
