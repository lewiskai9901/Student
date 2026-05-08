package com.school.management.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置.
 *
 * <p>MetaObjectHandler 采用 Composite 模式 — 见
 * {@link com.school.management.infrastructure.persistence.fill.CompositeMetaObjectHandler}
 * 注入多个 {@link com.school.management.infrastructure.persistence.fill.FieldFillerStrategy}
 * 策略 (审计字段 / inspection orgUnitId / 等). 不在本类直接 @Bean handler 以免冲突.
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 配置分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 乐观锁插件（必须在分页插件之前）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }

}