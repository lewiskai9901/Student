package com.school.management.application.place;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.place.model.valueobject.PlaceStatus;
import com.school.management.infrastructure.persistence.place.PlaceBatchJobMapper;
import com.school.management.infrastructure.persistence.place.PlaceBatchJobPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 批量Job执行器
 * 异步执行批量操作任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceBatchJobExecutor {

    private final PlaceBatchJobMapper batchJobMapper;
    private final PlaceBatchItemProcessor itemProcessor;
    private final PlaceBatchJobService batchJobService;
    private final ObjectMapper objectMapper;

    /**
     * 异步执行批量组织分配Job
     *
     * @param jobId 任务ID
     */
    @Async
    public void executeBatchAssignOrg(String jobId) {
        log.info("开始执行批量组织分配Job: jobId={}", jobId);

        PlaceBatchJobPO job = batchJobMapper.selectById(jobId);
        if (job == null) {
            log.error("Job不存在: {}", jobId);
            return;
        }

        try {
            // 解析请求参数
            Map<String, Object> params = objectMapper.readValue(
                    job.getRequestParameters(),
                    new TypeReference<Map<String, Object>>() {}
            );

            @SuppressWarnings("unchecked")
            List<Integer> placeIdInts = (List<Integer>) params.get("placeIds");
            List<Long> placeIds = placeIdInts.stream()
                    .map(Integer::longValue)
                    .toList();

            Object orgUnitIdObj = params.get("targetOrgUnitId");
            Long targetOrgUnitId = (orgUnitIdObj != null) ? ((Number) orgUnitIdObj).longValue() : null;
            String reason = (String) params.get("reason");

            // 逐个处理（独立事务，委托到独立Bean）
            for (Long placeId : placeIds) {
                try {
                    itemProcessor.processAssignOrg(placeId, targetOrgUnitId, reason);
                    batchJobService.updateProgress(jobId, 1, 1, 0);
                } catch (Exception e) {
                    log.error("处理场所失败: placeId={}, error={}", placeId, e.getMessage());
                    batchJobService.updateProgress(jobId, 1, 0, 1);
                }
            }

            log.info("批量组织分配Job执行完成: jobId={}", jobId);

        } catch (Exception e) {
            log.error("执行批量组织分配Job失败: jobId={}", jobId, e);
            job.setJobStatus("FAILED");
            job.setLastError(e.getMessage());
            batchJobMapper.updateById(job);
        }
    }

    /**
     * 异步执行批量状态变更Job
     *
     * @param jobId 任务ID
     */
    @Async
    public void executeBatchChangeStatus(String jobId) {
        log.info("开始执行批量状态变更Job: jobId={}", jobId);

        PlaceBatchJobPO job = batchJobMapper.selectById(jobId);
        if (job == null) {
            log.error("Job不存在: {}", jobId);
            return;
        }

        try {
            // 解析请求参数
            Map<String, Object> params = objectMapper.readValue(
                    job.getRequestParameters(),
                    new TypeReference<Map<String, Object>>() {}
            );

            @SuppressWarnings("unchecked")
            List<Integer> placeIdInts = (List<Integer>) params.get("placeIds");
            List<Long> placeIds = placeIdInts.stream()
                    .map(Integer::longValue)
                    .toList();

            String targetStatusStr = (String) params.get("targetStatus");
            PlaceStatus targetStatus = PlaceStatus.valueOf(targetStatusStr);
            String reason = (String) params.get("reason");

            // 逐个处理（独立事务，委托到独立Bean）
            for (Long placeId : placeIds) {
                try {
                    itemProcessor.processChangeStatus(placeId, targetStatus, reason);
                    batchJobService.updateProgress(jobId, 1, 1, 0);
                } catch (Exception e) {
                    log.error("处理场所失败: placeId={}, error={}", placeId, e.getMessage());
                    batchJobService.updateProgress(jobId, 1, 0, 1);
                }
            }

            log.info("批量状态变更Job执行完成: jobId={}", jobId);

        } catch (Exception e) {
            log.error("执行批量状态变更Job失败: jobId={}", jobId, e);
            job.setJobStatus("FAILED");
            job.setLastError(e.getMessage());
            batchJobMapper.updateById(job);
        }
    }

    /**
     * 提交并异步执行批量组织分配
     * （便捷方法）
     */
    public String submitAndExecuteBatchAssignOrg(List<Long> placeIds, Long targetOrgUnitId,
                                                 String reason, Long createdBy, String createdByName) {
        // 提交Job
        String jobId = batchJobService.submitBatchAssignOrg(
                placeIds, targetOrgUnitId, reason, createdBy, createdByName
        );

        // 异步执行
        executeBatchAssignOrg(jobId);

        return jobId;
    }

    /**
     * 提交并异步执行批量状态变更
     * （便捷方法）
     */
    public String submitAndExecuteBatchChangeStatus(List<Long> placeIds, String targetStatus,
                                                    String reason, Long createdBy, String createdByName) {
        // 提交Job
        String jobId = batchJobService.submitBatchChangeStatus(
                placeIds, targetStatus, reason, createdBy, createdByName
        );

        // 异步执行
        executeBatchChangeStatus(jobId);

        return jobId;
    }

    /**
     * 异步执行批量负责人分配Job
     *
     * @param jobId 任务ID
     */
    @Async
    public void executeBatchAssignResponsible(String jobId) {
        log.info("开始执行批量负责人分配Job: jobId={}", jobId);

        PlaceBatchJobPO job = batchJobMapper.selectById(jobId);
        if (job == null) {
            log.error("Job不存在: {}", jobId);
            return;
        }

        try {
            // 解析请求参数
            Map<String, Object> params = objectMapper.readValue(
                    job.getRequestParameters(),
                    new TypeReference<Map<String, Object>>() {}
            );

            @SuppressWarnings("unchecked")
            List<Integer> placeIdInts = (List<Integer>) params.get("placeIds");
            List<Long> placeIds = placeIdInts.stream()
                    .map(Integer::longValue)
                    .toList();

            Object responsibleUserIdObj = params.get("targetResponsibleUserId");
            Long targetResponsibleUserId = (responsibleUserIdObj != null)
                ? ((Number) responsibleUserIdObj).longValue() : null;
            String reason = (String) params.get("reason");

            // 逐个处理（独立事务，委托到独立Bean）
            for (Long placeId : placeIds) {
                try {
                    itemProcessor.processAssignResponsible(placeId, targetResponsibleUserId, reason);
                    batchJobService.updateProgress(jobId, 1, 1, 0);
                } catch (Exception e) {
                    log.error("处理场所失败: placeId={}, error={}", placeId, e.getMessage());
                    batchJobService.updateProgress(jobId, 1, 0, 1);
                }
            }

            log.info("批量负责人分配Job执行完成: jobId={}", jobId);

        } catch (Exception e) {
            log.error("执行批量负责人分配Job失败: jobId={}", jobId, e);
            job.setJobStatus("FAILED");
            job.setLastError(e.getMessage());
            batchJobMapper.updateById(job);
        }
    }

    /**
     * 提交并异步执行批量负责人分配
     * （便捷方法）
     */
    public String submitAndExecuteBatchAssignResponsible(List<Long> placeIds, Long targetResponsibleUserId,
                                                         String reason, Long createdBy, String createdByName) {
        // 提交Job
        String jobId = batchJobService.submitBatchAssignResponsible(
                placeIds, targetResponsibleUserId, reason, createdBy, createdByName
        );

        // 异步执行
        executeBatchAssignResponsible(jobId);

        return jobId;
    }
}
