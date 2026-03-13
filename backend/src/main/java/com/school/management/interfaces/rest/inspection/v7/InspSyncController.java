package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v7.execution.InspSubmission;
import com.school.management.domain.inspection.model.v7.execution.SubmissionDetail;
import com.school.management.domain.inspection.repository.v7.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.v7.SubmissionDetailRepository;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * V7 检查平台 - 离线同步端点
 * 支持移动端离线数据拉取和批量推送
 */
@Slf4j
@RestController
@RequestMapping("/v7/insp/sync")
@RequiredArgsConstructor
public class InspSyncController {

    private final InspSubmissionRepository submissionRepository;
    private final SubmissionDetailRepository detailRepository;

    /**
     * 拉取同步数据 — 获取任务下自某时间后被修改的提交和明细
     */
    @PostMapping("/pull")
    @CasbinAccess(resource = "insp:submission", action = "view")
    public Result<SyncPullResponse> pull(@RequestBody SyncPullRequest request) {
        List<InspSubmission> submissions;
        if (request.getLastSyncAt() != null) {
            submissions = submissionRepository.findModifiedAfter(
                    request.getTaskId(), request.getLastSyncAt());
        } else {
            submissions = submissionRepository.findByTaskId(request.getTaskId());
        }

        List<SubmissionDetail> allDetails = new ArrayList<>();
        for (InspSubmission sub : submissions) {
            allDetails.addAll(detailRepository.findBySubmissionId(sub.getId()));
        }

        SyncPullResponse response = new SyncPullResponse();
        response.setSubmissions(submissions);
        response.setDetails(allDetails);
        response.setServerTime(LocalDateTime.now());
        return Result.success(response);
    }

    /**
     * 批量推送离线表单数据 — 乐观锁冲突检测
     */
    @PostMapping("/push")
    @CasbinAccess(resource = "insp:submission", action = "execute")
    @Transactional
    public Result<SyncPushResponse> push(@RequestBody SyncPushRequest request) {
        List<SyncPushResult> results = new ArrayList<>();

        for (SyncPushItem item : request.getItems()) {
            SyncPushResult result = new SyncPushResult();
            result.setSubmissionId(item.getSubmissionId());

            InspSubmission submission = submissionRepository.findById(item.getSubmissionId())
                    .orElse(null);
            if (submission == null) {
                result.setStatus("NOT_FOUND");
                results.add(result);
                continue;
            }

            // Optimistic lock: compare client syncVersion with server
            if (submission.getSyncVersion() != null
                    && !submission.getSyncVersion().equals(item.getClientSyncVersion())) {
                // Conflict detected
                result.setStatus("CONFLICT");
                result.setServerSyncVersion(submission.getSyncVersion());
                result.setServerFormData(submission.getFormData());
                result.setServerUpdatedAt(submission.getUpdatedAt());
                results.add(result);
                log.info("Sync conflict for submission {}: client v{} vs server v{}",
                        item.getSubmissionId(), item.getClientSyncVersion(), submission.getSyncVersion());
                continue;
            }

            // No conflict — apply the change
            try {
                submission.saveFormData(item.getFormData());
                submissionRepository.save(submission);
                result.setStatus("SYNCED");
                result.setServerSyncVersion(submission.getSyncVersion());
            } catch (IllegalStateException e) {
                result.setStatus("REJECTED");
                result.setServerSyncVersion(submission.getSyncVersion());
                log.warn("Sync rejected for submission {}: {}", item.getSubmissionId(), e.getMessage());
            }
            results.add(result);
        }

        SyncPushResponse response = new SyncPushResponse();
        response.setResults(results);
        response.setServerTime(LocalDateTime.now());
        return Result.success(response);
    }

    // ========== Request/Response DTOs ==========

    @lombok.Data
    public static class SyncPullRequest {
        private Long taskId;
        private LocalDateTime lastSyncAt;
    }

    @lombok.Data
    public static class SyncPullResponse {
        private List<InspSubmission> submissions;
        private List<SubmissionDetail> details;
        private LocalDateTime serverTime;
    }

    @lombok.Data
    public static class SyncPushRequest {
        private List<SyncPushItem> items;
    }

    @lombok.Data
    public static class SyncPushItem {
        private Long submissionId;
        private String formData;
        private Integer clientSyncVersion;
    }

    @lombok.Data
    public static class SyncPushResult {
        private Long submissionId;
        private String status; // SYNCED, CONFLICT, NOT_FOUND, REJECTED
        private Integer serverSyncVersion;
        private String serverFormData;
        private LocalDateTime serverUpdatedAt;
    }

    @lombok.Data
    public static class SyncPushResponse {
        private List<SyncPushResult> results;
        private LocalDateTime serverTime;
    }
}
