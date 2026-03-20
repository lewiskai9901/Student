package com.school.management.interfaces.rest.place;

import com.school.management.application.place.PlaceBatchJobExecutor;
import com.school.management.application.place.PlaceBatchJobService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.context.RequestContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 场所批量Job管理接口
 * 对标: AWS S3 Batch Operations API
 */
@Tag(name = "场所批量操作", description = "场所批量操作Job管理API")
@RestController
@RequestMapping("/v9/places/batch-jobs")
@RequiredArgsConstructor
public class PlaceBatchJobController {

    private final PlaceBatchJobService batchJobService;
    private final PlaceBatchJobExecutor batchJobExecutor;

    /**
     * 提交批量组织分配Job
     */
    @Operation(summary = "批量分配组织", description = "批量将场所分配给指定组织（传入null表示清除覆盖，恢复继承）")
    @PostMapping("/assign-org")
    @CasbinAccess(resource = "place", action = "batch-assign")
    public Result<BatchJobSubmitResponse> submitBatchAssignOrg(@Valid @RequestBody BatchAssignOrgRequest request) {
        Long currentUserId = RequestContextHolder.getCurrentUserId();
        String currentUserName = RequestContextHolder.getCurrentUserName();

        String jobId = batchJobExecutor.submitAndExecuteBatchAssignOrg(
                request.getPlaceIds(),
                request.getTargetOrgUnitId(),
                request.getReason(),
                currentUserId,
                currentUserName
        );

        return Result.success(new BatchJobSubmitResponse(jobId, "批量组织分配Job已提交"));
    }

    /**
     * 提交批量状态变更Job
     */
    @Operation(summary = "批量变更状态", description = "批量变更场所状态（NORMAL/DISABLED/MAINTENANCE）")
    @PostMapping("/change-status")
    @CasbinAccess(resource = "place", action = "batch-update")
    public Result<BatchJobSubmitResponse> submitBatchChangeStatus(@Valid @RequestBody BatchChangeStatusRequest request) {
        Long currentUserId = RequestContextHolder.getCurrentUserId();
        String currentUserName = RequestContextHolder.getCurrentUserName();

        String jobId = batchJobExecutor.submitAndExecuteBatchChangeStatus(
                request.getPlaceIds(),
                request.getTargetStatus(),
                request.getReason(),
                currentUserId,
                currentUserName
        );

        return Result.success(new BatchJobSubmitResponse(jobId, "批量状态变更Job已提交"));
    }

    /**
     * 提交批量分配负责人Job
     */
    @Operation(summary = "批量分配负责人", description = "批量将场所分配给指定负责人（传入null表示清除）")
    @PostMapping("/assign-responsible")
    @CasbinAccess(resource = "place", action = "batch-assign")
    public Result<BatchJobSubmitResponse> submitBatchAssignResponsible(@Valid @RequestBody BatchAssignResponsibleRequest request) {
        Long currentUserId = RequestContextHolder.getCurrentUserId();
        String currentUserName = RequestContextHolder.getCurrentUserName();

        String jobId = batchJobExecutor.submitAndExecuteBatchAssignResponsible(
                request.getPlaceIds(),
                request.getTargetResponsibleUserId(),
                request.getReason(),
                currentUserId,
                currentUserName
        );

        return Result.success(new BatchJobSubmitResponse(jobId, "批量负责人分配Job已提交"));
    }

    /**
     * 查询Job状态
     */
    @Operation(summary = "查询Job状态", description = "根据JobID查询批量操作任务的执行状态和进度")
    @GetMapping("/{jobId}")
    @CasbinAccess(resource = "place", action = "view")
    public Result<PlaceBatchJobService.BatchJobDTO> getJobStatus(
            @Parameter(description = "Job ID") @PathVariable String jobId) {
        return Result.success(batchJobService.getJobStatus(jobId));
    }

    /**
     * 查询我的最近Job列表
     */
    @Operation(summary = "查询我的最近Job", description = "查询当前用户创建的最近批量操作任务列表")
    @GetMapping("/my-recent")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<PlaceBatchJobService.BatchJobDTO>> getMyRecentJobs(
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "20") int limit) {
        Long currentUserId = RequestContextHolder.getCurrentUserId();
        return Result.success(batchJobService.getRecentJobs(currentUserId, limit));
    }

    /**
     * 取消Job
     */
    @Operation(summary = "取消Job", description = "取消正在执行或待执行的批量操作任务")
    @PostMapping("/{jobId}/cancel")
    @CasbinAccess(resource = "place", action = "batch-assign")
    public Result<Void> cancelJob(@Parameter(description = "Job ID") @PathVariable String jobId) {
        batchJobService.cancelJob(jobId);
        return Result.success();
    }

    // ===== Request DTOs =====

    /**
     * 批量组织分配请求
     */
    @Data
    public static class BatchAssignOrgRequest {
        @NotEmpty(message = "场所ID列表不能为空")
        private List<Long> placeIds;

        private Long targetOrgUnitId;  // null表示清除覆盖

        private String reason;
    }

    /**
     * 批量状态变更请求
     */
    @Data
    public static class BatchChangeStatusRequest {
        @NotEmpty(message = "场所ID列表不能为空")
        private List<Long> placeIds;

        @NotNull(message = "目标状态不能为空")
        private String targetStatus;  // NORMAL/DISABLED/MAINTENANCE

        private String reason;
    }

    /**
     * 批量负责人分配请求
     */
    @Data
    public static class BatchAssignResponsibleRequest {
        @NotEmpty(message = "场所ID列表不能为空")
        private List<Long> placeIds;

        private Long targetResponsibleUserId;  // null表示清除负责人

        private String reason;
    }

    /**
     * Job提交响应
     */
    @Data
    public static class BatchJobSubmitResponse {
        private String jobId;
        private String message;

        public BatchJobSubmitResponse(String jobId, String message) {
            this.jobId = jobId;
            this.message = message;
        }
    }
}
