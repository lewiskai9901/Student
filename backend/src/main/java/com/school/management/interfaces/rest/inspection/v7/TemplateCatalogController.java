package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.TemplateCatalogApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.template.TemplateCatalog;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v7/insp/catalogs")
@RequiredArgsConstructor
public class TemplateCatalogController {

    private final TemplateCatalogApplicationService catalogService;

    @PostMapping
    @CasbinAccess(resource = "insp:catalog", action = "create")
    public Result<TemplateCatalog> createCatalog(@RequestBody CreateCatalogRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
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
                                                  @RequestBody UpdateCatalogRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
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
        private String catalogCode;
        private String catalogName;
        private Long parentId;
        private String description;
        private String icon;
        private Integer sortOrder;
    }

    @lombok.Data
    public static class UpdateCatalogRequest {
        private String catalogName;
        private String description;
        private Long parentId;
        private String icon;
        private Integer sortOrder;
        private Boolean isEnabled;
    }
}
