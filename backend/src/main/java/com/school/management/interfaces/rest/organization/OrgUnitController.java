package com.school.management.interfaces.rest.organization;

import com.school.management.application.organization.OrgMemberService;
import com.school.management.application.organization.OrgUnitApplicationService;
import com.school.management.application.organization.command.CreateOrgUnitCommand;
import com.school.management.application.organization.command.UpdateOrgUnitCommand;
import com.school.management.application.organization.query.OrgMemberDTO;
import com.school.management.application.organization.query.OrgStatisticsDTO;
import com.school.management.application.organization.query.OrgUnitDTO;
import com.school.management.application.organization.query.OrgUnitTreeDTO;
import com.school.management.common.result.Result;
import com.school.management.domain.organization.model.entity.OrgType;
import com.school.management.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API controller for organization unit management.
 */
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Organization Units", description = "Organization unit management API")
@RestController("orgUnitController")
@RequestMapping("/org-units")
public class OrgUnitController {

    private final OrgUnitApplicationService orgUnitService;
    private final OrgMemberService orgMemberService;
    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "Create organization unit")
    @PostMapping
    @CasbinAccess(resource = "system:org", action = "create")
    public Result<OrgUnitDTO> createOrgUnit(@Valid @RequestBody CreateOrgUnitRequest request) {
        CreateOrgUnitCommand command = CreateOrgUnitCommand.builder()
            .unitCode(request.getUnitCode())
            .unitName(request.getUnitName())
            .unitType(request.getUnitType())
            .parentId(request.getParentId())
            .createdBy(SecurityUtils.requireCurrentUserId())
            .attributes(request.getAttributes())
            .build();

        OrgUnitDTO result = orgUnitService.createOrgUnit(command);
        return Result.success(result);
    }

    @Operation(summary = "Update organization unit")
    @PutMapping("/{id}")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<OrgUnitDTO> updateOrgUnit(
            @Parameter(description = "Organization unit ID") @PathVariable Long id,
            @Valid @RequestBody UpdateOrgUnitRequest request) {
        UpdateOrgUnitCommand command = UpdateOrgUnitCommand.builder()
            .unitName(request.getUnitName())
            .sortOrder(request.getSortOrder())
            .headcount(request.getHeadcount())
            .attributes(request.getAttributes())
            .reason(request.getReason())
            .updatedBy(SecurityUtils.requireCurrentUserId())
            .build();

        OrgUnitDTO result = orgUnitService.updateOrgUnit(id, command);
        return Result.success(result);
    }

    @Operation(summary = "Get organization unit")
    @GetMapping("/{id}")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<OrgUnitDTO> getOrgUnit(
            @Parameter(description = "Organization unit ID") @PathVariable Long id) {
        OrgUnitDTO result = orgUnitService.getOrgUnit(id);
        return Result.success(result);
    }

    @Operation(summary = "Get organization tree")
    @GetMapping("/tree")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgUnitTreeDTO>> getOrgUnitTree() {
        List<OrgUnitTreeDTO> result = orgUnitService.getOrgUnitTree();
        return Result.success(result);
    }

    @Operation(summary = "Repair all tree paths")
    @PostMapping("/repair-tree-paths")
    @CasbinAccess(resource = "system:org", action = "edit")
    public Result<Integer> repairTreePaths() {
        int count = orgUnitService.repairTreePaths();
        return Result.success(count);
    }

    @Operation(summary = "Get by type")
    @GetMapping("/by-type/{type}")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgUnitDTO>> getByType(
            @Parameter(description = "Organization type code") @PathVariable String type) {
        List<OrgUnitDTO> result = orgUnitService.getOrgUnitsByType(type);
        return Result.success(result);
    }

    @Operation(summary = "Get allowed child types")
    @GetMapping("/allowed-child-types/{parentTypeCode}")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgType>> getAllowedChildTypes(
            @Parameter(description = "Parent type code") @PathVariable String parentTypeCode) {
        List<OrgType> result = orgUnitService.getAllowedChildTypes(parentTypeCode);
        return Result.success(result);
    }

    @Operation(summary = "Get all organization units")
    @GetMapping
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgUnitDTO>> getAll() {
        List<OrgUnitTreeDTO> tree = orgUnitService.getOrgUnitTree();
        List<OrgUnitDTO> list = new ArrayList<>();
        flattenTree(tree, list);
        return Result.success(list);
    }

    private void flattenTree(List<OrgUnitTreeDTO> nodes, List<OrgUnitDTO> result) {
        if (nodes == null) return;
        for (OrgUnitTreeDTO node : nodes) {
            OrgUnitDTO dto = new OrgUnitDTO();
            dto.setId(node.getId());
            dto.setUnitCode(node.getUnitCode());
            dto.setUnitName(node.getUnitName());
            dto.setUnitType(node.getUnitType());
            dto.setCategory(node.getCategory());
            dto.setTypeName(node.getTypeName());
            dto.setTypeIcon(node.getTypeIcon());
            dto.setTypeColor(node.getTypeColor());
            dto.setParentId(node.getParentId());
            dto.setStatus(node.getStatus());
            dto.setStatusLabel(node.getStatusLabel());
            dto.setHeadcount(node.getHeadcount());
            dto.setAttributes(node.getAttributes());
            result.add(dto);
            flattenTree(node.getChildren(), result);
        }
    }

    @Operation(summary = "Get children")
    @GetMapping("/{id}/children")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgUnitDTO>> getChildren(
            @Parameter(description = "Parent ID") @PathVariable Long id) {
        List<OrgUnitDTO> result = orgUnitService.getChildren(id);
        return Result.success(result);
    }

    @Operation(summary = "Org unit impact analysis — 删除/合并前查依赖统计 (P8-4)",
        description = "Returns counts of dependent entities (descendants, students, classes, " +
                      "teachers, places, access_relations) under this org_unit subtree. " +
                      "Used by admin UI to warn before destructive operations.")
    @GetMapping("/{id}/impact")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<Map<String, Object>> getImpact(
            @Parameter(description = "Organization unit ID") @PathVariable Long id) {
        Map<String, Object> impact = new LinkedHashMap<>();
        impact.put("orgUnitId", id);

        // 1. 组织树后代数 (走 tree_path 子树)
        String treePath;
        try {
            treePath = jdbcTemplate.queryForObject(
                "SELECT tree_path FROM org_units WHERE id = ? AND deleted = 0",
                String.class, id);
        } catch (Exception e) {
            return Result.error(404, "OrgUnit not found: " + id);
        }
        if (treePath == null) treePath = "";

        Integer descendants = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM org_units WHERE deleted = 0 AND tree_path LIKE ? AND id != ?",
            Integer.class, treePath + "%", id);
        impact.put("descendantOrgCount", descendants != null ? descendants : 0);

        // 2. 子树下的 students (含本节点 + 后代)
        Integer studentCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_student WHERE deleted = 0 AND org_unit_id IN " +
            "(SELECT id FROM org_units WHERE deleted = 0 AND tree_path LIKE ?)",
            Integer.class, treePath + "%");
        impact.put("studentCount", studentCount != null ? studentCount : 0);

        // 3. 子树下的 classes
        Integer classCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM classes WHERE deleted = 0 AND org_unit_id IN " +
            "(SELECT id FROM org_units WHERE deleted = 0 AND tree_path LIKE ?)",
            Integer.class, treePath + "%");
        impact.put("classCount", classCount != null ? classCount : 0);

        // 4. 子树下的 places
        Integer placeCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM places WHERE deleted = 0 AND org_unit_id IN " +
            "(SELECT id FROM org_units WHERE deleted = 0 AND tree_path LIKE ?)",
            Integer.class, treePath + "%");
        impact.put("placeCount", placeCount != null ? placeCount : 0);

        // 5. 子树下的 teachers (primary_org_unit_id 在子树)
        Integer teacherCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE deleted = 0 AND user_type_code = 'TEACHER' " +
            "AND primary_org_unit_id IN " +
            "(SELECT id FROM org_units WHERE deleted = 0 AND tree_path LIKE ?)",
            Integer.class, treePath + "%");
        impact.put("teacherCount", teacherCount != null ? teacherCount : 0);

        // 6. access_relations 引用本子树 org_unit 的关系数
        Integer relationCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM access_relations WHERE deleted = 0 " +
            "AND resource_type = 'org_unit' AND resource_id IN " +
            "(SELECT id FROM org_units WHERE deleted = 0 AND tree_path LIKE ?)",
            Integer.class, treePath + "%");
        impact.put("accessRelationCount", relationCount != null ? relationCount : 0);

        // 7. 警告级别 (用于 UI 是否显示二次确认)
        int total = (descendants != null ? descendants : 0)
                  + (studentCount != null ? studentCount : 0)
                  + (classCount != null ? classCount : 0)
                  + (teacherCount != null ? teacherCount : 0);
        String severity = total == 0 ? "NONE"
                        : total < 10 ? "LOW"
                        : total < 100 ? "MEDIUM"
                        : "HIGH";
        impact.put("severity", severity);
        impact.put("totalAffected", total);

        return Result.success(impact);
    }

    @Operation(summary = "Delete organization unit")
    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "system:org", action = "delete")
    public Result<Void> deleteOrgUnit(
            @Parameter(description = "Organization unit ID") @PathVariable Long id) {
        orgUnitService.deleteOrgUnit(id);
        return Result.success();
    }

    @Operation(summary = "Freeze organization unit")
    @PutMapping("/{id}/freeze")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<OrgUnitDTO> freezeOrgUnit(
            @Parameter(description = "Organization unit ID") @PathVariable Long id,
            @RequestBody(required = false) ReasonRequest request) {
        String reason = request != null ? request.getReason() : null;
        OrgUnitDTO result = orgUnitService.freezeOrgUnit(id, reason, SecurityUtils.requireCurrentUserId());
        return Result.success(result);
    }

    @Operation(summary = "Unfreeze organization unit")
    @PutMapping("/{id}/unfreeze")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<OrgUnitDTO> unfreezeOrgUnit(
            @Parameter(description = "Organization unit ID") @PathVariable Long id) {
        OrgUnitDTO result = orgUnitService.unfreezeOrgUnit(id, SecurityUtils.requireCurrentUserId());
        return Result.success(result);
    }

    @Operation(summary = "Dissolve organization unit")
    @PutMapping("/{id}/dissolve")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<OrgUnitDTO> dissolveOrgUnit(
            @Parameter(description = "Organization unit ID") @PathVariable Long id,
            @RequestBody ReasonRequest request) {
        OrgUnitDTO result = orgUnitService.dissolveOrgUnit(id, request.getReason(), SecurityUtils.requireCurrentUserId());
        return Result.success(result);
    }

    @Operation(summary = "Merge org unit into another")
    @PostMapping("/{sourceId}/merge-into/{targetId}")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<OrgUnitDTO> mergeOrgUnit(
            @Parameter(description = "Source org unit ID") @PathVariable Long sourceId,
            @Parameter(description = "Target org unit ID") @PathVariable Long targetId,
            @RequestBody(required = false) ReasonRequest request) {
        String reason = request != null ? request.getReason() : null;
        OrgUnitDTO result = orgUnitService.mergeOrgUnit(sourceId, targetId, reason, SecurityUtils.requireCurrentUserId());
        return Result.success(result);
    }

    @Operation(summary = "Split org unit into new units")
    @PostMapping("/{id}/split")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<List<OrgUnitDTO>> splitOrgUnit(
            @Parameter(description = "Source org unit ID") @PathVariable Long id,
            @RequestBody SplitOrgRequest request) {
        List<OrgUnitApplicationService.SplitRequest> splits = request.getSplits();
        List<OrgUnitDTO> result = orgUnitService.splitOrgUnit(id, splits, request.getReason(), SecurityUtils.requireCurrentUserId());
        return Result.success(result);
    }

    // ==================== Member management ====================

    @Operation(summary = "Get belonging members")
    @GetMapping("/{id}/members")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgMemberDTO>> getBelongingMembers(
            @Parameter(description = "Organization unit ID") @PathVariable Long id) {
        return Result.success(orgMemberService.getBelongingMembers(id));
    }

    @Operation(summary = "Get members recursively (this org + descendants)")
    @GetMapping("/{id}/members/recursive")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgMemberDTO>> getMembersRecursive(
            @Parameter(description = "Organization unit ID") @PathVariable Long id) {
        return Result.success(orgMemberService.getMembersRecursive(id));
    }

    @Operation(summary = "Get organization statistics")
    @GetMapping("/{id}/statistics")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<OrgStatisticsDTO> getOrgStatistics(
            @Parameter(description = "Organization unit ID") @PathVariable Long id) {
        return Result.success(orgMemberService.getOrgStatistics(id));
    }

    @Operation(summary = "Add member to organization")
    @PostMapping("/{id}/members/{userId}")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<Void> addMember(
            @Parameter(description = "Organization unit ID") @PathVariable Long id,
            @Parameter(description = "User ID") @PathVariable Long userId) {
        orgMemberService.addMember(id, userId);
        return Result.success();
    }

    @Operation(summary = "Remove member from organization")
    @DeleteMapping("/{id}/members/{userId}")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<Void> removeMember(
            @Parameter(description = "Organization unit ID") @PathVariable Long id,
            @Parameter(description = "User ID") @PathVariable Long userId) {
        orgMemberService.removeMember(id, userId);
        return Result.success();
    }

    @Data
    public static class ReasonRequest {
        private String reason;
    }

    @Data
    public static class SplitOrgRequest {
        private String reason;
        private List<OrgUnitApplicationService.SplitRequest> splits;
    }

}
