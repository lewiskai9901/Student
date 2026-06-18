package com.school.management.domain.access.model;

/**
 * 关系的物理存储种类 (统一锚定模型)。
 *
 * <ul>
 *   <li>{@link #SUBJECT_GRAPH} —— 存 {@code access_relations} 主体图 (资源本身是主体, 关系是主体↔主体边)。</li>
 *   <li>{@link #COLUMN} —— 存业务表的列 (单值; 最快, 强不变量)。</li>
 *   <li>{@link #RECORD_RELATION} —— 存 {@code record_relations} 表 (多值, 列装不下集合)。</li>
 *   <li>{@link #MATERIALIZED} —— 热路径叠加: 物化倒排索引加速 (设计 §11)。<b>不</b>作为关系的声明存储,
 *       由系统按热度叠加。</li>
 * </ul>
 *
 * <p><b>存储不是单一 derive 函数的输出</b>: 同一资源的不同关系存储各异 (如普通记录的 creator 落列、
 * reviewer 落 record_relations、被检查组织落列)。因此存储<b>按关系声明</b> (见
 * {@code ResourceRelationDef} 的 column()/recordRelation()/subjectGraph() 工厂),
 * 与基数的一致性 (COLUMN⟺SINGLE / RECORD_RELATION⟹MULTI) 在 Def 构造期校验。
 * "主体型资源的<i>组织归属</i>关系必须为 SUBJECT_GRAPH 而非 COLUMN" 是<b>资源级</b>反退化守护
 * (防重建已删的 user_student.org_unit_id), 属设计 §10 / R6。
 */
public enum StorageKind {
    SUBJECT_GRAPH, COLUMN, RECORD_RELATION, MATERIALIZED;

    /** 按 {@code name()} 严格解析, 未知/null 返回 {@code null} (供 PO 反序列化, 同 OrgAnchor 约定)。 */
    public static StorageKind fromCode(String c) {
        if (c != null) {
            for (StorageKind s : values()) {
                if (s.name().equals(c)) return s;
            }
        }
        return null;
    }
}
