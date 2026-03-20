package com.school.management.interfaces.rest.access;

import com.school.management.application.access.AccessRelationApplicationService;
import com.school.management.application.access.AccessRelationApplicationService.CreateCommand;
import com.school.management.application.access.AccessRelationApplicationService.UpdateCommand;
import com.school.management.common.result.Result;
import com.school.management.domain.access.model.entity.AccessRelation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统一访问关系 Controller
 * 替代旧的 PlaceOrgRelationController / UserOrgRelationController / UserPlaceRelationController
 */
@RestController
@RequestMapping("/access-relations")
@RequiredArgsConstructor
public class AccessRelationController {

    private final AccessRelationApplicationService service;

    /**
     * 按资源查询关系
     */
    @GetMapping
    public Result<List<AccessRelation>> query(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String subjectType,
            @RequestParam(required = false) Long subjectId) {
        if (resourceType != null && resourceId != null) {
            return Result.success(service.findByResource(resourceType, resourceId));
        }
        if (subjectType != null && subjectId != null) {
            if (resourceType != null) {
                return Result.success(service.findBySubjectAndResourceType(subjectType, subjectId, resourceType));
            }
            return Result.success(service.findBySubject(subjectType, subjectId));
        }
        return Result.error(400, "必须提供 resourceType+resourceId 或 subjectType+subjectId");
    }

    /**
     * 检查关系是否存在
     */
    @GetMapping("/check")
    public Result<Map<String, Boolean>> check(
            @RequestParam String resourceType,
            @RequestParam Long resourceId,
            @RequestParam String relation,
            @RequestParam String subjectType,
            @RequestParam Long subjectId) {
        boolean exists = service.checkAccess(resourceType, resourceId, relation, subjectType, subjectId);
        return Result.success(Map.of("exists", exists));
    }

    /**
     * 创建关系
     */
    @PostMapping
    @CasbinAccess(resource = "admin", action = "access")
    public Result<AccessRelation> create(@RequestBody CreateCommand cmd) {
        return Result.success(service.create(cmd));
    }

    /**
     * 更新关系
     */
    @PutMapping("/{id}")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Void> update(@PathVariable Long id, @RequestBody UpdateCommand cmd) {
        service.update(id, cmd);
        return Result.success(null);
    }

    /**
     * 删除关系
     */
    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success(null);
    }

    /**
     * 批量创建
     */
    @PostMapping("/batch")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Integer>> batchCreate(@RequestBody List<CreateCommand> commands) {
        int count = service.batchCreate(commands);
        return Result.success(Map.of("created", count));
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/batch")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Integer>> batchDelete(@RequestBody BatchDeleteRequest request) {
        int count = service.batchDelete(request.getIds());
        return Result.success(Map.of("deleted", count));
    }

    @Data
    public static class BatchDeleteRequest {
        private List<Long> ids;
    }
}
