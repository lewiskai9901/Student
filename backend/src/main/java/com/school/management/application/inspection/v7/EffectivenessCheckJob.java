package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.event.v7.EffectivenessFailedEvent;
import com.school.management.domain.inspection.model.v7.corrective.CaseStatus;
import com.school.management.domain.inspection.model.v7.corrective.CorrectiveCase;
import com.school.management.domain.inspection.model.v7.corrective.EffectivenessStatus;
import com.school.management.domain.inspection.repository.v7.CorrectiveCaseRepository;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 效果验证检查定时任务
 * 每天 8:00 AM 检查已关闭案例中到达效果验证日期的案例，发送提醒
 */
@Component
public class EffectivenessCheckJob {

    private static final Logger log = LoggerFactory.getLogger(EffectivenessCheckJob.class);

    private final CorrectiveCaseRepository repository;
    private final SpringDomainEventPublisher eventPublisher;

    public EffectivenessCheckJob(CorrectiveCaseRepository repository,
                                 SpringDomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "0 0 8 * * ?") // 每天 8:00 AM
    public void checkPendingEffectivenessReviews() {
        List<CorrectiveCase> closedCases = repository.findByStatus(CaseStatus.CLOSED);
        LocalDate today = LocalDate.now();

        // 先筛选出到期案例，再统一处理
        List<CorrectiveCase> dueCases = closedCases.stream()
                .filter(c -> c.getEffectivenessStatus() == EffectivenessStatus.PENDING
                        && c.getEffectivenessCheckDate() != null
                        && !c.getEffectivenessCheckDate().isAfter(today))
                .toList();

        if (dueCases.isEmpty()) {
            return;
        }

        log.info("效果验证检查: 共 {} 个已关闭案例中 {} 个到期需要验证", closedCases.size(), dueCases.size());
        for (CorrectiveCase c : dueCases) {
            log.debug("效果验证到期: caseId={}, caseCode={}, checkDate={}",
                    c.getId(), c.getCaseCode(), c.getEffectivenessCheckDate());
            eventPublisher.publish(new EffectivenessFailedEvent(c.getId(), c.getCaseCode(), null));
        }
    }
}
