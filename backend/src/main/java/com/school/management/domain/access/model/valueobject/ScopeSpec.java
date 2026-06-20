package com.school.management.domain.access.model.valueobject;

import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.SubjectScope;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 数据范围规格 (ScopeSpec) —— 一行 role×resource×applyTo 数据范围配置的内存表示。
 *
 * <p>数据范围模型正从扁平的单枚举重构为若干<b>正交可组合的轴</b>。本值对象把这些轴聚合为
 * 一个不可变 (immutable) 规格, 供后续 {@code ScopeEvaluator} 编译为 SQL (READ) 与目标谓词 (WRITE)。
 * 本类<b>只是值对象 + 便利谓词</b>, 不含 DB、不含 evaluator、不含 preset 映射。
 *
 * <p>Composable data-scope value object representing one role×resource×applyTo row.
 * Aggregates the orthogonal axes into one immutable spec consumed later by the evaluator.
 *
 * <ul>
 *   <li><b>apply_to</b> —— 该规格治理的动作类: {@code "READ"} / {@code "WRITE"} / {@code "BOTH"}。</li>
 *   <li><b>轴① org anchor</b> —— {@link #orgAnchor} + {@link #anchorParam} (RELATION 时为关系码,
 *       PLUGIN_DIM 时为维度码) + {@link #includeSubtree} + {@link #customOrgIds} (CUSTOM_ORG 时)。</li>
 *   <li><b>轴② subject-relation filter</b> —— {@link #subjectRelInclude} / {@link #subjectRelExclude}。</li>
 *   <li><b>轴③ type filter</b> —— {@link #typeFilter}。</li>
 * </ul>
 */
@Data
@Builder
public class ScopeSpec {

    /** apply_to: 该规格治理的动作类 ("READ" / "WRITE" / "BOTH")。 */
    private String applyTo;

    /** 轴① org anchor: 可见组织集合的派生方式。 */
    private OrgAnchor orgAnchor;

    /** 轴① 参数: RELATION 时为关系码, PLUGIN_DIM 时为维度码。 */
    private String anchorParam;

    /** 轴① 是否含子树 (锚定组织向下展开)。 */
    private boolean includeSubtree;

    /** 轴① CUSTOM_ORG 时的指定组织 id 集合。 */
    private Set<Long> customOrgIds;

    /** 轴② subject-relation include: 主体必须命中其一的关系码。 */
    private Set<String> subjectRelInclude;

    /** 轴② subject-relation exclude: 命中其一即排除的关系码。 */
    private Set<String> subjectRelExclude;

    /** 轴③ type filter: 允许的类型码集合。 */
    private Set<String> typeFilter;

    /**
     * R3: 关系授予数组 (relation_grants)。非空时取代轴①(orgAnchor) 作为可见性来源 —— 各 grant
     * 子条件 OR 叠加 (多锚点)。为空 (R3a 过渡期 / 旧 spec) 时引擎由轴① bridge 派生单 grant。
     */
    private List<RelationGrant> relationGrants;

    /**
     * 不做组织过滤 (unbounded)。R3 grant-aware: relation_grants 非空时 = <b>任一</b> grant subject 为 ALL
     * (多锚点并集只要一条无界即整体无界); 否则 (旧 spec / sub-spec) 看 orgAnchor==ALL。
     */
    public boolean isOrgUnbounded() {
        if (hasRelationGrants()) {
            return relationGrants.stream().anyMatch(g -> g.subject() == SubjectScope.ALL);
        }
        return orgAnchor == OrgAnchor.ALL;
    }

    /** R3: relation_grants 是否生效 (非空)。null-safe。 */
    public boolean hasRelationGrants() {
        return relationGrants != null && !relationGrants.isEmpty();
    }

    /** 轴③是否生效 (type filter 非空)。null-safe。 */
    public boolean hasTypeFilter() {
        return typeFilter != null && !typeFilter.isEmpty();
    }

    /** 轴② include 是否生效 (非空)。null-safe。 */
    public boolean hasRelInclude() {
        return subjectRelInclude != null && !subjectRelInclude.isEmpty();
    }

    /** 轴② exclude 是否生效 (非空)。null-safe。 */
    public boolean hasRelExclude() {
        return subjectRelExclude != null && !subjectRelExclude.isEmpty();
    }
}
