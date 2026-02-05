package com.school.management.domain.inspection.model.v6;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * V6检查明细实体
 * 记录每个检查项的具体扣分/加分情况
 */
public class InspectionDetail implements Entity<Long> {

    private Long id;
    private Long targetId;

    // 检查项信息
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private Long itemId;
    private String itemCode;
    private String itemName;

    // 作用范围
    private DetailScope scope;
    private String individualType;  // USER or SPACE
    private Long individualId;
    private String individualName;

    // 打分
    private ScoringMode scoringMode;
    private BigDecimal score;       // 扣分为负,加分为正
    private Integer quantity;
    private BigDecimal totalScore;  // score * quantity

    // 评级结果(GRADE模式)
    private String gradeCode;
    private String gradeName;

    // 清单结果(CHECKLIST模式)
    private Boolean checklistChecked;

    // 备注和证据
    private String remark;
    private List<Long> evidenceIds;

    // 审计
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected InspectionDetail() {
    }

    private InspectionDetail(Builder builder) {
        this.id = builder.id;
        this.targetId = builder.targetId;
        this.categoryId = builder.categoryId;
        this.categoryCode = builder.categoryCode;
        this.categoryName = builder.categoryName;
        this.itemId = builder.itemId;
        this.itemCode = builder.itemCode;
        this.itemName = builder.itemName;
        this.scope = builder.scope != null ? builder.scope : DetailScope.WHOLE;
        this.individualType = builder.individualType;
        this.individualId = builder.individualId;
        this.individualName = builder.individualName;
        this.scoringMode = builder.scoringMode;
        this.score = builder.score;
        this.quantity = builder.quantity != null ? builder.quantity : 1;
        this.totalScore = builder.totalScore != null ? builder.totalScore : calculateTotalScore();
        this.gradeCode = builder.gradeCode;
        this.gradeName = builder.gradeName;
        this.checklistChecked = builder.checklistChecked;
        this.remark = builder.remark;
        this.evidenceIds = builder.evidenceIds;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    /**
     * 创建扣分明细
     */
    public static InspectionDetail createDeduction(Long targetId, Long categoryId, String categoryCode,
                                                    String categoryName, Long itemId, String itemCode,
                                                    String itemName, BigDecimal score, Integer quantity,
                                                    String remark, Long createdBy) {
        return builder()
                .targetId(targetId)
                .categoryId(categoryId)
                .categoryCode(categoryCode)
                .categoryName(categoryName)
                .itemId(itemId)
                .itemCode(itemCode)
                .itemName(itemName)
                .scope(DetailScope.WHOLE)
                .scoringMode(ScoringMode.DEDUCTION)
                .score(score.negate().abs().negate()) // 确保为负数
                .quantity(quantity)
                .remark(remark)
                .createdBy(createdBy)
                .build();
    }

    /**
     * 创建带个体关联的扣分明细
     */
    public static InspectionDetail createDeductionWithIndividual(Long targetId, Long categoryId, String categoryCode,
                                                                   String categoryName, Long itemId, String itemCode,
                                                                   String itemName, BigDecimal score, Integer quantity,
                                                                   String individualType, Long individualId,
                                                                   String individualName, String remark, Long createdBy) {
        return builder()
                .targetId(targetId)
                .categoryId(categoryId)
                .categoryCode(categoryCode)
                .categoryName(categoryName)
                .itemId(itemId)
                .itemCode(itemCode)
                .itemName(itemName)
                .scope(DetailScope.INDIVIDUAL)
                .individualType(individualType)
                .individualId(individualId)
                .individualName(individualName)
                .scoringMode(ScoringMode.DEDUCTION)
                .score(score.negate().abs().negate()) // 确保为负数
                .quantity(quantity)
                .remark(remark)
                .createdBy(createdBy)
                .build();
    }

    /**
     * 创建加分明细
     */
    public static InspectionDetail createBonus(Long targetId, Long categoryId, String categoryCode,
                                                String categoryName, Long itemId, String itemCode,
                                                String itemName, BigDecimal score, Integer quantity,
                                                String remark, Long createdBy) {
        return builder()
                .targetId(targetId)
                .categoryId(categoryId)
                .categoryCode(categoryCode)
                .categoryName(categoryName)
                .itemId(itemId)
                .itemCode(itemCode)
                .itemName(itemName)
                .scope(DetailScope.WHOLE)
                .scoringMode(ScoringMode.ADDITION)
                .score(score.abs()) // 确保为正数
                .quantity(quantity)
                .remark(remark)
                .createdBy(createdBy)
                .build();
    }

    private BigDecimal calculateTotalScore() {
        if (score == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return score.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 更新备注
     */
    public void updateRemark(String remark) {
        this.remark = remark;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新分值
     */
    public void updateScore(BigDecimal score, Integer quantity) {
        this.score = score;
        this.quantity = quantity != null ? quantity : 1;
        this.totalScore = calculateTotalScore();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 添加证据
     */
    public void addEvidence(Long evidenceId) {
        if (this.evidenceIds == null) {
            this.evidenceIds = new java.util.ArrayList<>();
        }
        this.evidenceIds.add(evidenceId);
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTargetId() {
        return targetId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Long getItemId() {
        return itemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public DetailScope getScope() {
        return scope;
    }

    public String getIndividualType() {
        return individualType;
    }

    public Long getIndividualId() {
        return individualId;
    }

    public String getIndividualName() {
        return individualName;
    }

    public ScoringMode getScoringMode() {
        return scoringMode;
    }

    public BigDecimal getScore() {
        return score;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public String getGradeCode() {
        return gradeCode;
    }

    public String getGradeName() {
        return gradeName;
    }

    public Boolean getChecklistChecked() {
        return checklistChecked;
    }

    public String getRemark() {
        return remark;
    }

    public List<Long> getEvidenceIds() {
        return evidenceIds;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long targetId;
        private Long categoryId;
        private String categoryCode;
        private String categoryName;
        private Long itemId;
        private String itemCode;
        private String itemName;
        private DetailScope scope;
        private String individualType;
        private Long individualId;
        private String individualName;
        private ScoringMode scoringMode;
        private BigDecimal score;
        private Integer quantity;
        private BigDecimal totalScore;
        private String gradeCode;
        private String gradeName;
        private Boolean checklistChecked;
        private String remark;
        private List<Long> evidenceIds;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder targetId(Long targetId) { this.targetId = targetId; return this; }
        public Builder categoryId(Long categoryId) { this.categoryId = categoryId; return this; }
        public Builder categoryCode(String categoryCode) { this.categoryCode = categoryCode; return this; }
        public Builder categoryName(String categoryName) { this.categoryName = categoryName; return this; }
        public Builder itemId(Long itemId) { this.itemId = itemId; return this; }
        public Builder itemCode(String itemCode) { this.itemCode = itemCode; return this; }
        public Builder itemName(String itemName) { this.itemName = itemName; return this; }
        public Builder scope(DetailScope scope) { this.scope = scope; return this; }
        public Builder individualType(String individualType) { this.individualType = individualType; return this; }
        public Builder individualId(Long individualId) { this.individualId = individualId; return this; }
        public Builder individualName(String individualName) { this.individualName = individualName; return this; }
        public Builder scoringMode(ScoringMode scoringMode) { this.scoringMode = scoringMode; return this; }
        public Builder score(BigDecimal score) { this.score = score; return this; }
        public Builder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public Builder totalScore(BigDecimal totalScore) { this.totalScore = totalScore; return this; }
        public Builder gradeCode(String gradeCode) { this.gradeCode = gradeCode; return this; }
        public Builder gradeName(String gradeName) { this.gradeName = gradeName; return this; }
        public Builder checklistChecked(Boolean checklistChecked) { this.checklistChecked = checklistChecked; return this; }
        public Builder remark(String remark) { this.remark = remark; return this; }
        public Builder evidenceIds(List<Long> evidenceIds) { this.evidenceIds = evidenceIds; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public InspectionDetail build() {
            return new InspectionDetail(this);
        }
    }
}
