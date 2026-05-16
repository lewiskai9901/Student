package com.school.management.infrastructure.fill;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.school.management.infrastructure.access.UserContextHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 通用审计字段自动填充: createdAt / updatedAt / deleted / createdBy / updatedBy.
 *
 * <p>2026-05-16 修复: 之前注释说"createdBy 字段在数据库中不存在, 已移除自动填充" — 错误.
 * 130 张表实际有 created_by 列, 关掉自动填充导致填充率 0%-25%, SELF scope 几乎失效.
 *
 * <p>填充策略:
 * <ul>
 *   <li>createdAt / updatedAt / deleted 走 strictInsertFill — 需要 PO 上 @TableField(fill=...) 注解才生效,
 *       这是 MyBatis-Plus 既定契约, 历史 PO 都已配好</li>
 *   <li>createdBy / updatedBy 走直接 metaObject.setValue() — 绕过 fill 注解依赖,
 *       因为 130 张有 created_by 列的 PO 大多没加 @TableField(fill=...) 注解,
 *       逐个补注解需改 130+ 文件. setValue 直接走 reflection,
 *       字段存在即填, fill 注解不必要</li>
 *   <li>fallback null — 非 web 调用 (scheduler / event listener / 系统任务)
 *       UserContextHolder 没值时不填, 不污染审计</li>
 * </ul>
 *
 * <p>通过 {@link CompositeMetaObjectHandler} 集成, 与其他填充策略并存.
 */
@Component
@Order(10)  // 通用审计字段优先填充
public class AuditFieldsFiller implements FieldFillerStrategy {

    private static final String CREATED_AT = "createdAt";
    private static final String UPDATED_AT = "updatedAt";
    private static final String DELETED    = "deleted";
    private static final String CREATED_BY = "createdBy";
    private static final String UPDATED_BY = "updatedBy";

    /** 桥接到 strictInsertFill / strictUpdateFill 的私有 handler 实例 */
    private final MetaObjectHandler bridge = new MetaObjectHandler() {
        @Override public void insertFill(MetaObject metaObject) { /* unused */ }
        @Override public void updateFill(MetaObject metaObject) { /* unused */ }
    };

    @Override
    public void onInsert(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // 走 strictInsertFill (依赖 @TableField(fill=INSERT) 注解):
        bridge.strictInsertFill(metaObject, CREATED_AT, LocalDateTime.class, now);
        bridge.strictInsertFill(metaObject, UPDATED_AT, LocalDateTime.class, now);
        bridge.strictInsertFill(metaObject, DELETED, Integer.class, 0);

        // createdBy / updatedBy 走直接 setValue — 不依赖 fill 注解,
        // 130 张表有 created_by 列但大多 PO 没加 @TableField, 这样最少改动.
        Long userId = UserContextHolder.getUserId();
        if (userId != null) {
            setIfFieldExistsAndNull(metaObject, CREATED_BY, userId);
            setIfFieldExistsAndNull(metaObject, UPDATED_BY, userId);
        }
    }

    @Override
    public void onUpdate(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        bridge.strictUpdateFill(metaObject, UPDATED_AT, LocalDateTime.class, now);

        Long userId = UserContextHolder.getUserId();
        if (userId != null) {
            // UPDATE 阶段总是覆盖 updatedBy, 反映"谁最后改的"
            if (metaObject.hasSetter(UPDATED_BY)) {
                metaObject.setValue(UPDATED_BY, userId);
            }
        }
    }

    /** 只在 PO 有此字段且当前值为 null 时填 — 防止覆盖业务显式 set 的值 */
    private void setIfFieldExistsAndNull(MetaObject metaObject, String field, Object value) {
        if (metaObject.hasGetter(field) && metaObject.hasSetter(field)
                && metaObject.getValue(field) == null) {
            metaObject.setValue(field, value);
        }
    }
}
