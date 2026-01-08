package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.result.Result;
import com.school.management.dto.MajorCreateRequest;
import com.school.management.dto.MajorQueryRequest;
import com.school.management.dto.MajorUpdateRequest;
import com.school.management.entity.Major;
import com.school.management.service.MajorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 专业管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/majors")
@RequiredArgsConstructor
public class MajorController {

    private final MajorService majorService;

    /**
     * 分页查询专业列表
     */
    @GetMapping
    @PreAuthorize("hasAuthority('major:list')")
    public Result<IPage<Major>> getMajorPage(MajorQueryRequest request) {
        log.info("分页查询专业列表: {}", request);
        IPage<Major> page = majorService.getMajorPage(request);
        return Result.success(page);
    }

    /**
     * 查询所有启用的专业
     */
    @GetMapping("/enabled")
    @PreAuthorize("hasAuthority('major:list')")
    public Result<List<Major>> getAllEnabledMajors() {
        log.info("查询所有启用的专业");
        List<Major> majors = majorService.getAllEnabledMajors();
        return Result.success(majors);
    }

    /**
     * 根据部门ID查询专业列表
     */
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAuthority('major:list')")
    public Result<List<Major>> getMajorsByDepartmentId(@PathVariable Long departmentId) {
        log.info("根据部门ID查询专业列表: {}", departmentId);
        List<Major> majors = majorService.getMajorsByDepartmentId(departmentId);
        return Result.success(majors);
    }

    /**
     * 根据ID查询专业详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('major:info')")
    public Result<Major> getMajorById(@PathVariable Long id) {
        log.info("查询专业详情: {}", id);
        Major major = majorService.getMajorById(id);
        return Result.success(major);
    }

    /**
     * 创建专业
     */
    @PostMapping
    @PreAuthorize("hasAuthority('major:add')")
    public Result<Void> createMajor(@Valid @RequestBody MajorCreateRequest request) {
        log.info("创建专业: {}", request);
        majorService.createMajor(request);
        return Result.success();
    }

    /**
     * 更新专业
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('major:edit')")
    public Result<Void> updateMajor(@PathVariable Long id, @Valid @RequestBody MajorUpdateRequest request) {
        log.info("更新专业: id={}, request={}", id, request);
        majorService.updateMajor(id, request);
        return Result.success();
    }

    /**
     * 删除专业
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('major:delete')")
    public Result<Void> deleteMajor(@PathVariable Long id) {
        log.info("删除专业: {}", id);
        majorService.deleteMajor(id);
        return Result.success();
    }

    /**
     * 批量删除专业
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('major:delete')")
    public Result<Void> batchDeleteMajors(@RequestBody List<Long> ids) {
        log.info("批量删除专业: {}", ids);
        majorService.batchDeleteMajors(ids);
        return Result.success();
    }
}
