package com.school.management.infrastructure.inspection;

import com.school.management.infrastructure.extension.RecordRelationResolver;
import com.school.management.infrastructure.extension.ScopeContext;
import com.school.management.infrastructure.extension.SqlFragment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * R3c P2 PROVIDER:检查记录的「受检(我所属组织)」关系解析器 —— "发生在我所属组织的检查记录"。
 *
 * <p>受检面语义(对齐 {@code MyReceivedInspectionsApplicationService}):一条 {@code insp_submissions}
 * 的 {@code target_id} 是被检查的组织;"我能看到的受检记录" = target_id ∈ 我的成员组织。
 *
 * <p>过去这是"周级"难题:锚是 target_id <b>列</b>(非 owner 的 org_unit_id)+ 主体是"我的<b>全部</b>
 * 成员组织"(非 primary org)→ 既有引擎表达不了。PROVIDER 让 resolver <b>一把算</b>:子查询里
 * 先经访问图取我的成员组织,再命中 target_id。引擎包成 {@code id IN (<本子查询>)}。
 *
 * <p>注册见 {@code CoreManifest}:
 * {@code provider("inspection_record","inspected","受检(我所属组织)","USER","myReceivedInspectionsResolver")}。
 */
@Component("myReceivedInspectionsResolver")
public class MyReceivedInspectionsResolver implements RecordRelationResolver {

    @Override
    public SqlFragment subquery(ScopeContext ctx) {
        return SqlFragment.of(
            "SELECT sub.id FROM insp_submissions sub " +
            "WHERE sub.deleted = 0 AND sub.target_id IN (" +
            "  SELECT ar.resource_id FROM access_relations ar " +
            "  WHERE ar.subject_id = :me AND ar.relation = 'member' " +
            "    AND ar.resource_type = 'org_unit' AND ar.subject_type = 'user' AND ar.deleted = 0)",
            Map.of("me", ctx.userId())
        );
    }
}
