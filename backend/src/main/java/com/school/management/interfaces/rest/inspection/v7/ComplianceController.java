package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.ComplianceApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.compliance.ComplianceClause;
import com.school.management.domain.inspection.model.v7.compliance.ComplianceStandard;
import com.school.management.domain.inspection.model.v7.compliance.ItemComplianceMapping;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v7/insp")
@RequiredArgsConstructor
public class ComplianceController {

    private final ComplianceApplicationService service;

    // ========== Compliance Standards ==========

    @GetMapping("/compliance-standards")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<List<ComplianceStandard>> listStandards() {
        return Result.success(service.getAllStandards());
    }

    @GetMapping("/compliance-standards/{id}")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<ComplianceStandard> getStandard(@PathVariable Long id) {
        return Result.success(service.getStandardById(id));
    }

    @PostMapping("/compliance-standards")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<ComplianceStandard> createStandard(@RequestBody Map<String, Object> body) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ComplianceStandard result = service.createStandard(
                (String) body.get("standardCode"),
                (String) body.get("standardName"),
                (String) body.get("issuingBody"),
                body.get("effectiveDate") != null ? LocalDate.parse(body.get("effectiveDate").toString()) : null,
                (String) body.get("version"),
                (String) body.get("description"),
                currentUserId
        );
        return Result.success(result);
    }

    @PutMapping("/compliance-standards/{id}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<ComplianceStandard> updateStandard(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ComplianceStandard result = service.updateStandard(id,
                (String) body.get("standardName"),
                (String) body.get("issuingBody"),
                body.get("effectiveDate") != null ? LocalDate.parse(body.get("effectiveDate").toString()) : null,
                (String) body.get("version"),
                (String) body.get("description")
        );
        return Result.success(result);
    }

    @DeleteMapping("/compliance-standards/{id}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> deleteStandard(@PathVariable Long id) {
        service.deleteStandard(id);
        return Result.success(null);
    }

    // ========== Compliance Clauses ==========

    @GetMapping("/compliance-standards/{id}/clauses")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<List<ComplianceClause>> listClauses(@PathVariable Long id) {
        return Result.success(service.getClausesByStandardId(id));
    }

    @PostMapping("/compliance-standards/{id}/clauses")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<ComplianceClause> createClause(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ComplianceClause result = service.createClause(
                id,
                (String) body.get("clauseNumber"),
                (String) body.get("clauseTitle"),
                (String) body.get("clauseContent"),
                body.get("parentClauseId") != null ? Long.valueOf(body.get("parentClauseId").toString()) : null,
                body.get("sortOrder") != null ? Integer.valueOf(body.get("sortOrder").toString()) : 0
        );
        return Result.success(result);
    }

    @DeleteMapping("/compliance-standards/{standardId}/clauses/{clauseId}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> deleteClause(@PathVariable Long standardId, @PathVariable Long clauseId) {
        service.deleteClause(clauseId);
        return Result.success(null);
    }

    // ========== Item Compliance Mappings ==========

    @GetMapping("/template-items/{itemId}/compliance-mappings")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<List<ItemComplianceMapping>> listMappings(@PathVariable Long itemId) {
        return Result.success(service.getMappingsByItemId(itemId));
    }

    @PostMapping("/template-items/{itemId}/compliance-mappings")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<ItemComplianceMapping> createMapping(@PathVariable Long itemId, @RequestBody Map<String, Object> body) {
        ItemComplianceMapping result = service.createMapping(
                itemId,
                Long.valueOf(body.get("clauseId").toString()),
                (String) body.get("coverageLevel"),
                (String) body.get("notes")
        );
        return Result.success(result);
    }

    @DeleteMapping("/template-items/{itemId}/compliance-mappings/{mappingId}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> deleteMapping(@PathVariable Long itemId, @PathVariable Long mappingId) {
        service.deleteMapping(mappingId);
        return Result.success(null);
    }
}
