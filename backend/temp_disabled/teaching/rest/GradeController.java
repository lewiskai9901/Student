package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.GradeApplicationService;
import com.school.management.application.teaching.command.CreateGradeBatchCommand;
import com.school.management.application.teaching.command.RecordGradeCommand;
import com.school.management.application.teaching.query.GradeBatchDTO;
import com.school.management.application.teaching.query.StudentGradeDTO;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "成绩管理", description = "成绩录入、查询和统计")
@RestController
@RequestMapping("/api/v2/teaching/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeApplicationService gradeService;

    @Operation(summary = "创建成绩批次")
    @PostMapping("/batches")
    @PreAuthorize("hasAuthority('teaching:grade:create')")
    public Result<Long> createBatch(@Valid @RequestBody CreateGradeBatchRequest req,
                                     @AuthenticationPrincipal CustomUserDetails user) {
        CreateGradeBatchCommand cmd = CreateGradeBatchCommand.builder()
                .semesterId(req.getSemesterId()).courseId(req.getCourseId()).classId(req.getClassId())
                .batchName(req.getBatchName()).gradeType(req.getGradeType()).operatorId(user.getId()).build();
        return Result.success(gradeService.createBatch(cmd));
    }

    @Operation(summary = "录入单个成绩")
    @PostMapping("/record")
    @PreAuthorize("hasAuthority('teaching:grade:record')")
    public Result<Void> recordGrade(@Valid @RequestBody RecordGradeRequest req,
                                     @AuthenticationPrincipal CustomUserDetails user) {
        RecordGradeCommand cmd = RecordGradeCommand.builder()
                .batchId(req.getBatchId()).studentId(req.getStudentId()).totalScore(req.getTotalScore())
                .items(req.getItems() != null ? req.getItems().stream()
                        .map(i -> RecordGradeCommand.GradeItem.builder()
                                .itemName(i.getItemName()).score(i.getScore()).build())
                        .collect(Collectors.toList()) : null)
                .remark(req.getRemark()).operatorId(user.getId()).build();
        gradeService.recordGrade(cmd);
        return Result.success();
    }

    @Operation(summary = "批量录入成绩")
    @PostMapping("/batches/{batchId}/batch-record")
    @PreAuthorize("hasAuthority('teaching:grade:record')")
    public Result<Void> batchRecordGrades(@PathVariable Long batchId,
                                           @Valid @RequestBody List<RecordGradeRequest> reqs,
                                           @AuthenticationPrincipal CustomUserDetails user) {
        List<RecordGradeCommand> cmds = reqs.stream()
                .map(r -> RecordGradeCommand.builder().studentId(r.getStudentId()).totalScore(r.getTotalScore())
                        .remark(r.getRemark()).build())
                .collect(Collectors.toList());
        gradeService.batchRecordGrades(batchId, cmds, user.getId());
        return Result.success();
    }

    @Operation(summary = "提交成绩批次")
    @PostMapping("/batches/{id}/submit")
    @PreAuthorize("hasAuthority('teaching:grade:submit')")
    public Result<Void> submitBatch(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        gradeService.submitBatch(id, user.getId());
        return Result.success();
    }

    @Operation(summary = "发布成绩批次")
    @PostMapping("/batches/{id}/publish")
    @PreAuthorize("hasAuthority('teaching:grade:publish')")
    public Result<Void> publishBatch(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        gradeService.publishBatch(id, user.getId());
        return Result.success();
    }

    @Operation(summary = "获取成绩批次详情")
    @GetMapping("/batches/{id}")
    @PreAuthorize("hasAuthority('teaching:grade:view')")
    public Result<GradeBatchDTO> getBatch(@PathVariable Long id) {
        return Result.success(gradeService.getBatch(id));
    }

    @Operation(summary = "获取批次成绩列表")
    @GetMapping("/batches/{batchId}/grades")
    @PreAuthorize("hasAuthority('teaching:grade:view')")
    public Result<List<StudentGradeDTO>> getGradesByBatch(@PathVariable Long batchId) {
        return Result.success(gradeService.getGradesByBatch(batchId));
    }

    @Operation(summary = "获取学生成绩")
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAuthority('teaching:grade:view')")
    public Result<List<StudentGradeDTO>> getStudentGrades(@PathVariable Long studentId,
                                                           @RequestParam Long semesterId) {
        return Result.success(gradeService.getStudentGrades(studentId, semesterId));
    }

    @Operation(summary = "计算学生GPA")
    @GetMapping("/student/{studentId}/gpa")
    @PreAuthorize("hasAuthority('teaching:grade:view')")
    public Result<BigDecimal> calculateGPA(@PathVariable Long studentId, @RequestParam Long semesterId) {
        return Result.success(gradeService.calculateGPA(studentId, semesterId));
    }

    @Operation(summary = "分页查询成绩批次")
    @GetMapping("/batches/page")
    @PreAuthorize("hasAuthority('teaching:grade:view')")
    public Result<PageResult<GradeBatchDTO>> getBatchesPage(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long semesterId, @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer status) {
        List<GradeBatchDTO> list = gradeService.getBatchesPage(page, size, semesterId, courseId, status);
        long total = gradeService.countBatches(semesterId, courseId, status);
        return Result.success(new PageResult<>(list, total, page, size));
    }
}
