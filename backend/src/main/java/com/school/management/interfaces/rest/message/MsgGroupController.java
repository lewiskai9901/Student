package com.school.management.interfaces.rest.message;

import com.school.management.application.message.group.MsgGroupService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * S+2: 通知组 REST API.
 */
@RestController
@RequestMapping("/message/groups")
@RequiredArgsConstructor
public class MsgGroupController {

    private final MsgGroupService groupService;

    @GetMapping
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<List<Map<String, Object>>> list() {
        return Result.success(groupService.listGroups(0L));
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        return Result.success(groupService.getGroup(0L, id));
    }

    @GetMapping("/{id}/members")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<List<Map<String, Object>>> members(@PathVariable Long id) {
        return Result.success(groupService.listMembers(0L, id));
    }

    @PostMapping
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<Map<String, Object>> create(@RequestBody CreateRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long id = groupService.createGroup(0L, req.code(), req.name(), req.description(), userId);
        return Result.success(Map.of("id", id));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<Void> update(@PathVariable Long id, @RequestBody UpdateRequest req) {
        groupService.updateGroup(0L, id, req.name(), req.description(), req.enabled());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<Void> delete(@PathVariable Long id) {
        groupService.deleteGroup(0L, id);
        return Result.success();
    }

    @PostMapping("/{id}/members")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<Void> addMembers(@PathVariable Long id, @RequestBody MemberRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        groupService.addMembers(0L, id, req.userIds(), userId);
        return Result.success();
    }

    @DeleteMapping("/{id}/members/{userId}")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        groupService.removeMember(id, userId);
        return Result.success();
    }

    public record CreateRequest(String code, String name, String description) {}
    public record UpdateRequest(String name, String description, Boolean enabled) {}
    public record MemberRequest(List<Long> userIds) {}
}
