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
@MapperScan({"com.school.management.mapper", "com.school.management.casbin.mapper"})
public class StudentManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentManagementApplication.class, args);
    }
}