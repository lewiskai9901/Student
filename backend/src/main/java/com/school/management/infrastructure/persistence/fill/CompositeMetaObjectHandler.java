package com.school.management.infrastructure.persistence.fill;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 唯一对外暴露的 MyBatis-Plus MetaObjectHandler bean.
 *
 * <p>用 {@link ObjectProvider} 懒解析 {@link FieldFillerStrategy} 集合 — 必须如此因为存在
 * 循环依赖链:
 * <pre>
 * sqlSessionFactory → MetaObjectHandler → strategy → mapper → sqlSessionFactory
 * </pre>
 * 直接 inject {@code List<FieldFillerStrategy>} 会让 Spring 启动期就要求
 * 所有 strategy + 其传递依赖 (含 Mapper) 全部就绪, 与 sqlSessionFactory 启动顺序冲突.
 * ObjectProvider 在每次 {@link #insertFill}/{@link #updateFill} 调用时才 resolve, 此时
 * mapper bean 已就绪.
 *
 * <p>顺序由 strategy 自身的 {@link org.springframework.core.annotation.Order} 注解决定
 * (orderedStream 自动遵循).
 */
@Component
public class CompositeMetaObjectHandler implements MetaObjectHandler {

    private final ObjectProvider<FieldFillerStrategy> strategiesProvider;

    public CompositeMetaObjectHandler(ObjectProvider<FieldFillerStrategy> strategiesProvider) {
        this.strategiesProvider = strategiesProvider;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        strategiesProvider.orderedStream().forEach(s -> s.onInsert(metaObject));
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strategiesProvider.orderedStream().forEach(s -> s.onUpdate(metaObject));
    }
}
