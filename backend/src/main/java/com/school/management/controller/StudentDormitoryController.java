package com.school.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.result.Result;
import com.school.management.dto.*;
import com.school.management.service.StudentDormitoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生住宿管理控制器
 *
 * @author system
 * @since 1.0.0
 */
@Tag(name = "学生住宿管理", description = "学生入住、退宿、调换宿舍等接口")
@RestController
@RequestMapping("/student-dormitory")
@RequiredArgsConstructor
public class StudentDormitoryController {

    private final StudentDormitoryService studentDormitoryService;

    @Operation(summary = "分页查询住宿记录")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('dormitory:student:view', 'student:info:view')")
    public Result<Page<StudentDormitoryResponse>> getPage(StudentDormitoryQueryRequest request) {
        return Result.success(studentDormitoryService.getPage(request));
    }

    @Operation(summary = "查询学生当前住宿信息")
    @GetMapping("/student/{studentId}/current")
    @PreAuthorize("hasAnyAuthority('dormitory:student:view', 'student:info:view')")
    public Result<StudentDormitoryResponse> getCurrentByStudentId(@PathVariable Long studentId) {
        return Result.success(studentDormitoryService.getCurrentByStudentId(studentId));
    }

    @Operation(summary = "查询宿舍当前入住学生列表")
    @GetMapping("/dormitory/{dormitoryId}/students")
    @PreAuthorize("hasAnyAuthority('dormitory:student:view', 'dormitory:room:view')")
    public Result<List<StudentDormitoryResponse>> getCurrentByDormitoryId(@PathVariable Long dormitoryId) {
        return Result.success(studentDormitoryService.getCurrentByDormitoryId(dormitoryId));
    }

    @Operation(summary = "查询学生住宿历史")
    @GetMapping("/student/{studentId}/history")
    @PreAuthorize("hasAnyAuthority('dormitory:student:view', 'student:info:view')")
    public Result<List<StudentDormitoryResponse>> getHistoryByStudentId(@PathVariable Long studentId) {
        return Result.success(studentDormitoryService.getHistoryByStudentId(studentId));
    }

    @Operation(summary = "学生入住")
    @PostMapping("/check-in")
    @PreAuthorize("hasAuthority('dormitory:student:assign')")
    public Result<Long> checkIn(@Valid @RequestBody StudentCheckInRequest request) {
        return Result.success(studentDormitoryService.checkIn(request));
    }

    @Operation(summary = "学生退宿")
    @PostMapping("/check-out")
    @PreAuthorize("hasAuthority('dormitory:student:assign')")
    public Result<Void> checkOut(@Valid @RequestBody StudentCheckOutRequest request) {
        studentDormitoryService.checkOut(request);
        return Result.success();
    }

    @Operation(summary = "学生调换宿舍")
    @PostMapping("/change")
    @PreAuthorize("hasAuthority('dormitory:student:assign')")
    public Result<Void> changeDormitory(@Valid @RequestBody StudentChangeDormitoryRequest request) {
        studentDormitoryService.changeDormitory(request);
        return Result.success();
    }

    @Operation(summary = "批量入住")
    @PostMapping("/batch-check-in")
    @PreAuthorize("hasAuthority('dormitory:student:assign')")
    public Result<Integer> batchCheckIn(@Valid @RequestBody List<StudentCheckInRequest> requests) {
        return Result.success(studentDormitoryService.batchCheckIn(requests));
    }

    @Operation(summary = "批量退宿")
    @PostMapping("/batch-check-out")
    @PreAuthorize("hasAuthority('dormitory:student:assign')")
    public Result<Integer> batchCheckOut(@RequestBody List<Long> studentIds,
                                         @RequestParam(required = false) String reason) {
        return Result.success(studentDormitoryService.batchCheckOut(studentIds, reason));
    }

    @Operation(summary = "同步学生表宿舍数据(数据迁移用)")
    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('system:admin')")
    public Result<Integer> syncFromStudentTable() {
        return Result.success(studentDormitoryService.syncFromStudentTable());
    }
}
