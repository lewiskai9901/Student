package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.corrective.CaseStatus;
import com.school.management.domain.inspection.model.v7.corrective.CorrectiveCase;
import com.school.management.domain.inspection.model.v7.corrective.EffectivenessStatus;
import com.school.management.domain.inspection.repository.v7.CorrectiveCaseRepository;
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

    public EffectivenessCheckJob(CorrectiveCaseRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 8 * * ?") // 每天 8:00 AM
    public void checkPendingEffectivenessReviews() {
        List<CorrectiveCase> closedCases = repository.findByStatus(CaseStatus.CLOSED);
        LocalDate today = LocalDate.now();
        int dueCount = 0;

        for (CorrectiveCase c : closedCases) {
            if (c.getEffectivenessStatus() == EffectivenessStatus.PENDING
                    && c.getEffectivenessCheckDate() != null
                    && !c.getEffectivenessCheckDate().isAfter(today)) {
                // 到期或已过期，需要进行效果验证
                log.info("效果验证到期提醒: caseId={}, caseCode={}, checkDate={}",
                        c.getId(), c.getCaseCode(), c.getEffectivenessCheckDate());
                dueCount++;
                // TODO: 通过 NotificationRuleEngine 发送提醒通知
            }
        }
        if (dueCount > 0) {
            log.info("效果验证检查: {} 个案例到期需要验证", dueCount);
        }
    }
}
