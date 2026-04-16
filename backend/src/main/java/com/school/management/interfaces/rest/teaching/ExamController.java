package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.ExamApplicationService;
import com.school.management.common.audit.Audited;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.persistence.teaching.exam.ExamArrangementPO;
import com.school.management.infrastructure.persistence.teaching.exam.ExamBatchPO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 考试管理 REST Controller (DDD-lite)
 * 处理考试批次、考试安排、考场分配、监考分配的 CRUD
 */
@RestController
@RequestMapping("/teaching/examinations")
@RequiredArgsConstructor
public class ExamController {

    private final ExamApplicationService examService;

    // ==================== 考试批次管理 ====================

    @GetMapping("/batches")
    @CasbinAccess(resource = "teaching:exam", action = "view")
    public Result<Map<String, Object>> listBatches(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer examType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(examService.listBatches(semesterId, examType, status, pageNum, pageSize));
    }

    @GetMapping("/batches/{id}")
    @CasbinAccess(resource = "teaching:exam", action = "view")
    public Result<ExamBatchPO> getBatch(@PathVariable Long id) {
        return Result.success(examService.getBatch(id));
    }

    @PostMapping("/batches")
    @CasbinAccess(resource = "teaching:exam", action = "edit")
    @Audited(module = "teaching", action = "CREATE", resourceType = "ExamBatch", description = "创建考试批次")
    public Result<ExamBatchPO> createBatch(@RequestBody Map<String, Object> data) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(examService.createBatch(data, userId));
    }

    @PutMapping("/batches/{id}")
    @CasbinAccess(resource = "teaching:exam", action = "edit")
    public Result<Void> updateBatch(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        examService.updateBatch(id, data);
        return Result.success();
    }

    @DeleteMapping("/batches/{id}")
    @CasbinAccess(resource = "teaching:exam", action = "edit")
    public Result<Void> deleteBatch(@PathVariable Long id) {
        examService.deleteBatch(id);
        return Result.success();
    }

    @PostMapping("/batches/{id}/publish")
    @CasbinAccess(resource = "teaching:exam", action = "edit")
    @Audited(module = "teaching", action = "PUBLISH", resourceType = "ExamBatch", description = "发布考试批次")
    public Result<Void> publishBatch(@PathVariable Long id) {
        examService.publishBatch(id);
        return Result.success();
    }

    // ==================== 冲突检测 ====================

    @GetMapping("/batches/{batchId}/conflicts")
    @CasbinAccess(resource = "teaching:exam", action = "view")
    public Result<List<Map<String, Object>>> detectConflicts(@PathVariable Long batchId) {
        return Result.success(examService.detectExamConflicts(batchId));
    }

    // ==================== 考试安排 ====================

    @GetMapping("/batches/{batchId}/arrangements")
    @CasbinAccess(resource = "teaching:exam", action = "view")
    public Result<List<Map<String, Object>>> listArrangements(@PathVariable Long batchId) {
        return Result.success(examService.listArrangements(batchId));
    }

    @PostMapping("/batches/{batchId}/arrangements")
    @CasbinAccess(resource = "teaching:exam", action = "edit")
    public Result<ExamArrangementPO> createArrangement(
            @PathVariable Long batchId,
            @RequestBody Map<String, Object> data) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(examService.createArrangement(batchId, data, userId));
    }

    @PutMapping("/batches/{batchId}/arrangements/{arrangementId}")
    @CasbinAccess(resource = "teaching:exam", action = "edit")
    public Result<Void> updateArrangement(
            @PathVariable Long batchId,
            @PathVariable Long arrangementId,
            @RequestBody Map<String, Object> data) {
        examService.updateArrangement(batchId, arrangementId, data);
        return Result.success();
    }

    @DeleteMapping("/batches/{batchId}/arrangements/{arrangementId}")
    @CasbinAccess(resource = "teaching:exam", action = "edit")
    public Result<Void> deleteArrangement(
            @PathVariable Long batchId,
            @PathVariable Long arrangementId) {
        examService.deleteArrangement(batchId, arrangementId);
        return Result.success();
    }

    // ==================== 考场分配 ====================

    @PostMapping("/arrangements/{arrangementId}/rooms")
    @CasbinAccess(resource = "teaching:exam", action = "edit")
    public Result<Void> assignRooms(
            @PathVariable Long arrangementId,
            @RequestBody Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rooms = (List<Map<String, Object>>) data.get("rooms");
        examService.assignRooms(arrangementId, rooms);
        return Result.success();
    }

    // ==================== 监考分配 ====================

    @PostMapping("/rooms/{roomId}/invigilators")
    @CasbinAccess(resource = "teaching:exam", action = "edit")
    public Result<Void> assignInvigilators(
            @PathVariable Long roomId,
            @RequestBody Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Number> teacherIds = (List<Number>) data.get("teacherIds");
        Long mainTeacherId = data.get("mainTeacherId") != null ? ((Number) data.get("mainTeacherId")).longValue() : null;
        examService.assignInvigilators(roomId, teacherIds, mainTeacherId);
        return Result.success();
    }
}
