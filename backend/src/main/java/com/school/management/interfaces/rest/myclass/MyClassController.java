package com.school.management.interfaces.rest.myclass;

import com.school.management.application.myclass.MyClassApplicationService;
import com.school.management.application.myclass.query.DormitoryDistributionDTO;
import com.school.management.application.myclass.query.MyClassDTO;
import com.school.management.application.myclass.query.MyClassOverviewDTO;
import com.school.management.application.myclass.query.MyClassStudentDTO;
import com.school.management.common.result.Result;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 我的班级控制器
 * 提供班主任/教师班级管理工作台API
 */
@RestController
@RequestMapping("/my-class")
@RequiredArgsConstructor
@Tag(name = "我的班级", description = "班主任/教师班级管理工作台")
@PreAuthorize("isAuthenticated()")
public class MyClassController {

    private final MyClassApplicationService myClassService;

    @GetMapping("/classes")
    @Operation(summary = "获取我管理的班级列表")
    public Result<List<MyClassDTO>> getMyClasses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<MyClassDTO> classes = myClassService.getMyClasses(userDetails.getUserId());
        return Result.success(classes);
    }

    @GetMapping("/classes/{classId}/overview")
    @Operation(summary = "获取班级概览数据")
    public Result<MyClassOverviewDTO> getClassOverview(
            @PathVariable Long classId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MyClassOverviewDTO overview = myClassService.getClassOverview(classId, userDetails.getUserId());
        return Result.success(overview);
    }

    @GetMapping("/classes/{classId}/students")
    @Operation(summary = "获取班级学生列表")
    public Result<List<MyClassStudentDTO>> getClassStudents(
            @PathVariable Long classId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<MyClassStudentDTO> students = myClassService.getClassStudents(
            classId, userDetails.getUserId(), keyword, status);
        return Result.success(students);
    }

    @GetMapping("/classes/{classId}/dormitory-distribution")
    @Operation(summary = "获取班级宿舍分布")
    public Result<List<DormitoryDistributionDTO>> getDormitoryDistribution(
            @PathVariable Long classId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<DormitoryDistributionDTO> distribution = myClassService.getDormitoryDistribution(
            classId, userDetails.getUserId());
        return Result.success(distribution);
    }
}
