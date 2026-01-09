package com.school.management.interfaces.rest.semester;

import com.school.management.annotation.OperationLog;
import com.school.management.application.semester.SemesterApplicationService;
import com.school.management.application.semester.command.CreateSemesterCommand;
import com.school.management.application.semester.command.UpdateSemesterCommand;
import com.school.management.common.result.Result;
import com.school.management.domain.semester.model.aggregate.Semester;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 学期管理 REST API控制器 (纯 DDD 架构)
 *
 * 使用 DDD 应用服务实现，完全脱离 V1 架构
 */
@Slf4j
@Tag(name = "Semester Domain API", description = "学期管理API - 纯DDD架构")
@RestController("semesterDomainControllerV2")
@RequestMapping("/v2/domain/semesters")
@RequiredArgsConstructor
public class SemesterDomainController {

    private final SemesterApplicationService semesterApplicationService;
    private final JwtTokenService jwtTokenService;

    // ==================== 创建与更新 ====================

    @Operation(summary = "创建学期")
    @PostMapping
    @PreAuthorize("hasAuthority('system:semester:add')")
    @OperationLog(module = "system", type = "create", name = "创建学期")
    public Result<SemesterDomainResponse> createSemester(@Valid @RequestBody CreateSemesterRequest request) {
        log.info("DDD 创建学期: {}", request.getSemesterName());

        CreateSemesterCommand command = CreateSemesterCommand.builder()
                .semesterName(request.getSemesterName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .startYear(request.getStartYear())
                .semesterType(request.getSemesterType())
                .createdBy(getCurrentUserId())
                .build();

        Semester semester = semesterApplicationService.createSemester(command);
        return Result.success(SemesterDomainResponse.fromDomain(semester));
    }

    @Operation(summary = "更新学期")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:semester:edit')")
    @OperationLog(module = "system", type = "update", name = "更新学期")
    public Result<SemesterDomainResponse> updateSemester(
            @Parameter(description = "学期ID") @PathVariable Long id,
            @Valid @RequestBody UpdateSemesterRequest request) {
        log.info("DDD 更新学期: {}", id);

        UpdateSemesterCommand command = UpdateSemesterCommand.builder()
                .semesterName(request.getSemesterName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .updatedBy(getCurrentUserId())
                .build();

        Semester semester = semesterApplicationService.updateSemester(id, command);
        return Result.success(SemesterDomainResponse.fromDomain(semester));
    }

    // ==================== 删除操作 ====================

    @Operation(summary = "删除学期")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:semester:delete')")
    @OperationLog(module = "system", type = "delete", name = "删除学期")
    public Result<Void> deleteSemester(@Parameter(description = "学期ID") @PathVariable Long id) {
        log.info("DDD 删除学期: {}", id);
        semesterApplicationService.deleteSemester(id);
        return Result.success();
    }

    // ==================== 查询操作 ====================

    @Operation(summary = "获取学期详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:semester:query')")
    public Result<SemesterDomainResponse> getSemester(@Parameter(description = "学期ID") @PathVariable Long id) {
        return semesterApplicationService.getSemester(id)
                .map(semester -> Result.success(SemesterDomainResponse.fromDomain(semester)))
                .orElse(Result.error("学期不存在"));
    }

    @Operation(summary = "根据编码获取学期")
    @GetMapping("/by-code/{code}")
    @PreAuthorize("hasAuthority('system:semester:query')")
    public Result<SemesterDomainResponse> getSemesterByCode(
            @Parameter(description = "学期编码") @PathVariable String code) {
        return semesterApplicationService.getSemesterByCode(code)
                .map(semester -> Result.success(SemesterDomainResponse.fromDomain(semester)))
                .orElse(Result.error("学期不存在"));
    }

    @Operation(summary = "获取当前学期")
    @GetMapping("/current")
    public Result<SemesterDomainResponse> getCurrentSemester() {
        return semesterApplicationService.getCurrentSemester()
                .map(semester -> Result.success(SemesterDomainResponse.fromDomain(semester)))
                .orElse(Result.error("未设置当前学期"));
    }

    @Operation(summary = "获取所有正常状态的学期")
    @GetMapping("/active")
    public Result<List<SemesterDomainResponse>> getActiveSemesters() {
        List<SemesterDomainResponse> semesters = semesterApplicationService.getActiveSemesters()
                .stream()
                .map(SemesterDomainResponse::fromDomain)
                .collect(Collectors.toList());
        return Result.success(semesters);
    }

    @Operation(summary = "获取所有学期")
    @GetMapping
    @PreAuthorize("hasAuthority('system:semester:list')")
    public Result<List<SemesterDomainResponse>> getAllSemesters() {
        List<SemesterDomainResponse> semesters = semesterApplicationService.getAllSemesters()
                .stream()
                .map(SemesterDomainResponse::fromDomain)
                .collect(Collectors.toList());
        return Result.success(semesters);
    }

    @Operation(summary = "根据年份获取学期列表")
    @GetMapping("/by-year/{year}")
    public Result<List<SemesterDomainResponse>> getSemestersByYear(
            @Parameter(description = "年份") @PathVariable Integer year) {
        List<SemesterDomainResponse> semesters = semesterApplicationService.getSemestersByYear(year)
                .stream()
                .map(SemesterDomainResponse::fromDomain)
                .collect(Collectors.toList());
        return Result.success(semesters);
    }

    @Operation(summary = "检查学期编码是否存在")
    @GetMapping("/exists")
    @PreAuthorize("hasAuthority('system:semester:query')")
    public Result<Boolean> existsSemesterCode(
            @Parameter(description = "学期编码") @RequestParam String semesterCode,
            @Parameter(description = "排除的学期ID") @RequestParam(required = false) Long excludeId) {
        boolean exists = semesterApplicationService.existsSemesterCode(semesterCode, excludeId);
        return Result.success(exists);
    }

    @Operation(summary = "生成学期编码")
    @GetMapping("/generate-code")
    public Result<String> generateSemesterCode(
            @Parameter(description = "开始年份") @RequestParam Integer startYear,
            @Parameter(description = "学期类型: 1第一学期 2第二学期") @RequestParam Integer semesterType) {
        String code = semesterApplicationService.generateSemesterCode(startYear, semesterType);
        return Result.success(code);
    }

    // ==================== 状态操作 ====================

    @Operation(summary = "设置当前学期")
    @PutMapping("/{id}/set-current")
    @PreAuthorize("hasAuthority('system:semester:edit')")
    @OperationLog(module = "system", type = "update", name = "设置当前学期")
    public Result<SemesterDomainResponse> setCurrentSemester(
            @Parameter(description = "学期ID") @PathVariable Long id) {
        log.info("DDD 设置当前学期: {}", id);
        Semester semester = semesterApplicationService.setCurrentSemester(id);
        return Result.success(SemesterDomainResponse.fromDomain(semester));
    }

    @Operation(summary = "结束学期")
    @PostMapping("/{id}/end")
    @PreAuthorize("hasAuthority('system:semester:edit')")
    @OperationLog(module = "system", type = "update", name = "结束学期")
    public Result<SemesterDomainResponse> endSemester(
            @Parameter(description = "学期ID") @PathVariable Long id) {
        log.info("DDD 结束学期: {}", id);
        Semester semester = semesterApplicationService.endSemester(id);
        return Result.success(SemesterDomainResponse.fromDomain(semester));
    }

    @Operation(summary = "重新激活学期")
    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAuthority('system:semester:edit')")
    @OperationLog(module = "system", type = "update", name = "重新激活学期")
    public Result<SemesterDomainResponse> reactivateSemester(
            @Parameter(description = "学期ID") @PathVariable Long id) {
        log.info("DDD 重新激活学期: {}", id);
        Semester semester = semesterApplicationService.reactivateSemester(id);
        return Result.success(SemesterDomainResponse.fromDomain(semester));
    }

    // ==================== Helper ====================

    private Long getCurrentUserId() {
        return jwtTokenService.getCurrentUserId();
    }
}
