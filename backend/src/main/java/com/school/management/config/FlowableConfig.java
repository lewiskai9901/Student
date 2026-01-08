package com.school.management.config;

import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.engine.*;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * Flowable工作流引擎配置
 */
@Slf4j
@Configuration
public class FlowableConfig {

    /**
     * 流程引擎配置
     */
    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> engineConfigurationConfigurer() {
        return configuration -> {
            // 设置ID生成器为UUID
            configuration.setIdGenerator(new IdGenerator() {
                @Override
                public String getNextId() {
                    return UUID.randomUUID().toString().replace("-", "");
                }
            });

            // 设置活动字体(解决中文乱码)
            configuration.setActivityFontName("宋体");
            configuration.setLabelFontName("宋体");
            configuration.setAnnotationFontName("宋体");

            log.info("Flowable流程引擎配置完成");
        };
    }

    /**
     * 运行时服务
     */
    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    /**
     * 任务服务
     */
    @Bean
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    /**
     * 仓储服务
     */
    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    /**
     * 历史服务
     */
    @Bean
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    /**
     * 管理服务
     */
    @Bean
    public ManagementService managementService(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }

    /**
     * 身份服务
     */
    @Bean
    public IdentityService identityService(ProcessEngine processEngine) {
        return processEngine.getIdentityService();
    }
}
