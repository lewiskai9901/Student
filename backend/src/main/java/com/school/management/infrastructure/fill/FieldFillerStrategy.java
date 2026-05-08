package com.school.management.infrastructure.fill;

import org.apache.ibatis.reflection.MetaObject;

/**
 * MyBatis-Plus MetaObjectHandler 的可组合策略接口.
 *
 * <p>背景: MyBatis-Plus Spring Boot 集成只允许一个 MetaObjectHandler bean,
 * 多个模块需要 audit 字段时必然冲突 (本项目 2026-05-08 验证: 加第二个 @Component
 * 会让 UserDomainMapper 注入歧义, 应用启动失败).
 *
 * <p>本策略接口让多个填充逻辑通过 {@link CompositeMetaObjectHandler} 组合,
 * 单一 bean 对外, 内部按 strategies 顺序委派.
 *
 * <p>实现示例:
 * <ul>
 *   <li>{@code AuditFieldsFiller} — createdAt / updatedAt / deleted 通用审计</li>
 *   <li>{@code InspectionDataPermissionFiller} — inspection 表 orgUnitId 数据权限边界</li>
 * </ul>
 */
public interface FieldFillerStrategy {

    /** INSERT 时填充. 实现应自检 PO 类型 + 字段存在性, 不影响其他类型 PO. */
    void onInsert(MetaObject metaObject);

    /** UPDATE 时填充. 大部分横切字段在 UPDATE 不动 (org_unit_id / deleted), 仅 updatedAt 类型字段需更新. */
    void onUpdate(MetaObject metaObject);
}
