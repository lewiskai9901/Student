package com.school.management.application.place;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.infrastructure.persistence.place.PlaceBatchJobMapper;
import com.school.management.infrastructure.persistence.place.PlaceBatchJobPO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 场所批量操作Job服务
 * 对标: AWS S3 Batch Operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceBatchJobService {

    private final PlaceBatchJobMapper batchJobMapper;
    private final ObjectMapper objectMapper;

    /**
     * 提交批量组织分配Job
     *
     * @param placeIds 场所ID列表
     * @param targetOrgUnitId 目标组织单元ID（null表示清除覆盖）
     * @param reason 原因
     * @param createdBy 创建用户ID
     * @param createdByName 创建用户名
     * @return Job ID
     */
    @Transactional
    public String submitBatchAssignOrg(List<Long> placeIds, Long targetOrgUnitId,
                                       String reason, Long createdBy, String createdByName) {
        String jobId = UUID.randomUUID().toString();

        // 构建请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("placeIds", placeIds);
        params.put("targetOrgUnitId", targetOrgUnitId);
        params.put("reason", reason);

        String requestParams;
        try {
            requestParams = objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            log.error("序列化请求参数失败", e);
            requestParams = "{}";
        }

        PlaceBatchJobPO job = PlaceBatchJobPO.builder()
                .jobId(jobId)
                .jobType("BATCH_ASSIGN_ORG")
                .jobName(targetOrgUnitId != null ?
                        "批量分配组织到: " + targetOrgUnitId : "批量清除组织覆盖")
                .jobStatus("PENDING")
                .totalItems(placeIds.size())
                .processedItems(0)
                .successCount(0)
                .failureCount(0)
                .skippedCount(0)
                .requestParameters(requestParams)
                .createdBy(createdBy)
                .createdByName(createdByName)
                .createdAt(LocalDateTime.now())
                .build();

        batchJobMapper.insert(job);

        log.info("已提交批量组织分配Job: jobId={}, totalItems={}", jobId, placeIds.size());

        return jobId;
    }

    /**
     * 提交批量状态变更Job
     *
     * @param placeIds 场所ID列表
     * @param targetStatus 目标状态
     * @param reason 原因
     * @param createdBy 创建用户ID
     * @param createdByName 创建用户名
     * @return Job ID
     */
    @Transactional
    public String submitBatchChangeStatus(List<Long> placeIds, String targetStatus,
                                          String reason, Long createdBy, String createdByName) {
        String jobId = UUID.randomUUID().toString();

        Map<String, Object> params = new HashMap<>();
        params.put("placeIds", placeIds);
        params.put("targetStatus", targetStatus);
        params.put("reason", reason);

        String requestParams;
        try {
            requestParams = objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            log.error("序列化请求参数失败", e);
            requestParams = "{}";
        }

        PlaceBatchJobPO job = PlaceBatchJobPO.builder()
                .jobId(jobId)
                .jobType("BATCH_CHANGE_STATUS")
                .jobName("批量变更状态到: " + targetStatus)
                .jobStatus("PENDING")
                .totalItems(placeIds.size())
                .processedItems(0)
                .successCount(0)
                .failureCount(0)
                .skippedCount(0)
                .requestParameters(requestParams)
                .createdBy(createdBy)
                .createdByName(createdByName)
                .createdAt(LocalDateTime.now())
                .build();

        batchJobMapper.insert(job);

        log.info("已提交批量状态变更Job: jobId={}, totalItems={}", jobId, placeIds.size());

        return jobId;
    }

    /**
     * 提交批量负责人分配Job
     *
     * @param placeIds 场所ID列表
     * @param targetResponsibleUserId 目标负责人ID（null表示清除）
     * @param reason 原因
     * @param createdBy 创建用户ID
     * @param createdByName 创建用户名
     * @return Job ID
     */
    @Transactional
    public String submitBatchAssignResponsible(List<Long> placeIds, Long targetResponsibleUserId,
                                              String reason, Long createdBy, String createdByName) {
        String jobId = UUID.randomUUID().toString();

        Map<String, Object> params = new HashMap<>();
        params.put("placeIds", placeIds);
        params.put("targetResponsibleUserId", targetResponsibleUserId);
        params.put("reason", reason);

        String requestParams;
        try {
            requestParams = objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            log.error("序列化请求参数失败", e);
            requestParams = "{}";
        }

        PlaceBatchJobPO job = PlaceBatchJobPO.builder()
                .jobId(jobId)
                .jobType("BATCH_ASSIGN_RESPONSIBLE")
                .jobName(targetResponsibleUserId != null ?
                        "批量分配负责人: " + targetResponsibleUserId : "批量清除负责人")
                .jobStatus("PENDING")
                .totalItems(placeIds.size())
                .processedItems(0)
                .successCount(0)
                .failureCount(0)
                .skippedCount(0)
                .requestParameters(requestParams)
                .createdBy(createdBy)
                .createdByName(createdByName)
                .createdAt(LocalDateTime.now())
                .build();

        batchJobMapper.insert(job);

        log.info("已提交批量负责人分配Job: jobId={}, totalItems={}", jobId, placeIds.size());

        return jobId;
    }

    /**
     * 查询Job状态
     *
     * @param jobId 任务ID
     * @return Job DTO
     */
    public BatchJobDTO getJobStatus(String jobId) {
        PlaceBatchJobPO job = batchJobMapper.selectById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Job不存在: " + jobId);
        }
        return toBatchJobDTO(job);
    }

    /**
     * 查询用户创建的最近Job列表
     *
     * @param createdBy 创建用户ID
     * @param limit 限制数量
     * @return Job DTO列表
     */
    public List<BatchJobDTO> getRecentJobs(Long createdBy, int limit) {
        List<PlaceBatchJobPO> jobs = batchJobMapper.findRecentByCreatedBy(createdBy, limit);
        return jobs.stream()
                .map(this::toBatchJobDTO)
                .collect(Collectors.toList());
    }

    /**
     * 更新Job进度（由Executor调用）
     *
     * @param jobId 任务ID
     * @param processedIncrement 已处理增量
     * @param successIncrement 成功增量
     * @param failureIncrement 失败增量
     */
    @Transactional
    public void updateProgress(String jobId, int processedIncrement,
                              int successIncrement, int failureIncrement) {
        batchJobMapper.updateProgress(jobId, processedIncrement, successIncrement, failureIncrement);

        // 检查是否完成
        batchJobMapper.markAsCompleted(jobId);

        log.debug("已更新Job进度: jobId={}, processed+={}, success+={}, failure+={}",
                jobId, processedIncrement, successIncrement, failureIncrement);
    }

    /**
     * 取消Job
     *
     * @param jobId 任务ID
     */
    @Transactional
    public void cancelJob(String jobId) {
        PlaceBatchJobPO job = batchJobMapper.selectById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Job不存在: " + jobId);
        }

        if ("COMPLETED".equals(job.getJobStatus()) || "FAILED".equals(job.getJobStatus())) {
            throw new IllegalStateException("Job已完成或失败，无法取消");
        }

        job.setJobStatus("CANCELLED");
        job.setCompletedAt(LocalDateTime.now());
        batchJobMapper.updateById(job);

        log.info("已取消Job: jobId={}", jobId);
    }

    // ===== DTO转换 =====

    /**
     * 转换为BatchJobDTO
     */
    private BatchJobDTO toBatchJobDTO(PlaceBatchJobPO po) {
        BatchJobDTO dto = new BatchJobDTO();
        dto.setJobId(po.getJobId());
        dto.setJobType(po.getJobType());
        dto.setJobName(po.getJobName());
        dto.setJobStatus(po.getJobStatus());
        dto.setTotalItems(po.getTotalItems());
        dto.setProcessedItems(po.getProcessedItems());
        dto.setSuccessCount(po.getSuccessCount());
        dto.setFailureCount(po.getFailureCount());
        dto.setSkippedCount(po.getSkippedCount());
        dto.setProgressPercentage(po.getProgressPercentage());
        dto.setCreatedAt(po.getCreatedAt());
        dto.setStartedAt(po.getStartedAt());
        dto.setCompletedAt(po.getCompletedAt());
        dto.setCreatedBy(po.getCreatedBy());
        dto.setCreatedByName(po.getCreatedByName());
        dto.setLastError(po.getLastError());
        return dto;
    }

    /**
     * 批量Job DTO
     */
    @Data
    public static class BatchJobDTO {
        private String jobId;
        private String jobType;
        private String jobName;
        private String jobStatus;
        private Integer totalItems;
        private Integer processedItems;
        private Integer successCount;
        private Integer failureCount;
        private Integer skippedCount;
        private Object progressPercentage;
        private LocalDateTime createdAt;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private Long createdBy;
        private String createdByName;
        private String lastError;
    }
}
