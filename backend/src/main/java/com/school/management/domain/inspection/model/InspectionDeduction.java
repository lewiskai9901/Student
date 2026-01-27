package com.school.management.domain.inspection.model;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entity representing a single deduction within a class inspection record.
 * Contains details about what was deducted, from which space/student, and why.
 */
public class InspectionDeduction extends Entity<Long> {

    private Long id;
    private Long sessionId;
    private Long classRecordId;
    private Long deductionItemId;
    private String itemName;
    private String categoryName;
    private SpaceType spaceType;
    private Long spaceId;
    private String spaceName;
    private List<Long> studentIds;
    private List<String> studentNames;
    private Integer personCount;
    private BigDecimal deductionAmount;
    private InputSource inputSource;
    private String remark;
    private List<String> evidenceUrls;
    private LocalDateTime createdAt;

    protected InspectionDeduction() {
        this.studentIds = new ArrayList<>();
        this.studentNames = new ArrayList<>();
        this.evidenceUrls = new ArrayList<>();
    }

    private InspectionDeduction(Builder builder) {
        this.id = builder.id;
        this.sessionId = builder.sessionId;
        this.classRecordId = builder.classRecordId;
        this.deductionItemId = builder.deductionItemId;
        this.itemName = builder.itemName;
        this.categoryName = builder.categoryName;
        this.spaceType = builder.spaceType != null ? builder.spaceType : SpaceType.NONE;
        this.spaceId = builder.spaceId;
        this.spaceName = builder.spaceName;
        this.studentIds = builder.studentIds != null ? new ArrayList<>(builder.studentIds) : new ArrayList<>();
        this.studentNames = builder.studentNames != null ? new ArrayList<>(builder.studentNames) : new ArrayList<>();
        this.personCount = builder.personCount != null ? builder.personCount : 0;
        this.deductionAmount = builder.deductionAmount != null ? builder.deductionAmount : BigDecimal.ZERO;
        this.inputSource = builder.inputSource;
        this.remark = builder.remark;
        this.evidenceUrls = builder.evidenceUrls != null ? new ArrayList<>(builder.evidenceUrls) : new ArrayList<>();
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public Long getClassRecordId() { return classRecordId; }
    public Long getDeductionItemId() { return deductionItemId; }
    public String getItemName() { return itemName; }
    public String getCategoryName() { return categoryName; }
    public SpaceType getSpaceType() { return spaceType; }
    public Long getSpaceId() { return spaceId; }
    public String getSpaceName() { return spaceName; }
    public List<Long> getStudentIds() { return Collections.unmodifiableList(studentIds); }
    public List<String> getStudentNames() { return Collections.unmodifiableList(studentNames); }
    public Integer getPersonCount() { return personCount; }
    public BigDecimal getDeductionAmount() { return deductionAmount; }
    public InputSource getInputSource() { return inputSource; }
    public String getRemark() { return remark; }
    public List<String> getEvidenceUrls() { return Collections.unmodifiableList(evidenceUrls); }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long sessionId;
        private Long classRecordId;
        private Long deductionItemId;
        private String itemName;
        private String categoryName;
        private SpaceType spaceType;
        private Long spaceId;
        private String spaceName;
        private List<Long> studentIds;
        private List<String> studentNames;
        private Integer personCount;
        private BigDecimal deductionAmount;
        private InputSource inputSource;
        private String remark;
        private List<String> evidenceUrls;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder sessionId(Long sessionId) { this.sessionId = sessionId; return this; }
        public Builder classRecordId(Long classRecordId) { this.classRecordId = classRecordId; return this; }
        public Builder deductionItemId(Long deductionItemId) { this.deductionItemId = deductionItemId; return this; }
        public Builder itemName(String itemName) { this.itemName = itemName; return this; }
        public Builder categoryName(String categoryName) { this.categoryName = categoryName; return this; }
        public Builder spaceType(SpaceType spaceType) { this.spaceType = spaceType; return this; }
        public Builder spaceId(Long spaceId) { this.spaceId = spaceId; return this; }
        public Builder spaceName(String spaceName) { this.spaceName = spaceName; return this; }
        public Builder studentIds(List<Long> studentIds) { this.studentIds = studentIds; return this; }
        public Builder studentNames(List<String> studentNames) { this.studentNames = studentNames; return this; }
        public Builder personCount(Integer personCount) { this.personCount = personCount; return this; }
        public Builder deductionAmount(BigDecimal deductionAmount) { this.deductionAmount = deductionAmount; return this; }
        public Builder inputSource(InputSource inputSource) { this.inputSource = inputSource; return this; }
        public Builder remark(String remark) { this.remark = remark; return this; }
        public Builder evidenceUrls(List<String> evidenceUrls) { this.evidenceUrls = evidenceUrls; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public InspectionDeduction build() {
            return new InspectionDeduction(this);
        }
    }
}
