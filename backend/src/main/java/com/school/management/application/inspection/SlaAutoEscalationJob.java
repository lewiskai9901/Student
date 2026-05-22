package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.corrective.CorrectiveCase;
import com.school.management.domain.inspection.repository.CorrectiveCaseRepository;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SLA 超时自动升级定时任务
 * 每 5 分钟检查逾期的整改案例，自动升级并发送告警
 */
@Component
public class SlaAutoEscalationJob {

    private static final Logger log = LoggerFactory.getLogger(SlaAutoEscalationJob.class);

    private final CorrectiveCaseRepository repository;
    private final SpringDomainEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public SlaAutoEscalationJob(CorrectiveCaseRepository repository,
                                SpringDomainEventPublisher eventPublisher,
                                TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * P2#17: 逐个案例独立事务提交.
     *
     * <p>原实现整批包在一个 {@code @Transactional} 内, 单个案例 {@code catch}
     * 后继续 — 但同一事务里前面案例触发过的异常 (即便 catch) 仍可能把事务标为
     * rollback-only, 导致最终整批回滚, "部分成功"是假象. 改为每个案例用
     * {@code TransactionTemplate} 独立提交: 一个失败只回滚自己, 不牵连他人.
     * (参考 InspTaskApplicationService.reassignDepartedInspector 同款写法.)
     */
    @Scheduled(fixedRate = 300_000) // 5 minutes
    public void checkSlaBreaches() {
        List<CorrectiveCase> overdueCases = repository.findOverdue(LocalDateTime.now());
        int count = 0;
        for (CorrectiveCase c : overdueCases) {
            Boolean ok = transactionTemplate.execute(status -> {
                try {
                    CorrectiveCase fresh = repository.findById(c.getId()).orElse(null);
                    if (fresh == null) return false;
                    fresh.slaBreach();
                    repository.save(fresh);
                    eventPublisher.publishAll(fresh.getDomainEvents());
                    fresh.clearDomainEvents();
                    return true;
                } catch (Exception e) {
                    log.error("SLA升级失败 (本案例已回滚): caseId={}, caseCode={}",
                            c.getId(), c.getCaseCode(), e);
                    status.setRollbackOnly();
                    return false;
                }
            });
            if (Boolean.TRUE.equals(ok)) count++;
        }
        if (count > 0) {
            log.info("SLA自动升级: 处理 {} 个逾期案例", count);
        }
    }
}
