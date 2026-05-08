package com.school.management.infrastructure.fill;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 通用审计字段自动填充: createdAt / updatedAt / deleted.
 *
 * <p>原 {@code MybatisPlusConfig.metaObjectHandler} 匿名类的具名提取版本.
 * 通过 {@link CompositeMetaObjectHandler} 集成, 与其他填充策略并存.
 */
@Component
@Order(10)  // 通用审计字段优先填充
public class AuditFieldsFiller implements FieldFillerStrategy {

    /** 桥接到 strictInsertFill / strictUpdateFill 的私有 handler 实例 (拿不到 default method, 借壳用) */
    private final MetaObjectHandler bridge = new MetaObjectHandler() {
        @Override public void insertFill(MetaObject metaObject) { /* unused */ }
        @Override public void updateFill(MetaObject metaObject) { /* unused */ }
    };

    @Override
    public void onInsert(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        bridge.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        bridge.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        bridge.strictInsertFill(metaObject, "deleted", Integer.class, 0);
        // 注意: createdBy / updatedBy 字段在数据库中不存在, 已移除自动填充
    }

    @Override
    public void onUpdate(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        bridge.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);
        // 注意: updatedBy 字段在数据库中不存在, 已移除自动填充
    }
}
