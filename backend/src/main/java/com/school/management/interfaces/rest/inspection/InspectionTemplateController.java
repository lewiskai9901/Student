package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.*;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.InspectionTemplate;
import com.school.management.domain.inspection.model.TemplateScope;
import com.school.management.domain.inspection.model.TemplateStatus;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for inspection templates.
 */
@RestController
@RequestMapping("/v2/inspection-templates")
@Tag(name = "Inspection Templates", description = "Inspection template management API")
public class InspectionTemplateController {

    private final InspectionApplicationService inspectionService;
    private final JwtTokenService jwtTokenService;

    public InspectionTemplateController(
            InspectionApplicationService inspectionService,
            JwtTokenService jwtTokenService) {
        this.inspectionService = inspectionService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping
    @Operation(summary = "Create a new inspection template")
    @PreAuthorize("hasAuthority('quantification:template:add')")
    public Result<TemplateResponse> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        CreateTemplateCommand command = CreateTemplateCommand.builder()
            .templateCode(request.getTemplateCode())
            .templateName(request.getTemplateName())
            .description(request.getDescription())
            .scope(request.getScope())
            .applicableOrgUnitId(request.getApplicableOrgUnitId())
            .createdBy(getCurrentUserId())
            .build();

        InspectionTemplate template = inspectionService.createTemplate(command);
        return Result.success(toResponse(template));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID")
    @PreAuthorize("hasAuthority('quantification:template:view')")
    public Result<TemplateResponse> getTemplate(@PathVariable Long id) {
        return inspectionService.getTemplate(id)
            .map(template -> Result.success(toResponse(template)))
            .orElse(Result.error("Template not found"));
    }

    @GetMapping
    @Operation(summary = "List all published templates")
    @PreAuthorize("hasAuthority('quantification:template:view')")
    public Result<List<TemplateResponse>> listPublishedTemplates() {
        List<TemplateResponse> templates = inspectionService.listPublishedTemplates().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return Result.success(templates);
    }

    @PostMapping("/{id}/categories")
    @Operation(summary = "Add a category to template")
    @PreAuthorize("hasAuthority('quantification:template:edit')")
    public Result<TemplateResponse> addCategory(
            @PathVariable Long id,
            @Valid @RequestBody AddCategoryRequest request) {

        AddCategoryCommand command = AddCategoryCommand.builder()
            .categoryCode(request.getCategoryCode())
            .categoryName(request.getCategoryName())
            .description(request.getDescription())
            .baseScore(request.getBaseScore())
            .sortOrder(request.getSortOrder())
            .build();

        InspectionTemplate template = inspectionService.addCategoryToTemplate(id, command);
        return Result.success(toResponse(template));
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "Publish a template")
    @PreAuthorize("hasAuthority('quantification:template:edit')")
    public Result<TemplateResponse> publishTemplate(@PathVariable Long id) {
        InspectionTemplate template = inspectionService.publishTemplate(id);
        return Result.success(toResponse(template));
    }

    @PutMapping("/{id}/set-default")
    @Operation(summary = "Set template as default")
    @PreAuthorize("hasAuthority('quantification:template:edit')")
    public Result<TemplateResponse> setAsDefault(@PathVariable Long id) {
        InspectionTemplate template = inspectionService.setTemplateAsDefault(id);
        return Result.success(toResponse(template));
    }

    private Long getCurrentUserId() {
        return jwtTokenService.getCurrentUserId();
    }

    private TemplateResponse toResponse(InspectionTemplate template) {
        TemplateResponse response = new TemplateResponse();
        response.setId(template.getId());
        response.setTemplateCode(template.getTemplateCode());
        response.setTemplateName(template.getTemplateName());
        response.setDescription(template.getDescription());
        response.setScope(template.getScope());
        response.setApplicableOrgUnitId(template.getApplicableOrgUnitId());
        response.setIsDefault(template.isDefault());
        response.setCurrentVersion(template.getCurrentVersion());
        response.setStatus(template.getStatus());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());
        response.setTotalBaseScore(template.calculateTotalBaseScore());
        response.setCategoryCount(template.getCategories().size());
        return response;
    }
}
