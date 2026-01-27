package com.school.management.interfaces.rest.semester;

import com.school.management.application.semester.SemesterApplicationService;
import com.school.management.application.semester.command.CreateSemesterCommand;
import com.school.management.application.semester.command.UpdateSemesterCommand;
import com.school.management.common.result.Result;
import com.school.management.domain.semester.model.aggregate.Semester;
import com.school.management.domain.semester.model.valueobject.SemesterStatus;
import com.school.management.infrastructure.audit.annotation.OperationLog;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学期管理 REST API控制器 (DDD架构 V2)
 *
 * 已完成从V1 Service迁移到DDD应用服务，不再依赖MyBatis Plus或V1实体。
 * 委托 SemesterApplicationService 完成所有业务操作。
 */
@Slf4j
@Tag(name = "Semester Management V2", description = "学期管理API - DDD架构")
@RestController("semesterControllerV2")
@RequestMapping("/v2/semesters")
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterApplicationService semesterApplicationService;
    private final JwtTokenService jwtTokenService;

    // ==================== 基础CRUD ====================

    @Operation(summary = "分页查询学期列表")
    @GetMapping
    @PreAuthorize("hasAuthority('system:semester:list')")
    public Result<Map<String, Object>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "学期名称") @RequestParam(required = false) String semesterName,
            @Parameter(description = "状态: 1正常 0已结束") @RequestParam(required = false) Integer status) {
        log.info("V2 分页查询学期: pageNum={}, pageSize={}, semesterName={}, status={}",
                pageNum, pageSize, semesterName, status);

        // 获取所有学期（已按开始日期降序排列）
        List<Semester> allSemesters = semesterApplicationService.getAllSemesters();

        // 应用过滤条件
        List<Semester> filtered = allSemesters.stream()
                .filter(s -> semesterName == null || semesterName.isEmpty()
                        || (s.getSemesterName() != null && s.getSemesterName().contains(semesterName)))
                .filter(s -> {
                    if (status == null) return true;
                    if (s.getStatus() == null) return false;
                    if (status == 1) return s.getStatus() == SemesterStatus.ACTIVE;
                    if (status == 0) return s.getStatus() == SemesterStatus.ENDED;
                    return true;
                })
                .collect(Collectors.toList());

        // 手动分页
        long total = filtered.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<SemesterDomainResponse> pageRecords = filtered.subList(fromIndex, toIndex).stream()
                .map(SemesterDomainResponse::fromDomain)
                .collect(Collectors.toList());

        // 构造分页响应（与MyBatis Plus IPage结构兼容）
        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("records", pageRecords);
        pageResult.put("total", total);
        pageResult.put("size", pageSize);
        pageResult.put("current", pageNum);
        pageResult.put("pages", (total + pageSize - 1) / pageSize);

        return Result.success(pageResult);
    }

    @Operation(summary = "获取学期详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:semester:query')")
    public Result<SemesterDomainResponse> getById(
            @Parameter(description = "学期ID") @PathVariable Long id) {
        log.info("V2 获取学期详情: {}", id);
        return semesterApplicationService.getSemester(id)
                .map(semester -> Result.success(SemesterDomainResponse.fromDomain(semester)))
                .orElse(Result.error("学期不存在"));
    }

    @Operation(summary = "创建学期")
    @PostMapping
    @PreAuthorize("hasAuthority('system:semester:add')")
    @OperationLog(module = "system", type = "create", name = "创建学期")
    public Result<SemesterDomainResponse> create(@Valid @RequestBody CreateSemesterRequest request) {
        log.info("V2 创建学期: {}", request.getSemesterName());

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
    public Result<SemesterDomainResponse> update(
            @Parameter(description = "学期ID") @PathVariable Long id,
            @Valid @RequestBody UpdateSemesterRequest request) {
        log.info("V2 更新学期: {}", id);

        UpdateSemesterCommand command = UpdateSemesterCommand.builder()
                .semesterName(request.getSemesterName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .updatedBy(getCurrentUserId())
                .build();

        Semester semester = semesterApplicationService.updateSemester(id, command);
        return Result.success(SemesterDomainResponse.fromDomain(semester));
    }

    @Operation(summary = "删除学期")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:semester:delete')")
    @OperationLog(module = "system", type = "delete", name = "删除学期")
    public Result<Void> delete(
            @Parameter(description = "学期ID") @PathVariable Long id) {
        log.info("V2 删除学期: {}", id);
        semesterApplicationService.deleteSemester(id);
        return Result.success();
    }

    // ==================== 当前学期操作 ====================

    @Operation(summary = "获取当前学期")
    @GetMapping("/current")
    public Result<SemesterDomainResponse> getCurrentSemester() {
        log.info("V2 获取当前学期");
        return semesterApplicationService.getCurrentSemester()
                .map(semester -> Result.success(SemesterDomainResponse.fromDomain(semester)))
                .orElse(Result.error("未设置当前学期"));
    }

    @Operation(summary = "设置当前学期")
    @PutMapping("/{id}/set-current")
    @PreAuthorize("hasAuthority('system:semester:edit')")
    @OperationLog(module = "system", type = "update", name = "设置当前学期")
    public Result<SemesterDomainResponse> setCurrentSemester(
            @Parameter(description = "学期ID") @PathVariable Long id) {
        log.info("V2 设置当前学期: {}", id);
        Semester semester = semesterApplicationService.setCurrentSemester(id);
        return Result.success(SemesterDomainResponse.fromDomain(semester));
    }

    // ==================== 状态操作 ====================

    @Operation(summary = "结束学期")
    @PostMapping("/{id}/end")
    @PreAuthorize("hasAuthority('system:semester:edit')")
    @OperationLog(module = "system", type = "update", name = "结束学期")
    public Result<SemesterDomainResponse> endSemester(
            @Parameter(description = "学期ID") @PathVariable Long id) {
        log.info("V2 结束学期: {}", id);
        Semester semester = semesterApplicationService.endSemester(id);
        return Result.success(SemesterDomainResponse.fromDomain(semester));
    }

    @Operation(summary = "重新激活学期")
    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAuthority('system:semester:edit')")
    @OperationLog(module = "system", type = "update", name = "重新激活学期")
    public Result<SemesterDomainResponse> reactivateSemester(
            @Parameter(description = "学期ID") @PathVariable Long id) {
        log.info("V2 重新激活学期: {}", id);
        Semester semester = semesterApplicationService.reactivateSemester(id);
        return Result.success(SemesterDomainResponse.fromDomain(semester));
    }

    // ==================== 查询操作 ====================

    @Operation(summary = "获取所有正常状态的学期列表")
    @GetMapping("/active")
    public Result<List<SemesterDomainResponse>> getActiveSemesters() {
        log.info("V2 获取所有正常状态的学期");
        List<SemesterDomainResponse> semesters = semesterApplicationService.getActiveSemesters()
                .stream()
                .map(SemesterDomainResponse::fromDomain)
                .collect(Collectors.toList());
        return Result.success(semesters);
    }

    @Operation(summary = "根据年份获取学期列表")
    @GetMapping("/by-year/{year}")
    public Result<List<SemesterDomainResponse>> getSemestersByYear(
            @Parameter(description = "年份") @PathVariable Integer year) {
        log.info("V2 根据年份获取学期: {}", year);
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
        log.info("V2 检查学期编码是否存在: {}", semesterCode);
        boolean exists = semesterApplicationService.existsSemesterCode(semesterCode, excludeId);
        return Result.success(exists);
    }

    @Operation(summary = "生成学期编码")
    @GetMapping("/generate-code")
    public Result<String> generateSemesterCode(
            @Parameter(description = "开始年份") @RequestParam Integer startYear,
            @Parameter(description = "学期类型: 1第一学期 2第二学期") @RequestParam Integer semesterType) {
        log.info("V2 生成学期编码: startYear={}, semesterType={}", startYear, semesterType);
        String code = semesterApplicationService.generateSemesterCode(startYear, semesterType);
        return Result.success(code);
    }

    // ==================== Helper ====================

    private Long getCurrentUserId() {
        return jwtTokenService.getCurrentUserId();
    }
}
