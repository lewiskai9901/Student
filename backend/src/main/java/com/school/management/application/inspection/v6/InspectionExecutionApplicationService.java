package com.school.management.application.inspection.v6;

import com.school.management.domain.inspection.model.v6.*;
import com.school.management.domain.inspection.repository.v6.InspectionDetailRepository;
import com.school.management.domain.inspection.repository.v6.InspectionEvidenceRepository;
import com.school.management.domain.inspection.repository.v6.InspectionTargetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * V6检查执行应用服务
 * 管理检查明细和证据
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class InspectionExecutionApplicationService {

    private final InspectionDetailRepository detailRepository;
    private final InspectionEvidenceRepository evidenceRepository;
    private final InspectionTargetRepository targetRepository;

    // ========== 检查明细操作 ==========

    /**
     * 添加扣分明细
     */
    @Transactional
    public InspectionDetail addDeduction(AddDeductionCommand command) {
        InspectionDetail detail = InspectionDetail.createDeduction(
                command.getTargetId(),
                command.getCategoryId(),
                command.getCategoryCode(),
                command.getCategoryName(),
                command.getItemId(),
                command.getItemCode(),
                command.getItemName(),
                command.getScore(),
                command.getQuantity(),
                command.getRemark(),
                command.getCreatedBy()
        );

        InspectionDetail saved = detailRepository.save(detail);

        // 更新目标的扣分总计
        BigDecimal totalDeduction = detail.getTotalScore().abs();
        targetRepository.addDeduction(command.getTargetId(), totalDeduction);

        log.info("Added deduction detail {} for target {}, total: {}",
                saved.getId(), command.getTargetId(), totalDeduction);

        return saved;
    }

    /**
     * 添加带个体关联的扣分明细
     */
    @Transactional
    public InspectionDetail addDeductionWithIndividual(AddDeductionWithIndividualCommand command) {
        InspectionDetail detail = InspectionDetail.createDeductionWithIndividual(
                command.getTargetId(),
                command.getCategoryId(),
                command.getCategoryCode(),
                command.getCategoryName(),
                command.getItemId(),
                command.getItemCode(),
                command.getItemName(),
                command.getScore(),
                command.getQuantity(),
                command.getIndividualType(),
                command.getIndividualId(),
                command.getIndividualName(),
                command.getRemark(),
                command.getCreatedBy()
        );

        InspectionDetail saved = detailRepository.save(detail);

        // 更新目标的扣分总计
        BigDecimal totalDeduction = detail.getTotalScore().abs();
        targetRepository.addDeduction(command.getTargetId(), totalDeduction);

        log.info("Added individual deduction detail {} for target {}, individual: {} {}",
                saved.getId(), command.getTargetId(), command.getIndividualType(), command.getIndividualId());

        return saved;
    }

    /**
     * 添加加分明细
     */
    @Transactional
    public InspectionDetail addBonus(AddBonusCommand command) {
        InspectionDetail detail = InspectionDetail.createBonus(
                command.getTargetId(),
                command.getCategoryId(),
                command.getCategoryCode(),
                command.getCategoryName(),
                command.getItemId(),
                command.getItemCode(),
                command.getItemName(),
                command.getScore(),
                command.getQuantity(),
                command.getRemark(),
                command.getCreatedBy()
        );

        InspectionDetail saved = detailRepository.save(detail);

        // 更新目标的加分总计
        BigDecimal totalBonus = detail.getTotalScore();
        targetRepository.addBonus(command.getTargetId(), totalBonus);

        log.info("Added bonus detail {} for target {}, total: {}",
                saved.getId(), command.getTargetId(), totalBonus);

        return saved;
    }

    /**
     * 更新明细
     */
    @Transactional
    public InspectionDetail updateDetail(Long detailId, BigDecimal score, Integer quantity, String remark) {
        InspectionDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("明细不存在: " + detailId));

        BigDecimal oldTotalScore = detail.getTotalScore();
        detail.updateScore(score, quantity);
        detail.updateRemark(remark);

        InspectionDetail saved = detailRepository.save(detail);

        // 计算分数变化并更新目标
        if (saved.getScoringMode() == ScoringMode.DEDUCTION) {
            // deduction_total stores positive values; compute delta using abs to stay consistent
            BigDecimal deductionDiff = saved.getTotalScore().abs().subtract(oldTotalScore.abs());
            if (deductionDiff.compareTo(BigDecimal.ZERO) != 0) {
                targetRepository.addDeduction(saved.getTargetId(), deductionDiff);
            }
        } else if (saved.getScoringMode() == ScoringMode.ADDITION) {
            BigDecimal scoreDiff = saved.getTotalScore().subtract(oldTotalScore);
            if (scoreDiff.compareTo(BigDecimal.ZERO) != 0) {
                targetRepository.addBonus(saved.getTargetId(), scoreDiff);
            }
        }

        return saved;
    }

    /**
     * 删除明细
     */
    @Transactional
    public void deleteDetail(Long detailId) {
        InspectionDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("明细不存在: " + detailId));

        // 回滚分数
        if (detail.getScoringMode() == ScoringMode.DEDUCTION) {
            targetRepository.addDeduction(detail.getTargetId(), detail.getTotalScore().abs().negate());
        } else if (detail.getScoringMode() == ScoringMode.ADDITION) {
            targetRepository.addBonus(detail.getTargetId(), detail.getTotalScore().negate());
        }

        // 删除关联的证据
        evidenceRepository.deleteByDetailId(detailId);

        // 删除明细
        detailRepository.deleteById(detailId);

        log.info("Deleted detail {} for target {}", detailId, detail.getTargetId());
    }

    /**
     * 获取目标的所有明细
     */
    public List<InspectionDetail> getDetailsByTarget(Long targetId) {
        return detailRepository.findByTargetId(targetId);
    }

    /**
     * 获取目标某类别的明细
     */
    public List<InspectionDetail> getDetailsByTargetAndCategory(Long targetId, Long categoryId) {
        return detailRepository.findByTargetIdAndCategoryId(targetId, categoryId);
    }

    /**
     * 获取个体的所有明细
     */
    public List<InspectionDetail> getDetailsByIndividual(String individualType, Long individualId) {
        return detailRepository.findByIndividual(individualType, individualId);
    }

    /**
     * 获取明细详情
     */
    public Optional<InspectionDetail> getDetail(Long detailId) {
        return detailRepository.findById(detailId);
    }

    // ========== 证据操作 ==========

    /**
     * 添加明细证据
     */
    @Transactional
    public InspectionEvidence addEvidenceForDetail(AddEvidenceCommand command) {
        InspectionEvidence evidence = InspectionEvidence.createForDetail(
                command.getDetailId(),
                command.getFileName(),
                command.getFilePath(),
                command.getFileUrl(),
                command.getFileSize(),
                command.getFileType(),
                command.getUploadBy()
        );

        InspectionEvidence saved = evidenceRepository.save(evidence);

        // 更新明细的证据ID列表
        if (command.getDetailId() != null) {
            detailRepository.findById(command.getDetailId())
                    .ifPresent(detail -> {
                        detail.addEvidence(saved.getId());
                        detailRepository.save(detail);
                    });
        }

        log.info("Added evidence {} for detail {}", saved.getId(), command.getDetailId());

        return saved;
    }

    /**
     * 添加目标整体证据
     */
    @Transactional
    public InspectionEvidence addEvidenceForTarget(AddTargetEvidenceCommand command) {
        InspectionEvidence evidence = InspectionEvidence.createForTarget(
                command.getTargetId(),
                command.getFileName(),
                command.getFilePath(),
                command.getFileUrl(),
                command.getFileSize(),
                command.getFileType(),
                command.getUploadBy()
        );

        InspectionEvidence saved = evidenceRepository.save(evidence);

        log.info("Added evidence {} for target {}", saved.getId(), command.getTargetId());

        return saved;
    }

    /**
     * 添加带GPS的证据
     */
    @Transactional
    public InspectionEvidence addEvidenceWithLocation(AddEvidenceWithLocationCommand command) {
        InspectionEvidence evidence = InspectionEvidence.createWithLocation(
                command.getDetailId(),
                command.getTargetId(),
                command.getFileName(),
                command.getFilePath(),
                command.getFileUrl(),
                command.getFileSize(),
                command.getFileType(),
                command.getLatitude(),
                command.getLongitude(),
                command.getUploadBy()
        );

        InspectionEvidence saved = evidenceRepository.save(evidence);

        // 更新明细的证据ID列表
        if (command.getDetailId() != null) {
            detailRepository.findById(command.getDetailId())
                    .ifPresent(detail -> {
                        detail.addEvidence(saved.getId());
                        detailRepository.save(detail);
                    });
        }

        log.info("Added evidence with location {} for detail/target {}/{}",
                saved.getId(), command.getDetailId(), command.getTargetId());

        return saved;
    }

    /**
     * 删除证据
     */
    @Transactional
    public void deleteEvidence(Long evidenceId) {
        evidenceRepository.deleteById(evidenceId);
        log.info("Deleted evidence {}", evidenceId);
    }

    /**
     * 获取明细的证据
     */
    public List<InspectionEvidence> getEvidencesByDetail(Long detailId) {
        return evidenceRepository.findByDetailId(detailId);
    }

    /**
     * 获取目标的整体证据
     */
    public List<InspectionEvidence> getEvidencesByTarget(Long targetId) {
        return evidenceRepository.findByTargetId(targetId);
    }

    /**
     * 获取证据详情
     */
    public Optional<InspectionEvidence> getEvidence(Long evidenceId) {
        return evidenceRepository.findById(evidenceId);
    }

    // ========== 命令类 ==========

    public static class AddDeductionCommand {
        private Long targetId;
        private Long categoryId;
        private String categoryCode;
        private String categoryName;
        private Long itemId;
        private String itemCode;
        private String itemName;
        private BigDecimal score;
        private Integer quantity;
        private String remark;
        private Long createdBy;

        // Getters and Setters
        public Long getTargetId() { return targetId; }
        public void setTargetId(Long targetId) { this.targetId = targetId; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getCategoryCode() { return categoryCode; }
        public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        public String getItemCode() { return itemCode; }
        public void setItemCode(String itemCode) { this.itemCode = itemCode; }
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public BigDecimal getScore() { return score; }
        public void setScore(BigDecimal score) { this.score = score; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
        public Long getCreatedBy() { return createdBy; }
        public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    }

    public static class AddDeductionWithIndividualCommand extends AddDeductionCommand {
        private String individualType;
        private Long individualId;
        private String individualName;

        public String getIndividualType() { return individualType; }
        public void setIndividualType(String individualType) { this.individualType = individualType; }
        public Long getIndividualId() { return individualId; }
        public void setIndividualId(Long individualId) { this.individualId = individualId; }
        public String getIndividualName() { return individualName; }
        public void setIndividualName(String individualName) { this.individualName = individualName; }
    }

    public static class AddBonusCommand extends AddDeductionCommand {
        // Same structure as deduction
    }

    public static class AddEvidenceCommand {
        private Long detailId;
        private String fileName;
        private String filePath;
        private String fileUrl;
        private Long fileSize;
        private String fileType;
        private Long uploadBy;

        // Getters and Setters
        public Long getDetailId() { return detailId; }
        public void setDetailId(Long detailId) { this.detailId = detailId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public String getFileUrl() { return fileUrl; }
        public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        public Long getUploadBy() { return uploadBy; }
        public void setUploadBy(Long uploadBy) { this.uploadBy = uploadBy; }
    }

    public static class AddTargetEvidenceCommand extends AddEvidenceCommand {
        private Long targetId;

        public Long getTargetId() { return targetId; }
        public void setTargetId(Long targetId) { this.targetId = targetId; }
    }

    public static class AddEvidenceWithLocationCommand extends AddTargetEvidenceCommand {
        private BigDecimal latitude;
        private BigDecimal longitude;

        public BigDecimal getLatitude() { return latitude; }
        public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
        public BigDecimal getLongitude() { return longitude; }
        public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    }
}
