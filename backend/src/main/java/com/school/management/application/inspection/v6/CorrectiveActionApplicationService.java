package com.school.management.application.inspection.v6;

import com.school.management.domain.inspection.model.v6.CorrectiveAction;
import com.school.management.domain.inspection.model.v6.CorrectiveActionStatus;
import com.school.management.domain.inspection.repository.v6.CorrectiveActionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * V6整改记录应用服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CorrectiveActionApplicationService {

    private final CorrectiveActionRepository correctiveActionRepository;
    private final AtomicLong codeSequence = new AtomicLong(0);

    /**
     * 创建整改记录
     */
    @Transactional
    public CorrectiveAction createAction(Long detailId, Long targetId, Long taskId, Long projectId,
                                         String issueDescription, String requiredAction, LocalDate deadline,
                                         Long assigneeId, String assigneeName, Long createdBy) {
        String actionCode = generateActionCode();
        CorrectiveAction action = CorrectiveAction.create(detailId, targetId, taskId, projectId,
                actionCode, issueDescription, requiredAction, deadline, assigneeId, assigneeName, createdBy);
        return correctiveActionRepository.save(action);
    }

    /**
     * 提交整改
     */
    @Transactional
    public CorrectiveAction submitCorrection(Long id, String correctionNote, String evidenceIds) {
        CorrectiveAction action = correctiveActionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改记录不存在: " + id));
        action.submitCorrection(correctionNote, evidenceIds);
        return correctiveActionRepository.save(action);
    }

    /**
     * 验收通过
     */
    @Transactional
    public CorrectiveAction verify(Long id, Long verifierId, String verifierName, String verificationNote) {
        CorrectiveAction action = correctiveActionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改记录不存在: " + id));
        action.verify(verifierId, verifierName, verificationNote);
        return correctiveActionRepository.save(action);
    }

    /**
     * 验收驳回
     */
    @Transactional
    public CorrectiveAction reject(Long id, Long verifierId, String verifierName, String verificationNote) {
        CorrectiveAction action = correctiveActionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改记录不存在: " + id));
        action.reject(verifierId, verifierName, verificationNote);
        return correctiveActionRepository.save(action);
    }

    /**
     * 取消整改
     */
    @Transactional
    public void cancelAction(Long id) {
        CorrectiveAction action = correctiveActionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改记录不存在: " + id));
        action.cancel();
        correctiveActionRepository.save(action);
    }

    /**
     * 删除整改记录
     */
    @Transactional
    public void deleteAction(Long id) {
        correctiveActionRepository.deleteById(id);
    }

    /**
     * 根据ID查询
     */
    @Transactional(readOnly = true)
    public CorrectiveAction getById(Long id) {
        return correctiveActionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改记录不存在: " + id));
    }

    /**
     * 根据检查明细查询
     */
    @Transactional(readOnly = true)
    public List<CorrectiveAction> getByDetailId(Long detailId) {
        return correctiveActionRepository.findByDetailId(detailId);
    }

    /**
     * 根据检查目标查询
     */
    @Transactional(readOnly = true)
    public List<CorrectiveAction> getByTargetId(Long targetId) {
        return correctiveActionRepository.findByTargetId(targetId);
    }

    /**
     * 根据检查任务查询
     */
    @Transactional(readOnly = true)
    public List<CorrectiveAction> getByTaskId(Long taskId) {
        return correctiveActionRepository.findByTaskId(taskId);
    }

    /**
     * 根据检查项目查询
     */
    @Transactional(readOnly = true)
    public List<CorrectiveAction> getByProjectId(Long projectId) {
        return correctiveActionRepository.findByProjectId(projectId);
    }

    /**
     * 查询我的待整改
     */
    @Transactional(readOnly = true)
    public List<CorrectiveAction> getMyPendingActions(Long assigneeId) {
        return correctiveActionRepository.findByAssigneeId(assigneeId).stream()
                .filter(a -> a.getStatus() == CorrectiveActionStatus.PENDING || a.getStatus() == CorrectiveActionStatus.REJECTED)
                .toList();
    }

    /**
     * 查询逾期整改
     */
    @Transactional(readOnly = true)
    public List<CorrectiveAction> getOverdueActions() {
        return correctiveActionRepository.findOverdue();
    }

    /**
     * 根据项目和状态查询
     */
    @Transactional(readOnly = true)
    public List<CorrectiveAction> getByProjectIdAndStatus(Long projectId, CorrectiveActionStatus status) {
        return correctiveActionRepository.findByProjectIdAndStatus(projectId, status);
    }

    /**
     * 统计项目整改情况
     */
    @Transactional(readOnly = true)
    public CorrectiveActionStats getProjectStats(Long projectId) {
        long pending = correctiveActionRepository.countByProjectIdAndStatus(projectId, CorrectiveActionStatus.PENDING);
        long submitted = correctiveActionRepository.countByProjectIdAndStatus(projectId, CorrectiveActionStatus.SUBMITTED);
        long verified = correctiveActionRepository.countByProjectIdAndStatus(projectId, CorrectiveActionStatus.VERIFIED);
        long rejected = correctiveActionRepository.countByProjectIdAndStatus(projectId, CorrectiveActionStatus.REJECTED);
        return new CorrectiveActionStats(pending, submitted, verified, rejected);
    }

    private String generateActionCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = codeSequence.incrementAndGet() % 10000;
        return String.format("CA%s%04d", dateStr, seq);
    }

    public record CorrectiveActionStats(long pending, long submitted, long verified, long rejected) {
        public long total() {
            return pending + submitted + verified + rejected;
        }
    }
}
