package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.corrective.CorrectiveCase;
import com.school.management.domain.inspection.repository.v7.CorrectiveCaseRepository;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    public SlaAutoEscalationJob(CorrectiveCaseRepository repository,
                                SpringDomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedRate = 300_000) // 5 minutes
    @Transactional
    public void checkSlaBreaches() {
        List<CorrectiveCase> overdueCases = repository.findOverdue(LocalDateTime.now());
        int count = 0;
        for (CorrectiveCase c : overdueCases) {
            try {
                c.slaBreach();
                repository.save(c);
                eventPublisher.publishAll(c.getDomainEvents());
                c.clearDomainEvents();
                count++;
            } catch (Exception e) {
                log.error("SLA升级失败: caseId={}, caseCode={}", c.getId(), c.getCaseCode(), e);
            }
        }
        if (count > 0) {
            log.info("SLA自动升级: 处理 {} 个逾期案例", count);
        }
    }
}
