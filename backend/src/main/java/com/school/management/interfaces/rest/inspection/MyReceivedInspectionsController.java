package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.MyReceivedInspectionsApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 受检主体面 (Subject-side view) — 班主任 / 场所负责人 / 受检单位的"被检查"视角.
 *
 * <ul>
 *   <li>GET /received/inspections — 我所在组织被检查的全部历史</li>
 *   <li>GET /received/trends      — 4 周趋势 (按周聚合扣分)</li>
 *   <li>GET /received/recurring   — Top N 反复出问题的检查项</li>
 * </ul>
 *
 * <p>"我所在组织"通过 access_relations (subject=USER, relation=MEMBER_OF, resourceType=ORG_UNIT)
 * 推导. 如未挂任何 ORG_UNIT,返回空结果.
 */
@RestController
@RequestMapping("/inspection/received")
@RequiredArgsConstructor
public class MyReceivedInspectionsController {

    private final MyReceivedInspectionsApplicationService receivedService;

    /** 解析当前用户所在的 org_unit ID 列表. */
    private List<Long> myOrgUnitIds() {
        return receivedService.resolveOrgUnitIds(SecurityUtils.getCurrentUserId());
    }

    /** 历史检查记录 — 按时间倒序. */
    @GetMapping("/inspections")
    @CasbinAccess(resource = "insp:received", action = "view")
    public Result<List<Map<String, Object>>> myInspections(
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "30") int days) {
        List<Long> myOrgs = myOrgUnitIds();
        if (myOrgs.isEmpty()) return Result.success(List.of());
        return Result.success(receivedService.queryInspections(myOrgs, projectId, days));
    }

    /** 4 周趋势 — 按周聚合 (avg(score)/total/issueCount). */
    @GetMapping("/trends")
    @CasbinAccess(resource = "insp:received", action = "view")
    public Result<List<Map<String, Object>>> myTrends(
            @RequestParam(defaultValue = "4") int weeks) {
        List<Long> myOrgs = myOrgUnitIds();
        if (myOrgs.isEmpty()) return Result.success(List.of());
        if (weeks < 1) weeks = 4;
        if (weeks > 26) weeks = 26;
        return Result.success(receivedService.queryTrends(myOrgs, weeks));
    }

    /** Top N 反复出问题的检查项 (近 30 天). */
    @GetMapping("/recurring")
    @CasbinAccess(resource = "insp:received", action = "view")
    public Result<List<Map<String, Object>>> myRecurring(
            @RequestParam(defaultValue = "10") int limit) {
        List<Long> myOrgs = myOrgUnitIds();
        if (myOrgs.isEmpty()) return Result.success(List.of());
        if (limit < 1) limit = 10;
        if (limit > 50) limit = 50;
        return Result.success(receivedService.queryRecurring(myOrgs, limit));
    }

    /** 统计概览 — 顶部 KPI. */
    @GetMapping("/summary")
    @CasbinAccess(resource = "insp:received", action = "view")
    public Result<Map<String, Object>> mySummary(
            @RequestParam(defaultValue = "30") int days) {
        List<Long> myOrgs = myOrgUnitIds();
        Map<String, Object> out = new HashMap<>();
        out.put("orgUnitCount", myOrgs.size());
        out.put("orgUnitIds", myOrgs);
        if (myOrgs.isEmpty()) {
            out.put("totalInspections", 0);
            out.put("avgScore", null);
            out.put("openCorrectives", 0);
            out.put("overdueCorrectives", 0);
            return Result.success(out);
        }

        // 用 queryForList 避免 0 行抛 EmptyResultDataAccessException
        List<Map<String, Object>> inspRows = receivedService.querySummaryInspections(myOrgs, days);
        if (!inspRows.isEmpty()) {
            out.putAll(inspRows.get(0));
        } else {
            out.put("totalInspections", 0);
            out.put("avgScore", null);
            out.put("avgPct", null);
        }

        List<Map<String, Object>> csRows = receivedService.querySummaryCorrectives(myOrgs);
        if (!csRows.isEmpty()) {
            Map<String, Object> cs = csRows.get(0);
            out.put("openCorrectives", cs.get("openCorrectives") != null ? cs.get("openCorrectives") : 0);
            out.put("overdueCorrectives", cs.get("overdueCorrectives") != null ? cs.get("overdueCorrectives") : 0);
        } else {
            out.put("openCorrectives", 0);
            out.put("overdueCorrectives", 0);
        }

        return Result.success(out);
    }
}
