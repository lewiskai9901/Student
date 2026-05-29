package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.OrgUnitScoreQueryService;
import com.school.management.application.inspection.dto.OrgScoreView;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 组织树得分查询控制器 (Phase 3.5a).
 *
 * <p>暴露 {@link com.school.management.application.inspection.OrgUnitScoreRollupService} 写入
 * {@code org_unit_scores} 的 roll-up 结果, 供前端做部门/组织排名展示.
 *
 * <p>数据权限: 与同模块查询端点一致 — {@code @CasbinAccess(resource="insp:project", action="view")}
 * 把住入口; 实际行级过滤由 mapper 层 {@code @DataPermission} + Filler 注入 org_unit_id 控制.
 */
@RestController
@RequestMapping("/inspection/org-scores")
@RequiredArgsConstructor
public class OrgUnitScoreController {

    private final OrgUnitScoreQueryService queryService;

    /**
     * 查 (project, cycleDate) 下各级组织的 roll-up 得分, 按 score 降序返回 (排名).
     *
     * @param projectId 项目 id
     * @param cycleDate 周期日 (ISO yyyy-MM-dd)
     */
    @GetMapping
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<List<OrgScoreView>> getOrgScores(
            @RequestParam Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cycleDate) {
        return Result.success(queryService.getOrgScores(projectId, cycleDate));
    }
}
