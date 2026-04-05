package com.school.management.interfaces.rest.student;

import com.school.management.application.student.CohortApplicationService;
import com.school.management.application.student.command.AssignCohortLeaderCommand;
import com.school.management.application.student.command.CreateCohortCommand;
import com.school.management.application.student.command.UpdateCohortCommand;
import com.school.management.application.student.query.CohortDTO;
import com.school.management.common.result.Result;
import com.school.management.domain.student.model.CohortStatus;
import com.school.management.common.util.SecurityUtils;
import com.school.management.interfaces.rest.student.dto.AssignCohortLeaderRequest;
import com.school.management.interfaces.rest.student.dto.CreateCohortRequest;
import com.school.management.interfaces.rest.student.dto.UpdateCohortRequest;
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
 * REST API controller for cohort management.
 * Provides endpoints for CRUD operations and status management.
 */
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Cohorts", description = "Cohort management API")
@RestController("cohortController")
@RequestMapping("/students/cohorts")
public class CohortController {

    private final CohortApplicationService cohortService;

    @Operation(summary = "Create cohort", description = "Creates a new cohort")
    @PostMapping
    @CasbinAccess(resource = "student:cohort", action = "edit")
    public Result<CohortDTO> createCohort(@Valid @RequestBody CreateCohortRequest request) {
        CreateCohortCommand command = new CreateCohortCommand();
        command.setGradeCode(request.getGradeCode());
        command.setGradeName(request.getGradeName());
        command.setEnrollmentYear(request.getEnrollmentYear());
        command.setSchoolingYears(request.getSchoolingYears());
        command.setStandardClassSize(request.getStandardClassSize());
        command.setCreatedBy(SecurityUtils.requireCurrentUserId());

        CohortDTO result = cohortService.createCohort(command);
        return Result.success(result);
    }

    @Operation(summary = "Update cohort", description = "Updates an existing cohort")
    @PutMapping("/{id}")
    @CasbinAccess(resource = "student:cohort", action = "edit")
    public Result<CohortDTO> updateCohort(
            @Parameter(description = "Cohort ID") @PathVariable Long id,
            @RequestBody UpdateCohortRequest request) {
        UpdateCohortCommand command = new UpdateCohortCommand();
        command.setGradeName(request.getGradeName());
        command.setStandardClassSize(request.getStandardClassSize());
        command.setSortOrder(request.getSortOrder());
        command.setRemarks(request.getRemarks());
        command.setUpdatedBy(SecurityUtils.requireCurrentUserId());

        CohortDTO result = cohortService.updateCohort(id, command);
        return Result.success(result);
    }

    @Operation(summary = "Assign leaders", description = "Assigns director and counselor to a cohort")
    @PutMapping("/{id}/leaders")
    @CasbinAccess(resource = "student:cohort", action = "edit")
    public Result<CohortDTO> assignLeaders(
            @Parameter(description = "Cohort ID") @PathVariable Long id,
            @RequestBody AssignCohortLeaderRequest request) {
        AssignCohortLeaderCommand command = new AssignCohortLeaderCommand();
        command.setDirectorId(request.getDirectorId());
        command.setDirectorName(request.getDirectorName());
        command.setCounselorId(request.getCounselorId());
        command.setCounselorName(request.getCounselorName());
        command.setUpdatedBy(SecurityUtils.requireCurrentUserId());

        CohortDTO result = cohortService.assignLeaders(id, command);
        return Result.success(result);
    }

    @Operation(summary = "Get cohort", description = "Gets a cohort by ID")
    @GetMapping("/{id}")
    @CasbinAccess(resource = "student:cohort", action = "view")
    public Result<CohortDTO> getCohort(
            @Parameter(description = "Cohort ID") @PathVariable Long id) {
        CohortDTO result = cohortService.getCohort(id);
        return Result.success(result);
    }

    @Operation(summary = "Get all cohorts", description = "Gets all cohorts ordered by enrollment year")
    @GetMapping
    @CasbinAccess(resource = "student:cohort", action = "view")
    public Result<List<CohortDTO>> getAllCohorts() {
        List<CohortDTO> result = cohortService.getAllCohorts();
        return Result.success(result);
    }

    @Operation(summary = "Get active cohorts", description = "Gets all active cohorts")
    @GetMapping("/active")
    @CasbinAccess(resource = "student:cohort", action = "view")
    public Result<List<CohortDTO>> getActiveCohorts() {
        List<CohortDTO> result = cohortService.getActiveCohorts();
        return Result.success(result);
    }

    @Operation(summary = "Get by status", description = "Gets cohorts by status")
    @GetMapping("/by-status/{status}")
    @CasbinAccess(resource = "student:cohort", action = "view")
    public Result<List<CohortDTO>> getByStatus(
            @Parameter(description = "Cohort status") @PathVariable CohortStatus status) {
        List<CohortDTO> result = cohortService.getCohortsByStatus(status);
        return Result.success(result);
    }

    @Operation(summary = "Get by enrollment year", description = "Gets a cohort by enrollment year")
    @GetMapping("/by-year/{enrollmentYear}")
    @CasbinAccess(resource = "student:cohort", action = "view")
    public Result<CohortDTO> getByEnrollmentYear(
            @Parameter(description = "Enrollment year") @PathVariable Integer enrollmentYear) {
        CohortDTO result = cohortService.getCohortByEnrollmentYear(enrollmentYear);
        return Result.success(result);
    }

    @Operation(summary = "Activate cohort", description = "Activates a cohort (starts normal teaching)")
    @PutMapping("/{id}/activate")
    @CasbinAccess(resource = "student:cohort", action = "edit")
    public Result<CohortDTO> activateCohort(
            @Parameter(description = "Cohort ID") @PathVariable Long id) {
        CohortDTO result = cohortService.activateCohort(id, SecurityUtils.requireCurrentUserId());
        return Result.success(result);
    }

    @Operation(summary = "Graduate cohort", description = "Marks a cohort as graduated")
    @PutMapping("/{id}/graduate")
    @CasbinAccess(resource = "student:cohort", action = "edit")
    public Result<CohortDTO> graduateCohort(
            @Parameter(description = "Cohort ID") @PathVariable Long id) {
        CohortDTO result = cohortService.graduateCohort(id, SecurityUtils.requireCurrentUserId());
        return Result.success(result);
    }

    @Operation(summary = "Stop enrollment", description = "Stops enrollment for a cohort")
    @PutMapping("/{id}/stop-enrollment")
    @CasbinAccess(resource = "student:cohort", action = "edit")
    public Result<CohortDTO> stopEnrollment(
            @Parameter(description = "Cohort ID") @PathVariable Long id) {
        CohortDTO result = cohortService.stopEnrollment(id, SecurityUtils.requireCurrentUserId());
        return Result.success(result);
    }

    @Operation(summary = "Delete cohort", description = "Deletes a cohort")
    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "student:cohort", action = "edit")
    public Result<Void> deleteCohort(
            @Parameter(description = "Cohort ID") @PathVariable Long id) {
        cohortService.deleteCohort(id);
        return Result.success();
    }

}
