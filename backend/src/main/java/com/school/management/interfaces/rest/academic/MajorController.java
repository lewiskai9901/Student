package com.school.management.interfaces.rest.academic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.application.academic.MajorApplicationService;
import com.school.management.application.academic.command.CreateMajorCommand;
import com.school.management.application.academic.command.UpdateMajorCommand;
import com.school.management.application.academic.query.MajorDTO;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 专业管理 REST 控制器
 */
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Majors", description = "Major management API")
@RestController
@RequestMapping("/academic/majors")
public class MajorController {

    private final MajorApplicationService majorService;

    @Operation(summary = "Get major list (paginated)")
    @GetMapping
    @CasbinAccess(resource = "major", action = "list")
    public Result<Page<MajorDTO>> getMajorList(
            @RequestParam(required = false) String majorName,
            @RequestParam(required = false) String majorCode,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Page<MajorDTO> result = majorService.getMajorList(majorName, majorCode, orgUnitId, status, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "Get all enabled majors")
    @GetMapping("/enabled")
    @CasbinAccess(resource = "major", action = "list")
    public Result<List<MajorDTO>> getAllEnabledMajors() {
        return Result.success(majorService.getAllEnabledMajors());
    }

    @Operation(summary = "Get majors by org unit")
    @GetMapping("/org-unit/{orgUnitId}")
    @CasbinAccess(resource = "major", action = "list")
    public Result<List<MajorDTO>> getMajorsByOrgUnit(@PathVariable Long orgUnitId) {
        return Result.success(majorService.getMajorsByOrgUnit(orgUnitId));
    }

    @Operation(summary = "Get major detail")
    @GetMapping("/{id}")
    @CasbinAccess(resource = "major", action = "list")
    public Result<MajorDTO> getMajor(@PathVariable Long id) {
        return Result.success(majorService.getMajor(id));
    }

    @Operation(summary = "Create major")
    @PostMapping
    @CasbinAccess(resource = "major", action = "add")
    public Result<MajorDTO> createMajor(@Valid @RequestBody CreateMajorRequest request) {
        CreateMajorCommand command = new CreateMajorCommand();
        command.setMajorCode(request.getMajorCode());
        command.setMajorName(request.getMajorName());
        command.setOrgUnitId(request.getOrgUnitId());
        command.setDescription(request.getDescription());
        command.setCreatedBy(SecurityUtils.requireCurrentUserId());

        return Result.success(majorService.createMajor(command));
    }

    @Operation(summary = "Update major")
    @PutMapping("/{id}")
    @CasbinAccess(resource = "major", action = "edit")
    public Result<MajorDTO> updateMajor(@PathVariable Long id,
                                         @RequestBody UpdateMajorRequest request) {
        UpdateMajorCommand command = new UpdateMajorCommand();
        command.setMajorName(request.getMajorName());
        command.setDescription(request.getDescription());
        command.setStatus(request.getStatus());
        command.setUpdatedBy(SecurityUtils.requireCurrentUserId());

        return Result.success(majorService.updateMajor(id, command));
    }

    @Operation(summary = "Delete major")
    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "major", action = "delete")
    public Result<Void> deleteMajor(@PathVariable Long id) {
        majorService.deleteMajor(id);
        return Result.success();
    }

    @Operation(summary = "Batch delete majors")
    @DeleteMapping("/batch")
    @CasbinAccess(resource = "major", action = "delete")
    public Result<Void> batchDeleteMajors(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids != null) {
            ids.forEach(majorService::deleteMajor);
        }
        return Result.success();
    }

}
