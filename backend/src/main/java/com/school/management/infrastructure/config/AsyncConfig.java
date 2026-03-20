package com.school.management.infrastructure.config;

import com.school.management.infrastructure.context.RequestContext;
import com.school.management.infrastructure.context.RequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * 用于批量Job的异步执行
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(5);

        // 最大线程数
        executor.setMaxPoolSize(10);

        // 队列容量
        executor.setQueueCapacity(100);

        // 线程名前缀
        executor.setThreadNamePrefix("place-async-");

        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);

        // 等待任务完成后再关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 最长等待时间（秒）
        executor.setAwaitTerminationSeconds(60);

        // TaskDecorator：传播 RequestContext 到异步线程
        executor.setTaskDecorator(runnable -> {
            RequestContext ctx = null;
            try {
                ctx = RequestContextHolder.getContext();
            } catch (Exception e) {
                // 没有上下文时忽略
            }
            final RequestContext capturedCtx = ctx;
            return () -> {
                try {
                    if (capturedCtx != null) {
                        RequestContextHolder.setContext(capturedCtx);
                    }
                    runnable.run();
                } finally {
                    RequestContextHolder.clearContext();
                }
            };
        });

        executor.initialize();

        log.info("异步任务线程池已初始化: corePoolSize=5, maxPoolSize=10, queueCapacity=100, TaskDecorator已启用");

        return executor;
    }
}
