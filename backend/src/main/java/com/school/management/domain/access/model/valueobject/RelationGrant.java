package com.school.management.domain.access.model.valueobject;

import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.SubjectScope;
import com.school.management.domain.access.model.chain.Hop;

import java.util.List;
import java.util.Set;

/**
 * 关系授予 (统一锚定 R3) —— "这个角色对这个资源, 认可某关系, 其 subject 范围是 X"。
 *
 * <p>{@code role_data_scopes.relation_grants} JSON 数组的一个元素。一行配置 = 一组 grant,
 * 可见集 = 各 grant 子条件 <b>OR</b> 叠加 (这是 R3 相对 M1 单锚点的真增量: 多锚点 OR)。
 *
 * <ul>
 *   <li>{@link #relation} —— {@code resource_relations} 里该资源的关系码 (owner_org/creator/inspected…);
 *       引擎据其 storage_kind 出子条件。</li>
 *   <li>{@link #subject} + {@link #subjectParam} —— subject 范围 (见 {@link SubjectScope})。</li>
 *   <li>{@link #subtree} —— MY_ORG/RELATION 是否含子树。</li>
 *   <li>{@link #orgIds} —— CUSTOM 时的组织 id 集。</li>
 * </ul>
 */
public record RelationGrant(
        String relation,
        SubjectScope subject,
        String subjectParam,
        boolean subtree,
        Set<Long> orgIds,
        /**
         * P1 多级关系链中间跳 (可空)。空 = 1 跳 (subject 即组织解析, 走旧 composeGrant, 与 M1 字节等价);
         * 非空 = 多级链 (我 →[hops]→ 组织集 S, relation 作终端 over S, 走 ChainCompiler)。
         */
        List<Hop> hops
) {
    /**
     * 紧凑构造器: hops 归一非 null; subject 归一非 null (多级链 grant 的 subject 无意义可空,
     * 但 legacy 读路径桥接 {@link #anchorOf} 会 switch(subject) → 归一为 SELF 防 NPE; 链路径不读 subject)。
     */
    public RelationGrant {
        hops = hops == null ? List.of() : List.copyOf(hops);
        subject = subject == null ? SubjectScope.SELF : subject;
    }

    /** 向下兼容 5-arg 构造器 (旧 fromM1Axes / 既有 JSON 无 hops 字段 → 空跳)。 */
    public RelationGrant(String relation, SubjectScope subject, String subjectParam,
                         boolean subtree, Set<Long> orgIds) {
        this(relation, subject, subjectParam, subtree, orgIds, List.of());
    }

    /** 有中间跳 = 多级链 (走 ChainCompiler); 否则 1 跳 (走旧 composeGrant)。 */
    public boolean hasHops() {
        return hops != null && !hops.isEmpty();
    }

    /** 标准关系码常量 (与 resource_relations / CoreManifest 登记一致)。 */
    public static final String OWNER_ORG = "owner_org";
    public static final String CREATOR = "creator";

    /**
     * subject → 等价 {@link OrgAnchor} ({@link #fromM1Axes} 的逆, 单 grant)。
     *
     * <p>R3a-2b 读路径用: 删 role_data_scopes 轴①列后, getScopeSpec/mapToPermission 由 grant 反推
     * 轴① 喂拦截器既有检查 (isOrgUnbounded / PLUGIN_DIM), 避免改安全关键的拦截器。relation 字段
     * (creator/owner_org) 不影响 anchor (SELF 的成员/列锚分流由 meta.viaMembership 决定, 非此处)。
     */
    public OrgAnchor anchorOf() {
        return switch (subject) {
            case SELF -> OrgAnchor.SELF;
            case MY_ORG -> OrgAnchor.PRIMARY_ORG;
            case RELATION -> OrgAnchor.RELATION;
            case CUSTOM -> OrgAnchor.CUSTOM_ORG;
            case PLUGIN_DIM -> OrgAnchor.PLUGIN_DIM;
            case ALL -> OrgAnchor.ALL;
        };
    }

    /**
     * M1 三轴 → relation_grants 迁移映射 (R3a 字节等价的核心)。
     *
     * <p>{@code isMembershipResource}: 资源是否成员型 (storage_kind=SUBJECT_GRAPH, 如 user/student) ——
     * 决定 {@code SELF} 落 member-self (走 owner_org 成员图) 还是 creator 列。<b>这是迁移最易错点</b>:
     * 同一 M1 {@code SELF} 配置对成员资源与列锚资源产出不同 grant, 以保证引擎产同一 SQL。
     *
     * <p>轴② (subject_rel_include/exclude) 现有 74 行数据零使用, 不在本映射处理; 若出现由调用方守护。
     */
    public static List<RelationGrant> fromM1Axes(
            OrgAnchor anchor, String anchorParam, boolean subtree,
            Set<Long> customOrgIds, boolean isMembershipResource) {
        return switch (anchor) {
            case ALL -> List.of(new RelationGrant(OWNER_ORG, SubjectScope.ALL, null, false, null));
            // SELF: 成员资源走 member-self (owner_org 成员图); 列锚资源走 creator 列 (=我)。
            case SELF -> isMembershipResource
                    ? List.of(new RelationGrant(OWNER_ORG, SubjectScope.SELF, null, false, null))
                    : List.of(new RelationGrant(CREATOR, SubjectScope.SELF, null, false, null));
            case PRIMARY_ORG -> List.of(new RelationGrant(OWNER_ORG, SubjectScope.MY_ORG, null, subtree, null));
            case RELATION -> List.of(new RelationGrant(OWNER_ORG, SubjectScope.RELATION, anchorParam, subtree, null));
            case CUSTOM_ORG -> List.of(new RelationGrant(OWNER_ORG, SubjectScope.CUSTOM, null, subtree, customOrgIds));
            case PLUGIN_DIM -> List.of(new RelationGrant(OWNER_ORG, SubjectScope.PLUGIN_DIM, anchorParam, false, null));
        };
    }
}
