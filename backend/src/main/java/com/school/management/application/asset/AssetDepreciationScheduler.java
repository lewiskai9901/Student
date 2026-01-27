package com.school.management.application.asset;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * 资产折旧定时任务
 * 每月1日自动计提折旧
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssetDepreciationScheduler {

    private final AssetDepreciationApplicationService depreciationService;

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * 每月1日凌晨3点计提折旧
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    public void calculateMonthlyDepreciation() {
        log.info("开始执行月度折旧计提任务...");

        try {
            // 计算上个月的折旧（因为在1号执行，折旧是针对上个月的）
            String period = YearMonth.now().minusMonths(1).format(PERIOD_FORMATTER);

            int count = depreciationService.calculateAllDepreciation(period);

            log.info("月度折旧计提完成，期间: {}, 处理资产数: {}", period, count);
        } catch (Exception e) {
            log.error("月度折旧计提失败", e);
        }
    }

    /**
     * 手动触发折旧计算（用于补提或测试）
     */
    public void manualCalculate(String period) {
        log.info("手动触发折旧计算，期间: {}", period);

        try {
            int count = depreciationService.calculateAllDepreciation(period);
            log.info("折旧计算完成，期间: {}, 处理资产数: {}", period, count);
        } catch (Exception e) {
            log.error("折旧计算失败，期间: {}", period, e);
            throw e;
        }
    }
}
