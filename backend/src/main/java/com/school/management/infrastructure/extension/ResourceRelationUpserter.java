package com.school.management.infrastructure.extension;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资源关系 Upserter — 把单条 {@link ResourceRelationDef} UPSERT 到 {@code resource_relations}。
 *
 * <p>由 {@link ContributionDispatcher} 在分发 {@link Contribution.ResourceRelationContribution} 时调用。
 *
 * <p>与 {@link DataResourceUpserter} (UPDATE-only, 行由 migration 落库) 不同: {@code resource_relations}
 * 的行<b>由 contribute() 驱动</b>, 故 {@code INSERT ... ON DUPLICATE KEY UPDATE} (键
 * {@code uk_resource_relation(resource_code, relation_code, tenant_id)}), 重启幂等。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceRelationUpserter {

    private final JdbcTemplate jdbc;

    @Transactional
    public void upsert(ResourceRelationDef def, String industry) {
        jdbc.update(
            "INSERT INTO resource_relations " +
            "(resource_code, relation_code, relation_name, subject_type, cardinality, storage_kind, " +
            " column_name, type_column, ar_relation, resolver_bean, enforce_insert_scope, auto_fill, grants_by_default, industry, enabled, tenant_id) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,1,1) " +
            "ON DUPLICATE KEY UPDATE " +
            " relation_name=VALUES(relation_name), subject_type=VALUES(subject_type), " +
            " cardinality=VALUES(cardinality), storage_kind=VALUES(storage_kind), " +
            " column_name=VALUES(column_name), type_column=VALUES(type_column), " +
            " ar_relation=VALUES(ar_relation), resolver_bean=VALUES(resolver_bean), " +
            " enforce_insert_scope=VALUES(enforce_insert_scope), auto_fill=VALUES(auto_fill), " +
            " grants_by_default=VALUES(grants_by_default), industry=VALUES(industry), enabled=1",
            def.resourceCode(), def.relationCode(), def.relationName(), def.subjectType(),
            def.cardinality().name(), def.storageKind().name(),
            def.columnName(), def.typeColumn(), def.arRelation(), def.resolverBean(),
            def.enforceInsertScope() ? 1 : 0,
            def.autoFill() ? 1 : 0, def.grantsByDefault() ? 1 : 0, industry);
    }
}
