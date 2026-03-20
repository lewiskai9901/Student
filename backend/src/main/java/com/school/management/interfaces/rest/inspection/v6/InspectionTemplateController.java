package com.school.management.interfaces.rest.inspection.v6;

import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v6.InspectionTemplate;
import com.school.management.domain.inspection.repository.v6.InspectionTemplateRepository;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 检查模板管理控制器
 */
@RequiredArgsConstructor
@Tag(name = "检查模板管理", description = "检查模板CRUD接口")
@RestController
@RequestMapping("/inspection-templates")
public class InspectionTemplateController {

    private final InspectionTemplateRepository templateRepository;

    @Operation(summary = "获取所有模板")
    @GetMapping
    @CasbinAccess(resource = "inspection:template", action = "view")
    public Result<List<InspectionTemplate>> getAll() {
        return Result.success(templateRepository.findAll());
    }

    @Operation(summary = "获取模板详情")
    @GetMapping("/{id}")
    @CasbinAccess(resource = "inspection:template", action = "view")
    public Result<InspectionTemplate> getById(@PathVariable Long id) {
        return templateRepository.findById(id)
                .map(Result::success)
                .orElse(Result.error(404, "模板不存在"));
    }

    @Operation(summary = "创建模板")
    @PostMapping
    @CasbinAccess(resource = "inspection:template", action = "edit")
    public Result<InspectionTemplate> create(
            @RequestBody CreateTemplateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        InspectionTemplate template = new InspectionTemplate();
        template.setTemplateCode(request.getTemplateCode());
        template.setTemplateName(request.getTemplateName());
        template.setDescription(request.getDescription());
        template.setApplicableScope(request.getApplicableScope() != null ? request.getApplicableScope() : "ALL");
        template.setStatus("DRAFT");
        template.setIsDefault(false);
        template.setCurrentVersion(1);
        template.setUseCount(0);
        template.setCreatedBy(userDetails.getUserId());
        template.setUpdatedBy(userDetails.getUserId());

        template = templateRepository.save(template);
        return Result.success(template);
    }

    @Operation(summary = "更新模板")
    @PutMapping("/{id}")
    @CasbinAccess(resource = "inspection:template", action = "edit")
    public Result<InspectionTemplate> update(
            @PathVariable Long id,
            @RequestBody UpdateTemplateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        InspectionTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));

        if (request.getTemplateName() != null) template.setTemplateName(request.getTemplateName());
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getApplicableScope() != null) template.setApplicableScope(request.getApplicableScope());
        template.setUpdatedBy(userDetails.getUserId());

        template = templateRepository.save(template);
        return Result.success(template);
    }

    @Operation(summary = "发布模板")
    @PutMapping("/{id}/publish")
    @CasbinAccess(resource = "inspection:template", action = "edit")
    public Result<InspectionTemplate> publish(@PathVariable Long id) {
        InspectionTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));

        template.setStatus("PUBLISHED");
        template = templateRepository.save(template);
        return Result.success(template);
    }

    @Operation(summary = "按适用范围查询模板")
    @GetMapping("/by-scope")
    @CasbinAccess(resource = "inspection:template", action = "view")
    public Result<List<InspectionTemplate>> getByScope(@RequestParam String scopeType) {
        List<InspectionTemplate> all = templateRepository.findAll();
        List<InspectionTemplate> filtered = all.stream()
                .filter(t -> {
                    String scope = t.getApplicableScope();
                    return scope == null || "ALL".equals(scope) || scope.contains(scopeType);
                })
                .collect(Collectors.toList());
        return Result.success(filtered);
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "inspection:template", action = "edit")
    public Result<Void> delete(@PathVariable Long id) {
        templateRepository.deleteById(id);
        return Result.success();
    }

    // ========== Request DTOs ==========

    public static class CreateTemplateRequest {
        private String templateCode;
        private String templateName;
        private String description;
        private String applicableScope;

        public String getTemplateCode() { return templateCode; }
        public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
        public String getTemplateName() { return templateName; }
        public void setTemplateName(String templateName) { this.templateName = templateName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getApplicableScope() { return applicableScope; }
        public void setApplicableScope(String applicableScope) { this.applicableScope = applicableScope; }
    }

    public static class UpdateTemplateRequest {
        private String templateName;
        private String description;
        private String applicableScope;

        public String getTemplateName() { return templateName; }
        public void setTemplateName(String templateName) { this.templateName = templateName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getApplicableScope() { return applicableScope; }
        public void setApplicableScope(String applicableScope) { this.applicableScope = applicableScope; }
    }
}
