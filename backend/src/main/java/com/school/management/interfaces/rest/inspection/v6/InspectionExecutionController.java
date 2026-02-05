package com.school.management.interfaces.rest.inspection.v6;

import com.school.management.application.inspection.v6.InspectionExecutionApplicationService;
import com.school.management.application.inspection.v6.InspectionExecutionApplicationService.*;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v6.InspectionDetail;
import com.school.management.domain.inspection.model.v6.InspectionEvidence;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * V6检查执行控制器
 * 管理检查明细和证据的CRUD操作
 */
@Tag(name = "V6检查执行", description = "V6检查执行接口 - 管理检查明细和证据")
@RestController
@RequestMapping("/v6/inspection-execution")
public class InspectionExecutionController {

    private final InspectionExecutionApplicationService executionService;

    public InspectionExecutionController(InspectionExecutionApplicationService executionService) {
        this.executionService = executionService;
    }

    // ========== 检查明细相关 ==========

    @Operation(summary = "添加扣分明细")
    @PostMapping("/details/deduction")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<DetailResponse> addDeduction(
            @RequestBody AddDeductionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        AddDeductionCommand command = new AddDeductionCommand();
        command.setTargetId(request.getTargetId());
        command.setCategoryId(request.getCategoryId());
        command.setCategoryCode(request.getCategoryCode());
        command.setCategoryName(request.getCategoryName());
        command.setItemId(request.getItemId());
        command.setItemCode(request.getItemCode());
        command.setItemName(request.getItemName());
        command.setScore(request.getScore());
        command.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
        command.setRemark(request.getRemark());
        command.setCreatedBy(userDetails.getUserId());

        InspectionDetail detail = executionService.addDeduction(command);
        return Result.success(toDetailResponse(detail));
    }

    @Operation(summary = "添加带个体关联的扣分明细")
    @PostMapping("/details/deduction-individual")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<DetailResponse> addDeductionWithIndividual(
            @RequestBody AddDeductionWithIndividualRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        AddDeductionWithIndividualCommand command = new AddDeductionWithIndividualCommand();
        command.setTargetId(request.getTargetId());
        command.setCategoryId(request.getCategoryId());
        command.setCategoryCode(request.getCategoryCode());
        command.setCategoryName(request.getCategoryName());
        command.setItemId(request.getItemId());
        command.setItemCode(request.getItemCode());
        command.setItemName(request.getItemName());
        command.setScore(request.getScore());
        command.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
        command.setIndividualType(request.getIndividualType());
        command.setIndividualId(request.getIndividualId());
        command.setIndividualName(request.getIndividualName());
        command.setRemark(request.getRemark());
        command.setCreatedBy(userDetails.getUserId());

        InspectionDetail detail = executionService.addDeductionWithIndividual(command);
        return Result.success(toDetailResponse(detail));
    }

    @Operation(summary = "添加加分明细")
    @PostMapping("/details/bonus")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<DetailResponse> addBonus(
            @RequestBody AddBonusRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        AddBonusCommand command = new AddBonusCommand();
        command.setTargetId(request.getTargetId());
        command.setCategoryId(request.getCategoryId());
        command.setCategoryCode(request.getCategoryCode());
        command.setCategoryName(request.getCategoryName());
        command.setItemId(request.getItemId());
        command.setItemCode(request.getItemCode());
        command.setItemName(request.getItemName());
        command.setScore(request.getScore());
        command.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
        command.setRemark(request.getRemark());
        command.setCreatedBy(userDetails.getUserId());

        InspectionDetail detail = executionService.addBonus(command);
        return Result.success(toDetailResponse(detail));
    }

    @Operation(summary = "更新明细")
    @PutMapping("/details/{detailId}")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<DetailResponse> updateDetail(
            @PathVariable Long detailId,
            @RequestBody UpdateDetailRequest request) {

        InspectionDetail detail = executionService.updateDetail(
                detailId, request.getScore(), request.getQuantity(), request.getRemark());
        return Result.success(toDetailResponse(detail));
    }

