package com.school.management.interfaces.rest.organization;

import com.school.management.application.myclass.MyClassApplicationService;
import com.school.management.application.myclass.query.DormitoryDistributionDTO;
import com.school.management.application.myclass.query.MyClassDTO;
import com.school.management.application.myclass.query.MyClassOverviewDTO;
import com.school.management.application.myclass.query.MyClassStudentDTO;
import com.school.management.common.result.Result;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 我的班级 REST API控制器
 * 为班主任/教师提供班级管理工作台功能
 */
@Slf4j
@Tag(name = "我的班级", description = "班主任/教师班级管理工作台")
@RestController("myClassController")
@RequestMapping("/myclass")
public class MyClassController {

    private final MyClassApplicationService myClassService;

    @Autowired
    public MyClassController(MyClassApplicationService myClassService) {
        log.info("========== MyClassController bean is being created ==========");
        this.myClassService = myClassService;
    }

    /**
     * 简单的健康检查端点，用于测试控制器是否被注册
     */
    @GetMapping("/health")
    public Result<String> healthCheck() {
        log.info("MyClassController health check called");
        return Result.success("MyClassController is working!");
    }

    /**
     * 获取我管理的班级列表
     */
    @Operation(summary = "获取我管理的班级列表", description = "返回当前用户作为班主任或副班主任管理的所有班级")
    @GetMapping("/classes")
    @PreAuthorize("isAuthenticated()")
    public Result<List<MyClassDTO>> getMyClasses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("getMyClasses called for user: {}", userDetails.getUserId());
        List<MyClassDTO> classes = myClassService.getMyClasses(userDetails.getUserId());
        return Result.success(classes);
    }

    /**
     * 获取班级概览数据
     */
    @Operation(summary = "获取班级概览数据", description = "返回指定班级的概览信息，包括学生统计、排名、分数趋势等")
    @GetMapping("/classes/{classId}/overview")
    @PreAuthorize("isAuthenticated()")
    public Result<MyClassOverviewDTO> getClassOverview(
            @Parameter(description = "班级ID") @PathVariable Long classId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MyClassOverviewDTO overview = myClassService.getClassOverview(classId, userDetails.getUserId());
        return Result.success(overview);
    }

    /**
     * 获取班级学生列表
     */
    @Operation(summary = "获取班级学生列表", description = "返回指定班级的学生列表，支持关键词搜索和状态筛选")
    @GetMapping("/classes/{classId}/students")
    @PreAuthorize("isAuthenticated()")
    public Result<List<MyClassStudentDTO>> getClassStudents(
            @Parameter(description = "班级ID") @PathVariable Long classId,
            @Parameter(description = "搜索关键词（学号或姓名）") @RequestParam(required = false) String keyword,
            @Parameter(description = "学籍状态（0-在读, 1-休学, 2-退学, 3-毕业, 4-转学, 5-开除）") @RequestParam(required = false) String status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<MyClassStudentDTO> students = myClassService.getClassStudents(
                classId, userDetails.getUserId(), keyword, status);
        return Result.success(students);
    }

    /**
     * 获取班级宿舍分布
     */
    @Operation(summary = "获取班级宿舍分布", description = "返回指定班级学生的宿舍分布情况，按楼栋和房间分组")
    @GetMapping("/classes/{classId}/dormitory-distribution")
    @PreAuthorize("isAuthenticated()")
    public Result<List<DormitoryDistributionDTO>> getDormitoryDistribution(
            @Parameter(description = "班级ID") @PathVariable Long classId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<DormitoryDistributionDTO> distribution = myClassService.getDormitoryDistribution(
                classId, userDetails.getUserId());
        return Result.success(distribution);
    }
}
