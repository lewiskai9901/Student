package com.school.management.interfaces.rest.semester;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.annotation.OperationLog;
import com.school.management.common.result.Result;
import com.school.management.entity.Semester;
import com.school.management.service.SemesterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学期管理 REST API控制器 (DDD架构 V2)
 *
 * 迁移说明：当前版本使用V1服务实现，后续会迁移到DDD应用服务
 */
@Slf4j
@Tag(name = "Semester Management V2", description = "学期管理API - DDD架构")
@RestController("semesterControllerV2")
@RequestMapping("/v2/semesters")
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterService semesterService; // 暂时使用V1服务

    // ==================== 基础CRUD ====================

    @Operation(summary = "分页查询学期列表")
    @GetMapping
    @PreAuthorize("hasAuthority('system:semester:list')")
    public Result<IPage<Semester>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "学期名称") @RequestParam(required = false) String semesterName,
            @Parameter(description = "状态: 1正常 0已结束") @RequestParam(required = false) Integer status) {
        log.info("V2 分页查询学期: pageNum={}, pageSize={}, semesterName={}, status={}",
                pageNum, pageSize, semesterName, status);

        Page<Semester> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Semester> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);

        if (semesterName != null && !semesterName.isEmpty()) {
            wrapper.like("semester_name", semesterName);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }

        wrapper.orderByDesc("start_date");

        IPage<Semester> result = semesterService.page(page, wrapper);
        return Result.success(result);
    }

    @Operation(summary = "获取学期详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:semester:query')")
    public Result<Semester> getById(
            @Parameter(description = "学期ID") @PathVariable Long id) {
        log.info("V2 获取学期详情: {}", id);
        Semester semester = semesterService.getById(id);
        return Result.success(semester);
    }

    @Operation(summary = "创建学期")
    @PostMapping
    @PreAuthorize("hasAuthority('system:semester:add')")
    @OperationLog(module = "system", type = "create", name = "创建学期")
    public Result<Void> create(@RequestBody Semester semester) {
        log.info("V2 创建学期: {}", semester.getSemesterName());

        // 生成学期编码
        String code = semesterService.generateSemesterCode(
                semester.getStartYear(),
                semester.getSemesterType()
        );
        semester.setSemesterCode(code);

        boolean success = semesterService.save(semester);
        return success ? Result.success() : Result.error("创建失败");
    }

    @Operation(summary = "更新学期")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:semester:edit')")
    @OperationLog(module = "system", type = "update", name = "更新学期")
    public Result<Void> update(
            @Parameter(description = "学期ID") @PathVariable Long id,
            @RequestBody Semester semester) {
        log.info("V2 更新学期: {}", id);
        semester.setId(id);
        boolean success = semesterService.updateById(semester);
        return success ? Result.success() : Result.error("更新失败");
    }

    @Operation(summary = "删除学期")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:semester:delete')")
    @OperationLog(module = "system", type = "delete", name = "删除学期")
    public Result<Void> delete(
            @Parameter(description = "学期ID") @PathVariable Long id) {
        log.info("V2 删除学期: {}", id);
        boolean success = semesterService.removeById(id);
        return success ? Result.success() : Result.error("删除失败");
    }

    // ==================== 当前学期操作 ====================

    @Operation(summary = "获取当前学期")
    @GetMapping("/current")
    public Result<Semester> getCurrentSemester() {
        log.info("V2 获取当前学期");
        Semester semester = semesterService.getCurrentSemester();
        return Result.success(semester);
    }

    @Operation(summary = "设置当前学期")
    @PutMapping("/{id}/set-current")
    @PreAuthorize("hasAuthority('system:semester:edit')")
    @OperationLog(module = "system", type = "update", name = "设置当前学期")
    public Result<Void> setCurrentSemester(
            @Parameter(description = "学期ID") @PathVariable Long id) {
        log.info("V2 设置当前学期: {}", id);
        boolean success = semesterService.setCurrentSemester(id);
        return success ? Result.success() : Result.error("设置失败");
    }

    // ==================== 状态操作 ====================

    @Operation(summary = "结束学期")
    @PostMapping("/{id}/end")
    @PreAuthorize("hasAuthority('system:semester:edit')")
    @OperationLog(module = "system", type = "update", name = "结束学期")
    public Result<Void> endSemester(
            @Parameter(description = "学期ID") @PathVariable Long id) {
        log.info("V2 结束学期: {}", id);
        Semester semester = semesterService.getById(id);
        if (semester == null) {
            return Result.error("学期不存在");
        }
        if (semester.getIsCurrent() != null && semester.getIsCurrent() == 1) {
            return Result.error("不能结束当前学期，请先设置其他学期为当前学期");
        }
        semester.setStatus(0);
        boolean success = semesterService.updateById(semester);
        return success ? Result.success() : Result.error("结束失败");
    }

    @Operation(summary = "重新激活学期")
    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAuthority('system:semester:edit')")
    @OperationLog(module = "system", type = "update", name = "重新激活学期")
    public Result<Void> reactivateSemester(
            @Parameter(description = "学期ID") @PathVariable Long id) {
        log.info("V2 重新激活学期: {}", id);
        Semester semester = semesterService.getById(id);
        if (semester == null) {
            return Result.error("学期不存在");
        }
        semester.setStatus(1);
        boolean success = semesterService.updateById(semester);
        return success ? Result.success() : Result.error("激活失败");
    }

    // ==================== 查询操作 ====================

    @Operation(summary = "获取所有正常状态的学期列表")
    @GetMapping("/active")
    public Result<List<Semester>> getActiveSemesters() {
        log.info("V2 获取所有正常状态的学期");
        QueryWrapper<Semester> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0)
                .eq("status", 1)
                .orderByDesc("start_date");
        List<Semester> semesters = semesterService.list(wrapper);
        return Result.success(semesters);
    }

    @Operation(summary = "根据年份获取学期列表")
    @GetMapping("/by-year/{year}")
    public Result<List<Semester>> getSemestersByYear(
            @Parameter(description = "年份") @PathVariable Integer year) {
        log.info("V2 根据年份获取学期: {}", year);
        QueryWrapper<Semester> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0)
                .apply("YEAR(start_date) = {0}", year)
                .orderByAsc("start_date");
        List<Semester> semesters = semesterService.list(wrapper);
        return Result.success(semesters);
    }

    @Operation(summary = "检查学期编码是否存在")
    @GetMapping("/exists")
    @PreAuthorize("hasAuthority('system:semester:query')")
    public Result<Boolean> existsSemesterCode(
            @Parameter(description = "学期编码") @RequestParam String semesterCode,
            @Parameter(description = "排除的学期ID") @RequestParam(required = false) Long excludeId) {
        log.info("V2 检查学期编码是否存在: {}", semesterCode);
        QueryWrapper<Semester> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0)
                .eq("semester_code", semesterCode);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        boolean exists = semesterService.count(wrapper) > 0;
        return Result.success(exists);
    }

    @Operation(summary = "生成学期编码")
    @GetMapping("/generate-code")
    public Result<String> generateSemesterCode(
            @Parameter(description = "开始年份") @RequestParam Integer startYear,
            @Parameter(description = "学期类型: 1第一学期 2第二学期") @RequestParam Integer semesterType) {
        log.info("V2 生成学期编码: startYear={}, semesterType={}", startYear, semesterType);
        String code = semesterService.generateSemesterCode(startYear, semesterType);
        return Result.success(code);
    }
}
