package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.ExaminationApplicationService;
import com.school.management.application.teaching.command.CreateExamArrangementCommand;
import com.school.management.application.teaching.command.CreateExamBatchCommand;
import com.school.management.application.teaching.query.ExamArrangementDTO;
import com.school.management.application.teaching.query.ExamBatchDTO;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * 考试管理控制器
 */
@Tag(name = "考试管理", description = "考试批次和考试安排的管理")
@RestController
@RequestMapping("/api/v2/teaching/examinations")
@RequiredArgsConstructor
public class ExaminationController {

    private final ExaminationApplicationService examService;

    @Operation(summary = "创建考试批次")
    @PostMapping("/batches")
    @PreAuthorize("hasAuthority('teaching:exam:create')")
    public Result<Long> createBatch(
            @Valid @RequestBody CreateExamBatchRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        CreateExamBatchCommand command = CreateExamBatchCommand.builder()
                .semesterId(request.getSemesterId())
                .batchName(request.getBatchName())
                .examType(request.getExamType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .remark(request.getRemark())
                .operatorId(user.getId())
                .build();

        Long id = examService.createBatch(command);
        return Result.success(id);
    }

    @Operation(summary = "更新考试批次")
    @PutMapping("/batches/{id}")
    @PreAuthorize("hasAuthority('teaching:exam:update')")
    public Result<Void> updateBatch(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExamBatchRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        examService.updateBatch(id, request.getBatchName(), request.getExamType(),
                request.getStartDate(), request.getEndDate(), request.getRemark(), user.getId());
        return Result.success();
    }

    @Operation(summary = "发布考试批次")
    @PostMapping("/batches/{id}/publish")
    @PreAuthorize("hasAuthority('teaching:exam:publish')")
    public Result<Void> publishBatch(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        examService.publishBatch(id, user.getId());
        return Result.success();
    }

    @Operation(summary = "删除考试批次")
    @DeleteMapping("/batches/{id}")
    @PreAuthorize("hasAuthority('teaching:exam:delete')")
    public Result<Void> deleteBatch(@PathVariable Long id) {
        examService.deleteBatch(id);
        return Result.success();
    }

    @Operation(summary = "获取考试批次详情")
    @GetMapping("/batches/{id}")
    @PreAuthorize("hasAuthority('teaching:exam:view')")
    public Result<ExamBatchDTO> getBatch(@PathVariable Long id) {
        return Result.success(examService.getBatch(id));
    }

    @Operation(summary = "根据学期获取考试批次列表")
    @GetMapping("/batches/semester/{semesterId}")
    @PreAuthorize("hasAuthority('teaching:exam:view')")
    public Result<List<ExamBatchDTO>> getBatchesBySemester(@PathVariable Long semesterId) {
        return Result.success(examService.getBatchesBySemester(semesterId));
    }

    @Operation(summary = "分页查询考试批次")
    @GetMapping("/batches/page")
    @PreAuthorize("hasAuthority('teaching:exam:view')")
    public Result<PageResult<ExamBatchDTO>> getBatchesPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer examType,
            @RequestParam(required = false) Integer status) {

        List<ExamBatchDTO> list = examService.getBatchesPage(page, size, semesterId, examType, status);
        long total = examService.countBatches(semesterId, examType, status);

        return Result.success(new PageResult<>(list, total, page, size));
    }

    @Operation(summary = "添加考试安排")
    @PostMapping("/arrangements")
    @PreAuthorize("hasAuthority('teaching:exam:update')")
    public Result<Long> addArrangement(
            @Valid @RequestBody CreateExamArrangementRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        CreateExamArrangementCommand command = CreateExamArrangementCommand.builder()
                .batchId(request.getBatchId())
                .courseId(request.getCourseId())
                .examDate(request.getExamDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .examRooms(request.getExamRooms() != null ? request.getExamRooms().stream()
                        .map(r -> CreateExamArrangementCommand.ExamRoomItem.builder()
                                .classroomId(r.getClassroomId())
                                .capacity(r.getCapacity())
                                .invigilatorIds(r.getInvigilatorIds())
                                .build())
                        .collect(Collectors.toList()) : null)
                .remark(request.getRemark())
                .operatorId(user.getId())
                .build();

        Long id = examService.addArrangement(command);
        return Result.success(id);
    }

    @Operation(summary = "删除考试安排")
    @DeleteMapping("/arrangements/{id}")
    @PreAuthorize("hasAuthority('teaching:exam:update')")
    public Result<Void> deleteArrangement(@PathVariable Long id) {
        examService.deleteArrangement(id);
        return Result.success();
    }

    @Operation(summary = "获取考试安排详情")
    @GetMapping("/arrangements/{id}")
    @PreAuthorize("hasAuthority('teaching:exam:view')")
    public Result<ExamArrangementDTO> getArrangement(@PathVariable Long id) {
        return Result.success(examService.getArrangement(id));
    }

    @Operation(summary = "根据课程获取考试安排")
    @GetMapping("/arrangements/course/{courseId}")
    @PreAuthorize("hasAuthority('teaching:exam:view')")
    public Result<List<ExamArrangementDTO>> getArrangementsByCourse(
            @RequestParam Long semesterId,
            @PathVariable Long courseId) {
        return Result.success(examService.getArrangementsByCourse(semesterId, courseId));
    }

    @Operation(summary = "根据教室获取考试安排")
    @GetMapping("/arrangements/classroom/{classroomId}")
    @PreAuthorize("hasAuthority('teaching:exam:view')")
    public Result<List<ExamArrangementDTO>> getArrangementsByClassroom(
            @RequestParam Long semesterId,
            @PathVariable Long classroomId) {
        return Result.success(examService.getArrangementsByClassroom(semesterId, classroomId));
    }
}