    @Operation(summary = "删除明细")
    @DeleteMapping("/details/{detailId}")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<Void> deleteDetail(@PathVariable Long detailId) {
        executionService.deleteDetail(detailId);
        return Result.success();
    }

    @Operation(summary = "获取目标的明细列表")
    @GetMapping("/targets/{targetId}/details")
    @PreAuthorize("hasAuthority('inspection:task:view')")
    public Result<List<DetailResponse>> getDetailsByTarget(@PathVariable Long targetId) {
        List<InspectionDetail> details = executionService.getDetailsByTarget(targetId);
        return Result.success(details.stream().map(this::toDetailResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "获取目标某类别的明细")
    @GetMapping("/targets/{targetId}/categories/{categoryId}/details")
    @PreAuthorize("hasAuthority('inspection:task:view')")
    public Result<List<DetailResponse>> getDetailsByTargetAndCategory(
            @PathVariable Long targetId,
            @PathVariable Long categoryId) {
        List<InspectionDetail> details = executionService.getDetailsByTargetAndCategory(targetId, categoryId);
        return Result.success(details.stream().map(this::toDetailResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "获取个体的明细列表")
    @GetMapping("/individuals/{individualType}/{individualId}/details")
    @PreAuthorize("hasAuthority('inspection:task:view')")
    public Result<List<DetailResponse>> getDetailsByIndividual(
            @PathVariable String individualType,
            @PathVariable Long individualId) {
        List<InspectionDetail> details = executionService.getDetailsByIndividual(individualType, individualId);
        return Result.success(details.stream().map(this::toDetailResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "获取明细详情")
    @GetMapping("/details/{detailId}")
    @PreAuthorize("hasAuthority('inspection:task:view')")
    public Result<DetailResponse> getDetail(@PathVariable Long detailId) {
        return executionService.getDetail(detailId)
                .map(d -> Result.success(toDetailResponse(d)))
                .orElse(Result.error(404, "资源不存在"));
    }

    // ========== 证据相关 ==========

    @Operation(summary = "添加明细证据")
    @PostMapping("/details/{detailId}/evidences")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<EvidenceResponse> addEvidenceForDetail(
            @PathVariable Long detailId,
            @RequestBody AddEvidenceRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        AddEvidenceCommand command = new AddEvidenceCommand();
        command.setDetailId(detailId);
        command.setFileName(request.getFileName());
        command.setFilePath(request.getFilePath());
        command.setFileUrl(request.getFileUrl());
        command.setFileSize(request.getFileSize());
        command.setFileType(request.getFileType());
        command.setUploadBy(userDetails.getUserId());

        InspectionEvidence evidence = executionService.addEvidenceForDetail(command);
        return Result.success(toEvidenceResponse(evidence));
    }

    @Operation(summary = "添加目标整体证据")
    @PostMapping("/targets/{targetId}/evidences")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<EvidenceResponse> addEvidenceForTarget(
            @PathVariable Long targetId,
            @RequestBody AddEvidenceRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        AddTargetEvidenceCommand command = new AddTargetEvidenceCommand();
        command.setTargetId(targetId);
        command.setFileName(request.getFileName());
        command.setFilePath(request.getFilePath());
        command.setFileUrl(request.getFileUrl());
        command.setFileSize(request.getFileSize());
        command.setFileType(request.getFileType());
        command.setUploadBy(userDetails.getUserId());

        InspectionEvidence evidence = executionService.addEvidenceForTarget(command);
        return Result.success(toEvidenceResponse(evidence));
    }

    @Operation(summary = "添加带GPS的证据")
    @PostMapping("/evidences/with-location")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<EvidenceResponse> addEvidenceWithLocation(
            @RequestBody AddEvidenceWithLocationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        AddEvidenceWithLocationCommand command = new AddEvidenceWithLocationCommand();
        command.setDetailId(request.getDetailId());
        command.setTargetId(request.getTargetId());
        command.setFileName(request.getFileName());
        command.setFilePath(request.getFilePath());
        command.setFileUrl(request.getFileUrl());
        command.setFileSize(request.getFileSize());
        command.setFileType(request.getFileType());
        command.setLatitude(request.getLatitude());
        command.setLongitude(request.getLongitude());
        command.setUploadBy(userDetails.getUserId());

        InspectionEvidence evidence = executionService.addEvidenceWithLocation(command);
        return Result.success(toEvidenceResponse(evidence));
    }

    @Operation(summary = "删除证据")
    @DeleteMapping("/evidences/{evidenceId}")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<Void> deleteEvidence(@PathVariable Long evidenceId) {
        executionService.deleteEvidence(evidenceId);
        return Result.success();
    }

    @Operation(summary = "获取明细的证据列表")
    @GetMapping("/details/{detailId}/evidences")
    @PreAuthorize("hasAuthority('inspection:task:view')")
    public Result<List<EvidenceResponse>> getEvidencesByDetail(@PathVariable Long detailId) {
        List<InspectionEvidence> evidences = executionService.getEvidencesByDetail(detailId);
        return Result.success(evidences.stream().map(this::toEvidenceResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "获取目标的整体证据列表")
    @GetMapping("/targets/{targetId}/evidences")
    @PreAuthorize("hasAuthority('inspection:task:view')")
    public Result<List<EvidenceResponse>> getEvidencesByTarget(@PathVariable Long targetId) {
        List<InspectionEvidence> evidences = executionService.getEvidencesByTarget(targetId);
        return Result.success(evidences.stream().map(this::toEvidenceResponse).collect(Collectors.toList()));
    }

    // ========== 转换方法 ==========

    private DetailResponse toDetailResponse(InspectionDetail detail) {
        DetailResponse response = new DetailResponse();
        response.setId(detail.getId());
        response.setTargetId(detail.getTargetId());
        response.setCategoryId(detail.getCategoryId());
        response.setCategoryCode(detail.getCategoryCode());
        response.setCategoryName(detail.getCategoryName());
        response.setItemId(detail.getItemId());
        response.setItemCode(detail.getItemCode());
        response.setItemName(detail.getItemName());
        response.setScope(detail.getScope() != null ? detail.getScope().name() : null);
        response.setIndividualType(detail.getIndividualType());
        response.setIndividualId(detail.getIndividualId());
        response.setIndividualName(detail.getIndividualName());
        response.setScoringMode(detail.getScoringMode() != null ? detail.getScoringMode().name() : null);
        response.setScore(detail.getScore());
        response.setQuantity(detail.getQuantity());
        response.setTotalScore(detail.getTotalScore());
        response.setGradeCode(detail.getGradeCode());
        response.setGradeName(detail.getGradeName());
        response.setChecklistChecked(detail.getChecklistChecked());
        response.setRemark(detail.getRemark());
        response.setEvidenceIds(detail.getEvidenceIds());
        response.setCreatedBy(detail.getCreatedBy());
        response.setCreatedAt(detail.getCreatedAt());
        response.setUpdatedAt(detail.getUpdatedAt());
        return response;
    }

    private EvidenceResponse toEvidenceResponse(InspectionEvidence evidence) {
        EvidenceResponse response = new EvidenceResponse();
        response.setId(evidence.getId());
        response.setDetailId(evidence.getDetailId());
        response.setTargetId(evidence.getTargetId());
        response.setFileName(evidence.getFileName());
        response.setFilePath(evidence.getFilePath());
        response.setFileUrl(evidence.getFileUrl());
        response.setFileSize(evidence.getFileSize());
        response.setFileType(evidence.getFileType());
        response.setLatitude(evidence.getLatitude());
        response.setLongitude(evidence.getLongitude());
        response.setUploadBy(evidence.getUploadBy());
        response.setUploadTime(evidence.getUploadTime());
        response.setCreatedAt(evidence.getCreatedAt());
        return response;
    }

    // ========== DTO类 ==========

    public static class AddDeductionRequest {
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
    }

    public static class AddDeductionWithIndividualRequest extends AddDeductionRequest {
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

    public static class AddBonusRequest extends AddDeductionRequest {
        // Same structure
    }

    public static class UpdateDetailRequest {
        private BigDecimal score;
        private Integer quantity;
        private String remark;

        public BigDecimal getScore() { return score; }
        public void setScore(BigDecimal score) { this.score = score; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class AddEvidenceRequest {
        private String fileName;
        private String filePath;
        private String fileUrl;
        private Long fileSize;
        private String fileType;

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
    }

    public static class AddEvidenceWithLocationRequest extends AddEvidenceRequest {
        private Long detailId;
        private Long targetId;
        private BigDecimal latitude;
        private BigDecimal longitude;

        public Long getDetailId() { return detailId; }
        public void setDetailId(Long detailId) { this.detailId = detailId; }
        public Long getTargetId() { return targetId; }
        public void setTargetId(Long targetId) { this.targetId = targetId; }
        public BigDecimal getLatitude() { return latitude; }
        public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
        public BigDecimal getLongitude() { return longitude; }
        public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    }

    public static class DetailResponse {
        private Long id;
        private Long targetId;
        private Long categoryId;
        private String categoryCode;
        private String categoryName;
        private Long itemId;
        private String itemCode;
        private String itemName;
        private String scope;
        private String individualType;
        private Long individualId;
        private String individualName;
        private String scoringMode;
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

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
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
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
        public String getIndividualType() { return individualType; }
        public void setIndividualType(String individualType) { this.individualType = individualType; }
        public Long getIndividualId() { return individualId; }
        public void setIndividualId(Long individualId) { this.individualId = individualId; }
        public String getIndividualName() { return individualName; }
        public void setIndividualName(String individualName) { this.individualName = individualName; }
        public String getScoringMode() { return scoringMode; }
        public void setScoringMode(String scoringMode) { this.scoringMode = scoringMode; }
        public BigDecimal getScore() { return score; }
        public void setScore(BigDecimal score) { this.score = score; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getTotalScore() { return totalScore; }
        public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
        public String getGradeCode() { return gradeCode; }
        public void setGradeCode(String gradeCode) { this.gradeCode = gradeCode; }
        public String getGradeName() { return gradeName; }
        public void setGradeName(String gradeName) { this.gradeName = gradeName; }
        public Boolean getChecklistChecked() { return checklistChecked; }
        public void setChecklistChecked(Boolean checklistChecked) { this.checklistChecked = checklistChecked; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
        public List<Long> getEvidenceIds() { return evidenceIds; }
        public void setEvidenceIds(List<Long> evidenceIds) { this.evidenceIds = evidenceIds; }
        public Long getCreatedBy() { return createdBy; }
        public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class EvidenceResponse {
        private Long id;
        private Long detailId;
        private Long targetId;
        private String fileName;
        private String filePath;
        private String fileUrl;
        private Long fileSize;
        private String fileType;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private Long uploadBy;
        private LocalDateTime uploadTime;
        private LocalDateTime createdAt;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getDetailId() { return detailId; }
        public void setDetailId(Long detailId) { this.detailId = detailId; }
        public Long getTargetId() { return targetId; }
        public void setTargetId(Long targetId) { this.targetId = targetId; }
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
        public BigDecimal getLatitude() { return latitude; }
        public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
        public BigDecimal getLongitude() { return longitude; }
        public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
        public Long getUploadBy() { return uploadBy; }
        public void setUploadBy(Long uploadBy) { this.uploadBy = uploadBy; }
        public LocalDateTime getUploadTime() { return uploadTime; }
        public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
