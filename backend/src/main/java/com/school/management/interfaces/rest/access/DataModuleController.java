package com.school.management.interfaces.rest.access;

import com.school.management.application.access.DynamicModuleService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.persistence.access.DataModulePO;
import com.school.management.infrastructure.persistence.access.ScopeItemTypePO;
import com.school.management.infrastructure.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/data-modules")
@RequiredArgsConstructor
public class DataModuleController {

    private final DynamicModuleService dynamicModuleService;

    @GetMapping
    public Result<List<DataModulePO>> listModules() {
        Long tenantId = TenantContextHolder.getTenantId();
        return Result.success(dynamicModuleService.listModules(tenantId));
    }

    @GetMapping("/grouped")
    public Result<Map<String, List<DataModulePO>>> listModulesGrouped() {
        Long tenantId = TenantContextHolder.getTenantId();
        return Result.success(dynamicModuleService.listByDomain(tenantId));
    }

    @PostMapping
    @CasbinAccess(resource = "data_module", action = "create")
    public Result<DataModulePO> create(@RequestBody DataModulePO module) {
        module.setTenantId(TenantContextHolder.getTenantId());
        return Result.success(dynamicModuleService.createModule(module));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "data_module", action = "update")
    public Result<DataModulePO> update(@PathVariable Long id, @RequestBody DataModulePO module) {
        module.setId(id);
        module.setTenantId(TenantContextHolder.getTenantId());
        return Result.success(dynamicModuleService.updateModule(module));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "data_module", action = "delete")
    public Result<Void> delete(@PathVariable Long id) {
        dynamicModuleService.deleteModule(id);
        return Result.success(null);
    }

    @GetMapping("/{moduleCode}/scope-item-types")
    public Result<List<ScopeItemTypePO>> listModuleScopeItemTypes(
            @PathVariable String moduleCode) {
        Long tenantId = TenantContextHolder.getTenantId();
        return Result.success(dynamicModuleService.listScopeItemTypes(tenantId, moduleCode));
    }

    @GetMapping("/scope-item-types")
    public Result<List<ScopeItemTypePO>> listAllScopeItemTypes() {
        Long tenantId = TenantContextHolder.getTenantId();
        return Result.success(dynamicModuleService.listAllScopeItemTypes(tenantId));
    }
}
