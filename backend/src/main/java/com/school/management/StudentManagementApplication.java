package com.school.management;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 学生管理系统启动类
 *
 * @author system
 * @since 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableTransactionManagement
@EnableScheduling
@MapperScan(
    // annotationClass=Mapper.class: 只把带 @Mapper 注解的 interface 注册为 Mapper bean,
    // 跳过同包的 Service interface (FileStorageService / NotificationService).
    // 项目所有 78 个 Mapper 都已显式 @Mapper, 无回归.
    annotationClass = org.apache.ibatis.annotations.Mapper.class,
    basePackages = {
        "com.school.management.infrastructure.persistence",
        "com.school.management.infrastructure.external",
        "com.school.management.infrastructure.activity.impl",
        // Phase 3.5: 教育插件迁入的 mappers (calendar/teaching/academic/student)
        "com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence"
    }
)
public class StudentManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentManagementApplication.class, args);
    }
}