package com.school.management.interfaces.rest.organization;

import com.school.management.application.organization.GradeApplicationService;
import com.school.management.application.organization.command.AssignGradeLeaderCommand;
import com.school.management.application.organization.command.CreateGradeCommand;
import com.school.management.application.organization.command.UpdateGradeCommand;
import com.school.management.application.organization.query.GradeDTO;
import com.school.management.common.result.Result;
import com.school.management.domain.organization.model.GradeStatus;
import com.school.management.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST API controller for grade management.
 * Provides endpoints for CRUD operations and status management.
 */
@Slf4j
@Tag(name = "Grades", description = "Grade management API")
@RestController("gradeController")
@RequestMapping("/grades")
public class GradeController {

    private final GradeApplicationService gradeService;

    public GradeController(GradeApplicationService gradeService) {
        this.gradeService = gradeService;
    }

    @Operation(summary = "Create grade", description = "Creates a new grade")
    @PostMapping
    @PreAuthorize("hasAuthority('system:grade:create')")
    public Result<GradeDTO> createGrade(@Valid @RequestBody CreateGradeRequest request) {
        CreateGradeCommand command = new CreateGradeCommand();
        command.setGradeCode(request.getGradeCode());
        command.setGradeName(request.getGradeName());
        command.setEnrollmentYear(request.getEnrollmentYear());
        command.setSchoolingYears(request.getSchoolingYears());
        command.setStandardClassSize(request.getStandardClassSize());
        command.setCreatedBy(getCurrentUserId());

        GradeDTO result = gradeService.createGrade(command);
        return Result.success(result);
    }

    @Operation(summary = "Update grade", description = "Updates an existing grade")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:grade:update')")
    public Result<GradeDTO> updateGrade(
            @Parameter(description = "Grade ID") @PathVariable Long id,
            @RequestBody UpdateGradeRequest request) {
        UpdateGradeCommand command = new UpdateGradeCommand();
        command.setGradeName(request.getGradeName());
        command.setStandardClassSize(request.getStandardClassSize());
        command.setSortOrder(request.getSortOrder());
        command.setRemarks(request.getRemarks());
        command.setUpdatedBy(getCurrentUserId());

        GradeDTO result = gradeService.updateGrade(id, command);
        return Result.success(result);
    }

    @Operation(summary = "Assign leaders", description = "Assigns director and counselor to a grade")
    @PutMapping("/{id}/leaders")
    @PreAuthorize("hasAuthority('system:grade:update')")
    public Result<GradeDTO> assignLeaders(
            @Parameter(description = "Grade ID") @PathVariable Long id,
            @RequestBody AssignGradeLeaderRequest request) {
        AssignGradeLeaderCommand command = new AssignGradeLeaderCommand();
        command.setDirectorId(request.getDirectorId());
        command.setDirectorName(request.getDirectorName());
        command.setCounselorId(request.getCounselorId());
        command.setCounselorName(request.getCounselorName());
        command.setUpdatedBy(getCurrentUserId());

        GradeDTO result = gradeService.assignLeaders(id, command);
        return Result.success(result);
    }

    @Operation(summary = "Get grade", description = "Gets a grade by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:grade:view')")
    public Result<GradeDTO> getGrade(
            @Parameter(description = "Grade ID") @PathVariable Long id) {
        GradeDTO result = gradeService.getGrade(id);
        return Result.success(result);
    }

    @Operation(summary = "Get all grades", description = "Gets all grades ordered by enrollment year")
    @GetMapping
    @PreAuthorize("hasAuthority('system:grade:view') or hasAuthority('system:role:edit') or hasAuthority('system:role:view')")
    public Result<List<GradeDTO>> getAllGrades() {
        List<GradeDTO> result = gradeService.getAllGrades();
        return Result.success(result);
    }

    @Operation(summary = "Get active grades", description = "Gets all active grades")
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('system:grade:view')")
    public Result<List<GradeDTO>> getActiveGrades() {
        List<GradeDTO> result = gradeService.getActiveGrades();
        return Result.success(result);
    }

    @Operation(summary = "Get by status", description = "Gets grades by status")
    @GetMapping("/by-status/{status}")
    @PreAuthorize("hasAuthority('system:grade:view')")
    public Result<List<GradeDTO>> getByStatus(
            @Parameter(description = "Grade status") @PathVariable GradeStatus status) {
        List<GradeDTO> result = gradeService.getGradesByStatus(status);
        return Result.success(result);
    }

    @Operation(summary = "Get by enrollment year", description = "Gets a grade by enrollment year")
    @GetMapping("/by-year/{enrollmentYear}")
    @PreAuthorize("hasAuthority('system:grade:view')")
    public Result<GradeDTO> getByEnrollmentYear(
            @Parameter(description = "Enrollment year") @PathVariable Integer enrollmentYear) {
        GradeDTO result = gradeService.getGradeByEnrollmentYear(enrollmentYear);
        return Result.success(result);
    }

    @Operation(summary = "Activate grade", description = "Activates a grade (starts normal teaching)")
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('system:grade:update')")
    public Result<GradeDTO> activateGrade(
            @Parameter(description = "Grade ID") @PathVariable Long id) {
        GradeDTO result = gradeService.activateGrade(id, getCurrentUserId());
        return Result.success(result);
    }

    @Operation(summary = "Graduate grade", description = "Marks a grade as graduated")
    @PutMapping("/{id}/graduate")
    @PreAuthorize("hasAuthority('system:grade:update')")
    public Result<GradeDTO> graduateGrade(
            @Parameter(description = "Grade ID") @PathVariable Long id) {
        GradeDTO result = gradeService.graduateGrade(id, getCurrentUserId());
        return Result.success(result);
    }

    @Operation(summary = "Stop enrollment", description = "Stops enrollment for a grade")
    @PutMapping("/{id}/stop-enrollment")
    @PreAuthorize("hasAuthority('system:grade:update')")
    public Result<GradeDTO> stopEnrollment(
            @Parameter(description = "Grade ID") @PathVariable Long id) {
        GradeDTO result = gradeService.stopEnrollment(id, getCurrentUserId());
        return Result.success(result);
    }

    @Operation(summary = "Delete grade", description = "Deletes a grade")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:grade:delete')")
    public Result<Void> deleteGrade(
            @Parameter(description = "Grade ID") @PathVariable Long id) {
        gradeService.deleteGrade(id);
        return Result.success();
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId != null) {
                return userId;
            }
        } catch (Exception e) {
            log.warn("无法从安全上下文获取用户ID: {}", e.getMessage());
        }
        throw new RuntimeException("无法获取当前用户ID，请确保已登录");
    }
}
