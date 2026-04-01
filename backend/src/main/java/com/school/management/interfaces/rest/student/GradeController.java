package com.school.management.interfaces.rest.student;

import com.school.management.application.student.GradeApplicationService;
import com.school.management.application.student.command.AssignGradeLeaderCommand;
import com.school.management.application.student.command.CreateGradeCommand;
import com.school.management.application.student.command.UpdateGradeCommand;
import com.school.management.application.student.query.GradeDTO;
import com.school.management.common.result.Result;
import com.school.management.domain.student.model.GradeStatus;
import com.school.management.common.util.SecurityUtils;
import com.school.management.interfaces.rest.student.dto.AssignGradeLeaderRequest;
import com.school.management.interfaces.rest.student.dto.CreateGradeRequest;
import com.school.management.interfaces.rest.student.dto.UpdateGradeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST API controller for grade management.
 * Provides endpoints for CRUD operations and status management.
 */
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Grades", description = "Grade management API")
@RestController("gradeController")
@RequestMapping("/students/grades")
public class GradeController {

    private final GradeApplicationService gradeService;

    @Operation(summary = "Create grade", description = "Creates a new grade")
    @PostMapping
    @CasbinAccess(resource = "system:grade", action = "create")
    public Result<GradeDTO> createGrade(@Valid @RequestBody CreateGradeRequest request) {
        CreateGradeCommand command = new CreateGradeCommand();
        command.setGradeCode(request.getGradeCode());
        command.setGradeName(request.getGradeName());
        command.setEnrollmentYear(request.getEnrollmentYear());
        command.setSchoolingYears(request.getSchoolingYears());
        command.setStandardClassSize(request.getStandardClassSize());
        command.setCreatedBy(SecurityUtils.requireCurrentUserId());

        GradeDTO result = gradeService.createGrade(command);
        return Result.success(result);
    }

    @Operation(summary = "Update grade", description = "Updates an existing grade")
    @PutMapping("/{id}")
    @CasbinAccess(resource = "system:grade", action = "update")
    public Result<GradeDTO> updateGrade(
            @Parameter(description = "Grade ID") @PathVariable Long id,
            @RequestBody UpdateGradeRequest request) {
        UpdateGradeCommand command = new UpdateGradeCommand();
        command.setGradeName(request.getGradeName());
        command.setStandardClassSize(request.getStandardClassSize());
        command.setSortOrder(request.getSortOrder());
        command.setRemarks(request.getRemarks());
        command.setUpdatedBy(SecurityUtils.requireCurrentUserId());

        GradeDTO result = gradeService.updateGrade(id, command);
        return Result.success(result);
    }

    @Operation(summary = "Assign leaders", description = "Assigns director and counselor to a grade")
    @PutMapping("/{id}/leaders")
    @CasbinAccess(resource = "system:grade", action = "update")
    public Result<GradeDTO> assignLeaders(
            @Parameter(description = "Grade ID") @PathVariable Long id,
            @RequestBody AssignGradeLeaderRequest request) {
        AssignGradeLeaderCommand command = new AssignGradeLeaderCommand();
        command.setDirectorId(request.getDirectorId());
        command.setDirectorName(request.getDirectorName());
        command.setCounselorId(request.getCounselorId());
        command.setCounselorName(request.getCounselorName());
        command.setUpdatedBy(SecurityUtils.requireCurrentUserId());

        GradeDTO result = gradeService.assignLeaders(id, command);
        return Result.success(result);
    }

    @Operation(summary = "Get grade", description = "Gets a grade by ID")
    @GetMapping("/{id}")
    @CasbinAccess(resource = "system:grade", action = "view")
    public Result<GradeDTO> getGrade(
            @Parameter(description = "Grade ID") @PathVariable Long id) {
        GradeDTO result = gradeService.getGrade(id);
        return Result.success(result);
    }

    @Operation(summary = "Get all grades", description = "Gets all grades ordered by enrollment year")
    @GetMapping
    @CasbinAccess(resource = "system:grade", action = "view")
    public Result<List<GradeDTO>> getAllGrades() {
        List<GradeDTO> result = gradeService.getAllGrades();
        return Result.success(result);
    }

    @Operation(summary = "Get active grades", description = "Gets all active grades")
    @GetMapping("/active")
    @CasbinAccess(resource = "system:grade", action = "view")
    public Result<List<GradeDTO>> getActiveGrades() {
        List<GradeDTO> result = gradeService.getActiveGrades();
        return Result.success(result);
    }

    @Operation(summary = "Get by status", description = "Gets grades by status")
    @GetMapping("/by-status/{status}")
    @CasbinAccess(resource = "system:grade", action = "view")
    public Result<List<GradeDTO>> getByStatus(
            @Parameter(description = "Grade status") @PathVariable GradeStatus status) {
        List<GradeDTO> result = gradeService.getGradesByStatus(status);
        return Result.success(result);
    }

    @Operation(summary = "Get by enrollment year", description = "Gets a grade by enrollment year")
    @GetMapping("/by-year/{enrollmentYear}")
    @CasbinAccess(resource = "system:grade", action = "view")
    public Result<GradeDTO> getByEnrollmentYear(
            @Parameter(description = "Enrollment year") @PathVariable Integer enrollmentYear) {
        GradeDTO result = gradeService.getGradeByEnrollmentYear(enrollmentYear);
        return Result.success(result);
    }

    @Operation(summary = "Activate grade", description = "Activates a grade (starts normal teaching)")
    @PutMapping("/{id}/activate")
    @CasbinAccess(resource = "system:grade", action = "update")
    public Result<GradeDTO> activateGrade(
            @Parameter(description = "Grade ID") @PathVariable Long id) {
        GradeDTO result = gradeService.activateGrade(id, SecurityUtils.requireCurrentUserId());
        return Result.success(result);
    }

    @Operation(summary = "Graduate grade", description = "Marks a grade as graduated")
    @PutMapping("/{id}/graduate")
    @CasbinAccess(resource = "system:grade", action = "update")
    public Result<GradeDTO> graduateGrade(
            @Parameter(description = "Grade ID") @PathVariable Long id) {
        GradeDTO result = gradeService.graduateGrade(id, SecurityUtils.requireCurrentUserId());
        return Result.success(result);
    }

    @Operation(summary = "Stop enrollment", description = "Stops enrollment for a grade")
    @PutMapping("/{id}/stop-enrollment")
    @CasbinAccess(resource = "system:grade", action = "update")
    public Result<GradeDTO> stopEnrollment(
            @Parameter(description = "Grade ID") @PathVariable Long id) {
        GradeDTO result = gradeService.stopEnrollment(id, SecurityUtils.requireCurrentUserId());
        return Result.success(result);
    }

    @Operation(summary = "Delete grade", description = "Deletes a grade")
    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "system:grade", action = "delete")
    public Result<Void> deleteGrade(
            @Parameter(description = "Grade ID") @PathVariable Long id) {
        gradeService.deleteGrade(id);
        return Result.success();
    }

}
