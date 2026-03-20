package com.school.management.interfaces.rest.access;

import com.school.management.application.access.TenantApplicationService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.persistence.tenant.TenantPO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantApplicationService tenantApplicationService;

    @GetMapping
    @CasbinAccess(resource = "tenant", action = "view")
    public Result<List<TenantPO>> list() {
        return Result.success(tenantApplicationService.list());
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "tenant", action = "view")
    public Result<TenantPO> getById(@PathVariable Long id) {
        return Result.success(tenantApplicationService.getById(id));
    }

    @PostMapping
    @CasbinAccess(resource = "tenant", action = "create")
    public Result<TenantPO> create(@RequestBody TenantPO tenant) {
        return Result.success(tenantApplicationService.create(tenant));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "tenant", action = "update")
    public Result<TenantPO> update(@PathVariable Long id, @RequestBody TenantPO tenant) {
        tenant.setId(id);
        return Result.success(tenantApplicationService.update(tenant));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "tenant", action = "delete")
    public Result<Void> delete(@PathVariable Long id) {
        tenantApplicationService.delete(id);
        return Result.success(null);
    }
}
