package com.school.management.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 缓存预热 - 应用启动时预加载常用数据到Redis
 *
 * 优点:
 * 1. 减少首次请求的响应时间
 * 2. 避免缓存冷启动导致的数据库压力
 * 3. 提升用户体验
 *
 * @author Claude
 * @since 2025-12-28
 */
@Component
@Slf4j
public class CacheWarmupRunner implements ApplicationRunner {

    private final ApplicationContext applicationContext;

    public CacheWarmupRunner(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== 开始缓存预热 ==========");
        long startTime = System.currentTimeMillis();

        try {
            // 预热部门数据
            warmupDepartments();

            // 预热年级数据
            warmupGrades();

            // 预热专业数据
            warmupMajors();

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("========== 缓存预热完成,耗时: {} ms ==========", elapsedTime);

        } catch (Exception e) {
            log.error("缓存预热失败,应用将正常启动但缓存未预加载", e);
            // 不抛出异常,确保应用正常启动
        }
    }

    /**
     * 预热部门数据
     */
    private void warmupDepartments() {
        try {
            Object departmentService = getBeanSafely("com.school.management.service.DepartmentService");
            if (departmentService != null) {
                // 通过反射调用getAllDepartments方法
                Object result = departmentService.getClass()
                        .getMethod("getAllDepartments")
                        .invoke(departmentService);
                if (result instanceof java.util.List) {
                    int count = ((java.util.List<?>) result).size();
                    log.info("✓ 预热部门数据: {} 条", count);
                }
            }
        } catch (Exception e) {
            log.warn("预热部门数据失败: {}", e.getMessage());
        }
    }

    /**
     * 预热年级数据
     */
    private void warmupGrades() {
        try {
            Object gradeService = getBeanSafely("com.school.management.service.GradeService");
            if (gradeService != null) {
                Object result = gradeService.getClass()
                        .getMethod("getAllGrades")
                        .invoke(gradeService);
                if (result instanceof java.util.List) {
                    int count = ((java.util.List<?>) result).size();
                    log.info("✓ 预热年级数据: {} 条", count);
                }
            }
        } catch (Exception e) {
            log.warn("预热年级数据失败: {}", e.getMessage());
        }
    }

    /**
     * 预热专业数据
     */
    private void warmupMajors() {
        try {
            Object majorService = getBeanSafely("com.school.management.service.MajorService");
            if (majorService != null) {
                Object result = majorService.getClass()
                        .getMethod("getAllMajors")
                        .invoke(majorService);
                if (result instanceof java.util.List) {
                    int count = ((java.util.List<?>) result).size();
                    log.info("✓ 预热专业数据: {} 条", count);
                }
            }
        } catch (Exception e) {
            log.warn("预热专业数据失败: {}", e.getMessage());
        }
    }

    /**
     * 安全地获取Bean,如果不存在则返回null
     */
    private Object getBeanSafely(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return applicationContext.getBean(clazz);
        } catch (Exception e) {
            return null;
        }
    }
}
