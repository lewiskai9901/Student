package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.TeachingClassApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.teaching.model.teachingclass.TeachingClass;
import com.school.management.domain.teaching.model.teachingclass.TeachingClassMember;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teaching/teaching-classes")
@RequiredArgsConstructor
public class TeachingClassController {
    private final TeachingClassApplicationService service;

    @GetMapping
    public Result<List<TeachingClass>> list(@RequestParam Long semesterId) {
        return Result.success(service.listBySemester(semesterId));
    }

    @GetMapping("/{id}")
    public Result<TeachingClass> getById(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @PostMapping
    public Result<TeachingClass> create(@RequestBody Map<String, Object> body) {
        return Result.success(service.create(body, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/{id}")
    public Result<TeachingClass> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.success(service.update(id, body));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success(null);
    }

    @GetMapping("/{id}/members")
    public Result<List<TeachingClassMember>> getMembers(@PathVariable Long id) {
        return Result.success(service.getMembers(id));
    }

    @PostMapping("/{id}/members")
    public Result<Void> addMembers(@PathVariable Long id, @RequestBody List<Map<String, Object>> members) {
        service.addMembers(id, members);
        return Result.success(null);
    }

    @DeleteMapping("/{id}/members")
    public Result<Void> removeMembers(@PathVariable Long id) {
        service.removeMembers(id);
        return Result.success(null);
    }
}
