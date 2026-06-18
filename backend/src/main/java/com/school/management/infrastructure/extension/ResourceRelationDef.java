package com.school.management.infrastructure.extension;

import com.school.management.domain.access.model.Cardinality;
import com.school.management.domain.access.model.StorageKind;

/**
 * 单条「资源关系」的注册声明 (统一锚定模型核心)。对应 {@code resource_relations} 表一行。
 *
 * <p>一个资源 (检查记录 / 学生 / 预约…) 由若干具名关系描述; 每条关系声明它指向哪类主体、基数、
 * 物理存储落点。插件通过 {@link Contribution.ResourceRelationContribution} 在
 * {@link PluginPackage#contribute()} 里登记, 启动期由 {@code ResourceRelationUpserter} UPSERT。
 *
 * <p>用工厂方法声明 (存储与基数自洽):
 * <ul>
 *   <li>{@link #column} —— 单值落列 (creator/owner_org/单一被检查)。</li>
 *   <li>{@link #recordRelation} —— 多值落 record_relations (reviewer/分享/多被检查)。</li>
 *   <li>{@link #subjectGraph} —— 主体型资源的关系走 access_relations (student.owner_org=member)。</li>
 * </ul>
 * 链式修饰: {@link #withAutoFill()} (写入自动填列)、{@link #withGrantsByDefault()} (无显式授予时默认可见)、
 * {@link #polymorphic(String)} (多态主体的类型列, 如 target_type)。
 *
 * @param resourceCode    data_resources.resource_code
 * @param relationCode    关系码 (creator/owner_org/inspector/inspected/reviewer)
 * @param relationName    人话显示名 (UI)
 * @param subjectType     指向主体类型 USER/ORG_UNIT/PLACE/ASSET/ANY
 * @param cardinality     基数
 * @param storageKind     物理存储种类 (与基数一致性构造期校验; 不接受 MATERIALIZED)
 * @param columnName      COLUMN 存储: 业务表列名
 * @param typeColumn      多态主体的类型列 (配 columnName), 可空
 * @param arRelation      SUBJECT_GRAPH/RECORD_RELATION 存储: access_relations/record_relations 的 relation 值
 * @param autoFill        写入是否自动填 (creator/owner_org=true)
 * @param grantsByDefault 无显式授予时是否默认参与可见性 (一般 owner_org=true)
 */
public record ResourceRelationDef(
        String resourceCode, String relationCode, String relationName, String subjectType,
        Cardinality cardinality, StorageKind storageKind,
        String columnName, String typeColumn, String arRelation,
        boolean autoFill, boolean grantsByDefault) {

    public ResourceRelationDef {
        requireText(resourceCode, "resourceCode");
        requireText(relationCode, "relationCode");
        requireText(relationName, "relationName");
        requireText(subjectType, "subjectType");
        if (cardinality == null) {
            throw new IllegalArgumentException("cardinality 不能为空: " + relationCode);
        }
        if (storageKind == null) {
            throw new IllegalArgumentException("storageKind 不能为空: " + relationCode);
        }
        switch (storageKind) {
            case COLUMN -> {
                if (cardinality != Cardinality.SINGLE) {
                    throw new IllegalArgumentException(
                        "COLUMN 存储要求 SINGLE 基数 (列装不下集合): " + relationCode);
                }
                requireText(columnName, "columnName (COLUMN 存储必填): " + relationCode);
            }
            case RECORD_RELATION -> {
                if (cardinality != Cardinality.MULTI) {
                    throw new IllegalArgumentException(
                        "RECORD_RELATION 存储要求 MULTI 基数: " + relationCode);
                }
                requireText(arRelation, "arRelation (RECORD_RELATION 存储必填): " + relationCode);
            }
            case SUBJECT_GRAPH -> requireText(arRelation, "arRelation (SUBJECT_GRAPH 存储必填): " + relationCode);
            case MATERIALIZED -> throw new IllegalArgumentException(
                "MATERIALIZED 是物化叠加层, 不能作为关系的声明存储: " + relationCode);
        }
    }

    private static void requireText(String v, String field) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
    }

    // ── 工厂 ──────────────────────────────────────────────────────────────
    public static ResourceRelationDef column(String resourceCode, String relationCode, String relationName,
                                             String subjectType, String columnName) {
        return new ResourceRelationDef(resourceCode, relationCode, relationName, subjectType,
                Cardinality.SINGLE, StorageKind.COLUMN, columnName, null, null, false, false);
    }

    public static ResourceRelationDef recordRelation(String resourceCode, String relationCode, String relationName,
                                                     String subjectType, String arRelation) {
        return new ResourceRelationDef(resourceCode, relationCode, relationName, subjectType,
                Cardinality.MULTI, StorageKind.RECORD_RELATION, null, null, arRelation, false, false);
    }

    public static ResourceRelationDef subjectGraph(String resourceCode, String relationCode, String relationName,
                                                   String subjectType, Cardinality cardinality, String arRelation) {
        return new ResourceRelationDef(resourceCode, relationCode, relationName, subjectType,
                cardinality, StorageKind.SUBJECT_GRAPH, null, null, arRelation, false, false);
    }

    // ── 链式修饰 ──────────────────────────────────────────────────────────
    public ResourceRelationDef withAutoFill() {
        return new ResourceRelationDef(resourceCode, relationCode, relationName, subjectType,
                cardinality, storageKind, columnName, typeColumn, arRelation, true, grantsByDefault);
    }

    public ResourceRelationDef withGrantsByDefault() {
        return new ResourceRelationDef(resourceCode, relationCode, relationName, subjectType,
                cardinality, storageKind, columnName, typeColumn, arRelation, autoFill, true);
    }

    public ResourceRelationDef polymorphic(String typeColumn) {
        return new ResourceRelationDef(resourceCode, relationCode, relationName, subjectType,
                cardinality, storageKind, columnName, typeColumn, arRelation, autoFill, grantsByDefault);
    }
}
