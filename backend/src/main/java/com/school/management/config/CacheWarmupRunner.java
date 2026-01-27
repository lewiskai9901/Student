package com.school.management.config;

import com.school.management.application.organization.OrgUnitApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 缓存预热 - 应用启动时预加载常用数据到Redis
 */
@Component
@Slf4j
public class CacheWarmupRunner implements ApplicationRunner {

    private final OrgUnitApplicationService orgUnitApplicationService;

    public CacheWarmupRunner(OrgUnitApplicationService orgUnitApplicationService) {
        this.orgUnitApplicationService = orgUnitApplicationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== 开始缓存预热 ==========");
        long startTime = System.currentTimeMillis();

        try {
            warmupOrgUnits();

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("========== 缓存预热完成,耗时: {} ms ==========", elapsedTime);
        } catch (Exception e) {
            log.error("缓存预热失败,应用将正常启动但缓存未预加载", e);
        }
    }

    private void warmupOrgUnits() {
        try {
            int count = orgUnitApplicationService.getOrgUnitTree().size();
            log.info("预热组织单元数据: {} 条", count);
        } catch (Exception e) {
            log.warn("预热组织单元数据失败: {}", e.getMessage());
        }
    }
}
