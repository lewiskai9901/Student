package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.TemplateCatalogApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.template.TemplateCatalog;
import com.school.management.infrastructure.casbin.CasbinAccess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inspection/catalogs")
@RequiredArgsConstructor
public class TemplateCatalogController {

    private final TemplateCatalogApplicationService catalogService;

    @PostMapping
    @CasbinAccess(resource = "insp:catalog", action = "create")
    public Result<TemplateCatalog> createCatalog(@RequestBody @Valid CreateCatalogRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.success(catalogService.createCatalog(
                request.getCatalogCode(), request.getCatalogName(),
                request.getParentId(), request.getDescription(),
                request.getIcon(), request.getSortOrder(), userId));
    }

    @GetMapping
    @CasbinAccess(resource = "insp:catalog", action = "view")
    public Result<List<TemplateCatalog>> listCatalogs() {
        return Result.success(catalogService.listAllCatalogs());
    }

    @GetMapping("/tree")
    @CasbinAccess(resource = "insp:catalog", action = "view")
    public Result<List<Map<String, Object>>> getCatalogTree() {
        return Result.success(catalogService.getCatalogTree());
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:catalog", action = "edit")
    public Result<TemplateCatalog> updateCatalog(@PathVariable Long id,
                                                  @RequestBody @Valid UpdateCatalogRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.success(catalogService.updateCatalog(id,
                request.getCatalogName(), request.getDescription(),
                request.getParentId(), request.getIcon(),
                request.getSortOrder(), request.getIsEnabled(), userId));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:catalog", action = "delete")
    public Result<Void> deleteCatalog(@PathVariable Long id) {
        catalogService.deleteCatalog(id);
        return Result.success();
    }

    @lombok.Data
    public static class CreateCatalogRequest {
        @NotBlank
        private String catalogCode;
        @NotBlank
        private String catalogName;
        private Long parentId;
        private String description;
        private String icon;
        private Integer sortOrder;
    }

    @lombok.Data
    public static class UpdateCatalogRequest {
        @NotBlank
        private String catalogName;
        private String description;
        private Long parentId;
        private String icon;
        private Integer sortOrder;
        private Boolean isEnabled;
    }
}
